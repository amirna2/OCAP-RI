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

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPInputInterceptor;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPOutputInterceptor;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.ocap.hn.upnp.XMLUtil;
import org.ocap.hn.upnp.common.UPnPIncomingMessageHandler;
import org.ocap.hn.upnp.common.UPnPOutgoingMessageHandler;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.hn.upnp.server.UPnPManagedDeviceIcon;
import org.ocap.hn.upnp.server.UPnPManagedDeviceListener;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.ocap.system.MonitorAppPermission;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.cybergarage.upnp.diag.MessageInterceptor;


public class UPnPDeviceManagerImpl extends UPnPDeviceManager
{

    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(UPnPDeviceManagerImpl.class);

    private static final UPnPDeviceManagerImpl INSTANCE = new UPnPDeviceManagerImpl();

    private static final Set m_managedDevices = new HashSet();
    private final Set m_deviceListeners = new HashSet();

    /**
     * Construct the instance.  Protected by singleton pattern.
     */
    public UPnPDeviceManagerImpl()
    {

    }

    /**
     * Obtain the local UPnP device manager.
     *
     * @return The singleton UPnPDeviceManager.
     */
    public static UPnPDeviceManager getInstance()
    {
       return INSTANCE;
    }

    public UPnPManagedDevice[] getDevices()
    {
        return (UPnPManagedDevice[]) m_managedDevices.toArray(new UPnPManagedDevice[m_managedDevices.size()]);
    }

    public UPnPManagedDevice[] getDevicesByType(String type)
    {
        // Return a zero length array if type is null.
        if(type == null)
        {
            return new UPnPManagedDevice[0];
        }
        
        Set devicesForType = new HashSet();
        
        for (Iterator i = m_managedDevices.iterator(); i.hasNext(); )
        {
            UPnPManagedDevice managedDevice = (UPnPManagedDevice) i.next();
            
            if(managedDevice != null && 
                    sameOrLowerVersion(type, managedDevice.getDeviceType()))
            {
                devicesForType.add(managedDevice);
            }
        }
        return (UPnPManagedDevice[]) devicesForType.toArray(new UPnPManagedDevice[devicesForType.size()]);
    }

    public UPnPManagedDevice[] getDevicesByUDN(String UDN)
    {
        // Return a zero length array if type is null.
        if(UDN == null)
        {
            return new UPnPManagedDevice[0];
        }

        Set devicesForUDN = new HashSet();
        for (Iterator i = m_managedDevices.iterator(); i.hasNext(); )
        {
            UPnPManagedDevice managedDevice = (UPnPManagedDevice) i.next();
            if(managedDevice != null && UDN.equals(managedDevice.getUDN()))
            {
                devicesForUDN.add(managedDevice);
            }
        }
        return (UPnPManagedDevice[]) devicesForUDN.toArray(new UPnPManagedDevice[devicesForUDN.size()]);
    }

    public UPnPManagedDevice[] getDevicesByServiceType(String type)
    {
        // Return a zero length array if type is null.
        if(type == null)
        {
            return new UPnPManagedDevice[0];
        }

        Set devicesForService = new HashSet();
        for (Iterator i = m_managedDevices.iterator(); i.hasNext(); )
        {
            UPnPManagedDevice managedDevice = (UPnPManagedDevice) i.next();

            if(managedDevice != null &&
                    managedDevice.getServices() != null)
            {
                UPnPManagedService[] services = managedDevice.getServices();
                for(int x = 0; x < services.length; x++)
                {
                    if(services[x] != null &&
                            services[x].getAdvertisedServices() != null &&
                            services[x].getAdvertisedServices().length > 0)
                    {
                        if(sameOrLowerVersion(type, services[x].getServiceType()))
                        {
                            devicesForService.add(managedDevice);
                            break;
                        }
                    }
                }
            }

        }
        return (UPnPManagedDevice[]) devicesForService.toArray(new UPnPManagedDevice[devicesForService.size()]);
    }

    public UPnPManagedDevice createDevice(UPnPManagedDevice parent,
                                                 InputStream description,
                                                 UPnPManagedDeviceIcon [] icons)
                                                 throws IOException,
                                                 SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        if(description == null)
        {
            throw new IllegalArgumentException("Description is a null InputStream");
        }

        UPnPManagedDeviceImpl device = null;

        try
        {
            if(parent == null || parent instanceof UPnPManagedDeviceImpl)
            {
                device = new UPnPManagedDeviceImpl((UPnPManagedDeviceImpl)parent, XMLUtil.toNode(description).getFirstChild(), m_deviceListeners);
            }
        }
        catch (DOMException e)
        {
            throw new IllegalArgumentException("Description is invalid XML. " + e);
        }
        catch (SAXException e)
        {
            throw new IllegalArgumentException("Description is invalid XML. " + e);
        }

        if(icons != null && icons.length > 0)
        {
            for(int i = 0; i < icons.length; ++i)
            {
                device.addIcon(icons[i]);
            }
        }

        return device;
    }

    public void addDeviceListener(UPnPManagedDeviceListener listener)
    {
        // TODO : implement Context Sensitive Callbacks
        if(listener != null)
        {
            m_deviceListeners.add(listener);
        }
    }

    public void removeDeviceListener(UPnPManagedDeviceListener listener)
    {
        // TODO : implement Context Sensitive Callbacks
        m_deviceListeners.remove(listener);
    }

    public void setIncomingMessageHandler(UPnPIncomingMessageHandler inHandler)
                    throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        // NOTE: this can be done statically only because UPnPDeviceManager is a singleton!

        MessageInterceptor.setServerInputInterceptor(inHandler == null ? null : new UPnPInputInterceptor(inHandler));
    }

    public void setOutgoingMessageHandler(UPnPOutgoingMessageHandler outHandler)
                    throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        // NOTE: this can be done statically only because UPnPDeviceManager is a singleton!

        MessageInterceptor.setServerOutputInterceptor(outHandler == null ? null : new UPnPOutputInterceptor(outHandler));
    }

    // End of UPnPDeviceManager published APIs
    
    protected void addManagedDevice(UPnPManagedDeviceImpl device)
    {
        m_managedDevices.add(device);
    }
    
    protected void removeManagedDevice(UPnPManagedDeviceImpl device)
    {
        m_managedDevices.remove(device);
    }
    
    /**
     * Method to deal with requirement to match based on strings that end with a :v where
     * v is a number and the strings are considered equivalent if that number is the same
     * or lower than the first string passed in.
     * @param str1 String where version needs to be same or lower
     * @param str2 String to match against
     * @return true if the first string matches the second string as long as the v is the same or lower in the second string.
     */
    static private boolean sameOrLowerVersion(String str1, String str2)
    {
        assert(str1 != null);

        if(str2 == null)
        {
            return false;
        }

        String[] parts1 = Utils.split(str1, ":");
        String[] parts2 = Utils.split(str2, ":");

        if(parts1 != null &&
                parts2 != null &&
                parts1.length == parts2.length)
        {
            try
            {
                for(int i = 0; i < parts1.length - 1; ++i)
                {
                    if(parts1[i] == null || !(parts1[i].equals(parts2[i])))
                    {
                        return false;
                    }
                }

                Integer ver1 = new Integer(parts1[parts1.length - 1]);
                Integer ver2 = new Integer(parts2[parts2.length - 1]);
                return ver1.compareTo(ver2) <= 0;
            }
            catch(NumberFormatException ex)
            {
                // Swollowing exception and reporting all cases as parsing issues.
            }
        }
        if (log.isWarnEnabled())
        {
            log.warn("Numeric parse issue with " + str1 + " and " + str2);
        }

        return false;
    }
}
