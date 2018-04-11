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

import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ResourceReclamationManager.ContextID;
import org.cablelabs.impl.service.ServicesDatabase.BootProcessCallback;
import org.cablelabs.impl.service.ServicesDatabase.ShutdownCallback;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.ThreadPriority;
import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;

/**
 * Implementation of <code>CallerContextManager</code>.
 * 
 * @author Aaron Kamienski
 */
class CCMgr implements CallerContextManager
{
    /**
     * Implements <code>getInstance()</code> per the <code>Manager</code>
     * specification.
     * 
     * @return an instance of <code>CallerContextManager</code>
     */
    public static Manager getInstance()
    {
        return new CCMgr();
    }

    /**
     * Default access for testing purposes only.
     */
    CCMgr()
    {
        initThreadVars();
        if (log.isDebugEnabled())
        {
            log.debug("App Thread Priority: " + ThreadPriority.APP);
        }
        if (log.isDebugEnabled())
        {
            log.debug("System Thread Priority: " + SYSTEM_PRIORITY);
        }
        if (log.isDebugEnabled())
        {
            log.debug("Minimum System Thread Count: " + minSystemThreads);
        }
        if (log.isDebugEnabled())
        {
            log.debug("MAX Thread Priority: " + Thread.MAX_PRIORITY);
        }
        if (log.isDebugEnabled())
        {
            log.debug( "ThreadPool monitor sample period: " 
                       + threadPoolMonitorSamplePeriod + "ms" );
        }
        if (log.isDebugEnabled())
        {
            log.debug( "ThreadPool monitor adjustment frequency: " 
                       + threadPoolMonitorAdjustmentFrequency );
        }
        systemThreadPool = new ThreadPool( "System", 
                                           Thread.currentThread().getThreadGroup(), 
                                           minSystemThreads, 
                                           SYSTEM_PRIORITY );
        systemContext = new SystemContext( this, 
                                           systemThreadPool, 
                                           SYSTEM_PRIORITY );
        
        ThreadPool.startMonitoring( threadPoolMonitorSamplePeriod, 
                                    threadPoolMonitorAdjustmentFrequency );
    }

    /**
     * Initializes <code>ThreadPool</code> control variables based upon
     * environment configuration or defaults if the variables aren't
     * defined.
     * 
     * @see #updateThreadPool(int)
     * @see #minCount
     * @see #maxCount
     * @see #factor
     * @see #threshold
     */
    private void initThreadVars()
    {
        try
        {
            minSystemThreads = Integer.parseInt(MPEEnv.getEnv("OCAP.sys.tp.min"));
        }
        catch (Exception e)
        {
            minSystemThreads = MIN_SYSTEM_THREADS_DEFAULT;
        }

        try
        {
            threadPoolMonitorSamplePeriod = Integer.parseInt(MPEEnv.getEnv("OCAP.tp.monitor.period"));
        }
        catch (Exception e)
        {
            threadPoolMonitorSamplePeriod = DEFAULT_THREADPOOL_MONITOR_INTERVAL;
        }
        
        try
        {
            threadPoolMonitorAdjustmentFrequency = Integer.parseInt(MPEEnv.getEnv("OCAP.tp.monitor.adjustfrequency"));
        }
        catch (Exception e)
        {
            threadPoolMonitorAdjustmentFrequency = DEFAULT_THREADPOOL_ADJUSTMENT_FREQUENCY;
        }
    }

    /**
     * TODO: Should probably be moved to MPEEnv.
     * <p>
     * Get the value assigned to the environment variable <code>key</code>. If
     * there is no environment variable associated with <code>key</code>, return
     * <code>defValue</code>
     * 
     * @param key
     *            The environment variable name
     * @param defValue
     *            The default value
     * @return The value associated with the environment variable
     */
    /*
    private static float getEnv(String key, float defValue)
    {
        String value = MPEEnv.getEnv(key);
        if (value == null) return defValue;

        try
        {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException e)
        {
            return defValue;
        }
    }
    */

    /**
     * Returns the <code>CallerContext</code> corresponding to the calling code.
     * <p>
     * The caller context is determined by examining a <code>ThreadLocal</code>
     * variable. This variable is initialized upon first access by examining the
     * <code>ThreadGroup</code> hiearchy. If any <code>ThreadGroup</code> in the
     * hiearchy is an instance of <code>AppThreadGroup</code>, then the
     * associated context is returned. If not, then the
     * {@link #getSystemContext() system context} is returned.
     * 
     * @return the <code>CallerContext</code> corresponding to the caller
     */
    public CallerContext getCurrentContext()
    {
        return (CallerContext) cache.get();
    }

    /**
     * Returns the singleton system <code>CallerContext</code> as an instance of
     * {@link SystemContext}.
     * 
     * @return singleton instance of {@link SystemContext}
     */
    public CallerContext getSystemContext()
    {
        return systemContext;
    }

    // Description copied from CallerContext
    public void destroy()
    {
        systemContext.dispose();
        systemThreadPool.dispose();
    }

    /**
     * Retrieves the runtime application priority associated with the supplied
     * AppID.
     * 
     * @param id
     *            AppID instance to get the runtime priority for
     * @return the runtime priority of the app as an <code>int</code>.
     */
    int getRuntimePriority(AppID id)
    {
        int returnPriority = -1;
        if (id != null)
        {
            AppContext context = (AppContext) activeContexts.get(id);
            if (context != null)
            {
                Integer val = (Integer) context.get(AppContext.APP_PRIORITY);
                if (val != null) returnPriority = val.intValue();
            }
        }
        return returnPriority;
    }

    /**
     * Creates a new <code>AppContext</code> for executing the given
     * <code>XletApp</code>/<code>AppEntry</code> within the given
     * <code>AppDomain</code>. Will return <code>null</code> if there is already
     * an <i>active</i> <code>AppContext</code> with the same <code>AppID</code>
     * .
     * 
     * @param app
     *            the <code>XletApp</code> that is being launched
     * @param id
     *            the <code>AppID</code> that identifies the app
     * @param domain
     *            the domain within which the app will be executed
     * @return a newly created <code>AppContext</code> or <code>null</code> if
     *         there already exists an active context for the same
     *         <code><i>entry</i>.id</code>
     */
    synchronized AppContext createInstance(XletApp app, final AppID id, AppDomainImpl domain)
    {
        if (shutdown != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("No apps may be created in shutdown mode");
            }
            return null;
        }
        else if (activeContexts.get(id) != null)
        {
            if (log.isInfoEnabled())
            {
                log.info("App " + id + " already running");
            }
            return null;
        }
        else
        {
            AppEntry entry = domain.getAppEntry(id);
            if (entry == null)
            {
                AppManager.logErrorEvent("App " + id + " not available in database");
                return null;
            }
            else if (entry.controlCode != OcapAppAttributes.AUTOSTART && entry.controlCode != OcapAppAttributes.PRESENT)
            {
                AppManager.logErrorEvent("App " + id + " is not startable, controlCode=" + entry.controlCode);
                return null;
            }

            // TODO: can this mess be cleaned up?
            // Allow for replacement during testing.
            // Don't call own getCurrentContext().
            // Call that on the installed CCM.
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            CallerContext owner = ccm.getCurrentContext();
            
            int minThreads;
            try
            {
                minThreads = Integer.parseInt(MPEEnv.getEnv("OCAP.app.tp.min","3"));
            }
            catch (NumberFormatException e)
            {
                minThreads = 3;
            }

            // We have to create the application's thread group in the SystemContext so
            // that it does not have an incorrect parent group.  In the case of apps started
            // via the AppProxy APIs, the calling thread will belong to an application.
            final AppThreadGroup[] tg = new AppThreadGroup[1];
            try
            {
                ccm.getSystemContext().runInContextSync(new Runnable() {
                    public void run()
                    {
                        tg[0] = new AppThreadGroup(id.toString());
                    }
                });
            }
            catch (InvocationTargetException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Exception caught while trying to create application thread group!");
                }
                return null;
            }
                
            ThreadPool tp = new ThreadPool("App 0x" + id.toString(), tg[0], minThreads, Thread.NORM_PRIORITY);
            appThreadPools.put(id, tp);
            
            AppContext ctx = new AppContext(app, entry, domain, owner, this, tp, tg[0]);
            activeContexts.put(id, ctx);
            activeContextsList.addElement(ctx);
            
            if (log.isDebugEnabled())
            {
                log.debug("activate: " + ctx);
            }
            return ctx;
        }
    }

    /**
     * Locate and destroy the lowest-priority <code>AppContext</code>. This
     * operation is subject to the following rules:
     * <ul>
     * <li>If the contextId is zero, then any app may be destroyed. (I.e., there
     * is no max priority).
     * <li>The maximum priority to purge is extracted from the
     * {@link ContextID#getPriority contextId}.
     * <li>The AppID of the requestor is extracted from the
     * {@link ContextID#getAppID contextId}.
     * <li>If the maximum priority is 255, then any app but the requester and
     * the IMA may be destroyed.
     * <li>If the maximum priority is <255, then only apps of lower than maximum
     * priority may be destroyed.
     * </ul>
     * 
     * @param contextId
     *            resource reclamation context identifier
     * @param timeout
     *            time to wait for application to be destroyed
     * @param urgent
     *            <code>true</code> indicates that the requested purging is
     *            necessary to satisfy a current memory allocation request;
     *            <code>false</code> indicates that it is not
     * @return if an app was destroyed (or at least requested destroyed or not);
     *         <code>false</code> should only be returned if there was no app to
     *         destroy
     */
    boolean purgeLowestPriority(long contextId, long timeout, boolean urgent)
    {
        if (log.isInfoEnabled())
        {
            log.info("purgeLowestPriority - contextId: " + contextId + ", timeout: " + timeout + ", urgent: " + urgent);
        }
        final boolean systemRequest = contextId == 0L;
        final int minApps = (urgent && systemRequest) ? 0 : 1; // if not urgent,
                                                               // don't kill
                                                               // last app
        if (activeContextsList.size() <= minApps) return false;

        final AppID id = ContextID.getAppID(contextId);
        final int maxPriority = systemRequest ? Integer.MAX_VALUE : ContextID.getPriority(contextId);

        AppContext toDestroy = null;
        synchronized (activeContextsList)
        {
            if (activeContextsList.size() <= minApps) return false;

            // Get the "minimum" entry in the list
            Comparator comp = new Comparator()
            {
                public int compare(Object o1, Object o2)
                {
                    // short circuit
                    if (o1 == o2) return 0;

                    AppContext a1 = (AppContext) o1;
                    AppContext a2 = (AppContext) o2;
                    int p1 = a1.getPriority();
                    int p2 = a2.getPriority();

                    // Sort primarily based upon priority
                    int rc = p1 - p2;
                    // However, if priorities are equal...
                    if (rc == 0)
                    {
                        // Treat IMA as higher priority than all apps
                        if (p1 == 255 && a1.isMonApp())
                            rc = 1;
                        else if (p2 == 255 && a2.isMonApp())
                            rc = -1;
                        // Treat current app as higher priority than all other
                        // apps
                        else
                        {
                            AppID i1 = (AppID) a1.get(CallerContext.APP_ID);
                            AppID i2 = (AppID) a2.get(CallerContext.APP_ID);
                            if (id.equals(i1))
                                rc = 1;
                            else if (id.equals(i2)) rc = -1;
                        }
                    }

                    // NOTE: equal elements should remain in original order
                    return rc;
                }
            };
            // Get one that we want
            AppContext app = (AppContext) Collections.min(activeContextsList, comp);
            Collections.sort(activeContextsList, comp);

            if (log.isDebugEnabled())
            {
                log.debug("ActiveContexts: " + activeContextsList);
            }

            // Ensure that this is the one we want (else there is nothing left)
            int p = ((Integer) app.get(CallerContext.APP_PRIORITY)).intValue();
            if (p < maxPriority || (maxPriority == 255 && !id.equals(app.get(CallerContext.APP_ID))))
            {
                toDestroy = app;
                if (log.isInfoEnabled())
                {
                    log.info("Avail for purge: " + app);
                }
        }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("Not avail for purge: " + app);
                }
            }
        }

        if (toDestroy == null)
            return false;

        // Destroy it (wait until destroyed or timeout)
        if (toDestroy.destroyApp(timeout))
            activeContextsList.removeElement(toDestroy); // not
                                                                                        // up
                                                                                        // for
                                                                                        // later
                                                                                        // destruction
        // TODO: what if app WONT'T destroy... e.g., if in infinite loop?

        return true;
    }

    /**
     * Removes the given <code>AppContext</code> from the set of currently
     * <i>active</i> contexts.
     * 
     * @param ctx
     *            the <code>AppContext</code> to remove from the <i>active</i>
     *            set
     * @param entry
     *            the <code>AppEntry</code> describing the context
     */
    synchronized void deactivate(AppContext ctx, AppID id)
    {
        if (log.isDebugEnabled())
        {
            log.debug("deactivate(" + ctx + ")");
        }

        AppContext old = (AppContext) activeContexts.get(id);
        //same app may be restarted - don't remove or shutdown if different
        if (ctx == old)
        {
            removeActiveContext(ctx, id);

            // If currently in "shutdown" mode and all apps have been destroyed...
            // Notify boot process that it may continue
            if (shutdown != null && activeContexts.size() == 0)
            {
                if (log.isInfoEnabled())
                {
                    log.info("All applications have been shutdown.");
                }

                shutdown.complete(shutdownAct);
                shutdownAct = null;
                shutdown = null;
            }
        }
    }

    public void removeActiveContext(AppContext ctx, AppID id)
    {
        //may already be removed
        if (ctx == null)
        {
            return;
        }

        synchronized(activeContextsList)
        {
            AppContext old = (AppContext) activeContexts.get(id);
            //same app may be restarted - don't remove or shutdown if different
            if (ctx == old)
            {
                activeContexts.remove(id);
                activeContextsList.removeElement(ctx);
            }
            else
            {
                // I don't expect this to happen, but will warn if it does
                if (log.isDebugEnabled())
                {
                    log.debug("deactivating non-active context ignored " + id);
                }
            }
        }
    }

    /**
     * Implements {@link BootProcessCallback#shutdown}.
     * 
     * @param callback
     *            object used to notify of async shutdown completion
     * @return <code>true</code> indicating shutdown will be completed
     *         asynchronously; or <code>false</code> if nothing needs to be done
     */
    boolean shutdownApps(ShutdownCallback callback, BootProcessCallback act)
    {
        if (log.isInfoEnabled())
        {
            log.info("Shutting down all running applications (" + activeContextsList.size() + ")");
        }

        synchronized (this)
        {
            // If no apps, then shutdown procedures are unnecessary
            if (activeContextsList.isEmpty()) return false;

            if (activeContextsList.isEmpty())
            {
                // Nothing to do, return before setting shutdown and
                // shutdownAct, or re-clear them.
                return false; // Indicate synchronous completion.
            }

            // Indicate that we are in shutdown mode here -- no more contexts
            // may be created
            shutdown = callback;
            shutdownAct = act;
        }

        // Start shutting down existing contexts.
        while (!activeContextsList.isEmpty())
        {
            AppContext app = (AppContext) activeContextsList.elementAt(0);
            activeContextsList.removeElementAt(0);

            if (log.isDebugEnabled())
            {
                log.debug("Shutting down: " + app);
            }

            // Wait very little time for app to be destroyed.
            // Instead, we will key off of last invocation of deactivate.
            // Don't really care if it failed or succeeded
            app.destroyApp(10L);
        }

        // When the last context is shutdown, deactivate() will signal the
        // ShutdownCallback.
        // And clear shutdown indicating that more apps can be created.

        return true;
    }

    /**
     * Can be used to determine if an application is currently active with the
     * given <code>AppID</code>.
     * <p>
     * The value returned doesn't consider the state of the Xlet.
     * 
     * @param id
     * @return <code>true</code> if there is an active <code>AppContext</code>
     *         for the given <code>AppID</code>; <code>false</code> otherwise
     */
    synchronized boolean isActive(AppID id)
    {
        return activeContexts.get(id) != null;
    }

    /**
     * Returns the current <code>AbstractCallerContext</code> from the thread
     * local cache.
     * 
     * @return the current <code>AbstractCallerContext</code>
     */
    AbstractCallerContext getCache()
    {
        return (AbstractCallerContext) cache.get();
    }

    /**
     * Sets the current <code>AbstractCallerContext</code> in the thread local
     * cache.
     * 
     * @param acc
     *            the new current caller context
     */
    void setCache(AbstractCallerContext acc)
    {
        cache.set(acc);
    }

    /**
     * Returns the current caller context by examining the
     * <code>ThreadGroup</code> hiearchy. This is used to initialize the
     * <code>ThreadLocal</code> cache the first time it is accessed for a
     * thread.
     * 
     * @return caller's <code>CallerContext</code>
     */
    private CallerContext getThreadGroupCurrent()
    {
        ThreadGroup tg = getThreadGroup(Thread.currentThread());
        do
        {
            if (tg instanceof AppThreadGroup)
            {
                return ((AppThreadGroup) tg).getCallerContext();
            }
            tg = getParent(tg);
        }
        while (tg != null);

        return systemContext;
    }

    /**
     * Invokes {@link Thread#getThreadGroup} within a {@link PrivilegedAction}.
     * 
     * @param thread
     *            the thread to invoke <code>getThreadGroup()</code> on
     * @return the thread <code>ThreadGroup</code>
     */
    static ThreadGroup getThreadGroup(final Thread thread)
    {
        return (ThreadGroup) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return thread.getThreadGroup();
            }
        });
    }

    /**
     * Invokes {@link ThreadGroup#getParent} within a {@link PrivilegedAction}.
     * 
     * @param tg
     *            the thread group to invoke <code>getParent()</code> on
     * @return the thread group's parent thread group, or <code>null</code> if
     *         the root group
     */
    static ThreadGroup getParent(final ThreadGroup tg)
    {
        return (ThreadGroup) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return tg.getParent();
            }
        });
    }

    /**
     * Thread local storage is used to cache the <code>CallerContext</code> for
     * a given thread. The value is initialized based upon the
     * <code>ThreadGroup</code> for the calling <code>Thread</code>, but may be
     * set explicitly via {@link #setCache}. This allows for optimized
     * <code>CallerContext</code> lookup as well as the ability to change the
     * context in which a thread apparently executes.
     */
    private ThreadLocal cache = new ThreadLocal()
    {
        /**
         * Performs initial lookup of <code>CallerContext</code>.
         * 
         * @return the <code>CallerContext</code> based upon the calling
         *         <code>Thread</code>'s <code>ThreadGroup</code>
         */
        protected Object initialValue()
        {
            return getThreadGroupCurrent();
        }
    };

    /**
     * The set of <i>active</i> contexts.
     * 
     * @see #createInstance(XletApp, AppID, AppDomainImpl)
     */
    private Hashtable activeContexts = new Hashtable();

    /**
     * The list of <i>active</i> contexts. This is maintained in lock-step with
     * {@link #activeContext}, however it is only used by
     * {@link #purgeLowestPriority}. It is intended to be a list of the active
     * {@link AppContext AppContexts}, sorted from low-to-high priority.
     * However, it is only sorted when necessary. The main point is to already
     * have the memory allocated for this array/list so we don't have to do it
     * during a purge operation.
     */
    private Vector activeContextsList = new Vector();

    /**
     * When non-<code>null</code> no applications may be created.
     */
    private ShutdownCallback shutdown;

    /**
     * ACT for invocation of
     * {@link ShutdownCallback#complete(BootProcessCallback)}.
     */
    private BootProcessCallback shutdownAct;

    /**
     * The priority for system context threads.
     * <p>
     * This can be configured via the
     * <code>"OCAP.thread.priority.system_med"</code> variable.
     */
    private static final int SYSTEM_PRIORITY = ThreadPriority.SYSTEM_MED;

    /**
     * The <code>ThreadPool</code>s used by all <code>CallerContext</code>s
     * managed by this <code>CallerContextManager</code>.
     */
    private Hashtable appThreadPools = new Hashtable();
    private ThreadPool systemThreadPool;

    /**
     * The default minimum/initial number system context threads.
     * <p>
     * This is the minimum/initial number of system threads that will be used
     * when the <code>"OCAP.sys.tp.min"</code> variable is undefined.
     */
    private static final int MIN_SYSTEM_THREADS_DEFAULT = 5;

    /**
     * The minimum/initial number system context threads.
     */
    private int minSystemThreads;
    
    /**
     * The ThreadPool sampling interval
     */
    private int threadPoolMonitorSamplePeriod;
    
    private static final int DEFAULT_THREADPOOL_MONITOR_INTERVAL = 2000;
    
    /**
     * The ThreadPool adjustment frequency
     */
    private int threadPoolMonitorAdjustmentFrequency;
    
    private static final int DEFAULT_THREADPOOL_ADJUSTMENT_FREQUENCY = 2;
    
    /**
     * The singleton <i>system</i> <code>CallerContext</code>.
     * 
     * @see #getSystemContext()
     */
    private SystemContext systemContext;

    /**
     * Log4J logger.
     */
    private static final Logger log = Logger.getLogger(CCMgr.class.getName());
}
