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

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

/**
 * Tests DVBJProxy interface.
 */
public class DVBJProxyTest extends AppProxyTest
{
    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new org.cablelabs.test.iftc.InterfaceTestSuite(DVBJProxyTest.class);
        suite.setName(DVBJProxy.class.getName());
        return suite;
    }

    public static InterfaceTestSuite isuite(String[] tests)
    {
        InterfaceTestSuite suite = new org.cablelabs.test.iftc.InterfaceTestSuite(DVBJProxyTest.class, tests);
        suite.setName(DVBJProxy.class.getName());
        return suite;
    }

    protected DVBJProxyTest(String name, Class implClass, ImplFactory f)
    {
        super(name, implClass, f);
        dvbjFactory = (DVBJProxyImplFactory) factory;
    }

    public DVBJProxyTest(String name, ImplFactory f)
    {
        this(name, DVBJProxy.class, f);
    }

    protected DVBJProxy createDVBJProxy()
    {
        return (DVBJProxy) createImplObject();
    }

    protected DVBJProxyImplFactory dvbjFactory;

    protected DVBJProxy dvbjapp;

    protected void setUp() throws Exception
    {
        super.setUp();
        dvbjapp = (DVBJProxy) app;
    }

    protected void tearDown() throws Exception
    {
        dvbjapp = null;
        super.tearDown();
    }

    protected String stateToString(int state)
    {
        switch (state)
        {
            case DVBJProxy.LOADED:
                return "LOADED";
            default:
                return super.stateToString(state);
        }
    }

    /**
     * Cause transition from NOT_LOADED to LOADED.
     */
    protected void doLoaded() throws Exception
    {
        Listener l = listener;

        if (app.getState() == DVBJProxy.LOADED) return;
        synchronized (l)
        {
            l.clear();
            dvbjapp.load();
            l.waitNextEvent(WAIT_TIME);
        }
        checkState(DVBJProxy.LOADED);
    }

    /**
     * Cause transition from NOT_LOADED to INITED (PAUSED).
     */
    protected void doInited() throws Exception
    {
        Listener l = listener;

        if (app.getState() == AppProxy.PAUSED) return;
        synchronized (l)
        {
            l.clear();
            dvbjapp.init();
            l.waitNextEvent(WAIT_TIME);
        }
        checkState(AppProxy.PAUSED);
    }

    /**
     * Overrides {@link AppProxyTest#doLaunched} to invoke {@link #doInited}.
     */
    protected void doLaunched() throws Exception
    {
        doInited();
    }

    /**
     * Causes transition from NOT_LOADED to LOADED-because-init-failed.
     */
    protected void doInitedFailed() throws Exception
    {
        Listener l = listener;
        AppStateChangeEvent e;

        dvbjFactory.makeInitFail(dvbjapp);

        if (app.getState() != DVBJProxy.LOADED && app.getState() != AppProxy.NOT_LOADED)
        {
            fail("Cannot transition to LOADED-because-init-failed from current state " + app.getState());
        }

        synchronized (l)
        {
            l.clear();
            dvbjapp.init();
            e = l.waitNextEvent(WAIT_TIME); // want to get here, but failed!!!
        }
        assertTrue("Internal Test Error - Expected transition to PAUSED state to fail!", e.hasFailed());
        checkState(DVBJProxy.LOADED);
    }

    protected void expectEvent(boolean failed, int fromState, int toState) throws Exception
    {
        AppStateChangeEvent e;
        synchronized (listener)
        {
            e = listener.waitNextEvent(WAIT_TIME);
        }
        checkEvent(e, failed, toState, fromState);
    }

    /**
     * Test a transition from the LOADED state.
     */
    public void doTest_fromLOADED(XTest test, boolean failed, int fromState, int toState) throws Exception
    {
        Listener l = listener;
        AppStateChangeEvent e;

        doLoaded();

        synchronized (l)
        {
            l.clear();
            test.doX(app);
            e = l.waitNextEvent(WAIT_TIME);
        }
        checkEvent(e, failed, toState, fromState);
    }

    /**
     * Test a transition from the INITED state.
     */
    public void doTest_fromINITED(XTest test, boolean failed, int fromState, int toState) throws Exception
    {
        Listener l = listener;
        AppStateChangeEvent e;

        doInited();

        synchronized (l)
        {
            l.clear();
            test.doX(app);
            e = l.waitNextEvent(WAIT_TIME);
        }
        checkEvent(e, failed, toState, fromState);
    }

    /**
     * Test a transition from the LOADED-becaus-init-failed state.
     */
    public void doTest_fromLOADED_BAD(XTest test, boolean failed, int fromState, int toState) throws Exception
    {
        Listener l = listener;
        AppStateChangeEvent e;

        doInitedFailed();

        synchronized (l)
        {
            l.clear();
            test.doX(app);
            e = l.waitNextEvent(WAIT_TIME);
        }
        checkEvent(e, failed, toState, fromState);

        // If operation failed, should still be in LOADED state
        if (failed) checkState(DVBJProxy.LOADED);
    }

    /**
     * Test a transition from the LOADED state.
     */
    public void doTest_fromLOADED(XTest test, boolean failed, int toState) throws Exception
    {
        doTest_fromLOADED(test, failed, DVBJProxy.LOADED, toState);
    }

    /**
     * Test a transition from the INITED state.
     */
    public void doTest_fromINITED(XTest test, boolean failed, int toState) throws Exception
    {
        doTest_fromINITED(test, failed, AppProxy.PAUSED, toState);
    }

    /**
     * Test a transition from the LOADED-because-init-failed state.
     */
    public void doTest_fromLOADED_BAD(XTest test, boolean failed, int toState) throws Exception
    {
        doTest_fromLOADED_BAD(test, failed, DVBJProxy.LOADED, toState);
    }

    /**
     * Test pause() from LOADED().
     */
    public void testPause_fromLOADED() throws Exception
    {
        doTest_fromLOADED(XTest.PAUSE, true, AppProxy.PAUSED);
    }

    /**
     * Test pause() from INITED (PAUSED).
     */
    public void testPause_fromINITED() throws Exception
    {
        doTest_fromINITED(XTest.PAUSE, true, AppProxy.PAUSED);
    }

    /**
     * Test pause() from LOADED-because-init-failed (LOADED).
     */
    public void testPause_fromLOADED_BAD() throws Exception
    {
        doTest_fromLOADED_BAD(XTest.PAUSE, true, AppProxy.PAUSED);
    }

    /**
     * Tests pause() from STARTED and pauseXlet throws Exception.
     */
    public void testPause_fromSTARTED_pauseFails() throws Exception
    {
        dvbjFactory.makePauseFail(dvbjapp);
        doTest_fromSTARTED(XTest.PAUSE, true, AppProxy.STARTED, AppProxy.PAUSED);
    }

    /**
     * Tests pause() from RESUMED and pauseXlet throws Exception.
     */
    public void testPause_fromRESUMED_pauseFails() throws Exception
    {
        dvbjFactory.makePause2Fail(dvbjapp);
        doTest_fromRESUMED(XTest.PAUSE, true, AppProxy.STARTED, AppProxy.PAUSED);
    }

    /**
     * If true, then expect transition to paused state to be followed by
     * pauseXlet() call. If false, then expect transition to paused state to
     * follow pauseXlet() call.
     */
    final boolean PAUSE_XLET_AFTER_PAUSED = false;

    /**
     * Tests pause() from STARTED and pauseXlet() self-destructs.
     * 
     * @throws Exception
     */
    public void testPause_fromSTARTED_pauseDestroys() throws Exception
    {
        dvbjFactory.makeDestroyInPause(dvbjapp);
        if (!PAUSE_XLET_AFTER_PAUSED)
        {
            doTest_fromSTARTED(XTest.PAUSE, false, AppProxy.STARTED, AppProxy.DESTROYED);
            expectEvent(true, AppProxy.DESTROYED, AppProxy.PAUSED);
        }
        else
        {
            doTest_fromSTARTED(XTest.PAUSE, false, AppProxy.STARTED, AppProxy.PAUSED);
            expectEvent(false, AppProxy.PAUSED, AppProxy.DESTROYED);
        }
    }

    /**
     * Tests pause() from RESUMED and pauseXlet() self-destructs.
     * 
     * @throws Exception
     */
    public void testPause_fromRESUMED_pauseDestroys() throws Exception
    {
        dvbjFactory.makeDestroyInPause2(dvbjapp);
        if (!PAUSE_XLET_AFTER_PAUSED)
        {
            doTest_fromRESUMED(XTest.PAUSE, false, AppProxy.STARTED, AppProxy.DESTROYED);
            expectEvent(true, AppProxy.DESTROYED, AppProxy.PAUSED);
        }
        else
        {
            doTest_fromRESUMED(XTest.PAUSE, false, AppProxy.STARTED, AppProxy.PAUSED);
            expectEvent(false, AppProxy.PAUSED, AppProxy.DESTROYED);
        }
    }

    /**
     * Test start() from LOADED().
     */
    public void testStart_fromLOADED() throws Exception
    {
        doTest_fromLOADED(XTest.START, false, DVBJProxy.LOADED, AppProxy.STARTED);
    }

    /**
     * Test start() from INITED (PAUSED)
     * 
     * @todo disabled per 4598.
     */
    public void testStart_fromINITED() throws Exception
    {
        doTest_fromINITED(XTest.START, false, AppProxy.STARTED);
    }

    /**
     * Test start() from LOADED-because-init-failed (LOADED).
     */
    public void testStart_fromLOADED_BAD() throws Exception
    {
        doTest_fromLOADED_BAD(XTest.START, true, AppProxy.STARTED);
    }

    /**
     * Tests start() from NOT_LOADED and load operation fails.
     */
    public void testStart_fromNOT_LOADED_loadFails() throws Exception
    {
        dvbjFactory.makeLoadFail(dvbjapp);
        doTest_fromNOT_LOADED(XTest.START, true, AppProxy.NOT_LOADED, DVBJProxy.LOADED);
    }

    /**
     * Tests start() from NOT_LOADED and initXlet fails.
     */
    public void testStart_fromNOT_LOADED_initFails() throws Exception
    {
        dvbjFactory.makeInitFail(dvbjapp);
        doTest_fromNOT_LOADED(XTest.START, true, DVBJProxy.LOADED, AppProxy.PAUSED);
    }

    /**
     * Tests start() from NOT_LOADED and startXlet fails.
     */
    public void testStart_fromNOT_LOADED_startFails() throws Exception
    {
        dvbjFactory.makeStartFail(dvbjapp);
        doTest_fromNOT_LOADED(XTest.START, true, AppProxy.PAUSED, AppProxy.STARTED);
    }

    /**
     * Tests start() from LOADED and initXlet fails.
     */
    public void testStart_fromLOADED_initFails() throws Exception
    {
        dvbjFactory.makeInitFail(dvbjapp);
        doTest_fromLOADED(XTest.START, true, DVBJProxy.LOADED, AppProxy.PAUSED);
    }

    /**
     * Tests start() from LOADED and startXlet fails.
     */
    public void testStart_fromLOADED_startFails() throws Exception
    {
        dvbjFactory.makeStartFail(dvbjapp);
        doTest_fromLOADED(XTest.START, true, AppProxy.PAUSED, AppProxy.STARTED);
        // TODO: wait for failed event
    }

    /**
     * Tests start() from INITED and startXlet fails.
     */
    public void testStart_fromINITED_startFails() throws Exception
    {
        dvbjFactory.makeStartFail(dvbjapp);
        doTest_fromINITED(XTest.START, true, AppProxy.PAUSED, AppProxy.STARTED);
    }

    /**
     * Tests start() from NOT_LOADED, initXlet() self-destructs.
     */
    public void testStart_fromNOT_LOADED_initDestroys() throws Exception
    {
        dvbjFactory.makeDestroyInInit(dvbjapp);
        doTest_fromNOT_LOADED(XTest.START, false, DVBJProxy.LOADED, AppProxy.DESTROYED);
        expectEvent(true, AppProxy.DESTROYED, AppProxy.PAUSED);
    }

    /**
     * Tests start() from LOADED, initXlet() self-destructs.
     */
    public void testStart_fromLOADED_initDestroys() throws Exception
    {
        dvbjFactory.makeDestroyInInit(dvbjapp);
        doTest_fromLOADED(XTest.START, false, DVBJProxy.LOADED, AppProxy.DESTROYED);
        expectEvent(true, AppProxy.DESTROYED, AppProxy.PAUSED);
    }

    /**
     * Tests start() from NOT_LOADED, startXlet() self-destructs.
     */
    public void testStart_fromNOT_LOADED_startDestroys() throws Exception
    {
        dvbjFactory.makeDestroyInStart(dvbjapp);
        doTest_fromNOT_LOADED(XTest.START, false, AppProxy.PAUSED, AppProxy.DESTROYED);
        expectEvent(true, AppProxy.DESTROYED, AppProxy.STARTED);
    }

    /**
     * Tests start() from LOADED, startXlet() self-destructs.
     */
    public void testStart_fromLOADED_startDestroys() throws Exception
    {
        dvbjFactory.makeDestroyInStart(dvbjapp);
        doTest_fromLOADED(XTest.START, false, AppProxy.PAUSED, AppProxy.DESTROYED);
        expectEvent(true, AppProxy.DESTROYED, AppProxy.STARTED);
    }

    /**
     * Tests start() from INITED, startXlet() self-destructs.
     */
    public void testStart_fromINITED_startDestroys() throws Exception
    {
        dvbjFactory.makeDestroyInStart(dvbjapp);
        doTest_fromINITED(XTest.START, false, AppProxy.PAUSED, AppProxy.DESTROYED);
        expectEvent(true, AppProxy.DESTROYED, AppProxy.STARTED);
    }

    /**
     * Test resume() from LOADED().
     */
    public void testResume_fromLOADED() throws Exception
    {
        doTest_fromLOADED(XTest.RESUME, true, AppProxy.STARTED);
    }

    /**
     * Test resume() from INITED (PAUSED).
     */
    public void testResume_fromINITED() throws Exception
    {
        doTest_fromINITED(XTest.RESUME, false, AppProxy.STARTED);
    }

    /**
     * Test resume() from LOADED-because-init-failed (LOADED).
     * 
     * @todo disabled per 4598
     */
    public void testResume_fromLOADED_BAD() throws Exception
    {
        doTest_fromLOADED_BAD(XTest.RESUME, true, AppProxy.STARTED);
    }

    /**
     * Tests resume() from PAUSED and startXlet fails.
     */
    public void testResume_fromPAUSED_resumeFails() throws Exception
    {
        dvbjFactory.makeResumeFail(dvbjapp);
        doTest_fromPAUSED(XTest.RESUME, true, AppProxy.PAUSED, AppProxy.STARTED);
    }

    /**
     * Tests resume() from LOADED, startXlet() self-destructs.
     */
    public void testResume_fromPAUSED_resumeDestroys() throws Exception
    {
        dvbjFactory.makeDestroyInResume(dvbjapp);
        doTest_fromPAUSED(XTest.RESUME, false, AppProxy.PAUSED, AppProxy.DESTROYED);
        expectEvent(true, AppProxy.DESTROYED, AppProxy.STARTED);
    }

    /**
     * Test stop() from LOADED().
     */
    public void testStop_fromLOADED() throws Exception
    {
        doTest_fromLOADED(XTest.STOP, false, AppProxy.DESTROYED);
    }

    /**
     * Test stop() from INITED (PAUSED).
     */
    public void testStop_fromINITED() throws Exception
    {
        doTest_fromINITED(XTest.STOP, false, AppProxy.DESTROYED);
    }

    /**
     * Test stop() from LOADED-because-init-failed (LOADED).
     */
    public void testStop_fromLOADED_BAD() throws Exception
    {
        doTest_fromLOADED_BAD(XTest.STOP, false, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from LOADED and destroyXlet fails.
     */
    public void testStop_fromLOADED_destroyFails() throws Exception
    {
        dvbjFactory.makeDestroyFail(dvbjapp);
        doTest_fromLOADED(XTest.STOP, false, DVBJProxy.LOADED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from INITED and destroyXlet fails.
     */
    public void testStop_fromINITED_destroyFails() throws Exception
    {
        dvbjFactory.makeDestroyFail(dvbjapp);
        doTest_fromINITED(XTest.STOP, false, AppProxy.PAUSED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from STARTED and destroyXlet fails.
     */
    public void testStop_fromSTARTED_destroyFails() throws Exception
    {
        dvbjFactory.makeDestroyFail(dvbjapp);
        doTest_fromSTARTED(XTest.STOP, false, AppProxy.STARTED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from PAUSED and destroyXlet fails.
     */
    public void testStop_fromPAUSED_destroyFails() throws Exception
    {
        dvbjFactory.makeDestroyFail(dvbjapp);
        doTest_fromPAUSED(XTest.STOP, false, AppProxy.PAUSED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from RESUMED and destroyXlet fails.
     */
    public void testStop_fromRESUMED_destroyFails() throws Exception
    {
        dvbjFactory.makeDestroyFail(dvbjapp);
        doTest_fromRESUMED(XTest.STOP, false, AppProxy.STARTED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from INITED and destroyXlet() self-destructs first.
     */
    public void testStop_fromINITED_destroyDestroyed() throws Exception
    {
        dvbjFactory.makeDestroyInDestroy(dvbjapp);
        doTest_fromINITED(XTest.STOP, false, AppProxy.PAUSED, AppProxy.DESTROYED);
        expectEvent(true, AppProxy.DESTROYED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from STARTED and destroyXlet() self-destructs first.
     */
    public void testStop_fromSTARTED_destroyDestroyed() throws Exception
    {
        dvbjFactory.makeDestroyInDestroy(dvbjapp);
        doTest_fromSTARTED(XTest.STOP, false, AppProxy.STARTED, AppProxy.DESTROYED);
        expectEvent(true, AppProxy.DESTROYED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from PAUSED and destroyXlet() self-destructs first.
     */
    public void testStop_fromPAUSED_destroyDestroyed() throws Exception
    {
        dvbjFactory.makeDestroyInDestroy(dvbjapp);
        doTest_fromPAUSED(XTest.STOP, false, AppProxy.PAUSED, AppProxy.DESTROYED);
        expectEvent(true, AppProxy.DESTROYED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from RESUMED and destroyXlet() self-destructs first.
     */
    public void testStop_fromRESUMED_destroyDestroyed() throws Exception
    {
        dvbjFactory.makeDestroyInDestroy(dvbjapp);
        doTest_fromRESUMED(XTest.STOP, false, AppProxy.STARTED, AppProxy.DESTROYED);
        expectEvent(true, AppProxy.DESTROYED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from LOADED and destroyXlet fails.
     */
    public void testStopFalse_fromLOADED_destroyFails() throws Exception
    {
        dvbjFactory.makeDestroyFail(dvbjapp);
        doTest_fromLOADED(XTest.STOP_FALSE, true, DVBJProxy.LOADED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from INITED and destroyXlet fails.
     */
    public void testStopFalse_fromINITED_destroyFails() throws Exception
    {
        dvbjFactory.makeDestroyFail(dvbjapp);
        doTest_fromINITED(XTest.STOP_FALSE, true, AppProxy.PAUSED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from STARTED and destroyXlet fails.
     */
    public void testStopFalse_fromSTARTED_destroyFails() throws Exception
    {
        dvbjFactory.makeDestroyFail(dvbjapp);
        doTest_fromSTARTED(XTest.STOP_FALSE, true, AppProxy.STARTED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from PAUSED and destroyXlet fails.
     */
    public void testStopFalse_fromPAUSED_destroyFails() throws Exception
    {
        dvbjFactory.makeDestroyFail(dvbjapp);
        doTest_fromPAUSED(XTest.STOP_FALSE, true, AppProxy.PAUSED, AppProxy.DESTROYED);
    }

    /**
     * Tests stop() from RESUMED and destroyXlet fails.
     */
    public void testStopFalse_fromRESUMED_destroyFails() throws Exception
    {
        dvbjFactory.makeDestroyFail(dvbjapp);
        doTest_fromRESUMED(XTest.STOP_FALSE, true, AppProxy.STARTED, AppProxy.DESTROYED);
    }

    /*
     * Tests load(). <ul> <li> Provides a hint to preload at least the initial
     * class of the application into local storage, resources permitting. <li>
     * This does not require loading of classes into the virtual machine or
     * creation of a new logical virtual machine which are implications of the
     * init method. <li> This method is asynchronous and its completion will be
     * notified by an AppStateChangedEvent. <li> In case of failure, the
     * hasFailed method of the AppStateChangedEvent will return true. <li> Calls
     * to this method shall only succeed if the application is in the NOT_LOADED
     * state. <li> In all cases, an AppStateChangeEvent will be sent, whether
     * the call was successful or not. </ul>
     */

    /**
     * Test load() from NOT_LOADED.
     */
    public void testLoad_fromNOT_LOADED() throws Exception
    {
        doTest_fromNOT_LOADED(XTest2.LOAD, false, DVBJProxy.LOADED);
    }

    /**
     * Test load() from LOADED.
     */
    public void testLoad_fromLOADED() throws Exception
    {
        doTest_fromLOADED(XTest2.LOAD, true, DVBJProxy.LOADED);
    }

    /**
     * Test load() from LOADED-because-init-failed.
     */
    public void testLoad_fromLOADED_BAD() throws Exception
    {
        doTest_fromLOADED_BAD(XTest2.LOAD, true, DVBJProxy.LOADED);
    }

    /**
     * Test load() from INITED (PAUSED).
     */
    public void testLoad_fromINITED() throws Exception
    {
        doTest_fromINITED(XTest2.LOAD, true, DVBJProxy.LOADED);
    }

    /**
     * Test load() from PAUSED.
     */
    public void testLoad_fromPAUSED() throws Exception
    {
        doTest_fromPAUSED(XTest2.LOAD, true, DVBJProxy.LOADED);
    }

    /**
     * Test load() from STARTED.
     */
    public void testLoad_fromSTARTED() throws Exception
    {
        doTest_fromSTARTED(XTest2.LOAD, true, DVBJProxy.LOADED);
    }

    /**
     * Test load() from RESUMED.
     */
    public void testLoad_fromRESUMED() throws Exception
    {
        doTest_fromRESUMED(XTest2.LOAD, true, DVBJProxy.LOADED);
    }

    /**
     * Test load() from DESTROYED.
     */
    public void testLoad_fromDESTROYED() throws Exception
    {
        doTest_fromDESTROYED(XTest2.LOAD, true, DVBJProxy.LOADED);
    }

    /**
     * Tests load() from NOT_LOADED and load op fails.
     */
    public void testLoad_fromNOT_LOADED_loadFails() throws Exception
    {
        dvbjFactory.makeLoadFail(dvbjapp);
        doTest_fromNOT_LOADED(XTest2.LOAD, true, AppProxy.NOT_LOADED, DVBJProxy.LOADED);
    }

    /*
     * Tests init(). <ul> <li> Requests the application manager calls the
     * initXlet method on the application. <li> This method is asynchronous and
     * its completion will be notified by an AppStateChangedEvent. <li> In case
     * of failure, the hasFailed method of the AppStateChangedEvent will return
     * true. <li> Calls to this method shall only succeed if the application is
     * in the NOT_LOADED or LOADED states. <li> If the application is in the
     * NOT_LOADED state, the application will move through the LOADED state into
     * the PAUSED state before calls to this method complete. <li> In all cases,
     * an AppStateChangeEvent will be sent, whether the call was successful or
     * not. </ul>
     */

    /**
     * Test init() from NOT_LOADED.
     */
    public void testInit_fromNOT_LOADED() throws Exception
    {
        doTest_fromNOT_LOADED(XTest2.INIT, false, AppProxy.NOT_LOADED, AppProxy.PAUSED);
    }

    /**
     * Test init() from LOADED.
     * 
     * @todo disabled per 4598
     */
    public void testInit_fromLOADED() throws Exception
    {
        doTest_fromLOADED(XTest2.INIT, false, AppProxy.PAUSED);
    }

    /**
     * Test init() from LOADED-because-init-failed.
     */
    public void testInit_fromLOADED_BAD() throws Exception
    {
        doTest_fromLOADED_BAD(XTest2.INIT, true, AppProxy.PAUSED);
    }

    /**
     * Test init() from INITED (PAUSED).
     */
    public void testInit_fromINITED() throws Exception
    {
        doTest_fromINITED(XTest2.INIT, true, AppProxy.PAUSED);
    }

    /**
     * Test init() from PAUSED.
     */
    public void testInit_fromPAUSED() throws Exception
    {
        doTest_fromPAUSED(XTest2.INIT, true, AppProxy.PAUSED);
    }

    /**
     * Test init() from STARTED.
     */
    public void testInit_fromSTARTED() throws Exception
    {
        doTest_fromSTARTED(XTest2.INIT, true, AppProxy.PAUSED);
    }

    /**
     * Test init() from RESUMED.
     */
    public void testInit_fromRESUMED() throws Exception
    {
        doTest_fromRESUMED(XTest2.INIT, true, AppProxy.PAUSED);
    }

    /**
     * Test init() from DESTROYED.
     */
    public void testInit_fromDESTROYED() throws Exception
    {
        doTest_fromDESTROYED(XTest2.INIT, true, AppProxy.PAUSED);
    }

    /**
     * Tests init() from NOT_LOADED and load op fails.
     */
    public void testInit_fromNOT_LOADED_loadFails() throws Exception
    {
        dvbjFactory.makeLoadFail(dvbjapp);
        doTest_fromNOT_LOADED(XTest2.INIT, true, AppProxy.NOT_LOADED, DVBJProxy.LOADED);
    }

    /**
     * Tests init() from NOT_LOADED and initXlet fails.
     */
    public void testInit_fromNOT_LOADED_initFails() throws Exception
    {
        dvbjFactory.makeInitFail(dvbjapp);
        doTest_fromNOT_LOADED(XTest2.INIT, true, DVBJProxy.LOADED, AppProxy.PAUSED);
    }

    /**
     * Tests init() from LOADED and initXlet fails.
     */
    public void testInit_fromLOADED_initFails() throws Exception
    {
        dvbjFactory.makeInitFail(dvbjapp);
        doTest_fromLOADED(XTest2.INIT, true, DVBJProxy.LOADED, AppProxy.PAUSED);
    }

    /**
     * Tests that permissions are checked init().
     */
    public void testInitPermissions_otherLaunched() throws Exception
    {
        doTestPermission_otherLaunched(XTest2.INIT);
    }

    /**
     * Tests that permissions are not checked init().
     */
    public void testInitPermissions_notLaunched()
    {
        doTestNoPermission(XTest2.INIT);
    }

    /**
     * Tests that permissions are not checked init().
     */
    public void testInitPermissions_launched() throws Exception
    {
        doTestNoPermission_launched(XTest2.INIT);
    }

    /**
     * Tests that permissions are checked init().
     */
    public void testLoadPermissions_otherLaunched() throws Exception
    {
        doTestPermission_otherLaunched(XTest2.LOAD);
    }

    /**
     * Tests that permissions are not checked init().
     */
    public void testLoadPermissions_notLaunched()
    {
        doTestNoPermission(XTest2.LOAD);
    }

    /**
     * Tests that permissions are not checked init().
     */
    public void testLoadPermissions_launched() throws Exception
    {
        doTestNoPermission_launched(XTest2.LOAD);
    }

    protected static abstract class XTest2 extends AppProxyTest.XTest
    {
        public static final XTest LOAD = new XTest()
        {
            public void doX(AppProxy app)
            {
                ((DVBJProxy) app).load();
            }
        };

        public static final XTest INIT = new XTest()
        {
            public void doX(AppProxy app)
            {
                ((DVBJProxy) app).init();
            }
        };
    }

    /**
     * An extension of {@link AppProxyImplFactory} that adds a methods for
     * causing <code>DVBJProxy</code> state change requests to fail.
     * 
     * @author Aaron Kamienski
     */
    public static interface DVBJProxyImplFactory extends AppProxyImplFactory
    {
        /**
         * Causes initialization via {@link DVBJProxy#init()} to fail. This is
         * to get the DVBJProxy into an {@link DVBJProxy#LOADED} state what can
         * only be exited by moving to the {@link AppProxy#DESTROYED} state.
         * 
         * @see Xlet#initXlet
         */
        public void makeInitFail(DVBJProxy app) throws Exception;

        /**
         * Causes loading via {@link DVBJProxy#load()} to fail.
         */
        public void makeLoadFail(DVBJProxy app) throws Exception;

        /**
         * Causes launching via {@link DVBJProxy#start()} to fail.
         * 
         * @see Xlet#startXlet
         */
        public void makeStartFail(DVBJProxy app) throws Exception;

        /**
         * Causes {Xlet#pauseXlet} to throw an uncaught exception.
         */
        public void makePauseFail(DVBJProxy app) throws Exception;

        /**
         * Causes {Xlet#pauseXlet} to throw an uncaught exception.
         */
        public void makePause2Fail(DVBJProxy app) throws Exception;

        /**
         * Causes resumption via {@link DVBJProxy#resume()} to fail.
         * 
         * @see Xlet#startXlet
         */
        public void makeResumeFail(DVBJProxy app) throws Exception;

        /**
         * Causes destruction via {@link DVBJProxy#stop(boolean) stop(true)} to
         * fail.
         * 
         * @see Xlet#destroyXlet
         */
        public void makeDestroyFail(DVBJProxy app) throws Exception;

        /**
         * Causes proxied app to destroy itself during init.
         * 
         * @see XletContext#notifyDestroyed
         * @see Xlet#initXlet
         */
        public void makeDestroyInInit(DVBJProxy app) throws Exception;

        /**
         * Causes proxied app to destroy itself during start.
         * 
         * @see XletContext#notifyDestroyed
         * @see Xlet#startXlet
         */
        public void makeDestroyInStart(DVBJProxy app) throws Exception;

        /**
         * Causes proxied app to destroy itself during resume.
         * 
         * @see XletContext#notifyDestroyed
         * @see Xlet#startXlet
         */
        public void makeDestroyInResume(DVBJProxy app) throws Exception;

        /**
         * Causes proxied app to destroy itself during pause.
         * 
         * @see XletContext#notifyDestroyed
         * @see Xlet#pauseXlet
         */
        public void makeDestroyInPause(DVBJProxy app) throws Exception;

        /**
         * Causes proxied app to destroy itself during (2nd) pause.
         * 
         * @see XletContext#notifyDestroyed
         * @see Xlet#pauseXlet
         */
        public void makeDestroyInPause2(DVBJProxy app) throws Exception;

        /**
         * Causes proxied app to destroy itself during destroy.
         * 
         * @see XletContext#notifyDestroyed
         * @see Xlet#destroyXlet
         */
        public void makeDestroyInDestroy(DVBJProxy app) throws Exception;
    }
}
