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
import javax.media.ControllerEvent;
import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIChangeType;
import javax.tv.service.Service;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.media.JMFFactory;
import org.cablelabs.impl.media.mpe.CannedMediaAPI;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.media.protocol.ocap.DataSource;
import org.cablelabs.impl.media.source.ServiceDataSource;
import org.cablelabs.impl.ocap.si.ByteParser;
import org.cablelabs.impl.ocap.si.ProgramMapTableImpl;
import org.cablelabs.impl.service.ProgramMapTableHandle;
import org.ocap.net.OcapLocator;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.TableChangeListener;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

/**
 * BroadcastPlayerTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class BroadcastPlayerTest extends TestCase
{
    private CannedMediaAPI cannedMediaAPI;

    private CannedSIDatabase sidb;

    private JMFFactory playerFactory;

    private BroadcastServicePlayer player;

    private CannedControllerListener listener;

    private PlayerHelper helper;

    /**
	 * 
	 */
    public BroadcastPlayerTest()
    {

    }

    /**
     * @param name
     */
    public BroadcastPlayerTest(String name)
    {
        super(name);
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        TestRunner.run(suite());
        System.exit(0);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(BroadcastPlayerTest.class);
        ImplFactory factory = new BroadcastPlayerFactory();

        // Add the ServicePlayerInterfaceTest
        InterfaceTestSuite[] testSuites = new InterfaceTestSuite[] { PlayerInterfaceTest.isuite(factory),
                ServicePlayerInterfaceTest.isuite(factory), AVPlayerInterfaceTest.isuite(factory),
                AudioLanguageControlTest.isuite(factory), AWTVideoSizeControlTest.isuite(factory),
                BackgroundVideoPresentationControlTest.isuite(factory), ClosedCaptioningControlTest.isuite(factory),
                DVBMediaSelectControlTest.isuite(factory), FreezeControlTest.isuite(factory),
                VideoComponentControlTest.isuite(factory), VideoFormatControlTest.isuite(factory),
                VideoPresentationControlTest.isuite(factory) };

        for (int i = 0; i < testSuites.length; i++)
        {
            suite.addTest(testSuites[i]);
        }

        return suite;
    }

    public void setUp() throws Exception
    {
        super.setUp();
        playerFactory = new BroadcastPlayerFactory();
        playerFactory.setUp();
        sidb = playerFactory.getCannedSIDB();

        player = (BroadcastServicePlayer) playerFactory.createImplObject();
        listener = new CannedControllerListener(1);
        player.addControllerListener(listener);
        helper = new PlayerHelper(player, listener);

        cannedMediaAPI = (CannedMediaAPI) ManagerManager.getInstance(MediaAPIManager.class);
    }

    public void tearDown() throws Exception
    {
        player.close();
        playerFactory.tearDown();

        helper = null;
        listener = null;
        player = null;
        playerFactory = null;
        cannedMediaAPI = null;
        sidb = null;
        super.tearDown();
    }

    // Test Section

    private void nativeEventReceivedTestTemplate(int eventCode, Class expectedEventClass)
    {
        nativeEventReceivedTestTemplate(eventCode, 0, 0, expectedEventClass);
    }

    private void nativeEventReceivedTestTemplate(int eventCode, int data1, int data2, Class expectedEventClass)
    {
        if (player.getState() == Controller.Unrealized)
        {
            helper.prefetchPlayer();
            helper.callSyncStartWithNoWait();
            //
            // MediaPresented Event and MediaPresentationEvent
            //
            listener.waitForEvents(3);
        }
        CannedMediaAPI mediaAPI = (CannedMediaAPI) player.getMediaAPI();
        EDListener nativeListener = mediaAPI.cannedGetEDListener(player.getVideoDevice());
        assertNotNull("Native listener should not be null for video device " + player.getVideoDevice().getHandle(),
                nativeListener);

        listener.reset();
        nativeListener.asyncEvent(eventCode, data1, data2);
        listener.waitForEvents(1);
        assertTrue("Did not receive an event on a native event code of " + eventCode, listener.events.size() > 0);
        ControllerEvent event = listener.getEvent(0);
        assertTrue("Expected a subclass of " + expectedEventClass.getName() + " but instead got "
                + event.getClass().getName(), expectedEventClass.isAssignableFrom(event.getClass()));
    }

    /*
     * public void testGetDurationBroadcast() {
     * assertEquals("Returned Time is incorrect", Duration.DURATION_UNBOUNDED,
     * player.getDuration()); }
     * 
     * public void testContentPresentingWhileWaitingForContentPresenting()
     * throws Exception { helper.prefetchPlayer();
     * assertTrue(listener.waitForPrefetchCompleteEvent()); CannedMediaAPI
     * mediaAPI = (CannedMediaAPI)player.getMediaAPI(); boolean
     * deliverContentPresenting =
     * mediaAPI.cannedGetDeliverContentPresentingEvent(); try {
     * mediaAPI.cannedSetDeliverContentPresentingEvent(false);
     * 
     * player.syncStart(new Time(0));
     * assertFalse(listener.waitForMediaPresentationEvent());
     * 
     * EDListener nativeListener =
     * mediaAPI.cannedGetEDListener(player.getVideoDevice());
     * assertNotNull("EDListener should not be null", nativeListener); int data1
     * = 0; int data2 = 0;
     * 
     * listener.reset();
     * 
     * nativeListener.asyncEvent(MediaAPI.Event.CONTENT_PRESENTING, data1,
     * data2); listener.waitForEvents(1); assertTrue(listener.events.size() >
     * 0); ControllerEvent event = listener.getEvent(0);
     * assertTrue("Expected a subclass of MediaPresentedEvent" +
     * " but instead got " + event.getClass().getName(),
     * MediaPresentedEvent.class.isAssignableFrom(event.getClass())); } finally
     * {
     * mediaAPI.cannedSetDeliverContentPresentingEvent(deliverContentPresenting
     * ); } }
     */

    /*
     * The validity of this test is currently in question. Disabling for now.
     */
    public void xtestContentPresentingWhileNotWaitingForContentPresenting() throws Exception
    {
        helper.prefetchPlayer();
        helper.callSyncStartWithNoWait();
        //
        // MediaPresented Event and MediaPresentationEvent
        //
        listener.waitForEvents(3);

        CannedMediaAPI mediaAPI = (CannedMediaAPI) player.getMediaAPI();
        EDListener nativeListener = mediaAPI.cannedGetEDListener(player.getVideoDevice());
        assertNotNull("EDListener should not be null", nativeListener);
        int data1 = 0;
        int data2 = 0;

        listener.reset();

        //
        // at this point, the player has already received notification that
        // the content is presenting. This call should not result in another
        // event
        //
        nativeListener.asyncEvent(MediaAPI.Event.CONTENT_PRESENTING, data1, data2);
        listener.waitForEvents(1);
        assertTrue("Received unexpected events: " + listener.events, listener.events.size() == 0);
    }

    /*
     * public void testCAFailureStream() { int data1 =
     * sidb.jmfServiceComponent1A1.getPID(); int data2 = 0;
     * cannedMediaAPI.cannedSetDeliverContentPresentingEvent(false);
     * nativeEventReceivedTestTemplate
     * (MediaAPI.Event.STREAM_CA_DENIED_ENTITLEMENT, data1, data2,
     * PresentationChangedEvent.class);
     * 
     * nativeEventReceivedTestTemplate(MediaAPI.Event.CONTENT_PRESENTING,
     * MediaPresentedEvent.class); listener.waitForEvents(2);
     * assertTrue(listener.getEvent(1) instanceof
     * AlternativeMediaPresentationEvent); } public void testCAReturnedStream()
     * { int data1 = sidb.jmfServiceComponent1A1.getPID(); int data2 = 0;
     * cannedMediaAPI.cannedSetDeliverContentPresentingEvent(false);
     * nativeEventReceivedTestTemplate
     * (MediaAPI.Event.STREAM_CA_DENIED_ENTITLEMENT, data1, data2,
     * PresentationChangedEvent.class);
     * 
     * nativeEventReceivedTestTemplate(MediaAPI.Event.STREAM_RETURNED, data1,
     * data2, PresentationChangedEvent.class);
     * 
     * // // one stream was denied, then returned, we should get the normal //
     * media presentation event //
     * nativeEventReceivedTestTemplate(MediaAPI.Event.CONTENT_PRESENTING,
     * MediaPresentedEvent.class); listener.waitForEvents(2);
     * assertTrue(listener.getEvent(1) instanceof NormalMediaPresentationEvent);
     * }
     * 
     * 
     * public void testStreamUnavailable() { int data1 =
     * sidb.jmfServiceComponent1A1.getPID(); int data2 = 0;
     * cannedMediaAPI.cannedSetDeliverContentPresentingEvent(false);
     * nativeEventReceivedTestTemplate(MediaAPI.Event.STREAM_NO_DATA, data1,
     * data2, PresentationChangedEvent.class);
     * nativeEventReceivedTestTemplate(MediaAPI.Event.CONTENT_PRESENTING,
     * MediaPresentedEvent.class); listener.waitForEvents(2);
     * assertTrue(listener.getEvent(1) instanceof
     * AlternativeMediaPresentationEvent);
     * 
     * }
     * 
     * // TODO(Todd): Tests for MediaAPI.Event.CA_UNKNOWN // TODO(Todd): Tests
     * for MediaAPI.Event.DIALOG_PAYMENT // TODO(Todd): Tests for
     * MediaAPI.Event.DIALOG_TECHNICAL // TODO(Todd): Tests for
     * MediaAPI.Event.DIALOG_RATING // TODO(Todd): Tests for
     * MediaAPI.Event.HW_UNAVAILABLE
     * 
     * 
     * public void testServiceDetailsRemoveSelectedService() { player.start();
     * listener.waitForMediaPresentationEvent(); listener.reset();
     * 
     * // // since this is currently selected, it should cause a retune //
     * sidb.cannedRemoveSI(sidb.jmfServiceDetails1); boolean received =
     * listener.waitForStopEvent();
     * assertTrue("Did not receive a stop event when details removed " +
     * "from si database", received); }
     * 
     * public void testServiceDetailsRemoveNotSelectedService() {
     * player.start(); listener.waitForMediaPresentationEvent();
     * listener.reset();
     * 
     * // // since this is currently selected, it should cause a retune //
     * sidb.cannedRemoveSI(sidb.jmfServiceDetails2); boolean received =
     * listener.waitForStopEvent();
     * assertTrue("Received a stop event when details removed " +
     * "from si database for an unselected service", !received); }
     */
    private void basePMTChangeTest(SIChangeType changeType)
    {
        player.start();
        listener.waitForMediaPresentationEvent();
        listener.reset();

        Service svc = ((ServiceDataSource) player.getSource()).getService();
        ProgramMapTable pmt = new ProgramMapTableImpl(buildPMTData((OcapLocator) svc.getLocator()),
                sidb.transportStream10, new CannedProgramMapTableHandle());
        SIChangeEvent event = new SIChangeEvent(this, changeType, pmt)
        {
        };
        Vector listeners = sidb.cannedGetPMTChangeListeners();
        for (int i = 0; i < listeners.size(); i++)
        {
            TableChangeListener l = (TableChangeListener) listeners.get(i);
            l.notifyChange(event);
        }

    }

    // Disabled per bug 4918
    // public void testPMTChangeAdd()
    // {
    // System.out.println("\n\ntestPMTChangeAdd\n\n");
    // basePMTChangeTest(SIChangeType.ADD);
    // boolean received = listener.waitForMediaPresentationEvent();
    // assertTrue("Did not receive a media presentation event after a pmt add",
    // received);
    // }

    // Disabled per bug 4918
    // public void testPMTChangeRemove()
    // {
    // basePMTChangeTest(SIChangeType.REMOVE);
    // boolean received = listener.waitForStopEvent();
    // assertTrue("Did not receive a stop event after a PMT removal", received);
    // }

    // Disabled per bug 4918
    // public void testPMTChangeModify()
    // {
    // basePMTChangeTest(SIChangeType.MODIFY);
    // boolean received = listener.waitForMediaPresentationEvent();
    // assertTrue("Did not receive a media presentation event after a pmt change",
    // received);
    // }

    private static class BroadcastPlayerFactory extends JMFFactory
    {

        public Object createImplObject() throws Exception
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

            BroadcastServicePlayer player = new CannedBroadcastPlayer(ccm.getCurrentContext(), null);
            DataSource ds = new DataSource();
            ds.setService(getCannedSIDB().jmfService1);
            player.setSource(ds);

            return player;
        }

    }

    private static class CannedProgramMapTableHandle implements ProgramMapTableHandle
    {
        public int getHandle()
        {
            return 0;
        }
    }

    private void writeIntToBytes(byte[] arr, int offset, int data)
    {
        int d = data;
        if (ByteParser.isBigEndian())
        {
            for (int i = 3; i >= 0; i--)
            {
                arr[offset + i] = (byte) (d & 0xff);
                d >>= 8;
            }
        }
        else
        {
            for (int i = 0; i < 4; i++)
            {
                arr[offset + i] = (byte) (d & 0xff);
                d >>= 8;
            }
        }
    }

    private byte[] buildPMTData(OcapLocator lctr)
    {
        byte[] pmt = new byte[36];
        writeIntToBytes(pmt, 0, 0);
        writeIntToBytes(pmt, 4, 0);
        writeIntToBytes(pmt, 8, lctr.getFrequency());
        writeIntToBytes(pmt, 12, lctr.getSourceID());
        writeIntToBytes(pmt, 16, lctr.getProgramNumber());
        writeIntToBytes(pmt, 20, 0);
        writeIntToBytes(pmt, 24, 0);
        writeIntToBytes(pmt, 28, 0);

        return pmt;
    }

}
