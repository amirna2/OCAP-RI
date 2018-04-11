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
 * Tests {@link #HTextButton}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.9 $, $Date: 2002/11/07 21:14:10 $
 */
public class HTextButtonTest extends HTextTest
{
    /**
     * Standard constructor.
     */
    public HTextButtonTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HTextButtonTest.class);
        System.exit(0);
    }

    /**
     * The tested component.
     */
    protected HTextButton htextbutton;

    /**
     * Should be overridden to create subclass of HTextButton.
     * 
     * @return the instance of HTextButton to test
     */
    protected HTextButton createHTextButton()
    {
        return new HTextButton();
    }

    /**
     * Overridden to create an HTextButton.
     * 
     * @return the instance of HText to test
     */
    protected HText createHText()
    {
        return (htextbutton = createHTextButton());
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HText
     * <li>implements HActionable
     * <li>implements HActionInputPreferred
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HTextButton.class, HText.class);
        HActionableTest.testAncestry(HTextButton.class);
    }

    /**
     * Test the 5 constructors of HTextButton.
     * <ul>
     * <li>HTextButton()
     * <li>HTextButton(String txt)
     * <li>HTextButton(String txt, int x, int y, int w, int h)
     * <li>HTextButton(String txt, Font font, Color fg, Color bg,
     * HTextLayoutManager tlm)
     * <li>HTextButton(String txt, int x, int y, int w, int h, Font font, Color
     * fg, Color bg, HTextLayoutManager tlm)
     * </ul>
     */
    public void testConstructors()
    {
        Color fg = Color.blue;
        Color bg = Color.orange;
        Font f = new Font("Dialog", Font.BOLD, 22);
        HTextLayoutManager tlm = new HDefaultTextLayoutManager();

        checkConstructor("HTextButton()", new HTextButton(), null, 0, 0, 0, 0, null, null, null, null, false, false);
        checkConstructor("HTextButton(String txt)", new HTextButton("howdy"), "howdy", 0, 0, 0, 0, null, null, null,
                null, false, false);
        checkConstructor("HTextButton(String txt, int x, int y, int w, int h)", new HTextButton("hi", 1, 2, 3, 4),
                "hi", 1, 2, 3, 4, null, null, null, null, false, true);
        checkConstructor("HTextButton(String txt, " + "Font f, Color fg, Color bg, " + "HTextLayoutManager tlm)",
                new HTextButton("howdy", f, fg, bg, null), "howdy", 0, 0, 0, 0, f, fg, bg, null, true, false);
        checkConstructor("HTextButton(String txt, " + "int x, int y, int w, int h, " + "Font f, Color fg, Color bg, "
                + "HTextLayoutManager tlm)", new HTextButton("howdy", 1, 2, 3, 4, null, fg, bg, tlm), "howdy", 1, 2, 3,
                4, null, fg, bg, tlm, true, true);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, final HTextButton button, final String txt, int x, int y, int w, int h,
            Font f, Color fg, Color bg, HTextLayoutManager tlm, boolean hasTLM, boolean defaultSize)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", button);
        assertEquals(msg + " x-coordinated not initialized correctly", x, button.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, button.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, button.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, button.getSize().height);
        foreachState(new Callback()
        {
            public void callback(int state)
            {
                assertSame(stateToString(state) + " content not initialized correctly", txt,
                        button.getTextContent(state));
            }
        });
        assertSame(msg + " font not initialized correctly", f, button.getFont());
        assertSame(msg + " bg color not initialized correctly", bg, button.getBackground());
        assertSame(msg + " fg color not initialized correctly", fg, button.getForeground());
        if (!hasTLM)
            assertNotNull(msg + " text layout manager should not be null", button.getTextLayoutManager());
        else
            assertSame(msg + " text layout manager not initialized correctly", tlm, button.getTextLayoutManager());

        // Check variables NOT exposed in constructors
        assertEquals(msg + " should be NORMAL_STATE", NORMAL_STATE, button.getInteractionState());
        assertNull(msg + " matte should be unassigned", button.getMatte());
        assertEquals(msg + " bg mode not initialized incorrectly", button.getBackgroundMode(),
                button.NO_BACKGROUND_FILL);
        if (!defaultSize)
            // assertNull(msg+" default size should not be set",
            // button.getDefaultSize());
            assertEquals(msg + " default size should not be set", button.NO_DEFAULT_SIZE, button.getDefaultSize()); // changed
                                                                                                                    // Dec
                                                                                                                    // 10,
                                                                                                                    // 2006
                                                                                                                    // siegfried@heintze.com
        else
            assertEquals(msg + " default size initialized incorrectly", button.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", button.getHorizontalAlignment(),
                button.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", button.getVerticalAlignment(),
                button.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", button.getResizeMode(), button.RESIZE_NONE);
        assertSame(msg + " default look not used", HTextButton.getDefaultLook(), button.getLook());
        assertNull(msg + " gain focus sound incorrectly initialized", button.getGainFocusSound());
        assertNull(msg + " lose focus sound incorrectly initialized", button.getLoseFocusSound());
        assertNull(msg + " action sound incorrectly initialized", button.getActionSound());
        assertNull(msg + " action command incorrectly initialized", button.getActionCommand());
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
        HActionableTest.testActionListener(htextbutton);
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
        HActionableTest.testActionCommand(htextbutton);
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
        HActionableTest.testActionSound(htextbutton);
    }

    /**
     * Tests proper state traversal as a result of action events.
     */
    public void testProcessHActionEvent()
    {
        HActionableTest.testProcessHActionEvent(htextbutton);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HTextLook should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HTextButtons should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        checkClass(HTextButtonTest.class);

        assertSame("Default look should be used", HTextButton.getDefaultLook(), (new HTextButton()).getLook());

        HTextLook save = HTextButton.getDefaultLook();

        try
        {
            HTextLook look;
            HTextButton.setDefaultLook(look = new HTextLook());
            assertSame("Incorrect look retrieved", look, HTextButton.getDefaultLook());
            assertSame("Default look should be used", look, (new HTextButton()).getLook());

            /*
             * // Not possible, because signature doesn't allow it try {
             * HTextButton.setDefaultLook(new HVisibleTest.EmptyLook());
             * fail("Invalid look accepted"); } catch(HInvalidLookException
             * ignored) {}
             */
        }
        finally
        {
            // reset
            HTextButton.setDefaultLook(save);
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
        checkClass(HTextButtonTest.class);

        return new HTextButton()
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
