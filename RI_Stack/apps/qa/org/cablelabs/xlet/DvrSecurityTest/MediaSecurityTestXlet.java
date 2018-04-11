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
 * Created on July 26, 2005
 */
package org.cablelabs.xlet.DvrSecurityTest;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.security.AccessController;
import java.security.AccessControlException;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.event.HRcEvent;

import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingStateFilter;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageProxy;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.system.MonitorAppPermission;
import org.ocap.ui.event.OCRcEvent;

import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.storage.MediaStorageVolume;

import org.dvb.ui.DVBColor;

import org.cablelabs.lib.utils.ArgParser;
import org.cablelabs.lib.utils.VidTextBox;

/**
 * Xlet to submit one or more recording requests which will then be accessed by
 * other Xlets.
 * 
 */
public class MediaSecurityTestXlet implements Xlet, KeyListener
{
    private static final String BANNER = "+---------------------------------------------------------+";

    private static final String WHOAMI = "MediaSecurityTestXlet: ";

    private static final String CFG_ARG = "config_file";

    // We expect four LocatorRecordingSpecification tokens
    private static final int NUM_LRS_TOKENS = 6;

    private Vector m_locRecSpecs;

    // Instance variables in alphabetical order ...
    private MediaStorageVolume m_dest;

    private OcapRecordingManager m_mgr;

    private OcapRecordingRequest m_orr;

    private HScene m_scene;

    private XletContext m_xctx;

    private String m_configFname;

    private VidTextBox m_vbox;

    public void startXlet()
    {
        init();
        m_scene.show();
        m_scene.repaint();
        m_scene.requestFocus();
    }

    public void pauseXlet()
    {
        m_scene.setVisible(false);
    }

    public void destroyXlet(boolean x)
    {
        m_scene.dispose();
    }

    public void initXlet(XletContext ctx)
    {
        m_xctx = ctx;

        // Remote Control key listener
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        // x, y, w, h, font_size, #chars
        // m_vbox = new VidTextBox(50,200,530,200,14,5000);
        m_vbox = new VidTextBox(50, 50, 530, 400, 14, 5000);

        if (null == m_vbox)
        {
            System.err.println(WHOAMI + "FAILED TO INSTANTIATE VidTextBox!!!");
        }

        m_vbox.setBackgroundColor(new DVBColor(128, 128, 128, 155));
        m_vbox.setForegroundColor(new DVBColor(200, 200, 200, 255));

        m_scene.add(m_vbox);
        m_scene.addKeyListener(this);
        m_scene.addKeyListener(m_vbox);
        m_scene.setLayout(new BorderLayout());
        m_scene.validate();
        printMenu();
    }

    private void init()
    {
        StorageProxy proxyAry[] = StorageManager.getInstance().getStorageProxies();
        if (0 == proxyAry.length)
        {
            throw new IllegalStateException(WHOAMI + "StorageManager.getStorageProxies() returned zero-length array");
        }

        m_mgr = (OcapRecordingManager) OcapRecordingManager.getInstance();
        if (null == m_mgr)
        {
            throw new NullPointerException(WHOAMI + "Failed to retrieve OcapRecordingManager Object ref");
        }
        else
        {
            System.out.println(WHOAMI + "Retrieved OcapRecordingManager Object ref: " + m_mgr);
        }
    }

    public void printMenu()
    {
        log(BANNER);
        log(" Menu:");
        log("<1> - call removeAccess(null)");
        log("<2> - call allowAccess(null)");
        log("<3> - call disableBuffering()");
        log("<4> - call enableBuffering()");
        log(BANNER);
        m_scene.repaint();
    }

    /***
     *** KeyListener Interface
     ***/
    public void keyTyped(KeyEvent e)
    {
        System.out.println(WHOAMI + "keyTyped(): " + e);
    }

    public void keyReleased(KeyEvent e)
    {
        System.out.println(WHOAMI + "keyReleased(): " + e);
    }

    public void keyPressed(KeyEvent e)
    {
        boolean callShouldFail = true;
        boolean testPassed = false;
        int key = e.getKeyCode();

        if (HRcEvent.VK_INFO == key)
        {
            e.consume();
            printMenu();
            return;
        }
        else if (HRcEvent.VK_1 == key)
        {
            e.consume();

            testPassed = testRemoveAccess();
        }
        else if (HRcEvent.VK_2 == key)
        {
            e.consume();

            testPassed = testAllowAccess();
        }
        else if (HRcEvent.VK_3 == key)
        {
            e.consume();

            testPassed = testDisableBuffering();
        }
        else if (HRcEvent.VK_4 == key)
        {
            e.consume();

            testPassed = testEnableBuffering();
        }
        else
        {
            m_vbox.keyPressed(e);

            return;
        }

        if (testPassed == true)
        {
            log("TEST PASSED!");
        }
        else
        {
            log("TEST FAILED!");
        }
    }

    private void log(String msg)
    {
        System.out.println(WHOAMI + msg);
        m_vbox.write(msg);
    }

    private MediaStorageVolume getDefaultMediaStorageVolume()
    {
        MediaStorageVolume msv = null;
        StorageProxy[] proxies = StorageManager.getInstance().getStorageProxies();

        if (proxies == null || proxies[0] == null)
        {
            log("getDefaultMediaStorageVolume() couldn't find StorageProxies!");
            return null;
        }

        LogicalStorageVolume lsv[] = proxies[0].getVolumes();

        for (int i = 0; i < lsv.length; i++)
        {
            if (lsv[i] instanceof MediaStorageVolume)
            {
                msv = (MediaStorageVolume) lsv[i];
            }
        }

        if (msv == null)
        {
            log("getDefaultMediaStorageVolume() couldn't find MediaStorageVolume!");
            return null;
        }

        return msv;
    }

    private boolean testRemoveAccess()
    {
        boolean callShouldFail = true;

        log("Calling removeAccess(null) on default MediaStorageVolume...");

        MediaStorageVolume msv = getDefaultMediaStorageVolume();

        if (msv == null)
        {
            log("FAILED: Default MSV is null!");
            return false;
        }

        /*
         * Check to see if we have access. If we do, we expect no
         * SecurityException to be thrown when we call removeAccess(). If we
         * don't have access, we expect to get a SecurityException.
         */
        try
        {
            AccessController.checkPermission(new MonitorAppPermission("storage"));

            log("MonitorAppPermission(\"storage\") GRANTED.");
            callShouldFail = false;
        }
        catch (AccessControlException ace)
        {
            log("MonitorAppPermission(\"storage\") DENIED.");
            callShouldFail = true;
        }

        try
        {
            msv.removeAccess(null);

            log("No exception was thrown.");
            return (callShouldFail ? false : true);
        }
        catch (SecurityException se)
        {
            log("SecurityException was thrown.");
            return (callShouldFail ? true : false);
        }
    }

    private boolean testAllowAccess()
    {
        boolean callShouldFail = true;

        log("Calling allowAccess(null) on default MediaStorageVolume...");

        MediaStorageVolume msv = getDefaultMediaStorageVolume();

        if (msv == null)
        {
            log("FAILED: Default MSV is null!");
            return false;
        }

        /*
         * Check to see if we have access. If we do, we expect no
         * SecurityException to be thrown when we call removeAccess(). If we
         * don't have access, we expect to get a SecurityException.
         */
        try
        {
            AccessController.checkPermission(new MonitorAppPermission("storage"));

            log("MonitorAppPermission(\"storage\") GRANTED.");
            callShouldFail = false;
        }
        catch (AccessControlException ace)
        {
            log("MonitorAppPermission(\"storage\") DENIED.");
            callShouldFail = true;
        }

        try
        {
            msv.allowAccess(null);

            log("No exception was thrown.");
            return (callShouldFail ? false : true);
        }
        catch (SecurityException se)
        {
            log("SecurityException was thrown.");
            return (callShouldFail ? true : false);
        }
    }

    private boolean testDisableBuffering()
    {
        boolean callShouldFail = true;

        log("Calling disableBuffering()...");

        OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();

        try
        {
            AccessController.checkPermission(new MonitorAppPermission("recording"));

            log("MonitorAppPermission(\"recording\") GRANTED.");
            callShouldFail = false;
        }
        catch (AccessControlException ace)
        {
            log("MonitorAppPermission(\"recording\") DENIED.");
            callShouldFail = true;
        }

        try
        {
            orm.disableBuffering();

            log("No exception was thrown.");
            return (callShouldFail ? false : true);
        }
        catch (SecurityException se)
        {
            log("SecurityException was thrown.");
            return (callShouldFail ? true : false);
        }
    }

    private boolean testEnableBuffering()
    {
        boolean callShouldFail = true;

        log("Calling enableBuffering()...");

        OcapRecordingManager orm = (OcapRecordingManager) OcapRecordingManager.getInstance();

        try
        {
            AccessController.checkPermission(new MonitorAppPermission("recording"));

            log("MonitorAppPermission(\"recording\") GRANTED.");
            callShouldFail = false;
        }
        catch (AccessControlException ace)
        {
            log("MonitorAppPermission(\"recording\") DENIED.");
            callShouldFail = true;
        }

        try
        {
            orm.enableBuffering();

            log("No exception was thrown.");
            return (callShouldFail ? false : true);
        }
        catch (SecurityException se)
        {
            log("SecurityException was thrown.");
            return (callShouldFail ? true : false);
        }
    }
}
