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

import org.cablelabs.impl.manager.AuthManager.AuthInfo;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.MPEEnv;

import java.awt.AWTPermission;
import java.io.FilePermission;
import java.io.IOException;
import java.io.SerializablePermission;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.net.SocketPermission;
import java.net.URL;
import java.security.AllPermission;
import java.security.BasicPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.PropertyPermission;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.tv.media.MediaSelectPermission;
import javax.tv.service.ReadPermission;
import javax.tv.service.selection.SelectPermission;
import javax.tv.service.selection.ServiceContextPermission;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.dvb.application.AppID;
import org.dvb.application.AppsControlPermission;
import org.dvb.media.DripFeedPermission;
import org.dvb.net.tuning.TunerPermission;
import org.dvb.user.UserPreferencePermission;
import org.ocap.application.PermissionInformation;
import org.ocap.application.SecurityPolicyHandler;
import org.ocap.service.ServiceTypePermission;
import org.ocap.system.MonitorAppPermission;

// TODO(AaronK): refactor getPermission() tests to a parameterized test
// TODO(AaronK): don't rely on AppCodeSource impl to disable caching of PermissionCollections 

/**
 * Tests PolicyImpl.
 * 
 * @author Aaron Kamienski
 */
public class PolicyImplTest extends TestCase
{
    /**
     * Tests constructor. Just a smoke-test really.
     */
    public void testConstructor()
    {
        // Just a smoke-test really...
        Policy p = new PolicyImpl(null);
        p.getPermissions(new CodeSource(null, null));
        p = new PolicyImpl(p, null);
        p.getPermissions(new CodeSource(null, null));
        p = new PolicyImpl(p, new Vector());
        p.getPermissions(new CodeSource(null, null));
    }

    /**
     * Tests <code>refresh()</code>. Should pass the request along to the
     * basePolicy.
     */
    public void testRefresh()
    {
        basePolicy.clear();

        policy.refresh();
        assertTrue("Expected to pass along to basePolicy", basePolicy.refreshed);
    }

    /**
     * Test that getPermissions are cached per AppCodeSource.
     */
    public void testGetPermissions_cached() throws Exception
    {
        AppCodeSource acs = makeCodeSource(APPID_UNSIGNED, 0, new URL("file:/oc/yadda/yadda/"), true);

        NullHandler handler = new NullHandler();
        handler.clear();
        policy.setSecurityPolicyHandler(handler);

        // Call getPermissions
        PermissionCollection perms = policy.getPermissions(acs);
        assertNotNull("getPermissions() should return collection for " + acs, perms);
        assertEquals("Expected SecurityPolicyHandler to be called", 1, handler.infos.size());

        // Call again (same acs)
        PermissionCollection perms2 = policy.getPermissions(acs);
        assertNotNull("getPermissions() should return collection for " + acs, perms2);
        assertEquals("Expected handler to not be called -- permissions should be cached", 1, handler.infos.size());
        assertSame("Expected same collection returned", perms, perms2);

        // Call again (equiv acs)
        PermissionCollection perms3 = policy.getPermissions(makeCodeSource(APPID_UNSIGNED, 0, new URL(
                "file:/oc/yadda/yadda/"), false));
        assertNotNull("getPermissions() should return collection for " + acs, perms3);
        assertEquals("Expected handler to not be called -- permissions should be cached", 1, handler.infos.size());
        assertSame("Expected same collection returned", perms, perms3);

        // Call again (differnt acs)
        PermissionCollection perms4 = policy.getPermissions(makeCodeSource(APPID_UNSIGNED, 0, new URL(
                "file:/oc/yadda/yadda2/"), false));
        assertNotNull("getPermissions() should return collection for " + acs, perms4);
        assertEquals("Expected SecurityPolicyHandler to be called", 2, handler.infos.size());
    }

    /**
     * Tests getUnsignedPermissions(). Tests that expected permissions are
     * implied, and others are not.
     */
    public void testGetUnsignedPermissions()
    {
        PermissionCollection perms = policy.getUnsignedPermissions();

        assertNotNull("Unsigned permissions shouldn't be null", perms);

        Permission[] positive = {
                new MediaSelectPermission("*", null), // MHP 11.10.1.7
                new ReadPermission("*", null), // MHP 11.10.1.8
                new ServiceContextPermission("access", "own"), // MHP 11.10.1.9
                new ServiceContextPermission("getServiceContentHandlers", "own"), // MHP
                                                                                  // 11.10.1.9
                new FilePermission("/oc/some/dir/file", "read"), // MHP
                                                                 // 11.10.1.6
                new FilePermission("/oc/file", "read"), // MHP 11.10.1.6

                new PropertyPermission("file.separator", "read"), // MHP
                                                                  // 11.3.1.1
                new PropertyPermission("path.separator", "read"), // MHP
                                                                  // 11.3.1.1
                new PropertyPermission("line.separator", "read"), // MHP
                                                                  // 11.3.1.1
                new PropertyPermission("dvb.returnchannel.timeout", "read"), // MHP
                                                                             // 11.3.1.1

                new PropertyPermission("mhp.profile.internet_access", "read"), // MHP
                                                                               // 11.9.3
                new PropertyPermission("mhp.profile.interactive_broadcast", "read"), // MHP
                                                                                     // 11.9.3
                new PropertyPermission("mhp.profile.enhanced_broadcast", "read"), // MHP
                                                                                  // 11.9.3
                new PropertyPermission("mhp.eb.version.major", "read"), // MHP
                                                                        // 11.9.3
                new PropertyPermission("mhp.eb.version.minor", "read"), // MHP
                                                                        // 11.9.3
                new PropertyPermission("mhp.eb.version.micro", "read"), // MHP
                                                                        // 11.9.3
                new PropertyPermission("mhp.ib.version.major", "read"), // MHP
                                                                        // 11.9.3
                new PropertyPermission("mhp.ib.version.minor", "read"), // MHP
                                                                        // 11.9.3
                new PropertyPermission("mhp.ib.version.micro", "read"), // MHP
                                                                        // 11.9.3
                new PropertyPermission("mhp.ia.version.major", "read"), // MHP
                                                                        // 11.9.3
                new PropertyPermission("mhp.ia.version.minor", "read"), // MHP
                                                                        // 11.9.3
                new PropertyPermission("mhp.ia.version.micro", "read"), // MHP
                                                                        // 11.9.3
                new PropertyPermission("mhp.option.dvr", "read"), // MHP
                                                                  // 11.9.3.1

                new PropertyPermission(org.havi.ui.HVersion.HAVI_SPECIFICATION_VERSION, "read"), // MHP
                                                                                                 // A.7.4.36.3
                new PropertyPermission(org.havi.ui.HVersion.HAVI_SPECIFICATION_VENDOR, "read"),
                new PropertyPermission(org.havi.ui.HVersion.HAVI_SPECIFICATION_NAME, "read"),
                new PropertyPermission(org.havi.ui.HVersion.HAVI_IMPLEMENTATION_VERSION, "read"),
                new PropertyPermission(org.havi.ui.HVersion.HAVI_IMPLEMENTATION_VENDOR, "read"),
                new PropertyPermission(org.havi.ui.HVersion.HAVI_IMPLEMENTATION_NAME, "read"),

                new PropertyPermission("ocap.j.location", "read"), // OCAP
                                                                   // 13.3.7.2
                new PropertyPermission("ocap.profile", "read"), // OCAP 13.3.12
                new PropertyPermission("ocap.version", "read"), // OCAP 13.3.12
                new PropertyPermission("ocap.api.option.dvr", "read"), // OCAP
                                                                       // 13.3.12.2
                new PropertyPermission("ocap.api.option.hn", "read"), // OCAP
                                                                      // 13.3.12.2
                new PropertyPermission("ocap.system.highdef", "read"), // OCAP
                                                                       // 13.3.12.3
                new PropertyPermission("ocap.cablecard.manufacturer", "read"), // OCAP
                                                                               // 13.3.12.3
                new PropertyPermission("ocap.cablecard.version", "read"), // OCAP
                                                                          // 13.3.12.3
                new PropertyPermission("ocap.cablecard.vct-id", "read"), // OCAP
                                                                         // 13.3.12.3
                new PropertyPermission("ocap.hardware.host_id", "read"), // OCAP
                                                                         // 13.3.12.3
                new SocketPermission("localhost", "connect,accept,listen"), // OCAP
                                                                            // 13.3.13.4
        };
        Permission[] negative = {
                new ServiceContextPermission("*", "own"), // MHP 11.10.1.9
                new ServiceContextPermission("*", "*"), // MHP 11.10.1.9
                new FilePermission("/oc/some/dir/file", "write"),
                new FilePermission("/oc/file", "write"),
                new FilePermission("/itfs/-", "read"),

                new PropertyPermission("dvb.persistent.root", "read"),
                new PropertyPermission("dvb.persistent.root", "write"),

                new PropertyPermission("file.separator", "write"), // MHP
                                                                   // 11.3.1.1
                new PropertyPermission("path.separator", "write"), // MHP
                                                                   // 11.3.1.1
                new PropertyPermission("line.separator", "write"), // MHP
                                                                   // 11.3.1.1
                new PropertyPermission("dvb.returnchannel.timeout", "write"), // MHP
                                                                              // 11.3.1.1

                new PropertyPermission("*", "read,write"),
                new PropertyPermission("mhp.profile.internet_access", "write"), // MHP
                                                                                // 11.9.3
                new PropertyPermission("mhp.profile.interactive_broadcast", "write"), // MHP
                                                                                      // 11.9.3
                new PropertyPermission("mhp.profile.enhanced_broadcast", "write"), // MHP
                                                                                   // 11.9.3
                new PropertyPermission("mhp.eb.version.major", "write"), // MHP
                                                                         // 11.9.3
                new PropertyPermission("mhp.eb.version.minor", "write"), // MHP
                                                                         // 11.9.3
                new PropertyPermission("mhp.eb.version.micro", "write"), // MHP
                                                                         // 11.9.3
                new PropertyPermission("mhp.ib.version.major", "write"), // MHP
                                                                         // 11.9.3
                new PropertyPermission("mhp.ib.version.minor", "write"), // MHP
                                                                         // 11.9.3
                new PropertyPermission("mhp.ib.version.micro", "write"), // MHP
                                                                         // 11.9.3
                new PropertyPermission("mhp.ia.version.major", "write"), // MHP
                                                                         // 11.9.3
                new PropertyPermission("mhp.ia.version.minor", "write"), // MHP
                                                                         // 11.9.3
                new PropertyPermission("mhp.ia.version.micro", "write"), // MHP
                                                                         // 11.9.3
                new PropertyPermission("mhp.option.dvr", "write"), // MHP
                                                                   // 11.9.3.1

                new PropertyPermission(org.havi.ui.HVersion.HAVI_SPECIFICATION_VERSION, "write"), // MHP
                                                                                                  // A.7.4.36.3
                new PropertyPermission(org.havi.ui.HVersion.HAVI_SPECIFICATION_VENDOR, "write"),
                new PropertyPermission(org.havi.ui.HVersion.HAVI_SPECIFICATION_NAME, "write"),
                new PropertyPermission(org.havi.ui.HVersion.HAVI_IMPLEMENTATION_VERSION, "write"),
                new PropertyPermission(org.havi.ui.HVersion.HAVI_IMPLEMENTATION_VENDOR, "write"),
                new PropertyPermission(org.havi.ui.HVersion.HAVI_IMPLEMENTATION_NAME, "write"),

                new PropertyPermission("ocap.j.location", "write"), // OCAP
                                                                    // 13.3.7.2
                new PropertyPermission("ocap.profile", "write"), // OCAP 13.3.12
                new PropertyPermission("ocap.version", "write"), // OCAP 13.3.12
                new PropertyPermission("ocap.api.option.dvr", "write"), // OCAP
                                                                        // 13.3.12.2
                new PropertyPermission("ocap.api.option.hn", "write"), // OCAP
                                                                       // 13.3.12.2
                new PropertyPermission("ocap.hardware.vendor_id", "read"), // OCAP
                                                                           // 13.3.12.3
                new PropertyPermission("ocap.cablecard.manufacturer", "write"), // OCAP
                                                                                // 13.3.12.3
                new PropertyPermission("ocap.cablecard.version", "write"), // OCAP
                                                                           // 13.3.12.3
                new PropertyPermission("ocap.cablecard.vct-id", "write"), // OCAP
                                                                          // 13.3.12.3
                new PropertyPermission("ocap.hardware.host_id", "write"), // OCAP
                                                                          // 13.3.12.3

                new AWTPermission("accessEventQueue"), new AWTPermission("*"), new RuntimePermission("modifyThread"),
                new RuntimePermission("*"), new SerializablePermission("*"),
                new SerializablePermission("enableSubclassImplementation"),
                new PropertyPermission("user.language", "write"), new AppsControlPermission(),
                new MonitorAppPermission("properties"), new MonitorAppPermission("*"),
                new ServiceTypePermission(ServiceTypePermission.MFR, "*"),
                new ServiceTypePermission(ServiceTypePermission.MSO, "*"),
                new ServiceTypePermission(ServiceTypePermission.BROADCAST, "*"), new SelectPermission("*", "own"),
                new SelectPermission("*", "*"), new TunerPermission(""),
                new UserPreferencePermission("user @", "read"), new UserPreferencePermission("default font", "write"),
                new UserPreferencePermission("*", "read"), new DripFeedPermission(null), };

        // Positive tests
        for (int i = 0; i < positive.length; ++i)
            checkImplies(perms, positive[i]);

        // Negative tests
        for (int i = 0; i < negative.length; ++i)
            checkNotImplies(perms, negative[i]);
    }

    /**
     * Tests getUnsignedPermissions() w/ extensions installed.
     */
    public void testGetUnsignedPermissions_emptyExtensions()
    {
        // With no extensions
        policy = new TestedPolicyImpl(basePolicy, new Vector());
        testGetUnsignedPermissions();
    }

    /**
     * Tests getUnsignedPermissions() w/ extensions installed.
     */
    public void testGetUnsignedPermissions_extensions()
    {
        Vector extensions = new Vector();

        PolicyExtension ext = new DummyExtension();
        extensions.addElement(ext);
        Permission[] added = ext.getFixedUnsignedPermissions();

        policy = new TestedPolicyImpl(basePolicy, extensions);
        // testGetUnsignedPermissions();

        PermissionCollection perms = policy.getUnsignedPermissions();
        for (int i = 0; i < added.length; ++i)
        {
            checkImplies(perms, added[i]);
        }

        // Ensure still has standard unsigned perms
        checkImplies(perms, new ServiceContextPermission("access", "own")); // MHP
                                                                            // 11.10.1.9
    }

    /**
     * Tests setSecurityPolicyHandler.
     */
    public void testSetSecurityPolicyHandler() throws Exception
    {
        // Just smoke-test here -- make sure nothing bad happens

        URL url = new URL("file:/oc/blah/blah/blah");

        policy.setSecurityPolicyHandler(null);
        policy.getPermissions(makeCodeSource(APPID_UNSIGNED, 0, url, false));
        policy.setSecurityPolicyHandler(new NullHandler());
        policy.getPermissions(makeCodeSource(APPID_UNSIGNED, 0, url, false));
        policy.setSecurityPolicyHandler(null);
        policy.getPermissions(makeCodeSource(APPID_UNSIGNED, 0, url, false));
    }

    /**
     * Test getPermissions() for non-AppCodeSource. Should utilize the
     * basePolicy.
     */
    public void testGetPermissions_NonApp()
    {
        CodeSource cs = new CodeSource(null, null);
        doTestGetPermissions_NonApp(cs, null);

        cs = getClass().getProtectionDomain().getCodeSource();
        doTestGetPermissions_NonApp(cs, null);
    }

    /**
     * Test getPermissions() for non-AppCodeSource w/ no basePolicy set. Should
     * always returns AllPermission collection,
     */
    public void testGetPermissions_NonApp_NoBase()
    {
        basePolicy = null;
        policy = new TestedPolicyImpl(null);

        PermissionCollection all = new Permissions();
        all.add(new AllPermission());
        CodeSource cs = new CodeSource(null, null);
        doTestGetPermissions_NonApp(cs, all);

        cs = getClass().getProtectionDomain().getCodeSource();
        doTestGetPermissions_NonApp(cs, all);
    }

    /**
     * Tests getPermissions for unsigned app.
     * <ul>
     * <li>PRF should not be considered
     * <li>Handler should be invoked
     * </ul>
     */
    public void testGetPermissions_Unsigned() throws Exception
    {
        URL url = new URL("file:/local/blah/blah/blah");
        doTestGetAppPermissions_Unsigned(1, url, getUnsignedPermissions(url));
    }

    /**
     * Tests getPermissions() for signed app.
     */
    public void testGetPermissions_Signed() throws Exception
    {
        URL url = new URL("file:/oc/blah/blah/blah");

        doTestGetAppPermissions_OCAP_PRF(APPID_SIGNED, 0x200001, url, getSignedPermissions(url, true));
        doTestGetAppPermissions_OCAP_PRF(APPID_SIGNED, 0x2001, url, getSignedPermissions(url, true));
        doTestGetAppPermissions_DVB_PRF(APPID_SIGNED, 0x200001, url, getSignedPermissions(url, true));
        doTestGetAppPermissions_DVB_PRF(APPID_SIGNED, 0x2001, url, getSignedPermissions(url, true));
        doTestGetAppPermissions_No_PRF(APPID_SIGNED, 0x2001, url, getSignedPermissions(url, false));
    }

    /**
     * Tests getPermissions() for dually-signed app.
     */
    public void testGetPermissions_MonApp() throws Exception
    {
        URL url = new URL("file:/oc/blah/blah/blah");

        doTestGetAppPermissions_OCAP_PRF(APPID_MONAPP, 0x200001, url, getMonAppPermissions(url, true));
        doTestGetAppPermissions_DVB_PRF(APPID_MONAPP, 0x200001, url, getMonAppPermissions(url, true));
        doTestGetAppPermissions_No_PRF(APPID_MONAPP, 0x200001, url, getMonAppPermissions(url, false));
    }

    /**
     * Tests getPermissions() for host device apps.
     */
    public void testGetPermissions_HostUnsigned() throws Exception
    {
        URL url = new URL("file:/oc/blah/blah/blah");
        doTestGetAppPermissions_Unsigned(0x10001, url, getUnsignedPermissions(url));
    }

    /**
     * Tests getPermissions() for host device apps.
     */
    public void testGetPermissions_HostSigned() throws Exception
    {
        URL url = new URL("file:/oc/blah/blah/blah");

        doTestGetAppPermissions_OCAP_PRF(APPID_SIGNED, 0x10001, url, getHostSignedPermissions(url, true));
        doTestGetAppPermissions_DVB_PRF(APPID_SIGNED, 0x10001, url, getHostSignedPermissions(url, true));
        doTestGetAppPermissions_No_PRF(APPID_SIGNED, 0x10001, url, getHostSignedPermissions(url, false));
    }

    /**
     * Tests getPermissions() for host device apps.
     */
    public void testGetPermissions_HostMonApp() throws Exception
    {
        URL url = new URL("file:/oc/blah/blah/blah");

        doTestGetAppPermissions_OCAP_PRF(APPID_HOST_MONAPP, 0x10001, url, getHostMonAppPermissions(url, true));
        doTestGetAppPermissions_DVB_PRF(APPID_HOST_MONAPP, 0x10001, url, getHostMonAppPermissions(url, true));
        doTestGetAppPermissions_No_PRF(APPID_HOST_MONAPP, 0x10001, url, getHostMonAppPermissions(url, false));
    }

    /**
     * Tests getPermissions() for host device apps.
     */
    public void testGetPermissions_HostSuperApp() throws Exception
    {
        URL url = new URL("file:/oc/blah/blah/blah");
        PermissionCollection all = new Permissions();
        all.add(new AllPermission());

        AppCodeSource cs = makeCodeSource(APPID_HOST_SUPER, 0x10001, url, false);

        doTestGetPermissions(null, cs, all, null, false, false, null, null);
    }

    /**
     * Tests getPermissions() and checks that the PRF is authenticated
     * correctly.
     * <p>
     * This should be done by replacing the Authentication Manager and making
     * sure that it is queried appropriately.
     */
    public void XtestGetPermissions_PRF_auth() throws Exception
    {
        // TODO: implement testGetPermissions_PRF_auth
    }

    /**
     * Ensure that replaced SecurityPolicyHandler(s) aren't leaked.
     * 
     * @see "bug 4334"
     */
    public void testSecurityPolicyHandlerHandler_ClearLeak() throws Exception
    {
        // First ensure that WeakReference works as expected here
        Object o = new Object();
        Reference r0 = new WeakReference(o);
        assertNotNull("Reference should not be null", r0.get());
        o = null;
        // After GC, should be deleted
        System.gc();
        assertNull("Expected weak reference to return null", r0.get());

        SecurityPolicyHandler h1 = new NullHandler();
        Reference r1 = new WeakReference(h1);

        // Set security policy handler
        policy.setSecurityPolicyHandler(h1);
        h1 = null;

        // Clear security policy handler
        policy.setSecurityPolicyHandler(null);

        // After GC, should be deleted
        System.gc();
        assertNull("The cleared handler has apparently been leaked", r1.get());
    }

    /**
     * Ensure that replaced SecurityPolicyHandler(s) aren't leaked.
     * 
     * @see "bug 4334"
     */
    public void testSecurityPolicyHandler_ReplaceLeak() throws Exception
    {
        SecurityPolicyHandler h1 = new NullHandler();
        SecurityPolicyHandler h2 = new NullHandler();
        Reference r1 = new WeakReference(h1);
        Reference r2 = new WeakReference(h2);

        // Set security policy handler
        policy.setSecurityPolicyHandler(h1);
        h1 = null;

        // Replace security policy handler
        policy.setSecurityPolicyHandler(h2);
        h2 = null;

        // After GC, first should be deleted
        System.gc();

        try
        {
            assertNotNull("The currently set handler should still exist", r2.get());
            assertNull("The replaced has apparently been leaked", r1.get());
        }
        finally
        {
            policy.setSecurityPolicyHandler(null);
        }
    }

    /**
     * Test getPermissions assuming that PRF should never be queried. This is
     * valid for non-app cases.
     * 
     * @param cs
     *            the code source to use
     * @param expected
     *            the expected set of permissions (those defined by base policy
     *            -- or expected)
     */
    protected void doTestGetPermissions_NonApp(CodeSource cs, PermissionCollection expected)
    {
        doTestGetPermissions(null, cs, expected, null, false, false, null, null, null);
        doTestGetPermissions(null, cs, expected, null, false, false, null, null, TEST_HANDLERS[0]);
    }

    /**
     * Test getPermissions() for an AppCodeSource, assuming that PRF will never
     * be queried. This is invoked for unsigned apps only.
     * 
     * @param service
     *            service id to test
     * @param url
     *            base directory to test
     * @param expected
     *            expected set of permissions
     * 
     * @throws Exception
     *             upon error
     */
    protected void doTestGetAppPermissions_Unsigned(int service, URL url, PermissionCollection expected)
            throws Exception
    {
        AppCodeSource cs = makeCodeSource(APPID_UNSIGNED, service, url, false);

        doTestGetPermissions(null, cs, expected, null, false, false, null, null, null);
        doTestGetPermissions(null, cs, expected, null, false, false, null, null, TEST_HANDLERS[0]);
    }

    /**
     * Tests getPermissions() for an AppCodeSource, assuming that an OCAP PRF
     * should be found. This is invoked for all kinds of signed apps.
     * 
     * @param id
     *            the appid to test
     * @param service
     *            service id to test
     * @param url
     *            base directory to test
     * @param expected
     *            expected set of permissions
     * 
     * @throws Exception
     *             upon error
     */
    protected void doTestGetAppPermissions_OCAP_PRF(AppID id, int service, URL url, PermissionCollection expected)
            throws Exception
    {
        AppCodeSource cs = makeCodeSource(id, service, url, false);
        AppPermissions requested = getRequestedPermissions();

        String[] prfs = { createPrfUrl(cs, true) };
        boolean monApp[] = { isMonApp(id, service) };

        doTestGetPermissions(null, cs, expected, requested, true, true, prfs, monApp);
    }

    /**
     * Tests getPermissions() for an AppCodeSource, assuming that a DVB PRF
     * should be found. This is invoked for all kinds of signed apps.
     * 
     * @param id
     *            the appid to test
     * @param service
     *            service id to test
     * @param url
     *            base directory to test
     * @param expected
     *            expected set of permissions
     * 
     * @throws Exception
     *             upon error
     */
    protected void doTestGetAppPermissions_DVB_PRF(AppID id, int service, URL url, PermissionCollection expected)
            throws Exception
    {
        AppCodeSource cs = makeCodeSource(id, service, url, false);
        AppPermissions requested = getRequestedPermissions();

        String[] prfs = { createPrfUrl(cs, true), createPrfUrl(cs, false) };
        boolean monApp[] = { /* isMonApp(id, service), */false };

        doTestGetPermissions(null, cs, expected, requested, false, true, prfs, monApp);
    }

    private boolean isMonApp(AppID id, int service)
    {
        if (service <= 0xFFFF) return false;
        int aid = id.getAID();
        return aid >= 0x6000 && aid < 0x8000;
    }

    /**
     * Tests getPermissions() for an AppCodeSource, assuming that no PRF should
     * be found. This is invoked for all kinds of signed apps.
     * 
     * @param id
     *            the appid to test
     * @param service
     *            service id to test
     * @param url
     *            base directory to test
     * @param expected
     *            expected set of permissions
     * 
     * @throws Exception
     *             upon error
     */
    protected void doTestGetAppPermissions_No_PRF(AppID id, int service, URL url, PermissionCollection expected)
            throws Exception
    {
        AppCodeSource cs = makeCodeSource(id, service, url, false);
        AppPermissions requested = getRequestedPermissions();

        String[] prfs = { createPrfUrl(cs, true), createPrfUrl(cs, false) };
        boolean monApp[] = { /* isMonApp(id, service), false */};

        doTestGetPermissions(null, cs, expected, requested, false, false, prfs, monApp);
    }

    /**
     * Creates an AppCodeSource given the following info.
     * 
     * @param id
     *            appid
     * @param service
     *            service id
     * @param url
     *            base locator
     * @param allowCache
     *            AppCodeSource is written to not allow caching in hashtable if
     *            false;
     * @return an AppCodeSource
     */
    private AppCodeSource makeCodeSource(AppID id, int service, URL url, final boolean allowCache)
    {
        AppEntry entry = new AppEntry();
        entry.className = "initial.class.Name";
        //entry.serviceId = service;
        entry.id = id;
        Certificate[][] certChains = null;
        int authType = AuthInfo.AUTH_UNKNOWN;
        int AID = id.getAID();
        if (AID < 0x4000)
            authType = AuthInfo.AUTH_UNSIGNED;
        else if (service >= 0x10000)
        {
            if (AID < 0x6000)
                authType = AuthInfo.AUTH_SIGNED_OCAP;
            else if (AID < 0x8000) authType = AuthInfo.AUTH_SIGNED_DUAL;
        }
        else if (AID < 0x8000) authType = AuthInfo.AUTH_SIGNED_DVB;
        return new AppCodeSource(entry, url, authType, certChains)
        {
            // Explicitly disable any caching of AppCodeSource->Permissions
            public boolean equals(Object obj)
            {
                return allowCache && super.equals(obj);
            }
        };
    }

    /**
     * Creates a pathname for a PRF file.
     * 
     * @param cs
     *            app code source (contains AppEntry)
     * @param ocap
     *            if true, prefix is "ocap."; prefix is "dvb." otherwise
     * @return path name for a PRF file
     * @throws Exception
     */
    protected String createPrfUrl(AppCodeSource cs, boolean ocap) throws Exception
    {
        String path = cs.getAppEntry().className.replace('.', '/');
        String prefix = ocap ? "ocap." : "dvb.";
        String file;
        int idx = path.lastIndexOf('/');
        if (idx < 0)
        {
            file = path;
            path = "";
        }
        else
        {
            file = path.substring(idx + 1);
            path = path.substring(0, idx + 1);
        }

        String base = cs.getLocation().getPath();
        return base + (base.endsWith("/") ? "" : "/") + path + prefix + file + ".perm";
    }

    /**
     * Tests permissions with each type of handler.
     * 
     * @see #doTestGetPermissions(String, CodeSource, PermissionCollection,
     *      AppPermissions, boolean, boolean, String[], boolean[], TestHandler)
     */
    protected void doTestGetPermissions(String msg, CodeSource cs, PermissionCollection expected,
            AppPermissions requested, boolean ocap, boolean dvb, String[] prfFiles, boolean[] monAppPerms)
    {
        for (int i = 0; i < TEST_HANDLERS.length; ++i)
        {
            String name = TEST_HANDLERS[0].getClass().getName();
            int index = name.lastIndexOf('.');
            if (index > 0) name = name.substring(index);
            msg = cs + "/" + name + ": ";
            doTestGetPermissions(msg, cs, expected, requested, ocap, dvb, prfFiles, monAppPerms, TEST_HANDLERS[0]);
        }
    }

    /**
     * Validates the permission collection returned by getPermissions(cs). This
     * is called by just about everybody...
     * 
     * @param msg
     *            message prefix
     * @param cs
     *            the codesource to use
     * @param expected
     *            the expected set of permissions
     * @param requested
     *            the permissions that should be requested via PRF; null if none
     *            should be
     * @param ocap
     *            true if should find an ocap PRF
     * @param dvb
     *            true if should find a dvb PRF
     * @param prfFiles
     *            the URLs that should be given when querying PRF; empty if PRF
     *            should not be queried
     * @param monAppPerms
     *            the monAppPerm flags that should be given when quering PRF
     * @param handler
     *            the currently installed SecurityPolicyHandler
     */
    protected void doTestGetPermissions(String msg, CodeSource cs, PermissionCollection expected,
            AppPermissions requested, boolean ocap, boolean dvb, String[] prfFiles, boolean[] monAppPerms,
            TestHandler handler)
    {
        if (msg == null) msg = cs + ": ";
        if (basePolicy != null) basePolicy.clear();

        // Install handler if given one
        if (handler != null)
        {
            handler.clear();
            policy.setSecurityPolicyHandler(handler);
        }

        // Setup policy for test
        TestedPolicyImpl tpolicy = (TestedPolicyImpl) policy;
        tpolicy.clear();
        tpolicy.requested = requested;
        tpolicy.hasDvb = dvb;
        tpolicy.hasOcap = ocap;

        // Call getPermissions()
        PermissionCollection perms = policy.getPermissions(cs);

        // Check that basePolicy is called or not
        if (basePolicy != null)
            assertEquals(msg + "Expected basePolicy to be called?", !(cs instanceof AppCodeSource),
                    basePolicy.perms.size() > 0);

        // Check whether handler was called
        if (handler != null)
        {
            if (!(cs instanceof AppCodeSource))
                assertEquals("Did not expect handler to be queried for non-app", 0, handler.infos.size());
            else
                assertEquals("Expected handler to be called once", 1, handler.infos.size());
        }

        // non-apps w/ a basePolicy installed
        if (basePolicy != null && !(cs instanceof AppCodeSource))
        {
            PermissionCollection returned = (PermissionCollection) basePolicy.perms.get(cs);
            assertNotNull("Expected basePolicy to have been consulted", returned);
            assertSame("Expected same permissions returned", returned, perms);

            assertEquals("Did not expect PRF to be queried for non-app", 0,
                    ((TestedPolicyImpl) policy).calledFiles.size());
        }
        else
        {
            assertNotNull(msg + "Did not expect a null collection returned", perms);

            // If non-App w/out basePolicy or unsigned app
            if (prfFiles == null || prfFiles.length == 0)
            {
                assertEquals(msg + "Did not expect PRF to be queried for non-app/unsigned app", 0,
                        tpolicy.calledFiles.size());
                if (requested != null)
                    assertFalse(msg + "Expected returned collection not to imply requested collection",
                            PermissionCollectionTest.implies(perms, requested));
            }
            // Signed app...
            else
            {
                // Must have "requested" if we say ocap or dvb PRF exists
                if (dvb || ocap) assertNotNull("internal error", requested);

                // Check PRF query in getPrfData
                assertEquals(msg + "Expected PRF to be queried different number of times", prfFiles.length,
                        tpolicy.calledFiles.size());
                for (int i = 0; i < prfFiles.length; ++i)
                {
                    assertEquals(msg + "Unexpected prfFiles[" + i + "] in PRF request", prfFiles[i],
                            tpolicy.calledFiles.elementAt(i));
                }
                // Check PRF parse query in parsePrf
                assertEquals(msg + "Expected PRF to be parsed different number of times", monAppPerms.length,
                        tpolicy.calledMonApps.size());
                for (int i = 0; i < monAppPerms.length; ++i)
                    assertEquals(msg + "Unexpected monAppPerm[" + i + "] in PRF parse request", monAppPerms[i],
                            ((Boolean) tpolicy.calledMonApps.elementAt(i)).booleanValue());

                // Check handler query
                if (handler != null)
                {
                    AppCodeSource acs = (AppCodeSource) cs;
                    PermissionInformation info = (PermissionInformation) handler.infos.elementAt(0);

                    // Check PermissionInfo
                    assertEquals("Unexpected AppID", acs.getAppEntry().id, info.getAppID());
                    assertEquals("Unexpected certificates[][]", acs.getCertificateChains(), info.getCertificates());

                    int service = 1;
                    assertEquals("Unexpected isManufacturerApp()", service > 0xFFFF && service < 0x200000,
                            info.isManufacturerApp());

                    // Check granted permissions
                    handler.checkPermissions(info, perms);

                    // Replace perms with requestedPermissions for subsequent
                    // tests...
                    perms = info.getRequestedPermissions();
                }

                // If PRF is present, check that requested permissions are
                // granted
                if (dvb || ocap)
                {
                    assertTrue(msg + "Expected returned collection to imply requested collection",
                            PermissionCollectionTest.implies(perms, requested));

                    // Test that expected+requested implies perms
                    AppPermissions expectedRequested = new AppPermissions(expected);
                    expectedRequested.add(requested);
                    assertTrue(msg + "Expected returned collection to be implied by requested+expected",
                            PermissionCollectionTest.implies(perms, expectedRequested));
                }
                // If PRF isn't present, check that requested permissions AREN'T
                // granted
                else if (requested != null)
                {
                    assertFalse(msg + "Did not expect returned collection to imply requested collection",
                            PermissionCollectionTest.implies(perms, requested));
                }
            } // unsigned/signed app

            // FINALLY! check that expected permissions were granted!
            // Check that returned permissions imply the expected permissions
            // Note: if handler is set, this will be testing permissions passed
            // to the handler
            PermissionCollectionTest.checkImplies(perms, expected);
            if (requested == null)
                assertTrue(msg + "Expected returned collection to be implied by expected collection",
                        PermissionCollectionTest.implies(expected, perms));

        }

        policy.setSecurityPolicyHandler(null);
    }

    /**
     * Return a set of (fake) PRF-requested permissions.
     */
    protected AppPermissions getRequestedPermissions()
    {
        AppPermissions perms = new AppPermissions();
        perms.add(new SocketPermission("*", "listen, connect, resolve")); // NOTE:
                                                                          // don't
                                                                          // use
                                                                          // unresolvable
                                                                          // names
                                                                          // in
                                                                          // test
        perms.add(new AppsControlPermission("", ""));

        PersistentFileCredential pfc = new PersistentFileCredential();
        pfc.add(new FilePermission("/itfs/789a/4eef/*", "read"));
        pfc.add(new FilePermission("/itfs/789a/4eef/*", "write"));
        pfc.setExpiration(new Date(System.currentTimeMillis() + 3600 * 1000));
        perms.add(pfc);

        return perms;
    }

    /**
     * Return the set of unsigned permissions.
     */
    protected PermissionCollection getUnsignedPermissions(URL url)
    {
        AppPermissions perms = new AppPermissions(policy.getUnsignedPermissions());

        return perms;
    }

    /**
     * Get the set of permissions expected to be granted to a signed app.
     * 
     * @param url
     * @param hasPRF
     *            indicates whether a PRF is supplied
     */
    protected PermissionCollection getSignedPermissions(URL url, boolean hasPRF)
    {
        PermissionCollection unsigned = getUnsignedPermissions(url);
        AppPermissions perms = new AppPermissions(unsigned);

        // Add permissions to perms
        perms.add(new PropertyPermission("dvb.persistent.root", "read")); // MHP
                                                                          // 11.10.2.1
        perms.add(new ServiceContextPermission("*", "own")); // MHP 11.10.2.7

        // Shouldn't be included if there's a PRF...
        if (!hasPRF) perms.add(new SelectPermission("*", "own")); // MHP
                                                                  // 11.10.2.7

        return perms;
    }

    /**
     * Get the set of permissions expected to be granted to a signed host app.
     * 
     * @param url
     * @param hasPRF
     *            indicates whether a PRF is supplied
     * @return set of permission expected to be granted to a signed host app
     * @throws Exception
     */
    protected PermissionCollection getHostSignedPermissions(URL url, boolean hasPRF) throws Exception
    {
        PermissionCollection signed = getSignedPermissions(url, hasPRF);
        AppPermissions perms = new AppPermissions(signed);

        String value = MPEEnv.getSystemProperty("OCAP.security.policy.host.n");
        final int max = (value == null) ? 32 : Integer.parseInt(value);
        for (int i = 0; i < max; ++i)
        {
            value = MPEEnv.getSystemProperty("OCAP.security.policy.host." + i);
            if (value != null)
            {
                StringTokenizer tok = new StringTokenizer(value, " ");
                int nTokens = tok.countTokens();
                String className = tok.hasMoreTokens() ? tok.nextToken() : null;
                String name = tok.hasMoreTokens() ? tok.nextToken() : null;
                String action = tok.hasMoreTokens() ? tok.nextToken() : null;
                Permission p = null;
                switch (nTokens)
                {
                    case 3:
                        try
                        {
                            Class pClass = Class.forName(className);
                            Constructor ctor = pClass.getConstructor(new Class[] { String.class, String.class });
                            p = (Permission) ctor.newInstance(new Object[] { name, action });
                            break;
                        }
                        catch (Throwable e)
                        { /* ignore */
                        }
                    case 2:
                        try
                        {
                            Class pClass = Class.forName(className);
                            Constructor ctor = pClass.getConstructor(new Class[] { String.class });
                            p = (Permission) ctor.newInstance(new Object[] { name });
                            break;
                        }
                        catch (Throwable e)
                        { /* ignore */
                        }
                    case 1:
                        try
                        {
                            Class pClass = Class.forName(className);
                            p = (Permission) pClass.newInstance();
                        }
                        catch (Throwable e)
                        { /* ignore */
                        }
                        // fall through
                    default:
                        continue;
                }
                perms.add(p);
            }
        }
        return perms;
    }

    /**
     * Get the set of permissions expected to be granted to a monapp host app.
     * 
     * @param url
     * @param hasPRF
     *            indicates whether a PRF is supplied
     * @return set of permissions expected to be granted to a monapp host app
     * @throws Exception
     */
    protected PermissionCollection getHostMonAppPermissions(URL url, boolean hasPRF) throws Exception
    {
        PermissionCollection signed = getHostSignedPermissions(url, hasPRF);
        PermissionCollection monapp = getMonAppPermissions(url, hasPRF);

        AppPermissions perms = new AppPermissions();
        perms.add(signed);
        perms.add(monapp);
        return perms;
    }

    /**
     * Get the set of permissions expected to be granted to a MonApp.
     * 
     * @param url
     * @param hasPRF
     *            indicates whether a PRF is supplied
     * @return set of permissions expected to be granted to a MonApp
     */
    protected PermissionCollection getMonAppPermissions(URL url, boolean hasPRF)
    {
        PermissionCollection signed = getSignedPermissions(url, hasPRF);
        AppPermissions perms = new AppPermissions(signed);

        // MonApp specific properties access
        // OCAP 13.3.12.3 / 21.2.1.20
        // Only available with MonAppPermission("properties");
        /*
         * perms.add(new PropertyPermission("ocap.hardware.vendor_id", "read"));
         * perms.add(new PropertyPermission("ocap.hardware.version_id",
         * "read")); perms.add(new
         * PropertyPermission("ocap.hardware.createdate", "read"));
         * perms.add(new PropertyPermission("ocap.hardware.serialnum", "read"));
         * perms.add(new PropertyPermission("ocap.memory.video", "read"));
         * perms.add(new PropertyPermission("ocap.memory.total", "read"));
         * perms.add(new PropertyPermission("ocap.system.highdef", "read"));
         */

        return perms;
    }

    protected void checkImplies(PermissionCollection perms, Permission p)
    {
        assertTrue("Expected to imply " + p, perms.implies(p));
    }

    protected void checkNotImplies(PermissionCollection perms, Permission p)
    {
        assertFalse("Expected to imply " + p, perms.implies(p));
    }

    private static final AppID APPID_UNSIGNED = new AppID(27, 0x3FFF);

    private static final AppID APPID_SIGNED = new AppID(28, 0x5FFF);

    private static final AppID APPID_MONAPP = new AppID(29, 0x7FFF);

    private static final AppID APPID_HOST_MONAPP = new AppID(30, 0x6FFF);

    private static final AppID APPID_HOST_SUPER = new AppID(31, 0x7FFF);

    /**
     * Interface implemented by test SecurityPolicyHandlers. Adds a method that
     * allows the handler to verify the results.
     * 
     * @author Aaron Kamienski
     */
    static abstract class TestHandler implements SecurityPolicyHandler
    {
        public final Vector infos = new Vector();

        public void clear()
        {
            infos.clear();
        }

        public abstract void checkPermissions(PermissionInformation info, PermissionCollection granted);
    }

    private static final TestHandler[] TEST_HANDLERS = { new NullHandler(), new RequestedHandler(),
            new SignedSameHandler(), new UnsignedHandler(), new UnsignedSameHandler(), new ExtraPermissions(),
            new SubsetUnsignedPermissions(), null };

    /**
     * Returns null, expects requested to be granted.
     * 
     * @author Aaron Kamienski
     */
    static class NullHandler extends TestHandler
    {
        public PermissionCollection getAppPermissions(PermissionInformation permissionInfo)
        {
            infos.addElement(permissionInfo);
            return null;
        }

        public void checkPermissions(PermissionInformation info, PermissionCollection granted)
        {
            PermissionCollectionTest.checkImplies(granted, info.getRequestedPermissions());
            PermissionCollectionTest.checkImplies(info.getRequestedPermissions(), granted);
        }
    }

    /**
     * Returns requested, expects requested to be granted.
     * 
     * @author Aaron Kamienski
     */
    static class RequestedHandler extends NullHandler
    {
        public PermissionCollection getAppPermissions(PermissionInformation permissionInfo)
        {
            super.getAppPermissions(permissionInfo);
            return permissionInfo.getRequestedPermissions();
        }
    }

    /**
     * Returns same as requested, expects requested to be granted.
     * 
     * @author Aaron Kamienski
     */
    static class SignedSameHandler extends NullHandler
    {
        public PermissionCollection getAppPermissions(PermissionInformation permissionInfo)
        {
            super.getAppPermissions(permissionInfo);

            PermissionCollection perms = new Permissions();
            for (Enumeration e = permissionInfo.getRequestedPermissions().elements(); e.hasMoreElements();)
                perms.add((Permission) e.nextElement());
            return perms;
        }
    }

    /**
     * Returns unsigned permissions, expects unsigned granted.
     * 
     * @author Aaron Kamienski
     */
    static class UnsignedHandler extends NullHandler
    {
        public PermissionCollection getAppPermissions(PermissionInformation permissionInfo)
        {
            super.getAppPermissions(permissionInfo);
            return PermissionInformation.getUnsignedAppPermissions();
        }

        public void checkPermissions(PermissionInformation info, PermissionCollection granted)
        {
            PermissionCollectionTest.checkImplies(granted, PermissionInformation.getUnsignedAppPermissions());
            PermissionCollectionTest.checkImplies(PermissionInformation.getUnsignedAppPermissions(), granted);
        }
    }

    /**
     * Returns same as unsigned, expects unsigned granted.
     * 
     * @author Aaron Kamienski
     */
    static class UnsignedSameHandler extends UnsignedHandler
    {
        public PermissionCollection getAppPermissions(PermissionInformation permissionInfo)
        {
            PermissionCollection unsigned = super.getAppPermissions(permissionInfo);

            PermissionCollection perms = new Permissions();
            for (Enumeration e = unsigned.elements(); e.hasMoreElements();)
                perms.add((Permission) e.nextElement());
            return perms;
        }
    }

    /**
     * Returns requested+more, expectes requested to be granted.
     * 
     * @author Aaron Kamienski
     */
    static class ExtraPermissions extends SignedSameHandler
    {
        public PermissionCollection getAppPermissions(PermissionInformation permissionInfo)
        {
            PermissionCollection perms = super.getAppPermissions(permissionInfo);
            perms.add(new FilePermission("/oc/-", "write"));
            return perms;
        }
    }

    /**
     * Returns subset(unsigned), expects requested to be granted.
     * 
     * @author Aaron Kamienski
     */
    static class SubsetUnsignedPermissions extends SignedSameHandler
    {
        public PermissionCollection getAppPermissions(PermissionInformation permissionInfo)
        {
            super.getAppPermissions(permissionInfo);
            PermissionCollection unsigned = PermissionInformation.getUnsignedAppPermissions();

            PermissionCollection perms = new Permissions();
            boolean keep = false;
            for (Enumeration e = unsigned.elements(); e.hasMoreElements();)
            {
                if (keep) perms.add((Permission) e.nextElement());
                keep = !keep;
            }
            return perms;
        }
    }

    /**
     * Returns subset(requested), expects subset(requested) to be granted.
     * 
     * @author Aaron Kamienski
     */
    static class SubsetSignedPermissions extends SignedSameHandler
    {
        PermissionCollection returned;

        public PermissionCollection getAppPermissions(PermissionInformation permissionInfo)
        {
            PermissionCollection unsigned = PermissionInformation.getUnsignedAppPermissions();
            PermissionCollection signed = super.getAppPermissions(permissionInfo);

            PermissionCollection perms = new Permissions();
            boolean keep = false;
            for (Enumeration e = signed.elements(); e.hasMoreElements();)
            {
                Permission p = (Permission) e.nextElement();
                if (unsigned.implies(p))
                    perms.add(p);
                else if (keep)
                {
                    perms.add(p);
                    keep = !keep;
                }
            }
            return returned = perms;
        }

        public void checkPermissions(PermissionInformation info, PermissionCollection granted)
        {
            PermissionCollectionTest.checkImplies(granted, returned);
            PermissionCollectionTest.checkImplies(returned, granted);
        }
    }

    /**
     * A <i>dummy</i> policy used to verify the fallback capabilities of
     * <code>PolicyImpl</code>. This policy always returns collections with
     * <code>AllPermission</code>. It also records all calls to
     * <code>getPermissions()</code> and <code>refresh()</code>
     * 
     * @author Aaron Kamienski
     */
    private class DummyPolicy extends Policy
    {
        /**
         * Overrides super implementation.
         * 
         * @see java.security.Policy#getPermissions(java.security.CodeSource)
         */
        public PermissionCollection getPermissions(CodeSource codesource)
        {
            PermissionCollection pc;
            synchronized (perms)
            {
                pc = (PermissionCollection) perms.get(codesource);
                if (pc == null) pc = newCollection();
                perms.put(codesource, pc);
            }
            return pc;
        }

        public void refresh()
        {
            refreshed = true;
        }

        public void clear()
        {
            refreshed = false;
            perms.clear();
        }

        private PermissionCollection newCollection()
        {
            PermissionCollection pc = new Permissions();
            pc.add(new AllPermission());
            return pc;
        }

        public final Hashtable perms = new Hashtable();

        public boolean refreshed;
    }

    /**
     * An extension of PolicyImpl used during testing to allow the overriding of
     * <code>parsePrf()</code>. The actual parsing of the PRF is tested with
     * XmlManager testing.
     * 
     * @author Aaron Kamienski
     */
    class TestedPolicyImpl extends PolicyImpl
    {
        private final byte[] OCAP_PRF = "OCAP".getBytes();

        private final byte[] DVB_PRF = "DVB".getBytes();

        /**
         * Overrides super implementation.
         * 
         * @see org.cablelabs.impl.security.PolicyImpl#getPrfData(java.lang.String,
         *      int)
         */
        byte[] getPrfData(String filename, int status)
        {
            calledFiles.addElement(filename);

            String str = filename;
            if (str.indexOf("ocap.") >= 0)
            {
                return (!hasOcap) ? null : OCAP_PRF;
            }
            else if (str.indexOf("dvb.") >= 0)
            {
                return (!hasDvb) ? null : DVB_PRF;
            }
            return null;
        }

        /**
         * Overrides super implementation.
         * 
         * @see org.cablelabs.impl.security.PolicyImpl#parsePrf(byte[], boolean,
         *      boolean)
         */
        AppPermissions parsePrf(byte[] bytes, boolean ocapPerms, boolean monAppPerms) throws IOException
        {
            // This is now only going to be called when found to exist!!!!
            calledMonApps.addElement(monAppPerms ? Boolean.TRUE : Boolean.FALSE);
            if (bytes == OCAP_PRF && hasOcap)
                return requested;
            else if (bytes == DVB_PRF && hasDvb) return requested;
            return null;
        }

        public TestedPolicyImpl(Policy base)
        {
            this(base, null);
        }

        public TestedPolicyImpl(Policy base, Vector extensions)
        {
            super(base, extensions);
        }

        public void clear()
        {
            calledFiles.clear();
            calledMonApps.clear();
        }

        public AppPermissions requested = null;

        public boolean hasOcap = false;

        public boolean hasDvb = false;

        // public final Vector calledURLs = new Vector();
        public final Vector calledFiles = new Vector();

        public final Vector calledMonApps = new Vector();
    }

    private class DummyPermission extends BasicPermission
    {
        DummyPermission(String name)
        {
            super(name);
        }
    }

    private class DummyExtension implements PolicyExtension
    {
        public Permission[] getFixedUnsignedPermissions()
        {
            return new Permission[] { new DummyPermission("dummy1"), new DummyPermission("dummy2"), };
        }
    }

    private DummyPolicy basePolicy;

    private PolicyImpl policy;

    public void setUp() throws Exception
    {
        super.setUp();

        basePolicy = new DummyPolicy();
        // Install a dummy extension
        Vector extensions = new Vector();
        extensions.addElement(new DummyExtension());
        // Actually test a special subclass of PolicyImpl
        policy = new TestedPolicyImpl(basePolicy, extensions);
    }

    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /*
     * private AuthManager save; private AuthManager authMgr; private void
     * replaceAuthManager() { save =
     * (AuthManager)ManagerManager.getInstance(AuthManager.class);
     * ManagerManagerTest.updateManager(AuthManager.class, TestAuthMgr.class,
     * false, authMgr = new TestAuthMgr()); }
     * 
     * private void restoreAuthManager() { if ( save != null )
     * ManagerManagerTest.updateManager(AuthManager.class, save.getClass(),
     * false, save); }
     * 
     * public static class TestAuthMgr implements AuthManager { private int type
     * = AuthInfo.AUTH_UNSIGNED;
     * 
     * void setAuthType(int type) { this.type = type; }
     * 
     * int getAuthType() { return type; }
     * 
     * void setAuthType(AppID id, boolean mso) { int AID = id.getAID(); if ( AID
     * < 0x4000 ) type = AuthInfo.AUTH_UNSIGNED; else if ( AID < 0x6000 ) type =
     * mso ? AuthInfo.AUTH_SIGNED_OCAP : AuthInfo.AUTH_SIGNED_DVB; else if ( AID
     * < 0x8000 ) type = mso ? AuthInfo.AUTH_SIGNED_DUAL :
     * AuthInfo.AUTH_SIGNED_DVB; else type = AuthInfo.AUTH_UNKNOWN; }
     * 
     * public AuthContext createAuthCtx(String initialFile, int signers) { throw
     * new UnsupportedOperationException("Unimplemented for test"); //return
     * null; }
     * 
     * public AuthContext getAuthCtx(CallerContext cc) { throw new
     * UnsupportedOperationException("Unimplemented for test"); //return null; }
     * 
     * public AuthInfo getClassAuthInfo(String targName, FileSys fs) { return
     * null; }
     * 
     * public AuthInfo getDirAuthInfo(String dir, FileSys fs, String[] files)
     * throws IOException { return null; }
     * 
     * public AuthInfo getFileAuthInfo(String targName, FileSys fs, byte[] file)
     * throws IOException { return null; }
     * 
     * public AuthInfo getFileAuthInfo(String targName, FileSys fs) throws
     * IOException { return null; }
     * 
     * public String[] getHashfileNames(String dir, FileSys fs) throws
     * IOException { return null; }
     * 
     * public X509Certificate[][] getSigners(String targName, boolean knownRoot,
     * FileSys fs, byte[] file) throws InvalidFormatException,
     * InterruptedIOException, MPEGDeliveryException, ServerDeliveryException,
     * InvalidPathNameException, NotEntitledException, ServiceXFRException,
     * InsufficientResourcesException { return null; }
     * 
     * public X509Certificate[][] getSigners(String targName, FileSys fs, byte[]
     * file) { return null; }
     * 
     * public void invalidate(String targName) { throw new
     * UnsupportedOperationException("Unimplemented for test"); }
     * 
     * public void registerCRLMount(String path) { throw new
     * UnsupportedOperationException("Unimplemented for test"); }
     * 
     * public void setAuthCtx(CallerContext cc, AuthContext authCtx) { throw new
     * UnsupportedOperationException("Unimplemented for test"); }
     * 
     * public void setPrivilegedCerts(byte[] codes) { throw new
     * UnsupportedOperationException("Unimplemented for test"); }
     * 
     * public void unregisterCRLMount(String path) { throw new
     * UnsupportedOperationException("Unimplemented for test"); }
     * 
     * public void updateCRL(String path, FileSys fs) { throw new
     * UnsupportedOperationException("Unimplemented for test"); }
     * 
     * public void destroy() { // Does nothing } }
     */

    /**
     * Constructor for PolicyImplTest.
     * 
     * @param name
     */
    public PolicyImplTest(String name)
    {
        super(name);
    }

    public static Test suite()
    {
        return new TestSuite(PolicyImplTest.class);
    }

    static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(PolicyImplTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new PolicyImplTest(tests[i]));
            return suite;
        }
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite(args));
    }
}
