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

import junit.framework.*;

import java.security.PermissionCollection;
import java.util.Enumeration;

/**
 * Tests the org.dvb.dsmcc.io.persistent.FileAccessPermissions class.
 */
public class OcapIxcPermissionTest extends TestCase
{
    private OcapIxcPermission p1 = new OcapIxcPermission("/*/signed/4C/5abc/Name1", "bind");

    private OcapIxcPermission p2 = new OcapIxcPermission("/service-*/signed/4c/5abc/Name1", "bind");

    private OcapIxcPermission p3 = new OcapIxcPermission("/service-*/signed/4C/5abc/name1", "bind");

    private OcapIxcPermission p4 = new OcapIxcPermission("/service-1234/unsigned/4c/5abc/Name1", "bind");

    private OcapIxcPermission p5 = new OcapIxcPermission("/service-*/unsigned/4C/5abc/Name1", "bind");

    private OcapIxcPermission p6 = new OcapIxcPermission("/service-*/unsigned/4C/5abc/Name1", "lookup");

    private OcapIxcPermission p7 = new OcapIxcPermission("/service-*/unsigned/4C/5abc/Name*", "bind");

    private OcapIxcPermission p8 = new OcapIxcPermission("/service-1234/unsigned/*/5abc/Name1", "bind");

    private OcapIxcPermission p9 = new OcapIxcPermission("/service-1234/unsigned/4c/*/Name1", "bind");

    private OcapIxcPermission p10 = new OcapIxcPermission("/service-1234/*/4c/5abc/Name1", "bind");

    private OcapIxcPermission p11 = new OcapIxcPermission("/service-1234/unsigned/4c/5abc/Name1", "bind");

    private OcapIxcPermission p12 = new OcapIxcPermission("/*/*/*/*/*", "bind");

    private OcapIxcPermission p13 = new OcapIxcPermission("*", "bind");

    private OcapIxcPermission p14 = new OcapIxcPermission("/service-1234/unsigned/4c/5abc/Name1", "bind");

    private OcapIxcPermission p15 = new OcapIxcPermission("/service-1234/unsigned/*/*/Name1", "bind");

    private OcapIxcPermission p16 = new OcapIxcPermission("/service-1234/signed/4c/5abc/Name1", "bind");

    public OcapIxcPermissionTest(String name)
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
        TestSuite suite = new TestSuite(OcapIxcPermissionTest.class);
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

    public void testConstructor()
    {
        try
        {
            new OcapIxcPermission("service-1234/signed/4c/5abc/Name1", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("serv-1234/signed/4c/5abc/Name1", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234/", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234/uns/4c/5abc/Name1", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234/signed", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234/signed/", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234/signed/badID/5abc/Name1", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234/signed/4c", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234/signed/4c/", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234/signed/4c/badID/Name1", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234/signed/4c/5abc", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234/signed/4c/5abc/", "bind");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }

        try
        {
            new OcapIxcPermission("service-1234/signed/4c/5abc/Name1", "badaction");
            fail("Constructor should have thrown exception!");
        }
        catch (Exception e)
        {
        }
    }

    public void testEquals()
    {
        assertFalse("p1 " + printPerm(p1) + " should not be equal to p2 " + printPerm(p2), p1.equals(p2));
        assertFalse("p2 " + printPerm(p2) + " should not be equal to p3 " + printPerm(p3) + " -- bind names not same",
                p2.equals(p3));
        assertFalse("p4 " + printPerm(p4) + " should not be equal to p5 " + printPerm(p5) + " -- services not same",
                p4.equals(p5));
        assertFalse("p5 " + printPerm(p5) + " should not be equal to p6 " + printPerm(p6) + " -- actions not same",
                p5.equals(p6));
        assertFalse("p4 " + printPerm(p4) + " should not be equal to p8 " + printPerm(p8) + " -- orgIDs not same",
                p4.equals(p8));
        assertFalse("p4 " + printPerm(p4) + " should not be equal to p9 " + printPerm(p8) + " -- appIDs not same",
                p4.equals(p9));
        assertFalse("p4 " + printPerm(p4) + " should not be equal to p10 " + printPerm(p10) + " -- signed not same",
                p4.equals(p10));
        assertFalse("p12 " + printPerm(p12) + " should not be equal to p13 " + printPerm(p13), p12.equals(p13));
        assertTrue("p4 " + printPerm(p4) + " should be equal to p11 " + printPerm(p11), p4.equals(p11));
    }

    public void testImplies()
    {
        assertFalse("p3 " + printPerm(p3) + " should not imply p4 " + printPerm(p4) + " -- bind names not same",
                p3.implies(p4));
        assertTrue("p5 " + printPerm(p5) + " should imply p4 " + printPerm(p4), p5.implies(p4));
        assertTrue("p7 " + printPerm(p7) + " should imply p4 " + printPerm(p4), p7.implies(p4));
        assertTrue("p8 " + printPerm(p8) + " should imply p14 " + printPerm(p14), p8.implies(p14));
        assertTrue("p9 " + printPerm(p9) + " should imply p14 " + printPerm(p14), p9.implies(p14));
        assertTrue("p10 " + printPerm(p10) + " should imply p14 " + printPerm(p14), p10.implies(p14));
        assertTrue("p15 " + printPerm(p15) + " should imply p14 " + printPerm(p14), p15.implies(p14));
        assertTrue("p2 " + printPerm(p2) + " should imply p16 " + printPerm(p16), p2.implies(p16));
    }

    public void testPermissionCollection()
    {
        OcapIxcPermission perm;
        PermissionCollection pc;
        Enumeration e;

        // Test that a permission in a collection can be overridden by a more
        // general one
        pc = p1.newPermissionCollection();
        pc.add(p16);
        pc.add(p10);

        e = pc.elements();
        assertTrue("Permission collection should not have zero elements", e.hasMoreElements());
        perm = (OcapIxcPermission) e.nextElement();
        assertTrue("Permission collection should have p10 permission " + printPerm(p10), perm.equals(p10));
        assertFalse("Permission collection should only have one permission", e.hasMoreElements());

        // Test that adding a permission to a collection that already contains
        // a more general permission does not modify the collection
        pc.add(p4);

        e = pc.elements();
        assertTrue("Permission collection should not have zero elements", e.hasMoreElements());
        perm = (OcapIxcPermission) e.nextElement();
        assertTrue("Permission collection should have p10 permission " + printPerm(p10), perm.equals(p10));
        assertFalse("Permission collection should only have one permission", e.hasMoreElements());

        // Test implies
        assertTrue("Permission collection should imply p4 " + printPerm(p4), pc.implies(p4));
    }

    private String printPerm(OcapIxcPermission p)
    {
        return "(" + p.getName() + "," + p.getActions() + ")";
    }
}
