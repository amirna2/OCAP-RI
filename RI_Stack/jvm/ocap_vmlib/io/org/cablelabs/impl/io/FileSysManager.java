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

package org.cablelabs.impl.io;

import java.io.IOException;

/**
 * Allows the implementation to retrieve the appropriate <code>FileSys</code>
 * instance for reading and the appropriate <code>WriteableFileSys</code>
 * instance for writing to a path. Also allows the implementation to update the
 * <code>FileSysMgr</code> for the entire system.
 * 
 * Allows the implementation to update file metadata as needed (1) during file
 * creation, renaming, and deleting, and (2) to mark a file as read-only; and to
 * request that file metadata be deleted when the virtual machine terminates.
 * Also allows the implementation to update the <code>FileMetadataManager</code>
 * for the entire system.
 */
public class FileSysManager
{
    /**
     * Gives the implementation the ability to update the
     * <code>FileSysMgr</code> instance with a new instance.
     * 
     * @param newFsm
     *            new <code>FileSysMgr</code> instance to use.
     */
    public static void setFileManager(FileSysMgr newFsm)
    {
        fsm = newFsm == null ? defaultFsm : newFsm;
    }

    /**
     * Gives the implementation the ability to update the
     * <code>FileMetadataManager</code> instance with a new instance.
     * 
     * @param newFmm
     *            new <code>FileMetadataManager</code> instance to use.
     */
    public static void setFileMetadataManager(FileMetadataManager newFmm)
    {
        fmm = newFmm == null ? defaultFmm : newFmm;
    }

    /**
     * Returns the appropriate <code>FileSys</code> instance based on the
     * supplied <code>path</code>.
     * 
     * @param path
     *            location of the file to access
     * @return <code>FileSys</code> object to retrieve file data
     */
    public static FileSys getFileSys(String path)
    {
        return fsm.doGetFileSys(path);
    }

    /**
     * Returns the default file system implementation that sends all I/O calls
     * directly to native and MPE
     * 
     * @return The default file system
     */
    public static FileSys getDefaultFileSys()
    {
        return defaultFs;
    }

    /**
     * Returns the appropriate <code>WriteableFileSys</code> instance based on
     * the supplied <code>path</code>.
     * 
     * @param path
     *            location of the file to access
     * @return <code>WriteableFileSys</code> object to retrieve file data
     */
    public static WriteableFileSys getWriteableFileSys(String path)
    {
        return fsm.doGetWriteableFileSys(path);
    }

    /**
     * Returns the default writeable file system implementation that sends all
     * I/O calls directly to native and MPE
     * 
     * @return The default writeable file system
     */
    public static WriteableFileSys getDefaultWriteableFileSys()
    {
        return dwFs;
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
    public static void createMetadata(String path, boolean isFile) throws IOException
    {
        fmm.create(path, isFile);
    }

    /**
     * Updates file metadata in response to a certain file being deleted.
     * 
     * @param path
     *            pathname of the file being deleted.
     * 
     * @return <code>false</code> if an error occurred; else <code>true</code>.
     */
    public static boolean deleteMetadata(String path)
    {
        return fmm.delete(path);
    }

    /**
     * Requests that file metadata be deleted when the virtual machine
     * terminates.
     * 
     * @param path
     *            pathname of the file whose metadata is to be deleted.
     */
    public static void deleteMetadataOnExit(String path)
    {
        fmm.deleteOnExit(path);
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
    public static boolean renameMetadata(String oldPath, String newPath)
    {
        return fmm.rename(oldPath, newPath);
    }

    /**
     * Updates file metadata to mark a certain file as read-only.
     * 
     * @param path
     *            pathname of the file being set read-only.
     * 
     * @return <code>false</code> if an error occurred; else <code>true</code>.
     */
    public static boolean setReadOnly(String path)
    {
        return fmm.setReadOnly(path);
    }

    // DefaultFileSys instance returned by doGetFileSys()
    private static FileSys defaultFs = new DefaultFileSys();

    // DefaultWriteableFileSys instance return by doGetWriteableFileSys()
    private static WriteableFileSys dwFs = new DefaultWriteableFileSys();

    // Default FileSysMgr instance.
    private static FileSysMgr defaultFsm = new FileSysMgr()
    {
        public FileSys doGetFileSys(String path)
        {
            return defaultFs;
        }

        public WriteableFileSys doGetWriteableFileSys(String path)
        {
            return dwFs;
        }
    };

    // Default FileMetadataManager instance.
    private static FileMetadataManager defaultFmm = new FileMetadataManager()
    {
        public void create(String path, boolean isFile)
        {
        }

        public boolean delete(String path)
        {
            return true;
        }

        public void deleteOnExit(String path)
        {
        }

        public boolean rename(String oldPath, String newPath)
        {
            return true;
        }

        public boolean setReadOnly(String path)
        {
            return true;
        }
    };

    // FileSysMgr instance used by this class
    private static FileSysMgr fsm = defaultFsm;

    // FileMetadataManager instance used by this class
    private static FileMetadataManager fmm = defaultFmm;
}
