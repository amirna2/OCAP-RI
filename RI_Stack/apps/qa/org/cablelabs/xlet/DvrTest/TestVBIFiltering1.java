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
 * Created on Feb 12, 2007
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
public class TestVBIFiltering1 extends VbiTest
{

    private static final int ALL_FILTERS = 99;

    /**
     * @param locators
     */
    TestVBIFiltering1(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestVBIBasicFilterFormat((OcapLocator) m_locators.elementAt(0), 1));
        tests.addElement(new TestVBIBasicFilterFormat((OcapLocator) m_locators.elementAt(0), 2));
        // tests.addElement(new TestVBIBasicFilterFormat(
        // (OcapLocator)m_locators.elementAt(0), 3));
        tests.addElement(new TestNullFiltering((OcapLocator) m_locators.elementAt(0), 1));
        tests.addElement(new TestCC1Filtering((OcapLocator) m_locators.elementAt(0), 4));
        tests.addElement(new TestCC2Filtering((OcapLocator) m_locators.elementAt(0), 5));
        tests.addElement(new TestXDSFiltering((OcapLocator) m_locators.elementAt(0), 6));
        tests.addElement(new TestXDSFiltering((OcapLocator) m_locators.elementAt(0), 7));
        tests.addElement(new TestXDSFiltering((OcapLocator) m_locators.elementAt(0), 8));
        tests.addElement(new TestXDSFiltering((OcapLocator) m_locators.elementAt(0), 9));
        tests.addElement(new TestXDSFiltering((OcapLocator) m_locators.elementAt(0), 10));
        tests.addElement(new TestMultipleFilters((OcapLocator) m_locators.elementAt(0), 11, 6));
        tests.addElement(new TestClearFiltering((OcapLocator) m_locators.elementAt(0), 2));
        tests.addElement(new TestNegFiltering((OcapLocator) m_locators.elementAt(0), 31));
        tests.addElement(new TestRestartFiltering((OcapLocator) m_locators.elementAt(0), 1));
        return tests;
    }

    /**
     * @author Ryan This TestCase class is designed to cover test cases
     *         2.1.1.1.1 to 2.1.1.1.3 A VBI Filter group is setup with one
     *         filter and is attached to a presenting Service Context. Upon
     *         getting one data unit, filtering is stopped and the data unit
     *         length is validated. After a period of time, if an event is not
     *         sent, the test will fail and the filter group wil be detached and
     *         the Service Context is destroyed.
     * 
     */
    public class TestVBIBasicFilterFormat extends TestCase implements VBIFilterListener
    {

        protected OcapLocator m_locator;

        protected boolean eventReceived;

        protected int m_groupNum;

        /**
         * @param locator
         *            : service to be tuned to
         * @param groupNum
         *            : VBI Group defined in the config file
         */
        public TestVBIBasicFilterFormat(OcapLocator locator, int groupNum)
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
            return "TestBasicVBIFiltering : TestVBIBasicFilterFormat" + m_groupNum;
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

            // Attach a listener to the filter (this)
            m_eventScheduler.scheduleCommand(new registerVBIFilterListener(ALL_FILTERS, 0, this, 30000));

            // Start the filter
            m_eventScheduler.scheduleCommand(new startVBIFilters(ALL_FILTERS, 0, 40000));

            // Run the schedule
            m_eventScheduler.run(90000);

            // check for event sent
            if (!eventReceived)
            {
                DVRTestRunnerXlet.log("FAILED: No event was gerated in the time alloted");
                m_failed = TEST_FAILED;
            }
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
            if (VBIFilterEvent.EVENT_CODE_FIRST_VBI_DATA_AVAILABLE == event.getEventCode()
                    && findFilterToEvent(event) == 1)
            {
                eventReceived = true;
                // Stop listening and detach filter
                stopVBIFilter(0, 1);
                new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
                // Verfiy the unit legnth is not and present the data
                new checkUnitLength(1, 0, m_groupNum, 0).ProcessCommand();

            }
        }
    }

    /**
     * @author Ryan
     * 
     *         Similar test to TestVBIBasicFilterFormat. This test will set up
     *         the
     * 
     * 
     */
    public class TestNegFiltering extends TestCase implements VBIFilterListener
    {

        protected OcapLocator m_locator;

        protected boolean eventReceived;

        protected int m_groupNum;

        /**
         * @param locator
         *            : service to be tuned to
         * @param groupNum
         *            : VBI Group defined in the config file
         */
        public TestNegFiltering(OcapLocator locator, int groupNum)
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
            return "TestNegFiltering : TestNegFiltering";
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

            // Attach a listener to the filter (this)
            m_eventScheduler.scheduleCommand(new registerVBIFilterListener(ALL_FILTERS, 0, this, 30000));

            // Start the filter
            m_eventScheduler.scheduleCommand(new startVBIFilters(ALL_FILTERS, 0, 40000));

            // Run the schedule
            m_eventScheduler.run(90000);

            // check for event sent
            if (!eventReceived)
            {
                DVRTestRunnerXlet.log("PASSED: No event was gerated in the time alloted");
                m_failed = TEST_PASSED;
                stopVBIFilter(0, 1);
                new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
            }
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
            if (VBIFilterEvent.EVENT_CODE_FIRST_VBI_DATA_AVAILABLE == event.getEventCode()
                    && findFilterToEvent(event) == 1)
            {
                m_failed = TEST_FAILED;
                System.out.println("<<<<<<FAILED: TestNegFiltering: VBI first event fired ");
                m_failedReason = "<<<<<<FAILED: TestNegFiltering: VBi first event fired";
                // Stop listening and detach filter
                stopVBIFilter(0, 1);
                new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
            }
        }

    }

    /**
     * @author Ryan
     * 
     *         Similar test to TestVBIBasicFilterFormat. This test will stop
     *         buffering once the 36 byte buffer is full. Validation done checks
     *         that buffered contents contain no nulls
     * 
     */
    public class TestNullFiltering extends TestVBIBasicFilterFormat
    {

        /**
         * @param locator
         *            : service to be tuned to
         * @param groupNum
         *            : VBI Group defined in the config file
         */
        public TestNullFiltering(OcapLocator locator, int groupNum)
        {
            super(locator, groupNum);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestNullFiltering : TestNullFiltering";
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (VBIFilterEvent.EVENT_CODE_BUFFER_FULL == event.getEventCode() && findFilterToEvent(event) == 1)
            {
                eventReceived = true;
                // Stop listening and detach filter
                stopVBIFilter(0, 1);
                new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
                // Verfiy the unit legnth is not and present the data
                new checkNullData(1, 0, m_groupNum, 0).ProcessCommand();
            }
        }

    }

    /**
     * @author Ryan
     * 
     *         Similar test to TestVBIBasicFilterFormat. This test will stop
     *         buffering once the 36 byte buffer is full. Test will validate the
     *         CC data that is been filtered TODO: Determine a more rigorous way
     *         to validate out CC1 and CC2 data
     */
    public class TestCC1Filtering extends TestVBIBasicFilterFormat
    {

        /**
         * @param locator
         *            : service to be tuned to
         * @param groupNum
         *            : VBI Group defined in the config file
         */
        public TestCC1Filtering(OcapLocator locator, int groupNum)
        {
            super(locator, groupNum);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestCC1Filtering : TestGroup" + m_groupNum;
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // TODO: Need to find a better way of validating CC data

            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (VBIFilterEvent.EVENT_CODE_BUFFER_FULL == event.getEventCode() && findFilterToEvent(event) == 1)
            {
                eventReceived = true;
                // Stop listening and detach filter
                stopVBIFilter(0, 1);
                new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
                // Verfiy the unit legnth is not and present the data
                new checkNullData(1, 0, m_groupNum, 0).ProcessCommand();
            }
        }
    }

    public class TestCC2Filtering extends TestVBIBasicFilterFormat
    {

        /**
         * @param locator
         *            : service to be tuned to
         * @param groupNum
         *            : VBI Group defined in the config file
         */
        public TestCC2Filtering(OcapLocator locator, int groupNum)
        {
            super(locator, groupNum);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestCC2Filtering : TestGroup" + m_groupNum;
        }
    }

    /**
     * @author Ryan
     * 
     *         Similar test to TestVBIBasicFilterFormat. This test will stop
     *         buffering once the first XDS data unit is received. Test will
     *         then validate the byte array against the byte arrays defined in
     *         the config file.
     */
    public class TestXDSFiltering extends TestVBIBasicFilterFormat
    {
        /**
         * @param locator
         *            : service to be tuned to
         * @param groupNum
         *            : VBI Group defined in the config file
         */
        public TestXDSFiltering(OcapLocator locator, int groupNum)
        {
            super(locator, groupNum);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestXDSFiltering : TestGroup" + m_groupNum;
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (VBIFilterEvent.EVENT_CODE_FIRST_VBI_DATA_AVAILABLE == event.getEventCode()
                    && findFilterToEvent(event) == 1)
            {
                eventReceived = true;
                // Stop listening and detach filter
                stopVBIFilter(0, 1);
                new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
                // Verfiy the data is represented in the Sting array
                new compareData(1, 0, m_groupNum, 0).ProcessCommand();
            }
        }
    }

    /**
     * @author Ryan
     * 
     *         Similar test to TestVBIBasicFilterFormat. This test will stop
     *         filters from collecting specific XDS packets once they get one
     *         data unit. ClosedCaptioning data will not stop buffering until
     *         the buffer is full. Test will then validate the byte array
     *         against the byte arrays defined in the config file for XDS
     *         packets. Test will validate that the CC data contains no nulls.
     *         To pass, the test will requires that all filters have bee
     *         sucessfully stopped.
     */
    public class TestMultipleFilters extends TestVBIBasicFilterFormat
    {

        protected boolean[] eventChecks;

        /**
         * @param locator
         *            : service to be tuned to
         * @param groupNum
         *            : VBI Group defined in the config file
         * @param filters
         *            : number of ilters to be used by the group
         */
        public TestMultipleFilters(OcapLocator locator, int groupNum, int filters)
        {
            super(locator, groupNum);
            // initialize chaeck states
            eventChecks = new boolean[filters];
            for (int i = 0; i < filters; i++)
            {
                eventChecks[i] = false;
            }
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestMultipleFilters : TestGroup" + m_groupNum;
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (VBIFilterEvent.EVENT_CODE_BUFFER_FULL == event.getEventCode() && findFilterToEvent(event) == 1)
            {
                // Stop listening and detach filter
                stopVBIFilter(0, 1);
                new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
                // Verfiy the unit legnth is not and present the data
                new checkNullData(1, 0, m_groupNum, 0).ProcessCommand();
                // Note that this check is complete
                System.out.println("VBIFilterEvent.EVENT_CODE_BUFFER_FULL test on Closed Captioning done");
                eventChecks[0] = true;
            }
            if (VBIFilterEvent.EVENT_CODE_FIRST_VBI_DATA_AVAILABLE == event.getEventCode()
                    && findFilterToEvent(event) != 1)
            {
                // Discover which filter
                int filter = findFilterToEvent(event);
                // Stop listening and detach filter
                stopVBIFilter(0, filter);
                new removeVBIFilterListener(filter, 0, this, 0).ProcessCommand();
                // Verfiy the data is represented in the Sting array
                new compareData(filter, 0, m_groupNum, 0).ProcessCommand();
                System.out.println("VBIFilterEvent.EVENT_CODE_FIRST_VBI_DATA_AVAILABLE test on filter " + filter
                        + " done");
                eventChecks[filter - 1] = true;
            }
            // Check to see if all the events have occured
            for (int i = 0; i < eventChecks.length; i++)
            {
                if (!eventChecks[i])
                {
                    System.out.println("Check done on filter " + (i + 1) + " has not generated an event");
                    break;
                }
                else if (i == (eventChecks.length - 1))
                {
                    System.out.println("PASSED: All events checks are complete");
                    eventReceived = true;
                }
            }
        }
    }

    /**
     * @author Ryan
     * 
     *         Similar test to TestVBIBasicFilterFormat. This test will stop
     *         buffering once the buffer full notification is sent. Test will
     *         then clear the data in the buffer and verify that the byte array
     *         is 0
     */
    public class TestClearFiltering extends TestVBIBasicFilterFormat
    {
        /**
         * @param locator
         *            : service to be tuned to
         * @param groupNum
         *            : VBI Group defined in the config file
         */
        public TestClearFiltering(OcapLocator locator, int groupNum)
        {
            super(locator, groupNum);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestClearFiltering";
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (VBIFilterEvent.EVENT_CODE_FIRST_VBI_DATA_AVAILABLE == event.getEventCode()
                    && findFilterToEvent(event) == 1)
            {
                eventReceived = true;
                // Stop listening and detach filter
                stopVBIFilter(0, 1);
                new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
                // Verfiy the data is represented in the Sting array
                // Check the buffer to see if it is clear
                new clearBuffers(1, 0, 0).ProcessCommand();
            }
        }
    }

    /*
     * @author Ryan This test is designed to test restarting of the filter. The
     * filter should be cleared on read and restarted
     */
    public class TestRestartFiltering extends TestVBIBasicFilterFormat
    {
        /**
         * @param locator
         *            : service to be tuned to
         * @param groupNum
         *            : VBI Group defined in the config file
         */
        private int msgCount;

        public TestRestartFiltering(OcapLocator locator, int groupNum)
        {
            super(locator, groupNum);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRestartFiltering";
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (VBIFilterEvent.EVENT_CODE_FIRST_VBI_DATA_AVAILABLE == event.getEventCode()
                    && findFilterToEvent(event) == 1)
            {
                if (msgCount != 1)
                {
                    eventReceived = false;
                    // Stop listening and detach filter
                    DVRTestRunnerXlet.log("Filter stopping");
                    stopVBIFilter(0, 1);
                    try
                    {
                        Thread.sleep(5000);
                    }
                    catch (Exception e)
                    {
                        System.out.println("Issues with sleep");
                        e.printStackTrace();
                    }
                    DVRTestRunnerXlet.log("Filter starting");
                    startVBIFilter(1, 1, 0);
                    msgCount = 1;
                }
                else
                {
                    eventReceived = true;
                    // Stop listening and detach filter
                    stopVBIFilter(0, 1);
                    new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
                    msgCount = 2;
                }
            }
        }
    }

}
