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

import javax.media.Control;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Player;
import javax.media.Time;
import javax.media.RateChangeEvent;
import javax.tv.service.Service;
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;

import org.apache.log4j.Logger;
import org.ocap.dvr.TimeShiftProperties;
import org.ocap.net.OcapLocator;
import org.ocap.shared.media.TimeShiftControl;

import org.cablelabs.xlet.DvrTest.DvrTest.CreateBufferingRequest;
import org.cablelabs.xlet.DvrTest.DvrTest.SelectService;
import org.cablelabs.xlet.DvrTest.DvrTest.StartBufferingRequest;
import org.cablelabs.xlet.DvrTest.DvrTest.TestCase;
import org.cablelabs.xlet.DvrTest.DvrTest.initServiceContext;
import org.cablelabs.xlet.DvrTest.DvrTest.setSCBuffering;

/**
 * The EPG anticipates a consumer may tune aways from a paused service and
 * creates a BufferingRequest in steps 9-10. When the consumer does tune away
 * from the paused service 3 in step 11, the service continues to be buffered in
 * the background, i.e without presentation, by the BufferingRequest. Before the
 * EPG selects the new service 4 it gets and stores the media time and rate of
 * the paused service in step 13. When the consumer selects service 3 in step 15
 * the EPG sets the presentation time and rate using the new
 * TimeShiftProperties.setPresentation method proposed by ECR 931. When the EPG
 * selects the service in 3 in step 18 the implementation is able to apply the
 * media time and rate asserted by the setPresentation method because the
 * service has been buffering since it was last presented. This test validates
 * the requirements in the ECR for the "persistent" flag to setPresentation()
 * being either true or false.
 * 
 * Test procedure is as follows: 1) Tune to service A. 2) Pause service A. 3)
 * Call setPresentation(). 4) Tune to service B. 5) Tune back to service A. 6)
 * Tune back to service B. 7) Tune back to service A.
 * 
 * Test criteria are as follows: In step 4, the settings from setPresentation()
 * should always be in effect.
 * 
 * In step 7, if "persistent" was true, the settings from setPresentation()
 * should still be in effect.
 * 
 * In step 7, if "persistent" was false, the settings from setPresentation()
 * should NOT still be in effect. The settings should have reverted to defaults.
 * 
 * @author peterk
 * @author jspruiel
 */
public class TestTSPsetPresentationPersistent extends DvrTest
{

    TestTSPsetPresentationPersistent(Vector locators)
    {
        super(locators);
        m_locators = locators;
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestPresentationPersistentFlag((OcapLocator) m_locators.elementAt(0),
                (OcapLocator) m_locators.elementAt(1), true // persistent flag
                                                            // state
        ));

        tests.addElement(new TestPresentationPersistentFlag((OcapLocator) m_locators.elementAt(0),
                (OcapLocator) m_locators.elementAt(1), false // persistent flag
                                                             // state
        ));

        return tests;
    }

    class TestPresentationPersistentFlag extends TestCase
    {
        TestPresentationPersistentFlag(OcapLocator locatorA, OcapLocator locatorB, boolean persistentState)
        {
            m_persistentState = persistentState;
            m_locatorA = locatorA;
            m_locatorB = locatorB;
            m_eventScheduler = new EventScheduler();
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test setPresentation when Persistent Flag is " + m_persistentState;
        }

        public void runTest()
        {
            long testMilliseconds = 10000; // 10 seconds

            m_log = Logger.getLogger("TestPresentationPersistentFlag");

            m_log.info("runTest executing.");
            m_failed = TEST_PASSED;

            m_serviceA = locatorToService(m_log, m_locatorA);

            if (m_serviceA == null)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log(m_failedReason = getName() + " null service");
                return;
            }

            // Assumptions:
            // The EPG was launched and the channels keys are mapped.

            // Create a time-shifted service context
            createATimeShiftedServiceContext();

            // User Presses the Channel 3 key.
            testMilliseconds += 30000;
            onSelectChannel3Event(testMilliseconds, 0);

            // Pause playback.
            // Content is buffered with a buffering request of
            // 300 seconds.
            testMilliseconds += 30000;
            onPauseKeyEvent(testMilliseconds);

            // preserve media time and rate with setPresentation()
            testMilliseconds += 25000;
            setPresentationOnChannel3(testMilliseconds);

            // Switch to channel 4.
            testMilliseconds += 25000;
            onSelectChannel4Event(testMilliseconds);

            // Tune to channel 3 again.
            // Settings from setPresentation should be in effect now.
            testMilliseconds += 25000;
            onSelectChannel3Event(testMilliseconds, 1);

            // Tune back to channel 4
            testMilliseconds += 25000;
            onSelectChannel4Event(testMilliseconds);

            // Switch back to channel 3 a third time.
            // If persistent was true, settings should have been saved from
            // when we called setPresentation.
            // If persistent was false, the settings should have reverted to
            // the defaults.
            testMilliseconds += 25000;
            onSelectChannel3Event(testMilliseconds, 2);

            // cancel buffering request that got created in the pause event
            testMilliseconds += 30000;
            scheduleCancelBufferingRequest(testMilliseconds);

            // cleanup the service context used.
            testMilliseconds += 30000;
            scheduleDestroyService(testMilliseconds);

            //
            // Start the test
            //
            m_eventScheduler.run(30000);

            //
            // After test completion, check for test success/failure
            //
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: INTERNAL ERROR-DISK FULL");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------------");
            }

        }

        private void scheduleCancelBufferingRequest(long time)
        {
            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(m_log, "BufferingRequest", time));
        }

        private void scheduleDestroyService(long time)
        {
            m_eventScheduler.scheduleCommand(new DestroyService(m_log, "SC", false, time));
        }

        /**
         * Set presentation time and rate and select channel 3.
         * 
         */
        private void setPresentationOnChannel3(long time)
        {
            // ON CHANNEL CHANGE set the presentation time of
            // the Service Context.
            m_eventScheduler.scheduleCommand( // setPresentationTime
            new EventScheduler.NotifyShell(time)
            {
                public void ProcessCommand()
                {
                    try
                    {
                        m_log.debug("<<<<setPresentationOnChannel3:ProcessCommand ENTERED for user Selects Channel 3 again>>>>");
                        // Use the default Service Context
                        // Or find the object in the hashtable
                        m_serviceContext = (ServiceContext) findObject("SC");

                        // Get the Player
                        final Player player = getServicePlayer(m_serviceContext);
                        if (player == null)
                        {
                            System.out.println("\n\n\nsetPresentationOnChannel3:ProcessCommand: Player is null: FAILURE\n\n\n");
                            m_failedReason = "Can't get Player";
                            m_failed = TEST_FAILED;
                            return;
                        }

                        player.addControllerListener(new ControllerListener()
                        {
                            public void controllerUpdate(ControllerEvent event)
                            {
                                m_log.debug("\n\n\nsetPresentationOnChannel3:ProcessCommand:controllerUpdate event = "
                                        + event + "\n\n\n");
                                // remove when done.
                                player.removeControllerListener(this);
                            }
                        });// end

                        TimeShiftProperties tsp = (TimeShiftProperties) m_serviceContext;

                        m_presentationTime = player.getMediaTime();
                        m_rate = 0; // paused

                        tsp.setPresentation(m_serviceA, m_presentationTime, m_rate, true, // action
                                m_persistentState);// persistent
                        m_log.debug("\n\n\nnsetPresentationOnChannel3:ProcessCommand: set the presentation time in SC\n\n\n");
                        m_checkPoint_2 = true;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        DVRTestRunnerXlet.log("setPresentationOnChannel3 - Exception thrown in processing command");
                        m_failedReason = "setPresentationOnChannel3 - Exception thrown in processing command" + e;
                        m_failed = TEST_FAILED;
                    }
                }
            });

        }

        private void onSelectChannel4Event(long time)
        {
            // User Selects Channel 4
            // On Channel Change - get the media time and store it.
            // Then select the new channel.
            m_eventScheduler.scheduleCommand( // getMediaTime
            new EventScheduler.NotifyShell(time)
            {
                public void ProcessCommand()
                {
                    m_log.debug("<<<<onSelectChannel4Event:ProcessCommand ENTERED>>>>");
                    try
                    {
                        m_serviceContext = (ServiceContext) findObject("SC");
                        if (m_serviceContext == null)
                        {
                            m_log.debug("onSelectChannel4Event findObject for SC ret null.");
                            m_failed = TEST_FAILED;
                            return;
                        }

                        // Get the Player
                        Player player = getServicePlayer(m_serviceContext);
                        if (player == null)
                        {
                            System.out.println(m_failedReason = "onSelectChannel4Event - Can't get Player");
                            m_failed = TEST_FAILED;
                            return;
                        }

                        // Step 13. Stack gets mediaTime and rate from the SC
                        m_presentationTime = player.getMediaTime();
                        m_rate = player.getRate();
                        m_log.debug("\n\n\nonSelectChannel4Event:ProcessCommand presentation time in seconds = "
                                + m_presentationTime.getSeconds() + " m_rate = " + m_rate + "\n\n\n");

                        // Step 14. NOW select Channel 4.
                        new SelectService(m_log, "SC", m_locatorB, 0)
                        {

                            // Override the select event handler to print
                            // in-your-face traces. Super removes listener.
                            public void receiveServiceContextEvent(ServiceContextEvent ev)
                            {
                                if (ev instanceof NormalContentEvent)
                                {
                                    m_log.debug("\n\n\nonSelectChannel4Event: " + ev + "\n\n\n");
                                }
                                if (ev instanceof SelectionFailedEvent)
                                {
                                    m_log.debug("\n\n\nonSelectChannel4Event: " + ev + "\n\n\n");
                                    m_failed = TEST_FAILED;
                                    m_failedReason = "onSelectChannel4Event SelectionFailedEvent";
                                }
                                super.receiveServiceContextEvent(ev);
                            }
                        }.ProcessCommand();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        DVRTestRunnerXlet.log("onSelectChannel4Event:ProcessCommand - Exception thrown in processing command:"
                                + e);
                        m_failedReason = "onSelectChannel4Event:ProcessCommand - Exception thrown in processing command:"
                                + e;
                        m_failed = TEST_FAILED;
                    }
                }
            });

        }

        /**
         * Performs the actions as if Pause key event was received. Steps 8 -
         * 10. BufferingRequest can buffer 300 seconds of content.
         * 
         * @param time
         */
        private void onPauseKeyEvent(long time)
        {
            m_eventScheduler.scheduleCommand(new EventScheduler.NotifyShell(time)
            {
                public void ProcessCommand()
                {

                    m_log.debug("<<<<onPauseKeyEvent:ProcessCommand>>>>");
                    try
                    {
                        // Get the service context.
                        m_serviceContext = (ServiceContext) findObject("SC");

                        // Get the Player
                        final Player player = getServicePlayer(m_serviceContext);
                        if (player == null)
                        {
                            System.out.println(m_failedReason = "onPauseKeyEvent:ProcessCommand - Did not get player, it was null");
                            m_failed = TEST_FAILED;
                            return;
                        }

                        // It is helpful to print events for purpose of
                        // debugging.
                        // We should see a rate change event.
                        ControllerListener cl = new ControllerListener()
                        {
                            public void controllerUpdate(ControllerEvent event)
                            {
                                m_log.debug("\n\n\nonPauseKeyEvent: ControllerListener event = " + event + "\n\n\n");

                                // update the media rate and time now that we're
                                // sure we're paused
                                if (event instanceof RateChangeEvent)
                                {
                                    m_presentationTime = player.getMediaTime();
                                    m_rate = player.getRate();
                                }
                            }
                        };
                        player.addControllerListener(cl);

                        m_log.debug("onPauseKeyEvent: current presentationTime = " + player.getMediaTime().getSeconds()
                                + " (s)");
                        m_log.debug("onPauseKeyEvent: current player rate = " + player.getRate());

                        m_log.debug("onPauseKeyEvent: setting media rate to 0.0...");
                        m_rate = player.setRate(0);

                        m_log.debug("onPauseKeyEvent: new player rate  = " + player.getRate());
                        m_log.debug("onPauseKeyEvent: new presentationTime = " + player.getMediaTime().getSeconds()
                                + " (s)");

                        // Step 9 and 10: Create BufferingRequest to buffer
                        // channel 3.
                        // Step 10 is implicated because the service is passed
                        // into the
                        // CreateBufferingRequest command.
                        new CreateBufferingRequest(m_log, "BufferingRequest", m_locatorA, 300, 500, null, 0).ProcessCommand();

                        new StartBufferingRequest(m_log, "BufferingRequest", 0).ProcessCommand();

                        player.removeControllerListener(cl);
                        cl = null;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        DVRTestRunnerXlet.log("onPauseKeyEvent:ProcessCommand - setMediaTimeBack - Exception caught:"
                                + e);
                        m_failedReason = "onPauseKeyEvent:ProcessCommand - setMediaTimeBack - Exception caught:" + e;
                        m_failed = TEST_FAILED;
                    }
                }
            });

        }

        /**
         * Selects channel 3, waits for the <code>NormalContentEvent</code>.
         * 
         * @param time
         * 
         */
        private void onSelectChannel3Event(long time, int stage)
        {
            final int myStage = stage;

            // schedule the selection command.
            m_eventScheduler.scheduleCommand(new SelectService(m_log, "SC", m_locatorA, time)
            {

                // Time for processing the command.
                public void ProcessCommand()
                {
                    m_log.debug("<<<<onSelectChannel3Event:SelectService:ProcessCommand>>>>");
                    super.ProcessCommand();
                }

                // Override the listener callback method just for the sake
                // of printing an in-your-face- trace. Listener is removed
                // by super.
                public void receiveServiceContextEvent(ServiceContextEvent ev)
                {
                    if (ev instanceof NormalContentEvent)
                    {
                        m_log.debug("\n\n\nonSelectChannel3Event: NormalContentEvent\n\n\n");

                        final ServiceContext sc = ev.getServiceContext();
                        final Player p = getServicePlayer(sc);

                        m_log.debug("\n\n\n\nonSelectChannel3Event: Media time is " + p.getMediaTime().getSeconds()
                                + " (s)\n\n\n\n");

                        if (validatePresentationPersistent(p, myStage) == false)
                        {
                            m_log.debug("\n\n\nonSelectChannel3Event_Again:ProcessCommand: Test validation FAILED!!!\n\n\n");
                            m_failed = TEST_FAILED;
                        }
                    }
                    if (ev instanceof SelectionFailedEvent)
                    {
                        m_log.debug("\n\n\nonSelectChannel3Event: SelectionFailedEVent\n\n\n");
                        m_failed = TEST_FAILED;
                        m_failedReason = "SelectChannel3Event: SelectionFailedEvent";
                    }
                    super.receiveServiceContextEvent(ev);
                }
            });
        }

        /**
         * Create a buffered <code>ServiceContext</code>.
         */
        private void createATimeShiftedServiceContext()
        {
            // create a service buffered context.

            new initServiceContext("SC", 0).ProcessCommand();
            new setSCBuffering(m_log, "SC", // name
                    300, // minDur
                    500, // maxDur
                    false, // don't last buffer
                    false, // forget last preference
                    0).ProcessCommand();
        }

        // Validate where the Presentation Begins.
        boolean validatePresentationPersistent(Player playah, int stage)
        {
            m_log.debug("validatePresentationPersistent: Entered");

            if (stage == 0) return true;

            Control[] ctrls = playah.getControls();
            TimeShiftControl tsc = null;
            boolean status = true;
            // m_failedReason =
            // "validatePresentationPersistent: Validation successful!";

            for (int i = 0; i < ctrls.length && tsc == null; i++)
            {
                if (ctrls[i] instanceof TimeShiftControl)
                {
                    tsc = (TimeShiftControl) ctrls[i];
                }
            }

            if (tsc == null)
            {
                status = false;
                m_failedReason = "validatePresentationPersistent: TimeShiftControl not found!!!";
                m_log.debug(m_failedReason);

                return status;
            }

            // Time originalTime = m_presentationTime;
            double originalRate = m_rate;

            m_presentationTime = playah.getMediaTime();
            m_rate = playah.getRate();

            // on the first tune back to channel 3 the media time and rate
            // should always stick
            //
            // on the second tune back to channel 3, the media time and rate
            // should only stick
            // if persistent was set to true.

            m_log.debug("validatePresentationPersistent: presentationTime in seconds = "
                    + m_presentationTime.getSeconds());

            if (stage == 1)
            {
                if (originalRate != m_rate)
                {
                    m_failedReason = "validatePresentationPersistent: Presentation rate is wrong!";
                    status = false;
                }
            }
            else if (stage == 2)
            {
                if (m_persistentState == true)
                {
                    if (originalRate != m_rate)
                    {
                        m_failedReason = "validatePresentationPersistent: Presentation rate did not stick!";
                        status = false;
                    }
                }
                else
                {
                    if (originalRate == m_rate)
                    {
                        m_failedReason = "validatePresentationPersistent: Non-persistent presentation rate should not have stuck.";
                        status = false;
                    }
                }
            }

            if (status == false) m_log.debug(m_failedReason);

            return status;
        }

        private boolean m_persistentState;

        private float m_rate;

        private Time m_presentationTime;

        private OcapLocator m_locatorA;

        private OcapLocator m_locatorB;

        private Service m_serviceA = null;

        private boolean m_checkPoint_1;

        private boolean m_checkPoint_2;
    }// end testcase

    private Logger m_log;

    private Vector m_locators;
}
