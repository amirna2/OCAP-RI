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

package org.cablelabs.impl.manager.timer;

import junit.framework.*;
import javax.tv.util.*;
import org.cablelabs.impl.manager.*;
import org.cablelabs.impl.manager.timer.TimerMgrJava2.TVTimerImpl;

import java.util.*;
import java.lang.ref.*;

/**
 * Tests TimerMgrJava2 implementation of TimerMgr.
 * 
 * @author Aaron Kamienski
 */
public class TimerMgrJava2Test extends TimerMgrTest
{
    /**
     * Tests the TimerMgrJava2.TVTimerImpl inner class implementation of
     * TVTimer.
     */
    public static class TVTimerImplTest extends TVTimerTest
    {
        /**
         * Test for bug #608 - TVTimer: memory leak for non-repeating
         * timerspecs.
         * 
         * Verify that the TVTimer implementation implicitly "forgets" about
         * non-repeating TVTimerSpecs that have expired. This includes
         * forgetting about the TimerTask(s) that is (are) used to implement the
         * TVTimerSpec.
         * 
         * To do this, we'll make use of Java2 Reference mechanism to watch for
         * garbage collection of the objects in question. If they are collected,
         * then it is determined that there is no leak.
         */
        public void testForgetNonRepeatingSpecs() throws Exception
        {
            final ReferenceQueue rq = new ReferenceQueue();
            final Vector refs = new Vector(); // written within TVTimerImpl
                                              // subclass
            final int schedules[] = { 0 }; // written within TVTimerImpl
                                           // subclass
            final boolean done[] = { false }; // written by watcher thread

            final long timeout = 50000;
            Thread watcher = new Thread()
            {
                public void run()
                {
                    Reference r;
                    try
                    {
                        while ((r = rq.remove(timeout)) != null)
                        {
                            refs.removeElement(r);
                            if (refs.size() == 0)
                            {
                                done[0] = true;
                                break;
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            };

            // Create a TVTimerImpl with addSpecTask overridden
            // so that we can create and record WeakReferences.
            final TVTimerImpl t = new TVTimerImpl()
            {
                TimerTask addSpecTask(TVTimerSpec spec, TimerTask task, TimerTask toRemove)
                {
                    refs.addElement(new WeakReference(spec, rq));
                    refs.addElement(new WeakReference(task, rq));
                    return super.addSpecTask(spec, task, toRemove);
                }
            };

            final TVTimerSpec specs[] = new TVTimerSpec[2];

            TVTimerWentOffListener l = new TVTimerWentOffListener()
            {
                public void timerWentOff(TVTimerWentOffEvent e)
                {
                    --schedules[0];
                    if (schedules[0] == 0)
                    {
                        // Should be done by now...
                    }
                }
            };

            long delay = granularity * 10;
            specs[0] = new TVTimerSpec();
            specs[1] = new TVTimerSpec();
            specs[0].setDelayTime(delay);
            specs[0].addTVTimerWentOffListener(l);
            specs[1].setDelayTime(delay);
            specs[1].addTVTimerWentOffListener(l);

            int scheduled = schedules[0];
            t.scheduleTimerSpec(specs[0]);
            scheduled = ++schedules[0];
            t.scheduleTimerSpec(specs[0]);
            scheduled = ++schedules[0];
            t.scheduleTimerSpec(specs[1]);
            scheduled = ++schedules[0];
            t.scheduleTimerSpec(specs[1]);
            scheduled = ++schedules[0];
            t.scheduleTimerSpec(specs[0]);
            scheduled = ++schedules[0];
            t.scheduleTimerSpec(specs[1]);
            scheduled = ++schedules[0];

            // Verify the references that we have...
            // We expect 2 per schedule (TVTimerSpec and TimerTask)
            assertEquals("Expected 2 references recorded per scheduled TVTimerSpec", schedules[0] * 2, refs.size());
            for (int i = 0; i < refs.size(); ++i)
            {
                Reference r = (Reference) refs.elementAt(i);
                assertNotNull("Expected to find non-null References saved", r);
                assertNotNull("Expected references to initially be intact", r.get());
            }
            assertEquals("If any have gone off now, then we need to adjust the delay higher", scheduled, schedules[0]);

            // Start the watcher
            watcher.start();

            // Sleep long enough for timers to go off
            // The listeners could signal us.
            Thread.sleep(delay * 2);

            // Remove what we think should be the last remaining references to
            // specs
            for (int i = 0; i < specs.length; ++i)
                specs[i] = null;

            // Force gc/finalization
            System.gc();
            System.runFinalization();
            System.gc();
            System.runFinalization();
            Thread.sleep(500); // kludge to allow watcher thread to run

            // Verify that schedules[0] == 0
            assertEquals("Expected all each scheduled TVTimerSpec to call listener", 0, schedules[0]);
            // Verify that refs is now empty
            if (refs.size() != 0)
            {
                for (int i = 0; i < refs.size(); ++i)
                {
                    Reference r = (Reference) refs.elementAt(i);
                    if (r != null) assertSame("All refs should've been cleaned up now", null, r.get());
                }
            }
            assertEquals("Expected all refs to have been cleaned up now", 0, refs.size());
        }

        /**
         * Ensure that dispose() actually cleans up the timer. The Java2
         * <code>Timer</code> is cleaned up.
         */
        public void testDispose() throws Exception
        {
            // fail("Unimplemented test");

            // 1. Record the threads executing before creating a timer
            // 2. Record the threads executing after creating a timer
            // (May need to schedule a TVTimerSpec to force creation if lazy)
            // 3. Record the threads executing after disposing the timer
            // We should have 1==3, and 2 has one thread greater than 1.

            // Find top-level ThreadGroup
            ThreadGroup parent;
            ThreadGroup tg = Thread.currentThread().getThreadGroup();
            while ((parent = tg.getParent()) != null)
                tg = parent;

            // Record (1)
            int count1 = tg.activeCount();
            Thread threads1[] = new Thread[count1];
            tg.enumerate(threads1, true);

            TVTimerImpl timer = new TVTimerImpl();
            TVTimerSpec spec = new TVTimerSpec();
            spec.setDelayTime(granularity);
            timer.scheduleTimerSpec(spec);
            Thread.sleep(granularity);

            // Record (2)
            int count2 = tg.activeCount();
            Thread threads2[] = new Thread[count2];
            tg.enumerate(threads2, true);
            // Verify
            assertEquals("Expected a single worker thread to be created for the timer", count1 + 1, count2);

            // Actually do the dispose!
            timer.dispose();
            Thread.sleep(500); // kludge to allow timer thread to go away

            // Record (3)
            int count3 = tg.activeCount();
            Thread threads3[] = new Thread[count3];
            tg.enumerate(threads3, true);
            // Verify
            assertEquals("Expected single worker thread to go away after dispose()", count1, count3);
        }

        public void setUp() throws Exception
        {
            super.setUp();
            timer = new TVTimerImpl();
        }

        public void tearDown() throws Exception
        {
            timer = null;
            super.tearDown();
        }

        public TVTimerImplTest(String name)
        {
            super(name);
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

        public static Test suite(String[] tests)
        {
            if (tests == null || tests.length == 0)
                return suite();
            else
            {
                TestSuite suite = new TestSuite(TVTimerImplTest.class.getName());
                for (int i = 0; i < tests.length; ++i)
                    suite.addTest(new TVTimerImplTest(tests[i]));
                return suite;
            }
        }

        public static Test suite()
        {
            TestSuite suite = new TestSuite(TVTimerImplTest.class);
            return suite;
        }
    }

    /**
     * Tests getTimer(). Ensure that getTimer(CallerContext) is called with the
     * correct context.
     */
    public void testGetTimer()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        // !!! It might be better to create a stub context to run in...
        CallerContext context = ccm.getCurrentContext();
        TimerMgr mgr = null;
        final TVTimerImpl dummy = new TVTimerImpl();
        TVTimer timer = null;
        final CallerContext ctxArg[] = { null };
        try
        {
            mgr = new TimerMgrJava2()
            {
                public TVTimer getTimer(CallerContext ctx)
                {
                    ctxArg[0] = ctx;
                    return dummy;
                }
            };

            timer = mgr.getTimer();
            assertSame("Expected dummy timer returned", dummy, timer);
            assertSame("getTimer() should call getTimer(ctx) with the current Context", context, ctxArg[0]);
        }
        finally
        {
            if (mgr != null)
            {
                if (dummy != null) mgr.disposeTimer(context, dummy);
                mgr.destroy();
            }
        }

        timer = timermgr.getTimer();
        assertNotNull("getTimer() should not return null", timer);
        assertSame("getTimer() should return an instance of TVTimerImpl", TVTimerImpl.class, timer.getClass());
        // Do not dispose "current context"'s timer
    }

    /**
     * Tests createTimer().
     */
    public void testCreateTimer()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        // We don't care about the CallerContext at this point, so we'll test w/
        // null
        CallerContext contexts[] = { null, null, ccm.getSystemContext(), ccm.getCurrentContext() };
        TVTimer timers[] = new TVTimer[contexts.length];

        try
        {
            for (int i = 0; i < timers.length; ++i)
            {
                timers[i] = timermgr.createTimer(contexts[i]);
                assertNotNull("createTimer should never return null", timers[i]);
                assertSame("createTimer should return an instance of TVTimerImpl", TVTimerImpl.class,
                        timers[i].getClass());
            }
        }
        finally
        {
            // Cleanup
            // Since these are stored with the CallerContext we can dispose of
            // them
            for (int i = 0; i < timers.length; ++i)
            {
                if (timers[i] != null) timermgr.disposeTimer(contexts[i], timers[i]);
            }
        }
    }

    /**
     * Tests disposeTimer().
     */
    public void testDisposeTimer()
    {
        // Ensure that it calls dispose on the TVTimerImpl
        final boolean called[] = { false };
        TVTimerImpl timer = new TVTimerImpl()
        {
            public void dispose()
            {
                called[0] = true;
                super.dispose();
            }
        };
        timermgr.disposeTimer(null, timer);
        assertTrue("disposeTimer() should call dispose on TVTimerImpl", called[0]);
    }

    /**
     * Returns an instance of the TimerMgr under test.
     */
    protected TimerMgr createTimerMgr()
    {
        return new TimerMgrJava2();
    }

    public TimerMgrJava2Test(String name)
    {
        super(name);
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

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(TimerMgrJava2Test.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new TimerMgrJava2Test(tests[i]));
            return suite;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(TimerMgrJava2Test.class);
        suite.addTest(new TestSuite(TVTimerImplTest.class));
        return suite;
    }
}
