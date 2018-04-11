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
package org.cablelabs.xlet.ECN1057;

import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import java.util.*;
import java.awt.Component;
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
import org.cablelabs.lib.utils.Recorder;
import org.cablelabs.test.autoxlet.*;

import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;

/**
 * The class presents a simple Xlet example for writing an OCAP application.
 * 
 * @author Vidiom Systems, Inc.
 */
public class RecordingXlet implements javax.tv.xlet.Xlet, KeyListener, Driveable
{
    // A flag indicating that the Xlet has been started.
    boolean m_started = false;

    XletContext m_xctx;

    HScene scene = null;

    VidTextBox m_vbox;

    Recorder m_recorder;

    OcapLocator m_locator;

    ExtendedFileAccessPermissions m_efap = null;

    AutoXletClient m_axc = null; // Auto Xlet client

    Monitor m_eventMonitor = null; // Monitor for AutoXlet

    Logger m_log = null; // Logger for AutoXlet

    static Test m_test = null; // Current test function.s

    static final String LOCATOR = "RecFPQ";

    static final String CONFIG_FILE = "config_file";

    static final String ORG1 = "00000001";

    static final String ORG2 = "00000002";

    static final String REC_NAME = "TestRec";

    /**
     * The default constructor.
     */
    public RecordingXlet()
    {
        // Does nothing extra.
    }

    /**
     * Initializes the OCAP Xlet.
     * <p>
     * A reference to the context is stored for further need. This is the place
     * where any initialisation should be done, unless it takes a lot of time or
     * resources.
     * </p>
     * 
     * @param The
     *            context for this Xlet is passed in.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             initialised.
     */
    public void initXlet(javax.tv.xlet.XletContext ctx) throws javax.tv.xlet.XletStateChangeException
    {
        try
        {
            // store off our xlet context
            m_xctx = ctx;

            /*
             * Set up Auto Xlet Client
             */
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

            /*
             * Create a recorder object use
             */
            m_recorder = new Recorder();

            /*
             * Grab valid service locators from Xlet params
             */
            String str_locator = getConfigParameter(LOCATOR);

            /*
             * Parse out the string enty
             */
            StringTokenizer st = new StringTokenizer(str_locator, ",");
            String elem = st.nextToken();
            System.out.println("Using freq " + elem);
            int frequency = Integer.parseInt(elem);
            elem = st.nextToken();
            int programNum = Integer.parseInt(elem);
            elem = st.nextToken();
            int qam = Integer.parseInt(elem);

            System.out.println(" Channel- freq: " + frequency + " qam :" + qam + " pid :" + programNum);
            m_locator = new OcapLocator(frequency, programNum, qam);
            System.out.println("Recording Xlet locator: " + m_locator);

            /*
             * Set up video graphics for the application Establish self as RC
             * key listener
             */
            System.out.println("Setting up key listener and havi interface");
            scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
            m_vbox = new VidTextBox(50, 50, 530, 370, 14, 5000);
            m_vbox.setBackground(new DVBColor(128, 128, 128, 155));
            m_vbox.setForeground(new DVBColor(200, 200, 200, 255));

            scene.add(m_vbox);
            scene.addKeyListener(this);
            scene.addKeyListener(m_vbox);

            /*
             * Global EFAP to be used with recordings Grant everone access so
             * that control is determined by the recording
             * 
             * World permissions - read/write Org permissions - read/write App
             * permissions - read/write Other Orgs - no access
             */
            m_efap = new ExtendedFileAccessPermissions(true, true, true, true, true, true, null, null);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
        }
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
            scene.show();
            scene.requestFocus();
            m_vbox.write("Recorder Xlet App");

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
            scene.setVisible(false);
            // dispose of self
            HScene tmp = scene;
            scene = null;
            HSceneFactory.getInstance().dispose(tmp);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
        }
    }

    /*
     * Retrieve the preameters
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
            String str_config_file_name = xlet_args.getStringArg(CONFIG_FILE);

            // Set up a new parser for parsing in the config file
            fis_count = new FileInputStream(str_config_file_name);
            byte[] buffer = new byte[fis_count.available() + 1];
            fis_count.read(buffer);
            String str_config_file = new String(buffer);
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
        m_vbox.write("1: Create a recording granting access from all organizations ");
        m_vbox.write("2: Create a recording granting access to only org 1");
        m_vbox.write("3: Remove recording access from orgs 1 and 2");
        m_vbox.write("4: Grant access to organizations 1 and 2");
        m_vbox.write("5: Delete recordings created from this app");
        m_vbox.write("6: Attempt recording but fail with IllegalArgumentException");
    }

    /*
     * Key press handler function
     * 
     * @param key Key Event callback representing a keypress
     */
    public void keyPressed(KeyEvent key)
    {
        switch (key.getKeyCode())
        {
            case HRcEvent.VK_1:
                AllOrgRec();
                break;
            case HRcEvent.VK_2:
                OneOrgRec();
                break;
            case HRcEvent.VK_3:
                RemoveAccess1And2();
                break;
            case HRcEvent.VK_4:
                AddAccess1And2();
                break;
            case HRcEvent.VK_5:
                DeleteRecordings();
                break;
            case HRcEvent.VK_6:
                InvalidOrg();
                break;
            default:
                m_vbox.write("No test available");
                break;
        }
    }

    private void InvalidOrg()
    {
        boolean success = false;
        String msg = null;
        System.out.println("**** Starting Invalid Org testing *****");
        try
        {
            // Delete recordings of the same name
            m_recorder.deleteRecording(REC_NAME);

            // Get a refernce to the storage voulme
            MediaStorageVolume msv = m_recorder.getDefaultStorageVolume();

            // Schedule a recording
            m_recorder.scheduleRecording(REC_NAME, m_locator, (System.currentTimeMillis() + 30000), 60000,
                    1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                    OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, m_efap, ORG1, msv);

            msg = "Recording should not have succeeded";

            // Delete recording that had been successfully setup
            m_recorder.deleteRecording(REC_NAME);
        }
        catch (IllegalArgumentException e)
        {
            msg = "IllegalArgumentException thrown";
            success = true;
        }
        catch (Exception e)
        {
            msg = "Exception thrown but not IllegalArgumentException";
            e.printStackTrace();
        }

        // Report results
        if (success)
        {
            m_vbox.write("PASSED:" + msg);
            notifyTestComplete(0, msg);
        }
        else
        {
            m_vbox.write("FAILED:" + msg);
            notifyTestComplete(1, msg);
        }
    }

    /*
     * Add access to the default media storage volume to apps in Org 1 and 2
     */
    private void AddAccess1And2()
    {
        boolean success = false;
        String msg = null;
        System.out.println("**** Starting Access Addition of Org 1 and 2 *****");

        // Get the MediaStorageVolume associaed with the recording
        try
        {
            MediaStorageVolume msv = m_recorder.getMediaStorageVolume(REC_NAME);

            // Remove access from these volumes
            String[] orgs = new String[2];
            orgs[0] = ORG1;
            orgs[1] = ORG2;
            msv.allowAccess(orgs);
            success = true;
            msg = "AddAccess Finished";
        }
        catch (Exception e)
        {
            msg = "Exception thrown" + e.toString();
            e.printStackTrace();
        }

        // Report results
        if (success)
        {
            m_vbox.write("PASSED:" + msg);
            notifyTestComplete(0, msg);
        }
        else
        {
            m_vbox.write("FAILED:" + msg);
            notifyTestComplete(1, msg);
        }
    }

    /*
     * Action that removes access to organizations 1 and 2 from the recording
     * generated by this application
     */
    private void RemoveAccess1And2()
    {
        boolean success = false;
        String msg = null;
        System.out.println("**** Starting Access Removal of Org 1 and 2 *****");

        // Get the MediaStorageVolume associaed with the recording
        try
        {
            MediaStorageVolume msv = m_recorder.getMediaStorageVolume(REC_NAME);

            // Remove access from these volumes
            msv.removeAccess(ORG1);
            msv.removeAccess(ORG2);

            msg = "Executing Access Removal of Recordings FINISHED";
            success = true;
        }
        catch (Exception e)
        {
            msg = "Exception thrown" + e.toString();
            e.printStackTrace();
        }

        // Report results
        if (success)
        {
            m_vbox.write("PASSED:" + msg);
            notifyTestComplete(0, msg);
        }
        else
        {
            m_vbox.write("FAILED:" + msg);
            notifyTestComplete(1, msg);
        }
    }

    /*
     * Action that creates a recording that only can be played back with an app
     * in organization 1
     */
    private void OneOrgRec()
    {
        boolean success = false;
        String msg = null;
        System.out.println("**** Start creating Recording for Org1  *****");

        try
        {
            // Delete recordings of the same name
            m_recorder.deleteRecording(REC_NAME);

            // Get a refernce to the storage voulme
            MediaStorageVolume msv = m_recorder.getDefaultStorageVolume();

            // Schedule a recording
            LeafRecordingRequest rr = (LeafRecordingRequest) m_recorder.nowRecording(REC_NAME, m_locator, 60000,
                    1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                    OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, m_efap, ORG1, msv, true);

            success = (rr.getState() == LeafRecordingRequest.COMPLETED_STATE) ? true : false;
        }
        catch (Exception e)
        {
            msg = "Exception thrown" + e.toString();
            e.printStackTrace();
        }

        // Wait for event change to be signalled the recording is completed
        // Check for failure
        if (success)
        {
            m_vbox.write("PASSED : Recording creation passes");
            notifyTestComplete(0, msg);
        }
        else
        {
            if (msg == null)
            {
                msg = "Recording creation failed";
            }
            m_vbox.write("FAILED :" + msg);
            notifyTestComplete(1, msg);
        }
    }

    /*
     * Action that creates a recording that can be accessed by all organizations
     */
    private void AllOrgRec()
    {
        boolean success = false;
        String msg = null;
        System.out.println("**** Start Creating recording for all orgs *****");

        try
        {
            // Delete recordings with the same name
            m_recorder.deleteRecording(REC_NAME);

            // Get a refernce to the storage voulme
            MediaStorageVolume msv = m_recorder.getDefaultStorageVolume();

            // Schedule a recording
            LeafRecordingRequest rr = (LeafRecordingRequest) m_recorder.nowRecording(REC_NAME, m_locator, 60000,
                    1000 * 60 * 60 * 24, OcapRecordingProperties.DELETE_AT_EXPIRATION,
                    OcapRecordingProperties.RECORD_IF_NO_CONFLICTS, m_efap, null, msv, true);

            success = (rr.getState() == LeafRecordingRequest.COMPLETED_STATE) ? true : false;
        }
        catch (Exception e)
        {
            msg = "Exception thrown" + e.toString();
            e.printStackTrace();
        }

        // Wait for event change to be signalled the recording is completed
        // Check for failure
        if (success)
        {
            m_vbox.write("PASSED : Recording creation passes");
            notifyTestComplete(0, msg);
        }
        else
        {
            if (msg == null)
            {
                msg = "Recording creation failed";
            }
            m_vbox.write("FAILED :" + msg);
            notifyTestComplete(1, msg);
        }
    }

    /*
     * This will delete all recordings that exist for this application
     */
    private void DeleteRecordings()
    {
        boolean success = false;
        String msg = null;
        System.out.println("*** Start Deletion of Recordings from this app **");

        try
        {
            // Delete recordings with the same name
            m_recorder.deleteRecording(REC_NAME);
            msg = "Executing Deletion of Recordings FINISHED";
            success = true;
        }
        catch (Exception e)
        {
            msg = "Exception thrown :" + e.toString();
            e.printStackTrace();
        }

        // Report results
        if (success)
        {
            m_vbox.write("PASSED:" + msg);
            notifyTestComplete(0, msg);
        }
        else
        {
            m_vbox.write("FAILED:" + msg);
            notifyTestComplete(1, msg);
        }
    }

    /*
     * !!!!For AutoXlet automation framework!!!!! Method used to send completion
     * of events
     */
    public void notifyTestComplete(int result, String reason)
    {
        String testResult = "PASSED";
        if (result != 0)
        {
            testResult = "FAILED: " + reason;
        }
        m_log.log("Test completed; result=" + testResult);
        m_test.assertTrue("Test failed:" + reason, result == 0);
    }

    /*
     * For AutoXlet automation framework
     */
    public void dispatchEvent(KeyEvent e, boolean useMonitor, int timeout)
    {
        keyPressed(e);
    }

    public void keyReleased(KeyEvent key)
    {
        // TODO Auto-generated method stub

    }

    public void keyTyped(KeyEvent key)
    {
        // TODO Auto-generated method stub

    }

}
