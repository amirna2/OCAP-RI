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
import javax.tv.graphics.*;
import org.cablelabs.test.TestUtils;

/**
 * Tests DVBAlphaComposite.
 * 
 * @author Aaron Kamienski
 */
public class DVBAlphaCompositeTest extends TestCase
{
    private static final String intFieldNames[] = { "CLEAR", "SRC", "SRC_OVER", "DST_OVER", "SRC_IN", "DST_IN",
            "SRC_OUT", "DST_OUT" };

    private static final int intFieldValues[] = { 1, 2, 3, 4, 5, 6, 7, 8 };

    private static final String dvbFieldNames[] = { "Clear", "Src", "SrcOver", "DstOver", "SrcIn", "DstIn", "SrcOut",
            "DstOut" };

    private static final DVBAlphaComposite dvbFieldMatches[] = { DVBAlphaComposite.Clear, DVBAlphaComposite.Src,
            DVBAlphaComposite.SrcOver, DVBAlphaComposite.DstOver, DVBAlphaComposite.SrcIn, DVBAlphaComposite.DstIn,
            DVBAlphaComposite.SrcOut, DVBAlphaComposite.DstOut };

    /**
     * Tests for the expected public fields.
     */
    public void testPublicFields()
    {
        TestUtils.testPublicFields(DVBAlphaComposite.class, intFieldNames, int.class);
        TestUtils.testPublicFields(DVBAlphaComposite.class, dvbFieldNames, DVBAlphaComposite.class);
        TestUtils.testFieldValues(DVBAlphaComposite.class, intFieldNames, intFieldValues);
    }

    /**
     * Verifies that there are no public constructors.
     */
    public void testNoPublicConstructors()
    {
        TestUtils.testNoPublicConstructors(DVBAlphaComposite.class);
    }

    /**
     * Tests getInstance().
     */
    public void testGetInstance()
    {
        float alphas[] = { 0.0F, 0.3F, 0.5F, 0.7F, 1.0F };

        // No specified alpha
        for (int i = 0; i < intFieldValues.length; ++i)
        {
            DVBAlphaComposite dvb = DVBAlphaComposite.getInstance(intFieldValues[i]);
            assertNotNull("A composite object should be returned (" + i + ")", dvb);
            assertEquals("Expected a composite object with given rule", intFieldValues[i], dvb.getRule());
            assertEquals("Expected a composite object with opaque alpha", 1.0F, dvb.getAlpha(), 0.0F);

        }

        // Specified alpha
        for (int i = 0; i < intFieldValues.length; ++i)
        {
            DVBAlphaComposite dvb = DVBAlphaComposite.getInstance(intFieldValues[i]);
            for (int j = 0; j < alphas.length; ++j)
            {
                dvb = DVBAlphaComposite.getInstance(intFieldValues[i], alphas[j]);
                assertNotNull("A composite object should be returned (" + i + ")", dvb);
                assertEquals("Expected a composite object with given rule", intFieldValues[i], dvb.getRule());
                assertEquals("Expected a composite object with given alpha", alphas[j], dvb.getAlpha(), 0.0F);

            }
        }
    }

    /**
     * Tests getAlpha(). <i>Kind of overlaps with testGetInstance...</i>
     */
    public void testGetAlpha()
    {
        DVBAlphaComposite dvb = DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC);
        assertEquals("Expected getAlpha to return opaque alpha", 1.0F, dvb.getAlpha(), 0.0F);

        for (int i = 0; i <= 10; ++i)
        {
            float alpha = 0.1F * i;

            dvb = DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC, alpha);
            assertEquals("Expected getAlpha to return specified alpha", alpha, dvb.getAlpha(), 0.0F);
        }
        for (int i = 0; i < dvbFieldMatches.length; ++i)
        {
            dvb = dvbFieldMatches[i];
            assertEquals("Expected getAlpha to return opaque alpha", 1.0F, dvb.getAlpha(), 0.0F);
        }
    }

    /**
     * Tests getRule(). <i>Kind of overlaps with testGetInstance...</i>
     */
    public void testGetRule()
    {
        for (int i = 0; i < dvbFieldMatches.length; ++i)
        {
            DVBAlphaComposite dvb = dvbFieldMatches[i];
            assertEquals("Expected getRule() to return matching rule", intFieldValues[i], dvb.getRule());
        }
    }

    /**
     * Tests equals().
     */
    public void testEquals()
    {
        // Compare to self, equivalent
        for (int i = 0; i < intFieldValues.length; ++i)
        {
            // Equal to self
            DVBAlphaComposite dvb = DVBAlphaComposite.getInstance(intFieldValues[i]);
            assertTrue("Should be equal to self: " + dvb, dvb.equals(dvb));

            // Equal to equivalent
            DVBAlphaComposite dvb2 = DVBAlphaComposite.getInstance(intFieldValues[i]);
            assertTrue("Should be equal to equivalent: " + dvb, dvb.equals(dvb2));
            assertTrue("Should be equal to equivalent (reversed)", dvb2.equals(dvb));

            // Equal to equivalent with alpha
            dvb2 = DVBAlphaComposite.getInstance(intFieldValues[i], 1.0F);
            assertTrue("Should be equal to equivalent (with alpha): " + dvb, dvb.equals(dvb2));
            assertTrue("Should be equal to equivalent (with alpha, reversed): " + dvb, dvb2.equals(dvb));

            // Equal to equivalent field
            dvb2 = dvbFieldMatches[i];
            assertTrue("Should be equal to static field version: " + dvb, dvb.equals(dvb2));
            assertTrue("Should be equal to static field version (reversed): " + dvb, dvb2.equals(dvb));

            // Not equal to null
            assertFalse("Should not be equal to null: " + dvb, dvb.equals(null));

            // Not equal with different alpha
            dvb2 = DVBAlphaComposite.getInstance(intFieldValues[i], 0.5F);
            assertFalse("Should not be equal if alpha is different: " + dvb, dvb.equals(dvb2));
            assertFalse("Should not be equal if alpha is different (reversed): " + dvb, dvb2.equals(dvb));

            // Not equal with different rule/alpha
            for (int j = 0; j < intFieldValues.length; ++j)
            {
                if (j == i) continue;
                dvb2 = DVBAlphaComposite.getInstance(intFieldValues[j]);
                assertFalse("Should not be equal if rule is different: " + dvb, dvb.equals(dvb2));
                dvb2 = DVBAlphaComposite.getInstance(intFieldValues[j], 0.5F);
                assertFalse("Should not be equal if rule and alpha are different: " + dvb, dvb.equals(dvb2));
            }

            // Now do same with alpha
            dvb = DVBAlphaComposite.getInstance(intFieldValues[i], 0.5F);
            assertTrue("Should be equal to self (0.5): " + dvb, dvb.equals(dvb));

            // Equal to equivalent
            dvb2 = DVBAlphaComposite.getInstance(intFieldValues[i], 0.5F);
            assertTrue("Should be equal to equivalent (0.5): " + dvb, dvb.equals(dvb2));
            assertTrue("Should be equal to equivalent (0.5/reversed)", dvb2.equals(dvb));
        }
    }

    /**
     * Tests hashCode(). Should be defined if equals().
     */
    public void testHashCode()
    {
        DVBAlphaComposite rules[] = { DVBAlphaComposite.Src, DVBAlphaComposite.SrcOver, DVBAlphaComposite.Clear,
                DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC),
                DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC_OVER),
                DVBAlphaComposite.getInstance(DVBAlphaComposite.CLEAR),
                DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC, 1.0F),
                DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC_OVER, 1.0F),
                DVBAlphaComposite.getInstance(DVBAlphaComposite.CLEAR, 1.0F),
                DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC, 0.7F),
                DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC_OVER, 0.3F),
                DVBAlphaComposite.getInstance(DVBAlphaComposite.CLEAR, 0.1F),
                DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC, 0.7F),
                DVBAlphaComposite.getInstance(DVBAlphaComposite.SRC_OVER, 0.3F),
                DVBAlphaComposite.getInstance(DVBAlphaComposite.CLEAR, 0.1F), };

        for (int i = 0; i < rules.length; ++i)
            for (int j = 0; j < rules.length; ++j)
            {
                if (rules[i].equals(rules[j]))
                    assertEquals("If equals()==true, then hashCode should be the same", rules[i].hashCode(),
                            rules[j].hashCode());
                /*
                 * else
                 * assertTrue("If equals()==false, expect hashCode to be different"
                 * , rules[i].hashCode()!=rules[j].hashCode());
                 */
            }
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
        TestSuite suite = new TestSuite(DVBAlphaCompositeTest.class);
        return suite;
    }

    public DVBAlphaCompositeTest(String name)
    {
        super(name);
    }
}
