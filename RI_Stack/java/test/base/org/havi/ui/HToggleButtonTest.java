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
import java.awt.*;

/**
 * Tests {@link #HToggleButton}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.10 $, $Date: 2002/11/07 21:14:10 $
 */
public class HToggleButtonTest extends HGraphicButtonTest
{
    /**
     * Standard constructor.
     */
    public HToggleButtonTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HToggleButtonTest.class);
    }

    /**
     * The tested component.
     */
    protected HToggleButton htogglebutton;

    /**
     * Should be overridden to create subclass of HToggleButton.
     * 
     * @return the instance of HToggleButton to test
     */
    protected HToggleButton createHToggleButton()
    {
        return new HToggleButton();
    }

    /**
     * Overridden to create an HToggleButton.
     * 
     * @return the instance of HGraphicButton to test
     */
    protected HGraphicButton createHGraphicButton()
    {
        return (htogglebutton = createHToggleButton());
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HGraphicButton
     * <li>implements HSwitchable
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HToggleButton.class, HGraphicButton.class);
        HSwitchableTest.testAncestry(HToggleButton.class);
    }

    /**
     * Test the 10 constructors of HToggleButton.
     * <ul>
     * <li>HToggleButton()
     * <li>HToggleButton(Image img)
     * <li>HToggleButton(Image img, boolean state, HToggleGroup group)
     * <li>HToggleButton(Image normal, Image focus, Image action, Image
     * normalAction, boolean state)
     * <li>HToggleButton(Image normal, Image focus, Image action, Image
     * normalAction, boolean state, HToggleGroup group)
     * <li>HToggleButton(Image normal, Image focus, Image action, Image
     * normalAction, int x, int y, int w, int h, boolean state)
     * <li>HToggleButton(Image normal, Image focus, Image action, Image
     * normalAction, int x, int y, int w, int h, boolean state, HToggleGroup
     * group)
     * <li>HToggleButton(Image img, int x, int y, int w, int h)
     * <li>HToggleButton(Image img, int x, int y, int w, int h, boolean state)
     * <li>HToggleButton(Image img, int x, int y, int w, int h, boolean state,
     * HToggleGroup group)
     * </ul>
     */
    public void testConstructors()
    {
        HToggleGroup tg = new HToggleGroup();
        Image normal = new HVisibleTest.EmptyImage(), focus = new HVisibleTest.EmptyImage(), action = new HVisibleTest.EmptyImage(), switched = new HVisibleTest.EmptyImage();

        checkConstructor("HToggleButton()", new HToggleButton(), 0, 0, 0, 0, null, null, null, null, false, null, false);
        checkConstructor("HToggleButton(Image img)", new HToggleButton(normal), 0, 0, 0, 0, normal, normal, normal,
                normal, false, null, false);
        checkConstructor("HToggleButton(Image img, boolean state, " + "HToggleGroup group)", new HToggleButton(normal,
                true, tg), 0, 0, 0, 0, normal, normal, normal, normal, true, tg, false);
        checkConstructor("HToggleButton(Image normal, Image focus, "
                + "Image action, Image normalAction, boolean state)", new HToggleButton(normal, focus, action,
                switched, true), 0, 0, 0, 0, normal, focus, action, switched, true, null, false);
        checkConstructor("HToggleButton(Image normal, Image focus, " + "Image action, Image normalAction, "
                + "boolean state, HToggleGroup tg)", new HToggleButton(normal, focus, action, switched, false, tg), 0,
                0, 0, 0, normal, focus, action, switched, false, tg, false);
        checkConstructor("HToggleButton(Image normal, Image focus, " + "Image action, Image normalAction, "
                + "int x, int y, int w, int h)" + "boolean state)", new HToggleButton(normal, focus, action, switched,
                1, 2, 3, 4, false), 1, 2, 3, 4, normal, focus, action, switched, false, null, true);
        checkConstructor("HToggleButton(Image normal, Image focus, " + "Image action, Image normalAction, "
                + "int x, int y, int w, int h)" + "boolean state, HToggleGroup tg)", new HToggleButton(normal, focus,
                action, switched, 2, 4, 6, 8, true, tg), 2, 4, 6, 8, normal, focus, action, switched, true, tg, true);
        checkConstructor("HToggleButton(Image img, int x, int y, int w, int h)",
                new HToggleButton(normal, 4, 8, 12, 16), 4, 8, 12, 16, normal, normal, normal, normal, false, null,
                true);
        checkConstructor("HToggleButton(Image img, " + "int x, int y, int w, int h, " + "boolean state)",
                new HToggleButton(normal, 0, 1, 0, 2, true), 0, 1, 0, 2, normal, normal, normal, normal, true, null,
                true);
        checkConstructor("HToggleButton(Image img, " + "int x, int y, int w, int h, "
                + "boolean state, HToggleGroup tg)", new HToggleButton(normal, 1, 0, 2, 0, true, tg), 1, 0, 2, 0,
                normal, normal, normal, normal, true, tg, true);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HToggleButton button, int x, int y, int w, int h, Image normal,
            Image focus, Image action, Image switched, boolean state, HToggleGroup tg, boolean defaultSize)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", button);
        assertEquals(msg + " x-coordinated not initialized correctly", x, button.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, button.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, button.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, button.getSize().height);
        assertSame(msg + " NORMAL image not initialized correctly", normal, button.getGraphicContent(NORMAL_STATE));
        assertSame(msg + " FOCUSED image not initialized correctly", focus, button.getGraphicContent(FOCUSED_STATE));
        assertSame(msg + " ACTIONED_FOCUSED image not initialized correctly", action,
                button.getGraphicContent(ACTIONED_FOCUSED_STATE));
        assertSame(msg + " ACTIONED image not initialized correctly", switched,
                button.getGraphicContent(ACTIONED_STATE));
        assertEquals(msg + " state not initialized correctly", state, button.getSwitchableState());
        assertSame(msg + " toggle group not initialized correctly", tg, button.getToggleGroup());
        assertEquals(msg + " switchable state not initialized correctly", state, button.getSwitchableState());

        // Check variables NOT exposed in constructors
        if (!state)
            assertEquals(msg + " should be NORMAL_STATE", NORMAL_STATE, button.getInteractionState());
        else
            assertEquals(msg + " should be ACTIONED_STATE", ACTIONED_STATE, button.getInteractionState());
        assertNull(msg + " matte should be unassigned", button.getMatte());
        assertNotNull(msg + " text layout mgr should be assigned", button.getTextLayoutManager());
        assertEquals(msg + " bg mode not initialized incorrectly", button.getBackgroundMode(),
                button.NO_BACKGROUND_FILL);
        if (!defaultSize)
            // assertNull(msg+" default size should not be set",
            // button.getDefaultSize());
            assertEquals(msg + " default size should not be set", button.NO_DEFAULT_SIZE, button.getDefaultSize());
        else
            assertEquals(msg + " default size initialized incorrectly", button.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", button.getHorizontalAlignment(),
                button.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", button.getVerticalAlignment(),
                button.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", button.getResizeMode(), button.RESIZE_NONE);
        assertSame(msg + " default look not used", HToggleButton.getDefaultLook(), button.getLook());
        assertNull(msg + " gain focus sound incorrectly initialized", button.getGainFocusSound());
        assertNull(msg + " lose focus sound incorrectly initialized", button.getLoseFocusSound());
        assertNull(msg + " action sound incorrectly initialized", button.getActionSound());
        assertNull(msg + " unset action sound incorrectly initialized", button.getUnsetActionSound());
        assertNull(msg + " actionCommand incorrectly initialized", button.getActionCommand());
        assertEquals(msg + " border mode not initialized correctly", true, button.getBordersEnabled());
    }

    /**
     * Test addActionListener()/removeActionListener().
     * <ul>
     * <li>Test that the listener gets called
     * <li>Ensure that it doesn't after being removed
     * </ul>
     */
    public void testActionListener()
    {
        HSwitchableTest.testActionListener(new HToggleButton());
    }

    /**
     * Test {get|set}ActionSound().
     * <ul>
     * <li>Ensure that the set sound is the retreived sound
     * <li>Tests setActionSound(null)
     * <li>Test that the sound is played when the component is unset
     * </ul>
     */
    public void testActionSound()
    {
        HSwitchableTest.testActionSound(new HToggleButton());
    }

    /**
     * Test {get|set}UnsetActionSound().
     * <ul>
     * <li>Ensure that the set sound is the retreived sound
     * <li>Tests setUnsetActionSound(null)
     * <li>Test that the sound is played when the component is unset
     * </ul>
     */
    public void testUnsetActionSound()
    {
        HSwitchableTest.testUnsetActionSound(new HToggleButton());
    }

    /**
     * Test {get|set}SwitchableState
     * <ul>
     * <li>Ensure that the set state is the retreived state
     * <li>Ensure proper mapping: set={ACTION,NORMAL_ACTIONED},
     * unset={FOCUS,NORMAL}
     * <li>Ensure proper changes to interaction state based on current focus
     * state (focused or unfocused)
     * </ul>
     */
    public void testSwitchableState()
    {
        HSwitchableTest.testSwitchableState(new HToggleButton());
    }

    /**
     * Test {get|set|remove}ToggleGroup().
     * <ul>
     * <li>The set toggle group should be the returned toggle group
     * <li>Ensure radio-button functionality with a toggle group
     * <li>Ensure check-box functionality withOUT a toggle group (after removal)
     * <li>Ensure that changes to toggle button's current group (e.g., removal
     * or change) affects the group's current button.
     * <li>Ensure that changes to toggle buttons are reflected in the toggle
     * group (and other buttons in a group).
     * </ul>
     */
    public void testToggleGroup()
    {
        HToggleGroup tg[] = new HToggleGroup[] { new HToggleGroup(), new HToggleGroup(), };

        HToggleButton tb = new HToggleButton();

        // Ensure set/get/remove match up correctly
        for (int i = 0; i < tg.length; ++i)
        {
            tb.setToggleGroup(tg[i]);
            assertSame("Set toggle group should be retrieved toggle group " + "[" + i + "]", tg[i], tb.getToggleGroup());
        }

        tb.removeToggleGroup();
        assertNull("Should have been removed from toggle group", tb.getToggleGroup());
        tb.setToggleGroup(tg[1]);
        assertSame("Set toggle group should be retrieved toggle group", tg[1], tb.getToggleGroup());
        tb.setToggleGroup(null);
        assertNull("Should have been removed from toggle group", tb.getToggleGroup());

        HToggleButton b[] = new HToggleButton[] { new HToggleButton(), new HToggleButton(), new HToggleButton(),
                new HToggleButton(), new HToggleButton(), };
        b[b.length - 1].setSwitchableState(true);
        for (int i = 0; i < b.length; ++i)
            b[i].setToggleGroup(tg[0]);

        // Radio-button functionality (toggle group)
        for (int i = 0; i < b.length; ++i)
        {
            b[i].setSwitchableState(true);
            assertSame("ToggleGroup current should have changed [" + i + "]", b[i], tg[0].getCurrent());

            // Make sure others in group are unset
            for (int j = 1; j < b.length; ++j)
            {
                int k = (i + j) % b.length;

                assertTrue("Other buttons in group should be unset [" + i + "," + k + "]", !b[k].getSwitchableState());
            }
        }

        // Check-box functionality (no toggle group)
        for (int i = 0; i < b.length; ++i)
        {
            b[i].removeToggleGroup();
            b[i].setSwitchableState(true);
        }
        for (int i = 0; i < b.length; ++i)
            assertTrue("All buttons should be set [" + i + "]", b[i].getSwitchableState());

        // Check for proper addition to a group when all buttons are switched
        tg[1].setCurrent(null);
        for (int i = 0; i < b.length; ++i)
        {
            b[i].setToggleGroup(tg[1]);
            assertSame("ToggleGroup current should have changed [" + i + "]", b[i], tg[1].getCurrent());

            // Make sure previously added buttons are unswitched as a result
            for (int j = 0; j < i; ++j)
                assertTrue("Previously added buttons should be unswitched " + "[" + i + "," + j + "]",
                        !b[j].getSwitchableState());
        }

        // Check for proper changes as a result of group change
        b[0].setSwitchableState(true);
        assertSame("ToggleGroup current should have changed", b[0], tg[1].getCurrent());
        b[0].setToggleGroup(tg[0]);
        assertNull("Previous ToggleGroup should not have current after group change", tg[1].getCurrent());
        assertSame("ToggleGroup current should have changed", b[0], tg[0].getCurrent());
        b[0].removeToggleGroup();
        assertNull("Previous ToggleGroup should not have current after group removal", tg[0].getCurrent());
    }

    /**
     * Tests proper state traversal as a result of action events.
     */
    public void testProcessHActionEvent()
    {
        HSwitchableTest.testProcessHActionEvent(htogglebutton);
    }

    /**
     * Tests add/removeHFocusListener().
     */
    public void testFocusListener()
    {
        HSwitchableTest.testFocusListener(hicon);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HGraphicLook should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HToggleButtons should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        assertSame("Default look should be used", HToggleButton.getDefaultLook(), (new HToggleButton()).getLook());

        HGraphicLook save = HToggleButton.getDefaultLook();

        try
        {
            HGraphicLook look;

            HToggleButton.setDefaultLook(look = new HGraphicLook());
            assertSame("Incorrect look retrieved", look, HToggleButton.getDefaultLook());
            assertSame("Default look should be used", look, (new HToggleButton()).getLook());

            /*
             * // Not possible, because signature doesn't allow it try {
             * HToggleButton.setDefaultLook(new HVisibleTest.EmptyLook());
             * fail("Invalid look accepted"); } catch(HInvalidLookException
             * ignored) {}
             */
        }
        finally
        {
            // reset
            HToggleButton.setDefaultLook(save);
        }
    }

    /**
     * Create an HComponent of the appropriate class type that, in response to
     * HAVi Events, will set the generated[0] element to true.
     * <p>
     * The special component should (where appropriate) override:
     * <ul>
     * <li>processHFocusEvent
     * <li>processHTextEvent
     * <li>processHKeyEvent
     * </ul>
     * <p>
     * This is necessary because HNavigable and HTextValue components are not
     * required to support HFocusListeners.
     * 
     * @param ev
     *            a helper object used to test the event generation
     * @see #testProcessEvent
     */
    protected HComponent createSpecialComponent(final EventCheck ev)
    {
        checkClass(HToggleButtonTest.class);

        return new HToggleButton()
        {
            public void processHFocusEvent(org.havi.ui.event.HFocusEvent e)
            {
                ev.validate(e);
            }

            public void processHActionEvent(org.havi.ui.event.HActionEvent e)
            {
                ev.validate(e);
            }
        };
    }
}
