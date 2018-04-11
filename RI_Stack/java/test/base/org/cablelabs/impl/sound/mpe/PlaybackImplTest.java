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
import java.util.Collections;

import javax.media.Time;

import junit.framework.TestCase;

import org.cablelabs.impl.sound.PlaybackOwner;
import org.cablelabs.impl.sound.Sound;

public class PlaybackImplTest extends TestCase
{
    public void testConstructor()
    {
        int handle = 10;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);
        assertTrue(playback.getHandle() == handle);
        assertTrue(sound.equals(playback.getSound()));
        assertTrue(soundDevice.equals(playback.getDevice()));
        assertTrue(owner.equals(playback.getOwner()));
    }

    public void testStop()
    {
        int handle = 10;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);
        playback.stop();
        assertTrue(api.cannedGetStopSoundPlaybackCalled());
    }

    public void testStopTwice()
    {
        int handle = 10;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);
        playback.stop();
        playback.stop();
    }

    public void testSetTime()
    {
        int handle = 10;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);

        Time t = new Time(0);
        api.cannedSetExpectedTimeHandle(playback.getHandle());
        playback.setTime(t);

        assertTrue(api.cannedGetTime() != null);
        assertTrue(api.cannedGetTime().equals(t));
    }

    public void testGetTime()
    {
        int handle = 10;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);

        Time t = new Time(0);
        api.cannedSetTime(t);
        api.cannedSetExpectedTimeHandle(playback.getHandle());

        assertTrue(playback.getTime() != null);
        assertTrue(playback.getTime().equals(t));
    }

    public void testComparable()
    {
        int handle = 10;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        PlaybackImpl playback1 = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, 1);
        PlaybackImpl playback2 = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, 2);
        PlaybackImpl playback3 = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, 3);

        ArrayList list = new ArrayList();
        list.add(playback2);
        list.add(playback1);
        list.add(playback3);

        Collections.sort(list);
        assertTrue(list.get(0).equals(playback1));
        assertTrue(list.get(1).equals(playback2));
        assertTrue(list.get(2).equals(playback3));
    }

    public void testSetHandle()
    {
        int handle = 1;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);

        int newHandle = 10;
        assertTrue(handle != newHandle);

        playback.setHandle(newHandle);
        assertTrue(newHandle == playback.getHandle());
    }

    public void testFinalizeStops()
    {
        int handle = 1;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);

        playback.finalize();
        assertTrue(api.cannedGetStopSoundPlaybackCalled());
    }

    public void testExceptionInStopp()
    {
        int handle = 1;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();
        api.cannedSetStopSoundThrowsException(true);
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);
        playback.stop();

        assertTrue(api.cannedGetStopSoundPlaybackCalled());

    }

    public void testGetTimeOnStoppedReturnsNull()
    {
        int handle = 1;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();
        api.cannedSetStopSoundThrowsException(true);
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);
        playback.stop();
        Time t = playback.getTime();
        assertTrue(t == null);
    }

    public void testSetTimeOnStoppedReturnsNull()
    {
        int handle = 1;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();
        api.cannedSetStopSoundThrowsException(true);
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);
        playback.stop();
        Time t = playback.setTime(new Time(123));
        assertTrue(t == null);
    }

    public void testGetTimeNativeExceptionReturnsNull()
    {
        int handle = 1;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();
        // set the expected handle to something other than the playback handle
        api.cannedSetExpectedTimeHandle(-1);
        api.cannedSetStopSoundThrowsException(true);
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);
        Time t = playback.getTime();
        assertTrue(t == null);
    }

    public void testSetTimeNativeExceptionReturnsNull()
    {
        int handle = 1;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();
        // set the expected handle to something other than the playback handle
        api.cannedSetExpectedTimeHandle(-1);
        api.cannedSetStopSoundThrowsException(true);
        PlaybackOwner owner = new CannedPlaybackOwner();
        Sound sound = new SoundImpl(1, soundMgr);
        SoundDeviceImpl soundDevice = new SoundDeviceImpl(1, 1);
        int priority = 1;
        PlaybackImpl playback = new PlaybackImpl(handle, soundMgr, owner, sound, soundDevice, 0.0F, priority);
        Time t = playback.setTime(new Time(0));
        assertTrue(t == null);
    }
}
