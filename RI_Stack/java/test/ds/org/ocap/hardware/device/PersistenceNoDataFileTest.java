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

package org.ocap.hardware.device;

import org.ocap.hardware.Host;

import org.cablelabs.impl.debug.Debug;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.hardware.device.DeviceSettingsHostImpl;
import org.cablelabs.impl.ocap.hardware.device.DeviceSettingsHostPersistence;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class PersistenceNoDataFileTest extends TestCase
{
    public static final int SIM_POWERMODE = 2;

    public static final String SIM_AUDIO_PORT_0_ID = "AudioPort_SPDIF";

    public static final int SIM_AUDIO_PORT_0_COMPRESSION = 0;

    public static final float SIM_AUDIO_PORT_0_GAIN = 70.0f;

    public static final int SIM_AUDIO_PORT_0_ENCODING = 3;

    public static final float SIM_AUDIO_PORT_0_LEVEL = 0.6f;

    public static final boolean SIM_AUDIO_PORT_0_LOOPTHRU = false;

    public static final boolean SIM_AUDIO_PORT_0_MUTED = false;

    public static final int SIM_AUDIO_PORT_0_STEREOMODE = 3;

    public static final String SIM_VIDEO_PORT_CONFIG = "Fixed_ConfigSD"; // Same
                                                                         // for
                                                                         // all
                                                                         // ports

    public static final String SIM_VIDEO_PORT_0_ID = "VideoPort_RF";

    public static final String SIM_VIDEO_PORT_1_ID = "VideoPort_RCA";

    public static final String SIM_VIDEO_PORT_2_ID = "VideoPort_SVIDEO";

    public static final String SIM_VIDEO_PORT_3_ID = "VideoPort_1394";

    public static final String SIM_VIDEO_PORT_4_ID = "VideoPort_DVI";

    public static final String SIM_VIDEO_PORT_5_ID = "VideoPort_COMPONENT";

    public static final String SIM_VIDEO_PORT_6_ID = "VideoPort_HDMI";

    public static final String SIM_VIDEO_PORT_7_ID = "VideoPort_INTERNAL";

    // values used in TestWrites
    public static final int TW_POWER = Host.FULL_POWER;

    public static final String TW_AUDIO_PORT_0 = SIM_AUDIO_PORT_0_ID;

    public static final int TW_AUDIO_PORT_0_STEREOMODE = AudioOutputPort.STEREO_MODE_MONO;

    public static final String TW_VIDEO_PORT_0 = SIM_VIDEO_PORT_0_ID;

    public static final String TW_VIDEO_PORT__CONFIG = "Fixed_ConfigSD";

    private static final boolean SIM_VOLUMEKEY = false;

    private static final boolean SIM_MUTEKEY = false;

    private static final String SIM_MAINPORT = "VideoPort_RF";

    public PersistenceNoDataFileTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTest(new PersistenceNoDataFileTest("testStartAll")); // done
                                                                      // first
                                                                      // and
                                                                      // only
                                                                      // once
        suite.addTest(new PersistenceNoDataFileTest("testLoadDefaults"));
        // writing before this will persist the file and so we are checking w/
        // file.
        // This test catch any gross errors.
        suite.addTest(new PersistenceWithDataFileTest("testResetAllDefaults"));
        suite.addTest(new PersistenceNoDataFileTest("testWrites")); // change
                                                                    // default
                                                                    // state.
                                                                    // file is
                                                                    // now
                                                                    // written
        suite.addTest(new PersistenceWithDataFileTest("testResetAllDefaults"));
        suite.addTest(new PersistenceNoDataFileTest("testWrites")); // return to
                                                                    // state
                                                                    // expected
                                                                    // by
                                                                    // PersistWithDataFileTest
        return suite;
    }

    /*
     * Start of tests
     */

    public void testStartAll()
    {
        // this is needed to load the persistence and initialized the needed
        // host manager
        try
        {
            ManagerManager.startAll();
        }
        catch (IllegalStateException e)
        {
            ; // already started
        }
    }

    public void testLoadDefaults()
    {
        DeviceSettingsHostPersistence persist = getPersistence();

        assertNotNull(persist);

        assertDefaultValues(persist);
    }

    public void testResetAllDefaults()
    {
        DeviceSettingsHostPersistence persist = getPersistence();

        getHost().resetAllDefaults();

        assertDefaultValues(persist);
    }

    public static void assertDefaultValues(DeviceSettingsHostPersistence persist)
    {
        assertEquals(SIM_AUDIO_PORT_0_COMPRESSION, persist.getAudioCompression(SIM_AUDIO_PORT_0_ID));
        assertEquals(SIM_AUDIO_PORT_0_GAIN, persist.getAudioGain(SIM_AUDIO_PORT_0_ID), 0.0f);
        assertEquals(SIM_AUDIO_PORT_0_ENCODING, persist.getAudioEncoding(SIM_AUDIO_PORT_0_ID));
        assertEquals(SIM_AUDIO_PORT_0_LEVEL, persist.getAudioLevel(SIM_AUDIO_PORT_0_ID), 0.0f);
        assertEquals(SIM_AUDIO_PORT_0_LOOPTHRU, persist.getAudioLoopThru(SIM_AUDIO_PORT_0_ID));
        assertEquals(SIM_AUDIO_PORT_0_MUTED, persist.getAudioMuted(SIM_AUDIO_PORT_0_ID));
        assertEquals(SIM_AUDIO_PORT_0_STEREOMODE, persist.getAudioStereoMode(SIM_AUDIO_PORT_0_ID));

        assertEquals(SIM_POWERMODE, persist.getPowerMode());
        assertEquals(SIM_VOLUMEKEY, persist.getVolumeKeyControl());
        assertEquals(SIM_MUTEKEY, persist.getMuteKeyControl());

        assertEquals(SIM_MAINPORT, persist.getMainVideoOutputPort());
        assertEquals(SIM_VIDEO_PORT_CONFIG, persist.getVideoPortOutputConfig(SIM_VIDEO_PORT_0_ID).getName());
        // assertEquals(SIM_VIDEO_PORT_CONFIG,
        // persist.getVideoPortOutputConfig(SIM_VIDEO_PORT_1_ID));
        // assertEquals(SIM_VIDEO_PORT_CONFIG,
        // persist.getVideoPortOutputConfig(SIM_VIDEO_PORT_2_ID));
        // assertEquals(SIM_VIDEO_PORT_CONFIG,
        // persist.getVideoPortOutputConfig(SIM_VIDEO_PORT_3_ID));
        // assertEquals(SIM_VIDEO_PORT_CONFIG,
        // persist.getVideoPortOutputConfig(SIM_VIDEO_PORT_4_ID));
        // assertEquals(SIM_VIDEO_PORT_CONFIG,
        // persist.getVideoPortOutputConfig(SIM_VIDEO_PORT_5_ID));
        // assertEquals(SIM_VIDEO_PORT_CONFIG,
        // persist.getVideoPortOutputConfig(SIM_VIDEO_PORT_6_ID));
        // assertEquals(SIM_VIDEO_PORT_CONFIG,
        // persist.getVideoPortOutputConfig(SIM_VIDEO_PORT_7_ID));
    }

    /**
     * Test writing to persistence. This saves a host data file. Changes to this
     * test will affect the tests in PersistenceWithDataFileTest
     */
    public void testWrites()
    {
        DeviceSettingsHostPersistence persist = getPersistence();

        persist.persistPowerMode(TW_POWER);

        persist.persistAudioStereoMode(TW_AUDIO_PORT_0, TW_AUDIO_PORT_0_STEREOMODE);

        // persist.persistPortOutputConfig(portUid, config)
    }

    public static void assertTestWrites(DeviceSettingsHostPersistence persist)
    {
        assertEquals(TW_POWER, persist.getPowerMode());
        assertEquals(TW_AUDIO_PORT_0_STEREOMODE, persist.getAudioStereoMode(TW_AUDIO_PORT_0));
        VideoOutputConfiguration config = persist.getVideoPortOutputConfig(SIM_VIDEO_PORT_0_ID);

        // assertEquals(TW_VIDEO_PORT_2_CONFIG,
        // persist.getVideoPortOutputConfig(TW_VIDEO_PORT_2));

        return;
    }

    private DeviceSettingsHostPersistence getPersistence()
    {
        return (DeviceSettingsHostPersistence) DeviceSettingsHostImpl.getHostPersistence();
    }

    DeviceSettingsHostImpl getHost()
    {
        return (DeviceSettingsHostImpl) Host.getInstance();
    }

}
