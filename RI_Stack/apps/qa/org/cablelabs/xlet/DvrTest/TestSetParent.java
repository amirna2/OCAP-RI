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

import javax.tv.locator.Locator;

import org.havi.ui.HScene;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.net.OcapLocator;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.dvr.storage.MediaStorageVolume;

public class TestSetParent extends DvrTest
{

    TestSetParent(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestSetParentRecording((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * Perform a collection of scheduled recordings, and confirm their
     * successful completion
     * 
     * @param locators
     */
    public class TestSetParentRecording extends TestCase
    {
        private int recState;

        TestSetParentRecording(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestSetParentRecording";
        }

        public void runTest()
        {
            recState = 0;
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Create two OcapRecordingRequest
            m_eventScheduler.scheduleCommand(new Record("orr1", m_locator, now + 240000, 240000, 500));
            m_eventScheduler.scheduleCommand(new Record("orr2", m_locator, now + 240000, 240000, 1000));

            OcapRecordingProperties orp = getORP();

            // Create two root recording request.
            m_eventScheduler.scheduleCommand(new NewSeriesRoot("prr1", new PrivateRecordingSpec("TestSpec", orp), 2000L));
            m_eventScheduler.scheduleCommand(new NewSeriesRoot("prr2", new PrivateRecordingSpec("TestSpec", orp), 3000L));

            // Check ExtendedAccessFilePermission of each recording
            m_eventScheduler.scheduleCommand(new CheckRecordingFAP("orr1", 5000L));
            m_eventScheduler.scheduleCommand(new CheckRecordingFAP("prr1", 6000L));
            m_eventScheduler.scheduleCommand(new CheckRecordingFAP("prr2", 7000L));


            // no parent set yet, make sure getParent() returns null for orr1
            m_eventScheduler.scheduleCommand(new CheckSetParent("orr1", null, -999, 8000L));

            m_eventScheduler.scheduleCommand(new CheckSetParent("orr1", "prr1", ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 9000L));

            /* OC-SP-OCAP-DVR-IO5-090612: 
             * if the parent was already set, this rr (orr1) will be removed 
             * from the previously set parent and added to the parent 
             * parameter (prr2)
             */
            m_eventScheduler.scheduleCommand(new CheckSetParent("orr1", "prr2", ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 10000L));

            /* OC-SP-OCAP-DVR-IO5-090612: 
             * if the rr (orr1) is removed from a PRR(prr1) which is in
             * the COMPLETELY_RESOLVED_STATE and which contains no other
             * leaf, that prr (prr1) shall be transitioned to the  
             * PARTIALLY_RESOLVED_STATE
             */
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("prr1", ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 11000L)); 



            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("prr2", 12000L));
            m_eventScheduler.scheduleCommand(new ValidateRequest("prr2", true, 13000L));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("prr2", ParentRecordingRequest.CANCELLED_STATE, 14000L));

            //Check for IllegalArgumentException
            m_eventScheduler.scheduleCommand(new CheckSetParent("orr2", "prr2", ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 15000L)); 

            //prr2 is in CANCELLED_STATE and has no other recordings
            /* OC-SP-OCAP-DVR-IO5-090612: 
             * if the rr (orr1) is removed from a PRR(prr2) which is in
             * the CANCELLED_STATE and which contains no other leaf,
             * that prr (prr2) shall be deleted from the recording db
             */
            m_eventScheduler.scheduleCommand(new CheckSetParent("orr1", "prr1", ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 16000L));
            m_eventScheduler.scheduleCommand(new ValidateRequest("prr2", false, 17000L));

            // Clean up
            m_eventScheduler.scheduleCommand(new DeleteRecording("orr1", 18000));
            m_eventScheduler.scheduleCommand(new DeleteRecording("orr2", 19000));
            m_eventScheduler.scheduleCommand(new DeleteRecording("prr1", 20000));

            m_eventScheduler.run(5000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: FAILED - "+m_failedReason);
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() +" completed: PASSED.");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;

        private OcapRecordingProperties getORP()
        {
            return new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, (long) 10000000, 127, (byte) OcapRecordingProperties.RECORD_WITH_CONFLICTS, null, null, null);
        }
    }


    class CheckSetParent extends EventScheduler.NotifyShell
    {
        CheckSetParent(String rec, String prr, int state, long triggerTime)
        {
            super(triggerTime);
            m_rName = rec;
            m_pName = prr;
            m_resolutionParentState = state;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<SetParent::ProcessCommand "+m_rName +">>>>");

            OcapRecordingRequest rr = (OcapRecordingRequest)findObject(m_rName);
            if (rr == null)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils: CheckSetParent - recording not found! " + m_rName;
                System.out.println(m_failedReason);
                return;
            }
            ParentRecordingRequest prr = null;
            if (m_pName != null)
            {
                prr = (ParentRecordingRequest) findObject(m_pName);
                if (prr == null)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "DVRUtils: CheckSetParent - recording not found! " + m_pName;
                    System.out.println(m_failedReason);
                    return;
                }
            }

            boolean throwISE = false;
            RecordingRequest initialParent = rr.getParent();
            if (prr != null && prr.getState() == ParentRecordingRequest.CANCELLED_STATE) throwISE = true;
       
            // resolutionParentState 999 indicates to just do an initial 
            // check of rr's getParent() return value
            if (m_resolutionParentState != -999) 
            {
                try 
                { 
                    rr.setParent(prr, m_resolutionParentState);
                    if (throwISE)
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason = "DVRUtils: CheckSetParent Failure - IllegalStateException should have been thrown when parent parameter of setParent() is in the CANCELLED_STATE for " + m_rName;
                        System.out.println(m_failedReason);
                    }
                }
                catch (Exception e) 
                {
                    if (throwISE && e instanceof IllegalStateException)
                    {
                        System.out.println("DVRUtils: CheckSetParent - IllegalStateException correctly thrown when parent parameter of setParent() is in the CANCELLED_STATE for " + m_rName);
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason = "DVRUtils: CheckSetParent Failure - unexpected has been thrown by the setParent() call on "+m_rName;
                        System.out.println(m_failedReason);
                    }
                }
            }


            if (!throwISE && rr.getParent() != prr)
            {
                m_failedReason = "DVRUtils: CheckSetParent Failure - incorrect parent for " + m_rName;
                System.out.println(m_failedReason);
                m_failed = TEST_FAILED;
            }
            if (throwISE && rr.getParent() != initialParent)
            {
                m_failedReason = "DVRUtils: CheckSetParent Failure - incorrect parent for " + m_rName +", should not have changed";
                System.out.println(m_failedReason);
                m_failed = TEST_FAILED;
            }

            if (!throwISE)
            {
                if (prr.getState() != m_resolutionParentState && 
                    m_resolutionParentState != -999)
                {
                    m_failedReason = "DVRUtils: CheckSetParent Failure - state of " +m_pName +" should be " +m_resolutionParentState +"; not " +prr.getState();
                    System.out.println(m_failedReason);
                    m_failed = TEST_FAILED;
                }
            }
            else
            {
                if (prr.getState() != ParentRecordingRequest.CANCELLED_STATE)
                {
                    m_failedReason = "DVRUtils: CheckSetParent Failure - state of " +m_pName +" should have stayed at CANCELLED_STATE instead of " +m_resolutionParentState +"; not " +prr.getState();
                    System.out.println(m_failedReason);
                    m_failed = TEST_FAILED;
                }
            }

        }

        private String m_rName;
        private String m_pName;
        private int m_resolutionParentState;
    }

    class CheckRecordingFAP extends EventScheduler.NotifyShell
    {
        CheckRecordingFAP(String rec, long triggerTime)
        {
            super(triggerTime);
            m_rName = rec;

            int[] orgs = new int[1];
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<CheckRecordingFAP::ProcessCommand "+m_rName +">>>>");

            RecordingRequest rr = (RecordingRequest) findObject(m_rName);
            if (rr == null)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils: CheckRecordingFAP - recording not found! " + m_rName;
                System.out.println(m_failedReason);
                return;
            }

            RecordingSpec ors = rr.getRecordingSpec();
            RecordingProperties rp = ors.getProperties();

            if (rp instanceof OcapRecordingProperties)
            {
                OcapRecordingProperties orp = (OcapRecordingProperties) rp;
                ExtendedFileAccessPermissions eFAP = orp.getAccessPermissions();
                System.out.println("DVRUtils: CheckRecordingFAP - efap of "+m_rName +" = " +eFAP.toString());

                if (eFAP.hasReadWorldAccessRight() == true || 
                    eFAP.hasWriteWorldAccessRight() == true ||
                    eFAP.hasReadOrganisationAccessRight() == true || 
                    eFAP.hasWriteOrganisationAccessRight() == true || 
                    eFAP.hasReadApplicationAccessRight() == false || 
                    eFAP.hasWriteApplicationAccessRight() == false)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "DVRUtils: CheckRecordingFAP failure - Illegal eFAP found for " + m_rName + "  ::  " + eFAP.toString();
                    System.out.println(m_failedReason);
                }
            }
        }

        private String m_rName;
    }
}
