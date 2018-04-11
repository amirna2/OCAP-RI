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

package org.cablelabs.xlet.EventsTest;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsDatabase;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.dvb.io.ixc.IxcRegistry;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.ocap.event.EventManager;
import org.ocap.ui.event.OCRcEvent;

import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Monitor;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;
import org.cablelabs.xlet.EventsTest.ExclusiveEventTestControl;

public class ExclusiveEventTestRunnerXlet implements Xlet, AppStateChangeEventListener, Driveable, UserEventListener
{
    // /////////////////////////////////////////////////////////////////////////////
    // XLET FUNCTIONS //
    // /////////////////////////////////////////////////////////////////////////////

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        m_ctx = ctx;

        AppsDatabase adb = AppsDatabase.getAppsDatabase();

        // Each argument is a complete 48-bit AppID integer (orgID,appID)
        // in hex (0x) string format. Each argument indicates the test
        // xlet to launch
        String[] args = (String[]) (ctx.getXletProperty(XletContext.ARGS));

        // Populate our app proxies array from the arguments
        m_apps = new ExclusiveEventTestXletInfo[args.length];
        for (int i = 0; i < args.length; ++i)
        {
            // Parse the individual appID and orgID from the 48-bit int
            long orgIDappID = Long.parseLong(args[i].substring(2), 16);
            int orgID = (int) ((orgIDappID >> 16) & 0xFFFFFFFF);
            int appID = (int) (orgIDappID & 0xFFFF);

            // Get the AppProxy for this app from the database. If the
            // appID,orgID
            // is invalid, abort the test runner xlet startup
            AppID testAppID = new AppID(orgID, appID);
            AppProxy appProxy = adb.getAppProxy(testAppID);
            if (appProxy == null)
                throw new XletStateChangeException("Could not get exclusive event test xlet AppProxy!  " + testAppID);

            appProxy.addAppStateChangeEventListener(this);
            m_apps[i] = new ExclusiveEventTestXletInfo();
            m_apps[i].appID = testAppID;
            m_apps[i].appProxy = appProxy;
            m_apps[i].xletName = adb.getAppAttributes(testAppID).getName();
        }

        // Scene and InfoBox
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setVisible(false);
        m_infoBox.setBounds(236, 0, 353, 124);
        m_scene.add(m_infoBox);

        // Events
        EventManager em = (EventManager) EventManager.getInstance();
        UserEventRepository events = new UserEventRepository("Events");
        events.addKey(OCRcEvent.VK_UP);
        events.addKey(OCRcEvent.VK_DOWN);
        events.addKey(OCRcEvent.VK_PLAY);
        events.addKey(OCRcEvent.VK_PAUSE);
        events.addKey(OCRcEvent.VK_STOP);
        events.addKey(OCRcEvent.VK_CHANNEL_UP);
        events.addKey(OCRcEvent.VK_CHANNEL_DOWN);
        events.addKey(OCRcEvent.VK_VOLUME_UP);
        events.addKey(OCRcEvent.VK_VOLUME_DOWN);
        events.addAllNumericKeys();
        em.addUserEventListener(this, events);

        // Connect to AutoXlet framework
        m_axc = new AutoXletClient(this, ctx);
        m_test = m_axc.getTest();

        if (m_axc.isConnected())
            m_dbgLog = m_axc.getLogger();
        else
            m_dbgLog = new XletLogger();
    }

    public void startXlet() throws XletStateChangeException
    {
        // Setup the initial values for our display
        m_infoBox.setActiveXlet(m_apps[m_curAppIndex].xletName);
        m_infoBox.setActiveXletState(getStateString(m_apps[m_curAppIndex].appProxy.getState()));

        // Show the scene
        m_scene.show();
    }

    public void pauseXlet()
    {
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        // Stop test xlets
        for (int i = 0; i < m_apps.length; ++i)
            m_apps[i].appProxy.stop(true);

        m_scene.dispose();
    }

    // /////////////////////////////////////////////////////////////////////////////
    // EVENT HANDLING //
    // /////////////////////////////////////////////////////////////////////////////

    // AutoXlet Driveable interface implementation
    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
        UserEvent e = new UserEvent(new Object(), 0, KeyEvent.KEY_PRESSED, event.getKeyCode(), event.getModifiers(),
                event.getWhen());
        if (useMonitor)
        {
            m_eventMonitor.setTimeout(monitorTimeout);

            synchronized (m_eventMonitor)
            {
                userEventReceived(e);
                m_eventMonitor.waitForReady();
            }
        }
        else
            userEventReceived(e);
    }

    // Receives remote control key events regardless of focus
    public void userEventReceived(UserEvent e)
    {
        if (e.getType() != KeyEvent.KEY_PRESSED) return;

        ExclusiveEventTestXletInfo xlet = m_apps[m_curAppIndex];
        AppProxy curAppProxy = xlet.appProxy;
        int curAppState = curAppProxy.getState();

        // Dump test results (manual mode only)
        if (!m_axc.isConnected())
        {
            if (e.getCode() == OCRcEvent.VK_INFO)
            {
                m_dbgLog.log(m_test.getTestResult());
                m_test.getTestResult().clearTestResults();
                return;
            }
        }

        switch (e.getCode())
        {
            // Change the current active Xlet
            case OCRcEvent.VK_UP:
                if (m_curAppIndex == m_apps.length - 1)
                    setCurrentAppIndex(0);
                else
                    setCurrentAppIndex(m_curAppIndex + 1);
                updateCurrentAppState();
                break;

            case OCRcEvent.VK_DOWN:
                if (m_curAppIndex == 0)
                    setCurrentAppIndex(m_apps.length - 1);
                else
                    setCurrentAppIndex(m_curAppIndex - 1);
                updateCurrentAppState();
                break;

            case OCRcEvent.VK_PLAY:
                if (curAppState == AppProxy.NOT_LOADED)
                {
                    curAppProxy.start();
                }
                else if (curAppState == AppProxy.PAUSED)
                {
                    curAppProxy.resume();
                }

                updateCurrentAppState();
                break;

            case OCRcEvent.VK_PAUSE:
                if (curAppState == AppProxy.STARTED) curAppProxy.pause();

                updateCurrentAppState();
                break;

            case OCRcEvent.VK_STOP:
                if ((curAppState == AppProxy.STARTED) || (curAppState == AppProxy.PAUSED)) curAppProxy.stop(false);

                updateCurrentAppState();
                break;

            default:
                try
                {
                    xlet.control.handleKeyPress(e);
                }
                catch (RemoteException re)
                {
                    System.out.println("Remote exception thrown by " + xlet.xletName);
                }

                m_infoBox.repaint();
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // APP STATE CHANGE //
    // /////////////////////////////////////////////////////////////////////////////

    // This method receives events when one of our test apps changes state
    public void stateChange(AppStateChangeEvent evt)
    {
        // get the AppID of the event
        AppID app = evt.getAppID();
        ExclusiveEventTestXletInfo xlet = m_apps[getXletIndexByAppID(app)];

        // Check to see that the Xlet was successfully started
        if (evt.getToState() == AppProxy.STARTED)
        {
            m_dbgLog.log("Xlet \"" + xlet.xletName + "\" started.");
            m_infoBox.setActiveXletState(getStateString(evt.getToState()));
            m_infoBox.repaint();

            // Grab this Xlet's control via IXC
            String name = "/" + Integer.toHexString(xlet.appID.getOID()) + "/"
                    + Integer.toHexString(xlet.appID.getAID()) + "/" + "ExclusiveEventTestControl" + xlet.xletName;

            try
            {
                xlet.control = (ExclusiveEventTestControl) IxcRegistry.lookup(m_ctx, name);
            }
            catch (Exception e)
            {
                m_dbgLog.log("Could not access ExclusiveEventTestControl for \"" + xlet.xletName + "\"");
            }

            m_eventMonitor.notifyReady();
        }

        // Check to see that the Xlet was successfully paused
        if (evt.getToState() == AppProxy.PAUSED)
        {
            m_dbgLog.log("Xlet \"" + xlet.xletName + "\" paused.");
            m_infoBox.setActiveXletState(getStateString(evt.getToState()));
            m_infoBox.repaint();

            m_eventMonitor.notifyReady();
        }

        // Check to see that the Xlet was successfully stopped
        if (evt.getToState() == AppProxy.NOT_LOADED)
        {
            m_dbgLog.log("Xlet \"" + xlet.xletName + "\" stopped.");
            m_infoBox.setActiveXletState(getStateString(evt.getToState()));
            m_infoBox.repaint();

            m_eventMonitor.notifyReady();
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // MISC HELPER FUNCTIONS //
    // /////////////////////////////////////////////////////////////////////////////

    private void setCurrentAppIndex(int index)
    {
        m_curAppIndex = index;

        // Update display
        m_infoBox.setActiveXlet(m_apps[m_curAppIndex].xletName);
        m_infoBox.repaint();
    }

    private void updateCurrentAppState()
    {
        m_infoBox.setActiveXletState(getStateString(m_apps[m_curAppIndex].appProxy.getState()));
        m_infoBox.repaint();
    }

    // Translates an application state into a readable string
    private String getStateString(int appState)
    {
        switch (appState)
        {
            case AppProxy.DESTROYED:
                return "DESTROYED";
            case AppProxy.NOT_LOADED:
                return "NOT_LOADED";
            case AppProxy.PAUSED:
                return "PAUSED";
            case AppProxy.STARTED:
                return "STARTED";
        }
        return null;
    }

    // Returns the index in our apps array of the xlet described by the given
    // AppID
    private int getXletIndexByAppID(AppID appID)
    {
        for (int i = 0; i < m_apps.length; ++i)
            if (m_apps[i].appID.equals(appID)) return i;

        return -1;
    }

    // /////////////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES //
    // /////////////////////////////////////////////////////////////////////////////

    // Our main test runner UI component which displays the current test xlet
    // and component as well as the menu of input commands
    private class ExclusiveEventTestInfo extends Container
    {
        public ExclusiveEventTestInfo()
        {
            super();
            setBackground(Color.blue);
            setForeground(Color.white);
            setFont(new Font("tiresias", Font.PLAIN, 14));
        }

        public void setActiveXlet(String xletName)
        {
            m_activeXlet = xletName;
        }

        public void setActiveXletState(String state)
        {
            m_activeXletState = state;
        }

        public void paint(Graphics g)
        {
            g.drawString("<< UP/DOWN changes Xlet >>", 0, 40);
            g.drawString(m_activeXlet + " [" + m_activeXletState + "]", 20, 60);

            int x = 20, y = 80;
            int dy = 14;

            if (m_apps[m_curAppIndex].appProxy.getState() == AppProxy.PAUSED)
                g.drawString("(PLAY) Resume Xlet", x, y);
            else
                g.drawString("(PLAY) Start Xlet", x, y);
            y += dy;

            g.drawString("(PAUSE) Pause Xlet", x, y);
            y += dy;

            g.drawString("(STOP) Stop Xlet", x, y);
            y += dy;

            /*
             * // Provide inputs for dumping test results when // running in
             * manual mode if (!m_axc.isConnected()) { y += dy;
             * g.drawString("(0) Dump test results",x,y); y += dy; }
             */}

        private String m_activeXlet;

        private String m_activeXletState;
    }

    // Stores information about an Exclusive Event test Xlet
    private class ExclusiveEventTestXletInfo
    {
        public AppID appID;

        public AppProxy appProxy;

        public String xletName;

        public ExclusiveEventTestControl control;

    }

    // /////////////////////////////////////////////////////////////////////////////
    // DATA MEMBERS //
    // /////////////////////////////////////////////////////////////////////////////

    XletContext m_ctx;

    // AppProxy List
    ExclusiveEventTestXletInfo[] m_apps = null;

    int m_curAppIndex = 0; // Index into our XletInfo array

    int m_curComponentIndex = 0; // 0 = HScene, 1 = HText1, 2 = HText2

    HScene m_scene;

    ExclusiveEventTestInfo m_infoBox = new ExclusiveEventTestInfo();

    // AutoXlet stuff
    AutoXletClient m_axc = null;

    Logger m_dbgLog = null;

    Test m_test = null;

    Monitor m_eventMonitor = new Monitor();
}
