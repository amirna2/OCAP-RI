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
 * Resource Contention Gating Tests
 */

package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.dvr.SharedResourceUsage;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.dvr.OcapRecordingRequest;

/**
 * @author craigp
 * 
 *         Set of gating test cases for recording conflict resolution.
 */

public class TestTunerConflict_Gating extends DvrTest
{
    TestTunerConflict_Gating(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        OcapLocator[] locators = new OcapLocator[m_locators.size()];

        for (int i = 0; i < locators.length; i++)
        {
            locators[i] = (OcapLocator) (m_locators.elementAt(i));
        }

        Vector tests = new Vector();
        tests.addElement(new TestRecordingAndServiceConflict_ABC(locators));
        tests.addElement(new TestRecordingAndServiceConflict_ACB(locators));
        tests.addElement(new TestRecordingAndServiceConflict_CBA(locators));
        tests.addElement(new TestRecordingNIAndSCConflict_ABC(locators));
        tests.addElement(new TestRecordingNIAndSCConflict_ACB(locators));
        tests.addElement(new TestRecordingNIAndSCConflict_CBA(locators));
        tests.addElement(new TestRecordingAndServiceConflict_ABCD(locators));
        return tests;
    }

    /**
     * One ServiceContext is presenting and two recordings are scheduled. When
     * the second recording is scheduled, the ResourceContentionHandler should
     * prioritize the recordings over the SC.
     * 
     * @author craigp
     */
    public class TestRecordingAndServiceConflict_ABC extends TestCase implements RecordingAlertListener, RecordingChangedListener
    {
        Vector m_eventList;

        private OcapLocator[] m_locators;

        TestRecordingAndServiceConflict_ABC(OcapLocator[] locators)
        {
            m_locators = locators;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "RecordingAndServiceConflict_ABC";

        }

        public void runTest()
        {
            m_eventList = new Vector();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            rm.addRecordingChangedListener(this);
            rm.addRecordingAlertListener(this);

            String recording_A = "#1-Recording_A";
            String recording_B = "#2-Recording_B";
            String serviceContext_C = "#3-ServiceContext_C";

            //
            // clear the schedule of pending tasks
            //
            reset();

            //
            // Init the service context
            //
            initSC();

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            //
            // Schedule the registration of the default DvrTest contention
            // handler
            // The default resource contention handler will prioritize
            // lexographically by name
            // So, "#1" will be prioritized, first, "#2" second, and "#3" third
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800, 30000));

            //
            // Schedule 2 recordings - one starting in 1 minute, the other
            // starting in 2 minutes
            //
            m_eventScheduler.scheduleCommand(new Record(recording_A, m_locators[0], now + MINUTE, 5*MINUTE, 4000));
            m_eventScheduler.scheduleCommand(new Record(recording_B, m_locators[1], now + 2*MINUTE, 5*MINUTE, 6000));

            //
            // Schedule service selection for C to start before both recordings
            // (20 seconds into test)
            //
            m_eventScheduler.scheduleCommand(new SelectService(serviceContext_C, m_locators[2], 20000));

            //
            // Check Recordings' status before they start
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10010));

            //
            // Check Recordings' status after Recording A should have started
            // (25 seconds after A starts)
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.IN_PROGRESS_STATE, 60000 + 35000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 60000 + 35010));

            //
            // Check Recordings' status after Recording B should have started
            // (25 seconds after B starts)
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.IN_PROGRESS_STATE, 120000 + 35000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.IN_PROGRESS_STATE, 120000 + 35010));

            //
            // Delete recordings and service after 3 minutes
            // Note: Some of these WILL fail - which fail depend upon the test
            //
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recording_A, 180000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recording_B, 210000));

            //
            // Verify that the resource contention warning has been only been
            // envoked once
            //
            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(1, 220000));

            //
            // Schedule the deletion of the contention handler
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 250000));

            //
            // Start the test
            //
            m_eventScheduler.run(10000);

            //
            // After test completion, check for test success/failure
            //
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: INTERNAL ERROR-DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
            }


            rm.removeRecordingChangedListener(this);
            rm.removeRecordingAlertListener(this);

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();

        } // END runTest(GatingTest-RecordingAndServiceConflict_ABC)

        /**
         * Handler for recording notifications
         */
        public void recordingAlert(RecordingAlertEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);
                DvrEventPrinter.printRecordingAlertEvent(e);
            }
        } // END recordingAlert()

        /**
         * Handler for state, and list change events.
         */
        public void recordingChanged(RecordingChangedEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);

                DvrEventPrinter.printRecordingChangedEvent(e);
            }
        } // END recordingChanged()
    } // END class class TestRecordingAndServiceConflict_ABC

    /**
     * One ServiceContext is presenting and two recordings are scheduled. When
     * the second recording is scheduled, the ResourceContentionHandler should
     * prioritize ServiceContext over the second recording.
     * 
     * @author craigp
     */
    public class TestRecordingAndServiceConflict_ACB extends TestCase implements RecordingAlertListener, RecordingChangedListener
    {
        Vector m_eventList;

        private OcapLocator[] m_locators;

        TestRecordingAndServiceConflict_ACB(OcapLocator[] locators)
        {
            m_locators = locators;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "RecordingAndServiceConflict_ACB";
        }

        public void runTest()
        {
            m_eventList = new Vector();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            rm.addRecordingChangedListener(this);
            rm.addRecordingAlertListener(this);

            String recording_A = "#1-Recording_A";
            String serviceContext_C = "#2-ServiceContext_C";
            String recording_B = "#3-Recording_B";

            //
            // clear the schedule of pending tasks
            //
            reset();

            //
            // Init the service context
            //
            initSC();

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            //
            // Schedule the registration of the default DvrTest contention
            // handler
            // The default resource contention handler will prioritize
            // lexographically by name
            // So, "#1" will be prioritized, first, "#2" second, and "#3" third
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800, 30000));

            //
            // Schedule 2 recordings - one starting in 1 minute, the other
            // starting in 2 minutes
            //
            m_eventScheduler.scheduleCommand(new Record(recording_A, m_locators[0], now + MINUTE, 5*MINUTE, 4000));
            m_eventScheduler.scheduleCommand(new Record(recording_B, m_locators[1], now + 2*MINUTE, 5*MINUTE, 6000));

            //
            // Schedule service selection for C to start before both recordings
            // (20 seconds into test)
            //
            m_eventScheduler.scheduleCommand(new SelectService(serviceContext_C, m_locators[2], 20000));

            //
            // Check Recordings' status before they start
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10010));

            //
            // Check Recordings' status after Recording A should have started
            // (25 seconds after A starts)
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.IN_PROGRESS_STATE, 60000 + 35000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 60000 + 35010));

            //
            // Check Recordings' status after Recording B should have started
            // (25 seconds after B starts)
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.IN_PROGRESS_STATE, 120000 + 35000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.FAILED_STATE, 120000 + 35010));

            //
            // Delete recordings and service after 3 minutes
            // Note: Some of these WILL fail - which fail depend upon the test
            //
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recording_A, 180000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recording_B, 210000));

            //
            // Verify that the resource contention warning has been only been
            // envoked once
            //
            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(1, 220000));

            //
            // Schedule the deletion of the contention handler
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 250000));

            //
            // Start the test
            //
            m_eventScheduler.run(10000);

            //
            // After test completion, check for test success/failure
            //
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: INTERNAL ERROR-DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }


            rm.removeRecordingChangedListener(this);
            rm.removeRecordingAlertListener(this);

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();

        } // END runTest(GatingTest-RecordingAndServiceConflict_ACB)

        /**
         * Handler for recording notifications
         */
        public void recordingAlert(RecordingAlertEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);
                DvrEventPrinter.printRecordingAlertEvent(e);
            }
        } // END recordingAlert()

        /**
         * Handler for state, and list change events.
         */
        public void recordingChanged(RecordingChangedEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);

                DvrEventPrinter.printRecordingChangedEvent(e);
            }
        } // END recordingChanged()
    } // END class class TestRecordingAndServiceConflict_ACB

    /**
     * One ServiceContext is presenting and two recordings are scheduled. When
     * the second recording is scheduled, the ResourceContentionHandler should
     * prioritize the ServiceContext and first recording over the second
     * recording.
     * 
     * @author craigp
     */
    public class TestRecordingAndServiceConflict_CBA extends TestCase implements RecordingAlertListener, RecordingChangedListener
    {
        Vector m_eventList;

        private OcapLocator[] m_locators;

        TestRecordingAndServiceConflict_CBA(OcapLocator[] locators)
        {
            m_locators = locators;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "RecordingAndServiceConflict_CBA";

        }

        public void runTest()
        {
            m_eventList = new Vector();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            rm.addRecordingChangedListener(this);
            rm.addRecordingAlertListener(this);

            String serviceContext_C = "#1-ServiceContext_C";
            String recording_B = "#2-Recording_B";
            String recording_A = "#3-Recording_A";

            //
            // clear the schedule of pending tasks
            //
            reset();

            //
            // Init the service context
            //
            initSC();

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            //
            // Schedule the registration of the default DvrTest contention
            // handler
            // The default resource contention handler will prioritize
            // lexographically by name
            // So, "#1" will be prioritized, first, "#2" second, and "#3" third
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800, 30000));

            //
            // Schedule 2 recordings - one starting in 1 minute, the other
            // starting in 2 minutes
            //
            m_eventScheduler.scheduleCommand(new Record(recording_A, m_locators[0], now + MINUTE, 5*MINUTE, 4000));
            m_eventScheduler.scheduleCommand(new Record(recording_B, m_locators[1], now + 2*MINUTE, 5*MINUTE, 6000));

            //
            // Schedule service selection for C to start before both recordings
            // (20 seconds into test)
            //
            m_eventScheduler.scheduleCommand(new SelectService(serviceContext_C, m_locators[2], 20000));

            //
            // Check Recordings' status before they start
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10010));

            //
            // Check Recordings' status after Recording A should have started
            // (25 seconds after A starts)
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.IN_PROGRESS_STATE, 60000 + 35000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 60000 + 35010));

            //
            // Check Recordings' status after Recording B should have started
            // (25 seconds after B starts)
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 120000 + 35000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.IN_PROGRESS_STATE, 120000 + 35010));

            //
            // Delete recordings and service after 3 minutes
            // Note: Some of these WILL fail - which fail depend upon the test
            //
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recording_A, 180000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recording_B, 210000));

            //
            // Verify that the resource contention warning has been only been
            // envoked once
            //
            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(1, 220000));

            //
            // Schedule the deletion of the contention handler
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 250000));

            //
            // Start the test
            //
            m_eventScheduler.run(10000);

            //
            // After test completion, check for test success/failure
            //
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: INTERNAL ERROR-DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }


            rm.removeRecordingChangedListener(this);
            rm.removeRecordingAlertListener(this);

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();

        } // END runTest(GatingTest-RecordingAndServiceConflict_CBA)

        /**
         * Handler for recording notifications
         */
        public void recordingAlert(RecordingAlertEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);
                DvrEventPrinter.printRecordingAlertEvent(e);
            }
        } // END recordingAlert()

        /**
         * Handler for state, and list change events.
         */
        public void recordingChanged(RecordingChangedEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);

                DvrEventPrinter.printRecordingChangedEvent(e);
            }
        } // END recordingChanged()
    } // END class class TestRecordingAndServiceConflict_CBA

    /**
     * One NetworkInterface and one ServiceCOntext are presenting and one
     * recording is scheduled.  When the recording is scheduled, the 
     * the ResourceContentionHandler should prioritize the recording over the 
     * over the SeriveContext which causes the Network Intercace to become
     * unreserved
     * 
     * @author craigp
     */
    public class TestRecordingNIAndSCConflict_ABC extends TestCase implements RecordingAlertListener, RecordingChangedListener
    {
        Vector m_eventList;

        private OcapLocator[] m_locators;

        TestRecordingNIAndSCConflict_ABC(OcapLocator[] locators)
        {
            m_locators = locators;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "RecordingNIAndSCConflict_ABC";
        }

        public void runTest()
        {
            m_eventList = new Vector();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            rm.addRecordingChangedListener(this);
            rm.addRecordingAlertListener(this);

            String recording_A = "#1-Recording_A";
            String serviceContext_B = "#2-ServiceContext_B";
            String NetworkInterface_C = "#3-NetworkInterface_C";

            //
            // clear the schedule of pending tasks
            //
            reset();

            //
            // Init the Network Interface
            //
            initSC();

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            //
            // Schedule the registration of the default DvrTest contention
            // handler
            // The default resource contention handler will prioritize
            // lexographically by name
            // So, "#1" will be prioritized, first, "#2" second, and "#3" third
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800, 30000));

            //
            // Schedule a 5 minute recording to start in 1 minute
            //
            m_eventScheduler.scheduleCommand(new Record(recording_A, m_locators[0], now + MINUTE, 5*MINUTE, 3000));

            //
            // Schedule service selection for B to start before both recordings
            // (20 seconds into test)
            //
            m_eventScheduler.scheduleCommand(new SelectService(serviceContext_B, m_locators[1], 10000));

            //
            // Schedule NI selection for C to start before recording start
            // (30 seconds into test)
            //
            m_eventScheduler.scheduleCommand(new TuneNetworkInterface(NetworkInterface_C, m_locators[2], 40000));

            //
            // Check Recording's status before they start
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10000));

            //
            // Check status after Recording A should have started (25 seconds
            // after A starts)
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.IN_PROGRESS_STATE, MINUTE + 35000));
            m_eventScheduler.scheduleCommand(new ConfirmNetworkInterfaceReserved(NetworkInterface_C, false, MINUTE + 35010));

            //
            // Delete recording, SC, and NI after 3 minutes
            // Note: Some of these WILL fail - which fail depend upon the test
            //
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recording_A, 3*MINUTE));

            //
            // Verify that the resource contention warning has not been envoked
            //
            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(0, 3*MINUTE + 40000));

            //
            // Release the reserved network interface
            //   ignoreFailure is set to true
            m_eventScheduler.scheduleCommand(new ReleaseNetworkInterface(NetworkInterface_C, true, 5*MINUTE));

            //
            // Schedule the deletion of the contention handler
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 5*MINUTE + 10000));

            //
            // Start the test
            //
            m_eventScheduler.run(10000);

            //
            // After test completion, check for test success/failure
            //
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: INTERNAL ERROR-DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }


            rm.removeRecordingChangedListener(this);
            rm.removeRecordingAlertListener(this);

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();

        } // END runTest(GatingTest-RecordingNIAndSCConflict_ABC)

        /**
         * Handler for recording notifications
         */
        public void recordingAlert(RecordingAlertEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);
                DvrEventPrinter.printRecordingAlertEvent(e);
            }
        } // END recordingAlert()

        /**
         * Handler for state, and list change events.
         */
        public void recordingChanged(RecordingChangedEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);

                DvrEventPrinter.printRecordingChangedEvent(e);
            }
        } // END recordingChanged()
    } // END class class TestRecordingNIAndSCConflict_ABC

    /**
     * One NetworkInterface is presenting and two recordings are scheduled. When
     * the second recording is scheduled, the ResourceContentionHandler should
     * prioritize NetworkInterface over the second recording.
     * 
     * @author craigp
     */
    public class TestRecordingNIAndSCConflict_ACB extends TestCase implements RecordingAlertListener, RecordingChangedListener
    {
        Vector m_eventList;

        private OcapLocator[] m_locators;

        TestRecordingNIAndSCConflict_ACB(OcapLocator[] locators)
        {
            m_locators = locators;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "RecordingNIAndSCConflict_ACB";

        }

        public void runTest()
        {
            m_eventList = new Vector();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            rm.addRecordingChangedListener(this);
            rm.addRecordingAlertListener(this);

            String recording_A = "#1-Recording_A";
            String NetworkInterface_C = "#2-NetworkInterface_C";
            String serviceContext_B = "#3-ServiceContext_B";

            //
            // clear the schedule of pending tasks
            //
            reset();

            //
            // Init the Network Interface
            //
            initSC();

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            //
            // Schedule the registration of the default DvrTest contention
            // handler
            // The default resource contention handler will prioritize
            // lexographically by name
            // So, "#1" will be prioritized, first, "#2" second, and "#3" third
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800, 30000));

            //
            // Schedule a 5 minute long recording to start in 1 minute
            //
            m_eventScheduler.scheduleCommand(new Record(recording_A, m_locators[0], now + MINUTE, 5*MINUTE, 3000));

            //
            // Schedule service selection for B to start before recordings
            //
            m_eventScheduler.scheduleCommand(new SelectService(serviceContext_B, m_locators[1], 10000));

            //
            // Schedule NI selection for C to start before recording 
            // (30 seconds into test)
            //
            m_eventScheduler.scheduleCommand(new TuneNetworkInterface(NetworkInterface_C, m_locators[2], 40000));

            //
            // Check Recording's status before start
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10000));

            //
            // Check status after Recording A should have started (25 seconds
            // after A starts)
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.IN_PROGRESS_STATE, MINUTE + 35000));
            // confirm NI has been reserved (true) 
            m_eventScheduler.scheduleCommand(new ConfirmNetworkInterfaceReserved(NetworkInterface_C, true, MINUTE + 35010));

            //
            // Delete recording, SC, and NI after 3 minutes
            // Note: Some of these WILL fail - which fail depend upon the test
            //
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recording_A, 3*MINUTE));

            //
            // Verify that the resource contention warning has not been evoked
            //
            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(0, 3*MINUTE + 40000));

            //
            // Release the reserved network interface
            //    do ignore failure (true)
            m_eventScheduler.scheduleCommand(new ReleaseNetworkInterface(NetworkInterface_C, true, 5*MINUTE));

            //
            // Schedule the deletion of the contention handler
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 5*MINUTE + 10000));

            //
            // Start the test
            //
            m_eventScheduler.run(10000);

            //
            // After test completion, check for test success/failure
            //
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: INTERNAL ERROR-DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }


            rm.removeRecordingChangedListener(this);
            rm.removeRecordingAlertListener(this);

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();

        } // END runTest(GatingTest-RecordingNIAndSCConflict_ACB)

        /**
         * Handler for recording notifications
         */
        public void recordingAlert(RecordingAlertEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);
                DvrEventPrinter.printRecordingAlertEvent(e);
            }
        } // END recordingAlert()

        /**
         * Handler for state, and list change events.
         */
        public void recordingChanged(RecordingChangedEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);

                DvrEventPrinter.printRecordingChangedEvent(e);
            }
        } // END recordingChanged()
    } // END class class TestRecordingNIAndSCConflict_ACB

    /**
     * One NetworkInterface is presenting and two recordings are scheduled. When
     * the second recording is scheduled, the ResourceContentionHandler should
     * prioritize the NetworkInterface and first recording over the second
     * recording.
     * 
     * @author craigp
     */
    public class TestRecordingNIAndSCConflict_CBA extends TestCase implements RecordingAlertListener, RecordingChangedListener
    {
        Vector m_eventList;

        private OcapLocator[] m_locators;

        TestRecordingNIAndSCConflict_CBA(OcapLocator[] locators)
        {
            m_locators = locators;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "RecordingNIAndSCConflict_CBA";

        }

        public void runTest()
        {
            m_eventList = new Vector();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            rm.addRecordingChangedListener(this);
            rm.addRecordingAlertListener(this);

            String NetworkInterface_C = "#1-NetworkInterface_C";
            String serviceContext_B = "#2-ServiceContext_B";
            String recording_A = "#3-Recording_A";

            //
            // clear the schedule of pending tasks
            //
            reset();

            //
            // Init the Network Interface
            //
            initSC();

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            //
            // Schedule the registration of the default DvrTest contention
            // handler
            // The default resource contention handler will prioritize
            // lexographically by name
            // So, "#1" will be prioritized, first, "#2" second, and "#3" third
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800, 30000));

            //
            // Schedule a 5 minutes long recording to start in 1 minute
            //
            m_eventScheduler.scheduleCommand(new Record(recording_A, m_locators[0], now + MINUTE, 5*MINUTE, 3000));

            //
            // Schedule service selection for B to start before recordings
            // (20 seconds into test)
            //
            m_eventScheduler.scheduleCommand(new SelectService(serviceContext_B, m_locators[1], 20000));

            //
            // Schedule NI selection for C to start before recordings
            // (30 seconds into test)
            //
            m_eventScheduler.scheduleCommand(new TuneNetworkInterface(NetworkInterface_C, m_locators[2], 30000));

            //
            // Check Recording's status before they start
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10000));

            //
            // Check status after Recording A should have started (25 seconds
            // after A starts)
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.FAILED_STATE, MINUTE + 35000));
            m_eventScheduler.scheduleCommand(new ConfirmNetworkInterfaceReserved(NetworkInterface_C, true, MINUTE + 35010));

            //
            // Delete recording, SC, and NI after 3 minutes
            // Note: Some of these WILL fail - which fail depend upon the test
            //
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recording_A, 3*MINUTE));

            //
            // Verify that the resource contention warning has been evoked once
            //
            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(1, 3*MINUTE + 40000));

            //
            // Release the reserved network interface
            //    do ignore failure when releasing NI (true)
            m_eventScheduler.scheduleCommand(new ReleaseNetworkInterface(NetworkInterface_C, true, 5*MINUTE));

            //
            // Schedule the deletion of the contention handler
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 5*MINUTE + 10000));

            //
            // Start the test
            //
            m_eventScheduler.run(10000);

            //
            // After test completion, check for test success/failure
            //
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: INTERNAL ERROR-DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }

            rm.removeRecordingChangedListener(this);
            rm.removeRecordingAlertListener(this);

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();

        } // END runTest(GatingTest-RecordingNIAndSCConflict_CBA)

        /**
         * Handler for recording notifications
         */
        public void recordingAlert(RecordingAlertEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);
                DvrEventPrinter.printRecordingAlertEvent(e);
            }
        } // END recordingAlert()

        /**
         * Handler for state, and list change events.
         */
        public void recordingChanged(RecordingChangedEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);

                DvrEventPrinter.printRecordingChangedEvent(e);
            }
        } // END recordingChanged()
    } // END class class TestRecordingNIAndSCConflict_CBA

    /**
     * One ServiceContext is presenting and two recordings are scheduled. When
     * the second recording is scheduled, the ResourceContentionHandler should
     * prioritize the recordings over the SC.
     * 
     * @author craigp
     */
    public class TestRecordingAndServiceConflict_ABCD extends TestCase implements RecordingAlertListener, RecordingChangedListener
    {
        Vector m_eventList;

        private OcapLocator[] m_locators;

        TestRecordingAndServiceConflict_ABCD(OcapLocator[] locators)
        {
            m_locators = locators;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecordingAndServiceConflict_ABCD";
        }

        public void runTest()
        {
            m_eventList = new Vector();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            rm.addRecordingChangedListener(this);
            rm.addRecordingAlertListener(this);

            String recording_A = "#1-Recording_A";
            String recording_B = "#2-Recording_B";
            String recording_C = "#3-Recording_C";
            String serviceContext_C = "#4-ServiceContext_C";

            //
            // clear the schedule of pending tasks
            //
            reset();

            //
            // Init the service context
            //
            initSC();

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            //
            // Schedule the registration of the default DvrTest contention
            // handler
            // The default resource contention handler will prioritize
            // lexographically by name
            // So, "#1" will be prioritized, first, "#2" second, and "#3" third
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800, 30000));

            //
            // Schedule 3 recordings - two starting in 1 minute, the other
            // starting in 2 minutes
            //
            m_eventScheduler.scheduleCommand(new Record(recording_A, m_locators[0], now + MINUTE, 5*MINUTE, 4000));
            m_eventScheduler.scheduleCommand(new Record(recording_C, m_locators[2], now + MINUTE, 5*MINUTE, 4000));
            m_eventScheduler.scheduleCommand(new Record(recording_B, m_locators[1], now + 2*MINUTE, 5*MINUTE, 6000));

            //
            // Schedule service selection for C to start before recordings
            // (20 seconds into test)
            //
            m_eventScheduler.scheduleCommand(new SelectService(serviceContext_C, m_locators[2], 20000));

            //
            // Check Recordings' status before they start
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_C, OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 10010));

            //
            // Check Recordings' status after Recording A should have started
            // (25 seconds after A starts)
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.IN_PROGRESS_STATE, MINUTE + 25000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, MINUTE + 25010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_C, OcapRecordingRequest.FAILED_STATE, MINUTE + 25010));

            //
            // Check Recordings' status after Recording B should have started
            // (25 seconds after B starts)
            //
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_A, OcapRecordingRequest.IN_PROGRESS_STATE, 2*MINUTE + 25000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_B, OcapRecordingRequest.IN_PROGRESS_STATE, 2*MINUTE + 25010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording_C, OcapRecordingRequest.FAILED_STATE, 2*MINUTE + 25010));

            //
            // Delete recordings and service after 3 minutes
            // Note: Some of these WILL fail - which fail depend upon the test
            //
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recording_A, 3*MINUTE));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(recording_B, 3*MINUTE + 30000));

            // m_eventScheduler.scheduleCommand(new
            // DestroyService(serviceContext_C, true /* ignore failure */,
            // 240000));

            //
            // Verify that the resource contention warning has only been 
            // envoked once
            //
            m_eventScheduler.scheduleCommand(new CheckResourceContentionWarningCount(1, 3*MINUTE + 40000));

            //
            // Schedule the deletion of the contention handler
            //
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 4*MINUTE + 10000));

            //
            // Start the test
            //
            m_eventScheduler.run(10000);

            //
            // After test completion, check for test success/failure
            //
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: INTERNAL ERROR-DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }


            rm.removeRecordingChangedListener(this);
            rm.removeRecordingAlertListener(this);

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();

            //
            // Print the list of events that were received after 3.5 minutes
            //
            /*
             * m_eventScheduler.scheduleCommand( new PrintEvents( m_eventList,
             * null, 210000 ) );
             */

        } // END runTest(GatingTest-RecordingAndServiceConflict_ABC)

        /**
         * Handler for recording notifications
         */
        public void recordingAlert(RecordingAlertEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);
                DvrEventPrinter.printRecordingAlertEvent(e);
            }
        } // END recordingAlert()

        /**
         * Handler for state, and list change events.
         */
        public void recordingChanged(RecordingChangedEvent e)
        {
            synchronized (m_eventList)
            {
                m_eventList.addElement(e);

                DvrEventPrinter.printRecordingChangedEvent(e);
            }
        } // END recordingChanged()
    } // END class class TestRecordingAndServiceConflict_ABC

    // ---------------------------------------------------------------
    // Other utility classes
    class PrintEvents extends EventScheduler.NotifyShell
    {
        public PrintEvents(Vector eventQueue, String name, long taskTriggerTimer)
        {
            super(taskTriggerTimer);
            m_rr = name;
            m_eventQueue = eventQueue;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<PrintEvents::ProcessCommand>>>>");
            DvrEventPrinter.printEventQueue(m_eventQueue);
        }

        String m_rr;

        Vector m_eventQueue;
    } // END class PrintEvents
} // END class TestTunerConflict_Gating
