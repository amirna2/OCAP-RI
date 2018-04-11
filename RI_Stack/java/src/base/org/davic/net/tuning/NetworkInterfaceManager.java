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
import org.davic.resources.ResourceServer;
import org.davic.resources.ResourceStatusListener;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.NetManager;

/**
 * A network interface manager is an object that keeps track of broadcast
 * network interfaces that are connected to the receiver.
 * <p>
 * There is only one instance of the network interface manager in a receiver and
 * this can be retrieved using the getInstance method.
 */

public class NetworkInterfaceManager implements ResourceServer
{

    /* For javadoc to hide the non-public constructor. */
    protected NetworkInterfaceManager()
    {
    }

    /**
     * Returns the instance of the NetworkInterfaceManager
     * 
     * @return network interface manager
     */
    public static NetworkInterfaceManager getInstance()
    {
        NetManager nm = (NetManager) ManagerManager.getInstance(NetManager.class);

        if (nm == null) return null;
        return nm.getNetworkInterfaceManager();
    }

    /**
     * Returns all network interfaces.
     * <p>
     * If there are no network interfaces, returns an array with the length of
     * zero.
     * 
     * @return an array containing all network interfaces
     */
    public NetworkInterface[] getNetworkInterfaces()
    {
        return null;
    }

    /**
     * Returns the NetworkInterface with which the specified TransportStream
     * object is associated. It neither tunes nor reserves the NetworkInterface.
     * 
     * @param ts
     *            Transport stream object
     * @return network interface that is associated with the transport stream
     */
    public NetworkInterface getNetworkInterface(TransportStream ts)
    {
        return null;
    }

    /**
     * Registers a resource status listener to receive resource status events
     * 
     * @param listener
     *            listener to be registered
     */
    public void addResourceStatusEventListener(ResourceStatusListener listener)
    {
    }

    /**
     * Removes the registration of a registered listener so that it will not
     * receive resource status events any more
     * 
     * @param listener
     *            listener whose registration is to be removed
     */
    public void removeResourceStatusEventListener(ResourceStatusListener listener)
    {
    }

}
