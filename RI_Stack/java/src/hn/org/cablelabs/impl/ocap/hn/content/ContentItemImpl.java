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

package org.cablelabs.impl.ocap.hn.content;

import org.apache.log4j.Logger;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.UsageTrackingContentItem;
import org.cablelabs.impl.ocap.hn.transformation.Transformable;
import org.cablelabs.impl.ocap.hn.transformation.TransformationManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.service.RemoteServiceImpl;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.ContentResource;
import org.ocap.hn.transformation.Transformation;
import org.ocap.hn.transformation.TransformationManager;
import org.ocap.hn.upnp.common.UPnPAction;

import javax.tv.service.Service;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * DOCUMENT ME!
 *
 * @author $author$
 * @version $Revision$
 */
public class ContentItemImpl extends ContentEntryImpl 
    implements ContentItemExt, UsageTrackingContentItem
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(ContentItemImpl.class);

    /** File to actual contents of the item */
    private File m_content = null;

    protected boolean m_deleted = false;
    
    /** Sync object */
    private Object m_sync = new Object();

    /**
     * Keeps track of the number of occurrences where this item is being
     * streamed
     **/
    private int m_inUseCnt = 0;

    private Service itemService = null;
    
    private ContentResourceExt m_resourceList[] = null;
    
    public ContentItemImpl(MetadataNodeImpl metadataNode)
    {
        super(metadataNode);
    }

    /**
     * Creates a new ContentItemImpl object.
     *
     * @param element
     *            The element containing the properties for this ContentItem.
     */
    public ContentItemImpl(MetadataNodeImpl metadataNode, File content)
    {
        super(metadataNode);
        m_content = content;
    }

    // Constructor for client Browse/Search
    public ContentItemImpl(UPnPAction action, MetadataNodeImpl metadataNode)
    {
        super(action, metadataNode);
    }

    /**
     * Determines if this ContentItem contains a resource specified by the
     * ContentResource
     *
     * @param entry
     *            a Reference to the ContentResource contained within this
     *            ContentItem.
     *
     * @return boolean value of True if this ContentItem contains the specified
     *         ContentResource.
     */
    public boolean containsResource(ContentResource entry)
    {
        if (entry == null)
        {
            return false;
        }
        
        final ContentResource contentResource[] = getContentResourceList();
        for (int i=0; i< contentResource.length; i++)
        {
            if (entry.equals(contentResource[i]))
            {
                return true;
            }
        }

        return false;
    }
    
    /**
     * Deletes this item from the local repository.  Removes binary.
     * 
     * @return boolean state True if the ContentItem was successfully deleted. False
     *         indicates that the ContentItem could not be removed.
     * 
     * @throws IOException
     *             Thrown in the item is not local
     * @throws SecurityException
     *             Thrown if the Entry is protected and the requester doesn't
     *             have authorization.
     */
    public boolean deleteEntry() throws IOException, SecurityException
    {
        if (!isLocal())
        {
            throw new IOException();
        }
        
        boolean state = false;
        
        // Remove file using CDS reference before deleting entry.
        state = MediaServer.getInstance().getCDS().removeFile(this);
        
        // Remove CDS entry.
        if(state)
        {
            state = super.deleteEntry();
            m_deleted = true;
        }
        
        return state;
    }
    
    /**
     * Method to remove the resources associated with the ContentItem.
     * 
     * @return true if the resources are not present or were present and successfully removed, 
     *         false if the resources are present but could not be successfully removed.
     */
    public boolean deleteResources()
    {
        m_deleted = true; // Treat either the deletion of the CI or its resources as "deleted"
        if(m_content != null 
                && m_content.exists()) 
        {
           return m_content.delete();
        }
        
        return true;
    }

    /**
     * Returns the UPnP Class name.
     *
     * @return String representing the UPnP class name.
     */
    public String getContentClass()
    {
        return getMetadataRegardless(UPnPConstants.QN_UPNP_CLASS);
    }

    /**
     * Returns the Service associated with this ContentItem
     *
     * @return Service.
     */
    public Service getItemService()
    {
        // Create remote service if has Video
        if (hasVideo() && (itemService == null))
        {
            itemService = new RemoteServiceImpl(this, m_sync);
            if (log.isInfoEnabled()) 
            {
                log.info("getItemService itemService: " + itemService);
            }
        }
        return itemService;
    }

    /**
     * Returns the file associated with this content item
     *
     * @return File associated with this content item
     */
    public File getContentFile()
    {
        return m_content;
    }

    /**
     * Returns a list of renderable resources associated with this ContentItem.
     * If there are no renderable resources associated with this ContentItem the
     * ContentResource will be null.
     *
     * @return ContentResource[]
     */
    public ContentResource[] getRenderableResources()
    {
        final ContentResource contentResource[] = getContentResourceList();
        List renderable = new ArrayList(contentResource.length);
        
        for (int i=0; i< contentResource.length; i++)
        {
            if (contentResource[i].isRenderable())
            {
                renderable.add(contentResource[i]);
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("getRenderableResources - renderable: " + renderable);
        }
        return (ContentResource[]) renderable.toArray(new ContentResource[renderable.size()]);
    }

    /**
     * Returns the ContentResource associated with this ContentItem
     *
     * @param n
     *            The index of the ContentResource within the ContentResource
     *            List
     *
     * @return ContentResource
     *
     * @throws ArrayIndexOutOfBoundsException
     *             thrown if index 'n' is out of range.
     */
    public ContentResource getResource(int n) throws ArrayIndexOutOfBoundsException
    {
        final ContentResource contentResource[] = getContentResourceList();
        return contentResource[n];
    }

    /**
     * Returns the number of Resources this ContentItem contains.
     *
     * @return int represents the number of ContentResources this ContentItem
     *         contains.
     */
    public int getResourceCount()
    {
        final ContentResource contentResource[] = getContentResourceList();
        return contentResource.length;
    }

    /**
     * Returns the Index location where the Specified ContentRessource is
     * located.
     *
     * @param r
     *            A reference to the ContentResource.
     *
     * @return int value that identifies the location of with the Resource list.
     */
    public int getResourceIndex(ContentResource r)
    {
        if (r == null)
        {
            return -1;
        }
        
        final ContentResource contentResource[] = getContentResourceList();
        
        for (int i=0; i< contentResource.length; i++)
        {
            if (r.equals(contentResource[i]))
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the Array of Resources associated with this ContentItem.
     *
     * @return ContentResource[] An array of ContentResource objects.
     */
    public ContentResource[] getResources()
    {
        return getContentResourceList();
    }

    // A more efficient implementation of getProtocolInfo using the 
    // Override
    public HNStreamProtocolInfo[] getProtocolInfo()
    {
        HNStreamProtocolInfo protocolInfo[] = null;
        final ContentResourceExt contentResource[] = getContentResourceList();

        protocolInfo = new HNStreamProtocolInfo[contentResource.length];
        for (int i = 0; i < protocolInfo.length; i++)
        {
            protocolInfo[i] = contentResource[i].getProtocolInfo();
        }
        
        return protocolInfo;
    }

    /**
     * Returns the title of the ContentItem.
     *
     * @return String representing the ContentItems Title.
     */
    public String getTitle()
    {
        if (m_metadataNode != null && m_metadataNode.getMetadata(UPnPConstants.TITLE) instanceof String)
        {
            return (String) m_metadataNode.getMetadata(UPnPConstants.TITLE);
        }
        return null;
    }

    /**
     * Determines if this ContentItem has Audio
     *
     * @return boolean value of True if the ContentItem has Audio content. False
     *         indicates the Content is not Audio.
     */
    public boolean hasAudio()
    {
        return (getContentClass().equalsIgnoreCase(ContentItem.AUDIO_ITEM)
                || getContentClass().equalsIgnoreCase(ContentItem.AUDIO_ITEM_BOOK)
                || getContentClass().equalsIgnoreCase(ContentItem.AUDIO_ITEM_BROADCAST) 
                || getContentClass().equalsIgnoreCase(ContentItem.AUDIO_ITEM_TRACK));
    }

    /**
     * Determines if this ContentItem has a Still Image
     *
     * @return boolean value of True if the ContentItem has Image content. False
     *         indicates the Content is not an Image.
     */
    public boolean hasStillImage()
    {
        return (getContentClass().equalsIgnoreCase(ContentItem.IMAGE_ITEM) || getContentClass().equalsIgnoreCase(
                ContentItem.IMAGE_ITEM_PHOTO));
    }

    /**
     * Determines if this ContentItem has Video
     *
     * @return boolean value of True if the ContentItem has Video content. False
     *         indicates the Content is not Video
     */
    public boolean hasVideo()
    {
        return (getContentClass().equalsIgnoreCase(ContentItem.VIDEO_ITEM)
                || getContentClass().equalsIgnoreCase(ContentItem.VIDEO_ITEM_BROADCAST)
                || getContentClass().equalsIgnoreCase(ContentItem.VIDEO_ITEM_BROADCAST_VOD)
                || getContentClass().equalsIgnoreCase(ContentItem.VIDEO_ITEM_MOVIE) 
                || getContentClass().equalsIgnoreCase(ContentItem.VIDEO_ITEM_MUSIC_CLIP) 
                || getContentClass().equalsIgnoreCase(ContentItem.VIDEO_ITEM_VPOP) );
    }

    /**
     * Determines if the Item or Resource can be rendered.
     *
     * @return boolean value of True indicates that the content can be rendered.
     *         False indicates that the content can't be rendered.
     */
    public boolean isRenderable()
    {
        ContentResource[] renderableResources = getRenderableResources();
        return renderableResources != null && renderableResources.length > 0;
    }

    /**
     * This function will return the array of ContentResources contained in this ContentItem. 
     * Subclasses that maintain their own resource list should override this function 
     * (basically any (local) ContentItem that supports ContentResource.delete()).
     */
    /*
     * This is the implementation for non-local ContentItems.
     */
    protected ContentResourceExt[] getContentResourceList()
    {
        if (m_resourceList == null)
        {
            m_resourceList = buildContentResourceList();
        }
        return m_resourceList;
    }

    /**
     * This function will create an array of ContentResource objects based exclusively on 
     * the ContentItem metadata.
     */
    private ContentResourceExt[] buildContentResourceList()
    {
        if (log.isDebugEnabled()) 
        {
            log.debug("buildContentResourceList: " + this);
        }
        final String[] resProtocolInfoArray = (String[]) m_metadataNode.getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_PROTOCOL_INFO);

        if (resProtocolInfoArray == null)
        {
            if (log.isDebugEnabled()) 
            {
                log.debug("buildContentResourceList: No ProtocolInfo for " + this);
            }
            return new ContentResourceExt[0];
        }

        final String[] resArray = (String[]) m_metadataNode.getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES);

        if (resArray == null)
        {
            if (log.isDebugEnabled()) 
            {
                log.debug("buildContentResourceList: No res elements for " + this);
            }
            return new ContentResourceExt[0];
        }
        if (resArray.length != resProtocolInfoArray.length)
        {
            if (log.isDebugEnabled()) 
            {
                log.debug("buildContentResourceList: Number of res elements and number of protocolInfo elements don't match (" 
                          + resArray.length + "!=" + resProtocolInfoArray.length + ") for " + this);
            }
            return new ContentResourceExt[0];
        }

        if (log.isDebugEnabled()) 
        {
            log.debug("buildContentResourceList: Found " + resProtocolInfoArray.length + " res elements for " + this);
        }
        
        ContentResourceExt[] contentResource = new ContentResourceExt[resArray.length]; 

        for (int resIndex = 0; resIndex < resArray.length; ++ resIndex)
        {
            String resProtocolInfo = resProtocolInfoArray[resIndex];
            if (log.isDebugEnabled())
            {
                log.debug("buildContentResourceList: protocolInfo[" + resIndex + "]: " + resProtocolInfo);
            }
            
            HNStreamProtocolInfo protocolInfo = new HNStreamProtocolInfo(resProtocolInfo);

            String mimeType = protocolInfo.getContentFormat();
            
            if (mimeType.indexOf("image/") != -1)
            {
                if (hasAudio())
                {
                    contentResource[resIndex] = new AudioVideoResource(this, resIndex);
                }
                else
                {
                    contentResource[resIndex] = new VideoResourceImpl(this, resIndex);
                }
            }
            else if (mimeType.indexOf("video/") != -1)
            {
                contentResource[resIndex] = new StreamableAudioVideoResource(this, resIndex);
            }
            else if (mimeType.indexOf("audio/") != -1)
            {
                if (hasVideo() || hasStillImage())
                {
                    contentResource[resIndex] = new StreamableAudioVideoResource(this, resIndex);
                }
                else
                {
                    contentResource[resIndex] = new StreamableAudioResource(this, resIndex);
                }
            }
            else
            {
                if (hasAudio())
                {
                    if (hasVideo() || hasStillImage())
                    {
                        contentResource[resIndex] = new AudioVideoResource(this, resIndex);
                    }
                    else
                    {
                        contentResource[resIndex] = new AudioResourceImpl(this, resIndex);
                    }
                }
                else if (hasVideo() || hasStillImage())
                {
                    contentResource[resIndex] = new VideoResourceImpl(this, resIndex);
                }
                else
                {
                    contentResource[resIndex] = new ContentResourceImpl(this, resIndex);
                }
            }
                
            if (log.isDebugEnabled()) 
            {
                log.debug("buildContentResourceList: adding " + contentResource[resIndex]);
            }
        } // END for (resArray elements)
       
        return contentResource;
    }

    /**
     * Returns an indication of whether any asset within this object is in use
     * on the home network. "In Use" is indicated if there is an active network
     * transport protocol session (for example HTTP, RTSP) to the asset.
     * <p>
     * For objects which logically contain other objects, recursively iterates
     * through all logical children of this object. For ContentContainer
     * objects, recurses through all ContentEntry objects they contain. For
     * NetRecordingEntry objects, iterates through all RecordingContentItem
     * objects they contain.
     *
     * @return True if there is an active network transport protocol session to
     *         any asset that this ContentResource, ContentEntry, or any
     *         children of the ContentEntry contain, otherwise false.
     */
    public boolean isInUse()
    {
        boolean inUse = false;
        if (log.isDebugEnabled())
        {
            log.debug("ContentItemImpl.isInUse() - current cnt: " + m_inUseCnt);
        }

        // In use is only applicable to local items
        if (isLocal())
        {
            // Make sure this is the real CDS entry, not a proxy resulting from
            // a CDS action
            ContentItemImpl cii = (ContentItemImpl) getCDSContentEntry();
            if (cii == null)
            {
                cii = this;
            }

            if (cii.m_inUseCnt > 0)
            {
                inUse = true;
            }
        }
        return inUse;
    }

    /**
     * Increments the number of occurrences where this item is being streamed.
     */
    public void incrementInUseCount()
    {
        if (log.isDebugEnabled())
        {
            log.debug("ContentItemImpl.isInUse() - " + "incrementing in use, current cnt: " + m_inUseCnt);
        }
        m_inUseCnt++;
    }

    /**
     * Decrements the number of occurrences where this item is being streamed.
     */
    public void decrementInUseCount()
    {
        if (log.isDebugEnabled())
        {
            log.debug("ContentItemImpl.isInUse() - " + "decrementing in use, current cnt: " + m_inUseCnt);
        }
        m_inUseCnt--;
        if (m_inUseCnt < 0)
        {
            if (log.isErrorEnabled())
            {
                log.error("RecordingContentItemLocal.decrementInUseCnt() - "
                + "Problem with logic, current in use cnt is negative = " + m_inUseCnt);
            }
    }
    }
    
    /**
     * Returns true if this ContentItem has been successfully deleted
     */
    public boolean hasBeenDeleted()
    {
        return m_deleted;
    }
}
