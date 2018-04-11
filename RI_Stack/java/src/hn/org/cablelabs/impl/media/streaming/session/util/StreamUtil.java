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
package org.cablelabs.impl.media.streaming.session.util;


/**
 * @author Parthiban Balasubramanian
 * 
 */
public class StreamUtil
{

    public static final long SEGMENT_LENGTH = 3948;

    public static final long DTCP_PCP_HEADER_LENGTH = 14;

    private StreamUtil()
    {

    }

    /**
     * This method will return the encrypted size for a given clear text size
     * 
     * @param segmentClearSize
     *            - Cleartext size whose encrypted size has to be calculated.
     * @return Returns the encrypted size for the given clear text byte size.
     */
    public static long getDTCPEncryptedSize(long segmentClearSize)
    {
        long encryptedSegmentSize = segmentClearSize;
        final long NUM_FULL_PACKETS = segmentClearSize / SEGMENT_LENGTH;
        final long FULL_PACKET_PADDING = (16 - (SEGMENT_LENGTH % 16)) % 16;
        final long LAST_PACKET_SIZE = segmentClearSize % SEGMENT_LENGTH;
        final long LAST_PACKET_PADDING = (16 - (LAST_PACKET_SIZE % 16)) % 16;
        encryptedSegmentSize += (DTCP_PCP_HEADER_LENGTH + FULL_PACKET_PADDING) * NUM_FULL_PACKETS;
        if (LAST_PACKET_SIZE > 0)
        {
            encryptedSegmentSize += (DTCP_PCP_HEADER_LENGTH + LAST_PACKET_PADDING);
        }
        return encryptedSegmentSize;
    }

    /**
     * This method will return the encrypted byte position in the stream
     * corresponding to the clear text byte position.
     * 
     * @param clearStartBytePosition
     *            - Clear text byte position in the stream
     * @return - encrypted byte position for a corresponding clear text byte
     *         position.
     */
    public static long getDTCPEncryptedStartByte(long clearStartBytePosition)
    {
        long encryptedBytePosition = 0L;
        // Calculate the PCP packetization overhead.
        long NUM_FULL_PACKETS = clearStartBytePosition / SEGMENT_LENGTH;
        long FULL_PACKET_PADDING = (16 - (SEGMENT_LENGTH % 16)) % 16;
        long offset_in_pcp_packet = clearStartBytePosition % SEGMENT_LENGTH;

        encryptedBytePosition += ((14 + SEGMENT_LENGTH + FULL_PACKET_PADDING) * NUM_FULL_PACKETS);
        if (offset_in_pcp_packet != 0)
        {
            encryptedBytePosition += 14;
        }

        return encryptedBytePosition;
    }

}
