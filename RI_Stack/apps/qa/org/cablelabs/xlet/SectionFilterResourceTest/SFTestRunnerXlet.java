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

package org.cablelabs.xlet.SectionFilterResourceTest;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.lang.reflect.Field;
import java.util.Vector;
import java.io.FileInputStream;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;
import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIChangeType;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.SIRequestor;
import javax.tv.service.SIManager;
import javax.tv.service.SIRetrievable;
import javax.tv.service.transport.Network;
import javax.tv.service.transport.Transport;
import javax.tv.service.transport.TransportStream;
import javax.tv.service.transport.NetworkCollection;

import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseFilter;
import org.dvb.dsmcc.NotLoadedException;
import org.dvb.dsmcc.ServiceDomain;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.dvb.io.ixc.IxcRegistry;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HSceneTemplate;

import org.ocap.event.EventManager;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceUsage;
import org.ocap.si.Descriptor;
import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramAssociationTableManager;
import org.ocap.si.ProgramMapTable;
import org.ocap.si.ProgramMapTableManager;
import org.ocap.si.PMTElementaryStreamInfo;
import org.ocap.si.StreamType;
import org.ocap.si.TableChangeListener;
import org.ocap.ui.event.OCRcEvent;

import org.davic.net.InvalidLocatorException;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceController;
import org.davic.net.tuning.NetworkInterfaceEvent;
import org.davic.net.tuning.NetworkInterfaceException;
import org.davic.net.tuning.NetworkInterfaceListener;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.davic.net.tuning.NetworkInterfaceTuningOverEvent;
import org.davic.resources.ResourceClient;
import org.davic.resources.ResourceProxy;

import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Monitor;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;
import org.cablelabs.lib.utils.ArgParser;

public class SFTestRunnerXlet implements Xlet, AppStateChangeEventListener, Driveable, UserEventListener,
        ResourceContentionHandler
{
    // /////////////////////////////////////////////////////////////////////////////
    // XLET FUNCTIONS //
    // /////////////////////////////////////////////////////////////////////////////

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        m_ctx = ctx;

        String[] args = (String[]) (ctx.getXletProperty(XletContext.ARGS));

        Vector xlets = new Vector();
        Vector rejects = new Vector();

        // Populate our app proxies array from the arguments
        for (int i = 0; i < args.length; ++i)
        {
            // Parse "key=value" pairs
            String key = args[i].substring(0, args[i].indexOf("="));
            String value = args[i].substring(args[i].indexOf("=") + 1);

            if (key.equals(TEST_XLET_ARG))
            {
                // Parse the individual appID and orgID from the 48-bit int
                long orgIDappID = Long.parseLong(value.substring(2), 16);
                int orgID = (int) ((orgIDappID >> 16) & 0xFFFFFFFF);
                int appID = (int) (orgIDappID & 0xFFFF);

                // Get the AppProxy for this app from the database. If the
                // appID,orgID
                // is invalid, abort the test runner xlet startup
                AppID testAppID = new AppID(orgID, appID);
                AppsDatabase adb = AppsDatabase.getAppsDatabase();
                AppProxy appProxy = adb.getAppProxy(testAppID);
                if (appProxy == null)
                    throw new XletStateChangeException("Could not get section filter test xlet AppProxy!  " + testAppID);

                appProxy.addAppStateChangeEventListener(this);
                SFTestXletInfo xlet = new SFTestXletInfo();
                xlet.appID = testAppID;
                xlet.appProxy = appProxy;
                xlet.xletName = adb.getAppAttributes(testAppID).getName();
                xlet.appPriority = adb.getAppAttributes(testAppID).getPriority();
                xlets.addElement(xlet);
            }
            else if (key.equals(REJECT_XLET_ARG))
            {
                // Parse the individual appID and orgID from the 48-bit int
                long orgIDappID = Long.parseLong(value.substring(2), 16);
                int orgID = (int) ((orgIDappID >> 16) & 0xFFFFFFFF);
                int appID = (int) (orgIDappID & 0xFFFF);
                rejects.addElement(new AppID(orgID, appID));
            }
            else if (key.equals(DO_FILTERING_ARG))
            {
                if (value.equalsIgnoreCase("true")) m_doFiltering = true;
            }
            else if (key.equals(CONFIG_XLET_ARG))
            {
                m_configFile = new String(value);
            }
        }

        getConfigParams();

        m_apps = new SFTestXletInfo[xlets.size()];
        xlets.copyInto(m_apps);
        m_rejects = new AppID[rejects.size()];
        rejects.copyInto(m_rejects);

        // Publish event handler via IXC
        try
        {
            IxcRegistry.bind(ctx, "SFTestEvents", m_testEvents);
        }
        catch (AlreadyBoundException e)
        {
            throw new XletStateChangeException("SectionFilterEventHandler name already bound via IXC!");
        }

        // Scene and InfoBox
        m_scene = HSceneFactory.getInstance().getBestScene(new HSceneTemplate());
        m_infoBox.setBounds(5, 245, 630, 230);
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
        events.addKey(OCRcEvent.VK_ENTER);
        events.addKey(OCRcEvent.VK_PINP_DOWN);
        events.addKey(OCRcEvent.VK_PINP_UP);
        events.addKey(OCRcEvent.VK_COLORED_KEY_0);
        events.addKey(OCRcEvent.VK_COLORED_KEY_2);
        events.addKey(OCRcEvent.VK_EXIT);
        events.addKey(OCRcEvent.VK_LAST);
        events.addKey(OCRcEvent.VK_LIST);
        events.addKey(OCRcEvent.VK_INFO);
        events.addKey(OCRcEvent.VK_SETTINGS);
        events.addAllNumericKeys();
        em.addUserEventListener(this, events);

        // Register us as the DAVIC section filter resource contention handler
        ResourceContentionManager rcm = ResourceContentionManager.getInstance();
        rcm.setResourceFilter(m_resourceAppFilter, "org.davic.mpeg.sections.SectionFilterGroup");

        // Connect to AutoXlet framework
        m_axc = new AutoXletClient(this, ctx);
        m_test = m_axc.getTest();

        if (m_axc.isConnected())
            m_dbgLog = m_axc.getLogger();
        else
            m_dbgLog = new XletLogger();

        buildServiceList();
        reserveTuners();
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

        // Release reserved tuners
        for (int i = 0; i < m_tuners.length; ++i)
        {
            try
            {
                m_tuners[i].tuner.release();
            }
            catch (NetworkInterfaceException e)
            {
            }
        }

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

        SFTestXletInfo xlet = m_apps[m_curAppIndex];
        AppProxy curAppProxy = xlet.appProxy;
        int curAppState = curAppProxy.getState();

        switch (e.getCode())
        {
            // Change the current active Xlet
            case OCRcEvent.VK_UP:
                m_keySequence = m_keySequence + "U";

                if (m_curAppIndex == m_apps.length - 1)
                    setCurrentAppIndex(0);
                else
                    setCurrentAppIndex(m_curAppIndex + 1);
                updateCurrentAppState();
                break;

            case OCRcEvent.VK_DOWN:
                m_keySequence = m_keySequence + "D";

                if (m_curAppIndex == 0)
                    setCurrentAppIndex(m_apps.length - 1);
                else
                    setCurrentAppIndex(m_curAppIndex - 1);
                updateCurrentAppState();
                break;

            // Change current tuner
            case OCRcEvent.VK_LEFT:
                m_keySequence = m_keySequence + "L";

                m_currentTunerIdx = (m_currentTunerIdx == 0) ? m_tuners.length - 1 : m_currentTunerIdx - 1;
                m_infoBox.repaint();
                break;

            case OCRcEvent.VK_RIGHT:
                m_keySequence = m_keySequence + "R";

                m_currentTunerIdx = (m_currentTunerIdx == m_tuners.length - 1) ? 0 : m_currentTunerIdx + 1;
                m_infoBox.repaint();
                break;

            // Modify the current xlet's run state
            case OCRcEvent.VK_STOP:
                m_keySequence = m_keySequence + "S";

                if (curAppState == AppProxy.PAUSED || curAppState == AppProxy.STARTED) curAppProxy.stop(true);
                updateCurrentAppState();
                break;

            case OCRcEvent.VK_PLAY:
                m_keySequence = m_keySequence + "P";

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
                m_keySequence = m_keySequence + "X";

                if (curAppState == AppProxy.STARTED) curAppProxy.pause();
                updateCurrentAppState();
                break;

            // Change current service (UP)
            case OCRcEvent.VK_CHANNEL_UP:
                m_keySequence = m_keySequence + ">";

                if (m_locatorList.length > MAX_CHANNELS && m_currentDrawIdx == m_currentFrequencyIdx)
                    m_currentDrawIdx = (m_currentDrawIdx == 0) ? m_locatorList.length - 1 : m_currentDrawIdx - 1;
                m_currentFrequencyIdx = (m_currentFrequencyIdx == 0) ? m_locatorList.length - 1
                        : m_currentFrequencyIdx - 1;
                m_infoBox.repaint();
                break;

            // Change current service (DOWN)
            case OCRcEvent.VK_CHANNEL_DOWN:
                m_keySequence = m_keySequence + "<";

                m_currentFrequencyIdx = (m_currentFrequencyIdx == m_locatorList.length - 1) ? 0
                        : m_currentFrequencyIdx + 1;
                if (m_locatorList.length > MAX_CHANNELS
                        && m_currentFrequencyIdx == ((m_currentDrawIdx + MAX_CHANNELS) % m_locatorList.length))
                    m_currentDrawIdx = (m_currentDrawIdx + 1) % m_locatorList.length;
                m_infoBox.repaint();
                break;

            // Change current elementary stream PID (UP)
            case OCRcEvent.VK_PINP_UP:
                m_keySequence = m_keySequence + "+";

                TunedFrequency ts1 = m_tuners[m_currentTunerIdx];
                if (ts1.elementaryStreams.size() > MAX_STREAMPIDS && ts1.currentDrawIdx == ts1.currentStreamIdx)
                    ts1.currentDrawIdx = (ts1.currentDrawIdx == 0) ? ts1.elementaryStreams.size() - 1
                            : ts1.currentDrawIdx - 1;
                ts1.currentStreamIdx = (ts1.currentStreamIdx == 0) ? ts1.elementaryStreams.size() - 1
                        : ts1.currentStreamIdx - 1;
                m_infoBox.repaint();
                break;

            // Change current elementary stream PID (DOWN)
            case OCRcEvent.VK_PINP_DOWN:
                m_keySequence = m_keySequence + "-";

                TunedFrequency ts2 = m_tuners[m_currentTunerIdx];
                ts2.currentStreamIdx = (ts2.currentStreamIdx == ts2.elementaryStreams.size() - 1) ? 0
                        : ts2.currentStreamIdx + 1;
                if (ts2.elementaryStreams.size() > MAX_STREAMPIDS
                        && ts2.currentStreamIdx == ((ts2.currentDrawIdx + MAX_STREAMPIDS) % ts2.elementaryStreams.size()))
                    ts2.currentDrawIdx = (ts2.currentDrawIdx + 1) % ts2.elementaryStreams.size();
                m_infoBox.repaint();
                break;

            // Tune current service (on current tuner)
            case OCRcEvent.VK_ENTER:
                m_keySequence = m_keySequence + "E";

                // Clear out our current PIDs for this tuner, in case the tune
                // fails
                m_tuners[m_currentTunerIdx].elementaryStreams.removeAllElements();
                m_tuners[m_currentTunerIdx].currentDrawIdx = 0;
                m_tuners[m_currentTunerIdx].currentStreamIdx = -1;

                try
                {
                    final boolean[] patReceived = new boolean[1];
                    final int[] retryCount = new int[1];

                    m_dbgLog.log("[SFTestRunnerXlet] About to tune. Adding listener for PAT.");

                    ProgramAssociationTableManager.getInstance().addInBandChangeListener(new TableChangeListener()
                    {
                        public void notifyChange(SIChangeEvent changeEvent)
                        {
                            m_dbgLog.log("[SFTestRunnerXlet] Notified of PAT change: " + changeEvent.getChangeType());
                            patReceived[0] = (changeEvent.getChangeType().equals(SIChangeType.ADD))
                                    || (changeEvent.getChangeType().equals(SIChangeType.MODIFY));
                        }
                    }, m_locatorList[m_currentFrequencyIdx]);

                    m_tuners[m_currentTunerIdx].tuning = true;
                    m_tuners[m_currentTunerIdx].tuner.tune(m_locatorList[m_currentFrequencyIdx]);

                    while (!patReceived[0] && (retryCount[0] < 25))
                    {
                        try
                        {
                            m_dbgLog.log("[SFTestRunnerXlet] No PAT yet...");
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException ie)
                        {
                            m_dbgLog.log("[SFTestRunnerXlet] Sleep interrupted while listening for PAT!");
                        }

                        ++retryCount[0];
                    }

                    if (!patReceived[0])
                    {
                        m_dbgLog.log("[SFTestRunnerXlet] Was not notified of available PAT.  Attempting PAT aquisition anyways.");
                    }
                }
                catch (NetworkInterfaceException e1)
                {
                    m_tuners[m_currentTunerIdx].tuning = false;
                    m_tuners[m_currentTunerIdx].locator = null;
                    m_dbgLog.log("[SFTestRunnerXlet] Exception caught while tuning! -- " + e1.getMessage());
                }
                m_infoBox.repaint();
                break;

            // Create filter group
            case OCRcEvent.VK_1:
                m_keySequence = m_keySequence + "1";

                if (m_apps[m_curAppIndex].control == null) break;
                try
                {
                    m_apps[m_curAppIndex].control.createFilterGroup();
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Attach the current section filter to current tuner's transport
            // stream
            case OCRcEvent.VK_2:
                m_keySequence = m_keySequence + "2";

                if (m_apps[m_curAppIndex].control == null) break;
                try
                {
                    TunedFrequency ts = m_tuners[m_currentTunerIdx];
                    if (ts.locator != null)
                        m_apps[m_curAppIndex].control.attachFilterGroup(m_currentTunerIdx, ts.locator.getFrequency());
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Detach filter group
            case OCRcEvent.VK_3:
                m_keySequence = m_keySequence + "3";

                if (m_apps[m_curAppIndex].control == null) break;
                try
                {
                    m_apps[m_curAppIndex].control.detachFilterGroup();
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Create section filter
            case OCRcEvent.VK_4:
                m_keySequence = m_keySequence + "4";

                if (m_apps[m_curAppIndex].control == null) break;
                try
                {
                    m_apps[m_curAppIndex].control.createSectionFilter();
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Start the current section filter in the currently selected group
            case OCRcEvent.VK_5:
                m_keySequence = m_keySequence + "5";

                if (m_apps[m_curAppIndex].control == null) break;
                try
                {
                    TunedFrequency ts = m_tuners[m_currentTunerIdx];
                    if (!ts.elementaryStreams.isEmpty())
                    {
                        ElementaryStream es = (ElementaryStream) ts.elementaryStreams.elementAt(ts.currentStreamIdx);
                        m_apps[m_curAppIndex].control.startSectionFilter(es.pid, m_doFiltering);
                    }
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Stop section filter
            case OCRcEvent.VK_6:
                m_keySequence = m_keySequence + "6";

                if (m_apps[m_curAppIndex].control == null) break;
                try
                {
                    m_apps[m_curAppIndex].control.stopSectionFilter();
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Cycle through filter groups
            case OCRcEvent.VK_7:
                m_keySequence = m_keySequence + "7";

                if (m_apps[m_curAppIndex].control == null) break;
                try
                {
                    m_apps[m_curAppIndex].control.changeFilterGroup();
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Delete current filter group
            case OCRcEvent.VK_EXIT:
                m_keySequence = m_keySequence + "Q";

                if (m_apps[m_curAppIndex].control == null) break;
                try
                {
                    m_apps[m_curAppIndex].control.deleteFilterGroup();
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Cycle through section filters in the currently selected group
            case OCRcEvent.VK_9:
                m_keySequence = m_keySequence + "9";

                if (m_apps[m_curAppIndex].control == null) break;
                try
                {
                    m_apps[m_curAppIndex].control.changeSectionFilter();
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Attempt to mount the object carousel currently selected
            case OCRcEvent.VK_8:
                m_keySequence = m_keySequence + "8";

                TunedFrequency ocTS = m_tuners[m_currentTunerIdx];
                if (ocTS.currentStreamIdx == -1) break;
                ElementaryStream currentStream = (ElementaryStream) ocTS.elementaryStreams.elementAt(ocTS.currentStreamIdx);
                if (currentStream.carouselID != -1)
                {
                    m_objectCarousel = new ServiceDomain();
                    try
                    {
                        m_objectCarousel.attach(currentStream.programLocator, (int) currentStream.carouselID);
                        m_carouselAttached = true;
                    }
                    catch (Exception e1)
                    {
                        m_dbgLog.log("Failed to attach carousel -- " + e1.getMessage());
                        m_carouselAttached = false;
                    }
                }
                break;

            // Unmount the current carousel (if one if currently mounted)
            case OCRcEvent.VK_0:
                m_keySequence = m_keySequence + "0";

                if (m_carouselAttached)
                {
                    try
                    {
                        m_objectCarousel.detach();
                    }
                    catch (NotLoadedException e1)
                    {
                        m_dbgLog.log("Failed to detach carousel -- " + e1.getMessage());
                    }
                    m_carouselAttached = false;
                }
                break;

            // Toggle the willing-to-release state of newly created groups
            case OCRcEvent.VK_COLORED_KEY_0:
                m_keySequence = m_keySequence + "C";

                if (m_apps[m_curAppIndex].control == null) break;
                try
                {
                    m_apps[m_curAppIndex].control.toggleWillingToRelease();
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Toggle the filter group priority of newly created groups
            case OCRcEvent.VK_COLORED_KEY_2:
                m_keySequence = m_keySequence + "B";

                if (m_apps[m_curAppIndex].control == null) break;
                try
                {
                    m_apps[m_curAppIndex].control.toggleFilterGroupPriority();
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Get status of the current filter.
            case OCRcEvent.VK_INFO:

                try
                {
                    // Current filter status is used to verify automated test
                    // results.
                    m_status = m_apps[m_curAppIndex].control.getCurrentFilterState();

                    if (m_keySequence.endsWith("]"))
                    {
                        m_keySequence = m_keySequence.substring(0, m_keySequence.lastIndexOf("["));
                    }
                    m_keySequence = m_keySequence + "[" + m_status + "]";
                }
                catch (RemoteException e1)
                {
                    m_dbgLog.log(e1);
                }
                break;

            // Get automated test results for the next Test Case.
            case OCRcEvent.VK_LAST:
                if (m_last_test_completed == 0)
                {
                    m_last_test_completed = TestCase1();
                }
                else if (m_last_test_completed == 1)
                {
                    m_last_test_completed = TestCase2();
                }
                else if (m_last_test_completed == 2)
                {
                    m_last_test_completed = TestCase3();
                }
                else if (m_last_test_completed == 3)
                {
                    m_last_test_completed = TestCase4();
                }
                else if (m_last_test_completed == 4)
                {
                    m_last_test_completed = TestCase5();
                }
                else if (m_last_test_completed == 5)
                {
                    m_last_test_completed = TestCase6();
                }
                else if (m_last_test_completed == 6)
                {
                    m_last_test_completed = TestCase7();
                }

                break;

            // Get automated test results for all the Test Cases.
            case OCRcEvent.VK_LIST:

                TestCase1();
                TestCase2();
                TestCase3();
                TestCase4();
                TestCase5();
                TestCase6();
                TestCase7();

                break;

            // Tune to the frequency listed in the config.properties file:
            // Sample config.properties entry: sfrt_frequency=591000000
            // This feature is used for test automation.
            case OCRcEvent.VK_SETTINGS:
                m_keySequence = m_keySequence + "E";

                // Find a locator in the discovered channel list,
                // which matches the frequency from the config.properties file.

                int counter = 0;
                while (m_configFrequency != m_locatorList[m_currentFrequencyIdx].getFrequency()
                        && counter <= m_locatorList.length)
                {
                    if (m_locatorList.length > MAX_CHANNELS && m_currentDrawIdx == m_currentFrequencyIdx)
                    {
                        m_currentDrawIdx = (m_currentDrawIdx == 0) ? m_locatorList.length - 1 : m_currentDrawIdx - 1;
                    }
                    m_currentFrequencyIdx = (m_currentFrequencyIdx == 0) ? m_locatorList.length - 1
                            : m_currentFrequencyIdx - 1;
                    counter++;
                }

                m_infoBox.repaint();

                // Clear out our current PIDs for this tuner, in case the tune
                // fails.
                m_tuners[m_currentTunerIdx].elementaryStreams.removeAllElements();
                m_tuners[m_currentTunerIdx].currentDrawIdx = 0;
                m_tuners[m_currentTunerIdx].currentStreamIdx = -1;

                try
                {
                    m_tuners[m_currentTunerIdx].tuning = true;
                    m_tuners[m_currentTunerIdx].tuner.tune(m_locatorList[m_currentFrequencyIdx]);
                }
                catch (NetworkInterfaceException e1)
                {
                    m_tuners[m_currentTunerIdx].tuning = false;
                    m_tuners[m_currentTunerIdx].locator = null;
                    m_dbgLog.log("Exception caught while tuning! -- " + e1.getMessage());
                }
                m_infoBox.repaint();
                break;
        }

        m_infoBox.repaint();
    }

    // Add a new line character at the end of a String,
    // if it is not there already.
    private String NewLine(String s)
    {
        if (!(s.endsWith("\n")))
        {
            s += "\n";
        }
        return s;
    }

    private void getConfigParams()
    {
        ArgParser config_args = null;
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(m_configFile);
            config_args = new ArgParser(fis);
            m_configFrequency = config_args.getIntArg(FREQUENCY);
            fis.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public int TestCase1()
    {
        m_keySequence = NewLine(m_keySequence);
        m_dbgLog.log("\n\nRECORDED KEY SEQUENCE:\n" + m_keySequence);

        m_dbgLog.log("\n********** TEST CASE 1 **********");
        m_dbgLog.log("DESCRIPTION:      Test that only two filters per group can be created.");
        m_dbgLog.log("KEY SEQUENCE:     ENTER PLAY 12 45 INFO 45 INFO 45 INFO 95 INFO 95 INFO");
        m_dbgLog.log("PROCEDURE:        (1) Tune and start SFTest1.");
        m_dbgLog.log("                  (2) Create a filter group and attach it to the stream.");
        m_dbgLog.log("                  (3) Create and start three filters.");
        m_dbgLog.log("                  (4) Make sure the first two filters are still running,");
        m_dbgLog.log("                      after the third one fails to start.");
        m_dbgLog.log("EXPECTED RESULTS: The first two filters run, the third filter fails to start.");
        m_dbgLog.log("CLEANUP SEQUENCE: VK_EXIT");

        if (m_keySequence.indexOf("1245[4]45[4]45[7]95[4]95[4]") >= 0)
        {
            m_dbgLog.log("STATUS:           PASSED");
            m_test.assertTrue(true); // Passed the test.
        }
        else
        {
            m_dbgLog.log("STATUS:           FAILED");
            m_test.fail("Test Case 1 failed.");
        }
        m_dbgLog.log("*********************************");

        return 1;
    }

    private int TestCase2()
    {
        m_keySequence = NewLine(m_keySequence);
        m_dbgLog.log("\n\nRECORDED KEY SEQUENCE:\n" + m_keySequence);

        m_dbgLog.log("\n********** TEST CASE 2 **********");
        m_dbgLog.log("DESCRIPTION:      Test that only two filters per group can be created.");
        m_dbgLog.log("                  Then stop one of them, and verify that the third filter can run.");
        m_dbgLog.log("KEY SEQUENCE:     12 45 INFO 45 INFO 45 INFO 96 95 INFO 95 INFO");
        m_dbgLog.log("PROCEDURE:        (1) Create a filter group and attach it to the stream.");
        m_dbgLog.log("                  (2) Create and start three filters.  The first two filters");
        m_dbgLog.log("                      run, the third filter fails to start.");
        m_dbgLog.log("                  (3) Stop the first filter.  Make sure the second one is still");
        m_dbgLog.log("                      running.  Start the third one again.");
        m_dbgLog.log("EXPECTED RESULTS: The third filter should run.");
        m_dbgLog.log("CLEANUP SEQUENCE: VK_EXIT");

        if (m_keySequence.indexOf("1245[4]45[4]45[7]9695[4]95[4]") >= 0)
        {
            m_dbgLog.log("STATUS:           PASSED");
            m_test.assertTrue(true); // Passed the test.
        }
        else
        {
            m_dbgLog.log("STATUS:           FAILED");
            m_test.fail("Test Case 2 failed.");
        }
        m_dbgLog.log("*********************************");

        return 2;
    }

    private int TestCase3()
    {
        m_keySequence = NewLine(m_keySequence);
        m_dbgLog.log("\n\nRECORDED KEY SEQUENCE:\n" + m_keySequence);

        m_dbgLog.log("\n********** TEST CASE 3 **********");
        m_dbgLog.log("DESCRIPTION:      Test that monapp-rejected applications are not allowed");
        m_dbgLog.log("                  to use any filtering resources.");
        m_dbgLog.log("KEY SEQUENCE:     UP_ARROW UP_ARROW PLAY 12 45 INFO");
        m_dbgLog.log("PROCEDURE:        (1) Start SFTest3, which is configured to be monapp-rejected.");
        m_dbgLog.log("                  (2) Create a filter group and attach it to the stream.");
        m_dbgLog.log("                  (3) Create and start a filter.");
        m_dbgLog.log("EXPECTED RESULTS: The filter fails to start.");
        m_dbgLog.log("CLEANUP SEQUENCE: VK_EXIT : VK_DOWN : VK_DOWN");

        if (m_keySequence.indexOf("UUP1245[5]") >= 0)
        {
            m_dbgLog.log("STATUS:           PASSED");
            m_test.assertTrue(true); // Passed the test.
        }
        else
        {
            m_dbgLog.log("STATUS:           FAILED");
            m_test.fail("Test Case 3 failed.");
        }
        m_dbgLog.log("*********************************");

        return 3;
    }

    private int TestCase4()
    {
        m_keySequence = NewLine(m_keySequence);
        m_dbgLog.log("\n\nRECORDED KEY SEQUENCE:\n" + m_keySequence);

        m_dbgLog.log("\n********** TEST CASE 4 **********");
        m_dbgLog.log("DESCRIPTION:      Test the 8300HD ability to filter on 6 to 7 different PIDs,");
        m_dbgLog.log("                  before section filtering resources run out.");
        m_dbgLog.log("NOTE:             SFTest1 is expected to have a higher priority than SFTest2.");
        m_dbgLog.log("KEY SEQUENCE:     12 45 INFO -45 INFO 12 -45 INFO -45 INFO UP_ARROW PLAY");
        m_dbgLog.log("                  12 -45 INFO -45 INFO 12 -45 INFO -45 INFO");
        m_dbgLog.log("PROCEDURE:        (1) Create a filter group in SFTest1, and attach it to the stream.");
        m_dbgLog.log("                  (2) Create and start two filters on different PIDs: both should run.");
        m_dbgLog.log("                  (3) Create another filter group and attach it to the stream.");
        m_dbgLog.log("                  (4) Create and start two filters on different PIDs: both should run.");
        m_dbgLog.log("                  (5) Start SFTest2, and repeat steps (1) through (3) for it.");
        m_dbgLog.log("                  (6) Create and start two filters.");
        m_dbgLog.log("EXPECTED RESULTS: The last filter, or the last two filters created should fail to run.");
        m_dbgLog.log("CLEANUP SEQUENCE: VK_EXIT : VK_EXIT : VK_DOWN : VK_EXIT : VK_EXIT");

        String expected_sequence_1 = "1245[4]-45[4]12-45[4]-45[4]UP12-45[4]-45[4]12-45[4]-45[7]";
        String expected_sequence_2 = "1245[4]-45[4]12-45[4]-45[4]UP12-45[4]-45[4]12-45[7]-45[7]";

        if (m_keySequence.indexOf(expected_sequence_1) >= 0 || m_keySequence.indexOf(expected_sequence_2) >= 0)
        {
            m_dbgLog.log("STATUS:           PASSED");
            m_test.assertTrue(true); // Passed the test.
        }
        else
        {
            m_dbgLog.log("STATUS:           FAILED");
            m_test.fail("Test Case 4 failed.");
        }
        m_dbgLog.log("*********************************");

        return 4;
    }

    private int TestCase5()
    {
        m_keySequence = NewLine(m_keySequence);
        m_dbgLog.log("\n\nRECORDED KEY SEQUENCE:\n" + m_keySequence);

        m_dbgLog.log("\n********** TEST CASE 5 **********");
        m_dbgLog.log("DESCRIPTION:      Test that filters with a willing-to-release policy give up");
        m_dbgLog.log("                  their resources, when conflicts exist.");
        m_dbgLog.log("KEY SEQUENCE:     C 12 45 INFO -45 INFO 12 -45 INFO -45 INFO UP_ARROW 12 -45 INFO");
        m_dbgLog.log("                  -45 INFO 12 -45 INFO");
        m_dbgLog.log("                  7 +5 INFO 9 +5 INFO ARROW_DOWN +5 INFO 9 +5 INFO 7 +5 INFO 9 +5 INFO");
        m_dbgLog.log("PROCEDURE:        (1) Change SFTest1's priority to Willing.");
        m_dbgLog.log("                  (2) Create a filter group and attach it to the stream.");
        m_dbgLog.log("                  (3) Create and start two filters on different PIDs: both should run.");
        m_dbgLog.log("                  (4) Create another filter group and attach it to the stream.");
        m_dbgLog.log("                  (5) Create and start two filters on different PIDs: both should run.");
        m_dbgLog.log("                  (6) Repeat steps (2) through (5) in SFTest2.");
        m_dbgLog.log("EXPECTED RESULTS: The two filters created in SFTest2, Group1 should run,");
        m_dbgLog.log("                  while both filters in SFTest1/Group0, or SFTest1/Group1, get aborted.");
        m_dbgLog.log("CLEANUP SEQUENCE: VK_UP : VK_EXIT : VK_EXIT : VK_DOWN : VK_EXIT : VK_EXIT : VK_COLORED_KEY_0");

        String expected_sequence_1 = "C1245[4]-45[4]12-45[4]-45[4]U12-45[4]-45[4]12-45[4]"
                + "7+5[4]9+5[4]D+5[4]9+5[4]7+5[7]9+5[7]";

        String expected_sequence_2 = "C1245[4]-45[4]12-45[4]-45[4]U12-45[4]-45[4]12-45[4]"
                + "7+5[4]9+5[4]D+5[7]9+5[7]7+5[4]9+5[4]";

        if (m_keySequence.indexOf(expected_sequence_1) >= 0 || m_keySequence.indexOf(expected_sequence_2) >= 0)
        {
            m_dbgLog.log("STATUS:           PASSED");
            m_test.assertTrue(true); // Passed the test.
        }
        else
        {
            m_dbgLog.log("STATUS:           FAILED");
            m_test.fail("Test Case 5 failed.");
        }
        m_dbgLog.log("*********************************");

        return 5;
    }

    private int TestCase6()
    {
        m_keySequence = NewLine(m_keySequence);
        m_dbgLog.log("\n\nRECORDED KEY SEQUENCE:\n" + m_keySequence);

        m_dbgLog.log("\n********** TEST CASE 6 **********");
        m_dbgLog.log("DESCRIPTION:      Test that filters with low priority can be preempted by filters");
        m_dbgLog.log("                  with high priority within the same application.");
        m_dbgLog.log("NOTE:             SFTest1 is expected to have a higher priority than SFTest2.");
        m_dbgLog.log("KEY SEQUENCE:     12 45 INFO -45 INFO 12 -45 INFO -45 INFO UP_ARROW B 12 -45 INFO");
        m_dbgLog.log("                  -45 INFO B 12 -45 INFO -45 INFO");
        m_dbgLog.log("                  7 +5 INFO 9 +5 INFO ARROW_DOWN +5 INFO 9 +5 INFO 7 +5 INFO 9 +5 INFO");
        m_dbgLog.log("PROCEDURE:        (1) In SFTest1, Create a filter group and attach it to the stream.");
        m_dbgLog.log("                  (2) Create and start two filters on different PIDs: both should run.");
        m_dbgLog.log("                  (3) Create another filter group and attach it to the stream.");
        m_dbgLog.log("                  (4) Create and start two filters on different PIDs: both should run.");
        m_dbgLog.log("                  (5) Move up to SFTest2.");
        m_dbgLog.log("                  (6) Change priority of SFTest2 to Low (it affects Group0).");
        m_dbgLog.log("                  (7) Repeat steps (1) and (2) in SFTest2.");
        m_dbgLog.log("                  (8) Change SFTest2 priority for Group1 to High, then repeat steps (1) and (2).");
        m_dbgLog.log("EXPECTED RESULTS: The two filters created in SFTest2, Group1 should run, while");
        m_dbgLog.log("                  both filters in SFTest2, Group0 get aborted.");
        m_dbgLog.log("CLEANUP SEQUENCE: VK_UP : VK_EXIT : VK_EXIT : VK_DOWN : VK_EXIT : VK_EXIT");

        String expected_sequence = "1245[4]-45[4]12-45[4]-45[4]UB12-45[4]-45[4]B12-45[4]-45[4]"
                + "7+5[7]9+5[7]D+5[4]9+5[4]7+5[4]9+5[4]";

        if (m_keySequence.indexOf(expected_sequence) >= 0)
        {
            m_dbgLog.log("STATUS:           PASSED");
            m_test.assertTrue(true); // Passed the test.
        }
        else
        {
            m_dbgLog.log("STATUS:           FAILED");
            m_test.fail("Test Case 6 failed.");
        }
        m_dbgLog.log("*********************************");

        return 6;
    }

    private int TestCase7()
    {
        m_keySequence = NewLine(m_keySequence);
        m_dbgLog.log("\n\nRECORDED KEY SEQUENCE:\n" + m_keySequence);

        m_dbgLog.log("\n********** TEST CASE 7 **********");
        m_dbgLog.log("DESCRIPTION:      Test that mounting an object carousel will always pre-empt");
        m_dbgLog.log("                  one of the application filter groups if conflicts exist.");
        m_dbgLog.log("NOTE 1:           Xlet priorities are only be taken into account when the filter");
        m_dbgLog.log("                  request comes from DAVIC. Filters requested by OC or SITP will preempt");
        m_dbgLog.log("                  ANY filters currently in use by DAVIC regardless of the xlet priority.");
        m_dbgLog.log("NOTE 2:           This test case is dependent on the current channel map, and");
        m_dbgLog.log("                  on having an OC on freq=591, PID=0x120.");
        m_dbgLog.log("KEY SEQUENCE:     CHANNEL_UP ENTER 12 ---45 INFO -45 INFO 12 -45 INFO -45 INFO");
        m_dbgLog.log("                  UP_ARROW 12 -45 INFO -45 INFO ++++++ 8 9+5 INFO");
        m_dbgLog.log("                  ARROW_DOWN +5 INFO 9 +5 INFO 7 +5 INFO 9 +5 INFO");
        m_dbgLog.log("PROCEDURE:        (1) Tune to freq=591 and start SFTest1.");
        m_dbgLog.log("                  (2) Create a filter group and attach it to the stream.");
        m_dbgLog.log("                  (3) Create and start two filters on different PIDs: both should run.");
        m_dbgLog.log("                  (4) Create another filter group and attach it to the stream.");
        m_dbgLog.log("                  (5) Create and start two filters on different PIDs: both should run.");
        m_dbgLog.log("                  (6) Start SFTest2");
        m_dbgLog.log("                  (7) Repeat steps (2) and (3).");
        m_dbgLog.log("                  (8) Select PID=0x125, which contains an Object Carousel.");
        m_dbgLog.log("                  (9) Mount the Object Carousel.");
        m_dbgLog.log("                  (10)Restart existing filters to verify that at least one");
        m_dbgLog.log("                      filter group lost resources.");
        m_dbgLog.log("EXPECTED RESULTS: One of the existing filter groups is disconnected.");
        m_dbgLog.log("CLEANUP SEQUENCE: VK_EXIT : VK_EXIT : VK_UP : VK_EXIT : VK_DOWN");

        String expected_sequence_1 = ">E12---45[4]-45[4]12-45[4]-45[4]U12-45[4]-45[4]++++++89+5[7]"
                + "D+5[4]9+5[4]7+5[4]9+5[4]";

        String expected_sequence_2 = ">E12---45[4]-45[4]12-45[4]-45[4]U12-45[4]-45[4]++++++89+5[4]"
                + "D+5[7]9+5[7]7+5[4]9+5[4]";

        String expected_sequence_3 = ">E12---45[4]-45[4]12-45[4]-45[4]U12-45[4]-45[4]++++++89+5[4]"
                + "D+5[4]9+5[4]7+5[7]9+5[7]";

        if (m_keySequence.indexOf(expected_sequence_1) >= 0 || m_keySequence.indexOf(expected_sequence_2) >= 0
                || m_keySequence.indexOf(expected_sequence_3) >= 0)
        {
            m_dbgLog.log("STATUS:           PASSED");
            m_test.assertTrue(true); // Passed the test.
        }
        else
        {
            m_dbgLog.log("STATUS:           FAILED");
            m_test.fail("Test Case 7 failed.");
        }
        m_dbgLog.log("*********************************");

        return 0; // Reset the test counter.
    }

    // /////////////////////////////////////////////////////////////////////////////
    // APP STATE CHANGE //
    // /////////////////////////////////////////////////////////////////////////////

    // This method receives events when one of our test apps changes state
    public void stateChange(AppStateChangeEvent evt)
    {
        // get the AppID of the event
        AppID app = evt.getAppID();
        SFTestXletInfo xlet = m_apps[getXletIndexByAppID(app)];

        // Check to see that the Xlet was successfully started
        if (evt.getToState() == AppProxy.STARTED)
        {
            m_dbgLog.log("Xlet \"" + xlet.xletName + "\" started.");
            m_infoBox.setActiveXletState(getStateString(evt.getToState()));
            m_infoBox.repaint();

            // Grab this Xlet's control via IXC
            String name = "/" + Integer.toHexString(xlet.appID.getOID()) + "/"
                    + Integer.toHexString(xlet.appID.getAID()) + "/" + "SFTestControl" + xlet.xletName;
            try
            {
                xlet.control = (SFTestControl) IxcRegistry.lookup(m_ctx, name);
            }
            catch (Exception e)
            {
                m_dbgLog.log("Could not access SFTestControl for \"" + xlet.xletName + "\"");
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

    private void requestPATPMT(final OcapLocator locator, final int tunerIdx)
    {
        final int[] retryCount = { 0 };
        final int MAX_RETRIES = 5;
        final Object monitor = new Object();
        final boolean[] patReceived = { false };
        final ProgramAssociationTable[] pat = new ProgramAssociationTable[1];
        pat[0] = null;

        m_tuners[m_currentTunerIdx].gettingPATPMT = true;
        m_infoBox.repaint();

        // Request the PAT
        while (!patReceived[0] && retryCount[0] < MAX_RETRIES)
        {
            ProgramAssociationTableManager.getInstance().retrieveInBand(new SIRequestor()
            {

                public void notifySuccess(SIRetrievable[] result)
                {
                    pat[0] = (ProgramAssociationTable) result[0];
                    patReceived[0] = true;
                    synchronized (monitor)
                    {
                        monitor.notify();
                    }
                }

                public void notifyFailure(SIRequestFailureType reason)
                {
                    m_tuners[tunerIdx].elementaryStreams.removeAllElements();
                    m_tuners[tunerIdx].currentDrawIdx = -1;
                    m_tuners[tunerIdx].currentStreamIdx = -1;
                    m_dbgLog.log("Error getting PAT for tuner " + tunerIdx + ", locator = " + locator.toString());
                    synchronized (monitor)
                    {
                        monitor.notify();
                    }
                }

            }, locator);
            synchronized (monitor)
            {
                try
                {
                    monitor.wait(5000);
                }
                catch (InterruptedException e)
                {
                }
                retryCount[0]++;
            }
        }

        if (pat[0] == null || pat[0].getPrograms().length == 0)
        {
            m_dbgLog.log("PAT is NULL or empty for tuner " + tunerIdx + ", locator = " + locator.toString());
            m_tuners[m_currentTunerIdx].gettingPATPMT = false;
            m_infoBox.repaint();
            return;
        }

        // Request each PMT
        retryCount[0] = 0;
        boolean allPMTsReceived = false;
        final boolean[] pmts = new boolean[pat[0].getPrograms().length];
        for (int i = 0; i < pmts.length; ++i)
            pmts[i] = false;

        while (!allPMTsReceived && retryCount[0] < MAX_RETRIES)
        {
            for (int i = 0; i < pat[0].getPrograms().length; ++i)
            {
                // We may have already received this PMT
                if (pmts[i]) continue;

                // Build a new locator from the frequency and PAT program number
                OcapLocator ol = null;
                try
                {
                    ol = new OcapLocator(locator.getFrequency(), pat[0].getPrograms()[i].getProgramNumber(), -1);
                }
                catch (InvalidLocatorException e)
                {
                    m_dbgLog.log(e);
                    continue;
                }

                // Request the PMT
                final int index = i;
                final OcapLocator pmtLocator = ol;
                ProgramMapTableManager.getInstance().retrieveInBand(new SIRequestor()
                {

                    public void notifySuccess(SIRetrievable[] si)
                    {
                        // Populate our elementary stream list with all
                        // elementary stream PIDs
                        ProgramMapTable pmt = (ProgramMapTable) si[0];
                        for (int i = 0; i < pmt.getPMTElementaryStreamInfoLoop().length; ++i)
                        {
                            ElementaryStream stream = createStream(pmtLocator, pmt.getPMTElementaryStreamInfoLoop()[i]);

                            // Multiple PMTs can reference the same PIDs, so
                            // don't add dupes
                            if (!m_tuners[tunerIdx].elementaryStreams.contains(stream))
                                m_tuners[tunerIdx].elementaryStreams.addElement(stream);
                        }

                        pmts[index] = true;

                        synchronized (monitor)
                        {
                            monitor.notify();
                        }
                    }

                    public void notifyFailure(SIRequestFailureType type)
                    {
                        m_dbgLog.log("Error getting PMT for tuner " + tunerIdx + ", locator = " + pmtLocator.toString());
                        synchronized (monitor)
                        {
                            monitor.notify();
                        }
                    }

                }, pmtLocator);
                synchronized (monitor)
                {
                    try
                    {
                        monitor.wait(5000);
                    }
                    catch (InterruptedException e)
                    {
                    }
                    retryCount[0]++;
                }
            }

            // Check to see that we have received all PMTs
            allPMTsReceived = true;
            for (int p = 0; p < pmts.length; ++p)
            {
                if (!pmts[p])
                {
                    allPMTsReceived = false;
                    break;
                }
            }
        }

        if (!m_tuners[tunerIdx].elementaryStreams.isEmpty())
        {
            m_tuners[tunerIdx].currentDrawIdx = 0;
            m_tuners[tunerIdx].currentStreamIdx = 0;
        }

        m_tuners[m_currentTunerIdx].gettingPATPMT = false;
        m_infoBox.repaint();
    }

    private ElementaryStream createStream(OcapLocator programLocator, PMTElementaryStreamInfo es)
    {
        ElementaryStream stream = new ElementaryStream();
        stream.pid = es.getElementaryPID();
        stream.streamType = streamType2String(es.getStreamType());
        stream.programLocator = programLocator;

        // Check to see if this is a DSMCC object carousel PID
        Descriptor[] descriptors = es.getDescriptorLoop();
        for (int i = 0; i < descriptors.length; ++i)
        {
            Descriptor d = descriptors[i];
            if (d.getTag() == 0x13)
            {
                stream.streamType = streamType2String((short) -1);
                stream.carouselID = (d.getByteAt(0) << 24) | (d.getByteAt(1) << 16) | (d.getByteAt(2) << 8)
                        | d.getByteAt(3);
            }
        }

        return stream;
    }

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
                return "Destroy";
            case AppProxy.NOT_LOADED:
                return "Not_Loaded";
            case AppProxy.PAUSED:
                return "Paused";
            case AppProxy.STARTED:
                return "Started";
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
    // RESOURCE CONTENTION //
    // /////////////////////////////////////////////////////////////////////////////

    public ResourceUsage[] resolveResourceContention(ResourceUsage newRequest, ResourceUsage[] currentReservations)
    {
        return null;
    }

    public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
    {
    }

    // Unconditionally reject any app's request for section filter resource if
    // that app was signaled to be rejected in the runner's xlet arguments
    private class AppFilter extends AppsDatabaseFilter
    {
        public boolean accept(AppID appid)
        {
            for (int i = 0; i < m_rejects.length; ++i)
            {
                if (appid.equals(m_rejects[i])) return false;
            }
            return true;
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // PRIVATE CLASSES //
    // /////////////////////////////////////////////////////////////////////////////

    // Published via IXC. Allows test xlets to report their events to
    // the test runner
    private class FPTestEventHandler implements SFTestEvents
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
    private class SFTestUI extends Container
    {
        public SFTestUI()
        {
            super();
            setBackground(Color.black);
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
            int x = 44, y = 22;
            int dy = 14;

            g.setColor(Color.magenta);
            g.drawString("TUNER " + m_currentTunerIdx, x, y);
            y += dy;
            g.drawString(m_activeXlet + " [" + m_activeXletState + "]", x, y);
            y += dy;

            g.setColor(Color.green);
            g.drawString("CHANGE TUNER:", x, y);
            x += 15;
            y += dy;
            g.setColor(Color.white);
            g.drawString("(LEFT / RIGHT)", x, y);
            x -= 15;
            y += dy;

            g.setColor(Color.green);
            g.drawString("CHANGE FREQUENCY:", x, y);
            x += 15;
            y += dy;
            g.setColor(Color.white);
            g.drawString("(CH_UP / DOWN)", x, y);
            x -= 15;
            y += dy;

            g.setColor(Color.green);
            g.drawString("TUNE FREQUENCY:", x, y);
            x += 15;
            y += dy;
            g.setColor(Color.white);
            g.drawString("(SELECT)", x, y);
            x -= 15;
            y += dy;

            g.setColor(Color.green);
            g.drawString("CHANGE XLET:", x, y);
            x += 15;
            y += dy;
            g.setColor(Color.white);
            g.drawString("(UP / DOWN)", x, y);
            x -= 15;
            y += dy;

            g.setColor(Color.cyan);
            if (m_apps[m_curAppIndex].appProxy.getState() == AppProxy.PAUSED)
                g.drawString("(PLAY)   Resume Xlet", x, y);
            else
                g.drawString("(PLAY)   Start Xlet", x, y);
            y += dy;

            g.drawString("(STOP)   Stop Xlet", x, y);
            y += dy;
            g.drawString("(PAUSE) Pause Xlet", x, y);
            y += dy;

            // Draw service list (scrolling)
            g.setColor(Color.white);
            x = 190;
            y = 22;
            g.drawString("MHz", x, y);
            y += dy;

            int numServicesToDraw = (m_locatorList.length > MAX_CHANNELS) ? MAX_CHANNELS : m_locatorList.length;
            for (int i = m_currentDrawIdx, drawnServices = 0; drawnServices < numServicesToDraw; ++drawnServices)
            {
                String serviceString;
                g.setColor((i == m_currentFrequencyIdx) ? Color.yellow : Color.gray);

                // Draw a message string while we are in the process of tuning
                if (i == m_currentFrequencyIdx && m_tuners[m_currentTunerIdx].tuning)
                    serviceString = "Tuning...";
                else if (i == m_currentFrequencyIdx && m_tuners[m_currentTunerIdx].gettingPATPMT)
                    serviceString = "PAT/PMT...";
                else
                {
                    serviceString = "" + m_locatorList[i].getFrequency() / 1000000;

                    // Append the tuner index to the end of the service name if
                    // that
                    // tuner is currently tuned to the service
                    for (int j = 0; j < m_tuners.length; ++j)
                        if (m_tuners[j].locator != null && m_tuners[j].locator.equals(m_locatorList[i]))
                            serviceString += " (" + j + ")";
                }

                g.drawString(serviceString, x, y);
                y += dy;

                i = (i == m_locatorList.length - 1) ? 0 : i + 1;
            }
            g.setColor(Color.white);

            // Draw PMT PID list
            x = 450;
            y = 22;

            g.setColor(Color.green);
            g.drawString("CHANGE PID:", x, y);
            x += 15;
            y += dy;
            g.setColor(Color.white);
            g.drawString("(PIP_UP / DOWN)", x, y);
            x -= 15;
            y += dy;

            g.drawString("-- EStream PIDs --", x, y);
            y += dy;
            printStreams(g, x, y, dy);
            g.setColor(Color.white);

            // Xlet instructions
            x = 240;
            y = 22;

            // Only draw test control options when xlet is running
            if (m_apps[m_curAppIndex].appProxy.getState() == AppProxy.STARTED)
            {
                g.drawString("(B) Toggle group priority", x, y);
                y += dy;
                g.drawString("(C) Toggle willing to release", x, y);
                y += dy;
                g.drawString("(1) Create section filter group", x, y);
                y += dy;
                g.drawString("(2) Attach filter group", x, y);
                y += dy;
                g.drawString("(3) Detach filter group", x, y);
                y += dy;
                g.drawString("(7) Change filter group", x, y);
                y += dy;
                g.drawString("(EXIT) Delete filter group", x, y);
                y += dy;
                y += 5;
                g.drawString("(4) Create section filter", x, y);
                y += dy;
                g.drawString("(5) Start section filter", x, y);
                y += dy;
                g.drawString("(6) Stop section filter", x, y);
                y += dy;
                g.drawString("(9) Change section filter", x, y);
                y += dy;
                y += 8;
            }
            g.drawString("(8) Mount object carousel", x, y);
            y += dy;
            g.drawString("(0) Unmount object carousel", x, y);
            y += dy;
        }

        private String m_activeXlet;

        private String m_activeXletState;
    }

    private void printStreams(Graphics g, int x, int y, int dy)
    {
        Vector streams = m_tuners[m_currentTunerIdx].elementaryStreams;

        int numStreamsToDraw = (streams.size() > MAX_STREAMPIDS) ? MAX_STREAMPIDS : streams.size();
        for (int i = m_tuners[m_currentTunerIdx].currentDrawIdx, drawnStreams = 0; drawnStreams < numStreamsToDraw; ++drawnStreams)
        {
            ElementaryStream stream = (ElementaryStream) streams.elementAt(i);

            g.setColor((i == m_tuners[m_currentTunerIdx].currentStreamIdx) ? Color.yellow : Color.gray);
            g.drawString("0x" + Integer.toHexString(stream.pid) + " " + stream.streamType, x, y);
            y += dy;

            i = (i == streams.size() - 1) ? 0 : i + 1;
        }
    }

    // Setup the list of channels available
    private void buildServiceList()
    {
        final int MAX_TRIES = 10;

        // SIManager will provide us with our list of available services
        SIManager siManager = SIManager.createInstance();
        siManager.setPreferredLanguage("eng");

        final Object monitor = new Object();

        // Get Transports (assume only 1)
        Transport[] transports = siManager.getTransports();
        if (transports[0] instanceof NetworkCollection)
        {
            tries = 0;
            success = false;

            // Get the networks associated with our transport (assume only 1)
            final Network[] network = new Network[1];
            NetworkCollection nc = (NetworkCollection) transports[0];

            for (tries = 0; tries < MAX_TRIES && !success; tries++)
            {
                System.out.println("[SFTestRunnerXlet] Attempt to get networks: (" + (tries + 1) + ")\n");

                nc.retrieveNetworks(new SIRequestor()
                {
                    public void notifySuccess(SIRetrievable[] result)
                    {
                        System.out.println("[SFTestRunnerXlet] Got the networks.");

                        synchronized (monitor)
                        {
                            network[0] = (Network) result[0];
                            success = true;
                            monitor.notify();
                        }
                    }

                    public void notifyFailure(SIRequestFailureType reason)
                    {
                        if (tries == MAX_TRIES - 1)
                        {
                            m_dbgLog.log("[SFTestRunnerXlet] Failed to get networks!");
                        }

                        synchronized (monitor)
                        {
                            monitor.notify();
                        }
                    }
                });

                synchronized (monitor)
                {
                    try
                    {
                        monitor.wait();
                    }
                    catch (InterruptedException e)
                    {
                    }
                }

                if (!success)
                {
                    try
                    {
                        Thread.sleep(2000);
                    }
                    catch (Exception e2)
                    {
                    }
                }
            }

            // Get the transport streams on this network
            final Vector streams = new Vector();
            tries = 0;
            success = false;

            for (tries = 0; tries < MAX_TRIES && !success; tries++)
            {
                System.out.println("[SFTestRunnerXlet] Attempt to get transport streams: (" + (tries + 1) + ")\n");

                network[0].retrieveTransportStreams(new SIRequestor()
                {
                    public void notifySuccess(SIRetrievable[] result)
                    {
                        System.out.println("[SFTestRunnerXlet] Got the streams.");

                        synchronized (monitor)
                        {
                            for (int i = 0; i < result.length; ++i)
                            {
                                streams.addElement(result[i]);
                            }
                            success = true;
                            monitor.notify();
                        }
                    }

                    public void notifyFailure(SIRequestFailureType reason)
                    {
                        if (tries == MAX_TRIES - 1)
                        {
                            m_dbgLog.log("[SFTestRunnerXlet] Failed to get streams!");
                        }

                        synchronized (monitor)
                        {
                            monitor.notify();
                        }
                    }
                });

                synchronized (monitor)
                {
                    try
                    {
                        monitor.wait();
                    }
                    catch (InterruptedException e)
                    {
                    }
                }

                if (!success)
                {
                    try
                    {
                        Thread.sleep(2000);
                    }
                    catch (Exception e2)
                    {
                    }
                }
            }

            // Create our locator list
            m_locatorList = new OcapLocator[streams.size()];
            for (int i = 0; i < streams.size(); ++i)
            {
                TransportStream ts = (TransportStream) streams.elementAt(i);
                m_locatorList[i] = (OcapLocator) ts.getLocator();
            }
        }
        else
            m_locatorList = new OcapLocator[0];
    }

    // Reserve all tuners available on the box
    private void reserveTuners()
    {
        NetworkInterface[] tuners = NetworkInterfaceManager.getInstance().getNetworkInterfaces();
        m_tuners = new TunedFrequency[tuners.length];
        for (int i = 0; i < tuners.length; ++i)
        {
            final int idx = i;
            m_tuners[idx] = new TunedFrequency();
            m_tuners[idx].tuner = new NetworkInterfaceController(new ResourceClient()
            {

                public boolean requestRelease(ResourceProxy arg0, Object arg1)
                {
                    return false;
                }

                public void release(ResourceProxy arg0)
                {
                    m_dbgLog.log("[SFTestRunnerXlet] We were told to release tuner " + idx);
                }

                public void notifyRelease(ResourceProxy arg0)
                {
                    m_dbgLog.log("[SFTestRunnerXlet] We were told that tuner " + idx + " has beed released");
                }

            });

            // Attempt to reserve each tuner
            try
            {
                m_tuners[idx].tuner.reserve(tuners[idx], this);
            }
            catch (NetworkInterfaceException e)
            {
                m_dbgLog.log("[SFTestRunnerXlet] Could not reserve tuner " + idx);
                continue;
            }

            // Add listener to each tuner
            tuners[idx].addNetworkInterfaceListener(new NetworkInterfaceListener()
            {

                public void receiveNIEvent(NetworkInterfaceEvent event)
                {
                    if (event instanceof NetworkInterfaceTuningOverEvent)
                    {
                        NetworkInterfaceTuningOverEvent e = (NetworkInterfaceTuningOverEvent) event;
                        if (e.getStatus() == NetworkInterfaceTuningOverEvent.SUCCEEDED)
                        {
                            m_tuners[m_currentTunerIdx].locator = m_locatorList[m_currentFrequencyIdx];
                            // try { Thread.sleep(3000); } catch
                            // (InterruptedException e1) { }
                            m_tuners[m_currentTunerIdx].tuning = false;
                            requestPATPMT(m_locatorList[m_currentFrequencyIdx], m_currentTunerIdx);
                        }
                        else if (e.getStatus() == NetworkInterfaceTuningOverEvent.FAILED)
                        {
                            m_tuners[m_currentTunerIdx].locator = null;
                            m_tuners[m_currentTunerIdx].elementaryStreams.removeAllElements();
                            m_tuners[m_currentTunerIdx].tuning = false;
                            m_infoBox.repaint();
                        }
                    }
                }

            });
        }
    }

    // Stores information about a focus test Xlet
    private class SFTestXletInfo
    {
        public AppID appID;

        public AppProxy appProxy;

        public int appPriority;

        public String xletName;

        public SFTestControl control = null;
    }

    private class TunedFrequency
    {
        public OcapLocator locator = null;

        public NetworkInterfaceController tuner = null;

        boolean tuning = false;

        boolean gettingPATPMT = false;

        int currentStreamIdx = -1;

        int currentDrawIdx = 0;

        public Vector elementaryStreams = new Vector();
    }

    private class ElementaryStream
    {
        int pid;

        long carouselID = -1;

        String streamType;

        OcapLocator programLocator;

        public boolean equals(Object es)
        {
            if (es instanceof ElementaryStream)
            {
                ElementaryStream other = (ElementaryStream) es;
                return this.pid == other.pid && this.carouselID == other.carouselID;
            }
            return false;
        }
    }

    // Convert the stream type to its StreamType field name
    private String streamType2String(short streamType)
    {
        // -1 indicates an Object carousel
        if (streamType == -1) return "Object Carousel";

        Field[] fields = StreamType.class.getFields();

        for (int i = 0; i < fields.length; ++i)
        {
            try
            {
                if (fields[i].getShort(null) == streamType)
                {
                    return fields[i].getName();
                }
            }
            catch (Exception e)
            {
                m_dbgLog.log(e);
            }
        }

        return Short.toString(streamType);
    }

    // /////////////////////////////////////////////////////////////////////////////
    // DATA MEMBERS //
    // /////////////////////////////////////////////////////////////////////////////

    private XletContext m_ctx;

    private FPTestEventHandler m_testEvents = new FPTestEventHandler();

    // AppProxy List
    private SFTestXletInfo[] m_apps = null;

    private int m_curAppIndex = 0; // Index into our FocusTestXletInfo array

    // List of apps to unconditionally reject section filter resource requests
    private AppID[] m_rejects = null;

    private HScene m_scene;

    private SFTestUI m_infoBox = new SFTestUI();

    private ServiceDomain m_objectCarousel;

    private boolean m_carouselAttached = false;

    // Tuners -- There can be one TunedFrequency for each tuner on the box
    private TunedFrequency[] m_tuners = null;

    private int m_currentTunerIdx = 0;

    // List of available services
    private OcapLocator[] m_locatorList = null;

    private int m_currentDrawIdx = 0;

    private int m_currentFrequencyIdx = 0;

    private AppFilter m_resourceAppFilter = new AppFilter();

    // When this is FALSE, started section filters will filter on a garbage
    // table
    // ID. This ensure that even though a section filter resource will be
    // reserved,
    // the filter will not actually match any sections. This can keep the system
    // from bogging down too much. When this value is set to TRUE, all sections
    // on
    // the specified PID will be matched a separate application thread will run
    // to
    // dispose the matched sections
    private boolean m_doFiltering = false;

    private static final int MAX_CHANNELS = 14;

    private static final int MAX_STREAMPIDS = 10;

    private static final String TEST_XLET_ARG = "testXlet";

    private static final String REJECT_XLET_ARG = "rejectXlet";

    private static final String DO_FILTERING_ARG = "doFiltering";

    private static final String CONFIG_XLET_ARG = "configFile";

    private String m_configFile = "";

    private static final String FREQUENCY = "sfrt_frequency";

    private int m_configFrequency = 0;

    // AutoXlet stuff
    private AutoXletClient m_axc = null;

    private Logger m_dbgLog = null;

    private Test m_test = null;

    private Monitor m_eventMonitor = new Monitor();

    private int m_status = 0;

    private String m_keySequence = new String("");

    private int m_last_test_completed = 0;

    private boolean success = false;

    private int tries = 0;
}
