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

package org.cablelabs.impl.manager;

import org.cablelabs.impl.util.TaskQueue;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

/**
 * Tests CallerContext interface.
 */
public class CallerContextTest extends InterfaceTestCase
{
    /**
     * Tests {add|remove|get}CallbackData(). Also check that callback data is
     * called as appropriate.
     */
    public void testCallbackData()
    {
        Object key = new Object();

        assertNull("Data should not have been added yet", ctx.getCallbackData(key));

        // Add data
        CallbackData data = new CallbackData.SimpleData(null);
        ctx.addCallbackData(data, key);
        assertSame("Data should be retrievable via key", data, ctx.getCallbackData(key));

        // Add more data
        Object key2 = "A String";
        CallbackData data2 = new CallbackData.SimpleData(null);
        ctx.addCallbackData(data2, key2);
        assertSame("Data should be retrievable via key", data2, ctx.getCallbackData(key2));
        assertSame("Other data should still be retrievable via key", data, ctx.getCallbackData(key));

        // Replace data
        CallbackData data3 = new CallbackData.SimpleData(null);
        ctx.addCallbackData(data3, key);
        assertSame("New data should replace old data", data3, ctx.getCallbackData(key));

        // Remove data
        ctx.removeCallbackData(key2);
        assertNull("Removed data should no longer be available", ctx.getCallbackData(key2));
        assertSame("Unremoved data should still be available", data3, ctx.getCallbackData(key));

        // Removed data can be readded
        ctx.addCallbackData(data2, key2);
        assertSame("Data should be retrievable via key", data2, ctx.getCallbackData(key2));

        try
        {
            ctx.addCallbackData(null, "");
            fail("NullPointerException should be thrown for null CallbackData on add");
        }
        catch (NullPointerException e)
        {
        }
        try
        {
            ctx.addCallbackData(data2, null);
            fail("NullPointerException should be thrown for null key on add");
        }
        catch (NullPointerException e)
        {
        }
        try
        {
            ctx.removeCallbackData(null);
            fail("NullPointerException should be thrown for null key on remove");
        }
        catch (NullPointerException e)
        {
        }
        try
        {
            ctx.getCallbackData(null);
            fail("NullPointerException should be thrown for null key on get");
        }
        catch (NullPointerException e)
        {
        }

        // Remove all added data
        ctx.removeCallbackData(key2);
        ctx.removeCallbackData(key);

        // Calling again shouldn't cause any errors.
        ctx.removeCallbackData(key2);
        ctx.removeCallbackData(key);
    }

    /**
     * Tests that callback data is called back...
     */
    public void testCallbackDataCallback() throws Exception
    {
        TestCallback cb = new TestCallback();

        ctx.addCallbackData(cb, cb);
        assertTrue("No callbacks should be made yet", cb.destroyed == null && cb.paused == null && cb.actived == null);

        cb.destroyed = cb.paused = cb.actived = null;
        factory.makePaused(ctx);
        assertNotNull("Paused callback should be made in response to pause", cb.paused);
        assertSame("Expected the given context", ctx, cb.paused);
        assertNull("Destroyed callback should NOT be made in response to pause", cb.destroyed);
        assertNull("Resumed callback should NOT be made in response to pause", cb.actived);

        cb.destroyed = cb.paused = cb.actived = null;
        factory.makeActive(ctx);
        assertNotNull("Resumed callback should be made in response to pause", cb.actived);
        assertSame("Expected the given context", ctx, cb.actived);
        assertNull("Paused callback should NOT be made in response to pause", cb.paused);
        assertNull("Destroyed callback should NOT be made in response to pause", cb.destroyed);

        cb.destroyed = cb.paused = cb.actived = null;
        factory.makeDestroyed(ctx);
        assertNotNull("Destroyed callback should be made in response to pause", cb.destroyed);
        assertSame("Expected the given context", ctx, cb.destroyed);
        assertNull("Paused callback should be NOT made in response to pause", cb.paused);
        assertNull("Resumed callback should NOT be made in response to pause", cb.actived);

        ctx.removeCallbackData(cb);
    }

    /**
     * Tests that callback data is NOT called back...
     */
    public void testCallbackDataNoCallback() throws Exception
    {
        TestCallback cb = new TestCallback();

        ctx.addCallbackData(cb, cb);
        assertTrue("No callbacks should be made yet", cb.destroyed == null && cb.paused == null && cb.actived == null);

        ctx.removeCallbackData(cb);

        cb.destroyed = cb.paused = cb.actived = null;
        factory.makePaused(ctx);
        assertNull("Paused callback should NOT be made in response to pause", cb.paused);
        assertNull("Destroyed callback should NOT be made in response to pause", cb.destroyed);
        assertNull("Resumed callback should NOT be made in response to pause", cb.actived);

        cb.destroyed = cb.paused = cb.actived = null;
        factory.makeActive(ctx);
        assertNull("Paused callback should NOT be made in response to pause", cb.paused);
        assertNull("Destroyed callback should NOT be made in response to pause", cb.destroyed);
        assertNull("Resumed callback should NOT be made in response to pause", cb.actived);

        cb.destroyed = cb.paused = cb.actived = null;
        factory.makeDestroyed(ctx);
        assertNull("Paused callback should NOT be made in response to pause", cb.paused);
        assertNull("Destroyed callback should NOT be made in response to pause", cb.destroyed);
        assertNull("Resumed callback should NOT be made in response to pause", cb.actived);
    }

    /**
     * Tests runInContext().
     */
    public void testRunInContext() throws Exception
    {
        // Get to an active state
        factory.makeActive(ctx);

        final Thread[] called = new Thread[1];
        Runnable run = new Runnable()
        {
            public void run()
            {
                synchronized (called)
                {
                    called[0] = Thread.currentThread();
                    called.notifyAll();
                }
            }
        };

        called[0] = null;
        synchronized (called)
        {
            ctx.runInContext(run);
            called.wait(1000);
        }
        assertNotNull("Runnable should've been run by runInContext (sync)", called[0]);
        assertNotSame("Runnable should've been run in another thread (sync)", called[0], Thread.currentThread());

        // Exceptions
        try
        {
            ctx.runInContext(null);
            fail("Should throw NullPointerException given null Runnable");
        }
        catch (NullPointerException e)
        {
        }
    }

    /**
     * Tests runInContext(Runnable, true) and runInContext(Runnable,false)
     */
    public void testRunInContextSyncAsync() throws Exception
    {
        // Get to an active state
        factory.makeActive(ctx);

        final CallerContext[] called = new CallerContext[1];
        final Thread[] thread = new Thread[1];
        Runnable run = new Runnable()
        {
            public void run()
            {
                synchronized (called)
                {
                    CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                    called[0] = ccm.getCurrentContext();
                    thread[0] = Thread.currentThread();
                    called.notifyAll();
                }
            }
        };

        // Get serial async thread
        called[0] = null;
        synchronized (called)
        {
            ctx.runInContext(run);
            called.wait(5000);
        }
        assertNotNull("Runnable should've been run by runInContext (serial)", called[0]);
        assertSame("Expected Runnable to run in given context", ctx, called[0]);
        assertNotNull("Expected non-null thread", thread[0]);
        Thread serial = thread[0];

        // Test waiting
        called[0] = null;
        ctx.runInContextSync(run); // waiting
        assertNotNull("Runnable should've been run synchronously by runInContext", called[0]);
        assertSame("Expected Runnable to run in given context", ctx, called[0]);
        Thread sync = thread[0];

        // Test non-waiting (fully async)
        called[0] = null;
        synchronized (called)
        {
            ctx.runInContextAsync(run);
            called.wait(5000);
        }
        assertNotNull("Runnable should've been run by runInContext (async)", called[0]);
        assertSame("Expected Runnable to run in given context", ctx, called[0]);
        assertNotSame("Runnable should've been run in another thread (async)", sync, thread[0]);
        assertNotSame("Runnable should've been run in another thread (async)", serial, thread[0]);
        assertNotSame("Runnable should've been run in another thread (async)", Thread.currentThread(), thread[0]);

        // NullPointerException
        try
        {
            ctx.runInContextSync(null);
            fail("Should throw NullPointerException given null Runnable");
        }
        catch (NullPointerException e)
        {
        }
        try
        {
            ctx.runInContextAsync(null);
            fail("Should throw NullPointerException given null Runnable");
        }
        catch (NullPointerException e)
        {
        }

        // InvocationTargetException
        final RuntimeException re = new RuntimeException();
        InvocationTargetException ite = null;
        try
        {
            ctx.runInContextSync(new Runnable()
            {
                public void run()
                {
                    throw re;
                }
            });
        }
        catch (InvocationTargetException e)
        {
            ite = e;
        }
        assertNotNull("InvocationTargetException should've been thrown", ite);
        assertSame("InvocationTargetException should have thrown exception", re, ite.getTargetException());
    }

    /**
     * Tests runInContext() on a non-active context.
     */
    public void testRunInContextIllegalState() throws Exception
    {
        Runnable run = new Runnable()
        {
            public void run()
            {
            }
        };

        // Try destroyed
        factory.makeDestroyed(ctx);
        try
        {
            ctx.runInContext(run);
            fail("Expected IllegalStateException for destroyed app");
        }
        catch (IllegalStateException e)
        {
        }
        try
        {
            ctx.runInContextSync(run);
            fail("Expected IllegalStateException for destroyed app");
        }
        catch (IllegalStateException e)
        {
        }
    }

    /**
     * Tests isAlive().
     */
    public void testIsAlive() throws Exception
    {
        assertTrue("Context should be alive upon creation", ctx.isAlive());

        factory.makeDestroyed(ctx);

        assertFalse("Context should be dead upon destruction", ctx.isAlive());
    }

    /**
     * Tests checkAlive().
     */
    public void testCheckAlive() throws Exception
    {
        try
        {
            ctx.checkAlive();
        }
        catch (Exception e)
        {
            fail("checkAlive should not throw exceptions upon creation");
        }

        factory.makeDestroyed(ctx);

        try
        {
            ctx.checkAlive();
            fail("checkAlive should throw a SecurityException");
        }
        catch (SecurityException e)
        {
        }
    }

    /**
     * Tests isActive().
     */
    public void testIsActive() throws Exception
    {
        assertFalse("Context should not be active upon creation", ctx.isActive());

        factory.makeActive(ctx);

        assertTrue("context should be active", ctx.isActive());

        factory.makePaused(ctx);

        assertFalse("context should not be active when paused", ctx.isActive());

        factory.makeActive(ctx);

        assertTrue("context should be active", ctx.isActive());

        factory.makeDestroyed(ctx);

        assertFalse("Context should be dead upon destruction", ctx.isActive());
    }

    /**
     * Tests createTaskQueue.
     */
    public void testCreateTaskQueue() throws Exception
    {
        TaskQueue tq;
        try
        {
            tq = ctx.createTaskQueue();
        }
        catch (UnsupportedOperationException e)
        {
            return;
        }
        assertNotNull("createTaskQueue should not return null", tq);

        try
        {
            // Some simple tests with the task queue
            final Vector ran = new Vector();
            class Run implements Runnable
            {
                public void run()
                {
                    synchronized (ran)
                    {
                        ran.addElement(this);
                    }
                }
            }
            Run[] runs = { new Run(), new Run(), null, null, new Run() };
            runs[2] = runs[0];
            runs[3] = runs[1];

            for (int i = 0; i < runs.length; ++i)
                tq.post(runs[i]);
            if (ran.size() != runs.length)
            {
                synchronized (ran)
                {
                    ran.wait(5000L);
                }
            }
            assertEquals("Expected tasks to be executed", runs.length, ran.size());
            for (int i = 0; i < runs.length; ++i)
            {
                assertSame("Expected tasks to be run in order " + i, runs[i], ran.elementAt(i));
            }
            ran.clear();

            // Dispose, then try and use
            tq.dispose();
            try
            {
                tq.post(new Run());
                fail("Expected an IllegalStateException for post following dispose");
            }
            catch (IllegalStateException e)
            { /* expected */
            }

            tq = ctx.createTaskQueue();
            synchronized (ran)
            {
                ran.clear();
                tq.post(new Run());
                ran.wait(5000L);
            }
            assertEquals("Expected another queue to be useable", 1, ran.size());

            // Now, shutdown the caller context...
            factory.makeDestroyed(ctx);

            try
            {
                ctx.createTaskQueue();
                fail("Expected IllegalStateException when creating queue for disposed context");
            }
            catch (IllegalStateException e)
            { /* expected */
            }

            try
            {
                tq.post(new Run());
                fail("Expected IllegalStateException for post to implicitly disposed queue");
            }
            catch (IllegalStateException e)
            { /* expected */
            }

        }
        finally
        {
            if (tq != null) tq.dispose();
        }
    }

    public interface ContextImplFactory extends ImplFactory
    {
        public void setUp() throws Exception;

        public void tearDown() throws Exception;

        public void makeDestroyed(CallerContext ctx) throws Exception;

        public void makePaused(CallerContext ctx) throws Exception;

        public void makeActive(CallerContext ctx) throws Exception;
    }

    private static class TestCallback implements CallbackData
    {
        public CallerContext destroyed;

        public void destroy(CallerContext ctx)
        {
            destroyed = ctx;
        }

        public CallerContext paused;

        public void pause(CallerContext ctx)
        {
            paused = ctx;
        }

        public CallerContext actived;

        public void active(CallerContext ctx)
        {
            actived = ctx;
        }
    }

    /* ======================== Boilerplate =========================== */

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(CallerContextTest.class);
        suite.setName("org.cablelabs.impl.manager.CallerContext");
        return suite;
    }

    public CallerContextTest(String name, ImplFactory f)
    {
        super(name, CallerContext.class, f);
        factory = (ContextImplFactory) f;
    }

    protected CallerContext createCallerContext()
    {
        return (CallerContext) createImplObject();
    }

    private ContextImplFactory factory;

    protected CallerContext ctx;

    protected void setUp() throws Exception
    {
        super.setUp();

        factory.setUp();
        ctx = createCallerContext();
    }

    protected void tearDown() throws Exception
    {
        factory.makeDestroyed(ctx);
        factory.tearDown();
        ctx = null;
        super.tearDown();
    }
}
