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

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.sound.Playback;
import org.cablelabs.impl.sound.PlaybackOwner;
import org.cablelabs.impl.sound.Sound;
import org.cablelabs.impl.util.NativeHandle;

/**
 * SoundImpl
 * 
 * @author Joshua Keplinger
 * 
 */
public class SoundImpl implements Sound, NativeHandle
{

    private int handle;

    private SoundMgrImpl sndmgr;

    /**
     * 
     */
    SoundImpl(int sndHandle, SoundMgrImpl sndMgr)
    {
        this.handle = sndHandle;
        this.sndmgr = sndMgr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.sound.Sound#dispose()
     */
    public void dispose()
    {
        if (handle != 0)
        {
            sndmgr.getSoundAPI().destroySound(handle);
            handle = 0;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.cablelabs.impl.sound.Sound#play(org.cablelabs.impl.sound.PlaybackOwner
     * , javax.media.Time, boolean)
     */
    public Playback play(PlaybackOwner owner, Time start, boolean loop, CallerContext cc, boolean mute, float gain)
    {
        PlaybackImpl playback;

        // Synchronize this operation to make sure another play request doesn't
        // interfere with this one
        synchronized (sndmgr.getLock())
        {
            Integer pri = (Integer) cc.get(CallerContext.APP_PRIORITY);
            int priority = 255;
            if (pri != null) priority = pri.intValue();
            // Try to get a device that supports this sound
            SoundDeviceImpl device = sndmgr.getDevice(this, priority);
            if (device == null) return null;

            // We have a device, now we'll send the request to play this sound
            playback = sndmgr.playSound(device, this, owner, start, loop, mute, gain, priority);
        }
        return playback;
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

    public void finalize()
    {
        dispose();
    }

}
