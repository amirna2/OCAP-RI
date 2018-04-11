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

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.ocap.hn.Device;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetModule;
import org.ocap.hn.NetModuleEvent;
import org.ocap.hn.NetModuleEventListener;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientDeviceListener;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPClientStateVariable;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.client.UPnPStateVariableListener;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPGeneralErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;

/**
 * NetModuleImpl - implementation class for <code>NetModule</code>.
 * 
 * @author Ben Foran (Flashlight Engineering and Consulting)
 * @author Dan Woodard (Flashlight Engineering and Consulting)
 * 
 * @version $Revision$
 * 
 * @see {@link org.ocap.hn.NetModule}
 */
public class NetModuleImpl implements NetModule, UPnPStateVariableListener, UPnPClientDeviceListener
{
    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(NetModuleImpl.class);

    /** Identifies a Content Directory Service type */
    public static final int UNKNOWN_SERVICE_TYPE = -1;

    protected final UPnPClientService uService;
    private final Properties netModuleProperties;
    private static UPnPControlPoint ucp = null;
    
    // Registered HN Ext Net Module Event listeners
    private final Vector netModuleListeners = new Vector();

    public NetModuleImpl(UPnPClientService service)
    {
        this.uService = service;
        this.netModuleProperties = getNetModuleProperties();
    }

    public Device getDevice()
    {
        return new DeviceImpl(uService.getDevice());
    }

    public Enumeration getKeys()
    {
        return netModuleProperties.keys();
    }

    public String getNetModuleId()
    {
        return uService.getServiceId();
    }

    public String getNetModuleType()
    {
        String moduleType = "UNDEFINED";
        if(uService.getServiceType() != null)
        {
            if (uService.getServiceType().indexOf(UPnPConstants.CONTENT_DIRECTORY_URN) >= 0)
            {
                return NetModule.CONTENT_SERVER;
            }
            else if (uService.getServiceType().indexOf(UPnPConstants.SCHEDULED_RECORDING_URN) >= 0)
            {
                return NetModule.CONTENT_RECORDER;
            }
            else if (uService.getServiceType().indexOf(UPnPConstants.RENDERING_CONTROL_URN) >= 0)
            {
                return NetModule.CONTENT_RENDERER;
            }
        }
 
        return moduleType;
    }

    public String getProperty(String key)
    {
        return netModuleProperties.getProperty(key);
    }

    // *TODO* - this is part of NetModule methods
    public boolean isLocal()
    {
        if (new DeviceImpl(uService.getDevice()).isLocal())
        {
            return true;
        }
        return false;
    }

    public void addNetModuleEventListener(NetModuleEventListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("addNetModuleEventListener - " + listener.toString() +
                    " for service: " + uService.getServiceId() + ", device: " +
                    uService.getDevice().getFriendlyName());
        }

        synchronized (netModuleListeners)
        {
            // Make sure this listener isn't already in the list
            if (!netModuleListeners.contains(listener))
            {
                netModuleListeners.add(listener);

                if (netModuleListeners.size() == 1)
                {
                    // Register with control point to get notification now that there is a listener
                    if (ucp == null)
                    {
                        ucp = UPnPControlPoint.getInstance();
                    }
                    ucp.addDeviceListener(this);
                    uService.addStateVariableListener(this);
                }                            
            }
        }
    }

    public void removeNetModuleEventListener(NetModuleEventListener listener)
    {
        if (log.isDebugEnabled())
        {
            log.debug("removeNetModuleEventListener - " + listener.toString());
        }

        synchronized (netModuleListeners)
        {
            netModuleListeners.remove(listener);

            if (netModuleListeners.size() == 0)
            {
                // Unregister with control point now that there are no listeners
                ucp.removeDeviceListener(this);
                uService.removeStateVariableListener(this);
            }            
        }
    }

    /**
     * Gets action associated with this net module which matches
     * the supplied name.
     * 
     * @param action    name of action to return
     * 
     * @return named action, null if no action matches
     */
    public UPnPAction getActionByName(String action)
    {
        return uService.getAction(action);
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
        // Determine if this is the upnp device associated with this net module
        if (device.getUDN().equals(uService.getDevice().getUDN()))
        {
            // this service's device, need to send net module event
            if (log.isDebugEnabled())
            {
                log.debug("notifyDeviceRemoved() - notifying listeners net module removed device: " +
                        device.getFriendlyName() + ", service: " + uService.getServiceId());
            }
            notifyNetModuleEventListeners(new NetModuleEvent(NetModuleEvent.MODULE_REMOVED, this));
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
        if (log.isDebugEnabled())
        {
            log.debug("notifyValueChanged() - called for device: " +
                    uService.getDevice().getFriendlyName() + ", service: " + uService.getServiceId());
        }
        // Determine if variable is associated with underlying service
        UPnPClientService vService = variable.getService();
        if ((vService.getServiceId().equals(uService.getServiceId())) &&
            (vService.getDevice().getUDN().equals(uService.getDevice().getUDN())))
        {
            // State variable of underlying service has changed, send net module event
            if (log.isDebugEnabled())
            {
                log.debug("notifyValueChanged() - notifying listeners net module state changed, device: " +
                        uService.getDevice().getFriendlyName() + ", service: " + uService.getServiceId());
            }
            notifyNetModuleEventListeners(new NetModuleEvent(NetModuleEvent.STATE_CHANGE, this));
        }                
    }

    /**
     * Notifies the listener that the control point has successfully
     * subscribed to receive state variable eventing from the specified
     * service.
     *
     * @param service The UPnP service that was subscribed to.
     */
    public void notifySubscribed(UPnPClientService service)
    {
        // Nothing to do here
    }
    
    /**
     * Notifies the listener that the control point has successfully
     * unsubscribed from receiving state variable eventing from the specified
     * service.
     *
     * @param service The UPnP service that was un-subscribed from.
     */
    public void notifyUnsubscribed(UPnPClientService service)
    {
        // Nothing to do here
    }

    /**
     * notifyNetModuleEventListeners - method for informing all interested
     * listeners that the <code>NetModuleEvent</code> has occurred either
     * because the <code>NetModule</code> has been updated or has changed state.
     * 
     * @param netModuleEvent
     *            - the event that caused this notification to occur.
     */
    private void notifyNetModuleEventListeners(final NetModuleEvent netModuleEvent)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyNetModuleEventListeners() - called with event: " + netModuleEvent.getType() +
                    " service: " + uService.getServiceId() + ", device: " + 
                    uService.getDevice().getFriendlyName());
        }
        
        synchronized (netModuleListeners)
        {
            for (int i = 0; i < netModuleListeners.size(); i++)
            {
                NetModuleEventListener listener = (NetModuleEventListener)netModuleListeners.get(i);
                listener.notify(netModuleEvent);
            }
        }
    }
    
    /**
     * Translate the error code in UPnPResponse into NetActionEvent error code.
     * 
     * @param response  result of UPnP action
     * 
     * @return  appropriate NetActionEvent error code which translates to 
     *          UPnPResponse error code.
     */
    protected int getNetActionEventErrorCode(UPnPResponse response)
    {
        int errorCode = -1;
        if (response instanceof UPnPErrorResponse)
        {
            errorCode = ((UPnPErrorResponse)response).getHTTPResponseCode();
        }
        else if (response instanceof UPnPGeneralErrorResponse)
        {
            errorCode = ((UPnPGeneralErrorResponse)response).getHTTPResponseCode();
        }
        return errorCode;
    }
    
    /**
     * Translate the status code in UPnPResponse into NetActionEvent status code.
     * 
     * @param response  result of UPnP action
     * 
     * @return  appropriate NetActionEvent status code which translates to 
     *          UPnPResponse status code.
     */
    protected int getNetActionEventStatusCode(UPnPResponse response)
    {
        // Set status code to completed by default
        int statusCode = NetActionEvent.ACTION_FAILED;
        
        if (response instanceof UPnPActionResponse)
        {
           statusCode = NetActionEvent.ACTION_COMPLETED;
        }
        
        return statusCode;
    }
    
    /**
     * getNetModuleProperties - method used for obtaining all of the properties
     * for this <code>NetModule</code>.
     * 
     * @return a <code>Properties</code> object which contains all of the key,
     *         value pairs for the properties of this <code>NetModule</code>.
     */
    private Properties getNetModuleProperties()
    {
        Properties netModuleProperties = new Properties();

        netModuleProperties.put(NetModule.PROP_NETMODULE_ID, uService.getServiceId());
        
        netModuleProperties.put(NetModule.PROP_DESCRIPTION_URL, uService.getSCPDURL());
        netModuleProperties.put(NetModule.PROP_CONTROL_URL, uService.getControlURL());
        netModuleProperties.put(NetModule.PROP_EventSub_URL, uService.getEventSubURL());
        netModuleProperties.put(NetModule.PROP_NETMODULE_TYPE, getNetModuleType());

        return netModuleProperties;
    }

    protected UPnPClientService getService()
    {
        return uService;
    }    
}
