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

import org.cablelabs.test.TestUtils;
import java.io.FilePermission;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;
import junit.framework.*;
import org.dvb.application.AppID;
import org.dvb.application.AppsControlPermission;
import org.ocap.system.MonitorAppPermission;
import org.cablelabs.test.ProxySecurityManager;
import org.dvb.application.AppProxyTest.DummySecurityManager;
import net.sourceforge.groboutils.junit.v1.iftc.*;

/**
 * Tests the PermissionInformation class.
 * 
 * Should be made into an interface test, as this is now abstract.
 */
public class PermissionInformationTest extends InterfaceTestCase
{
    /**
     * Verifies that it's an interface.
     */
    public void testInterface() throws Exception
    {
        assertTrue("PermissionInformation should be an interface", PermissionInformation.class.isInterface());
    }

    /**
     * Tests getAppID().
     */
    public void testGetAppID() throws Exception
    {
        DummySecurityManager sm = new DummySecurityManager();
        ProxySecurityManager.install();
        ProxySecurityManager.push(sm);
        try
        {
            AppID id = new AppID(20, 10);
            AppID id2 = new AppID(30, 40);

            PermissionInformation pi = createInstance(id, new Permissions());
            PermissionInformation pi2 = createInstance(id2, new Permissions());

            sm.p = null;
            assertEquals("getAppID should return id set on construction", id, pi.getAppID());
            assertNotNull("getAppID should check with SecurityManager", sm.p);
            assertTrue("getAppID should check for MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("getAppID should check for MonitorAppPermission(security)", "security",
                    ((MonitorAppPermission) sm.p).getName());

            sm.p = null;
            assertEquals("getAppID should return id set on construction", id2, pi2.getAppID());
            assertNotNull("getAppID should check with SecurityManager", sm.p);
            assertTrue("getAppID should check for MonitorAppPermission", sm.p instanceof MonitorAppPermission);
            assertEquals("getAppID should check for MonitorAppPermission(security)", "security",
                    ((MonitorAppPermission) sm.p).getName());
        }
        finally
        {
            ProxySecurityManager.pop();
        }
    }

    /**
     * Tests isManufacturerApp().
     */
    public void testIsManufacturerApp()
    {
        fail("Unimplemented test");
    }

    /**
     * Tests getCertificates().
     */
    public void testGetCertificates() throws Exception
    {
        PermissionInformation pi = createInstance(new AppID(20, 20), new Permissions());
        assertNotNull("getCertificates should not return null", pi.getCertificates());
        // should return zero length array instead...

        // How can we know what it will return....?
        fail("Unfinished test");
    }

    private void verifyPermissionCollection(PermissionCollection p1, PermissionCollection p2)
    {
        for (Enumeration e = p1.elements(); e.hasMoreElements();)
        {
            assertTrue("PermissionCollection's should be equivalent", p2.implies((Permission) e.nextElement()));
        }
    }

    /**
     * Tests getRequestedPermissions().
     */
    public void testGetRequestedPermissions() throws Exception
    {
        Permissions permissions = new Permissions();

        permissions.add(new FilePermission("o:/project/RI_Stack", "read"));
        permissions.add(new FilePermission("o:/project", "execute"));
        permissions.add(new MonitorAppPermission("security"));
        permissions.add(new MonitorAppPermission("registrar"));
        permissions.add(new RuntimePermission("createClassLoader"));
        permissions.add(new RuntimePermission("getClassLoader"));
        permissions.add(new AppsControlPermission());
        permissions.add(new AppsControlPermission("blah", "blah"));

        PermissionInformation pi = createInstance(new AppID(10, 20), permissions);

        // If returns the exact same, we're done
        if (pi.getRequestedPermissions() != permissions)
        {
            PermissionCollection pc = pi.getRequestedPermissions();
            assertNotNull("getRequestedPermissions shouldn't return null", pc);
            verifyPermissionCollection(pc, permissions);
            verifyPermissionCollection(permissions, pc);
        }
    }

    /**
     * Tests getUnsignedAppPermissions().
     */
    public void testGetUnsignedAppPermissions() throws Exception
    {
        PermissionInformation pi = createInstance(new AppID(20, 20), new Permissions());
        PermissionCollection pc = pi.getUnsignedAppPermissions();
        assertNotNull("getUnsignedAppPermissions shouldn't return null", pc);

        // How can we know what it will return....?
        fail("Unfinished test");
    }

    public abstract static class PermissionInformationFactory implements ImplFactory
    {
        public Object createImplObject()
        {
            return this;
        }

        public abstract PermissionInformation createImplObject(AppID id, PermissionCollection perms);
    }

    public PermissionInformationTest(String name, ImplFactory f)
    {
        super(name, PermissionInformation.class, (PermissionInformationFactory) f);
    }

    protected PermissionInformation createInstance(AppID id, PermissionCollection perms)
    {
        return ((PermissionInformationFactory) createImplObject()).createImplObject(id, perms);
    }

    public static InterfaceTestSuite isuite()
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(PermissionInformationTest.class);
        suite.setName(PermissionInformation.class.getName());
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

}
