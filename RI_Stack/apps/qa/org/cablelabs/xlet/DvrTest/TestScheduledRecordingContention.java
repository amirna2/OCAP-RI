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
 * Created on Aug 17, 2005
 */

/*
 * Created on Jul 1, 2005
 *
 * 
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
 * @author cpratt, ryanh
 * 
 *         Set of test cases for scheduled recording conflict resolution.
 */

public class TestScheduledRecordingContention extends DvrTest
{
    TestScheduledRecordingContention(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestSimpleScheduledRecordingContention((OcapLocator) m_locators.elementAt(0), false));
        tests.addElement(new TestSimpleScheduledRecordingContention((OcapLocator) m_locators.elementAt(0), true));
        tests.addElement(new TestReprioritizedContention((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestReprioritizedContention1((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestReprioritizedContention2((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * Recording should be scheduled, contention handler invoked, and recordings
     * marked SCHEDULED_WITH_CONFLICT or SCHEDULED_NO_CONFLICT
     * 
     * @param locators
     */
    public class TestSimpleScheduledRecordingContention extends TestCase
    {
        private OcapLocator m_locator;
        private boolean m_rch;

        TestSimpleScheduledRecordingContention(OcapLocator locator, boolean rch)
        {
            m_locator = locator;
            m_rch = rch;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestSimpleScheduledRecordingContention(" + (m_rch ? "w/rch)" : "no rch)");
        }

        public void runTest()
        {
            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the addition of the contention handler
            if (m_rch)
            {
                m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(400));
            }

            // Schedule three 3-minute RecordingRequests to start in 1 min
            m_eventScheduler.scheduleCommand(new Record("rch3-RecFirst", m_locator, now + MINUTE, 3*MINUTE, 500));
            m_eventScheduler.scheduleCommand(new Record("rch2-RecMiddle", m_locator, now + MINUTE, 3*MINUTE, 600));
            m_eventScheduler.scheduleCommand(new Record("rch1-RecLast", m_locator, now + MINUTE, 3*MINUTE, 700));


            // Confirm rr state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("rch2-RecMiddle", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 30*SEC)); //pre-startTime
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("rch2-RecMiddle", OcapRecordingRequest.IN_PROGRESS_STATE, 93*SEC)); //post startTime

            if (m_rch) 
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("rch1-RecLast", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 31*SEC));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("rch1-RecLast", OcapRecordingRequest.IN_PROGRESS_STATE, 91*SEC));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("rch3-RecFirst", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 32*SEC));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("rch3-RecFirst", OcapRecordingRequest.FAILED_STATE, 92*SEC));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("rch1-RecLast", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 31*SEC));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("rch1-RecLast", OcapRecordingRequest.FAILED_STATE, 91*SEC));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("rch3-RecFirst", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 32*SEC));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("rch3-RecFirst", OcapRecordingRequest.IN_PROGRESS_STATE, 92*SEC));
            }

            // Clean up - Cancel scheduled recordings
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("rch3-RecFirst", 96*SEC));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("rch2-RecMiddle", 97*SEC));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("rch1-RecLast", 98*SEC));

            // Schedule the deletion of the contention handler
            if (m_rch)
                m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 99*SEC));

            m_eventScheduler.run(SEC);

            m_objects.removeAllElements();

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: FAILED - "+m_failedReason);
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        } 
    } 


    /**
     * Recording should be scheduled, contention handler invoked, and recordings
     * marked SCHEDULED_WITH_CONFLICT or SCHEDULED_NO_CONFLICT
     * 
     * @param locators
     */
    public class TestReprioritizedContention extends TestCase 
    {
        private OcapLocator m_locator;

        TestReprioritizedContention(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestReprioritizedContention";
        }

        public void runTest()
        {
            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("R4", m_locator, now + HOUR + 90*SEC, 90*SEC, 1000));
            m_eventScheduler.scheduleCommand(new Record("R2", m_locator, now + HOUR, 90*SEC, 1100));
            m_eventScheduler.scheduleCommand(new Record("R5", m_locator, now + HOUR + 90*SEC, 90*SEC, 1200));
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now + HOUR, 3*MINUTE, 1300));

            // Check Recordings for status
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 2400));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("R1", m_locator, now + HOUR, 90*SEC, 3000));

            // Check Recordings for status
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 4200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 4400));

       
            // Clean up 
            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R1", 5000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R2", 5100));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R3", 5200));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R4", 5300));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R5", 5400));

            // Schedule the deletion of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 6000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: FAILED - " +m_failedReason);
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }
    }

    /**
     * Recording should be scheduled, contention handler invoked, and recordings
     * marked SCHEDULED_WITH_CONFLICT or SCHEDULED_NO_CONFLICT
     * 
     * @param locators
     */
    public class TestReprioritizedContention1 extends TestCase 
    {
        private OcapLocator m_locator;

        TestReprioritizedContention1(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestReprioritizedContention1";
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

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("R4", m_locator, now + MINUTE, 30*SEC, 1000));
            m_eventScheduler.scheduleCommand(new Record("R2", m_locator, now + 30*SEC, 30*SEC, 1100));
            m_eventScheduler.scheduleCommand(new Record("R5", m_locator, now + MINUTE, 30*SEC, 1200));
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now + 15*SEC, MINUTE, 1300));

            // Check Recordings for status before R3's start time
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 2400));

            // Schedule another recording before R3 starts
            m_eventScheduler.scheduleCommand(new Record("R1", m_locator, now + 30*SEC, 30*SEC, 10000));

            // Check Recordings for status before R3's start time
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 12200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 12400));

            // Check Recordings for status after R3's start time
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.FAILED_STATE, 20200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20400));

            // Check Recordings for status after R1 and R2's start time
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.IN_PROGRESS_STATE, 45000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.IN_PROGRESS_STATE, 45100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.FAILED_STATE, 45200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 45300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 45400));

            // Check Recordings for status after R4 and R5's start time
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.COMPLETED_STATE, 75100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.COMPLETED_STATE, 75200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.FAILED_STATE, 75300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.IN_PROGRESS_STATE, 75400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.IN_PROGRESS_STATE, 75500));

            // Check Recordings for status after R4 and R5's end time
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.COMPLETED_STATE, 105100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.COMPLETED_STATE, 105200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.FAILED_STATE, 105300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.COMPLETED_STATE, 105400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.COMPLETED_STATE, 105500));


            // Clean up

            // Schedule the deletion of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 108000));

            m_eventScheduler.run(2000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: FAILED - "+m_failedReason);
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }
    }

    /**
     * Recording should be scheduled, contention handler invoked, and recordings
     * marked SCHEDULED_WITH_CONFLICT or SCHEDULED_NO_CONFLICT
     * 
     * @param locators
     */
    public class TestReprioritizedContention2 extends TestCase 
    {
        private OcapLocator m_locator;

        TestReprioritizedContention2(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestReprioritizedContention2";
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

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("R4", m_locator, now + 2*MINUTE, MINUTE, 1000));
            m_eventScheduler.scheduleCommand(new Record("R2", m_locator, now + MINUTE, MINUTE, 1100));
            m_eventScheduler.scheduleCommand(new Record("R5", m_locator, now + 2*MINUTE, MINUTE, 1200));
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now + 30*SEC, 2*MINUTE, 1300));

            // Check Recordings for status
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 2300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 2400));

            // Check Recordings for status after R3 has started
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 31000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.IN_PROGRESS_STATE, 31100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 31300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 31400));

            // Schedule another recording after R3 has started
            m_eventScheduler.scheduleCommand(new Record("R1", m_locator, now + MINUTE, MINUTE, 32000));

            // Check Recordings for status after R1 has been scheduled
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.IN_PROGRESS_STATE, 55200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55400));

            // Check Recordings for status after R1 and R2's start time
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.IN_PROGRESS_STATE, 110000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.IN_PROGRESS_STATE, 110100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 110200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 110300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 110400));

            // Check Recordings for status after R3 and R4's start time
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.COMPLETED_STATE, 175100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.COMPLETED_STATE, 175200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.INCOMPLETE_STATE, 175300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.IN_PROGRESS_STATE, 175400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.IN_PROGRESS_STATE, 175500));

            // Check Recordings for status after R4 and R5 are done
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.COMPLETED_STATE, 200000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.COMPLETED_STATE, 200100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.INCOMPLETE_STATE, 200200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R4", OcapRecordingRequest.COMPLETED_STATE, 200300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R5", OcapRecordingRequest.COMPLETED_STATE, 200400));


            // Clean up

            // Schedule the deletion of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 202000));

            m_eventScheduler.run(2000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: FAILED - " +m_failedReason);
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }
    }

} // END class TestScheduledRecordingContention
