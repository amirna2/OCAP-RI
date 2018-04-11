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
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.ocap.application.OcapAppAttributes;

import org.cablelabs.impl.signalling.Ait;
import org.cablelabs.impl.signalling.Ait.ExternalAuthorization;
import org.cablelabs.impl.signalling.AitImpl;
import org.cablelabs.impl.signalling.AitImpl.AddressingDescriptor;
import org.cablelabs.impl.signalling.AitImpl.Comparison;
import org.cablelabs.impl.signalling.AitImpl.LogicalOp;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * Support for AIT-type information in a properties file. This is suitable for
 * testing, providing an implementation in absence of network-based signalling.
 * 
 * <p>
 * The properties file is composed of the following sets of information:
 * <ul>
 * <li>version
 * <li>external authorization(s)
 * <li>transport protocols
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
 * Any number of external authorization specifications can be included (the
 * default maximum of 32 can be overridden via a <code>maxauth</code>
 * definition). The specification includes an application identifier and a
 * priority. Application identifiers with an AID of 0xFFFE and 0xFFFF have the
 * same wildcard meaning as for the AIT. The format is as follows:
 * 
 * <pre>
 * authorized.<i>i</i>=<i>appid</i>:<i>priority</i>
 * </pre>
 * 
 * Any number of transport protocols can be specified (the default maximum of 32
 * can be overridden using a <code>maxtp</code> definition). Transport protocols
 * are specified using the following form:
 * 
 * <pre>
 * transport.<i>i</i>=<i>type</i>
 * transport.<i>i</i>.<i>field</i>=<i>value</i>
 * </pre>
 * 
 * The following protocol types are supported:
 * <ul>
 * <li> <code>oc</code>
 * <li> <code>ip</code>
 * <li> <code>local</code>
 * </ul>
 * The following fields are supported:
 * <table border>
 * <tr>
 * <th>Field</th>
 * <th>Description</th>
 * <th>Type</th>
 * </tr>
 * <tr>
 * <td><code>remote</code></td>
 * <td>Boolean; indicates remote protocol</td>
 * <td><code>oc</code>, <code>ip</code></td>
 * </tr>
 * <tr>
 * <td><code>service</code></td>
 * <td>Service id, only used if <code>remote=true</code></td>
 * <td><code>oc</code>, <code>ip</code></td>
 * </tr>
 * <tr>
 * <td><code>component</code></td>
 * <td>Principal component of Object Carousel</td>
 * <td><code>oc</code></td>
 * </tr>
 * <tr>
 * <td><code>alignment</code></td>
 * <td>Boolean</td>
 * <td><code>ip</code></td>
 * </tr>
 * <tr>
 * <td><code>url.<i>i</i></code></td>
 * <td>URL</td>
 * <td><code>ip</code></td>
 * </tr>
 * <tr>
 * <td><code>prefetch</code></td>
 * <td>Comma-delimited module names</td>
 * <td><code>oc</code></td>
 * </tr>
 * <tr>
 * <td><code>dii.<i>i</i></code></td>
 * <td>DII identification and association tag, separated by comma</td>
 * <td><code>oc</code></td>
 * </tr>
 * </table>
 * By default, if no transport protocols are defined, an implicit <i>local</i>
 * transport protocol is defined, with an index of <code>0xFF</code>.
 * <p>
 * Only one <code>oc</code> entry can include a <code>prefetch</code> and/or
 * <code>dii</code> entry. It is unspecified which is remembered.
 * 
 * <p>
 * Any number of applications can be specified (the default maximum of 32 can be
 * overridden using a <code>maxapps</code> definition). Applications are
 * specified using entries of the form:
 * 
 * <pre>
 * app.<i>i</i>.<i>field</i>=<i>value</i>
 * </pre>
 * 
 * The following table describes the supported fields:
 * <table border>
 * <tr>
 * <th>Field</th>
 * <th>Description</th>
 * <th>Required/Default</th>
 * </tr>
 * <tr>
 * <td><code>application_identifier</code></td>
 * <td>Application Identifier</td>
 * <td>required</td>
 * </tr>
 * <tr>
 * <td><code>application_control_code</code></td>
 * <td>One of <code>PRESENT</code>, <code>AUTOSTART</code>, <code>DESTROY</code>, <code>KILL</code>, or <code>REMOTE</code></td>
 * <td>required</td>
 * </tr>
 * <tr>
 * <td><code>application_name</code></td>
 * <td>English name</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td><code>application_name.<i>i</i></code></td>
 * <td>Language-specific name expressed as <i>lang</i>,<i>Name</i>. E.g.,
 * <code>eng,EnglishName</code></td>
 * <td></td>
 * </tr>
 * <tr>
 * <td><code>service_bound</code></td>
 * <td>Boolean; indicates if service bound</td>
 * <td>true</td>
 * </tr>
 * <tr>
 * <td><code>visibility</code></td>
 * <td>One of <code>INVISIBLE</code>, <code>VISIBLE</code>,
 * <code>VISIBLE-TO-APPS-ONLY</code></td>
 * <td>Required</td>
 * </tr>
 * <tr>
 * <td><code>priority</code></td>
 * <td>Application priority (0-254)</td>
 * <td>Required</td>
 * </tr>
 * <tr>
 * <td><code>icon_locator</code></td>
 * <td>Path relative to base_directory for icons</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td><code>icon_flags</code></td>
 * <td>Icon flags as appropriate for AppIcon</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td><code>base_directory</code></td>
 * <td>Base directory for app</td>
 * <td>Required</td>
 * </tr>
 * <tr>
 * <td><code>initial_class_name</code></td>
 * <td>Name of Xlet implementation class (packages delimited by '.')</td>
 * <td>Required</td>
 * </tr>
 * <tr>
 * <td><code>classpath_extension</code>
 * <td>Semi-colon separated path classpath entries</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td><code>args.<i>i</i></code></td>
 * <td>Xlet arguments</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td><code>address_label.<i>i</i></code></td>
 * <td>Integer; Application addressing labels</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td><code>transport</code></td>
 * <td>Comma-separated list of transport protocol indices</td>
 * <td>Defaults to all defined transport protocols</td>
 * </tr>
 * <tr>
 * <td><code>application_mode</code></td>
 * <td>One of <code>LEGACY</code>, <code>NORMAL</code>,
 * <code>CROSSENVIRONMENT</code></td>
 * <code>BACKGROUND</code></td> <code>PAUSED</code></td>
 * 
 * </table>
 * 
 * Any number of attribute mapping descriptors can be specified (the default
 * maximum of 32 can be overridden using a <code>maxattr</code> definition).
 * Attribute mapping descriptors are specified using the following form:
 * 
 * <pre>
 * attribute.<i>i</i>.id=<i>attributeID</i>
 * attribute.<i>i</i>.name=<i>attributeName</i>
 * </pre>
 * 
 * Any number of addressing descriptors can be specified (the default maximum of
 * 32 can be overridden using a <code>maxaddr</code> definition). Addressing
 * descriptors are specified using the following form:
 * 
 * <pre>
 * addressing.<i>i</i>.<i>field</i>=<i>value</i>
 * </pre>
 * 
 * The following fields are supported:
 * <table border>
 * <tr>
 * <th>Field</th>
 * <th>Description</th>
 * <th>Type</th>
 * </tr>
 * <tr>
 * <td><code>group</code></td>
 * <td>Integer; Addressing group ID</td>
 * <td>Required</td>
 * </tr>
 * <tr>
 * <td><code>label</code></td>
 * <td>Integer; Application addressing label</td>
 * <td>Required</td>
 * </tr>
 * <tr>
 * <td><code>priority</code></td>
 * <td>Integer; Addressing descriptor priority</td>
 * <td>Required</td>
 * </tr>
 * </table>
 * 
 * Each addressing descriptor must contain at least one evaluation expression.
 * At most 32 expressions can be specified for each addressing descriptor.
 * Expressions are specified using the following form:
 * 
 * <pre>
 * addressing.<i>i</i>.expression.<i>j</i>=<i>logicalExpression</i>
 * </pre>
 * 
 * where: <i>logicalExpression</i> := <i>logical_comp</i> | <i>comparison</i> |
 * <i>sec_comparison</i> <i>logical_comp</i> := <code>TRUE</code> |
 * <code>NOT</code> | <code>AND</code> | <code>OR</code> <i>comparator</i> :=
 * <code>&lt;</code> | <code>&lt;=</code> | <code>==</code> | <code>&gt;=</code>
 * | <code>&gt;</code> <i>comparison</i> := attributeID <i>comparator</i>
 * attributeValue <i>sec_comparison</i> := <code>S</code> <i>comparison</i>
 * 
 * NOTE: attributeID is the ID from the corresponding attribute mapping
 * descriptor. Addressing expressions should be specified in the order that they
 * should be pushed on to the operation stack (See OCAP-1.0 11.2.2.5.1
 * Addressing Descriptor)
 * 
 * @author Aaron Kamienski
 * @author Greg Rutz
 */
class AitProps
{
    /**
     * Constructs an empty XaitProps object. {@link #parse} must be called to
     * populate this instance.
     */
    public AitProps()
    {
        ait = new AitImpl();
    }

    /**
     * Return a signalling object based on the parsed data
     * 
     * @return the signalling
     */
    public Ait getSignalling()
    {
        ait.initialize(version,externalAuth,allApplications,attributeMap,addrGroups);
        return ait;
    }

    /**
     * To be overridden by subclass.
     */
    protected AppInfo createAppInfo()
    {
        return new AppInfo();
    }

    /**
     * Instructs the object to parse the properties file specified by the given
     * <code>InputStream</code>.
     * 
     * @param is
     *            the <code>InputStream</code> to parse
     * @param lastVersion
     *            the previously read version (to ignore); or -1 if no known
     *            previous version
     */
    public void parse(InputStream is, int lastVersion) throws IOException
    {
        Properties props = new Properties();
        props.load(is);

        String verStr = props.getProperty("version");
        this.version = parseInt(verStr, 0);

        if (lastVersion != -1 && version == lastVersion)
        {
            // Skip if same version
            return;
        }

        parse(props);
    }

    /**
     * Extracts the value for the given app index/key pair.
     * 
     * @param i
     *            the app index
     * @param key
     *            the key
     */
    protected String getValue(Properties props, int i, String key)
    {
        return props.getProperty("app." + i + "." + key);
    }

    /**
     * Parses a single application entry.
     */
    protected AppInfo parseEntry(int i, Properties props)
    {
        try
        {
            final int maxargs = 32;
            String appID = getValue(props, i, "application_identifier");
            if (appID == null)
                return null;
            
            AppInfo app = createAppInfo();
            AppEntry ae = app.ae;

            ae.id = appid(appID);
            ae.controlCode = acc(getValue(props, i, "application_control_code"));
            ae.names = new Hashtable();
            String name = getValue(props, i, "application_name");
            if (name != null)
                ae.names.put("eng", name);
            else
            {
                for (int j = 0; j < maxargs; ++j)
                {
                    if ((name = getValue(props, i, "application_name." + j)) == null)
                    {
                        break;
                    }
                    
                    // Interpret as application_name.0=eng,Name
                    int idx = name.indexOf(',');
                    if (idx <= -1) break;
                    ae.names.put(name.substring(0, idx), name.substring(idx + 1));
                }
            }
            ae.versions = new Hashtable();

            // ocap 1.1.1 defines profile 0x102 w/version 1.1.1 (see section
            // 18.2.1.1) - use this as default
            String defaultProfile = "0x102";

            int defaultMajorVersion = 1;
            int defaultMinorVersion = 1;
            int defaultMicroVersion = 1;

            /*
             * Examine application profile/version information...default profile
             * 0x102, version 1.1.1 will be assigned if no profile is provided
             * 
             * Multiple profiles are supported per application. Profile and
             * version values can int or hex (hex prefixed with 0x)
             * 
             * Example entries: app.1.app_profiles.0.profile=0x102
             * app.1.app_profiles.0.version_major=0x1
             * app.1.app_profiles.0.version_minor=0x1
             * app.1.app_profiles.0.version_micro=0x1
             * app.1.app_profiles.1.profile=0x101
             * app.1.app_profiles.1.version_major=0x1
             * app.1.app_profiles.1.version_minor=0x0
             * app.1.app_profiles.1.version_micro=0x0
             */

            boolean profileEntryFound;
            int entry = 0;
            do
            {
                try
                {
                    // if an entry exists, profile, major, minor and micro are
                    // all required
                    String profile = getValue(props, i, "app_profiles." + entry + ".profile");

                    String major = getValue(props, i, "app_profiles." + entry + ".version_major");
                    String minor = getValue(props, i, "app_profiles." + entry + ".version_major");
                    String micro = getValue(props, i, "app_profiles." + entry + ".version_major");

                    if (profile != null && major != null && minor != null && micro != null)
                    {
                        int[] versionInfo = new int[3];
                        versionInfo[0] = parseInt(major);
                        versionInfo[1] = parseInt(minor);
                        versionInfo[2] = parseInt(micro);
                        int profileInt = parseInt(profile);
                        profileEntryFound = true;

                        ae.versions.put(new Integer(profileInt), versionInfo);
                    }
                    else
                    {
                        profileEntryFound = false;
                    }
                }
                catch (NumberFormatException nfe)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Unable to parse profile information for application: " + i + ", entry: " + entry, nfe);
                    }
                    // there was an entry, but it couldn't be parsed
                    profileEntryFound = true;
                }
                entry++;
            }
            while (profileEntryFound);

            // if no profile entries were found, assign defaults
            if (ae.versions.size() == 0)
            {
                ae.versions.put(new Integer(parseInt(defaultProfile)), new int[] { defaultMajorVersion,
                        defaultMinorVersion, defaultMicroVersion });
            }

            ae.serviceBound = parseBoolean(getValue(props, i, "service_bound"), true);
            ae.visibility = viz(getValue(props, i, "visibility"));
            ae.priority = parseInt(getValue(props, i, "priority"), 200);
            ae.application_mode = appMode(getValue(props, i, "application_mode"));

            // Addressing labels
            Vector labels = new Vector();
            for (int j = 0; j < maxargs; ++j)
            {
                String label;
                if ((label = getValue(props, i, "address_label." + j)) != null)
                {
                    try
                    {
                        labels.add(new Integer(Integer.parseInt(label)));
                    }
                    catch (NumberFormatException e)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Invalid Addressing Label (" + label + ") in app " + i);
                        }
                }
                }
                else
                    break;
            }
            // Copy labels into AppEntry
            if (!labels.isEmpty())
            {
                ae.addressLabels = new int[labels.size()];
                for (int idx = 0; idx < labels.size(); ++idx)
                    ae.addressLabels[idx] = ((Integer) labels.get(idx)).intValue();
            }

            try
            {
                ae.iconLocator = getValue(props, i, "icon_locator");
                ae.iconFlags = parseInt(getValue(props, i, "icon_flags"));
            }
            catch (Exception e)
            {
                ae.iconLocator = null;
            }

            Vector v = new Vector();
            for (int j = 0; j < maxargs; ++j)
            {
                String val;
                if ((val = getValue(props, i, "args." + j)) == null) break;
                v.addElement(val);
            }
            ae.parameters = new String[v.size()];
            v.copyInto(ae.parameters);

            // We keep the original version and then another version with leading
            // and trailing '/' characters removed
            ae.signaledBasedir = getValue(props, i, "base_directory");
            ae.baseDirectory = ae.signaledBasedir;
            if (ae.baseDirectory.startsWith("/"))
            {
                ae.baseDirectory = ae.baseDirectory.substring(1);
            }
            if (ae.baseDirectory.endsWith("/"))
            {
                ae.baseDirectory = ae.baseDirectory.substring(0, ae.baseDirectory.length()-1);
            }
            
            ae.className = getValue(props, i, "initial_class_name");
            String classpath = getValue(props, i, "classpath_extension");
            if (classpath == null || classpath.length() == 0)
            {
                ae.classPathExtension = new String[0];
            }
            else
            {
                StringTokenizer tok = new StringTokenizer(classpath, ";");
                String[] path = new String[tok.countTokens()];
                for (int j = 0; j < path.length; ++j)
                    path[j] = tok.nextToken();
                ae.classPathExtension = path;
            }

            // Transport protocols
            {
                String val = getValue(props, i, "transport");
                v = new Vector();

                // If no transport is referenced, include them all!!!!
                // Generally, this will just get a "default"
                // LocalTransportProtocol
                if (val == null)
                {
                    for (Enumeration e = transports.elements(); e.hasMoreElements();)
                        v.addElement(e.nextElement());
                }
                // Else, parse comma-separated list of transport tags
                else
                {
                    v = new Vector();
                    for (StringTokenizer tok = new StringTokenizer(val, ","); tok.hasMoreTokens();)
                    {
                        int tag = parseInt(tok.nextToken());
                        Object key = new Integer(tag);
                        Object tp = transports.get(key);
                        if (tp == null)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("Couldn't find transport " + tag + " in " + transports);
                            }
                        }
                        //findbugs complains about an NPE - as expected (and ignored).
                        tp.toString(); // force exception if not found
                        v.addElement(tp);

                        if (ae.prefetch == null)
                        {
                            ae.prefetch = (AppEntry.Prefetch) prefetches.get(key);
                        }
                        if (ae.diiLocation == null)
                        {
                            ae.diiLocation = (AppEntry.DiiLocation) diis.get(key);
                        }
                    }
                }

                ae.transportProtocols = new AppEntry.TransportProtocol[v.size()];
                v.copyInto(ae.transportProtocols);
            }

            // OCAP:Registered API descriptors
            ae.registeredApi = parseRegisteredApiDesc(i, props);

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
     * Parses the set of <i>external authorizations</i> for this service. The
     * expected format is:
     * 
     * <pre>
     * authorized.<i>i</i>=<i>appid</i>:<i>priority</i>>
     * </pre>
     * 
     * @throws IOException
     *             if data is of an unacceptable format
     */
    protected void parseExternalAuthorization(Properties props)
    {
        final int maxAuth = parseInt(props.getProperty("maxauth"), 32);
        try
        {
            for (int i = 0; i < maxAuth; ++i)
            {
                String val = props.getProperty("authorized." + i);
                int idx;
                if (val != null && (idx = val.indexOf(':')) > 0)
                {
                    AppID id = appid(val.substring(0, idx));
                    int priority = parseInt(val.substring(idx + 1));

                    if (priority < 255 && priority >= 0)
                    {
                        ExternalAuthorization auth = new ExternalAuthorization();
                        auth.id = id;
                        auth.priority = priority;
                        externalAuth.addElement(auth);
                    }
                }
            }
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
        }
    }

    /**
     * Parses the set of <i>tranport protocols</i> for this set of applications.
     * The expected format is:
     * 
     * <pre>
     * transport.<i>i</i>=<i>type</i>
     * transport.<i>i</i>.remote=<i>boolean</i>
     * </pre>
     * 
     * If <i>remote</i> is <code>true</code>, then the remote service id is
     * expected:
     * 
     * <pre>
     * transport.<i>i</i>.service=<i>id</i>
     * </pre>
     * 
     * Where <i>type</i> is one of <code>oc</code>, <code>ip</code>, or
     * <code>local</code>.
     * <p>
     * If the <i>type</i> is <code>oc</code>, then the component tag identifying
     * the principal component is specified as a number:
     * 
     * <pre>
     * transport.<i>i</i>.component=<i>tag</i>
     * </pre>
     * 
     * If the <i>type</i> is <code>ip</code>, then the alignment indicator and a
     * set of URLs are expected:
     * 
     * <pre>
     * transport.<i>i</i>.alignment=<i>boolean</i>
     * transport.<i>i</i>.url.<i>j</i>=<i>url</i>
     * ...
     * </pre>
     * 
     * As a side effect, the {@link #transports} hashtable is filled in. If no
     * transports are specified, then a default
     * <code>LocalTransportProtocol</code> is added.
     * 
     * @throws IOException
     *             if data is of an unacceptable format
     */
    protected void parseTransportProtocols(Properties props)
    {
        final int maxtp = parseInt(props.getProperty("maxtp"), 32);
        final int maxurl = parseInt(props.getProperty("maxurl"), 32);

        for (int i = 0; i < maxtp; ++i)
        {
            String pfx = "transport." + i;
            String val = props.getProperty(pfx);
            if (val == null) continue;

            AppEntry.TransportProtocol tp = null;
            try
            {
                val = val.trim();
                if ("oc".equals(val))
                {
                    // Read componentTag
                    AppEntry.OcTransportProtocol oc = new AppEntry.OcTransportProtocol();
                    oc.protocol = 1;
                    oc.componentTag = parseInt(props.getProperty(pfx + ".component"));

                    // Get Prefetch info, if available
                    Integer key = new Integer(i);
                    if (prefetches.get(key) == null)
                    {
                        AppEntry.Prefetch prefetch = parsePrefetch(props, pfx);
                        if (prefetch != null)
                        {
                            prefetch.transportLabel = i;
                            prefetches.put(key, prefetch);
                        }
                    }

                    // Get DII info, if available
                    if (diis.get(key) == null)
                    {
                        AppEntry.DiiLocation dii = parseDii(props, pfx);
                        if (dii != null)
                        {
                            dii.transportLabel = i;
                            diis.put(key, dii);
                        }
                    }

                    tp = oc;
                }
                else if ("ic".equals(val))
                {
                    AppEntry.IcTransportProtocol ic = new AppEntry.IcTransportProtocol();
                    ic.protocol = 0x101;

                    // Read URL
                    ic.urls.addElement(props.getProperty(pfx + ".url"));

                    tp = ic;
                }
                else if ("local".equals(val))
                {
                    tp = new AppEntry.LocalTransportProtocol();
                }
            }
            catch (Exception e)
            {
                // Ignore tp given any errors
                if (log.isInfoEnabled())
                {
                    log.info("Invalid transport." + i + " entry", e);
                }

                continue;
            }

            if (tp == null) continue;

            if (!(tp instanceof AppEntry.LocalTransportProtocol)) tp.label = i;
            tp.remoteConnection = parseBoolean(props.getProperty(pfx + ".remote"), false);
            if (tp.remoteConnection) tp.serviceId = parseInt(props.getProperty(pfx + ".service"));

            transports.put(new Integer(tp.label), tp);
        }

        // Add implicit LocalTransportProtocol
        if (transports.size() == 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Installing default LocalTransport");
            }

            transports.put(new Integer(0xFF), new AppEntry.LocalTransportProtocol());
        }
    }

    /**
     * Locates and parses registered api descriptor entries for the given
     * application entry.
     */
    private String[] parseRegisteredApiDesc(int i, Properties props)
    {
        Vector v = new Vector();
        for (int j = 0; j < 16; ++j)
        {
            String value = getValue(props, i, "api." + j);
            if (value != null) v.addElement(value);
        }
        String[] array = new String[v.size()];
        v.copyInto(array);

        return array;
    }

    /**
     * Parses the "prefetch" entry.
     * 
     * @param pfx
     *            the prefix of the prefetch entry
     * @return null or a new prefetch entry
     */
    private AppEntry.Prefetch parsePrefetch(Properties props, String pfx)
    {
        AppEntry.Prefetch prefetch = null;

        String prefetchValue = props.getProperty(pfx + ".prefetch");
        if (prefetchValue != null)
        {
            prefetch = new AppEntry.Prefetch();

            StringTokenizer toks = new StringTokenizer(prefetchValue, ",");
            prefetch.info = new AppEntry.Prefetch.Pair[toks.countTokens()];
            for (int idx = 0; toks.hasMoreTokens(); ++idx)
            {
                prefetch.info[idx] = new AppEntry.Prefetch.Pair();
                prefetch.info[idx].label = toks.nextToken();
                if (idx >= 100)
                    prefetch.info[idx].priority = 0;
                else
                    prefetch.info[idx].priority = 100 - idx;
            }

            // Sort prefetch entries based upon priority
            java.util.Arrays.sort(prefetch.info, new java.util.Comparator()
            {
                public int compare(Object o1, Object o2)
                {
                    // Reversed to give descending order
                    return ((AppEntry.Prefetch.Pair) o2).priority - ((AppEntry.Prefetch.Pair) o1).priority;
                }

                public boolean equals(Object o1, Object o2)
                {
                    return ((AppEntry.Prefetch.Pair) o2).priority == ((AppEntry.Prefetch.Pair) o1).priority;
                }
            });
        }

        return prefetch;
    }

    /**
     * Parses the "dii location" entry.
     * 
     * @param pfx
     *            the prefix of the dii location entry
     * @return null or a new dii location entry
     */
    private AppEntry.DiiLocation parseDii(Properties props, String pfx)
    {
        final int maxdii = parseInt(props.getProperty("maxdii"), 32);
        Vector id = new Vector();
        Vector assoc = new Vector();
        for (int j = 0; j < maxdii; ++j)
        {
            String diiValue = props.getProperty(pfx + ".dii." + j);
            if (diiValue != null)
            {
                StringTokenizer toks = new StringTokenizer(diiValue, ",");
                if (toks.countTokens() == 2)
                {
                    id.addElement(toks.nextToken());
                    assoc.addElement(toks.nextToken());
                }
            }
        }

        AppEntry.DiiLocation dii = null;
        if (id.size() > 0)
        {
            dii = new AppEntry.DiiLocation();
            dii.diiIdentification = new int[id.size()];
            for (int j = 0; j < id.size(); ++j)
                dii.diiIdentification[j] = parseInt((String) id.elementAt(j));
            dii.associationTag = new int[assoc.size()];
            for (int j = 0; j < id.size(); ++j)
                dii.associationTag[j] = parseInt((String) assoc.elementAt(j));
        }
        return dii;
    }

    /**
     * Parses the "addressing.i" entries. Used to represent the addressing
     * descriptors from X/AIT
     * 
     * @param props
     *            the properties set
     */
    protected void parseAddressing(Properties props)
    {
        final int maxaddr = parseInt(props.getProperty("maxaddr"), 32);

        for (int i = 0; i < maxaddr; ++i)
        {
            String propVal;
            String pfx = "addressing." + i;

            AddressingDescriptor ad = ait.new AddressingDescriptor();

            // Addressing group
            if ((propVal = props.getProperty(pfx + ".group")) == null) break;
            int groupID = parseInt(propVal);

            // Addressing label
            if ((propVal = props.getProperty(pfx + ".label")) == null) break;
            ad.addressLabel = parseInt(propVal);

            // Addressing priority
            if ((propVal = props.getProperty(pfx + ".priority")) == null) break;
            ad.priority = parseInt(propVal);

            // Addressing expressions
            for (int j = 0; j < 32; j++)
            {
                propVal = props.getProperty(pfx + ".expression." + j);
                if (propVal == null) break;

                propVal = propVal.trim();

                // Check for logical operators
                if (propVal.equals("NOT"))
                    ad.expressions.add(ait.new LogicalOp(LogicalOp.NOT));
                else if (propVal.equals("AND"))
                    ad.expressions.add(ait.new LogicalOp(LogicalOp.AND));
                else if (propVal.equals("OR"))
                    ad.expressions.add(ait.new LogicalOp(LogicalOp.OR));
                else if (propVal.equals("TRUE"))
                    ad.expressions.add(ait.new LogicalOp(LogicalOp.TRUE));
                else
                // We have a comparison
                {
                    // Check for security attribute
                    boolean security = false;
                    if (propVal.startsWith("S"))
                    {
                        security = true;
                        propVal = propVal.substring(1); // Remove "S"
                    }

                    // Search for comparison operator
                    int opCode = -1;
                    int operatorLength;
                    int opIndex;
                    if ((opIndex = propVal.indexOf("==")) != -1)
                    {
                        opCode = Comparison.EQ;
                        operatorLength = 2;
                    }
                    else if ((opIndex = propVal.indexOf(">=")) != -1)
                    {
                        opCode = Comparison.GTE;
                        operatorLength = 2;
                    }
                    else if ((opIndex = propVal.indexOf("<=")) != -1)
                    {
                        opCode = Comparison.LTE;
                        operatorLength = 2;
                    }
                    else if ((opIndex = propVal.indexOf(">")) != -1)
                    {
                        opCode = Comparison.GT;
                        operatorLength = 1;
                    }
                    else if ((opIndex = propVal.indexOf("<")) != -1)
                    {
                        opCode = Comparison.LT;
                        operatorLength = 1;
                    }
                    else
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("Invalid expression (" + j + "). Invalid operator");
                        }
                        continue;
                    }

                    // Attribute ID
                    int attributeID = parseInt(propVal.substring(0, opIndex).trim());

                    // Attribute value
                    String attributeValue = propVal.substring(opIndex + operatorLength).trim();

                    ad.expressions.add(ait.new Comparison(opCode, security, attributeID, attributeValue));
                }
            }

            // Validate that we have at least one expression
            if (ad.expressions.isEmpty()) break;

            AitImpl.addAddressingDescriptor(addrGroups, groupID, ad);
        }
    }

    /**
     * Parses the "attributes.i" entries. Used to represent the attribute
     * mapping descriptors from X/AIT
     * 
     * @param props
     *            the properties set
     */
    protected void parseAttributes(Properties props)
    {
        final int maxattr = parseInt(props.getProperty("maxattr"), 32);

        for (int i = 0; i < maxattr; ++i)
        {
            String propVal;
            String pfx = "attribute." + i;

            if ((propVal = props.getProperty(pfx + ".id")) == null) break;
            int attributeID = parseInt(propVal);

            if ((propVal = props.getProperty(pfx + ".name")) == null) break;

            attributeMap.put(new Integer(attributeID), propVal);
        }
    }

    /**
     * Parses the set of <code>Properties</code> for application data.
     * 
     * @throws IOException
     *             if data is of an unacceptable format
     */
    protected void parse(Properties props) throws IOException
    {
        // Transport Protocols
        parseTransportProtocols(props);

        // Apps
        final int maxapps = parseInt(props.getProperty("maxapps"), 32);
        for (int i = 0; i < maxapps; ++i)
        {
            String val;
            if ((val = getValue(props, i, "application_identifier")) != null)
            {
                try
                {
                    AppInfo app = parseEntry(i, props);

                    if (app != null)
                    {
                        allApplications.add(app.ae);
                    }
                }
                catch (Exception e)
                {
                    SystemEventUtil.logRecoverableError(e);
                }
            }
        }

        // Addressing Descriptors
        parseAddressing(props);

        // Addressing attributes
        parseAttributes(props);

        // External Authorization
        parseExternalAuthorization(props);
    }

    /**
     * Utility method used to parse booleans.
     */
    protected static boolean parseBoolean(String str, boolean defaultValue)
    {
        if (str == null) return defaultValue;
        return "true".equals(str.toLowerCase());
    }

    /**
     * Utility method used to parse decimal and hexadecimal integers.
     */
    protected static int parseInt(String str, int defaultValue) throws NumberFormatException
    {
        if (str == null) return defaultValue;
        return parseInt(str);
    }

    /**
     * Utility method used to parse decimal and hexadecimal integers.
     */
    protected static int parseInt(String str) throws NumberFormatException
    {
        str = str.trim();
        if (str.startsWith("0x")) return Integer.parseInt(str.substring(2), 16);

        return Integer.parseInt(str);
    }

    /**
     * Utility method used to parse hexadecimal longs.
     */
    protected static long parseLong(String str, long defaultValue) throws NumberFormatException
    {
        if (str == null) return defaultValue;
        return parseLong(str);
    }

    /**
     * Utility method used to parse hexadecimal longs.
     */
    protected static long parseLong(String str) throws NumberFormatException
    {
        str = str.trim();
        if (str.startsWith("0x"))
            return Long.parseLong(str.substring(2), 16);
        return Long.parseLong(str);
    }

    /**
     * Utility method used to parse 48-bit application identifiers.
     */
    protected static AppID appid(String str)
    {
        str = str.trim();
        long val = parseLong(str);
        return new AppID((int) ((val >> 16) & 0xFFFFFFFF), (int) (val & 0xFFFF));
    }

    /**
     * Parses the app control code.
     */
    protected int acc(String str)
    {
        if (str != null) str = str.trim();
        if (str == null || "PRESENT".equals(str))
            return OcapAppAttributes.PRESENT;
        else if ("AUTOSTART".equals(str))
            return OcapAppAttributes.AUTOSTART;
        else if ("DESTROY".equals(str))
            return OcapAppAttributes.DESTROY;
        else if ("KILL".equals(str))
            return OcapAppAttributes.KILL;
        else if ("REMOTE".equals(str)) return OcapAppAttributes.REMOTE;

        throw new IllegalArgumentException("Bad application_control_code: " + str);
    }

    /**
     * Parses the visibility flag.
     */
    protected static int viz(String str)
    {
        if (str != null) str = str.trim();
        if (str == null || "VISIBLE".equals(str))
            return AppEntry.VISIBLE;
        else if ("INVISIBLE".equals(str))
            return AppEntry.NON_VISIBLE;
        else if ("VISIBLE-TO-APPS-ONLY".equals(str)) return AppEntry.LISTING_ONLY;

        throw new IllegalArgumentException("Bad visibility: " + str);
    }

    /**
     * Parses the app mode flag
     */
    protected static int appMode(String str)
    {
        if (str != null) str = str.trim();
        if (str == null || "LEGACY".equals(str))
            return OcapAppAttributes.LEGACY_MODE;
        else if ("NORMAL".equals(str))
            return OcapAppAttributes.NORMAL_MODE;
        else if ("CROSSENVIRONMENT".equals(str))
            return OcapAppAttributes.CROSSENVIRONMENT_MODE;
        else if ("BACKGROUND".equals(str))
            return OcapAppAttributes.BACKGROUND_MODE;
        else if ("PAUSED".equals(str)) return OcapAppAttributes.PAUSED_MODE;

        throw new IllegalArgumentException("Bad application mode: " + str);
    }
    
    public class AppInfo
    {
        public AppInfo()
        {
            ae = new AppEntry();
        }
        
        protected AppEntry ae;
    }

    protected AitImpl ait;
    
    // Data required to construct an AIT
    protected int version;
    protected Vector externalAuth = new Vector();
    protected Vector allApplications = new Vector();
    protected Hashtable attributeMap = new Hashtable();
    protected Hashtable addrGroups = new Hashtable();
    
    private static final Logger log = Logger.getLogger(AitProps.class.getName());

    private Hashtable transports = new Hashtable();
    private Hashtable prefetches = new Hashtable();
    private Hashtable diis = new Hashtable();
}
