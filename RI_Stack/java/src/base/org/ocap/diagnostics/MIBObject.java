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

package org.ocap.diagnostics;

/**
 * The interface represents a MIB Object. It contains the oid, as well as the
 * encoding of the object with value formats corresponding to the ASN.1
 * definition of the object.
 */
public class MIBObject
{
    private String m_oid = null;

    private byte[] m_data = null;

    /**
     * Constructs a MIB object.
     * 
     * @param oid
     *            Object Identifier of the MIB object.
     * @param data
     *            Array of bytes representing the MIB encoding.
     */
    public MIBObject(String oid, byte[] data)
    {
        m_oid = oid; // save oid

        m_data = data; // save data
    }

    /**
     * Gets the MIB object identifier.
     * 
     * @return Object identifier of this MIB object. The object ID SHALL be
     *         formatted as per RFC 1778 section 2.15.
     */
    public String getOID()
    {
        return m_oid;
    }

    /**
     * Gets the current MIB object encoding in byte array form. The array is
     * formatted according to the ASN.1 format of the MIB.
     * 
     * @return A byte array representing the MIB object encoding.
     */
    public byte[] getData()
    {
        return m_data;
    }

   /**
    * Returns the unencoded value of the current MIB object data.  The 
    * returned array is the value field of the BER type-length-value encoding
    * provided by {@link #getData()}.
    *
    * @return A byte array representing the unencoded value of the MIB
    *         object data.
    */
    public byte[] getValue() 
    { 
        int arrayPosition = 0;

        // WARNING: we're only handling the first byte as a tag
        byte tag = m_data[arrayPosition++];

        // get the length
        int len = m_data[arrayPosition++];

        //  is the length multi-byte?
        if ((len & 0x80) == 0x80)
        {
            len &= 0x7f;    // yes - strip off the flag bit

            if ((len > 0) && (len < 4))
            {
                int temp = 0;

                for (int i = 0; i < len; i++)
                {
                    temp = (temp << 8) + (m_data[arrayPosition++] & 0xff);
                }

                len = temp;
            }
            else
            {
                return null;
            }
        }

        if (len > 0)
        {
            // we have the tag and the length...
            // copy the output to the new byte[]
            byte retVal[] = new byte[len];

            for (int i = 0; i < len; i++)
            {
                retVal[i] = m_data[arrayPosition++];
            }

            return retVal;
        }

        return null;
    }
}
