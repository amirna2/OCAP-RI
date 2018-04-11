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

package org.ocap.application;

import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.test.iftc.InterfaceTestSuite;

import java.util.Hashtable;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;

import org.dvb.application.AppAttributesTest;

/**
 * Tests the OcapAppAttributes interface.
 */
public class OcapAppAttributesTest extends AppAttributesTest
{
    /**
     * Tests getApplicationControlCode().
     */
    public void testGetApplicationControlCode()
    {
        assertEquals("Unexpected acc", ocapFactory.getControlCode(), ocapappattributes.getApplicationControlCode());
    }

    /**
     * Tests getStoragePriority().
     */
    public void testGetStoragePriority()
    {
        assertEquals("Unexpected storage priority", ocapFactory.isStored() ? ocapFactory.getStoragePriority() : 0,
                ocapappattributes.getStoragePriority());
    }

    /**
     * Tests hasNewVersion().
     */
    public void testHasNewVersion()
    {
        assertEquals("Unexpected hasNewVersion", ocapappattributes.hasNewVersion(), ocapFactory.hasNewVersion());
    }

    /**
     * Tests hasNewVersion().
     */
    public void testIsNewVersionStored()
    {
        assertEquals("Unexpected isNewVersionSignaled", ocapappattributes.isNewVersionSignaled(),
                ocapFactory.isNewVersionSignaled());
    }

    /**
     * Overrides AppAttributesTest.testGetType().
     */
    public void testGetType() throws Exception
    {
        assertEquals("Only OCAP_J apps should be supported", OcapAppAttributes.OCAP_J, ocapappattributes.getType());
    }

    public void testGetProperty() throws Exception
    {
        super.testGetProperty();

        checkProperty("ocap.j.location", ocapFactory.getLocationBase());
    }

    /**
     * Overrides AppAttributesTest.testGetProfiles.
     */
    public void testGetProfiles() throws Exception
    {
        String[] expected = { "ocap.profile" };
        String[] unexpected = {};

        String[] profiles = ocapappattributes.getProfiles();
        assertNotNull("getProfiles shouldn't return null", profiles);
        assertTrue("Profile set should be non-empty", profiles.length > 0);

        Hashtable h = new Hashtable();
        for (int i = 0; i < profiles.length; ++i)
        {
            assertNotNull("No null profiles should be returned", profiles[i]);
            h.put(profiles[i], profiles[i]);
        }

        for (int i = 0; i < expected.length; ++i)
            assertEquals("Did not find expected profile", expected[i], h.get(expected[i]));
        for (int i = 0; i < unexpected.length; ++i)
            assertNull("Did not expected to file profile: " + unexpected[i], h.get(unexpected[i]));
    }

    /**
     * Overrides AppAttributesTest.testGetVersions()
     * 
     * @throws Exception
     */
    public void testGetVersions() throws Exception
    {
        int[] versions = ocapappattributes.getVersions("ocap.profile");
        assertNotNull("Versions array should not be null", versions);
        assertEquals("Versions array size is incorrect", 3, versions.length);
        assertEquals("Major version does not match", 1, versions[0]);
        assertEquals("Minor version does not match", 0, versions[1]);
        assertEquals("Micro version does not match", 0, versions[2]);
    }

    protected OcapAttrFactory ocapFactory;

    public static interface OcapAttrFactory extends AppAttrFactory
    {
        boolean isStored(); // is the first version stored

        AppEntry getAppEntry();

        AppEntry getNewAppEntry();

        int getStoragePriority();

        long getVersion();

        boolean hasNewVersion();

        long getNewVersion();

        boolean isNewVersionSignaled();
    }

    protected OcapAppAttributesTest(String name, Class testedClass, ImplFactory f)
    {
        super(name, testedClass, f);
        ocapFactory = (OcapAttrFactory) f;
        setUseClassInName(true);
    }

    public OcapAppAttributesTest(String name, ImplFactory f)
    {
        this(name, OcapAppAttributes.class, f);
    }

    protected OcapAppAttributes createOcapAppAttributes()
    {
        return (OcapAppAttributes) createImplObject();
    }

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(OcapAppAttributesTest.class);
        suite.setName(OcapAppAttributes.class.getName());
        return suite;
    }

    protected OcapAppAttributes ocapappattributes;

    protected void setUp() throws Exception
    {
        super.setUp();
        ocapappattributes = createOcapAppAttributes();
    }

    protected void tearDown() throws Exception
    {
        ocapappattributes = null;
        super.tearDown();
    }
}
