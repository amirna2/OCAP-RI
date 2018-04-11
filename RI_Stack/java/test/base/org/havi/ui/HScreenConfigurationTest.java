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
import net.sourceforge.groboutils.junit.v1.iftc.*;
import org.w3c.dom.*;
import java.awt.Point;
import java.awt.Dimension;

/**
 * Tests {@link #HScreenConfiguration}.
 * 
 * @author Jay Tracy
 * @author Tom Henriksen
 * @author Aaron Kamienski
 * @version $Revision: 1.5 $, $Date: 2002/06/03 21:32:19 $
 */
public class HScreenConfigurationTest extends TestCase
{
    private static final boolean DEBUG = false;

    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends Object (by default)
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HScreenConfiguration.class, Object.class);
    }

    /**
     * Test that there are no public HScreenConfiguration constructors.
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HScreenConfiguration.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HScreenConfiguration.class);
    }

    /**
     * @return <code>NORM(Xu,Yu) = PXn + (Wn/Wu)*Xu, PYn + (Hn/Hu)*Yu</code>
     */
    static HScreenPoint normalize(Point p, HScreenConfiguration c)
    {
        HScreenRectangle area = c.getScreenArea();
        Dimension size = c.getPixelResolution();

        HScreenPoint result = new HScreenPoint(area.x + (area.width / size.width) * p.x, area.y
                + (area.height / size.height) * p.y);

        if (DEBUG)
        {
            System.out.println("NORM(Xu,Yu) = PXn + (Wn/Wu)*Xu, PYn + (Hn/Hu)*Yu");
            System.out.println("NORM(" + p.x + "," + p.y + ") = ");
            System.out.println("     " + area.x + " + (" + area.width + " / " + size.width + ") * " + p.x + ",");
            System.out.println("     " + area.y + " + (" + area.height + " / " + size.height + ") * " + p.y + ",");
            System.out.println("     = (" + result.x + ", " + result.y + ")");

            // What is the diff between
            // Hn/Hu * Yu
            // Hn * (Yu/Hu) = Hn / (Hu/Yu)
            System.out.println("Hn/Hu * Yu = (" + area.height + "/" + size.height + ") * " + p.y);
            try
            {
                System.out.println("           = (" + (area.height / size.height) + ") * " + p.y);
                System.out.println("           = " + ((area.height / size.height) * p.y));
            }
            catch (ArithmeticException e)
            {
            }
            System.out.println("Hn/(Hu/Yu) = " + area.height + "/(" + (size.height) + "/" + ((float) p.y) + ")");
            try
            {
                System.out.println("           = " + area.height + "/" + (size.height / (float) p.y));
                System.out.println("           = " + (area.height / (size.height / (float) p.y)));
            }
            catch (ArithmeticException e)
            {
            }
        }

        return result;
    }

    /**
     * @return <code>USER(Xn,Yn) = floor((Xn - PXn) * (Wu/Wn) + 0.5),
     *                             floor((Yn - PYn) * (Hu/Hn) + 0.5)</code>
     */
    static Point user(HScreenPoint p, HScreenConfiguration c)
    {
        HScreenRectangle area = c.getScreenArea();
        Dimension size = c.getPixelResolution();

        Point result = new Point((int) Math.floor((p.x - area.x) * (size.width / area.width) + 0.5),
                (int) Math.floor((p.y - area.y) * (size.height / area.height) + 0.5));

        if (DEBUG)
        {
            System.out.println("USER(Xn,Yn) = floor((Xn - PXn) * (Wu/Wn) + 0.5),");
            System.out.println("              floor((Yn - PYn) * (Hu/Hn) + 0.5)");
            System.out.println("USER(" + p.x + "," + p.y + ") 0 ");
            System.out.println("     floor((" + p.x + "-" + area.x + ") * (" + size.width + "/" + area.width
                    + " + 0.5)),");
            System.out.println("     floor((" + p.y + "-" + area.y + ") * (" + size.height + "/" + area.height
                    + " + 0.5))");
            System.out.println("     = (" + result.x + ", " + result.y + ")");
        }

        return result;
    }

    static HScreenDevice getDevice(HScreenConfiguration c)
    {
        return HScreenDeviceTest.getDevice(c);
    }

    /**
     * Instance test case. Used to test instances of HScreenConfiguration.
     */
    public static class InstanceTest extends InterfaceTestCase
    {
        public static interface DoTest
        {
            public void doTest(HScreen screen, HScreenDevice device, HScreenConfiguration config) throws Exception;
        }

        protected void foreach(DoTest test, HScreen screen, HScreenDevice devices[]) throws Exception
        {
            for (int i = 0; i < devices.length; ++i)
            {
                HScreenConfiguration[] configs = HScreenDeviceTest.getConfigs(devices[i]);
                for (int j = 0; j < configs.length; ++j)
                {
                    test.doTest(screen, devices[i], configs[j]);
                }
            }
        }

        protected void foreach(DoTest test) throws Exception
        {
            HScreen[] screens = HScreen.getHScreens();
            for (int i = 0; i < screens.length; ++i)
            {
                foreach(test, screens[i], screens[i].getHGraphicsDevices());
                foreach(test, screens[i], screens[i].getHVideoDevices());
                foreach(test, screens[i], screens[i].getHBackgroundDevices());
            }
        }

        private void fuzzyAssertEquals(String msg, Point p1, Point p2)
        {
            if (false)
                assertEquals(p1, p2);
            else
            {
                assertTrue(msg + " (x diff)", Math.abs(p1.x - p2.x) <= 1);
                assertTrue(msg + " (y diff)", Math.abs(p1.y - p2.y) <= 1);
            }
        }

        /**
         * Tests convertTo. Generate expected results by converting to screen
         * coords and then to alternate user coordinates.
         */
        private void checkConvertTo(Point startP, HScreen altScreen, HScreenDevice altDevice,
                HScreenConfiguration altConfig) throws Exception
        {
            Point p = config.convertTo(altConfig, startP);

            if (altConfig == HVideoDevice.NOT_CONTRIBUTING || config == HVideoDevice.NOT_CONTRIBUTING)
            {
                assertSame("Expected convertTo() to fail when involving NOT_CONTRIB", null, p);
            }
            else if (altScreen != screen)
            {
                assertNull("Expected null returned for different screen", p);
                assertNull("Expected same returned multiple calls", config.convertTo(altConfig, startP));
            }
            else
            {
                // Determine expected conversion
                fuzzyAssertEquals("Unexpected result", user(normalize(startP, config), altConfig), p);
                fuzzyAssertEquals("Expected same returned multiple calls", p, config.convertTo(altConfig, startP));
            }
        }

        /**
         * Test convertTo. Tests conversion of point from this config to given
         * alternate config.
         * 
         * @note !!!! should move convertTo and getOffset into a parameterized
         *       test (i.e., interface test). It screams for it!
         */
        public void testConvertTo() throws Exception
        {
            Dimension size = config.getPixelResolution();
            final Point origin = new Point(0, 0);
            final Point opposite = new Point(size.width - 1, size.height - 1);
            final Point center = new Point(size.width / 2, size.height / 2);
            class MyTest implements DoTest
            {
                public void doTest(HScreen altScreen, HScreenDevice altDevice, HScreenConfiguration altConfig)
                        throws Exception
                {
                    // Convert origin, opposite, and center to destination
                    // config
                    checkConvertTo(origin, altScreen, altDevice, altConfig);
                    checkConvertTo(opposite, altScreen, altDevice, altConfig);
                    checkConvertTo(center, altScreen, altDevice, altConfig);
                }
            }
            foreach(new MyTest());
        }

        /**
         * Test getOffset.
         */
        public void testGetOffset() throws Exception
        {
            // Given other configurations...
            // perform getOffset
            // Essentially, convert (0,0) in other config to this
            // configs coordinate space.
            // Expect to be same as convertTo with Point(0,0)
            // otherconfig.convertTo(thisconfig, (0,0))
            // want thisconfig's coordinate space

            class MyTest implements DoTest
            {
                public void doTest(HScreen altScreen, HScreenDevice altDevice, HScreenConfiguration altConfig)
                        throws Exception
                {
                    Dimension d = config.getOffset(altConfig);

                    if (altConfig == HVideoDevice.NOT_CONTRIBUTING || config == HVideoDevice.NOT_CONTRIBUTING)
                    {
                        assertSame("Expected getOffset() to fail when involving NOT_CONTRIB", null, d);
                    }
                    else if (altScreen != screen)
                    {
                        assertNull("Expected null returned for different screen", d);
                        assertSame("Expected same returned multiple calls", d, config.getOffset(altConfig));
                    }
                    else
                    {
                        Point ofs = altConfig.convertTo(config, new Point(0, 0));
                        assertEquals("Unexpected result", new Dimension(ofs.x, ofs.y), d);
                        assertEquals("Expected same returned multiple calls", d, config.getOffset(altConfig));
                    }
                }
            }
            foreach(new MyTest());
        }

        /**
         * Test getFlickerFilter.
         */
        public void testGetFlickerFilter()
        {
            // Verify against expected...
            assertEquals("getFlickerFilter() doesn't match expected value", test.getBoolean(xconfig, "flicker"),
                    config.getFlickerFilter());

            HScreenConfigTemplate template = HScreenDeviceTest.getTemplate(config);
            assertEquals(
                    "getFlickerFilter should match template value",
                    template.getPreferencePriority(HScreenConfigTemplate.FLICKER_FILTERING) == HScreenConfigTemplate.REQUIRED,
                    config.getFlickerFilter());
        }

        /**
         * Test getInterlaced.
         */
        public void testGetInterlaced()
        {
            // Verify against expected...
            assertEquals("getInterlaced() doesn't match expected value", test.getBoolean(xconfig, "interlaced"),
                    config.getInterlaced());
        }

        /**
         * Test getPixelAspectRatio.
         */
        public void testPixelAspectRatio()
        {
            // Verify against expected...
            assertEquals("Unexpected pixel aspect ratio", test.getDimension(xconfig, "pixelRatio"),
                    config.getPixelAspectRatio());
        }

        /**
         * Test getPixelResolution.
         */
        public void testPixelResolution()
        {
            // Verify against expected...
            assertEquals("Unexpected pixel resolution", test.getDimension(xconfig, "resolution"),
                    config.getPixelResolution());
        }

        /**
         * Test getScreenArea.
         */
        public void testScreenArea()
        {
            // Verify against expected...
            assertEquals("Unexpected screen area", test.getHRect(xconfig, "area"), config.getScreenArea());
        }

        /**
         * Tests getDevice() (of subclass).
         */
        public void testGetDevice()
        {
            assertSame("Unexpected device returned for configuration", device, getDevice(config));
        }

        static final int[] dontCare = { HScreenConfigTemplate.ZERO_BACKGROUND_IMPACT, // DONT_CARE
                HScreenConfigTemplate.ZERO_GRAPHICS_IMPACT, // DONT_CARE
                HScreenConfigTemplate.ZERO_VIDEO_IMPACT, // DONT_CARE
                HScreenConfigTemplate.VIDEO_GRAPHICS_PIXEL_ALIGNED, // DONT_CARE
                HVideoConfigTemplate.GRAPHICS_MIXING, // DONT_CARE
                HGraphicsConfigTemplate.VIDEO_MIXING, // DONT_CARE
        };

        static final int[] allPrefs = { HScreenConfigTemplate.INTERLACED_DISPLAY,
                HScreenConfigTemplate.FLICKER_FILTERING, HScreenConfigTemplate.PIXEL_ASPECT_RATIO,
                HScreenConfigTemplate.PIXEL_RESOLUTION, HScreenConfigTemplate.SCREEN_RECTANGLE,
                HGraphicsConfigTemplate.MATTE_SUPPORT, // gfx
                HGraphicsConfigTemplate.IMAGE_SCALING_SUPPORT, // gfx
                HBackgroundConfigTemplate.CHANGEABLE_SINGLE_COLOR, // bg
                HBackgroundConfigTemplate.STILL_IMAGE, // bg
        };

        /**
         * Tests getConfigTemplate() (of subclass).
         * <ul>
         * <li>does it select this configuration?
         * <li>does it match the exposed variables of this configuration
         * <li>all priorities are REQUIRED/REQUIRED_NOT/DONT_CARE
         * <li>certain prefs should always be DONT_CARE
         * <li>does it match generated template?
         * </ul>
         */
        public void testGetConfigTemplate()
        {
            HScreenConfigTemplate template = HScreenDeviceTest.getTemplate(config);
            HScreenConfigTemplate expected = test.toTemplate(xconfig);

            // Verify that template matches expected template
            // Check for always DONT_CARE
            for (int i = 0; i < dontCare.length; ++i)
            {
                try
                {
                    int prior = template.getPreferencePriority(dontCare[i]);
                    assertEquals("Expected priority to be DONT_CARE for pref=" + dontCare[i],
                            HScreenConfigTemplate.DONT_CARE, prior);
                    assertNull("Expected object to be null for pref=" + dontCare[i],
                            template.getPreferenceObject(dontCare[i]));
                }
                catch (IllegalArgumentException e)
                {
                }
            }

            // Check expected preferences
            for (int i = 0; i < allPrefs.length; ++i)
            {
                try
                {
                    assertEquals("Unexpected priority for pref=" + allPrefs[i],
                            expected.getPreferencePriority(allPrefs[i]), template.getPreferencePriority(allPrefs[i]));
                    Object obj = expected.getPreferenceObject(allPrefs[i]);
                    if (obj == null)
                        assertSame("Unexpected object for pref=" + allPrefs[i], obj,
                                template.getPreferenceObject(allPrefs[i]));
                    else
                        assertEquals("Unexpected object for pref=" + allPrefs[i], obj,
                                template.getPreferenceObject(allPrefs[i]));
                }
                catch (IllegalArgumentException e)
                {
                }
            }
        }

        /* ...Boilerplate... */

        public static InterfaceTestSuite isuite() throws Exception
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(InstanceTest.class);
            suite.setName(HScreenConfiguration.class.getName());
            return suite;
        }

        public InstanceTest(String name, ImplFactory f)
        {
            this(name, TestParam.class, f);
        }

        protected InstanceTest(String name, Class impl, ImplFactory f)
        {
            super(name, impl, f);
        }

        protected Element xconfig;

        protected HScreenConfiguration config;

        protected HScreenDevice device;

        protected HScreen screen;

        protected XMLDispConfig test;

        protected void setUp() throws Exception
        {
            super.setUp();

            TestParam param = (TestParam) createImplObject();
            test = param.test;
            xconfig = param.xconfig;
            device = param.device;
            screen = param.screen;
            if (param.config != null)
                config = param.config;
            else
            {
                config = HScreenDeviceTest.getBestConfig(device, test.toTemplate(xconfig));
                assertNotNull(device);
                assertNotNull(screen);
            }
            assertNotNull("Internal error - no XMLDispConfig", test);
            assertNotNull("Internal error - no XML config", xconfig);

            if (DEBUG)
            {
                if (config == null)
                {
                    System.out.println("Could not find config for:");
                    System.out.println(" device=" + device);
                    System.out.println(" xconfig=" + test.toString(xconfig));
                }
                else
                {
                    System.out.println("Found config for:");
                    System.out.println(" device=" + device);
                    System.out.println(" xconfig=" + test.toString(xconfig));
                }
            }
            assertNotNull("No config found to match XML: " + test.toString(xconfig), config);
        }

        protected void tearDown() throws Exception
        {
            if (device.isReserved())
            {
                device.releaseDevice();
            }

            xconfig = null;
            screen = null;
            device = null;
            config = null;
            test = null;

            super.tearDown();
        }

        /**
         * Type of object returned by ImplFactory.createImplObject().
         */
        public static class TestParam implements ImplFactory
        {
            public Element xconfig;

            public XMLDispConfig test;

            public HScreenDevice device;

            public HScreen screen;

            public HScreenConfiguration config; // for HVD.NOT_CONTRIBUTING

            public TestParam(HScreenDevice device, HScreen screen, Element xconfig, XMLDispConfig test)
            {
                this(null, device, screen, xconfig, test);
            }

            public TestParam(HScreenConfiguration config, HScreenDevice device, HScreen screen, Element xconfig,
                    XMLDispConfig test)
            {
                this.config = config;
                this.device = device;
                this.screen = screen;
                this.xconfig = xconfig;
                this.test = test;
            }

            public Object createImplObject()
            {
                return this;
            }

            public String toString()
            {
                String id = (xconfig != null) ? xconfig.getAttribute("id") : null;
                return (id == null) ? super.toString() : id;
            }
        }
    }

    /**
     * Used to implement suite() for subclass tests.
     */
    protected static Test suite(Class testClass, String type) throws Exception
    {
        return suite(InstanceTest.isuite(), testClass, type);
    }

    protected static Test suite(InterfaceTestSuite isuite, Class testClass, String type) throws Exception
    {
        TestSuite suite = new TestSuite(testClass);

        /* HScreenDeviceTest.InstanceTest foreach instance of HGraphicsDevice */
        HScreen screens[] = HScreen.getHScreens();
        if (screens != null)
        {
            XMLDispConfig test = new XMLDispConfig();
            Element xscreens[] = test.getScreens();
            // foreach screen
            boolean any = false;
            for (int i = 0; i < screens.length; ++i)
            {
                if (screens[i] != null)
                {
                    HScreenDevice[] devices = null;
                    if ("graphics".equals(type))
                        devices = screens[i].getHGraphicsDevices();
                    else if ("background".equals(type))
                        devices = screens[i].getHBackgroundDevices();
                    else if ("video".equals(type))
                        devices = screens[i].getHVideoDevices();
                    else
                        fail("Unknown type " + type);

                    // foreach device
                    for (int j = 0; j < devices.length; ++j)
                    {
                        String id = devices[i].getIDstring();
                        Element xdevice = test.findDevice(xscreens[i], id, type);
                        assertNotNull("Did not expect device of this name " + id, xdevice);
                        Element[] xconfigs = test.getConfigurations(xdevice);

                        // foreach config
                        for (int h = 0; h < xconfigs.length; ++h)
                        {
                            isuite.addFactory(new InstanceTest.TestParam(devices[j], screens[i], xconfigs[h], test));
                        }
                        any = true;
                    }
                }
            }
            if (any) suite.addTest(isuite);
        }
        return suite;
    }

    /**
     * Standard constructor.
     */
    public HScreenConfigurationTest(String str)
    {
        super(str);
    }

    public static Test suite() throws Exception
    {
        return new TestSuite(HScreenConfigurationTest.class);
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
