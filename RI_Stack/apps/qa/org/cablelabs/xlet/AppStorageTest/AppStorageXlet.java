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
package org.cablelabs.xlet.AppStorageTest;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.io.FileInputStream;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.application.AppID;
import org.dvb.application.AppsDatabase;
import org.dvb.ui.DVBGraphics;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HStaticText;
import org.havi.ui.event.HRcEvent;
import org.ocap.application.OcapAppAttributes;

/**
 * 
 * <p>
 * AppStorage:
 * </p>
 * <p>
 * Description:
 * 
 */
public class AppStorageXlet implements Xlet // , Driveable
{
    private XletContext m_xc;

    private static final String PRIORITY = "expected_storage_priority";

    private FileInputStream m_fis;

    private int m_storagePriority;

    private HScene m_scene;

    /**
     * initilize xlet
     */
    public void initXlet(XletContext xletContext) throws XletStateChangeException
    {
        System.out.println("AppStorageXlet::initXlet\n");

        // initialize AutoXlet;

        m_xc = xletContext;

        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setLayout(null);

        try
        {
            String[] args = (String[]) m_xc.getXletProperty(XletContext.ARGS);

            for (int i = 0; i < args.length; i++)
            {
                if (args[i].startsWith(PRIORITY))
                {
                    m_storagePriority = Integer.parseInt(args[i].substring(PRIORITY.length() + 1));
                }
            }
            System.out.println("!!!!!! AppStorageXlet::initXlet - expectedStoragePriority is " + m_storagePriority);
        }
        catch (Exception e)
        {
            System.out.println("!!!!!! AppStorageXlet::initXlet - EXCEPTION\n");
            throw new XletStateChangeException("Error reading Args!");
        }

        // setup a monitor
        // m_eventMonitor = new Monitor();
    }

    /**
     * start the xlet
     */
    public void startXlet() throws javax.tv.xlet.XletStateChangeException
    {
        // m_log.log("AppStorageXlet::startXlet\n");
        System.out.println("AppStorageXlet::startXlet\n");

        boolean success = false;
        int storagePriority;

        // make this synchronous
        // synchronized(m_eventMonitor)
        // {

        // get the appId
        String aidStr = (String) m_xc.getXletProperty("dvb.app.id");
        String oidStr = (String) m_xc.getXletProperty("dvb.org.id");
        if (aidStr == null || oidStr == null)
        {
            // m_log.log("AppStorageXlet::startXlet - test failed, unable to get AppId");
            System.out.println("AppStorageXlet::startXlet - test failed, unable to get AppId");
        }
        int aid = Integer.parseInt(aidStr, 16);
        long oid = Long.parseLong(oidStr, 16);
        AppID id = new AppID((int) oid, aid);
        System.out.println("\n!!!!!!!!AppStorageXlet::startXlet - APPID = " + id.toString() + ".\n");

        // get app's storage priority
        AppsDatabase db = AppsDatabase.getAppsDatabase();
        OcapAppAttributes info = (OcapAppAttributes) db.getAppAttributes(id);
        storagePriority = info.getStoragePriority();

        System.out.println("AppStorageXlet::startXlet - storagePriority is " + storagePriority);
        System.out.println("AppStorageXlet::startXlet - EXPECTED storagePriority is " + m_storagePriority);
        if (storagePriority == m_storagePriority)
        {
            success = true;
            // m_log.log("AppStorageXlet::startXlet - test passed");
            System.out.println("AppStorageXlet::startXlet - test passed");
        }
        // m_test.assertTrue("AppStorage successful", success);

        String name = info.getName();
        System.out.println("AppStorageXlet::startXlet - name is " + name);
        int ctrlCode = info.getApplicationControlCode();
        System.out.println("AppStorageXlet::startXlet - applicationControlCode is " + ctrlCode);
        int priority = info.getPriority();
        System.out.println("AppStorageXlet::startXlet - priority is " + priority);
        boolean isServiceBound = info.getIsServiceBound();
        System.out.println("AppStorageXlet::startXlet - isServiceBound is " + isServiceBound);
        boolean isStartable = info.isStartable();
        System.out.println("AppStorageXlet::startXlet - isStartable is " + isStartable);
        boolean isVisible = info.isVisible();
        System.out.println("AppStorageXlet::startXlet - isVisible is " + isVisible);
        // }

        HStaticText text = new HStaticText("AppStorageXlet - StoragePriority of " + storagePriority
                + " == the expected storagePriority of " + m_storagePriority + "??? ---- " + success);
        text.setForeground(Color.lightGray);
        text.setBackground(success ? Color.green.darker() : Color.red.darker());
        text.setBackgroundMode(HStaticText.BACKGROUND_FILL);
        text.setHorizontalAlignment(HStaticText.HALIGN_CENTER);
        m_scene.add(text);
        m_scene.setVisible(true);
        m_scene.requestFocus();

    }

    /**
     * pause the xlet
     */
    public void pauseXlet()
    {
        System.out.println("AppStorageXlet::pauseXlet\n");
        m_scene.setVisible(false);
    }

    /**
     * destroy the xlet
     */
    public void destroyXlet(boolean b) throws XletStateChangeException
    {
        System.out.println("AppStorageXlet::destroyXlet");
        try
        {
            m_scene.setVisible(false);
            HSceneFactory.getInstance().dispose(m_scene);
            if (m_fis != null) m_fis.close();
        }
        catch (Exception e)
        {
            System.out.println("AppStorageXlet::destroyXlet Exception closing socket");
        }

        m_xc.notifyDestroyed();

        throw new XletStateChangeException();
    }

    /**
     * keyTyped implementation of the KeyListener interface
     */
    public void keyTyped(KeyEvent e)
    {
    }

    /**
     * keyPressed implementation of the KeyListener interface
     */
    public void keyPressed(KeyEvent e)
    {
        System.out.println("AppStorageXlet::keyPressed - " + e.getKeyCode() + "\n");

        switch (e.getKeyCode())
        {
            case HRcEvent.VK_1:
                System.out.println("AppStorageXlet::VK_1 selected\n");
                break;
            case HRcEvent.VK_2:
                System.out.println("AppStorageXlet::VK_2 selected\n");
                break;
            case HRcEvent.VK_3:
                System.out.println("AppStoargeXlet::VK_3 selected\n");
                break;
        }
        m_scene.repaint();
    }

    /**
     * keyReleased implementation of the KeyListener interface
     */
    public void keyReleased(KeyEvent e)
    {
    }

    /**
     * 
     */
    public void paint(Graphics g)
    {
        System.out.println("AppStorageXlet::paint\n");

        int xpos = 50;// fixed x position for text lines
        int yspace = 20;// space between lines
        int ypos = 120;// starting y position for text lines

        DVBGraphics dvbg = (DVBGraphics) g;
        dvbg.setColor(Color.blue);// background color
        dvbg.fillRect(30, 100, 400, 180);

        // draw single strings
        dvbg.setFont(new Font("SansSerif", Font.BOLD, 20));
        dvbg.setColor(Color.white);

        dvbg.drawString("DVBMediaSelectControl Options:   ", xpos, ypos);
        ypos += yspace;
        dvbg.drawString("a", xpos, ypos);
        ypos += yspace;
        dvbg.drawString("b", xpos, ypos);
        ypos += yspace;
        dvbg.drawString("c", xpos, ypos);
        ypos += yspace;
    }

}
