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
 * Created on Jan 4, 2005
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.cablelabs.xlet.DvrTest;

import java.io.FileInputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import javax.tv.util.TVTimer;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import org.dvb.ui.DVBColor;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.ocap.OcapSystem;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.xlet.DvrTest.DvrTest.DeleteRecordingRequest;

/**
 * DvrTestMonAppXlet - Tests the setRecordingDelay() method and the
 * monitorConfiguredSignal() method of the stack. This xlet contains the test
 * cases for WS22b ECR856. The test cases are as follows: - Set Recording Delay
 * Within Range. - Set Recording Delay Within Range, Recording End After Delay
 * Start. - Set Recording Delay Within Range, Recording End Before Delay Start.
 * - Set Recording Delay Within Range, Recording Begin And End Before Delay
 * Start. - Set Recording Delay Within Range, Recording Begin Before Delay
 * Start. - Set Recording Delay Within Range, Delay Recording Start Called After
 * Monitor Configured Signal Call. - Set Recording Delay Within Range, Delay
 * Recording Start Called After Monitor Configured Signal TimeOut.
 * 
 * In order to run these test cases the mpeenv.ini file must contain the
 * following parameter: OCAP.monapp.resident=true This parameter allows the
 * first application that is launched when the stack starts to be the resident
 * mon app, in this case it will be DvrTestMonAppXlet.
 * 
 * This xlet also assumes that the DvrTestRunnerXlet has been launched and a
 * recording has been scheduled from the ECR856 Tests group. If a recording has
 * not been scheduled then this xlet will return with a failure stating that no
 * recording was found.
 * 
 * Once a recording has been scheduled from the ECR856 Tests group within the
 * DVRTestRunner, the hostapp.properties file must be changed before restarting
 * the set-top box and launching DvrTestMonAppXlet. Rename the file
 * ecr856_hostapp.properties to hostapp.properties.
 * 
 * The config.properties file must also be modified prior to running this xlet.
 * The file looks as follows: #Used by ECR856 testing to choose which test to
 * perform # # 1.1 = SetRecDelayWithinRange # 1.2 =
 * SetRecDelayWithinRangeStopAfterDelayStart # 1.3 =
 * SetRecDelayWithinRangeStopBeforeDelayStart # 1.4 =
 * SetRecDelayWithinRangeStartStopBeforeDelayStart # 1.5 =
 * SetRecDelayWithinRangeStartBeforeDelayStart # 1.6 =
 * SetRecDelayWithinRangeAfterMonConfSignal # 1.7 =
 * SetRecDelayWithinRangeAfterMonConfSignalTimeOut # DVR_ecr856_test=1.1
 * 
 * The parameter, DVR_ecr856_test= determines which test case will be run when
 * DvrTestMonAppXlet is launched. The other caveat for successfully running the
 * test is that the previously scheduled recording must correspond to the test
 * case being launched by DvrTestMonAppXlet. For example, in order to launch
 * test case 1.1 - Set Recording Delay Within Range, TestECR856 -
 * ScheduleLongRecording - 1.1, 1.2, 1.6, 1.7 must first be selected from the
 * ECR856 Tests group within DvrTestRunnerXlet.
 * 
 * @author bforan
 * 
 */
public class DvrTestMonAppXlet implements Xlet
{
    OcapRecordingManager m_recordingManager;

    /**
     * implementation of Xlet interface
     */
    public void initXlet(XletContext ctx)
    {
        m_recordingManager = (OcapRecordingManager) OcapRecordingManager.getInstance();
        m_recordingManager.setRecordingDelay(60L);

        // initialize AutoXlet
        System.out.println("DvrTestMonAppXlet:initXlet()");

        // store off our xlet context
        m_xctx = ctx;
        /*
         * Grab valid service locators from Xlet params
         */
        m_serviceLocators = retrieveDvrLocators(ctx);

        System.out.println("Setting up havi interface");
        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_vbox = new VidTextBox(50, 50, 530, 370, 14, 5000);
        m_vbox.setBackgroundColor(new DVBColor(128, 128, 128, 155));
        m_vbox.setForegroundColor(new DVBColor(200, 200, 200, 255));

        scene.add(m_vbox);
        scene.addKeyListener(m_vbox);

        m_timer = TVTimer.getTimer();

        m_testList = new Vector();

        m_testHashtable = new Hashtable();
    }

    /**
     * implementation of Xlet interface
     */
    public void startXlet()
    {
        System.out.println("DvrTestMonAppXlet:startXlet()");

        scene.show();
        scene.requestFocus();

        log("<<<<DvrTestMonApp - ECR856");

        populateTestHashtable();
        addTests(new DvrTestMonApp(m_serviceLocators).getTests());
        launchTest();
    }

    /**
     * implementation of Xlet interface
     */
    public void pauseXlet()
    {
        System.out.println("DvrTestMonAppXlet:pauseXlet()");
        scene.setVisible(false);
    }

    /**
     * implementation of Xlet interface
     */
    public void destroyXlet(boolean x)
    {
        System.out.println("DvrTestMonAppXlet:destroyXlet");
        // hide scene
        scene.setVisible(false);
        // dispose of self
        HScene tmp = scene;
        scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    public void addTests(Vector tests)
    {
        for (int i = 0; i < tests.size(); i++)
        {
            m_testList.addElement(tests.elementAt(i));
        }
    }

    public void launchTest()
    {
        int testSelection = -1;
        String testSelectionName = (String) m_testHashtable.get(m_testSelection);
        System.out.println("<<<<Looking for test: " + testSelectionName);

        for (int i = 0; i < m_testList.size(); i++)
        {
            DvrMonAppTest.TestCase testCase = (DvrMonAppTest.TestCase) m_testList.elementAt(i);
            if (testCase.getName().equals(testSelectionName))
            {
                testSelection = i;
            }
        }
        if (testSelection >= 0)
        {
            DvrMonAppTest.TestCase test = (DvrMonAppTest.TestCase) m_testList.elementAt(testSelection);

            if (test == null)
            {
                log("<<<<ERROR - " + testSelectionName + " not found! (null)");
            }
            else
            {
                log("<<<<Running test <" + test.getName() + ">");

                Thread tcrt = new Thread(test);
                tcrt.start();
            }
        }
        else
        {
            log("<<<<ERROR - " + testSelectionName + " not found! (-1)");
        }
    }

    public void populateTestHashtable()
    {
        m_testHashtable.put("1.1", "SetRecDelayWithinRange");
        m_testHashtable.put("1.2", "SetRecDelayWithinRangeStopAfterDelayStart");
        m_testHashtable.put("1.3", "SetRecDelayWithinRangeStopBeforeDelayStart");
        m_testHashtable.put("1.4", "SetRecDelayWithinRangeStartStopBeforeDelayStart");
        m_testHashtable.put("1.5", "SetRecDelayWithinRangeStartBeforeDelayStart");
        m_testHashtable.put("1.6", "SetRecDelayWithinRangeAfterMonConfSignal");
        m_testHashtable.put("1.7", "SetRecDelayWithinRangeAfterMonConfSignalTimeOut");
    }

    /**
     * Logging function Allows logging messages to post in the screen and in the
     * log
     * 
     * @param e
     */
    public static void log(String msg)
    {
        System.out.println(msg);
        m_vbox.write(msg);
    }

    public static void notifyTestComplete(String name, int result, String reason)
    {
        String testResult = "PASSED";
        if (result != 0)
        {
            testResult = "FAILED: " + reason;
        }
    }

    /**
     * Read DVR source ids from the config file, and build an array of OCAP
     * Locators which can be used for tuning and scheduling.
     * 
     * @param ctx
     * @return
     */
    public synchronized Vector retrieveDvrLocators(XletContext ctx)
    {
        Vector locators = new Vector();
        FileInputStream fis_count = null;
        FileInputStream fis_read = null;

        try
        {
            // Get path name of config file.
            ArgParser xlet_args = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));
            String str_config_file_name = xlet_args.getStringArg(CONFIG_FILE);

            // Count DVR source ids in config file.
            fis_count = new FileInputStream(str_config_file_name);
            byte[] buffer = new byte[fis_count.available() + 1];
            fis_count.read(buffer);
            String str_config_file = new String(buffer);

            int sourceId_count = -1;
            while (str_config_file.indexOf(DVR_SOURCE_ID + ++sourceId_count) != -1);

            fis_count.close();

            // Read DVR source ids from config file.
            fis_read = new FileInputStream(str_config_file_name);
            ArgParser config_args = new ArgParser(fis_read);

            for (int i = 0; i < sourceId_count; i++)
            {
                String str_source_id = config_args.getStringArg(DVR_SOURCE_ID + i);
                locators.addElement((Object) (new OcapLocator("ocap://" + str_source_id)));
                System.out.println("<<<<DVR Tests locator: " + locators.elementAt(i));
            }

            m_testSelection = config_args.getStringArg(DVR_TEST_SELECTION);
            System.out.println("<<<<DVR ECR856 Test: " + m_testSelection);

            fis_read.close();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return locators;
    }

    /**
     * The test that will be launched by the DvrTestMonAppXlet
     */
    private String m_testSelection = null;

    /**
     * Contains a list of locators which can be used for scheduling/recording
     */
    private Vector m_serviceLocators = null;

    /**
     * Private HAVi scene for receiving UI events
     */
    private HScene scene = null;

    /**
     * Our xlet context
     */
    private XletContext m_xctx = null;

    private TVTimer m_timer;

    private Vector m_testList;

    private Hashtable m_testHashtable;

    private static VidTextBox m_vbox;

    private static final String CONFIG_FILE = "config_file";

    private static final String DVR_SOURCE_ID = "DVR_sourceId_";

    private static final String DVR_TEST_SELECTION = "DVR_ecr856_test";

    // -------------

    public class DvrTestMonApp extends DvrMonAppTest
    {
        private Vector m_eventList;

        /**
         * @param locators
         */
        DvrTestMonApp(Vector locators)
        {
            super(locators);
            m_eventList = new Vector();
            m_eventScheduler = new EventScheduler();
        }

        public Vector getTests()
        {
            Vector tests = new Vector();
            tests.addElement(new SetRecDelayWithinRange((OcapLocator) m_locators.elementAt(0)));
            tests.addElement(new SetRecDelayWithinRangeStopAfterDelayStart((OcapLocator) m_locators.elementAt(0)));
            tests.addElement(new SetRecDelayWithinRangeStopBeforeDelayStart((OcapLocator) m_locators.elementAt(0)));
            tests.addElement(new SetRecDelayWithinRangeStartStopBeforeDelayStart((OcapLocator) m_locators.elementAt(0)));
            tests.addElement(new SetRecDelayWithinRangeStartBeforeDelayStart((OcapLocator) m_locators.elementAt(0)));
            tests.addElement(new SetRecDelayWithinRangeAfterMonConfSignal((OcapLocator) m_locators.elementAt(0)));
            tests.addElement(new SetRecDelayWithinRangeAfterMonConfSignalTimeOut((OcapLocator) m_locators.elementAt(0)));
            return tests;
        }

        public class SetRecDelayWithinRange extends TestCase
        {
            private OcapLocator m_locator;

            SetRecDelayWithinRange(OcapLocator locator)
            {
                m_locator = locator;
            }

            public Runnable getTest()
            {
                return this;
            }

            public String getName()
            {
                return "SetRecDelayWithinRange";
                // return "setRecordingDelay() within set range";
            }

            public void runTest()
            {
                String longRecordingRWC = "ECR856_longRecording_rwc";

                m_failed = TEST_PASSED;

                boolean recordingFound = discoverRecording(longRecordingRWC, longRecordingRWC, longRecordingRWC);

                if (recordingFound)
                {
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(longRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 100));

                    m_recordingManager.setRecordingDelay(60L);
                    OcapSystem.monitorConfiguredSignal();

                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(longRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 200));
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(longRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 65000));

                    // delete recording before shutting down
                    m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(longRecordingRWC, 90000));

                    m_eventScheduler.run(300);
                }

                if (m_failed != TEST_FAILED)
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": PASSED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else if (!recordingFound)
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + " - No Recording Found");
                    DvrTestMonAppXlet.log(getName() + " - Need To Run First Part Of Test");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": FAILED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
            }
        }

        public class SetRecDelayWithinRangeStopAfterDelayStart extends TestCase
        {
            private OcapLocator m_locator;

            SetRecDelayWithinRangeStopAfterDelayStart(OcapLocator locator)
            {
                m_locator = locator;
            }

            public Runnable getTest()
            {
                return this;
            }

            public String getName()
            {
                return "SetRecDelayWithinRangeStopAfterDelayStart";
                // return
                // "setRecordingDelay() within set range; stop time after Delay Start";
            }

            public void runTest()
            {
                String longRecordingRWC = "ECR856_longRecording_rwc";

                m_failed = TEST_PASSED;

                boolean recordingFound = discoverRecording(longRecordingRWC, longRecordingRWC, longRecordingRWC);

                if (recordingFound)
                {
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(longRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 100));

                    m_recordingManager.setRecordingDelay(60L);
                    // Wait for monitorConfiguredSignal to timeout (5 seconds)

                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(longRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 5400));
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(longRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 75000));

                    // delete recording before shutting down
                    m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(longRecordingRWC, 90000));

                    m_eventScheduler.run(300);
                }

                if (m_failed != TEST_FAILED)
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": PASSED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else if (!recordingFound)
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + " - No Recording Found");
                    DvrTestMonAppXlet.log(getName() + " - Need To Run First Part Of Test");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": FAILED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
            }
        }

        public class SetRecDelayWithinRangeStopBeforeDelayStart extends TestCase implements RecordingChangedListener
        {
            private OcapLocator m_locator;

            private String recordingName = "ECR856_longRecording_rwc";

            SetRecDelayWithinRangeStopBeforeDelayStart(OcapLocator locator)
            {
                m_locator = locator;
            }

            public Runnable getTest()
            {
                return this;
            }

            public String getName()
            {
                return "SetRecDelayWithinRangeStopBeforeDelayStart";
                // return
                // "setRecordingDelay() within set range; stop time before Delay Start";
            }

            public void runTest()
            {
                String shortRecordingRWC = "ECR856_shortRecording_rwc";

                m_failed = TEST_PASSED;

                m_recordingManager.addRecordingChangedListener(this);

                boolean recordingFound = discoverRecording(shortRecordingRWC, shortRecordingRWC, shortRecordingRWC);

                if (recordingFound)
                {
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(shortRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 100));

                    m_recordingManager.setRecordingDelay(300L);
                    // the current recording needs to end before the set
                    // recording delay time is up
                    // Wait for monitorConfiguredSignal to timeout (5 seconds)

                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(shortRecordingRWC,
                            OcapRecordingRequest.INCOMPLETE_STATE, 305000));

                    // delete recording before shutting down
                    m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(shortRecordingRWC, 315000));

                    m_eventScheduler.run(200);
                }

                if (m_failed != TEST_FAILED)
                {
                    // This could become the passed scenario
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": PASSED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else if (!recordingFound)
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + " - No Recording Found");
                    DvrTestMonAppXlet.log(getName() + " - Need To Run First Part Of Test");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": FAILED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
            }

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.ocap.shared.dvr.RecordingChangedListener#recordingChanged
             * (org.ocap.shared.dvr.RecordingChangedEvent)
             */
            public void recordingChanged(RecordingChangedEvent e)
            {
                if (e.getChange() == RecordingChangedEvent.ENTRY_STATE_CHANGED)
                {
                    DvrTestMonAppXlet.log("<<<<Recording Changed Event");
                    if (e.getOldState() == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE)
                    {
                        // check to see that it transitioned to the
                        // INCOMPLETE_STATE
                        if (e.getState() == LeafRecordingRequest.INCOMPLETE_STATE)
                        {
                            DvrTestMonAppXlet.log("<<<<" + recordingName
                                    + " changed state from IN_PROGRESS_WITH_ERROR_STATE to INCOMPLETE_STATE");
                        }
                        else
                        {
                            m_failed = TEST_FAILED;
                        }
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                    }
                }
            } // END recordingChanged()
        }

        public class SetRecDelayWithinRangeStartStopBeforeDelayStart extends TestCase implements
                RecordingChangedListener
        {
            private OcapLocator m_locator;

            private String recordingName = "ECR856_futureShortRecording_rwc";

            SetRecDelayWithinRangeStartStopBeforeDelayStart(OcapLocator locator)
            {
                m_locator = locator;
            }

            public Runnable getTest()
            {
                return this;
            }

            public String getName()
            {
                return "SetRecDelayWithinRangeStartStopBeforeDelayStart";
                // return
                // "setRecordingDelay() within set range; start and stop time before Delay Start";
            }

            public void runTest()
            {
                String futureShortRecordingRWC = "ECR856_futureShortRecording_rwc";

                m_failed = TEST_PASSED;

                m_recordingManager.addRecordingChangedListener(this);

                boolean recordingFound = discoverRecording(futureShortRecordingRWC, futureShortRecordingRWC,
                        futureShortRecordingRWC);

                if (recordingFound)
                {
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(futureShortRecordingRWC,
                            OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 100));

                    m_recordingManager.setRecordingDelay(300L);
                    // the current recording needs to begin and end before the
                    // set recording delay time is up
                    // Wait for monitorConfiguredSignal to timeout (5 seconds)

                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(futureShortRecordingRWC,
                            OcapRecordingRequest.FAILED_STATE, 305000));

                    // delete recording before shutting down
                    m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(futureShortRecordingRWC, 315000));

                    m_eventScheduler.run(200);
                }

                if (m_failed != TEST_FAILED)
                {
                    // This could become the passed scenario
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": PASSED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else if (!recordingFound)
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + " - No Recording Found");
                    DvrTestMonAppXlet.log(getName() + " - Need To Run First Part Of Test");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": FAILED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
            }

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.ocap.shared.dvr.RecordingChangedListener#recordingChanged
             * (org.ocap.shared.dvr.RecordingChangedEvent)
             */
            public void recordingChanged(RecordingChangedEvent e)
            {
                if (e.getChange() == RecordingChangedEvent.ENTRY_STATE_CHANGED)
                {
                    DvrTestMonAppXlet.log("<<<<Recording Changed Event");
                    if (e.getOldState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                    {
                        // check to see that it transitioned to the
                        // IN_PROGRESS_WITH_ERROR_STATE
                        if (e.getState() == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE)
                        {
                            DvrTestMonAppXlet.log("<<<<" + recordingName
                                    + " changed state from PENDING_NO_CONFLICT_STATE to IN_PROGRESS_WITH_ERROR_STATE");
                        }
                        else
                        {
                            m_failed = TEST_FAILED;
                        }
                    }
                    else if (e.getOldState() == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE)
                    {
                        // check to see that it transitioned to the FAILED_STATE
                        if (e.getState() == LeafRecordingRequest.FAILED_STATE)
                        {
                            DvrTestMonAppXlet.log("<<<<" + recordingName
                                    + " changed state from IN_PROGRESS_WITH_ERROR to FAILED_STATE");
                        }
                        else
                        {
                            m_failed = TEST_FAILED;
                        }
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                    }
                }
            } // END recordingChanged()
        }

        public class SetRecDelayWithinRangeStartBeforeDelayStart extends TestCase implements RecordingChangedListener
        {
            private OcapLocator m_locator;

            private String recordingName = "ECR856_futureLongRecording_rwc";

            SetRecDelayWithinRangeStartBeforeDelayStart(OcapLocator locator)
            {
                m_locator = locator;
            }

            public Runnable getTest()
            {
                return this;
            }

            public String getName()
            {
                return "SetRecDelayWithinRangeStartBeforeDelayStart";
                // return
                // "setRecordingDelay() within set range; start time before Delay Start (RWC)";
            }

            public void runTest()
            {
                String futureLongRecordingRWC = "ECR856_futureLongRecording_rwc";

                m_failed = TEST_PASSED;

                m_recordingManager.addRecordingChangedListener(this);

                boolean recordingFound = discoverRecording(futureLongRecordingRWC, futureLongRecordingRWC,
                        futureLongRecordingRWC);

                if (recordingFound)
                {
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(futureLongRecordingRWC,
                            OcapRecordingRequest.PENDING_NO_CONFLICT_STATE, 100));

                    // set recording delay to 5 minutes
                    m_recordingManager.setRecordingDelay(300L);
                    // Wait for monitorConfiguredSignal to timeout (5 seconds)

                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(futureLongRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 305000));

                    // delete recording before shutting down
                    m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(futureLongRecordingRWC, 315000));

                    m_eventScheduler.run(200);
                }

                if (m_failed != TEST_FAILED)
                {
                    // This could become the passed scenario
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": PASSED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else if (!recordingFound)
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + " - No Recording Found");
                    DvrTestMonAppXlet.log(getName() + " - Need To Run First Part Of Test");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": FAILED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
            }

            /*
             * (non-Javadoc)
             * 
             * @see
             * org.ocap.shared.dvr.RecordingChangedListener#recordingChanged
             * (org.ocap.shared.dvr.RecordingChangedEvent)
             */
            public void recordingChanged(RecordingChangedEvent e)
            {
                if (e.getChange() == RecordingChangedEvent.ENTRY_STATE_CHANGED)
                {
                    DvrTestMonAppXlet.log("<<<<Recording Changed Event");
                    if (e.getOldState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                    {
                        // check to see that it transitioned to the
                        // IN_PROGRESS_WITH_ERROR_STATE
                        if (e.getState() == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE)
                        {
                            DvrTestMonAppXlet.log("<<<<" + recordingName
                                    + " changed state from PENDING_NO_CONFLICT_STATE to IN_PROGRESS_WITH_ERROR_STATE");
                        }
                        else
                        {
                            m_failed = TEST_FAILED;
                        }
                    }
                    else if (e.getOldState() == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE)
                    {
                        // check to see that it transitioned to the
                        // IN_PROGRESS_INCOMPLETE_STATE
                        if (e.getState() == LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE)
                        {
                            DvrTestMonAppXlet.log("<<<<"
                                    + recordingName
                                    + " changed state from IN_PROGRESS_WITH_ERROR_STATE to IN_PROGRESS_INCOMPLETE_STATE");
                        }
                        else
                        {
                            m_failed = TEST_FAILED;
                        }
                    }
                    else
                    {
                        m_failed = TEST_FAILED;
                    }
                }
            } // END recordingChanged()
        }

        public class SetRecDelayWithinRangeAfterMonConfSignal extends TestCase
        {
            private OcapLocator m_locator;

            SetRecDelayWithinRangeAfterMonConfSignal(OcapLocator locator)
            {
                m_locator = locator;
            }

            public Runnable getTest()
            {
                return this;
            }

            public String getName()
            {
                return "SetRecDelayWithinRangeAfterMonConfSignal";
                // return
                // "setRecordingDelay() after monitorConfiguredSignal() call";
            }

            public void runTest()
            {
                String longRecordingRWC = "ECR856_longRecording_rwc";

                m_failed = TEST_PASSED;

                boolean recordingFound = discoverRecording(longRecordingRWC, longRecordingRWC, longRecordingRWC);

                if (recordingFound)
                {
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(longRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 10));

                    OcapSystem.monitorConfiguredSignal();
                    // Set recording delay for 10 minutes
                    m_recordingManager.setRecordingDelay(600L);

                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(longRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 1000));

                    // delete recording before shutting down
                    m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(longRecordingRWC, 10000));

                    m_eventScheduler.run(50);
                }

                if (m_failed != TEST_FAILED)
                {
                    // This could become the passed scenario
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": PASSED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else if (!recordingFound)
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + " - No Recording Found");
                    DvrTestMonAppXlet.log(getName() + " - Need To Run First Part Of Test");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": FAILED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
            }
        }

        public class SetRecDelayWithinRangeAfterMonConfSignalTimeOut extends TestCase
        {
            private OcapLocator m_locator;

            SetRecDelayWithinRangeAfterMonConfSignalTimeOut(OcapLocator locator)
            {
                m_locator = locator;
            }

            public Runnable getTest()
            {
                return this;
            }

            public String getName()
            {
                return "SetRecDelayWithinRangeAfterMonConfSignalTimeOut";
                // return
                // "setRecordingDelay() after monitorConfiguredSignal() times out";
            }

            public void runTest()
            {
                String longRecordingRWC = "ECR856_longRecording_rwc";

                m_failed = TEST_PASSED;

                boolean recordingFound = discoverRecording(longRecordingRWC, longRecordingRWC, longRecordingRWC);

                if (recordingFound)
                {
                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(longRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE, 10));

                    try
                    {
                        // Wait for monitorConfiguredSignal to timeout (5
                        // seconds)
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    // Set recording delay for 10 minutes
                    m_recordingManager.setRecordingDelay(600L);

                    m_eventScheduler.scheduleCommand(new ConfirmRecordingReq_CheckState(longRecordingRWC,
                            OcapRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, 7500));

                    // delete recording before shutting down
                    m_eventScheduler.scheduleCommand(new DeleteRecordingRequest(longRecordingRWC, 10000));

                    m_eventScheduler.run(50);
                }

                if (m_failed != TEST_FAILED)
                {
                    // This could become the passed scenario
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": PASSED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else if (!recordingFound)
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + " - No Recording Found");
                    DvrTestMonAppXlet.log(getName() + " - Need To Run First Part Of Test");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
                else
                {
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                    DvrTestMonAppXlet.log(getName() + ": FAILED!");
                    DvrTestMonAppXlet.log("---------------------------------------------------------------");
                }
            }
        }

        public boolean discoverRecording(String recName, String key, String data)
        {
            System.out.println("<<<<DvrMonAppTest: Discover Recording");
            RecordingList recList = m_recordingManager.getEntries();

            if (recList == null)
            {
                DvrTestMonAppXlet.log("<<<<DvrMonAppTest: RecordingList = null!");
                m_failed = TEST_FAILED;
                return false;
            }
            else if (recList.size() <= 0)
            {
                DvrTestMonAppXlet.log("<<<<DvrMonAppTest: RecordingList.size() <= 0!");
                m_failed = TEST_FAILED;
                return false;
            }
            else
            {
                for (int i = 0; i < recList.size(); i++)
                {

                    try
                    {
                        OcapRecordingRequest recReq = (OcapRecordingRequest) recList.getRecordingRequest(i);
                        String info = (String) recReq.getAppData(key);
                        RecordingSpec recSpec = recReq.getRecordingSpec();

                        if ((info != null) && (info.equals(data)))
                        {
                            DvrTestMonAppXlet.log("<<<< REQUEST FOUND! giving request the name : " + recName + " >>>>");
                            insertObject(recReq, recName);

                            if (recSpec instanceof LocatorRecordingSpec)
                            {
                                LocatorRecordingSpec lrs = (LocatorRecordingSpec) recSpec;
                                DvrTestMonAppXlet.log("rr_start_time(ms):  " + lrs.getStartTime());
                                DvrTestMonAppXlet.log("rr_duration  :  " + lrs.getDuration());
                            }

                            if (recSpec instanceof ServiceContextRecordingSpec)
                            {
                                ServiceContextRecordingSpec scrs = (ServiceContextRecordingSpec) recSpec;
                                DvrTestMonAppXlet.log("rr_start_time:" + scrs.getStartTime());
                                DvrTestMonAppXlet.log("rr_duration  :" + scrs.getDuration());
                            }
                        }
                        else
                        {
                            System.out.println("<<<<< ENTRY " + i + " not it!>>>>>");
                        }
                    }
                    catch (Exception e)
                    {
                        System.out.println("<<<<DvrMonAppTest: Exception finding recording request"
                                + recList.getRecordingRequest(i));
                        m_failed = TEST_FAILED;
                        m_failedReason = "DvrMonAppTest: Exception finding recording request"
                                + recList.getRecordingRequest(i) + ": " + e.getMessage();
                        e.printStackTrace();
                        return false;
                    }

                }
            }
            return true;
        }

    }

}
