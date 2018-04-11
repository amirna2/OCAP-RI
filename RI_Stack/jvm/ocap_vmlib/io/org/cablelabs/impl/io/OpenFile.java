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
 * This interface represents an open file and is the base class for all open
 * file classes used to obtain file data. An instance of this interface is
 * returned from a call to <code>FileSys.open()</code>
 */
public interface OpenFile
{
    /**
     * Returns the native file handle for this open file. Many filesystems do
     * not support persistent open files (OC, HTTP, etc). For these filesystems,
     * 0 is always returned
     * 
     * @return the native file handle associated with this open file
     */
    public int getNativeFileHandle();

    /**
     * Read <code>length</code> bytes from the file into <code>array</code>.
     * 
     * @param array
     *            the buffer to read data into
     * @param offset
     *            the offset into the file to start at
     * @param length
     *            number of bytes to read
     * @return total number of bytes read, or -1 if EOF was reached
     * @throws IOException
     */
    public int read(byte[] array, int offset, int length) throws IOException;

    /**
     * reads a single byte of data from the file. Returns -1 if there is no data
     * left in the file
     * 
     * @return the next byte of data
     * @throws IOException
     */
    public int read() throws IOException;

    /**
     * returns the number of unread bytes in the file.
     * 
     * @return number of bytes available in the file.
     * @throws IOException
     */
    public int available() throws IOException;

    /**
     * skips over <code>n</code> bytes in the file.
     * 
     * @param n
     *            number of bytes to skip
     * @return the actual number of bytes skipped
     * @throws IOException
     */
    public long skip(long n) throws IOException;

    /**
     * close the <code>OpenFile</code> and release any allocated system
     * resources.
     * 
     * @throws IOException
     */
    public void close() throws IOException;

    /**
     * Return the total number of bytes in the file.
     * 
     * @return the size of the file in bytes.
     * @throws IOException
     */
    public long length() throws IOException;

    /**
     * Get the current byte location in the file.
     * 
     * @return the offset in the file where the next read will occur
     * @throws IOException
     */
    public long getFilePointer() throws IOException;

    /**
     * set the current location in the file.
     * 
     * @param pos
     *            the offset in the file where the next read will occur
     * @throws IOException
     */
    public void seek(long pos) throws IOException;
}
