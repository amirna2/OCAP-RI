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
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.net.OcapLocator;

public class TestAddBeforeStartListener extends DvrTest
{
    TestAddBeforeStartListener(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestAddBeforeStartListener1((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestAddBeforeStartListener2((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestAddBeforeStartListener3((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    public class TestAddBeforeStartListener1 extends TestCase implements RecordingAlertListener
    {

        /**
         * Constructor
         * 
         * @param locator
         */
        TestAddBeforeStartListener1(OcapLocator locator)
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
            return "TestAddBeforeStartListener1";
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#runTest()
         */
        public void runTest()
        {
            // clear the schedule of pending tasks
            reset();
            eventCount = 0;

            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            m_failed = TEST_PASSED;

            startTime = System.currentTimeMillis() + 90000L; // 5 minutes from
                                                             // now
            long duration = 60000L * 2; // 2 minutes seconds

            DVRTestRunnerXlet.log("*** Going to listen for 5 alert event...");
            rm.addRecordingAlertListener(this, 60000L); // expect one event 25
                                                        // sec after schedule
            rm.addRecordingAlertListener(this, 65000L); // expect one event 30
                                                        // sec after schedule
            rm.addRecordingAlertListener(this, 45000L); // expect one event 45
                                                        // sec after schedule
            rm.addRecordingAlertListener(this, 30000L); // expect one event 60
                                                        // sec after schedule
            rm.addRecordingAlertListener(this, 20000L); // expect one event 70
                                                        // sec after schedule

            m_eventScheduler.scheduleCommand(new Record("RecordingA", m_locator, startTime, duration, 600L));
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("RecordingA", 80000L));

            m_eventScheduler.run(90000L);
            rm.removeRecordingAlertListener(this);
            if (eventCount != 5)
            {
                System.out.println("TestAddBeforeStartListener1 - Only received " + eventCount + " events.");
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestAddBeforeStartListener1 completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestAddBeforeStartListener1  completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestAddBeforeStartListener1  completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @seeorg.ocap.dvr.RecordingAlertListener#recordingAlert(org.ocap.dvr.
         * RecordingAlertEvent)
         */
        public void recordingAlert(RecordingAlertEvent e)
        {
            long tNOW = System.currentTimeMillis();

            eventCount++;

            DvrEventPrinter.printRecordingAlertEvent(e);

            System.out.println("TestAddBeforeStartListener1 - b4start = " + (startTime - tNOW));

            System.out.println("TestAddBeforeStartListener - eventCount = " + eventCount);
        } // END recordingAlert()

        int eventCount;

        private OcapLocator m_locator;

        private long startTime;

    }

    public class TestAddBeforeStartListener2 extends TestCase
    {

        /**
         * Constructor
         * 
         * @param locator
         */
        TestAddBeforeStartListener2(OcapLocator locator)
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
            return "TestAddBeforeStartListener2";
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#runTest()
         */
        public void runTest()
        {
            m_failed = TEST_PASSED;
            // clear the schedule of pending tasks
            reset();
            m_listeners = new Vector();
            StringBuffer sb = new StringBuffer();
            sb.append("----- Non-Realtime Dump of Test Traces ------\n");
            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            long m_startTime = System.currentTimeMillis() + 90000L;
            long duration = 60000L * 2;

            // create 3 listeners and add them to the collection.
            m_listeners.addElement(new Listener(sb, m_startTime));
            m_listeners.addElement(new Listener(sb, m_startTime));
            rm.addRecordingAlertListener((Listener) m_listeners.elementAt(0), 60000L);
            rm.addRecordingAlertListener((Listener) m_listeners.elementAt(1), 60000L);

            m_eventScheduler.scheduleCommand(new Record("RecordingA", m_locator, m_startTime, duration, 600L));

            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("RecordingA", 80000L));

            m_eventScheduler.run(5000L);

            rm.removeRecordingAlertListener((Listener) m_listeners.elementAt(0));
            rm.removeRecordingAlertListener((Listener) m_listeners.elementAt(1));

            // Each element in the collection has a count that records
            // the number of times the element has been called.
            int callPerListenerExpected = 1;
            if ((TestAddBeforeStartListener.confirmCallsPerListener(m_listeners, sb, callPerListenerExpected) == false))
            {
                m_failed = TEST_FAILED;
            }

            DVRTestRunnerXlet.log(sb.toString());

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestAddBeforeStartListener2 completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestAddBeforeStartListener2  completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestAddBeforeStartListener2  completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;

        private Vector m_listeners = null;

        private long m_startTime;
    }

    public class TestAddBeforeStartListener3 extends TestCase
    {
        /**
         * Constructor
         * 
         * @param locator
         */
        TestAddBeforeStartListener3(OcapLocator locator)
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
            return "TestAddBeforeStartListener3";
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.cablelabs.xlet.DvrTest.DvrTest.TestCase#runTest()
         */
        public void runTest()
        {
            m_failed = TEST_PASSED;
            // clear the schedule of pending tasks
            reset();
            m_listeners = new Vector();
            StringBuffer sb = new StringBuffer();
            sb.append("----- Non-Realtime Dump of Test Traces ------\n");
            OcapRecordingManager rm = (OcapRecordingManager) (OcapRecordingManager.getInstance());

            long m_startTime = System.currentTimeMillis() + 90000L;
            long duration = 60000L * 2;

            // create 3 listeners and add them to the collection.
            m_listeners.addElement(new Listener(sb, m_startTime));
            m_listeners.addElement(new Listener(sb, m_startTime));
            m_listeners.addElement(new Listener(sb, m_startTime));
            rm.addRecordingAlertListener((Listener) m_listeners.elementAt(0), 60000L);
            rm.addRecordingAlertListener((Listener) m_listeners.elementAt(1), 60000L);
            rm.addRecordingAlertListener((Listener) m_listeners.elementAt(2), 60000L);

            m_eventScheduler.scheduleCommand(new Record("RecordingA", m_locator, m_startTime, duration, 600L));

            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("RecordingA", 80000L));

            m_eventScheduler.run(5000L);

            rm.removeRecordingAlertListener((Listener) m_listeners.elementAt(0));
            rm.removeRecordingAlertListener((Listener) m_listeners.elementAt(1));
            rm.removeRecordingAlertListener((Listener) m_listeners.elementAt(2));

            // Each element in the collection has a count that records
            // the number of times the element has been called.
            int callPerListenerExpected = 1;
            if ((TestAddBeforeStartListener.confirmCallsPerListener(m_listeners, sb, callPerListenerExpected) == false))
            {
                m_failed = TEST_FAILED;
            }

            DVRTestRunnerXlet.log(sb.toString());
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestAddBeforeStartListener3 completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestAddBeforeStartListener3  completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestAddBeforeStartListener3  completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;

        private Vector m_listeners = null;

        private long m_startTime;
    }

    private static boolean confirmCallsPerListener(Vector listeners, StringBuffer buff, int callsPer)
    {
        int cnt = 0;
        boolean res = true;

        for (int i = 0; i < listeners.size(); i++)
        {
            cnt = ((Listener) listeners.elementAt(i)).getCallCount();
            if (cnt != callsPer)
            {
                res = false;
                buff.append("Error Detected: This listener's call count - expected [" + callsPer + "], actual = " + cnt
                        + "\n");
            }
        }
        return res;
    }

    private class Listener implements RecordingAlertListener
    {
        Listener(StringBuffer sb, long startTime)
        {
            m_sb = sb;
            m_started = startTime;
        }

        public void recordingAlert(RecordingAlertEvent e)
        {
            long tNOW = System.currentTimeMillis();

            callCount++;
            DvrEventPrinter.printRecordingAlertEvent(e);
            m_sb.append("\n----- Listener Obj = " + this + "-------\n");
            m_sb.append("My estimated - b4startTime = " + (m_started - tNOW) + "\n");

            m_sb.append("My callCount = " + callCount + "\n");
        } // END recordingAlert()

        public int getCallCount()
        {
            return callCount;
        }

        // class fields
        private int callCount;

        private StringBuffer m_sb;

        private AccessMethods m_helper;

        private long m_started;
    }

    interface AccessMethods
    {
        long getStartTime();
    }
}
