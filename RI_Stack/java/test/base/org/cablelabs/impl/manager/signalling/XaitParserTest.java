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

import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.AitTest;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.signalling.Xait;
import org.cablelabs.impl.signalling.XaitTest;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.util.MPEEnv;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import junit.framework.Test;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

/**
 * Tests <code>XaitParser</code>.
 * 
 * @author Aaron Kamienski
 */
public class XaitParserTest extends AitParserTest
{
    /**
     * Tests constructor. After construction, should look like an empty
     * <code>Ait</code>.
     */
    public void testConstructor()
    {
        XaitParser parser = new XaitParser(Xait.NETWORK_SIGNALLING);
        Xait xait = (Xait)parser.getSignalling();
        xait.filterApps(new Properties(), new Properties());

        AppEntry[] apps = xait.getApps();
        assertNotNull("getApps() should return valid array", apps);
        assertEquals("getApps() should return empty array", 0, apps.length);

        AbstractServiceEntry[] services = xait.getServices();
        assertNotNull("getServices() should return valid array", services);
        assertEquals("getServices() should return empty array", 0, services.length);
    }

    /* ==================== Boilerplate ================== */
    public XaitParserTest(String name)
    {
        super(name);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0)
            return suite();
        else
        {
            TestSuite suite = new TestSuite(XaitParserTest.class.getName());
            for (int i = 0; i < tests.length; ++i)
                suite.addTest(new XaitParserTest(tests[i]));
            return suite;
        }
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
        TestSuite suite = new TestSuite(XaitParserTest.class);
        InterfaceTestSuite aitSuite = XaitTest.isuite();

        // TODO: test with real certificate hashes, not these dummy bytes
        byte[][] priv = new byte[2][20];
        for (int i = 0; i < priv.length; ++i)
            for (int j = 0; j < priv[i].length; ++j)
                priv[i][j] = (byte) ('A' + ((i + j) % 26));

        // Generate Factories...
        AppEntry[] apps = XaitTest.TEST_APPS;
        for (int size = 0; size <= apps.length; ++size)
        {
            AppEntry[] slice = new AppEntry[size];
            System.arraycopy(apps, 0, slice, 0, size);

            aitSuite.addFactory(new InputStreamFactory(new XAITGenerator(), slice, "InputStream:" + size, priv));
            aitSuite.addFactory(new SectionFactory(new XAITGenerator(), slice, "Section[1]:" + size, priv));

            // Test TPs in common area
            XAITGenerator gen = new XAITGenerator();
            gen.addCommon(AitTest.TP0);
            gen.addCommon(AitTest.TP1);
            aitSuite.addFactory(new InputStreamFactory(gen, slice, "InputStream(common):" + size, priv));
            gen = new XAITGenerator();
            gen.addCommon(AitTest.TP0);
            gen.addCommon(AitTest.TP1);
            aitSuite.addFactory(new SectionFactory(gen, slice, "Section[1]:" + size, priv));

            // Test multi-section w/ common descriptors (See bug4801)
            aitSuite.addFactory(new InputStreamFactory(XAITGenerator.class, new TransportProtocol[][] { AitTest.TP0,
                    AitTest.TP1 }, slice, "InputStream[multi]:" + size, priv));
            aitSuite.addFactory(new SectionFactory(XAITGenerator.class, new TransportProtocol[][] { AitTest.TP0,
                    AitTest.TP1 }, slice, "Section[multi]:" + size, priv));
        }

        suite.addTest(aitSuite);

        return suite;
    }

    public static class InputStreamFactory extends AitParserTest.InputStreamFactory implements XaitTest.XaitFactory
    {
        public InputStreamFactory(AITGenerator gen, AppEntry[] apps, String name, byte[][] priv)
        {
            super(gen, apps, name);

            init(gen, apps, priv);
        }

        protected void init(AITGenerator xaitGen, AppEntry[] apps, byte[][] priv)
        {
            // Want to add each service only ONCE
            // So use a hashtable to get a single copy of each
            Hashtable services = new Hashtable();
            for (Enumeration e = vector.elements(); e.hasMoreElements();)
            {
                Object obj = e.nextElement();
                if (obj instanceof XaitTest.TestApp)
                {
                    XaitTest.TestApp app = (XaitTest.TestApp) obj;
                    Integer key = new Integer(app.service.id);

                    // Skip if seen this service id before
                    if (services.get(key) != null) continue;
                    services.put(key, app.service);

                    ((XAITGenerator) xaitGen).add(app.service);
                }
            }

            // Remember certificate bytes
            for (int i = 0; i < priv.length; ++i)
            {
                ((XAITGenerator) xaitGen).add(priv[i]);
            }
            privCertBytes = new byte[priv.length * 20];
            for (int i = 0; i < priv.length; ++i)
            {
                System.arraycopy(priv[i], 0, privCertBytes, i * 20, 20);
            }
        }

        public InputStreamFactory(Class genClass, TransportProtocol[][] tp, AppEntry[] apps, String name, byte[][] priv)
        {
            super(genClass, tp, apps, name);

            // Drop services & priv bytes into first section
            init(gen[0], apps, priv);
        }

        public Vector getServices()
        {
            if (gen.length == 1)
                return ((XAITGenerator) gen[0]).svclist;
            else
            {
                Vector v = new Vector();
                for (int i = 0; i < gen.length; ++i)
                    v.addAll(((XAITGenerator) gen[i]).svclist);
                return v;
            }
        }

        public int getSource()
        {
            return Xait.NETWORK_SIGNALLING;
        }

        public byte[] getPrivilegedCertificates()
        {
            return privCertBytes;
        }

        protected AitParser createParser()
        {
            return new XaitParser(Xait.NETWORK_SIGNALLING);
        }

        private byte[] privCertBytes;
    }

    public static class SectionFactory extends AitParserTest.SectionFactory implements XaitTest.XaitFactory
    {
        public SectionFactory(AITGenerator gen, AppEntry[] apps, String name, byte[][] priv)
        {
            super(gen, apps, name);

            init(gen, apps, priv);
        }

        protected void init(AITGenerator xaitGen, AppEntry[] apps, byte[][] priv)
        {
            // Want to add each service only ONCE
            // So use a hashtable to get a single copy of each
            Hashtable services = new Hashtable();
            for (Enumeration e = vector.elements(); e.hasMoreElements();)
            {
                Object obj = e.nextElement();
                if (obj instanceof XaitTest.TestApp)
                {
                    XaitTest.TestApp app = (XaitTest.TestApp) obj;
                    Integer key = new Integer(app.service.id);

                    // Skip if seen this service id before
                    if (services.get(key) != null) continue;
                    services.put(key, app.service);

                    ((XAITGenerator) xaitGen).add(app.service);
                }
            }

            // Remember certificate bytes
            for (int i = 0; i < priv.length; ++i)
            {
                ((XAITGenerator) xaitGen).add(priv[i]);
            }
            privCertBytes = new byte[priv.length * 20];
            for (int i = 0; i < priv.length; ++i)
            {
                System.arraycopy(priv[i], 0, privCertBytes, i * 20, 20);
            }
        }

        public SectionFactory(Class genClass, TransportProtocol[][] tp, AppEntry[] apps, String name, byte[][] priv)
        {
            super(genClass, tp, apps, name);

            // Drop services & priv bytes into first section
            init(gen[0], apps, priv);
        }

        public Vector getServices()
        {
            if (gen.length == 1)
                return ((XAITGenerator) gen[0]).svclist;
            else
            {
                Vector v = new Vector();
                for (int i = 0; i < gen.length; ++i)
                    v.addAll(((XAITGenerator) gen[i]).svclist);
                return v;
            }
        }

        public int getSource()
        {
            return Xait.NETWORK_SIGNALLING;
        }

        public byte[] getPrivilegedCertificates()
        {
            return privCertBytes;
        }

        protected AitParser createParser()
        {
            return new XaitParser(Xait.NETWORK_SIGNALLING);
        }

        private byte[] privCertBytes;
    }

    /**
     * Class used to generate an XAIT <code>InputStream</code> from a
     * description of applications and abstract services.
     */
    public static class XAITGenerator extends AITGenerator
    {
        public Vector svclist = new Vector();

        public Vector privCerts = new Vector();

        public XAITGenerator()
        {
            this("true".equals(MPEEnv.getEnv("OCAP.xait.I15")));
        }

        public XAITGenerator(boolean I15)
        {
            super(I15);
        }

        public void add(AbstractServiceEntry info)
        {
            svclist.addElement(info);
        }

        public void add(byte[] privCertHash)
        {
            assertEquals("PrivCert hash must be 20 bytes", 20, privCertHash.length);

            privCerts.add(privCertHash);
        }

        /**
         * Generates Abstract Service Descriptors.
         */
        protected void more_common_descriptors0(DataOutputStreamEx out, Vector fix) throws Exception
        {
            for (Enumeration e = svclist.elements(); e.hasMoreElements();)
            {
                AbstractServiceEntry svc = (AbstractServiceEntry) e.nextElement();
                abstract_service_descriptor(out, fix, svc);
            }
            privileged_certificate_descriptor(out, fix);
        }

        /**
         * Handles the abstract_service_descriptor.
         * 
         * <pre>
         * abstract_service_descriptor() {
         *   descriptor_tag 8 uimsbf 0xAE
         *   descriptor_length 8 uimsbf
         *   service_id 24 uimsbf
         *   reserved_for_future_use 7 uimsbf
         *   auto_select 1 bslbf
         *   for (i=0; i&lt;N; i++) {
         *     service_name_byte 8 uimsbf
         *   }
         * }
         * </pre>
         */
        protected void abstract_service_descriptor(DataOutputStreamEx out, Vector fix, AbstractServiceEntry svc)
                throws Exception
        {
            int length_pos;

            out.writeByte(I15 ? 0xAE : 0x66); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            int service_id = svc.id;
            out.writeByte((service_id >> 16) & 0xFF);
            out.writeByte((service_id >> 8) & 0xFF);
            out.writeByte((service_id) & 0xFF);

            out.writeByte(svc.autoSelect ? 1 : 0);

            string(out, svc.name, false);

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * Generate application descriptors.
         */
        protected void more_app_descriptors0(DataOutputStreamEx out, Vector fix, XAppEntry app) throws Exception
        {
            unbound_application_descriptor(out, fix, app);
            application_storage_descriptor(out, fix, app);
        }

        /**
         * <pre>
         * unbound_application_descriptor() {
         *   descriptor_tag 8 uimsbf 0xAF
         *   descriptor_length 8 uimsbf
         *   service_id 24 uimsbf
         *   version_number 32 uimsbf
         * }
         * </pre>
         */
        protected void unbound_application_descriptor(DataOutputStreamEx out, Vector fix, XAppEntry app)
                throws Exception
        {
            int length_pos;

            out.writeByte(I15 ? 0xAF : 0x67); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            int service_id = app.serviceId;
            out.writeByte((service_id >> 16) & 0xFF);
            out.writeByte((service_id >> 8) & 0xFF);
            out.writeByte((service_id) & 0xFF);

            out.writeByte((byte)((app.version >> 24) & 0xFF));
            out.writeByte((byte)((app.version >> 16) & 0xFF));
            out.writeByte((byte)((app.version >>  8) & 0xFF));
            out.writeByte((byte)((app.version      ) & 0xFF));
            
            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * <pre>
         * application_storage_descriptor() {
         *   descriptor_tag 8 uimsbf 0xB0
         *   descriptor_length 8 uimsbf
         *   storage_priority 16 uimsbf
         *   launch_order 8 uimsbf
         * }
         * </pre>
         */
        protected void application_storage_descriptor(DataOutputStreamEx out, Vector fix, XAppEntry app)
                throws Exception
        {
            // Ignore if not useful... i.e., both are zero
            if (app.storagePriority != 0 || app.launchOrder != 0)
            {
                int length_pos;

                out.writeByte(I15 ? 0xB0 : 0x69); // tag
                length_pos = out.getWritten();
                out.writeByte(0x00); // length

                out.writeShort(app.storagePriority);
                out.writeByte(app.launchOrder);

                fixLength(fix, length_pos, 1, out.getWritten());
            }
        }

        /**
         * <pre>
         * privileged_certificate_descriptor() {
         *   descriptor_tag 8 uimbsf 0xB1
         *   descriptor_length 8 uimbsf
         *   for(i=0; i&lt;N; i++) {
         *     for(j=0; j&lt;20; j++) {
         *       certificate_identifier_byte 8 uimbsf SHA-1 Hash
         *     }
         *   }
         * </pre>
         */
        protected void privileged_certificate_descriptor(DataOutputStreamEx out, Vector fix) throws Exception
        {
            int length_pos;

            out.writeByte(I15 ? 0xB1 : 0x68); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            // Write bytes (should be multiple of 20, in total)
            for (Enumeration e = privCerts.elements(); e.hasMoreElements();)
            {
                byte[] privCert = (byte[]) e.nextElement();

                for (int i = 0; i < privCert.length; ++i)
                    out.writeByte(privCert[i]);
            }

            fixLength(fix, length_pos, 1, out.getWritten());
        }
    }
}
