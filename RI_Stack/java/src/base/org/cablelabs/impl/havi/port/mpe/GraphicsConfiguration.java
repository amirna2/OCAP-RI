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
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoDevice;

/**
 * Implementation of {@link HGraphicsConfiguration} for the MPE port intended to
 * run on an OCAP implementation.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 */
public class GraphicsConfiguration extends HGraphicsConfiguration implements ExtendedGraphicsConfiguration
{
    /** The graphics device to which this configuration belongs. */
    private HGraphicsDevice device;

    /** The resolution (width) of this configuration in pixels */
    private int width;

    /** The resolution (height) of this configuration in pixels */
    private int height;

    /** Flicker filter supported if true */
    private boolean flickerFilter;

    /** Interlaced display supported if true */
    private boolean interlacedDisplay;

    /** Pixel aspect ratio */
    private Dimension pixelAspectRatio;

    /** Pixel resolution */
    private Dimension pixelResolution;

    /** Screen location */
    private HScreenRectangle screenLocation;

    /** Image scaling supported if true */
    private boolean imageScalingSupport;

    /** Mattes supported if true */
    private boolean matteSupport;

    /**
     * Construct a configuration for the specified device. This is a package
     * private construction since it should only used by the platform specific
     * code.
     * 
     * @param device
     *            the device to which this configuration belongs
     * @param d
     *            the dimensions of this configuration
     */
    GraphicsConfiguration(HGraphicsDevice device, Dimension d)
    {
        // Save the information passed to this constructor
        this.device = device;
        width = d.width;
        height = d.height;

        // Set the values that define this configuration
        flickerFilter = false;
        interlacedDisplay = false;
        pixelAspectRatio = new Dimension(1, 1);
        pixelResolution = new Dimension(width, height);
        screenLocation = new HScreenRectangle(0.0F, 0.0F, 1.0F, 1.0F);
        imageScalingSupport = false;
        matteSupport = true;
    }

    // Definition copied from superclass
    public boolean getFlickerFilter()
    {
        return flickerFilter;
    }

    // Definition copied from superclass
    public boolean getInterlaced()
    {
        return interlacedDisplay;
    }

    // Definition copied from superclass
    public Dimension getPixelAspectRatio()
    {
        return new Dimension(pixelAspectRatio);
    }

    // Definition copied from superclass
    public Dimension getPixelResolution()
    {
        return new Dimension(pixelResolution);
    }

    // Definition copied from superclass
    public HScreenRectangle getScreenArea()
    {
        return new HScreenRectangle(screenLocation.x, screenLocation.y, screenLocation.width, screenLocation.height);
    }

    // Definition copied from superclass
    public HGraphicsDevice getDevice()
    {
        return device;
    }

    // Definition copied from superclass
    public HGraphicsConfigTemplate getConfigTemplate()
    {
        // Create a template that describes this configuration
        HGraphicsConfigTemplate configTemplate = new HGraphicsConfigTemplate();
        configTemplate.setPreference(HScreenConfigTemplate.FLICKER_FILTERING,
                (flickerFilter == true) ? HScreenConfigTemplate.REQUIRED : HScreenConfigTemplate.REQUIRED_NOT);
        configTemplate.setPreference(HScreenConfigTemplate.INTERLACED_DISPLAY,
                (interlacedDisplay == true) ? HScreenConfigTemplate.REQUIRED : HScreenConfigTemplate.REQUIRED_NOT);
        configTemplate.setPreference(HScreenConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(pixelAspectRatio),
                HScreenConfigTemplate.REQUIRED);
        configTemplate.setPreference(HScreenConfigTemplate.PIXEL_RESOLUTION, new Dimension(pixelResolution),
                HScreenConfigTemplate.REQUIRED);
        configTemplate.setPreference(HScreenConfigTemplate.SCREEN_RECTANGLE, new HScreenRectangle(screenLocation.x,
                screenLocation.y, screenLocation.width, screenLocation.height), HScreenConfigTemplate.REQUIRED);
        configTemplate.setPreference(HGraphicsConfigTemplate.IMAGE_SCALING_SUPPORT,
                (imageScalingSupport == true) ? HScreenConfigTemplate.REQUIRED : HScreenConfigTemplate.REQUIRED_NOT);
        configTemplate.setPreference(HGraphicsConfigTemplate.MATTE_SUPPORT,
                (matteSupport == true) ? HScreenConfigTemplate.REQUIRED : HScreenConfigTemplate.REQUIRED_NOT);

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
}
