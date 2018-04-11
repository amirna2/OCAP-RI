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

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.net.OcapLocator;

/**
 * @author fsmith
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestConsecutiveRecordings extends DvrTest
{

    TestConsecutiveRecordings(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestBasicConsecutiveRecordings((OcapLocator) m_locators.elementAt(0), 60000));
        //tests.addElement(new TestBasicConsecutiveRecordings((OcapLocator) m_locators.elementAt(0), 40000));
        tests.addElement(new TestBasicConsecutiveRecordings2((OcapLocator) m_locators.elementAt(0), (OcapLocator) m_locators.elementAt(1)));
        tests.addElement(new TestSimultaneousRecordings((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * Perform a collection of scheduled recordings, and confirm their
     * successful completion
     * 
     * @param locators
     */
    public class TestBasicConsecutiveRecordings extends TestCase
    {
        TestBasicConsecutiveRecordings(OcapLocator locator, long recDuration)
        {
            m_locator = locator;
            m_recDuration = recDuration;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestBasicConsecutiveRecordings(1 loctor)";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_recDuration, m_recDuration, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 2 * m_recDuration, m_recDuration, 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 3 * m_recDuration, m_recDuration, 520));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + 4 * m_recDuration, m_recDuration, 530));
            m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, now + 5 * m_recDuration, m_recDuration, 540));
            m_eventScheduler.scheduleCommand(new Record("Recording6", m_locator, now + 6 * m_recDuration, m_recDuration, 550));
            m_eventScheduler.scheduleCommand(new Record("Recording7", m_locator, now + 7 * m_recDuration, m_recDuration, 560));
            m_eventScheduler.scheduleCommand(new Record("Recording8", m_locator, now + 8 * m_recDuration, m_recDuration, 570));

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingStateCount(
                countRecordingsByState(OcapRecordingRequest.COMPLETED_STATE) + 8, OcapRecordingRequest.COMPLETED_STATE, 9 * m_recDuration + 500));

            m_eventScheduler.run(1000);

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

        private OcapLocator m_locator;
        private long m_recDuration;
    }

    /**
     * Perform a collection of scheduled recordings, and confirm their
     * successful completion Similar to the single locator version, but this
     * test uses distict service locators
     * 
     * @param locators
     */
    public class TestBasicConsecutiveRecordings2 extends TestCase
    {
        TestBasicConsecutiveRecordings2(OcapLocator locator1, OcapLocator locator2)
        {
            m_locator1 = locator1;
            m_locator2 = locator2;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestBasicConsecutiveRecordings2(2 distinct loctors)";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator1, now + 10*SEC, 30*SEC, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator2, now + MINUTE, 30*SEC, 510));

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingStateCount(
                    countRecordingsByState(OcapRecordingRequest.COMPLETED_STATE) + 2, OcapRecordingRequest.COMPLETED_STATE, 2*MINUTE + 500));

            m_eventScheduler.run(1000);

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

        private OcapLocator m_locator1;

        private OcapLocator m_locator2;
    }


    /**
     * Perform a collection of scheduled simultaneous recordings (2 at a time),
     * and confirm their successful completion. This should fail on the
     * simulator (single tuner)
     * 
     * @param locators
     */
    public class TestSimultaneousRecordings extends TestCase
    {
        TestSimultaneousRecordings(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestSimultaneousRecordings";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long soon = now + 10000;

            // delete all recordings
            OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            try
            {
                orm.deleteAllRecordings();
            }
            catch (Exception e)
            {
                System.out.println(getName() +" failure trying to delete all the recordings: "+e);
            }

            // clear the schedule of pending tasks
            reset();

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, soon, 45000, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, soon, 45000, 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, soon + 60000, 45000, 520));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, soon + 60000, 45000, 530));
            m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, soon + 2 * 60000, 45000, 540));
            m_eventScheduler.scheduleCommand(new Record("Recording6", m_locator, soon + 2 * 60000, 45000, 550));
            m_eventScheduler.scheduleCommand(new Record("Recording7", m_locator, soon + 3 * 60000, 45000, 560));
            m_eventScheduler.scheduleCommand(new Record("Recording8", m_locator, soon + 3 * 60000, 45000, 570));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "Recording1", OcapRecordingRequest.COMPLETED_STATE, 4*MINUTE+10000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "Recording2", OcapRecordingRequest.COMPLETED_STATE, 4*MINUTE+10100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "Recording3", OcapRecordingRequest.COMPLETED_STATE, 4*MINUTE+10200));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "Recording4", OcapRecordingRequest.COMPLETED_STATE, 4*MINUTE+10300));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "Recording5", OcapRecordingRequest.COMPLETED_STATE, 4*MINUTE+10400));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "Recording6", OcapRecordingRequest.COMPLETED_STATE, 4*MINUTE+10500));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "Recording7", OcapRecordingRequest.COMPLETED_STATE, 4*MINUTE+10600));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState( "Recording8", OcapRecordingRequest.COMPLETED_STATE, 4*MINUTE+10700));

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingStateCount(
                    countRecordingsByState(OcapRecordingRequest.COMPLETED_STATE) + 8, OcapRecordingRequest.COMPLETED_STATE, 4 * 60000 + 15000));

            m_eventScheduler.run(1000);

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

        private OcapLocator m_locator;
    }

}
