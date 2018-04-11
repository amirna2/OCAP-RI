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

package javax.tv.net;

import java.net.InetAddress;
import javax.tv.locator.*;
import java.io.IOException;

// TODO(Todd): Should this class be present in an OCAP implementation?

/**
 * Class <code>InterfaceMap</code> reports the local IP address assigned to a
 * given service component that carries IP data. Applications may use the
 * returned IP address to specify the network interface to which an instance of
 * <code>java.net.DatagramSocket</code> or <code>java.net.MulticastSocket</code>
 * should bind.
 * 
 * @see java.net.DatagramSocket#DatagramSocket(int, java.net.InetAddress)
 *      java.net.DatagramSocket(int, java.net.InetAddress)
 * 
 * @see java.net.MulticastSocket#setInterface(java.net.InetAddress)
 *      java.net.MulticastSocket.setInterface(java.net.InetAddress)
 * 
 * @author Jon Courtney courtney@eng.sun.com
 */
public class InterfaceMap
{
    private InterfaceMap()
    {
    }

    /**
     * Reports the local IP address assigned to the given service component.
     * 
     * @param locator
     *            The service component for which the local IP address mapping
     *            is required.
     * 
     * @return The IP address assigned to this service component.
     * 
     * @throws InvalidLocatorException
     *             If the given locator does not refer to a valid source of IP
     *             data, or if this system does not support the reception of
     *             broadcast IP data.
     * 
     * @throws IOException
     *             If a local IP address is not available to be assigned to the
     *             source of IP data.
     * 
     */
    public static InetAddress getLocalAddress(Locator locator) throws InvalidLocatorException, IOException
    {
        // first check for null parameters
        if (locator == null) throw new NullPointerException("null Locator parameter not supported");

        // IP on the broadcast channel is not supported.
        throw new InvalidLocatorException(locator, "Not a source of IP data");
    }
}
