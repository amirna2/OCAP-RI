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

import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Date;
import java.util.Properties;

import javax.tv.service.SIManager;
import javax.tv.service.navigation.ServiceIterator;
import javax.tv.service.navigation.ServiceList;
import javax.tv.service.navigation.ServiceTypeFilter;

import org.apache.log4j.Logger;
import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseFilter;
import org.ocap.application.AppManagerProxy;
import org.ocap.application.AppSignalHandler;
import org.ocap.application.OcapAppAttributes;
import org.ocap.application.SecurityPolicyHandler;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;
import org.ocap.service.AbstractServiceType;
import org.ocap.system.MonitorAppPermission;
import org.ocap.system.event.ErrorEvent;
import org.ocap.system.event.SystemEventManager;

import org.cablelabs.impl.appctx.AppContextHandler;
import org.cablelabs.impl.appctx.AppContextManager;
import org.cablelabs.impl.manager.AppDomain;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.manager.SignallingManager;
import org.cablelabs.impl.manager.CallbackData.SimpleData;
import org.cablelabs.impl.service.ServicesDatabase;
import org.cablelabs.impl.service.ServicesDatabase.BootProcessCallback;
import org.cablelabs.impl.service.ServicesDatabase.ShutdownCallback;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * The <code>ApplicationManager</code> implementation. The application manager
 * serves two roles:
 * <ul>
 * <li>Gateway to all application management functionality (by implementing the
 * <code>ApplicationManager</code> interface).
 * <li>Internal to the implementation of that functionality, it is the
 * creator/manager of all applications.
 * </ul>
 * Implements the methods of <code>AppManagerProxy</code> (to be called by the
 * <code>AppManagerProxyImpl</code> implementation class). This is the intended
 * "full" implementation, rather than a simple placeholder or prototypical
 * implementation.
 * 
 * @author Aaron Kamienski
 */
public class AppManager implements ApplicationManager, CallerContextManager
{
    /**
     * Not publicly instantiable.
     */
    protected AppManager()
    {
        // Create the CCMgr
        ccMgr = (CCMgr) CCMgr.getInstance();

        // Register our app-context handler
        AppContextHandler handler = new AppContextHandler()
        {

            // Save default values now before we register our provider
            private String defaultUserDir = System.getProperty("user.dir");

            private String defaultOcapJLocation = System.getProperty("ocap.j.location");

            public String getProperty(String key)
            {
                String value;
                CallerContext cc = ccMgr.getCurrentContext();

                // user.dir may be requested by the SystemContext (in which case
                // it will
                // be null). Just return the current working directory
                if (key.equals("user.dir"))
                {
                    value = (String) cc.get(CallerContext.USER_DIR);
                    if (value == null) return defaultUserDir;
                    return value;
                }

                if (key.equals("ocap.j.location"))
                {
                    value = (String) cc.get(CallerContext.USER_DIR);
                    if (value == null) return defaultOcapJLocation;
                    return value;
                }

                if (key.equals("java.io.tmpdir"))
                {
                    value = (String) cc.get(CallerContext.JAVAIO_TMP_DIR);
                    if (value == null) return defaultOcapJLocation;
                    return value;
                }

                return null;
            }
        };
        AppContextManager.registerAppContextHandler(handler);
    }

    /**
     * Returns the singleton instance of the AppMgr/ContextMgr. Will be called
     * only once for each Manager class type.
     */
    public static synchronized Manager getInstance()
    {
        if (singleton == null) singleton = new AppManager();

        // refCount should be incremented once each for
        // AppMgr:getInstance and CallerContextMgr:getInstance.
        ++refCount;
        return singleton;
    }

    /**
     * Returns the singleton instance of the <code>AppManager</code>. This
     * method is meant to be used internally, only <i>after</i> the
     * <code>AppManager</code> has been initially created (via
     * {@link #getInstance}).
     */
    static synchronized AppManager getAppManager()
    {
        return (AppManager) singleton;
    }

    // Description copied from ApplicationManager
    public AppManagerProxy getAppManagerProxy()
    {
        CallerContext ctx = getCurrentContext();
        ctx.checkAlive();
        SimpleData data = (SimpleData) ctx.getCallbackData(AppManagerProxy.class);
        if (data == null)
            ctx.addCallbackData(data = new SimpleData(new AppManagerProxyImpl(this)), AppManagerProxy.class);
        return (AppManagerProxy) data.getData();
    }

    // Description copied from ApplicationManager
    public org.ocap.system.RegisteredApiManager getRegisteredApiManager()
    {
        return getApiRegistrar();
    }

    // ////////////////////////////////////////////
    // Addressable Properties APIs //
    // (just call through to SignallingManager) //
    // ////////////////////////////////////////////

    public void registerAddressingProperties(Properties properties, boolean persist, Date expirationDate)
    {
        ((SignallingManager) ManagerManager.getInstance(SignallingManager.class)).registerAddressingProperties(
                properties, persist, expirationDate);
    }

    public Properties getAddressingProperties()
    {
        return ((SignallingManager) ManagerManager.getInstance(SignallingManager.class)).getAddressingProperties();
    }

    public void removeAddressingProperties(String[] properties)
    {
        ((SignallingManager) ManagerManager.getInstance(SignallingManager.class)).removeAddressingProperties(properties);
    }

    public Properties getSecurityAddressableAttributes()
    {
        return ((SignallingManager) ManagerManager.getInstance(SignallingManager.class)).getSecurityAddressableAttributes();
    }

    /**
     * Returns the current <code>ApiRegistrar</code>.
     * 
     * @return the current <code>ApiRegistrar</code>
     */
    synchronized ApiRegistrar getApiRegistrar()
    {
        if (apiRegistrar == null) apiRegistrar = new ApiRegistrar();
        return apiRegistrar;
    }

    // Description copied from ApplicationManager
    public AppsDatabase getAppsDatabase()
    {
        CallerContext ctx = getCurrentContext();

        if (ctx instanceof AppContext)
        {
            SecurityManager sm = System.getSecurityManager();
            // Only grant access to globalDb if there is a SecurityManager
            if (sm != null)
            {
                try
                {
                    checkPermission(new MonitorAppPermission("servicemanager"));
                    if (log.isDebugEnabled())
                    {
                        log.debug("getAppsDatabase returning global db");
                    }
                    return globalDb;
                }
                catch (SecurityException e)
                {
                    // Simply fall through to below
                }
            }
            if (log.isDebugEnabled())
            {
                log.debug("getAppsDatabase returning db from context");
            }
            return (AppsDatabase) ctx.get(AppsDatabase.class);
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("getAppsDatabase returning global db due to privileged caller (or null): "
                    + SecurityUtil.isPrivilegedCaller());
        }
        return SecurityUtil.isPrivilegedCaller() ? globalDb : null;
    }

    // Description copied from ApplicationManager
    public ClassLoader getAppClassLoader(CallerContext ctx)
    {
        if (ctx == null) ctx = getCurrentContext();
        ctx.checkAlive();
        if (ctx instanceof AppContext)
            return ((AppContext) ctx).getClassLoader();
        
        return getClass().getClassLoader();
    }

    // Description copied from ApplicationManager
    public AppDomain createAppDomain(javax.tv.service.selection.ServiceContext svcCtx)
    {
        // Boot Process is delayed until here to avoid circular dependency
        // issues with ServicesManager
        initBootProcessCallback();

        return new AppDomainImpl(svcCtx, globalDb);
    }

    // Description copied from ApplicationManager
    public OcapAppAttributes createAppAttributes(org.cablelabs.impl.signalling.AppEntry entry,
            javax.tv.service.Service service)
    {
        // BTW, AttributesImpl is defined in AppDomainImpl source file
        return new AttributesImpl(entry, (OcapLocator)service.getLocator());
    }

    // Description copied from ApplicationManager
    public boolean purgeLowestPriorityApp(long contextId, long timeout, boolean urgent)
    {
        if (log.isDebugEnabled())
        {
            log.debug("purgeLowestPriorityApp(" + Long.toHexString(contextId) + ")");
        }
        return ccMgr.purgeLowestPriority(contextId, timeout, urgent);
    }

    // Description copied from CallerContextManager
    public CallerContext getCurrentContext()
    {
        return ccMgr.getCurrentContext();
    }

    /**
     * This is a utility method used within this package to access the current
     * <code>CallerContext</code> without assuming that the
     * <code>AppManager</code> is the current <code>CallerContextManager</code>.
     * While it would generally be <i>faster</i> simply call
     * {@link AppManager#getCurrentContext}, making such an assumption makes it
     * difficult to replace the <code>CallerContextManager</code> during
     * testing.
     * 
     * @return {@link CallerContextManager#getCurrentContext()}
     */
    static CallerContext getCaller()
    {
        // The way that allows for testing
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        return ccm.getCurrentContext();
    }

    // Description copied from CallerContextManager
    public CallerContext getSystemContext()
    {
        return ccMgr.getSystemContext();
    }

    // Copy description from Manager interface
    public void destroy()
    {
        // FIXME: remove this (added to help hunt down bug 4554 -- ignore during
        // testing)
        if (log.isErrorEnabled())
        {
            log.error("AppManager being DESTROYED!", new Exception("Stack Trace"));
        }
		// Added for findbugs issues fix
		// Changed the way to get the class
        synchronized (AppManager.class)
        {
            if (--refCount <= 0)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Destroying AppManager singleton");
                }
                // Clean up.
                // Destroy all running apps
                // Pass "destroy" on to the AppsDB
                // Don't be told about any more running apps

                // TODO: remove bootCallback

                ccMgr.destroy();
                singleton = null;
            }
        }
    }

    void setAppFilter(AppsDatabaseFilter filter) throws SecurityException
    {
        checkPermission(new MonitorAppPermission("handler.appFilter"));
        synchronized (this)
        {
            appFilter.dispose();
            appFilter = (filter != null) ? (new FilterContext(getCurrentContext(), filter)) : new Filter();
        }
    }

    /**
     * Utility method for logging a system/error event via the
     * <code>SystemEventManager</code>. The current context will be considered
     * when the <code>ErrorEvent</code> is created.
     * 
     * @param type
     *            <code>ErrorEvent</code> type
     * @param e
     *            <code>Throwable</code> to log
     */
    static void logErrorEvent(final int type, final Throwable e)
    {
        PrivilegedAction action = new PrivilegedAction()
        {
            public Object run()
            {
                return new ErrorEvent(type, e);
            }
        };
        logErrorEvent((ErrorEvent) AccessController.doPrivileged(action));
    }

    /**
     * Utility method for logging a system/error event via the
     * <code>SystemEventManager</code>. The current context will be considered
     * when the <code>ErrorEvent</code> is created.
     * 
     * @param type
     *            <code>ErrorEvent</code> type
     * @param message
     *            <code>message</code> to log
     */
    static void logErrorEvent(final int type, final String message)
    {
        PrivilegedAction action = new PrivilegedAction()
        {
            public Object run()
            {
                return new ErrorEvent(type, message);
            }
        };
        logErrorEvent((ErrorEvent) AccessController.doPrivileged(action));
    }

    /**
     * Utility method for logging system informational event via the
     * <code>SystemEventManager</code>. The current context will be considered
     * when the <code>ErrorEvent</code> is created.
     * <p>
     * A type of {@link ErrorEvent#SYS_INFO_GENERAL_EVENT} is implied.
     * 
     * @param message
     *            <code>message</code> to log
     */
    static void logErrorEvent(final String message)
    {
        logErrorEvent(ErrorEvent.SYS_INFO_GENERAL_EVENT, message);
    }

    /**
     * Logs the given <code>ErrorEvent</code> via the
     * <code>SystemEventManager</code>.
     * 
     * @param e
     *            event to log
     */
    private static void logErrorEvent(ErrorEvent e)
    {
        SystemEventManager sem = SystemEventManager.getInstance();
        sem.log(e);
    }

    /**
     * Removes the currently installed <i>AppFilter</i> if it is the same as the
     * given specified by the <i>filter</i> parameter.
     * 
     * @filter the filter to remove
     */
    private synchronized void clearAppFilter(Filter filter)
    {
        if (appFilter == filter)
        {
            appFilter = new Filter();
            filter.dispose();
        }
    }

    /**
     * Creates a new <code>AppContext</code> if launching the application
     * specified by the given <code>AppEntry</code> is allowed. Will consult the
     * currently installed <code>AppFilter</code> and also ensure that an
     * application with the given <code>AppID</code> doesn't already exist.
     * 
     * @param app
     *            the <code>XletApp</code> that is being launched
     * @param id
     *            the <code>AppID</code> that identifies the app
     * @param domain
     *            the domain within which the app will be executed
     * @return a newly created <code>AppContext</code> or <code>null</code> if
     *         the launching of the specifie application is not allowed
     */
	// Added for findbugs issues fix
	// Added synchronization modifier
    synchronized AppContext createAppContext(XletApp app, AppID id, AppDomainImpl domain)
    {
        if (appFilter.accept(id))
        {
            return ccMgr.createInstance(app, id, domain);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("App " + id + " not launched due to AppFilter");
            }
        }
        return null;
    }

    /**
     * Used to implement
     * {@link AppManagerProxy#setApplicationPriority(int, AppID)}. Will remember
     * the priority for the given application until called again or is replaced
     * by a new version. This method treats <code>AppID</code>s as unique (i.e.,
     * different instances of the same application in different services are
     * treated as the same application).
     * <p>
     * Note, if the current version of the application cannot be found, the
     * priority will be set against version zero. As such, any bound app will
     * use the priority but it won't be used for unbound apps with a non-zero
     * priority.
     * 
     * @param id
     *            the <code>AppID</code> for the app whose priority should be
     *            set
     * @param priority
     *            the new priority
     * 
     * @throws SecurityException
     *             is thrown when the caller does not have
     *             MonitorAppPermission("servicemanager").
     * @throws IllegalStateException
     *             if the application, i.e. Xlet, is in the active state, or is
     *             currently set at monitor application priority.
     * 
     * @see #setApplicationPriority(AppID, int, int)
     */
    void setApplicationPriority(AppID id, int priority)
    {
        checkPermission(new MonitorAppPermission("servicemanager"));

        // Look up the application version
        // 1. Look in AppsDatabase
        AppAttributes attrib = globalDb.getAppAttributes(id);
        // 2. Look in SIManager
        if (attrib == null)
        {
            SIManager si = SIManager.createInstance();

            // Iterate over list of AbstractServices...
            ServiceList list = si.filterServices(new ServiceTypeFilter(AbstractServiceType.OCAP_ABSTRACT_SERVICE));
            SERVICE_LOOP: for (ServiceIterator i = list.createServiceIterator(); i.hasNext();)
            {
                AbstractService svc = (AbstractService) i.nextService();

                // Iterate over list of applications
                for (Enumeration e = svc.getAppAttributes(); e.hasMoreElements();)
                {
                    // Stop looking once an AppAttributes for that AppID is
                    // found
                    AppAttributes attr = (AppAttributes) e.nextElement();
                    if (id.equals(attr.getIdentifier()))
                    {
                        attrib = attr;
                        break SERVICE_LOOP;
                    }
                }
            }
        }
        long version = 0;
        if (attrib != null)
        {
            // Ensure not attempting to modify a MonApp
            if (attrib.getPriority() == 255) throw new IllegalStateException("cannot modify priority of MonApp");

            // Use found version, else zero
            if (attrib instanceof AttributesImpl) version = ((AttributesImpl) attrib).getVersion();
        }

        // Finally, set the application priority
        setApplicationPriority(id, version, priority);
    }

    /**
     * Sets the <i>override</i> application priority for the given
     * <i>version</i> of the application with the given <code>AppID</code>.
     * <p>
     * This method is package-private for testing purposes.
     * 
     * @param id
     *            unique application identifier
     * @param version
     *            version of the app to which the priority setting applies
     * @param priority
     *            the new <i>override</i> priority for this application
     * 
     * @see #getApplicationPriority(AppID, int)
     */
    void setApplicationPriority(AppID id, long version, int priority)
    {
        // There's a race condition here...
        // The appContext could be created before we get the priority in-place.
        // It shouldn't cause a problem for us.
        // I'm not going to worry about synchronizing, because I think that
        // the current rule (exception if in "Active" state) is kinda silly.

        appPriorities.put(id, new AppPriority(version, priority));
    }

    /**
     * Retrieves the application priority <i>override</i> set with
     * {@link #setApplicationPriority} (used to implement
     * OcapAppAttributes#setApplicationPriority}), if set. If no priority has
     * been set, then <code>-1</code> is returned.
     * <p>
     * If an override had previously been set for the given <code>AppID</code>,
     * but a different version, then the previous override is ignored and
     * <code>-1</code> returned.
     * 
     * @param id
     *            <code>AppID</code> of application to look up
     * @param version
     *            <i>version</i> of application to look up
     * @return priority set for given <code>AppID</code> and <i>version</i>; -1
     *         if no priority is set
     */
    int getApplicationPriority(AppID id, long version)
    {
        synchronized (appPriorities)
        {
            AppPriority ap = (AppPriority) appPriorities.get(id);

            // If priority has previously been set...
            if (ap != null)
            {
                // If version has not been updated...
                if (ap.version == version)
                {
                    // ... return the set priority
                    return ap.priority;
                }
                // If version is older
                // else if (ap.version < version)
                {
                    // ... then forget it
                    appPriorities.remove(id);
                }
                // If version is newer, remember it, but don't use for this app.
            }
        }
        // Return the current version
        return -1;
    }

    // Description copied from ApplicationManager
    public int getRuntimePriority(AppID id)
    {
        return ccMgr.getRuntimePriority(id);
    }

    // Description copied from ApplicationManager
    public AppEntry getRunningVersion(final AppID id)
    {
        return (AppEntry)AccessController.doPrivileged(new PrivilegedAction() {
            public Object run()
            {
                AppsDatabase adb = AppsDatabase.getAppsDatabase();
                Application app = (Application)adb.getAppProxy(id);
                if (app != null)
                {
                    int currState = app.getState();
                    if (currState != AppProxy.NOT_LOADED && currState != AppProxy.DESTROYED)
                    {
                        return app.getAppEntry();
                    }
                }
                return null;
            }
        });
    }
    
    public void setSecurityPolicyHandler(SecurityPolicyHandler h) throws SecurityException
    {
        checkPermission(new MonitorAppPermission("security"));

        OcapSecurityManager osm = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);
        osm.setSecurityPolicyHandler(h);
    }

    public void setAppSignalHandler(AppSignalHandler h) throws SecurityException
    {
        checkPermission(monitor_registrar);

        SignallingManager sm = (SignallingManager) ManagerManager.getInstance(SignallingManager.class);
        sm.setAppSignalHandler(h);
    }

    void registerUnboundApp(java.io.InputStream xait) throws SecurityException, java.io.IOException
    {
        checkPermission(monitor_registrar);

        ((SignallingManager) ManagerManager.getInstance(SignallingManager.class)).registerUnboundApp(xait);
    }

    void unregisterUnboundApp(int serviceId, AppID appid) throws SecurityException
    {
        checkPermission(monitor_registrar);

        ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        ServicesDatabase db = sm.getServicesDatabase();
        db.unregisterUnboundApp(serviceId, appid);
    }

    /**
     * Ensures that a <code>BootProcessCallback</code> is registered. This is
     * not done during initialization instantiation and initialization so as to
     * avoid creating a dependency upon the ServicesManager and ServicesDatabase
     * implementations.
     */
    private synchronized void initBootProcessCallback()
    {
        if (bootCallback == null)
        {
            bootCallback = new BootProcessCallback()
            {

                /**
                 * Does nothing as we aren't interested in boot process startup.
                 */
                public void monitorApplicationStarted()
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Boot process started");
                    }
                }

                public void initialUnboundAutostartApplicationsStarted()
                {
                    // Nothing to do (only care about monitor app startup)
                }

                /**
                 * Propogates the notification (and request) on to the CCMgr.
                 */
                public boolean monitorApplicationShutdown(ShutdownCallback callback)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Boot process shutdown");
                    }

                    return ccMgr.shutdownApps(callback, this);
                }
            };

            ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
            ServicesDatabase sd = sm.getServicesDatabase();
            sd.addBootProcessCallback(bootCallback);
        }
    }

    /**
     * Checks whether the caller should be calling here. If the caller context
     * is <i>dead</i>, then a SecurityException should be thrown.
     * 
     * @throws SecurityException
     */
    private void checkAlive() throws SecurityException
    {
        CallerContext ctx = getCurrentContext();
        ctx.checkAlive();
    }

    /**
     * Check for proper permissions. Implicitly calls {@link #checkAlive()}.
     * 
     * @throws SecurityException
     */
    private void checkPermission(Permission p) throws SecurityException
    {
        checkAlive();

        SecurityUtil.checkPermission(p);
    }

    /**
     * Represents the unset (null) filter.
     * 
     * @author Aaron Kamienski
     */
    private class Filter
    {
        /**
         * Default implementation always returns true.
         */
        public boolean accept(AppID id)
        {
            return true;
        }

        /**
         * Should be invoked when this Filter is no longer needed. Default
         * implementation does nothing.
         */
        void dispose()
        {
            // empty
        }
    }

    /**
     * This class encapsulates a <code>CallerContext</code> and an
     * <code>AppFilter</code>. Implements <code>CallbackData</code> so that it
     * can remove itself when the installing application goes away.
     */
    private class FilterContext extends Filter implements CallbackData
    {
        public FilterContext(CallerContext ctx, AppsDatabaseFilter filter)
        {
            this.ctx = ctx;
            this.filter = filter;

            ctx.addCallbackData(this, this);
        }

        /**
         * Calls <code>filter.accept(id)</code> from within the context of the
         * caller who installed the <i>appFilter</i>.
         */
        public boolean accept(final AppID id)
        {
            final boolean[] ret = { true };
            CallerContext.Util.doRunInContextSync(ctx, new Runnable()
            {
                public void run()
                {
                    ret[0] = filter.accept(id);
                }
            });
            return ret[0];
        }

        /**
         * Cleans up links to this filter setup during construction.
         */
        void dispose()
        {
            ctx.removeCallbackData(this);
        }

        public void pause(CallerContext ctx)
        { /* empty */
        }

        public void active(CallerContext ctx)
        { /* empty */
        }

        /**
         * Ensures that this filter isn't used anymore.
         */
        public void destroy(CallerContext ctx)
        {
            // This is the one thing that is custom (when comparing w/ other
            // FilterContext impls)
            clearAppFilter(this);
        }

        private CallerContext ctx;

        private AppsDatabaseFilter filter;
    }

    /**
     * Class used to encapsulate application priority information.
     * 
     * @see #appPriorities
     * @see #setApplicationPriority
     * @see #getApplicationPriority
     * 
     * @author Aaron Kamienski
     */
    private class AppPriority
    {
        /**
         * The currently set priority.
         */
        public int priority;

        /**
         * The application version when the priority was set.
         */
        public long version;

        /**
         * Construct a new <code>AppPriority</code> for the application
         * described by the given <code>AppEntry</code>, specifying the given
         * <i>priority</i>.
         * 
         * @param version
         *            version of the app to which the priority applies
         * @param priority
         *            the new priority
         */
        AppPriority(long version, int priority)
        {
            this.version = version;
            this.priority = priority;
        }
    }

    /**
     * The set of application priorities set using
     * {@link #setApplicationPriority} (used to implament
     * {@link OcapAppAttributes#setApplicationPriority}).
     */
    private Hashtable appPriorities = new Hashtable();

    /**
     * The installed AppFilter. Called when launching an application to
     * determine if the application should be launched.
     */
    private Filter appFilter = new Filter();

    /**
     * The ApiRegistrar. A.K.A., the RegisteredApiManager.
     */
    private ApiRegistrar apiRegistrar;

    /**
     * The "global" AppsDatabase.
     */
    private CompositeAppsDB globalDb = new CompositeAppsDB();

    /**
     * The CallerContextManager instance.
     */
    private CCMgr ccMgr;

    /**
     * The boot process callback to be notified of salient events within the
     * OCAP boot process. Namely, boot shutdown and restart.
     */
    private BootProcessCallback bootCallback;

    /**
     * Singleton instance of the <code>ApplicationManager</code> and
     * <code>CallerContextManager</code>.
     */
    private static Manager singleton;

    /**
     * Singleton reference count.
     */
    private static int refCount;

    /**
     * Shared instance of MonitorAppPermission("registrar"). In most cases we
     * simply create a new instance because the permission is referenced in only
     * one place and would generally be used only once.
     */
    private static final MonitorAppPermission monitor_registrar = new MonitorAppPermission("registrar");

    /**
     * The Log4J Logger.
     */
    private static final Logger log = Logger.getLogger(AppManager.class.getName());

}
