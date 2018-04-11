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

import org.w3c.dom.Element;

import junit.framework.*;

import org.cablelabs.test.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;

/**
 * Tests {@link #HVideoDevice}.
 * 
 * @author Tom Henriksen
 * @author Aaron Kamienski
 * @version $Revision: 1.7 $, $Date: 2002/06/03 21:32:23 $
 */
public class HVideoDeviceTest extends HScreenDeviceTest
{
    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HScreenDevice
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HVideoDevice.class, HScreenDevice.class);
    }

    /**
     * Test that there are no public HVideoDevice constructors.
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HVideoDevice.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HVideoDevice.class);
    }

    /**
     * Tests NOT_CONTRIBUTING.
     */
    public void testNOT_CONTRIBUTING()
    {
        XMLDispConfig test = new XMLDispConfig();

        if (!test.hasNotContribVideo())
            assertSame("Expected null NOT_CONTRIBUTING", null, HVideoDevice.NOT_CONTRIBUTING);
        else
        {
            assertNotNull("Expected non-null NOT_CONTRIBUTING", HVideoDevice.NOT_CONTRIBUTING);

            assertEquals("Expected (0,0) pixel aspect ratio", new java.awt.Dimension(0, 0),
                    HVideoDevice.NOT_CONTRIBUTING.getPixelAspectRatio());
        }
    }

    private static final String allFields[] = { "FLICKER_FILTERING", "INTERLACED_DISPLAY", "PIXEL_ASPECT_RATIO",
            "PIXEL_RESOLUTION", "SCREEN_RECTANGLE", "VIDEO_GRAPHICS_PIXEL_ALIGNED", "ZERO_GRAPHICS_IMPACT",
            "ZERO_VIDEO_IMPACT", "GRAPHICS_MIXING" };

    private static final int allPrefs[] = { HVideoConfigTemplate.FLICKER_FILTERING,
            HVideoConfigTemplate.INTERLACED_DISPLAY, HVideoConfigTemplate.PIXEL_ASPECT_RATIO,
            HVideoConfigTemplate.PIXEL_RESOLUTION, HVideoConfigTemplate.SCREEN_RECTANGLE,
            HVideoConfigTemplate.VIDEO_GRAPHICS_PIXEL_ALIGNED, HVideoConfigTemplate.ZERO_GRAPHICS_IMPACT,
            HVideoConfigTemplate.ZERO_VIDEO_IMPACT, HVideoConfigTemplate.GRAPHICS_MIXING };

    final static boolean[] isObjPref = { false, false, true, true, true, true, false, false, true };

    public static class VideoInstanceTest extends InstanceTest
    {
        /**
         * Test getVideoSource.
         * <ul>
         * <li>Expect an OcapLocator.
         * </ul>
         */
        public void testVideoSource() throws Exception
        {
            HVideoDevice device = (HVideoDevice) this.device;
            Object vidSrc = device.getVideoSource();

            if (vidSrc != null)
            {
                // Implementation specific verification code goes here.
                assertTrue("VideoSource should be an OcapLocator", vidSrc instanceof org.ocap.net.OcapLocator);
            }
        }

        /**
         * Test getVideoController.
         * <ul>
         * <li>Excpect a JMF Player.
         * </ul>
         */
        public void testVideoController() throws Exception
        {
            HVideoDevice device = (HVideoDevice) this.device;
            Object vidController = device.getVideoController();

            if (vidController != null)
            {
                // Implementation specific verification code goes here...
                // For OCAP this will be a JMF player (or nothing at all)
                assertTrue("VideoController should be JMF player", vidController instanceof javax.media.Player);
            }
        }

        /**
         * Test getConfigurations() should not include NOT_CONTRIBUTING.
         */
        public void testConfigurations_NOTCONTRIB()
        {
            if (HVideoDevice.NOT_CONTRIBUTING == null) return;

            HScreenConfiguration[] configs = getConfigs(device);

            for (int i = 0; i < configs.length; ++i)
            {
                assertFalse("NOT_CONTRIBUTING shouldn't be returned by getConfigs",
                        configs[i] == HVideoDevice.NOT_CONTRIBUTING);
            }
        }

        /**
         * Test getCurrentConfiguration() should return NOT_CONTRIBUTING if
         * non-null and a still background configuration is in force.
         */
        public void testCurrentConfiguration_NOTCONTRIB() throws Exception
        {
            // Assume correct value of NOT_CONTRIBUTING tested elsewhere
            if (HVideoDevice.NOT_CONTRIBUTING == null) return;

            // Find a BG configuration that supports still
            HScreen screen = getHScreen(device);
            HBackgroundDevice bgDevice = screen.getDefaultHBackgroundDevice();
            // Assume correct value of default BG device tested elsewhere
            if (bgDevice == null) return;

            HBackgroundConfigTemplate t = new HBackgroundConfigTemplate();
            t.setPreference(HBackgroundConfigTemplate.STILL_IMAGE, HBackgroundConfigTemplate.REQUIRED);
            HBackgroundConfiguration bg = bgDevice.getBestConfiguration(t);
            // Assume still images supported if NOT_CONTRIBUTING != null
            assertNotNull("Assume that a still image configuration be supported", bg);
            assertTrue("Should've been able to reserve the device", bgDevice.reserveDevice(new TestResourceClient()));
            try
            // reserve/release
            {
                // Set still image config
                setConfig(bgDevice, bg);

                assertNotNull("Expected non-null configuration", getCurrConfig(device));

                if (test.doesDeviceUseNotContrib(xdevice))
                    assertSame("Expected NOT_CONTRIBUTING to be current config for device " + device.getIDstring(),
                            HVideoDevice.NOT_CONTRIBUTING, getCurrConfig(device));
                else
                    assertNotSame("Expected config other than NOT_CONTRIBUTING to be current for device "
                            + device.getIDstring(), HVideoDevice.NOT_CONTRIBUTING, getCurrConfig(device));
            }
            finally
            {
                bgDevice.releaseDevice();
            }
        }

        /**
         * Should not be able to set NOT_CONTRIBUTING as current config.
         */
        public void testSetConfiguration_NOTCONTRIB() throws Exception
        {
            if (HVideoDevice.NOT_CONTRIBUTING == null) return;

            assertTrue("Should've been able to reserve the device", device.reserveDevice(new TestResourceClient()));
            try
            // reserver/release
            {
                try
                {
                    setConfig(device, HVideoDevice.NOT_CONTRIBUTING);
                    fail("Expected HConfigurationException");
                }
                catch (HConfigurationException e)
                {
                }
            }
            finally
            {
                device.releaseDevice();
            }
        }

        /**
         * Should not be able to get NOT_CONTRIBUTING from
         * getBestConfiguration().
         */
        public void testBestConfiguration_NOTCONTRIB()
        {
            if (HVideoDevice.NOT_CONTRIBUTING == null) return;

            HScreenConfiguration best = getBestConfig(device, HVideoDevice.NOT_CONTRIBUTING.getConfigTemplate());
            assertSame("Expected no-such configuration to be returned", null, best);

            HVideoConfigTemplate t = new HVideoConfigTemplate();
            t.setPreference(HVideoConfigTemplate.PIXEL_RESOLUTION, new java.awt.Dimension(0, 0),
                    HVideoConfigTemplate.REQUIRED);
            best = getBestConfig(device, t);
            assertSame("Expected no configuration to be returned for 0x0", null, best);

        }

        /**
         * Tests for bug 4619.
         */
        public void testBestConfiguration_GRAPHICS_MIXING()
        {
            HGraphicsConfiguration hgc = HScreen.getDefaultHScreen()
                    .getDefaultHGraphicsDevice()
                    .getDefaultConfiguration();
            HVideoConfigTemplate hvct = new HVideoConfigTemplate();
            hvct.setPreference(HVideoConfigTemplate.GRAPHICS_MIXING, hgc, HScreenConfigTemplate.REQUIRED);
            HVideoDevice hvd = (HVideoDevice) device;
            HVideoConfiguration hvc = hvd.getBestConfiguration(hvct);
            // At this point we don't care what it returns... just that it
            // completes successfully!
        }

        /**
         * Tests for bug 4619.
         */
        public void testBestConfigurationArray_GRAPHICS_MIXING()
        {
            HGraphicsConfiguration hgc = HScreen.getDefaultHScreen()
                    .getDefaultHGraphicsDevice()
                    .getDefaultConfiguration();
            HVideoConfigTemplate[] array = { new HVideoConfigTemplate(), new HVideoConfigTemplate(),
                    new HVideoConfigTemplate(), new HVideoConfigTemplate(), };
            array[0].setPreference(HVideoConfigTemplate.GRAPHICS_MIXING, hgc, HScreenConfigTemplate.PREFERRED);
            array[1].setPreference(HVideoConfigTemplate.GRAPHICS_MIXING, hgc, HScreenConfigTemplate.REQUIRED);
            array[2].setPreference(HVideoConfigTemplate.GRAPHICS_MIXING, hgc, HScreenConfigTemplate.REQUIRED_NOT);
            array[3].setPreference(HVideoConfigTemplate.GRAPHICS_MIXING, hgc, HScreenConfigTemplate.PREFERRED_NOT);
            HVideoDevice hvd = (HVideoDevice) device;
            HVideoConfiguration hvc = hvd.getBestConfiguration(array);
            // At this point we don't care what it returns... just that it
            // completes successfully!
        }

        /* ...Boilerplate... */

        public static InterfaceTestSuite isuite() throws Exception
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(VideoInstanceTest.class);
            suite.setName(HVideoDevice.class.getName());
            return suite;
        }

        public VideoInstanceTest(String name, ImplFactory f)
        {
            this(name, TestParam.class, f);
        }

        protected VideoInstanceTest(String name, Class impl, ImplFactory f)
        {
            super(name, impl, f);
        }
    }

    // Overrides HScreenDeviceTest.getScreenDevice
    protected HScreenDevice getScreenDevice()
    {
        return HScreen.getDefaultHScreen().getDefaultHVideoDevice();
    }

    // Overrides HScreenDeviceTest.getConfigurationCount
    protected int getConfigurationCount(HScreenDevice device)
    {
        HVideoDevice dev = (HVideoDevice) device;
        return dev.getConfigurations().length;
    }

    // Overrides HScreenDeviceTest.changeConfiguration
    protected boolean changeConfiguration(HScreenDevice device) throws Exception
    {
        // Let's cycle through the configurations
        HVideoDevice dev = (HVideoDevice) device;
        HVideoConfiguration[] config = dev.getConfigurations();
        HVideoConfiguration curr = dev.getCurrentConfiguration();

        // Find which-ever one we are...
        int i;
        if (HVideoDevice.NOT_CONTRIBUTING != null && curr == HVideoDevice.NOT_CONTRIBUTING)
        {
            // just pick the first one
            i = 0;
        }
        else
        {
            for (i = 0; i < config.length; ++i)
                if (config[i].equals(curr)) break;
            assertFalse("Current configuration not found", i >= config.length);
        }

        // Select the next configuration, wrapping if necessary
        i = (i + 1) % config.length;
        return dev.setVideoConfiguration(config[i]);
    }

    // Boilerplate

    public static Test suite() throws Exception
    {
        return suite(VideoInstanceTest.isuite(), HVideoDeviceTest.class, allPrefs, "video");
    }

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

    public HVideoDeviceTest(String str)
    {
        super(str);
    }
}
