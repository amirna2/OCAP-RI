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
 * This PodAPDU class will parse an standard APDU and separate it into
 * its "tag" and "data" components.  It handles the work of parsing
 * the ASN.1 BER length field.
 */
public class PodAPDU
{
    private int m_sessionID;
    private int m_tag;
    private int m_length;
    
    private int m_dataOffset;
    private byte[] m_data;
    
    /**
     * Parse the given APDU.
     * 
     * @param apdu 
     * @throws ArrayIndexOutOfBoundsException if there was an error
     *         parsing the data
     */
    public PodAPDU(int sessionID, byte[] apdu)
    {
        m_sessionID = sessionID;
        m_data = apdu;
        
        // Parse APDU tag
        m_tag = ((apdu[0] & 0xFF) << 16) | ((apdu[1] & 0xFF) << 8) | ((apdu[2] & 0xFF));
            
        // Decode the length field. Adjust the
        // data bytes offset accordingly.
        m_dataOffset = 4;
        m_length = apdu[3] & 0xFF;
        if ((apdu[3] & 0x80) != 0)
        {
            // Decode ASN.1 BER length field.
            m_length = apdu[4] & 0xFF; // Get first byte of length.
            ++m_dataOffset;
            for (int i = 1; i < (apdu[3] & 0x7F); ++i)
            {
                m_length <<= 8;
                m_length |= (apdu[4 + i] & 0xFF);
                ++m_dataOffset;
            }
        }
    }
    
    /**
     * Return the 24-bit APDU tag
     * 
     * @return apdu tag
     */
    public int getTag()
    {
        return m_tag;
    }
    
    /**
     * Return the APDU data starting with the first byte after the
     * APDU length_field().  Modifications to the returned data array
     * will not affect the data stored in this APDU object
     * 
     * @return the APDU data
     */
    public byte[] getData()
    {
        byte[] retData = new byte[m_length];
        System.arraycopy(m_data, m_dataOffset, retData, 0, m_length);
        return retData;
    }
    
    /**
     * Returns the full APDU including the tag and length_field().
     * Modifications to the returned data array will not affect the
     * data stored in this APDU object
     * 
     * @return the full APDU byte array
     */
    public byte[] getAPDU()
    {
        byte[] retData = new byte[m_data.length];
        System.arraycopy(m_data, 0, retData, 0, m_data.length);
        return retData;
    }
    
    /**
     * Returns the length of this APDU as reported by the length_field()
     * field of the APDU
     * 
     * @return the APDU data length
     */
    public int getLength()
    {
        return m_length;
    }

    /**
     * Returns the resource session associated with this APDU
     * 
     * @return the resource session ID
     */
    public int getSessionID()
    {
        return m_sessionID;
    }
}
