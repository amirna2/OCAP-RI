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
 * Created on May 10, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;

import org.cablelabs.xlet.DvrTest.DvrTest.ConfirmRecordingReq_CheckState;
import org.cablelabs.xlet.DvrTest.DvrTest.DestroyBroadcastService;
import org.cablelabs.xlet.DvrTest.DvrTest.Record;
import org.cablelabs.xlet.DvrTest.DvrTest.RecordByService;
import org.cablelabs.xlet.DvrTest.DvrTest.SelectService;
import org.cablelabs.xlet.DvrTest.DvrTest.SetMediaTimeBack;
import org.cablelabs.xlet.DvrTest.DvrTest.StopBroadcastService;
import org.cablelabs.xlet.DvrTest.DvrTest.TestCase;
import org.cablelabs.xlet.DvrTest.DvrTest.initServiceContext;
import org.cablelabs.xlet.DvrTest.DvrTest.setSCBuffering;
import org.cablelabs.xlet.DvrTest.TestSharedResources3.TestBufferedSC_w_RecByServ_A;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestSharedResources5 extends DvrTest
{

    private OcapLocator locator;

    /**
     * @param locators
     */
    TestSharedResources5(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public class Rec_w_BufferReq_A_B_C_D extends TestCase
    {

        private OcapLocator m_locator;

        private boolean m_byService;

        private int m_delay;

        private String m_letter;

        Rec_w_BufferReq_A_B_C_D(OcapLocator locator, boolean byService, int delay, String letter)
        {
            m_locator = locator;
            m_byService = byService;
            m_delay = delay;
            m_letter = letter;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Rec_w_BufferReq " + m_letter + " record in the past " + m_locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("RecByLoc_w_BufferReq_A start: ");
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // Initiate a buffer request for service A
            m_eventScheduler.scheduleCommand(new ScheduleBufferingRequest("Buffer1", m_locator, 300, 1200, 1000));

            // Start the buffering request
            m_eventScheduler.scheduleCommand(new StartBufferingRequest("Buffer1", 2000));

            // After buffer request has been accepted, wait 1 or 2 minutes
            // and start recording seconds in the past for 90 seconds on service
            // A
            if (!m_byService)
            {
                m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 90000,
                        m_delay + 60000));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new RecordByService("Recording1", m_locator, now + 30000, 90000,
                        m_delay + 60000));
            }

            // Validate recording is in the completed state at end. If possible
            // visual confirmation of content.
            // Verify recording is completed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 135000));

            m_eventScheduler.scheduleCommand(new SelectRecordedService("Recording1", 140000));

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest("Buffer1", 160000));

            m_eventScheduler.run(25000);

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

    public class RecByBufferedSC_w_BufferReq_E extends TestCase
    {

        private OcapLocator m_locator;

        RecByBufferedSC_w_BufferReq_E(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "RecByBufferedSC_w_BufferReq_E " + m_locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("RecByBufferedSC_w_BufferReq_E start: ");
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // Initiate a buffer request on service A
            m_eventScheduler.scheduleCommand(new ScheduleBufferingRequest("Buffer1", m_locator, 300, 1200, 1000));

            // Start the buffering request
            m_eventScheduler.scheduleCommand(new StartBufferingRequest("Buffer1", 2000));

            // After buffer request has been accepted, wait 1 minute
            // Initiate tuning in a buffered ServiceContext to service A
            m_eventScheduler.scheduleCommand(new initServiceContext(40000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(400, 2400, 41000));
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 42000));

            // Compare duration and see if greater than 45 seconds
            m_eventScheduler.scheduleCommand(new checkSCBufferDuration(45000, 75000));

            // Get current MediaTime and set 50 seconds prior to buffered SC
            // call.
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack(50.0F, 80000));

            // stop presentation
            m_eventScheduler.scheduleCommand(new StopService(120000));
            m_eventScheduler.scheduleCommand(new DestroyService(130000));

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest("Buffer1", 140000));

            // wait for the presenation to complete
            m_eventScheduler.run(15000);

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

    /*
     * Test Case F: A unbuffered Service Context is started and a scrs recording
     * is called with start time in the past This will be implemented by
     * DVRTestRunner 1) Initiate a buffer request on service A 2) After buffer
     * request has been accepted, wait 1 minute 3) Initiate tuning in an
     * unbuffered ServiceContext to service A 4) Start recording 30 seconds in
     * the past for 1 minute 5) Validate recording is in the completed state at
     * end and that SC is presenting. If possible visual confirmation of
     * content.
     * 
     * Test Case G: A buffered Service Context is started and a scrs recording
     * is called with start time in the past This will be implemented by
     * DVRTestRunner 1) Initiate a buffer request on service A 2) After buffer
     * request has been accepted, wait 1 minute 3) Initiate tuning by a buffered
     * ServiceContext to service A 4) Start recording 30 seconds in the past for
     * 1 minute 5) Validate recording is in the completed state at end and that
     * SC is presenting. If possible visual confirmation of content.
     * 
     * Test Case H: An unbuffered Service Context is started and a scrs
     * recording is called with start and stop time in the past This will be
     * implemented by DVRTestRunner 1) Initiate a buffer request on service A 2)
     * After buffer request has been accepted, wait 2 minute 3) Initiate tuning
     * by a unbuffered ServiceContext to service A 4) Start recording 90 seconds
     * in the past for 1 minute 5) Validate recording is in the completed state
     * at end and that SC is presenting. If possible visual confirmation of
     * content.
     * 
     * Test Case I: A buffered Service Context is started and a scrs recording
     * is called with start and stop time in the past Recording by lrs is called
     * in the past This will be implemented by DVRTestRunner 1) Initiate a
     * buffer request on service A 2) After buffer request has been accepted,
     * wait 2 minute 3) Initiate tuning by a buffered ServiceContext to service
     * A 4) Start recording 90 seconds in the past for 1 minute 5) Validate
     * recording is in the completed state at end and that SC is presenting. If
     * possible visual confirmation of content.
     * 
     * Test Case J: An unbuffered Service Context is then started and a scrs
     * recording is called with start and stop time in the past and start time
     * is beyond buffered content This will be implemented by DVRTestRunner 1)
     * Initiate a buffer request on service A 2) After buffer request has been
     * accepted, wait 1 minutes 3) Initiate tuning by a unbuffered
     * ServiceContext to service A 4) Start recording 90 seconds in the past for
     * 1 minute 5) Validate recording is in the incomplete state at end and that
     * SC is presenting. If possible visual confirmation of content.
     * 
     * Test Case K: A buffered Service Context is then started and a scrs
     * recording is called with start and stop time in the past and start time
     * is beyond buffered content Recording by lrs is called in the past This
     * will be implemented by DVRTestRunner 1) Initiate a buffer request on
     * service A 2) After buffer request has been accepted, wait 1 minutes 3)
     * Initiate tuning by a buffered ServiceContext to service A 4) Start
     * recording 90 seconds in the past for 1 minute 5) Validate recording is in
     * the incomplete state at end and that SC is presenting. If possible visual
     * confirmation of content.
     */

    public class RecBySC_w_BufferReq_F_G_H_I_J_K extends TestCase
    {

        private OcapLocator m_locator;

        private String m_letter;

        private boolean m_buffer;

        private long m_delaySC;

        private long m_delayRec;

        RecBySC_w_BufferReq_F_G_H_I_J_K(OcapLocator locator, boolean buffer, long delaySC, long delayRec, String letter)
        {
            m_locator = locator;
            m_buffer = buffer;
            m_delaySC = delaySC;
            m_delayRec = delayRec;
            m_letter = letter;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "RecBySC_w_BufferReq" + m_letter + " " + m_locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("RecByLoc_w_BufferReq_A start: ");
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // Initiate a buffer request on service A
            m_eventScheduler.scheduleCommand(new ScheduleBufferingRequest("Buffer1", m_locator, 300, 1200, 10000));

            // Start the buffering request
            m_eventScheduler.scheduleCommand(new StartBufferingRequest("Buffer1", 35000));

            // After buffer request has been accepted, wait 1 minute
            // Initiate tuning in a buffered ServiceContext to service A
            m_eventScheduler.scheduleCommand(new initServiceContext(70000 + m_delaySC));
            if (m_buffer)
            {
                m_eventScheduler.scheduleCommand(new setSCBuffering(300, 2400, 71000 + m_delaySC));
            }
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 72000 + m_delaySC));

            m_eventScheduler.scheduleCommand(new RecordBySC("Recording1", (now + m_delayRec), 90000, m_delaySC + 100000));

            if (m_delayRec < 10000)
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                        OcapRecordingRequest.INCOMPLETE_STATE, 160000));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                        OcapRecordingRequest.COMPLETED_STATE, 160000));
            }

            // stop presentation
            m_eventScheduler.scheduleCommand(new StopService(170000));
            m_eventScheduler.scheduleCommand(new DestroyService(175000));

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest("Buffer1", 180000));

            // wait for the presenation to complete
            m_eventScheduler.run(15000);

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

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new Rec_w_BufferReq_A_B_C_D((OcapLocator) m_locators.elementAt(0), false, 10000, "A"));
        tests.addElement(new Rec_w_BufferReq_A_B_C_D((OcapLocator) m_locators.elementAt(0), true, 10000, "B"));
        tests.addElement(new Rec_w_BufferReq_A_B_C_D((OcapLocator) m_locators.elementAt(0), false, 70000, "C"));
        tests.addElement(new Rec_w_BufferReq_A_B_C_D((OcapLocator) m_locators.elementAt(0), true, 70000, "D"));
        tests.addElement(new RecByBufferedSC_w_BufferReq_E((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new RecBySC_w_BufferReq_F_G_H_I_J_K((OcapLocator) m_locators.elementAt(0), false, 0, 36000,
                "F"));
        tests.addElement(new RecBySC_w_BufferReq_F_G_H_I_J_K((OcapLocator) m_locators.elementAt(0), true, 0, 36000, "G"));
        tests.addElement(new RecBySC_w_BufferReq_F_G_H_I_J_K((OcapLocator) m_locators.elementAt(0), false, 36000,
                36000, "H"));
        tests.addElement(new RecBySC_w_BufferReq_F_G_H_I_J_K((OcapLocator) m_locators.elementAt(0), true, 36000, 36000,
                "I"));
        tests.addElement(new RecBySC_w_BufferReq_F_G_H_I_J_K((OcapLocator) m_locators.elementAt(0), false, 0, 0, "J"));
        tests.addElement(new RecBySC_w_BufferReq_F_G_H_I_J_K((OcapLocator) m_locators.elementAt(0), true, 0, 0, "K"));
        return tests;
    }
}
