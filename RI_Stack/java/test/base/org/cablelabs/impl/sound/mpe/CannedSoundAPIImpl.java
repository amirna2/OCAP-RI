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

import org.cablelabs.impl.manager.ed.EDListener;

public class CannedSoundAPIImpl extends SoundAPIImpl
{
    int nativeCreateSoundReturnValue;

    int nativeCreateSoundResult;

    int nativeDestroySoundReturnValue;

    int nativeInitReturnValue;

    int nativePlaySoundReturnValue;

    int nativePlaySoundResult;

    int nativeGetDeviceMaxPlaybacksReturnValue;

    int nativeGetDeviceMaxPlaybacksResult;

    int nativeGetSoundDeviceCountReturnValue;

    int nativeGetSoundDeviceCountResult;

    int nativeGetSoundDevicesReturnValue;

    int[] nativeGetSoundDevicesResult = new int[0];

    int nativeGetSoundPlaybackTimeReturnValue;

    long nativeGetSoundPlaybackTimeResult;

    int nativeSetSoundPlaybackTimeReturnValue;

    long nativeSetSoundPlaybackTimeResult;

    int nativeStopSoundPlaybackReturnValue;

    long nativeStopSoundPlaybackResult;

    public CannedSoundAPIImpl(EDListener listener)
    {
        super(listener);
    }

    protected int nativeCreateSound(String mimeType, byte[] data, int offset, int size, int[] sound)
    {
        sound[0] = nativeCreateSoundResult;
        return nativeCreateSoundReturnValue;
    }

    protected int nativeDestroySound(int sound)
    {
        return nativeDestroySoundReturnValue;
    }

    protected int nativeGetDeviceMaxPlaybacks(int device, int[] maxPB)
    {
        maxPB[0] = nativeGetDeviceMaxPlaybacksResult;
        return nativeGetDeviceMaxPlaybacksReturnValue;
    }

    protected int nativeGetSoundDeviceCount(int[] count)
    {
        count[0] = nativeGetSoundDeviceCountResult;
        return nativeGetSoundDeviceCountReturnValue;
    }

    protected int nativeGetSoundDevices(int sound, int[] devices, int[] count)
    {
        int size = Math.max(devices.length, nativeGetSoundDevicesResult.length);
        System.arraycopy(nativeGetSoundDevicesResult, 0, devices, 0, size);
        count[0] = size;
        return nativeGetSoundDevicesReturnValue;
    }

    protected int nativeGetSoundDevices(int[] devices)
    {
        int size = Math.max(devices.length, nativeGetSoundDevicesResult.length);
        System.arraycopy(nativeGetSoundDevicesResult, 0, devices, 0, size);
        return nativeGetSoundDevicesReturnValue;
    }

    protected int nativeGetSoundPlaybackTime(int playback, long[] time)
    {
        time[0] = nativeGetSoundPlaybackTimeResult;
        return nativeGetSoundPlaybackTimeReturnValue;
    }

    protected int nativeInit(EDListener listener)
    {
        return nativeInitReturnValue;
    }

    protected int nativePlaySound(int device, int sound, long start, boolean loop, int[] playback)
    {
        playback[0] = nativePlaySoundResult;
        return nativePlaySoundReturnValue;
    }

    protected int nativeSetSoundPlaybackTime(int playback, long[] time)
    {
        time[0] = nativeSetSoundPlaybackTimeResult;
        return nativeSetSoundPlaybackTimeReturnValue;
    }

    protected int nativeStopSoundPlayback(int playback, long[] stoptime)
    {
        stoptime[0] = nativeStopSoundPlaybackResult;
        return nativeStopSoundPlaybackReturnValue;
    }

}
