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
import java.text.SimpleDateFormat;
import java.util.Date;

import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.manager.CallerContext;
import org.davic.net.InvalidLocatorException;
import org.davic.resources.ResourceProxy;
import org.dvb.application.AppID;
import org.ocap.hn.resource.NetResourceUsage;
import org.ocap.net.OcapLocator;

/**
 * This class represents a usage of resources on a specific home network
 * activity.
 */
public class NetResourceUsageImpl extends ResourceUsageImpl implements NetResourceUsage
{
    private String m_usageType;
    private CallerContext m_context;
    private InetAddress m_inetAddress;
    private OcapLocator m_locator;
    private Date m_startTime;
   
    public NetResourceUsageImpl( final InetAddress address, 
                                 final String usageType,
                                 final OcapLocator locator,
                                 final Date startTime )
    {
        super(null, 0);
        
        m_usageType = usageType;
        m_inetAddress = address;
        m_locator = locator;
        if (startTime != null)
        {
            // startTime is mutable (unfortunately), make a copy
            m_startTime = new Date(startTime.getTime());
        }
        else
        {
            m_startTime = null;
        }
        
        // We know that a NetResourceUsage requires a NIC
        set("org.davic.net.tuning.NetworkInterfaceController", null);

        // set flag so this resource usage is not systemUsage(RezMgr) 
        setSystemUsage(false);
    }

    /**
     * {@inheritDoc}
     */
    public String getUsageType()
    {
        return m_usageType;
    }
    
    /**
     * {@inheritDoc}
     */
    public InetAddress getInetAddress()
    {
        return m_inetAddress;
    }

    /**
     * {@inheritDoc}
     */
    public OcapLocator getOcapLocator()
    {
        return m_locator;
    }

    /**
     * {@inheritDoc}
     */
    public AppID getAppID()
    {
        return null;
    }

    static final SimpleDateFormat m_shortDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ssZ");

    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("NetResourceUsage:[type ");
        buffer.append(m_usageType);
        buffer.append(", address ").append(m_inetAddress);
        buffer.append(", locator ").append(m_locator);
        buffer.append(", startTime ");
        if (m_startTime != null)
        {
            buffer.append(m_shortDateFormat.format(m_startTime).toString());
        }
        else
        {
            buffer.append("null");
        }
        buffer.append(']');
        return buffer.toString();
    }
} // END class NetResourceUsageImpl
