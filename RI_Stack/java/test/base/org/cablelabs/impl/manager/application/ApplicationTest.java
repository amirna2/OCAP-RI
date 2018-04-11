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

import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.util.TaskQueue;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppProxyTest;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppProxyTest.Listener;

/**
 * Tests the Application implementation.
 * 
 * @author Aaron Kamienski
 */
public abstract class ApplicationTest extends TestCase
{
    /**
     * Tests the constructor.
     */
    public abstract void testConstructor();

    /**
     * Tests notifyListeners(). Ensure that listeners are notified via the
     * appropriate context.
     */
    public void testNotifyListeners() throws Exception
    {
        DummyListener[] listener = new DummyListener[4];
        DummyContext[] context = new DummyContext[listener.length];

        // Replace the CallerContextManager with our own
        // so that getCurrentContext() works for us.
        replaceCCMgr();

        try
        {
            for (int i = 0; i < listener.length; ++i)
            {
                context[i] = new DummyContext();
                listener[i] = new DummyListener();
                final DummyListener l = listener[i];
                context[i].runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        application.addAppStateChangeEventListener(l);
                    }
                });
                assertEquals("Listeners should not have been called yet", 0, listener[i].called);
            }

            application.notifyListeners(new AppID(TestAppIDs.APPLICATION, 1), AppProxy.NOT_LOADED, AppProxy.DESTROYED,
                    application, false, true);
            Thread.sleep(100); // KLUDGE!!
            for (int i = 0; i < listener.length; ++i)
            {
                assertEquals("Listeners should have been called", 1, listener[i].called);
                assertSame("Listener should have been called on CallerContext", context[i].tg, listener[i].tg);
            }
        }
        finally
        {
            for (int i = 0; i < context.length; ++i)
            {
                if (context[i] != null) context[i].dispose();
            }
        }
    }

    /**
     * Tests dispose(). Should move to the disposed state. After which, no
     * further state changes should be allowed.
     */
    public void testDispose() throws Exception
    {
        Listener l = new Listener();
        application.addAppStateChangeEventListener(l);

        // Dispose the application.
        synchronized (l)
        {
            l.clear();
            application.dispose();
            l.waitNextEvent(1000);
        }
        // Verify that the DESTROYED state is entered
        assertEquals("Expected DESTROYED state to be entered", AppProxy.DESTROYED, application.getState());

        try
        {
            // Verify that state changes fail.
            AppStateChangeEvent e = null;
            synchronized (l)
            {
                l.clear();
                application.start();
                e = l.waitNextEvent(1000);
            }
            assertTrue("Expected failure for 'disposed' application", e.hasFailed());
            assertEquals("Unexpected from state for 'disposed' application", AppProxy.DESTROYED, e.getFromState());

            assertEquals("Expected DESTROYED state to constant for 'disposed' app", AppProxy.DESTROYED,
                    application.getState());
        }
        finally
        {
            // To prevent cleanup
            // application = null;
        }
    }

    /**
     * Tests getAppID().
     */
    public void testGetAppID()
    {
        AppID id = application.getAppID();

        assertNotNull("Expected a non-null AppID", id);
        assertEquals("Unexpected AppID returned", getExpectedAppID(), id);
        assertEquals("Expected same AppID returned on multiple calls", id, application.getAppID());
    }

    /**
     * Tests getAppDomain().
     */
    public void testGetAppDomain()
    {
        AppDomain id = application.getAppDomain();

        assertNotNull("Expected a non-null AppDomain", id);
        assertSame("Unexpected AppDomain returned", getExpectedAppDomain(), id);
        assertSame("Expected same AppDomain returned on multiple calls", id, application.getAppDomain());
    }

    /**
     * Tests forgetListeners(). Ensure that listeners are NOT notified via the
     * appropriate context.
     */
    public void testForgetListeners() throws Exception
    {
        DummyListener[] listener = new DummyListener[4];
        DummyContext[] context = new DummyContext[4];

        // Replace the CallerContextManager with our own
        // so that getCurrentContext() works.
        replaceCCMgr();

        try
        {
            for (int i = 0; i < listener.length; ++i)
            {
                context[i] = new DummyContext();
                listener[i] = new DummyListener();
                final DummyListener l = listener[i];
                context[i].runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        application.addAppStateChangeEventListener(l);
                    }
                });
                assertEquals("Listeners should not have been called yet", 0, listener[i].called);
            }

            application.forgetListeners(context[1]);
            application.forgetListeners(context[0]);

            application.notifyListeners(new AppID(TestAppIDs.APPLICATION, 2), AppProxy.NOT_LOADED, AppProxy.DESTROYED,
                    application, false, true);
            Thread.sleep(100); // KLUDGE!!

            // Verify the forgotten ones were forgotten
            for (int i = 0; i < 2; ++i)
            {
                assertEquals("Listeners should NOT have been called", 0, listener[i].called);
            }
            // And the others weren't
            for (int i = 2; i < listener.length; ++i)
            {
                assertEquals("Listeners should have been called", 1, listener[i].called);
                assertSame("Listener should have been called on CallerContext", context[i].tg, listener[i].tg);
            }
        }
        finally
        {
            for (int i = 0; i < context.length; ++i)
            {
                if (context[i] != null) context[i].dispose();
            }
        }
    }

    public ApplicationTest(String name)
    {
        super(name);
    }

    protected Application application;

    /**
     * The expected AppID for the Application created by
     * {@link #createApplication}.
     */
    protected abstract AppID getExpectedAppID();

    /**
     * The expected AppDomain for the Application created by
     * {@link #createApplication}.
     */
    protected abstract AppDomain getExpectedAppDomain();

    /**
     * To be overridden to create the Application instance to be tested.
     */
    protected abstract Application createApplication();

    protected static final boolean PRIV_DEBUG = false;

    protected static final boolean DEBUG = AppProxyTest.DEBUG || PRIV_DEBUG;

    protected void setUp() throws Exception
    {
        if (DEBUG) System.out.println("===============" + getName() + "===============");
        super.setUp();

        application = createApplication();
    }

    protected void tearDown() throws Exception
    {
        application = null;
        restoreCCMgr();

        super.tearDown();
    }

    private CallerContextManager save;

    private void replaceCCMgr()
    {
        save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, new CCMgr(save));
    }

    private void restoreCCMgr()
    {
        if (save != null) ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
    }

    /**
     * Replacement CallerContextManager so we can affect the getCurrentContext()
     * method.
     */
    public static class CCMgr implements CallerContextManager
    {
        private CallerContextManager ccm;

        public CallerContext alwaysReturned = null;

        private Hashtable runAsMap = new Hashtable();

        public CCMgr(CallerContextManager original)
        {
            ccm = original;
        }

        public CallerContext getCurrentContext()
        {
            if (alwaysReturned != null) return alwaysReturned;

            // First check runAsMap
            Thread t = Thread.currentThread();
            CallerContext cached = (CallerContext) runAsMap.get(t);
            if (cached != null) return cached;

            // Find ThreadGroup
            ThreadGroup tg = t.getThreadGroup();
            do
            {
                if (tg instanceof DummyContext.TG)
                {
                    return ((DummyContext.TG) tg).cc;
                }
                tg = tg.getParent();
            }
            while (tg != null);

            return ccm.getCurrentContext();
        }

        public void runAsContext(CallerContext ct, Runnable run)
        {
            Thread t = Thread.currentThread();
            CallerContext save = (CallerContext) runAsMap.get(t);
            try
            {
                runAsMap.put(t, ct);
                run.run();
            }
            finally
            {
                if (save != null)
                    runAsMap.put(t, save);
                else
                    runAsMap.remove(t);
            }
        }

        public CallerContext getSystemContext()
        {
            return ccm.getSystemContext();
        }

        public static Manager getInstance()
        {
            throw new UnsupportedOperationException("Unexpected");
        }

        public void destroy()
        {
            throw new UnsupportedOperationException("Unexpected");
        }
    }

    /**
     * An AppStateChangeEventListener implementation that records whether the
     * listener was called, the event argument, and the current thread's
     * ThreadGroup.
     */
    static class DummyListener implements AppStateChangeEventListener
    {
        public AppStateChangeEvent e;

        public ThreadGroup tg;

        int called = 0;

        public void stateChange(AppStateChangeEvent event)
        {
            this.e = event;
            this.tg = Thread.currentThread().getThreadGroup();
            ++called;
        }
    }

    /**
     * A CallerContext implementation that allows the test to control the thread
     * group used and record whether the runInContext method is called.
     */
    public static class DummyContext implements CallerContext
    {
        public boolean called = false;

        public Runnable ran;

        public ThreadGroup tg = new TG("Test", this);

        protected Hashtable callbackData = new Hashtable();

        ClassLoader cl = new ClassLoader()
        {
            protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                // Defer
                return getClass().getClassLoader().loadClass(name);
            }
        };

        private AppExecQueue q = AppExecQueue.createInstance(null, tg);

        public void addCallbackData(CallbackData data, Object key)
        {
            called = true;
            callbackData.put(key, data);
        }

        public void removeCallbackData(Object key)
        {
            called = true;
            callbackData.remove(key);
        }

        public CallbackData getCallbackData(Object key)
        {
            called = true;
            return (CallbackData) callbackData.get(key);
        }

        public void runInContext(Runnable run) throws SecurityException, IllegalStateException
        {
            called = true;
            q.post(run);
            ran = run;
        }

        public void runInContextSync(Runnable run) throws SecurityException, IllegalStateException,
                InvocationTargetException
        {
            called = true;
            //runAsContext(run);
            ran = run;
        }

        public void runInContextAsync(Runnable run)
        {
            called = true;
            Thread t = new Thread(tg, run);
            t.start();
            ran = run;
        }

        public boolean isAlive()
        {
            called = true;
            return true;
        }

        public void checkAlive()
        {
            called = true;
        }

        public boolean isActive()
        {
            called = true;
            return true;
        }

        public Object get(Object key)
        {
            if (CallerContext.THREAD_GROUP.equals(key)) return tg;
            throw new UnsupportedOperationException();
        }

        public TaskQueue createTaskQueue()
        {
            throw new UnsupportedOperationException();
        }

        public void dispose() throws Exception
        {
            q.dispose();
            q = null;
            Thread.sleep(100); // kludge
            try
            {
                tg.destroy();
                tg = null;
            }
            catch (IllegalArgumentException e)
            {
                // ignored... it might hang around, but who cares...
            }
        }

        /**
         * Used to detect current thread group.
         */
        public static class TG extends ThreadGroup
        {
            public CallerContext cc;

            public TG(String name, CallerContext cc)
            {
                super(name);
                this.cc = cc;
            }
        }

        public void runInContextAWT(Runnable run) throws SecurityException,
                IllegalStateException
        {
        }
    }
}
