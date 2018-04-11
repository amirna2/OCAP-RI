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

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.util.TaskQueue;
import org.cablelabs.test.TestUtils;

import java.util.Properties;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests SystemContext. Note that SystemContext does not test the
 * CallerContextTest interface test. It cannot be expected to do things like
 * pause/resume/destroy.
 * 
 * @author Aaron Kamienski
 */
public class SystemContextTest extends AbstractCallerContextTest
{
    public void testConstructor()
    {
        // TODO test constructor
    }

    /**
     * Tests no public constructors.
     */
    public void testNoPublicConstructor()
    {
        TestUtils.testNoPublicConstructors(SystemContext.class);
    }

    public void testIsAlive()
    {
        assertTrue("Expect SystemContext.isAlive() to always return true", acc.isAlive());
    }

    public void testCheckAlive()
    {
        acc.checkAlive();
    }

    public void testIsActive()
    {
        assertTrue("Expect SystemContext.isActive() to always return true", acc.isAlive());
    }

    public void testGet()
    {
        Object[] keys = { CallerContext.APP_ID, CallerContext.APP_PRIORITY, CallerContext.SERVICE_CONTEXT,
                CallerContext.THREAD_GROUP, CallerContext.USER_DIR };
        for (int i = 0; i < keys.length; ++i)
            assertNull("Expected get() to return null for " + keys[i], acc.get(keys[i]));
    }

    /**
     * Tests createTaskQueue.
     */
    public void testCreateTaskQueue() throws Exception
    {
        TaskQueue tq;
        try
        {
            tq = acc.createTaskQueue();
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

            tq = acc.createTaskQueue();
            synchronized (ran)
            {
                ran.clear();
                tq.post(new Run());
                ran.wait(5000L);
            }
            assertEquals("Expected another queue to be useable", 1, ran.size());

            // Don't shutdown system context...
            /*
             * // Now, shutdown the caller context...
             * factory.makeDestroyed(acc);
             * 
             * try { acc.createTaskQueue();fail(
             * "Expected IllegalStateException when creating queue for disposed context"
             * ); } catch(IllegalStateException e) { }
             * 
             * try { tq.post(new Run());fail(
             * "Expected IllegalStateException for post to implicitly disposed queue"
             * ); } catch(IllegalStateException e) { }
             */
        }
        finally
        {
            if (tq != null) tq.dispose();
        }
    }

    protected AbstractCallerContext createAbstractCallerContext()
    {
        return new SystemContext(ccMgr, new ThreadPool("sys", null, 1, Thread.NORM_PRIORITY), Thread.NORM_PRIORITY);
    }

    /**
     * Overrides super implementation.
     * 
     * @see org.cablelabs.impl.manager.application.AbstractCallerContextTest#destroyAbstractCallerContext(org.cablelabs.impl.manager.application.AbstractCallerContext)
     */
    protected void destroyAbstractCallerContext(AbstractCallerContext cc)
    {
        cc.q.dispose();
    }

    public SystemContextTest(String name)
    {
        super(name);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(SystemContextTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new SystemContextTest(tests[i]));
            return suite;
        }
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(SystemContextTest.class);
        /*
         * ImplFactory factory = new ContextImplFactory() { public Object
         * createImplObject() { return SystemContextTest.createImplObject(); }
         * public void makeDestroyed(CallerContext ctx) throws Exception {
         * doDestroyed((SystemContext)ctx); } public void
         * makePaused(CallerContext ctx) throws Exception {
         * doPaused((SystemContext)ctx); } public void makeResumed(CallerContext
         * ctx) throws Exception { doResumed((SystemContext)ctx); } };
         * InterfaceTestSuite ctxSuite = CallerContextTest.isuite();
         * 
         * ctxSuite.addFactory(factory); suite.addTest(ctxSuite);
         */

        return suite;
    }
}
