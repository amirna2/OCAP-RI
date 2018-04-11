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

// TODO: check need for synchronized

package org.ocap.hn.upnp.client;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.log4j.Logger;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.PropertiesManager;
import org.cablelabs.impl.ocap.hn.NetManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.client.UPnPClientDeviceImpl;
import org.cablelabs.impl.ocap.hn.upnp.client.UPnPClientServiceImpl;
import org.cablelabs.impl.ocap.hn.upnp.client.UPnPClientStateVariableImpl;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPInputInterceptor;
import org.cablelabs.impl.ocap.hn.upnp.common.UPnPOutputInterceptor;
import org.cablelabs.impl.util.HNEventMulticaster;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;

import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.device.DeviceChangeListener;
import org.cybergarage.upnp.device.NotifyListener;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.device.ST;
import org.cybergarage.upnp.device.USN;
import org.cybergarage.upnp.diag.MessageInterceptor;
import org.cybergarage.upnp.event.EventListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;
import org.cybergarage.util.Debug;

import org.ocap.hn.upnp.common.UPnPIncomingMessageHandler;
import org.ocap.hn.upnp.common.UPnPMessage;
import org.ocap.hn.upnp.common.UPnPOutgoingMessageHandler;
import org.ocap.system.MonitorAppPermission;

/**
 * This class represents a device control point that can
 * discover devices and services. It also offers a facility to
 * directly monitor and modify communication between the control
 * point and any devices.
 */
public class UPnPControlPoint
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(UPnPControlPoint.class);

    /**
     * The singleton instance of this class.
     */
    private static final UPnPControlPoint INSTANCE = new UPnPControlPoint();

    /* CallerContextManager */
    private static CallerContextManager ccm =
        (CallerContextManager)ManagerManager.getInstance(CallerContextManager.class);

    /**
     * The system context.
     */
    private static CallerContext systemContext;

    /**
     * The CyberLink <code>ControlPoint</code>, in terms of which this object is implemented.
     */
    private final ControlPoint cp = cyberlinkControlPoint();

    /**
     * List of <code>CallerContext</code>s that have added listeners.
     */
    private CallerContext ccList = null;

    /**
     * Used for locking addDeviceListener, removeDeviceListener calls and
     * protecting access to <code>ccList</code>.
     * Also used as a ListenerData key (instead of UPnPControlPoint.this),
     * since listener callback data and handler callback data is handled
     * separately - we cannot use the same key for caller context look-ups.
     * ccList cannot be used as a look-up key because it can be <code>null</code>.
     */
    private final Object listenerDataObject = new Object();

    /**
     * Holds the <code>CallerContext</code> for the application that supplied
     * a <code>UPnPIncomingMessageHandler</code>.
     */
    private static AppIMH appInHandler = null;

    /**
     * Used for locking setIncomingMessageHandler calls and protecting
     * access to <code>appInHandler</code> object.
     */
    private static final Object inHandlerDataObject = new Object();

    /**
     * Holds the <code>CallerContext</code> for the application that supplied
     * a <code>UPnPOutgoingMessageHandler</code>.
     */
    private static AppOMH appOutHandler = null;

    /**
     * Used for locking setOutgoingMessageHandler calls and protecting
     * access to <code>appOutHandler</code> object.
     */
    private static final Object outHandlerDataObject = new Object();

    /**
     * This object's map of DeviceID to UPnPDevice.
     * We use this map to preserve UPnPDevice identity
     * in the face of Device non-identity, and to de-bounce
     * alive and byebye messages.
     */
    private final Map deviceMap = new HashMap();

    /**
     * List of event information to temporary store events which are
     * received prior to the subscription response which contains the
     * subscription ID for the service.
     */
    private final Vector undeliveredEvents = new Vector();

    /**
     * Services stored by subscription id to allow for association
     * with events rather than traversing devices & services each
     * time an event is received
     */
    private final Map sidMap = new HashMap();

    /**
     * This object's CyberLink "device change", "notify", and "search response" listener.
     */
    private final Listener listener = new Listener();
    

    // METHODS REQUIRED BY THE SPEC

    /**
     * Construct the instance.
     */
    protected UPnPControlPoint()
    {
        //use debug enabled as the indicator that Debug.on should be called
        if (log.isDebugEnabled())
        {
            Debug.on();
        }
        if (log.isInfoEnabled())
        {
            log.info("Creating UPnPControlPoint");
        }

        // *TODO* - code moved from CControlPoint constructor
        List inetAddrs = new ArrayList();
        try
        {
            Enumeration e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements())
            {
                NetworkInterface ni = (NetworkInterface)e.nextElement();
                Enumeration ie = ni.getInetAddresses();
                while(ie.hasMoreElements())
                {
                    InetAddress i = (InetAddress)ie.nextElement();
                    if (log.isWarnEnabled())
                    {
                        log.warn("ControlPoint to listen on : " + i.getHostAddress());
                    }
                    inetAddrs.add(i);
                }
            }
        }
        catch(SocketException se)
        {
            if (log.isWarnEnabled())
            {
                log.warn(se);
            }
        }
        
        if(inetAddrs.size() == 0)
        {
            if (log.isWarnEnabled())
            {
                log.warn("No InetAddresses were assigned to this ControlPoint.");
            }
        }
        
        setInetAddresses((InetAddress[])inetAddrs.toArray(new InetAddress[inetAddrs.size()]));
        
        // *TODO - end of CControlPoint code

    }

    /**
     * Obtain the local UPnP device control point.
     *
     * @return The singleton UPnPControlPoint.
     */
    public static UPnPControlPoint getInstance()
    {
       return INSTANCE;
    }
    
    // *TODO* temp filler methods until UPnPClientService support is present
    /* Don't believe this method is required in the re-impl since start() is
     * called when constructed 
    public void discover(final IEventListener eventListener)
    {
        // *TODO* - add replacement
    }
    */
    /**
     * *TODO* - method still needed? UPnPCP start method should at least
     * utilized defined parameter SEARCH_MX?
     * Starts the Control Point Discovery
     */
    /*
    public synchronized void startDiscover()
    {
        if (firstTime)
        {
            firstTime = false;
            start(ST.ALL_DEVICE, SEARCH_MX);
        }
        else
        {
            search(ST.ALL_DEVICE, SEARCH_MX);
        }
    }
    */
    
    /**
     * Sets the InetAddresses that the
     * <code>UPnPControlPoint</code> is associated with. The control
     * point will only send searches and listen for device
     * advertisements on the most appropriate
     * interface for each of the addresses specified.
     *
     * <p>The passed array replaces any prior addresses that the
     * control point was associated with. The control point may need
     * to perform searches and update its list of devices in
     * response to this method being invoked.
     *
     * <p>Note that the control point defaults to all home network
     * interfaces with all their associated IP addresses. A client
     * application would not normally need to invoke this method.
     *
     * @param addresses Array of <code>InetAddress</code>
     *                   objects representing the IP addresses
     *                   that the control point is associated with.
     *                   May be zero length.
     *
     * @return Array of prior addresses that were associated with
     *         the <code>UPnPControlPoint</code>. If there were no
     *         prior addresses, returns a zero-length array.
     *
     * @throws NullPointerException if {@code addresses} or any of its
     * elements is {@code null}.
     *
     * @throws SecurityException if the calling application has not been
     *      granted MonitorAppPermission("handler.homenetwork").
     */
    public InetAddress[] setInetAddresses(InetAddress[] addresses)
                            throws SecurityException
    {
        if (log.isDebugEnabled())
        {
            log.debug("setInetAddresses() - called");
        }
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        InetAddress[] priorAddresses = cp.getInetAddresses();
        
        if (addresses == null) 
        {
            if (log.isErrorEnabled())
            {
                log.error("setInetAddresses(InetAddress[] addresses) - addresses arguement was null.");
            }
            throw new NullPointerException();
        }
        for (int x = 0; x < addresses.length; x++)
        {
            if (addresses[x] == null) 
            {
                if (log.isErrorEnabled())
                {
                    log.error("setInetAddresses(InetAddress[] addresses) - Element in addresses arguement was null.");
                }
                throw new NullPointerException();
            }
        }

        // *TODO* - add a check against prior addresses to verify things are really changing

        // Stop the control point
        stop();

        cp.setInetAddresses(addresses == null ? new InetAddress[0] : addresses);

        // Rescan with new set of network interfaces
        start();

        return priorAddresses == null ? new InetAddress[0] : priorAddresses;
    }
    
    /**
     * Gets the <code>InetAddress</code>es that this
     * <code>UPnPControlPoint</code> is associated with.
     *
     * @return Array of <code>InetAddress</code> objects
     *                   representing the network interfaces
     *                   that this control point is associated with.
     *                   If the control point has no associated
     *                   network interfaces, returns a zero length array.
     *
     */
    public InetAddress[] getInetAddresses()
    {
        InetAddress[] addresses = cp.getInetAddresses();
        return addresses == null ? new InetAddress[0] : addresses;
    }

    /**
     * Gets a client representation of all UPnP root devices visible
     * to this host. This does not cause a search to take place, but
     * simply returns the currently known devices.
     *
     * @return The UPnP devices visible to this host.  Each element
     *         in the array of <code>UPnPClientDevice</code>s returned
     *         represents one root device found by the local host
     *         via UPnP discovery. If no root devices are found,
     *         returns a zero-length array.
     */
    public UPnPClientDevice[] getDevices()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getDevices() - called");
        }

        List deviceList = getDeviceList();

        return (UPnPClientDevice[]) deviceList.toArray(new UPnPClientDevice[deviceList.size()]);
    }

    /**
     * Gets a client representation of all UPnP devices of the
     * specified type visible to this host. This does not cause a
     * search to take place, but simply returns the currently known
     * devices.
     *
     * @param type The type of devices to return. Of the form
     *             urn:schemas-upnp-org:device:deviceType:v where
     *             deviceType is replaced with a type specific to
     *             the device being requested, and v is a version
     *             specifier as defined in UPnP Device Architecture.
     *
     * @return The UPnP devices visible to this host matching the
     *         type specified, of the specified version or lower
     *         version number. Each element in the array of
     *         <code>UPnPClientDevice</code>s returned represents one
     *         device found by the local host via UPnP discovery. If
     *         no devices matching the type are found, returns a
     *         zero-length array.
     */
    public UPnPClientDevice[] getDevicesByType(String type)
    {
        List deviceList = filterDeviceList(getDeviceList(), new ByDeviceTypeDeviceFilter(type));

        return (UPnPClientDevice[]) deviceList.toArray(new UPnPClientDevice[deviceList.size()]);
    }

    /**
     * Gets a client representation of the UPnP devices of the
     * specified UDN visible to this host. This does not cause a
     * search to take place, but simply returns the currently known
     * devices.
     *
     * <p>Note that normally a UDN is unique and would return a
     * single device. In multi-homed server and control point
     * environments, a single server may be visible over multiple
     * interfaces, resulting in multiple {@code UPnPClientDevice}
     * representations.
     *
     * @param UDN The UDN of the devices to return.
     *
     * @return The UPnP devices visible to this host matching the
     *         UDN specified. Each element in the array of
     *         <code>UPnPClientDevice</code>s returned represents one
     *         device found by the local host via UPnP discovery. If
     *         no devices matching the UDN are found, returns a
     *         zero-length array.
     */
    public UPnPClientDevice[] getDevicesByUDN(String UDN)
    {
        List deviceList = filterDeviceList(getDeviceList(), new ByUDNDeviceFilter(UDN));

        return (UPnPClientDevice[]) deviceList.toArray(new UPnPClientDevice[deviceList.size()]);
    }

    /**
     * Gets a client representation of all UPnP devices containing a
     * service of the specified type, visible to this host. This
     * does not cause a search to take place, but simply returns the
     * currently known devices.
     *
     * @param type The type of service to use in determining which
     *             devices to return. Of the form
     *             urn:schemas-upnp-org:service:serviceType:v where
     *             serviceType is replaced with a type specific
     *             to the service being requested, and v is a
     *             version specifier as defined in UPnP Device
     *             Architecture.
     *
     * @return The UPnP devices visible to this host containing a
     *         service matching the type specified, of the specified
     *         version or lower version number. Each element in the
     *         array of <code>UPnPClientDevice</code>s returned represents
     *         one device found by the local host via UPnP
     *         discovery. Returns only devices directly containing
     *         a service of the matching type, not devices where
     *         only their embedded devices contain a service of the
     *         matching type. If no devices matching the criteria
     *         are found, returns a zero-length array.
     */
    public UPnPClientDevice[] getDevicesByServiceType(String type)
    {
        List deviceList = filterDeviceList(getDeviceList(), new ByServiceTypeDeviceFilter(type));

        return (UPnPClientDevice[]) deviceList.toArray(new UPnPClientDevice[deviceList.size()]);
    }

    /**
     * Initiate a UPnP M-SEARCH for UPnP root devices. The UPnP
     * stack constantly monitors for device arrival and departure.
     * This method is used to assist with detection of devices which
     * may not renew advertisements correctly and only respond to
     * search requests.
     *
     * @param mx The maximum time in seconds for client devices to
     *           respond to this search.
     *
     */
    public void search(final int mx)
    {
        getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                cp.search(ST.ALL_DEVICE, mx);
            }
        });
    }

    /**
     * Adds a listener for device changes.
     * Each <code>UPnPClientDeviceListener</code>is notified when a UPnP
     * device is added to or removed from a home network.
     *
     * <p>Adding a listener which is the same instance as a
     * previously added (and not removed) listener has no effect.
     *
     * @param listener The listener to add.
     */
    public void addDeviceListener(UPnPClientDeviceListener listener)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("Passed UPnPDeviceListener is null");
        }

        CallerContext ctx = ccm.getCurrentContext();
        synchronized (listenerDataObject)
        {
            ListenerData data = (ListenerData)ctx.getCallbackData(listenerDataObject);
            if (data == null)
            {
                data = new ListenerData();
                ctx.addCallbackData(data, listenerDataObject);
                ccList = CallerContext.Multicaster.add(ccList, ctx);
            }
            data.deviceListeners = HNEventMulticaster.add(data.deviceListeners, listener);
        }
    }

    /**
     * Removes a device listener.
     *
     * @param listener The listener to remove.
     */
    public void removeDeviceListener(UPnPClientDeviceListener listener)
    {
        if (listener == null)
        {
            throw new IllegalArgumentException("Passed UPnPDeviceListener is null");
        }

        CallerContext ctx = ccm.getCurrentContext();
        synchronized (listenerDataObject)
        {
            ListenerData data = (ListenerData)ctx.getCallbackData(listenerDataObject);
            if (data != null)
            {
                if (data.deviceListeners != null)
                {
                    data.deviceListeners = HNEventMulticaster.remove(data.deviceListeners, listener);
                }
                if (data.deviceListeners == null)
                {
                    ctx.removeCallbackData(listenerDataObject);
                    ccList = CallerContext.Multicaster.remove(ccList, ctx);
                }
            }
        }
    }


    /**
     * Sets a message handler for incoming messages (advertisements,
     * evented state variables, action responses, device and service
     * descriptions). Calls to set the message handler replace any
     * prior incoming message handler.
     *
     * <p>A message handler may be removed by passing null as the
     * inHandler. In the absence of a registered message handler the
     * stack will parse the incoming messages.
     *
     * <p>If the application-provided handler throws any exceptions
     * during execution, the stack will attempt to process the
     * message with the default (stack-provided) handler.
     *
     * @param inHandler The incoming message handler to set.
     *
     * @throws SecurityException if the calling application has not
     *      been granted
     *      MonitorAppPermission("handler.homenetwork").
     */
    public void setIncomingMessageHandler(UPnPIncomingMessageHandler inHandler)
                    throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        // NOTE: this can be done statically only because UPnPControlPoint is a singleton!

        synchronized (UPnPControlPoint.inHandlerDataObject)
        {
            // No need to do anything if both old and new handlers are the same
            if ((UPnPControlPoint.appInHandler == null && inHandler == null) ||
                (UPnPControlPoint.appInHandler != null && UPnPControlPoint.appInHandler.inHandler == inHandler))
            {
                return;
            }

            // At this point, we know that the old handler is getting replaced, so remove it
            if (UPnPControlPoint.appInHandler != null)
            {
                UPnPControlPoint.appInHandler.ctx.removeCallbackData(UPnPControlPoint.inHandlerDataObject);
                UPnPControlPoint.appInHandler = null;
                MessageInterceptor.setClientInputInterceptor(null);
            }

            // Add the new handler, if there is one
            if (inHandler != null)
            {
                UPnPControlPoint.appInHandler = new AppIMH(ccm.getCurrentContext(), inHandler);
                UPnPControlPoint.appInHandler.ctx.addCallbackData(UPnPControlPoint.appInHandler, UPnPControlPoint.inHandlerDataObject);
                MessageInterceptor.setClientInputInterceptor(new UPnPInputInterceptor(UPnPControlPoint.appInHandler));
            }
        }
    }

    /**
     * Sets a message handler for outgoing messages (action
     * invocations, subscription requests, device and service
     * retrievals). Calls to set the message handler replace any
     * prior outgoing message handler.
     *
     * <p>A message handler may be removed by passing null as the
     * outHandler. In the absence of a registered message handler the
     * stack will process the outgoing messages.
     *
     * <p>If the application-provided handler throws any exceptions
     * during execution, the stack will attempt to process the
     * message with the default (stack-provided) handler.
     *
     * @param outHandler The outgoing message handler to set.
     *
     * @throws SecurityException if the calling application has not
     *      been granted
     *      MonitorAppPermission("handler.homenetwork").
     */
    public void setOutgoingMessageHandler(UPnPOutgoingMessageHandler outHandler)
                    throws SecurityException
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.homenetwork"));

        // NOTE: this can be done statically only because UPnPControlPoint is a singleton!

        synchronized (UPnPControlPoint.outHandlerDataObject)
        {
            // No need to do anything if both old and new handlers are the same
            if ((UPnPControlPoint.appOutHandler == null && outHandler == null) ||
                (UPnPControlPoint.appOutHandler != null && UPnPControlPoint.appOutHandler.outHandler == outHandler))
            {
                return;
            }

            // At this point, we know that the old handler is getting replaced, so remove it
            if (UPnPControlPoint.appOutHandler != null)
            {
                UPnPControlPoint.appOutHandler.ctx.removeCallbackData(UPnPControlPoint.outHandlerDataObject);
                UPnPControlPoint.appOutHandler = null;
                MessageInterceptor.setClientOutputInterceptor(null);
            }

            // Add the new handler, if there is one
            if (outHandler != null)
            {
                UPnPControlPoint.appOutHandler = new AppOMH(ccm.getCurrentContext(), outHandler);
                UPnPControlPoint.appOutHandler.ctx.addCallbackData(UPnPControlPoint.appOutHandler, UPnPControlPoint.outHandlerDataObject);
                MessageInterceptor.setClientOutputInterceptor(new UPnPOutputInterceptor(UPnPControlPoint.appOutHandler));
            }
        }
    }

    // PRIVATE METHODS

    /**
     * TODO
     */
    private void addToDeviceList(List result, Device device)
    {

        /** 
         * TODO need to resolve multi-home issue with deviceMap
         * line below is a temp fix
         */
        // clear out existing deviceMap entry for this device
        //deviceMap.remove(createDeviceIDKey(device));
        
        UPnPClientDevice upnpDevice = getUPnPDeviceFromMap(device);

        if (upnpDevice == null)
        {
            upnpDevice = new UPnPClientDeviceImpl(device, cp);
        }
        // Add the device to map if not already in map
        if (deviceMap.get(createDeviceIDKey(device)) == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("addToDeviceList() - device not found in map, adding device: " +
                device.getFriendlyName());
            }
            map(device, upnpDevice);
            
            CallerContext ctx = ccList;
            if (ctx != null)
            {
                final UPnPClientDevice addedDevice = upnpDevice;
                ctx.runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        CallerContext ctx = ccm.getCurrentContext();
                        ListenerData data = (ListenerData)ctx.getCallbackData(listenerDataObject);
                        if (data != null && data.deviceListeners != null)
                        {
                            data.deviceListeners.notifyDeviceAdded(addedDevice);
                        }
                    }
                });
            }
        }
        /* TODO
        else
        {
            upnpDevice.updateDevice(device);
        }
        */

        result.add(upnpDevice);

        List childDevices = device.getDeviceList();

        for (Iterator i = childDevices.iterator(); i.hasNext(); )
        {
            Device childDevice = (Device) i.next();

            addToDeviceList(result, childDevice);
        }
    }

    /**
     * Get the CyberLink ControlPoint.
     *
     * <p>
     * NOTE: We temporarily return the CControlPoint used by the RI,
     * so we only have one ControlPoint active.
     * When the RI is re-implemented in terms of UPnPControlPoint,
     * we will simply instantiate a ControlPoint here, as in the
     * commented-out line below.
     */
    private static ControlPoint cyberlinkControlPoint()
    {
        return new ControlPoint();
/* *TODO* - remove this code
        try
        {
            final Method gcp = Client.class.getDeclaredMethod("getControlPoint", new Class[] {});

            AccessController.doPrivileged(new PrivilegedAction()
            {
                public Object run()
                {
                    gcp.setAccessible(true);
                    return null;
                }
            });

            return (ControlPoint) gcp.invoke(Client.getInstance(), null);
        }
        catch (Exception e)
        {
            if (Logging.LOGGING)
            {
                log.fatal("Can't get CyberLink ControlPoint!", e);
            }
            return null;
        }
        */
    }

    /**
     * Get Cybergarage device associated with the UDN which was
     * specified in ssdpPacket by asking Cybergarage control point
     * to return this device.  
     * 
     * @return  Cybergarage device which matches UDN, null if none is found
     */
    private Device getCGDeviceUsingUDN(SSDPPacket ssdpPacket)
    {
        String usn = ssdpPacket.getUSN();
        String udn = USN.getUDN(usn);

        Device d = cp.getDevice(udn);

        if (d != null)
        {
            d.setLocation(ssdpPacket.getLocation());
        }

        return d;
    }

    /**
     * TODO
     */
    private static List filterDeviceList(List upnpDevices, DeviceFilter deviceFilter)
    {
        List result = new ArrayList();

        for (Iterator i = upnpDevices.iterator(); i.hasNext(); )
        {
            UPnPClientDevice upnpDevice = (UPnPClientDevice) i.next();

            if (deviceFilter.accept(upnpDevice))
            {
                result.add(upnpDevice);
            }
        }

        return result;
    }

    /**
     * Return the List<UPnPDevice> corresponding to CyberLink's
     * record of currently known controlled devices on the network.
     *
     * @return The List<UPnPDevice>.
     */
    private List getDeviceList()
    {
        synchronized (deviceMap)
        {
            List result = new ArrayList();

            // add all devices from the ControlPoint to deviceMap and result

            List rootDevices = cp.getDeviceList();

            for (Iterator i = rootDevices.iterator(); i.hasNext(); )
            {
                Device device = (Device) i.next();

                addToDeviceList(result, device);
            }

            // remove anything from deviceMap that is not in result

            Set deviceMapIDs = new HashSet(deviceMap.keySet());
            Set resultIDs = new HashSet();

            for (Iterator i = result.iterator(); i.hasNext(); )
            {
                UPnPClientDevice upnpDevice = (UPnPClientDevice) i.next();
                resultIDs.add(createDeviceIDKey(((UPnPClientDeviceImpl) upnpDevice).getDevice()));
            }

            deviceMapIDs.removeAll(resultIDs);

            for (Iterator i = deviceMapIDs.iterator(); i.hasNext(); )
            {
                DeviceID id = (DeviceID) i.next();
                deviceMap.remove(id);
            }

            setParentChildRelationships();

            return result;
        }
    }

    /**
     * TODO
     */
    private static synchronized CallerContext getSystemContext()
    {
        if (systemContext == null)
        {
            CallerContextManager ccm = (CallerContextManager)ManagerManager.getInstance(CallerContextManager.class);
            systemContext = ccm.getSystemContext();
        }

        return systemContext;
    }

    /**
     * TODO
     */
    private static DeviceID createDeviceIDKey(Device device)
    {
        String location = device.getLocation();
        String udn = device.getUDN();

        return new DeviceID(location, udn);
    }

    /**
     * TODO
     */
    private void map(Device device, UPnPClientDevice upnpDevice)
    {
        synchronized (deviceMap)
        {
            deviceMap.put(createDeviceIDKey(device), upnpDevice);
        }

        // Add control point listener as service listener to support
        // notifying when subscription completes and SID is available
        UPnPClientServiceImpl services[] = (UPnPClientServiceImpl[])upnpDevice.getServices();
        for (int i = 0; i < services.length; i++)
        {
            if (services[i].hasEventedStateVariable())
            {
                services[i].addStateVariableListener(listener);
            }
        }
    }

    /**
     * TODO
     */
    private void setParentChildRelationships()
    {
        synchronized (deviceMap)
        {
            // set up all the parent/child relationships

            for (Iterator i = deviceMap.values().iterator(); i.hasNext(); )
            {
                UPnPClientDevice upnpDevice = (UPnPClientDevice) i.next();

                ((UPnPClientDeviceImpl) upnpDevice).setParentDevice(null);
                ((UPnPClientDeviceImpl) upnpDevice).clearChildDevices();
            }

            for (Iterator i = deviceMap.values().iterator(); i.hasNext(); )
            {
                UPnPClientDevice upnpDevice = (UPnPClientDevice) i.next();
                Device device = ((UPnPClientDeviceImpl) upnpDevice).getDevice();

                Device parentDevice = device.getParentDevice();

                UPnPClientDevice parentUPnPDevice;

                if (parentDevice != null && parentDevice.getDeviceNode() != null)
                {
                    parentUPnPDevice = getUPnPDeviceFromMap(parentDevice);
                    // assert parentUPnPDevice != null; /* Hmm... this was not true at one point */
                }
                else
                {
                    parentUPnPDevice = null;
                }

                ((UPnPClientDeviceImpl) upnpDevice).setParentDevice(parentUPnPDevice);
                if (parentUPnPDevice != null)
                {
                    ((UPnPClientDeviceImpl) parentUPnPDevice).addChildDevice(upnpDevice);
                }
            }
        }
    }

    /**
     * TODO
     */
    private void start()
    {
        getSystemContext().runInContextAsync(new Runnable()
        {
            public void run()
            {
                cp.start(ST.ALL_DEVICE, NetManagerImpl.SEARCH_MX);
            }
        });
    }

    /**
     * TODO
     */
    private void stop()
    {
        // Destroy any devices created which maybe in process of subscribing
        List devices = getDeviceList();
        for (int i = 0; i < devices.size(); i++)
        {
            UPnPClientDeviceImpl device = (UPnPClientDeviceImpl)devices.get(i);
            device.destroy();
        }

        // Stopping control point must be done in sync thread to avoid race condition
        // Need to guarantee stop is complete            
        try
        {
            getSystemContext().runInContextSync(new Runnable()
            {
                public void run()
                {
                    cp.stop();
                }
            });
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Exception stopping ControlPoint ",e);
            }
        }

        synchronized (deviceMap)
        {
             deviceMap.clear();
        }
        synchronized (sidMap)
        {
            sidMap.clear();
        }
    }

    /**
     * TODO
     */
    private void unmap(Device device)
    {
        synchronized (deviceMap)
        {
            deviceMap.remove(createDeviceIDKey(device));
        }
    }

    /**
     * TODO
     */
    private UPnPClientDevice getUPnPDeviceFromMap(Device device)
    {
        synchronized (deviceMap)
        {
            return (UPnPClientDevice) deviceMap.get(createDeviceIDKey(device));
        }
    }

    // PRIVATE TYPES

    /**
     * An immutable class that serves to uniquely identify a UPnPDevice.
     * It defines equals and hashCode methods so it can serve as a Map key.
     */
    private static final class DeviceID
    {
        private final String location;
        private final String UDN;

        public DeviceID(String location, String UDN)
        {
            this.location = location;
            this.UDN = UDN;
        }

        public boolean equals(Object obj)
        {
            if (! (obj instanceof DeviceID))
            {
                return false;
            }

            DeviceID that = (DeviceID) obj;

            return this.location.equals(that.location) && this.UDN.equals(that.UDN);
        }

        public int hashCode()
        {
            return location.hashCode() + UDN.hashCode();
        }
    }

    /**
     * TODO
     */
    private interface DeviceFilter
    {
        public boolean accept(UPnPClientDevice upnpDevice);
    }

    /**
     * TODO
     */
    private abstract class ByTypeDeviceFilter implements DeviceFilter
    {
        protected final PartitionedType partitionedType;

        protected ByTypeDeviceFilter(String type)
        {
            partitionedType = partition(type);
        }

        protected PartitionedType partition(String type)
        {
            if (type == null)
            {
                return null;
            }

            int i = type.lastIndexOf(':');

            if (i < 0)
            {
                return null;
            }

            String typePrefix = type.substring(0, i);
            String versionStr = type.substring(i + 1);

            int version;

            try
            {
                version = Integer.parseInt(versionStr);
            }
            catch (NumberFormatException e)
            {
                return null;
            }

            return new PartitionedType(typePrefix, version);
        }

        protected class PartitionedType
        {
            private final String typePrefix;
            private final int version;

            public PartitionedType(String typePrefix, int version)
            {
                this.typePrefix = typePrefix;
                this.version = version;
            }

            public String typePrefix()
            {
                return typePrefix;
            }

            public int version()
            {
                return version;
            }
        }
    }

    /**
     * TODO
     */
    private class ByDeviceTypeDeviceFilter extends ByTypeDeviceFilter
    {
        public ByDeviceTypeDeviceFilter(String type)
        {
            super(type);
        }

        public boolean accept(UPnPClientDevice upnpDevice)
        {
            if (partitionedType == null)
            {
                return false;
            }

            PartitionedType pt = partition(upnpDevice.getDeviceType());

            if (pt == null)
            {
                return false;
            }

            return pt.typePrefix().equals(partitionedType.typePrefix()) && pt.version() <= partitionedType.version();
        }
    }

    /**
     * TODO
     */
    private class ByServiceTypeDeviceFilter extends ByTypeDeviceFilter
    {
        public ByServiceTypeDeviceFilter(String type)
        {
            super(type);
        }

        public boolean accept(UPnPClientDevice upnpDevice)
        {
            if (partitionedType == null)
            {
                return false;
            }

            UPnPClientService[] upnpServices = upnpDevice.getServices();

            for (int i = 0, n = upnpServices.length; i < n; ++ i)
            {
                UPnPClientService upnpService = upnpServices[i];

                PartitionedType pt = partition(upnpService.getServiceType());

                if (pt != null && pt.typePrefix().equals(partitionedType.typePrefix()) && pt.version() <= partitionedType.version())
                {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * TODO
     */
    private static class ByUDNDeviceFilter implements DeviceFilter
    {
        private final String UDN;

        public ByUDNDeviceFilter(String UDN)
        {
            this.UDN = UDN;
        }

        public boolean accept(UPnPClientDevice upnpDevice)
        {
            return upnpDevice.getUDN().equals(UDN);
        }
    }

    /**
     * TODO
     */
    private class Listener implements DeviceChangeListener, NotifyListener, SearchResponseListener, 
                                      EventListener, UPnPStateVariableListener
    {
        /**
         * Construct an instance of this class.
         */
        public Listener()
        {
            cp.addNotifyListener(this);
            cp.addSearchResponseListener(this);
            cp.addEventListener(this);
            cp.addDeviceChangeListener(this);
        }

        /**
         * TODO
         */
        public void deviceAdded(Device device)
        {
        }

        /**
         * TODO
         */
        public void deviceExpired(Device device)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Listener.deviceExpired() - called for device: " + device.getFriendlyName());
            }

            potentiallyRemove(device);
        }

        /**
         * TODO
         */
        public void deviceNotifyReceived(SSDPPacket ssdpPacket, boolean processed)
        {

            if (processed)
            {
                if (ssdpPacket.isAlive())
                {
                    Device device = getCGDeviceUsingUDN(ssdpPacket);
                    if (device != null)
                    {
                        potentiallyAdd(getCGDeviceUsingUDN(ssdpPacket));
                    }
                }
            }
            else
            {
                if (ssdpPacket.isByeBye())
                {
                    Device device = getCGDeviceUsingUDN(ssdpPacket);
                    if (device != null)
                    {
                        potentiallyRemove(device);
                    }
                }
            }
        }

        /**
         * TODO
         */
        public void deviceRemoved(Device device)
        {
        }

        /**
         * TODO
         */
        public void deviceSearchResponseReceived(SSDPPacket ssdpPacket)
        {
            Device device = getCGDeviceUsingUDN(ssdpPacket);
            if (device != null)
            {
                potentiallyAdd(device);
            }
        }

        /**
         * TODO
         */
        private void potentiallyAdd(Device device)
        {
            if (device != null)
            {
                UPnPClientDevice upnpDevice = getUPnPDeviceFromMap(device);

                if (upnpDevice == null)
                {
                    upnpDevice = new UPnPClientDeviceImpl(device, cp);
                    map(device, upnpDevice);

                    CallerContext ctx = ccList;
                    if (ctx != null)
                    {
                        final UPnPClientDevice addedDevice = upnpDevice;
                        ctx.runInContextAsync(new Runnable()
                        {
                            public void run()
                            {
                                CallerContext ctx = ccm.getCurrentContext();
                                ListenerData data = (ListenerData)ctx.getCallbackData(listenerDataObject);
                                if (data != null && data.deviceListeners != null)
                                {
                                    data.deviceListeners.notifyDeviceAdded(addedDevice);
                                }
                            }
                        });
                    }

                    setParentChildRelationships();
                }
                /* TODO
                else
                {
                    upnpDevice.updateDevice(device);
                }
                */
            }
        }

        /**
         * TODO
         */
        private void potentiallyRemove(Device device)
        {
            if (device != null)
            {
                /*
                    TODO: It seems like we should not be removing child
                    devices here. It causes us to notify our listeners of
                    child device removal before we ourselves have been
                    notified of child device removal by CyberLink.
                    Unfortunately, if we wait till then, CyberLink has
                    thrown away its record of the child devices, and we
                    can no longer map the SSDPPacket back to a Device.
                */

                // begin removing child devices
                List childDevices = device.getDeviceList();

                for (Iterator i = childDevices.iterator(); i.hasNext(); )
                {
                    Device childDevice = (Device) i.next();

                    potentiallyRemove(childDevice);
                }
                // end removing child devices

                final UPnPClientDevice upnpDevice = getUPnPDeviceFromMap(device);

                if (upnpDevice != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Listener.potentiallyRemove: removing " + upnpDevice);
                    }

                    unmap(device);

                    CallerContext ctx = ccList;
                    if (ctx != null)
                    {
                        ctx.runInContextAsync(new Runnable()
                        {
                            public void run()
                            {
                                CallerContext ctx = ccm.getCurrentContext();
                                ListenerData data = (ListenerData)ctx.getCallbackData(listenerDataObject);
                                if (data != null && data.deviceListeners != null)
                                {
                                    data.deviceListeners.notifyDeviceRemoved(upnpDevice);
                                }
                            }
                        });
                    }

                    setParentChildRelationships();
                }
            }
        }

        /**
         * Receive events from cybergarage control point through this method.
         */
        public void eventNotifyReceived(String sid, long seq, String varName, String value)
        {
            // Find the service which matches this sid
            UPnPClientServiceImpl service = null;
            synchronized (sidMap)
            {
                service = (UPnPClientServiceImpl)sidMap.get(sid);
            }

            // Find matching state variable and update value
            if (service != null)
            {
                // Get state variable which matches and update value
                UPnPClientStateVariableImpl stateVar = (UPnPClientStateVariableImpl)service.getStateVariable(varName);
                if (stateVar != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Listener.eventNotifyReceived() - found matching state var = " +
                        varName + ", setting value: " + value);
                    }
                    stateVar.setEventedValue(value);

                    // Notify service to notify listeners that state variable has changed
                    service.notifyListenersValueChanged(stateVar);
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Listener.eventNotifyReceived() - did not find a service with SID match = " +
                    sid);
                }
                // *TODO* - Really shouldn't have to do this based on UDA Requirements in 4.1
                // "The device must insure that the control point has received the response to the
                // subscription request before sending the initial event message, to insure that the
                // control point has received the SID (subscription ID) and can thereby correlate the
                // event message to the subscription."

                // Add this event to list, when matching SID is received, forward on event to listeners
                undeliveredEvents.add(new EventInfo(sid, seq, varName, value));
            }
        }

        //
        // UPnpStateVariableChangeListener methods
        //
        /**
         * This method is called when a subscription to a service completes.
         * This allows this control point to dispatch any events received
         * which have been associated with the subscription prior to receiving
         * the subscription response.
         *
         * @param   variable    associated variable used to retrieve service
         */
        public synchronized void notifySubscribed(UPnPClientService clientService)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Listener.notifySubscribed() - called");
            }
            
            UPnPClientServiceImpl service = (UPnPClientServiceImpl)clientService;

            // Add this sid & service to map
            synchronized (sidMap)
            {
                sidMap.put(service.getSID(), service);
            }

            Vector toBeRemoved = new Vector();
            EventInfo events[] = new EventInfo[undeliveredEvents.size()];
            events = (EventInfo[])undeliveredEvents.toArray(events);
            for (int i = 0; i < events.length; i++)
            {
                EventInfo info = events[i];
                if (service.matchesSID(info.m_sid))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("Listener.notifySubscribed() - delivering event now that sid is known");
                    }
                    // Deliver event since now know which service it is associated with
                    eventNotifyReceived(info.m_sid, info.m_seq, info.m_name, info.m_value);
                    toBeRemoved.add(info);
                }
            }

            // Remove all events which have been delivered
            for (int i = 0; i < toBeRemoved.size(); i++)
            {
                undeliveredEvents.remove(toBeRemoved.get(i));
            }
        }

        public void notifyValueChanged(UPnPClientStateVariable variable)
        {
            // do nothing
        }
        public void notifyUnsubscribed(UPnPClientService service)
        {
            // do nothing
        }
    }

    /**
     * Inner class used to store information about a UPnP event whose
     * SID does not yet match any of the currently subscribed services.
     */
    private class EventInfo
    {
        protected String m_sid;
        protected long m_seq;
        protected String m_name;
        protected String m_value;

        protected EventInfo(String sid, long seq, String name, String value)
        {
            m_sid = sid;
            m_seq = seq;
            m_name = name;
            m_value = value;
        }
    }

    /**
     * Per-context global data. Remembers per-context
     * <code>UPnPDeviceListener</code>s.
     */
    private class ListenerData implements CallbackData
    {
        public UPnPClientDeviceListener deviceListeners;

        public void destroy(CallerContext cc)
        {
            synchronized (listenerDataObject)
            {
                // Simply forget the given cc
                // No harm done if never added
                cc.removeCallbackData(listenerDataObject);
                ccList = CallerContext.Multicaster.remove(ccList, cc);
            }
        }

        public void active(CallerContext cc) { }
        public void pause(CallerContext cc) {  }
    }

    /**
     * Wrapper class for <code>UPnPIncomingMessageHandler</code>. Holds the
     * application-supplied handler along with the associated application context.
     * Invokes the <code>handleIncomingMessage</code> method in the application
     * context.
     */
    private class AppIMH implements CallbackData, UPnPIncomingMessageHandler
    {
        private final CallerContext ctx;
        private final UPnPIncomingMessageHandler inHandler;

        public AppIMH(CallerContext ctx, UPnPIncomingMessageHandler inHandler)
        {
            this.ctx = ctx;
            this.inHandler = inHandler;
        }

        public UPnPMessage handleIncomingMessage(final InetSocketAddress address,
                                                 final byte[] incomingMessage,
                                                 final UPnPIncomingMessageHandler defaultHandler)
        {
            try
            {
                final UPnPMessage[] message = { null };
                ctx.runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        message[0] = inHandler.handleIncomingMessage(address, incomingMessage, defaultHandler);
                    }
                });
                return message[0];
            }
            catch (InvocationTargetException ite)
            {
                Throwable cause = ite.getTargetException();
                if (cause instanceof RuntimeException)
                {
                    throw (RuntimeException) cause;
                }
                else
                {
                    SystemEventUtil.logUncaughtException(cause, ctx);
                    return null;
                }
            }
        }

        public void destroy(CallerContext cc)
        {
            synchronized (UPnPControlPoint.inHandlerDataObject)
            {
                // assert(cc == UPnPControlPoint.appInHandler.ctx);
                cc.removeCallbackData(UPnPControlPoint.inHandlerDataObject);
                UPnPControlPoint.appInHandler = null;
                MessageInterceptor.setClientInputInterceptor(null);
            }
        }

        public void active(CallerContext cc) { }
        public void pause(CallerContext cc) {  }
    }

    /**
     * Wrapper class for <code>UPnPOutgoingMessageHandler</code>. Holds the
     * application-supplied handler along with the associated application context.
     * Invokes the <code>handleOutcomingMessage</code> method in the application
     * context.
     */
    private class AppOMH implements CallbackData, UPnPOutgoingMessageHandler
    {
        private final CallerContext ctx;
        private final UPnPOutgoingMessageHandler outHandler;

        public AppOMH(CallerContext ctx, UPnPOutgoingMessageHandler outHandler)
        {
            this.ctx = ctx;
            this.outHandler = outHandler;
        }

        public byte[] handleOutgoingMessage(final InetSocketAddress address,
                                            final UPnPMessage message,
                                            final UPnPOutgoingMessageHandler defaultHandler)
        {
            try
            {
                final byte[][] bytes = { null };
                ctx.runInContextSync(new Runnable()
                {
                    public void run()
                    {
                        bytes[0] = outHandler.handleOutgoingMessage(address, message, defaultHandler);
                    }
                });
                return bytes[0];
            }
            catch (InvocationTargetException ite)
            {
                Throwable cause = ite.getTargetException();
                if (cause instanceof RuntimeException)
                {
                    throw (RuntimeException) cause;
                }
                else
                {
                    SystemEventUtil.logUncaughtException(cause, ctx);
                    return null;
                }
            }
        }

        public void destroy(CallerContext cc)
        {
            synchronized (UPnPControlPoint.outHandlerDataObject)
            {
                // assert(cc == UPnPControlPoint.appOutHandler.ctx);
                cc.removeCallbackData(UPnPControlPoint.outHandlerDataObject);
                UPnPControlPoint.appOutHandler = null;
                MessageInterceptor.setClientOutputInterceptor(null);
            }
        }

        public void active(CallerContext cc) { }
        public void pause(CallerContext cc) {  }
    }
}
