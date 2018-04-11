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

import org.ocap.si.Descriptor;

/**
 * This class represents an MPEG-2 descriptor.
 */
public class DescriptorImpl extends Descriptor
{
    private byte content[] = null;

    private short desc_tag = -1;
    private final int DESCRIPTOR_HEADER_SIZE = 2;

    public DescriptorImpl(short descTag, byte data[])
    {
        this.content = data;
        this.desc_tag = descTag;
    }

    /**
     * Get the descriptor_tag field. Eight bit field that identifies each
     * descriptor. The range of valid MPEG-2 descriptor tag values is 0x2
     * through 0xFF.
     * 
     * @return The descriptor tag.
     */
    public short getTag()
    {
        return desc_tag;
    }

    /**
     * Get the descriptor_length field. Eight bit field specifying the number of
     * bytes of the descriptor immediately following the descriptor_length
     * field.
     * 
     * @return The descriptor length.
     */
    public short getContentLength()
    {
        return (short) content.length;
    }

    /**
     * Get the data contained within this descriptor. The data is returned in an
     * array of bytes and consists of the data immediately following the
     * descriptor_length field with length indicated by that field.
     * 
     * @return The descriptor data.
     */
    public byte[] getContent()
    {
        return content;
    }

    /**
     * Get a particular byte within the descriptor content. The data is returned
     * in a byte which is located at the position specified by an index
     * parameter in the data content immediately following the descriptor_length
     * field.
     * 
     * @param index
     *            An index to the descriptor content. Value 0 corresponds to the
     *            first byte after the length field.
     * 
     * @return The required byte data.
     * 
     * @throws IndexOutOfBoundsException
     *             if index < 0 or index >= ContentLength
     */
    public byte getByteAt(int index) throws IndexOutOfBoundsException
    {
        if (index < content.length)
            return content[index];
        else
            throw new IndexOutOfBoundsException("Array index out of Bound, Length of the Descriptor content = "
                    + content.length);
    }

    /**
     * Get raw descriptor data include header.
     *
     * @return byte array with raw descriptor data.
     */
    public byte[] getRawContent() {
        byte[] raw = new byte[DESCRIPTOR_HEADER_SIZE + getContentLength()];
        raw[0] = (byte) getTag();
        raw[1] = (byte) getContentLength();
        System.arraycopy(getContent(), 0, raw, DESCRIPTOR_HEADER_SIZE, getContentLength());
        return raw;
    }
}
