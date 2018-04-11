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
 * Created on Aug 28, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.io.Serializable;
import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;

import org.cablelabs.xlet.DvrTest.DvrTest.TestCase;
import org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestPersistentRecordingsCheck extends DvrTest
{

    /**
     * @param locators
     */
    TestPersistentRecordingsCheck(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestInterruptedRecordingCheck_wConflicts());
        tests.addElement(new TestInterruptedRecordingCheck_noConflicts());
        tests.addElement(new TestPersistentContentionCheck(false));
        tests.addElement(new TestPersistentContentionCheck(true));
        return tests;
    }

    /**
     * @author Ryan
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public void getRecReq(String recName, String key, String data)
    {
        // TODO Auto-generated constructor stub

        System.out.println("<<<<<<<<< getRecReq CALLED >>>>>>>>>>>>>>>>");

        OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
        RecordingList rl = rm.getEntries();

        if (rl == null)
        {
            System.out.println("<<<<getRecReq: RecordingList = null!");
            m_failed = TEST_FAILED;
            return;
        }

        System.out.println("<<<<<<<<< getRecReq : " + rl.size() + " entries >>>>>>>>>>>>>>>>");

        for (int x = 0; x < rl.size(); x++)
        {
            try
            {
                RecordingRequest rr = rl.getRecordingRequest(x);
                String info = (String) rr.getAppData(key);
                System.out.println("<<<<<<<<<ENTRY DATA :" + info + " >>>>>>>>>>>");
                if ((info != null) && (info.equals(data)))
                {
                    DVRTestRunnerXlet.log("<<<< REQUEST FOUND! giving request the name : " + recName + " >>>>");
                    insertObject(rr, recName);
                    return;
                }
                else
                {
                    System.out.println("<<<<< ENTRY " + x + " not it!>>>>>");
                }
            }
            catch (Exception e)
            {
                System.out.println("<<<<getRecReq: Exception finding recording request" + rl.getRecordingRequest(x));
                m_failed = TEST_FAILED;
                m_failedReason = "getRecReq: Exception finding recording request" + rl.getRecordingRequest(x) + ": "
                        + e.getMessage();
                e.printStackTrace();
            }
        }
    }

    public class TestInterruptedRecordingCheck_wConflicts extends TestCase
    {
        TestInterruptedRecordingCheck_wConflicts()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestInterruptedRecordingCheck_wConflicts";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            // clear the schedule of pending tasks
            reset();

            int[] rec5state = new int[2];
            rec5state[0] = OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE;
            rec5state[1] = OcapRecordingRequest.INCOMPLETE_STATE;

            // Recall the recordings
            getRecReq("Recording0", "interrupted_w_conflict", "Recording0");
            getRecReq("Recording1", "interrupted_w_conflict", "Recording1");
            getRecReq("Recording2", "interrupted_w_conflict", "Recording2");
            getRecReq("Recording3", "interrupted_w_conflict", "Recording3");
            getRecReq("Recording4", "interrupted_w_conflict", "Recording4");
            getRecReq("Recording5", "interrupted_w_conflict", "Recording5");
            getRecReq("Recording6", "interrupted_w_conflict", "Recording6");

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording0",
                    OcapRecordingRequest.CANCELLED_STATE, 1000));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 1100));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 1200));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.INCOMPLETE_STATE, 1300));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.FAILED_STATE, 1400));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckStateOneOf("Recording5", rec5state, 1500));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording6",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 1600));
            m_eventScheduler.run(5000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestInterruptedRecordingCheck_wConflicts: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestInterruptedRecordingCheck_wConflicts: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestInterruptedRecordingCheck_wConflicts: PASSED");
            }
        }
    }

    public class TestInterruptedRecordingCheck_noConflicts extends TestCase
    {
        TestInterruptedRecordingCheck_noConflicts()
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestInterruptedRecordingCheck_noConflicts";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            // clear the schedule of pending tasks
            reset();

            // Recall the recordings
            getRecReq("Recording0", "interrupted", "Recording0");
            getRecReq("Recording2", "interrupted", "Recording2");
            getRecReq("Recording3", "interrupted", "Recording3");
            getRecReq("Recording4", "interrupted", "Recording4");
            getRecReq("Recording5", "interrupted", "Recording5");
            getRecReq("Recording6", "interrupted", "Recording6");

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording0",
                    OcapRecordingRequest.CANCELLED_STATE, 1000));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 1200));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.INCOMPLETE_STATE, 1300));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.FAILED_STATE, 1400));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.FAILED_STATE, 1500));
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording6",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 1600));
            m_eventScheduler.run(5000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestInterruptedRecordingCheck_noConflicts: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestInterruptedRecordingCheck_noConflicts: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestInterruptedRecordingCheck_noConflicts: PASSED");
            }
        }
    }

    /**
     * @author Ryan
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */

    public class TestPersistentContentionCheck extends TestCase
    {
        private boolean m_wConflict;

        TestPersistentContentionCheck(boolean wConflict)
        {
            m_wConflict = wConflict;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestPersistentContentionCheck : " + (m_wConflict ? "with conflict" : "if no conflict");
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // Find the recordings
            if (m_wConflict)
            {
                getRecReq("Recording1", "contention_w_conflict", "Recording1");
                getRecReq("Recording2", "contention_w_conflict", "Recording2");
                getRecReq("Recording3", "contention_w_conflict", "Recording3");
            }
            else
            {
                getRecReq("Recording1", "contention", "Recording1");
                getRecReq("Recording2", "contention", "Recording2");
                getRecReq("Recording3", "contention", "Recording3");
            }

            // Get state of recordings
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            OcapRecordingRequest[] rr = new OcapRecordingRequest[3];
            rr[0] = (OcapRecordingRequest) findObject("Recording1");
            rr[1] = (OcapRecordingRequest) findObject("Recording2");
            rr[2] = (OcapRecordingRequest) findObject("Recording3");

            // Verify the ReccordingFailedException is appropriate
            for (int i = 0; i < rr.length; i++)
            {
                if ((OcapRecordingRequest.INCOMPLETE_STATE == rr[i].getState())
                        || (OcapRecordingRequest.FAILED_STATE == rr[i].getState())
                        || (OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE == rr[i].getState()))
                {
                    RecordingFailedException exception = (RecordingFailedException) rr[i].getFailedException();
                    int reasonCode = exception.getReason();
                    DVRTestRunnerXlet.log("Recording" + (i + 1) + " - RFException."
                            + DvrEventPrinter.xletRecFailedRsn(reasonCode));
                }

            }

            // Print out results
            DVRTestRunnerXlet.log("Disk fill - recording information");
            DVRTestRunnerXlet.log("Recording 1 - state: " + DvrEventPrinter.xletState(rr[0]));
            DVRTestRunnerXlet.log("Recording 2 - state: " + DvrEventPrinter.xletState(rr[1]));
            DVRTestRunnerXlet.log("Recording 3 - state: " + DvrEventPrinter.xletState(rr[2]));
        }
    }

}
