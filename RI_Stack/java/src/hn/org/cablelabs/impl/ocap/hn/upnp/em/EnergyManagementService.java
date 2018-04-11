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

package org.cablelabs.impl.ocap.hn.upnp.em;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.mpe.HNAPI;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.ocap.hn.upnp.ActionStatus;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.server.UPnPManagedServiceImpl;
import org.ocap.hardware.Host;
import org.ocap.hardware.PowerModeChangeListener;
import org.ocap.hn.NetworkInterface;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPErrorResponse;
import org.ocap.hn.upnp.common.UPnPResponse;
import org.ocap.hn.upnp.server.UPnPActionHandler;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedDevice;
import org.ocap.hn.upnp.server.UPnPManagedService;
import org.ocap.hn.upnp.server.UPnPManagedStateVariable;
import org.ocap.hn.upnp.server.UPnPStateVariableHandler;

public final class EnergyManagementService implements UPnPActionHandler, UPnPStateVariableHandler, PowerModeChangeListener
{
    private static final Logger log = Logger.getLogger(EnergyManagementService.class);

    /** UPnP Connection Manager service Type */
    public static final String SERVICE_TYPE = "urn:schemas-upnp-org:service:EnergyManagement:1";

    /** Action names */
    public static final String GET_INTERFACE_INFO   = "GetInterfaceInfo";
    public static final String SERVICE_SUBSCRIPTION = "ServiceSubscription";
    public static final String SERVICE_RENEWAL      = "ServiceRenewal";
    public static final String SERVICE_RELEASE      = "ServiceRelease";
    
    /** State variables */
    public static final String NETWORK_INTERFACE_INFO = "NetworkInterfaceInfo";
    
    private static final String XML_PREAMBLE = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    
    private static final String PRE_NII_ELEMENT = "<" + NETWORK_INTERFACE_INFO + " "
            + "xmlns=\"http://www.upnp.org/schemas/lp:em-NetworkInterfaceInfo.xsd\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xsi:schemaLocation=\"http://www.upnp.org/schemas/NetworkInterfaceInfo.xsd "
            + "http://www.upnp.org/schemas/lp:em-NetworkInterfaceInfo.xsd\">";
    
    private static final String POST_NII_ELEMENT = "</" + NETWORK_INTERFACE_INFO + ">";

    
    /**  UPnPManagedServices which this service is wrapping */
    private final List m_services = new ArrayList();
    
    /** class created to be able to send events when
     *  the NetworkInterfaceMode changes its value.
     */
    private final NetworkInterfaceInfoEventer m_networkInterfaceInfoEventer;   
    private final Host m_host;
    
    // Only Constructor 
    public EnergyManagementService()
    {
        m_host = Host.getInstance();
        
        m_networkInterfaceInfoEventer = new NetworkInterfaceInfoEventer();
        
        m_host.addPowerModeChangeListener(this);        
    }
    
    public void registerService(UPnPManagedService service)
    {
        assert service != null;
        
        // Register as listener for actions on this service
        service.setActionHandler(this);
        m_networkInterfaceInfoEventer.registerVariable(((UPnPManagedServiceImpl)service).getManagedStateVariable(NETWORK_INTERFACE_INFO));
        
        m_services.add(service);
    }
    
    public void initializeStateVariables()
    {
        m_networkInterfaceInfoEventer.set(getNetworkInterfaceInfo());       
    }

    public void powerModeChanged(int newPowerMode)
    {
        if (log.isDebugEnabled())
        {
            log.debug("Notified of power mode change to " + ((newPowerMode == Host.FULL_POWER) ? "Full" : "Low") + " power");
        }
        
        m_networkInterfaceInfoEventer.set(getNetworkInterfaceInfo());
    }
    
    public String getValue(UPnPManagedStateVariable variable)
    {
        if (variable.getName().equalsIgnoreCase(NETWORK_INTERFACE_INFO))
        {
            return getNetworkInterfaceInfo();
        }
        else
        {
            return "";
        }
    }

    public void notifySubscribed(UPnPManagedService service)
    {
        // Nothing to start doing.
        if (log.isDebugEnabled())
        {
            log.debug("notifySubscribed() - called");
        }
    }

    public void notifyUnsubscribed(UPnPManagedService service, int remainingSubs)
    {
        // Nothing to stop doing.
        if (log.isDebugEnabled())
        {
            log.debug("notifyUnsubscribed() - called");
        }        
    }

    public UPnPResponse notifyActionReceived(UPnPActionInvocation action)
    {
        String actionName = action.getAction().getName();

        if (GET_INTERFACE_INFO.equals(actionName))
        {
            return performGetInterfaceInfo(action);
        }
        else if (SERVICE_SUBSCRIPTION.equals(actionName))
        {
            return performServiceSubscription(action);
        }
        else if (SERVICE_RENEWAL.equals(actionName))
        {
            return performServiceRenewal(action);
        }
        else if (SERVICE_RELEASE.equals(actionName))
        {
            return performServiceRelease(action);
        }
        else
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ACTION.getCode(),
                    ActionStatus.UPNP_INVALID_ACTION.getDescription(), action);
        }   
    }

    public void notifyActionHandlerReplaced(UPnPActionHandler replacement)
    {
        if (log.isWarnEnabled())
        {
            log.warn("notifyActionHandlerReplaced() - RI's Energy Management has been replaced by: " +
            replacement.getClass().getName());
        }      
    }
    
    private String getNetworkInterfaceInfo()
    {
        // Build an EnergyManagement view based on UPnP Diagnostics data structure
        // [UDN => [NetworkInterface => [InetAddress]]]
        Map rootDevices = MediaServer.getInstance().getRootDevices();
        Map emView = new HashMap();
        
        for(Iterator i = rootDevices.keySet().iterator(); i.hasNext(); )
        {
            UPnPManagedDevice dev = (UPnPManagedDevice)rootDevices.get(i.next());
            Map niMap = (Map)emView.get(dev.getUDN());
            if(niMap == null)
            {
                niMap = new HashMap();
                emView.put(dev.getUDN(), niMap);
            }
            
            InetAddress[] addrs = dev.getInetAddresses();
            for(int index = 0; index < addrs.length; index++)
            {
                 NetworkInterface ni = getNetworkInterface(addrs[index]);
                 Set ipSet = (Set)niMap.get(ni);
                 if(ipSet == null)
                 {
                     ipSet = new HashSet();
                     niMap.put(ni, ipSet);
                 }
                 ipSet.add(addrs[index]);
            }
        }
        
        // Generate the EnergyManagement state variable
        StringBuffer netInfo = new StringBuffer(XML_PREAMBLE);
        netInfo.append(PRE_NII_ELEMENT);
        {
            // Generate DeviceInterfaces
            for(Iterator i = emView.keySet().iterator(); i.hasNext();)
            {
                String udn = (String)i.next();

                netInfo.append("<DeviceInterface>");
                {
                    // Get array of devices to find friendly name, use first one.
                    UPnPManagedDevice[] devices = UPnPDeviceManager.getInstance().getDevicesByUDN(udn);
                   
                    netInfo.append("<DeviceUUID>").append(udn).append("</DeviceUUID>");
                    netInfo.append("<FriendlyName>").append(devices[0].getFriendlyName()).append("</FriendlyName>");

                    // Generate NetworkInterfaces
                    Map niMap = (Map)emView.get(udn);
                    for(Iterator ni = niMap.keySet().iterator(); ni.hasNext();)
                    {
                        NetworkInterface netInt = (NetworkInterface)ni.next();
                        
                        netInfo.append("<NetworkInterface>");
                        {
                            netInfo.append("<SystemName>").append(netInt.getDisplayName()).append("</SystemName>");
                            netInfo.append("<Description></Description>");
                            netInfo.append("<MacAddress>").append(netInt.getMacAddress()).append("</MacAddress>");                
                            netInfo.append("<InterfaceType>").append(mapInterfaceType(netInt)).append("</InterfaceType>");
                            netInfo.append("<NetworkInterfaceMode>");
                            netInfo.append(HNAPIImpl.nativeGetLPENetworkInterfaceMode(netInt.getDisplayName()));
                            netInfo.append("</NetworkInterfaceMode>");                            
                            netInfo.append("<AssociatedIpAddresses>");
                            {
                                Set ipSet = (Set)niMap.get(netInt);
                                for(Iterator ip = ipSet.iterator(); ip.hasNext();)
                                {
                                    InetAddress addr = (InetAddress)ip.next();
                                    String elemName = "Ipv";
                                    if(addr instanceof Inet4Address)
                                    {
                                        elemName = elemName + "4";
                                    }
                                    else if(addr instanceof Inet6Address)
                                    {
                                        elemName = elemName + "6";
                                    }
                                    else
                                    {
                                        elemName = elemName + "4";
                                        log.warn("Unsupported InetAddress type, assigning Inet4Address");
                                    }

                                    netInfo.append("<").append(elemName).append(">")
                                    .append(addr.getHostAddress())
                                    .append("</").append(elemName).append(">");
                                }
                            }
                            netInfo.append("</AssociatedIpAddresses>");
                    
                            HNAPI.LPEWakeUp lpeWakeUpValues = HNAPIImpl.nativeGetLPEWakeUpVariables(netInt.getDisplayName());
                            if (lpeWakeUpValues != null)
                            {
                                if ((lpeWakeUpValues.wakeOnPattern).equals("") == false)
                                {
                                    netInfo.append("<WakeOnPattern>" + lpeWakeUpValues.wakeOnPattern + "</WakeOnPattern>");
                                }
                                if ((lpeWakeUpValues.wakeSupportedTransport).equals("") == false)
                                {
                                    netInfo.append("<WakeSupportedTransport>" + lpeWakeUpValues.wakeSupportedTransport + "</WakeSupportedTransport>");
                                }
                                if (lpeWakeUpValues.maxWakeOnDelay != 0)
                                {
                                    netInfo.append("<MaxWakeOnDelay>" + lpeWakeUpValues.maxWakeOnDelay + "</MaxWakeOnDelay>");
                                }
                                if (lpeWakeUpValues.dozeDuration != 0)
                                {
                                    netInfo.append("<DozeDuration>" + lpeWakeUpValues.dozeDuration + "</DozeDuration>");
                                }
                            }
                            else
                            {
                                if (log.isWarnEnabled())
                                {
                                    log.warn("lpeWakeUpValues object returned is null");
                                }
                            }
                        }
                        netInfo.append("</NetworkInterface>");
                    }
                }
                netInfo.append("</DeviceInterface>");
            }
        }
        netInfo.append(POST_NII_ELEMENT);
        
        return netInfo.toString();
    }
    
    private UPnPResponse performGetInterfaceInfo(UPnPActionInvocation action)
    {
        if (log.isDebugEnabled())
        {
            log.debug("performGetInterfaceInfo() - called");
        }
        
        return new UPnPActionResponse(new String[]{
                getNetworkInterfaceInfo(),
                getProxiedNetworkInterfaceInfo()}, action);        
    }
    
    private UPnPResponse performServiceSubscription(UPnPActionInvocation action)
    {
        if (log.isDebugEnabled())
        {
            log.debug("performServiceSubscription() - called");
        }
        
        return new UPnPActionResponse(new String[]{"0", "0"}, action);        
    }
    
    private UPnPResponse performServiceRenewal(UPnPActionInvocation action)
    {
        if (log.isDebugEnabled())
        {
            log.debug("performServiceRenewal() - called");
        }
        
        String idStr = action.getArgumentValue("ServiceSubscriptionID");
        
        if("0".equals(idStr))
        {
            return new UPnPActionResponse(new String[]{"0"}, action);
        }
        else
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ID.getCode(), 
                    ActionStatus.UPNP_INVALID_ID.getDescription(), action);        
        }
    }
    
    private UPnPResponse performServiceRelease(UPnPActionInvocation action)
    {
        if (log.isDebugEnabled())
        {
            log.debug("performServiceRelease() - called");
        }
        
        String idStr = action.getArgumentValue("ServiceSubscriptionID");
        
        if("0".equals(idStr))
        {
            return new UPnPActionResponse(new String[]{}, action);
        }
        else
        {
            return new UPnPErrorResponse(ActionStatus.UPNP_INVALID_ID.getCode(), 
                    ActionStatus.UPNP_INVALID_ID.getDescription(), action);        
        }
    }    
    
    private String mapInterfaceType(NetworkInterface netInt)
    {
        String interfaceType = "Other";
        if(netInt.getType() == NetworkInterface.WIRED_ETHERNET)
        {
            interfaceType = "Ethernet";
        }
        else if (netInt.getType() == NetworkInterface.MOCA)
        {
            interfaceType = "MoCA";
        }
        else if (netInt.getType() == NetworkInterface.WIRELESS_ETHERNET)
        {
            interfaceType = "Wi-Fi";
        }
        
        return interfaceType;
    }
    
    private String getProxiedNetworkInterfaceInfo()
    {
        StringBuffer netInfo = new StringBuffer(XML_PREAMBLE);
        netInfo.append(PRE_NII_ELEMENT);
        netInfo.append(POST_NII_ELEMENT);
        
        return netInfo.toString();
    }
    
    // Return the network interface associated with an ip address
    private NetworkInterface getNetworkInterface(InetAddress inetAddress)
    {
        if(inetAddress == null)
        {
            return null;
        }
        
        NetworkInterface[] netInterfaces = NetworkInterface.getNetworkInterfaces();
        
        for(int i = 0; i < netInterfaces.length; i++)
        {
            InetAddress[] ips = netInterfaces[i].getInetAddresses();
            for(int ip = 0; ip < ips.length; ip++)
            {
                if(inetAddress.equals(ips[ip]))
                {
                    return netInterfaces[i];
                }
            }
        }

        return null;
    }
}
