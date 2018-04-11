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
 * Tests {@link #HMultilineEntry}.
 * 
 * @author Aaron Kamienski
 * @author Tom Henriksen
 * @version $Revision: 1.6 $, $Date: 2002/11/07 21:14:08 $
 */
public class HMultilineEntryTest extends HSinglelineEntryTest
{

    /**
     * Standard constructor.
     */
    public HMultilineEntryTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HMultilineEntryTest.class);
    }

    /**
     * Creates a look of the appropriate type. Should be overridden by
     * subclasses.
     */
    protected HLook createLook()
    {
        return new HMultilineEntryLook()
        {
        };
    }

    /**
     * The tested component.
     */
    protected HMultilineEntry hmultilineentry;

    /**
     * Should be overridden to create subclass of HMultilineEntry.
     * 
     * @return the instance of HMultilineEntry to test
     */
    protected HMultilineEntry createHMultilineEntry()
    {
        return new HMultilineEntry();
    }

    /**
     * Overridden to create an HText.
     * 
     * @return the instance of HSinglelineEntry to test
     */
    protected HSinglelineEntry createHSinglelineEntry()
    {
        return (hmultilineentry = createHMultilineEntry());
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HSinglelineEntry
     * </ul>
     */
    public void testAncestry()
    {
        checkClass(HMultilineEntryTest.class);

        TestUtils.testExtends(HMultilineEntry.class, HSinglelineEntry.class);
    }

    /**
     * Test the 5 constructors (and verify defaults and non-defaults):
     * <ul>
     * <li>HMultilineEntry()
     * <li>HMultilineEntry(int maxChars, int maxLines)
     * <li>HMultilineEntry(int x, int y, int width, int height, int maxChars,
     * int maxLines)
     * <li>HMultilineEntry(String text, int maxChars, int maxLines, Font f,
     * Color c)
     * <li>HMultilineEntry(String text, int x, int y, int width, int height, int
     * maxChars, int maxLines, Font f, Color c)
     * </ul>
     */
    public void testConstructors()
    {
        Color fg = Color.blue;
        Font f = new Font("Dialog", Font.BOLD, 22);
        checkConstructor("HMultilineEntry()", new HMultilineEntry(), null, 0, 0, 0, 0, 16, null, null);
        checkConstructor("HMultilineEntry(int maxChars)", new HMultilineEntry(20), null, 0, 0, 0, 0, 20, null, null);
        checkConstructor("HMultilineEntry(int x, int y, int w, int h, int maxChars)", new HMultilineEntry(1, 2, 3, 4,
                20), null, 1, 2, 3, 4, 20, null, null);
        checkConstructor("HMultilineEntry(String txt, int maxChars, Font f, Color fg)", new HMultilineEntry("Hello",
                30, f, fg), "Hello", 0, 0, 0, 0, 30, f, fg);
        checkConstructor(
                "HMultilineEntry(String txt, int x, int y, int w, int h," + " int maxChars, Font f, Color fg)",
                new HMultilineEntry("Hello", 1, 2, 3, 4, 40, f, fg), "Hello", 1, 2, 3, 4, 40, f, fg);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HMultilineEntry entry, String txt, int x, int y, int w, int h,
            int maxChars, Font f, Color fg)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", entry);
        assertEquals(msg + " x-coordinated not initialized correctly", x, entry.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, entry.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, entry.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, entry.getSize().height);
        assertEquals(msg + " maxChars not initialized correctly", maxChars, entry.getMaxChars());
        if (txt != null)
            assertEquals(msg + " text not initialized correctly", txt, entry.getTextContent(HState.NORMAL_STATE));
        else
            assertNull(msg + " text not initialized to null", entry.getTextContent(HState.NORMAL_STATE));
        assertSame(msg + " font not initialized correctly", f, entry.getFont());
        assertSame(msg + " fg color not initialized correctly", fg, entry.getForeground());

        // Check variables NOT exposed in constructors
        assertEquals(msg + " type should be set to accept any input", HKeyboardInputPreferred.INPUT_ANY,
                entry.getType());
        assertSame(msg + " default look not used", HMultilineEntry.getDefaultLook(), entry.getLook());
        assertNull(msg + " gain focus sound incorrectly initialized", entry.getGainFocusSound());
        assertNull(msg + " lose focus sound incorrectly initialized", entry.getLoseFocusSound());
        assertEquals(msg + " caret position incorrectly initialized", (txt != null) ? txt.length() : 0,
                entry.getCaretCharPosition());
        assertEquals(msg + " echo character incorrectly initialized", '\0', entry.getEchoChar());
        assertEquals(msg + " edit mode incorrectly initialized", false, entry.getEditMode());
        assertEquals(msg + " border mode not initialized correctly", true, entry.getBordersEnabled());
        assertNull(msg + " custom input chars should not be assigned", entry.getValidInput());
    }

    /**
     * Tests for unexpected added fields.
     */
    public void testFields()
    {
        TestUtils.testNoAddedFields(getTestedClass(), null);
    }

    /**
     * Test setLook().
     * <ul>
     * 
     * 
     * <li>Only HMultilineEntryLook should be accepted.
     * <li>The set look should be the retreived look.
     * </ul>
     */
    public void testLook() throws Exception
    {
        HMultilineEntryLook newLook = new HMultilineEntryLook();
        try
        {
            hmultilineentry.setLook(newLook);
        }
        catch (HInvalidLookException e)
        {
        }
        assertEquals("Set look should equal retrieved look", newLook, hmultilineentry.getLook());
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HMultilineEntryLook should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HMultilineEntrys should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        assertSame("Default look should be used", HMultilineEntry.getDefaultLook(), (new HMultilineEntry()).getLook());

        HMultilineEntryLook save = (HMultilineEntryLook) HMultilineEntry.getDefaultLook();

        try
        {
            HMultilineEntryLook look;

            HMultilineEntry.setDefaultLook(look = new HMultilineEntryLook());
            assertSame("Incorrect look retrieved", look, HMultilineEntry.getDefaultLook());
            assertSame("Default look should be used", look, (new HMultilineEntry()).getLook());
        }
        finally
        {
            // reset
            HMultilineEntry.setDefaultLook(save);
        }
    }

    /**
     * Tests caret{Next|Previous}Line().
     * <ul>
     * <li>Moves the caret to the next/previous line (if possible)
     * <li>Does not (need to) do anything if cannot be moved further
     * </ul>
     */
    public void testCaretLine()
    {
        String testString = "Line #1\nLine #2\nLine #3\nLine #4\nLine #5\nLine #6\nLine #7\nLine #8\nLine #9.";
        HMultilineEntry multiLine = (HMultilineEntry) hvisible;
        int lineLength = 8;
        int lines = 9;

        HScene testScene = null;
        java.awt.Graphics g = null;
        java.awt.Color bgColor = null;
        java.awt.LayoutManager layout = null;

        HSceneFactory factory = HSceneFactory.getInstance();
        testScene = factory.getDefaultHScene();
        g = testScene.getGraphics();
        bgColor = testScene.getBackground();
        layout = testScene.getLayout();

        try
        {
            testScene.add(hvisible);
            hvisible.setSize(hvisible.getPreferredSize());
            testScene.show();
            Dimension sz = new Dimension(400, 400);
            hvisible.setSize(sz); // was 100x100
            multiLine.setMaxChars(testString.length() * 2);
            hvisible.setTextContent(testString, HState.NORMAL_STATE);
            hvisible.getLook().showLook(hvisible.getGraphics(), hvisible, HState.NORMAL_STATE);

            for (int x = 0; x < lineLength - 1; x++)
            {

                multiLine.setCaretCharPosition(x);
                multiLine.getLook().showLook(multiLine.getGraphics(), multiLine, HState.NORMAL_STATE);

                for (int y = 0; y < lines; y++)
                {
                    assertEquals("The calulated character position following a "
                            + "caretNextLine should equal the retrieved character " + "position", y * lineLength + x,
                            multiLine.getCaretCharPosition());
                    multiLine.caretNextLine();
                    multiLine.getLook().showLook(multiLine.getGraphics(), multiLine, HState.NORMAL_STATE);
                }
                for (int y = lines - 1; y >= 0; y--)
                {
                    assertEquals("The calulated character position following a "
                            + "caretPreviousLine should equal the retrieved character " + "position", y * lineLength
                            + x, multiLine.getCaretCharPosition());
                    multiLine.caretPreviousLine();
                    multiLine.getLook().showLook(multiLine.getGraphics(), multiLine, HState.NORMAL_STATE);
                }
            }
        }
        finally
        {
            testScene.remove(hvisible);
            // repaint the HScene
            testScene.setBackground(bgColor);
            testScene.setBackgroundMode(HScene.BACKGROUND_FILL);
            testScene.paint(g);
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
        checkClass(HMultilineEntryTest.class);

        return new HMultilineEntry()
        {
            public void processHFocusEvent(org.havi.ui.event.HFocusEvent e)
            {
                ev.validate(e);
            }

            public void processHTextEvent(org.havi.ui.event.HTextEvent e)
            {
                ev.validate(e);
            }

            public void processHKeyEvent(org.havi.ui.event.HKeyEvent e)
            {
                ev.validate(e);
            }
        };
    }
}
