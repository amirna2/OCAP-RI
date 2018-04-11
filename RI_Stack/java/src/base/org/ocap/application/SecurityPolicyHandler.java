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

package org.ocap.application;

import java.security.PermissionCollection;

/**
 * <P>
 * This interface provides a callback handler to modify the Permissions granted
 * to an application to be launched. An application that has a
 * MonitorAppPermission("security") can have a concrete class that implements
 * this interface and set an instance of it to the AppManagerProxy.
 *</P>
 * <P>
 * The {@link SecurityPolicyHandler#getAppPermissions} method shall be called
 * before the OCAP implementation launches any type of application (e.g. before
 * class loading of any OCAP-J application). The application shall then be
 * loaded and started with the set of Permissions that are returned as the
 * return value of this method.
 * </P>
 * 
 * @see AppManagerProxy#setSecurityPolicyHandler
 */
public interface SecurityPolicyHandler
{
    /**
     * <P>
     * This callback method is used to modify the set of Permissions that is
     * granted to an application to be launched.
     * </P>
     * <P>
     * The OCAP implementation shall call this method before class loading of
     * any application, if an instance of a class that implements the
     * {@link SecurityPolicyHandler} interface is set to the
     * {@link AppManagerProxy}. The permissionInfo parameter of this method
     * contains the AppID of the application to be launched and a requested set
     * of Permissions that consists of Permissions requested in a permission
     * request file and Permissions requested for the unsigned application. This
     * method can modify the requested set of Permissions and returns them as
     * the return value. The OCAP implementation shall grant them to the
     * application.
     *</P>
     * <P>
     * The modified set of Permissions shall be a subset of the requested set of
     * Permissions specified by the permissionInfo parameter, and shall be a
     * superset of the set of the Permissions granted to unsigned applications
     * (as returned by PermissionInformation.getUnsignedAppPermissions()).
     *</P>
     * 
     * @param permissionInfo
     *            The PermissionInformation that specifies the application to be
     *            launched and its requested set of Permissions that are
     *            requested in a permission request file and requested for the
     *            unsigned application.
     * 
     * @return An instance of a subclass of the
     *         java.security.PermissionCollection that contains a modified set
     *         of Permissions to be granted to the application specified by the
     *         permissionInfo parameter. The modified set of Permissions (i.e.,
     *         return value) shall be granted to the application. If the
     *         modified set of Permissions is not a subset of the requested
     *         Permissions, or is not a superset of the set of the Permissions
     *         granted to unsigned applications (as returned by
     *         PermissionInformation.getUnsignedAppPermissions()), the OCAP
     *         implementation shall ignore the returned PermissionCollection and
     *         shall grant the requested set of Permissions to the application.
     */
    public PermissionCollection getAppPermissions(PermissionInformation permissionInfo);
}
