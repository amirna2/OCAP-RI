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

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;

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
import org.ocap.hardware.frontpanel.FrontPanelManager;

import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Monitor;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;

public class FPTestRunnerXlet implements Xlet, AppStateChangeEventListener, Driveable, UserEventListener
{
    // /////////////////////////////////////////////////////////////////////////////
    // XLET FUNCTIONS //
    // /////////////////////////////////////////////////////////////////////////////

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        m_ctx = ctx;

        AppsDatabase adb = AppsDatabase.getAppsDatabase();

        // Each argument is a complete 48-bit AppID integer (orgID,appID)
        // in hex (0x) string format. Each argument indicates the focus test
        // xlet to launch
        String[] args = (String[]) (ctx.getXletProperty(XletContext.ARGS));

        // Populate our app proxies array from the arguments
        m_apps = new FPTestXletInfo[args.length];
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
                throw new XletStateChangeException("Could not get focus test xlet AppProxy!  " + testAppID);

            appProxy.addAppStateChangeEventListener(this);
            m_apps[i] = new FPTestXletInfo();
            m_apps[i].appID = testAppID;
            m_apps[i].appProxy = appProxy;
            m_apps[i].xletName = adb.getAppAttributes(testAppID).getName();
            m_apps[i].appPriority = adb.getAppAttributes(testAppID).getPriority();
        }

        // Publish event handler via IXC
        try
        {
            IxcRegistry.bind(ctx, "FPTestEvents", m_testEvents);
        }
        catch (AlreadyBoundException e)
        {
            throw new XletStateChangeException("FocusTestEventHandler name already bound via IXC!");
        }

        // Scene and InfoBox
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_infoBox.setBounds(426, 0, 213, 480);
        m_scene.add(m_infoBox);
        m_scene.validate();

        // Events
        EventManager em = (EventManager) EventManager.getInstance();
        UserEventRepository events = new UserEventRepository("Events");
        events.addKey(OCRcEvent.VK_UP);
        events.addKey(OCRcEvent.VK_DOWN);
        events.addKey(OCRcEvent.VK_LEFT);
        events.addKey(OCRcEvent.VK_RIGHT);
        events.addKey(OCRcEvent.VK_STOP);
        events.addKey(OCRcEvent.VK_PLAY);
        events.addKey(OCRcEvent.VK_PAUSE);
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
        m_availableIndicators = m_fpmgr.getSupportedIndicators();

        // Setup the initial values for our display
        m_infoBox.setActiveIndicator(m_availableIndicators[m_curIndicatorIndex]);
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

        // Try to run tests after each key press
        runTests();
    }

    // Receives remote control key events regardless of focus
    public void userEventReceived(UserEvent e)
    {
        if (e.getType() != KeyEvent.KEY_PRESSED) return;

        FPTestXletInfo xlet = m_apps[m_curAppIndex];
        AppProxy curAppProxy = xlet.appProxy;
        int curAppState = curAppProxy.getState();

        // Run tests and dump test results (manual mode only)
        if (!m_axc.isConnected())
        {
            if (e.getCode() == OCRcEvent.VK_0)
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

            // Change the current active component within the current active
            // xlet
            // Each xlet always has only 3 components (2 text + scene)
            case OCRcEvent.VK_LEFT:
                if (m_curIndicatorIndex == 0)
                    setCurrentIndicatorIndex(m_availableIndicators.length - 1);
                else
                    setCurrentIndicatorIndex(m_curIndicatorIndex - 1);
                break;

            case OCRcEvent.VK_RIGHT:
                if (m_curIndicatorIndex == m_availableIndicators.length - 1)
                    setCurrentIndicatorIndex(0);
                else
                    setCurrentIndicatorIndex(m_curIndicatorIndex + 1);
                break;

            // Modify the current xlet's run state
            case OCRcEvent.VK_STOP:
                if (curAppState == AppProxy.PAUSED || curAppState == AppProxy.STARTED) curAppProxy.stop(true);

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

            case OCRcEvent.VK_1: // Reserve Indicator
                if (curAppState != AppProxy.STARTED) break;
                predictIndicatorReserve(m_apps[m_curAppIndex].appID, m_availableIndicators[m_curIndicatorIndex]);
                try
                {
                    xlet.control.reserveIndicator(m_availableIndicators[m_curIndicatorIndex]);
                }
                catch (RemoteException e1)
                {
                }
                break;

            case OCRcEvent.VK_2: // Release Indicator
                if (curAppState != AppProxy.STARTED) break;
                predictIndicatorRelease(m_apps[m_curAppIndex].appID, m_availableIndicators[m_curIndicatorIndex]);
                try
                {
                    xlet.control.releaseIndicator(m_availableIndicators[m_curIndicatorIndex]);
                }
                catch (RemoteException e1)
                {
                }
                runTests();
                break;

            case OCRcEvent.VK_3: // Toggle "willing to release"
                if (curAppState != AppProxy.STARTED) break;
                try
                {
                    xlet.control.toggleWillingToRelease();
                }
                catch (RemoteException e1)
                {
                }
                runTests();
                break;
        }

        m_infoBox.repaint();
    }

    // /////////////////////////////////////////////////////////////////////////////
    // APP STATE CHANGE //
    // /////////////////////////////////////////////////////////////////////////////

    // This method receives events when one of our test apps changes state
    public void stateChange(AppStateChangeEvent evt)
    {
        // get the AppID of the event
        AppID app = evt.getAppID();
        FPTestXletInfo xlet = m_apps[getXletIndexByAppID(app)];

        // Check to see that the Xlet was successfully started
        if (evt.getToState() == AppProxy.STARTED)
        {
            m_dbgLog.log("Xlet \"" + xlet.xletName + "\" started.");
            m_infoBox.setActiveXletState(getStateString(evt.getToState()));
            m_infoBox.repaint();

            // Grab this Xlet's control via IXC
            String name = "/" + Integer.toHexString(xlet.appID.getOID()) + "/"
                    + Integer.toHexString(xlet.appID.getAID()) + "/" + "FPTestControl" + xlet.xletName;
            try
            {
                xlet.control = (FPTestControl) IxcRegistry.lookup(m_ctx, name);
            }
            catch (Exception e)
            {
                m_dbgLog.log("Could not access FocusTestControl for \"" + xlet.xletName + "\"");
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

    private void setCurrentIndicatorIndex(int index)
    {
        m_curIndicatorIndex = index;

        // Update display
        m_infoBox.setActiveIndicator(m_availableIndicators[m_curIndicatorIndex]);
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
    // TEST RESULT PREDICTION FUNCTIONS //
    // /////////////////////////////////////////////////////////////////////////////
    private void predictIndicatorReserve(AppID appID, String indicator)
    {
        FPTestXletInfo requestingApp = m_apps[getXletIndexByAppID(appID)];

        // Find out if any app currently has this indicator reserved
        int appIndex = -1;
        for (int i = 0; i < m_apps.length; ++i)
        {
            if (m_apps[i].reservedIndicators.containsKey(indicator))
            {
                appIndex = i;
                break;
            }
        }

        // We found an app holding the previous reservation. If it was willing
        // to give up the reservation OR it has a lower priority than the
        // requesting app, then it will give up the resource. So remove it from
        // the prediction hashtable
        if (appIndex != -1)
        {
            FPTestXletInfo currentApp = m_apps[appIndex];

            // Is the previous reservation holder willing to release the
            // indicator?
            boolean willingToRelease = ((Boolean) currentApp.reservedIndicators.get(indicator)).booleanValue();

            if (willingToRelease || requestingApp.appPriority > currentApp.appPriority)
            {
                // Remove reservation from previous holder in our prediction
                // structure
                currentApp.prediction.removeElement(indicator);

                // Add reservation to new holder in our prediction structure
                requestingApp.prediction.addElement(indicator);
            }
        }
        else
        // indicator not currently reserved
        {
            // Add reservation to new holder in our prediction structure
            requestingApp.prediction.addElement(indicator);
        }
    }

    private void predictIndicatorRelease(AppID appID, String indicator)
    {
        // Indicator will be released, so remove it from our prediction
        // structure
        m_apps[getXletIndexByAppID(appID)].prediction.removeElement(indicator);
    }

    // /////////////////////////////////////////////////////////////////////////////
    // TEST EVALUATION //
    // /////////////////////////////////////////////////////////////////////////////

    // This functions runs assert tests against our set of predictions
    private void runTests()
    {
        // We must check (for each app) that the list of currently reserved
        // indicators and the list in our prediction structure have all the
        // same items

        for (int i = 0; i < m_apps.length; ++i)
        {
            FPTestXletInfo xlet = m_apps[i];

            // Does the prediction structure contain each indicator from the
            // current reservation list?
            for (Enumeration e = xlet.reservedIndicators.keys(); e.hasMoreElements();)
            {
                String indicator = (String) e.nextElement();
                m_test.assertTrue("Indicator " + indicator + " was reserved by " + xlet.xletName + " (" + xlet.appID
                        + "), but should not have been!", xlet.prediction.contains(indicator));
            }

            // Does the current reservation list contain each indicator from the
            // predication structure?
            for (int j = 0; j < xlet.prediction.size(); ++j)
            {
                String indicator = (String) xlet.prediction.elementAt(j);
                m_test.assertTrue("Indicator " + indicator + " was not reserved by " + xlet.xletName + " ("
                        + xlet.appID + "), but should have been!", xlet.reservedIndicators.containsKey(indicator));
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES //
    // /////////////////////////////////////////////////////////////////////////////

    // Published via IXC. Allows test xlets to report their window events to
    // the test runner
    private class FPTestEventHandler implements FPTestEvents
    {
        public void indicatorReserved(int orgID, int appID, String indicator, boolean willingToRelease)
                throws RemoteException
        {
            FPTestXletInfo xletInfo = m_apps[getXletIndexByAppID(new AppID(orgID, appID))];
            xletInfo.reservedIndicators.put(indicator, new Boolean(willingToRelease));
        }

        public void indicatorReleased(int orgID, int appID, String indicator) throws RemoteException
        {
            FPTestXletInfo xletInfo = m_apps[getXletIndexByAppID(new AppID(orgID, appID))];
            xletInfo.reservedIndicators.remove(indicator);
        }
    }

    // Our main test runner UI component which displays the current test xlet
    // and component as well as the menu of input commands
    private class FPTestUI extends Container
    {
        public FPTestUI()
        {
            super();
            setBackground(Color.black);
            setForeground(Color.white);
            setFont(new Font("tiresias", Font.PLAIN, 12));
        }

        public void setActiveXlet(String xletName)
        {
            m_activeXlet = xletName;
        }

        public void setActiveXletState(String state)
        {
            m_activeXletState = state;
        }

        public void setActiveIndicator(String compName)
        {
            m_activeIndicator = compName;
        }

        public void paint(Graphics g)
        {
            int x = 10, y = 40;
            int dy = 14;

            g.drawString("<< UP/DOWN >>", x, y);
            x += 7;
            y += dy;
            g.drawString("Change Current Xlet", x, y);
            x -= 7;
            y += dy;
            g.drawString("<< LEFT/RIGHT >>", x, y);
            x += 7;
            y += dy;
            g.drawString("Change FP Component", x, y);
            x += 10;
            y += 40;
            g.drawString(m_activeXlet + " [" + m_activeXletState + "]", x, y);
            y += dy;
            g.drawString(m_activeIndicator, x, y);

            x = 25;
            y = 175;

            if (m_apps[m_curAppIndex].appProxy.getState() == AppProxy.PAUSED)
                g.drawString("(PLAY) Resume Xlet", x, y);
            else
                g.drawString("(PLAY) Start Xlet", x, y);
            y += dy;

            g.drawString("(STOP) Stop Xlet", x, y);
            y += dy;
            g.drawString("(PAUSE) Pause Xlet", x, y);
            y += dy;
            y += dy;

            // Only draw test control options when xlet is running
            if (m_apps[m_curAppIndex].appProxy.getState() == AppProxy.STARTED)
            {
                g.drawString("(1) Reserve Indicator", x, y);
                y += dy;
                g.drawString("(2) Release Indicator", x, y);
                y += dy;
                g.drawString("(3) Toggle Willing To Release", x, y);
                y += dy;
            }

            // Provide inputs for running tests and dumping test results when
            // running in manual mode
            if (!m_axc.isConnected())
            {
                y += dy;

                g.drawString("(0) Dump test results", x, y);
                y += dy;
            }
        }

        private String m_activeXlet;

        private String m_activeXletState;

        private String m_activeIndicator;
    }

    // Stores information about a focus test Xlet
    private class FPTestXletInfo
    {
        public AppID appID;

        public AppProxy appProxy;

        public int appPriority;

        public String xletName;

        public FPTestControl control;

        // Hashtable maps indicator names to "willing to release" flag
        public Hashtable reservedIndicators = new Hashtable();

        // Holds the list of indicator names that we predict this app should
        // have after a specific action takes place
        public Vector prediction = new Vector();

        // Equivalence operator used for easy Vector add/remove
        public boolean equals(Object other)
        {
            // Validate the cast
            FPTestXletInfo otherFPTXI;
            try
            {
                otherFPTXI = (FPTestXletInfo) other;
            }
            catch (ClassCastException e)
            {
                return false;
            }

            // Equal if AppIDs are equal
            return otherFPTXI.appID.equals(this.appID);
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // DATA MEMBERS //
    // /////////////////////////////////////////////////////////////////////////////

    XletContext m_ctx;

    FrontPanelManager m_fpmgr = FrontPanelManager.getInstance();

    String[] m_availableIndicators = null;

    FPTestEventHandler m_testEvents = new FPTestEventHandler();

    // AppProxy List
    FPTestXletInfo[] m_apps = null;

    int m_curAppIndex = 0; // Index into our FocusTestXletInfo array

    int m_curIndicatorIndex = 0;

    HScene m_scene;

    FPTestUI m_infoBox = new FPTestUI();

    // AutoXlet stuff
    AutoXletClient m_axc = null;

    Logger m_dbgLog = null;

    Test m_test = null;

    Monitor m_eventMonitor = new Monitor();
}
