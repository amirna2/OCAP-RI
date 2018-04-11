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

import org.cablelabs.test.*; //TODO import junit.framework.*;
import java.awt.*;

/**
 * Tests {@link #HContainer}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.10 $, $Date: 2002/06/03 21:32:12 $
 */
public class HContainerTest extends GUITest
{
    /**
     * Standard constructor.
     */
    public HContainerTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HContainerTest.class);
    }

    /** Common access HContainer */
    protected HContainer hcontainer;

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        hcontainer = new HContainer();
    }

    /**
     * Teardown.
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends Container
     * <li>implements HMatteLayer
     * <li>implements HComponentOrdering
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HContainer.class, Container.class);
        HMatteLayerTest.testAncestry(HContainer.class);
        HComponentOrderingTest.testAncestry(HContainer.class);
    }

    /**
     * Test the 2 constructors of HContainer.
     * <ul>
     * <li>HContainer()
     * <li>HContainer(int x, int y, int w, int h)
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HContainer()", hcontainer, 0, 0, 0, 0);
        checkConstructor("HContainer(int,int,int,int)", new HContainer(10, 10, 255, 255)
        {
        }, 10, 10, 255, 255);
    }

    /**
     * Check for proper initialization of constructor variables.
     */
    private void checkConstructor(String msg, HContainer v, int x, int y, int w, int h)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", v);
        assertEquals(msg + " x-coordinated not initialized correctly", x, v.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, v.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, v.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, v.getSize().height);

        assertNull(msg + " matte should be unassigned", v.getMatte());
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HContainer.class);
    }

    /**
     * Tests getMatte/setMatte
     * <ul>
     * <li>The set matte should be the retreived matte.
     * <li>Each of the 4 standard mattes should be allowed.
     * <li>A null matte should be allowed.
     * <li>Check for exception when setting matte and container already has a
     * running effect matte.
     * </ul>
     */
    public void testMatte() throws HMatteException
    {
        HMatteLayerTest.testMatte(hcontainer);
    }

    /**
     * Tests isDoubleBuffered(). Should return false.
     */
    public void testDoubleBuffered()
    {
        assertTrue("Double-buffering shouldn't be on for this component", !hcontainer.isDoubleBuffered());
    }

    /**
     * Tests isOpaque. Should return false.
     */
    public void testOpaque()
    {
        assertTrue("This component should not be opaque", !hcontainer.isOpaque());
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
    public void testAddAfter()
    {
        HComponentOrderingTest.testAddAfter(hcontainer);
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
    public void testAddBefore()
    {
        HComponentOrderingTest.testAddBefore(hcontainer);
    }

    /**
     * Tests push().
     * <ul>
     * <li>Push a component past the end
     * <li>Push a non-existent component
     * <li>Check return values
     * </ul>
     */
    public void testPush()
    {
        HComponentOrderingTest.testPush(hcontainer);
    }

    /**
     * Tests pop().
     * <ul>
     * <li>Pop a component past the front
     * <li>Pop a non-existent component
     * <li>Check return values
     * </ul>
     */
    public void testPop()
    {
        HComponentOrderingTest.testPop(hcontainer);
    }

    /**
     * Tests popToFront().
     * <ul>
     * <li>Pop components to the front
     * <li>Pop a non-existent component
     * <li>Check return values
     * </ul>
     */
    public void testPopToFront()
    {
        HComponentOrderingTest.testPopToFront(hcontainer);
    }

    /**
     * Tests pushToBack().
     * <ul>
     * <li>Push components to the back
     * <li>Push a non-existent component
     * <li>Check return values
     * </ul>
     */
    public void testPushToBack()
    {
        HComponentOrderingTest.testPushToBack(hcontainer);
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
    public void testPopInFrontOf()
    {
        HComponentOrderingTest.testPopInFrontOf(hcontainer);
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
    public void testPushBehind()
    {
        HComponentOrderingTest.testPushBehind(hcontainer);
    }

    /**
     * Tests group()/ungroup()/isGrouped().
     */
    public void testGroup()
    {
        hcontainer.group();
        assertTrue("Container should be grouped", hcontainer.isGrouped());
        hcontainer.ungroup();
        assertTrue("Container should not be grouped", !hcontainer.isGrouped());
    }
}
