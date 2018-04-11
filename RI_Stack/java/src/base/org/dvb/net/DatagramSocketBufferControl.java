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

package org.dvb.net;

import java.net.*;
import org.cablelabs.impl.manager.*;

/**
 * This class provides additional control over buffering for
 * <code>DatagramSocket</code>s.
 */
public class DatagramSocketBufferControl
{
    /**
     * Non-public constructor to stop javadoc generating a public one
     */
    DatagramSocketBufferControl()
    {
    }

    /**
     * Sets the SO_RCVBUF option to the specified value for this DatagramSocket.
     * The SO_RCVBUF option is used by the platform's networking code as a hint
     * for the size to use when allocating the underlying network I/O buffers.
     * <p>
     * 
     * Increasing buffer size can increase the performance of network I/O for
     * high-volume connection, while decreasing it can help reduce the backlog
     * of incoming data. For UDP, this sets the buffer size for received
     * packets.
     * <p>
     * 
     * Because SO_RCVBUF is a hint, applications that want to verify what size
     * the buffers were set to should call getReceiveBufferSize.
     * 
     * This method shall throw IllegalArgumentException - if size is 0 or is
     * negative.
     * 
     * @param d
     *            The DatagramSocket for which to change the receive buffer
     *            size.
     * 
     * @param size
     *            The requested size of the receive buffer, in bytes.
     * 
     * @throws SocketException
     *             - If there is an error when setting the SO_RCVBUF option.
     * 
     */
    public static void setReceiveBufferSize(DatagramSocket d, int size) throws java.net.SocketException
    {
        NetManager nm = (NetManager) ManagerManager.getInstance(NetManager.class);
        nm.setReceiveBufferSize(d, size);
    }

    /**
     * 
     * Get value of the SO_RCVBUF option for this socket, that is the buffer
     * size used by the platform for input on the this Socket. The value
     * returned need not be the value previously set by setReceiveBufferSize (if
     * any).
     * 
     * @param d
     *            The DatagramSocket for which to query the receive buffer size.
     * 
     * @return The size of the receive buffer, in bytes.
     * 
     * @throws SocketException
     *             - If there is an error when querying the SO_RCVBUF option.
     */
    public static int getReceiveBufferSize(DatagramSocket d) throws java.net.SocketException
    {
        NetManager nm = (NetManager) ManagerManager.getInstance(NetManager.class);
        return nm.getReceiveBufferSize(d);
    }
}
