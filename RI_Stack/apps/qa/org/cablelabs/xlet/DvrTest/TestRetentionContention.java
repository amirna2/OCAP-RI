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
 * Created on Jan 4, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.OcapRecordingProperties;

/**
 * @author RyanH
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestRetentionContention extends DvrTest
{

    TestRetentionContention(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestRetentionContention1((OcapLocator) m_locators.elementAt(0), 90, OcapRecordingProperties.DELETE_AT_EXPIRATION));
        tests.addElement(new TestRetentionContention1((OcapLocator) m_locators.elementAt(0), 90, 1));

        tests.addElement(new TestRetentionContention1((OcapLocator) m_locators.elementAt(0), 0, OcapRecordingProperties.DELETE_AT_EXPIRATION));
        tests.addElement(new TestRetentionContention1((OcapLocator) m_locators.elementAt(0), 0, 1));

        return tests;
    }

    public class TestRetentionContention1 extends TestCase implements RecordingAlertListener, RecordingChangedListener
    {
        TestRetentionContention1(OcapLocator locator, long exp, int rp)
        {
            m_locator = locator;
            m_retention = rp;
            m_expiration = exp;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRetentionContention1(expiration=" +m_expiration +(m_retention==OcapRecordingProperties.DELETE_AT_EXPIRATION? ", delete_at_expiration":"") +")";
        }

        public void runTest()
        {
            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());
            rm.addRecordingChangedListener(this);
            rm.addRecordingAlertListener(this);

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(500));

            // Schedule 3 minute-long parallel recordings 

            long start = 30*SEC;
            long duration = MINUTE;

            m_eventScheduler.scheduleCommand(new Record("R1", m_locator, now+start, duration, 1000, m_expiration, m_retention));
            m_eventScheduler.scheduleCommand(new Record("R2", m_locator, now+start, duration, 1010, m_expiration, m_retention));
            m_eventScheduler.scheduleCommand(new Record("R3", m_locator, now+start, duration, 1020, m_expiration, m_retention));


            // Check Recordings for status before completion
            long trigger = start + 45*SEC; 
            if (trigger >= start+m_expiration*SEC &&
                m_retention == OcapRecordingProperties.DELETE_AT_EXPIRATION)
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.DELETED_STATE, trigger));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.DELETED_STATE, trigger+100));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.DELETED_STATE, trigger+200));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.IN_PROGRESS_STATE, trigger));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.IN_PROGRESS_STATE, trigger+100));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.FAILED_STATE, trigger+200));
            }


            // Check Recordings after complete 
            trigger = start + duration + 10*SEC; 
            if (trigger >= start+m_expiration*SEC &&
                m_retention == OcapRecordingProperties.DELETE_AT_EXPIRATION)
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.DELETED_STATE, trigger));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.DELETED_STATE, trigger+100));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.DELETED_STATE, trigger+200));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.COMPLETED_STATE, trigger));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.COMPLETED_STATE, trigger+100));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.FAILED_STATE, trigger+200));
            }


            // Check Recordings after expiration
            trigger = start + m_expiration*SEC + 2000;
            if (m_expiration*SEC < duration)
            {
                trigger = start + duration + 12000;
            }
            if (m_retention == OcapRecordingProperties.DELETE_AT_EXPIRATION)
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.DELETED_STATE, trigger));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.DELETED_STATE, trigger+100));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.DELETED_STATE, trigger+200));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R1", OcapRecordingRequest.COMPLETED_STATE, trigger+2000));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R2", OcapRecordingRequest.COMPLETED_STATE, trigger+2100));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("R3", OcapRecordingRequest.FAILED_STATE, trigger+2200));

                //clean up
                m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R1", trigger+2600));
                m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("R2", trigger+2700));
            }


            // Schedule the deletion of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, trigger+3000));

            m_eventScheduler.run(2000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() +" completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() +" completed: FAILED - "+m_failedReason);
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

            rm.addRecordingChangedListener(this);
            rm.addRecordingAlertListener(this);

        }

        /**
         * Handler for start recording notifications
         */

        public void recordingAlert(RecordingAlertEvent e)
        {
            DvrEventPrinter.printRecordingAlertEvent(e);
        } // END recordingAlert()

        /**
         * Handler for state, and list change events.
         */
        public void recordingChanged(RecordingChangedEvent e)
        {
            DvrEventPrinter.printRecordingChangedEvent(e);
        } // END recordingChanged()

        private OcapLocator m_locator;
        private int m_retention; 
        private long m_expiration; 
    }
}
