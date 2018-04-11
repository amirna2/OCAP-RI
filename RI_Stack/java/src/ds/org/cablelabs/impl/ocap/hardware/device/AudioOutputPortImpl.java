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
import java.util.Vector;

import org.apache.log4j.Logger;

import org.ocap.hardware.VideoOutputPort;
import org.ocap.hardware.device.AudioOutputPort;
import org.ocap.hardware.device.FeatureNotSupportedException;

import org.cablelabs.impl.ocap.hardware.device.DeviceSettingsHostData.AudioPortValues;

/**
 * Implementation of AudioOutputPort
 * 
 * @author Alan Cossitt
 */
public class AudioOutputPortImpl extends AudioOutputPort implements Persistable
{
    private static final Logger log = Logger.getLogger(AudioOutputPortImpl.class.getName());

    // TODO: Need a flag to allow an invalid values to be persisted.
    private static final int defaultCompression = COMPRESSION_NONE;

    private static final int defaultEncoding = ENCODING_DISPLAY;

    private static final int defaultStereoMode = STEREO_MODE_STEREO;

    private Vector connectedVideoOutputPorts = new Vector();

    /*
     * JNI Calls
     */
    /**
     * Initializes JNI.
     */
    private static native void nInit();

    /**
     * Gets the data from the MPE layer and sets this object's fields.
     * 
     * @param nVideoPortHandle
     *            handle to owning video port
     * @return true if success
     */
    private native boolean nInitInfo(int nVideoPortHandle);

    /**
     * Set the compression in the MPE layer
     * 
     * @param nAudioPortHandle
     *            handle to this AudioOutputPort
     * @param vCompression
     *            value to set
     * 
     * @return true if successful, false if feature not supported.
     */
    private native boolean nSetCompression(int nAudioPortHandle, int vCompression, ActualValue actualValueObj);

    /**
     * Set the gain of an audio port
     * 
     * @param nAudioPortHandle
     *            MPEOS handle
     * @param vGain
     * @param actualValueObj
     *            the value actually set. It is possible that not all valid
     *            gains will be allowed.
     * 
     * @return true if successful
     */
    private native boolean nSetGain(int nAudioPortHandle, float vGain, ActualValue actualValueObj);

    /**
     * Set the level of an audio port
     * 
     * @param nAudioPortHandle
     *            MPEOS handle
     * @param level
     * @param actualValueObj
     *            the value actually set. It is possible that not all valid
     *            levels will be allowed.
     * 
     * @return true if successful
     */
    private native boolean nSetLevel(int nAudioPortHandle, float level, ActualValue actualValueObj);

    /**
     * Set the encoding of an audio port
     * 
     * @param nAudioPortHandle
     *            MPEOS handle
     * @param encoding
     * @param actualValueObj
     *            the value actually set. It is possible that not all valid
     *            encodings will be allowed.
     * 
     * @return true if successful, false if feature not supported
     */
    private native boolean nSetEncoding(int nAudioPortHandle, int vEncoding, ActualValue actualValueObj);

    /**
     * Set loopthru mode for the audio port
     * 
     * @param nAudioPortHandle
     *            MPEOS handle
     * @param loopThru
     * @return true if successful, false if not supported.
     */
    private native boolean nSetLoopThru(int nAudioPortHandle, boolean loopThru);

    /**
     * Mutes this audio port. Does not mute entire STB.
     * 
     * @param nAudioPortHandle
     *            MPEOS handle
     * @param muting
     * @return true if successful
     */
    private native boolean nSetMuting(int nAudioPortHandle, boolean muting);

    /**
     * Set stero mode for the audio port
     * 
     * @param nAudioPortHandle
     *            MPEOS handle
     * @param mode
     * @return true if successfull, false if feature not supported
     */
    private native boolean nSetStereoMode(int nAudioPortHandle, int mode);

    /*
     * fields initialized by JNI nInitInfo call.
     */
    private String uniqueId = "";

    private int compression = defaultCompression;

    private float gain = 0; // default according to javadoc

    private int encoding = defaultEncoding;

    private float level = 0;

    private float optimalLevel = 0;

    private float maxDb = 0;

    private float minDb = 0;

    private int stereoMode = defaultStereoMode;

    private int[] supportedCompressions = null;

    private int[] supportedEncodings = null;

    private int[] supportedStereoModes = null;

    private boolean loopThru = false;

    private boolean muted = false;

    private int audioPortHandle = -1;

    private AudioOutputPortImpl()
    {
        super();
    }

    /**
     * Package only constructor.
     * 
     * @param videoPortHandle
     * @param connectedVideoOutputPort
     */
    AudioOutputPortImpl(int videoPortHandle, VideoOutputPort connectedVideoOutputPort)
    {
        this();

        if (!nInitInfo(videoPortHandle))
        {
            if (log.isWarnEnabled())
            {
                 log.warn("Unable to initialize AudioOutputPortImpl from native layer! This may be due to an invalid configuration setting.");
            }
        }
        if (!this.connectedVideoOutputPorts.contains(connectedVideoOutputPort))
        {
            this.connectedVideoOutputPorts.add(connectedVideoOutputPort);
        }
        getPersistence().initAudioOutputPort(this);
    }

    public int getCompression()
    {
        return compression;
    }

    public Enumeration getConnectedVideoOutputPorts()
    {
        return this.connectedVideoOutputPorts.elements();
    }

    public float getDB()
    {
        return gain;
    }

    public int getEncoding()
    {
        return encoding;
    }

    public float getLevel()
    {
        return level;
    }

    public float getMaxDB()
    {
        return maxDb;
    }

    public float getMinDB()
    {
        return minDb;
    }

    public float getOptimalLevel()
    {
        return optimalLevel;
    }

    public int getStereoMode()
    {
        return stereoMode;
    }

    public int[] getSupportedCompressions()
    {
        return supportedCompressions;
    }

    public int[] getSupportedEncodings()
    {
        return supportedEncodings;
    }

    public int[] getSupportedStereoModes()
    {
        return supportedStereoModes;
    }

    public boolean isLoopThru()
    {
        return loopThru;
    }

    public boolean isMuted()
    {
        return muted;
    }

    public void setCompression(int compression) throws IllegalArgumentException, FeatureNotSupportedException
    {
        // first, make sure input is valid
        if (compression < AudioOutputPort.COMPRESSION_NONE || compression > AudioOutputPort.COMPRESSION_HEAVY)
        {
            throw new IllegalArgumentException("Invalid audio compression setting:" + compression);
        }
        if (getSupportedCompressions() == null || getSupportedCompressions().length == 0)
        {
            throw new FeatureNotSupportedException("Audio compression not supported.");
        }
        else
        {
            boolean compressionSupported = false;
            for (int i = 0; i < getSupportedCompressions().length; i++)
            {
                if (this.getSupportedCompressions()[i] == compression)
                {
                    compressionSupported = true;
                    break;
                }
            }
            if (!compressionSupported)
            {
                throw new FeatureNotSupportedException("Particular audio compression not supported:" + compression);
            }
        }
        if (compression == this.compression) return; // don't go through the
                                                     // overhead of setting the
                                                     // value if the value is
                                                     // unchanged.

        ActualValue av = new ActualValue();

        if (!nSetCompression(audioPortHandle, compression, av))
        {
            throw new IllegalArgumentException("Audio compression setting invalid: " + compression);
        }

        // don't set local copy until MPE layer successfully set
        this.compression = av.intValue;
        getPersistence().persistAudioCompression(getUniqueId(), this.compression);
    }

    public float setDB(float db)
    {
        if (db == this.gain) // don't go through the overhead of setting the
                             // value if the value is unchanged.
        {
            return this.gain;
        }

        ActualValue av = new ActualValue();

        if (nSetGain(audioPortHandle, db, av))
        {
            // don't set local copy until MPE layer successfully set
            this.gain = av.floatValue;
            getPersistence().persistAudioGain(getUniqueId(), this.gain);
        }

        return this.gain;
    }

    public void setEncoding(int encoding) throws IllegalArgumentException, FeatureNotSupportedException
    {
        if (encoding < ENCODING_NONE || encoding > ENCODING_AC3)
        {
            throw new IllegalArgumentException("Invalid encoding: " + encoding);
        }
        if (encoding == this.encoding) return; // don't go through the overhead
                                               // of setting the value if the
                                               // value is unchanged.

        ActualValue av = new ActualValue();

        if (!nSetEncoding(audioPortHandle, encoding, av))
        {
            throw new FeatureNotSupportedException("Encoding not supported: " + encoding);
        }

        // don't set local copy until MPE layer successfully set
        this.encoding = av.intValue;
        getPersistence().persistAudioEncoding(getUniqueId(), this.encoding);
    }

    public float setLevel(float level)
    {
        if (level == this.level) // don't go through the overhead of setting the
                                 // value if the value is unchanged.
        {
            return this.level;
        }

        ActualValue av = new ActualValue();

        if (nSetLevel(audioPortHandle, level, av))
        {
            // don't set local copy until MPE layer successfully set
            this.level = av.floatValue;
            getPersistence().persistAudioLevel(getUniqueId(), this.level);
        }

        return this.level;
    }

    public void setLoopThru(boolean loopThru) throws FeatureNotSupportedException
    {
        if (loopThru == this.loopThru) return; // don't go through the overhead
                                               // of setting the value if the
                                               // value is unchanged.

        if (!nSetLoopThru(audioPortHandle, loopThru))
        {
            throw new FeatureNotSupportedException("Could not set loop thru: " + loopThru);
        }
        // don't set local copy until MPE layer successfully set
        this.loopThru = loopThru;
        getPersistence().persistAudioLoopThru(getUniqueId(), this.loopThru);

    }

    public void setMuted(boolean mute)
    {
        if (mute == this.muted) return; // don't go through the overhead of
                                        // setting the value if the value is
                                        // unchanged.
        if (!nSetMuting(audioPortHandle, mute))
        {
            throw new RuntimeException("Could not set muted: " + mute);
        }
        // don't set local copy until MPE layer successfully set
        this.muted = mute;
        getPersistence().persistAudioMuted(getUniqueId(), this.muted);
    }

    public void setStereoMode(int mode) throws IllegalArgumentException, FeatureNotSupportedException
    {
        if (mode < STEREO_MODE_MONO || mode > STEREO_MODE_SURROUND)
        {
            throw new IllegalArgumentException("Invalid stereo mode: " + mode);
        }
        if (mode == this.stereoMode) return; // don't go through the overhead of
                                             // setting the value if the value
                                             // is unchanged.

        if (!nSetStereoMode(audioPortHandle, mode))
        {
            throw new FeatureNotSupportedException("Stereo mode = " + mode + " not supported.");
        }
        // don't set local copy until MPE layer successfully set
        this.stereoMode = mode;
        getPersistence().persistAudioStereoMode(getUniqueId(), this.stereoMode);

    }

    public String getUniqueId()
    {
        return uniqueId;
    }

    DeviceSettingsHostPersistence getPersistence()
    {
        return (DeviceSettingsHostPersistence) DeviceSettingsHostImpl.getHostPersistence();
    }

    /**
     * Initializes JNI.
     */
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
        nInit();
    }

    protected class ActualValue
    {
        public float floatValue;

        public int intValue;
    }

    public void initFromPersistence()
    {
        DeviceSettingsHostPersistence p = getPersistence();

        String portUid = getUniqueId();
        AudioPortValues values = p.getAudioValues(portUid);
        if (values == null)
        {
            p.getDSHostData().audioPortToValues.put(portUid, new AudioPortValues(this.getCompression(), this.getDB(),
                    this.getEncoding(), this.getLevel(), this.isLoopThru(), this.isMuted(), this.getStereoMode()));
        }
        else
        {
            try
            {
                setCompression(values.compression);
            }
            catch (IllegalArgumentException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Invalid audio compression value persisted - setting to default.");
                }
                try
                {
                    setCompression(AudioOutputPortImpl.defaultCompression);
                }
                catch (FeatureNotSupportedException err)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Audio compression not supported, but has been persisted!");
                    }
                }
            }
            catch (FeatureNotSupportedException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Audio compression not supported");
                }
            }

            if (values.gain >= this.minDb && values.gain <= this.maxDb)
            {
                setDB(values.gain);
            }
            else
            {
                setDB(0.0f);
            }

            try
            {
                setEncoding(values.encoding);
            }
            catch (IllegalArgumentException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Invalid audio encoding value persisted - setting to default.");
                }
                try
                {
                    setEncoding(AudioOutputPortImpl.defaultEncoding);
                }
                catch (FeatureNotSupportedException err)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Audio encoding not supported, but has been persisted!");
                    }
                }
            }
            catch (FeatureNotSupportedException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Audio encoding not supported");
                }
            }

            if (values.level >= 0.0 && values.level <= 1.0)
            {
                setLevel(values.level);
            }

            try
            {
                setLoopThru(values.loopThru);
            }
            catch (FeatureNotSupportedException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Audio loop through not supported");
                }
            }

            setMuted(values.muted);

            try
            {
                setStereoMode(values.stereoMode);
            }
            catch (IllegalArgumentException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Invalid audio stereo mode persisted - setting to first supported mode.");
                }
                try
                {
                    setStereoMode(this.getSupportedStereoModes()[0]);
                }
                catch (FeatureNotSupportedException err)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Audio stero mode not supported, but has been persisted!");
                    }
                }
            }
            catch (FeatureNotSupportedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Persisted audio stereo mode not supported!");
                }
                try
                {
                    setStereoMode(this.getSupportedStereoModes()[0]);
                }
                catch (FeatureNotSupportedException e2)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unable to restore stereo mode!");
                    }
                }
            }
        }
    }

}
