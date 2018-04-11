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
 * Created on Mar 6, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.RecordingRequest;

import org.cablelabs.xlet.DvrTest.DvrTest.ConfirmRecordingReq_CheckState;
import org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestGetPrioritizationList extends DvrTest
{

    /**
     * @author rharris
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public class CheckPrioritizedList extends NotifyShell
    {

        /**
         * @param time
         */
        CheckPrioritizedList(String rec, String[] rec_list, long time)
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
            String rec_name = null;

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            // find the associated recording request to name
            rr = (RecordingRequest) findObject(m_rec);

            // get priortized list
            ru = rm.getPrioritizedResourceUsages(rr);

            if (ru.length == m_rec_list.length)
            {
                for (int i = 0; i < ru.length; i++)
                {
                    rec_name = getNameForResourceUsage(ru[i]);
                    if ((rec_name.compareTo(m_rec_list[i])) == 0)
                    {
                        System.out.println("<<<<<<CheckPrioritizedList - priority match >>>>>>>>");
                    }
                    else
                    {
                        System.out.println("<<<<<<CheckPrioritizedList - priority mismatch at index " + i
                                + " expected be " + m_rec_list[i] + " not " + rec_name + ">>>>>>>>>>>");
                    }
                }
            }
            else
            {
                m_failed = TEST_FAILED;
                System.out.println("<<<<<<< CheckPrioritizedList - mismatched array lengths between resources and recordings :"
                        + m_rec_list + " recordings and " + ru.length + " resources >>>>>>>");
            }
        }

        String m_rec;

        String[] m_rec_list;
    }

    /**
     * @param locators
     */
    TestGetPrioritizationList(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestGetList((OcapLocator) m_locators.elementAt(2)));
        return tests;
    }

    public class TestGetList extends TestCase
    {
        TestGetList(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestGetPrioritizationList";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Create a vector list to resolve the names to
            String[] recordings = new String[3];
            recordings[0] = "Recording1";
            recordings[1] = "Recording2";
            recordings[2] = "Recording3";

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule 3 recordings at the same time
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 3600000 /*
                                                                                                * 1hr
                                                                                                */, 180000, 1000));
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 3600000 /*
                                                                                                * 1hr
                                                                                                */, 180000, 2000));
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 3600000 /*
                                                                                                * 1hr
                                                                                                */, 180000, 3000));
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + 3780000 /*
                                                                                                * 1hr
                                                                                                */, 180000, 4000));
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, now + 3420000 /*
                                                                                                * 1hr
                                                                                                */, 180000, 5000));

            // Check states based on 2 tuner box
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 30000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 30010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 30020));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 30030));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 30040));

            // Schedule a recording check by name; should have
            m_eventScheduler.scheduleCommand(new CheckPrioritizedList("Recording1", recordings, 40000));

            // Check states based on 2 tuner box
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 50000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 50010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 50020));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 50030));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 50040));

            // Delete the record call
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 61000));
            // Delete the record call
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 62000));
            // Delete the record call
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 63000));
            // Delete the record call
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording4", 64000));
            // Delete the record call
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording5", 65000));

            // Schedule the deletion of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 65000));

            m_eventScheduler.run(20000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestGetPrioritizationList  completed: FAILED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestGetPrioritizationList  completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        OcapLocator m_locator;
    }
}
