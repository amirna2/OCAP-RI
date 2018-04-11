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

import org.apache.log4j.Logger;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceReleasedEvent;
import org.davic.net.tuning.NetworkInterfaceReservedEvent;
import org.davic.resources.ResourceStatusEvent;
import org.davic.resources.ResourceStatusListener;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.TimeShiftBufferResourceUsage;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ApplicationResourceUsage;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceUsage;
import org.ocap.service.ServiceContextResourceUsage;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingRequest;

public class TestClassNetworkInterface extends DvrTest
{
    private Logger log;

    TestClassNetworkInterface(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestCaseA((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestCaseB((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestCaseC((OcapLocator) m_locators.elementAt(0), (OcapLocator) m_locators.elementAt(1)));
        return tests;
    }

    /**
     * @author jspruiel Test Case A: Tuning by NI and SCs Explicit calls to the
     *         Network Interface shall not share the tuner resources with any
     *         recording, ServiceContext, or granted buffer request.
     * 
     *         Pass/Fall Criteria: PASS: The ResourceContentionHandler is entry
     *         coutn equals 1.
     */
    class TestCaseA extends TestCase implements ResourceContentionHandler, ResourceStatusListener
    {
        TestCaseA(OcapLocator locator)
        {
            m_locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("TestCaseA: start: run test");
            m_failed = TEST_PASSED;
            m_callCnt = 0;
            final TestCaseA test = this;

            log = Logger.getLogger("TestCaseA");

            // clear the schedule of pending tasks
            reset();

            NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
            nim.removeResourceStatusEventListener(this);
            nim.addResourceStatusEventListener(this);

            // 1). Start by tuning by NetworkInterface to service A
            m_eventScheduler.scheduleCommand(new initServiceContext("2_SC_svcA", 1000));

            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(this, 100));

            m_eventScheduler.scheduleCommand(new TuneNetworkInterface(log, "1_NIC_svcA", m_locator, 2000));

            // 2). Then tune by Service Context to service A
            m_eventScheduler.scheduleCommand(new SelectService(log, "2_SC_svcA", m_locator, 32000));

            // 4). Start up another SC and tune to service B
            m_eventScheduler.scheduleCommand(new initServiceContext("3_SC_svcB", 70000));
            m_eventScheduler.scheduleCommand(new SelectService(log, "3_SC_svcB", m_locator, 72000));

            // 5). Resource contention handler should get invoked

            // clean up
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(log, null, 100000));
            m_eventScheduler.scheduleCommand(new ReleaseNetworkInterface(log, "1_NIC_svcA", true, 105000));
            m_eventScheduler.scheduleCommand(new StopService(log, "2_SC_svcA", true, 120000));
            m_eventScheduler.scheduleCommand(new StopService(log, "3_SC_svcB", true, 125000));
            m_eventScheduler.scheduleCommand(new DestroyService("2_SC_svcA", 130000));
            m_eventScheduler.scheduleCommand(new DestroyService("3_SC_svcB", 135000));

            m_eventScheduler.scheduleCommand(new EventScheduler.NotifyShell(140000)
            {
                public void ProcessCommand()
                {
                    NetworkInterfaceManager nim = NetworkInterfaceManager.getInstance();
                    nim.removeResourceStatusEventListener(test);
                }
            });

            m_eventScheduler.run(4000);
            if (m_callCnt != 1)
            {
                DVRTestRunnerXlet.log(getName() + "encountered unexpected entry count.");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {

                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestCaseA - Tuning by NI and SCs ------: " + m_locator;
        }

        public void resourceContentionWarning(ResourceUsage arg0, ResourceUsage[] arg1)
        {
        }

        public ResourceUsage[] resolveResourceContention(ResourceUsage arg0, ResourceUsage[] arg1)
        {
            m_callCnt++;

            log.debug("Test:ENTERED RCH");

            int len = arg1.length;
            boolean found = false;
            for (int i = 0; i < len; i++)
            {
                log.debug("currRes[" + i + "] = " + arg1[i]);
                if (arg1[i] instanceof ApplicationResourceUsage)
                {
                    found = true;
                }
            }

            if (!found)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log(m_failedReason = "FAILED: found no ApplicationResourceUsage");
            }

            return new DefaultResourceContentionHandler()
            {
            }.resolveResourceContention(arg0, arg1);
        }

        public void statusChanged(ResourceStatusEvent event)
        {
            if (event instanceof NetworkInterfaceReleasedEvent)
            {
                if (log != null)
                {
                    log.debug("TestCaseA recvd NetworkInterfaceReleasedEvent  = " + event);
                }
                return;
            }

            if (event instanceof NetworkInterfaceReservedEvent)
            {
                if (log != null)
                {
                    log.debug("TestCaseA recvd NetworkInterfaceReservedEvent  = " + event);
                }
                return;
            }

            if (log != null)
            {
                log.debug("TestCase A: ResourceStateEvent = " + event);
            }
        }

        private OcapLocator m_locator;

        int m_callCnt;

    }

    /**
     * @author jspruiel Test Case B: Tuning by NI and SC(unbuffered) with
     *         Recording(lrs) To be run DVRTestRunner 1). Start by tuning by
     *         Network Interface to service A 2). Then tune by Service Context
     *         to service A 3). Issue a record by lrs to service A 4). Recording
     *         should successfully run without conflict.
     */
    class TestCaseB extends TestCase implements ResourceContentionHandler, ResourceStatusListener
    {
        TestCaseB(OcapLocator locator)
        {
            m_locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("TestCaseB: start: run test");
            m_failed = TEST_PASSED;
            m_contentionOccurred = false;

            log = Logger.getLogger("TestCaseB");

            long now = System.currentTimeMillis();

            reset();

            // 1). Start by tuning by Network Interface to service A
            new initServiceContext("Sort2_SC_svcA", 500).ProcessCommand();
            new RegisterResourceContentionHandler(this, 1000).ProcessCommand();

            m_eventScheduler.scheduleCommand(new TuneNetworkInterface(log, "Sort1_NIC_svcA", m_locator, 1000));

            // 2). Then tune by Service Context to service A
            m_eventScheduler.scheduleCommand(new SelectService(log, "Sort2_SC_svcA", m_locator, 30000));

            /**
             * 3).Issue a record by lrs to service A
             */
            m_eventScheduler.scheduleCommand(new Record("Sort3_RR_svcA", m_locator, now + 90000, // start
                                                                                                 // time
                                                                                                 // of
                                                                                                 // recording
                    60000 * 5, // record for 5 minutes but will be deleted
                               // before duration is up.
                    80000)); // time to issue record request

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(log, "Sort3_RR_svcA",
                    LeafRecordingRequest.IN_PROGRESS_STATE, 98000));

            // clean up
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 110000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(log, "Sort3_RR_svcA", true, 115000));
            m_eventScheduler.scheduleCommand(new ReleaseNetworkInterface("Sort1_NIC_svcA", true, 120000));
            m_eventScheduler.scheduleCommand(new StopService("Sort2_SC_svcA", true, 125000));
            m_eventScheduler.scheduleCommand(new DestroyService("Sort2_SC_svcA", 130000));

            m_eventScheduler.run(4000);
            if (m_contentionOccurred == true)
            {
                DVRTestRunnerXlet.log(m_failedReason = getName() + " ResourceContentionHandler was called.");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {

                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestCase B: Tuning by NI & SC(unbuffered) with Rec(lrs) " + m_locator;
        }

        public void resourceContentionWarning(ResourceUsage arg0, ResourceUsage[] arg1)
        {
        }

        public ResourceUsage[] resolveResourceContention(ResourceUsage arg0, ResourceUsage[] arg1)
        {
            m_contentionOccurred = true;

            log.debug("------- ENTERED RCH -------");
            m_failed = TEST_FAILED;
            log.debug("FAILURE ResourceContentionHandler Called. Should be no conflicts.");

            return new DefaultResourceContentionHandler()
            {
            }.resolveResourceContention(arg0, arg1);
        }

        public void statusChanged(ResourceStatusEvent event)
        {
            if (event instanceof NetworkInterfaceReleasedEvent)
            {
                if (log != null)
                {
                    log.debug("TestCaseA recvd NetworkInterfaceReleasedEvent  = " + event);
                }
                return;
            }

            if (event instanceof NetworkInterfaceReservedEvent)
            {
                if (log != null)
                {
                    log.debug("TestCaseA recvd NetworkInterfaceReservedEvent  = " + event);
                }
                return;
            }

            if (log != null)
            {
                log.debug("TestCase A: ResourceStateEvent = " + event);
            }
        }

        private OcapLocator m_locator;

        boolean m_contentionOccurred;
    }

    /**
     * @author jspruiel Test Case C: Tuning by NI and SC(unbuffered) with
     *         Recording(lrs)
     * 
     *         To be run DVRTestRunner 1). Start by tuning by Network Interface
     *         to service A 2). Then tune by Service Context to service A 3).
     *         Issue a record by lrs to service B 4). Resource contention
     *         handler should get invoked
     */
    class TestCaseC extends TestCase implements ResourceContentionHandler, NetworkInterfaceListener
    {
        TestCaseC(OcapLocator locatorSvcA, OcapLocator locatorSvcB)
        {
            m_locatorSvcA = locatorSvcA;
            m_locatorSvcB = locatorSvcB;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("TestCaseC: start: run test");
            log = Logger.getLogger("TestCaseB");

            m_failed = TEST_PASSED;
            m_contentionOccurred = false;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // 1). Start by tuning by Network Interface to service A
            new RegisterResourceContentionHandler(this, 0).ProcessCommand();
            new initServiceContext("SORT2_SC_svcA", 0).ProcessCommand();

            m_eventScheduler.scheduleCommand(new TuneNetworkInterface(log, "SORT1_NIC_svcA", m_locatorSvcA, 500));

            // 2). Then tune by Service Context to service A
            m_eventScheduler.scheduleCommand(new SelectService(log, "SORT2_SC_svcA", m_locatorSvcA, 30000));

            /**
             * 3).Issue a record by lrs to service B
             */
            m_eventScheduler.scheduleCommand(new Record(log, "SORT3_RR_svcB", m_locatorSvcB, now + 900000, // start
                                                                                                           // time
                                                                                                           // of
                                                                                                           // recording
                    60000 * 5, // record for 5 minutes but will be deleted
                               // before duratio is up.
                    80000)); // time to issue record request

            // clean up
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(log, null, 130000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(log, "SORT3_RR_svcB", true, 135000));
            m_eventScheduler.scheduleCommand(new ReleaseNetworkInterface(log, "SORT1_NIC_svcA", true, 140000));
            m_eventScheduler.scheduleCommand(new StopService(log, "SORT2_SC_svcA", true, 145000));
            m_eventScheduler.scheduleCommand(new DestroyService(log, "SORT2_SC_svcA", true, 150000));

            m_eventScheduler.run(4000);
            if (m_contentionOccurred == false)
            {
                DVRTestRunnerXlet.log(getName() + " contention occurred.");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {

                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestCase C: Tuning by NI_a & SC_a(unbuffered) with Rec_b(lrs) ";
        }

        public void resourceContentionWarning(ResourceUsage arg0, ResourceUsage[] arg1)
        {
        }

        public ResourceUsage[] resolveResourceContention(ResourceUsage arg0, ResourceUsage[] arg1)
        {
            m_contentionOccurred = true;

            log.debug("Test:ENTERED RCH");

            log.debug("Test:newRequest RU = " + arg0);

            int len = arg1.length;

            boolean fSCRU = false;
            boolean fARU = false;

            for (int i = 0; i < len; i++)
            {
                log.debug("currRes[" + i + "] = " + arg1[i]);

                if (!fARU && arg1[i] instanceof ApplicationResourceUsage)
                {
                    fARU = true;
                    log.debug("Found ARU");
                    continue;
                }

                if (!fSCRU && arg1[i] instanceof ServiceContextResourceUsage)
                {
                    fSCRU = true;
                    log.debug("Found SCRU ");
                    continue;
                }

                m_failed = TEST_FAILED;
                log.debug(m_failedReason = "FAILED: Unexpected RU = " + arg1[i]);
            }

            // return null;
            return new DefaultResourceContentionHandler()
            {
            }.resolveResourceContention(arg0, arg1);
        }

        public void receiveNIEvent(NetworkInterfaceEvent arg0)
        {

        }

        private OcapLocator m_locatorSvcA;

        private OcapLocator m_locatorSvcB;

        boolean m_contentionOccurred;
    }
}
