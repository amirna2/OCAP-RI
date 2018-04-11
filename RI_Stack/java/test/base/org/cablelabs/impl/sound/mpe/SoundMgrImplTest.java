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
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.sound.PlaybackOwner;

import junit.framework.TestCase;

public class SoundMgrImplTest extends TestCase
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
        SoundMgrImpl test = new MyCannedSoundMgrImpl();

    }

    public void testConstructorException()
    {
        SoundMgrImpl test = new MyCannedSoundMgrImplException();
    }

    public void testCreateSoundResultingHandle()
    {
        int soundHandle = 123;
        SoundMgrImpl soundMgrImpl = new MyCannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgrImpl.getSoundAPI();
        api.cannedSetCreateSoundReturnValue(soundHandle);
        SoundImpl sound = (SoundImpl) soundMgrImpl.createSound("", new byte[0], 0, 0);
        assertTrue(sound.getHandle() == soundHandle);
    }

    public void testCreateSoundException()
    {
        SoundMgrImpl soundMgrImpl = new MyCannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgrImpl.getSoundAPI();
        api.cannedSetCreateSoundThrowsException(true);
        SoundImpl sound = (SoundImpl) soundMgrImpl.createSound("", new byte[0], 0, 0);
        assertTrue(sound == null);
    }

    public void testGetDeviceException()
    {
        SoundMgrImpl soundMgrImpl = new MyCannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgrImpl.getSoundAPI();
        api.cannedSetGetSoundDevicesThrowsException(true);
        SoundImpl sound = (SoundImpl) soundMgrImpl.createSound("", new byte[0], 0, 0);
        SoundDeviceImpl device = soundMgrImpl.getDevice(sound, 0);
        assertTrue(device == null);
    }

    public void testPlaySoundException()
    {
        SoundMgrImpl soundMgrImpl = new MyCannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgrImpl.getSoundAPI();
        api.cannedSetPlaySoundThrowsException(true);
        SoundImpl sound = (SoundImpl) soundMgrImpl.createSound("", new byte[0], 0, 0);
        CannedPlaybackOwner owner = new CannedPlaybackOwner();
        SoundDeviceImpl device = new SoundDeviceImpl(1, 1);
        try
        {
            soundMgrImpl.playSound(device, sound, owner, new Time(0), false, false, 0.0F, 0);
            fail("Expected MPEMediaError to be thrown, not caught by SoundMgrImpl");
        }
        catch (MPEMediaError error)
        {
            // expected
        }
    }

    //
    // mostly for code coverage, but also sanity test that calling
    // destroy twice doesn't cause an exception
    //
    public void testDestroyTwice()
    {
        SoundMgrImpl soundMgrImpl = new MyCannedSoundMgrImpl();
        soundMgrImpl.destroy();
        soundMgrImpl.destroy();

    }

    public void testLockNotNull()
    {
        SoundMgrImpl soundMgrImpl = new MyCannedSoundMgrImpl();
        assertTrue(soundMgrImpl.getLock() != null);
    }

    public void testGetDevice() throws Exception
    {
        int handle = 123;

        MyCannedSoundMgrImpl.testHandles = new int[] { handle };
        MyCannedSoundMgrImpl.testSupportedHandles = new int[] { handle };
        MyCannedSoundMgrImpl.maxPlaybacks = 1;

        SoundMgrImpl soundMgr = new MyCannedSoundMgrImpl();
        CannedSoundAPI api = (CannedSoundAPI) soundMgr.getSoundAPI();
        SoundImpl sound = new SoundImpl(handle, soundMgr);
        SoundDeviceImpl device = soundMgr.getDevice(sound, 0);
        assertTrue(device != null);
        assertTrue(device.getHandle() == handle);
    }

    public void testGetDeviceNoDevicesForSound() throws Exception
    {
        int handle = 123;

        MyCannedSoundMgrImpl.testHandles = new int[] { 0, 1, 2 };
        MyCannedSoundMgrImpl.testSupportedHandles = new int[] {};

        SoundMgrImpl soundMgr = new MyCannedSoundMgrImpl();

        SoundImpl sound = new SoundImpl(handle, soundMgr);
        SoundDeviceImpl device = soundMgr.getDevice(sound, 0);
        assertTrue(device == null);
    }

    public void testGetDeviceNoDevicesInMgr() throws Exception
    {
        int handle = 123;

        MyCannedSoundMgrImpl.testHandles = new int[] {};
        MyCannedSoundMgrImpl.testSupportedHandles = new int[] {};

        SoundMgrImpl soundMgr = new MyCannedSoundMgrImpl();

        SoundImpl sound = new SoundImpl(handle, soundMgr);
        SoundDeviceImpl device = soundMgr.getDevice(sound, 0);
        assertTrue(device == null);
    }

    public void testGetDeviceDeviceHandleMorePlaybacks()
    {
        int handle = 123;

        MyCannedSoundMgrImpl.testHandles = new int[] { handle };
        MyCannedSoundMgrImpl.testSupportedHandles = new int[] { handle };
        MyCannedSoundMgrImpl.maxPlaybacks = 2;

        SoundMgrImpl soundMgr = new MyCannedSoundMgrImpl();

        SoundImpl sound1 = new SoundImpl(handle, soundMgr);
        SoundDeviceImpl device1 = soundMgr.getDevice(sound1, 0);
        assertTrue(device1 != null);
        //
        // add a playback to the device that was returned
        //
        int priority1 = 0;
        PlaybackOwner owner = new CannedPlaybackOwner();
        PlaybackImpl playback = new PlaybackImpl(1, soundMgr, owner, sound1, device1, 0.0F, priority1);
        device1.addPlayback(playback);

        //
        // start the second sound playback
        //
        SoundImpl sound2 = new SoundImpl(handle, soundMgr);
        SoundDeviceImpl device2 = soundMgr.getDevice(sound2, 0);
        assertTrue(device2 != null);
        //
        // since there was a single device that handled multiple
        // playbacks, check that the devices are the same
        //
        assertTrue(device1.equals(device2));
    }

    public void testGetDeviceMultipleDevicesFirstOneFull()
    {
        int handle1 = 123;
        int handle2 = 456;

        MyCannedSoundMgrImpl.testHandles = new int[] { handle1, handle2 };
        MyCannedSoundMgrImpl.testSupportedHandles = new int[] { handle1, handle2 };
        MyCannedSoundMgrImpl.maxPlaybacks = 1;

        SoundMgrImpl soundMgr = new MyCannedSoundMgrImpl();

        SoundImpl sound1 = new SoundImpl(1, soundMgr);
        SoundDeviceImpl device1 = soundMgr.getDevice(sound1, 0);
        assertTrue(device1 != null);
        int device1handle = device1.getHandle();
        assertTrue(device1handle == handle1 || device1handle == handle2);
        //
        // add a playback to the device that was returned
        //
        int priority1 = 0;
        PlaybackOwner owner = new CannedPlaybackOwner();
        PlaybackImpl playback = new PlaybackImpl(1, soundMgr, owner, sound1, device1, 0.0F, priority1);
        device1.addPlayback(playback);

        //
        // start the second sound playback
        //
        SoundImpl sound2 = new SoundImpl(2, soundMgr);
        SoundDeviceImpl device2 = soundMgr.getDevice(sound2, 0);
        assertTrue(device2 != null);
        int device2handle = device2.getHandle();
        assertTrue(device2handle == handle1 || device2handle == handle2);
        assertTrue(!device1.equals(device2));
    }

    public void testGetDevicePreemptsLowerPriority() throws Exception
    {
        int handle1 = 123;
        int handle2 = 456;
        int priority1 = 0;
        int priority2 = 255;

        MyCannedSoundMgrImpl.testHandles = new int[] { handle1 };
        MyCannedSoundMgrImpl.testSupportedHandles = new int[] { handle1 };
        MyCannedSoundMgrImpl.maxPlaybacks = 1;

        SoundMgrImpl soundMgr = new MyCannedSoundMgrImpl();

        SoundImpl sound1 = new SoundImpl(1, soundMgr);
        SoundDeviceImpl device1 = soundMgr.getDevice(sound1, priority1);
        assertTrue(device1 != null);
        int device1handle = device1.getHandle();
        assertTrue(device1handle == handle1 || device1handle == handle2);
        //
        // add a playback to the device that was returned
        //
        CannedPlaybackOwner owner = new CannedPlaybackOwner();
        PlaybackImpl playback = new PlaybackImpl(1, soundMgr, owner, sound1, device1, 0.0F, priority1);
        device1.addPlayback(playback);

        //
        // there is only a single device that can handle 1 playback,
        // now try to get a device for a higher priority
        //
        SoundImpl sound2 = new SoundImpl(1, soundMgr);
        SoundDeviceImpl device2 = soundMgr.getDevice(sound2, priority2);
        // device 1 and 2 should be the same
        assertTrue(device1.equals(device2));
        // the first playback should have been stopped
        assertTrue(owner.playbackStopped);

    }

    private static class MyCannedSoundMgrImpl extends SoundMgrImpl
    {
        static int[] testHandles = new int[] { 1, 2, 3 };

        static int[] testSupportedHandles = new int[0];

        static int maxPlaybacks;

        private CannedSoundAPI api;

        protected SoundAPI getSoundAPI()
        {
            if (api == null)
            {
                api = new CannedSoundAPI();
                api.cannedSetDevices(testHandles);
                api.cannedSetSupportedDevices(testSupportedHandles);
                api.cannedSetDeviceMaxPlaybacks(maxPlaybacks);
            }
            return api;
        }
    }

    private static class MyCannedSoundMgrImplException extends SoundMgrImpl
    {
        protected SoundAPI getSoundAPI()
        {
            CannedSoundAPI api = new CannedSoundAPI();
            api.cannedSetGetSoundDevicesThrowsException(true);
            return api;
        }
    }
}
