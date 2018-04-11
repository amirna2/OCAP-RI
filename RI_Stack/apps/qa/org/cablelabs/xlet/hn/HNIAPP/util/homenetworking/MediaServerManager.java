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
package org.cablelabs.xlet.hn.HNIAPP.util.homenetworking;

import java.util.Enumeration;
import java.util.Hashtable;

import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;
import org.ocap.hn.Device;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.NetModule;
import org.ocap.hn.NetModuleEvent;
import org.ocap.hn.NetModuleEventListener;
import org.ocap.hn.PropertyFilter;
import org.ocap.hn.upnp.client.UPnPClientDevice;
import org.ocap.hn.upnp.client.UPnPClientDeviceListener;
import org.ocap.hn.upnp.client.UPnPControlPoint;
import org.ocap.hn.upnp.server.UPnPDeviceManager;

/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */
public class MediaServerManager implements NetModuleEventListener,UPnPClientDeviceListener
{
    private HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    /**
     * Instance of the class.
     */
    private static MediaServerManager mediaServerMgrInstance;

    /**
     * List of media servers
     */
    private Hashtable m_mediaServerList;
    
    private Hashtable m_allDevicesList;

    /**
     * Returns the singleton instance of this class.
     */
    public static MediaServerManager getInstance()
    {
        if (mediaServerMgrInstance == null)
        {
            mediaServerMgrInstance = new MediaServerManager();
        }
        return mediaServerMgrInstance;
    }

    /**
     * Default constructor.
     */
    private MediaServerManager()
    {
        m_mediaServerList = new Hashtable();
        m_allDevicesList = new Hashtable();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.ocap.hn.NetModuleEventListener#notify(org.ocap.hn.NetModuleEvent)
     */
    public void notify(NetModuleEvent e)
    {
        NetModule module = (NetModule) e.getSource();
        switch (e.getType())
        {
            case NetModuleEvent.MODULE_ADDED: // NetManager is initialized.
                // new module is registered to home network
                Device device = module.getDevice();
                hnLogger.homeNetLogger("inside module add :" + device.getName());
                addMediaServer(device, module.getNetModuleId());
                break;
            case NetModuleEvent.MODULE_REMOVED:
                // module is removed from home network.
                hnLogger.homeNetLogger("inside module remove " + module.getDevice().getName());
                removeModule(module);
                break;
            default:
                break;
        }
    }

    /**
     * This method retrieve the Device List from the NetManager and registers
     * itself as NetModule listener.
     * 
     * @throws InterruptedException 
     */
    public void initialize() 
    {
        // Retrieve the NetManager and register as listener
        UPnPControlPoint.getInstance().addDeviceListener(this);
        NetManager netManager = NetManager.getInstance();
        if (netManager == null)
        {
            return;
        }
        netManager.addNetModuleEventListener(this);
       
        // Get the list of media servers
        NetList list = null;
        list = netManager.getDeviceList((PropertyFilter) null);
        if (list == null)
        {
            return;
        }
        Device device;
        Object obj;
        String type;
        for (int i = 0; i < list.size(); i++)
        {
            obj = list.getElement(i);
            if (obj == null)
            {
                continue;
            }
            device = (Device) obj;
            type = device.getType();
            if (type == null)
            {
                continue;
            }
            if (type.equals(Device.TYPE_MEDIA_SERVER))
            {
                hnLogger.homeNetLogger("Device name:" + device.getName());
                addMediaServer(device, getNetModuleID(device));
                addAllDevices(device);
            }
            else
            {
                hnLogger.homeNetLogger("Other devices are " + device.getName());
                addAllDevices(device);
            }
        }
    }

    private void addAllDevices(Device device)
    {
        if (device != null)
        {
            String name = device.getProperty(Device.PROP_FRIENDLY_NAME);
            // make sure this device doesn't exist in the list
            if (m_allDevicesList.get(name) == null)
            {
                // add it to the list
                m_allDevicesList.put(name, device);
            }
        }
        
    }

    /**
     * Returns a list of media servers
     * 
     * @return list of MediaServer objects
     */
    public Enumeration getMediaServers()
    {
        return m_mediaServerList.elements();
    }

    public Enumeration getAllDevices()
    {
        return m_allDevicesList.elements();
    }
    
    public Hashtable getAllDevicesHash()
    {
        return m_allDevicesList;
    }
    
    /**
     * This method creates an instance of MediaServer class and adds it to the
     * Media Server list.
     * 
     * @param device
     *            - Device object
     * @param netModuleID
     *            - net module id of the device
     */
    public void addMediaServer(Device device, String netModuleID)
    {
        if (device != null && device.getType().equals(Device.TYPE_MEDIA_SERVER))
        {
            String name = device.getProperty(Device.PROP_FRIENDLY_NAME);
            // make sure this device doesn't exist in the list
            if (m_mediaServerList.get(name) == null)
            {
                // add it to the list
                MediaServer mediaServer = new MediaServer(device, netModuleID);
                m_mediaServerList.put(name, mediaServer);
                // Activate the media server
                mediaServer.activate();
            }
        }
    }

    /**
     * Removes the Media Server corresponding to the NetModule from the Media
     * server list.
     * 
     * @param module
     *            - NetModule of the Media Server to be removed.
     */
    public void removeModule(NetModule module)
    {
        // Find this media server in table using device net module id
        String netModuleID = module.getNetModuleId();

        // Look for media servers with this net module id
        Enumeration keys = m_mediaServerList.keys();
       
        Enumeration servers = m_mediaServerList.elements();
        
        while ((keys.hasMoreElements()) && (servers.hasMoreElements()))
        {
            String name = (String) keys.nextElement();
            MediaServer ms = (MediaServer) servers.nextElement();
            if ((ms.getNetModuleID() != null) && (ms.getNetModuleID().equalsIgnoreCase(netModuleID)))
            {
                ms.removedFromNetwork();
                m_mediaServerList.remove(name);
            }
        }
        
    }

    /**
     * Retrieves the Net module ID of the Content Server NetModule
     * 
     * @param device
     *            Device Object
     * @return NetModule ID
     */
    private String getNetModuleID(Device device)
    {
        String netModuleId = null;
        NetList list = device.getNetModuleList();
        if (list != null)
        {
            Enumeration e = list.getElements();
            int i = 0;
            while (e.hasMoreElements())
            {
                NetModule netModule = (NetModule) e.nextElement();
                if (netModule.getNetModuleType().equalsIgnoreCase(NetModule.CONTENT_SERVER))
                {
                    if (netModuleId == null)
                    {
                        netModuleId = netModule.getNetModuleId();
                    }
                }
                i++;
            }
        }
        return netModuleId;
    }

    public void notifyDeviceAdded(UPnPClientDevice device)
    {

        hnLogger.homeNetLogger("Client device added :" + device.getFriendlyName());
       
    }

    public void notifyDeviceRemoved(UPnPClientDevice device)
    {
       
        hnLogger.homeNetLogger("Removing client device " + device.getFriendlyName());
        Enumeration allKeys = m_allDevicesList.keys();
        while (allKeys.hasMoreElements())
        {
            String name = (String) allKeys.nextElement();
            if (device.getFriendlyName().equalsIgnoreCase(name))
            {
                hnLogger.homeNetLogger("Removing client device in the list:" + device.getFriendlyName());
            m_allDevicesList.remove(name);
            }
        }
    }
}
