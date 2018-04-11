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

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.RecordingFailedException;

import org.cablelabs.xlet.DvrTest.DvrTest.CheckFailedException;
import org.cablelabs.xlet.DvrTest.DvrTest.CheckRecordedServices;
import org.cablelabs.xlet.DvrTest.DvrTest.ConfirmRecordingReq_CheckState;
import org.cablelabs.xlet.DvrTest.DvrTest.Record;
import org.cablelabs.xlet.DvrTest.DvrTest.ScheduledLog;
import org.cablelabs.xlet.DvrTest.DvrTest.TestCase;
import org.cablelabs.xlet.DvrTest.StorageTest.attachDevice;
import org.cablelabs.xlet.DvrTest.StorageTest.detachDevice;
import org.cablelabs.xlet.DvrTest.TestDetachableStorageDevice.TestDetachDeviceDuringRec;
import org.cablelabs.xlet.DvrTest.TestPersistentRecordings.AddAppData;

public class TestDetachedDeviceOverReboot extends StorageTest
{

    Vector m_devices = null;

    TestDetachedDeviceOverReboot(Vector devices, Vector locators)
    {
        super(locators);
        m_devices = devices;
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new ScheduleRecPriorToReboot((String) m_devices.elementAt(0),
                (OcapLocator) m_locators.elementAt(0), true, true));
        tests.addElement(new ScheduleRecPriorToReboot((String) m_devices.elementAt(0),
                (OcapLocator) m_locators.elementAt(0), true, false));
        tests.addElement(new ScheduleRecPriorToReboot((String) m_devices.elementAt(0),
                (OcapLocator) m_locators.elementAt(0), false, true));
        tests.addElement(new ScheduleRecPriorToReboot((String) m_devices.elementAt(0),
                (OcapLocator) m_locators.elementAt(0), false, false));
        tests.addElement(new RecordingStateAfterReboot(true, true));
        tests.addElement(new RecordingStateAfterReboot(true, false));
        tests.addElement(new RecordingStateAfterReboot(false, true));
        tests.addElement(new RecordingStateAfterReboot(false, false));
        return tests;
    }

    public class ScheduleRecPriorToReboot extends TestCase
    {

        private String m_device;

        private OcapLocator m_locator;

        private boolean m_started;

        private boolean m_attached;

        public ScheduleRecPriorToReboot(String device, OcapLocator locator, boolean started, boolean attached)
        {
            m_device = device;
            m_locator = locator;
            m_started = started;
            m_attached = attached;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "ScheduleRecPriorToReboot - " + (m_started ? "recording started" : "recording pending") + " ; "
                    + (m_attached ? "device attached" : "device detached");
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long duration = 120000;

            // clear the schedule of pending tasks
            reset();

            // Get the storage device and find an MSV to record and
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, duration,
                    getFirstMSVinProxy(m_device), 500));

            //
            m_eventScheduler.scheduleCommand(new AddAppData("Recording1", "Test" + (m_started ? "Started" : "Pending")
                    + (m_attached ? "Attached" : "Detached"), "Recording1", 2000));

            // Detach device
            if (!m_attached)
            {
                m_eventScheduler.scheduleCommand(new detachDevice(m_device, 5000));
            }

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 10000));

            if (m_started)
            {
                if (m_attached)
                {
                    // Check recording
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                            OcapRecordingRequest.IN_PROGRESS_STATE, 40000));
                }
                else
                {
                    // Check recording
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                            OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 40000));
                }
                // Log message out
                m_eventScheduler.scheduleCommand(new ScheduledLog(
                        "------------------------REBOOT----------------------", 45000));
                m_eventScheduler.scheduleCommand(new ScheduledLog("------------length : " + (duration - 15000)
                        + " msec left--------------------", 45100));
            }
            else
            {
                // Log message out
                m_eventScheduler.scheduleCommand(new ScheduledLog(
                        "-----------------------REBOOT-----------------------", 15000));
                m_eventScheduler.scheduleCommand(new ScheduledLog("--------------length : " + duration
                        + " msec in 15 secs----------------------", 15100));
            }

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
            }
        }
    }

    public class RecordingStateAfterReboot extends TestCase
    {

        private boolean m_started;

        private boolean m_attached;

        public RecordingStateAfterReboot(boolean started, boolean attached)
        {
            m_started = started;
            m_attached = attached;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "RecordingStateAfterReboott - " + (m_started ? "recording started" : "recording pending") + " ; "
                    + (m_attached ? "device attached" : "device detached");
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();
            if (m_started)
            {
                if (m_attached)
                {
                    getRecReq("Recording1", "TestStartedAttached", "Recording1");
                    new CheckRecordedServices("Recording1", true, 2, 0).ProcessCommand();
                    // new
                    // CheckFailedException("Recording3",RecordingFailedException.POWER_INTERRUPTION,0).ProcessCommand();
                }
                else
                {
                    getRecReq("Recording1", "TestStartedDetached", "Recording1");
                    new CheckRecordedServices("Recording1", true, 1, 0).ProcessCommand();
                    // new
                    // CheckFailedException("Recording3",RecordingFailedException.POWER_INTERRUPTION,0).ProcessCommand();
                }
            }
            else
            {
                if (m_attached)
                {
                    getRecReq("Recording1", "TestPendingAttached", "Recording1");
                    new CheckRecordedServices("Recording1", true, 1, 0).ProcessCommand();
                    // new
                    // CheckFailedException("Recording3",RecordingFailedException.POWER_INTERRUPTION,0).ProcessCommand();
                }
                else
                {
                    getRecReq("Recording1", "TestPendingDetached", "Recording1");
                    new CheckRecordedServices("Recording1", true, 1, 0).ProcessCommand();
                    // new
                    // CheckFailedException("Recording3",RecordingFailedException.POWER_INTERRUPTION,0).ProcessCommand();
                }
            }

            // Get state of recordings
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject("Recording1");

            // Print out results
            DVRTestRunnerXlet.log("Disk fill - recording information");
            DVRTestRunnerXlet.log("TestStartedAttached - state: " + DvrEventPrinter.xletState(rr));
        }
    }
}
