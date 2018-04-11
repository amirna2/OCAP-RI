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

import java.util.Vector;
import java.util.Hashtable;

import org.apache.log4j.Logger;

/**
 * This class is used to process the contents of a hashfile. The format of the
 * hashfile is defined by MHP as follows:
 * 
 * HashFile () {
 *   digest_count                            16-bits uimsbf
 *   for (i=0; i < digest_count; i++) {
 *     digest_type                           8-bits  uimsbf
 *     name_count                            16-bits uimsbf
 *     for (j=0; j < name_count; j ++) {
 *       name_length                         8-bits  uimsbf
 *       for (k=0; k < name_length; k++) {
 *         name_byte                         8-bits  bslbf
 *       }
 *     }
 *     for (j=0; j < digest_length; j++) {
 *       digest_byte                         8-bits  bslbf
 *     }
 *   }
 * }
 */
class HashFile
{
    /**
     * HashFile constructor.
     * 
     * @param path
     *            is the path to the location of the hash file.
     * @param file
     *            is the data contents of the hashfile.
     */
    HashFile(String path, byte[] file)
    {
        // Save the path to the hashfile and its contents.
        this.path = path;
        this.contents = file;

        // Setup for parsing the file contents later.
        fileCount = contents.length;
        digestCount = (((contents[0] & 0xff) << 8) | (contents[1] & 0xff));
        fileLoc = 2;
        digestLoc = 0;
        digests = new Vector(digestCount);
    }

    /**
     * Get the path associated with the hash file.
     * 
     * @return String containing the path to the hash file.
     */
    String getPath()
    {
        return path;
    }

    /**
     * Get the entire contents of the hash file.
     * 
     * @return byte[] containing the contents of the hash file.
     */
    byte[] getBytes()
    {
        return contents; // Return the file contents.
    }

    /**
     * Get the digest entry for the specified file. This method will parse the
     * digest entries in the hashfile until the target entry is found. All of
     * the digest entries parsed prior to the target entry will be cached in a
     * digest entry cache for faster subsequent access and the location at which
     * processing was stopped will be saved so that any subsequent searches will
     * pickup from where processing left off in the previous search.
     * 
     * Any errors encountered while parsing will result in the associated digest
     * entries getting marked as "incorrectly authenticated". And, if the target
     * digest entry has not be found up to that point, null will be returned
     * indicating that the target was not found.
     * 
     * @param target
     *            is the full path to the target file.
     * 
     * @return <code>DigestEntry</code> if found or null.
     */
    DigestEntry getEntry(String target)
    {
        DigestEntry targDigest;

        // Check for digest_count == 0 case for hashfile (MHP 12.4.1.5).
        if (digestCount == 0)
        {
            (targDigest = new DigestEntry(DigestEntry.AUTH_NON, 1)).addFile(target);
            return targDigest;
        }

        // Extract the target file name.
        if (target != null)
        {
            target = target.substring(target.lastIndexOf('/') + 1);
        }

        // Check cached digest entries first.
        if ((targDigest = searchDigests(target)) != null)
        {
            return targDigest;
        }

        // Parse unprocessed digest entries until the target entry is found.
        for (; fileLoc < fileCount && digestLoc < digestCount && targDigest == null; ++digestLoc)
        {
            // Make sure hashfile has enough data.
            if (fileLoc + 3 > fileCount)
            {
                fileLoc = fileCount;
                return null;
            }

            // Read digest_type;
            int digestType = contents[fileLoc++] & 0xff;

            // Read name_count.
            int nameCount = (((contents[fileLoc] & 0xff) << 8) | (contents[fileLoc + 1] & 0xff));
            fileLoc += 2;

            // Read next digest_type and instantiate a new digest entry.
            DigestEntry de = new DigestEntry(digestType, nameCount);

            // Add this digest entry to the hash file cache of entries.
            addDigestEntry(de);

            // Parse one or more file names associated with the digest value.
            for (int j = 0; j < nameCount; ++j)
            {
                // Make sure hashfile has enough data.
                if (fileLoc + 1 > fileCount)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("hash file incomplete, marking '" + target + "' invalid");
                    }
                    de.setValidity(false); // Mark current digest as invalid.
                    fileLoc = fileCount;
                    return targDigest; // Return whatever was found so far.
                }

                // Read the next name_length.
                int nameLength = contents[fileLoc++] & 0xff;

                // If name_length is 0, all associated entries are
                // considered incorrectly authenticated (MHP 12.4.1.5).
                if (nameLength == 0)
                {
                    de.setValidity(false);
                }
                else
                {
                    if (fileLoc + nameLength > fileCount)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("hash file incomplete, marking '" + target + "' invalid");
                        }
                        de.setValidity(false);
                        fileLoc = fileCount;
                        return targDigest; // Return whatever was found so far.
                    }

                    // Read the next name.
                    String name = new String(contents, fileLoc, nameLength);

                    // Add it to the DigestEntry.
                    de.addFile(name);

                    // Is this the one we're looking for?
                    if (name.equalsIgnoreCase(target))
                    {
                        targDigest = de;
                    }

                    fileLoc += nameLength; // Advance content index.
                }
            }
            byte[] digestBytes = new byte[de.getLength()];

            // Make sure hashfile has enough data.
            if (fileLoc + digestBytes.length > fileCount)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("hash file incomplete, marking '" + target + "' invalid");
                }
                de.setValidity(false); // Mark current digest as invalid.
                fileLoc = fileCount;
                return targDigest; // Return whatever was found so far.
            }

            // Read the digest bytes.
            System.arraycopy(contents, fileLoc, digestBytes, 0, digestBytes.length);
            fileLoc += digestBytes.length;

            // Set the digest bytes in the entry.
            de.setDigest(digestBytes);
        }

        if (target != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("target entry '" + target + "' " + (targDigest == null ? "not " : "") + "found in hash file");
            }
        }

        // Return null or the entry found.
        return targDigest;
    }

    /**
     * Adds a digest entry to the database of entries (cache).
     * 
     * @param de
     *            the digest entry to add to the set of entries.
     */
    void addDigestEntry(DigestEntry de)
    {
        digests.add(de);
    }

    /**
     * Removes the specified digest entry from the database of entries (cache).
     * 
     * @param de
     *            is the target digest entry to remove.
     */
    void removeDigestEntry(DigestEntry de)
    {
        digests.removeElement(de);
    }

    /**
     * Search the database for a specific file's digest.
     * 
     * @param target
     *            is the full path to the target file.
     * 
     * @return DigestEntry for the target or null if not found.
     */
    DigestEntry searchDigests(String target)
    {
        if (target == null || digests.isEmpty()) return null;

        // Search the database.
        for (int i = 0; i < digests.size(); ++i)
        {
            DigestEntry de = (DigestEntry) digests.get(i);
            if (de.contains(target)) return de;
        }
        return null;
    }

    /**
     * Determine if the hashfile contains all of the specified objects. This
     * method is used for authentication of directories that are marked as
     * authenticated.
     * 
     * @param files
     *            is an array of strings to validate against the names in the
     *            hashfile.
     * 
     * @return true if the hashfile has proper coverage, false otherwise.
     */
    boolean contains(String[] files)
    {
        // Iterate through the files from the directory checking
        // for membership in the hashfile.
        for (int i = 0; i < files.length; ++i)
        {
            // Skip any security files (e.g. ocap.hashfile)
            if (isSecurityFile(files[i]) == false) if (getEntry(files[i]) == null) return false; // Discrepancy
                                                                                                 // found.
        }

        // At this point all of the listed files are also listed
        // in the hashfile. Now make sure there aren't any non-security
        // files in the hashfile that are in the set of listed files.
        Hashtable h = new Hashtable();
        for (int i = 0; i < files.length; ++i)
            h.put(files[i], files[i]); // Place names in a hashtable.

        // Iterate through each digest checking names against hashtable list.
        for (int i = 0; i < digests.size(); ++i)
        {
            String[] names = ((DigestEntry) digests.get(i)).getFileNames();
            for (int j = 0; j < names.length; ++j)
            {
                // Skip security files.
                if (isSecurityFile(names[j]) == false) if (h.contains(names[j]) == false) return false; // Discrepancy
                                                                                                        // found.
            }
        }
        // Hashfile matches.
        return true;
    }

    /**
     * Return the names contained in all of the digest entries in this hashfile.
     * 
     * @return String[] containing all of the names.
     */
    String[] getNames()
    {
        Vector names = new Vector();

        // First make sure the entire hashfile has been processed.
        getEntry(null);

        // Now iterate through all of the digest entries acquiring names.
        for (int i = 0; i < digests.size(); ++i)
        {
            // Added the next digest entry name(s).
            names.addAll(((DigestEntry) digests.get(i)).getHashedFileNames());
        }
        return (String[]) names.toArray(new String[0]);
    }

    /**
     * Determine if the specified file is an OCAP security file. This method is
     * used to support authentication of directories, which is supposed to
     * ignore any security files that may be in the directory.
     * 
     * @param file
     *            is the name of the file to check.
     * 
     * @return true if it is an OCAP security file.
     */
    private boolean isSecurityFile(String file)
    {
        // Iterate through all of the security file names.
        for (int i = 0; i < securityFiles.length; ++i)
        {
            // First check for an exact match.
            if (file.compareTo(securityFiles[i]) == 0) return true;

            // Does it start with "<ocap security name>"?
            if (file.startsWith(securityFiles[i]))
            {
                // At least a partial match, now check for one of
                // the security files that contains a suffix.
                if (securityFiles[i].endsWith("."))
                {
                    // Lastly, check for a numerical suffix.
                    try
                    {
                        Integer.parseInt(file.substring(file.lastIndexOf(".") + 1));
                    }
                    catch (NumberFormatException e)
                    {
                        return false; // Non-numerical suffix.
                    }

                }
                else
                    return false; // Not exact match & has incorrect suffix.
            }
        }
        return false; // Doesn't match any security files.
    }

    // Base names for OCAP security files (removed "dvb" signing support).
    String[] securityFiles = { "ocap.hashfile",
    // "dvb.hashfile",
            "ocap.signaturefile.",
            // "dvb.signaturefile.",
            "ocap.certificatefile.",
            // "dvb.certificatefile."
            "ocap.certificates." };

    // Vector to hold all of the digest in the hash file.
    private Vector digests = null;

    // Path to the hash file location.
    private String path = null;

    // Raw contents of the hash file.
    private byte[] contents = null;

    // Byte position within hash file.
    private int fileLoc;

    // Digest position within hash file.
    private int digestLoc;

    // Total number of bytes in hash file.
    private int fileCount;

    // Total number of digests in hash file.
    private int digestCount;

    private static final Logger log = Logger.getLogger(HashFile.class.getName());
}
