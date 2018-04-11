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

package org.ocap.storage;

import org.dvb.io.persistent.FileAccessPermissions;

/**
 * This class extends {@link FileAccessPermissions} to let granting applications
 * provide read and write file access to applications that have an organization
 * identifier different from a granting application.
 */
public class ExtendedFileAccessPermissions extends FileAccessPermissions
{
    /**
     * This constructor encodes application, application organization, and world
     * file access permissions as a set of booleans, and other organizations
     * file access permissions as arrays of granted organization identifiers.
     * 
     * @param readWorldAccessRight
     *            read access for all applications
     * @param writeWorldAccessRight
     *            write access for all applications
     * @param readOrganisationAccessRight
     *            read access for applications with the same organization as the
     *            granting application.
     * @param writeOrganisationAccessRight
     *            write access for applications with the same organization as
     *            the granting application.
     * @param readApplicationAccessRight
     *            read access for the owner.
     * @param writeApplicationAccessRight
     *            write access for the owner.
     * @param otherOrganisationsReadAccessRights
     *            array of other organization identifiers with read access.
     *            Applications with an organization identifier matching one of
     *            these organization identifiers will be given read access.
     * @param otherOrganisationsWriteAccessRights
     *            array of other organisation identifiers with write access.
     *            Applications with an organization identifier matching one of
     *            these organization identifiers will be given write access.
     */
    public ExtendedFileAccessPermissions(boolean readWorldAccessRight, boolean writeWorldAccessRight,
            boolean readOrganisationAccessRight, boolean writeOrganisationAccessRight,
            boolean readApplicationAccessRight, boolean writeApplicationAccessRight,
            int[] otherOrganisationsReadAccessRights, int[] otherOrganisationsWriteAccessRights)
    {
        super(readWorldAccessRight, writeWorldAccessRight, readOrganisationAccessRight, writeOrganisationAccessRight,
                readApplicationAccessRight, writeApplicationAccessRight);

        readOids = copy(otherOrganisationsReadAccessRights);
        writeOids = copy(otherOrganisationsWriteAccessRights);
    }

    /**
     * This method allows modification of the permissions on this instance of
     * the ExtendedFileAccessPermission class.
     * 
     * @param readWorldAccessRight
     *            read access for all applications
     * @param writeWorldAccessRight
     *            write access for all applications
     * @param readOrganisationAccessRight
     *            read access for organisation
     * @param writeOrganisationAccessRight
     *            write access for organisation
     * @param readApplicationAccessRight
     *            read access for the owner
     * @param writeApplicationAccessRight
     *            write access for the owner
     * @param otherOrganisationsReadAccessRights
     *            array of other organisation identifiers with read access.
     *            Applications with an organization identifier matching one of
     *            these organization identifiers will be given read access.
     * @param otherOrganisationsWriteAccessRights
     *            array of other organisation identifiers with write access.
     *            Applications with an organization identifier matching one of
     *            these organization identifiers will be given write access.
     */
    public void setPermissions(boolean readWorldAccessRight, boolean writeWorldAccessRight,
            boolean readOrganisationAccessRight, boolean writeOrganisationAccessRight,
            boolean readApplicationAccessRight, boolean writeApplicationAccessRight,
            int[] otherOrganisationsReadAccessRights, int[] otherOrganisationsWriteAccessRights)
    {
        setPermissions(readWorldAccessRight, writeWorldAccessRight, readOrganisationAccessRight,
                writeOrganisationAccessRight, readApplicationAccessRight, writeApplicationAccessRight);

        readOids = copy(otherOrganisationsReadAccessRights);
        writeOids = copy(otherOrganisationsWriteAccessRights);
    }

    /**
     * Gets the array of organization identifiers with read permission.
     * 
     * @return Array of organization identifiers with read permission.
     */
    public int[] getReadAccessOrganizationIds()
    {
        return copy(readOids);
    }

    /**
     * Gets the array of organization identifiers with write permission.
     * 
     * @return Array of organization identifiers with write permission.
     */
    public int[] getWriteAccessOrganizationIds()
    {
        return copy(writeOids);
    }

    /**
     * Returns a copy of the given array.
     * 
     * @param array
     *            the array to copy
     * @return a duplicate of the given array, or <code>null</code> if
     *         <i>array</i> is <code>null</code>
     */
    private static int[] copy(int[] array)
    {
        if (array == null) return null;
        return (int[]) array.clone();
    }

    /**
     * Other organizations with read access.
     */
    private int[] readOids;

    /**
     * Other organizations with write access.
     */
    private int[] writeOids;
}
