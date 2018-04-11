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

import org.havi.ui.HBackgroundConfigTemplate;
import org.havi.ui.HBackgroundConfiguration;
import org.havi.ui.HBackgroundDevice;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HScreenDevice;
import org.havi.ui.HScreenRectangle;

/**
 * Implementation of {@link HBackgroundConfiguration} for the MPE port intended
 * to run on an OCAP implementation.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 */
public class HDBackgroundConfiguration extends HBackgroundConfiguration implements ExtendedBackgroundConfiguration,
        HDScreenConfiguration
{
    /**
     * The background device to which this configuration belongs.
     */
    private HDBackgroundDevice device;

    /**
     * Cached information about this configuration.
     */
    private HDConfigInfo info;

    private UniqueConfigId uniqueId = null;

    /**
     * Returns a new instance of the appropriate background image configuration
     * based on whether the configuration supports a still image or not.
     * 
     * @param device
     *            the HBackgroundDevice for which this is a configuration
     * @param nConfig
     *            the native configuration handle
     */
    static HBackgroundConfiguration newInstance(HBackgroundDevice device, int nConfig)
    {
        HDBackgroundConfiguration bg = new HDBackgroundConfiguration(device, nConfig);

        if (bg.info.stillImage)
            return new HDStillImageBackgroundConfiguration(bg);
        else
            return bg;
    }

    /**
     * Construct a configuration for the specified device.
     * 
     * @param device
     *            the HBackgroundDevice for which this is a configuration
     * @param nConfig
     *            the native configuration handle
     */
    HDBackgroundConfiguration(HBackgroundDevice device, int nConfig)
    {
        // Save the information passed to this constructor
        this.device = (HDBackgroundDevice) device;

        uniqueId = new UniqueConfigId(nConfig);

        info = new HDConfigInfo(nConfig);
    }

    // Definition copied from NativeHandle
    public int getHandle()
    {
        return uniqueId.nativeHandle;
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
    public HBackgroundDevice getDevice()
    {
        return device;
    }

    // Definition copied from HDScreenConfiguration
    public HScreenDevice getScreenDevice()
    {
        return getDevice();
    }

    // Definition copied from superclass
    public HBackgroundConfigTemplate getConfigTemplate()
    {
        return (HBackgroundConfigTemplate) getConfigTemplate(HBackgroundConfigTemplate.REQUIRED,
                HBackgroundConfigTemplate.REQUIRED_NOT);
    }

    // Description inherited from HDScreenConfiguration
    public HScreenConfigTemplate getSoftConfigTemplate()
    {
        return getConfigTemplate(HBackgroundConfigTemplate.PREFERRED, HBackgroundConfigTemplate.PREFERRED_NOT);
    }

    // Description inherited from HDScreenConfiguration
    public HScreenConfigTemplate getHardConfigTemplate()
    {
        return getConfigTemplate();
    }

    /**
     * Returns a <code>HBackgroundConfigTemplate</code> using the given values
     * for <i>required</i> and <i>required_not</i> entries.
     * 
     * @param required
     *            either <code>REQUIRED</code> or <code>PREFERRED</code>
     * @param required_not
     *            either <code>REQUIRED_NOT</code> or <code>PREFERRED_NOT</code>
     * @return a <code>HBackgroundConfigTemplate</code> using the given values
     *         for <i>required</i> and <i>required_not</i> entries.
     */
    private HScreenConfigTemplate getConfigTemplate(int required, int required_not)
    {
        // Create a template that describes this configuration
        HBackgroundConfigTemplate configTemplate = new HBackgroundConfigTemplate();
        configTemplate.setPreference(HBackgroundConfigTemplate.FLICKER_FILTERING,
                (info.flickerFilter == true) ? required : required_not);
        configTemplate.setPreference(HBackgroundConfigTemplate.INTERLACED_DISPLAY,
                (info.interlacedDisplay == true) ? required : required_not);
        configTemplate.setPreference(HBackgroundConfigTemplate.PIXEL_ASPECT_RATIO,
                new Dimension(info.pixelAspectRatio), required);
        configTemplate.setPreference(HBackgroundConfigTemplate.PIXEL_RESOLUTION, new Dimension(info.pixelResolution),
                required);
        configTemplate.setPreference(HBackgroundConfigTemplate.SCREEN_RECTANGLE, new HScreenRectangle(
                info.screenArea.x, info.screenArea.y, info.screenArea.width, info.screenArea.height), required);
        configTemplate.setPreference(HBackgroundConfigTemplate.CHANGEABLE_SINGLE_COLOR,
                (info.changeableSingleColor == true) ? required : required_not);
        configTemplate.setPreference(HBackgroundConfigTemplate.STILL_IMAGE, (info.stillImage == true) ? required
                : required_not);

        return configTemplate;
    }

    // Definition copied from superclass
    public Color getColor()
    {
        return device.getColor();
    }

    // Definition copied from superclass
    public void setColor(Color color) throws HPermissionDeniedException, HConfigurationException
    {
        setColor(this, color);
    }

    /**
     * Used to implement {@link #setColor(java.awt.Color)}. Exists so that
     * {@link HDStillImageBackgroundConfiguration#setColor} can be implemented
     * the same.
     * 
     * @param config
     * @param color
     * @throws HPermissionDeniedException
     * @throws HConfigurationException
     */
    void setColor(HBackgroundConfiguration config, Color color) throws HPermissionDeniedException,
            HConfigurationException
    {
        device.setColor(config, color);
    }

    // Definition copied from superclass
    public boolean getChangeableSingleColor()
    {
        return info.changeableSingleColor;
    }

    // Definition copied from superclass
    public boolean getStillImage()
    {
        return info.stillImage;
    }

    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }
        if (obj instanceof HDBackgroundConfiguration)
        {
            HDBackgroundConfiguration hdbc = (HDBackgroundConfiguration)obj;
            return getUniqueId().equals(hdbc.getUniqueId());
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
