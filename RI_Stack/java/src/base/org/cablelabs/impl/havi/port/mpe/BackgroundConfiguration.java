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

import org.cablelabs.impl.havi.ExtendedBackgroundConfiguration;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import org.havi.ui.HBackgroundConfigTemplate;
import org.havi.ui.HBackgroundConfiguration;
import org.havi.ui.HBackgroundDevice;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreenRectangle;

/**
 * Implementation of {@link HBackgroundConfiguration} for the MPE port intended
 * to run on an OCAP implementation.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 * @version $Id: BackgroundConfiguration.java,v 1.16 2002/06/03 21:31:03 aaronk
 *          Exp $
 */
public class BackgroundConfiguration extends HBackgroundConfiguration implements ExtendedBackgroundConfiguration
{
    /** The background device to which this configuration belongs. */
    private HBackgroundDevice device;

    /** The resolution (width) of this configuration in pixels */
    private int width;

    /** The resolution (height) of this configuration in pixels */
    private int height;

    /** The current background color */
    private Color currentColor;

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

    /** Changeable single color supported if true */
    protected boolean changeableSingleColor;

    /** Still images supporte if true */
    protected boolean stillImage;

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
    BackgroundConfiguration(HBackgroundDevice device, Dimension d)
    {
        // Save the information passed to this constructor
        this.device = device;
        width = d.width;
        height = d.height;

        // Set the color
        currentColor = Toolkit.getColor(Property.BG_COLOR);
        if (currentColor == null) currentColor = Color.black;

        // Set the values that define this configuration
        flickerFilter = false;
        interlacedDisplay = false;
        pixelAspectRatio = new Dimension(1, 1);
        pixelResolution = new Dimension(width, height);
        screenLocation = new HScreenRectangle(0.0F, 0.0F, 1.0F, 1.0F);
        changeableSingleColor = true;
        stillImage = false;
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
    public HBackgroundDevice getDevice()
    {
        return device;
    }

    // Definition copied from superclass
    public HBackgroundConfigTemplate getConfigTemplate()
    {
        // Create a template that describes this configuration
        HBackgroundConfigTemplate configTemplate = new HBackgroundConfigTemplate();
        configTemplate.setPreference(HBackgroundConfigTemplate.FLICKER_FILTERING,
                (flickerFilter == true) ? HBackgroundConfigTemplate.REQUIRED : HBackgroundConfigTemplate.REQUIRED_NOT);
        configTemplate.setPreference(HBackgroundConfigTemplate.INTERLACED_DISPLAY,
                (interlacedDisplay == true) ? HBackgroundConfigTemplate.REQUIRED
                        : HBackgroundConfigTemplate.REQUIRED_NOT);
        configTemplate.setPreference(HBackgroundConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(pixelAspectRatio),
                HBackgroundConfigTemplate.REQUIRED);
        configTemplate.setPreference(HBackgroundConfigTemplate.PIXEL_RESOLUTION, new Dimension(pixelResolution),
                HBackgroundConfigTemplate.REQUIRED);
        configTemplate.setPreference(HBackgroundConfigTemplate.SCREEN_RECTANGLE, new HScreenRectangle(screenLocation.x,
                screenLocation.y, screenLocation.width, screenLocation.height), HBackgroundConfigTemplate.REQUIRED);
        configTemplate.setPreference(HBackgroundConfigTemplate.CHANGEABLE_SINGLE_COLOR,
                (changeableSingleColor == true) ? HBackgroundConfigTemplate.REQUIRED
                        : HBackgroundConfigTemplate.REQUIRED_NOT);
        configTemplate.setPreference(HBackgroundConfigTemplate.STILL_IMAGE,
                (stillImage == true) ? HBackgroundConfigTemplate.REQUIRED : HBackgroundConfigTemplate.REQUIRED_NOT);

        return configTemplate;
    }

    // Definition copied from superclass
    public Color getColor()
    {
        return currentColor;
    }

    // Definition copied from superclass
    public void setColor(Color color) throws HPermissionDeniedException, HConfigurationException
    {
        currentColor = color;
        ((BackgroundDevice) device).refresh(this);
    }

    // Definition copied from superclass
    public boolean getChangeableSingleColor()
    {
        return changeableSingleColor;
    }

    // Definition copied from superclass
    public boolean getStillImage()
    {
        return stillImage;
    }

    /**
     * Refresh (paint) the background component. This method is called whenever
     * the component that represents the device needs to be repainted.
     * 
     * @param g
     *            the graphics to use for drawing
     */
    void refresh(Graphics g)
    {
        /*
         * ClearRect is implicitly performed using SRC mode. We don't want any
         * blending to occur as we want to replace whatever might be there.
         */
        g.setColor(currentColor);
        g.clearRect(0, 0, width, height);
    }
}
