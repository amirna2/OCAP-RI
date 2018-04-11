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

package org.cablelabs.impl.signalling;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;

/**
 * Tests <code>Xait</code> implementations.
 */
public class XaitTest extends AitTest
{
    /**
     * Tests getApps().
     */
    public void testGetApps()
    {
        super.testGetApps();

        AppEntry[] apps = xait.getApps();
        for (int i = 0; i < apps.length; ++i)
            assertTrue("Expected apps[" + i + "] to be XAppEntry", apps[i] instanceof XAppEntry);
    }

    /**
     * Tests getServices().
     */
    public void testGetServices()
    {
        Vector vector = xFactory.getServices();
        AbstractServiceEntry[] actual = xait.getServices();
        Vector allApps = factory.getAppEntries();

        assertNotNull("should always return a valid array", actual);

        assertEquals("Expected same number of services", vector.size(), actual.length);

        // same entries
        for (Enumeration e = vector.elements(); e.hasMoreElements();)
        {
            AbstractServiceEntry expected = (AbstractServiceEntry) e.nextElement();
            AbstractServiceEntry svc = null;
            int id = expected.id;

            for (int i = 0; i < actual.length; ++i)
            {
                if (actual[i] != null && actual[i].id == expected.id)
                {
                    svc = actual[i];
                    actual[i] = null;
                    break;
                }
            }

            // Found an entry that matches
            assertNotNull("Could not find service " + expected.id, svc);

            // Make sure that they match
            assertEquals("Svc[" + expected.id + "]", expected, svc);

            // No check into the application match-ups!
            /*
             * AppSignalling apps[] = svc.getApps(); for(int i = 0; i <
             * apps.length; ++i) { XAppSignalling xapp =
             * (XAppSignalling)apps[i]; AppID id = xapp.getAppID();
             * 
             * 
             * assertEquals("App ("+id+") doesn't belong to this service "+svc.id
             * , svc.id, xapp.getServiceId());
             * 
             * boolean found; for(int j = 0; j < allApps.length; ++j) { if
             * (allApps[j] == xapp) { found = true; allApps[j] = null; break; }
             * } assertTrue("Did not found app ("+id+") amongst all apps",
             * found); }
             */
        }

        // Finally ensure that ALL apps belong to a service
        /*
         * for(int i = 0; i < allApps.length; ++i) {
         * assertTrue("There were some apps that didn't belong to a service",
         * allApps[i] == null); }
         */
    }

    /**
     * Tests getServices() for duplicates.
     */
    public void testGetServicesDuplicates()
    {
        AbstractServiceEntry[] actual = xait.getServices();
        Hashtable services = new Hashtable();

        for (int i = 0; i < actual.length; ++i)
        {
            Integer key = new Integer(actual[i].id);

            assertTrue("Found a duplicate for id==" + actual[i].id, services.get(key) == null);
            services.put(key, actual[i]);
        }
    }

    /**
     * Tests getSource().
     */
    public void testGetSource()
    {
        assertEquals("Expected same source", xFactory.getSource(), xait.getSource());
    }

    /**
     * Tests getPrivilegedCertificateBytes().
     */
    public void testGetPrivilegedCertificateBytes()
    {
        byte[] expected = xFactory.getPrivilegedCertificates();
        byte[] actual = xait.getPrivilegedCertificateBytes();
        if (expected == null)
        {
            assertSame("Expected no privileged certificates to be returned", expected, actual);
        }
        else
        {
            assertFalse("Internal test error - did not expect exact same array instance returned", expected == actual);
            assertEquals("Unexpected length of privileged certificate bytes", expected.length, actual.length);

            for (int i = 0; i < expected.length; ++i)
                assertEquals("Unexpected byte at " + i, expected[i], actual[i]);
        }
    }

    protected void assertEquals(String msg, AbstractServiceEntry expected, AbstractServiceEntry actual)
    {
        assertEquals(msg + ": unexpected id", expected.id, actual.id);
        assertEquals(msg + ": unexpected autoSelect", expected.autoSelect, actual.autoSelect);
        assertEquals(msg + ": unexpected name", expected.name, actual.name);

        // As we don't expect "expected" to actually refernece any apps,
        // we check for apps elsewhere
    }

    protected void assertEquals(String msg, XAppEntry expected, AppEntry actual)
    {
        super.assertEquals(msg, expected, actual);

        assertTrue(msg + ": expected instanceof XAppSignalling", actual instanceof XAppEntry);
        XAppEntry xactual = (XAppEntry) actual;
        assertEquals(msg + ": unexpected XAppSignalling serviceId", expected.serviceId, xactual.serviceId);
        assertEquals(msg + ": unexpected XAppSignalling versionNumber", expected.version, xactual.version);
    }

    /**
     * Overrides super implementation.
     * 
     * @see org.cablelabs.impl.signalling.AitTest#checkControlCode(java.lang.String,
     *      int, int)
     */
    protected void checkControlCode(String msg, int expected, int actual)
    {
        if (expected == OcapAppAttributes.REMOTE) expected = OcapAppAttributes.PRESENT;

        super.checkControlCode(msg, expected, actual);
    }

    protected void assertEquals(String msg, XAppEntry expected, XAppEntry actual)
    {
        super.assertEquals(msg, expected, actual);

        assertEquals(msg + ": unexpected serviceId", expected.serviceId, actual.serviceId);
        assertEquals(msg + ": unexpected version", expected.version, actual.version);
        assertEquals(msg + ": unexpected storagePriority", expected.storagePriority, actual.storagePriority);
        assertEquals(msg + ": unexpected launchOrder", expected.launchOrder, actual.launchOrder);
    }

    /* ================== boilerplate =================== */

    protected Xait xait;

    protected XaitFactory xFactory;

    protected void setUp() throws Exception
    {
        super.setUp();
        xait = (Xait) ait;
    }

    protected void tearDown() throws Exception
    {
        xait = null;
        super.tearDown();
    }

    public static InterfaceTestSuite isuite() // throws Exception
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(XaitTest.class);
        suite.setName(Xait.class.getName());
        return suite;
    }

    public XaitTest(String name, ImplFactory f)
    {
        this(name, Xait.class, f);
    }

    protected XaitTest(String name, Class impl, ImplFactory f)
    {
        super(name, impl, f);
        xFactory = (XaitFactory) f;
    }

    public interface XaitFactory
    {
        public Vector getServices();

        public int getSource();

        public byte[] getPrivilegedCertificates();
    }

    /* ================== samples =================== */

    private static final AbstractServiceEntry[] services = { new TestService(true, 0x020001, "Service1", true),
            new TestService(true, 0xFF0002, "Service2", false), new TestService(true, 0x7F0003, "Service3", false),
            new TestService(false, 0xFFFF0001, "Service4", false),
            new TestService(false, 0xFFFF0001, "Service5", true), };

    public static final TestApp[] TEST_APPS = {
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 9), new Hash(new String[] { "eng", "App0" }),
                    VERSIONS0, true, AppEntry.VISIBLE, 100, null, 0, new String[] { "arg1", "arg2" }, "/",
                    "org.cablelabs.app0.MainXlet", new String[0], TP0, null, null, null, services[0], 0, 0, 0, null,
                    new int[] { 10, 11 }),
            new TestApp(true, OcapAppAttributes.REMOTE, new AppID(10, 10), new Hash(new String[] { "spa", "App Uno",
                    "eng", "App1" }), VERSIONS0, true, AppEntry.VISIBLE, 99, "icons", 0xF, new String[0], "/app1",
                    "org.cablelabs.app1.MainXlet", new String[0], TP0, null, null, null, services[1], 0, 0, 0,
                    new String[0], new int[] { 10 }),
            new TestApp(
                    false, // No TPs
                    OcapAppAttributes.PRESENT, new AppID(10, 11), new Hash(new String[] { "eng", "App2" }), VERSIONS0,
                    true, AppEntry.VISIBLE, 101, "icons", 0xF, new String[0], "/app2", "org.cablelabs.app1.MainXlet",
                    new String[0], null, null, null, null, services[0], 1, 2, 3, new String[0], new int[] { 11 }),
            new TestApp(
                    false, // No classname
                    OcapAppAttributes.REMOTE, new AppID(10, 12), new Hash(new String[] { "eng", "App3" }), VERSIONS0,
                    true, AppEntry.VISIBLE, 102, "icons", 0xF, new String[0], "/app3", null, new String[0], TP0, null,
                    null, null, services[0], 0, 0, 0, new String[0], new int[] { 10, 11, 15 }),
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 13), new Hash(new String[] { "spa", "Quatro",
                    "eng", "App4" }), VERSIONS0, true, AppEntry.VISIBLE, 103, "icons", 0xF, new String[0],
                    "/app4/classes", "MainXlet", new String[] { "moreclasses", "/" }, TP1, null, null, null,
                    services[1], 3, 2, 1, new String[] { "com.a.b" }, new int[] { 10 }),
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 14), new Hash(new String[] { "eng", "App5" }),
                    VERSIONS0, true, AppEntry.VISIBLE, 105, null, 0, new String[0], "app5", "MainXlet",
                    new String[] { "/" }, TP0, null, null, null, services[0], 0, 0, 0, new String[] { "com.a.b",
                            "com.a.c" }, new int[] { 10 }),
            new TestApp(true, OcapAppAttributes.REMOTE, new AppID(10, 15), new Hash(new String[] { "eng", "App6" }),
                    VERSIONS0, true, AppEntry.NON_VISIBLE, 199, null, 0, new String[0], "/", "A", new String[0], TP1,
                    null, null, null, services[2], 1, 2, 3, new String[] { "com.a.c", "com.a.b", "com.x.y.z" },
                    new int[] {}), };

    public static class TestApp extends AitTest.TestApp
    {
        public boolean valid;

        public AbstractServiceEntry service;

        /** Adds a constructor... */
        public TestApp(boolean valid, int controlCode, AppID id, Hashtable names, Hashtable versions,
                boolean serviceBound, int visibility, int priority, String iconLocator, int iconFlags,
                String[] parameters, String baseDirectory, String className, String[] classPathExtension,
                TransportProtocol[] transportProtocols, Prefetch prefetch, DiiLocation diiLocation, Vector ipRouting,
                AbstractServiceEntry service, int version, int storagePriority, int launchOrder,
                String[] registeredApi, int[] addressLabels)
        {
            super(valid, controlCode, id, names, versions, serviceBound, visibility, priority, iconLocator, iconFlags,
                    parameters, baseDirectory, className, classPathExtension, transportProtocols, prefetch,
                    diiLocation, ipRouting, registeredApi, addressLabels);

            this.service = service;
            //this.serviceId = service.id;
            this.version = version;
            //this.storagePriority = storagePriority;
            //this.launchOrder = launchOrder;
        }
    }

    public static class TestService extends AbstractServiceEntry
    {
        public boolean valid;

        public TestService(boolean valid, int id, String name, boolean autoSelect)
        {
            this.valid = valid;
            this.id = id;
            this.name = name;
            this.autoSelect = autoSelect;
        }
    }
}
