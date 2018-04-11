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
public class SegmentedRec_RecordIfNoConflict extends DvrTest
{

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
     * start time, Verify Rec3 is FAILED_STATE4 Delete “Rec2”5 Verifed if
     * RecordingFailed Exception is INSUFFICIENT_RESOURCES6 Request shall finish
     * in FAILED_STATE7 Deschedule Contention Handler
     */
    public class TestCase2 extends TestCase
    {

        private OcapLocator m_locator;

        /**
         * Constructor
         */
        TestCase2(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "SegmentedRec_RecordIfNoConflict : Schedule basic recording conflict with deletion";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 0;
            // Clear event scheduler
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000 + delay, 90000, 1000));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 30000 + delay, 90000, 2000));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 30000 + delay, 90000, 3000));

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
                    OcapRecordingRequest.IN_PROGRESS_STATE, 40000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 40010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.FAILED_STATE, 40020 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 50000 + delay));

            // Delete the ongoing recording, Recording 2
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 60000 + delay));

            // Check Recording 3 and seee if it is in
            // IN_PROGRESS_INCOMPLETE_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.FAILED_STATE, 80000 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 140000 + delay));

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 150000 + delay));

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
     * Schedule staggered recording conflict
     * 
     * 1 Set up alphanumeric Contention Handler2 Schedule 3 recordings with
     * names “Rec1”,”Rec2”,”Rec3” and distinct locators. “Rec3” shall be
     * scheduled to start sooner than “Rec1” and “Rec2” and shall end before
     * “Rec1” and “Rec2” end. Recordings shall be in the following states
     * repectively: PENDING NO CONFLICT, PENDING NO CONFLICT, PENDING WITH
     * CONFLICT.3 Shortly after start time, Verify Rec3 is FAILED_STATE4 Verifed
     * if RecordingFailed Exception is INSUFFICIENT_RESOURCES5 “Rec1” and “Rec2”
     * shall finish in COMPLETED_STATE6 Deschedule Contention Handler
     */
    public class TestCase3 extends TestCase
    {

        private OcapLocator m_locator;

        private boolean m_long_rec;

        /**
         * Constructor
         */
        TestCase3(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "SegmentedRec_RecordIfNoConflict : Schedule staggered recording conflict";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long delay = 0;
            // Clear event scheduler
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(800));

            // Schedule the recordings
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 60000 + delay, 60000, 1000));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 60000 + delay, 60000, 2000));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 30000 + delay, 60000, 3000));

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
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 40000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 40010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.FAILED_STATE, 40020 + delay));

            // Shortly after start time, Verify Rec3 is
            // IN_PROGRESS_WITH_ERROR_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 80000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 80010 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.FAILED_STATE, 80020 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording3",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 140000 + delay));

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 150000 + delay));

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
     * Contruct a long recording that will lose contention over time
     * 
     * 1 Schedule a 5 minute recording, “Rec5”, that will have the lowest
     * priority to any recording scheduled.2 Schedule pairs of recordings
     * (“Rec1” and “Rec2”, “Rec3” and “Rec4”) that are 1 minute long 1 minute
     * apart. Rec5 should go to PENDING_WITH_CONFLICT_STATE and all other
     * recordings shall be in the3 Verify that Rec5 moves to the FAILED_STATE4
     * Verify that Rec1 and Rec 2 will move to the IN_PROGRESS_STATE5 Verifed if
     * RecordingFailed Exception is INSUFFICIENT_RESOURCES6 Rec1 and Rec 2 once
     * copleted shall move to the COMPLETED_STATE.7 In one minute, Rec3 and Rec4
     * shall move to the IN_PROGRESS_STATE.8 Rec3 and Rec4 once copleted shall
     * move to the COMPLETED_STATE.9 No Recorded Services shall be generated for
     * Rec5
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
            return "SegmentedRec_RecordIfNoConflict : Contruct a long recording that will lose contention over time";
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
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 90000 + delay, 60000, 1000));
            m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 90000 + delay, 60000, 2000));
            m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 210000 + delay, 60000, 3000));
            m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, now + 210000 + delay, 60000, 4000));
            m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, now + 60000 + delay, 270000, 5000));

            // Verify states prior to recording
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 50000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 50100 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 50200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 50300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 50400 + delay));

            // Shortly after start time, Verify Rec5 is IN_PROGRESS_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 70000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 70100 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 70200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 70300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.FAILED_STATE, 70400 + delay));

            // Shortly after start time, Verify Rec5 is IN_PROGRESS_STATE
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 110000 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 110100 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 110200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 110300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.FAILED_STATE, 110400 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording5",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 120000 + delay));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.FAILED_STATE, 170000 + delay));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 230200 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording4",
                    OcapRecordingRequest.IN_PROGRESS_STATE, 230300 + delay));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.FAILED_STATE, 230400 + delay));

            // Verifed if RecordingFailed Exception is INSUFFICIENT_RESOURCES
            m_eventScheduler.scheduleCommand(new CheckFailedException("Recording5",
                    RecordingFailedException.INSUFFICIENT_RESOURCES, 240000 + delay));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.FAILED_STATE, 290000 + delay));

            // Request shall finish in INCOMPLETE_STATE after stop timer has
            // elapsed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording5",
                    OcapRecordingRequest.FAILED_STATE, 330000 + delay));

            // Deschedule the resource contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 335000 + delay));

            // Run the schedule
            m_eventScheduler.run(5000);

            // Post results
            postResults(getName(), true);
        }
    }

    /**
     * @param locators
     */
    SegmentedRec_RecordIfNoConflict(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestCase2((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestCase3((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestCase4((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

}
