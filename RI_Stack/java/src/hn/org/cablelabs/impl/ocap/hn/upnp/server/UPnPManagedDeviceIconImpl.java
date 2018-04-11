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
import java.util.Set;

import org.ocap.hn.upnp.common.UPnPAdvertisedDeviceIcon;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.hn.upnp.server.UPnPManagedDeviceIcon;

public class UPnPManagedDeviceIconImpl extends UPnPManagedDeviceIcon
{
    private final UPnPManagedDevice m_device;

    public UPnPManagedDeviceIconImpl(String mimetype, int width, int height, int colordepth, byte[] data, UPnPManagedDevice device)
    {
        super(mimetype, width, height, colordepth, data);
        m_device = device;
    }

    /**
     * Override method now that we have the device to get info from.
     */    
    public UPnPAdvertisedDeviceIcon[] getAdvertisedDeviceIcons()
    {
        if(m_device == null || !m_device.isAlive())
        {
            return new UPnPAdvertisedDeviceIcon[0];
        }
        
        InetAddress[] inets = m_device.getInetAddresses();
        Set icons = new HashSet();
        for(int i = 0; i < inets.length; ++i)
        {
            // Creating new ones instead of work maintaining a list of advertised devices.
            icons.add(new UPnPAdvertisedDeviceIconImpl(this, inets[i]));
        }
        
        return (UPnPAdvertisedDeviceIconImpl[]) icons.toArray(new UPnPAdvertisedDeviceIconImpl[icons.size()]);
    }    
}