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
 * Created on Feb 21, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.OcapRecordingProperties;

import org.cablelabs.xlet.DvrTest.DvrTest.PrintRecordings;
import org.cablelabs.xlet.DvrTest.DvrTest.RecordBySC;
import org.cablelabs.xlet.DvrTest.DvrTest.SelectRecordedService;
import org.cablelabs.xlet.DvrTest.DvrTest.SelectService;
import org.cablelabs.xlet.DvrTest.DvrTest.StopBroadcastService;
import org.cablelabs.xlet.DvrTest.DvrTest.TestCase;

/**
 * @author fsmith
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestImmediateRecording extends DvrTest
{

    TestImmediateRecording(Vector locators, boolean testingSI)
    {
        super(locators, testingSI);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestBasicTSRecording(m_locators, 0));
        tests.addElement(new TestBasicTSRecording(m_locators, 255));

        tests.addElement(new TestImmediateTSRecording(m_locators, 0));
        tests.addElement(new TestImmediateTSRecording(m_locators, 255));

        tests.addElement(new TestSCRecordingTuneAway((OcapLocator) m_locators.elementAt(0), (OcapLocator) m_locators.elementAt(1)));

        tests.addElement(new TestTSRecordingSchedRecording((OcapLocator) m_locators.elementAt(0)));

        // tests.addElement(new TestImmediateSCRecording((OcapLocator)m_locators.elementAt(1)));
        // tests.addElement(new TestImmediateTSwSIRecording((OcapLocator)m_locators.elementAt(1)));

        return tests;
    }

    public class TestBasicTSRecording extends TestCase implements ResourceContentionHandler
    {
        private OcapLocator m_locator;

        private Vector m_locators;
        private int m_type;
        private String m_typeStr;

        TestBasicTSRecording(Vector locators, int type)
        {
            m_locators = locators;
            m_type = type;
            m_typeStr="Digital";
            if (type == 255) m_typeStr="Analog";
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestBasicTS Recording_" + 
                   (m_type==255 ? "Analog":"Digital");
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // Setup resource ContentionHandler - should not be envoked
            ResourceContentionManager rcm = ResourceContentionManager.getInstance();
            rcm.setResourceContentionHandler(this);

            // Initialize ServiceContext for playback
            initSC();

            // clear the schedule of pending tasks
            reset();

            // get the locator with the requested service type
            boolean gotCorrectSvc = false;
            for (int i = 0; i < m_locators.size(); i++)
            {
                m_locator = (OcapLocator)m_locators.elementAt(i);
                if ((m_type == 255 && m_locator.getModulationFormat() == 255) ||
                    (m_type != 255 && m_locator.getModulationFormat() != 255))
                {
                    gotCorrectSvc = true;
                    break;
                }
            }

            String svcType = "Digital";
            if (m_locator.getModulationFormat() == 255) svcType = "Analog";
            if (!gotCorrectSvc)
            {
                DVRTestRunnerXlet.log("!!!!!!!!!!!!!!!!!");
                DVRTestRunnerXlet.log(getName() +": could not find expected service type "+m_typeStr +", use " +svcType +" instead");
                DVRTestRunnerXlet.log("!!!!!!!!!!!!!!!!!");
            }


            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 1000));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new RecordBySC("Recording1", now + 30*SEC, 30*SEC, 70000));

            // the verification for recording in IN_PROGRESS_STATE is too
            // time dependant so should be removed.
            //m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.IN_PROGRESS_STATE, 75000)); 

            // Display the recordings
            m_eventScheduler.scheduleCommand(new StopBroadcastService(83000));
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 92000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 130000));

            // wait for the presenation to complete
            m_eventScheduler.run(40000);

            // Dissolve the resource contetion handler
            rcm.setResourceContentionHandler(null);
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

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.ocap.resource.ResourceContentionHandler#resolveResourceContention
         * (org.ocap.resource.ResourceUsage, org.ocap.resource.ResourceUsage[])
         */
        public ResourceUsage[] resolveResourceContention(ResourceUsage arg0, ResourceUsage[] arg1)
        {
            m_failed = TEST_FAILED;

            DVRTestRunnerXlet.log("***  newRequest: " + getNameForResourceUsage(arg0) + " (" + arg0 + ")");

            for (int i = 0; i < arg1.length; i++)
            {
                DVRTestRunnerXlet.log("***  curRes[" + i + "]: " + getNameForResourceUsage(arg1[i]) + " (" + arg1[i]
                        + ")");
            }

            DVRTestRunnerXlet.log("Resouce Contention Handler envoked when not expected");
            m_failedReason = "Resouce Contention Handler envoked when not expected ";
            return arg1;
        }

        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            DVRTestRunnerXlet.log("!!!!!!!! Warning called on " + getNameForResourceUsage(newRequest) + " !!!!!!!!!");
        }
    }

    public class TestImmediateTSRecording extends TestCase
    {
        private OcapLocator m_locator;

        private Vector m_locators;
        private int m_type;
        private String m_typeStr;

        TestImmediateTSRecording(Vector locators, int type)
        {
            m_locators = locators;
            m_type = type;
            m_typeStr="Digital";
            if (type == 255) m_typeStr="Analog";
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestImmediateTSRecording_" + 
                   (m_type==255 ? "Analog":"Digital");

        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // Initialize ServiceContext for playback
            initSC();

            // clear the schedule of pending tasks
            reset();

            // get the locator with the requested service type
            boolean gotCorrectSvc = false;
            for (int i = 0; i < m_locators.size(); i++)
            {
                m_locator = (OcapLocator)m_locators.elementAt(i);
                if ((m_type == 255 && m_locator.getModulationFormat() == 255) ||
                    (m_type != 255 && m_locator.getModulationFormat() != 255))
                {
                    gotCorrectSvc = true;
                    break;
                }
            }

            String svcType = "Digital";
            if (m_locator.getModulationFormat() == 255) svcType = "Analog";
            if (!gotCorrectSvc)
            {
                DVRTestRunnerXlet.log("!!!!!!!!!!!!!!!!!");
                DVRTestRunnerXlet.log(getName() +": could not find expected service type "+m_typeStr +", use " +svcType +" instead");
                DVRTestRunnerXlet.log("!!!!!!!!!!!!!!!!!");
            }


            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 1000));

            // Schedule the record call
            // hack - 0 start time = magic value meaning "now"
            m_eventScheduler.scheduleCommand(new RecordBySC("Recording1", 0, 30 * 1000, 50000));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 82000));

            // Display the recordings
            m_eventScheduler.scheduleCommand(new StopBroadcastService(83000));
            // select the recording
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 92000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 130000));

            // wait for the presenation to complete
            m_eventScheduler.run(40000);

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

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();
        }
    }

    public class TestTSRecordingSchedRecording extends TestCase
    {
        TestTSRecordingSchedRecording(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestTSRecordingNIRecording";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // Initialize ServiceContext for playback
            DVRTestRunnerXlet.log(" Test_Rec_SC_while_SR_In_Progress - initSC ");
            initSC();

            // clear the schedule of pending tasks
            DVRTestRunnerXlet.log(" Test_Rec_SC_while_SR_In_Progress reset ");

            reset();

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 1000));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new RecordBySC("SCRecording1", now + 30*SEC, 140000, 30*SEC));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + MINUTE, 30*SEC, MINUTE));

            // destoy the current ServiceContext for proper life cycle
            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("SCRecording1", OcapRecordingRequest.IN_PROGRESS_STATE, 110000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 110500));

            m_eventScheduler.scheduleCommand(new StopBroadcastService(200000));

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 210000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("SCRecording1", OcapRecordingRequest.COMPLETED_STATE, 210500));

            // Play Recording
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 250000));

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 310000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("SCRecording1", OcapRecordingRequest.COMPLETED_STATE, 310500));

            // Play Service Context Recording
            m_eventScheduler.scheduleCommand(new SelectRecordedService("SCRecording1", 300000));

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 410000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("SCRecording1", OcapRecordingRequest.COMPLETED_STATE, 410500));

            m_eventScheduler.run(5000);

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

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();
        }

        private OcapLocator m_locator;
    }

    public class TestImmediateSCRecording extends TestCase
    {
        TestImmediateSCRecording(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestImmediateSCRecording (recording <now> bug)";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            m_eventScheduler.scheduleCommand(new initServiceContext(1000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 5000));

            // Schedule the record call
            // hack - 0 start time = magic value meaning "now"
            m_eventScheduler.scheduleCommand(new RecordBySC("Recording1", 0, 30 * 1000, 50000));

            // Display the recordings
            m_eventScheduler.scheduleCommand(new StopBroadcastService(83000));
            // select the recording
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 92000));

            // wait for the presenation to complete
            m_eventScheduler.run(40000);

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

    public class TestImmediateTSwSIRecording extends TestCase
    {
        TestImmediateTSwSIRecording(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestImmediateTSwSIRecording";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // Initialize ServiceContext for playback
            initSC();

            // clear the schedule of pending tasks
            reset();
            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 1000));

            SIChecker siChecker = new SIChecker(m_locator, 6000);
            RecordedSIChecker recordedChecker = new RecordedSIChecker(siChecker, "Recording1", 98000);

            if (testingSI)
            {
                // runs at 1000
                m_eventScheduler.scheduleCommand(siChecker);
            }

            // Schedule the record call
            // hack - 0 start time = magic value meaning "now"
            m_eventScheduler.scheduleCommand(new RecordBySC("Recording1", 0, 30 * 1000, 50000));

            // Display the recordings
            m_eventScheduler.scheduleCommand(new StopBroadcastService(83000));
            // select the recording
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 92000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 130000));

            if (testingSI)
            {
                // runs at 98000
                m_eventScheduler.scheduleCommand(recordedChecker);
            }

            // wait for the presenation to complete
            m_eventScheduler.run(40000);
            if (testingSI)
            {
                if (recordedChecker.siMatched)
                {
                    DVRTestRunnerXlet.log("* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");
                    DVRTestRunnerXlet.log("-> The Recorded SI matched the broadcast SI.  DVRSI SUCCESS.   <-");
                    DVRTestRunnerXlet.log("* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");
                }
                else
                {
                    DVRTestRunnerXlet.log("* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");
                    DVRTestRunnerXlet.log("->The Recorded SI did not match the boradcast SI.  DVRSI FAILURE.<-");
                    DVRTestRunnerXlet.log("* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *");
                }
            }

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

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();
        }

        private OcapLocator m_locator;
    }

    public class TestSCRecordingTuneAway extends TestCase
    {
        TestSCRecordingTuneAway(OcapLocator locator, OcapLocator locator2)
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
            return "TestSCRecordingTuneAway";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            OcapLocator loc = null;
            long now = System.currentTimeMillis();

            // Initialize ServiceContext for playback
            initSC();

            // clear the schedule of pending tasks
            reset();
            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 1000));

            // Schedule the record call (with start time in the past)
            m_eventScheduler.scheduleCommand(new RecordBySC("Recording1", now + 30*SEC, 6*MINUTE, 40000));

            // Schedule the second service selection call while Recording1
            // is in progress
            m_eventScheduler.scheduleCommand(new SelectService(m_locator2, 70000));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.INCOMPLETE_STATE, 85000));

            // Display the recordings
            m_eventScheduler.scheduleCommand(new StopBroadcastService(125000));
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 140000));

            // Check recording before and after stop timer is fired if
            // record with conflicts
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.INCOMPLETE_STATE, 150000));

            // wait for the presenation to complete
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

            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();
        }

        private OcapLocator m_locator;

        private OcapLocator m_locator2;
    }

}
