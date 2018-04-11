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

package org.cablelabs.impl.manager.system;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p>
 * APDUWriter creates APDU byte arrays. It complements {@link APDUReader}.
 * </p>
 * 
 * <p>
 * The {@link putInt()} and {@link putBytes()} methods return the APDUWriter to
 * support method chaining. See {@link MMISystemModuleHandler} for a
 * demonstration of how to use this feature.
 * 
 * @author Spencer Schumann
 */
public class APDUWriter
{
    /**
     * List of the individual chunks of data that have been added to this APDU
     * and their total size in bytes.
     */
    private ArrayList data = new ArrayList();

    private int size = 0;

    /**
     * Add an integer value to the APDU. The value will be stored with the most
     * significant byte first, using the number of bytes specified by length.
     * 
     * @param x
     *            value to add
     * 
     * @param length
     *            number of bytes used to store the integer.
     * 
     * @return this (for method chaining)
     * 
     */
    public APDUWriter putInt(int x, int length)
    {
        byte[] chunk = new byte[length];
        int i;
        for (i = length - 1; i >= 0; i--)
        {
            chunk[i] = (byte) (x & 0xff);
            x >>>= 8;
        }
        return putBytes(chunk);
    }

    /**
     * Add a byte array to the APDU.
     * 
     * @param bytes
     *            data to add
     * 
     * @return this (for method chaining)
     * 
     */
    public APDUWriter putBytes(byte[] bytes)
    {
        size += bytes.length;
        data.add(bytes);
        return this;
    }

    /**
     * Construct a byte array representing the APDU that has been constructed
     * with the put methods.
     * 
     * @return byte array that can be passed to the
     *         {@link SystemModule#sendAPDU} method.
     * 
     */
    public byte[] getData()
    {
        byte[] dataBytes = new byte[size];
        int pos = 0;
        Iterator i = data.iterator();
        while (i.hasNext())
        {
            byte[] chunk = (byte[]) i.next();
            System.arraycopy(chunk, 0, dataBytes, pos, chunk.length);
            pos += chunk.length;
        }
        return dataBytes;
    }
}
