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
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;

import org.apache.log4j.Logger;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.SharedResourceUsage;
import org.ocap.dvr.TimeShiftBufferResourceUsage;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceUsage;
import org.ocap.service.ServiceContextResourceUsage;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.media.TimeShiftControl;

public class TestClassResourceContention extends DvrTest
{

    TestClassResourceContention(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestCaseA());
        tests.addElement(new TestCaseB());
        tests.addElement(new TestCaseC());
        tests.addElement(new TestCaseD());
        // tests.addElement(new TestCaseResContentionD());

        return tests;

    }

    /**
     * Test Case A: buffer request losing contention but regaining � destroy
     * recording
     * 
     * 1). Set up Resource Contention Warning to period of 20 seconds 2).
     * Schedule a recording to start in 2 minutes to go for 3 minutes on service
     * A. 3). Start a buffering service context on service B 4). Schedule a
     * Buffering Request on service C 5). Resource contention loses on Buffering
     * Request 6). Stop the recording when in progress 7). Verify Buffering
     * Request has been fulfilled and verify that recording is in Incomplete
     * state. Verify that only resourceContentionWarning is invoked once.
     * 
     */
    class TestCaseA extends TestCase implements ResourceContentionHandler
    {
        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestCaseA - buffer request losing contention but regaining � destroy recording";
        }

        public void runTest()
        {
            log = Logger.getLogger("TestCaseA:CBRequest");
            log.info("runTest executing.");
            long now = System.currentTimeMillis();

            OcapLocator service_A = (OcapLocator) m_locators.elementAt(0);
            OcapLocator service_B = (OcapLocator) m_locators.elementAt(1);
            OcapLocator service_C = (OcapLocator) m_locators.elementAt(2);

            new CancelBufferingRequest(0).ProcessCommand();
            new initServiceContext("2Service_B", 0).ProcessCommand();
            new initServiceContext("3Service_C", 0).ProcessCommand();

            // 1). Set up Resource Contention Warning to period of 20 seconds
            new RegisterResourceContentionHandler(this, 0, 20000).ProcessCommand();

            // 2). Schedule a recording to start in 1 minutes
            // to go for 4 minutes on service A.
            new Record(log, "1Rec_A", service_A, now + 60000, // start in 2
                                                              // minutes
                    240000, // duration 3 minutes
                    0).ProcessCommand();

            // 3). Start a buffering service context on service B
            new setSCBuffering("2Service_B", // name
                    300, // minDur
                    500, // maxDur
                    false, // don't last buffer
                    false, // forget last preference
                    0).ProcessCommand();

            new SelectService(log, "2Service_B", service_B, 0).ProcessCommand();

            // wait 20s for tuning to complete.
            m_eventScheduler.scheduleCommand(new CreateBufferingRequest(log, "4BuffRequest", service_C, 300, // min
                                                                                                             // dur
                    500, // max dur
                    null, // efap
                    130000)); // trigger time

            // Set test to failed, wait for resource contention handle to get
            // envoked
            m_failed = TEST_FAILED;
            DVRTestRunnerXlet.log("! Setting test to FAILED state unless RCH is envoked !");

            // Start Bufffering
            m_eventScheduler.scheduleCommand(new StartBufferingRequest(log, "4BuffRequest", 131000));

            // Verify that recording is in InProgess state.
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("1Rec_A",
                    LeafRecordingRequest.IN_PROGRESS_STATE, 150000));

            // 6). Stop the recording when in progress after 20 seconds.
            m_eventScheduler.scheduleCommand(new StopRecording(log, "1Rec_A", 160000));

            // cleanup first SC
            m_eventScheduler.scheduleCommand(new DestroyService("2Service_B", true, 165000));

            // Verify the BufferingRequest is buffering, should have
            // buffered more than 19s.
            m_eventScheduler.scheduleCommand(new setSCBuffering(log, "3Service_C", // name
                    300, // minDur
                    500, // maxDur
                    false, // turn off last buffering
                    false, // forget last preference
                    170000)); // trigger

            // selects the service
            // Amount of data already in tsb.
            final long durLessThanMax = 19;
            m_eventScheduler.scheduleCommand(new SelectService(log, "3Service_C", service_C, 172000)
            {
                public void receiveServiceContextEvent(ServiceContextEvent sce)
                {
                    super.receiveServiceContextEvent(sce);
                    if (sce == null)
                    {
                        log.debug("Service Context event is null!");
                        m_failed = TEST_FAILED;
                        return;
                    }

                    ServiceContext sc = sce.getServiceContext();
                    if (sc == null)
                    {
                        log.debug("Service Context associated with SCE is null!");
                        m_failed = TEST_FAILED;
                        return;
                    }

                    ServiceContentHandler[] schArray;
                    if (!(sce instanceof NormalContentEvent))
                    {
                        return;
                    }
                    else
                    {
                        log.debug("\n::receiveServiceContextEvent - NormalContentEvent received from Service Context\n");

                        schArray = sc.getServiceContentHandlers();

                        log.debug("\n\n::receiveServiceContextEvent - ServiceContext returned " + schArray.length
                                + " handlers\n");

                        if (schArray[0] != null && schArray[0] instanceof Player)
                        {
                            Player player = (Player) schArray[0];

                            TimeShiftControl tsc = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
                            if (tsc != null)
                            {
                                Time currDur = tsc.getDuration();
                                if ((currDur.getSeconds() < durLessThanMax) && (currDur.getSeconds() > 10))
                                {
                                    DVRTestRunnerXlet.log(getName()
                                            + " : Failed test for Active Buffering: Current Dur:"
                                            + currDur.getSeconds() + " Minimum Dur:" + durLessThanMax);
                                    m_failed = TEST_FAILED;
                                }
                            }
                        }
                    }
                }
            });

            // Verify that recording is in Incomplete state.
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("1Rec_A",
                    LeafRecordingRequest.INCOMPLETE_STATE, 180000));

            // Verify resourceContentionWarning was invoked once.
            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(0, 200000));

            m_eventScheduler.scheduleCommand(new DestroyService("3Service_C", true, 202000));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("1Rec_A", true, 203000));

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(log, "4BuffRequest", true, 204000));

            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 205000));

            m_eventScheduler.run(5000);

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

        public ResourceUsage[] resolveResourceContention(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            DVRTestRunnerXlet.log("! Setting test to PASSED state - RCH is envoked !");
            m_failed = TEST_PASSED;
            int i, j;

            log.debug("Default DvrTest contention handler called with:");

            log.debug("newRequest: " + getNameForResourceUsage(newRequest) + " (" + newRequest + ")");

            checkResourceUsageValues(newRequest, currentReservations);

            for (i = 0; i < currentReservations.length; i++)
            {
                log.debug(" curRes[" + i + "]: " + getNameForResourceUsage(currentReservations[i]) + " ("
                        + currentReservations[i] + ")");
            }

            ResourceUsage[] neworder = new ResourceUsage[currentReservations.length + 1];

            // Perform insertion sort of each element

            neworder[0] = newRequest;

            for (i = 0; i < currentReservations.length; i++)
            {
                String resKeyToInsert = getNameForResourceUsage(currentReservations[i]);

                for (j = i + 1; j > 0; j--)
                {
                    String sortResKey = getNameForResourceUsage(neworder[j - 1]);

                    if (resKeyToInsert != null && sortResKey != null)
                    {
                        if ((resKeyToInsert.compareTo(sortResKey) >= 0))
                        { // Stop - we hit the top or the entry below us is >=
                          // resToInsert
                            break;
                        }
                    }

                    // Assert: key for neworder[j] <= key for
                    // currentReservations[i]
                    // Move up the current entry - making a hole for the next
                    // interation
                    neworder[j] = neworder[j - 1];
                } // END for (j)

                // j will have stopped at the right place or hit the top
                // Either way, put it where it needs to go
                neworder[j] = currentReservations[i];
            } // END for (i)

            // Assert: neworder is sorted by key value (string name)

            log.debug("Contention handler returning prioritized list:");
            for (i = 0; i < neworder.length; i++)
            {
                log.debug("neworder[" + i + "]: " + getNameForResourceUsage(neworder[i]) + " (" + neworder[i] + ")");
            }

            return neworder;
        } // END resolveResourceContention()

        /**
         * Verify the following: newRequest = TSRU for service c
         * currentReservations = RRUa + sru(SCRUb + TSRUb)
         * 
         * @param newRequest
         * @param currentReservations
         */
        private void checkResourceUsageValues(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            log.info(" newRequest = " + newRequest);
            log.info(" # currReservations = " + currentReservations.length);

            if (!(newRequest instanceof TimeShiftBufferResourceUsage))
            {
                log.debug(" newRequest expected to be TSRU = " + newRequest);
                m_failed = TEST_FAILED;
            }
            else
            {
                log.debug(" newRequest ok = " + newRequest);
            }

            // check the shared usages
            int size = currentReservations.length;
            log.debug("currentReservations = " + size);
            if (size != 2)
            {
                m_failed = TEST_FAILED;
                log.debug("currentReservations size incorrect. Expected 2, got " + size);
                dumpRUs(currentReservations);
                return;
            }

            // Verify the presence of each resource usage type.
            for (int j = 0; j < 2; j++)
            {
                ResourceUsage ru = currentReservations[j];
                if (!(ru instanceof SharedResourceUsage))
                {
                    // The non-SRU must be a RRU
                    if (!(ru instanceof RecordingResourceUsage))
                    {
                        log.info("Should be an RRU, but it is: " + ru);
                        dumpRUs(currentReservations);
                        m_failed = TEST_FAILED;
                        return;
                    }
                }
                else
                {
                    // The shared RU should be SC and TSB RUs
                    log.debug("check Broadcast RUs");
                    boolean foundSCRU = false;
                    boolean foundTSRU = false;
                    ResourceUsage[] rua = ((SharedResourceUsage) ru).getResourceUsages();
                    for (int i = 0; i < 2; i++)
                    {
                        ResourceUsage r = rua[i];
                        if (r instanceof ServiceContextResourceUsage)
                        {
                            log.debug("Found SCRU");
                            foundSCRU = true;
                        }

                        if (r instanceof TimeShiftBufferResourceUsage)
                        {
                            log.debug("Found TSRU");
                            foundTSRU = true;
                        }
                    }

                    if ((foundSCRU == false) || (foundTSRU == false))
                    {
                        m_failed = TEST_FAILED;
                        log.info("SRU for Broadcast operation incorrect!");
                        return;
                    }
                }
            }
        }

        void dumpRUs(ResourceUsage[] currentReservations)
        {
            log.debug("Now dumping the currentReservations list.");
            for (int i = 0; i < currentReservations.length; i++)
            {
                log.debug("currentReservations[" + i + "] = " + currentReservations[i]);
            }
        }

        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            m_counter++;
            DVRTestRunnerXlet.log("!!!!!!!! Warning called on " + getNameForResourceUsage(newRequest) + " !!!!!!!!!");
        }

        String[] rruListForService_a = { "Service A", "two", "RRU", "TSRU" };

        String[] rruListForService_b = { "Service B", "two", "SCRU", "TSRU" };

        OcapLocator loc = (OcapLocator) m_locators.elementAt(0);

        Logger log;

    }

    /**
     * Test Case B: buffer request losing contention but regaining � destroy SC.
     * 
     * 1) Set up Resource Contention Warning to period of 20 seconds 2) Schedule
     * a recording to start in 2 minutes to go for 3 minutes on service A. 3).
     * Start a buffering service context on service B 4). Schedule a Buffering
     * Request on service C 5). Resource contention loses on Buffering Request
     * 6). Destroy the service context 7). Verify Buffering Request has been
     * fulfilled and verify recording is in progress and transfers to completed
     * state. Verify that only resourceContentionWarning is invoked once
     * 
     * @author jspruiel
     * 
     */
    class TestCaseB extends TestCase
    {

        public void runTest()
        {
            log = Logger.getLogger("TestCaseB:RC");
            log.info("runTest executing.");
            long now = System.currentTimeMillis();

            OcapLocator service_A = (OcapLocator) m_locators.elementAt(0);
            OcapLocator service_B = (OcapLocator) m_locators.elementAt(1);
            OcapLocator service_C = (OcapLocator) m_locators.elementAt(2);

            new CancelBufferingRequest(0).ProcessCommand();
            new initServiceContext("2Service_B", 0).ProcessCommand();

            // 1) Set up Resource Contention Warning to period of 20 seconds
            new RegisterResourceContentionHandler(0, 20000).ProcessCommand();

            // 2). Schedule a recording to start in 2 minutes
            // to go for 3 minutes on service A.
            new Record("1Rec_A", service_A, now + 120000, // start in 2 minutes
                    180000, // duration 3 minutes
                    0).ProcessCommand();

            // 3). Start a buffering service context on service B
            new setSCBuffering("2Service_B", // name
                    300, // minDur
                    500, // maxDur
                    false, // don't last buffer
                    false, // forget last preference
                    0).ProcessCommand();

            new SelectService("2Service_B", service_B, 0).ProcessCommand();

            // 4). Schedule a Buffering Request on service C
            m_eventScheduler.scheduleCommand(new CreateBufferingRequest(log, "3BuffRequest", service_C, 300, // min
                                                                                                             // dur
                    500, // max dur
                    null, // efap
                    130000)); // trigger time

            // Set test to failed, wait for resource contention handle to get
            // envoked
            m_failed = TEST_FAILED;
            DVRTestRunnerXlet.log("! Setting test to FAILED state unless RCH is envoked !");

            m_eventScheduler.scheduleCommand(new StartBufferingRequest(log, "3BuffRequest", 131000));

            // 5). Resource contention loses on Buffering Request

            // 6). Destroy the service context
            m_eventScheduler.scheduleCommand(new DestroyService("2Service_B", 155000));

            // Verify that recording is in state IN_PROGRESS_STATE.
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("1Rec_A",
                    LeafRecordingRequest.IN_PROGRESS_STATE, 156000));

            // Verify resourceContentionWarning was invoked once.
            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(0, 157000));

            // Verify the BufferingRequest is buffering, should have
            // buffered more than 19s.
            m_eventScheduler.scheduleCommand(new initServiceContext("3Service_C", 181000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(log, "3Service_C", // name
                    300, // minDur
                    500, // maxDur
                    false, // turn off last buffering
                    false, // forget last preference
                    185000)); // trigger

            // selects the service
            // Amount of data already in tsb.
            final long durLessThanMax = 19;
            m_eventScheduler.scheduleCommand(new SelectService("3Service_C", service_C, 190000)
            {
                public void receiveServiceContextEvent(ServiceContextEvent sce)
                {
                    super.receiveServiceContextEvent(sce);
                    if (sce == null)
                    {
                        log.debug("Service Context event is null!");
                        m_failed = TEST_FAILED;
                        return;
                    }

                    ServiceContext sc = sce.getServiceContext();
                    if (sc == null)
                    {
                        log.debug("Service Context associated with SCE is null!");
                        m_failed = TEST_FAILED;
                        return;
                    }

                    ServiceContentHandler[] schArray;
                    if (!(sce instanceof NormalContentEvent))
                    {
                        return;
                    }
                    else
                    {
                        log.debug("\n::receiveServiceContextEvent - NormalContentEvent received from Service Context\n");

                        schArray = sc.getServiceContentHandlers();

                        log.debug("\n\n::receiveServiceContextEvent - ServiceContext returned " + schArray.length
                                + " handlers\n");

                        if (schArray[0] != null && schArray[0] instanceof Player)
                        {
                            Player player = (Player) schArray[0];

                            TimeShiftControl tsc = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
                            if (tsc != null)
                            {
                                Time currDur = tsc.getDuration();
                                if ((currDur.getSeconds() < durLessThanMax) && (currDur.getSeconds() > 10))
                                {
                                    double dur = currDur.getSeconds();
                                    DVRTestRunnerXlet.log(getName()
                                            + " : Failed test for Active Buffering: Current Dur:" + dur
                                            + " Minimum Dur:" + durLessThanMax);
                                    m_failed = TEST_FAILED;
                                }
                                else
                                {
                                    log.debug("Active Buffering passed");
                                }
                            }
                        }
                    }
                }
            });

            // cleanup
            m_eventScheduler.scheduleCommand(new DestroyService("3Service_C", true, 199000));

            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 200000));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("1Rec_A", true, 201000));

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest("3BuffRequest", true, 202000));

            m_eventScheduler.run(5000);

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
            return "Test Case B: buffer request losing contention but regaining � destroy SC";
        }

        Logger log;
    }

    /**
     * Test Case C: SC losing contention
     * 
     * 1). Set up Resource Contention Warning to period of 20 seconds 2).
     * Schedule a recording to start in 2 minutes to go for 3 minutes on service
     * A. 3). Start a buffering service context on service B 4). Schedule a
     * Buffering Request on service C 5). Resource contention loses on Service
     * Context 6). Verify Buffering Request has been fulfilled and verify
     * recording is in progress and transfers to completed state. Verify that
     * only resourceContentionWarning is invoked once.
     * 
     */
    class TestCaseC extends TestCase
    {
        public void runTest()
        {
            log = Logger.getLogger("TestCaseC:RC");
            log.info("runTest executing.");
            long now = System.currentTimeMillis();

            OcapLocator service_A = (OcapLocator) m_locators.elementAt(0);
            OcapLocator service_B = (OcapLocator) m_locators.elementAt(1);
            OcapLocator service_C = (OcapLocator) m_locators.elementAt(2);

            new CancelBufferingRequest(0).ProcessCommand();
            new initServiceContext("3ServiceB", 0).ProcessCommand();

            // 1) Set up Resource Contention Warning to period of 20 seconds
            new RegisterResourceContentionHandler(0, 20000).ProcessCommand();

            // 2). Schedule a recording to start in 2 minutes
            // to go for 3 minutes on service A.
            new Record("1Rec_A", service_A, now + 120000, // start in 2 minutes
                    180000, // duration 3 minutes
                    0).ProcessCommand();

            // 3). Start a buffering service context on service B
            new setSCBuffering("3ServiceB", // name
                    300, // minDur
                    500, // maxDur
                    false, // don't last buffer
                    false, // forget last preference
                    0).ProcessCommand();

            new SelectService("3ServiceB", service_B, 0).ProcessCommand();

            // 4). Schedule a Buffering Request on service C
            m_eventScheduler.scheduleCommand(new CreateBufferingRequest(log, "2BuffRequest", service_C, 300, // min
                                                                                                             // dur
                    500, // max dur
                    null, // efap
                    130000)); // trigger time

            // Set test to failed, wait for resource contention handle to get
            // envoked
            m_failed = TEST_FAILED;
            DVRTestRunnerXlet.log("! Setting test to FAILED state unless RCH is envoked !");

            // Start buffering
            m_eventScheduler.scheduleCommand(new StartBufferingRequest(log, "2BuffRequest", 131000));

            // 5). Resource contention loses on Service Context
            // 1Rec_A | 2BuffRequest | 3ServiceB

            // Verify that recording is in state IN_PROGRESS_STATE.
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("1Rec_A",
                    LeafRecordingRequest.IN_PROGRESS_STATE, 135000));

            // Verify resourceContentionWarning was invoked once.
            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(0, 137000));

            // Destroy the service context
            m_eventScheduler.scheduleCommand(new DestroyService("3Service_B", true, 151000));

            // Verify the BufferingRequest is buffering, should have
            // buffered more than 19s.
            m_eventScheduler.scheduleCommand(new initServiceContext("3Service_C", 158000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(log, "3Service_C", // name
                    300, // minDur
                    500, // maxDur
                    false, // turn off last buffering
                    false, // forget last preference
                    160000)); // trigger

            // selects the service
            // Amount of data already in tsb.
            final long durLessThanMax = 19;
            m_eventScheduler.scheduleCommand(new SelectService("3Service_C", service_C, 162000)
            {
                public void receiveServiceContextEvent(ServiceContextEvent sce)
                {
                    super.receiveServiceContextEvent(sce);
                    if (sce == null)
                    {
                        log.debug("Service Context event is null!");
                        m_failed = TEST_FAILED;
                        return;
                    }

                    ServiceContext sc = sce.getServiceContext();
                    if (sc == null)
                    {
                        log.debug("Service Context associated with SCE is null!");
                        m_failed = TEST_FAILED;
                        return;
                    }

                    ServiceContentHandler[] schArray;
                    if (!(sce instanceof NormalContentEvent))
                    {
                        if (sce instanceof SelectionFailedEvent)
                        {
                            log.debug("Failed to tune to the service!");
                            m_failed = TEST_FAILED;
                        }
                        return;
                    }
                    else
                    {
                        System.out.println("\n::receiveServiceContextEvent - NormalContentEvent received from Service Context\n");

                        schArray = sc.getServiceContentHandlers();

                        System.out.println("\n\n::receiveServiceContextEvent - ServiceContext returned "
                                + schArray.length + " handlers\n");

                        if (schArray[0] != null && schArray[0] instanceof Player)
                        {
                            Player player = (Player) schArray[0];

                            TimeShiftControl tsc = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
                            if (tsc != null)
                            {
                                Time currDur = tsc.getDuration();
                                if (currDur.getSeconds() < durLessThanMax)
                                {
                                    DVRTestRunnerXlet.log(getName()
                                            + " : Failed test for Active Buffering - current dur: "
                                            + currDur.getSeconds() + " to be :" + durLessThanMax);
                                    m_failed = TEST_FAILED;
                                }
                            }
                        }
                    }
                }
            });

            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(0, 170000));

            // cleanup
            m_eventScheduler.scheduleCommand(new DestroyService("3Service_C", true, 201000));

            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 202000));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("1Rec_A", true, 203000));

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest("2BuffRequest", true, 204000));

            m_eventScheduler.run(5000);

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
            return "TestCaseC - SC losing contention";
        }

        Logger log;
    }

    /**
     * 
     * Test Case D: Recording losing contention but regaining � destroy SC
     * 
     * 1). Set up Resource Contention Warning to period of 20 seconds 2).
     * Schedule a recording to start in 2 minutes to go for 3 minutes on service
     * A. 3). Start a buffering service context on service B 4). Schedule a
     * Buffering Request on service C 5). Resource contention loses on Recording
     * 6). Destroy the service context 7). A new instance of a recording starts
     * up 6). Verify Buffering Request has been fulfilled and that recording is
     * in progress. Verify that only resourceContentionWarning is invoked once
     * 
     * @author jspruiel
     */
    class TestCaseD extends TestCase
    {
        public void runTest()
        {
            log = Logger.getLogger("TestCaseD:RC");
            log.info("runTest executing.");
            long now = System.currentTimeMillis();

            OcapLocator service_A = (OcapLocator) m_locators.elementAt(0);
            OcapLocator service_B = (OcapLocator) m_locators.elementAt(1);

            OcapLocator service_C = (OcapLocator) m_locators.elementAt(2);

            new CancelBufferingRequest(0).ProcessCommand();
            new initServiceContext("2SvcB", 0).ProcessCommand();

            // 1) Set up Resource Contention Warning to period of 20 seconds
            new RegisterResourceContentionHandler(0, 20000).ProcessCommand();

            // 2). Schedule a recording to start in 2 minutes
            // to go for 3 minutes on service A.

            new Record("3Rec", service_A, now + 120000,// start in 2 minutes
                    180000,// duration 3 minutes
                    0, (60 * 60 * 1000), 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS).ProcessCommand();

            // 3). Start a buffering service context on service B
            new setSCBuffering("2SvcB", // name
                    300, // minDur
                    500, // maxDur
                    false, // don't last buffer
                    false, // forget last preference
                    0).ProcessCommand();

            new SelectService("2SvcB", service_B, 0).ProcessCommand();

            // 4). Schedule a Buffering Request on service C
            m_eventScheduler.scheduleCommand(new CreateBufferingRequest(log, "1BReq", service_C, 300, // min
                                                                                                      // dur
                    500, // max dur
                    null, // efap
                    124000)); // trigger time

            // Set test to failed, wait for resource contention handle to get
            // envoked
            m_failed = TEST_FAILED;
            DVRTestRunnerXlet.log("! Setting test to FAILED state unless RCH is envoked !");

            // Start Buffering
            m_eventScheduler.scheduleCommand(new StartBufferingRequest(log, "1BReq", 125000));

            // 5). Resource contention loses on Recording
            // prioritized ordering will be: 1BReq | 2SvcB | 3Rec

            // 6). Destroy the service context
            m_eventScheduler.scheduleCommand(new DestroyService("2SvcB", 140000));

            // Verify the BufferingRequest is buffering, should have
            // buffered more than 19s.

            m_eventScheduler.scheduleCommand(new initServiceContext("3Service_C", 161000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(log, "3Service_C", // name
                    300, // minDur
                    500, // maxDur
                    false, // turn off last buffering
                    false, // forget last preference
                    162000)); // trigger

            // selects the service
            // Amount of data already in tsb.
            final long durLessThanMax = 19;
            m_eventScheduler.scheduleCommand(new SelectService("3Service_C", service_C, 163000)
            {
                public void receiveServiceContextEvent(ServiceContextEvent sce)
                {
                    super.receiveServiceContextEvent(sce);
                    if (sce == null)
                    {
                        log.debug("Service Context event is null!");
                        m_failed = TEST_FAILED;
                        return;
                    }

                    ServiceContext sc = sce.getServiceContext();
                    if (sc == null)
                    {
                        log.debug("Service Context associated with SCE is null!");
                        m_failed = TEST_FAILED;
                        return;
                    }

                    ServiceContentHandler[] schArray;
                    if (!(sce instanceof NormalContentEvent))
                    {
                        return;
                    }
                    else
                    {
                        System.out.println("\n::receiveServiceContextEvent - NormalContentEvent received from Service Context\n");

                        schArray = sc.getServiceContentHandlers();

                        System.out.println("\n\n::receiveServiceContextEvent - ServiceContext returned "
                                + schArray.length + " handlers\n");

                        if (schArray[0] != null && schArray[0] instanceof Player)
                        {
                            Player player = (Player) schArray[0];

                            TimeShiftControl tsc = (TimeShiftControl) player.getControl("org.ocap.shared.media.TimeShiftControl");
                            if (tsc != null)
                            {
                                Time currDur = tsc.getDuration();
                                if (currDur.getSeconds() < durLessThanMax)
                                {
                                    DVRTestRunnerXlet.log(getName() + " : Failed test for Active Buffering");
                                    m_failed = TEST_FAILED;
                                }
                            }
                        }
                    }
                }
            });

            // Verify that recording is in state IN_PROGRESS_STATE.
            // We are checking the new instance of the recording.
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("3Rec",
                    LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 195000));

            // Verify resourceContentionWarning was invoked once.
            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(0, 197000));

            // cleanup
            m_eventScheduler.scheduleCommand(new DestroyService("3Service_C", true, 201000));

            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 202000));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("3Rec", true, 203000));

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest("1BReq", true, 204000));

            m_eventScheduler.run(5000);

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
            return "TestCase D - Recording losing contention but regaining � destroy SC";
        }

        Logger log;
    }

}// End outer class4
