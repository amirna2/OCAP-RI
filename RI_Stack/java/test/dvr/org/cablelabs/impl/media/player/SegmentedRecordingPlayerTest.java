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

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.DVRAPIManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.recording.CannedRecordedServiceImpl;
import org.cablelabs.impl.manager.service.CannedSIDatabase;
import org.cablelabs.impl.manager.service.CannedSIDatabase.ServiceDetailsHandleImpl;
import org.cablelabs.impl.media.CannedHScreen;
import org.cablelabs.impl.media.JMFFactory;
import org.cablelabs.impl.media.JMFTests;
import org.cablelabs.impl.media.mpe.CannedDVRAPI;
import org.cablelabs.impl.media.mpe.DVRAPI.Event;
import org.cablelabs.impl.media.protocol.segrecsvc.DataSource;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.javatv.navigation.ServiceDetailsImpl;
import org.cablelabs.impl.util.TimeTable;
import org.cablelabs.impl.util.string.MultiString;
import org.havi.ui.HScreen;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.SegmentedRecordedService;
import org.ocap.shared.media.EndOfContentEvent;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

public class SegmentedRecordingPlayerTest extends TestCase
{
    private SegmentedRecordingPlayerFactory factory;

    private CannedSIDatabase sidb;

    private SegmentedRecordedServicePlayer player;

    private DataSource dataSource;

    private CannedSegmentedRecordedService service;

    private DVRCannedControllerListener listener;

    private PlayerHelper helper;

    private CannedDVRAPI cannedDVRAPI;

    public class DVRCannedControllerListener extends CannedControllerListener
    {
        public DVRCannedControllerListener(int id)
        {
            super(id);
        }

        public boolean waitForEndOfContentEvent()
        {
            return waitForEvent(EndOfContentEvent.class, WAIT_TIME);
        }

    }

    public SegmentedRecordingPlayerTest()
    {
        super();
    }

    public SegmentedRecordingPlayerTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
        System.exit(0);
    }

    public static TestSuite suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(SegmentedRecordingPlayerTest.class);

        suite.setName("RecordingPlayerTest");

        ImplFactory factory = new SegmentedRecordingPlayerFactory();

        // Add the ServicePlayerInterfaceTest
        InterfaceTestSuite[] testSuites = new InterfaceTestSuite[] {
                ServicePlayerInterfaceTest.isuite(factory),
                // AVPlayerInterfaceTest.isuite(factory),
                PlayerInterfaceTest.isuite(factory),
                // AudioLanguageControlTest.isuite(factory),
                AWTVideoSizeControlTest.isuite(factory), BackgroundVideoPresentationControlTest.isuite(factory),
                ClosedCaptioningControlTest.isuite(factory), FreezeControlTest.isuite(factory),
                VideoComponentControlTest.isuite(factory),
                // VideoFormatControlTest.isuite(factory),
                // not finished VideoPresentationControlTest.isuite(factory)
                //
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
        factory = new SegmentedRecordingPlayerFactory();
        factory.setUp();
        sidb = factory.getCannedSIDB();

        player = (SegmentedRecordedServicePlayer) factory.createImplObject();
        dataSource = (DataSource) player.getSource();
        service = (CannedSegmentedRecordedService) dataSource.getService();

        listener = new DVRCannedControllerListener(1);

        player.addControllerListener(listener);
        helper = new PlayerHelper(player, listener);
        cannedDVRAPI = (CannedDVRAPI) ManagerManager.getInstance(DVRAPIManager.class);
    }

    public void tearDown() throws Exception
    {
        player.removeControllerListener(listener);
        player.close();
        factory.tearDown();

        listener = null;
        player = null;
        sidb = null;
        factory = null;
        helper = null;
        super.tearDown();
    }

    /**
     * 2.4.1 Playback of Segmented Recorded service with multiple segments
     */
    public void testSegmentedRecordedServiceWithMultipleSegments()
    {
        baseTestSegmentTransitionsWithRatePositive(1.0f);
    }

    protected void baseTestSegmentTransitionsWithRatePositive(float rate)
    {
        RecordedService[] segments = service.getSegments();
        // Start playback in Segmented Recorded Service
        helper.startPlayer();
        listener.waitForMediaPresentationEvent();

        player.setRate(rate);
        listener.waitForRateChangeEvent();
        assertTrue("Could not set the initial rate of " + rate, player.getRate() == rate);

        assertEquals(segments[0].getName(), cannedDVRAPI.cannedGetLastDecodedRecording());

        for (int i = 1; i < segments.length; i++)
        {
            listener.reset();
            cannedDVRAPI.cannedGetLastDecodeRecordingListener().asyncEvent(Event.END_OF_FILE, 0, 0);
            listener.waitForMediaPresentationEvent();
            assertEquals(segments[i].getName(), cannedDVRAPI.cannedGetLastDecodedRecording());

            // Verify rate and that presentation of recorded service is not
            // terminated.
            assertTrue("Player rate is not " + rate + ": " + player.getRate(), player.getRate() == rate);
            assertTrue("Player is not playing", player.isPresenting());
        }

        // at this point, we should be playing the last segment, an
        // end of file event should cause the player to generate
        // and end of content event
        cannedDVRAPI.cannedGetLastDecodeRecordingListener().asyncEvent(Event.END_OF_FILE, 0, 0);
        listener.waitForEndOfContentEvent();

    }

    /**
     * setMediaTime control works to set start point of Segmented Recorded
     * Service
     */
    public void testSetMediaTimeInRecordedService() throws Exception
    {
        RecordedService[] segments = service.getSegments();
        Time[] segmentTimes = service.getSegmentMediaTimes();

        // Set media time of recorded service 10 milliseconds prior to end of
        // first segment

        helper.realizePlayer();

        //
        // Per Mike S., the initial media time is taken from the data source and
        // and previously set value for media time will be ignored. Once the
        // has been started the first time and stopped, the media time can
        // be set by calling setMediaTime as expected
        //
        helper.startPlayer();
        listener.waitForMediaPresentationEvent();
        helper.stopPlayer();
        listener.waitForStopEvent();

        //
        // set the time to the middle of the segment and start the player
        // verify that the right segment was played
        //
        for (int i = 0; i < segments.length; i++)
        {
            listener.reset();

            long nanos = segmentTimes[i].getNanoseconds() + 10000;
            if (i > 0 && i < segmentTimes.length - 1)
            {
                assertTrue("Could not find a correct starting time", nanos > segmentTimes[i].getNanoseconds()
                        && nanos < segmentTimes[i + 1].getNanoseconds());
            }
            if (i == segmentTimes.length - 1)
            {
                assertTrue("Could not find a correct starting time", nanos > segmentTimes[i].getNanoseconds());
            }
            else if (i == 0)
            {
                assertTrue("Could not find a correct starting time for i = 0: " + nanos, nanos > 0
                        && nanos < segmentTimes[i + 1].getNanoseconds());
            }

            Time mt = new Time(nanos);

            service.setMediaTime(mt);
            // Payback Segmented Recorded Service and get Media Time, verify
            // that
            // it is some time prior to the second segment but after media time
            // set
            // on recorded service
            player.start();
            listener.waitForMediaPresentationEvent();
            // Stop playback
            player.stop();
            listener.waitForStopEvent();

            Time stopTime = player.getMediaTime();
            if (i + 1 < segmentTimes.length)
            {
                assertTrue("Stopped at " + stopTime.getNanoseconds() + ", expected to stop before "
                        + segmentTimes[i + 1].getNanoseconds(),
                        stopTime.getNanoseconds() < segmentTimes[i + 1].getNanoseconds());
            }
            assertTrue("Stopped at " + stopTime.getNanoseconds() + ", expected to stop after "
                    + segmentTimes[i].getNanoseconds(), stopTime.getNanoseconds() > segmentTimes[i].getNanoseconds());

            assertEquals("Stopped in segment " + cannedDVRAPI.cannedGetLastDecodedRecording()
                    + "but expected to stop in " + segments[i].getName(), segments[i].getName(),
                    cannedDVRAPI.cannedGetLastDecodedRecording());
        }
    }

    /**
     * No undesirable effects if rate < 0 is set while jumping between segments
     */
    // crashes the sim
    // public void testSegmentTransitionsNegativeRate()
    // {
    // baseTestSegmentTransitionsWithRateNegative(-1.0f);
    // }

    protected void baseTestSegmentTransitionsWithRateNegative(float rate)
    {
        RecordedService[] segments = service.getSegments();
        Time[] segmentTimes = service.getSegmentMediaTimes();

        // Playback Segmented Recorded service
        helper.startPlayer();

        Time startOfLastSegment = segmentTimes[segmentTimes.length - 1];
        long desiredStartTimeNanos = startOfLastSegment.getNanoseconds() + 10000;
        player.setMediaTime(new Time(desiredStartTimeNanos));
        listener.waitForMediaTimePositionChangedEvent();

        player.setRate(rate);
        listener.waitForRateChangeEvent();
        assertTrue("Could not set the initial rate of " + rate, player.getRate() == rate);

        // Start playback in Segmented Recorded Service
        helper.startPlayer();
        listener.waitForMediaPresentationEvent();
        assertEquals(segments[0].getName(), cannedDVRAPI.cannedGetLastDecodedRecording());

        // verify that the segments are played back in the reverse order
        for (int i = segments.length - 2; i >= 0; i--)
        {
            listener.reset();
            cannedDVRAPI.cannedGetLastDecodeRecordingListener().asyncEvent(Event.END_OF_FILE, 0, 0);
            listener.waitForMediaPresentationEvent();
            assertEquals(segments[i].getName(), cannedDVRAPI.cannedGetLastDecodedRecording());

            assertTrue("Player rate is not " + rate + ": " + player.getRate(), player.getRate() == rate);
            assertTrue("Player is not playing", player.isPresenting());
        }

        // at this point, we should be playing the last segment, an
        // end of file event should cause the player to generate and end
        // of content event
        cannedDVRAPI.cannedGetLastDecodeRecordingListener().asyncEvent(Event.END_OF_FILE, 0, 0);
        listener.waitForEndOfContentEvent();

    }

    /**
     * No undesirable effects if rate > 1is set while jumping between segments
     */
    public void testSegmentTransitionsRateGreaterThanOne()
    {
        baseTestSegmentTransitionsWithRatePositive(2.0f);
    }

    /**
     * No undesirable effects if rate < 1 but > 0 is set while jumping between
     * segments
     */
    public void testSegmentTransitionPositiveRateLessThanOne()
    {
        baseTestSegmentTransitionsWithRatePositive(0.25f);
    }

    public void testChangeSegmentWhilePlaying()
    {

        CannedRecordedServiceImpl segment = new CannedRecordedServiceImpl();
        segment.cannedSetServiceDetails(sidb.jmfServiceDetails1);
        segment.cannedSetNativeName("testChangeSegmentWhilePlaying");

        assertTrue(service.services.length > 2);

        // Start playback in Segmented Recorded Service
        helper.startPlayer();
        listener.waitForMediaPresentationEvent();

        assertEquals(service.services[0].getName(), cannedDVRAPI.cannedGetLastDecodedRecording());

        //
        // while the first segment is playing, change the second segment
        // and verify that it plays
        //
        listener.reset();
        service.services[1] = segment;
        cannedDVRAPI.cannedGetLastDecodeRecordingListener().asyncEvent(Event.END_OF_FILE, 0, 0);
        listener.waitForMediaPresentationEvent();
        assertEquals(segment.getName(), cannedDVRAPI.cannedGetLastDecodedRecording());

    }

    public void testRemoveAllSegmentsWhilePlaying()
    {
        // Start playback in Segmented Recorded Service
        helper.startPlayer();
        listener.waitForMediaPresentationEvent();
        assertEquals(service.services[0].getName(), cannedDVRAPI.cannedGetLastDecodedRecording());

        //
        // remove all of the segments
        //
        service.services = new RecordedService[] { service.services[0] };
        service.times = new Time[] { service.times[0] };

        //
        // signal the end of the currently playing segment, this should
        // stop the player
        //
        cannedDVRAPI.cannedGetLastDecodeRecordingListener().asyncEvent(Event.END_OF_FILE, 0, 0);

        boolean received = listener.waitForEndOfContentEvent();
        assertTrue("The end of content event was not received", received);
    }

    public void testStopInSecondSegmentThenRestart()
    {
        RecordedService[] segments = service.getSegments();

        // Playback Segmented Recorded service
        helper.startPlayer();
        listener.waitForMediaPresentationEvent();
        assertEquals(segments[0].getName(), cannedDVRAPI.cannedGetLastDecodedRecording());
        //
        // transition to the second segment
        //
        listener.reset();
        cannedDVRAPI.cannedGetLastDecodeRecordingListener().asyncEvent(Event.END_OF_FILE, 0, 0);
        listener.waitForMediaPresentationEvent();
        assertEquals(segments[1].getName(), cannedDVRAPI.cannedGetLastDecodedRecording());

        //
        // stop the player while presenting in the second segment
        //
        player.stop();
        listener.waitForStopEvent();

        //
        // start the player back up and verify that it resumes in the second
        // segment
        //
        cannedDVRAPI.cannedClearLastDecodedRecording();
        listener.reset();
        player.start();
        listener.waitForMediaPresentationEvent();
        assertEquals(segments[1].getName(), cannedDVRAPI.cannedGetLastDecodedRecording());

    }

    private static class SegmentedRecordingPlayerFactory extends JMFFactory
    {

        private DVRAPIManager oldDVR;

        private ServiceContextExt sce;

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

            SegmentedRecordedServicePlayer player = new CannedSegmentedRecordingPlayer(ccm.getCurrentContext());
            DataSource ds = new DataSource();

            CannedSIDatabase sidb = getCannedSIDB();
            CannedSegmentedRecordedService service = new CannedSegmentedRecordedService(sidb);

            ServiceDetails sd = new ServiceDetailsImpl(JMFTests.csm.getSICache(), new ServiceDetailsHandleImpl(1110),
                    1100, 101, sidb.transportStream7, new MultiString(new String[] { "eng" },
                            new String[] { "JMF ServiceDetails 1" }), service, DeliverySystemType.CABLE,
                    ServiceInformationType.ATSC_PSIP, new java.util.Date(System.currentTimeMillis() - 1000),
                    new int[] {}, true, 0x1FFF, null);

            service.cannedSetServiceDetails(sd);
            service.setMediaTime(new Time(0));
            service.cannedSetRecordedDuration(20000);
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

    private static class CannedSegmentedRecordingPlayer extends SegmentedRecordedServicePlayer
    {
        public CannedSegmentedRecordingPlayer(CallerContext cc)
        {
            super(cc, new Object(), new ResourceUsageImpl(cc));
        }

        protected HScreen getDefaultScreen()
        {
            return CannedHScreen.getInstance();
        }
    }

    private static class CannedSegmentedRecordedService extends CannedRecordedServiceImpl implements
            SegmentedRecordedService
    {
        RecordedService[] services;

        Time[] times;

        Time firstMediaTime;

        private CannedSegmentedRecordedService(CannedSIDatabase sidb)
        {
            CannedRecordedServiceImpl service1 = new CannedRecordedServiceImpl();
            service1.cannedSetServiceDetails(sidb.jmfServiceDetails1);
            service1.cannedSetNativeName("Recording1");

            CannedRecordedServiceImpl service2 = new CannedRecordedServiceImpl();
            service2.cannedSetServiceDetails(sidb.jmfServiceDetails1);
            service2.cannedSetNativeName("Recording2");

            CannedRecordedServiceImpl service3 = new CannedRecordedServiceImpl();
            service3.cannedSetServiceDetails(sidb.jmfServiceDetails2);
            service3.cannedSetNativeName("Recording3");

            services = new RecordedService[] { service1, service2, service3 };
            times = new Time[] { new Time(0d), new Time(2d), new Time(4d) };
            firstMediaTime = times[0];

        }

        public long getRecordedDuration()
        {
            if (times != null)
            {
                return times[times.length - 1].getNanoseconds() * 2;
            }
            else
            {
                return 0;
            }
        }

        public Time[] getSegmentMediaTimes()
        {
            return times;
        }

        public RecordedService[] getSegments()
        {
            return services;
        }

        public Time getFirstMediaTime()
        {
            return firstMediaTime;
        }

        public TimeTable getCCITimeTable()
        {
            return null;
        }
        
        public void cannedSetFirstMediaTime(Time t)
        {
            firstMediaTime = t;
        }
    }

}
