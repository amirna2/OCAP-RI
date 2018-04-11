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

/**
 * Tests {@link #HScreenConfigTemplate}.
 * 
 * @author Jay Tracy
 * @author Tom Henriksen
 * @version $Revision: 1.7 $, $Date: 2002/06/03 21:32:19 $
 */
public class HScreenConfigTemplateTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HScreenConfigTemplateTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HScreenConfigTemplateTest.class);
        System.exit(0);
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends Object (by default)
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HScreenConfigTemplate.class, Object.class);
    }

    /**
     * Test that there are no public HScreenConfigTemplate constructors.
     */
    public void testConstructors()
    {
        // Actually, one is defined, but it is abstract
        // TestUtil.testNoPublicConstructors(HScreenConfigTemplate.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HScreenConfigTemplate.class);
    }

    /**
     * Tests for no unexpected fields and that expected ones are unique.
     */
    public void testFields()
    {
        TestUtils.testNoAddedFields(HScreenConfigTemplate.class, fields);
        TestUtils.testUniqueFields(HScreenConfigTemplate.class, fields, false, 0, 8);
        TestUtils.testUniqueFields(HScreenConfigTemplate.class, fields, false, 8, 5);
    }

    private final static String[] fields = { "DONT_CARE", "FLICKER_FILTERING", "INTERLACED_DISPLAY",
            "PIXEL_ASPECT_RATIO", "PIXEL_RESOLUTION", "SCREEN_RECTANGLE", "VIDEO_GRAPHICS_PIXEL_ALIGNED",
            "ZERO_GRAPHICS_IMPACT", "ZERO_VIDEO_IMPACT", "ZERO_BACKGROUND_IMPACT", "PREFERRED", "PREFERRED_NOT",
            "REQUIRED", "REQUIRED_NOT", "UNNECESSARY" };

    final static String[] priorities = { "REQUIRED", "PREFERRED",
    // "UNNECESSARY",
            "PREFERRED_NOT", "REQUIRED_NOT" };

    public final static String[] prefs = { "INTERLACED_DISPLAY", "FLICKER_FILTERING", "ZERO_GRAPHICS_IMPACT",
            "ZERO_VIDEO_IMPACT", "VIDEO_GRAPHICS_PIXEL_ALIGNED", "PIXEL_ASPECT_RATIO", "PIXEL_RESOLUTION",
            "SCREEN_RECTANGLE" };

    public final static boolean[] isObjPref = { false, false, false, false, false, true, true, true };

    /**
     * Test getPreferenceObject.
     * <ul>
     * <li>Ensure that what is set, is what is retrieved
     * </ul>
     */
    public static void checkPreferenceObject(HScreenConfigTemplate template, String[] preferences,
            boolean[] isObjPreference, Object[] objects)
    {
        for (int i = 0; i < preferences.length; i++)
        {
            if (isObjPreference[i])
            {
                int prefValue = getVal(template, preferences[i]);

                template.setPreference(prefValue, objects[i], HScreenConfigTemplate.REQUIRED);
                assertEquals("set preference object should equal retrieved preference object", objects[i],
                        template.getPreferenceObject(prefValue));
                assertEquals("set preference priority should equal retrieved preference priority",
                        HScreenConfigTemplate.REQUIRED, template.getPreferencePriority(prefValue));
            }
        }
    }

    /**
     * Test getPreferencePriority and setPreference(int preference, int
     * priority)
     * <ul>
     * <li>Test that the set priority is the retrieved priority.
     * </ul>
     */
    public static void checkPreferencePriority(HScreenConfigTemplate template, String[] preferences,
            boolean[] isObjPreference)
    {
        for (int x = 0; x < preferences.length; x++)
        {
            if (!isObjPreference[x])
            {
                int prefValue = getVal(template, preferences[x]);

                for (int y = 0; y < priorities.length; y++)
                {
                    int priorityValue = getVal(template, priorities[y]);
                    template.setPreference(prefValue, priorityValue);
                    assertEquals("Retrieved priority for \"" + preferences[x] + "\" should be \"" + priorities[y]
                            + "\"", priorityValue, template.getPreferencePriority(prefValue));
                }
            }
        }
    }

    /**
     * Test isDisplayConfigSupported.
     * <ul>
     * <li>
     * </ul>
     */
    public void testDisplayConfigSupported()
    {
        // tested in subclass tests
    }

    private static int getVal(HScreenConfigTemplate template, String name)
    {
        int val = -1;

        try
        {
            Class cl = template.getClass();
            val = cl.getField(name).getInt(null);
        }
        catch (Exception e)
        {
        }

        return val;
    }

    public static void checkInitialPriorities(HScreenConfigTemplate template, String[] preferences,
            boolean[] isObjPreference)
    {
        for (int x = 0; x < preferences.length; x++)
        {
            int prefValue = getVal(template, preferences[x]);

            // Make sure that all the preference priorities are marked as
            // UNNECESSARY
            assertEquals("Initial priority for \"" + preferences[x] + "\" should be UNNECESSARY",
                    HScreenConfigTemplate.DONT_CARE, template.getPreferencePriority(prefValue));

            // Make sure that all the object preferences have an initial null
            // value.
            if (isObjPreference[x] == true)
            {
                assertNull("The object preference \"" + preferences[x] + "\" should initially be null",
                        template.getPreferenceObject(prefValue));
            }
        }
    }

}
