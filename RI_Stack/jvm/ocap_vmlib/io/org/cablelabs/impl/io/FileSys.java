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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * This interface represents a file system used to retrieve file data.
 * 
 */
public interface FileSys
{
    /**
     * Opens the file referenced by <code>path</code> using authentication rules
     * for the java.io package. For some file system types, this may also read
     * all of the file contents into memory
     * 
     * @param path
     *            pathname of the file
     * @return an <code>OpenFile</code> instance for the file based on the file
     *         system type and authentication state of the file.
     * @throws FileNotFoundException
     *             if the file referenced by the pathname cannot be found or
     *             pathname refers to a directory.
     * @throws IOException
     *             if an I/O error occurred when reading the contents of the
     *             file.
     */
    public OpenFile open(String path) throws FileNotFoundException, IOException;

    /**
     * Get the complete contents of the file named by <code>path</code>. This
     * method does not perform any authentication.
     * 
     * @param path
     *            pathname of the file
     * @return FileData object containing the file data and file handle
     * @throws FileSysCommunicationException
     *             if a potentially recoverable communication error was
     *             encountered while trying to access a remote file system
     * @throws FileNotFoundException
     *             if the file referenced by the supplied pathname does not
     *             exist.
     * @throws IOException
     *             if an I/O error occurred when reading the contents of the
     *             file.
     */
    public FileData getFileData(String path) throws FileSysCommunicationException, FileNotFoundException, IOException;

    /**
     * Checks if the passed in path is a directory and returns <code>true</code>
     * if the pathname refers to a directory or <code>false</code> otherwise.
     * 
     * @param path
     *            file path to check
     * @return <code>true</code> if the path is a directory otherwise
     *         <code>false</code>
     */
    public boolean isDir(String path);

    /**
     * Lists the contents of a directory.
     * 
     * @param path
     *            the pathname of the directory
     * @return an array of String objects listing the files and directories
     *         contained in the directory referred to by pathname. The array
     *         will be empty if the directory is empty. <code>null</code> if the
     *         named directory does not exist or an I/O error occurs.
     */
    public String[] list(String path);

    /**
     * Checks if the file referenced by pathname exists in the file system. If
     * the directory information is not loaded, then implies a synchronous load
     * for broadcast carousels per MHP sec 11.5.1.1.
     * 
     * @param path
     *            pathname of the file to check.
     * @return <code>true</code> if the file exists, <code>false</code>
     *         otherwise.
     */
    public boolean exists(String path);

    /**
     * Returns the length of the file referenced by the pathname.
     * 
     * @param path
     *            pathname of the file to get the length
     * @return the length in bytes or <code>0</code> if the file does not exist
     */
    public long length(String path);

    /**
     * Tests whether the caller can read the file denoted by the abstract
     * pathname.
     * 
     * @param path
     *            abstract pathname pointing to the file to test.
     * @return <code>true</code> if the file can be read, otherwise
     *         <code>false</code>.
     */
    public boolean canRead(String path);

    /**
     * Tests whether the caller can modify the file denoted by the supplied
     * abstract pathname.
     * 
     * @param path
     *            abstract pathname pointing to the file to test.
     * @return <code>true</code> if the file can be written, otherwise
     *         <code>false</code>.
     */
    public boolean canWrite(String path);

    /**
     * Deletes the file or directory denoted by the supplied abstract pathname.
     * If the pathname denotes a directory, then the directory must be empty to
     * be deleted.
     * 
     * @param path
     *            abstract pathname pointing to the file to delete.
     * @return <code>true</code> if the file can be deleted, otherwise
     *         <code>false</code>.
     */
    public boolean delete(String path);

    /**
     * Returns the canonical form of the supplied abstract pathname.
     * 
     * @param path
     *            abstract pathname to canonicalize.
     * @return the canonical form of the supplied path.
     */
    public String getCanonicalPath(String path);

    /**
     * Tests whether the file denoted by the supplied abstract pathname is a
     * normal file.
     * 
     * @param path
     *            abstract pathname pointing to the file to test.
     * @return <code>true</code> if the file exists and is a normal file,
     *         otherwise <code>false</code>.
     */
    public boolean isFile(String path);

    /**
     * Returns the time that the file denoted by the supplied abstract pathname
     * was last modified.
     * 
     * @param path
     *            abstract pathname of the file to get modified time.
     * @return value representing the time the file was last modified in
     *         milliseconds.
     */
    public long lastModified(String path);

    /**
     * Creates the directory named by the supplied abstract pathname.
     * 
     * @param path
     *            abstract pathname of the directory to create.
     * @return <code>true</code> if the directory was created, otherwise
     *         <code>false</code>.
     */
    public boolean mkdir(String path);

    /**
     * Renames the file denoted by the abstract pathname to the new pathname.
     * 
     * @param fromPath
     *            file to rename
     * @param toPath
     *            new name for the target file
     * @return <code>true</code> if the rename succeeded, otherwise
     *         <code>false</code>.
     */
    public boolean renameTo(String fromPath, String toPath);

    /**
     * Creates a new file pointed to by the supplied abstract pathname.
     * 
     * @param path
     *            abstract pathname to the file to be created.
     * @return <code>true</code> if the file was created successfully, otherwise
     *         <code>false</code>.
     */
    public boolean create(String path) throws IOException;

    /**
     * Sets the last-modified time of the file or directory named by the
     * supplied abstract pathname
     * 
     * @param path
     * @param time
     * @return <code>true</code> if the last-modified time was changed
     *         successfully.
     */
    public boolean setLastModified(String path, long time);

    /**
     * Marks the file or directory named by the supplied abstract pathname so
     * that only read operations are allowed.
     * 
     * @param path
     *            abstract pathname to the file or directory to modify
     * @return <code>true</code> if the file was marked as read-only, otherwise
     *         <code>false</code>.
     */
    public boolean setReadOnly(String path);

    /**
     * Requests that the file or directory denoted by this abstract pathname be
     * deleted when the virtual machine terminates.
     * 
     * @param path
     *            abstract pathname of the file to delete.
     * @return true if the operation succeeded
     */
    public boolean deleteOnExit(String path);

    /**
     * Return the MIME type of a file, if available.
     * 
     * @param path
     *            abstract pathname of the file to check.
     * 
     * @return A string containing the MIME type of the file, if specified and
     *         available. Null if not available.
     */
    public String contentType(String path);
}
