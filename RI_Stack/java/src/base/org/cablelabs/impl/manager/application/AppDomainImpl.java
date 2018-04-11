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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.cablelabs.impl.util.SecurityUtil;
import org.davic.net.Locator;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseEvent;
import org.dvb.application.AppsDatabaseEventListener;
import org.dvb.application.AppsDatabaseFilter;
import org.dvb.application.CurrentServiceFilter;
import org.dvb.application.IllegalProfileParameterException;
import org.dvb.application.RunningApplicationsFilter;
import org.ocap.application.OcapAppAttributes;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;

import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.SignallingManager;
import org.cablelabs.impl.service.ServicesDatabase;
import org.cablelabs.impl.service.ServicesDatabase.ServiceChangeListener;
import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.Ait;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.AppEntry.AppIDCompare;
import org.cablelabs.impl.signalling.SignallingEvent;
import org.cablelabs.impl.signalling.SignallingListener;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.RefTracker;
import org.ocap.system.MonitorAppPermission;

// TODO: consider refactoring similar/common code

/**
 * Implementation of <code>AppDomain</code> and the domain-specific applications
 * database.
 * 
 * @author Aaron Kamienski
 */
public class AppDomainImpl extends AppsDatabase implements AppDomain, SignallingListener,
        TVTimerWentOffListener, ServiceChangeListener
{
    private ExternalAuthorization auth;
    private boolean processedInitialAutostart = false;
    private final AppStateChangeEventListener initialAutostartAppStateChangeEventListener = new InitialAutostartAppStateChangeEventListener();
    private final Map initialAutostartApps = new HashMap();
    private boolean notifiedInitialAutostartAppsStarted = false;
    private InitialAutostartAppsStartedListener initialAutostartAppsStartedListener;

    /**
     * Creates an instance of <code>AppDomainImpl</code> to be used with the
     * given <code>ServiceContext</code>.
     */
    AppDomainImpl(ServiceContext serviceContext, CompositeAppsDB globalDb)
    {
        this.serviceContext = serviceContext;
        this.globalDb = globalDb;
        
        // Axiom change
        synchronized (bootLock)
        {
            if (inBootupPhase)
            {
                bootCallbackListener = new BootProcessCallbackListener();
                // Listen for configured signal
                ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
                sm.getServicesDatabase().addBootProcessCallback(bootCallbackListener);
            }
        }

        synchronized (AppDomainImpl.class)
        {
            uniqueId = new Long(idBase++);
        }
    }

    /**
     * Retrieves the <code>ServiceContext</code> that this
     * <code>AppDomain</code> is associated with.
     * 
     * @return the <code>ServiceContext</code> that this <code>AppDomain</code>
     *         is associate with
     */
    ServiceContext getServiceContext()
    {
        return serviceContext;
    }

    /**
     * Retrieves the <code>Service</code> that is this <code>AppDomain</code>
     * currently has selected
     * 
     * @return the currently selected <code>Service</code>
     */
    ServiceDetails getCurrentServiceDetails()
    {
        return currentServiceDetails;
    }

    /**
     * Returns the <code>AppsDatabase</code> maintained by the
     * <code>AppDomain</code>.
     * 
     * @return the <code>AppsDatabase</code> maintained by the
     *         <code>AppDomain</code>
     */
    AppsDatabase getDatabase()
    {
        return this;
    }

    /**
     * Returns the <code>AppEntry</code> for the given <code>AppID</code>.
     * 
     * @return the <code>AppEntry</code> for the given <code>AppID</code>;
     *         <code>null</code> if no such <code>AppID</code> is known
     */
    AppEntry getAppEntry(AppID id)
    {
        synchronized (lock)
        {
            return (AppEntry) database.get(id);
        }
    }

    Long getID()
    {
        return uniqueId;
    }

    /* ========================AppsDatabase======================= */

    // Description copied from AppsDatabase
    public void removeListener(AppsDatabaseEventListener l)
    {
        CallerContext ctx = ccm.getCurrentContext();

        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = (Data) ctx.getCallbackData(this);

            // Remove the given listener from the set of listeners
            if (data != null && data.listeners != null)
            {
                data.listeners = EventMulticaster.remove(data.listeners, l);
                if (data.listeners == null) ctx.removeCallbackData(this);
            }
        }
    }

    // Description copied from AppsDatabase
    public void addListener(AppsDatabaseEventListener l)
    {
        CallerContext ctx = ccm.getCurrentContext();

        synchronized (lock)
        {
            // Listeners are maintained in-context
            Data data = getData(ctx, this);

            // Update listener/multicaster
            data.listeners = EventMulticaster.add(data.listeners, l);

            // Manage context/multicaster
            contexts = CallerContext.Multicaster.add(contexts, ctx);
        }
    }

    public void addCompositeDbAppsDatabaseEventListener(AppsDatabaseEventListener listener)
    {
        CallerContext ctx = ccm.getCurrentContext();

        synchronized (lock)
        {
            // Listeners are maintained in-context (using globalDb as the key)
            Data data = getData(ctx, globalDb);

            // Update listener/multicaster
            data.listeners = EventMulticaster.add(data.listeners, listener);

            // Manage context/multicaster
            compositeAppsDBContexts = CallerContext.Multicaster.add(compositeAppsDBContexts, ctx);
        }
    }

    // Description copied from AppsDatabase
    public AppProxy getAppProxy(AppID key)
    {
        synchronized (lock)
        {
            // if we're given nothing, return nothing
            // (preventing a NullPointerException below)
            if (key == null) return null;

            AppEntry entry = (AppEntry) database.get(key);
            return (entry == null || !isEntryVisibleToCurrentContext(entry)) ? null : getApp(entry);
        }
    }

    private boolean isEntryVisibleToCurrentContext(AppEntry entry)
    {
        return (entry.visibility != AppEntry.NON_VISIBLE)
                || SecurityUtil.hasPermission(MONAPP_SERVICE_MANAGER_PERMISSION);
    }

    /**
     * Package-private method that provides AppAttributes for any app in the
     * database, even those that can't be retrieved due to visibility set to
     * AppEntry.NON_VISIBLE
     * 
     * @param key
     *            the appID
     * @return app attributes
     */
    AppAttributes getAppAttributesIgnoringVisibility(AppID key)
    {
        if (key == null) return null;

        synchronized (lock)
        {
            AppEntry entry = (AppEntry) database.get(key);
            return (entry == null) ?
                null : (new AttributesImpl(entry, (OcapLocator)currentServiceDetails.getService().getLocator()));
        }
    }

    // Description copied from AppsDatabase
    public AppAttributes getAppAttributes(AppID key)
    {
        synchronized (lock)
        {
            // if we're given nothing, return nothing
            // (preventing a NullPointerException below)
            if (key == null) return null;

            AppEntry entry = (AppEntry) database.get(key);
            return (entry == null || !isEntryVisibleToCurrentContext(entry)) ?
                null : (new AttributesImpl(entry, (OcapLocator)currentServiceDetails.getService().getLocator()));
        }
    }

    // Description copied from AppsDatabase
    public Enumeration getAppIDs(AppsDatabaseFilter filter)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getAppIDs - filter: " + filter + ", known ids: " + knownAppIds);
        }

        Vector v = new Vector();
        // Get a copy of all applications that might satisfy requirements
        synchronized (lock)
        {
            // if we don't have a valid filter,
            // then return an empty enumeration
            if (filter == null) return new EmptyEnumeration();

            if (filter instanceof RunningApplicationsFilter)
            {
                for (Enumeration e = knownAppIds.elements(); e.hasMoreElements();)
                {
                    AppID id = (AppID) e.nextElement();
                    AppEntry entry = (AppEntry) database.get(id);
                    if (log.isDebugEnabled())
                    {
                        log.debug("getappids - runningapps filter - examining entry: " + entry + ", running: "
                                + isRunning(id) + ", filter accept: " + filter.accept(id));
                    }
                    if (entry != null && isEntryVisibleToCurrentContext(entry) && isRunning(id) && filter.accept(id))
                    {
                        v.addElement(id);
                    }
                }
            }
            else if (filter instanceof CurrentServiceFilter)
            {
                for (Enumeration e = database.elements(); e.hasMoreElements();)
                {
                    AppEntry entry = (AppEntry) e.nextElement();
                    if (log.isDebugEnabled())
                    {
                        log.debug("getappids - currentservicefilter - examining entry: " + entry + ", filter accept: "
                                + filter.accept(entry.id));
                    }

                    if (isEntryVisibleToCurrentContext(entry) && filter.accept(entry.id))
                    {
                        // exclude externally authorized apps
                        if (auth == null || auth.getAuthorization(entry.id) == -1)
                        {
                            v.addElement(entry.id);
                        }
                    }
                }
            }
        }

        return v.elements();
    }

    // Description copied from AppsDatabase
    public Enumeration getAppAttributes(AppsDatabaseFilter filter)
    {
        Vector v = new Vector();
        // Get a copy of all applications that might satisfy requirements
        synchronized (lock)
        {
            // if we don't have a valid filter,
            // then return an empty enumeration
            if (filter == null) return new EmptyEnumeration();

            if (filter instanceof RunningApplicationsFilter)
            {
                for (Enumeration e = knownAppIds.elements(); e.hasMoreElements();)
                {
                    AppID id = (AppID) e.nextElement();
                    AppEntry entry = (AppEntry) database.get(id);
                    if (entry != null && isEntryVisibleToCurrentContext(entry) && isRunning(id) && filter.accept(id))
                    {
                        v.addElement(new AttributesImpl(entry, (OcapLocator)currentServiceDetails.getService().getLocator()));
                    }
                }
            }
            else if (filter instanceof CurrentServiceFilter)
            {
                for (Enumeration e = database.elements(); e.hasMoreElements();)
                {
                    AppEntry entry = (AppEntry) e.nextElement();
                    if (isEntryVisibleToCurrentContext(entry) && filter.accept(entry.id))
                    {
                        // exclude externally authorized apps
                        if (auth == null || auth.getAuthorization(entry.id) == -1)
                        {
                            v.addElement(new AttributesImpl(entry, (OcapLocator)currentServiceDetails.getService().getLocator()));
                        }
                    }
                }
            }
        }

        return v.elements();
    }

    // Description copied from AppsDatabase
    public int size()
    {
        synchronized (lock)
        {
            return database.size();
        }
    }

    /* ======================== AppDomain ======================== */

    // Inherit description from AppDomain
    public void stop()
    {
        if (log.isDebugEnabled())
        {
            log.debug("stop() called...");
        }

        stateMachine.handleStop();
    }

    // Inherit description from AppDomain
    public void destroy()
    {
        if (log.isDebugEnabled())
        {
            log.debug("destroy() called...");
        }

        synchronized (bootLock)
        {
            if (bootCallbackListener != null)
            {
                ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
                sm.getServicesDatabase().removeBootProcessCallback(bootCallbackListener);
            }
        }

        stateMachine.handleDestroy();
    }

    // Inherit description from AppDomain
    public void select(ServiceDetails serviceDetails, InitialAutostartAppsStartedListener initialAutostartAppsStartedListener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("select(" + serviceDetails + ") called...");
        }
        this.initialAutostartAppsStartedListener = initialAutostartAppsStartedListener;
        stateMachine.handleSelect(serviceDetails);
    }

    // Inherit description from AppDomain
    public void preSelect(ServiceDetails serviceDetails)
    {
        if (log.isDebugEnabled())
        {
            log.debug("preSelect(" + serviceDetails + ") called...");
        }

        stateMachine.handlePreSelect(serviceDetails);
    }

    // Inherit description from AppDomain
    public void stopBoundApps()
    {
        if (log.isDebugEnabled())
        {
            log.debug("stopBoundApps()");
        }

        stateMachine.handleStopBoundApps();
    }

    /**
     * Returns an array of <code>ServiceContentHandler</code>s for the set of
     * currently running applications, if this <code>AppDomain</code> is
     * currently <i>selected</i>.
     */
    public ServiceContentHandler[] getServiceContentHandlers()
    {
        /**
         * Implementation of <code>ServiceContentHandler</code> that represents
         * a <i>running</i> application.
         * 
         * The implementation's{@link #getServiceContentHandlers()} method
         * returns an array containing a single locator which represents the
         * application as service content. Since <code>OcapLocator</code> is
         * incapable of representing such a custom locator is returned.
         * <p>
         * GEM 11.7.6 allows another locator type to be returned as it only
         * requires the defined type (in OCAP's case <code>OcapLocator</code>
         * "where the locator returned can be represented by the locator syntax
         * described by the terminal specification."
         */
        class SCH implements ServiceContentHandler
        {
            SCH(AppID id, String svc)
            {
                // loc = "app:[//<svc>]/<oid>/<aid>"
                loc = new Locator("app:" + svc + "/" + Integer.toHexString(id.getOID()) + "/"
                        + Integer.toHexString(id.getAID()))
                { /* empty */
                };
            }

            public javax.tv.locator.Locator[] getServiceContentLocators()
            {
                return new javax.tv.locator.Locator[] { loc };
            }

            public String toString()
            {
                return loc.toExternalForm();
            }

            private Locator loc;
        }

        Vector v;

        // Lock stateMachine then appsDb lock to ensure consistent state.
        synchronized (stateMachine)
        {
            // First determine if currently selected or not
            if (stateMachine.getState() != State.SELECTED) return new ServiceContentHandler[0];

            javax.tv.locator.Locator loc = currentServiceDetails.getLocator();
            String service;
            // Extract just the inner service specification part of OcapLocator
            if (loc instanceof OcapLocator)
            {
                // Remove the "ocap:" part.
                service = loc.toExternalForm().substring("ocap:".length());

                // Remove any trailing "/<path_elements>" (not really expected)
                int index = service.indexOf('/', 2);
                if (index >= 0) service = service.substring(0, index);
            }
            else
                service = ""; // Dunno how to represent anything else...

            // Get a copy of all applications that might satisfy requirements
            v = new Vector();
            synchronized (lock)
            {
                for (Enumeration e = knownAppIds.elements(); e.hasMoreElements();)
                {
                    AppID id = (AppID) e.nextElement();
                    if (isRunning(id))
                    {
                        // Add SCH to vector...
                        v.addElement(new SCH(id, service));
                    }
                }
            }
        }

        // Finally copy SCHs into array to return
        ServiceContentHandler[] array = new ServiceContentHandler[v.size()];
        v.copyInto(array);
        return array;
    }

    /**
     * Is notified of new AIT signalling.
     */
    public void signallingReceived(SignallingEvent event)
    {
        stateMachine.handleAitSignalling(event);
    }

    /**
     * Is notified of new Abstract Service information (based upon XAIT
     * signalling).
     */
    public void serviceUpdate(AbstractServiceEntry service)
    {
        if (log.isDebugEnabled())
        {
            log.debug("serviceUpdate: " + service);
        }
        stateMachine.handleXaitSignalling(service);
    }

    /* =======================Private Impl===================== */

    /**
     * Launches the given application, if it is not already running.
     * <p>
     * This method has <i>default</i> (i.e., <i>package</i>) access in order to
     * support unit testing.
     * 
     * @param entry
     *            the AppEntry for the application to start
     */
    void startApp(AppEntry entry)
    {
        synchronized (lock)
        {
            // See if app already exists
            // Create app if necessary
            Application app = getApp(entry);

            // If app is anything other than NOT_LOADED, return
            if (app.getState() == AppProxy.NOT_LOADED)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Autostarting app: " + entry.id);
                }
                if (!processedInitialAutostart)
                {
                    initialAutostartApps.put(app.getAppID(), app);
                    app.addAppStateChangeEventListener(initialAutostartAppStateChangeEventListener);
                }
                app.autostart();
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Not launching app: " + entry.id + ", state=" + app.getState());
                }
        }
    }
    }

    /**
     * Stops the given application (if it exists).
     * <p>
     * Synchronization is handled by the caller.
     * <P>
     * This method <i>default</i> (i.e., <i>package</i>) access in order to
     * support unit testing.
     * 
     * @param id
     *            the AppID for the app to stop
     * @param forced
     *            entry.controlCode == KILL
     */
    void stopApp(AppID id, boolean forced)
    {
        // Look up app by id
        Application app = (Application) knownApps.get(id);

        // If found, stop it!
        if (app != null)
        {
            app.stop(forced);
            knownApps.remove(id);
            knownAppIds.removeElement(id);
        }
    }

    /**
     * Dispose of the given application (if it exists).
     * <p>
     * Synchronization is handled by the caller.
     * <P>
     * This method <i>default</i> (i.e., <i>package</i>) access in order to
     * support unit testing.
     * 
     * @param id
     *            the AppID for the app to stop
     * 
     * @see #disposeApp(Application, AppID)
     */
    void disposeApp(AppID id)
    {
        // Look up app by id
        Application app = (Application) knownApps.get(id);

        // If found, stop it!
        if (app != null)
        {
            disposeApp(app, id);
        }
    }

    /**
     * Dispose of the given application.
     * <p>
     * Synchronization is handled by the caller.
     * <p>
     * This is only done when the app is being removed from the database
     * altogether. The app is disposed of and removed from the set of
     * <i>known</i> applications. Removal from the database is performed by the
     * caller.
     * <P>
     * This method <i>default</i> (i.e., <i>package</i>) access in order to
     * support unit testing.
     * 
     * @param app
     *            the <code>Application</code> to dispose of
     */
    void disposeApp(Application app, AppID id)
    {
        // Stop this app unconditionally and dispose of it
        app.dispose();

        // Forget the application immediately
        knownApps.remove(id);
        knownAppIds.removeElement(id);
    }

    /**
     * Sometimes autostart apps can fail to launch or be stored due to a
     * transient communication error with a remote filesystem (such as HTTP). In
     * that situation, we will instruct this app to be re-autostarted upon the
     * next receipt of this app's signalling
     * 
     * @param entry
     *            the application that should be re-autostarted
     */
    void reAutoStartApp(AppEntry entry)
    {
        // Sanity check. App must be signaled as auto-start
        if (entry.controlCode != OcapAppAttributes.AUTOSTART) return;

        synchronized (lock)
        {
            AppEntry dbEntry = (AppEntry) database.get(entry.id);

            // New signalling has removed this app from the database
            // or has modified this app so re-autostart no longer applies
            if (dbEntry == null || appModified(entry, dbEntry))
                return;

            // Add this app to our list of apps to be re-autostartd
            reAutoStartApps.put(entry.id, entry);
        }

        if (log.isDebugEnabled())
        {
            log.debug("Re-autostarting app (" + entry
                    + ") due to potentially transient file system communication error");
        }

        // Tell the signalling manager to re-signal the X/AIT that signalled
        // this app. We will attempt to re-autostart the app once we receive
        // the repeated signalling
        sm.resignal(entry.signalling);
    }

    /**
     * Retrieves the currently known instance of this application. If no known
     * application exists, then one is created. If one could not be created then
     * <code>null</code> is returned.
     * 
     * @return an existing or newly created application
     */
    private Application getApp(AppEntry entry)
    {
        AppID id = entry.id;
        Application app = (Application) knownApps.get(id);

        if (app == null)
        {
            app = createApp(entry);
        }
        return app;
    }

    /**
     * Creates a new application if one had not been previously created.
     * 
     * @return a newly created application
     */
    private Application createApp(AppEntry entry)
    {
        // Create the app
        Application app = new XletApp(entry, this);

        if (TRACKING)
        {
            RefTracker.getInstance().track(app);
        }

        // Remember it
        knownApps.put(entry.id, app);
        knownAppIds.remove(entry.id);
        knownAppIds.addElement(entry.id);

        if (log.isDebugEnabled())
        {
            log.debug("Created App: " + entry.id + " " + app);
        }

        return app;
    }

    /**
     * Returns whether the given appID is running or not.
     * 
     * @param id
     * @return <code>true</code> if the given application is not in the
     *         <code>NOT_LOADED</code> or <code>DESTROYED</code> states
     */
    private boolean isRunning(AppID id)
    {
        AppProxy proxy = globalDb.getAppProxy(id);
        int currState = proxy.getState();
        return proxy != null && currState != AppProxy.NOT_LOADED && currState != AppProxy.DESTROYED;
    }

    /**
     * Access this object's global data object associated with given context. If
     * none is assigned, then one is created.
     * <p>
     * Synchronizes on the internal object {@link #lock}.
     * 
     * @param ctx
     *            the context to access
     * @param key
     *            the key to use to look up the CallbackData in the context
     * @return the <code>Data</code> object
     */
    private Data getData(CallerContext ctx, Object key)
    {
        synchronized (lock)
        {
            Data data = (Data) ctx.getCallbackData(key);
            if (data == null)
            {
                data = new Data();
                ctx.addCallbackData(data, key);
            }
            return data;
        }
    }

    /**
     * Cause the applications database to <i>forget</i> all listeners associated
     * with this <code>CallerContext</code>. This is done simply by setting the
     * reference to <code>null</code> and letting the garbage collector take
     * care of the rest.
     * <p>
     * This is <i>package private</i> for testing purposes.
     * 
     * @param c
     *            the <code>CallerContext</code> to forget
     */
    private void forgetListeners(CallerContext c)
    {
        synchronized (lock)
        {
            // Simply forget the given c
            // No harm done if never added
            c.removeCallbackData(this);
            contexts = CallerContext.Multicaster.remove(contexts, c);
            compositeAppsDBContexts = CallerContext.Multicaster.remove(compositeAppsDBContexts, c);
        }
    }

    private void notifyCompositeAppsDBListeners(AppsDatabaseEvent e)
    {
        notifyListeners(e, compositeAppsDBContexts, globalDb);
    }

    private void notifyAppDomainListeners(AppsDatabaseEvent e)
    {
        notifyListeners(e, contexts, this);
    }

    /**
     * Called when we fail to acquire an AIT for a broadcast service. This will
     * clean up any non-service-bound bound apps
     */
    public void timerWentOff(TVTimerWentOffEvent e)
    {
        synchronized (lock)
        {
            // AIT Timer went off -- No AIT present in this broadcast service
            if (aitTimerSpec != null)
            {
                stopApps(false);
                aitTimerSpec = null;
            }
        }
    }

    /**
     * Notify all listeners that the applications database has changed.
     * 
     * @param e
     *            the <code>AppsDatabaseEvent</code> describing the database
     *            change
     */
    private void notifyListeners(final AppsDatabaseEvent e, CallerContext contextsToNotify, final Object key)
    {
        // notify listeners of event
        final Notifier n;
        switch (e.getEventId())
        {
            case AppsDatabaseEvent.NEW_DATABASE:
                n = Notifier.NEW_DATABASE;
                break;
            case AppsDatabaseEvent.APP_CHANGED:
                n = Notifier.CHANGED;
                break;
            case AppsDatabaseEvent.APP_ADDED:
                n = Notifier.ADDED;
                break;
            case AppsDatabaseEvent.APP_DELETED:
                n = Notifier.DELETED;
                break;
            default:
                return;
        }

        if (contextsToNotify != null)
        {
            // Execute listener callbacks within context-specific threads
            contextsToNotify.runInContext(new Runnable()
            {
                public void run()
                {
                    CallerContext ctx = ccm.getCurrentContext();
                    Data data = (Data) ctx.getCallbackData(key);

                    if (data != null && data.listeners != null)
                    {
                        n.notify(data.listeners, e);
                    }
                }
            });
        }
    }

    /**
     * Stops applications executing within this <code>AppDomain</code>. This
     * method either stops <i>all</i> applications that are currently running,
     * or only those that are service bound.
     * <p>
     * Note that all <i>unbound</i> applications are service bound. It is
     * assumed that the <code>OcapAppAttributes</code> implementation knows
     * this.
     * <p>
     * In both cases the applications database entry should be removed.
     * 
     * @param bound
     *            if <code>true</code> then only service bound applications
     *            should be stopped; if <code>false</code> then ALL apps should
     *            be stopped
     */
    private void stopApps(boolean bound)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Stopping " + (bound ? "bound" : "all") + " apps for service details:" + currentServiceDetails);
        }

        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("There are " + knownAppIds.size() + " apps");
            }
            // Iterate over running apps
            for (int i = 0; i < knownAppIds.size();
            /* i is incremented only if element isn't removed */)
            {
                AppID id = (AppID) knownAppIds.elementAt(i);
                Application app = (Application) knownApps.get(id);
                AppEntry entry = (AppEntry) database.get(id);

                if (!bound || entry.serviceBound)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Stopping " + id);
                    }
                    // Stop this app unconditionally and dispose of it
                    // Will remove AppID from knownAppIds...
                    disposeApp(app, id);

                    // Remove the database entry
                    database.remove(id);
                }
                else
                {
                    ++i;
                }
            }
        }
    }

    /**
     * Updates the applications database based upon an in-band AIT signalling
     * event.
     * 
     * @param event
     *            SignallingEvent
     */
    private void updateAppsDB(SignallingEvent event)
    {
        if (log.isDebugEnabled())
        {
            log.debug("updateAppsDB - signallingEvent: " + event);
        }
        
        AppEntry[] apps = (AppEntry[])event.getSignalling().getApps().clone();
        
        // update bound apps
        synchronized (lock)
        {
            // We got an AIT, so deschedule our timer
            if (aitTimerSpec != null)
            {
                aitTimer.deschedule(aitTimerSpec);
                aitTimerSpec = null;
            }

            // New database
            Hashtable newDb = new Hashtable();
            Vector autoStart = new Vector();

            // Acquire signalling

            auth = new ExternalAuthorization(event.getSignalling());

            // Sort new and old apps based on AppID
            Arrays.sort(apps, new AppIDCompare());
            
            AppEntry[] oldApps = new AppEntry[database.size()];
            int i = 0;
            for (Enumeration e = database.elements(); e.hasMoreElements();)
            {
                oldApps[i++] = (AppEntry)e.nextElement();
            }
            Arrays.sort(oldApps, new AppIDCompare());

            // Iterate over current set of applications
            // and new set of applications
            int curIdx = 0;
            int newIdx = 0;
            final int curSize = oldApps.length;
            final int newSize = apps.length;

            while (curIdx < curSize && newIdx < newSize)
            {
                AppEntry oldEntry = oldApps[curIdx];
                AppEntry newApp = apps[newIdx];

                // Compare AppIDs
                int cmp = (new AppIDCompare()).compare(oldEntry, newApp);

                // Add app
                if (cmp > 0)
                {
                    // newApp represents a new application
                    // Add to database and AUTOSTART if necessary
                    if (log.isDebugEnabled())
                    {
                        log.debug("Add new app: " + newApp);
                    }
                    addNewApp(newDb, autoStart, newApp);

                    // Advance to next new app
                    ++newIdx;
                }
                // Delete app
                else if (cmp < 0)
                {
                    // oldApp has been deleted
                    // kill if running and not externally authorized
                    if (log.isDebugEnabled())
                    {
                        log.debug("Delete old app not in new app list: " + oldEntry);
                    }
                    delOldApp(newDb, auth, oldEntry);

                    // TODO: bug 1503: keep externally authorized appentry
                    // (separate) if running!

                    // Advance to next old app
                    ++curIdx;
                }
                // Update app
                else
                {
                    boolean modified = appModified(oldEntry, newApp);
                    // was old app service-bound? If so, delete
                    if (!modified)
                    {
                        // Has this app been indicated for re-autostart? If the
                        // app is
                        // running, but has not yet reached the active state,
                        // stop it
                        boolean reAutoStart = (reAutoStartApps.remove(oldEntry.id) != null);
                        if (reAutoStart && isRunning(oldEntry.id))
                        {
                            stopApp(oldEntry.id, true);
                        }

                        // Not modified, stick with the old information
                        if (log.isDebugEnabled())
                        {
                            log.debug("Add unmodified app: " + oldEntry);
                        }
                        newDb.put(oldEntry.id, oldEntry);

                        // app was already in db as autostart, but we've either
                        // performed a new select
                        // or it needs to be re-autostarted.. autostart it
                        if (OcapAppAttributes.AUTOSTART == oldEntry.controlCode && (needSignalling || reAutoStart))
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("Add unmodified app to autostart: " + oldEntry);
                            }
                            autoStart.addElement(oldEntry);
                        }
                    }
                    else
                    {
                        boolean forced = false;

                        switch (newApp.controlCode)
                        {
                            case OcapAppAttributes.AUTOSTART:
                                // Not previously autostart!
                                // We will launch it after updating the DB

                                if (log.isDebugEnabled())
                                {
                                    log.debug("Add modified app to autostart: " + newApp);
                                }
                                autoStart.addElement(newApp);
                                break;
                            case OcapAppAttributes.PRESENT:
                            case OcapAppAttributes.REMOTE:
                                // Do nothing
                                break;
                            case OcapAppAttributes.KILL:
                                forced = true;
                                // FALLTHROUGH
                            case OcapAppAttributes.DESTROY:
                                if (log.isDebugEnabled())
                                {
                                    log.debug("Stop modified app (kill or destroy) - forced: " + forced + ", "
                                            + newApp);
                                }
                                stopApp(newApp.id, forced);
                                break;
                        }

                        // Update database with new entry
                        if (log.isDebugEnabled())
                        {
                            log.debug("Add modified app: " + newApp);
                        }
                        newDb.put(newApp.id, newApp);

                        // Generate APP_CHANGED event!
                        if (!needSignalling)
                        {
                            AppsDatabaseEvent appChangedEvent = new AppsDatabaseEvent(AppsDatabaseEvent.APP_CHANGED,
                                    newApp.id, this);
                            // don't use isEntryVisibleToCurrentContext helper
                            // here, only notify domain listeners if entry is
                            // visible
                            // always notify composite apps db listeners
                            if (newApp.visibility != AppEntry.NON_VISIBLE)
                            {
                                notifyAppDomainListeners(appChangedEvent);
                            }
                            notifyCompositeAppsDBListeners(appChangedEvent);
                        }
                    }

                    // Advance to next old app
                    ++curIdx;
                    // Advance to next new app
                    ++newIdx;
                }
            }
            // old apps not referenced
            for (; curIdx < curSize; ++curIdx)
            {
                AppEntry oldEntry = oldApps[curIdx];
                if (log.isDebugEnabled())
                {
                    log.debug("Delete no longer referenced old app: " + oldEntry);
                }
                delOldApp(newDb, auth, oldEntry);
            }
            // more new apps
            AppID lastId = null;
            for (; newIdx < newSize; ++newIdx)
            {
                if (!apps[newIdx].id.equals(lastId)) // Only add first entry for app
                {
                    AppEntry newEntry = apps[newIdx];
                    if (log.isDebugEnabled())
                    {
                        log.debug("Add new app: " + newEntry);
                    }
                    addNewApp(newDb, autoStart, newEntry);
                    lastId = newEntry.id;
                }
            }

            if (needSignalling)
            {
                AppsDatabaseEvent newDatabaseEvent = new AppsDatabaseEvent(AppsDatabaseEvent.NEW_DATABASE, null, this);
                notifyAppDomainListeners(newDatabaseEvent);
                notifyCompositeAppsDBListeners(newDatabaseEvent);
            }

            // Update database
            database = newDb;
            needSignalling = false;

            // Launch auto-start apps
            if (log.isDebugEnabled())
            {
                log.debug("autostart entries: " + autoStart);
            }
            for (Enumeration e = autoStart.elements(); e.hasMoreElements();)
            {
                AppEntry entry = (AppEntry) e.nextElement();
                startApp(entry);
            }
        }
    }

    /**
     * Compares two AppEntrys representing the same app to see if it changed.
     * 
     * @param oldApp
     * @param newApp
     * @return
     */
    private boolean appModified(AppEntry oldApp, AppEntry newApp)
    {
        if (oldApp.controlCode != newApp.controlCode || oldApp.serviceBound != newApp.serviceBound
                || oldApp.priority != newApp.priority || oldApp.visibility != newApp.visibility) return true;

        AttributesImpl oldAttr =
            new AttributesImpl(oldApp, (OcapLocator)currentServiceDetails.getService().getLocator());
        AttributesImpl newAttr =
            new AttributesImpl(newApp, (OcapLocator)currentServiceDetails.getService().getLocator());
        // Check the profiles
        String[] oldProf = oldAttr.getProfiles();
        String[] newProf = newAttr.getProfiles();
        if (oldProf.length != newProf.length) return true;
        Arrays.sort(oldProf);
        Arrays.sort(newProf);
        for (int i = 0; i < oldProf.length; i++)
        {
            if (!oldProf[i].equals(newProf[i])) return true;
            // Check the versions for each profile
            try
            {
                int[] oldVer = oldAttr.getVersions(oldProf[i]);
                int[] newVer = newAttr.getVersions(newProf[i]);
                if (!Arrays.equals(oldVer, newVer)) return true;
            }
            catch (IllegalProfileParameterException ex)
            {
                // This shouldn't happen since we're using the profile directly
                // from the AppAttributes itself
                return true;
            }
        }
        // Finally, we'll check the names
        String[][] oldNames = oldAttr.getNames();
        String[][] newNames = newAttr.getNames();
        if (oldNames.length != newNames.length) return true;
        boolean diff = false;
        for (int i = 0; i < oldNames.length; i++)
        {
            boolean found = false;
            for (int j = 0; j < newNames.length; j++)
            {
                // name[i][0] is the language code
                // name[i][1] is the name
                // Safe to use Arrays.equals() here because the order should be
                // identical
                if (Arrays.equals(oldNames[i], newNames[j]))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                diff = true;
                break;
            }
        }

        return diff;
    }

    /**
     * Modifies the priority of externaly authorized applications that remain
     * from the previous service. This method does nothing if the given app is
     * not running.
     * <p>
     * <i><em>Currently unimplemented</em></i>
     * 
     * @param id
     *            the AppID for the app
     * @param newPriority
     *            the new priority for the application
     */
    private void modifyPriority(AppID id, int newPriority)
    {
        // Find running app
        synchronized (lock)
        {
            if (isRunning(id))
            {
                Application app = (Application) knownApps.get(id);
                app.setPriority(newPriority);
            }
        }
    }

    /**
     * Determines whether the specified application is signed or not.
     * <p>
     * <i><em>Currently unimplemented</em></i>
     * 
     * @param id
     *            specifies the application to query for
     * @return <code>true</code> if the currently known application is signed
     */
    private boolean isSigned(AppID id)
    {
        int aid = id.getAID();
        return (aid >= 0x4000 && aid < 0x8000);
    }

    /**
     * Updates the applications database based upon out-of-band XAIT signalling.
     * <code>OcapAppAttributes</code> objects are returned directly from the
     * <i>services database</i>.
     * <p>
     * The <i>autoStartEnabled</i> and <i>autoStartNewOnly</i> are used to
     * control if and how application autostart is performed. It is expected
     * that this method will be invoked as follows:
     * <table border>
     * <tr>
     * <th>Caller</th>
     * <th><code>autoStartEnabled</code></th>
     * <th><code>autoStartNewOnly</code></th>
     * </tr>
     * <tr>
     * <td>{@link State#handlePreSelect}</td>
     * <td><code>false</code></td>
     * <td>NA</td>
     * </tr>
     * <tr>
     * <td>{@link State#handleSelect}</td>
     * <td><code>true</code></td>
     * <td><code>true</code> unless <code>PRE_SELECTED</code></td>
     * </tr>
     * <tr>
     * <td>{@link State#handleXaitSignalling}</td>
     * <td><code>true</code></td>
     * <td><code>true</code></td>
     * </tr>
     * </table>
     * 
     * @param service
     *            the most recent <code>AbstractServiceEntry</code> for the
     *            service
     * @param autoStartEnabled
     *            if <code>true</code> then autostart is enabled; if
     *            <code>false</code> then autostart is disabled
     * @param autoStartNewOnly
     *            if <code>true</code> then only newly autostart apps are
     *            autostarted; if <code>false</code> then all currently-signaled
     *            autostart apps are autostarted
     */
    private void updateAppsDB(AbstractServiceEntry service, boolean autoStartEnabled, boolean autoStartNewOnly)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Update apps db - service entry: " + service);
        }

        boolean autoStartAll = autoStartEnabled && !autoStartNewOnly;
        boolean notifyInitialAutostartApps = false;
        // Update unbound apps
        synchronized (lock)
        {
            // New database
            Hashtable newDb = new Hashtable();
            Vector autoStart = autoStartEnabled ? new Vector() : null;
            boolean stopAbstractService = true;

            // Sort set of new applications
            final int curSize = database.size();
            final int newSize = (service == null || service.apps == null) ? 0 : service.apps.size();

            XAppEntry[] apps = new XAppEntry[newSize];
            if (newSize > 0)
            {
                service.apps.copyInto(apps);
                Arrays.sort(apps, new AppIDCompare());
            }

            XAppEntry[] oldApps = new XAppEntry[database.size()];
            int i = 0;
            for (Enumeration e = database.elements(); e.hasMoreElements();)
            {
                oldApps[i++] = (XAppEntry)e.nextElement();
            }
            Arrays.sort(oldApps, new AppIDCompare());

            // Iterate over current set of applications
            // and new set of applications
            int curIdx = 0;
            // if service is marked for removal,
            // default newIdx to number of apps - that will bypass the
            // following while-loop and result in all old apps being deleted
            int newIdx = service.markedForRemoval ? newSize : 0;

            while (curIdx < curSize && newIdx < newSize)
            {
                XAppEntry oldEntry = oldApps[curIdx];
                XAppEntry newApp = apps[newIdx];

                if (isAutoStartOrPresent(newApp))
                {
                    stopAbstractService = false;
                }

                // Compare AppIDs
                int cmp = (new AppIDCompare()).compare(oldEntry, newApp);

                // Add app
                if (cmp > 0)
                {
                    // It is possible that this app is a new version of an app
                    // currently running in a different service.  This will only
                    // happen if the old version is no longer signaled.  Otherwise
                    // this app would have been placed in the newVersion field
                    // of the original app's XAppEntry
                    AppEntry running;
                    if ((running = am.getRunningVersion(newApp.id)) != null)
                    {
                        ((XAppEntry)running).newVersion = newApp;
                    }
                    else
                    {
                        AppVersionManager.clearApps(newApp.id);
                        addNewApp(newDb, autoStart, newApp);
                    }
                
                    storeApp(newApp);
                    
                    // Always store new versions
                    while (newApp.newVersion != null)
                    {
                        storeApp(newApp.newVersion);
                        newApp = newApp.newVersion;
                    }
                    
                    // Advance to next new app
                    ++newIdx;
                }
                // Delete app
                else if (cmp < 0)
                {
                    // Even though this app is no longer signaled, if it is
                    // running and has a new version signaled, we do not kill
                    // the old version
                    XAppEntry ae = (XAppEntry)am.getRunningVersion(oldEntry.id);
                    if (ae != null && ae.newVersion != null)
                    {
                        // Store any new versions
                        XAppEntry newVersion = ae.newVersion;
                        while (newVersion != null)
                        {
                            storeApp(newVersion);
                            newVersion = newVersion.newVersion;
                        }
                        
                        stopAbstractService = false;
                        newDb.put(oldEntry.id, oldEntry);
                    }
                    else
                    {
                        delOldApp(newDb, null, oldEntry);
                    }
                        
                    // Advance to next old app
                    ++curIdx;
                }
                // Update app
                else
                {
                    boolean forceControlCodeCheck = false;
                    
                    // Note: expect ServicesDatabase to prevent modification
                    // using differing sources
                    // Per OCAP 10.2.2.3.2
                    
                    AppID id = oldEntry.id;
                    AppVersionManager.clearApps(newApp.id);
                    
                    boolean modified = appModified(oldEntry, newApp);
                    boolean newVersionsModified = newVersionsModified(oldEntry, newApp);
                    boolean diffVersion = newApp.version != oldEntry.version;
                    if (!modified && !diffVersion && !newVersionsModified)
                    {
                        // Has this app been indicated for re-autostart? If the app is
                        // running, but has not yet reached the active state, stop it
                        boolean reAutoStart = (reAutoStartApps.remove(id) != null);
                        if (reAutoStart && isRunning(id))
                        {
                            stopApp(id, true);
                        }

                        // Not modified, stick with the old information
                        if (log.isDebugEnabled())
                        {
                            log.debug("Old app: " + oldEntry);
                        }
                        newDb.put(id, oldEntry);

                        // AutoStart as if "newly AUTOSTART" or re-autostart
                        if (oldEntry.controlCode == OcapAppAttributes.AUTOSTART &&
                            (autoStartAll || reAutoStart))
                        {
                            autoStart.addElement(oldEntry);
                        }
                    }
                    else
                    {
                        // Update database with new information

                        // Propogate MonApp designation
                        // may have changed since made initial monApp
                        if (oldEntry.isMonitorApp && newApp.priority == 255 &&
                            newApp.controlCode == OcapAppAttributes.AUTOSTART &&
                            service.autoSelect)
                        {
                            newApp.isMonitorApp = oldEntry.isMonitorApp;
                        }

                        if (diffVersion)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("New app version signaled: " + newApp);
                            }
                            
                            if (isRunning(id))
                            {
                                // App is running, so just keeep this as a new version
                                oldEntry.newVersion = newApp;
                                newDb.put(id, oldEntry);
                            }
                            else
                            {
                                knownApps.remove(id);
                                newDb.put(id, newApp);
                                forceControlCodeCheck = true;
                            }
                            
                            storeApp(newApp);
                        }
                        else if (newVersionsModified)
                        {
                            // Always store new/modified versions
                            XAppEntry newVersion = newApp.newVersion;
                            while (newVersion != null)
                            {
                                storeApp(newVersion);
                                newVersion = newVersion.newVersion;
                            }
                            
                            newDb.put(id, newApp);
                        }
                        else
                        {
                            newDb.put(id, newApp);
                        }

                        // Generate APP_MODIFIED event!
                        if (!needSignalling)
                        {
                            AppsDatabaseEvent appChangedEvent =
                                new AppsDatabaseEvent(AppsDatabaseEvent.APP_CHANGED, id, this);
                            // don't use isEntryVisibleToCurrentContext helper here,
                            // only notify domain listeners if entry is visible
                            // always notify composite apps db listeners
                            if (newApp.visibility != AppEntry.NON_VISIBLE)
                            {
                                notifyAppDomainListeners(appChangedEvent);
                            }
                            notifyCompositeAppsDBListeners(appChangedEvent);
                        }
                    }
                    
                    // If our new database entry has a different version or a
                    // different control code from the previous entry, then apply
                    // control code update
                    XAppEntry ccCheck = (XAppEntry)newDb.get(id);
                    if (forceControlCodeCheck ||
                        ccCheck.version != oldEntry.version ||
                        ccCheck.controlCode != oldEntry.controlCode)
                    {
                        // Handle control code change
                        boolean forced = false;
                        switch (ccCheck.controlCode)
                        {
                            case OcapAppAttributes.AUTOSTART:
                                // Not previously autostart!
                                // Will start *after* database is updated
                                if (autoStart != null) // Skip if disabled
                                    autoStart.addElement(ccCheck);
                                break;
                            case OcapAppAttributes.PRESENT:
                            case OcapAppAttributes.REMOTE:
                                // Do nothing
                                break;
                            case OcapAppAttributes.KILL:
                                forced = true;
                                // FALLTHROUGH
                            case OcapAppAttributes.DESTROY:
                                stopApp(id, forced);
                                stopAbstractService = false;
                                break;
                        }
                    }
                    
                    ++curIdx; 
                    ++newIdx;
                }
            }

            // old apps not referenced
            for (; curIdx < curSize; ++curIdx)
            {
                XAppEntry oldEntry = oldApps[curIdx];
                // Even though this app is no longer signaled, if it is
                // running and has a new version signaled, we do not kill
                // the old version
                XAppEntry ae = (XAppEntry)am.getRunningVersion(oldEntry.id);
                if (ae != null && ae.newVersion != null)
                {
                    // Store any new versions
                    XAppEntry newVersion = ae.newVersion;
                    while (newVersion != null)
                    {
                        storeApp(newVersion);
                        newVersion = newVersion.newVersion;
                    }
                    
                    stopAbstractService = false;
                    newDb.put(oldEntry.id, oldEntry);
                }
                else
                {
                    delOldApp(newDb, null, oldEntry);
                }
            }

            // more new apps
            while (newIdx < newSize)
            {
                XAppEntry newApp = apps[newIdx];
                
                // It is possible that this app is a new version of an app
                // currently running in a different service.  This will only
                // happen if the old version is no longer signaled.  Otherwise
                // this app would have been placed in the newVersion field
                // of the original app's XAppEntry
                AppEntry running;
                if ((running = am.getRunningVersion(newApp.id)) != null)
                {
                    ((XAppEntry)running).newVersion = newApp;
                }
                else
                {
                    AppVersionManager.clearApps(newApp.id);
                    addNewApp(newDb, autoStart, newApp);
                    stopAbstractService = false;
                }
                
                storeApp(newApp);
                
                // Always store new versions
                while (newApp.newVersion != null)
                {
                    storeApp(newApp.newVersion);
                    newApp = newApp.newVersion;
                }                    
                
                newIdx++;
            }
                    
            if (needSignalling)
            {
                AppsDatabaseEvent newDatabaseEvent =
                    new AppsDatabaseEvent(AppsDatabaseEvent.NEW_DATABASE, null, this);
                notifyAppDomainListeners(newDatabaseEvent);
                notifyCompositeAppsDBListeners(newDatabaseEvent);
            }

            // Update database
            database = newDb;
            needSignalling = false;

            // Launch auto-start apps (if enabled)
            if (autoStart != null)
            {
                synchronized (bootLock)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Launching " + (autoStartNewOnly ? "newly" : "all") + " AUTO_START apps...");
                    }
                    for (Enumeration e = autoStart.elements(); e.hasMoreElements();)
                    {
                        XAppEntry entry = (XAppEntry)e.nextElement();

                        if (!inBootupPhase || (inBootupPhase && (entry.isMonitorApp)))
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("Launching " + entry);
                            }
                            startApp(entry);
                        }
                        else
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info("Deferring " + inBootupPhase + " app: " + entry);
                            }
                            if (inBootupPhase)
                                deferredAutoStartApps.add(entry);

                        }
                    }
                }
            }
            
            //only register listeners one time through autostart list
            if (!processedInitialAutostart)
            {
                processedInitialAutostart = true;
                //if no entries, notify
                if (initialAutostartApps.isEmpty())
                {
                    notifyInitialAutostartApps = true;
                }
            }

            // If we don't have any 
            if (stopAbstractService &&
                !service.hasNewAppVersions &&
                !AppVersionManager.checkForNewApps(getServiceID()))
            {
                if (serviceContext instanceof org.cablelabs.impl.service.ServiceContextExt)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("No AUTOSTART|PRESENT apps - stopping presentation");
                    }
                    (( org.cablelabs.impl.service.ServiceContextExt) serviceContext).stopAbstractService();
                }
            }
        }
        if (notifyInitialAutostartApps)
        {
            if (initialAutostartAppsStartedListener != null)
            {
                initialAutostartAppsStartedListener.initialAutostartAppsStarted();
                //one-time notification, release reference
                initialAutostartAppsStartedListener = null;
            }
        }
    }
    
    private int getServiceID()
    {
        return (currentServiceDetails == null) ? -1 :
                ((OcapLocator)currentServiceDetails.getService().getLocator()).getSourceID();
    }
    
    private boolean isAutoStartOrPresent(AppEntry app)
    {
        return OcapAppAttributes.AUTOSTART == app.controlCode ||
               OcapAppAttributes.PRESENT == app.controlCode;
    }

    private boolean newVersionsModified(XAppEntry oldApp, XAppEntry newApp)
    {
        XAppEntry oldNewVersion = oldApp.newVersion;
        XAppEntry newNewVersion = newApp.newVersion;
        while (oldNewVersion != null && newNewVersion != null)
        {
            if (appModified(oldNewVersion,newNewVersion))
            {
                return true;
            }
            oldNewVersion = oldNewVersion.newVersion;
            newNewVersion = newNewVersion.newVersion;
        }
        if (oldNewVersion != null || newNewVersion != null)
        {
            return true;
        }
        
        return false;
    }
    
    // Called to background store an AUTOSTART app that has been signaled with
    // a new version but is also currently running. AUTOSTART apps are not
    // stored
    // by the ServicesDatabase due to the fact that storage takes place just
    // before
    // launch. In this case, the app will not be stored since it can not start
    // until its previous version has been voluntarily stopped.
    private void storeApp(XAppEntry entry)
    {
        // This app may not have been stored by the ServicesDatabase so
        // make sure it gets stored now
        if (entry.storagePriority != 0)
        {
            AppEntry.TransportProtocol tp = entry.transportProtocols[0];
            asm.backgroundStoreApp(entry, tp);
        }
    }

    /**
     * Method used by
     * {@link #updateAppsDB(AbstractServiceEntry, boolean, boolean)} and
     * {@link #updateAppsDB(SignallingEvent)} to remove an old app from the
     * database. At this point it is known that the app is to be deleted. Takes
     * care of disposing of the app (i.e., KILLing it) and notifying listeners
     * of the change. If <i>auth</i> is included, it is consulted before killing
     * off the application. If <i>auth</i> is <code>null</code> then the app is
     * always killed off.
     * 
     * @param newDb
     *            hashtable holding app entries that are not to be deleted
     * @param auth
     *            external authorization information (always <code>null</code>
     *            when called for an XAIT)
     * @param oldEntry
     *            the old AppEntry
     */
    private void delOldApp(Hashtable newDb, ExternalAuthorization auth, AppEntry oldEntry)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Del app: " + oldEntry + ", null auth: " + (auth == null) + ", running: "
                    + isRunning(oldEntry.id));
        }

        // If not externally authorized, or externally authorized and not
        // running, kill it off
        int priority = (auth == null || (auth != null && !isRunning(oldEntry.id))) ? -1
                : auth.getAuthorization(oldEntry.id);
        if (priority < 0)
        {
            disposeApp(oldEntry.id);
        }
        else
        {
            // Update the priority if running
            modifyPriority(oldEntry.id, priority);
            // TODO: bug 1503: keep externally authorized appentry (separate) if
            // running!

            if (log.isDebugEnabled())
            {
                log.debug("ExtAuth app: " + oldEntry);
            }

            // app was not disposed, make sure it's in the newDb
            newDb.put(oldEntry.id, oldEntry);
        }

        // Generate APP_DELETED event!
        if (!needSignalling)
        {
            AppsDatabaseEvent appDeletedEvent = new AppsDatabaseEvent(AppsDatabaseEvent.APP_DELETED, oldEntry.id, this);
            // don't use isEntryVisibleToCurrentContext helper here, only notify
            // domain listeners if entry is visible
            // always notify composite apps db listeners
            if (oldEntry.visibility != AppEntry.NON_VISIBLE)
            {
                notifyAppDomainListeners(appDeletedEvent);
            }
            notifyCompositeAppsDBListeners(appDeletedEvent);
        }

        // Ensure that this app is not in the re-autostart list
        reAutoStartApps.remove(oldEntry.id);
    }

    /**
     * Method used by
     * {@link #updateAppsDB(AbstractServiceEntry, boolean, boolean)} and
     * {@link #updateAppsDB(SignallingEvent)} to add a new app to the database.
     * At this point it is known that the app is new. Takes care of AUTOSTARTing
     * the app if necessary and notifying listeners of the change.
     * 
     * @param newDb
     *            database to add the app to
     * @param autoStart
     *            autostart list (update if new app is AUTOSTART);
     *            <code>null</code> if autostart is not enabled
     * @param newEntry
     *            the new app entry
     */
    private void addNewApp(Hashtable newDb, Vector autoStart, AppEntry newEntry)
    {
        if (autoStart != null && newEntry.controlCode == OcapAppAttributes.AUTOSTART)
        {
            // Launch it!
            autoStart.addElement(newEntry);
        }

        // Add to new database
        if (log.isDebugEnabled())
        {
            log.debug("New app: " + newEntry);
        }
        newDb.put(newEntry.id, newEntry);

        // Generate APP_ADDED event!
        if (!needSignalling)
        {
            AppsDatabaseEvent appAddedEvent = new AppsDatabaseEvent(AppsDatabaseEvent.APP_ADDED, newEntry.id, this);
            // don't use isEntryVisibleToCurrentContext helper here, only notify
            // domain listeners if entry is visible
            // always notify composite apps db listeners
            if (newEntry.visibility != AppEntry.NON_VISIBLE)
            {
                notifyAppDomainListeners(appAddedEvent);
            }
            notifyCompositeAppsDBListeners(appAddedEvent);
        }

    }
    
    /**
     * Called by AppContext when an application is destroyed
     * 
     * @param id the appID that was destroyed
     */
    void appDestroyed(AppID id)
    {
        synchronized (lock)
        {
            AppEntry app = (AppEntry)database.get(id);

            // Make sure this is:
            //    An app known to us
            //    An unbound app domain
            if (app != null && (app instanceof XAppEntry))
            {
                XAppEntry xae = (XAppEntry)app;
                if (xae.newVersion != null)
                {
                    if (!xae.isMonitorApp)
                    {
                        database.remove(id);
                        AppVersionManager.newVersionAvailable(xae.newVersion);
                    }
                }
                else
                {
                    AppVersionManager.checkForNewVersion(xae.id);
                }
            }
        }
    }
    
    /**
     * Called by the AppVersionManager when a new application version is available
     * for startup by this domain
     * 
     * @param newVersion
     */
    void notifyNewAppVersion(XAppEntry newVersion)
    {
        synchronized (lock)
        {
            // Replace current entry with new entry
            database.put(newVersion.id, newVersion);
        }

        // If AUTOSTART, launch it! (Consider as if "newly" AUTOSTART)
        // Avoid restarting if IMA -- this will be handled "externally"
        if (newVersion.controlCode == OcapAppAttributes.AUTOSTART)
        {
            if (log.isInfoEnabled())
            {
                log.info("Launching new version of app " + newVersion.id);
            }

            startApp(newVersion);
        }
    }

    /**
     * Returns the <code>ServicesDatabase</code> instance to be used.
     * 
     * @return the <code>ServicesDatabase</code> instance to be used
     */
    private ServicesDatabase getServicesDatabase()
    {
        ServiceManager svcMgr = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        return svcMgr.getServicesDatabase();
    }

    /**
     * An Enumeration class which is always empty
     */
    private static class EmptyEnumeration implements Enumeration
    {
        EmptyEnumeration()
        {
            // do nothing
        }

        public boolean hasMoreElements()
        {
            return false;
        }

        public Object nextElement()
        {
            throw new NoSuchElementException("Empty Enumeration - no more elements");
        }
    }

    /**
     * Used within <code>State</code> for <code>toString()</code>.
     */
    private static final String[] states = { "DESTROYED", "UNSELECTED", "SELECTED", "SEMI_SELECTED", "PRE_SELECTED" };

    /**
     * Manages the state transitions of this <code>AppDomain</code>
     * implementation.
     * <p>
     * This class represents four states:
     * <ul>
     * <li> <code>UNSELECTED</code>
     * <li> <code>SELECTED</code>
     * <li> <code>SEMI_SELECTED</code.
     * <li> <code>DESTROYED</code>
     * </ul>
     * State transitions can occur as the result of operations on the
     * <code>AppDomain</code> object. Specifically:
     * <ul>
     * <li> {@link AppDomainImpl#select}
     * <li> {@link AppDomainImpl#stopBoundApps}
     * <li> {@link AppDomainImpl#stop}
     * <li> {@link AppDomainImpl#destroy}
     * </ul>
     * 
     * @author Aaron Kamienski
     */
    private class State
    {
        /**
         * The <code>UNSELECTED</code> state indicates that a
         * <code>Service</code> has not yet been selected.
         * <p>
         * Can transition to <code>SELECTED</code>, <code>PRE_SELECTED</code>,
         * or <code>DESTROYED</code> states.
         */
        static final int UNSELECTED = 0;

        /**
         * The <code>SELECTED</code> state indicates that a <code>Service</code>
         * has been selected and that the <code>AppDomain</code> is waiting on
         * application signalling changes for further instructions.
         * <p>
         * Can transition to <code>UNSELECTED</code>, <code>SEMI_SELECTED</code>
         * , and <code>DESTROYED</code> states.
         */
        static final int SELECTED = 1;

        /**
         * The <code>SEMI_SELECTED</code> state indicates that the associated
         * <code>ServiceContext</code> is in the process of selecting a new
         * <code>Service</code>. At this time, the previously selected services
         * <i>bound</i> apps have been destroyed, but the new service has yet to
         * be selected.
         * <p>
         * Can transition to <code>UNSELECTED</code>, <code>SELECTED</code>, and
         * <code>DESTROYED</code> states.
         */
        static final int SEMI_SELECTED = 2;

        /**
         * The <code>PRE_SELECTED</code> state is entered from the
         * <code>UNSELECTED</code> state upon invocation of
         * {@link AppDomainImpl#preSelect(ServiceDetails)}. The transition to
         * the <code>PRE_SELECTED</code> state is like a reduced transition to
         * the <code>SELECTED</code> state.
         * <p>
         * Can transition to <code>SELECTED</code>, <code>SEMI_SELECTED</code>,
         * <code>UNSELECTED</code>, and <code>DESTROYED</code> states.
         */
        static final int PRE_SELECTED = 3;

        /**
         * The <code>DESTROYED</code> state indicates that the associated
         * <code>ServiceContext</code> has been destroyed. The
         * <code>AppDomain</code> should no longer be used -- in fact, it should
         * soon be a candidate for garbage collection.
         * <p>
         * No state transitions out of <code>DESTROYED</code> state are
         * possible.
         */
        static final int DESTROYED = -1;

        private int state;

        public String toString()
        {
            return states[getState() + 1];
        }

        synchronized int getState()
        {
            return state;
        }

        private void checkState()
        {
            if (state == DESTROYED) throw new IllegalStateException("operation not valid in DESTROYED state");
        }

        /**
         * Implements the state change initiated by a call to select().
         * 
         * @param serviceDetails
         *            the <code>ServiceDetails</code> being selected
         */
        private synchronized void handleSelect(ServiceDetails serviceDetails)
        {
            if (state == PRE_SELECTED)
            {
                if (serviceDetails != null && !(serviceDetails.equals(currentServiceDetails)))
                    throw new IllegalStateException("select following preSelect expects same service");
            }
            else if (state != UNSELECTED && state != SEMI_SELECTED)
            {
                // Allow context to call select twice with exact same service
                // object.
                // And pretend like 2nd (and subsequent) calls did not happen.
                // This is mainly to support case where SC gets
                // EnteringLiveEvent on initial playback
                // we can get unique but equal instances here..don't just
                // compare object identity
                if (serviceDetails != null && serviceDetails.equals(currentServiceDetails))
                {
                    return;
                }
            }

            // allow selection of new serviceDetails while selected (apps that
            // should be stopped will be in updateAppsDb)
            if (log.isDebugEnabled())
            {
                log.debug("Setting current service = " + serviceDetails);
            }

            needSignalling = true;

            ServiceDetails origServiceDetails = currentServiceDetails;
            currentServiceDetails = serviceDetails;

            // Register self with Global AppsDB
            globalDb.addAppsDatabase(AppDomainImpl.this);

            // Figure out the type of service
            // Only know how to deal with Services w/ OcapLocator
            // (I.e., AbstractService or broadcast Service)
            currentServiceSupported = isSupportedService(serviceDetails);
            if (currentServiceSupported)
                handleSelectSupported(origServiceDetails, serviceDetails);
            else
                stopApps(false);

            // Move to the SELECTED state
            state = SELECTED;
        }

        /**
         * Implements {@link #handleSelect(ServiceDetails)} for a supported
         * service type. This is only called if
         * {@link #isSupportedService(ServiceDetails)} returns <code>true</code>
         * .
         * 
         * @param serviceDetails
         *            the supported serviceDetails
         */
        private void handleSelectSupported(ServiceDetails orig, ServiceDetails serviceDetails)
        {
            if (log.isDebugEnabled())
            {
                log.debug("handleSelectSupported: " + serviceDetails);
            }
            OcapLocator loc = (OcapLocator) serviceDetails.getLocator();
            // AbstractService?
            if (serviceDetails.getService() instanceof AbstractService)
            {
                int serviceId = loc.getSourceID();
                ServicesDatabase sdb = getServicesDatabase();

                AppVersionManager.registerAppDomain(AppDomainImpl.this,serviceId);
                
                // Get the latest information from ServicesDatabase
                AbstractServiceEntry entry = sdb.getServiceEntry(serviceId);
                updateAppsDB(entry, true, state != PRE_SELECTED); // AUTOSTART
                                                                  // all if
                                                                  // PRE_SELECTED

                // Add listener for changes
                sdb.addServiceChangeListener(serviceId, AppDomainImpl.this);
            }
            // Broadcast Service?
            else
            {
                // we're selecting a new service -
                // delete all apps flagged as service_bound
                // if same app is signaled in this update, it will be started by
                // updateappsdb
                if (log.isDebugEnabled())
                {
                    log.debug("handleSelectSupported - looking for existing service-bound apps to dispose - app count: "
                            + database.size());
                }
                Vector serviceBoundApps = new Vector();
                for (Enumeration e = database.keys(); e.hasMoreElements();)
                {
                    AppEntry app = (AppEntry)database.get(e.nextElement());
                    if (app.serviceBound)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("found service-bound app - disposing: " + app);
                        }
                        // we were service bound, dispose and remove from the
                        // current db
                        disposeApp(app.id);
                        serviceBoundApps.add(app.id);
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("app not service-bound - not disposing: " + app);
                        }
                    }
                }
                // Remove all of our service bound apps from the database
                for (Iterator i = serviceBoundApps.iterator(); i.hasNext();)
                {
                    database.remove(i.next());
                }

                // Remove previous AIT listener (if any) and listener for new
                // service
                if (!IGNORE_AIT)
                {
                    if (orig != null)
                        sm.removeAitListener((OcapLocator) orig.getLocator(), AppDomainImpl.this);
                    if (log.isDebugEnabled())
                    {
                        log.debug("adding AppDomainImpl as an AIT listener");
                    }
                    sm.addAitListener(loc, AppDomainImpl.this);

                    synchronized (lock)
                    {
                        // Start our AIT timer
                        try
                        {
                            // We will wait up to 11 seconds for an AIT to
                            // arrive
                            aitTimerSpec = new TVTimerSpec();
                            aitTimerSpec.setDelayTime(11000);
                            aitTimerSpec.addTVTimerWentOffListener(AppDomainImpl.this);
                            aitTimer.scheduleTimerSpec(aitTimerSpec);
                        }
                        catch (TVTimerScheduleFailedException e)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("Failed to schedult AIT timer!");
                            }
                        }
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("ignoring AIT - not adding AppDomainImpl as AIT listener");
                    }
                }
            }
        }

        /**
         * Returns <code>true</code> if this type of service is supported when
         * running applications. Only services whose getLocator() returns an
         * instance of <code>OcapLocator</code> are supported at this time.
         * 
         * @param serviceDetails
         *            the serviceDetails to test
         * @return <code>true</code> if running apps for this service is
         *         supported; <code>false</code> otherwise
         */
        private boolean isSupportedService(ServiceDetails serviceDetails)
        {
            try
            {
                return serviceDetails != null && serviceDetails.getLocator().getClass() == OcapLocator.class;
            }
            catch (RuntimeException e)
            {
                // This is generally for the case where currentService
                // represents a deleted recording.
                // But would also handle the case where the service doesn't have
                // a locator (???)
                if (log.isDebugEnabled())
                {
                    log.debug("Service is likely no longer valid", e);
                }
                return false;
            }
        }

        /**
         * Remove listeners added during select.
         */
        private synchronized void removeListeners()
        {
            if (currentServiceSupported)
            {
                OcapLocator loc = (OcapLocator) currentServiceDetails.getLocator();
                if (loc == null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("NO LOCATOR!!!  Service is likely no longer valid");
                    }
                }
                else
                {
                    if (currentServiceDetails.getService() instanceof AbstractService)
                    {
                        ServicesDatabase sdb = getServicesDatabase();
                        sdb.removeServiceChangeListener(loc.getSourceID(), AppDomainImpl.this);
                    }
                    else
                    {
                        sm.removeAitListener(loc, AppDomainImpl.this);
                    }
                }
            }
        }

        /**
         * Implements the state change associated with a call to preSelect().
         */
        private synchronized void handlePreSelect(ServiceDetails serviceDetails)
        {
            if (state != UNSELECTED || currentServiceDetails != null)
                throw new IllegalStateException("preSelect not valid in current state: " + this);

            if (serviceDetails == null || !(serviceDetails.getService() instanceof AbstractService))
                throw new IllegalStateException("preSelect only valid on AbstractServices");

            needSignalling = true;

            currentServiceDetails = serviceDetails;
            currentServiceSupported = true;

            // NOTE: does not register self with Global AppsDB -- wait until
            // select()

            int serviceId = ((OcapLocator) serviceDetails.getLocator()).getSourceID();
            ServicesDatabase sdb = getServicesDatabase();

            // Get the latest information from ServicesDatabase
            AbstractServiceEntry entry = sdb.getServiceEntry(serviceId);
            updateAppsDB(entry, false, false); // AUTOSTART disabled

            state = PRE_SELECTED;

            // NOTE: does not register for changes -- wait until select()
        }

        /**
         * Implements the state change associated with a call to stop().
         */
        private synchronized void handleStop()
        {
            checkState();
            if (state == UNSELECTED) return;

            Service s = currentServiceDetails.getService();
            
            if (s instanceof AbstractService)
            {
                int serviceID = ((OcapLocator)s.getLocator()).getSourceID();
                AppVersionManager.unregisterAppDomain(serviceID);
            }
            else
            {
                synchronized (lock)
                {
                    if (aitTimerSpec != null)
                    {
                        aitTimer.deschedule(aitTimerSpec);
                        aitTimerSpec = null;
                    }
    
                    needSignalling = true;
                }
            }

            // Remove listeners
            removeListeners();

            // Stop all applications
            stopApps(false);

            // Remove self from "global" AppsDB
            globalDb.removeAppsDatabase(AppDomainImpl.this);

            // Move to the UNSELECTED state
            currentServiceDetails = null;
            currentServiceSupported = false;
            state = UNSELECTED;
        }

        /**
         * Implements the state change associated with a call to destroy().
         */
        private synchronized void handleDestroy()
        {
            checkState();

            synchronized (lock)
            {
                needSignalling = true;
            }

            // Remove listeners
            removeListeners();

            // Stop all applications
            stopApps(false);

            // Clean up resources

            // Move to the DESTROYED state
            state = DESTROYED;
        }

        /**
         * Implements the state change associated with a call to
         * stopBoundApps().
         */
        private synchronized void handleStopBoundApps()
        {
            checkState();

            // Ignore altogether if current state is PRE_SELECTED
            if (state == PRE_SELECTED) return;

            // Remove listeners
            removeListeners();

            // If current service is unbound, then kill all apps
            // else, kill all apps marked as service bound
            if (currentServiceDetails != null)
            {
                stopApps(!(currentServiceDetails.getService() instanceof AbstractService));
            }

            // Move to the SEMI-SELECTED state
            state = SEMI_SELECTED;
        }

        /**
         * Implements the state change associated with the reception of updated
         * AIT signalling.
         * 
         * @param event
         *            the signalling event
         */
        private synchronized void handleAitSignalling(SignallingEvent event)
        {
            if (state != SELECTED)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("handleAitSignalling - not selected - not processing signalling event: " + event);
                }
                return;
            }

            // Update AppsDB
            updateAppsDB(event);

            // Stay in the SELECTED state...
        }

        /**
         * Implements the state change associated with the reception of updated
         * XAIT signalling.
         * 
         * @param service
         *            the signalled service
         */
        private synchronized void handleXaitSignalling(AbstractServiceEntry service)
        {
            if (state != SELECTED || service == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("handleXaitSignalling called but not selected or service is null - ignoring: " + service);
                }
                return;
            }

            // Update AppsDB
            updateAppsDB(service, true, true); // AUTOSTART "newly AUTOSTART"
                                               // only

            // Stay in the SELECTED state...
        }
    }

    /**
     * Used to call the appropriate method on a listener. Avoids having to
     * either repeat the calling code within a big switch or do a switch within
     * the calling code. May be a bit of overkill, perhaps.
     * 
     * @author Aaron Kamienski
     */
    private abstract static class Notifier
    {
        public abstract void notify(AppsDatabaseEventListener l, AppsDatabaseEvent e);

        public static final Notifier NEW_DATABASE = new Notifier()
        {
            public void notify(AppsDatabaseEventListener l, AppsDatabaseEvent e)
            {
                l.newDatabase(e);
            }
        };

        public static final Notifier ADDED = new Notifier()
        {
            public void notify(AppsDatabaseEventListener l, AppsDatabaseEvent e)
            {
                l.entryAdded(e);
            }
        };

        public static final Notifier DELETED = new Notifier()
        {
            public void notify(AppsDatabaseEventListener l, AppsDatabaseEvent e)
            {
                l.entryRemoved(e);
            }
        };

        public static final Notifier CHANGED = new Notifier()
        {
            public void notify(AppsDatabaseEventListener l, AppsDatabaseEvent e)
            {
                l.entryChanged(e);
            }
        };
    }

    /**
     * Simple class for handling the lookup of externally authorized
     * applications.
     * 
     * @author Aaron Kamienski
     */
    private class ExternalAuthorization
    {
        private Hashtable auth = new Hashtable();

        /**
         * Creates an instance of <code>ExternalAuthorization</code> based upon
         * the set of <i>external_authorization</i> entries specified by the
         * given AIT.
         * 
         * @param ait
         *            representation of the AIT
         */
        ExternalAuthorization(Ait ait)
        {
            Ait.ExternalAuthorization[] ids = ait.getExternalAuthorization();

            for (int i = 0; i < ids.length; ++i)
            {
                auth.put(ids[i].id, new Integer(ids[i].priority));
            }
        }

        /**
         * Determines if the specified application is externally authorized or
         * not.
         * <p>
         * Three searches are performed on the <i>external_authorization</i>
         * entries of the AIT. The searches are as follows:
         * <ol>
         * <li>an explicit reference for the given <code>AppID</code>
         * <li>wildcard search with <code>AID==0xFFFE</code>, if application is
         * signed
         * <li>wildcard search with <code>AID==0xFFFF</code>
         * </ol>
         * The priority associated with the first successful search is returned.
         * 
         * @param id
         *            specifies the application for which authorization is
         *            desired
         * @return the new application priority if the application is externally
         *         authorized; <code>-1</code> otherwise
         */
        int getAuthorization(AppID id)
        {
            Integer priority = (Integer) auth.get(id);
            if (priority == null && isSigned(id)) priority = (Integer) auth.get(new AppID(id.getOID(), 0xFFFE));
            if (priority == null) priority = (Integer) auth.get(new AppID(id.getOID(), 0xFFFF));
            return (priority == null) ? -1 : priority.intValue();
        }
    }

    /**
     * Per-context global data. Remembers per-context
     * <code>AppsDatabaseListener</code>s.
     * 
     * @author Aaron Kamienski
     */
    private class Data implements CallbackData
    {
        public AppsDatabaseEventListener listeners;

        public void destroy(CallerContext cc)
        {
            forgetListeners(cc);
        }

        public void active(CallerContext cc)
        { /* empty */
        }

        public void pause(CallerContext cc)
        { /* empty */
        }
    }

    /**
     * Internal lock.
     */
    private Object lock = new Object();

    /**
     * Associated <code>ServiceContext</code>.
     */
    private ServiceContext serviceContext;

    /**
     * Global AppsDatabase. Upon {@link #select},
     * {@link CompositeAppsDB#addAppsDatabase(AppsDatabase)} should be called.
     * Upon {@link #stop},
     * {@link CompositeAppsDB#removeAppsDatabase(AppsDatabase)} should be
     * called.
     */
    private CompositeAppsDB globalDb;

    /**
     * Manages the current <i>state</i> of this <code>AppDomain</code>.
     */
    private State stateMachine = new State();

    /**
     * The <code>Service</code> currently being presented by this
     * <code>AppDomain</code>.
     */
    private ServiceDetails currentServiceDetails = null;

    /**
     * Set when the {@link #currentServiceDetails} is
     * {@link State#isSupportedService(ServiceDetails) supported}. As such,
     * <code>isSupportedService()</code> only needs to be called once, when the
     * <i>currentService</i> is set.
     */
    private boolean currentServiceSupported;

    /**
     * Indicates whether we are waiting for the initial delivery of application
     * signalling for a newly selected service. If <code>true</code> then we
     * have not recieved any signalling yet.
     */
    private boolean needSignalling = true;

    /**
     * Timer used to detect when an AIT is not present in a given service
     */
    private TVTimer aitTimer = TVTimer.getTimer();

    private TVTimerSpec aitTimerSpec = null;

    /**
     * The set of <code>Application</code> objects created to represent OCAP-J
     * applications in this <code>ServiceContext</code>. They may or may not be
     * running.
     */
    private Hashtable knownApps = new Hashtable();

    /**
     * The list of <code>AppID</code>s corresponding to AppIDs of knownApps.
     */
    private Vector knownAppIds = new Vector();

    /**
     * The main representation of the applications database.
     */
    private Hashtable database = new Hashtable();

    /**
     * Applications that should be re-autostarted upon the next receipt of
     * signalling
     */
    private Hashtable reAutoStartApps = new Hashtable();

    /**
     * The set of AppDomain-specific <code>CallerContext</code>s that have
     * installed database listeners.
     */
    private CallerContext contexts = null;

    /**
     * The set of CompositeAppsDB-specific <code>CallerContext </code>s that
     * have installed database listeners.
     */
    private CallerContext compositeAppsDBContexts = null;

    // permission needed to receive information/notifications for NON_VISIBLE
    // apps
    private final MonitorAppPermission MONAPP_SERVICE_MANAGER_PERMISSION = new MonitorAppPermission("servicemanager");

    /**
     * The unique id of this AppDomain
     */
    private Long uniqueId;

    private static long idBase = 1L;

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(AppDomainImpl.class.getName());

    private static final boolean TRACKING = true;

    /**
     * Flag used to determine if we should ignore the AIT. Based upon the value
     * <code>"OCAP.ait.ignore"</code> property. If the property is set to
     * <code>"true"</code>, then this will be set and we'll ignore the AIT.
     */
    private static final boolean IGNORE_AIT = "true".equals(MPEEnv.getEnv("OCAP.ait.ignore", "false"));

    /**
     * Reset to false once BootProcessCallback.start() is called.
     */
    private static volatile boolean inBootupPhase = true;

    private static Object bootLock = new Object();

    /**
     * Application entries that are marked as auto-start, but cannot be started
     * until the Monitor App is ready
     */
    private Vector deferredAutoStartApps = new Vector();

    private BootProcessCallbackListener bootCallbackListener;

    private CallerContextManager ccm =
        (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
    private SignallingManager sm =
        (SignallingManager) ManagerManager.getInstance(SignallingManager.class);
    private ApplicationManager am =
        (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);
    private AppStorageManager asm =
        (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);

    /**
     * Implements the boot process state listener
     */
    class BootProcessCallbackListener implements ServicesDatabase.BootProcessCallback
    {
        /**
         * Upon shutdown notification, all active recordings should be
         * synchronously transitioned into a disabled state
         */
        public boolean monitorApplicationShutdown(ServicesDatabase.ShutdownCallback cb)
        {
            // reset our state.
            deferredAutoStartApps.clear();
            inBootupPhase = true;
            // return false to indicate that our clean has been performed
            // synchronously
            // Axiom change - adding sync block
            synchronized (lock)
            {
                stop();
                // reset our state.
                if (log.isInfoEnabled())
                {
                    log.info("Boot process shutdown, reset state!");
                }
                deferredAutoStartApps.clear();
                inBootupPhase = true;
                // return false to indicate that our clean has been performed
                // synchronously
            }
            return false;
        }

        /**
         * from ServicesDatabase.BootProcessCallback interface. When notified
         * that the reboot process has been completed, enable the recording
         * schedule according to the delay set through setRecordingDelay()
         */
        public void monitorApplicationStarted()
        {
            synchronized (bootLock)
            {
                inBootupPhase = false;
                for (Enumeration e = deferredAutoStartApps.elements(); e.hasMoreElements();)
                {
                    AppEntry entry = (AppEntry) e.nextElement();
                    if (log.isInfoEnabled())
                    {
                        log.info("Launching (deferred)" + entry);
                    }
                    startApp(entry);
                }
                deferredAutoStartApps.clear();
            }
        }
        
        /**
         * Called after unbound autostart apps have been started
         */
        public void initialUnboundAutostartApplicationsStarted()
        {
            // Nothing to do (only care about monitor app startup)
        }
    }

    /**
     * This class manages the launch of new application versions that may or may
     * not span different abstract services (app domains)
     *	
     *  @author Greg Rutz
     */
    private static class AppVersionManager
    {
        // Called by AppDomains when they become selected
        public static synchronized void registerAppDomain(AppDomainImpl domain, int serviceID)
        {
            domainListeners.put(new Integer(serviceID), domain);
            
            // Are there any outstanding new versions for this service
            Integer id = new Integer(serviceID);
            Vector newApps = (Vector)newAppVersions.get(id);
            if (newApps != null)
            {
                for (Iterator i = newApps.iterator(); i.hasNext();)
                {
                    domain.notifyNewAppVersion((XAppEntry)i.next());
                }
                newAppVersions.remove(id);
            }
        }
        
        // Called by AppDomains when they become unselected
        public static synchronized void unregisterAppDomain(int serviceID)
        {
            domainListeners.remove(new Integer(serviceID));
        }
        
        // Called when a new application version is found
        public static synchronized void addNewVersion(Integer serviceID, XAppEntry app)
        {
            Vector newVersions = (Vector)newAppVersions.get(serviceID);
            if (newVersions == null)
            {
                newVersions = new Vector();
                newAppVersions.put(serviceID, newVersions);
            }
            newVersions.add(app);
        }
        
        // Called when a currently running application has been destroyed
        // We will alert the registered app domain associated with the service
        // ID for the new app.  Otherwise, we will store this new
        // version until such time when its app domain becomes selected
        public static synchronized void newVersionAvailable(XAppEntry app)
        {
            Integer serviceID = new Integer(app.serviceId);
            
            AppDomainImpl listener;
            if ((listener = (AppDomainImpl)domainListeners.get(serviceID)) != null)
            {
                listener.notifyNewAppVersion(app);
            }
            else
            {
                addNewVersion(serviceID,app);
            }
        }
        
        // Called when a currently running application has been destroyed
        // and it doesn't have a new version associated with it.  This
        // scenario can happen when a new version of a running application is
        // signaled and the original version is no longer signaled
        public static synchronized void checkForNewVersion(AppID id)
        {
            for (Enumeration e = newAppVersions.keys(); e.hasMoreElements();)
            {
                Integer key = (Integer)e.nextElement();
                Vector newVersions = (Vector)newAppVersions.get(key);
                for (Iterator i = newVersions.iterator(); i.hasNext();)
                {
                    // Search for a matching AppID.
                    XAppEntry entry = (XAppEntry)i.next();
                    if (id.equals(entry.id))
                    {
                        // Found it.  Notify any registered listener and
                        // delete the app from our list
                        AppDomainImpl listener = (AppDomainImpl)domainListeners.get(key);
                        if (listener != null)
                        {
                            listener.notifyNewAppVersion(entry);
                            newVersions.remove(entry);
                            if (newVersions.isEmpty())
                            {
                                i.remove();
                            }
                        }
                        return;
                    }
                }
            }
        }
        
        // Returns true if there are new app versions available for the given service ID
        public static synchronized boolean checkForNewApps(int serviceID)
        {
            return newAppVersions.get(new Integer(serviceID)) != null;
        }
        
        // Removes all versions of the given app ID currently being tracked
        // Called when new signaling is received for an app.
        public static synchronized void clearApps(AppID id)
        {
            boolean found = false;
            for (Enumeration e = newAppVersions.keys(); e.hasMoreElements();)
            {
                Object key = e.nextElement();
                Vector apps = (Vector)newAppVersions.get(key);
                for (Iterator i = apps.iterator(); i.hasNext();)
                {
                    XAppEntry app = (XAppEntry)i.next();
                    if (app.id.equals(id))
                    {
                        found = true;
                        i.remove();
                        break;
                    }
                }
                
                // If we found the app and removed it, we may also have to
                // remove this service from our list.
                if (found)
                {
                    if (apps.isEmpty())
                    {
                        newAppVersions.remove(key);
                    }
                    break;
                }
            }
        }
        
        // List of registered AppDomainImpls keyed by service ID
        private static Hashtable domainListeners = new Hashtable();
        
        // List of new application versions available to other services
        // Key = Integer (serviceID)
        // Value = Vector (XAppEntrys)
        private static Hashtable newAppVersions = new Hashtable();
    }

    private class InitialAutostartAppStateChangeEventListener implements AppStateChangeEventListener
    {
        public void stateChange(AppStateChangeEvent evt)
        {
            //listening for autostart apps to start - all notifications will have a toState of STARTED regardless if they
            //successfully started or failed to start - no need to check the event state
            boolean notify = false;
            synchronized(lock)
            {
                if (!notifiedInitialAutostartAppsStarted)
                {
                    Application app = (Application) initialAutostartApps.remove(evt.getAppID());
                    app.removeAppStateChangeEventListener(initialAutostartAppStateChangeEventListener);
                    if (initialAutostartApps.isEmpty())
                    {
                        notify = true;
                        notifiedInitialAutostartAppsStarted = true;
                    }
                }
            }
            if (notify)
            {
                if (log.isInfoEnabled())
                {
                    log.info("all initial unbound autostart apps started - notifying");
                }
                if (initialAutostartAppsStartedListener != null)
                {
                    initialAutostartAppsStartedListener.initialAutostartAppsStarted();
                    //one-time notification, release reference
                    initialAutostartAppsStartedListener = null;
                }
            }
        }
    }
}
