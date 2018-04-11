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

package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceUsage;

/**
 * @author knunzio
 * 
 */
public class TestECN1049ResourcePriority extends DvrTest
{

    TestECN1049ResourcePriority(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestECN1049ResourcePriorityScenario1((OcapLocator) m_locators.elementAt(0)));

        return tests;
    }

    /**
     * Recording should be scheduled, show in the nav list, get cancelled, and
     * show a null navigator list
     * 
     * @param locators
     */
    public class TestECN1049ResourcePriorityScenario1 extends TestCase
    {
        TestECN1049ResourcePriorityScenario1(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Basic resource priority verification during RCH.";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 395));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1049ResourcePriorityScenario1 registering the default resourceContention handler.", 396));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 397));

            // Register the resource contention handler
            ECN1049ResourceContentionHandler myRCH = new ECN1049ResourceContentionHandler();
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(myRCH, 400));

            // Schedule 3 overlapping recordings in the future.
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 495));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1049ResourcePriorityScenario1 Before Scheduling Recordings", 496));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 497));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + m_oneHour, m_oneHour, 1, 500));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + m_oneHour, m_oneHour, 2, 510));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + m_oneHour, m_oneHour, 3, 520));

            // check the recordings states
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5995));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "TestECN1049ResourcePriorityScenario1 Checking states for Recording1,Recording2 and Recording3",
                    5996));
            m_eventScheduler.scheduleCommand(new ScheduledLog(
                    "---------------------------------------------------------", 5997));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 6010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 6020));

            // Un Register the resource contention handler by setting it to
            // NULL.
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 25000));

            // Delete Scheduled programs
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 20000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 20010));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 20020));

            m_eventScheduler.run(1000);

            // Check RCH call 3 after the cancel.
            String[] RCHNames = { "Recording1", "Recording2", "Recording3" };
            String newRequest = "Recording3";
            int[] RCHPriorities = { 3, 1, 2 };
            RCHCallInfo info = new RCHCallInfo(newRequest, RCHNames, RCHPriorities, 0);
            boolean result = myRCH.checkCallAt(0, info);

            if (true != result)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1049ResourcePriorityScenario1 - RCH check failed.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1049ResourcePriorityScenario1 completed : TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1049ResourcePriorityScenario1 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1049ResourcePriorityScenario1 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    class ECN1049ResourceContentionHandler extends DefaultResourceContentionHandler
    {
        private Vector infoForCalls = new Vector();

        private int callIndex = 0;

        public ResourceUsage[] resolveResourceContention(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            System.out.println("ECH1049ResourceContentHandler Enter.");

            RCHCallInfo info = new RCHCallInfo(newRequest, currentReservations, callIndex++);
            System.out.println("ECH1049ResourceContentHandler RCHCallInfo: " + info.toString());

            ResourceUsage[] usageArray = super.resolveResourceContention(newRequest, currentReservations);

            info.setPrioritizedResult(usageArray);
            infoForCalls.add(info);

            return usageArray;
        }

        /**
         * 
         * @param index
         *            - index of the RCH call order - 0 based.
         * @param info
         *            - the RCH call info to compare
         * @return true if infos match.
         */
        public boolean checkCallAt(int index, RCHCallInfo info)
        {
            System.out.println("checkCallAt Enter.");

            synchronized (infoForCalls)
            {
                if (index > this.infoForCalls.size())
                {
                    return false;
                }
                System.out.println("---------------------------------------------------------");
                System.out.println("checkCallAt::comparing Infos: ");
                System.out.println("---------------------------------------------------------");
                System.out.println(info.toString());
                System.out.println("---------------------------------------------------------");
                System.out.println(this.infoForCalls.elementAt(index));
                System.out.println("---------------------------------------------------------");
                return ((RCHCallInfo) this.infoForCalls.elementAt(index)).equals(info);
            }
        }

        public boolean checkTestRCHCalls(Vector infos)
        {
            synchronized (infoForCalls)
            {
                if (infos.size() != infoForCalls.size())
                {

                    System.out.println("ECN1321ResourceContentionHandler::checkTestRCHCalls number of calls does not match. callcount: "
                            + this.infoForCalls.size() + " != test count:" + infos.size());
                    return false;
                }

                for (int ii = 0; ii < this.infoForCalls.size(); ii++)
                {
                    if (true != ((RCHCallInfo) this.infoForCalls.elementAt(ii)).equals(infos.elementAt(ii)))
                    {
                        System.out.println("ECN1321ResourceContentionHandler::checkTestRCHCalls call at index: " + ii
                                + " failed.");
                        return false;
                    }
                }
                return true;

            }
        }
    }

    class RCHCallInfo
    {
        private String newRequestName = null;

        private Vector currrentReservationsNames = new Vector();

        private Vector prioritizedUsageList = new Vector();

        private Vector prioritizedPriorityList = new Vector();

        private int callIndex = 0;

        RCHCallInfo(ResourceUsage newRequest, ResourceUsage[] currentReservations, int callIndex)
        {
            this.newRequestName = getNameForResourceUsage(newRequest);
            this.prioritizedPriorityList.add(new Integer(
                    ((OcapRecordingProperties) (((RecordingResourceUsage) newRequest).getRecordingRequest()
                            .getRecordingSpec().getProperties())).getResourcePriority()));
            for (int ii = 0; ii < currentReservations.length; ii++)
            {
                this.currrentReservationsNames.add(getNameForResourceUsage(currentReservations[ii]));
                this.prioritizedPriorityList.add(new Integer(
                        ((OcapRecordingProperties) (((RecordingResourceUsage) currentReservations[ii]).getRecordingRequest()
                                .getRecordingSpec().getProperties())).getResourcePriority()));
            }
            this.callIndex = callIndex;
        }

        RCHCallInfo(String newRequest, String[] finalOrderedRRNameList, int[] finalOrderedRRPriorityList, int callIndex)
        {
            for (int ii = 0; ii < finalOrderedRRNameList.length; ii++)
            {
                this.prioritizedUsageList.add(finalOrderedRRNameList[ii]);
            }
            for (int ii = 0; ii < finalOrderedRRPriorityList.length; ii++)
            {
                this.prioritizedPriorityList.add(new Integer(finalOrderedRRPriorityList[ii]));
            }
            this.callIndex = callIndex;
            this.newRequestName = newRequest;
        }

        RCHCallInfo(String[] finalOrderedRRNameList, int[] finalOrderedRRPriorityList, int callIndex)
        {
            for (int ii = 0; ii < finalOrderedRRNameList.length; ii++)
            {
                this.prioritizedUsageList.add(finalOrderedRRNameList[ii]);
            }

            for (int ii = 0; ii < finalOrderedRRPriorityList.length; ii++)
            {
                this.prioritizedPriorityList.add(new Integer(finalOrderedRRPriorityList[ii]));
            }
            this.callIndex = callIndex;
        }

        public void setPrioritizedResult(ResourceUsage[] prioritizedUsages)
        {
            for (int ii = 0; ii < prioritizedUsages.length; ii++)
            {
                prioritizedUsageList.add(getNameForResourceUsage(prioritizedUsages[ii]));
            }
        }

        public Vector getPrioritizedUsageNames()
        {
            return (Vector) this.prioritizedUsageList.clone();
        }

        public Vector getPrioritizedUsagePriorities()
        {
            return (Vector) this.prioritizedPriorityList.clone();
        }

        public boolean equals(RCHCallInfo info)
        {
            Vector names = info.getPrioritizedUsageNames();
            if (names.size() != this.prioritizedUsageList.size()) return false;

            Vector priorities = info.getPrioritizedUsagePriorities();
            if (priorities.size() != this.prioritizedUsageList.size()) return false;

            for (int ii = 0; ii < names.size(); ii++)
            {
                String string1 = (String) this.prioritizedUsageList.elementAt(ii);
                String string2 = (String) names.elementAt(ii);
                if (0 != (string1.compareTo(string2)))
                {
                    return false;
                }
            }

            for (int ii = 0; ii < priorities.size(); ii++)
            {
                int priority1 = ((Integer) this.prioritizedPriorityList.elementAt(ii)).intValue();
                int priority2 = ((Integer) priorities.elementAt(ii)).intValue();
                if (priority1 != priority2)
                {
                    return false;
                }
            }
            return true;
        }

        public String toString()
        {
            StringBuffer buffer = new StringBuffer();
            buffer.append("RCHCallInfo - callIndex: " + this.callIndex + " Recordings in Call:\n");
            for (int ii = 0; ii < this.prioritizedUsageList.size(); ii++)
            {
                buffer.append("   " + (String) this.prioritizedUsageList.elementAt(ii) + "\n");
            }

            for (int ii = 0; ii < this.prioritizedPriorityList.size(); ii++)
            {
                buffer.append("   Priority index-" + ii + ": "
                        + ((Integer) this.prioritizedPriorityList.elementAt(ii)).toString() + "\n");
            }
            buffer.append("   New Request Name: " + this.newRequestName + "\n");
            return buffer.toString();
        }
    }
}
