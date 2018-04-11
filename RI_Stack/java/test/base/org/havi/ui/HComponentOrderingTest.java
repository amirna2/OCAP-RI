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

package org.havi.ui;

import junit.framework.*;
import org.cablelabs.test.*;
import java.awt.*;
import java.util.*;

/**
 * Test framework required for HComponentOrdering tests.
 * 
 * @author Aaron Kamienski
 */
public abstract class HComponentOrderingTest extends Assert
{
    /**
     * Test for correct ancestry.
     * <ul>
     * <li>implements HComponentOrdering
     * </ul>
     */
    public static void testAncestry(Class testClass)
    {
        TestUtils.testImplements(testClass, HComponentOrdering.class);
    }

    /**
     * Tests addAfter().
     * <ul>
     * <li>Add a new component after an existing
     * <li>Add an existing component after an existing
     * <li>Add a new component after a non-existing
     * <li>Check return values
     * </ul>
     */
    public static void testAddAfter(HComponentOrdering hc)
    {
        Vector v = addComponents(hc);

        Component c = new HVisible();

        // Add a new component after an existing
        assertSame("Add new component should have succeeded", c, hc.addAfter(c, (Component) v.elementAt(4)));
        v.insertElementAt(c, 5);

        // Add an existing after an existing (higher index)
        assertSame("Move existing component should have succeeded (>)", c, hc.addAfter(c, (Component) v.elementAt(0)));
        v.removeElementAt(5);
        v.insertElementAt(c, 1);

        // Add an existing after itself
        assertSame("Move existing after itself should have succeeded", c, hc.addAfter(c, (Component) v.elementAt(1)));
        // no actual change

        // Add an existing after an existing (lower index)
        c = (Component) v.elementAt(2);
        assertSame("Move existing component should have succeeded (<)", c, hc.addAfter(c, (Component) v.elementAt(6)));
        v.removeElementAt(2);
        v.insertElementAt(c, 6);

        // Add a new component after a non-existing
        assertNull("AddAfter non-existing component should have failed", hc.addAfter(new HVisible(), new HVisible()));

        checkComponents(hc, v);

        // Make sure that adding a null component does not succeed
        try
        {
            Component ok = hc.addAfter(null, (Component) v.elementAt(0));
            assertNull("If exception isn't thrown, " + "addAfter(null) should've failed", ok);
        }
        catch (Exception e)
        {
            // We really don't care if it throws an exception or not
            // Just that it did not succeed
        }
        checkComponents(hc, v);
    }

    /**
     * Tests addBefore().
     * <ul>
     * <li>Add a new component before an existing
     * <li>Add an existing component before an existing
     * <li>Add a new component before a non-existing
     * <li>Check return values
     * </ul>
     */
    public static void testAddBefore(HComponentOrdering hc)
    {
        Vector v = addComponents(hc);

        Component c = new HVisible();

        // Add a new component before an existing
        assertSame("Add new component should have succeeded", c, hc.addBefore(c, (Component) v.elementAt(4)));
        v.insertElementAt(c, 4);

        // Add an existing before an existing (higher index)
        assertSame("Move existing component should have succeeded (>)", c, hc.addBefore(c, (Component) v.elementAt(0)));
        v.removeElementAt(4);
        v.insertElementAt(c, 0);

        // Add an existing before itself
        assertSame("Move existing before itself should have succeeded", c, hc.addBefore(c, (Component) v.elementAt(1)));
        // no actual change

        // Add an existing before an existing (lower index)
        c = (Component) v.elementAt(1);
        assertSame("Move existing component should have succeeded (<)", c, hc.addBefore(c, (Component) v.elementAt(5)));
        v.removeElementAt(1);
        v.insertElementAt(c, 4);

        // Add a new component after a non-existing
        assertNull("AddBefore non-existing component should have failed", hc.addBefore(new HVisible(), new HVisible()));

        checkComponents(hc, v);

        // Make sure that adding a null component does not succeed
        try
        {
            Component ok = hc.addBefore(null, (Component) v.elementAt(1));
            assertNull("If exception isn't thrown, " + "addBefore(null) should've failed", ok);
        }
        catch (Exception e)
        {
            // We really don't care if it throws an exception or not
            // Just that it did not succeed
        }
        checkComponents(hc, v);
    }

    /**
     * Tests pop().
     * <ul>
     * <li>Pop a component past the front
     * <li>Pop a non-existent component
     * <li>Check return values
     * </ul>
     */
    public static void testPop(HComponentOrdering hc)
    {
        Vector v = addComponents(hc);

        Component c = (Component) v.elementAt(9);

        // Pop past end
        for (int i = 0; i < 9; ++i)
            assertTrue("Pop should have succeeded", hc.pop(c));
        assertTrue("Pop should have failed", !hc.pop(c));
        v.removeElementAt(9);
        v.insertElementAt(c, 0);

        // Pop non-existent
        assertTrue("Pop of non-existent should have failed", !hc.pop(new HVisible()));

        // Pop another
        c = (Component) v.elementAt(5);
        assertTrue("Pop should have succeeded", hc.pop(c));
        v.removeElementAt(5);
        v.insertElementAt(c, 4);

        checkComponents(hc, v);

        // Make sure that popping a null component does not succeed
        try
        {
            boolean ok = hc.pop(null);
            assertEquals("If exception isn't thrown, " + "pop(null) should've failed", false, ok);
        }
        catch (Exception e)
        {
            // We really don't care if it throws an exception or not
            // Just that it did not succeed
        }
        checkComponents(hc, v);
    }

    /**
     * Tests popInFrontOf().
     * <ul>
     * <li>Pop an existing component before an existing
     * <li>Attempt to pop a new component before an existing
     * <li>Pop an existing component before a non-existing
     * <li>Attempt to pop a new component before a non-existing
     * <li>Check return values
     * </ul>
     */
    public static void testPopInFrontOf(HComponentOrdering hc)
    {
        Vector v = addComponents(hc);

        Component c0 = (Component) v.elementAt(8);
        Component c1 = (Component) v.elementAt(3);

        assertTrue("Pop-in-front should have succeeded (>)", hc.popInFrontOf(c0, c1));
        v.removeElementAt(8);
        v.insertElementAt(c0, 3);

        // Try opposite direction
        c0 = (Component) v.elementAt(5);
        c1 = (Component) v.elementAt(7);
        assertTrue("Pop-in-front should have succeeded (<)", hc.popInFrontOf(c0, c1));
        v.removeElementAt(5);
        v.insertElementAt(c0, 6);

        // Try "identity" case
        c0 = (Component) v.elementAt(2);
        c1 = (Component) v.elementAt(3);
        assertTrue("Pop-in-front should have succeeded (identity)", hc.popInFrontOf(c0, c1));
        // There should be no change

        // Try "boundary" case
        c0 = (Component) v.elementAt(v.size() - 1);
        c1 = (Component) v.elementAt(0);
        assertTrue("Pop-in-front should have succeeded (boundary)", hc.popInFrontOf(c0, c1));
        v.removeElementAt(v.size() - 1);
        v.insertElementAt(c0, 0);

        assertTrue("Pop-in-front for a non-existing should fail", !hc.popInFrontOf(new HVisible(), c1));
        assertTrue("Pop-in-front of a non-existing should fail", !hc.popInFrontOf(c0, new HVisible()));
        assertTrue("Pop-in-front for/of non-existing should fail", !hc.popInFrontOf(new HVisible(), new HVisible()));

        checkComponents(hc, v);

        // Make sure that popping a null component does not succeed
        try
        {
            boolean ok = hc.popInFrontOf(null, (Component) v.elementAt(1));
            assertEquals("If exception isn't thrown, " + "popInFrontOf(null) should've failed", false, ok);
        }
        catch (Exception e)
        {
            // We really don't care if it throws an exception or not
            // Just that it did not succeed
        }
        checkComponents(hc, v);
    }

    /**
     * Tests popToFront().
     * <ul>
     * <li>Pop components to the front
     * <li>Pop a non-existent component
     * <li>Check return values
     * </ul>
     */
    public static void testPopToFront(HComponentOrdering hc)
    {
        Vector v = addComponents(hc);

        // Reverse order
        for (int i = 0; i < 10; ++i)
        {
            Component c = (Component) v.elementAt(i);
            assertTrue("Pop-to-front should have succeeded", hc.popToFront(c));
            v.removeElementAt(i);
            v.insertElementAt(c, 0);
        }

        // Pop non-existent
        assertTrue("Pop-to-front of non-existent should have failed", !hc.popToFront(new HVisible()));

        checkComponents(hc, v);

        // Make sure that popping a null component does not succeed
        try
        {
            boolean ok = hc.popToFront(null);
            assertEquals("If exception isn't thrown, " + "popToFront(null) should've failed", false, ok);
        }
        catch (Exception e)
        {
            // We really don't care if it throws an exception or not
            // Just that it did not succeed
        }
        checkComponents(hc, v);
    }

    /**
     * Tests push().
     * <ul>
     * <li>Push a component past the end
     * <li>Push a non-existent component
     * <li>Check return values
     * </ul>
     */
    public static void testPush(HComponentOrdering hc)
    {
        Vector v = addComponents(hc);

        Component c = (Component) v.elementAt(0);

        // Push past end
        for (int i = 0; i < 9; ++i)
            assertTrue("Push should have succeeded", hc.push(c));
        assertTrue("Push should have failed", !hc.push(c));
        v.removeElementAt(0);
        v.insertElementAt(c, 9);

        // Push non-existent
        assertTrue("Push of non-existent should have failed", !hc.push(new HVisible()));

        // Push another
        c = (Component) v.elementAt(5);
        assertTrue("Push should have succeeded", hc.push(c));
        v.removeElementAt(5);
        v.insertElementAt(c, 6);

        checkComponents(hc, v);

        // Make sure that pushing a null component does not succeed
        try
        {
            boolean ok = hc.push(null);
            assertEquals("If exception isn't thrown, " + "push(null) should've failed", false, ok);
        }
        catch (Exception e)
        {
            // We really don't care if it throws an exception or not
            // Just that it did not succeed
        }
        checkComponents(hc, v);
    }

    /**
     * Tests pushBehind().
     * <ul>
     * <li>Push an existing component before an existing
     * <li>Attempt to push a new component before an existing
     * <li>Push an existing component before a non-existing
     * <li>Attempt to push a new component before a non-existing
     * <li>Check return values
     * </ul>
     */
    public static void testPushBehind(HComponentOrdering hc)
    {
        Vector v = addComponents(hc);

        Component c0 = (Component) v.elementAt(8);
        Component c1 = (Component) v.elementAt(3);

        assertTrue("Push-behind should have succeeded (>)", hc.pushBehind(c0, c1));
        v.removeElementAt(8);
        v.insertElementAt(c0, 4);

        // Try opposite direction
        c0 = (Component) v.elementAt(5);
        c1 = (Component) v.elementAt(7);
        assertTrue("Push-behind should have succeeded (<)", hc.pushBehind(c0, c1));
        v.removeElementAt(5);
        v.insertElementAt(c0, 7);

        // Try "identity" case
        c0 = (Component) v.elementAt(3);
        c1 = (Component) v.elementAt(2);
        assertTrue("Push-behind should have succeeded (identity)", hc.pushBehind(c0, c1));
        // There should be no change

        // Try "boundary" case
        c0 = (Component) v.elementAt(0);
        c1 = (Component) v.elementAt(v.size() - 1);
        assertTrue("Push-behind should have succeeded (boundary)", hc.pushBehind(c0, c1));
        v.removeElementAt(0);
        v.addElement(c0);

        assertTrue("Push-behind for a non-existing should fail", !hc.pushBehind(new HVisible(), c1));
        assertTrue("Push-behind of a non-existing should fail", !hc.pushBehind(c0, new HVisible()));
        assertTrue("Push-behind for/of non-existing should fail", !hc.pushBehind(new HVisible(), new HVisible()));

        checkComponents(hc, v);

        // Make sure that pushing a null component does not succeed
        try
        {
            boolean ok = hc.pushBehind(null, (Component) v.elementAt(1));
            assertEquals("If exception isn't thrown, " + "pushBehind(null) should've failed", false, ok);
        }
        catch (Exception e)
        {
            // We really don't care if it throws an exception or not
            // Just that it did not succeed
        }
        checkComponents(hc, v);
    }

    /**
     * Tests pushToBack().
     * <ul>
     * <li>Push components to the back
     * <li>Push a non-existent component
     * <li>Check return values
     * </ul>
     */
    public static void testPushToBack(HComponentOrdering hc)
    {
        Vector v = addComponents(hc);

        // Reverse order
        for (int i = 0; i < 10; ++i)
        {
            Component c = (Component) v.elementAt(0);
            assertTrue("Push-to-back should have succeeded", hc.pushToBack(c));
            v.removeElementAt(0);
            v.insertElementAt(c, 9);
        }

        // Push non-existent
        assertTrue("Push-to-back of non-existent should have failed", !hc.pushToBack(new HVisible()));

        checkComponents(hc, v);

        // Make sure that pushing a null component does not succeed
        try
        {
            boolean ok = hc.pushToBack(null);
            assertEquals("If exception isn't thrown, " + "pushToBack(null) should've failed", false, ok);
        }
        catch (Exception e)
        {
            // We really don't care if it throws an exception or not
            // Just that it did not succeed
        }
        checkComponents(hc, v);
    }

    /**
     * Checks that the correct components are in the given container, and in the
     * right places.
     */
    private static void checkComponents(HComponentOrdering co, Vector v)
    {
        Container c = (Container) co;
        Component current[] = c.getComponents();

        assertEquals("Component count should be the same", v.size(), current.length);
        for (int i = 0; i < current.length; ++i)
        {
            assertSame("A Component is out of place (" + i + ")", v.elementAt(i), current[i]);
        }
    }

    /**
     */
    private static Vector addComponents(HComponentOrdering co)
    {
        Container hc = (Container) co;
        Vector v = new Vector();
        for (int i = 0; i < 10; ++i)
        {
            Component c = new HVisible();
            hc.add(c);
            v.addElement(c);
        }
        return v;
    }

}
