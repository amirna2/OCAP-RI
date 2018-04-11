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

package org.cablelabs.xlet.ECN1057;

import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import java.util.*;
import java.awt.Component;
import java.awt.event.*;

import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.selection.NormalContentEvent;

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

import org.ocap.dvr.storage.MediaStorageVolume;

import org.cablelabs.test.autoxlet.*;

public class PlaybackXlet implements javax.tv.xlet.Xlet, KeyListener, ServiceContextListener, Driveable
{

    // A flag indicating that the Xlet has been started.
    boolean m_started = false;

    boolean m_NCE = false;

    XletContext m_xctx;

    HScene scene = null;

    VidTextBox m_vbox;

    Recorder m_recorder;

    ServiceContext m_sc;

    AutoXletClient m_axc = null; // Auto Xlet client

    Monitor m_eventMonitor = null; // Monitor for AutoXlet

    Logger m_log = null; // Logger for AutoXlet

    static Test m_test = null; // Current test function.s

    static final String ORG1 = "00000001";

    static final String ORG2 = "00000002";

    static final String REC_NAME = "TestRec";

    /**
     * The default constructor.
     */
    public PlaybackXlet()
    {
        // Does nothing extra.
    }

    public void destroyXlet(boolean arg0) throws XletStateChangeException
    {
        try
        {
            if (m_started)
            {
                m_started = false;
            }
            // Dispose of Service Context
            m_sc.removeListener(this);
            m_sc.destroy();
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

    public void initXlet(XletContext ctx) throws XletStateChangeException
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
             * Create a Service Context to display video
             */
            ServiceContextFactory scf = ServiceContextFactory.getInstance();
            m_sc = scf.createServiceContext();
            m_sc.addListener(this);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
        }
    }

    public void pauseXlet()
    {

    }

    public void startXlet() throws XletStateChangeException
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
            System.out.println("PlaybackXlet:startXlet()");
            scene.show();
            scene.requestFocus();
            m_vbox.write("Playback Xlet App");

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

    private void displaySelections()
    {
        m_vbox.write("1: Attempt Playback, expect playback success");
        m_vbox.write("2: Attempt Playback, expect exception to be thrown");
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
                PlaybackSuccess();
                break;
            case HRcEvent.VK_2:
                PlaybackFail();
                break;
            default:
                m_vbox.write("No test available");
                break;
        }
    }

    private void PlaybackFail()
    {
        boolean success = false;
        boolean sleep = false;
        String msg = null;
        System.out.println("**** Starting Playback Fail check *****");

        // Setup flag for Normal Content Event
        m_NCE = false;

        // Attempt playback
        try
        {
            m_recorder.playbackRecording(REC_NAME, m_sc);
            msg = "";
        }
        catch (Exception e)
        {
            msg = "Exception thrown" + e.toString();
            e.printStackTrace();
        }

        // Wait a while
        try
        {
            Thread.sleep(60000);
            sleep = true;
        }
        catch (Exception e)
        {
            msg = msg.concat(" Troubles sleeping ");
            e.printStackTrace();
        }

        // Check to see if playback actually presented
        if (m_NCE)
        {
            msg = msg.concat(" NormalContentEvent generated ");
            success = false;
        }
        else
        {
            msg = msg.concat(" NormalContentEvent not generated ");
            success = true;
        }

        // Report results
        if (success & sleep)
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

    private void PlaybackSuccess()
    {
        boolean success = false;
        String msg = null;
        System.out.println("**** Starting Playback Success check *****");

        // Setup flag for Normal Content Event
        m_NCE = false;

        // Attempt playback
        try
        {
            m_recorder.playbackRecording(REC_NAME, m_sc);
            msg = "Playback to have been started";
            success = true;
        }
        catch (Exception e)
        {
            msg = "Exception thrown unexpectedly" + e.toString();
            e.printStackTrace();
        }

        // Wait a while
        try
        {
            Thread.sleep(60000);
        }
        catch (Exception e)
        {
            msg = msg.concat(" Troubles sleeping ");
            success = false;
            e.printStackTrace();
        }

        // Check to see if playback actually presented
        if (!m_NCE)
        {
            msg = msg.concat(" NormalContentEvent never generated ");
            success = false;
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

    public void keyReleased(KeyEvent key)
    {

    }

    public void keyTyped(KeyEvent key)
    {

    }

    public void receiveServiceContextEvent(ServiceContextEvent ev)
    {
        if (ev instanceof NormalContentEvent)
        {
            System.out.println("*************************************");
            System.out.println("****NormalContentEvent received******");
            System.out.println("*************************************");
            m_NCE = true;
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

}
