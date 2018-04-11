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

import junit.framework.TestCase;

/**
 * Tests AbstractCallerContextTest. This test is abstract and meant to be
 * extended.
 * 
 * @author Aaron Kamienski
 */
public abstract class AbstractCallerContextTest extends TestCase
{

    /**
     * Tests constructor.
     */
    public void testConstructor()
    {
        // TODO: test constructor...
        // execqueue
        // tp
        // ccMgr
    }

    /**
     * Tests setExecQueue(). After constructor, always expect this method to
     * fail.
     */
    public void testSetExecQueue()
    {
        try
        {
            acc.setExecQueue(new ExecQueue());
            fail("Expected setExecQueue() to fail");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    /**
     * Given a <code>Runnable</code>, runAsContext() should appear to execute in
     * context under test. Event when executed from another. The priority of the
     * calling thread should not change.
     */
    public void testRunAsContextSamePriority()
    {
        final int[] ccPriority = { -1 };
        final CallerContext[] cc = { null };

        int priority = Thread.MAX_PRIORITY;
        Thread.currentThread().setPriority(priority);
        /*
        acc.runAsContextSamePriority(new Runnable()
        {
            public void run()
            {
                cc[0] = ccMgr.getCurrentContext();
                ccPriority[0] = Thread.currentThread().getPriority();
            }
        });
        */

        assertSame("Expected runAsContext() to cause ccMgr.getCurrentContext() to return acc", acc, cc[0]);
        assertEquals("Expected runAsContext() to not change priority", priority, ccPriority[0]);
        assertEquals("Expected runAsContext() to (not need to) restore priority", priority, Thread.currentThread()
                .getPriority());
    }

    protected ExecQueue eq;

    protected AbstractCallerContext acc;

    protected CCMgr ccMgr;

    protected int restorePriority;

    protected abstract AbstractCallerContext createAbstractCallerContext();

    /*
     * { eq = new ExecQueue(); return new AbstractCallerContext(ccMgr,
     * ccMgr.getThreadPool(), eq) { public String getProperty(String key, String
     * val) { return val; } public Properties getProperties(Properties base) {
     * return base; } public boolean isAlive() { return true; } public void
     * checkAlive() { } public Object get(Object key) { return null; } }; }
     */

    protected abstract void destroyAbstractCallerContext(AbstractCallerContext acc);

    protected void setUp() throws Exception
    {
        super.setUp();

        restorePriority = Thread.currentThread().getPriority();

        ccMgr = new CCMgr();

        acc = createAbstractCallerContext();
    }

    protected void tearDown() throws Exception
    {
        if (acc != null) destroyAbstractCallerContext(acc);

        ccMgr.destroy();

        Thread.currentThread().setPriority(restorePriority);

        super.tearDown();
    }

    public AbstractCallerContextTest(String test)
    {
        super(test);
    }

    // Should have a suite similar to this to test CallerContext interface...
    /*
     * public static Test suite(String[] tests) { if (tests == null ||
     * tests.length == 0) return suite(); else { TestSuite suite = new
     * TestSuite(AbstractCallerContextTest.class.getName()); for(int i = 0; i <
     * tests.length; ++i) suite.addTest(new
     * AbstractCallerContextTest(tests[i])); return suite; } }
     * 
     * public static Test suite() { TestSuite suite = new
     * TestSuite(AbstractCallerContextTest.class); ImplFactory factory = new
     * ContextImplFactory() { public Object createImplObject() { return
     * AbstractCallerContextTest.createImplObject(); } public void
     * makeDestroyed(CallerContext ctx) throws Exception {
     * doDestroyed((AbstractCallerContext)ctx); } public void
     * makePaused(CallerContext ctx) throws Exception {
     * doPaused((AbstractCallerContext)ctx); } public void
     * makeResumed(CallerContext ctx) throws Exception {
     * doResumed((AbstractCallerContext)ctx); } }; InterfaceTestSuite ctxSuite =
     * CallerContextTest.isuite();
     * 
     * ctxSuite.addFactory(factory); suite.addTest(ctxSuite);
     * 
     * return suite; }
     */
}
