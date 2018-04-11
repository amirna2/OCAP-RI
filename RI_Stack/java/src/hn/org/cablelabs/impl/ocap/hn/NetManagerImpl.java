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

import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Enumeration;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.Device;
import org.ocap.hn.DeviceEvent;
import org.ocap.hn.DeviceEventListener;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.NetModule;
import org.ocap.hn.NetModuleEvent;
import org.ocap.hn.NetModuleEventListener;
import org.ocap.hn.PropertyFilter;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientDeviceListener;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPClientStateVariable;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.client.UPnPStateVariableListener;

import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.HomeNetworkingManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.media.mpe.HNAPI;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.service.ServicesDatabase.BootProcessCallback;
import org.cablelabs.impl.service.ServicesDatabase.ShutdownCallback;
import org.cablelabs.impl.util.MPEEnv;

/**
 * NetManagerImpl - implementation class for <code>NetManager</code>.
 * 
 * @author Ben Foran (Flashlight Engineering and Consulting)
 * 
 * @version $Revision$
 * 
 * @see {@link org.ocap.hn.NetManager}
 */
public class NetManagerImpl extends NetManager 
implements HomeNetworkingManager, UPnPClientDeviceListener, UPnPStateVariableListener
{

    // Config parameter which specifies the value to use for the MX parameter
    // in M-SEARCH requests.  The MX parameter is the max wait time in seconds
    // to delay in sending a response. 
    public static final int SEARCH_MX =
        Integer.parseInt(PropertiesManager.getInstance().getProperty("OCAP.hn.ssdp.searchMX","15"));

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(NetManagerImpl.class);

    private static NetManagerImpl instance = null;

    private static UPnPControlPoint ucp = null;
    
    private static Device localDevice = null;

    // *TODO* - just have single instances of local services instead of NetModuleFactory?
    private static NetModule m_localSRS; 
    private static ContentServerNetModuleImpl m_localCDS;
    private static NetModuleImpl m_localCMS;
    
    
    // Registered HN Ext device listeners
    private final Vector deviceListeners = new Vector();

    // Registered HN Ext Net Module Event listeners
    private final Vector netModuleListeners = new Vector();
    
    // Flag indicating if currently registered as UPnPControl Point device listener
    private boolean isRegisteredAsUcpListener = false;

    // List of profiles/protocols supported by the platform
    String[] m_platformSupportedProfiles;
    String[][] m_platformSupportedMimeTypes;
    HNStreamProtocolInfo[] m_platformSupportedPlaybackProtocols;
    
    private NetManagerImpl()
    {
        if (log.isInfoEnabled())
        {
            log.info("Instance of NetManagerImpl created.");
        }


        // Initiate creation of local UPnP Control Point
        ucp = UPnPControlPoint.getInstance();

        // Register to receive notifications in order to determine when local device is found
        ucp.addDeviceListener(this);
        
        // Set flag so don't register multiple times
        isRegisteredAsUcpListener = true;
        
        registerMediaServerStartup();

        loadPlatformProtocolInfo();
        
		// Set up EDListener to get IP address changes from mpeos
		ManagerManager.getInstance(EventDispatchManager.class);
       
        String linkLocalInterface = MediaServer.getInstance().getLinkLocalInterface(); 
		setInterfaceListener(linkLocalInterface, new EDListener() 
		{
			public synchronized void asyncEvent(int eventCode, int eventData1, int eventData2)
			{
				notifyIPChange(eventCode, eventData2);
			}
		});
		
    }
    
    /**
     * At startup, we wait for our local device to be discovered
     */
    private void waitForLocalServer()
    {
        /**
         * Make sure the local device can be found prior to completing start up.
         * Get the config value for the max amount of time in seconds to wait
         * for the local device to be found. The default is 15 seconds.
         */
        long maxSelfDiscoveryTimeoutMS = 15 * 1000;
        try
        {
            maxSelfDiscoveryTimeoutMS = MPEEnv.getEnv("OCAP.hn.MaxSelfDiscoveryTimeoutMS", 15000L);
        }
        catch (Throwable t)
        {
            if (log.isErrorEnabled())
            {
                log.error("NetManagerImpl() - Exception while initializing maxStartupTimeSecs: ",t);
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug("NetManagerImpl() - MaxSelfDiscoveryTimeoutMS initialized to: " + 
            maxSelfDiscoveryTimeoutMS);
        }

        /**
         * Request an update to the device list which will request a search of devices on network
         */
        //deviceList.update();
        ucp.search(SEARCH_MX);
        
        /**
         * Wait here for notification through callback of DeviceList that local device 
         * has been found.  Wait will timeout if configurable amount of time has expired.
         */
        try
        {
            synchronized (this)
            {
                wait(maxSelfDiscoveryTimeoutMS);
            }
        }
        catch (InterruptedException e)
        {
            // ignore exception
        }
        
        // If unable to find local device after specified timeout, log error 
        if (localDevice == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("HOME NETWORKING IS INOPERATIVE: no local device with media server can be found " +
                        "within max startup time of " + maxSelfDiscoveryTimeoutMS + " msecs");
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("HOME NETWORKING IS OPERATIVE");            
            }            
            ucp.search(SEARCH_MX);
        }
    }
    
    private void registerMediaServerStartup()
    {
        if (log.isInfoEnabled())
        {
            log.info("MediaServer waiting for monitor & autostart app startup...");            
        }

        // Listen for configured signal
        ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        sm.getServicesDatabase().addBootProcessCallback(
                new BootProcessCallback()
                {
                    /**
                     * {@inheritDoc}
                     */
                    public void monitorApplicationStarted()
                    {
                        // We only care about initialUnboundAutostartApplicationsStarted()...
                    } // END monitorApplicationStarted()
                    
                    /**
                     * {@inheritDoc}
                     */
                    public void initialUnboundAutostartApplicationsStarted()
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("received initialUnboundAutostartApplicationsStarted notification");
                        }

                        if (log.isInfoEnabled())
                        {
                            log.info("Initial app startup complete.  Installing MediaServer...");
                        }
                        // Run VPOP setup on the system thread pool (not in callback context)
                        CallerContextManager ccm = (CallerContextManager)ManagerManager.getInstance(CallerContextManager.class);
                        ccm.getSystemContext().runInContextAsync(new Runnable()
                            {
                                public void run()
                                {
                                    MediaServer.getInstance().startMediaServer();
                                    ucp.search(SEARCH_MX);
                                }
                            } );
                    } // END unboundAutostartApplicationsStarted()
                    
                    /**
                     * {@inheritDoc}
                     */
                    public boolean monitorApplicationShutdown(ShutdownCallback callback)
                    {
                        // We'll keep HN running even across IMA restarts
                        return false;
                    }
                } ); // END anonymous BootProcessCallback
    }
        
    /**
     * instance - gets the singleton instance of this class.
     * 
     * @return the singleton NetManagerImpl instance.
     */
    public static NetManagerImpl instance()
    {
        if (instance == null)
        {
            instance = new NetManagerImpl();
        }

        return instance;
    }

    public void addDeviceEventListener(DeviceEventListener listener)
    {        
        synchronized (ucp)
        {
            // Put this listener in local list to be notified when UPnPControlPoint notification
            // is received
            deviceListeners.add(listener);
            
            // Register with UPnPControlPoint directly in order to maintain application context
            registerAsUcpListener();            
        }
    }

    public void addNetModuleEventListener(NetModuleEventListener listener)
    {
        synchronized (ucp)
        {
            // Put this listener in local list to be notified when UPnPControlPoint notification
            // is received
            netModuleListeners.add(listener);
            
            // Register with UPnPControlPoint directly in order to maintain application context
            registerAsUcpListener();            
        }
    }

    private void registerAsUcpListener()
    {
        if (!isRegisteredAsUcpListener)
        {
            // Add self as device listener to receive device added & removed notifications
            // Also add self as state variable change listener to propagate device changed events
            ucp.addDeviceListener(this);

            // Get all services and register as listener in order to get service related events
            UPnPClientDevice devices[] = ucp.getDevices();
            for (int i = 0; i < devices.length; i++)
            {
                UPnPClientService services[] = devices[i].getServices();
                for (int j = 0; j < services.length; j++)
                {
                    services[j].addStateVariableListener(this);

                }
            }
            isRegisteredAsUcpListener = true;
        }
    }
    
    private void unregisterAsUcpListener()
    {
        if ((isRegisteredAsUcpListener) && (localDevice != null) &&
                (deviceListeners.size() == 0) && (netModuleListeners.size() == 0))
        {
            // Remove self as device listener 
            ucp.removeDeviceListener(this);

            // Get all services and unregister as listener
            UPnPClientDevice devices[] = ucp.getDevices();
            for (int i = 0; i < devices.length; i++)
            {
                UPnPClientService services[] = devices[i].getServices();
                for (int j = 0; j < services.length; j++)
                {
                    services[j].removeStateVariableListener(this);

                }
            }
            isRegisteredAsUcpListener = false;
        }        
    }
    
    /**
     * Gets the device by friendly name
     */
    public Device getDevice(String name)
    {
        Device device = null;
        
        // Get all devices 
        UPnPClientDevice[] devices = ucp.getDevices();
        for (int i = 0; i < devices.length; i++)
        {
            if (devices[i].getFriendlyName().equalsIgnoreCase(name))
            {
                device = new DeviceImpl(devices[i]);
                break;
            }
        }
        return device;
    }

    /**
     * Gets the device by UDN
     * @param uuid
     * @return
     */
    public DeviceImpl getDeviceByUUID(String uuid)
    {
        DeviceImpl device = null;
        UPnPClientDevice[] devices = ucp.getDevicesByUDN(uuid);
        if (devices.length > 0)
        {
            device = new DeviceImpl(devices[0]);
        }
        return device;
    }

    public NetList getDeviceList(PropertyFilter filter)
    {
        NetListImpl deviceNetList = new NetListImpl();
        
        // Get all the devices from control point
        UPnPClientDevice[] devices = ucp.getDevices();

        for (int i = 0; i < devices.length; i++)
        {
            deviceNetList.add(new DeviceImpl(devices[i]));
        }

        return deviceNetList.filterElement(filter);
    }

    public NetList getDeviceList(String name)
    {
        NetListImpl deviceNetList = new NetListImpl();
        if (name == null || "".equals(name))
        {
            return deviceNetList;
        }
        
        // Get all the devices from control point
        UPnPClientDevice[] devices = ucp.getDevices();

        for (int i = 0; i < devices.length; i++)
        {
            if (name.equalsIgnoreCase(devices[i].getFriendlyName()))
            {
                deviceNetList.add(new DeviceImpl(devices[i]));
            }
        }

        return deviceNetList;
    }

    public NetModule getNetModule(String deviceName, String moduleID)
    {
        // Find the device with this name
        NetModule service = null;
        UPnPClientDevice device = null;
        UPnPClientDevice[] devices = ucp.getDevices();
        for (int i = 0; i < devices.length; i++)
        {
            if (devices[i].getFriendlyName().equalsIgnoreCase(deviceName))
            {
                device = devices[i];
                break;
            }
        }
 
        // Find the service with this id
        if (device != null)
        {
            UPnPClientService[] services = device.getServices();
            for (int i = 0; i < services.length; i++)
            {
                if (services[i].getServiceId().equals(moduleID))
                {
                    service = createNetModule(services[i]);
                    break;
                }
            }
        }
        return service;
    }

    /**
     * Return NetModule representation of service which matches supplied 
     * module ID for the given device.  If module ID is a CDS, a ContentServerNetModule
     * is created.
     * 
     * @param device    look for services associated with this device
     * @param moduleID  find service whose service ID matches this value
     * 
     * @return  matching NetModule, ContentServerNetModule if module ID is for a CDS
     */
    public NetModule getNetModule(UPnPClientDevice device, String moduleID)
    {
        // Find the service with this id
        NetModule netModule = null;
        if (device != null)
        {
            UPnPClientService[] services = device.getServices();
            for (int i = 0; i < services.length; i++)
            {
                if (services[i].getServiceId().equals(moduleID))
                {
                    netModule = createNetModule(services[i]);
                     break;
                }
            }
        }
        return netModule;
    }

    public ContentServerNetModule getLocalCDS()
    {
        // *TODO* - is this OK? what if it is NULL?
        return m_localCDS;
    }
    
    public NetList getNetModuleList(PropertyFilter filter)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getNetModuleList() - called");
        }
        // NetList that will be populated and returned to the caller
        NetListImpl netModuleNetList = new NetListImpl();

        // Get all the devices from control point
        UPnPClientDevice[] devices = ucp.getDevices();

        // Get all the services associated with all devices
        for (int i = 0; i < devices.length; i++)
        {
            UPnPClientService[] services = devices[i].getServices();
            for (int j = 0; j < services.length; j++)
            {
                netModuleNetList.add((createNetModule(services[j])));
            }
        }

        return netModuleNetList.filterElement(filter);
    }

    public NetList getNetModuleList(String deviceName, String moduleID)
    {
        if (log.isDebugEnabled())
        {
            log.debug("getNetModuleList() - called");
        }
        // NetList that will be populated and returned to the caller
        NetListImpl netModuleList = new NetListImpl();

        // Get all the devices from control point
        UPnPClientDevice[] devices = ucp.getDevices();

        // if deviceName == null or empty and moduleID == null or empty
        // return NetModules of all devices and subdevices
        if ((deviceName == null || "".equals(deviceName)) &&
            (moduleID == null || "".equals(moduleID)) )
        {
           // return all known NetModules
           return getNetModuleList((PropertyFilter)null); 
        }

        // if deviceName == null or empty and moduleID is non null
        if ((deviceName == null || "".equals(deviceName)) &&
            (moduleID != null))
        {
            //  return all known NetModules that match moduleID
            for (int i = 0; i < devices.length; i++)
            {
                UPnPClientService[] services = devices[i].getServices();
                for (int j = 0; j < services.length; j++)
                {
                    if (services[j].getServiceId().equals(moduleID))
                    {
                        netModuleList.add((createNetModule(services[j])));
                    }
                }
            }
            return netModuleList;
        }
                
        // if deviceName is non null and moduleID is null or empty
        if ((deviceName != null) && (moduleID == null || "".equals(moduleID)))
        {
            // return all NetModules for all devices that match
	        for (int i = 0; i < devices.length; i++)
	        {
	            if (deviceName.equals(devices[i].getFriendlyName()))
	            {
		            UPnPClientService[] services = devices[i].getServices();
		            for (int j = 0; j < services.length; j++)
		            {
			            netModuleList.add((createNetModule(services[j])));
		            }
		        }
	        }
            return netModuleList;
	    }

        // otherwise match on deviceName and moduleID 
        for (int i = 0; i < devices.length; i++)
        {
            if (deviceName.equalsIgnoreCase(devices[i].getFriendlyName()))
            {
                // found devicename..look for module id
                UPnPClientService[] services = devices[i].getServices();
                for (int j = 0; j < services.length; j++)
                {
                    if (services[j].getServiceId().equals(moduleID))
                    {
                        netModuleList.add((createNetModule(services[j])));
                    }
                }
            }
        }

        return netModuleList;
    }

    public void removeDeviceEventListener(DeviceEventListener listener)
    {
        synchronized (ucp)
        {
            deviceListeners.remove(listener);
            
            // If no more listeners are registered, unregister self as listener
            unregisterAsUcpListener();
        }
    }

    public void removeNetModuleEventListener(NetModuleEventListener listener)
    {
        synchronized (ucp)
        {
            netModuleListeners.remove(listener);

            // If no more listeners are registered, unregister self as listener
            unregisterAsUcpListener();
        }
    }

    /**
     * Requests that the NetManager proactively refresh its local database of
     * connected devices. This operation will be performed asynchronously. Any
     * listeners registered with the NetManager changes to connected Devices or
     * NetModules will be notified of any changes discovered during this
     * process.
     */
    public void updateDeviceList()
    {
        if (log.isDebugEnabled())
        {
            log.debug("updatedDeviceList() - called");
        }
        // Initiate refresh via UPnPControlPoint
        ucp.search(SEARCH_MX);
    }

    public void destroy()
    {

    }
    
    /**
     * Notifies the listener that a UPnP device was added to a home network.
     *
     * @param device The <code>UPnPDevice</code> that was added.
     */
    public void notifyDeviceAdded(UPnPClientDevice device)
    {
        // If local device has not yet been found, see if it has been found
        if (localDevice == null)
        {
            updateDevices();
        }
        
        notifyDeviceEventListeners(new DeviceEvent(DeviceEvent.DEVICE_ADDED, new DeviceImpl(device)));
        
        UPnPClientService services[] = device.getServices();
        if (log.isDebugEnabled())
        {
            log.debug("notifyDeviceAdded() - added device " + device.getFriendlyName() + 
                    ", service cnt: " + services.length);
        }
        for (int i = 0; i < services.length; i++)
        {
            notifyNetModuleEventListeners(new NetModuleEvent(NetModuleEvent.MODULE_ADDED,
                    createNetModuleInstance(services[i])));
        }
        
        // Add self as listener for any services on new device if any listeners are registered
        synchronized (ucp)
        {
            if ((deviceListeners.size() > 0) || (netModuleListeners.size() > 0))
            {
                for (int i = 0; i < services.length; i++)
                {
                    services[i].addStateVariableListener(this);
                }
            }
        }
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
        if (log.isDebugEnabled())
        {
            log.debug("notifyDeviceRemoved() - removed device " + device.getFriendlyName());
        }
        notifyDeviceEventListeners(new DeviceEvent(DeviceEvent.DEVICE_REMOVED, new DeviceImpl(device)));

        UPnPClientService services[] = device.getServices();
        for (int i = 0; i < services.length; i++)
        {
            notifyNetModuleEventListeners(new NetModuleEvent(NetModuleEvent.MODULE_REMOVED, 
                    createNetModuleInstance(services[i])));
        }
        
        // Remove self as listener on any services of the device which has been removed
        synchronized (ucp)
        {
            for (int i = 0; i < services.length; i++)
            {
                services[i].removeStateVariableListener(this);
            }
        }        
    }

    public void notifyValueChanged(UPnPClientStateVariable variable)
    {
        UPnPClientService service = variable.getService();
        UPnPClientDevice device = service.getDevice();
        if (log.isDebugEnabled())
        {
            log.debug("notifyValueChanged() - changed device " + device.getFriendlyName());
        }
        notifyDeviceEventListeners(new DeviceEvent(DeviceEvent.STATE_CHANGE, 
                new DeviceImpl(device)));

        notifyNetModuleEventListeners(new NetModuleEvent(NetModuleEvent.STATE_CHANGE, 
                new NetModuleImpl(service)));
    }

    public void notifySubscribed(UPnPClientService service)
    {
        // Nothing to do here
    }
    public void notifyUnsubscribed(UPnPClientService service)
    {
        // Nothing to do here
    }

    private void updateDevices()
    {
        if (log.isInfoEnabled())
        {
            log.info("updateDevices() - called");
        }
        synchronized (this)
        {
            // Always get the device list since this causes the UPnPControlPoint to add embedded devices
            NetList netList = getDeviceList((PropertyFilter) null);
            
            // Look for local device if not discovered yet
            if (localDevice == null)
            {
                for (Enumeration netListEnum = netList.getElements(); netListEnum.hasMoreElements();)
                {
                    Device device = (Device) netListEnum.nextElement();
                    if (device.isLocal())
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn("notifyDeviceEvent() - local device found, HN will now operate");
                        }
                        localDevice = device;
                        
                        // Unregister as listener to prevent data leak if no listeners registered
                        unregisterAsUcpListener();

                        break;
                    }
                }
            }

            // If local device has now been found, done waiting so send notification
            if (localDevice != null)
            {
                // notify that local device has been found
                notifyAll();
                ucp.search(SEARCH_MX);
                if (log.isDebugEnabled())
                {
                    log.debug("notifyDeviceEventListeners() - MX-SEARCH sent");
                }
            }
        }     
    }

    // TODO OCORI-4242 : Called in SystemContext
    private void notifyDeviceEventListeners(final DeviceEvent event)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyDeviceEventListeners() - called");
        }
        synchronized (ucp)
        {
            // Notify listeners just by calling since context is maintained by UPnPControlPoint
            // notification mechanism
            for (int i = 0; i < deviceListeners.size(); i++)
            {
                DeviceEventListener listener = (DeviceEventListener)deviceListeners.get(i);
                listener.notify(event);
            }
        }
    }
    
    // TODO OCORI-4242 ; Called in SystemContext
    private void notifyNetModuleEventListeners(final NetModuleEvent event)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyNetModuleEventListeners() - called");
        }
        synchronized (ucp)
        {
            // Notify listeners just by calling since context is maintained by UPnPControlPoint
            // notification mechanism
            for (int i = 0; i < netModuleListeners.size(); i++)
            {
                NetModuleEventListener listener = (NetModuleEventListener)netModuleListeners.get(i);
                listener.notify(event);
            }
        }
    }
    
    /**
     * Returns net modules which correspond to services in UPnPControlPoint
     * 
     * @param service
     * @return
     */
    protected NetModule createNetModule(UPnPClientService service)
    {
        NetModule result = null;
        UPnPClientDevice uDevice = service.getDevice();
        Device device = new DeviceImpl(uDevice);
        
        if (device.isLocal())
        {
            result = getLocalNetModule(service);
        }
        else
        {
            result = createNetModuleInstance(service);
        }
        
        return result;
    }
    
    /**
     * Get net module associated with local device.
     * 
     * @param service   get NetModule associated with this UPnP service
     * 
     * @return  local instance of NetModule with associated UPnP service
     */
    private NetModule getLocalNetModule(UPnPClientService service)
    {
        if(service == null || service.getServiceType() == null)
        {
            return null;
        }
        
        if(service.getServiceType().indexOf(UPnPConstants.SCHEDULED_RECORDING_URN) >= 0)
        {
            // *TODO* - might need logic here to not create if not a DVR host
            if (m_localSRS == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getLocalNetModule() - creating SRS");
                }
                m_localSRS = (NetModuleImpl)createNetModuleInstance(service);
            }
            return m_localSRS;            
        }
        else if(service.getServiceType().indexOf(UPnPConstants.CONTENT_DIRECTORY_URN) >= 0)
        {
            if (m_localCDS == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getLocalNetModule() - creating CDS");
                }
                m_localCDS = (ContentServerNetModuleImpl)createNetModuleInstance(service);
            }
            return m_localCDS;            
        }
        else if(service.getServiceType().indexOf(UPnPConstants.CONNECTION_MANAGER_URN) >= 0)
        {
            if (m_localCMS == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getLocalNetModule() - creating CMS");
                }
                m_localCMS = (NetModuleImpl)createNetModuleInstance(service);
            }
            return m_localCMS;            
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("createNetModuleInstance() - creating unknown type net module: " +
            service.getServiceType());
        }        
        
        return new NetModuleImpl(service);
    }

    /**
     * Create a NetModule representation of supplied UPnPService from a remote device.
     * 
     * @param service   UPnP service associated with NetModule
     * 
     * @return  NetModule representation of remote UPnP service
     */
    private NetModule createNetModuleInstance(UPnPClientService service)
    {
        if(service == null || service.getServiceType() == null)
        {
            return null;
        }
        
        if(service.getServiceType().indexOf(UPnPConstants.SCHEDULED_RECORDING_URN) >= 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("createNetModuleInstance() - creating SRS for device: " +
                service.getDevice().getFriendlyName() + " / " + service.getDevice().getInetAddress());
            }

            // create SRS NetModule using reflection for separable builds
            NetModule srs = null;
            UPnPClientService[] initargs = {service}; 
            try
            {
                Class netRequestManager = Class.forName("org.cablelabs.impl.ocap.hn.upnp.srs.NetRecordingRequestManagerImpl"); 
                Class[] ctorArgs = new Class[1];
                ctorArgs[0] = UPnPClientService.class;
                Constructor ctor = netRequestManager.getConstructor(ctorArgs);
                srs = (NetModule) ctor.newInstance(initargs);
            }
            catch (ClassNotFoundException e)
            {
                // default for HN Only build
            }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("NetManagerImpl() - Exception while initializing NetRecordingRequestManagerImpl", e);
                }
            }

            return srs; 
        }
        else if(service.getServiceType().indexOf(UPnPConstants.CONTENT_DIRECTORY_URN) >= 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("createNetModuleInstance() - creating CDS for device: " + 
                service.getDevice().getFriendlyName() + " / " + service.getDevice().getInetAddress());
            }
            return new ContentServerNetModuleImpl(service);            
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("createNetModuleInstance() - creating unknown type net module: " +
            service.getServiceType());
        }        
        
        return new NetModuleImpl(service);
    }
    
    public HNStreamProtocolInfo[] getPlatformSupportedPlaybackProtocolInfos()
    {
        return m_platformSupportedPlaybackProtocols;
    }
    
    private void loadPlatformProtocolInfo()
    {
        if (log.isInfoEnabled())
        {
            log.info("NetManagerImpl: loadPlatformProtocolInfo()");
        }

        m_platformSupportedProfiles = HNAPIImpl.nativePlayerGetDLNAProfileIds();
        m_platformSupportedMimeTypes = new String[m_platformSupportedProfiles.length][];
        int totalProtocolInfos = 0;

        for (int i = 0; i < m_platformSupportedProfiles.length; i++)
        {
            m_platformSupportedMimeTypes[i] = HNAPIImpl.nativePlayerGetMimeTypes(m_platformSupportedProfiles[i]);
            totalProtocolInfos += m_platformSupportedMimeTypes[i].length;
        }

        m_platformSupportedPlaybackProtocols = new HNStreamProtocolInfo[totalProtocolInfos];
        int platformProtocolsIndex = 0;

        for (int i = 0; i < m_platformSupportedProfiles.length; i++)
        {
            for (int j = 0; j < m_platformSupportedMimeTypes[i].length; j++)
            {
                String[] playspeeds = HNAPIImpl.nativePlayerGetPlayspeeds(m_platformSupportedProfiles[i], m_platformSupportedMimeTypes[i][j]);

                // Create protocol info to represent what is supported by the platform
                // Only fields that really matter are profileID and mime type
                int protocolType = HNStreamProtocolInfo.PROTOCOL_TYPE_RECORDING;
                boolean isLinkProtected = false;
                String opParam = null;
                String flagsParam = null;

                if (log.isInfoEnabled())
                {
                    log.info( "NetManagerImpl: loadPlatformProtocolInfo: Profile["
                              + platformProtocolsIndex + "]: "
                              + m_platformSupportedProfiles[i] 
                              + " (" + m_platformSupportedMimeTypes[i][j] + ')' );
                }
                
                m_platformSupportedPlaybackProtocols[platformProtocolsIndex] 
                    = new HNStreamProtocolInfo(protocolType, isLinkProtected, m_platformSupportedProfiles[i],
                                               m_platformSupportedMimeTypes[i][j], m_platformSupportedMimeTypes[i][j], opParam, 
                                               HNStreamProtocolInfo.generatePlayspeeds(playspeeds), 
                                               flagsParam, false );
                platformProtocolsIndex++;
            }
        }
        if (log.isInfoEnabled())
        {
            log.info( "NetManagerImpl: loadPlatformProtocolInfo: Done. Found " 
                      + totalProtocolInfos 
                      + " platform-supported profiles");
        }
    } // END loadPlatformProtocolInfo()

    /**
     * Process a notification from platform that IP change has occurred 
     * on interface.
     * 
     * @param eventCode SOCKET_EVT_IP_ADDED or SOCKET_EVT_IP_REMOVED 
     * @param addr      IPV4 address associated with event 
     * 
     */
    private void notifyIPChange(int eventCode, int addr)
    {

        byte[] ipAddr = new byte[4];
        ipAddr[0] = (byte) (addr >> 24);
        ipAddr[1] = (byte) (addr >> 16);
        ipAddr[2] = (byte) (addr >> 8); 
        ipAddr[3] = (byte) addr;
        InetAddress changedAddr = null;
        try
        {
            changedAddr = InetAddress.getByAddress(ipAddr);
        }
        catch (UnknownHostException uh)
        {
            if (log.isWarnEnabled())
            {
                log.warn("notifyIPChange UnknownHostException");
            } 
        }

        if (log.isInfoEnabled())
        {
            log.info( "NetManagerImpl: notifyIPChange IP = "
               + changedAddr.getHostAddress() + " event = " + eventCode); 
        }

        if (changedAddr == null)
        {
            return;
        }

        if (MediaServer.getInstance().isInitialized())
        {
            if (log.isInfoEnabled())
            {
                log.info( "NetManagerImpl: notifyIPChange Process"); 
            }
        }
 
        //TODO - implement logic to dynamic add removed UPnP device/services

    }

    private static native void setInterfaceListener(String linkinterface,EDListener edListener);
}


