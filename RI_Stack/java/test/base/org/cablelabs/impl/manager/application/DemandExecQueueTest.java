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

import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests DemandExecQueue.
 * 
 * @author Aaron Kamienski
 */
public class DemandExecQueueTest extends ExecQueueTest
{

    public void testPost()
    {
        super.testPost();
    }

    /**
     * Removes thread from ThreadPool before invoking super.testDispose().
     */
    public void testDispose()
    {
        // Convince ThreadPool not to satisfy requests
        //tp.removeThread();

        super.testDispose();
    }

    /**
     * This test doesn't apply here. Ignore it.
     */
    public void testGetNext_blocking()
    {
        // Does nothing
    }

    /**
     * Removes thread from ThreadPool before invoking super.testGetNext().
     */
    public void testGetNext() throws Exception
    {
        // Convince ThreadPool not to satisfy requests
        //tp.removeThread();

        super.testGetNext();
    }

    /*
     * Class under test for void DemandExecQueue(ThreadPool, long)
     */
    public void testConstructor()
    {
        // TODO: test constructor
    }

    /*
     * Class under test for void DemandExecQueue(ThreadPool, long, Runnable)
     */
    public void testContstructor_task()
    {
        // TODO: test constructor
    }

    /**
     * Verify that multiple tasks get run, and tasks get run on same thread, in
     * expected order.
     * 
     * @throws Exception
     */
    public void testPost_isrun() throws Exception
    {
        final int[] order = { 0 };
        class Task implements Runnable
        {
            public Thread thread;

            public int myOrder;

            public synchronized void run()
            {
                thread = Thread.currentThread();
                synchronized (Task.class)
                {
                    myOrder = order[0]++;
                }
                notifyAll();
            }
        }

        Vector tasks = new Vector();
        for (int i = 0; i < 10; ++i)
        {
            Task task = new Task();
            tasks.addElement(task);
        }
        Task last = (Task) tasks.elementAt(tasks.size() - 1);
        synchronized (last)
        {
            for (int i = 0; i < tasks.size(); ++i)
            {
                Task task = (Task) tasks.elementAt(i);
                execqueue.post(task);
            }
            last.wait(5000);
        }

        assertNotNull("Expected last Task to be executed", last.thread);
        assertEquals("Expected last Task to be executed last", tasks.size() - 1, last.myOrder);

        for (int i = 0; i < tasks.size() - 1; ++i)
        {
            Task task = (Task) tasks.elementAt(i);

            assertNotNull("Expected task to be executed", task.thread);
            assertSame("Expected task to be executed on same thread", last.thread, task.thread);
            assertEquals("Expected task to be executed in order", i, task.myOrder);
        }
    }

    /**
     * Tests that the worker thread is released after the timeout period. Uses a
     * custom worker that records run-enter/run-exit.
     * 
     * @throws Exception
     */
    public void testTimeout() throws Exception
    {
        // Use custom WorkerTask that records run-enter/run-exit
        class Worker extends WorkerTask
        {
            public boolean started, exited;

            Worker()
            {
                super(execqueue);
            }

            public void run()
            {
                synchronized (this)
                {
                    started = true;
                    notifyAll();
                }
                super.run();
                synchronized (this)
                {
                    exited = true;
                    notifyAll();
                }
            }
        }
        class Task implements Runnable
        {
            boolean executed = true;

            public synchronized void run()
            {
                executed = true;
                notifyAll();
            }
        }

        Worker worker = new Worker();
        synchronized (worker)
        {
            ((DemandExecQueue) execqueue).setWorkerTask(worker);

            Task task = new Task();
            synchronized (task)
            {
                execqueue.post(task);

                worker.wait(5000);
                assertTrue("Expected worker to have started", worker.started);
                assertFalse("Expected worker to not have exited", worker.exited);

                task.wait(5000);
            }
            assertTrue("Expected task to be executed", task.executed);
            assertFalse("Expected worker to not have exited yet", worker.exited);

            // Wait for timeout to expire
            worker.wait(3000);
            assertTrue("Expected worker to have exited after timeout", worker.exited);
        }
    }

    /**
     * No matter the posting order, expect a queue to be emptied before another
     * queue is considered.
     * 
     * @throws Exception
     */
    public void testExclusiveAccess() throws Exception
    {
        final Vector executed = new Vector();
        class Task implements Runnable
        {
            Task(int which)
            {
                this.which = which;
            }

            public int which;

            boolean ran;

            public synchronized void run()
            {
                executed.addElement(this);
                ran = true;
                notifyAll();
            }
        }

        // create two queues
        DemandExecQueue q1 = (DemandExecQueue) execqueue;
        DemandExecQueue q2 = (DemandExecQueue) createExecQueue();
        try
        {
            Task task1 = null, task2 = null;
            Vector t1 = new Vector();
            Vector t2 = new Vector();

            // Post 4 to each
            final int N = 4;
            for (int i = 0; i < N; ++i)
            {
                task1 = new Task(10 + i);
                task2 = new Task(20 + i);

                q1.post(task1);
                q2.post(task2);
            }

            // Expect all 8 to execute, but in order of 10-13, 20-23
            // Wait for timeout
            synchronized (task2)
            {
                if (!task2.ran) task2.wait(10000);
                assertEquals("Expected all tasks to be executed by now", N * 2, executed.size());

                for (int i = 0; i < N; ++i)
                {
                    Task t = (Task) executed.elementAt(i);
                    assertEquals("Unexpected order of execution", 10 + i, t.which);
                }
            }
        }
        finally
        {
            q2.dispose();
        }
    }

    /**
     * Tests proper operation when two "ensure" threads are dispatched. We cause
     * two "ensure" threads by making two posts that should both require a
     * ensure operation that cannot be handled immediately by the ThreadPool.
     * 
     * @throws Exception
     */
    public void testRaceCondition_twoThreads() throws Exception
    {
        // ThreadPool starts out with no threads (until we want it to run)
        //tp.removeThread();

        ((DemandExecQueue) execqueue).setWorkerTask(new WorkerTask(execqueue));

        // two posted Runnables
        class Task implements Runnable
        {
            public Thread thread;

            public synchronized void run()
            {
                thread = Thread.currentThread();
                notifyAll();

                // Sleep so that we at least give second worker thread chance to
                // exec
                try
                {
                    Thread.sleep(500);
                }
                catch (Exception e)
                {
                }
            }
        }
        Task task1 = new Task();
        Task task2 = new Task();
        Task task3 = new Task();

        synchronized (task3)
        {
            execqueue.post(task1);
            execqueue.post(task2);

            // ThreadPool with two threads
            //tp.addThread(2);
            execqueue.post(task3);

            task3.wait(5000);
            assertNotNull("Expected task3 to execute", task1.thread);
        }
        assertSame("Expected all tasks to execute on same thread", task3.thread, task1.thread);
        assertSame("Expected all tasks to execute on same thread", task3.thread, task2.thread);
    }

    /**
     * Tests race condition where a task is posted while the worker thread is in
     * the middle of exiting, but before it clears itself. Problem could occur
     * if ensureThread() decides *not* to reserve a thread because it thinks it
     * has one, but then it ends up losing it.
     * 
     * @throws Exception
     */
    public void testRaceCondition_noThreads() throws Exception
    {
        class Task implements Runnable
        {
            boolean executed = true;

            public synchronized void run()
            {
                executed = true;
                notifyAll();
            }
        }
        Task task1 = new Task();
        final Task task2 = new Task();

        // Set up a WorkerTask that posts a runnable as it exits
        class Worker extends WorkerTask
        {
            Worker()
            {
                super(execqueue);
            }

            public void run()
            {
                super.run();
                // On way out, WorkerTask should post a Runnable
                execqueue.post(task2);
            }
        }

        // Post a runnable that ensures the thread and executes
        synchronized (task1)
        {
            execqueue.post(task1);
            task1.wait(5000);
            assertTrue("Expected task to execute", task1.executed);
        }

        // Let timeout
        synchronized (task2)
        {
            // Let timeout, and wait for task2 to execute
            task2.wait(5000);
            assertTrue("Expected task2 to execute, even though ensureThread() didn't get a new thread", task2.executed);
        }
    }

    protected ExecQueue createExecQueue()
    {
        return new DemandExecQueue(tp, 2000L);
    }

    protected ThreadPool tp;

    protected void setUp() throws Exception
    {
        //tp = new ThreadPool();
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        tp.dispose();
    }

    public DemandExecQueueTest(String test)
    {
        super(test);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(DemandExecQueueTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new DemandExecQueueTest(tests[i]));
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
        return new TestSuite(DemandExecQueueTest.class);
    }
}
