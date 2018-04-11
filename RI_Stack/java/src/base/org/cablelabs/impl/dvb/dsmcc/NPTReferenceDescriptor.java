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

import java.io.IOException;

/**
 * The NPTReferenceDescriptor class corresponds to the actual NPT Reference
 * Descriptor in a stream with a 1 to 1 relationship. This class is a container
 * for the data in the stream descriptor. For each of the parts of the
 * descriptor there is a get method. Beyond that there currently isn't any other
 * logic in this class.
 */
public class NPTReferenceDescriptor extends GenericDescriptor
{
    /**
     * @param data
     *            the data from the stream descriptor
     * @throws IOException
     */
    public NPTReferenceDescriptor(byte data[]) throws IOException
    {
        super(data, 0);
    }

    /**
     * @param data
     *            the data from the stream descriptor
     * @param offset
     *            the offset of bytes where descriptor begins
     */
    public NPTReferenceDescriptor(byte data[], int offset)
    {
        super(data, offset);
    }

    /**
     * @return the bytes containing the post discontiuity indicator
     */
    public byte getPostDiscontinuityIndicator()
    {
        return ((byte) (getByteAt(0) >>> 7));
    }

    /**
     * @return the content id for the timebase
     */
    public byte getContentId()
    {
        return ((byte) (getByteAt(0) & 0x7f));
    }

    /**
     * @return the stc time that relates to the npt reference
     */
    public long getSTCReference()
    {
        return (((long) (getByteAt(1) & 0x1)) << 32) + ((long) getIntAt(2));
    }

    /**
     * @return the npt time reference that relates to the stc
     */
    public long getNPTReference()
    {
        return getLongAt(6) & 0x00000001ffffffffL;
    }

    /**
     * @return the numerator part of the rate at which the npt is progressing
     */
    public int getScaleNumerator()
    {
        return getShortAt(14);
    }

    /**
     * @return the denominator part of the rate at which the npt is progressing
     */
    public int getScaleDenominator()
    {
        // Denominator is an unsigned 16 bit value, so we'll just extend it a
        // bit.
        return (getShortAt(16) & 0x0000ffff);
    }

    protected static short getExpectedTag()
    {
        return 23;
    }
}
