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
 * Created on Jan 24, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.dvr.RequestResolutionHandler;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.resource.ResourceUsage;

import org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell;


/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestSeriesContention extends DvrTest
{
    private static final String ROOT1 = "RootShow1";
    private static final String BRANCH1 = "BranchSeason1";
    private static final String LEAF1 = "LeafEpisode1";
    private static final String ROOT2 = "RootShow2";
    private static final String BRANCH2 = "BranchSeason2";
    private static final String LEAF2 = "LeafEpisode2";
    private static final String ROOT3 = "RootShow3";
    private static final String BRANCH3 = "BranchSeason3";
    private static final String LEAF3 = "LeafEpisode3";

    private static final boolean IN_LIST = true;
    private static final boolean NOT_IN_LIST = false;

    TestSeriesContention(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestSeriesContention1((OcapLocator) m_locators.elementAt(0), false));
        tests.addElement(new TestSeriesContention1((OcapLocator) m_locators.elementAt(0), true));
        tests.addElement(new TestSeriesContention_delete((OcapLocator) m_locators.elementAt(0), true));
        tests.addElement(new TestSeriesContention_delete((OcapLocator) m_locators.elementAt(0), false));

        return tests;
    }


    /**
     * 
     */
    public class TestSeriesContention1 extends TestCase implements RequestResolutionHandler
    {
        TestSeriesContention1(OcapLocator locator, boolean reverseOrder)
        {
            m_locator = locator;
            m_reverseOrder = reverseOrder;
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long expiration = 60L * 60L * 24L; // 24 hour expiration
            long start = MINUTE;
            long duration = MINUTE;

            String[] recPriorities = {LEAF2, LEAF1, LEAF3};

            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(500));

            // Install the request resolution handler.
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            rm.setRequestResolutionHandler(this);

            // inherited down to the leaf.
            OcapRecordingProperties orp = new OcapRecordingProperties(
                OcapRecordingProperties.HIGH_BIT_RATE,
                expiration, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, 
                null, null, null);


            // Create the 1st set of series recording
            String root = ROOT1;
            String branch = BRANCH1;
            String leaf = LEAF1;
            if (m_reverseOrder)
            {
                root = ROOT3;
                branch = BRANCH3;
                leaf = LEAF3;
            }
            m_eventScheduler.scheduleCommand(new NewSeriesRoot(root,
                new PrivateRecordingSpec("Show1", orp), 1000));

            m_eventScheduler.scheduleCommand(new AddSeason(branch, root,
                new PrivateRecordingSpec("Season1", orp), 
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 1500));

            m_eventScheduler.scheduleCommand(new AddEpisode(leaf, branch, 
                m_locator, now + start, duration,
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 
                2000, expiration));


            // Create the 2nd set of series recording
            root = ROOT2;
            branch = BRANCH2;
            leaf = LEAF2;
            if (m_reverseOrder)
            {
                root = ROOT1;
                branch = BRANCH1;
                leaf = LEAF1;
            }
            m_eventScheduler.scheduleCommand(new NewSeriesRoot(root,
                new PrivateRecordingSpec("Show2", orp), 4000));

            m_eventScheduler.scheduleCommand(new AddSeason(branch, root,
                new PrivateRecordingSpec("Season1", orp), 
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 5000));

            m_eventScheduler.scheduleCommand(new AddEpisode(leaf, branch,
                    m_locator, now + start, duration,
                    ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 
                    6000, expiration));


            // Create the 3rd set of series recording
            root = ROOT3;
            branch = BRANCH3;
            leaf = LEAF3;
            if (m_reverseOrder)
            {
                root = ROOT2;
                branch = BRANCH2;
                leaf = LEAF2;
            }
            m_eventScheduler.scheduleCommand(new NewSeriesRoot(root, 
                new PrivateRecordingSpec("Show3", orp), 8000));

            m_eventScheduler.scheduleCommand(new AddSeason(branch, root,
                new PrivateRecordingSpec("Season1", orp), 
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 9000));

            m_eventScheduler.scheduleCommand(new AddEpisode(leaf, branch,
                m_locator, now + start, duration,
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 
                10000, expiration));



            // verifications 
            m_eventScheduler.scheduleCommand(new CheckRUPriority("LeafEpisode1", recPriorities, 15000));


            long trigger = 3000 + start + duration;
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(
                LEAF1, OcapRecordingRequest.COMPLETED_STATE, trigger));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(
                LEAF2, OcapRecordingRequest.COMPLETED_STATE, trigger+100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(
                LEAF3, OcapRecordingRequest.FAILED_STATE, trigger+200));

            // all rr should still be in the RecordingList
            m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH1, IN_LIST, trigger+1000));
            m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF1, IN_LIST, trigger+1100));
            m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT1, IN_LIST, trigger+1200));

            m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH2, IN_LIST, trigger+1300));
            m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF2, IN_LIST, trigger+1400));
            m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT2, IN_LIST, trigger+1500));

            m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF3, IN_LIST, trigger+1600));
            m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH3, IN_LIST, trigger+1700));
            m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT3, IN_LIST, trigger+1800));

            // Delete roots
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(ROOT1, trigger+2000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(ROOT2, trigger+3000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(ROOT3, trigger+4000));

            // Verify all recording requests created are deleted
            m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH1, NOT_IN_LIST, trigger+5000));
            m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF1, NOT_IN_LIST, trigger+5100));
            m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT1, NOT_IN_LIST, trigger+5200));

            m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH2, NOT_IN_LIST, trigger+5300));
            m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF2, NOT_IN_LIST, trigger+5400));
            m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT2, NOT_IN_LIST, trigger+5500));

            m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH3, NOT_IN_LIST, trigger+5600));
            m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF3, NOT_IN_LIST, trigger+5700));
            m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT3, NOT_IN_LIST, trigger+5800));

            m_eventScheduler.run(1000);

            // remove listenters and handlers
            rm.setRequestResolutionHandler(null);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log(getName() + " Completed FAILED - " +m_failedReason);
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " Completed PASSED");
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.ocap.dvr.RequestResolutionHandler#requestResolution(org.ocap.
         * shared.dvr.RecordingRequest)
         */
        public void requestResolution(RecordingRequest request)
        {
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestSeriesContention1(" +
                (m_reverseOrder ? "ReversedOrder" : "NormalOrder" + ")");
        }

        private OcapLocator m_locator;
        private boolean m_reverseOrder;
    }

    /**
     * 
     */
    public class TestSeriesContention_delete extends TestCase implements RequestResolutionHandler, RecordingChangedListener
    {
        TestSeriesContention_delete(OcapLocator locator, boolean deleteByReq)
        {
            m_locator = locator;
            m_deleteByReq = deleteByReq;
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long expiration = 60L * 60L * 24L; // 24 hour expiration
            long start = MINUTE;
            long start3 = 2*MINUTE;
            long duration = 2*MINUTE;

            String[] recPriorities = {LEAF2, LEAF1, LEAF3};

            reset();

            // Schedule the addition of the contention handler
            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(500));

            // Install the request resolution handler.
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            rm.setRequestResolutionHandler(this);
            rm.addRecordingChangedListener(this);

            // inherited down to the leaf.
            OcapRecordingProperties orp = new OcapRecordingProperties(
                OcapRecordingProperties.HIGH_BIT_RATE,
                expiration, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, 
                null, null, null);


            // Create the 1st set of series recording
            m_eventScheduler.scheduleCommand(new NewSeriesRoot(ROOT3,
                new PrivateRecordingSpec("Show3", orp), 1000));

            m_eventScheduler.scheduleCommand(new AddSeason(BRANCH3, ROOT3,
                new PrivateRecordingSpec("Season1", orp), 
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 2000));

            m_eventScheduler.scheduleCommand(new AddEpisode(LEAF3, BRANCH3, 
                m_locator, now + start3, duration,
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 
                3000, expiration));


            // Create the 2nd set of series recording
            m_eventScheduler.scheduleCommand(new NewSeriesRoot(ROOT1,
                new PrivateRecordingSpec("Show1", orp), 5000));

            m_eventScheduler.scheduleCommand(new AddSeason(BRANCH1, ROOT1,
                new PrivateRecordingSpec("Season1", orp), 
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 6000));

            m_eventScheduler.scheduleCommand(new AddEpisode(LEAF1, BRANCH1,
                    m_locator, now + start, duration,
                    ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 
                    7000, expiration));


            // Create the 3rd set of series recording
            m_eventScheduler.scheduleCommand(new NewSeriesRoot(ROOT2, 
                new PrivateRecordingSpec("Show2", orp), 9000));

            m_eventScheduler.scheduleCommand(new AddSeason(BRANCH2, ROOT2,
                new PrivateRecordingSpec("Season1", orp), 
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 10000));

            m_eventScheduler.scheduleCommand(new AddEpisode(LEAF2, BRANCH2,
                m_locator, now + start, duration,
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 
                11000, expiration));

            // verifications 

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(
                LEAF1, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 14000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(
                LEAF2, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 15000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(
                LEAF3, OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, 16000));

            m_eventScheduler.scheduleCommand(new CheckRUPriority(LEAF1, recPriorities, 18000));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(
                LEAF1, OcapRecordingRequest.IN_PROGRESS_STATE, start+15000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(
                LEAF2, OcapRecordingRequest.IN_PROGRESS_STATE, start+15100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(
                LEAF3, OcapRecordingRequest.PENDING_WITH_CONFLICT_STATE, start+15200));


            if (m_deleteByReq)
            {
                m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(ROOT1, start+18000));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new DeleteRecordedService(LEAF1, start+18000));
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF1, LeafRecordingRequest.DELETED_STATE, start+19000));
            }


            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF2, OcapRecordingRequest.IN_PROGRESS_STATE, start+20000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF3, OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, start+21000));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF2, OcapRecordingRequest.IN_PROGRESS_STATE, start3+15000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF3, OcapRecordingRequest.IN_PROGRESS_STATE, start3+16000));

            long trigger = start3+duration+15000;

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF2, OcapRecordingRequest.COMPLETED_STATE, trigger+100));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF3, OcapRecordingRequest.COMPLETED_STATE, trigger+200));

            // all rr should still be in the RecordingList
            if (m_deleteByReq)
            {
                m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT1, NOT_IN_LIST, trigger+1000));
                m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH1, NOT_IN_LIST, trigger+1100));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT1, IN_LIST, trigger+1000));
                m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH1, IN_LIST, trigger+1100));
            }
         
            if (m_deleteByReq)
            {
                // If deleted by request then RecordingRequest should not be present in the list
                m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF1, NOT_IN_LIST, trigger+1200));
            }
            else
            {
                // If deleted by Service then RecordingRequest should still be present in the list
                m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF1, IN_LIST, trigger+1200));
            } 
            
            m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT2, IN_LIST, trigger+1500));
            m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH2, IN_LIST, trigger+1300));
            m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF2, IN_LIST, trigger+1400));

            m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF3, IN_LIST, trigger+1600));
            m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH3, IN_LIST, trigger+1700));
            m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT3, IN_LIST, trigger+1800));

            // Delete roots
            if (!m_deleteByReq)
            {
                m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(ROOT1, trigger+3000));
            }
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(ROOT2, trigger+4500));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(ROOT3, trigger+4000));

            // Verify all recording requests created are deleted
            m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH1, NOT_IN_LIST, trigger+5000));
            m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF1, NOT_IN_LIST, trigger+5100));
            m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT1, NOT_IN_LIST, trigger+5200));

            m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH2, NOT_IN_LIST, trigger+5300));
            m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF2, NOT_IN_LIST, trigger+5400));
            m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT2, NOT_IN_LIST, trigger+5500));

            m_eventScheduler.scheduleCommand(new ValidateRequest(BRANCH3, NOT_IN_LIST, trigger+5600));
            m_eventScheduler.scheduleCommand(new ValidateRequest(LEAF3, NOT_IN_LIST, trigger+5700));
            m_eventScheduler.scheduleCommand(new ValidateRequest(ROOT3, NOT_IN_LIST, trigger+5800));

            m_eventScheduler.run(1000);

            if (!m_deletedLEAF1)
            {
                m_failed = TEST_FAILED;
                m_failedReason = LEAF1 +" never reached DELETED_STATE";
                System.out.println(m_failedReason);
            }

            // remove listenters and handlers
            rm.setRequestResolutionHandler(null);
            rm.removeRecordingChangedListener(this);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log(getName() + " Completed FAILED - " +m_failedReason);
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " Completed PASSED");
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.ocap.dvr.RequestResolutionHandler#requestResolution(org.ocap.
         * shared.dvr.RecordingRequest)
         */
        public void requestResolution(RecordingRequest request)
        {
        }

        public void recordingChanged(RecordingChangedEvent e)
        {
            RecordingRequest changedRR = e.getRecordingRequest(); 
            String changedRName = findKey(changedRR);

            if (changedRName == null) 
            {
                return;
            }
            if (changedRName == LEAF1 && 
                e.getState() == OcapRecordingRequest.DELETED_STATE)
            {
                m_deletedLEAF1 = true;
            }

            System.out.println("<<received recordingChangedEvent for "+changedRName +": newState = "+e.getState() +", oldState="+e.getOldState()); 

            //DvrEventPrinter.printRecordingChangedEvent(e);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestSeriesContention_delete" +(m_deleteByReq ? "ByReq" : "ByService");
        }

        private OcapLocator m_locator;
        private boolean m_deleteByReq;
        private boolean m_deletedLEAF1 = false;
    }


    public class CheckRUPriority extends NotifyShell
    {
        /**
         * @param time
         */
        CheckRUPriority(String rec, String[] rec_list, long time)
        {
            super(time);
            m_rec = rec;
            m_recList = rec_list;
        }

        public void ProcessCommand()
        {
            RecordingRequest rr;
            ResourceUsage[] ru;
            String recName = null;

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            // find the associated recording request to name
            rr = (RecordingRequest) findObject(m_rec);

            // get priortized list
            ru = rm.getPrioritizedResourceUsages(rr);

            if (ru.length != m_recList.length)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "<<<<<<< CheckRUPriority Failure - found " +ru.length +" resources for " +m_recList.length  +" recordings";
                System.out.println(m_failedReason);
                return;
            }

            for (int i = 0; i < ru.length; i++)
            {
                recName = getNameForResourceUsage(ru[i]);

                if (!recName.equals(m_recList[i]))
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "<<<CheckRUPriority - mismatch at " +i +": expected " +m_recList[i] +" not " +recName;
                    System.out.println(m_failedReason);
                    return;
                }
                System.out.println("<<<CheckRUPriority - ok for "+recName);
            }
        }

        String m_rec;

        String[] m_recList;
    }
}

