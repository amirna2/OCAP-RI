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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPDeviceImpl;
import org.cablelabs.impl.util.SecurityUtil;
import org.cybergarage.http.HTTPRequest;
import org.cybergarage.http.HTTPResponse;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.UPnP;
import org.cybergarage.upnp.Device.Interceptor;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.device.InvalidDescriptionException;
import org.ocap.hn.upnp.common.UPnPAdvertisedDevice;
import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.hn.upnp.server.UPnPManagedDeviceIcon;
import org.ocap.hn.upnp.server.UPnPManagedDeviceListener;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.ocap.system.MonitorAppPermission;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class UPnPManagedDeviceImpl extends UPnPDeviceImpl implements UPnPManagedDevice
{
    // Log4J logging facility
    private static final Logger log = Logger.getLogger(UPnPManagedDeviceImpl.class);

    // Directly managed components
    private final List m_managedDeviceIcons = new ArrayList();
    private final List m_managedServices = new ArrayList();

    // Object graph of managed devices
    private final UPnPManagedDeviceImpl m_parentDevice;
    private final List m_subDevices = new ArrayList();

    // Parsed description fields from InputStream XML
    private Map m_properties = new LinkedHashMap();

    // Reference to device listeners shared by all devices and added through UPnPDeviceManager calls
    private final Set m_deviceListeners;

    // System context
    private static CallerContext systemContext;

    // TODO : Can we keep track here or do we need to in our composite UPnPDevices
    // Who else would be able to start and stop the devices on the network behind our
    // backs?
    private boolean isAlive = false;
    
    private boolean m_mediaDevice = false;

    public UPnPManagedDeviceImpl(UPnPManagedDeviceImpl parent, Node deviceNode, Set deviceListeners)
    {
        assert deviceListeners != null;
        m_parentDevice = parent;
        m_deviceListeners = deviceListeners;

        if(deviceNode == null || !validNode(deviceNode))
        {
            throw new IllegalArgumentException("Invalid device description.");
        }

        if(m_parentDevice instanceof UPnPManagedDeviceImpl)
        {
            m_parentDevice.addSubDevice(this);
        }

        NodeList nodeList = deviceNode.getChildNodes();
        for(int i = 0; i < nodeList.getLength(); ++i)
        {
            Node node = nodeList.item(i);
            if(Node.ELEMENT_NODE == node.getNodeType())
            {
                Node child = node.getFirstChild();
                m_properties.put(node.getNodeName(), child != null ? child.getNodeValue() : "");
            }
        }

        reloadDevice();

        // Making sure we have one device parsed and created, this is on the local loopback.
        // TODO : discuss this with implementation detail with spec team.  Can you really have
        // a managed device without being on the local loopback or somewhere it can be
        // fully checked out?
    }

    synchronized public void sendAlive()
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        if(getInetAddresses().length > 0)
        {
            // Notify application of impending advertisement on the network(s).
            for(Iterator l = m_deviceListeners.iterator(); l.hasNext(); )
            {
                ((UPnPManagedDeviceListener)l.next()).notifyDeviceAdded(this);
            }

            try
            {
                getSystemContext().runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        isAlive = getDevice().start();
                    }
                });
            }
            catch (SecurityException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Failed to call sendAlive: ", e);
                }
            }
            catch (IllegalStateException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Failed to call sendAlive: ", e);
                }
            }
            catch (InvocationTargetException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Failed to call sendAlive: ", e);
                    
                }
            }
            
            // Register advertised device  and subdevices with UPnPDeviceManager
            ((UPnPDeviceManagerImpl)UPnPDeviceManager.getInstance()).addManagedDevice(this);

            UPnPManagedDevice[] subDevices = getEmbeddedDevices(); 
            for(int i = 0; i < subDevices.length; i++)
            {
                ((UPnPDeviceManagerImpl)UPnPDeviceManager.getInstance()).addManagedDevice((UPnPManagedDeviceImpl)subDevices[i]);
            }
        }
    }

    synchronized public void sendByeBye()
        throws SecurityException
    {

        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        try
        {
            getSystemContext().runInContextSync(new Runnable()
            {
                public void run()
                {
                    getDevice().stop();
                }
            });
        }
        catch (SecurityException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Failed to call sendByeBye: " + e);
            }
        }
        catch (IllegalStateException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Failed to call sendByeBye: " + e);
            }
        }
        catch (InvocationTargetException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Failed to call sendByeBye: " + e);
            }
        }

        // TODO : Should isAlive still be true if exceptions thrown above?
        isAlive = false;

        // Notify application that removal from the network(s) has occurred.
        // TODO : spec clarification, the sendAlive is mentioned for notification of add
        // should sendByeBye be mentioned for notification of remove?
        // Should all references to this be removed?
        for(Iterator l = m_deviceListeners.iterator(); l.hasNext(); )
        {
            ((UPnPManagedDeviceListener)l.next()).notifyDeviceRemoved(this);
        }
        
        // Un-register un-advertised device and subdevices from UPnPDeviceManager
        ((UPnPDeviceManagerImpl)UPnPDeviceManager.getInstance()).removeManagedDevice(this);
        UPnPManagedDevice[] subDevices = getEmbeddedDevices(); 
        for(int i = 0; i < subDevices.length; i++)
        {
            ((UPnPDeviceManagerImpl)UPnPDeviceManager.getInstance()).removeManagedDevice((UPnPManagedDeviceImpl)subDevices[i]);
        }
    }

    synchronized public InetAddress[] setInetAddresses(InetAddress[] addresses)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        // If device is alive at this time, send a byebye
        if(isAlive())
        {
            sendByeBye();
        }

        InetAddress[] priorAddresses = getDevice().getInetAddresses();
        
        if(m_mediaDevice)
        {
            InetAddress[] removeAddresses = getMissingAddresses(priorAddresses, addresses);
            InetAddress[] addAddresses = getMissingAddresses(addresses, priorAddresses);
            
            if(MediaServer.getInstance().setupContentDelivery(removeAddresses, addAddresses))
            {
                if(log.isInfoEnabled())
                {
                    log.info("Content Delivery is available.");
                }
            }
            else
            {
                if(log.isWarnEnabled())
                {
                    log.warn("Content Delivery failed to initialize.");
                }
            }
        }
        
        getDevice().setInetAddresses(addresses == null ? new InetAddress[0] : addresses);

        this.sendAlive();

        return priorAddresses == null ? new InetAddress[0] : priorAddresses;
    }

    synchronized public InetAddress[] getInetAddresses()
    {
        // InetAddress only come from root device
        Device dev = getRootManagedDevice().getDevice();
        if(dev != null &&
                dev.getInetAddresses() != null)
        {
            return dev.getInetAddresses();
        }
        return new InetAddress[0];
    }

    public boolean isAlive()
    {
        return getRootManagedDevice().getAlive() && getInetAddresses().length > 0;
    }


    public UPnPAdvertisedDevice[] getAdvertisedDevices()
    {
        InetAddress[] inets = getInetAddresses();
        if(inets == null || !isAlive())
        {
            return new UPnPAdvertisedDevice[0];
        }
        
        UPnPAdvertisedDevice[] devices = new UPnPAdvertisedDevice[inets.length];
        for(int i = 0; i < inets.length; ++i)
        {
            devices[i] = new UPnPAdvertisedDeviceImpl(this, inets[i]);
        }
        
        return devices;
    }

    public UPnPManagedDevice[] getEmbeddedDevices()
    {
        return (UPnPManagedDevice[]) m_subDevices.toArray(new UPnPManagedDevice[m_subDevices.size()]);
    }

    public UPnPManagedDevice getManagedParentDevice()
    {
        return m_parentDevice;
    }

    public boolean setDeviceType(String type)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        return publishNewValue("deviceType", type);
    }

    public boolean setFriendlyName(String friendlyName)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        return publishNewValue("friendlyName", friendlyName);
    }

    public boolean setManufacturer(String manufacturer)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        return publishNewValue("manufacturer", manufacturer);
    }

    public boolean setManufacturerURL(String manufacturerURL)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        return publishNewValue("manufacturerURL", manufacturerURL);
    }

    public boolean setModelDescription(String modelDescription)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        return publishNewValue("modelDescription", modelDescription);
    }

    public boolean setModelName(String modelName)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        return publishNewValue("modelName", modelName);
    }

    public boolean setModelNumber(String modelNumber)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        return publishNewValue("modelNumber", modelNumber);
    }

    public boolean setModelURL(String modelURL)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        return publishNewValue("modelURL", modelURL);
    }

    public boolean setSerialNumber(String serialNumber)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        return publishNewValue("serialNumber", serialNumber);
    }

    public boolean setUDN(String UDN)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        return publishNewValue("UDN", UDN);
    }

    public boolean setUPC(String UPC)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        return publishNewValue("UPC", UPC);
    }

    public UPnPManagedDeviceIcon[] setIcons(UPnPManagedDeviceIcon[] icons)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        // Get array of previous icons for future.
        UPnPManagedDeviceIcon[] previousIcons = getIcons();

        // Clear current icons.
        m_managedDeviceIcons.removeAll(m_managedDeviceIcons);

        // Add new icons.
        if(icons != null && icons.length > 0)
        {
            for(int i = 0; i < icons.length; ++i)
            {
                // Use cover method here to copy semantics are preserved.
                addIcon(icons[i]);
            }
        }
        reloadDevice();
        return previousIcons;
    }

    public UPnPManagedDeviceIcon[] getIcons()
    {
        return (UPnPManagedDeviceIcon[]) m_managedDeviceIcons.toArray(new UPnPManagedDeviceIcon[m_managedDeviceIcons.size()]);
    }

    public UPnPManagedService[] getServices()
    {
        return (UPnPManagedService[]) m_managedServices.toArray(new UPnPManagedService[m_managedServices.size()]);
    }

    public boolean addService(UPnPManagedService service)
        throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        if(service != null)
        {
            m_managedServices.add(service);

            reloadDevice();

            return true;
        }
        return false;
    }

    // End of UPnPManagedDevice published APIs

    /**
     * Required to use a new icon when added from application.
     * Also use our own implementation in order to set the device. 
     */
    public void addIcon(UPnPManagedDeviceIcon icon)
    {
        // Required to use a new icon from passed in information.
        UPnPManagedDeviceIcon newIcon = new UPnPManagedDeviceIconImpl(icon.getMimeType(), 
                icon.getWidth(), icon.getHeight(), icon.getColorDepth(), icon.getData(), this);
        
        m_managedDeviceIcons.add(newIcon);
        reloadDevice();
    }

    public String getXMLDescription()
    {
        StringBuffer description = new StringBuffer();

        // This is the root device, so include all XML
        if(m_parentDevice == null)
        {
            description.append("<?xml version=\"1.0\" ?>\n");
            description.append("<root xmlns=\"urn:schemas-upnp-org:device-1-0\"");
            description.append(" xmlns:dlna=\"urn:schemas-dlna-org:device-1-0\"");
            description.append(" xmlns:ocap=\"urn:schemas-cablelabs-com:device-1-0\"");
            description.append(">\n");
            description.append("    <specVersion>\n");
            description.append("        <major>1</major>\n");
            description.append("        <minor>0</minor>\n");
            description.append("    </specVersion>\n");
        }

        description.append("    <device>\n");
        for(Iterator i = m_properties.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry)i.next();
            description.append("    <" + entry.getKey() + ">" + entry.getValue() + "</" + entry.getKey() + ">\n");
        }

        if(this.m_subDevices.size() > 0)
        {
            description.append("    <deviceList>\n");
            for(Iterator i = m_subDevices.iterator(); i.hasNext(); )
            {
                description.append(((UPnPManagedDeviceImpl)i.next()).getXMLDescription());
            }
            description.append("    </deviceList>\n");
        }

        if(this.m_managedDeviceIcons.size() > 0)
        {
            description.append("    <iconList>\n");
            for(Iterator i = m_managedDeviceIcons.iterator(); i.hasNext(); )
            {
                description.append(getXMLDescriptionForIcon((UPnPManagedDeviceIcon)i.next()));
            }
            description.append("    </iconList>\n");
        }

        if(m_managedServices.size() > 0)
        {
            description.append("    <serviceList>\n");
            for(Iterator i = m_managedServices.iterator(); i.hasNext(); )
            {
                description.append(((UPnPManagedServiceImpl)i.next()).getXMLDescription());
            }
            description.append("    </serviceList>\n");
        }

        description.append("    </device>\n");

        // This is the root device, so include all XML
        if(m_parentDevice == null)
        {
            description.append("</root>\n");
        }

        return description.toString();
    }

    private String getXMLDescriptionForIcon(UPnPManagedDeviceIcon icon)
    {
        assert icon != null;

        StringBuffer description = new StringBuffer();

        description.append("<icon>\n");
        description.append("    <mimetype>" + icon.getMimeType()     + "</mimetype>\n");
        description.append("    <width>"    + icon.getWidth()        + "</width>\n");
        description.append("    <height>"   + icon.getHeight()       + "</height>\n");
        description.append("    <depth>"    + icon.getColorDepth()   + "</depth>\n");
        description.append("    <url>" + MediaServer.ICON_REQUEST_URI_PREFIX + icon.hashCode() + "</url>\n");        
        description.append("</icon>\n");

        return description.toString();
    }

    // Set new value for device, then republish devices on all interfaces.
    synchronized private boolean publishNewValue(String key, String value)
    {
        assert key != null;
        assert value != null;

        if(!value.equals(m_properties.get(key)))
        {
            m_properties.put(key, value);
            return reloadDevice();
        }

        // No need to change and send byebye/alive.
        // TODO : possible spec clarification if setting value to previous value.
        return false;
    }

    /**
     * This class links the cybergarage constructed objects in it's graph with the ones
     * that are part of the UPnPManaged graph of objects.
     * 
     * Managed icons do not require information from the cybergarage class so they are not linked.
     */
    private void refreshObjectGraph() throws InvalidDescriptionException
    {
        if(!isRootDevice())
        {
            // Find in your parent device.
            setDevice(m_parentDevice.getDevice().getDevice((String)this.m_properties.get("UDN")));            
        }
        
        // Recursively reload each of your sub devices.
        for(Iterator i = m_subDevices.iterator(); i.hasNext();)
        {
            ((UPnPManagedDeviceImpl)i.next()).refreshObjectGraph();
        }

        // Refresh your service descriptions and callbacks
        for(Iterator i = m_managedServices.iterator(); i.hasNext();)
        {
            UPnPManagedServiceImpl ms = (UPnPManagedServiceImpl)i.next();
            Service s = getDevice().getService(ms.getServiceId());
            if(s != null)
            {
                ms.refreshService(s);
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Attempted to load service that was not available. " + ms.getServiceType());
                }
            }
        }
    }

    synchronized private boolean reloadDevice()
    {
        try
        {
            boolean state = false;

            // Reload root device
            UPnPManagedDeviceImpl rootManagedDevice = getRootManagedDevice();

            boolean restart = rootManagedDevice.isAlive();
            state = rootManagedDevice.getDevice().loadDescription(rootManagedDevice.getXMLDescription());

            // only send byes if restart not happening since sendAlive will 
            // send byes
            if(restart && !state)
            {
                rootManagedDevice.sendByeBye();
            }


            if(state)
            {
                rootManagedDevice.refreshObjectGraph();
                rootManagedDevice.getDevice().addInterceptor(new IconInterceptor());

                if(restart)
                {
                    rootManagedDevice.sendAlive();
                    return true;
                }
            }
            else
            {
                throw new IllegalArgumentException("Failed to load device description.");
            }
        }
        catch (InvalidDescriptionException e)
        {
            throw new IllegalArgumentException("Invalid device description " + e);
        }
        return false;
    }

    protected void addSubDevice(UPnPManagedDevice device)
    {
        m_subDevices.add(device);
    }

    /**
     * Lazily initialize and return the system context
     */
    private static synchronized CallerContext getSystemContext()
    {
        if (systemContext == null)
        {
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

            systemContext = ccm.getSystemContext();
        }

        return systemContext;
    }

    /**
     * Recursively find root node.
     */
    private UPnPManagedDeviceImpl getRootManagedDevice()
    {
        if(m_parentDevice == null)
        {
            // Initialize the root device if it does not already exist.
            if(getDevice() == null)
            {
                setDevice(new Device());
            }
            
            return this;
        }
        return m_parentDevice.getRootManagedDevice();

    }

    private boolean validNode(Node node)
    {
        boolean hasDeviceType = false;
        boolean hasFriendlyName = false;
        boolean hasManufacturer = false;
        boolean hasModelName = false;
        boolean hasUDN = false;
        if(!node.hasChildNodes())
        {
            if (log.isErrorEnabled())
            {
                log.error("device desciption had no child nodes.");
            }
            return false;
        }
        if(!"device".equals(node.getNodeName()))
        {
            if (log.isErrorEnabled())
            {
                log.error("First element in device desciption was not <device>: " + node.getNodeName());
            }
            return false;
        }
        
        NodeList nodeList = node.getChildNodes();
        for(int i = 0; i < nodeList.getLength(); ++i)
        {
            Node n = nodeList.item(i);
            if(Node.ELEMENT_NODE == node.getNodeType() &&
                    ("serviceList".equals(node.getNodeName()) ||
                            "iconList".equals(node.getNodeName())))
            {
                if (log.isErrorEnabled())
                {
                    log.error("Invalid element in device desciption: " + node.getNodeName());
                }
                return false;
            }
            if ("deviceType".equals(n.getNodeName())) 
            {
                hasDeviceType = true;
            } 
            else if ("friendlyName".equals(n.getNodeName())) 
            {
                hasFriendlyName = true;
            } 
            else if ("manufacturer".equals(n.getNodeName())) 
            {
                hasManufacturer = true;
            } 
            else if ("modelName".equals(n.getNodeName())) 
            {
                hasModelName = true;
            } 
            else if ("UDN".equals(n.getNodeName())) 
            {
                hasUDN = true;
            };
        }
        if (!hasDeviceType || !hasFriendlyName || !hasManufacturer || !hasModelName || !hasUDN ) {
            if (log.isErrorEnabled())
            {
                if (!hasDeviceType)
            {
                    log.error("Missing <deviceType> element in device desciption.");
            }
                if (!hasFriendlyName)
                {
                    log.error("Missing <friendlyName> element in device desciption.");
                }
                if (!hasManufacturer)
                {
                    log.error("Missing <manufacturer> element in device desciption.");
                }
                if (!hasModelName)
                {
                    log.error("Missing <modelName> element in device desciption.");
                }
                if (!hasUDN)
                {
                    log.error("Missing <UDN> element in device desciption.");
                }
            }
            return false;
        } 
        return true;
    }

    public boolean hasService(UPnPManagedServiceImpl service)
    {
        if(service != null)
        {
            for(Iterator i = m_subDevices.iterator(); i.hasNext();)
            {
                UPnPManagedDeviceImpl device = (UPnPManagedDeviceImpl)i.next();
                if(device != null && device.hasService(service))
                {
                    return true;
                }
            }

            for(Iterator i = m_managedServices.iterator(); i.hasNext();)
            {
                // TODO : what should be considered equivalent services?
                UPnPManagedServiceImpl s = (UPnPManagedServiceImpl)i.next();
                if(s.getServiceType() != null &&
                        s.getServiceType().equals(service.getServiceType()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    public UPnPManagedDevice getParentDevice()
    {
        return m_parentDevice;
    }

    public UPnPManagedService createService(String serviceType, InputStream description, UPnPActionHandler handler)
            throws IOException, SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        if(description == null)
        {
            throw new IllegalArgumentException("Null InputStream for description");
        }

        if(serviceType == null)
        {
            // TODO : spec clarification issue, should this throw an IllegalArgumentException as well.
            return null;
        }

        UPnPManagedService service = new UPnPManagedServiceImpl(this, serviceType, Utils.toString(description));
        if (handler != null)
        {
            service.setActionHandler(handler);
        }
        
        addService(service);

        return service;
    }
    
    public void setMediaDevice(boolean mediaDevice)
    {
        m_mediaDevice = mediaDevice;
    }

    private boolean getAlive()
    {
        return isAlive;
    }
    
    /**
     * 
     * @param a Array of InetAddresses that are possibly missing 
     * @param b Array of InetAddresses be compared against
     * @return array of all InetAddresses that are in array a, but not in array b.
     */
    private InetAddress[] getMissingAddresses(InetAddress[] a, InetAddress[] b)
    {
        // If no a, none missing
        if(a == null || a.length == 0)
        {
            return new InetAddress[0];
        }
               
        Set missingAddresses = new HashSet();
        if(b == null || b.length == 0)
        {
            missingAddresses.addAll(Arrays.asList(a));
        }
        else
        {
            Set newSet = new HashSet(Arrays.asList(b));
            for(int i = 0; i < b.length; i++)
            {
                if(!newSet.contains(b[i]))
                {
                    missingAddresses.add(b[i]);
                }
            }
        }
        
        return (InetAddress[])missingAddresses.toArray(new InetAddress[0]);
    }
    
    // Serve icon data requests
    private class IconInterceptor implements Interceptor
    {
        public boolean intercept(HTTPRequest httpReq)
        {
            UPnPManagedDeviceIcon icon = null;
            
            if(httpReq.getURI() != null &&
                    httpReq.getURI().startsWith(MediaServer.ICON_REQUEST_URI_PREFIX))
            {
                try 
                {
                    int id = Integer.parseInt(httpReq.getURI().substring(MediaServer.ICON_REQUEST_URI_PREFIX.length()));
                    
                    UPnPDeviceManager devManager = UPnPDeviceManager.getInstance();
                    UPnPManagedDevice[] devices = devManager.getDevices();
                    for(int i = 0; i < devices.length; i++)
                    {
                        UPnPManagedDeviceIcon[] icons = devices[i].getIcons();
                        for(int x = 0; x < icons.length; x++)
                        {
                            if(id == icons[x].hashCode())
                            {
                                icon = icons[x];
                                break;
                            }
                        }
                        
                        if(icon != null)
                        {
                            break;
                        }
                    }
                    
                    if(icon == null)
                    {
                        if(log.isDebugEnabled())
                        {
                            log.debug("Icon not found for uri: " + httpReq.getURI());
                        }
                        return false;
                    }                   

                    HTTPResponse resp = new HTTPResponse(UPnP.SERVER);
                    resp.setContentType(icon.getMimeType());
                    if(icon.getData() != null)
                    {
                        resp.setContent(icon.getData(), true);
                    }
                    resp.setStatusCode(ActionStatus.HTTP_OK.getCode());
                    httpReq.post(resp);                    
                }
                    
                catch(NumberFormatException nfe)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Incoming icon request is malformed " + httpReq.getURI());
                    }
                }                
                
                return true;
            }
            return false;
        }
        
    }
}

