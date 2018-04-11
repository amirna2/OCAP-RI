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

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.util.MPEEnv;
import org.ocap.system.event.ErrorEvent;

/**
 * A <code>ThreadPool</code> represents a pool of <code>WorkerTask</code>
 * threads. The threads that belong to the <code>ThreadPool</code> continuously
 * monitor the <code>ThreadPool</code>'s <code>ExecQueue</code> for new tasks to
 * execute.
 * 
 * @author Aaron Kamienski
 */
class ThreadPool
{
    /**
     * Creates an instance of ThreadPool with the given name and initial size.
     * 
     * @param name
     *            the name of this thread pool.  Will be used to name member threads
     * @param group 
     *            the <code>ThreadGroup</code> to which all member threads will be
     *            assigned
     * @param size
     *            the initial and minimum thread pool size
     * @param priority
     *            default execution priority for thread pool threads
     * 
     * @throws IllegalArgumentException
     *             given an invalid size
     */
    public ThreadPool(String name, ThreadGroup group, int size, int priority)
    {
        this.group = group;
        this.name = name;
        this.priority = priority;
        this.minThreadCount = size;

        if (size < 0)
            throw new IllegalArgumentException("Invalid size = " + size);
        
        adjustThreadCount(size);
        
        threadPools.add(this);
    }

    /**
     * Forwards all <code>Runnable</code> posts to this <code>ThreadPool</code>s
     * <code>ExecQueue</code> for further execution.
     * 
     * @param runnable
     *            the <code>Runnable</code> to execute
     */
    public void post(Runnable runnable)
    {
        queue.post(runnable);
    }

    /**
     * Adds threads to or removes threads from the <code>ThreadPool</code>. If
     * <i>n</i> is positive threads are added.  If <i>n</i> is negative threads
     * are removed.  The thread pool will never contain fewer than the minimum
     * number of threads as specified in the constructor
     * 
     * @param n
     *            the number of threads to add or remove
     */
    private void adjustThreadCount(int n)
    {
        if (n == 0)
            return;

        if (log.isInfoEnabled())
        {
            log.info("ThreadPool (" + name + "): adjustThreadCount(" + n + ')');
        }
        
        if (n < 0)
        {
            for (int count = 0; count < -n; ++count)
            {
                // Post a runnable that will cause a thread to exit...
                post(new Runnable()
                {
                    public void run()
                    {
                        throw new KillThread();
                    }
                });
            }
        }
        else
        {
            for (int count = 0; count < n; ++count)
            {
                final String newThreadName = "ThreadPool-" + name + "-" + nextThread();
                
                if (log.isDebugEnabled())
                {
                    log.debug( "Adding thread " + newThreadName + 
                               " to pool (" + name + "), count: " + count );
                }
                WorkerTask task = new WorkerTask(queue)
                {
                    public void run()
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Thread entered pool (" + name + "): " + Thread.currentThread());
                        }
    
                        // Exec from queue
                        super.run();
    
                        if (log.isDebugEnabled())
                        {
                            log.debug("Thread exiting pool (" + name + "): " + Thread.currentThread());
                        }
                    }
    
                    protected boolean handleThrowable(Throwable e)
                    {
                        // If expected, then exit;
                        // Else just log it and continue execution.
                        boolean expected = e instanceof KillThread;
                        if (!expected)
                            AppManager.logErrorEvent(ErrorEvent.SYS_CAT_JAVA_THROWABLE, e);
                        return expected;
                    }
                };
                Thread t = new Thread(group, task, newThreadName);
                t.setPriority(priority);
                t.start();
            }
        }
        
        threadCount += n;
    }

    /**
     * Shutdown this <code>ThreadPool</code> by posting <i>quit</i> messages for
     * each outstanding thread to the internal queue. Any threads that are busy
     * handling other tasks should exit as soon as those tasks are completed.
     */
    public void dispose()
    {
        queue.dispose();
        threadPools.remove(this);
    }
    
    /**
     * Instructs this <code>ThreadPool</code> to gather information about thread
     * usage.  If the <i>adjust</i> flag is set to true, the pool may potentially
     * modify the number of threads in the thread pool based on thread usage
     * 
     * @param adjust if false, just update sampling. If true, add/remove 
     *               threads if needed.
     */
    public synchronized void checkThreads(boolean adjust)
    {
        final int tasksWithoutThreads = queue.numOutstandingTasks();
        // Note: This will be negative when there are more threads 
        //       than tasks...

        numSamples++;
//        if (Logging.LOGGING) // If only we had log.trace()...
//        {
//            log.debug( "ThreadPool (" + name + "): monitorThreads: Sample " + numSamples 
//                       + ": tasksWithoutThreads: " + tasksWithoutThreads );
//        }

        if (tasksWithoutThreads < this.minTasksWithoutThreads)
        {
            this.minTasksWithoutThreads = tasksWithoutThreads;
        }
        
        if (adjust)
        {
            int adjustmentFactor = minTasksWithoutThreads;
            
            if ((threadDumpThreshold > 0) && (adjustmentFactor >= threadDumpThreshold))
            {
                dumpBacktraces();
            }
            
            if (this.threadCount + adjustmentFactor < minThreadCount)
            { // Don't adjust below the min
                adjustThreadCount(minThreadCount - threadCount);
            }
            else
            {
                adjustThreadCount(adjustmentFactor);
            }
            
            // Reset our sampling variables
            minTasksWithoutThreads = Integer.MAX_VALUE;
            numSamples = 0;
        }
    }

    /** Increments and returns {@link #threadCounter}. */
    private synchronized int nextThread()
    {
        return ++threadCounter;
    }

    /**
     * A private class used to signal that a thread should exit.
     * 
     * @author Aaron Kamienski
     */
    private class KillThread extends ThreadDeath
    {
    }
    
    /**
     * Log the backtraces of all outstanding tasks.
     */
    void dumpBacktraces()
    {
        if (log.isInfoEnabled())
        {
            log.info("dumpBacktraces: Enter");
        }
        
        if (threadDumperPort > 0)
        {
            // Connect to the backttrace dumper, if configured to do so
            try
            {
                java.net.Socket dumperSocket = new java.net.Socket( InetAddress.getLocalHost(),
                                                                    threadDumperPort );
                dumperSocket.close();
                // Hold off growing the pool for just a moment to let the dumper do its job...
                Thread.sleep(200);
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info( "dumpBacktraces: Exception connecting to localhost port " 
                              + threadDumperPort, e );
                }
            }
        }
        
        if (log.isInfoEnabled())
        {
            log.info("dumpBacktraces: Exit");
        }
    } // END dumpOutstandingTaskBacktraces()

    /**
     * The <code>ExecQueue</code> from which component threads will pull tasks
     * for execution.
     */
    private ExecQueue queue = new ExecQueue();

    /**
     * The <code>ThreadGroup</code> to which all threads managed by this
     * <code>ThreadPool</code> belong.
     */
    private ThreadGroup group;

    /** Counts the number of threads that have been created. */
    private int threadCounter;
    
    /** The current number of threads in the pool */
    private int threadCount = 0;
    
    /** The minimum number of threads that this thread pool must maintain */
    private int minThreadCount;
    
    /** Default thread priority. */
    private int priority;
    
    /** Thread pool name */
    private String name;
    
    /**
     * Value used to accumulate thread usage data so that we can adjust the
     * thread count based on recent usage
     */
    private int minTasksWithoutThreads;
    private int numSamples = 0;

    /** 
     * Perform a diagnostic thread dump when the thread threadDumperPort is set and count is 
     * adjusted beyond this threshold.
     */
    private static int threadDumpThreshold = 3;
    
    /**
     * The port that the thread dump routine will connect to (on the localhost) to trigger
     * a thread dump.
     */
    private static int threadDumperPort = -1;
    
    private static Vector threadPools = new Vector();

    public static void startMonitoring( final int samplePeriod, 
                                        final int adjustmentFrequency )
    {
        try
        {
            threadDumpThreshold = Integer.parseInt(MPEEnv.getEnv("OCAP.sys.tp.threadDumpGrowThreshold"));
        }
        catch (Exception e)
        {
            threadDumpThreshold = -1;
        }
        try
        {
            threadDumperPort = Integer.parseInt(MPEEnv.getEnv("OCAP.sys.tp.dumpSignalPort"));
        }
        catch (Exception e)
        {
            threadDumperPort = -1;
        }

        if (threadDumperPort > 0)
        {
            if (log.isInfoEnabled())
            {
                log.info( "startMonitoring: Thread dumping enabled (port " + threadDumperPort
                          + ", threshold " + threadDumpThreshold + ')' );
            }
        }

        final Thread threadPoolMonitor = new Thread(new Runnable()
        {
            public void run()
            {
                int sample = 0;
                
                if (log.isDebugEnabled())
                {
                    log.debug("Thread pool monitor started.");
                }
                while (true)
                {
                    boolean performAdjust = false;
                    if (++sample == adjustmentFrequency)
                    {
                        performAdjust = true;
                        sample = 0;
                    }
                    
                    synchronized (threadPools)
                    {
                        for (Iterator i = threadPools.iterator(); i.hasNext();)
                        {
                            ThreadPool tp = (ThreadPool)i.next();
                            tp.checkThreads(performAdjust);
                        }
                    }
                    
                    try
                    {
                        Thread.sleep(samplePeriod);
                    }
                    catch (InterruptedException e)
                    {
                }
            }
            }
        }, "ThreadPoolMonitor");
        
        threadPoolMonitor.start();
    }

    /** Log4J Logger. */
    private static final Logger log = Logger.getLogger(ThreadPool.class);
}
