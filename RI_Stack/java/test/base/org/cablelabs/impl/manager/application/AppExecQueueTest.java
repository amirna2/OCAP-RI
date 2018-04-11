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
import junit.framework.TestSuite;

import org.dvb.application.AppID;

/**
 * @author Aaron Kamienski
 */
public class AppExecQueueTest extends ExecQueueTest
{

    /*
     * Tests createInstance(AppID, ClassLoader, ThreadGroup).
     */
    public void testCreateInstance_AppIDClassLoaderThreadGroup() throws Exception
    {
        // Get rid of execqueue created in setUp
        execqueue.dispose();

        // Test constructor
        AppID id = new AppID(27, 33);
        ThreadGroup tg = new ThreadGroup("Test");
        ClassLoader cl = new ClassLoader()
        {
            protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                // Defer
                return getClass().getClassLoader().loadClass(name);
            }
        };
        execqueue = AppExecQueue.createInstance(id, tg);

        checkAppExecQueue((AppExecQueue) execqueue, "App-" + id, tg, Thread.currentThread().getContextClassLoader());
    }

    /*
     * Tests createInstance().
     */
    public void testCreateInstance() throws Exception
    {
        // Get rid of execqueue created in setUp
        execqueue.dispose();

        // Test constructor
        execqueue = AppExecQueue.createInstance(Thread.NORM_PRIORITY);

        checkAppExecQueue((AppExecQueue) execqueue, "System-", Thread.currentThread().getThreadGroup(),
                Thread.currentThread().getContextClassLoader());
    }

    /*
     * Tests ctor.
     */
    public void testCtor() throws Exception
    {
        // Get rid of execqueue created in setUp
        execqueue.dispose();

        // Test constructor
        execqueue = new AppExecQueue(Thread.NORM_PRIORITY);

        checkAppExecQueue((AppExecQueue) execqueue, "System-", Thread.currentThread().getThreadGroup(),
                Thread.currentThread().getContextClassLoader());
    }

    /*
     * Tests ctor.
     */
    public void testCtor_AppIDClassLoaderThreadGroup() throws Exception
    {
        // Get rid of execqueue created in setUp
        execqueue.dispose();

        // Test constructor
        AppID id = new AppID(27, 33);
        ThreadGroup tg = new ThreadGroup("Test");
        ClassLoader cl = new ClassLoader()
        {
            protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException
            {
                // Defer
                return getClass().getClassLoader().loadClass(name);
            }
        };
        execqueue = new AppExecQueue(id, tg);

        checkAppExecQueue((AppExecQueue) execqueue, "App-" + id, tg, Thread.currentThread().getContextClassLoader());
    }

    protected void checkAppExecQueue(AppExecQueue aeq, String namePfx, ThreadGroup tg, ClassLoader cl) throws Exception
    {
        // Must invoke on the dispatch thread.
        final Object[] answers = new Object[3];
        synchronized (answers)
        {
            aeq.post(new Runnable()
            {
                public void run()
                {
                    synchronized (answers)
                    {
                        answers[0] = Thread.currentThread().getThreadGroup();
                        answers[1] = Thread.currentThread().getContextClassLoader();
                        answers[2] = Thread.currentThread();
                        answers.notifyAll();
                    }
                }
            });
            answers.wait(3000);
        }

        assertNotNull("Expected Runnable to execute, and ThreadGroup to be set", answers[0]);
        assertNotNull("Expected Runnable to execute, and ClassLoader to be set", answers[1]);
        assertNotNull("Expected Runnable to execute, and Thread to be set", answers[2]);
        assertSame("Should've been created with the specified ThreadGroup", tg, answers[0]);
        assertSame("Should've been created with the specified ClassLoader", cl, answers[1]);

        String name = ((Thread) answers[2]).getName();
        assertNotNull("Expected name to be set", name);
        assertTrue("Expected name (" + name + ") to start with " + namePfx, name.startsWith(namePfx));
    }

    public void testGetNext() throws Exception
    {
        // Cause thread to exit quietly
        execqueue.post(null);
        // Pause to let the thread pull it off
        Thread.sleep(200);

        super.testGetNext();
    }

    /**
     * A simple runnable object that synchronizes it's run method, and notifies
     * any waiters.
     */
    private static class Waitable implements Runnable
    {
        public Waitable()
        {
        }

        public synchronized void run()
        {
            notifyAll();
        }

        public static void block(AppExecQueue eq) throws Exception
        {
            Waitable w = new Waitable();
            synchronized (w)
            {
                eq.post(w);
                w.wait();
            }
        }
    }

    /**
     * Overrides super implementation.
     * 
     * @see org.cablelabs.impl.manager.application.ExecQueueTest#createExecQueue()
     */
    protected ExecQueue createExecQueue()
    {
        return new AppExecQueue(Thread.NORM_PRIORITY);
    }

    public AppExecQueueTest(String test)
    {
        super(test);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(AppExecQueueTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new AppExecQueueTest(tests[i]));
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
        return new TestSuite(AppExecQueueTest.class);
    }
}
