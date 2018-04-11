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
package org.cablelabs.impl.sound.mpe;

import javax.media.GainControl;
import javax.media.Time;

/**
 * SoundAPI This interface exposes MPE Sound API functions.
 * 
 * @author Joshua Keplinger
 * 
 */
public interface SoundAPI
{
    public static final int MPE_SND_EVENT_COMPLETE = 0x2000;

    public static final int MPE_SND_EVENT_ERROR = 0x2001;

    /**
     * Returns the total number of SoundDevices on this system
     * 
     * @return the total number of SoundDevices on this system
     */
    public int getSoundDeviceCount();

    /**
     * Returns an array handles of all SoundDevices supported by MPE.
     * 
     * @return an array handles of all SoundDevices supported by MPE.
     */
    public void getSoundDevices(int[] devices);

    /**
     * Returns an array of MPE SoundDevice handles for devices that can support
     * playback of the specified MPE Sound. The returned devices are sorted in
     * order of preference. Preference is determined by the platform.
     * 
     * @param sound
     *            the handle to the Sound we wish to play
     * @return an array of MPE SoundDevice handles
     */
    public int getSoundDevices(int sound, int[] devices);

    /**
     * Creates a native resource for the Sound and returns a handle to that
     * newly constructed native sound.
     * 
     * @param mimeType
     *            the mimeType of the Sound
     * @param data
     *            the array of data that is the sound itself
     * @param offset
     *            the offset of the array from the beginning of the array
     * @param size
     *            the length in bytes of this sound
     * @return a handle to the native sound resource
     */
    public int createSound(String mimeType, byte[] data, int offset, int size);

    /**
     * Destroy an MPE Sound resource created by a prior call to createSound().
     * 
     * @param sound
     *            the sound resource to be released.
     */
    public void destroySound(int sound);

    /**
     * Start playing an MPE Sound on an MPE SoundDevice. If successful, return
     * an MPE Playback handle that can be used to control the playback. Playback
     * begins at the specified start time. It can also be designated to loop
     * indefinitely. Asynchronous events occurring from playback will be
     * delivered to the specified EDListener.
     * 
     * @param device
     *            the handle to the device on which to play the sound
     * @param sound
     *            the handle to the sound to be played
     * @param start
     *            the start time
     * @param loop
     *            true/false to loop the sound
     * @param mute
     *            true if muted
     * @param gain
     *            requested gain level (index zero is updated with actual gain)
     * @return a handle to the sound playback
     */
    public int playSound(int device, int sound, Time start, boolean loop, boolean mute, float[] gain);

    /**
     * Synchronously stops the playback of the specified MPE Playback handle. If
     * playback has already stopped, this method has no effect (is harmless).
     * The underlying implementation will release any resources that were held
     * by the playback.
     * 
     * @param playback
     *            the handle to the sound playback
     */
    public Time stopSoundPlayback(int playback);

    /**
     * Returns the current media time of an active MPE Playback. If playback has
     * expired, this returns null.
     * 
     * @param playback
     *            the handle to the playback
     * @return the current time of the playback or null if not currently playing
     */
    public Time getSoundPlaybackTime(int playback);

    /**
     * Sets the current media time of an active MPE Playback and returns the
     * actual media time assigned. If playback has expired, it returns null.
     * 
     * @param playback
     *            the handle to the playback
     * @param time
     *            the new time to set
     * @return the current time of the playback or null if not currently playing
     */
    public Time setSoundPlaybackTime(int playback, Time time);

    /**
     * Gets the maximum number of playbacks this device can handle
     * 
     * @param device
     *            the device in question
     * @return the maximum number of playbacks
     */
    public int getDeviceMaxPlaybacks(int device);

    /**
     * Mute or unmute the audio of a decode session
     *
     * @param sessionHandle the session
     * @param mute new mute state
     */
    void setMute(int sessionHandle, boolean mute);

    /**
     * Update the audio gain value as described by {@link GainControl#setDB}
     *
     * @param sessionHandle the session
     * @param gain new audio gain value in decibels
     */
    float setGain(int sessionHandle, float gain);
}
