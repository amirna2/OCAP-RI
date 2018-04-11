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
package org.cablelabs.xlet.monappautotest;

import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.security.PermissionCollection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.rmi.AlreadyBoundException;
import java.rmi.Remote;

import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.io.ixc.IxcRegistry;
import org.ocap.OcapSystem;
import org.ocap.application.AppFilter;
import org.ocap.application.AppManagerProxy;
import org.ocap.application.AppPattern;
import org.ocap.application.AppSignalHandler;
import org.ocap.application.OcapAppAttributes;
import org.ocap.application.PermissionInformation;
import org.ocap.application.SecurityPolicyHandler;
import org.ocap.net.OcapLocator;
import org.ocap.resource.ResourceContentionHandler;
import org.ocap.resource.ResourceContentionManager;
import org.ocap.resource.ResourceUsage;

import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Monitor;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.XletLogger;

import org.cablelabs.xlet.monappautotest.MonAppAutoTestConstants;

public class MonAppAutoTest implements Xlet, AppStateChangeEventListener, AppSignalHandler, ResourceContentionHandler,
        SecurityPolicyHandler, Driveable
{

    private static final boolean DEBUG = true;

    private boolean started = false;

    private XletContext ctx;

    private AppManagerProxy appmgr;

    Test test;

    MonAppAutoXletInfo[] appsInfo = null;

    MonAppAutoTestResultHandler ixcHandler;

    AppID runnerAppID;

    int currStep;

    int testNum;

    AutoXletClient axc;

    Logger dbgLog = null;

    // /////////////////////////////////////////////////////////////////////////////
    // Constructor //
    // Only purpose is to invoke monitorConfiguringSignal() as soon as possible.
    // //
    // /////////////////////////////////////////////////////////////////////////////

    public MonAppAutoTest()
    {
        try
        {
            OcapSystem.monitorConfiguringSignal(0, 0);
        }
        catch (Throwable e)
        {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // XLET FUNCTIONS //
    // /////////////////////////////////////////////////////////////////////////////

    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        dbgLog.log("destroyXlet(" + unconditional + ")");
        if (!unconditional) throw new XletStateChangeException("Don't want to go away");
    }

    public void initXlet(XletContext ctx) throws XletStateChangeException
    {

        // Connect to AutoXlet framework and set up a logger
        axc = new AutoXletClient(this, ctx);

        if (axc.isConnected())
            dbgLog = axc.getLogger();
        else
            dbgLog = new XletLogger();

        debug("initXlet");

        test = axc.getTest();
        this.ctx = ctx;

        runnerAppID = getAppID();
        dbgLog.log("name: " + getAppName());
        dbgLog.log("id:   " + runnerAppID);
        dbgLog.log("svc:  " + getService());

        AppsDatabase adb = AppsDatabase.getAppsDatabase();

        // Each argument is a complete 48-bit AppID integer (orgID,appID)
        // in hex (0x) string format. Each argument indicates the monitor app
        // test xlet to launch
        String[] args = (String[]) (ctx.getXletProperty(XletContext.ARGS));

        // Populate our app proxies array from the arguments
        appsInfo = new MonAppAutoXletInfo[args.length];

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
                throw new XletStateChangeException("Could not get mon app test xlet AppProxy!  " + testAppID);

            appProxy.addAppStateChangeEventListener(this);
            appsInfo[i] = new MonAppAutoXletInfo();
            appsInfo[i].appID = testAppID;
            appsInfo[i].appProxy = appProxy;
            appsInfo[i].appName = adb.getAppAttributes(testAppID).getName();
            appsInfo[i].appData = new MonAppAutoTestData();
            appsInfo[i].appMon = new Monitor();
        }

        ixcHandler = new MonAppAutoTestResultHandler();

    }

    public void pauseXlet()
    {
        debug("pauseXlet");
    }

    // /////////////////////////////////////////////////////////////////////////////
    // startXlet Function //
    // Most of the work happens here. Maybe it shouldn't? //
    // /////////////////////////////////////////////////////////////////////////////

    public void startXlet() throws XletStateChangeException
    {
        AppFilter appfilter = new AppFilter();

        dbgLog.log("startXlet");
        if (!started)
        {
            initialize();
        }

        for (testNum = 0; testNum < MonAppAutoTestConstants.NUMMONAPPTESTS; testNum++)
        {
            TestSequence currSequence = new TestSequence(testNum, appsInfo);

            currStep = currSequence.getTestStep();
            while (currStep >= 0)
            {
                switch (testNum)
                {
                    case MonAppAutoTestConstants.APPDENY:
                        if (currStep == 1)
                        {
                            appfilter = currSequence.getAppFilter();
                            appmgr.setAppFilter(appfilter);
                        }

                        launchEm(currSequence, appsInfo);
                        syncWithSubs(currSequence, appsInfo);
                        break;

                    case MonAppAutoTestConstants.APPALLOW:
                    case MonAppAutoTestConstants.APPASK:
                    case MonAppAutoTestConstants.REZDENYSECTION:
                    case MonAppAutoTestConstants.REZDENYNIC:
                    case MonAppAutoTestConstants.REZDENYBGDEV:
                    case MonAppAutoTestConstants.REZDENYGFXDEV:
                    case MonAppAutoTestConstants.REZDENYVIDEODEV:
                    case MonAppAutoTestConstants.REZDENYVBI:
                    case MonAppAutoTestConstants.REZALLOW:
                    case MonAppAutoTestConstants.REZASK:
                    case MonAppAutoTestConstants.REZCONTRELEASE:
                    case MonAppAutoTestConstants.REZCONTDEFAULT:
                    case MonAppAutoTestConstants.REZCONTALLDENY:
                    case MonAppAutoTestConstants.REZCONTNEVER:
                    case MonAppAutoTestConstants.REZCONTALWAYS:
                    case MonAppAutoTestConstants.PERMADD:
                    case MonAppAutoTestConstants.PERMDENY:

                    default:
                        break;
                }

                // Clean up filter
                if (appfilter != null)
                {
                    appmgr.setAppFilter(null);
                    appfilter = null;
                }

                // Get results

                for (int i = 0; i < appsInfo.length; i++)
                {
                    if (currSequence.getLaunchDecision(i))
                    {
                        test.assertTrue(appsInfo[i].appData.equals(currSequence.appSeqData[i].expectData));
                    }
                }

                test.assertTrue("Contention handler call/expectation don't match!", currSequence.getContentionMatch());

                test.getTestResult();

                // Get next step
                currStep = currSequence.getTestStep();

            }
        }
    }

    /**
     * Initializes the app.
     */
    private void initialize() throws XletStateChangeException
    {

        // Set up handlers
        // Get AppManagerProxy instance
        appmgr = AppManagerProxy.getInstance();

        // Publish test data setters via IXC
        try
        {
            IxcRegistry.bind(ctx, "MonAppAutoTestIxc", ixcHandler);
        }
        catch (AlreadyBoundException e)
        {
            throw new XletStateChangeException("MonAppAutoTestSetter name already bound via IXC!");
        }

        /*
         * // SecurityPolicyHandler appmgr.setSecurityPolicyHandler(this);
         * 
         * // ResourceContentionHandler ResourceContentionManager rezmgr =
         * ResourceContentionManager.getInstance();
         * rezmgr.setResourceContentionHandler(this);
         * 
         * // ResourceFilter for(Enumeration e = rezFilter.keys();
         * e.hasMoreElements();) { String proxy = (String)e.nextElement();
         * AppFilter f = (AppFilter)rezFilter.get(proxy);
         * 
         * rezmgr.setResourceFilter(f, proxy); }
         */
        // Finally, signal configured
        try
        {
            // TODO - switch to signature with no args for 8300 build
            OcapSystem.monitorConfiguredSignal();
            started = true;
        }
        catch (Throwable e)
        {
            dbgLog.log(e.toString());
            e.printStackTrace();
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // App Signal Handler Methods //
    // /////////////////////////////////////////////////////////////////////////////

    public boolean notifyXAITUpdate(OcapAppAttributes[] newApps)
    {
        // We should never see this in our test environment...
        return true;
    }

    // /////////////////////////////////////////////////////////////////////////////
    // Resource Contention Handler Methods //
    // /////////////////////////////////////////////////////////////////////////////

    public ResourceUsage[] resolveResourceContention(ResourceUsage newRequest, ResourceUsage[] currentReservations)
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void resourceContentionWarning(ResourceUsage newRequest, ResourceUsage[] currentReservations)
    {
        // TODO Auto-generated method stub

    }

    // /////////////////////////////////////////////////////////////////////////////
    // Permission Handler Methods //
    // /////////////////////////////////////////////////////////////////////////////

    public PermissionCollection getAppPermissions(PermissionInformation permissionInfo)
    {
        // TODO Auto-generated method stub
        return null;
    }

    // /////////////////////////////////////////////////////////////////////////////
    // Test Steps //
    // /////////////////////////////////////////////////////////////////////////////

    private class TestSequence
    {
        int testStep = 0;

        int testID;

        AppFilter appFilter;

        Hashtable rezFilter;

        AppPattern[] pattern;

        AppSequenceData[] appSeqData;

        boolean contentionHandlerCalled = false;

        boolean contentionHandlerExpected = false;

        final int HIPRIO = 254;

        final int LOPRIO = 100;

        final int[] stepsPerTest = { 2, 2, 1, 2, 2, 2, 2, 2, 2, 2, 1, 2, 2, 2, 2, 2, 1, 1 };

        TestSequence(int testNum, MonAppAutoXletInfo[] appsInfo)
        {
            testID = testNum;
            appSeqData = new AppSequenceData[stepsPerTest[testNum]];
            for (int i = 0; i < appsInfo.length; i++)
            {
                appSeqData[i] = new AppSequenceData(appsInfo[i].oid, appsInfo[i].aid);
            }
        }

        // Return the next step number:
        // -1 if the test is over
        // 0-n for the next step. In this case, set up the filters and result
        // expectations for the step

        int getTestStep()
        {
            if (testStep >= stepsPerTest[testID])
            {
                return -1;
            }
            else
            {
                // Set up the data for this step

                for (int i = 0; i < appSeqData.length; i++)
                {
                    appSeqData[i].launchApp = false;
                }

                switch (testID)
                {
                    case MonAppAutoTestConstants.APPDENY:
                        appSeqData[0].launchApp = appSeqData[1].launchApp = true;
                        if (testStep == 0)
                        {
                            appSeqData[0].expectData = new MonAppAutoTestData(false, true, false, false, false, false);
                            appSeqData[1].expectData = new MonAppAutoTestData(false, true, false, false, false, false);
                        }
                        else
                        {
                            appSeqData[0].expectData = new MonAppAutoTestData(true, false, false, false, false, false);
                            appSeqData[1].expectData = new MonAppAutoTestData(true, true, false, false, false, false);

                            pattern[0] = new AppPattern(appSeqData[0].appFilterStr, AppPattern.DENY, LOPRIO);
                            appFilter = new AppFilter(pattern);
                        }
                        break;

                    case MonAppAutoTestConstants.APPALLOW:
                    case MonAppAutoTestConstants.APPASK:
                    case MonAppAutoTestConstants.REZDENYSECTION:
                    case MonAppAutoTestConstants.REZDENYNIC:
                    case MonAppAutoTestConstants.REZDENYBGDEV:
                    case MonAppAutoTestConstants.REZDENYGFXDEV:
                    case MonAppAutoTestConstants.REZDENYVIDEODEV:
                    case MonAppAutoTestConstants.REZDENYVBI:
                    case MonAppAutoTestConstants.REZALLOW:
                    case MonAppAutoTestConstants.REZASK:
                    case MonAppAutoTestConstants.REZCONTRELEASE:
                    case MonAppAutoTestConstants.REZCONTDEFAULT:
                    case MonAppAutoTestConstants.REZCONTALLDENY:
                    case MonAppAutoTestConstants.REZCONTNEVER:
                    case MonAppAutoTestConstants.REZCONTALWAYS:
                    case MonAppAutoTestConstants.PERMADD:
                    case MonAppAutoTestConstants.PERMDENY:

                    default:
                        break;
                }

                // Return the step number
                return testStep++;
            }
        }

        boolean getLaunchDecision(int which)
        {
            return appSeqData[which].launchApp;
        }

        MonAppAutoTestData getExpectedResults(int which)
        {
            return appSeqData[which].expectData;
        }

        AppFilter getAppFilter()
        {
            return appFilter;
        }

        Hashtable getRezFilter()
        {
            return rezFilter;
        }

        boolean getContentionMatch()
        {
            return contentionHandlerCalled == contentionHandlerExpected;
        }

        void setContentionHandlerCalled()
        {
            contentionHandlerCalled = true;
        }

        class AppSequenceData
        {
            String appFilterStr;

            boolean launchApp;

            MonAppAutoTestData expectData;

            AppSequenceData(int oid, int aid)
            {
                appFilterStr = new String(Integer.toString(oid, 16) + ":" + Integer.toString(aid, 16));
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // Xlet Data //
    // /////////////////////////////////////////////////////////////////////////////

    private class MonAppAutoXletInfo
    {
        int oid;

        int aid;

        public AppID appID;

        public AppProxy appProxy;

        public String appName;

        public Monitor appMon;

        MonAppAutoTestData appData;
    }

    // /////////////////////////////////////////////////////////////////////////////
    // Report Results from Sub Xlets //
    // /////////////////////////////////////////////////////////////////////////////

    private class MonAppAutoTestResultHandler implements MonAppAutoTestIxc
    {
        public void reportResults(int subXlet, String result)
        {
            appsInfo[subXlet].appData.setTestDataResult(result);
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // Test Data //
    // /////////////////////////////////////////////////////////////////////////////

    private class MonAppAutoTestData implements Remote
    {
        boolean filtered;

        boolean started;

        boolean allowed;

        boolean requestRelease;

        boolean release;

        boolean notifyRelease;

        final String[] items = { "filtered", "started", "allowed", "requestRelease", "release", "notifyRelease" };

        public MonAppAutoTestData()
        {
            resetTestData();
        }

        public MonAppAutoTestData(boolean f, boolean s, boolean a, boolean rR, boolean r, boolean nR)
        {
            filtered = f;
            started = s;
            allowed = a;
            requestRelease = rR;
            release = r;
            notifyRelease = nR;
        }

        public void resetTestData()
        {
            filtered = false;
            started = false;
            allowed = false;
            requestRelease = false;
            release = false;
            notifyRelease = false;
        }

        public void setTestDataResult(String item)
        {
            int i;
            for (i = 0; i < items.length; i++)
            {
                if (items[i].equalsIgnoreCase(item)) break;
            }

            switch (i)
            {
                case 0:
                    filtered = true;
                    break;

                case 1:
                    started = true;
                    break;

                case 2:
                    allowed = true;
                    break;

                case 3:
                    requestRelease = true;
                    break;

                case 4:
                    release = true;
                    break;

                case 5:
                    notifyRelease = true;
                    break;

                default:
            }
        }

        public boolean getTestDataResult(String item)
        {
            int i;
            boolean result;
            for (i = 0; i < items.length; i++)
            {
                if (items[i].equalsIgnoreCase(item)) break;
            }

            switch (i)
            {
                case 0:
                    result = filtered;
                    break;

                case 1:
                    result = started;
                    break;

                case 2:
                    result = allowed;
                    break;

                case 3:
                    result = requestRelease;
                    break;

                case 4:
                    result = release;
                    break;

                case 5:
                    result = notifyRelease;
                    break;

                default:
                    result = false;
            }
            return result;
        }

        public boolean equals(MonAppAutoTestData td)
        {
            if ((filtered == td.getTestDataResult("filtered")) && (started == td.getTestDataResult("started"))
                    && (allowed == td.getTestDataResult("allowed"))
                    && (requestRelease == td.getTestDataResult("requestRelease"))
                    && (release == td.getTestDataResult("release"))
                    && (notifyRelease == td.getTestDataResult("notifyRelease")))
                return true;
            else
                return false;
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    // APP STATE CHANGE //
    // /////////////////////////////////////////////////////////////////////////////

    // This method receives events when one of our test apps changes state
    public void stateChange(AppStateChangeEvent evt)
    {
        MonAppAutoXletInfo axi;
        MonAppAutoTestData atd;
        Monitor mon;

        // get the AppID of the event
        AppID app = evt.getAppID();

        int index = getXletIndexByAppID(app);

        axi = appsInfo[index];
        mon = axi.appMon;

        if (evt.hasFailed())
        {
            dbgLog.log("App " + axi.appName + " failed.");
            // Signal the test loop that this app has failed
            mon.notifyReady();
        }
        else
        {
            atd = axi.appData;

            // Check to see that the Xlet was successfully started
            if (evt.getToState() == AppProxy.STARTED)
            {
                debug("App " + axi.appName + " started.");
                atd.setTestDataResult("started");
            }
            /*
             * // Check to see that the Xlet was successfully paused if
             * (evt.getToState() == AppProxy.PAUSED) { m_dbgLog.log("Xlet \"" +
             * xlet.xletName + "\" paused.");
             * m_infoBox.setActiveXletState(getStateString(evt.getToState()));
             * m_infoBox.repaint();
             * 
             * // Remove from activable list
             * m_activableWindows.removeElement(xlet);
             * 
             * m_eventMonitor.notifyReady(); }
             */
            // Check to see that the Xlet was successfully stopped
            if (evt.getToState() == AppProxy.NOT_LOADED)
            {
                debug("App " + axi.appName + " stopped.");

                // Signal the test loop that this app has completed its test
                mon.notifyReady();
            }
        }
    }

    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
        // Don't do anything. We need this method to be Driveable, which we need
        // to use
        // logging
    }

    // /////////////////////////////////////////////////////////////////////////////
    // Utility methods //
    // /////////////////////////////////////////////////////////////////////////////

    // Returns the index in our apps array of the xlet described by the given
    // AppID
    private int getXletIndexByAppID(AppID appID)
    {
        for (int i = 0; i < appsInfo.length; ++i)
            if (appsInfo[i].appID.equals(appID)) return i;

        return -1;
    }

    public void debug(String message)
    {
        if (DEBUG) dbgLog.log("[MonAppAutoTest:DEBUG] - " + message);
    }

    /*
     * public void log(String message) { Date d = new Date();
     * System.out.println(d+": "+message); }
     * 
     * public void info(String message) { log("[MonAppAutoTest] - "+message); }
     * 
     * public void error(String message) {
     * log("[MonAppAtotest:ERROR] - "+message); }
     * 
     * public void error(String message, Throwable e) { error(e.toString());
     * e.printStackTrace(); }
     */
    private AppID getAppID()
    {
        String aidStr = (String) ctx.getXletProperty("dvb.app.id");
        String oidStr = (String) ctx.getXletProperty("dvb.org.id");

        if (aidStr == null || oidStr == null) return null;

        int aid = Integer.parseInt(aidStr, 16);
        long oid = Long.parseLong(oidStr, 16);

        return new AppID((int) oid, aid);
    }

    private String getAppName()
    {
        AppID id = getAppID();
        if (id == null) return null;
        AppsDatabase db = AppsDatabase.getAppsDatabase();
        AppAttributes info = db.getAppAttributes(id);
        return info.getName();
    }

    private OcapLocator getService()
    {
        AppID id = getAppID();
        AppsDatabase db = AppsDatabase.getAppsDatabase();
        AppAttributes attr;
        OcapLocator service;
        if (id != null && db != null && (attr = db.getAppAttributes(id)) != null
                && (service = (OcapLocator) attr.getServiceLocator()) != null)
        {
            return service;
        }
        else
        {
            try
            {
                try
                {
                    Thread.sleep(500);
                }
                catch (Exception e)
                {
                }
                ServiceContext sc = ServiceContextFactory.getInstance().getServiceContext(ctx);
                return (OcapLocator) sc.getService().getLocator();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return null;
            }
        }
    }

    /*
     * private AppID parseAppID(String appid) { try { int i; if ( (i =
     * appid.indexOf(':')) < 0 ) { dbgLog.log("Cannot parse \""+appid+"\"");
     * return null; } String oid = appid.substring(0, i); String aid =
     * appid.substring(i+1);
     * 
     * int OID = Integer.parseInt(oid, 16); int AID = Integer.parseInt(aid, 16);
     * 
     * return new AppID(OID, AID); } catch(Exception e) { dbgLog.log(e); return
     * null; } }
     */
    private void launchEm(TestSequence ts, MonAppAutoXletInfo[] appInfo)
    {
        int launchIndex = 0;

        for (int i = 0; i < appInfo.length; i++)
        {
            if (ts.getLaunchDecision(i))
            {
                String[] args = new String[4];
                args[0] = new String(this.runnerAppID.toString());
                args[1] = Integer.toString(launchIndex++);
                args[2] = Integer.toString(this.testNum);
                args[3] = Integer.toString(this.currStep);
                appInfo[i].appProxy.start(args);
            }
        }
    }

    private void syncWithSubs(TestSequence ts, MonAppAutoXletInfo[] appInfo)
    {
        for (int i = 0; i < appInfo.length; i++)
        {
            if (ts.getLaunchDecision(i))
            {
                appInfo[i].appMon.waitForReady();
            }
        }
    }
}
