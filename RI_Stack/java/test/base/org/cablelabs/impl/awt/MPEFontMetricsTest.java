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

/*
 * Created on Apr 21, 2006
 */
package org.cablelabs.impl.awt;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Toolkit;
import java.util.Hashtable;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests MPEFontMetrics.
 * 
 * @author Aaron Kamienski
 */
public class MPEFontMetricsTest extends TestCase
{
    private static final String STANDARD_NAME = "SansSerif";

    private static Font[] makeStandardFonts()
    {
        Font[] fonts = { new Font(STANDARD_NAME, Font.PLAIN, 24), new Font(STANDARD_NAME, Font.PLAIN, 26),
                new Font(STANDARD_NAME, Font.PLAIN, 31), new Font(STANDARD_NAME, Font.PLAIN, 36), };
        return fonts;
    }

    /**
     * Targeting OCAP, we know that the standard fonts should be supported.
     * Namely: Tiresias PLAIN in 24, 26, 31, 36.
     */
    public void testStandardFonts()
    {
        Font[] fonts = makeStandardFonts();
        Hashtable fms = new Hashtable();

        for (int i = 0; i < fonts.length; ++i)
        {
            FontMetrics fm = tk.getFontMetrics(fonts[i]);
            assertNotNull("FontMetrics should always be returned", fm);
            assertNull("Did not expect FM to be returned already for " + fonts[i], fms.get(fm));
            fms.put(fm, fonts[i]);

            assertSame("Expected same instance returned again", fm, tk.getFontMetrics(fonts[i]));
        }
    }

    /**
     * Asking for the same font, an identical FontMetrics should be returned.
     */
    public void testMPEFontMetricsCache()
    {
        Font f1 = new Font(STANDARD_NAME, Font.PLAIN, 27);
        Font f2 = new Font(STANDARD_NAME, Font.PLAIN, 27);

        assertSame("Expected same FontMetrics for equivalent fonts", tk.getFontMetrics(f1), tk.getFontMetrics(f2));
    }

    /**
     * Asking for same font, after cache has been purged.
     */
    public void testMPEFontMetricsCache_purge()
    {
        Font f1 = new Font(STANDARD_NAME, Font.PLAIN, 28);
        FontMetrics fm1 = tk.getFontMetrics(f1);

        int hash1 = System.identityHashCode(fm1);

        Font f2 = new Font(STANDARD_NAME, Font.PLAIN, 28);
        FontMetrics fm2 = tk.getFontMetrics(f2);
        int hash2 = System.identityHashCode(fm2);

        assertEquals("Expected same FontMetrics for equivalent fonts", hash1, hash2);

        // Force fonts to be collected
        f1 = null;
        fm1 = null;
        f2 = null;
        fm2 = null;
        gc();

        Font f3 = new Font(STANDARD_NAME, Font.PLAIN, 28);
        FontMetrics fm3 = tk.getFontMetrics(f3);
        int hash3 = System.identityHashCode(fm3);

        assertFalse("Expected different FontMetrics following collection of old fonts", hash1 == hash3);
    }

    /**
     * For all required fonts, expect that native font is different for each.
     * <p>
     * <b>NOTE: This test requires that all code (including tests) go on boot
     * classpath!</b>
     */
    public void testMPEFontMetrics_NativeFont()
    {
        Font[] fonts = makeStandardFonts();
        Hashtable nativeFonts = new Hashtable();
        for (int i = 0; i < fonts.length; ++i)
        {
            MPEFontMetrics fm = (MPEFontMetrics) tk.getFontMetrics(fonts[i]);
            Integer key = new Integer(fm.nativeFont);
            assertNull("Did not expect native font to be returned already for " + fonts[i], nativeFonts.get(key));
            nativeFonts.put(key, fonts[i]);
        }
    }

    /**
     * This test expects that styled versions of the native fonts aren't
     * supported. This may or may not be true. Assuming that they aren't, this
     * test checks for "fallback" to same-sized PLAIN instances.
     * <p>
     * <b>NOTE: This test requires that all code (including tests) go on boot
     * classpath!</b>
     */
    public void testMPEFontMetrics_NativeFont_fuzzy_Standard()
    {
        // First get the standard native fonts...
        Font[] fonts = makeStandardFonts();
        Hashtable nativeFonts = new Hashtable();
        for (int i = 0; i < fonts.length; ++i)
        {
            MPEFontMetrics fm = (MPEFontMetrics) tk.getFontMetrics(fonts[i]);
            Integer key = new Integer(fm.nativeFont);
            assertNull("Did not expect native font to be returned already for " + fonts[i], nativeFonts.get(key));
            nativeFonts.put(key, fonts[i]);
        }

        // Now get the BOLD, ITALIC, and BOLD+ITALIC versions
        for (int i = 0; i < fonts.length; ++i)
        {
            Font[] styled = { new Font(STANDARD_NAME, Font.BOLD, fonts[i].getSize()),
                    new Font(STANDARD_NAME, Font.ITALIC, fonts[i].getSize()),
                    new Font(STANDARD_NAME, Font.BOLD + Font.ITALIC, fonts[i].getSize()) };
            for (int j = 0; j < styled.length; ++j)
            {
                // Expect the native font to be the same as the one previously
                // returned for the exact match
                MPEFontMetrics fm = (MPEFontMetrics) tk.getFontMetrics(styled[j]);
                Integer key = new Integer(fm.nativeFont);
                assertNotNull("Expected this native font to be seen already for " + styled[j], nativeFonts.get(key));
                assertSame("Expected this native font to be that for a standard font " + fonts[i], fonts[i],
                        nativeFonts.get(key));

                // Check MPEFontMetrics cache while we're here
                // Don't expect to be same as FontMetrics returned for original
                // font, though
                assertSame("Expected FontMetrics to be cached for this font", fm, tk.getFontMetrics(styled[j]));
            }
        }
    }

    /**
     * Tests that unknown fonts fallback to same-sized fonts from the standard
     * set.
     * <p>
     * <b>NOTE: This test requires that all code (including tests) go on boot
     * classpath!</b>
     */
    public void testMPEFontMetrics_NativeFont_fuzzy_Unknown()
    {
        // First get the standard native fonts...
        Font[] fonts = makeStandardFonts();
        Hashtable nativeFonts = new Hashtable();
        for (int i = 0; i < fonts.length; ++i)
        {
            MPEFontMetrics fm = (MPEFontMetrics) tk.getFontMetrics(fonts[i]);
            Integer key = new Integer(fm.nativeFont);
            assertNull("Did not expect native font to be returned already for " + fonts[i], nativeFonts.get(key));
            nativeFonts.put(key, fonts[i]);
        }

        // Now get the Other named fonts
        for (int i = 0; i < fonts.length; ++i)
        {
            Font[] other = { new Font("NoSuchFont1", fonts[i].getStyle(), fonts[i].getSize()),
                    new Font("NoSuchFont2", fonts[i].getStyle(), fonts[i].getSize()), };
            for (int j = 0; j < other.length; ++j)
            {
                // Expect the native font to be the same as the one previously
                // returned for the exact match
                MPEFontMetrics fm = (MPEFontMetrics) tk.getFontMetrics(other[j]);
                Integer key = new Integer(fm.nativeFont);
                assertNotNull("Expected this native font to be seen already for " + other[j], nativeFonts.get(key));
                assertSame("Expected this native font to be that for a standard font " + fonts[i], fonts[i],
                        nativeFonts.get(key));

                // Check MPEFontMetrics cache while we're here
                // Don't expect to be same as FontMetrics returned for original
                // font, though
                assertSame("Expected FontMetrics to be cached for this font", fm, tk.getFontMetrics(other[j]));
            }
        }
    }

    private static void gc()
    {
        for (int i = 0; i < 2; ++i)
        {
            System.gc();
            System.runFinalization();
        }
        System.gc();
    }

    /*  ********** Boilerplate ************* */

    private Toolkit tk;

    protected void setUp() throws Exception
    {
        tk = Toolkit.getDefaultToolkit();
    }

    public MPEFontMetricsTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(MPEFontMetricsTest.class);
        return suite;
    }

}
