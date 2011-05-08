/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.server.impl;

import java.util.Collection;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.Inlet;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.Server;
import org.mobicents.media.server.impl.clock.LocalTask;
import org.mobicents.media.server.spi.events.NotifyEvent;

/**
 * The base implementation of the Media source.
 * 
 * <code>AbstractSource</code> and <code>AbstractSink</code> are implement general wirring contruct. All media
 * components have to extend one of these classes.
 * 
 * @author Oleg Kulikov
 */
public abstract class AbstractSource extends BaseComponent implements MediaSource {

    //reference to the media sink connected to this component.
    protected transient MediaSink otherParty;
    
    //state lock for locking state during start and stop
    private ReentrantLock state = new ReentrantLock();
    
    //transmission statisctics
    private long packetsTransmitted;
    private long bytesTransmitted;
    
    //shows if component is started or not.
    private volatile boolean started;
    
    //events generated by each component
    private NotifyEvent evtStarted;
    private NotifyEvent evtCompleted;
    private NotifyEvent evtStopped;
    
    //logger instance
    protected Logger logger;
    
    //local media time and packet sequence number.
    private volatile long timestamp = 0;
    private long sequenceNumber = 1;

    //duration is assigned
    private long duration = -1;
    
    private LocalTask worker;
    /**
     * Creates new instance of source with specified name.
     * 
     * @param name
     *            the name of the sink to be created.
     */
    public AbstractSource(String name) {
        super(name);
        logger = Logger.getLogger(getClass());
        evtStarted = new NotifyEventImpl(this, NotifyEvent.STARTED,"Started");
        evtCompleted = new NotifyEventImpl(this, NotifyEvent.COMPLETED,"Completed");
        evtStopped = new NotifyEventImpl(this, NotifyEvent.STOPPED,"Stoped");
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#getMediaTime();
     */
    public long getMediaTime() {
        return timestamp;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#setMediaTime(long timestamp);
     */
    public void setMediaTime(long timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#getDuration(long duration);
     */
    public long getDuration() {
        return duration;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#setDuration(long duration);
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    /**
     * This method is called just before start.
     * 
     * The descendant classes can verride this method and put additional logic
     */
    protected void beforeStart() throws Exception {
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#start().
     */
    public void start() {
        state.lock();
        try {
            if (started) {
                return;
            }

            started = true;
            timestamp = 0;
            sequenceNumber = 0;

            beforeStart();

            if (otherParty != null && !otherParty.isStarted()) {
                otherParty.start();
            }

            //obtain current timestamp and schedule periodic execution
            //timestamp = syncSource.getTimestamp();
            worker = Server.scheduler.execute(this);

            //started!
            started();
        } catch (Exception e) {
            e.printStackTrace();
            started = false;
            failed(NotifyEvent.START_FAILED, e);
        } finally {
            state.unlock();
        }
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#stop().
     */
    public void stop() {
        state.lock();
        try {
            started = false;
            if (worker != null) {
                worker.cancel();
            }
            
            if (logger.isDebugEnabled()) {
                logger.debug(this + " stopped");
            }

            if (otherParty != null && otherParty.isStarted()) {
                otherParty.stop();
            }

            stopped();
            afterStop();
            timestamp = 0;
        } finally {
            state.unlock();
        }
    }

    public void cancel() {
        stop();
    }

    public boolean isActive() {
        return started;
    }

    protected void setStarted(boolean started) {
        this.started = started;
    }
    
    /**
     * This method is called immediately after processing termination.
     * 
     */
    public void afterStop() {
    }

    public boolean isMultipleConnectionsAllowed() {
        return false;
    }

    /**
     * This methods is called by media sink to setup preffered format.
     * Media source in opposite direction can ask media sink to get 
     * preffered format by calling <code>sink.getPreffred(Collection<Format>)</code>
     * where collection is a subset of common formats.
     * 
     * @param format preffred format.
     */
    public void setPreffered(Format format) {
        this.format = format;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#connect(MediaSink).
     */
    public void connect(MediaSink otherParty) {
        state.lock();
        try {
            // we can not connect with null
            if (otherParty == null) {
                throw new IllegalArgumentException("Other party can not be null");
            }

            //this implemntation suppose that other party is instance of AbstractSink
            if (!(otherParty instanceof AbstractSink)) {
                throw new IllegalArgumentException("Can not connect: " +
                        otherParty + " does not extends AbstractSink");
            }

            //if other party allows multiple connection (like mixer or mux/demux
            //we should delegate connection procedure to other party because other party
            //maintances internal components
            if (otherParty.isMultipleConnectionsAllowed()) {
                otherParty.connect(this);
                return;
            }

            //if we are here then this is the most common case when we are joining 
            //two components
            AbstractSink sink = (AbstractSink) otherParty;

            //calculating common formats
            Collection<Format> subset = this.subset(getFormats(), otherParty.getFormats());

            //connection is possible if and only if both components have common formats
            if (subset.isEmpty()) {
                throw new IllegalArgumentException("Format missmatch");
            }

            //now we have to ask sink to select preffered format
            //if sink can not determine preffred format at this time it will return null
            //and from now the sink is responsible for assigning preffred format to this sink
            Format preffered = sink.getPreffered(subset);
            if (preffered != null) {
                setPreffered(preffered);
            }

            //creating cross refernces to each other
            sink.otherParty = this;
            this.otherParty = sink;


            if (logger.isDebugEnabled()) {
                logger.debug(this + " is connected to " + otherParty);
            }
        } finally {
            state.unlock();
        }
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#diconnection(MediaSink).
     */
    public void disconnect(MediaSink otherParty) {
        state.lock();
        try {
            // check argument
            if (otherParty == null) {
                throw new IllegalArgumentException("Other party can not be null");
            }

            //this implementation suppose to work with AbstractSink
            if (!(otherParty instanceof AbstractSink)) {
                throw new IllegalArgumentException("Can not disconnect: " + otherParty + " is not connected");
            }

            //if other party allows multiple connections then we have to deligate call to other party
            if (otherParty.isMultipleConnectionsAllowed()) {
                otherParty.disconnect(this);
                return;
            }

            //in this case we are checking that other party is connected to this component
            if (otherParty != this.otherParty) {
                throw new IllegalArgumentException("Can not disconnect: " + otherParty + " is not connected");
            }

            //indeed the other party is connected so we can break this connection 
            //by removing cross reference and cleaning formats
            AbstractSink sink = (AbstractSink) otherParty;

            //cleaning formats
            setPreffered(null);
            sink.getPreffered(null);

            //cleaning references
            sink.otherParty = null;
            this.otherParty = null;
        } finally {
            state.unlock();
        }
    }

    public void connect(Inlet inlet) {
        connect(inlet.getInput());
    }

    public void disconnect(Inlet inlet) {
        disconnect(inlet.getInput());
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSink#isConnected().
     */
    public boolean isConnected() {
        return otherParty != null;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#isStarted().
     */
    public boolean isStarted() {
        return this.started;
    }

    /**
     * This method must be overriden by concrete media source. The media have to fill buffer with media data and
     * attributes.
     * 
     * @param buffer the buffer object for media.
     * @param sequenceNumber
     *            the number of timer ticks from the begining.
     */
    public abstract void evolve(Buffer buffer, long timestamp);

    protected String getSupportedFormatList() {
        String s = "";
        if (otherParty != null) {
            Format[] formats = otherParty.getFormats();
            for (int i = 0; i < formats.length; i++) {
                s += formats[i] + ";";
            }
        }
        return s;
    }

    public int perform() {
        Buffer buffer = new Buffer();
        buffer.setFormat(format);

        evolve(buffer, timestamp);
        
        sequenceNumber = inc(sequenceNumber);
        
        buffer.setTimeStamp(timestamp);
        buffer.setSequenceNumber(sequenceNumber);
        
        
        //if buffer is marked as discarde we should not send it to consumer
        if (buffer.isDiscard()) {
            return (int) buffer.getDuration();
        }

        //timestamp is incremented if and only if buffer is not discarded
        timestamp += buffer.getDuration();
        

        if (logger.isTraceEnabled()) {
            logger.trace(this + " sending " + buffer + " to " + otherParty);
        }

        //let's do final check because other thread can disconect parties.
        if (otherParty == null) {
            return (int) buffer.getDuration();
        }
        //delivering data to the other party.
        try {
            if (buffer.getLength() > 0) {
                otherParty.receive(buffer);
            }
        } catch (Exception e) {
            logger.error("Can not deliver packet to " + otherParty, e);
            failed(NotifyEvent.TX_FAILED, e);
        }


        //at the final step we are incrementing stats
        packetsTransmitted++;
        bytesTransmitted += buffer.getLength();

        //if max duration is specified and media time reaches duration
        //the raise flag end of media.
        if (duration > 0 && timestamp >= duration) {
            buffer.setEOM(true);
        }
        
        //notify listeners and return negative duration
        if (buffer.isEOM()) {
            this.completed();
            return -1;
        }
        
        //return duration of this packet for scheduling next run
        return (int) buffer.getDuration();
    }

    /**
     * Sends notification that media processing has been started.
     */
    protected void started() {
        sendEvent(evtStarted);
    }

    /**
     * Sends failure notification.
     * 
     * @param eventID
     *            failure event identifier.
     * @param e
     *            the exception caused failure.
     */
    protected void failed(int eventID, Exception e) {
        FailureEventImpl failed = new FailureEventImpl(this, eventID, e);
        worker.cancel();
        this.stop();
        sendEvent(failed);
    }

    /**
     * Sends notification that signal is completed.
     * 
     */
    protected void completed() {
        worker.cancel();
        System.out.println("Stopping " + this);
        this.started = false;
        sendEvent(evtCompleted);
    }

    /**
     * Sends notification that detection is terminated.
     * 
     */
    protected void stopped() {
        sendEvent(evtStopped);
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#getPacketsReceived()
     */
    public long getPacketsTransmitted() {
        return packetsTransmitted;
    }

    /**
     * (Non Java-doc).
     * 
     * @see org.mobicents.media.MediaSource#getBytesTransmitted()
     */
    public long getBytesTransmitted() {
        return bytesTransmitted;
    }

    @Override
    public void resetStats() {
        this.packetsTransmitted = 0;
        this.bytesTransmitted = 0;
    }
    
    private long inc(long v) {
        if (v == Long.MAX_VALUE) {
            v = -1;
        }
        return ++v;
    }
    /* (non-Javadoc)
	 * @see org.mobicents.media.MediaSink#getInterface(java.lang.Class)
	 */
	public <T> T getInterface(Class<T> interfaceType) {
		//should we check default?
		return null;
	}
}
