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
 * Created on May 10, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestSharedResources1 extends DvrTest
{

    private OcapLocator locator;

    /**
     * @author Ryan
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public class TestRecByLoc_w_UnBufferedSC_FailedRec extends TestCase
    {
        private OcapLocator m_locator;

        TestRecByLoc_w_UnBufferedSC_FailedRec(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecByLoc_w_UnBufferedSC_FailedRec - start and end time in the past: " + m_locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("TestRecByLoc_w_UnBufferedSC_FailedRec : run test");
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // Intialize the Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext(1000));

            // Setup Buffering: 0 min, 0 max time (msec)
            m_eventScheduler.scheduleCommand(new setSCBuffering(0, 0, 2000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 3000));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 10000L, 30 * 1000, 60000));

            // Stop and destroy the Service Context
            m_eventScheduler.scheduleCommand(new StopBroadcastService(80000));
            m_eventScheduler.scheduleCommand(new DestroyBroadcastService(90000));

            // Verify recording is ongoing
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.FAILED_STATE, 100000));

            // wait for the presenation to complete
            m_eventScheduler.run(4000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }
    }

    public class TestRecByLoc_w_UnBufferedSC extends TestCase
    {
        private OcapLocator m_locator;

        TestRecByLoc_w_UnBufferedSC(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecByLoc_w_UnBufferedSC - start time in the future: " + m_locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("TestRecByLoc_w_UnBufferedSC : run test");
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // Intialize the Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext(1000));

            // Setup Buffering: 0 min, 0 max time (msec)
            m_eventScheduler.scheduleCommand(new setSCBuffering(0, 0, 2000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 3000));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 70000, 50 * 1000, 60000));

            // Stop and destroy the Service Context
            m_eventScheduler.scheduleCommand(new StopBroadcastService(80000));
            m_eventScheduler.scheduleCommand(new DestroyBroadcastService(90000));

            // Verify recording is ongoing
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 100000));

            // Verify once recording is complete that it is in the completed
            // state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 130000));

            // wait for the presenation to complete
            m_eventScheduler.run(4000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }
    }

    public class TestRecByLoc_w_BufferedSC_A extends TestCase
    {

        private OcapLocator m_locator;

        TestRecByLoc_w_BufferedSC_A(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecByLoc_w_BufferedSC_A - start time in the past: " + m_locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("TestRecByLoc_w_BufferedSC_A start: run test");
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // Intialize the Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext(1000));

            // Setup Buffering: 300 min, 2400 max time (msec)
            m_eventScheduler.scheduleCommand(new setSCBuffering(300, 2400, 2000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 3000));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000L, 90 * 1000, 75000));

            // Stop and destroy the Service Context
            m_eventScheduler.scheduleCommand(new StopBroadcastService(85000));
            m_eventScheduler.scheduleCommand(new DestroyBroadcastService(90000));

            // Verify recording is ongoing
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 100000));

            // Verify once recording is complete that it is in the completed
            // state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 130000));

            // wait for the presenation to complete
            m_eventScheduler.run(4000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }
    }

    public class TestRecByServ_w_BufferedSC_B extends TestCase
    {

        private OcapLocator m_locator;

        TestRecByServ_w_BufferedSC_B(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecByServ_w_BufferedSC_B - start time in the past: " + m_locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("TestRecByServ_w_BufferedSC_B start:");
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // Intialize the Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext(1000));

            // Setup Buffering: 300 min, 2400 max time (msec)
            m_eventScheduler.scheduleCommand(new setSCBuffering(300, 2400, 2000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 3000));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new RecordByService("Recording1", m_locator, now + 30000L, 90 * 1000,
                    75000));

            // Stop and destroy the Service Context
            m_eventScheduler.scheduleCommand(new StopBroadcastService(85000));
            m_eventScheduler.scheduleCommand(new DestroyBroadcastService(90000));

            // Verify recording is ongoing
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 100000));

            // Verify once recording is complete that it is in the completed
            // state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 130000));

            // wait for the presenation to complete
            m_eventScheduler.run(4000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }
    }

    public class TestRecByLoc_w_BufferedSC_C extends TestCase
    {

        private OcapLocator m_locator;

        TestRecByLoc_w_BufferedSC_C(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecByLoc_w_BufferedSC_C - start and end time in the past: " + m_locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("TestRecByLoc_w_BufferedSC_C start:");
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // Intialize the Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext(1000));

            // Setup Buffering: 300 min, 2400 max time (msec)
            m_eventScheduler.scheduleCommand(new setSCBuffering(300, 2400, 2000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 3000));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000L, 90 * 1000, 130000));

            // Stop and destroy the Service Context
            m_eventScheduler.scheduleCommand(new StopBroadcastService(150000));
            m_eventScheduler.scheduleCommand(new DestroyBroadcastService(160000));

            // Verify once recording is complete that it is in the completed
            // state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 170000));

            // wait for the presenation to complete
            m_eventScheduler.run(4000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }
    }

    public class TestRecByServ_w_BufferedSC_D extends TestCase
    {

        private OcapLocator m_locator;

        TestRecByServ_w_BufferedSC_D(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecByServ_w_BufferedSC_D - start and end time in the past: " + m_locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("TestRecByServ_w_BufferedSC_D start:");
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // Intialize the Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext(1000));

            // Setup Buffering: 300 min, 2400 max time (msec)
            m_eventScheduler.scheduleCommand(new setSCBuffering(300, 2400, 2000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 3000));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new RecordByService("Recording1", m_locator, now + 30000L, 90 * 1000,
                    130000));

            // Stop and destroy the Service Context
            m_eventScheduler.scheduleCommand(new StopBroadcastService(140000));
            m_eventScheduler.scheduleCommand(new DestroyBroadcastService(150000));

            // Verify once recording is complete that it is in the completed
            // state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 160000));

            // wait for the presenation to complete
            m_eventScheduler.run(4000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }
    }

    /**
     * @param locators
     */
    TestSharedResources1(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestRecByLoc_w_UnBufferedSC((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRecByLoc_w_UnBufferedSC_FailedRec((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRecByLoc_w_BufferedSC_A((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRecByServ_w_BufferedSC_B((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRecByLoc_w_BufferedSC_C((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRecByServ_w_BufferedSC_D((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }
}
