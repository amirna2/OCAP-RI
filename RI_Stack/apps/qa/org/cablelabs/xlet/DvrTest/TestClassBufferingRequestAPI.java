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

import javax.media.Player;
import javax.media.Time;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;

import org.apache.log4j.Logger;
import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.net.OcapLocator;
import org.ocap.shared.media.TimeShiftControl;

import org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell;

public class TestClassBufferingRequestAPI extends DvrTest
{

    TestClassBufferingRequestAPI(Vector locators)
    {
        super(locators);
        m_locators = locators;
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestCaseA((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestCaseB((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestCaseC((OcapLocator) m_locators.elementAt(0), (OcapLocator) m_locators.elementAt(1)));
        tests.addElement(new TestCaseD((OcapLocator) m_locators.elementAt(0), (OcapLocator) m_locators.elementAt(1)));
        tests.addElement(new TestCaseE((OcapLocator) m_locators.elementAt(0), (OcapLocator) m_locators.elementAt(1)));
        return tests;
    }

    /**
     * Test Case A: Canceling a Buffering Request This will be done in the DVR
     * Test Runner 1). Create a Buffering Request a). Start BufferingRequest 2).
     * Validate Buffering Request and wait 1 minute 3). Cancel the Buffering
     * Request 4). Start a buffered SC 5). Call setMediaTime back 50 seconds
     * from now
     */
    class TestCaseA extends TestCase
    {
        TestCaseA(OcapLocator locator)
        {
            m_locator = locator;
            m_eventScheduler = new EventScheduler();
        }

        public void runTest()
        {
            log = Logger.getLogger("TestCaseA:");
            log.info("runTest executing.");

            DVRTestRunnerXlet.log("TestCaseA: Cancelling a Buffering Request: run test");
            m_failed = TEST_PASSED;
            // clear the schedule of pending tasks
            // reset();

            new CancelBufferingRequest(0).ProcessCommand();

            // 1). Start by tuning by NetworkInterface to service A
            new initServiceContext("SC_A", 0).ProcessCommand();

            // m_eventScheduler.scheduleCommand(
            new CreateBufferingRequest(log, "BuffRequest", m_locator, 300, 500, null, 0).ProcessCommand();

            new StartBufferingRequest("BuffRequest", 500).ProcessCommand();

            // 2). Validate Buffering Request and wait 1 minute
            m_eventScheduler.scheduleCommand(new VerifyBufferingRequest(log, "BuffRequest", 1000));

            // 3). Cancel the Buffering Request
            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(log, "BuffRequest", 62000));

            // 4). Start a buffered SC.
            m_eventScheduler.scheduleCommand(new setSCBuffering(log, "SC_A", // name
                    300, // minDur
                    500, // maxDur
                    false, // don't last buffer
                    false, // forget last preference
                    67000));

            final double durLessThanMax = 50.0;
            m_eventScheduler.scheduleCommand(new SelectService(log, "SC_A", m_locator, 75000)
            {
                public void receiveServiceContextEvent(ServiceContextEvent sce)
                {
                    log.info("recieveServiceContextEvent: " + sce);
                    super.receiveServiceContextEvent(sce);
                    if (sce == null)
                    {
                        m_failed = TEST_FAILED;
                        System.out.println("\n::receiveServiceContextEvent - FAILED1\n");
                        return;
                    }

                    ServiceContext sc = sce.getServiceContext();
                    if (sc == null)
                    {
                        m_failed = TEST_FAILED;
                        System.out.println("\n::receiveServiceContextEvent - FAILED2\n");
                        return;
                    }

                    ServiceContentHandler[] schArray;
                    if (!(sce instanceof NormalContentEvent))
                    {
                        System.out.println("\n::receiveServiceContextEvent - FAILED3\n");
                        return;
                    }
                    else
                    {
                        insertObject(sc, "SC_A");

                        log.info("\n::receiveServiceContextEvent - NormalContentEvent received from Service Context\n");

                        schArray = sc.getServiceContentHandlers();

                        log.info("\n\n::receiveServiceContextEvent - ServiceContext returned " + schArray.length
                                + " handlers\n");

                        if (schArray[0] != null && schArray[0] instanceof Player)
                        {
                            Player player = (Player) schArray[0];

                            tsc = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");

                            if (tsc != null)
                            {
                                Time currDur = tsc.getDuration();
                                log.info("\n\n::receiveServiceContextEvent - recording duration = "
                                        + currDur.getSeconds());
                                if (currDur.getSeconds() < durLessThanMax)
                                {
                                    DVRTestRunnerXlet.log(getName()
                                            + " : Failed test for Active Buffering on first check");
                                    m_failed = TEST_FAILED;
                                }
                            }
                            else
                            {
                                log.info("\n\n::receiveServiceContextEvent - error: TimeShiftControl is null! ");
                                m_failed = TEST_FAILED;
                            }
                        }
                        else
                        {
                            log.info("\n\n::receiveServiceContextEvent - error: No Player found for this service context!");
                            m_failed = TEST_FAILED;
                        }
                    }
                }
            });

            m_eventScheduler.scheduleCommand(new NotifyShell(135000)
            {
                public void ProcessCommand()
                {
                    try
                    {
                        ServiceContext sc = (ServiceContext) findObject("SC_A");
                        ServiceContentHandler[] sch = sc.getServiceContentHandlers();

                        if (sch.length == 0 || sch[0] instanceof Player == false)
                        {
                            m_failed = TEST_FAILED;
                            log.info("\n\nCheckTimeShiftControlAgain - No player found in this service context!");
                            return;
                        }

                        Player player = (Player) sch[0];
                        TimeShiftControl tsctrl = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
                        double seconds = tsctrl.getDuration().getSeconds();

                        log.info("<<<<NotifyShell ProcessCommand>>>>>");
                        log.info("\n\nCheckTimeShiftControlAgain - CheckTimeShiftControlAgain recording duration = "
                                + seconds);

                        if (seconds < durLessThanMax)
                        {
                            DVRTestRunnerXlet.log(getName() + " : Failed test for Active Buffering on second check");
                            m_failed = TEST_FAILED;
                        }
                    }
                    catch (NullPointerException npe)
                    {
                        m_failed = TEST_FAILED;
                        log.info("\n\nCheckTimeShiftControlAgain - error: Unable to get TimeShiftControl for ServiceContext SC_A! ");
                        npe.printStackTrace();
                    }
                    catch (Exception e)
                    {
                        m_failed = TEST_INTERNAL_ERROR;
                        log.info("\n\nCheckTimeShiftControlAgain - Unexpected exception!");
                        e.printStackTrace();
                    }
                }
            });

            m_eventScheduler.scheduleCommand(new DestroyService("SC_A", 185000));

            m_eventScheduler.run(10000);

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
            return "TestCaseA - Cancelling a Buffering Request";
        }

        OcapLocator m_locator;

        TimeShiftControl tsc;

        long m_gTZero;

        private Logger log;

    }// end class

    /**
     * Test Case B: Checking File Access Permissions explicitly created by
     * application This will be done in the DVR Test 1). Setup a buffering
     * Request in DVRTestRunner 2). Verify the Application who requested by
     * calling get AppID 3). Cancel the Buffering Request
     * 
     * @author jspruiel
     */
    class TestCaseB extends TestCase
    {
        TestCaseB(OcapLocator locator)
        {
            m_locator = locator;
        }

        public void runTest()
        {
            log = Logger.getLogger("TestCaseA:CBRequest");

            log.info("TestCaseB: Cancelling a Buffering Request: run test");
            DVRTestRunnerXlet.log("TestCaseB: Cancelling a Buffering Request: run test");

            m_failed = TEST_PASSED;
            // clear the schedule of pending tasks
            reset();

            // clear all buffering requests
            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(500));

            // 1). Start by tuning by NetworkInterface to service A
            m_eventScheduler.scheduleCommand(new initServiceContext("SC", 1000));

            m_eventScheduler.scheduleCommand(new CreateBufferingRequest(log, "BR", m_locator, 300, 500, null, 2000));

            m_eventScheduler.scheduleCommand(new VerifyBufferingRequest_AppID(log, "BR", 2500));

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(log, "BR", true, 3500));

            m_eventScheduler.run(2000);

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
            return "TestCaseB - Checking FAP explicitly created by App";
        }

        private OcapLocator m_locator;

        private Logger log;
    }// end class

    /**
     * Test Case C: Checking File Access Permissions implicitly created last
     * channel buffering This will be done in the DVR Test 1). Setup a buffered
     * Service Context 2). Enable last channel buffering 3). Tune to another
     * channels 4). Query resource usages and find the associated Buffering
     * Request 5). Confirm AppId of Buffering Request is null.
     */
    class TestCaseC extends TestCase
    {
        TestCaseC(OcapLocator locator1, OcapLocator locator2)
        {
            m_locator1 = locator1;
            m_locator2 = locator2;
        }

        public void runTest()
        {
            log = Logger.getLogger("TestCaseA:CBRequest");
            log.info("runTest executing.");

            DVRTestRunnerXlet.log("TestCaseC: Cancelling a Buffering Request: run test");
            m_failed = TEST_PASSED;
            // clear the schedule of pending tasks
            reset();

            // clear all buffering requests
            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(500));

            m_eventScheduler.scheduleCommand(new initServiceContext("SC_A", 1000));

            // 1). Setup a buffered Service Context
            m_eventScheduler.scheduleCommand(new setSCBuffering(log, "SC_A", // name
                    300, // minDur
                    500, // maxDur
                    true, // last service buffer
                    false, // forget last preference
                    3000));

            m_eventScheduler.scheduleCommand(new SelectService(log, "SC_A", m_locator1, 2000));

            // 3). Tune to another channel
            m_eventScheduler.scheduleCommand(new SelectService(log, "SC_A", m_locator2, 22000));

            // 4). Query resource usages and find the associated Buffering
            // Request.
            // 5). Confirm AppId of Buffering Request is not null.
            m_eventScheduler.scheduleCommand(new VerifyLastChannelBRHasAppID(m_locator1, 28000));

            m_eventScheduler.scheduleCommand(new DestroyService("SC_A", 35000));

            m_eventScheduler.run(10000);

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
            return "TestCaseC - Checking FAP implicitly created by LastChannelBuffering";
        }

        private OcapLocator m_locator1;

        private OcapLocator m_locator2;

        private Logger log;
    }// end class

    /**
     * @author jspruiel This command succeeds if the appID is not null.
     */
    class VerifyLastChannelBRHasAppID extends EventScheduler.NotifyShell
    {
        /**
         * @param time
         */
        VerifyLastChannelBRHasAppID(OcapLocator loc, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_locatorForLastService = loc;
        }

        VerifyLastChannelBRHasAppID(Logger log, OcapLocator loc, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_locatorForLastService = loc;
            m_log = log;
        }

        public void ProcessCommand()
        {
            if (m_log != null)
            {
                m_log.debug("<<<<VerifyLastChannelBRHasAppID::ProcessCommand>>>>");
            }
            else
            {
                System.out.println("<<<<VerifyLastChannelBRHasAppID::ProcessCommand>>>>");
            }

            OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            BufferingRequest[] bReqs = orm.getBufferingRequests();

            if (bReqs == null)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest:VerifyLastChannelBRHasAppID: Flagged FAILURE:BufferingRequest list is null.");
                m_failedReason = "DvrTest:VerifyLastChannelBRHasAppID: Flagged FAILURE:BufferinfRequest list is null.";
                return;
            }

            // should be null
            if (bReqs[0].getAppID() == null)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest:VerifyLastChannelBRHasAppID: Flagged FAILURE:getAppID is null.");
                m_failedReason = "DvrTest:VerifyLastChannelBRHasAppID: Flagged FAILURE:getAppID() is null.";
                return;
            }

            // serviceBR should represent the last service,
            // not the current.
            Service serviceBR = bReqs[0].getService();
            Service lastService;
            try
            {
                lastService = SIManager.createInstance().getService(m_locatorForLastService);
                if (!serviceBR.equals(lastService))
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("DvrTest:VerifyLastChannelBRHasAppID: Flagged FAILURE:lastService check mismatch.");
                    if (m_log != null)
                    {
                        m_log.debug("DvrTest:VerifyLastChannelBRHasAppID: Flagged FAILURE:lastService check mismatch.");
                    }
                    m_failedReason = "DvrTest:VerifyLastChannelBRHasAppID: Flagged FAILURE:lastService check mismatch.";
                }
                if (m_log != null)
                {
                    m_log.debug("<<<<VerifyLastChannelBRHasAppID::ProcessCommand Done >>>>");
                }
                else
                {
                    System.out.println("<<<<VerifyLastChannelBRHasAppID::ProcessCommand Done>>>>");
                }

            }
            catch (SecurityException e)
            {
                e.printStackTrace();
            }
            catch (InvalidLocatorException e)
            {
                e.printStackTrace();
            }
        }

        Service m_service;

        private OcapLocator m_locatorForLastService;

        Logger m_log;
    }// end class

    /**
     * Test Case D: SetService on active BufferingRequest This will be done in
     * the DVR Test 1).
     */
    class TestCaseD extends TestCase
    {
        TestCaseD(OcapLocator locator1, OcapLocator locator2)
        {
            m_locator1 = locator1;
            m_locator2 = locator2;
        }

        public void runTest()
        {
            log = Logger.getLogger("TestCaseD:");
            log.info("runTest executing.");

            DVRTestRunnerXlet.log("TestCaseD: SetService on Active BReq: run test");
            m_failed = TEST_PASSED;
            // clear the schedule of pending tasks
            reset();

            // clear all buffering requests

            new CancelBufferingRequest(0).ProcessCommand();

            new CreateBufferingRequest(log, "BuffRequest", m_locator1, 300, 500, null, 0).ProcessCommand();

            new StartBufferingRequest(log, "BuffRequest", 0).ProcessCommand();

            m_eventScheduler.scheduleCommand(new SetServiceOnBufferingRequest(log, "BuffRequest", m_locator2, 35000L));

            m_eventScheduler.scheduleCommand(new SetServiceOnBufferingRequest(log, "BuffRequest", m_locator1, 65000L));

            m_eventScheduler.scheduleCommand(new SetServiceOnBufferingRequest(log, "BuffRequest", m_locator2, 85000L));

            m_eventScheduler.scheduleCommand(new CreateBufferingRequest(log, "BuffRequest2", m_locator1, 300, 500,
                    null, 100000L));

            m_eventScheduler.scheduleCommand(new StartBufferingRequest(log, "BuffRequest2", 102000L));

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(log, "BuffRequest", 122000L));

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(log, "BuffRequest2", 124000L));

            m_eventScheduler.run(10000);

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
            return "TestCaseD - SetService on active BufferingRequest.";
        }

        private OcapLocator m_locator1;

        private OcapLocator m_locator2;

        private OcapLocator m_locator3;

        private Logger log;
    }// end class

    /**
     * Test Case D: SetService on active BufferingRequest This will be done in
     * the DVR Test 1).
     */
    class TestCaseE extends TestCase
    {
        TestCaseE(OcapLocator locator1, OcapLocator locator2)
        {
            m_locator1 = locator1;
            m_locator2 = locator2;
        }

        public void runTest()
        {
            log = Logger.getLogger("TestCaseE:");
            log.info("runTest executing.");

            DVRTestRunnerXlet.log("TestCaseE: SetService on inactive BReq: run test");
            m_failed = TEST_PASSED;
            // clear the schedule of pending tasks
            reset();

            // clear all buffering requests
            new CancelBufferingRequest(0).ProcessCommand();

            new CreateBufferingRequest(log, "BuffRequest", m_locator1, 300, 500, null, 0).ProcessCommand();

            m_eventScheduler.scheduleCommand(new SetServiceOnBufferingRequest(log, "BuffRequest", m_locator2, 20000));

            m_eventScheduler.run(10000);

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
            return "TestCaseE - SetService on inactive BufferingRequest ";
        }

        private OcapLocator m_locator1;

        private OcapLocator m_locator2;

        private Logger log;
    }// end class

    private Vector m_locators;
}
