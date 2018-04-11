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
import org.cablelabs.impl.sound.Playback;
import org.cablelabs.impl.sound.PlaybackOwner;
import org.cablelabs.impl.sound.Sound;
import org.cablelabs.impl.util.NativeHandle;

/**
 * PlaybackImpl
 * 
 * @author Joshua Keplinger
 * 
 */
public class PlaybackImpl implements Playback, NativeHandle, Comparable
{

    private int handle;

    private SoundMgrImpl sndmgr;

    private PlaybackOwner owner;

    private Sound sound;

    private boolean playing;

    private SoundDeviceImpl device;

    private int priority;

    private float initialGain;

    private Object lock = new Object();

    /**
     * 
     */
    public PlaybackImpl(int sndHandle, SoundMgrImpl sndMgr, PlaybackOwner pbOwner, Sound snd, SoundDeviceImpl sndDev,
            float initialGain, int playbackPriority)
    {
        this.handle = sndHandle;
        this.sndmgr = sndMgr;
        this.owner = pbOwner;
        this.sound = snd;
        this.device = sndDev;
        playing = true;
        this.priority = playbackPriority;
        this.initialGain = initialGain;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.Playback#stop()
     */
    public void stop()
    {
        synchronized (lock)
        {
            if (playing)
            {
                try
                {
                    sndmgr.getSoundAPI().stopSoundPlayback(handle);
                }
                catch (MPEMediaError err)
                {
                    // We can't really do anything here
                }
                cleanup();
            }
        }
    }

    public void cleanup()
    {
        synchronized (sndmgr.getLock())
        {
            sndmgr.removePlayback(this);
        }
        owner = null;
        sound = null;
        device = null;
        playing = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.Playback#getTime()
     */
    public Time getTime()
    {
        synchronized (lock)
        {
            try
            {
                if (playing)
                    return sndmgr.getSoundAPI().getSoundPlaybackTime(handle);
                else
                    return null;
            }
            catch (MPEMediaError err)
            {
                return null;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.Playback#setTime(javax.media.Time)
     */
    public Time setTime(Time time)
    {
        synchronized (lock)
        {
            try
            {
                if (playing)
                    return sndmgr.getSoundAPI().setSoundPlaybackTime(handle, time);
                else
                    return null;
            }
            catch (MPEMediaError err)
            {
                return null;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.Playback#getOwner()
     */
    public PlaybackOwner getOwner()
    {
        synchronized (lock)
        {
            return owner;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.Playback#getSound()
     */
    public Sound getSound()
    {
        synchronized (lock)
        {
            return sound;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.util.NativeHandle#getHandle()
     */
    public int getHandle()
    {
        return handle;
    }

    public SoundDeviceImpl getDevice()
    {
        synchronized (lock)
        {
            return device;
        }
    }

    protected void setHandle(int sndHandle)
    {
        this.handle = sndHandle;
    }

    protected int getPriority()
    {
        return priority;
    }

    public void finalize()
    {
        stop();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object rhs)
    {
        return priority - ((PlaybackImpl) rhs).priority;
    }

    public void setMute(boolean mute)
    {
        sndmgr.setMute(handle, mute);
    }

    public float setGain(float gain)
    {
        return sndmgr.setGain(handle, gain);
    }
}
