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

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.util.Vector;

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

public class FocusTestRunnerXlet implements Xlet, AppStateChangeEventListener, Driveable, UserEventListener
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
        m_apps = new FocusTestXletInfo[args.length];
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
            m_apps[i] = new FocusTestXletInfo();
            m_apps[i].appID = testAppID;
            m_apps[i].appProxy = appProxy;
            m_apps[i].xletName = adb.getAppAttributes(testAppID).getName();
        }

        // Publish event handler via IXC
        try
        {
            IxcRegistry.bind(ctx, "FocusTestEventHandler", m_testEvents);
        }
        catch (AlreadyBoundException e)
        {
            throw new XletStateChangeException("FocusTestEventHandler name already bound via IXC!");
        }

        // Scene and InfoBox
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_scene.setVisible(false);
        m_infoBox.setBounds(386, 0, 213, 480);
        m_scene.add(m_infoBox);

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
        System.out.println("Runner xlet fully initialized");
    }

    public void startXlet() throws XletStateChangeException
    {
        // Setup the initial values for our display
        m_infoBox.setActiveComponent(getComponentName(m_curComponentIndex));
        m_infoBox.setActiveXlet(m_apps[m_curAppIndex].xletName);
        m_infoBox.setActiveXletState(getStateString(m_apps[m_curAppIndex].appProxy.getState()));

        // Show the scene
        m_scene.show();
        System.out.println("Runner xlet fully started");
    }

    public void pauseXlet()
    {
    }

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        // Stop test xlets
        System.out.println("Runner xlet being stopped. Stopping other xlets");
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

        // Try to run tests after each key press. Wait for any asynchronous
        // events (like windowActivated) to come in before doing test
        // comparisons.
        try
        {
            Thread.sleep(250);
        }
        catch (InterruptedException except)
        {
        };
        runTests();
    }

    // Receives remote control key events regardless of focus
    public void userEventReceived(UserEvent e)
    {
        if (e.getType() != KeyEvent.KEY_PRESSED) return;

        FocusTestXletInfo xlet = m_apps[m_curAppIndex];
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

        // Before handling each event, clear out our event result variables
        m_testEvents.clearTestVariables();
        m_prediction = null;

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
                if (m_curComponentIndex == 0)
                    setCurrentComponentIndex(2);
                else
                    setCurrentComponentIndex(m_curComponentIndex - 1);
                break;

            case OCRcEvent.VK_RIGHT:
                if (m_curComponentIndex == 2)
                    setCurrentComponentIndex(0);
                else
                    setCurrentComponentIndex(m_curComponentIndex + 1);
                break;

            // Modify the current xlet's run state
            case OCRcEvent.VK_STOP:
                m_prediction = predictAppPauseStop();

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
                    m_prediction = predictAppResume();
                    curAppProxy.resume();
                }

                updateCurrentAppState();
                break;

            case OCRcEvent.VK_PAUSE:
                m_prediction = predictAppPauseStop();
                if (curAppState == AppProxy.STARTED) curAppProxy.pause();

                updateCurrentAppState();
                break;

            // Component Actions
            case OCRcEvent.VK_1: // requestFocus()
                if (curAppState != AppProxy.STARTED) break;
                m_prediction = predictRequestFocus();
                try
                {
                    xlet.control.requestFocus(m_curComponentIndex);
                }
                catch (RemoteException e1)
                {
                }
                break;

            // Scene actions
            case OCRcEvent.VK_2: // setVisible(true)
                if (m_curComponentIndex == 0 && curAppState == AppProxy.STARTED && !m_activableWindows.contains(xlet))
                {
                    m_prediction = predictSetVisible(true);
                    try
                    {
                        xlet.control.setVisible(m_curComponentIndex, true);
                        xlet.isVisible = true;
                        updateActivableList(xlet);
                    }
                    catch (RemoteException e1)
                    {
                    }
                }
                break;

            case OCRcEvent.VK_3: // setVisible(false)
                if (m_curComponentIndex == 0 && curAppState == AppProxy.STARTED)
                {
                    m_prediction = predictSetVisible(false);
                    try
                    {
                        xlet.control.setVisible(m_curComponentIndex, false);
                        xlet.isVisible = false;
                        updateActivableList(xlet);
                    }
                    catch (RemoteException e1)
                    {
                    }
                }
                break;

            case OCRcEvent.VK_4: // setActive(true)
                if (m_curComponentIndex == 0 && curAppState == AppProxy.STARTED)
                {
                    m_prediction = predictSetActive(true);
                    try
                    {
                        xlet.control.setActive(true);
                        xlet.isActive = true;
                        updateActivableList(xlet);
                    }
                    catch (RemoteException e1)
                    {
                    }
                }
                break;

            case OCRcEvent.VK_5: // setActive(false)
                if (m_curComponentIndex == 0 && curAppState == AppProxy.STARTED)
                {
                    m_prediction = predictSetActive(false);
                    try
                    {
                        xlet.control.setActive(false);
                        xlet.isActive = false;
                        updateActivableList(xlet);
                    }
                    catch (RemoteException e1)
                    {
                    }
                }
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
        FocusTestXletInfo xlet = m_apps[getXletIndexByAppID(app)];

        // Check to see that the Xlet was successfully started
        if (evt.getToState() == AppProxy.STARTED)
        {
            m_dbgLog.log("Xlet \"" + xlet.xletName + "\" started.");
            m_infoBox.setActiveXletState(getStateString(evt.getToState()));
            m_infoBox.repaint();

            // Grab this Xlet's control via IXC
            String name = "/" + Integer.toHexString(xlet.appID.getOID()) + "/"
                    + Integer.toHexString(xlet.appID.getAID()) + "/" + "FocusTestControl" + xlet.xletName;
            try
            {
                xlet.control = (FocusTestControl) IxcRegistry.lookup(m_ctx, name);
            }
            catch (Exception e)
            {
                m_dbgLog.log("Could not access FocusTestControl for \"" + xlet.xletName + "\"");
            }

            // If activable, this xlet will be inserted at the end of the list
            if (xlet.isActive && xlet.isVisible && xlet.componentWithFocus != -1) m_activableWindows.addElement(xlet);

            m_eventMonitor.notifyReady();
        }

        // Check to see that the Xlet was successfully paused
        if (evt.getToState() == AppProxy.PAUSED)
        {
            m_dbgLog.log("Xlet \"" + xlet.xletName + "\" paused.");
            m_infoBox.setActiveXletState(getStateString(evt.getToState()));
            m_infoBox.repaint();

            // Remove from activable list
            m_activableWindows.removeElement(xlet);

            m_eventMonitor.notifyReady();
        }

        // Check to see that the Xlet was successfully stopped
        if (evt.getToState() == AppProxy.NOT_LOADED)
        {
            m_dbgLog.log("Xlet \"" + xlet.xletName + "\" stopped.");
            m_infoBox.setActiveXletState(getStateString(evt.getToState()));
            m_infoBox.repaint();

            // Remove from activable list and clear component with focus
            m_activableWindows.removeElement(xlet);
            xlet.componentWithFocus = -1;

            m_eventMonitor.notifyReady();
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // MISC HELPER FUNCTIONS //
    // /////////////////////////////////////////////////////////////////////////////

    // Checks to see if the given xlet is activable. If it is activable and
    // not already in the activable list, it is added to the end of the list.
    // If the xlet is not activable, make sure that it is removed from the list
    private void updateActivableList(FocusTestXletInfo xlet)
    {
        if (!m_activableWindows.contains(xlet) && xlet.isVisible && xlet.isActive && xlet.componentWithFocus != -1)
        {
            m_activableWindows.addElement(xlet);
        }
        else if (m_activableWindows.contains(xlet)
                && (!xlet.isVisible || !xlet.isActive || xlet.componentWithFocus == -1))
        {
            m_activableWindows.removeElement(xlet);
        }
    }

    private void setCurrentAppIndex(int index)
    {
        m_curAppIndex = index;

        // Update display
        m_infoBox.setActiveXlet(m_apps[m_curAppIndex].xletName);
        m_infoBox.repaint();
    }

    private void setCurrentComponentIndex(int index)
    {
        m_curComponentIndex = index;

        // Update display
        m_infoBox.setActiveComponent(getComponentName(m_curComponentIndex));
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

    // Translates a component index into a readable string
    private String getComponentName(int compIndex)
    {
        switch (compIndex)
        {
            case 0:
                return "Scene";
            case 1:
                return "Text1";
            case 2:
                return "Text2";
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
    private FocusTestPrediction predictAppResume()
    {
        FocusTestPrediction ftp = new FocusTestPrediction();
        FocusTestXletInfo xlet = m_apps[m_curAppIndex];

        // When resuming an xlet, its scene will be added to the end of the
        // activable list if it is visible, active, and previously had a
        // component
        // that received focus
        if (xlet.isVisible && xlet.isActive && xlet.componentWithFocus != -1)
            ftp.appAddedToEndOfActivableList = xlet.appID;

        return ftp;
    }

    private FocusTestPrediction predictAppPauseStop()
    {
        FocusTestPrediction ftp = new FocusTestPrediction();
        FocusTestXletInfo xlet = m_apps[m_curAppIndex];

        // App will be removed from the activable window list (if present)
        if (m_activableWindows.contains(xlet))
        {
            ftp.appRemovedFromActivableList = xlet.appID;

            // App window will be deactivated (if active) and next app in the
            // list will receive focus
            if (xlet.isActivated)
            {
                ftp.appWindowDeactivated = xlet.appID;
                ftp.focusLostAppID.addElement(xlet.appID);
                ftp.focusLostComponentIndex.addElement(new Integer(xlet.componentWithFocus));

                if (m_activableWindows.size() > 1)
                {
                    FocusTestXletInfo newActiveXlet = (FocusTestXletInfo) m_activableWindows.elementAt(1);

                    ftp.appWindowActivated = newActiveXlet.appID;
                    ftp.focusGainedAppID = newActiveXlet.appID;
                    ftp.focusGainedComponentIndex = newActiveXlet.componentWithFocus;
                }
            }
        }

        return ftp;
    }

    private FocusTestPrediction predictRequestFocus()
    {
        FocusTestPrediction ftp = new FocusTestPrediction();
        FocusTestXletInfo xlet = m_apps[m_curAppIndex];

        // First, handle the case where this scene is already the currently
        // active scene. In this case, the previously focused component will
        // lose focus and the new component will gain focus.
        if (xlet.isActivated && xlet.componentWithFocus != m_curComponentIndex)
        {
            ftp.focusLostAppID.addElement(xlet.appID);
            ftp.focusLostComponentIndex.addElement(new Integer(xlet.componentWithFocus));
            ftp.focusGainedAppID = xlet.appID;
            ftp.focusGainedComponentIndex = m_curComponentIndex;
        }

        // Next, handle the case where this scene is not the currently activated
        // scene.
        else if (!xlet.isActivated && xlet.isVisible && xlet.isActive)
        {
            // If there is another activated window at the front of the list, it
            // will be deactivated and its focused component will lose focus
            if (!m_activableWindows.isEmpty())
            {
                FocusTestXletInfo frontOfList = (FocusTestXletInfo) m_activableWindows.elementAt(0);

                if (frontOfList.isActivated)
                {
                    ftp.focusLostAppID.addElement(frontOfList.appID);
                    ftp.focusLostComponentIndex.addElement(new Integer(frontOfList.componentWithFocus));
                    ftp.appWindowDeactivated = frontOfList.appID;
                }
            }

            // If this scene has previously had the focus and the component is
            // different
            // than the one currently requesting focus, the previous component
            // will lost
            // focus
            if (xlet.componentWithFocus != -1 && xlet.componentWithFocus != m_curComponentIndex)
            {
                ftp.focusLostAppID.addElement(xlet.appID);
                ftp.focusLostComponentIndex.addElement(new Integer(xlet.componentWithFocus));
            }

            // Window will gain focus, move to front of list, and be activated
            ftp.focusGainedAppID = xlet.appID;
            ftp.focusGainedComponentIndex = m_curComponentIndex;
            ftp.appMovedToFrontOfActivableList = xlet.appID;
            ftp.appWindowActivated = xlet.appID;
        }

        return ftp;
    }

    private FocusTestPrediction predictSetVisible(boolean visible)
    {
        FocusTestPrediction ftp = new FocusTestPrediction();
        FocusTestXletInfo xlet = m_apps[m_curAppIndex];

        // If this scene is becoming visible it may be added to the activable
        // list
        // if it had previously received focus and is active
        if (visible)
        {
            if (xlet.isActive && xlet.componentWithFocus != -1) ftp.appAddedToEndOfActivableList = xlet.appID;
        }
        else
        {
            // If this scene is becoming invisible, it will be removed from the
            // activable list.
            ftp.appRemovedFromActivableList = xlet.appID;

            // If the scene was also activated, its focused component will lose
            // focus, the scene will be deactivated, and the next scene in the
            // list
            // will gain focus
            if (xlet.isActivated)
            {
                ftp.focusLostAppID.addElement(xlet.appID);
                ftp.focusLostComponentIndex.addElement(new Integer(xlet.componentWithFocus));
                ftp.appWindowDeactivated = xlet.appID;

                if (m_activableWindows.size() > 1)
                {
                    FocusTestXletInfo newActiveXlet = (FocusTestXletInfo) m_activableWindows.elementAt(1);

                    ftp.appWindowActivated = newActiveXlet.appID;
                    ftp.focusGainedAppID = newActiveXlet.appID;
                    ftp.focusGainedComponentIndex = newActiveXlet.componentWithFocus;
                }
            }
        }

        return ftp;
    }

    private FocusTestPrediction predictSetActive(boolean active)
    {
        FocusTestPrediction ftp = new FocusTestPrediction();
        FocusTestXletInfo xlet = m_apps[m_curAppIndex];

        // If this scene is becoming active it may be added to the activable
        // list
        // if it had previously received focus and is visible
        if (active)
        {
            if (xlet.isVisible && xlet.componentWithFocus != -1) ftp.appAddedToEndOfActivableList = xlet.appID;
        }
        else
        {
            // If this scene is becoming invisible, it will be removed from the
            // activable list.
            ftp.appRemovedFromActivableList = xlet.appID;

            // If the scene was also activated, its focused component will lose
            // focus, the scene will be deactivated, and the next scene in the
            // list
            // will gain focus
            if (xlet.isActivated)
            {
                ftp.focusLostAppID.addElement(xlet.appID);
                ftp.focusLostComponentIndex.addElement(new Integer(xlet.componentWithFocus));
                ftp.appWindowDeactivated = xlet.appID;

                if (m_activableWindows.size() > 1)
                {
                    FocusTestXletInfo newActiveXlet = (FocusTestXletInfo) m_activableWindows.elementAt(1);

                    ftp.appWindowActivated = newActiveXlet.appID;
                    ftp.focusGainedAppID = newActiveXlet.appID;
                    ftp.focusGainedComponentIndex = newActiveXlet.componentWithFocus;
                }
            }
        }

        return ftp;
    }

    // /////////////////////////////////////////////////////////////////////////////
    // TEST EVALUATION //
    // /////////////////////////////////////////////////////////////////////////////

    // This functions runs assert tests against our set of predictions
    private void runTests()
    {
        if (m_prediction == null) return;

        AppID appID;

        // App added to end of activable list
        appID = m_prediction.appAddedToEndOfActivableList;
        if (appID != null)
        {
            // Fail if list is empty
            if (m_activableWindows.isEmpty())
                m_test.fail("App " + appID + " should have been added to end of activable list, "
                        + "but list was empty!");

            // Fail if xlet at end of list is not the correct one
            FocusTestXletInfo xlet = (FocusTestXletInfo) m_activableWindows.elementAt(m_activableWindows.size() - 1);
            m_test.assertEquals("App should have been added to end of activable list!", appID, xlet.appID);
        }

        // App moved to front of activable list
        appID = m_prediction.appMovedToFrontOfActivableList;
        if (appID != null)
        {
            // Fail if list is empty
            if (m_activableWindows.isEmpty())
                m_test.fail("App " + appID + " should have been moved to front of activable list, "
                        + "but list was empty!");

            // Fail if xlet at front of list is not the correct one
            FocusTestXletInfo xlet = (FocusTestXletInfo) m_activableWindows.elementAt(0);
            m_test.assertEquals("App should have been added to front of activable list!", appID, xlet.appID);
        }

        // App removed from activable list
        appID = m_prediction.appRemovedFromActivableList;
        if (appID != null)
        {
            FocusTestXletInfo xlet = new FocusTestXletInfo();
            xlet.appID = appID;

            m_test.assertFalse("Activable window list should not contain " + appID + "!",
                    m_activableWindows.contains(xlet));
        }

        // Window activated
        appID = m_prediction.appWindowActivated;
        if (appID == null)
            m_test.assertEquals("App should not have received a 'WindowActivated' event!", appID,
                    m_testEvents.windowActivatedXletAppID);
        else
            m_test.assertEquals("App should have received a 'WindowActivated' event!", appID,
                    m_testEvents.windowActivatedXletAppID);

        // Window deactivated
        appID = m_prediction.appWindowDeactivated;
        if (appID == null)
            m_test.assertEquals("App should not have received a 'WindowDeactivated' event!", appID,
                    m_testEvents.windowDeactivatedXletAppID);
        else
            m_test.assertEquals("App should have received a 'WindowDeactivated' event!", appID,
                    m_testEvents.windowDeactivatedXletAppID);

        int compIndex;

        // Focus gained
        appID = m_prediction.focusGainedAppID;
        compIndex = m_prediction.focusGainedComponentIndex;
        if (appID == null && compIndex == -1)
        {
            m_test.assertEquals("App should not have gained focus!", appID, m_testEvents.focusGainedXletAppID);
            m_test.assertEquals("Component should not have gained focus!", compIndex,
                    m_testEvents.focusGainedComponentIndex);
        }
        else if (appID != null && compIndex != -1)
        {
            m_test.assertEquals("App should have gained focus!", appID, m_testEvents.focusGainedXletAppID);
            m_test.assertEquals("Component should have gained focus!", compIndex,
                    m_testEvents.focusGainedComponentIndex);
        }

        // Focus lost
        Vector focusLostApps = m_prediction.focusLostAppID;
        Vector focusLostComps = m_prediction.focusLostComponentIndex;
        if (focusLostApps.isEmpty() && focusLostComps.isEmpty())
        {
            m_test.assertTrue("App should not have lost focus", m_testEvents.focusLostXletAppID.isEmpty());
            m_test.assertTrue("Component should not have lost focus", m_testEvents.focusLostComponentIndex.isEmpty());
        }
        else if (!focusLostApps.isEmpty() && !focusLostComps.isEmpty())
        {
            // These vectors should always be the same size. If not, there is
            // problem
            // with the test code
            if (focusLostApps.size() != focusLostComps.size())
            {
                m_test.fail("focusLostApps and focusLostComps should always be the same size! "
                        + "Might be something wrong with the test code.");
            }
            else
            {
                // Our events should match our predictions in size
                if (focusLostApps.size() != m_testEvents.focusLostXletAppID.size()
                        || focusLostApps.size() != m_testEvents.focusLostComponentIndex.size())
                {
                    m_test.fail("FocusLostEvents and FocusLostPredicions should always be the same size! "
                            + "Might be something wrong with the test code.");
                }
                else
                {
                    // Check each prediction against actual event
                    for (int i = 0; i < focusLostApps.size(); ++i)
                    {
                        m_test.assertTrue("App should have lost focus!",
                                          m_testEvents.focusLostXletAppID.contains(focusLostApps.elementAt(i)));
                    }
                    for (int i = 0; i < focusLostComps.size(); ++i)
                    {
                        m_test.assertTrue("Component should have lost focus",
                                          m_testEvents.focusLostComponentIndex.contains(focusLostComps.elementAt(i)));
                    }
                }
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES //
    // /////////////////////////////////////////////////////////////////////////////

    // Published via IXC. Allows test xlets to report their window events to
    // the test runner
    private class FocusTestEventHandler implements FocusTestEvents
    {
        public void windowActivated(int oID, int aID) throws RemoteException
        {
            AppID appID = new AppID(oID, aID);
            int index = getXletIndexByAppID(appID);

            m_apps[index].isActivated = true;

            // Set test result flags
            windowActivatedXletAppID = appID;
        }

        public void windowDeactivated(int oID, int aID) throws RemoteException
        {
            AppID appID = new AppID(oID, aID);
            int index = getXletIndexByAppID(appID);

            m_apps[index].isActivated = false;

            // Set test result flags
            windowDeactivatedXletAppID = m_apps[index].appID;
        }

        // Called when a test xlet component has gained focus. The window for
        // this
        // xlet is moved to the front of the activable list
        public void focusGained(int oID, int aID, int componentIndex) throws RemoteException
        {
            AppID appID = new AppID(oID, aID);
            int index = getXletIndexByAppID(appID);

            // Remove app and re-insert at front of list
            m_activableWindows.removeElement(m_apps[index]);
            m_activableWindows.insertElementAt(m_apps[index], 0);

            // Store the component that has received focus
            m_apps[index].componentWithFocus = componentIndex;

            // Set test result flags
            focusGainedXletAppID = m_apps[index].appID;
            focusGainedComponentIndex = componentIndex;
        }

        // Called when a test xlet component has lost focus
        public void focusLost(int oID, int aID, int componentIndex) throws RemoteException
        {
            AppID appID = new AppID(oID, aID);
            int index = getXletIndexByAppID(appID);

            // Make sure that the component which has just lost focus was
            // previously marked as having focus
            m_test.assertEquals("Component that lost focus did not previously have focus!",
                    m_apps[index].componentWithFocus, componentIndex);

            focusLostXletAppID.addElement(m_apps[index].appID);
            focusLostComponentIndex.addElement(new Integer(componentIndex));
        }

        public void clearTestVariables()
        {
            windowDeactivatedXletAppID = null;
            windowActivatedXletAppID = null;
            focusGainedXletAppID = null;
            focusGainedComponentIndex = -1;
            focusLostXletAppID = new Vector();
            focusLostComponentIndex = new Vector();
        }

        // These vars help us keep track of which callbacks have been called
        // so that we can properly verify tests
        public AppID windowDeactivatedXletAppID = null;

        public AppID windowActivatedXletAppID = null;

        public AppID focusGainedXletAppID = null;

        public int focusGainedComponentIndex = -1;

        public Vector focusLostXletAppID = new Vector();

        public Vector focusLostComponentIndex = new Vector();
    }

    // Our main test runner UI component which displays the current test xlet
    // and component as well as the menu of input commands
    private class FocusTestInfo extends Container
    {
        public FocusTestInfo()
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

        public void setActiveComponent(String compName)
        {
            m_activeComponent = compName;
        }

        public void paint(Graphics g)
        {
            g.drawString("<< UP/DOWN changes Xlet >>", 10, 40);
            g.drawString("<< LEFT/RIGHT changes Component >>", 10, 54);
            g.drawString(m_activeXlet + " [" + m_activeXletState + "]", 60, 78);
            g.drawString(m_activeComponent, 60, 92);

            int x = 25, y = 135;
            int dy = 14;

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
                g.drawString("(1) requestFocus()", x, y);
                y += dy;

                // Only draw these tools if the current component is Scene
                if (m_curComponentIndex == 0)
                {
                    y += dy;
                    g.drawString("(2) setVisible(true)", x, y);
                    y += dy;
                    g.drawString("(3) setVisible(false)", x, y);
                    y += dy;
                    g.drawString("(4) setActive(true)", x, y);
                    y += dy;
                    g.drawString("(5) setActive(false)", x, y);
                    y += dy;
                }
            }

            // Provide inputs for running tests and dumping test results when
            // running in manual mode
            if (!m_axc.isConnected())
            {
                y += dy;

                if (m_prediction != null) g.drawString("(8) Run tests", x, y);
                y += dy;

                g.drawString("(0) Dump test results", x, y);
                y += dy;
            }
        }

        private String m_activeXlet;

        private String m_activeXletState;

        private String m_activeComponent;
    }

    // Stores information about a focus test Xlet
    private class FocusTestXletInfo
    {
        public AppID appID;

        public AppProxy appProxy;

        public String xletName;

        public FocusTestControl control;

        // 0 = Scene, 1 = Text1, 2 = Text2, -1 = No Focus
        public int componentWithFocus = -1;

        public boolean isActive = true;

        public boolean isVisible = true;

        public boolean isActivated = false;

        // Equivalence operator used for easy Vector add/remove
        public boolean equals(Object other)
        {
            // Validate the cast
            FocusTestXletInfo otherFTXI;
            try
            {
                otherFTXI = (FocusTestXletInfo) other;
            }
            catch (ClassCastException e)
            {
                return false;
            }

            // Equal if AppIDs are equal
            return otherFTXI.appID.equals(this.appID);
        }
    }

    private class FocusTestPrediction
    {
        public AppID appRemovedFromActivableList = null;

        public AppID appAddedToEndOfActivableList = null;

        public AppID appMovedToFrontOfActivableList = null;

        public AppID appWindowActivated = null;

        public AppID appWindowDeactivated = null;

        public AppID focusGainedAppID = null;

        public int focusGainedComponentIndex = -1;

        public Vector focusLostAppID = new Vector();

        public Vector focusLostComponentIndex = new Vector();
    }

    // /////////////////////////////////////////////////////////////////////////////
    // DATA MEMBERS //
    // /////////////////////////////////////////////////////////////////////////////

    XletContext m_ctx;

    FocusTestEventHandler m_testEvents = new FocusTestEventHandler();

    // AppProxy List
    FocusTestXletInfo[] m_apps = null;

    int m_curAppIndex = 0; // Index into our FocusTestXletInfo array

    int m_curComponentIndex = 0; // 0 = HScene, 1 = HText1, 2 = HText2

    // Test prediction data structure
    FocusTestPrediction m_prediction = null;

    // Internal list of activable scenes -- just as the implementation would
    // keep
    Vector m_activableWindows = new Vector();

    HScene m_scene;

    FocusTestInfo m_infoBox = new FocusTestInfo();

    // AutoXlet stuff
    AutoXletClient m_axc = null;

    Logger m_dbgLog = null;

    Test m_test = null;

    Monitor m_eventMonitor = new Monitor();
}
