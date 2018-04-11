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

import org.cablelabs.impl.io.OpenFile;

public class CachedOpenFile implements OpenFile
{
    public CachedOpenFile(String path, byte[] data)
    {
        this.data = data;
        this.path = path;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#available()
     */
    public synchronized int available() throws IOException
    {
        if (data == null) throw new IOException();

        return data.length - index;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#close()
     */
    public synchronized void close() throws IOException
    {
        // nothing really to do
        data = null;
        index = 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#getFilePointer()
     */
    public synchronized long getFilePointer() throws IOException
    {
        if (data == null) throw new IOException();

        return index;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#length()
     */
    public synchronized long length() throws IOException
    {
        if (data == null) throw new IOException();

        return data.length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#read()
     */
    public synchronized int read() throws IOException
    {
        if (data == null) throw new IOException();

        // did we already read past the end of the file
        if (index >= data.length) return -1;

        return data[index++];
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#read(byte[], int, int)
     */
    public synchronized int read(byte[] array, int offset, int length) throws IOException
    {
        // are we already closed?
        if (data == null) throw new IOException();

        if (array == null) throw new NullPointerException();

        // have we already read past the end of the file?
        if (index >= data.length) return -1;

        // return 0 if length is 0
        if (length == 0) return 0;

        // is length larger than the number of available bytes?
        if (length > available()) length = available();

        // copy length bytes into the destination array
        System.arraycopy(data, index, array, offset, length);
        // increment the array index
        index += length;

        return length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#seek(long)
     */
    public synchronized void seek(long pos) throws IOException
    {
        if (data == null) throw new IOException();

        if ((pos > data.length) || (pos < 0)) throw new IOException();

        index = (int) pos;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.cablelabs.impl.io.OpenFile#skip(long)
     */
    public synchronized long skip(long n) throws IOException
    {
        if (data == null) throw new IOException();

        // don't want to skip past the end of the array
        if (index + n > data.length)
        {
            n = data.length - index;
        }

        // don't want to skip past the front of the array
        if (index + n < 0)
        {
            n = -index;
        }
        index += n;
        return n;
    }

    public int getNativeFileHandle()
    {
        return 0;
    }

    String path;

    // file data
    byte data[];

    // current position in the data array
    int index;
}
