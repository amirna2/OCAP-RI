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

package org.cablelabs.impl.manager.service;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.tv.service.SIChangeType;
import javax.tv.service.navigation.DeliverySystemType;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextEvent;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextListener;
import javax.tv.service.transport.ServiceDetailsChangeEvent;
import javax.tv.service.transport.Transport;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppStateChangeEventListener;
import org.dvb.application.DVBJProxy;
import org.ocap.application.OcapAppAttributes;
import org.ocap.hardware.Host;
import org.ocap.service.AbstractService;

import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.AppStorageManager;
import org.cablelabs.impl.manager.AuthManager;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.SignallingManager;
import org.cablelabs.impl.ocap.OcapMain;
import org.cablelabs.impl.ocap.hardware.ExtendedHost;
import org.cablelabs.impl.service.SICache;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceContextFactoryExt;
import org.cablelabs.impl.service.ServicesDatabase;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.SignallingEvent;
import org.cablelabs.impl.signalling.SignallingListener;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.signalling.Xait;
import org.cablelabs.impl.signalling.AppEntry.LocalTransportProtocol;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.manager.AppStorageManager.AppStorage;
import org.cablelabs.impl.manager.progress.ProgressMgr;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * Default implementation of the <code>ServicesDatabase</code>.
 * <p>
 * This implementation supports some system properties that can configure its
 * operation.
 * <table border>
 * <tr>
 * <th>Property</th>
 * <th>Comments</th>
 * </tr>
 * <tr>
 * <td>OCAP.xait.ignore</td>
 * <td>if "true" then ignore XAIT</td>
 * </tr>
 * <tr>
 * <td>OCAP.xait.timeout</td>
 * <td>timeout in seconds for initial XAIT access (30 sec)</td>
 * </tr>
 * <tr>
 * <td>OCAP.monapp.resident</td>
 * <td>if "true" then MonApp is a resident app</td>
 * </tr>
 * </table>
 * 
 * @author Aaron Kamienski
 * @author Todd Earles
 * 
 * @see SIManagerImpl
 */
class ServicesDatabaseImpl implements ServicesDatabase, SignallingListener
{
    /**
     * Returns the singleton instance of <code>ServicesDatabaseImpl</code>.
     * 
     * @return the singleton instance of <code>ServicesDatabaseImpl</code>
     */
    static ServicesDatabaseImpl getInstance()
    {
        return singleton;
    }

    /**
     * Not publicly instantiable.
     */
    ServicesDatabaseImpl()
    { /* EMPTY */
    }

    /**
     * This method encapsulates the entire boot process, from initial XAIT
     * acquisition to MonApp launch, to abstract service selection. This is a
     * blocking call, that does not return until the boot process is complete.
     * The boot process is not considered complete until all auto-select
     * abstract services have been selected.
     * <p>
     * If there is no XAIT, that is not considered an error. The boot process
     * continues without MSO applications.
     * <p>
     * If the Initial MonApp cannot be launched for some reason, that is not
     * considered an error. Instead the boot process continues without a monitor
     * application. If the XAIT is versioned, then an IMA is tested for again
     * and the boot process may be restarted.
     */
    public void bootProcess()
    {
        ProgressMgr progressMgr = (ProgressMgr)
        // ManagerManager.getInstance(ProgressMgr.class);
        // Axiom:
        (ProgressMgr) ProgressMgr.getInstance();

        progressMgr.signal(ProgressMgr.kStartBoot);

        if (log.isInfoEnabled())
        {
            log.info("bootProcess() initiated...");
        }

        // Enable ServiceContext creation (if previously disabled)
        ServiceContextFactoryExt scf = (ServiceContextFactoryExt) ServiceContextFactory.getInstance();
        scf.setCreateEnabled(true);

        // Load hostapp.properties
        loadHostApps();

        // Go through boot process
        if (IGNORE_XAIT)
        {
            if (log.isDebugEnabled())
            {
                log.debug("IGNORE_XAIT set");
            }
        }
        else
        {
            // Set up to acquire XAIT
            sm.addXaitListener(this);

            if (log.isDebugEnabled())
            {
                log.debug("Waiting for XAIT...");
            }

            if (xaitAcquired.acquire(XAIT_TIMEOUT, false) || sm.loadPersistentXait())
            {
                progressMgr.signal(ProgressMgr.kXaitReceived); // Axiom chg
                launchMonApp();
            }
        }

        // Notify boot process callbacks that monitor application has been started
        notifyMonitorApplicationStarted();

        boolean notifyInitialAutoStartAppsStarted = false;
        synchronized (this)
        {
            Vector autoSelect = new Vector();
            // Normal boot process, auto-start all remaining services
            for (Enumeration e = db.elements(); e.hasMoreElements();)
            {
                AbstractServiceEntry svc = (AbstractServiceEntry) e.nextElement();
                if (svc.autoSelect && (currIMA == null || !currIMA.isMonApp(svc)))
                {
                    autoSelect.addElement(svc);
                }
            }
            autoSelect(autoSelect.elements(), true);
            //no autostart apps - notify
            if (autoSelect.isEmpty())
            {
                notifyInitialAutoStartAppsStarted = true; 
            }

            // Finally, enter normal run state
            autoSelectEnabled = true;
        }
        if (notifyInitialAutoStartAppsStarted)
        {
            if (log.isInfoEnabled())
            {
                log.info("no autoselect services - notifying unbound applications started");
            }
            notifyUnboundApplicationsStarted();
        }

        if (log.isDebugEnabled())
        {
            log.debug("Boot Process Complete.");
        }
    }

    // Description copied from ServicesDatabase
    public void notifyMonAppConfiguring()
    {
		// Added for findbugs issues fix - using cached copy
    	InitialMonitorApp initMonApp = currIMA;
        if (initMonApp != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Signalling MonApp CONFIGURING...");
            }

            initMonApp.notifyMonAppConfiguring();
        }
    }

    // Descriptions copied from ServicesDatabase
    public void notifyMonAppConfigured()
    {
		// Added for findbugs issues fix - using cached copy
    	InitialMonitorApp initMonApp = currIMA;
        if (initMonApp != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Signalling MonApp CONFIGURED");
            }

            initMonApp.notifyMonAppConfigured();
        }
    }

    // Description copied from ServicesDatabase
    public void addBootProcessCallback(BootProcessCallback toAdd)
    {
        // due to timing issues (and managers attempting to avoid circular
        // dependencies by delaying bootProcessCallback registration),
        // some managers may attempt to register a BootProcessCallback AFTER
        // notifyCallbacksStarted has been called
        // if this happens, immediately invoke the started method on the
        // newly-registered callback
        boolean callBackMonAppStartedImmediately;
        boolean callBackHostAppsStartedImmediately;
        synchronized (callbacks)
        {
            callBackMonAppStartedImmediately = monAppStarted;
            callBackHostAppsStartedImmediately = autostartHostAppsStarted;
            callbacks.addElement(toAdd);
        }
        if (callBackMonAppStartedImmediately)
        {
            if (log.isDebugEnabled())
            {
                log.debug("addBootProcessCallback - monitor application started callback added after notifyCallbacksStarted - immediately calling: "
                        + toAdd);
            }
            try
            {
                toAdd.monitorApplicationStarted();
            }
            catch (Throwable t)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("monitorApplicationStarted on BootProcessCallback " + toAdd + " threw an exception during notificiation", t);
                }
            }
        }
        if (callBackHostAppsStartedImmediately)
        {
            if (log.isDebugEnabled())
            {
                log.debug("addBootProcessCallback - autostart host application started callback added after notifyCallbacksStarted - immediately calling: "
                        + toAdd);
            }
            try
            {
                toAdd.initialUnboundAutostartApplicationsStarted();
            }
            catch (Throwable t)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("initialUnboundAutostartApplicationsStarted on BootProcessCallback " + toAdd + " threw an exception during notificiation", t);
                }
            }
        }
    }

    // Description copied from ServicesDatabase
    public void removeBootProcessCallback(BootProcessCallback toRemove)
    {
        synchronized (callbacks)
        {
            callbacks.removeElement(toRemove);
        }
    }

    // Description copied from ServicesDatabase
    public synchronized AbstractService getAbstractService(int serviceId)
    {
        AbstractServiceEntry entry = (AbstractServiceEntry) db.get(new Integer(serviceId));
        if (entry == null)
        {
            return null;
        }
        return new AbstractServiceImpl(entry);
    }

    // Description copied from ServicesDatabase
    public synchronized void getAbstractServices(ServiceCollection collection)
    {
        for (Enumeration e = db.elements(); e.hasMoreElements();)
        {
            AbstractServiceEntry entry = (AbstractServiceEntry) e.nextElement();
            AbstractService service = new AbstractServiceImpl(entry);
            collection.add(service);
        }
    }

    /**
     * Returns the latest accepted <code>AbstractServiceEntry</code> for the
     * service specified by the given <i>serviceId</i>.
     * 
     * @return <code>AbstractServiceEntry</code> for the desired service;
     *         <code>null</code> if no such service is currently known
     */
    public synchronized AbstractServiceEntry getServiceEntry(int serviceId)
    {
        return (AbstractServiceEntry) db.get(new Integer(serviceId));
    }

    /**
     * Installs the given listener to be notified of changes to the service
     * specified by the given <i>serviceId</i>.
     * 
     * @param serviceId
     *            the service to listen for changes
     * @param l
     *            the listener to add
     */
    public synchronized void addServiceChangeListener(int serviceId, ServiceChangeListener l)
    {
        // Ignore if resident service
        if (isHostService(serviceId)) return;

        Integer key = new Integer(serviceId);
        ServiceChangeListener listener = (ServiceChangeListener) listeners.get(key);

        listener = EventMulticaster.add(listener, l);

        listeners.put(key, listener);
    }

    /**
     * Removes the given listener which previously had been waiting for
     * notification of changes to the service specified by the given
     * <i>serviceId</i>.
     * 
     * @param serviceId
     *            the service previously listened to for changes
     * @param l
     *            the listener to remove
     */
    public synchronized void removeServiceChangeListener(int serviceId, ServiceChangeListener l)
    {
        // Ignore if resident service
        if (isHostService(serviceId)) return;

        Integer key = new Integer(serviceId);
        ServiceChangeListener listener = (ServiceChangeListener) listeners.get(key);

        listener = EventMulticaster.remove(listener, l);

        if (listener == null)
            listeners.remove(key);
        else
            listeners.put(key, listener);
    }

    /**
     * Provides the initial entry point for the implementation of the
     * {@link org.ocap.application.AppManagerProxy#unregisterUnboundApp}. This
     * will remove the application from the associated abstract service entry in
     * the services database and then notify any installed
     * <code>ServiceChangeListeners</code> (i.e., interested
     * <code>AppsDatabase</code> implementations), which would then take the
     * appropriate actions including termination of the application (as if an
     * application signalled in the XAIT were removed from the XAIT).
     * <p>
     * Note that appropriate security permissions should be tested prior to
     * invoking this method.
     * 
     * @param serviceId
     *            The service identifier to which this application is
     *            registered.
     * 
     * @param appid
     *            An AppID instance identifies the application entry to be
     *            unregistered from the service.
     * 
     * @throws IllegalArgumentException
     *             if this method attempts to modify an application signaled in
     *             the XAIT or an AIT or a host device manufacturer application.
     */
    public synchronized void unregisterUnboundApp(int serviceId, AppID appid) throws IllegalArgumentException
    {
        // Invalid AppID?
        if (appid == null) throw new NullPointerException("appid is null");
        // Not MSO serviceId?
        if (serviceId <= 0x1FFFF) throw new IllegalArgumentException("invalid serviceId " + serviceId);

        // Get the service
        Integer key = new Integer(serviceId);
        AbstractServiceEntry entry = (AbstractServiceEntry) db.get(key);
        if (entry == null || entry.apps == null) return;

        // If the caller isn't the same AppID that registered the unbound
        // application then ignore request
        CallerContext cc = ccm.getCurrentContext();
        AppID ccAppID = (AppID) cc.get(CallerContext.APP_ID);

        // Iterate over apps, deciding what to keep and what to get rid of
        Vector newApps = new Vector();
        Vector toDelete = new Vector();
        for (Enumeration e = entry.apps.elements(); e.hasMoreElements();)
        {
            XAppEntry app = (XAppEntry)e.nextElement();
            if (!appid.equals(app.id))
            {
                // keep
                newApps.addElement(app);
            }
            else
            {
                // got a match for serviceId and AppId
                // Don't copy this one to newApps
                // If invalid, throw exception
                if (app.source != Xait.REGISTER_UNBOUND_APP)
                    throw new IllegalArgumentException("cannot modify non-registered app");

                // Only caller who did register of app can do unregister
                AppID regAppId = app.owner;
                if (regAppId != null && regAppId.equals(ccAppID))
                {
                    toDelete.addElement(app);
                    if (log.isInfoEnabled())
                    {
                        log.info("In unregisterUnboundApp and AppID matches");
                    }

                }
                else
                {
                    newApps.addElement(app);
                    if (log.isInfoEnabled())
                    {
                        log.info("In unregisterUnboundApp and AppID NOT matches");
                    }

                }
            }
        }
        // If none have been removed, simply return
        if (newApps.size() == entry.apps.size()) return;

        // Go with the updated set of apps
        entry.apps = newApps;

        // If the service has at least one application then keep it
        // in the database and signal the change.
        // Otherwise, signal that the service has been removed.
        if (entry.apps.size() > 0)
        {
            // Update the database
            db.put(key, entry);

            // Notify listeners that the service changed
            notifyUpdate(entry);
            postEvent(entry, SIChangeType.MODIFY);
        }
        else
        {
            // If this service is currently selected then keep it in the
            // database but mark it for removal. Otherwise, remove it from
            // the database.
            if (isServiceSelected(entry.id))
            {
                if (log.isInfoEnabled())
                {
                    log.info("Marking for REMOVAL 0x" + Integer.toHexString(entry.id));
                }

                entry.markedForRemoval = true;
                db.put(key, entry);
            }
            else
                db.remove(key);

            // Send notification that the service has been removed
            notifyUpdate(entry);
            postEvent(entry, SIChangeType.REMOVE);
        }

        // Delete the application(s) from storage
        deleteApps(toDelete);
    }

    /**
     * Invoked when new XAIT signalling is available. Will perform the following
     * steps:
     * <ol>
     * <li>Check with <code>AppSignalHandler</code>
     * <li>Update services database
     * <li>Notify listeners of changes
     * <li>Auto-select "new" auto-select services
     * </ol>
     * 
     * @param event
     *            an event object describing the new signalling
     */
    public void signallingReceived(SignallingEvent event)
    {
        Xait xait = (Xait) event.getSignalling();
        int source = xait.getSource();

        ProgressMgr progressMgr = (ProgressMgr) ProgressMgr.getInstance();
        progressMgr.signal(ProgressMgr.kXaitReceived);

        if (source == Xait.NETWORK_SIGNALLING)
            updateByNetwork(source, xait);
        else
            updateByRegister(source, xait);
    }

    /**
     * Locates the initial monitor application in the current signalling and
     * selects it into a new <code>ServiceContext</code>. After the service is
     * selected, this method will block waiting for the application to reach the
     * <i>LOADED</i> state and then wait for it to invoke the
     * {@link org.ocap.OcapSystem#monitorConfiguredSignal()} or the
     * {@link org.ocap.OcapSystem#monitorConfiguringSignal} followed by
     * <code>monitorConfiguredSignal()</code>.
     * <p>
     * If the app does not make it to the <i>LOADED</i> state then the launch is
     * considered to have failed.
     * <p>
     * After this method executes, a combination of the return value and the
     * current value of {@link #currIMA} indicates the results:
     * <table border>
     * <tr>
     * <th>Return</th>
     * <th>{@link #currIMA}</th>
     * <th>Results</th>
     * </tr>
     * <tr>
     * <td>false</td>
     * <td><code>null</code></td>
     * <td>No IMA found.</td>
     * </tr>
     * <tr>
     * <td>false</td>
     * <td>non-<code>null</code></td>
     * <td>IMA found, failed launch</td>
     * </tr>
     * <tr>
     * <td>true</td>
     * <td>non-<code>null</code></td>
     * <td>IMA found and launched</td>
     * </tr>
     * 
     * @return <code>true</code> if the launch was successful or there was no
     *         signaled monitor application; <code>false</code> if the launch
     *         was unsuccessful (for whatever reason)
     */
    private boolean launchMonApp()
    {
        ProgressMgr progressMgr = (ProgressMgr) ProgressMgr.getInstance();
        progressMgr.signal(ProgressMgr.kSearchingForMonitor);

        InitialMonitorApp newIma;
        synchronized (this)
        {
            // Forget the current monitor app if there is one. We are going to
            // search
            // the services database to locate the current monitor app which may
            // be
            // different from the one previously marked.
            if (currIMA != null)
            {
                currIMA.dispose();
                currIMA = null;
            }

            // Property indicates whether "resident" MonApp should be allowed
            if (log.isInfoEnabled())
            {
                log.info("Launching network MonApp");
            }

            // Find the monitor app
            newIma = findMonApp();

            // No IMA found
            if (newIma == null) return false;

            progressMgr.signal(ProgressMgr.kSelectMonitorService);

            // IMA located, attempt to launch
            currIMA = newIma;
        }

        if (!newIma.launch())
        {
            newIma.dispose();

            // IMA found, but launch failed
            return false;
        }

        // IMA found and launched
        Host.getInstance().setPowerMode(Host.LOW_POWER); // OCAP 20.2.1
        return true;
    }

    /**
     * Locates the <i>Initial Monitor Application</i> in the current signaling.
     * The initial monitor application is an application would satisfies the
     * following requirements:
     * <ul>
     * <li>Containing abstract service must have
     * {@link AbstractServiceEntry#autoSelect auto_select==true}
     * <li>Not an
     * {@link org.ocap.application.AppManagerProxy#registerUnboundApp
     * application-registered} application.
     * <li>Signaled with {@link AppEntry#controlCode app_control_code} of
     * {@link OcapAppAttributes#AUTOSTART AUTOSTART}
     * <li>Signaled with {@link AppEntry#priority app_priority} of 255
     * <li>Highest {@link AppEntry#launchOrder launch_order} version of
     * application
     * <li>Highest {@link AppEntry#version version} of application (if same or
     * no <code>launch_order</code>)
     * </ul>
     * 
     * @return an instance of <code>InitialMonitorApp</code> or
     *         <code>null</code> if none could be located
     */
    private synchronized InitialMonitorApp findMonApp()
    {
        ProgressMgr progressMgr = (ProgressMgr) ProgressMgr.getInstance();

        AbstractServiceEntry svc = null;
        XAppEntry app = null;

        // Find the monitor app
        for (Enumeration svcs = db.elements(); svc == null && svcs.hasMoreElements();)
        {
            AbstractServiceEntry currSvc = (AbstractServiceEntry) svcs.nextElement();

            // Monitor app service must be marked as autoselect
            if (!currSvc.autoSelect) continue;

            // Sort apps so that we consider highest
            // priority/launchOrder/version first
            XAppEntry[] sortedApps = sortAppsByPriority(currSvc.apps);

            // Monitor app must be the only app marked as autostart and
            // must have a priority of 255.
            AppID last = null;
            for (int i = 0; i < sortedApps.length; ++i)
            {
                XAppEntry currApp = sortedApps[i];

                // Skip non XAIT-signalled apps
                if (currApp.source != Xait.NETWORK_SIGNALLING)
                    continue;

                // Skip entry that doesn't win out on priority/launch
                // order/version
                AppID id = currApp.id;
                if (id.equals(last))
                    continue;
                last = id;

                if (log.isDebugEnabled())
                {
                    log.debug("Considering IMA: " + currApp);
                }

                // Test for monitor app
                if (currApp.controlCode == OcapAppAttributes.AUTOSTART)
                {
                    // As per OCAP 10.2.2.3.1, the MonApp must be the ONLY
                    // AUTO_START app in the service
                    if (svc != null)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Ignoring abstract service (" + svc
                                    + ") as candidate for IMA.  It contains more than one AUTOSTART application!");
                        }
                        app = null;
                        svc = null;
                        break;
                    }

                    // Take first IMA found
                    if (currApp.priority == 255)
                    {
                        progressMgr.signal(ProgressMgr.kFoundMonitorApp);
                        app = currApp;
                        svc = currSvc;
                    }
                }
            }

            // Have we found a monapp?
            if (app != null) break;
        }

        if (app == null)
        {
            SystemEventUtil.logEvent("No MonApp found");
            return null;
        }

        SystemEventUtil.logEvent("MonitorApp " + app.id + ":" + app.version + " found in service 0x"
                + Integer.toHexString(svc.id));
        return new InitialMonitorApp(svc, app);
    }
    
    /**
     * Handles updating of the services database given new <code>Xait</code>
     * information received from the
     * <code>AppManagerProxy.registerUnboundApp()</code> method.
     * <p>
     * This is covered by OCAP 10.2.2.2.2 "Abstract Services" for "When new
     * service and application information is provided through a call to
     * registerUnboundApp()..."
     * <ol type="a">
     * <li>When an abstract service descriptor is included in the xait parameter
     * w/out any associated apps and service is not currently selected, the
     * applications previously registered are deleted. If no applications remain
     * assocaited with this abstract service, then it is removed from the list
     * of available abstract services.
     * <li>When an abstract service descriptor is included in the xait parameter
     * w/out any associated apps and service is currently selected, the
     * applications previously registered are deleted. If no applcations remain
     * associated with this abstract service, then it is marked for removal and
     * will be removed when the presentation is stopped. The service is no
     * longer available for selection.
     * <li>newly signalled
     * <li>changed
     * <li>Abstract service descriptor not required to add or update (previously
     * registered) apps.
     * <li>Ignore apps that have no known service.
     * </ol>
     * <p>
     * How applications signalled in the xait fragment are handled is documented
     * in 10.2.2.3.2 "Applications registered by a previleged application":
     * <ol type="a>
     * <li>When an application that is currently signalled in the XAIT or an
     * application that is associated with a host manufacturer abstract service
     * is included in the xait fragment, then this application in the xait is
     * ignored.
     * <li>Other entries in the Applications Database for currently selected
     * abstract services are updated in the manner defined in 10.2.2.3.1,
     * Applications Signalled in the XAIT.
     * </ol>
     * 
     * @param source
     *            <code>REGISTER_UNBOUND_APP</code>
     * @param xait
     *            the parsed XAIT
     */
    private synchronized void updateByRegister(int source, Xait xait)
    {
        AbstractServiceEntry entries[] = xait.getServices();

        Hashtable newDb = new Hashtable();
        Vector autoSelect = new Vector();

        Vector toStore = new Vector(); // set of applications to save/update in
                                       // storage
        Vector toDelete = new Vector(); // set of applications to delete from
                                        // storage

        // Save the AppID of the caller
        CallerContext cc = ccm.getCurrentContext();
        AppID ccAppID = (AppID) cc.get(CallerContext.APP_ID);

        // Check for re-signalling
        boolean sameVersion = xait.getVersion() == lastRegisteredXAITVersion;
        lastRegisteredXAITVersion = xait.getVersion();

        int entriesNotChanged = 0;

        // Iterate over new entries to determine:
        // - Applications to remove
        // - Services to remove (or mark)
        // - added services
        // - added applications
        // - apps to store/delete from store
        for (int i = 0; i < entries.length; ++i)
        {
            AbstractServiceEntry entry = entries[i];

            Integer key = new Integer(entry.id);
            AbstractServiceEntry oldEntry = (AbstractServiceEntry) db.get(key);
            boolean alreadyMarkedForRemoval = false;

            // if references a new service...
            if (oldEntry == null || (alreadyMarkedForRemoval = oldEntry.markedForRemoval))
            {
                // Ignore services with no apps
                if (entry.apps == null || entry.apps.size() == 0) continue;

                // Create a duplicate, as we'll be modifying the entry
                entry = entry.copy();

                // Sort applications to make them easier to compare later
                entry.apps = sortAppsByVersion(entry.apps);

                // Save the callers AppID to be used by unregisterUnboundApp
                // method
                for (Enumeration e = entry.apps.elements(); e.hasMoreElements();)
                {
                    XAppEntry app = (XAppEntry)e.nextElement();
                    if (source == Xait.REGISTER_UNBOUND_APP)
                    {
                        app.owner = ccAppID;
                        if (log.isInfoEnabled())
                        {
                            log.info("updateByRegister - setting owner of " + app.id + " to " + ccAppID);
                        }
                }
                }

                // New apps to potentially be stored
                addBackgroundStorageApps(toStore, entry.apps, entry);

                // If the service is already marked for removal then notify that
                // it has been updated. Otherwise, add it to the auto-select
                // list if it is so marked.
                if (alreadyMarkedForRemoval)
                {
                    notifyUpdate(entry);
                    db.remove(key);
                }
                else if (entry.autoSelect) autoSelect.addElement(entry);

                // Add to new database
                newDb.put(key, entry);
                postEvent(entry, SIChangeType.ADD);
            }
            // If references an existing service...
            else
            {
                db.remove(key);

                // Create a duplicate, as we'll be modifying the entry
                entry = entry.copy();

                // Determine if changed!
                boolean autoSelectChanged = oldEntry.autoSelect != entry.autoSelect;

                // Sort applications to make them easier to compare later
                entry.apps = sortAppsByVersion(entry.apps);

                // Check for new/removed/changed applications and determine
                // if abstract service name changed
                Vector newApps = new Vector();
                Vector oldApps = new Vector();
                boolean appsChanged = reviewApps(source, oldEntry.apps, entry.apps, newApps, oldApps);
                entry.apps = newApps;

                boolean changed = autoSelectChanged || !oldEntry.name.equals(entry.name) || appsChanged;

                // Nothing has changed about this service, just add it to
                // our new db. Also notify our listeners of a change in this
                // service if it has any partially stored apps which need to be
                // stored again or maybe even re-autostarted
                if (!changed)
                {
                    newDb.put(key, entry);
                    Vector reStore = checkPartialStorage(entry);
                    addBackgroundStorageApps(toStore, reStore, entry);
                    continue;
                }

                // Remove all registered apps
                if (entry.apps.size() == 0)
                {
                    // We simply remove all registered apps!
                    // If no apps remain, then we remove the entry altogether
                    for (Enumeration e = oldEntry.apps.elements(); e.hasMoreElements();)
                    {
                        XAppEntry app = (XAppEntry)e.nextElement();

                        // Keep an app
                        if (app.source != source)
                        {
                            newApps.addElement(app);
                        }
                        // Remove an app
                        else
                        {
                            changed = true;
                        }
                    }
                }

                // If the service has at least one application then put it in
                // the new database and signal a change if there were any.
                // Otherwise, signal that the service has been removed.
                if (entry.apps.size() > 0)
                {
                    // Check for no more apps
                    // and no apps signaled as PRESENT or AUTOSTART...
                    boolean appsAutoStartOrPresent = false;
                    for (Enumeration e = entry.apps.elements(); e.hasMoreElements();)
                    {
                        XAppEntry app = (XAppEntry) e.nextElement();
                        int controlCode = app.controlCode;
                        if ((controlCode == OcapAppAttributes.AUTOSTART) || (controlCode == OcapAppAttributes.PRESENT))
                        {
                            appsAutoStartOrPresent = true;
                            break;
                        }
                    }

                    // entry that was not autoselect but now is
                    if (autoSelectChanged && entry.autoSelect)
                    {
                        autoSelect.addElement(entry);
                    }
                    // Replace entry in database
                    newDb.put(key, entry);

                    // Update what is stored
                    addBackgroundStorageApps(toStore, entry.apps, entry);
                    toDelete.addAll(oldApps);

                    // Notify listeners that the service changed
                    if (changed)
                    {
                        notifyUpdate(entry);
                        postEvent(entry, SIChangeType.MODIFY);
                    }
                    // If no apps Auto Start nor Present then stop the
                    // presentation.
                    else if (!appsAutoStartOrPresent)
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("No Apps AutoStart or Present for 0x" + Integer.toHexString(entry.id));
                        }
                        notifyUpdate(entry);
                    }
                    else
                    {
                        // If no change, keep only update time
                        entry.time = oldEntry.time;
                        entriesNotChanged++;
                    }
                }
                else
                {
                    // Update what is stored (delete it all)
                    toDelete.addAll(newApps);
                    toDelete.addAll(oldApps);

                    // If this service is currently selected then keep it in the
                    // database but mark it for removal.
                    if (isServiceSelected(entry.id))
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("Marking for REMOVAL 0x" + Integer.toHexString(entry.id));
                        }

                        entry.markedForRemoval = true;
                        newDb.put(key, entry);
                    }

                    // Send notification that the service has been removed
                    notifyUpdate(entry);
                    postEvent(entry, SIChangeType.REMOVE);
                }
            }
        }

        // Now look at applications for which there is no abstract service
        // descriptor
        // Generate a mapping of serviceId->apps.
        Hashtable newApps = new Hashtable();
        XAppEntry[] apps = (XAppEntry[])xait.getApps();
        for (int i = 0; i < apps.length; ++i)
        {
            XAppEntry app = apps[i];
            Integer key = new Integer(app.serviceId);

            // This is an app for which there is no Abstract Service Descriptor.
            // But there is a known abstract service.
            if ((newDb.get(key) == null) && (db.get(key) != null))
            {
                Vector vector = (Vector) newApps.get(key);
                if (vector == null)
                {
                    vector = new Vector();
                    newApps.put(key, vector);
                }
                vector.addElement(app);
                if (source == Xait.REGISTER_UNBOUND_APP)
                {
                    app.owner = ccAppID;
                    if (log.isInfoEnabled())
                    {
                        log.info("updateByRegister - setting owner of " + app.id + " to " + ccAppID);
                    }
            }
        }
        }

        // Update old service (not found in current XAIT) with new apps
        for (Enumeration e = newApps.elements(); e.hasMoreElements();)
        {
            Vector vector = sortAppsByVersion((Vector) e.nextElement());
            Integer key = new Integer(((XAppEntry)vector.elementAt(0)).serviceId);
            AbstractServiceEntry entry = (AbstractServiceEntry) db.get(key);
            Vector newVector = new Vector();
            boolean changed = reviewApps(source, entry.apps, vector, newVector, null);
            entry = entry.copy();
            entry.apps = newVector;
            addBackgroundStorageApps(toStore, newVector, entry);

            db.remove(key);
            newDb.put(key, entry);

            // Notify listeners of this change (new app)
            if (changed)
            {
                notifyUpdate(entry);
                postEvent(entry, SIChangeType.MODIFY);

                // Just reset our entriesNotChanged value here, this will ensure
                // that we don't see this XAIT as a resignalling of a previous
                // one
                entriesNotChanged = 0;
            }
        }

        // What remains in db is ignored, should be copied to newDb
        for (Enumeration e = db.elements(); e.hasMoreElements();)
        {
            AbstractServiceEntry entry = (AbstractServiceEntry) e.nextElement();

            newDb.put(new Integer(entry.id), entry);

            // There aren't any changes, so don't bother updating storage
        }

        // Update database
        db = newDb;

        // Request storage/deletion
        storeApps(toStore, toDelete);

        // This is the same XAIT version and we did not find any changes, so
        // this
        // must be a re-signal because of a transport protocol communication
        // failure.
        // Just notify all our listeners that this is a resignal. We do the
        // "entriesNotChanged" check because it is unclear in the spec whether
        // each
        // XAIT fragment passed to registerUnboundApp() must contain a new
        // version
        // number, so the only way we can be sure that the signalling is
        // identical
        // is to make sure no abstract service entries have changed
        if (sameVersion && entriesNotChanged == entries.length)
        {
            for (int i = 0; i < entries.length; i++)
            {
                AbstractServiceEntry entry = entries[i];
                entry.resignal = true;
                notifyUpdate(entry);
            }
        }
        else
        {
            // Perform autoSelect
            if (autoSelectEnabled)
            {
                final Enumeration e = autoSelect.elements();
                AccessController.doPrivileged(new PrivilegedAction()
                {
                    public Object run()
                    {
                        autoSelect(e, false);
                        return null;
                    }
                });
            }
        }
    }

    /**
     * Handles updating of the services database given new <code>Xait</code>
     * information received from the network.
     * <p>
     * This is covered by OCAP 10.2.2.2.2 "Abstract Services" for
     * "When a new version of the XAIT is recieved...":
     * <ol type="a">
     * <li>When an abstractservice descriptor is no longer signaled in the XAIT
     * and it is not currently selected, it should be removed from the list of
     * available abstract services. An SIChangeEvent of type REMOVE is
     * generated.
     * <li>When an abstract service descriptor is no longer signaled in the XAIT
     * and the service is currently selected, the abstract service is marked for
     * removal and will be removed when presentation of the abstract service is
     * stopped. An SIChangeEvent of type REMOVE is generated when the abstract
     * service is marked for removal and the abstract service is no longer
     * available for selection. Any attempt to select an abstract service that
     * is marked for removal results in an InvalidServiceComponentException from
     * the ServiceContext.select() method.
     * <li>newly signaled
     * <li>changed
     * </ol>
     * <p>
     * How applications signalled in the XAIT are handled is documented in
     * 10.2.2.3.1 "Applications Signaled in the XAIT":
     * <ol type="a">
     * <li>Added apps.
     * <li>When an application currently exists in the Application Database that
     * was previously signalled in the XAIT but is no longer signalled, then the
     * implementation MUST destroy any active application instance and remove
     * the application from the Application Database.
     * <li>Same version, modified.
     * <li>New version, inactive application.
     * <li>New version, active application.
     * </ol>
     * 
     * @param source
     *            <code>NETWORK_SIGNALLING</code>
     * @param xait
     *            the parsed XAIT
     */
    private synchronized void updateByNetwork(int source, Xait xait)
    {
        // If the initial XAIT is acquired, but service auto-selection is
        // not yet enabled, then we are still in the middle of the boot
        // process. Ignore and re-signal new network-based XAIT signalling
        // until after after the boot process is complete.
        if (xaitAcquired.get() && !autoSelectEnabled)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Postponing updated XAIT signalling until after boot process is complete");
            }
            sm.resignal(xait);
            return;
        }

        AbstractServiceEntry entries[] = xait.getServices();

        Hashtable newDb = new Hashtable();
        Vector autoSelect = new Vector();

        Vector toStore = new Vector(); // set of applications to save/update in
                                       // storage
        Vector toDelete = new Vector(); // set of applications to delete from
                                        // storage

        // Update last XAIT version
        boolean sameVersion = (xait.getVersion() == lastNetworkXAITVersion);
        lastNetworkXAITVersion = xait.getVersion();

        // Iterate over new entries to determine:
        // - removed services
        // - added services
        // - modified services
        // - apps to store/delete from store
        for (int i = 0; i < entries.length; ++i)
        {
            AbstractServiceEntry entry = entries[i];

            // Skip unless service defined by MSO
            if (entry.id < 0x020000 || entry.id > 0xFFFFFF) continue;

            Integer key = new Integer(entry.id);
            AbstractServiceEntry oldEntry = (AbstractServiceEntry) db.get(key);
            boolean alreadyMarkedForRemoval = false;

            // If references a new service...
            if (oldEntry == null || (alreadyMarkedForRemoval = oldEntry.markedForRemoval))
            {
                // Ignore services with no apps -- except those that are associated
                // with new application versions
                if (!entry.hasNewAppVersions && entry.apps.size() == 0)
                {
                    continue;
                }

                // Create a duplicate, as we'll be modifying the entry
                entry = entry.copy();

                // Sort applications to make them easier to compare later
                entry.apps = sortAppsByVersion(entry.apps);

                // Add new apps to potentially be background stored if they will
                // not be auto-started
                addBackgroundStorageApps(toStore, entry.apps, entry);

                // If the service is already marked for removal then notify that
                // it has been updated. Otherwise, add it to the auto-select
                // list if it is so marked.
                if (alreadyMarkedForRemoval)
                {
                    notifyUpdate(entry);
                    db.remove(key);
                }
                else if (entry.autoSelect)
                    autoSelect.addElement(entry);

                // Add to new database
                if (log.isDebugEnabled())
                {
                    log.debug("Adding service: " + entry);
                }
                newDb.put(key, entry);
                postEvent(entry, SIChangeType.ADD);
            }

            // If references an existing service...
            else
            {
                db.remove(key);

                // Create a duplicate, as we'll be modifying the entry
                entry = entry.copy();

                // Determine if changed!
                boolean autoSelectChanged = oldEntry.autoSelect != entry.autoSelect;

                // Sort applications to make them easier to compare later
                entry.apps = sortAppsByVersion(entry.apps);

                // Check for new/removed/changed applications
                Vector newApps = new Vector();
                Vector oldApps = new Vector();
                boolean appsChanged = reviewApps(source, oldEntry.apps, entry.apps, newApps, oldApps);
                entry.apps = newApps;

                boolean changed = autoSelectChanged || !oldEntry.name.equals(entry.name) || appsChanged;

                // Nothing has changed about this service, just add it to
                // our new db. Also notify our listeners of a change in this
                // service if it has any partially stored apps which need to be
                // stored again or maybe even re-autostarted
                if (!changed)
                {
                    newDb.put(key, entry);
                    Vector reStore = checkPartialStorage(entry);
                    addBackgroundStorageApps(toStore, reStore, entry);
                    continue;
                }

                // If the service has at least one application then put it in
                // the new database and signal a change if there were any.
                // Otherwise, signal that the service has been removed.
                if (entry.apps.size() > 0 || entry.hasNewAppVersions)
                {
                    // entry that was not autoselect but now is
                    if (autoSelectChanged && entry.autoSelect)
                    {
                        autoSelect.addElement(entry);
                    }

                    // Replace entry in database
                    newDb.put(key, entry);

                    // Update what is stored
                    addBackgroundStorageApps(toStore, newApps, entry);
                    toDelete.addAll(oldApps);

                    // Notify listeners that the service changed
                    if (changed)
                    {
                        notifyUpdate(entry);
                        postEvent(entry, SIChangeType.MODIFY);
                    }
                    else
                    {
                        // If no change, keep only update time
                        entry.time = oldEntry.time;
                    }
                }
                else
                {
                    // Update what is stored (delete all)
                    toDelete.addAll(newApps);
                    toDelete.addAll(oldApps);

                    // If this service is currently selected then keep it in the
                    // database but mark it for removal.
                    if (isServiceSelected(entry.id))
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("Marking for REMOVAL 0x" + Integer.toHexString(entry.id));
                        }

                        entry.markedForRemoval = true;
                        newDb.put(key, entry);
                    }

                    // Send notification that the service has been removed
                    notifyUpdate(entry);
                    postEvent(entry, SIChangeType.REMOVE);
                }
            }
        }

        // Now look at applications for which there is no abstract service
        // descriptor
        XAppEntry[] apps = (XAppEntry[])xait.getApps();
        for (int i = 0; i < apps.length; ++i)
        {
            XAppEntry app = apps[i];
            Integer key = new Integer(app.serviceId);

            // This is an app for which there is no Abstract Service Descriptor.
            // But there is a known abstract service.
            if ((newDb.get(key) == null) && (db.get(key) != null))
            {
                AbstractServiceEntry entry = (AbstractServiceEntry) db.get(key);
                if (isServiceSelected(entry.id))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Marking for REMOVAL 0x" + Integer.toHexString(entry.id));
                    }

                    entry.markedForRemoval = true;
                    newDb.put(key, entry);
                }
                else
                {
                    db.remove(key);
                }
            }
        }

        boolean monappServiceDeleted = false;

        // What remains in db is set of deleted services and services which
        // were not network-signalled. Remove the former but keep the latter.
        for (Enumeration e = db.elements(); e.hasMoreElements();)
        {
            AbstractServiceEntry entry = (AbstractServiceEntry) e.nextElement();

            // If the service containing the monapp is to be deleted, then
            // begin the reboot process
            if (currIMA != null && currIMA.isMonApp(entry))
            {
                monappServiceDeleted = true;
            }

            // Keep host-device manufacturer services or services already marked
            // for removal.
            if (isHostService(entry.id) || entry.markedForRemoval)
            {
                newDb.put(new Integer(entry.id), entry);
                continue;
            }

            // Determine whether this service has any registered apps
            boolean hasRegisteredApps = false;
            Enumeration e2 = entry.apps.elements();
            while (e2.hasMoreElements())
            {
                XAppEntry app = (XAppEntry)e2.nextElement();
                if (app.source == Xait.REGISTER_UNBOUND_APP)
                {
                    hasRegisteredApps = true;
                    break;
                }
            }

            // If this service has any registered apps, then keep it but
            // remove any network-signalled apps.
            if (hasRegisteredApps)
            {
                boolean hasNetworkApps = false;
                Vector newApps = new Vector();
                e2 = entry.apps.elements();
                while (e2.hasMoreElements())
                {
                    XAppEntry app = (XAppEntry)e2.nextElement();
                    if (app.source == Xait.REGISTER_UNBOUND_APP)
                    {
                        // Keep apps which were registered
                        newApps.add(app);
                    }
                    else
                    {
                        // Remove apps which were network signaled
                        hasNetworkApps = true;
                        toDelete.add(app);
                    }
                }

                // If we deleted any apps then notify. Add the new entry
                // containing
                // only registered apps to the new database.
                entry = entry.copy();
                entry.apps = newApps;
                if (hasNetworkApps) notifyUpdate(entry);
                newDb.put(new Integer(entry.id), entry);
                continue;
            }

            // If this service is currently selected then keep it in the
            // database but mark it for removal.
            if (isServiceSelected(entry.id))
            {
                if (log.isInfoEnabled())
                {
                    log.info("Marking for REMOVAL 0x" + Integer.toHexString(entry.id));
                }

                // Keep it in the database and mark it for removal
                entry = entry.copy();
                entry.markedForRemoval = true;
                newDb.put(new Integer(entry.id), entry);
            }

            // Delete service's apps from storage
            toDelete.addAll(entry.apps);

            // Send notification that the service has been removed
            notifyUpdate(entry);
            postEvent(entry, SIChangeType.REMOVE);
        }

        // Update database
        db = newDb;

        // Handle authentication - update privileged certificates
        byte[] privCerts = xait.getPrivilegedCertificateBytes();
        AuthManager auth = (AuthManager) ManagerManager.getInstance(AuthManager.class);
        auth.setPrivilegedCerts(privCerts);

        // Handle storage - update privileged certificates
        asm.updatePrivilegedCertificates(privCerts);

        // If the IMA is not running and we have a signaled IMA, start the
        // reboot process
        if (autoSelectEnabled)
        {
            if ((currIMA == null || !currIMA.isRunning() || monappServiceDeleted) && findMonApp() != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("New MonApp detected with no MonApp currently running! Attempting to start the boot process...");
                }
                restartMonitorApp();
                return;
            }
        }

        // Request storage/deletion
        storeApps(toStore, toDelete);

        // Check for re-signalling
        if (sameVersion)
        {
            // Just notify our listeners that these entries are being
            // re-signaled
            for (int i = 0; i < entries.length; ++i)
            {
                AbstractServiceEntry entry = entries[i];
                entry.resignal = true;
                notifyUpdate(entry);
            }
        }
        else
        {
            // Perform autoSelect
            if (autoSelectEnabled) autoSelect(autoSelect.elements(), false);
        }

        // In case we are waiting to receive initial XAIT
        xaitAcquired.set(true);
    }

    /**
     * Analyzes the given <code>AbstractServiceEntry</code> looking for
     * partially stored apps. If any autostart apps are partially stored, the
     * service's registered listener is notified so that it may re-autostart
     * those apps. Non-autostart apps that are partially stored are returned.
     * 
     * @param service
     *            the abstract service entry to analyze
     * @return a list of non-autostart apps that are partially stored
     */
    private Vector checkPartialStorage(AbstractServiceEntry service)
    {
        Vector backgroundStore = new Vector();
        boolean notify = false;

        for (Iterator iter = service.apps.iterator(); iter.hasNext();)
        {
            XAppEntry app = (XAppEntry)iter.next();
            if (asm.isPartiallyStored(app.id, app.version) ||
                (asm.retrieveApp(app.id, app.version, app.className) == null &&
                 app.storagePriority != 0))
            {
                if (app.controlCode == OcapAppAttributes.AUTOSTART)
                    notify = true;
                else
                    backgroundStore.add(app);
            }
        }

        if (notify) notifyUpdate(service);

        return backgroundStore;
    }

    /**
     * Add apps to the given list if they qualify for background storage. An app
     * qualifies if it will not be auto-started
     * 
     * @param dest
     *            the vector where qualifying apps will be added
     * @param candidateApps
     *            apps to be considered for background storage. elements are
     *            instance of <code>XAppSignalling</code>
     * @param entry
     *            the abstract service in which these apps were signaled
     */
    private void addBackgroundStorageApps(Vector dest, Vector candidateApps, AbstractServiceEntry entry)
    {
        // Add new apps to potentially be background stored if they will not be
        // auto-started.  Also, do not store apps if they use LocalTransportProtocol
        for (Enumeration e = candidateApps.elements(); e.hasMoreElements();)
        {
            XAppEntry app = (XAppEntry)e.nextElement();
            if ((entry.autoSelect && app.controlCode == OcapAppAttributes.AUTOSTART) ||
                app.transportProtocols[0] instanceof LocalTransportProtocol)
                continue;
            
            dest.add(app);
        }
    }

    /**
     * Requests that the {@link AppStorageManager} store and/or delete the given
     * applications in the background.
     * 
     * @param toStore
     *            the list of applications to store in the background
     * @param toDelete
     *            the list of applications to delete in the background
     */
    private void storeApps(Vector toStore, Vector toDelete)
    {
        // First delete apps (in case will free up some space for storage)
        for (Enumeration e = toDelete.elements(); e.hasMoreElements();)
        {
            deleteApp(asm, (XAppEntry)e.nextElement());
        }

        // Sort this list based upon storagePriority.
        // So that we don't try and store lower-priority only to be replaced by
        // higher-priority apps.
        XAppEntry[] sorted = sortAppsByStoragePriority(toStore);

        // Now store the apps (in that order)
        for (int i = 0; i < sorted.length; ++i)
        {
            XAppEntry entry = sorted[i];
            
            // We may just need to update our storage priority if this app is
            // already stored
            AppStorage storage = asm.retrieveApp(entry.id, entry.version, entry.className);
            if (storage != null)
            {
                if (storage.getStoragePriority() != entry.storagePriority)
                    asm.updateStoragePriority(storage, entry.storagePriority);
            }
            else if (entry.storagePriority != 0)
            {
                // According to MHP1.0.3 10.7.3, when an app is signaled as delivered
                // via multiple transport protocols, the implementation can choose
                // a single one for the lifetime of the app.  Right now, we simply choose
                // the first one, but this could be made smarter in the future
                AppEntry.TransportProtocol tp = entry.transportProtocols[0];
        
                if (log.isDebugEnabled())
                {
                    log.debug("App to background storage: " + entry);
                }
                
                asm.backgroundStoreApp(entry, tp);
            }
        }
    }

    /**
     * Requests that the <code>AppStorageManager</code> delete the given
     * application.
     * 
     * @param toDelete
     *            apps to delete from storage
     */
    private void deleteApps(Vector toDelete)
    {
        // First delete apps (in case will free up some space for storage)
        for (Enumeration e = toDelete.elements(); e.hasMoreElements();)
        {
            deleteApp(asm, (XAppEntry)e.nextElement());
        }
    }

    /**
     * Requests that the <code>AppStorageManager</code> delete the given
     * application.
     * 
     * @param asm
     *            instance of <code>AppStorageManager</code>
     * @param app
     *            describes the app to delete
     */
    private void deleteApp(AppStorageManager asm, XAppEntry app)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Delete from storage: " + app);
        }
        asm.deleteApp(app.id, app.version);
    }

    /**
     * Reviews the set of <i>newApps</i> against the set of <i>oldApps</i>. Puts
     * the results into <i>keep</i>.
     * 
     * @param source
     *            the source of the <i>newApps</i> (changes where sources differ
     *            between <i>oldApps</i> and <i>newApps</i> are ignored)
     * @param oldApps
     * @param newApps
     * @param keep
     *            in the end, holds the sorted contents of <i>oldApps</i>,
     *            <i>newApps</i>, or a combination of both
     * @param delete
     *            in the end, holds the apps in oldApps, but not newApps; if
     *            <code>null</code> then don't delete any apps
     * 
     * @return changed
     */
    private boolean reviewApps(int source, Vector oldApps, Vector newApps, Vector keep, Vector delete)
    {
        int i0 = 0;
        int i1 = 0;
        final int n0 = oldApps.size();
        final int n1 = newApps.size();

        boolean changed = n0 != n1;

        // Save the AppID of the caller
        CallerContext cc = ccm.getCurrentContext();
        AppID ccAppID = (AppID) cc.get(CallerContext.APP_ID);

        // Iterate over sorted set of apps
        while (i0 < n0 && i1 < n1)
        {
            XAppEntry oldApp = (XAppEntry)oldApps.elementAt(i0);
            XAppEntry newApp = (XAppEntry)newApps.elementAt(i1);

            int cmp = compare(oldApp, newApp, false);

            // If signalling refers to the same apps...
            if (cmp == 0)
            {
                // Consider sources
                // Consider control code
                if (oldApp.source != source)
                {
                    // Ignore change from different source
                    keep.addElement(oldApp);
                }
                else
                {
                    // Determine if changed
                    if (!changed)
                    {
                        changed = isAppModified(oldApp, newApp);
                        
                        // Check for change in new versions
                        if (!changed)
                        {
                            XAppEntry oldNewVersion = oldApp.newVersion;
                            XAppEntry newNewVersion = newApp.newVersion;
                            while (oldNewVersion != null && newNewVersion != null)
                            {
                                if (isAppModified(oldNewVersion,newNewVersion))
                                {
                                    changed = true;
                                    break;
                                }
                                oldNewVersion = oldNewVersion.newVersion;
                                newNewVersion = newNewVersion.newVersion;
                            }
                            if (oldNewVersion != null || newNewVersion != null)
                            {
                                changed = true;
                            }
                        }
                    }

                    // Copy the monitor app flag and owner from the old to the
                    // new AppEntry
                    newApp.isMonitorApp = oldApp.isMonitorApp;
                    newApp.owner = oldApp.owner;
                    keep.addElement(newApp);
                }
                ++i0;
                ++i1;
            }
            // old < new: oldApp was removed!
            else if (cmp < 0)
            {
                // Consider sources
                if (oldApp.source != source || delete == null)
                {
                    // Ignore deletion from different source
                    keep.addElement(oldApp);
                }
                else
                {
                    delete.addElement(oldApp);
                    changed = true;
                }
                ++i0;
            }
            // old > new: newApp was added!
            else
            {
                // Set app id of caller
                if (source == Xait.REGISTER_UNBOUND_APP)
                {
                    newApp.owner = ccAppID;
                }

                changed = true;
                keep.addElement(newApp);
                ++i1;
            }
        }
        // old apps not referenced
        while (i0 < n0)
        {
            XAppEntry oldApp = (XAppEntry)oldApps.elementAt(i0);
            if (oldApp.source != source || delete == null)
            {
                // Ignore deletion from different source
                keep.addElement(oldApp);
            }
            else
            {
                delete.addElement(oldApp);
                changed = true;
            }
            ++i0;
        }
        // new apps
        while (i1 < n1)
        {
            // Set app id of caller
            XAppEntry newApp = (XAppEntry)newApps.elementAt(i1);
            if (source == Xait.REGISTER_UNBOUND_APP)
            {
                newApp.owner = ccAppID;
            }
            changed = true;
            keep.addElement(newApp);
            ++i1;
        }

        return changed;
    }

    /**
     * isAppModified compare two <code>AppEntry</code>s.
     * 
     * @param oldAppEntry
     * @param newAppEntry
     * @return <code>true</code> if AppEntry's differ.
     */
    private boolean isAppModified(XAppEntry oldAppEntry, XAppEntry newAppEntry)
    {
        boolean changed = oldAppEntry.controlCode != newAppEntry.controlCode
                || oldAppEntry.serviceBound != newAppEntry.serviceBound || oldAppEntry.priority != newAppEntry.priority
                || oldAppEntry.visibility != newAppEntry.visibility || oldAppEntry.iconFlags != newAppEntry.iconFlags
                || oldAppEntry.names.size() != newAppEntry.names.size()
                || oldAppEntry.version != newAppEntry.version || oldAppEntry.launchOrder != newAppEntry.launchOrder
                || oldAppEntry.storagePriority != newAppEntry.storagePriority;

        if (changed)
        {
            return changed;
        }

        String[][] newnames = new String[newAppEntry.names.size()][];
        String[][] oldnames = new String[oldAppEntry.names.size()][];

        changed = oldnames.length != newnames.length;

        if (!changed)
        {
            // check if names are different
            int i = 0;
            for (Enumeration e = newAppEntry.names.keys(); e.hasMoreElements();)
            {
                String lang = (String) e.nextElement();
                String name = (String) newAppEntry.names.get(lang);
                newnames[i++] = new String[] { lang, name };
            }

            i = 0;
            for (Enumeration e = oldAppEntry.names.keys(); e.hasMoreElements();)
            {
                String lang = (String) e.nextElement();
                String name = (String) oldAppEntry.names.get(lang);
                oldnames[i++] = new String[] { lang, name };
            }

            for (i = 0; i < oldnames.length; i++)
            {
                boolean found = false;
                for (int j = 0; j < newnames.length; j++)
                {
                    // name[i][0] is the language code
                    // name[i][1] is the name
                    // Safe to use Arrays.equals() here because the order should
                    // be identical
                    if (Arrays.equals(oldnames[i], newnames[j]))
                    {
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    changed = true;
                    break;
                }
            }
        }
        
        return changed;
    }

    /**
     * Compares the two <code>AppID</code>s.
     * 
     * @return <code><i>id1</i> - <i>id2</i></code>
     */
    private int compare(AppID id1, AppID id2)
    {
        int cmp = id1.getOID() - id2.getOID();
        return (cmp == 0) ? (id1.getAID() - id2.getAID()) : cmp;
    }

    /**
     * Compares the two <code>XAppSignalling</code> entries. The following
     * fields are compared as necessary, in order:
     * <ul>
     * <li> {@link AppEntry#id}
     * <li> {@link XAppEntry#version}
     * <li> {@link AppEntry#priority} (only if <i>beyondIdVer</i>)
     * <li> {@link XAppEntry#launchOrder} (only if <i>beyondIdVer</i>)
     * </ul>
     * 
     * Note that <i>app1</i> and <i>app2</i> parameters are swapped so that we
     * sort in decreasing order.
     * 
     * @param app2
     *            app 2
     * @param app1
     *            app 1
     * @param beyondIdVer
     *            if <code>true</code> then <i>priority</i> and
     *            <i>launchOrder</i> are considered; if <code>false</code> then
     *            they are not considered
     * 
     * @return &lt; 0 if <i>app1</i> is "less than" <i>app2</i>; 0 if
     *         <i>app1</i> "equals" <i>app2</i>; &gt; if <i>app1</i> is
     *         "greater than" <i>app2</i>
     * 
     * @see #reviewApps
     * @see #sortAppsByVersion
     */
    private int compare(XAppEntry app2, XAppEntry app1, boolean beyondIdVer)
    {
        // Consider AppID first
        int cmp = compare(app1.id, app2.id);
        if (cmp == 0)
        {
            // Consider version next (AppID+version should uniquely identify apps)
            // Note: for our purposes, version numbers don't really imply an
            // ordering -- we just use it to differentiate
            if (app1.version - app2.version < 0)
            {
                return -1;
            }
            else if (app1.version - app2.version > 0)
            {
                return 1;
            }
            else
            {
                // Finally, compare on priority/launchOrder
                cmp = app1.priority - app2.priority;
                if (cmp == 0)
                {
                    cmp = app1.launchOrder - app2.launchOrder;
                }
            }
        }
        return cmp;
    }

    /**
     * Sorts the applications and returns a new <code>Vector</code>. Returns a
     * copy of the original <code>Vector</code>.
     * <p>
     * Applications are sorted such that entries with the same AppID and version
     * are grouped together. Within a group of application entries of the same
     * AppID and version the entries will be sorted in descending order based
     * upon priority and launch order.
     * <p>
     * This method of sorting is used to maintain the list of applications in an
     * <code>AbstractServiceEntry</code> in an order optimized for finding
     * changes between versions of the XAIT.
     * 
     * @param apps
     *            <code>Vector</code> of <code>XAppSignalling</code> entries
     * @return <code>Vector</code> of <code>XAppSignalling</code> entries; a
     *         sorted (in descending order based upon <code>AppID:version</code>
     *         ) copy of the original <code>Vector</code>
     * 
     * @see #updateByNetwork(int, Xait)
     * @see #updateByRegister(int, Xait)
     */
    private Vector sortAppsByVersion(Vector apps)
    {
        Vector sorted = (Vector) apps.clone();

        Collections.sort(sorted, new Comparator()
        {
            // Note: compare(XAppSignalling,XAppSignalling) reverses the params
            // to ensure descending order
            public int compare(Object a, Object b)
            {
                return ServicesDatabaseImpl.this.compare((XAppEntry) a, (XAppEntry) b, true);
            }
        });

        return sorted;
    }

    /**
     * Returns the given <code>Vector</code> of apps as a sorted array.
     * <p>
     * Applications are sorted such that entries with the same AppID are grouped
     * together. Within a group of application entries of the same AppID,
     * entries will be sorted in descending order based upon priority, launch
     * order, and finally version.
     * <p>
     * This method of sorting is the same that is used for population of the
     * {@link org.dvb.application.AppsDatabase apps database} and application
     * selection employed by {@link AbstractService#getAppAttributes()}.
     * 
     * @param apps
     *            <code>Vector</code> of <code>XAppEntry</code> entries
     * @return an <code>XAppEntry[]</code> sorted in descending order based
     *         upon <code>AppID</code>, priority, launch order, and version
     * 
     * @see #findMonApp()
     */
    private XAppEntry[] sortAppsByPriority(Vector apps)
    {
        return sortArray(apps, new Comparator()
        {
            // Reversed to ensure sorted in descending order
            public int compare(Object a2, Object a1)
            {
                XAppEntry ae1 = (XAppEntry) a1;
                XAppEntry ae2 = (XAppEntry) a2;

                int cmp = ServicesDatabaseImpl.this.compare(ae1.id, ae2.id);
                if (cmp == 0)
                {
                    cmp = ae1.priority - ae2.priority;
                    if (cmp == 0)
                    {
                        cmp = ae1.launchOrder - ae2.launchOrder;
                        if (cmp == 0)
                        {
                            if (ae1.version - ae2.version < 0)
                            {
                                return -1;
                            }
                            else if (ae1.version - ae2.version > 0)
                            {
                                return 1;
                            }
                            else
                            {
                                return 0;
                            }
                        }
                    }
                }
                return cmp;
            }
        });
    }

    /**
     * Returns the given <code>Vector</code> of apps as a sorted array.
     * <p>
     * Applications are sorted in descending order by storage priority.
     * 
     * @param apps
     *            <code>Vector</code> of <code>XAppEntry</code> entries
     * @return an <code>XAppEntry[]</code> having been sorted in descending
     *         order based upon storage priority
     * 
     * @see #storeApps(Vector, Vector)
     */
    private XAppEntry[] sortAppsByStoragePriority(Vector apps)
    {
        return sortArray(apps, new Comparator()
        {
            // parameters are reversed to get a descending order list
            public int compare(Object o2, Object o1)
            {
                XAppEntry a1 = (XAppEntry) o1;
                XAppEntry a2 = (XAppEntry) o2;
                return a1.storagePriority - a2.storagePriority;
            }
        });
    }

    /**
     * Sorts the given <code>Vector</code> according to the given compare
     * strategy. Returns the sorted contents as an array.
     * 
     * @param vector
     * @param compare
     * @return sorted array of the original vector
     */
    private XAppEntry[] sortArray(Vector vector, Comparator compare)
    {
        XAppEntry[] array = new XAppEntry[vector.size()];

        if (vector.size() > 0)
        {
            vector.copyInto(array);

            Arrays.sort(array, compare);
        }

        return array;
    }

    /**
     * Auto-selects the given service, assuming that all necessary checks have
     * already been performed.
     * 
     *
     * @param entry
     *            the service to auto-select
     * @param notifyInitialAutoStartAppsStarted
     * @return the <code>ServiceContext</code> within which the service is
     *         selected
     */
    private ServiceContext autoSelect(ServiceContextFactory scf, AbstractServiceEntry entry, boolean notifyInitialAutoStartAppsStarted)
    {
        ServiceContext sc = null;
        try
        {
            synchronized(this)
            {
                sc = ((ServiceContextFactoryExt) scf).createAutoSelectServiceContext();
                if (notifyInitialAutoStartAppsStarted)
                {
                    sc.addListener(serviceContextListener);
                    serviceContexts.add(sc);
                }
            }

            sc.select(new AbstractServiceImpl(entry));
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError("AutoSelect abstract service could not be selected: " + entry, e);

            return null;
        }
        return sc;
    }

    /**
     * Auto-selects the services contained in the given <code>Enumeration</code>
     * .
     * 
     * @param e
     *            <code>Enumeration</code> of services to
     *            <code>autoSelect</code>
     * @param notifyInitialAutoStartAppsStarted true if listeners should be registered for ServiceContextEvents and notification
     *                                          sent when all ServiceContextEvents are received
     */
    private void autoSelect(Enumeration e, boolean notifyInitialAutoStartAppsStarted)
    {
        ServiceContextFactory scf = ServiceContextFactory.getInstance();

        NEXT:
        while (e.hasMoreElements())
        {
            AbstractServiceEntry entry = (AbstractServiceEntry) e.nextElement();
            if (log.isInfoEnabled())
            {
                log.info("AutoSelect AbstractService 0x" + Integer.toHexString(entry.id));
            }

            // Skip if currently SELECTED! Only necessary for NEWLY_AUTOSELECT
            if (NEWLY_AUTOSELECT)
            {
                if (isServiceSelected(entry.id))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("AbstractService 0x" + Integer.toHexString(entry.id) + " already selected");
                    }
                    continue NEXT;
                }
            }

            autoSelect(scf, entry, notifyInitialAutoStartAppsStarted);
        }
    }

    /**
     * Returns whether the given service is a host-device manufacturer service
     * or not.
     * 
     * @param id
     *            the service id to test
     * @return <code>id &gt;= 0x010000 && id &lt;= 0x01FFFF</code>
     */
    private boolean isHostService(int id)
    {
        return id >= 0x010000 && id <= 0x01FFFF;
    }

    /**
     * Loads information about resident abstract services into the database.
     * Does not auto-select services.
     */
    private void loadHostApps()
    {
        if (log.isDebugEnabled())
        {
            log.debug("ENTERING loadHostApps()");
        }

        InputStream is = OcapMain.class.getResourceAsStream("/" + HOSTAPPS);
        if (is == null)
            is = OcapMain.class.getResourceAsStream(HOSTAPPS);
        if (is == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("No '" + HOSTAPPS + "' file found");
            }
            return;
        }

        if (log.isDebugEnabled())
        {
            log.debug("Creating XaitProps");
        }
        HostappProps hostappProps = new HostappProps(Xait.HOST_DEVICE);
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("Parsing XaitProps");
            }
            hostappProps.parse(is, -1);
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(new Exception("Could not read '" + HOSTAPPS + "' " + e));

            return;
        }

        // Install these services directly
        Xait xait = (Xait)hostappProps.getSignalling();
        AbstractServiceEntry[] entries = xait.getServices();
        if (log.isDebugEnabled())
        {
            log.debug("Found " + entries.length + " abstract services");
        }
		// Added synchronization block for findbugs issues fix
        synchronized (this) 
		{
        for (int i = 0; i < entries.length; ++i)
        {
            // Ignore non-host device services
            if (!isHostService(entries[i].id))
            {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Ignoring non-host service " + entries[i].id);
                    }
                continue;
            }

                if (log.isInfoEnabled())
                {
                    log.info("Found AbstractService: 0x" + Integer.toHexString(entries[i].id));
                }

            // Ignore app-less services.
            if (entries[i].apps == null || entries[i].apps.size() == 0)
            {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Ignoring app-less service " + entries[i].id);
                    }
                continue;
            }
            db.put(new Integer(entries[i].id), entries[i]);
        }
		}
    }

    /**
     * Notifies listeners of changes to the <code>AbstractServiceEntry</code>
     * for the associated service.
     * 
     * @param newEntry
     *            the new <code>AbstractServiceEntry</code> for the service
     */
    private void notifyUpdate(AbstractServiceEntry newEntry)
    {
        Integer key = new Integer(newEntry.id);
        ServiceChangeListener listener = (ServiceChangeListener) listeners.get(key);

        if (listener != null)
        {
            listener.serviceUpdate(newEntry);
        }
    }

    /**
     * Posts an ServiceDetailsChangeEvent indicating a change in the services
     * database.
     * 
     * @param entry
     *            the entry that was added, removed, or modified
     * @param type
     *            the type of change that occurred
     */
    private void postEvent(AbstractServiceEntry entry, SIChangeType type)
    {
        ServiceManager serviceManager = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        SICache cache = serviceManager.getSICache();
        // create the object implementing ServiceDetails for the event
        AbstractServiceImpl asi = new AbstractServiceImpl(entry);
        ServiceDetails details = asi.getDetails();
        // get the available Transports
        Transport trans[] = cache.getTransports();
        // target transport object that will receive the events
        TransportExt transExt = null;

        // iterate through the Transport array and find the instance that
        // represents the cable transport
        for (int i = 0; i < trans.length; i++)
        {
            if (trans[i].getDeliverySystemType() == DeliverySystemType.CABLE)
            {
                transExt = (TransportExt) trans[i];
                break;
            }
        }

        // send the event if we found the correct Transport object
        if (transExt != null)
        {
            ServiceDetailsChangeEvent evt = new ServiceDetailsChangeEvent(transExt, type, details);
            // notify the transport object of this change.
            transExt.postServiceDetailsChangeEvent(evt);
        }
    }

    /**
     * Notifies all registered callbacks that the IMA has been configured (if
     * present) and that all other auto-select abstract services are about to be
     * launched.
     */
    private void notifyMonitorApplicationStarted()
    {
        BootProcessCallback[] array;
        synchronized (callbacks)
        {
            array = new BootProcessCallback[callbacks.size()];
            callbacks.copyInto(array);
            monAppStarted = true;
        }
        for (int i = 0; i < array.length; ++i)
        {
            try
            {
                array[i].monitorApplicationStarted();
            }
            catch (Throwable t)
            {
                if (log.isWarnEnabled())
                {
                    log.warn( "monitorApplicationStarted on BootProcessCallback " 
                              + array[i] + " threw an exception during notificiation", t );
                }
                // Continue notifying...
            }
        }
    }

    /**
     * Notifies all registered callbacks that all auto-select abstract services have been
     * launched.
     */
    private void notifyUnboundApplicationsStarted()
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifying unbound applications started");
        }
        
        BootProcessCallback[] array;
        synchronized (callbacks)
        {
            array = new BootProcessCallback[callbacks.size()];
            callbacks.copyInto(array);
            autostartHostAppsStarted = true;
        }
        for (int i = 0; i < array.length; ++i)
        {
            try
            {
                array[i].initialUnboundAutostartApplicationsStarted();
            }
            catch (Throwable t)
            {
                if (log.isWarnEnabled())
                {
                    log.warn( "initialUnboundAutostartApplicationsStarted on BootProcessCallback " 
                              + array[i] + " threw an exception during notificiation", t );
                }
                // Continue notifying...
            }
        }
    }

    /**
     * Notifies all registered callbacks that the boot process must be shutdown
     * so that the IMA may be restarted.
     * 
     * @return <code>true</code> if there are outstanding asynchronous shutdowns
     */
    private boolean notifyCallbacksShutdown()
    {
        BootProcessCallback[] array;
        synchronized (callbacks)
        {
            if (callbacks.size() == 0) return false;

            array = new BootProcessCallback[callbacks.size()];
            callbacks.copyInto(array);
        }

        // Set up time-out for asynchronous shutdown operations
        final TVTimer timer = TVTimer.getTimer();
        final TVTimerSpec[] timerList = new TVTimerSpec[1];
        final TVTimerSpec timerSpec = new TVTimerSpec();
        final Vector outstandingShutdown = new Vector();
        ShutdownCallback callback = new ShutdownCallback()
        {

            public void complete(BootProcessCallback act)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("BootProcessCallback.shutdown() finished: " + act);
                }

                boolean finish;
                synchronized (this)
                {
                    finish = outstandingShutdown.removeElement(act) && outstandingShutdown.isEmpty();
                }
                if (finish)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Callbacks finished asynchronously!");
                    }

                    // Cancel time-out
                    if (timerList[0] != null)
                        timer.deschedule(timerList[0]);

                    finishShutdown();
                }
            }
        };

        // Sync to prevent async callbacks from invoking before we are ready
        synchronized (callback)
        {
            // Invoke all callbacks
            // Remember those that will complete async
            for (int i = 0; i < array.length; ++i)
            {
                if (array[i].monitorApplicationShutdown(callback)) outstandingShutdown.addElement(array[i]);
            }

            boolean mustWait = !outstandingShutdown.isEmpty();

            // If there are async callbacks, set up timeout
            if (ASYNC_SHUTDOWN_TIMEOUT > 0 && mustWait)
            {
                // Schedule TVTimerSpec
                timerSpec.setDelayTime(ASYNC_SHUTDOWN_TIMEOUT);
                timerSpec.addTVTimerWentOffListener(new TVTimerWentOffListener()
                {
                    public void timerWentOff(TVTimerWentOffEvent e)
                    {
                        Host host = Host.getInstance();
                        if (host instanceof ExtendedHost)
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn("Forcing system reboot -- clean shutdown did not occur in "
                                        + timerSpec.getTime() + "ms");
                            }
                            ((ExtendedHost) host).forceReboot();
                        }
                        else
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("Full system reboot not possible", new Exception());
                            }
                    }
                    }
                });
                try
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Scheduling reboot timeout for " + timerSpec.getTime());
                    }
                    timerList[0] = timer.scheduleTimerSpec(timerSpec);

                }
                catch (TVTimerScheduleFailedException e)
                {
                    SystemEventUtil.logRecoverableError("Reboot timer could not be set", e);
                }
            }

            // Return whether there are async callbacks outstanding
            return mustWait;
        }
    }

    /**
     * Performs a restart of the monitor application in cases where it has
     * terminated. The services database is searched for the current monitor
     * application which may be different from the one which was previously
     * running. Once it is found, the service containing it is re-selected.
     * <p>
     * Note that the operation of this method is asynchronous. It essentially
     * schedules the relaunching of the monitor app.
     * 
     * @see #restartMonitorAppImpl()
     */
    private void restartMonitorApp()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Requesting IMA restart...");
        }

        ccm.getSystemContext().runInContext(new Runnable()
        {
            public void run()
            {
                restartMonitorAppImpl();
            }
        });
    }

    /**
     * Implementation of {@link #restartMonitorApp}. To be called asynchronously
     * from the system context.
     * <p>
     * <ul>
     * <li>Locate the IMA. If none is signaled, returns and does nothing.
     * <li>Initiate download of IMA. If download fails, returns and does
     * nothing.
     * <li>The restart process continues after completion of IMA download
     * (successful or not).
     * </ul>
     * 
     * @see #scheduleReboot(InitialMonitorApp, boolean)
     */
    private synchronized void restartMonitorAppImpl()
    {
        if (currIMA != null && !currIMA.isRunning())
        {
            currIMA.dispose();
        }

        if (log.isDebugEnabled())
        {
            log.debug("Searching for IMA...");
        }

        // First determine if there is a MonApp
        if (findMonApp() == null)
        {
            // Do nothing, and return
            if (log.isDebugEnabled())
            {
                log.debug("IMA not found, no restart");
            }
            return;
        }

        // Continue w/ reboot process
        scheduleReboot();
    }

    /**
     * Invoked following completion (successful or not) of IMA download. If
     * download was unsuccessful, then clean up and return. Else, if download
     * was successful, then we are ready to proceed with the shutdown process.
     * This process consists of the following:
     * <ol>
     * <li>Cancel XAIT listener
     * <li>Shutdown all ServiceContexts
     * <li>Notify all Callbacks (Async completion)
     * <li>Ensure shutdown of all ServiceContexts
     * <li>Clear database
     * <li>Invoke bootProcess again
     * </ol>
     * 
     * After notification of boot process callbacks, if none require
     * asynchronous completion, then the shutdown operation is completed by
     * <code>finishShutdown()</code>. If callbacks must be completed
     * asynchronously, then <code>finishShutdown()</code> will be invoked
     * asynchronously.
     * 
     * @see InitialMonitorApp#download()
     * @see #finishShutdown()
     */
    private synchronized void scheduleReboot()
    {
        // We have a new IMA, time to actually start shutdown procedures
        // ?currIMA = newIMA;

        // 1. Cancel XAIT listener
        // 2. Shutdown all ServiceContexts
        // 3. Notify all Callbacks
        // (Async completion)
        // 4. Ensure shutdown of all ServiceContexts
        // 5. Clear database
        // 6. Invoke bootProcess again

        // Cancel XAIT listener
        sm.removeXaitListener(this);

        // Shutdown all ServiceContexts
        ServiceContextFactoryExt scf = (ServiceContextFactoryExt) ServiceContextFactory.getInstance();
        ServiceContext[] all = scf.getAllServiceContexts();
        if (all != null)
        {
            for (int i = 0; i < all.length; ++i)
                all[i].destroy();
        }

        // Notify all callbacks
        if (!notifyCallbacksShutdown())
        {
            if (log.isDebugEnabled())
            {
                log.debug("Callbacks finished synchronously!");
            }
            finishShutdown();
        }
    }

    /**
     * Finish shutdown completes the shutdown and "reboot" process. This is
     * invoked following successful notification of all boot process callbacks
     * and completion of any asynchronous shutdown procedures. This operation
     * actually completes its task asynchronously, via the creation of a new
     * <code>Thead</code>. This is intended to mirror the original boot process
     * which is invoked from the primordial thread before exiting.
     * 
     * @see #finishShutdownImpl()
     */
    private void finishShutdown()
    {
        // Since boot process is always done from own thread... create a new
        // thread....
        Thread bootThread = new Thread("Boot")
        {
            public void run()
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Re-starting boot process...");
                }

                // Ensure shutdown of all ServiceContexts
                // Clear database
                synchronized (this)
                {
                    autoSelectEnabled = false;
                    db = new Hashtable();
                    currIMA = null;
                    xaitAcquired.reset();
                }

                // 6. Invoke bootProcess again
                bootProcess();
            }
        };
        try
        {
            bootThread.start();
        }
        catch (Throwable e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Could not start boot thread", e);
            }

            // Shall we reboot?

        }
    }

    // Description copied from ServicesDatabase
    public synchronized AbstractService addSelectedService(int serviceID)
    {
        Integer serviceIdObj = new Integer(serviceID);

        // Make sure the service is in the database
        AbstractServiceEntry entry;
        if ((entry = getServiceEntry(serviceID)) == null)
        {    
            return null;
        }

        // Add the service to the list of selected services
        if (selectedServiceIDs.get(serviceIdObj) == null)
        {
            // Not already in the hashtable so add it and return success
            selectedServiceIDs.put(serviceIdObj, serviceIdObj);
            return new AbstractServiceImpl(entry);
        }

        // Already in the hashtable so return failure
        return null;
    }

    // Description copied from ServicesDatabase
    public synchronized void removeSelectedService(int serviceID)
    {
        Integer serviceIdObj = new Integer(serviceID);
        if (selectedServiceIDs.remove(serviceIdObj) == null)
            if (log.isWarnEnabled())
            {
                log.warn("Attempt to remove unknown service from list of selected services.");
            }

        // If the service entry is marked for removal then remove it from the
        // database
        AbstractServiceEntry entry = getServiceEntry(serviceID);
        if (entry != null && entry.markedForRemoval)
        {
            db.remove(serviceIdObj);
        }
    }

    // Description copied from ServicesDatabase
    public synchronized boolean isServiceSelected(int serviceID)
    {
        return selectedServiceIDs.contains(new Integer(serviceID));
    }

    // Description copied from ServiceDatabase
    public synchronized boolean isServiceMarked(int serviceID)
    {
        AbstractServiceEntry entry = getServiceEntry(serviceID);
        return (entry != null && entry.markedForRemoval);
    }

    /**
     * A simple latching flag variable that provides a method to wait for the
     * flag to be modified or a timeout occurs.
     * 
     * @author Aaron Kamienski
     */
    // TODO: move to a common location
    private class Flag
    {
        private volatile boolean flag;

        private volatile boolean signaled;

        private volatile Object lock;

        public Flag(Object lock)
        {
            this.lock = lock;
        }

        public void reset()
        {
            synchronized (lock)
            {
                flag = false;
                signaled = false;
            }
        }

        public boolean acquire(long ms, boolean reset)
        {
            synchronized (lock)
            {
                boolean value = acquire(ms);
                flag = reset;
                signaled = false;
                return value;
            }
        }

        public boolean acquire(long ms)
        {
            synchronized (lock)
            {
                if (signaled || ms == 0) return flag;

                try
                {
                    if (ms < 0)
                        lock.wait();
                    else
                        lock.wait(ms);
                }
                catch (InterruptedException e)
                {
                    SystemEventUtil.logRecoverableError(new Exception("Exception waiting on flag (object "
                            + this.hashCode() + " lock " + this.lock.hashCode() + ") " + e));

                }
                return flag;
            }
        }

        public void set(boolean value)
        {
            synchronized (lock)
            {
                this.flag = value;
                signaled = true;
                lock.notifyAll();
            }
        }

        public boolean get()
        {
            synchronized (lock)
            {
                return flag;
            }
        }
    }

    private class ServiceContextListenerImpl implements ServiceContextListener
    {
        public void receiveServiceContextEvent(ServiceContextEvent e)
        {
            boolean notify = false;
            synchronized(ServicesDatabaseImpl.this)
            {
                //it doesn't matter what the event is...servicecontext event for abstract services implies the autostart apps have started or it failed to start
                ServiceContext serviceContext = e.getServiceContext();
                
                //remove listener now that we have received the notification
                serviceContext.removeListener(this);

                serviceContexts.remove(serviceContext);
                if (serviceContexts.isEmpty())
                {
                    notify = true;
                }
            }
            if (notify)
            {
                if (log.isInfoEnabled())
                {
                    log.info("received final servicecontext event - notifying unbound applications started");
                }
                notifyUnboundApplicationsStarted();
            }
        }
    }
    /**
     * Represents the current <i>Initial Monitor Application</i> and its
     * containing abstract service.
     * 
     * @author Aaron Kamienski
     */
    private class InitialMonitorApp implements AppStateChangeEventListener
    {
        /**
         * Creates an instance of <code>InitialMonitorApp</code>.
         * 
         * @param svc
         *            the containing abstract service
         * @param app
         *            the monitor application
         */
        InitialMonitorApp(AbstractServiceEntry svc, XAppEntry app)
        {
            this.svc = svc;
            this.app = app;

            app.isMonitorApp = true;
        }

        /**
         * Returns <code>true</code> if the IMA is running; <code>false</code>
         * otherwise.
         * 
         * @return <code>true</code> if the IMA is running; <code>false</code>
         *         otherwise
         */
        public boolean isRunning()
        {
            AppProxy tmp = appProxy;
            if (tmp == null) return false;
            int state = tmp.getState();
            return state != AppProxy.DESTROYED && state != AppProxy.NOT_LOADED;
        }

        /**
         * Launch the <i>Initial Monitor Application</i> and wait for it to
         * signal that is has been
         * {@link ServicesDatabase#notifyMonAppConfigured configured}.
         * <p>
         * This method uses the following algorithm:
         * <ol>
         * <li>Create a
         * {@link ServiceContextFactoryExt#createAutoSelectServiceContext
         * auto-select} <code>ServiceContext</code>.
         * <li>Retrieve the {@link ServiceContextExt#getAppDomain() AppDomain}
         * for the <code>ServiceContext</code>.
         * <li>Prime the domain's <code>AppsDatabase</code> via
         * {@link AppDomain#preSelect}.
         * <li>Retrieve the {@link AppDomain#getAppProxy AppProxy} for the IMA
         * so that app state changes may be observed.
         * <li> {@link ServiceContext#select(Service) Select} the IMA abstract
         * service.
         * <li>Wait for the IMA to be launched.
         * <li>Following launch wait for one of the following conditions:
         * <ul>
         * <li>IMA {@link ServicesDatabase#notifyMonAppConfigured() signals} it
         * is configured.
         * <li>IMA {@link ServicesDatabase#notifyMonAppConfiguring() signals} it
         * is configuring within 5 seconds; then wait for it to signal that is
         * is configured.
         * <li>5 second timeout.
         * </ul>
         * <li>Finally return success or failure.
         * </ol>
         * 
         * @return <code>true</code> if the IMA was successfully launched;
         *         <code>false</code> otherwise
         */
        public boolean launch()
        {
            synchronized (this)
            {
                // Create a ServiceContext
                ServiceContextFactory scf = ServiceContextFactory.getInstance();
                sc = ((ServiceContextFactoryExt) scf).createAutoSelectServiceContext();
                if (sc == null) return false;

                // Get AppDomain
                AppDomain appDomain = ((ServiceContextExt) sc).getAppDomain();

                // Pre-select the AppDomain to "prime" the AppsDatabase
                AbstractServiceImpl abstractSvc = new AbstractServiceImpl(svc);
                appDomain.preSelect(abstractSvc.getDetails());

                // Locate the AppProxy for the IMA
                appProxy = appDomain.getAppProxy(app.id);
                if (appProxy == null) return false;
                appProxy.addAppStateChangeEventListener(this);

                // Finally, select the service
                try
                {
                    sc.select(abstractSvc);
                }
                catch (Exception e)
                {
                    SystemEventUtil.logRecoverableError("AutoSelect abstract service could not be selected: " + svc, e);

                    return false;
                }
            }

            // Wait for MonApp to be launched
            if (log.isDebugEnabled())
            {
                log.debug("Waiting for MonApp launch...");
            }

            // If the monapp has not already made it through loading and set the
            // configured or
            // configuring flags, wait for it to load
            if (monAppConfigured == MONAPP_UNCONFIGURED && !monAppLoaded.acquire(-1))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("MonApp failed to reach the LOADED state!");
                }
                return false;
            }

            // Wait for monitorConfigureSignaled
            if (log.isDebugEnabled())
            {
                log.debug("Waiting for MonApp to be configured...");
            }
            synchronized (monAppConfiguredLock)
            {
                try
                {
                    // Wait 5 seconds for the monapp to signal configuring
                    if (monAppConfigured != MONAPP_CONFIGURING)
                        monAppConfiguredLock.wait(5000);

                    // If the monapp did signal configuring, wait indefinitely
                    // for it to finish
                    if (monAppConfigured == MONAPP_CONFIGURING)
                        monAppConfiguredLock.wait();
                }
                catch (InterruptedException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Unexpected interruption of IMA config wait", e);
                    }
            }
            }

            if (log.isDebugEnabled())
            {
                log.debug("MonApp launch succeeded");
            }
            return true;
        }

        /**
         * Disposes of this <i>IMA</i>. This includes:
         * <ul>
         * <li> {@link ServiceContext#destroy Destroying} the
         * <code>ServiceContext</code>
         * <li> {@link AppProxy#removeAppStateChangeEventListener Forgetting} the
         * <code>AppProxy</code>
         * </ul>
         */
        public void dispose()
        {
            // No longer the IMA
            app.isMonitorApp = false;

            // Destroy other apps
            ServiceContext tmpSc = sc;
            sc = null;
            if (tmpSc != null) tmpSc.destroy();

            AppProxy tmpApp = appProxy;
            appProxy = null;
            if (tmpApp != null) tmpApp.removeAppStateChangeEventListener(this);
        }

        /**
         * Returns <code>true</code> if the given
         * <code>AbstractServiceEntry</code> is the same as this <i>IMA</i>
         * 
         * @param other
         * @return <code><i>other</i>==this.svc</code>
         */
        public synchronized boolean isMonApp(AbstractServiceEntry other)
        {
            return svc == other;
        }

        /**
         * Implements {@link AppStateChangeEventListener#stateChange}. Monitors
         * state changes for the IMA.
         * 
         * Basically, what we are looking for is:
         * <ul>
         * <li>Launching of the app (as indicated by successful
         * <code>LOADING</code>)
         * <li>Starting of the app
         * <li>Destruction of the app
         * <li>State transition failure (prior to successful starting)
         * </ul>
         * 
         * @param e
         */
        public synchronized void stateChange(AppStateChangeEvent e)
        {
            int to = e.getToState();
            AppProxy proxy = (AppProxy) e.getSource();

            if (!e.hasFailed())
            {
                switch (to)
                {
                    case AppProxy.DESTROYED:
                    case AppProxy.PAUSED:
                        // no-op: don't care about transitions to this state
                        break;
                    case DVBJProxy.LOADED:
                        if (log.isDebugEnabled())
                        {
                            log.debug("MonitorApp reached LOADED state...");
                        }
                        launched = true;
                        monAppLoaded.set(true);
                        break;
                    case AppProxy.STARTED:
                        if (!started)
                        {
                            SystemEventUtil.logEvent("MonitorApp(" + app.id + "): STARTED");
                            started = true;
                        }
                        break;
                    case AppProxy.NOT_LOADED:
                        if (log.isDebugEnabled())
                        {
                            log.debug("MonitorApp became NOT_LOADED -- restarting boot process...");
                        }
                        if (launched)
                            restartMonitorApp();
                        break;
                }
            }
            else
            {
                if (started) return; // don't care about failures after started

                switch (to)
                {
                    case AppProxy.NOT_LOADED: // FIXME: shouldn't be here, but
                                              // we apparently notify about
                                              // NOT_LOADED->~PRE_LOADED
                                              // incorrectly!!!
                    case DVBJProxy.LOADED:
                    case AppProxy.PAUSED:
                    case AppProxy.STARTED:
                        if (log.isDebugEnabled())
                        {
                            log.debug("MonitorApp encountered a state transition failure!");
                        }
                        proxy.stop(true);
                        monAppLoaded.set(false);
                        break;
                    // case AppProxy.NOT_LOADED:
                    case AppProxy.DESTROYED:
                        // no-op: don't care about failures to transition to
                        // this state
                        break;
                }
            }
        }

        public void notifyMonAppConfiguring()
        {
            synchronized (monAppConfiguredLock)
            {
                monAppConfigured = MONAPP_CONFIGURING;
                monAppConfiguredLock.notifyAll();
            }
        }

        public void notifyMonAppConfigured()
        {
            synchronized (monAppConfiguredLock)
            {
                monAppConfigured = MONAPP_CONFIGURED;
                monAppConfiguredLock.notifyAll();
            }
        }

        /**
         * The IMA service.
         */
        private final AbstractServiceEntry svc;

        /**
         * The IMA app.
         */
        private final XAppEntry app;

        /**
         * The <code>ServiceContext</code> used to select the service.
         */
        private ServiceContext sc;

        /**
         * The <code>AppProxy</code> that corresponds to the <i>IMA</i>.
         */
        private AppProxy appProxy;

        /**
         * Indicates that the <i>IMA</i> has been <i>launched</i>. I.e., it has
         * been {@link DVBJProxy#LOADED loaded}.
         * 
         * @see #stateChange(AppStateChangeEvent)
         */
        private boolean launched;

        /**
         * Indicates that the <i>IMA</i> has been <i>started</i>. I.e., it has
         * been {@link AppProxy#STARTED started}.
         * <p>
         * Only <code>true</code> if {@link #launched}.
         * 
         * @see #stateChange(AppStateChangeEvent)
         */
        private boolean started;

        /**
         * Flag that is set when the initial MonApp transitions to the LOADED
         * state.
         */
        private Flag monAppLoaded = new Flag(this);

        /**
         * Flag that is signaled upon configuration of the initial MonApp.
         */
        private int monAppConfigured = MONAPP_UNCONFIGURED;

        private static final int MONAPP_UNCONFIGURED = 0;

        private static final int MONAPP_CONFIGURING = 1;

        private static final int MONAPP_CONFIGURED = 2;

        private Object monAppConfiguredLock = new Object();
    }

    // track if monitorApplicationStarted callbacks have been called during bootProcess
    private boolean monAppStarted;

    // track if autostartHostApplicationsStarted callbacks have been called during bootProcess
    private boolean autostartHostAppsStarted;

    /**
     * Represents the current Initial Monitor Application. This is only non-
     * <code>null</code> when an IMA has been found and launch has been
     * attempted.
     */
    private InitialMonitorApp currIMA;

    /**
     * Flag that is signaled upon reception and acceptance of a new XAIT.
     */
    private Flag xaitAcquired = new Flag(this);

    /**
     * Keep track of the last received version of XAIT so that we can tell if we
     * have a "resignalling" due to transport protocol communication failure
     */
    private int lastNetworkXAITVersion = -1;

    private int lastRegisteredXAITVersion = -1;

    /**
     * Auto select is enabled if true.
     */
    private boolean autoSelectEnabled = false;

    /**
     * The database of <code>AbstractServiceEntry</code>s.
     */
    private Hashtable db = new Hashtable();

    /**
     * The set of listeners for any service.
     */
    private Hashtable listeners = new Hashtable();

    /**
     * The set of boot process callbacks.
     */
    private Vector callbacks = new Vector();

    /**
     * Singleton instance of the <code>ServicesDatabaseImpl</code>.
     */
    private static ServicesDatabaseImpl singleton = new ServicesDatabaseImpl();

    /**
     * Whether to support <i>newly auto-select</i> abstract services or not (in
     * a manner similar to <i>newly auto-start</i> applications). In OCAP
     * 10.2.2.2.2, autoselect is only considered for newly signalled services;
     * implying that this should be <code>false</code>.
     */
    private static final boolean NEWLY_AUTOSELECT = false;

    /**
     * Name of properties file containing resident applications.
     */
    private static final String HOSTAPPS = "hostapp.properties";

    /**
     * Property that, if set to "true", indicates that we should ignore XAIT.
     */
    private static final boolean IGNORE_XAIT = "true".equals(MPEEnv.getEnv("OCAP.xait.ignore"));

    /**
     * Property that indicates the timeout (in seconds) for the initial XAIT
     * access. Timeout defaults to 30 seconds.
     */
    private static final long XAIT_TIMEOUT = MPEEnv.getEnv("OCAP.xait.timeout", 30) * 1000L;

    /**
     * The maximum amount of time to wait for all asynchronous shutdown
     * callbacks to complete, including the shutdown of all running
     * applications. If this time-out expires, then the system will be forcibly
     * rebooted.
     * <p>
     * Set to 0 (zero) to disable timeout altogether.
     */
    private static final long ASYNC_SHUTDOWN_TIMEOUT = MPEEnv.getEnv("OCAP.monapp.reboot.timeout", 60000L);

    /**
     * List of service IDs which are currently selected or in the process of
     * being selected.
     */
    private Hashtable selectedServiceIDs = new Hashtable();

    private static CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    private static SignallingManager sm = (SignallingManager) ManagerManager.getInstance(SignallingManager.class);

    private static AppStorageManager asm = (AppStorageManager) ManagerManager.getInstance(AppStorageManager.class);

    private final ServiceContextListener serviceContextListener = new ServiceContextListenerImpl();
    
    private final Set serviceContexts = new HashSet();

    private static final Logger log = Logger.getLogger(ServicesDatabaseImpl.class.getName());
}
