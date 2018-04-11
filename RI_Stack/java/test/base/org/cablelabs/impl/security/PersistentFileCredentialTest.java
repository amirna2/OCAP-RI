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
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.Date;
import java.util.PropertyPermission;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests PersistentFileCredential.
 * 
 * @author Aaron Kamienski
 */
public class PersistentFileCredentialTest extends PermissionCollectionTest
{
    /**
     * Tests that only FilePermissions can be add to PRF.
     */
    public void testAddFilePermissionOnly()
    {
        try
        {
            pc.add(new PropertyPermission("user.dir", "read"));
            fail("Expected IllegalArgumentException for non-FilePermission");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    /**
     * Tests permissions are still implied, even after expiration.
     * 
     * @throws Exception
     */
    public void testImpliesAfterExpiration() throws Exception
    {
        super.testImplies();

        pc = createPermissionCollection();
        PersistentFileCredential pfc = (PersistentFileCredential) pc;
        pfc.setExpiration(new Date(System.currentTimeMillis() + 500));
        Thread.sleep(600);
        super.testImplies();
    }

    /**
     * Tests setExpiration()/isExpired().
     */
    public void testExpiration()
    {
        PersistentFileCredential pfc = (PersistentFileCredential) pc;

        assertSame("Should have no expiration date set", null, pfc.getExpiration());
        assertFalse("Should not be expired if no date is set", pfc.isExpired());

        pfc.setExpiration(new Date(System.currentTimeMillis() + 1000));
        assertFalse("Should not be expired", pfc.isExpired());

        pfc.setExpiration(new Date(System.currentTimeMillis() - 1));
        assertTrue("Should be expired", pfc.isExpired());
    }

    /**
     * Tests filter().
     */
    public void testFilter()
    {
        PermissionCollection all = new Permissions();

        PermissionCollection subset1 = new Permissions();
        PermissionCollection subset2 = new Permissions();

        PersistentFileCredential pfc = (PersistentFileCredential) pc;
        pfc.setExpiration(new Date(System.currentTimeMillis() + 1000 * 3600));

        add(subset1, all, new FilePermission("/itfs/cafecafe/4df7/public/*", "read,write"));
        add(subset1, all, new FilePermission("/itfs/cafecafe/4df7/public/-", "read"));
        add(subset2, all, new FilePermission("/itfs/cafecafe/4df7/*", "read"));
        all.add(new FilePermission("/itfs/cafecafe/4df7/public2/*", "read"));
        all.add(new PropertyPermission("user.dir", "read"));
        add(pfc, subset1.elements());
        add(pfc, subset2.elements());

        // filter against same
        PersistentFileCredential filtered = pfc.filter(pfc);
        assertTrue("Filtered by self, should be implied by self", implies(pfc, filtered));
        assertTrue("Filtered by self should imply self", implies(filtered, pfc));
        assertEquals("Expected same expiration after filter", pfc.getExpiration(), filtered.getExpiration());

        // filter against all
        assertNotNull("filter should not return null", filtered);
        assertEquals("Expected same expiration after filter", pfc.getExpiration(), filtered.getExpiration());
        assertTrue("Filtered by more than self, should imply self", implies(filtered, pfc));
        assertTrue("Filtered by more than self, should imply self", implies(pfc, filtered));
        assertTrue("Filtered should be implied by filter", implies(all, filtered));

        // filter against subset -> subset
        // - subset of pfc's (non-expired)
        filtered = pfc.filter(subset1);
        assertNotNull("filter should not return null", filtered);
        assertEquals("Expected same expiration after filter", pfc.getExpiration(), filtered.getExpiration());
        assertTrue("Filtered by subset should imply subset", implies(filtered, subset1));
        assertTrue("Filtered by subset should imply subset", implies(subset1, filtered));
        assertTrue("Filtered should be implied by original", implies(pfc, subset1));
        assertFalse("Filtered should not be implied by other subset", implies(subset2, filtered));

        // filter against equivalent
        PermissionCollection filter = createAllBut(pfc, null);
        filtered = pfc.filter(filter);
        assertNotNull("filter should not return null", filtered);
        assertEquals("Expected same expiration after filter", pfc.getExpiration(), filtered.getExpiration());
        assertTrue("Original should imply filtered", implies(pfc, filtered));
        assertTrue("filtered should imply original", implies(filtered, pfc));

        // Now filter expired PFC
        pfc.setExpiration(new Date(System.currentTimeMillis() - 100));
        assertSame("Expect null for expired filter", null, pfc.filter(null));
        assertSame("Expect null for expired filter", null, pfc.filter(pfc));
        assertSame("Expect null for expired filter", null, pfc.filter(all));
        assertSame("Expect null for expired filter", null, pfc.filter(subset1));
    }

    /* =============boilerplate===================== */

    /**
     * Overrides super implementation.
     * 
     * @see org.cablelabs.impl.security.PermissionCollectionTest#createPermissionCollection()
     */
    protected PermissionCollection createPermissionCollection()
    {
        return new PersistentFileCredential();
    }

    /**
     * Overrides super implementation.
     * 
     * @see org.cablelabs.impl.security.PermissionCollectionTest#createPermissions()
     */
    protected Permission[] createPermissions()
    {
        return new FilePermission[] { new FilePermission("/itfs/feedcafe/4321/file1.txt", "read"),
                new FilePermission("/itfs/feedcafe/4321/file2.txt", "read"),
                new FilePermission("/itfs/feedcafe/4322/PUBLIC/*", "read,write"),
                new FilePermission("/itfs/feedcafe/4322/PUBLIC/-", "read") };
    }

    /**
     * Overrides super implementation.
     * 
     * @see org.cablelabs.impl.security.PermissionCollectionTest#createSubPermission()
     */
    protected Permission createSubPermission()
    {
        return new FilePermission("/itfs/beefcafe/5de1/dir/file.txt", "read");
    }

    /**
     * Overrides super implementation.
     * 
     * @see org.cablelabs.impl.security.PermissionCollectionTest#createNotPermission()
     */
    protected Permission createNotPermission()
    {
        return new FilePermission("/itfs/beefcafe/5de1/dir/file.txt", "read,write");
    }

    /**
     * Overrides super implementation.
     * 
     * @see org.cablelabs.impl.security.PermissionCollectionTest#createSuperPermission()
     */
    protected Permission createSuperPermission()
    {
        return new FilePermission("/itfs/beefcafe/5de1/-", "read");
    }

    /**
     * Overrides super implementation.
     * 
     * @see org.cablelabs.impl.security.PermissionCollectionTest#createAllPermission()
     */
    protected Permission createAllPermission()
    {
        return new FilePermission("<<ALL FILES>>", "read,write,delete,execute");
    }

    /**
     * Constructor for PersistentFileCredentialTest.
     * 
     * @param name
     */
    public PersistentFileCredentialTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(PersistentFileCredentialTest.class);
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
