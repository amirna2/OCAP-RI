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

import java.util.Date;
import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.net.OcapLocator;

import org.cablelabs.xlet.DvrTest.DvrTest.CheckFailedException;
import org.cablelabs.xlet.DvrTest.DvrTest.ConfirmRecordingReq_CheckState;
import org.cablelabs.xlet.DvrTest.DvrTest.Record;
import org.cablelabs.xlet.DvrTest.DvrTest.RegisterResourceContentionHandler;
import org.cablelabs.xlet.DvrTest.DvrTest.Reschedule;

public class TestRecordingResourceLost extends DvrTest
{

    TestRecordingResourceLost(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestRecordingResourceLost1((OcapLocator) m_locators.elementAt(0), false));
        tests.addElement(new TestRecordingResourceLost1((OcapLocator) m_locators.elementAt(0), true));
        return tests;
    }

    /**
     * Scenario description: 3 recordings are scheduled - recording1 is placed
     * in PENDING_W_CONFLICT Recording1 starts first (marked
     * recording_with_conflicts) When recording2 and 3 start, Recording1 will
     * lose it's tuner reservation Recording1 should be stopped and marked
     * incomplete.
     * 
     * @param locators
     */
    public class TestRecordingResourceLost1 extends TestCase
    {
        private boolean m_long_rec;

        TestRecordingResourceLost1(OcapLocator locator1, boolean long_rec)
        {
            m_locator1 = locator1;
            m_long_rec = long_rec;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return m_long_rec ? "Tests in-progress recording losing resources - long recording"
                    : "Tests in-progress recording losing resources";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(new DenyRecording1RCH(), 800));

            // Scenario description:

            // Schedule the record call
            if (!m_long_rec)
            {
                m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator1, now + 30000, 90000, 1000,
                        3600000, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator1, now + 30000,
                        (4 * 24 * 60 * 60 * 1000), 1000, 3600000, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            }
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator1, now + 60000, 90000, 1010, 3600000, 0,
                    OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator1, now + 60000, 90000, 1020, 3600000, 0,
                    OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            // Check Recordings for status after scheduling
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 10000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10020));

            // Check Recordings for status after Recording1 starts
            if (m_long_rec)
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                        OcapRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE, 50000));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                        OcapRecordingRequest.IN_PROGRESS_STATE, 50000));
            }

            // Check Recordings for status after Recording2,3 start
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 90000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 90010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 90020));
            if (!m_long_rec)
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                        OcapRecordingRequest.INCOMPLETE_STATE, 130000));
            }
            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording1",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 135000));

            m_eventScheduler.scheduleCommand(new DeleteRecordings(140000));
            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("Tests in-progress recording losing resources  completed: FAILED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("Tests in-progress recording losing resources completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }

        private OcapLocator m_locator1;
    }

    /**
     * This RCH has one mission - deny "Recording1" any resources, if possible
     */
    private class DenyRecording1RCH implements org.ocap.resource.ResourceContentionHandler
    {
        public ResourceUsage[] resolveResourceContention(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            int i, j;

            DVRTestRunnerXlet.log("*** TestRecordingResourceLost contention handler called with:");

            DVRTestRunnerXlet.log("***  newRequest: " + getNameForResourceUsage(newRequest) + " (" + newRequest + ")");

            for (i = 0; i < currentReservations.length; i++)
            {
                DVRTestRunnerXlet.log("***  curRes[" + i + "]: " + getNameForResourceUsage(currentReservations[i])
                        + " (" + currentReservations[i] + ")");
            }

            ResourceUsage[] neworder = new ResourceUsage[currentReservations.length + 1];

            //
            // Insert RUs, placeing "Recording1" last, if found
            //

            j = 0;

            if (getNameForResourceUsage(newRequest) != "Recording1") neworder[j++] = newRequest;

            for (i = 0; i < currentReservations.length; i++)
            {
                if (getNameForResourceUsage(currentReservations[i]) != "Recording1")
                    neworder[j++] = currentReservations[i];
            } // END for (i)

            for (i = 0; i < currentReservations.length; i++)
            {
                if (getNameForResourceUsage(currentReservations[i]) == "Recording1")
                    neworder[j++] = currentReservations[i];
            } // END for (i)

            if (getNameForResourceUsage(newRequest) == "Recording1") neworder[j++] = newRequest;

            // Assert: neworder is sorted by key value (string name)

            DVRTestRunnerXlet.log("*** TestRecordingResourceLost contention handler returning prioritized list:");
            for (i = 0; i < neworder.length; i++)
            {
                DVRTestRunnerXlet.log("***  neworder[" + i + "]: " + getNameForResourceUsage(neworder[i]) + " ("
                        + neworder[i] + ")");
            }

            return neworder;
        } // END resolveResourceContention()

        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            // empty for now.
        }

    } // END class MyResourceContentionHandler

}
