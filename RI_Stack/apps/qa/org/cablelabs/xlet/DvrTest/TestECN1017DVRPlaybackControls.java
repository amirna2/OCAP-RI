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
 * Created on Feb 7, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Vector;

import javax.media.Control;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Manager;
import javax.media.Player;
import javax.media.RealizeCompleteEvent;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.Time;

import org.dvb.media.VideoPresentationControl;
import org.havi.ui.HScene;
import org.havi.ui.HVideoComponent;
import org.ocap.dvr.OcapRecordedService;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.shared.media.EndOfContentEvent;
import org.ocap.shared.media.FrameControl;
import org.ocap.shared.media.MediaTimeFactoryControl;

/**
 * @author knunzio
 * 
 */
public class TestECN1017DVRPlaybackControls extends DvrTest
{

    private HScene sc = null;

    TestECN1017DVRPlaybackControls(HScene scene, Vector locators)
    {
        super(locators);
        sc = scene;
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestECN1017RecordingPlaybackUsingJMF(sc, (OcapLocator) m_locators.elementAt(0)));
        return tests;
    }

    public class TestECN1017RecordingPlaybackUsingJMF extends TestCase
    {
        private HScene sc;

        TestECN1017RecordingPlaybackUsingJMF(HScene scene, OcapLocator locator)
        {
            sc = scene;
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestECN1017RecordingPlaybackUsingJMF";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 2000, 60000, 500));

            // Count the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 80000));

            // Schedule the record call
            m_eventScheduler.scheduleCommand(new TestECN1017SelectRecordedServiceUsingJMFPlayer("Recording1", 81000, sc));

            m_eventScheduler.run(80000); // wait ~40 secs for recording to play
                                         // through

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestECN1017RecordingPlaybackUsingJMF completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                    DVRTestRunnerXlet.log("TestECN1017RecordingPlaybackUsingJMF completed: FAILED");
                    DVRTestRunnerXlet.log("---------------------------------------------------------");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("---------------------------------------------------------");
                DVRTestRunnerXlet.log("TestECN1017RecordingPlaybackUsingJMF completed: PASSED");
                DVRTestRunnerXlet.log("---------------------------------------------------------");
            }
        }

        private OcapLocator m_locator;
    }

    class TestECN1017SelectRecordedServiceUsingJMFPlayer extends EventScheduler.NotifyShell implements
            ControllerListener, ComponentListener
    {
        TestECN1017SelectRecordedServiceUsingJMFPlayer(String recording, long triggerTime, HScene sc)
        {
            super(triggerTime);
            m_recording = recording;
            m_rr = null;
            scene = sc;
        }

        TestECN1017SelectRecordedServiceUsingJMFPlayer(OcapRecordingRequest rr, long triggerTime, HScene sc)
        {
            super(triggerTime);
            m_recording = null;
            m_rr = rr;
            scene = sc;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<TestECN1017SelectRecordedServiceUsingJMFPlayer::ProcessCommand>>>>");

            OcapRecordingRequest rr;

            // were we given a recording list entry, or do we look it up?
            if (m_rr == null)
                rr = (OcapRecordingRequest) findObject(m_recording);
            else
                rr = m_rr;

            if (rr == null)
            {
                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - entry not found!" + m_recording);
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in TestECN1017SelectRecordedServiceUsingJMFPlayer due to unfound recording: "
                        + m_recording);
                m_failedReason = "DvrTest: Flagged FAILURE in TestECN1017SelectRecordedServiceUsingJMFPlayer due to unfound recording: "
                        + m_recording;
                return;
            }

            OcapRecordedService rsvc = null;
            try
            {
                rsvc = (OcapRecordedService) rr.getService();
            }
            catch (Exception e)
            {
                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - Exception obtaining service."
                        + m_recording);
                m_failed = TEST_FAILED;
                m_failedReason = "TestECN1017SelectRecordedServiceUsingJMFPlayer - Exception obtaining service."
                        + m_recording;
                e.printStackTrace();
            }
            if (rsvc == null)
            {
                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - Service not found!" + m_recording);
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("TestECN1017SelectRecordedServiceUsingJMFPlayer: Flagged FAILURE in TestECN1017SelectRecordedServiceUsingJMFPlayer due to failed getService() for "
                        + m_recording);
                m_failedReason = "TestECN1017SelectRecordedServiceUsingJMFPlayer: Flagged FAILURE in TestECN1017SelectRecordedServiceUsingJMFPlayer due to failed getService() for "
                        + m_recording;
                return;
            }

            DvrEventPrinter.printRecording(rr);
            System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer\n");

            if (state == UNREALIZED)
            {
                state = CREATING;
                try
                {
                    // Create the Player and wait for it to be realized.
                    System.out.println("\nMediaLocator: " + rsvc.getMediaLocator() + "\n");
                    player = Manager.createPlayer(rsvc.getMediaLocator());
                    System.out.println("\nplayer: " + player + "\n");
                    player.addControllerListener(this);
                    state = REALIZING;
                    player.realize();
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("TestECN1017SelectRecordedServiceUsingJMFPlayer: Flagged FAILURE in creating JMF player: "
                            + e.toString());
                    m_failedReason = "TestECN1017SelectRecordedServiceUsingJMFPlayer: Flagged FAILURE in creating JMF player: "
                            + e.toString();

                }
            }
            else
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("TestECN1017SelectRecordedServiceUsingJMFPlayer: Flagged FAILURE in SelectRecordedServiceUsingJMFPlayer: Player not in unrealized state");
                m_failedReason = "TestECN1017SelectRecordedServiceUsingJMFPlayer: Flagged FAILURE in SelectRecordedServiceUsingJMFPlayer: Player not in unrealized state";
            }
        }

        public void controllerUpdate(ControllerEvent event)
        {
            System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - controllerUpdate ( " + event + ")\n");

            synchronized (this)
            {
                try
                {
                    if (event instanceof RealizeCompleteEvent)
                    {
                        System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer controllerUpdate - RealizeCompleteEvent\n");

                        Control[] controlArray = player.getControls();
                        System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer getting controls...length="
                                + controlArray.length + "\n");

                        for (int ii = 0; ii < controlArray.length; ii++)
                        {
                            System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer getting control - index:"
                                    + ii + "control: " + controlArray[ii].toString());
                            if (controlArray[ii] instanceof MediaTimeFactoryControl)
                            {
                                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - Found MediaTimeFactorControl.\n");

                            }
                            else if (controlArray[ii] instanceof FrameControl)
                            {
                                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - Found FrameControl.\n");
                            }
                            else
                            {
                                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - Found control.\n");
                            }
                        }

/* TODO: VideoComponentControl is not a spec compliant class, remove its use
                        VideoComponentControl vidCtrl = (VideoComponentControl) player.getControl("org.ocap.media.VideoComponentControl");
                        video = (HVideoComponent) vidCtrl.getVisualComponent();
                        video.setVisible(false);
                        video.addComponentListener(this);
                        video.setBounds(getBounds());
                        System.out.println("add video: " + video);
                        scene.add(video);
                        video.setVisible(true);
*/
                        player.start();
                    }
                    else if (event instanceof StartEvent)
                    {
                        System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer controllerUpdate - StartEvent\n");
                        Control[] controlArray = player.getControls();

                        for (int ii = 0; ii < controlArray.length; ii++)
                        {
                            System.out.println("Event == StartEvent TestECN1017SelectRecordedServiceUsingJMFPlayer getting control - index:"
                                    + ii + "control: " + controlArray[ii].toString());
                            if (controlArray[ii] instanceof MediaTimeFactoryControl)
                            {
                                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - Found MediaTimeFactorControl.\n");
                                MediaTimeFactoryControl mtfc = (MediaTimeFactoryControl) controlArray[ii];
                                Time relativeTime = mtfc.getRelativeTime(1000);
                                System.out.println("getRelativeTime returned: " + relativeTime.getSeconds());

                                relativeTime = mtfc.setTimeApproximations(new Time(5000), true);
                                System.out.println("setTimeApprox forward returned: " + relativeTime.getSeconds());
                                relativeTime = mtfc.setTimeApproximations(new Time(5000), false);
                                System.out.println("setTimeApprox back returned: " + relativeTime.getSeconds());

                            }
                            else if (controlArray[ii] instanceof FrameControl)
                            {
                                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - Found FrameControl.\n");
                                FrameControl fc = (FrameControl) controlArray[ii];
                                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - pausing before move.\n");
                                player.setRate(0);
                                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - moving forward.\n");
                                fc.move(true);
                                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - moving back.\n");
                                fc.move(false);
                                player.setRate(1);
                            }
                            else
                            {
                                System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer - Found control.\n");
                            }
                        }
                        System.out.println("pop to front");
                        scene.popToFront(video);
                        System.out.println("setVisible\n");
                        video.setVisible(true);
                        System.out.println("<<<<<<<<<<Redrawing of the screen>>>>>>>>>>");
                        Rectangle bounds = video.getBounds();
                        scene.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
                        m_failed = TEST_FAILED; // set to failed with intent
                                                // that the EndOfContentEvent or
                                                // StopEvent is called
                    }
                    else if (event instanceof EndOfContentEvent)
                    {
                        System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer controllerUpdate - End of Content event\n");
                        scene.remove(video);
                        System.out.println("<<<<<<<<<<Redrawing of the screen>>>>>>>>>>");
                        Rectangle bounds = video.getBounds();
                        scene.repaint(bounds.x, bounds.y, bounds.width, bounds.height);
                        video = null;
                        player.stop();
                        m_failed = TEST_PASSED; // got the EndofContent event -
                                                // test passes
                    }
                    else if (event instanceof StopEvent)
                    {
                        System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer controllerUpdate - Stop event\n");
                        player.close();
                        player.removeControllerListener(this);
                        m_failed = TEST_PASSED; // got the StopEvent - test
                                                // passes
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("TestECN1017SelectRecordedServiceUsingJMFPlayer: Flagged FAILURE in JMF player: "
                            + e.toString());
                    m_failedReason = "TestECN1017SelectRecordedServiceUsingJMFPlayer: Flagged FAILURE in JMF player: "
                            + e.toString();
                }
            }
        }

        public Control getControllerToPause(Control[] controls)
        {
            for (int ii = 0; ii < controls.length; ii++)
            {
                if (controls[ii] instanceof VideoPresentationControl)
                {
                    return controls[ii];
                }
            }
            return null;
        }

        public void componentShown(ComponentEvent e)
        {
            System.out.println("componentShown: " + e + "\n");

            synchronized (this)
            {
                // After receiving the PaintEvent, start the decoder and repaint
                // to change from opaque to transparent to PIP video underneath.
                if (e.getID() == ComponentEvent.COMPONENT_SHOWN)
                {
                    player.start();
                }
            }
        }

        public void componentResized(ComponentEvent e)
        {
            System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer componentResized...\n");
            System.out.println("componentResized: " + e + "\n");
        }

        public void componentMoved(ComponentEvent e)
        {
            System.out.println("componentMoved: " + e + "\n");
        }

        public void componentHidden(ComponentEvent e)
        {
            System.out.println("TestECN1017SelectRecordedServiceUsingJMFPlayer componentHidden...\n");
            System.out.println("componentHidden: " + e + "\n");
        }

        private Rectangle getBounds()
        {
            return BOUNDS[0];
        }

        public class State
        {
            String name;

            State(String name)
            {
                this.name = name;
            }

            public String toString()
            {
                return name;
            }
        };

        private String m_recording;

        private OcapRecordingRequest m_rr;

        // the Player that will present the video
        private Player player;

        // component player is added to this scene
        private HScene scene;

        private boolean visible;

        // the AWT component that contains the player
        private HVideoComponent video;

        private Rectangle BOUNDS[] = { new Rectangle(340, 260, 240, 180), };

        public final State UNREALIZED = new State("UNREALIZED");

        public final State CREATING = new State("CREATING");

        public final State REALIZING = new State("REALIZING");

        // current state -- start at UNREALIZED
        private State state = UNREALIZED;
    }
}
