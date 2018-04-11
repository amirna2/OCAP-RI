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

import org.apache.log4j.Logger;
import org.dvb.media.VideoFormatControl;
import org.havi.ui.*;

import java.awt.Dimension;

import org.cablelabs.impl.havi.ExtendedVideoConfiguration;
import org.cablelabs.impl.media.player.Util;
import org.cablelabs.impl.util.MPEEnv;

/**
 * Implementation of {@link HVideoConfiguration} for the MPE port intended to
 * run on an OCAP implementation.
 * 
 * @author Todd Earles
 * @author Aaron Kamienski (mpe mods from generic)
 * @author Alan Cossitt (DSExt changes)
 * 
 */
public class HDVideoConfiguration extends HVideoConfiguration implements ExtendedVideoConfiguration,
        HDScreenConfiguration
{
    private static final Logger log = Logger.getLogger(HDVideoConfiguration.class.getName());

    /**
     * The video device to which this configuration belongs.
     */
    private HVideoDevice device;

    /**
     * Cached MPEOS information about this configuration.
     */
    private HDConfigInfo info;

    private UniqueConfigId uniqueId = null;

    HDVideoConfiguration(HVideoDevice device, int nConfig, int dfc)
    {
        // Save the information passed to this constructor

        uniqueId = new UniqueConfigId(nConfig, dfc);

        this.device = device;

        info = new HDConfigInfo(nConfig);
    }

    /**
     * Construct a configuration for the specified device.
     * 
     * @param device
     *            the HVideoDevice for which this is a configuration
     * @param nConfig
     *            the native configuration handle
     */
    HDVideoConfiguration(HVideoDevice device, int nConfig)
    {
        this(device, nConfig, VideoFormatControl.DFC_PROCESSING_UNKNOWN);
    }

    public int getPlatformDfc()
    {
        return uniqueId.platformDfc;
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
    public HVideoDevice getDevice()
    {
        return device;
    }

    // Definition copied from HDScreenConfiguration
    public HScreenDevice getScreenDevice()
    {
        return getDevice();
    }

    /**
     * 
     * @return {@link VideoFormatControl.ASPECT_RATIO_4_3} or
     *         {@link VideoFormatControl.ASPECT_RATIO_16_9} or
     *         {@link VideoFormatControl.ASPECT_RATIO_2_21_1} or
     *         {@link VideoFormatControl.ASPECT_RATIO_UNKNOWN}
     */
    public int getAspectRatio()
    {
        return Util.getAspectRatio(info.screenAspectRatio);
    }

    // Definition copied from superclass
    public HVideoConfigTemplate getConfigTemplate()
    {
        return getConfigTemplate(HVideoConfigTemplate.REQUIRED, HVideoConfigTemplate.REQUIRED_NOT);
    }

    // Description inherited from HDScreenConfiguration
    public HScreenConfigTemplate getSoftConfigTemplate()
    {
        return getConfigTemplate(HVideoConfigTemplate.PREFERRED, HVideoConfigTemplate.PREFERRED_NOT);
    }

    // Description inherited from HDScreenConfiguration
    public HScreenConfigTemplate getHardConfigTemplate()
    {
        return getConfigTemplate();
    }

    /**
     * Returns a <code>HVideoConfigTemplate</code> using the given values for
     * <i>required</i> and <i>required_not</i> entries.
     * 
     * @param required
     *            either <code>REQUIRED</code> or <code>PREFERRED</code>
     * @param required_not
     *            either <code>REQUIRED_NOT</code> or <code>PREFERRED_NOT</code>
     * @return a <code>HVideoConfigTemplate</code> using the given values for
     *         <i>required</i> and <i>required_not</i> entries.
     */
    private HVideoConfigTemplate getConfigTemplate(int required, int required_not)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getConfigTemplate - required: " + required + ", required_not: " + required_not);
        }
        // Create a template that describes this configuration
        HVideoConfigTemplate configTemplate = new HVideoConfigTemplate();
        configTemplate.setPreference(HVideoConfigTemplate.FLICKER_FILTERING, (info.flickerFilter == true) ? required
                : required_not);
        configTemplate.setPreference(HVideoConfigTemplate.INTERLACED_DISPLAY,
                (info.interlacedDisplay == true) ? required : required_not);
        configTemplate.setPreference(HVideoConfigTemplate.PIXEL_ASPECT_RATIO, new Dimension(info.pixelAspectRatio),
                required);
        configTemplate.setPreference(HVideoConfigTemplate.PIXEL_RESOLUTION, new Dimension(info.pixelResolution),
                required);
        configTemplate.setPreference(HVideoConfigTemplate.SCREEN_RECTANGLE, new HScreenRectangle(info.screenArea.x,
                info.screenArea.y, info.screenArea.width, info.screenArea.height), required);

        boolean dsExtUsed = (MPEEnv.getEnv("ocap.api.option.ds") != null);

        if (dsExtUsed) // if device extension is in place
        {
            HDVideoDevice device = (HDVideoDevice) getDevice();
            if (device.isBackgroundVideo())
            {
                // ZOOM_MODE only has meaning for TV (non-pip) video devices.

                /*
                 * From VideoZoomPreference javadoc:
                 * 
                 * Instances of {@link HVideoConfigTemplate} generated by the
                 * platform and returned to applications (e.g., from {@link
                 * HVideoConfiguration#getConfigTemplate()} SHALL have this
                 * preference set to a valid platform-supported DFC constant (as
                 * an instance of <code>Integer</code>) with {@link
                 * HScreenConfigTemplate#REQUIRED REQUIRED} priority.
                 */
                if (log.isDebugEnabled())
                {
                    log.debug("dsExt available and device is background video device - setting zoom preference object: "
                            + new Integer(getPlatformDfc()) + ", required: " + required);
                }
                configTemplate.setPreference(ZOOM_PREFERENCE, new Integer(getPlatformDfc()),
                        HScreenConfigTemplate.REQUIRED);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("dsExt available but device is not a background video device - not setting zoom preference");
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("dsExt not available - not setting zoom preference");
            }
        }

        return configTemplate;
    }

    /**
     * Override the equals method. This is necessary for DSExt since if DSExt is
     * in place, several configs, each with different DFCs but the same handle
     * generated. In DSExt, configs are only equal when the handle and DFC are
     * equal.
     */
    public boolean equals(Object obj)
    {
        if (super.equals(obj))
        {
            return true;
        }
        if (obj instanceof HDVideoConfiguration)
        {
            HDVideoConfiguration hdvc = (HDVideoConfiguration)obj;
            return getUniqueId().equals(hdvc.getUniqueId());
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
