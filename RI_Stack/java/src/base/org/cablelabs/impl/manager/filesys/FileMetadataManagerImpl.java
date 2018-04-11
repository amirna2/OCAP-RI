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

import java.io.IOException;

import org.cablelabs.impl.io.FileMetadataManager;
import org.cablelabs.impl.security.PersistentStorageAttributes;

/**
 * Implements FileMetadataManager for java.io support in the OCAP stack.
 * <p>
 * An object of this class can be installed as the file metadata manager for the
 * implementation. The object will update file metadata as needed (1) during
 * file creation, renaming, and deleting, and (2) to mark a file as read-only;
 * and will request that file metadata be deleted when the virtual machine
 * terminates.
 */
public class FileMetadataManagerImpl implements FileMetadataManager
{
    /**
     * Private constructor.
     * <p>
     * Use <code>FileMetadataManagerImpl.getInstance()</code> to instantiate
     * this class.
     */
    private FileMetadataManagerImpl()
    {
    }

    /**
     * Returns the singleton instance
     * 
     * @return
     */
    public static FileMetadataManager getInstance()
    {
        if (singleton == null)
        {
            singleton = new FileMetadataManagerImpl();
        }
        return singleton;
    }

    /**
     * Updates file metadata in response to a certain file being created.
     * 
     * @param path
     *            pathname of the file being created.
     * 
     * @throws IOException
     *             if the creation fails.
     */
    public void create(String path, boolean isFile) throws IOException
    {
        psa.createEntry(path, isFile);
    }

    /**
     * Updates file metadata in response to a certain file being deleted.
     * 
     * @param path
     *            pathname of the file being deleted.
     * 
     * @return <code>false</code> if an error occurred; else <code>true</code>.
     */
    public boolean delete(String path)
    {
        try
        {
            psa.deleteEntry(path);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /**
     * Requests that file metadata be deleted when the virtual machine
     * terminates.
     * 
     * @param path
     *            pathname of the file whose metadata is to be deleted.
     */
    public void deleteOnExit(String path)
    {
        psa.deleteEntryOnExit(path);
    }

    /**
     * Updates file metadata in response to a certain file being renamed.
     * 
     * @param oldPath
     *            old pathname of the file being renamed.
     * @param newPath
     *            new pathname of the file being renamed.
     * 
     * @return <code>false</code> if an error occurred; else <code>true</code>.
     */
    public boolean rename(String oldPath, String newPath)
    {
        try
        {
            psa.renameEntry(oldPath, newPath);
            return true;
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /**
     * Updates file metadata to mark a certain file as read-only.
     * 
     * @param path
     *            pathname of the file being set read-only.
     * 
     * @return <code>false</code> if an error occurred; else <code>true</code>.
     */
    public boolean setReadOnly(String path)
    {
        // TODO:
        return false;
    }

    // Singleton instance of the FileMetadataManagerImpl.
    private static FileMetadataManagerImpl singleton = null;

    private static PersistentStorageAttributes psa = PersistentStorageAttributes.getInstance();
}
