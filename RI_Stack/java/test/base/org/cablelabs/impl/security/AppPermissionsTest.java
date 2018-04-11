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
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.PropertyPermission;

import javax.tv.service.selection.SelectPermission;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.dvb.application.AppsControlPermission;
import org.ocap.service.ServiceTypePermission;
import org.ocap.system.MonitorAppPermission;

/**
 * Tests AppPermissions.
 * 
 * @author Aaron Kamienski
 */
public class AppPermissionsTest extends PermissionCollectionTest
{
    /**
     * Tests <init>(PermissionCollection).
     */
    public void testConstructorCollection()
    {
        // Test with a heterogeneous collection
        PermissionCollection base = new Permissions();

        base.add(new FilePermission("/oc/-", "read"));
        base.add(new PropertyPermission("*", "read"));
        base.add(new SelectPermission("*", "*"));

        AppPermissions perms = new AppPermissions(base);

        // One should imply the other
        assertTrue("Base should imply new AppPermissions", implies(base, perms));
        assertTrue("New AppPermissions should imply base", implies(base, perms));

        // Try again with a homogeneous collection
        AllPermission p = new AllPermission();
        base = p.newPermissionCollection();
        base.add(p);

        perms = new AppPermissions(base);

        // One should imply the other
        assertTrue("Base should imply new AppPermissions", implies(base, perms));
        assertTrue("New AppPermissions should imply base", implies(base, perms));
    }

    /**
     * Tests PermissionCollection.implies() w/ PFC.
     * 
     * @throws Exception
     */
    public void testImpliesPfc() throws Exception
    {
        // test expired pfc
        AppPermissions pc = (AppPermissions) this.pc;

        PersistentFileCredential pfc1 = new PersistentFileCredential();
        pfc1.add(new FilePermission("/itfs/deadbeef/4311/dir/-", "read"));
        pc.add(pfc1);

        assertTrue("Should imply before expiration", pc.implies(new FilePermission("/itfs/deadbeef/4311/dir/dir/x",
                "read")));
        pfc1.setExpiration(new Date(System.currentTimeMillis() - 1000));
        assertFalse("Should not imply after expiration", pc.implies(new FilePermission("/itfs/deadbeef/4311/dir/dir/x",
                "read")));
    }

    /**
     * Tests add(PermissionCollection).
     */
    public void testAddCollection()
    {
        // Add a heterogeneous collection
        PermissionCollection base = new Permissions();
        base.add(new MonitorAppPermission("*"));
        base.add(new AppsControlPermission("*", "*"));
        base.add(new ServiceTypePermission(ServiceTypePermission.BROADCAST, "own"));

        AppPermissions perms = new AppPermissions();

        assertFalse("Should not imply other permissions (yet)", implies(perms, base));

        perms.add(base);
        assertTrue("Base should imply AppPermissions", implies(base, perms));
        assertTrue("AppPermissions should imply base", implies(perms, base));

        // Add a homogeneous collection
        FilePermission p = new FilePermission("/oc/-", "read");
        PermissionCollection base2 = p.newPermissionCollection();
        base2.add(p);
        base2.add(new FilePermission("/oc/*", "read"));

        perms.add(base2);
        assertTrue("AppPermissions should still imply base", implies(perms, base));
        assertTrue("AppPermissions should imply base2", implies(perms, base2));
        assertFalse("AppPermissions should not be implied by base anymore", implies(base, perms));
        assertFalse("AppPermissions should not be implied by base2", implies(base2, perms));
    }

    /**
     * Tests PermissionCollection.elements() w/ PFCs.
     * 
     * @throws Exception
     */
    public void testElementsPFC() throws Exception
    {
        AppPermissions pc = (AppPermissions) createPermissionCollection();
        Hashtable added = new Hashtable();

        // pfcs
        PersistentFileCredential pfc1 = new PersistentFileCredential();
        pfc1.add(new FilePermission("/itfs/beefcafe/4011/*", "read"));
        pfc1.setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000));
        hashPut(added, pfc1.elements());
        pc.add(pfc1);
        checkElements(added, pc);

        // pfcs (expired)
        PersistentFileCredential pfc2 = new PersistentFileCredential();
        pfc2.add(new FilePermission("/itfs/beefcafe/4012/*", "read"));
        Hashtable copy = (Hashtable) added.clone();
        hashPut(copy, pfc2.elements());
        pc.add(pfc2);
        checkElements(copy, pc); // not expired yet

        // Make expired
        pfc2.setExpiration(new Date(System.currentTimeMillis() - 1000));
        checkElements(added, pc); // expired now

        // pcs
        PermissionCollection pcs = new Permissions();
        pcs.add(new PropertyPermission("path.separator", "read"));
        pcs.add(new PropertyPermission("path.*", "write"));
        hashPut(added, pcs.elements());
        pc.add(pcs);
        checkElements(added, pc);

        // pcs overrides pfcs
        PersistentFileCredential pfc3 = new PersistentFileCredential();
        pfc3.add(new FilePermission("/itfs/beefcafe/4013/*", "read"));
        hashPut(added, pfc3.elements());
        pc.add(pfc3);
        // Make expired
        pfc3.setExpiration(new Date(System.currentTimeMillis() - 1000));
        // Should be there anyhow
        pc.add(new FilePermission("/itfs/beefcafe/4013/*", "read"));
        checkElements(added, pc);
    }

    /**
     * Tests add(AppPermissions).
     */
    public void testAddAppPermissions()
    {
        PermissionCollection pc1 = new Permissions();
        pc1.add(new MonitorAppPermission("handler.EAS"));
        pc1.add(new MonitorAppPermission("handler.ClosedCaption"));
        pc1.add(new FilePermission("/oc/-", "read"));

        PersistentFileCredential pfc1 = new PersistentFileCredential();
        pfc1.add(new FilePermission("/itfs/deadbeef/4321/dir/-", "read"));
        pfc1.add(new FilePermission("/itfs/deadbeef/4321/dir/sub/*", "read,write"));
        pfc1.setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000));

        PersistentFileCredential pfc2 = new PersistentFileCredential();
        pfc2.add(new FilePermission("/itfs/deadbeef/4322/dir/-", "read"));
        pfc2.add(new FilePermission("/itfs/deadbeef/4322/dir/*", "read,write"));
        pfc2.setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000));

        AppPermissions base = new AppPermissions();
        base.add(pc1);
        base.add(pfc1);
        base.add(pfc2);

        // Add AppPermissions to perms
        AppPermissions perms = new AppPermissions();
        perms.add(base);

        assertTrue("AppPermissions should imply base", implies(perms, base));
        assertTrue("Base should imply AppPermissions", implies(base, perms));

        // Add another AppPermissions to perms
        AppPermissions base2 = new AppPermissions();
        base2.add(new FilePermission("/itfs/-", "write"));

        assertFalse("AppPermissions should not imply base2", implies(perms, base2));
        perms.add(base2);
        assertTrue("AppPermissions should still imply base", implies(perms, base));
        assertTrue("AppPermissions should imply base2", implies(perms, base2));
        assertFalse("base should no longer imply AppPermissions", implies(base, perms));
    }

    /**
     * Tests add(PersistentFileCredential).
     */
    public void testAddPersistentFileCredential()
    {
        AppPermissions perms = new AppPermissions();

        // Add PFC (non-expired)
        PersistentFileCredential pfc1 = new PersistentFileCredential();
        pfc1.add(new FilePermission("/itfs/deadbeef/4311/dir/-", "read"));
        pfc1.setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000));

        assertFalse("AppPermissions should not imply pfc1", implies(perms, pfc1));
        perms.add(pfc1);
        assertTrue("AppPermissions should imply pfc1", implies(perms, pfc1));
        assertTrue("pfc1 should imply AppPermissions", implies(pfc1, perms));

        // Add PFC (expired)
        PersistentFileCredential pfc2 = new PersistentFileCredential();
        pfc2.add(new FilePermission("/itfs/deadbeef/4311/dir/-", "write"));
        pfc2.setExpiration(new Date(System.currentTimeMillis() - 3600 * 1000));

        assertFalse("AppPermissions should not imply pfc2", implies(perms, pfc2));
        perms.add(pfc2);
        assertFalse("AppPermissions should not imply pfc2", implies(perms, pfc2));

        // Add PFC (non-expired)
        PersistentFileCredential pfc3 = new PersistentFileCredential();
        pfc3.add(new FilePermission("/itfs/deadbeef/4311/dir/-", "write"));
        pfc3.setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000));

        assertFalse("AppPermissions should not imply pfc3", implies(perms, pfc3));
        perms.add(pfc3);
        assertTrue("AppPermissions should imply pfc3", implies(perms, pfc3));
    }

    /**
     * Tests filter().
     * 
     * @throws Exception
     */
    public void testFilter() throws Exception
    {
        PermissionCollection all = new Permissions();

        PermissionCollection unsigned = new Permissions();
        add(unsigned, all, new PropertyPermission("unsigned", "read,write"));
        add(unsigned, all, new SelectPermission("*", "own"));

        PermissionCollection additional = new Permissions();
        add(additional, all, new PropertyPermission("blah", "read"));
        add(additional, all, new FilePermission("/some/dir", "read"));

        PermissionCollection subUnsigned = new Permissions();
        for (Enumeration e = additional.elements(); e.hasMoreElements();)
            subUnsigned.add((Permission) e.nextElement());
        subUnsigned.add(new SelectPermission("*", "own"));

        PermissionCollection unsignedAdditional = new Permissions();
        for (Enumeration e = unsigned.elements(); e.hasMoreElements();)
            unsignedAdditional.add((Permission) e.nextElement());
        for (Enumeration e = additional.elements(); e.hasMoreElements();)
            unsignedAdditional.add((Permission) e.nextElement());

        AppPermissions perms = new AppPermissions();
        perms.add(unsigned);
        perms.add(additional);
        add(perms, all, new FilePermission("/oc/-", "read"));
        add(perms, all, new FilePermission("/itfs/cafecafe/4ef7/public/*", "read"));
        add(perms, all, new FilePermission("/itfs/cafecafe/4ef7/public/-", "read"));

        // not expired
        PersistentFileCredential pfc1 = new PersistentFileCredential();
        add(pfc1, all, new FilePermission("/itfs/cafecafe/4ef7/*", "read"));
        pfc1.setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000));
        perms.add(pfc1);

        // already expired
        PersistentFileCredential pfc2 = new PersistentFileCredential();
        pfc2.add(new FilePermission("/itfs/cafecafe/4ef7/public1/*", "read"));
        pfc2.setExpiration(new Date(System.currentTimeMillis() - 1000));
        perms.add(pfc2);

        // expiring soon
        PersistentFileCredential pfc3 = new PersistentFileCredential();
        pfc3.add(new FilePermission("/itfs/cafecafe/4ef7/public2/*", "read"));
        perms.add(pfc3);
        // Make expired
        pfc3.setExpiration(new Date(System.currentTimeMillis() - 1000));

        // filter against null
        AppPermissions filtered = perms.filter(null, unsigned, additional);
        assertSame("Expected filter=null to return same", perms, filtered);

        // filter against same
        filtered = perms.filter(perms, unsigned, additional);
        assertSame("Expected filter=same to return same", perms, filtered);

        // filter against unsigned -> unsigned+additional
        filtered = perms.filter(unsigned, unsigned, additional);
        assertNotNull("filter should not return null", filtered);
        assertTrue("Filtered by unsigned should imply unsigned+additional", implies(filtered, unsignedAdditional));
        assertTrue("Filtered by unsigned should be implied by unsigned+additional", implies(unsignedAdditional,
                filtered));
        assertFalse("Filtered by unsigned should not imply same", implies(filtered, perms));
        assertTrue("Filtered by unsigned should be implied by same", implies(perms, filtered));

        // filter against <unsigned -> same
        filtered = perms.filter(subUnsigned, unsigned, additional);
        assertSame("Expected filter=<unsigned to return same", perms, filtered);

        // filter against super-set -> same
        AppPermissions superSet = new AppPermissions(perms);
        superSet.add(new AllPermission());
        filtered = perms.filter(superSet, unsigned, additional);
        assertSame("Expected filter=superSet to return same", perms, filtered);

        // filter against subset -> subset
        // - subset of pfc's (non-expired)
        for (Enumeration e = all.elements(); e.hasMoreElements();)
        {
            Permission toRemove = (Permission) e.nextElement();
            PermissionCollection filter = createAllBut(perms, toRemove);

            filtered = perms.filter(filter, unsigned, additional);
            if (unsigned.implies(toRemove))
                assertSame("Expected same returned if filter implies subset of unsigned: " + toRemove, perms, filtered);
            else
            {
                assertTrue("Original should imply filtered", implies(perms, filtered));
                assertTrue("Original should be implied by filtered, except for the removed permission", implies(perms,
                        filtered, toRemove));
            }
        }

        // filter against equivalent
        PermissionCollection filter = createAllBut(perms, null);
        filtered = perms.filter(filter, unsigned, additional);
        assertTrue("Original should imply filtered", implies(perms, filtered));
        assertTrue("filtered should imply original", implies(filtered, perms));

        // Now make sure PFC w/ expiration are copied, even a subset
        PersistentFileCredential pfc4 = new PersistentFileCredential();
        FilePermission except = new FilePermission("/itfs/beefbeef/4444/public/*", "write");
        pfc4.add(new FilePermission("/itfs/beefbeef/4444/public/*", "read"));
        pfc4.add(except);
        pfc4.setExpiration(new Date(System.currentTimeMillis() + 2000));
        perms.add(pfc4);
        filter = createAllBut(perms, except);
        filtered = perms.filter(filter, unsigned, additional);

        // Make sure everything is there...
        assertTrue("Original should imply filtered", implies(perms, filtered));
        assertTrue("Original should be implied by filtered, except for the removed permission", implies(perms,
                filtered, except));

        // Let expire
        Thread.sleep(2100);
        assertTrue("Original should imply filtered", implies(perms, filtered));
        // Filtered should no longer imply others
        for (Enumeration e = pfc4.elements(); e.hasMoreElements();)
        {
            Permission p = (Permission) e.nextElement();
            assertFalse("PFC expiration date was not propogated correctly: " + p, perms.implies(p));
        }
    }

    /**
     * Overrides super implementation.
     * 
     * @see org.cablelabs.impl.security.PermissionCollectionTest#createPermissionCollection()
     */
    protected PermissionCollection createPermissionCollection()
    {
        return new AppPermissions();
    }

    /**
     * Constructor for AppPermissionsTest.
     * 
     * @param name
     */
    public AppPermissionsTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(AppPermissionsTest.class);
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
