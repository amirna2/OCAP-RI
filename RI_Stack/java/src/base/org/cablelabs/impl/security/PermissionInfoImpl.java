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

import java.security.PermissionCollection;
import java.security.cert.Certificate;

import org.dvb.application.AppID;
import org.ocap.application.PermissionInformation;

/**
 * An implementation of <code>PermissionInformation</code>.
 * 
 * @author Aaron Kamienski
 */
class PermissionInfoImpl extends PermissionInformation
{
    /**
     * Constructs an instance of <code>PermissionInfoImpl</code>.
     * 
     * @param id
     *            <code>AppID</code> to be returned by {@link #getAppID}
     * @param hostApp
     *            <code>boolean</code> to be returned by
     *            {@link #isManufacturerApp}
     * @param certChains
     *            <code>Certificate[][]</code> to be returned by
     *            {@link #getCertificates}
     * @param requestedPerms
     *            <code>PermissionCollection</code> to be returned by
     *            {@link #getRequestedPermissions}
     */
    public PermissionInfoImpl(AppID id, boolean hostApp, Certificate[][] certChains, PermissionCollection requestedPerms)
    {
        this.id = id;
        this.hostApp = hostApp;
        this.certChains = certChains;
        this.requestedPerms = requestedPerms;
    }

    // JavaDoc inherited from PermissionInformation
    public AppID getAppID()
    {
        return id;
    }

    // JavaDoc inherited from PermissionInformation
    public boolean isManufacturerApp()
    {
        return hostApp;
    }

    // JavaDoc inherited from PermissionInformation
    public Certificate[][] getCertificates()
    {
        return certChains;
    }

    // JavaDoc inherited from PermissionInformation
    public boolean isPrivilegedCertificate(Certificate cert)
    {
        return false;
    }

    // JavaDoc inherited from PermissionInformation
    public PermissionCollection getRequestedPermissions()
    {
        return requestedPerms;
    }

    /**
     * Implement {@link Object#toString} to return a string representation of
     * this object.
     * 
     * @return a string representation of this object
     */
    public String toString()
    {
        return super.toString() + "[" + id + "," + hostApp + "," + requestedPerms + toString(certChains) + "]";
    }

    /**
     * Returns a string representation of the given array of certificate chains.
     * 
     * @param certChains
     *            array of certificate chains
     * @return a string representation of the given array of certificate chains
     */
    private String toString(Certificate[][] certChains)
    {
        if (certChains == null) return "null";

        StringBuffer sb = new StringBuffer("[");
        for (int i = 0; i < certChains.length; ++i)
        {
            if (certChains[i] != null && certChains[i].length > 0) sb.append(certChains[i][0]).append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * @see #getAppID()
     */
    private final AppID id;

    /**
     * @see #isManufacturerApp()
     */
    private final boolean hostApp;

    /**
     * @see #getCertificates()
     */
    private final Certificate[][] certChains;

    /**
     * @see #getRequestedPermissions()
     */
    private final PermissionCollection requestedPerms;

}
