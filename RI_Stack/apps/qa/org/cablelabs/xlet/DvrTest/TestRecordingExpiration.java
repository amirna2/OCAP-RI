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
 * Created on Feb 9, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.dvr.OcapRecordingRequest;

/**
 * @author fsmith
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestRecordingExpiration extends DvrTest
{

    TestRecordingExpiration(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();

    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestCompletedRecordingExpiration((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestInProgressRecordingExpiration((OcapLocator) m_locators.elementAt(0)));

        // With the I02 use of an expiration period (as opposed to absolute
        // time)
        // it is no longer possible for a future recording to expire
        // tests.addElement(new
        // TestFutureRecordingExpiration((OcapLocator)m_locators.elementAt(0)));
        return tests;
    }

    public class TestCompletedRecordingExpiration extends TestCase
    {
        TestCompletedRecordingExpiration(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test Completed Recording  Expiration";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            int reccount = OcapRecordingManager.getInstance().getEntries().size();

            // clear the schedule of pending tasks
            reset();
            // Schedule the 30 second record call for 1 second from now (expires
            // in 60 seconds
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 1000, 30000, 500, 60));

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 40000));

            // Count the recordings again at 22 seconds (recording should have
            // expired and thus be gone)
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.DELETED_STATE, 70000));

            m_eventScheduler.run(5000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("<< " + getName() + " >> : TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("<< " + getName() + " >> : Test   FAILED   ");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("<< " + getName() + " >> : Test   PASSED   ");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestFutureRecordingExpiration extends TestCase
    {
        TestFutureRecordingExpiration(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test Future Recording  Expiration";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            int reccount = OcapRecordingManager.getInstance().getEntries().size();

            // clear the schedule of pending tasks
            reset();
            // Schedule the 30 second record call for 30 seconds from now
            // (expires in 60 seconds
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 30000, 500, 10));

            // Count the recordings
            m_eventScheduler.scheduleCommand(new CountRecordings(reccount + 1, 3500));

            // Count the recordings again at 42 seconds (recording should have
            // expired and thus be gone)
            m_eventScheduler.scheduleCommand(new CountRecordings(reccount, 42000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("<< " + getName() + " >> : Test   FAILED   ");
            }
            else
            {
                DVRTestRunnerXlet.log("<< " + getName() + " >> : Test   PASSED   ");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestInProgressRecordingExpiration extends TestCase
    {
        TestInProgressRecordingExpiration(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test InProgress Recording  Expiration";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            int reccount = OcapRecordingManager.getInstance().getEntries().size();

            // clear the schedule of pending tasks
            reset();
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 1000, 60000, 500, 40));

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 30000));

            // Count the recordings again at 24 seconds (recording should have
            // expired and thus be gone)
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.DELETED_STATE, 50000));

            m_eventScheduler.run(5000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("<< " + getName() + " >> : TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("<< " + getName() + " >> : Test   FAILED   ");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("<< " + getName() + " >> : Test   PASSED   ");
            }
        }

        private OcapLocator m_locator;
    }

}
