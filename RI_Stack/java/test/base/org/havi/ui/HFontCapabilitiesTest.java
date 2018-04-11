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
import java.awt.*;

import java.util.BitSet;
import java.util.Hashtable;
import java.util.Enumeration;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Tests {@link #HFontCapabilities}.
 * 
 * @author Aaron Kamienski
 * @version $Revision: 1.6 $, $Date: 2002/11/07 21:14:06 $
 */
public class HFontCapabilitiesTest extends TestCase
{
    /**
     * Standard constructor.
     */
    public HFontCapabilitiesTest(String str)
    {
        super(str);
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
    {
        junit.textui.TestRunner.run(HFontCapabilitiesTest.class);
        System.exit(0);
    }

    /**
     * Setup.
     */
    public void setUp() throws Exception
    {
        super.setUp();
        setUpHashes();
    }

    /**
     * Teardown.
     */
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends Object
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HFontCapabilities.class, Object.class);
    }

    /**
     * Test that there are no HFontCapabilities constructors.
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HFontCapabilities.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HFontCapabilities.class);
    }

    /**
     * Test getSupportedCharacterRanges.
     * <ul>
     * <li>Read info from a Property:
     * 
     * <pre>
     * HFontCapabilities.<font-name>(#<size>)?(+<style>(&<style>)*)?=<range_field_name>
     * </pre>
     * 
     * And compare to actual tests.
     * <li>If property is set to null, expect null to be returned.
     * </ul>
     * 
     * <p>
     * This test is disabled until we can define the appropriate contents for
     * the havatest.properties file.
     * </p>
     */
    public void xtestSupportedCharacterRanges()
    {
        doTestCapabilities(new TestCapabilities()
        {
            public void test(String f)
            {
                Font font = getFont(f);

                int ranges[] = HFontCapabilities.getSupportedCharacterRanges(font);
                String ref[] = lookupFontCapabilities(f);

                assertEquals("SupportedCharacterRanges(" + f + ") should be null?", ref == null, ranges == null);

                if (ref != null)
                {
                    assertEquals("The number of SupportedCharacterRanges" + " is incorrect ", ref.length, ranges.length);

                    BitSet rangeSet = getBitSet(ranges);
                    BitSet refSet = getBitSet(ref);

                    assertEquals("", refSet, rangeSet);
                }
            }
        });
    }

    /**
     * ??? Esmertec implementation doesn't support java.awt.Font.canDisplay()
     * method - Sumathi Test isCharAvailable.
     * <ul>
     * If the font can display the character, it must be supported. Right?
     * </ul>
     */
    /*
     * public void testCharAvailable() { doTestCapabilities(new
     * TestCapabilities() { public void test(String f) { Font font = getFont(f);
     * 
     * // Do all chars... for(int i = 0; i < 0x10000; ++i) {
     * assertEquals("Char should be available "+
     * "if it can be displayed ("+f+","+ "'\\u"+Integer.toOctalString(i)+"')",
     * font.canDisplay((char)i), HFontCapabilities.isCharAvailable(font,
     * (char)i)); } } }); }
     */
    /**
     * Runs the given TestCapabilities test over each of the fonts specified in
     * the TestSupport.getProperties().
     * 
     * A more appropriate implementation may be to run this over all available
     * fonts.
     */
    private void doTestCapabilities(TestCapabilities test)
    {
        if (TestSupport.getProperty("HFontCapabilities.all", true))
        {
            // deprecated
            // String list[] = Toolkit.getDefaultToolkit().getFontList();
            String list[] = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

            for (int i = 0; i < list.length; ++i)
                test.test(list[i]);
        }
        else
        {
            Enumeration e = TestSupport.getProperties().keys();
            while (e.hasMoreElements())
            {
                String key = (String) e.nextElement();
                if (key.startsWith("HFontCapabilities."))
                {
                    String font = key.substring("HFontCapabilities.".length());

                    test.test(font);
                }
            }
        }
    }

    /**
     * Tests that only the required fields are defined and that they are unique.
     */
    public void testFields()
    {
        TestUtils.testPublicFields(HFontCapabilities.class, fields, int.class);
        TestUtils.testUniqueFields(HFontCapabilities.class, fields, false);
        TestUtils.testNoAddedFields(HFontCapabilities.class, fields);
    }

    /**
     * Parse a font name (with size and style).
     */
    private Font getFont(String f)
    {
        return Font.decode(f);
    }

    private String fields[] = { "BASIC_LATIN", "LATIN_1_SUPPLEMENT", "LATIN_EXTENDED_A", "LATIN_EXTENDED_B",
            "IPA_EXTENSIONS", "SPACING_MODIFIER_LETTERS", "COMBINING_DIACRITICAL_MARKS", "BASIC_GREEK",
            "GREEK_SYMBOLS_AND_COPTIC", "CYRILLIC", "ARMENIAN", "BASIC_HEBREW", "HEBREW_EXTENDED", "BASIC_ARABIC",
            "ARABIC_EXTENDED", "DEVANAGARI", "BENGALI", "GURMUKHI", "GUJARATI", "ORIYA", "TAMIL", "TELUGU", "KANNADA",
            "MALAYALAM", "THAI", "LAO", "BASIC_GEORGIAN", "GEORGIAN_EXTENDED", "HANGUL_JAMO",
            "LATIN_EXTENDED_ADDITIONAL", "GREEK_EXTENDED", "GENERAL_PUNCTUATION", "SUPERSCRIPTS_AND_SUBSCRIPTS",
            "CURRENCY_SYMBOLS", "COMBINING_DIACTRICAL_MARKS_FOR_SYMBOLS", "LETTERLIKE_SYMBOLS", "NUMBER_FORMS",
            "ARROWS", "MATHEMATICAL_OPERATORS", "MISCELLANEOUS_TECHNICAL", "CONTROL_PICTURES",
            "OPTICAL_CHARACTER_RECOGNITION", "ENCLOSED_ALPHANUMERICS", "BOX_DRAWING", "BLOCK_ELEMENTS",
            "GEOMETRICAL_SHAPES", "MISCELLANEOUS_SYMBOLS", "DINGBATS", "CJK_SYMBOLS_AND_PUNCTUATION", "HIRAGANA",
            "KATAKANA", "BOPOMOFO", "HANGUL_COMPATIBILITY_JAMO", "CJK_MISCELLANEOUS",
            "ENCLOSED_CJK_LETTERS_AND_MONTHS", "CJK_COMPATIBILITY", "HANGUL", "HANGUL_SUPPLEMENTARY_A",
            "HANGUL_SUPPLEMENTARY_B", "CJK_UNIFIED_IDEOGRAPHS", "PRIVATE_USE_AREA", "CJK_COMPATIBILITY_IDEOGRAPHS",
            "ALPHABETIC_PRESENTATION_FORMS_A", "ARABIC_PRESENTATION_FORMS_A", "COMBINING_HALF_MARKS",
            "CJK_COMPATIBILITY_FORMS", "SMALL_FORM_VARIANTS", "ARABIC_PRESENTATION_FORMS_B",
            "HALFWIDTH_AND_FULLWIDTH_FORMS", "SPECIALS", };

    /** Used to look up range id by name. */
    private static Hashtable hashByName;

    /** Used to look up range name by id. */
    private static Hashtable hashByValue;

    /**
     * Sets up the hashByName and hashByValue hashtables. These are used to get
     * the correct names.
     */
    private void setUpHashes() throws Exception
    {
        if (hashByValue == null)
        {
            hashByValue = new Hashtable();
            hashByName = new Hashtable();
            Field f[] = HFontCapabilities.class.getDeclaredFields();

            for (int i = 0; i < f.length; ++i)
            {
                int mods = f[i].getModifiers();
                if (Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods))
                {
                    hashByValue.put(f[i].get(null), f[i].getName());
                    hashByName.put(f[i].getName(), f[i].get(null));
                }
            }
        }
    }

    /**
     * Extension to BitSet which prints the Unicode range names for each bit in
     * toString().
     */
    private class MyBitSet extends BitSet
    {
        public String toString()
        {
            return getNames(this);
        }
    }

    /**
     * Returns a BitSet based on the given array of Unicode ranges.
     */
    private BitSet getBitSet(int[] values)
    {
        BitSet set = new MyBitSet();

        for (int i = 0; i < values.length; ++i)
            set.set(values[i]);
        return set;
    }

    /**
     * Returns a BitSet based on the given array of Unicode range names.
     */
    private BitSet getBitSet(String[] names)
    {
        BitSet set = new MyBitSet();

        for (int i = 0; i < names.length; ++i)
        {
            Integer val = (Integer) hashByName.get(names[i]);
            if (val == null) fail("Unknown Unicode Range: " + names[i]);
            if (val != null) set.set(val.intValue());
        }
        return set;
    }

    /**
     * Gets the names of Unicode ranges in the given BitSet, delimited by a '|'.
     */
    private String getNames(BitSet set)
    {
        StringBuffer buf = new StringBuffer();
        String or = "";
        for (int i = 0; i < set.size(); ++i)
        {
            if (set.get(i))
            {
                buf.append(or);
                buf.append((String) hashByValue.get(new Integer(i)));
                or = "|";
            }
        }
        return buf.toString();
    }

    /**
     * Returns the array of Unicode range names supported by the given font.
     */
    private String[] lookupFontCapabilities(String f)
    {
        String value = TestSupport.getProperty("HFontCapabilities." + f);

        if (value == null || value.equals("null")) return null;
        return getCharRanges(value);
    }

    /**
     * Splits the string representing Unicode range names into an array.
     */
    private String[] getCharRanges(String names)
    {
        String array[] = org.havi.ui.event.HEventRepresentationTest.split(names, "|", 10000);

        return array;
    }

    private interface TestCapabilities
    {
        public void test(String f);
    }

}
