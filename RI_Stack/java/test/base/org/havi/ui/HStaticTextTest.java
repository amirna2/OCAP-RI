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
 * Tests {@link #HStaticText}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.12 $, $Date: 2002/11/07 21:14:09 $
 */
public class HStaticTextTest extends HVisibleTest
{
    /**
     * Standard constructor.
     */
    public HStaticTextTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HStaticTextTest.class);
    }

    /**
     * Creates a look of the appropriate type. Should be overridden by
     * subclasses.
     */
    protected HLook createLook()
    {
        return new HTextLook()
        {
        };
    }

    /**
     * The tested component.
     */
    protected HStaticText hstatictext;

    /**
     * Should be overridden to create subclass of HStaticText.
     * 
     * @return the instance of HStaticText to test
     */
    protected HStaticText createHStaticText()
    {
        return new HStaticText();
    }

    /**
     * Overridden to create an HStaticText.
     * 
     * @return the instance of HVisible to test
     */
    protected HVisible createHVisible()
    {
        return (hstatictext = createHStaticText());
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HVisible
     * <li>implements HNoInputPreferred
     * </ul>
     */
    public void testAncestry()
    {
        checkClass(HStaticTextTest.class);

        TestUtils.testExtends(HStaticText.class, HVisible.class);
        TestUtils.testImplements(HStaticText.class, HNoInputPreferred.class);
    }

    /**
     * Test the 5 constructors of HStaticText.
     * <ul>
     * <li>HStaticText()
     * <li>HStaticText(String txt)
     * <li>HStaticText(String txt, int x, int y, int w, int h)
     * <li>HStaticText(String txt, Font font, Color fg, Color bg,
     * HTextLayoutManager tlm)
     * <li>HStaticText(String txt, int x, int y, int w, int h, Font font, Color
     * fg, Color bg, HTextLayoutManager tlm)
     * </ul>
     */
    public void testConstructors()
    {
        Color fg = Color.blue;
        Color bg = Color.orange;
        Font f = new Font("Dialog", Font.BOLD, 22);
        HTextLayoutManager tlm = new HDefaultTextLayoutManager();
        checkConstructor("HStaticText()", new HStaticText(), null, 0, 0, 0, 0, null, null, null, null, false, false);
        checkConstructor("HStaticText(String txt)", new HStaticText("Hello"), "Hello", 0, 0, 0, 0, null, null, null,
                null, false, false);
        checkConstructor("HStaticText(String txt, int x, int y, int w, int h)", new HStaticText("Hello", 1, 2, 3, 4),
                "Hello", 1, 2, 3, 4, null, null, null, null, false, true);
        checkConstructor("HStaticText(String txt, Font f, Color fg, Color bg," + " HTextLayoutManager tlm)",
                new HStaticText("Hello", f, fg, bg, tlm), "Hello", 0, 0, 0, 0, f, fg, bg, tlm, true, false);
        checkConstructor("HStaticText(String txt, int x, int y, int w, int h" + " Font f, Color fg, Color bg,"
                + " HTextLayoutManager tlm)", new HStaticText("Hello", 2, 4, 6, 7, null, null, null, null), "Hello", 2,
                4, 6, 7, null, null, null, null, true, true);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, final HStaticText text, String txt, int x, int y, int w, int h, Font f,
            Color fg, Color bg, HTextLayoutManager tlm, boolean hasTLM, boolean defaultSize)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", text);
        assertEquals(msg + " x-coordinated not initialized correctly", x, text.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, text.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, text.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, text.getSize().height);
        assertSame(msg + " font not initialized correctly", f, text.getFont());
        assertSame(msg + " bg color not initialized correctly", bg, text.getBackground());
        assertSame(msg + " fg color not initialized correctly", fg, text.getForeground());
        if (!hasTLM)
            assertNotNull(msg + " text layout manager should not be null", text.getTextLayoutManager());
        else
            assertSame(msg + " text layout manager not initialized correctly", tlm, text.getTextLayoutManager());
        assertEquals(msg + " text not initialized correctly", txt, text.getTextContent(HState.NORMAL_STATE));
        foreachState(new Callback()
        {
            public void callback(int state)
            {
                if (state != NORMAL_STATE)
                    assertNull(stateToString(state) + " content should not be set", text.getTextContent(state));
            }
        });

        // Check variables NOT exposed in constructors
        assertSame(msg + " default look not used", HStaticText.getDefaultLook(), text.getLook());
        assertEquals(msg + " should be NORMAL_STATE", NORMAL_STATE, text.getInteractionState());
        assertNull(msg + " matte should be unassigned", text.getMatte());
        assertEquals(msg + " bg mode not initialized incorrectly", text.getBackgroundMode(), text.NO_BACKGROUND_FILL);
        if (!defaultSize)
            // assertNull(msg+" default size should not be set",
            // text.getDefaultSize());
            assertEquals(msg + " default size should not be set", text.NO_DEFAULT_SIZE, text.getDefaultSize());
        else
            assertEquals(msg + " default size initialized incorrectly", text.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", text.getHorizontalAlignment(),
                text.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", text.getVerticalAlignment(), text.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", text.getResizeMode(), text.RESIZE_NONE);
        assertEquals(msg + " border mode not initialized correctly", true, text.getBordersEnabled());
    }

    /**
     * Tests for unexpected added fields.
     */
    public void testFields()
    {
        TestUtils.testNoAddedFields(getTestedClass(), null);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HTextLook should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HStaticTexts should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        checkClass(HStaticTextTest.class);

        assertSame("Default look should be used", HStaticText.getDefaultLook(), (new HStaticText()).getLook());

        HTextLook save = HStaticText.getDefaultLook();

        try
        {
            HTextLook look;

            HStaticText.setDefaultLook(look = new HTextLook());
            assertSame("Incorrect look retrieved", look, HStaticText.getDefaultLook());
            assertSame("Default look should be used", look, (new HStaticText()).getLook());

            /*
             * // Not possible, because signature doesn't allow it try {
             * HStaticText.setDefaultLook(new HVisibleTest.EmptyLook());
             * fail("Invalid look accepted"); } catch(HInvalidLookException
             * ignored) {}
             */
        }
        finally
        {
            // reset
            HStaticText.setDefaultLook(save);
        }
    }
}