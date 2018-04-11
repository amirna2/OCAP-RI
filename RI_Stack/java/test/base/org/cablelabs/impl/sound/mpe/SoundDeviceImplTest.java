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

import java.util.List;

import junit.framework.TestCase;

public class SoundDeviceImplTest extends TestCase
{

    public void testConstructor() throws Exception
    {
        int handle[] = new int[] { 0, -1, 1 };
        int maxPlaybacks[] = new int[] { 0, -1, 1 };

        assertTrue("Test incorrectly configured", handle.length == maxPlaybacks.length);
        for (int i = 0; i < handle.length; i++)
        {
            SoundDeviceImpl soundDevice = new SoundDeviceImpl(handle[i], maxPlaybacks[i]);
            assertTrue(soundDevice.getHandle() == handle[i]);
            assertTrue(soundDevice.getMaxPlaybacks() == maxPlaybacks[i]);
        }
    }

    public void testAddPlayback() throws Exception
    {
        SoundMgrImpl soundMgrImpl = new CannedSoundMgrImpl();
        CannedPlaybackOwner owner = new CannedPlaybackOwner();
        SoundDeviceImpl device = new SoundDeviceImpl(0, 1);
        PlaybackImpl playback = new PlaybackImpl(0, soundMgrImpl, owner, null, device, 0.0F, 0);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(0, 1);

        List playbacks = null;
        boolean added = soundDevice.addPlayback(playback);
        playbacks = soundDevice.getAllPlaybacks();
        assertTrue(added);
        assertTrue(playbacks.contains(playback));

        PlaybackImpl playback2 = new PlaybackImpl(0, soundMgrImpl, owner, null, device, 0.0F, 0);
        added = soundDevice.addPlayback(playback);
        playbacks = soundDevice.getAllPlaybacks();
        assertTrue(!added);
        assertTrue(!playbacks.contains(playback2));
    }

    public void testAddPlaybackMaxSizeZero() throws Exception
    {
        SoundMgrImpl soundMgrImpl = new CannedSoundMgrImpl();
        CannedPlaybackOwner owner = new CannedPlaybackOwner();
        SoundDeviceImpl device = new SoundDeviceImpl(0, 1);
        PlaybackImpl playback = new PlaybackImpl(0, soundMgrImpl, owner, null, device, 0.0F, 0);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(0, 0);

        List playbacks = null;
        boolean added = soundDevice.addPlayback(playback);
        playbacks = soundDevice.getAllPlaybacks();
        assertTrue(!added);
        assertTrue(!playbacks.contains(playback));
    }

    public void testRemovePlayback() throws Exception
    {
        SoundMgrImpl soundMgrImpl = new CannedSoundMgrImpl();
        CannedPlaybackOwner owner = new CannedPlaybackOwner();
        SoundDeviceImpl device = new SoundDeviceImpl(0, 1);
        PlaybackImpl playback = new PlaybackImpl(0, soundMgrImpl, owner, null, device, 0.0F, 0);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(0, 1);

        List playbacks = null;
        boolean added = soundDevice.addPlayback(playback);
        playbacks = soundDevice.getAllPlaybacks();
        assertTrue(added);
        assertTrue(playbacks.contains(playback));

        soundDevice.removePlayback(playback);
        playbacks = soundDevice.getAllPlaybacks();
        assertTrue(!playbacks.contains(playback));
    }

    public void testRemovePlaybackNotAdded() throws Exception
    {
        SoundMgrImpl soundMgrImpl = new CannedSoundMgrImpl();
        CannedPlaybackOwner owner = new CannedPlaybackOwner();
        SoundDeviceImpl device = new SoundDeviceImpl(0, 1);
        PlaybackImpl playback = new PlaybackImpl(0, soundMgrImpl, owner, null, device, 0.0F, 0);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(0, 1);
        soundDevice.removePlayback(playback);
        // no assertion, just a sanity check that no exception is thrown here
    }

}
