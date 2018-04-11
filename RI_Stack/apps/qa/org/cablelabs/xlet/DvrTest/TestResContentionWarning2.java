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
 * Created on Mar 28, 2006
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
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingRequest;
import org.apache.log4j.*;

import org.cablelabs.xlet.DvrTest.DvrTest.DeleteRecordingRequest;
import org.cablelabs.xlet.DvrTest.DvrTest.Record;
import org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell;
import org.cablelabs.xlet.DvrTest.TestResContentionWarning.RecordA;
import org.cablelabs.xlet.DvrTest.TestResContentionWarning.Test_RCW_0;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestResContentionWarning2 extends DvrTest
{

    /**
     * @author Ryan
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public class TestRCW_SetWarningTimeInFuture extends TestCase implements ResourceContentionHandler
    {
        private Logger log;

        private OcapLocator m_locator;

        private OcapLocator m_locator2;

        private OcapLocator m_locator3;

        private int[] methodCallCount;

        /**
         * Constructor
         * 
         * @param locator
         * @param locator3
         * @param locator2
         */
        TestRCW_SetWarningTimeInFuture(OcapLocator locator, OcapLocator locator2, OcapLocator locator3)
        {
            m_locator = locator;
            m_locator2 = locator2;
            m_locator3 = locator3;
        }

        /**
         * Returns this test object.
         * 
         * @return
         */
        public Runnable getTest()
        {
            return this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#getName()
         */
        public String getName()
        {
            return "TesRCW - reset warning period to be in the future";
        }

        /**
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#runTest()
         */
        public void runTest()
        {
            log = Logger.getLogger("TesRCWFutureWarning");
            log.info("runTest executing.");

            m_rcm = ResourceContentionManager.getInstance();

            m_rcm.setResourceContentionHandler(this);
            m_rcm.setWarningPeriod(60000);

            m_failed = TEST_PASSED;
            methodCallCount = new int[3];

            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording1", m_locator, now + 100000L, 600000L, 500L)); 

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording2", m_locator2, now + 100000L, 600000L, 3000L));

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording3", m_locator3, now + 100000L, 600000L, 6000L));

            // Reset the Warning Period
            m_eventScheduler.scheduleCommand(new RescheduleWarning(30000, 50000));

            // Recording1 and 2 are timed to still be in pending state.
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 90000L));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 92000L));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 93000L));

            m_eventScheduler.run(5000);
            m_rcm.setResourceContentionHandler(null);

            int sum = methodCallCount[0] + methodCallCount[1] + methodCallCount[2];

            // The methodCallCount structure holds a counter, mapped to each
            // recording request. Each counter is mapped to one of three
            // recordings. The counter is incremented each time the
            // resourceContentionWarning is invoked for newRequest associated
            // with a recording.
            if ((sum != 6) || (methodCallCount[0] != 2) || (methodCallCount[1] != 2) || (methodCallCount[2] != 2))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Call count = " + sum + "\n" + " item[0,1,2] = " + methodCallCount[0] + ", "
                        + methodCallCount[1] + ", " + methodCallCount[2]);

                DVRTestRunnerXlet.log("TesRCW - reset warning period to be in the future: RCH called wrong number of times.");
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TesRCW - reset warning period to be in the future: completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TesRCW - reset warning period to be in the future  completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TesRCW - reset warning period to be in the future  completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.ocap.resource.ResourceContentionHandler#resolveResourceContention
         * (org.ocap.resource.ResourceUsage, org.ocap.resource.ResourceUsage[])
         */
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
                // Collects prints statements and prints them all at the end of
                // this method
                // buff = new StringBuffer();

                /*
                 * log.info(
                 * "---- Entered test's resourceContentionWarning Method -----\n"
                 * + "newReq = "+newRequest +"\n");
                 */
                log.info("---- Entered test's resourceContentionWarning Method -----\n" + "newReq = " + newRequest
                        + "\n");

                // reference recordings if any are null, the test is faulty or
                // something totally unexpected has occurred.
                RecordingRequest rec1 = (RecordingRequest) findObject("Recording1");
                RecordingRequest rec2 = (RecordingRequest) findObject("Recording2");
                RecordingRequest rec3 = (RecordingRequest) findObject("Recording3");
                if (rec1 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording1");
                    // log.info("could not find recordings.\n");
                    // DVRTestRunnerXlet.log(buff.toString());
                    return;
                }

                if (rec2 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording2");
                    // log.info("could not find recordings.\n");
                    // DVRTestRunnerXlet.log(buff.toString());
                    return;
                }

                if (rec3 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording");
                    // log.info("could not find recordings.\n");
                    // DVRTestRunnerXlet.log(buff.toString());
                    return;
                }

                if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec1))
                {
                    methodCallCount[0]++;
                }
                else if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec2))
                {
                    methodCallCount[1]++;
                }
                else if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec3))
                {
                    methodCallCount[2]++;
                }

                // Read each state to for validation below.
                int state1 = rec1.getState();
                int state2 = rec2.getState();
                int state3 = rec3.getState();

                // Print references so that we can map the objects when
                // examining the log.
                log.info("RecA = " + rec1 + "\n");
                log.info("RecB = " + rec2 + "\n");
                log.info("RecC = " + rec3 + "\n");

                // Print each recordings state in user-friendly fashion.
                log.info("state1 = " + DvrEventPrinter.xletLeafState(state1) + "\n");

                log.info("state2 = " + DvrEventPrinter.xletLeafState(state2) + "\n");

                log.info("state3 = " + DvrEventPrinter.xletLeafState(state3) + "\n");

                // Examine the actual state with the expected states.

                if (!((state1 == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)

                &&

                (state2 == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)

                &&

                (state3 == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)))
                {
                    m_failed = TEST_FAILED;
                    log.error("States incorrect:\n" + " expected state1 and state2 = PNC\n"
                            + " expected state3 = PWC... look at log above for states.\n");
                    return;
                }

                // validate the total actual current reservations match
                // the expected reservations.
                if (currentReservations.length != 2)
                {
                    m_failed = TEST_FAILED;
                    log.error("NG; Expected currentReservations length of 2 " + "actual = "
                            + currentReservations.length);
                }

                // Log properties associated with the new request.
                String[] resNames = newRequest.getResourceNames();
                log.info("newRequest has " + resNames.length + "' res names.\n");
            }
        }
    }

    public class TestRCW_SetWarningTimeInPast extends TestCase implements ResourceContentionHandler
    {
        private Logger log;

        private OcapLocator m_locator;

        private OcapLocator m_locator2;

        private OcapLocator m_locator3;

        private int[] methodCallCount;

        /**
         * Constructor
         * 
         * @param locator
         * @param locator3
         * @param locator2
         */
        TestRCW_SetWarningTimeInPast(OcapLocator locator, OcapLocator locator2, OcapLocator locator3)
        {
            m_locator = locator;
            m_locator2 = locator2;
            m_locator3 = locator3;
        }

        /**
         * Returns this test object.
         * 
         * @return
         */
        public Runnable getTest()
        {
            return this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#getName()
         */
        public String getName()
        {
            return "TesRCW - reset warning period to be in the past";
        }

        /**
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#runTest()
         */
        public void runTest()
        {
            log = Logger.getLogger("TesRCWPastWarning");
            log.info("runTest executing.");

            m_rcm = ResourceContentionManager.getInstance();

            m_rcm.setResourceContentionHandler(this);
            m_rcm.setWarningPeriod(20000);

            m_failed = TEST_PASSED;
            methodCallCount = new int[3];

            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording1", m_locator, now + 100000L, 600000L, 500L)); 

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording2", m_locator2, now + 100000L, 600000L, 3000L));

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording3", m_locator3, now + 100000L, 600000L, 6000L));

            // Reset the Warning Period
            m_eventScheduler.scheduleCommand(new RescheduleWarning(100000, 40000));

            // Recording1 and 2 are timed to still be in pending state.
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 90000L));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 92000L));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 93000L));

            m_eventScheduler.run(5000);
            m_rcm.setResourceContentionHandler(null);

            int sum = methodCallCount[0] + methodCallCount[1] + methodCallCount[2];

            // The methodCallCount structure holds a counter, mapped to each
            // recording request. Each counter is mapped to one of three
            // recordings. The counter is incremented each time the
            // resourceContentionWarning is invoked for newRequest associated
            // with a recording.
            if ((sum != 0) || (methodCallCount[0] != 0) || (methodCallCount[1] != 0) || (methodCallCount[2] != 0))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Call count = " + sum + "\n" + " item[0,1,2] = " + methodCallCount[0] + ", "
                        + methodCallCount[1] + ", " + methodCallCount[2]);

                DVRTestRunnerXlet.log("TesRCW - reset warning period to be in the future: RCH called wrong number of times.");
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() + " : completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + ":  FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + "  PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.ocap.resource.ResourceContentionHandler#resolveResourceContention
         * (org.ocap.resource.ResourceUsage, org.ocap.resource.ResourceUsage[])
         */
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
                // Collects prints statements and prints them all at the end of
                // this method
                // buff = new StringBuffer();

                /*
                 * log.info(
                 * "---- Entered test's resourceContentionWarning Method -----\n"
                 * + "newReq = "+newRequest +"\n");
                 */
                log.info("---- Entered test's resourceContentionWarning Method -----\n" + "newReq = " + newRequest
                        + "\n");

                // reference recordings if any are null, the test is faulty or
                // something totally unexpected has occurred.
                RecordingRequest rec1 = (RecordingRequest) findObject("Recording1");
                RecordingRequest rec2 = (RecordingRequest) findObject("Recording2");
                RecordingRequest rec3 = (RecordingRequest) findObject("Recording3");
                if (rec1 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording1");
                    // log.info("could not find recordings.\n");
                    // DVRTestRunnerXlet.log(buff.toString());
                    return;
                }

                if (rec2 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording2");
                    // log.info("could not find recordings.\n");
                    // DVRTestRunnerXlet.log(buff.toString());
                    return;
                }

                if (rec3 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording");
                    // log.info("could not find recordings.\n");
                    // DVRTestRunnerXlet.log(buff.toString());
                    return;
                }

                if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec1))
                {
                    methodCallCount[0]++;
                }
                else if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec2))
                {
                    methodCallCount[1]++;
                }
                else if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec3))
                {
                    methodCallCount[2]++;
                }

                // Read each state to for validation below.
                int state1 = rec1.getState();
                int state2 = rec2.getState();
                int state3 = rec3.getState();

                // Print references so that we can map the objects when
                // examining the log.
                log.info("RecA = " + rec1 + "\n");
                log.info("RecB = " + rec2 + "\n");
                log.info("RecC = " + rec3 + "\n");

                // Print each recordings state in user-friendly fashion.
                log.info("state1 = " + DvrEventPrinter.xletLeafState(state1) + "\n");

                log.info("state2 = " + DvrEventPrinter.xletLeafState(state2) + "\n");

                log.info("state3 = " + DvrEventPrinter.xletLeafState(state3) + "\n");

                // Examine the actual state with the expected states.

                if (!((state1 == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)

                &&

                (state2 == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)

                &&

                (state3 == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)))
                {
                    m_failed = TEST_FAILED;
                    log.error("States incorrect:\n" + " expected state1 and state2 = PNC\n"
                            + " expected state3 = PWC... look at log above for states.\n");
                    return;
                }

                // validate the total actual current reservations match
                // the expected reservations.
                if (currentReservations.length != 2)
                {
                    m_failed = TEST_FAILED;
                    log.error("NG; Expected currentReservations length of 2 " + "actual = "
                            + currentReservations.length);
                }

                // Log properties associated with the new request.
                String[] resNames = newRequest.getResourceNames();
                log.info("newRequest has " + resNames.length + "' res names.\n");
            }
        }
    }

    public class TestRCW_ResoveContentionBeforeWarning extends TestCase implements ResourceContentionHandler
    {
        private Logger log;

        private OcapLocator m_locator;

        private OcapLocator m_locator2;

        private OcapLocator m_locator3;

        private int[] methodCallCount;

        /**
         * Constructor
         * 
         * @param locator
         * @param locator3
         * @param locator2
         */
        TestRCW_ResoveContentionBeforeWarning(OcapLocator locator, OcapLocator locator2, OcapLocator locator3)
        {
            m_locator = locator;
            m_locator2 = locator2;
            m_locator3 = locator3;
        }

        /**
         * Returns this test object.
         * 
         * @return
         */
        public Runnable getTest()
        {
            return this;
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#getName()
         */
        public String getName()
        {
            return "TesRCW - delete recording before waring is fired";
        }

        /**
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#runTest()
         */
        public void runTest()
        {
            // Set up a simple configuration that logs on the console.
            // BasicConfigurator.configure();
            log = Logger.getLogger("TesRCWBeforeWarning");
            log.info("runTest executing.");

            m_rcm = ResourceContentionManager.getInstance();

            m_rcm.setResourceContentionHandler(this);
            m_rcm.setWarningPeriod(20000);

            m_failed = TEST_PASSED;
            methodCallCount = new int[3];

            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            //set up 3 recordings
            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording1", m_locator, now + 30000L, 90000L,   500L));                
            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording2", m_locator2, now + 30000L, 90000L, 3000L));
            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording3", m_locator3, now + 100000L, 60000L, 6000L));

            // Check the state of the recording of lowest priority
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1", LeafRecordingRequest.IN_PROGRESS_STATE, 50000));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2", LeafRecordingRequest.IN_PROGRESS_STATE, 50010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3", LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE, 50020));

            // Delete an ongoing recording
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 60000L));

            // Recordings should get reprioritized
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording2", LeafRecordingRequest.COMPLETED_STATE, 180010));
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording3", LeafRecordingRequest.COMPLETED_STATE, 180020));

            // Delete the completed Recordings
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 192000L));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 193000L));

            m_eventScheduler.run(5000);
            m_rcm.setResourceContentionHandler(null);

            int sum = methodCallCount[0] + methodCallCount[1] + methodCallCount[2];

            // The methodCallCount structure holds a counter, mapped to each
            // recording request. Each counter is mapped to one of three
            // recordings. The counter is incremented each time the
            // resourceContentionWarning is invoked for newRequest associated
            // with a recording.
            if ((sum != 0) || (methodCallCount[0] != 0) || (methodCallCount[1] != 0) || (methodCallCount[2] != 0))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Call count = " + sum + "\n" + " item[0,1,2] = " + methodCallCount[0] + ", "
                        + methodCallCount[1] + ", " + methodCallCount[2]);

                DVRTestRunnerXlet.log(getName() + " : RCH called wrong number of times.");
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() + " : TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " : FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " : PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * org.ocap.resource.ResourceContentionHandler#resolveResourceContention
         * (org.ocap.resource.ResourceUsage, org.ocap.resource.ResourceUsage[])
         */
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
                // Collects prints statements and prints them all at the end of
                // this method
                // buff = new StringBuffer();

                /*
                 * log.info(
                 * "---- Entered test's resourceContentionWarning Method -----\n"
                 * + "newReq = "+newRequest +"\n");
                 */
                log.info("---- Entered test's resourceContentionWarning Method -----\n" + "newReq = " + newRequest
                        + "\n");

                // reference recordings if any are null, the test is faulty or
                // something totally unexpected has occurred.
                RecordingRequest rec1 = (RecordingRequest) findObject("Recording1");
                RecordingRequest rec2 = (RecordingRequest) findObject("Recording2");
                RecordingRequest rec3 = (RecordingRequest) findObject("Recording3");
                if (rec1 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording1");
                    // log.info("could not find recordings.\n");
                    // DVRTestRunnerXlet.log(buff.toString());
                    return;
                }

                if (rec2 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording2");
                    // log.info("could not find recordings.\n");
                    // DVRTestRunnerXlet.log(buff.toString());
                    return;
                }

                if (rec3 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording");
                    // log.info("could not find recordings.\n");
                    // DVRTestRunnerXlet.log(buff.toString());
                    return;
                }

                if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec1))
                {
                    methodCallCount[0]++;
                }
                else if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec2))
                {
                    methodCallCount[1]++;
                }
                else if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec3))
                {
                    methodCallCount[2]++;
                }

                // Read each state to for validation below.
                int state1 = rec1.getState();
                int state2 = rec2.getState();
                int state3 = rec3.getState();

                // Print references so that we can map the objects when
                // examining the log.
                log.info("RecA = " + rec1 + "\n");
                log.info("RecB = " + rec2 + "\n");
                log.info("RecC = " + rec3 + "\n");

                // Print each recordings state in user-friendly fashion.
                log.info("state1 = " + DvrEventPrinter.xletLeafState(state1) + "\n");

                log.info("state2 = " + DvrEventPrinter.xletLeafState(state2) + "\n");

                log.info("state3 = " + DvrEventPrinter.xletLeafState(state3) + "\n");

                // Examine the actual state with the expected states.

                if (!((state1 == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)

                &&

                (state2 == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)

                &&

                (state3 == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)))
                {
                    m_failed = TEST_FAILED;
                    log.error("States incorrect:\n" + " expected state1 and state2 = PNC\n"
                            + " expected state3 = PWC... look at log above for states.\n");
                    return;
                }

                // validate the total actual current reservations match
                // the expected reservations.
                if (currentReservations.length != 2)
                {
                    m_failed = TEST_FAILED;
                    log.error("NG; Expected currentReservations length of 2 " + "actual = "
                            + currentReservations.length);
                }

                // Log properties associated with the new request.
                String[] resNames = newRequest.getResourceNames();
                log.info("newRequest has " + resNames.length + "' res names.\n");
            }
        }
    }

    /**
     * @author Ryan
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public class RescheduleWarning extends NotifyShell
    {

        private int m_warnTime;

        /**
         * @param time
         */
        RescheduleWarning(int warnTime, long triggerTime)
        {
            super(triggerTime);
            m_warnTime = warnTime;
            // TODO Auto-generated constructor stub
        }

        public void ProcessCommand()
        {
            try
            {
                m_rcm.setWarningPeriod(m_warnTime);
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                System.out.print("DVR Utils: Failure on rcm.setWarningPeriod");
            }
        }
    }

    /**
     * Schedules a future call to schedule a recording At the task's trigger
     * time, a call to RecordingManager.record will be made with the parameters
     * specified. The resulting recording will placed in the test's recording
     * map.
     */
    class RecordA extends Record
    {
        RecordA(Logger log, String recordingName, OcapLocator source, long startTime, long duration,
                long taskTriggerTime)
        {
            super(recordingName, source, startTime, duration, taskTriggerTime);
            m_log = log;
            m_source = new OcapLocator[1];
            m_source[0] = source;
            m_startTime = startTime;
            m_duration = duration;
            m_expiration = 1000 * 60 * 60 * 24; // 24 hour expiration
            m_recordingName = recordingName;
            m_retentionPriority = OcapRecordingProperties.DELETE_AT_EXPIRATION;
        }

        public void ProcessCommand()
        {
            OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();

            try
            {
                OcapRecordingRequest rr;
                LocatorRecordingSpec lrs;
                OcapRecordingProperties orp;
                m_log.info("DVRUtils: issuing recording:" + m_source[0] + " StartTime:" + m_startTime + " Duration:"
                        + m_duration + " retentionPriority = " + m_retentionPriority);

                orp = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, m_expiration,
                        m_retentionPriority, OcapRecordingProperties.RECORD_WITH_CONFLICTS, null, null, null);
                lrs = new LocatorRecordingSpec(m_source, new Date(m_startTime), m_duration, orp);

                m_defaultRecordingName = m_recordingName;

                rr = (OcapRecordingRequest) rm.record(lrs);

                m_defaultRecordingName = null;

                if (rr != null)
                {
                    insertObject(rr, m_recordingName);
                    m_log.debug("insertObject called");

                    m_log.debug("*****************************************************************");
                    m_log.debug("****" + m_recordingName + " scheduled as " + rr.toString() + "*****");
                    m_log.debug("*****************************************************************");

                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                m_failed = TEST_FAILED;
                m_log.error("DvrTest: Flagged FAILURE in Record due to rm.record() xception: " + e.toString());
                m_failedReason = "DvrTest: Flagged FAILURE in Record due to rm.record() xception: " + e.toString();
            }
        }

        private OcapLocator m_source[];

        private long m_startTime;

        private long m_duration;

        private long m_expiration;

        private String m_recordingName;

        private int m_retentionPriority;

        private Logger m_log;

        private long m_tZero;

    }

    /*
     * @param locators
     */
    TestResContentionWarning2(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestRCW_SetWarningTimeInFuture(
            (OcapLocator) m_locators.elementAt(0), 
            (OcapLocator) m_locators.elementAt(1), 
            (OcapLocator) m_locators.elementAt(2)));
        tests.addElement(new TestRCW_SetWarningTimeInPast(
            (OcapLocator) m_locators.elementAt(0), 
            (OcapLocator) m_locators.elementAt(1), 
            (OcapLocator) m_locators.elementAt(2)));
        tests.addElement(new TestRCW_ResoveContentionBeforeWarning(
            (OcapLocator) m_locators.elementAt(0), 
            (OcapLocator) m_locators.elementAt(1), 
            (OcapLocator) m_locators.elementAt(2)));
        return tests;
    }

    ResourceContentionManager m_rcm = null;
}
