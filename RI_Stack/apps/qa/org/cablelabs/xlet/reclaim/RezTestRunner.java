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
package org.cablelabs.xlet.reclaim;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Vector;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsDatabase;
import org.dvb.application.CurrentServiceFilter;
import org.dvb.ui.DVBBufferedImage;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.HScreen;
import org.havi.ui.HStaticText;
import org.ocap.system.event.ErrorEvent;
import org.ocap.system.event.ResourceDepletionEvent;
import org.ocap.system.event.SystemEvent;
import org.ocap.system.event.SystemEventListener;
import org.ocap.system.event.SystemEventManager;

import org.cablelabs.test.autoxlet.AutoXletClient;
import org.cablelabs.test.autoxlet.Driveable;
import org.cablelabs.test.autoxlet.Logger;
import org.cablelabs.test.autoxlet.Test;
import org.cablelabs.test.autoxlet.TestFailure;
import org.cablelabs.test.autoxlet.TestResult;
import org.cablelabs.test.autoxlet.XletLogger;

/**
 * RezTestRunner
 * 
 * @author Joshua Keplinger
 * 
 */
public class RezTestRunner implements Xlet, SystemEventListener, Driveable
{

    private XletContext xctx;

    private AppAttributes rezHP;

    private AppAttributes rezLP;

    private String rezHPName;

    private String rezLPName;

    private TestAppStateChangeEventListener proxylistener;

    private AppsDatabase db;

    private Vector sysEvents;

    private Vector appEvents;

    private boolean handleSysEvent;

    private HScene scene;

    private boolean started = false;

    private HStaticText screenText;

    private int sysMemCount; // Number of one meg chunks of available system
                             // memory

    private static final double ONE_MEG = Math.pow(2, 20);

    private AutoXletClient axc;

    private Logger log;

    private Test test;

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#destroyXlet(boolean)
     */
    public void destroyXlet(boolean unconditional) throws XletStateChangeException
    {
        SystemEventManager.getInstance().unsetEventListener(SystemEventManager.ERROR_EVENT_LISTENER);
        SystemEventManager.getInstance().unsetEventListener(SystemEventManager.RESOURCE_DEPLETION_EVENT_LISTENER);

        db.getAppProxy(rezHP.getIdentifier()).removeAppStateChangeEventListener(proxylistener);
        db.getAppProxy(rezLP.getIdentifier()).removeAppStateChangeEventListener(proxylistener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#initXlet(javax.tv.xlet.XletContext)
     */
    public void initXlet(XletContext ctx) throws XletStateChangeException
    {
        xctx = ctx;
        sysEvents = new Vector();

        SystemEventManager.getInstance().setEventListener(SystemEventManager.ERROR_EVENT_LISTENER, this);
        SystemEventManager.getInstance().setEventListener(SystemEventManager.RESOURCE_DEPLETION_EVENT_LISTENER, this);

        axc = new AutoXletClient(this, xctx);
        test = axc.getTest();
        if (axc.isConnected())
            log = axc.getLogger();
        else
            log = new XletLogger();

        String[] args = (String[]) (ctx.getXletProperty(XletContext.ARGS));
        if (args.length < 2) throw new XletStateChangeException("High priority or low priority app was not specified");
        rezHPName = args[0];
        rezLPName = args[1];

        if (rezHPName == null || rezLPName == null)
            throw new XletStateChangeException("High priority or low priority app was not specified");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#pauseXlet()
     */
    public void pauseXlet()
    {
        scene.setVisible(false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.tv.xlet.Xlet#startXlet()
     */
    public void startXlet() throws XletStateChangeException
    {
        if (!started)
        {
            getApps();

            scene = HSceneFactory.getInstance().getFullScreenScene(
                    HScreen.getDefaultHScreen().getDefaultHGraphicsDevice());;
            scene.setLayout(new BorderLayout());
            screenText = new HStaticText("Testing memory reclamation...");
            screenText.setBackgroundMode(HStaticText.BACKGROUND_FILL);
            screenText.setForeground(new Color(234, 234, 234));
            screenText.setBackground(Color.darkGray); // temporary
            screenText.setFont(new Font("SansSerif", 0, 20));
            scene.add(screenText);
            scene.validate();
            scene.show();
            screenText.requestFocus();

            setup();
            testResourceDepletionVMSelf();
            cleanup();

            setup();
            testResourceDepletionVMMonappHandles();
            cleanup();

            setup();
            testResourceDepletionVMMonappIgnores();
            cleanup();

            setup();
            testResourceDepletionVMOOME();
            cleanup();

            setup();
            testResourceDepletionSysSelf();
            cleanup();

            setup();
            testResourceDepletionSysMonappHandles();
            cleanup();

            setup();
            testResourceDepletionSysMonappIgnores();
            cleanup();

            setup();
            testResourceDepletionSysOOME();
            cleanup();

            showResults();

            started = true;
        }

        scene.show();
        screenText.requestFocus();
    }

    private void showResults()
    {
        Color color;
        String text;
        TestResult results = test.getTestResult();
        log.log(results);
        if (results.wasSuccessful())
        {
            // all the tests passed - background color should be green
            color = Color.green.darker();
            text = "All " + results.runCount() + " tests passed";
        }
        else
        {
            // some of the tests failed - background should be red
            color = Color.red.darker();
            StringBuffer sb = new StringBuffer();
            // iterate over the list of failures and draw them on the screen
            for (Enumeration e = results.failures(); e.hasMoreElements();)
            {
                sb.append(((TestFailure) e.nextElement()).toString()).append('\n');
            }
            text = sb.toString();
        }
        screenText.setBackground(color);
        screenText.setTextContent(text, HStaticText.ALL_STATES);
        screenText.repaint();
    }

    private void getApps() throws XletStateChangeException
    {
        db = AppsDatabase.getAppsDatabase();
        Enumeration attrs = db.getAppAttributes(new CurrentServiceFilter());
        while (attrs.hasMoreElements())
        {
            AppAttributes app = (AppAttributes) attrs.nextElement();
            // Find the one high priority app
            if (rezHPName.equals(app.getName())) rezHP = app;
            // Find the one low priority app
            if (rezLPName.equals(app.getName())) rezLP = app;
        }
        if (rezHP == null || rezLP == null)
            throw new XletStateChangeException("Unable to find required apps for testing");

        // Okay, we got the two apps that we'll use for testing
        proxylistener = new TestAppStateChangeEventListener();
        db.getAppProxy(rezHP.getIdentifier()).addAppStateChangeEventListener(proxylistener);
        db.getAppProxy(rezLP.getIdentifier()).addAppStateChangeEventListener(proxylistener);

    }

    private void testResourceDepletionVMSelf()
    {
        try
        {
            long free = Runtime.getRuntime().freeMemory();
            double count = (free / ONE_MEG) + 1; // Number of 1 MB chunks
            System.out.println("Number of 1 MB chuncks is " + count);
            Object[] waste = new Object[(int) count];
            for (int i = 0; i < count; i++)
            {
                System.out.println("Processing out chunk " + count);
                waste[i] = new byte[new Double(ONE_MEG).intValue()];
            }
            test.fail("OutOfMemoryError should've been thrown");
        }
        catch (OutOfMemoryError err)
        {
            test.assertTrue(true); // Mark a test
        }
    }

    private void testResourceDepletionVMMonappHandles()
    {
        handleSysEvent = true;
        // Start up the low priority app
        boolean result = startApp(rezLP.getIdentifier(), new String[] { "low", "vm", "0" });
        if (!result) return;
        // Low priority app is started
        // Now start up the high priority app
        result = startApp(rezHP.getIdentifier(), new String[] { "high", "vm", "0" });
        if (!result) return;
        // High priority app is started
        if (!test.assertTrue("Did not receive expected ResourceDepletionEvent\n", waitForSysEvent(1))) return;
        if (!test.assertEquals("Number of SystemEvents received is incorrect\n", 1, sysEvents.size())) return;
        if (!test.assertTrue("Event is not a ResourceDepletionEvent, instead is " + sysEvents.elementAt(0).getClass()
                + "\n", sysEvents.elementAt(0) instanceof ResourceDepletionEvent)) return;
        ResourceDepletionEvent evt = (ResourceDepletionEvent) sysEvents.elementAt(0);
        test.assertEquals("Did not get correct event code", ResourceDepletionEvent.RESOURCE_VM_MEM_DEPLETED,
                evt.getTypeCode());
    }

    private void testResourceDepletionVMMonappIgnores()
    {
        handleSysEvent = false;
        // Start up the low priority app
        boolean result = startApp(rezLP.getIdentifier(), new String[] { "low", "vm", "0" });
        if (!result) return;
        // Low priority app is started
        // Now start up the high priority app
        db.getAppProxy(rezHP.getIdentifier()).start(new String[] { "high", "vm", "0" });
        // We'll wait for the low priority app to get killed off
        if (!test.assertTrue("App " + db.getAppAttributes(rezLP.getIdentifier()).getName() + " did not stop",
                proxylistener.waitForStop(rezLP.getIdentifier()))) return;
        // Now we'll wait to make sure the high priority one started
        if (!test.assertTrue("App " + db.getAppAttributes(rezHP.getIdentifier()).getName() + " did not start",
                proxylistener.waitForStart(rezHP.getIdentifier()))) return;
        // High priority app is started
        if (!test.assertTrue("Did not receive expected ResourceDepletionEvent", waitForSysEvent(1))) return;
        if (!test.assertEquals("Number of SystemEvents received is incorrect ", 1, sysEvents.size())) return;
        if (!test.assertTrue("Event is not a ResourceDepletionEvent, instead is " + sysEvents.elementAt(0).getClass(),
                sysEvents.elementAt(0) instanceof ResourceDepletionEvent)) return;
        ResourceDepletionEvent evt = (ResourceDepletionEvent) sysEvents.elementAt(0);
        test.assertEquals("Did not get correct event code", ResourceDepletionEvent.RESOURCE_VM_MEM_DEPLETED,
                evt.getTypeCode());
    }

    private void testResourceDepletionVMOOME()
    {
        handleSysEvent = false;

        // Eat up a bunch of memory
        long free = Runtime.getRuntime().freeMemory();
        double count = (free / Math.pow(2, 20)) - 1; // Number of 1 MB chunks
        Object[] waste = new Object[(int) count + 1];
        for (int i = 0; i < count; i++)
        {
            waste[i] = new byte[new Double(Math.pow(2, 20)).intValue()];
        }

        db.getAppProxy(rezHP.getIdentifier()).start(new String[] { "high", "vm", "0" });

        if (!test.assertTrue("Did not receive both events", waitForSysEvent(2))) return;
        if (!test.assertEquals("Number of SystemEvents received is incorrect \n", 2, sysEvents.size())) return;
        SystemEvent evt = (SystemEvent) sysEvents.elementAt(0);
        if (!test.assertTrue("Event is not a ResourceDepletionEvent, instead is \n" + evt.getClass(),
                evt instanceof ResourceDepletionEvent)) return;
        if (!test.assertEquals("Did not get correct event code", ResourceDepletionEvent.RESOURCE_VM_MEM_DEPLETED,
                evt.getTypeCode())) return;
        evt = (SystemEvent) sysEvents.elementAt(1);
        if (!test.assertTrue("Event is not a ErrorEvent, instead is " + evt.getClass() + "\n",
                evt instanceof ErrorEvent)) return;
        test.assertEquals("Did not get correct event code\n", ErrorEvent.SYS_CAT_JAVA_THROWABLE, evt.getTypeCode());
    }

    private void testResourceDepletionSysSelf()
    {
        // This test doubles as a OOME test and a gauge
        // to determine how much system memory we can use up.
        try
        {
            Vector images = new Vector();
            while (true) // This should eventually throw OOME
            {
                images.addElement(new DVBBufferedImage(1, 262144)); // A 1MB
                                                                    // 32-bit
                                                                    // image
                sysMemCount++;
            }
        }
        catch (OutOfMemoryError err)
        {
            test.assertTrue(true); // Mark a test
        }

    }

    private void testResourceDepletionSysMonappHandles()
    {
        handleSysEvent = true;
        // Start up the low priority app
        boolean result = startApp(rezLP.getIdentifier(), new String[] { "low", "sys", "" + sysMemCount });
        if (!result) return;
        // Low priority app is started
        // Now start up the high priority app
        result = startApp(rezHP.getIdentifier(), new String[] { "high", "sys", "0" });
        if (!result) return;
        // High priority app is started
        if (!test.assertTrue("Did not receive expected ResourceDepletionEvent", waitForSysEvent(1))) return;
        if (!test.assertEquals("Number of SystemEvents received is incorrect \n", 1, sysEvents.size())) return;
        if (!test.assertTrue("Event is not a ResourceDepletionEvent, instead is " + sysEvents.elementAt(0).getClass()
                + "\n", sysEvents.elementAt(0) instanceof ResourceDepletionEvent)) return;
        ResourceDepletionEvent evt = (ResourceDepletionEvent) sysEvents.elementAt(0);
        test.assertEquals("Did not get correct event code\n", ResourceDepletionEvent.RESOURCE_SYS_MEM_DEPLETED,
                evt.getTypeCode());
    }

    private void testResourceDepletionSysMonappIgnores()
    {
        handleSysEvent = false;
        // Start up the low priority app
        boolean result = startApp(rezLP.getIdentifier(), new String[] { "low", "sys", "" + sysMemCount });
        if (!result) return;
        // Low priority app is started
        // Now start up the high priority app
        db.getAppProxy(rezHP.getIdentifier()).start(new String[] { "high", "sys", "0" });
        // We'll wait for the low priority app to get killed off
        if (!test.assertTrue("App " + db.getAppAttributes(rezLP.getIdentifier()).getName() + " did not stop",
                proxylistener.waitForStop(rezLP.getIdentifier()))) return;
        // Now we'll wait to make sure the high priority one started
        if (!test.assertTrue("App " + db.getAppAttributes(rezHP.getIdentifier()).getName() + " did not start",
                proxylistener.waitForStart(rezHP.getIdentifier()))) return;
        // High priority app is started
        if (!test.assertTrue("Did not receive expected ResourceDepletionEvent", waitForSysEvent(1))) return;
        if (!test.assertEquals("Number of SystemEvents received is incorrect \n", 1, sysEvents.size())) return;
        if (!test.assertTrue("Event is not a ResourceDepletionEvent, instead is " + sysEvents.elementAt(0).getClass()
                + "\n", sysEvents.elementAt(0) instanceof ResourceDepletionEvent)) return;
        ResourceDepletionEvent evt = (ResourceDepletionEvent) sysEvents.elementAt(0);
        test.assertEquals("Did not get correct event code\n", ResourceDepletionEvent.RESOURCE_SYS_MEM_DEPLETED,
                evt.getTypeCode());
    }

    private void testResourceDepletionSysOOME()
    {
        handleSysEvent = false;

        // Eat up a bunch of memory
        Object[] waste = new Object[sysMemCount];
        for (int i = 0; i < sysMemCount; i++)
        {
            waste[i] = new DVBBufferedImage(1, 262144); // 1MB image
        }

        db.getAppProxy(rezHP.getIdentifier()).start(new String[] { "high", "sys", "0" });

        if (!test.assertTrue("Did not receive both events", waitForSysEvent(2))) return;
        if (!test.assertEquals("Number of SystemEvents received is incorrect \n", 2, sysEvents.size())) return;
        SystemEvent evt = (SystemEvent) sysEvents.elementAt(0);
        if (!test.assertTrue("Event is not a ResourceDepletionEvent, instead is " + evt.getClass() + "\n",
                evt instanceof ResourceDepletionEvent)) return;
        if (!test.assertEquals("Did not get correct event code\n", ResourceDepletionEvent.RESOURCE_SYS_MEM_DEPLETED,
                evt.getTypeCode())) return;
        evt = (SystemEvent) sysEvents.elementAt(1);
        if (!test.assertTrue("Event is not a ErrorEvent, instead is " + evt.getClass() + "\n",
                evt instanceof ErrorEvent)) return;
        test.assertEquals("Did not get correct event code", ErrorEvent.SYS_CAT_JAVA_THROWABLE, evt.getTypeCode());
    }

    // We want a fresh clean slate for memory
    private void setup()
    {
        freeMem();

        appEvents = new Vector();
        sysEvents = new Vector();
    }

    private void cleanup()
    {
        if (db.getAppProxy(rezLP.getIdentifier()).getState() == AppProxy.STARTED)
        {
            db.getAppProxy(rezLP.getIdentifier()).stop(true);
            proxylistener.waitForStop(rezLP.getIdentifier());
        }

        if (db.getAppProxy(rezHP.getIdentifier()).getState() == AppProxy.STARTED)
        {
            db.getAppProxy(rezHP.getIdentifier()).stop(true);
            proxylistener.waitForStop(rezHP.getIdentifier());
        }
    }

    private void freeMem()
    {
        System.gc();
        System.runFinalization();
        System.gc();
        System.runFinalization();
    }

    private boolean startApp(AppID id, String[] args)
    {
        AppProxy proxy = db.getAppProxy(id);
        proxy.start(args);
        return test.assertTrue("App " + db.getAppAttributes(id).getName() + " did not start",
                proxylistener.waitForStart(id));
    }

    private boolean stopApp(AppID id)
    {
        AppProxy proxy = db.getAppProxy(id);
        proxy.stop(true);
        proxylistener.waitForStop(id);
        return test.assertEquals("App " + db.getAppAttributes(id).getName() + " did not stop", AppProxy.DESTROYED,
                proxy.getState());
    }

    private boolean waitForSysEvent(int count)
    {
        synchronized (sysEvents)
        {
            long startTime = System.currentTimeMillis();
            while (sysEvents.size() < count && System.currentTimeMillis() < startTime + 60000)
            {
                try
                {
                    sysEvents.wait(5000);
                }
                catch (InterruptedException ex)
                {

                }
            }
            if (sysEvents.size() >= count)
                return true;
            else
                return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.system.event.SystemEventListener#notifyEvent(org.ocap.system
     * .event.SystemEvent)
     */
    public void notifyEvent(SystemEvent event)
    {
        synchronized (sysEvents)
        {
            if (handleSysEvent
                    && (event.getTypeCode() == ResourceDepletionEvent.RESOURCE_SYS_MEM_DEPLETED || event.getTypeCode() == ResourceDepletionEvent.RESOURCE_VM_MEM_DEPLETED))
            {
                stopApp(rezLP.getIdentifier());
            }
            sysEvents.addElement(event);
            sysEvents.notify();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.test.autoxlet.Driveable#dispatchEvent(java.awt.event.KeyEvent
     * , boolean, int)
     */
    public void dispatchEvent(KeyEvent event, boolean useMonitor, int monitorTimeout) throws RemoteException
    {
        // This app isn't key driven
    }

    private class TestAppStateChangeEventListener implements AppStateChangeEventListener
    {

        public void stateChange(AppStateChangeEvent evt)
        {
            synchronized (appEvents)
            {
                appEvents.addElement(evt);
                appEvents.notify();
            }
        }

        public boolean waitForStart(AppID id)
        {
            return waitForEvent(AppProxy.STARTED, id);
        }

        public boolean waitForStop(AppID id)
        {
            return waitForEvent(AppProxy.DESTROYED, id);
        }

        private boolean waitForEvent(int event, AppID id)
        {
            long startTime = System.currentTimeMillis();
            synchronized (appEvents)
            {
                while (!checkEvent(event, id) && System.currentTimeMillis() < startTime + 15000)
                {
                    try
                    {
                        appEvents.wait(500);
                    }
                    catch (InterruptedException exc)
                    {
                        break;
                    }
                }
                return checkEvent(event, id);
            }
        }

        private boolean checkEvent(int event, AppID id)
        {
            for (int i = 0; i < appEvents.size(); i++)
            {
                AppStateChangeEvent appEvt = (AppStateChangeEvent) appEvents.elementAt(i);
                if (event == appEvt.getToState() && id.equals(appEvt.getAppID())) return true;
            }
            return false;
        }

    }

}
