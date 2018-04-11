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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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
 * Created on Oct 2, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.net.OcapLocator;

import org.cablelabs.xlet.DvrTest.DvrTest.AddAppData;
import org.cablelabs.xlet.DvrTest.DvrTest.AddEpisode;
import org.cablelabs.xlet.DvrTest.DvrTest.AddSeason;
import org.cablelabs.xlet.DvrTest.DvrTest.ConfirmRecordingReq_CheckState;
import org.cablelabs.xlet.DvrTest.DvrTest.NewSeriesRoot;
import org.cablelabs.xlet.DvrTest.DvrTest.Record;
import org.cablelabs.xlet.DvrTest.DvrTest.RegisterResourceContentionHandler;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class SegmentedRec_RecordWithConflict extends DvrTest
{

    /**
     * @author Ryan Scheduling a basic recording conflict
     * 
     *        1 Set up alphanumeric Contention Handler 2 Schedule 3 recordings
     *         that all have identical start times with names
     *         “Rec1”,”Rec2”,”Rec3” and distinct locators Recordings shall be in
     *         the following states repectively: PENDING NO CONFLICT, PENDING NO
     *         CONFLICT, PENDING WITH CONFLICT 3 Shortly after start time,
     *         Verify Rec3 is IN_PROGRESS_WITH_ERROR 4 Verifed if
     *         RecordingFailed Exception is INSUFFICIENT_RESOURCES 5 Request
     *         shall finish in FAILED_STATE after stop timer has 6 Deschedule
     *         Contention Handler
     * 
     * 
     */
    public class TestCase1 extends TestCase
    {

        private OcapLocator m_locator;

        /**
         * Constructor
         */
        TestCase1(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "SegmentedRec_RecordWithConflict : Schedule Basic Recording Conflict";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 0;
            long expire_time = 24 * 60 * 60 * 1000;
            // Clear event scheduler
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000 + delay, 60000, 1000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 30000 + delay, 60000, 2000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 30000 + delay, 50000, 3000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            // Verify states prior to recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 20020 + delay));

            // Shortly after start time, Verify Rec3 is
            // IN_PROGRESS_WITH_ERROR_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 60000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 60010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 60020 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 70000 + delay));

            // Request shall finish in FAILED_STATE after stop timer has elapsed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.FAILED_STATE, 110000 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 115000 + delay));

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 120000 + delay));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }

        /**
         * @param m_failed
         * @param name
         */

    }

    /*
     * 
     * @author Ryan
     * 
     * Scheduling a basic recording conflict with deletion
     * 
     * 1 Set up alphanumeric Contention Handler2 Schedule 3 recordings that all
     * have identical start times with names “Rec1”,”Rec2”,”Rec3” and distinct
     * locators Recordings shall be in the following states repectively: PENDING
     * NO CONFLICT, PENDING NO CONFLICT, PENDING WITH CONFLICT3 Shortly after
     * start time, Verify Rec3 is IN_PROGRESS_WITH_ERROR4 Delete “Rec2”5 “Rec 3”
     * Request shall move to the IN_PROGRESS_INCOMPLETE6 Verifed if
     * RecordingFailed Exception is INSUFFICIENT_RESOURCES7 Request shall finish
     * in INCOMPLETE_STATE8 Determine if the recording is an instance of
     * SegmentedRecordedeService, that there is one RecordedService in the
     * array, and play back9 Deschedule Contention Handler
     */
    public class TestCase2and8 extends TestCase
    {

        private OcapLocator m_locator;

        private boolean m_long_rec;

        /**
         * Constructor
         */
        TestCase2and8(OcapLocator locator, boolean long_rec)
        {
            m_locator = locator;
            m_long_rec = long_rec;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "SegmentedRec_RecordWithConflict : "
                    + (m_long_rec ? "Long recording that loses contention but never starts"
                            : "Schedule basic recording conflict with deletion");
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 0;
            long expire_time = 5 * 24 * 60 * 60 * 1000;
            // Clear event scheduler
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000 + delay, 90000, 1000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 30000 + delay, 90000, 2000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            if (!m_long_rec)
            {
                m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 30000 + delay, 110000, 3000,
                        expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 30000 + delay,
                        (4 * 24 * 60 * 60 * 1000), 3000, expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            }

            // Verify states prior to recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 20020 + delay));

            // Shortly after start time, Verify Rec3 is
            // IN_PROGRESS_WITH_ERROR_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 60000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 60010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 60020 + delay));

            // Delete the ongoing recording, Recording 2
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 70000 + delay));

            if (!m_long_rec)
            {
                // Verified if RecordingFailed Exception is
                // INSUFFICIENT_RESOURCES
                m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                        RecordingFailedException.INSUFFICIENT_RESOURCES, 155000 + delay));
                // Check Recording 3 and seee if it is in
                // IN_PROGRESS_INCOMPLETE_STATE
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                        OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 115000 + delay));
                // Request shall finish in INCOMPLETE_STATE after stop timer has
                // elapsed
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                        OcapRecordingRequest.INCOMPLETE_STATE, 150000 + delay));
                // Verifed if RecordingFailed Exception is
                // INSUFFICIENT_RESOURCES
                m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                        RecordingFailedException.INSUFFICIENT_RESOURCES, 155000 + delay));
            }
            else
            {
                // Check Recording 3 and seee if it is in
                // IN_PROGRESS_INCOMPLETE_STATE
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                        OcapRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE, 115000 + delay));
            }

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 160000 + delay));

            // Verify the number of recorded services generated
            // m_eventScheduler.scheduleCommand(new
            // CheckRecordedServices("Recording3",true,1,170000));

            // Delete the Recording Requests
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 170000 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 170000 + delay));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * TODO To change the template for this generated type comment go to Window
     * - Preferences - Java - Code Style - Code Templates
     */
    public class TestCase4 extends TestCase
    {

        private OcapLocator m_locator;

        /**
         * Constructor
         */
        TestCase4(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "SegmentedRec_RecordWithConflict : Schedule a recording conflict with a reschedule";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 0;
            long expire_time = 24 * 60 * 60 * 1000;
            // Clear event scheduler
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000 + delay, 150000, 1000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 350000 + delay, 150000, 2000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 30000 + delay, 200000, 3000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            // Verify states prior to recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20020 + delay));

            // Shortly after start time, Verify Rec3 is
            // IN_PROGRESS_WITH_ERROR_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 60000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 60010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 60020 + delay));

            // Reschedule recording to some time in the past
            m_eventScheduler.scheduleCommand(new Reschedule("Recording2", now + 30000, 140000, 90000 + delay));

            // After reschedule, verify recording states
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 150000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 150010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 150020 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 155000 + delay));

            // Stop Recording 2
            m_eventScheduler.scheduleCommand(new StopRecording("Recording1", 157000));

            // Verify the resarting of Recording 3
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.INCOMPLETE_STATE, 220000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.INCOMPLETE_STATE, 221000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 222000 + delay));

            // Request shall finish in INCOMPLETE_STATE after stop timer has
            // elapsed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.INCOMPLETE_STATE, 240000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.INCOMPLETE_STATE, 241000 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 242000 + delay));

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 243000 + delay));

            // Verify the number of recorded services generated
            m_eventScheduler.scheduleCommand(new CheckRecordedServices("Recording3", true, 2, 245000));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * TODO To change the template for this generated type comment go to Window
     * - Preferences - Java - Code Style - Code Templates
     */
    public class TestCase5 extends TestCase
    {

        private OcapLocator m_locator;

        /**
         * Constructor
         */
        TestCase5(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "SegmentedRec_RecordWithConflict : Schedule larger recording confilct with double deletion";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 0;
            long expire_time = 24 * 60 * 60 * 1000;
            // Clear event scheduler
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000 + delay, 360000, 1000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 30000 + delay, 360000, 2000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 30000 + delay, 360000, 3000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + 30000 + delay, 360000, 4000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            // Shortly after start time, Verify Rec3 is
            // IN_PROGRESS_WITH_ERROR_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 90000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 90010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 90020 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 90030 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            // for both recordings
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 100000 + delay));
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording4",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 100100 + delay));

            // Delete the ongoing recording, Recording 1 and Recording 2
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 135000 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 136000 + delay));

            // Lower priority requests will start up and record
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 350000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 351000 + delay));

            // Request shall finish in INCOMPLETE_STATE after stop timer has
            // elapsed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.INCOMPLETE_STATE, 401000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.INCOMPLETE_STATE, 402000 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 411000 + delay));
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording4",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 412000 + delay));

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 433000 + delay));

            // Verify the number of recorded services generated
            m_eventScheduler.scheduleCommand(new CheckRecordedServices("Recording3", true, 1, 435000));

            // Verify the number of recorded services generated
            m_eventScheduler.scheduleCommand(new CheckRecordedServices("Recording4", true, 1, 436000));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * TODO To change the template for this generated type comment go to Window
     * - Preferences - Java - Code Style - Code Templates
     */
    public class TestCase6 extends TestCase
    {

        private OcapLocator m_locator;

        /**
         * Constructor
         */
        TestCase6(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "SegmentedRec_RecordWithConflict : Schedule basic series recording conflict with deletion";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 0;
            long expiration = 24 * 60 * 60 * 1000;
            // Clear event scheduler
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // inherited down to the leaf.
            OcapRecordingProperties orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE,
                    expiration, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                    OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, null, null, null);

            // Create the 1st root recording request.
            m_eventScheduler.scheduleCommand(new NewSeriesRoot("Show3", // name
                    new PrivateRecordingSpec("History Channel Series", orp), 1000));

            // Create the child recording request off the 1st root
            m_eventScheduler.scheduleCommand(new AddSeason("Season3", // name
                    "Show3", // parent's name
                    new PrivateRecordingSpec("Season 1", orp), ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 2000));

            // Create the leaf recording request for this parent
            m_eventScheduler.scheduleCommand(new AddEpisode(
                    "Recording3", // name
                    "Season3", // parent's name
                    m_locator,
                    now + 70000, // start time
                    150000, // duration
                    ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 3000, expiration,
                    OcapRecordingProperties.RECORD_WITH_CONFLICTS, OcapRecordingProperties.DELETE_AT_EXPIRATION));

            // Create the 2nd root recording request.
            m_eventScheduler.scheduleCommand(new NewSeriesRoot("Show1", // name
                    new PrivateRecordingSpec("Family Guy", orp), 6000));

            // Create the child recording request (Season 1) for the second root
            m_eventScheduler.scheduleCommand(new AddSeason("Season1", // name
                    "Show1", // parent's name
                    new PrivateRecordingSpec("Season 1", orp), ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 7000));

            // Create the leaf recording request under the 2nd root
            m_eventScheduler.scheduleCommand(new AddEpisode(
                    "Recording1", // name
                    "Season1", // parent's name
                    m_locator,
                    now + 70000, // start time
                    150000, // duration
                    ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 8000, expiration,
                    OcapRecordingProperties.RECORD_WITH_CONFLICTS, OcapRecordingProperties.DELETE_AT_EXPIRATION));

            // Create the 3rd root recording request
            m_eventScheduler.scheduleCommand(new NewSeriesRoot("Show2", // name
                    new PrivateRecordingSpec("Aqua Teen Hunger Force", orp), 11000));

            // Create the child recording request (Season 1) for the third root
            m_eventScheduler.scheduleCommand(new AddSeason("Season2", // name
                    "Show2", // parent's name
                    new PrivateRecordingSpec("Season 1", orp), ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 12000));

            // Create the leaf recording request
            m_eventScheduler.scheduleCommand(new AddEpisode(
                    "Recording2", // name
                    "Season2", // parent's name
                    m_locator,
                    now + 70000, // start time
                    150000, // duration
                    ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 13000, expiration,
                    OcapRecordingProperties.RECORD_WITH_CONFLICTS, OcapRecordingProperties.DELETE_AT_EXPIRATION));

            // Verify states prior to recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 60000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 60010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 60020 + delay));

            // Shortly after start time, Verify Rec3 is
            // IN_PROGRESS_WITH_ERROR_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 130000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 130010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 130020 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 140000 + delay));

            // Delete the ongoing recording, Recording 2
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Show2", 150000 + delay));

            // Check Recording 3 and seee if it is in
            // IN_PROGRESS_INCOMPLETE_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 210000 + delay));

            // Request shall finish in INCOMPLETE_STATE after stop timer has
            // elapsed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.INCOMPLETE_STATE, 240000 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 245000 + delay));

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 250000 + delay));

            // Delete the other shows
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Show1", 255000 + delay));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Show3", 260000 + delay));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * TODO To change the template for this generated type comment go to Window
     * - Preferences - Java - Code Style - Code Templates
     */
    public class TestCase7 extends TestCase
    {

        private OcapLocator m_locator;

        /**
         * Constructor
         */
        TestCase7(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "SegmentedRec_RecordWithConflict : Contruct a long recording that will lose contention over time";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 0;
            long expire_time = 24 * 60 * 60 * 1000;
            // Clear event scheduler
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 90000 + delay, 60000, 1000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 90000 + delay, 60000, 2000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 210000 + delay, 60000, 3000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + 210000 + delay, 60000, 4000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, now + 30000 + delay, 300000, 5000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            // Verify states prior to recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20100 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 20400 + delay));

            // Shortly after start time, Verify Rec5 is IN_PROGRESS_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55100 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 55300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 55400 + delay));

            // Shortly after start time, Verify Rec5 is IN_PROGRESS_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 140000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 140100 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 140200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 140300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 140400 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording5",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 140000 + delay));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 200000 + delay));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 250200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 250300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 250400 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording5",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 280000 + delay));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 315000 + delay));

            // Request shall finish in INCOMPLETE_STATE after stop timer has
            // elapsed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.INCOMPLETE_STATE, 340000 + delay));

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 345000 + delay));

            // Verify the number of recorded services generated
            m_eventScheduler.scheduleCommand(new CheckRecordedServices("Recording5", true, 3, 350000));

            // Add AppData to find recorded service
            m_eventScheduler.scheduleCommand(new AddAppData("Recording5", "segmented_playback", "Recording1", 352000));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }
    }

    /*
     * 
     * @author Ryan
     * 
     * Consrtuct a long recording that loses contention with other recordings
     * but starts with other resource usages
     * 
     * 1 Setup Contention Handler to put recording resource usages at the bottom
     * of the list 2 Start 2 recordings: “Rec1” and “Rec2”. “Rec1” will have
     * higher priority over “Rec2” 3 Verify recordings are in INPROGRESS_STATE 4
     * Initiate a Service Context and have it tune to a service other than the
     * services recorded 5 Verify that “Rec 2” is now in INPROGRESS_ERROR with a
     * RecordingFailed Exception that has reason code INSUFFICIENT_RESOURCES 6
     * Verify “Rec1” is still IN_PROGRESS 7 Destroy Service Context 8 Verify
     * that “Rec 2” is now in IN_PROGRESS_INCOMPLETE 9 Verify “Rec1” is still
     * IN_PROGRESS_STATE 10 Reserve the Network Interface 11 Verify that “Rec 2”
     * is now in IN_PROGRESS_WITH_ERROR with a RecordingFailed Exception that
     * has reason code INSUFFICIENT_RESOURCES 12 Verify “Rec1” is still
     * INPROGRESS 13 Release the Network Interface 14 Verify that “Rec 2” is now
     * in INPROGRESS_INCOMPLETE 15 Verify “Rec1” is still INPROGRESS_STATE
     */
    public class TestCase10 extends TestCase
    {

        private OcapLocator m_locator;

        private OcapLocator m_locator2;

        /**
         * Constructor
         */
        TestCase10(OcapLocator locator, OcapLocator locator2)
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
            return "SegmentedRec_RecordWithConflict : Consrtuct a long recording that loses contention with other resources but starts";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 0;
            long expire_time = 24 * 60 * 60 * 1000;
            // Clear event scheduler
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("1Recording", m_locator, now + 30000 + delay, 360000, 1000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            m_eventScheduler.scheduleCommand(new Record("3Recording", m_locator, now + 30000 + delay, 360000, 2000,
                    expire_time, 0, OcapRecordingProperties.RECORD_WITH_CONFLICTS));

            // Verify states prior to recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("1Recording",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("3Recording",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 20100 + delay));

            // Shortly after start time, Verify recordings are in
            // IN_PROGRESS_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("1Recording",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 55000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("3Recording",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 55100 + delay));

            // Initialize a Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext("2SC", 60000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService("2SC", m_locator2, 61000));

            // Check recording states - 3Recording should be not in progress
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("1Recording",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 120300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("3Recording",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 120400 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("3Recording",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 130000 + delay));

            // Stop and destroy the Service Context
            m_eventScheduler.scheduleCommand(new StopService("2SC", 140000));
            m_eventScheduler.scheduleCommand(new DestroyService("2SC", 145000));

            // Verify both recordings are back in progess once resources have
            // been removed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("1Recording",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 200200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("3Recording",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 202000 + delay));

            // Reserve the network interface
            m_eventScheduler.scheduleCommand(new TuneNetworkInterface("2NI", m_locator2, 210000));

            // Check recording states - 3Recording should be not in progress
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("1Recording",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 270300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("3Recording",
                    OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 270400 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("3Recording",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 281000 + delay));

            // Release the Network interface
            m_eventScheduler.scheduleCommand(new ReleaseNetworkInterface("2NI", false /*
                                                                                       * ignore
                                                                                       * failure
                                                                                       */, 290000 + delay));

            // Verify both recordings go back into in progress
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("1Recording",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 370200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("3Recording",
                    OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 370000 + delay));

            // 3Recording shall finish in INCOMPLETE_STATE and 1Recording will
            // be in COMPLETED_STATE after stop timer has elapsed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("1Recording",
                    OcapRecordingRequest.COMPLETED_STATE, 410000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("3Recording",
                    OcapRecordingRequest.INCOMPLETE_STATE, 410000 + delay));

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 420000 + delay));

            // Verify the number of recorded services generated
            m_eventScheduler.scheduleCommand(new CheckRecordedServices("3Recording", true, 3, 425000));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }
    }

    /**
     * @param locators
     */
    SegmentedRec_RecordWithConflict(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestCase1((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestCase2and8((OcapLocator) m_locators.elementAt(0), false));
        tests.addElement(new TestCase4((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestCase5((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestCase6((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestCase7((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestCase2and8((OcapLocator) m_locators.elementAt(0), true));
        tests.addElement(new TestCase10((OcapLocator) m_locators.elementAt(0), (OcapLocator) m_locators.elementAt(1)));
        return tests;
    }

}
