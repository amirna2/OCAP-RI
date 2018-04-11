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

package org.dvb.ui;

import junit.framework.*;
import java.awt.*;
import java.util.*;
import org.cablelabs.test.TestUtils;

/**
 * Tests DVBGraphics.
 * 
 * @todo Test other inherited graphics calls, including drawing operations.
 * @todo Test creation via Component.getGraphics() as well as
 *       DVBBufferedImage.createGraphics().
 * @todo Verify that DVBGraphics is passed to component paint methods.
 * @todo Expect full support from TYPE_ADVANCED-based DVBGraphics???
 * 
 * @author Aaron Kamienski
 */
public class DVBGraphicsTest extends TestCase
{
    /**
     * Verifies lack of public constructors.
     */
    public void testNoPublicConstructors() throws Exception
    {
        TestUtils.testNoPublicConstructors(DVBGraphics.class);
        TestUtils.testNoPublicFields(DVBGraphics.class);
    }

    /**
     * Tests getAvailableCompositeRules().
     */
    public void testGetAvailableCompositeRules()
    {
        DVBGraphics g = dvbgraphics;

        int[] rules = g.getAvailableCompositeRules();

        assertNotNull("Should not return null rules array");
        assertTrue("Rules array should be at least 3 in length", rules.length >= 3);

        BitSet ruleSet = new BitSet();
        for (int i = 0; i < rules.length; ++i)
        {
            assertFalse("Rules should not be specified more than once", ruleSet.get(rules[i]));

            ruleSet.set(rules[i]);
        }

        assertTrue("Expected SRC to be available", ruleSet.get(DVBAlphaComposite.SRC));
        assertTrue("Expected SRC_OVER to be available", ruleSet.get(DVBAlphaComposite.SRC_OVER));
        assertTrue("Expected CLR to be available", ruleSet.get(DVBAlphaComposite.CLEAR));

        if (g.getType() == DVBBufferedImage.TYPE_ADVANCED)
        {
            // Do we make sure the other modes are available?
        }

        // Should return the same over two calls
        int[] rules2 = g.getAvailableCompositeRules();
        assertFalse("Should not return same array -- as could be modified -- across two calls", rules == rules2);
        assertEquals("Should return same array length over two calls", rules.length, rules2.length);
        for (int i = 0; i < rules.length; ++i)
            assertEquals("Should return same array values over two calls", rules[i], rules2[i]);
    }

    // See MHP G.1.5
    private static final int opaqueGreys[] = { 42, 85, 170, 212 };

    private static final int opaqueReds[] = { 0, 63, 127, 191, 255 };

    private static final int opaqueGreens[] = { 0, 31, 63, 95, 127, 159, 191, 223, 255 };

    private static final int opaqueBlues[] = { 0, 127, 255 };

    private static final int transReds[] = { 0, 85, 170, 255 };

    private static final int transGreens[] = { 0, 51, 102, 153, 204, 255 };

    private static final int transBlues[] = { 0, 255 };

    /**
     * The minimum color palette that must be supported by MHP. According to
     * table G.1 in G.1.5.
     */
    private static Color perfectMatches[];

    /**
     * Constructs the minimal color palette.
     */
    protected Color[] getPerfectMatches()
    {
        if (perfectMatches == null)
        {
            Color[] t = new Color[opaqueGreys.length + opaqueReds.length * opaqueGreens.length * opaqueBlues.length
                    + transReds.length * transGreens.length * transBlues.length + 1];
            int index = 0;

            for (int i = 0; i < opaqueGreys.length; ++i)
                t[index++] = new Color(opaqueGreys[i], opaqueGreys[i], opaqueGreys[i]);
            for (int r = 0; r < opaqueReds.length; ++r)
                for (int g = 0; g < opaqueGreens.length; ++g)
                    for (int b = 0; b < opaqueBlues.length; ++b)
                        t[index++] = new Color(opaqueReds[r], opaqueGreens[g], opaqueBlues[b]);
            for (int r = 0; r < transReds.length; ++r)
                for (int g = 0; g < transGreens.length; ++g)
                    for (int b = 0; b < transBlues.length; ++b)
                        t[index++] = new DVBColor(transReds[r], transGreens[g], transBlues[b], 179);
            t[index++] = new DVBColor(0, 0, 0, 0);

            perfectMatches = t;
        }
        return perfectMatches;
    }

    /**
     * Tests getBestColorMatch().
     */
    public void testGetBestColorMatch()
    {
        DVBGraphics g = dvbgraphics;
        Color[] perfect = getPerfectMatches();

        for (int i = 0; i < perfect.length; ++i)
        {
            DVBColor c = g.getBestColorMatch(perfect[i]);
            assertNotNull("Should not return null DVBColor", c);
            assertEquals("Expected perfect match for " + perfect[i], perfect[i].getRGB(), c.getRGB());
        }
    }

    /**
     * Tests get/setColor().
     */
    public void testGetSetColor()
    {
        DVBGraphics g = dvbgraphics;
        Color[] perfect = getPerfectMatches();

        for (int i = 0; i < perfect.length; ++i)
        {
            g.setColor(perfect[i]);

            Color c = g.getColor();
            assertNotNull("getColor should not return null", c);
            assertTrue("getColor should always return a DVBColor", c instanceof DVBColor);
            assertEquals("Expected get color to be set color", perfect[i].getRGB(), c.getRGB());
        }
    }

    /**
     * Tests get/setDVBComposite().
     */
    public void testGetSetDVBComposite() throws Exception
    {
        DVBGraphics g = dvbgraphics;
        int[] rules = g.getAvailableCompositeRules();
        float[] alphas = { 1.0F, 0.3F, 0.0F };
        BitSet ruleSet = new BitSet();

        for (int a = 0; a < alphas.length; ++a)
        {
            for (int i = 0; i < rules.length; ++i)
            {
                ruleSet.set(rules[i]);
                DVBAlphaComposite rule = DVBAlphaComposite.getInstance(rules[i], alphas[a]);

                g.setDVBComposite(rule);
                DVBAlphaComposite rule2 = g.getDVBComposite();

                assertEquals("The get rule should be the set rule", rule.getRule(), rule2.getRule());
                assertEquals("The get alpha should be the set alpha", rule.getAlpha(), rule2.getAlpha(), 0.0F);
            }
        }

        // Unsupported rules (know that rules are in 1-8 range, go over by 1
        // each way)
        for (int i = 0; i < 9; ++i)
        {
            // Try unsupported rules
            if (!ruleSet.get(i))
            {
                DVBAlphaComposite rule;
                try
                {
                    rule = DVBAlphaComposite.getInstance(i);
                }
                catch (IllegalArgumentException e)
                {
                    // Ignore invalid rule
                    continue;
                }

                try
                {
                    g.setDVBComposite(rule);
                    fail("Expected UnsupportedDrawingOperationException for rule " + rule);
                }
                catch (UnsupportedDrawingOperationException e)
                {
                }
            }
        }

        // Null rule
        try
        {
            g.setDVBComposite(null);
            fail("Expected NullPointerException for null rule");
        }
        catch (NullPointerException e)
        {
        }

        assertNotNull("Should not return null following setDVBComposite(null)", g.getDVBComposite());
    }

    /**
     * Tests getType().
     */
    public void testGetType()
    {
        DVBGraphics g = dvbgraphics;
        int type = g.getType();
        assertTrue("getType() should return TYPE_BASE or TYPE_ADVANCED", type == DVBBufferedImage.TYPE_ADVANCED
                || type == DVBBufferedImage.TYPE_BASE);
        assertEquals("getType() should return same on successive calls", type, g.getType());
    }

    /**
     * Verifies that a double dispose() does not result in an exception. A
     * double dispose() could be expected if the finalize() method calls
     * dispose() in addition to a programmer calling dispose().
     */
    public void testDoubleDispose()
    {
        dvbgraphics.dispose();
        dvbgraphics.dispose();
    }

    protected DVBGraphics dvbgraphics;

    protected DVBBufferedImage dvbbufferedimage;

    protected void setUp() throws Exception
    {
        super.setUp();

        // We have two ways of creating the DVBGraphics...
        // One is from a displayable component
        // Other is from an offscreen DVBBufferedImage/Image
        // For now we'll do the latter
        dvbbufferedimage = new DVBBufferedImage(100, 100);
        dvbgraphics = dvbbufferedimage.createGraphics();
    }

    protected void tearDown() throws Exception
    {
        dvbgraphics.dispose();
        dvbbufferedimage.flush();
        dvbbufferedimage.dispose();
        dvbgraphics = null;
        dvbbufferedimage = null;
        super.tearDown();
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
        TestSuite suite = new TestSuite(DVBGraphicsTest.class);
        return suite;
    }

    public DVBGraphicsTest(String name)
    {
        super(name);
    }
}
