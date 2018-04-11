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

import junit.framework.*;
import java.lang.reflect.*;

/**
 * Tests {@link #HScreenRectangle}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.4 $, $Date: 2002/06/03 21:32:19 $
 */
public class HScreenRectangleTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HScreenRectangleTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HScreenRectangleTest.class);
    }

    /**
     * Test the 1 constructor of HScreenRectangle.
     * <ul>
     * <li>HScreenRectangle(float x, float y, float width, float height)
     * </ul>
     */
    public void testConstructors()
    {
        HScreenRectangle hsr = new HScreenRectangle(0, 0.4F, 0.25F, 0.5F);
        checkLocation(hsr, 0, 0.4F, "initialized");
        checkSize(hsr, 0.25F, 0.5F, "initialized");

        hsr = new HScreenRectangle(0, 0, 0, 0);
        checkLocation(hsr, 0.0F, 0.0F, "initialized");
        checkSize(hsr, 0.0F, 0.0F, "initialized");
    }

    /**
     * Test that the x, y, width, and height fields are accessible and are
     * floats.
     */
    public void testFields() throws NoSuchFieldException
    {
        Class cl = HScreenRectangle.class;
        Field fields[] = new Field[] { cl.getField("x"), cl.getField("y"), cl.getField("width"), cl.getField("height") };

        // Make sure that the type is float and its publicly accessible
        for (int i = 0; i < fields.length; ++i)
        {
            int mods = fields[i].getModifiers();

            assertTrue("The " + fields[i].getName() + " field should be public", Modifier.isPublic(mods));
            assertSame("The " + fields[i].getName() + " field should be a float", float.class, fields[i].getType());
        }
    }

    /**
     * Test setLocation(). Test that the x and y fields are set accordingly.
     */
    public void testLocation()
    {
        HScreenRectangle hsr = new HScreenRectangle(0, 0, 0, 0);

        hsr.setLocation(0.75F, 0.9F);
        checkLocation(hsr, 0.75F, 0.9F, "set");
    }

    /**
     * Test setSize(). Test that the width and height fields are set
     * accordingly.
     */
    public void testSize()
    {
        HScreenRectangle hsr = new HScreenRectangle(0, 0, 0, 0);

        hsr.setSize(0.75F, 0.9F);
        checkSize(hsr, 0.75F, 0.9F, "set");
    }

    /**
     * Check the location variables for the given HScreenRectangle.
     */
    static void checkLocation(HScreenRectangle hsr, float x, float y, String set)
    {
        assertEquals("X coordinate not " + set + " correctly", x, hsr.x, 0.0);
        assertEquals("Y coordinate not " + set + " correctly", y, hsr.y, 0.0);
    }

    /**
     * Check the size variables for the given HScreenRectangle.
     */
    static void checkSize(HScreenRectangle hsr, float w, float h, String set)
    {
        assertEquals("Width not " + set + " correctly", w, hsr.width, 0.0F);
        assertEquals("Height not " + set + " correctly", h, hsr.height, 0.0F);
    }

}
