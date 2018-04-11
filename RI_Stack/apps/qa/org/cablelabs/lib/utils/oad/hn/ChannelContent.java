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

import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import javax.tv.locator.Locator;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentEntryFactory;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.MetadataNode;
import org.ocap.net.OcapLocator;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * Purpose: This class contains methods related to support Channel Content items
 * and live streaming HN functionality. 
*/
public class ChannelContent
{
    private static final Logger log = Logger.getLogger(ChannelContent.class);
    
    private OcapAppDriverHN m_oadHN = null;
    private OcapAppDriverCore m_oadCore = null;
    
    /**
     * A HashMap to map ChannelContentItem indices to their index in the CDS
     */
    private HashMap m_channelIndexMap;
    
    ChannelContent(OcapAppDriverHN oadHN, OcapAppDriverCore oadCore)
    {
        m_oadHN = oadHN;
        m_oadCore = oadCore;
        m_channelIndexMap = new HashMap();
    }
        
    protected boolean isChannelContentItem(int serverIndex, int index)
    {
        boolean isChannel = false;

        Vector contentItems = m_oadHN.getContentItems(serverIndex);
        if ((contentItems != null) && (index >=0) && (index < contentItems.size()))
        {
            ContentEntry entry = (ContentEntry)contentItems.get(index);
            if (entry instanceof ChannelContentItem)
            {
                isChannel = true;
            }
        }
        return isChannel;
    }

    protected int hnGetNumChannelContentItems(int serverIndex)
    {
        int size = -1;
   
        Vector contentItems = m_oadHN.getContentItems(serverIndex);
        if (contentItems != null)
        {
            Vector cci = new Vector();
            for (int i = 0; i < contentItems.size(); i++)
            {
                ContentEntry entry = (ContentEntry)contentItems.get(i);
                if (entry instanceof ChannelContentItem)
                {
                    cci.add(entry);
                }
            }
            size = cci.size();
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("hnGetNumChannelContentItems() - null content item list");
            }
        }
        return size;
    }            

    protected String getHnChannelContentItemURL(int serverIndex, int contentItemIndex)
    {
        String retVal = null;
        Vector contentItems = m_oadHN.getContentItems(serverIndex);
        if (contentItems != null)
        {
            if ((contentItemIndex > -1) && (contentItemIndex < contentItems.size()))
            {    
                Object obj = contentItems.get(contentItemIndex);
                if (obj instanceof ChannelContentItem)
                {
                    ChannelContentItem cci = (ChannelContentItem)obj;
                    retVal =  cci.getItemService().getLocator().toExternalForm().trim();
                }
            }
        }
        return retVal;
    }

    protected int getChannelItemIndexByName(int serverIndex, String channelName)
    {
        int i = 0;

        // Get list of content items for this server
        Vector contentItems = m_oadHN.getContentItems(serverIndex);        
        if (contentItems != null)
        {
            if (log.isInfoEnabled())
            {
                log.info("getChannelItemIndex: content list size: " + contentItems.size() +
                        ", looking for channel name: " + channelName);
            }
            for (i = 0; i < contentItems.size(); i++)
            {
                ContentEntry entry = (ContentEntry)contentItems.get(i);
                if (entry instanceof ChannelContentItem)
                {
                    if (((ChannelContentItem)entry).getChannelName().equalsIgnoreCase(channelName))
                    {
                        return i;
                    }
                }
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("getChannelItemIndex: content list NULL!");
            }
        }
        return -1;
    }
    
    /**
     * The channelIndex parameter indicates the relative index of the published
     * ChannelContentItem in the server at the given serverIndex. So if there
     * are four ChannelContentItems published, index 1 would be for the second
     * published ChannelContentItem. 
     */
    protected int getChannelItemIndex(int serverIndex, int channelIndex)
    {
        m_channelIndexMap.clear();
        Integer channelIndexWrapper = new Integer(channelIndex);
        int channelContentItemIndex = -1;
        Vector contentItems = m_oadHN.getContentItems(serverIndex);
        if (channelIndex >= 0)
        {
            int channelCount = 0;
            Vector channelContentItems = new Vector();
            for (int i = 0; i < contentItems.size(); i++)
            {
                ContentEntry entry = (ContentEntry)contentItems.get(i);
                if (entry instanceof ChannelContentItem)
                {
                    channelContentItems.add(entry);
                    m_channelIndexMap.put(new Integer(channelCount), new Integer(i));
                    channelCount++;
                }
            }
            if (m_channelIndexMap.containsKey(channelIndexWrapper))
            {
                Integer returnIndex = (Integer)m_channelIndexMap.get(channelIndexWrapper);
                return returnIndex.intValue();
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("getChannelItemIndex() - invalid channel index: " +
                            channelIndex);
                }            
            }
        }

        return channelContentItemIndex;
    }

    /** 
     * Returns the index of the ChannelContentItem in the CDS that relates to
     * the Service at the given index
     * @param channelIndex the index of the local Service
     * @return the index in the CDS of the ChannelContentItem in the CDS matching
     * the Service at the given index, or -1 for error
     */
    protected int getIndexForLocalChannel(int channelIndex, int serverIndex)
    {
        Vector services = m_oadCore.getServicesList();
        if (channelIndex >= 0 && channelIndex < services.size())
        {
            Service service = (Service)services.get(channelIndex);
            Locator locator = service.getLocator();
            Vector publishedItems = m_oadHN.getContentItems(serverIndex);
            
            if (publishedItems == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("getIndexForLocalChannel() - publishedItems is null");
                }
                return -1;
            }
            
            for (int i = 0; i < publishedItems.size(); i++)
            {
                ContentEntry entry = (ContentEntry)publishedItems.get(i);
                if (entry instanceof ChannelContentItem)
                {
                    ChannelContentItem cci = (ChannelContentItem)entry;
                    if (cci.getChannelLocator().equals(locator))
                    {
                        return i;
                    }
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("getIndexForLocalChannel() - Channel index out of bounds: " + channelIndex);
            }
        }
        return -1;
    }
    
    protected boolean publishService(int serviceIndex, long timeoutMS)
    {
        Vector services = m_oadCore.getServicesList();
        Service svc = (Service)services.elementAt(serviceIndex);
        //String channelName = svc.getName();
        String channelName = m_oadCore.getInformativeChannelName(serviceIndex);
        ExtendedFileAccessPermissions efap =
            new ExtendedFileAccessPermissions(true,
                                              true,
                                              false,
                                              false,
                                              false,
                                              true,
                                              null,
                                              null);
        ContentContainer root = m_oadHN.getRootContainer(timeoutMS);
        if (root == null)
        {
            return false;
        }

        // For the new ChannelContentItem, create a ChannelGroupContainer
        ContentContainer cgContainer = null;
        try
        {
            cgContainer = root.createChannelGroupContainer(
                                                       channelName, efap);
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("createChannelGroupContainer threw exception", e);
            }

            return false;
        }

        if (cgContainer == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("createChannelGroupContainer() returned null!");
            }

            return false;
        }

        // Create a ChannelContentItem and add it to the ChannelGroupContainer
        ChannelContentItem cci = null;
        String uriId = null;
        try
        {
            OcapLocator ol = (OcapLocator) svc.getLocator();
            ContentEntryFactory ccf = ContentEntryFactory.getInstance();
            cci = ccf.createChannelContentItem(ContentItem.VIDEO_ITEM_BROADCAST,
                                               "Channel: " + channelName,
                                               channelName,
                                               "Digital,15,2",
                                               ol,
                                               efap);
            if (cci == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("createChannelContentItem() returned null.");
                }

                return false;
            }
            MetadataNode md = cci.getRootMetadataNode();
            String primaryURI = ((String[])md.getMetadata("didl-lite:res"))[0];
            uriId = m_oadHN.getURIId(md);

            if (log.isInfoEnabled())
            {
                log.info("createChannelContentItem() cci: " + cci);
            }
            
            if (log.isInfoEnabled())
            {
                log.info("Publishing primary URI: " + primaryURI);
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("createChannelContentItem() threw an exception", e);
            }

            return false;
        }
        try
        {
            if (!cgContainer.addContentEntry(cci))
            {
                if (log.isInfoEnabled())
                {
                    log.info("addContentEntry() returned false.");
                }

                return false;
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("addContentEntry() threw an exception", e);
            }

            return false;
        }
        String id = cci.getID();
        String name = m_oadCore.getInformativeChannelName(cci.getItemService());
        String title = cci.getChannelTitle(); 
        m_oadHN.addPublishedContent("Channel: " + id + ", " + name + " "  + title + ", URI ID: " +
                uriId); 
        return true;
    }
    
    protected boolean publishAllServices(long timeoutMS)
    {
        m_oadHN.clearPublishedContent();
        Vector services = m_oadCore.getServicesList();

        for (int i = 0; i < services.size(); i++)
        {
            boolean published = publishService(i, timeoutMS);
            if (!published)
            {
                return false;
            }
        }
        return true;
    }
    
    protected boolean publishServiceToChannelContainer(int serviceIndex, long timeoutMS, boolean publishAsVOD)
    {
        Vector services = m_oadCore.getServicesList();
        Service svc = (Service)services.elementAt(serviceIndex);
        //String channelName = svc.getName();
        String channelName = m_oadCore.getInformativeChannelName(serviceIndex);
        ExtendedFileAccessPermissions efap =
            new ExtendedFileAccessPermissions(true,
                                              true,
                                              false,
                                              false,
                                              false,
                                              true,
                                              null,
                                              null);
        ContentContainer channelContainer = m_oadHN.getChannelContentContainer(timeoutMS);
        if (channelContainer == null)
        {
            return false;
        }

        // For the new ChannelContentItem, create a ChannelGroupContainer
        ContentContainer cgContainer = null;
        try
        {
            cgContainer = channelContainer.createChannelGroupContainer(
                                                       channelName, efap);
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("createChannelGroupContainer threw exception", e);
            }

            return false;
        }

        if (cgContainer == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("createChannelGroupContainer() returned null!");
            }

            return false;
        }

        // Create a ChannelContentItem and add it to the ChannelGroupContainer
        ChannelContentItem cci = null;
        String cciType = null;
        String uriId = null;
        try
        {
            if(publishAsVOD)
            {
                cciType = ContentItem.VIDEO_ITEM_BROADCAST_VOD;
                if (log.isInfoEnabled())
                {
                    log.info("createChannelContentItem() creating ContentItem.VIDEO_ITEM_BROADCAST_VOD");
                }
            }
            else
            {
                cciType = ContentItem.VIDEO_ITEM_BROADCAST;
                if (log.isInfoEnabled())
                {
                    log.info("createChannelContentItem() creating ContentItem.VIDEO_ITEM_BROADCAST");
                }
            }
            OcapLocator ol = (OcapLocator) svc.getLocator();
            ContentEntryFactory ccf = ContentEntryFactory.getInstance();
            cci = ccf.createChannelContentItem(cciType,
                                               "Channel: " + channelName,
                                               channelName,
                                               "Digital,15,2",
                                               ol,
                                               efap);
            if (cci == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("createChannelContentItem() returned null.");
                }

                return false;
            }
            MetadataNode md = cci.getRootMetadataNode();
            String primaryURI = ((String[])md.getMetadata("didl-lite:res"))[0];
            uriId = m_oadHN.getURIId(md);

            if (log.isInfoEnabled())
            {
                log.info("Publishing primary URI: " + primaryURI);
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("createChannelContentItem() threw an exception", e);
            }

            return false;
        }
        try
        {
            if (!cgContainer.addContentEntry(cci))
            {
                if (log.isInfoEnabled())
                {
                    log.info("addContentEntry() returned false.");
                }

                return false;
            }
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info("addContentEntry() threw an exception", e);
            }

            return false;
        }
        String id = cci.getID();
        String name = m_oadCore.getInformativeChannelName(cci.getItemService());
        String title = cci.getChannelTitle(); 
        m_oadHN.addPublishedContent("Channel: " + id + ", " + name + " "  + title + ", URI ID: " +
                uriId); 
        return true;
    }
    
    protected boolean publishAllServicesToChannelContainer(long timeoutMS)
    {
        m_oadHN.clearPublishedContent();
        Vector services = m_oadCore.getServicesList();

        for (int i = 0; i < services.size(); i++)
        {
            boolean published = publishServiceToChannelContainer(i, timeoutMS, false);
            if (!published)
            {
                return false;
            }
        }
        return true;
    }

    protected boolean unPublishChannels(long timeoutSecs)
    {
        int serverIndex = -1;
        if (m_oadHN.waitForLocalContentServerNetModule(timeoutSecs))
        {
            serverIndex = m_oadHN.findLocalMediaServer();
        }
        
        if (serverIndex == -1)
        {
            if (log.isInfoEnabled())
            {
                log.info("unpublishChannels() - Unable to find local media server");
            }
            return false;
        }
        Vector contentItems = m_oadHN.getContentItems(serverIndex);
        if (contentItems != null)
        {
            for (int i = 0; i < contentItems.size(); i++)
            {
                ContentItem item = (ContentItem)contentItems.get(i);
                if (item instanceof ChannelContentItem)
                {
                    try
                    {
                        ContentContainer ci = ((ChannelContentItem)item).getEntryParent();
                        ci.delete();
                    }
                    catch (IOException e)
                    {
                        if (log.isErrorEnabled())
                        {
                            log.error("unPublishChannel() - error deleting container, exception: ", e);
                        }
                        return false;
                    }
                }
            }
        }
        else
        {  
            if (log.isInfoEnabled())
            {
                log.info("unPublishChannels: content list NULL!");
            }
        }
        
        return true;
    }    
}

