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

package org.cablelabs.impl.util;

import org.dvb.io.persistent.FileAccessPermissions;
import org.dvb.io.persistent.FileAttributes;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * Utility class containing some useful methods pertaining to
 * <code>FileAttributes</code>.
 * 
 * @author Wes Munsil
 */
public final class FileAttributesUtil
{
    /**
     * Private constructor, to prevent instantiation.
     */
    private FileAttributesUtil()
    {
    }

    /**
     * Returns a deep copy of a <code>FileAccessPermissions</code> object, or
     * null if no object exists.
     * 
     * @param fap
     *            a reference to the <code>FileAccessPermissions</code> object,
     *            or null.
     * 
     * @return a deep copy of the object, or null if no object exists.
     */
    public static FileAccessPermissions clone(FileAccessPermissions fap)
    {
        FileAccessPermissions result;

        if (fap == null)
        {
            result = null;
        }
        else if (fap instanceof ExtendedFileAccessPermissions)
        {
            ExtendedFileAccessPermissions efap = (ExtendedFileAccessPermissions) fap;

            result = new ExtendedFileAccessPermissions(efap.hasReadWorldAccessRight(), efap.hasWriteWorldAccessRight(),
                    efap.hasReadOrganisationAccessRight(), efap.hasWriteOrganisationAccessRight(),
                    efap.hasReadApplicationAccessRight(), efap.hasWriteApplicationAccessRight(),
                    clone(efap.getReadAccessOrganizationIds()), clone(efap.getWriteAccessOrganizationIds()));
        }
        else
        {
            result = new FileAccessPermissions(fap.hasReadWorldAccessRight(), fap.hasWriteWorldAccessRight(),
                    fap.hasReadOrganisationAccessRight(), fap.hasWriteOrganisationAccessRight(),
                    fap.hasReadApplicationAccessRight(), fap.hasWriteApplicationAccessRight());
        }

        return result;
    }

    /**
     * Returns a deep copy of an array of <code>int</code>s, or null if no array
     * exists.
     * 
     * @param ia
     *            a reference to the array of <code>int</code>s, or null.
     * 
     * @return a deep copy of the array, or null if no array exists.
     */
    private static int[] clone(int[] ia)
    {
        return ia == null ? null : (int[]) ia.clone();
    }

    /**
     * Returns a string representation of a priority.
     * 
     * @param p
     *            the priority.
     * 
     * @return a string representation of the priority.
     */
    public static String priorityToString(int p)
    {
        String result;

        switch (p)
        {
            case FileAttributes.PRIORITY_LOW:
                result = "PRIORITY_LOW";
                break;
            case FileAttributes.PRIORITY_MEDIUM:
                result = "PRIORITY_MEDIUM";
                break;
            case FileAttributes.PRIORITY_HIGH:
                result = "PRIORITY_HIGH";
                break;
            default:
                result = Integer.toString(p);
                break;
        }

        return result;
    }

    /**
     * Returns a string representation of a <code>FileAccessPermissions</code>
     * object.
     * 
     * @param t
     *            a reference to the <code>FileAccessPermissions</code> object,
     *            or null.
     * 
     * @return a string representation of the <code>FileAccessPermissions</code>
     *         object if any; else the string <code>"null"</code>.
     */
    public static String toString(FileAccessPermissions t)
    {
        if (t == null)
        {
            return "null";
        }

        StringBuffer sb = new StringBuffer();

        sb.append('[');

        sb.append(r(t.hasReadWorldAccessRight()));
        sb.append(w(t.hasWriteWorldAccessRight()));
        sb.append(r(t.hasReadOrganisationAccessRight()));
        sb.append(w(t.hasWriteOrganisationAccessRight()));
        sb.append(r(t.hasReadApplicationAccessRight()));
        sb.append(w(t.hasWriteApplicationAccessRight()));

        if (t instanceof ExtendedFileAccessPermissions)
        {
            ExtendedFileAccessPermissions et = (ExtendedFileAccessPermissions) t;

            sb.append(',');

            append(sb, 'r', et.getReadAccessOrganizationIds());
            append(sb, 'w', et.getWriteAccessOrganizationIds());
        }

        sb.append(']');

        return sb.toString();
    }

    /**
     * Returns a string representation of a <code>FileAttributes</code> object.
     * 
     * @param t
     *            a reference to the <code>FileAttributes</code> object, or
     *            null.
     * 
     * @return a string representation of the <code>FileAttributes</code> object
     *         if any; else the string <code>"null"</code>.
     */
    public static String toString(FileAttributes t)
    {
        if (t == null)
        {
            return "null";
        }

        StringBuffer sb = new StringBuffer();

        sb.append('[');

        sb.append("expiration date = " + t.getExpirationDate());
        sb.append(", ");

        sb.append("permissions = " + t.getPermissions());
        sb.append(", ");

        sb.append("priority = " + priorityToString(t.getPriority()));

        sb.append(']');

        return sb.toString();
    }

    /**
     * Appends a string representation of a character and an array of
     * <code>int</code>s to a <code>StringBuffer</code>.
     * 
     * @param sb
     *            the <code>StringBuffer</code>.
     * @param c
     *            the character.
     * @param ia
     *            the array of <code>int</code>s.
     */
    private static void append(StringBuffer sb, char c, int[] ia)
    {
        sb.append(c);
        sb.append('(');
        if (ia == null)
        {
            sb.append("null");
        }
        else
        {
            boolean first = true;
            for (int i = 0, n = ia.length; i < n; ++i)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(", ");
                }
                sb.append(Integer.toHexString(ia[i]));
            }
        }
        sb.append(')');
    }

    /**
     * Returns a character representation of a read permission.
     * 
     * @param b
     *            the read permission.
     * 
     * @return a character representation of the read permission.
     */
    private static char r(boolean b)
    {
        return b ? 'r' : '-';
    }

    /**
     * Returns a character representation of a write permission.
     * 
     * @param b
     *            the write permission.
     * 
     * @return a character representation of the write permission.
     */
    private static char w(boolean b)
    {
        return b ? 'w' : '-';
    }
}
