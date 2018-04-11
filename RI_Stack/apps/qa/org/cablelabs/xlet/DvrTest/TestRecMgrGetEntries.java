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
 * Created on Jul 2, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.net.OcapLocator;

/**
 * @author jspruiel
 * 
 *         1. RecordingManager.getEntries(new
 *         RecordingStateFilter(LeafRecordingRequest.COMPLETED)) returns a list
 *         of completed recordings.
 * 
 *         Strategy: Create a few completed recording in addition those that may
 *         already reside on disk.
 * 
 *         Create a RecordingStateFilter to pass to the getEntries method.
 */
public class TestRecMgrGetEntries extends DvrTest
{

    TestRecMgrGetEntries(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestRecMgrGetEntriesFilter((OcapLocator) m_locators.elementAt(0), OcapRecordingRequest.PENDING_NO_CONFLICT_STATE));
        tests.addElement(new TestRecMgrGetEntriesFilter((OcapLocator) m_locators.elementAt(0), OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE));
        tests.addElement(new TestRecMgrGetEntriesFilter((OcapLocator) m_locators.elementAt(0), OcapRecordingRequest.IN_PROGRESS_STATE));
        tests.addElement(new TestRecMgrGetEntriesFilter((OcapLocator) m_locators.elementAt(0), OcapRecordingRequest.INCOMPLETE_STATE));
        tests.addElement(new TestRecMgrGetEntriesFilter((OcapLocator) m_locators.elementAt(0), OcapRecordingRequest.FAILED_STATE));
        tests.addElement(new TestRecMgrGetEntriesFilter((OcapLocator) m_locators.elementAt(0), OcapRecordingRequest.COMPLETED_STATE));
        return tests;
    }

    public class TestRecMgrGetEntriesFilter extends TestCase
    {
        TestRecMgrGetEntriesFilter(OcapLocator locator, int state)
        {
            m_locator = locator;
            m_state = state;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            String stateString = (DvrEventPrinter.xletLeafState(m_state));
            stateString = stateString.substring(stateString.indexOf(".")+1);

            return "TestRecMgrGetEntriesFilter (" +stateString +")";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            long duration = MINUTE;
            long triggerTime = 500;
            int expectedCt = 7;

            // delete all recordings
            OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            try
            {
                orm.deleteAllRecordings();
            }
            catch (Exception e)
            {
                System.out.println(getName() +" failure trying to delete all the recordings: " +e);
            }
             
            // clear the schedule of pending tasks
            reset();

            int initRecCt = countRecordingsByState(m_state);
            System.out.println(getName() +": initial number of recordings in state "+m_state +" is " +initRecCt);

            // Schedule three 30 seconds long recordings that overlap
            // assuming 2-tuner platform, Recording1a and Recording1b should
            // complete; Recording1c should fail
            m_eventScheduler.scheduleCommand(new Record("R1a", m_locator, now + 30*SEC, duration, triggerTime+10));
            m_eventScheduler.scheduleCommand(new Record("R1b", m_locator, now + 40*SEC, duration, triggerTime+20));
            m_eventScheduler.scheduleCommand(new Record("R1c", m_locator, now + 50*SEC, duration, triggerTime+30));

            // Schedule five 30 seconds long recordings to start back-to-back
            m_eventScheduler.scheduleCommand(new Record("R2", m_locator, now + 3*30000, duration, triggerTime+40 ));
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now + 4*30000, duration, triggerTime+50));
            m_eventScheduler.scheduleCommand(new Record("R4", m_locator, now + 5*30000, duration, triggerTime+60));
            m_eventScheduler.scheduleCommand(new Record("R5", m_locator, now + 6*30000, duration, triggerTime+70));
            m_eventScheduler.scheduleCommand(new Record("R6", m_locator, now + 7*30000, duration, triggerTime+80));

            triggerTime = 2000;
            switch (m_state)
            {
                //OcapRecordingRequest.PENDING_NO_CONFLICT_STATE:
                // expect 7 (Recording1a, 1b, 2-6) 

                case OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                    //Recording1c
                    expectedCt = 1;  
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R1c", OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, triggerTime));
                    break;

                case OcapRecordingRequest.IN_PROGRESS_STATE:
                    //check while the 3 Recording1s overlapped
                    triggerTime = triggerTime + 57*SEC;
                    expectedCt = 2;
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R1a", OcapRecordingRequest.IN_PROGRESS_STATE, triggerTime));
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R1b", OcapRecordingRequest.IN_PROGRESS_STATE, triggerTime+10));
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R1c", OcapRecordingRequest.FAILED_STATE, triggerTime+20));
                    break;

                case OcapRecordingRequest.INCOMPLETE_STATE:
                    //check while the 3 Recording1s overlapped
                    triggerTime = triggerTime + 55000;
                    expectedCt = 0;
                    break;

                case OcapRecordingRequest.FAILED_STATE:
                    //check after the 3 overlapping Recording1s are done
                    //Recording1c should fail due to INSUFFICIENT_RESOURCE
                    triggerTime = triggerTime + 85000;
                    expectedCt = 1;
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R1c", OcapRecordingRequest.FAILED_STATE, triggerTime));
                    break;

                case OcapRecordingRequest.COMPLETED_STATE:
                    // 7 completed recs: R1a, R1b, R2-R6
                    triggerTime = triggerTime + 9*30000;
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R1a", OcapRecordingRequest.COMPLETED_STATE, triggerTime));
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R1b", OcapRecordingRequest.COMPLETED_STATE, triggerTime+10));
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R2", OcapRecordingRequest.COMPLETED_STATE, triggerTime+20));
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R3", OcapRecordingRequest.COMPLETED_STATE, triggerTime+30));
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R4", OcapRecordingRequest.COMPLETED_STATE, triggerTime+40));
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R5", OcapRecordingRequest.COMPLETED_STATE, triggerTime+50));
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "R6", OcapRecordingRequest.COMPLETED_STATE, triggerTime+60));
                    break;

                default:
                    break;
            }

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingStateCount(
                    initRecCt + expectedCt, m_state, triggerTime+100));

            m_eventScheduler.scheduleCommand(new FilterState(m_state, expectedCt, triggerTime+1000));


            //clean up
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R1a", triggerTime+2000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R1b", triggerTime+2100));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R1c", triggerTime+2200));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R2", triggerTime+2300));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R3", triggerTime+2400));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R4", triggerTime+2500));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R5", triggerTime+2600));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R6", triggerTime+2700));

            m_eventScheduler.run(10000);

            m_objects.removeAllElements();

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() +": TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log(getName() +": FAILED - "+m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log(getName() +" completed: PASSED");
            }
        }

        private OcapLocator m_locator;
        private int m_state;
    }
}
