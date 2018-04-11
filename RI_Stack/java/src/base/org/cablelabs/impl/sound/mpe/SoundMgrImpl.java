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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.media.Time;

import org.cablelabs.impl.manager.SoundManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.sound.PlaybackOwner;
import org.cablelabs.impl.sound.Sound;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * SoundMgrImpl
 * 
 * @author Joshua Keplinger
 * 
 */
public class SoundMgrImpl implements SoundManager, EDListener
{

    private SoundAPI api;

    private Object lock = new Object();

    private int devCount;

    private SoundDeviceImpl[] devices;

    private Map playbacks;

    private static SoundMgrImpl mgr;

    /**
     * 
     */
    public SoundMgrImpl()
    {
        try
        {
            // Get the number of devices and create an array of SoundDeviceImpls
            api = getSoundAPI();
            devCount = api.getSoundDeviceCount();
            devices = new SoundDeviceImpl[devCount];

            // Now we need to get the array of native handles
            int[] devHandles = new int[devCount];
            api.getSoundDevices(devHandles);

            // We have the handles, now we create the new
            for (int i = 0; i < devCount; i++)
            {
                devices[i] = new SoundDeviceImpl(devHandles[i], api.getDeviceMaxPlaybacks(devHandles[i]));
            }
        }
        catch (MPEMediaError err)
        {
            devCount = 0;
            devices = new SoundDeviceImpl[0];
        }

        playbacks = new HashMap();

    }

    public static synchronized SoundManager getInstance()
    {
        if (mgr == null) mgr = new SoundMgrImpl();
        return mgr;
    }

    public SoundDeviceImpl getDevice(SoundImpl sound, int currPriority)
    {
        int[] devHandles = new int[devCount];
        int retCount;
        try
        {
            retCount = api.getSoundDevices(sound.getHandle(), devHandles);
        }
        catch (MPEMediaError err)
        {
            SystemEventUtil.logRecoverableError("error getting supporting sound devices", err);
            return null;
        }

        // Get all the devices in a handy format
        Vector supportedDevs = new Vector();
        for (int i = 0; i < retCount; i++)
        {
            for (int j = 0; j < devices.length; j++)
            {
                if (devices[j].getHandle() == devHandles[i])
                {
                    supportedDevs.add(devices[j]);
                    break;
                }
            }// end inner for loop
        }// end outer for loop

        // Now we'll iterate through the supported devices and find one that's
        // available to use or steal
        synchronized (getLock())
        {
            // We'll go through the devices and find one that has room
            SoundDeviceImpl dev;
            for (int i = 0; i < supportedDevs.size(); i++)
            {
                dev = (SoundDeviceImpl) supportedDevs.get(i);
                int maxPB = dev.getMaxPlaybacks();
                int currPB = dev.getCurrentPlaybackCount();
                if (maxPB == -1 || maxPB > currPB) return dev;
            }

            // None of them have room, so let's start at the top again and try
            // take one.
            for (int i = 0; i < supportedDevs.size(); i++)
            {
                dev = (SoundDeviceImpl) supportedDevs.get(i);
                List pbList = dev.getAllPlaybacks();
                Collections.sort(pbList);
                for (int j = 0; j < pbList.size(); j++)
                {
                    PlaybackImpl pb = (PlaybackImpl) pbList.get(j);
                    if (currPriority > pb.getPriority())
                    {
                        pb.getOwner().playbackStopped(PlaybackOwner.PREEMPTED);
                        pb.stop();
                        return dev;
                    }
                }// end inner for loop
            }// end outer for loop (resource contention loop)
        }// end synch block

        // If it got here, no device was available
        return null;
    }

    public PlaybackImpl playSound(SoundDeviceImpl device, SoundImpl sound, PlaybackOwner owner, Time start,
            boolean loop, boolean mute, float gain, int priority)
    {
        float[] gainArray = new float[]{gain};
        int pbHandle = api.playSound(device.getHandle(), sound.getHandle(), start, loop, mute, gainArray);

        PlaybackImpl playback = new PlaybackImpl(pbHandle, this, owner, sound, device, gainArray[0], priority);

        playbacks.put(new Integer(playback.getHandle()), playback);
        device.addPlayback(playback);

        return playback;
    }

    public void removePlayback(PlaybackImpl playback)
    {
        playbacks.remove(new Integer(playback.getHandle()));
        playback.getDevice().removePlayback(playback);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.manager.SoundManager#createSound(java.lang.String,
     * byte[], int, int)
     */
    public Sound createSound(String mimeType, byte[] data, int offset, int size)
    {
        try
        {
            int sndHandle = api.createSound(mimeType, data, offset, size);
            return new SoundImpl(sndHandle, this);
        }
        catch (Error err)
        {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.Manager#destroy()
     */
    public void destroy()
    {
        api = null;
        devCount = 0;
        lock = null;
        devices = null;
    }

    public void setMute(int sessionHandle, boolean mute)
    {
        api.setMute(sessionHandle, mute);
    }

    public float setGain(int sessionHandle, float gain)
    {
        return api.setGain(sessionHandle, gain);
    }

    Object getLock()
    {
        return lock;
    }

    protected SoundAPI getSoundAPI()
    {
        if (api == null) api = new SoundAPIImpl(this);
        return api;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.manager.ed.EDListener#asyncEvent(int, int, int)
     */
    public void asyncEvent(int eventCode, int eventData1, int eventData2)
    {
        PlaybackImpl playback = (PlaybackImpl) playbacks.get(new Integer(eventData1));
        if (playback != null)
        {
            switch (eventCode)
            {
                case SoundAPI.MPE_SND_EVENT_COMPLETE:
                    playback.getOwner().playbackStopped(PlaybackOwner.END_OF_CONTENT);
                    break;
                case SoundAPI.MPE_SND_EVENT_ERROR:
                    playback.getOwner().playbackStopped(PlaybackOwner.DECODE_ERROR);
                    break;
            }
        }
    }

}
