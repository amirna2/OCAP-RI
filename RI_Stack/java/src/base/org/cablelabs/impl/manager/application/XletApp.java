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

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.RefTracker;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.DVBJProxy;

/**
 * A concrete implementation of the Application class which implements the
 * DVB-MHP DVBJProxy interface. An <code>XletApp</code> also implements the
 * <code>CallerContext</code> interface. An instance of an XletApplication can
 * be used to control (e.g., launch and destroy) an xlet application.
 * 
 *<p>
 * The <i>handle</i> methods defined by the {@link Application superclass}
 * (e.g., {@link #handleStart}) as well as new methods (e.g.,
 * {@link #handleLoad}) defined manage the application's state transitions. This
 * means performing whatever operations are necessary when transitioning between
 * states. This includes loading the <code>Xlet</code> and calling its methods
 * to initialize, start, pause, and destroy the application.
 * <p>
 * Transitions are handled between:
 * <ol>
 * <li>NOT_LOADED
 * <li>LOADED
 * <li>PAUSED
 * <li>STARTED
 * <li>DESTROYED
 * </ol>
 * Following is a simple diagram for the state tranisitions as per the DVB-J
 * specification.
 * 
 * <pre>
 *           +------------+   stop    +-----------+
 * START---->| NOT_LOADED |---------->| DESTROYED |----->END
 *           +------------+           +-----------+
 *             |                          ^
 *        load |                          | stop
 *             |      +-------------------+-------------------+
 *             v      |                   |                   |
 *           +----------+   init     +----------+  start  +---------+
 *           |  LOADED  |----------->|  PAUSED  |-------->| STARTED |
 *           +----------+            +----------+  pause  +---------+
 *                                               <-------/
 * </pre>
 * <p>
 * Following is a simple diagram for the state transitions used by this
 * implementation (with the addition of the <code>PARTIALLY_LOADED</code>,
 * <code>INITED</code>, and <code>RESUMED</code> states).
 * 
 * <pre>
 *           +------------+   stop    +-----------+
 * START---->| NOT_LOADED |---------->| DESTROYED |----->END
 *           +--/--|------+           +^----^----^+<---------+
 *   autostart /   | load             /     |     \           \
 *            /    |            stop /      |      \ stop      \
 *  +-------v---+  |        +-------/--+  resume  +-\------+    \
 *  |   AUTO    |  |        | RESUMED  |<---------| PAUSED |     |
 *  | PARTIALLY |  |        +-----^----+  pause   +------^-+     |
 *  |   LOADED  |  |               \    \---------^       \      |
 *  +------ \---+  |         resume \       |              \     | stop
 *  finish   \     |                 \      | stop    pause \    |
 * autostart  v    |                  \     |                \   |
 *           +-----v----+   init     +-\----|---+  start  +---\--|--+
 *           |  LOADED  |----------->|  INITED  |-------->| STARTED |
 *           +----------+            +----------+         +---------+
 * </pre>
 * <p>
 * There are also {@link #EXT_AUTO_STATE AUTOSTART} versions of the
 * {@link #LOADED}, {@link #INITED}, and {@link #STARTED} states. These
 * represent transitions due to the auto-starting of applications due to service
 * selection.
 * <p>
 * There is also support for moving from <code>DESTROYED</code> to
 * <code>NOT_LOADED</code> state. This fulfills the OCAP requirement that
 * <code>DESTROYED</code> state is transient.
 * <p>
 * If this proxy has been <i>disposed</i>, then all state transitions (except to
 * <code>DESTROYED</code>) will fail. A Proxy is destroyed when it's connection
 * to the containing <code>AppDomain</code> is no longer valid (e.g., it's been
 * removed from the applications database).
 * 
 * @author Aaron Kamienski
 */
public class XletApp extends Application implements DVBJProxy
{
    /**
     * The <i>inited</i> state represents the <code>PAUSED</code> state
     * transitioned to from the <code>NOT_LOADED</code> or <code>LOADED</code>
     * states.
     * 
     * @see #STATE_MASK
     * @see #PAUSED
     */
    public static final int INITED = PAUSED | EXT_STATE;

    /**
     * An implementation-specific, non-public, failure state. This state is
     * entered following a failed transition to the {@link #INITED} state from
     * the {@link #LOADED} state. {@link AppProxy#getState() Externally}, this
     * state appears to be the <code>LOADED</code> state.
     * <p>
     * Only the {@link #DESTROYED} state is reachable from this state.
     */
    public static final int INIT_FAILED = LOADED | EXT_STATE;

    /**
     * An implementation-specific, non-public, intermediate state. This state is
     * the mid-point between the {@link #NOT_LOADED} and {@link #LOADED} states
     * while an application is in the process of being
     * {@link Application#AUTO_STARTED auto-started}. The
     * <code>AUTO_PARTIALLY_LOADED</code> state may only be left either as part
     * of completing the transition to the <code>LOADED</code> state or as part
     * of application destruction (and a transition to the
     * {@link AppProxy#DESTROYED destroyed} state.
     */
    public static final int AUTO_PARTIALLY_LOADED = NOT_LOADED | EXT_STATE | EXT_AUTO_STATE;

    /**
     * Instantiates a new <i>Xlet Application</code> as specified by the given
     * <code>AppID</code> to be executed within the given <code>AppDomain</code>
     * .
     * 
     * @param entry
     *            the <code>AppEntry</code> specifying the application
     * @param domain
     *            the <code>AppDomain</code> within which this application will
     *            execute
     * @param version
     *            the application version
     */
    XletApp(AppEntry entry, AppDomainImpl domain)
    {
        super(entry, domain, NOT_LOADED);
    }

    /**
     * Overrides <code>Object.toString()</code>.
     * 
     * @return string representation of this object
     */
    public String toString()
    {
        return "XletApp@" + System.identityHashCode(this) + "[" + entry.id + "]";
    }

    /** Implements the DVBJProxy.load() method. */
    public void load()
    {
        // State change to LOADED
        tq.post(new StateChange(LOADED, null, AppManager.getCaller())
        {
            public void run()
            {
                handleLoad(requestor);
            }
        });
    }

    /** Implements the DVBJProxy.init() method. */
    public void init()
    {
        // State change to INITED
        tq.post(new StateChange(INITED, null, AppManager.getCaller())
        {
            public void run()
            {
                handleInit(requestor);
            }
        });
    }

    /**
     * Invoked once the application has successfully been cleaned up, allowing
     * it to move back to the <code>NOT_LOADED</code> from the
     * <code>DESTROYED</code> state.
     * <p>
     * This is invoked once the <code>AppContext</code> previously associated
     * with this <code>XletApp</code> has been destroyed.
     * <p>
     * This should <i>only</i> be invoked by the implementation (specifically
     * the <code>AppContext</code>) as part of finalizing clean up. It should
     * only succeed when the application is in the <code>DESTROYED</code> state.
     */
    void unload()
    {
        // State change to NOT_LOADED
        // If FORGET_IN_DESTROY (no real work done in doUnload)
        // then don't bother attempting to move to NOT_LOADED state if disposed
        if (unloadEnabled && (!FORGET_IN_DESTROY || !disposed))
        {
            //no need to use xlet threadqueue to call handleUnload (no notification of the transition to NOT_LOADED)
            //run in SystemContext (sync)
            CallerContextManager callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            callerContextManager.getSystemContext().runInContextAsync(new StateChange(NOT_LOADED)
            {
                public void run()
                {
                    handleUnload();
                }
            });
        }
    }

    /**
     * This method can be used to disable (and subsequently enable) the use of
     * the {@link #unload} method. Invoking <code>setUnloadEnabled(false)</code>
     * will cause any subsequent invocation of <code>unload()</code> to fail
     * silently (w/out even generating an event). Invoking
     * <code>setUnloadEnabled(true)</code> will re-enable <code>unload</code> to
     * function as normal.
     * <p>
     * This method is present for testing purposes only.
     * 
     * @param enabled
     *            if <code>true</code> then <code>unload()</code> is enabled; if
     *            <code>false</code> then <code>unload()</code> is disabled
     */
    void setUnloadEnabled(boolean enabled)
    {
        unloadEnabled = enabled;
    }

    /**
     * Returns <code>true</code> if this app has been launched and it's been
     * launched by an app other than the caller.
     * 
     * @return whether this app has been launched by an app other than the
     *         caller
     * @see AppContext#isOwner
     */
    protected boolean isAppsControlPermissionRequired()
    {
        // If this app has already been disposed, there is no harm is letting
        // any other app try to call a state change function. They will all fail
        // anyway
        if (disposed) return false;

        AppContext context = this.appContext;
        if (context == null) return true;

        return !context.isOwner(AppManager.getCaller());
    }

    /**
     * Implements {@link Application#handlePause()}.
     * 
     * @see #doPause
     */
    protected void handlePause()
    {
        int startState = state.getPrivateState();
        int destState = PAUSED;

        if (log.isDebugEnabled())
        {
            log.debug("handlePause -- " + startState + "->" + destState + "), disposed=" + disposed + " - " + this);
        }

        // Check for invalid app states
        if (disposed ||
            (startState != STARTED && startState != RESUMED && startState != AUTO_STARTED) ||
            xlet == null)
        {
            state.setState(PAUSED, startState, true, true);
        }
        else
        {
            doPause(startState);
        }
    }

    /**
     * Implements {@link Application#handleResume()}.
     * 
     * @see #doResume
     */
    protected void handleResume()
    {
        int startState = state.getPrivateState();
        int destState = RESUMED;

        if (log.isDebugEnabled())
        {
            log.debug("handleResume -- " + startState + "->" + destState + "), disposed=" + disposed + " - " + this);
        }

        // Check for invalid app states
        if (disposed || ((startState & STATE_MASK) != PAUSED) || xlet == null)
        {
            state.setState(RESUMED, startState, true, true);
        }
        else
        {
            doResume(startState);
        }
    }

    /**
     * Implements {@link Application#handleStart}.
     * <p>
     * Transitions to the {@link AppProxy#STARTED} state from
     * {AppProxy#NOT_LOADED}, {DVBJProxy#LOADED}, and {AppProxy#PAUSED} (if
     * never started).
     * 
     * @see #doCompoundInit(CallerContext, boolean, Object)
     * @see #doStart
     */
    protected void handleStart(String[] args, CallerContext requestor)
    {
        int startState = state.getPrivateState();
        int prevState = startState;

        if (log.isDebugEnabled())
        {
            log.debug("handleStart -- " + startState + " previous state: " + prevState + "), disposed=" + disposed + " - " + this);
        }
        switch (startState)
        {
            case NOT_LOADED:
            case LOADED:
                // Transition through LOADED to INITED, w/out an event upon
                // success
                if (doCompoundInit(requestor, false, args)) break;
                prevState = state.getPrivateState();
                // FALLTHROUGH
            case INITED:
                doStart(args, startState, prevState);
                break;
            default:
                state.setState(STARTED, startState, true, true);
                break;
        }
    }

    /**
     * Implements {@link Application#handleStop(boolean)}.
     * 
     * @see #doDestroy(Object)
     * @see #doUnload()
     */
    protected void handleStop(boolean unconditional)
    {
        int oldState = state.getPrivateState();
        int destState = DESTROYED;

        if (log.isDebugEnabled())
        {
            log.debug("handleStop -- " + oldState + "->" + destState + "), disposed=" + disposed + " - " + this);
        }

        // only destroy if we aren't in destroyed
        boolean failed = false;
        if (oldState != DESTROYED)
        {
            failed = doDestroy(unconditional ? Boolean.TRUE : Boolean.FALSE, oldState);
            if (log.isDebugEnabled())
            {
                log.debug("handleStop - doDestroy - failed: " + failed);
            }
        }

        // DESTROYED state is transient -- move directly to NOT_LOADED.  But only if
        // we successfully destroyed, otherwise we must stay in the same state
        // MHP1.0.3 9.2.4.1
        oldState = state.getState();
        if (oldState != NOT_LOADED && !failed)
        {
            state.setState(NOT_LOADED, oldState, doUnload(), false);
        }
    }

    /**
     * Implements the handling of <i>load</i> state change request.
     * <p>
     * Transitions to the {@link DVBJProxy#LOADED} state from
     * {@link AppProxy#NOT_LOADED}. All other transition requests result in
     * failure.
     * <p>
     * Should be executed on the private {@link #tq TaskQueue}.
     * 
     * @param requestor
     *            the application requesting that the app be loaded
     * 
     * @see #load
     * @see #doCompoundLoad(CallerContext, boolean)
     */
    protected void handleLoad(CallerContext requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug("handleLoad - " + this);
        }
        // Load and generate an event upon success
        doCompoundLoad(requestor, true);
    }

    /**
     * Implements the handling of <i>init</i> state change request.
     * <p>
     * Transitions to the {@link AppProxy#PAUSED} state from
     * {@link AppProxy#NOT_LOADED} or {@link DVBJProxy#LOADED}. All other
     * transition request result in failure.
     * <p>
     * Should be executed on the private {@link #tq TaskQueue}.
     * 
     * @param requestor
     *            the application requesting that the app be initialized
     * 
     * @see #init
     * @see #doCompoundInit(CallerContext, boolean, Object)
     */
    protected void handleInit(CallerContext requestor)
    {
        if (log.isDebugEnabled())
        {
            log.debug("handleInit - " + this);
        }
        // Transition through LOADED to INITED and generate event upon success
        doCompoundInit(requestor, true, null);
    }

    /**
     * Implements {@link Application#handleAutoStart}.
     * <p>
     * Manages this application through the following state transitions:
     * <table border>
     * <tr>
     * <th>from</th>
     * <th>to</th>
     * <th>action</th>
     * </tr>
     * <tr>
     * <td>{@link AppProxy#NOT_LOADED NOT_LOADED}</td>
     * <td>{@link XletApp#AUTO_PARTIALLY_LOADED ~AUTO_PARTIALLY_LOADED}</td>
     * <td>{@link #doCreateContext create} {@link AppContext}, and kick-off
     * asynchronous {@link AppContext#autoLoad download}</td>
     * </tr>
     * <tr>
     * <td><code>~AUTO_PARTIALLY_LOADED</code></td>
     * <td>{@link DVBJProxy#LOADED LOADED}</td>
     * <td>Start {@link AppContext#load load process} and
     * {@link AppContext#create() create} instance of <code>Xlet</code>; This
     * happens asynchronously if app is to be downloaded</td>
     * </tr>
     * <tr>
     * <td><code>LOADED</code></td>
     * <td>{@link XletApp#INITED ~INITED}</td>
     * <td>Invokes {@link Xlet#initXlet(XletAppContext)}</td>
     * </tr>
     * <tr>
     * <td><code>~INITED</code></td>
     * <td>{@link AppProxy#STARTED STARTED}</td>
     * <td>Invokes {@link Xlet#startXlet()}</td>
     * </tr>
     * </table>
     * 
     * @see #finishAutoLoad(boolean)
     */
    protected void handleAutoStart(CallerContext requestor)
    {
        int startState = state.getPrivateState();
        int destState = AUTO_STARTED;

        if (log.isDebugEnabled())
        {
            log.debug("handleAutoStart -- " + startState + "->" + destState + "), disposed=" + disposed + " - " + this);
        }

        // Catch invalid transition up-front
        if (disposed || startState != NOT_LOADED)
            state.setState(AUTO_STARTED, startState, true, true); // failure
        // Create AppContext
        else if (doCreateContext(requestor))
            state.setState(LOADED, startState, true, true); // failure
        else
        {
            // Intermediate state between NOT_LOADED and STARTED used or
            // auto-start only
            state.setState(AUTO_PARTIALLY_LOADED, startState, false, false); // success,
                                                                             // no
                                                                             // event

            // Kick off auto-load.
            if (appContext.autoLoad())
            {
                // If cannot auto-load, then continue on synchronously

                int prevState = state.getState();
                if (doLoad(prevState, true))
                {
                    state.setState(LOADED, prevState, true, true);
                    handleStop(true);
                }
                // Successful load
                else 
                {
                    // Finish with start
                    handleStart(null, null);
                }
            }
            // else, do nothing else... rest is handled async!
        }
    }

    /**
     * Implements the handling of <i>unload</i> state change request.
     * <p>
     * Transitions to the {@link AppProxy#NOT_LOADED} state from
     * {@link AppProxy#DESTROYED}. All other transition request result in
     * failure.
     * <p>
     * Should be executed on the private {@link #tq TaskQueue}.
     * 
     * @see #unload
     * @see #doUnload()
     */
    protected void handleUnload()
    {
        int oldState = state.getPrivateState();
        int destState = NOT_LOADED;
        if (log.isDebugEnabled())
        {
            log.debug("handleUnload -- " + oldState + "->" + destState + "), disposed=" + disposed + " - " + this);
        }

        if (oldState != NOT_LOADED)
        {
            boolean invalid = disposed || oldState != DESTROYED;
    
            state.setState(NOT_LOADED, oldState, invalid || doUnload(), false);
        }
    }

    /**
     * Implements transition to {@link DVBJProxy#LOADED LOADED} state from
     * {@link DVBJProxy#NOT_LOADED NOT_LOADED}. If the transition to
     * <code>LOADED</code> state fails due to outside issues (e.g., an uncaught
     * exception is thrown from the <code>Xlet</code> constructor), then an
     * implicit transition to {@link AppProxy#DESTROYED} is taken.
     * 
     * @param requestor
     *            the application requesting that the app be initialized
     * @param genEvent
     *            <code>true</code> if an event should be generated upon success
     * @return <code>true</code> if the state transition failed;
     *         <code>false</code> otherwise
     * 
     * @see #doCreateContext
     * @see #doLoad()
     * @see #handleStop
     */
    protected boolean doCompoundLoad(CallerContext requestor, boolean genEvent)
    {
        int startState = state.getPrivateState();

        // Verify start state
        // Create context
        if (disposed || startState != NOT_LOADED || doCreateContext(requestor))
        {
            return state.setState(LOADED, startState, true, true);
        }
        // Load Xlet
        else if (doLoad(startState,genEvent))
        {
            state.setState(LOADED, startState, true, true);
            // Clean-up if couldn't move out of NOT_LOADED
            handleStop(true);
            return true;
        }
        
        return false;
    }

    /**
     * Implements transition to {@link XletApp#INITED ~INITED} state from
     * {@link DVBJProxy#NOT_LOADED NOT_LOADED} or {@link DVBJProxy#LOADED}.
     * 
     * @param requestor
     *            the application requesting that the app be initialized
     * @param genEvent
     *            <code>true</code> if an event should be generated upon success
     * @return <code>true</code> if the state transition failed;
     *         <code>false</code> otherwise
     * 
     * @see #doCompoundLoad
     * @see #doInit
     */
    protected boolean doCompoundInit(CallerContext requestor, boolean genEvent, Object payload)
    {
        int startState = state.getPrivateState();
        int prevState = startState;

        switch (startState)
        {
            case NOT_LOADED:
                if (doCompoundLoad(requestor, false)) return true;
                prevState = state.getState();
                // FALLTHROUGH
            case LOADED:
                return doInit(payload, startState, prevState, genEvent);

            default:
                return state.setState(INITED, startState, true, true);
        }
    }

    /**
     * The second half of {@link #handleAutoStart} should be invoked by the
     * {@link AppContext} after application download has completed (successfully
     * or not). After successful download, this will complete the transition to
     * the {@link DVBJProxy#LOADED} state and subsequent transitions through to
     * {@link DVBJProxy#STARTED}.
     * 
     * @param failed
     *            <code>true</code> if app download failed
     * 
     * @see #handleAutoStart
     * @see #doLoad
     * @see #handleStart
     */
    void finishAutoLoad(boolean failed)
    {
        // Handle transition from AUTO_PARTIALLY_LOADED to LOADED

        // A failed AUTOSTART -> goes to destroyed
        if (failed)
            tq.post(new StateChange(DESTROYED, Boolean.TRUE, null));
        else
        {
            tq.post(new Runnable()
            {
                public void run()
                {
                    // Should be AUTO_PARTIALLY_LOADED
                    int oldState = state.getPrivateState();
                    if (oldState != AUTO_PARTIALLY_LOADED)
                        state.setState(LOADED, oldState, true, true);
                    else if (doLoad(oldState, true))
                    {
                        state.setState(LOADED, oldState, true, true);
                        handleStop(true);
                    }
                    // Successful load
                    else
                    {
                        // Finish with start
                        handleStart(null, null);
                    }
                }
            });
        }
    }

    /**
     * Takes care of creating the application context as part of the transition
     * to the <code>LOADED</code> state (or <code>AUTO_PARTIALLY_LOADED</code>
     * in the case of an <i>autostart</i>).
     * <p>
     * Responsible for the creation of the application's
     * <code>CallerContext</code> (an instance of <code>AppContext</code>). Will
     * also perform the necessary checks to see if launching of this application
     * is even allowed (i.e., if an application instance with the same
     * <code>AppID</code> isn't already running and it is allowed by a
     * monitor-installed filter).
     * 
     * @param requestor
     *            application requesting this application launch
     * @returns <code>true</code> if the transition failed
     */
    private boolean doCreateContext(CallerContext requestor)
    {
        appContext = createAppContext(entry.id, domain, requestor);
        // If couldn't create, then return failure
        if (appContext == null) return true;

        if (TRACKING)
        {
            RefTracker.getInstance().track(appContext);
        }

        return false;
    }

    /**
     * A simple base class used to execute code that returns a boolean within
     * the associated {@link AppContext}.
     * 
     * @author Aaron Kamienski
     */
    abstract class Returnable implements Runnable
    {
        private boolean returnCode;

        /**
         * Simply implement this method.
         */
        abstract boolean doRun();

        public void run()
        {
            returnCode = doRun();
        }

        /**
         * Invoke this method to execute the {@link #doRun} method in the
         * application's context.
         * 
         * <pre>
         * boolean rc = new Returnable()
         * {
         *     boolean doRun()
         *     {
         *         //...
         *         return true;
         *     }
         * }.doit();
         * </pre>
         * 
         * @return the value returned by <code>doRun()</code>
         */
        boolean doit()
        {
            try
            {
                appContext.runInContextSync(this);
            }
            catch (Throwable e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Could not exec " + this + " - " + XletApp.this, e);
                }
                return true;
            }
            return returnCode;
        }
    }

    /**
     * Performs the transition to the <code>LOADED</code> state from the
     * <code>NOT_LOADED</code> (or <code>AUTO_PARTIALLY_LOADED</code>) state.
     * 
     * @returns <code>true</code> if the transition failed
     */
    private boolean doLoad(final int oldState, final boolean genEvent)
    {
        return new Returnable()
        {
            boolean doRun()
            {
                synchronized (state)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doLoad runnable running - " + XletApp.this);
                    }
                    AppContext ac = appContext;
                    if (!ac.load())
                    {
                        if (ac != null && (xlet = ac.create()) != null)
                        {
                            if (TRACKING)
                            {
                                RefTracker.getInstance().track(xlet);
                            }
                            return state.setState(LOADED, oldState, false, genEvent);
                        }
                    }
                    return true;
                }
            }
        }.doit();
    }

    /**
     * Performs the transition to <code>PAUSED</code> state from the
     * <code>LOADED</code> state.
     * 
     * @returns <code>true</code> if the transition failed
     */
    private boolean doInit(Object payload, final int startState, final int prevState, final boolean genEvent)
    {
        final String args[] = ((payload == null) ? new String[0] : (String[]) payload);
        return new Returnable()
        {
            boolean doRun()
            {
                synchronized (state)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doInit runnable running: " + XletApp.this);
                    }
                    // Create the Xlet
                    AppContext ac = appContext;
                    if (ac == null || xlet == null) return true;
    
                    // Call initXlet
                    boolean failed = true;
                    try
                    {
                        xc = new XletAppContext(XletApp.this);
    
                        if (TRACKING)
                        {
                            RefTracker.getInstance().track(xc);
                        }
    
                        ((XletAppContext) xc).setXletProperty(XletAppContext.PARAMS, args);
    
                        xlet.initXlet(xc);
                        failed = false;
                    }
                    catch (XletStateChangeException e)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("initXlet state change exception - " + xlet, e.getException());
                        }
                    }
                    catch (Throwable e)
                    {
                        ac.uncaughtException(null, e);
                    }
    
                    if (!failed)
                    {
                        // Success
                        reachedInitState = true;
                        return state.setState(INITED, startState, prevState, false, genEvent);
                    }
                    
                    // Enter INIT_FAILED, from which can only go to DESTROYED state
                    state.setState(INIT_FAILED, false);
                    // Signal failure
                    return state.setState(INITED, prevState, true, true);
                }
            }
        }.doit();
    }

    /**
     * Performs the transition to <code>STARTED</code> state from the
     * <code>PAUSED</code> state.
     * 
     * @returns <code>true</code> if the transition failed
     */
    private boolean doStart(final Object payload, final int startState, final int prevState)
    {
        return new Returnable()
        {
            boolean doRun()
            {
                synchronized (state)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doStart runnable running - " + XletApp.this);
                    }
                    boolean failed = false;
                    // Modify the params available via the XletContext.PARAMS
                    // Originally should've been an empty array
                    ((XletAppContext) xc).setXletProperty(XletAppContext.PARAMS,
                                                          payload == null ? new String[0] : payload);
    
                    try
                    {
                        xlet.startXlet();
                    }
                    catch (XletStateChangeException e)
                    {
                        failed = true;
                        if (log.isInfoEnabled())
                        {
                            log.info("startXlet", e.getException());
                        }
                    }
                    catch (Throwable e)
                    {
                        // log error
                        appContext.uncaughtException(null, e);
                        failed = true;
                    }
    
                    if (!failed)
                    {
                        // Success
                        return state.setState(STARTED, startState, prevState, false, true);
                    }
                    
                    return state.setState(STARTED, prevState, true, true);
                }
            }
        }.doit();
    }

    /**
     * Performs the transition to <code>RESUMED</code> state from the
     * <code>PAUSED</code> state.
     * 
     * @returns <code>true</code> if the transition failed
     */
    private boolean doResume(final int startState)
    {
        return new Returnable()
        {
            boolean doRun()
            {
                synchronized (xlet.getXlet())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doResume runnable running - " + XletApp.this);
                    }
                    boolean failed = false;
                    // Modify the params available via the XletContext.PARAMS
                    // property.Originally should've been an empty array
                    ((XletAppContext) xc).setXletProperty(XletAppContext.PARAMS, new String[0]);
    
                    try
                    {
                        xlet.startXlet();
                    }
                    catch (XletStateChangeException e)
                    {
                        failed = true;
                        if (log.isInfoEnabled())
                        {
                            log.info("startXlet", e.getException());
                        }
                    }
                    catch (Throwable e)
                    {
                        // log error
                        appContext.uncaughtException(null, e);
                        failed = true;
                    }
    
                    return state.setState(RESUMED, startState, failed, true);
                }
            }
        }.doit();
    }

    /**
     * Performs the transition to <code>PAUSED</code> state from the
     * <code>STARTED</code> state.
     * 
     * @returns <code>true</code> if the transition failed
     */
    private boolean doPause(final int startState)
    {
        return new Returnable()
        {
            boolean doRun()
            {
                synchronized (state)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("doPause runnable running - " + XletApp.this);
                    }
                    boolean failed = false;
    
                    try
                    {
                        // Call pauseXlet
                        xlet.pauseXlet();
                    }
                    catch (Throwable e)
                    {
                        failed = true;
                        appContext.uncaughtException(null, e);
                    }
    
                    return state.setState(PAUSED, startState, failed, true);
                }
            }
        }.doit();
    }

    /**
     * Performs the transition to <code>DESTROYED</code> state from any state.
     * 
     * @returns <code>true</code> if the transition failed
     */
    private boolean doDestroy(final Object payload, final int startState)
    {
        if (log.isDebugEnabled())
        {
            log.debug("doDestroy(" + payload + ")");
        }

        boolean failed = false;

        if (log.isDebugEnabled())
        {
            log.debug("doDestroy -- xlet=" + xlet);
        }

        // If no xlet... don't bother calling
        if (xlet != null)
        {
            failed = new Returnable()
            {
                boolean doRun()
                {
                    synchronized (state)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("doDestroy runnable running - " + XletApp.this);
                        }
                        boolean myFailed = false;
                        boolean forced = ((Boolean) payload).booleanValue();
        
                        // Don't bother calling destroyXlet() if never got to INITED
                        // stage
                        if (reachedInitState)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("doDestroy -- xlet.destroyXlet() called");
                            }
        
                            try
                            {
                                xlet.destroyXlet(forced);
                            }
                            catch (XletStateChangeException e)
                            {
                                // If forced, then this operation never fails
                                myFailed = !forced;
                                if (log.isInfoEnabled())
                                {
                                    log.info("destroyXlet", e.getException());
                                }
                            }
                            catch (Throwable e)
                            {
                                // If forced, then this operation never fails
                                myFailed = !forced;
                                appContext.uncaughtException(null, e);
                            }
                            if (log.isDebugEnabled())
                            {
                                log.debug("doDestroy -- myFailed=" + myFailed);
                            }
                        }
                        //destroyXlet has been called, it is now ok to deactivate the xlet prior to posting the destroyed event
                        appContext.setInactive();
        
                        // Destroyed is always the final state for event notification,
                        // so send the event
                        return state.setState(DESTROYED, startState, myFailed, true);
                    }
                }
            }.doit();
        }
        else
        {
            state.setState(DESTROYED, startState, false, false);
        }

        // "Forget" objects associated with the previously execution environment
        if (FORGET_IN_DESTROY && !failed)
        {
            xlet = null;
            xc = null;
            // NOTE: cannot forget appContext here, as it prevents notification
            // appContext = null;
        }

        return failed;
    }

    /**
     * Performs the transition from <code>DESTROYED</code> to
     * <code>NOT_LOADED</code>. All state information from previous execution is
     * forgotten, including the execution environment.
     * 
     * @return <code>true</code> if the transition failed
     */
    private boolean doUnload()
    {
        // Forget everything
        // (May have been forgotten in doDestroy() already.)
        // (Although not if destroyed via XletContext.)
        xlet = null;
        xc = null;
        appContext = null;

        // Always succeeds
        // - must've been in DESTROYED state to even get here
        // - only accessible to the implementation
        // However, if we are disposed, then pretend like this operation
        // failed so that we stay in the DESTROYED state.
        return disposed;
    }

    /**
     * Creates a new <code>AppContext</code> to be used by this application.
     * This is only invoked via {@link #doCreateContext}. This essentially
     * performs the following operation:
     * 
     * <pre>
     * AppManager appmgr = AppManager.getAppManager();
     * return appmgr.createAppContext(this, id, domain);
     * </pre>
     * <p>
     * 
     * This method exists purely for test purposes (otherwise the contained code
     * normally would be inlined within <code>doPartialLoad</code>. The method
     * is package-private for testing purposes.
     * 
     * @param requestor
     *            context requesting the creation
     * 
     * @return a new <code>AppContext</code> to be used by this application
     */
    // NOTE: default access (package private) for testing purposes!!!!
    AppContext createAppContext(final AppID appId, final AppDomainImpl appDomain, CallerContext requestor)
    {
        final AppContext[] rc = { null };
        if (!CallerContext.Util.doRunInContextSync(requestor, new Runnable()
        {
            public void run()
            {
                try
                {
                    AppManager appmgr = AppManager.getAppManager();
                    rc[0] = appmgr.createAppContext(XletApp.this, appId, appDomain);
                }
                catch (Throwable e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Failed to create context", e);
                    }
                    if (e instanceof RuntimeException)
                        throw (RuntimeException) e;
                    else if (e instanceof Error)
                        throw (Error) e;
                }
            }
        }))
        {
            return null;
        }
        return rc[0];
    }

    /**
     * Overrides the
     * {@link Application#notifyListeners(AppID, int, int, Object, boolean, boolean)
     * superclass} method to additionally inform managers about a change in
     * state through associated {@link CallbackData} objects.
     * 
     * @param appId
     *            a registry entry representing the tracked application
     * @param fromState
     *            the state the application was in before the state transition
     *            was requested, where the value of fromState is one of the
     *            state values defined in the AppProxy interface or in the
     *            interfaces inheriting from it
     * @param toState
     *            state the application would be in if the state transition
     *            succeeds, where the value of toState is one of the state
     *            values defined in the AppProxy interface or in the interfaces
     *            inheriting from it
     * @param source
     *            the <code>AppProxy</code> where the state transition happened
     * @param hasFailed
     *            an indication of whether the transition failed (true) or
     *            succeeded (false)
     */
    protected void notifyListeners(AppID appId, int fromState, int toState, Object source, boolean hasFailed,
            boolean genEvent)
    {
        // Go over set of callback data and perform callbacks
        // Cool part, is that this will occur on the application thread!
        super.notifyListeners(appId, fromState, toState, source, hasFailed, genEvent);

        // Or should we do it BEFORE notifying listeners?
        if (!hasFailed)
        {
            AppContext appCC = this.appContext;
            if (appCC != null)
            {
                switch (toState)
                {
                    case PAUSED: // RESUMED|STARTED -> PAUSED
                        appCC.notifyPaused();
                        break;
                    case STARTED: // INITED -> STARTED
                    case RESUMED: // PAUSED -> RESUMED
                        appCC.notifyActive();
                        break;
                    case DESTROYED: // * -> DESTROYED
                        appCC.notifyDestroyed();
                        break;
                }
            }
        }
    }

    // Description copied from Application
    void setPriority(int newPriority)
    {
        // Do nothing if not currently running
        // Else, pass on to current appContext
        if (appContext != null) appContext.setPriority(newPriority);
    }

    /**
     * Returns the <code>ClassLoader</code> used to load this application.
     * <p>
     * This exists purely for testing purposes.
     * 
     * @return the <code>ClassLoader</code> used to load this application.
     */
    ClassLoader getAppClassLoader()
    {
        return (appContext == null) ? null : appContext.getClassLoader();
    }

    /**
     * Returns the <code>javax.tv.xlet.XletContext</code> used by this
     * application.
     * <p>
     * This exists purely for testing purposes.
     * 
     * @return this Xlet application's <code>javax.tv.xlet.XletContext</code>.
     */
    javax.tv.xlet.XletContext getJavaTVXletContext()
    {
        return xc;
    }

    /**
     * Returns the <code>javax.microedition.xlet.XletContext</code> used by this
     * application.
     * <p>
     * This exists purely for testing purposes.
     * 
     * @return this Xlet application's
     *         <code>javax.microedition.xlet.XletContext</code>.
     */
    javax.microedition.xlet.XletContext getJavaMEXletContext()
    {
        return xc;
    }

    /**
     * Returns the <code>Xlet</code> that is the initial object of this
     * application.
     * <p>
     * This exists purely for testing purposes.
     * 
     * @return the initial <code>Xlet</code> object that is this application
     */
    Xlet getXlet()
    {
        return xlet;
    }

    /**
     * Returns the <code>AppContext</code> that is currently being used to run
     * this application.
     * <p>
     * This exists purely for testing purposes.
     * 
     * @return current <code>AppContext</code>.
     */
    AppContext getAppContext()
    {
        return appContext;
    }

    /** This application's CallerContext. */
    private AppContext appContext;

    /** This application's <code>XletContext</code>. */
    private XletAppContext xc;

    /** This application's initial <code>Xlet</code> instance. */
    private Xlet xlet;

    /**
     * Indicates whether {@link #unload} is enabled or not.
     * 
     * @see #setUnloadEnabled
     */
    private boolean unloadEnabled = true;

    /**
     * Indicates whether or not this xlet has successfully reached the INITED
     * state. Only when an Xlet has reached the INITED state will its
     * destroyXlet() method be called.
     */
    private boolean reachedInitState = false;

    /**
     * Debug flag used to enable "forgetting" in doDestroy as opposed to
     * doUnload().
     * 
     * Note that we cannot forget the AppContext in doDestroy. As we would be
     * forgetting it before it's notified about the DESTROY. As such, it never
     * cleans up; it never causes the ->NOT_LOADED transition.
     */
    private static final boolean FORGET_IN_DESTROY = true;

    /** Log4J Logger. */
    private static final Logger log = Logger.getLogger(XletApp.class.getName());

    private static final boolean TRACKING = true;
}
