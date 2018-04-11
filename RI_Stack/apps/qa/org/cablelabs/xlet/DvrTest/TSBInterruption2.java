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

public class TSBInterruption2 extends DvrTest
{
    /*
     * Scenarios x: period prior to buffering start AX: altcontent period NX:
     * normalcontent period L: live point (may be in altcontent or
     * normalcontent)
     * 
     * CONFIGURATION1 only normalcontent | x | N1 L
     * 
     * CONFIGURATION2 only altcontent | x | A1 L
     * 
     * CONFIGURATION3 initial altcontent, with recovery to normalcontent | x |
     * A1 | N1 L
     * 
     * CONFIGURATION4 initial normalcontent, back to altcontent | x | N1 | A1 L
     * 
     * CONFIGURATION5 initial normalcontent, back to altcontent, recovery to
     * normalcontent | x | N1 | A1 | N2 L
     * 
     * CONFIGURATION6 initial altcontent, recovery to normalcontent, back to
     * altcontent | x | A1 | N1 | A2 L
     * 
     * CONFIGURATION7 initial altcontent, two recoveries to normalcontent (final
     * duration normalcontent) | x | A1 | N1 | A2 | N2 L
     * 
     * CONFIGURATION8 initial altcontent, two recoveries to normalcontent (final
     * duration altcontent) | x | A1 | N1 | A2 | N2 | A3 L
     * 
     * CONFIGURATION9 initial normalcontent, two altcontent durations (final
     * duration altcontent) | x | N1 | A1 | N2 | A2 L
     * 
     * CONFIGURATION10 initial normalcontent, two altcontent durations (final
     * duration altcontent)
     */

    public TSBInterruption2(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new Configuration1SetMediaTimeTest((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new Configuration1SetRateTest((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    /*
     * CONFIGURATION1 only normalcontent | x | N1 L
     */
    private class Configuration1SetMediaTimeTest extends TestCase
    {
        private OcapLocator locator;

        Configuration1SetMediaTimeTest(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("Configuration1SetMediaTimeTest : run test");
            m_failed = TEST_PASSED;
            reset();
            addInitialBufferingSelectionCommands(locator); // selection starts
                                                           // at 5 seconds

            // for this test, initial selection results in our initial NORMAL
            // CONTENT duration, so no additional commands needed for this
            // configuration
            // N1: post-buffering to LIVE POINT (8 secs to ?)
            /*
             * SetMediaTime tests: - X from LIVE to LIVE - X from LIVE to N1 - X
             * from LIVE to PRE-BUFFERING - X from N1 to N1 - X from N1 to LIVE
             * - X from N1 to PRE-BUFFERING - X from PRE-BUFFERING to
             * PRE-BUFFERING - X from PRE-BUFFERING to LIVE - X from
             * PRE-BUFFERING to N1
             */
            // MediaTime tests
            // from LIVE to LIVE
            addMoveToLiveCommand(10000);
            // from LIVE to N1
            // -- already at live, move to N1
            addSetMediaTimeFixedCommand(12, 25000);
            // from LIVE TO PRE-BUFFERING
            // -- reset back to live (reset tests from N1 to LIVE), then move to
            // PRE-BUFFERING
            addMoveToLiveCommand(30000);
            addMoveToPreBufferingCommand(35000);
            // from N1 to N1 (reset tests from PRE-BUFFERING to N1)
            // -- reset back to N1 (PRE-BUFFERING to N1), then move to a
            // different time in N1
            addSetMediaTimeFixedCommand(10, 40000);
            addSetMediaTimeFixedCommand(15, 45000);
            // from N1 to PRE-BUFFERING
            addMoveToPreBufferingCommand(50000);
            // from PRE-BUFFERING to PRE-BUFFERING
            addMoveToPreBufferingCommand(50500);
            addMoveToLiveCommand(61000);

            // SetRate tests
            // at live, rate 1
            addSetRateCommand(1.0F, 65000);
            addStopPresentationCommand(70000);
            execute(this);
        }

        public String getName()
        {
            return "Configuration1SetMediaTimeTest";
        }
    }

    private class Configuration1SetRateTest extends TestCase
    {
        private OcapLocator locator;

        Configuration1SetRateTest(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("Configuration1SetRateTest : run test");
            m_failed = TEST_PASSED;
            reset();
            addInitialBufferingSelectionCommands(locator); // selection starts
                                                           // at 5 seconds

            // for this test, initial selection results in our initial NORMAL
            // CONTENT duration, so no additional commands needed for this
            // configuration
            // N1: post-buffering to LIVE POINT (8 secs to ?)
            /*
             * SetRate tests: - at LIVE, rate 1 - at LIVE, rate 2 - at LIVE,
             * rate 0 - at LIVE, rate -1 - from LIVE, rate -2 - from N1, rate 1
             * - from N1, rate 2 - from N1, rate 0 - from N1, rate -1 - from N1,
             * rate -2 - from PRE-BUFFERING, rate 1 - from PRE-BUFFERING, rate 2
             * - from PRE-BUFFERING, rate 0 - from PRE-BUFFERING, rate -1 - from
             * PRE-BUFFERING, rate -2
             */

            // SetRate tests
            // at live, rate 1
            addSetRateCommand(1.0F, 10000);
            addStopPresentationCommand(15000);

            execute(this);
        }

        public String getName()
        {
            return "Configuration1SetRateTest";
        }
    }

    private void execute(TestCase test)
    {
        m_eventScheduler.run(4000);
        if (m_failed == TEST_FAILED)
        {
            DVRTestRunnerXlet.log(test.getName() + " completed: FAILED");
        }
        else
        {
            DVRTestRunnerXlet.log(test.getName() + " completed: PASSED");
        }
    }

    public void addInitialBufferingSelectionCommands(OcapLocator locator)
    {
        m_eventScheduler.scheduleCommand(new initServiceContext(2000));
        m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));
        m_eventScheduler.scheduleCommand(new SelectService(locator, 5000));
    }

    public void addAltContentDuration(long startTime)
    {
        m_eventScheduler.scheduleCommand(new TuneUnsync(startTime));
    }

    public void addNormalContentDuration(long startTime)
    {
        m_eventScheduler.scheduleCommand(new TuneSync(startTime));
    }

    public void addMoveToLiveCommand(long startTime)
    {
        m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", Float.MAX_VALUE, false, false, true, 0, 1, startTime));
    }

    public void addMoveToPreBufferingCommand(long startTime)
    {
        m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", 0, false, false, true, 0, 7, startTime));
        // assuming no more than 7 seconds before buffering started
    }

    public void addSetMediaTimeFixedCommand(float newMediaTimeSeconds, long startTime)
    {
        m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", newMediaTimeSeconds, false, false, true, 0, 1,
                startTime));
    }

    public void addSetRateCommand(float newRate, long startTime)
    {
        m_eventScheduler.scheduleCommand(new SetRate("", newRate, true, startTime));
    }

    public void addStopPresentationCommand(long startTime)
    {
        m_eventScheduler.scheduleCommand(new StopBroadcastService(startTime));
    }
}
