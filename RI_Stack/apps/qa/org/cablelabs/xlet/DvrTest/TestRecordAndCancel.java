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

import org.ocap.net.OcapLocator;
import org.ocap.dvr.OcapRecordingRequest;

/**
 * @author fsmith
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestRecordAndCancel extends DvrTest
{

    TestRecordAndCancel(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestBasicRecordAndCancel((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestBasicSchedule((OcapLocator) m_locators.elementAt(0)));

        return tests;
    }

    /**
     * Recording should be scheduled, show in the nav list, get cancelled, and
     * show a null navigator list
     * 
     * @param locators
     */
    public class TestBasicRecordAndCancel extends TestCase
    {
        TestBasicRecordAndCancel(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestBasicRecordAndCancel";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneHour, m_oneHour, 500));

            // check the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));

            // Schedule the cancel
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("Recording1", 9000));

            // check the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.CANCELLED_STATE, 12000));

            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestRecordAndCancel testBasicRecordAndCancel completed : TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestRecordAndCancel testBasicRecordAndCancel completed: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestRecordAndCancel testBasicRecordAndCancel completed: PASSED");
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
            return "TestBasicSchedule";
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
