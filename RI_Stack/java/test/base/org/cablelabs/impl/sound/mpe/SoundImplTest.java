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

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.sound.PlaybackOwner;

import junit.framework.TestCase;

public class SoundImplTest extends TestCase
{
    private CallerContextManager save;

    private CCMgr ccmgr;

    protected void setUp() throws Exception
    {
        super.setUp();
        replaceCCMgr();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        restoreCCMgr();
    }

    private void replaceCCMgr()
    {
        save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, ccmgr = new CCMgr(save));
    }

    private void restoreCCMgr()
    {
        if (save != null) ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
    }

    public void testConstructor()
    {
        int handle = 123;
        SoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        SoundImpl sound = new SoundImpl(handle, soundMgr);
        assertTrue(sound.getHandle() == handle);
    }

    public void testDispose()
    {
        int handle = 123;
        CannedSoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        SoundImpl sound = new SoundImpl(handle, soundMgr);
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();

        sound.dispose();
        assertEquals(handle, api.cannedGetDestroyedSoundHandle());
    }

    public void testPlay() throws Exception
    {
        final CannedCallerContext cc = new CannedCallerContext();
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {

                int handle = 123;
                int playbackHandle = 999;
                CannedSoundMgrImpl soundMgr = new CannedSoundMgrImpl();
                CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();

                SoundDeviceImpl device = new SoundDeviceImpl(0, 1);
                SoundDeviceImpl[] deviceArr = new SoundDeviceImpl[] { device };
                int[] deviceHandles = new int[] { deviceArr[0].getHandle() };
                soundMgr.cannedSetDevices(deviceArr);
                api.cannedSetDevices(deviceHandles);

                SoundImpl sound = new SoundImpl(handle, soundMgr);

                PlaybackOwner owner = new CannedPlaybackOwner();
                Time time = new Time(0);

                api.cannedSetPlaySoundReturnValue(playbackHandle);
                PlaybackImpl playback = (PlaybackImpl) sound.play(owner, time, false, cc, false, 0.0F);
                assertTrue(playback != null);
                assertEquals(playbackHandle, playback.getHandle());
                assertEquals(handle, api.cannedGetPlaySoundSound());
                assertTrue(device.getAllPlaybacks().contains(playback));
                assertTrue(playback.getOwner().equals(owner));
            }
        });
    }

    public void testFinalizedCallsDestroy()
    {
        int handle = 123;
        CannedSoundMgrImpl soundMgr = new CannedSoundMgrImpl();
        SoundImpl sound = new SoundImpl(handle, soundMgr);
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();

        sound.finalize();
        assertEquals(handle, api.cannedGetDestroyedSoundHandle());

    }

    public void testPlayNoDevices() throws Exception
    {
        final CannedCallerContext cc = new CannedCallerContext();
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {

                int handle = 123;
                int playbackHandle = 999;
                CannedSoundMgrImpl soundMgr = new CannedSoundMgrImpl();
                CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();

                SoundDeviceImpl[] deviceArr = new SoundDeviceImpl[] {};
                int[] deviceHandles = new int[] {};
                soundMgr.cannedSetDevices(deviceArr);
                api.cannedSetDevices(deviceHandles);

                SoundImpl sound = new SoundImpl(handle, soundMgr);

                PlaybackOwner owner = new CannedPlaybackOwner();
                Time time = new Time(0);

                api.cannedSetPlaySoundReturnValue(playbackHandle);
                PlaybackImpl playback = (PlaybackImpl) sound.play(owner, time, false, cc, false, 0.0F);
                assertTrue(playback == null);

            }
        });
    }

}
