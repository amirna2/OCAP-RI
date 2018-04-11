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

package org.cablelabs.impl.ocap.hn.upnp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.upnp.ruihsrc.RemoteUIServerService;
import org.cablelabs.impl.ocap.hn.upnp.ruihsrc.RemoteUIServerServiceSCPD;
import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.hn.upnp.server.UPnPManagedDeviceIcon;

/**
 * This is the main class for implementation of the RemoteUIService
 * It is a composite class that utilizes the UPnP Diagnostics API to create a
 * UPnP network device, and contains implementation classes for each of the
 * services required by the RemoteUIService
 */
public class RemoteUIServer
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(RemoteUIServer.class);
    
    public static final RemoteUIServer INSTANCE = new RemoteUIServer();
    
    private RemoteUIServerService m_russ;
    private UPnPManagedDevice m_device;
    
    private static final String DEFAULT_DEVICE_UUID_FILENAME = "remoteui_device_uuid.txt";
    
    // Prevent outside instantiation of this singleton
    private RemoteUIServer()
    {
    }
    
    public static RemoteUIServer getInstance()
    {
        return INSTANCE;
    }
    
    // Add a sub-device and service to parent device
    public synchronized void addRemoteUIServer(UPnPManagedDevice parent, InetAddress inet)
    {
        UPnPDeviceManager devMgr = UPnPDeviceManager.getInstance();
        try
        {
            m_device = devMgr.createDevice(parent, 
                    new ByteArrayInputStream(XMLUtil.toByteArray(getDeviceDescription(inet))),
                    new UPnPManagedDeviceIcon[] { new UPnPManagedDeviceIcon("image/jpeg", 48, 32, 8, null),
                                                  new UPnPManagedDeviceIcon("image/png", 48, 32, 8, null)
                                                });
            
            m_russ = new RemoteUIServerService(m_device.createService(RemoteUIServerService.SERVICE_TYPE, 
                    new ByteArrayInputStream(XMLUtil.toByteArray(RemoteUIServerServiceSCPD.getSCPD())), 
                    null));
        }
        catch (SecurityException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to create RemoteUIServer ", e);
            }
        }
        catch (IOException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to create RemoteUIServer ", e);
            }
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Unable to create RemoteUIServer ", e);
            }
        }
    }
    
    private String getDeviceDescription(InetAddress inet)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("<device>\n");
        sb.append("    <deviceType>urn:schemas-upnp-org:device:RemoteUIServerDevice:1</deviceType>\n");
        sb.append("    <friendlyName>"    + getDeviceName() +    "</friendlyName>\n");
        sb.append("    <manufacturer>OCAP</manufacturer>\n");
        sb.append("    <manufacturerURL>http://www.cablelabs.com</manufacturerURL>\n");
        sb.append("    <modelDescription>RemoteUI Server Device</modelDescription>\n");
        sb.append("    <modelName>"       + getDeviceName() + "</modelName>\n");
        sb.append("    <modelNumber>OCORI-1.2</modelNumber>\n");
        sb.append("    <modelURL>http://www.cablelabs.com</modelURL>\n");
        sb.append("    <serialNumber>OCORI-000.1.2</serialNumber>\n");
        sb.append("    <UDN>" + MediaServer.createUUID(DEFAULT_DEVICE_UUID_FILENAME, inet) + "</UDN>\n");
        sb.append("    <UPC>111111111111</UPC>\n");
        sb.append("</device>\n");
                
        return sb.toString();
    }
    
    private String getDeviceName()
    {
        return "RemoteUI Server";
    }

    public RemoteUIServerService getRemoteUIServerService()
    {
        return m_russ;
    } 
    

}
