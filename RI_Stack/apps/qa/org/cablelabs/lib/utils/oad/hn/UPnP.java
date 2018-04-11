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

package org.cablelabs.lib.utils.oad.hn;

import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.server.UPnPDeviceManager;
import org.ocap.hn.upnp.server.UPnPManagedDevice;

/**
 * Purpose: This class contains methods related to support UPnP related
 * HN functionality along with methods which utilize Ocap UPnP Diags
 * methods.
*/
public class UPnP
{
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(UPnP.class);

    private static final String MS_DEVICE_TYPE = "urn:schemas-upnp-org:device:MediaServer:2";
    protected static final String CDS_SERVICE_ID = "urn:upnp-org:serviceId:ContentDirectory";
    private static final String UPDATE_OBJECT_ACTON_NAME = "UpdateObject";
    protected static final String CDS_SEARCH_ACTION = "Search";
    private static final String CM_SERVICE_ID = "urn:upnp-org:serviceId:ConnectionManager";
    private static final String CM_GET_CURRENT_CONNECTION_IDS_ACTION = "GetCurrentConnectionIDs";
    private static final String RUIS_DEVICE_TYPE = "urn:schemas-upnp-org:device:RemoteUIServerDevice:1";
    protected static final String RUIS_SERVICE_ID = "urn:upnp-org:serviceId:RemoteUIServer";
    
    // A list of media server indices that support live streaming
    private final Vector m_liveStreamingMediaServerList = new Vector();
    
    // A list of UPnPClientDevices on the network that are RemoteUIServer devices.
    private final Vector m_upnpRuiServerList = new Vector();

    // A list of UPnPClientDevices on the network that support the UPnP CDS service.
    private final Vector m_upnpMediaServerList = new Vector();
    private OcapAppDriverHN m_oadHN;

    UPnP(OcapAppDriverHN oadHN)
    {
        m_oadHN = oadHN;
    }
    
    protected UPnPManagedDevice getLocalRootDevice()
    {
        UPnPManagedDevice device = null;
        UPnPManagedDevice[] devices =
                            UPnPDeviceManager.getInstance().getDevices();

        for (int x = 0; x < devices.length; x++)
        {
            if (devices[x].isRootDevice())
            {
                device = devices[x];
            }
        }

        if (device == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Could not find local root device.");
            }
        }

        return device;
    }

    protected UPnPManagedDevice getLocalMediaServerDevice()
    {
        UPnPManagedDevice device = null;
        UPnPManagedDevice[] devices =
                            UPnPDeviceManager.getInstance().getDevices();

        for (int x = 0; x < devices.length; x++)
        {
            if ("urn:schemas-upnp-org:device:MediaServer:2".equals(devices[x].getDeviceType()))
            {
                device = devices[x];
                break;
            }
        }

        if (device == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Could not find local media server.");
            }
        }

        return device;
    }
    
    /**
     * Returns the friendly name of the HN root device.
     * @return - the friendly name
     */
    public String getHnRootDeviceFriendlyName()
    {
        return getFriendlyName(getLocalRootDevice());
    }

    /**
     * Returns the friendly name of the HN local media server for which the RI
     * currently only supports one at this time.
     * @return - the friendly name
     */
    public String getHnLocalMediaServerFriendlyName()
    {
        return getFriendlyName(getLocalMediaServerDevice());
    }

    /**
     * Returns the friendly name of an UPnPManagedDevice
     * @param device - the device to retrieve the name.
     * @return - the name or null if cannot be found.
     */
    private String getFriendlyName(final UPnPManagedDevice device) {
        if (device == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Device is null, cannot retrieve the friendly name.");
            }

            return null;
        }
        return device.getFriendlyName();
    }

    protected String getHnLocalMediaServerUDN()
    {
        UPnPManagedDevice device = getLocalMediaServerDevice();

        if (device == null) 
        {
            if (log.isInfoEnabled())
            {
                log.info("Error finding local media sever");
            }

            return null;
        }

        return device.getUDN();
    }

    /**
     * Changes and re-broadcasts the friendly name of the HN Root device.
     * @param newName - the name to set
     * @return true if successful
     */
    public boolean hnChangeRootDeviceFriendlyName(String newName)
    {
        final UPnPManagedDevice rootDevice = getLocalRootDevice();
        return changeUPnPDeviceFriendlyName(rootDevice, newName);
    }

    /**
     * Changes and re-broadcasts the friendly name of the HN Media Server device.
     * @param newName - the name to set
     * @return true if successful
     */
    public boolean hnChangeLocalMediaServerFriendlyName(String newName)
    {
        UPnPManagedDevice localDevice = getLocalMediaServerDevice();
        return changeUPnPDeviceFriendlyName(localDevice, newName);
    }

    /**
     * Changes and re-broadcasts the friendly name of an UPnPManagedDevice.
     * @param device - the device whose name will change
     * @param newName - the new name
     * @return
     */
    private boolean changeUPnPDeviceFriendlyName(UPnPManagedDevice device, String newName) {

        if (device == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Device cannot be null.");
            }

            return false;
        }

        if (log.isInfoEnabled())
        {
            log.info("Device's current friendly name to change is: " +
                    device.getFriendlyName());
        }

        device.sendByeBye();
        device.setFriendlyName(newName);
        device.sendAlive();

        if (log.isInfoEnabled())
        {
            log.info("Device friendly name has been set to: " +
                    device.getFriendlyName());
        }

        return true;
    }

    protected boolean hnChangeLocalMediaServerUDN(String newUDN)
    {
        UPnPManagedDevice device = getLocalMediaServerDevice();

        if (device == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Error finding local media sever");
            }

            return false;
        }

        String udn = device.getUDN();

        if (log.isInfoEnabled())
        {
            log.info("Device UDN is currently: " + udn);
        }

        device.sendByeBye();
        device.sendByeBye();
        device.setUDN(newUDN);
        device.sendAlive();

        if (log.isInfoEnabled())
        {
            log.info("Device UDN of '" +  device.getFriendlyName() +
                     "' has been set to:   " + device.getUDN());
        }

        return true;
    }

    protected boolean hnSendRootDeviceByeBye()
    {
        UPnPManagedDevice device = getLocalRootDevice();

        if (device == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Error: device is null. Can't send byebye messages.");
            }

            return false;
        }

        device.sendByeBye();

        if (log.isInfoEnabled())
        {
            log.info("ByeBye messages have been sent.");
        }

        return true;
    }

    protected boolean hnSendRootDeviceAlive()
    {
        UPnPManagedDevice device = getLocalRootDevice();

        if (device == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Error: device is null. Can't send byebye messages.");
            }

            return false;
        }

        device.sendAlive();

        if (log.isInfoEnabled())
        {
            log.info("One ssdp:alive has been sent.");
        }

        return true;
    }
    
    /**
     * Calls CDS:UpdateObject on the local CDS.  See "ContentDirectory service:3 Service Template 
     * Version 1.01" section "2.5.11 UpdateObject()" for details
     * 
     * @param objectId - the id of the CDS object to be updated.
     * @param currentTagValue -  the set of existing object properties (and their values) that are to be updated.
     * @param newTagValue -  how the object is to be updated.
     * @return true if the action invocation succeeded.
     */
    public boolean invokeCdsUpdateObject(String objectId, String currentTagValue, String newTagValue)
    {
        String[] args = {objectId, currentTagValue, newTagValue};
        int localMediaServerIndex = m_oadHN.findLocalMediaServer();
        String localMediaServer = m_oadHN.getMediaServerFriendlyName(localMediaServerIndex);
        if (localMediaServer == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("invokeCdsUpdateObject()- Media server name is null");
            }
        }
        int mediaServerIndex = getUpnpMediaServerIndexByName(localMediaServer);
        UPnPClientService service = getClientService(mediaServerIndex, CDS_SERVICE_ID);
        if (service == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Could not find CDS service advertised by this host.");
            }
            return false;
        }
        UPnPActionResponseHandlerImpl handler = invokeUPnPAction(service, UPDATE_OBJECT_ACTON_NAME, args);
        if (!handler.waitRequestResponse())
        {
            if (log.isInfoEnabled())
            {
                log.info(handler.getResponseDescription());
            }
            return false;
        }
        return true;
    }
    

    protected int getNumUPnPMediaServersOnNetwork()
    {
        m_upnpMediaServerList.clear();
        UPnPClientDevice[] devices = UPnPControlPoint.getInstance().getDevices();
        UPnPClientService[] services = null;
        for (int i = 0; i < devices.length; i++)
        {
            services = devices[i].getServices();
            for (int x = 0; x < services.length; x++)
            {
                if (CDS_SERVICE_ID.equals(services[x].getServiceId()))
                {
                    m_upnpMediaServerList.add(devices[i]);
                }
            }
        }
        return m_upnpMediaServerList.size();
    }
    
    protected int getUpnpMediaServerIndexByName(String name)
    {
        int index = -1;
        UPnPClientDevice device = null;
        if (m_upnpMediaServerList.size() == 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("upnpMediaServerList is empty- attempting to refresh");
            }
        }
        getNumUPnPMediaServersOnNetwork();
        for (int i = 0; i < m_upnpMediaServerList.size(); i++)
        {
            device = (UPnPClientDevice)m_upnpMediaServerList.get(i);
            if (device.getFriendlyName().equals(name))
            {
                index = i;
                break;
            }
        }
        if (index < 0)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Could not find media server with friendly name '" + name + "'.");
            }
        }
        return index;
    }
        
    protected String[] invokeCmGetConnectionIds(int serverIndex)
    {
        String[] scids = {};
        UPnPClientService service = getClientService(serverIndex, CM_SERVICE_ID);
        final String COMMA = ","; 
        
        if (service == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Could not find " + CM_SERVICE_ID + " service on given media server.");
            }
            return scids;
        }
        
        UPnPActionResponseHandlerImpl handler = invokeUPnPAction(service, CM_GET_CURRENT_CONNECTION_IDS_ACTION, null);
        if (handler == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("Error in invoking " + CM_GET_CURRENT_CONNECTION_IDS_ACTION + ".");   
            }
            return scids;  
        }
        if (!handler.waitRequestResponse())
        {
            if (log.isErrorEnabled())
            {
                log.error("Error in invoking " + CM_GET_CURRENT_CONNECTION_IDS_ACTION + ": " + handler.getResponseDescription());
            }
            return scids;   
        }
        else
        {
            String outArg = handler.getOutArgValues()[0];
            if (outArg == null || outArg.length() == 0)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(CM_GET_CURRENT_CONNECTION_IDS_ACTION + " returned no connectionIds.");   
                }
                return scids;
            }
            if (outArg.indexOf(COMMA) > 0) 
            {
                StringTokenizer stok = new StringTokenizer(outArg, COMMA);
                scids = new String[stok.countTokens()];
                for (int x = 0; stok.hasMoreTokens(); x++)
                {
                    scids[x] = stok.nextToken();
                }
            }
            else 
            {
                scids = new String[1];
                scids[0] = outArg;
            }
        }
        return scids;
    }

    /**
     * 
     * @param serverIndex - index of the UPnPClientDevice in the list of UPnPClientDevices corresponding to the given serviceId.
     * @param serviceId - ID of the requested service.
     * @return UPnPClientService representing the service provided by the indexed UPnP device.
     *           Returns null if the UPnP device does not support that service. 
     */
    protected UPnPClientService getClientService(int serverIndex, String serviceId)
    {
        Vector upnpServerList = null;
        String deviceType = null;
        UPnPClientService[] services = null;
        if (RUIS_SERVICE_ID.equals(serviceId))
        {
            upnpServerList = m_upnpRuiServerList;
            deviceType = RUIS_DEVICE_TYPE;
        }
        else 
        {
            upnpServerList = m_upnpMediaServerList;
            deviceType = MS_DEVICE_TYPE;
        }
        try 
        {
            services = ((UPnPClientDevice)upnpServerList.get(serverIndex)).getServices();
        }
        catch (ArrayIndexOutOfBoundsException e)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Could not get service " + serviceId + ".  Device index # " + serverIndex + " is invalid in the list of this device type: " + deviceType);
            }
            return null;
        }
        UPnPClientService service = null;
        for (int x = 0; x < services.length; x++)
        {
            if (services[x].getServiceId().equals(serviceId))
            {
                service = services[x];;
            }
        }
        return service;
    }
    
    /**
     * 
     * @param service - the UPnPClientService on which the action is to be invoked.
     * @param actionName - the name of the action is to be invoked.
     * @param args - the arguments of the action.
     * @return UPnPClientService representing the service name provided by the indexed media server.
     *         returns null if the media server does not support that service. 
     */
    protected UPnPActionResponseHandlerImpl invokeUPnPAction(UPnPClientService service, String actionName, String[] args)
    {
        UPnPActionResponseHandlerImpl handler = new UPnPActionResponseHandlerImpl();
        UPnPAction[] actions = service.getActions();
        UPnPAction action = null; 
        for (int x = 0; x < actions.length; x++)
        {
            if (actions[x].getName().equals(actionName))
            {
                action = actions[x];
            }
        }
        if (action == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Could not find " + actionName + " action in the given service.");
            }
            return null;
        }   
        UPnPActionInvocation ai = null;
        if (args == null)
        {
            String[] emptyArray = {};
            args = emptyArray;
        }
        try {
            ai = new UPnPActionInvocation(args, action);
        } catch (Exception e) {
            if (log.isErrorEnabled())
            {
                log.error("Exception thrown instanciatiating UPnPActionInvocation: ", e);
            }
            return null;
        }
        service.postActionInvocation(ai, handler);
        return handler;
    }
    
    protected int getNumUpnpLiveStreamingMediaServers()
    {
        initializeLiveStreamingMediaServerList();
        return m_liveStreamingMediaServerList.size();
    }
    
    protected String getUpnpLiveStreamingMediaServerInfo(int serverIndex)
    {
        String serverInfo = OcapAppDriverCore.NOT_FOUND;
        if (serverIndex < m_liveStreamingMediaServerList.size())
        {
            Object obj = m_liveStreamingMediaServerList.get(serverIndex);
            
            if (obj instanceof UPnPClientDevice)
            {
                UPnPClientDevice ucd = (UPnPClientDevice)obj;
                serverInfo = ucd.getFriendlyName() + ", " + ucd.getInetAddress() + 
                    ", " + ucd.getUDN() + " (CDS)";
            }
        }
        return serverInfo;
    }
    
    protected int getNumUPnPRuiServersOnNetwork()
    {
        m_upnpRuiServerList.clear();
        UPnPClientDevice[] devices = UPnPControlPoint.getInstance().getDevices();
        UPnPClientService[] services = null;
        for (int i = 0; i < devices.length; i++)
        {
            services = devices[i].getServices();
            for (int x = 0; x < services.length; x++)
            {
                if (RUIS_SERVICE_ID.equals(services[x].getServiceId()))
                {
                    m_upnpRuiServerList.add(devices[i]);
                }
            }
        }
        return m_upnpRuiServerList.size();
    }
    
    protected int getUpnpRuiServerIndexByName(String name)
    {
        int index = -1;
        UPnPClientDevice device = null;
        if (m_upnpRuiServerList.size() == 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("upnpRuiServerList is empty- attempting to refresh");
            }
        }
        getNumUPnPRuiServersOnNetwork();
        for (int i = 0; i < m_upnpRuiServerList.size(); i++)
        {
            device = (UPnPClientDevice)m_upnpRuiServerList.get(i);
            if (device.getFriendlyName().equals(name))
            {
                index = i;
                break;
            }
        }
        if (index < 0)
        {
            if (log.isWarnEnabled())
            {
                log.warn("Could not find remote UI server with friendly name '" + name + "'.");
            }
        }
        return index;
    }
        
    
    /**
     * Populates the list of media servers that support live streaming
     */
    private void initializeLiveStreamingMediaServerList()
    {
        Object sync = new Object();
        boolean isTuner = false;
        m_liveStreamingMediaServerList.clear();
        
        LiveStreamingUPnPActionResponseHandler handler = new LiveStreamingUPnPActionResponseHandler(sync, isTuner);
        final String CDS = "urn:schemas-upnp-org:service:ContentDirectory:3";
        final String GFL = "GetFeatureList";
        final String[] IN_ARGS = {};
        final long TIMEOUT = 30000L;
        if (log.isInfoEnabled())
        {
            log.info("Creating list of live streaming media servers");
        }
        UPnPClientDevice[] devices = UPnPControlPoint.getInstance().getDevicesByServiceType(CDS);
        for (int devIdx = 0; devIdx < devices.length; devIdx++)
        {
            if (log.isInfoEnabled())
            {
                log.info("Investigating device: " + devices[devIdx].getFriendlyName());
            }
            UPnPClientService[] services = devices[devIdx].getServices();
            for(int srvIdx = 0; srvIdx < services.length; srvIdx++)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Discovered service : " + services[srvIdx].getServiceType());
                }
                if (!CDS.equals(services[srvIdx].getServiceType()))
                {
                    continue;
                }
                UPnPAction[] actions = services[srvIdx].getActions();
                for (int actIdx = 0; actIdx < actions.length; actIdx++)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Action : " + actions[actIdx].getName());
                    }
                    if (GFL.equals(actions[actIdx].getName()))
                    {
                        synchronized(sync) 
                        {
                            isTuner = false;
                            services[srvIdx].postActionInvocation(new UPnPActionInvocation(IN_ARGS, actions[actIdx]), handler);
                            try 
                            {
                                long endTime = System.currentTimeMillis() + TIMEOUT;
                                while (!handler.m_responseReceived && System.currentTimeMillis() < endTime)
                                {
                                    sync.wait(1000L);
                                    isTuner = handler.m_isTuner;
                                }
                            } 
                            catch (InterruptedException e) 
                            {   
                                if (log.isInfoEnabled())
                                {
                                    log.info("Exception thrown while waiting for " +
                                            "action response from device: " + 
                                            devices[devIdx].getFriendlyName());
                                }
                            }
                        }
                        if (isTuner) 
                        {
                            m_liveStreamingMediaServerList.add(devices[devIdx]);
                            if (log.isInfoEnabled())
                            {
                                log.info(devices[devIdx].getFriendlyName() + " published a ChannelContentItem");
                            }
                        }
                        else 
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(devices[devIdx].getFriendlyName() + " did not published a ChannelContentItem");
                            }
                        }
                        break;
                    }
                }
            }
        }
    }
}
