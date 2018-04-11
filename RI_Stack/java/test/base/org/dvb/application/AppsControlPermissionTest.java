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

package org.dvb.application;

import junit.framework.*;
import java.security.BasicPermission;

/**
 * Tests AppsControlPermission.
 * 
 * @author Aaron Kamienski
 */
public class AppsControlPermissionTest extends TestCase
{
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
        TestSuite suite = new TestSuite(AppsControlPermissionTest.class);
        return suite;
    }

    public AppsControlPermissionTest(String name)
    {
        super(name);
    }

    protected AppsControlPermission permission;

    protected void setUp() throws Exception
    {
        super.setUp();
        permission = new AppsControlPermission("wacky", "ignored");
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     *
     */
    public void testGetName() throws Exception
    {
        assertNotSame("The name is not used and should not be the passed in value", "wacky", permission.getName());
    }

    /**
     * Tests getActions().
     */
    public void testGetActions() throws Exception
    {
        assertNull("The actions are not used and should return null", permission.getActions());
    }

    /**
     * Tests implies().
     */
    public void testImplies() throws Exception
    {
        assertTrue("Permission should imply itself", permission.implies(permission));
        assertTrue("Permission should imply same kind of permission", permission.implies(new AppsControlPermission()));
        assertFalse("Permission should not imply another permission type", permission.implies(new BasicPermission(
                permission.getName())
        {
        }));
    }

    /**
     * Tests equals().
     */
    public void testEquals() throws Exception
    {
        assertEquals("Permission should equal itself", permission, permission);
        assertEquals("Permission should equal same kind of permission", permission, new AppsControlPermission());
        assertFalse("Permission should not equal another permission type", permission.equals(new BasicPermission(
                permission.getName())
        {
        }));
        assertFalse("Permission should not equal null", permission.equals(null));
    }

    /**
     * Tests hashCode().
     */
    public void testHashCode() throws Exception
    {
        assertEquals("HashCode should return same value on successive calls", permission.hashCode(),
                permission.hashCode());
        assertEquals("HashCode should be the same for equivalent permissions", permission.hashCode(),
                (new AppsControlPermission()).hashCode());
        assertFalse("HashCode should be different for different permissions",
                permission.hashCode() == (new BasicPermission(permission.getName())
                {
                }).hashCode());
    }
}
