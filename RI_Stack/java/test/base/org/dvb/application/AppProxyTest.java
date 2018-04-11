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

package org.dvb.application;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;
import org.cablelabs.test.ProxySecurityManager;
import org.cablelabs.test.ProxySecurityManager.NullSecurityManager;

import java.security.Permission;
import java.util.Enumeration;
import java.util.Vector;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

/**
 * Tests AppProxy interface.
 * 
 * @author Aaron Kamienski
 */
public class AppProxyTest extends InterfaceTestCase
{
    protected static final long WAIT_TIME = 5000;

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new org.cablelabs.test.iftc.InterfaceTestSuite(AppProxyTest.class);
        suite.setName(AppProxy.class.getName());
        return suite;
    }

    public static InterfaceTestSuite isuite(String[] tests)
    {
        InterfaceTestSuite suite = new org.cablelabs.test.iftc.InterfaceTestSuite(AppProxyTest.class, tests);
        suite.setName(AppProxy.class.getName());
        return suite;
    }

    public AppProxyTest(String name, ImplFactory f)
    {
        this(name, AppProxy.class, f);
    }

    protected AppProxyTest(String name, Class impl, ImplFactory f)
    {
        super(name, impl, f);
        setUseClassInName(false); // simplify output... see InterfaceTestCase
                                  // docs
        factory = (AppProxyImplFactory) f;
    }

    protected AppProxy createAppProxy()
    {
        return (AppProxy) createImplObject();
    }

    protected AppProxy app;

    protected Listener listener;

    protected AppProxyImplFactory factory;

    protected boolean noUnload;

    protected void setUp() throws Exception
    {
        if (DEBUG) System.out.println("===============" + getName() + "===============");
        super.setUp();
        app = createAppProxy();
        listener = new Listener();
        app.addAppStateChangeEventListener(listener);

        for (int i = 0; i < 3; i++)
        {
            System.gc();
        }

    }

    protected void tearDown() throws Exception
    {
        try
        {
            if (DEBUG) System.out.println("###############" + getName() + "###############");
            app.removeAppStateChangeEventListener(listener);
            if (!noUnload) factory.makeUnloaded(app); // make sure it goes away
            factory = null;
            app = null;
            super.tearDown();
        }
        finally
        {
            if (DEBUG) System.out.println("!!!!!!!!!!!!!!!" + getName() + "!!!!!!!!!!!!!!!");
        }
    }

    protected String stateToString(int state)
    {
        switch (state)
        {
            case AppProxy.STARTED:
                return "STARTED";
            case AppProxy.DESTROYED:
                return "DESTROYED";
            case AppProxy.NOT_LOADED:
                return "NOT_LOADED";
            case AppProxy.PAUSED:
                return "PAUSED";
            default:
                return "Unknown=" + state;
        }
    }

    /**
     * Tests getState(). Ensure that initial state is correct. Other states will
     * be checked as part of other tests. <li><i>DVB?</i> <li><i>OCAP?</i>
     */
    public void testGetState() throws InterruptedException
    {
        Listener l = listener;
        AppStateChangeEvent e;

        assertEquals("The initial state of the application should be NOT_LOADED", AppProxy.NOT_LOADED, app.getState());
        assertEquals("Repeated calls should return same", AppProxy.NOT_LOADED, app.getState());

        synchronized (l)
        {
            app.start();
            e = l.waitNextEvent(WAIT_TIME);
        }
        assertEquals("Unexpected value from getState()", AppProxy.STARTED, app.getState());
        assertEquals("Repeated calls should return same", AppProxy.STARTED, app.getState());
        assertNotNull("Expected event", e);
        assertFalse("Expected successful event", e.hasFailed());

        // Following failure, should not affect state
        synchronized (l)
        {
            app.resume();
            e = l.waitNextEvent(WAIT_TIME);
        }
        assertEquals("State should not change following failure", AppProxy.STARTED, app.getState());
        assertEquals("Repeated calls should return same", AppProxy.STARTED, app.getState());
        assertNotNull("Expected event", e);
        assertTrue("Expected failed event", e.hasFailed());

    }

    /**
     * Performs test for testAddRemoveAppStateChangeEventListener().
     */
    public void do_testAddRemoveAppStateChangeEventListener(Listener[] listeners) throws Exception
    {
        // Reset counters
        for (int i = 0; i < listeners.length; ++i)
        {
            listeners[i].clear();
        }
        // Perform state change (attempt, can fail)
        app.pause();
        // Verify expected events
        for (int i = 0; i < listeners.length; ++i)
        {
            synchronized (listeners[i])
            {
                if (listeners[i].expected > 0 && listeners[i].events.size() == 0) listeners[i].waitEvent(WAIT_TIME);
            }
            assertEquals("Listener should've been called n times", listeners[i].expected, listeners[i].events.size());
        }
    }

    /**
     * Tests add|removeAppStateChangeEventListener(). Note that we rely on this
     * capability to perform the other tests.
     * <ul>
     * <li>multiple adds
     * <li>multiple adds of same
     * <li>removal halts notification
     * <li><i>DVB?</i>
     * <li><i>OCAP?</i>
     * </ul>
     */
    public void testAddRemoveAppStateChangeEventListener() throws Exception
    {
        Listener[] listeners = { new Listener(), new Listener(), null, new Listener(), null };
        listeners[2] = listeners[3]; // to be added twice
        listeners[4] = listeners[0]; // to be added twice

        // Add listeners
        for (int i = 0; i < listeners.length; ++i)
        {
            app.addAppStateChangeEventListener(listeners[i]);
            listeners[i].expected = 1;
            // Cause a state change & verify callbacks
            do_testAddRemoveAppStateChangeEventListener(listeners);
        }

        // Remove listeners, in no particular order
        // odd...
        for (int i = 1; i < listeners.length; i += 2)
        {
            app.removeAppStateChangeEventListener(listeners[i]);
            listeners[i].expected = 0;
            // Cause a state change & verify callbacks
            do_testAddRemoveAppStateChangeEventListener(listeners);
        }
        // even...
        for (int i = 0; i < listeners.length; i += 2)
        {
            app.removeAppStateChangeEventListener(listeners[i]);
            listeners[i].expected = 0;
            // Cause a state change & verify callbacks
            do_testAddRemoveAppStateChangeEventListener(listeners);
        }
        // An extraneous removal shouldn't be a problem
        for (int i = 0; i < listeners.length; ++i)
            app.removeAppStateChangeEventListener(listeners[i]);
        // Cause a state change & verify callbacks
        do_testAddRemoveAppStateChangeEventListener(listeners);
    }

    /**
     * Cause transition from NOT_LOADED to STARTED.
     */
    protected void doStarted() throws Exception
    {
        Listener l = listener;

        if (app.getState() == AppProxy.STARTED) return;
        synchronized (l)
        {
            l.clear();
            app.start();
            l.waitNextEvent(WAIT_TIME);
        }
        checkState(AppProxy.STARTED);
    }

    /**
     * Cause transition from NOT_LOADED to PAUSED.
     */
    protected void doPaused() throws Exception
    {
        Listener l = listener;

        if (app.getState() == AppProxy.PAUSED) return;
        if (app.getState() != AppProxy.STARTED) doStarted();
        synchronized (l)
        {
            l.clear();
            app.pause();
            l.waitNextEvent(WAIT_TIME);
        }
        checkState(AppProxy.PAUSED);
    }

    /**
     * Cause transition from NOT_LOADED to RESUMED.
     */
    protected void doResumed() throws Exception
    {
        Listener l = listener;

        doPaused();
        synchronized (l)
        {
            l.clear();
            app.resume();
            l.waitNextEvent(WAIT_TIME);
        }
        checkState(AppProxy.STARTED);
    }

    /**
     * Cause transition from NOT_LOADED to DESTROYED. Uses the
     * {@link AppProxyImplFactory#makeDestroyedPermanent} to create a
     * <i>permanent</i> (i.e., not transient) transition to the DESTROYED state.
     */
    protected void doDestroyed() throws Exception
    {
        Listener l = listener;

        synchronized (l)
        {
            l.clear();
            noUnload = true; // don't bother with unload if DESTROYED is
                             // permanent
            factory.makeDestroyedPermanent(app);
            l.waitNextEvent(WAIT_TIME);
        }
        checkState(AppProxy.DESTROYED);
    }

    /**
     * Cause transition from NOT_LOADED to simply <i>launched</i>. This is
     * expected to be overridden by subclass tests. This implementation simply
     * calls doPaused().
     */
    protected void doLaunched() throws Exception
    {
        doPaused();
    }

    /**
     * Test a transition from the NOT_LOADED state.
     */
    protected void doTest_fromNOT_LOADED(XTest test, boolean failed, int fromState, int toState) throws Exception
    {
        Listener l = listener;
        AppStateChangeEvent e;

        checkState(AppProxy.NOT_LOADED);

        synchronized (l)
        {
            l.clear();
            test.doX(app);
            e = l.waitNextEvent(WAIT_TIME);
        }
        checkEvent(e, failed, toState, fromState);
    }

    /**
     * Test a transition from the PAUSED state.
     */
    protected void doTest_fromPAUSED(XTest test, boolean failed, int fromState, int toState) throws Exception
    {
        Listener l = listener;
        AppStateChangeEvent e;

        doPaused();

        synchronized (l)
        {
            l.clear();
            test.doX(app);
            e = l.waitNextEvent(WAIT_TIME);
        }
        checkEvent(e, failed, toState, fromState);
    }

    /**
     * Test a transition from the STARTED state.
     */
    protected void doTest_fromSTARTED(XTest test, boolean failed, int fromState, int toState) throws Exception
    {
        Listener l = listener;
        AppStateChangeEvent e;

        doStarted();

        synchronized (l)
        {
            l.clear();
            test.doX(app);
            e = l.waitNextEvent(WAIT_TIME);
        }
        checkEvent(e, failed, toState, fromState);
    }

    /**
     * Test a transition from the RESUMED state.
     */
    protected void doTest_fromRESUMED(XTest test, boolean failed, int fromState, int toState) throws Exception
    {
        Listener l = listener;
        AppStateChangeEvent e;

        doResumed();

        synchronized (l)
        {
            l.clear();
            test.doX(app);
            e = l.waitNextEvent(WAIT_TIME);
        }
        checkEvent(e, failed, toState, fromState);
    }

    /**
     * Test a transition from the DESTROYED state.
     */
    protected void doTest_fromDESTROYED(XTest test, boolean failed, int fromState, int toState) throws Exception
    {
        Listener l = listener;
        AppStateChangeEvent e;

        doDestroyed();

        synchronized (l)
        {
            l.clear();
            test.doX(app);
            e = l.waitNextEvent(WAIT_TIME);
        }
        checkEvent(e, failed, toState, fromState);
    }

    /**
     * Test a transition from the NOT_LOADED state.
     */
    protected void doTest_fromNOT_LOADED(XTest test, boolean failed, int toState) throws Exception
    {
        doTest_fromNOT_LOADED(test, failed, AppProxy.NOT_LOADED, toState);
    }

    /**
     * Test a transition from the PAUSED state.
     */
    protected void doTest_fromPAUSED(XTest test, boolean failed, int toState) throws Exception
    {
        doTest_fromPAUSED(test, failed, AppProxy.PAUSED, toState);
    }

    /**
     * Test a transition from the STARTED state.
     */
    protected void doTest_fromSTARTED(XTest test, boolean failed, int toState) throws Exception
    {
        doTest_fromSTARTED(test, failed, AppProxy.STARTED, toState);
    }

    /**
     * Test a transition from the RESUMED state.
     */
    protected void doTest_fromRESUMED(XTest test, boolean failed, int toState) throws Exception
    {
        doTest_fromRESUMED(test, failed, AppProxy.STARTED, toState);
    }

    /**
     * Test a transition from the DESTROYED state.
     */
    protected void doTest_fromDESTROYED(XTest test, boolean failed, int toState) throws Exception
    {
        doTest_fromDESTROYED(test, failed, AppProxy.DESTROYED, toState);
    }

    /**
     * Assert that the app is in the given state.
     */
    public static void checkState(AppProxy app, int state)
    {
        assertEquals("Unexpected application state", state, app.getState());
    }

    /**
     * Assert that the app is in the given state.
     */
    protected void checkState(int state)
    {
        checkState(app, state);
    }

    /**
     * Assert that the given event matches the expectations.
     */
    protected void checkEvent(AppStateChangeEvent e, boolean hasFailed, int toState, int fromState)
    {
        assertEquals("To state should be " + stateToString(toState), toState, e.getToState());
        assertEquals("From state should be " + stateToString(fromState), fromState, e.getFromState());
        assertEquals("Transition from " + stateToString(fromState) + " -> " + stateToString(toState) + " should "
                + (hasFailed ? "fail" : "succeed"), hasFailed, e.hasFailed());
    }

    /*
     * Tests pause(). <ul> <li> Requires the permissions to stop an application.
     * <li> The application will be paused. <li> Calls to this method shall fail
     * if the application is not in the active state. <li> If the application
     * represented by this AppProxy is a DVB-J application, calling this method
     * will, if successful, result in the pauseXlet method being called on the
     * Xlet making up the DVB-J application. <li> <i>DVB?</i> <li> <i>OCAP?</i>
     * </ul>
     */

    /**
     * Test pause() from NOT_LOADED.
     */
    public void testPause_fromNOT_LOADED() throws Exception
    {
        doTest_fromNOT_LOADED(XTest.PAUSE, true, AppProxy.PAUSED);
    }

    /**
     * Test pause() from PAUSED.
     */
    public void testPause_fromPAUSED() throws Exception
    {
        doTest_fromPAUSED(XTest.PAUSE, true, AppProxy.PAUSED);
    }

    /**
     * Test pause() from STARTED.
     */
    public void testPause_fromSTARTED() throws Exception
    {
        doTest_fromSTARTED(XTest.PAUSE, false, AppProxy.PAUSED);
    }

    /**
     * Test pause() from RESUMED.
     */
    public void testPause_fromRESUMED() throws Exception
    {
        doTest_fromRESUMED(XTest.PAUSE, false, AppProxy.PAUSED);
    }

    /**
     * Test pause() from DESTROYED.
     */
    public void testPause_fromDESTROYED() throws Exception
    {
        doTest_fromDESTROYED(XTest.PAUSE, true, AppProxy.PAUSED);
    }

    /*
     * Tests start()/start(String[]). <ul> <li> This method will throw a
     * security exception if the application does not have the authority to
     * start applications. <li> Success requires: <ol> <li> the application
     * (DVB-J or DVB-HTML) is in the not loaded or paused states <li> a DVB-J
     * application is in the "loaded" state <li> a DVB-HTML application is in
     * the "loading" state </ol> <li> If the application was not loaded at the
     * moment of this call, then the application will be started <li> In the
     * case of a DVB-J application, it will be initialized and then started by
     * the Application Manager, hence causing the Xlet to go from NotLoaded to
     * Paused and then from Paused to Active. <li> If the application was in the
     * Paused state at the moment of the call and had never been in the Active
     * state, then the application will be started. <li> If the application
     * represented by this AppProxy is a DVB-J application, calling this method
     * will, if successful, result in the startXlet method being called on the
     * Xlet making up the DVB-J application. <li> This method is asynchronous
     * and its completion will be notified by an AppStateChangedEvent. <li> In
     * case of failure, the hasFailed method of the AppStateChangedEvent will
     * return true. <li> <i>DVB?</i> <li> <i>OCAP?</i> </ul>
     */

    /**
     * Test start() from NOT_LOADED.
     */
    public void testStart_fromNOT_LOADED() throws Exception
    {
        doTest_fromNOT_LOADED(XTest.START, false, AppProxy.NOT_LOADED, AppProxy.STARTED);
    }

    /**
     * Test start() from PAUSED.
     */
    public void testStart_fromPAUSED() throws Exception
    {
        doTest_fromPAUSED(XTest.START, true, AppProxy.STARTED);
    }

    /**
     * Test start() from STARTED.
     */
    public void testStart_fromSTARTED() throws Exception
    {
        doTest_fromSTARTED(XTest.START, true, AppProxy.STARTED);
    }

    /**
     * Test start() from RESUMED.
     */
    public void testStart_fromRESUMED() throws Exception
    {
        doTest_fromRESUMED(XTest.START, true, AppProxy.STARTED);
    }

    /**
     * Test start() from DESTROYED.
     */
    public void testStart_fromDESTROYED() throws Exception
    {
        doTest_fromDESTROYED(XTest.START, true, AppProxy.STARTED);
    }

    /*
     * Tests stop(). <ul> <li> A call to this method shall fail if the
     * application was already in the destroyed state. <li> This method call
     * will stop the application if it was in any other state before the call.
     * <li> If the application is in the NOT_LOADED state then it shall move
     * directly to the DESTROYED state with no other action being taken <li> If
     * the application represented by this AppProxy is a DVB-J application and
     * is not in the DESTROYED state then calling this method will, if
     * successful, result in the destroyXlet method being called on the Xlet
     * making up the DVB-J application with the same value for the parameter as
     * passed to this method. <li> This method is asynchronous and its
     * completion will be notified by an AppStateChangedEvent. <li> In case of
     * failure, the hasFailed method of the AppStateChangedEvent will return
     * true. <li> <i>DVB?</i> <li> <i>OCAP?</i> </ul>
     */

    /**
     * Test stop() from NOT_LOADED.
     */
    public void testStop_fromNOT_LOADED() throws Exception
    {
        doTest_fromNOT_LOADED(XTest.STOP, false, AppProxy.DESTROYED);
    }

    /**
     * Test stop() from PAUSED.
     */
    public void testStop_fromPAUSED() throws Exception
    {
        doTest_fromPAUSED(XTest.STOP, false, AppProxy.DESTROYED);
    }

    /**
     * Test stop() from STARTED.
     */
    public void testStop_fromSTARTED() throws Exception
    {
        doTest_fromSTARTED(XTest.STOP, false, AppProxy.DESTROYED);
    }

    /**
     * Test stop() from RESUMED.
     */
    public void testStop_fromRESUMED() throws Exception
    {
        doTest_fromRESUMED(XTest.STOP, false, AppProxy.DESTROYED);
    }

    /**
     * Test stop() from DESTROYED.
     */
    public void testStop_fromDESTROYED() throws Exception
    {
        doTest_fromDESTROYED(XTest.STOP, true, AppProxy.DESTROYED);
    }

    /*
     * Tests resume(). <ul> <li> The application will be started. <li> This
     * method will throw a security exception if the application does not have
     * the authority to resume the application. <li> Calls to this method shall
     * fail if the application is not in the paused state. <li> This method is
     * asynchronous and its completion will be notified by an
     * AppStateChangedEvent. <li> In case of failure, the hasFailed method of
     * the AppStateChangedEvent will return true. <li> If the application
     * represented by this AppProxy is a DVB-J application, calling this method
     * will, if successful, result in the startXlet method being called on the
     * Xlet making up the DVB-J application. <li> <i>DVB?</i> <li> <i>OCAP?</i>
     * </ul>
     */

    public void testResume_fromNOT_LOADED() throws Exception
    {
        doTest_fromNOT_LOADED(XTest.RESUME, true, AppProxy.STARTED);
    }

    public void testResume_fromPAUSED() throws Exception
    {
        doTest_fromPAUSED(XTest.RESUME, false, AppProxy.STARTED);
    }

    public void testResume_fromSTARTED() throws Exception
    {
        doTest_fromSTARTED(XTest.RESUME, true, AppProxy.STARTED);
    }

    public void testResume_fromRESUMED() throws Exception
    {
        doTest_fromRESUMED(XTest.RESUME, true, AppProxy.STARTED);
    }

    public void testResume_fromDESTROYED() throws Exception
    {
        doTest_fromDESTROYED(XTest.RESUME, true, AppProxy.STARTED);
    }

    /**
     * Used to implement tests that verify that Permissions are tested when
     * performing state-change operations. This is used to test under the
     * following conditions:
     * <ul>
     * <li>App was previously launched by <i>another</i> caller context
     * </ul>
     * 
     * @param test
     *            object that performs the tested operation
     */
    private void doTestPermission(XTest test)
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {

            // Clear current Permission (only catch the first)
            sm.p = null;

            // Perform transition
            test.doX(app);

            assertNotNull("SecurityManager.checkPermission should be called", sm.p);
            assertTrue("SecurityManager.checkPermission should be called with an AppsControlPermission",
                    sm.p instanceof AppsControlPermission);
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests that no permission checks are made when performing state-change
     * operations. This is used to test that permission checks aren't made under
     * the following conditions:
     * <ul>
     * <li>App was not previously launched.
     * <li>App was previously launched by the caller
     * </ul>
     * 
     * @param test
     *            object that performs the tested operation
     */
    public void doTestNoPermission(XTest test)
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {

            // Clear all permissions
            sm.p = null;
            sm.all.clear();

            // Perform transition
            test.doX(app);

            if (sm.all.size() > 0)
            {
                for (Enumeration e = sm.all.elements(); e.hasMoreElements();)
                {
                    Permission p = (Permission) e.nextElement();
                    assertFalse("Did not expect permission to be tested: " + p, p instanceof AppsControlPermission);
                }
            }
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests that no permission checks are made for a proxy launched by the same
     * caller.
     * 
     * @param test
     *            object that performs the tested operation
     * @throws Exception
     *             if errors occur launching the proxy
     */
    public void doTestNoPermission_launched(XTest test) throws Exception
    {
        doLaunched();

        doTestNoPermission(test);
    }

    public void doTestPermission_otherLaunched(XTest test) throws Exception
    {
        // Set up CCMgr for testing
        replaceCCMgr();
        try
        {
            DummyContext ctx = new DummyContext();
            try
            {
                // launch from other CallerContext
                final Exception[] failure = { null };
                ctx.runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            doLaunched();
                        }
                        catch (Exception e)
                        {
                            failure[0] = e;
                        }
                    }
                });
                if (failure[0] != null) throw failure[0];

                // Test permission check from this context
                doTestPermission(test);
            }
            finally
            {
                ctx.dispose();
            }
        }
        finally
        {
            // finally, restore original CCMgr
            restoreCCMgr();
        }
    }

    private CallerContextManager save;

    protected void replaceCCMgr()
    {
        save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, new CCMgr(save));
    }

    protected void restoreCCMgr()
    {
        if (save != null) ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
    }

    /**
     * Tests that permissions are checked pause().
     */
    public void testPausePermissions_otherLaunched() throws Exception
    {
        doTestPermission_otherLaunched(XTest.PAUSE);
    }

    /**
     * Tests that permissions are not checked pause().
     */
    public void testPausePermissions_notLaunched()
    {
        doTestNoPermission(XTest.PAUSE);
    }

    /**
     * Tests that permissions are not checked pause().
     */
    public void testPausePermissions_launched() throws Exception
    {
        doTestNoPermission_launched(XTest.PAUSE);
    }

    /**
     * Tests that permissions are checked start().
     */
    public void testStartPermissions_otherLaunched() throws Exception
    {
        doTestPermission_otherLaunched(XTest.START);
    }

    /**
     * Tests that permissions are not checked start().
     */
    public void testStartPermissions_notLaunched()
    {
        doTestNoPermission(XTest.START);
    }

    /**
     * Tests that permissions are not checked start().
     */
    public void testStartPermissions_launched() throws Exception
    {
        doTestNoPermission_launched(XTest.START);
    }

    /**
     * Tests that permissions are not checked stop().
     */
    public void testStopPermissions_otherLaunched() throws Exception
    {
        doTestPermission_otherLaunched(XTest.STOP);
        // Wait until DESTROYED...
        // Avoid problem where cleanup code stops again, but ends up wiating for
        // this initial stop!
        waitUntilEvent(AppProxy.DESTROYED, WAIT_TIME);
    }

    /**
     * Tests that permissions are not checked stop().
     */
    public void testStopPermissions_notLaunched()
    {
        doTestNoPermission(XTest.STOP);
        // Wait until DESTROYED...
        // Avoid problem where cleanup code stops again, but ends up wiating for
        // this initial stop!
        waitUntilEvent(AppProxy.DESTROYED, WAIT_TIME);
    }

    /**
     * Tests that permissions are not checked stop().
     */
    public void testStopPermissions_launched() throws Exception
    {
        doTestNoPermission_launched(XTest.STOP);
        // Wait until DESTROYED...
        // Avoid problem where cleanup code stops again, but ends up wiating for
        // this initial stop!
        waitUntilEvent(AppProxy.DESTROYED, WAIT_TIME);
    }

    /**
     * Tests that permissions are not checked resume().
     */
    public void testResumePermissions_otherLaunched() throws Exception
    {
        doTestPermission_otherLaunched(XTest.RESUME);
    }

    /**
     * Tests that permissions are not checked resume().
     */
    public void testResumePermissions_notLaunched()
    {
        doTestNoPermission(XTest.RESUME);
    }

    /**
     * Tests that permissions are not checked resume().
     */
    public void testResumePermissions_launched() throws Exception
    {
        doTestNoPermission_launched(XTest.RESUME);
    }

    private void waitUntilEvent(int type, long ms)
    {
        try
        {
            synchronized (listener)
            {
                listener.waitUntilEvent(type, ms);
            }
        }
        catch (InterruptedException e)
        { /* ignored */
        }
    }

    public static abstract class XTest
    {
        /** Execute the transition. */
        public abstract void doX(AppProxy app);

        public static final XTest START = new XTest()
        {
            public void doX(AppProxy app)
            {
                app.start();
            }
        };

        public static final XTest PAUSE = new XTest()
        {
            public void doX(AppProxy app)
            {
                app.pause();
            }
        };

        public static final XTest RESUME = new XTest()
        {
            public void doX(AppProxy app)
            {
                app.resume();
            }
        };

        public static final XTest STOP = new XTest()
        {
            public void doX(AppProxy app)
            {
                app.stop(true);
            }
        };

        public static final XTest STOP_FALSE = new XTest()
        {
            public void doX(AppProxy app)
            {
                app.stop(false);
            }
        };
    }

    /**
     * An implementation of AppStateChangeEventListener used to watch and wait
     * for specific state transitions.
     */
    public static class Listener implements AppStateChangeEventListener
    {
        public int expected;

        public Vector events = new Vector();

        public synchronized void stateChange(AppStateChangeEvent e)
        {
            if (DEBUG)
                System.out.println(System.currentTimeMillis() + " stateChange: " + e.getFromState() + "->"
                        + e.getToState() + ":" + (e.hasFailed() ? "failed" : "success"));
            events.addElement(e);
            notifyAll();
        }

        public void clear()
        {
            events.removeAllElements();
        }

        public AppStateChangeEvent pull()
        {
            try
            {
                synchronized (events)
                {
                    Object e = events.elementAt(0);
                    events.removeElementAt(0);
                    return (AppStateChangeEvent) e;
                }
            }
            catch (Exception e)
            {
                fail("Expected at least one event");
            }
            // Will never reach here!
            return null;
        }

        public AppStateChangeEvent waitNextEvent(long millis) throws InterruptedException
        {
            AppStateChangeEvent e = null;
            if (events.size() <= 0) waitEvent(millis);
            e = pull();
            if (DEBUG)
                System.out.println(System.currentTimeMillis() + " Event: " + e.getFromState() + "->" + e.getToState()
                        + ": " + e.hasFailed());
            return e;
        }

        public AppStateChangeEvent waitUntilEvent(int event, long millis) throws InterruptedException
        {
            AppStateChangeEvent e = null;
            do
            {
                if (events.size() <= 0) waitEvent(millis);
                e = pull();
                if (DEBUG)
                    System.out.println(System.currentTimeMillis() + " Event: " + e.getFromState() + "->"
                            + e.getToState() + ": " + e.hasFailed());
            }
            while (e.getToState() != event);
            return e;
        }

        private void waitEvent(long millis) throws InterruptedException
        {
            int size = events.size();

            if (DEBUG) System.out.println(System.currentTimeMillis() + " waiting " + millis);
            wait(millis);
            if (DEBUG) System.out.println(System.currentTimeMillis() + " waited " + millis);

            if (DEBUG && events.size() <= size)
                System.out.println("We have " + events.size() + " events, was " + size);
            assertTrue("New events were expected, current events " + events, events.size() > size);
        }
    }

    public static class DummySecurityManager extends NullSecurityManager
    {
        public Permission p;

        public Vector all = new Vector();

        public void checkPermission(Permission p)
        {
            if (this.p == null) this.p = p;
            all.addElement(p);
        }
    }

    /**
     * Extension of <code>ImplFactory</code> that adds methods for manipulating
     * the given <code>AppProxy</code> in an implementation-specific manner.
     * <p>
     * The {@link ImplFactory#createImplObject} method should return a valid
     * <code>AppProxy</code>.
     * 
     * @author Aaron Kamienski
     */
    public interface AppProxyImplFactory extends ImplFactory
    {
        /**
         * Unloads the given <code>AppProxy</code> and waits for that operation
         * to complete. This is performed (if it is even necessary) in an
         * implementation-dependent manner. This means that the applications is
         * made to be no longer active.
         * <p>
         * This is similar to the <code>DESTROYED</code> state, except that the
         * subsequent transition out of the transient <code>DESTROYED</code> to
         * <code>NOT_LOADED</code> state is waited for.
         * <p>
         * 
         * @param app
         *            the <code>AppProxy</code> to unload
         */
        public void makeUnloaded(AppProxy app) throws Exception;

        /**
         * Permanently destroys this application. The <code>DESTROYED</code>
         * state is not considered transient, and there is no subsequent
         * transition to the <code>NOT_LOADED</code> state.
         * <p>
         * This operation is potentially asynchronous.
         * 
         * @param app
         *            the <code>AppProxy</code> to destroy permanently
         */
        public void makeDestroyedPermanent(AppProxy app) throws Exception;
    }

    public static final boolean DEBUG = false;
}
