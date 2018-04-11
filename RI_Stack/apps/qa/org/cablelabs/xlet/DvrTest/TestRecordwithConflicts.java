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
 * Created on Feb 27, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.RecordingFailedException;

import org.cablelabs.xlet.DvrTest.DvrTest.CheckFailedException;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestRecordwithConflicts extends DvrTest
{

    /**
     * @param locators
     */
    TestRecordwithConflicts(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestRecordwithConflicts1((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    public class TestRecordwithConflicts1 extends TestCase
    {
        /**
         * @param locator
         */
        public TestRecordwithConflicts1(OcapLocator locator)
        {

            m_locator = locator;
            // TODO Auto-generated constructor stub
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecordwithConflicts";
        }

        public void runTest()
        {

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long expire_time = 60 * 60 * 24; // 24 hour expiration
            long expire_pol = 0;

            // clear the schedule of pending tasks
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record(
                "Recording1", m_locator, now + 90000, 60000, 1000, 
                expire_time, 0, OcapRecordingProperties.RECORD_IF_NO_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record(
                "Recording2", m_locator, now + 90000, 60000, 2000, 
                expire_time, 0, OcapRecordingProperties.RECORD_IF_NO_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record(
                "Recording3", m_locator, now + 60000, 150000, 3000,
                expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            // Verify states prior to recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 40000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 40010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 40020));

            // Verify states after recording has started
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 80000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 80010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3", OcapRecordingRequest.IN_PROGRESS_STATE, 80020));

            // Verify states after recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.IN_PROGRESS_STATE, 135000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2", OcapRecordingRequest.IN_PROGRESS_STATE, 135010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3", OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 135020));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3", RecordingFailedException.INSUFFICIENT_RESOURCES, 150000));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3", OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 195020));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3", OcapRecordingRequest.INCOMPLETE_STATE, 215020));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3", RecordingFailedException.INSUFFICIENT_RESOURCES, 220000));

            // Delete the record calls
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 221000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 222000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 223000));

            // Schedule the deletion of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 242000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("testBasicServiceSelection completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestRecordwithConflicts  completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestRecordwithConflicts  completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        OcapLocator m_locator;
    }

}
