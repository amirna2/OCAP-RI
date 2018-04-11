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
 * Cable Television Laboratories, Inc. makes available all content in this template
 * ("Content"). Unless otherwise indicated below, the Content is provided
 * to you under the terms and conditions of the Common Public License
 * Version 1.0 ("CPL").
 *
 * A copy of the CPL is available at http://www.eclipse.org/legal/cpl-v10.html.
 * For purposes of the CPL, "Program" will mean the Content.
 */

// Declare package.
package org.cablelabs.xlet.DvrTest.ECN1098;

import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import java.util.*;
import java.awt.event.*;

import org.havi.ui.*;
import org.havi.ui.event.HRcEvent;
import org.ocap.dvr.OcapRecordedService;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.net.OcapLocator;
import org.davic.net.InvalidLocatorException;
import org.dvb.ui.DVBColor;

import java.io.*;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.Recorder;
import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Monitor;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;

/**
 * The class presents a simple Xlet example for writing an OCAP application.
 * 
 * @author Vidiom Systems, Inc.
 */
public class MediaStorageVolumeXlet implements javax.tv.xlet.Xlet, KeyListener, Driveable
{
    private boolean m_started = false; // A flag indicating that the Xlet has
                                       // been started.

    private XletContext m_xctx = null;

    private HScene m_scene = null;

    private VidTextBox m_vbox = null;

    private Recorder m_recorder = null;

    private OcapLocator m_locator = null;

    private boolean m_isAutoXlet = true;

    AutoXletClient m_axc = null; // Autoxlet client

    Monitor m_eventMonitor = null; // Monitor for AutoXlet

    Logger m_log = null; // Logger for AutoXlet

    Test m_test = null; // JUnit like asserts, etc.

    static final String S_LOCATOR = "RecFPQ";

    static final String S_ORG1 = "ORG1";

    static final String S_REC_NAME = "REC_NAME";

    static final String S_MULTIPLE_REC_NUM = "MULTIPLE_REC_NUM";

    static final String S_BEGINNING_REC_NUM = "BEGINNING_REC_NUM";

    static final String S_DEFAULT_WAIT_FOR_EXPIRATION = "DEFAULT_WAIT_FOR_EXPIRATION";

    static final String S_SHORT_EXPIRATION_LENGTH = "SHORT_EXPIRATION_LENGTH";

    static final String S_DEFAULT_REC_LENGTH = "DEFAULT_REC_LENGTH";

    static final String S_DEFAULT_EXPIRATION_LENGTH = "DEFAULT_EXPIRATION_LENGTH";

    static final String S_CONFIG_FILE = "config_file";

    static String ORG1 = "00000001";

    static String REC_NAME = "MSV";

    static int MULTIPLE_REC_NUM = 5;

    static int BEGINING_REC_NUM = 1;

    static int DEFAULT_WAIT_FOR_EXPIRATION = 40000;

    static int SHORT_EXPIRATION_LENGTH = 20000;

    static int DEFAULT_REC_LENGTH = 10000;

    static int DEFAULT_EXPIRATION_LENGTH = 1000 * 60 * 60 * 24;

    /**
     * The default constructor.
     */
    public MediaStorageVolumeXlet()
    {
        // Does nothing extra.
    }

    /**
     * Initializes the OCAP Xlet.
     * <p>
     * A reference to the context is stored for further need. This is the place
     * where any initialization should be done, unless it takes a lot of time or
     * resources.
     * </p>
     * 
     * @param The
     *            context for this Xlet is passed in.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             initialized.
     */
    public void initXlet(javax.tv.xlet.XletContext ctx) throws javax.tv.xlet.XletStateChangeException
    {
        try
        {
            // store off our xlet context
            m_xctx = ctx;

            // autoxlet stuff
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
            m_eventMonitor = new Monitor();

            /*
             * Create a recorder object
             */
            m_recorder = new Recorder();

            /*
             * Grab valid service locators from config file
             */
            ORG1 = getConfigParameter(S_ORG1);
            REC_NAME = getConfigParameter(S_REC_NAME);

            String s_locator = getConfigParameter(S_LOCATOR);
            parseLocator(s_locator);

            String s_multipleRecNum = getConfigParameter(S_MULTIPLE_REC_NUM);
            MULTIPLE_REC_NUM = Integer.parseInt(s_multipleRecNum);

            String s_beginningRecNum = getConfigParameter(S_BEGINNING_REC_NUM);
            BEGINING_REC_NUM = Integer.parseInt(s_beginningRecNum);

            String s_defaultWaitForExpiration = getConfigParameter(S_DEFAULT_WAIT_FOR_EXPIRATION);
            DEFAULT_WAIT_FOR_EXPIRATION = Integer.parseInt(s_defaultWaitForExpiration);

            String s_shortExpirationLength = getConfigParameter(S_SHORT_EXPIRATION_LENGTH);
            SHORT_EXPIRATION_LENGTH = Integer.parseInt(s_shortExpirationLength);

            String s_defaultRecLength = getConfigParameter(S_DEFAULT_REC_LENGTH);
            DEFAULT_REC_LENGTH = Integer.parseInt(s_defaultRecLength);

            String s_defaultExpirationLength = getConfigParameter(S_DEFAULT_EXPIRATION_LENGTH);
            DEFAULT_EXPIRATION_LENGTH = Integer.parseInt(s_defaultExpirationLength);

            /*
             * Set up video graphics for the application Establish self as RC
             * key listener
             */
            System.out.println("Setting up key listener and havi interface");
            m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
            m_vbox = new VidTextBox(50, 50, 530, 370, 14, 5000);
            m_vbox.setBackground(new DVBColor(128, 128, 128, 155));
            m_vbox.setForeground(new DVBColor(200, 200, 200, 255));

            m_scene.add(m_vbox);
            m_scene.addKeyListener(this);
            m_scene.addKeyListener(m_vbox);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
        }
    }

    private void parseLocator(String s_locator) throws InvalidLocatorException
    {
        /*
         * Parse out the locator entry
         */
        StringTokenizer st = new StringTokenizer(s_locator, ",");
        String elem = st.nextToken();
        System.out.println("Using freq " + elem);
        int frequency = Integer.parseInt(elem);
        elem = st.nextToken();
        int programNum = Integer.parseInt(elem);
        elem = st.nextToken();
        int qam = Integer.parseInt(elem);

        System.out.println(" Channel- freq: " + frequency + " qam :" + qam + " pid :" + programNum);
        m_locator = new OcapLocator(frequency, programNum, qam);
        System.out.println("MediaStorageVolume Xlet locator: " + m_locator);
    }

    /**
     * Starts the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             started.
     */
    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        try
        {
            if (!m_started)
            {
                m_started = true;

            }
            /*
             * Request UI keys
             */
            System.out.println("DVRTestRunnerXlet:startXlet()");
            m_scene.show();
            m_scene.requestFocus();
            logStatus("MediaStorageVolume getFreeSpace Xlet App");

            /*
             * Display the options
             */
            displaySelections();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
        }
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        if (m_started)
        {
            // XXX - Do something here, like hiding the application.
        }
    }

    /**
     * Destroys the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             destroyed.
     */
    public void destroyXlet(boolean forced) throws javax.tv.xlet.XletStateChangeException
    {
        try
        {
            if (m_started)
            {
                m_started = false;
            }
            // Hide graphics
            m_scene.setVisible(false);
            // dispose of self
            HScene tmp = m_scene;
            m_scene = null;
            HSceneFactory.getInstance().dispose(tmp);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
        }
    }

    /*
     * Retrieve the parameters
     * 
     * @param Retrieve a parameter for the xlet from the config file
     */
    private String getConfigParameter(String param)
    {
        FileInputStream fis_count = null;
        FileInputStream fis_read = null;
        String config_param = null;

        try
        {
            // Get path of config file
            ArgParser xlet_args = new ArgParser((String[]) m_xctx.getXletProperty(XletContext.ARGS));
            String str_config_file_name = xlet_args.getStringArg(S_CONFIG_FILE);

            // Set up a new parser for parsing in the config file
            fis_count = new FileInputStream(str_config_file_name);
            byte[] buffer = new byte[fis_count.available() + 1];
            fis_count.read(buffer);
            // String str_config_file = new String(buffer);
            fis_read = new FileInputStream(str_config_file_name);
            ArgParser config_args = new ArgParser(fis_read);
            config_param = config_args.getStringArg(param);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        // Get the argument
        return config_param;
    }

    /*
     * Displays the selections for keypresses
     */
    private void displaySelections()
    {
        if (m_isAutoXlet) return; // don't display selections in autoxlet.

        m_vbox.write("1: Test one recording ");
        m_vbox.write("2: Test one recording that expires");
        m_vbox.write("3: Test one recording that is deleted");
        m_vbox.write("4: Test multiple recordings");
        m_vbox.write("5: Test multiple recordings that expire");
        m_vbox.write("6: Test multiple recordings that are deleted");
    }

    /*
     * Key press handler function
     * 
     * @param key Key Event callback representing a keypress
     */
    public void keyPressed(KeyEvent inkey)
    {
        executeTest(inkey, false);
    }

    private void executeTest(KeyEvent inkey, boolean autoXlet)
    {
        final KeyEvent key = inkey;
        m_isAutoXlet = autoXlet;

        new Thread()
        {
            public void run()
            {
                switch (key.getKeyCode())
                {
                    case HRcEvent.VK_1:
                        test_oneRecording();
                        break;
                    case HRcEvent.VK_2:
                        test_oneRecordingExpired();
                        break;
                    case HRcEvent.VK_3:
                        test_oneRecordingDeleted();
                        break;
                    case HRcEvent.VK_4:
                        test_mRecordings();
                        break;
                    case HRcEvent.VK_5:
                        test_mRecordingsExpired();
                        break;
                    case HRcEvent.VK_6:
                        test_mRecordingsDeleted();
                        break;
                    default:
                        break;
                }
            }
        }.start();
    }

    /*
	 */

    /*
	 */
    private void test_oneRecording()
    {
        String testName = "test_oneRecording";

        startTest(testName);

        String testRecName = getRecName(testName);
        logRecName(testRecName);

        TestResults btr = new TestResults();

        basicTest(testRecName, DEFAULT_REC_LENGTH, DEFAULT_EXPIRATION_LENGTH, "test_oneRecording", btr);

        if (btr.rr == null)
        {
            stopTest(testName, btr);
            return;
        }

        try
        {
            m_recorder.deleteRecording(testRecName);
        }
        catch (Exception e)
        {
            btr.resultsValue++;
            btr.errorStrings.add("delete failed: " + e.getMessage());
        }
        stopTest(testName, btr);
    }

    private void logRecName(String testRecName)
    {
        logStatus("recording '" + testRecName + "'");
    }

    private String getRecName(String testName)
    {
        String testRecName = REC_NAME + "_" + testName;
        return testRecName;
    }

    private void test_oneRecordingExpired()
    {
        String testName = "test_oneRecordingExpired";

        startTest(testName);

        String testRecName = getRecName(testName);
        logRecName(testRecName);

        TestResults btr = new TestResults();
        basicTest(testRecName, DEFAULT_REC_LENGTH, SHORT_EXPIRATION_LENGTH, testName, btr); // 15
                                                                                            // second
                                                                                            // recording
                                                                                            // that
                                                                                            // expires
                                                                                            // 30
                                                                                            // seconds
                                                                                            // after
                                                                                            // its
                                                                                            // initiation

        if (btr.rr == null)
        {
            stopTest(testName, btr);
            return;
        }

        logToScreen("\n" + testName + ": Waiting " + DEFAULT_WAIT_FOR_EXPIRATION / 1000 + " seconds for expiration");
        try
        {
            Thread.sleep(DEFAULT_WAIT_FOR_EXPIRATION); // give the recording
                                                       // time to expire (with
                                                       // slop factor)
        }
        catch (InterruptedException e)
        {
            btr.resultsValue++;
            btr.errorStrings.add("delete failed: " + e.getMessage());
        }

        // at this point the recording should be expired
        long freeSpaceAfterExpire = btr.msv.getFreeSpace();

        logStatus("" + freeSpaceAfterExpire + "  (Free space after expire)");

        long deltaAfterExpire = btr.freeSpaceAfterRec - freeSpaceAfterExpire;

        logResultsObj(testName, btr);
        logStatus(freeSpaceAfterExpire + "  (Free space after expire)");
        logStatus(deltaAfterExpire + "(free space recovered after expire)");

        if (freeSpaceAfterExpire == btr.freeSpaceAfterRec)
        {
            btr.errorStrings.add("freespace did not increase after expiration");
            btr.resultsValue++;
        }
        if (freeSpaceAfterExpire < btr.freeSpaceBeforeRec)
        {
            btr.resultsValue++;
            btr.errorStrings.add("not all space recovered after expiration");
        }
        stopTest(testName, btr);
    }

    private void test_oneRecordingDeleted()
    {
        String testName = "test_oneRecordingDeleted";

        startTest(testName);

        String testRecName = getRecName(testName);
        logRecName(testRecName);

        TestResults btr = new TestResults();
        basicTest(testRecName, DEFAULT_REC_LENGTH, DEFAULT_EXPIRATION_LENGTH, testName, btr);

        try
        {
            logToScreen("\n" + testName + ": deleting recording " + "'" + testRecName + "'");
            m_recorder.deleteRecording(testRecName);
        }
        catch (Exception e)
        {
            btr.resultsValue++;
            btr.errorStrings.add("delete failed: " + e.getMessage());
        }

        // at this point the recording should be deleted
        long freeSpaceAfterDelete = btr.msv.getFreeSpace();
        long deltaAfterDelete = btr.freeSpaceAfterRec - freeSpaceAfterDelete;

        logResultsObj(testName, btr);
        logStatus(freeSpaceAfterDelete + "  (Free space after delete)");
        logStatus(deltaAfterDelete + "(free space recovered after delete)");

        logToScreen("");

        if (freeSpaceAfterDelete == btr.freeSpaceAfterRec)
        {
            btr.resultsValue++;
            btr.errorStrings.add("FAILED:  freespace did not increase after deletion");
        }
        if (freeSpaceAfterDelete < btr.freeSpaceBeforeRec)
        {
            btr.resultsValue++;
            btr.errorStrings.add("FAILED:  not all space recovered after delete");
        }
        stopTest(testName, btr);
    }

    private class StatusThread extends Thread
    {
        public boolean stop = false;

        public void run()
        {
            int count = 0;
            while (!stop)
            {
                m_vbox.writeNoNewline("" + count);
                count++;
                if (count > 9) count = 0;

                try
                {
                    sleep(1000);
                }
                catch (InterruptedException e)
                {
                }
            }
            return;
        }

        public void stopMe()
        {
            stop = true;
        }
    };

    private StatusThread statusThread = null;

    private void startTest(String testName)
    {
        String s = "\n" + testName + "\n-----------------------------------------------------------------------------";

        logToScreen(s);
        logStatus(s);
        statusThread = new StatusThread();
        statusThread.start();
    }

    private void stopTest(String testName, TestResults btr)
    {
        boolean newLine = true; // newline before printing results out.
        logResultToScreen(testName, btr, newLine);

        if (statusThread != null)
        {
            statusThread.stopMe();
            statusThread = null;
        }
        notifyTestComplete(testName, btr);
        displaySelections();
    }

    /**
     * Tests that freespace decreases as multiple recordings are fired off
     */
    private void test_mRecordings()
    {
        String testName = "test_mRecordings";

        startTest(testName);

        TestResults btr = new TestResults();

        for (int i = BEGINING_REC_NUM; i <= MULTIPLE_REC_NUM; i++)
        {
            record(testName, i, DEFAULT_EXPIRATION_LENGTH, btr);
            logToScreen(""); // new line between tests
        }
        for (int j = BEGINING_REC_NUM; j <= MULTIPLE_REC_NUM; j++)
        {
            String recName = getMultipleRecName(testName, j);
            logStatus("test_mRecordings deleting '" + recName + "'");

            try
            {
                m_recorder.deleteRecording(recName);
            }
            catch (Exception e)
            {
                String errString = "delete of " + recName + " failed:  " + e.getMessage();
                btr.resultsValue++;
                btr.errorStrings.add(errString);
            }
        }
        stopTest(testName, btr);
    }

    private void test_mRecordingsExpired()
    {

        String testName = "test_mRecordingsExpired";

        startTest(testName);

        TestResults btr = new TestResults();
        TestResults initialResults = null;

        try
        {
            for (int i = BEGINING_REC_NUM; i <= MULTIPLE_REC_NUM; i++)
            {
                logStatus("recording " + getMultipleRecName(testName, i));
                if (i == BEGINING_REC_NUM)
                {
                    initialResults = record(testName, i, SHORT_EXPIRATION_LENGTH, btr);
                }
                else
                {
                    record(testName, i, SHORT_EXPIRATION_LENGTH, btr);
                }
                logToScreen(""); // new line between tests
            }

            logToScreen(testName + ": Waiting " + DEFAULT_WAIT_FOR_EXPIRATION / 1000 + " seconds for expiration");
            try
            {
                Thread.sleep(DEFAULT_WAIT_FOR_EXPIRATION); // give the recording
                                                           // time to expire
                                                           // (with slop factor)
            }
            catch (InterruptedException e)
            {
                btr.resultsValue++;
                btr.errorStrings.add("delete failed: " + e.getMessage());
            }

            // at this point the recording should be expired
            long freeSpaceAfterExpire = btr.msv.getFreeSpace();

            logStatus("" + freeSpaceAfterExpire + "  (Free space after expire)");

            long deltaAfterExpire = btr.freeSpaceAfterRec - freeSpaceAfterExpire;

            logResultsObj(testName, btr);
            logStatus(freeSpaceAfterExpire + "  (Free space after expire)");
            logStatus(deltaAfterExpire + "(free space recovered after expire)");

            if (freeSpaceAfterExpire == btr.freeSpaceAfterRec) // compare to
                                                               // freespace
                                                               // after last
                                                               // recording
            {
                btr.errorStrings.add("freespace did not increase after expiration");
                btr.resultsValue++;
            }
            else if (freeSpaceAfterExpire < initialResults.freeSpaceBeforeRec)
            {
                btr.resultsValue++;
                btr.errorStrings.add("not all space recovered after expiration");
            }
            else
            // passed
            {
                ;
            }
        }
        catch (Exception e)
        {
            String errStr = e.getMessage();
            try
            {
                for (int i = BEGINING_REC_NUM; i <= MULTIPLE_REC_NUM; i++)
                {
                    m_recorder.deleteRecording(REC_NAME + i);
                }
            }
            catch (Exception e1)
            {
                errStr += " and cleanup delete did not work.";
                btr.errorStrings.add(errStr);
                btr.resultsValue++;
            }
        }
        stopTest(testName, btr);
    }

    /*
     * m_vbox.write("1: Test one recording ");
     * m_vbox.write("2: Test one recording that expires");
     * m_vbox.write("3: Test multiple recordings");
     * m_vbox.write("4: Test multiple recordings that expire");
     * m_vbox.write("5: Test one recording that is deleted");
     * m_vbox.write("6: Test multiple recordings that are deleted");
     */

    private void logResultsObj(String testName, TestResults results)
    {
        logStatus(testName + ":");
        logStatus("" + results.freeSpaceBeforeRec + "  (Free space before recording)");
        logStatus("" + results.freeSpaceAfterRec + "  (Free space after recording)");
        logStatus("" + (results.freeSpaceAfterRec - results.freeSpaceBeforeRec) + "  (delta)");
        logStatus("" + results.recordingSize + "  (Recording Size (bytes))");
    }

    private void test_mRecordingsDeleted()
    {
        String testName = "test_mRecordingsDeleted";

        startTest(testName);

        TestResults btr = new TestResults();
        TestResults initialResults = null;

        for (int i = BEGINING_REC_NUM; i <= MULTIPLE_REC_NUM; i++)
        {
            logRecName(getMultipleRecName(testName, i));
            if (i == BEGINING_REC_NUM)
            {
                initialResults = record(testName, i, SHORT_EXPIRATION_LENGTH, btr);
            }
            else
            {
                record(testName, i, SHORT_EXPIRATION_LENGTH, btr);
            }
            logToScreen(""); // new line between records
        }

        for (int j = BEGINING_REC_NUM; j <= MULTIPLE_REC_NUM; j++)
        {
            String recName = getMultipleRecName(testName, j);
            logToScreen(testName + ": deleting recording " + "'" + recName + "'");
            try
            {
                m_recorder.deleteRecording(recName);
            }
            catch (Exception e)
            {
                btr.resultsValue++;
                btr.errorStrings.add("delete failed: " + e.getMessage());
            }
        }

        // at this point the recordings should be deleted
        long freeSpaceAfterDeletes = btr.msv.getFreeSpace();
        long deltaAfterDelete = freeSpaceAfterDeletes - btr.freeSpaceAfterRec;
        long deltaFromStart = freeSpaceAfterDeletes - initialResults.freeSpaceBeforeRec;

        // logResultsObj(testName, btr);
        logStatus(freeSpaceAfterDeletes + "  (Free space after deletes)");
        logStatus(deltaAfterDelete + "(free space recovered after deletes)");
        logStatus(deltaFromStart + "(free space recovered from start)");

        logToScreen("");
        if (freeSpaceAfterDeletes == btr.freeSpaceAfterRec)
        {
            btr.resultsValue++;
            btr.errorStrings.add("FAILED:  freespace did not increase after deletes");
        }

        if (freeSpaceAfterDeletes < initialResults.freeSpaceBeforeRec)
        {
            btr.resultsValue++;
            btr.errorStrings.add("FAILED:  not all space recovered after deletes");
        }

        stopTest(testName, btr);
    }

    // private void test_mRecordingsDeleted()
    // {
    // String testName = "test_mRecordingsDeleted";
    // BasicTestResults initialResults = null;
    // long freeSpaceAfterDeletes = 0;
    //
    // startTest("\n"+testName+"\n-----------------------------------------------------------------------------");
    //        
    // try
    // {
    // for(int i = BEGINING_REC_NUM; i <= MULTIPLE_REC_NUM; i++)
    // {
    // if(i == BEGINING_REC_NUM)
    // {
    // initialResults = createRec(testName, i, DEFAULT_EXPIRATION_LENGTH);
    // }
    // else
    // {
    // createRec(testName, i, DEFAULT_EXPIRATION_LENGTH);
    // }
    // }
    //        
    // for(int j = BEGINING_REC_NUM; j <= MULTIPLE_REC_NUM; j++)
    // {
    // logToScreen(testName + " deleting " + REC_NAME+j);
    // m_recorder.deleteRecording(REC_NAME + j);
    // }
    //
    // freeSpaceAfterDeletes = initialResults.msv.getFreeSpace();
    // }
    // catch (Exception e)
    // {
    // logToScreen(testName + " FAILED:  "+e.getMessage(), true);
    // e.printStackTrace();
    // stopTest(0, "");
    // return;
    // }
    //
    //        
    //
    // logStatus(testName+" freeSpaceBeforeRecordings: " +
    // initialResults.freeSpaceBeforeRec);
    // logStatus(testName+" freeSpaceAfterDeletes:     " +
    // freeSpaceAfterDeletes);
    // logStatus(testName+" delta:                     " +
    // (freeSpaceAfterDeletes - initialResults.freeSpaceBeforeRec));
    //        
    //        
    // logToScreen("");
    // if(freeSpaceAfterDeletes < initialResults.freeSpaceBeforeRec)
    // {
    // logToScreen(testName+" FAILED:  deleting recordings did not free space");
    // }
    // else
    // {
    // logToScreen(testName+" PASSED");
    // }
    // stopTest(0, "");
    //
    // }

    private TestResults record(String testName, int i, long expiration, TestResults results)
    {
        String testRecName = getMultipleRecName(testName, i);
        return basicTest(testRecName, DEFAULT_REC_LENGTH, expiration, testName + " (" + i + "/" + MULTIPLE_REC_NUM
                + ")", results);
    }

    private String getMultipleRecName(String testName, int i)
    {
        String testRecName = getRecName(testName) + i;
        return testRecName;
    }

    public void keyReleased(KeyEvent key)
    {

    }

    public void keyTyped(KeyEvent key)
    {

    }

    public class TestResults implements Cloneable
    {
        protected Object clone() throws CloneNotSupportedException
        {
            return super.clone();
        }

        public long freeSpaceBeforeRec = 0;

        public long freeSpaceAfterRec = 0;

        public long recordingSize = 0;

        public OcapRecordedService ors = null;

        public OcapRecordingRequest rr = null;

        public MediaStorageVolume msv = null;

        public int resultsValue = 0;

        public Vector errorStrings = new Vector();

    }

    private TestResults basicTest(String recName, long duration, long expiration, String testStr, TestResults results)
    {
        String outStrStart = "BasicTest of '" + recName + "'";

        try
        {
            results.msv = m_recorder.getDefaultStorageVolume();
            results.freeSpaceBeforeRec = results.msv.getFreeSpace();

            results.rr = (OcapRecordingRequest) m_recorder.nowRecording(recName, m_locator, duration, // duration
                    expiration, // expiration (from recording initiation)
                    OcapRecordingProperties.DELETE_AT_EXPIRATION, // retentionPriority
                    OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, // recordingPriority
                    null, // ExtendedFileAccessPermissions
                    ORG1, // Org
                    null, // MediaStorageVolume
                    true);

            if (results.rr == null)
            {
                String s = outStrStart + " FAILED:  unable to create recording";
                results.errorStrings.add(s);
                results.resultsValue++;

                return (TestResults) results.clone(); // return snapshot
            }

            results.freeSpaceAfterRec = results.msv.getFreeSpace();
            results.ors = (OcapRecordedService) results.rr.getService();
            results.recordingSize = results.ors.getRecordedSize();
        }
        catch (Exception e)
        {
            String s = outStrStart + " FAILED:  " + e.getMessage();
            e.printStackTrace();

            results.errorStrings.add(s);
            results.resultsValue++;

            try
            {
                return (TestResults) results.clone();
            }
            catch (CloneNotSupportedException e1)
            {
                e1.printStackTrace();
                results.resultsValue++;
                results.errorStrings.add(e.getMessage());
                return null;
            }
        }

        logStatus(outStrStart + ":  ");
        logStatus("'" + recName + "': " + results.freeSpaceBeforeRec + "  (Free space before rec.)");
        logStatus("'" + recName + "': " + results.freeSpaceAfterRec + "  (Free space after rec.)");
        logStatus("'" + recName + "': " + (results.freeSpaceAfterRec - results.freeSpaceBeforeRec) + "  (delta)");
        logStatus("'" + recName + "': " + results.recordingSize + "  (Recording Size (bytes))");

        // allocation can be larger than the recording size due to overhead,
        // etc.
        if ((results.freeSpaceBeforeRec - results.freeSpaceAfterRec) >= results.recordingSize)
        {
            ;
        }
        else
        {
            String s = outStrStart + " FAILED:   freespace did not decrease by >= recording size";

            results.errorStrings.add(s);
            results.resultsValue++;
        }

        try
        {
            return (TestResults) results.clone(); // return snapshot
        }
        catch (CloneNotSupportedException e)
        {
            e.printStackTrace();
            results.resultsValue++;
            results.errorStrings.add(e.getMessage());
            return null;
        }
    }

    private void logStatus(final String str)
    {
        if (m_isAutoXlet) System.out.println(str);
        m_log.log(str);
    }

    private void logToScreen(final String str, boolean lineBefore)
    {
        if (lineBefore)
        {
            logToScreen("");
        }

        logToScreen(str);
    }

    private void logToScreen(final String str)
    {
        // logStatus(str);
        m_vbox.write(str);
    }

    private void logResultToScreen(String testName, TestResults btr, boolean newLine)
    {
        String testResult = getReasonString(btr);
        if (testResult.length() != 0)
        {
            logToScreen("Test <" + testName + "> FAILED:", newLine);
            logToScreen(testResult, false);
        }
        else
        {
            logToScreen("Test <" + testName + "> PASSED", newLine);
        }
    }

    private String getReasonString(TestResults btr)
    {
        String reason = "";
        int size = btr.errorStrings.size();
        for (int i = 0; i < size; i++)
        {
            String s = (String) btr.errorStrings.elementAt(i);
            reason += "    (" + i + ") " + s;
            if (i < (size - 1)) reason += "\n";
        }
        return reason;
    }

    /*
     * !!!!For AutoXlet automation framework!!!!! Method used to send completion
     * of events
     */
    public void notifyTestComplete(String testName, TestResults btr)
    {
        String reason = null; // non null if error
        String testResult = "PASSED";

        if (btr.resultsValue != 0)
        {
            testResult = "FAILED";
            reason = getReasonString(btr);
        }

        m_log.log("Test <" + testName + "> completed; result=" + testResult);
        if (reason != null) m_log.log(reason);

        // if reason == null, the test passed. If it failed, display the message
        m_test.assertTrue("Test <" + testName + "> FAILED:\n" + reason, reason == null);
        logToScreen("Done");

        m_eventMonitor.notifyReady();
    }

    public void dispatchEvent(KeyEvent e, boolean useMonitor, int timeout)
    {
        if (useMonitor)
        {
            m_eventMonitor.setTimeout(timeout);
            synchronized (m_eventMonitor)
            {
                executeTest(e, true);
                m_eventMonitor.waitForReady();
            }
        }
        else
        {
            keyPressed(e);
        }
    }
}
