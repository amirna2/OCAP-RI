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

package org.cablelabs.impl.dvb.dsmcc;

import org.ocap.si.Descriptor;

/**
 * @author Eric Koldinger
 */
public class GenericDescriptor extends Descriptor
{
    protected short m_tag;

    protected short m_length = 256; // set to really long at first.

    protected byte m_data[];

    protected int m_offset;

    public GenericDescriptor(byte data[], int offset)
    {
        m_data = data;
        m_tag = data[offset];
        m_length = data[offset + 1];
        m_offset = offset + 2; // Skip the first two bytes, tag and length.
    }

    public int getDescriptorLength()
    {
        return m_length + 2;
    }

    public short getTag()
    {
        return m_tag;
    }

    public short getContentLength()
    {
        return m_length;
    }

    public byte[] getContent()
    {
        byte[] content = new byte[m_length];
        System.arraycopy(m_data, m_offset, content, 0, m_length);
        return content;
    }

    public final byte getByteAt(int index)
    {
        return m_data[index + m_offset];
    }

    public final short getShortAt(int index) throws IndexOutOfBoundsException
    {
        index += m_offset;
        return (short) (((m_data[index] & 0xff) << 8) + ((m_data[index + 1] & 0xff)));
    }

    public final int getIntAt(int index) throws IndexOutOfBoundsException
    {
        index += m_offset;
        return ((m_data[index] & 0xff) << 24) + ((m_data[index + 1] & 0xff) << 16) + ((m_data[index + 2] & 0xff) << 8)
                + (m_data[index + 3] & 0xff);
    }

    public final long getLongAt(int index) throws IndexOutOfBoundsException
    {
        index += m_offset;
        return ((m_data[index] & 0xffL) << 56) + ((m_data[index + 1] & 0xffL) << 48)
                + ((m_data[index + 2] & 0xffL) << 40) + ((m_data[index + 3] & 0xffL) << 32)
                + ((m_data[index + 4] & 0xffL) << 24) + ((m_data[index + 5] & 0xffL) << 16)
                + ((m_data[index + 6] & 0xffL) << 8) + (m_data[index + 7] & 0xffL);
    }
}
