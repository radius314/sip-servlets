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

package org.mobicents.media.server.resource;

import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.spi.Valve;

/**
 * Pipe is part of a channel and joins individual components.
 * 
 * @author kulikov
 */
public class Pipe {
    
    private MediaSink sink;
    private MediaSource source;
    
    //by default setting to close
    private Valve valve = Valve.CLOSE;
    
    private Channel channel;

    /**
     * Coinstructs new pipe.
     * 
     * @param channel the owner of this pipe;
     */
    protected Pipe(Channel channel) {
        this.channel = channel;
    }
    
    public Valve getValve() {
        return valve;
    }
    
    public void setValve(Valve valve) {
        this.valve = valve;
    }
    
    /**
     * Joins source with sink
     * 
     * @param inletName the name of source component.
     * @param outletName the name of sink component.
     * @throws UnknownComponentException if inletName or outletName is unknown
     */
    public void open(String inletName, String outletName) throws UnknownComponentException {
  	
        if (!channel.sources.containsKey(inletName)) {
            throw new UnknownComponentException(inletName+" \n--> "+channel.sources+" \n--> "+channel.sinks);
        }
        
        source = channel.sources.get(inletName);
        if (!channel.sinks.containsKey(outletName)) {
            source = null;
            throw new UnknownComponentException(outletName+" --> "+channel.sinks);
        }
        
        sink = channel.sinks.get(outletName);
        sink.connect(source);
    }
    
    public void start() {
    	if(this.valve == Valve.OPEN){
    		if (sink != null ) {
    			sink.start();
    		}
        
    		if(source != null){
    			source.start();
    		}
    	}
    }
    
    public void stop() {
        if (sink != null) {            
            sink.stop();
        }
        
        if( source != null){
        	source.stop();
        }
    }
    /**
     * Closes this pipe if it was open and do nothing if pipe was not open.
     */
    public void close() {
        if (sink != null && source != null) {
            sink.disconnect(source);
            sink = null;
            source = null;
        }
    }
}

