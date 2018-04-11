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

import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;

/**
 * An <code>AppEntry</code> contains all of the information signalled in the AIT
 * about an application.
 * <p>
 * Instances of <code>AppEntry</code> are created as the result of parsing
 * application signalling contained in either in-band AIT.
 * <p>
 * While all attributes are publicly accessible and non-<code>final</code>, they
 * should be considered <code>final</code> and not modified.
 */
public class AppEntry implements Cloneable
{
    /**
     * Corresponds to the <i>application_control_code</i> of the inner
     * "application" loop and is the basis for
     * {@link org.ocap.application.OcapAppAttributes#getApplicationControlCode}.
     * 
     * @see "MHP-1.0.2: 10.4.6 Syntax of the AIT"
     */
    public int controlCode;

    /**
     * Corresponds to the <i>application_identifier()</i> of the inner
     * "application" loop and is the basis for
     * {@link org.ocap.application.OcapAppAttributes#getIdentifier}.
     * 
     * @see "MHP-1.0.2: 10.4.6 Syntax of the AIT"
     */
    public AppID id;

    /**
     * Corresponds to the application name(s) contained in the single instance
     * of an <i>application_name_descriptor()</i> for an application; and is the
     * basis for {@link org.ocap.application.OcapAppAttributes#getName} and
     * {@link org.ocap.application.OcapAppAttributes#getNames}.
     * 
     * @see "MHP-1.0.2: 10.7.4.1 Application Name Descriptor"
     */
    public Hashtable names;

    /**
     * Corresponds to the <i>version.major</i>, <i>version.minor</i>, and
     * <i>version.micro</i> fields contained within the
     * <i>application_descriptor</i> of the inner "application" loop; and is the
     * basis for {@link org.ocap.application.OcapAppAttributes#getVersions}.
     * <p>
     * This <code>Hashtable</code> is composed of <code>int[3]</code> entries,
     * keyed by <code>Integer</code> objects indicating the
     * <i>application_profile</i>.
     * <p>
     * Note that OCAP-1.0 does not follow the MHP profile model.
     * 
     * @see "MHP-1.0.2: 10.7.3 Application Descriptor"
     * @see "OCAP-1.0: 18.2.1.1 Deviations from DVB-MHP Specification: System Constants"
     */
    public Hashtable versions;

    /**
     * Corresponds to the <i>application_profile</i>s contained within the
     * <i>application_descriptor</i> found within the inner "application" loop;
     * and is the basis for
     * {@link org.ocap.application.OcapAppAttributes#getProfiles}.
     * <p>
     * Element <code>profiles[<i>i</i>]</code> corresponds to
     * <code>versions[<i>i</i>]</code>.
     * 
     * @see "MHP-1.0.2: 10.7.3 Application Descriptor"
     */
    // public int[] profiles;

    /**
     * Corresponds to the <i>version.major</i>, <i>version.minor</i>, and
     * <i>version.micro</i> fields contained within the
     * <i>application_descriptor</i> of the inner "application" loop; and is the
     * basis for {@link org.ocap.application.OcapAppAttributes#getVersions}.
     * <p>
     * Element <code>versions[<i>i</i>]</code> corresponds to
     * <code>profiles[<i>i</i>]</code>. Each entry in
     * <code>versions</i> is a 3-element array such that
     * entries 0, 1, and 2 correspond to the major, minor, and micro versions
     * respectively.
     * 
     * @see "MHP-1.0.2: 10.7.3 Application Descriptor"
     */
    // public int[][] versions;

    /**
     * Corresponds to the <i>service_bound_flag</i> found in the
     * <i>application_descriptor()</i> of the inner "application" loop; and is
     * the basis for
     * {@link org.ocap.application.OcapAppAttributes#isServiceBound}.
     * 
     * @see "MHP-1.0.2: 10.7.3 Application Descriptor"
     */
    public boolean serviceBound;

    /**
     * Value for {@link #visibility} which indicates that this application shall
     * not be visible either to applications via an application listing API or
     * to users via the navigator with the exception of any error reporting or
     * logging facility, etc.
     * 
     * @see "MHP-1.0.2: 10.7.3 Application Descriptor"
     */
    public static final int NON_VISIBLE = 0;

    /**
     * Value for {@link #visibility} which indicates that this application shall
     * be not be visible to users but shall be visible to applications via an
     * application listing API.
     * 
     * @see "MHP-1.0.2: 10.7.3 Application Descriptor"
     */
    public static final int LISTING_ONLY = 1;

    /**
     * Value for {@link #visibility} which indicates that this application can
     * be visible to users and shall be visible to applications via the
     * application listing API.
     * 
     * @see "MHP-1.0.2: 10.7.3 Application Descriptor"
     */
    public static final int VISIBLE = 3;

    /**
     * Corresponds to the <i>visibility</i> field found in the
     * <i>application_descriptor()</i> of the inner "application" loop. This
     * field is not exposed via <code>OcapAppAttributes</code>.
     * 
     * @see "MHP-1.0.2: 10.7.3 Application Descriptor"
     */
    public int visibility;

    /**
     * Corresponds to the <i>application_priority</i> entry of the
     * <i>application_descriptor()</i> of the inner "application" loop; and is
     * the basis for {@link org.ocap.application.OcapAppAttributes#getPriority}.
     * 
     * @see "MHP-1.0.2: 10.7.3 Application Descriptor"
     */
    public int priority;

    /**
     * Corresponds to the <i>icon_locator</i> entry carried in the
     * <i>application_icons_descriptor()</i>; and is the basis for
     * {@link org.ocap.application.OcapAppAttributes#getAppIcon}.
     * 
     * @see "MHP-1.0.2: 10.7.4.2 Application icons descriptor"
     */
    public String iconLocator;

    /**
     * Corresponds to the <i>icon_flags</i> field carried in the
     * <i>application_icons_descriptor()</i>; and is the basis for
     * {@link org.ocap.application.OcapAppAttributes#getAppIcon}.
     * 
     * @see "MHP-1.0.2: 10.7.4.2 Application icons descriptor"
     */
    public int iconFlags;

    /**
     * Corresponds to the <i>parameter</i> entries of the
     * <i>dvb_j_application_descriptor()</i>; and is the basis for
     * {@link javax.tv.xlet.XletContext#getProperty} with a parameter of
     * <code>XletContext.ARGS</code>.
     * 
     * @see "MHP-1.0.2: 10.9.1 DVB-J application descriptor"
     */
    public String[] parameters;

    /**
     * Corresponds to the <i>base_directory</i> entry in the
     * <i>dvb_j_application_location_descriptor()</i> contained in the inner
     * "application" loop; and is the basis for
     * {@link org.ocap.application.OcapAppAttributes#getProperty} with a
     * parameter of <code>"dvb.j.location.base"</code>.
     * 
     * @see "MHP-1.0.2: 10.9.2 DVB-J application location descriptor"
     */
    public String signaledBasedir;
    
    // This is a slightly modified version of the base directory (with
    // leading/trailing slashes removed) that makes it easier for us to
    // create file paths in a consistent fashion
    public String baseDirectory;
    
    /**
     * Corresponds to the <i>intial_class</i> entry in the
     * <i>dvb_j_application_location_descriptor()</i> contained in the inner
     * "application" loop. This is not exposed via
     * <code>OcapAppAttributes</code> but indicates the name of the
     * <code>Xlet</code> implementation for this application.
     * 
     * @see "MHP-1.0.2: 10.9.2 DVB-J application location descriptor"
     */
    public String className;

    /**
     * Corresponds to the <i>classpath_extension</i> entry in the
     * <i>dvb_j_application_location_descriptor()</i> contained in the inner
     * "application" loop; and is the basis for
     * {@link org.ocap.application.OcapAppAttributes#getProperty} with a
     * parameter of <code>"dvb.j.cpath.extension"</code>.
     * 
     * @see "MHP-1.0.2: 10.9.2 DVB-J application location descriptor"
     */
    public String[] classPathExtension;

    /**
     * Corresponds to the <i>transport_protocol_descriptor()</i>s contained
     * within both the outer "common" descriptor loop and the inner
     * "application" loop. May provide the basis for implementation of
     * {@link org.ocap.application.OcapAppAttributes#getProperty} with an
     * argument of <code>"dvb.transport.oc.component.tag"</code>.
     */
    public TransportProtocol[] transportProtocols;

    /**
     * Corresponds to the prefetch descriptor, if present.
     * 
     * @see "MHP-1.0.2: 10.8.3.2 Pre-fetch descriptor"
     */
    public Prefetch prefetch;

    /**
     * Corresponds to the DII location descriptor, if present.
     * 
     * @see "MHP-1.0.2: 10.8.3.3 DII location descriptor"
     */
    public DiiLocation diiLocation;

    /**
     * Corresponds to the IP routing descriptors, if present.
     * 
     * @see "MHP-1.0.2: 10.8.2 IP Routing Descriptors"
     */
    public Vector ipRouting;

    /**
     * Corresponds to the <i>registered_api_descriptor</i> entries, if present.
     * 
     * @see "OCAP-1.0: 11.2.2.4 Registered API Descriptor"
     */
    public String[] registeredApi;

    /**
     * Corresponds to the Application Mode Descriptor. Modes are defined in:
     * {@link org.ocap.application.OcapAppAttributes} OCAP-1.1.1: 11.2.2.3.18
     */
    public int application_mode = OcapAppAttributes.LEGACY_MODE;

    /**
     * List of address labels associated with this signaled application. If this
     * application was not signaled with an
     * <i>addressable_application_descriptor</i>, this will always be null. If
     * this application was signaled with an
     * <i>addressable_application_descriptor</i>, but the label list was empty,
     * this will contain a 0-length array
     * 
     * @see �OCAP-1.0: 11.2.2.5.3 Addressable Application Descriptor�
     */
    public int[] addressLabels;

    /**
     * For <i>unbound</i> applications signalled in an XAIT, this corresponds to
     * the <i>version_number</i> field of the
     * <i>unbound_application_descriptor()</i>. For <i>bound</i> applications
     * signalled in an AIT, this is always zero. This field (when compared to
     * that of another <code>AppEntry</code> with identical {@link #id}) is the
     * basis for {@link org.ocap.application.OcapAppAttributes#hasNewVersion}.
     * <p>
     * Even though this is an XAIT-specific field, we place it here because
     * the stack needs bound apps to have a version -- we just always use zero
     * 
     * @see "OCAP-1.0: 11.2.2.3.15 Unbound Application Descriptor"
     */
    public long version = 0;

    /**
     * The (X)AIT instance that signaled this particular application entry
     */
    public Ait signalling;
    
    /**
     * @author Aaron Kamienski
     */
    public static class TransportProtocol
    {
        public int protocol;

        public int label;

        /**
         * The <i>remote_connection</i> field identifies whether the transport
         * connection is provided by a remote service or not. If
         * <code>true</code> then the transport connection should be acquired
         * from the signalled <i>service_id</i> (available via {@link #id}).
         */
        public boolean remoteConnection;

        /**
         * Corresponds to the <i>service_id</i> (i.e., sourceId) that carries
         * the transport. This should be ignored unless
         * {@link #isRemoteConnection} returns <code>true</code>.
         * 
         * @return the <i>service_id</i>
         */
        public int serviceId;
    }

    /**
     * An instance of <code>OcTransportProtocol</code> represents a
     * <i>transport_protocol_descriptor</i> specifying transport via object
     * carousel (OC).
     * <p>
     * Note that there are no fields for the <i>original_network_id</i> and
     * <i>transport_stream_id</i> fields as these default to zero and are
     * ignored for OCAP-1.0.
     * 
     * @see "OCAP-1.0: 11.2.1.6 Transport via OC"
     * @see "OCAP-1.0: 11.2.2.3.6 Transport via OC"
     * @see "MHP-1.0.2: 10.8.1.1 Transport via OC"
     * @author Aaron Kamienski
     */
    public static class OcTransportProtocol extends TransportProtocol
    {
        /**
         * The <i>component_tag</i> identifies the principal service component
         * that delivers the application.
         * <p>
         * An <code>OcapLocator</code> can be generated specifying the object
         * carousel in the following manner:
         * 
         * <pre>
         * OcTransportProtocol oc;
         * ...
         * int[] tags = { oc.componentTag };
         * OcapLocator loc = new OcapLocator(sourceId, -1, tags, null);
         * </pre>
         */
        public int componentTag;

        /**
         * Overrides <code>Object.toString()</code> to provide information about
         * this object.
         */
        public String toString()
        {
            return "OC[" + label + "," + remoteConnection + "," + this.serviceId + "," + componentTag + "]";
        }
    }

    /**
     * An instance of <code>IcTransportProtocol</code> represents a
     * <i>transport_protocol_descriptor</i> specifying transport via Interaction
     * Channel (using HTTP 1.1).
     * <p>
     * Note that support for "Transport via Interaction Channel" <i>is</i>
     * required in OCAP 1.0.
     * 
     * @see "OCAP-1.0: 11.2.1.7 Transport via Interaction Channel"
     * @see "OCAP-1.0: 11.2.2.3.9 Transport via Interaction Channel"
     * @author Aaron Kamienski
     */
    public static class IcTransportProtocol extends TransportProtocol
    {
        /**
         * The HTTP URL to a top-level directory that should be mounted.
         */
        public Vector urls = new Vector();

        /**
         * Overrides <code>Object.toString()</code> to provide information about
         * this object.
         */
        public String toString()
        {
            StringBuffer sb = new StringBuffer();
            sb.append("IC[");
            for (int i = 0; i < urls.size(); i++)
                sb.append((String)urls.elementAt(i) + ",");
            sb.append("]");
            return sb.toString();
        }
    }

    /**
     * An implementation-specific <code>TransportProtocol</code> used for
     * <i>resident</i> applications. This indicates that a local filesystem is
     * used to acquire the appliations.
     * <p>
     * A {@link #protocol} of <code>0xFFFF</code> is used to indicate
     * <code>LocalTransportProtocol</code>. A {@link #label} of
     * <code>0xFF</code> is used to indicate <code>LocalTransportProtocol</code>
     * . The {@link #remoteConnection} field is <i>always</i> <code>false</code>.
     * 
     * @author Aaron Kamienski
     */
    public static class LocalTransportProtocol extends TransportProtocol
    {
        public LocalTransportProtocol()
        {
            protocol = 0xFFFF;
            label = 0xFF;
            remoteConnection = false;
        }
    }

    public static class Prefetch
    {
        public int transportLabel;

        public Pair[] info;

        public static class Pair
        {
            public String label;

            public int priority;
        }

        public String toString()
        {
            String str = transportLabel + "[";
            for (int i = 0; i < info.length; ++i)
                str += info[i].label + ":" + info[i].priority + ",";
            str += "]";
            return str;
        }
    }

    public static class DiiLocation
    {
        public int transportLabel;

        public int[] diiIdentification;

        public int[] associationTag;

        public String toString()
        {
            String str = transportLabel + "[";
            for (int i = 0; i < diiIdentification.length; ++i)
                str += diiIdentification[i] + ":" + associationTag[i] + ",";
            str += "]";
            return str;
        }
    }

    public static class IpRoutingEntry
    {
        public int componentTag;

        public int port;
    }

    public static class Ipv4RoutingEntry extends IpRoutingEntry
    {
        public int address;

        public int mask;
    }

    public static class Ipv6RoutingEntry extends IpRoutingEntry
    {
        public int[] address;

        public int[] mask;
    }

    /**
     * Clones this <code>AppEntry</code>.
     * 
     * @return a shallow copy of this <code>AppEntry</code>.
     */
    public AppEntry copy()
    {
        try
        {
            return (AppEntry) clone();
        }
        catch (Exception e)
        {
            return null;
        }
    }
    
    /**
     * Returns whether this app is unsigned (0), signed (1), or dual-signed (2)
     * based on the signaling information.  Only dual-signed, unbound apps can
     * request monapp permissions
     * 
     * OCAP Table 11-5 Describes the AppID ranges that delineate unsigned/signed/dual-signed
     * OCAP Table 11-7 Describes the ServiceID ranges for unbound apps
     */
    public int getNumSigners()
    {
        int aid = id.getAID();
        if (aid < 0x4000)
            return 0;
        
        return 1;
    }
    
    /**
     * Overrides <code>Object.toString()</code> to provide information about
     * this <code>AppEntry</code>.
     */
    public String toString()
    {
        StringBuffer str = new StringBuffer("AppEntry[");
        String comma;
        str.append("id=" + id + ",");
        str.append("names=[");
        if (names != null)
        {
            comma = "";
            for (Enumeration keys = names.keys(); keys.hasMoreElements();)
            {
                String key = (String) keys.nextElement();
                str.append(comma + key + ":" + names.get(key));
                comma = ",";
            }
        }
        str.append("],");
        str.append("code=" + controlCode + ",");
        str.append("bound=" + serviceBound + ",");
        str.append("visibility=" + visibility + ",");
        str.append("priority=" + priority + ",");
        str.append("baseDir=" + signaledBasedir + ",");
        str.append("classPath=[");
        if (classPathExtension != null)
        {
            comma = "";
            for (int i = 0; i < classPathExtension.length; ++i)
            {
                str.append(comma + classPathExtension[i]);
                comma = ",";
            }
        }
        str.append("],");
        str.append("classname=" + className + ",");
        str.append("params=[");
        if (parameters != null)
        {
            comma = "";
            for (int i = 0; i < parameters.length; ++i)
            {
                str.append(comma + "\"" + parameters[i] + "\"");
                comma = ",";
            }
        }
        str.append("],");
        str.append("icon=" + iconLocator + ",");
        str.append("icon_flags=" + Integer.toHexString(iconFlags) + ",");

        str.append("transport_protocols=[");
        if (transportProtocols != null)
        {
            comma = "";
            for (int i = 0; i < transportProtocols.length; ++i)
            {
                str.append(comma + transportProtocols[i]);
                comma = ",";
            }
        }
        str.append("],");
        str.append("prefetch=" + prefetch + ",");
        str.append("dii=" + diiLocation + ",");
        if (registeredApi != null)
        {
            str.append("registered_apis=[");
            comma = "";
            for (int i = 0; i < registeredApi.length; ++i)
            {
                str.append(comma + registeredApi[i]);
                comma = ",";
            }
            str.append("],");
        }
        if (addressLabels != null)
        {
            str.append("address_labels=[");
            comma = "";
            for (int i = 0; i < addressLabels.length; ++i)
            {
                str.append(comma + addressLabels[i]);
                comma = ",";
            }
            str.append("],");
        }
        str.append("]");

        return str.toString();
    }
    
    /**
     * Standard comparator used to sort applications by AppID
     */
    public static class AppIDCompare implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            AppEntry app1 = (AppEntry) o1;
            AppEntry app2 = (AppEntry) o2;

            int cmp = app1.id.getOID() - app2.id.getOID();
            if (cmp == 0)
            {
                cmp = app1.id.getAID() - app2.id.getAID();
            }
            return cmp;
        }
    }
    
    /**
     * Standard comparator used to sort applications by AppID,
     * then by priority (ascending)
     */
    public static class AppIDPrioCompare extends AppIDCompare
    {
        public int compare(Object o1, Object o2)
        {
            AppEntry app1 = (AppEntry) o1;
            AppEntry app2 = (AppEntry) o2;
            
            int cmp = super.compare(o1, o2);
            if (cmp == 0)
            {
                cmp = app1.priority - app2.priority;
            }
            return cmp;
        }
    }
}
