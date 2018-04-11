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
 * Created on Feb 15, 2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.media.VBIFilterEvent;
import org.ocap.media.VBIFilterListener;
import org.ocap.net.OcapLocator;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestVBIFiltering2 extends VbiTest
{

    private static final int ALL_FILTERS = 99;

    private static final byte END_BYTE = 0x0f;

    private static final int SLOP = 2;

    /**
     * @param locators
     */
    TestVBIFiltering2(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        // Note: the delay attribute should be the same as the timeout/time
        // notification/data units on the config files
        tests.addElement(new TestVBIGroupMethods((OcapLocator) m_locators.elementAt(0), 2));
        tests.addElement(new TestVBINotification((OcapLocator) m_locators.elementAt(0), 12, 0, 10000)); // timeout
        tests.addElement(new TestVBINotification((OcapLocator) m_locators.elementAt(0), 13, 1, 30000));
        tests.addElement(new TestVBINotification((OcapLocator) m_locators.elementAt(0), 14, 1, 60000));

        // These tests have been invalidated at this current point in time -
        // requirements not dictating current action
        // tests.addElement(new TestVBINotification(
        // (OcapLocator)m_locators.elementAt(0),15,1,60000));
        // tests.addElement(new TestVBIZeroNotification(
        // (OcapLocator)m_locators.elementAt(0),16,1,60000));

        tests.addElement(new TestVBITimeNotification((OcapLocator) m_locators.elementAt(0), 17, 0, 10000)); // time
                                                                                                            // notification
        tests.addElement(new TestVBITimeNotification((OcapLocator) m_locators.elementAt(0), 18, 2, 30000));
        tests.addElement(new TestVBITimeNotification((OcapLocator) m_locators.elementAt(0), 19, 2, 60000));

        // These tests have been invalidated at this current point in time -
        // requirements not dictating current action
        // tests.addElement(new TestVBITimeNotification(
        // (OcapLocator)m_locators.elementAt(0),20,2,60000));
        // tests.addElement(new TestVBITimeNotification(
        // (OcapLocator)m_locators.elementAt(0),21,2,60000));

        tests.addElement(new TestVBIDataNotification((OcapLocator) m_locators.elementAt(0), 22, 0, 10)); // data
                                                                                                         // units
        tests.addElement(new TestVBIDataNotification((OcapLocator) m_locators.elementAt(0), 23, 3, 6));
        tests.addElement(new TestVBIDataNotification((OcapLocator) m_locators.elementAt(0), 24, 3, 6));
        tests.addElement(new TestVBIDataNotification2((OcapLocator) m_locators.elementAt(0), 25, 3, 6));
        tests.addElement(new TestVBIDataNotification3((OcapLocator) m_locators.elementAt(0), 26, 3, 6));

        tests.addElement(new TestVBIEventNotification((OcapLocator) m_locators.elementAt(0), 32, 0, 10000));
        tests.addElement(new TestVBIEventNotification((OcapLocator) m_locators.elementAt(0), 33, 0, 10000));
        tests.addElement(new TestVBIEventNotification((OcapLocator) m_locators.elementAt(0), 34, 0, 10));
        return tests;
    }

    public class TestVBIGroupMethods extends TestCase
    {
        protected OcapLocator m_locator;

        protected boolean eventReceived;

        protected int m_groupNum;

        /*
         * @param locator : service to be tuned to
         * 
         * @param groupNum : VBI Group defined in the config file
         */
        public TestVBIGroupMethods(OcapLocator locator, int groupNum)
        {
            m_locator = locator;
            m_groupNum = groupNum;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestVBIGroupMethods";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            eventReceived = false;
            // Clear event scheduler
            reset();

            // Intialize the Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext(1000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 3000));

            // Create the filter group
            m_eventScheduler.scheduleCommand(new createVBIFilterGroup(m_groupNum, 0, 20000));

            // Attach the filter group - use SC and default Resource Client
            m_eventScheduler.scheduleCommand(new attachFilterGroup(true, true, 30000));

            // Validate getter and setter methods
            m_eventScheduler.scheduleCommand(new checkVBIGroupInfo(m_groupNum, 1, 0, 50000));

            // Detach the filter group
            m_eventScheduler.scheduleCommand(new detachFilterGroup(0, 70000));

            // Destroy the Service Context
            m_eventScheduler.scheduleCommand(new DestroyBroadcastService(80000));

            // Run the schedule
            m_eventScheduler.run(10000);

            // Post results
            postResults(getName(), true);
        }
    }

    public class TestVBINotification extends TestCase implements VBIFilterListener
    {

        protected OcapLocator m_locator;

        protected boolean eventReceived;

        protected int m_groupNum;

        protected int m_reset;

        protected long[] args;

        protected long startTime;

        protected long m_delay;

        /**
         * @param locator
         *            : service to be tuned to
         * @param groupNum
         *            : VBI Group defined in the config file
         */
        public TestVBINotification(OcapLocator locator, int groupNum, int reset, long delay)
        {
            m_locator = locator;
            m_groupNum = groupNum;
            m_reset = reset;
            m_delay = delay;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestVBINotification : TestTimeout" + m_groupNum;
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            eventReceived = false;
            // initalize start time
            startTime = System.currentTimeMillis();
            // Clear event scheduler
            reset();
            setupArgs();

            // Intialize the Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext(1000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 3000));

            // Create the filter group
            m_eventScheduler.scheduleCommand(new createVBIFilterGroup(m_groupNum, 0, 20000));

            // Attach the filter group - use SC and default Resource Client
            m_eventScheduler.scheduleCommand(new attachFilterGroup(true, true, 30000));

            // Attach a listener to the filter (this)
            m_eventScheduler.scheduleCommand(new registerVBIFilterListener(ALL_FILTERS, 0, this, 30000));

            // Start the filter
            m_eventScheduler.scheduleCommand(new startVBIFilters(ALL_FILTERS, 0, 40000));

            if (m_reset != 0)
            {
                args = getParamsForReset(m_groupNum);
                switch (m_reset)
                {
                    case 1:
                        m_eventScheduler.scheduleCommand(new resetTimeout(1, 0, args[1], 40000 + args[0]));
                        // reset the delay of the event to the startTime
                        if (args[1] < args[0])
                            m_delay = args[0]; // this is the amount of time
                                               // from start time (test case
                                               // start)
                        // to when the timeout is to occur (the notifacation
                        // should happen immediately)
                        else
                            m_delay = args[1]; // this is amount of time from
                                               // start time (test case start)
                        // to when the attibute is changed
                        break;
                    case 2:
                        m_eventScheduler.scheduleCommand(new resetTimeNotification(1, 0, args[1], 40000 + args[0]));
                        if (args[1] < args[0])
                            m_delay = args[0]; // this is the amount of time
                                               // from first data unit
                                               // notification
                        // to when the timeout is to occur (the notifacation
                        // should happen immediately)
                        else
                            m_delay = args[1]; // this is amount of time from
                                               // first data unit notification
                        // to when the attibute is changed
                        break;
                    case 3: // the reset will be handled when the first data
                            // unit has arrived.
                        m_delay = args[0]; // hack used here - firast argument
                                           // should have the
                        break;
                    default:
                        break;
                }
            }

            // Run the schedule
            m_eventScheduler.run(100000);

            // Check if event was received
            eventCheck();

            // Finally, detach the filter group
            detachGroup(0);

            // Destroy the presenting service
            new DestroyBroadcastService(0).ProcessCommand();
            // Post results
            postResults(getName(), true);
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (VBIFilterEvent.EVENT_CODE_TIMEOUT == event.getEventCode() && findFilterToEvent(event) == 1)
            {
                eventReceived = true;
                // Verfiy the time and see if it is in range
                System.out.println("Time to be compared " + (startTime + m_delay + 40000) + "Start time" + startTime);
                verifyEventTime(startTime + m_delay + 43000);
                // Stop listening
                stopVBIFilter(0, 1);
                new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
            }
        }

        public void eventCheck()
        {
            // check for event sent
            if (!eventReceived)
            {
                DVRTestRunnerXlet.log("FAILED: No event was gerated in the time alloted");
                m_failedReason = "FAILED: No event was gerated in the time alloted";
                m_failed = TEST_FAILED;
            }
        }
    }

    public class TestVBIZeroNotification extends TestVBINotification
    {

        /**
         * @param locator
         * @param groupNum
         * @param reset
         * @param delay
         */
        public TestVBIZeroNotification(OcapLocator locator, int groupNum, int reset, long delay)
        {
            super(locator, groupNum, reset, delay);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestVBIZeroNotification";
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (VBIFilterEvent.EVENT_CODE_TIMEOUT == event.getEventCode() && findFilterToEvent(event) == 1)
            {
                eventReceived = true;
                // Should not be triggered
                stopVBIFilter(0, 1);
                new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
            }
        }

        public void eventCheck()
        {
            // check for event sent
            if (eventReceived)
            {
                DVRTestRunnerXlet.log("FAILED: Event gerated !!!!!");
                m_failed = TEST_FAILED;
            }
        }
    }

    public class TestVBITimeNotification extends TestVBINotification
    {

        /**
         * @param locator
         * @param groupNum
         * @param reset
         * @param delay
         */
        public TestVBITimeNotification(OcapLocator locator, int groupNum, int reset, long delay)
        {
            super(locator, groupNum, reset, delay);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestVBITimeNotification : TimeNotifyGroup " + m_groupNum;
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (findFilterToEvent(event) == 1)
            {
                if (VBIFilterEvent.EVENT_CODE_FIRST_VBI_DATA_AVAILABLE == event.getEventCode())
                {
                    DVRTestRunnerXlet.log("Received First VBI data notification");
                    startTime = System.currentTimeMillis();
                    System.out.println("Time of first VBI event: " + startTime);
                }
                if (VBIFilterEvent.EVENT_CODE_TIME_NOTIFICATION == event.getEventCode())
                {
                    DVRTestRunnerXlet.log("Received Time Notification");
                    eventReceived = true;
                    // Verfiy the time and see if it is in range
                    verifyEventTime(startTime + m_delay);
                    // Should not be triggered
                    stopVBIFilter(0, 1);
                    new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
                }
            }
        }
    }

    public class TestVBIDataNotification extends TestVBINotification
    {

        /**
         * @param locator
         * @param groupNum
         * @param reset
         * @param delay
         */
        public TestVBIDataNotification(OcapLocator locator, int groupNum, int reset, long delay)
        {
            super(locator, groupNum, reset, delay);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestVBIDataNotification : DataNotifyGroup " + m_groupNum;
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (findFilterToEvent(event) == 1)
            {
                if (VBIFilterEvent.EVENT_CODE_FIRST_VBI_DATA_AVAILABLE == event.getEventCode())
                {
                    DVRTestRunnerXlet.log("Received First VBI data notification");
                    if (m_reset != 0)
                    {
                        new resetDataNotification(1, 0, (int) args[1], 0).ProcessCommand();
                    }
                }
                if (VBIFilterEvent.EVENT_CODE_UNITS_NOTIFICATION == event.getEventCode())
                {
                    DVRTestRunnerXlet.log("Received EVENT_CODE_UNITS_NOTIFICATION data notification");
                    eventReceived = true;
                    checkUnitsReceived((int) m_delay);
                    stopVBIFilter(0, 1);
                    new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
                }
            }
        }

        public void checkUnitsReceived(int units)
        {
            int unitsRcvd = getUnitsReceived(1, 0, END_BYTE);
            if (units > unitsRcvd || units + SLOP < unitsRcvd)
            {
                DVRTestRunnerXlet.log("FAILED: Mismatich with units received got :" + unitsRcvd + " shold be:" + units);
                m_failed = TEST_FAILED;
            }
        }
    }

    public class TestVBIDataNotification2 extends TestVBIDataNotification
    {

        public TestVBIDataNotification2(OcapLocator locator, int groupNum, int reset, long delay)
        {
            super(locator, groupNum, reset, delay);
        }

        public void checkUnitsReceived(int units)
        {
            int unitsRcvd = getUnitsReceived(1, 0, END_BYTE);
            if (units < unitsRcvd)
            {
                DVRTestRunnerXlet.log("FAILED: Units received greater than was to be received got :" + unitsRcvd
                        + " shold be:" + units);
                m_failed = TEST_FAILED;
            }
        }
    }

    public class TestVBIDataNotification3 extends TestVBIDataNotification
    {

        public TestVBIDataNotification3(OcapLocator locator, int groupNum, int reset, long delay)
        {
            super(locator, groupNum, reset, delay);
        }

        public void eventCheck()
        {
            // check for event sent
            if (eventReceived)
            {
                DVRTestRunnerXlet.log("FAILED: Event gerated !!!!!");
                m_failed = TEST_FAILED;
            }
        }
    }

    /*
     * This test case is designed to test to see if each of the Notifications
     * will be signalled once in a session. In each test case only one of the
     * notifications will be used to verify if multple instances of it are
     * broadcasted
     */
    public class TestVBIEventNotification extends TestVBINotification
    {

        public TestVBIEventNotification(OcapLocator locator, int groupNum, int reset, long delay)
        {
            super(locator, groupNum, reset, delay);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestVBIEventNotification : Group " + m_groupNum;
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (VBIFilterEvent.EVENT_CODE_TIMEOUT == event.getEventCode())
            {
                DVRTestRunnerXlet.log("Received TimeOut Notification");
                if (!eventReceived)
                {
                    eventReceived = true;
                    // Verfiy the time and see if it is in range
                    args = getParamsForReset(m_groupNum);
                    new resetTimeout(1, 0, args[1], 0).ProcessCommand();
                }
                else
                {
                    DVRTestRunnerXlet.log("FAILED: Extra EVENT_CODE_TIMEOUT was generated ");
                    m_failedReason = "FAILED: Extra EVENT_CODE_TIMEOUT was generated ";
                    m_failed = TEST_FAILED;
                }
            }
            if (VBIFilterEvent.EVENT_CODE_TIME_NOTIFICATION == event.getEventCode())
            {
                if (!eventReceived)
                {
                    DVRTestRunnerXlet.log("Received Time Notification");
                    eventReceived = true;
                    // Verfiy the time and see if it is in range
                    args = getParamsForReset(m_groupNum);
                    new resetTimeNotification(1, 0, args[1], 0).ProcessCommand();
                }
                else
                {
                    DVRTestRunnerXlet.log("FAILED: Extra EVENT_CODE_TIME_NOTIFICATION was generated ");
                    m_failedReason = "FAILED: Extra EVENT_CODE_TIME_NOTIFICATION was generated ";
                    m_failed = TEST_FAILED;
                }
            }
            if (VBIFilterEvent.EVENT_CODE_UNITS_NOTIFICATION == event.getEventCode())
            {
                if (!eventReceived)
                {
                    eventReceived = true;
                    DVRTestRunnerXlet.log("Received EVENT_CODE_UNITS_NOTIFICATION data notification");
                    args = getParamsForReset(m_groupNum);
                    new resetDataNotification(1, 0, (int) args[1], 0).ProcessCommand();
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("Set to FAILED until extra EVENT_CODE_TIME_NOTIFICATION are generated ");
                }
                else
                {
                    DVRTestRunnerXlet.log("PASSED: Extra EVENT_CODE_UNITS__NOTIFICATION was generated");
                    m_failed = TEST_PASSED;
                }
            }
        }

        public void checkUnitsReceived(int units)
        {
            int unitsRcvd = getUnitsReceived(1, 0, END_BYTE);
            if (units > unitsRcvd || units + SLOP < unitsRcvd)
            {
                DVRTestRunnerXlet.log("FAILED: Mismatich with units received got :" + unitsRcvd + " shold be:" + units);
                m_failed = TEST_FAILED;
            }
        }

        public void eventCheck()
        {
            // check for event sent
            if (!eventReceived)
            {
                DVRTestRunnerXlet.log("FAILED: No event was gerated in the time alloted");
                m_failedReason = "FAILED: No event was gerated in the time alloted";
                m_failed = TEST_FAILED;
            }
            stopVBIFilter(0, 1);
            new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
        }
    }

}
