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

import javax.media.Controller;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.media.StopByResourceLossEvent;
import org.havi.ui.HVideoComponent;
import org.havi.ui.HVideoDevice;
import org.havi.ui.event.HScreenLocationModifiedEvent;
import org.havi.ui.event.HScreenLocationModifiedListener;
import org.ocap.media.VideoComponentControl;

import org.cablelabs.impl.media.CannedHScreen;
import org.cablelabs.impl.media.CannedVideoDevice;
import org.cablelabs.impl.media.JMFBaseInterfaceTest;

/**
 * AVPlayerInterfaceTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class AVPlayerInterfaceTest extends JMFBaseInterfaceTest
{

    private AVPlayer avplayer;

    private CannedControllerListener listener;

    private PlayerHelper helper;

    /**
     * @param arg0
     * @param arg1
     * @param arg2
     */
    public AVPlayerInterfaceTest(String name, Class clazz, ImplFactory factory)
    {
        super(name, clazz, factory);
    }

    public AVPlayerInterfaceTest(String name, ImplFactory factory)
    {
        this(name, AVPlayer.class, factory);
    }

    public static InterfaceTestSuite isuite(ImplFactory factory)
    {
        return new InterfaceTestSuite(AVPlayerInterfaceTest.class, factory);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        avplayer = (AVPlayer) createImplObject();
        listener = new CannedControllerListener(1);
        avplayer.addControllerListener(listener);
        helper = new PlayerHelper(avplayer, listener);
    }

    public void tearDown() throws Exception
    {
        ((Player) avplayer).close();

        listener = null;
        helper = null;
        avplayer = null;

        super.tearDown();
    }

    // Test Section

    public void testGetAndSetVideoDevice() throws Exception
    {
        helper.prefetchPlayer();
        assertNotNull("VideoDevice should not be null", avplayer.getVideoDevice());
        CannedVideoDevice cvd = (CannedVideoDevice) CannedHScreen.getInstance().getHVideoDevices()[1];
        listener.reset();
        avplayer.setVideoDevice(cvd);
        assertEquals("Returned VideoDevice is incorrect", cvd, avplayer.getVideoDevice());
    }

    public void testLoseVideoDeviceControl() throws Exception
    {
        helper.prefetchPlayer();
        assertNotNull("VideoDevice should not be null", avplayer.getVideoDevice());
        avplayer.start();
        listener.waitForMediaPresentationEvent();
        assertTrue("Event count is incorrect", 7 <= listener.events.size());
        // assertEquals("Player should be in the started state",
        // Controller.Started,
        // player.getState());
        listener.reset();
        avplayer.loseVideoDeviceControl();
        listener.waitForStopEvent();
        StopByResourceLossEvent event = (StopByResourceLossEvent) listener.findEventOfClass(StopByResourceLossEvent.class);
        assertEquals("Player should be in the realized state", Controller.Realized, avplayer.getState());
        assertTrue("Event should be StopByResourceLostEvent - ", event != null);
    }

    public void xtestStartWithNonContributingVideoDevice() throws Exception
    {
        avplayer.prefetch();
        listener.waitForEvents(4);

        CannedVideoDevice cvd = (CannedVideoDevice) CannedHScreen.getInstance().getHVideoDevices()[1];
        avplayer.setVideoDevice(cvd);
        cvd.setControlVideoDevice(true);
        cvd.setVideoConfiguration(HVideoDevice.NOT_CONTRIBUTING);
        avplayer.start();
        listener.waitForEvents(6);
        assertTrue("Event count is incorrect", 6 <= listener.events.size());
        assertTrue(listener.getEvent(5) instanceof StopByResourceLossEvent);
    }

    public void testVideoComponentVideoDevice()
    {
        helper.prefetchPlayer();
        VideoComponentControl vidCompControl = (VideoComponentControl) avplayer.getControl("org.ocap.media.VideoComponentControl");
        assertNotNull("Control should not be null", vidCompControl);
        HVideoComponent vc = vidCompControl.getVisualComponent();
        assertNotNull("VideoComponent should not be null", vc);
        assertTrue(vc.getVideoDevice().equals(avplayer.getVideoDevice()));
    }

    public void testRemoveScreenLocationModifiedListenerNotAdded()
    {
        helper.realizePlayer();
        MyScreenLocationModifiedListener l = new MyScreenLocationModifiedListener();
        VideoComponentControl vidCompControl = (VideoComponentControl) avplayer.getControl("org.ocap.media.VideoComponentControl");
        HVideoComponent vc = vidCompControl.getVisualComponent();
        vc.removeOnScreenLocationModifiedListener(l);
        //
        // no assertion, just verify we don't get an exception
        //
    }

    public void xtestIsComponentPlayer()
    {
        assertFalse("Player should not be component player", avplayer.isComponentPlayer());
        VideoComponentControl control = (VideoComponentControl) avplayer.getControl("org.ocap.media.VideoComponentControl");

        assertTrue("Player should be component player", avplayer.isComponentPlayer());
    }

    // Test support

    private static class MyScreenLocationModifiedListener implements HScreenLocationModifiedListener
    {
        boolean reportCalled = false;

        public void report(HScreenLocationModifiedEvent gce)
        {
            reportCalled = true;
        }

    }
}
