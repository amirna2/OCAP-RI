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

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.net.OcapLocator;

public class TestRescheduleExpiration extends DvrTest
{

    TestRescheduleExpiration(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestRescheduleExpiration1((OcapLocator) m_locators.elementAt(0),
                (OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * Perform a collection of scheduled recordings, and confirm their
     * successful completion
     * 
     * @param locators
     */
    public class TestRescheduleExpiration1 extends TestCase
    {
        TestRescheduleExpiration1(OcapLocator locator1, OcapLocator locator2)
        {
            m_locator1 = locator1;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test reschedule past expiration";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // 30 second recording w/ 60 second expiration
            m_eventScheduler.scheduleCommand(new RecordWithRetentionPriority("Recording1", m_locator1, now + 10000,
                    30 * 1000, 1000, 60, 1));// use 1 as non-purge expiration
                                             // priority
            m_eventScheduler.scheduleCommand(new VerifyPriority("Recording1", 1, 2000));

            // Change the retention priority
            m_eventScheduler.scheduleCommand(new ReschedulePriority("Recording1", 2, 3000));
            m_eventScheduler.scheduleCommand(new VerifyPriority("Recording1", 2, 4000));

            // wait for expiration (at 60 seconds), then reschedule expiration
            m_eventScheduler.scheduleCommand(new RescheduleExpiration("Recording1", 180, 90000));

            // Change the retention priority to 0
            m_eventScheduler.scheduleCommand(new ReschedulePriority("Recording1",
                    OcapRecordingProperties.DELETE_AT_EXPIRATION, 91000));
            m_eventScheduler.scheduleCommand(new VerifyPriority("Recording1",
                    OcapRecordingProperties.DELETE_AT_EXPIRATION, 93000));

            // Check for status prior epiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 170000));

            // Check for deletion after expiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.DELETED_STATE, 200000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("Test reschedule past expiration  completed: FAILED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("Test reschedule past expiration completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }

        private OcapLocator m_locator1;
    }

    class RecordWithRetentionPriority extends EventScheduler.NotifyShell
    {
        RecordWithRetentionPriority(String recordingName, OcapLocator source, long startTime, long duration,
                long taskTriggerTime, long expiration, int priority)
        {
            super(taskTriggerTime);
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = expiration;
            m_recordingName = recordingName;
            m_priority = priority;
        }

        public void ProcessCommand()
        {
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();

            try
            {
                OcapRecordingRequest rr;
                LocatorRecordingSpec lrs;
                OcapRecordingProperties orp;
                System.out.println("DVRUtils: issuing recording:" + m_source[0] + " StartTime:" + m_startTime
                        + " Duration:" + m_duration);

                orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, m_expiration, m_priority,
                        OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, null, null, null);
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
                System.out.println("DVRUtils: Record: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Record due to rm.record() xception: " + e.toString());
            }
        }

        private OcapLocator m_source[];

        private long m_startTime;

        private long m_duration;

        private long m_expiration;

        private String m_recordingName;

        private int m_priority;
    }

    /**
     * Schedules a future call to reschedule a recording At the task's trigger
     * time, a call to RecordingRequest.reschedule will be made with the
     * parameters specified.
     */
    class RescheduleExpiration extends EventScheduler.NotifyShell
    {
        /**
         * Schedule a recording request to be rescheduled.
         * 
         * @param recordingName
         *            DvrTest recording name
         * @param newExpiration
         *            New expiration period
         * @param taskTriggerTime
         *            Time offset for scheduling the reschedule.
         */
        RescheduleExpiration(String recordingName, long newExpiration, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_expiration = newExpiration;
            m_recordingName = recordingName;
        }

        public void ProcessCommand()
        {
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recordingName);

            if (rr == null)
            {
                System.out.println("DVRUtils: RescheduleExpiration - recording not found: " + m_recordingName);
                return;
            }

            try
            {
                LocatorRecordingSpec oldLrs, newLrs;
                oldLrs = (LocatorRecordingSpec) (rr.getRecordingSpec());

                OcapRecordingProperties oldOrp = (OcapRecordingProperties) oldLrs.getProperties();
                OcapRecordingProperties newOrp = new OcapRecordingProperties(oldOrp.getBitRate(), m_expiration,
                        oldOrp.getRetentionPriority(), oldOrp.getPriorityFlag(), oldOrp.getAccessPermissions(),
                        oldOrp.getOrganization(), oldOrp.getDestination());

                System.out.println("DVRUtils: issuing reschedule for " + m_recordingName + ": " + " Expiration: "
                        + m_expiration);

                newLrs = new LocatorRecordingSpec(oldLrs.getSource(), oldLrs.getStartTime(), oldLrs.getDuration(),
                        newOrp);

                rr.reschedule(newLrs);
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Reschedule: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Reschedule due to exception: " + e.toString());
            }
        }

        private String m_recordingName;

        private long m_expiration;
    } // END class RescheduleExpiration

    /**
     * Schedules a future call to reschedule a recording At the task's trigger
     * time, a call to RecordingRequest.reschedule will be made with the
     * parameters specified.
     */
    class ReschedulePriority extends EventScheduler.NotifyShell
    {
        /**
         * Schedule a recording request to be rescheduled.
         * 
         * @param recordingName
         *            DvrTest recording name
         * @param newExpiration
         *            New expiration period
         * @param taskTriggerTime
         *            Time offset for scheduling the reschedule.
         */
        ReschedulePriority(String recordingName, int newPriority, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_priority = newPriority;
            m_recordingName = recordingName;
        }

        public void ProcessCommand()
        {
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recordingName);

            if (rr == null)
            {
                System.out.println("DVRUtils: ReschedulePriority - recording not found: " + m_recordingName);
                return;
            }

            try
            {
                LocatorRecordingSpec oldLrs, newLrs;
                oldLrs = (LocatorRecordingSpec) (rr.getRecordingSpec());

                OcapRecordingProperties oldOrp = (OcapRecordingProperties) oldLrs.getProperties();
                OcapRecordingProperties newOrp = new OcapRecordingProperties(oldOrp.getBitRate(),
                        oldOrp.getExpirationPeriod(), m_priority, oldOrp.getPriorityFlag(),
                        oldOrp.getAccessPermissions(), oldOrp.getOrganization(), oldOrp.getDestination());

                System.out.println("DVRUtils: issuing reschedule for " + m_recordingName + ": " + " RetPriority: "
                        + m_priority);

                newLrs = new LocatorRecordingSpec(oldLrs.getSource(), oldLrs.getStartTime(), oldLrs.getDuration(),
                        newOrp);

                rr.reschedule(newLrs);
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Reschedule: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Reschedule due to exception: " + e.toString());
            }
        }

        private String m_recordingName;

        private int m_priority;
    } // END class

    /**
     * Schedules a future call to reschedule a recording At the task's trigger
     * time, a call to RecordingRequest.reschedule will be made with the
     * parameters specified.
     */
    class VerifyPriority extends EventScheduler.NotifyShell
    {
        /**
         * Schedule a recording request to be rescheduled.
         * 
         * @param recordingName
         *            DvrTest recording name
         * @param newExpiration
         *            New expiration period
         * @param taskTriggerTime
         *            Time offset for scheduling the reschedule.
         */
        VerifyPriority(String recordingName, int newPriority, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_priority = newPriority;
            m_recordingName = recordingName;
        }

        public void ProcessCommand()
        {
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recordingName);

            if (rr == null)
            {
                System.out.println("DVRUtils: VerifyPriority - recording not found: " + m_recordingName);
                return;
            }

            try
            {
                LocatorRecordingSpec oldLrs;
                oldLrs = (LocatorRecordingSpec) (rr.getRecordingSpec());

                OcapRecordingProperties oldOrp = (OcapRecordingProperties) oldLrs.getProperties();
                if (oldOrp.getRetentionPriority() != m_priority)
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in VerifyPriority. Expected RetPri: " + m_priority
                            + " received: " + oldOrp.getRetentionPriority());
                }
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Reschedule: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Reschedule due to exception: " + e.toString());
            }
        }

        private String m_recordingName;

        private int m_priority;
    } // END class RescheduleExpiration

}
