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

import java.io.FilePermission;
import java.security.AllPermission;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.PropertyPermission;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Base test for PermissionCollection implementations.
 * 
 * @author Aaron Kamienski
 */
public class PermissionCollectionTest extends TestCase
{

    /**
     * Tests constructor. Need not be overridden by subclass tests.
     */
    public void testConstructor()
    {
        PermissionCollection pc = createPermissionCollection();

        // Should imply nothing
        assertFalse("Should imply nothing", pc.implies(createPermissions()[0]));
        assertFalse("Should imply nothing", pc.implies(createAllPermission()));

        // Should contain no permissions
        Enumeration e = pc.elements();
        assertNotNull("Enumeration should not be null", e);
        assertFalse("Enumeration should be empty", e.hasMoreElements());
    }

    /**
     * Ensures that toString() can be called w/out error. Doesn't test much
     * beyond that.
     */
    public void testToString()
    {
        String str = pc.toString();
        assertNotNull("Should return a non-null string", str);
        assertEquals("Expected to return same each time", str, pc.toString());

        pc.add(createPermissions()[0]);
        str = pc.toString();
        assertNotNull("Should return a non-null string", str);
        assertEquals("Expected to return same each time", str, pc.toString());
    }

    /**
     * Tests the ability to add permissions to the collection.
     */
    public void testAdd()
    {
        Permission[] ps = createPermissions();

        for (int i = 0; i < ps.length; ++i)
        {
            assertFalse("Should not imply " + ps[i], pc.implies(ps[i]));
            pc.add(ps[i]);
            assertTrue("Should now imply " + ps[i], pc.implies(ps[i]));
        }
    }

    /**
     * Tests PermissionCollection.implies.
     * 
     * @throws Exception
     */
    public void testImplies() throws Exception
    {
        // Pretty simpl stuff...
        pc.add(createSuperPermission());
        assertTrue("Should imply permission " + createSuperPermission(), pc.implies(createSuperPermission()));
        assertFalse("Should not imply " + createNotPermission(), pc.implies(createNotPermission()));
        pc.add(createAllPermission());
        assertTrue("Should imply all permissions", pc.implies(createNotPermission()));
    }

    /**
     * Tests PermissionCollection.elements().
     * 
     * @throws Exception
     */
    public void testElements() throws Exception
    {
        // empty
        Enumeration e = pc.elements();
        assertNotNull("Enumeration should not be null", e);
        assertFalse("Enumeration should be empty", e.hasMoreElements());
        try
        {
            e.nextElement();
            fail("Expected NoSuchElementException");
        }
        catch (NoSuchElementException x)
        {
        }

        Permission[] ps = createPermissions();
        Hashtable added = new Hashtable();
        // add simple perms
        for (int i = 0; i < ps.length; ++i)
        {
            pc.add(ps[i]);
            added.put(ps[i], ps[i]);
            checkElements(added, pc);
        }
    }

    /**
     * Tests setReadOnly()/isReadOnly().
     */
    public void testReadOnly()
    {
        assertFalse("Collection should not be read-only upon init", pc.isReadOnly());

        // invoke setReadOnly
        pc.setReadOnly();

        assertTrue("Collection should be read-only", pc.isReadOnly());

        // attempt to add permissions
        try
        {
            pc.add(createPermissions()[0]);
            fail("Expected SecurityException when adding permissions to read-only collection");
        }
        catch (SecurityException e)
        {
        }
    }

    /**
     * Test whether pc1 implies pc2.
     * 
     * @param pc1
     * @param pc2
     * @return true if pc1 implies pc2
     */
    public static boolean implies(PermissionCollection pc1, PermissionCollection pc2)
    {
        return implies(pc1, pc2, null);
    }

    /**
     * Test whether pc1 implies pc2, except for the given permission.
     * 
     * @param pc1
     * @param pc2
     * @return true if pc1 implies pc2
     */
    public static boolean implies(PermissionCollection pc1, PermissionCollection pc2, Permission except)
    {
        for (Enumeration e = pc2.elements(); e.hasMoreElements();)
        {
            Permission p = (Permission) e.nextElement();
            if (p.equals(except)) continue;
            if (!pc1.implies(p))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Check whether pc1 implies pc2, except for the given permission.
     * 
     * @param pc1
     * @param pc2
     */
    public static void checkImplies(PermissionCollection pc1, PermissionCollection pc2)
    {
        for (Enumeration e = pc2.elements(); e.hasMoreElements();)
        {
            Permission p = (Permission) e.nextElement();
            assertTrue("Should be implied: " + p + " by " + pc1, pc1.implies(p));
        }
    }

    /**
     * Given a hashtable of permission->permission, tests that only those
     * permissions are present in the collection.
     * 
     * @param added
     *            hashtable of permissions expected to be added
     * @param perms
     *            the collection to which permissions should've been added
     */
    protected void checkElements(Hashtable added, PermissionCollection perms)
    {
        Enumeration e = perms.elements();
        Hashtable elements = new Hashtable();
        for (; e.hasMoreElements();)
        {
            Object p = e.nextElement();
            elements.put(p, p);
        }
        // count on hashtable's equals()!!!
        assertEquals("Expected same permissions returned", added, elements);
        try
        {
            e.nextElement();
            fail("Expected NoSuchElementException");
        }
        catch (NoSuchElementException x)
        {
        }
    }

    /**
     * Adds a set of permissions to the hashtable.
     * 
     * @param h
     *            hashtable to add to
     * @param e
     *            enumeration of permissions
     */
    protected void hashPut(Hashtable h, Enumeration e)
    {
        for (; e.hasMoreElements();)
        {
            Object o = e.nextElement();
            h.put(o, o);
        }
    }

    /**
     * Adds the given permission to the given collections.
     * 
     * @param p1
     *            collection to add to
     * @param p2
     *            collection to add to
     * @param p
     *            permission to add
     */
    protected void add(PermissionCollection p1, PermissionCollection p2, Permission p)
    {
        p1.add(p);
        p2.add(p);
    }

    protected void add(PermissionCollection perms, Permission[] ps)
    {
        for (int i = 0; i < ps.length; ++i)
            perms.add(ps[i]);
    }

    protected void add(PermissionCollection perms, Enumeration e)
    {
        for (; e.hasMoreElements();)
            perms.add((Permission) e.nextElement());
    }

    /**
     * Creates a new PermissionCollection with all the same permissions as is in
     * perms except for the given one.
     * 
     * @param perms
     * @param except
     * @return perms minus except
     */
    protected PermissionCollection createAllBut(PermissionCollection perms, Permission except)
    {
        PermissionCollection pc = new Permissions();
        for (Enumeration e = perms.elements(); e.hasMoreElements();)
        {
            Permission p = (Permission) e.nextElement();
            if (!p.equals(except)) pc.add(p);
        }
        return pc;
    }

    /** The collection under test. */
    protected PermissionCollection pc;

    /**
     * Should be overridden by sub-class tests. This implementation simply
     * returns an instance of <code>Permissions</code>.
     * 
     * @return an instance of <code>PermissionCollection</code> to test
     */
    protected PermissionCollection createPermissionCollection()
    {
        return new Permissions();
    }

    /**
     * Returns an array of permissions that can be added to the type of
     * collection under test. This implementation simply returns a heterogeneous
     * set of permissions.
     * 
     * @return a heterogeneous set of permissions
     */
    protected Permission[] createPermissions()
    {
        return new Permission[] { new PropertyPermission("user.dir", "read"),
                new FilePermission("/snfs/qa", "read,write"), new RuntimePermission("*"), };
    }

    protected Permission createAllPermission()
    {
        return new AllPermission();
    }

    protected Permission createSuperPermission()
    {
        return new PropertyPermission("*", "read");
    }

    protected Permission createSubPermission()
    {
        return new PropertyPermission("user.dir", "read");
    }

    protected Permission createNotPermission()
    {
        return new PropertyPermission("user.dir", "write");
    }

    public void setUp() throws Exception
    {
        super.setUp();
        pc = createPermissionCollection();
    }

    public PermissionCollectionTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(PermissionCollectionTest.class);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }
}
