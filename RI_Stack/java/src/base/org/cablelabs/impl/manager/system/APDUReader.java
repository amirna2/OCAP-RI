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

/**
 * APDUReader reads data from cable card APDUs. It complements
 * {@link APDUWriter}.
 * 
 * @author Spencer Schumann
 */
public class APDUReader
{
    /**
     * APDU byte array that this reader reads from and the current read position
     * within the array.
     */
    private final byte[] data;

    private int pos;

    /**
     * Construct an APDUReader.
     * 
     * @param data
     *            APDU byte array from which to read.
     * 
     */
    public APDUReader(byte[] data)
    {
        if (null == data)
        {
            this.data = new byte[0];
        }
        else
        {
            this.data = data;
        }
        pos = 0;
    }

    /**
     * Exception class for attempts to read past the end of the apdu
     * 
     */
    public class APDUReadException extends Exception
    {
    }

    /**
     * Read an integer value and advance the current read position. The value is
     * stored with the most significant byte first, using the number of bytes
     * specified by length.
     * 
     * @param length
     *            number of bytes used to store the integer.
     * 
     * @return int value read from APDU.
     * 
     * @throws APDUReadException
     *             if reading past the end of the APDU.
     * 
     */
    public int getInt(int length) throws APDUReadException
    {
        int result = 0;
        int i;
        try
        {
            for (i = 0; i < length; i++)
            {
                result <<= 8;
                result += ((int) data[pos++]) & 0xFF;
            }
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new APDUReadException();
        }
        return result;
    }

    /**
     * Read a byte array from the APDU and advance the current read position.
     * 
     * @param length
     *            number of bytes to read
     * 
     * @return new byte array containing the requested bytes.
     * 
     * @throws java.lang.IndexOutOfBoundsException
     *             if reading past the end of the APDU.
     * 
     */
    public byte[] getBytes(int length) throws APDUReadException
    {
        byte[] array = new byte[length];
        try
        {
            System.arraycopy(data, pos, array, 0, length);
        }
        catch (IndexOutOfBoundsException e)
        {
            throw new APDUReadException();
        }
        pos += length;
        return array;
    }
}
