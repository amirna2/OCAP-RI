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

package org.cablelabs.impl.havi.port.mpe;

import org.cablelabs.impl.havi.ExtendedGraphicsConfiguration;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;

import org.havi.ui.HGraphicsConfigTemplate;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HImageHints;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HScreenDevice;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoDevice;

/**
 * Implementation of {@link HGraphicsConfiguration} for the MPE port intended to
 * run on an OCAP implementation.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 */
public class HDGraphicsConfiguration extends HGraphicsConfiguration implements ExtendedGraphicsConfiguration,
        HDScreenConfiguration
{
    /**
     * The graphics device to which this configuration belongs.
     */
    private HGraphicsDevice device;

    /**
     * Cached information about this configuration.
     */
    private HDConfigInfo info;

    private UniqueConfigId uniqueId = null;

    /**
     * Currently not enabled.
     */
    private static final boolean matteSupport = false;

    /**
     * No known hardware image scaling support is provided at this time.
     */
    private static final boolean imageScalingSupport = false;

    /**
     * Construct a configuration for the specified device.
     * 
     * @param device
     *            the HGraphicsDevice for which this is a configuration
     * @param nConfig
     *            the native configuration handle
     */
    HDGraphicsConfiguration(HGraphicsDevice device, int nConfig)
    {
        // Save the information passed to this constructor
        this.device = device;

        uniqueId = new UniqueConfigId(nConfig);

        info = new HDConfigInfo(nConfig);
    }

    // Definition copied from NativeHandle
    public int getHandle()
    {
        return getUniqueId().nativeHandle;
    }

    // Definition copied from superclass
    public boolean getFlickerFilter()
    {
        return info.flickerFilter;
    }

    // Definition copied from superclass
    public boolean getInterlaced()
    {
        return info.interlacedDisplay;
    }

    // Definition copied from superclass
    public Dimension getPixelAspectRatio()
    {
        return new Dimension(info.pixelAspectRatio);
    }

    // Definition copied from superclass
    public Dimension getPixelResolution()
    {
        return new Dimension(info.pixelResolution);
    }

    // Definition copied from superclass
    public HScreenRectangle getScreenArea()
    {
        return new HScreenRectangle(info.screenArea.x, info.screenArea.y, info.screenArea.width, info.screenArea.height);
    }

    // Definition copied from superclass
    public HGraphicsDevice getDevice()
    {
        return device;
    }

    // Definition copied from HDScreenConfiguration
    public HScreenDevice getScreenDevice()
    {
        return getDevice();
    }

    // Definition copied from superclass
    public HGraphicsConfigTemplate getConfigTemplate()
    {
        return getConfigTemplate(HScreenConfigTemplate.REQUIRED, HScreenConfigTemplate.REQUIRED_NOT);
    }

    // Description inherited from HDScreenConfiguration
    public HScreenConfigTemplate getSoftConfigTemplate()
    {
        return getConfigTemplate(HScreenConfigTemplate.PREFERRED, HScreenConfigTemplate.PREFERRED_NOT);
    }

    // Description inherited from HDScreenConfiguration
    public HScreenConfigTemplate getHardConfigTemplate()
    {
        return getConfigTemplate();
    }

    /**
     * Returns a <code>HGraphicsConfigTemplate</code> using the given values for
     * <i>required</i> and <i>required_not</i> entries.
     * 
     * @param required
     *            either <code>REQUIRED</code> or <code>PREFERRED</code>
     * @param required_not
     *            either <code>REQUIRED_NOT</code> or <code>PREFERRED_NOT</code>
     * @return a <code>HGraphicsConfigTemplate</code> using the given values for
     *         <i>required</i> and <i>required_not</i> entries.
     */
    private HGraphicsConfigTemplate getConfigTemplate(int required, int required_not)
    {
        // Create a template that describes this configuration
        HGraphicsConfigTemplate configTemplate = new HGraphicsConfigTemplate();
        configTemplate.setPreference(HScreenConfigTemplate.FLICKER_FILTERING, (info.flickerFilter == true) ? required
                : required_not);
        configTemplate.setPreference(HScreenConfigTemplate.INTERLACED_DISPLAY,
                (info.interlacedDisplay == true) ? required : required_not);
        configTemplate.setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(info.pixelAspectRatio),
                required);
        configTemplate.setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(info.pixelResolution),
                required);
        configTemplate.setPreference(HScreenConfigTemplate.SCREEN_RECTANGLE, new HScreenRectangle(info.screenArea.x,
                info.screenArea.y, info.screenArea.width, info.screenArea.height), required);
        configTemplate.setPreference(HGraphicsConfigTemplate.IMAGE_SCALING_SUPPORT,
                (imageScalingSupport == true) ? required : required_not);
        configTemplate.setPreference(HGraphicsConfigTemplate.MATTE_SUPPORT, (matteSupport == true) ? required
                : required_not);

        return configTemplate;
    }

    // Definition copied from superclass
    public Image getCompatibleImage(Image input, HImageHints ih)
    {
        // Pretend like image is already optimized for this configuration
        return input;
    }

    // Definition copied from superclass
    public Font[] getAllFonts()
    {
        String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        Font[] fonts = new Font[names.length];

        for (int i = 0; i < names.length; ++i)
            fonts[i] = new Font(names[i], 0, 1);
        return fonts;
    }

    // Definition copied from superclass
    public Color getPunchThroughToBackgroundColor(int percentage)
    {
        // This platform does not support a punch-through color
        return null;
    }

    // Definition copied from superclass
    public Color getPunchThroughToBackgroundColor(int percentage, HVideoDevice hvd)
    {
        // This platform does not support a punch-through color
        return null;
    }

    // Definition copied from superclass
    public Color getPunchThroughToBackgroundColor(Color color, int percentage)
    {
        // This platform does not support a punch-through color
        return null;
    }

    // Definition copied from superclass
    public Color getPunchThroughToBackgroundColor(Color color, int percentage, HVideoDevice v)
    {
        // This platform does not support a punch-through color
        return null;
    }

    // Definition copied from superclass
    public void dispose(Color c)
    {
        // Nothing to do here since this platform does not support a punch-
        // through color.
    }

    // Definition copied from superclass
    public boolean getMatteSupport()
    {
        return matteSupport;
    }

    // Definition copied from superclass
    public boolean getImageScalingSupport()
    {
        return imageScalingSupport;
    }

    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }
        if (obj instanceof HDGraphicsConfiguration)
        {
            HDGraphicsConfiguration hdgc = (HDGraphicsConfiguration)obj;
            return getUniqueId().equals(hdgc.getUniqueId());
        }
        return false;
    }
    
    public int hashCode()
    {
        return this.getUniqueId().hashCode();
    }

    public UniqueConfigId getUniqueId()
    {
        return uniqueId;
    }

    public boolean equals(int nHandle, int dfc)
    {
        return getUniqueId().equals(nHandle, dfc);
    }

    public String toString()
    {
        return getUniqueId().toString();
    }
}
