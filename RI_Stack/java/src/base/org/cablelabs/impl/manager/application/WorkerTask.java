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

import org.cablelabs.impl.util.MPEEnv;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 * <code>WorkerTask</code> is a <code>Runnable</code> task suitable for
 * execution by a <code>Thread</code>. The <code>WorkerTask</code> will execute
 * a loop in which it does the following:
 * <ol>
 * <li>Retrieves the next item from the queue.
 * <li>If non-null, then the next item is executed.
 * <li>If null, then the task will consider it's job finished and will return
 * </ol>
 * 
 * @author Aaron Kamienski
 */
class WorkerTask implements Runnable
{
    private static Logger log = Logger.getLogger(WorkerTask.class);

    /**
     * Creates an instance of <code>WorkerTask</code> that extracts
     * <code>Runnable</code> tasks for execution from the given
     * <code>ExecQueue</code>. Uncaught exceptions will be ignored (as if
     * {@link #WorkerTask(ExecQueue, boolean)} were called with
     * <code>false</code>.
     * 
     * @param queue
     *            the queue to retrieve <code>Runnable</code>s for execution
     */
    WorkerTask(ExecQueue queue)
    {
        this(queue, false);
    }

    /**
     * Creates an instance of <code>WorkerTask</code> that extracts
     * <code>Runnable</code> tasks for execution from the given
     * <code>ExecQueue</code>.
     * 
     * @param queue
     *            the queue to retrieve <code>Runnable</code>s for execution
     * @param exitOnException
     *            if <code>true</code> then uncaught <code>Throwable</code>s
     *            will cause the {@link #run} metho to exit; if
     *            <code>false</code> then the exception will be ignored
     */
    WorkerTask(ExecQueue queue, boolean exitOnException)
    {
        this(queue, exitOnException, false);
    }

    /**
     * Creates an instance of <code>WorkerTask</code> that extracts
     * <code>Runnable</code> tasks for execution from the given
     * <code>ExecQueue</code>.
     * 
     * @param queue
     *            the queue to retrieve <code>Runnable</code>s for execution
     * @param exitOnException
     *            if <code>true</code> then uncaught <code>Throwable</code>s
     *            will cause the {@link #run} metho to exit; if
     *            <code>false</code> then the exception will be ignored
     * @param useWatchDog
     *            if <code>true</code> then a <code>WatchDog</code> will be used
     *            to watch for and log long-running tasks
     */
    WorkerTask(ExecQueue queue, boolean exitOnException, boolean useWatchDog)
    {
        if (queue == null) throw new NullPointerException("queue is null");
        this.queue = queue;
        this.exitOnException = exitOnException;
        this.watchdog = WatchDog.createInstance(useWatchDog);
    }

    /**
     * Retrieves <code>Runnable</code> objects from the associated
     * <code>ExecQueue</code> and executes them. When {@link ExecQueue#getNext}
     * returns <code>null</code> then the thread will exit.
     */
    public void run()
    {
        Runnable runnable;

        // Start watchdog
        watchdog.start();

        while ((runnable = queue.getNext()) != null)
        {
            // Log the runnable that we are going to run
            watchdog.current = runnable;

            try
            {
                runnable.run();
            }
            catch (Throwable e)
            {
                if (handleThrowable(e))
                {
                    break;
                }
                else
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Throwable caught in WorkerThread run", e);
                    }
                }
            }

            // Forget the runnable that we ran
            watchdog.current = null;
        }

        // Disable the WatchDog
        watchdog.stop();
    }

    /**
     * Called by {@link #run} if a <code>Throwable</code> is thrown by a call to
     * a given {@link Runnable#run}. The return value indicates whether the main
     * loop should exit or not (the main loop will exit if <code>true</code> is
     * returned). This implementation always returns the value of
     * <i>exitOnException</i> given to the
     * {@link #WorkerTask(ExecQueue, boolean)} constructor.
     * <p>
     * Extensions of this class may override the default behavior as necessary.
     * 
     * @param e
     *            the <code>Throwable</code> that was thrown
     * @return <code>true</code> to indicate that the main loop should exit
     */
    protected boolean handleThrowable(Throwable e)
    {
        return exitOnException;
    }

    /**
     * The queue to retrieve <code>Runnable</code>s from for execution
     */
    private ExecQueue queue;

    /**
     * If <code>true</code> then uncaught exceptions will cause {@link #run} to
     * exit. If <code>false</code>, then uncaught exceptions will be ignored.
     */
    private boolean exitOnException;

    /**
     * The <i>watchdog</i> used to watch for <i>dead</i> or <i>stuck</i> tasks.
     * 
     * @see WatchDog
     */
    private WatchDog watchdog;
}

/**
 * The base class for <i>watchdog</i> support. This implementation doesn't
 * actually do anything (i.e., the {@link #start} and {@link #stop} methods are
 * empty).
 * <p>
 * The {@link #createInstance} method returns a new instance of
 * <code>WatchDog</code> that can be used in the following manner:
 * <ul>
 * <li>Invoke {@link #start}
 * <li>For each <code>Runnable</code> that will be executed, set
 * {@link #current} before execution.
 * <li>Following execution of the <code>Runnable</code>, clear {@link #current}
 * to <code>null</code>.
 * <li>When watchdog is no longer needed, invoke {@link #stop}
 * </ul>
 * 
 * @author Aaron Kamienski
 * 
 * @see TimerWatchDog
 */
class WatchDog
{
    volatile Runnable current;

    WatchDog()
    { /* EMPTY */
    }

    /**
     * Start the watchdog process. This implementation does nothing.
     */
    public void start()
    { /* EMPTY */
    }

    /**
     * Stop the watchdog process. This implementation does nothing.
     */
    public void stop()
    { /* EMPTY */
    }

    /**
     * The period in milliseconds that a watchdog should test for <i>dead</i>
     * tasks. If less than or equal to zero, then watchdog support is disabled.
     */
    public static final long WATCHDOG_PERIOD = MPEEnv.getEnv("OCAP.watchdog", 0L);

    /**
     * Returns an instance of <code>WatchDog</code> based upon the value of
     * {@link #WATCHDOG_PERIOD} and the <i>enable</i> parameter.
     * <p>
     * If <code>WATCHDOG_PERIOD > 0</code> and <code>enable==true</code> then an
     * instance of {@link TimerWatchDog} is returned; else a placeholder
     * instance of <code>WatchDog</code> is returned.
     * 
     * @param enable
     *            <code>true</code> indicates that a real timer watchdog is
     *            desired
     * @return an instance of <code>WatchDog</code>
     */
    public static WatchDog createInstance(boolean enable)
    {
        return (WATCHDOG_PERIOD > 0 && enable) ? new TimerWatchDog() : new WatchDog();
    }
}

/**
 * An implementation of <code>WatchDog</code> that uses a timer to test for a
 * <i>dead</i> task every {@link WatchDog#WATCHDOG_PERIOD} milliseconds.
 * 
 * @author Aaron Kamienski
 */
class TimerWatchDog extends WatchDog
{
    private static Timer timer = new Timer(); // count on this being created in
                                              // system context...

    private Runnable last;

    private TimerTask task;

    TimerWatchDog()
    {
        // Empty
    }

    /**
     * Starts the timer with a period of {@link WatchDog#WATCHDOG_PERIOD}
     * milliseconds. When the timer goes off {@link #current} will be compared
     * to the previous value -- if they are the same (and not null) then a
     * potential <i>dead task</i> has been discovered. This information is
     * logged.
     */
    public void start()
    {
        final Thread currThread = Thread.currentThread();
        task = new TimerTask()
        {
            public void run()
            {
                Runnable r = current;
                if (r != null && r == last)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Possible dead task? [" + currThread + "]:" + last);
                    }
                }
                last = r;
            }
        };
        timer.schedule(task, 500L, WATCHDOG_PERIOD);
    }

    /**
     * Stops the timer.
     */
    public void stop()
    {
        task.cancel();
    }

    private static final Logger log = Logger.getLogger("WATCHDOG");
}
