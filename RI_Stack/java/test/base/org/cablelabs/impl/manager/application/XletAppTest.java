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

package org.cablelabs.impl.manager.application;

import org.cablelabs.impl.io.AsyncLoadCallback;
import org.cablelabs.impl.io.AsyncLoadHandle;
import org.cablelabs.impl.io.FileData;
import org.cablelabs.impl.io.FileDataImpl;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.OpenFile;
import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.TestUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.Permission;
import java.security.Policy;
import java.security.cert.X509Certificate;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.xlet.XletStateChangeException;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppProxyTest;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseFilter;
import org.dvb.application.DVBJProxy;
import org.dvb.application.DVBJProxyTest;
import org.dvb.application.AppProxyTest.Listener;
import org.ocap.application.OcapAppAttributes;

/**
 * Tests the XletApp implementation.
 * 
 * @author Aaron Kamienski
 */
public class XletAppTest extends ApplicationTest
{
    private static final long WAIT_TIME = 5000;

    public static void doDestroyed(XletApp app) throws Exception
    {
        Listener l = new Listener();
        app.addAppStateChangeEventListener(l);

        synchronized (l)
        {
            l.clear();
            app.stop(true);
            l.waitNextEvent(WAIT_TIME);
        }
        app.removeAppStateChangeEventListener(l);

        int state = app.getState();
        if (state != AppProxy.NOT_LOADED && state != AppProxy.DESTROYED)
            fail("Expected NOT_LOADED or DESTROYED; not " + state);
        // AppProxyTest.checkState(app, AppProxy.DESTROYED);
    }

    /*
     * public static void doDispose(XletApp app) throws Exception { Listener l =
     * new Listener(); app.addAppStateChangeEventListener(l);
     * 
     * synchronized(l) { l.clear(); app.dispose();
     * l.waitEvent(AppProxy.DESTROYED, WAIT_TIME); }
     * app.removeAppStateChangeEventListener(l);
     * 
     * int state = app.getState(); AppProxyTest.checkState(app,
     * AppProxy.DESTROYED); }
     */
    public static void doStarted(XletApp app) throws Exception
    {
        if (app.getState() == AppProxy.STARTED) return;

        Listener l = new Listener();
        app.addAppStateChangeEventListener(l);

        synchronized (l)
        {
            l.clear();
            app.start();
            l.waitNextEvent(WAIT_TIME);
        }
        app.removeAppStateChangeEventListener(l);
        AppProxyTest.checkState(app, AppProxy.STARTED);
    }

    public static void doPaused(XletApp app) throws Exception
    {
        if (app.getState() == AppProxy.PAUSED) return;
        if (app.getState() != AppProxy.STARTED) doStarted(app);

        Listener l = new Listener();
        app.addAppStateChangeEventListener(l);

        synchronized (l)
        {
            l.clear();
            app.pause();
            l.waitNextEvent(WAIT_TIME);
        }
        app.removeAppStateChangeEventListener(l);
        AppProxyTest.checkState(app, AppProxy.PAUSED);
    }

    public static void doResumed(XletApp app) throws Exception
    {
        Listener l = new Listener();
        app.addAppStateChangeEventListener(l);

        doPaused(app);
        synchronized (l)
        {
            l.clear();
            app.resume();
            l.waitNextEvent(WAIT_TIME);
        }
        app.removeAppStateChangeEventListener(l);
        AppProxyTest.checkState(app, AppProxy.STARTED);
    }

    /**
     * Tests the constructor.
     */
    public void testConstructor()
    {
        TestUtils.testNoPublicConstructors(XletApp.class);

        AppEntry info = new AppEntry();
        info.id = new AppID(TestAppIDs.XLETAPP, 1);
        info.version = 1;
        AppDomainImpl domain = new AppDomainImpl(null, null);
        XletApp app = new XletApp(info, domain);

        assertSame("The constructor should create the app with the given id", info.id, app.getAppID());
        assertSame("The constructor should create the app with the given AppDomainImpl", domain, app.getAppDomain());
        assertEquals("The initial state should be NOT_LOADED", AppProxy.NOT_LOADED, app.getState());
    }

    /**
     * Used to test for access following entry into DESTROYED state.
     */
    private void doTestDestroyedAccess(String desc, Class testClass) throws Exception
    {
        // Install custom SecurityManager
        ProxySecurityManager.install();
        DummySecurity sm = new DummySecurity();
        ProxySecurityManager.push(sm);

        try
        {
            XletApp evil = createEvilApp(testClass);
            xletapp = evil; // so it will be cleaned up in tearDown

            // Get the app started
            doStarted(evil);

            // !!!!! NOTE !!!!!
            // This class will fail with an ERROR if run within the
            // awtui/swingui.
            // This is because we end up using the JUnit class loader but the
            // AppClassLoader
            // will fall back on the system class loader. This is The
            // TestCaseClassLoader's fault.
            // Need to turn OFF "Reload classes every run" in the gui!
            JavaTVXlet jtvXlet = (JavaTVXlet) evil.getXlet();
            EvilXlet xlet = (EvilXlet) jtvXlet.getXlet();

            // Shut it down
            doDestroyed(evil);

            sm.setFailAllChecks();

            // Tell it to do it's stuff
            xlet.signal.signalLatch();
            // Wait for it to finish
            assertTrue(desc + ": did not signal that it tried something", xlet.done.waitLatch(WAIT_TIME));

            // Verify the error
            assertFalse(desc + ": should not have been able to perform it's task", xlet.okay);

            assertNotNull(desc + ": An exception should've been thrown and caught", xlet.e);
            // NOTE: the exception may have been the interrupted exception when
            // the shutdown code interrupted the app... makes this kinda hard to
            // really test...
            /*
             * assertTrue(desc+": The exception should be a RuntimeException",
             * xlet.e instanceof RuntimeException);
             */

            assertFalse(desc + ": The exception shouldn't be a NullPointerException",
                    xlet.e instanceof NullPointerException);
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Verify the ability of the XletApp to restrict the xlet's access to the
     * system upon destruction.
     * <p>
     * Disabled. This is better tested via a "system test xlet". The AppContent
     * shutdown methods make it difficult to test here, but the testing of that
     * should be sufficient.
     */
    public void XtestDestroyedThread() throws Exception
    {
        doTestDestroyedAccess("Thread", EvilXletThread.class);
    }

    /**
     * Verify the ability of the XletApp to restrict the xlet's access to the
     * system upon destruction.
     * <p>
     * Disabled. This is better tested via a "system test xlet". The AppContent
     * shutdown methods make it difficult to test here, but the testing of that
     * should be sufficient.
     */
    public void XtestDestroyedIO() throws Exception
    {
        doTestDestroyedAccess("I/O", EvilXletIO.class);
    }

    /**
     * Verify the ability of the XletApp to restrict the xlet's access to the
     * system upon destruction.
     * <p>
     * Disabled. This is better tested via a "system test xlet". The AppContent
     * shutdown methods make it difficult to test here, but the testing of that
     * should be sufficient.
     */
    public void XtestDestroyedAPI() throws Exception
    {
        doTestDestroyedAccess("API", EvilXletAPI.class);
    }

    private static XletApp createEvilApp(Class evilAppClass)
    {
        AppEntry info = new AppEntry();
        info.id = new AppID(TestAppIDs.XLETAPP, 999);
        info.version = 1;
        info.controlCode = OcapAppAttributes.PRESENT;
        info.parameters = new String[0];
        info.baseDirectory = "/"; // doesn't matter, we set it up to get from
                                  // system CL
        info.classPathExtension = new String[0];
        info.className = evilAppClass.getName();
        info.transportProtocols = new AppEntry.TransportProtocol[] { new AppEntry.LocalTransportProtocol() };

        DummyDomain domain = new DummyDomain(info);
        return new XletApp(info, domain);
    }

    /**
     * Tests extension to notifyListeners(). Should notify the associated
     * <code>AppContext</code> that of the state change.
     */
    public void testNotifyListeners_AppContext() throws Exception
    {
        // We override XletApp to replace the createAppContext() method.
        // Our custom createAppContext() returns a custom AppContext.
        // Our custom AppContext (DummyContext2) overrides the notify*() methods
        // so that we can determine if they are called as expected.

        // TODO: this could be cleaned up...

        // Create replacement AppContext that allows us to see invocations
        // of notifyPaused(), notifyResumed(), notifyDestroyed().
        final org.cablelabs.impl.manager.application.CCMgr ccmgr = (org.cablelabs.impl.manager.application.CCMgr) org.cablelabs.impl.manager.application.CCMgr.getInstance();
        try
        {
            class DummyContext2 extends AppContext
            {
                public boolean paused;

                public boolean actived;

                public boolean destroyed;

                protected DummyContext2(XletApp app, AppEntry entry, AppDomainImpl domain)
                {
                    super(app, entry, domain, null /* owner */, ccmgr, null, null);
                }

                void clear()
                {
                    paused = false;
                    actived = false;
                    destroyed = false;
                }

                void notifyPaused()
                {
                    paused = true;
                    super.notifyPaused();
                }

                void notifyActive()
                {
                    actived = true;
                    super.notifyActive();
                }

                void notifyDestroyed()
                {
                    destroyed = true;
                    super.notifyDestroyed();
                }
            }
            // Replacement XletApp so that we can override createAppContext().
            AppEntry entry = new AppEntry();
            entry.id = getExpectedAppID();
            entry.version = 1;
            XletApp app = new XletApp(entry, (AppDomainImpl) getExpectedAppDomain())
            {
                AppContext createAppContext(AppID appId, AppDomainImpl appDomain, CallerContext req)
                {
                    return new DummyContext2(this, appDomain.getAppEntry(appId), appDomain);
                }
            };
            xletapp = app;

            // Get into STARTED state
            doStarted(app);
            AppContext appCtx = app.getAppContext();
            assertNotNull("Expected an AppContext to be created", appCtx);
            assertEquals("Expected DummyContext2 to be used", DummyContext2.class, appCtx.getClass());

            DummyContext2 ctx = (DummyContext2) appCtx;
            assertFalse("notifyPaused() shouldn't be called", ctx.paused);
            assertTrue("notifyActive() *should* be called", ctx.actived);
            assertFalse("notifyDestroyed() shouldn't be called", ctx.destroyed);

            // Test PAUSE
            ctx.clear();
            doPaused(app);
            assertTrue("notifyPaused() *should* be called", ctx.paused);
            assertFalse("notifyActive() shouldn't be called", ctx.actived);
            assertFalse("notifyDestroyed() shouldn't be called", ctx.destroyed);

            // Test RESUME
            ctx.clear();
            doResumed(app);
            assertFalse("notifyPaused() shouldn't be called", ctx.paused);
            assertTrue("notifyActive() *should* be called", ctx.actived);
            assertFalse("notifyDestroyed() shouldn't be called", ctx.destroyed);

            // Test DESTROY
            ctx.clear();
            doDestroyed(app);
            assertFalse("notifyPaused() shouldn't be called", ctx.paused);
            assertFalse("notifyActive() shouldn't be called", ctx.actived);
            assertTrue("notifyDestroyed() *should* be called", ctx.destroyed);
        }
        finally
        {
            ccmgr.destroy();
        }
    }

    /**
     * Tests getAppClassLoader().
     */
    public void testGetAppClassLoader() throws Exception
    {
        assertEquals("Expected app to be in NOT_LOADED state", AppProxy.NOT_LOADED, xletapp.getState());
        assertNull("No ClassLoader should be available in NOT_LOADED state", xletapp.getAppClassLoader());

        // Get into LOADED state
        Listener l = new Listener();
        xletapp.addAppStateChangeEventListener(l);
        synchronized (l)
        {
            l.clear();
            xletapp.load();
            l.waitNextEvent(WAIT_TIME);
        }
        assertEquals("Unexpected application state", DVBJProxy.LOADED, xletapp.getState());

        ClassLoader cl = xletapp.getAppClassLoader();
        assertNotNull("ClassLoader should be set in LOADED state", cl);
        assertSame("Same ClassLoader should be returned each call", cl, xletapp.getAppClassLoader());
        assertTrue("ClassLoader should be instance of AppClassLoader", cl instanceof AppClassLoader);
        cl = null;
    }

    /**
     * Tests getXletContext().
     */
    public void testGetXletContext() throws Exception
    {
        assertEquals("Expected app to be in NOT_LOADED state", AppProxy.NOT_LOADED, xletapp.getState());
        assertNull("No XletContext should be available in NOT_LOADED state", xletapp.getJavaTVXletContext());

        Thread.sleep(500);

        // Get into INITED state
        Listener l = new Listener();
        xletapp.addAppStateChangeEventListener(l);
        synchronized (l)
        {
            l.clear();
            xletapp.init();
            l.waitNextEvent(WAIT_TIME);
        }
        assertEquals("Unexpected application state", AppProxy.PAUSED, xletapp.getState());

        XletContext xc = xletapp.getJavaTVXletContext();
        assertNotNull("XletContext should be set in INITED(PAUSED) state", xc);
        assertTrue("XletContext should be instance of XletAppContext", xc instanceof XletAppContext);
        xc = null;
    }

    /**
     * Tests getXlet().
     */
    public void testGetXlet() throws Exception
    {
        assertEquals("Expected app to be in NOT_LOADED state", AppProxy.NOT_LOADED, xletapp.getState());
        assertNull("No Xlet should be availabe in NOT_LOADED state", xletapp.getXlet());

        // Get into INITED state
        Listener l = new Listener();
        xletapp.addAppStateChangeEventListener(l);
        synchronized (l)
        {
            l.clear();
            xletapp.init();
            l.waitNextEvent(WAIT_TIME);
        }
        assertEquals("Unexpected application state", AppProxy.PAUSED, xletapp.getState());

        Xlet xlet = (Xlet)xletapp.getXlet();
        assertNotNull("Xlet should be set in INITED(PAUSED) state", xlet);
        assertSame("Xlet class should be accessed through xlet class loader", xletapp.getAppClassLoader().loadClass(
                xlet.getClass().getName()), xlet.getClass());
        // Note that our instance of DummyXlet is loaded with JUnit's
        // TestCaseLoader
        // The xlet's should be found with the AppClassLoader
        assertEquals("Xlet should be instance of DummyXlet", DummyJTVXlet.class.getName(), xlet.getClass().getName());
        xlet = null;
    }

    /**
     * Tests getAppContext().
     */
    public void testGetAppContext() throws Exception
    {
        assertEquals("Expected app to be in NOT_LOADED state", AppProxy.NOT_LOADED, xletapp.getState());
        assertNull("No AppContext should be available in NOT_LOADED state", xletapp.getAppContext());

        // Get into LOADED state
        Listener l = new Listener();
        xletapp.addAppStateChangeEventListener(l);
        synchronized (l)
        {
            l.clear();
            xletapp.load();
            l.waitNextEvent(WAIT_TIME);
        }
        assertEquals("Unexpected application state", DVBJProxy.LOADED, xletapp.getState());

        AppContext ctx = xletapp.getAppContext();
        assertNotNull("AppContext should be created in LOADED state", ctx);

        // Get into DESTROYED state
        synchronized (l)
        {
            l.clear();
            xletapp.stop(true);
            l.waitNextEvent(WAIT_TIME);
        }
        // Note that DESTROYED state should be transient... so allow for
        // NOT_LOADED
        int state = xletapp.getState();
        assertTrue("Unexpected application state: " + state, state == AppProxy.DESTROYED
                || state == AppProxy.NOT_LOADED);
    }

    /**
     * Extends DVBJProxyTest to add tests for unload() similar to the other
     * DVBJProxy and AppProxy state-change methods.
     * 
     * @see XletAppTest#suite()
     */
    public static class ProxyTest extends DVBJProxyTest
    {
        /*
         * Tests unload(). <ul> <li> Operation is only available to
         * implementation. <li> A call to this method shall fail if in any state
         * other than the <code>DESTROYED</code> state. <li> This method is
         * synchronous (when it works) and its completion will be notified by an
         * AppStateChangeEvent. <li> In case of failure, the hasFailed method of
         * the AppStateChangedEvent will return true.
         */

        /**
         * Test unload() from NOT_LOADED.
         */
        public void testUnload_fromNOT_LOADED() throws Exception
        {
            doTest_fromNOT_LOADED(XTest3.UNLOAD, true, AppProxy.NOT_LOADED);
        }

        /**
         * Test unload() from INITED.
         */
        public void testUnload_fromINITED() throws Exception
        {
            doTest_fromINITED(XTest3.UNLOAD, true, AppProxy.NOT_LOADED);
        }

        /**
         * Test unload() from STARTED.
         */
        public void testUnload_fromSTARTED() throws Exception
        {
            doTest_fromSTARTED(XTest3.UNLOAD, true, AppProxy.NOT_LOADED);
        }

        /**
         * Test unload() from PAUSED.
         */
        public void testUnload_fromPAUSED() throws Exception
        {
            doTest_fromPAUSED(XTest3.UNLOAD, true, AppProxy.NOT_LOADED);
        }

        /**
         * Test unload() from RESUMED.
         */
        public void testUnload_fromRESUMED() throws Exception
        {
            doTest_fromRESUMED(XTest3.UNLOAD, true, AppProxy.NOT_LOADED);
        }

        /**
         * Test unload() from DESTROYED.
         * <P>
         * This test is disabled (by not following the naming conventions for
         * tests) because the <code>doDestroyed()</code> that is used in will
         * make any and all state transitions fail.
         */
        public void XtestUnload_fromDESTROYED() throws Exception
        {
            doTest_fromDESTROYED(XTest3.UNLOAD, false, AppProxy.NOT_LOADED);
        }

        /*
         * Tests autostart(). <ul> <li> Operation is only available to
         * implementation. <li> A call to this method shall fail if in any state
         * other than NOT_LOADED. <li> If the application was not loaded at the
         * moment of this call, then the application will be started <li> The
         * application will be initialized and then started by the Application
         * Manager, hence causing the Xlet to go from NotLoaded to Paused and
         * then from Paused to Active. <li> This method is asynchronous and its
         * completion will be notified by an AppStateChangedEvent. <li> In case
         * of failure, the hasFailed method of the AppStateChangedEvent will
         * return true. </ul>
         */

        /**
         * Test start() from NOT_LOADED.
         */
        public void testAutostart_fromNOT_LOADED() throws Exception
        {
            // Normally would expect this...
            // doTest_fromNOT_LOADED(XTest3.AUTOSTART, false,
            // AppProxy.NOT_LOADED, AppProxy.STARTED);
            // However, for autostart() we are generating two events ->LOADED
            // and ->STARTED

            doTest_fromNOT_LOADED(XTest3.AUTOSTART, false, AppProxy.NOT_LOADED, DVBJProxy.LOADED);
            expectEvent(false, DVBJProxy.LOADED, AppProxy.STARTED);
        }

        /**
         * Test start() from PAUSED.
         */
        public void testAutostart_fromPAUSED() throws Exception
        {
            doTest_fromPAUSED(XTest3.AUTOSTART, true, AppProxy.STARTED);
        }

        /**
         * Test start() from STARTED.
         */
        public void testAutostart_fromSTARTED() throws Exception
        {
            doTest_fromSTARTED(XTest3.AUTOSTART, true, AppProxy.STARTED);
        }

        /**
         * Test start() from RESUMED.
         */
        public void testAutostart_fromRESUMED() throws Exception
        {
            doTest_fromRESUMED(XTest3.AUTOSTART, true, AppProxy.STARTED);
        }

        /**
         * Test start() from DESTROYED.
         */
        public void testAutostart_fromDESTROYED() throws Exception
        {
            doTest_fromDESTROYED(XTest3.AUTOSTART, true, AppProxy.STARTED);
        }

        /**
         * Test start() from LOADED().
         */
        public void testAutostart_fromLOADED() throws Exception
        {
            doTest_fromLOADED(XTest3.AUTOSTART, true, AppProxy.STARTED);
        }

        /**
         * Test start() from INITED (PAUSED).
         */
        public void testAutostart_fromINITED() throws Exception
        {
            doTest_fromINITED(XTest3.AUTOSTART, true, AppProxy.STARTED);
        }

        /**
         * Test start() from LOADED-because-init-failed (LOADED).
         */
        public void testAutostart_fromLOADED_BAD() throws Exception
        {
            doTest_fromLOADED_BAD(XTest3.AUTOSTART, true, AppProxy.STARTED);
        }

        /**
         * Tests start() from NOT_LOADED and load operation fails.
         */
        public void testAutostart_fromNOT_LOADED_loadFails() throws Exception
        {
            dvbjFactory.makeLoadFail(dvbjapp);
            doTest_fromNOT_LOADED(XTest3.AUTOSTART, true, AppProxy.NOT_LOADED, DVBJProxy.LOADED);

            // Upon load failure, expect to be destroyed
            expectEvent(false, AppProxy.NOT_LOADED, AppProxy.DESTROYED);
        }

        /**
         * Tests start() from NOT_LOADED and initXlet fails.
         */
        public void testAutostart_fromNOT_LOADED_initFails() throws Exception
        {
            // Normally would expect this...
            // dvbjFactory.makeInitFail(dvbjapp);
            // doTest_fromNOT_LOADED(XTest3.AUTOSTART, true, DVBJProxy.LOADED,
            // AppProxy.PAUSED);
            // However, for autostart() we are generating two events ->LOADED
            // and ->STARTED

            dvbjFactory.makeInitFail(dvbjapp);
            doTest_fromNOT_LOADED(XTest3.AUTOSTART, false, AppProxy.NOT_LOADED, DVBJProxy.LOADED);
            expectEvent(true, DVBJProxy.LOADED, AppProxy.PAUSED);
        }

        /**
         * Tests start() from NOT_LOADED and startXlet fails.
         */
        public void testAutostart_fromNOT_LOADED_startFails() throws Exception
        {
            // Normally would expect this...
            // dvbjFactory.makeStartFail(dvbjapp);
            // doTest_fromNOT_LOADED(XTest3.AUTOSTART, true, AppProxy.PAUSED,
            // AppProxy.STARTED);
            // However, for autostart() we are generating two events ->LOADED
            // and ->STARTED

            dvbjFactory.makeStartFail(dvbjapp);
            doTest_fromNOT_LOADED(XTest3.AUTOSTART, false, AppProxy.NOT_LOADED, DVBJProxy.LOADED);
            expectEvent(true, AppProxy.PAUSED, AppProxy.STARTED);
        }

        /**
         * Tests start() from NOT_LOADED, initXlet() self-destructs.
         */
        public void testAutostart_fromNOT_LOADED_initDestroys() throws Exception
        {
            dvbjFactory.makeDestroyInInit(dvbjapp);
            doTest_fromNOT_LOADED(XTest3.AUTOSTART, false, AppProxy.NOT_LOADED, DVBJProxy.LOADED);
            expectEvent(false, DVBJProxy.LOADED, AppProxy.DESTROYED);
            expectEvent(true, AppProxy.DESTROYED, AppProxy.PAUSED);
        }

        /**
         * Tests start() from NOT_LOADED, startXlet() self-destructs.
         */
        public void testAutostart_fromNOT_LOADED_startDestroys() throws Exception
        {
            dvbjFactory.makeDestroyInStart(dvbjapp);
            doTest_fromNOT_LOADED(XTest3.AUTOSTART, false, AppProxy.NOT_LOADED, DVBJProxy.LOADED);
            expectEvent(false, AppProxy.PAUSED, AppProxy.DESTROYED);
            expectEvent(true, AppProxy.DESTROYED, AppProxy.STARTED);
        }

        protected static abstract class XTest3 extends XTest
        {
            public static final XTest UNLOAD = new XTest()
            {
                public void doX(AppProxy app)
                {
                    ((XletApp) app).unload();
                }
            };

            public static final XTest AUTOSTART = new XTest()
            {
                public void doX(AppProxy app)
                {
                    ((XletApp) app).autostart();
                }
            };
        }

        /* ================== Boilerplate =================== */
        public static InterfaceTestSuite isuite()
        {
            InterfaceTestSuite suite = new org.cablelabs.test.iftc.InterfaceTestSuite(ProxyTest.class);
            suite.setName("org.dvb.application.DVBJProxy[XletApp]");
            return suite;
        }

        public static InterfaceTestSuite isuite(String[] tests)
        {
            InterfaceTestSuite suite = new org.cablelabs.test.iftc.InterfaceTestSuite(ProxyTest.class, tests);
            suite.setName("org.dvb.application.DVBJProxy[XletApp]");
            return suite;
        }

        protected ProxyTest(String name, Class implClass, ImplFactory f)
        {
            super(name, implClass, f);
        }

        public ProxyTest(String name, ImplFactory f)
        {
            this(name, XletApp.class, f);
        }
    }

    /**
     * A simple AppDomain implementation used for testing. It can return the
     * given <code>AppEntry</code>. Mainly used for <code>getAppEntry()</code>,
     * which returns the <code>AppEntry</code> given at startup -- if the
     * <code>AppID</code> matches.
     */
    private static class DummyDomain extends AppDomainImpl
    {
        public AppEntry entry;

        public DummyDomain()
        {
            super(null, null);
        }

        public DummyDomain(AppEntry entry)
        {
            this();
            this.entry = entry;
        }

        AppEntry getAppEntry(AppID id)
        {
            return entry.id.equals(id) ? entry : null;
        }

        javax.tv.service.Service getCurrentService()
        {
            return new org.ocap.service.AbstractService()
            {
                private void fail()
                {
                    Assert.fail("should not be invoked");
                }

                public javax.tv.service.SIRequest retrieveDetails(javax.tv.service.SIRequestor requestor)
                {
                    fail();
                    return null;
                }

                public String getName()
                {
                    fail();
                    return null;
                }

                public boolean hasMultipleInstances()
                {
                    fail();
                    return false;
                }

                public javax.tv.service.ServiceType getServiceType()
                {
                    fail();
                    return null;
                }

                public javax.tv.locator.Locator getLocator()
                {
                    try
                    {
                        return new org.ocap.net.OcapLocator(0x12345);
                    }
                    catch (Exception e)
                    {
                        fail();
                        return null;
                    }
                }

                public java.util.Enumeration getAppIDs()
                {
                    fail();
                    return null;
                }

                public java.util.Enumeration getAppAttributes()
                {
                    fail();
                    return null;
                }
            };
        }
    }

    /**
     * A simple Xlet implementation used for testing.
     */
    public static class DummyJTVXlet implements Xlet
    {
        public int state = 0;

        public int started = 0;

        public XletContext xc;

        public synchronized void initXlet(XletContext ctx) throws XletStateChangeException
        {
            xc = ctx;
            state = 1;
            notifyAll();
        }

        public synchronized void startXlet() throws XletStateChangeException
        {
            state = started + 2; // 2 or 4
            started = 2;
            notifyAll();
        }

        public synchronized void pauseXlet()
        {
            state = 3;
            notifyAll();
        }

        public synchronized void destroyXlet(boolean unconditional) throws XletStateChangeException
        {
            if (!unconditional)
            {
                state = -state;
                notifyAll();
                throw new XletStateChangeException();
            }
            state = 1000;
            notifyAll();
        }
    }

    /**
     * A modified version of DummyXlet that always fails it's initXlet()
     * operation.
     */
    public static class InitFailXlet extends DummyJTVXlet
    {
        public synchronized void initXlet(XletContext ctx) throws XletStateChangeException
        {
            super.initXlet(ctx);

            throw new XletStateChangeException("initXlet() failed");
        }
    }

    public static class LoadFailXlet extends DummyJTVXlet
    {
        public LoadFailXlet()
        {
            throw new RuntimeException("load failed");
        }

    }

    public static class StartFailXlet extends DummyJTVXlet
    {
        public synchronized void startXlet() throws XletStateChangeException
        {
            throw new XletStateChangeException("startXlet() failed");
        }
    }

    public static class ResumeFailXlet extends DummyJTVXlet
    {
        private boolean once;

        public synchronized void startXlet() throws XletStateChangeException
        {
            if (once) throw new XletStateChangeException("resume failed");
            once = true;
            super.startXlet();
        }
    }

    public static class PauseFailXlet extends DummyJTVXlet
    {
        public synchronized void pauseXlet()
        {
            throw new RuntimeException();
        }
    }

    public static class Pause2FailXlet extends DummyJTVXlet
    {
        private boolean once;

        public synchronized void pauseXlet()
        {
            if (once) throw new RuntimeException();
            once = true;
            super.pauseXlet();
        }
    }

    public static class DestroyFailXlet extends DummyJTVXlet
    {
        private boolean flag = true;

        public synchronized void destroyXlet(boolean ignored) throws XletStateChangeException
        {
            boolean first = flag;
            flag = false;
            if (first) throw new XletStateChangeException("");
        }
    }

    public static class DestroySelfDestroy extends DummyJTVXlet
    {
        public synchronized void destroyXlet(boolean ignored) throws XletStateChangeException
        {
            xc.notifyDestroyed();
        }
    }

    public static class StartSelfDestroy extends DummyJTVXlet
    {
        public synchronized void startXlet()
        {
            xc.notifyDestroyed();
        }
    }

    public static class ResumeSelfDestroy extends DummyJTVXlet
    {
        private boolean once;

        public synchronized void startXlet() throws XletStateChangeException
        {
            if (once) xc.notifyDestroyed();
            once = true;
        }
    }

    public static class PauseSelfDestroy extends DummyJTVXlet
    {
        public synchronized void pauseXlet()
        {
            xc.notifyDestroyed();
        }
    }

    public static class Pause2SelfDestroy extends DummyJTVXlet
    {
        private boolean once;

        public synchronized void pauseXlet()
        {
            if (once) xc.notifyDestroyed();
            once = true;
        }
    }

    public static class InitSelfDestroy extends DummyJTVXlet
    {
        public synchronized void initXlet(XletContext xc)
        {
            xc.notifyDestroyed();
        }
    }

    /**
     * A simple implementation of a {@link FileSys} that allows access to files
     * on the system classpath.
     * 
     * @author Aaron Kamienski
     */
    public static class ClasspathFS implements FileSys
    {
        private static int count = 0;

        public static synchronized ClasspathFS mount()
        {
            String path = "/classpath" + (count++) + "/";
            ClasspathFS newFS = new ClasspathFS(path);

            FileManager fm = (FileManager) ManagerManager.getInstance(FileManager.class);
            fm.registerFileSys(path, newFS);

            return newFS;
        }

        public void unmount()
        {
            FileManager fm = (FileManager) ManagerManager.getInstance(FileManager.class);
            fm.unregisterFileSys(mount);
        }

        public final String mount;

        private ClasspathFS(String mount)
        {
            this.mount = mount;
        }

        public AsyncLoadHandle asynchronousLoad(String path, int loadMode, AsyncLoadCallback cb)
                throws FileNotFoundException
        {
            throw new UnsupportedOperationException();
        }

        public boolean canRead(String path)
        {
            return true;
        }

        public boolean canWrite(String path)
        {
            return false;
        }

        public boolean create(String path)
        {
            return false;
        }

        public boolean delete(String path)
        {
            return false;
        }

        public boolean deleteOnExit(String path)
        {
            return false;
        }

        public boolean exists(String path)
        {
            return getResource(path) != null;
        }

        public String getCanonicalPath(String path)
        {
            return path;
        }

        public FileData getFileData(String path) throws FileNotFoundException, IOException
        {
            InputStream is = getResourceAsStream(path);
            if (is == null) throw new FileNotFoundException(path);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] array = new byte[256];
            int read;
            while ((read = is.read(array)) >= 0)
                bos.write(array, 0, read);
            FileDataImpl fd = new FileDataImpl(bos.toByteArray());
            return fd;
        }

        public X509Certificate[][] getSigners(String path, boolean checkRoot) throws Exception
        {
            throw new UnsupportedOperationException();
        }

        public X509Certificate[][] getSigners(String path)
        {
            throw new UnsupportedOperationException();
        }

        public boolean isDir(String path)
        {
            // isDir not implemented - implement if necessary
            return false;
        }

        public boolean isFile(String path)
        {
            // isFile not implemented - implement if necessary
            return true;
        }

        public long lastModified(String path)
        {
            return 0L;
        }

        public long length(String path)
        {
            try
            {
                return getResource(path).openConnection().getContentLength();
            }
            catch (Exception e)
            {
                return -1L;
            }
        }

        public String[] list(String path)
        {
            return null;
        }

        public FileSys load(String path, int loadMode) throws FileNotFoundException, IOException
        {
            return this;
        }

        public boolean mkdir(String path)
        {
            return false;
        }

        public OpenFile open(String path) throws FileNotFoundException
        {
            return new DummyOpenFile(getResource(path));
        }

        public boolean renameTo(String fromPath, String toPath)
        {
            return false;
        }

        public boolean setLastModified(String path, long time)
        {
            return false;
        }

        public boolean setReadOnly(String path)
        {
            return false;
        }

        public FileSys unload()
        {
            return this;
        }

        private URL getResource(String path)
        {
            String unpath = unpath(path);
            return getClass().getResource(unpath);
        }

        private InputStream getResourceAsStream(String path)
        {
            String unpath = unpath(path);
            return getClass().getResourceAsStream(unpath);
        }

        private String unpath(String path)
        {
            if (path.startsWith(mount)) return path.substring(mount.length() - 1);
            return null;
        }

        class DummyOpenFile implements OpenFile
        {
            private BufferedInputStream is;

            private int length;

            private long fp;

            DummyOpenFile(URL url) throws FileNotFoundException
            {
                try
                {
                    URLConnection conn = url.openConnection();
                    length = conn.getContentLength();
                    is = new BufferedInputStream(conn.getInputStream());
                    is.mark(length);
                }
                catch (FileNotFoundException e)
                {
                    throw e;
                }
                catch (IOException e)
                {
                    throw new FileNotFoundException(e.getMessage());
                }
            }

            public int available() throws IOException
            {
                return is.available();
            }

            public void close() throws IOException
            {
                is.close();
            }

            public long getFilePointer() throws IOException
            {
                return fp;
            }

            public long length() throws IOException
            {
                return length;
            }

            public int read() throws IOException
            {
                int rc = is.read();
                if (fp != -1) ++fp;
                return rc;
            }

            public int read(byte[] array, int offset, int len) throws IOException
            {
                int rc = is.read(array, offset, len);
                if (fp != -1) fp += rc;
                return rc;
            }

            public void seek(long pos) throws IOException
            {
                is.reset();
                fp = is.skip(pos);
            }

            public long skip(long n) throws IOException
            {
                long skipped = is.skip(n);
                fp += skipped;
                return fp;
            }

            public int getNativeFileHandle()
            {
                return 0;
            }

        }

        public String contentType(String path)
        {
            return null;
        }
    }

    /** Simple latch variable. */
    public static class Latch
    {
        protected boolean latch = false;

        public synchronized void waitLatch() throws InterruptedException
        {
            while (!latch)
                wait();
        }

        public synchronized boolean waitLatch(long ms) throws InterruptedException
        {
            wait(ms);
            return latch;
        }

        public synchronized void signalLatch()
        {
            latch = true;
            notifyAll();
        }
    }

    /**
     * An evil Xlet that trys to do some things that it shouldn't AFTER it's
     * been destroyed.
     */
    public abstract static class EvilXlet extends DummyJTVXlet implements Runnable
    {
        public synchronized void destroyXlet(boolean unconditional) throws XletStateChangeException
        {
            super.destroyXlet(unconditional);

            (new Thread(this)).start();
        }

        public void run()
        {
            try
            {
                signal.waitLatch(WAIT_TIME);
                doit();
                okay = true;
            }
            catch (Throwable ex)
            {
                e = ex;
                ex.printStackTrace();
            }
            finally
            {
                done.signalLatch();
            }
        }

        // wait for condition to be true before doit()
        public Latch signal = new Latch();

        // signal that we are done
        public Latch done = new Latch();

        public Throwable e;

        public boolean okay;

        public abstract void doit() throws Exception;
    }

    public static class EvilXletThread extends EvilXlet
    {
        public void doit()
        {
            for (int i = 0; i < 10; ++i)
                (new Thread()).start();
        }
    }

    public static class EvilXletIO extends EvilXlet
    {
        public void doit() throws java.io.IOException
        {
            java.io.InputStream is = getClass().getResourceAsStream(
                    "/org/cablelabs/impl/manager/application/XletAppTest.class");
            try
            {
                while (is.read() != -1)
                {
                    /* EMPTY */
                }
            }
            finally
            {
                if (is != null) is.close();
            }
        }
    }

    public static class EvilXletAPI extends EvilXlet
    {
        public void doit()
        {
            AppsDatabase db = AppsDatabase.getAppsDatabase();
            java.util.Enumeration appids = db.getAppIDs(new AppsDatabaseFilter()
            {
                public boolean accept(AppID id)
                {
                    return true;
                }
            });
            while (appids.hasMoreElements())
            {
                AppID id = (AppID) appids.nextElement();
                db.getAppAttributes(id);
                db.getAppProxy(id);
            }
        }
    }

    public static class DummySecurity extends SecurityManager
    {
        private boolean fail = false;

        public void setFailAllChecks()
        {
            fail = true;
        }

        public void checkPermission(Permission perm, Object context)
        {
            super.checkPermission(perm, context);
            if (fail) throw new SecurityException("DummySecurity failure");
        }

        public void checkPermission(Permission perm)
        {
            super.checkPermission(perm);
            if (fail) throw new SecurityException("DummySecurity failure");
        }
    }

    // Boilerplate

    static
    {
        // HACK ALERT!!!!
        // This is done to ensure that the default policy is initialized BEFORE
        // we install our dummy file system... which will be the CWD for a
        // loaded
        // AppContext... and an AppClassLoader is created that tests for <file/>
        // permission... which causes the default policy to be loaded in the
        // complete wrong context (where CWD is relative to our dummy FS)!!!
        if (true)
            try
            {
                Policy.getPolicy().getPermissions((java.security.CodeSource) null).implies(
                        new FilePermission(".", "read"));
            }
            catch (Exception e)
            { /* don't care */
            }
    }

    protected XletApp xletapp;

    protected AppDomain appdomain;

    protected static ClasspathFS fs = ClasspathFS.mount(); // NOTE: this is
                                                           // NEVER unmounted!

    protected static XletApp createImplObject(AppID id, DummyDomain domain)
    {
        // Allow any class to be loaded for Xlet
        AppClassLoader.bypassPublicAPICheck(true);

        AppEntry info = new AppEntry();
        info.id = id;
        info.version = 1;
        info.controlCode = OcapAppAttributes.PRESENT;
        info.parameters = new String[0];
        info.baseDirectory = fs.mount;
        info.className = DummyJTVXlet.class.getName();
        info.classPathExtension = new String[0];
        info.transportProtocols = new AppEntry.TransportProtocol[] { new AppEntry.LocalTransportProtocol() };

        domain.entry = info;
        return new XletApp(info, domain);
    }

    /** Used for DVBJProxy ImplFactory. */
    protected static XletApp createImplObject()
    {
        return createImplObject(new AppID(TestAppIDs.XLETAPP, 2), new DummyDomain());
    }

    protected AppID getExpectedAppID()
    {
        return new AppID(TestAppIDs.XLETAPP, 3);
    }

    protected AppDomain getExpectedAppDomain()
    {
        if (appdomain == null)
        {
            appdomain = new DummyDomain();
        }
        return appdomain;
    }

    protected Application createApplication()
    {

        return createImplObject(getExpectedAppID(), (DummyDomain) getExpectedAppDomain());
    }

    protected XletApp createXletApp()
    {
        return (XletApp) createApplication();
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        xletapp = createXletApp();
        for (int i = 0; i < 3; i++)
        {
            System.gc();
        }
    }

    /**
     * Basically the same as {@link #doDestroyed} except waits for movement back
     * to <code>NOT_LOADED</code> state.
     * <ol>
     * <li>Invoke <code>app.stop(true);</code>
     * <li>Wait for <code>DESTROYED</code> state event.
     * <li>If already in <code>DESTROYED</code> state, will get event with both
     * to and from state indicated <code>DESTROYED</code> and failed=true
     * <li>Finally wait for <code>NOT_LOADED</code> state
     * </ol>
     */
    public static void doUnloaded(XletApp app) throws Exception
    {
        Listener l = new Listener();
        app.addAppStateChangeEventListener(l);

        synchronized (l)
        {
            app.stop(true);

            // Wait for DESTROYED state
            // Or DESTROYED->DESTROYED (failed)
            AppStateChangeEvent e = null;
            do
            {
                e = l.waitNextEvent(WAIT_TIME); // will fail() if no event is
                                                // received in time...
            }
            while (e.getToState() != AppProxy.DESTROYED || (e.hasFailed() && e.getFromState() != AppProxy.DESTROYED));
            assertEquals("Expected DESTROYED state to be entered", AppProxy.DESTROYED, e.getToState());

            // Wait for NOT_LOADED
            l.waitUntilEvent(AppProxy.NOT_LOADED, WAIT_TIME);
        }
        app.removeAppStateChangeEventListener(l);

        AppProxyTest.checkState(app, AppProxy.NOT_LOADED);
    }

    protected void tearDown() throws Exception
    {
        if (xletapp != null)
        {
            doUnloaded(xletapp);
            xletapp = null;
        }

        // Restore public API check
        AppClassLoader.bypassPublicAPICheck(false);

        super.tearDown();
    }

    public XletAppTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private static class XletAppFactory implements DVBJProxyTest.DVBJProxyImplFactory
    {
        public Object createImplObject()
        {
            return XletAppTest.createImplObject();
        }

        public void makeUnloaded(AppProxy app) throws Exception
        {
            ((XletApp) app).setUnloadEnabled(true); // just in case
            doUnloaded((XletApp) app);

            // Restore public API check
            AppClassLoader.bypassPublicAPICheck(false);
        }

        public void makeDestroyedPermanent(AppProxy app) throws Exception
        {
            ((XletApp) app).setUnloadEnabled(false); // don't go to NOT_LOADED
            app.stop(true);
            // doDestroyed((XletApp)app);
            // ((XletApp)app).dispose();
        }

        private void changeXletClass(DVBJProxy app, Class cls)
        {
            ((XletApp)app).entry.className = cls.getName();

            if (app.getState() != AppProxy.NOT_LOADED)
                fail("Internal Test Error - cannot cause initXlet() to fail at this stage " + app.getState());
        }

        public void makeLoadFail(DVBJProxy app) throws Exception
        {
            changeXletClass(app, LoadFailXlet.class);
        }

        public void makeInitFail(DVBJProxy app) throws Exception
        {
            changeXletClass(app, InitFailXlet.class);
        }

        public void makeStartFail(DVBJProxy app) throws Exception
        {
            changeXletClass(app, StartFailXlet.class);
        }

        public void makeResumeFail(DVBJProxy app) throws Exception
        {
            changeXletClass(app, ResumeFailXlet.class);
        }

        public void makePauseFail(DVBJProxy app) throws Exception
        {
            changeXletClass(app, PauseFailXlet.class);
        }

        public void makePause2Fail(DVBJProxy app) throws Exception
        {
            changeXletClass(app, Pause2FailXlet.class);
        }

        public void makeDestroyFail(DVBJProxy app) throws Exception
        {
            changeXletClass(app, DestroyFailXlet.class);
        }

        public void makeDestroyInDestroy(DVBJProxy app) throws Exception
        {
            changeXletClass(app, DestroySelfDestroy.class);
        }

        public void makeDestroyInInit(DVBJProxy app) throws Exception
        {
            changeXletClass(app, InitSelfDestroy.class);
        }

        public void makeDestroyInPause(DVBJProxy app) throws Exception
        {
            changeXletClass(app, PauseSelfDestroy.class);
        }

        public void makeDestroyInPause2(DVBJProxy app) throws Exception
        {
            changeXletClass(app, Pause2SelfDestroy.class);
        }

        public void makeDestroyInResume(DVBJProxy app) throws Exception
        {
            changeXletClass(app, ResumeSelfDestroy.class);
        }

        public void makeDestroyInStart(DVBJProxy app) throws Exception
        {
            changeXletClass(app, StartSelfDestroy.class);
        }

        public String toString()
        {
            return "XletAppFactory";
        }
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(XletAppTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new XletAppTest(tests[i]));

            ImplFactory factory = new XletAppFactory();
            // InterfaceTestSuite asm = DVBJProxyTest.isuite(tests); // filters
            // tests
            InterfaceTestSuite dvbjSuite = ProxyTest.isuite(tests); // filters
                                                                    // tests
            dvbjSuite.addFactory(factory);
            suite.addTest(dvbjSuite);

            return suite;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(XletAppTest.class);

        ImplFactory factory = new XletAppFactory();
        // InterfaceTestSuite dvbjSuite = DVBJProxyTest.isuite();
        InterfaceTestSuite dvbjSuite = ProxyTest.isuite();

        dvbjSuite.addFactory(factory);

        suite.addTest(dvbjSuite);

        return suite;
    }
}
