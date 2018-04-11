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

import org.apache.log4j.Logger;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingRequest;

/**
 * DVRTest for bug 4524.
 * 
 * @author jspruiel
 * 
 */
public class TestForDeadLock_bug4524 extends DvrTest
{
    private OcapLocator m_locatorC;

    private OcapLocator m_locatorA;

    private OcapLocator m_locatorB;

    TestForDeadLock_bug4524(Vector locators)
    {
        super(locators);
        m_locators = locators;
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new DdLockOnBRequest((OcapLocator) m_locators.elementAt(0),
                (OcapLocator) m_locators.elementAt(1), (OcapLocator) m_locators.elementAt(2)));

        return tests;
    }

    /**
     * This test case is used to verify absence of the deadlock described in bug
     * 4524. Test Proceedure: - Register the ResourceContentionHandler - Start
     * Recording 1 - Start Recording 2 - Create and start and BufferingRequest
     * Results: - Recording 1 should loose contention to the BufferingRequest -
     * The system should remain responsive. Run the test a second time and look
     * for lockups.
     */
    class DdLockOnBRequest extends TestCase implements ResourceContentionHandler
    {
        DdLockOnBRequest(OcapLocator locatorA, OcapLocator locatorB, OcapLocator locatorC)
        {
            m_locatorA = locatorA;
            m_locatorB = locatorB;
            m_locatorC = locatorC;
            m_eventScheduler = new EventScheduler();
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "DdLockOnBRequest -";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            m_log = Logger.getLogger("DdLockOnBRequest");
            m_log.info("runTest executing.");

            long now = System.currentTimeMillis();

            // register the contention handler
            new RegisterResourceContentionHandler(m_log, this, 500).ProcessCommand();

            // create the BR but do not requestBuffering
            new CreateBufferingRequest(m_log, "A", m_locatorA, 300, 500, null, 0).ProcessCommand();

            // start the first recording and the second.
            new Record(m_log, "B", m_locatorB, now + 500, 60000 * 8L, 0).ProcessCommand();

            m_eventScheduler.scheduleCommand(new Record(m_log, "C", m_locatorC, now + 20000, 60000 * 8L, 2000));

            // both should be in the in progress state.
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(m_log, "B",
                    LeafRecordingRequest.IN_PROGRESS_STATE, 70000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(m_log, "C",
                    LeafRecordingRequest.IN_PROGRESS_STATE, 95000));

            // Now start buffering
            m_eventScheduler.scheduleCommand(new StartBufferingRequest(m_log, "A", 100000));

            // Were done, so start cleanup.
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(m_log, null, 120000));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(m_log, "B", 130000));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(m_log, "C", 140000));

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(m_log, "A", 150000));

            m_eventScheduler.run(4000);

            if (!rchCalled) m_failed = TEST_FAILED;

            if (m_failed == TEST_FAILED)
            {

                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        public ResourceUsage[] resolveResourceContention(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            rchCalled = true;

            m_log.debug(getName() + ":@@@@@@@@@@@@@@@@@@ ENTERED RCH.resolveResourceContention @@@@@@@@@@@@@@@@@@");
            return new DefaultResourceContentionHandler().resolveResourceContention(newRequest, currentReservations);
        }

        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            // TODO Auto-generated method stub

        }

        private boolean rchCalled = false;

        private Logger m_log;
    }// end testcase

    private Vector m_locators;
}
