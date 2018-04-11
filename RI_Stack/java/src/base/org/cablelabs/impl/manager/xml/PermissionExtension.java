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

package org.cablelabs.impl.manager.xml;

import java.security.PermissionCollection;
import java.io.IOException;

import org.cablelabs.impl.security.AppPermissions;

/**
 * This class defines an interface for extending the permissions handled by the
 * <code>PermissionsRequest</code>. Each running extension will register a
 * <code>PermissionsExtension</code> with the <code>OcapSecurityManager</code>
 * during system startup
 * 
 * @author Aaron Kamienski
 */
public interface PermissionExtension
{
    /**
     * Given the permission name, adds any additional <code>Permission</code>s
     * to the given <code>PermissionCollection</code>.
     * 
     * @param name
     *            name of permission to handle
     * @param perms
     *            <code>PermissionCollection</code> to add additional
     *            permissions to
     */
    public void handleMonAppPermission(String name, PermissionCollection perms);

    /**
     * Returns the set of permissions granted by this extension to all unsigned
     * applications
     * 
     * @return the set of permissions or an empty collection if no permissions
     *         are granted to unsigned applications
     */
    AppPermissions getDefaultUnsignedPermissions();

    /**
     * Returns the set of permissions granted by this extension to all signed
     * applications
     * 
     * @return the set of permissions or an empty collection if no permissions
     *         are granted to signed applications
     */
    AppPermissions getDefaultSignedPermissions();

    /**
     * Parses any extension-specific permission from the given permission
     * request file.
     * 
     * @param prf
     *            the permission request file byte data
     * @return a collection of extension-specific permissions granted by the
     *         given PRF or an empty collection if no permissions are expected
     *         or found by this extension
     * @throws IOException
     *             if any error was encountered while parsing the PRF
     */
    AppPermissions parsePRF(byte[] prf) throws IOException;
}
