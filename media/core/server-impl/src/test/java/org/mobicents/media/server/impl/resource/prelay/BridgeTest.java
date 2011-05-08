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

package org.mobicents.media.server.impl.resource.prelay;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.Utils;
import org.mobicents.media.server.VirtualEndpointImpl;
import org.mobicents.media.server.impl.resource.fft.AnalyzerFactory;
import org.mobicents.media.server.impl.resource.fft.SpectrumEvent;
import org.mobicents.media.server.impl.resource.test.SineGeneratorFactory;
import org.mobicents.media.server.resource.ChannelFactory;
import org.mobicents.media.server.spi.NotificationListener;
import org.mobicents.media.server.spi.events.NotifyEvent;

/**
 *
 * @author kulikov
 */
public class BridgeTest {

    private final static int FREQ_ERROR = 5;
    private int MAX_ERRORS = 3;

    private final static int[] FREQ = new int[]{50, 250};
    private VirtualEndpointImpl e1,  e2;
    private VirtualEndpointImpl cnf;
    private SineGeneratorFactory g1,  g2;
    private AnalyzerFactory a1,  a2;
    private ArrayList<double[]> s1,  s2;
    private ChannelFactory channelFactory;
    private BridgeFactory packetRelayFactory;
    
    private Semaphore semaphore;
    private boolean res;
    
    public BridgeTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
/*        semaphore = new Semaphore(0);
        res = false;

        s1 = new ArrayList();
        s2 = new ArrayList();

        channelFactory = new ChannelFactory();
        channelFactory.start();

        ConnectionFactory connectionFactory = new ConnectionFactory();
        
        Hashtable rxFactories = new Hashtable();
        rxFactories.put("audio", channelFactory);

        Hashtable txFactories = new Hashtable();
        txFactories.put("audio", channelFactory);
        
        connectionFactory.setRxChannelFactory(rxFactories);
        connectionFactory.setTxChannelFactory(txFactories);
        
        a1 = new AnalyzerFactory();
        a1.setName("a1");

        a2 = new AnalyzerFactory();
        a2.setName("a2");

        g1 = new SineGeneratorFactory();
        g1.setName("g1");
        g1.setF(FREQ[0]);
        g1.setA(Short.MAX_VALUE);
        
        g2 = new SineGeneratorFactory();
        g2.setName("g2");
        g2.setF(FREQ[1]);
        g2.setA(Short.MAX_VALUE);


        e1 = new VirtualEndpointImpl("/pr/test/1");
        e2 = new VirtualEndpointImpl("/pr/test/2");

        cnf = new VirtualEndpointImpl("/pr/test/cnf");

        e1.setConnectionFactory(connectionFactory);
        e2.setConnectionFactory(connectionFactory);

        cnf.setConnectionFactory(connectionFactory);

        Hashtable sources1 = new Hashtable();
        sources1.put("audio", g1);

        Hashtable sources2 = new Hashtable();
        sources2.put("audio", g2);

        Hashtable sinks1 = new Hashtable();
        sinks1.put("audio", a1);

        Hashtable sinks2 = new Hashtable();
        sinks2.put("audio", a2);
        
        e1.setSourceFactory(sources1);
        e2.setSourceFactory(sources2);

        e1.setSinkFactory(sinks1);
        e2.setSinkFactory(sinks2);
        
        ArrayList<CodecFactory> codecs = new ArrayList();
        DspFactory dspFactory = new DspFactory();
        dspFactory.setName("dsp");
        
        dspFactory.setCodecFactories(codecs);
        
        packetRelayFactory = new BridgeFactory();
        packetRelayFactory.setName("Packet-Relay");
        packetRelayFactory.setDspFactory(dspFactory);
        
        cnf.setGroupFactory(packetRelayFactory);

        e1.start();
        e2.start();

        cnf.start();
        
        e1.getComponent("a1").addListener(new AnalyzerListener(s1));
        e2.getComponent("a2").addListener(new AnalyzerListener(s2));
 */ 
    }

    @After
    public void tearDown() {
    }


    /**
     * Test of getSink method, of class Bridge.
     */
    @Test
    public void testSimpleTransmission() throws Exception {
/*        Connection c1 = e1.createLocalConnection();
        c1.setMode(ConnectionMode.SEND_RECV);
        Connection c2 = e2.createLocalConnection();
        c2.setMode(ConnectionMode.SEND_RECV);

        Connection cc1 = cnf.createLocalConnection();
        cc1.setMode(ConnectionMode.SEND_RECV);
        Connection cc2 = cnf.createLocalConnection();
        cc2.setMode(ConnectionMode.SEND_RECV);

        c1.setOtherParty(cc1);
        c2.setOtherParty(cc2);

        MediaSource gen1 = (MediaSource) e1.getComponent("g1");
        gen1.start();
        ((MediaSink)e1.getComponent("a1")).start();
        
        MediaSource gen2 = (MediaSource) e2.getComponent("g2");
        gen2.start();
        ((MediaSink)e2.getComponent("a2")).start();

        semaphore.tryAcquire(10, TimeUnit.SECONDS);
        
        gen1.stop();
        gen2.stop();

        e1.deleteAllConnections();
        e2.deleteAllConnections();
        
        cnf.deleteAllConnections();
        //We have to wait, cause threads may not end immediatly...
        try{
        	Thread.currentThread().sleep(1000);
    	}catch(Exception e)
    	{}
        res = verify(s1, new int[]{FREQ[1]});
        assertEquals(true, res);
        
        res = verify(s2, new int[]{FREQ[0]});
        assertEquals(true, res);
*/
    }

    private boolean verify(ArrayList<double[]> spectra, int[] F) {
        int errorCount = 0;
        int i =0;
        for (double[] s : spectra) {
            int[] ext = Utils.getFreq(s);
            boolean r = Utils.checkFreq(ext, F, FREQ_ERROR);
            if (!r) {
                errorCount++;
            }
        }
        return (errorCount <= MAX_ERRORS);
    }

    private class AnalyzerListener implements NotificationListener {

        private ArrayList<double[]> s;

        public AnalyzerListener(ArrayList<double[]> s) {
            this.s = s;
        }

        public void update(NotifyEvent event) {
            if (event.getEventID() == SpectrumEvent.SPECTRA) {
                SpectrumEvent evt = (SpectrumEvent) event;
                s.add(evt.getSpectra());
            }
        }
    }
    
}