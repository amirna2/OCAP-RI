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

package org.cablelabs.xlet.DvrExerciser;

import java.util.Vector;

/**
 * This class supports the data representation for items displayed in a SelectorList.
 * Extend this class to provide another type of data list. 
 */
public abstract class SelectorSource 
{
    /**
     * Generate a list of items to be displayed based on the type supplied.
     * Put items into the supplied vector.
     * 
     * @param   list    put items into this vector
     */
    public abstract void populateSelectorList(Vector list);
    
    /**
     * Formulate string to be used for a specific item when list of items
     * is displayed in SelectorList
     * 
     * @param   list    list of data items to be displayed
     * @param   i       index of the item in list of items to formulate display
     *                  string 
     * @return  string to be used for displaying the item at the specified index
     * from the supplied list, null if problems are encountered                 
     */
    public String getSelectorItemDisplayStr(Vector list, int i)
    {
        return "";
    }
    
    /**
     * Formulate a special string to be displayed in DvrExerciser log message
     * area which reports that an item was selected.  If no message is required
     * for a specific type, it can return null.
     * 
     * @param item  item that was selected from list
     * @return  string to be displayed in DvrExerciser log message screen area.
     */
    public String getSelectorItemSelectedStr(Object item)
    {
        return "";
    }
    
    /**
     * Perform necessary actions since item has been selected from list.
     * 
     * @param item  item from list which was selected
     */
    public void itemSelected(Object item)
    {
        // do nothing
    }
}
/*    
    public void populateSelectorList(int type, Vector list)
    {
        switch (type)
        {
        case LIST_TYPE_INTERFACES:
            NetworkInterface[] nis = NetworkInterface.getNetworkInterfaces();
            for (int i = 0; i < nis.length; i++)
            {
                list.add(nis[i]);
            }
            break;

        case LIST_TYPE_EVENTS:
            if (HNTest.m_eventReceivedList != null)
            {
                for (int i = 0; i < HNTest.m_eventReceivedList.size(); i++)
                {
                    DeviceEvent event = (DeviceEvent)HNTest.m_eventReceivedList.elementAt(i);
                    list.add(event);
                }
            }
            break;

        case LIST_TYPE_DEVICES:
            NetList deviceList = NetManager.getInstance().getDeviceList(null);
            if (deviceList.size() > 0)
            {
                Enumeration enumerator = deviceList.getElements();
                while (enumerator.hasMoreElements())
                {
                    list.add(enumerator.nextElement());
                }
            }
            break;

        case LIST_TYPE_MEDIA_SERVERS:
            NetList serviceList = NetManager.getInstance().getNetModuleList(null);
            if (serviceList.size() > 0)
            {
                System.out.println("SelectorSource.populateSelectorList() - initial list size: " +
                        serviceList.size());
                Enumeration enumerator = serviceList.getElements();
                while (enumerator.hasMoreElements())
                {
                    Object obj = enumerator.nextElement();
                    if (obj instanceof ContentServerNetModule)
                    {
                        System.out.println("SelectorSource.populateSelectorList() - adding item");
                        list.add(obj);
                    }
                }
            }
            break;

        case LIST_TYPE_CONTENT_ITEMS:
            String startingID = "0";
            if (HNTest.m_remoteContentEntry != null)
            {
                startingID = HNTest.m_remoteContentEntry.getID();
            }
            DvrHNTest.getContentViaBrowse(HNTest.m_mediaServer, startingID, list, false);
            break;
            
        case LIST_TYPE_REMOTE_SERVICES:
            populateRemoteServices(list);
            break;
            
        default:
            System.out.println("ERROR - populateList() - unsupported type: " + 
                    type);
        }
    }
    
    private void populateRemoteServices(Vector list)
    {
        Vector contentList = new Vector();
        Vector nreList = new Vector();
        Vector rciList = new Vector();
        
        DvrHNTest.getContentViaSearch(HNTest.m_mediaServer, contentList, false);
        System.out.println("SelectorSource.populateRemoteServices() - initial list size: " + 
                contentList.size());                       
        for (int i = 0; i < contentList.size(); i++)
        {
            ContentEntry ce = (ContentEntry)contentList.get(i);
            
            // Put the objects in appropriate lists
            if (ce instanceof NetRecordingEntry)
            {
                nreList.add(ce);
            }
            else if (ce instanceof RecordingContentItem)
            {
                rciList.add(ce);
            }
            else
            {
                System.out.println("Unexpected object in list, class: " + ce.getClass().getName());                       
            }            
        }
        
        for (int i = 0; i < nreList.size(); i++)
        {
            // ...get the NetRecordingEntry corresponding to the selection
            NetRecordingEntry nre = (NetRecordingEntry)nreList.get(i);
            
            // Get the associated id to match up with item
            MetadataNode mn = nre.getRootMetadataNode();
            String name = (String)mn.getMetadata("dc:title");
            String entryIdStrDidl = (String)mn.getMetadata("didl-lite:@id");
            String entryIdStr = (String)mn.getMetadata("@id");
            
            for (int j = 0; j < rciList.size(); j++)
            {
                RecordingContentItem rci = (RecordingContentItem)rciList.get(j);
                mn = rci.getRootMetadataNode();
                String curEntryIdStr = (String)mn.getMetadata("ocap:netRecordingEntry");
                if ((curEntryIdStr.equals(entryIdStr)) || (curEntryIdStr.equals(entryIdStrDidl)))
                {
                    list.add(new RemoteService("Remote Service: " + name, rci.getItemService()));
                }
            }            
        }
    }
    
    protected class RemoteService
    {
        String m_displayStr;
        Service m_service;
        protected RemoteService(String displayStr, Service service)
        {
            m_displayStr = displayStr;
            m_service = service;
        }
    }
    public String getSelectorItemDisplayStr(int type, Vector list, int i)
    {
        String displayStr = null;
        Device device;
        ContentEntry ce;
        MetadataNode mn;
        
        switch (type)
        {
        case LIST_TYPE_INTERFACES:
            NetworkInterface nis = (NetworkInterface)list.get(i);
            displayStr = "NI " + (i+1) + ": Name: " + nis.getDisplayName() + 
            ", Type: " + nis.getType() + ", Mac:" + 
            nis.getMacAddress() + ", Addr: " + nis.getInetAddress();
            break;

        case LIST_TYPE_EVENTS:
            DeviceEvent event = (DeviceEvent)list.get(i);
            device = (Device)event.getSource();
            displayStr = "Event " + (i+1) + ", Source: " 
                + device.getProperty(Device.PROP_FRIENDLY_NAME) + ", Type: " 
                + HNTest.getEventTypeStr(event);
            break;
            
        case LIST_TYPE_DEVICES:
            device = (Device)list.get(i);
            displayStr = "Device: " + device.getName() + ", Type: " + 
                            device.getType() + ", " + device.getProperty(Device.PROP_UDN);
            break;
            
        case LIST_TYPE_MEDIA_SERVERS:
            ContentServerNetModule cds = (ContentServerNetModule)list.get(i);
            displayStr = cds.getDevice().getProperty(Device.PROP_FRIENDLY_NAME) + ", " + 
                            cds.getDevice().getInetAddress().getHostAddress() + ", " + 
                            cds.getDevice().getProperty(Device.PROP_UDN);
            break;
            
        case LIST_TYPE_CONTENT_ITEMS:
            ce = (ContentEntry)list.get(i);
            mn = ce.getRootMetadataNode();
            if (mn != null)
            {
                displayStr = "Content Title: " + mn.getMetadata("dc:title");
            }
            else
            {
                displayStr = "Content Item: " + (i+1);
            }
            break;
            
        case LIST_TYPE_REMOTE_SERVICES:
            displayStr = ((RemoteService)list.get(i)).m_displayStr;
            break;
            
        default:
            System.out.println("ERROR - getItemDisplayStr() - unsupported type: " + 
                    type);
        }
        return displayStr;
    }

    public String getSelectorItemSelectedStr(int type, Object item)
    {
        String itemStr = null;

        switch (type)
        {
        case LIST_TYPE_DEVICES:
            Device device = (Device)item;
            itemStr = "Current Device: " + device.getProperty(Device.PROP_FRIENDLY_NAME) + 
                        " " + device.getProperty(Device.PROP_UDN);
            break;
            
        case LIST_TYPE_MEDIA_SERVERS:
            ContentServerNetModule mediaServer = (ContentServerNetModule)item;
            itemStr = "Current Media Server: " + mediaServer.getDevice().getProperty(Device.PROP_FRIENDLY_NAME) + 
                        " " + mediaServer.getDevice().getProperty(Device.PROP_UDN);
            break;
            
        case LIST_TYPE_CONTENT_ITEMS:
            if (item instanceof ContentItem)
            {
                itemStr = "Content Item selected for Remote JMF Playback";
            }
            break;
            
        case LIST_TYPE_REMOTE_SERVICES:
            itemStr = ((RemoteService)item).m_displayStr + " selected for Playback";            
            break;

        default:
            // No special string for other types
        }
        return itemStr;        
    }
    
    public void itemSelected(int type, Object item)
    {
        switch (type)
        {
            case LIST_TYPE_MEDIA_SERVERS:
                HNTest.m_mediaServer = (ContentServerNetModule)item;
                break;
                
            case LIST_TYPE_CONTENT_ITEMS:
                if (item instanceof ContentContainer)
                {
                    DvrExerciser.getInstance().logIt("Content Container Selected");
                    HNTest.displaySelectorList(SelectorSource.LIST_TYPE_CONTENT_ITEMS);
                }
                else
                {
                    HNTest.playbackViaRemoteJMF();                                
                }
                break;
                
            case LIST_TYPE_REMOTE_SERVICES:
                HNTest.playbackViaRemoteService();
                break;

            case LIST_TYPE_DEVICES:
                HNTest.m_device = (Device)item;
                break;

            default:
                // No additional processing for other types
        }
    }
*/
