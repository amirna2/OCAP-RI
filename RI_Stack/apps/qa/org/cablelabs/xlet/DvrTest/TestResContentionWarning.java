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

import org.apache.log4j.Logger;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.SharedResourceUsage;
import org.ocap.dvr.TimeShiftBufferResourceUsage;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingRequest;

/**
 * A <code>ResourceContentionWarning</code> targeted <code>DvrTest</code> class
 * that defines one or more tests. <BR>
 * Additionaly, the tests are implicit requirements for Workset 40.
 * 
 * The ResourceContentionWarning algorithm must evaluate recordings based on
 * state, timing and priority.
 * 
 * A recording that is 'pending with conflict' with priority signifying
 * 'recording if no conflicts exists' will not cause a contention.
 * 
 * 
 * @author jspruiel
 */
public class TestResContentionWarning extends DvrTest
{
    public static final int RRU = 1;

    public static final int TSBRU = 2;

    /**
     * Constructor <code>TestResContentionWarning</code>
     * 
     * @param locators
     *            A list of valid locators.
     */
    TestResContentionWarning(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.xlet.DvrTest.DvrTest#getTests()
     */
    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new Test_RCW_0((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new Test_RCW_3((OcapLocator) m_locators.elementAt(0), (OcapLocator) m_locators.elementAt(1),
                (OcapLocator) m_locators.elementAt(2)));

        // tests.addElement(new
        // Test_RCW_4((OcapLocator)m_locators.elementAt(0)));
        tests.addElement(new Test_RCW_5((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new Test_RCW_6(m_locators));
        tests.addElement(new Test_RCW_7((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new Test_RCW_8((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * Verifies the ability to identify and deliver resource contention warnings
     * for tuner resources usages that are <B>unknown</B> to the
     * <code>NetworkInterfaceController</code>.<BR>
     * <BR>
     * 
     * Tuner Resources are not reserved for pending recordings.<BR>
     * <BR>
     * 
     * Expected Results: The implementation invokes the
     * ResourceContentionHandler.resourceContentionWarning three times; once for
     * each of the three recordings.<BR>
     * The states for Recordings 1 and 2 shall be pending no conflict while 3
     * shall be pending with conflict.
     * 
     * @author jspruiel
     */
    public class Test_RCW_0 extends TestCase implements ResourceContentionHandler
    {

        long m_gTZero;

        private Logger log;

        /**
         * Constructor
         * 
         * @param locator
         */
        Test_RCW_0(OcapLocator locator)
        {
            m_locator = locator;
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
            return "Test_RCW_0 (basic 2 pending; pri)";
        }

        /**
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#runTest()
         */
        public void runTest()
        {
            log = Logger.getLogger("Test_RCW_0");
            log.info("runTest executing.");

            m_failed = TEST_PASSED;
            methodCallCount = new int[3];

            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Each recordings is RECORD_WITH_CONFLICTS.
            m_eventScheduler.scheduleCommand(new RecordA(log, // logger
                    "Recording1", // recordingName
                    m_locator, // source
                    now + 100000L, // startTime
                    TEN_MINUTES, // duration
                    500L)); // taskTriggerTime

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording2", m_locator, now + 100000L, TEN_MINUTES,
                    1000L));

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording3", m_locator, now + 100000L, TEN_MINUTES,
                    2000L));

            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(this, 60000, 30000));

            // all recordins are scheduled to be pending.
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(log, "Recording1", 93000L));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(log, "Recording2", 94000L));

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(log, "Recording3", 95000L));

            m_eventScheduler.scheduleCommand(new RegisterResourceContentionHandler(null, 92000L));

            m_eventScheduler.run(5000);

            int sum = methodCallCount[0] + methodCallCount[1] + methodCallCount[2];

            // methodCallCount holds a counters corresponding to each
            // recording request. The counter is incremented each time the
            // resourceContentionWarning; each recording will become the
            // newRequest.
            if ((sum != 3) || (methodCallCount[0] != 1) || (methodCallCount[1] != 1) || (methodCallCount[2] != 1))
            {
                m_failed = TEST_FAILED;

                DVRTestRunnerXlet.log("TEST FAILURE Call count = " + sum + "\n" + " item[0,1,2] = "
                        + methodCallCount[0] + ", " + methodCallCount[1] + ", " + methodCallCount[2]);

                DVRTestRunnerXlet.log("Test-RCW-0 RCH reason for failure.");
            }
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("Test-RCW-0 completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("Test-RCW-0  completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("Test-RCW-0  completed: PASSED");
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
                log.info("----- Entered test's resourceContentionWarning Method -----\n" + "newReq = " + newRequest
                        + "\n");

                // validate the total actual current reservations match
                // the expected reservations.
                if (currentReservations.length != 2)
                {
                    m_failed = TEST_FAILED;
                    log.error(m_failedReason = "FAILED; Expected currentReservations length is 2 " + "actual length = "
                            + currentReservations.length);
                }

                RecordingRequest rec1, rec2, rec3 = null;
                if ((rec1 = (RecordingRequest) findObject("Recording1")) == null)
                {
                    m_failed = TEST_FAILED;
                    log.error(m_failedReason = "findObject did not find recording1");
                    return;
                }

                if ((rec2 = (RecordingRequest) findObject("Recording2")) == null)
                {
                    m_failed = TEST_FAILED;
                    log.error(m_failedReason = "findObject did not find recording2");
                    return;
                }

                if ((rec3 = (RecordingRequest) findObject("Recording3")) == null)
                {
                    m_failed = TEST_FAILED;
                    log.error(m_failedReason = "findObject did not find recording3");
                    return;
                }

                // Log ResourceUsage properties (resource names etc...)
                // informational purposes.
                if (newRequest instanceof RecordingResourceUsage)
                {
                    // Log the newRequest and its associated RecordingRequest.
                    RecordingResourceUsage rru = (RecordingResourceUsage) newRequest;

                    RecordingRequest rr = rru.getRecordingRequest();
                    log.info("newRequest RRU =  " + newRequest);
                    log.info("newRequest RR =  " + rr);
                }

                if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec1))
                {
                    log.info("newRequest RecName =  Recording1");
                    methodCallCount[0]++;
                }
                else if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec2))
                {
                    log.info("newRequest RecName =  Recording2");
                    methodCallCount[1]++;
                }
                else if (((RecordingResourceUsage) newRequest).getRecordingRequest().equals(rec3))
                {
                    log.info("newRequest RecName =  Recording3");
                    methodCallCount[2]++;
                }

                // -- Now handle the currentReservations array.--

                // Log the properties for each ResourceUsage in
                // the current reservations set (informational).
                log.info("\n" + "================= List of Current Reservations ================== ");
                log.info("Num RUsin CurrentReservations count = " + currentReservations.length);
                for (int i = 0; i < currentReservations.length; i++)
                {
                    ResourceUsage ru = currentReservations[i];
                    if (ru instanceof RecordingResourceUsage)
                    {
                        // Print helpful info.
                        RecordingResourceUsage rru = (RecordingResourceUsage) ru;
                        RecordingRequest rr = rru.getRecordingRequest();

                        log.info("currentReservation[" + i + "] = " + rru);
                        log.info("currentReservation[" + i + "].RecReq = " + rr);
                    }
                    else
                    {
                        log.error(m_failedReason = "FAILURE: Did not expect RU = " + ru);
                        m_failed = TEST_FAILED;
                    }

                    // Log properties associated with the new request.
                    String[] resNames = newRequest.getResourceNames();
                    log.info("newRequest has " + resNames.length + "' res names.");

                    for (int names_i = 0; names_i < resNames.length; names_i++)
                    {
                        String currName = resNames[names_i];
                        log.info(" newRequest ResourceName[" + names_i + "] = " + currName);
                    }
                    resNames = ru.getResourceNames();

                    // Print the list of names for this ResourceUsage
                    // Also print the ResourceProxy (informational).
                    for (int j = 0; j < resNames.length; j++)
                    {
                        String currName = resNames[j];

                        log.info(" ResourceName[" + j + "] = " + currName + "\n");
                        ResourceProxy rproxy = ru.getResource(currName);

                        // could be null for resources that are not currently
                        // known by the tuner.
                        if (rproxy != null)
                        {
                            ResourceClient rclient = rproxy.getClient();
                            if (rclient != null)
                            {
                                log.info("ResourceProxy = " + rproxy + "\n");
                                log.info("ResourceClient = " + rclient + "\n");
                            }
                        }
                    }// end-for
                }// end-for

                // Check the actual state with the expected states.
                if (!((rec1.getState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)

                &&

                (rec2.getState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)

                ||

                (rec3.getState() == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)))
                {
                    m_failed = TEST_FAILED;
                    log.error(m_failedReason = "Recording States incorrect:\n"
                            + " expected Recording1 and Recording2 = PNC\n" + " expected Recording3 = PWC.");

                    log.info("Recording1 = " + rec1 + "\nRecording1 = "
                            + DvrEventPrinter.xletLeafState(rec1.getState()));

                    log.info("Recording2 = " + rec2 + "\nRecording2 = "
                            + DvrEventPrinter.xletLeafState(rec2.getState()));

                    log.info("Recording3 = " + rec3 + "\nRecording3 = "
                            + DvrEventPrinter.xletLeafState(rec3.getState()));
                }
            }
        }// end resourceContentionWarning

        private OcapLocator m_locator;

        private int methodCallCount[] = null;

    }

    /**
     * Verifies the stack generates resource contention warnings for each of the
     * three pending recordings. Verifies the stack returns expected
     * ResourceUsages
     * 
     * This test differs from <code>TestWhenTwoAreInProgress</code> only by
     * prioritization of recordings.
     * 
     * Expected Results: The implementation invokes the
     * ResourceContentionHandler.resourceContentionWarning three times; once for
     * each of the three recordings.<BR>
     * The states for each recording is irrelevant.
     * 
     * @author jspruiel
     * 
     */
    public class Test_RCW_3 extends TestCase implements ResourceContentionHandler
    {
        /**
         * Constructor
         * 
         * @param locator
         */
        Test_RCW_3(OcapLocator locator1, OcapLocator locator2, OcapLocator locator3)
        {
            m_locator1 = locator1;
            m_locator2 = locator2;
            m_locator3 = locator3;

        }

        /**
         * Returns this test when called by the test framework.
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
            return "Test_RCW_3 (2 inProg; no pri)";
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#runTest()
         */
        public void runTest()
        {
            log = Logger.getLogger("Test_RCW_3");

            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            new RegisterResourceContentionHandler(this, 200L, 30000).ProcessCommand();

            // timed to be in progress.
            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording1", m_locator1, now + 30000L, TEN_MINUTES, 500L));

            // timed to be in progress.
            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording2", m_locator2, now + 30000L, TEN_MINUTES, 550L));

            // timed to be pending.
            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording3", m_locator3, now + 100000L, TEN_MINUTES,
                    560L));

            // This hack is to get around a bug in the stack.
            // The bug causes the RCW Handler to get invoked after the
            // recording is deleted.
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(log, "Recording1", 90000L)
            {
                public void ProcessCommand()
                {
                    super.ProcessCommand();
                    removeObject("Recording1");
                }
            });

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(log, "Recording2", 95000L)
            {
                public void ProcessCommand()
                {
                    super.ProcessCommand();
                    removeObject("Recording2");
                }

            });

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(log, "Recording3", 100000L)
            {
                public void ProcessCommand()
                {
                    super.ProcessCommand();
                    removeObject("Recording3");
                }
            });

            // m_eventScheduler.scheduleCommand(new
            // RegisterResourceContentionHandler(null, 110000L));
            m_eventScheduler.run(5000);
            new RegisterResourceContentionHandler(null, 110000L).ProcessCommand();
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("Test_RCW_3 completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("Test_RCW_3  completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("Test_RCW_3  completed: PASSED");
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
            return null;
        }

        /**
		 *
		 */
        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            synchronized (this)
            {
                log.info("-- Entered resourceContentionWarning --:" + "newReq = " + newRequest + "\n");

                // If the method is invoked because the newRequest was just
                // added,
                // then the thread in on which we are executing has not yet
                // returned
                // in order to add the <name, recording> pair to the DVRTest
                // hashtable.
                getNameForResourceUsage(newRequest);

                if (!(newRequest instanceof RecordingResourceUsage))
                {
                    log.error(m_failedReason = "newRequest NOT RecordingResourceUsage: = " + newRequest);
                    m_failed = TEST_FAILED;
                    return;
                }

                RecordingResourceUsage rru = (RecordingResourceUsage) newRequest;
                RecordingRequest rr = rru.getRecordingRequest();
                log.info("<newRequest RRU, RR> = <" + newRequest + ", " + rr + ">");

                RecordingRequest rec1 = null;
                rec1 = (RecordingRequest) findObject("Recording1");
                if (rec1 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error(m_failedReason = "findObject failed: recording1");
                    return;
                }
                log.info("Recording1= " + rec1 + "\nRecording1 = " + DvrEventPrinter.xletLeafState(rec1.getState()));

                RecordingRequest rec2 = (RecordingRequest) findObject("Recording2");
                if (rec2 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error(m_failedReason = "findObject failed: recording2");
                    return;
                }
                log.info("Recording2 = " + rec2 + "\nRecording2 = " + DvrEventPrinter.xletLeafState(rec2.getState()));

                RecordingRequest rec3 = (RecordingRequest) findObject("Recording3");
                if (rec3 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error(m_failedReason = "findObject failed: recording3");
                    return;
                }
                log.info("Recording3 = " + rec3 + "\nRecording3 = " + DvrEventPrinter.xletLeafState(rec3.getState()));

                if (currentReservations.length != 2)
                {
                    m_failed = TEST_FAILED;
                    log.error(m_failedReason = "Expected 2 SharedResourceUsages not " + currentReservations.length);
                    return;
                }

                if (rec2.getState() == LeafRecordingRequest.IN_PROGRESS_STATE
                        || rec2.getState() == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE)
                {
                    log.info("\n================= List of Current Reservations ================== ");
                    log.info("CurrRes count = " + currentReservations.length);
                    checkSharedUsages(log, currentReservations);
                }
                else
                {
                    log.info("Exiting resourceContentionWarning: NoOP as Recordings are Pending.");
                }
            }
        }// end resourceContentionWarning

        private Logger log;

        OcapLocator m_locator1;

        OcapLocator m_locator2;

        OcapLocator m_locator3;

        boolean deleted = false;

    }// end-class

    /**
     * <B>Purpose:</B><BR>
     * 
     * Verifies a ResourceContentionWarning is generated at the scheduling and
     * startTime of a recording.
     * 
     * 
     * Expected Results: Success when the test verifies that one recording is in
     * a an in-progress state while the other is pending-with-no-conflicts.
     * 
     * @author jspruiel
     */
    public class Test_RCW_8 extends TestCase implements ResourceContentionHandler, HelperIFace
    {
        /**
         * Constructor
         * 
         * @param locator
         */
        Test_RCW_8(OcapLocator locator)
        {
            m_locator = locator;
        }

        /**
         * Returns this test when called by the test framework.
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
            return "Test_RCW_8";
        }

        /**
		 */
        public void runTest()
        {
            log = Logger.getLogger("Test_RCW_8");

            m_failed = TEST_PASSED;
            // add the RCH and set warning period.
            ResourceContentionManager rcm = ResourceContentionManager.getInstance();

            rcm.setResourceContentionHandler(this);
            rcm.setWarningPeriod(100000);

            // clear the schedule of pending tasks
            reset();

            long now = System.currentTimeMillis();

            m_eventScheduler.scheduleCommand(new RecordA(log, // recordingName
                    "Recording1", // source
                    m_locator, // startTime
                    now + 30000L, // duration
                    TEN_MINUTES, 500L)); // taskTriggerTime

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording2", m_locator, now + 32000L, TEN_MINUTES, 550L));

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording3", m_locator, now + 200000L, TEN_MINUTES,
                    5000L));

            // Confirms ResourceContentionWarning receipt.
            m_eventScheduler.scheduleCommand(new CheckRCWReceipt(log, this, 110000L));

            // Confirms RecA and RecB states are in-progress and
            // state of RecC is pending with conflict.
            m_eventScheduler.scheduleCommand(new CheckState_1(log, 115000L));

            // Delete RecA
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(log, "Recording1", true, 120000L));

            // Confirme RecB is in-progress and RecC is pending without
            // conflict.
            m_eventScheduler.scheduleCommand(new CheckState_2(log, 150000L));

            // Cleanup RecB and RecC.
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(log, "Recording2", true, 155000L));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(log, "Recording3", true, 160000L));

            m_eventScheduler.run(5000);
            rcm.setResourceContentionHandler(null);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("Test_RCW_8 - delete a CurrReservation completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("Test_RCW_8 - delete a CurrReservation  completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("Test_RCW_8 - delete a CurrReservation  completed: PASSED");
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

            log.debug("*** TestRCW_8 contention handler called with:");

            log.debug("***  newRequest: " + getNameForResourceUsage(newRequest) + " (" + newRequest + ")");

            for (i = 0; i < currentReservations.length; i++)
            {
                log.debug("***  curRes[" + i + "]: " + getNameForResourceUsage(currentReservations[i]) + " ("
                        + currentReservations[i] + ")");
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

            log.debug("*** TESTS DvrTest contention handler returning prioritized list:");
            for (i = 0; i < neworder.length; i++)
            {
                log.debug("***  neworder[" + i + "]: " + getNameForResourceUsage(neworder[i]) + " (" + neworder[i]
                        + ")");
            }

            return neworder;
        }

        /**
         * Respond by cancelling one the currentReservations.
         * 
         */
        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            synchronized (this)
            {
                // buff = new StringBuffer();
                log.info("---- Entered test's resourceContentionWarning Method -----\n" + "newReq = " + newRequest
                        + "\n");

                String[] resNames = newRequest.getResourceNames();
                log.info("newRequest has " + resNames.length + " resource names elems.\n");

                RecordingRequest rec1 = (RecordingRequest) findObject("Recording1");
                RecordingRequest rec2 = (RecordingRequest) findObject("Recording2");

                if (rec1 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording1");
                    return;
                }

                if (rec2 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording2");
                    return;
                }

                if (newRequest instanceof RecordingResourceUsage)
                {
                    // Print helpful info.
                    RecordingResourceUsage rru = (RecordingResourceUsage) newRequest;
                    RecordingRequest rr = rru.getRecordingRequest();
                    log.info("newRequest RRU =  " + newRequest + "\n");
                    log.info("newRequest RR =  " + rr + "\n");

                    if (rr.equals(rec1) || rr.equals(rec2))
                    {
                        return;
                    }
                    else
                    {
                        // set flag to indicate waring call was received for
                        // Recording3.
                        m_rcwReceived = true;

                        int state1 = rec1.getState();
                        int state2 = rec2.getState();
                        int state3 = rr.getState();
                        log.info("RecA = " + rec1);
                        log.info("RecB = " + rec2);
                        log.info("RecC = " + rr);
                        log.info("state1 = " + DvrEventPrinter.xletLeafState(state1));
                        log.info("state2 = " + DvrEventPrinter.xletLeafState(state2));
                        log.info("state3 = " + DvrEventPrinter.xletLeafState(state3));

                        if (!((state1 == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE)
                                || (state1 == LeafRecordingRequest.IN_PROGRESS_STATE)
                                && (state2 == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE) || (state2 == LeafRecordingRequest.IN_PROGRESS_STATE)))
                        {
                            m_failed = TEST_FAILED;
                            log.info("Recordings A and B should be in progress, test pre-conditions are not "
                                    + " as expected.\n");
                            return;
                        }

                        if (state3 != LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)
                        {
                            m_failed = TEST_FAILED;
                            log.info("Not good; this recording should be pending with conflict.\n");
                            return;
                        }
                    }
                }

                // list resource names for newRequest
                for (int i = 0; i < resNames.length; i++)
                {
                    String currName = resNames[i];
                    log.info("newRequest ResourceName[" + i + "] = " + currName + "\n");
                }

                // -- Now handle the currentReservations array.--

                // Log the properties for each ResourceUsage in
                // the current reservations set (informational).
                log.info("\n" + "================= List of Current Reservations ================== ");
                log.info("CurrRes count = " + currentReservations.length + "\n");
                for (int i = 0; i < currentReservations.length; i++)
                {
                    ResourceUsage ru = currentReservations[i];
                    if (ru instanceof RecordingResourceUsage)
                    {
                        RecordingResourceUsage rru = (RecordingResourceUsage) ru;
                        RecordingRequest rr = rru.getRecordingRequest();
                        log.info("elem[" + i + "] - RRU = " + rru + "\n");
                        log.info("whose RR = " + rr + "\n");
                    }

                    resNames = ru.getResourceNames();

                    // Print the list of names for this ResourceUsage
                    // Also print the ResourceProxy
                    for (int j = 0; j < resNames.length; j++)
                    {
                        String currName = resNames[j];
                        log.info("ResourceName[" + j + "] = " + currName + "\n");
                        ResourceProxy rproxy = ru.getResource(currName);

                        // could be null for resource that are not currently
                        // known by the tuner.
                        if (rproxy != null)
                        {
                            ResourceClient rclient = rproxy.getClient();
                            if (rclient != null)
                            {
                                log.info("ResourceProxy = " + rproxy + "\n");
                                log.info("ResourceClient = " + rclient + "\n");
                            }
                        }
                    }// end-for
                }// end-for

                // validate the total currentReservation set match
                // the expected reservations set. Did this last because,
                // so that the logging above is not short circuited.
                if (currentReservations.length != 2)
                {
                    log.info("NG; Expected currentReservations length of 2 " + "actual = " + currentReservations.length);
                    m_failed = TEST_FAILED;
                }
            }// end-sync
        }// end-meth

        public synchronized boolean getRcwReceived()
        {
            return m_rcwReceived;
        }

        private Logger log;

        private boolean m_rcwReceived = false;

        private OcapLocator m_locator;
    }// end-class

    /**
     * Verifies the implementation invokes the
     * <code>ResourceContentionHandler.resourceContentionWarning()</code> method
     * during processing of the
     * <code>RecordingManager.record()<\code> request when the
     * walk-clock time is between the warning period and the start time of the
     * recording.<BR><BR>
     * 
     *  <B>Expected Results:</B><BR>
     *  The implementation invokes the test's
     *  <code>ResourceContentionHandler.resourceContentionWarning()</code> method during scheduling of the
     * recording. The test also validates that two recordings are in progress
     * and the third is pending with conflict state.
     * 
     * @author jspruiel
     */
    public class Test_RCW_7 extends TestCase implements ResourceContentionHandler, HelperIFace
    {
        private Logger log;

        long m_gTZero;

        /**
         * Constructor
         * 
         * @param locator
         */
        Test_RCW_7(OcapLocator locator)
        {
            m_locator = locator;
        }

        /**
         * Returns this test when called by the test framework.
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
            return "Test_RCW_7";
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#runTest()
         */
        public void runTest()
        {
            log = Logger.getLogger("Test_RCW_7");
            log.info("runTest executing.");

            // sp = new SPrint(m_gTZero = System.currentTimeMillis());
            m_failed = TEST_PASSED;

            // add the RCH
            ResourceContentionManager rcm = ResourceContentionManager.getInstance();

            rcm.setResourceContentionHandler(this);
            rcm.setWarningPeriod(110000);

            long now = System.currentTimeMillis();
            m_startTime = now;

            // clear the schedule of pending tasks
            reset();

            m_eventScheduler.scheduleCommand(new RecordA(log, // recording name
                    "Recording1", // source
                    m_locator, // start time
                    now + 30000L, // duration
                    TEN_MINUTES, 500L)); // trigger time

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording2", m_locator, now + 32000L, TEN_MINUTES, 550L));

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording3", m_locator, // start
                                                                                       // time
                    now + 120000L + 80000L, // duration
                    TEN_MINUTES, 120000L)); // trigger in 120s

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 182000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 183000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 184000));

            m_eventScheduler.run(5000);
            rcm.setResourceContentionHandler(null);
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("Test_RCW_7.  During Scheduling. completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("Test_RCW_7.  During Scheduling.  completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("Test_RCW_7.  During Scheduling.  completed: PASSED");
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
        }

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
                // buff = new StringBuffer();

                log.info("---- Entered test's resourceContentionWarning Method -----\n" + "newReq = " + newRequest
                        + "\n");

                String[] resNames = newRequest.getResourceNames();

                log.info("newRequest has " + resNames.length + " resource names elems.\n");

                RecordingRequest rec1 = (RecordingRequest) findObject("Recording1");
                RecordingRequest rec2 = (RecordingRequest) findObject("Recording2");
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

                if (newRequest instanceof RecordingResourceUsage)
                {
                    // Print helpful info.
                    RecordingResourceUsage rru = (RecordingResourceUsage) newRequest;
                    RecordingRequest rr = rru.getRecordingRequest();

                    log.info("newRequest RRU =  " + newRequest + "\n");
                    log.info("newRequest RR =  " + rr + "\n");
                    if (rr.equals(rec1) || rr.equals(rec2))
                    {
                        return;
                    }
                    else
                    {
                        m_rcwReceived = true;

                        int state1 = rec1.getState();
                        int state2 = rec2.getState();
                        int state3 = rr.getState();
                        log.info("RecA = " + rec1 + "\n");
                        log.info("RecB = " + rec2 + "\n");
                        log.info("RecC = " + rr + "\n");
                        log.info("state1 = " + DvrEventPrinter.xletLeafState(state1) + "\n");
                        log.info("state2 = " + DvrEventPrinter.xletLeafState(state2) + "\n");
                        log.info("state3 = " + DvrEventPrinter.xletLeafState(state3) + "\n");

                        // Confirm expected to actual states.

                        if (!(((state1 == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE) || (state1 == LeafRecordingRequest.IN_PROGRESS_STATE))
                                && ((state2 == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE) || (state2 == LeafRecordingRequest.IN_PROGRESS_STATE)) && (state3 == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)))
                        {
                            m_failed = TEST_FAILED;
                            log.info("Recordings A and B should be in progress, test pre-conditions are not "
                                    + " as expected.\n");
                            // DVRTestRunnerXlet.log(buff.toString());
                            return;
                        }

                        if (state3 != LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)
                        {
                            m_failed = TEST_FAILED;
                            log.info("Not good; this recording should be pending with conflict.\n");
                            // DVRTestRunnerXlet.log(buff.toString());
                            return;
                        }
                    }
                }

                // for this newRequest list resource names
                for (int i = 0; i < resNames.length; i++)
                {
                    String currName = resNames[i];
                    log.info("newRequest ResourceName[" + i + "] = " + currName + "\n");
                }

                // -- Now handle the currentReservations array.--

                // Log the properties for each ResourceUsage in
                // the current reservations set (informational).
                log.info("\n" + "================= List of Current Reservations ================== ");
                log.info("CurrRes count = " + currentReservations.length + "\n");
                for (int i = 0; i < currentReservations.length; i++)
                {
                    ResourceUsage ru = currentReservations[i];
                    if (ru instanceof RecordingResourceUsage)
                    {
                        // Print helpful info.
                        RecordingResourceUsage rru = (RecordingResourceUsage) ru;
                        RecordingRequest rr = rru.getRecordingRequest();
                        log.info("elem[" + i + "] - RRU = " + rru + "\n");
                        log.info("whose RR = " + rr + "\n");
                    }

                    resNames = ru.getResourceNames();

                    // Print the list of names for this ResourceUsage
                    // Also print the ResourceProxy
                    for (int j = 0; j < resNames.length; j++)
                    {
                        String currName = resNames[j];
                        log.info("ResourceName[" + j + "] = " + currName + "\n");
                        ResourceProxy rproxy = ru.getResource(currName);
                        if (rproxy != null)
                        {
                            ResourceClient rclient = rproxy.getClient();
                            if (rclient != null)
                            {
                                log.info("ResourceProxy = " + rproxy + "\n");
                                log.info("ResourceClient = " + rclient + "\n");
                            }
                        }
                    }// end-for
                }// end-for

                // validate the total currentReservation set match
                // the expected reservations set. Did this last because,
                // so that the logging above is not short circuited.
                if (currentReservations.length != 2)
                {
                    log.info("NG; Expected currentReservations length of 2 " + "actual = " + currentReservations.length);
                    m_failed = TEST_FAILED;
                }
                // DVRTestRunnerXlet.log(buff.toString());
            }
        }// end-meth

        public boolean getRcwReceived()
        {
            return m_rcwReceived;
        }

        private boolean m_rcwReceived = false;

        private long m_startTime;

        private OcapLocator m_locator;
    }// end-class

    /**
     * Verifies the implementation detects a contention based on implicit
     * resource usage and explicit resource usage contention. The test invokes
     * one of the overloaded NetworkInterfaceController.reserve methods to cause
     * the implementation to create an implicitly reserved resource usage.
     * 
     * @author jspruiel
     */
    public class Test_RCW_6 extends TestCase implements ResourceContentionHandler, HelperIFace
    {
        private Logger log;

        long m_gTZero;

        /**
         * Constructor
         * 
         * @param locator
         */
        Test_RCW_6(Vector locator)
        {
            m_locators = locator;
        }

        /**
         * Returns this test.
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
            return "Test_RCW_6";
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#runTest()
         */
        public void runTest()
        {
            log = Logger.getLogger("Test_RCW_6");
            log.info("runTest executing.");

            // sp = new SPrint(m_gTZero = System.currentTimeMillis());
            m_failed = TEST_PASSED;

            // add the RCH
            ResourceContentionManager rcm = ResourceContentionManager.getInstance();

            rcm.setResourceContentionHandler(this);
            rcm.setWarningPeriod(20000);

            //
            // Init the Network Interface
            initSC();
            // clear the schedule of pending tasks
            reset();

            long now = System.currentTimeMillis();
            m_startTime = now;

            m_eventScheduler.scheduleCommand(new TuneNetworkInterface("Ni1", (OcapLocator) m_locators.elementAt(0),
                    500L));

            m_eventScheduler.scheduleCommand(new TuneNetworkInterface("Ni2", (OcapLocator) m_locators.elementAt(1),
                    1000L));

            m_eventScheduler.scheduleCommand(new RecordA(log, // recordingName
                    "Recording1", // source
                    (OcapLocator) m_locators.elementAt(0), // startTime
                    now + 60000L, // duration
                    TEN_MINUTES, 1500L)); // taskTriggerTime

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 55000L));

            m_eventScheduler.scheduleCommand(new ReleaseNetworkInterface("Ni1", true, 58000L));

            m_eventScheduler.scheduleCommand(new ReleaseNetworkInterface("Ni2", true, 61000L));

            m_eventScheduler.run(5000L);

            rcm.setResourceContentionHandler(null);
            // Stop and destroy the SC and stop and detach the TSB
            cleanSC();

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("Test_RCW_6. completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("Test_RCW_6.  completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("Test_RCW_6.  completed: PASSED");
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
        }

        /**
         * Respond by cancelling on the currentReservations.
         * 
         */
        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            synchronized (this)
            {
                // buff = new StringBuffer();

                log.info("---- Entered test's resourceContentionWarning Method -----\n" + "newReq = " + newRequest
                        + "\n");

                String[] resNames = newRequest.getResourceNames();

                log.info("newRequest has " + resNames.length + " resource names elems.\n");

                RecordingRequest rec1 = (RecordingRequest) findObject("Recording1");
                if (rec1 == null)
                {
                    log.info("Could not find recording.\n");
                    // DVRTestRunnerXlet.log(buff.toString());
                    return;
                }

                if (newRequest instanceof RecordingResourceUsage)
                {
                    // Print helpful info.
                    RecordingResourceUsage rru = (RecordingResourceUsage) newRequest;
                    RecordingRequest rr = rru.getRecordingRequest();

                    log.info("newRequest RRU =  " + newRequest + "\n");
                    log.info("newRequest RR =  " + rr + "\n");
                    m_rcwReceived = true;

                    int state1 = rec1.getState();
                    log.info("RecA = " + rec1 + "\n");
                    log.info("state1 = " + DvrEventPrinter.xletLeafState(state1) + "\n");
                }

                // for this newRequest list resource names
                for (int i = 0; i < resNames.length; i++)
                {
                    String currName = resNames[i];
                    log.info("newRequest ResourceName[" + i + "] = " + currName + "\n");
                }

                // -- Now handle the currentReservations array.--

                // Log the properties for each ResourceUsage in
                // the current reservations set (informational).
                log.info("\n" + "================= List of Current Reservations ================== ");
                log.info("CurrRes count = " + currentReservations.length + "\n");
                for (int i = 0; i < currentReservations.length; i++)
                {
                    ResourceUsage ru = currentReservations[i];
                    log.info("CurrentReservation[" + i + "] = " + ru);
                    if (ru instanceof RecordingResourceUsage)
                    {
                        // Print helpful info.
                        RecordingResourceUsage rru = (RecordingResourceUsage) ru;
                        RecordingRequest rr = rru.getRecordingRequest();
                        log.info("elem[" + i + "] - RRU = " + rru + "\n");
                        log.info("whose RR = " + rr + "\n");
                    }

                    resNames = ru.getResourceNames();

                    // Print the list of names for this ResourceUsage
                    // Also print the ResourceProxy
                    for (int j = 0; j < resNames.length; j++)
                    {
                        String currName = resNames[j];
                        log.info("ResourceName[" + j + "] = " + currName + "\n");
                        ResourceProxy rproxy = ru.getResource(currName);
                        if (rproxy != null)
                        {
                            ResourceClient rclient = rproxy.getClient();
                            if (rclient != null)
                            {
                                log.info("ResourceProxy = " + rproxy + "\n");
                                log.info("ResourceClient = " + rclient + "\n");
                            }
                        }
                    }// end-for
                }// end-for
                // DVRTestRunnerXlet.log(buff.toString());
            }
        }// end-meth

        public boolean getRcwReceived()
        {
            return m_rcwReceived;
        }

        private long m_startTime;

        private boolean m_rcwReceived = false;

        // private StringBuffer buff = null;
        private Vector m_locators;
    }// end-class

    // =================== CUSTOM COMMANDS Classes ==========================

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

    class DeleteRecordingRequestExt extends DeleteRecordingRequest
    {

        DeleteRecordingRequestExt(Logger log, String rec, long taskTriggerTime)
        {
            super(rec, taskTriggerTime);
            m_rName = rec;
            m_log = log;
        }

        public void ProcessCommand()
        {

            m_log.info("\n<<<<DeleteRecordingRequest::ProcessCommand>>>>");
            try
            {
                RecordingRequest rr = (RecordingRequest) findObject(m_rName);

                if (rr == null)
                {
                    m_failed = TEST_FAILED;
                    m_log.error("DVRUtils:DeleteRecordingRequest:" + DvrEventPrinter.FindObjFailed + m_rName);
                    m_failedReason = "DeleteRecordingRequest:" + DvrEventPrinter.FindObjFailed + m_rName;
                    return;
                }

                m_log.info("DVRUtils:DeleteRecordingRequest:issueing rr.delete()");
                rr.delete();
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                m_log.error("DVRUtils:DeleteRecordingRequest:failed exception on rr.delete()");
                m_failedReason = "DeleteRecordingRequest:failed exception on rr.delete(). Exception: " + e.getMessage();
                e.printStackTrace();
            }
        }

        private Logger m_log;

        private String m_rName;
    }

    class CheckRCWReceipt extends EventScheduler.NotifyShell
    {
        HelperIFace test;

        CheckRCWReceipt(HelperIFace iface, long taskTriggerTime)
        {
            super(taskTriggerTime);
            test = iface;
        }

        CheckRCWReceipt(Logger logger, HelperIFace iface, long taskTriggerTime)
        {
            super(taskTriggerTime);
            test = iface;
            m_log = logger;
        }

        public void ProcessCommand()
        {

            m_log.debug("<<<<CheckRCWReceipt::ProcessCommand>>>>");

            if (test.getRcwReceived() == false)
            {
                m_failed = TEST_FAILED;
                return;
            }
            m_log.debug("CheckRCWReceipt ok");
        }

        Logger m_log;
    }

    class CheckState_1 extends EventScheduler.NotifyShell
    {
        private Logger m_log;

        CheckState_1(Logger log, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_log = log;
        }

        public void ProcessCommand()
        {
            System.out.println();
            m_log.info("<<<<CheckState_1::ProcessCommand>>>>");
            RecordingRequest rec1 = (RecordingRequest) findObject("Recording1");
            RecordingRequest rec2 = (RecordingRequest) findObject("Recording2");
            RecordingRequest rec3 = (RecordingRequest) findObject("Recording3");
            if (rec3 == null || rec2 == null || rec3 == null)
            {
                m_failed = TEST_FAILED;
                m_log.error("CheckState_1 failed, rec1,B,or C is null");
                return;
            }

            int state1 = rec1.getState();
            int state2 = rec2.getState();
            int state3 = rec3.getState();

            m_log.info("state1 = " + DvrEventPrinter.xletLeafState(state1));
            m_log.info("state2 = " + DvrEventPrinter.xletLeafState(state2));
            m_log.info("state3 = " + DvrEventPrinter.xletLeafState(state3));

            if (!((state1 == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE)
                    || (state1 == LeafRecordingRequest.IN_PROGRESS_STATE)
                    && (state2 == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE) || (state2 == LeafRecordingRequest.IN_PROGRESS_STATE)
                    && (state3 == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)))
            {
                m_failed = TEST_FAILED;
                m_log.error("CheckState_1 failed");
            }

        }
    }

    class CheckState_2 extends EventScheduler.NotifyShell
    {

        CheckState_2(Logger log, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_log = log;
        }

        public void ProcessCommand()
        {
            System.out.println();
            m_log.info("<<<<CheckState_2::ProcessCommand>>>>");
            RecordingRequest rec2 = (RecordingRequest) findObject("Recording2");
            RecordingRequest rec3 = (RecordingRequest) findObject("Recording3");
            if (rec3 == null || rec2 == null || rec3 == null)
            {
                m_failed = TEST_FAILED;
                m_log.error("CheckState_2 failed, rec2 or C is null");
                return;
            }

            int state2 = rec2.getState();
            int state3 = rec3.getState();

            m_log.info("state2 = " + DvrEventPrinter.xletLeafState(state2));
            m_log.info("state3 = " + DvrEventPrinter.xletLeafState(state3));

            if (!((state2 == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE) || (state2 == LeafRecordingRequest.IN_PROGRESS_STATE)
                    && (state3 == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)))
            {
                // m_failed = TEST_FAILED;
                m_log.info("CheckState_2 warning");
            }
        }

        private Logger m_log;
    }

    interface HelperIFace
    {
        boolean getRcwReceived();
    }

    /**
     * 
     * For the Scheduling of a recording whose before start time has past.
     * 
     * @author jspruiel
     */
    public class Test_RCW_5 extends TestCase implements ResourceContentionHandler, HelperIFace
    {
        long m_gTZero;

        private Logger log;

        /**
         * Constructor
         * 
         * @param locator
         */
        Test_RCW_5(OcapLocator locator)
        {
            m_locator = locator;
        }

        /**
         * Returns this test when called by the test framework.
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
            return "Test_RCW_5";
        }

        /*
         * (non-Javadoc) Three Recordings are scheduled to start at the same
         * time. In this case the NetworkInterface does not have knowledge of
         * the required resource usages.
         * 
         * Expected Results: For each of the three before start event triggers,
         * the code executes checkImpendingConflicts and invokes the
         * ResourceContentionHandler.resourceContentionWarning method. The
         * current reservations will include reservations for the other two
         * recordings.
         * 
         * In other words, the test will pass if the counter is three.
         * 
         * 
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#runTest()
         */
        public void runTest()
        {
            log = Logger.getLogger("Test_RCW_5");
            log.info("runTest executing.");

            m_failed = TEST_PASSED;

            // add the RCH
            ResourceContentionManager rcm = ResourceContentionManager.getInstance();

            rcm.setResourceContentionHandler(this);
            rcm.setWarningPeriod(110000);

            long now = System.currentTimeMillis();
            m_startTime = now;

            // clear the schedule of pending tasks
            reset();

            m_eventScheduler.scheduleCommand(new RecordA(log, // recording name
                    "Recording1", // source
                    m_locator, // start time
                    now + 30000L, // duration
                    TEN_MINUTES, 500L)); // trigger time

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording2", m_locator, now + 32000L, TEN_MINUTES, 550L));

            m_eventScheduler.scheduleCommand(new RecordA(log, "Recording3", m_locator, // start
                                                                                       // time
                    now + 120000L + 80000L, // duration
                    TEN_MINUTES, 120000L)); // trigger in 120s

            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording3", 182000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording2", 183000));
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("Recording1", 184000));

            m_eventScheduler.run(5000);
            rcm.setResourceContentionHandler(null);
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("Test_RCW_5. completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("Test_RCW_5.  completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("Test_RCW_5.  completed: PASSED");
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
            return null;
        }

        /**
         * Respond by cancelling on the currentReservations.
         * 
         */
        public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
        {
            synchronized (this)
            {
                log.info("---- Entered test's resourceContentionWarning Method -----\n" + "newReq = " + newRequest
                        + "\n");
                String[] resNames = newRequest.getResourceNames();
                log.info("newRequest has " + resNames.length + " resource names elems.\n");

                RecordingRequest rec1 = (RecordingRequest) findObject("Recording1");
                RecordingRequest rec2 = (RecordingRequest) findObject("Recording2");
                RecordingRequest rec3 = (RecordingRequest) findObject("Recording3");

                if (rec1 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording1");
                    return;
                }

                if (rec2 == null)
                {
                    m_failed = TEST_FAILED;
                    log.error("findObject did not find recording2");
                    return;
                }

                if (newRequest instanceof RecordingResourceUsage)
                {
                    // Print helpful info.
                    RecordingResourceUsage rru = (RecordingResourceUsage) newRequest;
                    RecordingRequest rr = rru.getRecordingRequest();

                    log.info("newRequest RRU =  " + newRequest);
                    log.info("newRequest RR =  " + rr);

                    if (rr.equals(rec1) || rr.equals(rec2))
                    {
                        return;
                    }
                    else
                    {
                        m_rcwReceived = true;

                        int state1 = rec1.getState();
                        int state2 = rec2.getState();
                        int state3 = rr.getState();

                        log.info("RecA = " + rec1);
                        log.info("RecB = " + rec2);
                        log.info("RecC = " + rr);
                        log.info("state1 = " + DvrEventPrinter.xletLeafState(state1));
                        log.info("state2 = " + DvrEventPrinter.xletLeafState(state2));
                        log.info("state3 = " + DvrEventPrinter.xletLeafState(state3));
                    }
                }

                // for this newRequest list resource names
                for (int i = 0; i < resNames.length; i++)
                {
                    String currName = resNames[i];
                    // log.info("newRequest ResourceName[" + i + "] = " +
                    // currName +"\n");
                    log.info("newRequest ResourceName[" + i + "] = " + currName + "\n");
                }

                // -- Now handle the currentReservations array.--

                // Log the properties for each ResourceUsage in
                // the current reservations set (informational).

                log.info("================= List of Current Reservations ================== ");
                log.info("CurrRes count = " + currentReservations.length);

                for (int i = 0; i < currentReservations.length; i++)
                {
                    ResourceUsage ru = currentReservations[i];
                    if (ru instanceof RecordingResourceUsage)
                    {
                        // Print helpful info.
                        RecordingResourceUsage rru = (RecordingResourceUsage) ru;
                        RecordingRequest rr = rru.getRecordingRequest();
                        log.info("elem[" + i + "] - RRU = " + rru);
                        log.info("whose RR = " + rr);

                    }

                    resNames = ru.getResourceNames();

                    // Print the list of names for this ResourceUsage
                    // Also print the ResourceProxy
                    for (int j = 0; j < resNames.length; j++)
                    {
                        String currName = resNames[j];
                        log.info("ResourceName[" + j + "] = " + currName);
                        ResourceProxy rproxy = ru.getResource(currName);
                        if (rproxy != null)
                        {
                            ResourceClient rclient = rproxy.getClient();
                            if (rclient != null)
                            {
                                log.info("ResourceProxy = " + rproxy);
                                log.info("ResourceClient = " + rclient);
                            }
                        }
                    }// end-for
                }// end-for
                // DVRTestRunnerXlet.log(buff.toString());
            }
        }// end-meth

        public boolean getRcwReceived()
        {
            return m_rcwReceived;
        }

        private boolean m_rcwReceived = false;

        private OcapLocator m_locator;

        private long m_startTime;
    }// end-class

    public class SPrint
    {
        private long m_tzero;

        SPrint(long refTime)
        {
            m_tzero = refTime;
        }

        void print(String s)
        {
            long tstamp = System.currentTimeMillis() - m_tzero;
            System.out.println("<- " + tstamp + " ms->  " + s);
        }
    }

    public class BPrint
    {
        private StringBuffer m_sb;

        private long m_tzero;

        BPrint(long refTime)
        {
            m_tzero = refTime;
            m_sb = new StringBuffer();
        }

        public void append(String s)
        {
            long tstamp = System.currentTimeMillis() - m_tzero;
            String tmp = "<- " + tstamp + " ms->" + " " + s; // echo
            m_sb.append(tmp);
        }

        public void appendNL(String s)
        {
            long tstamp = System.currentTimeMillis() - m_tzero;
            String tmp = "<- " + tstamp + " ms->" + " " + s + "\n"; // echo
            m_sb.append(tmp);
        }

        public String getString()
        {
            return m_sb.toString();
        }
    }

    ResourceUsage getFromList(Logger log, int ruType, ResourceUsage[] rua)
    {
        ResourceUsage ru = null;
        int len = rua.length;
        boolean found = false;

        for (int i = 0; i < len & !found; i++)
        {
            if (ruType == TestResContentionWarning.RRU)
            {
                if (rua[i] instanceof RecordingResourceUsage)
                {
                    found = true;
                    ru = rua[i];
                }
            }
            else if (ruType == TestResContentionWarning.TSBRU)
            {
                if (rua[i] instanceof TimeShiftBufferResourceUsage)
                {
                    found = true;
                    ru = rua[i];
                }
            }
        }

        if (ru == null)
        {
            log.debug("Returning null");
        }
        return ru;
    }

    void checkSharedUsages(Logger log, ResourceUsage[] currentReservations)
    {
        for (int i = 0; i < currentReservations.length; i++)
        {
            ResourceUsage ru = currentReservations[i];
            log.info("ResourceUsage[" + i + "]=" + ru);

            // Per ECN 1135 - recordings implicitly backed by TSBs should not be
            // shared
            // resource usages.
            if (!(ru instanceof RecordingResourceUsage))
            {
                log.error(m_failedReason = "FAILURE - Expected a RecordingResourceUsages: ru = " + ru);
                m_failed = TEST_FAILED;
                return;
            }
        }
    }

    long TEN_MINUTES = 600000L;
}
