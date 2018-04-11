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
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests WorkerTask.
 * 
 * @author Aaron Kamienski
 */
public class WorkerTaskTest extends TestCase
{
    /**
     * Tests constructor.
     */
    public void testCtor()
    {
        try
        {
            new WorkerTask(null);
            fail("Expect NullPointerException for null ExecQueue");
        }
        catch (NullPointerException e)
        {
        }

        // Expect no errors for following
        eq = new ExecQueue();
        WorkerTask wt = new WorkerTask(eq);
    }

    /**
     * Tests run(). Should pull entries from queue and execute them in order.
     */
    public void testRun() throws Exception
    {
        eq = new ExecQueue();
        WorkerTask wt = new WorkerTask(eq);

        // Set up a bunch of runnables to execute
        final Vector ran = new Vector();
        class TestRun implements Runnable
        {
            boolean done;

            public synchronized void run()
            {
                ran.addElement(this);
                done = true;
                notifyAll();
            }

            synchronized void waitForDone(long ms) throws InterruptedException
            {
                if (!done) wait(ms);
            }
        }
        TestRun runs[] = new TestRun[10];
        for (int i = 0; i < runs.length; ++i)
            runs[i] = new TestRun();

        // Post to event queue
        for (int i = 0; i < runs.length; ++i)
            eq.post(runs[i]);

        // Create a thread to run the worker
        Thread t = new Thread(wt);
        t.start();

        // Wait for last one to execute
        runs[runs.length - 1].waitForDone(3000);

        assertEquals("Unexpected number of runs", runs.length, ran.size());
        for (int i = 0; i < runs.length; ++i)
            assertSame("Unexpected order of runs", runs[i], ran.elementAt(i));

        // Wait for thread to end -- it shouldn't
        t.join(100);
        assertTrue("Expect thread to still be alive", t.isAlive());
    }

    /**
     * Tests that run() exits given getNext() returning null.
     */
    public void testRun_exit() throws InterruptedException
    {
        eq = new ExecQueue();
        WorkerTask wt = new WorkerTask(eq);

        // Post null to exit
        eq.post(null);

        // Create a thred to run the worker
        Thread t = new Thread(wt);
        t.start();

        // Wait for thread to finish
        t.join(3000);

        assertFalse("Expected thread to exit given null post", t.isAlive());
    }

    /**
     * Tests that handleThrowable() is called given an exception in run.run().
     */
    public void testHandleThrowable_exit() throws Exception
    {
        // Create WorkerTask that overrides handleThrowable()
        eq = new ExecQueue();
        final Throwable[] actual = { null };
        WorkerTask wt = new WorkerTask(eq)
        {
            protected boolean handleThrowable(Throwable e)
            {
                actual[0] = e;
                return true; // thread should exit
            }
        };

        // Post Runnable that throws Throwable
        final Throwable[] expected = { null };
        eq.post(new Runnable()
        {
            public void run()
            {
                RuntimeException e = new RuntimeException();
                expected[0] = e;
                throw e;
            }
        });

        // Create a thred to run the worker
        Thread t = new Thread(wt);
        t.start();

        // Wait for thread to finish
        t.join(3000);
        assertFalse("Expected thread to exit on handleThrowable", t.isAlive());

        assertNotNull("Internal error - exception not thrown", expected[0]);

        assertNotNull("Expected throwable to be thrown", actual[0]);
        assertEquals("Unexpected throwable passed to handleThrowable", expected[0], actual[0]);
    }

    /**
     * Tests handleThrowable(), returning false should not exit.
     * 
     * @throws Exception
     */
    public void testHandleThrowable_noExit() throws Exception
    {
        // Create WorkerTask that overrides handleThrowable()
        eq = new ExecQueue();
        final Throwable[] actual = { null };
        class WT extends WorkerTask
        {
            WT()
            {
                super(eq);
            }

            protected synchronized boolean handleThrowable(Throwable e)
            {
                actual[0] = e;
                notifyAll();
                return false; // thread should NOT exit
            }

            synchronized void waitForThrown(long ms) throws InterruptedException
            {
                if (actual[0] == null) wait(ms);
            }
        }
        WT wt = new WT();

        // Post Runnable that throws Throwable
        final Throwable[] expected = { null };
        eq.post(new Runnable()
        {
            public void run()
            {
                RuntimeException e = new RuntimeException();
                expected[0] = e;
                throw e;
            }
        });

        // Create a thred to run the worker
        Thread t = new Thread(wt);
        t.start();

        // Wait for handleThrowable to execute
        wt.waitForThrown(3000);

        assertNotNull("Expected throwable to be thrown", actual[0]);
        assertEquals("Unexpected throwable thrown", expected[0], actual[0]);

        // Thread should not have exited
        t.join(500);
        assertTrue("Thread should not have exited", t.isAlive());
    }

    /* ==================== Boilerplate ========================== */

    protected ExecQueue eq;

    protected void tearDown() throws Exception
    {
        if (eq != null) eq.dispose();
        super.tearDown();
    }

    public WorkerTaskTest(String test)
    {
        super(test);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(WorkerTaskTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new WorkerTaskTest(tests[i]));
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
        return new TestSuite(WorkerTaskTest.class);
    }
}
