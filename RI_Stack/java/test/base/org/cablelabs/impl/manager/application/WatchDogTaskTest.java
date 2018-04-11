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

import org.cablelabs.impl.manager.application.ThreadPoolTest.BlockerTask;
import org.cablelabs.impl.manager.application.ThreadPoolTest.DummyTask;
import org.cablelabs.impl.manager.application.ThreadPoolTest.Signal;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests WatchDogTask.
 * 
 * @author Aaron Kamienski
 */
public class WatchDogTaskTest extends TestCase
{

    /**
     * Tests constructor.
     */
    public void testCtor()
    {
        Runnable run = new Runnable()
        {
            public void run()
            {
            }
        };

        try
        {
            WatchDogTask dog = new WatchDogTask(null, -1L);
            fail("Expected exception given null runnable and invalid timeout");
        }
        catch (NullPointerException e)
        {
        }
        catch (IllegalArgumentException e)
        {
        }

        try
        {
            WatchDogTask dog = new WatchDogTask(null, 3000);
            fail("Expected exception given null runnable");
        }
        catch (NullPointerException e)
        {
        }

        try
        {
            WatchDogTask dog = new WatchDogTask(run, -1L);
            fail("Expected exception given invalid timeout");
        }
        catch (IllegalArgumentException e)
        {
        }

        // Expect no errors
        WatchDogTask dog = new WatchDogTask(run, 1);
    }

    /**
     * Tests run(). Should execute and exit after execution.
     */
    public void testRun() throws Exception
    {
        // Run a simple task in a watchdog task... it should run w/out incident
        DummyTask task = new DummyTask();
        TestedWatchDogTask dog = new TestedWatchDogTask(task, 500L);

        dog.run();

        assertTrue("Expected sub-task to be executed", task.done);
        assertFalse("Expected no timeout to occur", dog.timeout);
        assertFalse("Expected no timer failure to occur", dog.failed);

        // Expect timer *not* to go off anytime soon
        dog.waitForTimeout(1000L);
        assertFalse("Timer should've been cancelled", dog.timeout);
    }

    /**
     * Tests timerFailure() returning true.
     */
    // TODO: re-enable test after fixing up doTestTimerFailure
    public void XtestTimerFailure_true()
    {
        doTestTimerFailure(true);
    }

    /**
     * Tests timerFailure() returning false.
     */
    // TODO: re-enable test after fixing up doTestTimerFailure
    public void XtestTimerFailure_false()
    {
        doTestTimerFailure(false);
    }

    private void doTestTimerFailure(boolean exitOnFailure)
    {
        DummyTask task = new DummyTask();
        TestedWatchDogTask dog = new TestedWatchDogTask(task, 1000L, exitOnFailure);

        // TODO how to cause a failure to setup timer???
        // Enable testTimerFailure() tests once we figure this out.
        dog.run();

        assertTrue("Expected timerFailure", dog.failed);
        assertEquals("Expected sub-task to execute?", exitOnFailure, task.done);
    }

    /**
     * Tests timerExpired().
     */
    public void testTimeoutExpired() throws Exception
    {
        Signal m = new Signal();
        BlockerTask task = new BlockerTask(m);
        TestedWatchDogTask dog = new TestedWatchDogTask(task, 500L);

        // Run it in a thread
        Thread t = new Thread(dog);
        t.start();

        // Wait for timeout
        dog.waitForTimeout(5000L);

        // timeout should've occurred
        assertTrue("Timeout was expected", dog.timeout);
        assertFalse("Timer failure should not have occurred", dog.failed);
        assertFalse("Task should not have completed yet", dog.done);
        assertFalse("Sub-task should not have completed yet", task.done);

        // Signal task to complete
        m.signal();

        // Wait for complete
        dog.waitForDone(3000L);

        assertTrue("Expected task to have completed", dog.done);
        assertTrue("Sub-task should have completed yet", task.done);
        assertFalse("Timer failure should not have occurred", dog.failed);

        t.join(3000);
        assertFalse("Thread should've exited by now", t.isAlive());
    }

    /**
     * Extension to WatchDogTask used during testing.
     * 
     * @author Aaron Kamienski
     */
    class TestedWatchDogTask extends WatchDogTask
    {
        public boolean done;

        public boolean failed;

        public boolean timeout;

        private boolean exitOnFailure = true;

        private Object timeoutLock = new Object();

        TestedWatchDogTask(Runnable run, long timeout)
        {
            super(run, timeout);
        }

        TestedWatchDogTask(Runnable run, long timeout, boolean exitOnFailure)
        {
            super(run, timeout);
            this.exitOnFailure = exitOnFailure;
        }

        public void run()
        {
            super.run();
            synchronized (this)
            {
                done = true;
                notifyAll();
            }
        }

        protected void timeoutExpired()
        {
            synchronized (timeoutLock)
            {
                timeout = true;
                timeoutLock.notifyAll();
            }
        }

        protected synchronized boolean timerFailure()
        {
            failed = true;
            return exitOnFailure;
        }

        synchronized void waitForDone(long ms) throws InterruptedException
        {
            if (!done) wait(ms);
        }

        void waitForTimeout(long ms) throws InterruptedException
        {
            synchronized (timeoutLock)
            {
                if (!timeout) timeoutLock.wait(ms);
            }
        }
    }

    /* ======================== Boilerplate ======================== */

    public WatchDogTaskTest(String test)
    {
        super(test);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(WatchDogTaskTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new WatchDogTaskTest(tests[i]));
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
        return new TestSuite(WatchDogTaskTest.class);
    }
}
