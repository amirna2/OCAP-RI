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
 * Created on Aug 9, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Date;
import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.dvr.RequestResolutionHandler;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.DeletionDetails;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;

/**
 * @author jspruiel
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestSeriesRecordingExpiration extends DvrTest
{
    private static final String ROOT = "Show";
    private static final String BRANCH = "Season";
    private static final String LEAF1 = "Episode1";
    private static final String LEAF2 = "Episode2";

    /**
     * 
     * @param locators
     */
    public TestSeriesRecordingExpiration(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestSeriesRec_Season((OcapLocator) m_locators.elementAt(0), (OcapLocator)m_locators.elementAt(1), 24*60*60));
        tests.addElement(new TestSeriesRec_Season((OcapLocator) m_locators.elementAt(0), (OcapLocator)m_locators.elementAt(1), 90));
        tests.addElement(new TestSeriesRec_Season((OcapLocator) m_locators.elementAt(0), (OcapLocator)m_locators.elementAt(1), 60));
        tests.addElement(new TestSeriesRec_Season((OcapLocator) m_locators.elementAt(0), (OcapLocator)m_locators.elementAt(1), 30));
        return tests;
    }

    public class TestSeriesRec_Season extends TestCase implements RequestResolutionHandler
    {
        TestSeriesRec_Season(OcapLocator locator)
        {
            m_locator = locator;
            m_locator1 = locator;
        }

        TestSeriesRec_Season(OcapLocator loc, OcapLocator loc1, long expSec)
        {
            m_locator = loc;
            m_locator1 = loc1;
            m_expiration = expSec;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestSeriesRec_Season(expiration=" +m_expiration +"secs)";
        }

        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        public void runTest()
        {
            // get the current time
            m_failed = TEST_PASSED;
            System.out.println("Resetting m_failed to " + m_failed);

            long now = System.currentTimeMillis();
            long start = MINUTE;
            long duration = MINUTE;

            reset();

            // Install the request resolution handler.
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            rm.setRequestResolutionHandler(this);

            // inherited down to the leaf.
            OcapRecordingProperties orp = new OcapRecordingProperties(
                OcapRecordingProperties.HIGH_BIT_RATE,
                m_expiration, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, 
                null, null, null);

            // Create the root parent.
            m_eventScheduler.scheduleCommand(new NewSeriesRoot(ROOT,
                new PrivateRecordingSpec("Show1", orp), 500));

            // Create a child (of type parent) of the root
            m_eventScheduler.scheduleCommand(new AddSeason(BRANCH, ROOT,
                new PrivateRecordingSpec("Season1", orp), 
                ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 1000));

            m_eventScheduler.scheduleCommand(new AddEpisode(LEAF1, BRANCH,
                m_locator, now + start, duration, 
                ParentRecordingRequest.PARTIALLY_RESOLVED_STATE,
                2000, m_expiration));


            // Schedule a second leaf recording to be deleted in order to
            // check DeletionDetail USER_DELETED
            m_eventScheduler.scheduleCommand(new AddEpisode(LEAF2, BRANCH,
                m_locator1, now+start, duration, 
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE,
                2500, m_expiration));


            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF1, LeafRecordingRequest.IN_PROGRESS_STATE, start+6000));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF2, LeafRecordingRequest.IN_PROGRESS_STATE, start+6100));

            
            // Validate the leaf and parents are correct
            m_eventScheduler.scheduleCommand(new ValidateSeriesList(2, start+7500));

            // Delete CCR2Leaf leaf recording and validate it's DeletionDetail
            m_eventScheduler.scheduleCommand(new DeleteRecordedService(LEAF2, start+9000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF2, LeafRecordingRequest.DELETED_STATE, start+10000));
            m_eventScheduler.scheduleCommand(new ValidateDeleteReason(LEAF2, DeletionDetails.USER_DELETED, start+10500));

            // ------------------------------------------------------------//
            // Verification after recording is done 

            long triggerTime = start + duration + 3100;

            // Verify recording is in COMPLETED_STATE after it's end time
            // don't run this verification if expiration is prior to rec's end
            // because the rec would have been deleted before it completes
            if (m_expiration*SEC > duration)
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF1, LeafRecordingRequest.COMPLETED_STATE, triggerTime));
            }


            // ------------------------------------------------------------//
            // Verification after recording is done but before expiration
            // triggertime is half way between rec's end time and expiration
            triggerTime = start + duration + ((m_expiration*SEC - duration)>>1) + 3200;

            // don't wait for a very long expiration - test would take too long
            if (m_expiration > 180)
            {
                triggerTime = start + duration + 2*MINUTE;
            }

            // recording request should be in DELETED_STATE. 
            // don't run this verification if expiration is prior to rec's end
            // because the rec would have been deleted before it completes
            if (m_expiration*SEC > duration)
            {
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF1, LeafRecordingRequest.COMPLETED_STATE, triggerTime));

                // Validate the leaf and parents are correct
                m_eventScheduler.scheduleCommand(new ValidateSeriesList(2, triggerTime+100));
            }


            // ------------------------------------------------------------ //
            // Verification after expiration time is up

            // don't wait for a very long expiration - test would take too long
            if (m_expiration < 180)
            {
                triggerTime = start + m_expiration*SEC + 3300;

                // Validate that the leaf rr is in the deleted state
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF1, LeafRecordingRequest.DELETED_STATE, triggerTime));

                // Validate DeletionDetails reflects EXPIRED
                m_eventScheduler.scheduleCommand(new ValidateDeleteReason(LEAF1, DeletionDetails.EXPIRED, triggerTime+100));

                // Validate the leaf and parents are correct
                m_eventScheduler.scheduleCommand(new ValidateSeriesList(2, triggerTime+200));
            }


            // clean up
            // delete the recording hierarchy from root
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(ROOT, triggerTime + 1000L));

            rm.setRequestResolutionHandler(null);

            m_eventScheduler.run(1000L);

            m_objects.removeAllElements();


            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("<< " + getName() + " >> : TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log(getName() + " Completed FAILED - " +m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " Completed PASSED");
            }
        }

        public void requestResolution(RecordingRequest request)
        {
            // Intentionally does nothing. However, the a
            // request resolution handler must be installed.
        }

        private OcapLocator m_locator;
        private OcapLocator m_locator1;

        private long m_expiration = 24*60*60; 
    }


    class ValidateSeriesList extends EventScheduler.NotifyShell
    {
        ValidateSeriesList(int childrenCt, long taskTriggerTimer)
        {
            super(taskTriggerTimer);
            m_childrenCt = childrenCt;
        }

        public void ProcessCommand()
        {
            System.out.println();
            System.out.println("<<<<ValidateSeriesList::ProcessCommand>>>>");

            RecordingRequest rrRoot = (RecordingRequest) findObject(ROOT);
            RecordingRequest rrParent = (RecordingRequest) findObject(BRANCH);
            RecordingRequest rrLeaf1 = (RecordingRequest) findObject(LEAF1);
            RecordingRequest rrLeaf2 = (RecordingRequest) findObject(LEAF2);

            if (rrRoot == null || rrParent == null || rrLeaf1 == null || rrLeaf2 == null)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "<<ValidateSeriesList>> Recording was not found";
                System.out.println(m_failedReason);
                return;
            }

            OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            RecordingList rlist = orm.getEntries();

            if (!rlist.contains(rrRoot) || !rlist.contains(rrParent))
            {
                m_failed = TEST_FAILED;
                m_failedReason = "<<ValidateSeriesList>> ParentRecordingRequests missing from OcapRecordingManager.getEntries()";
                System.out.println(m_failedReason);
                return;

            }

            if (m_childrenCt == 0 &&
               (rlist.contains(rrLeaf2) || rlist.contains(rrLeaf1)))
            { 
                m_failed = TEST_FAILED;
                m_failedReason = "<<ValidateSeriesList>> no LeafRecordingRequests should be returned by OcapRecordingManager.getEntries()";
                System.out.println(m_failedReason);
            } 

            if (m_childrenCt == 1 &&
               (rlist.contains(rrLeaf2) || !rlist.contains(rrLeaf1)))
            { 
                m_failed = TEST_FAILED;
                m_failedReason = "<<ValidateSeriesList>> should only have a single leaf " +LEAF1;
                System.out.println(m_failedReason);
                return;
            } 

            if (!rlist.contains(rrLeaf2) && m_childrenCt == 2)
            if (m_childrenCt == 2 &&
               (!rlist.contains(rrLeaf2) || !rlist.contains(rrLeaf1)))
            { 
                m_failed = TEST_FAILED;
                m_failedReason = "<<ValidateSeriesList>> LeafRecordingRequests " +LEAF1 +" and/or "+LEAF2 +" missing from OcapRecordingManager.getEntries()";
                System.out.println(m_failedReason);
                return;
            } 


            if (!(rrRoot instanceof ParentRecordingRequest) || !(rrParent instanceof ParentRecordingRequest))
            {
                m_failed = TEST_FAILED;
                m_failedReason = "<<ValidateSeriesList>> Both " +ROOT +" and" +BRANCH +" should be ParentRecordingRequests";
                System.out.println(m_failedReason);
                return;
            }
            if (!(rrLeaf1 instanceof LeafRecordingRequest) ||
                !(rrLeaf2 instanceof LeafRecordingRequest))
            {
                m_failed = TEST_FAILED;
                m_failedReason = "<<ValidateSeriesList>> Both " +LEAF1 +" and" +LEAF2 +" should be LeafRecordingRequests";
                System.out.println(m_failedReason);
                return;
            }

            RecordingList rootChildrenList;
            RecordingList parentChildrenList;
            try
            {
                rootChildrenList = ((ParentRecordingRequest)rrRoot).getKnownChildren();
                parentChildrenList = ((ParentRecordingRequest)rrParent).getKnownChildren();
            }
            catch (IllegalStateException e)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "<<ValidateSeriesList>> unexpected IllegalStateException when calling getKnwonChildren() - "+e;
                System.out.println(m_failedReason);
                return;
            }

            if (rootChildrenList.size() != 1 || 
                !((RecordingRequest)rootChildrenList.getRecordingRequest(0)).equals(rrParent))
            {
                m_failed = TEST_FAILED;
                m_failedReason = "<<ValidateSeriesList>> Child of " +ROOT +" is incorrect";
                System.out.println(m_failedReason);
                return;
            }

            if (parentChildrenList.size() != m_childrenCt)
            { 
                m_failed = TEST_FAILED;
                m_failedReason = "<<ValidateSeriesList>> incorrect number of children for " +BRANCH +"; expected " +m_childrenCt +" got "+parentChildrenList.size();
                System.out.println(m_failedReason);
                return;
            } 

            for (int i = 0; i < m_childrenCt; i++) 
            {
                RecordingRequest childReq = (RecordingRequest)parentChildrenList.getRecordingRequest(i);

                if (!childReq.equals(rrLeaf1) && 
                    !childReq.equals(rrLeaf2) )
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "<<ValidateSeriesList>> Child(ren) of " +BRANCH +" is incorrect";
                    System.out.println(m_failedReason);
                    return;
                }
            }

            // all is well -  print recordings
            System.out.println("<<ValidateSeriesList>> all good");
            DvrEventPrinter.printRecording(rrRoot);
            DvrEventPrinter.printRecording(rrParent);
            DvrEventPrinter.printRecording(rrLeaf1);
            DvrEventPrinter.printRecording(rrLeaf2);
        }
        int m_childrenCt;
    }


    class ValidateDeleteReason extends EventScheduler.NotifyShell
    {
        ValidateDeleteReason(String recordingName, int delReason, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rName = recordingName;
            m_reason = delReason;
        }

        public void ProcessCommand()
        {
            System.out.println();
            System.out.println("<<<<ValidateDeleteReason::ProcessCommand>>>>");

            RecordingRequest rr = (RecordingRequest) findObject(m_rName);
            if (rr == null)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateDeleteReason FAILED - can't find " +m_rName;
                System.out.print(m_failedReason);
                return;
            }

            try
            {
                System.out.println("DVRUtils:ValidateDeleteReason issueing rr.getDeletionDetails()");
                DeletionDetails dd = ((LeafRecordingRequest) rr).getDeletionDetails();
                int reason = dd.getReason();

                if (reason != m_reason)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "DVRUtils:ValidateDeleteReason FAILED - expected DeletionDetails reason code: "
                        + DvrEventPrinter.xletDeletionDetailsReason(m_reason)
                        + "; got " +DvrEventPrinter.xletDeletionDetailsReason(reason);
                    System.out.println(m_failedReason);
                    return;
                }
                System.out.println("DVRUtils:ValidateDeleteReason ok, reason=" +reason);
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateDeleteReason FAILED - exception on rr.getDeletionDetails()!";
                System.out.println(m_failedReason);
                e.printStackTrace();
            }
        }

        private String m_rName;

        private int m_reason;
    }
}
