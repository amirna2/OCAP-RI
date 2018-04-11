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

import java.io.IOException;
import java.security.PermissionCollection;

import org.cablelabs.impl.security.AppPermissions;
import org.ocap.shared.dvr.RecordingPermission;
import org.ocap.system.MonitorAppPermission;

/**
 * Implements <code>PermissionExtension</code> to add support for DVR-specific
 * <code>MonitorAppPermission</code>s.
 * 
 * @author Aaron Kamienski
 */
public class DvrPermissions implements PermissionExtension
{
    /**
     * Adds instances of <code>RecordingPermission</code> to the given
     * collection. If <i>name</i> is <code>"recording"</code> then the following
     * permissions are added:
     * <ul>
     * <li> <code>RecordingPermission("create", "own")</code>
     * <li> <code>RecordingPermission("read", "own")</code>
     * <li> <code>RecordingPermission("modify", "own")</code>
     * <li> <code>RecordingPermission("delete", "own")</code>
     * <li> <code>RecordingPermission("cancel", "own")</code>
     * </ul>
     * If <i>name</i> is <code>"handler.recording"</code> then the following
     * permissions are added:
     * <ul>
     * <li> <code>RecordingPermission("*", "*")</code>
     * </ul>
     * 
     * @param name
     *            <code>"recording"</code> or <code>"handler.recording"</code>
     * @param permissions
     *            collection to add new permissions to
     */
    public void handleMonAppPermission(String name, PermissionCollection permissions)
    {
        if ("recording".equals(name))
        {
            // Additional permissions implied by
            // MonitorAppPermission("recording")
            // See DVR I02 7.2.1.3.2
            permissions.add(new MonitorAppPermission(name));
            permissions.add(new RecordingPermission("create", "own"));
            permissions.add(new RecordingPermission("read", "own"));
            permissions.add(new RecordingPermission("modify", "own"));
            permissions.add(new RecordingPermission("delete", "own"));
            permissions.add(new RecordingPermission("cancel", "own"));
        }
        else if ("handler.recording".equals(name))
        {
            permissions.add(new MonitorAppPermission(name));
            // Additional permissions implied by
            // MonitorAppPermission("handler.recording")
            // See DVR I02 7.2.1.3.2
            permissions.add(new RecordingPermission("*", "*"));
        }
    }

    /**
     * Returns the set of permissions granted by this extension to all signed
     * applications
     * 
     * @return the set of permissions or an empty collection if no permissions
     *         are granted to signed applications
     */
    public AppPermissions getDefaultSignedPermissions()
    {
        return new AppPermissions();
    }

    /**
     * Returns the set of permissions granted by this extension to all unsigned
     * applications
     * 
     * @return the set of permissions or an empty collection if no permissions
     *         are granted to unsigned applications
     */
    public AppPermissions getDefaultUnsignedPermissions()
    {
        return new AppPermissions();
    }

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
    public AppPermissions parsePRF(byte[] prf) throws IOException
    {
        return new AppPermissions();
    }
}
