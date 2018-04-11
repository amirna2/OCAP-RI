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

package org.cablelabs.impl.security;

import org.cablelabs.impl.security.AppCodeSourceTest.DummyCert;

import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.cert.Certificate;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppID;

/**
 * Tests PermissionInfoImpl.
 * 
 * @author Aaron Kamienski
 */
public class PermissionInfoImplTest extends TestCase
{
    public void testGetAppID()
    {
        AppID id = new AppID(27, 33);
        PermissionInfoImpl info = new PermissionInfoImpl(id, false, null, null);
        assertEquals("UnExpected appid returned", id, info.getAppID());
        assertEquals("Unexpected appid returned (repeat)", id, info.getAppID());
    }

    public void testIsManufacturerApp()
    {
        PermissionInfoImpl info = new PermissionInfoImpl(null, false, null, null);
        assertEquals("Unexpected host app value returned", false, info.isManufacturerApp());
        assertEquals("Unexpected host app value returned (repeat)", false, info.isManufacturerApp());

        info = new PermissionInfoImpl(null, true, null, null);
        assertEquals("Unexpected host app value returned", true, info.isManufacturerApp());
        assertEquals("Unexpected host app value returned (repeat)", true, info.isManufacturerApp());
    }

    public void testGetCertificates()
    {
        PermissionInfoImpl info = new PermissionInfoImpl(null, false, null, null);
        assertSame("Expected null certificates", null, info.getCertificates());

        Certificate[][] certs = { { new DummyCert(1), new DummyCert(2) },
                { new DummyCert(3), new DummyCert(4), new DummyCert(10) },
                { new DummyCert(5), new DummyCert(11), new DummyCert(0), new DummyCert(10) }, };
        info = new PermissionInfoImpl(null, false, certs, null);
        assertTrue("Expected equiv certificates returned", equals(certs, info.getCertificates()));
    }

    protected boolean equals(Certificate[][] c1, Certificate[][] c2)
    {
        if (c1 == c2) return true;
        if (c1 == null) return c2 == null;
        if (c2 == null) return false;

        if (c1.length != c2.length) return false;

        for (int i = 0; i < c1.length; ++i)
            if (!equals(c1[i], c2[i])) return false;
        return true;
    }

    protected boolean equals(Certificate[] c1, Certificate[] c2)
    {
        if (c1 == c2) return true;
        if (c1 == null) return c2 == null;
        if (c2 == null) return false;

        if (c1.length != c2.length) return false;

        for (int i = 0; i < c1.length; ++i)
        {
            if (c1[i] == null)
            {
                if (c2[i] != null) return false;
            }
            else if (!c1[i].equals(c2[i])) return false;
        }
        return true;
    }

    public void XtestIsPrivilegedCertificate()
    {
        // TODO(AaronK): test isPrivilegedCertificate
        fail("Unimplemented test");
    }

    public void testGetRequestedPermissions()
    {
        PermissionCollection perms = new Permissions();
        PermissionInfoImpl info = new PermissionInfoImpl(null, false, null, perms);

        assertSame("Expected same permission collection", perms, info.getRequestedPermissions());
    }

    public void testToString()
    {
        PermissionInfoImpl info = new PermissionInfoImpl(null, false, null, null);
        assertNotNull("Should not return null", info.toString());

        info = new PermissionInfoImpl(new AppID(10, 10), false, null, null);
        assertNotNull("Should not return null", info.toString());

        info = new PermissionInfoImpl(null, true, null, null);
        assertNotNull("Should not return null", info.toString());

        info = new PermissionInfoImpl(null, false, new Certificate[2][2], null);
        assertNotNull("Should not return null", info.toString());

        info = new PermissionInfoImpl(null, false, null, new Permissions());
        assertNotNull("Should not return null", info.toString());
    }

    /**
     * Constructor for PermissionInfoImplTest.
     * 
     * @param name
     */
    public PermissionInfoImplTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(PermissionInfoImplTest.class);
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
}
