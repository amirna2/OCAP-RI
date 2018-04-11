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
 * Created on Mar 28, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.havi.ui.HScene;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.net.OcapLocator;

/**
 * @author fsmith
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestBasicRecordingAndPlayback extends DvrTest
{

    private HScene sc = null;

    TestBasicRecordingAndPlayback(HScene scene, Vector locators)
    {
        super(locators);
        sc = scene;
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestScheduledRecording(m_locators, 0, true));
        //tests.addElement(new TestScheduledRecording(m_locators, 0, false));
        tests.addElement(new TestScheduledRecording(m_locators, 255, true));
        //tests.addElement(new TestScheduledRecording(m_locators, 255, false));

        tests.addElement(new TestRecordingPlayback((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRecordingPlaybackDeleteInProgress((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRecordingPlaybackUsingJMF(sc, (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRecordingPlaybackSetMediaTime((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * Perform a scheduled recording, and confirm its successful completion
     * 
     * @param locators
     */
    public class TestScheduledRecording extends TestCase implements RecordingChangedListener
    {
        private OcapLocator m_locator;

        private Vector m_locators;
        private int recState;
        private int m_type;
        private String m_typeStr;
        private boolean m_recPass;

        TestScheduledRecording(Vector locators, int type, boolean recPass)
        {
            m_locators = locators;
            m_type = type;
            m_typeStr="Digital";
            if (type == 255) m_typeStr="Analog";
            m_recPass = recPass;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestScheduledRecording_" + 
                   (m_type==255 ? "Analog(":"Digital(") + 
                   (m_recPass ? "rec should pass)" : "rec should fail)");
        }

        public void runTest()
        {
            recState = 0;
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            RecordingManager rm = RecordingManager.getInstance();
            rm.addRecordingChangedListener(this);

            // clear the schedule of pending tasks
            reset();

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

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 30000, 500));

            // Display the recordings
            m_eventScheduler.scheduleCommand(new PrintRecordings(60000));

            // Check recording - confirm it is in the COMPLETED_STATE
            if (m_recPass) 
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 70000));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.FAILED_STATE, 70000));
            }

            m_eventScheduler.run(10000);

            if ((recState != 3 && m_recPass) ||
                (recState == 3 && !m_recPass))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Failed to transition to proper states: last sucessful state" + recState);
            }

            rm.removeRecordingChangedListener(this);

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


        public void recordingChanged(RecordingChangedEvent arg0)
        {
            DvrEventPrinter.printEvent(arg0);
            switch (recState)
            {
                case 0:
                    if ((arg0.getOldState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                            && (arg0.getState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                            && (arg0.getChange() == RecordingChangedEvent.ENTRY_ADDED))
                    {
                        recState = 1;
                        System.out.println("State change #1 PASSED");
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("State change #1 FAILED");
                    }
                    break;
                case 1:
                    if ((arg0.getOldState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                            && (arg0.getState() == LeafRecordingRequest.IN_PROGRESS_STATE)
                            && (arg0.getChange() == RecordingChangedEvent.ENTRY_STATE_CHANGED))
                    {
                        recState = 2;
                        System.out.println("State change #2 PASSED");
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("State change #2 FAILED");
                    }
                    break;
                case 2:
                    if ((arg0.getOldState() == LeafRecordingRequest.IN_PROGRESS_STATE)
                            && (arg0.getState() == LeafRecordingRequest.COMPLETED_STATE)
                            && (arg0.getChange() == RecordingChangedEvent.ENTRY_STATE_CHANGED))
                    {
                        recState = 3;
                        System.out.println("State change #3 PASSED");
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("State change #3 FAILED");
                    }
                    break;
            }
        }
    }


    public class TestRecordingPlaybackDeleteInProgress extends TestCase
    {
        TestRecordingPlaybackDeleteInProgress(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecordingPlaybackDeleteInProgress";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;

            // Initialize ServiceContext for playback
            initSC();

            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 2000, 40000, 500));

            // verify recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.IN_PROGRESS_STATE, 19000));

            // Schedule asynchronous recorded service selection call
            //   (recording, timetoDie, triggerTime, ignoreEvent)
            m_eventScheduler.scheduleCommand(new SelectRecordedServiceAsync("Recording1", 0, 21000, true));

            // delete recording while it is IN_PROGRESS_STATE
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 30000));

            // wait ~50 secs for recording to play through
            m_eventScheduler.run(50000); 

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() +" completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() +" completed: FAILED - "+m_failedReason);
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }


    public class TestRecordingPlayback extends TestCase
    {
        TestRecordingPlayback(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecordingPlayback";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;

            // Initialize ServiceContext for playback
            initSC();

            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 2000, 30000, 500));

            // verify recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 40000));

            // Schedule recorded service selection call
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 41000));

            // wait ~40 secs for recording to play through
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
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() +" completed: FAILED - "+m_failedReason);
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

        private OcapLocator m_locator;
    }

    public class TestRecordingPlaybackUsingJMF extends TestCase
    {
        private HScene sc;

        TestRecordingPlaybackUsingJMF(HScene scene, OcapLocator locator)
        {
            sc = scene;
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecordingPlaybackUsingJMF";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 2000, 30000, 500));

            // Verify recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 40000));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new SelectRecordedServiceUsingJMFPlayer("Recording1", 41000, sc));

            // wait ~40 secs for recording to play through
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
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() +" completed: FAILED - "+m_failedReason);
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestRecordingPlaybackSetMediaTime extends TestCase
    {
        TestRecordingPlaybackSetMediaTime(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecordingPlaybackSetMediaTime";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 2000, 30000, 500));

            // Verify recording state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", OcapRecordingRequest.COMPLETED_STATE, 40000));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new SetMediaTimeTest("Recording1", 41000));

            // wait ~40 secs for recording to play through
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
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() +" completed: FAILED - "+m_failedReason);
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

}
