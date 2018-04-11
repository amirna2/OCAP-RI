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
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.signalling.AitTest.AitFactory;
import org.cablelabs.impl.signalling.AppEntry.TransportProtocol;
import org.cablelabs.impl.util.MPEEnv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.CRC32;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.groboutils.junit.v1.iftc.InterfaceTestSuite;

import org.davic.mpeg.sections.NoDataAvailableException;
import org.davic.mpeg.sections.Section;
import org.dvb.application.AppID;

/**
 * Tests <code>AitParser</code>.
 * <p>
 * Missing tests (i.e., todo):
 * <ul>
 * <li>multiple sections tests
 * <li>ignored (invalid) descriptors
 * <li>invalid sections
 * </ul>
 */
public class AitParserTest extends TestCase
{
    /**
     * Tests constructor. After construction, should look like an empty
     * <code>Ait</code>.
     */
    public void testConstructor()
    {
        AitParser parser = new AitParser();
        Ait ait = parser.getSignalling();
        ait.filterApps(new Properties(), new Properties());
        AppEntry[] apps = ait.getApps();
        
        assertNotNull("getApps() should return valid array", apps);
        assertEquals("getApps() should return empty array", 0, apps.length);
    }

    /* ==================== Boilerplate ================== */
    public AitParserTest(String name)
    {
        super(name);
    }

    public static Test suite(String[] tests)
    {
        if (tests == null || tests.length == 0) return suite();

        TestSuite suite = new TestSuite(AitParserTest.class.getName());
        for (int i = 0; i < tests.length; ++i)
            suite.addTest(new AitParserTest(tests[i]));
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
        TestSuite suite = new TestSuite(AitParserTest.class);
        InterfaceTestSuite aitSuite = AitTest.isuite();

        // Generate Factories...

        AppEntry[] apps = AitTest.TEST_APPS;
        for (int size = 0; size <= apps.length; ++size)
        {
            AppEntry[] slice = new AppEntry[size];
            System.arraycopy(apps, 0, slice, 0, size);

            aitSuite.addFactory(new InputStreamFactory(new AITGenerator(), slice, "InputStream:" + size));
            aitSuite.addFactory(new SectionFactory(new AITGenerator(), slice, "Section[1]:" + size));

            // Test TPs in common area
            AITGenerator gen = new AITGenerator();
            gen.addCommon(AitTest.TP0);
            gen.addCommon(AitTest.TP1);
            aitSuite.addFactory(new InputStreamFactory(gen, slice, "InputStream(common):" + size));
            gen = new AITGenerator();
            gen.addCommon(AitTest.TP0);
            gen.addCommon(AitTest.TP1);
            aitSuite.addFactory(new SectionFactory(gen, slice, "Section[1]:" + size));

            // Test multi-section w/ common descriptors (See bug4801)
            aitSuite.addFactory(new InputStreamFactory(AITGenerator.class, new TransportProtocol[][] { AitTest.TP0,
                    AitTest.TP1 }, slice, "InputStream[multi]:" + size));
            aitSuite.addFactory(new SectionFactory(AITGenerator.class, new TransportProtocol[][] { AitTest.TP0,
                    AitTest.TP1 }, slice, "Section[multi]:" + size));
        }

        suite.addTest(aitSuite);

        return suite;
    }

    public static class InputStreamFactory extends AitFactory
    {
        protected AITGenerator[] gen;

        protected String name;

        public InputStreamFactory(AITGenerator gen, AppEntry[] apps, String name)
        {
            this.gen = new AITGenerator[] { gen };
            this.name = name;
            add(apps);
        }

        public InputStreamFactory(Class genClass, TransportProtocol[][] tp, AppEntry[] apps, String name)
        {
            // common TP in first sections
            // apps in own section
            this.gen = new AITGenerator[tp.length + apps.length];
            this.name = name;
            int j = 0;
            try
            {
                for (int i = 0; i < tp.length; ++i, ++j)
                {
                    gen[j] = (AITGenerator) genClass.newInstance();
                    gen[j].sectionCount = gen.length;
                    gen[j].sectionNum = j;
                    gen[j].addCommon(tp[i]);
                    gen[j].allTpAreCommon = true;
                }
                for (int i = 0; i < apps.length; ++i, ++j)
                {
                    gen[j] = (AITGenerator) genClass.newInstance();
                    gen[j].sectionCount = gen.length;
                    gen[j].sectionNum = j;
                    gen[j].add(apps[i]);
                    gen[j].allTpAreCommon = true;

                    // Do what add(apps) does
                    addAppEntry(apps[i]);
                }
            }
            catch (Exception e)
            {
                fail(e.toString());
            }
        }

        public void add(AppEntry info)
        {
            gen[0].add(info);
        }

        protected void generate(AitParser parser) throws Exception
        {
            InputStream is = null;
            if (gen.length == 1)
                is = gen[0].generate();
            else
            {
                Vector v = new Vector();
                for (int i = 0; i < gen.length; ++i)
                    v.add(gen[i].generate());
                is = new SequenceInputStream(v.elements());
            }
            //parser.parse(is);
        }

        protected AitParser createParser()
        {
            return new AitParser();
        }

        public Object createImplObject()
        {
            try
            {
                AitParser parser = createParser();
                generate(parser);
                parser.getSignalling().filterApps(new Properties(), new Properties());
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

    public static class SectionFactory extends InputStreamFactory
    {
        public SectionFactory(AITGenerator gen, AppEntry[] apps, String name)
        {
            super(gen, apps, name);
        }

        public SectionFactory(Class genClass, TransportProtocol[][] tp, AppEntry[] apps, String name)
        {
            super(genClass, tp, apps, name);
        }

        protected void generate(AitParser parser) throws Exception
        {
            Section[] sections = new Section[gen.length];
            for (int i = 0; i < gen.length; ++i)
                sections[i] = new BasicSection(gen[i].genBytes());

            parser.parse(sections);
        }

    }

    /**
     * Class used to generate an AIT <code>InputStream</code> from a description
     * of applications.
     */
    public static class AITGenerator
    {
        public Vector applist = new Vector();

        int table_id = 0x74; // application_information_section

        int application_type = 0x0001; // application_type

        int version = 0;

        int sectionNum = 0;

        int sectionCount = 1;

        public Vector commonTpList = new Vector();

        boolean allTpAreCommon = false;

        /**
         * This doesn't seem like it should belong here. However, OCAP defines
         * the registered_api_descriptor as being valid in both AIT and XAIT.
         * So, we need to consider which version tags we are supporting here.
         */
        protected final boolean I15;

        public AITGenerator(boolean I15)
        {
            this.I15 = I15;
        }

        public AITGenerator()
        {
            this("true".equals(MPEEnv.getEnv("OCAP.xait.I15")));
        }

        public void add(AppEntry info)
        {
            applist.addElement(info);
        }

        public void addCommon(TransportProtocol[] tp)
        {
            for (int i = 0; i < tp.length; ++i)
                addCommon(tp[i]);
        }

        public void addCommon(TransportProtocol tp)
        {
            if (!commonTpList.contains(tp)) commonTpList.add(tp);
        }

        public void setVersion(int version)
        {
            this.version = version & 0x1F;
        }

        public void setSectionNo(int number, int total)
        {
            this.sectionNum = number;
            this.sectionCount = total;
        }

        protected static class DataOutputStreamEx extends DataOutputStream
        {
            public DataOutputStreamEx(OutputStream os)
            {
                super(os);
            }

            public int getWritten()
            {
                return written;
            }
        }

        /**
         * Generates an InputStream for the application information that's been
         * added to this XAITGenerator.
         * 
         * @returns an <code>InputStream</code> that can be passed to
         *          <code>AppManagerProxy.registerUnboundApp()</code>.
         */
        public InputStream generate() throws Exception
        {
            return new ByteArrayInputStream(genBytes());
        }

        public byte[] genBytes() throws Exception
        {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            DataOutputStreamEx out = new DataOutputStreamEx(os);
            Vector cleanup = new Vector();

            // Generate table
            application_information_section(out, cleanup);

            // Fix up 16-bit length locations
            byte[] bytes = os.toByteArray();
            for (Enumeration e = cleanup.elements(); e.hasMoreElements();)
            {
                int[] fix = (int[]) e.nextElement();

                // fix[0] = offset
                // fix[1] = size
                // fix[2] = ubyte/ushort/uint to copy
                switch (fix[1])
                {
                    case 1:
                        bytes[fix[0]] |= (fix[2]) & 0xFF;
                        break;
                    case 2:
                        bytes[fix[0]] |= (fix[2] >> 8) & 0xFF;
                        bytes[fix[0] + 1] |= (fix[2]) & 0xFF;
                        break;
                    case 4:
                        bytes[fix[0]] |= (fix[2] >> 24) & 0xFF;
                        bytes[fix[0] + 1] |= (fix[2] >> 16) & 0xFF;
                        bytes[fix[0] + 2] |= (fix[2] >> 8) & 0xFF;
                        bytes[fix[0] + 3] |= (fix[2]) & 0xFF;
                        break;
                }
            }

            // Update CRC-32
            if (bytes.length > 4)
            {
                CRC32 crc32 = new CRC32();
                crc32.update(bytes, 0, bytes.length - 4);

                long value = crc32.getValue();

                bytes[bytes.length - 1] = (byte) (value & 0xFF);
                bytes[bytes.length - 2] = (byte) ((value >> 8) & 0xFF);
                bytes[bytes.length - 3] = (byte) ((value >> 16) & 0xFF);
                bytes[bytes.length - 4] = (byte) ((value >> 24) & 0xFF);
            }

            return bytes;
        }

        /*
         * <pre> application_information_section() { table_id 8 uimsbf
         * section_syntax_indicator 1 bslbf reserved_future_use 1 bslbf reserved
         * 2 bslbf section_length 12 uimsbf application_type 16 uimsbf reserved
         * 2 bslbf version_number 5 uimsbf current_next_indicator 1 bslbf
         * section_number 8 uimsbf last_section_number 8 uimsbf
         * reserved_future_use 4 bslbf common_descriptors_length 12 uimsbf
         * for(i=0;i&lt;N;i++){ descriptor() } reserved_future_use 4 bslbf
         * application_loop_length 12 uimsbf for(i=0;i&lt;N;i++){
         * application_identifier() application_control_code 8 uimsbf
         * reserved_future_use 4 bslbf application_descriptors_loop_length 12
         * uimsbf for(j=0;j&lt;N;j++){ descriptor() } } CRC_32 32 rpchof }
         * </pre>
         */
        protected void application_information_section(DataOutputStreamEx out, Vector fix) throws Exception
        {
            int length_pos;
            int common_pos;
            int apps_pos;

            int tmp;

            out.writeByte((byte) table_id); // table_id
            length_pos = out.getWritten();
            out.writeShort(0x8000); // section_syntax_indicator + length
            out.writeShort(application_type); // OCAP_J
            tmp = (version << 1) | 1;
            out.writeByte(tmp); // version_number + current_next_indicator
            out.writeByte(sectionNum); // section_number
            out.writeByte(sectionCount - 1); // last_section_number
            common_pos = out.getWritten();
            out.writeShort(0x0000); // common_descriptors_length

            // Common descriptors
            more_common_descriptors0(out, fix);
            more_common_descriptors1(out, fix);
            common_transport_protocols(out, fix);
            // -- external authorization
            // -- application_name
            more_common_descriptors2(out, fix);

            fixLength(fix, common_pos, 2, out.getWritten());

            apps_pos = out.getWritten();
            out.writeShort(0x0000); // application_loop_length !!!!
            for (Enumeration apps = applist.elements(); apps.hasMoreElements();)
            {
                AppEntry app = (AppEntry) apps.nextElement();
                application_loop(out, fix, app);
            }
            fixLength(fix, apps_pos, 2, out.getWritten());

            out.writeInt(0x00000000); // CRC32

            fixLength(fix, length_pos, 2, out.getWritten());
        }

        protected void more_common_descriptors0(DataOutputStreamEx out, Vector fix) throws Exception
        {
        }

        protected void more_common_descriptors1(DataOutputStreamEx out, Vector fix) throws Exception
        {
        }

        protected void more_common_descriptors2(DataOutputStreamEx out, Vector fix) throws Exception
        {
        }

        protected void common_transport_protocols(DataOutputStreamEx out, Vector fix) throws Exception
        {
            for (int i = 0; i < commonTpList.size(); ++i)
                transport_protocol_descriptor(out, fix, (TransportProtocol) commonTpList.elementAt(i));
        }

        protected void application_loop(DataOutputStreamEx out, Vector fix, AppEntry app) throws Exception
        {
            int app_pos;

            app_id(out, app.id);
            out.writeByte(app.controlCode);

            app_pos = out.getWritten();
            out.writeShort(0x0000); // application_descriptors_loop_length!!!!!

            more_app_descriptors0(out, fix, app);
            application_descriptor(out, fix, app);
            application_name_descriptor(out, fix, app);
            application_icons_descriptor(out, fix, app);
            more_app_descriptors1(out, fix, app);
            dvb_j_application_descriptor(out, fix, app);
            dvb_j_application_location_descriptor(out, fix, app);
            // unbound_application_descriptor(out, fix, app);
            // application_storage_descriptor(out, fix, app);
            transport_protocol_descriptor(out, fix, app);
            prefetch_descriptor(out, fix, app);
            dii_descriptor(out, fix, app);
            more_app_descriptors2(out, fix, app);

            // OCAP: registered api
            if (app.registeredApi != null)
            {
                for (int i = 0; i < app.registeredApi.length; ++i)
                {
                    registered_api_descriptor(out, fix, app.registeredApi[i]);
                }
            }

            fixLength(fix, app_pos, 2, out.getWritten());
        }

        protected void more_app_descriptors0(DataOutputStreamEx out, Vector fix, AppEntry app) throws Exception
        {
        }

        protected void more_app_descriptors1(DataOutputStreamEx out, Vector fix, AppEntry app) throws Exception
        {
        }

        protected void more_app_descriptors2(DataOutputStreamEx out, Vector fix, AppEntry app) throws Exception
        {
        }

        protected void fixLength(Vector fix, int pos, int size, int written)
        {
            fix.addElement(new int[] { pos, size, written - pos - size });
        }

        protected void app_id(DataOutputStreamEx out, AppID id) throws Exception
        {
            out.writeInt(id.getOID());
            out.writeShort(id.getAID());
        }

        /**
         * <pre>
         * application_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   application_profiles_length 8 uimsbf
         *   for( i=0; i&lt;N; i++ ) {
         *     application_profile 16 uimsbf
         *     version.major 8 uimsbf
         *     version.minor 8 uimsbf
         *     version.micro 8 uimsbf
         *   }
         *   service_bound_flag 1 bslbf
         *   visibility 2 bslbf
         *   reserved_future_use 5 bslbf
         *   application_priority 8 uimsbf
         *   for( i=0; i&lt;N; i++ ) {
         *     transport_protocol_label 8 uimsbf
         *   }
         * }
         * </pre>
         */
        protected void application_descriptor(DataOutputStreamEx out, Vector fix, AppEntry app) throws Exception
        {
            int length_pos;
            int prof_pos;

            out.writeByte(0x00); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length !!!

            prof_pos = out.getWritten();
            out.writeByte(0x00); // application_profiles length!!!

            // profiles
            if (false) // according to 18.2.1.1, OCAP doesn't support profiles
            {
                // According to OCAP 13.3.12
                // There is only one profile with version 1.0
                out.writeShort(1); // assume ocap.profile.basic_profile == 1
                out.writeByte(1); // assume 1.0==1.0.0
                out.writeByte(0);
                out.writeByte(0);
            }
            else if (app.versions != null)
            {
                for (Enumeration e = app.versions.keys(); e.hasMoreElements();)
                {
                    Integer profile = (Integer) e.nextElement();
                    int[] version = (int[]) app.versions.get(profile);

                    out.writeShort(profile.intValue());
                    out.writeByte(version[0]);
                    out.writeByte(version[1]);
                    out.writeByte(version[2]);
                }
            }

            fixLength(fix, prof_pos, 1, out.getWritten());

            out.writeByte(0x80 | (app.visibility << 5)); // service_bound +
                                                         // visibility
            out.writeByte(app.priority);

            AppEntry.TransportProtocol[] tp = app.transportProtocols;
            if (tp != null) for (int i = 0; i < tp.length; ++i)
                out.writeByte(tp[i].label); // transport_protocol_label

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * <pre>
         * application_name_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   for (i=0; i&lt;N; i++) {
         *     ISO_639_language_code 24 bslbf
         *     application_name_length 8 uimsbf
         *     for (i=0; i&lt;N; i++) {
         *       application_name_char 8 uimsbf
         *     }
         *   }
         * }
         * </pre>
         */
        protected void application_name_descriptor(DataOutputStreamEx out, Vector fix, AppEntry app) throws Exception
        {
            int length_pos;

            out.writeByte(0x01); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length !!!

            // iso_lang
            for (Enumeration keys = app.names.keys(); keys.hasMoreElements();)
            {
                String key = (String) keys.nextElement();
                if (key.length() > 3) key = key.substring(0, 3);
                string(out, key, false);
                string(out, (String) app.names.get(key));
            }

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        protected void string(DataOutputStreamEx out, String str, boolean length) throws Exception
        {
            byte[] bytes = (str == null) ? (new byte[0]) : str.getBytes();

            if (length) out.writeByte(bytes.length);
            for (int i = 0; i < bytes.length; ++i)
                out.writeByte(bytes[i]);
        }

        protected void string(DataOutputStreamEx out, String str) throws Exception
        {
            string(out, str, true);
        }

        /**
         * <pre>
         * application_icons_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   icon_locator_length 8 uimsbf
         *   for (i=0; i&lt;N; i++) {
         *     icon_locator_byte 8 uimsbf
         *   }
         *   icon_flags 16 bslbf
         *   for (i=0; i&lt;N; i++) {
         *     reserved_future_use 8 bslbf
         *   }
         * }
         * </pre>
         */
        protected void application_icons_descriptor(DataOutputStreamEx out, Vector fix, AppEntry app) throws Exception
        {
            if (app.iconLocator != null)
            {
                int length_pos;

                out.writeByte(0x0B); // tag
                length_pos = out.getWritten();
                out.writeByte(0x00); // length

                string(out, app.iconLocator);
                out.writeShort(app.iconFlags);

                fixLength(fix, length_pos, 1, out.getWritten());
            }
        }

        /**
         * <pre>
         * dvb_j_application_descriptor(){
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   for(i=0; i&lt;N; i++) {
         *     parameter_length 8 uimsbf
         *     for(j=0; j<parameter_length; j++) {
         *       parameter_byte 8 uimsbf
         *     }
         *   }
         * }
         * </pre>
         */
        protected void dvb_j_application_descriptor(DataOutputStreamEx out, Vector fix, AppEntry app) throws Exception
        {
            int length_pos;

            out.writeByte(0x03); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            String[] parms = app.parameters;
            if (parms != null) for (int i = 0; i < parms.length; ++i)
                string(out, parms[i]);

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * <pre>
         * dvb_j_application_location_descriptor {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   base_directory_length 8 uimsbf
         *   for(i=0; i&lt;N; i++) {
         *     base_directory_byte 8 uimsbf
         *   }
         *   classpath_extension_length 8 uimsbf
         *   for(i=0; i&lt;N; i++) {
         *     classpath_extension_byte 8 uimsbf
         *   }
         *   for(i=0; i&lt;N; i++) {
         *     initial_class_byte 8 uimsbf
         *   }
         * }
         * </pre>
         */
        protected void dvb_j_application_location_descriptor(DataOutputStreamEx out, Vector fix, AppEntry app)
                throws Exception
        {
            int length_pos;

            out.writeByte(0x04); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            string(out, app.baseDirectory);
            StringBuffer str = new StringBuffer();
            if (app.classPathExtension != null)
            {
                String sep = "";
                for (int i = 0; i < app.classPathExtension.length; ++i)
                {
                    str.append(sep);
                    str.append(app.classPathExtension[i]);
                    sep = ";";
                }
            }
            string(out, str.toString());
            string(out, app.className, false);

            fixLength(fix, length_pos, 1, out.getWritten());
        }

        /**
         * <pre>
         * transport_protocol_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   protocol_id 16 uimsbf
         *   transport_protocol_label 8 uimsbf
         *   for(i=0; i&lt;N; i++) {
         *     selector_byte 8 uimsbf N1
         *   }
         * }
         * </pre>
         */
        protected void transport_protocol_descriptor(DataOutputStreamEx out, Vector fix, AppEntry app) throws Exception
        {
            AppEntry.TransportProtocol[] tp = app.transportProtocols;
            if (tp != null && !allTpAreCommon)
            {
                for (int i = 0; i < tp.length; ++i)
                {
                    if (!commonTpList.contains(tp[i])) transport_protocol_descriptor(out, fix, tp[i]);
                }
            }
        }

        /**
         * <pre>
         * transport_protocol_descriptor() {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   protocol_id 16 uimsbf
         *   transport_protocol_label 8 uimsbf
         *   for(i=0; i&lt;N; i++) {
         *     selector_byte 8 uimsbf N1
         *   }
         * }
         * </pre>
         */
        protected void transport_protocol_descriptor(DataOutputStreamEx out, Vector fix, TransportProtocol tp)
                throws Exception
        {
            if (tp != null)
            {
                int length_pos;

                out.writeByte(0x02); // tag
                length_pos = out.getWritten();
                out.writeByte(0x00); // length

                out.writeShort(tp.protocol);
                out.writeByte(tp.label);

                if (!(tp instanceof AppEntry.IcTransportProtocol))
                {
                    out.writeByte(tp.remoteConnection ? 0x80 : 0x00);
                    if (tp.remoteConnection)
                    {
                        out.writeShort(0);
                        out.writeShort(0);
                        out.writeShort(tp.serviceId);
                    }
                }
                if (tp instanceof AppEntry.OcTransportProtocol)
                {
                    out.writeByte(((AppEntry.OcTransportProtocol) tp).componentTag);
                }
                else if (tp instanceof AppEntry.IcTransportProtocol)
                {
                    //string(out, ((AppEntry.IcTransportProtocol) tp).url);
                }
                else if (tp instanceof AppEntry.LocalTransportProtocol)
                {
                    // remote_connection is output though...
                    // nothing!!!!
                }

                fixLength(fix, length_pos, 1, out.getWritten());
            }
        }

        /**
         * <pre>
         * prefetch_descriptor () {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   transport_protocol_label 8 uimsbf
         *   for(i=0; i&lt;N; i++ ) {
         *     label_length
         *     for(j=0; j<label_length; j++ ) {
         *       label_char 8 uimsbf
         *     }
         *     prefetch_priority 8 uimsbf
         *   }
         * }
         * </pre>
         */
        protected void prefetch_descriptor(DataOutputStreamEx out, Vector fix, AppEntry app) throws Exception
        {
            if (app.prefetch != null)
            {
                int length_pos;

                out.writeByte(0x0c); // tag
                length_pos = out.getWritten();
                out.writeByte(0x00); // length

                out.writeByte(app.prefetch.transportLabel); // transport_label
                if (app.prefetch.info != null) for (int i = 0; i < app.prefetch.info.length; ++i)
                {
                    string(out, app.prefetch.info[i].label); // length+label
                    out.writeByte(app.prefetch.info[i].priority); // priority
                }

                fixLength(fix, length_pos, 1, out.getWritten());
            }
        }

        /**
         * <pre>
         * DII_location_descriptor () {
         *   descriptor_tag 8 uimsbf
         *   descriptor_length 8 uimsbf
         *   transport_protocol_label 8 uimsbf
         *   for(i=0; i&lt;N; i++ ) {
         *     reserved_future_use 1 bslbf
         *     DII_identification 15 uimsbf
         *     association_tag 16 uimsbf
         *   }
         * }
         * </pre>
         */
        protected void dii_descriptor(DataOutputStreamEx out, Vector fix, AppEntry app) throws Exception
        {
            if (app.diiLocation != null)
            {
                int length_pos;

                out.writeByte(0x0d); // tag
                length_pos = out.getWritten();
                out.writeByte(0x00); // length

                out.writeByte(app.prefetch.transportLabel); // transport_label
                if (app.diiLocation.diiIdentification != null)
                {
                    for (int i = 0; i < app.diiLocation.diiIdentification.length; ++i)
                    {
                        out.writeShort(app.diiLocation.diiIdentification[i]); // dii_ident
                        out.writeShort(app.diiLocation.associationTag[i]); // assoc_tag
                    }
                }

                fixLength(fix, length_pos, 1, out.getWritten());
            }
        }

        /**
         * <pre>
         * ocap_j_registered_api_descriptor() {
         *   descriptor_tag 8 uimsbf 0xB2
         *   descriptor_length 8 uimsbf
         *   for( i=0; i&lt;N; i++ } {
         *     registered_api_name_char 8 uimsbf
         *   }
         * }
         * </pre>
         */
        private void registered_api_descriptor(DataOutputStreamEx out, Vector fix, String api) throws Exception
        {
            int length_pos;

            out.writeByte(I15 ? 0xB2 : 0x6A); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            string(out, api, false);

            fixLength(fix, length_pos, 1, out.getWritten());
        }
    }
}

/**
 * Basic implementation of a <code>Section</code> object. This implementation is
 * the basis for other implementations and is implemented around a simple
 * <code>byte[]</code>. For subclasses (e.g., <code>NativeSection</code>) this
 * simple <code>byte[]</code> functions as a cache of the section data. For
 * <i>cloned</i> <code>BasicSection</code> (and <code>NativeSection</code>) this
 * is the only copy if the data.
 * 
 * @author Aaron Kamienski
 */
class BasicSection extends Section
{
    /**
     * Creates a <code>BasicSection</code> composed of the given data.
     * 
     * @param data
     *            <code>byte</code> array to be cached; may be <code>null</code>
     */
    BasicSection(byte[] data)
    {
        this.cache = data;
    }

    /**
     * This method returns all data from the filtered section in the Section
     * object, including the section header. Each call to this method results in
     * a new a copy of the section data.
     * 
     * @exception NoDataAvailableException
     *                if no valid data is available.
     */
    public byte[] getData() throws NoDataAvailableException
    {
        try
        {
            byte[] cache = this.cache;
            byte[] copy = new byte[cache.length];

            System.arraycopy(cache, 0, copy, 0, cache.length);

            return copy;
        }
        catch (NullPointerException e)
        {
            throw new NoDataAvailableException();
        }
    }

    /**
     * This method returns the specified part of the filtered data. Each call to
     * this method results in a new a copy of the section data.
     * 
     * @param index
     *            defines within the filtered section the index of the first
     *            byte of the data to be retrieved. The first byte of the
     *            section (the table_id field) has index 1.
     * @param length
     *            defines the number of consecutive bytes from the filtered
     *            section to be retrieved.
     * @exception NoDataAvailableException
     *                if no valid data is available.
     * @exception java.lang.IndexOutOfBoundsException
     *                if any part of the filtered data requested would be
     *                outside the range of data in the section.
     */
    public byte[] getData(int index, int length) throws NoDataAvailableException, java.lang.IndexOutOfBoundsException
    {
        if (length < 0) throw new IndexOutOfBoundsException();

        try
        {
            byte[] cache = this.cache;
            byte[] copy = new byte[length];

            System.arraycopy(cache, index - 1, copy, 0, length);

            return copy;
        }
        catch (NullPointerException e)
        {
            throw new NoDataAvailableException();
        }
    }

    /**
     * This method returns one byte from the filtered data.
     * 
     * @param index
     *            defines within the filtered section the index of the byte to
     *            be retrieved. The first byte of the section (the table_id
     *            field) has index 1.
     * @exception NoDataAvailableException
     *                if no valid data is available.
     * @exception java.lang.IndexOutOfBoundsException
     *                if the byte requested would be outside the range of data
     *                in the section.
     */
    public byte getByteAt(int index) throws NoDataAvailableException, java.lang.IndexOutOfBoundsException
    {
        try
        {
            // table_id field has index 1, but is first byte
            return cache[index - 1];
        }
        catch (NullPointerException e)
        {
            throw new NoDataAvailableException();
        }
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     * 
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public int table_id() throws NoDataAvailableException
    {
        return getByteAt(1);
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     * 
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public boolean section_syntax_indicator() throws NoDataAvailableException
    {
        return (getByteAt(2) & 0x80) != 0;
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     * 
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public boolean private_indicator() throws NoDataAvailableException
    {
        return (getByteAt(2) & 0x40) != 0;
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     * 
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public int section_length() throws NoDataAvailableException
    {
        return getShortAt(2) & 0xFFF;
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     * 
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public int table_id_extension() throws NoDataAvailableException
    {
        return getUShortAt(4);
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     * 
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public short version_number() throws NoDataAvailableException
    {
        return (short) ((getByteAt(6) >> 1) & 0x1F);
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     * 
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public boolean current_next_indicator() throws NoDataAvailableException
    {
        return (getByteAt(6) & 1) != 0;
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     * 
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public int section_number() throws NoDataAvailableException
    {
        return (int) getUByteAt(7);
    }

    /**
     * This method returns the value of the corresponding field from an MPEG-2
     * section header.
     * 
     * @exception NoDataAvailableException
     *                thrown if no valid data is available
     */
    public int last_section_number() throws NoDataAvailableException
    {
        return (int) getUByteAt(8);
    }

    /**
     * This method reads whether a Section object contains valid data.
     * 
     * @return true when the Section object contains valid data otherwise false
     */
    public boolean getFullStatus()
    {
        try
        {
            byte[] cache = this.cache;
            return cache != null && cache.length == section_length();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * This method sets a Section object such that any data contained within it
     * is no longer valid. This is intended to be used with RingSectionFilters
     * to indicate that the particular object can be re-used.
     */
    public void setEmpty()
    {
        // Obviously, would have more meaning for RingSectionFilters
        cache = null;
    }

    /**
     * Create a copy of this Section object. A cloned Section object is a new
     * and separate object. It is unaffected by changes in the state of the
     * original Section object or restarting of the SectionFilter the source
     * Section object originated from.
     * 
     */
    public Object clone()
    {
        return new BasicSection(cache);
    }

    /**
     * Ensure that native handle is released if nobody is using it.
     */
    public void finalize()
    {
        setEmpty();
    }

    /**
     * Accesses a short at the given index.
     * 
     * @param index
     * @return a short at the given index
     */
    protected short getShortAt(int index) throws NoDataAvailableException
    {
        return (short) ((getByteAt(index) << 8) | getUByteAt(index + 1));
    }

    /**
     * Accesses an unsigned byte at the given index.
     * 
     * @param index
     * @return an unsigned at the given index
     */
    protected int getUByteAt(int index) throws NoDataAvailableException
    {
        return (int) getByteAt(index) & 0xFF;
    }

    /**
     * Accesses an unsigned short at the given index.
     * 
     * @param index
     * @return an unsigned short at the given index
     */
    protected int getUShortAt(int index) throws NoDataAvailableException
    {
        return (int) getShortAt(index) & 0xFFFF;
    }

    /**
     * Cache of byte array that make up this section.
     */
    protected volatile byte[] cache;
}
