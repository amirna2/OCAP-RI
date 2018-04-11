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

package org.cablelabs.impl.ocap.hardware.device;

import java.util.Enumeration;
import java.util.Hashtable;
import java.awt.Dimension;

import org.apache.log4j.Logger;
import org.dvb.media.VideoFormatControl;

import org.havi.ui.*;

import org.ocap.hardware.device.*;
import org.ocap.hardware.VideoOutputPort;

import org.cablelabs.impl.havi.port.mpe.HDScreen;
import org.cablelabs.impl.havi.port.mpe.HDVideoDevice;
import org.cablelabs.impl.manager.host.DeviceSettingsHostManagerImpl;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.MediaAPIManager;
import org.cablelabs.impl.media.mpe.MediaAPI;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.media.player.Util;



/**
 * 
 * @author Alan Cossitt
 */
public class HostSettingsProxy implements HostSettings
{
    private Hashtable screenPortMap = new Hashtable();

    private MediaAPI mediaAPI;

    // Initialize all field and method IDs used in this class.
    // private static native boolean nInit(); // not needed at this point

    private native boolean nSetSystemMuteKeyControl(boolean enable);

    private native boolean nSetSystemVolumeKeyControl(boolean enable);

    private native boolean nSetSystemVolumeRange(int range);

    private static final Logger log = Logger.getLogger("HostSettingsProxy");

    HostSettingsProxy()
    {
        // the following calls initFromPersistence
        getPersistence().initHostSettings(this);
    }

    public Enumeration getAudioOutputs()
    {
        checkDSExtPermissions();

        return DeviceSettingsVideoOutputPortImpl.getAudioOutputPorts();
    }

    public VideoOutputPort getMainVideoOutputPort(HScreen screen)
    {
        VideoOutputPort port = (VideoOutputPort) screenPortMap.get(screen);
        return port;
    }

    public void resetAllDefaults() throws SecurityException
    {
        checkDSExtPermissions();

        getPersistence().resetAllDefaults();

        getPersistence().initHostSettings(this);

    }

    void initFromPersistence()
    {
        DeviceSettingsHostPersistence p = getPersistence();

        setSystemMuteKeyControlNoPerm(p.getMuteKeyControl());
        setSystemVolumeKeyControlNoPerm(p.getVolumeKeyControl());
        setSystemVolumeRangeNoPerm(p.getVolumeRange());

        String videoOutputPortId =  p.getMainVideoOutputPort();
        if (log.isDebugEnabled())
        {
            log.debug("initFromPersistence: videoOutputPortId = " + videoOutputPortId);
        }
            
        DeviceSettingsVideoOutputPortImpl port = DeviceSettingsVideoOutputPortImpl.findPort(videoOutputPortId);
        if (log.isDebugEnabled())
        {
            log.debug("port = " + port);
        }

        boolean bIniOverridesPersistence = false;
        String var;
        if ((var = MPEEnv.getEnv("OCAP.overide.persisted.video.port")) != null)
        {
            if (var.equalsIgnoreCase("true"))
            {
                bIniOverridesPersistence = true;
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("bIniOverridesPersistence = " + bIniOverridesPersistence);
        }


        HGraphicsDevice graphicsDevice = HScreen.getDefaultHScreen().getDefaultHGraphicsDevice();
        HScreenConfiguration graphicsDeviceConfig = graphicsDevice.getCurrentConfiguration();
        Dimension graphicsDeviceResolution = graphicsDeviceConfig.getPixelResolution();
        if (log.isDebugEnabled())
        {
            log.debug("graphics resolution = " + graphicsDeviceResolution);
        }

        HBackgroundDevice backgroundDevice = HScreen.getDefaultHScreen().getDefaultHBackgroundDevice();
        HScreenConfiguration backgroundDeviceConfig = backgroundDevice.getCurrentConfiguration();
        Dimension backgroundDeviceResolution = backgroundDeviceConfig.getPixelResolution();
        if (log.isDebugEnabled())
        {
            log.debug("background resolution = " + backgroundDeviceResolution);
        }

        HVideoDevice videoDevice = HScreen.getDefaultHScreen().getDefaultHVideoDevice();
        HScreenConfiguration videoDeviceConfig = videoDevice.getCurrentConfiguration();
        Dimension videoDeviceResolution = videoDeviceConfig.getPixelResolution();
        Dimension videoDevicePixelAspectRatio = videoDeviceConfig.getPixelAspectRatio();
        int videoDeviceAspectRatio = Util.getAspectRatio(videoDeviceResolution.width * videoDevicePixelAspectRatio.width, 
            videoDeviceResolution.height * videoDevicePixelAspectRatio.height);
        // Note that aspect ratio is in terms of VideoFormatControl.ASPECT_RATIO_XXXXXX enum

        if (log.isDebugEnabled())
        {
            log.debug("video device resolution = " + videoDeviceResolution);
        }

/* for testing
        Enumeration videoOutputPorts = DeviceSettingsVideoOutputPortImpl.getVideoOutputPorts();
        while (videoOutputPorts.hasMoreElements())
        {
                DeviceSettingsVideoOutputPortImpl portTemp = (DeviceSettingsVideoOutputPortImpl) videoOutputPorts.nextElement();
                if (Logging.LOGGING) log.info("portTemp = " + portTemp);


                VideoOutputConfiguration currentConfig = portTemp.getOutputConfiguration();
                if (Logging.LOGGING) log.info("currentConfig = " + currentConfig);
                if (currentConfig instanceof FixedVideoOutputConfiguration)
                {
                    FixedVideoOutputConfiguration tempCurrentConfig = (FixedVideoOutputConfiguration) currentConfig;
                    if (Logging.LOGGING) log.info("tempCurrentConfig.getVideoResolution = " + tempCurrentConfig.getVideoResolution().getPixelResolution());
                }
                else if (currentConfig instanceof DynamicVideoOutputConfiguration)
                {
                    FixedVideoOutputConfiguration tempCurrentConfig1 = ((DynamicVideoOutputConfiguration) currentConfig).getOutputResolution (DynamicVideoOutputConfiguration.INPUT_SD);
                    if (Logging.LOGGING) log.info("tempCurrentConfig1.getVideoResolution = " + tempCurrentConfig1.getVideoResolution().getPixelResolution());
                    FixedVideoOutputConfiguration tempCurrentConfig2 = ((DynamicVideoOutputConfiguration) currentConfig).getOutputResolution (DynamicVideoOutputConfiguration.INPUT_HD);
                    if (Logging.LOGGING) log.info("tempCurrentConfig2.getVideoResolution = " + tempCurrentConfig2.getVideoResolution().getPixelResolution());
                }



                VideoOutputConfiguration[] configs = portTemp.getSupportedConfigurations();
                for (int i=0; i<configs.length; i++)
                {
                    if (Logging.LOGGING) log.info("configs[" + i + "] = " + configs[i]);

                    if (configs[i] instanceof FixedVideoOutputConfiguration)
                    {
                        FixedVideoOutputConfiguration tempConfig = (FixedVideoOutputConfiguration) configs[i];
                        if (Logging.LOGGING) log.info("tempConfig.getVideoResolution = " + tempConfig.getVideoResolution().getPixelResolution());
                    }
                    else if (configs[i] instanceof DynamicVideoOutputConfiguration)
                    {
                        FixedVideoOutputConfiguration tempConfig1 = ((DynamicVideoOutputConfiguration) configs[i]).getOutputResolution (DynamicVideoOutputConfiguration.INPUT_SD);
                        if (Logging.LOGGING) log.info("tempConfig1.getVideoResolution = " + tempConfig1.getVideoResolution().getPixelResolution());
                        FixedVideoOutputConfiguration tempConfig2 = ((DynamicVideoOutputConfiguration) configs[i]).getOutputResolution (DynamicVideoOutputConfiguration.INPUT_HD);
                        if (Logging.LOGGING) log.info("tempConfig2.getVideoResolution = " + tempConfig2.getVideoResolution().getPixelResolution());
                    }
            }
        }
*/


        try
        {
            if (port == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Port NOT persisted");
                }
                /*
                No persisted port.  

            	1) get the desired video aspect ratio from the current coherent config
	            2) walk list of VideoOutputPorts getting their list of supported configs
		            a) if we find one that supports the desired video aspect ratio, then choose it as the main VideoOutputPort 
                        (just pick first match)
		            b) if we do not find one that supports the desired video aspect ratio, then pick the coherent config with 
                        the same graphics resolution, but with the other the other videoaspect ratio (SD or HD), and with the 
                        lowest background resolution.  Then rewalk the VideOutputPort list, picking first match to the video 
                        aspect ratio.
		            c) set this VideoOutputPort as main VideoOutputPort associated with the HScreen and setOutputConfiguration on 
                        video port
                */

                if (log.isDebugEnabled())
                {
                    log.debug("Looking for port to match videoDeviceAspectRatio " + videoDeviceAspectRatio);
                }
                port = findMatchingPort(videoDeviceAspectRatio);

                if (port == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("no matching port found");
                    }

                    // no matching port found, switch video from SD -> HD or vice versa, and change coherent config
                    int newVideoAspectRatio = VideoFormatControl.ASPECT_RATIO_4_3;
                    if (videoDeviceAspectRatio == VideoFormatControl.ASPECT_RATIO_4_3)
                    {
                        newVideoAspectRatio = VideoFormatControl.ASPECT_RATIO_16_9;
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("Looking for port to match videoDeviceAspectRatio " + newVideoAspectRatio);
                    }
                    port = findMatchingPort(newVideoAspectRatio);
                    if (port == null)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("no matching port found");
                        }
                        throw new IllegalArgumentException("Main Video Output Port supporting video aspect ratio not found.");
                    }

                    HDScreen screen = (HDScreen)(HScreen.getDefaultHScreen());
                    if (log.isDebugEnabled())
                    {
                        log.debug("Setting coherent config: VideoAspectRatio = " + newVideoAspectRatio +
                        ", graphicsDeviceResolution = " + graphicsDeviceResolution);
                    }
                    
                    int desiredDFC = 0;
                    if (videoDevice instanceof HDVideoDevice)
                    {
                        desiredDFC = getMediaAPI().getDFC(((HDVideoDevice)videoDevice).getHandle());
                    }
                    screen.setCoherentConfiguration (newVideoAspectRatio, graphicsDeviceResolution, desiredDFC);
                }
            }
            else if (bIniOverridesPersistence)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Port persisted and IniOveride true");
                }
                 /*
                Port persisted, but ini file override boolean in ini file is set to true

                1) get the desired video aspect ratio from the current coherent config
                2) see if video aspect ratio of persisted main VideoOutputPort matches desired video aspect ratio.
                    a) if so, we are done
                    b) if not, 
                        i) walk the list of supported configs for the current video output port -- if we
                           find one that matches the desired video aspect ratio, change the config
                           on current port to this one.
                	 ii) if there is no match in the supported configs of the current video output port, 
                                1) walk list of VideoOutputPorts getting their lists of supported configs
                	            2) if we find one that supports the desired video aspect ratio, then choose it as 
                			       the main VideoOutputPort
                			    3) if we do not find one that supports the desired video aspect ratio, then 
                			        pick the coherent config with the same graphics resolution, but with the other 
                			        video aspect ratio (SD or HD), and with the lowest background resolution.  Apply this coherent config.
                			        Then rewalk the VideOutputPort list, picking first match to the video aspect ratio.
                			    4) set this VideoOutputPort as main VideoOutputPort associated with the HScreen
                */

                if (log.isDebugEnabled())
                {
                    log.debug("Calling setPortVideoAspectRatio with videoDeviceAspectRatio = " + videoDeviceAspectRatio);
                }
                if (!setPortVideoAspectRatio(videoDeviceAspectRatio, port))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("failed setting port video aspect ratio -- finding new port");
                    }
                    port = findMatchingPort(videoDeviceAspectRatio);
                    
                    if (port == null)
                    {
                        // no matching port found, switch video from SD -> HD or vice versa, and change coherent config
                        int newVideoAspectRatio = VideoFormatControl.ASPECT_RATIO_4_3;
                        if (videoDeviceAspectRatio == VideoFormatControl.ASPECT_RATIO_4_3)
                        {
                            newVideoAspectRatio = VideoFormatControl.ASPECT_RATIO_16_9;
                        }
                        if (log.isDebugEnabled())
                        {
                            log.debug("no matching port found, switching video aspect ratio to " + newVideoAspectRatio);
                        }

                        port = findMatchingPort(newVideoAspectRatio);
                        if (port == null)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("no matching port found");
                            }
                            throw new IllegalArgumentException("Main Video Output Port supporting video aspect ratio not found.");
                        }

                        HDScreen screen = (HDScreen)(HScreen.getDefaultHScreen());
                        if (log.isDebugEnabled())
                        {
                            log.debug("Setting coherent config: VideoAspectRatio = " + newVideoAspectRatio +
                            ", graphicsDeviceResolution = " + graphicsDeviceResolution);
                        }

                        int desiredDFC = 0;
                        if (videoDevice instanceof HDVideoDevice)
                        {
                            desiredDFC = getMediaAPI().getDFC(((HDVideoDevice)videoDevice).getHandle());
                        }
                        screen.setCoherentConfiguration (newVideoAspectRatio, graphicsDeviceResolution, desiredDFC);
                    }
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Port persisted and IniOveride false");
                }

                /*
                Port persisted, and ini file override boolean in ini file is set to false

         	    1) get the desired video aspect ratio from the current coherent config
              	2) see if video aspect ratio of persisted main VideoOutputPort matches desired video aspect ratio.
                    a) if so, we are done
              	    b) if not, then pick the coherent config with the same graphics resolution, but with the 
                        other video aspect ratio (SD or HD), and with the lowest background resolution.  
                        Apply this coherent config.
                */

                if (log.isDebugEnabled())
                {
                    log.debug("Calling setPortVideoAspectRatio with videoDeviceAspectRatio = " + videoDeviceAspectRatio);
                }
                if (!setPortVideoAspectRatio(videoDeviceAspectRatio, port))
                {
                    // no matching port found, switch video from SD -> HD or vice versa, and change coherent config
                    int newVideoAspectRatio = VideoFormatControl.ASPECT_RATIO_4_3;
                    if (videoDeviceAspectRatio == VideoFormatControl.ASPECT_RATIO_4_3)
                    {
                        newVideoAspectRatio = VideoFormatControl.ASPECT_RATIO_16_9;
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("failed setting port video aspect ratio -- switching video aspect ratio to " + newVideoAspectRatio);
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("Calling setPortVideoAspectRatio with videoDeviceAspectRatio = " + newVideoAspectRatio);
                    }
                    if (!setPortVideoAspectRatio(newVideoAspectRatio, port))
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("failed setting port video aspect ratio");
                        }
                        throw new IllegalArgumentException("Failed setting port video aspect ratio");
                    }

                    HDScreen screen = (HDScreen)(HScreen.getDefaultHScreen());
                    if (log.isDebugEnabled())
                    {
                        log.debug("Setting coherent config: VideoAspectRatio = " + newVideoAspectRatio +
                        ", graphicsDeviceResolution = " + graphicsDeviceResolution);
                    }

                    int desiredDFC = 0;
                    if (videoDevice instanceof HDVideoDevice)
                    {
                        desiredDFC = getMediaAPI().getDFC(((HDVideoDevice)videoDevice).getHandle());
                    }
                    screen.setCoherentConfiguration (newVideoAspectRatio, graphicsDeviceResolution, desiredDFC);
                }
            }


            if (log.isDebugEnabled())
            {
                log.debug("calling setMainVideoOutputPort: port = " + port);
            }
            setMainVideoOutputPort(HScreen.getDefaultHScreen(), port);
        }
        catch (FeatureNotSupportedException e)
        {
            ; // do nothing
        }
    }
            
    private void printConfig(DeviceSettingsVideoOutputPortImpl port)
    {
        if (log.isInfoEnabled())
        {
            VideoOutputConfiguration currentConfig = port.getOutputConfiguration();
            if (log.isInfoEnabled())
            {
                log.info("currentConfig = " + currentConfig);
            }
            if (currentConfig instanceof FixedVideoOutputConfiguration)
            {
                FixedVideoOutputConfiguration tempCurrentConfig = (FixedVideoOutputConfiguration) currentConfig;
                if (log.isInfoEnabled())
                {
                    log.info("tempCurrentConfig.getVideoResolution = " + tempCurrentConfig.getVideoResolution().getPixelResolution());
                }
            }
            else if (currentConfig instanceof DynamicVideoOutputConfiguration)
            {
                FixedVideoOutputConfiguration tempCurrentConfig1 = ((DynamicVideoOutputConfiguration) currentConfig).getOutputResolution (DynamicVideoOutputConfiguration.INPUT_SD);
                if (log.isInfoEnabled())
                {
                    log.info("tempCurrentConfig1.getVideoResolution = " + tempCurrentConfig1.getVideoResolution().getPixelResolution());
                }
                FixedVideoOutputConfiguration tempCurrentConfig2 = ((DynamicVideoOutputConfiguration) currentConfig).getOutputResolution (DynamicVideoOutputConfiguration.INPUT_HD);
                if (log.isInfoEnabled())
                {
                    log.info("tempCurrentConfig2.getVideoResolution = " + tempCurrentConfig2.getVideoResolution().getPixelResolution());
                }
        }
    }
    }

    // setPortVideoAspectRatio preserves the current stereoscopic mode of the video output port
    private boolean setPortVideoAspectRatio (int desiredAspectRatio, DeviceSettingsVideoOutputPortImpl port) throws FeatureNotSupportedException
    {
        if (log.isDebugEnabled())
        {
            log.debug("setPortVideoAspectRatio: port = " + port + ", desiredAspectRatio = " + desiredAspectRatio);
        }

        VideoOutputConfiguration portConfig = port.getOutputConfiguration();
        if (portConfig instanceof FixedVideoOutputConfiguration)
        {
            FixedVideoOutputConfiguration portFixedConfig = (FixedVideoOutputConfiguration) portConfig;
            if (log.isDebugEnabled())
            {
                log.debug("current portFixedConfig.getAspectRatio = " + portFixedConfig.getVideoResolution().getAspectRatio());
            }

            int currentStereoscopicMode = portFixedConfig.getVideoResolution().getStereoscopicMode();

            // Note: portFixedConfig.getVideoResolution().getAspectRatio() returns an VideoFormatControl.ASPECT_RATIO_XXXXX enum
            if (portFixedConfig.getVideoResolution().getAspectRatio() == desiredAspectRatio)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("current port video resolution matches desiredAspectRatio");
                }

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
                            port.setOutputConfiguration (configs[i]);
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

    private DeviceSettingsVideoOutputPortImpl findMatchingPort(int desiredAspectRatio) throws FeatureNotSupportedException
    {
        if (log.isDebugEnabled())
        {
            log.debug("findMatchingPort: desiredAspectRatio = " + desiredAspectRatio);
        }

        Enumeration videoOutputPorts = DeviceSettingsVideoOutputPortImpl.getVideoOutputPorts();
        while (videoOutputPorts.hasMoreElements())
        {
            DeviceSettingsVideoOutputPortImpl portTemp = (DeviceSettingsVideoOutputPortImpl) videoOutputPorts.nextElement();

            if (log.isDebugEnabled())
            {
                log.debug("walking ports: port = " + portTemp);
            }
            if (log.isDebugEnabled())
            {
                log.debug("walking ports: port connected = " + portTemp.isDisplayConnected());
            }

            if (!portTemp.isDisplayConnected())
            {
                continue;
            }

            VideoOutputConfiguration[] configs = portTemp.getSupportedConfigurations();
            for (int i=0; i<configs.length; i++)
            {
                if (configs[i] instanceof FixedVideoOutputConfiguration)
                {
                    FixedVideoOutputConfiguration tempFixedConfig = (FixedVideoOutputConfiguration) configs[i];
                    if (log.isDebugEnabled())
                    {
                        log.debug("FixedVideoOutputConfiguration video aspect ratio = " + tempFixedConfig.getVideoResolution().getAspectRatio());
                    }

                    if (tempFixedConfig.getVideoResolution().getAspectRatio() == desiredAspectRatio)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("MATCH found: changing video port to " + portTemp);
                        }
                        portTemp.setOutputConfiguration (configs[i]);
                        return portTemp;
                    }
                }
            }
        }

        // no port supports this video resolution
        return null;
    }

    private DeviceSettingsHostPersistence getPersistence()
    {
        return (DeviceSettingsHostPersistence) DeviceSettingsHostImpl.getHostPersistence();
    }

    public void setMainVideoOutputPort(HScreen screen, VideoOutputPort port) throws FeatureNotSupportedException
    {
        checkDSExtPermissions();

        DeviceSettingsVideoOutputPortImpl portImpl = setMainVideoOutputPortNoPerm(screen, port, true);

        if (portImpl != null) // something needs to be persisted if portImpl !=
                              // null
        {
            if (log.isDebugEnabled())
            {
                log.debug("persisting " + portImpl);
            }
            Persistable p = (Persistable) portImpl;
            getPersistence().persistMainVideoOutputPort(p.getUniqueId());
        }
    }

    DeviceSettingsVideoOutputPortImpl setMainVideoOutputPortNoPerm(HScreen screen, VideoOutputPort port,
            boolean updateAspectRatio) throws FeatureNotSupportedException
    {
        VideoOutputPort currPort = (VideoOutputPort) screenPortMap.get(screen);
        // since this process is expensive, make sure that there is a change before modifying anything

        if (currPort == null || !currPort.equals(port))
        {
            if (log.isDebugEnabled())
            {
                log.debug("changing port to " + port);
            }
            screenPortMap.put(screen, port);

            DeviceSettingsVideoOutputPortImpl portImpl = (DeviceSettingsVideoOutputPortImpl) port;

            if (updateAspectRatio) 
            {
                // Note: portImpl.getAspectRatio() returns a VideoFormatControl.ASPECT_RATIO_XXXXXX enum
                if (log.isDebugEnabled())
                {
                    log.debug("Calling updateHScreenAspectRatio with aspect ratio " + portImpl.getAspectRatio());
                }
                updateHScreenAspectRatio(screen, portImpl.getAspectRatio());
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("NOT calling updateHScreenAspectRatio");
                }
            }

            return portImpl;
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("port " + port + " already set -- NOT changing port");
            }
        }

        return null;
    }

    /**
     * 
     * Code will not do anything if the current aspect ratio matches the
     * requested aspect ratio. Aspect ratio isn't checked here because this
     * object doesn't have easy access to the needed information. The HDScreen
     * has the needed information internally.
     * 
     * @param screen
     * @param portImpl
     */
    private void updateHScreenAspectRatio(HScreen screen, int aspectRatio)
    {
        if (aspectRatio != VideoFormatControl.ASPECT_RATIO_UNKNOWN)
        {
            ((HDScreen) screen).setCoherentConfiguration(aspectRatio, true);
        }
    }

    public void handlePortDisconnected(int videoPortHandle, int eventCounter)
    {
        // do nothing (don't change the HScreen aspect ratio)

    }

    public void handlePortConnected(int videoPortHandle, int eventCounter)
    {
        // if the video port handle represents the main video port for an
        // HScreen, then change the
        // aspect ratio of the HScreen if necessary.

        Enumeration screenE = screenPortMap.keys();
        while (screenE.hasMoreElements())
        {
            HDScreen screen = (HDScreen) screenE.nextElement();
            DeviceSettingsVideoOutputPortImpl portImpl = (DeviceSettingsVideoOutputPortImpl) screenPortMap.get(screen);

            portImpl.waitForRefresh(eventCounter); // make sure the aspect ratio
                                                   // and other values are
                                                   // updated and correct

            if (portImpl.getHandle() == videoPortHandle)
            {
                updateHScreenAspectRatio(screen, portImpl.getAspectRatio());
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.ocap.hardware.device.HostSettings#setPowerMode(int)
     * 
     * Power modes can not be handled by the proxy. Only Host and Host derived
     * classes can do this correctly.
     */
    public void setPowerMode(int mode)
    {
        throw new IllegalArgumentException("HostSettingsProxy.setPowerMode should not be called");
    }

    public void setSystemMuteKeyControl(boolean enable)
    {
        checkDSExtPermissions();

        setSystemMuteKeyControlNoPerm(enable);

        getPersistence().persistMuteKeyControl(enable);
    }

    void setSystemMuteKeyControlNoPerm(boolean enable)
    {
        if (!nSetSystemMuteKeyControl(enable))
        {
            SystemEventUtil.logCatastrophicError(new RuntimeException("nSetSystemMuteKeyControl failed"));
        }
    }

    public void setSystemVolumeKeyControl(boolean enable)
    {
        checkDSExtPermissions();

        setSystemVolumeKeyControlNoPerm(enable);

        getPersistence().persistVolumeKeyControl(enable);
    }

    void setSystemVolumeKeyControlNoPerm(boolean enable)
    {
        if (!nSetSystemVolumeKeyControl(enable))
        {
            SystemEventUtil.logCatastrophicError(new RuntimeException("nSetSystemVolumeKeyControl failed"));
        }
    }

    public void setSystemVolumeRange(int range)
    {
        checkDSExtPermissions();

        setSystemVolumeRangeNoPerm(range);

        getPersistence().persistVolumeRange(range);

    }

    void setSystemVolumeRangeNoPerm(int range)
    {
        if (range == HostSettings.RANGE_NARROW || range == HostSettings.RANGE_NORMAL
                || range == HostSettings.RANGE_WIDE)
        {
            if (!nSetSystemVolumeRange(range))
            {
                RuntimeException e = new RuntimeException("nSetSystemVolumeRange failed");
                SystemEventUtil.logCatastrophicError(e);
                throw e;
            }
        }
        else
        {
            throw new IllegalArgumentException("System Volume range not allowed value, range=" + range);
        }
    }

    private void checkDSExtPermissions()
    {
        DeviceSettingsHostManagerImpl.checkPermissions();
    }

    public MediaAPI getMediaAPI()
    {
        if (mediaAPI == null)
        {
            mediaAPI = (MediaAPI) ManagerManager.getInstance(MediaAPIManager.class);
        }

        return mediaAPI;
    }

    /**
     * Initializes JNI.
     */
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }

}
