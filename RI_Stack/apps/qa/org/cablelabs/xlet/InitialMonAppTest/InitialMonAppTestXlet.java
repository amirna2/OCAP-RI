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

package org.cablelabs.xlet.InitialMonAppTest;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.*;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;

import org.havi.ui.event.HRcEvent;

import org.ocap.ui.event.OCRcEvent;
import org.dvb.application.*;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;
import org.ocap.system.MonitorAppPermission;
import java.security.PermissionCollection;
import java.security.Permissions;

import org.ocap.hardware.frontpanel.*;

import java.lang.SecurityException;
import java.lang.IllegalArgumentException;
import java.io.IOException;

import org.cablelabs.lib.utils.VidTextBox;
import org.cablelabs.lib.utils.ArgParser;

import org.ocap.OcapSystem;
import org.ocap.application.AppPattern;
import org.ocap.application.AppFilter;
import org.ocap.application.AppManagerProxy;
import org.ocap.application.AppSignalHandler;
import org.ocap.application.OcapAppAttributes;

public class InitialMonAppTestXlet extends Container implements Xlet, KeyListener, AppSignalHandler
{
    // The OCAP Xlet context.
    XletContext m_ctx;

    // A HAVi Scene.
    private HScene m_scene;

    private static VidTextBox m_vbox;

    private AppFilter appFilter;

    private int patternPriority = 255;

    private String message = "DEFAULT MESSAGE";

    private boolean sendMonitorConfiguringSignal = true;

    private boolean sendMonitorConfiguredSignal = true;

    private static String SECTION_DIVIDER = "==================================";

    public InitialMonAppTestXlet()
    {
        System.out.println("InitialMonAppTestXlet::constructor");

        try
        {
            System.out.println("InitialMonAppTestXlet::constructor - calling MonitorConfiguringSignal()");
            OcapSystem.monitorConfiguringSignal(0, 0);
        }
        catch (Exception configuringE)
        {
            System.out.println("InitialMonAppTestXlet::constructor - MonitorConfiguringSignal() exception: "
                    + configuringE);
            configuringE.printStackTrace();
        }
        System.out.println("InitialMonAppTestXlet::constructor - called MonitorConfiguringSignal()");

        /*
         * try {System.out.println(
         * "InitialMonAppTestXlet::constructor - calling MonitorConfiguredSignal()"
         * ); OcapSystem.monitorConfiguredSignal(); } catch (Exception
         * configuredE) {System.out.println(
         * "InitialMonAppTestXlet::constructor - MonitorConfiguredSignal() exception: "
         * +configuredE); configuredE.printStackTrace(); }System.out.println(
         * "InitialMonAppTestXlet::constructor - called MonitorConfiguredSignal()"
         * ); print("called MonitorConfiguredSignal()");
         */
    }

    /**
     * Initializes the OCAP Xlet.
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        System.out.println("[InitialMonAppTestXlet] : initXlet() - begin");

        // store off our xlet context
        m_ctx = ctx;

        // Setup the application graphical user interface.
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_vbox = new VidTextBox(40, 280, 430, 200, 14, 5000);
        m_scene.add(m_vbox);
        m_scene.addKeyListener(this);
        m_scene.addKeyListener(m_vbox);

        System.out.println("[InitialMonAppTestXlet] : initXlet() - end");
    }

    /**
     * Starts the OCAP Xlet.
     */
    public void startXlet() throws XletStateChangeException
    {
        System.out.println("[InitialMonAppTestXlet] : startXlet() - start");

        parseArgs((String[]) m_ctx.getXletProperty(XletContext.ARGS));

        print("send MonitorConfiguring Signal=" + sendMonitorConfiguringSignal);
        print("send monitorConfigured Signal=" + sendMonitorConfiguredSignal);

        // Set up handler
        AppManagerProxy appmgr = AppManagerProxy.getInstance();
        try
        {
            appmgr.setAppSignalHandler(this); // AppSignalHandler

            if (appFilter != null) // AppFilterHandler
            {
                appmgr.setAppFilter(appFilter);
            }
        }
        catch (Exception e)
        {
            System.out.println("!!! caught exception while setting handlers" + e);
        }

        print("\n" + message);

        // Display the application.
        m_scene.show();
        m_scene.requestFocus();

        System.out.println("[InitialMonAppTestXlet] : startXlet() - begin");
    }

    /**
     * Pauses the OCAP Xlet.
     */
    public void pauseXlet()
    {
        m_scene.setVisible(false);
    }

    /**
     * Destroys the OCAP Xlet.
     * 
     * @throws XletStateChangeException
     *             If something goes wrong, then an XletStateChangeException is
     *             sent so that the runtime system knows that the Xlet can't be
     *             destroyed.
     */
    public void destroyXlet(boolean forced) throws XletStateChangeException
    {
        print("calling InitialMonAppTest::destroyXlet()");

        m_scene.setVisible(false);

        // Clean up and dispose of resources.
        HScene tmp = m_scene;
        m_scene = null;
        HSceneFactory.getInstance().dispose(tmp);
    }

    private void parseArgs(String[] args)
    {
        if (args == null) return;
        for (int i = 0; i < args.length; ++i)
        {
            if (args[i].startsWith("msg="))
            {
                message = args[i].substring(4);
            }
            else if (args[i].startsWith("configuringSignaled="))
            {
                String configuringVal = args[i].substring(20);
                if (configuringVal.equalsIgnoreCase("true"))
                {
                    sendMonitorConfiguringSignal = true;
                }
                else
                {
                    sendMonitorConfiguringSignal = false;
                }
            }
            else if (args[i].startsWith("configuredSignaled="))
            {
                String configuredVal = args[i].substring(19);
                if (configuredVal.equalsIgnoreCase("true"))
                {
                    sendMonitorConfiguredSignal = true;
                }
                else
                {
                    sendMonitorConfiguredSignal = false;
                }
            }
            else if (args[i].startsWith("app.deny="))
            {
                addFilter(AppPattern.DENY, args[i].substring(9));
            }
            else
            {
                System.out.println("Unknown argument: " + args[i]);
            }
        }
    }

    /**
     * Adds the given pattern to the application launch filter.
     * 
     * @param type
     *            one of {@link AppPattern#DENY}, {@link AppPattern#ALLOW}, or
     *            {@link AppPattern#ASK}
     * @param pattern
     *            a pattern suitable for {@link AppPattern}
     */
    private void addFilter(int type, String pattern)
    {
        AppPattern p = new AppPattern(pattern, type, patternPriority--);
        if (appFilter == null)
        {
            appFilter = new AppFilter()
            {
                public boolean accept(AppID id)
                {
                    boolean rc = super.accept(id);
                    print("app: App=" + id + " was " + (rc ? "allowed" : "denied"));
                    return rc;
                }
            };
        }

        appFilter.add(p);
    }

    // 
    // Key Handling methods
    // 
    public void keyPressed(KeyEvent e)
    {
        int key = e.getKeyCode();
        m_vbox.write("keyPressed: " + e.getKeyText(key) + "(" + key + ")");

        switch (key)
        {
            case OCRcEvent.VK_COLORED_KEY_3: // yellow triangle (A)
                break;
            case OCRcEvent.VK_COLORED_KEY_2: // blue square (B)
                break;

            case OCRcEvent.VK_0:
            case OCRcEvent.VK_1:
            case OCRcEvent.VK_2:
            case OCRcEvent.VK_3:
            case OCRcEvent.VK_4:
            case OCRcEvent.VK_5:
            case OCRcEvent.VK_6:
            case OCRcEvent.VK_7:
            case OCRcEvent.VK_8:
            case OCRcEvent.VK_9:
                break;

            case OCRcEvent.VK_ENTER:
                break;

            case OCRcEvent.VK_INFO:
                break;

            default:
                break;
        }
    }

    public void keyReleased(KeyEvent e)
    {
    }

    public void keyTyped(KeyEvent e)
    {
    }

    /**
     * Implements AppSignalHandler.
     */
    public boolean notifyXAITUpdate(OcapAppAttributes[] apps)
    {
        if (apps == null)
        {
            print("XAIT is null");
            return true;
        }

        print("XAIT Update contains " + apps.length + " applications");
        for (int i = 0; i < apps.length; ++i)
        {
            print(" " + apps[i].getIdentifier() + " " + apps[i].getName());
        }

        return true;
    }

    //
    // printing function - allow messages to post in screen and log
    //
    private void print(String msg)
    {
        System.out.println("[InitialMonAppTestXlet] : " + msg);
        m_vbox.write("    " + msg);
    }

}
