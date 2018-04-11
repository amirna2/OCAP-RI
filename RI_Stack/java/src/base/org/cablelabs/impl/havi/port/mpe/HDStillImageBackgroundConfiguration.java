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

import org.cablelabs.impl.havi.ExtendedStillImageBackgroundConfiguration;
import org.cablelabs.impl.havi.MpegBackgroundImage;

import java.awt.Color;
import java.awt.Dimension;

import java.lang.IllegalStateException;

import org.havi.ui.HBackgroundConfigTemplate;
import org.havi.ui.HBackgroundDevice;
import org.havi.ui.HBackgroundImage;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HScreenDevice;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HStillImageBackgroundConfiguration;
import org.havi.ui.event.HBackgroundImageEvent;

/**
 * Implementation of {@link HStillImageBackgroundConfiguration} for the MPE port
 * intended to run on an OCAP implementation.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 */
public class HDStillImageBackgroundConfiguration extends HStillImageBackgroundConfiguration implements
        ExtendedStillImageBackgroundConfiguration, HDScreenConfiguration
{
    /**
     * The base configuration for this background.
     */
    private HDBackgroundConfiguration baseConfiguration = null;

    private UniqueConfigId uniqueId = null;

    protected HDStillImageBackgroundConfiguration()
    {

    }

    /**
     * Construct a still image configuration based upon the given background
     * configuration. The common implementation is provided by the base
     * background configuration.
     * 
     * @param base
     *            the base configuration
     */
    HDStillImageBackgroundConfiguration(HDBackgroundConfiguration base)
    {
        // Save the information passed to this constructor
        this.baseConfiguration = base;
        uniqueId = new UniqueConfigId(base.getHandle());

    }

    // Definition copied from NativeHandle
    public int getHandle()
    {
        return uniqueId.nativeHandle;
    }

    // Definition copied from superclass
    public boolean getFlickerFilter()
    {
        return baseConfiguration.getFlickerFilter();
    }

    // Definition copied from superclass
    public boolean getInterlaced()
    {
        return baseConfiguration.getInterlaced();
    }

    // Definition copied from superclass
    public Dimension getPixelAspectRatio()
    {
        return baseConfiguration.getPixelAspectRatio();
    }

    // Definition copied from superclass
    public Dimension getPixelResolution()
    {
        return baseConfiguration.getPixelResolution();
    }

    // Definition copied from superclass
    public HScreenRectangle getScreenArea()
    {
        return baseConfiguration.getScreenArea();
    }

    // Definition copied from superclass
    public HBackgroundDevice getDevice()
    {
        return baseConfiguration.getDevice();
    }

    // Definition copied from HDScreenConfiguration
    public HScreenDevice getScreenDevice()
    {
        return getDevice();
    }

    // Definition copied from superclass
    public HBackgroundConfigTemplate getConfigTemplate()
    {
        return baseConfiguration.getConfigTemplate();
    }

    // Definition copied from HDScreenConfiguration
    public HScreenConfigTemplate getSoftConfigTemplate()
    {
        return baseConfiguration.getSoftConfigTemplate();
    }

    // Definition copied from HDScreenConfiguration
    public HScreenConfigTemplate getHardConfigTemplate()
    {
        return baseConfiguration.getHardConfigTemplate();
    }

    // Definition copied from superclass
    public Color getColor()
    {
        return baseConfiguration.getColor();
    }

    // Definition copied from superclass
    public void setColor(Color color) throws HPermissionDeniedException, HConfigurationException
    {
        baseConfiguration.setColor(this, color);
    }

    // Definition copied from superclass
    public boolean getChangeableSingleColor()
    {
        return baseConfiguration.getChangeableSingleColor();
    }

    // Definition copied from superclass
    public boolean getStillImage()
    {
        return baseConfiguration.getStillImage();
    }

    // Definition copied from superclass
    public void displayImageImpl(HBackgroundImage image, HScreenRectangle r) throws java.io.IOException,
            HPermissionDeniedException, HConfigurationException
    {
        if (image == null || !(image instanceof MpegBackgroundImage))
            throw new IllegalArgumentException("Unknown BackgroundImage impl");

        // Load the image if not already loaded
        MpegBackgroundImage mpeg = (MpegBackgroundImage) image;
        switch (mpeg.loadNow())
        {
            case HBackgroundImageEvent.BACKGROUNDIMAGE_LOADED:
                /* Successful load, move along. */
                break;
            case HBackgroundImageEvent.BACKGROUNDIMAGE_INVALID:
                throw new IllegalArgumentException();
            case HBackgroundImageEvent.BACKGROUNDIMAGE_FILE_NOT_FOUND:
                throw new java.io.FileNotFoundException();
            case HBackgroundImageEvent.BACKGROUNDIMAGE_IOERROR:
                throw new java.io.IOException();
            default:
                throw new RuntimeException("Internal error - unexpected value");
        }

        // Finally display the image
        // Will verify reservation and configuration
        ((HDBackgroundDevice) getDevice()).displayImage(this, mpeg, r);
    }

    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }
        
        if (obj instanceof HDStillImageBackgroundConfiguration)
        {
            HDStillImageBackgroundConfiguration hdsibc = 
                (HDStillImageBackgroundConfiguration)obj;
            return getUniqueId().equals(hdsibc.getUniqueId());
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
