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

package org.cablelabs.xlet.FrontPanelResourceTest;

import org.cablelabs.lib.utils.ArgParser;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.ocap.hardware.frontpanel.FrontPanelManager;

import org.dvb.application.AppsDatabase;
import org.dvb.application.AppID;
import org.dvb.io.ixc.IxcRegistry;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

public class FPTestXlet implements Xlet, FPTestControl
{
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        // Store this xlet's name and context
        m_appID = new AppID((int) (Long.parseLong((String) (ctx.getXletProperty("dvb.org.id")), 16)),
                (int) (Long.parseLong((String) (ctx.getXletProperty("dvb.app.id")), 16)));
        m_xletName = AppsDatabase.getAppsDatabase().getAppAttributes(m_appID).getName();
        m_appPriority = AppsDatabase.getAppsDatabase().getAppAttributes(m_appID).getPriority();
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

            m_eventHandler = (FPTestEvents) (IxcRegistry.lookup(ctx, "/" + Integer.toHexString(oID) + "/"
                    + Integer.toHexString(aID) + "/FPTestEvents"));

            // Publish control object via IXC to make it available to the test
            // runner
            IxcRegistry.bind(ctx, "FPTestControl" + m_xletName, this);
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

        // Initialize HScene
        m_scene = HSceneFactory.getInstance().getDefaultHScene();
        m_testInfo.setBounds(m_x, m_y, m_width, m_height);
        m_scene.add(m_testInfo);
        m_scene.validate();
    }

    public void startXlet() throws XletStateChangeException
    {
        m_scene.show();
    }

    public void pauseXlet()
    {
        releaseAllIndicators();
        m_scene.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        releaseAllIndicators();
        m_scene.dispose();

        try
        {
            IxcRegistry.unbind(m_ctx, "FPTestControl" + m_xletName);
        }
        catch (NotBoundException e)
        {
        }
    }

    // Called by the test runner Xlet to request a reservation
    public void reserveIndicator(String indicator) throws RemoteException
    {
        FPResourceClient client = new FPResourceClient(indicator, m_willingToRelease, this);

        boolean result;
        if (indicator.equals("text"))
            result = m_fpmgr.reserveTextDisplay(client);
        else
            result = m_fpmgr.reserveIndicator(client, indicator);

        if (result)
        {
            m_reservedList.put(indicator, client);

            try
            {
                m_eventHandler.indicatorReserved(m_appID.getOID(), m_appID.getAID(), indicator, m_willingToRelease);
            }
            catch (RemoteException e)
            {
            }

            m_testInfo.setMessage("Reserve SUCCESS (" + indicator + ")!");
        }
        else
            m_testInfo.setMessage("Reserve FAILED (" + indicator + ")!");

        m_scene.repaint();
    }

    // Called by the test runner Xlet to release a reservation
    public void releaseIndicator(String indicator) throws RemoteException
    {
        if (indicator.equals("text"))
            m_fpmgr.releaseTextDisplay();
        else
            m_fpmgr.releaseIndicator(indicator);

        m_reservedList.remove(indicator);

        try
        {
            m_eventHandler.indicatorReleased(m_appID.getOID(), m_appID.getAID(), indicator);
        }
        catch (RemoteException e)
        {
        }

        m_testInfo.setMessage("Request release (" + indicator + ")");
        m_scene.repaint();
    }

    public void toggleWillingToRelease() throws RemoteException
    {
        m_willingToRelease = !m_willingToRelease;
        m_scene.repaint();
    }

    // Called by a ResourceClient when its resource has been taken away
    public void freeResource(String resource)
    {
        m_reservedList.remove(resource);

        try
        {
            m_eventHandler.indicatorReleased(m_appID.getOID(), m_appID.getAID(), resource);
        }
        catch (RemoteException e)
        {
        }

        m_testInfo.setMessage("Force release (" + resource + ")");
        m_scene.repaint();
    }

    private void releaseAllIndicators()
    {
        for (Enumeration e = m_reservedList.keys(); e.hasMoreElements();)
        {
            String indicator = (String) e.nextElement();
            if (indicator.equals("text"))
                m_fpmgr.releaseTextDisplay();
            else
                m_fpmgr.releaseIndicator(indicator);

            try
            {
                m_eventHandler.indicatorReleased(m_appID.getOID(), m_appID.getAID(), indicator);
            }
            catch (RemoteException ex)
            {
            }
        }
        m_reservedList.clear();
        m_scene.repaint();
    }

    private class FPTestUI extends Container
    {
        public FPTestUI()
        {
            super();
            setBackground(Color.black);
            setForeground(Color.white);
            setFont(new Font("tiresias", Font.PLAIN, 12));
        }

        public void setMessage(String message)
        {
            m_message = message;
        }

        public void paint(Graphics g)
        {
            g.setColor(Color.white);

            // Draw outline
            g.drawRoundRect(2, 2, m_width - 4, m_height - 4, 15, 15);

            int x = 20, y = 25;

            // App Name
            g.drawString("<<" + m_xletName + ">> Priority = " + m_appPriority, x, y);

            // Newly reserved resources will either be "willing" or
            // "not willing"
            // to give up their reservation upon request
            y += 14;
            g.drawString(m_willingToRelease ? "Willing to release" : "Not willing to release", x, y);

            x += 15;
            y += 25;

            // Draw reserved indicator list
            if (m_reservedList.isEmpty())
            {
                g.drawString("No display elements reserved", x, y);
            }
            else
            {
                for (Enumeration e = m_reservedList.elements(); e.hasMoreElements(); y += 14)
                {
                    FPResourceClient client = (FPResourceClient) e.nextElement();
                    String willing = client.isWillingToRelease() ? " [Will Release]" : " [Will Not Release]";
                    g.drawString(client.getName() + willing, x, y);
                }
            }

            if (m_message != null) g.drawString(m_message, 15, 190);
        }

        String m_message = null;
    }

    private class FPResourceClient implements ResourceClient
    {
        public FPResourceClient(String name, boolean willingToRelease, FPTestXlet xlet)
        {
            m_indicatorName = name;
            m_willingToRelease = willingToRelease;
            m_xlet = xlet;
        }

        public boolean requestRelease(ResourceProxy arg0, Object arg1)
        {
            if (m_willingToRelease)
            {
                m_xlet.freeResource(m_indicatorName);
                return true;
            }
            return false;
        }

        public void release(ResourceProxy arg0)
        {
            m_xlet.freeResource(m_indicatorName);
        }

        public void notifyRelease(ResourceProxy arg0)
        {
        }

        public String getName()
        {
            return m_indicatorName;
        }

        public boolean isWillingToRelease()
        {
            return m_willingToRelease;
        }

        // For placement in HashSet
        public boolean equals(Object o)
        {
            if ((o instanceof String) && ((String) o).equals(m_indicatorName)) return true;
            return false;
        }

        // For placement in HashSet
        public int hashCode()
        {
            return m_indicatorName.hashCode();
        }

        private String m_indicatorName;

        private FPTestXlet m_xlet;

        private boolean m_willingToRelease;
    }

    private FrontPanelManager m_fpmgr = FrontPanelManager.getInstance();

    private XletContext m_ctx;

    private String m_xletName;

    private AppID m_appID;

    private int m_appPriority;

    private HScene m_scene;

    // When new indicator reservations are made, we will set a policy of either
    // being "willing" or "not willing" to give up that reservation upon request
    private boolean m_willingToRelease = false;

    // Scene screen position and size
    private int m_x = 0;

    private int m_y = 0;

    private int m_width = 640;

    private int m_height = 480;

    private Hashtable m_reservedList = new Hashtable();

    // GUI Component
    private FPTestUI m_testInfo = new FPTestUI();

    // Event handler provided by the test runner via IXC
    private FPTestEvents m_eventHandler;
}
