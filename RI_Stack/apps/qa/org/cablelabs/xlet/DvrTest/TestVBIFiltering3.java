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

import org.havi.ui.HScene;
import org.ocap.media.VBIFilterEvent;
import org.ocap.media.VBIFilterListener;
import org.ocap.net.OcapLocator;

import org.cablelabs.xlet.DvrTest.VbiTest.clearBuffers;

public class TestVBIFiltering3 extends VbiTest
{

    private static final int ALL_FILTERS = 99;

    private HScene m_scene;

    /**
     * @param locators
     */
    TestVBIFiltering3(HScene scene, Vector locators)
    {
        super(locators);
        m_scene = scene;
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestRapidXDSfiltering((OcapLocator) m_locators.elementAt(0), 27));
        tests.addElement(new TestRapidXDSMultipleFilters((OcapLocator) m_locators.elementAt(0), 28));
        return tests;
    }

    public class TestRapidXDSfiltering extends TestCase implements VBIFilterListener
    {
        protected OcapLocator m_locator;

        protected int m_groupNum;

        protected int sampleCount;

        protected boolean eventReceived;

        protected static final int SAMPLE_SIZE = 100;

        public TestRapidXDSfiltering(OcapLocator locator, int groupNum)
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
            return "TestRapidXDSFiltering: Group " + m_groupNum;
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            eventReceived = false;
            sampleCount = 0;
            long startTime = System.currentTimeMillis();
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
            m_eventScheduler.run(60000);

            // check for event sent in the last minute
            if (!eventReceived)
            {
                DVRTestRunnerXlet.log("FAILED: No event was gerated in the time alloted");
                m_failed = TEST_FAILED;
            }
            else
            {
                while (sampleCount != SAMPLE_SIZE)
                {
                    if (startTime + 600000 < System.currentTimeMillis())
                    {
                        DVRTestRunnerXlet.log("FAILED: Unable to get all events genarted in the time alloted");
                        m_failedReason = "FAILED: Unable to get all events genarted in the time alloted";
                        m_failed = TEST_FAILED;
                        break;
                    }
                }
                DVRTestRunnerXlet.log("SampleCollection completed");
            }

            stopVBIFilter(0, 1);
            new removeVBIFilterListener(1, 0, this, 0).ProcessCommand();
            // Finally, detach the filter group
            detachGroup(0);
            // Destroy the presenting service
            new DestroyBroadcastService(0).ProcessCommand();
            // Post results
            postResults(getName(), true);
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // synchronized(event){
            if (VBIFilterEvent.EVENT_CODE_FIRST_VBI_DATA_AVAILABLE == event.getEventCode()
                    && findFilterToEvent(event) == 1)
            {
                eventReceived = true;
                DVRTestRunnerXlet.log("Receieved first unit");
                sampleCount = 1;
                new checkCRC(1, 0, 0).ProcessCommand();
            }
            if (VBIFilterEvent.EVENT_CODE_UNITS_NOTIFICATION == event.getEventCode())
            {
                eventReceived = true;
                ++sampleCount;
                DVRTestRunnerXlet.log("Receieved unit #" + sampleCount);
                new checkCRC(findFilterToEvent(event), 0, 0).ProcessCommand();
            }
            if (VBIFilterEvent.EVENT_CODE_BUFFER_FULL == event.getEventCode())
            {
                DVRTestRunnerXlet.log("Buffer has gotten full; dumping out and restarting");
                new clearBuffers(1, 0, 0).ProcessCommand();
                startVBIFilter(1, m_groupNum, 0);
            }
            // }
        }
    }

    public class TestRapidXDSMultipleFilters extends TestRapidXDSfiltering
    {
        protected static final int SAMPLE_SIZE = 100;

        public TestRapidXDSMultipleFilters(OcapLocator locator, int groupNum)
        {
            super(locator, groupNum);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestRapidXDSFiltering on Multiple Filters";
        }

        public void filterUpdate(VBIFilterEvent event)
        {
            // Check to see if the SC is the one attached
            // Verify that is a notification of first data unit recieved for the
            // proper filter
            if (VBIFilterEvent.EVENT_CODE_BUFFER_FULL == event.getEventCode() && findFilterToEvent(event) == 1)
            {
                eventReceived = true;
                // Verfiy the unit legnth is not and present the data
                new checkNullData(1, 0, m_groupNum, 0).ProcessCommand();
                // Note that this check is complete
                System.out.println("VBIFilterEvent.EVENT_CODE_BUFFER_FULL, empty buffer");
            }
            if (VBIFilterEvent.EVENT_CODE_UNITS_NOTIFICATION == event.getEventCode() && findFilterToEvent(event) != 1)
            {
                ++sampleCount;
                eventReceived = true;
                // Discover which filter
                int filter = findFilterToEvent(event);
                // Verfiy the data is represented in the Sting array
                new compareData(filter, 0, m_groupNum, 0).ProcessCommand();
                System.out.println("VBIFilterEvent.EVENT_CODE_UNITS_NOTIFICATION on filter " + filter);
            }
        }
    }

}
