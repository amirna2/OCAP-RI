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

package org.davic.net.tuning;

import org.davic.mpeg.TransportStream;

/**
 * Objects of this class represent physical network interfaces that can be used
 * for receiving broadcast transport streams.
 */
public class NetworkInterface
{

    /**
     * This constructor is provided for the use of implementations and
     * specifications which extend this specification. Applications shall not
     * define sub-classes of this class. Implementations are not required to
     * behave correctly if any such application defined sub-classes are used.
     */
    protected NetworkInterface()
    {
    }

    /**
     * Returns the transport stream to which the network Interface is currently
     * tuned. Returns null if the network interface is not currently tuned to a
     * transport stream, e.g. because it is performing a tune action.
     * 
     * @return Transport stream to which the network interface is currently
     *         tuned
     */
    public TransportStream getCurrentTransportStream()
    {
        return null;
    }

    /**
     * Returns the Locator of the transport stream to which the network
     * interface is connected. Returns null if the network interface is not
     * currently tuned to a transport stream.
     * 
     * @return Locator of the transport stream to which the network interface is
     *         tuned
     */
    public org.davic.net.Locator getLocator()
    {
        return null;
    }

    /**
     * @return true, if the network interface is reserved, otherwise false
     */
    public synchronized boolean isReserved()
    {
        return false;
    }

    /**
     * @return true, if the network interface is local (i.e. embedded in the
     *         receiver), otherwise false
     */
    public boolean isLocal()
    {
        return false;
    }

    /**
     * Lists the known transport streams that are accessible through this
     * network interface. If there are no such streams, returns an array with
     * length of zero.
     * 
     * @return array of transport streams accassible through this network
     *         interface
     */
    public TransportStream[] listAccessibleTransportStreams()
    {
        return null;
    }

    /**
     * This method returns the type of the delivery system that this network
     * interface is connected to.
     * 
     * @return delivery system type
     */
    public int getDeliverySystemType()
    {
        return 0;
    }

    /**
     * Adds a listener for network interface events
     * 
     * @param listener
     *            listener object to be registered to receive network interface
     *            events
     */
    public void addNetworkInterfaceListener(NetworkInterfaceListener listener)
    {
    }

    /**
     * Removes a registered listener
     * 
     * @param listener
     *            listener object to be removed so that it will not receive
     *            network interface events in future
     */
    public void removeNetworkInterfaceListener(NetworkInterfaceListener listener)
    {
    }
}
