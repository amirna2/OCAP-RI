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
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.dvr.RequestResolutionHandler;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;

/**
 * @author jspruiel
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestSeriesReschedule extends DvrTest
{

    TestSeriesReschedule(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestParentReschedule((OcapLocator) m_locators.elementAt(0)));

        return tests;
    }

    /**
     * Tests the ParentRecordingRequest.reschedule method.
     */
    public class TestParentReschedule extends TestCase implements RequestResolutionHandler
    {
        TestParentReschedule(OcapLocator locator)
        {
            m_locator = locator;
        }

        /**
         * Executes the tests.
         */
        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            long expiration = 60 * 60 * 24;
            long startTime = now + MINUTE;
            long duration = 90*SEC;

            String ROOT = "RootShow";
            String BRANCH1 = "Branch_SeasonA";
            String BRANCH2 = "Branch_SeasonB";
            String LEAF1 = "Leaf_SeasonAEpisode1";
            String LEAF2 = "Leaf_SeasonAEpisode2";
            String LEAF3 = "Leaf_SeasonBEpisode1";


            reset();

            // Install the request resolution handler.
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            rm.setRequestResolutionHandler(this);

            OcapRecordingProperties orp = new OcapRecordingProperties(
                OcapRecordingProperties.HIGH_BIT_RATE,
                expiration, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, 
                null, null, null);

            // SHOW
            m_eventScheduler.scheduleCommand(new NewSeriesRoot(ROOT, 
                new PrivateRecordingSpec("PRS", orp), 500));

            // SEASON 1
            m_eventScheduler.scheduleCommand(new AddSeason(BRANCH1, ROOT,
                new PrivateRecordingSpec("PRS", orp), 
                ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 500*3));

            // EPISODE 1
            m_eventScheduler.scheduleCommand(new AddEpisode(LEAF1, BRANCH1, 
                m_locator, startTime, duration, 
                ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 500*4, expiration));
            // EPISODE 2
            m_eventScheduler.scheduleCommand(new AddEpisode(LEAF2, BRANCH1, 
                m_locator, startTime, duration, 
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 500*5, expiration));

            // The Series is now fully resolved. Now we perform the reschedule.

            // RESCHEDULE
            m_eventScheduler.scheduleCommand(new RescheduleParent(ROOT, 
                new PrivateRecordingSpec("PRS", orp), 500*6));

            // SEASON 2
            m_eventScheduler.scheduleCommand(new AddSeason(BRANCH2, ROOT,
                new PrivateRecordingSpec("PRS", orp), 
                ParentRecordingRequest.PARTIALLY_RESOLVED_STATE, 500*7));

            // EPISODE 1
            m_eventScheduler.scheduleCommand(new AddEpisode(LEAF3, BRANCH2, 
                m_locator, startTime, duration, 
                ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 500*8, expiration));

            // SHOW should have 2 SEASON children
            m_eventScheduler.scheduleCommand(new ValidateChildCount(ROOT, 2, 500*9));

            // SEASON 1 should have 2 EPISODES
            m_eventScheduler.scheduleCommand(new ValidateChildCount(BRANCH1, 2, 500*10));

            // SEASON 2 should have 1 EPISODE
            m_eventScheduler.scheduleCommand(new ValidateChildCount(BRANCH2, 1, 500*11));

            // DELETE the SHOW (the root) and everything should become deleted.
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(ROOT, 500*12));

            m_eventScheduler.run(3000);

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log(getName() + " Completed FAILED - " +m_failedReason);
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " Completed PASSED");
            }

            m_objects.removeAllElements();
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
            // Intentionally does nothing. However, the a
            // request resolution handler must be installed.
        }


        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestParentReschedule";
        }

        private OcapLocator m_locator;
    }

    /**
     * A command to perform reschedule on a parent recording.
     * 
     * @author jspruiel
     * 
     */
    class RescheduleParent extends EventScheduler.NotifyShell
    {

        public RescheduleParent(String recording, RecordingSpec rSpec, int triggerTime)
        {
            super(triggerTime);
            m_recording = recording;
            m_rSpec = rSpec;
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell#ProcessCommand
         * ()
         */
        public void ProcessCommand()
        {
            System.out.println("<<<<RescheduleParent::ProcessCommand>>>>");

            ParentRecordingRequest rr = (ParentRecordingRequest) findObject(m_recording);
            if (rr == null)
            {
                System.out.println("RescheduleParent:ProcessCommand failed to find " + m_recording);
            }
            try
            {
                rr.reschedule(m_rSpec);
            }
            catch (AccessDeniedException e)
            {
                System.out.println("RescheduleParent: exception");
                m_failed = TEST_FAILED;
                m_failedReason = "DVRUtils:RescheduleParent Failed - caught exception: " +e;
                System.out.println(m_failedReason);
            }

        }

        String m_recording;

        RecordingSpec m_rSpec;
    }

    class ValidateChildCount extends EventScheduler.NotifyShell
    {

        public ValidateChildCount(String recording, int count, int triggerTime)
        {
            super(triggerTime);
            m_recording = recording;
            m_count = count;

        }

        public void ProcessCommand()
        {
            System.out.println("<<<<ValidateChildCount ::ProcessCommand>>>>");

            ParentRecordingRequest rr = (ParentRecordingRequest) findObject(m_recording);
            RecordingList rList = rr.getKnownChildren();

            if (rList.size() != m_count)
            {
                m_failed = TEST_FAILED;
                m_failedReason = "ValidateChildCount invalid child count for " + m_recording;
                System.out.println(m_failedReason);
                return;
            }

            String data = new String(m_recording) + " children:";
            data = data + "\n";
            for (int i = 0; i < rList.size(); i++)
            {
                String name = (String) findKey(rList.getRecordingRequest(i));
                data = data + "    " + name + "\n";
            }
            DVRTestRunnerXlet.log(data);
        }

        String m_recording;

        int m_count;
    }
}
