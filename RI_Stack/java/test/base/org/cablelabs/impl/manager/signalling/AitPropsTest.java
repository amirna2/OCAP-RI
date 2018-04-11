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

package org.cablelabs.impl.manager.signalling;

import org.cablelabs.impl.signalling.Ait;
import org.cablelabs.impl.signalling.AitTest;
import org.cablelabs.impl.signalling.AitTest.AitFactory;
import org.cablelabs.impl.signalling.AitTest.TestAddressingDescriptor;
import org.cablelabs.impl.signalling.AitTest.TestAttributeMapping;
import org.cablelabs.impl.signalling.AitTest.TestExpression;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.AppEntry.DiiLocation;
import org.cablelabs.impl.signalling.AppEntry.IcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.LocalTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.OcTransportProtocol;
import org.cablelabs.impl.signalling.AppEntry.Prefetch;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;

/**
 * Tests <code>AitProps</code>.
 */
public class AitPropsTest extends TestCase
{
    /**
     * Tests constructor. After construction, should look like an empty
     * <code>Ait</code>.
     */
    public void testConstructor()
    {
        AitProps props = new AitProps();
        props.getSignalling().filterApps(new Properties(), new Properties());
        

        AppEntry[] apps = props.getSignalling().getApps();
        assertNotNull("getApps() should return valid array", apps);
        assertEquals("getApps() should return empty array", 0, apps.length);
    }

    /**
     * 
     */
    public void testAddressing()
    {
        AITPropsGenerator propGen = new AITPropsGenerator();

        for (int i = 0; i < AitTest.TEST_ADDRESSABLE_APPS.length; ++i)
        {
            propGen.add(AitTest.TEST_ADDRESSABLE_APPS[i]);
        }

        for (int i = 0; i < AitTest.TEST_ATTRIBUTES.length; ++i)
        {
            propGen.add(AitTest.TEST_ATTRIBUTES[i]);
        }

        for (int i = 0; i < AitTest.TEST_ADDR_DESCRIPTORS.length; ++i)
        {
            propGen.add(AitTest.TEST_ADDR_DESCRIPTORS[i]);
        }

        // Parse
        AitProps aitProps = new AitProps();
        try
        {
            aitProps.parse(propGen.generate(0), -1);
        }
        catch (Exception e)
        {
            fail("Error parsing AIT prop data");
        }
        
        Ait ait = aitProps.getSignalling(); 

        AppEntry[] actualApps;
        AppID[] expectedApps;

        // Modify Properties and run our tests

        // Test 1
        // DescGroup1 -- Highest priority desc is true (addresslabel 11)
        // DescGroup2 -- Highest priority desc is true (addressLabel 15)
        System.setProperty("ocap.cablecard.version", "54");
        System.setProperty("ocap.cablecard.identifier", "105");
        System.setProperty("ocap.hardware.createdate", "05-232008");
        System.setProperty("ocap.cablecard.vct-id", "23455431");
        System.setProperty("ocap.system.highdef", "false");

        ait.filterApps(new Properties(), new Properties());
        actualApps = ait.getApps();

        // Should only have apps with addressLabels 11 (OID=10,AID=11,12) and
        // 15 (OID=10,AID=13)
        expectedApps = new AppID[] { new AppID(10, 11), new AppID(10, 12), new AppID(10, 13) };
        testApps(actualApps, expectedApps);

        // Test 2
        // DescGroup1 -- Second priority desc is true (addresslabel 10)
        // DescGroup2 -- Lowest priority desc is true (addressLabel 16)
        System.setProperty("ocap.cablecard.manufacturer", "12345");
        System.setProperty("ocap.cablecard.version", "55");
        System.setProperty("ocap.system.highdef", "true");
        System.setProperty("ocap.cablecard.vct-id", "23455433");
        System.setProperty("ocap.hardware.serialnum", "1ac34DV.5");

        ait.filterApps(new Properties(), new Properties());
        actualApps = ait.getApps();

        // Should only have apps with addressLabels 10 (OID=10,AID=10,12) and
        // 16 (OID=10,AID=14)
        expectedApps = new AppID[] { new AppID(10, 10), new AppID(10, 12), new AppID(10, 14) };
        testApps(actualApps, expectedApps);
    }

    public void testAddressingNoTrueDescriptors()
    {
        AITPropsGenerator propGen = new AITPropsGenerator();

        for (int i = 0; i < AitTest.TEST_ADDRESSABLE_APPS.length; ++i)
        {
            propGen.add(AitTest.TEST_ADDRESSABLE_APPS[i]);
        }

        for (int i = 0; i < AitTest.TEST_ATTRIBUTES.length; ++i)
        {
            propGen.add(AitTest.TEST_ATTRIBUTES[i]);
        }

        for (int i = 0; i < AitTest.TEST_ADDR_DESCRIPTORS_NO_DEFAULT.length; ++i)
        {
            propGen.add(AitTest.TEST_ADDR_DESCRIPTORS_NO_DEFAULT[i]);
        }

        // Parse
        AitProps aitProps = new AitProps();
        try
        {
            aitProps.parse(propGen.generate(0), -1);
        }
        catch (Exception e)
        {
            fail("Error parsing AIT prop data");
        }
        
        Ait ait = aitProps.getSignalling();

        AppEntry[] actualApps;
        AppID[] expectedApps;

        // Test 1
        // DescGroup1 -- No descriptors are true
        // DescGroup2 -- No descriptors are true
        System.setProperty("ocap.cablecard.manufacturer", "12346");
        System.setProperty("ocap.cablecard.version", "57");
        System.setProperty("ocap.system.highdef", "true");
        System.setProperty("ocap.cablecard.vct-id", "23455433");
        System.setProperty("ocap.hardware.serialnum", "1ac34DV.5");

        ait.filterApps(new Properties(), new Properties());
        actualApps = ait.getApps();

        // Should only have non-addressable apps with no address labels
        // (OID=10,AID=15)
        expectedApps = new AppID[] { new AppID(10, 15) };
        testApps(actualApps, expectedApps);
    }

    public void testAddressingNoDescriptors()
    {
        AITPropsGenerator propGen = new AITPropsGenerator();

        for (int i = 0; i < AitTest.TEST_ADDRESSABLE_APPS.length; ++i)
        {
            propGen.add(AitTest.TEST_ADDRESSABLE_APPS[i]);
        }

        for (int i = 0; i < AitTest.TEST_ATTRIBUTES.length; ++i)
        {
            propGen.add(AitTest.TEST_ATTRIBUTES[i]);
        }

        // Parse
        AitProps aitProps = new AitProps();
        try
        {
            aitProps.parse(propGen.generate(0), -1);
        }
        catch (Exception e)
        {
            fail("Error parsing AIT prop data");
        }
        
        Ait ait = aitProps.getSignalling();

        AppEntry[] actualApps;
        AppID[] expectedApps;

        ait.filterApps(new Properties(), new Properties());
        actualApps = ait.getApps();

        // Should only have non-addressable apps with no address labels
        // (OID=10,AID=15)
        expectedApps = new AppID[] { new AppID(10, 15) };
        testApps(actualApps, expectedApps);
    }

    // Test that the actual list contains exactly the AppIDs from the expected
    // list
    private void testApps(AppEntry[] actual, AppID[] expected)
    {
        // Check that all expected are found in actual
        for (int i = 0; i < expected.length; ++i)
        {
            boolean found = false;
            for (int j = 0; j < actual.length; j++)
            {
                if (expected[i].equals(actual[j].id))
                {
                    found = true;
                    break;
                }
            }
            assertTrue("AppID was expected! " + expected[i], found);
        }

        // Check that all actual are found in expected
        for (int i = 0; i < actual.length; ++i)
        {
            boolean found = false;
            for (int j = 0; j < expected.length; j++)
            {
                if (actual[i].id.equals(expected[j]))
                {
                    found = true;
                    break;
                }
            }
            assertTrue("AppID was not expected! " + actual[i], found);
        }
    }

    /* ==================== Boilerplate ================== */
    public AitPropsTest(String name)
    {
        super(name);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0) return suite();

        TestSuite suite = new TestSuite(AitPropsTest.class.getName());
        for (int i = 0; i < tests.length; ++i)
            suite.addTest(new AitPropsTest(tests[i]));
        return suite;
    }

    public static void main(String[] args)
    {
        try
        {
            junit.textui.TestRunner.run(suite(args));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite(AitPropsTest.class);
        InterfaceTestSuite aitSuite = AitTest.isuite();

        // Generate Factories...

        AppEntry[] apps = AitTest.TEST_APPS;
        for (int size = 0; size <= apps.length; ++size)
        {
            AppEntry[] slice = new AppEntry[size];
            System.arraycopy(apps, 0, slice, 0, size);

            aitSuite.addFactory(new Factory(new AITPropsGenerator(), slice, "Props:" + size));
        }

        suite.addTest(aitSuite);

        return suite;
    }

    public static class Factory extends AitFactory
    {
        protected AITPropsGenerator gen;

        protected String name;

        public Factory(AITPropsGenerator gen, AppEntry[] apps, String name)
        {
            this.gen = gen;
            this.name = name;
            add(apps);
        }

        public void add(AppEntry info)
        {
            gen.add(info);
        }

        protected void generate(AitProps parser) throws Exception
        {
            parser.parse(gen.generate(0), -1);
        }

        protected AitProps createParser()
        {
            return new AitProps();
        }

        public Object createImplObject()
        {
            try
            {
                AitProps parser = createParser();
                generate(parser);
                Ait ait = parser.getSignalling();
                ait.filterApps(new Properties(), new Properties());
                return parser;
            }
            catch (Exception e)
            {
                if (e instanceof RuntimeException) throw (RuntimeException) e;
                throw new RuntimeException(e.toString());
            }
        }

        public String toString()
        {
            return name;
        }
    }

    /**
     * Class used to generate an InputStream suitable for reading into an
     * <code>AitProps</code> Ait.
     */
    public static class AITPropsGenerator
    {
        public Vector applist = new Vector();

        public Vector attributes = new Vector();

        public Vector addressing = new Vector();

        Hashtable transports = new Hashtable();

        Hashtable prefetch = new Hashtable();

        Hashtable dii = new Hashtable();

        public AITPropsGenerator()
        {
        }

        public void add(AppEntry info)
        {
            applist.addElement(info);
        }

        public void add(TestAttributeMapping tam)
        {
            attributes.add(tam);
        }

        public void add(TestAddressingDescriptor tad)
        {
            addressing.add(tad);
        }

        /**
         * Generates an InputStream for the application information that's been
         * added to this AITPropsGenerator.
         * 
         * @returns an <code>InputStream</code> that can be passed to
         *          <code>AitProps.parse()</code>
         */
        public InputStream generate(int version) throws Exception
        {
            Properties props = genProps(version);

            ByteArrayOutputStream os = new ByteArrayOutputStream();

            props.store(os, "");
            os.flush();

            return new ByteArrayInputStream(os.toByteArray());
        }

        protected Properties genProps(int version)
        {
            Properties props = new Properties();

            props.put("version", "0x" + Integer.toHexString(version));

            int i = 0;
            for (Enumeration e = applist.elements(); e.hasMoreElements();)
            {
                AppEntry app = (AppEntry) e.nextElement();
                addApp(props, app, i++);
                saveTransports(app);
            }

            // Gen transports/prefetchs/dii
            genTransports(props);

            // Gen addressable attribute mappings
            genAttributeMappings(props);

            // Gen addressing descriptors
            genAddressingDescriptors(props);

            return props;
        }

        private void saveTransports(AppEntry app)
        {
            if (app.transportProtocols != null)
            {
                for (int i = 0; i < app.transportProtocols.length; ++i)
                {
                    // Assume all TPs are global
                    Integer label = new Integer(app.transportProtocols[i].label);
                    transports.put(label, app.transportProtocols[i]);
                }
            }

            if (app.prefetch != null)
            {
                prefetch.put(new Integer(app.prefetch.transportLabel), app.prefetch);
            }

            if (app.diiLocation != null)
            {
                dii.put(new Integer(app.diiLocation.transportLabel), app.diiLocation);
            }
        }

        private void genAttributeMappings(Properties props)
        {
            int i = 0;
            for (Enumeration e = attributes.elements(); e.hasMoreElements();)
            {
                TestAttributeMapping tam = (TestAttributeMapping) e.nextElement();
                props.put("attribute." + i + ".id", (new Integer(tam.attrID)).toString());
                props.put("attribute." + i + ".name", tam.attrName);
                i++;
            }
        }

        private void genAddressingDescriptors(Properties props)
        {
            int i = 0;
            for (Enumeration e = addressing.elements(); e.hasMoreElements(); i++)
            {
                TestAddressingDescriptor tad = (TestAddressingDescriptor) e.nextElement();
                props.put("addressing." + i + ".group", (new Integer(tad.groupID)).toString());
                props.put("addressing." + i + ".label", (new Integer(tad.addressLabel)).toString());
                props.put("addressing." + i + ".priority", (new Integer(tad.priority)).toString());

                int j = 0;
                for (Enumeration e2 = tad.expressions.elements(); e2.hasMoreElements(); j++)
                {
                    TestExpression te = (TestExpression) e2.nextElement();
                    switch (te.opCode)
                    {
                        case TestExpression.EXPR_OPCODE_AND:
                            props.put("addressing." + i + ".expression." + j, "AND");
                            break;
                        case TestExpression.EXPR_OPCODE_OR:
                            props.put("addressing." + i + ".expression." + j, "OR");
                            break;
                        case TestExpression.EXPR_OPCODE_NOT:
                            props.put("addressing." + i + ".expression." + j, "NOT");
                            break;
                        case TestExpression.EXPR_OPCODE_TRUE:
                            props.put("addressing." + i + ".expression." + j, "TRUE");
                            break;
                        case TestExpression.EXPR_OPCODE_EQ:
                            props.put("addressing." + i + ".expression." + j, (new Integer(te.attrID)).toString()
                                    + " == " + te.attrValue);
                            break;
                        case TestExpression.EXPR_OPCODE_LT:
                            props.put("addressing." + i + ".expression." + j, (new Integer(te.attrID)).toString()
                                    + " < " + te.attrValue);
                            break;
                        case TestExpression.EXPR_OPCODE_LTE:
                            props.put("addressing." + i + ".expression." + j, (new Integer(te.attrID)).toString()
                                    + " <= " + te.attrValue);
                            break;
                        case TestExpression.EXPR_OPCODE_GT:
                            props.put("addressing." + i + ".expression." + j, (new Integer(te.attrID)).toString()
                                    + " > " + te.attrValue);
                            break;
                        case TestExpression.EXPR_OPCODE_GTE:
                            props.put("addressing." + i + ".expression." + j, (new Integer(te.attrID)).toString()
                                    + " >= " + te.attrValue);
                            System.out.println("*** " + props.getProperty("addressing." + i + ".expression." + j));
                            break;
                    }
                }
            }
        }

        private void genTransports(Properties props)
        {
            for (Enumeration e = transports.keys(); e.hasMoreElements();)
            {
                Object key = e.nextElement();

                TransportProtocol tp = (TransportProtocol) transports.get(key);
                props.put("transport." + key + ".service", "" + tp.serviceId);
                props.put("transport." + key + ".remote", "" + tp.remoteConnection);
                if (tp instanceof OcTransportProtocol)
                {
                    OcTransportProtocol oc = (OcTransportProtocol) tp;
                    props.put("transport." + key, "oc");
                    props.put("transport." + key + ".component", "" + oc.componentTag);
                }
                else if (tp instanceof IcTransportProtocol)
                {
                    IcTransportProtocol ic = (IcTransportProtocol) tp;
                    props.put("transport." + key, "ic");
                    //props.put("transport." + key + ".url", ic.url);
                }
                else if (tp instanceof LocalTransportProtocol)
                {
                    props.put("transport." + key, "local");
                }

                Prefetch p = (Prefetch) prefetch.get(key);
                if (p != null)
                {
                    String value = "";
                    String sep = "";
                    for (int i = 0; i < p.info.length; ++i)
                    {
                        value = value + sep + p.info[i].label;
                        sep = ",";
                    }
                    props.put("transport." + key + ".prefetch", value);
                }

                DiiLocation d = (DiiLocation) dii.get(key);
                if (d != null)
                {
                    for (int i = 0; i < d.diiIdentification.length; ++i)
                    {
                        props.put("transport." + key + ".dii." + i, d.diiIdentification[i] + "," + d.associationTag[i]);
                    }
                }
            }
        }

        protected void putValue(Properties props, int i, String key, String value)
        {
            if (value != null) props.put("app." + i + "." + key, value);
        }

        protected void putValue(Properties props, int i, String key, int value)
        {
            putValue(props, i, key, "0x" + Integer.toHexString(value));
        }

        protected void putValue(Properties props, int i, String key, long value)
        {
            putValue(props, i, key, "0x" + Long.toHexString(value));
        }
        
        protected String acc(AppEntry app)
        {
            switch (app.controlCode)
            {
                case OcapAppAttributes.AUTOSTART:
                    return "AUTOSTART";
                case OcapAppAttributes.REMOTE:
                    return "REMOTE";
                case OcapAppAttributes.PRESENT:
                    return "PRESENT";
                case OcapAppAttributes.KILL:
                    return "KILL";
                case OcapAppAttributes.DESTROY:
                    return "DESTROY";
            }
            return "";
        }

        protected String visibility(AppEntry app)
        {
            switch (app.visibility)
            {
                case AppEntry.NON_VISIBLE:
                    return "INVISIBLE";
                case AppEntry.LISTING_ONLY:
                    return "VISIBLE-TO-APPS-ONLY";
                case AppEntry.VISIBLE:
                    return "VISIBLE";
            }
            return "";
        }

        protected boolean addApp(Properties props, AppEntry app, int i)
        {
            // Currently, AitProps doesn't skip an app given bad data
            // This will cause us to fail the test
            // So, we won't generate such entries here...
            if (app.transportProtocols == null || app.transportProtocols.length == 0 || app.className == null
                    || app.className.length() == 0) return false;

            putValue(props, i, "application_identifier", "0x" + app.id.toString());
            putValue(props, i, "application_control_code", acc(app));
            String name = (String) app.names.get("eng");
            if (name != null && app.names.size() == 1)
                putValue(props, i, "application_name", (String) app.names.get("eng"));
            else
            {
                int idx = 0;
                for (Enumeration e = app.names.keys(); e.hasMoreElements();)
                {
                    String lang = (String) e.nextElement();
                    name = (String) app.names.get(lang);
                    putValue(props, i, "application_name." + idx, lang + "," + name);
                    ++idx;
                }
            }
            putValue(props, i, "service_bound", app.serviceBound ? "true" : "false");
            putValue(props, i, "visibility", visibility(app));
            putValue(props, i, "priority", app.priority);

            if (app.iconLocator != null)
            {
                putValue(props, i, "icon_locator", app.iconLocator);
                putValue(props, i, "icon_flags", app.iconFlags);
            }

            if (app.parameters != null) for (int j = 0; j < app.parameters.length; ++j)
            {
                putValue(props, i, "args." + j, app.parameters[j]);
            }

            putValue(props, i, "base_directory", app.baseDirectory);
            putValue(props, i, "initial_class_name", app.className);
            if (app.classPathExtension != null)
            {
                StringBuffer classpath = new StringBuffer();
                String sep = "";
                for (int j = 0; j < app.classPathExtension.length; ++j)
                {
                    classpath.append(sep);
                    classpath.append(app.classPathExtension[j]);
                    sep = ";";
                }
                putValue(props, i, "classpath_extension", classpath.toString());
            }

            // Address labels
            if (app.addressLabels != null && app.addressLabels.length > 0)
            {
                for (int j = 0; j < app.addressLabels.length; j++)
                {
                    putValue(props, i, "address_label." + j, "" + app.addressLabels[j]);
                }
            }

            // TPs
            if (app.transportProtocols != null)
            {
                String sep = "";
                String list = "";
                for (int tpi = 0; tpi < app.transportProtocols.length; ++tpi)
                {
                    list = list + sep + app.transportProtocols[tpi].label;
                    sep = ",";
                }
                putValue(props, i, "transport", list);
            }

            // Prefetch
            // Dii

            // OCAP:registeredApi
            if (app.registeredApi != null)
            {
                for (int api = 0; api < app.registeredApi.length; ++api)
                {
                    putValue(props, i, "api." + api, app.registeredApi[api]);
                }
            }

            return true;
        }
    }
}
