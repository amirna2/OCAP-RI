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

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * SoundAPIImpl
 * 
 * @author Joshua Keplinger
 * 
 */
public class SoundAPIImpl implements SoundAPI
{
    private static final Logger log = Logger.getLogger(SoundAPIImpl.class);

    // Initialize class IDs used by JNI layer.
    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();

        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);
    }

    public SoundAPIImpl(EDListener listener)
    {
        int err = nativeInit(listener);
        if (err != 0) throwMPEMediaError(err, "init");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.mpe.SoundAPI#getSoundDeviceCount()
     */
    public int getSoundDeviceCount()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getSoundDeviceCount()");
        }
        int[] count = new int[1];
        int err = nativeGetSoundDeviceCount(count);
        if (err != 0)
            throwMPEMediaError(err, "getSoundDeviceCount");
        return count[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.mpe.SoundAPI#getSoundDevices()
     */
    public void getSoundDevices(int[] devices)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getSoundDevices(): " + "devices=" + devices);
        }
        int err = nativeGetSoundDevices(devices);
        if (err != 0)
            throwMPEMediaError(err, "getSoundDevices");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.mpe.SoundAPI#getSoundDevices(int)
     */
    public int getSoundDevices(int sound, int[] devices)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getSoundDevices(): " + "devices=" + devices + ", sound=" + sound);
        }

        int[] count = new int[1];
        int err = nativeGetSoundDevices(sound, devices, count);
        if (err != 0)
            throwMPEMediaError(err, "getSoundDevices");
        return count[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.mpe.SoundAPI#createSound(java.lang.String,
     * byte[], int, int)
     */
    public int createSound(String mimeType, byte[] data, int offset, int size)
    {
        if (log.isDebugEnabled())
        {
            log.debug("createSound(): " + "mimeType=" + mimeType + ", data=" + data + ", offset=" + offset + ", size="
                    + size);
        }
        int[] sound = new int[1];
        int err = nativeCreateSound(mimeType, data, offset, size, sound);
        if (err != 0)
            throwMPEMediaError(err, "createSound");
        return sound[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.mpe.SoundAPI#destroySound(int)
     */
    public void destroySound(int sound)
    {
        if (log.isDebugEnabled())
        {
            log.debug("destroySound(): " + "sound=" + sound);
        }
        int err = nativeDestroySound(sound);
        if (err != 0)
            throwMPEMediaError(err, "destroySound");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.mpe.SoundAPI#playSound(int, int,
     * org.cablelabs.impl.manager.ed.EDListener, javax.media.Time, boolean)
     */
    public int playSound(int device, int sound, Time start, boolean loop, boolean mute, float[] gain)
    {
        if (log.isDebugEnabled())
        {
            log.debug("playSound(): " + "device=" + device + ", sound=" + sound + ", start=" + start.getNanoseconds()
                    + ", loop=" + loop);
        }
        int[] playback = new int[1];
        int err = nativePlaySound(device, sound, start.getNanoseconds(), loop, mute, gain, playback);
        if (err != 0)
            throwMPEMediaError(err, "playSound");
        return playback[0];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.mpe.SoundAPI#stopSoundPlayback(int)
     */
    public Time stopSoundPlayback(int playback)
    {
        if (log.isDebugEnabled())
        {
            log.debug("stopSoundPlayback(): " + "playback=" + playback);
        }
        long[] stoptime = new long[1];
        int err = nativeStopSoundPlayback(playback, stoptime);
        if (err != 0)
            throwMPEMediaError(err, "stopSoundPlayback");
        return new Time(stoptime[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.mpe.SoundAPI#getSoundPlaybackTime(int)
     */
    public Time getSoundPlaybackTime(int playback)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getSoundPlaybackTime(): " + "playback=" + playback);
        }
        long[] time = new long[1];
        int err = nativeGetSoundPlaybackTime(playback, time);
        if (err != 0)
            throwMPEMediaError(err, "getSoundPlaybackTime");
        return new Time(time[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.mpe.SoundAPI#setSoundPlaybackTime(int,
     * javax.media.Time)
     */
    public Time setSoundPlaybackTime(int playback, Time time)
    {
        if (log.isDebugEnabled())
        {
            log.debug("setSoundPlaybackTime(): " + "playback=" + playback + "time=" + time.getNanoseconds());
        }
        long[] newTime = new long[] { time.getNanoseconds() };
        int err = nativeSetSoundPlaybackTime(playback, newTime);
        if (err != 0)
            throwMPEMediaError(err, "setSoundPlaybackTime");
        return new Time(newTime[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.mpe.SoundAPI#getDeviceMaxPlaybacks(int)
     */
    public int getDeviceMaxPlaybacks(int device)
    {
        int[] maxPB = new int[1];
        int err = nativeGetDeviceMaxPlaybacks(device, maxPB);
        if (err != 0) throwMPEMediaError(err, "getDeviceMaxPlaybacks");
        return maxPB[0];
    }

    public void setMute(int playback, boolean mute)
    {
        int err = jniSetMute(playback, mute);
        if (err != 0)
        {
            throwMPEMediaError(err, "setMute: " + mute);
        }
    }

    public float setGain(int playback, float gain)
    {
        float[] arg = new float[]{gain};
        int err = jniSetGain(playback, arg);
        if (err != 0)
        {
            throwMPEMediaError(err, "setGain: " + gain);
        }
        return arg[0];
    }

    // Helper methods

    protected static void throwMPEMediaError(int err, String info)
    {
        MPEMediaError x = new MPEMediaError(err, info);
            x.printStackTrace(new PrintWriter(new StringWriter()));
            SystemEventUtil.logRecoverableError(new Exception("MediaAPIImpl - " + x.toString()));
        throw x;
    }

    // Thin 'native' layer that can be overridden by a subclass

    protected int nativeInit(EDListener listener)
    {
        return jniInit(listener);
    }

    protected int nativeGetSoundDeviceCount(int[] count)
    {
        return jniGetSoundDeviceCount(count);
    }

    protected int nativeGetSoundDevices(int[] devices)
    {
        return jniGetSoundDevices(devices);
    }

    protected int nativeGetSoundDevices(int sound, int[] devices, int[] count)
    {
        return jniGetSoundDevices(sound, devices, count);
    }

    protected int nativeCreateSound(String mimeType, byte[] data, int offset, int size, int[] sound)
    {
        return jniCreateSound(mimeType, data, offset, size, sound);
    }

    protected int nativeDestroySound(int sound)
    {
        return jniDestroySound(sound);
    }

    protected int nativePlaySound(int device, int sound, long start, boolean loop, boolean mute, float[] gain, int[] playback)
    {
        return jniPlaySound(device, sound, start, loop, mute, gain, playback);
    }

    protected int nativeStopSoundPlayback(int playback, long[] stoptime)
    {
        return jniStopSoundPlayback(playback, stoptime);
    }

    protected int nativeGetSoundPlaybackTime(int playback, long[] time)
    {
        return jniGetSoundPlaybackTime(playback, time);
    }

    protected int nativeSetSoundPlaybackTime(int playback, long[] time)
    {
        return jniSetSoundPlaybackTime(playback, time);
    }

    protected int nativeGetDeviceMaxPlaybacks(int device, int[] maxPB)
    {
        return jniGetDeviceMaxPlaybacks(device, maxPB);
    }

    // Real native functions

    private static native int jniInit(EDListener listener);

    private static native int jniGetSoundDeviceCount(int[] count);

    private static native int jniGetSoundDevices(int[] devices);

    private static native int jniGetSoundDevices(int sound, int[] devices, int[] count);

    private static native int jniCreateSound(String mimeType, byte[] data, int offset, int size, int[] sound);

    private static native int jniDestroySound(int sound);

    private static native int jniPlaySound(int device, int sound, long start, boolean loop, boolean mute, float[] gain, int[] playback);

    private static native int jniStopSoundPlayback(int playback, long[] stoptime);

    private static native int jniGetSoundPlaybackTime(int playback, long[] time);

    private static native int jniSetSoundPlaybackTime(int playback, long[] time);

    private static native int jniGetDeviceMaxPlaybacks(int device, int[] maxPB);

    private static native int jniSetMute(int playback, boolean mute);

    private static native int jniSetGain(int playback, float[] arg);
}
