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
import org.ocap.service.AlternativeContentErrorEvent;

public class TestGainCtrl extends DvrTest
{
    float m_minDB;
    float m_maxDB;

    public TestGainCtrl(Vector locators, float minDB, float maxDB)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
        m_minDB = minDB;
        m_maxDB = maxDB;
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new ChangeAudioSignals((OcapLocator) m_locators.elementAt(0), m_minDB, m_maxDB));
        return tests;
    }


    private class ChangeAudioSignals extends TestCase
    {
        private OcapLocator locator;
        private float maxDB;
        private float minDB;

        ChangeAudioSignals (OcapLocator locator, float min, float max)
        {
            this.locator = locator;
            this.maxDB = max;
            this.minDB = min;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("ChangeAudioSignals: run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));


            // selecting mediatime that should be valid (inside buffer, not
            // prior to buffering start)
            m_eventScheduler.scheduleCommand(new SetAudioSignals(minDB, maxDB, 35000)); 

            m_eventScheduler.scheduleCommand(new StopBroadcastService(45000));

            m_eventScheduler.run(4000);
            if (m_failed == TEST_FAILED)
            {
                DVRTestRunnerXlet.log(getName() + " completed: FAILED");
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
            }
        }

        public String getName()
        {
            return "ChangeAudioSignals";
        }
    }

}
