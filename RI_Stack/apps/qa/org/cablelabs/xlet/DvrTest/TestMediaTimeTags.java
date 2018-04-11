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
 * Created on August 17, 2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Date;
import java.util.Vector;

import javax.media.Player;
import javax.media.Time;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import org.apache.log4j.Logger;
import org.dvb.dsmcc.DSMCCException;
import org.dvb.dsmcc.DSMCCObject;
import org.dvb.dsmcc.DSMCCStreamEvent;
import org.dvb.dsmcc.IllegalObjectTypeException;
import org.dvb.dsmcc.InsufficientResourcesException;
import org.dvb.dsmcc.MPEGDeliveryException;
import org.dvb.dsmcc.ServiceDomain;
import org.dvb.dsmcc.StreamEvent;
import org.dvb.dsmcc.StreamEventListener;
import org.dvb.dsmcc.UnknownEventException;
import org.havi.ui.HScene;
import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.RecordingPlaybackListener;
import org.ocap.dvr.TimeShiftProperties;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.dvr.event.LightweightTriggerManager;
import org.ocap.dvr.event.LightweightTriggerHandler;
import org.ocap.dvr.event.LightweightTriggerSession;

/**
 * @author
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestMediaTimeTags extends DvrTest
{

    // private HScene sc = null;
    private static int m_eventNum = 0;

    TestMediaTimeTags(HScene scene, Vector locators)
    {
        super(locators);
        // sc = scene;
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestTuneViaScheduledRecording((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestMultipleRegisterUnregister((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestMultipleBufferingRequests((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new MTTViaNetworkInterface((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new MTTViaTuneBySC((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new MTTViaScheduledRecording((OcapLocator) m_locators.elementAt(0), 0.8f, true, 6, 6));
        tests.addElement(new MTTViaScheduledRecording((OcapLocator) m_locators.elementAt(0), 1.0f, true, 6, 6));
        tests.addElement(new MTTViaScheduledRecording((OcapLocator) m_locators.elementAt(0), 1.2f, true, 6, 6));
        tests.addElement(new MTTViaScheduledRecording((OcapLocator) m_locators.elementAt(0), 2.0f, true, 6, 6));
        tests.addElement(new MTTViaScheduledRecording((OcapLocator) m_locators.elementAt(0), 1.0f, false, 6, 12));
        tests.addElement(new MTTViaScheduledRecording((OcapLocator) m_locators.elementAt(0), 2.0f, false, 6, 12));
        tests.addElement(new MTTViaScheduledRecording((OcapLocator) m_locators.elementAt(0), 4.0f, false, 6, 14));
        tests.addElement(new MTTViaBufferingRequest((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestTuneByBufferingRequest((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new SimplePMTDetection(m_locators));
        tests.addElement(new MultipleHandlerSimplePMTDetection(m_locators));
        tests.addElement(new MultipleHandlerComplexPMTDetection(m_locators));

        return tests;
    }

    /**
     * 
     */
    private LWTriggerSel subscribeRecordingEvents(String contextName, ServiceContext context, int artificialCarouselID,
            int expectedEvents)
    {
        ServiceDomain sd = null;
        try
        {
            if (context == null)
            {
                DVRTestRunnerXlet.log("subscribeRecordingEvents -- context==null");
                m_failed = TEST_FAILED;
                m_failedReason += " : subscribeRecordingEvents -- context ==null";
                return null;
            }

            if (context.getService() == null)
            {
                DVRTestRunnerXlet.log("subscribeRecordingEvents -- getService==null, sleep 5 seconds");
                Thread.sleep(5000);
            }

            if (context.getService() == null)
            {

                DVRTestRunnerXlet.log("subscribeRecordingEvents -- getService==null");
                m_failed = TEST_FAILED;
                m_failedReason += " : subscribeRecordingEvents -- getService==null";
                return null;
            }
            else
            {
                DVRTestRunnerXlet.log("subscribeRecordingEvents -- getService==" + context.getService());
            }

            Locator loc = context.getService().getLocator();

            if (loc == null)
            {
                DVRTestRunnerXlet.log("subscribeRecordingEvents -- locator==null");
                m_failedReason += " : subscribeRecordingEvents -- locator==null";
                m_failed = TEST_FAILED;
                return null;
            }

            DVRTestRunnerXlet.log("subscribeRecordingEvents -- "
            // +"context="+m_context
                    + "artificialCarouselID=" + artificialCarouselID + ", locator=" + loc);
            sd = new ServiceDomain();
            sd.attach((org.davic.net.Locator) loc, artificialCarouselID);
        }
        catch (MPEGDeliveryException e)
        {
            DVRTestRunnerXlet.log("subscribeRecordingEvents attach failed: MPEGDeliveryException: " + e.getMessage());
            m_failedReason += " : subscribeRecordingEvents attach failed: MPEGDeliveryException" + e.getMessage();
            e.printStackTrace();
            return null;
        }
        catch (DSMCCException e)
        {
            DVRTestRunnerXlet.log("subscribeRecordingEvents attach failed: DSMCCException " + e.getMessage());
            m_failedReason += " : subscribeRecordingEvents attach failed: DSMCCException" + e.getMessage();
            e.printStackTrace();
            return null;
        }
        catch (InterruptedIOException e)
        {
            DVRTestRunnerXlet.log("subscribeRecordingEvents attach failed: InterruptedIOException " + e.getMessage());
            m_failedReason += " : subscribeRecordingEvents attach failed: InterruptedIOException" + e.getMessage();
            e.printStackTrace();
            return null;
        }
        catch (Throwable e)
        {
            DVRTestRunnerXlet.log("subscribeRecordingEvents attach failed: Exception " + e.getMessage());
            m_failedReason += " : subscribeRecordingEvents attach failed: Exception" + e.getMessage();

            e.printStackTrace();
            return null;
        }

        DSMCCStreamEvent se = null;
        try
        {
            DSMCCObject mp = sd.getMountPoint();
            se = new DSMCCStreamEvent(mp.getPath());
        }
        catch (IllegalObjectTypeException e)
        {
            DVRTestRunnerXlet.log("subscribeRecordingEvents new DSMCCStreamEvent failed: IllegalObjectTypeException"
                    + e.getMessage());
            m_failedReason += " : subscribeRecordingEvents new DSMCCStreamEvent failed: IllegalObjectTypeException"
                    + e.getMessage();
            e.printStackTrace();
            return null;
        }
        catch (IOException e)
        {
            DVRTestRunnerXlet.log("subscribeRecordingEvents new DSMCCStreamEvent failed: IOException");
            m_failedReason += " : subscribeRecordingEvents new DSMCCStreamEvent failed: IOException" + e.getMessage();
            e.printStackTrace();
            return null;
        }
        catch (Throwable e)
        {
            DVRTestRunnerXlet.log("subscribeRecordingEvents new DSMCCStreamEvent failed: Exception" + e.getMessage());
            m_failedReason += " : subscribeRecordingEvents new DSMCCStreamEvent failed: Exception" + e.getMessage();
            e.printStackTrace();
            return null;
        }

        LWTriggerSel listener = new LWTriggerSel(contextName, true);

        String[] eList = se.getEventList();
        if (eList.length != expectedEvents)
        {
            DVRTestRunnerXlet.log("LWTriggerRecPlaybackListener -- failed.  Expected " + expectedEvents
                    + " events, found " + eList.length);
            m_failed = TEST_FAILED;
            m_failedReason += " : Expected " + expectedEvents + " events, found " + eList.length;
        }
        for (int i = 0; i < eList.length; i++)
        {
            DVRTestRunnerXlet.log("Subscribing to event: " + eList[i]);

            try
            {
                se.subscribe(eList[i], listener);
            }
            catch (UnknownEventException e)
            {
                DVRTestRunnerXlet.log("Subscribe threw UnknownEventException, event: " + eList[i]);
                m_failedReason += " : Subscribe threw UnknownEventException, event: " + eList[i];
                e.printStackTrace();
            }
            catch (InsufficientResourcesException e)
            {
                DVRTestRunnerXlet.log("Subscribe threw InsufficientResourcesException, event: " + eList[i]);
                m_failedReason += " : Subscribe threw InsufficientResourcesException, event: " + eList[i];
                e.printStackTrace();
            }
            catch (Throwable e)
            {
                DVRTestRunnerXlet.log("Subscribe thew throwable, event: " + eList[i]);
                m_failedReason += " : Subscribe threw throwable, event: " + eList[i];
                e.printStackTrace();
            }
        }
        return listener;
    }

    /**
     * 
     */
    private void scheduleRecordingAndSubscribeEvents(OcapLocator locator, float rate, boolean forward,
            int expectedEvents, int expectedEventReceived)
    {
        long time = 0;

        // recording time values
        // temp time = 0;
        // time = 0;
        long timeRegisterTaskMs = 1000;
        long timeRecTaskMs = 1000;
        long timeRecDelayMs = 10000;
        long recStartDate = new Date().getTime() + timeRecDelayMs;
        long defaultRecLenMs = 90 * 1000;
        long rewPlaybackMs = (defaultRecLenMs + (long) ((1.0f / rate) * (float) defaultRecLenMs));
        long forwardPlaybackMs = (long) ((1.0f / rate) * (float) defaultRecLenMs);
        long playbackLenMs = (forward ? forwardPlaybackMs : rewPlaybackMs);
        long delayAfterPlaybackRecRewMs = defaultRecLenMs - 5000; // when rew
                                                                  // occurs
                                                                  // after
                                                                  // playback
                                                                  // starts
        long timeRecEnd = timeRecDelayMs + defaultRecLenMs + timeRecTaskMs + timeRegisterTaskMs;

        // clear the schedule of pending tasks
        reset();

        // m_eventScheduler.scheduleCommand(new
        // LWTriggerRecordingRegisterEvents(time+=timeRegisterTask));
        LightweightTriggerManager lwtMgr = LightweightTriggerManager.getInstance();
        DVRTestRunnerXlet.log("LightweightTriggerManager: " + lwtMgr);

        LWTriggerHandlerAndRegister lthVideo = new LWTriggerHandlerAndRegister("session1", null, false, 35, 40, 45, 50,
                20);
        LWTriggerHandlerAndRegister lthAudio = new LWTriggerHandlerAndRegister("session2", null, false, 55, 60, 65, 70,
                30);

        // register now before scheduling tasks
        try
        {
            lwtMgr.registerHandler(lthVideo, org.ocap.si.StreamType.MPEG_2_VIDEO);
            lwtMgr.registerHandler(lthAudio, org.ocap.si.StreamType.MPEG_1_AUDIO);
        }
        catch (IllegalArgumentException iae)
        {
            m_failed = TEST_FAILED;
            DVRTestRunnerXlet.log("failed register handler exception");
            m_failedReason += " : failed register handler exception";
        }

        // this subscribes to the events when the recording starts.
        LWTriggerRecPlaybackListener recPlaybackListener = new LWTriggerRecPlaybackListener("RecordingContext",
                "ArtificalCarouselId", expectedEvents);
        DVRTestRunnerXlet.log("LWTriggerRecPlaybackListener.addRecordingPlaybackListener");
        ((OcapRecordingManager) OcapRecordingManager.getInstance()).addRecordingPlaybackListener(recPlaybackListener);

        m_eventScheduler.scheduleCommand(new Record("Recording1", locator, recStartDate, defaultRecLenMs,
                time += timeRecTaskMs));
        DVRTestRunnerXlet.log("TestInfo: Recording starts at scheduled time=" + time);

        // confirm the recordings 10 seconds after rec ends
        long confirmTime = time = timeRecEnd + 10000;
        m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                OcapRecordingRequest.COMPLETED_STATE, confirmTime));

        long delayForwardSetRateMs = 2000;
        // play the recording 5 seconds after confirm and if things are working
        // right, the events will be played.
        long timeRecPlay = time += 5000;

        if (forward)
        {
            playbackLenMs += delayForwardSetRateMs;
        }
        DVRTestRunnerXlet.log("TestInfo: Playback length=" + playbackLenMs);

        DVRTestRunnerXlet.log("TestInfo: Playback starts at scheduled time=" + time);
        m_eventScheduler.scheduleCommand(new SelectRecordedServiceAsync("Recording1", playbackLenMs, timeRecPlay, true)); // playback
                                                                                                                          // should
                                                                                                                          // only
                                                                                                                          // end
                                                                                                                          // when
                                                                                                                          // test
                                                                                                                          // stopped
        DVRTestRunnerXlet.log("TestInfo: Recording is played at scheduled time=" + timeRecPlay);

        if (forward)
        {
            m_eventScheduler.scheduleCommand(new SetRate("RecordingContext", rate, true, timeRecPlay
                    + delayForwardSetRateMs));
        }
        else
        {
            DVRTestRunnerXlet.log("TestInfo: Recording is rewound at scheduled time="
                    + (timeRecPlay + delayAfterPlaybackRecRewMs));
            m_eventScheduler.scheduleCommand(new SetRate("RecordingContext", -rate, true, timeRecPlay
                    + delayAfterPlaybackRecRewMs));
        }
        DVRTestRunnerXlet.log("TestInfo: Playback length=" + playbackLenMs);

        m_eventScheduler.run(playbackLenMs + 10000);

        if (recPlaybackListener.getNumEventsReceived() != expectedEventReceived)
        {
            m_failed = TEST_FAILED;
            String failStr = "Received " + recPlaybackListener.getNumEventsReceived() + " events, expected to receive"
                    + expectedEventReceived;
            DVRTestRunnerXlet.log(failStr);
            m_failedReason += " : " + failStr;
        }

        LightweightTriggerManager.getInstance().unregisterHandler(lthVideo);
        LightweightTriggerManager.getInstance().unregisterHandler(lthAudio);
        ((OcapRecordingManager) OcapRecordingManager.getInstance()).removeRecordingPlaybackListener(recPlaybackListener);
    }

    /**
     * @param session
     */
    static void logSession(LightweightTriggerSession session)
    {
        logSession("", session);
    }

    static void logSession(String prefix, LightweightTriggerSession session)
    {
        if (session != null)
        {
            if (session.getLocator() != null)
            {
                DVRTestRunnerXlet.log(prefix + "notifyStreamType called...session.getLocator(): "
                        + session.getLocator().toString());
            }
            else
            {
                DVRTestRunnerXlet.log(prefix + "notifyStreamType called...session.getLocator(): null");
            }
            DVRTestRunnerXlet.log(prefix + "notifyStreamType called...session.getStreamType(): "
                    + session.getStreamType());
            DVRTestRunnerXlet.log(prefix + "notifyStreamType called...session.getRecordingRequest(): "
                    + session.getRecordingRequest());
            DVRTestRunnerXlet.log(prefix + "notifyStreamType called...session.isPresenting(): "
                    + session.isPresenting());
        }
    }

    private abstract class BaseTest extends TestCase
    {
        int m_recState = 0;

        /**
         * @param now
         * @return
         */
        RecordingManager scheduleRecordingTask(long now, OcapLocator locator, RecordingChangedListener listener)
        {
            RecordingManager rm = RecordingManager.getInstance();
            rm.addRecordingChangedListener(listener);

            m_eventScheduler.reset();

            // Schedule the record call, start 30 sec from 'now' with duration
            // of 30 sec
            m_eventScheduler.scheduleCommand(new Record("Recording1", locator, now + 30000, 30000, 500));

            // Display the recordings
            m_eventScheduler.scheduleCommand(new PrintRecordings(60000));

            // Check recording, confirm it is completed
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 70000));

            m_eventScheduler.run(1000);

            return rm;
        }

        public void recordingChanged(RecordingChangedEvent arg0)
        {
            DvrEventPrinter.printEvent(arg0);
            switch (m_recState)
            {
                case 0:
                    if ((arg0.getOldState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                            && (arg0.getState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                            && (arg0.getChange() == RecordingChangedEvent.ENTRY_ADDED))
                    {
                        m_recState = 1;
                        System.out.println("State change #1 PASSED");
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("State change #1 FAILED");
                    }
                    break;
                case 1:
                    if ((arg0.getOldState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                            && (arg0.getState() == LeafRecordingRequest.IN_PROGRESS_STATE)
                            && (arg0.getChange() == RecordingChangedEvent.ENTRY_STATE_CHANGED))
                    {
                        m_recState = 2;
                        System.out.println("State change #2 PASSED");
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("State change #2 FAILED");
                    }
                    break;
                case 2:
                    if ((arg0.getOldState() == LeafRecordingRequest.IN_PROGRESS_STATE)
                            && (arg0.getState() == LeafRecordingRequest.COMPLETED_STATE)
                            && (arg0.getChange() == RecordingChangedEvent.ENTRY_STATE_CHANGED))
                    {
                        m_recState = 3;
                        System.out.println("State change #3 PASSED");
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                        DVRTestRunnerXlet.log("State change #3 FAILED");
                    }
                    break;
            }
        }
    }

    /**
     * TestTuneViaScheduledRecording Test case 2.1.3 in the Test plan
     * 
     * @param locators
     */
    public class TestTuneViaScheduledRecording extends BaseTest implements RecordingChangedListener,
            LightweightTriggerHandler
    {
        TestTuneViaScheduledRecording(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestTuneViaScheduledRecording";
        }

        public void runTest()
        {
            m_recState = 0;
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            LightweightTriggerManager lwtMgr = LightweightTriggerManager.getInstance();

            DVRTestRunnerXlet.log("LightweightTriggerManager: " + lwtMgr);

            // MPEG_2_VIDEO = 0x02
            short streamType = 0x02;

            // Register for VIDEO stream type for now. It is guaranteed to be
            // there
            try
            {
                lwtMgr.registerHandler(this, streamType);
            }
            catch (Exception e)
            {

            }

            DVRTestRunnerXlet.log("LightweightTriggerManager::registerHandler done...");

            RecordingManager rm = scheduleRecordingTask(now, m_locator, this);

            if (m_recState != 3)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Failed to transition to proper states: last sucessful state" + m_recState);
            }

            rm.removeRecordingChangedListener(this);

            // Unregister the LightweightTriggerHandler
            lwtMgr.unregisterHandler(this);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestTuneViaScheduledRecording completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestTuneViaScheduledRecording completed: FAILED -- " + m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestTuneViaScheduledRecording completed: PASSED");
            }
        }

        private OcapLocator m_locator;

        // Callback for LightweightTriggerHandler, will be called if the
        // specified 'streamType' is found
        // on any transport stream currently tuned to
        public void notifyStreamType(LightweightTriggerSession session)
        {
            DVRTestRunnerXlet.log("notifyStreamType called...with session: " + session);

            session.registerEvent(new Date(), "TestTuneViaScheduledRecordingEvent" + m_eventNum, m_eventNum, null);
            session.store();

            logSession(session);
        }

    }

    // class LWTriggerRecordingRegisterEvents extends EventScheduler.NotifyShell
    // {
    //    
    // public LWTriggerRecordingRegisterEvents(long time)
    // {
    // super(time);
    // }
    //
    // public void ProcessCommand() {
    //       
    // super.ProcessCommand();
    // System.out.println("<<<<LWTriggerRecordingRegisterEvents::ProcessCommand>>>>");
    //
    // DVRTestRunnerXlet.log ("LWTriggerRecordingRegisterEvents");
    //        
    // LightweightTriggerManager lwtMgr =
    // LightweightTriggerManager.getInstance();
    // DVRTestRunnerXlet.log("LightweightTriggerManager: "+ lwtMgr);
    //    
    // LWTriggerHandlerAndRegister lth = new
    // LWTriggerHandlerAndRegister("session1", null, false, 30,35,40,45,20);
    // // register handler (not events) now before scheduling tasks
    // try
    // {
    // lwtMgr.registerHandler(lth, org.ocap.si.StreamType.MPEG_2_VIDEO);
    // }
    // catch(IllegalArgumentException iae)
    // {
    // m_failed = TEST_FAILED;
    // DVRTestRunnerXlet.log ("failed register handler exception");
    // }
    //            
    // LWTriggerRecPlaybackListener recPlaybackListener = new
    // LWTriggerRecPlaybackListener();
    // DVRTestRunnerXlet.log("LWTriggerRecPlaybackListener.addRecordingPlaybackListener");
    // ((OcapRecordingManager)
    // OcapRecordingManager.getInstance()).addRecordingPlaybackListener(recPlaybackListener);
    // }
    // }

    /**
     * MTTViaScheduledRecording Test case 2.1.3 in the Test plan
     * 
     * @param locators
     */
    public class MTTViaScheduledRecording extends BaseTest
    {
        OcapLocator m_locator;

        float m_rate;

        boolean m_forward;

        int m_expectedEvents;

        int m_expectedEventsReceived;

        MTTViaScheduledRecording(OcapLocator locator, float rate, boolean forward, int expectedEvents,
                int expectedEventsReceived)
        {
            m_locator = locator;
            m_rate = rate;
            m_forward = forward;
            m_expectedEventsReceived = expectedEventsReceived;
            m_expectedEvents = expectedEvents;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "MTTViaScheduledRecording: rate=" + m_rate + ", direction="
                    + (m_forward ? "forward" : "forward then rewind");
        }

        public void runTest()
        {
            m_recState = 0;
            m_failed = TEST_PASSED;

            scheduleRecordingAndSubscribeEvents(m_locator, m_rate, m_forward, m_expectedEvents,
                    m_expectedEventsReceived);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED -- " + m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
            }

        }
    }

    /**
     * TestMultipleRegisterUnregister
     * 
     * @param locators
     */
    public class TestMultipleRegisterUnregister extends BaseTest implements RecordingChangedListener
    {
        private OcapLocator m_locator;

        TestMultipleRegisterUnregister(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestMultipleRegisterUnregister";
        }

        public void runTest()
        {
            m_recState = 0;
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            LightweightTriggerManager lwtMgr = LightweightTriggerManager.getInstance();

            DVRTestRunnerXlet.log("LightweightTriggerManager: " + lwtMgr);

            // MPEG_2_VIDEO = 0x02
            short streamType = 0x02;

            // Register for VIDEO stream type for now. It is guaranteed to be
            // there

            LightweightTriggerHandler listener1 = new Listener1();
            LightweightTriggerHandler listener2 = new Listener2();

            try
            {
                lwtMgr.registerHandler(listener1, streamType);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener1: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                lwtMgr.registerHandler(listener2, streamType);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener2: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            DVRTestRunnerXlet.log("LightweightTriggerManager::registerHandler done...");

            RecordingManager rm = scheduleRecordingTask(now, m_locator, this);

            if (m_recState != 3)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("Failed to transition to proper states: last sucessful state" + m_recState);
            }

            rm.removeRecordingChangedListener(this);

            // Unregister the LightweightTriggerHandler
            try
            {
                lwtMgr.unregisterHandler(listener1);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener1: " + e.getMessage());
                m_failed = TEST_FAILED;
            }
            try
            {
                lwtMgr.unregisterHandler(listener2);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener2: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            // try unregistering listener2 again
            try
            {
                lwtMgr.unregisterHandler(listener2);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed when unregistering listener2 twice: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestMultipleRegisterUnregister completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestMultipleRegisterUnregister completed: FAILED -- " + m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestMultipleRegisterUnregister completed: PASSED");
            }
        }

        private class Listener1 implements LightweightTriggerHandler
        {
            public void notifyStreamType(LightweightTriggerSession session)
            {
                DVRTestRunnerXlet.log("Listener1.notifyStreamType called...with session: " + session);

                if (session != null)
                {
                    logSession("Listener1.", session);
                }
            }

        }

        private class Listener2 implements LightweightTriggerHandler
        {
            public void notifyStreamType(LightweightTriggerSession session)
            {
                DVRTestRunnerXlet.log("Listener2.notifyStreamType called...with session: " + session);

                if (session != null)
                {
                    logSession("Listener2.", session);
                }
            }

        }
    }

    /**
     * TestMultipleBufferingRequests
     * 
     * @param locators
     */
    public class TestMultipleBufferingRequests extends BaseTest implements RecordingChangedListener
    {
        private OcapLocator m_locator;

        TestMultipleBufferingRequests(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestMultipleBufferingRequests";
        }

        public void runTest()
        {
            m_recState = 0;
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();
            long time = 0;

            LightweightTriggerManager lwtMgr = LightweightTriggerManager.getInstance();

            DVRTestRunnerXlet.log("LightweightTriggerManager: " + lwtMgr);

            // MPEG_2_VIDEO = 0x02
            short streamType = 0x02;

            // Register for VIDEO stream type for now. It is guaranteed to be
            // there

            LightweightTriggerHandler listener1 = new Listener1();

            try
            {
                lwtMgr.registerHandler(listener1, streamType);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener1: " + e.getMessage());
                m_failed = TEST_FAILED;
            }
            DVRTestRunnerXlet.log("LightweightTriggerManager::registerHandler listener1 done...");
            
            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(time));

            // 1). Start by tuning by NetworkInterface to service
            m_eventScheduler.scheduleCommand(new initServiceContext("SC1", time += 500));
            
            // BufferingRequest 1
            m_eventScheduler.scheduleCommand(new CreateBufferingRequest(null, "BuffRequest1", m_locator, 300, 500, null,
                    time += 1000));

            m_eventScheduler.scheduleCommand(new StartBufferingRequest("BuffRequest1", time += 1000));

            // 2). Validate Buffering Request and wait 1 minute
            m_eventScheduler.scheduleCommand(new VerifyBufferingRequest(null, "BuffRequest1", time += 1000));    
            
            // BufferingRequest 2
            m_eventScheduler.scheduleCommand(new CreateBufferingRequest(null, "BuffRequest2", m_locator, 300, 500, null,
                    time += 10000));

            m_eventScheduler.scheduleCommand(new StartBufferingRequest("BuffRequest2", time += 1000));

            // 2). Validate Buffering Request and wait 1 minute
            m_eventScheduler.scheduleCommand(new VerifyBufferingRequest(null, "BuffRequest2", time += 1000));
            
            // 3). Cancel the Buffering Request 1
            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(null, "BuffRequest1", time += 1000));

            // 3). Cancel the Buffering Request 2
            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(null, "BuffRequest2", time += 5000));
            
            // start executing and terminate 5 seconds after last task executes
            m_eventScheduler.run(5000);
            
            // Unregister the LightweightTriggerHandler
            try
            {
                lwtMgr.unregisterHandler(listener1);
                DVRTestRunnerXlet.log("Unregistered listener1: " + listener1);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener1: " + e.getMessage());
                m_failed = TEST_FAILED;
            }
            
            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestMultipleRegisterUnregisterWithBufferingRequest completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestMultipleRegisterUnregisterWithBufferingRequest completed: FAILED -- " + m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestMultipleRegisterUnregisterWithBufferingRequest completed: PASSED");
            }
        }

        private class Listener1 implements LightweightTriggerHandler
        {
            public void notifyStreamType(LightweightTriggerSession session)
            {
                if (session != null)
                {
                    logSession("Listener1.", session);
                }
            }
        }
    }

    /**
     * SimplePMTDetection
     * 
     * @param locators
     */
    public class SimplePMTDetection extends BaseTest implements RecordingChangedListener
    {
        // private OcapLocator m_locator1;
        private OcapLocator m_locator2;

        SimplePMTDetection(Vector locators)
        {
            // m_locator1 = (OcapLocator) locators.elementAt(0);
            m_locator2 = (OcapLocator) locators.elementAt(0);
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "SimplePMTDetection";
        }

        public void runTest()
        {
            m_recState = 0;
            m_failed = TEST_PASSED;

            LightweightTriggerManager lwtMgr = LightweightTriggerManager.getInstance();

            DVRTestRunnerXlet.log("LightweightTriggerManager: " + lwtMgr);

            // MPEG_2_VIDEO = 0x02
            short streamType_2 = 0x02;
            // arbritary data type found in transport stream 2
            short streamType_C = 0x0c;
            // arbritary data type found in transport stream 2
            short streamType_D = 0x0d;

            LightweightTriggerHandler listener_2 = new Listener_2();
            LightweightTriggerHandler listener_C = new Listener_C();
            LightweightTriggerHandler listener_D = new Listener_D();

            try
            {
                lwtMgr.registerHandler(listener_2, streamType_2);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener_2: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                lwtMgr.registerHandler(listener_C, streamType_C);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener_C: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                lwtMgr.registerHandler(listener_D, streamType_D);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener_D: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            DVRTestRunnerXlet.log("LightweightTriggerManager::registerHandler done...");

            // one segment: program id == 1, type=2
            // tuneNetworkInterface("tuner1", m_locator1, 5000);

            tuneNetworkInterface("tuner2", m_locator2, 180000);

            // Unregister the LightweightTriggerHandler
            try
            {
                DVRTestRunnerXlet.log("unregister listener_2");
                lwtMgr.unregisterHandler(listener_2);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener_2: " + e.getMessage());
                m_failed = TEST_FAILED;
            }
            try
            {
                DVRTestRunnerXlet.log("unregister listener_C");
                lwtMgr.unregisterHandler(listener_C);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener_C: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                DVRTestRunnerXlet.log("unregister listener_D");
                lwtMgr.unregisterHandler(listener_D);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener_D: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            // now tune back to locator 1. This should cause all sessions to be
            // orphaned and stopped
            // this should also mean that there are no active sessions.
            // tuneNetworkInterface("tuner1", m_locator1, 5000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestMultipleRegisterUnregister completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestMultipleRegisterUnregister completed: FAILED -- " + m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestMultipleRegisterUnregister completed: PASSED");
            }
        }

        private void tuneNetworkInterface(String niName, OcapLocator locator, long tuneTime)
        {
            // clear the schedule of pending tasks
            reset();

            // Tune network interface
            m_eventScheduler.scheduleCommand(new TuneNetworkInterface(niName, locator, 5000));

            m_eventScheduler.run(tuneTime);
        }

        private class Listener_2 implements LightweightTriggerHandler
        {
            public void notifyStreamType(LightweightTriggerSession session)
            {
                DVRTestRunnerXlet.log("Listener1.notifyStreamType called...with session: " + session);

                if (session != null)
                {
                    logSession("Listener_2.", session);
                }
            }

        }

        private class Listener_C implements LightweightTriggerHandler
        {

            public void notifyStreamType(LightweightTriggerSession session)
            {
                DVRTestRunnerXlet.log("Listener_C0.notifyStreamType called...with session: " + session);

                if (session != null)
                {
                    logSession("Listener_C0.", session);
                    session.registerEvent(new Date(), "SimplePMTDetection" + m_eventNum, m_eventNum++, null);
                    session.store();
                }
            }

        }

        private class Listener_D implements LightweightTriggerHandler
        {
            public void notifyStreamType(LightweightTriggerSession session)
            {
                DVRTestRunnerXlet.log("Listener_D0.notifyStreamType called...with session: " + session);

                if (session != null)
                {
                    logSession("Listener_D0.", session);
                }
            }

        }
    } // end SimpePMTDetection

    /**
     * MultipleHandlerSimplePMTDetection
     * 
     * @param locators
     */
    public class MultipleHandlerSimplePMTDetection extends BaseTest implements RecordingChangedListener
    {
        protected OcapLocator m_locator2;

        MultipleHandlerSimplePMTDetection(Vector locators)
        {
            if (locators == null)
            {
                m_locator2 = null;
            }
            else
            {
                m_locator2 = (OcapLocator) locators.elementAt(4);
            }
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "MultipleHandlerSimplePMTDetection";
        }

        public void runTest()
        {
            m_recState = 0;
            m_failed = TEST_PASSED;

            LightweightTriggerManager lwtMgr = LightweightTriggerManager.getInstance();

            DVRTestRunnerXlet.log("LightweightTriggerManager: " + lwtMgr);

            // MPEG_2_VIDEO = 0x02
            short streamType_2 = 0x02;
            // arbritary data type found in transport stream 2
            short streamType_C = 0x0c;
            // arbritary data type found in transport stream 2
            short streamType_D = 0x0d;

            LightweightTriggerHandler listener_20 = new Listener_20();
            LightweightTriggerHandler listener_21 = new Listener_21();

            LightweightTriggerHandler listener_C0 = new Listener_C0();
            LightweightTriggerHandler listener_C1 = new Listener_C1();

            LightweightTriggerHandler listener_D0 = new Listener_D0();
            LightweightTriggerHandler listener_D1 = new Listener_D1();

            try
            {
                lwtMgr.registerHandler(listener_20, streamType_2);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener_20: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                lwtMgr.registerHandler(listener_21, streamType_2);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener_21: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                lwtMgr.registerHandler(listener_C0, streamType_C);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener_C0: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                lwtMgr.registerHandler(listener_C1, streamType_C);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener_C1: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                lwtMgr.registerHandler(listener_D0, streamType_D);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener_D0: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                lwtMgr.registerHandler(listener_D1, streamType_D);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to register listener_D1: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            DVRTestRunnerXlet.log("LightweightTriggerManager::registerHandler done...");

            // one segment: program id == 1, type=2
            // tuneNetworkInterface("tuner1", m_locator1, 5000);

            tuneNetworkInterface("tuner2", m_locator2, 180000);

            // Unregister the LightweightTriggerHandler
            try
            {
                DVRTestRunnerXlet.log("unregister listener_20");
                lwtMgr.unregisterHandler(listener_20);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener_20: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                DVRTestRunnerXlet.log("unregister listener_21");
                lwtMgr.unregisterHandler(listener_21);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener_21: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                DVRTestRunnerXlet.log("unregister listener_C0");
                lwtMgr.unregisterHandler(listener_C0);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener_C0: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                DVRTestRunnerXlet.log("unregister listener_C1");
                lwtMgr.unregisterHandler(listener_C1);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener_C1: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            try
            {
                DVRTestRunnerXlet.log("unregister listener_D0");
                lwtMgr.unregisterHandler(listener_D0);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener_D0: " + e.getMessage());
                m_failed = TEST_FAILED;
            }
            try
            {
                DVRTestRunnerXlet.log("unregister listener_D1");
                lwtMgr.unregisterHandler(listener_D1);
            }
            catch (Exception e)
            {
                DVRTestRunnerXlet.log("failed to unregister listener_D1: " + e.getMessage());
                m_failed = TEST_FAILED;
            }

            // now tune back to locator 1. This should cause all sessions to be
            // orphaned and stopped
            // this should also mean that there are no active sessions.
            // tuneNetworkInterface("tuner1", m_locator1, 5000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestMultipleRegisterUnregister completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestMultipleRegisterUnregister completed: FAILED -- " + m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestMultipleRegisterUnregister completed: PASSED");
            }
        }

        private void tuneNetworkInterface(String niName, OcapLocator locator, long tuneTime)
        {
            // clear the schedule of pending tasks
            reset();

            // Tune network interface
            m_eventScheduler.scheduleCommand(new TuneNetworkInterface(niName, locator, 5000));

            m_eventScheduler.run(tuneTime);
        }

        private class Listener_20 implements LightweightTriggerHandler
        {
            public void notifyStreamType(LightweightTriggerSession session)
            {
                DVRTestRunnerXlet.log("Listener_20.notifyStreamType called...with session: " + session);

                if (session != null)
                {
                    logSession("Listener_20.", session);
                }
            }

        }

        private class Listener_21 implements LightweightTriggerHandler
        {
            public void notifyStreamType(LightweightTriggerSession session)
            {
                DVRTestRunnerXlet.log("Listener_21.notifyStreamType called...with session: " + session);

                if (session != null)
                {
                    logSession("Listener_21.", session);
                }
            }

        }

        private class Listener_C0 implements LightweightTriggerHandler
        {
            public void notifyStreamType(LightweightTriggerSession session)
            {
                DVRTestRunnerXlet.log("Listener_C0.notifyStreamType called...with session: " + session);

                if (session != null)
                {
                    logSession("Listener_C0.", session);
                }
            }

        }

        private class Listener_C1 implements LightweightTriggerHandler
        {
            public void notifyStreamType(LightweightTriggerSession session)
            {
                DVRTestRunnerXlet.log("Listener_C1.notifyStreamType called...with session: " + session);

                if (session != null)
                {
                    logSession("Listener_C1.", session);
                }
            }

        }

        private class Listener_D0 implements LightweightTriggerHandler
        {
            public void notifyStreamType(LightweightTriggerSession session)
            {
                DVRTestRunnerXlet.log("Listener_D0.notifyStreamType called...with session: " + session);

                if (session != null)
                {
                    logSession("Listener_D0.", session);
                }
            }

        }

        private class Listener_D1 implements LightweightTriggerHandler
        {
            public void notifyStreamType(LightweightTriggerSession session)
            {
                DVRTestRunnerXlet.log("Listener_D1.notifyStreamType called...with session: " + session);

                if (session != null)
                {
                    logSession("Listener_D1.", session);
                }
            }

        }
    }

    /**
     * MultipleHandlerSimplePMTDetection
     * 
     * @param locators
     */
    public class MultipleHandlerComplexPMTDetection extends MultipleHandlerSimplePMTDetection implements
            RecordingChangedListener
    {
        MultipleHandlerComplexPMTDetection(Vector locators)
        {
            super(null);
            m_locator2 = (OcapLocator) locators.elementAt(4);
        }

        public String getName()
        {
            return "MultipleHandlerComplexPMTDetection";
        }
    }

    /**
     * MTTViaNetworkInterface Test case 2.1.4 in the Test plan - Work in
     * progress...
     * 
     * @param locators
     */
    public class MTTViaNetworkInterface extends BaseTest implements LightweightTriggerHandler
    {
        MTTViaNetworkInterface(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "MTTViaNetworkInterface: locator" + m_locator;
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long time = 0;
            // long now = System.currentTimeMillis();

            LightweightTriggerManager ltm = LightweightTriggerManager.getInstance();
            LWTriggerHandlerAndRegister lthVideo = new LWTriggerHandlerAndRegister("session1", null, false, 40, 45, 50,
                    55, 20);
            LWTriggerHandlerAndRegister lthAudio = new LWTriggerHandlerAndRegister("session2", null, false, 60, 65, 70,
                    75, 30);

            // register now before scheduling tasks
            try
            {
                ltm.registerHandler(lthVideo, org.ocap.si.StreamType.MPEG_2_VIDEO);
                ltm.registerHandler(lthAudio, org.ocap.si.StreamType.MPEG_1_AUDIO);
            }
            catch (IllegalArgumentException iae)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failed register handler exception");
            }

            // clear the schedule of pending tasks
            reset();

            // Tune network interface
            m_eventScheduler.scheduleCommand(new TuneNetworkInterface(m_locator, time += 1000));

            // subscribe to available events. In this case, there are no events
            // available
            m_eventScheduler.scheduleCommand(new SubscribeToTimeShiftEvents("session1", time += 30000, 0));

            // Cleanup by removing handler, stoping and destroying the service.
            m_eventScheduler.scheduleCommand(new RemoveHandler(lthVideo, (time += 90000)));
            m_eventScheduler.scheduleCommand(new RemoveHandler(lthAudio, (time += 500)));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("MTTViaNetworkInterface completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("MTTViaNetworkInterface completed: FAILED -- " + m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log("MTTViaNetworkInterface completed: PASSED");
            }
        }

        private OcapLocator m_locator;

        // Callback for LightweightTriggerHandler, will be called if the
        // specified 'streamType' is found
        // on any transport stream currently tuned to
        public void notifyStreamType(LightweightTriggerSession session)
        {
            DVRTestRunnerXlet.log("notifyStreamType called...with session: " + session);

            if (session != null)
            {
                logSession(session);
            }
        }

    }

    /*
     * public class TestRecordingPlayback extends TestCase {
     * TestRecordingPlayback(OcapLocator locator) { m_locator = locator; }
     * 
     * public Runnable getTest() { return this; }
     * 
     * public String getName() { return "TestRecordingPlayback"; }
     * 
     * public void runTest() { m_failed = TEST_PASSED;
     * 
     * // Initialize ServiceContext for playback initSC();
     * 
     * long now = System.currentTimeMillis();
     * 
     * // clear the schedule of pending tasks reset();
     * 
     * // Schedule the record call m_eventScheduler.scheduleCommand(new
     * Record("Recording1", m_locator, now + 2000, 30000, 500));
     * 
     * // Count the recordings m_eventScheduler.scheduleCommand(new
     * ConfirmRecordingReq_CheckState("Recording1",
     * OcapRecordingRequest.COMPLETED_STATE, 40000));
     * 
     * // Schedule the record call m_eventScheduler.scheduleCommand(new
     * SelectRecordedService("Recording1", 41000));
     * 
     * m_eventScheduler.run(40000); // wait ~40 secs for recording to play
     * through
     * 
     * if(m_failed == TEST_FAILED) { diskFreeCheck(); if(m_failed ==
     * TEST_INTERNAL_ERROR){DVRTestRunnerXlet.log(
     * "TestRecordingPlayback completed: TEST_INTERNAL_ERROR - DISK FULL"); }
     * else{DVRTestRunnerXlet.log(
     * "---------------------------------------------------------");
     * DVRTestRunnerXlet.log("TestRecordingPlayback completed: FAILED");
     * DVRTestRunnerXlet
     * .log("---------------------------------------------------------"); } }
     * else {DVRTestRunnerXlet.log(
     * "---------------------------------------------------------");
     * DVRTestRunnerXlet.log("TestRecordingPlayback completed: PASSED");
     * DVRTestRunnerXlet
     * .log("---------------------------------------------------------"); } }
     * 
     * private OcapLocator m_locator; }
     * 
     * public class TestRecordingPlaybackUsingJMF extends TestCase { private
     * HScene sc; TestRecordingPlaybackUsingJMF(HScene scene, OcapLocator
     * locator) { sc = scene; m_locator = locator; }
     * 
     * public Runnable getTest() { return this; }
     * 
     * public String getName() { return "TestRecordingPlaybackUsingJMF"; }
     * 
     * public void runTest() { m_failed = TEST_PASSED; long now =
     * System.currentTimeMillis();
     * 
     * // clear the schedule of pending tasks reset();
     * 
     * // Schedule the record call m_eventScheduler.scheduleCommand(new
     * Record("Recording1", m_locator, now + 2000, 30000, 500));
     * 
     * // Count the recordings m_eventScheduler.scheduleCommand(new
     * ConfirmRecordingReq_CheckState("Recording1",
     * OcapRecordingRequest.COMPLETED_STATE, 40000));
     * 
     * // Schedule the record call m_eventScheduler.scheduleCommand(new
     * SelectRecordedServiceUsingJMFPlayer("Recording1", 41000, sc));
     * 
     * m_eventScheduler.run(40000); // wait ~40 secs for recording to play
     * through
     * 
     * if(m_failed == TEST_FAILED) { diskFreeCheck(); if(m_failed ==
     * TEST_INTERNAL_ERROR){DVRTestRunnerXlet.log(
     * "TestScheduledAnalogRecording completed: TEST_INTERNAL_ERROR - DISK FULL"
     * ); } else{DVRTestRunnerXlet.log(
     * "---------------------------------------------------------");
     * DVRTestRunnerXlet.log("TestRecordingPlaybackUsingJMF completed: FAILED");
     * DVRTestRunnerXlet
     * .log("---------------------------------------------------------"); } }
     * else {DVRTestRunnerXlet.log(
     * "---------------------------------------------------------");
     * DVRTestRunnerXlet.log("TestRecordingPlaybackUsingJMF completed: PASSED");
     * DVRTestRunnerXlet
     * .log("---------------------------------------------------------"); } }
     * 
     * private OcapLocator m_locator; }
     */
    /**
     * Verify if the handler can be registered and a LightweightTriggerSession
     * is returned.
     * 
     * TODO: Comment is absolete. Need to update. This now tests the entire
     * lifecycle of the MTT software including registering and receiving MTT
     * events.
     * 
     * Verifies: - locator is the one tuned to - stream type is the same as
     * defined in registerHandler - programNumber is that to the tuned service -
     * pids[] listed reflect that defined for the stream type - current
     * ServiceContext is identified in getServiceContext - getRecordingRequest
     * returns null - isPresenting is true
     * 
     * @author jspruiel
     * 
     */
    public class MTTViaTuneBySC extends BaseTest
    {
        private OcapLocator m_locator;

        // private Logger m_log;

        MTTViaTuneBySC(OcapLocator locator)
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
            return "MTTViaTuneBySC (test TSB) " + m_locator;
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long time = 0;

            // m_log = Logger.getLogger("MTTViaTuneBySC ");

            // clear the schedule of pending tasks
            reset();

            LightweightTriggerManager ltm = LightweightTriggerManager.getInstance();
            LWTriggerHandlerAndRegister lthVideo = new LWTriggerHandlerAndRegister("session1", null, false, 40, 45, 50,
                    55, 20);
            LWTriggerHandlerAndRegister lthAudio = new LWTriggerHandlerAndRegister("session2", null, false, 60, 65, 70,
                    75, 30);

            // register now before scheduling tasks
            try
            {
                ltm.registerHandler(lthVideo, org.ocap.si.StreamType.MPEG_2_VIDEO);
                ltm.registerHandler(lthAudio, org.ocap.si.StreamType.MPEG_1_AUDIO);
            }
            catch (IllegalArgumentException iae)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failed register handler exception");
            }

            // initialize Service Context
            m_eventScheduler.scheduleCommand(new initServiceContext("SC1", (time += 500)));

            m_eventScheduler.scheduleCommand(new InitTSB("SC1", (time += 5000)));

            // Select first service
            SelectService selectService = new SelectService("SC1", m_locator, (time += 10000));
            m_eventScheduler.scheduleCommand(selectService);

            /*
             * Even though there are two sessions, both sessions have access to
             * all events, so just subscribe once otherwise we'll get multiple
             * events being fired
             */
            SubscribeToTimeShiftEvents subscribe = new SubscribeToTimeShiftEvents("session1", time += 40000, 8);
            m_eventScheduler.scheduleCommand(subscribe);

            // Set expected values for verification after letting things play
            // for 160 seconds
            VerifySession verifySession = new VerifySession(lthVideo, (time += 160000));

            // if DVR_by_FPQ=FALSE in the config.properties file, the program
            // number will be unset
            // make a guess of 1 in that case
            int programNum = m_locator.getProgramNumber();
            if (programNum == -1) programNum = 1;

            Service service = null;

            SIManager siManager = SIManager.createInstance();
            try
            {
                service = siManager.getService(m_locator);
            }
            catch (InvalidLocatorException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            verifySession.setTestName(getName());
            verifySession.setLocator(m_locator);
            verifySession.setExpectdProgramNum(programNum);
            verifySession.setExpectedBufferingRequest(null);
            verifySession.setExpectedPids(m_locator.getPIDs());
            verifySession.setExpectedRecordingRequest(null);
            verifySession.setExpectedPresentationState(true);
            verifySession.setExpectedServiceContext(selectService.getServiceContext());
            verifySession.setExpectedService(service);
            verifySession.setExpectedStreamType(org.ocap.si.StreamType.MPEG_2_VIDEO);

            // Schedule verifications
            m_eventScheduler.scheduleCommand(verifySession);

            // Stop and destroy the Service Context
            m_eventScheduler.scheduleCommand(new StopBroadcastService((time += 10000)));
            m_eventScheduler.scheduleCommand(new DestroyBroadcastService((time += 10000)));

            // Cleanup by removing handler, stoping and destroying the service.
            m_eventScheduler.scheduleCommand(new RemoveHandler(lthVideo, (time += 10000)));
            m_eventScheduler.scheduleCommand(new RemoveHandler(lthAudio, (time += 500)));

            // start executing and terminate 5 seconds after last task executes
            m_eventScheduler.run(5000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED -- " + m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
            }
        }
    }

    public class MTTViaBufferingRequest extends BaseTest 
    {
        private OcapLocator m_locator;

        // private Logger m_log;

        MTTViaBufferingRequest(OcapLocator locator)
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
            return "MTTViaBufferingRequest " + m_locator;
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long time = 0;

            // clear the schedule of pending tasks
            reset();

            LightweightTriggerManager ltm = LightweightTriggerManager.getInstance();
            LWTriggerHandlerAndRegister lthVideo = new LWTriggerHandlerAndRegister("session1", null, true, 40, 45, 50,
                    55, 20);
            LWTriggerHandlerAndRegister lthAudio = new LWTriggerHandlerAndRegister("session2", null, true, 60, 65, 70,
                    75, 30);

            // register now before scheduling tasks
            try
            {
                ltm.registerHandler(lthVideo, org.ocap.si.StreamType.MPEG_2_VIDEO);
                ltm.registerHandler(lthAudio, org.ocap.si.StreamType.MPEG_1_AUDIO);
            }
            catch (IllegalArgumentException iae)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failed register handler exception");
            }

            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(time));

            // 1). Start by tuning by NetworkInterface to service
            m_eventScheduler.scheduleCommand(new initServiceContext("SC1", time += 500));

            // m_eventScheduler.scheduleCommand(
            m_eventScheduler.scheduleCommand(new CreateBufferingRequest(null, "BuffRequest", m_locator, 300, 500, null,
                    time += 1000));

            m_eventScheduler.scheduleCommand(new StartBufferingRequest("BuffRequest", time += 1000));

            // 2). Validate Buffering Request and wait 1 minute
            m_eventScheduler.scheduleCommand(new VerifyBufferingRequest(null, "BuffRequest", time += 1000));

            // 3). Cancel the Buffering Request
            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(null, "BuffRequest", time += 60000));

            // Cleanup by removing handler, stoping and destroying the service.
            m_eventScheduler.scheduleCommand(new RemoveHandler(lthVideo, (time += 5000)));
            m_eventScheduler.scheduleCommand(new RemoveHandler(lthAudio, (time += 500)));

            // start executing and terminate 5 seconds after last task executes
            m_eventScheduler.run(5000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log(getName() + " completed: TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED -- " + m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
            }
        }
    }

    /**
     * Tune by BufferingRequest and detect session.
     * 
     * @author jspruiel
     * 
     */
    public class TestTuneByBufferingRequest extends TestCase
    {
        private OcapLocator m_locator;

        private Logger m_log;

        TestTuneByBufferingRequest(OcapLocator locator)
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
            return "TestTuneByBufferingRequest " + m_locator;
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            m_log = Logger.getLogger("TestTuneByBufferingRequest ");

            // clear the schedule of pending tasks
            reset();

            // clear all buffering requests
            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(500));

            // create and start the buffering request
            m_eventScheduler.scheduleCommand(new initServiceContext("SC", 1000));
            m_eventScheduler.scheduleCommand(new setSCBuffering(m_log, // logger
                    "SC", // name
                    300, // minDur
                    500, // maxDur
                    false, // don't last buffer
                    false, // forget last preference
                    2000));

            m_eventScheduler.scheduleCommand(new CreateBufferingRequest(m_log, "BuffRequest", m_locator, 300, 500,
                    null, 3000));
            m_eventScheduler.scheduleCommand(new StartBufferingRequest("BuffRequest", 4000));
            m_eventScheduler.scheduleCommand(new VerifyBufferingRequest(m_log, "BuffRequest", 10000));

            // Register LTH, set expected values and schedule it.
            LWTriggerHandlerAndRegister lth = new LWTriggerHandlerAndRegister("session1", null, false, 5, 10, 15, 20, 1);
            VerifySession verifySession = new VerifySession(lth, 15000);

            Service service = null;
            SIManager siManager = SIManager.createInstance();
            try
            {
                service = siManager.getService(m_locator);
            }
            catch (InvalidLocatorException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            verifySession.setTestName(getName());
            verifySession.setLocator(m_locator);
            verifySession.setExpectdProgramNum(m_locator.getProgramNumber());
            verifySession.setExpectedBufferingRequest(null);
            verifySession.setExpectedPids(m_locator.getPIDs());
            verifySession.setExpectedRecordingRequest(null);
            verifySession.setExpectedPresentationState(false);
            verifySession.setExpectedServiceContext(null);
            verifySession.setExpectedService(service);
            verifySession.setExpectedStreamType(org.ocap.si.StreamType.MPEG_2_VIDEO);

            // Install the handler and then schedule the verification object.
            LightweightTriggerManager ltm = LightweightTriggerManager.getInstance();
            try
            {
                ltm.registerHandler(lth, org.ocap.si.StreamType.MPEG_2_VIDEO);
            }
            catch (IllegalArgumentException iae)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failed register handler exception");
            }

            // Schedule verifications
            m_eventScheduler.scheduleCommand(verifySession);

            // Cleanup by removing handler, stoping and destroying the service.
            m_eventScheduler.scheduleCommand(new RemoveHandler(lth, 82000));

            // Stop and destroy the Service Context
            m_eventScheduler.scheduleCommand(new CancelBufferingRequest(m_log, "BuffRequest", true, 20000));

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
                    DVRTestRunnerXlet.log(getName() + " completed: FAILED -- " + m_failedReason);
                }
            }
            else
            {
                DVRTestRunnerXlet.log(getName() + " completed: PASSED");
            }
        }

    }

    class VerifySession extends EventScheduler.NotifyShell
    {
        LWTriggerHandlerAndRegister m_obs;

        String m_testName;

        // Expected values to test actauls results.
        OcapLocator m_locator;

        RecordingRequest m_recordingExp;

        BufferingRequest m_bufferingRequestExp;

        ServiceContext m_serviceContextExp;

        short m_streamTypeExp;

        int[] m_pidsExp;

        boolean m_presStateExp;

        int m_progNumExp;

        Service m_serviceExp;

        public VerifySession(LWTriggerHandlerAndRegister obs, long time)
        {
            super(time);
            m_obs = obs;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<VerifySession::ProcessCommand>>>>");

            LightweightTriggerSession lts = m_obs.getLTS();
            DVRTestRunnerXlet.log("VerifyingSession " + lts);

            if (lts == null)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failure session is null");
            }

            // Verify Program # is that to the tuned service
            /*
             * if(m_progNumExp != lts.getProgramNumber()) { m_failed =
             * TEST_FAILED; DVRTestRunnerXlet.log("failure progNum expected: " +
             * m_progNumExp + " actual: " + lts.getProgramNumber());
             * 
             * }
             */

            // Verify stream type is the same as defined in registerHandler
            if (m_streamTypeExp != lts.getStreamType())
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failure streamType expected: " + m_streamTypeExp + " actual: "
                        + lts.getStreamType());

            }

            if (m_bufferingRequestExp == null)
            {
                if (lts.getBufferingRequest() != null)
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("failure expected null BufferingRequest actual: " + lts.getBufferingRequest());
                }
            }
            else if (!m_bufferingRequestExp.equals(lts.getBufferingRequest()))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failure not equal; BufferingRequest expected: " + m_bufferingRequestExp
                        + " actual: " + lts.getBufferingRequest());
            }

            if (m_presStateExp != lts.isPresenting())
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failure not equal; isPresenting expected: " + m_presStateExp + " actual: "
                        + lts.isPresenting());
            }

            // Verify that the current Service Context is identified in
            // getServiceContext
            if (m_serviceContextExp == null)
            {
                if (lts.getServiceContext() != null)
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("failure expected null ServiceContext actual: = " + lts.getServiceContext());
                }
            }
            else if (!m_serviceContextExp.equals(lts.getServiceContext()))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failure not equal ServiceContext expect: " + m_serviceContextExp + " actual: = "
                        + lts.getServiceContext());
            }

            // Verify the service
            if (m_serviceExp == null)
            {
                if (lts.getService() != null)
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("failure expected null service actual: = " + lts.getService());
                }
            }
            else if (!m_serviceExp.equals(lts.getService()))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failure not equal Service expect: " + m_serviceExp + " actual: = "
                        + lts.getService());
            }
            // Verify that getRecordingRequest() returns null
            if (m_recordingExp == null)
            {
                if (lts.getRecordingRequest() != null)
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("failed RecordingRequest not null");
                }
            }
            else if (!m_recordingExp.equals(lts.getRecordingRequest()))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failed RecordingRequest not equal");
            }

            // Verify the PIDs[] listed reflect that defined for the stream type
            if (m_pidsExp.length != lts.getPIDs().length)
            {
                m_failed = TEST_FAILED;
            }

            int len = m_pidsExp.length;
            int[] actual = lts.getPIDs();

            for (int j = 0; j < len; j++)
            {
                int pid = m_pidsExp[j];

                for (int k = 0; k < len; k++)
                {
                    if (pid == actual[k])
                    {
                        actual[k] = -1;
                    }
                }

                for (int k = 0; k < len; k++)
                {
                    if (actual[k] != -1)
                    {
                        m_failed = TEST_FAILED;
                    }
                }
            }

            // Verify Locator of LTS is the same as the one used to perform
            // the tune.
            // Todo: Do not depend on the ServiceContext to obtain the locator.
            ServiceContext sc = lts.getServiceContext();
            if (sc == null)
            {
                DVRTestRunnerXlet.log("failed ServiceContext is null");
            }
            Service svc = sc.getService();
            if (svc == null)
            {
                DVRTestRunnerXlet.log("failed Service is null");
            }

            OcapLocator oloc = (OcapLocator) svc.getLocator();
            if (oloc == null)
            {
                DVRTestRunnerXlet.log("failed Service.getLocator returned null");
            }

            if (!m_locator.equals(oloc))
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("failure Locator expected = " + m_locator + " actual = " + oloc);
            }
        }

        void setTestName(String name)
        {
            m_testName = name;
        }

        void setLocator(OcapLocator locator)
        {
            m_locator = locator;
        }

        void setExpectedStreamType(short st)
        {
            m_streamTypeExp = st;
        }

        void setExpectedServiceContext(ServiceContext sc)
        {
            m_serviceContext = sc;
        }

        void setExpectedService(Service s)
        {
            m_serviceExp = s;
        }

        void setExpectedBufferingRequest(BufferingRequest br)
        {
            m_bufferingRequestExp = br;
        }

        void setExpectedPids(int[] pids)
        {
            m_pidsExp = pids;
        }

        void setExpectedRecordingRequest(RecordingRequest rr)
        {
            m_recordingExp = rr;
        }

        void setExpectedPresentationState(boolean presentingState)
        {
            m_presStateExp = presentingState;
        }

        void setExpectdProgramNum(int progNum)
        {
            m_progNumExp = progNum;
        }
    }// end verify session

    class RemoveHandler extends EventScheduler.NotifyShell
    {
        LWTriggerHandlerAndRegister m_obs = null;

        public RemoveHandler(LWTriggerHandlerAndRegister obs, long time)
        {
            super(time);
            m_obs = obs;
        }

        public RemoveHandler(long time)
        {
            super(time);
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<RemoveHandler::ProcessCommand>>>>\n");
            LightweightTriggerManager.getInstance().unregisterHandler(m_obs);
        }
    }

    class InitTSB extends EventScheduler.NotifyShell
    {
        String m_serviceContextName = null;

        public InitTSB(String name, long time)
        {
            super(time);
            m_serviceContextName = name;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<InitTSB::ProcessCommand>>>>\n");
            TimeShiftProperties props = (TimeShiftProperties) findObject(m_serviceContextName);

            props.setMinimumDuration(60 * 60); // 1 hour
            long minDur = props.getMinimumDuration();
            DVRTestRunnerXlet.log("InitTSB -- getMinimumDuration() (seconds) == " + minDur);
        }
    }

    class LWTriggerSel implements StreamEventListener
    {
        public static final long MILLIS_PER_SEC = 1000L;

        /** Nanoseconds per millisecond */
        public static final long NANOS_PER_MILLI = Time.ONE_SECOND / MILLIS_PER_SEC;

        public int m_numReceived = 0;

        public boolean m_displayMediaTime = false;

        public String m_scName;

        LWTriggerSel()
        {
            this(null, false);
        }

        LWTriggerSel(String scName, boolean displayMediaTime)
        {
            m_displayMediaTime = displayMediaTime;
            m_scName = scName;
        }

        public void receiveStreamEvent(StreamEvent e)
        {
            String displayStr = null;
            m_numReceived++;
            if (m_displayMediaTime)
            {
                ServiceContext sc = (ServiceContext) findObject(m_scName);

                Player player = null;

                ServiceContentHandler[] handlers = sc.getServiceContentHandlers();

                for (int i = 0; i < handlers.length; ++i)
                {
                    ServiceContentHandler handler = handlers[i];
                    System.out.println("check handler " + handler);
                    if (handler instanceof Player)
                    {
                        System.out.println("found player " + handler + " for context " + sc);
                        player = (Player) handler;
                    }
                }

                float mediaTimeNs = (float) player.getMediaNanoseconds();

                float eventTime = mediaTimeNs / ((float) (NANOS_PER_MILLI * MILLIS_PER_SEC));

                displayStr = "Event at " + Float.toString(eventTime).substring(0, 5) + " (mediatime in sec)"
                        + ", name= " + e.getEventName() + ", id=" + e.getEventId();
            }
            else
            {
                displayStr = "Event at " + new Date().getTime() + " date in MS" + ", name= " + e.getEventName()
                        + ", id=" + e.getEventId();
            }
            DVRTestRunnerXlet.log(displayStr);
        }
    }

    class LWTriggerHandlerAndRegister implements LightweightTriggerHandler
    {
        LightweightTriggerSession m_session;

        long m_delay1;

        long m_delay2;

        long m_delay3;

        long m_delay4;

        String m_sessionName;

        int m_startingEventNum;

        Date m_now;

        boolean m_testBufferingRequest;

        public LWTriggerHandlerAndRegister(String sessionName, Date now, boolean testBufferRequest, long delay1,
                long delay2, long delay3, long delay4, int startingEventNum)
        {
            m_session = null;
            m_delay1 = delay1;
            m_delay2 = delay2;
            m_delay3 = delay3;
            m_delay4 = delay4;
            m_sessionName = sessionName;
            m_startingEventNum = startingEventNum;
            m_now = now;
            m_testBufferingRequest = testBufferRequest;
        }

        public void notifyStreamType(LightweightTriggerSession session)
        {
            m_session = session;
            DVRTestRunnerXlet.log("LWTriggerHandlerAndRegister.notifyStreamType, session: " + session);

            logSession(session);

            insertObject(m_session, m_sessionName);

            new Thread()
            {
                public void run()
                {
                    super.run();
                    setName("registerStreamEvent");
                    try
                    {
                        if (m_now == null) m_now = new Date();
                        DVRTestRunnerXlet.log("Registering StreamEvent now is " + (m_now.getTime()));
                        Date now1 = new Date(m_now.getTime() + m_delay1 * 1000);
                        Date now2 = new Date(m_now.getTime() + m_delay2 * 1000);
                        Date now3 = new Date(m_now.getTime() + m_delay3 * 1000);
                        Date now4 = new Date(m_now.getTime() + m_delay4 * 1000);

                        sleep(10000); // this is necessary in order to have the
                                      // recording ready to store. This
                        // is because our test case doesn't follow the main use
                        // case for this api.

                        DVRTestRunnerXlet.log("Test Buffering Request = " + m_testBufferingRequest);
                        if (m_testBufferingRequest)
                        {
                            BufferingRequest bufReq = m_session.getBufferingRequest();
                            if (bufReq == null)
                            {
                                m_failed = TEST_FAILED;
                                m_failedReason += " : did not find BufferingRequest";
                            }
                            else
                            {
                                DVRTestRunnerXlet.log("Found BufferingRequest: " + bufReq);
                            }
                        }

                        int event1Num = m_startingEventNum;
                        int event2Num = m_startingEventNum + 1;
                        int event3Num = m_startingEventNum + 2;
                        int event4Num = m_startingEventNum + 3;

                        DVRTestRunnerXlet.log("Registering Event_" + event1Num + " at " + (now1.getTime()));
                        m_session.registerEvent(now1, "Event_" + event1Num, event1Num, null);
                        DVRTestRunnerXlet.log("Registering Event_" + event2Num + " at " + (now2.getTime()));
                        m_session.registerEvent(now2, "Event_" + event2Num, event2Num, null);
                        DVRTestRunnerXlet.log("Registering Event_" + event3Num + " at " + (now3.getTime()));
                        m_session.registerEvent(now3, "Event_" + event3Num, event3Num, null);

                        DVRTestRunnerXlet.log("Storing StreamEvents " + event1Num + "-" + event3Num);
                        m_session.store();

                        // now test that register event still works. Don't store
                        // this since we need to make sure that his
                        // doesn't show up when using recording. should show up
                        // when using TSB
                        DVRTestRunnerXlet.log("Registering Event_" + event4Num + " at " + (now1.getTime()));
                        m_session.registerEvent(now4, "Event_" + event4Num, event4Num, null);
                        DVRTestRunnerXlet.log("Purposely did not store Event_" + event4Num);

                        boolean exception1Caught = false;
                        boolean exception2Caught = false;
                        try
                        {
                            DVRTestRunnerXlet.log("Registering unstored Event_" + event4Num
                                    + " again.  Should get an IllegalArgumentException....");
                            m_session.registerEvent(now1, "Event_" + event4Num, event4Num, null);
                        }
                        catch (IllegalArgumentException e)
                        {
                            DVRTestRunnerXlet.log("Good:  Got IllegalArgumentException when registering Event_"
                                    + event4Num + " twice");
                            exception1Caught = true;
                        }
                        try
                        {
                            DVRTestRunnerXlet.log("Registering unstored Event_" + event1Num
                                    + " again.  Should get an IllegalArgumentException....");
                            m_session.registerEvent(now2, "Event_" + event1Num, event1Num, null);
                        }
                        catch (IllegalArgumentException e)
                        {
                            DVRTestRunnerXlet.log("Good:  Got IllegalArgumentException when registering Event_"
                                    + event1Num + " twice");
                            exception2Caught = true;
                        }
                        if (!(exception1Caught && exception2Caught))
                        {
                            DVRTestRunnerXlet.log("Failed:  Did not get both IllegalArgumentException when registering events twice");
                            m_failed = TEST_FAILED;
                            m_failedReason += " : Did not get both IllegalArgumentException when registering events twice";
                        }
                    }
                    catch (Throwable e)
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason += " : Registering StreamEvent. Unable to register (test failure).  Exception msg: "
                                + e.getMessage();
                        DVRTestRunnerXlet.log("Registering StreamEvent. Unable to register (test failure).  Exception msg: "
                                + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }.start();
        }

        LightweightTriggerSession getLTS()
        {
            return m_session;
        }
    }

    class LWTriggerHandlerOnly implements LightweightTriggerHandler
    {
        LightweightTriggerSession m_session;

        String m_sessionName;

        public LWTriggerHandlerOnly(String sessionName)
        {
            m_session = null;
            m_sessionName = sessionName;
        }

        public void notifyStreamType(LightweightTriggerSession session)
        {
            m_session = session;
            DVRTestRunnerXlet.log("LWTriggerHandlerAndRegister.notifyStreamType, session: " + session);

            logSession(session);

            insertObject(m_session, m_sessionName);
        }

        LightweightTriggerSession getLTS()
        {
            return m_session;
        }
    }

    class SubscribeToTimeShiftEvents extends EventScheduler.NotifyShell
    {
        String m_sessionName;

        int m_expectedEvents;

        public SubscribeToTimeShiftEvents(String sessionName, long time, int expectedEvents)
        {
            super(time);
            m_sessionName = sessionName;
            m_expectedEvents = expectedEvents;
        }

        public void ProcessCommand()
        {
            System.out.println("<<<<SubscribeToTimeShiftEvents::ProcessCommand>>>>\n");

            new Thread()
            {
                public void run()
                {
                    super.run();
                    setName("SubscribeToTimeShiftEvents");

                    DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents");

                    LightweightTriggerSession session = null;
                    for (int i = 0; i < 7; i++)
                    {
                        session = (LightweightTriggerSession) findObject(m_sessionName);
                        if (session == null)
                        {
                            DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents -- no session, try again in 5 secs");
                            try
                            {
                                sleep(5000);
                            }
                            catch (InterruptedException e)
                            {
                                e.printStackTrace();
                            }
                        }
                    }

                    if (session == null)
                    {
                        m_failed = TEST_FAILED;
                        m_failedReason += " : SubscribeToTimeShiftEvents, null session";
                        DVRTestRunnerXlet.log("Test Failed: " + m_failedReason);
                        return;
                    }

                    ServiceDomain sd = new ServiceDomain();

                    try
                    {
                        sd.attach(session.getLocator());
                    }
                    catch (MPEGDeliveryException e)
                    {
                        DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents attach failed: MPEGDeliveryException: "
                                + e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                    catch (DSMCCException e)
                    {
                        DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents attach failed: DSMCCException"
                                + e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                    catch (InterruptedIOException e)
                    {
                        DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents attach failed: InterruptedIOException"
                                + e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                    DSMCCObject mp = sd.getMountPoint();
                    DSMCCStreamEvent se = null;
                    try
                    {
                        se = new DSMCCStreamEvent(mp.getPath());
                    }
                    catch (IllegalObjectTypeException e)
                    {
                        DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents attach failed: IllegalObjectTypeException"
                                + e.getMessage());
                        e.printStackTrace();
                        return;
                    }
                    catch (IOException e)
                    {
                        DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents attach failed: IOException");
                        e.printStackTrace();
                        return;
                    }
                    LWTriggerSel listener = new LWTriggerSel();

                    String[] eList = se.getEventList();
                    if (eList.length != m_expectedEvents)
                    {
                        DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents -- failed.  Expected " + m_expectedEvents
                                + " events found only  : " + eList.length + " events");
                        m_failed = TEST_FAILED;
                        m_failedReason += " : Expected " + m_expectedEvents + " events, fount only " + eList.length;
                    }

                    DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents now is " + (new Date().getTime()));

                    for (int i = 0; i < eList.length; i++)
                    {
                        DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents -- subscribing to event: " + eList[i]);

                        try
                        {
                            se.subscribe(eList[i], listener);
                        }
                        catch (UnknownEventException e)
                        {
                            DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents -- UnknownEventException, event: "
                                    + eList[i]);
                            e.printStackTrace();
                            return;
                        }
                        catch (InsufficientResourcesException e)
                        {
                            DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents -- InsufficientResourcesException, event: "
                                    + eList[i]);
                            e.printStackTrace();
                            return;
                        }
                        catch (Throwable e)
                        {
                            DVRTestRunnerXlet.log("SubscribeToTimeShiftEvents -- throwable, event: " + eList[i]);
                            e.printStackTrace();
                            return;
                        }
                    }
                }
            }.start();
        }
    }

    class LWTriggerRecPlaybackListener implements RecordingPlaybackListener
    {
        int m_expectedEvents;

        String m_contextName;

        String m_artificialCarouselIDName;

        LWTriggerSel m_sel = null;;

        public LWTriggerRecPlaybackListener(String contextName, String artificialIdName, int expectedEvents)
        {
            m_contextName = contextName;
            m_artificialCarouselIDName = artificialIdName;
            m_expectedEvents = expectedEvents;
        }

        public int getNumEventsReceived()
        {
            if (m_sel == null)
            {
                return 0;
            }
            else
                return m_sel.m_numReceived;
        }

        public void notifyRecordingPlayback(ServiceContext context, int artificialCarouselID, int[] carouselIDs)
        {
            DVRTestRunnerXlet.log("LWTriggerRecPlaybackListener.notifyRecordingPlayback");

            insertObject(context, m_contextName);
            insertObject(new Integer(artificialCarouselID), m_artificialCarouselIDName);

            m_sel = subscribeRecordingEvents(m_contextName, context, artificialCarouselID, m_expectedEvents);
        }
    }
}
