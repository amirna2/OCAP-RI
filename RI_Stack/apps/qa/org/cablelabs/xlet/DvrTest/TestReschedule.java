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
 * Created on Mar 23, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import javax.tv.locator.Locator;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingRequest;

import org.cablelabs.xlet.DvrTest.DvrTest.ConfirmRecordingReq_CheckState;
import org.cablelabs.xlet.DvrTest.DvrTest.DeleteRecordingRequest;
import org.cablelabs.xlet.DvrTest.DvrTest.Reschedule;
import org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell;
import org.cablelabs.xlet.DvrTest.TestRescheduleExpiration.RecordWithRetentionPriority;
import org.cablelabs.xlet.DvrTest.TestRescheduleExpiration.TestRescheduleExpiration1;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestReschedule extends DvrTest
{

    /**
     * @author rharris
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public class ValidateSource extends NotifyShell
    {

        private OcapLocator m_locator;

        private String m_rec;

        /**
         * @param time
         */
        ValidateSource(String rec_name, OcapLocator locator, long time)
        {
            super(time);
            m_locator = locator;
            m_rec = rec_name;
            // TODO Auto-generated constructor stub
        }

        public void ProcessCommand()
        {
            RecordingRequest rr = null;
            LocatorRecordingSpec lrs = null;
            OcapLocator rec_locs[] = null;

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());
            try
            {
                // find the associated recording request to name
                rr = (RecordingRequest) findObject(m_rec);

                // get the associated recording spec
                lrs = (LocatorRecordingSpec) (rr.getRecordingSpec());

                // get the locator
                rec_locs = (OcapLocator[]) lrs.getSource();

                // compare the locator by the passed in locator
                // if equal, test passes
                if ((rec_locs[0].getSourceID()) == (m_locator.getSourceID()))
                {
                    System.out.println("<<<<<<<ValidateSource: SourceIDs are equal>>>>>>>>");
                }
                // if not, fail test
                else
                {
                    m_failed = TEST_FAILED;
                    System.out.println("<<<<<<<ValidateSource: SourceIDs are not equal>>>>>>>>");
                    System.out.println("<<<<<<<Recording SourceID : " + rec_locs[0].getSourceID() + " >>>>>>>>");
                    System.out.println("<<<<<<<Recording SourceID : should be " + m_locator.getSourceID() + " >>>>>>>>");
                }
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                System.out.println("<<<<<<<Exception thrown in ValidateSource>>>>>>>>");
                e.printStackTrace();
            }
        }

    }

    /**
     * @author Ryan
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public class TestRescheduleSource extends TestCase
    {
        /**
         * @param locator
         * @param locator2
         */
        public TestRescheduleSource(OcapLocator locator, OcapLocator locator2)
        {
            m_locator = locator;
            m_locator2 = locator2;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRescheduling - locators (source IDs)";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // 30 second recording
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 120000, 30000, 500));

            // print out locators
            System.out.println(m_locator.getSourceID());
            System.out.println(m_locator2.getSourceID());

            // Reschedule Recording 1
            m_eventScheduler.scheduleCommand(new Reschedule("Recording1", now + 60000, 60000, 30000, m_locator2));

            // Verify source after reschedule
            m_eventScheduler.scheduleCommand(new ValidateSource("Recording1", m_locator2, 40000));

            // Verify recording is completed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 130000));

            // Delete the recording
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 133000));

            m_eventScheduler.run(20000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestRescheduling - locators (source IDs) completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestRescheduling - locators (source IDs) FAILED ");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestRescheduling - locators (source IDs) PASSED ");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;

        private OcapLocator m_locator2;
    }

    /**
     * @param locators
     */
    TestReschedule(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestRescheduleSource((OcapLocator) m_locators.elementAt(0),
                (OcapLocator) m_locators.elementAt(1)));
        return tests;
    }

}
