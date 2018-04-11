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

package org.cablelabs.xlet.FocusTest;

import org.cablelabs.lib.utils.ArgParser;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;
import java.awt.GridLayout;
import java.awt.event.WindowListener;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.application.AppsDatabase;
import org.dvb.application.AppID;
import org.dvb.io.ixc.IxcRegistry;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HStaticText;
import org.havi.ui.HVisible;
import org.havi.ui.HText;

public class FocusTestXlet implements Xlet, FocusListener, WindowListener
{
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        // Store this xlet's name and context
        m_appID = new AppID((int) (Long.parseLong((String) (ctx.getXletProperty("dvb.org.id")), 16)),
                (int) (Long.parseLong((String) (ctx.getXletProperty("dvb.app.id")), 16)));
        m_xletName = AppsDatabase.getAppsDatabase().getAppAttributes(m_appID).getName();
        m_ctx = ctx;

        // Parse xlet arguments and initialize IXC communication with test
        // runner
        ArgParser ap = null;
        try
        {
            ap = new ArgParser((String[]) (ctx.getXletProperty(XletContext.ARGS)));

            // Lookup test runner's event handler object
            String arg = ap.getStringArg("runner");

            // Parse the individual appID and orgID from the 48-bit int
            long orgIDappID = Long.parseLong(arg.substring(2), 16);
            int oID = (int) ((orgIDappID >> 16) & 0xFFFFFFFF);
            int aID = (int) (orgIDappID & 0xFFFF);

            m_eventHandler = (FocusTestEvents) (IxcRegistry.lookup(ctx, "/" + Integer.toHexString(oID) + "/"
                    + Integer.toHexString(aID) + "/FocusTestEventHandler"));

            // Publish control object via IXC to make it available to the test
            // runner
            IxcRegistry.bind(ctx, "FocusTestControl" + m_xletName, m_control);
        }
        catch (Exception e)
        {
            throw new XletStateChangeException("Error setting up IXC communication with runner! -- " + e.getMessage());
        }

        // ///////////////////////////////////////////////////////////////////////////
        // UI Setup
        //

        // Scene size and position
        try
        {
            m_x = ap.getIntArg("x");
        }
        catch (Exception e)
        {
        }
        try
        {
            m_y = ap.getIntArg("y");
        }
        catch (Exception e)
        {
        }
        try
        {
            m_width = ap.getIntArg("width");
        }
        catch (Exception e)
        {
        }
        try
        {
            m_height = ap.getIntArg("height");
        }
        catch (Exception e)
        {
        }

        // Foreground and background colors for scene. Components will
        // also use these colors (but with switched fg and bg)
        try
        {
            m_fgColor = ap.getColorArg("fgColor");
        }
        catch (Exception e)
        {
        }
        try
        {
            m_bgColor = ap.getColorArg("bgColor");
        }
        catch (Exception e)
        {
        }

        // Initialize HScene
        m_scene = HSceneFactory.getInstance().getDefaultHScene();
        m_scene.setLocation(m_x, m_y);
        m_scene.setSize(m_width, m_height);
        m_scene.setLayout(new GridLayout(2, 2));
        m_scene.setBackgroundMode(HScene.BACKGROUND_FILL);
        m_scene.setForeground(m_fgColor);
        m_scene.setBackground(m_bgColor);
        m_scene.addFocusListener(this);
        m_scene.addWindowListener(this);

        // Initialize Component Widgets
        m_text1 = new HText(m_xletName + "\nNORMAL 1", m_xletName + "\nFOCUS 1");
        m_text1.setBackgroundMode(HVisible.BACKGROUND_FILL);
        m_text1.setForeground(m_bgColor);
        m_text1.setBackground(m_fgColor.brighter());
        m_text1.addFocusListener(this);
        m_text1.setFont(new Font("tiresias", Font.PLAIN, 12));

        m_text2 = new HText(m_xletName + "\nNORMAL 2", m_xletName + "\nFOCUS 2");
        m_text2.setBackgroundMode(HVisible.BACKGROUND_FILL);
        m_text2.setForeground(m_bgColor);
        m_text2.setBackground(m_fgColor.brighter());
        m_text2.addFocusListener(this);
        m_text2.setFont(new Font("tiresias", Font.PLAIN, 12));

        // Build scene
        m_scene.add(m_text1);
        m_scene.add(new HStaticText(""));
        m_scene.add(new HStaticText(""));
        m_scene.add(m_text2);
        m_scene.validate();

    }

    public void startXlet() throws XletStateChangeException
    {
        m_scene.show();
    }

    public void pauseXlet()
    {
        m_scene.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        m_scene.dispose();

        try
        {
            IxcRegistry.unbind(m_ctx, "FocusTestControl" + m_xletName);
        }
        catch (NotBoundException e)
        {
        }
    }

    // Window/Component Callbacks
    public void focusGained(FocusEvent arg0)
    {
        try
        {
            if (arg0.getComponent() == m_scene)
                m_eventHandler.focusGained(m_appID.getOID(), m_appID.getAID(), 0);
            else if (arg0.getComponent() == m_text1)
                m_eventHandler.focusGained(m_appID.getOID(), m_appID.getAID(), 1);
            else if (arg0.getComponent() == m_text2) m_eventHandler.focusGained(m_appID.getOID(), m_appID.getAID(), 2);
        }
        catch (RemoteException e)
        {
        }
    }

    public void focusLost(FocusEvent arg0)
    {
        try
        {
            if (arg0.getComponent() == m_scene)
                m_eventHandler.focusLost(m_appID.getOID(), m_appID.getAID(), 0);
            else if (arg0.getComponent() == m_text1)
                m_eventHandler.focusLost(m_appID.getOID(), m_appID.getAID(), 1);
            else if (arg0.getComponent() == m_text2) m_eventHandler.focusLost(m_appID.getOID(), m_appID.getAID(), 2);
        }
        catch (RemoteException e)
        {
        }
    }

    public void windowActivated(WindowEvent arg0)
    {
        try
        {
            m_eventHandler.windowActivated(m_appID.getOID(), m_appID.getAID());
        }
        catch (RemoteException e)
        {
        }
    }

    public void windowDeactivated(WindowEvent arg0)
    {
        try
        {
            m_eventHandler.windowDeactivated(m_appID.getOID(), m_appID.getAID());
        }
        catch (RemoteException e)
        {
        }
    }

    // TestControl class that is published to the test runner via IXC
    private class TestControl implements FocusTestControl
    {
        public void requestFocus(int componentIndex) throws RemoteException
        {
            switch (componentIndex)
            {
                case 0:
                    m_scene.requestFocus();
                    break;
                case 1:
                    m_text1.requestFocus();
                    break;
                case 2:
                    m_text2.requestFocus();
                    break;
            }
        }

        public void setVisible(int componentIndex, boolean visible) throws RemoteException
        {
            switch (componentIndex)
            {
                case 0:
                    m_scene.setVisible(visible);
                    break;
                case 1:
                    m_text1.setVisible(visible);
                    break;
                case 2:
                    m_text2.setVisible(visible);
                    break;
            }
        }

        public void setActive(boolean active) throws RemoteException
        {
            m_scene.setActive(active);
        }
    }

    XletContext m_ctx;

    String m_xletName;

    AppID m_appID;

    HScene m_scene;

    HText m_text1, m_text2;

    // Scene screen position and size
    int m_x = 0;

    int m_y = 0;

    int m_width = 640;

    int m_height = 480;

    // Scene and component foreground and background colors
    Color m_fgColor = Color.darkGray;

    Color m_bgColor = Color.white;

    TestControl m_control = new TestControl();

    // Event handler provided by the test runner via IXC
    FocusTestEvents m_eventHandler;

    // //////////////////////////////////////////////////////////////////////////
    // UNUSED -- but required by WindowListener interface
    public void windowOpened(WindowEvent arg0)
    {
    }

    public void windowClosing(WindowEvent arg0)
    {
    }

    public void windowClosed(WindowEvent arg0)
    {
    }

    public void windowIconified(WindowEvent arg0)
    {
    }

    public void windowDeiconified(WindowEvent arg0)
    {
    }
    // //////////////////////////////////////////////////////////////////////////
}
