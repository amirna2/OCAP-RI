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
 * Created on Feb 16, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.cablelabs.xlet.DvrTest;

import org.ocap.net.OcapLocator;
import java.util.Vector;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;

/**
 * @author Fred Smith
 * 
 */
public class TestRecordingAlertListener extends DvrTest implements RecordingAlertListener
{

    TestRecordingAlertListener(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestBasicRecordingAlertListener((OcapLocator) m_locators.elementAt(0), false));
        tests.addElement(new TestBasicRecordingAlertListener((OcapLocator) m_locators.elementAt(0), true));

        return tests;
    }

    /**
     * Recording should be scheduled, show in the nav list, get cancelled, and
     * show a null navigator list
     * 
     * @param locators
     */
    public class TestBasicRecordingAlertListener extends TestCase
    {
        TestBasicRecordingAlertListener(OcapLocator locator, boolean delRec)
        {
            m_locator = locator;
            m_deletePendingRec = delRec;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestBasicRecordingAlertListener" +(m_deletePendingRec ? " (delete a pending recording)" : "");
        }

        public void runTest()
        {
            m_alertCount = 0;
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            int expectedAlertCt = 2;

            // clear the schedule of pending tasks
            reset();
            OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            orm.addRecordingAlertListener(TestRecordingAlertListener.this);

            //
            // Schedule 3 recordings. Cancel 1 recording. Check the alert
            // count after 2 recordings should have completed.  
            // The count should equal 2. Then, clean up and finish.

            // schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30*SEC, 60*SEC, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 60*SEC, 60*SEC, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_oneDay, m_oneHour, 500));

m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 24000));
m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 24500));
m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 25000));
m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.IN_PROGRESS_STATE, 65*SEC));

            // schedule the delete
            if (m_deletePendingRec)
            {
                expectedAlertCt = 1;
                m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 56 * 1000));
            }

            // schedule the alert count check
            m_eventScheduler.scheduleCommand(new CheckAlertCount(expectedAlertCt, 70000));

            // Delete ongoing recordings
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 75000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 76000));

            m_eventScheduler.run(1000);

            orm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            orm.removeRecordingAlertListener(TestRecordingAlertListener.this);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("TestRecordingAlertListener testBasicRecordingAlertListener completed: FAILED");
            }
            else
            {
                DVRTestRunnerXlet.log("TestRecordingAlertListener testBasicRecordingAlertListener completed: PASSED");
            }
        }

        private OcapLocator m_locator;
        private boolean m_deletePendingRec;
    }

    /**
     * Schedules a delete of all recordings in the recording DB
     */
    class CheckAlertCount extends EventScheduler.NotifyShell
    {
        CheckAlertCount(int count, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_expectedCount = count;
        }

        public void ProcessCommand()
        {
            // confirm that we've received the expected number of recording
            // alerts
            if (m_alertCount != m_expectedCount)
            {
                DVRTestRunnerXlet.log("CheckAlertCount failed! Expected " + m_expectedCount + " Received"
                        + m_alertCount);
                m_failed = TEST_FAILED;
            }
        }

        private int m_expectedCount;
    }

    public void recordingAlert(RecordingAlertEvent e)
    {
        DVRTestRunnerXlet.log("Recording Alert Event Recieved!");
        m_alertCount++;
    }

    private int m_alertCount = 0;
}
