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

package org.havi.ui.event;

import org.cablelabs.test.*;
import org.havi.ui.*;

import java.awt.*;
import java.util.*;
import java.lang.reflect.*;

/**
 * Tests {@link #HEventRepresentation}.
 * 
 * A couple issues remain:
 * <ol>
 * <li>HEventRepresentation.isSupported() should be reconciled with
 * HRcCapabilities.isSupported(int)
 * </ol>
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.8 $, $Date: 2002/06/03 21:34:29 $
 */
public class HEventRepresentationTest extends GUITest// TestCase
{
    /**
     * Standard constructor.
     */
    public HEventRepresentationTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HEventRepresentationTest.class);
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends Object
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HEventRepresentation.class, Object.class);
    }

    /**
     * Ensure that there are no accessible constructors.
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HEventRepresentation.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HEventRepresentation.class);
    }

    private static String fields[] = { "ER_TYPE_NOT_SUPPORTED", "ER_TYPE_COLOR", "ER_TYPE_STRING", "ER_TYPE_SYMBOL" };

    /**
     * Tests that the appropriate constant fields are defined. Also ensures that
     * the constant fields (ER_*) do not overlap (so that they can be added/ored
     * to produce a mask).
     */
    public void testFields() throws Exception
    {
        TestUtils.testPublicFields(HEventRepresentation.class, fields, int.class);
        TestUtils.testNoAddedFields(HEventRepresentation.class, fields);
        TestUtils.testUniqueFields(HEventRepresentation.class, fields, true);
    }

    /**
     * Tests getColor(). The havitest.properties file is consulted to determine:
     * <ul>
     * <li>if an HEventRepresentation object should be available for a key
     * <li>if a Color should be defined for that key
     * <li>if the defined Color is the correct Color
     * </ul>
     * 
     * @see #lookupRepresentation(int)
     */
    public void testColor() throws Exception
    {
        doTestRepresentation(new TestRepresentation()
        {
            public void test(HEventRepresentation her, Representation rep)
            {
                assertEquals("HEventRepresentation: " + rep.keyName + " color is assigned?", rep.color != null,
                        her.getColor() != null);
                if (rep.color != null)
                    assertEquals("HEventRepresentation: " + rep.keyName + " color is incorrect", rep.color,
                            her.getColor());
            }
        });
    }

    /**
     * Tests getString(). The havitest.properties file is consulted to
     * determine:
     * <ul>
     * <li>if an HEventRepresentation object should be available for a key
     * <li>if a textual representation should be defined for that key
     * <li>if the defined text is the correct text
     * </ul>
     * 
     * @see #lookupRepresentation(int)
     */
    public void testString() throws Exception
    {
        doTestRepresentation(new TestRepresentation()
        {
            public void test(HEventRepresentation her, Representation rep)
            {
                assertEquals("HEventRepresentation: " + rep.keyName + " string is assigned?", rep.name != null,
                        her.getString() != null);
                if (rep.name != null)
                    assertEquals("HEventRepresentation: " + rep.keyName + " string is incorrect", rep.name,
                            her.getString());
            }
        });
    }

    /**
     * Tests getSymbol(). The havitest.properties file is consulted to
     * determine:
     * <ul>
     * <li>if an HEventRepresentation object should be available for a key
     * <li>if a graphic representation should be defined for that key
     * <li>if the defined graphic is the correct graphic
     * </ul>
     * 
     * @see #lookupRepresentation(int)
     */
    public void testSymbol() throws Exception
    {
        doTestRepresentation(new TestRepresentation()
        {
            public void test(HEventRepresentation her, Representation rep)
            {
                // Get the image
                Image symbol = her.getSymbol();

                assertEquals("HEventRepresentation: " + rep.keyName + " symbol is assigned?", rep.image != null,
                        symbol != null);

                // Ask user if image is correct using
                // description from HEventRepresentation
                if (symbol != null)
                {
                    int width = symbol.getWidth(null);
                    int height = symbol.getHeight(null);
                    int x = 0;
                    int y = 0;
                    byte[] pixels = ScreenCapture.capture(x, y, width, height);
                    assertImage(rep.image, new String[] { rep.image + "?" }, pixels, width, height, rep.keyName
                            + "_img");
                }
            }
        });
    }

    /**
     * Tests getType(). The type returned is reconciled with the string, color,
     * and symbol associated with each key. If an HEventRepresentation returns a
     * string, color, or symbol that should be reflected in the type; if not,
     * that should also be reflected in the type.
     */
    public void testType() throws Exception
    {
        doTestRepresentation(new TestRepresentation()
        {
            public void test(HEventRepresentation her, Representation rep)
            {
                int type0 = her.getType();
                int type1 = 0;

                type1 += her.getColor() != null ? HEventRepresentation.ER_TYPE_COLOR : 0;
                type1 += her.getSymbol() != null ? HEventRepresentation.ER_TYPE_SYMBOL : 0;
                type1 += her.getString() != null ? HEventRepresentation.ER_TYPE_STRING : 0;
                type1 += !her.isSupported() ? HEventRepresentation.ER_TYPE_NOT_SUPPORTED : 0;

                assertEquals("HEventRepresentation: " + rep.keyName + " type is incorrect", type1, type0);
            }
        });
    }

    /**
     * Tests isSupported(). The havitest.properties file is consulted to
     * determine:
     * <ul>
     * <li>if an HEventRepresentation object should be available for a key
     * <li>if the key should be supported or not
     * </ul>
     * 
     * @see #lookupRepresentation(int)
     */
    public void testSupported() throws Exception
    {
        doTestRepresentation(new TestRepresentation()
        {
            public void test(HEventRepresentation her, Representation rep)
            {
                assertEquals("HEventRepresentation: " + rep.keyName + " is supported?", rep.supported,
                        her.isSupported());
            }
        });
    }

    /**
     * Helper method which runs a given TestRepresentation object over each key
     * defined by HRcEvent.
     */
    private void doTestRepresentation(TestRepresentation test) throws Exception
    {
        String fields[] = HRcEventTest.fields;

        for (int i = 0; i < fields.length; ++i)
        {
            if (fields[i].startsWith("VK_"))
            {
                Field field = HRcEvent.class.getField(fields[i]);
                int vk = field.getInt(null);

                Representation rep = lookupRepresentation(fields[i]);
                HEventRepresentation her = HRcCapabilities.getRepresentation(vk);

                assertEquals("Expected an HEventRepresentation for " + fields[i] + "?", rep != null, her != null);

                if (rep != null) test.test(her, rep);
            }
        }
    }

    /**
     * Helper method used to lookup the reference representation information for
     * the given key. This information is extracted from the havitest.properties
     * file using the properties support in the TestSupport class.
     * 
     * The properties that are consulted have the following comma-separated
     * format:
     * 
     * <pre>
     * HEventRepresentation.KEY-NAME=SUPPORTED,NAME,COLOR,IMAGE-DESCR
     * </pre>
     * 
     * Where:
     * <ul>
     * <li> <code>KEY-NAME</code> is the name of the key field in HRcEvent (e.g.,
     * <code>VK_STOP</code>).
     * <li> <code>SUPPORTED</code> is <code>true</code> or <code>false</code>,
     * specifying whether the key is supported or not. Defaults to false.
     * <li> <code>NAME</code> specifies the textual representation, if there is
     * one.
     * <li> <code>COLOR</code> is the color name or ARGB color integer that
     * represents this key, if there is one.
     * <li> <code>IMAGE-DESCR</code> is the textual description of the image
     * representation for this key, if there is one.
     * </ul>
     */
    private Representation lookupRepresentation(String keyName)
    {
        if (info == null)
        {
            info = new Hashtable();

            Properties props = TestSupport.getProperties();
            Enumeration e = props.keys();
            while (e.hasMoreElements())
            {
                String key = (String) e.nextElement();

                if (key.startsWith("HEventRepresentation.VK_"))
                {
                    String val = props.getProperty(key);
                    key = key.substring("HEventRepresentation.".length());

                    Representation rep = new Representation();
                    rep.keyName = key;

                    String entries[] = split(val, ",", 4);

                    // true|false?
                    if (entries.length > 0 && entries[0] != null) rep.supported = entries[0].equals("true");

                    // name
                    if (entries.length > 1) rep.name = entries[1];

                    // color
                    if (entries.length > 2 && entries[2] != null)
                    {
                        Color color = null;
                        try
                        { // static color name
                            Class c = Color.class;
                            Field f = c.getField(entries[2]);
                            color = (Color) f.get(null);
                        }
                        catch (Exception badColorName)
                        {
                            try
                            { // decode color numbers
                                color = Color.decode(entries[2]);
                            }
                            catch (Exception e2)
                            {
                                // try a system property (null if none found)
                                color = Color.getColor(entries[2]);
                            }
                        }
                        rep.color = color;
                    }

                    // image explanation
                    if (entries.length > 3) rep.image = entries[3];
                    info.put(key, rep);
                }
            }
        }

        return (Representation) info.get(keyName);
    }

    /**
     * Splits the given string into an array based on the given delimeter token.
     * This method should probably be available elsewhere!
     */
    public static String[] split(String str, String delimeter, int max)
    {
        Vector vector = new Vector();

        final int delLen = delimeter.length();
        final int strLen = str.length();
        int prev = 0;
        int loc;
        int n = 0;

        if (max > 0 && str.length() > 0)
        {
            loc = str.indexOf(delimeter, prev);
            do
            {
                ++n;
                if (prev == loc)
                    vector.addElement(null);
                else if (n >= max || loc == -1)
                    vector.addElement(str.substring(prev));
                else
                    vector.addElement(str.substring(prev, loc));
                prev = loc + delLen;
                loc = str.indexOf(delimeter, prev);
            }
            while ((n < max) && (loc != -1) && (prev < strLen));
            if (n < max && prev > 0)
            {
                ++n;
                vector.addElement(str.substring(prev));
            }
        }

        String[] array = new String[vector.size()];
        vector.copyInto(array);
        return array;
    }

    private static Hashtable info;

    private interface TestRepresentation
    {
        public void test(HEventRepresentation her, Representation rep);
    }

    private static class Representation
    {
        String keyName;

        boolean supported;

        String name; // if null, no name is provided

        Color color; // if null, no color is provided

        String image; // if null, no image is provided
    }
}
