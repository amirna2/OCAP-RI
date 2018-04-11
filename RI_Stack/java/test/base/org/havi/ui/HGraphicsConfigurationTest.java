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
import net.sourceforge.groboutils.junit.v1.iftc.*;

/**
 * Tests {@link #HGraphicsConfiguration}.
 * 
 * @author Jay Tracy
 * @author Tom Henriksen
 * @author Todd Earles
 * @author Aaron Kamienski
 */
public class HGraphicsConfigurationTest extends HScreenConfigurationTest
{
    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HScreenConfiguration
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HGraphicsConfiguration.class, HScreenConfiguration.class);
    }

    /**
     * Test the constructor of HGraphicsConfiguration.
     * <ul>
     * <li>HGraphicsConfiguration()
     * </ul>
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HGraphicsConfiguration.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HGraphicsConfiguration.class);
    }

    /**
     * Tests instances of HGraphicsConfiguration.
     */
    public static class GfxInstanceTest extends InstanceTest
    {
        /**
         * Tests the variations of the
         * <code>getPunchThroughToBackgroundColor</code> method. They are:
         * <ul>
         * <li>getPunchThroughToBackgroundColor(int percentage)
         * <li>getPunchThroughToBackgroundColor(int percentage, HVideoDevice
         * hvd)
         * <li>getPunchThroughToBackgroundColor(Color color, int percentage)
         * <li>getPunchThroughToBackgroundColor(Color color, int percentage,
         * HVideoDevice v)
         * </ul>
         */
        // Don't bother testing as apps aren't supposed to use in MHP/OCAP...
        /*
         * public void testPunchThroughToBackgroundColor() {
         * fail("Unimplemented test"); }
         */

        /**
         * Tests the dispose method.
         */
        // Don't bother testing as apps aren't supposed to use in MHP/OCAP...
        /*
         * public void testDispose() { fail("Unimplemented test"); }
         */

        /**
         * Tests getAllFonts.
         */
        public void testAllFonts()
        {
            Font[] fonts = ((HGraphicsConfiguration) config).getAllFonts();

            assertNotNull("getAllFonts should never return a null array", fonts);
            assertTrue("getAllFonts should return an array of at least 1 font", fonts.length >= 1);

            // Ensure that at least SanSerif/Tiresias is available
            boolean sansserif = false;
            boolean tiresias = false;
            for (int i = 0; i < fonts.length; ++i)
            {
                assertNotNull(i + "th entry in fonts[] is null", fonts[i]);

                String name = fonts[i].getName().toLowerCase();
                sansserif = sansserif || "sansserif".equals(name);
                tiresias = tiresias || "tiresias".equals(name);
            }
            assertTrue("SansSerif/Tiresias must be available", sansserif || tiresias);

            // ???
            // If there are more than one font, ensure that "standard" names are
            // there
        }

        /**
         * Tests getCompatibleImage.
         * <ul>
         * <li>test that we get SOMETHING back.
         * </ul>
         */
        public void testCompatibleImage()
        {
            // load image
            Image testImg = TestSupport.getArrow(1);

            assertNotNull("null returned from testCompatibleImage",
                    ((HGraphicsConfiguration) config).getCompatibleImage(testImg, null));
        }

        /**
         * Tests getComponentHScreenRectangle.
         */
        public void X_testComponentHScreenRectangle()
        {
            // dependant on associated HGraphicsDevice and setup HScene
            fail("Unimplemented test");

            // select configuration into graphics device
            // create an HScene (full-screen, bigger, smaller)
            // add a component (full-scene, bigger, smaller)
            // verify screen rectangle

            // First find absolute location of component in scene
            // Next find absolute location of scene on screen
            // Next convert that location to HScreenRectangle
        }

        /**
         * Tests getPixelCoordinatesHScreenRectangle.
         */
        public void X_testPixelCoordinatesHScreenRectangle()
        {
            // dependant on associated HGraphicsDevice and setup HScene
            fail("Unimplemented test");
        }

        /* ...Boilerplate... */

        public static InterfaceTestSuite isuite() throws Exception
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(GfxInstanceTest.class);
            suite.setName(HGraphicsConfiguration.class.getName());
            return suite;
        }

        public GfxInstanceTest(String name, ImplFactory f)
        {
            this(name, TestParam.class, f);
        }

        protected GfxInstanceTest(String name, Class impl, ImplFactory f)
        {
            super(name, impl, f);
        }
    }

    /**
     * Standard constructor.
     */
    public HGraphicsConfigurationTest(String str)
    {
        super(str);
    }

    public static Test suite() throws Exception
    {
        return suite(GfxInstanceTest.isuite(), HGraphicsConfigurationTest.class, "graphics");
    }

    /**
     * Standalone runner.
     */
    public static void main(String args[])
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
}
