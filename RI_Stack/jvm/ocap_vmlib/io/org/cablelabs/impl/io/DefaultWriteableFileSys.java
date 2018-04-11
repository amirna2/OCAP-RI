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
 * The <code>WriteableFileSys</code> class supports direct access to the native
 * "write" output support.
 */
public class DefaultWriteableFileSys implements WriteableFileSys
{
    public DefaultWriteableFileSys()
    {

    }

    public void write(int nativeHandle, byte[] buf, int off, int len) throws IOException, StorageMediaFullException
    {
        nativeWrite(nativeHandle, buf, off, len);
    }

    /**
     * Native write method.
     * 
     * @param fd
     * @param buf
     * @param off
     * @param len
     * @throws IOException
     * @throws StorageMediaFullException
     */
    private native void nativeWrite(int nativeHandle, byte[] buf, int off, int len) throws IOException,
            StorageMediaFullException;

    /**
     * Native set length of writeable file method.
     * 
     * @param fd
     * @param length
     * @throws IOException
     * @throws StorageMediaFullException
     */
    public native void setLength(int nativeHandle, long length) throws IOException, StorageMediaFullException;

    /**
     * Native set length of writeable file method.
     * 
     * @param fd
     * @param length
     *            is the desired length of the file
     * @param current
     *            is the current length of the file
     * @throws IOException
     * @throws StorageMediaFullException
     */
    public void setLength(int nativeHandle, long length, long current) throws IOException, StorageMediaFullException
    {
        setLength(nativeHandle, length);
    }

    /***
     * The following two methods are not intercepted from their normal location
     * within RandomAccessFile. They are provide here for convenient access to
     * the associated native methods for the <code>PersistentFileSys</code>.
     */
    public native void seek(int nativeHandle, long pos) throws IOException;

    public native long getFilePointer(int nativeHandle) throws IOException;

    /***
     * The following method is used to simply set the MPE_FS_ERROR_DEVFULL error
     * code value in the native jni for this class. This is done dynamically
     * since the native libraries are not compiled against MPE includes.
     */
    public native void setDevFullErrVal(int err);
}
