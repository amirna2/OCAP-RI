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

import org.cablelabs.impl.signalling.AppEntry.DiiLocation;
import org.cablelabs.impl.signalling.AppEntry.IcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.LocalTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.OcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.Prefetch;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;

import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import net.sourceforge.groboutils.junit.v1.iftc.ImplFactory;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestCase;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;

/**
 * Tests <code>Ait</code> implementations.
 */
public class AitTest extends InterfaceTestCase
{
    /**
     * Tests getExternalAuthorization.
     */
    public void testGetExternalAuthorization()
    {
        assertEquals("Unexpected externalAuthorization", factory.getExternalAuthorization(),
                ait.getExternalAuthorization());
    }

    /**
     * Tests getApps().
     */
    public void testGetApps()
    {
        Vector vector = factory.getAppEntries();

        AppEntry apps[] = ait.getApps();

        assertNotNull("should always return a valid array", apps);

        assertEquals("Expected same number of apps", vector.size(), apps.length);

        for (Enumeration e = vector.elements(); e.hasMoreElements();)
        {
            AppEntry original = (AppEntry) e.nextElement();
            AppEntry app = null;

            // Locate app with same AppID
            for (int i = 0; i < apps.length; ++i)
            {
                if (apps[i] != null && original.id.equals(apps[i].id))
                {
                    app = apps[i];
                    apps[i] = null;
                    break;
                }
            }
            assertNotNull("Could not find " + original.id, app);

            assertEquals("App[" + original.id + "]", original, app);
        }

        // Shouldn't have left-overs as we checked the size
    }

    protected static void assertEquals(String msg, Ait.ExternalAuthorization[] expected,
            Ait.ExternalAuthorization[] actual)
    {
        assertEquals(msg + ": unexpected Ait.ExternalAuthorization[].length", expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i)
        {
            assertEquals(msg + ": unexpected Ait.ExternalAuthorization[" + i + "].id", expected[i].id, actual[i].id);
            assertEquals(msg + ": unexpected Ait.ExternalAuthorization[" + i + "].priority", expected[i].priority,
                    actual[i].priority);
        }
    }

    protected static void assertEquals(String msg, int[] expected, int[] actual)
    {
        assertEquals(msg + ": unexpected int[].length", expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i)
            assertEquals(msg + ": unexpected int[" + i + "]", expected[i], actual[i]);
    }

    protected static void assertEquals(String msg, String[] expected, String[] actual)
    {
        assertEquals(msg + ": unexpected String[].length", expected.length, actual.length);
        for (int i = 0; i < expected.length; ++i)
            assertEquals(msg + ": unexpected String[" + i + "]", expected[i], actual[i]);
    }

    protected void assertEquals(String msg, TransportProtocol[] expected, TransportProtocol[] actual)
    {
        if (expected == null)
            assertSame(msg, expected, actual);
        else
        {
            assertFalse(msg + ": did not expect EXACT same protocols", expected == actual);

            assertNotNull(msg, actual);

            assertEquals(msg + ".length", expected.length, actual.length);
            for (int i = 0; i < expected.length; ++i)
                assertEquals(msg + i, expected[i], actual[i]);
        }
    }

    protected void assertEquals(String msg, TransportProtocol expected, TransportProtocol actual)
    {
        if (expected == null)
            assertSame(msg, expected, actual);
        else
        {
            assertFalse(msg + ": did not expect EXACT same protocol", expected == actual);

            assertEquals(msg + ": label", expected.label, actual.label);
            assertEquals(msg + ": remote", expected.remoteConnection, actual.remoteConnection);
            assertEquals(msg + ": service", expected.serviceId, actual.serviceId);
            if (expected instanceof OcTransportProtocol)
            {
                OcTransportProtocol ex = (OcTransportProtocol) expected;
                OcTransportProtocol ac = (OcTransportProtocol) actual;

                assertEquals(msg + ": comp", ex.componentTag, ac.componentTag);
            }
            else if (expected instanceof IcTransportProtocol)
            {
                IcTransportProtocol ex = (IcTransportProtocol) expected;
                IcTransportProtocol ac = (IcTransportProtocol) actual;

                //assertEquals(msg + ": url", ex.url, ac.url);
            }
            else if (expected instanceof LocalTransportProtocol)
            {
                // Don't do anything
            }
        }
    }

    protected void assertEquals(String msg, URL[] expected, URL[] actual)
    {
        if (expected == null)
            assertSame(msg, expected, actual);
        else
        {
            assertFalse(msg + ": did not expect EXACT same urls", expected == actual);
            assertNotNull(msg, actual);
            assertEquals(msg + ".length", expected.length, actual.length);

            for (int i = 0; i < expected.length; ++i)
                assertEquals(msg + i, expected[i], actual[i]);
        }
    }

    protected void assertEquals(String msg, Prefetch expected, Prefetch actual)
    {
        if (expected == null)
            assertSame(msg, expected, actual);
        else
        {
            assertFalse(msg + ": did not expect EXACT same prefetch", expected == actual);
            assertNotNull(msg, actual);

            assertEquals(msg + ": label", expected.transportLabel, actual.transportLabel);

            if (expected.info == null)
                assertSame(msg + ": info", expected.info, actual.info);
            else
            {
                assertFalse(msg + ": did not expect EXACT same prefetch", expected.info == actual.info);
                assertNotNull(msg, actual.info);

                for (int i = 0; i < expected.info.length; ++i)
                {
                    assertEquals(msg + ": info" + i, expected.info[i].label, actual.info[i].label);
                    // Ignore actual priority
                    // assertEquals(msg + ": priority"+i,
                    // expected.info[i].priority, actual.info[i].priority);
                }
            }
        }
    }

    protected void assertEquals(String msg, DiiLocation expected, DiiLocation actual)
    {
        if (expected == null)
            assertSame(msg, expected, actual);
        else
        {
            assertFalse(msg + ": did not expect EXACT same dii", expected == actual);
            assertNotNull(msg, actual);

            assertEquals(msg + ": label", expected.transportLabel, actual.transportLabel);

            assertEquals(msg + ": dii", expected.diiIdentification, actual.diiIdentification);
            assertEquals(msg + ": assoc", expected.associationTag, actual.associationTag);
        }
    }

    /**
     * Can be overridden by subclass (XaitTest) to ensure that REMOTE is treated
     * as PRESENT.
     */
    protected void checkControlCode(String msg, int expected, int actual)
    {
        assertEquals(msg, expected, actual);
    }

    protected void assertEquals(String msg, AppEntry expected, AppEntry actual)
    {
        checkControlCode(msg + ": unexpected controlCode", expected.controlCode, actual.controlCode);
        assertEquals(msg + ": unexpected id", expected.id, actual.id);
        assertEquals(msg + ": expected same number of names", expected.names.size(), actual.names.size());
        for (Enumeration e = expected.names.keys(); e.hasMoreElements();)
        {
            String key = (String) e.nextElement();
            assertEquals(msg + ": expected name for " + key, expected.names.get(key), actual.names.get(key));
        }
        if (expected.versions == null)
            assertEquals(msg + ": expected empty versions set", 0, actual.versions.size());
        else
            assertEquals(msg + ": expected same number of versions", expected.versions.size(), actual.versions.size());
        for (Enumeration e = expected.versions.keys(); e.hasMoreElements();)
        {
            Integer key = (Integer) e.nextElement();
            int[] ver0 = (int[]) expected.versions.get(key);
            int[] ver1 = (int[]) actual.versions.get(key);

            assertEquals(msg + ": expected same versions", ver0, ver1);
        }
        assertEquals(msg + ": unexpected serviceBound", expected.serviceBound, actual.serviceBound);
        assertEquals(msg + ": unexpected visibility", expected.visibility, actual.visibility);
        assertEquals(msg + ": unexpected priority", expected.priority, actual.priority);
        if (expected.iconLocator == null)
            assertTrue(msg + ": expected null iconLocator", null == actual.iconLocator);
        else
        {
            assertEquals(msg + ": unexpected iconLocator", expected.iconLocator, actual.iconLocator);
            assertEquals(msg + ": unexpected iconFlags", expected.iconFlags, actual.iconFlags);
        }

        assertEquals(msg + ": unexpected params", expected.parameters, actual.parameters);
        assertEquals(msg + ": unexpected baseDirectory", expected.baseDirectory, actual.baseDirectory);
        assertEquals(msg + ": unexpected className", expected.className, actual.className);
        assertEquals(msg + ": unexpected classPathExtension", expected.classPathExtension, actual.classPathExtension);

        // transportProtocols
        assertEquals(msg + ": unexpected tps", expected.transportProtocols, actual.transportProtocols);

        // prefetch
        assertEquals(msg + ": unexpected prefetch", expected.prefetch, actual.prefetch);

        // diiLocation
        assertEquals(msg + ": unexpected diiLocation", expected.diiLocation, actual.diiLocation);

        // ipRouting

        // OCAP:registeredApi descriptor
        assertNotNull(msg + ": null registeredApi", actual.registeredApi);
        if (expected.registeredApi != null)
        {
            assertEquals(msg + ": registeredApi", expected.registeredApi, actual.registeredApi);
        }
        else
        {
            assertEquals(msg + ": unexpected registeredApi", 0, actual.registeredApi.length);
        }
    }

    /* ================== boilerplate =================== */

    protected Ait createAit()
    {
        return (Ait) createImplObject();
    }

    protected Ait ait;

    protected AitFactory factory;

    protected void setUp() throws Exception
    {
        super.setUp();

        ait = createAit();
    }

    protected void tearDown() throws Exception
    {
        ait = null;
        super.tearDown();
    }

    public static InterfaceTestSuite isuite() // throws Exception
    {
        InterfaceTestSuite suite = new InterfaceTestSuite(AitTest.class);
        suite.setName(Ait.class.getName());
        return suite;
    }

    public AitTest(String name, ImplFactory f)
    {
        this(name, Ait.class, f);
    }

    protected AitTest(String name, Class impl, ImplFactory f)
    {
        super(name, impl, f);
        factory = (AitFactory) f;
    }

    /* ================== samples =================== */

    public static final TransportProtocol TP0[] = { new TestOc(5, false, 0, 0x73),
            new TestIc(12, "http://127.0.0.1/apps/"), };

    public static final TransportProtocol TP1[] = { new TestOc(6, false, 0, 0x73),
    // Don't bother testing IP... mainly because it's extra info...
            /*
             * new TestIp(7, true, 0x456, new String[] {
             * "http://www.cablelabs.org/apps/apps1",
             * "http://www.cablelabs.org/common" }),
             */
            new TestOc(9, true, 99, 0x73), new TestIc(13, "http://www.cablelabs.org/apps/"), };

    public static final DiiLocation DII0 = new TestDii(5, new int[] { 1, 2, 3 }, new int[] { 4, 5, 6 });

    public static final DiiLocation DII1 = new TestDii(9, new int[] { 9 }, new int[] { 10 });

    public static final Prefetch PREFETCH0 = new TestPrefetch(5, new TestPair[] { new TestPair("m1", 20),
            new TestPair("m2", 19), new TestPair("m3", 18), new TestPair("m4", 16) });

    public static final Prefetch PREFETCH1 = new TestPrefetch(9, new TestPair[] { new TestPair("m1", 1) });

    static final Hashtable VERSIONS0 = new Hashtable();

    public static final TestApp[] TEST_APPS = {
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 9), new Hash(new String[] { "eng", "App0" }),
                    VERSIONS0, true, AppEntry.VISIBLE, 100, null, 0, new String[] { "arg1", "arg2" }, "/",
                    "org.cablelabs.app0.MainXlet", new String[0], TP0, PREFETCH0, DII0, null, null, new int[] {}),
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 10), new Hash(new String[] { "spa", "App Uno",
                    "eng", "App1" }), VERSIONS0, true, AppEntry.VISIBLE, 99, "icons", 0xF, new String[0], "/app1",
                    "org.cablelabs.app1.MainXlet", new String[0], TP0, PREFETCH0, DII0, null, new String[0],
                    new int[] {}),
            new TestApp(
                    false, // No TPs
                    OcapAppAttributes.PRESENT, new AppID(10, 11), new Hash(new String[] { "eng", "App2" }), VERSIONS0,
                    true, AppEntry.VISIBLE, 101, "icons", 0xF, new String[0], "/app2", "org.cablelabs.app1.MainXlet",
                    new String[0], null, null, null, null, new String[0], new int[] {}),
            new TestApp(
                    false, // No classname
                    OcapAppAttributes.PRESENT, new AppID(10, 12), new Hash(new String[] { "eng", "App3" }), VERSIONS0,
                    true, AppEntry.VISIBLE, 102, "icons", 0xF, new String[0], "/app3", null, new String[0], TP0, null,
                    null, null, new String[0], new int[] {}),
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 13), new Hash(new String[] { "spa", "Quatro",
                    "eng", "App4" }), VERSIONS0, true, AppEntry.VISIBLE, 103, "icons", 0xF, new String[0],
                    "/app4/classes", "MainXlet", new String[] { "moreclasses", "/" }, TP1, PREFETCH1, DII1, null,
                    new String[] { "com.a.b" }, new int[] {}),
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 14), new Hash(new String[] { "eng", "App5" }),
                    VERSIONS0, true, AppEntry.VISIBLE, 105, null, 0, new String[0], "app5", "MainXlet",
                    new String[] { "/" }, TP0, PREFETCH0, DII0, null, new String[] { "com.a.b", "com.a.c" },
                    new int[] {}),
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 15), new Hash(new String[] { "eng", "App6" }),
                    VERSIONS0, true, AppEntry.NON_VISIBLE, 199, null, 0, new String[0], "/", "A", new String[0], TP1,
                    PREFETCH1, DII1, null, new String[] { "com.a.c", "com.a.b", "com.x.y.z" }, new int[] {}), };

    public static final TestApp[] TEST_ADDRESSABLE_APPS = {
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 9), new Hash(new String[] { "eng", "App0" }),
                    VERSIONS0, true, AppEntry.VISIBLE, 100, null, 0, new String[] { "arg1", "arg2" }, "/",
                    "org.cablelabs.app0.MainXlet", new String[0], TP0, PREFETCH0, DII0, null, null, new int[] { 18 }),
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 10), new Hash(new String[] { "spa", "App Uno",
                    "eng", "App1" }), VERSIONS0, true, AppEntry.VISIBLE, 99, "icons", 0xF, new String[0], "/app1",
                    "org.cablelabs.app1.MainXlet", new String[0], TP0, PREFETCH0, DII0, null, new String[0],
                    new int[] { 10 }),
            new TestApp(
                    false, // No TPs
                    OcapAppAttributes.PRESENT, new AppID(10, 11), new Hash(new String[] { "eng", "App2" }), VERSIONS0,
                    true, AppEntry.VISIBLE, 101, "icons", 0xF, new String[0], "/app2", "org.cablelabs.app1.MainXlet",
                    new String[0], TP0, null, null, null, new String[0], new int[] { 11 }),
            new TestApp(
                    false, // No classname
                    OcapAppAttributes.PRESENT, new AppID(10, 12), new Hash(new String[] { "eng", "App3" }), VERSIONS0,
                    true, AppEntry.VISIBLE, 102, "icons", 0xF, new String[0], "/app3", "MainXlet", new String[0], TP0,
                    null, null, null, new String[0], new int[] { 10, 11 }),
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 13), new Hash(new String[] { "spa", "Quatro",
                    "eng", "App4" }), VERSIONS0, true, AppEntry.VISIBLE, 103, "icons", 0xF, new String[0],
                    "/app4/classes", "MainXlet", new String[] { "moreclasses", "/" }, TP1, PREFETCH1, DII1, null,
                    new String[] { "com.a.b" }, new int[] { 15 }),
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 14), new Hash(new String[] { "eng", "App5" }),
                    VERSIONS0, true, AppEntry.VISIBLE, 105, null, 0, new String[0], "app5", "MainXlet",
                    new String[] { "/" }, TP0, PREFETCH0, DII0, null, new String[] { "com.a.b", "com.a.c" },
                    new int[] { 16 }),
            new TestApp(true, OcapAppAttributes.PRESENT, new AppID(10, 15), new Hash(new String[] { "eng", "App6" }),
                    VERSIONS0, true, AppEntry.NON_VISIBLE, 199, null, 0, new String[0], "/", "A", new String[0], TP1,
                    PREFETCH1, DII1, null, new String[] { "com.a.c", "com.a.b", "com.x.y.z" }, new int[] {}), };

    public static class TestApp extends AppEntry
    {
        public boolean valid;

        /** Adds a constructor... */
        public TestApp(boolean valid, int controlCode, AppID id, Hashtable names, Hashtable versions,
                boolean serviceBound, int visibility, int priority, String iconLocator, int iconFlags,
                String[] parameters, String baseDirectory, String className, String[] classPathExtension,
                TransportProtocol[] transportProtocols, Prefetch prefetch, DiiLocation diiLocation, Vector ipRouting,
                String[] registeredApi, int[] addressLabels)
        {
            this.valid = valid;

            this.controlCode = controlCode;
            this.id = id;
            this.names = names;
            this.versions = versions;
            this.serviceBound = serviceBound;
            this.visibility = visibility;
            this.priority = priority;
            this.iconLocator = iconLocator;
            this.iconFlags = iconFlags;
            this.parameters = parameters;
            this.baseDirectory = baseDirectory;
            this.className = className;
            this.classPathExtension = classPathExtension;
            this.transportProtocols = transportProtocols;
            this.prefetch = prefetch;
            this.diiLocation = diiLocation;
            this.ipRouting = ipRouting;
            this.registeredApi = registeredApi;
            this.addressLabels = addressLabels;
        }
    }

    public static class Hash extends Hashtable
    {
        public Hash(Object[] keysAndValues)
        {
            for (int i = 0; i < keysAndValues.length; i += 2)
            {
                put(keysAndValues[i], keysAndValues[i + 1]);
            }
        }
    }

    public static final TestAttributeMapping[] TEST_ATTRIBUTES = {
            new TestAttributeMapping(15, "ocap.cablecard.manufacturer"),
            new TestAttributeMapping(16, "ocap.cablecard.version"),
            new TestAttributeMapping(17, "ocap.cablecard.identifier"),
            new TestAttributeMapping(18, "ocap.cablecard.vct-id"), new TestAttributeMapping(19, "ocap.system.highdef"),
            new TestAttributeMapping(20, "ocap.hardware.createdate"),
            new TestAttributeMapping(21, "ocap.hardware.serialnum") };

    public static class TestAttributeMapping
    {
        public TestAttributeMapping(int attrID, String attrName)
        {
            this.attrID = attrID;
            this.attrName = attrName;
        }

        public int attrID;

        public String attrName;
    }

    public static final TestExpression TEST_EXPR_AND = new TestExpression(TestExpression.EXPR_OPCODE_AND);

    public static final TestExpression TEST_EXPR_OR = new TestExpression(TestExpression.EXPR_OPCODE_OR);

    public static final TestExpression TEST_EXPR_NOT = new TestExpression(TestExpression.EXPR_OPCODE_NOT);

    public static final TestExpression TEST_EXPR_TRUE = new TestExpression(TestExpression.EXPR_OPCODE_TRUE);

    public static final TestExpression[] TEST_EXPRESSIONS = {
            new TestExpression(TestExpression.EXPR_OPCODE_EQ, false, 15, "12345"), // 0
            new TestExpression(TestExpression.EXPR_OPCODE_LT, false, 16, "55"), // 1
            new TestExpression(TestExpression.EXPR_OPCODE_LTE, false, 16, "55"), // 2
            new TestExpression(TestExpression.EXPR_OPCODE_GT, false, 17, "105"), // 3
            new TestExpression(TestExpression.EXPR_OPCODE_GTE, false, 17, "105"), // 4
            new TestExpression(TestExpression.EXPR_OPCODE_LT, false, 18, "23455432"), // 5
            new TestExpression(TestExpression.EXPR_OPCODE_EQ, false, 19, "false"), // 6
            new TestExpression(TestExpression.EXPR_OPCODE_EQ, false, 19, "true"), // 7
            new TestExpression(TestExpression.EXPR_OPCODE_EQ, false, 20, "05-232008"), // 8
            new TestExpression(TestExpression.EXPR_OPCODE_EQ, false, 21, "1ac34DV.5") // 9
    };

    public static Vector TEST_EXPR1 = new Vector();

    public static Vector TEST_EXPR2 = new Vector();

    public static Vector TEST_EXPR3 = new Vector();

    public static Vector TEST_EXPR4 = new Vector();

    public static Vector TEST_EXPR5 = new Vector();

    // Build our expressions for use in AddressingDescriptors
    static
    {
        /**
         * CCManuf == 12345 && HighDef
         */
        TEST_EXPR1.add(TEST_EXPRESSIONS[0]);
        TEST_EXPR1.add(TEST_EXPRESSIONS[7]);
        TEST_EXPR1.add(TEST_EXPR_AND);

        /**
         * CCVers < 55 && HWCreateDate == 05-232008 && CCID >= 105
         */
        TEST_EXPR2.add(TEST_EXPRESSIONS[1]);
        TEST_EXPR2.add(TEST_EXPRESSIONS[8]);
        TEST_EXPR2.add(TEST_EXPR_AND);
        TEST_EXPR2.add(TEST_EXPRESSIONS[4]);
        TEST_EXPR2.add(TEST_EXPR_AND);

        /**
         * (CCVers <= 55 || CCID > 105) && !(HWSerialNum == 1ac34DV.5)
         */
        TEST_EXPR3.add(TEST_EXPRESSIONS[2]);
        TEST_EXPR3.add(TEST_EXPRESSIONS[3]);
        TEST_EXPR3.add(TEST_EXPR_OR);
        TEST_EXPR3.add(TEST_EXPRESSIONS[9]);
        TEST_EXPR3.add(TEST_EXPR_NOT);
        TEST_EXPR3.add(TEST_EXPR_AND);

        /**
         * CCVCTID < 23455432 || !HighDef
         */
        TEST_EXPR4.add(TEST_EXPRESSIONS[5]);
        TEST_EXPR4.add(TEST_EXPRESSIONS[6]);
        TEST_EXPR4.add(TEST_EXPR_OR);

        /**
         * TRUE
         */
        TEST_EXPR5.add(TEST_EXPR_TRUE);
    }

    public static class TestExpression
    {
        public static final int EXPR_OPCODE_LT = 0x11;

        public static final int EXPR_OPCODE_LTE = 0x12;

        public static final int EXPR_OPCODE_EQ = 0x13;

        public static final int EXPR_OPCODE_GTE = 0x14;

        public static final int EXPR_OPCODE_GT = 0x15;

        public static final int EXPR_OPCODE_AND = 0x31;

        public static final int EXPR_OPCODE_OR = 0x32;

        public static final int EXPR_OPCODE_NOT = 0x33;

        public static final int EXPR_OPCODE_TRUE = 0x34;

        public TestExpression(int opCode) // LogicalOp
        {
            this.opCode = opCode;
        }

        public TestExpression(int opCode, boolean isSecurityAttribute, int attrID, String attrValue) // Comparison
        {
            this.opCode = opCode;
            this.isSecurityAttribute = isSecurityAttribute;
            this.attrID = attrID;
            this.attrValue = attrValue;
        }

        public int opCode;

        public boolean isSecurityAttribute;

        public int attrID;

        public String attrValue;
    }

    public static TestAddressingDescriptor[] TEST_ADDR_DESCRIPTORS = {
            new TestAddressingDescriptor(1000, 0, 15, TEST_EXPR5),
            new TestAddressingDescriptor(1000, 5, 10, TEST_EXPR1),
            new TestAddressingDescriptor(1000, 50, 11, TEST_EXPR2),
            new TestAddressingDescriptor(2000, 0, 16, TEST_EXPR5),
            new TestAddressingDescriptor(2000, 5, 18, TEST_EXPR3),
            new TestAddressingDescriptor(2000, 50, 15, TEST_EXPR4) };

    public static TestAddressingDescriptor[] TEST_ADDR_DESCRIPTORS_NO_DEFAULT = {
            new TestAddressingDescriptor(1000, 5, 10, TEST_EXPR1),
            new TestAddressingDescriptor(1000, 50, 11, TEST_EXPR2),
            new TestAddressingDescriptor(2000, 5, 18, TEST_EXPR3),
            new TestAddressingDescriptor(2000, 50, 15, TEST_EXPR4) };

    public static class TestAddressingDescriptor
    {
        public TestAddressingDescriptor(int groupID, int priority, int addressLabel, Vector expressions)
        {
            this.groupID = groupID;
            this.priority = priority;
            this.addressLabel = addressLabel;
            this.expressions = expressions;
        }

        public int groupID;

        public int priority;

        public int addressLabel;

        public Vector expressions;
    }

    public static class TestOc extends OcTransportProtocol
    {
        public TestOc(int label, boolean remoteConnection, int serviceId, int componentTag)
        {
            this.protocol = 1;
            this.label = label;
            this.remoteConnection = remoteConnection;
            this.serviceId = serviceId;
            this.componentTag = componentTag;
        }
    }

    public static class TestIc extends IcTransportProtocol
    {
        public TestIc(int label, String url)
        {
            this.protocol = 0x0101;
            this.label = label;
            this.urls.add(url);
        }
    }

    public static class TestDii extends DiiLocation
    {
        public TestDii(int label, int dii[], int assoc[])
        {
            this.transportLabel = label;
            this.diiIdentification = dii;
            this.associationTag = assoc;
        }
    }

    public static class TestPrefetch extends Prefetch
    {
        public TestPrefetch(int label, Prefetch.Pair[] info)
        {
            this.transportLabel = label;
            this.info = info;
        }
    }

    public static class TestPair extends Prefetch.Pair
    {
        public TestPair(String label, int priority)
        {
            this.label = label;
            this.priority = priority;
        }
    }

    public static abstract class AitFactory implements ImplFactory
    {
        protected Vector vector;

        public AitFactory()
        {
            vector = new Vector();
        }

        protected void add(AppEntry[] apps)
        {
            for (int i = 0; i < apps.length; ++i)
            {
                addAppEntry(apps[i]);
                add(apps[i]);
            }
        }

        protected void addAppEntry(AppEntry app)
        {
            if (!(app instanceof TestApp) || ((TestApp) app).valid) vector.add(app);
        }

        public Vector getAppEntries()
        {
            return vector;
        }

        public int getType()
        {
            return OcapAppAttributes.OCAP_J;
        }

        public Ait.ExternalAuthorization[] getExternalAuthorization()
        {
            return new Ait.ExternalAuthorization[0];
        }

        protected abstract void add(AppEntry app);
    }
}
