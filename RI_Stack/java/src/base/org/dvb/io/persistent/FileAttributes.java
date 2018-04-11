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

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.cablelabs.impl.security.PersistentStorageAttributes;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.system.MonitorAppPermission;

/**
 * This class encapsulates the attributes of a file stored in persistent
 * storage. The default attributes for a file are low priority, owner read /
 * write only permissions and null expiration date.
 */

public class FileAttributes
{

    private int attribPriority;

    private Date attribExpiration;

    private FileAccessPermissions attribPerms;

    /**
     * Value for use as a file priority.
     */
    public static final int PRIORITY_LOW = 1;

    /**
     * Value for use as a file priority.
     */
    public static final int PRIORITY_MEDIUM = 2;

    /**
     * Value for use as a file priority.
     */
    public static final int PRIORITY_HIGH = 3;

    /**
     * Constructor.
     * 
     * @param expiration_date
     *            an expiration date or null
     * @param p
     *            the access permissions to use
     * @param priority
     *            the priority to use in persistent storage
     * 
     */
    FileAttributes(Date expiration_date, FileAccessPermissions p, int priority)
    {
        attribExpiration = expiration_date;
        attribPerms = p;
        attribPriority = priority;
    }

    /**
     * Returns the expiration date. It will return the value used by the
     * platform, which need not be the same as the value set.
     * 
     * @return the expiration date
     */
    public Date getExpirationDate()
    {
        return (attribExpiration);
    }

    /**
     * Sets the expiration date. This field is a hint to the platform to
     * identify the date after which a file is no longer useful as percieved by
     * the application. The platform may choose to use a different date than the
     * one given as a parameter.
     * 
     * @param d
     *            the expiration date
     */
    public void setExpirationDate(Date d)
    {
        attribExpiration = d;
    }

    /**
     * Returns the file access permissions
     * 
     * @return the file access permissions
     */
    public FileAccessPermissions getPermissions()
    {
        return (attribPerms);
    }

    /**
     * Sets the file access permissions.
     * 
     * @param p
     *            the file access permissions
     */
    public void setPermissions(FileAccessPermissions p)
    {
        attribPerms = p;
    }

    /**
     * Returns the priority to use in persistent storage
     * 
     * @return the priority
     */
    public int getPriority()
    {
        return (attribPriority);
    }

    /**
     * Sets the priority to use in persistent storage
     * 
     * @param priority
     *            the priority to set
     */
    public void setPriority(int priority)
    {
        attribPriority = priority;
    }

    /**
     * Get the attributes of a file.
     * 
     * @param f
     *            the file to use
     * @return a copy of the attributes of a file
     * @throws SecurityException
     *             if the application is denied access to the file or to
     *             directories needed to reach the file by security policy.
     * @throws IOException
     *             if access to the file fails due to an I/O error or if the
     *             file reference is not to a valid location in persistent
     *             storage.
     */
    public static FileAttributes getFileAttributes(File f) throws SecurityException, IOException
    {
        String cp = f.getCanonicalPath();

        PersistentStorageAttributes psa = PersistentStorageAttributes.getInstance();

        // Attempts to access attributes of a file outside of persistent storage
        // MUST
        // throw an IOException before a security exception, so we have to do
        // this check
        if (!psa.isLocatedInPersistentStorage(cp)) throw new IOException();

        // Verify "read" permission for target file.
        SecurityUtil.checkRead(cp);
        return new FileAttributes(psa.getFileExpirationDate(cp), psa.getFilePermissions(cp), psa.getFilePriority(cp));
    }

    /**
     * Associate a set of file attributes with a file.
     * 
     * @param p
     *            the file attributes to use
     * @param f
     *            the file to use
     * @throws SecurityException
     *             if the application is either denied access to the file or
     *             directories needed to reach the file by security policy or is
     *             not authorised to modify the attributes of the file.
     * @throws IOException
     *             if access to the file fails due to an I/O error or if the
     *             file reference is not to a valid location in persistent
     *             storage.
     */
    public static void setFileAttributes(FileAttributes p, File f) throws SecurityException, IOException
    {
        String cp = f.getCanonicalPath();

        PersistentStorageAttributes psa = PersistentStorageAttributes.getInstance();

        // Attempts to access attributes of a file outside of persistent storage
        // MUST
        // throw an IOException before a security exception, so we have to do
        // this check
        if (!psa.isLocatedInPersistentStorage(cp)) throw new IOException();

        // See Javadoc for MonitorAppPermission("storage")
        if (SecurityUtil.hasPermission(new MonitorAppPermission("storage")))
        {
            psa.setFileAttributes(cp, p.getPermissions(), p.getExpirationDate(), p.getPriority(), true);
        }
        else
        {
            // Verify "write" permission for target file.
            SecurityUtil.checkWrite(cp);
            psa.setFileAttributes(cp, p.getPermissions(), p.getExpirationDate(), p.getPriority(), false);
        }
    }

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}
