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

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
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
import org.havi.ui.HScreen;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;
import org.havi.ui.HScreenRectangle;
import org.ocap.application.OcapAppAttributes;
import org.ocap.event.EventManager;
import org.ocap.ui.event.OCRcEvent;
import org.ocap.ui.HSceneBinding;
import org.ocap.ui.HSceneChangeRequestHandler;
import org.ocap.ui.HSceneManager;

import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Monitor;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;

public class HSMTestRunnerXlet implements Xlet, AppStateChangeEventListener, Driveable, UserEventListener,
        HSceneChangeRequestHandler
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

        m_appID = new AppID((int) (Long.parseLong((String) (ctx.getXletProperty("dvb.org.id")), 16)),
                (int) (Long.parseLong((String) (ctx.getXletProperty("dvb.app.id")), 16)));

        int testXletNum = 0;
        for (int i = 0; i < args.length; i++) // count the number of testXlets
        {
            if (args[i].substring(0, args[i].indexOf("=")).indexOf("testXlet") != -1) testXletNum++;
        }

        // Populate our app proxies array from the arguments
        m_apps = new HSMTestXletInfo[testXletNum];
        for (int i = 0; i < args.length; ++i)
        {
            String argName = args[i].substring(0, args[i].indexOf("="));
            String argVal = args[i].substring(args[i].indexOf("=") + 1);

            if (argName.indexOf("testXlet") != -1)
            {
                long orgIDappID = Long.parseLong(argVal.substring(2), 16);
                int orgID = (int) ((orgIDappID >> 16) & 0xFFFFFFFF);
                int appID = (int) (orgIDappID & 0xFFFF);

                // Get the AppProxy for this app from the database. If the
                // appID, orgID is invalid, abort the test runner xlet startup
                AppID testAppID = new AppID(orgID, appID);
                AppProxy appProxy = adb.getAppProxy(testAppID);
                if (appProxy == null)
                    throw new XletStateChangeException("Could not get focus test xlet AppProxy!  " + testAppID);

                appProxy.addAppStateChangeEventListener(this);
                m_apps[i] = new HSMTestXletInfo();
                m_apps[i].appID = testAppID;
                m_apps[i].appProxy = appProxy;
                m_apps[i].xletName = adb.getAppAttributes(testAppID).getName();
                m_apps[i].appPriority = adb.getAppAttributes(testAppID).getPriority();
            }
            else
            // possible argNames are: noMove, noShow, or noOrder
            {
                Vector aIds = new Vector();

                long id;
                int oid;
                int aid;

                String remainingArgVal = argVal;
                int aidIdx = remainingArgVal.indexOf(",");
                while (aidIdx != -1)
                {
                    id = Long.parseLong(remainingArgVal.substring(0, aidIdx).substring(2), 16);
                    oid = (int) ((id >> 16) & 0xFFFFFFFF);
                    aid = (int) (id & 0xFFFF);
                    aIds.addElement(new AppID(oid, aid));

                    remainingArgVal = argVal.substring(aidIdx + 1, remainingArgVal.length());
                    aidIdx = remainingArgVal.indexOf(",");
                } // aidIdx==-1, then only 1 aid to read
                id = Long.parseLong(remainingArgVal.substring(2), 16);
                oid = (int) ((id >> 16) & 0xFFFFFFFF);
                aid = (int) (id & 0xFFFF);
                aIds.addElement(new AppID(oid, aid));

                if (argName.indexOf("noShow") != -1)
                {
                    m_noShowIDs = aIds;
                    continue;
                }
                if (argName.indexOf("noOrder") != -1)
                {
                    m_noOrderIDs = aIds;
                    continue;
                }
                if (argName.indexOf("noMove") != -1)
                {
                    m_noMoveIDs = aIds;
                    continue;
                }
            }
        }

        // Publish event handler via IXC
        try
        {
            IxcRegistry.bind(ctx, "HSMTestEvents", m_testEvents);
        }
        catch (AlreadyBoundException e)
        {
            throw new XletStateChangeException("HSceneManagerTestEventHandler name already bound via IXC!");
        }

        Dimension d = HScreen.getDefaultHScreen()
                .getDefaultHGraphicsDevice()
                .getCurrentConfiguration()
                .getPixelResolution();
        System.out.println("CURRENT SCREEN DIMENSIONS -- " + d.width + "," + d.height);

        // Scene and InfoBox
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setBounds(426, 0, 213, 480);
        m_infoBox.setBounds(0, 0, 213, 480);
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
        events.addKey(OCRcEvent.VK_CHANNEL_UP);
        events.addKey(OCRcEvent.VK_CHANNEL_DOWN);
        events.addAllNumericKeys();
        em.addUserEventListener(this, events);

        // Register this xlet as an HSceneChangeRequestHandler
        HSceneManager.getInstance().setHSceneChangeRequestHandler(this);

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

        // unregister as a HSceneChangeRequestHandler before dying.
        HSceneManager.getInstance().setHSceneChangeRequestHandler(null);
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

        HSMTestXletInfo xlet = m_apps[m_curAppIndex];
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
            else if (e.getCode() == OCRcEvent.VK_8)
            {
                runTests();
                return;
            }
        }

        switch (e.getCode())
        {
            // Change the current active Xlet
            case OCRcEvent.VK_CHANNEL_UP:
                if (m_curAppIndex == m_apps.length - 1)
                    setCurrentAppIndex(0);
                else
                    setCurrentAppIndex(m_curAppIndex + 1);
                updateCurrentAppState();
                break;

            case OCRcEvent.VK_CHANNEL_DOWN:
                if (m_curAppIndex == 0)
                    setCurrentAppIndex(m_apps.length - 1);
                else
                    setCurrentAppIndex(m_curAppIndex - 1);
                updateCurrentAppState();
                break;

            // Commands to change xlet size and position
            case OCRcEvent.VK_UP:
                try
                {
                    xlet.control.moveYMinus();
                }
                catch (RemoteException ex)
                {
                }
                break;

            case OCRcEvent.VK_DOWN:
                try
                {
                    xlet.control.moveYPlus();
                }
                catch (RemoteException ex)
                {
                }
                break;

            case OCRcEvent.VK_LEFT:
                try
                {
                    xlet.control.moveXMinus();
                }
                catch (RemoteException ex)
                {
                }
                break;

            case OCRcEvent.VK_RIGHT:
                try
                {
                    xlet.control.moveXPlus();
                }
                catch (RemoteException ex)
                {
                }
                break;

            // Modify the current xlet's run state
            case OCRcEvent.VK_STOP:
                if (curAppState == AppProxy.PAUSED || curAppState == AppProxy.STARTED) curAppProxy.stop(true);

                updateCurrentAppState();
                if (!(m_noShowIDs.contains(xlet.appID)))
                {
                    m_zOrderPrediction.removeElement(xlet.appID);
                }
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

                if (!(m_noShowIDs.contains(xlet.appID)))
                {
                    if (m_noOrderIDs.contains(xlet.appID))
                    {
                        m_zOrderPrediction.addElement(xlet.appID);
                    }
                    else
                    {
                        m_zOrderPrediction.insertElementAt(xlet.appID, 0);
                    }
                }

                break;

            case OCRcEvent.VK_PAUSE:
                if (curAppState == AppProxy.STARTED) curAppProxy.pause();

                updateCurrentAppState();
                break;

            case OCRcEvent.VK_1: // Toggle move type
                try
                {
                    xlet.control.toggleMoveType();
                }
                catch (RemoteException ex)
                {
                }
                break;

            case OCRcEvent.VK_2: // Pop-to-front
                try
                {
                    xlet.control.popToFront();
                }
                catch (RemoteException ex)
                {
                }

                if (!(m_noShowIDs.contains(xlet.appID)) && !(m_noOrderIDs.contains(xlet.appID)))
                {
                    m_zOrderPrediction.removeElement(xlet.appID);
                    m_zOrderPrediction.insertElementAt(xlet.appID, 0);
                }
                runTests(); // run z-order test after a test xlet has
                            // pop-to-front
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
        HSMTestXletInfo xlet = m_apps[getXletIndexByAppID(app)];

        // Check to see that the Xlet was successfully started
        if (evt.getToState() == AppProxy.STARTED)
        {
            m_dbgLog.log("Xlet \"" + xlet.xletName + "\" started.");
            m_infoBox.setActiveXletState(getStateString(evt.getToState()));
            m_infoBox.repaint();

            // Grab this Xlet's control via IXC
            String name = "/" + Integer.toHexString(xlet.appID.getOID()) + "/"
                    + Integer.toHexString(xlet.appID.getAID()) + "/" + "HSMTestControl" + xlet.xletName;
            try
            {
                xlet.control = (HSMTestControl) IxcRegistry.lookup(m_ctx, name);
            }
            catch (Exception e)
            {
                m_dbgLog.log("Could not access HSMTestControl for \"" + xlet.xletName + "\"");
            }

            runTests(); // run z-order test after a test xlet has started up
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
    // TEST RESULT PREDICTION FUNCTIONS //
    // /////////////////////////////////////////////////////////////////////////////

    // /////////////////////////////////////////////////////////////////////////////
    // TEST EVALUATION //
    // /////////////////////////////////////////////////////////////////////////////

    // This functions runs assert tests against our set of predictions
    private void runTests()
    {
        // We must check (for each app) that the list of currently reserved
        // indicators and the list in our prediction structure have all the
        // same items

        Vector zOrderResults = new Vector();
        OcapAppAttributes[] attr = HSceneManager.getHSceneOrder();
        for (int i = 0; i < attr.length; i++)
        {
            for (int j = 0; j < m_apps.length; j++)
            {
                if (attr[i].getIdentifier().equals(m_apps[j].appID))
                {
                    zOrderResults.addElement(attr[i].getIdentifier());
                }
            }
        }

        m_test.assertTrue("zOrder of test xlets are incorrect", zOrderResults.equals(m_zOrderPrediction));
        if (!(zOrderResults.equals(m_zOrderPrediction)))
        {
            m_dbgLog.log("zOrder of testXlets do not match:");
            for (int i = 0; i < m_zOrderPrediction.size(); ++i)
            {
                m_dbgLog.log("\tzOrder " + i + " is " + zOrderResults.elementAt(i).toString()
                        + " instead of the expected " + m_zOrderPrediction.elementAt(i).toString());
            }
        }
        else
        {
            m_dbgLog.log("zOrder of testXlets is as expected:");
            for (int i = 0; i < zOrderResults.size(); ++i)
            {
                m_dbgLog.log("\tzOrder " + i + " is " + zOrderResults.elementAt(i).toString());
            }
        }
    }

    // Defined in HSceneChangeRequestHandler
    public boolean testShow(HSceneBinding show, HSceneBinding[] currentScenes)
    {
        if (m_noShowIDs.contains(show.getAppAttributes().getIdentifier())) return false;

        // Don't allow a scene to be shown that overlaps with the runner scene
        return checkBounds(show, currentScenes);
    }

    // Defined in HSceneChangeRequestHandler
    public boolean testMove(HSceneBinding move, HSceneBinding[] currentScenes)
    {
        if (m_noMoveIDs.contains(move.getAppAttributes().getIdentifier())) return false;

        return checkBounds(move, currentScenes);
    }

    // Defined in HSceneChangeRequestHandler
    public boolean testOrder(HSceneBinding[] scenes, int currentPos, int newPos)
    {
        if (m_noOrderIDs.contains(scenes[currentPos].getAppAttributes().getIdentifier()) && newPos == 0) return false;

        return true;
    }

    // Ensure that the proposed scene position does not exit the defined screen
    // bounds and that it does not collide with the test runner's scene bounds
    private boolean checkBounds(HSceneBinding position, HSceneBinding[] allScenes)
    {
        // The runner can be positioned wherever it likes
        if (position.getAppAttributes().getIdentifier().equals(m_appID)) return true;

        // Find the index of the scene associated with the test runner
        HScreenRectangle runner = null;
        for (int i = 0; i < allScenes.length; ++i)
        {
            if (allScenes[i].getAppAttributes().getIdentifier().equals(m_appID)) runner = allScenes[i].getRectangle();
        }

        // Ensure the the new scene position abides by the rules of placement
        HScreenRectangle scene = position.getRectangle();

        // Check that scene is within the screen bounds
        if (scene.x < 0 || scene.x + scene.width > 1 || scene.y < 0 || scene.y + scene.height > 1) return false;

        // Ensure that the scene does not overlap with the runner scene
        if (scene.x > runner.x + runner.width || scene.x + scene.width < runner.x || scene.y > runner.y + runner.height
                || scene.y + scene.height < runner.y) return true;

        return false;
    }

    // /////////////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES //
    // /////////////////////////////////////////////////////////////////////////////

    // Published via IXC. Allows test xlets to report their window events to
    // the test runner
    private class HSMTestEventHandler implements HSMTestEvents
    {
        public void indicatorReserved(int orgID, int appID, String indicator, boolean willingToRelease)
                throws RemoteException
        {
        }

        public void indicatorReleased(int orgID, int appID, String indicator) throws RemoteException
        {
        }
    }

    // Our main test runner UI component which displays the current test xlet
    // and component as well as the menu of input commands
    private class HSMTestUI extends Container
    {
        public HSMTestUI()
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

        public void paint(Graphics g)
        {
            Rectangle bounds = getBounds();
            g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 15, 15);

            int x = 10, y = 40;
            int dy = 14;

            g.drawString("<< CH UP / CH DOWN >>", x, y);
            x += 7;
            y += dy;
            g.drawString("Change Current Xlet", x, y);
            x += 10;
            y += 40;
            g.drawString(m_activeXlet + " [" + m_activeXletState + "]", x, y);
            y += dy;

            x = 25;
            y = 165;

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
                g.drawString("  (1) Toggle Move Type", x, y);
                y += dy;
                g.drawString("  (2) Pop To Front", x, y);
                y += dy;
                g.drawString("(UP) Decrease Y Size/Pos", x, y);
                y += dy;
                g.drawString("(DN) Increase Y Size/Pos", x, y);
                y += dy;
                g.drawString("(LT) Decrease X Size/Pos", x, y);
                y += dy;
                g.drawString("(RT) Increase X Size/Pos", x, y);
                y += dy;
            }

            x = 10;
            y += 15;

            // Draw Scene Order and Position of Current App's scene
            g.drawString("Scene Z-Order:", x, y);
            y += dy;
            x += 15;
            OcapAppAttributes[] attr = HSceneManager.getHSceneOrder();
            for (int i = 0; i < attr.length; ++i)
            {
                g.drawString(attr[i].getName(), x, y);
                y += dy;
            }
        }

        private String m_activeXlet;

        private String m_activeXletState;
    }

    // Stores information about a focus test Xlet
    private class HSMTestXletInfo
    {
        public AppID appID;

        public AppProxy appProxy;

        public int appPriority;

        public String xletName;

        public HSMTestControl control;

        // Hashtable maps indicator names to "willing to release" flag
        public Hashtable reservedIndicators = new Hashtable();

        // Holds the list of indicator names that we predict this app should
        // have after a specific action takes place
        public Vector prediction = new Vector();

        // Equivalence operator used for easy Vector add/remove
        public boolean equals(Object other)
        {
            // Validate the cast
            HSMTestXletInfo otherFPTXI;
            try
            {
                otherFPTXI = (HSMTestXletInfo) other;
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

    AppID m_appID = null;

    HSMTestEventHandler m_testEvents = new HSMTestEventHandler();

    // AppProxy List
    HSMTestXletInfo[] m_apps = null;

    int m_curAppIndex = 0; // Index into our FocusTestXletInfo array

    HScene m_scene;

    HSMTestUI m_infoBox = new HSMTestUI();

    // AutoXlet stuff
    AutoXletClient m_axc = null;

    Logger m_dbgLog = null;

    Test m_test = null;

    Monitor m_eventMonitor = new Monitor();

    Vector m_noShowIDs = new Vector();

    Vector m_noMoveIDs = new Vector();

    Vector m_noOrderIDs = new Vector();

    Vector m_zOrderPrediction = new Vector();

}
