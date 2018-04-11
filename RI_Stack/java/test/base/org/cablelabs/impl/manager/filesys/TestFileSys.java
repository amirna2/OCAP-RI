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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.cert.X509Certificate;

import org.cablelabs.impl.io.AsyncLoadCallback;
import org.cablelabs.impl.io.AsyncLoadHandle;
import org.cablelabs.impl.io.FileData;
import org.cablelabs.impl.io.FileDataImpl;
import org.cablelabs.impl.io.FileSys;
import org.cablelabs.impl.io.OpenFile;

public class TestFileSys implements FileSys
{
    public final static String EXISTS_FILE = "testfile.txt";

    public final static String EXISTS_FILE2 = "testfile2.txt";

    public final static String EXISTS_DIR = "/test";

    public final static String EXISTS_CLASS = "testclass.class";

    public final static String EXISTS_CLASS2 = "testclass2.class";

    public final static byte[] FILE_DATA = new byte[] { (byte) 0x01, (byte) 0x23, (byte) 0x45, (byte) 0x67,
            (byte) 0x89, (byte) 0xab, (byte) 0xcd, (byte) 0xef };

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#exists(java.lang.String)
     */
    public boolean exists(String path)
    {
        if (EXISTS_FILE.equals(path) || EXISTS_CLASS.equals(path) || EXISTS_FILE2.equals(path)
                || EXISTS_CLASS2.equals(path))
            return true;
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#getFileData(java.lang.String)
     */
    public FileData getFileData(String path) throws IOException
    {
        if (EXISTS_FILE.equals(path) || EXISTS_CLASS.equals(path) || EXISTS_FILE2.equals(path)
                || EXISTS_CLASS2.equals(path))
        {
            FileDataImpl fd = new FileDataImpl(FILE_DATA);
            return fd;
        }
        else
            return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#getSigners(java.lang.String, boolean)
     */
    public X509Certificate[][] getSigners(String path, boolean checkRoot) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#getSigners(java.lang.String)
     */
    public X509Certificate[][] getSigners(String path)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#isDir(java.lang.String)
     */
    public boolean isDir(String path)
    {
        if (EXISTS_DIR.equals(path))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#length(java.lang.String)
     */
    public long length(String path)
    {
        return FILE_DATA.length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#list(java.lang.String)
     */
    public String[] list(String path)
    {
        if (EXISTS_DIR.equals(path))
        {
            return new String[] { "dir1", "dir2", "dir3" };
        }
        else
            return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#load(java.lang.String)
     */
    public FileSys load(String path, int loadMode)
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#open(java.lang.String)
     */
    public OpenFile open(String path) throws FileNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#unload()
     */
    public FileSys unload()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#canRead(java.lang.String)
     */
    public boolean canRead(String path)
    {
        if (EXISTS_FILE.equals(path) || EXISTS_CLASS.equals(path) || EXISTS_FILE2.equals(path)
                || EXISTS_CLASS2.equals(path))
            return true;
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#canWrite(java.lang.String)
     */
    public boolean canWrite(String path)
    {
        if (EXISTS_FILE.equals(path) || EXISTS_CLASS.equals(path) || EXISTS_FILE2.equals(path)
                || EXISTS_CLASS2.equals(path))
            return true;
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#create(java.lang.String)
     */
    public boolean create(String path)
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#delete(java.lang.String)
     */
    public boolean delete(String path)
    {
        if (EXISTS_FILE.equals(path) || EXISTS_CLASS.equals(path) || EXISTS_FILE2.equals(path)
                || EXISTS_CLASS2.equals(path))
            return true;
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#getCanonicalPath(java.lang.String)
     */
    public String getCanonicalPath(String path)
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#isFile(java.lang.String)
     */
    public boolean isFile(String path)
    {
        if (EXISTS_FILE.equals(path) || EXISTS_CLASS.equals(path) || EXISTS_FILE2.equals(path)
                || EXISTS_CLASS2.equals(path))
            return true;
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#lastModified(java.lang.String)
     */
    public long lastModified(String path)
    {
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#mkdir(java.lang.String)
     */
    public boolean mkdir(String path)
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#renameTo(java.lang.String,
     * java.lang.String)
     */
    public boolean renameTo(String fromPath, String toPath)
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#setLastModified(java.lang.String,
     * long)
     */
    public boolean setLastModified(String path, long time)
    {
        if (EXISTS_FILE.equals(path) || EXISTS_CLASS.equals(path) || EXISTS_FILE2.equals(path)
                || EXISTS_CLASS2.equals(path))
            return true;
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#setReadOnly(java.lang.String)
     */
    public boolean setReadOnly(String path)
    {
        if (EXISTS_FILE.equals(path) || EXISTS_CLASS.equals(path) || EXISTS_FILE2.equals(path)
                || EXISTS_CLASS2.equals(path))
            return true;
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#deleteOnExit(java.lang.String)
     */
    public boolean deleteOnExit(String path)
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.FileSys#asynchronousLoad(java.lang.String,
     * int, org.cablelabs.impl.io.AsyncLoadCallback)
     */
    public AsyncLoadHandle asynchronousLoad(String path, int loadMode, AsyncLoadCallback cb)
            throws FileNotFoundException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public String contentType(String path)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
