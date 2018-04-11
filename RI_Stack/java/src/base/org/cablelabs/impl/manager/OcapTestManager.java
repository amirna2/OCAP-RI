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

package org.cablelabs.impl.manager;

import java.io.IOException;

/**
 * Provides the implementation for Public Test facilities in the OCAP stack.
 * 
 * @author Brent Thompson
 */
public interface OcapTestManager extends Manager
{

    /**
     * Implementation for {@link org.ocap.OcapSystem.monitorConfiguredSignal}.
     * <p>
     * This operation should be synchronized so that only 1 thread can try to
     * setup the OCAP test networking connection at a time.
     * 
     * @param port
     *            the IP port number to listen for datagrams from the test
     *            system on.
     * 
     * @param timeout
     *            the time, in seconds to allow for a communications channel to
     *            be established with the test system.
     * 
     * @throws IOException
     *             if a communications channel cannot be established with the
     *             test system within the amount of time specified by the
     *             <code>timeout</code> parameter.
     */
    public void setup(int port, int timeout) throws IOException;

    /**
     * Implementation for {@link org.ocap.test.OCAPTest#send()}.
     * <p>
     * This operation should be synchronized so that only 1 thread can try to
     * send OCAP debug messages at a time.
     * 
     * @param rawMessage
     *            a byte array of the raw message to be sent to ATE via TCP/IP
     *            protocol.
     * 
     * @throws IllegalArgumentException
     *             If rawMessage contains a byte with the value
     *             MESSAGE_TERMINATION_BYTE.
     * 
     * @throws IOException
     *             If there is any problem with I/O operations or an interaction
     *             channel has not been initialized.
     */
    public void send(byte[] rawMessage) throws IOException;

    /**
     * Implementation for {@link org.ocap.test.OCAPTest#receive()}.
     * <p>
     * This method should be synchronized so that only 1 thread can try to
     * receive OCAP debug messages at a time.
     * 
     * @return a byte array coming from ATE via TCP/IP protocol. The termination
     *         character is not included.
     * 
     * @throws IOException
     *             If there is any problem with I/O operations or an interaction
     *             channel has not been initialized.
     */
    public byte[] receive() throws IOException;

    /**
     * Implementation for {@link org.ocap.test.OCAPTest#getProtocol()}.
     * 
     * @return {@link OCAPTest#UDP} or {@link OCAPTest#TCP}
     */
    public int getProtocol();

    /**
     * Implementation for {@link org.ocap.test.OCAPTest#receiveUDP}.
     * <p>
     * This operation should be synchronized so that only 1 thread can try to
     * receive OCAP debug messages at a time.
     * 
     * @return byte a payload bytes in a UDP packet. The byte length must be
     *         less than a max length limited by the interaction channel and it
     *         is responsibility of a caller of this method to do so.
     * 
     * @throws IOException
     *             If there is any problem with I/O operations or an interaction
     *             channel has not been initialized.
     */
    public byte[] receiveUDP() throws IOException;

    /**
     * Implementation for {@link org.ocap.test.OCAPTest#sendUDP}.
     * <p>
     * This operation should be synchronized so that only 1 thread can try to
     * send OCAP debug messages at a time.
     * 
     * @param rawMessage
     *            byte data to be sent. The byte length must be less than a max
     *            length limited by the interaction channel and it is
     *            responsibility of a caller of this method to do so.
     * 
     * @throws IOException
     *             If there is any problem with I/O operations or an interaction
     *             channel has not been initialized.
     */
    public void sendUDP(byte[] rawMessage) throws IOException;
}
