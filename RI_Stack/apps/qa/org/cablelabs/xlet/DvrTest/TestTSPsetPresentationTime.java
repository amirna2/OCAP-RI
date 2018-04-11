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
import javax.tv.service.selection.NormalContentEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;

import org.apache.log4j.Logger;
import org.ocap.dvr.TimeShiftProperties;
import org.ocap.net.OcapLocator;
import org.ocap.shared.media.TimeShiftControl;

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
 * the requirements in the ECR for typical and bounddary behavior, i.e., when
 * the presentation time falls within, or in the past or beyond the
 * BufferingRequest duration.
 * 
 * @author jspruiel
 * 
 */
public class TestTSPsetPresentationTime extends DvrTest
{

    TestTSPsetPresentationTime(Vector locators)
    {
        super(locators);
        m_locators = locators;
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestPresTimeFallsWithinBuffReqDur((OcapLocator) m_locators.elementAt(0),
                (OcapLocator) m_locators.elementAt(1)));
        return tests;
    }

    class TestPresTimeFallsWithinBuffReqDur extends TestCase
    {
        private static final boolean NORMAL_TEST_MODE = true;

        TestPresTimeFallsWithinBuffReqDur(OcapLocator locatorA, OcapLocator locatorB)
        {
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
            return "TestPresTimeFallsWithinBuffReqDur";
        }

        public void runTest()
        {
            m_log = Logger.getLogger("TestPresTimeFallsWithinBuffReqDur");
            m_log.info("runTest executing.");
            long base = 10000; // 10 seconds
            m_failed = TEST_PASSED;

            if (locatorToService(m_log, m_locatorA) == null)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log(m_failedReason = getName() + " null service; check config.properties file.");
                return;
            }

            // Assumptions:
            // Step 1 and 2. The EPG was launched and the channels keys are
            // mapped.

            // Step3. Create a time-shifted service context
            createATimeShiftedServiceContext();

            // Step 4 and 5. Assume user Presses the Channel 3 key.
            // We respond by selecting the Service.
            onSelectChannel3Event(base + 2000);

            // 60 seconds later.
            // Step 7 -10. Assume user selects the Pause key.
            // Content is buffered with a buffering request of
            // 300 seconds.
            onPauseKeyEvent(base + 2000 + 60000);

            // 40 seconds later.
            // Step 11 - 14.
            onSelectChannel4Event(base + 2000 + 60000 + 40000);

            // 50 seconds later.
            // Step 15 - 18 Use selects CHANNEL 3 again.
            // If the selection is performed within the 300
            // second duration of the buffering request, then
            // the media time stored during the onPauseKeyEvent
            // will fall between the pause time and the 300 duration.
            // In other words, that media time will be valid.
            onSelectChannel3Event_Again(base + 2000 + 60000 + 40000 + 50000);

            // 60 seconds later.
            // cleanup the service context used.
            scheduleCleanup(base + 2000 + 60000 + 40000 + 50000 + 70000);
            //
            // Start the test
            //
            m_eventScheduler.run(10000);

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

        private void scheduleCleanup(long time)
        {
            if (NORMAL_TEST_MODE == true)
                m_eventScheduler.scheduleCommand(new CancelBufferingRequest(m_log, "BufferingRequest", time));

            m_eventScheduler.scheduleCommand(new DestroyService(m_log, "SC", false, time + 5000));
        }

        /**
         * Set presentation time and rate and select channel 3.
         * 
         */
        private void onSelectChannel3Event_Again(long time)
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
                        m_log.debug("<<<<onSelectChannel3Event_Again:ProcessCommand ENTERED for user Selects Channel 3 again>>>>");
                        // Use the default Service Context
                        // Or find the object in the hashtable
                        m_serviceContext = (ServiceContext) findObject("SC");

                        // Get the Player
                        final Player player = getServicePlayer(m_serviceContext);
                        if (player == null)
                        {
                            System.out.println("\n\n\nonSelectChannel3Event_Again:ProcessCommand: Player is null: FAILURE\n\n\n");
                            m_failedReason = "Can't get Player";
                            m_failed = TEST_FAILED;
                            return;
                        }

                        player.addControllerListener(new ControllerListener()
                        {
                            public void controllerUpdate(ControllerEvent event)
                            {
                                m_log.debug("\n\n\nonSelectChannel3Event_Again:ProcessCommand:controllerUpdate event = "
                                        + event + "\n\n\n");
                                // remove when done.
                                player.removeControllerListener(this);
                            }
                        });// end

                        TimeShiftProperties tsp = (TimeShiftProperties) m_serviceContext;
                        m_log.debug("\n\n\nnonSelectChannel3Event_Again: Presentation params : "
                                + locatorToService(m_log, m_locatorA) + ", " + m_presentationTime + ", " + m_rate);

                        tsp.setPresentation(locatorToService(m_log, m_locatorA), m_presentationTime, m_rate, true, // action
                                false);// persistence
                        m_log.debug("\n\n\nnonSelectChannel3Event_Again:ProcessCommand: set the presentation time in SC\n\n\n");

                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        DVRTestRunnerXlet.log("onSelectChannel3Event_Again - Exception thrown in processing command");
                        m_failedReason = "onSelectChannel3Event_Again - Exception thrown in processing command" + e;
                        m_failed = TEST_FAILED;
                    }
                }
            });

            // Step 18. Select Channel 3.
            m_eventScheduler.scheduleCommand(new SelectService(m_log, "SC", m_locatorA, time + 2000)
            {

                public void ProcessCommand()
                {
                    m_log.debug("<<<<onSelectChannel3Event_Again:SelectService:ProcessCommand>>>>");
                    super.ProcessCommand();
                }

                // Override the event handler to print
                // in-your-face traces. Super removes listener.
                public void receiveServiceContextEvent(ServiceContextEvent ev)
                {
                    m_log.debug("onSelectChannel3Event_Again ServiceContextEvent = " + ev);
                    if (ev instanceof NormalContentEvent)
                    {
                        Player player = getServicePlayer(this.getServiceContext());
                        if (player == null)
                        {
                            m_log.debug("\n\n\nonSelectChannel3Event_Again:NormalContentEvent: got null playah.\n\n\n");
                        }
                        // call a validation method
                        if (assertPresTimeFallsWithinBuffReqDur(player) == false)
                        {
                            m_failed = TEST_FAILED;
                        }
                    }
                    else if (ev instanceof SelectionFailedEvent)
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason = "\n\n\nonSelectChannel3Event_Again: SelectionFailedEvent.\n\n\n";
                    }
                    super.receiveServiceContextEvent(ev);
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
                    m_log.debug("<<<<onSelectChannel4Event.GetMediaTime:ProcessCommand ENTERED>>>>");
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
                            System.out.println(m_failedReason = "onSelectChannel4Event.GetMediaTime - Can't get Player");
                            m_failed = TEST_FAILED;
                            return;
                        }

                        // Step 13. Stack gets mediaTime and rate from the SC
                        m_presentationTime = player.getMediaTime();
                        m_rate = player.getRate();
                        m_log.debug("\n\n\nonSelectChannel4Event.GetMediaTime:ProcessCommand presentation time in seconds = "
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
                                    m_failedReason = "nSelectChannel4Event SelectionFailedEvent";
                                }
                                super.receiveServiceContextEvent(ev);
                            }
                        }.ProcessCommand();
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        DVRTestRunnerXlet.log("onSelectChannel4Event.GetMediaTime:ProcessCommand - Exception thrown in processing command:"
                                + e);
                        m_failedReason = "onSelectChannel4Event.GetMediaTime:ProcessCommand - Exception thrown in processing command:"
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
                        Player player = getServicePlayer(m_serviceContext);
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
                            }
                        };
                        player.addControllerListener(cl);

                        m_log.debug("onPauseKeyEvent: get player's current rate = " + player.getRate());
                        m_rate = player.setRate(0);
                        m_log.debug("onPauseKeyEvent: setRate to zero. Player rate  = " + player.getRate());
                        m_log.debug("onPauseKeyEvent: current presentationTime = " + player.getMediaNanoseconds()
                                + "(ns)");
                        m_log.debug("onPauseKeyEvent: current presentationTime = " + 1E+9
                                * player.getMediaNanoseconds() + "(s)");

                        // Normal set to true. Defined for purpose of debugging
                        // an anomally.
                        if (NORMAL_TEST_MODE)
                        {
                            // Step 9 and 10: Create BufferingRequest to buffer
                            // channel 3.
                            // Step 10 is implicated because the service is
                            // passed into the
                            // CreateBufferingRequest command.
                            new CreateBufferingRequest(m_log, "BufferingRequest", m_locatorA, 300, 500, null, 0).ProcessCommand();

                            new StartBufferingRequest(m_log, "BufferingRequest", 0).ProcessCommand();
                        }

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
        private void onSelectChannel3Event(long time)
        {
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
        boolean assertPresTimeFallsWithinBuffReqDur(Player playah)
        {
            m_log.debug("assertPresTimeFallsWithinBuffReqDur: Entered");

            Control[] ctrls = playah.getControls();
            TimeShiftControl tsc = null;
            boolean found = false;
            boolean status = false;

            for (int i = 0; i < ctrls.length && (!found); i++)
            {
                m_log.debug("PControl[" + i + "] = " + ctrls[i]);
                if (ctrls[i] instanceof TimeShiftControl)
                {
                    found = true;
                    tsc = (TimeShiftControl) ctrls[i];
                }
            }

            if (found)
            {
                Time tBeginning = tsc.getBeginningOfBuffer();
                Time tEnd = tsc.getEndOfBuffer();

                m_log.debug("assertPresTimeFallsWithinBuffReqDur:<presTime,tsbBeggining,tsbEnd> = <"
                        + m_presentationTime.getSeconds() + ", " + tBeginning.getSeconds() + ", " + tEnd.getSeconds()
                        + ">");

                if (m_presentationTime.getSeconds() < tBeginning.getSeconds())
                {
                    m_log.debug(m_failedReason = "assertPresTimeFallsWithinBuffReqDur: presTime is less than tsb start time.");

                }
                else if (m_presentationTime.getSeconds() >= tBeginning.getSeconds()
                        && m_presentationTime.getSeconds() <= tEnd.getSeconds())
                {
                    m_log.debug("assertPresTimeFallsWithinBuffReqDur: presTime is inside tsb Buffer bounds.");
                    status = true;
                }
                else
                {
                    m_log.debug(m_failedReason = "assertPresTimeFallsWithinBuffReqDur: presTime is great than tsb End.");
                }

            }
            else
            {
                m_log.debug(m_failedReason = "assertPresTimeFallsWithinBuffReqDur: playah not found !!!");

            }

            return status;
        }

        private float m_rate;

        private Time m_presentationTime;

        private OcapLocator m_locatorA;

        private OcapLocator m_locatorB;
    }// end testcase

    private Logger m_log;

    private Vector m_locators;
}
