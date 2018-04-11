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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.signalling.AbstractServiceEntry;
import org.cablelabs.impl.signalling.Ait;
import org.cablelabs.impl.signalling.XAppEntry;
import org.cablelabs.impl.signalling.Xait;
import org.cablelabs.impl.signalling.XaitImpl;
import org.ocap.application.OcapAppAttributes;

/**
 * Support for XAIT-type information in a properties file. This is suitable for
 * testing, providing an implementation in absence of network-based signalling,
 * and supportings resident applications.
 * 
 * <p>
 * The properties file is composed of the following sets of information:
 * <ul>
 * <li>version
 * <li>transport protocols
 * <li>abstract service(s)
 * <li>application(s)
 * </ul>
 * The <code>version</code> field indicates the version of the AIT represented
 * by the properties file. Changes to the properties file are only expected and
 * honored when the version changes. The version number is generally required,
 * but is assumed to be zero if not present. The format is as follows:
 * 
 * <pre>
 * version=<i>version</i>
 * </pre>
 * 
 * The definitions for transport protocols is the same as for {@link AitProps}.
 * <p>
 * Any number of abstract services can be specified (the default maximum of 32
 * can be overridden using a <code>maxsvcs</code> definition). Services are
 * specified using entries of the form:
 * 
 * <pre>
 * svc.<i>i</i>.<i>field</i>=<i>value</i>
 * </pre>
 * 
 * The following table describes the supported fields:
 * <table border>
 * <tr>
 * <td><code>service_id</code>, <code>service</code>, or <code>id</code></td>
 * <td>Service id: should be in 0x010000-0x01FFFF for hostapps and
 * 0x020000-0xFFFFFF for MOS apps</td>
 * <td>required</td>
 * </tr>
 * <tr>
 * <td><code>auto_select</code> or <code>autoselect</code></td>
 * <td>Specifies whether system should auto-select the service</td>
 * <td><code>false</code></td>
 * </tr>
 * <tr>
 * <td><code>name</code></td>
 * <td>The name of the service</td>
 * <td>Required</td>
 * </tr>
 * </table>
 * <p>
 * The definitions for applications extend that used for {@link AitProps}. Here
 * are the additional fields:
 * <table border>
 * <tr>
 * <th>Field</th>
 * <th>Description</th>
 * <th>Required/Default</th>
 * </tr>
 * <tr>
 * <td><code>service</code></td>
 * <td>The abstract service that this app is associated with</td>
 * <td>Required for XAIT; can default to 0x012345 for hostapp.properties</td>
 * </tr>
 * <tr>
 * <td><code>version</code></td>
 * <td>Application version</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td><code>storage_priority</code></td>
 * <td>Storage priority</td>
 * <td>0</td>
 * </tr>
 * <tr>
 * <td><code>launch_order</code></td>
 * <td>Launch order</td>
 * <td>0</td>
 * </tr>
 * <tr>
 * <td><code>api.<i>j</i></code></td>
 * <td>Registered API Name</td>
 * <td></td>
 * </tr>
 * </table>
 * 
 * @author Aaron Kamienski
 */
public class XaitProps extends AitProps
{
    private static final Logger log = Logger.getLogger(XaitProps.class);

    /**
     * Constructs an empty XaitProps object. The {@link #go(InputStream)} method
     * must be called to populate this instance.
     * 
     * @param source
     *            the source of this signalling
     */
    public XaitProps(int source)
    {
        ait = new XaitImpl();
        this.source = source;
    }

    /**
     * Return a signalling object based on the parsed data
     * 
     * @return the signalling
     */
    public Ait getSignalling()
    {
        XaitImpl xait = (XaitImpl)ait;
        xait.initialize(version,externalAuth,allApplications,attributeMap,addrGroups,
                        source,privilegedCertificates,abstractServices);
        return ait;
    }

    /**
     * Overridden to return an XaitProps-specific implementation.
     */
    protected AppInfo createAppInfo()
    {
        return new XAppInfo();
    }

    /**
     * Parses a single application entry.
     */
    protected AppInfo parseEntry(int i, Properties props)
    {
        try
        {
            AppInfo app = super.parseEntry(i, props);
            if (app != null)
            {
                String val = getValue(props, i, "service");
                if (val == null && source == Xait.HOST_DEVICE)
                {
                    // Create a dummy service
                    val = "0x012345";
                    // Remember it...
                    Integer key = new Integer(0x012345);
                    AbstractServiceEntry service = (AbstractServiceEntry) abstractServices.get(key);
                    if (service == null)
                    {
                        service = new AbstractServiceEntry();
                        service.id = 0x012345;
                        service.name = "Default Service";
                        service.autoSelect = true;
                        service.apps = new Vector();

                        abstractServices.put(key, service);
                    }
                }

                XAppEntry xae = (XAppEntry)app.ae;
                xae.serviceId = parseInt(val);
                xae.version = parseLong(getValue(props, i, "version"), 0);
                xae.storagePriority = parseInt(getValue(props, i, "storage_priority"), 0);
                xae.launchOrder = parseInt(getValue(props, i, "launch_order"), 0);

                // Look up service
                AbstractServiceEntry service =
                    (AbstractServiceEntry)abstractServices.get(new Integer(xae.serviceId));
                if (service == null)
                {
                    return null;
                }
                service.apps.addElement(xae);
            }
            return app;
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("Invalid app." + i + " entry", e);
            }
            return null;
        }
    }

    /**
     * Overrides {@link AitProps#acc(java.lang.String)}. Enforces requirement
     * that {@link OcapAppAttributes#REMOTE} be interpreted as
     * {@link OcapAppAttributes#PRESENT} (per OCAP 11.2.2.3 -- ECN 913).
     */
    protected int acc(String str)
    {
        int acc = super.acc(str);
        return (acc == OcapAppAttributes.REMOTE) ? OcapAppAttributes.PRESENT : acc;
    }

    /**
     * Extracts the value for the given service index/key pair.
     * 
     * @param i
     *            the service index
     * @param key
     *            the key
     */
    private String getServiceValue(Properties props, int i, String key)
    {
        return props.getProperty("svc." + i + "." + key);
    }

    /**
     * Parses <code>privcertbytes</code> or <code>privcertbytes.i</code>
     * entries. If none are found, then a default entry is used. If any are
     * invalid, then an <code>IOException</code> is thrown.
     * 
     * @throws IOException
     *             if data is of an unacceptable format
     */
    private void parsePrivCertBytes(Properties props) throws IOException
    {
        String privcert = props.getProperty("privcertbytes");
        if (privcert != null)
        {
            if (privcert.startsWith("0x"))
            {
                privcert = privcert.substring(2);
            }
            if (privcert.length() == 0 || privcert.length() % 40 != 0)
            {
                throw new IOException("Invalid privcertbytes specification");
            }

            byte[] bytes = new byte[privcert.length() / 2];
            hex2Bytes(bytes, privcert, 0);
            privilegedCertificates = bytes;

            return;
        }

        final int maxpriv = parseInt(props.getProperty("maxprivcerts"), 32);

        Vector entries = new Vector();
        for (int i = 0; i < maxpriv; ++i)
        {
            privcert = props.getProperty("privcertbytes." + i);
            if (privcert != null)
            {
                if (privcert.startsWith("0x"))
                {
                    privcert = privcert.substring(2);
                }
                if (privcert.length() != 40)
                {
                    throw new IOException("Invalid privcertbytes.i specification");
                }
                entries.addElement(privcert);
            }
        }

        if (entries.size() > 0)
        {
            byte[] bytes = new byte[entries.size() * 20];
            int index = 0;
            for (Enumeration e = entries.elements(); e.hasMoreElements(); index += 20)
            {
                privcert = (String) e.nextElement();

                hex2Bytes(bytes, privcert, index);
            }
            privilegedCertificates = bytes;
        }
        else
        {
            // No priv certs have been set
            privilegedCertificates = "Dummy Priv Cert Data".getBytes();
        }
    }

    /**
     * Parses the given hexadecimal string, converting the result into bytes.
     * The result is written into the given destination array starting at the
     * given offset.
     * 
     * @param dest
     *            destination array
     * @param hex
     *            hex string
     * @param ofs
     *            offset in destination array
     */
    private int hex2Bytes(byte[] dest, String hex, int ofs) throws NumberFormatException
    {
        char[] chars = hex.toCharArray();

        String HEX = "0123456789ABCDEF";
        int idx = ofs;
        for (int i = 0; i < chars.length && idx < dest.length; i += 2, ++idx)
        {
            int x1 = HEX.indexOf(Character.toUpperCase(chars[i])) * 16;
            int x2 = HEX.indexOf(Character.toUpperCase(chars[i + 1]));

            if (x1 < 0 || x2 < 0)
            {
                throw new NumberFormatException("Invalid byte: " + chars[i] + chars[i + 1]);
            }

            dest[idx] = (byte) (x1 | x2);
        }
        return idx - ofs;
    }

    /**
     * Parses the set of <code>Properties</code> for application and abstract
     * service data.
     * 
     * @throws IOException
     *             if data is of an unacceptable format
     */
    protected void parse(Properties props) throws IOException
    {
        final int maxsvcs = 32;

        for (int i = 0; i < maxsvcs; ++i)
        {
            String val;
            if ((val = getServiceValue(props, i, "service_id")) != null
                    || (val = getServiceValue(props, i, "service")) != null
                    || (val = getServiceValue(props, i, "id")) != null)
            {
                int serviceId = parseInt(val);

                if (source == Xait.HOST_DEVICE)
                {
                    if (serviceId < 0x10000 || serviceId > 0x1FFFF) continue;
                }
                else if (serviceId < 0x20000 || serviceId > 0x01000000)
                {
                    continue;
                }
                AbstractServiceEntry service = new AbstractServiceEntry();
                service.id = serviceId;
                val = getServiceValue(props, i, "auto_select");
                if (val == null)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Must provided auto_select value for abstract service " + i);
                    }
                    continue;
                }
                service.autoSelect = parseBoolean(val.trim(),false);
                service.name = getServiceValue(props, i, "name");
                service.apps = new Vector();

                abstractServices.put(new Integer(serviceId), service);
            }
        }

        parsePrivCertBytes(props);

        super.parse(props);
    }

    /**
     * Extends AppInfo to add support for XAppSignalling.
     */
    protected class XAppInfo extends AppInfo
    {
        public XAppInfo()
        {
            ae = new XAppEntry();
        }
    }

    // Data required to construct an XAIT
    protected int source;
    protected byte[] privilegedCertificates;
    protected Hashtable abstractServices = new Hashtable();
}
