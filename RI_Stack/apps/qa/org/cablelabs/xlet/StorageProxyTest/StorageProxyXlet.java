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
package org.cablelabs.xlet.StorageProxyTest;

import java.io.IOException;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import java.awt.event.*;

import org.havi.ui.*;
import org.havi.ui.event.HRcEvent;
import org.dvb.ui.DVBColor;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.test.autoxlet.*;

import org.ocap.storage.DetachableStorageOption;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageOption;
import org.ocap.storage.StorageProxy;

/**
 * The class presents a simple Xlet example for writing an OCAP application.
 * 
 * @author Vidiom Systems, Inc.
 */
public class StorageProxyXlet implements javax.tv.xlet.Xlet, KeyListener, Driveable
{
    // A flag indicating that the Xlet has been started.
    boolean m_started = false;

    XletContext m_xctx;

    HScene scene = null;

    VidTextBox m_vbox;

    AutoXletClient m_axc = null; // Auto Xlet client

    Monitor m_eventMonitor = null; // Monitor for AutoXlet

    Logger m_log = null; // Logger for AutoXlet

    static Test m_test = null; // Current test function.s

    private static final String LSV_NAME = "TestVolume";

    private StorageManager m_storageMgr;

    /**
     * The default constructor.
     */
    public StorageProxyXlet()
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

            m_storageMgr = StorageManager.getInstance();

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
            System.out.println("StorageProxyXlet:startXlet()");
            scene.show();
            scene.requestFocus();
            m_vbox.write("Storage Proxy Xlet App");

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
            m_storageMgr = null;

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            throw new javax.tv.xlet.XletStateChangeException(ex.getMessage());
        }
    }

    /*
     * Displays the selections for keypresses
     */
    private void displaySelections()
    {
        m_vbox.write("1: Initialize Test");
        m_vbox.write("2: Check Volumes");
        m_vbox.write("3: Create Volumes");
        m_vbox.write("4: Wipe everything");
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
                initializeTest();
                break;
            case HRcEvent.VK_2:
                printVolumes(getDetachedStorageProxy());
                break;
            case HRcEvent.VK_3:
                try
                {
                    createVolume(getDetachedStorageProxy());
                }
                catch (IOException e)
                {
                    m_vbox.write("IOException caught while creating volume.");
                    e.printStackTrace();
                }
                break;
            case HRcEvent.VK_4:
                reinitialize(getDetachedStorageProxy());
                break;
            default:
                m_vbox.write("No test available");
                break;
        }
    }

    public void initializeTest()
    {
        // Create a logical volume on an internal device.
        StorageProxy proxy = getDetachedStorageProxy();
        LogicalStorageVolume lsv = null;
        try
        {
            createVolume(proxy);
        }
        catch (IOException ex)
        {
            System.out.println("IOException caught while creating volume.");
            ex.printStackTrace();
        }

        if (lsv != null)
        {
            reinitialize(proxy);
        }
        else
            System.out.println("Unable to allocate general purpose volume.");
    }

    public LogicalStorageVolume createVolume(StorageProxy proxy) throws java.io.IOException
    {
        ExtendedFileAccessPermissions fap = new ExtendedFileAccessPermissions(true, true, true, true, true, true, null,
                null);
        if (fap == null) System.out.println("Unable to create ExtendedFileAccessPermissions.");
        LogicalStorageVolume lsv = null;
        // try{
        lsv = proxy.allocateGeneralPurposeVolume(LSV_NAME, fap);
        // }catch(IOException err){
        // System.out.println("Exception thrown, allocating general purpose volume. ");
        // err.printStackTrace();
        // }

        printVolumes(proxy);
        return lsv;
    }

    public void reinitialize(StorageProxy proxy)
    {
        try
        {
            proxy.initialize(true);
        }
        catch (Exception e)
        {
            System.out.println("Exception thrown on initialize call");
            e.printStackTrace();
        }
        // Verify that no LogicalStorageVolumes exist
        printVolumes(proxy);
    }

    public void printVolumes(StorageProxy proxy)
    {
        LogicalStorageVolume[] lsvs = proxy.getVolumes();
        if (lsvs != null && lsvs.length != 0)
        {
            for (int i = 0; i < lsvs.length; i++)
            {
                System.out.println("Volume " + i + " : " + lsvs[i].toString());
            }
            System.out.println("LogicalStorageVolumes found; length : " + lsvs.length);
        }
        else if (lsvs == null)
        {
            System.out.println("No Logical Storage Volumes existing (null)");
        }
        else
        {
            System.out.println("Empty LogicalStorageVolume array");
        }
    }

    public StorageProxy getDetachedStorageProxy()
    {
        // Create a logical volume on an internal device.
        StorageProxy[] proxies = m_storageMgr.getStorageProxies();
        if (proxies.length == 0)
            System.out.println("Expected at leaset one storage proxy device as configured in the mpeenv.ini.");
        for (int i = 0; i < proxies.length; i++)
        {
            // Check to see if this is a detachable device, if not do nothing
            StorageOption[] options = proxies[i].getOptions();
            for (int j = 0; j < options.length; j++)
            {
                if (options[j] instanceof DetachableStorageOption) return proxies[i];
            }
        }
        System.out.println("No detachable devices");
        return null;
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
