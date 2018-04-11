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

package org.cablelabs.impl.util;

import junit.framework.*;

//import org.cablelabs.impl.util.MPEEnv;

/**
 * Tests the org.cablelabs.impl.util.MPEEnv class
 * 
 * @author Greg Rutz
 */
public class MPEEnvTest extends TestCase
{

    public MPEEnvTest(String name)
    {
        super(name);
    }

    /**
     * @todo disabled per 4595
     */
    public void testGetEnv()
    {
        String testString;
        int testInt;
        long testLong;
        testString = MPEEnv.getEnv("MPE.SYS.OCAPVERSION");
        assertNotNull("Should be able to get MPE.SYS.OCAPVERSION property", testString);

        testString = MPEEnv.getEnv("this.should.not.be.a.property.name");
        assertNull("Should not be able to get garbage property", testString);

        // Test default value functions where the default value is/isnot used

        // String
        testString = MPEEnv.getEnv("MPE.SYS.OCAPVERSION", "JunkOCAPVersion");
        assertFalse("MPE.SYS.OCAPVERSION should be defined and we should not be using the default",
                testString.equals("JunkOCAPVersion"));

        testString = MPEEnv.getEnv("some.ridiculous.property.name", "test1");
        assertTrue("Default string property is incorrect", testString.equals("test1"));

        // Integer
        testInt = MPEEnv.getEnv("ocap.memory.total", 15);
        assertFalse("ocap.memory.total property should not use default int value", testInt == 15);

        testInt = MPEEnv.getEnv("another.ridiculous.property.name", 15);
        assertTrue("Default int property is incorrect", testInt == 15);

        // Long
        testLong = MPEEnv.getEnv("ocap.memory.total", 101010101L);
        assertFalse("ocap.memory.total property should not use default long value", testLong == 101010101L);

        testLong = MPEEnv.getEnv("yet.another.ridiculous.property.name", 1000000L);
        assertTrue("Default long property is incorrect", testLong == 1000000L);
    }

    public void testOverrideEnv()
    {
        String original;
        String env;

        // Save the original env value
        original = MPEEnv.getEnv("OCAP.userprefs.datafile");

        if (original == null) return;

        // Set a new value
        MPEEnv.setEnv("OCAP.userprefs.datafile", "/newtestdatafile");

        env = MPEEnv.getEnv("OCAP.userprefs.datafile");
        assertTrue("Env value should have been overidden with new value", env.equals("/newtestdatafile"));

        // Remove the override
        MPEEnv.removeEnvOverride("OCAP.userprefs.datafile");
        env = MPEEnv.getEnv("OCAP.userprefs.datafile");
        assertTrue("Env value should have been overidden with new value", env.equals(original));
    }

    public void testDisplayCableLabsVars()
    {
        // Helpful property printouts (but no tests)
        System.out.println("OCAP.mgrmgr.Signalling = " + MPEEnv.getEnv("OCAP.mgrmgr.Signalling"));
        System.out.println("OCAP.mgrmgr.Service = " + MPEEnv.getEnv("OCAP.mgrmgr.Service"));
        System.out.println("OCAP.mgrmgr.Storage = " + MPEEnv.getEnv("OCAP.mgrmgr.Storage"));
        System.out.println("OCAP.mgrmgr.props = " + MPEEnv.getEnv("OCAP.mgrmgr.props", "/mgrmgr.properties"));
        System.out.println("OCAP.havi.setup = " + MPEEnv.getEnv("OCAP.havi.setup", "havi.properties"));
        System.out.println("OCAP.extensions = " + MPEEnv.getEnv("OCAP.extensions"));
        System.out.println("OCAP.ait.ignore = " + MPEEnv.getEnv("OCAP.ait.ignore", "false"));
        System.out.println("OCAP.watchdog.class = " + MPEEnv.getEnv("OCAP.watchdog.class"));
        System.out.println("OCAP.watchdog = " + MPEEnv.getEnv("OCAP.watchdog", "Debug"));
        System.out.println("OCAP.appstorage.maxbytes = " + MPEEnv.getEnv("OCAP.appstorage.maxbytes", 1024L * 100L));
        System.out.println("OCAP.appstorage.dir = " + MPEEnv.getEnv("OCAP.appstorage.dir"));
        System.out.println("OCAP.rcInterface.type = " + MPEEnv.getEnv("OCAP.rcInterface.type"));
        System.out.println("OCAP.rcInterface.subType = " + MPEEnv.getEnv("OCAP.rcInterface.subType"));
        System.out.println("OCAP.rcInterface.dataRate = " + MPEEnv.getEnv("OCAP.rcInterface.dataRate"));
        System.out.println("OCAP.xait.ignore = " + MPEEnv.getEnv("OCAP.xait.ignore"));
        System.out.println("OCAP.xait.timeout = " + MPEEnv.getEnv("OCAP.xait.timeout", 30));
        System.out.println("OCAP.monapp.resident = " + MPEEnv.getEnv("OCAP.monapp.resident"));
        System.out.println("OCAP.sicache.asyncTimeout = " + MPEEnv.getEnv("OCAP.sicache.asyncTimeout", 30000));
        System.out.println("OCAP.sicache.asyncInterval = " + MPEEnv.getEnv("OCAP.sicache.asyncInterval", 10000));
        System.out.println("OCAP.sicache.maxAge = " + MPEEnv.getEnv("OCAP.sicache.maxAge", 3600000));
        System.out.println("OCAP.sicache.flushInterval = " + MPEEnv.getEnv("OCAP.sicache.flushInterval", 30000));
        System.out.println("OCAP.security.disabled = " + MPEEnv.getEnv("OCAP.security.disabled"));
        System.out.println("OCAP.userprefs.datafile = " + MPEEnv.getEnv("OCAP.userprefs.datafile"));
        System.out.println("OCAP.dvr.serial.dir = " + MPEEnv.getEnv("OCAP.dvr.serial.dir"));
        System.out.println("OCAP.dvr.serial.prefix = " + MPEEnv.getEnv("OCAP.dvr.serial.prefix", ""));

        // Used by the JVM
        System.out.println("MPE_SYS_OSVERSION = " + MPEEnv.getEnv("MPE_SYS_OSVERSION"));
        System.out.println("MPE_SYS_OSNAME = " + MPEEnv.getEnv("MPE_SYS_OSNAME"));
        System.out.println("MPE_SYS_MPEVERSION = " + MPEEnv.getEnv("MPE_SYS_MPEVERSION"));
        System.out.println("MPE.SYS.OCAPVERSION = " + MPEEnv.getEnv("MPE.SYS.OCAPVERSION"));

        assertTrue(true);
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
        TestSuite suite = new TestSuite(MPEEnvTest.class);
        return suite;
    }

}
