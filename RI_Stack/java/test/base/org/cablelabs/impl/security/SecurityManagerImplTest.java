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

/*
 * Created on May 10, 2005
 */
package org.cablelabs.impl.security;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ManagerManagerTest;
import org.cablelabs.impl.manager.application.ApplicationTest.CCMgr;
import org.cablelabs.impl.manager.application.ApplicationTest.DummyContext;
import org.cablelabs.impl.util.MPEEnv;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilePermission;
import java.io.InputStream;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.PropertyPermission;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppID;
import org.dvb.io.persistent.FileAccessPermissions;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.system.MonitorAppPermission;

/**
 * Tests SecurityManagerImpl.
 * 
 * @author Aaron Kamienski
 */
public class SecurityManagerImplTest extends TestCase
{
    /*
     * Class under test for ThreadGroup getThreadGroup()
     */
    public void testGetThreadGroup() throws Exception
    {
        // Make sure it works for "system" context
        ThreadGroup tg = sm.getThreadGroup();
        assertNotNull("Should not return null", tg);
        assertSame("Should return same on repeated calls", tg, sm.getThreadGroup());

        // Make sure get(THREAD_GROUP) is called
        class CC extends DummyContext
        {
            public boolean getThreadGroupCalled;

            public Object get(Object key)
            {
                if (CC.THREAD_GROUP.equals(key)) getThreadGroupCalled = true;
                return super.get(key);
            }
        }
        CC cc = new CC();

        final ThreadGroup tg2[] = { null };
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {
                tg2[0] = sm.getThreadGroup();
            }
        });

        assertNotNull("Should not return null", tg2[0]);
        assertTrue("Should have access CC.get()", cc.getThreadGroupCalled);
        assertEquals("Should have returned the CC's thread group", cc.tg, tg2[0]);

        // Now test that we don't rely on ThreadGroup
        // And make sure that get(THREAD_GROUP) is called
        cc.getThreadGroupCalled = false;
        ccmgr.alwaysReturned = cc;
        ThreadGroup tg3 = sm.getThreadGroup();

        assertNotNull("Should not return null", tg3);
        assertTrue("Should have access CC.get()", cc.getThreadGroupCalled);
        assertEquals("Should have returned the CC's thread group", cc.tg, tg3);
    }

    /*
     * Class under test for void checkPermission(Permission)
     */
    public void testCheckPermission() throws Exception
    {
        PermissionCollection perms = new Permissions();
        perms.add(new FilePermission("/oc/-", "read"));
        perms.add(new PropertyPermission("file.*", "read"));
        perms.add(new RuntimePermission("getProtectionDomain"));
        perms.add(new MonitorAppPermission("properties"));

        Loader loader = new Loader();
        loader.pd = new ProtectionDomain(new CodeSource(null, null), perms);
        loader.addClass(PermissionTesterImpl.class.getName());

        Class testClass = loader.loadClass(PermissionTesterImpl.class.getName());
        PermissionTester test = (PermissionTester) testClass.newInstance();

        // Positive tests
        for (Enumeration e = perms.elements(); e.hasMoreElements();)
        {
            Permission p = (Permission) e.nextElement();
            test.checkPermission(sm, p);
        }

        // Negative tests
        perms = new Permissions();
        perms.add(new FilePermission("/itfs/file", "read"));
        perms.add(new FilePermission("/oc/dir/file", "write"));
        perms.add(new PropertyPermission("file.blah", "write"));
        perms.add(new RuntimePermission("modifyThread"));
        perms.add(new MonitorAppPermission("registeredapi"));
        for (Enumeration e = perms.elements(); e.hasMoreElements();)
        {
            Permission p = (Permission) e.nextElement();
            try
            {
                test.checkPermission(sm, p);
                fail("Expected SecurityException for " + p);
            }
            catch (SecurityException ex)
            {
                // expected
            }
        }
    }

    /*
     * Class under test for void checkPermission(Permission, Object)
     */
    public void testCheckPermission_context() throws Exception
    {
        PermissionCollection perms = new Permissions();
        perms.add(new FilePermission("/oc/-", "read"));
        perms.add(new PropertyPermission("file.*", "read"));
        perms.add(new RuntimePermission("getProtectionDomain"));
        perms.add(new MonitorAppPermission("properties"));

        Loader loader = new Loader();
        loader.pd = new ProtectionDomain(new CodeSource(null, null), perms);
        loader.addClass(PermissionTesterImpl.class.getName());

        Class testClass = loader.loadClass(PermissionTesterImpl.class.getName());
        PermissionTester test = (PermissionTester) testClass.newInstance();

        // Get the security context... and test with it
        Object context = test.getSecurityContext(sm);

        // Positive tests
        for (Enumeration e = perms.elements(); e.hasMoreElements();)
        {
            Permission p = (Permission) e.nextElement();
            sm.checkPermission(p, context);
        }

        // Negative tests
        perms = new Permissions();
        perms.add(new FilePermission("/itfs/file", "read"));
        perms.add(new FilePermission("/oc/dir/file", "write"));
        perms.add(new PropertyPermission("file.blah", "write"));
        perms.add(new RuntimePermission("modifyThread"));
        perms.add(new MonitorAppPermission("registeredapi"));
        for (Enumeration e = perms.elements(); e.hasMoreElements();)
        {
            Permission p = (Permission) e.nextElement();
            try
            {
                sm.checkPermission(p, context);
                fail("Expected SecurityException for " + p);
            }
            catch (SecurityException ex)
            {
                // expected
            }
        }
    }

    /**
     * Tests checkRead().
     */
    public void testCheckRead_access() throws Exception
    {
        // <file/> granted
        // FAP grants access
        // doTestCheck("read", AccessHelper.READ, false, true, true);
    }

    /**
     * Tests checkRead().
     */
    public void testCheckRead_noAccess() throws Exception
    {
        // <file/> granted
        // FAP doesn't grants access
        // doTestCheck("read", AccessHelper.READ, false, false, true);
    }

    /**
     * Tests checkRead().
     */
    public void testCheckRead_context_access() throws Exception
    {
        // <file/> granted
        // FAP grants access
        // doTestCheck("readcontext", AccessHelper.READ, false, true, true);
    }

    /**
     * Tests checkRead().
     */
    public void testCheckRead_context_noAccess() throws Exception
    {
        // <file/> granted
        // FAP doesn't grants access
        // doTestCheck("readcontext", AccessHelper.READ, false, false, true);
    }

    /**
     * Tests checkWrite().
     */
    public void testCheckWrite_access() throws Exception
    {
        // <file/> granted
        // FAP grants access
        // doTestCheck("write", AccessHelper.WRITE, false, true, true);
    }

    /**
     * Tests checkWrite().
     */
    public void testCheckWrite_noAccess() throws Exception
    {
        // <file/> granted
        // FAP doesn't grants access
        // doTestCheck("write", AccessHelper.WRITE, false, false, true);
    }

    /**
     * Tests checkDelete(). Note that FileAccessPermissions are needed for
     * parent directory.
     */
    public void testCheckDelete_access() throws Exception
    {
        // <file/> granted
        // FAP grants access
        // doTestCheck("delete", AccessHelper.WRITE, true, true, true);
    }

    /**
     * Tests checkDelete(). Note that FileAccessPermissions are needed for
     * parent directory.
     */
    public void testCheckDelete_noAccess() throws Exception
    {
        // <file/> granted
        // FAP doesn't grants access
        // doTestCheck("delete", AccessHelper.WRITE, true, false, true);
    }

    /**
     * Tests checkRead(). The caller isn't granted <file/> so it should have
     * access to any persistent files.
     */
    public void testCheckRead_noFile() throws Exception
    {
        // <file/> NOT granted
        // FAP doesn't grants access
        // doTestCheck("read", AccessHelper.READ, false, false, false);
    }

    public void testCheckRead_access_noFile() throws Exception
    {
        // <file/> NOT granted
        // FAP grants access (insufficiently)
        // doTestCheck("read", AccessHelper.READ, false, true, false);
    }

    /**
     * Tests checkRead(). The caller isn't granted <file/> so it should have
     * access to any persistent files.
     */
    public void testCheckRead_context_noFile() throws Exception
    {
        // <file/> NOT granted
        // FAP doesn't grants access
        // doTestCheck("readcontext", AccessHelper.READ, false, false, false);
    }

    public void testCheckRead_context_access_noFile() throws Exception
    {
        // <file/> NOT granted
        // FAP grants access (insufficiently)
        // doTestCheck("readcontext", AccessHelper.READ, false, true, false);
    }

    /**
     * Tests checkWrite(). The caller isn't granted <file/> so it should have
     * access to any persistent files.
     */
    public void testCheckWrite_noFile() throws Exception
    {
        // <file/> NOT granted
        // FAP doesn't grants access
        // doTestCheck("write", AccessHelper.WRITE, false, false, false);
    }

    public void testCheckWrite_access_noFile() throws Exception
    {
        // <file/> NOT granted
        // FAP grants access (insufficiently)
        // doTestCheck("write", AccessHelper.WRITE, false, true, false);
    }

    /**
     * Tests checkDelete(). The caller isn't granted <file/> so it should have
     * access to any persistent files. Note that FileAccessPermissions are
     * needed for parent directory.
     */
    public void testCheckDelete_noFile() throws Exception
    {
        // <file/> NOT granted
        // FAP doesn't grants access
        // doTestCheck("delete", AccessHelper.WRITE, true, false, false);
    }

    public void testCheckDelete_access_noFile() throws Exception
    {
        // <file/> NOT granted
        // FAP grants access (insufficiently)
        // doTestCheck("delete", AccessHelper.WRITE, true, true, false);
    }

    /**
     * Implements test of check[Read|Write|Delete].
     * 
     * @param type
     *            "read", "write", "delete", "readcontext"
     * @param accessHelper
     *            implementation strategy for testing file access permissions
     * @param delete
     *            true if implementing test of checkDelete()
     * @param hasAccess
     *            true if testing whether access is expected
     * @param hasFilePerm
     *            whether &lt;file/&gt; access is to be expected (overrides
     *            hasAccess)
     * @throws Exception
     */
    /*
     * private void doTestCheck(final String type, final AccessHelper
     * accessHelper, boolean delete, boolean hasAccess, boolean hasFilePerm)
     * throws Exception { AppID caller = new AppID(0xc001d00d, 0x4515);
     * PermissionCollection perms = new Permissions(); perms.add(new
     * FilePermission("/oc/-", "read")); if ( hasFilePerm ) { // Give <file/>...
     * String root = MPEEnv.getSystemProperty("dvb.persistent.root"); if (
     * !root.endsWith("/") ) root = root + "/"; root = root +
     * Integer.toHexString(caller.getOID())+"/"; perms.add(new
     * FilePermission(root + "*", "read,write,delete")); root = root +
     * Integer.toHexString(caller.getAID())+"/"; perms.add(new
     * FilePermission(root + "-", "read,write,delete")); }
     * 
     * Loader loader = new Loader(); loader.pd = new ProtectionDomain(new
     * CodeSource(null, null), perms);
     * loader.addClass(PermissionTesterImpl.class.getName());
     * 
     * Class testClass = loader.loadClass(PermissionTesterImpl.class.getName());
     * final PermissionTester test = (PermissionTester)testClass.newInstance();
     * 
     * String root = MPEEnv.getSystemProperty("dvb.persistent.root"); if (
     * !root.endsWith(File.separator) ) root = root + File.separator;
     * 
     * doTestCheck(root + "beefcafe/4dad/some/dir/file.txt", hasAccess, delete,
     * accessHelper, new CheckHelper() { void check(SecurityManager sm, String
     * name) { test.checkFile(sm, name, type); } }, hasFilePerm, caller); }
     */

    /**
     * Implement testing of check[Read|Write|Delete]. SecurityManagerImpl is
     * subclassed w/ an implementation that overrides <code>getAppID()</code>
     * and <code>hasFileAccess()</code>.
     * <p>
     * Tests params to getFileAccessPerms() and hasFileAccess().
     * 
     * @param name
     *            filename
     * @param hasAccess
     *            whether access is to be expected
     * @param parent
     *            whether permissions of parent dir are tested
     * @param accessHelper
     *            strategy used by implementation to query read/write perms on
     *            FAP
     * @param helper
     *            strategy used by test to invoke checkRead/checkWrite/etc
     * @param hasFilePerm
     *            whether &lt;file/&gt; access is to be expected (overrides
     *            hasAccess)
     */
    /*
     * private void doTestCheck(String name, final boolean hasAccess, boolean
     * parent, AccessHelper accessHelper, CheckHelper helper, boolean
     * hasFilePerm, final AppID caller) { final FileAccessPermissions fap = new
     * FileAccessPermissions(true, true, true, true, true, true); class
     * CheckFileSM extends SecurityManagerImpl { public boolean getAppIDCalled;
     * public boolean getFileAccessPermsCalled; public boolean lastFapParent;
     * public String lastFapName; public String lastName; public AccessHelper
     * lastHelper; public FileAccessPermissions lastFap; public AppID lastId;
     * public boolean hasFileAccessCalled;
     * 
     * // overridden so we can control who the caller appears to be AppID
     * getAppID(CallerContext ctx) { getAppIDCalled = true; return caller; } //
     * overridden so we can control what FAP are returned; and test parms
     * FileAccessPermissions getFileAccessPerms(String name, boolean parent) {
     * getFileAccessPermsCalled = true; lastFapParent = parent; lastFapName =
     * name; return fap; } // overridden so we can control what is returned; and
     * test parms boolean hasFileAccess(String name, AccessHelper helper,
     * FileAccessPermissions fap, AppID id) { hasFileAccessCalled = true;
     * lastName = name; lastHelper = helper; lastFap = fap; lastId = id; return
     * hasAccess; } } CheckFileSM csm = new CheckFileSM(); sm = csm;
     * 
     * try { // Invoke SecurityManager.check[Read|Write|Delete] helper.check(sm,
     * name); assertTrue("Expected exception if not granted <file/>",
     * hasFilePerm); assertTrue("Expected exception if !hasAccess", hasAccess);
     * } catch(SecurityException e) { // Only expect exception if !hasFilePerm
     * || (hasFilePerm && !hasAccess) if ( hasFilePerm )
     * assertFalse("Expected no exception if hasAccess", hasAccess); }
     * assertTrue("Expected getAppID called", csm.getAppIDCalled); if (
     * hasFilePerm ) { assertTrue("Expected getFileAccessPerms called",
     * csm.getFileAccessPermsCalled);
     * assertTrue("Expected hasFileAccess called", csm.hasFileAccessCalled);
     * 
     * assertEquals("Unexpected name", name, csm.lastFapName);
     * assertEquals("Unexpected parent", parent, csm.lastFapParent);
     * 
     * assertEquals("Unexpected name", name, csm.lastName);
     * assertSame("Unexpected fap", fap, csm.lastFap);
     * assertEquals("Unexpected helper", accessHelper, csm.lastHelper);
     * assertEquals("Unexpected id", caller, csm.lastId); } else {
     * assertFalse("Expected getFileAccessPerms NOT to be called (w/out <file/>)"
     * , csm.getFileAccessPermsCalled);
     * assertFalse("Expected hasFileAccess NOT to be called (w/out <file/>",
     * csm.hasFileAccessCalled); } }
     */

    private abstract class CheckHelper
    {
        abstract void check(SecurityManager sm, String value);
    }

    /**
     * Tests hasFileAccess() w/ a FAP with world read or write access.
     */
    public void testHasFileAccess_World()
    {
        doTestHasFileAccess(new FileAccessPermissions(true, false, false, false, false, false));
        doTestHasFileAccess(new FileAccessPermissions(false, true, false, false, false, false));
    }

    /**
     * Tests hasFileAccess() w/ a FAP with organization read or write access.
     */
    public void testHasFileAccess_Org()
    {
        doTestHasFileAccess(new FileAccessPermissions(false, false, true, false, false, false));
        doTestHasFileAccess(new FileAccessPermissions(false, false, false, true, false, false));
    }

    /**
     * Tests hasFileAccess() w/ a FAP with application (owner) read or write
     * access.
     */
    public void testHasFileAccess_App()
    {
        doTestHasFileAccess(new FileAccessPermissions(false, false, false, false, true, false));
        doTestHasFileAccess(new FileAccessPermissions(false, false, false, false, false, true));
    }

    /**
     * Tests hasFileAccess() w/ a FAP with other organization read or write
     * access.
     */
    public void testHasFileAccess_Other()
    {
        int[] empty = new int[0];
        int[] none = new int[] { 0xbeef, 0xcafe, 0xd00d };
        int[] oid1 = new int[] { 0xbeefbeef, 0xcafebeef, 0xcafecafe };
        int[] oid2 = new int[] { 0xbeefbeef, 0xd00dd00d, 0xcafecafe };

        doTestHasFileAccess(new ExtendedFileAccessPermissions(false, false, false, false, false, false, empty, empty));
        doTestHasFileAccess(new ExtendedFileAccessPermissions(false, false, false, false, false, false, none, none));
        doTestHasFileAccess(new ExtendedFileAccessPermissions(false, false, false, false, false, false, oid2, none));
        doTestHasFileAccess(new ExtendedFileAccessPermissions(false, false, false, false, false, false, none, oid2));
        doTestHasFileAccess(new ExtendedFileAccessPermissions(false, false, false, false, false, false, oid1, oid1));
        doTestHasFileAccess(new ExtendedFileAccessPermissions(false, false, false, false, false, false, oid2, oid1));
        doTestHasFileAccess(new ExtendedFileAccessPermissions(false, false, false, false, false, false, oid1, oid2));
        doTestHasFileAccess(new ExtendedFileAccessPermissions(false, false, false, false, false, false, oid2, oid2));
    }

    /**
     * Implements the testHasFileAccess_* methods. Tests the given FAP with the
     * following combinations of owner/caller:
     * <ul>
     * <li>null owner
     * <li>owner and caller are same
     * <li>owner and caller have same org
     * <li>owner and caller same based on wildcard
     * <li>owner and caller are different
     * </ul>
     * 
     * @param fap
     *            file access permissions to use during test
     * @see #doTestHasFileAccess(String, AppID, AppID, FileAccessPermissions)
     */
    private void doTestHasFileAccess(FileAccessPermissions fap)
    {
        // No owner -- only world or other should be considered
        doTestHasFileAccess("no-owner", null, new AppID(0xd00dd00d, 0x4dad), fap);

        // Same owner -- only owner should be considered
        doTestHasFileAccess("same-owner", new AppID(0xcafebeef, 0x4dad), new AppID(0xcafebeef, 0x4dad), fap);

        // Same org -- only org should be considered
        doTestHasFileAccess("same-org", new AppID(0xcafebeef, 0x4dad), new AppID(0xcafebeef, 0x5000), fap);

        // Same owner (by wildcard) -- only owner should be considered
        doTestHasFileAccess("same-wild", new AppID(0xcafebeef, 0xFFFF), new AppID(0xcafebeef, 0x5000), fap);

        // Absolutely different -- only world or other should be considered
        doTestHasFileAccess("diff", new AppID(0xcafebeef, 0x4dad), new AppID(0xd00dd00d, 0x5432), fap);
    }

    /**
     * Implements doTestHasFileAccess with the given caller/owner and FAP. Makes
     * two test runs: one testing read access and the other testing write
     * access.
     * 
     * @param msg
     *            describes parameters
     * @param owner
     * @param caller
     * @param fap
     * 
     * @see #doTestHasFileAccess(String, AppID, AppID, FileAccessPermissions,
     *      AccessHelper)
     */
    private void doTestHasFileAccess(String msg, final AppID owner, final AppID caller, FileAccessPermissions fap)
    {
        // doTestHasFileAccess(msg+" read", owner, caller, fap,
        // AccessHelper.READ);
        // doTestHasFileAccess(msg+" write", owner, caller, fap,
        // AccessHelper.WRITE);
    }

    /**
     * Tests hasFileAccess() using the given parameters. This invokes
     * {@link #expectedAccess} to determine what the test expects should be the
     * result.
     * 
     * @param msg
     *            describes params
     * @param owner
     *            owner AppID (if null, then expects no access -- ever!)
     * @param caller
     *            caller AppID
     * @param fap
     *            file access permissions given to file to access
     * @param helper
     *            indicates what on the fap the impl will test
     */
    /*
     * private void doTestHasFileAccess(String msg, final AppID owner, final
     * AppID caller, FileAccessPermissions fap, AccessHelper helper) { //
     * Override getFileOwner() so we don't have to have a real filename... //
     * Leave testing of it until later. sm = new SecurityManagerImpl() { AppID
     * getFileOwner(String file) { return owner; } };
     * 
     * boolean access = sm.hasFileAccess("some/file", helper, fap, caller);
     * 
     * if ( owner == null )
     * assertFalse(msg+": Expected no if there is no owner", access); else
     * assertEquals
     * (msg+": Expected for owner="+owner+" caller="+caller+" to "+toString
     * (fap), expectedAccess(owner, fap, caller, helper == AccessHelper.READ),
     * access); }
     */

    /**
     * Represent FAP as a string.
     * 
     * @param fap
     *            fap to convert
     * @return string representation of FAP
     */
    private String toString(FileAccessPermissions fap)
    {
        String className = fap.getClass().getName();
        int idx = className.lastIndexOf('.');
        if (idx >= 0) className = className.substring(idx + 1);
        StringBuffer sb = new StringBuffer(className);

        sb.append(':');

        sb.append(fap.hasReadApplicationAccessRight() ? 'r' : '-');
        sb.append(fap.hasWriteApplicationAccessRight() ? 'w' : '-');
        sb.append(fap.hasReadOrganisationAccessRight() ? 'r' : '-');
        sb.append(fap.hasWriteOrganisationAccessRight() ? 'w' : '-');
        sb.append(fap.hasReadWorldAccessRight() ? 'r' : '-');
        sb.append(fap.hasWriteWorldAccessRight() ? 'w' : '-');

        if (fap instanceof ExtendedFileAccessPermissions)
        {
            ExtendedFileAccessPermissions xfap = (ExtendedFileAccessPermissions) fap;

            sb.append(",r=[");
            int[] oid = xfap.getReadAccessOrganizationIds();
            for (int i = 0; i < oid.length; ++i)
                sb.append(Integer.toHexString(oid[i])).append(',');
            sb.append("],w=[");
            oid = xfap.getWriteAccessOrganizationIds();
            for (int i = 0; i < oid.length; ++i)
                sb.append(Integer.toHexString(oid[i])).append(',');
            sb.append(']');
        }

        return sb.toString();
    }

    /**
     * If true, then access to files is granted if any of the applicable access
     * rights are specified on the FAP (see MHP 12.6.2.7.2). If false, then only
     * the most specific applicable access right applies (see OCAP 14.2.2.8).
     */
    private static final boolean OR_ACCESS = true;

    /**
     * Tests whether the caller has rights to a file owned by the given owner,
     * with the given file access permissions.
     * 
     * @param owner
     *            owner AppID
     * @param fap
     *            file access permissions
     * @param caller
     *            caller AppID
     * @param read
     *            test read if true; test write if false
     * @return if the caller has access
     */
    private boolean expectedAccess(AppID owner, FileAccessPermissions fap, AppID caller, boolean read)
    {
        if (OR_ACCESS)
        {
            if (caller.equals(owner) || (owner.getAID() == 0xFFFF && owner.getOID() == caller.getOID()))
                if (read ? fap.hasReadApplicationAccessRight() : fap.hasWriteApplicationAccessRight()) return true;
            if (caller.getOID() == owner.getOID())
                if (read ? fap.hasReadOrganisationAccessRight() : fap.hasWriteOrganisationAccessRight()) return true;

            if (fap instanceof ExtendedFileAccessPermissions)
            {
                ExtendedFileAccessPermissions xfap = (ExtendedFileAccessPermissions) fap;
                int[] oids = read ? xfap.getReadAccessOrganizationIds() : xfap.getWriteAccessOrganizationIds();

                for (int i = 0; i < oids.length; ++i)
                    if (oids[i] == caller.getOID()) return true;
            }
            return read ? fap.hasReadWorldAccessRight() : fap.hasWriteWorldAccessRight();
        }
        else
        {
            if (caller.equals(owner) || (owner.getAID() == 0xFFFF && owner.getOID() == caller.getOID()))
                return read ? fap.hasReadApplicationAccessRight() : fap.hasWriteApplicationAccessRight();
            if (caller.getOID() == owner.getOID())
                return read ? fap.hasReadOrganisationAccessRight() : fap.hasWriteOrganisationAccessRight();

            if (fap instanceof ExtendedFileAccessPermissions)
            {
                ExtendedFileAccessPermissions xfap = (ExtendedFileAccessPermissions) fap;
                int[] oids = read ? xfap.getReadAccessOrganizationIds() : xfap.getWriteAccessOrganizationIds();

                for (int i = 0; i < oids.length; ++i)
                    if (oids[i] == caller.getOID()) return true;
            }
            return read ? fap.hasReadWorldAccessRight() : fap.hasWriteWorldAccessRight();
        }
    }

    /**
     * Tests getFileOwner(). Test with a precise owner and a file in the owner's
     * root.
     */
    public void testGetFileOwner_exact_fileInDir() throws Exception
    {
        String root = MPEEnv.getSystemProperty("dvb.persistent.root");
        checkOwner(root, "cafe/4dad/blah.txt", new AppID(0xcafe, 0x4dad));
    }

    /**
     * Tests getFileOwner(). Test with a precise owner and a file in a subdir.
     */
    public void testGetFileOwner_exact_fileInSubDir() throws Exception
    {
        String root = MPEEnv.getSystemProperty("dvb.persistent.root");
        checkOwner(root, "cafe/4dad/sub/dir/blah.txt", new AppID(0xcafe, 0x4dad));
    }

    /**
     * Tests getFileOwner(). Test with a precise owner, testing the owner's
     * directory.
     */
    public void testGetFileOwner_exact_dir() throws Exception
    {
        String root = MPEEnv.getSystemProperty("dvb.persistent.root");
        checkOwner(root, "cafe/4dad", new AppID(0xcafe, 0x4dad));
    }

    /**
     * Tests getFileOwner(). Test with a file in the organization directory.
     */
    public void testGetFileOwner_group_fileInDir() throws Exception
    {
        String root = MPEEnv.getSystemProperty("dvb.persistent.root");

        // Check "group" ownership
        AppID groupId = new AppID(0xcafe, 0xFFFF);
        checkOwner(root, "cafe/blah.txt", groupId);
    }

    /**
     * Tests getFileOwner(). Test with a file in a subdir of the organization
     * directory.
     */
    public void testGetFileOwner_group_fileInSubDir() throws Exception
    {
        String root = MPEEnv.getSystemProperty("dvb.persistent.root");

        // Check "group" ownership
        AppID groupId = new AppID(0xcafe, 0xFFFF);
        checkOwner(root, "cafe/sub/dir/blah.txt", groupId);
    }

    /**
     * Tests getFileOwner(). Test with the organization directory itself.
     */
    public void testGetFileOwner_group_dir() throws Exception
    {
        String root = MPEEnv.getSystemProperty("dvb.persistent.root");

        // Check "group" ownership
        AppID groupId = new AppID(0xcafe, 0xFFFF);
        checkOwner(root, "cafe", groupId);
    }

    /**
     * Tests getFileOwner(). Tests files that don't have an owner.
     */
    public void testGetFileOwner_none() throws Exception
    {
        String root = MPEEnv.getSystemProperty("dvb.persistent.root");

        // Check no ownership
        String[] names_none = { "blah.txt", "sub/dir/blah.txt" };
        for (int i = 0; i < names_none.length; ++i)
            checkOwner(root, names_none[i], null);

        // Check no ownership - under other root
        String[] names_owned = { "cafe/4dad/blah.txt", "cafe/4dad/sub/dir/blah.txt", "cafe/4dad", };
        for (int i = 0; i < names_owned.length; ++i)
            checkOwner("/blah", names_owned[i], null);
    }

    /**
     * Used to implement tests of
     * {@link SecurityManagerImpl#getFileOwner(String)}.
     * 
     * @param prefix
     *            root directory
     * @param path
     *            path relative to prefix
     * @param id
     *            caller AppID
     * @throws Exception
     */
    private void checkOwner(String prefix, String path, AppID id) throws Exception
    {
        File root = new File(prefix);
        File file = new File(root, path);
        String name = file.getCanonicalPath();

        /*
         * AppID owner = sm.getFileOwner(name);
         * 
         * if ( id == null )
         * assertSame("Expected no owner to be returned for \""+name+"\"", id,
         * owner); else
         * assertEquals("Unexpected owner returned for \""+name+"\"", id,
         * owner);
         */
    }

    /*
     * Class under test for Object getSecurityContext()
     */
    public void testGetSecurityContext() throws Exception
    {
        Object context = sm.getSecurityContext();
        assertNotNull("SecurityContext should not be null", context);
        assertEquals("SecurityContext should equals self", context, context);
        assertEquals("SecurityContext should equal same", context, sm.getSecurityContext());

        DummyContext cc = new DummyContext();
        final Object[] context2 = { null, null };
        cc.runInContextSync(new Runnable()
        {
            public void run()
            {
                context2[0] = sm.getSecurityContext();
                context2[1] = sm.getSecurityContext();
            }
        });
        assertNotNull("SecurityContext should not be null", context2[0]);
        assertNotNull("SecurityContext should not be null", context2[1]);
        assertEquals("SecurityContext should equals self", context2[0], context2[0]);
        assertEquals("SecurityContext should equal same", context2[0], context2[1]);

        assertFalse("SecurityContexts for different CallerContexts should not be equal", context.equals(context2[0]));
        assertFalse("SecurityContexts for different CallerContexts should not be equal", context2[0].equals(context));
    }

    /**
     * Interface used to test permissions. Implemented by PermissionTesterImpl.
     * 
     * @author Aaron Kamienski
     */
    public static interface PermissionTester
    {
        public void checkPermission(SecurityManager sm, Permission p) throws SecurityException;

        public Object getSecurityContext(SecurityManager sm);

        public void checkFile(SecurityManager sm, String name, String type);
    }

    /**
     * This class will be loaded by a specialized ClassLoader (an instance of
     * Loader). This will be used so that we can give the class specific
     * permissions so that we can test checkPermission().
     * 
     * @author Aaron Kamienski
     */
    public static class PermissionTesterImpl implements PermissionTester
    {
        public void checkPermission(SecurityManager sm, Permission p) throws SecurityException
        {
            sm.checkPermission(p);
        }

        public Object getSecurityContext(SecurityManager sm)
        {
            return sm.getSecurityContext();
        }

        public void checkFile(SecurityManager sm, String name, String type)
        {
            if ("read".equals(type))
                sm.checkRead(name);
            else if ("write".equals(type))
                sm.checkWrite(name);
            else if ("delete".equals(type))
                sm.checkDelete(name);
            else if ("readcontext".equals(type))
                sm.checkRead(name, sm.getSecurityContext());
            else
                fail("Internal error - unknown type: " + type);
        }
    }

    /**
     * A specialized class loader that can be used to load classes and given
     * them the permissions that we desire. The permissions that we desire are
     * granted via the ProtectionDomain assigned with set ProtectionDomain.
     * 
     * @author Aaron Kamienski
     */
    public static class Loader extends SecureClassLoader
    {
        public Hashtable classNames = new Hashtable();

        private Hashtable classes = new Hashtable();

        public ProtectionDomain pd = getClass().getProtectionDomain();

        public void setProtectionDomain(ProtectionDomain pd)
        {
            this.pd = pd;
        }

        public void addClass(String className)
        {
            classNames.put(className, className);
        }

        /**
         * Overrides ClassLoader.loadClass() to not go to the parent for certain
         * classes, possibly loading a secondary copy.
         * 
         * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
         */
        protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
        {
            if (classNames.get(name) == null)
            {
                return super.loadClass(name, resolve);
            }
            else
            {
                Class found = (Class) classes.get(name);
                if (found != null) return found;
                found = loadMyClass(name, resolve);
                classes.put(name, found);
                return found;
            }
        }

        private Class loadMyClass(String name, boolean resolve) throws ClassNotFoundException
        {
            byte[] classBytes = null;

            try
            {
                String rezName = name.replace('.', '/') + ".class";
                InputStream is = getResourceAsStream(rezName);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int bytes = 0;

                while ((bytes = is.read(buf)) > 0)
                {
                    bos.write(buf, 0, bytes);
                }
                classBytes = bos.toByteArray();
            }
            catch (Exception e)
            {
                throw new ClassNotFoundException(e.getMessage());
            }

            return defineClass(name, classBytes, 0, classBytes.length, pd);
        }
    }

    private CallerContextManager save;

    private CCMgr ccmgr;

    private void replaceCCMgr()
    {
        save = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ManagerManagerTest.updateManager(CallerContextManager.class, CCMgr.class, false, ccmgr = new CCMgr(save));
    }

    private void restoreCCMgr()
    {
        if (save != null) ManagerManagerTest.updateManager(CallerContextManager.class, save.getClass(), true, save);
    }

    protected SecurityManagerImpl sm;

    protected void setUp() throws Exception
    {
        super.setUp();

        replaceCCMgr();
        sm = new SecurityManagerImpl();
    }

    protected void tearDown() throws Exception
    {
        restoreCCMgr();
        super.tearDown();
    }

    public SecurityManagerImplTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(SecurityManagerImplTest.class);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

}
