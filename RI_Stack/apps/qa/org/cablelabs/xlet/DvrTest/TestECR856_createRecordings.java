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
 * Created on Dec 4, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;

/**
 * @author bforan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestECR856_createRecordings extends DvrTest
{

    /**
     * @param locators
     */
    TestECR856_createRecordings(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestECR856_SchedLongRec_RWC((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECR856_SchedShortRec_RWC((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECR856_SchedFutureShortRec_RWC((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestECR856_SchedFutureLongRec_RWC((OcapLocator) m_locators.elementAt(0)));
        // tests.addElement(new TestECR856_SchedLongRec_RINC((OcapLocator)
        // m_locators.elementAt(0)));
        return tests;
    }

    /**
     * @param locator
     */
    public class TestECR856_SchedLongRec_RWC extends TestCase
    {
        private OcapLocator m_locator;

        TestECR856_SchedLongRec_RWC(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestECR856 - ScheduleLongRecording - 1.1, 1.2, 1.6, 1.7";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long expiration = 1000 * 60 * 60 * 24; // 24 hour expiration

            // clear the schedule of pending tasks
            reset();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            String recording = "ECR856_longRecording_rwc";

            m_eventScheduler.scheduleCommand(new Record(recording, m_locator, now, 18000000 /*
                                                                                             * 5
                                                                                             * hour
                                                                                             * dur
                                                                                             */, 500, expiration,
                    OcapRecordingProperties.DELETE_AT_EXPIRATION, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            m_eventScheduler.scheduleCommand(new AddAppData(recording, recording, recording, 510));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording,
                    OcapRecordingRequest.IN_PROGRESS_STATE, 1000));

            m_eventScheduler.run(1000);

            if (m_failed != TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " - Recording Scheduled");
                DVRTestRunnerXlet.log(getName() + " - Turn Off Set-Top Box");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " - ERROR scheduling Recording");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }

        }
    }

    public class TestECR856_SchedLongRec_RINC extends TestCase
    {
        private OcapLocator m_locator;

        TestECR856_SchedLongRec_RINC(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestECR856 - ScheduleLongRecording (RINC)";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long expiration = 1000 * 60 * 60 * 24; // 24 hour expiration

            // clear the schedule of pending tasks
            reset();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            String recording = "ECR856_longRecording_rinc";

            m_eventScheduler.scheduleCommand(new Record(recording, m_locator, now, 18000000 /*
                                                                                             * 5
                                                                                             * hour
                                                                                             * dur
                                                                                             */, 500, expiration,
                    OcapRecordingProperties.DELETE_AT_EXPIRATION, OcapRecordingProperties.RECORD_IF_NO_CONFLICTS));

            m_eventScheduler.scheduleCommand(new AddAppData(recording, recording, recording, 510));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording,
                    OcapRecordingRequest.IN_PROGRESS_STATE, 1000));

            m_eventScheduler.run(1000);

            if (m_failed != TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " - Recording Scheduled");
                DVRTestRunnerXlet.log(getName() + " - Turn Off Set-Top Box");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " - ERROR scheduling Recording");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }
        }
    }

    /**
     * This recording is used for test:
     * SetRecDelayWithinRangeStopBeforeDelayStart
     * 
     * @param locator
     */
    public class TestECR856_SchedShortRec_RWC extends TestCase
    {
        private OcapLocator m_locator;

        TestECR856_SchedShortRec_RWC(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestECR856 - ScheduleShortRecording - 1.3";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long expiration = 1000 * 60 * 60 * 24; // 24 hour expiration

            // clear the schedule of pending tasks
            reset();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            String recording = "ECR856_shortRecording_rwc";

            m_eventScheduler.scheduleCommand(new Record(recording, m_locator, now, 270000 /*
                                                                                           * 4.5
                                                                                           * minute
                                                                                           * dur
                                                                                           */, 500, expiration,
                    OcapRecordingProperties.DELETE_AT_EXPIRATION, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            m_eventScheduler.scheduleCommand(new AddAppData(recording, recording, recording, 510));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording,
                    OcapRecordingRequest.IN_PROGRESS_STATE, 1000));

            m_eventScheduler.run(1000);

            if (m_failed != TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " - Recording Scheduled");
                DVRTestRunnerXlet.log(getName() + " - Turn Off Set-Top Box");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " - ERROR scheduling Recording");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }

        }
    }

    /**
     * This recording is used for test:
     * SetRecDelayWithinRangeStartBeforeDelayStart
     * 
     * @param locator
     */
    public class TestECR856_SchedFutureShortRec_RWC extends TestCase
    {
        private OcapLocator m_locator;

        TestECR856_SchedFutureShortRec_RWC(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestECR856 - ScheduleFutureShortRecording - 1.4";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long expiration = 1000 * 60 * 60 * 24; // 24 hour expiration

            // clear the schedule of pending tasks
            reset();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            String recording = "ECR856_futureShortRecording_rwc";

            m_eventScheduler.scheduleCommand(new Record(recording, m_locator, now + 270000, /*
                                                                                             * start
                                                                                             * 4.5
                                                                                             * min
                                                                                             * from
                                                                                             * now
                                                                                             */
            150000, /* 2.5 min dur */
            500, expiration, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                    OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            m_eventScheduler.scheduleCommand(new AddAppData(recording, recording, recording, 510));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording,
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 5000));

            m_eventScheduler.run(1000);

            if (m_failed != TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " - Recording Scheduled");
                DVRTestRunnerXlet.log(getName() + " - Turn Off Set-Top Box");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " - ERROR scheduling Recording");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }

        }
    }

    /**
     * This recording is used for test:
     * SetRecDelayWithinRangeStartBeforeDelayStart
     * 
     * @param locator
     */
    public class TestECR856_SchedFutureLongRec_RWC extends TestCase
    {
        private OcapLocator m_locator;

        TestECR856_SchedFutureLongRec_RWC(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestECR856 - ScheduleFutureLongRecording - 1.5";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long expiration = 1000 * 60 * 60 * 24; // 24 hour expiration

            // clear the schedule of pending tasks
            reset();

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            String recording = "ECR856_futureLongRecording_rwc";

            m_eventScheduler.scheduleCommand(new Record(recording, m_locator, now + 270000, /*
                                                                                             * start
                                                                                             * 4.5
                                                                                             * min
                                                                                             * from
                                                                                             * now
                                                                                             */
            18000000, /* 5 hour dur */
            500, expiration, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                    OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            m_eventScheduler.scheduleCommand(new AddAppData(recording, recording, recording, 510));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(recording,
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 1000));

            m_eventScheduler.run(1000);

            if (m_failed != TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " - Recording Scheduled");
                DVRTestRunnerXlet.log(getName() + " - Turn Off Set-Top Box");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " - ERROR scheduling Recording");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }

        }
    }

}
