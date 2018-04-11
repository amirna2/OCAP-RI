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

import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.ocap.hardware.VideoOutputPort;
import org.ocap.hardware.device.FixedVideoOutputConfiguration;
import org.ocap.hardware.device.VideoOutputConfiguration;

import org.cablelabs.impl.ocap.hardware.HostData;
import org.cablelabs.impl.ocap.hardware.HostPersistence;
import org.cablelabs.impl.ocap.hardware.HostPersistenceException;
import org.cablelabs.impl.ocap.hardware.device.DeviceSettingsHostData.AudioPortValues;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;


/**
 * 
 * @author Alan Cossitt
 */
public class DeviceSettingsHostPersistence extends HostPersistence
{
    private static final Logger log = Logger.getLogger(DeviceSettingsHostPersistence.class.getName());

    transient private static final String VOLUME_RANGE = "OCAP.ds.hostSetting.volumeRange";

    transient private static final String MUTE_KEY_CONTROL = "OCAP.ds.hostSetting.muteKeyControl";

    transient private static final String VOLUME_KEY_CONTROL = "OCAP.ds.hostSetting.volumeKeyControl";

    transient private static final String POWER_MODE = "OCAP.ds.hostSetting.powerMode";

    transient private static final String MAIN_VIDEO_OUTPUT_PORT = "OCAP.ds.hostSetting.mainVideoOutputPort.0";

    Hashtable portToFixedConfigMapping = new Hashtable();

    public void persistVolumeRange(int value)
    {
        try
        {
            persistHostProperty(VOLUME_RANGE, value);
        }
        catch (HostPersistenceException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    public void persistMuteKeyControl(boolean value)
    {
        try
        {
            persistHostProperty(MUTE_KEY_CONTROL, value);
        }
        catch (HostPersistenceException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    public void persistVolumeKeyControl(boolean value)
    {
        try
        {
            persistHostProperty(VOLUME_KEY_CONTROL, value);
        }
        catch (HostPersistenceException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    /*
     * PowerMode should not be persisted. This kludge persists the default value
     * (StandBy mode
     */
    public void persistPowerMode(int value)
    {
        String s_value = MPEEnv.getEnv(POWER_MODE, null);
        if (s_value == null)
        {
            s_value = "2";
            if (log.isDebugEnabled())
            {
                log.debug("Missing POWER_MODE value in file: Setting default value " + value);
            }
            // throw new
            // RuntimeException("Unable to initialize DSExt persistence");
        }
        hostDataPut(POWER_MODE, s_value);

        value = Integer.parseInt(s_value);

        try
        {
            persistHostProperty(POWER_MODE, value);
        }
        catch (HostPersistenceException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    public void persistMainVideoOutputPort(String portUniqueId)
    {
        try
        {
            persistHostProperty(MAIN_VIDEO_OUTPUT_PORT, portUniqueId);
        }
        catch (HostPersistenceException e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    public void persistAudioCompression(String portUid, int compression)
    {
        AudioPortValues values = getAudioValues(portUid);
        values.compression = compression;

        writeHostData();
    }

    public void persistAudioGain(String portUid, float gain)
    {
        AudioPortValues values = getAudioValues(portUid);
        values.gain = gain;

        writeHostData();
    }

    public void persistAudioEncoding(String portUid, int encoding)
    {
        AudioPortValues values = getAudioValues(portUid);
        values.encoding = encoding;

        writeHostData();
    }

    public void persistAudioLevel(String portUid, float level)
    {
        AudioPortValues values = getAudioValues(portUid);
        values.level = level;

        writeHostData();
    }

    public void persistAudioLoopThru(String portUid, boolean loopThru)
    {
        AudioPortValues values = getAudioValues(portUid);
        values.loopThru = loopThru;

        writeHostData();
    }

    public void persistAudioMuted(String portUid, boolean muted)
    {
        AudioPortValues values = getAudioValues(portUid);
        values.muted = muted;

        writeHostData();
    }

    public void persistAudioStereoMode(String portUid, int stereoMode)
    {
        AudioPortValues values = getAudioValues(portUid);
        values.stereoMode = stereoMode;

        writeHostData();
    }

    public void persistPortOutputConfig(String portUid, VideoOutputConfiguration config)
    {
        if (portUid == null || config == null)
            throw new IllegalArgumentException("param is null, portUid=" + portUid + ", config=" + config);

        Object previousConfig = putVideoPortOutputConfig(portUid, config);

        // every port should already have a config set
        if (previousConfig == null) throw new IllegalArgumentException("portUid is invalid " + portUid);

        writeHostData();
    }

    public int getVolumeRange()
    {
        String fromHost = getHostProperty(VOLUME_RANGE);
        if (fromHost == null)
        {
            fromHost = "2";
        }
        return Integer.parseInt(fromHost);
    }

    public boolean getMuteKeyControl()
    {
        return Boolean.valueOf(getHostProperty(MUTE_KEY_CONTROL)) == Boolean.TRUE;
    }

    public boolean getVolumeKeyControl()
    {
        return Boolean.valueOf(getHostProperty(VOLUME_KEY_CONTROL)) == Boolean.TRUE;
    }

    public int getPowerMode()
    {
        String fromHost = getHostProperty(POWER_MODE);
        if (fromHost == null)
        {
            fromHost = "2";
        }
        return Integer.parseInt(fromHost);
    }

    /**
     * getMainVideoOutputPort
     * 
     * For HostSettings. This is the video port for the HScreen
     * 
     * @return uniqueId of the video port
     */
    public String getMainVideoOutputPort()
    {
        return getHostProperty(MAIN_VIDEO_OUTPUT_PORT);
    }

    public void initHostSettings(HostSettingsProxy proxy)
    {
        proxy.initFromPersistence();
    }

    public void initHostSettings(DeviceSettingsHostImpl impl)
    {
        impl.initFromPersistence();
    }

    public void initVideoOutputSettings(VideoOutputSettingsProxy proxy)
    {
        VideoOutputConfiguration validConfig = null;

        // proxy.getUniqueId returns the unique id of the port
        String portUniqueId = proxy.getUniqueId();

        if (getDSHostData().isPersisted()) // starting from persisted
                                           // (serialized) values
        {
            // get the persisted config
            VideoOutputConfiguration persistedConfig;
            try
            {
                persistedConfig = getVideoPortOutputConfig(portUniqueId);
                if (persistedConfig instanceof FixedVideoOutputConfiguration)
                {
                    // need to replace the config that is in the host data with
                    // the new config. This
                    // makes sure that the handle of the config and other
                    // information is up-to-date.
                    // all supported configs are Fixed
                    validConfig = proxy.findSupportedConfiguration(persistedConfig.getName());
                    if (validConfig == null)
                    {
                        validConfig = proxy.getOutputConfiguration();
                    }
                }
                else
                {
                    validConfig = persistedConfig;
                }
            }
            catch (IllegalArgumentException err)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Attempt to read configuration for port from persisted context failed! Id = "
                            + portUniqueId);
                }
                validConfig = proxy.getOutputConfiguration();
            }
        }
        else
        {
            validConfig = proxy.getOutputConfiguration();
        }
        putVideoPortOutputConfig(portUniqueId, validConfig);
        // proxy will be responsible for setting the output config or any other
        // action that
        // changes the state of the proxy
        proxy.initFromPersistence(validConfig);
    }

    void initAudioOutputPort(AudioOutputPortImpl port)
    {
        port.initFromPersistence();
    }

    public int getAudioCompression(String portUid)
    {
        AudioPortValues values = getAudioValues(portUid);

        return values.compression;
    }

    public float getAudioGain(String portUid)
    {
        AudioPortValues values = getAudioValues(portUid);

        return values.gain;
    }

    public int getAudioEncoding(String portUid)
    {
        AudioPortValues values = getAudioValues(portUid);

        return values.encoding;
    }

    public float getAudioLevel(String portUid)
    {
        AudioPortValues values = getAudioValues(portUid);

        return values.level;
    }

    public boolean getAudioLoopThru(String portUid)
    {
        AudioPortValues values = getAudioValues(portUid);

        return values.loopThru;
    }

    public boolean getAudioMuted(String portUid)
    {
        AudioPortValues values = getAudioValues(portUid);

        return values.muted;
    }

    public int getAudioStereoMode(String portUid)
    {
        AudioPortValues values = getAudioValues(portUid);

        return values.stereoMode;
    }

    public VideoOutputConfiguration getVideoPortOutputConfig(String portUid)
    {
        VideoOutputConfiguration config = (VideoOutputConfiguration) getDSHostData().videoPortToOutputConfig.get(portUid);
        if (config == null) throw new IllegalArgumentException("Video Port not found: " + portUid);

        return config;
    }

    /**
     * Maps a video port (through unique ID) to its output configuration. One
     * output config per port.
     * 
     * @param portUid
     * @param config
     * @return
     */
    private Object putVideoPortOutputConfig(String portUid, VideoOutputConfiguration config)
    {
        if (portUid == null || config == null)
            throw new IllegalArgumentException("null param, portUid=" + portUid + ", config=" + config);

        // debug // config = new FixedVideoOutputConfigurationImpl(false,
        // "alantest", new VideoResolution(new Dimension(16,9),2, 60.0f, 2));

        Object previousConfig = getDSHostData().videoPortToOutputConfig.put(portUid, config);
        return previousConfig;
    }

    AudioPortValues getAudioValues(String portUid)
    {
        AudioPortValues values = (AudioPortValues) getDSHostData().audioPortToValues.get(portUid);

        // checkValidAudioValues(portUid, values);

        return values;

    }

    private void checkValidAudioValues(String portUid, AudioPortValues values)
    {
        if (values == null) throw new IllegalArgumentException("Audio Port not found:  " + portUid);
    }

    /*
     * Get data from persistence
     */
    protected DeviceSettingsHostData getDSHostData()
    {
        return (DeviceSettingsHostData) super.getHostData();
    }

    protected HostData newHostData()
    {
        return new DeviceSettingsHostData();
    }
}
