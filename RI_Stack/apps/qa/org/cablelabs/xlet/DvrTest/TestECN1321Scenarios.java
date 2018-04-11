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
 * Created on Feb 7, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceUsage;

/**
 * @author knunzio
 * 
 */
public class TestECN1321Scenarios extends DvrTest
{

    TestECN1321Scenarios(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestECN1321Scenario1((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECN1321Scenario2((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECN1321Scenario3((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECN1321Scenario4((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECN1321Scenario5((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECN1321Scenario6((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECN1321Scenario7((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECN1321Scenario8((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECN1321Scenario9((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECN1321Scenario10((OcapLocator) m_locators.elementAt(0)));

        return tests;
    }

    /**
     * Recording should be scheduled, show in the nav list, get cancelled, and
     * show a null navigator list
     * 
     * @param locators
     */
    public class TestECN1321Scenario1 extends TestCase
    {
        TestECN1321Scenario1(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Basic Conflict with cancel() BEFORE start.";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 395));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario1 registering the default resourceContention handler.", 396));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 397));

            // Register the resource contention handler
            ECN1321ResourceContentionHandler myRCH = new ECN1321ResourceContentionHandler();
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(myRCH, 400));

            // Schedule 3 overlapping recordings in the future.
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 495));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario1 Before Scheduling Recordings", 496));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 497));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneHour, m_oneHour, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + m_oneHour, m_oneHour, 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_oneHour, m_oneHour, 520));

            // check the recordings states
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario1 Checking states for Recording1,Recording2 and Recording3", 5996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6020));
            // Schedule the cancel

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8995));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario1 Cancelling Recording2", 8996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8997));
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("Recording2", 9000));
            // check the recordings states after the cancel.

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario1 Checking states after the Canceling of Recording2.", 11996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.CANCELLED_STATE, 12010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12020));

            // Un Register the resource contention handler by setting it to
            // NULL.
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 25000));

            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 20010));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 20020));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario1 completed : TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario1 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario1 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestECN1321Scenario2 extends TestCase
    {
        TestECN1321Scenario2(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Basic Conflict with cancel() BEFORE start with RCH.";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 395));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario2 registering the default resourceContention handler.", 396));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 397));
            // Register the resource contention handler
            ECN1321ResourceContentionHandler myRCH = new ECN1321ResourceContentionHandler();
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(myRCH, 400));

            // Schedule 3 overlapping recordings in the future.
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 495));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario2 Before Scheduling Recordings", 496));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 497));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneHour, m_oneHour, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + m_oneHour, m_oneHour, 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_oneHour, m_oneHour, 520));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + m_oneHour, m_oneHour, 530));

            // check the recordings states
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario2 Checking states for Recording1,Recording2 and Recording3", 5996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6020));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6030));
            // Schedule the cancel

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8995));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario2 Cancelling Recording2", 8996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8997));
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("Recording2", 9000));
            // check the recordings states after the cancel.

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario2 Checking states after the Canceling of Recording2.", 11996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.CANCELLED_STATE, 12010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12020));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 12030));

            // Un Register the resource contention handler by setting it to
            // NULL.
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 25000));
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 20010));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 20020));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording4", 20030));

            m_eventScheduler.run(1000);

            // Check RCH call 3 after the cancel.
            String[] firstRCHNames = { "Recording3", "Recording4" };
            RCHCallInfo info = new RCHCallInfo(firstRCHNames, 2);
            boolean result = myRCH.checkCallAt(2, info);

            if (true != result)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario2 - RCH check failed.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario2 completed : TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario2 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario2 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestECN1321Scenario3 extends TestCase
    {
        static final long m_twoHour = m_oneHour * 2;

        TestECN1321Scenario3(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Basic Conflict with cancel() BEFORE start with RCH.";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 395));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario3 registering the default resourceContention handler.", 396));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 397));
            // Register the resource contention handler
            ECN1321ResourceContentionHandler myRCH = new ECN1321ResourceContentionHandler();
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(myRCH, 400));

            // Schedule 3 overlapping recordings in the future.
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 495));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario3 Before Scheduling Recordings", 496));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 497));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneHour, m_oneHour, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + m_oneHour, m_twoHour, 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_twoHour, m_oneHour, 520));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + m_oneHour, m_oneHour, 530)); // RCH
                                                                                                                    // index
                                                                                                                    // 0
            m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, now + m_twoHour, m_oneHour, 540)); // RCH
                                                                                                                    // index
                                                                                                                    // 1
            m_eventScheduler.scheduleCommand(new Record("Recording6", m_locator, now + m_oneHour, m_oneHour, 550)); // RCH
                                                                                                                    // index
                                                                                                                    // 2
            m_eventScheduler.scheduleCommand(new Record("Recording7", m_locator, now + m_twoHour, m_oneHour, 560)); // RCH
                                                                                                                    // index
                                                                                                                    // 3

            // check the recordings states
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario3 Checking states for Recording1,Recording2 and Recording3", 5996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6020));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6030));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6040));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording6",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6050));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6060));
            // Schedule the cancel

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8995));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario3 Cancelling Recording2", 8996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8997));
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("Recording2", 9000));
            // check the recordings states after the cancel.

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario3 Checking states after the Canceling of Recording2.", 11996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.CANCELLED_STATE, 12010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12020));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12030));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12040));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording6",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 12050));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 12060));
            // RCH index 4 and index 5

            // Un Register the resource contention handler by setting it to
            // NULL.
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 25000));
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 20010));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 20020));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording4", 20030));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording5", 20030));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording6", 20030));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording7", 20030));

            m_eventScheduler.run(1000);

            // Check RCH calls 4 and 5.
            String[] firstRCHNames = { "Recording4", "Recording6" };
            RCHCallInfo info = new RCHCallInfo(firstRCHNames, 4);
            boolean result = myRCH.checkCallAt(4, info);

            if (true != result)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario3 first RCH check failed.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                m_failed = TEST_FAILED;
            }

            String[] secondRCHNames = { "Recording5", "Recording7" };
            info = new RCHCallInfo(secondRCHNames, 5);
            result = myRCH.checkCallAt(5, info);

            if (true != result)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario3 second RCH check failed.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario3 completed : TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario3 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario3 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestECN1321Scenario4 extends TestCase
    {
        TestECN1321Scenario4(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Basic Conflict with cancel() BEFORE start - non interrupting - RCH.";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 50000;

            // clear the schedule of pending tasks
            reset();
            m_eventScheduler.scheduleCommand(new DeleteRecordings(300));

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 395));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario4 registering the default resourceContention handler.", 396));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 397));
            // Register the resource contention handler
            ECN1321ResourceContentionHandler myRCH = new ECN1321ResourceContentionHandler();
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(myRCH, 400));

            // Schedule 3 overlapping recordings in the future.
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 495));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario4 Before Scheduling Recordings", 496));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 497));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneHour, m_oneHour, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now, (m_oneHour * 2), 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_oneHour, m_oneHour, 520));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + m_oneHour, m_oneHour, 530));

            // check the recordings states
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5995 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario4 Checking states for Recording1,Recording2 and Recording3", 5996 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5997 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 6010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6020 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6030 + delay));
            // Schedule the cancel

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8995 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario4 Cancelling Recording1",
                    8996 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8997 + delay));
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("Recording1", 9000 + delay));
            // check the recordings states after the cancel.

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11995 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario4 Checking states after the Canceling of Recording2.", 11996 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11997 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.CANCELLED_STATE, 12000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 12010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12020 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 12030 + delay));

            // Un Register the resource contention handler by setting it to
            // NULL.
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 25000 + delay));
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 20010 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 20020 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording4", 20030 + delay));

            m_eventScheduler.run(1000);

            // Check RCH call 3 after the cancel.
            String[] firstRCHNames = { "Recording3", "Recording4" };
            RCHCallInfo info = new RCHCallInfo(firstRCHNames, 2);
            boolean result = myRCH.checkCallAt(2, info);

            if (true != result)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario4 - RCH check failed.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario4 completed : TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario4 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario4 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    /**
     * Recording should be scheduled, show in the nav list, get cancelled, and
     * show a null navigator list
     * 
     * @param locators
     */
    public class TestECN1321Scenario5 extends TestCase
    {
        TestECN1321Scenario5(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Basic Conflict with delete() BEFORE start.";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 395));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario5 registering the default resourceContention handler.", 396));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 397));

            // Register the resource contention handler
            ECN1321ResourceContentionHandler myRCH = new ECN1321ResourceContentionHandler();
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(myRCH, 400));

            // Schedule 3 overlapping recordings in the future.
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 495));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario5 Before Scheduling Recordings", 496));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 497));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneHour, m_oneHour, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + m_oneHour, m_oneHour, 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_oneHour, m_oneHour, 520));

            // check the recordings states
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario5 Checking states for Recording1,Recording2 and Recording3", 5996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6020));
            // Schedule the cancel

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8995));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario5 Deleting Recording2", 8996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8997));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 9000));
            // check the recordings states after the cancel.

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario5 Checking states after the deletion of Recording2.", 11996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12020));

            // Un Register the resource contention handler by setting it to
            // NULL.
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 25000));

            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 20020));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario5 completed : TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario5 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario5 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestECN1321Scenario6 extends TestCase
    {
        TestECN1321Scenario6(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Basic Conflict with delete() BEFORE start with RCH.";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 395));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario6 registering the default resourceContention handler.", 396));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 397));
            // Register the resource contention handler
            ECN1321ResourceContentionHandler myRCH = new ECN1321ResourceContentionHandler();
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(myRCH, 400));

            // Schedule 3 overlapping recordings in the future.
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 495));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario6 Before Scheduling Recordings", 496));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 497));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneHour, m_oneHour, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + m_oneHour, m_oneHour, 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_oneHour, m_oneHour, 520));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + m_oneHour, m_oneHour, 530));

            // check the recordings states
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario6 Checking states for Recording1,Recording2 and Recording3", 5996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6020));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6030));
            // Schedule the cancel

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8995));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario6 Cancelling Recording2", 8996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8997));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 9000));
            // check the recordings states after the cancel.

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario6 Checking states after the Canceling of Recording2.", 11996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12020));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 12030));

            // Un Register the resource contention handler by setting it to
            // NULL.
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 25000));
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 20020));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording4", 20030));

            m_eventScheduler.run(1000);

            // Check RCH call 3 after the cancel.
            String[] firstRCHNames = { "Recording3", "Recording4" };
            RCHCallInfo info = new RCHCallInfo(firstRCHNames, 2);
            boolean result = myRCH.checkCallAt(2, info);

            if (true != result)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario6 - RCH check failed.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario6 completed : TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario6 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario6 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestECN1321Scenario7 extends TestCase
    {
        static final long m_twoHour = m_oneHour * 2;

        TestECN1321Scenario7(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Basic Conflict with delete() BEFORE start with RCH.";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 395));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario7 registering the default resourceContention handler.", 396));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 397));
            // Register the resource contention handler
            ECN1321ResourceContentionHandler myRCH = new ECN1321ResourceContentionHandler();
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(myRCH, 400));

            // Schedule 3 overlapping recordings in the future.
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 495));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario7 Before Scheduling Recordings", 496));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 497));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneHour, m_oneHour, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + m_oneHour, m_twoHour, 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_twoHour, m_oneHour, 520));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + m_oneHour, m_oneHour, 530)); // RCH
                                                                                                                    // index
                                                                                                                    // 0
            m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, now + m_twoHour, m_oneHour, 540)); // RCH
                                                                                                                    // index
                                                                                                                    // 1
            m_eventScheduler.scheduleCommand(new Record("Recording6", m_locator, now + m_oneHour, m_oneHour, 550)); // RCH
                                                                                                                    // index
                                                                                                                    // 2
            m_eventScheduler.scheduleCommand(new Record("Recording7", m_locator, now + m_twoHour, m_oneHour, 560)); // RCH
                                                                                                                    // index
                                                                                                                    // 3

            // check the recordings states
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario7 Checking states for Recording1,Recording2 and Recording3", 5996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6020));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6030));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6040));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording6",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6050));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6060));
            // Schedule the cancel

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8995));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario7 Delete Recording2", 8996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8997));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 9000));
            // check the recordings states after the cancel.

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario7 Checking states after the Deletion Recording2.", 11996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12020));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12030));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12040));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording6",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 12050));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording7",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 12060));
            // RCH index 4 and index 5

            // Un Register the resource contention handler by setting it to
            // NULL.
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 25000));
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 20020));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording4", 20030));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording5", 20030));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording6", 20030));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording7", 20030));

            m_eventScheduler.run(1000);

            // Check RCH calls 4 and 5.
            String[] firstRCHNames = { "Recording4", "Recording6" };
            RCHCallInfo info = new RCHCallInfo(firstRCHNames, 4);
            boolean result = myRCH.checkCallAt(4, info);

            if (true != result)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario7 first RCH check failed.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                m_failed = TEST_FAILED;
            }

            String[] secondRCHNames = { "Recording5", "Recording7" };
            info = new RCHCallInfo(secondRCHNames, 5);
            result = myRCH.checkCallAt(5, info);

            if (true != result)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario7 second RCH check failed.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario7 completed : TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario7 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario7 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestECN1321Scenario8 extends TestCase
    {
        TestECN1321Scenario8(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Basic Conflict with delete() BEFORE start - non interrupting - RCH.";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 50000;

            // clear the schedule of pending tasks
            reset();
            m_eventScheduler.scheduleCommand(new DeleteRecordings(300));

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 395));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario8 registering the default resourceContention handler.", 396));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 397));
            // Register the resource contention handler
            ECN1321ResourceContentionHandler myRCH = new ECN1321ResourceContentionHandler();
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(myRCH, 400));

            // Schedule 3 overlapping recordings in the future.
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 495));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario8 Before Scheduling Recordings", 496));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 497));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneHour, m_oneHour, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now, (m_oneHour * 2), 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_oneHour, m_oneHour, 520));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + m_oneHour, m_oneHour, 530));

            // check the recordings states
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5995 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario8 Checking states for Recording1,Recording2 and Recording3", 5996 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5997 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 6010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6020 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6030 + delay));
            // Schedule the cancel

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8995 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario8 Cancelling Recording1",
                    8996 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8997 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 9000 + delay));
            // check the recordings states after the cancel.

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11995 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario8 Checking states after the Canceling of Recording2.", 11996 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11997 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 12010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12020 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 12030 + delay));

            // Un Register the resource contention handler by setting it to
            // NULL.
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 25000 + delay));
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 20010 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 20020 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording4", 20030 + delay));

            m_eventScheduler.run(1000);

            // Check RCH call 3 after the cancel.
            String[] firstRCHNames = { "Recording3", "Recording4" };
            RCHCallInfo info = new RCHCallInfo(firstRCHNames, 2);
            boolean result = myRCH.checkCallAt(2, info);

            if (true != result)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario8 - RCH check failed.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario8 completed : TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario8 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario8 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestECN1321Scenario9 extends TestCase
    {
        TestECN1321Scenario9(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Ongoing stop with PENDING Conflict creating RCH.";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 50000;

            // clear the schedule of pending tasks
            reset();
            m_eventScheduler.scheduleCommand(new DeleteRecordings(300));

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 395));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario9 registering the default resourceContention handler.", 396));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 397));
            // Register the resource contention handler
            ECN1321ResourceContentionHandler myRCH = new ECN1321ResourceContentionHandler();
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(myRCH, 400));

            // Schedule 3 overlapping recordings in the future.
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 495));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario9 Before Scheduling Recordings", 496));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 497));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneHour, m_oneHour, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now, (m_oneHour * 2), 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_oneHour, m_oneHour, 520));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + m_oneHour, m_oneHour, 530));

            // check the recordings states
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5995 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario9 Checking states for Recording1,Recording2 and Recording3", 5996 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5997 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 6010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6020 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6030 + delay));
            // Schedule the cancel

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8995 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario4 Cancelling Recording1",
                    8996 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 8997 + delay));
            m_eventScheduler.scheduleCommand(new StopRecording("Recording2", 9000 + delay));
            // check the recordings states after the cancel.

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11995 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario9 Checking states after the Canceling of Recording2.", 11996 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11997 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.INCOMPLETE_STATE, 12010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12020 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 12030 + delay));

            // Un Register the resource contention handler by setting it to
            // NULL.
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 25000 + delay));
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 20010 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 20020 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording4", 20030 + delay));

            m_eventScheduler.run(1000);

            // Check RCH call 3 after the cancel.
            String[] firstRCHNames = { "Recording3", "Recording4" };
            RCHCallInfo info = new RCHCallInfo(firstRCHNames, 2);
            boolean result = myRCH.checkCallAt(2, info);

            if (true != result)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario9 - RCH check failed.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario9 completed : TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario9 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario9 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestECN1321Scenario10 extends TestCase
    {
        TestECN1321Scenario10(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Actual start after requested start goes to IN_PROGRESS then COMPLETE.";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 50000;

            // clear the schedule of pending tasks
            reset();
            m_eventScheduler.scheduleCommand(new DeleteRecordings(300));

            // Schedule 3 overlapping recordings in the future.
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 495));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario10 Before Scheduling Recording", 496));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 497));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now - m_oneHour, (m_oneHour + 180000),
                    510));

            // check the recordings states
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5995 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog("TestECN1321Scenario10 Checking state for Recording1",
                    5996 + delay));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5997 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 6010 + delay));
            // Schedule the cancel

            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11995 + delay + 240000));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1321Scenario10 Checking states after the completion of Recording1.", 11996 + delay + 240000));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 11997 + delay + 240000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 12000 + delay + 240000));

            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000 + delay + 240000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario10 completed : TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1321Scenario10 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1321Scenario10 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    class ECN1321ResourceContentionHandler extends DefaultResourceContentionHandler
    {
        private Vector infoForCalls = new Vector();

        private int callIndex = 0;

        public ResourceUsage[] resolveResourceContention(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            System.out.println("ECH1321ResourceContentHandler Enter.");

            RCHCallInfo info = new RCHCallInfo(newRequest, currentReservations, callIndex++);

            ResourceUsage[] usageArray = super.resolveResourceContention(newRequest, currentReservations);

            info.setPrioritizedResult(usageArray);
            infoForCalls.add(info);

            return usageArray;
        }

        /**
         * 
         * @param index
         *            - index of the RCH call order - 0 based.
         * @param info
         *            - the RCH call info to compare
         * @return true if infos match.
         */
        public boolean checkCallAt(int index, RCHCallInfo info)
        {
            System.out.println("checkCallAt Enter.");

            synchronized (infoForCalls)
            {
                if (index > this.infoForCalls.size())
                {
                    return false;
                }
                System.out.println("---------------------------------------------------------");
                System.out.println("checkCallAt::comparing Infos: ");
                System.out.println("---------------------------------------------------------");
                System.out.println(info.toString());
                System.out.println("---------------------------------------------------------");
                System.out.println(this.infoForCalls.elementAt(index));
                System.out.println("---------------------------------------------------------");
                return ((RCHCallInfo) this.infoForCalls.elementAt(index)).equals(info);
            }
        }

        public boolean checkTestRCHCalls(Vector infos)
        {
            synchronized (infoForCalls)
            {
                if (infos.size() != infoForCalls.size())
                {

                    System.out.println("ECN1321ResourceContentionHandler::checkTestRCHCalls number of calls does not match. callcount: "
                            + this.infoForCalls.size() + " != test count:" + infos.size());
                    return false;
                }

                for (int ii = 0; ii < this.infoForCalls.size(); ii++)
                {
                    if (true != ((RCHCallInfo) this.infoForCalls.elementAt(ii)).equals(infos.elementAt(ii)))
                    {
                        System.out.println("ECN1321ResourceContentionHandler::checkTestRCHCalls call at index: " + ii
                                + " failed.");
                        return false;
                    }
                }
                return true;

            }
        }
    }

    class RCHCallInfo
    {
        private String newRequestName = null;

        private Vector currrentReservationsNames = new Vector();

        private Vector prioritizedUsageList = new Vector();

        private int callIndex = 0;

        RCHCallInfo(ResourceUsage newRequest, ResourceUsage[] currentReservations, int callIndex)
        {
            this.newRequestName = getNameForResourceUsage(newRequest);
            for (int ii = 0; ii < currentReservations.length; ii++)
            {
                this.currrentReservationsNames.add(getNameForResourceUsage(currentReservations[ii]));
            }
            this.callIndex = callIndex;
        }

        RCHCallInfo(String[] finalOrderedRRNameList, int callIndex)
        {
            for (int ii = 0; ii < finalOrderedRRNameList.length; ii++)
            {
                this.prioritizedUsageList.add(finalOrderedRRNameList[ii]);
            }
            this.callIndex = callIndex;
        }

        public void setPrioritizedResult(ResourceUsage[] prioritizedUsages)
        {
            for (int ii = 0; ii < prioritizedUsages.length; ii++)
            {
                prioritizedUsageList.add(getNameForResourceUsage(prioritizedUsages[ii]));
            }
        }

        public Vector getPrioritizedUsageNames()
        {
            return (Vector) this.prioritizedUsageList.clone();
        }

        public boolean equals(RCHCallInfo info)
        {
            Vector names = info.getPrioritizedUsageNames();
            if (names.size() != this.prioritizedUsageList.size()) return false;

            for (int ii = 0; ii < names.size(); ii++)
            {
                String string1 = (String) this.prioritizedUsageList.elementAt(ii);
                String string2 = (String) names.elementAt(ii);
                if (0 != (string1.compareTo(string2)))
                {
                    return false;
                }
            }
            return true;
        }

        public String toString()
        {
            StringBuffer buffer = new StringBuffer();
            buffer.append("RCHCallInfo - callIndex: " + this.callIndex + " Recordings in Call:\n");
            for (int ii = 0; ii < this.prioritizedUsageList.size(); ii++)
            {
                buffer.append("   " + (String) this.prioritizedUsageList.elementAt(ii) + "\n");
            }
            return buffer.toString();
        }
    }

    /**
     * Recording should be scheduled, show in the nav list, get cancelled, and
     * show a null navigator list
     * 
     * @param locators
     */
    public class TestBasicSchedule extends TestCase
    {
        TestBasicSchedule(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test Basic Schedule";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            int dvrFiles = 0;

            // clear the schedule of pending tasks
            reset();

            // Find out how many recordings there are
            dvrFiles = dumpDvrRecordings();

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneDay, m_oneHour, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + m_oneDay * 1, m_oneHour, 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_oneDay * 2, m_oneHour, 520));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + m_oneDay * 3, m_oneHour, 530));
            m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, now + m_oneDay * 4, m_oneHour, 540));

            // Count the recordings
            m_eventScheduler.scheduleCommand(new CountRecordings((dvrFiles + 5), 2000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestBasicSchedule completed: FAILED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestBasicSchedule completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    /**
     * 
     * <<<<<This test has now been obsoleted for release 0.9 and beyond>>>>
     * 
     * 
     * <<<<<This test has now been obsoleted for release 0.9 and beyond>>>>
     * 
     * Recording should be scheduled, show in the nav list, and start. Cancel
     * call should throw illegal state exception.
     * 
     * null navigator list
     * 
     * @param locators
     */
    public class TestRunningRecordAndCancel extends TestCase
    {
        TestRunningRecordAndCancel(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test Running Record and Cancel";
        }

        public void runTest()
        {
            long now = System.currentTimeMillis();

            m_failed = TEST_PASSED;
            // clear the schedule of pending tasks
            reset();

            // Schedule the record call (record start 5 secs from now, 15 sec
            // recording)
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 5000, 35000, 500));
            // Schedule the cancel (cancel 10 secs into the recording
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("Recording1", 10500));
            // check the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.CANCELLED_STATE, 20000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestRecordAndCancel testRunningRecordAndCancel completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestRecordAndCancel testRunningRecordAndCancel completed: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestRecordAndCancel testRunningRecordAndCancel completed: PASSED");
            }
        }

        private OcapLocator m_locator;
    }

}
