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

import org.cablelabs.impl.manager.CallerContext.Multicaster;
import org.cablelabs.impl.util.TaskQueue;
import org.cablelabs.test.TestUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests CallerContext.Multicaster. Does not use the CallerContextTest interface
 * test.
 * 
 * @author Aaron Kamienski
 */
public class CallerContextMulticasterTest extends TestCase
{
    /**
     * Verifies that there are no public constructors.
     */
    public void testNoPublicConstructors()
    {
        TestUtils.testNoPublicConstructors(Multicaster.class);
    }

    /**
     * Verifies that non-runInContext() operations are unimplemented.
     */
    public void testUnimplemented()
    {
        CallerContext mc = Multicaster.add(new DummyContext(), new DummyContext());

        try
        {
            mc.addCallbackData(new CallbackData.SimpleData(null), "");
            fail("Expected addCallbackData to fail");
        }
        catch (Exception e)
        {
        }

        try
        {
            mc.removeCallbackData("");
            fail("Expected removeCallbackData to fail");
        }
        catch (Exception e)
        {
        }

        try
        {
            mc.getCallbackData("");
            fail("Expected getCallbackData to fail");
        }
        catch (Exception e)
        {
        }

        try
        {
            mc.isAlive();
            fail("Expected isAlive to fail");
        }
        catch (Exception e)
        {
        }

        try
        {
            mc.checkAlive();
            fail("Expected checkAlive to fail");
        }
        catch (SecurityException e)
        {
            fail("Did not expect a SecurityException from checkAlive");
        }
        catch (Exception e)
        {
        }
    }

    /**
     * Tests add() for thread safety. Ensure that add always returns a different
     * multicaster (i.e., it doesn't modify one).
     */
    public void testAdd() throws Exception
    {
        DummyContext cc[] = { new DummyContext(), new DummyContext(), new DummyContext() };
        CallerContext mc = Multicaster.add(cc[0], cc[1]);
        CallerContext mc2;
        DummyContext add[] = { new DummyContext(), new DummyContext(), };

        assertFalse("Should not be the same multicasters", Multicaster.add(mc, add[0]) == Multicaster.add(mc, add[0]));
        assertFalse("Should not be the same multicasters", Multicaster.add(add[0], mc) == Multicaster.add(mc, add[0]));

        // add different contexts
        mc2 = Multicaster.add(mc, add[1]);
        mc = Multicaster.add(mc, add[0]);

        // add same contexts
        mc = Multicaster.add(cc[2], mc);
        mc2 = Multicaster.add(cc[2], mc2);

        mc.runInContext(RUNNABLE);
        mc2.runInContext(RUNNABLE);
        for (int i = 0; i < cc.length; ++i)
            assertEquals("Expected contexts to be called twice", 2, cc[i].calls);
        for (int i = 0; i < add.length; ++i)
            assertEquals("Expected contexts to be called once", 1, add[i].calls);
    }

    /**
     * Tests add(). Makes sure that single/same context is returned.
     */
    public void testAddSingle()
    {
        CallerContext cc = new DummyContext();

        assertNull("Expected add to return null", Multicaster.add(null, null));

        assertSame("Expected the same single CallerContext to be returned by add", cc, Multicaster.add(cc, null));
        assertSame("Expected the same single CallerContext to be returned by add", cc, Multicaster.add(null, cc));
    }

    /**
     * Tests add(). Makes sure that a new multicaster is returned.
     */
    public void testAddMulticaster() throws Exception
    {
        DummyContext cc[] = { new DummyContext(), new DummyContext(), new DummyContext(), new DummyContext(),
                new DummyContext(), new DummyContext(), };
        CallerContext mc = cc[0];

        Runnable r = RUNNABLE;
        for (int i = 1; i < cc.length; ++i)
        {
            for (int j = 0; j < cc.length; ++j)
                cc[j].reset();

            CallerContext mc2 = Multicaster.add(mc, cc[i]);

            assertTrue("Expected add to return a Multicaster", mc2 instanceof Multicaster);
            assertTrue("Expected add to create a new Multicaster", mc2 != mc);
            assertTrue("Expected add to create a new Multicaster", mc2 != cc[i]);

            // Run in context
            mc2.runInContext(r);

            for (int j = 0; j <= i; ++j)
                assertEquals("Expected each context to be called once", 1, cc[j].calls);
            for (int j = i + 1; j < cc.length; ++j)
                assertEquals("Expected unadded contexts not to be called", 0, cc[j].calls);

            mc = mc2;
        }

        // Do again, but this time swapping argument order
        mc = cc[0];
        for (int i = 1; i < cc.length; ++i)
        {
            for (int j = 0; j < cc.length; ++j)
                cc[j].reset();

            CallerContext mc2 = Multicaster.add(cc[i], mc);

            assertTrue("Expected add to return a Multicaster", mc2 instanceof Multicaster);
            assertTrue("Expected add to create a new Multicaster", mc2 != mc);
            assertTrue("Expected add to create a new Multicaster", mc2 != cc[i]);

            // Run in context
            mc2.runInContext(r);

            for (int j = 0; j <= i; ++j)
                assertEquals("Expected each context to be called once", 1, cc[j].calls);
            for (int j = i + 1; j < cc.length; ++j)
                assertEquals("Expected unadded contexts not to be called", 0, cc[j].calls);

            mc = mc2;
        }
    }

    /**
     * Tests add(). Makes sure that each CallerContext is present only once.
     */
    public void testAddMultiple() throws Exception
    {
        DummyContext cc = new DummyContext();

        assertSame("Expect single to be returned if specified twice", cc, Multicaster.add(cc, cc));

        Runnable r = RUNNABLE;
        CallerContext mc = cc;
        for (int i = 0; i < 10; ++i)
        {
            // Try to add same to multicaster
            mc = Multicaster.add(cc, mc);

            // Verify cc is called only once
            cc.reset();
            mc.runInContext(r);
            assertEquals("Expected context to be called once", 1, cc.calls);

            // Add another to add to the searching...
            mc = Multicaster.add(new DummyContext(), mc);
        }
    }

    /**
     * Tests remove() for thread safety. Ensure that remove always returns a
     * different multicaster (i.e., it doesn't modify one).
     */
    public void testRemove()
    {
        DummyContext cc[] = { new DummyContext(), new DummyContext(), new DummyContext(), new DummyContext(), };
        DummyContext remove[] = { new DummyContext(), new DummyContext() };
        CallerContext mc = Multicaster.add(cc[0], cc[1]);
        CallerContext mc1, mc2;
        for (int i = 0; i < remove.length; ++i)
            mc = Multicaster.add(remove[i], mc);
        for (int i = 2; i < cc.length; ++i)
            mc = Multicaster.add(mc, cc[i]);

        assertFalse("remove should not return the same multicaster",
                Multicaster.remove(mc, cc[0]) == Multicaster.remove(cc[0], mc));
        assertFalse("remove should not return the same multicaster",
                Multicaster.remove(mc, remove[0]) == Multicaster.remove(remove[0], mc));

        // Remove same, generating two new multicasters
        mc1 = Multicaster.remove(mc, remove[0]);
        mc2 = Multicaster.remove(mc, remove[0]);

        mc.runInContext(RUNNABLE);
        mc1.runInContext(RUNNABLE);
        mc2.runInContext(RUNNABLE);

        for (int i = 0; i < cc.length; ++i)
        {
            assertEquals("Expected to be called 3 times", 3, cc[i].calls);
            cc[i].reset();
        }
        assertEquals("Expected to be called 1 times", 1, remove[0].calls);
        assertEquals("Expected to be called 3 times", 3, remove[1].calls);
        remove[0].reset();
        remove[1].reset();

        // Remove different, generating two new multicasters
        mc1 = Multicaster.remove(mc, remove[0]);
        mc2 = Multicaster.remove(mc, remove[1]);

        mc.runInContext(RUNNABLE);
        mc1.runInContext(RUNNABLE);
        mc2.runInContext(RUNNABLE);

        for (int i = 0; i < cc.length; ++i)
        {
            assertEquals("Expected to be called 3 times", 3, cc[i].calls);
            cc[i].reset();
        }
        for (int i = 0; i < remove.length; ++i)
        {
            assertEquals("Expected to be called 2 times", 2, remove[i].calls);
            remove[i].reset();
        }
    }

    /**
     * Tests remove() if one to remove is unfound.
     */
    public void testRemoveUnfound()
    {
        DummyContext cc[] = { new DummyContext(), new DummyContext(), new DummyContext(), new DummyContext(),
                new DummyContext(), new DummyContext(), null };
        CallerContext mc = null;

        for (int i = 0; i < cc.length; ++i)
        {
            CallerContext mc2 = Multicaster.remove(mc, new DummyContext());

            assertSame("Removing un-added context should return original", mc, mc2);

            mc = Multicaster.add(mc, cc[i]);
        }
    }

    /**
     * Tests runInContext().
     */
    public void testRunInContext() throws Exception
    {
        DummyContext cc[] = { new DummyContext(), new DummyContext(), new DummyContext(), new DummyContext(),
                new DummyContext(), new DummyContext(), new DummyContext(), };
        CallerContext mc = null;

        for (int i = 0; i < cc.length; ++i)
            mc = Multicaster.add(cc[i], mc);

        mc.runInContext(RUNNABLE);
        for (int i = 0; i < cc.length; ++i)
        {
            assertTrue("Expected to be called", cc[i].called);
            assertEquals("Expected to be called once", 1, cc[i].calls);
            assertFalse("Expected no other calls to be made", cc[i].calledOther);
            assertFalse("Expected no other calls to be made", cc[i].calledSync);
            assertFalse("Expected no other calls to be made", cc[i].calledAsync);
            assertEquals("Expected given Runnable to be used", RUNNABLE, cc[i].ran);

            cc[i].reset();
        }

        mc.runInContextSync(RUNNABLE);
        for (int i = 0; i < cc.length; ++i)
        {
            assertTrue("Expected to be called", cc[i].calledSync);
            assertEquals("Expected to be called once", 1, cc[i].calls);
            assertFalse("Expected no other calls to be made", cc[i].calledOther);
            assertFalse("Expected no other calls to be made", cc[i].calledAsync);
            assertFalse("Expected no other calls to be made", cc[i].called);
            assertEquals("Expected given Runnable to be used", RUNNABLE, cc[i].ran);

            cc[i].reset();
        }

        mc.runInContextAsync(RUNNABLE);
        for (int i = 0; i < cc.length; ++i)
        {
            assertTrue("Expected to be called", cc[i].calledAsync);
            assertEquals("Expected to be called once", 1, cc[i].calls);
            assertFalse("Expected no other calls to be made", cc[i].calledOther);
            assertFalse("Expected no other calls to be made", cc[i].calledSync);
            assertFalse("Expected no other calls to be made", cc[i].called);
            assertEquals("Expected given Runnable to be used", RUNNABLE, cc[i].ran);

            cc[i].reset();
        }

    }

    private static final Runnable RUNNABLE = new Runnable()
    {
        public void run()
        {
        }
    };

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
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

    public static Test suite()
    {
        TestSuite suite = new TestSuite(CallerContextMulticasterTest.class);
        return suite;
    }

    public CallerContextMulticasterTest(String name)
    {
        super(name);
    }

    public static class DummyContext implements CallerContext
    {
        public Runnable ran;

        public int calls;

        public boolean called;

        public boolean calledSync;

        public boolean calledAsync;

        public boolean calledOther;

        public void reset()
        {
            calls = 0;
            called = false;
            calledSync = false;
            calledOther = false;
            calledAsync = false;
            ran = null;
        }

        public void addCallbackData(CallbackData data, Object key)
        {
            calledOther = true;
        }

        public void removeCallbackData(Object key)
        {
            calledOther = true;
        }

        public CallbackData getCallbackData(Object key)
        {
            calledOther = true;
            return null;
        }

        public boolean isAlive()
        {
            calledOther = true;
            return true;
        }

        public void checkAlive()
        {
            calledOther = true;
        }

        public boolean isActive()
        {
            calledOther = true;
            return true;
        }

        public void runInContext(Runnable run)
        {
            ++calls;
            called = true;
            ran = run;
        }

        public void runInContextSync(Runnable run)
        {
            ++calls;
            calledSync = true;
            ran = run;
        }

        public void runInContextAsync(Runnable run)
        {
            ++calls;
            calledAsync = true;
            ran = run;
        }

        public void runAsContext(Runnable run)
        {
            throw new RuntimeException("Should not be used");
        }

        public Object get(Object key)
        {
            throw new UnsupportedOperationException();
        }

        public TaskQueue createTaskQueue()
        {
            throw new UnsupportedOperationException();
        }

        public void runInContextAWT(Runnable run) throws SecurityException,
                IllegalStateException
        {
        }
    }
}
