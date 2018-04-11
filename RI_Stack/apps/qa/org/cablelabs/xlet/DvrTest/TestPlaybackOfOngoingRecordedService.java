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

package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.MediaTimeSetEvent;
import javax.media.Player;
import javax.media.Time;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextListener;

import org.apache.log4j.Logger;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.SegmentedRecordedService;

/**
 * @author PeterK
 * 
 *         These tests test what happens if you:
 * 
 *         1) start recording 2) play back the recording while it's still going
 *         3) create a brief resource contention that segments the recording 4)
 *         continue playback over the segmentation
 */
public class TestPlaybackOfOngoingRecordedService extends DvrTest
{
    final Logger m_log = Logger.getLogger("TestPlaybackOfOngoingRecordedService");

    TestPlaybackOfOngoingRecordedService(Vector locators)
    {
        super(locators);

        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new PlaybackSegmentedWithMultipleOngoing((float) 1.0, (OcapLocator) m_locators.elementAt(0),
                (OcapLocator) m_locators.elementAt(1)));
        tests.addElement(new PlaybackSegmentedWithMultipleOngoing((float) 64.0, (OcapLocator) m_locators.elementAt(0),
                (OcapLocator) m_locators.elementAt(1)));
        tests.addElement(new PlaybackSegmentedWithMultipleOngoing((float) 0.5, (OcapLocator) m_locators.elementAt(0),
                (OcapLocator) m_locators.elementAt(1)));
        tests.addElement(new JoggingBetweenSegmentsWhenSegmentCreated((OcapLocator) m_locators.elementAt(0),
                (OcapLocator) m_locators.elementAt(1)));

        return tests;
    }

    /**
     * Schedules a long recording, then two other short ones after it in order
     * to create a resource contention and segment the first recording.
     */
    class ScheduleSegmentedRecording extends EventScheduler.NotifyShell
    {
        long time = 0;

        long gap = 1000 * 60 * 2; // 2 mins between starting long recording and
                                  // short recordings

        long recordingDuration = 1000 * 60 * 5; // 5 mins

        OcapLocator m_locator = null;

        OcapLocator m_locator2 = null;

        public ScheduleSegmentedRecording(OcapLocator locatorA, OcapLocator locatorB, long delayInMillis, long gap)
        {
            super(delayInMillis);

            initialize(locatorA, locatorB, delayInMillis, gap);
        }

        public ScheduleSegmentedRecording(OcapLocator locatorA, OcapLocator locatorB, long delayInMillis, long gap,
                long duration)
        {
            super(delayInMillis);

            initialize(locatorA, locatorB, delayInMillis, gap);

            recordingDuration = duration;
        }

        private void initialize(OcapLocator locatorA, OcapLocator locatorB, long delayInMillis, long gap)
        {
            m_locator = locatorA;
            m_locator2 = locatorB;

            time = delayInMillis;
            this.gap = gap;
        }

        public void ProcessCommand()
        {
            final long now = System.currentTimeMillis();
            final long expireTime = 1000 * 60 * 60 * 24; // 24 hours

            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(time));

            time += 10000;
            m_eventScheduler.scheduleCommand(new Record("Recording3", // recording
                                                                      // name
                    m_locator, // service locator
                    now + time, // recording start time
                    recordingDuration, // recording duration
                    time, // task start time
                    expireTime, // recording expiration time
                    0, // retention priority
                    OcapRecordingProperties.RECORD_WITH_CONFLICTS // recording
                                                                  // priority
            ));

            // factor in the gap between starting to record and creating
            // the resource contention
            time += gap;

            // schedule a second recording to start later but with higher
            // priority, to cause the first one to be segmented
            m_eventScheduler.scheduleCommand(new Record("Recording2", // recording
                                                                      // name
                    m_locator2, // service locator
                    now + time, // recording start time
                    30000, // recording duration
                    time, // task start time
                    expireTime, // recording expiration time
                    0, // retention priority
                    OcapRecordingProperties.RECORD_WITH_CONFLICTS // recording
                                                                  // priority
            ));

            // schedule a third recording, same as the second one
            m_eventScheduler.scheduleCommand(new Record("Recording1", // recording
                                                                      // name
                    m_locator2, // service locator
                    now + time + 5000, // recording start time
                    30000, // recording duration
                    time, // task start time
                    expireTime, // recording expiration time
                    0, // retention priority
                    OcapRecordingProperties.RECORD_WITH_CONFLICTS // recording
                                                                  // priority
            ));
        }
    }

    /**
     * Sets the playback rate on a NormalContentEvent, then unregisters itself
     * from the ServiceContext's listeners.
     */
    class MediaRateSetter extends EventScheduler.NotifyShell implements ServiceContextListener
    {
        private ServiceContext m_serviceContext = null;

        private Logger m_log = null;

        private float m_rate = (float) 1.0;

        public MediaRateSetter(ServiceContext sc, Logger log, long delayInMillis, float rate)
        {
            super(delayInMillis);

            m_serviceContext = sc;
            m_log = log;
            m_rate = rate;
        }

        public void ProcessCommand()
        {
            if (m_serviceContext == null)
            {
                m_log.debug("*******************************************************");
                m_log.debug("!!!! Unable to locate service context to clean up! !!!!");
                m_log.debug("*******************************************************");
                return;
            }

            m_serviceContext.addListener(this);
        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            if (ev instanceof NormalContentEvent == false) return;

            m_serviceContext = ev.getServiceContext();
            m_serviceContext.removeListener(this);

            Player player = getServicePlayer(m_serviceContext);

            if (player == null)
            {
                m_log.debug("*************************************************");
                m_log.debug("!!!! Unable to locate player to set rate on! !!!!");
                m_log.debug("*************************************************");

                return;
            }

            m_log.debug("*******************************************************");
            m_log.debug("MediaRateSetter: Setting media rate to " + m_rate + ".");
            m_log.debug("*******************************************************");

            player.setRate(m_rate);
        }
    }

    /**
     * Performs some test cleanup tasks when playback stops and a
     * PresentationTerminatedEvent is received.
     */
    public class TestCleanupHandler extends EventScheduler.NotifyShell implements ServiceContextListener
    {
        private ServiceContext m_serviceContext = null;

        private Logger m_log = null;

        public TestCleanupHandler(ServiceContext sc, Logger log, long delayInMillis)
        {
            super(delayInMillis);
            this.m_log = log;
            this.m_serviceContext = sc;
        }

        public void ProcessCommand()
        {
            if (m_serviceContext == null)
            {
                m_log.debug("*******************************************************");
                m_log.debug("!!!! Unable to locate service context to clean up! !!!!");
                m_log.debug("*******************************************************");
                return;
            }

            m_serviceContext.addListener(this);
        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            if (ev instanceof PresentationTerminatedEvent)
            {
                m_log.debug("***************************");
                m_log.debug("PresentationTerminatedEvent");
                m_log.debug("***************************");

                // new DeleteRecordings(0).ProcessCommand();
                new RegisterResourceContentionHandler(null, 0).ProcessCommand();
            }
        }
    }

    /**
     * Boilerplate checks for all of these tests to make sure the segmentation
     * of the recording worked and the segment times are realistic.
     */
    class PlaybackStateChecker extends EventScheduler.NotifyShell implements ServiceContextListener
    {
        private TestCompletionListener m_test = null;

        private ServiceContext m_serviceContext = null;

        private Logger m_log = null;

        private long testTime = 0;

        private int normalContentEvents = 0;

        public PlaybackStateChecker(TestCompletionListener test, ServiceContext sc, Logger log, long delayInMillis)
        {
            super(delayInMillis);

            m_test = test;
            m_serviceContext = sc;
            m_log = log;
            testTime = delayInMillis;
        }

        public void ProcessCommand()
        {
            if (m_serviceContext == null)
            {
                m_log.debug("*******************************************");
                m_log.debug("!!!! Unable to locate service context! !!!!");
                m_log.debug("*******************************************");
                return;
            }

            m_serviceContext.addListener(this);
        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            if (ev instanceof NormalContentEvent == false) return; // we're not
                                                                   // interested
                                                                   // in any
                                                                   // other
                                                                   // events

            normalContentEvents++;

            if (normalContentEvents < 2)
            {
                m_log.debug("*****************************************************************");
                m_log.debug("PlaybackStateChecker: Got a NormalContentEvent, waiting for more.");
                m_log.debug("*****************************************************************");

                return;
            }

            m_log.debug("****************************************************************");
            m_log.debug("PlaybackStateChecker: NormalContentEvent. Validating test state.");
            m_log.debug("****************************************************************");

            m_failed = TEST_PASSED;

            // TEST PROCEDURE:
            // verify that the recording is in the correct state
            //
            // verify that the recording was in fact segmented
            //
            // verify that the duration of the entire recording is greater than
            // the duration of the first segment
            //
            // verify that the current media time is greater than the second
            // segment's start time (ie. playback is playing the second segment)
            new ConfirmRecordingReq_CheckState("Recording3", OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE,
                    testTime).ProcessCommand();

            try
            {
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject("Recording3");
                RecordedService rs = rr.getService();

                if (rs instanceof SegmentedRecordedService)
                {
                    RecordedService[] rss = ((SegmentedRecordedService) rs).getSegments();
                    long entireDuration = rs.getRecordedDuration();
                    long segmentDuration = rss[0].getRecordedDuration();

                    if (rss.length < 2)
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason = "Recording has less than two segments.";
                    }
                    else if (entireDuration <= segmentDuration)
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason = "Duration of entire segmented recording was equal to or less than duration of first segment.";
                    }
                    else
                    {
                        double curMediaTime = rs.getMediaTime().getSeconds();
                        double segmentMediaTime = rss[1].getMediaTime().getSeconds();

                        if (curMediaTime < segmentMediaTime)
                        {
                            m_failed = TEST_FAILED;
                            m_failedReason = "Current media time is less than second segment's media time.";
                        }
                    }
                }
                else
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "Recording was not a segmented recording.";
                }
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "Unexpected exception: " + e.toString();
                e.printStackTrace();
            }

            if (m_failed == TEST_FAILED)
            {
                m_log.debug("************************** TEST FAILED **************************");
                m_log.debug(m_failedReason);
                m_log.debug("*****************************************************************");
            }

            // notify the test so it can post its results
            m_test.completeTest();
        }
    }

    /**
     * Kind of a hack... waits for NormalContentEvent and schedules a jog ahead
     * 30 seconds in the playback to happen after a specified delay.
     */
    class PlaybackJogger extends EventScheduler.NotifyShell implements ServiceContextListener, ControllerListener
    {
        private ServiceContext m_serviceContext = null;

        private Logger m_log = null;

        private long testTime = 0;

        private int normalContentEvents = 0;

        private ControllerListener controllerListener = this; // ugly hack :(

        private float m_rate = 1;

        private long m_gap = 180000; // gap between recording start and
                                     // segmentation

        public PlaybackJogger(ServiceContext sc, Logger log, long delayInMillis, long gap)
        {
            super(delayInMillis);

            m_serviceContext = sc;
            m_log = log;
            testTime = delayInMillis;
            m_gap = gap;
        }

        public void ProcessCommand()
        {
            if (m_serviceContext == null)
            {
                m_log.debug("*******************************************");
                m_log.debug("!!!! Unable to locate service context! !!!!");
                m_log.debug("*******************************************");
                return;
            }

            m_serviceContext.addListener(this);
        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            if (ev instanceof NormalContentEvent == false) return; // we're not
                                                                   // interested
                                                                   // in any
                                                                   // other
                                                                   // events

            m_serviceContext = ev.getServiceContext();

            normalContentEvents++;

            if (normalContentEvents > 1)
            {
                m_log.debug("**************************************************");
                m_log.debug("PlaybackJogger: Ignoring extra NormalContentEvent.");
                m_log.debug("**************************************************");

                return;
            }

            m_log.debug("***********************************************************************");
            m_log.debug("PlaybackJogger: First NormalContentEvent. Jogging ahead in " + (double) (m_gap / 1000)
                    + " seconds.");
            m_log.debug("***********************************************************************");

            new Thread()
            {
                public void run()
                {
                    try
                    {
                        Thread.sleep(m_gap);

                        m_log.debug("********************************");
                        m_log.debug("PlaybackJogger: Jogging ahead...");
                        m_log.debug("********************************");

                        // Get the Player
                        Player player = getServicePlayer(m_serviceContext);

                        if (player == null)
                        {
                            m_failedReason = "PlaybackJogger can't get Player";
                            m_failed = TEST_FAILED;

                            m_log.debug("********************************");
                            m_log.debug(m_failedReason);
                            m_log.debug("********************************");

                            return;
                        }

                        player.addControllerListener(controllerListener);

                        m_rate = player.getRate();

                        // Get the current media time from the SC
                        double secs = player.getMediaTime().getSeconds();
                        double newsecs = 0;

                        newsecs = secs + 30;

                        player.setMediaTime(new Time(newsecs));
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();

                        m_failedReason = "PlaybackJogger - Exception thrown in processing command: " + e;
                        m_failed = TEST_FAILED;

                        DVRTestRunnerXlet.log(m_failedReason);
                    }
                }
            }.start();
        }

        public void controllerUpdate(ControllerEvent event)
        {
            if (event instanceof MediaTimeSetEvent)
            {
                DVRTestRunnerXlet.log("Received MediaTimeSetEventEvent");
                Controller controller = event.getSourceController();
                float rate = controller.getRate();

                if (rate != m_rate)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "PlaybackJogger media rate = " + rate + " not " + m_rate;
                    DVRTestRunnerXlet.log(m_failedReason);
                }
            }
        }
    }

    /**
     * A TestCompletionListener waits for a signal from some other class before
     * doing whatever tasks it needs to do upon completion of the test
     * procedure.
     */
    interface TestCompletionListener
    {
        public void completeTest();
    }

    /**
     * Perform the test, setting playback to the specified rate.
     */
    class PlaybackSegmentedWithMultipleOngoing extends TestCase implements TestCompletionListener
    {
        private OcapLocator m_locator;

        private OcapLocator m_locator2;

        private float m_rate = (float) 1.0;

        PlaybackSegmentedWithMultipleOngoing(float rate, OcapLocator locator, OcapLocator otherLocator)
        {
            m_locator = locator;
            m_locator2 = otherLocator;
            m_rate = rate;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Playback between segments during ongoing recording at rate " + m_rate;
        }

        public void runTest()
        {
            long testTime = 0;
            long duration = 1000 * 60 * 8; // recording duration
            long gap = 1000 * 60 * 2; // duration of gap between starting the
            // long recording and attempting to segment it

            m_failed = TEST_PASSED;

            m_eventScheduler.scheduleCommand(new ScheduleSegmentedRecording(m_locator, m_locator2, testTime,
                    (long) (gap * m_rate), duration));

            testTime += (long) (120000);
            SelectRecordedServiceAsync srsa = new SelectRecordedServiceAsync("Recording3", duration, testTime, false);

            srsa.addListener(new TestCleanupHandler(srsa.getServiceContext(), m_log, testTime));
            srsa.addListener(new PlaybackStateChecker(this, srsa.getServiceContext(), m_log, testTime));

            if (m_rate != 1) srsa.addListener(new MediaRateSetter(srsa.getServiceContext(), m_log, testTime, m_rate));

            m_eventScheduler.scheduleCommand(srsa);

            // schedule an event at the end of the test to push out the
            // timer since everything else is event driven
            testTime = duration;
            m_eventScheduler.scheduleCommand(new EventScheduler.NotifyShell(testTime)
            {
                public void ProcessCommand()
                {
                    // do nothing
                }
            });

            // run the scheduled tasks
            m_eventScheduler.run(15000);
        }

        public void completeTest()
        {
            m_eventScheduler.cancel();
            postResults(getName(), false);
        }
    }

    /**
     * Same test with playback rate of 1.0 and a 30 second jog forward over the
     * segment.
     */
    class JoggingBetweenSegmentsWhenSegmentCreated extends TestCase implements TestCompletionListener
    {
        private OcapLocator m_locator;

        private OcapLocator m_locator2;

        JoggingBetweenSegmentsWhenSegmentCreated(OcapLocator locator, OcapLocator otherLocator)
        {
            m_locator = locator;
            m_locator2 = otherLocator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Jogging during ongoing recording as segment is created";
        }

        public void runTest()
        {
            long testTime = 0;
            long duration = 1000 * 60 * 8; // recording duration
            long gap = 1000 * 60 * 2; // duration of gap between starting the
            // long recording and attempting to segment it

            m_failed = TEST_PASSED;

            m_eventScheduler.scheduleCommand(new ScheduleSegmentedRecording(m_locator, m_locator2, testTime, gap,
                    duration));

            testTime += 120000;
            SelectRecordedServiceAsync srsa = new SelectRecordedServiceAsync("Recording3", duration, testTime, false);

            srsa.addListener(new TestCleanupHandler(srsa.getServiceContext(), m_log, testTime));
            srsa.addListener(new PlaybackStateChecker(this, srsa.getServiceContext(), m_log, testTime));
            srsa.addListener(new PlaybackJogger(srsa.getServiceContext(), m_log, testTime, gap + 30000));

            m_eventScheduler.scheduleCommand(srsa);

            // schedule an event at the end of the test to push out the
            // timer since everything else is event driven
            testTime = duration;
            m_eventScheduler.scheduleCommand(new EventScheduler.NotifyShell(testTime)
            {
                public void ProcessCommand()
                {
                    // do nothing
                }
            });

            // run the scheduled tasks
            m_eventScheduler.run(15000);
        }

        public void completeTest()
        {
            m_eventScheduler.cancel();
            postResults(getName(), false);
        }
    }
}
