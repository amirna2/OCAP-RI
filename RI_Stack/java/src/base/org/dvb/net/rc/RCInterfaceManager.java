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

package org.dvb.net.rc;

import java.net.*;
import org.davic.resources.*;
import org.cablelabs.impl.manager.*;

/**
 * This class is the factory and manager for all return channel interfaces in
 * the system. The methods on this class which return instances of the
 * <code>RCInterface</code> will only return new instances of that class under
 * the following conditions:
 * <ul>
 * <li>on the first occasion an instance needs to be returned to a particular
 * application for a particular interface.
 * <li>when new return channel interfaces are added to the system
 * </ul>
 */
public class RCInterfaceManager implements org.davic.resources.ResourceServer
{
    /**
     * Call getInstance() to obtain the RCInterfaceManager. Do not use this
     * constructor directly.
     */
    protected RCInterfaceManager()
    {
    }

    /**
     * Factory method to obtain a manager. The RCInterfaceManager is either a
     * singleton for each MHP application or a singleton for the MHP terminal.
     *
     * @return an instance of an RCInterfaceManager
     */
    public static RCInterfaceManager getInstance()
    {
        NetManager nm = (NetManager) ManagerManager.getInstance(NetManager.class);
        return nm.getRCInterfaceManager();
    }

    /**
     * Factory method to return a list of all return channel interfaces visible
     * to this application. The number of entries in the array will exactly
     * match the number of return channel interfaces visible to the application.
     * Null is returned if no interfaces are visible to this application.
     *
     * @return an array of available return channel interfaces
     */
    public RCInterface[] getInterfaces()
    {
        // this method should not be directly called - the NetMgr subclassed
        // version should be called instead
        return null;
    }

    /**
     * Return the interface which will be used when connecting to a particular
     * host. Null is returned if this is not known when the method is called.
     *
     * @param addr
     *            the IP address of the host to connect to
     * @return the interface which will be used or null if this is not known
     */
    public RCInterface getInterface(InetAddress addr)
    {
        // this method should not be directly called - the NetMgr subclassed
        // version should be called instead
        return null;
    }

    /**
     * Return the interface which is used for a particular socket.
     *
     * @param s
     *            the socket to use
     * @return the interface which is used or null if the socket isn't connected
     */
    public RCInterface getInterface(Socket s)
    {
        // this method should not be directly called - the NetMgr subclassed
        // version should be called instead
        return null;
    }

    /**
     * Return the interface which is used for a particular URLConnection
     *
     * @param u
     *            the URLConnection to use
     * @return the interface which is used or null if the URLConnection isn't
     *         connected
     */
    public RCInterface getInterface(URLConnection u)
    {
        // this method should not be directly called - the NetMgr subclassed
        // version should be called instead
        return null;
    }

    /**
     * This method informs a resource server that a particular object should be
     * informed of changes in the state of the resources managed by that server.
     *
     * @param listener
     *            the object to be informed of state changes
     */
    public void addResourceStatusEventListener(ResourceStatusListener listener)
    {
        // Connection oriented return channels are not supported in OCAP
    }

    /**
     * This method informs a resource server that a particular object is no
     * longer interested in being informed about changes in state of resources
     * managed by that server. If the object had not registered its interest
     * initially then this method has no effect.
     *
     * @param listener
     *            the object which is no longer interested
     */
    public void removeResourceStatusEventListener(ResourceStatusListener listener)
    {
        // Connection oriented return channels are not supported in OCAP
    }
}
