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
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsControlPermission;
import org.dvb.application.DVBJProxy;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContext.Multicaster;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.Arrays;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.TaskQueue;

/**
 * Provides a common base class for <code>AppProxy</code> implementations. An
 * <code>Application</code> represents, to the Application Manager, an
 * application and it's state.
 * <p>
 * Subclasses of this class must provide all abstract methods, including the
 * <code><i>handle*()</i></code> methods which are responsible for managing
 * application state transitions. These <code><i>handle*()</i></code> methods
 * should do the following:
 * 
 * <ol>
 * <li>Check the validity of the state change. Set <code>failed = valid?</code>.
 * <li>Do any work involved with the state change, if valid.
 * <li> {@link State#setState(int,int,int,boolean,boolean) Record} the state
 * change(s), success or failure.
 * </ol>
 * 
 * 
 * @author Aaron Kamienski
 */
public abstract class Application implements AppProxy
{
    /**
     * Constructs a new <code>Application</code>.
     * 
     * @param entry
     *            the <code>AppEntry</code> specifying the application
     * @param domain
     *            the domain to which the app belongs
     * @param state
     *            the initial state of the application
     * @param version
     *            the application version
     */
    protected Application(AppEntry entry, AppDomainImpl domain, int state)
    {
        this.entry = entry;
        this.domain = domain;
        this.tq = createTaskQueue();
        this.state = new State(state);
    }

    /**
     * Provides for state change listening. The listener (as well as the current
     * <code>CallerContext</code> is stored in a multicaster. No effort is made
     * to guard against duplicate listeners (as the API doc specifically says
     * this should not be the case).
     * 
     * @param listener
     *            the listener to add
     */
    public void addAppStateChangeEventListener(AppStateChangeEventListener listener)
    {
        addAppStateChangeEventListener(listener, AppManager.getCaller());
    }

    /**
     * Removes the listener previously added.
     * 
     * @param listener
     *            the listener to remove
     */
    public void removeAppStateChangeEventListener(AppStateChangeEventListener listener)
    {
        removeAppStateChangeEventListener(listener, AppManager.getCaller());
    }

    /**
     * Provides for state change listening. The listener (as well as the current
     * <code>CallerContext</code> is stored in a multicaster. No effort is made
     * to guard against duplicate listeners (as the API doc specifically says
     * this should not be the case).
     * 
     * @param listener
     *            the listener to add
     * @param ctx
     *            the associated caller <code>CallerContext</code>
     */
    void addAppStateChangeEventListener(AppStateChangeEventListener listener, CallerContext ctx)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = getData(ctx);

            // Update listener/multicaster
            data.listeners = EventMulticaster.add(data.listeners, listener);
        }
    }

    /**
     * Removes the listener previously added.
     * 
     * @param listener
     *            the listener to remove
     * @param ctx
     *            the associated caller <code>CallerContext</code>
     */
    void removeAppStateChangeEventListener(AppStateChangeEventListener listener, CallerContext ctx)
    {
        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = (Data) ctx.getCallbackData(this);

            if (data != null)
            {
                // Remove the given listener from the set of listeners
                if (data.listeners != null)
                {
                    data.listeners = EventMulticaster.remove(data.listeners, listener);
                }

                // Remove data altogether
                if (data.listeners == null)
                {
                    ctx.removeCallbackData(this);
                    contexts = Multicaster.remove(contexts, ctx);
                }
            }
        }
    }

    /**
     * Returns the current state of the given application.
     * 
     * @return the current state of the given application.
     */
    public int getState()
    {
        return state.getState() & STATE_MASK;
    }

    /** Submits a request to transition the application to the paused state. */
    public void pause()
    {
        // verify this application is signaled in the current service
        if (disposed || domain.getAppEntry(entry.id) == null)
        {
            throw new SecurityException("application is not signaled in current service");
        }

        checkAppsControl("pause");
        // State change to PAUSED
        tq.post(new StateChange(PAUSED));
    }

    /** Submits a request to transition the application to the active state. */
    public void resume()
    {
        // verify this application is signaled in the current service
        if (disposed || domain.getAppEntry(entry.id) == null)
        {
            throw new SecurityException("application is not signaled in current service");
        }

        checkAppsControl("resume");
        resumeRequest();
    }

    /** Implements resume without making security checks. */
    void resumeRequest()
    {
        // State change to STARTED/RESUMED
        tq.post(new StateChange(RESUMED));
    }

    /** Submits a request to transition the application to the active state. */
    public void start()
    {
        // state change to STARTED
        start(null);
    }

    /**
     * Request that the application manager start the application bound to this
     * information structure passing to that application the specified
     * parameters.
     * <p>
     * The <code>application</code> will be started. This method will throw a
     * security exception if the application does not have the authority to
     * start applications.
     * 
     * Calls to this method shall only succeed under the following conditions:
     * <ul>
     * <li>if the application (DVB-J or DVB-HTML) is in the not loaded or paused
     * states,
     * <li>if a DVB-J application is in the "loaded" state,
     * <li>if a DVB-HTML application is in the "loading" state.
     * </ul>
     * <p>
     * If the application was not loaded at the moment of this call, then the
     * application will be started. In the case of a DVB-J application, it will
     * be initialized and then started by the Application Manager, hence causing
     * the Xlet to go from NotLoaded to Paused and then from Paused to Active.
     * If the application was in the Paused state at the moment of the call and
     * had never been in the Active state, then the application will be started.
     * If the application represented by this AppProxy is a DVB-J application,
     * calling this method will, if successful, result in the
     * <code>startXlet</code> method being called on the Xlet making up the
     * DVB-J application.
     * <p>
     * This method is asynchronous and its completion will be notified by an
     * AppStateChangedEvent. In case of failure, the hasFailed method of the
     * <code>AppStateChangedEvent</code> will return true.
     * 
     * @param args
     *            the parameters to be passed into the application being started
     * @throws SecurityException
     *             if the application is not entitled to start this application.
     * 
     * @since MHP1.0.1
     */
    public void start(String[] args)
    {
        // verify this application is signaled in the current service
        if (disposed || domain.getAppEntry(entry.id) == null)
        {
            throw new SecurityException("application is not signaled in current service");
        }

        if (checkProfileVersion())
        {
            // State change to STARTED
            tq.post(new StateChange(STARTED, args, AppManager.getCaller()));
        }
        else
        {
            notifyListeners(entry.id, state.getPrivateState(), // from State,
                    STARTED, // to state
                    Application.this, // AppProxy
                    true, // has failed
                    true); // gen event
        }
    }

    /**
     * Requests that the application manager auto-start the given application.
     * This method is to be used internally to start() applications which are
     * being launched because they are "AUTOSTART".
     * <p>
     * For in-band applications, <code>autostart()</code> should function
     * identical to {@link #start()}. For out-of-band applications, which are
     * signaled as part of an <code>AbstractService</code>,
     * <code>autostart()</code> implies that the application should be
     * implicitly downloaded if necessary.
     */
    void autostart()
    {
        if (checkProfileVersion())
        {
            // No security check -- as this is only available to the
            // implementation
            tq.post(new StateChange(AUTO_STARTED, null, AppManager.getCaller()));
        }
        else
        {
            notifyListeners(entry.id, state.getPrivateState(), // from State,
                    AUTO_STARTED, // to state
                    Application.this, // AppProxy
                    true, // has failed
                    true); // gen event
        }
    }

    /**
     * Submits a request to transition the application to the destroyed state.
     * 
     * @param forced
     *            If true, then the application must stop and destroy itself. If
     *            false, then the application has the opportunity to ignore the
     *            request.
     */
    public void stop(boolean forced)
    {
        // verify this application is signaled in the current service
        if (disposed || domain.getAppEntry(entry.id) == null)
        {
            throw new SecurityException("application is not signaled in current service");
        }

        checkAppsControl("stop");
        if (log.isInfoEnabled())
        {
            log.info("stop - forced: " + forced + " - AppID: " + getAppID());
        }
        // State change to STOPPED
        tq.post(new StateChange(DESTROYED, forced ? Boolean.TRUE : Boolean.FALSE));
    }
    
    /**
     * Implements the handling of a <i>pause</i> state change request.
     * <p>
     * Transitions to {@link AppProxy#PAUSED} state from
     * {@link AppProxy#STARTED}. All other transition requests result in
     * failure.
     * <p>
     * Should be executed on the private {@link #tq TaskQueue}.
     * 
     * @see #pause
     */
    protected abstract void handlePause();

    /**
     * Implements the handling of a <i>resume</i> state change request.
     * <p>
     * Transitions to {@link AppProxy#STARTED} state from
     * {@link AppProxy#PAUSED}, if application has previously been
     * {@link AppProxy#STARTED}. All other transition requests result in
     * failure.
     * <p>
     * Should be executed on the private {@link #tq TaskQueue}.
     * 
     * @see #resume
     * 
     * @see #RESUMED
     */
    protected abstract void handleResume();

    /**
     * Implements the handling of a <i>start</i> state change request.
     * <p>
     * Transitions to {@link AppProxy#STARTED} state from
     * {@link AppProxy#NOT_LOADED} state. All other transition requests result
     * in failure.
     * <p>
     * Should be executed on the private {@link #tq TaskQueue}.
     * 
     * @param args
     *            the parameters to be passed into the application being
     *            started; or <code>null</code>
     * @param requestor
     *            the application requesting that the app be started
     * 
     * @see #start()
     * @see #start(String[])
     */
    protected abstract void handleStart(String[] args, CallerContext requestor);

    /**
     * Implements the handling of <i>stop</i> state change request.
     * <p>
     * Transitions to {@link AppProxy#DESTROYED} state from any state -- other
     * than <code>DESTROYED</code>.
     * <p>
     * Should be executed on the private {@link #tq TaskQueue}.
     * 
     * @param forced
     *            If true, then the application must stop and destroy itself. If
     *            false, then the application has the opportunity to ignore the
     *            request.
     * 
     * @see #stop
     */
    protected abstract void handleStop(boolean forced);

    /**
     * Implements the handling of <i>autostart</i> state change request.
     * <p>
     * Transitions to {@link AppProxy#STARTED} from {@link AppProxy#NOT_LOADED}.
     * <p>
     * Should be executed on the private {@link #tq TaskQueue}.
     * 
     * @param requestor
     *            the application requesting that the app be started
     * 
     * @see #autostart()
     * @see #AUTO_STARTED
     */
    protected abstract void handleAutoStart(CallerContext requestor);

    /**
     * Performs a security check to see if the calling code has the necessary
     * access rights to be able to control the given application.
     * <p>
     * Any application (including unsigned) is allowed to launch an application
     * that it has access to. However, only signed applications with
     * AppControlPermission can control (pause, resume, stop) applications that
     * it did not launch.
     * 
     * @param action
     *            the action to be performed on the given app
     * 
     * @throws SecurityException
     *             if the necessary rights are not granted to the caller.
     * 
     * @see "MHP 12.6.2.9"
     */
    protected void checkAppsControl(String action) throws SecurityException
    {
        if (isAppsControlPermissionRequired()) SecurityUtil.checkPermission(PERMISSION);
    }

    /**
     * This method is used to determine if <code>AppsControlPermission</code> is
     * required. This is invoked by {@link #checkAppsControl} before testing for
     * this permission.
     * <p>
     * This method should be implemented such that:
     * <ul>
     * <li> <code>false</code> returned if this app hasn't been launched
     * <li> <code>false</code> returned if this app has been launched by the
     * caller
     * <li> <code>true</code> returned if this app has been launched by a
     * different application
     * </ul>
     * 
     * @return whether <code>AppsControlPermission</code> is required to control
     *         this application
     */
    protected abstract boolean isAppsControlPermissionRequired();

    /**
     * Notify all listeners that the application state has changed. Note that
     * this is required to handle each notification on it's app-specific thread.
     * I.e., it must use the <code>CallerContext</code> that was in place when
     * the listener was installed.
     * <p>
     * Parameters are as for <code>AppStateChangeEvent</code>, but with
     * <i>fromstate</i> and <i>tostate</i> including implementation-specific
     * values.
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
     * @param genEvent
     *            if <code>true</code> then generate an event (should always be
     *            <code>true</code> if <i>hasFailed</i> is <code>true</code>)
     */
    protected void notifyListeners(final AppID appId, int fromState, int toState, Object source, boolean hasFailed,
            boolean genEvent)
    {
        if (log.isInfoEnabled())
        {
            log.info("notifyListeners - appID: " + appId + " fromstate: " + state2String(fromState) + " toState: " + state2String(toState) + ", source: " + source +", hasfailed: " + hasFailed);
        }
        // notify listeners of event
        if (genEvent)
        {
            final AppStateChangeEvent event = new AppStateChangeEvent(appId, fromState & Application.STATE_MASK,
                    toState & Application.STATE_MASK, source, hasFailed);

            CallerContext localContexts = this.contexts;
            if (localContexts != null)
            {
                final CallerContextManager cm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                localContexts.runInContext(new Runnable()
                {
                    public void run()
                    {
                        CallerContext ctx = cm.getCurrentContext();
                        Data data = (Data) ctx.getCallbackData(Application.this);

                        if (data != null && data.listeners != null)
                        {
                            data.listeners.stateChange(event);
                        }
                    }
                });
            }
        }
    }

    /**
     * This application is no longer needed and is marked as
     * <code>disposed</code> and is implicitly moved to the
     * <code>DESTROYED</code> state. Once disposed, it should not be allowed to
     * move to any state other than the <code>DESTROYED</code> state.
     * <p>
     * This is used by {@link AppDomainImpl#disposeApp(Application, AppID)} when
     * stopping an application that will be forgotten and should not be
     * remembered.
     */
    void dispose()
    {
        disposed = true;

        // State change to DESTROYED
        if (log.isInfoEnabled())
        {
            log.info("dispose - AppID: " + getAppID());
        }
        tq.post(new StateChange(DESTROYED, Boolean.TRUE));
    }
    
    /**
     * Returns the signalling entry associated with this application
     * 
     * @return the app signalling entry
     */
    AppEntry getAppEntry()
    {
        return entry;   
    }

    /**
     * Returns the <code>AppID</code> for this application.
     * 
     * @return the <code>AppID</code> for this application
     */
    AppID getAppID()
    {
        return entry.id;
    }

    /**
     * Returns the <code>AppDomain</code> for this application.
     * 
     * @return the <code>AppDomain</code> for this application
     */
    AppDomainImpl getAppDomain()
    {
        return domain;
    }

    /**
     * Sets the priority of a running application. If this application is not
     * currently running, then it has no effect.
     * <p>
     * This is used to adjust the priority of a (potentially) running
     * application per signaling (specifically per the AIT
     * <i>external_authorization_descriptor</i>).
     * 
     * @param newPriority
     */
    abstract void setPriority(int newPriority);

    /**
     * Cause this <code>Application</code> to <i>forget</i> all listeners
     * associated with the given <code>CallerContext</code>. This is done simply
     * by setting the reference to <code>null</code> and letting the garbage
     * collector take care of the rest.
     * <p>
     * This is <i>package private</i> for testing purposes.
     * 
     * @param c
     *            the <code>CallerContext</code> to forget
     */
    void forgetListeners(CallerContext c)
    {
        // Simply forget the given c
        // No harm done if never added
        synchronized (lock)
        {
            c.removeCallbackData(this);
            contexts = Multicaster.remove(contexts, c);
        }
    }

    /**
     * Return a <code>String</code> representation of the given state.
     * 
     * @param st
     * @return a <code>String</code> representation of the given state.
     */
    // TODO: move to a separate utility class
    static String state2String(int st)
    {
        switch (st)
        {
            case AppProxy.NOT_LOADED:
                return "NOT_LOADED";
            case XletApp.AUTO_PARTIALLY_LOADED:
                return "~AUTO_PARTIALLY_LOADED";
            case DVBJProxy.LOADED:
                return "LOADED";
            case XletApp.INIT_FAILED:
                return "~INIT_FAILED";
            case XletApp.INITED:
                return "~INITED";
            case AppProxy.PAUSED:
                return "PAUSED";
            case AppProxy.STARTED:
                return "STARTED";
            case Application.AUTO_STARTED:
                return "~AUTO_STARTED";
            case Application.RESUMED:
                return "~RESUMED";
            case AppProxy.DESTROYED:
                return "DESTROYED";
            default:
                return "?" + st + "?";
        }
    }

    /**
     * Create and return a <code>TaskQueue</code> for use in making task state
     * changes occur asynchronously, but sequentially.
     * 
     * @return a new system <code>TaskQueue</code>
     */
    private static TaskQueue createTaskQueue()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        return ccm.getSystemContext().createTaskQueue();
    }

    /**
     * Access this object's global data object associated with current context.
     * If none is assigned, then one is created.
     * <p>
     * Synchronizes on the internal object {@link #lock}.
     * 
     * @param ctx
     *            the context to access
     * @return the <code>Data</code> object
     */
    private Data getData(CallerContext ctx)
    {
        synchronized (lock)
        {
            Data data = (Data) ctx.getCallbackData(this);
            if (data == null)
            {
                data = new Data();
                ctx.addCallbackData(data, this);
                contexts = Multicaster.add(contexts, ctx);
            }
            return data;
        }
    }

    /*
     * Refer to DVB_MHP 103 10.7.3 Application Descriptor Application Profile
     * 
     * @return true if the application can run on this platform else false.
     */
    boolean checkProfileVersion()
    {
        int appVer = 0;
        int sysVer = 0;
        try
        {
            // we can't use AppsDatabase#getAppAttributes(AppID) because that
            // method returns null for non-visible apps
            AppAttributes appAttributes = domain.getAppAttributesIgnoringVisibility(entry.id);
            if (appAttributes == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("checkProfileVersion - unable to retrieve application attributes for app: " + entry.id
                            + " - returning false");
                }
                return false;
            }

            String[] profiles = appAttributes.getProfiles();
            if (0 == profiles.length)
            {
                // profile must be provided per MHP 1.0.3 Section 10.7.3
                if (log.isWarnEnabled())
                {
                    log.warn("checkProfileVersion - no profile entry exists for app: " + entry.id + " - returning false");
                }
                return false;
            }

            for (int x = 0; x < profiles.length; x++)
            {
                String sysProperty;
                // get application version for profile

                int[] appVers = appAttributes.getVersions(profiles[x]);
                if (log.isDebugEnabled())
                {
                    log.debug("examining app profile for index " + x + "(" + profiles[x] + "): "
                            + Arrays.toString(appVers));
                }
                appVer = ((appVers[0] & 0xFF) << 16) | // major
                        ((appVers[1] & 0xFF) << 8) | // minor
                        (appVers[2] & 0xFF); // micro

                // get versions for system
                // refer to AttributesImpl.decodeProfile() method
                if ("mhp.profile.enhanced_broadcast".equals(profiles[x])) // MHP
                {
                    sysProperty = "mhp.eb.version.";
                }
                else if ("mhp.profile.interactive_broadcast".equals(profiles[x])) // MHP
                {
                    sysProperty = "mhp.ib.version.";
                }
                else
                {
                    sysProperty = "ocap.version.";
                }
                if (log.isDebugEnabled())
                {
                    log.debug("system property used to evaluate app profile: " + sysProperty);
                }

                String major = System.getProperty(sysProperty + "major");
                String minor = System.getProperty(sysProperty + "minor");
                String micro = System.getProperty(sysProperty + "micro");

                if (major == null || minor == null || micro == null)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("profile not defined - returning false - profile: " + sysProperty + ", app version: "
                                + Arrays.toString(appVers));
                    }
                    return false;
                }

                sysVer = ((Integer.parseInt(major) & 0xFF) << 16) | ((Integer.parseInt(minor) & 0xFF) << 8)
                        | (Integer.parseInt(micro) & 0xFF);

                // can app run on the system?
                if (appVer <= sysVer)
                {
                    return true;
                }
            }
            // no good version found
            if (log.isWarnEnabled())
            {
                log.warn("App 0x" + entry.id + " has invalid profile: 0x" + Integer.toHexString(appVer)
                        + " system version: 0x" + Integer.toHexString(sysVer));
            }
            return false;

        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("failure attempting to verify application profile is supported", e);
            }
            return false;
        }
    }

    /**
     * The <code>AppProxy</code> state machine is extended to separate some
     * states into multiple states. E.g., <code>RESUMED</code> is separated into
     * <code>STARTED</code> so that {@link #resume} doesn't do anything if not
     * in the <code>PAUSED</code> state. The {@link #getState} implementation
     * uses <code>STATE_MASK</code> to mask off the alternate state information
     * such that <code>RESUMED</code> becomes <code>STARTED</code> to the
     * outside world.
     * 
     * @see #EXT_STATE
     * @see #RESUMED
     * @see #ILLEGAL
     */
    public static final int STATE_MASK = 0xFF;

    /**
     * <i>Or</i>ed against <code>AppProxy</code> states to create extension
     * states. E.g., <code>PAUSED | EXT_STATE</code> is <code>INITED</code>.
     */
    public static final int EXT_STATE = 0x100;

    /**
     * <i>Or</i>ed against <code>AppProxy</code> states to create extension
     * states representing an application being <i>AUTOSTARTED</i>.
     * 
     * @see #AUTO_STARTED
     */
    public static final int EXT_AUTO_STATE = 0x200;

    /**
     * The <i>resumed</i> state represents the <code>STARTED</code> state
     * transitioned to from the <code>PAUSED</code> (not <code>INITED</code>)
     * state.
     * 
     * @see #STATE_MASK
     * @see #STARTED
     */
    public static final int RESUMED = STARTED | EXT_STATE;

    /**
     * Used to flag an application that should be <code>DESTROYED</code> but is
     * not cooperating.
     * 
     * @see #STATE_MASK
     * @see #DESTROYED
     */
    public static final int ILLEGAL = DESTROYED | EXT_STATE;

    /**
     * The <i>auto-started</i> state represents the <code>STARTED</code> state
     * being transitioned to as part of {@link #autostart} procedures.
     * 
     * @see #STATE_MASK
     * @see #STARTED
     */
    public static final int AUTO_STARTED = STARTED | EXT_AUTO_STATE;

    /**
     * Statically initialized singleton used for all calls to
     * <code>checkPermission()</code>.
     */
    private static final AppsControlPermission PERMISSION = new AppsControlPermission();

    /**
     * TaskQueue used to handle state change requests (e.g.,
     * {@link AppProxy#start()}.
     */
    protected TaskQueue tq;

    /** The current state of this application. */
    protected State state;

    /** The <code>AppDomain</code> associated with this application. */
    protected AppDomainImpl domain;

    protected AppEntry entry;
    
    /**
     * Indicates that this component has been disposed. After which, state
     * transitions to states other than <code>DESTROYED</code> will fail.
     * <p>
     * 
     * @note This is in-place so that the <code>AppDomainImpl</code> can get rid
     *       of <code>XletApp</code>s that it no longer needs (i.e., when it
     *       removes them from the DB).
     */
    protected boolean disposed = false;

    /** The set of CallerContext's who have installed listeners */
    private CallerContext contexts = null;

    /** Internal lock. */
    private Object lock = new Object();

    /**
     * Represents a state change request, usually initiated by calls made on an
     * {@link AppProxy}.
     * 
     * @author Aaron Kamienski
     */
    protected class StateChange implements Runnable
    {
        public StateChange(int state)
        {
            this(state, null, null);
        }

        public StateChange(int state, Object payload)
        {
            this(state, payload, null);
        }

        public StateChange(int state, Object payload, CallerContext requestor)
        {
            this.targetState = state;
            this.payload = payload;
            this.requestor = requestor;
            if (log.isDebugEnabled())
            {
                log.debug("created StateChange for: " + state2String(targetState) + " payload: " + payload + ", requestor: " + requestor + ", this: " + Application.this + ", AppID: " + getAppID());
            }
        }

        public void run()
        {
            if (log.isDebugEnabled())
            {
                log.debug("StateChange running for: " + state2String(targetState) + " payload: " + payload + ", requestor: " + requestor + ", this: " + Application.this + ", AppID: " + getAppID());
            }
            switch (targetState)
            {
                case STARTED:
                    handleStart((String[]) payload, requestor);
                    break;
                case AUTO_STARTED:
                    handleAutoStart(requestor);
                    break;
                case PAUSED:
                    handlePause();
                    break;
                case RESUMED:
                    handleResume();
                    break;
                case DESTROYED:
                    handleStop(((Boolean) payload).booleanValue());
                    break;
                default:
                    if (log.isErrorEnabled())
                    {
                        log.error("Need to handle event transition to " + targetState, new Exception("stack trace"));
                    }
                    break;
            }
        }

        public String toString()
        {
            return "StateChange[" + state2String(targetState) + "]";
        }

        protected int targetState;

        protected Object payload;

        protected CallerContext requestor;
    }

    /**
     * The <code>State</code> object maintains the state of the application.
     * This is a separate instance <code>Object</code> from the
     * <code>Application</code> itself so that it's access can be synchronized
     * without using the {@link Application}'s monitor.
     * <p>
     * This is important if the <code>Application</code> instance is to be
     * returned as the <code>AppProxy</code> instance to applications.
     * <p>
     * The <i>state</i> of the application is maintained as the current state
     * {@link #getState() identifier}.
     * 
     * @author Aaron Kamienski
     */
    protected class State
    {
        /**
         * Creates the <code>State</code> object with the given initial
         * <code>state</code>.
         * 
         * @param state
         *            initial <code>state</code> id
         */
        public State(int state)
        {
            this.privateState = state;
        }

        /**
         * Returns the current state identifier.  If we are currently in a state
         * transition, this will always return the current state and not the
         * destination state.
         * 
         * @return the current state identifier.
         */
        public int getState()
        {
            return privateState;
        }
        
        /**
         * Blocking call to get the current state.  This ensures that we always get
         * the destination state if we are currently in a state transition.
         * 
         * @return
         */
        synchronized int getPrivateState()
        {
            return privateState;
        }

        /**
         * Directly sets the state of this <code>State</code>. Performs no work
         * involved with the state change other than notifying listeners and
         * recording the state change. The new state is recorded as the new
         * state identifier and the new state transition function.
         * <p>
         * Note that the return value specifies whether the operation ultimately
         * failed or not. In general the return value will be identical to the
         * input <code>failed</code> value. However, if the current <i>state</i>
         * is different from <code>oldState</code>, then the state change will
         * fail and <code>true</code> will be returned. This is possible if the
         * state is changed while in the middle of a transition (which is only
         * possible if the <code>Xlet</code> calls a
         * <code>XletContext.notifyXXX</code> method).
         * 
         * @param newState
         *            the state being transitioned to; corresponds to the value
         *            returned by
         *            {@link org.dvb.application.AppStateChangeEvent#getToState}
         *            .
         * @param fromState
         *            the state being transitioned from; corresponds to the
         *            value returned by
         *            {@link org.dvb.application.AppStateChangeEvent#getFromState}
         *            .
         * @param oldState
         *            the current state; if this isn't the current state then
         *            this state change will fail
         * @param failed
         *            whether the transition is considered unsuccessful
         * @param genEvent
         *            if <code>true</code> then generate an
         *            <code>AppStateChangeEvent</code>
         * @return <code>true</code> if the setting of the state ultimately
         *         failed. This will be <code>true</code> if failed is
         *         <code>true</code>. This will be <code>true</code> if failed
         *         is <code>false</code> and <code>oldState</code> does not
         *         match the current state. This will be <code>false</code>
         *         otherwise.
         */
        public synchronized boolean setState(int newState, int fromState, int oldState, boolean failed, boolean genEvent)
        {
            Application app = Application.this;

            // Will implicitly fail, unless DESTROYED is the target state
            if (privateState != oldState)
            {
                failed = true;

                // Change fromState, because it has changed since...
                oldState = privateState;
                fromState = oldState;
                genEvent = true;
            }

            if (failed)
            {
                // Failed state transitions always generate an event
                genEvent = true;
            }
            else
            {
                // Change the state
                privateState = newState;
            }

            if (log.isInfoEnabled())
            {
                log.info("appid=" + app.entry.id + " " + state2String(fromState) + "->" + state2String(newState) + " "
                        + (failed ? "failed" : "success") + ", Event? " + (genEvent ? "yes" : "no"));
            }

            // Notify Listeners
            Application.this.notifyListeners(app.entry.id, fromState, newState, app, failed, genEvent);

            return failed;
        }

        /**
         * Directly sets the state of this <code>State</code> by calling
         * {@link #setState(int,int,int,boolean,boolean)} as appropriate to
         * simply go from <i>fromState</i> to <i>newState</i>.
         * 
         * @param newState
         *            the state being transitioned to; corresponds to the value
         *            returned by
         *            {@link org.dvb.application.AppStateChangeEvent#getToState}
         *            .
         * @param fromState
         *            the state being transitioned from; corresponds to the
         *            value returned by
         *            {@link org.dvb.application.AppStateChangeEvent#getFromState}
         *            .
         * @param failed
         *            whether the transition is considered unsuccessful
         * @param genEvent
         *            if <code>true</code> then generate an
         *            <code>AppStateChangeEvent</code>
         * @return <code>true</code> if the setting of the state ultimately
         *         failed. This will be <code>true</code> if failed is
         *         <code>true</code>. This will be <code>true</code> if failed
         *         is <code>false</code> and <code>oldState</code> does not
         *         match the current state. This will be <code>false</code>
         *         otherwise.
         */
        public synchronized boolean setState(int newState, int fromState, boolean failed, boolean genEvent)
        {
            return setState(newState, fromState, fromState, failed, genEvent);
        }

        /**
         * Directly sets the state of this <code>State</code> by calling
         * {@link #setState(int,int,boolean, boolean)} as appropriate to simply
         * go directly to the given state. This call is meant to be called by
         * the <code>XletContext</code> {@link XletAppContext implemention} as
         * part of the {@link XletAppContext#notifyDestroyed} and
         * {@link XletAppContext#notifyPaused} implementations.
         * 
         * @param newState
         *            the state to transition to
         * @param genEvent
         *            if <code>true</code> then generate an
         *            <code>AppStateChangeEvent</code>
         */
        public synchronized boolean setState(int newState, boolean genEvent)
        {
            return setState(newState, privateState, false, genEvent);
        }

        /**
         * Current state as defined by {@link org.dvb.application.DVBJProxy}.
         */
        private int privateState;

    }

    /**
     * Log4J Logger.
     */
    private static final Logger log = Logger.getLogger(State.class.getName());

    /**
     * Per-context global data.
     * <p>
     * TODO: Could probably be refactored such that it extends an existing
     * implementation to get the dummy method impls.
     */
    private class Data implements CallbackData
    {
        public AppStateChangeEventListener listeners;

        public void destroy(CallerContext cc)
        {
            forgetListeners(cc);
        }

        public void active(CallerContext cc)
        { /* EMPTY */
        }

        public void pause(CallerContext cc)
        { /* EMPTY */
        }
    }

    private static class AppStateChangeEvent extends org.dvb.application.AppStateChangeEvent
    {
        public AppStateChangeEvent(AppID id, int state1, int state2, Object source, boolean failed)
        {
            super(id, state1, state2, source, failed);
        }

        public String toString()
        {
            return "AppStateChangeEvent@" + hashCode() + "[id=" + this.getAppID() + ",fromState="
                    + state2String(getFromState()) + ",toState=" + state2String(getToState()) + ",proxy=" + getSource()
                    + ",hasFailed=" + hasFailed() + "]";
        }
    }
}
