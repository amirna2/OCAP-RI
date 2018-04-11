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

package org.cablelabs.xlet.AbstractSvcMgmtTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.CRC32;

import javax.tv.service.SIManager;

import org.dvb.application.AppID;
import org.dvb.application.AppIcon;
import org.dvb.application.AppProxy;
import org.dvb.application.AppStateChangeEvent;
import org.dvb.application.AppsDatabase;
import org.dvb.application.AppsDatabaseFilter;
import org.dvb.application.DVBJProxy;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;
import org.ocap.system.MonitorAppPermission;

public class XAITGenerator
{
    private Vector applist = new Vector();

    private Vector svclist = new Vector();

    int table_id = 0x74; // application_information_section

    int application_type = 0x0001; // application_type

    public XAITGenerator()
    {
    }

    public void add(AppInfo info)
    {
        applist.addElement(info);
    }

    public void add(SvcInfo svc)
    {
        svclist.addElement(svc);
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
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStreamEx out = new DataOutputStreamEx(os);
        Vector cleanup = new Vector();

        // Generate table
        application_information_section(out, cleanup);

        // Fix up 16-bit length locations
        byte[] bytes = os.toByteArray();
        int i = 1;
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
            i++;
        }

        return new ByteArrayInputStream(bytes);
    }

    /*
     * <pre> application_information_section() { table_id 8 uimsbf
     * section_syntax_indicator 1 bslbf reserved_future_use 1 bslbf reserved 2
     * bslbf section_length 12 uimsbf application_type 16 uimsbf reserved 2
     * bslbf version_number 5 uimsbf current_next_indicator 1 bslbf
     * section_number 8 uimsbf last_section_number 8 uimsbf reserved_future_use
     * 4 bslbf common_descriptors_length 12 uimsbf for(i=0;i<N;i++){
     * descriptor() } reserved_future_use 4 bslbf application_loop_length 12
     * uimsbf for(i=0;i<N;i++){ application_identifier()
     * application_control_code 8 uimsbf reserved_future_use 4 bslbf
     * application_descriptors_loop_length 12 uimsbf for(j=0;j<N;j++){
     * descriptor() } } CRC_32 32 rpchof } </pre>
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
        out.writeByte(0x03); // version_number + current_next_indicator
        out.writeByte(0x00); // section_number
        out.writeByte(0x00); // last_section_number
        common_pos = out.getWritten();
        out.writeShort(0x0000); // common_descriptors_length

        // Common descriptors
        more_common_descriptors0(out, fix);
        // -- abstract services
        for (Enumeration svcs = svclist.elements(); svcs.hasMoreElements();)
        {
            SvcInfo svc = (SvcInfo) svcs.nextElement();
            abstract_service_descriptor(out, fix, svc);
        }
        more_common_descriptors1(out, fix);
        // -- external authorization
        // -- transport_protocol
        more_common_descriptors2(out, fix);

        fixLength(fix, common_pos, 2, out.getWritten());

        apps_pos = out.getWritten();
        out.writeShort(0x0000); // application_loop_length !!!!
        for (Enumeration apps = applist.elements(); apps.hasMoreElements();)
        {
            AppInfo app = (AppInfo) apps.nextElement();
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

    protected void application_loop(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
    {
        int app_pos;

        app_id(out, app.getAppID());
        out.writeByte(app.getControlCode());

        app_pos = out.getWritten();
        out.writeShort(0x0000); // application_descriptors_loop_length!!!!!

        more_app_descriptors0(out, fix, app);
        application_descriptor(out, fix, app);
        application_name_descriptor(out, fix, app);
        application_icons_descriptor(out, fix, app);
        more_app_descriptors1(out, fix, app);
        dvb_j_application_descriptor(out, fix, app);
        dvb_j_application_location_descriptor(out, fix, app);
        unbound_application_descriptor(out, fix, app);
        application_storage_descriptor(out, fix, app);
        transport_protocol_descriptor(out, fix, app);
        more_app_descriptors2(out, fix, app);

        fixLength(fix, app_pos, 2, out.getWritten());
    }

    protected void more_app_descriptors0(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
    {
    }

    protected void more_app_descriptors1(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
    {
    }

    protected void more_app_descriptors2(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
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
     *   for( i=0; i<N; i++ ) {
     *     application_profile 16 uimsbf
     *     version.major 8 uimsbf
     *     version.minor 8 uimsbf
     *     version.micro 8 uimsbf
     *   }
     *   service_bound_flag 1 bslbf
     *   visibility 2 bslbf
     *   reserved_future_use 5 bslbf
     *   application_priority 8 uimsbf
     *   for( i=0; i<N; i++ ) {
     *     transport_protocol_label 8 uimsbf
     *   }
     * }
     * </pre>
     */
    protected void application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
    {
        int length_pos;
        int prof_pos;

        out.writeByte(0x00); // tag
        length_pos = out.getWritten();
        out.writeByte(0x00); // length !!!

        prof_pos = out.getWritten();
        out.writeByte(0x00); // application_profiles length!!!

        // profiles
        // According to OCAP 13.2.1.10.1...
        // There is only one profile with version 1.0
        out.writeShort(1); // assume ocap.profile.basic_profile == 1
        out.writeByte(1); // assume 1.0==1.0.0
        out.writeByte(0);
        out.writeByte(0);

        fixLength(fix, prof_pos, 1, out.getWritten());

        out.writeByte(0x80 | (app.getVisibility() << 5)); // service_bound +
                                                          // visibility
        out.writeByte(app.getPriority());

        AppInfo.TPInfo[] tp = app.getTransportProtocols();
        for (int i = 0; i < tp.length; ++i)
            out.writeByte(tp[i].getLabel()); // transport_protocol_label

        fixLength(fix, length_pos, 1, out.getWritten());
    }

    /**
     * <pre>
     * application_name_descriptor() {
     *   descriptor_tag 8 uimsbf
     *   descriptor_length 8 uimsbf
     *   for (i=0; i<N; i++) {
     *     ISO_639_language_code 24 bslbf
     *     application_name_length 8 uimsbf
     *     for (i=0; i<N; i++) {
     *       application_name_char 8 uimsbf
     *     }
     *   }
     * }
     * </pre>
     */
    protected void application_name_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
    {
        int length_pos;

        out.writeByte(0x01); // tag
        length_pos = out.getWritten();
        out.writeByte(0x00); // length !!!

        // iso_lang
        string(out, "eng", false);
        string(out, app.getName());

        fixLength(fix, length_pos, 1, out.getWritten());
    }

    protected void string(DataOutputStreamEx out, String str, boolean length) throws Exception
    {
        byte[] bytes = str.getBytes();

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
     *   for (i=0; i<N; i++) {
     *     icon_locator_byte 8 uimsbf
     *   }
     *   icon_flags 16 bslbf
     *   for (i=0; i<N; i++) {
     *     reserved_future_use 8 bslbf
     *   }
     * }
     * </pre>
     */
    protected void application_icons_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
    {
        AppIcon icon = app.getAppIcon();
        if (icon == null) return;

        int length_pos;

        out.writeByte(0x0B); // tag
        length_pos = out.getWritten();
        out.writeByte(0x00); // length

        string(out, icon.getLocator().toExternalForm());

        BitSet set = icon.getIconFlags();
        int icon_flags = 0;
        for (int i = 0; i < 16; ++i)
            icon_flags |= 1 << i;
        out.writeShort(icon_flags);

        fixLength(fix, length_pos, 1, out.getWritten());
    }

    /**
     * <pre>
     * dvb_j_application_descriptor(){
     *   descriptor_tag 8 uimsbf
     *   descriptor_length 8 uimsbf
     *   for(i=0; i<N; i++) {
     *     parameter_length 8 uimsbf
     *     for(j=0; j<parameter_length; j++) {
     *       parameter_byte 8 uimsbf
     *     }
     *   }
     * }
     * </pre>
     */
    protected void dvb_j_application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
    {
        int length_pos;

        out.writeByte(0x03); // tag
        length_pos = out.getWritten();
        out.writeByte(0x00); // length

        String[] parms = app.getParameters();
        for (int i = 0; i < parms.length; ++i)
            string(out, parms[i]);

        fixLength(fix, length_pos, 1, out.getWritten());
    }

    /**
     * <pre>
     * dvb_j_application_location_descriptor {
     *   descriptor_tag 8 uimsbf
     *   descriptor_length 8 uimsbf
     *   base_directory_length 8 uimsbf
     *   for(i=0; i<N; i++) {
     *     base_directory_byte 8 uimsbf
     *   }
     *   classpath_extension_length 8 uimsbf
     *   for(i=0; i<N; i++) {
     *     classpath_extension_byte 8 uimsbf
     *   }
     *   for(i=0; i<N; i++) {
     *     initial_class_byte 8 uimsbf
     *   }
     * }
     * </pre>
     */
    protected void dvb_j_application_location_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app)
            throws Exception
    {
        int length_pos;

        out.writeByte(0x04); // tag
        length_pos = out.getWritten();
        out.writeByte(0x00); // length

        string(out, app.getBaseDir());
        string(out, app.getClasspath());
        string(out, app.getClassName(), false);

        fixLength(fix, length_pos, 1, out.getWritten());
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
     *   for (i=0; i<N; i++) {
     *     service_name_byte 8 uimsbf
     *   }
     * }
     * </pre>
     */
    protected void abstract_service_descriptor(DataOutputStreamEx out, Vector fix, SvcInfo svc) throws Exception
    {
        int length_pos;

        out.writeByte(0xAE); // tag
        length_pos = out.getWritten();
        out.writeByte(0x00); // length

        int service_id = svc.getId();
        out.writeByte((service_id >> 16) & 0xFF);
        out.writeByte((service_id >> 8) & 0xFF);
        out.writeByte((service_id) & 0xFF);

        out.writeByte(svc.isAutoSelect() ? 1 : 0);

        string(out, svc.getName(), false);

        fixLength(fix, length_pos, 1, out.getWritten());
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
    protected void unbound_application_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
    {
        int length_pos;

        out.writeByte(0xAF); // tag
        length_pos = out.getWritten();
        out.writeByte(0x00); // length

        int service_id = app.getServiceID();
        out.writeByte((service_id >> 16) & 0xFF);
        out.writeByte((service_id >> 8) & 0xFF);
        out.writeByte((service_id) & 0xFF);

        out.writeInt(app.getVersion());

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
    protected void application_storage_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
    {
        int length_pos;

        out.writeByte(0xB0); // tag
        length_pos = out.getWritten();
        out.writeByte(0x00); // length

        out.writeShort(app.getStoragePriority());
        out.writeByte(app.getLaunchOrder());

        fixLength(fix, length_pos, 1, out.getWritten());
    }

    /**
     * <pre>
     * transport_protocol_descriptor() {
     *   descriptor_tag 8 uimsbf
     *   descriptor_length 8 uimsbf
     *   protocol_id 16 uimsbf
     *   transport_protocol_label 8 uimsbf
     *   for(i=0; i<N; i++) {
     *     selector_byte 8 uimsbf N1
     *   }
     * }
     * </pre>
     */
    protected void transport_protocol_descriptor(DataOutputStreamEx out, Vector fix, AppInfo app) throws Exception
    {
        AppInfo.TPInfo[] tp = app.getTransportProtocols();
        for (int i = 0; i < tp.length; ++i)
        {
            int length_pos;

            out.writeByte(0x02); // tag
            length_pos = out.getWritten();
            out.writeByte(0x00); // length

            out.writeShort(tp[i].getId());
            out.writeByte(tp[i].getLabel());

            out.writeByte(tp[i].isRemote() ? 0x80 : 0x00);
            if (tp[i].isRemote())
            {
                out.writeShort(tp[i].getNetId());
                out.writeShort(tp[i].getTsId());
                out.writeShort(tp[i].getSId());
            }
            if (tp[i] instanceof AppInfo.OCInfo)
            {
                out.writeByte(((AppInfo.OCInfo) tp[i]).getComponent());
            }
            else if (tp[i] instanceof AppInfo.IPInfo)
            {
                String[] urls = ((AppInfo.IPInfo) tp[i]).getUrls();
                for (int ii = 0; ii < urls.length; ++ii)
                {
                    string(out, urls[ii]);
                }
            }

            fixLength(fix, length_pos, 1, out.getWritten());
        }
    }

    public static interface SvcInfo
    {
        public int getId();

        public boolean isAutoSelect();

        public String getName();
    }

    public static interface AppInfo
    {
        public static final int VIS_NONVIS = 0;

        public static final int VIS_LISTING = 1;

        public static final int VIS_FULL = 3;

        public AppID getAppID();

        public String getName();

        public int getControlCode();

        public int getVisibility();

        public int getPriority();

        public int getLaunchOrder();

        public int[] getPlatformVersion();

        public int getVersion();

        public int getServiceID();

        public String getBaseDir();

        public String getClasspath();

        public String getClassName();

        public String[] getParameters();

        public AppIcon getAppIcon();

        public int getStoragePriority();

        public TPInfo[] getTransportProtocols();

        public static interface TPInfo
        {
            public int getId();

            public int getLabel();

            public boolean isRemote();

            public int getNetId();

            public int getTsId();

            public int getSId();
        }

        public static interface OCInfo extends TPInfo
        {
            public int getComponent();
        }

        public static interface IPInfo extends TPInfo
        {
            public boolean isAligned();

            public String[] getUrls();
        }
    }
}
