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

import javax.media.Time;

import org.cablelabs.impl.media.mpe.MPEMediaError;

public class CannedSoundAPI implements SoundAPI
{
    private int destroyedHandle;

    private int playSoundReturnValue;

    private int createSoundReturnValue;

    private boolean playSoundThrowsException;

    private boolean getSoundDeviceThrowsException;

    private boolean createSoundThrowsException;

    private boolean stopSoundThrowsException;

    private boolean stopSoundPlaybackCalled;

    private int deviceMaxPlaybacks;

    int playSoundSound;

    int playSoundDevice;

    int[] devices = new int[0];

    int[] supportedDevices = new int[0];

    private Time time;

    private int expectedTimeHandle;

    public CannedSoundAPI()
    {

    }

    public int getSoundDeviceCount()
    {
        return devices.length;
    }

    public void getSoundDevices(int[] devices)
    {
        if (getSoundDeviceThrowsException)
        {
            throw new MPEMediaError(0);
        }
        System.arraycopy(this.devices, 0, devices, 0, this.devices.length);
    }

    public int getSoundDevices(int sound, int[] devices)
    {
        if (getSoundDeviceThrowsException)
        {
            throw new MPEMediaError(0);
        }
        System.arraycopy(this.supportedDevices, 0, devices, 0, this.supportedDevices.length);
        return this.supportedDevices.length;
    }

    public void cannedSetDevices(int[] d)
    {
        devices = d;
    }

    public void cannedSetSupportedDevices(int[] d)
    {
        supportedDevices = d;
    }

    public int createSound(String mimeType, byte[] data, int offset, int size)
    {
        if (createSoundThrowsException)
        {
            throw new MPEMediaError(0);
        }
        return createSoundReturnValue;
    }

    public void destroySound(int sound)
    {
        destroyedHandle = sound;
    }

    public int cannedGetDestroyedSoundHandle()
    {
        return destroyedHandle;
    }

    public int playSound(int device, int sound, Time start, boolean loop, boolean mute, float[] gain)
    {
        if (playSoundThrowsException)
        {
            throw new MPEMediaError(0);
        }
        playSoundDevice = device;
        playSoundSound = sound;
        return playSoundReturnValue;
    }

    public void cannedSetPlaySoundThrowsException(boolean b)
    {
        playSoundThrowsException = b;
    }

    public void cannedSetPlaySoundReturnValue(int i)
    {
        playSoundReturnValue = i;
    }

    public int cannedGetPlaySoundDevice()
    {
        return playSoundDevice;
    }

    public int cannedGetPlaySoundSound()
    {
        return playSoundSound;
    }

    public Time stopSoundPlayback(int playback)
    {
        stopSoundPlaybackCalled = true;
        if (stopSoundThrowsException)
        {
            throw new MPEMediaError(0);
        }
        return null;
    }

    public boolean cannedGetStopSoundPlaybackCalled()
    {
        return stopSoundPlaybackCalled;
    }

    public void cannedSetStopSoundThrowsException(boolean b)
    {
        stopSoundThrowsException = b;
    }

    public Time getSoundPlaybackTime(int playback)
    {
        if (playback != expectedTimeHandle)
        {
            throw new MPEMediaError(0);
        }
        return time;
    }

    public Time setSoundPlaybackTime(int playback, Time t)
    {
        if (playback != expectedTimeHandle)
        {
            throw new MPEMediaError(0);
        }
        time = t;
        return time;
    }

    public int getDeviceMaxPlaybacks(int device)
    {
        return deviceMaxPlaybacks;
    }

    public void setMute(int sessionHandle, boolean mute)
    {
        //no-op
    }

    public float setGain(int sessionHandle, float gain)
    {
        return 0.0F;
    }

    public void cannedSetDeviceMaxPlaybacks(int i)
    {
        deviceMaxPlaybacks = i;
    }

    public void cannedSetCreateSoundReturnValue(int i)
    {
        createSoundReturnValue = i;
    }

    public void cannedSetGetSoundDevicesThrowsException(boolean b)
    {
        getSoundDeviceThrowsException = b;
    }

    public void cannedSetCreateSoundThrowsException(boolean b)
    {
        createSoundThrowsException = b;
    }

    public void cannedSetTime(Time t)
    {
        time = t;
    }

    public Time cannedGetTime()
    {
        return time;
    }

    public void cannedSetExpectedTimeHandle(int h)
    {
        expectedTimeHandle = h;
    }
}
