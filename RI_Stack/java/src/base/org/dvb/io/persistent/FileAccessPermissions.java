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

package org.dvb.io.persistent;

/**
 * This class encapsulates file access permissions, world, Organisation and
 * owner. World means all applications authorised to access persistent storage.
 * Owner means the application which created the file. Organisation is defined
 * as applications with the same organisation id as defined elsewhere in
 * the present document.
 */

public class FileAccessPermissions
{

    private boolean worldRead;

    private boolean worldWrite;

    private boolean orgRead;

    private boolean orgWrite;

    private boolean appRead;

    private boolean appWrite;

    /**
     * This constructor encodes all the file access permissions as a set of
     * booleans.
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
     */
    public FileAccessPermissions(boolean readWorldAccessRight, boolean writeWorldAccessRight,
            boolean readOrganisationAccessRight, boolean writeOrganisationAccessRight,
            boolean readApplicationAccessRight, boolean writeApplicationAccessRight)
    {
        worldRead = readWorldAccessRight;
        worldWrite = writeWorldAccessRight;
        orgRead = readOrganisationAccessRight;
        orgWrite = writeOrganisationAccessRight;
        appRead = readApplicationAccessRight;
        appWrite = writeApplicationAccessRight;

        return;
    }

    /**
     * Query whether this permission includes read access for the world.
     *
     * @return true if all applications can have read access, otherwise false.
     */
    public boolean hasReadWorldAccessRight()
    {
        return (worldRead);
    }

    /**
     * Query whether this permission includes write access for the world.
     *
     * @return true if all applications can have write access, otherwise false.
     */
    public boolean hasWriteWorldAccessRight()
    {
        return (worldWrite);
    }

    /**
     * Query whether this permission includes read access for the organisation
     *
     * @return true if applications in this organisation can have read access,
     *         otherwise false.
     */
    public boolean hasReadOrganisationAccessRight()
    {
        return (orgRead);
    }

    /**
     * Query whether this permission includes write access for the organisation
     *
     * @return true if applications in this organisation can have read access,
     *         otherwise false.
     */
    public boolean hasWriteOrganisationAccessRight()
    {
        return (orgWrite);
    }

    /**
     * Query whether this permission includes read access for the owning
     * application
     *
     * @return true if the owning application can have read access, otherwise
     *         false.
     */
    public boolean hasReadApplicationAccessRight()
    {
        return (appRead);
    }

    /**
     * Query whether this permission includes write access for the owning
     * application
     *
     * @return true if the owning application can have write access, otherwise
     *         false.
     */
    public boolean hasWriteApplicationAccessRight()
    {
        return (appWrite);
    }

    /**
     * This method allows to modify the permissions on this instance of the
     * FileAccessPermission class.
     *
     * @param ReadWorldAccessRight
     *            read access for all applications
     * @param WriteWorldAccessRight
     *            write access for all applications
     * @param ReadOrganisationAccessRight
     *            read access for organisation
     * @param WriteOrganisationAccessRight
     *            write access for organisation
     * @param ReadApplicationAccessRight
     *            read access for the owner
     * @param WriteApplicationAccessRight
     *            write access for the owner
     */
    public void setPermissions(boolean ReadWorldAccessRight, boolean WriteWorldAccessRight,
            boolean ReadOrganisationAccessRight, boolean WriteOrganisationAccessRight,
            boolean ReadApplicationAccessRight, boolean WriteApplicationAccessRight)
    {
        worldRead = ReadWorldAccessRight;
        worldWrite = WriteWorldAccessRight;
        orgRead = ReadOrganisationAccessRight;
        orgWrite = WriteOrganisationAccessRight;
        appRead = ReadApplicationAccessRight;
        appWrite = WriteApplicationAccessRight;

        return;
    }
}
