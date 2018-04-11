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

/**
 * <code>OpenFile</code> implementation for accessing files directly through the
 * native MPE APIs.
 * 
 */
public class DefaultOpenFile implements OpenFile
{
    public DefaultOpenFile(String path) throws FileNotFoundException
    {
        this(path, "r");
    }

    public DefaultOpenFile(String path, String mode) throws FileNotFoundException
    {
        if (path == null || path.length() == 0) throw new FileNotFoundException();

        boolean read = false;
        boolean write = false;
        boolean append = false;
        if (mode.indexOf('r') != -1) read = true;
        if (mode.indexOf('w') != -1) write = true;
        if (mode.indexOf('a') != -1) append = true;

        try
        {
            nativeFileHandle = open(path, read, write, append);
        }
        catch (IOException e)
        {
            throw new FileNotFoundException();
        }
    }

    private native int open(String path, boolean read, boolean write, boolean append) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#read(byte[], int, int)
     */
    public int read(byte[] array, int offset, int length) throws IOException
    {
        if (length == 0) return 0;
        return read(nativeFileHandle, array, offset, length);
    }

    private native int read(int handle, byte[] array, int offset, int length) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#read()
     */
    public int read() throws IOException
    {
        return read(nativeFileHandle);
    }

    private native int read(int handle) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#available()
     */
    public int available() throws IOException
    {
        return available(nativeFileHandle);
    }

    private native int available(int handle) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#skip(long)
     */
    public long skip(long n) throws IOException
    {
        return skip(nativeFileHandle, n);
    }

    private native long skip(int handle, long n) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#close()
     */
    public void close() throws IOException
    {
        close(nativeFileHandle);
    }

    private native void close(int handle) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#length()
     */
    public long length() throws IOException
    {
        return length(nativeFileHandle);
    }

    private native long length(int handle) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#getFilePointer()
     */
    public long getFilePointer() throws IOException
    {
        return getFilePointer(nativeFileHandle);
    }

    private native long getFilePointer(int handle) throws IOException;

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#seek(long)
     */
    public void seek(long pos) throws IOException
    {
        seek(nativeFileHandle, pos);
    }

    private native void seek(int handle, long pos) throws IOException;

    public int getNativeFileHandle()
    {
        return nativeFileHandle;
    }

    private int nativeFileHandle;
}
