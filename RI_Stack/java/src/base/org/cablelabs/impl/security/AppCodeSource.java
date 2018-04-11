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

import java.net.URL;
import java.security.CodeSource;
import java.security.cert.Certificate;

// TODO: AppCodeSource should support serialization (how to get AppEntry info?)
//       Probably should save AppID and restore AppID.
//       Or should we simply prevent this from happening outright?

/**
 * An implementation of <code>CodeSource</code> that contains additional
 * information necessary for resolving <code>Permission</code>s for a file. In
 * order to resolve permissions for an application the <code>Policy</code>
 * needs:
 * <ul>
 * <li> <code>AppID</code> to determine if application is <i>signed</i> or
 * <i>unsigned</i>
 * <li>the initial class name (which can be used, along with the base directory
 * name, to construct the path to the permission request file)
 * <li>the locations where application classes may come from (i.e., the
 * classpath)
 * </ul>
 * 
 * @author Aaron Kamienski
 */
public class AppCodeSource extends CodeSource
{
    /**
     * Construct an instance of <code>AppCodeSource</code>. The given
     * <code>AppEntry</code> describes this application. It includes the
     * following applicable information:
     * <ul>
     * <li> <code>AppID</code>
     * <li>name of initial class
     * <li>class path (relative to base directory)
     * </ul>
     * The base directory specified by the given <code>URL</code> is transport
     * protocol dependent.
     * 
     * @param entry
     *            the <code>AppEntry</code> describing this application
     * @param url
     *            the base path for the loaded code (e.g., the
     *            <i>base_directory</i>)
     * @param authType
     *            one of {@link AuthInfo#AUTH_UNSIGNED},
     *            {@link AuthInfo#AUTH_SIGNED_DVB},
     *            {@link AuthInfo#AUTH_SIGNED_OCAP}, or
     *            {@link AuthInfo#AUTH_SIGNED_DUAL}
     * @param certChains
     *            the certificates used to sign the classes
     */
    public AppCodeSource(AppEntry entry, URL url, int authType, Certificate[][] certChains)
    {
        super(url, getLeafCertificates(certChains));

        this.entry = entry;
        this.authType = authType;
        this.certChains = certChains;
    }

    /**
     * Given an array of <code>Certificate[]</code> chains, returns a
     * <code>Certificate[]</code> that contains only the leaf
     * <code>Certificate</code>s. This creates a <code>Certificate[]</code> of
     * the same length as <i>certChains</i>. Then it initializes each entry
     * using <i>certChains[i][0]</i>.
     * 
     * @param certChains
     * @return array of leaf certificates; or <code>null</code> if
     *         <i>certChains</i> is <code>null</code>
     */
    private static Certificate[] getLeafCertificates(Certificate[][] certChains)
    {
        if (certChains == null) return null;

        Certificate[] certs = new Certificate[certChains.length];
        for (int i = 0; i < certChains.length; ++i)
            certs[i] = certChains[i][0];
        return certs;
    }

    /**
     * Returns the <code>AppEntry</code> for the corresponding application.
     * 
     * @return the <code>AppEntry</code> for the corresponding application
     */
    public AppEntry getAppEntry()
    {
        return entry;
    }

    /**
     * Returns the authentication status for the corresponding application.
     * 
     * @return one of {@link AuthInfo#AUTH_UNSIGNED},
     *         {@link AuthInfo#AUTH_SIGNED_DVB},
     *         {@link AuthInfo#AUTH_SIGNED_OCAP}, or
     *         {@link AuthInfo#AUTH_SIGNED_DUAL}.
     */
    public int getAuthenticationStatus()
    {
        return authType;
    }

    /**
     * Returns the <code>Certificate[]</code> chains given upon construction.
     * <p>
     * Note that the caller should <b>not</b> modify the contents of the
     * returned arrays
     * 
     * @return the <code>Certificate[]</code> chains given upon construction
     */
    public Certificate[][] getCertificateChains()
    {
        return certChains;
    }

    /**
     * Extends {@link CodeSource#equals} to additionally compare the
     * <code>AppEntry</code> information.
     * <p>
     * This places the additional requirement that <i>obj</i> is an instance of
     * <code>AppCodeSouce</code> and the contained <code>AppID</code> is the
     * same.
     * 
     * @return <code>true</code> if the code sources are considered equivalent
     */
    public boolean equals(Object obj)
    {
        if (!super.equals(obj)) return false;

        // objects types must be equal
        if (!(obj instanceof AppCodeSource)) return false;
        AppCodeSource cs = (AppCodeSource) obj;
        return this.entry == cs.entry || this.entry.id.equals(cs.entry.id);
    }

    /*
     * Extends {@link CodeSource#implies} to additionally consider the
     * <code>AppEntry</code>. <p> This places the additional requirement that
     * <i>codesource</i> is an instance of <code>AppCodeSource</code> and the
     * contained <code>AppID</code> is the same. This requirement could be a
     * little less restrictive, and simply test that the AID range is inclusive.
     * 
     * @return <code>true</code> if this code source implies the given on
     */
    public boolean implies(CodeSource codesource)
    {
        if (!super.implies(codesource)) return false;

        if (!(codesource instanceof AppCodeSource)) return false;

        AppCodeSource acs = (AppCodeSource) codesource;
        return entry == acs.entry || entry.id.equals(acs.entry.id);
    }

    /**
     * Overrides {@link CodeSource#toString} to additionally show the
     * <code>AppID</code>.
     * 
     * @return a <code>String</code> representation of this object
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append("(");
        sb.append(entry.id);
        sb.append(" ");
        sb.append(getLocation());
        Certificate[] certs = getCertificates();
        if (certs == null || certs.length <= 0)
            sb.append(" <no certificates>");
        else
        {
            for (int i = 0; i < certs.length; i++)
            {
                sb.append(" " + certs[i]);
            }
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * The <code>AppEntry</code> for the associated application. It includes the
     * following applicable information:
     * <ul>
     * <li> <code>AppID</code>
     * <li>name of initial class
     * <li>class path (relative to base directory)
     * </ul>
     */
    private final transient AppEntry entry;

    /**
     * The authentication type. One of {@link AuthInfo#AUTH_UNSIGNED},
     * {@link AuthInfo#AUTH_SIGNED_DVB}, {@link AuthInfo#AUTH_SIGNED_OCAP}, or
     * {@link AuthInfo#AUTH_SIGNED_DUAL}.
     */
    private final int authType;

    /**
     * The entire <code>Certificate</code> chains used to sign classes.
     */
    private final Certificate[][] certChains;
}
