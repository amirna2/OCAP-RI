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

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingRequest;

import org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestFailedRecordingContention extends DvrTest
{

    /**
     * @param locators
     */
    TestFailedRecordingContention(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestSimpleFailedRecordingContention((OcapLocator) m_locators.elementAt(0),
                (OcapLocator) m_locators.elementAt(1), (OcapLocator) m_locators.elementAt(2)));
        return tests;
    }

    /**
     * @author rharris
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public class SetList extends NotifyShell
    {

        /**
         * @param time
         */
        SetList(String rec, String[] rec_list, long time)
        {
            super(time);
            m_rec = rec;
            m_rec_list = rec_list;
            // TODO Auto-generated constructor stub
        }

        public void ProcessCommand()
        {
            RecordingRequest rr;
            ResourceUsage[] ru;
            ResourceUsage[] new_ru;
            String rec_name = null;

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            // find the associated recording request to name
            rr = (RecordingRequest) findObject(m_rec);

            // get priortized list
            ru = rm.getPrioritizedResourceUsages(rr);
            // Create new list
            new_ru = new ResourceUsage[ru.length];

            if (ru.length == m_rec_list.length)
            {
                for (int i = 0; i < ru.length; i++)
                {
                    rec_name = getNameForResourceUsage(ru[i]);
                    if ((rec_name.compareTo(m_rec_list[i])) == 0)
                    {
                        System.out.println("<<<<<<SetList - priority match >>>>>>>>");
                        // Put new entry at the same index
                        new_ru[i] = ru[i];
                    }
                    else
                    {
                        int j;
                        System.out.println("<<<<<<SetList - priority mismatch at index " + i + " expected to be "
                                + m_rec_list[i] + " not " + rec_name + ">>>>>>>>>>>");
                        // Go back to string array to find its priority in the
                        // list
                        for (j = 0; j < m_rec_list.length; j++)
                        {
                            if (rec_name.compareTo(m_rec_list[j]) == 0)
                            {
                                break;
                            }
                        }
                        new_ru[j] = ru[i];
                        System.out.println("<<<<<< " + rec_name + " now at index " + j + " >>>>>>>>>");
                    }
                }
            }
            else
            {
                m_failed = TEST_FAILED;
                System.out.println("<<<<<<< SetList - mismatched array lengths between resources and recordings :"
                        + m_rec_list + " recordings and " + ru.length + " resources >>>>>>>");
            }

            // Setting new prioritization
            rm.setPrioritization(new_ru);
        }

        String m_rec;

        String[] m_rec_list;
    }

    /**
     * @author Ryan
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public class TestSimpleFailedRecordingContention extends TestCase
    {

        private OcapLocator m_locator;

        private OcapLocator m_locator2;

        private OcapLocator m_locator3;

        /**
         * @param locator
         * @param locator3
         * @param locator2
         */
        public TestSimpleFailedRecordingContention(OcapLocator locator, OcapLocator locator2, OcapLocator locator3)
        {
            m_locator = locator;
            m_locator2 = locator2;
            m_locator3 = locator3;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestSimpleScheduledRecordingContention";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // Create a vector list to resolve the names to
            String[] recordings = new String[3];
            recordings[0] = "Recording1";
            recordings[1] = "Recording2";
            recordings[2] = "Recording3";

            // clear the schedule of pending tasks
            reset();
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 60000/*
                                                                                             * 1hr
                                                                                             */, 60000, 1000));
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator2, now + 60000/*
                                                                                              * 1hr
                                                                                              */, 60000, 2000));
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator3, now + 90000/*
                                                                                              * 1hr
                                                                                              */, 30000, 3000));

            // Schedule a recording check by name; should have
            m_eventScheduler.scheduleCommand(new SetList("Recording1", recordings, 30000));

            // Check the state of the recording of lowest priority
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    LeafRecordingRequest.IN_PROGRESS_STATE, 80000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    LeafRecordingRequest.IN_PROGRESS_STATE, 80010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE, 80020));

            // Check the state of the recording of lowest priority
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    LeafRecordingRequest.IN_PROGRESS_STATE, 105000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    LeafRecordingRequest.IN_PROGRESS_STATE, 105010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    LeafRecordingRequest.FAILED_STATE, 105020));

            // Cancel scheduled recordings
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 110000 + 1000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 110000 + 2000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 110000 + 3000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestSimpleScheduledRecordingContention  completed: FAILED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestSimpleScheduledRecordingContention completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        } // END runTest()

    }
}
