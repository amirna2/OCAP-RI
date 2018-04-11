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
 * Created on Sep 11, 2006
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
public class TestOverlappingRecordings extends DvrTest
{

    /**
     * @author Ryan
     * 
     *         1). Schedule a recording that is 1 minutes long by lrs to start
     *         in 1 minutes 2). Start up a buffering service context 1 minute
     *         later 3). Record an instant recording for 1 minutes 4). Verify SC
     *         is presenting and recordings are complete.
     * 
     */
    public class TestOverlappingRecsWithSC extends TestCase
    {

        private OcapLocator m_locator;

        private int m_state;

        /**
         * @param i
         * @param locator
         */
        public TestOverlappingRecsWithSC(int state, OcapLocator locator)
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
            String name = "TestOverlappingRecsWithSC";
            if (m_state == 0)
                name = name +"(both rec by loc)";
            else
                name = name +"(both rec by svcCtxt)";

            return name;
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the recordings to be ongoing
            switch (m_state)
            {
                case 0:
                    m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 60000, 60000, 1000));
                    break;
                case 1:
                    m_eventScheduler.scheduleCommand(new RecordByService("Recording1", m_locator, now + 60000, 60000, 1000));
                    break;
            }

            // Tune by Service Context and enable buffering
            // Intialize the Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));

            // Setup Buffering: 30000 min, 60000 max time (msec)
            m_eventScheduler.scheduleCommand(new setSCBuffering(300, 600, 3000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 4000));

            // Schedule the recordings while SC is presenting
            switch (m_state)
            {
                case 0:
                    m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 30000, 60000, 30000));
                    break;
                case 1:
                    m_eventScheduler.scheduleCommand(new RecordBySC("Recording2", 0, 60000, 30000));
                    break;
            }

            // Verifiying completion states of recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 140000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2", OcapRecordingRequest.COMPLETED_STATE, 145000));

            // Stop and destroy the Service Context
            m_eventScheduler.scheduleCommand(new StopBroadcastService(150000));
            m_eventScheduler.scheduleCommand(new DestroyBroadcastService(160000));

            // wait for the presenation to complete
            m_eventScheduler.run(40000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() +" completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log(getName() +" completed: FAILED - "+m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log(getName() +" completed: PASSED");
            }
        }

    }

    /**
     * @author Ryan
     * 
     *         1) Schedule 2 recordings 1.5 minute long with 40 seconds overlap
     *         2) Let execute 3) Verify completion state and playback for
     *         overlap if possible
     */
    public class TestOverlappingSchedRecs extends TestCase
    {

        private OcapLocator m_locator;

        private int m_state;

        /**
         * @param locator
         */
        public TestOverlappingSchedRecs(int state, OcapLocator locator)
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
            String name="TestOverlappingScheduledRecordings";
            switch (m_state)
            {
                case 0:
                    name = name +"(both rec by loc)";
                    break;
                case 1:
                    name = name +"(both rec by svc)";
                    break;
                case 2:
                    name = name +"(one rec by svc, the other by loc)";
                    break;
            }

            return name;
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the recordings
            switch (m_state)
            {
                case 0:
                    m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 10000, 90000, 1000));
                    m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 60000, 90000, 60000));
                    break;
                case 1:
                    m_eventScheduler.scheduleCommand(new RecordByService("Recording1", m_locator, now + 10000, 90000, 1000));
                    m_eventScheduler.scheduleCommand(new RecordByService("Recording2", m_locator, now + 60000, 90000, 60000));
                    break;
                case 2:
                    m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 10000, 90000, 1000));
                    m_eventScheduler.scheduleCommand(new RecordByService("Recording2", m_locator, now + 60000, 90000, 60000));
                    break;
            }

            // Verifiying completion states of recordings
            // Verify recording is ongoing
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 160000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2", OcapRecordingRequest.COMPLETED_STATE, 170000));

            // wait for the presenation to complete
            m_eventScheduler.run(40000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() +" completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log(getName() +" completed: FAILED " +m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log(getName() +" completed: PASSED");
            }
        }
    }

    /**
     * @param locators
     */
    TestOverlappingRecordings(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestOverlappingSchedRecs(0, (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestOverlappingSchedRecs(1, (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestOverlappingSchedRecs(2, (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestOverlappingRecsWithSC(0, (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestOverlappingRecsWithSC(1, (OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

}
