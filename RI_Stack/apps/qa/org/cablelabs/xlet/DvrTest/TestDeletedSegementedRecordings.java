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

/*
 * Created on Oct 30, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import javax.media.Player;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextListener;

import org.ocap.dvr.OcapRecordedService;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.shared.dvr.SegmentedRecordedService;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestDeletedSegementedRecordings extends DvrTest
{

    /**
     * @param locators
     */
    TestDeletedSegementedRecordings(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector test = new Vector();
        test.addElement(new Generate4Segments((OcapLocator) m_locators.elementAt(0)));
        test.addElement(new TestPlayback4Segment());
        test.addElement(new TestCase1());
        test.addElement(new TestCase2and3(true));
        test.addElement(new TestCase2and3(false));
        test.addElement(new TestCase4and5((OcapLocator) m_locators.elementAt(0), true));
        test.addElement(new TestCase4and5((OcapLocator) m_locators.elementAt(0), false));
        // test.addElement(new TestCase6());
        test.addElement(new TestCase7and8(true));
        test.addElement(new TestCase7and8(false));
        // test.addElement(new TestCase9());
        // test.addElement(new TestCase10());
        return test;
    }

    public class Generate4Segments extends TestCase
    {
        private OcapLocator m_locator;

        /**
         * Constructor
         */
        Generate4Segments(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Generate4Segments";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 0;
            long expire_time = 24 * 60 * 60 * 1000;
            // Clear event scheduler
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 90000 + delay, 60000, 1000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 90000 + delay, 60000, 2000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 210000 + delay, 60000, 3000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + 210000 + delay, 60000, 4000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, now + 330000 + delay, 60000, 5000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording6", m_locator, now + 330000 + delay, 60000, 6000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording7", m_locator, now + 30000 + delay, 420000, 7000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            // Verify states prior to recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20100 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20400 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording6",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20500 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 20600 + delay));

            // Shortly after start time, Verify Rec5 is IN_PROGRESS_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55100 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55400 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording6",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55500 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 55600 + delay));

            // Shortly after start time, Verify Rec5 is IN_PROGRESS_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 140000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 140100 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 140200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 140300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 140400 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording6",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 140500 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 140600 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording7",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 140000 + delay));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 200000 + delay));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 250200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 250300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 250400 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording6",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 250500 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 250600 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording7",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 280000 + delay));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 315000 + delay));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 370400 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording6",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 370500 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 370600 + delay));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 435000 + delay));

            // Request shall finish in INCOMPLETE_STATE after stop timer has
            // elapsed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.INCOMPLETE_STATE, 460000 + delay));

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 475000 + delay));

            // Verify the number of recorded services generated
            m_eventScheduler.scheduleCommand(new CheckRecordedServices("Recording7", true, 4, 470000));

            // Add AppData to find recorded service
            m_eventScheduler.scheduleCommand(new AddAppData("Recording7", "4segment_playback", "Recording1", 482000));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }
    }

    public class TestPlayback4Segment extends TestCase
    {
        TestPlayback4Segment()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestPlayback4Segment";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // Clear event scheduler
            reset();

            // Find Recording
            getRecReq("Recording1", "4segmented_playback", "Recording1");

            // Get the duration for the segments
            double segDur1 = getSegmentDuration("Recording1", 0);
            double segDur2 = getSegmentDuration("Recording1", 1);
            double segDur3 = getSegmentDuration("Recording1", 2);
            double segDur4 = getSegmentDuration("Recording1", 3);
            long duration = ((((long) segDur1) + ((long) segDur2) + ((long) segDur3) + ((long) segDur4)) * 1000) - 5000;
            System.out.println("Duration : " + duration);

            // Select recorded service
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", duration, 1000, false));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }
    }

    public class TestCase1 extends TestCase
    {
        /**
         * Constructor
         */
        TestCase1()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDeletedSegementedRecordings :Deletion of a Recording Request deletes associated Recorded Services ";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // Clear event scheduler
            reset();

            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Identify the recorded services
            // Store the instances of these services
            selectSegWithinRecSvc("Recording1", 0, "rec1-seg1");
            selectSegWithinRecSvc("Recording1", 1, "rec1-seg2");
            selectSegWithinRecSvc("Recording1", 2, "rec1-seg3");

            // Delete the Recording request
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 1000));

            m_eventScheduler.run(5000);

            // Verify exceptions by selecting the stored instances of the
            // services
            verifyIllegalStateException("rec1-seg1");
            verifyIllegalStateException("rec1-seg2");
            verifyIllegalStateException("rec1-seg3");

            // Post results
            postResults(getName(), true);
        }
    }

    public class TestCase2and3 extends TestCase implements ServiceContextListener
    {
        /**
         * Constructor
         */
        private boolean gotPTE = false;

        private boolean deleteService;

        TestCase2and3(boolean delSvc)
        {
            deleteService = delSvc;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDeletedSegementedRecordings :Deletion of "
                    + (deleteService ? "the Recorded Service" : "a Recording Request")
                    + " while in playback deletes associated Recorded Services";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            gotPTE = false;
            // Clear event scheduler
            reset();

            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Set up a Service Context
            new initServiceContext("RecSvcSC", 0).ProcessCommand();
            m_serviceContext.addListener(this);

            // Get the duration for the segments
            double segDur1 = getSegmentDuration("Recording1", 0);
            double segDur2 = getSegmentDuration("Recording1", 1);
            double segDur3 = getSegmentDuration("Recording1", 2);
            long duration = ((((long) segDur1) + ((long) segDur2) + ((long) segDur1)) * 1000) - 1000;
            System.out.println("Duration : " + duration);

            // Identify the recorded services
            // Store the instances of these services
            selectSegWithinRecSvc("Recording1", 0, "rec1-seg1");
            selectSegWithinRecSvc("Recording1", 1, "rec1-seg2");
            selectSegWithinRecSvc("Recording1", 2, "rec1-seg3");

            // Select recorded service - ignore termination events
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", "RecSvcSC", 0.0, 0, 1000, true));

            // Delete the currently presenting segmented recorded service
            if (!deleteService)
            {
                m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new DeleteRecordedService("Recording1", 20000));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                        OcapRecordingRequest.DELETED_STATE, 40000));
            }

            // Run the schedule
            m_eventScheduler.run(5000);

            // Verify exceptions by selecting the stored instances of the
            // services
            verifyIllegalStateException("rec1-seg1");
            verifyIllegalStateException("rec1-seg2");
            verifyIllegalStateException("rec1-seg3");

            // Verify that the PresentationTerminatedEvent occured else failed
            if (!gotPTE)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Failed to receive PresentationTerminatedEvent");
            }

            m_serviceContext.removeListener(this);
            // Clean up Service Context
            cleanSC();

            // Post results
            postResults(getName(), true);
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.service.selection.ServiceContextListener#
         * receiveServiceContextEvent
         * (javax.tv.service.selection.ServiceContextEvent)
         */
        public void receiveServiceContextEvent(ServiceContextEvent arg0)
        {
            // TODO Auto-generated method stub
            // Verify that a Presentation Termination Event is fired. Verify
            // that the reason code is SERVICE VANISHED
            if (arg0 instanceof PresentationTerminatedEvent)
            {
                if (((PresentationTerminatedEvent) arg0).getReason() == PresentationTerminatedEvent.SERVICE_VANISHED)
                {
                    gotPTE = true;
                }
                else
                {
                    System.out.println("Reason not correct : reason" + ((PresentationTerminatedEvent) arg0).getReason());
                }
            }
        }
    }

    public class TestCase4and5 extends TestCase implements ServiceContextListener
    {
        private boolean deleteService;

        private boolean gotPTE;

        private OcapLocator m_locator;

        /**
         * Constructor
         */
        TestCase4and5(OcapLocator locator, boolean delSvc)
        {
            m_locator = locator;
            deleteService = delSvc;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDeletedSegementedRecordings :Deletion of an ongoing "
                    + (deleteService ? "Recording" : "Request")
                    + " deletes associated Recorded Services during playback";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            gotPTE = false;
            long expire_time = 24 * 60 * 60 * 1000;
            // Clear event scheduler
            reset();

            // Set up a Service Context
            new initServiceContext("RecSvcSC", 0).ProcessCommand();
            m_serviceContext.addListener(this);

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Create a segmented Recorded service with two segments
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 240000, 3000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 60000, 240000, 4000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 60000, 240000, 5000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            // Delete higher prioritized recordings
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 120000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 120000));

            // Upon starting of the recording of the second segment, begin
            // playback of the Segmented Recorded Service
            // Select recorded service - ignore termination events
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording3", "RecSvcSC", 0.0, 0, 180000, true));

            // Delete the currently presenting segmented recorded service
            if (deleteService)
            {
                m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 200000));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new DeleteRecordedService("Recording3", 200000));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                        OcapRecordingRequest.DELETED_STATE, 220000));
            }

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 235000));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Verify that the PresentationTerminatedEvent occured else failed
            if (!gotPTE)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Failed to receive PresentationTerminatedEvent");
            }

            m_serviceContext.removeListener(this);
            // Clean up Service Context
            cleanSC();

            // Post results
            postResults(getName(), true);
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.service.selection.ServiceContextListener#
         * receiveServiceContextEvent
         * (javax.tv.service.selection.ServiceContextEvent)
         */
        public void receiveServiceContextEvent(ServiceContextEvent arg0)
        {
            // TODO Auto-generated method stub
            // Verify that a Presentation Termination Event is fired. Verify
            // that the reason code is SERVICE VANISHED
            if (arg0 instanceof PresentationTerminatedEvent)
            {
                if (((PresentationTerminatedEvent) arg0).getReason() == PresentationTerminatedEvent.SERVICE_VANISHED)
                {
                    gotPTE = true;
                }
                else
                {
                    System.out.println("Reason not correct : reason" + ((PresentationTerminatedEvent) arg0).getReason());
                }
            }
        }
    }

    public class TestCase6 extends TestCase implements ServiceContextListener
    {
        private boolean gotPTE;

        /**
         * Constructor
         */
        TestCase6()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDeletedSegementedRecordings : Jogging between segments does not cause undesirable side effect segments  ";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            gotPTE = false;
            // Clear event scheduler
            reset();

            // Find Recording
            getRecReq("Recording1", "4segmented_playback", "Recording1");

            // Get the duration for the first segment
            double segDur1 = getSegmentDuration("Recording1", 0);
            double segDur3 = getSegmentDuration("Recording1", 2);

            // Identify the recorded services
            // Store the instances of these services
            selectSegWithinRecSvc("Recording1", 1, "rec1-seg2");
            selectSegWithinRecSvc("Recording1", 2, "rec1-seg3");

            new initServiceContext("RecSvcSC", 0).ProcessCommand();
            m_serviceContext.addListener(this);

            // Set media time of recorded service 10 seconds prior to end of
            // first segment
            // and Playback Segmented Recorded service
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", "RecSvcSC", (segDur1 - 20.0), 60000,
                    1000, false));

            // Delete second segment
            m_eventScheduler.scheduleCommand(new DeleteSegmentedService("rec1-seg2", 10000));

            // Jump to 20 seconds after start of playback and deletion of
            // recorded service
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("RecSvcSC", 20.0F, false, false, 15000));

            // Verify playback through comparison of start Media time of the
            // third segment and current Media time.
            m_eventScheduler.scheduleCommand(new VerifyPlaybackWithinSegment("Recording1", 2, "RecSvcSC", 20000));

            // Jump back to first segment
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("RecSvcSC", -40.0F, false, false, 25000));

            // Verify Playback in first segment by comparing the start Media
            // time of the first segment against current Media time
            m_eventScheduler.scheduleCommand(new VerifyPlaybackWithinSegment("Recording1", 0, "RecSvcSC", 30000));

            // Jump to playback into the fourth segment
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("RecSvcSC", (float) (20.0 + segDur3), false, false,
                    35000));

            // Verify Playback in fourth segment by comparing the start Media
            // time of the fourth segment against current Media time
            m_eventScheduler.scheduleCommand(new VerifyPlaybackWithinSegment("Recording1", 3, "RecSvcSC", 40000));

            // Delete second segment
            m_eventScheduler.scheduleCommand(new DeleteSegmentedService("rec1-seg3", 45000));

            // Jump back to first segment
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("RecSvcSC", -40.0F, false, false, 50000));

            // Verify Playback in first segment by comparing the start Media
            // time of the first segment against current Media time
            m_eventScheduler.scheduleCommand(new VerifyPlaybackWithinSegment("Recording1", 0, "RecSvcSC", 55000));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Remove the listener and Destroy
            m_serviceContext.removeListener(this);
            cleanSC();

            // Post results
            postResults(getName(), true);
        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.service.selection.ServiceContextListener#
         * receiveServiceContextEvent
         * (javax.tv.service.selection.ServiceContextEvent)
         */
        public void receiveServiceContextEvent(ServiceContextEvent arg0)
        {
            // TODO Auto-generated method stub
            // Frame step to 15 seconds after start of playback
            if ((arg0 instanceof PresentationTerminatedEvent))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Presentation Terminated Event thrown!");
                m_failedReason = "TestCase5 - Presentation Terminated Event thrown!";
            }
        }
    }

    public class TestCase7and8 extends TestCase implements ServiceContextListener
    {
        private boolean gotPTE;

        private boolean m_forward;

        /**
         * Constructor
         */
        TestCase7and8(boolean forward)
        {
            m_forward = forward;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDeletedSegementedRecordings : "
                    + (m_forward ? " Implicit deletion if rate > 1  " : "Implicit deletion  if rate < 0 ");
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            new initServiceContext("RecSvcSC", 0).ProcessCommand();
            m_serviceContext.addListener(this);
            gotPTE = false;
            // Clear event scheduler
            reset();

            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Get the duration for the first segment
            double segDur1 = getSegmentDuration("Recording1", 0);
            double segDur2 = getSegmentDuration("Recording1", 1);

            // Tag middle recording
            selectSegWithinRecSvc("Recording1", 0, "rec1-seg1");
            selectSegWithinRecSvc("Recording1", 1, "rec1-seg2");
            selectSegWithinRecSvc("Recording1", 2, "rec1-seg3");

            if (m_forward)
            {
                // Set recorded service media time into the first service
                m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", "RecSvcSC", (segDur1 - 30.0), 0,
                        1000, false));
            }
            else
            {
                // Set recorded service media time into the third service
                m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", "RecSvcSC",
                        (segDur1 + segDur2 + 10.0), 0, 1000, false));
            }

            // Set rate of playback at -2.0
            if (m_forward)
            {
                m_eventScheduler.scheduleCommand(new SetRate("RecSvcSC", 2.0, true, 10000));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new SetRate("RecSvcSC", -2.0, true, 10000));
            }

            // Delete middle segment
            m_eventScheduler.scheduleCommand(new DeleteSegmentedService("rec1-seg2", 15000));

            // Run the schedule
            m_eventScheduler.run(30000);

            // Remove the listener and Destroy
            m_serviceContext.removeListener(this);
            cleanSC();

            // Verify that the PresentationTerminatedEvent occured else failed
            if (!gotPTE)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Failed to receive PresentationTerminatedEvent");
            }

            // Verify exceptions by selecting the stored instances of the
            // services
            verifyIllegalStateException("rec1-seg1");
            verifyIllegalStateException("rec1-seg2");
            verifyIllegalStateException("rec1-seg3");

            // Post results
            postResults(getName(), true);

        }

        /*
         * (non-Javadoc)
         * 
         * @seejavax.tv.service.selection.ServiceContextListener#
         * receiveServiceContextEvent
         * (javax.tv.service.selection.ServiceContextEvent)
         */
        public void receiveServiceContextEvent(ServiceContextEvent arg0)
        {
            // TODO Auto-generated method stub
            if (arg0 instanceof PresentationTerminatedEvent)
            {
                if (((PresentationTerminatedEvent) arg0).getReason() == PresentationTerminatedEvent.SERVICE_VANISHED)
                {
                    gotPTE = true;
                }
                else
                {
                    System.out.println("Reason not correct : reason" + ((PresentationTerminatedEvent) arg0).getReason());
                }
            }
        }
    }

    public class TestCase9 extends TestCase
    {
        /**
         * Constructor
         */
        TestCase9()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDeletedSegementedRecordings :Stack will update parameters on deleting Segments ";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // Clear event scheduler
            reset();
            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            // Identify the recorded services
            // Store the instances of these services
            selectSegWithinRecSvc("Recording1", 0, "rec1-seg1");
            selectSegWithinRecSvc("Recording1", 1, "rec1-seg2");
            selectSegWithinRecSvc("Recording1", 2, "rec1-seg3");

            // Verify that the getFirstMediaTime of the Segmented Recording
            // Service is the same as that of the first Recorded Service in the
            // array
            m_eventScheduler.scheduleCommand(new ValidateFirstMediaTime("Recording1", 1000));
            // Verify that the duration of each recorded service equals that of
            // the duration of the Segmented Recorded Service
            m_eventScheduler.scheduleCommand(new ValidateSegmentLength("Recording1", 2000));
            // Delete the first recorded service in the array
            m_eventScheduler.scheduleCommand(new DeleteSegmentedService("rec1-seg1", 3000));

            // Verify that the getFirstMediaTime of the Segmented Recording
            // Service is the same as that of the first Recorded Service in the
            // array
            m_eventScheduler.scheduleCommand(new ValidateFirstMediaTime("Recording1", 11000));
            // Verify that the duration of each recorded service equals that of
            // the duration of the Segmented Recorded Service
            m_eventScheduler.scheduleCommand(new ValidateSegmentLength("Recording1", 12000));
            // Delete the first recorded service in the array
            m_eventScheduler.scheduleCommand(new DeleteSegmentedService("rec1-seg2", 13000));

            // Verify that the getFirstMediaTime of the Segmented Recording
            // Service is the same as that of the first Recorded Service in the
            // array
            m_eventScheduler.scheduleCommand(new ValidateFirstMediaTime("Recording1", 2100));
            // Verify that the duration of the recorded service equals that of
            // the duration of the Segmented Recorded Service
            m_eventScheduler.scheduleCommand(new ValidateSegmentLength("Recording1", 22000));
            // Delete the remaining service in the array
            m_eventScheduler.scheduleCommand(new DeleteSegmentedService("rec1-seg3", 23000));

            // Verify that the duration of the recorded is 0
            m_eventScheduler.scheduleCommand(new ValidateFirstMediaTime("Recording1", 31000));

            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }
    }

    public class TestCase10 extends TestCase implements ServiceContextListener
    {
        private boolean gotPTE;

        /**
         * Constructor
         */
        TestCase10()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDeletedSegementedRecordings :SetMediaTime of a Recording Sevice adjusts";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            gotPTE = false;
            // Clear event scheduler
            reset();
            // Find Recording
            getRecReq("Recording1", "segmented_playback", "Recording1");

            new initServiceContext("RecSvcSC", 0).ProcessCommand();
            m_serviceContext.addListener(this);

            // Identify the recorded services
            // Store the instances of these services
            selectSegWithinRecSvc("Recording1", 0, "rec1-seg1");

            // SetMediaTime at some midpoint on the first recorded service
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", "RecSvcSC", 10.0, 0, 1000, true));

            // Delete the first recorded service in the array
            m_eventScheduler.scheduleCommand(new DeleteSegmentedService("rec1-seg1", 13000));
            m_eventScheduler.scheduleCommand(new DestroyService("RecSvcSC", 20000));

            // Verify that the getFirstMediaTime of the Segmented Recording
            // Service is the same as that of the first Recorded Service in the
            // array
            m_eventScheduler.scheduleCommand(new ValidateFirstMediaTime("Recording1", 21000));

            // Verify that playback is on the second segment of the segmented
            // recorded service
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 30000));

            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }

        public void receiveServiceContextEvent(ServiceContextEvent arg0)
        {
            // TODO Auto-generated method stub
            // Frame step to 15 seconds after start of playback
            if (arg0 instanceof PresentationTerminatedEvent)
            {
                if (((PresentationTerminatedEvent) arg0).getReason() == PresentationTerminatedEvent.SERVICE_VANISHED)
                {
                    gotPTE = true;
                }
                else
                {
                    System.out.println("Reason not correct : reason" + ((PresentationTerminatedEvent) arg0).getReason());
                }
            }
        }
    }

    private void selectSegWithinRecSvc(String rrName, int seg, String segName)
    {
        // TODO Auto-generated method stub
        try
        {
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(rrName);
            RecordedService rs = rr.getService();
            if (rs instanceof SegmentedRecordedService)
            {
                RecordedService[] rss = ((SegmentedRecordedService) rs).getSegments();
                insertObject(rss[seg], segName);
            }
            else if (rs instanceof RecordedService)
            {
                System.out.println("ERROR - recorded service but not a segmented recorded service");
                m_failed = TEST_FAILED;
            }
            else if (rs == null)
            {
                System.out.println("ERROR - recorded service is null!");
                m_failed = TEST_FAILED;
            }
            else
            {
                System.out.println("ERROR - not an instace of a recorded service " + rs.toString());
                m_failed = TEST_FAILED;
            }
        }
        catch (Exception e)
        {
            System.out.println("ERROR - Exception thrown");
            e.printStackTrace();
            m_failed = TEST_FAILED;
        }
    }

    private double getSegmentDuration(String rrName, int seg)
    {
        try
        {
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(rrName);
            RecordedService rs = rr.getService();
            if (rs instanceof SegmentedRecordedService)
            {
                RecordedService[] rss = ((SegmentedRecordedService) rs).getSegments();
                double recDur = (double) ((rss[seg].getRecordedDuration()) / 1000.0);
                System.out.println("Returning back duration of segment " + seg + " : " + recDur);
                return recDur;
            }
            else if (rs instanceof RecordedService)
            {
                System.out.println("ERROR - recorded service but not a segmented recorded service");
                m_failed = TEST_FAILED;
            }
            else if (rs == null)
            {
                System.out.println("ERROR - recorded service is null!");
                m_failed = TEST_FAILED;
            }
            else
            {
                System.out.println("ERROR - not an instace of a recorded service " + rs.toString());
                m_failed = TEST_FAILED;
            }
        }
        catch (Exception e)
        {
            System.out.println("ERROR - Exception thrown");
            e.printStackTrace();
            m_failed = TEST_FAILED;
        }
        return 0;
    }

    private void verifyIllegalStateException(String segName)
    {
        try
        {
            RecordedService rs = (RecordedService) findObject(segName);
            long recDur = rs.getRecordedDuration();
            System.out.println("ERROR - No Exception thrown Recorded duration: " + recDur);
            m_failed = TEST_FAILED;
        }
        catch (IllegalStateException e)
        {
            System.out.println("PASSED - Exception thrown correct");
            e.printStackTrace();
        }
        catch (Exception e)
        {
            System.out.println("ERROR - Exception thrown other than IllegalStateException");
            e.printStackTrace();
            m_failed = TEST_FAILED;
        }
    }

    class DeleteSegmentedService extends EventScheduler.NotifyShell
    {
        /**
         * @param rec
         *            the recording
         * @param taskTriggerTime
         *            when to run
         */
        DeleteSegmentedService(String rec, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rName = rec;
        }

        public void ProcessCommand()
        {
            System.out.println();
            System.out.println("<<<<DeleteSegmentedService::ProcessCommand>>>>");
            try
            {
                OcapRecordedService rs = (OcapRecordedService) findObject(m_rName);

                if (rs == null)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils:DeleteSegmentedService:" + DvrEventPrinter.FindObjFailed + m_rName);
                    m_failedReason = "DVRUtils:DeleteSegmentedService:" + DvrEventPrinter.FindObjFailed + m_rName;
                    return;
                }
                System.out.println("DVRUtils:DeleteSegmentedService issueing rs.delete()");
                rs.delete();
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                System.out.println("DVRUtils:DeleteSegmentedService:failed exception on rr.delete()");
                m_failedReason = "DVRUtils:DeleteSegmentedService:failed exception on rr.delete(). Exception: "
                        + e.getMessage();
                e.printStackTrace();
            }
        }

        private String m_rName;
    }

    class VerifyPlaybackWithinSegment extends EventScheduler.NotifyShell
    {

        private String m_recName;

        private String m_scName;

        private int m_seg;

        /**
         * @param time
         */
        VerifyPlaybackWithinSegment(String recName, int seg, String scName, long time)
        {
            super(time);
            m_recName = recName;
            m_scName = scName;
            m_seg = seg;
        }

        public void ProcessCommand()
        {
            try
            {
                System.out.println("<<<<VerifyPlaybackWithinSegment::ProcessCommand>>>>");
                // Get the Current Media time from the Presenting SC
                ServiceContext sc = (ServiceContext) findObject(m_scName);
                Player player = getServicePlayer(sc);
                long currTime = player.getMediaNanoseconds();

                // Get the start time of the segment
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject("Recording1");
                RecordedService[] rs = ((SegmentedRecordedService) rr.getService()).getSegments();
                long startTime = rs[m_seg].getFirstMediaTime().getNanoseconds();

                // Get the duration of the segment
                long duration = (rs[m_seg].getRecordedDuration()) * 1000000;

                // If current media time lies between start of segment
                // and start of segment + duration, test passes
                if ((currTime < (duration + startTime)) && (currTime > startTime))
                {
                    System.out.println();
                }
                else
                {
                    m_failed = TEST_FAILED;
                    System.out.println("DVRUtils:VerifyPlaybackWithinSegment: unexpected value of MediaTime :"
                            + (currTime / 1000000000));
                    m_failedReason = "DVRUtils:VerifyPlaybackWithinSegment: unexpected value of MediaTime :"
                            + (currTime / 1000000000);
                }
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                System.out.println("DVRUtils:VerifyPlaybackWithinSegment:failed exception ");
                m_failedReason = "DVRUtils:VerifyPlaybackWithinSegment:Exception: " + e.getMessage();
                e.printStackTrace();
            }
        }

    }

    class ValidateSegmentLength extends EventScheduler.NotifyShell
    {

        private String m_recName;

        /**
         * @param time
         */
        ValidateSegmentLength(String rec, long time)
        {
            super(time);
            m_recName = rec;
        }

        public void ProcessCommand()
        {
            try
            {
                long recDur = 0;
                long segDur = 0;

                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recName);
                RecordedService rs = rr.getService();
                if (rs instanceof SegmentedRecordedService)
                {
                    recDur = rs.getRecordedDuration();
                    RecordedService[] rss = ((SegmentedRecordedService) rs).getSegments();
                    for (int i = 0; i < rss.length; i++)
                    {
                        segDur = segDur + (rss[i].getRecordedDuration());
                        System.out.println("Current duration after adding " + i + "segments = " + segDur);
                    }
                    if (segDur == recDur)
                    {
                        System.out.println("PASSED : Durations equal");
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("DVRUtils:VerifyPlaybackWithinSegment:Durations not equal srs =" + recDur
                                + " | combined rs = " + segDur);
                        m_failedReason = "DVRUtils:VerifyPlaybackWithinSegment: Durations not equal srs =" + recDur
                                + " | combined rs = " + segDur;
                    }
                }
                else
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("DVRUtils:VerifyPlaybackWithinSegment:Not a Segmented Recorded Service ");
                    m_failedReason = "DVRUtils:VerifyPlaybackWithinSegment:Exception: Not a Segmented Recorded Service";
                }

            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DVRUtils:ValidateSegmentLength:failed exception ");
                m_failedReason = "DVRUtils:ValidateSegmentLength:Exception: " + e.getMessage();
                e.printStackTrace();
            }
        }

    }

    class ValidateFirstMediaTime extends EventScheduler.NotifyShell
    {

        private String m_recName;

        /**
         * @param time
         */
        ValidateFirstMediaTime(String rec, long time)
        {
            super(time);
            m_recName = rec;
        }

        public void ProcessCommand()
        {
            try
            {
                OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recName);
                RecordedService rs = rr.getService();
                if (rs instanceof SegmentedRecordedService)
                {
                    double srsFirstTime = rs.getFirstMediaTime().getSeconds();
                    double rsFirstTime = 0;
                    RecordedService[] rss = ((SegmentedRecordedService) rs).getSegments();
                    if (rss.length != 0)
                    {
                        rsFirstTime = rss[0].getFirstMediaTime().getSeconds();
                    }
                    if (srsFirstTime == rsFirstTime)
                    {
                        System.out.println("PASSED : Durations equal");
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("DVRUtils:VerifyPlaybackWithinSegment:Times not equal srs ="
                                + srsFirstTime + " | rs = " + rsFirstTime);
                        m_failedReason = "DVRUtils:VerifyPlaybackWithinSegment: Times not equal srs =" + srsFirstTime
                                + " | rs = " + rsFirstTime;
                    }
                }

                else
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("DVRUtils:ValidateFirstMediaTime:Not a Segmented Recorded Service ");
                    m_failedReason = "DVRUtils:ValidateFirstMediaTime:Exception: Not a Segmented Recorded Service";
                }
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DVRUtils:ValidateFirstMediaTime:failed exception ");
                m_failedReason = "DVRUtils:ValidateFirstMediaTime:Exception: " + e.getMessage();
                e.printStackTrace();
            }
        }

    }
}
