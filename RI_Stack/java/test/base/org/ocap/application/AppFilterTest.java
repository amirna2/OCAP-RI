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

package org.ocap.application;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import junit.framework.*;
import org.dvb.application.*;
import org.ocap.application.*;

/**
 * Tests the AppFilter class.
 */
public class AppFilterTest extends TestCase
{
    /**
     * Tests the constructor.
     */
    public void testConstructor()
    {
        AppPattern pat[] = { new AppPattern("10", AppPattern.ALLOW, 0), new AppPattern("10-12", AppPattern.DENY, 0),
                new AppPattern("9-11", AppPattern.ASK, 0), new AppPattern("13:12-14", AppPattern.ALLOW, 0), };
        checkConstructor("AppFilter(AppPattern[])", new AppFilter(pat), pat);
        checkConstructor("AppFilter()", new AppFilter(), null);
    }

    /**
     * Verify construction.
     * 
     * @param desc
     *            description used to identifier the constructor
     * @param filter
     *            the constructed filter object
     * @param pat
     *            the expected set of patterns
     */
    private void checkConstructor(String desc, AppFilter filter, AppPattern[] pat)
    {
        int checked[] = (pat == null) ? null : new int[pat.length];

        int size = 0;
        for (Enumeration e = filter.getAppPatterns(); e.hasMoreElements();)
        {
            ++size;
            AppPattern p = (AppPattern) e.nextElement();
            if (pat != null) for (int i = 0; i < pat.length; ++i)
                if (pat[i] == p)
                {
                    ++checked[i];
                    break;
                }
        }
        if (pat != null)
            assertEquals(
                    "The number of elements returned by the Enumeration should be the number put in the constructor",
                    pat.length, size);
        else
            assertEquals("No elements should've been returned by the Enumeration", 0, size);
        if (pat != null) for (int i = 0; i < checked.length; ++i)
        {
            assertEquals("Each pattern set in the constructor should be returned in the Enumeration", 1, checked[i]);
        }
    }

    /**
     * Verify the enumeration of patterns.
     * 
     * @param msg
     *            additional explanatory message, or null
     * @param e
     *            the enumeration returned from the filter
     * @param patterns
     *            the set of expected patterns
     */
    private void verifyEnumeration(String msg, Enumeration e, Hashtable patterns)
    {
        if (msg == null)
            msg = "";
        else if (!msg.endsWith(" ") && !msg.endsWith(":")) msg = msg + ": ";
        try
        {
            assertNotNull(msg + "Enumeration should never be null", e);
            if (patterns == null || patterns.size() == 0)
            {
                assertFalse(msg + "Enumeration should be empty", e.hasMoreElements());
                try
                {
                    e.nextElement();
                    fail(msg + "Enumeration.nextElement() should cause exception if empty");
                }
                catch (NoSuchElementException nsee)
                {
                }
            }
            else
            {
                assertTrue(msg + "Enumeration should not be empty", e.hasMoreElements());

                while (e.hasMoreElements())
                {
                    Object obj = e.nextElement();
                    assertNotNull(msg + "Enumeration elements should not be null", obj);
                    assertTrue(msg + "Enumeration elements should be AppPatterns", obj instanceof AppPattern);
                    Count count = (Count) patterns.get(obj);
                    assertNotNull(msg + "Enumeration contains extra element " + obj, count);
                    ++count.count;
                    assertEquals(msg + "Element returned more than once " + obj, 1, count.count);
                }
                for (Enumeration all = patterns.elements(); all.hasMoreElements();)
                {
                    Count count = (Count) all.nextElement();
                    assertEquals(msg + "Each element should be found once and only once", 1, count.count);
                }
            }
        }
        finally
        {
            // Reset counters
            if (patterns != null) for (Enumeration counters = patterns.elements(); counters.hasMoreElements();)
            {
                ((Count) counters.nextElement()).count = 0;
            }
        }
    }

    /**
     * Tests getAppPatterns().
     */
    public void testGetAppPatterns() throws Exception
    {
        verifyEnumeration(null, filter.getAppPatterns(), null);

        AppPattern pat[] = { new AppPattern("a:b", AppPattern.ALLOW, 20), new AppPattern("a:b", AppPattern.DENY, 10),
                new AppPattern("a:b", AppPattern.ASK, 1), // not tested
                new AppPattern("a:b", AppPattern.ASK, 30), };
        Hashtable patterns = new Hashtable();
        for (int i = 0; i < pat.length; ++i)
        {
            // Add pattern
            filter.add(pat[i]);
            patterns.put(pat[i], new Count());

            // Verify enumeration
            verifyEnumeration("add-" + i, filter.getAppPatterns(), patterns);
        }
        for (int i = 0; i < pat.length; ++i)
        {
            // Verify enumeration
            verifyEnumeration("pre-remove-" + i, filter.getAppPatterns(), patterns);

            // Remove a pattern
            patterns.remove(pat[i]);
            filter.remove(pat[i]);
        }

        verifyEnumeration("remove-*", filter.getAppPatterns(), null);
    }

    /**
     * Tests getAppPatterns() -- should not return expired entries.
     */
    public void testGetAppPatterns_expiration() throws Exception
    {
        verifyEnumeration(null, filter.getAppPatterns(), null);

        AppPattern pat[] = new AppPattern[4];

        final long INTERVAL = 6000L;
        final long now = System.currentTimeMillis();
        Hashtable patterns = new Hashtable();

        // Add patterns that will expire in the future
        // Ensure that they are returned before they expire
        for (int i = 0; i < pat.length; ++i)
        {
            long exp = now + INTERVAL * (i + 1);
            pat[i] = new AppPattern("a:" + (1 + i), AppPattern.ALLOW, 100, new Date(exp), null);

            filter.add(pat[i]);
            patterns.put(pat[i], new Count());

            verifyEnumeration("add-" + i, filter.getAppPatterns(), patterns);
        }

        // Let them expire
        // Ensure that they are not returned after they expire
        for (int i = 0; i < pat.length; ++i)
        {
            // Forget next to expire
            patterns.remove(pat[i]);

            long waitTime = now + INTERVAL * (i + 1) - System.currentTimeMillis() + 100;
            if (waitTime > 0L)
            {
                Thread.sleep(waitTime);
                Date expTime = pat[i].getExpirationTime();
                Date nowTime = new Date();
                assertTrue("Internal error - expected it to be expired: " + expTime + " < " + nowTime,
                        expTime.before(nowTime));

                verifyEnumeration("exp-" + i, filter.getAppPatterns(), patterns);
            }
            else
            {
                // Waited too long! Skip this one!
                fail("Waited too long to catch " + i);
            }
        }
        // Finally, all should've expired
        verifyEnumeration("exp-all", filter.getAppPatterns(), null);
    }

    /**
     * Tests accept(). Verify action is taken.
     */
    public void testAccept_action() throws Exception
    {
        AppID id = new AppID(0xa, 0xb);
        AppPattern p;
        Handler handler = new Handler();
        filter.setAskHandler(handler = new Handler());

        assertTrue("Should return true if no patterns", filter.accept(id));

        filter.add(p = new AppPattern("a:b", AppPattern.ALLOW, 0));
        assertTrue("Should accept given ALLOW", filter.accept(id));
        assertFalse("Handler shouldn't be called", handler.called);
        filter.remove(p);

        filter.add(p = new AppPattern("a:b", AppPattern.DENY, 0));
        assertFalse("Should not accept given DENY", filter.accept(id));
        assertFalse("Handler shouldn't be called", handler.called);
        filter.remove(p);

        filter.add(p = new AppPattern("a:b", AppPattern.ASK, 0));
        assertFalse("Should not accept given ASK", filter.accept(id));
        assertTrue("Handler should be called", handler.called);
        handler.called = false;
        handler.doAccept = true;
        assertTrue("Should accept given ASK", filter.accept(id));
        assertTrue("Handler should be called", handler.called);
        filter.setAskHandler(null);
        handler.called = false;
        handler.doAccept = false;
        assertTrue("Should accept given ASK and no handler", filter.accept(id));
        assertFalse("Handler should NOT be called", handler.called);
        filter.remove(p);
    }

    /**
     * Tests accept(). Verify priority is respected.
     */
    public void testAccept_priority() throws Exception
    {
        AppID id = new AppID(0xa, 0xb);
        AppPattern pat[] = { new AppPattern("a:b", AppPattern.ALLOW, 20), new AppPattern("a:b", AppPattern.DENY, 10),
                new AppPattern("a:b", AppPattern.ASK, 1), // not tested
                new AppPattern("a:b", AppPattern.ASK, 30), };
        for (int i = 0; i < pat.length; ++i)
            filter.add(pat[i]);

        Handler handler;
        filter.setAskHandler(handler = new Handler(true));
        assertTrue("Accept should return what the handler returned", filter.accept(id));
        assertTrue("The highest priority pattern should've been consulted", handler.called);

        filter.setAskHandler(handler = new Handler(false));
        assertFalse("Accept should return what the handler returned", filter.accept(id));
        assertTrue("The highest priority pattern should've been consulted", handler.called);

        // Remove the highest priority one
        assertTrue("Expected pattern to be removed", filter.remove(pat[pat.length - 1]));
        assertTrue("Accept should return the *new* highest priority pattern action", filter.accept(id));

        assertTrue("Expected pattern to be removed", filter.remove(pat[0]));
        assertFalse("Accept should return the *newer* highest priority pattern action", filter.accept(id));
    }

    /**
     * Tests accept(). Verify pattern parsing.
     */
    public void testAccept_pattern() throws Exception
    {
        AppID id = new AppID(0x10, 0x20);
        AppID other = new AppID(0x11, 0x21);
        AppPattern p;

        assertTrue("Given no patterns, all should be accepted", filter.accept(id));

        filter.add(p = new AppPattern("10", AppPattern.DENY, 0));
        assertTrue("An unfound pattern should be accepted", filter.accept(other));
        assertFalse("Org ID pattern should be found", filter.accept(id));
        filter.remove(p);

        filter.add(p = new AppPattern("10-11", AppPattern.DENY, 0));
        assertFalse("Org ID range pattern should be found", filter.accept(other));
        assertFalse("Org ID range pattern should be found", filter.accept(id));
        filter.remove(p);

        filter.add(p = new AppPattern("10:20", AppPattern.DENY, 0));
        assertTrue("An unfound pattern should be accepted", filter.accept(other));
        assertFalse("An exact match should be found", filter.accept(id));
        filter.remove(p);

        filter.add(p = new AppPattern("10-11:20", AppPattern.DENY, 0));
        assertTrue("And unfound pattern should be accepted", filter.accept(other));
        assertFalse("Org ID range w/ exact app id should be found", filter.accept(id));
        filter.remove(p);

        filter.add(p = new AppPattern("10-11:20-21", AppPattern.DENY, 0));
        assertFalse("Org ID range/App ID range pattern should be found", filter.accept(other));
        assertFalse("Org ID range/App ID range pattern should be found", filter.accept(id));
        filter.remove(p);

        // Multiple patterns together
        AppPattern p2;
        filter.add(p = new AppPattern("10:20-21", AppPattern.DENY, 0));
        filter.add(p2 = new AppPattern("10-11:21", AppPattern.DENY, 0));
        assertFalse("Org ID range/App ID range pattern should be found", filter.accept(other));
        assertFalse("Org ID range/App ID range pattern should be found", filter.accept(id));
        filter.remove(p);
        filter.remove(p2);
    }

    /**
     * Tests accept(). Verify expiration date.
     */
    public void testAccept_expiration() throws Exception
    {
        AppID id = new AppID(0x10, 0x20);
        AppPattern p;
        final int diff = 72 * 3600 * 1000;
        Date past = new Date(System.currentTimeMillis() - diff);
        Date future = new Date(System.currentTimeMillis() + diff);

        filter.add(p = new AppPattern("10:20", AppPattern.DENY, 0, past, null));
        assertTrue("An expired pattern should not be used", filter.accept(id));
        filter.remove(p);

        filter.add(p = new AppPattern("10:20", AppPattern.DENY, 0, future, null));
        assertFalse("An as-yet-unexpired pattern should be used", filter.accept(id));
        filter.remove(p);
    }

    /**
     * Tests add()/remove(). When removing, must use "equals" method to find one
     * to remove. When removing, return whether one was removed. When adding,
     * replace one that is equals.
     */
    public void testAddRemove() throws Exception
    {
        // Similar to testEnumeration, except we use some duplicates and
        // multiples
        AppPattern pat[] = {
                new AppPattern("10", AppPattern.ALLOW, 0, new Date(System.currentTimeMillis() + 3600), "null"),
                new AppPattern("10-12", AppPattern.DENY, 10),
                new AppPattern("9-11", AppPattern.ASK, 30),
                new AppPattern("13:12-14", AppPattern.ALLOW, 0),
                new AppPattern("10-12", AppPattern.DENY, 10, new Date(System.currentTimeMillis() + 3600), null),
                new AppPattern("10", AppPattern.ALLOW, 0, new Date(System.currentTimeMillis() + 72 * 3600 * 1000), null),
                new AppPattern("10-12", AppPattern.DENY, 100), new AppPattern("9-11", AppPattern.ASK, 0), };

        // Depends on AppPattern.equals/hashCode being implemented correctly.
        Hashtable patterns = new Hashtable();
        // Add patterns
        for (int i = 0; i < pat.length; ++i)
        {
            // Add pattern
            filter.add(pat[i]);
            boolean already = patterns.get(pat[i]) != null;
            int oldSize = patterns.size();
            patterns.put(pat[i], new Count());
            if (already)
                assertTrue("Expected size to go unchanged when replacing", oldSize == patterns.size());
            else
                assertTrue("Expected size to increase by one when adding", oldSize + 1 == patterns.size());

            // Verify that pattern was added, replacing existing
            verifyEnumeration("add-" + i, filter.getAppPatterns(), patterns);
        }
        // Remove patterns
        for (int i = 0; i < pat.length; ++i)
        {
            // Verify enumeration
            verifyEnumeration("pre-remove-" + i, filter.getAppPatterns(), patterns);

            // Remove a pattern
            boolean inthere = patterns.get(pat[i]) != null;
            int oldSize = patterns.size();
            patterns.remove(pat[i]);
            boolean removed = filter.remove(pat[i]);
            assertTrue("Remove should return whether the pattern was removed or not", removed == inthere);
            if (!inthere)
                assertTrue("Expected size to go unchanged when not removing", oldSize == patterns.size());
            else
                assertTrue("Expected size to decrease by one when adding", oldSize - 1 == patterns.size());
        }

        verifyEnumeration("all-removed", filter.getAppPatterns(), null);
    }

    /**
     * Tests setAskHandler().
     */
    public void testSetAskHandler() throws Exception
    {
        Handler h1 = new Handler(), h2 = new Handler();
        AppID id = new AppID(0x10, 0x20);

        filter.add(new AppPattern("10:20", AppPattern.ASK, 0));
        filter.setAskHandler(h1);
        filter.accept(id);
        assertTrue("Handler should be consulted", h1.called);
        h1.called = false;

        filter.setAskHandler(h2);
        filter.accept(id);
        assertTrue("Replacement handler should be consulted", h2.called);
        assertFalse("Replaced handler should NOT be consulted", h1.called);
        h2.called = false;

        assertTrue("Handler should not be consulted for non-matches", filter.accept(new AppID(10, 21)));
        assertFalse("Handler should not be consulted for non-matches", h2.called);
        assertTrue("Handler should not be consulted for non-matches", filter.accept(new AppID(11, 20)));
        assertFalse("Handler should not be consulted for non-matches", h2.called);
    }

    /**
     * A mutable form of <code>Integer</code>.
     */
    static class Count
    {
        int count;
    };

    /**
     * Test handler which records whether it was called and can be made to
     * return a specific value from accept().
     */
    static class Handler implements AppFilterHandler
    {
        public boolean called;

        public boolean doAccept;

        public Handler()
        {
        }

        public Handler(boolean accept)
        {
            doAccept = accept;
        }

        public boolean accept(AppID id, AppPattern pat)
        {
            called = true;
            return doAccept;
        }
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
        TestSuite suite = new TestSuite(AppFilterTest.class);
        return suite;
    }

    public AppFilterTest(String name)
    {
        super(name);
    }

    protected AppFilter filter;

    protected void setUp() throws Exception
    {
        super.setUp();
        filter = new AppFilter();
    }

    protected void tearDown() throws Exception
    {
        filter = null;
        super.tearDown();
    }

}
