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

package org.ocap.hn;

import java.net.InetAddress;
import org.cablelabs.impl.ocap.hn.NetworkInterfaceImpl;

// NOTE: This is treated as an abstract base class in the implementation. 
//       i.e. this class is not directly instantiated by the implementation

/**
 * This class represents a home network interface including MoCA, wired
 * ethernet, and wireless ethernet.  Reverse channel interfaces are not
 * represented by objects of this class. For each wired ethernet, wireless
 * ethernet, MoCA interface, or interface that is not a reverse channel
 * interface the HNIMP SHALL create an instance of this class.
 *
 */
public class NetworkInterface
{
    /**
     * Unknown network type.
     */
    public final static int UNKNOWN = 0;

    /**
     * Network interface type for hard-wired and MoCA based.
     */
    public final static int MOCA = 1;

    /**
     * Network interface type for hard-wired and ethernet based.
     */
    public final static int WIRED_ETHERNET = 2;

    /**
     * Network interface type for wireless and ethernet based.
     */
    public final static int WIRELESS_ETHERNET = 3;
    
    /**
     * Protected constructor.
     */
    protected NetworkInterface() {}

    /**
     * Gets an array of <code>NetworkInterface</code> instances that represent
     * all of the network interfaces supported by the device.
     *
     * @return An array of NetworkInterface instances.
     */
    public static NetworkInterface [] getNetworkInterfaces()
    {
        return NetworkInterfaceImpl.getNetworkInterfaces();
    }

    /**
     * Gets the type of this network interface.  Possibilities include
     * UNKNOWN, MOCA, WIRED_ETHERNET, WIRELESS_ETHERNET.
     *
     * @return The type of this interface.
     */
    public int getType()
    {
        return -1;
    }

    /**
     * Gets a humanly readable name for this interface, e.g. "ie0".
     *
     * @return The display name of this interface.
     */
    public String getDisplayName()
    {
        return null;
    }

    /**
     * Gets the <code>InetAddress</code> of this interface.  Returns one of the
     * <code>InetAddress</code> instances in the array returned by the 
     * <code>getInetAddresses</code> method.  If the array contains multiple
     * <code>InetAddress</code> instances, unless specified elsewhere, the
     * determination of which <code>InetAddress</code> to return is
     * implementation specific.
     *
     * @return The <code>InetAddress</code> of this interface.
     * @see  NetAuthorizationHandler2#notifyActivityStart
     * @see  NetAuthorizationHandler2#notifyAction
     * @see  HttpRequestResolutionHandler#resolveHttpRequest
     */
    public InetAddress getInetAddress()
    {
        return null;
    }

    /**
     * Gets an array of <code>InetAddress</code> containing all of the IP addresses
     * configured for this <code>NetworkInterface</code>.
     *
     * @return The array of <code>InetAddress</code> for this interface.
     */
    public InetAddress [] getInetAddresses()
    {
        return null;
    }
    
    /**
     * Gets the MAC address of this interface.
     *
     * @return The MAC address of this interface.
     */
    public String getMacAddress()
    {
        return null;
    }
}
