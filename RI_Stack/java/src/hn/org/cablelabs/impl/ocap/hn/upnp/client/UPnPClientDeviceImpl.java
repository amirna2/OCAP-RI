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

package org.cablelabs.impl.ocap.hn.upnp.client;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.net.URL;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPDeviceImpl;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Icon;
import org.cybergarage.upnp.IconList;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceList;

import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientDeviceIcon;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPClientStateVariable;
import org.ocap.hn.upnp.common.UPnPAdvertisedDevice;
import org.ocap.hn.upnp.common.UPnPAdvertisedDeviceIcon;
import org.ocap.hn.upnp.common.UPnPAdvertisedService;

/**
 * This class provides the client representation of a
 * UPnP device, associated with a single IP address.
 */
public class UPnPClientDeviceImpl extends UPnPDeviceImpl implements UPnPClientDevice
{
    private static final Logger log = Logger.getLogger(UPnPClientDeviceImpl.class);

    // Cybergarage Control Point which may be associated this device representation if obtained from a control point.
    private final ControlPoint m_controlPoint;

    // List of icons associated with this device
    private UPnPClientDeviceIconImpl m_icons[] = null;

    // List of services associated with this device
    private UPnPClientServiceImpl m_services[] = null;

    // List of embedded devices associated with this device
    private List m_embeddedDevices = new ArrayList();

    // Parent device of this device
    private UPnPClientDevice m_parentDevice;

    // Indicates if this device is going to be destroyed
    private boolean m_destroyed = false;
    
    /**
     * Construct an object of this class constructed from a UPnPControlPoint.
     *
     * @param device        Cybergarage device which this class wraps
     * @param controlPoint  Cybergarage control point associated with
     *                      UPnPControlPoint.getInstance()
     */
    public UPnPClientDeviceImpl(Device device, ControlPoint controlPoint)
    {
        super(device);
        if (log.isDebugEnabled())
        {
            log.debug("Creating device: " + device.getFriendlyName());
        }

        assert device != null;
        m_device = device;

        assert controlPoint != null;
        m_controlPoint = controlPoint;

        // Create services associated with this device and subscribe as default
        getServices();

     }

    /**
     * Construct an object of this class from a UPnPManagedDeviceImpl.
     *
     * @param device        Cybergarage device which this class wraps
     */
    public UPnPClientDeviceImpl(Device device)
    {
        super(device);
        assert device != null;
        m_device = device;

        m_controlPoint = null;
     }

    // METHODS REQUIRED TO IMPLEMENT THE INTERFACE


    /**
     * Called when shutting down the control point to indicate that device is
     * destroyed.
     */
    public void destroy()
    {
        if (log.isDebugEnabled())
        {
            log.debug("destroy() - called for device: " + getFriendlyName());
        }                            
        m_destroyed = true;
    }
    
    /**
     * Returns indication if this device has been marked for destruction.
     * Used by threads which are gathering service information.
     * 
     * @return  true if device is marked as destroyed, false otherwise
     */
    public boolean isDestroyed()
    {
        return m_destroyed;
    }
    
    /**
     * 
     * Reports the base URL for all relative URLs of this device. This value is obtained from the 
     * URLBase element within the device description document. If this is an embedded device, the 
     * URLBase element of the root device is returned.
     * 
     * <p>If the URLBase property is not specified in the device description document, this method 
     * returns the URL from which the device description may be retrieved.
     * 
     * @return The base URL for all relative URLs of this
     *         <code>UPnPDevice</code>.
     */
    public String getURLBase()
    {
        String urlBase = null;
        if (m_device.isRootDevice()) 
        {
            urlBase =  m_device.getURLBase();
        }
        else 
        {
            urlBase =  m_device.getRootDevice().getURLBase();
        }
        if ("".equals(urlBase))
        {
            urlBase = m_device.getLocation();
            int startHost = urlBase.indexOf("//");
            int startURI = urlBase.indexOf("/", startHost + 2);
            urlBase = urlBase.substring(0, startURI);
        }
        return urlBase;
    }

    /**
     * Gets the UPnP icons for this device. This returned array is
     * derived from the icon elements within the iconList element
     * of a device description.
     *
     * <p>If the iconList element in the device description is empty
     * or not present, returns a zero length array.
     *
     * @return An array of <code>UPnPDeviceIcon</code>s representing
     *         the icons that the device declares.
     */
    public UPnPClientDeviceIcon[] getIcons()
    {
        if (m_icons == null)
        {
            m_icons = new UPnPClientDeviceIconImpl[0];
            IconList list = m_device.getIconList();
            if ((list != null) && (!list.isEmpty()))
            {
                m_icons = new UPnPClientDeviceIconImpl[list.size()];
                for (int i = 0; i < list.size(); i++)
                {
                    Icon icon = list.getIcon(i);
                    m_icons[i] = new UPnPClientDeviceIconImpl(icon, this);
                }
            }
        }

        UPnPClientDeviceIcon icons[] = new UPnPClientDeviceIconImpl[m_icons.length];
        System.arraycopy(m_icons, 0, icons, 0, icons.length);
        return icons;
    }

    /**
     * Gets the services supported by this device.  Does not return
     * services held in embedded devices.
     *
     * @return An array of <code>UPnPService</code>s.  If the
     *         serviceList element in the device description is
     *         empty, this method returns a zero length array.
     */
    public UPnPClientService[] getServices()
    {
        if (m_services == null)
        {
            m_services = new UPnPClientServiceImpl[0];
            ServiceList cServices = m_device.getServiceList();

            if (cServices != null)
            {
                m_services = new UPnPClientServiceImpl[cServices.size()];
                for (int i = 0; i < cServices.size(); i++)
                {
                    m_services[i] = m_controlPoint != null ?
                            new UPnPClientServiceImpl((Service)cServices.get(i), this, m_controlPoint) :
                            new UPnPClientServiceImpl((Service)cServices.get(i), this);
                }

                // Subscribe to these services by default in a separate thread to prevent hangs for client side subscription.
                if(m_controlPoint != null)
                {
                    // Perform this in a separate thread because getting state variables requires network requests
                    // which may hang or be delayed
                    CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                    ccm.getSystemContext().runInContextAsync(new Runnable()
                    {
                        public void run()
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("getServices() - initializing services for device: " + getFriendlyName() + 
                                ", location: " + getLocation());
                            }                            
                            

                            for (int i = 0; i < m_services.length; i++)
                            {
                                if (!m_destroyed)
                                {
                                    UPnPClientStateVariable vars[] = m_services[i].getStateVariables();
                                    boolean hasEventedVars = false;
                                    for (int j = 0; j < vars.length; j++)
                                    {
                                        if (vars[j].isEvented())
                                        {
                                            hasEventedVars = true;
                                            break;
                                        }
                                    }

                                    // Only subscribe to services which have evented variables
                                    if (hasEventedVars)
                                    {
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug("getServices() - has evented vars, subscribing to service: " + m_services[i].getServiceId());
                                        }                            
                                        m_services[i].setSubscribedStatus(true);
                                    }
                                    else
                                    {
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug("getServices() - has no evented vars, NOT subscribing to service: " + m_services[i].getServiceId());
                                        }                                                                    
                                    }
                                }
                                else
                                {
                                    break;
                                }
                            }
                        }
                    });
                }
            }
        }

        UPnPClientServiceImpl services[] = new UPnPClientServiceImpl[m_services.length];
        System.arraycopy(m_services, 0, services, 0, m_services.length);
        return services;
    }

    /**
     * Gets the embedded devices for this <code>UPnPDevice</code>.
     *
     * @return The embedded devices for this device.  If this device
     *         has no embedded devices, returns a zero length array.
     *         Returns only the next level of embedded devices, not
     *         recursing through embedded devices for subsequent
     *         levels of embedded devices.
     */
    public UPnPClientDevice[] getEmbeddedDevices()
    {
        return (UPnPClientDevice[]) m_embeddedDevices.toArray(new UPnPClientDevice[m_embeddedDevices.size()]);
    }

    /**
     * Gets the UPnP presentation page URL of this device. This
     * value is taken from the value of the presentationURL
     * element within a device description.
     *
     * <p>If the presentationURL is empty or not present, returns
     * the empty String.
     *
     * @return The presentationURL of this device.
     */
    public String getPresentationURL()
    {
        String result = m_device.getPresentationURL();
        return result != null ? result : "";
    }

    /**
     * Returns the IP address for this device.
     *
     * @return an InetAddress representing this device's IP address.
     */
    public InetAddress getInetAddress()
    {
        InetAddress address = null;
        
        try
        {
            URL url = new URL(super.m_device.getLocation());
            address = InetAddress.getByName(url.getHost());
        }
        catch (Exception e)
        {
            if (log.isErrorEnabled())
            {
                log.error("Can't get InetAddress - " + toString(),e);
            }
        }
        return address;
    }

    /**
     * Returns the parent UPnPDevice of this UPnPDevice, if
     * any.
     *
     * @return A UPnPDevice representing this device's parent
     *         device. Returns null if this device has no parent.
     */
    public UPnPClientDevice getParentDevice()
    {
        return m_parentDevice;
    }

    // OTHER PUBLIC METHODS

    /**
     * TODO
     */
    public void addChildDevice(UPnPClientDevice childDevice)
    {
        m_embeddedDevices.add(childDevice);
    }

    /**
     * TODO
     */
    public void clearChildDevices()
    {
        m_embeddedDevices.clear();
    }

    /**
     * TODO
     */
    public void setParentDevice(UPnPClientDevice parentDevice)
    {
        m_parentDevice = parentDevice;
    }

    /**
     * Return a string representation of this object.
     *
     * @return A string representation of this object.
     */
    public String toString()
    {
        return getFriendlyName() + " <\"" + m_device.getLocation() + "\", \"" + getUDN() + "\">";
    }

    public UPnPAdvertisedDevice[] getEmbeddedAdvertisedDevices()
    {
        return getEmbeddedDevices();
    }

    public UPnPAdvertisedDeviceIcon[] getAdvertisedIcons()
    {
        return getIcons();
    }

    public UPnPAdvertisedService[] getAdvertisedServices()
    {
        return getServices();
    }

    // *TODO* - additional methods to support using this class instead of IDevice/CDevice in DeviceImpl
    // *TODO* - Do we need these added to the spec?  probably not
    /**
     * Retrieves the Device's Middleware Profile
     * 
     * @return String - representing the middleware profile of the device
     */
    public String getMiddlewareProfile()
    {
        return super.m_device.getDeviceNode().getNodeValue(UPnPConstants.MIDDLEWARE_PROFILE);
    }

    /**
     * Retrieves the Device's Middleware Version
     * 
     * @return String - representing the middleware version of the device
     */
    public String getMiddlewareVersion()
    {
        return super.m_device.getDeviceNode().getNodeValue(UPnPConstants.MIDDLEWARE_VERSION);
    }
    
    /**
     * Retrieves the Device's Ocap HomeNetwork value
     * 
     * @return String - representing the devices Ocap HomeNetwork value
     */
    public String getOcapHomeNetworkString()
    {
        return super.m_device.getDeviceNode().getNodeValue(UPnPConstants.OCAP_HOME_NETWORK);
    }
    
    
    // *TODO* - is this a possible spec issue?  no we need a location method on Device
    /**
     * Retrieves the Device's location
     * 
     * @return String - representing the location of the device
     */
    public String getLocation()
    {
        return super.m_device.getLocation();
    }
}
