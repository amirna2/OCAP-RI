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

package org.cablelabs.impl.ocap.hn;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.util.MPEEnv;
import org.cybergarage.http.HTTPRequest;
import org.ocap.hn.NetworkInterface;

public class NetworkInterfaceImpl extends NetworkInterface
{
    /* logging */
    private static final Logger log = Logger.getLogger(NetworkInterfaceImpl.class);

    /** We can override any network interface to return MOCA type */
    private static final String mocaInterfaceProperty = MPEEnv.getEnv("OCAP.guides.MOCAinterface");

    /** The real java.net.NetworkInterface */
    private final java.net.NetworkInterface realNI;
    
    /** The Network Interface types defined in {@link org.ocap.hn.NetworkInterface} */
    private final int type;

    /** This address is the "special" address that will be returned by getInetAddress(),
     *  since this method is still supported for backwards-compatibility (see EC-1830) */
    private final InetAddress inetAddress;

    /** The Network Interface MAC address {@link org.ocap.hn.NetworkInterface.getMacAddress} */
    private final String macAddress;

    private NetworkInterfaceImpl( final java.net.NetworkInterface realNI)
    {
        this(realNI, (InetAddress)(realNI.getInetAddresses().hasMoreElements() 
                                  ? realNI.getInetAddresses().nextElement() : null));
    }
    
    /**
     * Provide the ability to create a org.ocap.hn.NetworkInterface for an arbitrary 
     *  inetAddress. This is to support the getInetAddress() requirements of 
     *  EC-1830 related to the passing of NI references. See 
     *  {@link org.ocap.hn.security.NetAuthorizationHandler2.notifyActivityStart}
     *  and {@link org.ocap.hn.service.HttpRequestResolutionHandler.resolveHttpRequest}.
     *  
     * @param realNI the java.net.NetworkInteface to associate with the org.ocap.hn.NetworkInterface
     * @param inetAddress the address to associate with the org.ocap.hn.NetworkInterface
     * 
     * @return An org.ocap.hn.NetworkInterface referencing the provided java.net.NetworkInterface 
     *         and given address
     */
    public NetworkInterfaceImpl( final java.net.NetworkInterface realNI,
                                 final InetAddress inetAddress )
    {
        this.realNI = realNI;
        this.inetAddress = inetAddress;
        
        // Get the network interface type
        this.type = HNAPIImpl.nativeGetNetworkInterfaceType(realNI.getDisplayName());

        // Get the MAC Address
        this.macAddress = HNAPIImpl.nativeGetMacAddress(realNI.getDisplayName());
    }

    /**
     * Provide the ability to create a org.ocap.hn.NetworkInterface for an arbitrary 
     *  inetAddress. This is to support the getInetAddress() requirements of 
     *  EC-1830 related to the passing of NI references. See
     *  {@link org.ocap.hn.security.NetAuthorizationHandler2.notifyActivityStart}
     *  and {@link org.ocap.hn.service.HttpRequestResolutionHandler.resolveHttpRequest}.
     *  
     * This constructor will enumerate the local java.net.NetworkInterfaces and associate
     *  this org.ocap.hn.NetworkInterface with the one associated with the provided address.
     *  
     * @param inetAddress the address to associate with the org.ocap.hn.NetworkInterface
     * 
     * @return An org.ocap.hn.NetworkInterface referencing the given address and the 
     * java.net.NetworkInterface associated with the address.
     */
    public NetworkInterfaceImpl(final InetAddress inetAddress)
    {
        this(getNIForAddress(inetAddress),inetAddress);
    }

    //
    // Implementation of NetworkInterface functions
    //

    // Override
    public static NetworkInterface[] getNetworkInterfaces()
    {
        final NetworkInterface[][] networkInterfaces = new NetworkInterface[1][];
        
        try
        {
            // Must be in system context mode for getNetworkInterfaces call
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            ccm.getSystemContext().runInContextSync(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        List l = new ArrayList();

                        for (Enumeration e = java.net.NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();)
                        {
                            java.net.NetworkInterface ni = (java.net.NetworkInterface) e.nextElement();
                            l.add(new NetworkInterfaceImpl(ni));
                        }

                        networkInterfaces[0] = (NetworkInterface[]) l.toArray(new NetworkInterface[l.size()]);
                    }
                    catch (SocketException e)
                    {
                        // Spec issue: behavior in this case is undefined.
                        networkInterfaces[0] = null;
                    }
                }
            });
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Exception when trying to retrieve Inet Addresses.");
            }
        }

        return networkInterfaces[0];
    }

    // Override
    public int getType()
    {
        final String displayName = realNI.getDisplayName();
        
        if (mocaInterfaceProperty != null && mocaInterfaceProperty.equalsIgnoreCase(displayName))
        {
            return MOCA;
        }
        return type;
    }

    // Override
    public String getDisplayName()
    {
        return realNI.getDisplayName();
    }

    // Override
    public InetAddress getInetAddress()
    {
        return inetAddress;
    }

    // Override
    public String getMacAddress()
    {
        return macAddress;
    }

    // Override
    public InetAddress[] getInetAddresses()
    {
        final InetAddress[][] addresses = new InetAddress[1][];
        
        try
        {
            // Must be in system context mode for getAddresses call
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            ccm.getSystemContext().runInContextSync(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        final List l = new ArrayList();

                        for (Enumeration e = realNI.getInetAddresses(); e.hasMoreElements();)
                        {
                            java.net.InetAddress address = (java.net.InetAddress) e.nextElement();
                            l.add(address);
                        }

                        addresses[0] = (InetAddress[]) l.toArray(new InetAddress[l.size()]);
                    }
                    catch (Exception e)
                    {
                        addresses[0] = null;
                    }
                }
            });
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Exception when trying to retrieve Inet Addresses.");
            }
        }

        return addresses[0];
    }

    // 
    // NetworkInterfaceImpl-only functions
    //
    
    public java.net.NetworkInterface getRealNI()
    {
        return realNI;
    }
    
    /**
     * Find the java.net.NetworkInterface bound to the given address or null if no NI
     * is bound to the address.
     * 
     * @param address The bind address to search for
     * @return The java.net.NetworkInterface bound to the given address
     */
    public static java.net.NetworkInterface getNIForAddress(final InetAddress address)
    {
        Enumeration niEnum;
        try
        {
            niEnum = java.net.NetworkInterface.getNetworkInterfaces();
        }
        catch (SocketException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn( "getNIForAddress: exception enumerating the NetworkInterfaces ", e );
            }
            return null;
        }
            
        while (niEnum.hasMoreElements())
        {
            java.net.NetworkInterface ni = (java.net.NetworkInterface)niEnum.nextElement();
            Enumeration addrEnum = ni.getInetAddresses();
            while (addrEnum.hasMoreElements())
            {
                final InetAddress niAddress = (InetAddress)addrEnum.nextElement();
                if (niAddress != null && niAddress.equals(address))
                {
                    return ni;
                }
            }
        }
        return null;
    } // END getNIForAddress()
    
    public static java.net.NetworkInterface getRealNIForRequest(final HTTPRequest httpRequest)
    {
        final InetAddress localAddress = httpRequest.getSocket().getSocket().getLocalAddress();
        
        Enumeration niEnum;
        try
        {
            niEnum = java.net.NetworkInterface.getNetworkInterfaces();
        }
        catch (SocketException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn( "getNIForRequest: exception enumerating the NetworkInterfaces ", e );
            }
            return null;
        }
            
        while (niEnum.hasMoreElements())
        {
            java.net.NetworkInterface ni = (java.net.NetworkInterface)niEnum.nextElement();
            Enumeration addrEnum = ni.getInetAddresses();
            while (addrEnum.hasMoreElements())
            {
                final InetAddress niAddress = (InetAddress)addrEnum.nextElement();
                if (niAddress != null && niAddress.equals(localAddress))
                {
                    return ni;
                }
            }
        }
        if (log.isWarnEnabled())
        {
            log.warn( "getNIForRequest: could not find an NI for address " + localAddress );
        }
        return null;
    }
} // END class NetworkInterfaceImpl
