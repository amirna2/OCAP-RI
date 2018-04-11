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

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.CallerContextTest;
import org.cablelabs.impl.manager.FileManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.AuthManager.AuthContext;
import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.manager.CallerContextTest.ContextImplFactory;
import org.cablelabs.impl.manager.application.AppDomainImplTest.TestServiceContext;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.test.TestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.tv.locator.Locator;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;
import javax.tv.service.ServiceInformationType;
import javax.tv.service.ServiceType;
import javax.tv.service.guide.ProgramSchedule;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceComponentChangeListener;
import javax.tv.service.selection.ServiceContext;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;

/**
 * Tests AppContext2.
 * 
 * @author Aaron Kamienski
 */
public class AppContextTest extends AbstractCallerContextTest
{
    /**
     * Tests no public constructors.
     */
    public void testNoPublicConstructor()
    {
        TestUtils.testNoPublicConstructors(AppContext.class);
    }

    /**
     * Tests load().
     * <ul>
     * <li>file system access (base_directory resolution)
     * <li>create class loader
     * <li>load the class
     * </ul>
     */
    public void testLoad() throws Exception
    {
        domain.clear();
        assertFalse("load() failed", appContext.load());
        // Expected AppDomain.getCurrentService() to be called.
        assertTrue("Expected AppDomain.getCurrentService() to be called", domain.getServiceCalled);

        // Expected AuthManager/AuthContext to be called
        assertEquals("Expected AuthManager.createAuthCtx() to be called exactly once", 1, am.createAuthCtxCalled);
        assertEquals("Expected AuthManager.setAuthCtx() to be called exactly once", 1, am.setAuthCtxCalled);
        AuthContext ac = am.getAuthCtx(appContext);
        assertNotNull("Expected non-null AuthContext to be set", ac);
        assertSame("Expected set context to be created context", am.lastCreatedCtx, ac);
        assertTrue("Expected AuthContext.getAppSignedStatus() to be called",
                1 <= ((DummyAuthCtx) ac).getAppSignedStatusCalled);

        // TODO: Unfinished testLoad

        // Test with LocalTransportProtocol...
        // Should load main class
        // Should return true on failure; false on success

        // Test setupFileSystem
        // Test locateInStorage
        // Test storeApp

        // Test with OcTransportProtocol... (expect to fail... expect not tuned)
        // Test HTTP transport

        // It's kinda hard to test this here...
        // I may need to modify AppContext for better testability
    }

    /**
     * Tests load() failure due to authentication.
     */
    private void doTestLoad_AuthType(int type) throws Exception
    {
        domain.clear();
        am.initType = type;

        assertTrue("Expected load() to fail", appContext.load());
        // Expected AppDomain.getCurrentService() to be called.
        assertTrue("Expected AppDomain.getCurrentService() to be called", domain.getServiceCalled);

        // Expected AuthManager/AuthContext to be called
        assertEquals("Expected AuthManager.createAuthCtx() to be called exactly once", 1, am.createAuthCtxCalled);
        assertEquals("Expected AuthContext.getAppSignedStatus() to be called once", 1,
                am.lastCreatedCtx.getAppSignedStatusCalled);
        assertEquals("Expected AuthManager.setAuthCtx() to not be called", 0, am.setAuthCtxCalled);
    }

    /**
     * Tests load() failure due to authentication.
     */
    public void testLoad_AUTH_FAIL() throws Exception
    {
        doTestLoad_AuthType(AuthInfo.AUTH_FAIL);
    }

    /**
     * Tests load() failure due to authentication.
     */
    public void testLoad_AUTH_UNKNOWN() throws Exception
    {
        doTestLoad_AuthType(AuthInfo.AUTH_UNKNOWN);
    }

    /**
     * Tests create().
     */
    public void testCreate() throws Exception
    {
        assertFalse("load() failed", appContext.load());

        Xlet xlet = appContext.create();

        assertNotNull("Expected create() to return instance of main class", xlet);
    }

    /**
     * Tests getClassLoader(). Should return null until load is called.
     */
    public void testGetClassLoader() throws Exception
    {
        assertSame("should return null unless \"loaded\"", null, appContext.getClassLoader());

        assertFalse("Load operation failed", appContext.load());

        ClassLoader cl = appContext.getClassLoader();
        assertNotNull("after load(), should have a ClassLoader", cl);

        assertTrue("ClassLoader not instanceof AppClassLoader", cl instanceof AppClassLoader);
    }

    /**
     * Tests getThreadGroup(). Should return AppThreadGroup immediately.
     */
    public void testGetThreadGroup() throws Exception
    {
        ThreadGroup tg = appContext.getThreadGroup();

        assertNotNull("ThreadGroup should not be null", tg);
        assertTrue("ThreadGroup should be instanceof AppThreadGroup", tg instanceof AppThreadGroup);
        assertSame("Repeated calls should return same", tg, appContext.getThreadGroup());
    }

    /**
     * Tests get(APP_PRIORITY).
     */
    public void testGet_AppPriority() throws Exception
    {
        int initial = domain.getAppEntry(id).priority;

        // For this test, don't expect 0...
        assertTrue("Internal Error - bad original priority", initial > 5 && initial <= 250);

        Integer expected = new Integer(initial);
        assertEquals("Unexpected initial priority", expected, appContext.get(CallerContext.APP_PRIORITY));

        // Set runtime priority
        Integer runtime = new Integer(initial + 1);
        appContext.setPriority(runtime.intValue());
        assertEquals("Unexpected runtime priority", runtime, appContext.get(CallerContext.APP_PRIORITY));

        // Set override priority
        Integer override = new Integer(initial - 1);
        AppManager.getAppManager().setApplicationPriority(id, 0, override.intValue());
        assertEquals("Unexpected override priority", override, appContext.get(CallerContext.APP_PRIORITY));

        // Set override priority (ignored)
        Integer override2 = new Integer(initial - 2);
        AppManager.getAppManager().setApplicationPriority(id, 1, override2.intValue());
        assertEquals("Expected override to be ignored given version change", runtime,
                appContext.get(CallerContext.APP_PRIORITY));
    }

    /**
     * Tests get(APP_ID).
     */
    public void testGet_AppID() throws Exception
    {
        Object appid = appContext.get(CallerContext.APP_ID);

        assertNotNull("Expected non-null AppID", appid);
        assertEquals("Unexpected AppID", id, appid);
    }

    /**
     * Tests get(SERVICE_CONTEXT).
     */
    public void testGet_ServiceContext() throws Exception
    {
        ServiceContext expected = domain.getServiceContext();

        assertNotNull("Internal Error - ServiceContext should not be null", expected);
        assertSame("Unexpected ServiceContext returned", expected, appContext.get(CallerContext.SERVICE_CONTEXT));
    }

    /**
     * Tests get(THREAD_GROUP).
     */
    public void testGet_ThreadGroup() throws Exception
    {
        ThreadGroup tg = appContext.getThreadGroup();

        assertNotNull("Unexpected thread group returned by getThreadGroup", tg);
        assertSame("Expected same returned by get(THREAD_GROUP)", tg, appContext.get(CallerContext.THREAD_GROUP));
    }

    /**
     * Tests avoidance of callback data deadlock when invoking callbacks.
     */
    public void testCallbackData_deadlock() throws Exception
    {
        // add callback data, callback will grab a lock and pause
        // on a separate thread, grab same lock and attempt to add/remove
        // callback data

        final long start = System.currentTimeMillis();
        System.out.println((System.currentTimeMillis() - start) + " [" + Thread.currentThread() + "] "
                + "testCallbackData_deadlock()");
        final Object lock = new Object();
        Thread thread = new Thread("Blocked?")
        {
            private void log(String msg)
            {
                System.out.println((System.currentTimeMillis() - start) + " [" + Thread.currentThread() + "] " + msg);
            }

            public void run()
            {
                log("thread: synchronizing on lock");
                // synchronized(lock)
                {
                    CallbackData data = new CallbackData()
                    {
                        public void destroy(CallerContext cc)
                        { /* empty */
                        }

                        public void pause(CallerContext cc)
                        { /* empty */
                        }

                        public void active(CallerContext cc)
                        { /* empty */
                        }
                    };
                    log("thread: adding callback data");
                    appContext.addCallbackData(data, data);
                    log("thread: done");
                }
            }
        };
        CallbackData cb = new CallbackData()
        {
            public void destroy(CallerContext cc)
            {
                // empty
            }

            public void pause(CallerContext cc)
            {
                // empty
            }

            public void active(CallerContext cc)
            {
                block();
            }

            private void log(String msg)
            {
                System.out.println((System.currentTimeMillis() - start) + " [" + Thread.currentThread() + "] " + msg);
            }

            private void block()
            {
                log("block: synchronizing on lock");
                // synchronized(lock)
                {
                    try
                    {
                        log("block: holding for 10 sec");
                        Thread.sleep(10000);
                    }
                    catch (Exception e)
                    {
                        // empty
                    }
                    log("block: done");
                }
            }
        };
        appContext.addCallbackData(cb, cb);
        try
        {
            new Thread("AppContext")
            {
                public void run()
                {
                    try
                    {
                        doPaused(appContext);

                        // Will block
                        doActive(appContext);
                    }
                    catch (Exception e)
                    {
                        // TODO: propogate this error
                        e.printStackTrace();
                    }
                }
            }.start();
            Thread.sleep(500);
            thread.start();
            thread.join(5000);
            try
            {
                // Expect thread to not have been blocked
                assertFalse("Expected thread to not have been blocked", thread.isAlive());
            }
            finally
            {
                thread.join(10000); // Wait for thread to end eventually
            }
        }
        finally
        {
            appContext.removeCallbackData(cb);
        }
    }

    /**
     * Tests get(USER_DIR).
     */
    public void XtestGet_UserDir() throws Exception
    {
        fail("Unimplemented test");
    }

    /**
     * Tests the static createInstance().
     */
    public void testCreateInstance() throws Exception
    {
        AppContext appContext1 = this.appContext;
        assertNotNull("createInstance() should return an instance", appContext1);

        assertSame("createInstance() should return null for same id", null, ccMgr.createInstance(app, id, domain)); // need
                                                                                                                    // owner,
                                                                                                                    // ccMgr;
                                                                                                                    // could
                                                                                                                    // just
                                                                                                                    // go
                                                                                                                    // through
                                                                                                                    // ccMgr

        // Following destruction, should be able to create
        doDestroyed(appContext1);
        this.appContext = null; // don't try to destroy again
        domain.clear();
        AppContext appContext2 = ccMgr.createInstance(app, id, domain);
        this.appContext = appContext2; // now destroy this one on teardown

        // Expect to call AppDomain.getAppEntry()
        assertNotNull("expected AppDomain.getAppEntry() to be called", domain.getEntryCalled);
        assertEquals("expected AppDomain.getAppEntry() called with id", id, domain.getEntryCalled);

        assertNotNull("createInstance() should succeed after destruction", appContext2);
        assertNotSame("Different AppContext should be returned", appContext1, appContext2);

        // Should fail with "unknown" AppID
        domain.clear();
        assertSame("createInstance() should fail with unknown AppID", null, ccMgr.createInstance(app,
                DummyDomain.UNKNOWN, domain));
    }

    /**
     * Tests the CCMgr.isActive() method. TODO: should be moved to CCMgrTest.
     */
    public void testIsActive() throws Exception
    {
        // isActive() should be false for UNKNOWN appid
        assertFalse("isActive() should be false for unknown id", ccMgr.isActive(DummyDomain.UNKNOWN));

        // isActive() should be true from the get-go...
        assertTrue("isActive() should be initially true", ccMgr.isActive(id));

        assertNotNull("appContext shouldn't be null here!", appContext);

        // should be true if "paused"
        doPaused(appContext);
        assertTrue("isActive() should be true if paused", ccMgr.isActive(id));

        // should be true if "resumed"
        doActive(appContext);
        assertTrue("isActive() should be true if resumed", ccMgr.isActive(id));

        // should be false following "destroyed"
        doDestroyed(appContext);
        appContext = null;
        assertFalse("isActive() should be false if destroyed", ccMgr.isActive(id));
    }

    /**
     * Used to extend framework for testing shutdown process.
     * 
     * @see AppContextTest#doTestShutdownProcess(DoStuff)
     */
    private class DoStuff
    {
        void doit(AppContext ctx, ThreadGroup tg, Thread t)
        {
            // should be overridden
        }
    }

    /**
     * Framework for testing shutdown process.
     */
    private void doTestShutdownProcess(DoStuff stuff) throws Exception
    {
        AppContext ac = this.appContext;
        ThreadGroup tg = ac.getThreadGroup();

        // Get Access to EventQueue thread
        class GetThread implements Runnable
        {
            public Thread thread;

            public synchronized void run()
            {
                thread = Thread.currentThread();
            }
        }
        GetThread run = new GetThread();
        synchronized (run)
        {
            ac.runInContext(run);
            run.wait(10000);
        }

        assertNotSame("Expected separate eventQueue thread", Thread.currentThread(), run.thread);
        assertNotNull("Problem getting eventQueue thread", run.thread);
        assertTrue("Expected eventQueue thread to be alive", run.thread.isAlive());

        stuff.doit(ac, tg, run.thread);

        // Call notifyDestroyed() to kick it off
        // Note that notifyDestroyed() is usually called via a runInContext...
        // Should probably test it there as well...
        ac.notifyDestroyed();
        this.appContext = null;

        // ClassLoader is now null...
        assertSame("ClassLoader should be null", null, ac.getClassLoader());

        // ThreadGroup is now null...
        assertSame("ThreadGroup should now be null", null, ac.getThreadGroup());

        // Run In Context should fail miserably now
        Runnable nothing = new Runnable()
        {
            public void run()
            { /* empty */
            }
        };
        try
        {
            ac.runInContext(nothing);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException e)
        { /* expected */
        }

        // Run In Context should fail miserably now
        try
        {
            ac.runInContextSync(nothing);
            fail("Expected IllegalStateException");
        }
        catch (IllegalStateException e)
        { /* expected */
        }

        // AppEventQueue should be disposed...
        // ... unless it's a pool thread
        if (run.thread.getName().indexOf("pool") == -1)
        {
            if (run.thread.isAlive())
            {
                run.thread.join(10000);
            }
            assertFalse("Expected eventQueue thread to die", run.thread.isAlive());
        }

        // Wait for ThreadGroup to be destroyed...
        for (int countdown = 10; !tg.isDestroyed() && countdown != 0; --countdown)
        {
            Thread.sleep(1000);
        }

        // thread group(s) should be destroyed
        assertTrue("Expected main ThreadGroup to be destroyed", tg.isDestroyed());

        // The XletApp.unload() should've been called
        synchronized (app)
        {
            if (!app.unloaded) app.wait(5000);
        }
        assertTrue("Expected XletApp.unload() to have been invoked", app.unloaded);
        assertEquals("Expected XletApp.unload() to have been called only once", 1, app.unloadedCount);

        // testCleanupAfterTesting();
    }

    /**
     * Tests <i>shutdown</i> process (implied by notifyDestroyed).
     */
    public void testShutdownProcess() throws Exception
    {
        // Don't do anything extra before shutdown
        doTestShutdownProcess(new DoStuff());
    }

    /**
     * Tests <i>shutdown()</i> process with sleeping thread.
     */
    public void testShutdownProcess_sleep() throws Exception
    {
        doTestShutdownProcess(new DoStuff()
        {
            public void doit(AppContext ctx, ThreadGroup tg, Thread t)
            {
                createThread(tg, "sleep");
            }
        });
    }

    /**
     * Tests <i>shutdown()</i> process with multiple ThreadGroups and
     * sub-threads.
     */
    public void testShutdownProcess_multiple() throws Exception
    {
        doTestShutdownProcess(new DoStuff()
        {
            public void doit(AppContext ctx, ThreadGroup tg, Thread t)
            {
                createThreadGroup(tg, "multiple", 2, 2);
            }
        });
    }

    /**
     * Tests <i>shutdown()</i> process with unresponsive xlet. (E.g., the
     * destroyXlet() never returns).
     */
    public void XtestShutdownProcess_unresponsive() throws Exception
    {
        fail("unimplemented test");
    }

    /**
     * Tests <i>shutdown()</i> process with while(true) thread.
     * <p>
     * We cannot currently pass this test. I'm not sure we'll ever be able to,
     * but we'll at least be able to limit the impact that it has.
     */
    public void XtestShutdownProcess_forever() throws Exception
    {
        doTestShutdownProcess(new DoStuff()
        {
            public void doit(AppContext ctx, ThreadGroup tg, Thread t)
            {
                Thread newThread = new Thread(tg, "forever")
                {
                    public void run()
                    {
                        while (true)
                        {
                            /* loop forever doing nothing */
                        }
                    }
                };
                newThread.start();
            }
        });
    }

    /**
     * Finally, test that we've cleaned up properly after all this testing. Make
     * sure that there aren't any AppThreadGroups lying around.
     * <p>
     * Note: it is expected that this will fail if any of the <i>shutdown
     * process</i> tests fail.
     */
    public void testCleanupAfterTesting() throws Exception
    {
        // Perform tearDown early
        // (We don't want the stuff created by setUp)
        tearDownImpl();

        ThreadGroup tg = Thread.currentThread().getThreadGroup();
        assertFalse("Did not expect to be running within an app", tg instanceof AppThreadGroup);

        int count = countActiveAppThreadGroups(tg);
        if (count != 0)
        {
            System.out.println("AppThreadGroups still running:");
            tg.list();
        }

        assertEquals("Expected no active AppThreadGroups; cleanup failed", 0, count);
    }

    /**
     * Recursively search starting with the given ThreadGroup and look for
     * <code>AppThreadGroup</code>s, return the number found.
     */
    private int countActiveAppThreadGroups(ThreadGroup tg)
    {
        int count = 0;
        ThreadGroup[] groups = new ThreadGroup[tg.activeGroupCount()];
        tg.enumerate(groups);

        for (int i = 0; i < groups.length; ++i)
        {
            if (groups[i] == null) continue;

            if (groups[i] instanceof AppThreadGroup)
            {
                ++count;
            }
            else
            {
                count += countActiveAppThreadGroups(groups[i]);
            }
        }

        return count;
    }

    public static void doDestroyed(AppContext appContext) throws Exception
    {
        doDestroyed(appContext, 10000);
    }

    public static void doDestroyed(AppContext appContext, long timeout) throws Exception
    {
        ThreadGroup tg = appContext.getThreadGroup();

        appContext.notifyDestroyed();

        if (timeout > 0 && tg != null)
        {
            int countdown = (int) (timeout / 1000);
            while (!tg.isDestroyed() && countdown-- >= 0)
            {
                Thread.sleep(1000);
            }
            if (!tg.isDestroyed())
            {
                fail("Timeout waiting for destroyed");
            }
            // A little leeway...
            Thread.sleep(100);
        }
    }

    public static void doPaused(AppContext appContext) throws Exception
    {
        appContext.notifyPaused();
    }

    public static void doActive(AppContext appContext) throws Exception
    {
        appContext.notifyActive();
    }

    /**
     * Create a thread within the thread group. The thread will sleep 100s.
     * 
     * @param tg
     *            the original parent thread group
     * @param name
     *            the name to be used for the thread
     * @return the new thread, already started
     */
    private Thread createThread(ThreadGroup tg, String name)
    {
        Thread newThread = new Thread(tg, name)
        {
            public void run()
            {
                try
                {
                    Thread.sleep(100000);
                    System.out.println("shutdownProcess no good");
                }
                catch (InterruptedException e)
                {
                    // ignore
                }
            }
        };
        newThread.start();

        return newThread;
    }

    /**
     * Create a thread group within the given thread. Then create a number of
     * sub-threads and thread groups.
     * 
     * @param tg
     *            the original parent thread group
     * @param name
     *            the name to be used for the threadgroup
     * @param threads
     *            the number of direct sub-threads
     * @param groups
     *            the number of direct sub-groups
     * @return the new thread group
     */
    private ThreadGroup createThreadGroup(ThreadGroup tg, String name, int threads, int groups)
    {
        ThreadGroup newGroup = new ThreadGroup(tg, name);

        for (int i = 0; i < threads; ++i)
            createThread(newGroup, name + "-" + i);

        for (int i = 0; i < groups; ++i)
            createThreadGroup(newGroup, name + "-" + i, threads - 1, groups - 1);

        return newGroup;
    }

    /**
     * A dummy application used to test <code>unload()</code>. All other
     * operations do nothing (they will actually throw an exception).
     * 
     * @author Aaron Kamienski
     */
    private static class DummyApp extends XletApp
    {
        public DummyApp(final AppID appID, AppDomainImpl domain)
        {
            super(new AppEntry() {{
                this.id = appID;
                version = 1;
            }}, domain);
        }

        public boolean unloaded;

        public int unloadedCount;

        void clear()
        {
            unloaded = false;
            unloadedCount = 0;
        }

        synchronized void unload()
        {
            unloaded = true;
            ++unloadedCount;
            notifyAll();
        }

        // Don't expect these to ever be called
        public void load()
        {
            fail("Unexpected");
        }

        public void init()
        {
            fail("Unexpected");
        }

        public void start()
        {
            fail("Unexpected");
        }

        public void start(String args[])
        {
            fail("Unexpected");
        }

        public void resume()
        {
            fail("Unexpected");
        }

        public void stop(boolean f)
        {
            fail("Unexpected");
        }
    }

    /**
     * Dummy domain used by the AppContext under test. Mainly used for
     * <code>getAppEntry()</code>, which returns an otherwise static
     * <code>AppEntry</code> with the desired <code>AppID</code>.
     */
    private static class DummyDomain extends AppDomainImpl
    {
        public AppID getEntryCalled;

        public boolean getServiceCalled;

        public static final AppID UNKNOWN = new AppID(TestAppIDs.APPCONTEXT, 999);

        public DummyDomain()
        {
            super(new TestServiceContext(), null);
        }

        void clear()
        {
            getEntryCalled = null;
            getServiceCalled = false;
        }

        AppEntry getAppEntry(AppID id)
        {
            getEntryCalled = id;

            if (id == UNKNOWN) return null;

            AppEntry entry = new AppEntry();

            entry.id = id;
            entry.transportProtocols = new AppEntry.TransportProtocol[] { new AppEntry.LocalTransportProtocol() };
            entry.baseDirectory = "/";
            entry.className = XletAppTest.DummyJTVXlet.class.getName();
            entry.classPathExtension = new String[0];
            entry.controlCode = org.ocap.application.OcapAppAttributes.PRESENT;
            entry.priority = 33;
            entry.version = 0;
            // fill in other stuff as necessary...

            return entry;
        }

        javax.tv.service.navigation.ServiceDetails getCurrentServiceDetails()
        {
            getServiceCalled = true;
            return new DummyServiceDetails();
        }

        private class DummyServiceDetails implements javax.tv.service.navigation.ServiceDetails
        {

            org.ocap.service.AbstractService service;

            DummyServiceDetails()
            {
                service = new org.ocap.service.AbstractService()
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

            public void addServiceComponentChangeListener(ServiceComponentChangeListener listener)
            {
                // TODO Auto-generated method stub

            }

            public DeliverySystemType getDeliverySystemType()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public String getLongName()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public ProgramSchedule getProgramSchedule()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public Service getService()
            {
                return service;
            }

            public ServiceType getServiceType()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public void removeServiceComponentChangeListener(ServiceComponentChangeListener listener)
            {
                // TODO Auto-generated method stub

            }

            public SIRequest retrieveComponents(SIRequestor requestor)
            {
                // TODO Auto-generated method stub
                return null;
            }

            public SIRequest retrieveServiceDescription(SIRequestor requestor)
            {
                // TODO Auto-generated method stub
                return null;
            }

            public Locator getLocator()
            {
                return service.getLocator();
            }

            public ServiceInformationType getServiceInformationType()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public Date getUpdateTime()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public int[] getCASystemIDs()
            {
                // TODO Auto-generated method stub
                return null;
            }

            public boolean isFree()
            {
                // TODO Auto-generated method stub
                return false;
            }

        }
    }

    protected static AppContext createImplObject(CCMgr ccMgr)
    {
        AppDomainImpl domain = new DummyDomain();
        AppID id = new AppID(TestAppIDs.APPCONTEXT, 1);
        XletApp app = new DummyApp(id, domain);
        return ccMgr.createInstance(app, id, domain);
    }

    /**
     * Dummy <code>AuthManager</code>.
     * 
     * @author Aaron Kamienski
     */
    static class DummyAuthMgr implements AuthManager
    {
        int initType = AuthInfo.AUTH_SIGNED_OCAP;

        int createAuthCtxCalled;

        int setAuthCtxCalled;

        DummyAuthCtx lastCreatedCtx;

        public void clear()
        {
            createAuthCtxCalled = 0;
            setAuthCtxCalled = 0;
        }

        private AuthContext ac;

        public AuthContext createAuthCtx(String initialFile, int signers, int orgId)
        {
            ++createAuthCtxCalled;
            return lastCreatedCtx = new DummyAuthCtx(initType);
        }

        public void setAuthCtx(CallerContext cc, AuthContext authCtx)
        {
            ++setAuthCtxCalled;
            ac = authCtx;
        }

        public AuthContext getAuthCtx(CallerContext cc)
        {
            return ac;
        }

        public AuthInfo getClassAuthInfo(String targName, FileSys fs)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public AuthInfo getDirAuthInfo(String dir, FileSys fs, String[] files) throws IOException
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public AuthInfo getFileAuthInfo(String targName, FileSys fs, byte[] file) throws IOException
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public AuthInfo getFileAuthInfo(String targName, FileSys fs) throws IOException
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public String[] getHashfileNames(String dir, FileSys fs) throws IOException
        {
            // will make http always fail...
            throw new UnsupportedOperationException("unimplemented");
        }

        public X509Certificate[][] getSigners(String targName, boolean knownRoot, FileSys fs, byte[] file)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public X509Certificate[][] getSigners(String targName, FileSys fs, byte[] file)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public void invalidate(String targName)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public void registerCRLMount(String path)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public void setPrivilegedCerts(byte[] codes)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public void unregisterCRLMount(String path)
        {
            throw new UnsupportedOperationException("unimplemented");
        }

        public void destroy()
        {
            throw new UnsupportedOperationException("unimplemented");
        }
    }

    /**
     * Dummy authentication context.
     * 
     * @author Aaron Kamienski
     */
    public static class DummyAuthCtx implements AuthContext
    {
        public int getAppSignedStatusCalled;

        public int getClassAuthInfoCalled;

        public int type = AuthInfo.AUTH_SIGNED_OCAP;

        DummyAuthCtx(int type)
        {
            this.type = type;
        }

        public int getAppSignedStatus()
        {
            ++getAppSignedStatusCalled;
            return type;
        }

        public AuthInfo getClassAuthInfo(String targName, FileSys fs)
        {
            ++getClassAuthInfoCalled;

            URL url = getClass().getResource(targName);
            return url != null ? (new DummyAuth(type, url)) : null;
        }
    }

    /**
     * Dummy file authentication.
     * <p>
     * This is the only way that we load files through the class loader.
     * 
     * @author Aaron Kamienski
     */
    static class DummyAuth implements AuthInfo
    {
        DummyAuth(int type, URL url)
        {
            this.type = type;

            try
            {
                this.bytes = getData(url);
            }
            catch (IOException e)
            {
                this.bytes = null;
                e.printStackTrace();
            }
        }

        private byte[] getData(URL url) throws IOException
        {
            URLConnection conn = url.openConnection();
            byte[] array = new byte[conn.getContentLength()];

            InputStream is = conn.getInputStream();
            int n;
            int ofs = 0;
            while (ofs < array.length && (n = is.read(array, ofs, array.length - ofs)) >= 0)
            {
                ofs += n;
            }
            return array;
        }

        public int getClassAuth()
        {
            return (bytes == null) ? AuthInfo.AUTH_UNKNOWN : type;
        }

        public byte[] getFile()
        {
            return bytes;
        }

        public boolean isSigned()
        {
            return bytes != null && type != AuthInfo.AUTH_FAIL && type != AuthInfo.AUTH_UNKNOWN
                    && type != AuthInfo.AUTH_UNSIGNED;
        }

        private byte[] bytes;

        private int type;
    }

    protected AppContext appContext;

    protected DummyApp app;

    protected AppID id;

    protected DummyDomain domain;

    protected AuthManager savedAm;

    protected DummyAuthMgr am;

    protected AbstractCallerContext createAbstractCallerContext()
    {
        return createInstance(app, id, domain);
    }

    protected void destroyAbstractCallerContext(AbstractCallerContext cc)
    {
        ((AppContext) cc).notifyDestroyed();
    }

    protected AppContext createInstance(XletApp xletApp, AppID appId, AppDomainImpl appDomain)
    {
        return ccMgr.createInstance(xletApp, appId, appDomain);
    }

    protected void setUp() throws Exception
    {
        // mount = ClassLoaderFileSys.mount(getClass().getClassLoader());
        id = new AppID(TestAppIDs.APPCONTEXT, 2);
        domain = new DummyDomain();
        app = new DummyApp(id, domain);

        super.setUp();

        // Allow any class to be loaded for AppContext
        // AppClassLoader.bypassPublicAPICheck(true); // TODO: remove

        appContext = (AppContext) acc;

        // Why would it return null?
        // It would return null if the previously created AppContext with the
        // same id,
        // hadn't cleaned up yet.
        // Currently we simply wait a time in tearDown() for this to occur.
        // This probably isn't good enough, and may run into problems on slower
        // systems.
        // Or systems with different threading models than what this was tested
        // on when written!

        // ???? Do we *really* need to use createInstance() for testing?
        // Couldn't we just create a new instance directly?

        assertNotNull("AppContext.createInstance() returned null", appContext);

        // This is a workaround for bug 5072.
        // Ensure that the FM is up first (else we may end up being stuck with
        // our AuthManager forever)
        ManagerManager.getInstance(FileManager.class);

        // Replace the AuthManager
        savedAm = (AuthManager) ManagerManager.getInstance(AuthManager.class);
        am = new DummyAuthMgr();
        ManagerManagerTest.updateManager(AuthManager.class, am.getClass(), false, am);
    }

    private void tearDownImpl() throws Exception
    {
        if (appContext != null)
        {
            doDestroyed(appContext);
            // !!!! Should wait for EventQueue thread to die off...
            Thread.sleep(100);
            System.gc();
            System.runFinalization();
            appContext = null;
        }
    }

    protected void tearDown() throws Exception
    {
        ManagerManagerTest.updateManager(AuthManager.class, savedAm.getClass(), false, savedAm);

        tearDownImpl();

        // Restore public API check
        AppClassLoader.bypassPublicAPICheck(false);

        super.tearDown();
    }

    public AppContextTest(String test)
    {
        super(test);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(AppContextTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new AppContextTest(tests[i]));
            return suite;
        }
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

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AppContextTest.class);
        ImplFactory factory = new ContextImplFactory()
        {
            CCMgr ccMgr;

            Manager save;

            public void setUp() throws Exception
            {
                if (ccMgr != null)
                {
                    ccMgr.destroy();
                    ccMgr = null;
                }
                ccMgr = new CCMgr();
                save = ManagerManager.getInstance(CallerContextManager.class);
                ManagerManagerTest.updateManager(CallerContextManager.class, ccMgr.getClass(), true, ccMgr);
            }

            public void tearDown() throws Exception
            {
                if (save != null)
                    ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
                ccMgr.destroy();
                ccMgr = null;
            }

            public Object createImplObject()
            {
                return AppContextTest.createImplObject(ccMgr);
            }

            public void makeDestroyed(CallerContext ctx) throws Exception
            {
                doDestroyed((AppContext) ctx);
            }

            public void makePaused(CallerContext ctx) throws Exception
            {
                doPaused((AppContext) ctx);
            }

            public void makeActive(CallerContext ctx) throws Exception
            {
                doActive((AppContext) ctx);
            }
        };
        InterfaceTestSuite ctxSuite = CallerContextTest.isuite();

        ctxSuite.addFactory(factory);
        suite.addTest(ctxSuite);

        return suite;
    }
}
