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

package org.cablelabs.impl.manager.reclaim;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.log4j.Logger;
import org.cablelabs.impl.java.ReclaimThread;
import org.cablelabs.impl.manager.ApplicationManager;
import org.cablelabs.impl.manager.Manager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ResourceReclamationManager;
import org.cablelabs.impl.ocap.OcapMain;
import org.cablelabs.impl.util.ExtendedSystemEventManager;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.ThreadPriority;
import org.ocap.system.event.ResourceDepletionEvent;
import org.ocap.system.event.SystemEventListener;
import org.ocap.system.event.SystemEventManager;

/**
 * Default implementation of the <code>ResourceReclamationManager</code>. Native
 * registration is {@link #getInstance performed} such that the following
 * methods may be invoked in an effort to reclaim resources:
 * <ul>
 * <li> {@link #performGC}
 * <li> {@link #notifyMonApp}
 * <li> {@link #destroyApp}
 * </ul>
 * The implementation also installs hooks with the VM by creating an instance of
 * {@link ReclaimThread} that is used by the VM to invoke resource reclamation
 * procedures when it finds them necessary.
 * 
 * @author Aaron Kamienski
 */
public class RRMgrImpl implements ResourceReclamationManager
{
    private static final Logger log = Logger.getLogger(RRMgrImpl.class);

    /**
     * Returns a new instance of <code>RRMgrImpl</code>.
     * 
     * This new instance is registered natively so that it is invoked to handle
     * resource reclamation requests as appropriate.
     * 
     * @return a new instance of <code>RRMgrImpl</code>
     */
    public static Manager getInstance()
    {
        RRMgrImpl rr = new RRMgrImpl();

        // Perform post-constructor initialization
        rr.init();

        return rr;
    }

    /** Constructor is not public. */
    RRMgrImpl()
    {
        //no-op
    }

    /**
     * Initializes this instance. This is post-constructor initialization called
     * by {@link #getInstance}.
     * <p>
     * This implementation does the following:
     * <ol>
     * <li>invokes {@link #register register()}
     * <li>
     * <code>thresholdThread = {@link #startThresholdChecker() startThresholdChecker()}</code>
     * </ol>
     */
    void init()
    {
        // Register the given ResourceReclamationManager natively
        register();

        // Start VM reclaim thread
        reclaimThread = startReclaimThread();

        // Start threshold-checking thread
        thresholdThread = startThresholdChecker();

    }

    /**
     * Creates and runs an instance of {@link ReclaimThread} as configured. This
     * can be overridden by a subclass to use a custom
     * <code>ReclaimThread</code>.
     * <p>
     * The default implementation creates and starts an instance of
     * <code>ReclaimThread</code> that does the following:
     * <ol>
     * <li>Invokes {@link #notifyMonApp} given a level of 1 (one). Returns next
     * level of 2 (two).
     * <li>Invokes {@link #destroyApp} given a level of 2 (two). If an app was
     * destroyed, returns 2 (two). If no app was destroyed, returns 0 (zero).
     * </ol>
     * 
     * @return the started thread, if any
     */
    Thread startReclaimThread()
    {
        if (!"true".equals(MPEEnv.getEnv("OCAP.rr.reclaim.thread", "true"))) return null;

        ReclaimThread thread = new ReclaimThread()
        {
            public int reclaimMemory(int level, long contextId)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("reclaimMemory requested " + level + " " + Long.toHexString(contextId));
                }

                switch (level)
                {
                    // TODO: add support for clearing caches

                    case 1:
                        notifyMonApp(ResourceDepletionEvent.RESOURCE_VM_MEM_DEPLETED, false, contextId);
                        return ++level;

                    case 2:
                        return destroyApp(false, contextId) ? level // As long
                                                                    // as there
                                                                    // are apps
                                                                    // to
                                                                    // destroy,
                                                                    // return
                                                                    // same
                                                                    // level
                                : 0; // Return zero when no apps to destroy

                    default:
                        return 0;
                }
            }
        };
        /*
         * thread.start();
         * 
         * // Set priority of reclamation thread.
         * thread.setPriority(RECLAMATION_PRIORITY);
         */

        return thread;
    }

    /**
     * Creates and runs an instance of <code>ThresholdChecker</code> as
     * configured. This can be overridden by a subclass to use a custom
     * <code>ThresholdChecker</code>.
     * <p>
     * The default implementation does the following:
     * <ol>
     * <li>If <code>"OCAP.rr.threshold.max_used"</code> is defined as non-zero,
     * then a {@link UsedMemThreshold} is returned.
     * <li>If <code>"OCAP.rr.threshold.min_free"</code> is defined as non-zero,
     * then a {@link FreeMemThreshold} is returned.
     * <li>Otherwise, a {@link FreeMemThreshold} is returned with a threshold of
     * 1M.
     * </ol>
     */
    Thread startThresholdChecker()
    {
        // If max_used defined, start ThresholdChecker
        long maxUsed = MPEEnv.getEnv("OCAP.rr.threshold.max.used", 0L);
        if (maxUsed > 0)
        {
            Thread thread = new UsedMemThreshold(maxUsed);
            thread.start();
            thread.setPriority(RECLAMATION_PRIORITY);
            if (log.isInfoEnabled())
            {
                log.info("using UsedMemThreshold checker");
            }
            return thread;
        }

        // If min_free defined, start ThresholdChecker
        long minFree = MPEEnv.getEnv("OCAP.rr.threshold.min.free", 100 * 1024L);
        if (minFree > 0)
        {
            Thread thread = new FreeMemThreshold(minFree);
            thread.start();
            thread.setPriority(RECLAMATION_PRIORITY);
            if (log.isInfoEnabled())
            {
                log.info("using FreeMemThreshold checker");
            }

            return thread;
        }
        if (log.isInfoEnabled())
        {
            log.info("using no memory threshold checker");
        }

        // Else, do nothing
        return null;
    }

    /**
     * Implements {@link Manager#destroy()}. Unregisters itself so that it will
     * no longer be used to handle resource reclamation requests.
     * <p>
     * This implementation does the following:
     * <ol>
     * <li>invokes {@link #stopThresholdChecker() stopThresholdChecker()}
     * <li>invokes {@link #unregister unregister()}
     * </ol>
     */
    public void destroy()
    {
        // Shutdown the threshold checker, if running
        stopThresholdChecker();

        // Shutdown the reclaim thread, if running
        stopReclaimThread();

        unregister();
    }

    /**
     * Shuts down the <code>ReclaimThread</code> if there is any. This can be
     * overridden by a subclass to use handle a specially-created
     * <code>ReclaimThread</code>. The default implementation
     * {@link Thread#interrupt interrupts} the <code>Thread</code> previously
     * returned by {@link #startReclaimThread()}.
     */
    void stopReclaimThread()
    {
        // stop the ReclaimThread
        Thread thread = reclaimThread;
        reclaimThread = null;
        if (thread != null) thread.interrupt();
    }

    /**
     * Shuts down the <code>ThresholdChecker</code> if there is any. This can be
     * overridden by a subclass to use handle a <code>ThresholdChecker</code>.
     * The default implementation {@link Thread#interrupt interrupts} the
     * <code>Thread</code> previously returned by
     * {@link #startThresholdChecker()}.
     */
    void stopThresholdChecker()
    {
        // stop ThresholdChecker
        Thread thread = thresholdThread;
        thresholdThread = null;
        if (thread != null) thread.interrupt();
    }

    /**
     * Notifies the registered {@link ResourceDepletionEvent}
     * {@link SystemEventListener listener}, if any, so that it may take
     * appropriate resource reclamation steps.
     * 
     * @param type
     *            type of resource being requested reclaimed
     * @param proactive
     *            <code>true</code> if this is a proactive request to reclaim
     *            memory
     * @param contextId
     *            identifies the application context that (implicitly) initiated
     *            the resource reclamation request; zero if not specific to an
     *            application
     * @see #MONAPP_TIMEOUT
     */
    boolean notifyMonApp(int type, boolean proactive, long contextId)
    {
        if (log.isDebugEnabled())
        {
            log.debug("MonApp notification requested");
        }

        // Invoke MonApp listener and wait
        ExtendedSystemEventManager sem = (ExtendedSystemEventManager) SystemEventManager.getInstance();
        int status = sem.log(createEvent(type, proactive, contextId), MONAPP_TIMEOUT);

        return status != ExtendedSystemEventManager.NOLISTENER;
    }

    /**
     * Destroys the lowest priority running application in an effort to reclaim
     * resources.
     * <p>
     * If <i>contextId</i> is zero, then potentially all applications may be
     * destroyed, including the IMA (but leaving it until last). If
     * <i>contextId</i> is non-zero, then only applications of a lower-priority
     * than specified by the <i>contextId</i> will be killed.
     * 
     * @param proactive
     *            indicates whether a monitored threshold has been crossed or
     *            there is a current failure
     * @param contextId
     *            identifies the application context that (implicitly) initiated
     *            the resource reclamation request; zero if not specific to an
     *            application
     * @return <code>true</code> if an application was destroyed;
     *         <code>false</code> if no application was destroyed
     * 
     * @see #DESTROY_TIMEOUT
     * @see #DESTROY_ENABLED
     */
    boolean destroyApp(boolean proactive, long contextId)
    {
        if (!DESTROY_ENABLED) return false;

        if (log.isDebugEnabled())
        {
            log.debug("App Destruction requested " + (proactive ? "proactive " : "") + Long.toHexString(contextId));
        }

        // Get the AppManager...
        ApplicationManager am = (ApplicationManager) ManagerManager.getInstance(ApplicationManager.class);

        // ...request that the lowest lower-priority app be destoyed
        return am.purgeLowestPriorityApp(contextId, DESTROY_TIMEOUT, !proactive);
    }

    /**
     * Perform up to <i>nCycles</i> GC cycles, as long as the threshold has been
     * crossed.
     * 
     * @param nCycles
     *            number of GC cycles
     * @param checker
     *            used to perform threshold check (by calling
     *            {@link ThresholdCheck#checkThreshold})
     */
    void performGC(int nCycles, ThresholdCheck checker)
    {
        memoryUsage("Prior to GC");

        for (int i = 0; i < nCycles - 1 && checker.checkThreshold(true); ++i)
        {
            //findbugs complains that explicit garbage collection is a bad idea - except when its not.
            //findbugs configured to ignore...
            System.gc();
            System.runFinalization();
        }
        // Always make the last cycle a compacting cycle
        compactingGC();

        memoryUsage("After GC");
    }

    /**
     * Logs current memory usage.
     * 
     * @param msg
     *            message describing current state added to logging
     */
    private void memoryUsage(String msg)
    {
        if (log.isDebugEnabled())
        {
            Runtime runtime = Runtime.getRuntime();
            long free = runtime.freeMemory();
            long total = runtime.totalMemory();
            long used = total - free;
            if (log.isDebugEnabled())
        {
            log.debug(msg + ": used=" + used + " free=" + free + " total=" + total);
        }
    }
    }

    /**
     * Requests that a <i>compacting</i> garbage collection cycle be performed.
     * This implementation simply does a regular GC but it may be overridden by
     * a sublcass.
     */
    void compactingGC()
    {
        //findbugs complains that explicit garbage collection is a bad idea - except when its not.
        //findbugs configured to ignore...
        System.gc();
    }

    /**
     * Create a new <code>ResourceDepletionEvent</code> to signal resource
     * depletion.
     * 
     * @param type
     *            type of resource being requested reclaimed
     * @param proactive
     *            <code>true</code> if this is a proactive request to reclaim
     *            memory
     * @param contextId
     *            identifies the application context that (implicitly) initiated
     *            the resource reclamation request; zero if not specific to an
     *            application
     * @return a new <code>ResourceDepletionEvent</code> to signal resource
     *         depletion
     */
    private ResourceDepletionEvent createEvent(final int type, final boolean proactive, final long contextId)
    {
        return (ResourceDepletionEvent) AccessController.doPrivileged(new PrivilegedAction()
        {
            public Object run()
            {
                return new ResourceDepletionEvent(type, proactive ? "Depletion threshold reached" : "Failure recovery",
                        System.currentTimeMillis(), contextId == 0L ? null : ContextID.getAppID(contextId));
            }
        });
    }

    /**
     * Registers this <code>RRMgrImpl</code> natively to handle resource
     * reclamation. This method may be overridden by subclasses if necessary.
     */
    void register()
    {
        gcCallback = nAddCallback(CALLBACK_JAVA, new NonRecursiveWrapper(new GCCallback()));
        monAppCallback = nAddCallback(CALLBACK_MONAPP, new NonRecursiveWrapper(new MonAppCallback()));
        destroyCallback = nAddCallback(CALLBACK_DESTROY, new NonRecursiveWrapper(new DestroyCallback()));
    }

    /**
     * Unregisters this <code>RRMgrImpl</code> so that it no longer handles
     * resource reclamation requests. This method may be overridden by
     * subclasses if necessary.
     */
    void unregister()
    {
        nRemoveCallback(CALLBACK_JAVA, gcCallback);
        gcCallback = 0;
        nRemoveCallback(CALLBACK_MONAPP, monAppCallback);
        monAppCallback = 0;
        nRemoveCallback(CALLBACK_DESTROY, destroyCallback);
        destroyCallback = 0;
    }

    /**
     * Threshold checking interface. The {@link #checkThreshold} method can be
     * used to determine if a threshold has been crossed.
     * 
     * @author Aaron Kamienski
     */
    interface ThresholdCheck
    {
        /**
         * Tests to determine if the VM Java heap memory usage threshold has
         * been crossed.
         * 
         * @param breathingRoom
         *            if <code>true</code> then test for additional breathing
         *            room beyond the default free memory threshold
         * @return <code>true</code> if free memory has dropped below a specific
         *         threshold; <code>false</code> if not
         */
        boolean checkThreshold(boolean breathingRoom);
    }

    /**
     * Thread class that performs Java heap threshold checking.
     * 
     * @author Aaron Kamienski
     */
    abstract class ThresholdCheckThread extends Thread implements ThresholdCheck
    {
        ThresholdCheckThread()
        {
            super("RR-Threshold");

            setPriority(Thread.MAX_PRIORITY);
        }

        /**
         * Thread used to implement asynchronous VM memory threshold test and
         * response. Simply enters a loop that sleeps, checks, sleeps, checks...
         * continuously. Can be stopped by {@link Thread#interrupt}.
         */
        public void run()
        {
            int i = 1;
            while (!Thread.interrupted())
            {
                try
                {
                    Thread.sleep(i * THRESHOLD_SLEEP);
                }
                catch (InterruptedException e)
                {
                    break;
                }

                try
                {
                    // If still not past threshold after cycle...
                    if (!thresholdCycle())
                        i = 1; // reset, if necessary
                    else
                    {
                        // else, back off a little (we apparently ain't helpin'
                        // any)
                        ++i;
                        if (log.isDebugEnabled())
                        {
                            log.debug("Increasing sleep time to " + (i * THRESHOLD_SLEEP) + "ms");
                        }
                }
                }
                catch (Exception e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Exception during Async Threshold cycle", e);
                    }
            }
        }
            if (log.isWarnEnabled())
            {
                log.warn("Async Threshold thread exiting");
            }
        }

        /**
         * Performs a single iteration of the VM memory threshold check and any
         * necessary attempt at resource reclamation.
         * 
         * @return <code>false</code> if threshold is not crossed;
         *         <code>true</code> if theshold is crossed even after trying to
         *         reclaim memory
         */
        private boolean thresholdCycle()
        {
            if (checkThreshold(false))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Low memory threshold detected");
                }

                // Perform forced GC
                performGC(N_GC_CYCLES, this);

                // Require minimum memory plus breathing room
                if (checkThreshold(true))
                {
                    // Let MonApp perform cleanup
                    if (notifyMonApp(ResourceDepletionEvent.RESOURCE_VM_MEM_DEPLETED, true, 0L))
                        performGC(N_MONAPP_GC_CYCLES, this);

                    // Require minimum memory plus breathing room
                    while (checkThreshold(true))
                    {
                        // Destroy low priority applications
                        if (destroyApp(true, 0L))
                            performGC(N_DESTROY_GC_CYCLES, this);
                        else
                            break;
                    }
                }

                // Verify that we at least have minimum memory
                boolean low = checkThreshold(false);
                if (low)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("VM memory low even after reclamation");
                    }
                }
                return low;
            }
            return false;
        }
    }

    /**
     * An implementation of <code>ThresholdCheckThread</code> which monitors the
     * amount of {@link Runtime#freeMemory() free memory} in the system.
     * 
     * @author Aaron Kamienski
     */
    private class FreeMemThreshold extends ThresholdCheckThread
    {
        FreeMemThreshold(long minFree)
        {
            this(minFree, 1.25F);
        }

        FreeMemThreshold(long minFree, float extra)
        {
            this.minFree = minFree;
            this.minFreeExtra = (long) (minFree * extra);
        }

        public boolean checkThreshold(boolean breathingRoom)
        {
            Runtime runtime = Runtime.getRuntime();

            return runtime.freeMemory() < (breathingRoom ? minFreeExtra : minFree);
        }

        private final long minFree;

        private final long minFreeExtra;
    }

    /**
     * An implementation of <code>ThresholdCheckThread</code> which monitors the
     * amount of memory in use (defined as
     * <code>{@link Runtime#totalMemory() total} - 
     * {@link Runtime#freeMemory() free}</code>).
     * 
     * @author Aaron Kamienski
     */
    private class UsedMemThreshold extends ThresholdCheckThread
    {
        UsedMemThreshold(long maxUsed)
        {
            this(maxUsed, 0.80F);
        }

        UsedMemThreshold(long maxUsed, float less)
        {
            this.maxUsed = maxUsed;
            this.maxUsedLess = (long) (maxUsed * less);
        }

        public boolean checkThreshold(boolean breathingRoom)
        {
            Runtime runtime = Runtime.getRuntime();

            return (runtime.totalMemory() - runtime.freeMemory()) > (breathingRoom ? maxUsedLess : maxUsed);
        }

        private final long maxUsed;

        private final long maxUsedLess;
    }

    /**
     * An instance of <code>ThresholdCheck</code> that always returns
     * <code>true</code>.
     */
    private final ThresholdCheck ALWAYS = new ThresholdCheck()
    {
        public boolean checkThreshold(boolean ignored)
        {
            return true;
        }
    };

    /**
     * A <code>Callback</code> that performs multiple garbage
     * collection/finalization cycles when invoked.
     * 
     * @author Aaron Kamienski
     */
    private class GCCallback implements Callback
    {
        /**
         * @see RRMgrImpl#performGC
         * @see RRMgrImpl#N_GC_CYCLES
         */
        public boolean releaseResources(int type, int reason, long contextId)
        {
            performGC(N_GC_CYCLES, ALWAYS);
            return false;
        }
    }

    /**
     * A <code>Callback</code> that notifies the <i>Monitor Application</i> by
     * sending a {@link ResourceDepletionEvent} to the appropriate
     * {@link SystemEventListener} (if any).
     * 
     * @author Aaron Kamienski
     */
    private class MonAppCallback implements Callback
    {
        /**
         * @see RRMgrImpl#notifyMonApp(int, boolean, long)
         */
        public boolean releaseResources(int type, int reason, long contextId)
        {
            if (notifyMonApp(type == TYPE_SYSTEM ? ResourceDepletionEvent.RESOURCE_SYS_MEM_DEPLETED
                    : ResourceDepletionEvent.RESOURCE_VM_MEM_DEPLETED, reason == REASON_THRESHOLD, contextId))
            {
                // Perform GC only if listener was invoked.
                // Even if a timeout occurred
                performGC(N_MONAPP_GC_CYCLES, ALWAYS);
                return true;
            }
            return false;
        }
    }

    /**
     * A <code>Callback</code> that requests that the
     * <code>ApplicationManager</code>
     * {@link ApplicationManager#purgeLowestPriorityApp(long, long, boolean)
     * purge} the lowest <i>lower-priority</i> running application from memory.
     * 
     * @author Aaron Kamienski
     */
    private class DestroyCallback implements Callback
    {
        /**
         * @see RRMgrImpl#destroyApp(boolean, long)
         */
        public boolean releaseResources(int type, int reason, long contextId)
        {
            if (destroyApp(reason == REASON_THRESHOLD, contextId))
            {
                performGC(N_DESTROY_GC_CYCLES, ALWAYS);
                return true;
            }
            return false;
        }
    }

    /**
     * Token returned from {@link #nAddCallback(int, Callback)} when adding GC
     * callback. Could be grouped together with other arbitrary (i.e.,
     * non-destructive) callbacks if we chose to support them. Used to remove
     * the callback during shutdown.
     */
    private int gcCallback;

    /**
     * Token returned from {@link #nAddCallback(int, Callback)} when adding
     * MonApp callback. Used to remove the callback during shutdown.
     */
    private int monAppCallback;

    /**
     * Token returned from {@link #nAddCallback(int, Callback)} when adding
     * destroy callback. Used to remove the callback during shutdown.
     */
    private int destroyCallback;

    /**
     * A <i>type</i> to be specified on a call to
     * {@link #nAddCallback(int, Callback)}. Non-destructive Java Callbacks,
     * including the default GC Callback.
     */
    private static final int CALLBACK_JAVA = 0;

    /**
     * A <i>type</i> to be specified on a call to
     * {@link #nAddCallback(int, Callback)}. Potentially destructive MonApp
     * Callback.
     */
    private static final int CALLBACK_MONAPP = 1;

    /**
     * A <i>type</i> to be specified on a call to
     * {@link #nAddCallback(int, Callback)}. Destructive Application Destroy
     * Callback.
     */
    private static final int CALLBACK_DESTROY = 2;

    /**
     * Adds the given <code>Callback</code> object of the specified type.
     * 
     * @param type
     *            {@link #CALLBACK_JAVA}, {@link #CALLBACK_MONAPP}, or
     *            {@link #CALLBACK_DESTROY}
     * @param callback
     *            <code>Callback</code> object
     * @return token that can be passed on {@link #nRemoveCallback(int, int)}
     */
    private synchronized static native int nAddCallback(int type, Callback callback);

    /**
     * Removes the callback previously added with
     * {@link #nAddCallback(int, Callback)}.
     * 
     * @param type
     *            type originall specified when added
     * @param token
     *            token returned by <code>nAddCallback</code>.
     */
    private synchronized static native void nRemoveCallback(int type, int token);

    /**
     * Number of GC/finalization cycles to perform. Will always perform at least
     * one GC cycle. Initialized based upon <code>OCAP.rr.gc.count</code>.
     */
    private static final int N_GC_CYCLES = MPEEnv.getEnv("OCAP.rr.gc.count", 2);

    /**
     * Number of GC/finalization cycles to perform upon MonApp notification Will
     * always perform at least one GC cycle. Initialized based upon
     * <code>OCAP.rr.monapp.gc_count</code>.
     */
    private static final int N_MONAPP_GC_CYCLES = MPEEnv.getEnv("OCAP.rr.monapp.gc.count", N_GC_CYCLES);

    /**
     * Number of GC/finalization cycles to perform upon application destruction.
     * Will always perform at least one GC cycle. Initialized based upon
     * <code>OCAP.rr.destroy.gc_count</code>.
     */
    private static final int N_DESTROY_GC_CYCLES = MPEEnv.getEnv("OCAP.rr.destroy.gc.count", N_GC_CYCLES);

    /**
     * Timeout (from 5-60sec) expressed in milliseconds to wait for MonApp's
     * ResourceDepletionEvent listener to return before continuing on.
     * Initialized based upon <code>OCAP.rr.monapp.timeout</code>.
     */
    private static final long MONAPP_TIMEOUT = MPEEnv.getEnv("OCAP.rr.monapp.timeout", 60000L);

    /**
     * Timeout expressed in milliseconds to wait for destroyed app to get to
     * <code>NOT_LOADED</code> state before returning. Initialized based upon
     * <code>OCAP.rr.destroy.timeout</code>.
     */
    private static final long DESTROY_TIMEOUT = MPEEnv.getEnv("OCAP.rr.destroy.timeout", 60000L);

    /**
     * How long the Async Threshold should sleep between checks. Initialized
     * based upon <code>OCAP.rr.threshold.sleep</code>.
     */
    private static final long THRESHOLD_SLEEP = MPEEnv.getEnv("OCAP.rr.threshold.sleep", 2000L);

    /**
     * Whether application destruction should be enabled or not. By default, it
     * is enabled, however it may be disabled by defining
     * <code>OCAP.rr.destroy.enabled</code> as <code>false</code>.
     */
    private static final boolean DESTROY_ENABLED = "true".equals(MPEEnv.getEnv("OCAP.rr.destroy.enabled", "true"));

    /**
     * Thread, if any, that is monitoring for VM memory threshold(s).
     */
    private Thread thresholdThread;

    /**
     * Thread, if any, that is to be used by the VM to perform reclamation
     * operations.
     */
    private Thread reclaimThread;

    private final int RECLAMATION_PRIORITY = ThreadPriority.SYSTEM_HI;

    static
    {
        // Load JNI code
        OcapMain.loadLibrary();
    }
}
