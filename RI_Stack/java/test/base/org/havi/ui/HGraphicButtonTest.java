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
 * Tests {@link #HGraphicButton}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.9 $, $Date: 2002/11/07 21:14:07 $
 */
public class HGraphicButtonTest extends HIconTest
{
    /**
     * Standard constructor.
     */
    public HGraphicButtonTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HGraphicButtonTest.class);
    }

    /**
     * The tested component.
     */
    protected HGraphicButton hgraphicbutton;

    /**
     * Should be overridden to create subclass of HGraphicButton.
     * 
     * @return the instance of HGraphicButton to test
     */
    protected HGraphicButton createHGraphicButton()
    {
        return new HGraphicButton();
    }

    /**
     * Overridden to create an HGraphicButton.
     * 
     * @return the instance of HIcon to test
     */
    protected HIcon createHIcon()
    {
        return (hgraphicbutton = createHGraphicButton());
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HIcon
     * <li>implements HActionable
     * <li>implements HActionInputPreferred
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HGraphicButton.class, HIcon.class);
        HActionableTest.testAncestry(HGraphicButton.class);
    }

    /**
     * Test the 3 constructors of HGraphicButton.
     * <ul>
     * <li>HGraphicButton()
     * <li>HGraphicButton(Image img)
     * <li>HGraphicButton(Image normal, Image focus, Image action)
     * <li>HGraphicButton(Image normal, Image focus, Image action, int x, int y,
     * int w, int h)
     * <li>HGraphicButton(Image img, int x, int y, int w, int h)
     * </ul>
     */
    public void testConstructors()
    {
        checkClass(HGraphicButtonTest.class);

        Image normal = new HVisibleTest.EmptyImage(), focus = new HVisibleTest.EmptyImage(), action = new HVisibleTest.EmptyImage();
        checkConstructor("HGraphicButton()", new HGraphicButton(), 0, 0, 0, 0, null, null, null, false);
        checkConstructor("HGraphicButton(Image img)", new HGraphicButton(normal), 0, 0, 0, 0, normal, normal, normal,
                false);
        checkConstructor("HGraphicButton(Image normal, Image focus, Image action)", new HGraphicButton(normal, focus,
                action), 0, 0, 0, 0, normal, focus, action, false);
        checkConstructor("HGraphicButton(Image normal, Image focus, Image action, " + "int x, int y, int w, int h)",
                new HGraphicButton(normal, focus, action, 1, 2, 3, 4), 1, 2, 3, 4, normal, focus, action, true);
        checkConstructor("HGraphicButton(Image img, " + "int x, int y, int w, int h)", new HGraphicButton(focus, 2, 4,
                6, 8), 2, 4, 6, 8, focus, focus, focus, true);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HGraphicButton button, int x, int y, int w, int h, Image normal,
            Image focus, Image action, boolean defaultSize)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", button);
        assertEquals(msg + " x-coordinated not initialized correctly", x, button.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, button.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, button.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, button.getSize().height);
        assertSame(msg + " NORMAL image not initialized correctly", normal,
                button.getGraphicContent(HState.NORMAL_STATE));
        assertSame(msg + " FOCUSED image not initialized correctly", focus,
                button.getGraphicContent(HState.FOCUSED_STATE));
        assertSame(msg + " ACTIONED image not initialized correctly", action,
                button.getGraphicContent(HState.ACTIONED_FOCUSED_STATE));

        // Check variables NOT exposed in constructors
        assertEquals(msg + " should be NORMAL_STATE", NORMAL_STATE, button.getInteractionState());
        assertNull(msg + " matte should be unassigned", button.getMatte());
        assertNotNull(msg + " text layout mgr should be assigned", button.getTextLayoutManager());
        assertEquals(msg + " bg mode not initialized incorrectly", button.getBackgroundMode(),
                button.NO_BACKGROUND_FILL);
        if (!defaultSize)
            assertEquals(msg + " default size should not be set", button.NO_DEFAULT_SIZE, button.getDefaultSize()); // assertNull(msg+" default size should not be set",
                                                                                                                    // button.getDefaultSize());
        else
            assertEquals(msg + " default size initialized incorrectly", button.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", button.getHorizontalAlignment(),
                button.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", button.getVerticalAlignment(),
                button.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", button.getResizeMode(), button.RESIZE_NONE);
        assertSame(msg + " default look not used", HGraphicButton.getDefaultLook(), button.getLook());
        assertNull(msg + " gain focus sound incorrectly initialized", button.getGainFocusSound());
        assertNull(msg + " lose focus sound incorrectly initialized", button.getLoseFocusSound());
        assertNull(msg + " action sound incorrectly initialized", button.getActionSound());
        assertNull(msg + " action command incorrectly initialized", button.getActionCommand());
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
        HActionableTest.testActionListener(hgraphicbutton);
    }

    /**
     * Test setActionCommand/getActionCommand.
     * <ul>
     * <li>Tests the default value (most likely null)
     * <li>Ensures that the set command is the retreived command
     * <li>Tests setActionCommand(null)
     * <li>Test that the command is set in the ActionEvent
     * </ul>
     */
    public void testActionCommand()
    {
        HActionableTest.testActionCommand(hgraphicbutton);
    }

    /**
     * Test setActionSound/getActionSound.
     * <ul>
     * <li>Tests the default value (most likely null)
     * <li>Ensures that the set sound is the retreived sound
     * <li>Tests setActionSound(null)
     * <li>Test that the sound is played when the component is actioned
     * </ul>
     */
    public void testActionSound()
    {
        HActionableTest.testActionSound(hgraphicbutton);
    }

    /**
     * Tests proper state traversal as a result of action events.
     */
    public void testProcessHActionEvent()
    {
        HActionableTest.testProcessHActionEvent(hgraphicbutton);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HGraphicLook should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HGraphicButtons should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        checkClass(HGraphicButtonTest.class);

        assertSame("Default look should be used", HGraphicButton.getDefaultLook(), (new HGraphicButton()).getLook());

        HGraphicLook save = HGraphicButton.getDefaultLook();

        try
        {
            HGraphicLook look;
            HGraphicButton.setDefaultLook(look = new HGraphicLook());
            assertSame("Incorrect look retrieved", look, HGraphicButton.getDefaultLook());
            assertSame("Default look should be used", look, (new HGraphicButton()).getLook());

            /*
             * // Not possible, because signature doesn't allow it try {
             * HGraphicButton.setDefaultLook(new HVisibleTest.EmptyLook());
             * fail("Invalid look accepted"); } catch(HInvalidLookException
             * ignored) {}
             */
        }
        finally
        {
            // reset
            HGraphicButton.setDefaultLook(save);
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
        checkClass(HGraphicButtonTest.class);

        return new HGraphicButton()
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
