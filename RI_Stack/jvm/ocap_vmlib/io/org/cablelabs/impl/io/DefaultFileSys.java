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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Default implementation of the <code>FileSys</code> interface. Most methods
 * call directly to the native filesystem on the host.
 * 
 */
public class DefaultFileSys extends FileSysImpl
{

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#open(java.lang.String)
     * 
     * read-only access
     */
    public OpenFile open(String path) throws FileNotFoundException
    {
        return new DefaultOpenFile(path);
    }

    /**
     * Used for opening files with write access
     * 
     * @param path
     *            the path name of the file
     * @param mode
     *            specifies the read/write/append mode to use when opening. Can
     *            be one of ["r", "rw", "w", "wa"] where 'r' is read, 'w' is
     *            write, and 'a' is append
     * @return an <code>OpenFile</code> that can be used to access file data
     */
    public OpenFile open(String path, String mode) throws FileNotFoundException
    {
        return new DefaultOpenFile(path, mode);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#getFileData(java.lang.String)
     */
    public FileData getFileData(String path) throws IOException, FileNotFoundException
    {
        if (!(new File(path).exists())) throw new FileNotFoundException();

        OpenFile of = new DefaultOpenFile(path);

        // get the file length and create an array of bytes of that length
        int len = (int) of.length();
        byte array[] = new byte[len];
        int offset = 0;
        int read;

        try
        {
            // fully read the data
            while ((read = of.read(array, offset, len)) > 0 && len > 0)
            {
                offset += read;
                len -= read;
            }
        }
        finally
        {
            of.close();
        }
        FileData data = new FileDataImpl(array);
        return data;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#exists(java.lang.String)
     */
    public native boolean exists(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#isFile(java.lang.String)
     */
    public native boolean isFile(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#isDir(java.lang.String)
     */
    public native boolean isDir(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#list(java.lang.String)
     */
    public native String[] list(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#length(java.lang.String)
     */
    public native long length(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#canRead(java.lang.String)
     */
    public native boolean canRead(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#canWrite(java.lang.String)
     */
    public native boolean canWrite(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#create(java.lang.String)
     */
    public boolean create(String path) throws IOException
    {
        if (exists(path))
        {
            return false;
        }

        boolean ok = ncreate(path);

        if (ok)
        {
            try
            {
                FileSysManager.createMetadata(path, true);
            }
            catch (IOException e)
            {
                ndelete(path);
                ok = false;
                throw new IOException (e.getMessage());
            }
        }
        else
        {
            throw new IOException ("Could not create file");
        }

        return ok;
    }

    private native boolean ncreate(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#delete(java.lang.String)
     */
    public boolean delete(String path)
    {
        boolean ok = ndelete(path);

        if (ok)
        {
            ok = FileSysManager.deleteMetadata(path);
        }

        return ok;
    }

    private native boolean ndelete(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#getCanonicalPath(java.lang.String)
     */
    public String getCanonicalPath(String path)
    {
        try
        {
            return new File(path).getCanonicalPath();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#lastModified(java.lang.String)
     */
    public native long lastModified(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#mkdir(java.lang.String)
     */
    public boolean mkdir(String path)
    {
        boolean ok = nmkdir(path);

        if (ok)
        {
            try
            {
                FileSysManager.createMetadata(path, false);
            }
            catch (IOException e)
            {
                ndelete(path);
                ok = false;
            }
        }

        return ok;
    }

    private native boolean nmkdir(String path);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#renameTo(java.lang.String,
     * java.lang.String)
     */
    public boolean renameTo(String fromPath, String toPath)
    {
        boolean ok = nrenameTo(fromPath, toPath);

        if (ok && !FileSysManager.renameMetadata(fromPath, toPath))
        {
            nrenameTo(toPath, fromPath);
            ok = false;
        }

        return ok;
    }

    private native boolean nrenameTo(String fromPath, String toPath);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#setLastModified(java.lang.String,
     * long)
     */
    public native boolean setLastModified(String path, long time);

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#setReadOnly(java.lang.String)
     */
    public boolean setReadOnly(String path)
    {
        return FileSysManager.setReadOnly(path);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSysImpl#deleteOnExit(java.lang.String)
     */
    public native boolean deleteOnExit(String path);

    private static int asyncLoadCount = 1;
}
