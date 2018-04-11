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

import java.util.Enumeration;

import org.apache.log4j.Logger;

import org.cablelabs.impl.util.SystemEventUtil;

/**
 * An extension of <code>ExecQueue</code> that reserves a
 * <code>ThreadPool</code>- managed <code>Thread</code> on-demand. The
 * <code>Thread</code> is maintained as long as this <code>ExecQueue</code> is
 * non-empty . If this queue becomes non-empty for a period exceeding the
 * timeout value specified in the constructor, then the reserved
 * <code>Thread</code> is returned to the <code>ThreadPool</code>.
 * 
 * @author Aaron Kamienski
 */
class DemandExecQueue extends ExecQueue
{
    /**
     * Constructs a <code>DemandExecQueue</code> with a simple
     * <code>WorkerTask</code>. A <i>simple</i> <code>WorkerTask</code> is
     * created to execute on an on-demand thread and handle the
     * <code>Runnable</code> tasks posted to this queue.
     * 
     * @param threadPool
     *            the <code>ThreadPool</code> from which to reserve a thread
     *            when necessary (i.e., on-demand)
     * @param timeout
     *            the amount of time that this exec queue must be empty before a
     *            reserved <code>Thread</code> is returned to the pool
     */
    DemandExecQueue(ThreadPool threadPool, long timeout)
    {
        this(threadPool, timeout, null);
        setWorkerTask(new WorkerTask(this, false));
    }

    /**
     * Constructs a <code>DemandExecQueue</code>. If <i>task</i> is
     * <code>null</code>, then it is he caller's responsibility to call
     * {@link #setWorkerTask}.
     * 
     * @param threadPool
     *            the <code>ThreadPool</code> from which to reserve a thread
     *            when necessary (i.e., on-demand)
     * @param timeout
     *            the amount of time that this exec queue must be empty before a
     *            reserved <code>Thread</code> is returned to the pool
     * @param task
     *            the <code>WorkerTask</code> or other suitable
     *            <code>Runnable</code> that will handle the
     *            <code>Runnable</code> tasks posted to this queue
     */
    DemandExecQueue(ThreadPool threadPool, long timeout, Runnable task)
    {
        this.threadPool = threadPool;
        this.timeout = timeout;
        if (task != null) setWorkerTask(task);
    }

    /**
     * Sets the given <code>Runnable</code> (that is expected to function as a
     * <code>WorkerTask</code> -- pulling items off of this queue for execution)
     * as <i>the</i> worker task. The given <code>Runnable</code> is actually
     * wrapped with an instance of {@link DemandTask} that handles the task of
     * logging whether this class has a thread associated with it or not.
     * 
     * @param task
     *            worker task
     */
    final void setWorkerTask(Runnable task)
    {
        this.task = new DemandTask(task);
    }

    /**
     * Overrides {@link ExecQueue#waitOnQueue()} to only wait for the timeout
     * period specified in the constructor before returning.
     * 
     * @return <code>true</code> indicating that <code>getNext()</code> should
     *         return <code>null</code> if the queue is empty
     * 
     * @throws InterruptedException
     */
    protected synchronized boolean waitOnQueue() throws InterruptedException
    {
        wait(timeout);
        return true;
    }

    /**
     * Overrides {@link ExecQueue#post(java.lang.Runnable)}, ensuring that a
     * thread is available to handle the posted <code>Runnable</code>.
     */
    public synchronized void post(Runnable exec)
    {
        super.post(exec);

        // If no thread is currently associated with this queue, then request
        // one
        ensureThread();
    }

    /**
     * Ensures that this queue has a thread currently associated with it. If
     * none is currently associated, a request is posted to the associated
     * <code>ThreadPool</code>. The request is actually the associated
     * {@link #task WorkerTask}.
     */
    private synchronized void ensureThread()
    {
        if (thread == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("ensureThread() called for " + this);
            }

            if (!disposed)
                threadPool.post(task);
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Queue disposed with " + q.size() + " items outstanding");
                }

                if (log.isDebugEnabled())
                {
                    StringBuffer sb = new StringBuffer();
                    String sep = System.getProperty("line.separator") + "\t";
                    for (Enumeration e = q.elements(); e.hasMoreElements();)
                {
                        sb.append(sep).append(e.nextElement());
                }
                    if (log.isDebugEnabled())
                    {
                        log.debug("Outstanding items: " + sb);
                    }
            }
        }
    }
    }

    /**
     * Invoked by the <code>WorkerTask</code> thread before it begins executing
     * <code>Runnable</code>s pulled from this queue. This is called to inform
     * this queue of its new worker thread.
     * <p>
     * This method returns <code>true</code> if the operation was successful.
     * This method will fail with <code>false</code> if there is already a
     * thread set. If <code>false</code> is returned, then the
     * <code>WorkerTask</code> should exit immediately.
     * 
     * @return <code>true</code> if succesful; <code>false</code> if there is
     *         already a worker thread associated with this queue
     */
    protected synchronized boolean setThread()
    {
        if (thread != null) return false;
        thread = Thread.currentThread();
        return true;
    }

    /**
     * Clears the worker thread associated with this queue. This should be
     * invoked by the associated <code>WorkerTask</code> when it is exiting.
     */
    protected synchronized void clearThread()
    {
        if (thread == Thread.currentThread())
        {
            thread = null;

            // Handle race condition where thread returns, immediately after
            // ensureThread()
            if (q.size() > 0) ensureThread();
        }
        else
        {
            SystemEventUtil.logRecoverableError(new Exception("clearThread() invoked from unexpected thread (was not "
                    + thread + ")"));
        }
    }

    /**
     * A <i>Decorator</i> for the <code>WorkerTask</code> that is employed by
     * this <code>DemandExecQueue</code>. And instance of this class decorates
     * another <code>Runnable</code> (which is expected to be a
     * <code>WorkerTask</code>, pulling <code>Runnable</code>s from this queue
     * and executing them). This class' <code>run()</code> invokes
     * <code>run()</code> on the decorated task, adding callouts to set and
     * clear the thread associated with this queue.
     * 
     * @author Aaron Kamienski
     */
    protected class DemandTask implements Runnable
    {
        /**
         * Creates a <code>DemandTask</code> that decorates the given
         * <code>Runnable</code>.
         * 
         * @param run
         *            the task to be decorated
         */
        DemandTask(Runnable run)
        {
            this.run = run;
        }

        /**
         * Invokes {@link Runnable#run} on the decorated <code>Runnable</code>.
         * Before calling <code>run()</code>, invokes
         * {@link DemandExecQueue#setThread()}. After calling <code>run()</code>
         * (from within a <code>finally</code> block),
         * {@link DemandExecQueue#clearThread()} is called.
         */
        public void run()
        {
            if (setThread())
            {
                try
                {
                    run.run();
                } finally
                {
                    clearThread();
                    if (log.isDebugEnabled())
                    {
                        log.debug("ContextDemandExecQueue finished...");
                    }
            }
        }
        }

        /** The decorated <code>Runnable</code>. */
        private Runnable run;
    }

    /**
     * The <code>WorkerTask</code> (or similar <code>Runnable</code>
     * implementation) that will be posted to the associated
     * <code>ThreadPool</code> when necessary to reserve a <code>Thread</code>.
     */
    private Runnable task;

    /**
     * If non-<code>null</code>, then this <code>DemandExecQueue</code>
     * currently has a <code>Thread</code> assigned to it.
     */
    private Thread thread;

    /**
     * The <code>ThreadPool</code> from which this <code>ExecQueue</code> will
     * reserve a thread when necessary.
     */
    private ThreadPool threadPool;

    /**
     * Maximum time that this <code>ExecQueue</code> will hold onto a
     * <code>ThreadPool</code> thread before it is released back to the pool.
     */
    protected long timeout;

    /** Log4J Logger. */
    private static final Logger log = Logger.getLogger(DemandExecQueue.class.getName());

}
