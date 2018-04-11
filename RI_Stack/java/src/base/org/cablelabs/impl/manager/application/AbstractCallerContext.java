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

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.ResourceReclamationManager.ContextID;
import org.cablelabs.impl.util.MPEEnv;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.event.InvocationEvent;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlContext;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * Abstract base class for <code>CallerContext</code> implementations in this
 * package.
 * 
 * @see AppContext
 * @see SystemContext
 * 
 * @author Aaron Kamienski
 */
abstract class AbstractCallerContext implements CallerContext, AppThreadGroup.ExceptionListener
{
    /**
     * Creates an instance of AbstractCallerContext.
     * <p>
     * Note that the provided <code>ExecQueue</code> must execute
     * <code>Runnable</code>s such that they run within this
     * <code>CallerContext</code>. Either they run within
     * <code>AppThreadGroup</code> or with the appropriate caching mechanism.
     * [This does imply a bit of a chicken-and-egg issue... context cannot be
     * created w/out queue; queue cannot be created w/out context.]
     * 
     * @param ccMgr
     *            the <code>CCMgr</code> managing this caller context
     * @param tp
     *            the <code>ThreadPool</code> to use for
     *            {@link #runInContext(Runnable)}
     * @param q
     *            the <code>ExecQueue</code> to use for
     *            {@link #runInContext(Runnable)}
     * @param threadPriority
     *            the priority at which context threads should execute
     */
    protected AbstractCallerContext(CCMgr ccMgr, ThreadPool tp, ExecQueue q)
    {
        this.ccMgr = ccMgr;
        this.tp = tp;
        this.q = q;
    }

    /**
     * May be used to set the <code>ExecQueue</code> after construction if a
     * <code>null</code> was passed in to the constructor. This should be
     * invoked as-soon-as-possible after the constructor executes, preferably
     * within the constructor of a sub-class.
     * 
     * @param q
     *            the <code>ExecQueue</code> to use for
     *            {@link #runInContext(Runnable)}
     * @throws IllegalArgumentException
     *             if current <code>ExecQueue</code> is not <code>null</code>
     */
    protected void setExecQueue(ExecQueue q)
    {
        if (this.q != null) throw new IllegalArgumentException("ExecQueue already assigned");
        this.q = q;
    }

    // Description copied from CallerContext
    public void addCallbackData(CallbackData data, Object key)
    {
        // Add callback data to hashtable
        callbackData.put(key, data);

        // Log potential leak of data
        if (LEAK_THRESH_ENABLED && LEAK_THRESH_INIT != 0 && LEAK_THRESH_GROW != 0
                && callbackData.size() > leakThreshold)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Potential CallbackData leak (size=" + callbackData.size() + ") for " + this);
            }

            // Update threshold for next report
            leakThreshold += LEAK_THRESH_GROW;

            // If DEBUG is enabled, dump out all data
            if (log.isDebugEnabled())
            {
                synchronized (callbackData)
            {
                    for (Enumeration e = callbackData.keys(); e.hasMoreElements();)
            {
                        Object k = e.nextElement();
                        if (log.isDebugEnabled())
            {
                        log.debug("DataLeak? " + k + " -> " + callbackData.get(k));
            }
                }
            }
        }
    }
    }

    // Description copied from CallerContext
    public void removeCallbackData(Object key)
    {
        // Remove CallbackData from hashtable
        callbackData.remove(key);
    }

    // Description copied from CallerContext
    public CallbackData getCallbackData(Object key)
    {
        // Retrieve callback data from hashtable
        return (CallbackData) callbackData.get(key);
    }

    // Description copied from CallerContext
    public void runInContext(Runnable run) throws SecurityException, IllegalStateException
    {
        if (run == null) throw new NullPointerException("null Runnable");
        if (q == null) throw new IllegalStateException();

        q.post(run);
    }

    // Description copied from CallerContext
    public void runInContextSync(Runnable run) throws SecurityException, IllegalStateException,
            InvocationTargetException
    {
        if (run == null) throw new NullPointerException("null Runnable");
        if (q == null) throw new IllegalStateException();
        
        if (ccMgr.getCurrentContext() == this)
        {
            try
            {
                run.run();
            }
            catch (Throwable e)
            {
                throw new InvocationTargetException(e);
            }
        }
        else
        {
            SyncTask task = new SyncTask(run);
            synchronized (task)
            {
                // We post within the sync block so that the task does not run until
                // we are in wait()
                tp.post(task);
                try
                {
                    task.wait(60000);
                }
                catch (InterruptedException e)
                {
                    throw new InvocationTargetException(e);
                }
                
                // Check for any errors
                Throwable e;
                if ((e = task.getError()) != null)
                {
                    throw new InvocationTargetException(e);
                }
            }
        }
    }

    // Description copied from CallerContext
    public void runInContextAsync(Runnable run)
    {
        if (run == null) throw new NullPointerException("null Runnable");
        if (tp == null) throw new IllegalStateException();

        // Execute as appropriate
        tp.post(new LogException(run));
    }

    /**
     * Called by the <code>AppThreadGroup</code> when a code running on a
     * contained thread does not catch an exception.
     * <p>
     * This is also invoked internally if an exception is thrown by
     * {@link #runInContext(Runnable)}.
     * 
     * @param thread
     *            the Thread on which an uncaught exception occurred
     * @param throwable
     *            the instance of <code>Throwable</code> that was not caught
     * @see ThreadGroup#uncaughtException(Thread,Throwable)
     */
    public abstract void uncaughtException(Thread thread, final Throwable throwable);

    /**
     * A wrapper for <code>Runnable</code> that catches any uncaught
     * <code>Throwable</code>s and logs them via the
     * <code>SystemEventManager</code>.
     * 
     * @see AbstractCallerContext#uncaughtException(Thread, Throwable)
     * 
     * @author Aaron Kamienski
     */
    private class LogException implements Runnable
    {
        /**
         * Invokes the wrapped runnable, catching any uncaught exceptions and
         * logging via
         * {@link AbstractCallerContext#uncaughtException(Thread, Throwable)}.
         */
        public void run()
        {
            try
            {
                run.run();
            }
            catch (Throwable e)
            {
                uncaughtException(Thread.currentThread(), e);
            }
        }

        public String toString()
        {
            return run.toString();
        }

        /**
         * Creates an instance of LogException.
         * 
         * @param run
         *            the <code>Runnable</code> to wrap
         */
        LogException(Runnable run)
        {
            this.run = run;
        }

        /**
         * The wrapped <code>Runnable</code>.
         */
        private final Runnable run;
    }

    // Description copied from CallerContext
    public abstract boolean isAlive();

    // Description copied from CallerContext
    public abstract void checkAlive() throws SecurityException;

    // Description copied from CallerContext
    public abstract Object get(Object key);

    /**
     * Maps <code>Object</code> keys to <code>CallbackData</code>.
     */
    protected Hashtable callbackData = new Hashtable();

    private static final boolean LEAK_THRESH_ENABLED = true;

    private static final int LEAK_THRESH_INIT = LEAK_THRESH_ENABLED ? MPEEnv.getEnv("OCAP.cc.leakthresh.init", 30) : 0;

    private static final int LEAK_THRESH_GROW = LEAK_THRESH_ENABLED ? MPEEnv.getEnv("OCAP.cc.leakthresh.grow", 20) : 0;

    /**
     * Next threshold at which a callback data leak warning will be produced.
     */
    private int leakThreshold = LEAK_THRESH_INIT;

    /**
     * The ACC associated with this caller.
     */
    protected AccessControlContext acc = null;

    /**
     * The {@link ContextID context id} for this context. This is used during
     * resource reclamation. It defaults to zero for system contexts.
     */
    protected long contextID;

    /** The main execution queue for this context. */
    protected ExecQueue q;

    /** The ThreadPool to be used by this context. */
    protected ThreadPool tp;

    /** The CCMgr that created this caller context. */
    protected CCMgr ccMgr;
    
    /** Log4J logger. */
    private static final Logger log = Logger.getLogger(AbstractCallerContext.class);
}
