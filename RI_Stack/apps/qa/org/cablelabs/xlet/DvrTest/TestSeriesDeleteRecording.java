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

import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.dvr.RequestResolutionHandler;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.DeletionDetails;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;

/**
 * @author jspruiel
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestSeriesDeleteRecording extends DvrTest
{
    private static final String ROOT = "Show";
    private static final String BRANCH = "Season";
    private static final String LEAF = "Episode";

    private static final boolean IN_LIST = true;
    private static final boolean NOT_IN_LIST = false;

    TestSeriesDeleteRecording(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestDeleteSeriesParent((OcapLocator) m_locators.elementAt(0), ROOT, false,
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE));
        tests.addElement(new TestDeleteSeriesParent((OcapLocator) m_locators.elementAt(0), ROOT, true,
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE));
/*
        tests.addElement(new TestDeleteSeriesParent((OcapLocator) m_locators.elementAt(0), ROOT, false,
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE));
        tests.addElement(new TestDeleteSeriesParent((OcapLocator) m_locators.elementAt(0), ROOT, true,
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE));
        tests.addElement(new TestDeleteSeriesParent((OcapLocator) m_locators.elementAt(0), ROOT, false,
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE));
        tests.addElement(new TestDeleteSeriesParent((OcapLocator) m_locators.elementAt(0), ROOT, true,
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE));
*/
        // Branch
        tests.addElement(new TestDeleteSeriesParent((OcapLocator) m_locators.elementAt(0), BRANCH, false,
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE));
        tests.addElement(new TestDeleteSeriesParent((OcapLocator) m_locators.elementAt(0), BRANCH, true,
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE));

        // Leaf
        tests.addElement(new TestDeleteSeriesLeaf((OcapLocator) m_locators.elementAt(0), false,
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE));
        tests.addElement(new TestDeleteSeriesLeaf((OcapLocator) m_locators.elementAt(0), true,
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE));
        tests.addElement(new TestDeleteSeriesLeaf((OcapLocator) m_locators.elementAt(0), false,
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE));
        tests.addElement(new TestDeleteSeriesLeaf((OcapLocator) m_locators.elementAt(0), true,
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 
            ParentRecordingRequest.PARTIALLY_RESOLVED_STATE));
        tests.addElement(new TestDeleteSeriesLeaf((OcapLocator) m_locators.elementAt(0), false,
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE));
        tests.addElement(new TestDeleteSeriesLeaf((OcapLocator) m_locators.elementAt(0), true,
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 
            ParentRecordingRequest.COMPLETELY_RESOLVED_STATE));

        return tests;
    }


    /**
     * Creates a heirarchial recording structure and delete either root or 
     * branche.
     * Varify all children are deleted as a result of root deletion.
     */
    public class TestDeleteSeriesParent extends TestCase implements RequestResolutionHandler
    {
        TestDeleteSeriesParent(OcapLocator locator, String parentName, boolean deletePostStart, int rootRezState, int parentRezState)
        {
            m_locator = locator;
            m_deletePostStart = deletePostStart;
            m_parentName = parentName;
            m_parentRezState = parentRezState;
            m_rootRezState = rootRezState;
        }

        public void runTest()
        {
            // Timings for User Deleted test.
            m_failed = TEST_PASSED;

            long now = System.currentTimeMillis();
            long expiration = 60*SEC;
            long start = 30*SEC;
            long duration = 30*SEC;

            System.out.println("Running " +getName());
            reset();

            // Install the request resolution handler.
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            rm.setRequestResolutionHandler(this);


            // inherited down to the LEAF.
            OcapRecordingProperties orp = new OcapRecordingProperties(
                OcapRecordingProperties.HIGH_BIT_RATE,
                expiration, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, 
                null, null, null);

            m_eventScheduler.scheduleCommand(new NewSeriesRoot(ROOT,
                new PrivateRecordingSpec("Show1", orp), 500)); 

            m_eventScheduler.scheduleCommand(new AddSeason(BRANCH, ROOT, 
                new PrivateRecordingSpec("Season1", orp), 
                m_rootRezState, 1000));

            m_eventScheduler.scheduleCommand(new AddEpisode(LEAF, BRANCH, 
                m_locator, now + start, duration, 
                m_parentRezState, 2000, expiration));
            

            long triggerTime = 5000;
            if (m_deletePostStart)
            {
                triggerTime = triggerTime + start;
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF, LeafRecordingRequest.IN_PROGRESS_STATE, triggerTime));
                triggerTime = triggerTime + 3000;

            }

            // Delete the recorded service of the recording request after start
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(m_parentName, triggerTime));

            triggerTime = triggerTime + 3000;
            m_eventScheduler.scheduleCommand(new ValidateParentDelete(m_parentName, triggerTime));


            m_eventScheduler.run(1000L);

            // Clean up 
            m_objects.removeAllElements();

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() + " INTERNAL ERROR - DISK FULL");
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

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDeleteSeriesParent-" +m_parentName +"_" +
                (m_deletePostStart ? "Post" : "Pre") +"LeafStart";

                /*
                + "(" +
                (m_rootRezState == ParentRecordingRequest.PARTIALLY_RESOLVED_STATE ? "PARTIAL_RESOLVED Root," : "COMPLETE_RESOLVED Root,") +
                (m_parentRezState == ParentRecordingRequest.PARTIALLY_RESOLVED_STATE ? "PARTIAL_RESOLVED Branch)" : "COMPLETE_RESOLVED Branch)");
                */
        }

        private OcapLocator m_locator;

        private boolean m_deletePostStart;
        private int m_rootRezState;
        private int m_parentRezState;
        private String m_parentName;
    }

    /**
     * Creates a heirarchial recording structure and delete the leaf.
     */
    public class TestDeleteSeriesLeaf extends TestCase implements RequestResolutionHandler
    {
        TestDeleteSeriesLeaf(OcapLocator locator, boolean deletePostStart, int rootRezState, int parentRezState)
        {
            m_locator = locator;
            m_deletePostStart = deletePostStart;
            m_rootRezState = rootRezState;
            m_parentRezState = parentRezState;
        }

        public void runTest()
        {
            // Timings for User Deleted test.
            m_failed = TEST_PASSED;

            long now = System.currentTimeMillis();
            long expiration = 60*SEC;
            long start = 30*SEC;
            long duration = 30*SEC;

            System.out.println("Running " +getName());
            reset();

            // Install the request resolution handler.
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            rm.setRequestResolutionHandler(this);


            // inherited down to the LEAF.
            OcapRecordingProperties orp = new OcapRecordingProperties(
                OcapRecordingProperties.HIGH_BIT_RATE,
                expiration, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, 
                null, null, null);

            m_eventScheduler.scheduleCommand(new NewSeriesRoot(ROOT,
                new PrivateRecordingSpec("Show1", orp), 500)); 

            m_eventScheduler.scheduleCommand(new AddSeason(BRANCH, ROOT, 
                new PrivateRecordingSpec("Season1", orp), 
                m_rootRezState, 1000));

            m_eventScheduler.scheduleCommand(new AddEpisode(LEAF, BRANCH, 
                m_locator, now + start, duration, 
                m_parentRezState, 2000, expiration));
            

            long triggerTime = 5000;
            if (m_deletePostStart)
            {
                triggerTime = triggerTime + start;
                m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(LEAF, LeafRecordingRequest.IN_PROGRESS_STATE, triggerTime));
                triggerTime = triggerTime + 3000;

            }

            // Delete the recorded service of the recording request after start
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(LEAF, triggerTime));

            triggerTime = triggerTime + 3000;
            m_eventScheduler.scheduleCommand(new ValidateLeafDelete(triggerTime));


            m_eventScheduler.run(1000L);

            // Clean up 
            m_objects.removeAllElements();

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() + " INTERNAL ERROR - DISK FULL");
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

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestDeleteSeriesLeaf_" +
                (m_deletePostStart ? "Post" : "Pre") +"LeafStart(" +
                (m_rootRezState == ParentRecordingRequest.PARTIALLY_RESOLVED_STATE ? "PARTIAL Root," : "COMPLETE Root,") +
                (m_parentRezState == ParentRecordingRequest.PARTIALLY_RESOLVED_STATE ? "PARTIAL Branch)" : "COMPLETE Branch)");
        }

        private OcapLocator m_locator;

        private boolean m_deletePostStart;
        private int m_rootRezState;
        private int m_parentRezState;
    }


    class ValidateParentDelete extends EventScheduler.NotifyShell
    {
        ValidateParentDelete(String rName, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_rName = rName;
        }

        public void ProcessCommand()
        {
            System.out.println();
            System.out.println("<<<<ValidateParentDelete::ProcessCommand>>>>");

            ParentRecordingRequest rr = (ParentRecordingRequest) findObject(m_rName);
            LeafRecordingRequest leafRR = (LeafRecordingRequest) findObject(LEAF);
            ParentRecordingRequest otherParentRR = (ParentRecordingRequest) findObject(BRANCH);
            if (m_rName.equals(BRANCH))
            {
                otherParentRR = (ParentRecordingRequest) findObject(ROOT);
            }

            if (rr == null || otherParentRR == null || leafRR == null) 
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateParentDelete FAILED - can't find recording";
                System.out.println(m_failedReason);
                return;
            }

            OcapRecordingManager orm = (OcapRecordingManager)OcapRecordingManager.getInstance();
            RecordingList rlist = orm.getEntries();
            if (rlist.contains(rr) || rlist.contains(leafRR))
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateParentDelete FAILED - recording should not be present";
                System.out.println(m_failedReason);
                return;
            }

            if (m_rName.equals(ROOT) && rlist.contains(otherParentRR))
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateParentDelete FAILED - " +BRANCH +" should not be present";
                System.out.println(m_failedReason);
                return;
            }  

            if (m_rName.equals(BRANCH) && !rlist.contains(otherParentRR))
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateParentDelete FAILED - " +ROOT +" should be present";
                System.out.println(m_failedReason);
                return;
            }  

            try 
            {
                int childrenCt = (rr.getKnownChildren()).size();

                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateParentDelete FAILED - an ISE should be thrown when calling getKnownChildren on" +m_rName;
                System.out.println(m_failedReason);
                return;
            }
            catch(IllegalStateException e) 
            {
                System.out.println("DVRUtils:ValidateParenDelete Correctly caught ISE when trying to call getKnownChildren on "+m_rName);
            }

            try 
            {
                int otherChildrenCt = (otherParentRR.getKnownChildren()).size();

                // calling getKnownChildren on otherParent(BRANCH) should 
                // throw ISE since BRANCH should have been deleted
                if (m_rName.equals(ROOT))
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "DVRUtils:ValidateParentDelete FAILED - branchRR.getKnownChildren() should thrown ISE because it should have been deleted";
                    System.out.println(m_failedReason);
                    return;
                }

                // if rr is BRANCH, then otherParent (ROOT) should have
                // no children since it's only child (BRANCH) was deleted
                if (otherChildrenCt != 0)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "DVRUtils:ValidateParentDelete FAILED - rootRR.getKnownChildren should have returned 0, instead it found " +otherChildrenCt;
                    System.out.println(m_failedReason);
                    return;
                }
            }
            catch (IllegalStateException ise)
            {
                if (m_rName.equals(BRANCH))
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "DVRUtils:ValidateParentDelete FAILED - rootRR.getKnownChildren() should have returned zero; instead caught exception: " +ise;
                    System.out.println(m_failedReason);
                    return;
                } 
                else
                {
                    System.out.println("DVRUtils:ValidateParenDelete correctly caught ISE when trying to call getKnownChildren on "+BRANCH);
                }
            }

            DeletionDetails dd = leafRR.getDeletionDetails();
            int reason = dd.getReason();
            if (reason != DeletionDetails.USER_DELETED)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateParentDelete Failed - leafRR deletionDetail reason should be USER_DELTED; instead got "+reason;
                System.out.println(m_failedReason);
            }
            System.out.println("DVRUtils:ValidateParenDelete DeleteDetails of Leaf recording correctly reflects USER_DELETED as the deletion reason of "+LEAF);
            
        }
        private String m_rName;
    }


    class ValidateLeafDelete extends EventScheduler.NotifyShell
    {
        ValidateLeafDelete(long taskTriggerTime)
        {
            super(taskTriggerTime);
        }

        public void ProcessCommand()
        {
            System.out.println();
            System.out.println("<<<<ValidateLeafDelete::ProcessCommand>>>>");

            ParentRecordingRequest rootRR = (ParentRecordingRequest) findObject(ROOT);
            LeafRecordingRequest leafRR = (LeafRecordingRequest) findObject(LEAF);
            ParentRecordingRequest branchRR = (ParentRecordingRequest) findObject(BRANCH);

            if (leafRR == null || branchRR == null || rootRR == null) 
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateLeafDelete FAILED - can't find recording";
                System.out.println(m_failedReason);
                return;
            }

            OcapRecordingManager orm = (OcapRecordingManager)OcapRecordingManager.getInstance();
            RecordingList rlist = orm.getEntries();
            if (!rlist.contains(rootRR) || !rlist.contains(branchRR))
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateLeafDelete FAILED - both parent recording (Root and Branch) should be present";
                System.out.println(m_failedReason);
                return;
            }

            if (rlist.contains(leafRR))
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateLeafDelete FAILED - " +LEAF +" should not be present";
                System.out.println(m_failedReason);
                return;
            }  

            try 
            {
                int rootChildrenCt = (rootRR.getKnownChildren()).size();
                int branchChildrenCt = (branchRR.getKnownChildren()).size();

                if (rootChildrenCt != 1 || branchChildrenCt != 0)
                {
                    m_failed = TEST_FAILED;
                    m_failedReason = "DVRUtils:ValidateLeafDelete FAILED - incorrect number of children: root=" +rootChildrenCt +" branch=" +branchChildrenCt;
                    System.out.println(m_failedReason);
                    return;
                }
            }
            catch(IllegalStateException e) 
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateLeafDelete FAILED - getKnownChildren() threw exception "+e;
                System.out.println(m_failedReason);
                return;
            }

            DeletionDetails dd = leafRR.getDeletionDetails();
            int reason = dd.getReason();
            if (reason != DeletionDetails.USER_DELETED)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:ValidateLeafDelete Failed - leafRR deletionDetail reason should be USER_DELTED; instead got "+reason;
                System.out.println(m_failedReason);
            }
            System.out.println("DVRUtils:ValidateParenDelete DeleteDetails of Leaf recording correctly reflects USER_DELETED as the deletion reason of "+LEAF);
            
        }
        private String m_rName;
    }
}
