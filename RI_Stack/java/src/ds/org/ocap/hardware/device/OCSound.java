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

import java.util.Enumeration;
import java.util.Vector;

import org.havi.ui.HSound;

import org.cablelabs.impl.ocap.hardware.device.DeviceSettingsVideoOutputPortImpl;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * Extends the HAVi <code>HSound</code> class, adding additional configuration
 * options. Instances of this class provide control over audio gain level,
 * muting, and output ports.
 * 
 * @see AudioOutputPort
 * 
 * @author Aaron Kamienski
 * @author Alan Cossitt (implementation)
 */
public class OCSound extends HSound
{
    private final Vector audioPorts = new Vector();

    private float gain = 1.0F;

    private boolean muted = false;

    /**
     * Creates an <code>OCSound</code> object. The following defaults apply upon
     * construction:
     * <table border>
     * <tr>
     * <th>Attribute</th>
     * <th>Method</th>
     * <th>Default</th>
     * </tr>
     * <tr>
     * <td>Level</td>
     * <td>{@link #getLevel()}</td>
     * <td><code>1.0</code></td>
     * </tr>
     * <tr>
     * <td>Mute</td>
     * <td>{@link #isMuted()}</td>
     * <td><code>false</code></td>
     * </tr>
     * <tr>
     * <td>Outputs</td>
     * <td>{@link #getAudioOutputs}</td>
     * <td>the default audio output for the application constructing the
     * <code>OCSound</code> instance</td>
     * </table>
     * 
     */
    public OCSound()
    {
        /*
         * per spec, the list of audio ports should have the
         * "application default audio port" According to Aaron Kamienski, how
         * this is defined is implementation dependent.
         * 
         * TODO_DS: Should this code be common and/or the default audio port be
         * stored in the host persistence layer
         */
        Enumeration audioPortsE = DeviceSettingsVideoOutputPortImpl.getAudioOutputPorts();
        if (audioPortsE.hasMoreElements())
        {
            audioPorts.add(audioPortsE.nextElement());
        }
        else
        {
            SystemEventUtil.logRecoverableError(
                    "There should be at least one audio port available but none were found",
                    new IllegalArgumentException("no audio ports available"));
        }

    }

    /**
     * Set the gain using a floating point scale with values between 0.0 and
     * 1.0. 0.0 is silence; 1.0 is the loudest level for associated audio output
     * ports.
     * 
     * @param level
     *            The new gain value specified in the level scale.
     * @return The level that was actually set.
     * 
     * @see #getLevel
     * @see AudioOutputPort#setLevel
     * @see AudioOutputPort#getLevel
     */
    public float setLevel(float level)
    {
        Enumeration audioE = audioPorts.elements();
        AudioOutputPort port = null;
        while (audioE.hasMoreElements())
        {
            port = (AudioOutputPort) audioE.nextElement();
            port.setLevel(level);
        }

        /*
         * The assumption here is that the level allowed is on a host basis and
         * thus getting the set value for any port is accurate.
         * 
         * If the level was previously set but nothing set now, use the old
         * level.
         */
        if (port != null)
        {
            this.gain = port.getLevel();
        }

        return level;
    }

    /**
     * Get the current gain set for this <code>OCSound</code> as a value between
     * 0.0 and 1.0.
     * 
     * @return The gain in the level scale (0.0-1.0).
     * @see #setLevel
     */
    public float getLevel()
    {
        return this.gain;
    }

    /**
     * Get the mute state of the audio signal associated with this audio clip.
     * 
     * @return The current mute state: <code>true</code> if muted and
     *         <code>false</code> otherwise.
     * @see #setMuted
     */
    public boolean isMuted()
    {
        return this.muted;
    }

    /**
     * Mute or unmute the signal associated with this <code>OCSound</code>.
     * Redundant invocations of this method are ignored. The mute state does not
     * effect the gain (as represented by {@link #getLevel()}.
     * 
     * @param mute
     *            The new mute state: <code>true</code> mutes the signal and
     *            <code>false</code> unmutes the signal.
     * 
     * @see #isMuted()
     */
    public void setMuted(boolean mute)
    {
        Enumeration audioE = audioPorts.elements();
        AudioOutputPort port = null;
        while (audioE.hasMoreElements())
        {
            port = (AudioOutputPort) audioE.nextElement();
            port.setMuted(mute);
        }

        /*
         * The assumption here is muting is always allowed and thus the last one
         * set is accurate for all ports.
         * 
         * If the muting was previously set but nothing set now, use the old
         * muting.
         */
        if (port != null)
        {
            this.muted = port.isMuted();
        }
    }

    /**
     * Get the audio output ports on which this audio clip would be played. By
     * default, audio-clips will be played on the default audio output port for
     * the application that created this <code>OCSound</code>. Unless
     * <code>AudioOutputPort</code>s have been removed by calling
     * <code>removeAudioOutput</code>, this method SHALL return the at least the
     * default <code>AudioOutputPort</code> for the application. Unless
     * <code>AudioOutputPort</code>s have been added by calling
     * <code>addAudioOutput</code>, this method SHALL return at most the default
     * <code>AudioOutputPort</code> for the application.
     * 
     * @return The set of target <code>AudioOutputPort</code>s as an array. If
     *         all ports have been removed, then an empty array SHALL be
     *         returned.
     * 
     * @see #addAudioOutput
     * @see #removeAudioOutput
     */
    public AudioOutputPort[] getAudioOutputs()
    {
        return (AudioOutputPort[]) audioPorts.toArray(new AudioOutputPort[audioPorts.size()]);
    }

    /**
     * Add an <code>AudioOutputPort</code> to the set of audio output ports
     * where this clip will be played.
     * <p>
     * Redundant additions SHALL have no effect.
     * 
     * @param au
     *            The <code>AudioOutputPort</code> to add.
     */
    public void addAudioOutput(AudioOutputPort au)
    {
        if (!audioPorts.contains(au))
        {
            audioPorts.add(au);
        }
    }

    /**
     * Remove an <code>AudioOutputPort</code> from the set of audio ouput ports
     * where this clip will be played.
     * <p>
     * Attempting to remove an <code>AudioOutputPort</code> that is not
     * currently in the set of audio output ports for this <code>OCSound</code>
     * SHALL have no effect.
     * 
     * @param au
     *            The <code>AudioOutputPort</code> to remove.
     */
    public void removeAudioOutput(AudioOutputPort au)
    {
        audioPorts.remove(au);
    }
}
