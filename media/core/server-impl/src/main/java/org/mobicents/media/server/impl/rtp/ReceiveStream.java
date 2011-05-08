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

package org.mobicents.media.server.impl.rtp;

import java.util.ArrayList;
import org.mobicents.media.Buffer;
import org.mobicents.media.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.rtp.rfc2833.DtmfConverter;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 * 
 * @author Oleg Kulikov
 */
public class ReceiveStream extends AbstractSource {

    private final static int DEFAULT_POLL_PERIOD = 20;
    
    private RtpSocketImpl rtpSocket;
    private JitterBuffer jitterBuffer;
    protected int mainstream;
    private Format rtpFormat;
    private int dtmf;
    private ArrayList<Format> formats = new ArrayList();
    private AVProfile avProfile;
    private Codec codec;
    private int jitter ;
    //RFC 2833
    private DtmfConverter dtmfConverter = new DtmfConverter();
    
    protected long byteCount;
    
    /** Creates a new instance of ReceiveStream */
    public ReceiveStream(RtpSocketImpl rtpSocket, int jitter, AVProfile formatConfig) {
        super("ReceiveStream");
        this.jitter = jitter;
        this.avProfile = formatConfig;
        this.rtpSocket = rtpSocket;
        
        //construct jitter buffer
        jitterBuffer = new JitterBuffer(jitter);
        jitterBuffer.setClock(rtpSocket.getClock());
        
        dtmfConverter.setClock(rtpSocket.getClock());        
    }

    /**
     * Processes received RTP packet.
     * 
     * @param rtpPacket packet for processing
     */
    protected void process(RtpPacket rtpPacket) {
        //write packet to jitter buffer.
        //the purpose of jitter buffer is to transform varibale jitter into
        //fixed delay.
        jitterBuffer.write(rtpPacket);
    }

    @Override
    public void beforeStart() {
        //let's reset jitter buffer
        jitterBuffer.reset();
    }

    @Override
    public void connect(MediaSink sink) {
        if (this.rtpSocket.getFormat() == null) {
            throw new IllegalStateException("RTP has no negotiated formats");
        }
        super.connect(sink);
    }

    /**
     * Assigns payload number for rfc2833 dtmf.
     * 
     * @param dtmf the number of payload.
     */
    public void setDtmf(int dtmf) {
        this.dtmf = dtmf;
    }
    
    /**
     * Configures supported formats of main stream.
     * 
     * @param payloadID the payload number of format used by rtp socket
     * @param format the format used by rtp socket.
     */
    protected void setFormat(int payloadID, Format format) {
        //set format of the mail stream
        this.mainstream = payloadID;
        this.rtpFormat = format;
        
        jitterBuffer.setFormat(format);

        //supported formats are combination of
        //specified format and possible transcodings
        formats.clear();
        formats.add(format);

        //looking for possible transcodings
        for (Codec c : rtpSocket.codecs) {
            if (c.getSupportedInputFormat().matches(format)) {
                formats.add(c.getSupportedOutputFormat());
            }
        }
        
//        this.dtmfConverter.setPreffered(format);
    }

    /**
     * (Non Java-doc.)
     * 
     * @see org.mobicents.media.MediaSink#getFormats() 
     */
    public Format[] getFormats() {
        //if RTP socket is not configured send stream can 
        //not send something
        if (rtpSocket.getFormat() == null) {
            return new Format[0];
        }

        //return supported formats
        Format[] fmts = new Format[formats.size()];
        formats.toArray(fmts);

        return fmts;
    }

    @Override
    public void setPreffered(Format format) {
        super.setPreffered(format);
        if (format == null) {
            return;
        }
        
        dtmfConverter.setPreffered(format);
        //if preffred format matches to socket's format then no need 
        //in transcoding
        if (format.matches(rtpSocket.getFormat())) {
            codec = null;
            return;
        }

        //transcoding required. selecting suitable codec
        //NOTE! at least one suitable codec exist because components  
        //has just comleted analysis of supported formats!
        for (Codec c : rtpSocket.codecs) {
            if (c.getSupportedOutputFormat().matches(format) && c.getSupportedInputFormat().matches(this.rtpFormat)) {
                codec = c;
                return;
            }
        }
    }

    /**
     * Gets the value of the interarrival jitter.
     * 
     * @return jitter value
     */
    public double getInterArrivalJitter() {
        return jitterBuffer.getInterArrivalJitter();
    }
    
    /**
     * Returns maximum jitter value.
     * 
     * @return the jitter value.
     */
    public double getMaxJitter() {
        return jitterBuffer.getMaxJitter();        
    }
    
    public void evolve(Buffer buffer, long timestamp) {
        //reading next packet from jitter buffer
        RtpPacket packet = jitterBuffer.read(timestamp);
        if (packet == null) {
            buffer.setFlags(Buffer.FLAG_DISCARD);
            buffer.setDuration(DEFAULT_POLL_PERIOD);
        } else if (packet.getPayloadType() == mainstream) {
            buffer.setData(packet.getPayload());
            buffer.setOffset(0);
            buffer.setTimeStamp(timestamp);
            buffer.setLength(packet.getPayload().length);
            
            if (packet.getDuration() >= 0) {
                buffer.setDuration(packet.getDuration());
            } else {
                buffer.setDuration(DEFAULT_POLL_PERIOD);
            }
            buffer.setFormat(format);
            if (packet.getMarker()) {
                buffer.setFlags(buffer.getFlags() | Buffer.FLAG_KEY_FRAME);
            }
            if (codec != null) {
                codec.process(buffer);
            }
        } else if (packet.getPayloadType() == dtmf) {
            dtmfConverter.process(packet, buffer);
            buffer.setTimeStamp(timestamp);
        } else {
            buffer.setFlags(Buffer.FLAG_DISCARD);
            buffer.setDuration(DEFAULT_POLL_PERIOD);
        }
        
        if (logger.isTraceEnabled()) {
            logger.trace("Receive " + buffer);
        }
        byteCount += (buffer.getLength() + 12);
    }

    /**
     * Resets this stream.
     */
    protected void reset() {
        mainstream = -1;
        formats.clear();
        byteCount = 0;
        //jitterBuffer.reset();
        jitterBuffer = new JitterBuffer(jitter);
        jitterBuffer.setClock(rtpSocket.getClock());
    }
    
    
}
