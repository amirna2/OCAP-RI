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

import java.util.ArrayList;
import java.util.List;

import org.cablelabs.impl.util.NativeHandle;

/**
 * SoundDeviceImpl
 * 
 * @author Joshua Keplinger
 * 
 */
public class SoundDeviceImpl implements NativeHandle
{

    private int handle;

    private int maxPlaybacks;

    private List playbacks;

    /**
     * 
     */
    public SoundDeviceImpl(int sndDevHandle, int maxSndPlaybacks)
    {
        this.handle = sndDevHandle;
        this.maxPlaybacks = maxSndPlaybacks;
        playbacks = new ArrayList();
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

    /**
     * Returns the max number of playbacks this device is capable of maintaining
     * 
     * @return the max playbacks
     */
    public int getMaxPlaybacks()
    {
        return maxPlaybacks;
    }

    /**
     * Gets the current number of ongoing playbacks
     * 
     * @return the playback count
     */
    public int getCurrentPlaybackCount()
    {
        return playbacks.size();
    }

    /**
     * Adds the playback to its list of ongoing playbacks if the the limit has
     * not already been reached or the max playbacks is infinite (-1).
     * 
     * @param playback
     *            the playback to add
     * @return true if the playback was successfully added, false if not
     */
    public boolean addPlayback(PlaybackImpl playback)
    {
        if (maxPlaybacks == -1 || playbacks.size() < maxPlaybacks)
        {
            playbacks.add(playback);
            return true;
        }
        else
            return false;
    }

    /**
     * Removes the Playback from the list of ongoing playbacks
     * 
     * @param playback
     *            the playback to remove
     */
    public void removePlayback(PlaybackImpl playback)
    {
        playbacks.remove(playback);
    }

    /**
     * Returns the list of all ongoing playbacks
     * 
     * @return the list of all ongoing playbacks
     */
    public List getAllPlaybacks()
    {
        return playbacks;
    }

}
