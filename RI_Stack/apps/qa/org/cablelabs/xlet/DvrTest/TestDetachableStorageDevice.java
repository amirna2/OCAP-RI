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
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.net.OcapLocator;

import org.cablelabs.xlet.DvrTest.DvrTest.CheckFailedException;

public class TestDetachableStorageDevice extends StorageTest
{

    Vector m_devices = null;

    TestDetachableStorageDevice(Vector devices, Vector locators)
    {
        super(locators);
        m_devices = devices;
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestDetachDeviceDuringRec((String) m_devices.elementAt(0),
                (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestAttachDeviceDuringRec((String) m_devices.elementAt(0),
                (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestAttachDeviceDuringRecFailed((String) m_devices.elementAt(0),
                (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestDetachAndAttachDeviceDuringRec((String) m_devices.elementAt(0),
                (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestDetachAndAttachDeviceDuringRecFailed((String) m_devices.elementAt(0),
                (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestDetachDeviceDuringRecAndDelete((String) m_devices.elementAt(0),
                (OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * 
     * @param locators
     */
    public class TestDetachDeviceDuringRec extends TestCase
    {
        private OcapLocator m_locator;

        String m_device;

        TestDetachDeviceDuringRec(String device, OcapLocator locator)
        {
            m_device = device;
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDetachDeviceDuringRec";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Get the storage device and find an MSV to record and
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 60000,
                    getFirstMSVinProxy(m_device), 500));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 44000));

            // Detach device
            m_eventScheduler.scheduleCommand(new detachDevice(m_device, 45000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 70000));

            // Attach device
            m_eventScheduler.scheduleCommand(new attachDevice(m_device, 110000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.INCOMPLETE_STATE, 120000));

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

    /**
     * 
     * @param locators
     */
    public class TestAttachDeviceDuringRec extends TestCase
    {
        private OcapLocator m_locator;

        String m_device;

        TestAttachDeviceDuringRec(String device, OcapLocator locator)
        {
            m_device = device;
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestAttachDeviceDuringRec";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Get the storage device and find an MSV to record and
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 60000,
                    getFirstMSVinProxy(m_device), 500, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            // Detach device
            m_eventScheduler.scheduleCommand(new detachDevice(m_device, 5000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 44000));

            // Attach device
            m_eventScheduler.scheduleCommand(new attachDevice(m_device, 45000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 70000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.INCOMPLETE_STATE, 110000));
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

    /**
     * 
     * @param locators
     */
    public class TestAttachDeviceDuringRecFailed extends TestCase
    {
        private OcapLocator m_locator;

        String m_device;

        TestAttachDeviceDuringRecFailed(String device, OcapLocator locator)
        {
            m_device = device;
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestAttachDeviceDuringRecFailed";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Get the storage device and find an MSV to record and
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 60000,
                    getFirstMSVinProxy(m_device), 500, OcapRecordingProperties.RECORD_IF_NO_CONFLICTS));

            // Detach device
            m_eventScheduler.scheduleCommand(new detachDevice(m_device, 5000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 44000));

            // Attach device
            m_eventScheduler.scheduleCommand(new attachDevice(m_device, 45000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 70000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.INCOMPLETE_STATE, 110000));
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

    /**
     * 
     * @param locators
     */
    public class TestDetachAndAttachDeviceDuringRec extends TestCase
    {
        private OcapLocator m_locator;

        String m_device;

        TestDetachAndAttachDeviceDuringRec(String device, OcapLocator locator)
        {
            m_device = device;
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDetachAndAttachDeviceDuringRec";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Get the storage device and find an MSV to record and
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 60000,
                    getFirstMSVinProxy(m_device), 500));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 44000));

            // Detach device
            m_eventScheduler.scheduleCommand(new detachDevice(m_device, 45000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 70000));

            // Attach device
            m_eventScheduler.scheduleCommand(new attachDevice(m_device, 71000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 84000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.INCOMPLETE_STATE, 110000));

            m_eventScheduler.scheduleCommand(new CheckRecordedServices("Recording1", true, 2, 120000));

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

    /**
     * 
     * @param locators
     */
    public class TestDetachAndAttachDeviceDuringRecFailed extends TestCase
    {
        private OcapLocator m_locator;

        String m_device;

        TestDetachAndAttachDeviceDuringRecFailed(String device, OcapLocator locator)
        {
            m_device = device;
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDetachAndAttachDeviceDuringRecFailed";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Get the storage device and find an MSV to record and
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 60000,
                    getFirstMSVinProxy(m_device), 500));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 14000));

            // Detach device
            m_eventScheduler.scheduleCommand(new detachDevice(m_device, 15000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 45000));

            // Attach device
            m_eventScheduler.scheduleCommand(new attachDevice(m_device, 100000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.FAILED_STATE, 101000));

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

    /**
     * 
     * @param locators
     */
    public class TestDetachDeviceDuringRecAndDelete extends TestCase
    {
        private OcapLocator m_locator;

        String m_device;

        TestDetachDeviceDuringRecAndDelete(String device, OcapLocator locator)
        {
            m_device = device;
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDetachDeviceDuringRecAndDelete";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Get the storage device and find an MSV to record and
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 60000,
                    getFirstMSVinProxy(m_device), 500));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 44000));

            // Detach device
            m_eventScheduler.scheduleCommand(new detachDevice(m_device, 45000));

            // Check recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 60000));

            // Delete all recordings
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 65000));

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
}
