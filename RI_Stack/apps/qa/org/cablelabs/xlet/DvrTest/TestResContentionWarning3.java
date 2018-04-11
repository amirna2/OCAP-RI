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
 * Created on Jul 18, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.RequestResolutionHandler;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordingRequest;

import org.cablelabs.xlet.DvrTest.DvrTest.AddEpisode;
import org.cablelabs.xlet.DvrTest.DvrTest.AddSeason;
import org.cablelabs.xlet.DvrTest.DvrTest.ConfirmRecordingReq_CheckState;
import org.cablelabs.xlet.DvrTest.DvrTest.DeleteRecordingRequest;
import org.cablelabs.xlet.DvrTest.DvrTest.NewSeriesRoot;
import org.cablelabs.xlet.DvrTest.DvrTest.ValidateRequest;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestResContentionWarning3 extends DvrTest
{

    /**
     * @param locators
     */
    TestResContentionWarning3(Vector locators)
    {
        super(locators);
        // TODO Auto-generated constructor stub
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestSeriesResContentionWarn((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * @author Ryan
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public class TestSeriesResContentionWarn extends TestCase implements ResourceContentionHandler,
            RequestResolutionHandler
    {
        private String[] recordings = new String[3];

        private boolean[] trigger = new boolean[3];

        static final boolean IN_LIST = true;

        static final boolean NOT_IN_LIST = false;

        private OcapLocator m_locator;

        /**
         * @param locator
         */
        public TestSeriesResContentionWarn(OcapLocator locator)
        {
            m_locator = locator;
            // TODO Auto-generated constructor stub
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestSeriesResContentionWarn";
        }

        public String getShortName()
        {
            return "<<< " + getName() + " >>>>";
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
            System.out.println("<<<Requesting Resolution being called>>>");
        }

        /**
         * Test Case
         */
        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long expiration = 60L * 60L * 24L; // 24 hour expiration

            // Create a vector list to resolve the names to
            recordings[0] = "Recording1";
            recordings[1] = "Recording2";
            recordings[2] = "Recording3";
            trigger[0] = false;
            trigger[1] = false;
            trigger[2] = false;

            // Clear the event scheduler
            reset();

            // Install the request resolution handler.
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
            rm.setRequestResolutionHandler(this);

            // Setup the resource contention manager
            ResourceContentionManager m_rcm = ResourceContentionManager.getInstance();
            m_rcm.setResourceContentionHandler(this);
            m_rcm.setWarningPeriod(30000);

            // inherited down to the leaf.
            OcapRecordingProperties orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE,
                    expiration, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                    OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, null, null, null);

            // Create the 1st root recording request.
            m_eventScheduler.scheduleCommand(new NewSeriesRoot("Show3", // name
                    new PrivateRecordingSpec("History Channel Series", orp), 1500));

            // Create the leaf recording request for this parent
            m_eventScheduler.scheduleCommand(new AddEpisode("Recording3", // name
                    "Show3", // parent's name
                    m_locator, now + 90000, // start time
                    60000, // duration
                    ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 1850, expiration));

            // Create the 2nd root recording request.
            m_eventScheduler.scheduleCommand(new NewSeriesRoot("Show1", // name
                    new PrivateRecordingSpec("Family Guy", orp), 4000));

            // Create the leaf recording request under the 2nd root
            m_eventScheduler.scheduleCommand(new AddEpisode("Recording1", // name
                    "Show1", // parent's name
                    m_locator, now + 90000, // start time
                    60000, // duration
                    ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 4300, expiration));

            // Create the 3rd root recording request
            m_eventScheduler.scheduleCommand(new NewSeriesRoot("Show2", // name
                    new PrivateRecordingSpec("Aqua Teen Hunger Force", orp), 7450));

            // Create the leaf recording request
            m_eventScheduler.scheduleCommand(new AddEpisode("Recording2", // name
                    "Show2", // parent's name
                    m_locator, now + 90000, // start time
                    60000, // duration
                    ParentRecordingRequest.COMPLETELY_RESOLVED_STATE, 7950, expiration));

            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    LeafRecordingRequest.COMPLETED_STATE, 190000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2",
                    LeafRecordingRequest.COMPLETED_STATE, 190010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3",
                    LeafRecordingRequest.FAILED_STATE, 190020));

            // Verify all recording requests created are are still in the
            // RecordingList
            m_eventScheduler.scheduleCommand(new ValidateRequest("Recording1", IN_LIST, 200010));
            m_eventScheduler.scheduleCommand(new ValidateRequest("Show1", IN_LIST, 200020));

            m_eventScheduler.scheduleCommand(new ValidateRequest("Recording2", IN_LIST, 200040));
            m_eventScheduler.scheduleCommand(new ValidateRequest("Show2", IN_LIST, 200050));

            m_eventScheduler.scheduleCommand(new ValidateRequest("Recording3", IN_LIST, 200070));
            m_eventScheduler.scheduleCommand(new ValidateRequest("Show3", IN_LIST, 200080));

            // Delete roots
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Show1", 220000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Show2", 221000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Show3", 222000));

            // Verify all recording requests created are deleted
            m_eventScheduler.scheduleCommand(new ValidateRequest("Recording1", NOT_IN_LIST, 250010));
            m_eventScheduler.scheduleCommand(new ValidateRequest("Show1", NOT_IN_LIST, 250020));

            m_eventScheduler.scheduleCommand(new ValidateRequest("Recording2", NOT_IN_LIST, 250040));
            m_eventScheduler.scheduleCommand(new ValidateRequest("Show2", NOT_IN_LIST, 250050));

            m_eventScheduler.scheduleCommand(new ValidateRequest("Recording3", NOT_IN_LIST, 250070));
            m_eventScheduler.scheduleCommand(new ValidateRequest("Show3", NOT_IN_LIST, 250080));

            m_eventScheduler.run(1000);

            m_rcm.setResourceContentionHandler(null);

            // remove listenters and handlers
            rm.setRequestResolutionHandler(null);

            for (int x = 0; x < 3; x++)
            {
                if (!(trigger[x]))
                {
                    m_failed = TEST_FAILED;
                }
            }

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log(getShortName() + DvrEventPrinter.Fail);
            }
            else
            {
                DVRTestRunnerXlet.log(getShortName() + DvrEventPrinter.Pass);
            }
        }

        public ResourceUsage[] resolveResourceContention(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {

            int i, j;

            DVRTestRunnerXlet.log("*** TESTS DvrTest contention handler called with:");

            DVRTestRunnerXlet.log("***  newRequest: " + getNameForResourceUsage(newRequest) + " (" + newRequest + ")");

            for (i = 0; i < currentReservations.length; i++)
            {
                DVRTestRunnerXlet.log("***  curRes[" + i + "]: " + getNameForResourceUsage(currentReservations[i])
                        + " (" + currentReservations[i] + ")");
            }

            ResourceUsage[] neworder = new ResourceUsage[currentReservations.length + 1];

            //
            // Perform insertion sort of each element
            //
            neworder[0] = newRequest;

            for (i = 0; i < currentReservations.length; i++)
            {
                String resKeyToInsert = getNameForResourceUsage(currentReservations[i]);

                for (j = i + 1; j > 0; j--)
                {
                    String sortResKey = getNameForResourceUsage(neworder[j - 1]);

                    if ((resKeyToInsert.compareTo(sortResKey) >= 0))
                    { // Stop - we hit the top or the entry below us is >=
                      // resToInsert
                        break;
                    }

                    // Assert: key for neworder[j] <= key for
                    // currentReservations[i]
                    // Move up the current entry - making a hole for the next
                    // interation
                    neworder[j] = neworder[j - 1];
                } // END for (j)

                // j will have stopped at the right place or hit the top
                // Either way, put it where it needs to go
                neworder[j] = currentReservations[i];
            } // END for (i)

            // Assert: neworder is sorted by key value (string name)

            DVRTestRunnerXlet.log("*** TESTS DvrTest contention handler returning prioritized list:");
            for (i = 0; i < neworder.length; i++)
            {
                DVRTestRunnerXlet.log("***  neworder[" + i + "]: " + getNameForResourceUsage(neworder[i]) + " ("
                        + neworder[i] + ")");
            }

            return neworder;
        } // END resolveResourceContention()

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.ocap.resource.ResourceContentionHandler#resourceContentionWarning
         * (org.ocap.resource.ResourceUsage, org.ocap.resource.ResourceUsage[])
         */
        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            synchronized (this)
            {
                DVRTestRunnerXlet.log("***  Warning called on: " + getNameForResourceUsage(newRequest) + " ("
                        + newRequest + ")");

                if (recordings[0].equals(getNameForResourceUsage(newRequest)))
                {
                    System.out.println("<<<<FIRST FLAG THROWN>>>>>");
                    trigger[0] = true;
                }
                else if (recordings[1].equals(getNameForResourceUsage(newRequest)))
                {
                    System.out.println("<<<<SECOND FLAG THROWN>>>>>");
                    trigger[1] = true;
                }
                else if (recordings[2].equals(getNameForResourceUsage(newRequest)))
                {
                    System.out.println("<<<<THIRD FLAG THROWN>>>>>");
                    trigger[2] = true;
                }
                else
                {
                    System.out.println("<<<<<NO FLAG THROWN>>>>>");
                }
            }
        }
    }
}
