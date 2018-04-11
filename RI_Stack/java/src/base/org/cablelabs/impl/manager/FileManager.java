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

package org.cablelabs.impl.manager;

import java.io.File;
import java.io.IOException;
import java.security.PermissionCollection;

import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.WriteableFileSys;
import org.cablelabs.impl.io.FileData;
import org.cablelabs.impl.signalling.AppEntry;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * A <code>Manager</code> that provides the system's file system management
 * functionality. The <code>FileManager</code> implementation is used to provide
 * support for asynchronous file system calls (eg, load file, track version
 * changes, etc.).
 * 
 * @see ManagerManager
 */
public interface FileManager extends Manager
{
    /**
     * Adds or updates the file cache for the given path with the supplied data.
     * 
     * @param path
     *            pathname to update the file data for
     * @param data
     *            file data to store in the cache
     */
    public void updateCache(String path, FileData data);

    /**
     * Returns the file data found in the cache for the specified pathname.
     * 
     * @param path
     *            pathname to supply the path information for.
     * @return the file data or <code>null</code> if the file is not cached.
     */
    public FileData getCached(String path);

    /**
     * Removes the data stored in the cache for the given pathname.
     * 
     * @param path
     *            pathname to remove the cached data for.
     */
    public void flushCache(String path);

    /**
     * Returns the appropriate <code>FileSys</code> instance based on the
     * supplied <code>path</code>.
     * 
     * @param path
     *            location of the file to access
     * @return <code>FileSys</code> object to retrieve file data
     */
    public FileSys getFileSys(String path);

    /**
     * Returns the default (base) <code>FileSys</code> instance.
     * 
     * @return <code>FileSys</code> default object to retrieve file data
     */
    public FileSys getDefaultFileSys();

    /**
     * Returns the default (base) <code>FileSys</code> instance.
     * 
     * @return <code>FileSys</code> default object to retrieve file data
     */
    public WriteableFileSys getDefaultWriteableFileSys();

    /**
     * Gives the implementation the ability to register other
     * <code>FileSys</code> objects with the <code>FileManager</code>.
     * 
     * @param path
     *            a partial path string to use for path matching
     * @param fs
     *            the <code>FileSys</code> instance to return when the paths
     *            match
     */
    public void registerFileSys(String path, FileSys fs);

    /**
     * Gives the implementation the ability to remove registered
     * <code>FileSys</code> objects from the <code>FileSysManager</code>. This
     * call removes mounts added with the <code>registerFileSys()</code> call.
     * 
     * @param path
     *            mount pathname used when <code>registerFileSys()</code> was
     *            called.
     */
    public void unregisterFileSys(String path);

    /**
     * Acquire the file system to be used to write data to a file on the
     * specificied path.
     * 
     * @param path
     *            is the path to the target file.
     * 
     * @return WriteableFileSys reference to use for the "write" methods.
     */
    public WriteableFileSys getWriteableFileSys(String path);

    /**
     * Method for registering a writeable file system.
     * 
     * @param path
     *            is the base mount point
     * @param fs
     *            is the writable file system.
     */
    public void registerWriteableFileSys(String path, WriteableFileSys fs);

    /**
     * <code>DSMCCObject</code> is a special kind of <code>File</code> that can
     * be associated with 2 different kinds of filesystems depending on whether
     * or not it has been loaded:
     * <ul>
     * <li>OCFileSys: In this case the file has not yet been loaded and all
     * attempts to access file data must go through the native OC code
     * <li>LoadedFileSys: In this case, the file data has been loaded and the
     * system data is cached in memory
     * </ul>
     * 
     * This method registers an OC file, directory, stream, or stream event path
     * with a <code>LoadedFileSys</code>
     * 
     * @param path
     *            the file, directory, stream, or stream event path
     * @param fs
     *            the associated filesystem
     */
    public void registerLoadedFileSys(String path, FileSys fs);

    /**
     * Unregisters a previously registered loaded filesystem path
     * 
     * @param path
     *            the <code>FileSys</code> to unregister
     */
    public void unregisterLoadedFileSys(FileSys fs);

    /**
     * Register the specified storage device mount point for support under the
     * implementation's peristent storage system. The mount points may take one
     * of the following forms:
     * <ul>
     * <li>/<dev>
     * <li>/<dev>=<num>[k,K,m,M,g,G]
     * <li>/<dev>/<path>
     * <li>/<dev>/<path>=<num>[k,K,m,M,g,G]
     * </ul>
     * Where <num> is the quota amount of storage in bytes unless on of the size
     * modifiers is present. The size modifiers are supported as follows:
     * <ul>
     * <li>k or K = <num> * 1024
     * <li>m or M = <num> * 1024 * 1024
     * <li>g or G = <num> * 1024 * 1024 * 1024
     * </ul>
     * <p>
     * 
     * @param mount
     *            is a string representing the mount point
     */
    public void registerWriteableMount(String mount);

    /**
     * Unregister the specified storage device mount point for support under the
     * implementation's persistent storage system.
     * <p>
     * 
     * @param mount
     *            is a string representing the mount point
     */
    public void unregisterWriteableMount(String mount);

    /**
     * Create the persistent storage directory for the given application, if
     * persistent storage access is granted by the given permissions.
     * <p>
     * 
     * @param appEntry
     *            application entry
     * @return the full path of the java.io.tmpdir directory that was designated
     *         for this application
     */
    public String createPersistentStorage(AppEntry appEntry);

    /**
     * Retrieve the application priority associated with the named file.
     * 
     * @param path
     *            abstract pathname of the file to get the priority for
     * @return the priority of the application that created the file
     */
    public int getStoredApplicationPriority(String path);
    
    /**
     * This class provides generic file utilities that might be used
     * throughout the stack
     */
    public static class Util
    {
        /**
         * Recursively delete the given file/directory and all subdirectories
         * 
         * @param f the file or directory to delete
         * @return the size in bytes of all deleted files
         */
        public static long deleteFiles(File f)
        {
            long freed = 0;
            if (f.isDirectory())
            {
                File[] files = f.listFiles();
                for (int i = 0; i < files.length; ++i)
                {
                    if (files[i].isDirectory())
                        freed += deleteFiles(files[i]);
                    else
                        freed += files[i].length();
                    if (files[i].exists() && !files[i].delete())
                    {
                        SystemEventUtil.logRecoverableError(new IOException("File deletion failed: " + files[i]));
                    }
                }
            }
            if (f.exists() && !f.delete())
            {
                SystemEventUtil.logRecoverableError(new IOException("Deletion failed: " + f));
            }
    
            return freed;
        }
    }
}
