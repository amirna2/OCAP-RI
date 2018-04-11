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

import java.util.Enumeration;
import java.util.Vector;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;

/**
 * Class consists of tests for ECN 829, the RecordingId feature addition.
 * 
 * @author jspruiel
 */
public class TestRecordingID_ECN829 extends DvrTest
{

    TestRecordingID_ECN829(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestRecordingIDBasic((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * Validates method RecordingRequest.getID() returns a uniqueID per
     * recording and that method RecordingManager.getRecordingRequest(int id)
     * returns a unique RecordingRequest per id.
     * 
     * @author jspruiel
     */
    public class TestRecordingIDBasic extends TestCase
    {
        TestRecordingIDBasic(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRecordingIDBasic";
        }

        public void runTest()
        {
            reset();

            Vector id_bucket = new Vector();
            m_failed = TEST_PASSED;

            OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();

            RecordingList rlist = orm.getEntries();

            int count = rlist.size();

            DVRTestRunnerXlet.log("TestRecordingIDBasic " + "found " + count + " Recordings already on disk");

            for (int i = 0; i < count; i++)
            {
                int id= (rlist.getRecordingRequest(i)).getId();

                // verify the id is unique
                if (id_bucket.contains(new Integer(id)))
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("TestRecordingIDBasic: " +" id returned by getId() of existing recording " +i +"RecordingRequest is not unique: " +id +" already exists");
                }

                id_bucket.addElement(new Integer(id));
            }
            // find an integer value that is not a member of the existingIds.
            int hole = 1;
            while (true)
            {
                if (!id_bucket.contains(new Integer(hole)))
                {
                    try
                    {
                        orm.getRecordingRequest(hole);
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("TestRecordingIDBasic IllegalArgumentException was not thrown when trying to getRecordingRequest(...) on a non-existing id: " +hole);
                    }
                    catch (IllegalArgumentException iae)
                    {
                        DVRTestRunnerXlet.log("TestRecordingIDBasic IllegalArgumentException was correcly thrown when trying to getRecordingRequest(...) on a non-existing id: " +hole);
                    }

                    break;
                }
                else
                    hole++;
            }


            String[] rnames = { "Recording1", "Recording2", "Recording3", "Recording4", "Recording5", "Recording6", "Recording7", "Recording8" };

            // 1. Schedule recordings
            long trigger = 500;
            long now = System.currentTimeMillis();

            for (int i = 0; i < rnames.length; i++)
            {
                m_eventScheduler.scheduleCommand(new Record(rnames[i], m_locator, now + 60000000L, 90000L, trigger));

                trigger += 100L;
            }

            trigger = 5000L;
            // 2. Must have 8 extra recordings.
            int expectedCount = count + rnames.length;
            m_eventScheduler.scheduleCommand(new CountRecordings(expectedCount, trigger));

            trigger += 1000L;

            // 3. Create a set of recording ids.
            m_eventScheduler.scheduleCommand(new CreateRecordingIdSet(rnames, id_bucket, expectedCount, trigger));

            trigger += 3000L;
            // 4. Validate RecordingManager.getRecordingRequest in database
            // succeeds for each recording id in the set.
            m_eventScheduler.scheduleCommand(new LookupById(id_bucket, trigger));

            // 5. To cleanup, start by deleting all recordings. Allowing
            // 100ms to process each delete.
            trigger += 2000L;
            for (int i = 0; i < rnames.length; i++)
            {
                m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(rnames[i], trigger));
                trigger += 500L;
            }

            // 6. Initiate processing the command list,
            // and wait for 1000ms after the last command is launched.
            m_eventScheduler.run(10000);

            // 7. Cleanup framework objects.
            // Needed because DeleteRecordingRequest does not signal the the
            // framework to forget its references to recording objects.
            m_objects.removeAllElements();

            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("TestRecordingIDBasic  " + "completed: FAILED");
            }
            else
            {
                DVRTestRunnerXlet.log("TestRecordingIDBasic " + "completed: PASSED");
            }

        }// end method

        /**
         * Retrieves the id from each recording request and adds it to the
         * bucket.
         * 
         * @author jspruiel
         * 
         */
        class CreateRecordingIdSet extends EventScheduler.NotifyShell
        {
            String[] m_recNames;

            Vector m_bucket;
            int m_expectedBucketSize;

            /**
             * 
             * @param names
             *            A set of recording names.
             * @param bucket
             *            The input bucket to store recording ids
             * @param trigger
             *            This objects scheduled execution time.
             */
            public CreateRecordingIdSet(String[] names, Vector bucket, int expectedBucketSize, long trigger)
            {
                super(trigger);
                m_recNames = names;
                m_bucket = bucket;
                m_expectedBucketSize = expectedBucketSize;
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell#
             * ProcessCommand()
             */
            public void ProcessCommand()
            {
                System.out.println("TestRecordingIDBasic:" + "CreateRecordingIdSet:ProcessCommand Executing");

                if (m_failed == TEST_FAILED)
                {
                    System.out.println("TestRecordingIDBasic:"
                            + "CreateRecordingIdSet:ProcessCommand error; previous CMD failed.");
                    return;
                }

                // Add each id to the bucket.
                for (int i = 0; i < m_recNames.length; i++)
                {
                    RecordingRequest rr = (RecordingRequest) findObject(m_recNames[i]);
                    if (rr == null)
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("TestRecordingIDBasic:"
                                + "CreateRecordingIdSet:ProcessCommand error:findObject " + m_recNames[i]);
                        return;
                    }

                    int id = rr.getId();

                    // verify the id is unique
                    if (m_bucket.contains(new Integer(id)))
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("TestRecordingIDBasic:" + "CreateRecordingIdSet:ProcessCommand error: id returned by getId() of " +m_recNames[i] +" RecordingRequest is not unique: " +id +" already exists");
                    }

                    m_bucket.addElement(new Integer(id));
                }

                // A sanity check that says fail if the expected number of
                // recordings do not exist for some weird reason.
                if (m_bucket.size() != m_expectedBucketSize)
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("TestRecordingIDBasic:" + "CreateRecordingIdSet:ProcessCommand error:size = " + m_bucket.size());
                }
                System.out.println("TestRecordingIDBasic:" + "CreateRecordingIdSet:ProcessCommand Done");

            }
        }// end inner class

        /**
         * Validates all recording ids received via the input bucket object are
         * returned from the call to RecordingManager.getRecordingRequest(int
         * id). If the function fails for any id in the set the test as a whole
         * is considered failed. failed.
         * 
         * @author jspruiel
         * 
         */
        class LookupById extends EventScheduler.NotifyShell
        {
            Vector m_bucket;

            /**
             * Constructor
             * 
             * @param bucket
             *            Input parameter representing a set of recording ids
             *            that was created earlier in the test.
             * 
             * @param trigger
             *            The execution time for this object.
             */
            public LookupById(Vector bucket, long trigger)
            {
                super(trigger);
                m_bucket = bucket;
            }

            /*
             * (non-Javadoc)
             * 
             * @see org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell#
             * ProcessCommand()
             */
            public void ProcessCommand()
            {
                System.out.println("TestRecordingIDBasic:" + "LookupById:ProcessCommand Executing");

                if (m_failed == TEST_FAILED)
                {
                    System.out.println("TestRecordingIDBasic:"
                            + "LookupById:ProcessCommand Aborting; previous CMD failed.");
                    return;
                }

                OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();
                RecordingRequest rr;

                Enumeration elems = m_bucket.elements();
                while (elems.hasMoreElements())
                {
                    Integer intObj = (Integer) elems.nextElement();
                    int id = intObj.intValue();
                    try
                    {
                        rr = orm.getRecordingRequest(id);
                        if (rr == null)
                        {
                            m_failed = TEST_FAILED;
                            DVRTestRunnerXlet.log("TestRecordingIDBasic:"
                                    + "LookupById:ProcessCommand compliance error");
                        }

                        if (rr.getId() != id)
                        {
                            m_failed = TEST_FAILED;
                            DVRTestRunnerXlet.log("TestRecordingIDBasic:" + "LookupById:ProcessCommand error");
                        }
                    }
                    catch (IllegalStateException ise)
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("TestRecordingIDBasic:" + "LookupById:ProcessCommand RecordingRequest " + "IllegalStateException");
                    }
                    catch (SecurityException se)
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("TestRecordingIDBasic:" + "LookupById:ProcessCommand RecordingRequest " + "SecurityException");
                    }
                }
                System.out.println("TestRecordingIDBasic:" + "LookupById:ProcessCommand Done");

            }
        }// end inner class

        // Fields for test.
        private OcapLocator m_locator;
    }
}
