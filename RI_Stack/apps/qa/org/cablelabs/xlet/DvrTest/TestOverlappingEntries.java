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
 * Created on Feb 12, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.net.OcapLocator;
import org.ocap.dvr.OcapRecordingRequest;

/**
 * @author fsmith
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestOverlappingEntries extends DvrTest
{

    TestOverlappingEntries(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestOverlapping((OcapLocator) m_locators.elementAt(0)));

        return tests;
    }

    /*
     * This test doesn't benefit from using the scheduler framework, but....
     */
    public class TestOverlapping extends TestCase
    {
        TestOverlapping(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestOverlappingEntries";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // Array from Jeff's J-unit tests
            // Create a list of RecordingImpl to feed this test
            // and add it to the list to be returned.
            int sdArr[][] = { { 0, 10 }, { 0, 12 }, { 0, 8 }, { -5, 20 }, { -5, 13 }, { -5, 15 }, { -5, 3 }, { 3, 9 }, { 2, 8 }, { 2, 4 }, { 12, 14 }, { -5, 5 }, { 10, 2 } };

            // clear the schedule of pending tasks
            reset();
            // Schedule the record call
            for (int x = 0; x < 13; x++)
            {
                m_eventScheduler.scheduleCommand(new Record("Recording" + x, m_locator, now + m_oneDay + sdArr[x][0] * 10000, sdArr[x][1] * 10000, 500 + x * 50));
            }

            // verify there are 8 overlapping recording request with 
            // "Recording9": Recording0, 1, 2, 3, 4, 5, 7, 8
            m_eventScheduler.scheduleCommand(new CheckOverlappingEntries("Recording" + 9, 8, 3000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("testOverlappingEntries testOverlappingEntries completed: FAILED");
            }
            else
            {
                DVRTestRunnerXlet.log("testOverlappingEntries testOverlappingEntries completed: PASSED");
            }

        }

        private OcapLocator m_locator;
    }

    class CheckOverlappingEntries extends EventScheduler.NotifyShell
    {
        CheckOverlappingEntries(String recording, int expectedCount, long triggerTime)
        {
            super(triggerTime);

            m_recording = recording;
            m_recordingCount = expectedCount;
        }

        public void ProcessCommand()
        {

            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recording);

            if (rr == null)
            {
                DVRTestRunnerXlet.log("CheckOverlappingEntries - recording not found: " + m_recording);
                m_failed = TEST_FAILED;
                return;
            }

            DVRTestRunnerXlet.log("Checking overlapping entries against:");
            DvrEventPrinter.printRecording(rr);

            RecordingList rl = rr.getOverlappingEntries();
            if (rl == null)
            {
                DVRTestRunnerXlet.log("CheckOverlappingEntries Recording list null!");
                m_failed = TEST_FAILED;
                return;
            }

            if (rl.size() != m_recordingCount)
            {
                DVRTestRunnerXlet.log("CheckOverlappingEntries FAILED: Expected " + m_recordingCount + " found " + rl.size());
                m_failed = TEST_FAILED;
            }

            DVRTestRunnerXlet.log("Starting Overlapping Entries:");
            for (int x = 0; x < rl.size(); x++)
            {
                rr = (OcapRecordingRequest) rl.getRecordingRequest(x);
                DvrEventPrinter.printRecording(rr);
            }
            DVRTestRunnerXlet.log("End Overlapping Entries:");
        }

        private int m_recordingCount;

        private String m_recording;
    }

}
