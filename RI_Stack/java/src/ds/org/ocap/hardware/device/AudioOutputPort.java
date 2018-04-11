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

package org.ocap.hardware.device;

import javax.media.GainControl;

/**
 * Represents an individually controllable audio output port of the host device.
 * A single instance of this class MAY represent the audio outputs for one or
 * more video output ports depending upon the output port configuration of the
 * device. If a host device is capable of outputting multiple AV streams to
 * multiple output devices simultaneously, the device MAY have multiple
 * instances of this class, each representing an individually controllable audio
 * output.
 * <p>
 * 
 * The interpretation of audio gain measured in <i>level</i> and <i>decibel</i>
 * scales is as for <code>javax.media.GainControl</code>.
 * <p>
 * The volume controlled via an instance of <code>java.media.GainControl</code>,
 * if supported, SHALL be relative to the volume controlled by an appropriate
 * instance of <code>AudioOutputPort</code>.
 * 
 * @see GainControl
 * @see HostSettings#getAudioOutputs()
 * @see HostSettings#setSystemVolumeKeyControl(boolean)
 * @see HostSettings#setSystemMuteKeyControl(boolean)
 * @see HostSettings#setSystemVolumeRange(int)
 */
public class AudioOutputPort
{
    /**
     * Constant representing single channel (monoaural or mono) audio.
     */
    public static final int STEREO_MODE_MONO = 1;

    /**
     * Constant representing two-channel stereo audio.
     */
    public static final int STEREO_MODE_STEREO = 2;

    /**
     * Constant representing multi-channel stereo surround audio.
     */
    public static final int STEREO_MODE_SURROUND = 3;

    /**
     * Constant representing lack of digital audio output.
     */
    public static final int ENCODING_NONE = 0;

    /**
     * Constant representing a platform-selected digital audio encoding format.
     */
    public static final int ENCODING_DISPLAY = 1;

    /**
     * Constant representing pulse-code modulation (PCM) digital audio encoding.
     */
    public static final int ENCODING_PCM = 2;

    /**
     * Constant representing AC3 digital audio encoding.
     */
    public static final int ENCODING_AC3 = 3;

    /**
     * Constant representing no audio compression.
     */
    public static final int COMPRESSION_NONE = 0;

    /**
     * Constant representing light audio level compression.
     */
    public static final int COMPRESSION_LIGHT = 1;

    /**
     * Constant representing medium audio level compression.
     */
    public static final int COMPRESSION_MEDIUM = 2;

    /**
     * Constant representing heavy audio level compression.
     */
    public static final int COMPRESSION_HEAVY = 3;

    /**
     * An application cannot construct an instance of this class directly.
     */
    protected AudioOutputPort()
    {
        // to be implemented
    }

    /**
     * Get the current stereo mode of the audio device.
     * 
     * @return The current stereo mode. SHALL be one of
     *         {@link #STEREO_MODE_MONO}, {@link #STEREO_MODE_STEREO} or
     *         {@link #STEREO_MODE_SURROUND}.
     * 
     * @see #setStereoMode
     * @see #getSupportedStereoModes
     */
    public int getStereoMode()
    {
        return 0;
    }

    /**
     * Set the stereo mode of the audio device.
     * 
     * @param mode
     *            The desired stereo mode.
     * 
     * @throws IllegalArgumentException
     *             if <i>mode</i> is not one of {@link #STEREO_MODE_MONO},
     *             {@link #STEREO_MODE_STEREO} or {@link #STEREO_MODE_SURROUND}
     * @throws FeatureNotSupportedException
     *             if the given setting is not supported
     * 
     * @see #getStereoMode
     * @see #getSupportedStereoModes
     */
    public void setStereoMode(int mode) throws IllegalArgumentException, FeatureNotSupportedException
    {
        // to be implemented
    }

    /**
     * Get the set of stereo modes supported by this audio device. The returned
     * values SHALL NOT produce a <code>FeatureNotSupportedException</code> when
     * provided to <code>setStereoMode(int)</code>.
     * 
     * @return a non-null array containing the set of supported compression
     *         level settings
     * 
     * @see #setStereoMode
     * @see #getStereoMode
     */
    public int[] getSupportedStereoModes()
    {
        return null;
    }

    /**
     * Get the current audio level compression of the audio device.
     * 
     * @return The current compression level. SHALL be one of
     *         {@link #COMPRESSION_NONE}, {@link #COMPRESSION_LIGHT},
     *         {@link #COMPRESSION_MEDIUM} or {@link #COMPRESSION_HEAVY}
     * 
     * @see #setCompression
     * @see #getSupportedCompressions
     */
    public int getCompression()
    {
        return 0;
    }

    /**
     * Set the compression level of the audio device. Compression reduces the
     * dynamic range of an audio signal by reducing the gain when the signal
     * level is higher than a given threshold.
     * <p>
     * The compression levels correspond to implementation-specific ratios of
     * input level to output gain. The following table describes the compression
     * levels.
     * <table border>
     * <tr>
     * <th>Level</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td><code>COMPRESSION_NONE</code></td>
     * <td>Always a 1:1 ratio of input level to output gain. No compression is
     * applied to the output signal</td>
     * </tr>
     * <tr>
     * <td><code>COMPRESSION_LIGHT</code></td>
     * <td>The lightest level of compression. Example ratio would be 2:1, where
     * a 2dB change in input level is required to effect a 1dB change in output,
     * above the threshold.</td>
     * </tr>
     * <tr>
     * <td><code>COMPRESSION_MEDIUM</code></td>
     * <td>An equal or higher level of compression than
     * <code>COMPRESSION_LIGHT</code></td>
     * </tr>
     * <tr>
     * <td><code>COMPRESSION_HEAVY</code></td>
     * <td>The highest level of compression. This may be a high enough level so
     * as to be considered <i>limiting</i>.</td>
     * </tr>
     * </table>
     * <p>
     * Other attributes of audio level compression (e.g., threshold, attack, and
     * release) are implementation-specific and not exposed by this API.
     * 
     * @param compression
     *            The desired compression level.
     * 
     * @throws IllegalArgumentException
     *             if <i>compression</i> is not one of {@link #COMPRESSION_NONE}
     *             , {@link #COMPRESSION_LIGHT}, {@link #COMPRESSION_MEDIUM} or
     *             {@link #COMPRESSION_HEAVY}
     * @throws FeatureNotSupportedException
     *             if the given setting is not supported
     * 
     * @see #getCompression
     * @see #getSupportedCompressions
     */
    public void setCompression(int compression) throws IllegalArgumentException, FeatureNotSupportedException
    {
        // to be implemented
    }

    /**
     * Get the set of compression levels supported by this audio device. The
     * returned values SHALL NOT produce a
     * <code>FeatureNotSupportedException</code> when provided to
     * <code>setCompression(int)</code>.
     * 
     * @return a non-null array containing the set of supported compression
     *         level settings
     * 
     * @see #setCompression
     * @see #getCompression
     */
    public int[] getSupportedCompressions()
    {
        return null;
    }

    /**
     * Get the current encoding format for digital audio output for this audio
     * device.
     * <p>
     * This method will never return <code>ENCODING_DISPLAY</code>. Instead, the
     * current platform-selected encoding will be returned. The platform selects
     * such an encoding based upon the device connected to this audio output
     * port.
     * 
     * @return The current encoding format. SHALL be one of
     *         {@link #ENCODING_NONE}, {@link #ENCODING_PCM} or
     *         {@link #ENCODING_AC3}
     * 
     * @see #setEncoding
     * @see #getSupportedEncodings
     */
    public int getEncoding()
    {
        return 0;
    }

    /**
     * Set the desired encoding format for digital audio output for this audio
     * device.
     * <p>
     * This method MAY be used to control the desired encoding format for
     * digital audio output for this audio device. The following table describes
     * the supported values for the <i>encoding</i> parameter:
     * <table border>
     * <tr>
     * <th>Value</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td><code>ENCODING_PCM</code></td>
     * <td>Pulse code modulation digital audio encoding.</td>
     * </tr>
     * <tr>
     * <td><code>ENCODING_AC3</code></td>
     * <td>AC-3 digital audio encoding.</td>
     * </tr>
     * <tr>
     * <td><code>ENCODING_DISPLAY</code></td>
     * <td>No explicit format is specified, instead the Host device will select
     * the preferred output format.</td>
     * </tr>
     * </table>
     * 
     * @param encoding
     *            The desired digital encoding format.
     * 
     * @throws IllegalArgumentException
     *             if <i>encoding</i> is not one of {@link #ENCODING_DISPLAY},
     *             {@link #ENCODING_PCM} or {@link #ENCODING_AC3}
     * @throws FeatureNotSupportedException
     *             if the given setting is not supported
     * 
     * @see #getEncoding
     * @see #getSupportedEncodings
     */
    public void setEncoding(int encoding) throws IllegalArgumentException, FeatureNotSupportedException
    {
        // to be implemented
    }

    /**
     * Get the set of encoding formats supported by this audio device. The
     * returned values SHALL NOT produce a
     * <code>FeatureNotSupportedException</code> when provided to
     * <code>setEncoding(int)</code>.
     * 
     * @return a non-null array containing the set of supported digital encoding
     *         formats
     * 
     * @see #setEncoding
     * @see #getEncoding
     */
    public int[] getSupportedEncodings()
    {
        return null;
    }

    /**
     * Get the current gain set for this <code>AudioOutputPort</code> in
     * decibels.
     * 
     * @return The gain in dB.
     * 
     * @see #setDB
     */
    public float getDB()
    {
        return 0F;
    }

    /**
     * Set the gain in decibels for this <code>AudioOutputPort</code>. Setting
     * the gain to 0.0 (the default) implies that the audio signal is neither
     * amplified nor attenuated. Positive values amplify the audio signal and
     * negative values attenuate the signal.
     * 
     * @param db
     *            The new gain in dB.
     * @return The gain that was actually set.
     * 
     * @see GainControl
     * @see #getDB
     * @see #getMaxDB
     * @see #getMinDB
     * @see #setLevel
     */
    public float setDB(float db)
    {
        return 0F;
    }

    /**
     * Get the maximum gain in decibels for this <code>AudioOutputPort</code>.
     * Calling {@link #setDB} with values greater than those returned by this
     * API will have no effect.
     * 
     * @return The maximum gain in decibels.
     */
    public float getMaxDB()
    {
        return 0F;
    }

    /**
     * Get the minimum gain in decibels for this <code>AudioOutputPort</code>.
     * Calling {@link #setDB} with values less than those returned by this API
     * will have no effect.
     * 
     * @return The minimum gain in decibels.
     */
    public float getMinDB()
    {
        return 0F;
    }

    /**
     * Get the current gain set for this <code>AudioOutputPort</code> as a value
     * between 0.0 and 1.0.
     * 
     * @return The gain in the level scale (0.0-1.0).
     * @see #setLevel
     */
    public float getLevel()
    {
        return 0F;
    }

    /**
     * Set the gain using a floating point scale with values between 0.0 and
     * 1.0. 0.0 is silence; 1.0 is the loudest useful level that this
     * <code>AudioOutputPort</code> supports.
     * 
     * @param level
     *            The new gain value specified in the level scale.
     * @return The level that was actually set.
     * 
     * @see GainControl
     * @see #getLevel
     * @see #getDB
     * @see #setDB
     */
    public float setLevel(float level)
    {
        return 0F;
    }

    /**
     * Get the gain level that is optimal for stereo playback. Selection of this
     * gain level (using {@link #setLevel}) will reduce audio distortion on
     * televisions that contain stereo decoders.
     * <p>
     * Where a fixed volume level is desired, it is recommended that the
     * <i>optimal</i> level returned by this method be used.
     * 
     * @return The optimal volume level.
     * @see #setLevel
     */
    public float getOptimalLevel()
    {
        return 0F;
    }

    /**
     * Get the mute state of the audio signal associated with this host.
     * 
     * @return The current mute state: <code>true</code> if muted and
     *         <code>false</code> otherwise.
     * @see #setMuted
     */
    public boolean isMuted()
    {
        return true;
    }

    /**
     * Mute or unmute the signal associated with this
     * <code>AudioOutputPort</code>. Redundant invocations of this method are
     * ignored. The mute state does not effect the gain (as represented by
     * {@link #getLevel()} or {@link #getDB()}.
     * 
     * @param mute
     *            The new mute state: <code>true</code> mutes the signal and
     *            <code>false</code> unmutes the signal.
     * 
     * @see #isMuted()
     */
    public void setMuted(boolean mute)
    {
        // to be implemented
    }

    /**
     * Get the current loop-thru setting of the audio device.
     * 
     * @return The currentloop-thru state: <code>true</code> if loop-thru is
     *         enabled and <code>false</code> otherwise.
     * @see #setLoopThru
     */
    public boolean isLoopThru()
    {
        return false;
    }

    /**
     * Set the loop-thru setting for the audio device.
     * <p>
     * Audio loop-thru refers to a mechanism that allows audio from other
     * devices (e.g., DVD player or VCR) to connect to a home theater or TV
     * through this Host device when this <code>AudioOutputPort</code> is
     * otherwise not in use.
     * 
     * When loop-thru is enabled, audio inputs associated with this
     * <code>AudioOutputPort</code> will be routed through this
     * <code>AudioOutputPort</code>.
     * 
     * @param loopthru
     *            The new loop-thru state: (<code>true</code> enables loop-thru
     *            and <code>false</code> disables loop-thru.
     * 
     * @throws FeatureNotSupportedException
     *             if the requested loop-thru setting can not be achieved by the
     *             device
     * 
     * @see #isLoopThru
     */
    public void setLoopThru(boolean loopthru) throws FeatureNotSupportedException
    {
        // to be implemented
    }

    /**
     * Get the set of <code>VideoOutputPort</code>s whose audio is controlled by
     * this <code>AudioOutputPort</code> instance.
     * 
     * @return The set of controlled <code>VideoOutputPort</code>s as an
     *         <code>Enumeration</code>.
     */
    public java.util.Enumeration getConnectedVideoOutputPorts()
    {
        return null;
    }
}
