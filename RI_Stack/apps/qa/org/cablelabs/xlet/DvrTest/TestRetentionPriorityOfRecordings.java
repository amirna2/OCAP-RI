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
 * Created on Nov 22, 2005
 *
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;

import org.ocap.dvr.OcapRecordedService;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;
import org.ocap.dvr.storage.MediaStorageVolume; //CHANGE MADE FOR iTSB INTEGRATION
//import org.ocap.dvr.storage.AllocateTimeShiftBufferOption;
//import org.ocap.dvr.storage.TimeShiftBufferOption;
import org.ocap.storage.StorageOption;
import org.ocap.dvr.storage.MediaStorageVolume;

//CHANGE MADE FOR iTSB INTEGRATION
//import org.ocap.dvr.storage.AllocateTimeShiftBufferOption;
//import org.ocap.dvr.storage.TimeShiftBufferOption;

/**
 * @author Prasanna
 * 
 */
public class TestRetentionPriorityOfRecordings extends DvrTest
{

    TestRetentionPriorityOfRecordings(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestRetentionPriority1((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRetentionPriority2((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRetentionPriority3((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestRetentionPriority4((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestPlaybackExpiredRecording1((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestPlaybackExpiredRecording2((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestScheduleRealLongRecording((OcapLocator) m_locators.elementAt(0)));
        // tests.addElement(new
        // TestCreateNewTSB((OcapLocator)m_locators.elementAt(0)));
        tests.addElement(new TestPurgeDuringPlayback((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestPurgeOnExpiration((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestPurgeExpired((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * Schedule a series of recordings with different retention priorities
     * 
     * @param locator
     */
    public class TestRetentionPriority1 extends TestCase
    {
        TestRetentionPriority1(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRetentionPriority1";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            /**
             * Schedule 5 recordings:
             * 
             */

            // Recording 1
            // Start time - 30 sec from 'now' (current system time)
            // duration - 70 sec
            // task trigger time - 500 msec
            // Expire - 20 sec from start time
            // rentention priority set to 'DELETE_AT_EXPIRATION'

            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 70000, 500, 20,
                    OcapRecordingProperties.DELETE_AT_EXPIRATION));

            // Recording 2
            // Start time - 60 sec from 'now' (current system time)
            // duration - 60 sec
            // task trigger time - 510 msec
            // Expire - 60 sec from start time
            // rentention priority set to 1

            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 60000, 60000, 510, 60, 1));

            // Recording 3
            // Start time - 100 sec from 'now' (current system time)
            // duration - 40 sec
            // task trigger time - 520 msec
            // Expire - 60 sec from start time
            // rentention priority set to 1

            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 100000, 40000, 520, 60, 1));

            // Recording 4
            // Start time - 200 sec from 'now' (current system time)
            // duration - 30 sec
            // task trigger time - 530 msec
            // Expire - 120 sec from start time
            // rentention priority set to 2
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + 200000, 30000, 530, 120, 2));

            // Recording 5
            // Start time - 300 sec from 'now' (current system time)
            // duration - 60 sec
            // task trigger time - 540 msec
            // Expire - 120 sec from start time
            // rentention priority set to 2

            m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, now + 300000, 60000, 540, 120, 2));

            // Check for deletion after expiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.DELETED_STATE, 370000));

            // Check for completion after expiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.COMPLETED_STATE, 370010));

            // Check for completion after expiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.COMPLETED_STATE, 370020));

            // Check for completion after expiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.COMPLETED_STATE, 370030));

            // Check for completion after expiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.COMPLETED_STATE, 370040));

            m_eventScheduler.run(3000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestRetentionPriority1 completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestRetentionPriority1 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestRetentionPriority1 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }

        private OcapLocator m_locator;
    }

    public class TestPlaybackExpiredRecording1 extends TestCase
    {
        TestPlaybackExpiredRecording1(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestPlaybackExpiredRecording1";
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
            // Recording 1
            // Start time - 1 sec from 'now' (current system time)
            // duration - 30 sec
            // task trigger time - 500 msec
            // Expire - 60 sec from start time
            // rentention priority set to delete_at_expiration

            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 1000, 30000, 500, 60,
                    OcapRecordingProperties.DELETE_AT_EXPIRATION));

            // Check the recording for proper state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 36000));

            // Check for deletion after expiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.DELETED_STATE, 66000));

            // Schedule playback
            // In the middle of playback, the recording expires. Since the
            // retention priority is set to
            // delete_at_expiration, playback should be terminated and recorded
            // service deleted
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 45000));

            m_eventScheduler.run(60000); // wait ~60 secs for recording to play
                                         // through

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestPlaybackExpiredRecording1 completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestPlaybackExpiredRecording1 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestPlaybackExpiredRecording1 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestPlaybackExpiredRecording2 extends TestCase
    {
        TestPlaybackExpiredRecording2(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestPlaybackExpiredRecording2";
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
            // Recording
            // Start time - 1 sec from 'now' (current system time)
            // duration - 30 sec
            // task trigger time - 500 msec
            // Expire - 60 sec from start time
            // retention priority is NOT delete_at_expiration (set it to 1)
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 1000, 30000, 500, 60, 1));

            // Check the recording for proper state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 36000));

            // Check for deletion after expiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 66000));

            // Schedule play call
            // In the middle of playback, the recording expires. Since the
            // retention priority is
            // NOT set to delete_at_expiration, playback should continue
            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 45000));

            m_eventScheduler.run(45000); // wait ~45 secs for recording to play
                                         // through

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestPlaybackExpiredRecording2 completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestPlaybackExpiredRecording2 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestPlaybackExpiredRecording2 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    /**
     * Schedule a recording
     * 
     */
    public class TestRetentionPriority2 extends TestCase
    {
        TestRetentionPriority2(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRetentionPriority2";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            /**
             * Schedule a recording:
             * 
             */

            // Recording 1
            // Start time - 1 sec from 'now' (current system time)
            // duration - 30 sec
            // task trigger time - 500 msec
            // Expire - 20 sec from start time
            // rentention priority set to 'DELETE_AT_EXPIRATION'

            // This is a recording that is in_progress at the time
            // it expires. The recording should be terminated and the recorded
            // service should be deleted.

            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 1000, 30000, 500, 20,
                    OcapRecordingProperties.DELETE_AT_EXPIRATION));

            // Check the recording for proper state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 14000));

            // Display the recordings Recording should be ongoing
            m_eventScheduler.scheduleCommand(new PrintRecordings(16000));

            // Check for deletion after expiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.DELETED_STATE, 30000));

            m_eventScheduler.run(20000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestRetentionPriority2 completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestRetentionPriority2 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestRetentionPriority2 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }

        private OcapLocator m_locator;
    }

    /**
         *  
         * 
         */
    public class TestRetentionPriority3 extends TestCase
    {
        TestRetentionPriority3(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRetentionPriority3";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            /**
             * Schedule a recording:
             * 
             */
            // Recording 1
            // Start time - 1 sec from 'now' (current system time)
            // duration - 30 sec
            // task trigger time - 500 msec
            // Expire - 20 sec from start time
            // rentention priority set to 1 (should be added to purgable list)

            // This is a recording that is in 'in_progress' state at the time
            // it expires. Recording should be terminated?
            // Since the retention priority is NOT set to
            // delete_at_expiration the recording gets added as purgable
            // to retention manager.

            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 1000, 30000, 500, 20, 1));

            // Check the recording for proper state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 15000));

            // Display the recordings Recording should be ongoing
            m_eventScheduler.scheduleCommand(new PrintRecordings(16000));

            // Check for deletion after expiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 35000));

            m_eventScheduler.run(20000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestRetentionPriority3 completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestRetentionPriority3 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestRetentionPriority3 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }

        private OcapLocator m_locator;
    }

    /**
         *  
         * 
         */
    public class TestRetentionPriority4 extends TestCase
    {
        TestRetentionPriority4(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRetentionPriority4";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            /**
             * Schedule a recording:
             * 
             */
            // Recording 1
            // Start time - 1 sec from 'now' (current system time)
            // duration - 30 sec
            // task trigger time - 500 msec
            // Expire - 40 sec from start time
            // rentention priority set to 2 (should be added to purgable list)

            // This is a recording that is in 'completed' state at the time
            // it expires. It should be added to rentention manager as purgable.

            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 1000, 30000, 500, 40, 2));

            // Check the recording for proper state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 15000));

            // Check the recording for proper state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 45000));

            // Display the recordings
            m_eventScheduler.scheduleCommand(new PrintRecordings(50000));

            m_eventScheduler.run(20000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestRetentionPriority4 completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestRetentionPriority4 completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestRetentionPriority4 completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }

        private OcapLocator m_locator;
    }

    /**
     * Schedule a very long recording
     * 
     */
    public class TestScheduleRealLongRecording extends TestCase
    {
        TestScheduleRealLongRecording(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestScheduleRealLongRecording";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            long space = getAvailableDiskSpace();
            // Calculate recording length needed to fill, space, the remaining
            // disk space.
            long duration1 = ((space * 3L) / (HD_BITS_PER_MILLISEC / 8L)) / 4;

            /**
             * Schedule really long recording:
             * 
             */

            // Recording 1
            // Start time - 1 sec from 'now' (current system time)
            // duration - (3 * available space (this should basically force
            // purging
            // of any expired recordings)
            // task trigger time - 500 msec
            // Expire - 3 hours from start time
            // rentention priority set to 'DELETE_AT_EXPIRATION'

            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 1000, duration1, 500,
                    3 * 60 * 60 * 1000, OcapRecordingProperties.DELETE_AT_EXPIRATION));

            // Check the recording for proper state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 10000));

            // Display the recordings Recording should be ongoing
            m_eventScheduler.scheduleCommand(new PrintRecordings(16000));

            // Check the recording for proper state
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.DELETED_STATE, duration1 + 5000));

            // wait additional 20 sec
            m_eventScheduler.run(20000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestScheduleRealLongRecording completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestScheduleRealLongRecording completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestScheduleRealLongRecording completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }

        long getAvailableDiskSpace()
        {
            StorageProxy[] sproxy = StorageManager.getInstance().getStorageProxies();

            for (int i = 0; i < sproxy.length; i++)
            {
                LogicalStorageVolume[] lsv = sproxy[i].getVolumes();

                for (int j = 0; j < lsv.length; j++)
                {
                    if (lsv[j] instanceof MediaStorageVolume)
                    {
                        return ((MediaStorageVolume) (lsv[j])).getFreeSpace();
                    }
                }
            }
            return 0;
        }

        private final long HD_BITS_PER_SEC = 19500000L; // 19500Kbps

        private final long HD_BITS_PER_MILLISEC = 19500L; // 19500Kbp

        private OcapLocator m_locator;
    }

    /**
     * Create new TSB
     * 
     */

    public class TestPurgeDuringPlayback extends TestCase
    {
        TestPurgeDuringPlayback(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestPurgeDuringPlayback";
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
            // Recording
            // Start time - 1 sec from 'now' (current system time)
            // duration - 1 mins
            // task trigger time - 500 msec
            // Expire - 60 sec from start time
            // retention priority is NOT delete_at_expiration (set it to 1)
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 5000, 120000, 500, 121, 1));

            // schedule a second recording
            // this one should be schedule to start while the first is being
            // played back.
            // the first will be playing back from "roughly" 90 secs in test to
            // 90 secs + 60 secs (150)
            // so schedule this to start at approx 120 secs
            // this one should be "huge" so as to cause the first to attempt to
            // purge
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 210000, 1000L * 60L * 60L * 24L
                    * 28L, // 28 day long recording!
                    500, 1000L * 60L * 60L * 24L * 28L, // 2 month expiration
                    1));

            // Schedule play call
            // In the middle of playback, the recording expires. Since the
            // retention priority is
            // NOT set to delete_at_expiration, playback should continue
            m_eventScheduler.scheduleCommand(new SelectRecordedServiceNoDestroy("Recording1", 150000));

            m_eventScheduler.scheduleCommand(new VerifyNotDeleted("Recording1", 230000));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 240000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 240000));

            m_eventScheduler.run(15000); // wait ~45 secs for recording to play
                                         // through

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestPurgeDuringPlayback completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestPurgeDuringPlayback completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestPurgeDuringPlayback completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestPurgeExpired extends TestCase
    {
        TestPurgeExpired(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestPurgeExpired";
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
            // Recording
            // Start time - 1 sec from 'now' (current system time)
            // duration - 1 mins
            // task trigger time - 500 msec
            // Expire - 60 sec from start time
            // retention priority is NOT delete_at_expiration (set it to 1)
            // All three recordings should be purged when the "big" recording is
            // started
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 5000, 30000, 500, 60, 1));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 5000, 30000, 500, 60, 1));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 60000, 30000, 500, 60, 1));

            // schedule a second recording
            // this one should be schedule to start while the first is being
            // played back.
            // the first will be playing back from "roughly" 90 secs in test to
            // 90 secs + 60 secs (150)
            // so schedule this to start at approx 150 secs
            // this one should be "huge" so as to cause the first to attempt to
            // purge
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + 150000, 1000L * 60L * 60L * 24L
                    * 28L, // 28 day long recording!
                    500, 1000L * 60L * 60L * 24L * 28L, // 2 month expiration
                    1));

            m_eventScheduler.scheduleCommand(new VerifyDeleted("Recording1", 200000));
            m_eventScheduler.scheduleCommand(new VerifyDeleted("Recording2", 200000));
            m_eventScheduler.scheduleCommand(new VerifyDeleted("Recording3", 200000));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 220000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 220000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 220000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording4", 220000));

            m_eventScheduler.run(15000); // wait ~45 secs for recording to play
                                         // through

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestPurgeExpired completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestPurgeExpired completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestPurgeExpired completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    public class TestPurgeOnExpiration extends TestCase
    {
        TestPurgeOnExpiration(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestPurgeOnExpiration";
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
            // Recording
            // Start time - 1 sec from 'now' (current system time)
            // duration - 1 mins
            // task trigger time - 500 msec
            // Expire - 60 sec from start time
            // retention priority is NOT delete_at_expiration (set it to 1)
            // All three recordings should be purged when the "big" recording is
            // started
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 5000, 30000, 500, 180, 1));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 5000, 30000, 500, 180, 1));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 60000, 30000, 500, 130, 1));

            // schedule a second recording
            // this one should be schedule to start while the first is being
            // played back.
            // the first will be playing back from "roughly" 90 secs in test to
            // 90 secs + 60 secs (150)
            // so schedule this to start at approx 150 secs
            // this one should be "huge" so as to cause the first to attempt to
            // purge
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + 150000, 1000L * 60L * 60L * 24L
                    * 28L, // 28 day long recording!
                    500, 1000L * 60L * 60L * 24L * 28L, // 2 month expiration
                    1));

            m_eventScheduler.scheduleCommand(new VerifyDeleted("Recording1", 220000));
            m_eventScheduler.scheduleCommand(new VerifyDeleted("Recording2", 220000));
            m_eventScheduler.scheduleCommand(new VerifyDeleted("Recording3", 220000));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 240000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 240000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 240000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording4", 240000));

            m_eventScheduler.run(15000); // wait ~45 secs for recording to play
                                         // through

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestPurgeOnExpiration completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestPurgeOnExpiration completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestPurgeOnExpiration completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    class VerifyNotDeleted extends EventScheduler.NotifyShell
    {
        VerifyNotDeleted(String recording, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recording = recording;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<VerifyNotDeleted>>>>");
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recording);

            if (rr == null)
            {
                System.out.println("DVRUtils: VerifyNotDeleted- recording not found: " + m_recording);
                return;
            }

            if (rr.getState() == org.ocap.shared.dvr.LeafRecordingRequest.DELETED_STATE)
            {
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in VerifyNotDeleted! (deleted recording found)");
                m_failed = TEST_FAILED;
            }
            else
            {
                DVRTestRunnerXlet.log("DvrTest: Recording is not deleted, as expected. Pass.");
            }
        }

        private String m_recording;
    }

    class VerifyDeleted extends EventScheduler.NotifyShell
    {
        VerifyDeleted(String recording, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_recording = recording;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<VerifyDeleted>>>>");
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recording);

            if (rr == null)
            {
                System.out.println("DVRUtils: VerifyDeleted- recording not found: " + m_recording);
                return;
            }

            if (rr.getState() != org.ocap.shared.dvr.LeafRecordingRequest.DELETED_STATE)
            {
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in VerifyDeleted! (non deleted recording found)");
                m_failed = TEST_FAILED;
            }
            else
            {
                DVRTestRunnerXlet.log("DvrTest: Recording is deleted, as expected. Pass.");
            }
        }

        private String m_recording;
    }

    class SelectRecordedServiceNoDestroy extends EventScheduler.NotifyShell implements ServiceContextListener
    {
        SelectRecordedServiceNoDestroy(String recording, long triggerTime)
        {
            super(triggerTime);
            m_recording = recording;
            m_rr = null;
        }

        SelectRecordedServiceNoDestroy(OcapRecordingRequest rr, long triggerTime)
        {
            super(triggerTime);
            m_recording = null;
            m_rr = rr;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<SelectRecordedService::ProcessCommand>>>>");

            ServiceContextFactory scf = ServiceContextFactory.getInstance();
            try
            {
                serviceContext = scf.createServiceContext();
            }
            catch (Exception e)
            {
                System.out.println("SelectRecordedService - createServiceContext failed!");
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in SelectRecordedService due to sc.createServiceContext() exception: "
                        + e.toString());
                e.printStackTrace();
                return;
            }
            serviceContext.addListener(this);
            OcapRecordingRequest rr;

            // were we given a recording list entry, or do we look it up?
            if (m_rr == null)
                rr = (OcapRecordingRequest) findObject(m_recording);
            else
                rr = m_rr;

            if (rr == null)
            {
                System.out.println("SelectRecordedService - entry not found!" + m_recording);
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in SelectRecordedService due to unfound recording: "
                        + m_recording);
                return;
            }

            OcapRecordedService rsvc = null;
            try
            {
                rsvc = (OcapRecordedService) rr.getService();
            }
            catch (Exception e)
            {
                System.out.println("SelectRecordedService - Exception obtaining service." + m_recording);
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in SelectRecordingService due to rr.getService() exception: "
                        + e.toString());
                e.printStackTrace();
            }
            if (rsvc == null)
            {
                System.out.println("SelectRecordedService - Service not found!" + m_recording);
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in SelectRecordingService due to unfound service for: "
                        + m_recording);
                return;
            }

            DvrEventPrinter.printRecording(rr);
            System.out.println("Selecting Recorded Service\n");
            serviceContext.select(rsvc);
        }

        public void receiveServiceContextEvent(ServiceContextEvent ev)
        {
            System.out.println("SelectRecordedService: receiveServiceContextEvent" + ev);
        }

        private String m_recording;

        private OcapRecordingRequest m_rr;

        private ServiceContext serviceContext;

    }
}
