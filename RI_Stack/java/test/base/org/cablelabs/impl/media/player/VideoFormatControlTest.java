// COPYRIGHT_BEGIN
//  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
//  
//  Copyright (C) 2008-2013, Cable Television Laboratories, Inc. 
//  
//  This software is available under multiple licenses: 
//  
//  (1) BSD 2-clause 
//   Redistribution and use in source and binary forms, with or without modification, are
//   permitted provided that the following conditions are met:
//        ·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        ·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
//             and the following disclaimer in the documentation and/or other materials provided with the 
//             distribution.
//   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
//   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED 
//   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
//   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
//   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
//   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
//   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
//   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
//   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
//   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
//   THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//  
//  (2) GPL Version 2
//   This program is free software; you can redistribute it and/or modify
//   it under the terms of the GNU General Public License as published by
//   the Free Software Foundation, version 2. This program is distributed
//   in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//   even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
//   PURPOSE. See the GNU General Public License for more details.
//  
//   You should have received a copy of the GNU General Public License along
//   with this program.If not, see<http:www.gnu.org/licenses/>.
//  
//  (3)CableLabs License
//   If you or the company you represent has a separate agreement with CableLabs
//   concerning the use of this code, your rights and obligations with respect
//   to this code shall be as set forth therein. No license is granted hereunder
//   for any other purpose.
//  
//   Please contact CableLabs if you need additional information or 
//   have any questions.
//  
//       CableLabs
//       858 Coal Creek Cir
//       Louisville, CO 80027-9750
//       303 661-9100
// COPYRIGHT_END
package org.cablelabs.impl.media.player;

import java.util.Vector;

import javax.media.Controller;
import javax.media.Player;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.media.ActiveFormatDescriptionChangedEvent;
import org.dvb.media.AspectRatioChangedEvent;
import org.dvb.media.DFCChangedEvent;
import org.dvb.media.VideoFormatControl;
import org.dvb.media.VideoFormatEvent;
import org.dvb.media.VideoFormatListener;
import org.dvb.media.VideoTransformation;

import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.JMFBaseInterfaceTest;
import org.cablelabs.impl.media.mpe.MediaAPI;

/**
 * VideoFormatControlTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class VideoFormatControlTest extends JMFBaseInterfaceTest
{

    private AVPlayer player;

    private VideoFormatControl control;

    private CannedVideoFormatListener listener;

    public static InterfaceTestSuite isuite(ImplFactory factory)
    {
        return new InterfaceTestSuite(VideoFormatControlTest.class, factory);
    }

    public VideoFormatControlTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
    }

    public VideoFormatControlTest(String name, ImplFactory factory)
    {
        this(name, Player.class, factory);
    }

    // Test setup

    public void setUp() throws Exception
    {
        super.setUp();

        player = (AVPlayer) createImplObject();
        control = (VideoFormatControl) player.getControl(VideoFormatControl.class.getName());
        listener = new CannedVideoFormatListener();

        startPlayer();
    }

    public void tearDown() throws Exception
    {
        control.removeVideoFormatListener(listener);
        player.close();
        listener = null;
        control = null;
        player = null;

        super.tearDown();
    }

    // Test Section

    public void testGetAspectRatio()
    {
        assertEquals("Returned aspect ratio is invalid", VideoFormatControl.ASPECT_RATIO_4_3, control.getAspectRatio());
        cma.cannedSetAspectRatio(player.getVideoDevice().getHandle(), VideoFormatControl.ASPECT_RATIO_16_9);
        assertEquals("Returned aspect ratio is invalid", VideoFormatControl.ASPECT_RATIO_16_9, control.getAspectRatio());

        // Clean up
        cma.cannedSetAspectRatio(player.getVideoDevice().getHandle(), VideoFormatControl.ASPECT_RATIO_4_3);
    }

    public void testGetActiveFormatDefinition()
    {
        assertEquals("Returned AFD is invalid", VideoFormatControl.AFD_4_3, control.getActiveFormatDefinition());
        cma.cannedSetAFD(player.getVideoDevice().getHandle(), VideoFormatControl.AFD_16_9);
        assertEquals("Returned AFD is invalid", VideoFormatControl.AFD_16_9, control.getActiveFormatDefinition());

        // clean up
        cma.cannedSetAFD(player.getVideoDevice().getHandle(), VideoFormatControl.AFD_4_3);
    }

    public void testGetDecoderFormatConversion()
    {
        assertEquals("Returned DFC is incorrect", VideoFormatControl.DFC_PROCESSING_NONE,
                control.getDecoderFormatConversion());
        cma.setDFC(player.getVideoDevice().getHandle(), VideoFormatControl.DFC_PROCESSING_FULL);
        assertEquals("Returned DFC is incorrect", VideoFormatControl.DFC_PROCESSING_FULL,
                control.getDecoderFormatConversion());

        // Clean up
        cma.setDFC(player.getVideoDevice().getHandle(), VideoFormatControl.DFC_PROCESSING_NONE);
    }

    public void testGetVideoFormatTransformation()
    {
        VideoTransformation vt = control.getVideoTransformation(VideoFormatControl.DFC_PLATFORM);
        assertNotNull("Returned VideoTransformation cannot be null", vt);
        // TODO: check values in VideoTransformation
    }

    public void testGetDisplayAspectRatio()
    {
        assertEquals("Returned display aspect ratio is invalid", VideoFormatControl.DAR_4_3,
                control.getDisplayAspectRatio());
    }

    public void testIsPlatform()
    {
        // This will always return false. The platform will never
        // be in control of the DFC
        assertFalse("isPlatform() should return false", control.isPlatform());
    }

    public void testAddAndRemoveVideoFormatListener()
    {
        control.addVideoFormatListener(listener);
        // Send the event
        EDListener l = cma.cannedGetEDListener(player.getVideoDevice());
        l.asyncEvent(MediaAPI.Event.ASPECT_RATIO_CHANGED, VideoFormatControl.ASPECT_RATIO_16_9, 0);
        listener.waitForEvents(1);
        assertEquals("Incorrect number of events received", 1, listener.events.size());
        assertTrue("Received event is not of expected type AspectRatioChangedEvent",
                listener.events.get(0) instanceof AspectRatioChangedEvent);

        control.removeVideoFormatListener(listener);
        listener.reset();
        l.asyncEvent(MediaAPI.Event.ASPECT_RATIO_CHANGED, VideoFormatControl.ASPECT_RATIO_16_9, 0);
        listener.waitForEvents(1);
        assertEquals("Incorrect number of events received", 0, listener.events.size());
    }

    public void testReceivedAFDCEvent()
    {
        control.addVideoFormatListener(listener);
        EDListener l = cma.cannedGetEDListener(player.getVideoDevice());
        cma.cannedSetAFD(player.getVideoDevice().getHandle(), VideoFormatControl.AFD_16_9);
        l.asyncEvent(MediaAPI.Event.ACTIVE_FORMAT_CHANGED, VideoFormatControl.AFD_16_9, 0);
        listener.waitForEvents(1);
        assertEquals("Incorrect number of events received", 1, listener.events.size());
        assertTrue("Received event is not of expected type ActiveFormatDescriptionChangedEvent" + ", instead is "
                + listener.events.get(0).getClass().getName(),
                listener.events.get(0) instanceof ActiveFormatDescriptionChangedEvent);
        ActiveFormatDescriptionChangedEvent event = (ActiveFormatDescriptionChangedEvent) listener.events.get(0);
        assertEquals("AFD in event is incorrect", VideoFormatControl.AFD_16_9, event.getNewFormat());
        assertEquals("AFD in control is incorrect", VideoFormatControl.AFD_16_9, control.getActiveFormatDefinition());

        // Clean up
        cma.cannedSetAFD(player.getVideoDevice().getHandle(), VideoFormatControl.AFD_4_3);
    }

    public void testReceivedARCEvent()
    {
        control.addVideoFormatListener(listener);
        EDListener l = cma.cannedGetEDListener(player.getVideoDevice());
        cma.cannedSetAspectRatio(player.getVideoDevice().getHandle(), VideoFormatControl.ASPECT_RATIO_16_9);
        l.asyncEvent(MediaAPI.Event.ASPECT_RATIO_CHANGED, VideoFormatControl.ASPECT_RATIO_16_9, 0);
        listener.waitForEvents(1);
        assertEquals("Incorrect number of events received", 1, listener.events.size());
        assertTrue("Received event is not of expected type AspectRatioChangedEvent" + ", instead is "
                + listener.events.get(0).getClass().getName(),
                listener.events.get(0) instanceof AspectRatioChangedEvent);
        AspectRatioChangedEvent event = (AspectRatioChangedEvent) listener.events.get(0);
        assertEquals("ARC in event is incorrect", VideoFormatControl.ASPECT_RATIO_16_9, event.getNewRatio());
        assertEquals("ARC in control is incorrect", VideoFormatControl.ASPECT_RATIO_16_9, control.getAspectRatio());

        // Clean up
        cma.cannedSetAspectRatio(player.getVideoDevice().getHandle(), VideoFormatControl.ASPECT_RATIO_4_3);
    }

    public void testRecievedDFCCEvent()
    {
        control.addVideoFormatListener(listener);
        EDListener l = cma.cannedGetEDListener(player.getVideoDevice());
        cma.cannedSetAspectRatio(player.getVideoDevice().getHandle(), VideoFormatControl.DFC_PROCESSING_FULL);
        l.asyncEvent(MediaAPI.Event.DFC_CHANGED, VideoFormatControl.DFC_PROCESSING_FULL, 0);
        listener.waitForEvents(1);
        assertEquals("Incorrect number of events received", 1, listener.events.size());
        assertTrue("Received event is not of expected type DFCChangedEvent" + ", instead is "
                + listener.events.get(0).getClass().getName(), listener.events.get(0) instanceof DFCChangedEvent);
        DFCChangedEvent event = (DFCChangedEvent) listener.events.get(0);
        assertEquals("DFC in event is incorrect", VideoFormatControl.DFC_PROCESSING_FULL, event.getNewDFC());
        assertEquals("DFC in control is incorrect", VideoFormatControl.DFC_PROCESSING_FULL, control.getAspectRatio());

        // clean up
        cma.setDFC(player.getVideoDevice().getHandle(), VideoFormatControl.DFC_PLATFORM);
    }

    // Helper section

    public void startPlayer()
    {
        CannedControllerListener l = new CannedControllerListener(1);
        player.addControllerListener(l);
        player.start();
        l.waitForMediaPresentationEvent();
        assertEquals("Player is not in Prefetched state", Controller.Started, player.getState());
    }

    private class CannedVideoFormatListener implements VideoFormatListener
    {

        private final static int WAIT_TIME = 8000;

        private Vector events = new Vector();

        public void receiveVideoFormatEvent(VideoFormatEvent anEvent)
        {
            events.add(anEvent);
        }

        public void waitForEvents(int count)
        {
            long startTime = System.currentTimeMillis();

            //
            // wait until we get the correct number of events or until
            // we get tired of waiting
            // 
            while (events.size() < count && System.currentTimeMillis() < startTime + WAIT_TIME)
            {
                Thread.yield();
                synchronized (events)
                {
                    try
                    {
                        events.wait(WAIT_TIME / 20);
                    }
                    catch (InterruptedException exc)
                    {
                        //
                        // if we got interrupted, break out of the loop and
                        // return
                        //
                        exc.printStackTrace();
                        return;
                    }// end catch
                }// end synch
            }// end while
        }// end waitForEvents()

        public void reset()
        {
            events.removeAllElements();
        }
    }
}
