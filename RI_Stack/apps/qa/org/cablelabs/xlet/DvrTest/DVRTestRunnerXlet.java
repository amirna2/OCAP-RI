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

import javax.tv.xlet.*;

import java.util.*;
import java.awt.event.*;

import org.havi.ui.*;
import org.havi.ui.event.HRcEvent;
import org.ocap.net.OcapLocator;
import org.ocap.ui.event.OCRcEvent;
import org.dvb.ui.DVBColor;
import javax.tv.util.*;
import java.io.*;
import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;

import org.cablelabs.test.autoxlet.*;

/**
 * Xlet to test basic scheduling of future recordings
 * 
 */
public class DVRTestRunnerXlet implements Xlet, KeyListener, Driveable
{

    /**
     * implementation of Xlet interface
     */
    public void initXlet(XletContext ctx)
    {
        // initialize AutoXlet

        m_axc = new AutoXletClient(this, ctx);
        m_test = m_axc.getTest();
        if (m_axc.isConnected())
        {
            m_log = m_axc.getLogger();
        }
        else
        {
            m_log = new XletLogger();
        }

        m_log.log("DVRTestRunnerXlet.initXlet()");

        // store off our xlet context
        m_xctx = ctx;
        /*
         * Grab valid service locators and storage device from Xlet params
         */
        m_serviceLocators = retrieveConfigParams(ctx);

        /*
         * Establish self as RC key listener
         */
        System.out.println("Setting up key listener and havi interface");
        scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_vbox = new VidTextBox(50, 50, 530, 370, 14, 5000);
        m_vbox.setBackground(new DVBColor(128, 128, 128, 155));
        m_vbox.setForeground(new DVBColor(200, 200, 200, 255));

        scene.add(m_vbox);
        scene.addKeyListener(this);
        scene.addKeyListener(m_vbox);

        m_timer = TVTimer.getTimer();

        // find out if we are testing the DVR SI features in addition to the
        // normal tests.
        isTestingDVRSI = isTestingDVRSI(ctx);

        System.out.println("Setup test case list");
        m_testList = new Vector();

        m_loopingTests = new Vector();

        // setup a monitor that will be used by the event dispatcher
        // to synchronize testcase launch events
        m_eventMonitor = new Monitor();
    }

    /**
     * implementation of Xlet interface
     */
    public void startXlet()
    {
        m_log.log("DVRTestRunnerXlet.startXlet()");
        /*
         * Request UI keys
         */
        System.out.println("DVRTestRunnerXlet:startXlet()");
        scene.show();
        scene.requestFocus();
        stopTest = false;
        log("Make a group selection");
        printTestGroups();
    }

    /**
     * implementation of Xlet interface
     */
    public void pauseXlet()
    {
        scene.setVisible(false);
        stopTest = true;
    }

    /**
     * implementation of Xlet interface
     */
    public void destroyXlet(boolean x)
    {
        System.out.println("DvrTestRunnerXlet: destroyXlet");
        // hide scene
        scene.setVisible(false);
        // dispose of self
        HScene tmp = scene;
        scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    /**
     * keyTyped implementation of KeyListener interface nop implentation
     * 
     * @param e
     */
    public void keyTyped(KeyEvent e)
    {
    }

    /**
     * keyPressed implementation of KeyListener interface all of the key related
     * processing happens here.
     * 
     * @param e
     */
    public void keyPressed(java.awt.event.KeyEvent e)
    {
        int key = e.getKeyCode();

        if (guiState == 0)
        {
            switch (key)
            {
                case HRcEvent.VK_0:
                case HRcEvent.VK_1:
                case HRcEvent.VK_2:
                case HRcEvent.VK_3:
                case HRcEvent.VK_4:
                case HRcEvent.VK_5:
                case HRcEvent.VK_6:
                case HRcEvent.VK_7:
                case HRcEvent.VK_8:
                case HRcEvent.VK_9:

                    m_testSelection *= 10;
                    m_testSelection += key - HRcEvent.VK_0;
                    if (m_testSelection != 0)
                        log("Test Group:" + m_testSelection);
                    else
                        log("Test Group: -- ");

                    break;

                case HRcEvent.VK_ENTER:

                    if (m_testSelection != 0)
                    {
                        getTestSuite(m_testSelection - 1);
                        printTestList();
                        guiState = 3;
                    }
                    else
                    {
                        log("Test Group: -- ");
                    }
                    m_testSelection = 0;
                    break;

                case HRcEvent.VK_LEFT:
                    if (m_testSelection < 10)
                        m_testSelection = 0;
                    else
                        m_testSelection = (m_testSelection - (m_testSelection % 10)) / 10;

                    if (m_testSelection != 0)
                        log("Test Group:" + m_testSelection);
                    else
                        log("Test Group: -- ");
                    break;

                case HRcEvent.VK_INFO:
                    if (m_testSelection == 0)
                    {
                        printTestGroups();
                    }
                    break;

                case OCRcEvent.VK_COLORED_KEY_2:
                    clearLoopingList();
                    break;
                case OCRcEvent.VK_COLORED_KEY_0:
                    runLoopingTests();
                    break;
                case OCRcEvent.VK_LIST:
                    printLoopingTests();
                    break;
            }
        }
        else if (guiState == 1 || guiState == 2)
        {
            switch (key)
            {
                case OCRcEvent.VK_COLORED_KEY_3:
                    if (guiState == 1)
                    {
                        this.launchTest(m_testSelection);
                        m_testSelection = 0;
                        guiState = 3;
                    }
                    else if (guiState == 2)
                    {
                        m_loopingTests.addElement(m_testList.elementAt(m_testSelection - 1));
                        log("Adding test:" + ((DvrTest.TestCase) m_testList.elementAt(m_testSelection - 1)).getName());
                        m_testSelection = 0;
                        guiState = 3;
                    }
                    break;
                case OCRcEvent.VK_COLORED_KEY_2:
                    log("Selection: -- ");
                    m_testSelection = 0;
                    guiState = 3;
                    break;
            }
        }
        else if (guiState == 3)
        {
            switch (key)
            {
                case HRcEvent.VK_0:
                case HRcEvent.VK_1:
                case HRcEvent.VK_2:
                case HRcEvent.VK_3:
                case HRcEvent.VK_4:
                case HRcEvent.VK_5:
                case HRcEvent.VK_6:
                case HRcEvent.VK_7:
                case HRcEvent.VK_8:
                case HRcEvent.VK_9:

                    m_testSelection *= 10;
                    m_testSelection += key - HRcEvent.VK_0;
                    if (m_testSelection != 0)
                        log("Selection:" + m_testSelection);
                    else
                        log("Selection: -- ");

                    break;

                case HRcEvent.VK_ENTER:
                    if (m_testSelection != 0)
                    {
                        log("Test selection is:" + m_testSelection);
                        m_log.log("Test selection is:" + m_testSelection);
                        if (m_testSelection != 1)
                        {
                            this.launchTest(m_testSelection);
                            m_testSelection = 0;
                        }
                        else
                        {
                            log("Do you wish to run "
                                    + ((DvrTest.TestCase) m_testList.elementAt(m_testSelection - 1)).getName() + " ?");
                            log("Press A for YES, B for NO");
                            guiState = 1;
                        }
                    }
                    else
                    {
                        log("Selection: -- ");
                        m_testSelection = 0;
                    }
                    break;

                case HRcEvent.VK_LEFT:
                    if (m_testSelection < 10)
                        m_testSelection = 0;
                    else
                        m_testSelection = (m_testSelection - (m_testSelection % 10)) / 10;

                    if (m_testSelection != 0)
                        log("Selection:" + m_testSelection);
                    else
                        log("Selection: -- ");
                    break;

                case HRcEvent.VK_INFO:
                    if (m_testSelection == 0)
                    {
                        printTestList();
                    }
                    break;

                case HRcEvent.VK_GUIDE:
                {
                    printTestGroups();
                    guiState = 0;
                }

                case OCRcEvent.VK_COLORED_KEY_3:
                    if (m_testSelection > 0 && m_testSelection <= m_testList.size())
                    {
                        if (m_testSelection != 1)
                        {
                            m_loopingTests.addElement(m_testList.elementAt(m_testSelection - 1));
                            log("Adding test:"
                                    + ((DvrTest.TestCase) m_testList.elementAt(m_testSelection - 1)).getName());
                            m_testSelection = 0;
                            if (m_testSelection != 0)
                                log("Selection:" + m_testSelection);
                            else
                                log("Selection: -- ");
                        }
                        else
                        {
                            log("Do you wish to schedule in "
                                    + ((DvrTest.TestCase) m_testList.elementAt(m_testSelection - 1)).getName() + " ?");
                            log("Press A for YES, B for NO");
                            guiState = 2;
                        }
                    }
                    break;
                case OCRcEvent.VK_COLORED_KEY_2:
                    clearLoopingList();
                    break;
                case OCRcEvent.VK_COLORED_KEY_0:
                    runLoopingTests();
                    break;
                case OCRcEvent.VK_LIST:
                    printLoopingTests();
                    break;
            }
        }
    }

    /**
     * Key Released, update and display banner nop implementations
     * 
     * @param e
     */
    public void keyReleased(KeyEvent e)
    {
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

    public static XletContext getContext()
    {
        return m_xctx;
    }

    public static void notifyTestComplete(String name, int result, String reason)
    {
        String testResult = "PASSED";
        if (result != 0)
        {
            testResult = "FAILED: " + reason;
        }
        m_log.log("Test <" + name + "> completed; result=" + testResult);
        m_test.assertTrue("Test <" + name + "> failed:" + reason, result == 0);
        m_eventMonitor.notifyReady();
    }

    /**
     * Read DVR source ids from the config file, and build an array of OCAP
     * Locators which can be used for tuning and scheduling.
     * 
     * @param ctx
     * @return
     */
    public synchronized Vector retrieveConfigParams(XletContext ctx)
    {
        Vector locators = new Vector();
        FileInputStream fis_count = null;
        FileInputStream fis_read = null;

        try
        {
            // Get path name of config file.
            ArgParser xlet_args = new ArgParser((String[]) ctx.getXletProperty(XletContext.ARGS));
            String str_config_file_name = xlet_args.getStringArg(CONFIG_FILE);

            fis_count = new FileInputStream(str_config_file_name);
            byte[] buffer = new byte[fis_count.available() + 1];
            fis_count.read(buffer);
            String str_config_file = new String(buffer);
            fis_read = new FileInputStream(str_config_file_name);
            ArgParser config_args = new ArgParser(fis_read);

            String idName = null;
            String str = config_args.getStringArg(DVR_IF_FPQ);
            if (str.compareTo("TRUE") == 0)
            {
                idName = DVR_FPQ;
                System.out.println("Using Frequency, Progm QAM");
            }
            else
            {
                idName = DVR_SOURCE_ID;
                System.out.println("Using Source Id");
            }
            // Count DVR source ids in config file.
            int chan_count = -1;
            while (str_config_file.indexOf(idName + ++chan_count) != -1);
            fis_count.close();

            // Read DVR channel from config file.
            if (idName.compareTo(DVR_SOURCE_ID) == 0)
            {
                for (int i = 0; i < chan_count; i++)
                {
                    System.out.println("Using Source Id");
                    String str_source_id = config_args.getStringArg(DVR_SOURCE_ID + i);
                    locators.addElement((Object) (new OcapLocator("ocap://" + str_source_id)));
                    System.out.println("DVR Tests locator: " + locators.elementAt(i));
                }
            }
            else
            {
                for (int i = 0; i < chan_count; i++)
                {
                    int frequency = 0;
                    int programNum = 0;
                    int qam = 0;
                    System.out.println("Using Frequency, Progm QAM");
                    String str_fpq = config_args.getStringArg(DVR_FPQ + i);
                    StringTokenizer st = new StringTokenizer(str_fpq, ",");
                    String elem = st.nextToken();
                    System.out.println("Using freq " + elem);
                    frequency = Integer.parseInt(elem);
                    elem = st.nextToken();
                    programNum = Integer.parseInt(elem);
                    elem = st.nextToken();
                    qam = Integer.parseInt(elem);

                    System.out.println(" Channel- freq: " + frequency + " qam :" + qam + " pid :" + programNum);
                    locators.addElement((Object) new OcapLocator(frequency, programNum, qam));
                    System.out.println("DVR Tests locator: " + locators.elementAt(i));
                }
            }
            // get the Platform DB min and max (for the GainControl)
            try
            {
                m_minDB = Float.parseFloat(config_args.getStringArg("DVR_MIN_DB"));
            }
            catch (Exception e)
            {
                System.out.println("no min DB defined, use default -24.0");
            }
            try
            {
                m_maxDB = Float.parseFloat(config_args.getStringArg("DVR_MAX_DB"));
            }
            catch (Exception e)
            {
                System.out.println("no max DB defined, use default 4.0");
            }
            // Get the list of storage names from the config file
            String str_devices = null;
            try 
            {
                str_devices = config_args.getStringArg(DVR_DEVICES);
            }
            catch (Exception e)
            {
                System.out.println("no DVR_DEVICES defined");
            }
            if (str_devices != null)
            {
                StringTokenizer st = new StringTokenizer(str_devices, ",");
                m_devices = new Vector();
                while (true)
                {
                    try
                    {
                        String device = st.nextToken();
                        System.out.println("Adding device to list of devices: " + device);
                        m_devices.add(device);
                    }
                    catch (NoSuchElementException e)
                    {
                        System.out.println("No more devices to add");
                        break;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        break;
                    }
                }
            }
            fis_read.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return locators;
    }

    /**
     * Given an xlet context, parses through the arguments looking for the
     * argument "testDVRSI". If this argument exists then the tests will run
     * testing the new JavaTV implementation backed DVRSI in addition to their
     * normal functionality.
     * 
     * @param ctx
     * @return
     */
    public synchronized boolean isTestingDVRSI(XletContext ctx)
    {
        boolean ret = false;
        String[] xletArgs = (String[]) ctx.getXletProperty(XletContext.ARGS);
        if (xletArgs == null)
        {
            return false;
        }
        String arg;
        for (int x = 0; x < xletArgs.length; x++)
        {
            arg = xletArgs[x];
            if (arg.equals("testDVRSI"))
            {
                return true;
            }
        }
        return ret;
    }

    public void getTestSuite(int suite)
    {
        if (m_testList.size() != 0)
        {
            m_testList.removeAllElements();
        }
        addTests(new TestDeleteAllRecordings(m_serviceLocators).getTests());
        switch (suite)
        {
            case 0:
                addTests(new TestBasicRecordingAndPlayback(scene, m_serviceLocators).getTests());
                addTests(new TestConsecutiveRecordings(m_serviceLocators).getTests());
                addTests(new TestImmediateRecording(m_serviceLocators, isTestingDVRSI).getTests());
                addTests(new TestOverlappingEntries(m_serviceLocators).getTests());
                addTests(new TestOverlappingRecordings(m_serviceLocators).getTests());
                addTests(new TestRecordAndCancel(m_serviceLocators).getTests());
                addTests(new TestRecordingAlertListener(m_serviceLocators).getTests());
                addTests(new TestRecordingPlaybackListener(m_serviceLocators).getTests());
                addTests(new TestRecordingID_ECN829(m_serviceLocators).getTests());
                addTests(new TestRecMgrGetEntries(m_serviceLocators).getTests());
                addTests(new TestSortRecordingList(m_serviceLocators).getTests());
                addTests(new TestInsufficientStateChange(m_serviceLocators).getTests());
                addTests(new TestScheduledRecordingContention(m_serviceLocators).getTests());
                addTests(new TestScheduledRecordingContention1(m_serviceLocators).getTests());
                addTests(new TestAppData(m_serviceLocators).getTests());
                addTests(new TestSetParent(m_serviceLocators).getTests());

                addTests(new TestSeriesRecordingExpiration(m_serviceLocators).getTests());
                addTests(new TestSeriesDeleteRecording(m_serviceLocators).getTests());
                addTests(new TestSeriesReschedule(m_serviceLocators).getTests());
                addTests(new TestSeriesContention(m_serviceLocators).getTests());
                addTests(new TestTunerConflict_Gating(m_serviceLocators).getTests());
                addTests(new TestGainCtrl(m_serviceLocators, m_minDB, m_maxDB).getTests());
                addTests(new TestStorageLimitedDvr(m_serviceLocators).getTests());
                addTests(new TestRetentionContention(m_serviceLocators).getTests());
                addTests(new TestRetentionPriorityOfRecordings(m_serviceLocators).getTests());
                addTests(new TestRescheduleExpiration(m_serviceLocators).getTests());
                addTests(new TestDelayedScheduleStart(scene, m_serviceLocators).getTests());
                addTests(new TestRecordingResourceLost(m_serviceLocators).getTests());
                addTests(new TestAddBeforeStartListener(m_serviceLocators).getTests());
                addTests(new TestPropertiesChangesOnCompletedRecording(m_serviceLocators).getTests());
                addTests(new TestRecordwithConflicts(m_serviceLocators).getTests());
                addTests(new TestGetPrioritizationList(m_serviceLocators).getTests());
                addTests(new TestSetPrioritizationList(m_serviceLocators).getTests());
                addTests(new TestStopPersistentRecording(m_serviceLocators).getTests());
                addTests(new TestFailedRecordingContention(m_serviceLocators).getTests());
                addTests(new TestReschedule(m_serviceLocators).getTests());
                addTests(new TestResContentionWarning(m_serviceLocators).getTests());
                addTests(new TestResContentionWarning2(m_serviceLocators).getTests());
                break;
            case 1:
                addTests(new TestSharedResources1(m_serviceLocators).getTests());
                addTests(new TestSharedResources2(m_serviceLocators).getTests());
                addTests(new TestSharedResources3(m_serviceLocators).getTests());
                addTests(new TestSharedResources5(m_serviceLocators).getTests());
                addTests(new TestSharedResources6(m_serviceLocators).getTests());
                addTests(new TestSharedResources9(scene, m_serviceLocators).getTests());
                addTests(new TestWS70_ServiceContext_1(m_serviceLocators).getTests());
                addTests(new TestWS70_ServiceContext_2(m_serviceLocators).getTests());
                addTests(new TestWS70_ServiceContext_3(m_serviceLocators).getTests());
                addTests(new TestWS70_ServiceContext_4(m_serviceLocators).getTests());
                addTests(new TestClassBufferingRequestAPI(m_serviceLocators).getTests());
                addTests(new TestClassResourceContention(m_serviceLocators).getTests());
                addTests(new TestClassNetworkInterface(m_serviceLocators).getTests());
                addTests(new SegmentedRec_RecordWithConflict(m_serviceLocators).getTests());
                addTests(new SegmentedRec_RecordIfNoConflict(m_serviceLocators).getTests());
                addTests(new TestSegmentedRecordingPlayback(m_serviceLocators).getTests());
                addTests(new TestDeletedSegementedRecordings(m_serviceLocators).getTests());
                addTests(new TestForDeadLock_bug4524(m_serviceLocators).getTests());
                addTests(new TestTSPsetPresentationTime(m_serviceLocators).getTests());
                addTests(new TestTSPsetPresentationAction(m_serviceLocators).getTests());
                addTests(new TestTSPsetPresentationPersistent(m_serviceLocators).getTests());
                addTests(new TestDisabledStorage(m_serviceLocators).getTests());
                addTests(new TestPlaybackOfOngoingRecordedService(m_serviceLocators).getTests());
                break;
            case 2:
                addTests(new TestBasicRecordingAndPlayback(scene, m_serviceLocators).getTests());
                addTests(new TestConsecutiveRecordings(m_serviceLocators).getTests());
                addTests(new TestSetParent(m_serviceLocators).getTests());
                addTests(new TestAppData(m_serviceLocators).getTests());
                break;
            case 3:
                addTests(new TestScheduledRecordingContention(m_serviceLocators).getTests());
                addTests(new TestScheduledRecordingContention1(m_serviceLocators).getTests());
                break;
            case 4:
                addTests(new TestImmediateRecording(m_serviceLocators, isTestingDVRSI).getTests());
                break;
            case 5:
                addTests(new TSBInterruption1(m_serviceLocators).getTests());
                addTests(new TSBInterruption2(m_serviceLocators).getTests());
                break;
            case 6:
                addTests(new TestPersistentRecordings(m_serviceLocators).getTests());
                addTests(new TestPersistentRecordingsCheck(m_serviceLocators).getTests());
                addTests(new TestDelayedScheduleStart(scene, m_serviceLocators).getTests());
                addTests(new TestPropertiesChangesOnCompletedRecording(m_serviceLocators).getTests());
                addTests(new TestStopPersistentRecording(m_serviceLocators).getTests());
                addTests(new TestDiskFull(m_serviceLocators).getTests());
                addTests(new TestLowPowerResumeRecording(m_serviceLocators).getTests());
                break;
            case 7:
                addTests(new TestBasicRecordingAndPlayback(scene, m_serviceLocators).getTests());
                addTests(new TestImmediateRecording(m_serviceLocators, isTestingDVRSI).getTests());
                addTests(new TestTunerConflict_Gating(m_serviceLocators).getTests());
                addTests(new TestRecMgrGetEntries(m_serviceLocators).getTests());
                addTests(new TestSortRecordingList(m_serviceLocators).getTests());
                addTests(new TestInsufficientStateChange(m_serviceLocators).getTests());
            case 8:
                addTests(new TestResContentionWarning(m_serviceLocators).getTests());
                addTests(new TestResContentionWarning2(m_serviceLocators).getTests());
                break;
            case 9:
                addTests(new TestSharedResources1(m_serviceLocators).getTests());
                addTests(new TestSharedResources2(m_serviceLocators).getTests());
                addTests(new TestSharedResources3(m_serviceLocators).getTests());
                addTests(new TestSharedResources5(m_serviceLocators).getTests());
                addTests(new TestSharedResources6(m_serviceLocators).getTests());
                addTests(new TestSharedResources9(scene, m_serviceLocators).getTests());
                addTests(new TestWS70_ServiceContext_1(m_serviceLocators).getTests());
                addTests(new TestWS70_ServiceContext_2(m_serviceLocators).getTests());
                addTests(new TestWS70_ServiceContext_3(m_serviceLocators).getTests());
                addTests(new TestWS70_ServiceContext_4(m_serviceLocators).getTests());
                addTests(new TestClassBufferingRequestAPI(m_serviceLocators).getTests());
                addTests(new TestClassResourceContention(m_serviceLocators).getTests());
                addTests(new TestOverlappingRecordings(m_serviceLocators).getTests());
                addTests(new TestClassNetworkInterface(m_serviceLocators).getTests());
                addTests(new TestForDeadLock_bug4524(m_serviceLocators).getTests());
                addTests(new TestTSPsetPresentationTime(m_serviceLocators).getTests());
                addTests(new TestTSPsetPresentationAction(m_serviceLocators).getTests());
                addTests(new TestTSPsetPresentationPersistent(m_serviceLocators).getTests());
                break;
            case 10:
                addTests(new SegmentedRec_RecordWithConflict(m_serviceLocators).getTests());
                addTests(new TestRecordingResourceLost(m_serviceLocators).getTests());
                addTests(new TestRecordwithConflicts(m_serviceLocators).getTests());
                addTests(new SegmentedRec_RecordIfNoConflict(m_serviceLocators).getTests());
                addTests(new TestSegmentedRecordingPlayback(m_serviceLocators).getTests());
                addTests(new TestDeletedSegementedRecordings(m_serviceLocators).getTests());
                addTests(new TestPersistentRecordings(m_serviceLocators).getTests());
                addTests(new TestPersistentRecordingsCheck(m_serviceLocators).getTests());
                addTests(new TestDiskFull(m_serviceLocators).getTests());
                addTests(new TestPlaybackOfOngoingRecordedService(m_serviceLocators).getTests());
                break;
            case 11:
                addTests(new TestDisabledStorage(m_serviceLocators).getTests());
                break;
            case 12:
                addTests(new TestECR856_createRecordings(m_serviceLocators).getTests());
                break;
            case 13:
                addTests(new TestMediaTimeTags(scene, m_serviceLocators).getTests());
                break;
            case 14:
                addTests(new TestDetachableStorageDevice(m_devices, m_serviceLocators).getTests());
                break;
            case 15:
                addTests(new TestECN1321Scenarios(m_serviceLocators).getTests());
                break;
            case 16:
                addTests(new TestDetachedDeviceOverReboot(m_devices, m_serviceLocators).getTests());
                break;
            case 17:
                addTests(new TestECN1017DVRPlaybackControls(scene, m_serviceLocators).getTests());
                break;
            case 18:
                addTests(new TestECN1049ResourcePriority(m_serviceLocators).getTests());
                break;
            case 19:
                addTests(new TestMediaSelectControl(m_serviceLocators).getTests());
                break;
            default:
                break;
        }
    }

    protected void addTests(Vector tests)
    {
        for (int i = 0; i < tests.size(); i++)
        {
            m_testList.addElement(tests.elementAt(i));
        }
    }

    public void printTestGroups()
    {
        for (int i = 0; i < m_testGroups.length; i++)
        {
            log("Group " + ((int) (i + 1)) + ": " + m_testGroups[i]);
        }
    }

    void printTestList()
    {
        for (int i = 0; i < m_testList.size(); i++)
        {
            DvrTest.TestCase test = (DvrTest.TestCase) m_testList.elementAt(i);
            log("Test " + ((int) (i + 1)) + ": " + test.getName());
        }
    }

    void printLoopingTests()
    {
        log("Tests in the test list:");
        for (int i = 0; i < m_loopingTests.size(); i++)
        {
            DvrTest.TestCase test = (DvrTest.TestCase) m_loopingTests.elementAt(i);
            log("Test " + ((int) (i + 1)) + ": " + test.getName());
        }
    }

    void clearLoopingList()
    {
        m_loopingTests.removeAllElements();
        log("Test list cleared");
    }

    void runLoopingTests()
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                int count = 0;
                while (!stopTest)
                {
                    count++;
                    for (int i = 0; i < m_loopingTests.size(); i++)
                    {
                        DvrTest.TestCase test = (DvrTest.TestCase) m_loopingTests.elementAt(i);
                        log("Running test <" + test.getName() + ">");
                        Thread tcrt = new Thread(test);
                        tcrt.start();
                        test.waitForCompletion();
                        try
                        {
                            test.wait(3000);
                        }
                        catch (Exception e)
                        {
                            System.out.println("Test thread finished, moving on");
                        }

                        if (stopTest)
                        {
                            break;
                        }
                    }
                    log("runLoopingTests: completed " + count + " passes.");
                }
            }
        };
        Thread t = new Thread(r);
        t.start();
    }

    void launchTest(int testIndex)
    {
        if (testIndex > m_testList.size() || testIndex <= 0)
        {
            log("Unable to find test #" + (testIndex) + ".");
            m_log.log("Unable to find test #" + (testIndex) + ".");
        }

        else
        {
            DvrTest.TestCase test = (DvrTest.TestCase) m_testList.elementAt(testIndex - 1);
            if (test == null)
            {
                log("Unable to find test #" + (testIndex) + ".");
                m_log.log("Unable to find test #" + (testIndex) + ".");
            }
            else
            {
                log("Running test <" + test.getName());
                m_log.log("Running test <" + test.getName());

                Thread tcrt = new Thread(test);
                tcrt.start();
            }
        }
    }

    /*
     * For autoXlet automation framework
     */
    public void dispatchEvent(KeyEvent e, boolean useMonitor, int timeout)
    {
        if (useMonitor)
        {
            m_eventMonitor.setTimeout(timeout);
            synchronized (m_eventMonitor)
            {
                keyPressed(e);

                int key = e.getKeyCode();
                if (key == HRcEvent.VK_ENTER || key == OCRcEvent.VK_COLORED_KEY_3)
                {
                    m_eventMonitor.waitForReady();
                }
            }
        }
        else
        {
            keyPressed(e);
        }
    }

    protected float m_minDB = -24.0F;
    protected float m_maxDB = 4.0F;

    /**
     * Contains a list of locators which can be used for scheduling/recording
     */
    protected Vector m_serviceLocators = null;

    protected Vector m_devices = null;

    /**
     * Private HAVi scene for receiving UI events
     */
    protected HScene scene = null;

    /**
     * Our xlet context
     */
    protected static XletContext m_xctx = null;

    protected TVTimer m_timer;

    protected Vector m_testList;

    protected int m_testSelection;

    protected static VidTextBox m_vbox;

    protected Vector m_loopingTests;

    protected static final String CONFIG_FILE = "config_file";

    protected static final String DVR_SOURCE_ID = "DVR_sourceId_";

    protected static final String DVR_FPQ = "DVR_FPQ_";

    protected static final String DVR_IF_FPQ = "DVR_by_FPQ";

    protected static final String DVR_DEVICES = "DVR_devices";

    protected boolean isTestingDVRSI = false;

    protected int guiState = 0;

    protected boolean stopTest = true;

    protected String[] m_testGroups = {"Group 1 Automation",  
                                       "Group 2 Automation", 
                                       "Basic Tests",        
                                       "Contention Tests",   
                                       "TSB Tests",          
                                       "TSB Interruption Tests", 
                                       "Manual Tests", 
                                       "Old Gating Tests",
                                       "Resource Contention Warning Tests", 
                                       "ITSB Tests", 
                                       "Segmented Recordings Tests", 
                                       "Disable Storage Tests",
                                       "ECR856 Tests", 
                                       "ECN911 - MediaTimeTags Tests", 
                                       "Storage Resource Tests", 
                                       "ECN 1321 Tests",
                                       "Test Detached Device Over Reboot", 
                                       "ECN 1017 Control Tests", 
                                       "ECN 1049 Resource Priority Tests",
            "MediaSelectControl Tests" };

    // autoXlet
    protected AutoXletClient m_axc = null;

    protected static Logger m_log = null;

    protected static Test m_test = null;

    protected static Monitor m_eventMonitor = null;
}
