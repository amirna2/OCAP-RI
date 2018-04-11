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

package javax.tv.graphics;

import junit.framework.*;
import org.cablelabs.test.TestUtils;
import java.awt.Color;

/**
 * Tests AlphaColor.
 * 
 * @author Aaron Kamienski
 */
public class AlphaColorTest extends TestCase
{
    /**
     * Verifies heritage.
     */
    public void testHeritage()
    {
        TestUtils.testExtends(AlphaColor.class, Color.class);
    }

    protected AlphaColor construct(float r, float g, float b, float a)
    {
        return new AlphaColor(r, g, b, a);
    }

    protected AlphaColor construct(int r, int g, int b, int a)
    {
        return new AlphaColor(r, g, b, a);
    }

    protected AlphaColor construct(int argb, boolean hasAlpha)
    {
        return new AlphaColor(argb, hasAlpha);
    }

    protected AlphaColor construct(Color c)
    {
        return new AlphaColor(c);
    }

    protected Class testedClass()
    {
        return AlphaColor.class;
    }

    /**
     * Tests the AlphaColor(float,float,float,float) constructor.
     */
    public void testConstructorFloatRGBA()
    {
        float rf = 0.7F;
        float gf = 0.6F;
        float bf = 0.9F;
        float af = 0.5F;
        int ri = (int) (rf * 255);
        int gi = (int) (gf * 255);
        int bi = (int) (bf * 255);
        int ai = (int) (af * 255);
        AlphaColor c = construct(rf, gf, bf, af);

        checkConstructor("FloatRGBA: ", c, ri, gi, bi, ai);

        // Test illegal values
        float illegal[] = { -0.1F, 1.1F };
        for (int i = 0; i < illegal.length; ++i)
        {
            try
            {
                construct(illegal[i], gf, bf, af);
                fail("Expected IllegalArgumentException for illegal red:" + illegal[i]);
            }
            catch (IllegalArgumentException iae)
            {
            }
            try
            {
                construct(rf, illegal[i], bf, af);
                fail("Expected IllegalArgumentException for illegal green:" + illegal[i]);
            }
            catch (IllegalArgumentException iae)
            {
            }
            try
            {
                construct(rf, gf, illegal[i], af);
                fail("Expected IllegalArgumentException for illegal blue:" + illegal[i]);
            }
            catch (IllegalArgumentException iae)
            {
            }
            try
            {
                construct(rf, gf, bf, illegal[i]);
                fail("Expected IllegalArgumentException for illegal alpha:" + illegal[i]);
            }
            catch (IllegalArgumentException iae)
            {
            }
        }
    }

    /**
     * Tests the AlphaColor(int,int,int,int) constructor.
     */
    public void testConstructorIntRGBA()
    {
        int ri = 77;
        int gi = 66;
        int bi = 122;
        int ai = 99;
        AlphaColor c = construct(ri, gi, bi, ai);

        checkConstructor("IntRGBA: ", c, ri, gi, bi, ai);

        // Test illegal values
        int illegal[] = { -1, 256 };
        for (int i = 0; i < illegal.length; ++i)
        {
            try
            {
                construct(illegal[i], gi, bi, ai);
                fail("Expected IllegalArgumentException for illegal red: " + illegal[i]);
            }
            catch (IllegalArgumentException iae)
            {
            }
            try
            {
                construct(ri, illegal[i], bi, ai);
                fail("Expected IllegalArgumentException for illegal green: " + illegal[i]);
            }
            catch (IllegalArgumentException iae)
            {
            }
            try
            {
                construct(ri, gi, illegal[i], ai);
                fail("Expected IllegalArgumentException for illegal blue: " + illegal[i]);
            }
            catch (IllegalArgumentException iae)
            {
            }
            try
            {
                construct(ri, gi, bi, illegal[i]);
                fail("Expected IllegalArgumentException for illegal alpha: " + illegal[i]);
            }
            catch (IllegalArgumentException iae)
            {
            }
        }
    }

    /**
     * Tests the AlphaColor(int,boolean) constructor.
     */
    public void testConstructorIntBoolean()
    {
        int rgba = 0x12345678;
        int ri = (rgba >> 16) & 0xFF;
        int gi = (rgba >> 8) & 0xFF;
        int bi = (rgba >> 0) & 0xFF;
        int ai = (rgba >> 24) & 0xFF;
        for (int i = 0; i < 2; ++i)
        {
            boolean alpha = i == 0;
            AlphaColor c = construct(rgba, alpha);
            int a = alpha ? ai : 0xFF;

            checkConstructor("IntBoolean[" + alpha + "]: ", c, ri, gi, bi, a);
        }
    }

    public void testConstructorColor()
    {
        Color colors[] = { Color.lightGray, construct(80, 80, 80, 80), };
        for (int i = 0; i < colors.length; ++i)
        {
            int ri = colors[i].getRed();
            int gi = colors[i].getGreen();
            int bi = colors[i].getBlue();
            int ai = colors[i].getAlpha();
            AlphaColor c = construct(colors[i]);

            checkConstructor("Color[" + i + "]: ", c, ri, gi, bi, ai);
        }

        try
        {
            construct(null);
            fail("Expected a NullPointerException");
        }
        catch (NullPointerException e)
        {
        }
    }

    protected void checkConstructor(String msg, AlphaColor c, int ri, int gi, int bi, int ai)
    {
        assertEquals(msg + "Unexpected red value", ri, c.getRed());
        assertEquals(msg + "Unexpected green value", gi, c.getGreen());
        assertEquals(msg + "Unexpected blue value", bi, c.getBlue());
        assertEquals(msg + "Unexpected alpha value", ai, c.getAlpha());
        assertEquals(msg + "Unexpected RGB value", (ri << 16) | (gi << 8) | bi | (ai << 24), c.getRGB());

        String str = c.toString();
        assertNotNull("toString should not return null", str);
        assertEquals("toString should return same twice in a row", str, c.toString());
        // How can we interpret contents of toString?
    }

    /**
     * Tests brighter(). Should move color closer to white. Eventually, color
     * should be white. Unless it's a single color... e.g., just blue moves
     * closer to full blue.
     */
    public void testBrighter()
    {
        AlphaColor colors[] = { construct(Color.black), construct(255, 0, 0, 127), construct(0, 255, 0, 127),
                construct(0, 0, 255, 127), construct(Color.white) };
        for (int j = 0; j < colors.length; ++j)
        {
            AlphaColor c = colors[j];
            int r0, g0, b0;
            r0 = c.getRed();
            g0 = c.getGreen();
            b0 = c.getBlue();
            int rn = r0, gn = g0, bn = b0;
            for (int i = 0; i < 10; ++i)
            {
                Color b = c.brighter();
                assertTrue("brighter() should return instanceof same, not " + b.getClass(), testedClass().isInstance(b));
                AlphaColor bc = (AlphaColor) b;

                int r1, g1, b1;
                r1 = c.getRed();
                g1 = c.getGreen();
                b1 = c.getBlue();
                int r2, g2, b2, a2;
                rn = r2 = bc.getRed();
                gn = g2 = bc.getGreen();
                bn = b2 = bc.getBlue();
                a2 = bc.getAlpha();

                assertEquals("Alpha should be made opaque by brighter()", 0xFF, a2);
                assertTrue("Brighter red should be >= previous", r2 >= r1);
                assertTrue("Brighter green should be >= previous", g2 >= g1);
                assertTrue("Brighter blue should be >= previous", b2 >= b1);

                if (r1 == 255 && g1 == 255 && b1 == 255) break;
                c = bc;
            }
            /*
             * If all black, than goes toward gray. If not all black, then 0
             * stays zero and others march toward 255.
             */
            boolean black = r0 == 0 && g0 == 0 && b0 == 0;
            if (r0 < 255) if (black || r0 != 0)
                assertTrue("Final red (" + rn + ") should be brighter than original (" + r0 + ")", rn > r0);
            else
                assertEquals("Final red should still be zero", 0, rn);
            if (g0 < 255) if (black || g0 != 0)
                assertTrue("Final green (" + gn + ") should be brighter than original (" + g0 + ")", gn > g0);
            else
                assertEquals("Final green should still be zero", 0, gn);
            if (b0 < 255) if (black || b0 != 0)
                assertTrue("Final red (" + bn + ") should be brighter than original (" + b0 + ")", bn > b0);
            else
                assertEquals("Final red should still be zero", 0, bn);
        }
    }

    /**
     * Tests brighter(). Should move color closer to black. Eventually, color
     * should be black.
     */
    public void testDarker()
    {
        AlphaColor colors[] = { construct(Color.black), construct(255, 0, 0, 127), construct(0, 255, 0, 127),
                construct(0, 0, 255, 127), construct(Color.white) };
        for (int j = 0; j < colors.length; ++j)
        {
            AlphaColor c = colors[j];
            int r0, g0, b0;
            r0 = c.getRed();
            g0 = c.getGreen();
            b0 = c.getBlue();
            int rn = r0, gn = g0, bn = b0;
            for (int i = 0; i < 10; ++i)
            {
                Color b = c.darker();
                assertTrue("darker() should return instanceof same, not " + b.getClass(), testedClass().isInstance(b));
                AlphaColor bc = (AlphaColor) b;

                int r1, g1, b1;
                r1 = c.getRed();
                g1 = c.getGreen();
                b1 = c.getBlue();
                int r2, g2, b2, a2;
                rn = r2 = bc.getRed();
                gn = g2 = bc.getGreen();
                bn = b2 = bc.getBlue();
                a2 = bc.getAlpha();

                assertEquals("Alpha should be made opaque by darker()", 0xFF, a2);
                assertTrue("Darker red should be <= previous", r2 <= r1);
                assertTrue("Darker green should be <= previous", g2 <= g1);
                assertTrue("Darker blue should be <= previous", b2 <= b1);

                if (r1 == 0 && g1 == 0 && b1 == 0) break;
                c = bc;
            }
            if (r0 > 0) assertTrue("Final red should be darker than original", rn < r0);
            if (g0 > 0) assertTrue("Final green should be darker than original", gn < g0);
            if (b0 > 0) assertTrue("Final blue should be darker than original", bn < b0);
        }
    }

    /**
     * Tests equals(). Note that null should be valid and shouldn't throw an
     * exception! Also note that Color.equals(AlphaColor) will ignore the alpha!
     */
    public void testEqualsHashCode()
    {
        // Negative tests
        Color notEquals[] = { null, Color.black, Color.white, construct(Color.black), construct(Color.white),
                construct(0, 0, 0, 0), construct(255, 255, 255, 0), construct(255, 0, 0, 127),
                construct(0, 255, 0, 127), construct(0, 0, 255, 127), };
        for (int i = 3; i < notEquals.length; ++i)
        {
            for (int j = 0; j < notEquals.length; ++j)
            {
                if (i == j)
                {
                    assertTrue("Should be equal to self: " + notEquals[i], notEquals[i].equals(notEquals[j]));
                }
                else
                {
                    assertFalse("Should not be equals: " + notEquals[i] + " and " + notEquals[j],
                            notEquals[i].equals(notEquals[j]));
                }
            }
        }

        // Positive tests
        Color equals[][] = {
                { construct(Color.black), construct(0, 0, 0, 255), construct(0F, 0F, 0F, 1F),
                        construct(0xFF000000, true), construct(0, false) },
                { construct(Color.white), construct(255, 255, 255, 255), construct(1F, 1F, 1F, 1F),
                        construct(0xFFFFFFFF, true), construct(0x00FFFFFF, false), },
                { construct(Color.red), construct(255, 0, 0, 255), construct(1F, 0F, 0F, 1F),
                        construct(0xFFFF0000, true), construct(0x00FF0000, false), },
                { construct(construct(0, 255, 0, 127)), construct(0, 255, 0, 127), construct(0F, 1F, 0F, 0.5F),
                        construct(0x7F00FF00, true), }, };
        for (int i = 0; i < equals.length; ++i)
        {
            for (int j = 0; j < equals[i].length; ++j)
            {
                for (int k = j; k < equals[i].length; ++k)
                {
                    assertEquals("Should be equal", equals[i][j], equals[i][k]);
                    assertEquals("Should have equal hashcodes", equals[i][j].hashCode(), equals[i][k].hashCode());
                }

                AlphaColor color2 = construct(equals[i][j]);
                assertEquals("Should equal color created from same", equals[i][j], color2);
                assertEquals("Should equal color created from same (reflexive)", color2, equals[i][j]);
                assertEquals("Should have equal hashcodes", equals[i][j].hashCode(), color2.hashCode());
            }
        }
    }

    /* These are tested in constructors. */
    /*
     * public void testGetAlpha() { fail("Unimplemented test"); }
     * 
     * public void testGetRGB() { fail("Unimplemented test"); }
     * 
     * public void testToString() { fail("Unimplemented test"); }
     */

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
        TestSuite suite = new TestSuite(AlphaColorTest.class);
        return suite;
    }

    public AlphaColorTest(String name)
    {
        super(name);
    }
}
