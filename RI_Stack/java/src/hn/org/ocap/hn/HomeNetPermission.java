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

package org.ocap.hn;

/**
 * The HomeNetPermission class represents permission to execute privileged home
 * networking operations only signed applications MAY be granted.
 * <p>
 * A HomeNetPermission consists of a permission name, representing a single
 * privileged operation. The name given in the constructor may end in "*" to
 * represent all permissions beginning with the given string, such as
 * <code>"*"</code> to allow all HomeNetPermission operations.
 * <p>
 * The following table lists all HomeNetPermission permission names.
 * <table border=1 cellpadding=5>
 * <tr>
 * <th>Permission Name</th>
 * <th>What the Permission Allows</th>
 * <th>Description</th>
 * </tr>
 * 
 * <tr>
 * <td>contentmanagement</td>
 * <td>Provides management of local or remote content</td>
 * <td>Applications with this permission can copy, move, delete content as well
 * as allocate and delete logical volumes on a local network device.</td>
 * </tr>
 * 
 * <tr>
 * <td>contentlisting</td>
 * <td>Provides listing of content on remote devices</td>
 * <td>Applications with this permission can discover and query lists of content
 * stored on or streamable from remote devices.</td>
 * </tr>
 * 
 * <tr>
 * <td>recording</td>
 * <td>Provides recording operations on remote devices</td>
 * <td>Applications with this permission can request that recordings be
 * scheduled, prioritized, and deleted on remote devices.</td>
 * </tr>
 * 
 * <tr>
 * <td>recordinghandler</td>
 * <td>Provides recording request handler functionality on the local device</td>
 * <td>Applications with this permission can manage network recording requests
 * for the local device.</td>
 * </tr>
 * 
 * </table>
 * 
 * Other permissions may be added as necessary.
 */
public final class HomeNetPermission extends java.security.BasicPermission
{

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions.See spec available
     * at http://docs.oracle.com/javase/1.4.2/docs/guide/serialization/ for
     * details
     */
    private static final long serialVersionUID = 6476271138960352122L;

    /**
     * Constructor for the HomeNetPermission
     * 
     * @param name
     *            The name of this permission (see table in class description).
     */
    public HomeNetPermission(String name)
    {
        super(name);
    }
}
