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

import junit.framework.*;
import org.cablelabs.test.*;
import org.havi.ui.event.*;
import org.w3c.dom.*;
import net.sourceforge.groboutils.junit.v1.iftc.*;
import java.util.Vector;
import java.util.Enumeration;

/**
 * Tests {@link #HGraphicsDevice}.
 * 
 * @author Jay Tracy
 * @author Tom Henriksen
 * @author Todd Earles
 * @author Aaron Kamienski
 * @version $Revision: 1.12 $, $Date: 2002/06/03 21:32:14 $
 */
public class HGraphicsDeviceTest extends HScreenDeviceTest
{
    /**
     * Tests for correct ancestry.
     * <ul>
     * <li>extends HScreenDevice
     * </ul>
     */
    public void testAncestry()
    {
        TestUtils.testExtends(HGraphicsDevice.class, HScreenDevice.class);
    }

    /**
     * Test the constructor of HGraphicsDevice.
     * <ul>
     * <li>HGraphicsDevice()
     * </ul>
     */
    public void testConstructors()
    {
        TestUtils.testNoPublicConstructors(HGraphicsDevice.class);
    }

    /**
     * Tests for any exposed non-final fields.
     */
    public void testNoPublicFields()
    {
        TestUtils.testNoPublicFields(HGraphicsDevice.class);
    }

    // Preferences
    private static final int allPrefs[] = { HGraphicsConfigTemplate.ZERO_BACKGROUND_IMPACT,
            HGraphicsConfigTemplate.ZERO_GRAPHICS_IMPACT, HGraphicsConfigTemplate.ZERO_VIDEO_IMPACT,
            HGraphicsConfigTemplate.INTERLACED_DISPLAY, HGraphicsConfigTemplate.FLICKER_FILTERING,
            HGraphicsConfigTemplate.VIDEO_GRAPHICS_PIXEL_ALIGNED, HGraphicsConfigTemplate.PIXEL_ASPECT_RATIO,
            HGraphicsConfigTemplate.PIXEL_RESOLUTION, HGraphicsConfigTemplate.SCREEN_RECTANGLE,
            HGraphicsConfigTemplate.VIDEO_MIXING, HGraphicsConfigTemplate.MATTE_SUPPORT,
            HGraphicsConfigTemplate.IMAGE_SCALING_SUPPORT, };

    // Overrides HScreenDeviceTest.getScreenDevice
    protected HScreenDevice getScreenDevice()
    {
        return HScreen.getDefaultHScreen().getDefaultHGraphicsDevice();
    }

    // Overrides HScreenDeviceTest.getConfigurationCount
    protected int getConfigurationCount(HScreenDevice device)
    {
        HGraphicsDevice dev = (HGraphicsDevice) device;
        return dev.getConfigurations().length;
    }

    // Overrides HScreenDeviceTest.changeConfiguration
    protected boolean changeConfiguration(HScreenDevice device) throws Exception
    {
        // Let's cycle through the configurations
        HGraphicsDevice dev = (HGraphicsDevice) device;
        HGraphicsConfiguration[] config = dev.getConfigurations();
        HGraphicsConfiguration curr = dev.getCurrentConfiguration();

        // Find which-ever one we are...
        int i;
        for (i = 0; i < config.length; ++i)
            if (config[i].equals(curr)) break;
        assertFalse("Current configuration not found", i >= config.length);

        // Select the next configuration, wrapping if necessary
        i = (i + 1) % config.length;
        return dev.setGraphicsConfiguration(config[i]);
    }

    // Boilerplate

    public static Test suite() throws Exception
    {
        return suite(HGraphicsDeviceTest.class, allPrefs, "graphics");
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

    public HGraphicsDeviceTest(String str)
    {
        super(str);
    }
}
