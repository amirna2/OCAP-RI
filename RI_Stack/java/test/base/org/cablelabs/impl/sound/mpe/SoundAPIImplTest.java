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

import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.media.mpe.MPEMediaError;

import junit.framework.TestCase;

public class SoundAPIImplTest extends TestCase
{
    CannedSoundAPIImpl soundAPI;

    protected void setUp() throws Exception
    {
        super.setUp();
        EDListener edListener = null;
        soundAPI = new CannedSoundAPIImpl(edListener);
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    public void testGetSoundDeviceCountException()
    {
        soundAPI.nativeGetSoundDeviceCountReturnValue = -1;
        try
        {
            int value = soundAPI.getSoundDeviceCount();
            fail("Nonzero return value from native should throw an exception");
        }
        catch (MPEMediaError err)
        {
            // expected
        }
    }

    public void testGetSoundDevicesException()
    {
        soundAPI.nativeGetSoundDevicesReturnValue = -1;
        try
        {
            int value = soundAPI.getSoundDevices(0, new int[0]);
            fail("Nonzero return value from native should throw an exception");
        }
        catch (MPEMediaError err)
        {
            // expected
        }
    }

    public void testCreateSoundException()
    {
        soundAPI.nativeCreateSoundReturnValue = -1;
        try
        {
            int value = soundAPI.createSound("test", new byte[0], 0, 0);
            fail("Nonzero return value from native should throw an exception");
        }
        catch (MPEMediaError err)
        {
            // expected
        }
    }

    public void testDestroySoundException()
    {
        soundAPI.nativeDestroySoundReturnValue = -1;
        try
        {
            soundAPI.destroySound(0);
            fail("Nonzero return value from native should throw an exception");
        }
        catch (MPEMediaError err)
        {
            // expected
        }
    }

    public void testPlaySoundException()
    {
        soundAPI.nativePlaySoundReturnValue = -1;
        try
        {
            soundAPI.playSound(0, 0, new Time(0), false, false, new float[]{0.0F});
            fail("Nonzero return value from native should throw an exception");
        }
        catch (MPEMediaError err)
        {
            // expected
        }
    }

    public void testStopSoundPlaybackException()
    {
        soundAPI.nativeStopSoundPlaybackReturnValue = -1;
        try
        {
            soundAPI.stopSoundPlayback(0);
            fail("Nonzero return value from native should throw an exception");
        }
        catch (MPEMediaError err)
        {
            // expected
        }
    }

    public void testGetSoundPlaybackTimeException()
    {
        soundAPI.nativeGetSoundPlaybackTimeReturnValue = -1;
        try
        {
            soundAPI.getSoundPlaybackTime(0);
            fail("Nonzero return value from native should throw an exception");
        }
        catch (MPEMediaError err)
        {
            // expected
        }
    }

    public void testSetSoundPlaybackTimeException()
    {
        soundAPI.nativeSetSoundPlaybackTimeReturnValue = -1;
        try
        {
            soundAPI.setSoundPlaybackTime(0, new Time(0));
            fail("Nonzero return value from native should throw an exception");
        }
        catch (MPEMediaError err)
        {
            // expected
        }
    }

    public void testGetDeviceMaxPlaybacksException()
    {
        soundAPI.nativeGetDeviceMaxPlaybacksReturnValue = -1;
        try
        {
            soundAPI.getDeviceMaxPlaybacks(0);
            fail("Nonzero return value from native should throw an exception");
        }
        catch (MPEMediaError err)
        {
            // expected
        }
    }

    public void testGetSoundDeviceCount()
    {
        int deviceCount = 5;
        soundAPI.nativeGetSoundDeviceCountResult = deviceCount;

        int testValue = soundAPI.getSoundDeviceCount();
        assertTrue(deviceCount == testValue);
    }

    public void testGetSoundDevices()
    {
        int[] soundDevices = new int[] { 1, 2, 3 };
        soundAPI.nativeGetSoundDevicesResult = soundDevices;
        int[] testValue = new int[3];
        soundAPI.getSoundDevices(testValue);

        assertArrayContentEqual(soundDevices, testValue);
    }

    public void testGetSoundDevicesForSound()
    {
        int[] soundDevices = new int[] { 1, 2, 3 };
        soundAPI.nativeGetSoundDevicesResult = soundDevices;
        int[] testValue = new int[3];
        soundAPI.getSoundDevices(0, testValue);

        assertArrayContentEqual(soundDevices, testValue);
    }

    public void testCreateSound()
    {
        int soundHandle = 1;
        soundAPI.nativeCreateSoundResult = soundHandle;
        int retValue = soundAPI.createSound("test", new byte[0], 0, 0);
        assertTrue(retValue == soundHandle);
    }

    public void testStopSoundPlayback()
    {
        long stopTime = 123l;
        soundAPI.nativeStopSoundPlaybackResult = stopTime;
        Time t = soundAPI.stopSoundPlayback(0);
        assertTrue(t != null);
        assertTrue(stopTime == t.getNanoseconds());
    }

    public void testPlaySound()
    {
        int playbackHandle = 123;
        soundAPI.nativePlaySoundResult = playbackHandle;
        int retValue = soundAPI.playSound(0, 0, new Time(0), false, false, new float[]{0.0F});
        assertTrue(retValue == playbackHandle);
    }

    public void testGetSoundPlaybackTime()
    {
        long playbackTime = 23l;
        soundAPI.nativeGetSoundPlaybackTimeResult = playbackTime;
        Time t = soundAPI.getSoundPlaybackTime(0);
        assertTrue(t != null);
        assertTrue(playbackTime == t.getNanoseconds());
    }

    public void testSetSoundPlaybackTime()
    {
        long playbackTime = 123l;
        soundAPI.nativeSetSoundPlaybackTimeResult = playbackTime;
        Time t = soundAPI.setSoundPlaybackTime(0, new Time(0));
        assertTrue(t != null);
        assertTrue(t.getNanoseconds() == playbackTime);
    }

    public void testGetDeviceMaxPlaybacks()
    {
        int maxPlaybacks = 5;
        soundAPI.nativeGetDeviceMaxPlaybacksResult = maxPlaybacks;
        int retValue = soundAPI.getDeviceMaxPlaybacks(0);
        assertTrue(retValue == maxPlaybacks);
    }

    private static void assertArrayContentEqual(int[] a, int[] b)
    {
        assertTrue(a != null);
        assertTrue(b != null);
        assertTrue(a.length == b.length);

        for (int i = 0; i < a.length; i++)
        {
            boolean matchFound = false;
            for (int j = 0; j < b.length; j++)
            {
                if (a[i] == b[j])
                {
                    matchFound = true;
                    break;
                }
            }
            assertTrue(matchFound);
        }
    }
}
