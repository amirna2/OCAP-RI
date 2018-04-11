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
 * Tests {@link #HStaticRange}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.9 $, $Date: 2002/11/07 21:14:09 $
 */
public class HStaticRangeTest extends HVisibleTest
{
    /**
     * Standard constructor.
     */
    public HStaticRangeTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HStaticRangeTest.class);
        System.exit(0);
    }

    /**
     * Creates a look of the appropriate type. Should be overridden by
     * subclasses.
     */
    protected HLook createLook()
    {
        return new HRangeLook()
        {
        };
    }

    /**
     * The tested component.
     */
    protected HStaticRange hstaticrange;

    /**
     * Should be overridden to create subclass of HStaticRange.
     * 
     * @return the instance of HStaticRange to test
     */
    protected HStaticRange createHStaticRange()
    {
        return new HStaticRange();
    }

    /**
     * Overridden to create an HStaticRange.
     * 
     * @return the instance of HVisible to test
     */
    protected HVisible createHVisible()
    {
        return (hstaticrange = createHStaticRange());
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HVisible
     * <li>implements HNoInputPreferred
     * <li>implements HOrientable
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HStaticRange.class, HVisible.class);
        TestUtils.testImplements(HStaticRange.class, HNoInputPreferred.class);
        HOrientableTest.testAncestry(HStaticRange.class);
    }

    /**
     * Test the 3 constructors of HStaticRange.
     * <ul>
     * <li>HStaticRange()
     * <li>HStaticRange(int orient, int min, int max, int value)
     * <li>HStaticRange(int orient, int min, int max, int value, int x, int y,
     * int width, int height)
     * </ul>
     */
    public void testConstructors()
    {
        checkClass(HStaticRangeTest.class);

        checkConstructor("HStaticRange()", new HStaticRange(), HOrientable.ORIENT_LEFT_TO_RIGHT, 0, 100, 0, 0, 0, 0, 0,
                false);
        checkConstructor("HStaticRange(int orient, int min, int max, int val)", new HStaticRange(
                HOrientable.ORIENT_BOTTOM_TO_TOP, 20, 30, 25), HOrientable.ORIENT_BOTTOM_TO_TOP, 20, 30, 25, 0, 0, 0,
                0, false);
        checkConstructor("HStaticRange(int orient, int min, int max, int val," + "int x, int y, int w, int h)",
                new HStaticRange(HOrientable.ORIENT_BOTTOM_TO_TOP, 0, 359, 180, 100, 100, 100, 100),
                HOrientable.ORIENT_BOTTOM_TO_TOP, 0, 359, 180, 100, 100, 100, 100, true);
    }

    /**
     * Check for proper initialization of constructor values.
     */
    private void checkConstructor(String msg, HStaticRange range, int orient, int min, int max, int value, int x,
            int y, int w, int h, boolean defaultSize)
    {
        // Check variables exposed in constructors
        assertNotNull(msg + " not allocated", range);
        assertEquals(msg + " x-coordinated not initialized correctly", x, range.getLocation().x);
        assertEquals(msg + " y-coordinated not initialized correctly", y, range.getLocation().y);
        assertEquals(msg + " width not initialized correctly", w, range.getSize().width);
        assertEquals(msg + " height not initialized correctly", h, range.getSize().height);
        assertEquals(msg + " orientation not initializaed correctly", orient, range.getOrientation());
        assertEquals(msg + " value not initializaed correctly", value, range.getValue());
        assertEquals(msg + " minimum value not initializaed correctly", min, range.getMinValue());
        assertEquals(msg + " maximum value not initializaed correctly", max, range.getMaxValue());

        // Check variables NOT exposed in constructors (see java docs for not
        // exposed valuse)
        assertNull(msg + " matte should be unassigned", range.getMatte());
        assertNotNull(msg + " text layout mgr should be assigned", range.getTextLayoutManager());
        assertEquals(msg + " bg mode not initialized incorrectly", range.getBackgroundMode(), range.NO_BACKGROUND_FILL);
        if (!defaultSize)
            // assertNull(msg+" default size should not be set",
            // range.getDefaultSize());
            assertEquals(msg + " default size should not be set", range.NO_DEFAULT_SIZE, range.getDefaultSize()); // Dec
                                                                                                                  // 10,
                                                                                                                  // 2006
                                                                                                                  // changed
                                                                                                                  // siegfried@heitnze.com
        else
            assertEquals(msg + " default size initialized incorrectly", range.getDefaultSize(), new Dimension(w, h));
        assertEquals(msg + " horiz alignment initialized incorrectly", range.getHorizontalAlignment(),
                range.HALIGN_CENTER);
        assertEquals(msg + " vert alignment initialized incorrectly", range.getVerticalAlignment(), range.VALIGN_CENTER);
        assertEquals(msg + " resize mode initialized incorrectly", range.getResizeMode(), range.RESIZE_NONE);
        assertSame(msg + " default look not used", HStaticRange.getDefaultLook(), range.getLook());
        assertEquals(msg + " min thumb offset default is incorrect", 0, range.getThumbMinOffset());
        assertEquals(msg + " max thumb offset default is incorrect", 0, range.getThumbMaxOffset());
        assertEquals(msg + " default behavior is incorrect", HStaticRange.SLIDER_BEHAVIOR, range.getBehavior());
        assertEquals(msg + " border mode not initialized correctly", true, range.getBordersEnabled());
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HStaticRange.class);
    }

    private static final String fields[] = { "SLIDER_BEHAVIOR", "SCROLLBAR_BEHAVIOR" };

    /**
     * Tests for expected, unique fields.
     */
    public void testFields()
    {
        TestUtils.testNoAddedFields(HStaticRange.class, fields);
        TestUtils.testUniqueFields(HStaticRange.class, fields, false);
    }

    /**
     * Tests {set|get}Behavior.
     * <ul>
     * <li>set behavior should be retrieved behavior
     * <li>check behaviors ability to limit valid values
     * </ul>
     */
    public void testBehavior()
    {
        HStaticRange r = hstaticrange;

        r.setBehavior(HStaticRange.SCROLLBAR_BEHAVIOR);
        assertEquals("Set behavior not retrieved behavior", HStaticRange.SCROLLBAR_BEHAVIOR, r.getBehavior());

        // Test scrollbar behavior
        r.setThumbOffsets(2, 2);
        r.setRange(0, 10);
        r.setValue(5);
        assertEquals("Set value not retrieved value", 5, r.getValue());
        r.setValue(2);
        assertEquals("Set value not retrieved value", 2, r.getValue());
        r.setValue(8);
        assertEquals("Set value not retrieved value", 8, r.getValue());
        r.setValue(1);
        assertTrue("Value should not be set out of range (scrollbar)", r.getValue() != 1);
        r.setValue(9);
        assertTrue("Value should not be set out of range (scrollbar)", r.getValue() != 9);

        r.setBehavior(HStaticRange.SLIDER_BEHAVIOR);
        assertEquals("Set behavior not retrieved behavior", HStaticRange.SLIDER_BEHAVIOR, r.getBehavior());

        // Test slider behavior
        r.setThumbOffsets(2, 2);
        r.setRange(0, 10);
        r.setValue(5);
        assertEquals("Set value not retrieved value", 5, r.getValue());
        r.setValue(0);
        assertEquals("Set value not retrieved value", 0, r.getValue());
        r.setValue(10);
        assertEquals("Set value not retrieved value", 10, r.getValue());
        r.setValue(-1);
        assertTrue("Value should not be set out of range (scrollbar)", r.getValue() != -1);
        r.setValue(11);
        assertTrue("Value should not be set out of range (scrollbar)", r.getValue() != 11);
    }

    /**
     * Tests setRange()/getMinValue()/getMaxValue().
     * <ul>
     * <li>Test that the set values are the retrieved values
     * <li>Test that minimum < maximum returns true
     * <li>Test that minimum >= maximum returns false and doesn't set the range
     * <li>check minimum/maximum ability to limit valid values
     * </ul>
     */
    public void testRange()
    {
        HStaticRange r = hstaticrange;

        assertTrue("Successful setting of range should return true", r.setRange(7, 9));
        assertEquals("Set min value not retrieved mininum", 7, r.getMinValue());
        assertEquals("Set max value not retrieved maximum", 9, r.getMaxValue());

        assertTrue("Unuccessful setting of range should return false", !r.setRange(9, 7));
        assertEquals("Invalid setRange() should not set min and max values", 7, r.getMinValue());
        assertEquals("Invalid setRange() should not set min and max values", 9, r.getMaxValue());

        r.setValue(8);
        assertEquals("Set value not retrieved value", 8, r.getValue());
        r.setValue(6);
        assertTrue("Value should not be set out of range", r.getValue() != 6);
        r.setValue(10);
        assertTrue("Value should not be set out of range", r.getValue() != 10);
    }

    /**
     * Tests setThumbOffsets()/getThumMinOffset()/getThumMaxOffset().
     * <ul>
     * <li>Test that the set values are the retrieved values
     * </ul>
     */
    public void testThumbOffsets()
    {
        HStaticRange r = hstaticrange;

        r.setThumbOffsets(7, 9);
        assertEquals("Set thumb min offset not retrieved offset", 7, r.getThumbMinOffset());
        assertEquals("Set thumb min offset not retrieved offset", 9, r.getThumbMaxOffset());
    }

    /**
     * Tests {set|get}Value().
     * <ul>
     * <li>set value should be the retrieved value
     * </ul>
     */
    public void testValue()
    {
        HStaticRange r = hstaticrange;

        r.setValue(47);
        assertEquals("Set value not retrieved value", 47, r.getValue());
    }

    /**
     * Tests {set|get}Orientation.
     * <ul>
     * <li>Test that the set orientation is the retrieved orientation
     * </ul>
     */
    public void testOrientation() throws Exception
    {
        HOrientableTest.testOrientation(hstaticrange);
    }

    /**
     * Test setDefaultLook/getDefaultLook.
     * <ul>
     * <li>Only HRangeLooks should be accepted.
     * <li>The set look should be the retreived look.
     * <li>newly created HStaticRange should use the new default look
     * </ul>
     */
    public void testDefaultLook() throws HInvalidLookException
    {
        checkClass(HStaticRangeTest.class);

        assertSame("Default look should be used", HStaticRange.getDefaultLook(), (new HStaticRange()).getLook());

        HRangeLook save = HStaticRange.getDefaultLook();
        try
        {
            HRangeLook look;
            HStaticRange.setDefaultLook(look = new HRangeLook());
            assertSame("Incorrect look retrieved", look, HStaticRange.getDefaultLook());
            assertSame("Default look should be used", look, (new HStaticRange()).getLook());

            /*
             * // Not possible, because signature doesn't allow it try {
             * HStaticRange.setDefaultLook(new HVisibleTest.EmptyLook());
             * fail("Invalid look accepted"); } catch(HInvalidLookException
             * ignored) {}
             */
        }
        finally
        {
            // reset
            HStaticRange.setDefaultLook(save);
        }
    }
}
