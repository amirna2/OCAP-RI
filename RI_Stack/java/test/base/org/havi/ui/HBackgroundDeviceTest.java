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

//import org.havi.ui.*;
import junit.framework.*;
import org.cablelabs.test.*;
import org.havi.ui.event.*;
import org.w3c.dom.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;
import java.util.Enumeration;
import java.util.Vector;
import java.awt.Dimension;
import org.havi.ui.HScreenRectangle;

/**
 * Tests {@link #HBackgroundDevice}.
 * 
 * @author Tom Henriksen
 * @author Aaron Kamienski
 * @version $Revision: 1.7 $, $Date: 2002/06/03 21:32:11 $
 */
public class HBackgroundDeviceTest extends HScreenDeviceTest
{
    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HScreenDevice
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HBackgroundDevice.class, HScreenDevice.class);
    }

    /**
     * Test the constructor of HBackgroundDevice.
     * <ul>
     * <li>HBackgroundDevice()
     * </ul>
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HBackgroundDevice.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HBackgroundDevice.class);
    }

    // Preferences
    private static final String allFields[] = { "ZERO_BACKGROUND_IMPACT", "ZERO_GRAPHICS_IMPACT", "ZERO_VIDEO_IMPACT",
            "INTERLACED_DISPLAY", "FLICKER_FILTERING", "VIDEO_GRAPHICS_PIXEL_ALIGNED", "PIXEL_ASPECT_RATIO",
            "PIXEL_RESOLUTION", "SCREEN_RECTANGLE", "CHANGEABLE_SINGLE_COLOR", "STILL_IMAGE" };

    // Preferences
    private static final int allPrefs[] = { HBackgroundConfigTemplate.ZERO_BACKGROUND_IMPACT,
            HBackgroundConfigTemplate.ZERO_GRAPHICS_IMPACT, HBackgroundConfigTemplate.ZERO_VIDEO_IMPACT,
            HBackgroundConfigTemplate.INTERLACED_DISPLAY, HBackgroundConfigTemplate.FLICKER_FILTERING,
            HBackgroundConfigTemplate.VIDEO_GRAPHICS_PIXEL_ALIGNED, HBackgroundConfigTemplate.PIXEL_ASPECT_RATIO,
            HBackgroundConfigTemplate.PIXEL_RESOLUTION, HBackgroundConfigTemplate.SCREEN_RECTANGLE,
            HBackgroundConfigTemplate.CHANGEABLE_SINGLE_COLOR, HBackgroundConfigTemplate.STILL_IMAGE };

    // Object identifiers
    final static boolean[] isObjPref = { false, false, false, false, false, true, true, true, true, false, false };

    public static class BackgroundInstanceTest extends InstanceTest
    {
        /**
         * Tests getBestConfiguration(). Verify that setting the
         * ZERO_VIDEO_IMPACT to PREFERRED gets expected results.
         * <p>
         * Supports the following from HAVi spec:
         * <p>
         * <blockquote> The ZERO_VIDEO_IMPACT property may be used in instances
         * of this class to discover whether displaying background stills will
         * have any impact on already running video. Implementations supporting
         * the STILL_IMAGE preference shall return an
         * HStillImageBackgroundConfiguration when requested except as described
         * below.
         * <ul>
         * <li>If displaying an STILL_IMAGE interrupts video transiently while
         * the image is decoded then a configuration shall not be returned if
         * the ZERO_VIDEO_IMPACT property is present with the priority REQUIRED.
         * <li>If displaying an STILL_IMAGE interrupts video while the image is
         * decoded and for the entire period while the image is displayed then a
         * configuration shall not be returned if the ZERO_VIDEO_IMPACT property
         * is present with either the priorities REQUIRED or PREFERRED.
         * </ul>
         * </blockquote>
         * 
         * <p>
         * The behavior is keyed off of whether HVideoDevice.NOT_CONTRIBUTING is
         * set or not. If it is set, then it is assumed that STILL_IMAGE support
         * completely interrupts video display.
         */
        public void testBestConfiguration_zeroImpactPref() throws Exception
        {
            HScreenConfiguration[] configs = getConfigs(device);

            // Foreach configuration
            // - get set of compat/incompat configs
            // - determine if only compat video configs are non-visible
            // - Foreach incompat video configuration
            // - set it
            // - see what we get for ZERO_VIDEO_IMPACT:PREFERRED
            for (int i = 0; i < configs.length; ++i)
            {
                if (!(configs[i] instanceof HStillImageBackgroundConfiguration)) continue;

                HScreenConfiguration config = configs[i];
                HScreenConfigTemplate t = getTemplate(config);

                // Should be able to get it if asked for explicitly
                HScreenConfiguration best;
                best = getBestConfig(device, t);
                assertNotNull("Should be able to get if asked for explicitly", best);
                assertSame("Unexpected configuration returned", config, best);

                // Find compat/incompat configs
                Vector compat = new Vector();
                Vector incompat = new Vector();
                findCompat(config, compat, incompat);

                // Try incompat VIDEO configs
                for (Enumeration e = incompat.elements(); e.hasMoreElements();)
                {
                    HScreenConfiguration other = (HScreenConfiguration) e.nextElement();
                    if (!(other instanceof HVideoConfiguration)) continue;

                    // Select other as current configuration
                    HScreenDevice otherDevice = getDevice(other);
                    assertFalse("Internal error? should not be same device!", device == otherDevice);
                    otherDevice.reserveDevice(new TestResourceClient());
                    try
                    // reserver/release
                    {
                        setConfig(otherDevice, other);

                        // Try PREFERRED
                        t.setPreference(HScreenConfigTemplate.ZERO_VIDEO_IMPACT, HScreenConfigTemplate.PREFERRED);
                        best = getBestConfig(device, t);

                        // what should be expected?
                        // if HVideoDevice.NOT_CONTRIBUTING!=null, then null
                        // otherwise expect what we started with
                        if (HVideoDevice.NOT_CONTRIBUTING != null)
                        {
                            assertSame("Expected null returned for ZERO_VIDEO_IMPACT:PREFERRED", null, best);
                        }
                        else
                        {
                            assertSame("Expected original configuration", config, best);
                        }
                    }
                    finally
                    {
                        otherDevice.releaseDevice();
                    }
                }
            }
        }

        /* ...Boilerplate... */

        public static InterfaceTestSuite isuite() throws Exception
        {
            InterfaceTestSuite suite = new InterfaceTestSuite(BackgroundInstanceTest.class);
            suite.setName(HBackgroundDevice.class.getName());
            return suite;
        }

        public BackgroundInstanceTest(String name, ImplFactory f)
        {
            this(name, TestParam.class, f);
        }

        protected BackgroundInstanceTest(String name, Class impl, ImplFactory f)
        {
            super(name, impl, f);
        }
    }

    // Overrides HScreenDeviceTest.getScreenDevice
    protected HScreenDevice getScreenDevice()
    {
        return HScreen.getDefaultHScreen().getDefaultHBackgroundDevice();
    }

    // Overrides HScreenDeviceTest.getConfigurationCount
    protected int getConfigurationCount(HScreenDevice device)
    {
        HBackgroundDevice dev = (HBackgroundDevice) device;
        return dev.getConfigurations().length;
    }

    // Overrides HScreenDeviceTest.changeConfiguration
    protected boolean changeConfiguration(HScreenDevice device) throws Exception
    {
        // Let's cycle through the configurations
        HBackgroundDevice dev = (HBackgroundDevice) device;
        HBackgroundConfiguration[] config = dev.getConfigurations();
        HBackgroundConfiguration curr = dev.getCurrentConfiguration();

        // Find which-ever one we are...
        int i;
        for (i = 0; i < config.length; ++i)
            if (config[i].equals(curr)) break;
        assertFalse("Current configuration not found", i >= config.length);

        // Select the next configuration, wrapping if necessary
        i = (i + 1) % config.length;
        return dev.setBackgroundConfiguration(config[i]);
    }

    // Boilerplate

    public static Test suite() throws Exception
    {
        return suite(BackgroundInstanceTest.isuite(), HBackgroundDeviceTest.class, allPrefs, "background");
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

    public HBackgroundDeviceTest(String str)
    {
        super(str);
    }
}
