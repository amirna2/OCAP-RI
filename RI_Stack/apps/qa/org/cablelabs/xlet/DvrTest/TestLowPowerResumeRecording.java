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
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.net.OcapLocator;

/**
 * Test to verify that DVR devices are brought out of low power mode prior to
 * the start of a recording. The test assumes that some values are set the same
 * as in the mpeenv.ini file.
 * 
 * @author David H
 * @since November 14, 2005
 */
public class TestLowPowerResumeRecording extends DvrTest implements RecordingAlertListener
{
    /*
     * These two values need to be set to the same values as they appear in the
     * mpeenv.ini file.
     */
    private static final long DVR_LOW_POWER_RESUME_TIME = 2*MINUTE;

    private static final long DVR_STANDBY_LOW_POWER_IDLE_TIME = 3;

    TestLowPowerResumeRecording(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestBasicLowPowerResumeRecording((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * Schedule a recording, wait for the drive to spin down, print a
     * notification at about the time the drive should be spinning up and print
     * another one when recording should be starting. Note that the drive may
     * not always spin down due to other activity on the STB, in which case the
     * test should just be re-executed.
     */
    public class TestBasicLowPowerResumeRecording extends TestCase
    {
        TestBasicLowPowerResumeRecording(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestBasicLowPowerResumeRecording";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;

            DVRTestRunnerXlet.log("Drive will spin up now, but should spin back down in about "
                    + DVR_STANDBY_LOW_POWER_IDLE_TIME + " minute(s)");

            long now = System.currentTimeMillis();

            // Clear the schedule of pending tasks.
            reset();

            // Add a listener to print a message when the recording starts.
            OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            orm.addRecordingAlertListener(TestLowPowerResumeRecording.this);

            
            long recordingWaitTime = (((DVR_STANDBY_LOW_POWER_IDLE_TIME + 2) * MINUTE) + DVR_LOW_POWER_RESUME_TIME);
            long recordingStart = now + recordingWaitTime;
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, recordingStart, MINUTE, 500));

            // This will schedule a notification that the DVR devices should be
            // resuming from a low power mode (DVR_LOW_POWER_RESUME_TIME)
            // milliseconds before when the recording is set to start.
            m_eventScheduler.scheduleCommand(new PrintOut("Drive should spin up now.  Recording starts in about "
                    + (DVR_LOW_POWER_RESUME_TIME / 1000) + " second(s)", 
                recordingWaitTime - DVR_LOW_POWER_RESUME_TIME + 500));

            m_eventScheduler.run(DVR_LOW_POWER_RESUME_TIME + 3000);

            orm.removeRecordingAlertListener(TestLowPowerResumeRecording.this);

            DVRTestRunnerXlet.log("TestLowPowerResumeRecording testBasicLowPowerResumeRecording completed");
        }

        private OcapLocator m_locator;
    }

    public void recordingAlert(RecordingAlertEvent e)
    {
        DVRTestRunnerXlet.log("Recording Started");
    }
}
