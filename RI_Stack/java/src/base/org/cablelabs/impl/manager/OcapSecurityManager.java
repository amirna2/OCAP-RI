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

package org.cablelabs.impl.manager;

import java.security.PermissionCollection;
import java.security.ProtectionDomain;

import org.dvb.application.AppID;
import org.dvb.io.persistent.FileAccessPermissions;
import org.ocap.application.AppManagerProxy;
import org.ocap.application.PermissionInformation;
import org.ocap.application.SecurityPolicyHandler;

/**
 * The instance of this manager is chiefly responsible for installing the
 * Security subsystem for OCAP. At this time this interface does not define any
 * additional methods for accessing the security policy and implementation. Its
 * only responsibility is to install the <code>SecurityManager</code> and
 * <code>Policy</code>.
 * 
 * @see java.lang.SecurityManager
 * @see java.security.Policy
 * 
 * @author Aaron Kamienski
 */
public interface OcapSecurityManager extends Manager
{
    /**
     * Instruct the <code>OcapSecurityManager</code> to take care of setting up
     * the <code>SecurityManager</code> and <code>Policy</code> implementations.
     */
    public void securitySetup();

    /**
     * Used to implement {@link AppManagerProxy#setSecurityPolicyHandler}.
     * <p>
     * The security checks for the necessary privileges should be performed
     * before invoking this method.
     * 
     * @param handler
     *            the handler to install
     */
    public void setSecurityPolicyHandler(SecurityPolicyHandler handler);

    /**
     * Used to implement {@link PermissionInformation#getUnsignedPermissions}.
     * 
     * @return A read-only instance of a sub class of PermissionCollection
     *         containing the set of Permissions for an unsigned application.
     */
    public PermissionCollection getUnsignedPermissions();

    /**
     * Used to get the <code>ProtectionDomain</code> of the caller.
     * 
     * @return the <code>ProtectionDomain</code> of the caller
     */
    public ProtectionDomain getProtectionDomain();

    public static final int FILE_PERMS_WORLD = 1;

    public static final int FILE_PERMS_ORG = 2;

    public static final int FILE_PERMS_OWNER = 3;

    public static final int FILE_PERMS_OTHER_ORG = 4;

    public static final int FILE_PERMS_OCAP_LSV = 5;

    public static final int FILE_PERMS_ANY = 6;

    public static final int FILE_PERMS_RECORDING = FILE_PERMS_OCAP_LSV;

    public static final int FILE_PERMS_PERSISTENT = FILE_PERMS_ANY;

    /**
     * Returns true if the calling application has read File Access Permissions.
     * 
     * @param owner
     *            the <code>AppID</code> of the resource owner
     * @param perms
     *            the <code>FileAccessPermissions</code> to check
     * @param caller
     *            the <code>AppID</code> of the calling application
     * @param category
     *            one of the following
     *            <ul>
     *            <li> <code>FILE_PERMS_WORLD</code>
     *            <li> <code>FILE_PERMS_ORG</code>
     *            <li> <code>FILE_PERMS_OWNER</code>
     *            <li> <code>FILE_PERMS_OTHER_ORG</code>
     *            <li> <code>FILE_PERMS_OCAP_LSV</code>
     *            <li> <code>FILE_PERMS_ANY</code>
     *            </ul>
     *            These flags should not be OR'd together. The
     *            <code>FILE_PERMS_OCAP_LSV</code> category is used when
     *            enforcing the Logical Storage Volume rules defined by OCAP1
     *            I16 Section 14.2.2.8. The <code>FILE_PERMS_ANY</code> category
     *            can be used to check for any kind of access.
     * 
     *            Two additional category codes are provided specifically for
     *            checking access to persistent-storage files and recording
     *            files:
     * 
     *            <ul>
     *            <li> <code>FILE_PERMS_PERSISTENT</code>
     *            <li> <code>FILE_PERMS_RECORDING</code>
     *            </ul>
     * 
     * @return <code>true</code> if the caller has read file access permission
     *         <code>false</code> otherwise
     * 
     * @see org.dvb.io.persistent.FileAccessPermissions#hasReadApplicationAccessRight()
     * @see org.dvb.io.persistent.FileAccessPermissions#hasReadOrganisationAccessRight()
     * @see org.dvb.io.persistent.FileAccessPermissions#hasReadWorldAccessRight()
     * @see org.ocap.storage.ExtendedFileAccessPermissions#getReadAccessOrganizationIds()
     */
    public boolean hasReadAccess(AppID owner, FileAccessPermissions perms, AppID caller, int category);

    /**
     * Returns true if the calling application has write File Access
     * Permissions.
     * 
     * @param owner
     *            the <code>AppID</code> of the resource owner
     * @param perms
     *            the <code>FileAccessPermissions</code> to check
     * @param caller
     *            the <code>AppID</code> of the calling application
     * @param category
     *            one of the following
     *            <ul>
     *            <li> <code>FILE_PERMS_WORLD</code>
     *            <li> <code>FILE_PERMS_ORG</code>
     *            <li> <code>FILE_PERMS_OWNER</code>
     *            <li> <code>FILE_PERMS_OTHER_ORG</code>
     *            <li> <code>FILE_PERMS_OCAP_LSV</code>
     *            <li> <code>FILE_PERMS_ANY</code>
     *            </ul>
     *            These flags should not be OR'd together. The
     *            <code>FILE_PERMS_OCAP_LSV</code> category is used when
     *            enforcing the Logical Storage Volume rules defined by OCAP1
     *            I16 Section 14.2.2.8. The <code>FILE_PERMS_ANY</code> category
     *            can be used to check for any kind of access.
     * 
     *            Two additional category codes are provided specifically for
     *            checking access to persistent-storage files and recording
     *            files:
     * 
     *            <ul>
     *            <li> <code>FILE_PERMS_PERSISTENT</code>
     *            <li> <code>FILE_PERMS_RECORDING</code>
     *            </ul>
     * 
     * @return <code>true</code> if the caller has write file access permission
     *         <code>false</code> otherwise
     * 
     * @see org.dvb.io.persistent.FileAccessPermissions#hasWriteApplicationAccessRight()
     * @see org.dvb.io.persistent.FileAccessPermissions#hasWriteOrganisationAccessRight()
     * @see org.dvb.io.persistent.FileAccessPermissions#hasWriteWorldAccessRight()
     * @see org.ocap.storage.ExtendedFileAccessPermissions#getWriteAccessOrganizationIds()
     */
    public boolean hasWriteAccess(AppID owner, FileAccessPermissions perms, AppID caller, int category);
}
