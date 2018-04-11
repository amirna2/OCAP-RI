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

import javax.media.Time;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceDetails;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.DVRAPIManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.recording.CannedRecordedServiceImpl;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedSIDatabase.ServiceDetailsHandleImpl;
import org.cablelabs.impl.media.JMFFactory;
import org.cablelabs.impl.media.JMFTests;
import org.cablelabs.impl.media.mpe.CannedDVRAPI;
import org.cablelabs.impl.media.protocol.recording.DataSource;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.javatv.navigation.ServiceDetailsImpl;
import org.cablelabs.impl.util.string.MultiString;
import org.davic.media.MediaPresentedEvent;
import org.ocap.media.CannedMediaAccessHandler;
import org.ocap.media.MediaAccessHandlerRegistrar;
import org.ocap.media.MediaPresentationEvent;
import org.ocap.shared.media.LeavingLiveModeEvent;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

/**
 * RecordingPlayerTest
 * 
 * @author Joshua Keplinger
 * 
 */
public class RecordingPlayerTest extends TestCase
{
    private static final long RECORDING_LENGTH_MS = 6000;

    private RecordingPlayerFactory factory;

    private RecordedServicePlayer player;

    private CannedControllerListener listener;

    private PlayerHelper helper;

    MediaAccessHandlerRegistrar registrar;

    /**
     *
     */
    public RecordingPlayerTest()
    {
        super();
        // TODO (Josh) Implement
    }

    /**
     * @param name
     */
    public RecordingPlayerTest(String name)
    {
        super(name);
        // TODO (Josh) Implement
    }

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(RecordingPlayerTest.class);

        suite.setName("RecordingPlayerTest");

        ImplFactory factory = new RecordingPlayerFactory();

        // Add the ServicePlayerInterfaceTest
        InterfaceTestSuite[] testSuites = new InterfaceTestSuite[] { ServicePlayerInterfaceTest.isuite(factory),
                AVPlayerInterfaceTest.isuite(factory),
                PlayerInterfaceTest.isuite(factory),
                PlayerStopTimeTest.isuite(factory),

                // AudioLanguageControlTest.isuite(factory),
                AWTVideoSizeControlTest.isuite(factory), BackgroundVideoPresentationControlTest.isuite(factory),
                ClosedCaptioningControlTest.isuite(factory), FreezeControlTest.isuite(factory),
                VideoComponentControlTest.isuite(factory),
                // VideoFormatControlTest.isuite(factory),
                // not finished VideoPresentationControlTest.isuite(factory)

                MediaTimeEventControlTest.isuite(factory), };

        for (int i = 0; i < testSuites.length; i++)
        {
            suite.addTest(testSuites[i]);
        }

        return suite;
    }

    public void setUp() throws Exception
    {
        super.setUp();
        factory = new RecordingPlayerFactory();
        factory.setUp();

        player = (RecordedServicePlayer) factory.createImplObject();
        listener = new CannedControllerListener(1);

        player.addControllerListener(listener);
        registrar = MediaAccessHandlerRegistrar.getInstance();
        helper = new PlayerHelper(player, listener);
    }

    public void tearDown() throws Exception
    {
        player.removeControllerListener(listener);
        player.close();
        factory.tearDown();

        listener = null;
        player = null;
        factory = null;
        helper = null;
        registrar.registerMediaAccessHandler(null);
        super.tearDown();
    }

    private void baseSetRateTest(float newRate)
    {
        helper.startPlayer();
        listener.waitForMediaPresentationEvent();

        listener.reset();
        player.setRate(newRate);
        listener.waitForEvents(1);

    }

    public void testSetRateNegative()
    {
        baseSetRateTest(-1f);
        assertTrue(player.getRate() < 0);
    }

    public void testSetRateZero()
    {
        float newRate = 0f;
        baseSetRateTest(newRate);
        assertTrue(player.getRate() == newRate);
    }

    public void testSetRateZeroBackToOne()
    {
        assertTrue(player.getRate() == 1.0f);

        float newRate = 0f;
        baseSetRateTest(newRate);
        assertTrue(player.getRate() == newRate);

        player.setRate(1.0f);
        listener.waitForEvents(1);
        assertTrue(player.getRate() == 1.0f);
    }

    public void xtestGetLiveMediaTime()
    {
        helper.realizePlayer();
        // assertEquals("Returned value is incorrect", RECORDING_LENGTH_MS *
        // 1000,
        // player.getLiveMediaTime().getNanoseconds());
    }

    public void testStartPlayerEvents()
    {
        helper.startPlayer();
        assertTrue(!player.isStopped());
        assertTrue(listener.events.toString(), listener.events.size() >= 8);
        assertTrue(listener.events.get(5) instanceof LeavingLiveModeEvent);
        assertTrue(listener.events.get(6) instanceof MediaPresentedEvent);
        assertTrue(listener.events.get(7) instanceof MediaPresentationEvent);
    }

    /*
     * FIXME This test currently fails because the RecordedServicePlayer will
     * not get default components for a RecordedService.
     */
    public void xtestOnlyAuthorizedComponentsDecoded()
    {
        CannedMediaAccessHandler handler = new CannedMediaAccessHandler();
        registrar.registerMediaAccessHandler(handler);

        // TODO: commented out for ECN 972 integration
        // handler.addAcceptPID(factory.getCannedSIDB().serviceComponent72.getPID());

        helper.startPlayer();
        //
        // wait for an extra event that won't be delivered --
        // this is to make sure that the decoding has been done
        //
        listener.waitForEvents(listener.events.size() + 1);
        CannedDVRAPI dvrAPI = (CannedDVRAPI) player.getDVRAPI();
        int[] pids = dvrAPI.cannedGetDecodeRecordingPIDs();
        assertTrue(pids.length == 1);
        assertTrue(pids[0] == factory.getCannedSIDB().serviceComponent72.getPID());
    }

    /**
     * This test simulates stepping that is done in SWAK. It starts the player,
     * plays a little while, pauses the player by setting the rate to zero, and
     * then attempts to set the media time to a previous value.
     */
    public void testSetMediaTimeInRecordedWhileRateZero() throws InterruptedException
    {
        helper.startPlayer();
        Thread.sleep(1000);

        player.setRate(0.0f);
        assertTrue(listener.waitForRateChangeEvent());

        Time currentTime = player.getMediaTime();
        Time nextTime = new Time(currentTime.getSeconds() / 2.0);
        // nextTime = new Time(0);

        assertTrue("Test should be stepping to a recorded time", !factory.ds.isLiveMediaTime(nextTime));
        player.setMediaTime(nextTime);
        assertTrue(listener.waitForMediaTimeSetEvent());

        Time testTime = player.getMediaTime();
        assertTrue("setMediaTime, time set to " + nextTime.getNanoseconds() + " but got back "
                + testTime.getNanoseconds(), testTime.getNanoseconds() == nextTime.getNanoseconds());
    }

    /**
     * Start a recorded service, then switch to live mode
     */
    public void testSetMediaTimeSwitchToLive() throws InterruptedException
    {
        helper.startPlayer();
        Thread.sleep(1000);

        Time liveTime = factory.ds.getLiveMediaTime();
        player.setMediaTime(liveTime);
        assertTrue(listener.waitForMediaTimeSetEvent());

        Time currentTime = player.getMediaTime();
        assertTrue("Switching to live time on a player did not work", factory.ds.isLiveMediaTime(currentTime));

    }

    private static class RecordingPlayerFactory extends JMFFactory
    {

        private DVRAPIManager oldDVR;

        private ServiceContextExt sce;

        private DataSource ds;

        public void setUp() throws Exception
        {
            super.setUp();
            oldDVR = (DVRAPIManager) ManagerManager.getInstance(DVRAPIManager.class);
            CannedDVRAPI cda = (CannedDVRAPI) CannedDVRAPI.getInstance();
            ManagerManagerTest.updateManager(DVRAPIManager.class, cda.getClass(), true, cda);

        }

        public void tearDown() throws Exception
        {
            if (sce != null)
            {
                sce.destroy();
            }
            ManagerManagerTest.updateManager(DVRAPIManager.class, oldDVR.getClass(), true, oldDVR);
            super.tearDown();
        }

        public Object createImplObject() throws Exception
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

            RecordedServicePlayer player = new CannedRecordingPlayer(ccm.getCurrentContext());
            ds = new DataSource();

            CannedRecordedServiceImpl service = new CannedRecordedServiceImpl();

            CannedSIDatabase sidb = getCannedSIDB();
            ServiceDetails sd = new ServiceDetailsImpl(JMFTests.csm.getSICache(), new ServiceDetailsHandleImpl(1110),
                    1100, 101, sidb.transportStream7, new MultiString(new String[] { "eng" },
                            new String[] { "JMF ServiceDetails 1" }), service, DeliverySystemType.CABLE,
                    ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() - 1000),
                    new int[] {}, true, 0x1FFF, null);

            service.cannedSetServiceDetails(sd);
            service.setMediaTime(new Time(0));
            // duration of 1 hour
            service.cannedSetRecordedDuration(60 * 60 * 1000);
            ds.setService(service);

            player.setSource(ds);
            ds.setService(service);

            ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
            sce = (ServiceContextExt) sm.getServiceContextFactory().createServiceContext();

            ServiceDetailsExt sdx = Util.getServiceDetails(service);
            player.setInitialSelection(sdx, null);

            return player;
        }

    }

}
