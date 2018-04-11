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

package org.cablelabs.impl.manager.filesys;

import org.cablelabs.impl.util.FileAttributesUtil;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.Date;

import org.ocap.storage.ExtendedFileAccessPermissions;
import org.dvb.io.persistent.FileAccessPermissions;

/**
 * Metadata associated with a file or directory.
 * <p>
 * A <code>FileMetadata</code> object knows how to save and restore itself to
 * and from disk. Currently this is done through hard-coded calls to
 * <code>DataInput</code> and <code>DataOutput</code> methods. An alternative is
 * to use serialization; serialization, however, consumes on the order of 4 to 5
 * times as much disk space.
 * <p>
 * Each file or directory created by an application in persistent storage has a
 * metadata file that is its sibling. Some alternatives to this approach are:
 * <ul>
 * <li>use a file fork instead of a sibling file;
 * <li>use a parallel directory structure instead of "polluting" the "real"
 * directory structure with sibling files; and
 * <li>use a single repository, a la the old Mac Desktop file.
 * </ul>
 */
public class FileMetadata
{
    /**
     * The metadata file format version. If the metadata file format changes,
     * increment this, decide how many old versions you want to continue
     * supporting for the time being, and change the
     * {@link #readExternal(DataInput) readExternal} method accordingly.
     */
    private static final String VERSION_00 = "FM00";

    /**
     * The disk representation of a null date.
     * <p>
     * If anyone were to actually specify an expiration date of 292269055/12/02
     * BC at 16:47:04 GMT, we'd incorrectly report it back as null.
     */
    private static final long NULL_DATE_REP = Long.MIN_VALUE;

    /**
     * The metadata.
     */
    private byte applicationPriority;

    private long expirationDate;

    private byte filePriority;

    private long owner;

    private FileAccessPermissions permissions;

    /**
     * Constructs a <code>FileMetadata</code> object with all fields initialized
     * to their Java defaults.
     */
    private FileMetadata()
    {
    }

    /**
     * Constructs a <code>FileMetadata</code> object with a specified owner and
     * all other fields initialized to their business defaults.
     * 
     * @param owner
     *            The owner, as a long. This is the 48-bit application ID of the
     *            creating application.
     */
    /* package */FileMetadata(long owner)
    {
        this.applicationPriority = 0;
        this.filePriority = 1;
        this.owner = owner;
        this.expirationDate = NULL_DATE_REP;
        this.permissions = new FileAccessPermissions(false, false, false, false, true, true);
    }

    /**
     * Packs a boolean array into a short.
     * <p>
     * This method assumes the boolean array is of length 16 at most. If this is
     * not true, the results are undefined.
     * 
     * @param ba
     *            The boolean array.
     * 
     * @return The array packed into a short, one bit per boolean,
     *         right-justified and zero-padded.
     */
    private static short booleanPack(boolean[] ba)
    {
        short result = 0;

        for (int i = 0, n = ba.length; i < n; ++i)
        {
            boolean b = ba[i];
            result = (short) ((result << 1) | (b ? 1 : 0));
        }

        return result;
    }

    /**
     * Unpacks a short into a boolean array.
     * <p>
     * This method assumes the boolean array is of length 16 at most. If this is
     * not true, the results are undefined.
     * 
     * @param ba
     *            A boolean array to receive the result, defined so that if the
     *            boolean arrays <code>ba1</code> and <code>ba2</code> are the
     *            same length, then after the call
     *            <code>booleanUnpack(ba2, booleanPack(ba1))</code> they will be
     *            element-wise equal.
     * 
     * @param s
     *            The short.
     */
    private static void booleanUnpack(boolean[] ba, short s)
    {
        for (int i = ba.length - 1; i >= 0; --i)
        {
            ba[i] = (s & 1) != 0;
            s >>= 1;
        }
    }

    /**
     * Gets the application priority.
     * 
     * @return The application priority.
     */
    public int getApplicationPriority()
    {
        return applicationPriority;
    }

    /**
     * Gets the expiration date.
     * 
     * @return The expiration date.
     */
    public Date getExpirationDate()
    {
        return toDate(expirationDate);
    }

    /**
     * Gets the file priority.
     * 
     * @return The file priority.
     */
    public int getFilePriority()
    {
        return filePriority;
    }

    /**
     * Gets the owner.
     * 
     * @return The owner.
     */
    public long getOwner()
    {
        return owner;
    }

    /**
     * Gets the file access permissions.
     * 
     * @return The file access permissions.
     */
    public FileAccessPermissions getPermissions()
    {
        return FileAttributesUtil.clone(permissions);
    }

    /**
     * Reads the values of the fields of this object from a file, in a
     * privileged action.
     * 
     * @param f
     *            The file.
     * 
     * @throws IOException
     *             if an IOException happens during the reading.
     */
    private void read(final File f) throws IOException
    {
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws IOException
                {
                    readPriv(f);
                    return null;
                }
            });
        }
        catch (PrivilegedActionException e)
        {
            Exception ex = e.getException();

            throw ex instanceof IOException ? (IOException) ex : new IOException(ex.toString());
        }
    }

    /**
     * Constructs and returns a <code>FileMetadata</code> object from a file.
     * 
     * @param path
     *            The pathname of the file.
     * 
     * @throws IOException
     *             if an IOException happens during the reading of the file.
     */
    /* package */static FileMetadata read(String path) throws IOException
    {
        FileMetadata result = new FileMetadata();

        result.read(new File(path));

        return result;
    }

    /**
     * Reads the values of the fields of this object from a
     * <code>DataInput</code>.
     * 
     * @param in
     *            The <code>DataInput</code>.
     * 
     * @throws IOException
     *             if an IOException happens during the reading.
     */
    private void readExternal(DataInput in) throws IOException
    {
        byte[] version = new byte[4];
        in.readFully(version);
        if (!VERSION_00.equals(new String(version)))
        {
            throw new IOException("unrecognized metadata file format");
        }

        applicationPriority = in.readByte();
        filePriority = in.readByte();

        short pack = in.readShort();

        expirationDate = in.readLong();

        int orgID = in.readInt();
        short appID = in.readShort();
        owner = ((orgID & 0xFFFFFFFFL) << 16) | (appID & 0xFFFFL);

        boolean[] ba = new boolean[8];
        booleanUnpack(ba, pack);

        if (ba[0])
        {
            permissions = null;
        }
        else if (ba[1])
        {
            permissions = new ExtendedFileAccessPermissions(ba[2], ba[3], ba[4], ba[5], ba[6], ba[7], readIntArray(in),
                    readIntArray(in));
        }
        else
        {
            permissions = new FileAccessPermissions(ba[2], ba[3], ba[4], ba[5], ba[6], ba[7]);
        }
    }

    /**
     * Reads an array of integers from a <code>DataInput</code>, decoding length
     * = -1 as null.
     * 
     * @param in
     *            The <code>DataInput</code>.
     * 
     * @return The array of integers.
     * 
     * @throws IOException
     *             if an IOException happens during the reading.
     */
    private static int[] readIntArray(DataInput in) throws IOException
    {
        int[] ia;

        int n = in.readInt();

        if (n == -1)
        {
            ia = null;
        }
        else
        {
            ia = new int[n];

            for (int i = 0; i < n; ++i)
            {
                ia[i] = in.readInt();
            }
        }

        return ia;
    }

    /**
     * Reads the values of the fields of this object from a file.
     * 
     * @param f
     *            The file.
     * 
     * @throws IOException
     *             if an IOException happens during the reading.
     */
    private void readPriv(File f) throws IOException
    {
        InputStream is = new FileInputStream(f);

        try
        {
            DataInputStream in = new DataInputStream(is);

            try
            {
                readExternal(in);
            }
            finally
            {
                in.close();
            }
        }
        finally
        {
            is.close();
        }
    }

    /**
     * Determines whether or not an application priority is the same as this
     * object's application priority as far as the disk representation is
     * concerned.
     * 
     * @param applicationPriority
     *            the application priority.
     * 
     * @return <code>true</code> if the application priority is the same as this
     *         object's application priority as far as the disk representation
     *         is concerned; else <code>false</code>.
     */
    public boolean sameApplicationPriority(int applicationPriority)
    {
        return (byte) this.applicationPriority == (byte) applicationPriority;
    }

    /**
     * Determines whether or not an expiration date is the same as this object's
     * expiration date as far as the disk representation is concerned.
     * 
     * @param expirationDate
     *            the expiration date.
     * 
     * @return <code>true</code> if the expiration date is the same as this
     *         object's expiration date as far as the disk representation is
     *         concerned; else <code>false</code>.
     */
    public boolean sameExpirationDate(Date expirationDate)
    {
        return this.expirationDate == toLong(expirationDate);
    }

    /**
     * Determines whether or not a file priority is the same as this object's
     * file priority as far as the disk representation is concerned.
     * 
     * @param filePriority
     *            the file priority.
     * 
     * @return <code>true</code> if the file priority is the same as this
     *         object's file priority as far as the disk representation is
     *         concerned; else <code>false</code>.
     */
    public boolean sameFilePriority(int filePriority)
    {
        return (byte) this.filePriority == (byte) filePriority;
    }

    /**
     * Determines whether or not two arrays of <code>int</code>s are the same as
     * far as the disk representation is concerned.
     * 
     * @param ia1
     *            the first array of <code>int</code>s.
     * @param ia2
     *            the second array of <code>int</code>s.
     * 
     * @return <code>true</code> if the two array of <code>int</code>s are the
     *         same as far as the disk representation is concerned; else
     *         <code>false</code>.
     */
    private static boolean sameInts(int[] ia1, int[] ia2)
    {
        if (ia1 == null)
        {
            return ia2 == null;
        }

        if (ia2 == null)
        {
            return false;
        }

        if (ia1.length != ia2.length)
        {
            return false;
        }

        for (int i = 0, n = ia1.length; i < n; ++i)
        {
            if (ia1[i] != ia2[i])
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines whether or not some file access permissions are the same as
     * this object's file access permissions as far as the disk representation
     * is concerned.
     * 
     * @param permissions
     *            the file access permissions.
     * 
     * @return <code>true</code> if the file access permissions are the same as
     *         this object's file access permissions as far as the disk
     *         representation is concerned; else <code>false</code>.
     */
    public boolean samePermissions(FileAccessPermissions permissions)
    {
        if (toShort(this.permissions) != toShort(permissions))
        {
            return false;
        }

        if (!(permissions instanceof ExtendedFileAccessPermissions))
        {
            return true;
        }

        // assert this.permissions instanceof ExtendedFileAccessPermissions;
        // assert permissions instanceof ExtendedFileAccessPermissions;

        ExtendedFileAccessPermissions thisEfap = (ExtendedFileAccessPermissions) this.permissions;
        ExtendedFileAccessPermissions thatEfap = (ExtendedFileAccessPermissions) permissions;

        return sameInts(thisEfap.getReadAccessOrganizationIds(), thatEfap.getReadAccessOrganizationIds())
                && sameInts(thisEfap.getWriteAccessOrganizationIds(), thatEfap.getWriteAccessOrganizationIds());
    }

    /**
     * Sets the application priority.
     * 
     * @param applicationPriority
     *            The application priority.
     */
    public void setApplicationPriority(int applicationPriority)
    {
        this.applicationPriority = (byte) applicationPriority;
    }

    /**
     * Sets the expiration date.
     * 
     * @param expirationDate
     *            The expiration date.
     */
    public void setExpirationDate(Date expirationDate)
    {
        this.expirationDate = toLong(expirationDate);
    }

    /**
     * Sets the file priority.
     * 
     * @param filePriority
     *            The file priority.
     */
    public void setFilePriority(int filePriority)
    {
        this.filePriority = (byte) filePriority;
    }

    /**
     * Sets the file access permissions.
     * 
     * @param permissions
     *            The file access permissions.
     */
    public void setPermissions(FileAccessPermissions permissions)
    {
        this.permissions = FileAttributesUtil.clone(permissions);
    }

    /**
     * Returns the date with a given disk representation.
     * 
     * @param d
     *            the disk representation.
     * 
     * @return the date with the disk representation.
     */
    private static Date toDate(long l)
    {
        return l == NULL_DATE_REP ? null : new Date(l);
    }

    /**
     * Returns the disk representation of a date.
     * 
     * @param d
     *            the date.
     * 
     * @return the disk representation of the date.
     */
    private static long toLong(Date d)
    {
        return d == null ? NULL_DATE_REP : d.getTime();
    }

    /**
     * Returns the first part of the disk representation of some file access
     * permissions.
     * 
     * @param permissions
     *            the file access permissions.
     * 
     * @return the first part of the disk representation of the file access
     *         permissions.
     */
    private static short toShort(FileAccessPermissions permissions)
    {
        boolean[] ba = permissions == null ? new boolean[] { true, false, false, false, false, false, false, false }
                : new boolean[] { false, permissions instanceof ExtendedFileAccessPermissions,
                        permissions.hasReadWorldAccessRight(), permissions.hasWriteWorldAccessRight(),
                        permissions.hasReadOrganisationAccessRight(), permissions.hasWriteOrganisationAccessRight(),
                        permissions.hasReadApplicationAccessRight(), permissions.hasWriteApplicationAccessRight() };

        return booleanPack(ba);
    }

    /**
     * Returns a string representation of the object.
     * 
     * @return a string representation of the object.
     */
    public String toString()
    {
        return "[" + "application priority = " + applicationPriority + ", expiration date = " + toDate(expirationDate)
                + ", file priority = " + FileAttributesUtil.priorityToString(filePriority) + ", owner = "
                + Long.toHexString(owner) + ", permissions = " + FileAttributesUtil.toString(permissions) + "]";
    }

    /**
     * Writes the values of the fields of this object to a file, in a privileged
     * action.
     * 
     * @param f
     *            The file.
     * 
     * @throws IOException
     *             if an IOException happens during the writing.
     */
    /* package */void write(final File f) throws IOException
    {
        try
        {
            AccessController.doPrivileged(new PrivilegedExceptionAction()
            {
                public Object run() throws IOException
                {
                    writePriv(f);
                    return null;
                }
            });
        }
        catch (PrivilegedActionException e)
        {
            Exception ex = e.getException();

            throw ex instanceof IOException ? (IOException) ex : new IOException(ex.toString());
        }
    }

    /**
     * Writes the values of the fields of this object to a
     * <code>DataOutput</code>.
     * 
     * @param out
     *            The <code>DataOutput</code>.
     * 
     * @throws IOException
     *             if an IOException happens during the writing.
     */
    private void writeExternal(DataOutput out) throws IOException
    {
        out.writeBytes(VERSION_00);

        out.writeByte(applicationPriority);
        out.writeByte(filePriority);

        out.writeShort(toShort(permissions));

        out.writeLong(expirationDate);

        int orgID = (int) (owner >> 16);
        short appID = (short) owner;
        out.writeInt(orgID);
        out.writeShort(appID);

        if (permissions instanceof ExtendedFileAccessPermissions)
        {
            ExtendedFileAccessPermissions efap = (ExtendedFileAccessPermissions) permissions;

            writeIntArray(out, efap.getReadAccessOrganizationIds());
            writeIntArray(out, efap.getWriteAccessOrganizationIds());
        }
    }

    /**
     * Writes an array of integers to a <code>DataOutput</code>, encoding null
     * as length = -1.
     * 
     * @param out
     *            The <code>DataOutput</code>.
     * @param ia
     *            The array of integers.
     * 
     * @throws IOException
     *             if an IOException happens during the writing.
     */
    private static void writeIntArray(DataOutput out, int[] ia) throws IOException
    {
        if (ia == null)
        {
            out.writeInt(-1);
        }
        else
        {
            int n = ia.length;

            out.writeInt(n);

            for (int i = 0; i < n; ++i)
            {
                out.writeInt(ia[i]);
            }
        }
    }

    /**
     * Writes the values of the fields of this object to a file.
     * 
     * @param f
     *            The file.
     * 
     * @throws IOException
     *             if an IOException happens during the writing.
     */
    private void writePriv(File f) throws IOException
    {
        OutputStream os = new FileOutputStream(f);

        try
        {
            DataOutputStream out = new DataOutputStream(os);

            try
            {
                writeExternal(out);
            }
            finally
            {
                out.close();
            }
        }
        finally
        {
            os.close();
        }
    }
}
