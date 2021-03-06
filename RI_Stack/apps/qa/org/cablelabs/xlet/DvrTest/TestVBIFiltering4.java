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

import org.ocap.net.OcapLocator;

import org.cablelabs.xlet.DvrTest.DvrTest.DestroyBroadcastService;

public class TestVBIFiltering4 extends VbiTest
{

    TestVBIFiltering4(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestExceptionBySetNotoficationByDataUnits((OcapLocator) m_locators.elementAt(0), 30));
        return tests;
    }

    public class TestExceptionBySetNotoficationByDataUnits extends TestCase
    {
        protected OcapLocator m_locator;

        protected boolean eventReceived;

        protected int m_groupNum;

        /*
         * @param locator : service to be tuned to
         * 
         * @param groupNum : VBI Group defined in the config file
         */
        public TestExceptionBySetNotoficationByDataUnits(OcapLocator locator, int groupNum)
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
            return "TestExceptionBySetNotoficationByDataUnits";
        }

        public void runTest()
        {
            m_failed = TEST_FAILED;
            eventReceived = false;
            // Clear event scheduler
            reset();

            // Intialize the Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext(1000));

            // Schedule the service selection call
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 3000));

            // Run the schedule
            m_eventScheduler.run(20000);

            // Create the filter group but catch exceptions thrown
            try
            {
                new createVBIFilterGroup(m_groupNum, 0, 0).ProcessCommand();
            }
            catch (IllegalArgumentException e)
            {
                m_failed = TEST_PASSED;
            }
            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log("!!!TEST FAILED : IllegalArgumentException not genrated");
                m_failedReason = "!!!TEST FAILED : IllegalArgumentException not genrated";
            }

            // Destroy the presenting service
            new DestroyBroadcastService(0).ProcessCommand();
            // Post results
            postResults(getName(), true);
        }

    }
}
