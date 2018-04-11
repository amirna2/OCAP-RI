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

package org.cablelabs.impl.ocap.hn.upnp.server;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPDeviceImpl;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceList;
import org.ocap.hn.upnp.common.UPnPAdvertisedDevice;
import org.ocap.hn.upnp.common.UPnPAdvertisedDeviceIcon;
import org.ocap.hn.upnp.common.UPnPAdvertisedService;
import org.ocap.hn.upnp.server.UPnPManagedDeviceIcon;

public class UPnPAdvertisedDeviceImpl extends UPnPDeviceImpl implements UPnPAdvertisedDevice
{
    private final InetAddress m_inet;
    private final UPnPManagedDeviceImpl m_device; 
    
    public UPnPAdvertisedDeviceImpl(UPnPManagedDeviceImpl device, InetAddress inet)
    {
        super(device != null ? device.getDevice() : null);
        m_device = device;
        m_inet = inet;
    }

    public UPnPAdvertisedDevice[] getEmbeddedAdvertisedDevices()
    {
        Set devices = new HashSet();
        
        if(getDevice().getDeviceList() != null)
        {
            for(Iterator i = getDevice().getDeviceList().iterator(); i.hasNext();)
            {
                devices.add(i.next());
            }
        }
        return (UPnPAdvertisedDevice[]) devices.toArray(new UPnPAdvertisedDevice[devices.size()]);
    }

    public UPnPAdvertisedDeviceIcon[] getAdvertisedIcons()
    {
        if(m_device == null)
        {
            return new UPnPAdvertisedDeviceIcon[0];
        }
        
        UPnPManagedDeviceIcon[] icons = m_device.getIcons();
        UPnPAdvertisedDeviceIcon[] advIcons = new UPnPAdvertisedDeviceIcon[icons.length];
        for(int i = 0; i < icons.length; ++i)
        {
            advIcons[i] = new UPnPAdvertisedDeviceIconImpl(icons[i], m_inet);
        }
        return advIcons;
    }

    public UPnPAdvertisedService[] getAdvertisedServices()
    {
        ServiceList services = getDevice().getServiceList();
        UPnPAdvertisedService[] advServices = new UPnPAdvertisedService[services.size()];
        for(int i = 0; i < services.size(); ++i)
        {
            advServices[i] = new UPnPAdvertisedServiceImpl((Service)services.get(i), m_inet);
        }
        return advServices;
    }

    public InetAddress getInetAddress()
    {
        return m_inet;
    }

    public String getPresentationURL()
    {
        return UPnPAdvertisedImpl.replaceInetInURL(getDevice().getPresentationURL(), m_inet.getHostAddress());
    }

    public String getURLBase()
    {
        if (getDevice().isRootDevice()) 
        {
            return UPnPAdvertisedImpl.replaceInetInURL(getDevice().getURLBase(), m_inet.getHostAddress());
        }
        return UPnPAdvertisedImpl.replaceInetInURL(getDevice().getRootDevice().getURLBase(), m_inet.getHostAddress());
    }
}
