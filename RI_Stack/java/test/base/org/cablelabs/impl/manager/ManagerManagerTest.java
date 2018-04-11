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

import junit.framework.TestCase;

/**
 * Tests the ManagerManager class.
 */
public class ManagerManagerTest extends TestCase
{
    public ManagerManagerTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();

        ManagerManager.cleanManagers();
    }

    protected void tearDown() throws Exception
    {
        ManagerManager.resetManagers();

        super.tearDown();
    }

    /**
     * Tests startAll().
     */
    public void testStartAll() throws Exception
    {
        // Add a couple of Managers
        ManagerManager.updateManager(TestManager.class, TestManagerImpl.class, true, null);
        ManagerManager.updateManager(Test1Manager.class, Test1ManagerImpl.class, false, null);
        ManagerManager.updateManager(Test2Manager.class, Test2ManagerImpl.class, true, null);

        // Start all autostart managers
        ManagerManager.startAll();

        // Check that the right ones were started
        assertFalse("Manager should've been started", TestManagerImpl.isDestroyed());
        assertTrue("Manager should not have been started", Test1ManagerImpl.isDestroyed());
        assertFalse("Manager should've been started", Test2ManagerImpl.isDestroyed());

        ManagerManager.getInstance(Test1Manager.class);
        assertFalse("Manager should've been started", Test1ManagerImpl.isDestroyed());
    }

    /**
     * Tests destroyAll().
     * <ol>
     * <li>Ensures that destroyAll() actually results in destroy being called on
     * all managers.
     * <li>Ensures that all subsequent manager calls fail.
     * </ol>
     */
    public void testDestroyAll() throws Exception
    {
        // Add a couple of Managers
        ManagerManager.updateManager(TestManager.class, TestManagerImpl.class, true, null);
        ManagerManager.updateManager(Test1Manager.class, Test1ManagerImpl.class, true, null);
        ManagerManager.updateManager(Test2Manager.class, Test2ManagerImpl.class, true, null);
        ManagerManager.startAll();

        // First, make sure that they were all started
        assertFalse("Manager should've been started", TestManagerImpl.isDestroyed());
        assertFalse("Manager should've been started", Test1ManagerImpl.isDestroyed());
        assertFalse("Manager should've been started", Test2ManagerImpl.isDestroyed());

        // Now call destroyAll
        ManagerManager.destroyAll();

        // Now, make sure that they were all destroyed
        assertTrue("Manager should've been destroyed", TestManagerImpl.isDestroyed());
        assertTrue("Manager should've been destroyed", Test1ManagerImpl.isDestroyed());
        assertTrue("Manager should've been destroyed", Test2ManagerImpl.isDestroyed());

        try
        {
            ManagerManager.getInstance(TestManagerImpl.class);
            fail("Expect all manager calls to fail after destroyAll()");
        }
        catch (Exception e)
        { /* ignored */
        }
        try
        {
            ManagerManager.getInstance(Test1ManagerImpl.class);
            fail("Expect all manager calls to fail after destroyAll()");
        }
        catch (Exception e)
        { /* ignored */
        }
        try
        {
            ManagerManager.getInstance(Test2ManagerImpl.class);
            fail("Expect all manager calls to fail after destroyAll()");
        }
        catch (Exception e)
        { /* ignored */
        }
    }

    public void testGetInstance() throws Exception
    {
        // Add a couple of Managers
        ManagerManager.updateManager(TestManager.class, Test1ManagerImpl.class, true, null);
        ManagerManager.updateManager(Test1Manager.class, Test1ManagerImpl.class, true, null);
        ManagerManager.updateManager(Test2Manager.class, Test2ManagerImpl.class, true, null);

        Manager m;

        // GetInstance by class
        m = ManagerManager.getInstance(TestManager.class);
        assertNotNull("A manager should've been returned (for TestManager.class)", m);
        assertTrue("Manager should be of specified class", TestManager.class.isInstance(m));

        assertSame("Same instance should be returned", m, ManagerManager.getInstance(TestManager.class));

        assertSame("Same instance should be returned", m, ManagerManager.getInstance(Test1Manager.class));

        m = ManagerManager.getInstance(Test2Manager.class);
        assertNotNull("A manager should've been returned (for Test2Manager.class)", m);
        assertTrue("Manager should be of specified class", Test2Manager.class.isInstance(m));
    }

    public void testGetInstance_redirect() throws Exception
    {
        // Add a couple of Managers
        ManagerManager.updateManager(TestManager.class, Test1Manager.class, true, null, true);
        ManagerManager.updateManager(Test1Manager.class, Test3ManagerImpl.class, false, null, false);
        ManagerManager.updateManager(Test2Manager.class, TestManager.class, false, null, true);

        Manager m;

        // GetInstance by class
        m = ManagerManager.getInstance(TestManager.class);
        assertNotNull("A manager should've been returned (for TestManager.class)", m);
        assertTrue("Manager should be of specified class", TestManager.class.isInstance(m));

        assertSame("Same instance should be returned", m, ManagerManager.getInstance(TestManager.class));

        assertSame("Same instance should be returned", m, ManagerManager.getInstance(Test1Manager.class));
        assertTrue("Manager should be of specified class", Test1Manager.class.isInstance(m));

        assertSame("Same instance should be returned", m, ManagerManager.getInstance(Test2Manager.class));
        assertTrue("Manager should be of specified class", Test2Manager.class.isInstance(m));
    }

    /**
     * Tests getInstance() thread-safety.
     */
    public void testGetInstance_threadSafety() throws Exception
    {
        final Class clazz = Test1Manager.class;
        ManagerManager.updateManager(clazz, TestThreadManager.class, false, null);

        class Counter
        {
            private int count;

            synchronized int incr()
            {
                int tmp = ++count;
                notify();
                return tmp;
            }

            synchronized int get()
            {
                return count;
            }

            synchronized void wait(int dest, long ms) throws InterruptedException
            {
                while (count < dest)
                    wait(ms);
            }
        }
        final boolean go[] = { false };
        final Counter startedCounter = new Counter();
        final Counter doneCounter = new Counter();
        class TestThread extends Thread
        {
            public Manager mgr;

            public void run()
            {
                startedCounter.incr();

                try
                {
                    // Wait until all threads are started
                    synchronized (go)
                    {
                        if (!go[0]) go.wait();
                    }
                }
                catch (InterruptedException e)
                { /* ignored */
                }

                mgr = ManagerManager.getInstance(clazz);
                doneCounter.incr();
            }
        }
        TestThread[] threads = new TestThread[30];

        for (int i = 0; i < threads.length; ++i)
            threads[i] = new TestThread();
        int count = 0;
        for (int i = 0; i < threads.length; ++i)
        {
            try
            {
                threads[i].start();
                ++count;
            }
            catch (Throwable e)
            {
                break;
            }
        }
        int minThreads = threads.length / 3;
        assertTrue("Expected at least " + minThreads + " threads to run (was " + count + ")", count > minThreads);

        // Wait for threads to be started
        startedCounter.wait(count, 20000L);
        assertEquals("Expected threads to be started", count, startedCounter.get());

        // Tell threads to go
        // (Assume that thread startup is slower than monitor notification)
        synchronized (go)
        {
            go[0] = true;
            go.notifyAll();
        }

        // Wait for threads to be done
        doneCounter.wait(count, 20000L);
        assertEquals("Expected threads to run", count, doneCounter.get());

        // Finally, verify that they all have the same instance!
        Manager mgr = ManagerManager.getInstance(clazz);
        assertNotNull("Internal error - manager not installed " + clazz);
        for (int i = 0; i < count; ++i)
        {
            assertSame("Thread safety broke down as two different managers created", mgr, threads[i].mgr);
        }
        for (int i = count; count < threads.length; ++i)
            assertNull("Expected thread[" + i + "] not to have executed", threads[i].mgr);
    }

    /**
     * Tests getInstance() ability to detect a cycle.
     */
    public void testGetInstance_cycle() throws Exception
    {
        ManagerManager.updateManager(Test1Manager.class, TestCycleManager.class, false, null);
        Manager mgr = ManagerManager.getInstance(Test1Manager.class);
        assertTrue("Expected getInstance() to have been called", TestCycleManager.called);
        assertTrue("Expected IllegalStateException to be thrown", TestCycleManager.error);
        assertNull("Expected getInstance() to fail because of cycle", mgr);

        // If there's a StackOverflow... that will be flagged as an error!
    }

    /**
     * Makes the package-private static method available to other test code.
     * 
     * @see ManagerManager#updateManager(Class, Class, boolean, Manager)
     */
    public static void updateManager(Class clazz, Class implClass, boolean auto, Manager impl)
    {
        ManagerManager.updateManager(clazz, implClass, auto, impl);
    }

    /**
     * Makes the package-private static method available to other test code.
     * 
     * @see ManagerManager#updateManager(Class, Class, boolean, Manager,
     *      boolean)
     */
    public static void updateManager(Class clazz, Class implClass, boolean auto, Manager impl, boolean redirect)
    {
        ManagerManager.updateManager(clazz, implClass, auto, impl, redirect);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(ManagerManagerTest.class);
    }
}

/** Simple manager interface. */
interface Test1Manager extends Manager
{
    // Empty
}

/** Simple manager interface. */
interface Test2Manager extends Manager
{
    // Empty
}

/** Simple manager interface. */
interface TestManager extends Manager
{
    // Empty
}

/** Simple manager interface. */
interface Test3Manager extends Manager
{
    // Empty
}

/** Simple manager implementation. */
class TestManagerImpl implements TestManager, Test1Manager, Test2Manager
{
    private static Manager instance;

    public static Manager getInstance()
    {
        if (instance == null) instance = new TestManagerImpl();
        return instance;
    }

    public void destroy()
    {
        instance = null;
    }

    public static boolean isDestroyed()
    {
        return instance == null;
    }
}

/** Simple manager implementation. */
class Test1ManagerImpl implements TestManager, Test1Manager
{
    private static Manager instance;

    public static Manager getInstance()
    {
        if (instance == null) instance = new Test1ManagerImpl();
        return instance;
    }

    public void destroy()
    {
        instance = null;
    }

    public static boolean isDestroyed()
    {
        return instance == null;
    }
}

/** Simple manager implementation. */
class Test2ManagerImpl implements TestManager, Test2Manager
{
    private static Manager instance;

    public static Manager getInstance()
    {
        if (instance == null) instance = new Test2ManagerImpl();
        return instance;
    }

    public void destroy()
    {
        instance = null;
    }

    public static boolean isDestroyed()
    {
        return instance == null;
    }
}

class Test3ManagerImpl implements TestManager, Test1Manager, Test2Manager, Test3Manager
{
    private static boolean destroyed;

    public static Manager getInstance()
    {
        Thread.yield(); // Give up timeslice (see testGetInstance_threadSafety)
        return new Test3ManagerImpl();
    }

    public void destroy()
    {
        destroyed = true;
    }

    public static boolean isDestroyed()
    {
        return destroyed;
    }
}

class TestThreadManager implements Test1Manager
{
    public static Manager getInstance()
    {
        Thread.yield();
        // try { Thread.sleep(1000); } catch(InterruptedException e) { /* ignore
        // */}
        return new TestThreadManager();
    }

    public void destroy()
    { /* empty */
    }
}

class TestCycleManager implements Test1Manager
{
    static boolean called = false;

    static boolean error = false;

    public static Manager getInstance()
    {
        called = true;
        try
        {
            ManagerManager.getInstance(Test1Manager.class);
        }
        catch (IllegalStateException e)
        {
            error = true;
            throw e;
        }
        return new TestCycleManager();
    }

    public void destroy()
    { /* empty */
    }
}
