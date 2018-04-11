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

package org.cablelabs.impl.ocap.si;

/**
 * ByteParser.java
 */
public class ByteParser
{
    private static boolean isBE = false;

    static
    {
        isBE = nativeIsBigEndian();
    }

    /*
     * this class is a static utility so don't let it be instanciated
     */
    ByteParser()
    {
    }

    /**
     * <code>getIntFromBytes</code> utility method to create an int from a
     * section of the byte array
     * 
     * @param bytes
     *            a <code>byte[]</code> value
     * @param counter
     *            an <code>Integer</code> value which will be incremented
     *            appropriately once the int has been created
     * @return an <code>int</code> value
     */
    public static int getInt(byte[] bytes, int offset)
    {
        return ByteParser.getInt(new ByteArrayWrapper(bytes, offset));
    }

    public static int getInt(ByteArrayWrapper wrapper)
    {
        byte[] bytes = wrapper.getBytes();
        int offset = wrapper.getIndex();
        int value = 0; // 32 bits, 4 bytes
        if (!isBigEndian())
        {
            // Little endian so need to swap bytes, 0->LSB, 3->MSB
            value = (bytes[offset + 0] & 0xff) + ((bytes[offset + 1] & 0xff) << 8) + ((bytes[offset + 2] & 0xff) << 16)
                    + ((bytes[offset + 3] & 0xff) << 24);
        }
        else
        {
            // Big endian no need to swap bytes, 0->MSB, 3->LSB
            value = (bytes[offset + 3] & 0xff) + ((bytes[offset + 2] & 0xff) << 8) + ((bytes[offset + 1] & 0xff) << 16)
                    + ((bytes[offset + 0] & 0xff) << 24);
        }
        // increment counter for calling application
        wrapper.setIndex(wrapper.getIndex() + 4);
        return value;
    }

    public static short getShort(ByteArrayWrapper wrapper)
    {
        byte[] bytes = wrapper.getBytes();
        int offset = wrapper.getIndex();
        short value = 0; // 16 bits, 2 bytes
        if (!isBigEndian())
        {
            // Little endian so need to swap bytes, 0->LSB, 1->MSB
            value = (short) ((bytes[offset + 0] & 0xff) + ((bytes[offset + 1] & 0xff) << 8));
        }
        else
        {
            // Big endian no need to swap bytes, 1->LSB, 0->MSB
            value = (short) ((bytes[offset + 1] & 0xff) + ((bytes[offset + 0] & 0xff) << 8));
        }
        // increment counter for calling application
        wrapper.setIndex(wrapper.getIndex() + 2);
        return value;
    }

    public static byte getByte(ByteArrayWrapper wrapper)
    {
        byte[] bytes = wrapper.getBytes();
        int offset = wrapper.getIndex();
        // increment counter for calling application
        wrapper.setIndex(wrapper.getIndex() + 1);
        return bytes[offset];
    }

    public static byte[] getByteArray(ByteArrayWrapper wrapper, int size)
    {
        byte[] bytes = wrapper.getBytes();
        int offset = wrapper.getIndex();
        byte[] someBytes = new byte[size];
        System.arraycopy(bytes, offset, someBytes, 0, size);

        // increment counter for calling application
        wrapper.setIndex(wrapper.getIndex() + size);
        return someBytes;
    }

    /**
     * Describe <code>getString</code> method here.
     * 
     * @param bytes
     *            a <code>byte[]</code> value
     * @param counter
     *            an <code>Integer</code> value which will be incremented
     *            appropriately once the string has been created
     * @return a <code>String</code> value
     */
    public static String getString(byte[] bytes, int offset)
    {
        return ByteParser.getString(new ByteArrayWrapper(bytes, offset));
    }

    public static String getString(ByteArrayWrapper wrapper)
    {
        int start = wrapper.getIndex();
        int count = wrapper.getIndex();
        byte[] bytes = wrapper.getBytes();
        while (bytes[count] != 0)
        {
            count++;
        }
        count++;
        String name = new String(bytes, start, (count - start));
        // increment counter for calling application
        wrapper.setIndex(count);
        return name;
    }

    public static String getString(ByteArrayWrapper wrapper, int size)
    {
        int start = wrapper.getIndex();
        byte[] bytes = wrapper.getBytes();
        String name = new String(bytes, start, size);
        // increment counter for calling application
        wrapper.setIndex(start + size);
        return name;
    }

    /**
     * <code>isBigEndian</code> is a utility method to determine if the system
     * is bigEndian or littleEndian
     * 
     * @return a <code>boolean</code> value
     */
    public static boolean isBigEndian()
    {
        return isBE;
    }

    private static native boolean nativeIsBigEndian();

    static
    {
        org.cablelabs.impl.ocap.OcapMain.loadLibrary();
    }
}
