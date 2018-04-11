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
 * Created on Mar 2, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.io.FileInputStream;
import java.io.Serializable;
import java.net.SocketException;
import java.util.Vector;
import java.util.Date;

import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;

import org.cablelabs.test.autoxlet.ArgParser;
import org.cablelabs.test.autoxlet.UDPLogger;
import org.cablelabs.xlet.DvrTest.DvrTest.ConfirmRecordingReq_CheckState;
import org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell;

/**
 * @author fsmith, cpratt, ryanh
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestPersistentRecordings extends DvrTest
{

    private String autotestserver = null;

    private Integer autotestport = null;

    TestPersistentRecordings(Vector locators)
    {
        super(locators);
        m_eventScheduler = new EventScheduler();
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestInterruptedRecordings((OcapLocator) m_locators.elementAt(0), false));
        tests.addElement(new TestInterruptedRecordings((OcapLocator) m_locators.elementAt(0), true));
        tests.addElement(new TestPersistentPurge((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestPersistentRecordingContention((OcapLocator) m_locators.elementAt(0), false));
        tests.addElement(new TestPersistentRecordingContention((OcapLocator) m_locators.elementAt(0), true));
        return tests;
    }

    /**
     * Schedule a series of recordings for testing persistent recordings
     * 
     * @param locator
     */
    public class TestInterruptedRecordings extends TestCase
    {
        private boolean m_with_conflicts;

        TestInterruptedRecordings(OcapLocator locator, boolean with_conflicts)
        {
            m_locator = locator;
            m_with_conflicts = with_conflicts;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test Interrupted Recordings : " + (m_with_conflicts ? "with conflicts" : "if no conflicts");
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;

            // clear the schedule of pending tasks
            reset();
            getUDPParams();

            // check disk for empty space if not send error message
            diskFreeCheck();
            if (m_failed == TEST_INTERNAL_ERROR)
            {
                DVRTestRunnerXlet.log("TestInterruptedRecordingsRecordings: TEST_INTERNAL_ERROR - DISK FULL");
            }

            /**
             * Schedule 7 recordings:
             * 
             * Recording 0 should start before power down and get cancelled
             * before starting. Upon reboot, this recording should be in the
             * CANCELLED_STATE. Recording 1 should start before power down and
             * end before power down. Upon reboot, this recording should be in
             * the COMPLETED_STATE. Recording 2 should start before power down
             * and end after power up. Upon reboot, this recording should be in
             * the INCOMPLETE_STATE and a new recording should be created with
             * the same parameters. Upon reboot, this recording should be in the
             * IN_PROGRESS_INCOMPLETE state. Recording 3 should start before
             * power down and end during power down. Upon reboot, this recording
             * should be in the INCOMPLETE_STATE. Recording 4 should start
             * during power down and end during power down. Upon reboot, this
             * recording should be in the FAILED_STATE. Recording 5 should start
             * when the box is down and end after power up. Upon reboot, this
             * recording should go to the IN_PROGRESS_STATE. Recording 6 should
             * start after power up and end after power up. Upon reboot, this
             * recording should go to the PENDING_NO_CONFLICT_STATE.
             */

            // POWER DOWN SHOULD BE AT (now + 1.5 minutes).
            // POWER UP SHOULD BE AT (now + 2.5minutes)

            long now = System.currentTimeMillis();
            long powerDown = now + 90 * m_oneSecond;
            long downTime = m_oneMinute;
            long powerUp = powerDown + downTime; // Not factoring bootup time
                                                 // here

            long rec0Start = powerUp + (11 * m_oneMinute), rec0End = powerUp + (12 * m_oneMinute);
            long rec1Start = now + 10 * m_oneSecond, rec1End = now + 40 * m_oneSecond;
            long rec2Start = now + 60 * m_oneSecond, rec2End = now + 20 * m_oneMinute;
            long rec3Start = now + 75 * m_oneSecond, rec3End = powerUp - (10 * m_oneSecond);
            long rec4Start = powerDown + (20 * m_oneSecond), rec4End = powerUp - (10 * m_oneSecond);
            long rec5Start = powerDown + (30 * m_oneSecond), rec5End = powerUp + (8 * m_oneMinute);
            long rec6Start = powerUp + (9 * m_oneMinute), rec6End = powerUp + (10 * m_oneMinute);

            long rec0Duration = rec0End - rec0Start;
            long rec1Duration = rec1End - rec1Start;
            long rec2Duration = rec2End - rec2Start;
            long rec3Duration = rec3End - rec3Start;
            long rec4Duration = rec4End - rec4Start;
            long rec5Duration = rec5End - rec5Start;
            long rec6Duration = rec6End - rec6Start;

            Date rec0StartDate = new Date(rec0Start);
            Date rec1StartDate = new Date(rec1Start);
            Date rec2StartDate = new Date(rec2Start);
            Date rec3StartDate = new Date(rec3Start);
            Date rec4StartDate = new Date(rec4Start);
            Date rec5StartDate = new Date(rec5Start);
            Date rec6StartDate = new Date(rec6Start);

            if (m_with_conflicts)
            {
                m_eventScheduler.scheduleCommand(new Record("Recording0", m_locator, rec0Start, rec0Duration, 500,
                        1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                        OcapRecordingProperties.RECORD_WITH_CONFLICTS));
                m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, rec1Start, rec1Duration, 510,
                        1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                        OcapRecordingProperties.RECORD_WITH_CONFLICTS));
                m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, rec2Start, rec2Duration, 520,
                        1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                        OcapRecordingProperties.RECORD_WITH_CONFLICTS));
                m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, rec3Start, rec3Duration, 530,
                        1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                        OcapRecordingProperties.RECORD_WITH_CONFLICTS));
                m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, rec4Start, rec4Duration, 540,
                        1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                        OcapRecordingProperties.RECORD_WITH_CONFLICTS));
                m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, rec5Start, rec5Duration, 550,
                        1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                        OcapRecordingProperties.RECORD_WITH_CONFLICTS));
                m_eventScheduler.scheduleCommand(new Record("Recording6", m_locator, rec6Start, rec6Duration, 560,
                        1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                        OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new Record("Recording0", m_locator, rec0Start, rec0Duration, 500));
                m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, rec2Start, rec2Duration, 520));
                m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, rec3Start, rec3Duration, 530));
                m_eventScheduler.scheduleCommand(new Record("Recording4", m_locator, rec4Start, rec4Duration, 540));
                m_eventScheduler.scheduleCommand(new Record("Recording5", m_locator, rec5Start, rec5Duration, 550));
                m_eventScheduler.scheduleCommand(new Record("Recording6", m_locator, rec6Start, rec6Duration, 560));
            }

            m_eventScheduler.scheduleCommand(new PrintOut("Time now: " + now, 4999));
            m_eventScheduler.scheduleCommand(new PrintOut("Recording0: Start " + rec0Start + " (" + rec0StartDate
                    + "), dur " + rec0Duration, 5000));
            m_eventScheduler.scheduleCommand(new PrintOut("!IGNORE IF IF_CONFILCTS CASE! Recording1: Start "
                    + rec1Start + " (" + rec1StartDate + "), dur " + rec1Duration, 5001));
            m_eventScheduler.scheduleCommand(new PrintOut("Recording2: Start " + rec2Start + " (" + rec2StartDate
                    + "), dur " + rec2Duration, 5002));
            m_eventScheduler.scheduleCommand(new PrintOut("Recording3: Start " + rec3Start + " (" + rec3StartDate
                    + "), dur " + rec3Duration, 5003));
            m_eventScheduler.scheduleCommand(new PrintOut("Recording4: Start " + rec4Start + " (" + rec4StartDate
                    + "), dur " + rec4Duration, 5004));
            m_eventScheduler.scheduleCommand(new PrintOut("Recording5: Start " + rec5Start + " (" + rec5StartDate
                    + "), dur " + rec5Duration, 5005));
            m_eventScheduler.scheduleCommand(new PrintOut("Recording6: Start " + rec6Start + " (" + rec6StartDate
                    + "), dur " + rec6Duration, 5006));

            if (m_with_conflicts)
            {
                m_eventScheduler.scheduleCommand(new AddAppData("Recording0", "interrupted_w_conflict", "Recording0",
                        6100));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording1", "interrupted_w_conflict", "Recording1",
                        6200));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording2", "interrupted_w_conflict", "Recording2",
                        6300));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording3", "interrupted_w_conflict", "Recording3",
                        6400));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording4", "interrupted_w_conflict", "Recording4",
                        6500));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording5", "interrupted_w_conflict", "Recording5",
                        6600));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording6", "interrupted_w_conflict", "Recording6",
                        6700));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new AddAppData("Recording0", "interrupted", "Recording0", 6100));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording2", "interrupted", "Recording2", 6300));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording3", "interrupted", "Recording3", 6400));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording4", "interrupted", "Recording4", 6500));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording5", "interrupted", "Recording5", 6600));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording6", "interrupted", "Recording6", 6700));
            }
            // Cancel Recording0
            m_eventScheduler.scheduleCommand(new PrintOut("Cancelling pending recording Recording0...", 7000));
            m_eventScheduler.scheduleCommand(new CancelRecordingRequest("Recording0", 7500));

            if (m_with_conflicts)
            {
                m_eventScheduler.scheduleCommand(new PrintOut("Recording0: Start " + rec0Start + " (" + rec0StartDate
                        + "), dur " + rec0Duration, 75000));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording0: Should be CANCELLED after power-up", 75501));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording1: Start " + rec1Start + " (" + rec1StartDate
                        + "), dur " + rec1Duration, 76002));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording1: Should be COMPLETED after power-up", 76503));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording2: Start " + rec2Start + " (" + rec2StartDate
                        + "), dur " + rec2Duration, 77005));
                m_eventScheduler.scheduleCommand(new PrintOut(
                        "Recording2: Should be IN_PROGRESS_INCOMPLETE after power-up", 77506));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording3: Start " + rec3Start + " (" + rec3StartDate
                        + "), dur " + rec3Duration, 78010));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording3: Should be INCOMPLETE after power-up", 78511));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording4: Start " + rec4Start + " (" + rec4StartDate
                        + "), dur " + rec4Duration, 79015));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording4: Should be FAILED after power-up", 79516));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording5: Start " + rec5Start + " (" + rec5StartDate
                        + "), dur " + rec5Duration, 80020));
                m_eventScheduler.scheduleCommand(new PrintOut(
                        "Recording5: Should be IN_PROGRESS_INCOMPLETE after power-up", 80521));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording6: Start " + rec6Start + " (" + rec6StartDate
                        + "), dur " + rec6Duration, 81025));
                m_eventScheduler.scheduleCommand(new PrintOut(
                        "Recording6: Should be PENDING_NO_CONFLICT after power-up", 81526));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new PrintOut("Recording0: Start " + rec0Start + " (" + rec0StartDate
                        + "), dur " + rec0Duration, 76000));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording0: Should be CANCELLED after power-up", 76501));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording2: Start " + rec2Start + " (" + rec2StartDate
                        + "), dur " + rec2Duration, 77005));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording2: Should be INCOMPLETE after power-up", 77506));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording3: Start " + rec3Start + " (" + rec3StartDate
                        + "), dur " + rec3Duration, 78010));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording3: Should be FAILED after power-up", 78511));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording4: Start " + rec4Start + " (" + rec4StartDate
                        + "), dur " + rec4Duration, 79015));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording4: Should be FAILED after power-up", 79516));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording5: Start " + rec5Start + " (" + rec5StartDate
                        + "), dur " + rec5Duration, 80020));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording5: Should be IN_PROGRESS after power-up", 80521));
                m_eventScheduler.scheduleCommand(new PrintOut("Recording6: Start " + rec6Start + " (" + rec6StartDate
                        + "), dur " + rec6Duration, 81025));
                m_eventScheduler.scheduleCommand(new PrintOut(
                        "Recording6: Should be PENDING_NO_CONFLICT after power-up", 81526));
            }
            // Run and wait for 5 seconds , shutdown should happen during this
            // time.
            m_eventScheduler.scheduleCommand(new PrintOut("Time now: " + now, 85000));
            m_eventScheduler.scheduleCommand(new PrintOut("*******************************************************",
                    86000));
            m_eventScheduler.scheduleCommand(new PrintOut("*******************************************************",
                    87001));
            m_eventScheduler.scheduleCommand(new PrintOut("*** SNAPSHOT ME - REBOOT BOX NOW - POWER UP IN " + downTime
                    / m_oneSecond + " SECONDS ***", 88010));
            m_eventScheduler.scheduleCommand(new PrintOut("*******************************************************",
                    89019));
            m_eventScheduler.scheduleCommand(new PrintOut("*******************************************************",
                    90020));
            m_eventScheduler.run(5000);

            sendTestDoneMsg();
        }

        private OcapLocator m_locator;
    }

    /**
     * Schedule a recording for testing re-start of expiration and purge timers.
     * 
     * @param locator
     * 
     * @author Jeff Spruiel
     */
    public class TestPersistentPurge extends TestCase
    {
        TestPersistentPurge(OcapLocator locator)
        {
            m_locator = locator;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test Persistent Purge";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();

            // check disk for empty space if not send error message
            diskFreeCheck();
            if (m_failed == TEST_INTERNAL_ERROR)
            {
                DVRTestRunnerXlet.log("TestPersistentRecordings: TEST_INTERNAL_ERROR - DISK FULL");
            }

            /**
             * Schedule 1 recording: Start the recording and wait for it to
             * complete. Reboot the box and relaunch the stack. In the stack,
             * you should find the message "only re-starting expiration timer".
             * Upon expiration you will see the message
             * "Scheduler In ExpirationSpec handler". After expiration handling
             * completes, (give it a few seconds to process the event). Reboot
             * the box again and relaunch the stack. You should see the trace,
             * "re-starting purge timer". Depending on the time for purge to
             * expire, you will see the message
             * "Scheduler In PurgeSpec handler".
             * 
             * Finally, display the recordings to verify the recording is no
             * longer available.
             */
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 30000, 700000, 500, 120));

            // Run and wait for 5 seconds , shutdown should happen during this
            // time.
            // m_eventScheduler.scheduleCommand(new PrintRecordings(56000));
            m_eventScheduler.scheduleCommand(new PrintOut("Reboot box now - Leave down for 60 seconds", 60000));
            m_eventScheduler.run(5000);

        }

        private OcapLocator m_locator;
    }

    /**
     * Schedule a recording for testing of multiple recordings starting
     * 
     * @param locator
     * 
     * @author ryanh
     */
    public class TestPersistentRecordingContention extends TestCase
    {
        private boolean m_with_conflicts;

        TestPersistentRecordingContention(OcapLocator locator, boolean with_conflicts)
        {
            m_locator = locator;
            m_with_conflicts = with_conflicts;
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "Test Persistent Recording Contention: " + (m_with_conflicts ? "with conflicts" : "if no conflicts");
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();
            getUDPParams();

            // check disk for empty space if not send error message
            diskFreeCheck();
            if (m_failed == TEST_INTERNAL_ERROR)
            {
                DVRTestRunnerXlet.log("TestPersistentRecordings: TEST_INTERNAL_ERROR - DISK FULL");
            }

            /**
             * Schedule 3 recordings at the same time The first two should fire
             * off and go to the INCOMPLETE_STATE The last should be in the
             * FAILED_STATE
             * 
             */
            if (m_with_conflicts)
            {
                m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 60000, 480000, 500,
                        1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                        OcapRecordingProperties.RECORD_WITH_CONFLICTS));
                m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 60000, 900000, 510,
                        1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                        OcapRecordingProperties.RECORD_WITH_CONFLICTS));
                m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 60000, 900000, 520,
                        1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                        OcapRecordingProperties.RECORD_WITH_CONFLICTS));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 60000, 600000, 500));
                m_eventScheduler.scheduleCommand(new Record("Recording2", m_locator, now + 60000, 600000, 510));
                m_eventScheduler.scheduleCommand(new Record("Recording3", m_locator, now + 60000, 600000, 520));
            }

            if (m_with_conflicts)
            {
                m_eventScheduler.scheduleCommand(new AddAppData("Recording1", "contention_w_conflict", "Recording1",
                        30100));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording2", "contention_w_conflict", "Recording2",
                        30200));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording3", "contention_w_conflict", "Recording3",
                        30300));
            }
            else
            {
                m_eventScheduler.scheduleCommand(new AddAppData("Recording1", "contention", "Recording1", 30100));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording2", "contention", "Recording2", 30200));
                m_eventScheduler.scheduleCommand(new AddAppData("Recording3", "contention", "Recording3", 30300));
            }

            // Run and wait for 5 seconds , shutdown should happen during this
            // time.
            // m_eventScheduler.scheduleCommand(new PrintRecordings(56000));
            m_eventScheduler.scheduleCommand(new PrintOut("Reboot box now - Leave down for 30 seconds", 40000));
            m_eventScheduler.run(5000);

            sendTestDoneMsg();
        }

        private OcapLocator m_locator;
    }

    /**
     * 
     * @author Ryan
     * 
     *         NotifyShell object that allows recordings to be serialized
     */
    class AddAppData extends NotifyShell
    {
        private String m_rec;

        private String m_key;

        private String m_data;

        /**
         * @param string
         * @param string2
         * @param string3
         * @param i
         */
        public AddAppData(String rec, String key, String data, int triggerTime)
        {
            super(triggerTime);
            m_rec = rec;
            m_key = key;
            m_data = data;
        }

        public void ProcessCommand()
        {
            RecordingRequest rr = null;
            try
            {
                rr = (RecordingRequest) findObject(m_rec);
                if (rr == null)
                {
                    m_failed = TEST_FAILED;
                    System.out.println("<<<<<AddAppData recording - recording not found! " + m_rec + " >>>>>>");
                    return;
                }
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                e.printStackTrace();
            }
            try
            {
                rr.addAppData(m_key, (Serializable) m_data);
                DVRTestRunnerXlet.log(" <<<<AddAppData : KEY: " + m_key + "   DATA:" + m_data + " >>>>");
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                e.printStackTrace();
            }
        }
    }

    public void sendTestDoneMsg()
    {
        UDPLogger sender = null;
        try
        {
            if (autotestserver != null)
            {
                if (autotestport != null)
                {
                    sender = new UDPLogger(autotestserver, autotestport.intValue());
                    if (sender != null)
                        sender.send("AutoTestDone");
                    else
                        System.out.println("sender is null");
                }
                else
                {
                    System.out.println("autotestport is null");
                }
            }
            else
            {
                System.out.println("autotestserver is null");
            }
        }
        catch (SocketException e)
        {
            System.out.println("Socket exception send failed");
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            System.out.println("Illegal arg send failed");
            e.printStackTrace();
        }
        if (sender != null) sender.close();
    }

    public void getUDPParams()
    {
        try
        {
            ArgParser xletArgs = new ArgParser((String[]) DVRTestRunnerXlet.getContext().getXletProperty(
                    XletContext.ARGS));

            String configFile = xletArgs.getStringArg("config_file");
            FileInputStream fis = new FileInputStream(configFile);
            ArgParser fopts = new ArgParser(fis);

            autotestserver = fopts.getStringArg("AutoTestServer");
            autotestport = fopts.getIntegerArg("AutoTestPort");
            System.out.println("AutoTestDoneXlet args: autotest server:port=" + autotestserver + ":"
                    + autotestport.intValue());
        }
        catch (Exception e)
        {
            System.out.println("AutoTestDoneXlet error getting args");
            e.printStackTrace();
        }
    }

}
