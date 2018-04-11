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

package org.cablelabs.impl.manager.auth;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Hashtable;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.FileSysCommunicationException;
import org.cablelabs.impl.util.SystemEventUtil;
import org.apache.log4j.Logger;

/**
 * This class represents a digest entry from a hash file. It contains the digest
 * type, digest value and one or more file names associated with the digest
 * value.
 */
class DigestEntry
{
    /**
     * Construction for a digest entry.
     * 
     * @param type
     *            is the type of digest entry.
     * @param count
     *            is the number of digest entries.
     */
    DigestEntry(int type, int count)
    {
        this.type = type;
        names = new Hashtable(count);
        inorder = new String[count];

        // Determine digest length based on type.
        switch (type)
        {
            case AUTH_MD5:
                length = 16;
                break;
            case AUTH_SHA1:
            case AUTH_SHA1_PREFIX:
                length = 20;
                break;
            case AUTH_NON:
            default:
                length = 0; // Set length, no digest.
                type = AUTH_NON; // Set type to non-authenticated.
                validity = new Boolean(true); // Set state to valid for java.io.
                break;
        }
    }

    /**
     * Determine if this digest entry is valid. This done by performing the
     * correct hash over 1 or more objects contained in the digest.
     * 
     * @param targName
     *            is a path string to the target file.
     * @param subPath
     *            is the sub-path associated with the hashfile that this digest
     *            entry came from.
     * @param fs
     *            is the FileSys to use to access the security files associated
     *            with the target file.
     * @param file
     *            is a byte array reference containing the file data to verify
     *            if provided by the caller, null otherwise.
     * @param prefix
     *            is the "ocap" or "dvb" prefix in use for security files.
     * @param e
     *            is an array for holding any excetpions that may occur during
     *            the validation process.
     * 
     * @return true if the digest entry is valid, false otherwise.
     */
    boolean isValidDigest(String targName, String subPath, FileSys fs, byte[] file, String prefix, Exception[] e)
            throws FileSysCommunicationException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Checking validity of digest for '" + targName + "'");
        }

        // Get a message digest of the appropriate type for digest calculation.
        MessageDigest md;
        try
        {
            md = MessageDigest.getInstance(getAlgorithmName());
        }
        catch (NoSuchAlgorithmException expt)
        {
            SystemEventUtil.logRecoverableError(expt);
            return false;
        }

        // Iterate through all the files in the digest building up the hash
        // value.
        String[] names = getFileNames();

        for (int i = 0; i < names.length; ++i)
        {
            String name = subPath + "/" + names[i];
            byte[] data = file;
            byte entry_type = 1; // Used for SHA-1 with prefix digests.

            // If no file data available (i.e. not target file), acquire file
            // data.
            if ((file == null) || (targName.compareTo(name) != 0))
            {
                // If the digest entry is a directory, get its hashfile.
                String hashFileName = name + "/" + prefix + ".hashfile";
                if (fs.exists(hashFileName))
                {
                    name = hashFileName;
                    entry_type = 0; // Directories use entry type 0 for SHA-1
                                    // with prefix.
                }

                // Load the target file data, for content verification.
                try
                {
                    data = fs.getFileData(name).getByteData();
                }
                catch (IOException ioe)
                {
                    if (e != null)
                        e[0] = ioe;// Return exception.
                    SystemEventUtil.logRecoverableError(ioe);
                    return setValidity(false); // Can't read file, can't
                                               // authenticate.
                }
            }
            // Update value based on digest type.
            if (this.type == AUTH_SHA1_PREFIX)
            {
                md.update(new byte[] { 0, 0, 0, entry_type }); // Add first
                                                               // portion of
                                                               // prefix.

                // Now add in file size as second portion of prefix.
                byte buf[] = new byte[4];
                int fsize = data.length;
                buf[0] = (byte) ((fsize & 0xFF000000L) >> 24);
                buf[1] = (byte) ((fsize & 0x00FF0000L) >> 16);
                buf[2] = (byte) ((fsize & 0x0000FF00L) >> 8);
                buf[3] = (byte) ((fsize & 0x000000FFL));
                md.update(buf);
            }
            md.update(data);
        }

        // If the target digest failed authentication, record it and return the
        // result.
        if (MessageDigest.isEqual(md.digest(), getDigest()) == false)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Invalid digest value found for '" + targName + "'");
            }
            return setValidity(false); // Bad hash code value.
        }
        if (log.isDebugEnabled())
        {
            log.debug("Valid digest value found for '" + targName + "'");
        }

        // Checked out ok.
        return true;
    }

    /**
     * Add a file name to this digest entry.
     * 
     * @param name
     *            is a file name associated with this digest entry.
     */
    void addFile(String name)
    {
        names.put(name, name);
        inorder[index++] = name;
    }

    /**
     * Determine if this digest entry contains the specified file name.
     * 
     * @param target
     *            is the name of a file to check for membership of this digest
     *            entry.
     * 
     * @return boolean indicating whether the file is a member of this digest.
     */
    boolean contains(String target)
    {
        return (names.get(target) != null);
    }

    /**
     * Get the length of the digest value.
     * 
     * @return int size of the digest value.
     */
    int getLength()
    {
        return length; // Get the length of this digest type.
    }

    /**
     * Set the digest value byte array.
     * 
     * @param digest
     *            is the byte array of containing the digest.
     */
    void setDigest(byte[] digest)
    {
        this.digest = digest;
    }

    /**
     * Get the digest value byte array.
     * 
     * @return byte[] containing the digest value.
     */
    byte[] getDigest()
    {
        return digest; // Get the digest bytes.
    }

    /**
     * Get the digest type.
     * 
     * @return short value of the digest type.
     */
    int getType()
    {
        return type; // Return the digest type.
    }

    /**
     * Get the digest algorithm type name.
     * 
     * @return String name of hash algorithm.
     */
    String getAlgorithmName()
    {
        switch (type)
        {
            case AUTH_MD5:
                return "MD5";
            case AUTH_SHA1:
            case AUTH_SHA1_PREFIX:
                return "SHA-1";
            case AUTH_NON:
            default:
                return null;
        }
    }

    /**
     * Set authentication state.
     * 
     * @param state
     *            indicating if the digest authenticated correctly.
     * 
     * @return boolean is the same value passed in.
     */
    boolean setValidity(boolean state)
    {
        validity = new Boolean(state);
        return state;
    }

    /**
     * Returns the authentication state of the entry.
     * 
     * @return boolean indicating whether the digest entry authenticated
     *         correctly or not.
     */
    Boolean getValidity()
    {
        return validity;
    }

    /**
     * Get all of the file names in the digest.
     * 
     * @return Enumeration of all the file names in the digest.
     */
    String[] getFileNames()
    {
        return inorder; // Return all file name in digest.
    }

    /**
     * Get all of the file names in the digest.
     * 
     * @return Enumeration of all the file names in the digest.
     */
    Collection getHashedFileNames()
    {
        return names.values(); // Return all file name in digest.
    }

    private Boolean validity = null; // Indicates if the digest entry if valid

    private int type; // Digest type.

    private Hashtable names = null; // All the names in the digest.

    private int length = 0; // Length of the digest.

    private byte[] digest = null; // Digest entry bytes.

    private String[] inorder = null; // Digest names in order for hash
                                     // calculation.

    private int index = 0;

    // Digest types from MHP 12.4.1.1, Table 49.
    static final short AUTH_NON = 0;

    static final short AUTH_MD5 = 1;

    static final short AUTH_SHA1 = 2;

    static final short AUTH_SHA1_PREFIX = 3;

    private static final Logger log = Logger.getLogger(DigestEntry.class.getName());
}
