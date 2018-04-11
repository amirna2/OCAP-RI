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
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.client.UPnPClientDeviceImpl;
import org.ocap.hn.Device;
import org.ocap.hn.DeviceEvent;
import org.ocap.hn.DeviceEventListener;
import org.ocap.hn.HomeNetPermission;
import org.ocap.hn.NetList;
import org.ocap.hn.NetModule;
import org.ocap.hn.recording.RecordingNetModule;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientDeviceListener;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPClientStateVariable;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.client.UPnPStateVariableListener;
import org.ocap.hn.upnp.common.UPnPDevice;
import org.ocap.hn.upnp.server.UPnPManagedDevice;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.util.SecurityUtil;

/**
 * DeviceImpl - implementation class for <code>Device</code>.
 * 
 * @author Ben Foran (Flashlight Engineering and Consulting)
 * 
 * @version $Revision$
 * 
 * @see {@link org.ocap.hn.Device}
 */
public class DeviceImpl implements Device, UPnPClientDeviceListener, UPnPStateVariableListener
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(DeviceImpl.class);

    private static final int FRIENDLY_NAME_MAX_LEN = 63;

    private static final HomeNetPermission CONTENT_MANAGEMENT_PERMISSION = new HomeNetPermission("contentmanagement");

    private UPnPClientDevice uDevice = null;

    private static UPnPControlPoint ucp = null;

    private Properties deviceProperties = null;
    
    // Registered HN Ext Device Event listeners
    private final Vector deviceListeners = new Vector();

    public DeviceImpl(UPnPClientDevice uDevice)
    {
        this.uDevice = uDevice;
        this.deviceProperties = getDeviceProperties();
    }

    private NetListImpl getLocalNetModuleList()
    {
        NetManagerImpl nMgr = NetManagerImpl.instance();
        NetListImpl netModuleList = new NetListImpl();
        UPnPClientService uServices[] = uDevice.getServices();
        for (int i = 0; i < uServices.length; i++)
        {
            NetModule netMod = nMgr.createNetModule(uServices[i]);
            if (netMod != null)
            {
                netModuleList.add(netMod);
            }
        }
        
        return netModuleList;        
    }
    
    public Enumeration getCapabilities()
    {
        return getDeviceCapabilities().elements();
    }

    public InetAddress getInetAddress()
    {
        return uDevice.getInetAddress();
    }

    public Enumeration getKeys()
    {
        return deviceProperties.keys();
    }

    public String getName()
    {
        return uDevice.getFriendlyName();
    }

    public NetModule getNetModule(String moduleId)
    {
        NetListImpl netModuleList = getLocalNetModuleList();
        Enumeration netModuleEnumeration = netModuleList.getElements();

        while (netModuleEnumeration.hasMoreElements())
        {
            NetModule netModule = (NetModule) netModuleEnumeration.nextElement();
            if (netModule.getNetModuleId().equals(moduleId))
            {
                return netModule;
            }
        }
        return null;
    }

    public NetList getNetModuleList()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getNetModuleList() - called for device: " + getName());
        }
        return getLocalNetModuleList();
    }

    public Device getParentDevice()
    {
        UPnPDevice parent = uDevice.getParentDevice();
        if (parent instanceof UPnPClientDevice)
        {
            return new DeviceImpl((UPnPClientDevice)parent);
        }
        return null;
    }

    public String getProperty(String key)
    {
        return deviceProperties.getProperty(key);
    }

    public String getUUID()
    {
        return uDevice.getUDN();
    }

    public NetList getSubDevices()
    {
        /**
         * Create a new NetList that will be populated with all of the sub
         * devices as well as the sub devices of the sub devices and so on.
         */
        NetListImpl allSubDevicesList = new NetListImpl();

        return addSubDevicesToList(this, allSubDevicesList);
    }

    public String getType()
    {
        String upnpDeviceType = uDevice.getDeviceType();
        
        //If the deviceType is empty or not present, UPnPDevice.getDeviceType() returns an 
        // empty String
        if (upnpDeviceType == null || upnpDeviceType.length() == 0)
        {
            return "";
        }
        
        /*
         * upnpDeviceType =
         * "urn:schemas-blah-blah-blah:device:actual_device_type:version"
         */
        /* endPos = ^ */
        final int endPos = upnpDeviceType.lastIndexOf(":");

        /*
         * upnpDeviceType =
         * "urn:schemas-blah-blah-blah:device:actual_device_type:version"
         */
        /* startPos = ^ */
        final int startPos = upnpDeviceType.lastIndexOf(":", endPos - 1);

        /* hnDeviceType = "actual_device_type" */
        final String hnDeviceType = upnpDeviceType.substring(startPos + 1, endPos);

        return hnDeviceType;
    }

    public String getVersion()
    {
        /**
         * The version value is the last numerical term in the deviceType
         * element which is obtained by making a call to
         * <code>UPnPDevice.getDeviceType()</code>.
         */
        int subStringIndex = 0;

        String deviceType = uDevice.getDeviceType();

        // Convert deviceType from String to char array to determine the device
        // version
        char[] deviceTypeChars = deviceType.toCharArray();

        for (int i = deviceTypeChars.length - 1; i >= 0; i--)
        {
            // if the current char is neither a number or a decimal point then
            // stop
            if (!Character.isDigit(deviceTypeChars[i]) && deviceTypeChars[i] != '.')
            {
                subStringIndex = i + 1;
                break;
            }
        }

        return deviceType.substring(subStringIndex);
    }

    /**
     * Determine if this device is local by comparing with MediaServer device & embedded devices.
     * 
     * @return  true if this is a local device, false otherwise
     */
    public boolean isLocal()
    {
        // Check that the media server is up and running, otherwise
        // this did not come from local server.
        if(MediaServer.getInstance().getRootDevice() == null)
        {
            return false;
        }
        
        String udnLocal = MediaServer.getInstance().getRootDevice().getUDN();
        if (uDevice.getUDN().equals(udnLocal))
        {
            return true;
        }
        
        // Check if it matches one of the root devices embedded devices
        UPnPManagedDevice embeddedDevices[] = MediaServer.getInstance().getRootDevice().getEmbeddedDevices();
        {
            for (int i = 0; i < embeddedDevices.length; i++)
            {
                if (uDevice.getUDN().equals(embeddedDevices[i].getUDN()))
                {
                    return true;
                }  
             }
        }
        return false;
    }

    public void addDeviceEventListener(DeviceEventListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addDeviceEventListener - called for device: " + uDevice.getFriendlyName());
        }
        synchronized (deviceListeners)
        {
            // Make sure this device isn't already in list
            if (!deviceListeners.contains(listener))
            {
                deviceListeners.add(listener);
                
                if (deviceListeners.size() == 1)
                {
                    // Add self as device listener to receive device removed notifications
                    if (ucp == null)
                    {
                        ucp = UPnPControlPoint.getInstance();
                    }
                    ucp.addDeviceListener(this);

                    // Get all services and register as listener in order to get service related events
                    UPnPClientService services[] = uDevice.getServices();
                    for (int j = 0; j < services.length; j++)
                    {
                        services[j].addStateVariableListener(this);
                    }
                }                
            }
        }
    }

    public void removeDeviceEventListener(DeviceEventListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removeDeviceEventListener - " + listener.toString());
        }

        synchronized (deviceListeners)
        {
            deviceListeners.remove(listener);
            
            if (deviceListeners.size() == 0)
            {
                // Remove self as device listener 
                if (ucp == null)
                {
                    ucp = UPnPControlPoint.getInstance();
                }
                ucp.removeDeviceListener(this);

                // Get all services and unregister as listener
                UPnPClientService services[] = uDevice.getServices();
                for (int j = 0; j < services.length; j++)
                {
                    services[j].removeStateVariableListener(this);

                }
            }
        }       
    }

    /**
     * Notifies the listener that a UPnP device was added to a home network.
     *
     * @param device The <code>UPnPDevice</code> that was added.
     */
    public void notifyDeviceAdded(UPnPClientDevice device)
    {
        // Nothing to do here
    }
    
    /**
     * Notifies the listener that a UPnP device was removed from a 
     * home network, or did not renew its advertisement prior to 
     * expiration of the prior advertisement. 
     *
     * @param device The <code>UPnPDevice</code> that was removed.
     */
    public void notifyDeviceRemoved(UPnPClientDevice device)
    {
        // Determine if this is the upnp device associated with this hn device
        if (device.getUDN().equals(uDevice.getUDN()))
        {
            // this is for this device, notify listeners of event
            notifyDeviceEventListeners(new DeviceEvent(DeviceEvent.DEVICE_REMOVED, this));
        }        
    }
    
    /**
     * Notifies the listener that the value of the UPnP state variable being
     * listened to has changed.
     *
     * @param variable The UPnP state variable that changed.
     */
    public void notifyValueChanged(UPnPClientStateVariable variable)
    {
        // Determine if variable is associated with underlying service
        UPnPClientService service = variable.getService();
        if (service.getDevice().getUDN().equals(uDevice.getUDN()))
        {
            // State variable of underlying service on this device has changed, send net module event
            if (log.isDebugEnabled())
            {
                log.debug("notifyValueChanged() - notifying listeners device state changed, device: " +
                        uDevice.getFriendlyName() + ", service: " + service.getServiceId());
            }
            notifyDeviceEventListeners(new DeviceEvent(DeviceEvent.STATE_CHANGE, this));
        }                
    }

    public void notifySubscribed(UPnPClientService service)
    {
        // Nothing to do here
    }

    public void notifyUnsubscribed(UPnPClientService service)
    {
        // Nothing to do here
    }

    /**
     * notifyDeviceEventListeners - method for informing all interested
     * listeners that the <code>Device</code> has been updated.
     */
    private void notifyDeviceEventListeners(DeviceEvent event)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyDeviceEventListeners() called for device: " +
                    uDevice.getFriendlyName() + ", event type: " + event.getType());
        }
        
        synchronized (deviceListeners)
        {
            for (int i = 0; i < deviceListeners.size(); i++)
            {
                DeviceEventListener listener = (DeviceEventListener)deviceListeners.get(i);
                listener.notify(event);
            }
        }    
    }
    
    /**
     * isRootDevice - determines if this <code>Device</code> is a root
     * <code>Device</code>.
     * 
     * @return a boolean value indicating if the if the <code>Device</code> is
     *         the root <code>Device</code>. A return value of TRUE indicates
     *         that the <code>Device</code> is the root and FALSE indicates that
     *         the <code>Device</code> is not the root.
     */
    boolean isRootDevice()
    {
        return uDevice.isRootDevice();
    }

    /**
     * addSubDevicesToList - recursively iterate through all of the immediate
     * sub <code>Devices</code> of the passed in <code>Device</code> to find
     * their sub <code>Devices</code>.
     * 
     * @param device
     *            - the <code>Device</code> that will be queried for its sub
     *            <code>Devices</code>.
     * @param subDevices
     *            - the <code>NetList</code> to add all additional sub
     *            <code>Devices</code> to.
     * 
     * @return a <code>NetList</code> that contains all of the sub
     *         <code>Devices</code> for the <code>Device</code> that was passed
     *         in.
     */
    private NetList addSubDevicesToList(DeviceImpl device, NetListImpl subDevices)
    {
        UPnPClientDevice ueDevices[] = device.uDevice.getEmbeddedDevices();
        for (int i = 0; i < ueDevices.length; i++)
        {
            DeviceImpl subDevice = new DeviceImpl(ueDevices[i]);
            subDevices.add(subDevice);     
            subDevices = (NetListImpl)addSubDevicesToList(subDevice, subDevices);            
        }
        
        return subDevices;
    }

    /**
     * getDeviceProperties - method used for obtaining all of the properties for
     * this <code>Device</code>.
     * 
     * @return a <code>Properties</code> object which contains all of the key,
     *         value pairs for the properties of this <code>Device</code>.
     */
    private Properties getDeviceProperties()
    {
        Properties deviceProperties = new Properties();

        deviceProperties.put(Device.PROP_FRIENDLY_NAME, uDevice.getFriendlyName());
        deviceProperties.put(Device.PROP_MANUFACTURER, uDevice.getManufacturer());
        deviceProperties.put(Device.PROP_MANUFACTURER_URL, uDevice.getManufacturerURL());
        deviceProperties.put(Device.PROP_MODEL_DESCRIPTION, uDevice.getModelDescription());
        deviceProperties.put(Device.PROP_MODEL_NAME, uDevice.getModelName());
        deviceProperties.put(Device.PROP_MODEL_NUMBER, uDevice.getModelNumber());
        deviceProperties.put(Device.PROP_MODEL_URL, uDevice.getModelURL());
        deviceProperties.put(Device.PROP_SERIAL_NUMBER, uDevice.getSerialNumber());
        deviceProperties.put(Device.PROP_UDN, uDevice.getUDN());
        deviceProperties.put(Device.PROP_UPC, uDevice.getUPC());
        deviceProperties.put(Device.PROP_PRESENTATION_URL, uDevice.getPresentationURL());
        deviceProperties.put(Device.PROP_LOCATION, ((UPnPClientDeviceImpl)uDevice).getLocation());
        deviceProperties.put(Device.PROP_MIDDLEWARE_PROFILE, ((UPnPClientDeviceImpl)uDevice).getMiddlewareProfile());
        deviceProperties.put(Device.PROP_MIDDLEWARE_VERSION, ((UPnPClientDeviceImpl)uDevice).getMiddlewareVersion());
        deviceProperties.put(Device.PROP_DEVICE_TYPE, getType());
        deviceProperties.put(Device.PROP_DEVICE_VERSION, getVersion());

        return deviceProperties;
    }

    /**
     * getDeviceCapabilities - method used for determining the capabilities of
     * this <code>Device</code>. If this <code>Device</code> is a root device
     * than it also contains all of the capabilities of its sub-devices.
     * 
     * @return a <code>Vector</code> object that contains all of the
     *         capabilities for this <code>Device</code>.
     */
    private Vector getDeviceCapabilities()
    {
        // Create the list of capabilities for this device
        Vector capabilities = new Vector();
        NetListImpl netModuleList = getLocalNetModuleList();
        Enumeration netModuleEnumeration = netModuleList.elements();

        while (netModuleEnumeration.hasMoreElements())
        {
            NetModule netModule = (NetModule) netModuleEnumeration.nextElement();

            if (netModule instanceof RecordingNetModule)
            {
                capabilities.add(Device.CAP_RECORDING_SUPPORTED);

                break;
            }
        }
       
        if (Device.TYPE_MEDIA_SERVER.equals(getType()))
        {
            capabilities.add(Device.CAP_STREAMING_SUPPORTED);
        }

        // *TODO* - investigate this because it seems kind of hacky, is it really needed?
        // Also specified to advertised device
        if (((UPnPClientDeviceImpl)uDevice).getOcapHomeNetworkString().startsWith("OC-DMS"))
        {
            capabilities.add(Device.CAP_REMOTE_STORAGE_SUPPORTED);
            capabilities.add(Device.CAP_TUNER_SUPPORTED);
        }

        if (isRootDevice())
        {
            /**
             * Obtain all sub-device capabilities and add them to the list of
             * capabilities for this device since it is the root device.
             */
            NetList subDevices = this.getSubDevices();

            // Obtain the list of sub-devices for this root device
            Enumeration subDevicesEnumeration = subDevices.getElements();

            while (subDevicesEnumeration.hasMoreElements())
            {
                // Obtain the list of capabilities for this particular
                // sub-device
                Enumeration subDeviceCapabilities = ((Device) subDevicesEnumeration.nextElement()).getCapabilities();

                while (subDeviceCapabilities.hasMoreElements())
                {
                    String subDeviceCapability = (String) subDeviceCapabilities.nextElement();

                    if (!capabilities.contains(subDeviceCapability))
                    {
                        /**
                         * If the list of capabilities for the root device does
                         * not already contain this particular capability then
                         * add it.
                         */
                        capabilities.add(subDeviceCapability);
                    }
                }
            }
        }

        return capabilities;
    }

    public void setFriendlyName(String value)
    {
        if (value == null)
        {
            value = "";
        }

        if (value.equals(getProperty(PROP_FRIENDLY_NAME)) == true)
        {
            return;
        }

        if (value.length() > FRIENDLY_NAME_MAX_LEN)
        {
            throw new IllegalArgumentException("Device friendly name length cannot exceed " + FRIENDLY_NAME_MAX_LEN
                    + " characters.");
        }

        if (isLocal() == false)
        {
            throw new UnsupportedOperationException("Device " + getName() + " is not local.");
        }

        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);

        final String newFriendlyName = value;

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        ccm.getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                MediaServer.getInstance().getRootDevice().sendByeBye();
                MediaServer.getInstance().getRootDevice().setFriendlyName(newFriendlyName);
                MediaServer.getInstance().getRootDevice().sendAlive();
            }
        });
    }
    
    public String toString()
    {
        return uDevice.getModelDescription() + ", local: " + isLocal();
    }
}
