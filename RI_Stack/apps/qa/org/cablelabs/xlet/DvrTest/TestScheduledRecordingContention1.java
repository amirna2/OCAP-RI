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
 * Created on Oct 13, 2005
 * Tests 5-8 of contention tests written by Donald Murray.
 */

package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.dvr.OcapRecordingRequest;

/**
 * @author donaldm
 * 
 *         Set of test cases for scheduled recording conflict resolution.
 */

public class TestScheduledRecordingContention1 extends DvrTest
{
    TestScheduledRecordingContention1(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestRRContention1((OcapLocator) m_locators.elementAt(0), false));
        tests.addElement(new TestRRContention1((OcapLocator) m_locators.elementAt(0), true));
        tests.addElement(new TestRRContention2((OcapLocator) m_locators.elementAt(0), false));
        tests.addElement(new TestRRContention2((OcapLocator) m_locators.elementAt(0), true));
        tests.addElement(new TestRRContention3((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRRContention4((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRRContention5((OcapLocator) m_locators.elementAt(0), false));
        tests.addElement(new TestRRContention5((OcapLocator) m_locators.elementAt(0), true));

        return tests;
    }

    public class TestRRContention1 extends TestCase
    {
        private OcapLocator m_locator;
        private boolean m_changeR3;

        TestRRContention1(OcapLocator locator, boolean changeWConflictRec)
        {
            m_locator = locator;
            m_changeR3 = changeWConflictRec;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRRContention1" +(m_changeR3 ? 
                   "(change recording in WITH_CONFLICT state)" : "");
        }

        public void runTest()
        {
            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(500));

            // Schedule four 3 mins long recording; first 3 starts in 1 hour
            // last recording starts immediately after
            m_eventScheduler.scheduleCommand(new Record("R1", m_locator, now + HOUR, 3*MINUTE, 1100));
            m_eventScheduler.scheduleCommand(new Record("R2", m_locator, now + HOUR, 3*MINUTE, 1200));
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now + HOUR, 3*MINUTE, 1300));
            m_eventScheduler.scheduleCommand(new Record("R4", m_locator, now + HOUR + 3*MINUTE, 3*MINUTE, 1100));

            // Check Recordings for status before reschedule
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 2300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2400));

            String recName = "R1";
            String otherRecName = "R3";
            if (m_changeR3) 
            {
                recName = "R3";
                otherRecName = "R1";
            }  


            // resolve conflict by rescheduling 
            m_eventScheduler.scheduleCommand(new Reschedule(recName, now + HOUR + 3*MINUTE, 3*MINUTE, 3000));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recName, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(otherRecName, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4300));

            // reschedule recoridng to be like original
            m_eventScheduler.scheduleCommand(new Reschedule(recName, now + HOUR, 3*MINUTE, 5000));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6300));


            // resolve conflict by deleting recording
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recName, 7000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(otherRecName, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8300));
             
            // recreate the deleted recording
            m_eventScheduler.scheduleCommand(new Record(recName, m_locator, now + HOUR, 3*MINUTE, 9000));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 10200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10300));


            // resolve conflict by cancelling recording
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest(recName, 11000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recName, OcapRecordingRequest.CANCELLED_STATE, 12100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(otherRecName, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12200));



            /**** Clean up ****/
            // Clean up - Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R1", 13100));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R2", 13200));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R3", 13300));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R4", 13400));


            // Schedule the deletion of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 14000));

            m_eventScheduler.run(2000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName()+"completed: FAILED - "+m_failedReason);
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + "completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        } 
    } 


    public class TestRRContention2 extends TestCase
    {
        private OcapLocator m_locator;
        private boolean m_changeR3;

        TestRRContention2(OcapLocator locator, boolean changeWConflictRec)
        {
            m_locator = locator;
            m_changeR3 = changeWConflictRec;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRRContention2" + (m_changeR3 ? 
                   "(change recording in WITH_CONFLICT state)" : "");
        }

        public void runTest()
        {
            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(500));

            // Schedule four 3 mins long recording; first 2 in parallel, last
            // two in series that spans the duration of the first two.
            m_eventScheduler.scheduleCommand(new Record("R1", m_locator, now + HOUR, 3*MINUTE, 1000));
            m_eventScheduler.scheduleCommand(new Record("R2", m_locator, now + HOUR, 3*MINUTE, 1100));
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now + HOUR, 90*SEC, 1200));
            m_eventScheduler.scheduleCommand(new Record("R4", m_locator, now + HOUR + 90*SEC, 90*SEC, 1300));

            // Check Recordings for status before reschedule
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 2200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 2300));


            String recName = "R1";
            String otherRecName = "R3";
            int r4State = OcapRecordingRequest.PENDING_NO_CONFLICT_STATE;
            long recDuration = 3*MINUTE;
            if (m_changeR3) 
            {
                recName = "R3";
                otherRecName = "R1";
                r4State = OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE;
                recDuration = 90*SEC;
            }  


            //Resolve contention by rescheduling
            m_eventScheduler.scheduleCommand(new Reschedule(recName, now + HOUR + 3*MINUTE, 3*MINUTE, 3000));
            //check recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recName, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(otherRecName, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", r4State, 4300));

            // reschedule back to original state
            m_eventScheduler.scheduleCommand(new Reschedule(recName, now + HOUR, recDuration, 5000));
            // Check Recordings state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6400));


            // resolve conflict by deleting Recording
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recName, 7000));
            // check recoridng state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(otherRecName, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", r4State, 8200));

            // Schedule the deleted recording back
            m_eventScheduler.scheduleCommand(new Record(recName, m_locator, now + HOUR, recDuration, 9000));
            // Check recordings state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 10200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 10300));


            //Resolve conflict by Cancelling recording
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest(recName, 11000));
            //check recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recName, OcapRecordingRequest.CANCELLED_STATE, 12100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(otherRecName, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", r4State, 12200));



            /**** Clean up ****/
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R1", 13100));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R2", 13200));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R3", 13300));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R4", 13400));


            // Schedule the deletion of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 14000));

            m_eventScheduler.run(2000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName()+"completed: FAILED - "+m_failedReason);
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + "completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        } 
    }

    public class TestRRContention3 extends TestCase
    {
        private OcapLocator m_locator;

        TestRRContention3(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRRContention3";
        }

        public void runTest()
        {
            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(500));

            // Schedule five 3 mins long recording: 
            // R1&R2 and R4&R5 are in series while R3 spans in the middle
            m_eventScheduler.scheduleCommand(new Record("R1", m_locator, now + HOUR, 3*MINUTE, 1000));
            m_eventScheduler.scheduleCommand(new Record("R2", m_locator, now + HOUR + 3*MINUTE, 3*MINUTE, 1100));
            m_eventScheduler.scheduleCommand(new Record("R4", m_locator, now + HOUR, 3*MINUTE, 1200));
            m_eventScheduler.scheduleCommand(new Record("R5", m_locator, now + HOUR + 3*MINUTE, 3*MINUTE, 1300));
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now + HOUR + 90*SEC, 3*MINUTE, 1400));

            // Check Recordings for status before reschedule
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 2300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 2400));


            // Resolve conflict by rescheduling Recording
            m_eventScheduler.scheduleCommand(new Reschedule("R3", now + 30*MINUTE, 3*MINUTE, 3000));
            // Confirm recoding state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4400));

            // rescheduling Recording back to original state
            m_eventScheduler.scheduleCommand(new Reschedule("R3", now + HOUR + 90*SEC, 3*MINUTE, 5000));
            // confirm recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6400));

            
            // Resolve conflict by Deleting recording
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R3", 7000));
            // Confirm recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8500));
           
            // Schedule R3 to put everything back to original state
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now + HOUR + 90*SEC, 3*MINUTE, 9000));
            // confirm recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 10300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 10400));


            // Resolve conflict by Cancelling recording
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("R3", 11000));
            //confirm recording states
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.CANCELLED_STATE, 12000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12400));



            /**** Clean up ****/
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R1", 13100));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R2", 13200));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R4", 13300));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R5", 13400));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R3", 13500));


            // Schedule the deletion of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 14000));

            m_eventScheduler.run(2000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName()+"completed: FAILED - "+m_failedReason);
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + "completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        } 
    } 

    public class TestRRContention4 extends TestCase
    {
        private OcapLocator m_locator;

        TestRRContention4(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRRContention4";
        }

        public void runTest()
        {
            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(500));

            // Schedule three pairs of 3 mins long recording in series:
            //  R1&R5 followed by R3&R4 followed R2&R6
            m_eventScheduler.scheduleCommand(new Record("R1", m_locator, now + HOUR, 3*MINUTE, 1000));
            m_eventScheduler.scheduleCommand(new Record("R4", m_locator, now + HOUR + 3*MINUTE, 3*MINUTE, 1100));
            m_eventScheduler.scheduleCommand(new Record("R2", m_locator, now + HOUR + 6*MINUTE, 3*MINUTE, 1200));
            m_eventScheduler.scheduleCommand(new Record("R5", m_locator, now + HOUR, 3*MINUTE, 1300));
            m_eventScheduler.scheduleCommand(new Record("R6", m_locator, now + HOUR + 6*MINUTE, 3*MINUTE, 1400));
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now + HOUR + 3*MINUTE, 3*MINUTE, 1500));

            // Check Recordings for status
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2500));


            // Reschedule R3 to make it start earlier (w/original end time)
            // to cause contention in a recording (R5)
            m_eventScheduler.scheduleCommand(new Reschedule("R3", now + HOUR + 90*SEC, 4*MINUTE + 30*SEC, 3000));
            // Check Recordings for status
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 4400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4500));


            // Resolve conflict by rescheduling recording
            m_eventScheduler.scheduleCommand(new Reschedule("R3", now + HOUR + 3*MINUTE, 3*MINUTE, 5000));
            // Check Recording states after delete/cancel/reschedule R3
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6500));


            // Reschedule R3 to make it start earlier and end later
            // so it overlaps 2 other recording on both ends to cause
            // contentions to two recordings (R5 & R6)
            m_eventScheduler.scheduleCommand(new Reschedule("R3", now + HOUR + 90*SEC, 6*MINUTE + 30*SEC, 7000));
            // confirm recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 8400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 8500));

            // Reschedule R3 to make it run longer (w/original start time)
            // to cause contention in a different recording (R6)
            m_eventScheduler.scheduleCommand(new Reschedule("R3", now + HOUR + 3*MINUTE, 4*MINUTE + 30*SEC, 9000));
            // confirm recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10500));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 10600));


            //resolve conflict by deleting recording
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R3", 11000));
            // confirm recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12500));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 12600));
         
            // Schedule Recording 3 again to introduce conflict again
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now + HOUR + 90*SEC, 4*MINUTE, 13000));
            // confirm recording state 
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 14000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 14100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 14200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 14300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 14400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 14500));


            // Resolve conflict by cancelling recording
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("R3", 15000));
            // confirm recording state 
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 16000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 16100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.CANCELLED_STATE, 16200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 16300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 16400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 16500));



            /**** Clean up ****/
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R1", 17000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R2", 17100));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R3", 17200));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R4", 17300));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R5", 17400));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R6", 17500));


            // Schedule the deletion of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 18000));


            m_eventScheduler.run(2000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName()+"completed: FAILED - "+m_failedReason);
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + "completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        } 
    } 


    /**
     * This test involves 6 recordings. Recording 1 and 2 are in parallel using
     * 2 tuners. Recordings 3 and 4 are in series with recordings 1 and 2
     * respectively and parallel with one another. Recording 5 begins before
     * everything else and intersects the time of Recordings 1 and 2. Finally
     * Recording 6 starts before the end of recordings 1 and 2 and ends before
     * the end of recordings 3 and 4. Finally, before checking the final status,     * Recording 1 is Deleteled. The result should be that Recording 2,3,4 and 5
     * are in PendingNoConflict state while Recording 6 should be in
     * PendingWithConflict state as it interferes with Recordings 3 and 4. That
     * is 3 recordings with only 2 tuners, hence the conflict.
     * 
     * @param locators
     */
    public class TestRRContention5 extends TestCase
    {
        private OcapLocator m_locator;
        private boolean m_change2Recs;

        TestRRContention5(OcapLocator locator, boolean change2Recs)
        {
            m_locator = locator;
            m_change2Recs = change2Recs;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRRContention5" + (m_change2Recs ? 
                   "(change 2 recordings)" : "(Change 1 recording)");
        }

        public void runTest()
        {
            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(500));

            // Schedule the 6 recordings.
            m_eventScheduler.scheduleCommand(new Record("R1", m_locator, now + HOUR, HOUR, 1000)); 
            m_eventScheduler.scheduleCommand(new Record("R2", m_locator, now + HOUR, HOUR, 1100));  
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now + 2*HOUR, 30*MINUTE, 1200));
            m_eventScheduler.scheduleCommand(new Record("R4", m_locator, now + 2*HOUR, 30*MINUTE, 1300));
            m_eventScheduler.scheduleCommand(new Record("R5", m_locator, now + 30*MINUTE, HOUR, 1400));  
            m_eventScheduler.scheduleCommand(new Record("R6", m_locator, now + HOUR + 56*MINUTE, 30*MINUTE, 1500)); 

            // Check Recordings for status before reschedule
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 2400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 2500));


            int changeTrigger = 2000;
            int r6State = OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE;
            if (m_change2Recs)
            {
                changeTrigger = 0;
                r6State = OcapRecordingRequest.PENDING_NO_CONFLICT_STATE;
            }


            // Resolve conflict by rescheduling recording(s)
            m_eventScheduler.scheduleCommand(new Reschedule("R1", now + 2*HOUR + 30*MINUTE, HOUR, 3000));
            m_eventScheduler.scheduleCommand(new Reschedule("R4", now + 2*HOUR + 30*MINUTE, 30*MINUTE, 3100+changeTrigger));

            // Confirm recoridng state 
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", r6State, 4500));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6500));

            //Reschedule R1 and R4 again back to original
            m_eventScheduler.scheduleCommand(new Reschedule("R1", now + HOUR, HOUR, 7000));
            m_eventScheduler.scheduleCommand(new Reschedule("R4", now + 2*HOUR, 30*MINUTE, 7100));
            // Check Recordings state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 8300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 8400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 8500));



            // Resolve conflict by Deleting recording(s)
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R1", 9000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R4", 9100 + changeTrigger)); 
            //confirm recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", r6State, 10500));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12500));
            
            // schedule R1 and R4 again to put everything back to original state
            m_eventScheduler.scheduleCommand(new Record("R1", m_locator, now + HOUR, HOUR, 13000)); 
            m_eventScheduler.scheduleCommand(new Record("R4", m_locator, now + 2*HOUR, 30*MINUTE, 13100));
            // Check Recordings for status before reschedule
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 14000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 14100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 14200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 14400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 14500));



            // Resolve conflict by CANCELLING recording
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("R1", 15000));
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("R4", 15100 + changeTrigger));
            //confirm recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.CANCELLED_STATE, 16000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 16100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 16200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 16400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", r6State, 16500));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.CANCELLED_STATE, 18000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 18100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 18200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.CANCELLED_STATE, 18300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 18400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R6", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 18500));



            /**** Clean up ****/
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R1", 19000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R2", 19100));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R3", 19200));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R4", 19300));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R5", 19400));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R6", 19500));


            // Schedule the deletion of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 20000));

            m_eventScheduler.run(2000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName()+"completed: FAILED - "+m_failedReason);
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + "completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }
    } 
} 
