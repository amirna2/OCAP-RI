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

public class TSBInterruption1 extends DvrTest
{
    public TSBInterruption1(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();
        tests.addElement(new SelectUnsyncSetMediaTimeIntoTSBRunToLivePoint((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectUnsyncSyncSetMediaTimeIntoInterruption((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectUnsyncSetMediaTimeIntoLivePointInterruption((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectUnsyncSyncUnsyncSetMediaTimeIntoNotLivePointInterruption(
                (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectUnsyncSyncSetRateNegativeSetMediaTimeIntoInterruption(
                (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectUnsyncSyncSetRateNegativeToBeginningOfContent((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectUnsync((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectUnsyncSyncTwice((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectSetMediaTimeIntoTSBNoInterruption((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectSetRateZeroSetMediaTimeForwardIntoTSBNoInterruption(
                (OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectNeverSyncedSetMediaTimeBack((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectSetLiveMultipleTimes((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectSetMediaTimeEventListenerNoInterruption((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SelectSetMediaTimeEventListenerWithInterruption((OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    private class SelectUnsyncSyncSetRateNegativeToBeginningOfContent extends TestCase
    {
        private OcapLocator locator;

        SelectUnsyncSyncSetRateNegativeToBeginningOfContent(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectUnsyncSyncSetRateNegativeToBeginningOfContent : run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));
            ServiceContextListenerCommand listenerCommand = new ServiceContextListenerCommand(4000);
            m_eventScheduler.scheduleCommand(listenerCommand);

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));
            m_eventScheduler.scheduleCommand(new TuneUnsync(25000));
            m_eventScheduler.scheduleCommand(new TuneSync(35000));
            m_eventScheduler.scheduleCommand(new SetRate("", -2.0F, true, 50000));
            // initial normalcontent event
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 90000));
            // alt content after tune sync toggle
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 91000));
            // normalcontent after tune sync
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 92000));
            // when playing backward, we cross an interruption - expect
            // altcontent.tuning_failure followed by normalcontent
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 93000));
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 94000));
            // when playing forward, we cross an interruption - expect
            // altcontent.tuningfailure followed by normalcontent
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 95000));
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 96000));

            m_eventScheduler.scheduleCommand(new StopBroadcastService(97000));

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
            return "SelectUnsyncSyncSetRateNegativeToBeginningOfContent";
        }
    }

    private class SelectUnsyncSyncSetMediaTimeIntoInterruption extends TestCase
    {
        private OcapLocator locator;

        SelectUnsyncSyncSetMediaTimeIntoInterruption(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectUnsyncSyncSetMediaTimeIntoInterruption : run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));
            ServiceContextListenerCommand listenerCommand = new ServiceContextListenerCommand(4000);
            m_eventScheduler.scheduleCommand(listenerCommand);

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));
            m_eventScheduler.scheduleCommand(new TuneUnsync(25000));
            m_eventScheduler.scheduleCommand(new TuneSync(50000));

            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", -30, true, false, true, 20, 5, 65000));
            // offset 20s because we'll start playing out at end of interruption (20 secs past 'expected') - also added threshold for buffering

            // initial normalcontent
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 70000));
            // tune unsync
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 71000));
            // re-sync
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 72000));
            // selecting into tsb, should get altcontent then normalcontent
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 73000));
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 74000));

            m_eventScheduler.scheduleCommand(new StopBroadcastService(75000));

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
            return "SelectUnsyncSyncSetMediaTimeIntoInterruption";
        }
    }

    private class SelectUnsyncSyncUnsyncSetMediaTimeIntoNotLivePointInterruption extends TestCase
    {
        private OcapLocator locator;

        SelectUnsyncSyncUnsyncSetMediaTimeIntoNotLivePointInterruption(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectUnsyncSyncUnsyncSetMediaTimeIntoNotLivePointInterruption : run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));
            ServiceContextListenerCommand listenerCommand = new ServiceContextListenerCommand(4000);
            m_eventScheduler.scheduleCommand(listenerCommand);

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));
            m_eventScheduler.scheduleCommand(new TuneUnsync(25000));
            m_eventScheduler.scheduleCommand(new TuneSync(50000));
            m_eventScheduler.scheduleCommand(new TuneUnsync(75000));

            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", -55, true, false, true, 20, 5, 90000));
            // offset 20s because we'll start playing out at end of interruption (20 secs past 'expected') - also added threshold for buffering

            // initial normalcontent
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 95000));
            // tune unsync
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 96000));
            // re-sync
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 97000));
            // tune unsync
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 98000));

            // selecting into interruption, should get altcontent followed by
            // normalcontent
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 99000));
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 100000));

            m_eventScheduler.scheduleCommand(new TuneSync(101000));
            m_eventScheduler.scheduleCommand(new StopBroadcastService(102000));

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
            return "SelectUnsyncSyncUnsyncSetMediaTimeIntoNotLivePointInterruption";
        }
    }

    private class SelectUnsyncSetMediaTimeIntoLivePointInterruption extends TestCase
    {
        private OcapLocator locator;

        SelectUnsyncSetMediaTimeIntoLivePointInterruption(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectUnsyncSetMediaTimeIntoLivePointInterruption : run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));
            ServiceContextListenerCommand listenerCommand = new ServiceContextListenerCommand(4000);
            m_eventScheduler.scheduleCommand(listenerCommand);

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));

            m_eventScheduler.scheduleCommand(new TuneUnsync(25000));

            // since we're currently in an interruption and playing out at
            // positive rate, we should stay at the live point/altcontent and generate an ACEE for the interruption and one )
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", -10, true, true, false, 10, 1, 45000));
            // 10 seconds ago was altcontent, so we won't change our mediatime

            // initial normalcontent
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 70000));
            // tune unsync
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 71000));
            //setmediatime didn't 'encounter' an interruption (already at the interruption) - receive altcontent event
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 73000));

            m_eventScheduler.scheduleCommand(new TuneSync(74000));
            m_eventScheduler.scheduleCommand(new StopBroadcastService(75000));

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
            return "SelectUnsyncSetMediaTimeIntoLivePointInterruption";
        }
    }

    private class SelectUnsyncSyncSetRateNegativeSetMediaTimeIntoInterruption extends TestCase
    {
        private OcapLocator locator;

        SelectUnsyncSyncSetRateNegativeSetMediaTimeIntoInterruption(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectUnsyncSyncSetRateNegativeSetMediaTimeIntoInterruption : run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));
            ServiceContextListenerCommand listenerCommand = new ServiceContextListenerCommand(4000);
            m_eventScheduler.scheduleCommand(listenerCommand);

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));
            m_eventScheduler.scheduleCommand(new TuneUnsync(25000));
            m_eventScheduler.scheduleCommand(new TuneSync(50000));
            m_eventScheduler.scheduleCommand(new SetRate("", -1.0d, true, 70000));

            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", -30, true, false, true, -10, 5, 75000));
            // offset -10s because we'll start playing out prior to interruption (10 secs before 'expected') - also added threshold for buffering

            // initial normalcontent
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 78000));
            // tune unsync
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 79000));
            // re-sync
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 80000));
            // selecting into tsb, should get altcontent then normalcontent
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 81000));
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 82000));

            m_eventScheduler.scheduleCommand(new StopBroadcastService(82000));

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
            return "SelectUnsyncSyncSetRateNegativeSetMediaTimeIntoInterruption";
        }
    }

    private class SelectUnsyncSetMediaTimeIntoTSBRunToLivePoint extends TestCase
    {
        private OcapLocator locator;

        SelectUnsyncSetMediaTimeIntoTSBRunToLivePoint(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectUnsyncSetMediaTimeIntoTSBRunToLivePoint : run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));
            ServiceContextListenerCommand listenerCommand = new ServiceContextListenerCommand(4000);
            m_eventScheduler.scheduleCommand(listenerCommand);

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));
            m_eventScheduler.scheduleCommand(new TuneUnsync(20000));
            //jump to 10 seconds before the unsync (should trigger alt/normal since we were at the live point interruption and
            //crossed the interruption into content
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", -32, true, false, true, 0, 3, 45000));
            // offset zero because expected mediatime should equal actual
            // initial normalcontent event
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 65000));
            // alt content after tune sync toggle
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 66000));
            // alt content due to crossing the interruption (we are at the live point, but still signal interruption)
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 67000));
            // normalcontent interuption notification
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 68000));
            //re-entering altcontent at the live point
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 69000));

            m_eventScheduler.scheduleCommand(new TuneSync(70000));
            m_eventScheduler.scheduleCommand(new StopBroadcastService(71000));

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
            return "SelectUnsyncSetMediaTimeIntoTSBRunToLivePoint";
        }
    }

    private class SelectSetMediaTimeEventListenerWithInterruption extends TestCase
    {
        private OcapLocator locator;

        SelectSetMediaTimeEventListenerWithInterruption(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectSetMediaTimeEventListenerWithInterruption: run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));
            // selecting mediatime that should be valid (inside buffer, not
            // prior to buffering start)
            MediaTimeEventListenerImpl eventListener = new MediaTimeEventListenerImpl();

            m_eventScheduler.scheduleCommand(new TuneUnsync(20000));
            m_eventScheduler.scheduleCommand(new TuneSync(50000));

            // intentionally setting the alarm -after- we've crossed that time
            // (35 sec absolute)
            m_eventScheduler.scheduleCommand(new SetMediaTimeEvent(1, eventListener, 35, true, 51000));

            // back up so we go across the event (should fire)
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack(-50, true, 60000));
            //go across the event in a forward direction (should fire)
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack(50, true, 62000));
            // back up so we go across the event (should fire)
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack(-50, true, 63000));
            //go across the event in a forward direction (should fire)
            m_eventScheduler.scheduleCommand(new SetRate("", 32.0d, true, 65000));
            //now go across in the backward direction (should fire)
            m_eventScheduler.scheduleCommand(new SetRate("", -32.0d, true, 95000));
            //let this be long enough to play forward at rate 1.0 from beginning of presentation
            m_eventScheduler.scheduleCommand(new StopBroadcastService(145000));
            //ensure we encountered the event six times
            m_eventScheduler.scheduleCommand(new ConfirmMediaTimeEventReceivedCommand(eventListener, 1, 146000));
            m_eventScheduler.scheduleCommand(new ConfirmMediaTimeEventReceivedCommand(eventListener, 1, 147000));
            m_eventScheduler.scheduleCommand(new ConfirmMediaTimeEventReceivedCommand(eventListener, 1, 148000));
            m_eventScheduler.scheduleCommand(new ConfirmMediaTimeEventReceivedCommand(eventListener, 1, 149000));
            m_eventScheduler.scheduleCommand(new ConfirmMediaTimeEventReceivedCommand(eventListener, 1, 150000));
            m_eventScheduler.scheduleCommand(new ConfirmMediaTimeEventReceivedCommand(eventListener, 1, 151000));
            //should receive 6 (should be no more in the list)
            m_eventScheduler.scheduleCommand(new ConfirmMediaTimeEventNotReceivedCommand(eventListener, 152000));
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
            return "SelectSetMediaTimeEventListenerWithInterruption";
        }
    }

    private class SelectSetMediaTimeEventListenerNoInterruption extends TestCase
    {
        private OcapLocator locator;

        SelectSetMediaTimeEventListenerNoInterruption(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectSetMediaTimeEventListenerNoInterruption: run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));
            // selecting mediatime that should be valid (inside buffer, not
            // prior to buffering start)
            MediaTimeEventListenerImpl eventListener = new MediaTimeEventListenerImpl();
            m_eventScheduler.scheduleCommand(new SetMediaTimeEvent(1, eventListener, 5, false, 15000));
            // back up so we go across the event again
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack(-25, true, 40000));
            // now play backward and encounter the same event again
            m_eventScheduler.scheduleCommand(new SetRate("", -2.0d, true, 55000));
            m_eventScheduler.scheduleCommand(new StopBroadcastService(70000));
            // event encountered via initial playout
            m_eventScheduler.scheduleCommand(new ConfirmMediaTimeEventReceivedCommand(eventListener, 1, 71000));
            // same event encountered after setmediatime before event, forward
            // playout
            m_eventScheduler.scheduleCommand(new ConfirmMediaTimeEventReceivedCommand(eventListener, 1, 72000));
            // same event encountered after reverse playout
            m_eventScheduler.scheduleCommand(new ConfirmMediaTimeEventReceivedCommand(eventListener, 1, 73000));

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
            return "SelectSetMediaTimeEventListenerNoInterruption";
        }
    }

    private class SelectSetMediaTimeIntoTSBNoInterruption extends TestCase
    {
        private OcapLocator locator;

        SelectSetMediaTimeIntoTSBNoInterruption(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectSetMediaTimeIntoTSBNoInterruption: run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));
            // selecting mediatime that should be valid (inside buffer, not
            // prior to buffering start)
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", -15, true, false, true, 0, 1, 35000)); // offset
                                                                                                             // zero
                                                                                                             // because
                                                                                                             // expected
                                                                                                             // mediatime
                                                                                                             // should
                                                                                                             // equal
                                                                                                             // actual
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", 5, true, false, true, 0, 1, 40000)); // offset
                                                                                                           // zero
                                                                                                           // because
                                                                                                           // expected
                                                                                                           // mediatime
                                                                                                           // should
                                                                                                           // equal
                                                                                                           // actual

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
            return "SelectSetMediaTimeIntoTSBNoInterruption";
        }
    }

    private class SelectSetRateZeroSetMediaTimeForwardIntoTSBNoInterruption extends TestCase
    {
        private OcapLocator locator;

        SelectSetRateZeroSetMediaTimeForwardIntoTSBNoInterruption(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectSetRateZeroSetMediaTimeForwardIntoTSBNoInterruption: run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));
            // selecting mediatime that should be valid (inside buffer, not
            // prior to buffering start)
            m_eventScheduler.scheduleCommand(new SetRate("", 0.0d, true, 20000));
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", 5, true, false, true, 0, 1, 35000)); // offset
                                                                                                           // zero
                                                                                                           // because
                                                                                                           // expected
                                                                                                           // mediatime
                                                                                                           // should
                                                                                                           // equal
                                                                                                           // actual
            m_eventScheduler.scheduleCommand(new SetRate("", 1.0d, true, 45000));

            m_eventScheduler.scheduleCommand(new StopBroadcastService(55000));

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
            return "SelectSetRateZeroSetMediaTimeForwardIntoTSBNoInterruption";
        }
    }

    private class SelectUnsyncSyncTwice extends TestCase
    {
        private OcapLocator locator;

        SelectUnsyncSyncTwice(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("TestTSBInterruptionSelectUnsyncSync : run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));
            ServiceContextListenerCommand listenerCommand = new ServiceContextListenerCommand(4000);
            m_eventScheduler.scheduleCommand(listenerCommand);

            m_eventScheduler.scheduleCommand(new SelectService(locator, 6000));
            m_eventScheduler.scheduleCommand(new TuneUnsync(15000));
            m_eventScheduler.scheduleCommand(new TuneSync(25000));
            m_eventScheduler.scheduleCommand(new TuneUnsync(35000));
            m_eventScheduler.scheduleCommand(new TuneSync(45000));
            // initial normalcontent event
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 55000));
            // alt content after tune sync toggle to unsync
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 56000));
            // normalcontent after tune sync toggle to sync
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 57000));
            // alt content after tune sync toggle to unsync
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 58000));
            // normalcontent after tune sync toggle to sync
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 59000));

            m_eventScheduler.scheduleCommand(new StopBroadcastService(60000));

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
            return "SelectUnsyncSyncTwice";
        }
    }

    private class SelectSetLiveMultipleTimes extends TestCase
    {
        private OcapLocator locator;

        SelectSetLiveMultipleTimes(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectSetLiveMultipleTimes : run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));
            m_eventScheduler.scheduleCommand(new SelectService(locator, 5000));
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", Float.MAX_VALUE, false, false, true, 0, 1, 10000));
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", Float.MAX_VALUE, false, false, true, 0, 1, 15000));
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", Float.MAX_VALUE, false, false, true, 0, 1, 20000));
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", Float.MAX_VALUE, false, false, true, 0, 1, 25000));
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", Float.MAX_VALUE, false, false, true, 0, 1, 30000));
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack("", Float.MAX_VALUE, false, false, true, 0, 1, 35000));
            m_eventScheduler.scheduleCommand(new StopBroadcastService(40000));

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
            return "SelectSetLiveMultipleTimes";
        }
    }

    private class SelectNeverSyncedSetMediaTimeBack extends TestCase
    {
        private OcapLocator locator;

        SelectNeverSyncedSetMediaTimeBack(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectNeverSyncedSetMediaTimeBack : run test");
            m_failed = TEST_PASSED;
            reset();
            // this test assumes the NI has been modified to not signal tuned
            // sync..tune unsync notification currently fires after 30 seconds
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));
            ServiceContextListenerCommand listenerCommand = new ServiceContextListenerCommand(4000);
            m_eventScheduler.scheduleCommand(listenerCommand);
            m_eventScheduler.scheduleCommand(new SelectService(locator, 5000));
            // this should NOT result in a started tsb session - we should just
            // end up w/a mediatime event representing live
            m_eventScheduler.scheduleCommand(new SetMediaTimeBack(-20, true, 40000));
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 45000));

            m_eventScheduler.scheduleCommand(new StopBroadcastService(46000));

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
            return "SelectNeverSyncedSetMediaTimeBack";
        }
    }

    private class SelectUnsync extends TestCase
    {
        private OcapLocator locator;

        SelectUnsync(OcapLocator locator)
        {
            this.locator = locator;
        }

        public void runTest()
        {
            DVRTestRunnerXlet.log("SelectUnsync : run test");
            m_failed = TEST_PASSED;
            reset();
            m_eventScheduler.scheduleCommand(new initServiceContext(2000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(60, 120, 3000));
            ServiceContextListenerCommand listenerCommand = new ServiceContextListenerCommand(4000);
            m_eventScheduler.scheduleCommand(listenerCommand);
            m_eventScheduler.scheduleCommand(new SelectService(locator, 5000));
            m_eventScheduler.scheduleCommand(new ConfirmNormalContentEventReceivedCommand(listenerCommand, 12000));
            m_eventScheduler.scheduleCommand(new TuneUnsync(15000));
            m_eventScheduler.scheduleCommand(new ConfirmAlternativeContentErrorEventReceivedCommand(listenerCommand,
                    AlternativeContentErrorEvent.TUNING_FAILURE, 18000));
            m_eventScheduler.scheduleCommand(new TuneSync(20000));
            m_eventScheduler.scheduleCommand(new StopBroadcastService(21000));

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
            return "SelectUnsync";
        }
    }
}
