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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests ThreadPool.
 * 
 * @author Aaron Kamienski
 */
public class ThreadPoolTest extends TestCase
{
    /*
     * Tests ThreadPool().
     */
    public void testCtor() throws Exception
    {
        tp = new ThreadPool(getName());
        doTestNThreads(tp, 1);
    }

    /*
     * Tests ThreadPool(int)
     */
    public void testCtor_int() throws Exception
    {
        tp = new ThreadPool(getName() + ":1", 1, Thread.NORM_PRIORITY);
        doTestNThreads(tp, 1);
        tp.dispose();

        tp = new ThreadPool(getName() + ":2", 2, Thread.NORM_PRIORITY);
        doTestNThreads(tp, 2);
        tp.dispose();

        tp = new ThreadPool(getName() + ":10", 10, Thread.NORM_PRIORITY);
        doTestNThreads(tp, 10);
    }

    public void testCtor_priority() throws Exception
    {
        int[] priorities = { Thread.NORM_PRIORITY - 1, Thread.NORM_PRIORITY, Thread.NORM_PRIORITY + 1,
                Thread.MAX_PRIORITY };
        for (int i = 0; i < priorities.length; ++i)
        {
            tp = new ThreadPool(getName() + ":" + i, 1, priorities[i]);

            class PriorityTask extends DummyTask
            {
                int priority = -1;

                public void run()
                {
                    priority = Thread.currentThread().getPriority();
                    super.run();
                }
            }
            PriorityTask t = new PriorityTask();
            tp.post(t);
            t.waitForDone(10000);

            assertEquals("Expected threads to execute at given priority", priorities[i], t.priority);

            tp.dispose();
        }
    }

    /**
     * Used to test the number of threads in a pool.
     * 
     * @param tp
     *            the thread pool to test
     * @param nThreads
     *            the number of threads that should be present
     * @throws Exception
     */
    private void doTestNThreads(ThreadPool tp, int nThreads) throws Exception
    {
        // Should have nThreads that will be blocked
        Signal signal = new Signal();
        BlockerTask[] blockers = new BlockerTask[nThreads];
        try
        {
            // Without all threads blocked, should execute immediately
            DummyTask d;
            for (int i = 0; i < nThreads; ++i)
            {
                d = new DummyTask();
                tp.post(d);
                d.waitForDone(3000);
                assertTrue("No threads should be blocked, thus allowing new tasks " + "[" + i + "/" + nThreads + "]",
                        d.done);

                tp.post(blockers[i] = new BlockerTask(signal));
                blockers[i].waitForStarted(3000);
                assertTrue("Internal error - blocker not started " + i, blockers[i].started);
            }

            // Now, with all threads blocked, should not execute immediately
            d = new DummyTask();
            tp.post(d);
            d.waitForDone(250);
            assertFalse("All threads should be blocked [" + nThreads + "]", d.done);

            // Release blocker(s), other event should execute immediately.
            signal.signal(nThreads);
            d.waitForDone(nThreads * 1500);
            assertTrue("Threads should be unblocked, allowing new tasks", d.done);
        }
        finally
        {
            // Signal nThreads just in case
            signal.signal(nThreads);
            for (int i = 0; i < blockers.length; ++i)
            {
                if (blockers[i] != null)
                {
                    blockers[i].waitForDone(3000);
                    assertTrue("Internal error - blocker task should complete " + i, blockers[i].done);
                }
            }
        }
    }

    /**
     * Tests post().
     */
    public void testPost() throws Exception
    {
        tp = new ThreadPool(getName());

        DummyTask tasks[] = new DummyTask[30];
        for (int i = 0; i < tasks.length; ++i)
            tasks[i] = new DummyTask();
        for (int i = 0; i < tasks.length; ++i)
            tp.post(tasks[i]);

        // Wait for the last one
        tasks[tasks.length - 1].waitForDone(tasks.length * 1000);
        for (int i = tasks.length; i-- > 0;)
            assertTrue("Expected all tasks to have executed " + i, tasks[i].done);
    }

    /*
     * Tests addThread().
     */
    public void testAddThread() throws Exception
    {
        tp = new ThreadPool(getName());
        for (int i = 0; i < 10; ++i)
        {
            doTestNThreads(tp, i + 1);

            tp.addThread();
        }
    }

    /**
     * Tests addThread() -- adding a thread should handle a task immediately.
     */
    public void testAddThread_immediate() throws Exception
    {
        tp = new ThreadPool(getName());

        // Post a blocker task
        Signal signal = new Signal();
        try
        {
            BlockerTask b = new BlockerTask(signal);
            tp.post(b);

            // Post a new task, which should not execute
            DummyTask task = new DummyTask();
            tp.post(task);
            task.waitForDone(300);
            assertFalse("All threads should be tied up now", task.done);

            // Add a new thread to the pool
            tp.addThread();
            task.waitForDone(3000);
            assertTrue("New thread should immediately service blocked task", task.done);
        }
        finally
        {
            signal.signal();
        }
    }

    /*
     * Tests addThread(int).
     */
    public void testAddThread_int() throws Exception
    {
        tp = new ThreadPool(getName());
        int nThreads = 1;
        for (int i = 1; i < 5; ++i)
        {
            doTestNThreads(tp, nThreads);

            tp.addThread(i);
            nThreads += i;
        }
    }

    /**
     * Tests addThread(int) -- adding a thread should handle a task immediately.
     */
    public void testAddThread_int_immediate() throws Exception
    {
        tp = new ThreadPool(getName());

        // Post a blocker task
        Signal signal = new Signal();
        int nBlockers = 5;
        try
        {
            for (int i = 0; i < nBlockers; ++i)
            {
                BlockerTask b = new BlockerTask(signal);
                tp.post(b);
            }

            // Post a new task, which should not execute
            DummyTask task = new DummyTask();
            tp.post(task);
            task.waitForDone(300);
            assertFalse("All threads should be tied up now", task.done);

            // Add new thread(s) to the pool
            tp.addThread(nBlockers);
            task.waitForDone(3000);
            assertTrue("New thread should immediately service blocked task", task.done);
        }
        finally
        {
            signal.signal(nBlockers);
        }
    }

    /*
     * Tests removeThread().
     */
    public void testRemoveThread() throws Exception
    {
        tp = new ThreadPool(getName(), 10, Thread.NORM_PRIORITY);
        for (int i = 10; i > 0; --i)
        {
            doTestNThreads(tp, i);

            tp.removeThread();
        }
    }

    /*
     * Tests removeThread(int).
     */
    public void testRemoveThread_int() throws Exception
    {
        int nThreads = 0;
        for (int i = 5; i > 0; --i)
            nThreads += i;
        tp = new ThreadPool(getName(), nThreads, Thread.NORM_PRIORITY);
        for (int i = 5; i > 0; --i)
        {
            doTestNThreads(tp, nThreads);

            tp.removeThread(i);
            nThreads -= i;
        }
    }

    /**
     * Tests addThread(-n).
     */
    public void testAddThread_int_minus() throws Exception
    {
        int nThreads = 0;
        for (int i = 5; i > 0; --i)
            nThreads += i;
        tp = new ThreadPool(getName(), nThreads, Thread.NORM_PRIORITY);
        for (int i = 5; i > 0; --i)
        {
            doTestNThreads(tp, nThreads);

            // basically same as testRemoveThread_int() except for following
            // line...
            tp.addThread(-i);
            nThreads -= i;
        }
    }

    /**
     * Tests removeThread(-n).
     */
    public void testRemoveThread_int_minus() throws Exception
    {
        tp = new ThreadPool(getName());
        int nThreads = 1;
        for (int i = 1; i < 5; ++i)
        {
            doTestNThreads(tp, nThreads);

            // basically same as testAddThread_int() except for following
            // line...
            tp.removeThread(-i);
            nThreads += i;
        }
    }

    /**
     * Tests dispose().
     */
    public void testDispose() throws Exception
    {
        int nThreads = 2;
        tp = new ThreadPool(getName(), nThreads, Thread.NORM_PRIORITY);

        // Post blockers so we can get the threads
        Signal signal = new Signal();
        BlockerTask bs[] = new BlockerTask[nThreads];
        for (int i = 0; i < bs.length; ++i)
        {
            bs[i] = new BlockerTask(signal);
            tp.post(bs[i]);
            // Wait for tasks to start... (so we don't signal too soon)
            bs[i].waitForStarted(3000);
            assertTrue("Internal error - blocker task did not start " + i, bs[i].started);
        }

        // Tell them to finish
        signal.signal(nThreads);
        for (int i = 0; i < bs.length; ++i)
        {
            bs[i].waitForDone(3000);
            assertTrue("Internal error - blocker task did not finish " + i, bs[i].done);
            assertNotNull("Internal error - thread not set " + i, bs[i].t);

            // Expect thread to still be runnning
            assertTrue("Expected thread to still be running", bs[i].t.isAlive());
        }

        // Now get on with the real test...

        // Post a blocker
        BlockerTask b = new BlockerTask(signal);
        tp.post(b);

        // Dispose the queue
        tp.dispose();

        // Release the blocking task
        signal.signal();

        // Wait for threads
        for (int i = 0; i < bs.length; ++i)
        {
            bs[i].t.join(3000);
            assertFalse("Expected thread to exit", bs[i].t.isAlive());
        }
    }

    /**
     * Implements a simple method of signalling multiple listeners.
     * 
     * @author Aaron Kamienski
     */
    static class Signal
    {
        private int signaled = 0;

        synchronized void listen() throws InterruptedException
        {
            try
            {
                while (signaled <= 0)
                    wait();
                --signaled;
            }
            catch (InterruptedException e)
            {
                // Notify somebody else
                notify();
                throw e;
            }
        }

        synchronized void signal()
        {
            ++signaled;
            notify();
        }

        void signal(int n)
        {
            for (int i = 0; i < n; ++i)
            {
                signal();
            }
        }
    }

    /**
     * A simple Runnable task that notify's anybody waiting on it when it
     * finishes. Also records the thread used for execution.
     * 
     * @author Aaron Kamienski
     */
    static class DummyTask implements Runnable
    {
        boolean done = false;

        Thread t;

        public synchronized void run()
        {
            done = true;
            t = Thread.currentThread();
            notifyAll();
        }

        public synchronized void waitForDone(long ms) throws InterruptedException
        {
            if (!done) wait(ms);
        }
    }

    /**
     * A simple Runnable task that blocks waiting for a Signal to be signalled.
     * 
     * @author Aaron Kamienski
     */
    static class BlockerTask extends DummyTask
    {
        Signal m;

        boolean started = false;

        BlockerTask(Signal m)
        {
            this.m = m;
        }

        public void run()
        {
            synchronized (this)
            {
                started = true;
                notifyAll();
            }
            while (!done)
            {
                try
                {
                    m.listen();
                    super.run();
                }
                catch (InterruptedException e)
                {
                }
            }
        }

        public synchronized void waitForStarted(long ms) throws InterruptedException
        {
            if (!started) wait(ms);
        }
    }

    /* ==================== Boilerplate ========================== */

    protected ThreadPool tp;

    protected void tearDown() throws Exception
    {
        if (tp != null) tp.dispose();
        super.tearDown();
    }

    public ThreadPoolTest(String test)
    {
        super(test);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(ThreadPoolTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new ThreadPoolTest(tests[i]));
            return suite;
        }
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        return new TestSuite(ThreadPoolTest.class);
    }
}
