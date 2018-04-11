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

import java.awt.Dimension;

import org.dvb.media.VideoFormatControl;
import org.havi.ui.HBackgroundDevice;
import org.havi.ui.HBackgroundConfiguration;
import org.havi.ui.HConfigurationException;
import org.havi.ui.HGraphicsConfigTemplate;
import org.havi.ui.HGraphicsConfiguration;
import org.havi.ui.HGraphicsDevice;
import org.havi.ui.HPermissionDeniedException;
import org.havi.ui.HScreen;
import org.havi.ui.HScreenConfigTemplate;
import org.havi.ui.HVideoConfigTemplate;
import org.havi.ui.HBackgroundConfigTemplate;
import org.havi.ui.HScreenConfiguration;
import org.havi.ui.HScreenDevice;
import org.havi.ui.HScreenRectangle;
import org.havi.ui.HVideoConfiguration;
import org.havi.ui.HVideoDevice;

import org.cablelabs.impl.media.player.Util;

import org.ocap.hardware.VideoOutputPort;
import org.ocap.hardware.Host;
import org.ocap.hardware.device.FixedVideoOutputConfiguration;
import org.ocap.hardware.device.DynamicVideoOutputConfiguration;
import org.ocap.hardware.device.VideoOutputConfiguration;
import org.ocap.hardware.device.VideoOutputSettings;
import org.ocap.hardware.device.HostSettings;

public class HDScreenWithDS extends HDScreen
{
    private static final Logger log = Logger.getLogger(HDScreenWithDS.class.getName());

    HDScreenWithDS(int nScreen)
    {
        super(nScreen);

        if (log.isDebugEnabled())
        {
            log.debug("HDScreenWithDS: inside constructor");
        }
    }

    protected boolean setVideoOutputPortConfig()
    {
        if (log.isDebugEnabled())
        {
            log.debug("HDScreenWithDS: inside setVideoOutputPortConfig");
        }

        // set port config here
        HostSettings hostSettings = (HostSettings) Host.getInstance();
        VideoOutputPort port = hostSettings.getMainVideoOutputPort(this);
	    VideoOutputSettings VOPS = (VideoOutputSettings)port;

        HVideoDevice videoDevice = HScreen.getDefaultHScreen().getDefaultHVideoDevice();
        HScreenConfiguration videoDeviceConfig = videoDevice.getCurrentConfiguration();
        Dimension videoDeviceResolution = videoDeviceConfig.getPixelResolution();
        Dimension videoDevicePixelAspectRatio = videoDeviceConfig.getPixelAspectRatio();
        int desiredAspectRatio = Util.getAspectRatio(videoDeviceResolution.width * videoDevicePixelAspectRatio.width, 
                    videoDeviceResolution.height * videoDevicePixelAspectRatio.height);
        // Note that aspect ratio is in terms of VideoFormatControl.ASPECT_RATIO_XXXXXX enum

        return setPortVideoAspectRatio (desiredAspectRatio, VOPS);
    }

    private boolean setPortVideoAspectRatio (int desiredAspectRatio, VideoOutputSettings port)
    {
        if (log.isDebugEnabled()) log.debug("setPortVideoAspectRatio: port = " + port + ", desiredAspectRatio = " + desiredAspectRatio);

        VideoOutputConfiguration portConfig = port.getOutputConfiguration();
        if (portConfig instanceof FixedVideoOutputConfiguration)
        {
            FixedVideoOutputConfiguration portFixedConfig = (FixedVideoOutputConfiguration) portConfig;
            if (log.isDebugEnabled()) log.debug("current portFixedConfig.getAspectRatio = " + portFixedConfig.getVideoResolution().getAspectRatio());

            int currentStereoscopicMode = portFixedConfig.getVideoResolution().getStereoscopicMode();

            // Note: portFixedConfig.getVideoResolution().getAspectRatio() returns an VideoFormatControl.ASPECT_RATIO_XXXXX enum
            if (portFixedConfig.getVideoResolution().getAspectRatio() == desiredAspectRatio)
            {
                if (log.isDebugEnabled()) log.debug("current port video resolution matches desiredAspectRatio");

                // resolution matches, so we are done
                return true;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("current port video resolution does not match desiredAspectRatio -- searching for new config...");
                }

                VideoOutputConfiguration[] configs = port.getSupportedConfigurations();
                for (int i=0; i<configs.length; i++)
                {
                    if (configs[i] instanceof FixedVideoOutputConfiguration)
                    {
                        portFixedConfig = (FixedVideoOutputConfiguration) configs[i];
                        if (log.isDebugEnabled())
                        {
                            log.debug("FixedVideoOutputConfiguration video aspect ratio = " + portFixedConfig.getVideoResolution().getAspectRatio());
                        }

                        int stereoscopicMode = portFixedConfig.getVideoResolution().getStereoscopicMode();

                        if ((portFixedConfig.getVideoResolution().getAspectRatio() == desiredAspectRatio)
                            && (stereoscopicMode == currentStereoscopicMode))
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("MATCH found: changing video output config to " + configs[i]);
                            }

                            try
                            {
                                port.setOutputConfiguration (configs[i]);
                            }
                            catch (Exception ex)
                            {
                                if (log.isErrorEnabled())
                                {
                                    log.error("Error changing video output config to " + configs[i] + ": " + ex.getMessage());
                                }
                                return false;
                            }

                            return true;
                        }
                    }
                }
            }
        }
        else if (portConfig instanceof DynamicVideoOutputConfiguration)
        {
            // GORP: is this OK?
            return true;
        }

        return false;
    }

}
