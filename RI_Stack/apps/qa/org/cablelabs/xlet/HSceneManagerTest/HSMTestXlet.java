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

package org.cablelabs.xlet.HSceneManagerTest;

import org.cablelabs.lib.utils.ArgParser;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;

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

import org.ocap.ui.HSceneManager;

public class HSMTestXlet implements Xlet, HSMTestControl
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

            m_eventHandler = (HSMTestEvents) (IxcRegistry.lookup(ctx, "/" + Integer.toHexString(oID) + "/"
                    + Integer.toHexString(aID) + "/HSMTestEvents"));

            // Publish control object via IXC to make it available to the test
            // runner
            IxcRegistry.bind(ctx, "HSMTestControl" + m_xletName, this);
        }
        catch (Exception e)
        {
            throw new XletStateChangeException("Error setting up IXC communication with runner! -- " + e.getMessage());
        }

        // ///////////////////////////////////////////////////////////////////////////
        // UI Setup
        //

        // Scene size and position
        int x = 0;
        int y = 0;
        int width = 0;
        int height = 0;
        try
        {
            x = ap.getIntArg("x");
        }
        catch (Exception e)
        {
        }
        try
        {
            y = ap.getIntArg("y");
        }
        catch (Exception e)
        {
        }
        try
        {
            width = ap.getIntArg("width");
        }
        catch (Exception e)
        {
        }
        try
        {
            height = ap.getIntArg("height");
        }
        catch (Exception e)
        {
        }

        Color bgColor = Color.black;
        try
        {
            bgColor = (Color) Color.class.getField(ap.getStringArg("color")).get(null);
        }
        catch (Exception e)
        {
        }

        m_testInfo = new HSMTestUI(bgColor);

        // Initialize HScene
        m_scene = HSceneFactory.getInstance().getDefaultHScene();
        m_scene.setBounds(x, y, width, height);
        m_testInfo.setBounds(0, 0, width, height);
        m_scene.add(m_testInfo);

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
            IxcRegistry.unbind(m_ctx, "HSMTestControl" + m_xletName);
        }
        catch (NotBoundException e)
        {
        }
    }

    // Called by the test runner Xlet to request that the xlet pop itself to
    // the front of the z-order
    public void popToFront() throws RemoteException
    {
        m_scene.show();
    }

    // Called by the test runner Xlet to modify the move type
    public void toggleMoveType() throws RemoteException
    {
        m_moveType = (m_moveType == MOVE_TYPE_POSITION) ? MOVE_TYPE_SIZE : MOVE_TYPE_POSITION;

        m_scene.repaint();
    }

    // Called by the test runner Xlet to move or resize the xlet scene
    // in the positive X direction
    public void moveXPlus() throws RemoteException
    {
        if (m_moveType == MOVE_TYPE_POSITION)
        {
            Point currentPos = m_scene.getLocation();
            m_scene.setLocation(currentPos.x + MOVE_INCREMENT, currentPos.y);
        }
        else if (m_moveType == MOVE_TYPE_SIZE)
        {
            Dimension currentSize = m_scene.getSize();
            m_scene.setSize(currentSize.width + MOVE_INCREMENT, currentSize.height);
            m_testInfo.setSize(m_scene.getSize());
        }
    }

    // Called by the test runner Xlet to move or resize the xlet scene
    // in the negative X direction
    public void moveXMinus() throws RemoteException
    {
        if (m_moveType == MOVE_TYPE_POSITION)
        {
            Point currentPos = m_scene.getLocation();
            m_scene.setLocation(currentPos.x - MOVE_INCREMENT, currentPos.y);
        }
        else if (m_moveType == MOVE_TYPE_SIZE)
        {
            Dimension currentSize = m_scene.getSize();
            if (currentSize.width - MOVE_INCREMENT > 0)
            {
                m_scene.setSize(currentSize.width - MOVE_INCREMENT, currentSize.height);
                m_testInfo.setSize(m_scene.getSize());
            }
        }
    }

    // Called by the test runner Xlet to move or resize the xlet scene
    // in the positive Y direction
    public void moveYPlus() throws RemoteException
    {
        if (m_moveType == MOVE_TYPE_POSITION)
        {
            Point currentPos = m_scene.getLocation();
            m_scene.setLocation(currentPos.x, currentPos.y + MOVE_INCREMENT);
        }
        else if (m_moveType == MOVE_TYPE_SIZE)
        {
            Dimension currentSize = m_scene.getSize();
            m_scene.setSize(currentSize.width, currentSize.height + MOVE_INCREMENT);
            m_testInfo.setSize(m_scene.getSize());
        }
    }

    // Called by the test runner Xlet to move or resize the xlet scene
    // in the negative Y direction
    public void moveYMinus() throws RemoteException
    {
        if (m_moveType == MOVE_TYPE_POSITION)
        {
            Point currentPos = m_scene.getLocation();
            m_scene.setLocation(currentPos.x, currentPos.y - MOVE_INCREMENT);
        }
        else if (m_moveType == MOVE_TYPE_SIZE)
        {
            Dimension currentSize = m_scene.getSize();
            if (currentSize.height - MOVE_INCREMENT > 0)
            {
                m_scene.setSize(currentSize.width, currentSize.height - MOVE_INCREMENT);
                m_testInfo.setSize(m_scene.getSize());
            }
        }
    }

    private class HSMTestUI extends Container
    {
        public HSMTestUI(Color bgColor)
        {
            super();
            this.bgColor = bgColor;
            // setBackground(bgColor);
            // setForeground(Color.black);
            setFont(new Font("tiresias", Font.BOLD, 12));
        }

        public void paint(Graphics g)
        {
            g.setColor(bgColor);

            // Draw outline
            Dimension d = getParent().getSize();
            g.fillRoundRect(0, 0, d.width, d.height, 15, 15);

            int x = 20, y = 25;

            // App Name
            g.setColor(Color.black);
            g.drawString("<<" + m_xletName + ">>", x, y);

            y += 20;

            // Move Type
            g.drawString("Move Type: " + moveTypeToString(), x, y);

            y += 20;

            // Current Z-order position
            g.drawString("Current Z-Order Position: " + HSceneManager.getInstance().getAppHSceneLocation(), x, y);
        }

        private Color bgColor = null;
    }

    private String moveTypeToString()
    {
        switch (m_moveType)
        {
            case MOVE_TYPE_POSITION:
                return "Position";
            case MOVE_TYPE_SIZE:
                return "Size";
            default:
                return "Unknown";
        }
    }

    private XletContext m_ctx;

    private String m_xletName;

    private AppID m_appID;

    private HScene m_scene;

    // Test runner xlet can choose to modify either the size or position
    // of the xlet at any one time
    private static final int MOVE_TYPE_POSITION = 0x0;

    private static final int MOVE_TYPE_SIZE = 0x1;

    private int m_moveType = MOVE_TYPE_POSITION;

    private static final int MOVE_INCREMENT = 10;

    // GUI Component
    private HSMTestUI m_testInfo = null;

    // Event handler provided by the test runner via IXC
    private HSMTestEvents m_eventHandler;
}
