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
 * Created on Feb 25, 2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.cablelabs.xlet.DvrTest;

import java.io.FileInputStream;
import java.io.Serializable;
import java.net.SocketException;
import java.util.Vector;

import javax.tv.xlet.XletContext;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;

import org.cablelabs.test.autoxlet.ArgParser;
import org.cablelabs.test.autoxlet.UDPLogger;
import org.cablelabs.xlet.DvrTest.EventScheduler.NotifyShell;

/**
 * @author Ryan
 * 
 *         TODO To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class TestPropertiesChangesOnCompletedRecording extends DvrTest
{

    private String autotestserver = null;

    private Integer autotestport = null;

    /**
     * @param locators
     */
    TestPropertiesChangesOnCompletedRecording(Vector locators)
    {
        super(locators);
        // TODO Auto-generated constructor stub
    }

    public Vector getTests()
    {
        Vector tests = new Vector();

        tests.addElement(new TestPropertiesChangesOnCompletedRecording1((OcapLocator) m_locators.elementAt(0)));
        tests.addElement(new TestPropertiesChangesOnCompletedRecording2());
        return tests;
    }

    /**
     * @author Ryan
     * 
     *         First part of the test that schedules a recording and sets the
     *         app data so that it can be found after a reboot
     */
    public class TestPropertiesChangesOnCompletedRecording2 extends TestCase
    {

        /**
		 */
        public TestPropertiesChangesOnCompletedRecording2()
        {
            m_eventScheduler = new EventScheduler();
            // TODO Auto-generated constructor stub
        }

        public Runnable getTest()
        {
            return this;
        }

        public String getName()
        {
            return "TestPropertiesChangesOnCompletedRecording - find recording and modify";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;

            // clear the schedule of pending tasks
            reset();
            // Get the recording and give it a name
            getRecReq("comRec", "persistent_rec", "yes");

            // Change the retention priority
            m_eventScheduler.scheduleCommand(new ReschedulePriority("comRec", 2, 20000));
            m_eventScheduler.scheduleCommand(new VerifyPriority("comRec", OcapRecordingProperties.DELETE_AT_EXPIRATION,
                    22050));

            // wait for expiration (at 60 seconds), then reschedule expiration
            m_eventScheduler.scheduleCommand(new RescheduleExpiration("comRec", 60000, 30000));
            m_eventScheduler.scheduleCommand(new VerifyExpiration("comRec", (1000 * 60 * 60 * 24), 32050));

            // Check for status prior epiration
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("comRec",
                    OcapRecordingRequest.COMPLETED_STATE, 40000));

            // Delete Recording
            m_eventScheduler.scheduleCommand(new DeleteRecordingRequest("comRec", 50000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestPropertiesChangesOnCompletedRecording - find recording and modify TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestPropertiesChangesOnCompletedRecording - find recording and modify: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestPropertiesChangesOnCompletedRecording - find recording and modify: PASSED");
            }
        }

    }

    /**
     * @author Ryan
     * 
     *         Second part of test that retrieves the presisted recording and
     *         changes properties within the Recording.
     */
    public class TestPropertiesChangesOnCompletedRecording1 extends TestCase
    {

        /**
         * @param locator
         */
        public TestPropertiesChangesOnCompletedRecording1(OcapLocator locator)
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
            return "TestPropertiesChangesOnCompletedRecording - create recording w/ AppData";
        }

        public void runTest()
        {
            m_failed = TEST_PASSED;
            long now = System.currentTimeMillis();

            // clear the schedule of pending tasks
            reset();
            getUDPParams();
            // Schedule the record call
            m_eventScheduler.scheduleCommand(new Record("Recording1", m_locator, now + 10000, 30000, 500));
            // Check the recordings
            m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState("Recording1",
                    OcapRecordingRequest.COMPLETED_STATE, 40000 + 5000));
            // Add a marker in the recording so we can find it.
            m_eventScheduler.scheduleCommand(new AddAppData("Recording1", "persistent_rec", "yes", 50000));

            m_eventScheduler.run(1000);

            if (m_failed == TEST_FAILED)
            {
                diskFreeCheck();
                if (m_failed == TEST_INTERNAL_ERROR)
                {
                    DVRTestRunnerXlet.log("TestPropertiesChangesOnCompletedRecording - create recording TEST_INTERNAL_ERROR - DISK FULL");
                }
                else
                {
                    DVRTestRunnerXlet.log("TestPropertiesChangesOnCompletedRecording - create recording: FAILED");
                }
            }
            else
            {
                DVRTestRunnerXlet.log("TestPropertiesChangesOnCompletedRecording - create recording: PASSED");
            }
        }

        OcapLocator m_locator;
    }

    /**
     * @author Ryan
     * 
     *         Retrieves the recording with a key.
     */
    public void getRecReq(String recName, String key, String data)
    {
        // TODO Auto-generated constructor stub

        System.out.println("<<<<<<<<< getRecReq CALLED >>>>>>>>>>>>>>>>");

        OcapRecordingManager rm = (OcapRecordingManager) OcapRecordingManager.getInstance();
        RecordingList rl = rm.getEntries();

        if (rl == null)
        {
            System.out.println("<<<<getRecReq: RecordingList = null!");
            m_failed = TEST_FAILED;
            return;
        }

        System.out.println("<<<<<<<<< getRecReq : " + rl.size() + " entries >>>>>>>>>>>>>>>>");

        for (int x = 0; x < rl.size(); x++)
        {
            try
            {
                RecordingRequest rr = rl.getRecordingRequest(x);
                String info = (String) rr.getAppData(key);
                System.out.println("<<<<<<<<<ENTRY DATA :" + info + " >>>>>>>>>>>");
                if ((info != null) && (info.equals("yes")))
                {
                    DVRTestRunnerXlet.log("<<<< REQUEST FOUND! giving request the name : " + recName + " >>>>");
                    insertObject(rr, recName);
                    return;
                }
                else
                {
                    System.out.println("<<<<< ENTRY " + x + " not it!>>>>>");
                }
            }
            catch (Exception e)
            {
                System.out.println("<<<<getRecReq: Exception finding recording request" + rl.getRecordingRequest(x));
                m_failed = TEST_FAILED;
                m_failedReason = "getRecReq: Exception finding recording request" + rl.getRecordingRequest(x) + ": "
                        + e.getMessage();
                e.printStackTrace();
            }
        }
    }

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

    /**
     * Schedules a future call to reschedule a recording's expiration time At
     * the task's trigger time, a call to RecordingRequest.reschedule will be
     * made with the parameters specified.
     */
    class RescheduleExpiration extends NotifyShell
    {
        /**
         * Schedule a recording request to be rescheduled.
         * 
         * @param recordingName
         *            DvrTest recording name
         * @param newExpiration
         *            New expiration period
         * @param taskTriggerTime
         *            Time offset for scheduling the reschedule.
         */
        RescheduleExpiration(String recordingName, long newExpiration, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_expiration = newExpiration;
            m_recordingName = recordingName;
        }

        public void ProcessCommand()
        {
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recordingName);

            if (rr == null)
            {
                System.out.println("DVRUtils: RescheduleExpiration - recording not found: " + m_recordingName);
                return;
            }
            LocatorRecordingSpec oldLrs = null, newLrs = null;
            try
            {

                oldLrs = (LocatorRecordingSpec) (rr.getRecordingSpec());

                OcapRecordingProperties oldOrp = (OcapRecordingProperties) oldLrs.getProperties();
                OcapRecordingProperties newOrp = new OcapRecordingProperties(oldOrp.getBitRate(), m_expiration,
                        oldOrp.getRetentionPriority(), oldOrp.getPriorityFlag(), oldOrp.getAccessPermissions(),
                        oldOrp.getOrganization(), oldOrp.getDestination());

                System.out.println("DVRUtils: issuing reschedule for " + m_recordingName + ": " + " Expiration: "
                        + m_expiration);

                newLrs = new LocatorRecordingSpec(oldLrs.getSource(), oldLrs.getStartTime(), oldLrs.getDuration(),
                        newOrp);
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Reschedule: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Reschedule due to exception on lrs: " + e.toString());
            }
            try
            {
                rr.reschedule(newLrs);
                System.out.println("DVRUtils: Reschedule: FAILED - Exception was to be thrown here");
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Reschedule: Exception not thrown ");
            }
            catch (IllegalStateException e)
            {
                DVRTestRunnerXlet.log("DvrTest: PASSED IllegalStateException caught ");
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Reschedule: Wrong exception thrown: " + e.toString());
            }
        }

        private String m_recordingName;

        private long m_expiration;
    } // END class RescheduleExpiration

    /**
     * Schedules a future call to check a recording's retention prioirty
     */
    public class VerifyExpiration extends NotifyShell
    {

        private int m_expiration;

        private String m_recordingName;

        /**
         * @param time
         */
        VerifyExpiration(String recordingName, int newExpiration, long time)
        {
            super(time);
            m_expiration = newExpiration;
            m_recordingName = recordingName;
        }

        public void ProcessCommand()
        {
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recordingName);

            if (rr == null)
            {
                System.out.println("DVRUtils: VerifyExpiration - recording not found: " + m_recordingName);
                return;
            }

            try
            {
                LocatorRecordingSpec Lrs;
                Lrs = (LocatorRecordingSpec) (rr.getRecordingSpec());

                OcapRecordingProperties Orp = (OcapRecordingProperties) Lrs.getProperties();
                if (Orp.getExpirationPeriod() != m_expiration)
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in VerifyExpiration. Expected RetPri: "
                            + m_expiration + " received: " + Orp.getExpirationPeriod());
                }
                else
                {
                    DVRTestRunnerXlet.log("DvrTest: VerifyExpiration PASSED");
                }
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Reschedule: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Reschedule due to exception: " + e.toString());
            }
        }
    }

    /**
     * Schedules a future call to reschedule a recording's retention prioirty At
     * the task's trigger time, a call to RecordingRequest.reschedule will be
     * made with the parameters specified.
     */
    class ReschedulePriority extends NotifyShell
    {
        /**
         * Schedule a recording request to be rescheduled.
         * 
         * @param recordingName
         *            DvrTest recording name
         * @param newExpiration
         *            New expiration period
         * @param taskTriggerTime
         *            Time offset for scheduling the reschedule.
         */
        ReschedulePriority(String recordingName, int newPriority, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_priority = newPriority;
            m_recordingName = recordingName;
        }

        public void ProcessCommand()
        {
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recordingName);

            if (rr == null)
            {
                System.out.println("DVRUtils: ReschedulePriority - recording not found: " + m_recordingName);
                return;
            }
            LocatorRecordingSpec oldLrs = null, newLrs = null;
            try
            {
                oldLrs = (LocatorRecordingSpec) (rr.getRecordingSpec());

                OcapRecordingProperties oldOrp = (OcapRecordingProperties) oldLrs.getProperties();
                OcapRecordingProperties newOrp = new OcapRecordingProperties(oldOrp.getBitRate(),
                        oldOrp.getExpirationPeriod(), m_priority, oldOrp.getPriorityFlag(),
                        oldOrp.getAccessPermissions(), oldOrp.getOrganization(), oldOrp.getDestination());

                System.out.println("DVRUtils: issuing reschedule for " + m_recordingName + ": " + " RetPriority: "
                        + m_priority);

                newLrs = new LocatorRecordingSpec(oldLrs.getSource(), oldLrs.getStartTime(), oldLrs.getDuration(),
                        newOrp);
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Reschedule: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Reschedule due to exception on lrs: " + e.toString());
            }
            try
            {
                rr.reschedule(newLrs);
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Reschedule : no exception thrown");
            }
            catch (IllegalStateException e)
            {
                DVRTestRunnerXlet.log("DvrTest: PASSED IllegalStateException thrown");
            }
            catch (Exception e)
            {
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Reschedule due to unexpected exception thrown: "
                        + e.toString());
            }
        }

        private String m_recordingName;

        private int m_priority;
    } // END class ReschedulePriority

    /**
     * Schedules a future call to check a recording's retention prioirty
     */
    class VerifyPriority extends NotifyShell
    {
        /**
         * Schedule a recording request to be rescheduled.
         * 
         * @param recordingName
         *            DvrTest recording name
         * @param newExpiration
         *            New expiration period
         * @param taskTriggerTime
         *            Time offset for scheduling the reschedule.
         */
        VerifyPriority(String recordingName, int newPriority, long taskTriggerTime)
        {
            super(taskTriggerTime);
            m_priority = newPriority;
            m_recordingName = recordingName;
        }

        public void ProcessCommand()
        {
            OcapRecordingRequest rr = (OcapRecordingRequest) findObject(m_recordingName);

            if (rr == null)
            {
                System.out.println("DVRUtils: VerifyPriority - recording not found: " + m_recordingName);
                return;
            }

            try
            {
                LocatorRecordingSpec Lrs;
                Lrs = (LocatorRecordingSpec) (rr.getRecordingSpec());

                OcapRecordingProperties Orp = (OcapRecordingProperties) Lrs.getProperties();
                if (Orp.getRetentionPriority() != m_priority)
                {
                    m_failed = TEST_FAILED;
                    DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in VerifyPriority. Expected RetPri: " + m_priority
                            + " received: " + Orp.getRetentionPriority());
                }
                else
                {
                    DVRTestRunnerXlet.log("DvrTest: VerifyPriority PASSED");
                }
            }
            catch (Exception e)
            {
                System.out.println("DVRUtils: Reschedule: FAILED");
                e.printStackTrace();
                m_failed = TEST_FAILED;
                DVRTestRunnerXlet.log("DvrTest: Flagged FAILURE in Reschedule due to exception: " + e.toString());
            }
        }

        private String m_recordingName;

        private int m_priority;
    } // END class VerifyPriority

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
