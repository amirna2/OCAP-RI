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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.cablelabs.xlet.hn.HNIAPP.util.HNConstants;
import org.cablelabs.xlet.hn.HNIAPP.util.logging.HomeNetLogging;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.ContentResource;
import org.ocap.hn.content.VideoResource;
import org.ocap.hn.content.navigation.ContentList;
import java.util.Properties;
/**
 * @author Parthiban Balasubramanian, Cognizant Technology Solutions
 * 
 */

public class HNUtil
{
    private static HomeNetLogging hnLogger = HomeNetLogging.getInstance();

    private static HNUtil utilInstance = null;
    
    private static final long DEFAULT_SLEEP_TIME = 10000;
    
    private static long deviceSleepTime;
    
    private static long contentSleepTime;

    /**
     * This method returns a singleton instance of HNUtil.
     * 
     * @return instance of HNUtil
     */
    public static HNUtil getInstance()
    {
    	hnLogger.homeNetLogger("Entering getInstance():HNUtil");
    	boolean propertiesDefined = false;
    	try 
    	{   
    		Properties props = new Properties();
    		String envDir = ".." + File.separator + ".." + File.separator;
    		props.load(new FileInputStream(envDir + HNConstants.PROPERTIES_FILE_NAME));
			if (props.containsKey(HNConstants.CONTENT_DISCOVERY_TIME_PROPERTY))
			{
				try
				{
					contentSleepTime = Long.parseLong(props.getProperty
							(HNConstants.CONTENT_DISCOVERY_TIME_PROPERTY));
					hnLogger.homeNetLogger("Setting content sleep time to " + contentSleepTime);
				}
				catch (NumberFormatException e)
				{
					hnLogger.homeNetLogger("Content sleep time is not valid.");
					contentSleepTime = DEFAULT_SLEEP_TIME;
				}
			}
			else
			{
				contentSleepTime = DEFAULT_SLEEP_TIME;
			}
			if (props.containsKey(HNConstants.DEVICE_DISCOVERY_TIME_PROPERTY))
			{
				try
				{
					deviceSleepTime = Long.parseLong(props.getProperty
							(HNConstants.DEVICE_DISCOVERY_TIME_PROPERTY));
					hnLogger.homeNetLogger("Setting device sleep time to " + deviceSleepTime);
				}
				catch (NumberFormatException e)
				{
					hnLogger.homeNetLogger("Discovery sleep time is not valid.");
					deviceSleepTime = DEFAULT_SLEEP_TIME;
				}
			}
			else
			{
				deviceSleepTime = DEFAULT_SLEEP_TIME;
			}
		} 
    	catch (FileNotFoundException e) 
    	{
    		hnLogger.homeNetLogger("Properties file not found.");
			deviceSleepTime = DEFAULT_SLEEP_TIME;
			contentSleepTime = DEFAULT_SLEEP_TIME;
		} 
    	catch (IOException e) 
    	{
    		hnLogger.homeNetLogger("IOException occurred.");
    		deviceSleepTime = DEFAULT_SLEEP_TIME;
			contentSleepTime = DEFAULT_SLEEP_TIME;
		}
        if (utilInstance == null)
        {
            utilInstance = new HNUtil();
        }
        else
        {
            return utilInstance;
        }
        hnLogger.homeNetLogger("Exiting getInstance():HNUtil");
        return utilInstance;
    }

    /**
     * This method initializes the Media Server Manager and retrieves the list
     * of MediaServers present in the network.
     * 
     * @return - ArrayList of Media Servers
     */
    public ArrayList getMediaServers()
    {
        hnLogger.homeNetLogger("Entering getMediaServers():HNUtil");
        ArrayList mediaServers = new ArrayList();
        MediaServerManager mgr = MediaServerManager.getInstance();
        mgr.initialize();
        try
        {
            Thread.sleep(deviceSleepTime);
        }
        catch (InterruptedException ie)
        {
            hnLogger.homeNetLogger(ie);
        }
        Enumeration e = mgr.getMediaServers();
        MediaServer server = null;
        while (e.hasMoreElements())
        {
            server = (MediaServer) e.nextElement();
            hnLogger.homeNetLogger("server name" + server.getServerName());
            mediaServers.add(server);
        }
        hnLogger.homeNetLogger("Exiting getMediaServers():HNUtil");
        return mediaServers;
    }

    public ArrayList getAllDevices()
    {
        hnLogger.homeNetLogger("Entering getAllDevices():HNUtil");
        ArrayList allDevices = new ArrayList();
        MediaServerManager mgr = MediaServerManager.getInstance();
        mgr.initialize();
        try
        {
            Thread.sleep(deviceSleepTime);
        }
        catch (InterruptedException ie)
        {
            hnLogger.homeNetLogger(ie);
        }
        Enumeration e = mgr.getAllDevicesHash().keys();
        while (e.hasMoreElements())
        {
            allDevices.add((String)e.nextElement());
        }
        hnLogger.homeNetLogger("Exiting getAllDevices():HNUtil");
        return allDevices;
    }
    /**
     * This method retrieves the Albums present in the Media server.
     * 
     * @param mediaServer
     *            Media server to be browsed
     * @return List of Albums
     */
    public ArrayList retrieveAlbums(MediaServer mediaServer, int startIndex ,int albumsCount)
    {
        hnLogger.homeNetLogger("Entering retrieveAlbums():HNUtil");
        hnLogger.homeNetLogger("Media server to be accessed :" + mediaServer.getServerName());
        mediaServer.browseData("0", "0", true, startIndex, albumsCount, "");
        // wait to retrieve the content list
        try
        {
            Thread.sleep(contentSleepTime);
        }
        catch (InterruptedException e)
        {
            hnLogger.homeNetLogger(e);
        }
        ContentList list = mediaServer.getContentList();
        ArrayList albumList = retrieveContentList(list);
        hnLogger.homeNetLogger("Exiting retrieveAlbums():HNUtil");
        return albumList;
    }

    /**
     * This method checks for albums(Container) in the ContentList and adds it
     * to an ArrayList
     * 
     * @param list
     *            ContentList
     * @return List of ContentContainer
     */
    private ArrayList retrieveContentList(ContentList list)
    {
        hnLogger.homeNetLogger("Entering retrieveContentList():HNUtil");
        ArrayList containerList = new ArrayList();
        while (list != null && list.hasMoreElements())
        {
            ContentEntry entry = (ContentEntry) list.nextElement();
            if (entry instanceof ContentContainer)
            {
                ContentContainer container = (ContentContainer) entry;
                hnLogger.homeNetLogger("Container Title: " + container.getName());
                hnLogger.homeNetLogger("Container Child Count: " + container.getComponentCount());
                containerList.add(container);
            }
           
        }
        hnLogger.homeNetLogger("Exiting retrieveContentList():HNUtil");
        return containerList;
    }

    /**
     * This method retrieves the List of images present inside an album.
     * 
     * @param mediaServer
     *            Media server object
     * @param contentContainer
     *            Container Object
     * @param start
     *            Start index
     * @return List of images
     */
    public ArrayList getImagesFromAlbum(MediaServer mediaServer, ContentContainer contentContainer, int start, int count)
    {
        hnLogger.homeNetLogger("Entering getImagesFromAlbum():HNUtil");
        hnLogger.homeNetLogger("input details::" + start + "," + contentContainer.getName());
        mediaServer.browseData(contentContainer.getID(), "0", true, start, count, "");
        try
        {
            Thread.sleep(contentSleepTime);
        }
        catch (InterruptedException e)
        {
            hnLogger.homeNetLogger(e);
        }
        ContentList list = mediaServer.getContentList();
        ArrayList m_contentList = new ArrayList();
        while (list != null && list.hasMoreElements())
        {
            ContentEntry entry = (ContentEntry) list.nextElement();
            hnLogger.homeNetLogger("The contentEntry is " + entry);
            if (entry instanceof ContentItem)
            {
                ContentItem item = (ContentItem) entry;
                hnLogger.homeNetLogger("Content ID: " + item.getID());
                hnLogger.homeNetLogger("Content Title: " + item.getTitle());
                if (!m_contentList.contains(entry))
                {
                    m_contentList.add(entry);
                }
            }
        }
        hnLogger.homeNetLogger("Exiting getImagesFromAlbum():HNUtil");
        return m_contentList;
    }

    public Map getAllContentEntriesFromAlbum(MediaServer mediaServer, ContentContainer contentContainer, int start,
            int count)
    {
    	if(contentContainer == null)
    	{
    		hnLogger.homeNetLogger("input details::" + start + "," + "0");
       	 	return getAllContentEntriesFromAlbum(mediaServer, "0", start, count);
    	}
    	else
    	{
    		hnLogger.homeNetLogger("input details::" + start + "," + contentContainer.getName());
       	 	return getAllContentEntriesFromAlbum(mediaServer, contentContainer.getID(), start, count);
    	}
        
    }
    
    public Map getAllContentEntriesFromAlbum(MediaServer mediaServer, String containerID, int start, int count)
    {

    	Map m_allContents = new HashMap();
        hnLogger.homeNetLogger("Entering getAllContentEntriesFromAlbum():HNUtil");
       
        mediaServer.browseData(containerID, "0", true, start, count, "");
        try
        {
            Thread.sleep(contentSleepTime);
        }
        catch (InterruptedException e)
        {
            hnLogger.homeNetLogger(e);
        }
        ContentList list = mediaServer.getContentList();
        ArrayList m_contentList = new ArrayList();
        ArrayList m_contentContainer = new ArrayList();
        ArrayList m_contentItemNR = new ArrayList();
        ArrayList m_contentListVideoItem = new ArrayList();
        ArrayList m_contentChannelItem = new ArrayList();
        while (list != null && list.hasMoreElements())
        {
            ContentEntry entry = (ContentEntry) list.nextElement();
            hnLogger.homeNetLogger("The contentEntry is " + entry);
            if (entry instanceof ContentContainer)
            {
                ContentContainer container = (ContentContainer) entry;
                hnLogger.homeNetLogger("Container Title: " + container.getName());
                hnLogger.homeNetLogger("Container Child Count: " + container.getComponentCount());
                m_contentContainer.add(container);
            } 
            else if (entry instanceof ContentItem && isImage((ContentItem) entry))
            {
                ContentItem item = (ContentItem) entry;
                hnLogger.homeNetLogger("Content ID: " + item.getID());
                hnLogger.homeNetLogger("Content Title: " + item.getTitle());
                if (!m_contentList.contains(entry))
                {
                    m_contentList.add(entry);
                }
            }
            else if(entry instanceof ChannelContentItem && isVideo((ContentItem) entry))
            {
            	 ChannelContentItem item = (ChannelContentItem) entry;
                 hnLogger.homeNetLogger("Channel No: " + item.getChannelNumber());
                 hnLogger.homeNetLogger("Channel Title: " + item.getChannelTitle());
                 if (!m_contentChannelItem.contains(entry))
                 {
                	 m_contentChannelItem.add(entry);
                 }
            }
            else if (entry instanceof ContentItem && isVideo((ContentItem) entry))
            {
                ContentItem item = (ContentItem) entry;
                hnLogger.homeNetLogger("Content ID: " + item.getID());
                hnLogger.homeNetLogger("Content Title: " + item.getTitle());
                if (!m_contentListVideoItem.contains(entry))
                {
                	m_contentListVideoItem.add(entry);
                }
            }
            // to capture the items that are not recognized
            else
            {
            	ContentItem item = (ContentItem) entry;
                hnLogger.homeNetLogger("Content ID: " + item.getID());
                hnLogger.homeNetLogger("Content Title: " + item.getTitle());
                // This to avoid the Recording Title empty item from getting populated in the list.
                // (Only applicable for OCAP RI recorded items)
                if(!(item.getTitle()!=null && item.getTitle().trim().length()>0 && item.getTitle().equalsIgnoreCase("Recording Title")))
                {
                	m_contentItemNR.add(item.getTitle());
                }
                
            }
        }
        hnLogger.homeNetLogger("Exiting getAllContentEntriesFromAlbum():HNUtil");
        m_allContents.put("ContentList", m_contentList);
        m_allContents.put("ContentContainer", m_contentContainer);
        m_allContents.put("NRContenItems",m_contentItemNR);
        m_allContents.put("ContentListVideo",m_contentListVideoItem);
        m_allContents.put("StreamedChannels", m_contentChannelItem);
        return m_allContents;
    
    }
    /**
     * This method returns the Image URL of the Content Item.
     * 
     * @param content
     *            Content Item
     * @return Image URL
     */
    public String getImageUrl(ContentItem content)
    {
        hnLogger.homeNetLogger("Entering getImageUrl():HNUtil");
        ContentResource maxCR = null;
        hnLogger.homeNetLogger("resource count:" + content.getResourceCount());
        if (content.getResourceCount() >0)
        {
            ContentResource res[] = content.getResources();
            for (int i = 0; i < res.length; i++)
            {
                if (res[i] instanceof VideoResource)
                {
                	maxCR = res[i];
                	
                }
            }
        }
        String	resource;
        if (maxCR.getLocator() == null)
        {
        	resource = "";
        }
        else
        {
        	resource = maxCR.getLocator().toExternalForm();
        }
        
        hnLogger.homeNetLogger("THe resource url is :" + resource);
        hnLogger.homeNetLogger("Exiting getImageUrl():HNUtil");
        return resource;
    }

    public String getVideoURL(ContentItem content)
    {
        hnLogger.homeNetLogger("Entering getVideoUrl():HNUtil");
        ContentResource maxCR = null;
        hnLogger.homeNetLogger("resource count:" + content.getResourceCount());
    	if (content.getResourceCount() > 0)
        {
            ContentResource res[] = content.getResources();
            for (int i = 0; i < res.length; i++)
            {
                hnLogger.homeNetLogger("file format:" + res[i].getContentFormat());
                if ("video/mpeg".equals(res[i].getContentFormat()))
                {
                    hnLogger.homeNetLogger("it is  a video");
                    maxCR =res[i];
                }
            }
        }
    	String	resource = maxCR.getLocator().toExternalForm();
        
        hnLogger.homeNetLogger("The video resource url is :" + resource);
        hnLogger.homeNetLogger("Exiting getVideoUrl():HNUtil");
        return resource;
    }

    /**
     * This method checks the format of the ContentItem.
     * 
     * @param content
     *            ContentItem
     * @return true if it is a video
     */
    public boolean isVideo(ContentItem content)
    {
        hnLogger.homeNetLogger("Entering isVideo():HNUtil");
        hnLogger.homeNetLogger("resource count:" + content.getResourceCount());
        if (content.getResourceCount() > 0)
        {
            ContentResource res[] = content.getResources();
            for (int i = 0; i < res.length; i++)
            {
                hnLogger.homeNetLogger("file format:" + res[i].getContentFormat());
                if ("video/mpeg".equals(res[i].getContentFormat()))
                {
                    hnLogger.homeNetLogger("it is  a video");
                    return true;
                }
            }
        }
        hnLogger.homeNetLogger("Exiting isVideo():HNUtil");
        return false;
    }
    
    public boolean isImage(ContentItem content)
    {
        hnLogger.homeNetLogger("Entering isImage():HNUtil");
        hnLogger.homeNetLogger("resource count:" + content.getResourceCount());
        if (content.getResourceCount() > 0)
        {
            ContentResource res[] = content.getResources();
            for (int i = 0; i < res.length; i++)
            {
                hnLogger.homeNetLogger("file format:" + res[i].getContentFormat());
                if (res[i].getContentFormat().indexOf("image/")!=-1)
                {
                    hnLogger.homeNetLogger("it is an image");
                    return true;
                }
            }
        }
        hnLogger.homeNetLogger("Exiting isImage():HNUtil");
        return false;
    }

}
