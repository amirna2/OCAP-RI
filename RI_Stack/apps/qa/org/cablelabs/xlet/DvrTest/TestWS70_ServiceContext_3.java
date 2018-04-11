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
 * Created on Jul 14, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.util.Vector;

import org.ocap.net.OcapLocator;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestWS70_ServiceContext_3 extends DvrTest
{

    /**
     * @param locators
     */
    TestWS70_ServiceContext_3(Vector locators)
    {
        super(locators);
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new TestBufferResize((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /**
     * @author Ryan
     * 
     *         TODO To change the template for this generated type comment go to
     *         Window - Preferences - Java - Code Style - Code Templates
     */
    public class TestBufferResize extends TestCase
    {

        private OcapLocator m_locator;

        TestBufferResize(OcapLocator locator)
        {
            m_locator = locator;
            m_eventScheduler = new EventScheduler();
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestBufferResize - " + m_locator;
        }

        /**
         * @param locator
         */
        public void runTest()
        {
            DVRTestRunnerXlet.log("TestBufferResize start: ");
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            // clear the schedule of pending tasks
            reset();

            // initialize Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext("SC1", 1000));

            // set up buffering - make it between 5 and 10 minutes
            m_eventScheduler.scheduleCommand(new setSCBuffering("SC1", 300, 600, false, false, 5000));

            // Select first service
            m_eventScheduler.scheduleCommand(new SelectService(m_locator, 20000));

            // set up buffering - make it between 20 and 40 minutes
            m_eventScheduler.scheduleCommand(new setSCBuffering("SC1", 1200, 2400, false, false, 35000));

            // Invoke a trick mode to verify the integity of buffering
            m_eventScheduler.scheduleCommand(new SetRate("SC1", 0.167, true, 60000));

            // Stop and destroy the Service Context
            m_eventScheduler.scheduleCommand(new StopBroadcastService(80000));
            m_eventScheduler.scheduleCommand(new DestroyBroadcastService(90000));

            m_eventScheduler.run(4000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
            }
        }
    }
}
