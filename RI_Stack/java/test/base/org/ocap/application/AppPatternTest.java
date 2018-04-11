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

import junit.framework.*;
import java.util.*;

/**
 * Tests the AppPattern class.
 * <p>
 * idPattern specfies an AppID group with a String: a pair of ranges for
 * Organization IDs and Application IDs. The syntax is:
 * 
 * <pre>
 * &quot;oid1[-oid2][:aid1[-aid2]]&quot;
 * </pre>
 * 
 * <ul>
 * <li>oid1 and oid2 specify a range of Organization IDs inclusive. Each of them
 * must be a 32-bit value.
 * <li>aid1 and aid2 specify a range of Application IDs inclusive. Each of them
 * must be a 16-bit value.
 * <li>oid2 and aid2 must be greater than oid1 and aid1, respectively.
 * <li>The encoding of these IDs follows 14.5 Text encoding of application
 * identifiers of DVB-MHP 1.0.2 [11]; hexadecimal, lower case, no leading zeros.
 * (Also doesn't indicate that there is a leading "0x")
 * <li>Symbols in brackets are optional.
 * <li>When oid2 is omitted, only oid1 is in the range.
 * <li>When aid2 is omitted, only aid1 is in the range.
 * <li>When both aid1 and aid2 are omitted, all Application IDs are in the
 * range.
 */
public class AppPatternTest extends TestCase
{
    /**
     * Tests the (String, int, int) constructor. Verifies that it recognizes bad
     * input.
     */
    private void doTestConstructor1(String pattern, int action, int priority)
    {
        try
        {
            new AppPattern(pattern, action, priority);
            fail("Expected IllegalArgumentException given: " + "(" + pattern + "; " + action + ", " + priority + ")");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    /**
     * Patterns that should result in an IllegalArgumentException on an
     * AppPattern constructor.
     */
    static final String[] badPatterns = { null, "hello", "$abc", "-1", "0000", "FFF-1", "100000000",
            "FFFFFFFF-100000000", ":", ":22", "1234-1235-a", "1234:1234:1235", "a:10000", "a:FFFF-10000",
            // good patterns, with "0x" prefixes
            "0x1", "0x7ff00ff0", "0x90-10000", "0x1:0xa", "0x1:0xa95-7fff", "0x90-0x10000:0xa",
            "0x7ff-0x800:0xa12-0x1000", };

    /**
     * Action values that should result in an IllegalArgumentException on an
     * AppPattern constructor.
     */
    static final int[] badActions = { 0, 4, 10, -1 };

    /**
     * Priority values that should result in an IllegalArgumentException on an
     * AppPattern constructor.
     */
    static final int[] badPriorities = { 256, -1, 0xFFFFFFFF, -255 };

    /**
     * Patterns that should result is good AppPattern construction.
     */
    static final String[] goodPatterns = { "1", "7ff00ff0", "90-10000", "1:a", "1:a95-7fff", "90-10000:a",
            "7ff-800:a12-1000", "1-FFFFFFFF", "1-FFFFFFFF:FFF0" };

    /**
     * Action values that should result is good AppPattern construction.
     */
    static final int[] goodActions = { AppPattern.ALLOW, AppPattern.DENY, AppPattern.ASK };

    /**
     * Priority values that should result is good AppPattern construction.
     */
    static final int[] goodPriorities = { 0, 1, 10, 13, 127, 128, 194, 255, };

    /**
     * Tests constructors.
     */
    public void testConstructor() throws Exception
    {
        // Bad patterns
        for (int i = 0; i < badPatterns.length; ++i)
            doTestConstructor1(badPatterns[i], goodActions[i % goodActions.length], goodPriorities[i
                    % goodPriorities.length]);

        // Bad actions
        for (int i = 0; i < badActions.length; ++i)
            doTestConstructor1(goodPatterns[i % goodPatterns.length], badActions[i], goodPriorities[i
                    % goodPriorities.length]);

        // Bad priorities
        for (int i = 0; i < badPriorities.length; ++i)
            doTestConstructor1(goodPatterns[i % goodPatterns.length], goodActions[i % goodActions.length],
                    badPriorities[i]);
    }

    /**
     * Tests getAppIDPattern().
     */
    public void testGetAppIDPattern() throws Exception
    {
        for (int i = 0; i < goodPatterns.length; ++i)
        {
            AppPattern pat = new AppPattern(goodPatterns[i], goodActions[0], goodPriorities[0]);
            assertEquals("AppIDPattern should be same passed to constructor", goodPatterns[i], pat.getAppIDPattern());
        }
    }

    /**
     * Tests getAction().
     */
    public void testGetAction() throws Exception
    {
        for (int i = 0; i < goodActions.length; ++i)
        {
            AppPattern pat = new AppPattern(goodPatterns[0], goodActions[i], goodPriorities[0]);
            assertEquals("Action should be same as passed to constructor", goodActions[i], pat.getAction());
        }
    }

    /**
     * Tests getPriority().
     */
    public void testGetPriority() throws Exception
    {
        for (int i = 0; i < goodActions.length; ++i)
        {
            AppPattern pat = new AppPattern(goodPatterns[0], goodActions[0], goodPriorities[i]);
            assertEquals("Priority should be same as passed to constructor", goodPriorities[i], pat.getPriority());
        }
    }

    /**
     * Tests getExpirationTime().
     */
    public void testGetExpirationTime() throws Exception
    {
        Date[] dates = { new Date(System.currentTimeMillis() - 72 * 3600 * 1000), new Date(),
                new Date(System.currentTimeMillis() + 72 * 3600 * 1000), };
        for (int i = 0; i < dates.length; ++i)
        {
            AppPattern pat = new AppPattern(goodPatterns[1], goodActions[1], goodPriorities[1], dates[i], null);
            assertEquals("ExpirationTime should be same as passed to constructor", dates[i], pat.getExpirationTime());
        }
    }

    /**
     * Tests getPrivateInfo().
     */
    public void testGetPrivateInfo() throws Exception
    {
        Object[] data = { null, "hello", };
        for (int i = 0; i < data.length; ++i)
        {
            AppPattern pat = new AppPattern(goodPatterns[1], goodActions[1], goodPriorities[1], new Date(), data[i]);
            assertSame("PrivateInfo should be same as passed to constructor", data[i], pat.getPrivateInfo());
        }
    }

    /**
     * Tests equals(). Should not look at expiration time or private info.
     */
    public void testEquals() throws Exception
    {
        final int MAX = Math.max(goodPatterns.length, Math.max(goodActions.length, goodPriorities.length));
        for (int i = 0; i < MAX; ++i)
        {
            assertEquals("Two equivalent patterns should compare the same [" + i + "]", new AppPattern(goodPatterns[i
                    % goodPatterns.length], goodActions[i % goodActions.length], goodPriorities[i
                    % goodPriorities.length]), new AppPattern(goodPatterns[i % goodPatterns.length], goodActions[i
                    % goodActions.length], goodPriorities[i % goodPriorities.length]));
            assertEquals("Two equivalent patterns should compare the same, " + "irrespective of constructor [" + i
                    + "]", new AppPattern(goodPatterns[i % goodPatterns.length], goodActions[i % goodActions.length],
                    goodPriorities[i % goodPriorities.length], new Date(), "data"), new AppPattern(goodPatterns[i
                    % goodPatterns.length], goodActions[i % goodActions.length], goodPriorities[i
                    % goodPriorities.length]));
            assertEquals("Two equivalent patterns should compare the same, " + "irrespective of date/info [" + i + "]",
                    new AppPattern(goodPatterns[i % goodPatterns.length], goodActions[i % goodActions.length],
                            goodPriorities[i % goodPriorities.length], new Date(), "data"), new AppPattern(
                            goodPatterns[i % goodPatterns.length], goodActions[i % goodActions.length],
                            goodPriorities[i % goodPriorities.length], new Date(
                                    System.currentTimeMillis() + 3600 * 72 * 1000), null));
        }

        // Should not be equal to other/null objects
        assertTrue("Should not be equals to 'other' objects", !(new AppPattern(goodPatterns[0], goodActions[0],
                goodPriorities[0])).equals("Hello, world"));
        try
        {
            assertTrue("Should not be equals to null objects", !(new AppPattern(goodPatterns[0], goodActions[0],
                    goodPriorities[0])).equals(null));
        }
        catch (NullPointerException e)
        {
        }

        // Should not be equal to DIFFERENT objects
        for (int i = 0; i < MAX; ++i)
        {
            assertTrue("Two different patterns should compare not equals with different pattern string",
                    !(new AppPattern(goodPatterns[i % goodPatterns.length], goodActions[i % goodActions.length],
                            goodPriorities[i % goodPriorities.length])).equals(new AppPattern(goodPatterns[(i + 1)
                            % goodPatterns.length], goodActions[i % goodActions.length], goodPriorities[i
                            % goodPriorities.length])));
            assertTrue("Two different patterns should compare not equals with different actions", !(new AppPattern(
                    goodPatterns[i % goodPatterns.length], goodActions[i % goodActions.length], goodPriorities[i
                            % goodPriorities.length])).equals(new AppPattern(goodPatterns[i % goodPatterns.length],
                    goodActions[(i + 1) % goodActions.length], goodPriorities[i % goodPriorities.length])));
            assertTrue("Two different patterns should compare not equals with different priorities", !(new AppPattern(
                    goodPatterns[i % goodPatterns.length], goodActions[i % goodActions.length], goodPriorities[i
                            % goodPriorities.length])).equals(new AppPattern(goodPatterns[i % goodPatterns.length],
                    goodActions[i % goodActions.length], goodPriorities[(i + 1) % goodPriorities.length])));
        }
    }

    /**
     * Tests hashCode(). Should not look at expiration time or private info. To
     * equals should return same; two different *should* return different.
     */
    public void testHashCode() throws Exception
    {
        // Should not consider expiration time or private info -- because not
        // considered in equals()
        assertEquals("Expiration date should not be considered in hashCode", (new AppPattern(goodPatterns[0],
                goodActions[0], goodPriorities[0], new Date(), null)).hashCode(),
                (new AppPattern(goodPatterns[0], goodActions[0], goodPriorities[0], new Date(
                        System.currentTimeMillis() + 3600 * 72 * 1000), null)).hashCode());
        Date now = new Date();
        assertEquals("Private info should not be considered in hashCode", (new AppPattern(goodPatterns[1],
                goodActions[1], goodPriorities[1], now, "hello")).hashCode(), (new AppPattern(goodPatterns[1],
                goodActions[1], goodPriorities[1], now, "goodbye")).hashCode());

        // Items that would compare as equals should have same hashCode
        final int MAX = Math.max(goodPatterns.length, Math.max(goodActions.length, goodPriorities.length));
        for (int i = 0; i < MAX; ++i)
        {
            assertEquals("Two equivalent patterns have same hashCode [" + i + "]", (new AppPattern(goodPatterns[i
                    % goodPatterns.length], goodActions[i % goodActions.length], goodPriorities[i
                    % goodPriorities.length])).hashCode(), (new AppPattern(goodPatterns[i % goodPatterns.length],
                    goodActions[i % goodActions.length], goodPriorities[i % goodPriorities.length])).hashCode());
            assertEquals("Two equivalent patterns should have same hashCode", (new AppPattern(goodPatterns[i
                    % goodPatterns.length], goodActions[i % goodActions.length], goodPriorities[i
                    % goodPriorities.length], new Date(), "data")).hashCode(), (new AppPattern(goodPatterns[i
                    % goodPatterns.length], goodActions[i % goodActions.length], goodPriorities[i
                    % goodPriorities.length])).hashCode());
            assertEquals("Two equivalent patterns should have same hashCode", (new AppPattern(goodPatterns[i
                    % goodPatterns.length], goodActions[i % goodActions.length], goodPriorities[i
                    % goodPriorities.length], new Date(), "data")).hashCode(),
                    (new AppPattern(goodPatterns[i % goodPatterns.length], goodActions[i % goodActions.length],
                            goodPriorities[i % goodPriorities.length], new Date(
                                    System.currentTimeMillis() + 3600 * 72 * 1000), null)).hashCode());
        }

        // Completely different patterns *should* have different hashCodes
        // Although one can't be guaranteed that this will assert true
        assertTrue("Two disparate patterns *should* have different hashCodes",
                (new AppPattern("1", AppPattern.ALLOW, 0)).hashCode() != (new AppPattern("fff-1000:a-fff",
                        AppPattern.DENY, 100)).hashCode());

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
        TestSuite suite = new TestSuite(AppPatternTest.class);
        return suite;
    }

    public AppPatternTest(String name)
    {
        super(name);
    }
}
