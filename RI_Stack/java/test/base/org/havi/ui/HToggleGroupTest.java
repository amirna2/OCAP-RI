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

import org.cablelabs.test.*;
import junit.framework.*;
import java.awt.*;

/**
 * Tests {@link #HToggleGroup}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.6 $, $Date: 2002/06/03 21:32:22 $
 */
public class HToggleGroupTest extends TestCase implements HState
{
    /**
     * Standard constructor.
     */
    public HToggleGroupTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HToggleGroupTest.class);
    }

    /**
     * The tested component.
     */
    private HToggleGroup htogglegroup;

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        htogglegroup = new HToggleGroup();
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
     * <li>extends Object
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HToggleGroup.class, Object.class);
    }

    /**
     * Test the single constructor of HToggleGroup.
     * <ul>
     * HToggleGroup()
     * </ul>
     */
    public void testConstructors()
    {
        checkConstructor("HToggleGroup()", htogglegroup);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    public void checkConstructor(String msg, HToggleGroup tg)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", tg);

        // Check variables not exposed in constructors
        assertNull(msg + " no current toggle button should be assigned", tg.getCurrent());
        assertTrue(msg + " enabled state initialized incorrectly", tg.isEnabled());
        assertTrue(msg + " forced selection mode initialized incorrectly", !tg.getForcedSelection());
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HToggleGroup.class);
    }

    /**
     * Tests for no unexpected fields.
     */
    public void testNoAddedFields()
    {
        TestUtils.testNoAddedFields(HToggleGroup.class, null);
    }

    /**
     * Test {set|get}Current().
     * <ul>
     * <li>Value changes depending upon state of toggle buttons
     * <li>The set current toggle button must be the retreived
     * <li>Setting to null should unset the currently set toggle button
     * </ul>
     */
    public void testCurrent()
    {
        HToggleGroup tg = htogglegroup;
        Image img = new HVisibleTest.EmptyImage();
        HToggleButton b[] = new HToggleButton[] { new HToggleButton(img, false, tg), new HToggleButton(img, false, tg),
                new HToggleButton(img, false, tg), };

        assertNull("No current toggle button should be set", tg.getCurrent());

        for (int i = 0; i < b.length; ++i)
        {
            tg.setCurrent(b[i]);
            assertSame("Set current should be retrieved current [" + i + "]", b[i], tg.getCurrent());

            assertTrue("Button state should be set to true", b[i].getSwitchableState());

            for (int j = 1; j < b.length; ++j)
            {
                int k = (i + j) % b.length;

                assertTrue("Other button's state should be false [" + i + "," + k + "]", !b[k].getSwitchableState());
            }
        }

        tg.setCurrent(b[1]);
        tg.setCurrent(null);
        assertNull("Current should have been cleared", tg.getCurrent());
        assertTrue("Previous current button state should now be false", !b[1].getSwitchableState());
    }

    /**
     * Tests (set|get)ForcedSelection().
     * <ul>
     * <li>The set selection mode should be the retrieved selection mode.
     * <li>Test setting of forced selection given no current selection.
     * <li>Test proper operation if no components are added
     * <li>How forced selection is done is unspecified, however we will assume
     * that the currently selected item remains selected.
     * </ul>
     */
    public void testForcedSelection()
    {
        HToggleGroup tg = htogglegroup;

        // Test set/get
        tg.setForcedSelection(true);
        assertTrue("Retrieved forcedSelection mode should be set mode", tg.getForcedSelection());
        assertNull("No selection expected (given no added buttons)", tg.getCurrent());
        tg.setForcedSelection(false);
        assertTrue("Retrieved forcedSelection mode should be set mode", !tg.getForcedSelection());
        assertNull("No selection expected (given no added buttons)", tg.getCurrent());

        // Test setting given no current selection
        HToggleButton[] tb = new HToggleButton[] { new HToggleButton(), new HToggleButton(), new HToggleButton(), };
        addAll(tg, tb);

        // Set forcedSelection given no current selection
        assertNull("No selection is expected", tg.getCurrent());
        tg.setForcedSelection(true);
        checkSelected(tg, tb, tb[0]);

        // Try to turn off switchable state
        tb[0].setSwitchableState(false);
        checkSelected(tg, tb, tb[0]);
        assertSame("Expected forcedSelection to select 1st component", tb[0], tg.getCurrent());
        assertTrue("Expected current component to be switched", tb[0].getSwitchableState());

        // Select another component
        tb[1].setSwitchableState(true);
        checkSelected(tg, tb, tb[1]);
        assertTrue("Expected current component to be switched", tb[1].getSwitchableState());
        assertFalse("Expected previous component to be switched", tb[0].getSwitchableState());

        // Unselect and check for forced selection (of same)
        tb[1].setSwitchableState(false);
        checkSelected(tg, tb, tb[1]);
        assertSame("Expected forcedSelection to select current component", tb[1], tg.getCurrent());
        assertTrue("Expected current component to be switched", tb[1].getSwitchableState());

        tg.setForcedSelection(false);

        // Try to turn off switchable state again
        tb[1].setSwitchableState(false);
        checkSelected(tg, tb, null);
        assertNull("No selection expected", tg.getCurrent());
        assertTrue("Expected old current component to be unswitched", !tb[0].getSwitchableState());
    }

    /**
     * Check that the given component is the only one selected.
     */
    private void checkSelected(HToggleGroup tg, HToggleButton[] tb, HToggleButton selected)
    {
        if (selected == null)
            assertNull("No selection expected", tg.getCurrent());
        else
            assertSame("Unexpected current selection", selected, tg.getCurrent());
        if (selected != null) assertTrue("Expected current selection to be switched", selected.getSwitchableState());
        for (int i = 0; i < tb.length; ++i)
            if (tb[i] != selected) assertTrue("Unexpected selection of other component", !tb[i].getSwitchableState());
    }

    /**
     * Check that the buttons are enabled/disabled.
     */
    private void checkEnabled(HToggleButton[] tb, boolean enabled)
    {
        for (int i = 0; i < tb.length; ++i)
            assertEquals("Expected all buttons to be " + (enabled ? "en" : "dis") + "abled", enabled,
                    (tb[i].getInteractionState() & DISABLED_STATE_BIT) == 0);
    }

    /**
     * Add all the given components.
     */
    private void addAll(HToggleGroup tg, HToggleButton[] tb)
    {
        for (int i = 0; i < tb.length; ++i)
        {
            // We probably are testing add...
            tg.add(tb[i]);

            // ...However, the REAL way to add to the group is using
            // setToggleGroup().
            // Hopefully, this doesn't add things twice... Could be a problem.
            tb[i].setToggleGroup(tg);
        }
    }

    /**
     * Remove all the given components.
     */
    private void removeAll(HToggleGroup tg, HToggleButton[] tb)
    {
        for (int i = 0; i < tb.length; ++i)
            tg.remove(tb[i]);
    }

    /**
     * Tests (set|is)Enabled.
     * <ul>
     * <li>The set value should be the retreived value
     * <li>Disabling/Enabling components should affect all added buttons
     * </ul>
     */
    public void testEnabled()
    {
        HToggleGroup tg = htogglegroup;

        // Test set/is
        tg.setEnabled(false);
        assertTrue("Retrieved enabled mode should be set mode", !tg.isEnabled());
        tg.setEnabled(true);
        assertTrue("Retrieved enabled mode should be set mode", tg.isEnabled());

        // Test resetting of components
        HToggleButton[] tb = new HToggleButton[] { new HToggleButton(), new HToggleButton(), new HToggleButton(), };
        addAll(tg, tb);
        /*
         * for(int i = 0; i < tb.length; ++i) tb[i].setToggleGroup(tg);
         */

        // Check for all enabled (initially)
        checkEnabled(tb, true);

        // Check for all disabled
        tg.setEnabled(false);
        checkEnabled(tb, false);

        // Check for all enabled
        tg.setEnabled(true);
        checkEnabled(tb, true);
    }

    /**
     * Tests add().
     * <ul>
     * <li>Added components should be enabled/disabled according to isEnabled
     * <li>ForcedSelection mode should be enforced on first add
     * <li>Only one component should be selected (so adding multiple selected
     * components should result in changes to the switchable state).
     * </ul>
     */
    public void testAdd()
    {
        HToggleGroup tg = htogglegroup;
        HToggleButton[] tb = new HToggleButton[] { new HToggleButton(), new HToggleButton(), new HToggleButton(), };

        // Check that all added components are disabled
        tg.setEnabled(false);
        addAll(tg, tb);
        checkEnabled(tb, false);
        removeAll(tg, tb);

        // Check that they are enabled
        tg.setEnabled(true);
        addAll(tg, tb);
        checkEnabled(tb, true);
        removeAll(tg, tb);

        // Check for enforcement of selection
        tg.setForcedSelection(true);
        addAll(tg, tb);
        checkSelected(tg, tb, tb[0]);
        removeAll(tg, tb);

        // Check that only one component is selected
        for (int i = 0; i < tb.length; ++i)
            tb[i].setSwitchableState(true);
        addAll(tg, tb);
        int count = 0;
        for (int i = 0; i < tb.length; ++i)
            if (tb[i].getSwitchableState()) ++count;
        assertEquals("Only one component should be switched after adds", 1, count);
    }

    /**
     * Tests remove().
     * <ul>
     * <li>If a component is not a member, throw IllegalArgumentException
     * <li>If the component is selected and forcedSelection is on, the first
     * remaing should become the current selected component
     * </ul>
     */
    public void testRemove()
    {
        HToggleGroup tg = htogglegroup;
        HToggleButton[] tb = new HToggleButton[] { new HToggleButton(), new HToggleButton(), new HToggleButton(), };

        // IllegalArgumentException
        try
        {
            tg.remove(tb[0]);
            fail("Expected IllegalArgumentException on removal on un-added " + "component");
        }
        catch (IllegalArgumentException expected)
        {
        }

        // Removal of forcedSelection
        addAll(tg, tb);
        tg.setForcedSelection(true);
        checkSelected(tg, tb, tb[0]);
        tg.remove(tb[0]);
        tb[0].setSwitchableState(false); // so that checkSelected doesn't bark
        checkSelected(tg, tb, tb[1]);
        // Re-add, selection should not change
        tg.add(tb[0]);
        checkSelected(tg, tb, tb[1]);
        removeAll(tg, tb);

        assertNull("No selection expected following removeAll", tg.getCurrent());
    }
}
