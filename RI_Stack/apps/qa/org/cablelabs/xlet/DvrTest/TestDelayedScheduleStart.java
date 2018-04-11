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

import java.util.Date;
import java.util.Vector;

import org.havi.ui.HScene;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.net.OcapLocator;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.dvr.RecordingAlertEvent;

import org.cablelabs.xlet.DvrTest.DvrTest.PrintRecordings;
import org.cablelabs.xlet.DvrTest.DvrTest.Record;

/**
 * Tests for SCR 20051104
 */
public class TestDelayedScheduleStart extends DvrTest
{

    private HScene sc = null;

    TestDelayedScheduleStart(HScene scene, Vector locators)
    {
        super(locators);
        sc = scene;
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestDelayedRecordingStart((OcapLocator) m_locators.elementAt(0), (OcapLocator) m_locators.elementAt(1)));
        tests.addElement(new TestDelayedRecordingStartPrerun((OcapLocator) m_locators.elementAt(0), (OcapLocator) m_locators.elementAt(1)));
        tests.addElement(new TestStartRecordingManager());

        return tests;
    }

    public class TestStartRecordingManager extends TestCase
    {
        TestStartRecordingManager()
        {

        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestStartRecordingManager";
        }

        public void runTest()
        {
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            DVRTestRunnerXlet.log("Calling recording manager to signal start.");
            long t = System.currentTimeMillis();
            rm.signalRecordingStart();
            t = System.currentTimeMillis() - t;
            DVRTestRunnerXlet.log("Calling recording manager to signal start - completed. Time(ms): " + t);
        }
    }

    public class TestDelayedRecordingStartPrerun extends TestCase
    {
        TestDelayedRecordingStartPrerun(OcapLocator locator1, OcapLocator locator2)
        {
            m_locator1 = locator1;
            m_locator2 = locator2;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test Failed Delayed Start Recording - Pre boot run";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new StartRecordingManager(100));
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator1, now + 120000, 600000, 500));  //start in 2 mins duration is 10 mins
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator2, now + 120000, 600000, 1000)); //start in 2 mins; duration is 10 mins

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("Test Failed Delayed Start Recording - Pre boot run: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("Test Failed Delayed Start Recording - Pre boot run: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("Test Failed Delayed Start Recording - Pre boot run: PASSED. ");
                DVRTestRunnerXlet.log("Reboot now and run Test Failed Delayed Start Recording.");
            }
        }

        private OcapLocator m_locator1;

        private OcapLocator m_locator2;

    }

    /**
     * @param locators
     */
    public class TestDelayedRecordingStart extends TestCase
    {
        TestDelayedRecordingStart(OcapLocator locator1, OcapLocator locator2)
        {
            m_locator1 = locator1;
            m_locator2 = locator2;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test Failed Delayed Start Recording";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            initSC();
            // clear the schedule of pending tasks
            reset();

            // register RCH
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler( new LoggingResourceContentionHandler(), 200));

            // Register change listener
            RegisterChangeListener rcl = new RegisterChangeListener(400);
            m_eventScheduler.scheduleCommand(rcl);

            // select service
            m_eventScheduler.scheduleCommand(new SelectService(m_locator1, 5000));

            // perform NI tune
            m_eventScheduler.scheduleCommand(new TuneNetworkInterface(m_locator2, 20000));

            // perform a failed recording attempt
            m_eventScheduler.scheduleCommand(new FailedRecord("Failed 1", m_locator1, now + 30000, 60000, 25000)); //start in 30 secs; duration is 1 minute

            // call start (or wait until start trigger)
            m_eventScheduler.scheduleCommand(new StartRecordingManager(45000));

            // cleanup
            m_eventScheduler.scheduleCommand(new RemoveChangeListener(rcl, 125000));
            m_eventScheduler.scheduleCommand(new RemoveContentionHandler(126000));

            m_eventScheduler.run(30000);

            cleanSC();
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestScheduledAnalogRecording completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("Test Failed Delayed Start Recording: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("Test Failed Delayed Start Recording: PASSED");
            }
        }

        private OcapLocator m_locator1;

        private OcapLocator m_locator2;
    }

    /**
     * Local definition of modified record command This should be executed prior
     * to issuing the start recording call and so should generate an illegal
     * state exception.
     */
    class FailedRecord extends EventScheduler.NotifyShell
    {
        FailedRecord(String recordingName, OcapLocator source, long startTime, long duration, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = 1000 * 60 * 60 * 24; // 24 hour expiration
            m_recordingName = recordingName;
        }

        FailedRecord(String recordingName, OcapLocator source, long startTime, long duration, long taskTriggerTime,
                long expiration)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = expiration;
            m_recordingName = recordingName;
        }

        public void ProcessCommand()
        {
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            boolean failed = false;
            try
            {
                OcapRecordingRequest rr;
                LocatorRecordingSpec lrs;
                OcapRecordingProperties orp;
                System.out.println("DVRUtils: issuing recording:" + m_source[0] + " StartTime:" + m_startTime + " Duration:" + m_duration);

                orp = new OcapRecordingProperties(
                              OcapRecordingProperties.HIGH_BIT_RATE, 
                              m_expiration, 
                              OcapRecordingProperties.DELETE_AT_EXPIRATION, 
                              OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, 
                              null, null, null);

                lrs = new LocatorRecordingSpec(m_source, new Date(m_startTime), m_duration, orp);

                m_defaultRecordingName = m_recordingName;

                rr = (OcapRecordingRequest) rm.record(lrs);

                m_defaultRecordingName = null;

                if (rr != null)
                {
                    insertObject(rr, m_recordingName);
                }
            }
            catch (Exception e)
            {
                if (e instanceof IllegalStateException)
                {
                    DVRTestRunnerXlet.log("DvrTest: received EXPECTED failure in Record due to rm.record() xception: "
                            + e.toString());
                    failed = true;
                }
                e.printStackTrace();
            }

            if (failed == false)
            {
                DVRTestRunnerXlet.log("DvrTest: Did not recieve expected exception!! in FailedRecord command.");
                m_failed = TEST_FAILED;
            }
        }

        private OcapLocator m_source[];

        private long m_startTime;

        private long m_duration;

        private long m_expiration;

        private String m_recordingName;
    }

    class StartRecordingManager extends EventScheduler.NotifyShell
    {
        StartRecordingManager(long taskTriggerTime)
        {
            super(taskTriggerTime);
        }

        public void ProcessCommand()
        {
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            DVRTestRunnerXlet.log("Calling recording manager to signal start.");
            long t = System.currentTimeMillis();
            rm.signalRecordingStart();
            t = System.currentTimeMillis() - t;
            DVRTestRunnerXlet.log("Calling recording manager to signal start - completed. Time(ms): " + t);
        }
    }

    /**
     * Simple logging resource contention handler. grants the new request and
     * logs the conflicts.
     * 
     * 
     */
    private class LoggingResourceContentionHandler implements org.ocap.resource.ResourceContentionHandler
    {
        public ResourceUsage[] resolveResourceContention(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            ResourceUsage retUsages[] = new ResourceUsage[currentReservations.length + 1];

            DVRTestRunnerXlet.log("LoggingResourceContentionHandler called.");
            DVRTestRunnerXlet.log("New request: " + newRequest);

            retUsages[0] = newRequest;
            for (int i = 1; i < currentReservations.length + 1; i++)
            {
                DVRTestRunnerXlet.log("Existing request: " + currentReservations[i - 1]);
                retUsages[i] = currentReservations[i - 1];
            }

            return retUsages;
        }

        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            System.out.println("Contention found");
        }

    }

    class RegisterChangeListener extends EventScheduler.NotifyShell implements RecordingAlertListener
    {
        RegisterChangeListener(long taskTriggerTime)
        {
            super(taskTriggerTime);
        }

        public void ProcessCommand()
        {
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            DVRTestRunnerXlet.log("Calling Recording manager to register change listener.");
            rm.addRecordingAlertListener(this);
        }

        public void recordingAlert(RecordingAlertEvent e)
        {
            DVRTestRunnerXlet.log("Received recording alert event: " + e + " " + e.getRecordingRequest());
        }
    }

    class RemoveChangeListener extends EventScheduler.NotifyShell implements RecordingAlertListener
    {
        RemoveChangeListener(RecordingAlertListener ral, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_ral = ral;
        }

        public void ProcessCommand()
        {
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            DVRTestRunnerXlet.log("Calling Recording manager to remove change listener.");
            rm.removeRecordingAlertListener(m_ral);
        }

        public void recordingAlert(RecordingAlertEvent e)
        {
            DVRTestRunnerXlet.log("Received recording alert event: " + e + " " + e.getRecordingRequest());
        }

        RecordingAlertListener m_ral;
    }

    class RemoveContentionHandler extends EventScheduler.NotifyShell
    {
        RemoveContentionHandler(long taskTriggerTime)
        {
            super(taskTriggerTime);
        }

        public void ProcessCommand()
        {
            try
            {
                DVRTestRunnerXlet.log("<<<<RemoveResourceContentionHandler::ProcessCommand>>>>");
                ResourceContentionManager rcm = org.ocap.resource.ResourceContentionManager.getInstance();
                rcm.setResourceContentionHandler(null);
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Exception registering contention handler");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in RegisterResourceContentionHandler due to rcm.setResourceContentionHandler() exception: "
                        + e.toString());
                return;
            }
        }
    }
}
