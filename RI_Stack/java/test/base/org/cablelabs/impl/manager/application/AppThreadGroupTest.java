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

import junit.framework.*;
import org.cablelabs.impl.manager.application.AppThreadGroup;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;
import org.cablelabs.test.TestUtils;

/**
 * Tests the AppThreadGroup implementation.
 * 
 * @author Aaron Kamienski
 */
public class AppThreadGroupTest extends TestCase
{
    /**
     * Tests that AppThreadGroup extends ThreadGroup.
     */
    public void testAncestry()
    {
        TestUtils.testExtends(AppThreadGroup.class, ThreadGroup.class);
    }

    /**
     * Test constructors.
     */
    public void testConstructor() throws Exception
    {
        DummyContext ctx = new DummyContext();
        AppThreadGroup tg = new AppThreadGroup(ctx);

        try
        {
            assertSame("The context used for creation should be the returned ctx", ctx, tg.getCallerContext());
        }
        finally
        {
            ctx.dispose();
        }
    }

    /**
     * Tests getCallerContext.
     */
    public void testGetCallerContext() throws Exception
    {
        assertSame("The callerContext used for creation should be the returned ctx", ctx,
                appthreadgroup.getCallerContext());
        assertSame("The ctx should remain unchanged", appthreadgroup.getCallerContext(),
                appthreadgroup.getCallerContext());
    }

    private void doit(Listener l, final RuntimeException ex, final Error err) throws Exception
    {
        Throwable toThrow = (ex == null) ? (Throwable) err : (Throwable) ex;
        l.th = null;
        l.thr = null;

        Thread thread = new Thread(appthreadgroup, new Runnable()
        {
            public void run()
            {
                if (ex != null)
                    throw ex;
                else
                    throw err;
            }
        });

        thread.start();
        thread.join(); // wait for it to finish

        assertTrue("Thread should be dead", !thread.isAlive());
        assertTrue("Listener should've been called", l.th != null && l.thr != null);
        assertSame("Listener should've been called with correct thread", l.th, thread);
        assertSame("Listener should've been called with correct throwable", l.thr, toThrow);
    }

    /**
     * Tests that this method is called for an uncaught exception and that it
     * calls the installed listeners.
     */
    public void testUncaughtException() throws Exception
    {
        Listener l = new Listener();

        Throwable[] throwme = new Throwable[] { new Exception(), new Error(), new NullPointerException(),
                new LinkageError(), new ThreadDeath(), };

        appthreadgroup.addExceptionListener(l);
        doit(l, new RuntimeException(), null);
        doit(l, new NullPointerException(), null);
        doit(l, null, new Error());
        doit(l, null, new LinkageError());
        doit(l, null, new ThreadDeath());
    }

    /**
     * Tests that this method is called for an uncaught exception and that it
     * calls the installed listeners.
     */
    public void testUncaughtException_stop() throws Exception
    {
        if (org.cablelabs.impl.util.JavaVersion.PBP_10) return;

        Listener l = new Listener();
        appthreadgroup.addExceptionListener(l);

        // Now let's try with ThreadDeath by calling stop!
        final Object lock = new Object();
        Thread thread;
        l.th = null;
        l.thr = null;
        synchronized (lock)
        {
            thread = new Thread(appthreadgroup, new Runnable()
            {
                public void run()
                {
                    synchronized (lock)
                    {
                        lock.notify();
                    }
                    while (true)
                    {
                        ;
                    }
                }
            });

            // Start the thread
            thread.start();

            // wait here until it's running...
            lock.wait();
        }
        assertTrue("Thread should be alive", thread.isAlive());
        // kludge -- without this we can still get illegalmonitorstate exception
        // Why? I don't know. It occurs after the notify...
        thread.sleep(100);

        doThreadStop(thread);
        thread.join();
        assertTrue("Thread should be dead", !thread.isAlive());
        assertTrue("Listener should've been called", l.th != null && l.thr != null);
        assertSame("Listener should've been called with correct thread", thread, l.th);
        assertTrue("Listener should've been called with ThreadDeath", l.thr instanceof ThreadDeath);
    }

    private void doThreadStop(Thread t) throws Exception
    {
        if (org.cablelabs.impl.util.JavaVersion.PBP_10)
        {
            // Cannot use stop, so we'll just interrupt
            t.interrupt();
        }
        else
        {
            // To avoid compiler errors when compiling for PBP,
            // we'll use reflection to call t.stop().
            Class clz = t.getClass();
            java.lang.reflect.Method stop = clz.getMethod("stop", new Class[0]);
            stop.invoke(t, new Object[0]);
        }
    }

    /**
     * Tests that only one listener can be added.
     */
    public void testAddExceptionListener() throws Exception
    {
        Listener l = new Listener();
        appthreadgroup.addExceptionListener(l);
        try
        {
            appthreadgroup.addExceptionListener(new Listener());
            fail("Should not be able to add more than one listener");
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Tests that only the specified listener is removed. Tests that once it is
     * removed, another can be added.
     */
    public void testRemoveExceptionListener() throws Exception
    {
        Listener l = new Listener();
        appthreadgroup.addExceptionListener(l);
        try
        {
            appthreadgroup.removeExceptionListener(new Listener());
            fail("Should not be able to remove a non-added listener");
        }
        catch (Exception e)
        {
        }

        try
        {
            appthreadgroup.addExceptionListener(new Listener());
            fail("Should not be able to add more than one listener");
        }
        catch (Exception e)
        {
        }

        // No exceptions should be thrown...
        appthreadgroup.removeExceptionListener(l);
        appthreadgroup.addExceptionListener(l);
        appthreadgroup.removeExceptionListener(l);
    }

    private class Listener implements AppThreadGroup.ExceptionListener
    {
        public Thread th;

        public Throwable thr;

        public void uncaughtException(Thread th, Throwable thr)
        {
            this.th = th;
            this.thr = thr;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AppThreadGroupTest.class);
        return suite;
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public AppThreadGroupTest(String name)
    {
        super(name);
    }

    private AppThreadGroup appthreadgroup;

    private DummyContext ctx;

    protected void setUp() throws Exception
    {
        super.setUp();

        ctx = new DummyContext();
        appthreadgroup = new AppThreadGroup(ctx);
    }

    protected void tearDown() throws Exception
    {
        appthreadgroup = null;
        if (ctx != null) ctx.dispose();
        ctx = null;

        super.tearDown();
    }
}
