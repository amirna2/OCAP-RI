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
 * Tests {@link #HStaticIcon}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.10 $, $Date: 2002/11/07 21:14:09 $
 */
public class HStaticIconTest extends HVisibleTest
{
    /**
     * Standard constructor.
     */
    public HStaticIconTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HStaticIconTest.class);
        System.exit(0);
    }

    /**
     * Creates a look of the appropriate type. Should be overridden by
     * subclasses.
     */
    protected HLook createLook()
    {
        return new HGraphicLook()
        {
        };
    }

    /**
     * The tested component.
     */
    protected HStaticIcon hstaticicon;

    /**
     * Should be overridden to create subclass of HStaticIcon.
     * 
     * @return the instance of HStaticIcon to test
     */
    protected HStaticIcon createHStaticIcon()
    {
        return new HStaticIcon();
    }

    /**
     * Overridden to create an HStaticIcon.
     * 
     * @return the instance of HVisible to test
     */
    protected HVisible createHVisible()
    {
        return (hstaticicon = createHStaticIcon());
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
        checkClass(HStaticIconTest.class);

        TestUtils.testExtends(HStaticIcon.class, HVisible.class);
        TestUtils.testImplements(HStaticIcon.class, HNoInputPreferred.class);
    }

    /**
     * Test the 3 constructors of HStaticIcon.
     * <ul>
     * <li>HStaticIcon()
     * <li>HStaticIcon(Image img)
     * <li>HStaticIcon(Image img, int x, int y, int w, int h)
     * </ul>
     */
    public void testConstructors()
    {
        checkClass(HStaticIconTest.class);

        Image image = new EmptyImage();
        checkConstructor("HStaticIcon()", new HStaticIcon(), 0, 0, 0, 0, null, false);
        checkConstructor("HStaticIcon(Image img)", new HStaticIcon(image), 0, 0, 0, 0, image, false);
        checkConstructor("HStaticIcon(Image img, int x, int y, int w, int h)", new HStaticIcon(image, 10, 20, 30, 40),
                10, 20, 30, 40, image, true);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HStaticIcon icon, int x, int y, int w, int h, Image img,
            boolean defaultSize)
    {
        // Check variables exposed in constructors
        final HStaticIcon i = icon;
        assertNotNull(msg + " not allocated", icon);
        assertEquals(msg + " x-coordinated not initialized correctly", x, icon.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, icon.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, icon.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, icon.getSize().height);
        assertSame(msg + " Image not initialized correctly", img, icon.getGraphicContent(NORMAL_STATE));
        foreachState(new Callback()
        {
            public void callback(int state)
            {
                if (state != NORMAL_STATE)
                    assertNull(stateToString(state) + " content should not be set", i.getGraphicContent(state));
            }
        });

        // Check variables NOT exposed in constructors
        assertEquals(msg + " should be NORMAL_STATE", NORMAL_STATE, icon.getInteractionState());
        assertNull(msg + " matte should be unassigned", icon.getMatte());
        assertNotNull(msg + " text layout mgr should be assigned", icon.getTextLayoutManager());
        assertEquals(msg + " bg mode not initialized incorrectly", icon.getBackgroundMode(), icon.NO_BACKGROUND_FILL);
        if (!defaultSize)
            // assertNull(msg+" default size should not be set",
            // icon.getDefaultSize());
            assertEquals(msg + " default size should not be set", icon.NO_DEFAULT_SIZE, icon.getDefaultSize());
        else
            assertEquals(msg + " default size initialized incorrectly", icon.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", icon.getHorizontalAlignment(),
                icon.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", icon.getVerticalAlignment(), icon.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", icon.getResizeMode(), icon.RESIZE_NONE);
        assertSame(msg + " default look not used", HStaticIcon.getDefaultLook(), icon.getLook());
        assertEquals(msg + " border mode not initialized correctly", true, icon.getBordersEnabled());
    }

    /**
     * Tests for any exposed non-final fields or added final fields.
     */
    public void testFields()
    {
        TestUtils.testNoAddedFields(getTestedClass(), null);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HGraphicLook should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HStaticIcons should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        checkClass(HStaticIconTest.class);

        assertSame("Default look should be used", HStaticIcon.getDefaultLook(), (new HStaticIcon()).getLook());

        HGraphicLook save = HStaticIcon.getDefaultLook();

        try
        {
            HGraphicLook look;

            HStaticIcon.setDefaultLook(look = new HGraphicLook());
            assertSame("Incorrect look retrieved", look, HStaticIcon.getDefaultLook());
            assertSame("Default look should be used", look, (new HStaticIcon()).getLook());

            /*
             * // Not possible, because signature doesn't allow it try {
             * HStaticIcon.setDefaultLook(new HVisibleTest.EmptyLook());
             * fail("Invalid look accepted"); } catch(HInvalidLookException
             * ignored) {}
             */
        }
        finally
        {
            // reset
            HStaticIcon.setDefaultLook(save);
        }
    }
}
