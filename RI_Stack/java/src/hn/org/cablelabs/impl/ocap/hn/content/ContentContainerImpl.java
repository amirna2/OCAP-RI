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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.NetManagerImpl;
import org.cablelabs.impl.ocap.hn.content.navigation.ContentListImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.cds.DIDLLite;
import org.cablelabs.impl.util.Containable;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.HomeNetPermission;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.content.ChannelContentItem;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.navigation.ContentDatabaseFilter;
import org.ocap.hn.content.navigation.ContentList;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * Implementation of the ContentContainer interface
 */
public class ContentContainerImpl extends ContentEntryImpl implements ContentContainer
{
    /** Log4J logging facility. */
    private static final Logger log = Logger.getLogger(ContentContainerImpl.class);

    private Vector m_children = null;
    
    private long totalDeletedChildCount = 0;

    /** permissions */
    private static final HomeNetPermission CONTENT_MANAGEMENT_PERMISSION = new HomeNetPermission("contentmanagement");
    
    /**
     * Creates a new ContentContainerImpl object.
     * 
     * @param metadataNode
     *            The element containing the properties for this
     *            ContentContainer.
     */
    public ContentContainerImpl(MetadataNodeImpl metadataNode)
    {
        super(metadataNode);
    }

    /**
     * Creates a new ContentContainerImpl object.
     * 
     * @param action
     *            The IAction that created this ContentContainer
     * @param metadataNode
     *            The element containing the properties for this
     *            ContentContainer.
     */
    public ContentContainerImpl(UPnPAction action, MetadataNodeImpl metadataNode)
    {
        super(action, metadataNode);
        resetChildCount();
    }

    /**
     * Adds an entry to this ContentContainer
     * 
     * @param entries
     *            The entries to be added to this ContentContainer
     * 
     * @return boolean value of <true> if all the entries are local, and are not
     *         previously part of the ContentContainer
     * 
     * @throws SecurityException
     *             Thrown if caller is not authorized
     */
    public boolean addContentEntries(ContentEntry[] entries) throws SecurityException
    {
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);

        if (!isLocal() || contains(entries))
        {
            return false;    
        }
        // Check requirements for Channel Group Containers
        if(isChannelGroupContainer())
        {
            for(int i = 0; i < entries.length; i++)
            {
                if(!(entries[i] instanceof ChannelContentItem))
                {
                    return false;
                }
            }
        }
        
        for (int i = 0; i < entries.length; ++i)
        {
            if (!addContentEntry(entries[i]))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Implementation of ContentContainer.addContentEntry()
     * TODO : Publish question: should this entry be cloned? 
     * Otherwise direct manipulation of CDS entries is allowed.
     * Direct manipulation is assumed in NetRecordingContentItems.
     */
    public boolean addContentEntry(ContentEntry entry) throws SecurityException
    {
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);
        
        if (entry == null || !isLocal() || !entry.isLocal())
        {
            return false;
        }

        if (!hasWritePermission())
        {
            throw new SecurityException(NO_WRITE_PERMISSONS);
        }

        String parentID = getParentID();

        if (parentID == null || parentID.length() == 0)
        {
            throw new IllegalStateException();
        }

        boolean state = false;

        // Check more specific classes from hierarchy
        // Following logic as written in spec.
        if (isChannelGroupContainer() && !(entry instanceof ChannelContentItem))
        {
            state = false;
        }
        else if (entry instanceof Containable)
        {
            state = ((Containable) entry).enter(this);
        }
        else if (entry instanceof ContentEntryImpl)
        {
            state = addEntry((ContentEntryImpl) entry);
        }
        else
        {
            state = false;
        }
        
        return state;
    }

    /**
     * Determines if this ContentContainer contains a specified ContentEntry
     * 
     * @param entry
     *            The entry to check if already exists in this ContentContainer.
     * 
     * @return boolean value of <true> if the entry exists. <false> indicates
     *         that entry is not associated with the ContentContainer.
     */
    public boolean contains(ContentEntry entry)
    {
        boolean state = false;
        Enumeration enumEntry = null;

        if (entry != null)
        {
            enumEntry = getEntries();

            while ((enumEntry != null) && enumEntry.hasMoreElements())
            {
                if (((ContentEntry) enumEntry.nextElement()).getID().equalsIgnoreCase(entry.getID()))
                {
                    state = true;

                    break;
                }
            }
        }

        return state;
    }

    /**
     * Creates a Container in the local CDS assuming that this Container is
     * local.
     * 
     * @param name
     *            The name or Title of the Container
     * @param permissions
     *            provides the type of permissions governing the container.
     * 
     * @return boolean value True if the creation was successful. False
     *         indicates the creation was unsuccessful.
     * 
     * @throws SecurityException
     *             Thrown if the calling request is not authorized.
     */
    public boolean createContentContainer(String name, ExtendedFileAccessPermissions permissions)
            throws SecurityException
    {
        boolean state = false;

        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);
        if (!hasWritePermission())
        {
            throw new SecurityException(NO_WRITE_PERMISSONS);
        }

        if (isLocal())
        {
            // Creates container in the CDS.
            ContentContainerImpl newCC = MediaServer.getInstance().getCDS().createContainer(this, name, permissions, 
                    ContentDirectoryService.CONTAINER_TYPE);
            
            // Add copy locally
            addChild(newCC);
            newCC.setEntryParent(this);
            state = true;
        }

        return state;
    }
       
    /**
     * Creates a new channel group <code>ContentContainer</code> as a child of this 
     * ContentContainer, when the host device is capable of supporting
     * tuner requests from the home network. This channel group only
     * contains <code>ChannelContentItem</code> instances representing 
     * broadcast channels that can be tuned by the host device.
     * 
     * If this ContentContainer #isLocal method 
     * returns false, this method will return null.
     * 
     * If the ContentServerNetModule that contains this ContentContainer
     * is not prepared to support tuners, this method will return null.
     *
     * @param name The name of the new ContentContainer.
     * @param permissions Access permissions of the new ContentContainer.
     * 
     * @return ContentContainer if a new ContentContainer has been created, 
     * otherwise returns null.  
     * 
     * @throws SecurityException if the caller does not have 
     *         HomeNetPermission("contentmanagement"), or if the caller
     *         does not have write permission on this container.
     */
    public ContentContainer createChannelGroupContainer(String name, ExtendedFileAccessPermissions permissions)
    {
        ContentContainer newCC = null;
        
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);
       
        if (!hasWritePermission())
        {
            throw new SecurityException(NO_WRITE_PERMISSONS);
        }

        if (isLocal())
        {
            newCC = MediaServer.getInstance().getCDS().createContainer(this, name, permissions,
                    ContentDirectoryService.CHANNEL_GROUP_TYPE);
        }
        
        return newCC;
    }
   

    /**
     * Creates a Content Item in the local CDS assuming that this Container is
     * local.
     * 
     * @param content
     *            a file handle to the physical content
     * @param name
     *            The name of the Content Item
     * @param permissions
     *            Access permissions for the Content Item
     * 
     * @return boolean value True if the Item was created successfully. False
     *         indicates that the Item was not created successfully. p
     * @throws SecurityException
     *             Thrown if the requester is unauthorized.
     * 
     */
    public boolean createContentItem(File content, String name, ExtendedFileAccessPermissions permissions)
            throws SecurityException
    {
        boolean state = false;
        
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);
        
        if (!hasWritePermission())
        {
            throw new SecurityException(NO_WRITE_PERMISSONS);
        }

        if (isLocal() && !isChannelGroupContainer())
        {
            // Creates item in CDS.
            ContentItemImpl newCI = MediaServer.getInstance().getCDS().createItem(this, name, permissions, content);
            
            // Add copy locally
            this.addChild(newCI);
            newCI.setEntryParent(this);
            state = true;
        }
        return state;
    }


    /**
     * Deletes this ContentContainer if and only if it is empty.
     * This method removes the content container from its parent. 
     * This method returns false if this is a root container.
     * This method deletes a local ContentContainer only. If the #isLocal
     * method returns false an exception is thrown.
     * 
     * @return True if this ContentContainer was deleted, otherwise returns
     *      false.
     * 
     * @throws java.lang.SecurityException if the application is denied to
     *      perform the action
     * @throws java.io.IOException if this ContentContainer is not local.
     */
    public boolean delete() throws IOException, SecurityException
    {
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);

        boolean state = false;
        
        if (!isLocal())
        {
            throw new IOException();
        }
        
        // Delete CDS container, check for empty occurs in the CDS.
        state = MediaServer.getInstance().getCDS().delete(this);
        
        // Delete local ContentContainer
        if(state)
        {
            recursiveRemove(this);
        }
        
        return state;
    }
    
    /**
     * Deletes this ContentContainer.
     * 
     * @return boolean True if the entry was deleted. False if the entry was not
     *         deleted
     * 
     * @throws IOException
     *             Thrown if an IO error condition occurred while trying to
     *             remove the entry.
     * 
     * @throws SecurityException
     *             Thrown if the requester doesn't have the appropriate access
     *             permissions.
     */
    public boolean deleteEntry() throws IOException, SecurityException
    {
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);
        
        boolean state = false;
        
        if(!isLocal())
        {
            throw new IOException();
        }
        
        // Delete CDS entry
        state = MediaServer.getInstance().getCDS().deleteContainer(this);
        
        // Delete local entry
        if(state)
        {
            recursiveRemove(this);
        }
                        
        return state;
    }  
    
    /**
     * Deletes all the ContentEntry objects in this container except for
     * ContentContainer entries. This method deletes local ContentEntry
     * instances only. If the #isLocal method returns false, an exception is 
     * thrown.
     * 
     * @return True if all of the ContentEntry objects required to be
     * deleted are deleted, otherwise returns false (e.g. ContentContainer entries
     * are not required to be deleted)
     * 
     * @throws SecurityException if the caller does not have 
     *         HomeNetPermission("contentmanagement"), or if the caller
     *         does not have write permission on this container or
     *         and entries contained in this container (except for
     *         ContentContainer entries).
     *         
     * @throws java.io.IOException if this ContentContainer is not local.
     */
    public boolean deleteContents() throws IOException, SecurityException
    {
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);
        
        if(!isLocal())
        {
            throw new IOException();
        }
        
        boolean state = false;
        
        // Delete CDS contents
        state = MediaServer.getInstance().getCDS().deleteContents(this);
        
        // Delete local contents
        if(state && m_children != null && m_children.size() > 0)
        {
            for(int i = 0; i < m_children.size(); ++i)
            {
                if(m_children.get(i) instanceof ContentContainer)
                {
                    state = removeEntry((ContentEntry)m_children.get(i));
                }
            }            
        }
        
        return state;
    }

    /**
     * If the recursive parameter is true, this method behaves in a manner
     * equivalent to {@link #deleteEntry}.  If the recursive parameter is false,
     * this method behaves in a manner equivalent to {@link #deleteContents}. 
     * This method deletes local ContentEntry instances only. If the #isLocal
     * method returns false, an exception is thrown.
     * 
     * If a SecurityException is thrown due to insufficient write access permissions 
     * on any entry contained within this ContentContainer, this method MAY delete 
     * a partial subset of the entries contained within.
     * 
     * @param recursive if true all entries and their entries are to be deleted.
     *
     * @return True if all ContentEntry objects that are required to be deleted are
     *      deleted, otherwise returns false.
     * 
     * @throws SecurityException if the caller does not have 
     *         HomeNetPermission("contentmanagement"), or if the caller
     *         does not have write permission on this container or
     *         any entries contained in this container.
     *         
     * @throws java.io.IOException if this ContentContainer is not local.
     *  
     * @see #deleteContents()
     * @see #delete() 
     **/
    public boolean deleteRecursive(boolean recursive) throws IOException, SecurityException
    {
        return recursive ? deleteEntry() : deleteContents();
    }

    /**
     * Retrieves all of the content entries associated with this content
     * container.
     * 
     * @return int value representing a count of the content entries associated
     *         with this ContentContainer.
     */
    public int getComponentCount()
    {
        return m_children != null ? m_children.size() : 0;
    }

    /**
     * Returns the UPnP Container Class
     * 
     * @return String representing the UPnP Container Class id.
     */
    public String getContainerClass()
    {
        return getMetadata(UPnPConstants.UPNP_CLASS);
    }

    /**
     * Iterates through the list of entries and sums the file sizes associated
     * with this entry,
     * 
     * @return long value which represents the sum of all all files associated
     *         with this ContentContainer.
     */
    public long getContentSize()
    {
        ContentEntry contentEntry = null;
        Enumeration enumContent = null;
        long size = 0;

        enumContent = getEntries();

        while ((enumContent != null) && enumContent.hasMoreElements())
        {
            contentEntry = (ContentEntry) enumContent.nextElement();

            if (contentEntry != null)
            {
                long cs = contentEntry.getContentSize();
                if (cs == -1)
                {
                    return -1; // at least one entry is indeterminate
                }
                size += cs;
            }
        }

        return size;
    }

    /**
     * Returns an Enumeration over all entries in this ContentContainers, or
     * null if there are no entries.
     * 
     * @return Enumeration containing entries for this ContentContainer.
     */
    public Enumeration getEntries()
    {
        Enumeration enumContents = null;

        Vector entries = getEntries(false);

        if ((entries != null) && (entries.size() != 0))
        {
            enumContents = entries.elements();
        }

        return enumContents;
    }

    /**
     * Returns a ContentList containing ContentEntries
     * 
     * @param filter
     *            specifies the property values each entry must satisfy in order
     *            to be added to the ContentList.
     * 
     * @param traverse
     *            boolean value True indicates to traverse sub containers, and
     *            their entries.
     * 
     * @return ContentList Containing entries satisfying the conditions of the
     *         filter.
     */
    public ContentList getEntries(ContentDatabaseFilter filter, boolean traverse)
    {
        ContentListImpl contentList = new ContentListImpl();
        ContentEntry contentEntry = null;
        Enumeration enumContents = null;

        Vector entries = getEntries(traverse);

        if (entries != null)
        {
            enumContents = entries.elements();
        }

        while ((enumContents != null) && enumContents.hasMoreElements())
        {
            contentEntry = (ContentEntry) enumContents.nextElement();

            if (filter == null || filter.accept(contentEntry))
            {
                contentList.add(contentEntry);
            }
        }

        return contentList;
    }

    /**
     * Returns the ContentEntry associated with the given ID in this container,
     * or NULL if no entry is found. This method SHALL recursively search this
     * container and any sub-containers. This method searches local cache only;
     * does not cause network activity.
     * 
     * @param ID
     *            the unique id of the ContentEntry
     * 
     * @return ContentEntry that is specified by the ID. If the ID doesn't exist
     *         in this ContentContainer, ContentEntry will be null.
     */
    public ContentEntry getEntry(String ID)
    {
        ContentEntry result = null;
        Enumeration enumContent = null;

        /* Get all entries, recursing=true */
        Vector entries = getEntries(true);
        if (entries != null)
        {
            enumContent = entries.elements();
        }

        /* Look for the specified ID in the returned entries */
        while ((enumContent != null) && enumContent.hasMoreElements())
        {
            ContentEntry contentEntry = (ContentEntry) enumContent.nextElement();

            if ((contentEntry != null) && contentEntry.getID().equalsIgnoreCase(ID))
            {
                result = contentEntry;
                break;
            }
        }

        if (result != null)
        {
            if (log.isDebugEnabled()) 
            {
                log.debug("getEntry - id: " + ID + ", result: " + result);
            }
        }
        return result;
    }

    public class ResBlockReference
    {
        final ContentEntry contentEntry;
        final int resIndex;
        final String resValue;
        
        public ResBlockReference(final ContentEntry contentEntry,final int resIndex,final String resValue)
        {
            this.contentEntry = contentEntry;
            this.resIndex = resIndex;
            this.resValue = resValue;
        }
        
        public ContentEntry getContentEntry()
        {
            return contentEntry;
        }

        public int getResIndex()
        {
            return resIndex;
        }

        public String getResValue()
        {
            return resValue;
        }
        
        public String toString()
        {
            return "ResBlock:{entry " + contentEntry 
                   + ", resIndex " + resIndex 
                   + ",resVal " + resValue + '}';
        }
    }
    
    /**
     * Look up an entry containing a res block or alternateURI matching the URL
     * 
     */
    public ResBlockReference getEntryByURL(URL url)
    {
        ResBlockReference foundEntry = null;
        Enumeration enumContent = null;

        /* Get all entries, recursing=true */
        Vector entries = getEntries(true);
        if (entries != null)
        {
            enumContent = entries.elements();
        }

        while ((enumContent != null) && enumContent.hasMoreElements() && foundEntry == null)
        {
            ContentEntry contentEntry = (ContentEntry) enumContent.nextElement();

            if ((contentEntry != null) && (contentEntry instanceof ContentItem))
            {
                final String resValues[] = (String[])contentEntry.getRootMetadataNode()
                                              .getMetadata(UPnPConstants.RESOURCE);
                // TODO: TRACE logging
                //    if (log.isDebugEnabled()) 
                //    {
                //        log.debug("getEntryByURL: Found " + resValues.length + " res URIs");
                //    }
                if (resValues != null)
                {
                    for (int i=0; i<resValues.length; i++)
                    {
                        final String resURI = resValues[i];
                        
                        try
                        {
                            // TODO: TRACE logging
                            //    if (log.isDebugEnabled()) 
                            //    {
                            //        log.debug("getEntryByURL: Looking at res URI " + resURI);
                            //    }
                            
                            if (checkURLAgainstEntry(url, resURI))
                            {
                                foundEntry = new ResBlockReference(contentEntry, i, resValues[i]);;
                                if (log.isDebugEnabled()) 
                                {
                                    log.debug("getEntryByURL: Found res match: " + resURI );
                                }
                                break;
                            }
                        }
                        catch (MalformedURLException murle)
                        {
                            if (log.isInfoEnabled()) 
                            {
                                log.info("getEntryByURL: parsing error for " 
                                         + resURI + ": " 
                                         + murle.getMessage() );
                            }
                            // Continue checking
                        }
                    } // END loop through res values
                }
                
                final String altURIValues[] = (String[])contentEntry.getRootMetadataNode()
                                                 .getMetadata(UPnPConstants.RESOURCE_ALT_URI);
                if (foundEntry == null && altURIValues != null)
                {
                    for (int i=0; i<altURIValues.length; i++)
                    {
                        final String resURI = altURIValues[i];
                        
                        try
                        {
                            if (checkURLPathAgainstEntry(url, resURI))
                            {
                                foundEntry = new ResBlockReference(contentEntry, i, resValues[i]);
                                if (log.isDebugEnabled()) 
                                {
                                    log.debug("getEntryByURL: Found alt URI match: " + resURI );
                                }
                                break;
                            }
                        }
                        catch (MalformedURLException murle)
                        {
                            if (log.isInfoEnabled()) 
                            {
                                log.info("getEntryByURL: parsing error for " 
                                         + resURI + ": " 
                                         + murle.getMessage() );
                            }
                            // Continue checking
                        }
                    } // END loop through alt uri values
                }
            }
        } // END loop through content entries
        
        if (foundEntry != null)
        {
            if (log.isDebugEnabled()) 
            {
                log.debug("getEntryByURL for: " + url.toString() + ", returning: " + foundEntry);
            }
        }

        return foundEntry;
    }
    
    boolean checkURLAgainstEntry(final URL requestURL, final String entryURL) 
        throws MalformedURLException
    {
        final URL resURL;
        resURL = new URL(entryURL);

        if  ((resURL.getHost().equals(MediaServer.HOST_PORT_PLACEHOLDER)
                || resURL.getHost().equals(requestURL.getHost()))
              && resURL.getPath().equals(requestURL.getPath())
              && (resURL.getQuery() == requestURL.getQuery() // If both null
                   || resURL.getQuery().equals(requestURL.getQuery())))
        {
            return true;
        }
        
        return false;
    } // END checkURLAgainstEntry()
    
    boolean checkURLPathAgainstEntry(final URL requestURL, final String entryURL) 
    throws MalformedURLException
    {
        final URL resURL;
        resURL = new URL(entryURL);

        return (resURL.getPath().equals(requestURL.getPath()));
    } // END checkURLPathAgainstEntry()

    public String dumpDiddly(ContentListImpl list, ContentEntry entry)
    {
        list.add(entry);
        if (entry instanceof ContentContainer)
        {
            if (m_children != null)
            {
                for (int i = 0; i < m_children.size(); i++)
                {
                    dumpDiddly(list, (ContentEntry) m_children.get(i));
                }
            }
        }
        return DIDLLite.getView(list);
    }

    /**
     * Returns the n'th entry located within this container.
     * 
     * @param n
     *            an int value indicating the offset within this container.
     * 
     * @return ContentEntry associated with this ContentContainer located in the
     *         n'th position.
     */
    public ContentEntry getEntry(int n)
    {

        ContentEntry result = null;
        int i = n + 1;
        Enumeration enumContent = null;

        /* Get all entries, recursing=true */
        Vector entries = getEntries(true);
        if (entries != null)
        {
            enumContent = entries.elements();
        }

        /* now get the n'th element */
        if (enumContent != null)
        {
            while ((i != 0) && enumContent.hasMoreElements())
            {
                ContentEntry contentEntry = (ContentEntry) enumContent.nextElement();
                i--;
                if (i == 0)
                {
                    result = contentEntry;
                    break;
                }
            }
        }
        if (result == null)
        {
            throw new ArrayIndexOutOfBoundsException();
        }
        return result;
    }

    /**
     * Returns the offset of the specified ContentEntry within this
     * ContentContainer
     * 
     * @param n
     *            ContentEntry to retrieve the index for.
     * 
     * @return int value representing the index or offset into this
     *         ContentContainer for the specified ContentEntry.
     */
    public int getIndex(ContentEntry n)
    {
        int index = -1;

        if (n != null)
        {
            Vector entries = getEntries(false);
            if (entries != null)
            {
                index = entries.indexOf(n);
            }
        }

        return index;
    }

    /**
     * Returns the name of this ContentContainer
     * 
     * @return String representing the name of this ContentContainer.
     */
    public String getName()
    {
        return getMetadata(UPnPConstants.TITLE);
    }

    /**
     * Returns the boolean status of this ContentContainer. If the
     * ContentContainer is empty, meaning that it doesn't have any
     * ContentEntry's associated with this ContentContainer.
     * 
     * @return boolean value of True indicates that this ContentContainer is not
     *         empty.
     */
    public boolean isEmpty()
    {
        return (getComponentCount() == 0);
    }

    /**
     * Dereferences the entries associated with this ContentContainer and
     * assigns then a new reference to the parent of this ContentContainer.
     * 
     * @param entries
     *            The specified entries to be dereferenced.
     * 
     * @return boolean value <true> if the values have been reassigned.
     * 
     * @throws IllegalArgumentException
     *             Thrown if one of the entries is a NetRecording Entry
     *             containing one or more RecordingContentItems.
     * 
     * @throws SecurityException
     *             Thrown if the user is not authorized to perform this action.
     */
    public boolean removeContentEntries(ContentEntry[] entries) throws SecurityException, IllegalArgumentException
    {
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);

        boolean state = false;
        
        if (isLocal() && entries != null)
        {
            // Check for locality of all entries before removing any.
            for (int i = 0; i < entries.length; ++i)
            {
                if (entries[i] != null && !entries[i].isLocal())
                {
                    return false;
                }
            }
            
            // Remove CDS entries
            state = MediaServer.getInstance().getCDS().removeContentEntries(this, entries);
            
            // Remove local entries 
            if(state)
            {
                for (int i = 0; i < entries.length; ++i)
                {
                    recursiveRemove(entries[i]);
                }
            }
        }

        return state;
    }

    /**
     * Removes an entry from this container
     * 
     * @param entry
     *            A reference to the entry to be removed.
     * 
     * @return boolean True if the entry was successfully removed. False
     *         indicates the entry was not removed.
     * 
     * @throws IllegalArgumentException
     *             Thrown if one of the entries is a NetRecording Entry
     *             containing one or more RecordingContentItems.
     * 
     * @throws SecurityException
     *             Thrown if the user is not authorized to perform this action.
     */
    public boolean removeContentEntry(ContentEntry entry) throws SecurityException, IllegalArgumentException
    {
        SecurityUtil.checkPermission(CONTENT_MANAGEMENT_PERMISSION);
       
        boolean state = false;

        // Remove CDS entry
        if(isLocal() && 
                MediaServer.getInstance().getCDS().removeContentEntry(this, entry, true))
        {
            state = true;
        }
        
        // Remove local entry
        recursiveRemove(entry);
                        
        return state;
    }

    /**
     * Converts the Entries associated with this container to an array of
     * ContentEntry objects
     * 
     * @return ContentEntry[] and array of ContentEntries associated with this
     *         ContentContainer.
     */
    public ContentEntry[] toArray()
    {
        Vector entries = getEntries(false);
        if (entries != null)
        {
            return (ContentEntry[]) entries.toArray(new ContentEntry[entries.size()]);
        }
        return new ContentEntry[0];
    }
/*
    public IAction getAction()
    {
        return m_action;
    }
*/
    /**
     * Recursively builds a ContentList of all ContentEntries under this
     * container.
     * 
     * @return ContentList of all ContentEntries under this container.
     */
    public ContentListImpl getContentList()
    {
        ContentListImpl cl = new ContentListImpl();
        buildContentList(cl);
        return cl;
    }

    /**
     * Returns direct child ContentEntries of this container as a sorted
     * ContentList.
     * 
     * @param sortCriteria
     *            description of fields to sort by
     * @param startingIndex
     *            index of child to start with
     * @param limit
     *            number of ContentEntries to include
     * @param localRequest
     * @return Sorted ContentList constrained by starting index and limit
     */
    public ContentList getSortedChildren(String sortCriteria, int startingIndex, int limit, boolean localRequest)
    {
        ContentListImpl cl = new ContentListImpl();
        if (sortCriteria == null || m_children == null)
        {
            return cl;
        }

        // Need to filter out hidden children prior to sorting
        Vector visibleChildren = getVisibleChildren(localRequest);
        
        int numEntries = visibleChildren.size();
        int start = 0;

        if (startingIndex > 0 && start < visibleChildren.size())
        {
            start = startingIndex;
        }

        if (limit > 0 && limit + start < visibleChildren.size())
        {
            numEntries = limit + start;
        }

        for (int i = start; i < numEntries; i++)
        {
            cl.add(visibleChildren.get(i));
        }

        if (sortCriteria != null && sortCriteria.length() > 0)
        {
            Collections.sort(cl, new ContentEntryComparator(sortCriteria));
        }

        return cl;
    }
    
    // Factor out logic to find visible children.
    public Vector getVisibleChildren(boolean localRequest)
    {

        // Need to filter out hidden children prior to sorting
        Vector visableChildren = new Vector();
        if (m_children != null)
        {
            for(int i = 0; i < m_children.size(); i++)
            {
                ContentEntryImpl child = (ContentEntryImpl)m_children.get(i);
                if(child != null)
                {
                    if(localRequest || !child.isHiddenContent())
                    {
                        visableChildren.add(m_children.get(i));
                    }
                }
            }
        }
        
        return visableChildren;
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
        if (log.isDebugEnabled())
        {
            log.debug("ContentContainerImpl.isInUse() - called");
        }

        // Only local items could possibly be in use
        if (isLocal())
        {
            // Make sure this is the real CDS entry, not a proxy resulting from
            // a CDS action
            ContentContainerImpl cci = (ContentContainerImpl) getCDSContentEntry();
            if (cci == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ContentContainerImpl.isInUse() - "
                    + "got back NULL, already have real content container");
                }
                cci = this;
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ContentContainerImpl.isInUse() - retrieved the real content container");
                }
            }

            // Use the real content entry to check for in use
            int cnt = 0;
            if (cci.m_children != null)
            {
                for (Iterator i = cci.m_children.iterator(); i.hasNext();)
                {
                    ContentEntryImpl ce = (ContentEntryImpl) i.next();
                    if (log.isDebugEnabled())
                    {
                        log.debug("ContentContainerImpl.isInUse() - calling child " + cnt + "isInUse method");
                    }
                    if (ce.isInUse())
                    {
                        return true;
                    }
                    cnt++;
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("ContentContainerImpl.isInUse() - children were NULL");
                }
            }
        }

        return false;
    }

    /**
     * Adds an entry to the CDS and then adds a local version of the new CDS entry.
     * @param entry the ContentEntry you want to add.
     * @return True if added, False if not added.
     */
    public boolean addEntry(ContentEntryImpl entry)
    {
        if(entry == null)
        {
            return false;
        }

        // Capturing parent before adding to CDS where parent will be changed.
        ContentContainerImpl origParent = entry.m_entryParent;
        
        // Adds or moves entry in the CDS
        String id = MediaServer.getInstance().getCDS().addEntry(this, entry);
        if(id == null)
        {
            return false;
        }        
        
        // Add newly added or moved entry from CDS locally
        LocalNetActionHandler nah = new LocalNetActionHandler();
        
        // Have been getting null content lists from response.  Should not be the case.
        // Attempt to query a few times if this issue continues, then fail.
        ContentList cl = null;
        int i = 0;
        while(cl == null)
        {
            i++;
            ContentServerNetModule csnm = ((NetManagerImpl)(NetManagerImpl.getInstance())).getLocalCDS();
            csnm.requestBrowseEntries(id, "*", false, 0, 1, "", nah);
            NetActionEvent event = nah.getLocalEvent();
            cl = event != null ? (ContentList) event.getResponse() : null;
            // Picked five arbitrarily.  Should not occur once, if it does five times then surely something else is
            // happening.
            if(i > 5)
            {
                // Removing entry from CDS and failing call.
                MediaServer.getInstance().getCDS().removeEntry(entry);
                return false;
            }
        }
        
        ContentEntryImpl ce = (ContentEntryImpl)cl.nextElement();
        if(ce != null)
        {
            addChild(ce);
            ce.setEntryParent(this);
        }
        
        // Remove old local reference from previous parent
        // to satisfy the ContentContainer.addEntry requirement.
        if(origParent != null && origParent.m_children != null)
        {
            origParent.m_children.remove(entry);
        }
        
        return true;
    }

    /**
     * Recursive method used to add all ContentEntries to a ContentList
     * 
     * @param cl
     *            list to add ContentEntries to
     */
    private void buildContentList(ContentListImpl cl)
    {
        if (m_children != null)
        {
            for (int i = 0; i < m_children.size(); i++)
            {
                Object obj = m_children.get(i);
                if (obj instanceof ContentEntry)
                {
                    cl.add(obj);
                }

                if (obj instanceof ContentContainerImpl)
                {
                    ((ContentContainerImpl) obj).buildContentList(cl);
                }
            }
        }
    }

    /**
     * checks to see if entries exist within this container
     * 
     * @param entries
     *            a list of entries to verify.
     * 
     * @return boolean true of the entries exist in this container.
     */
    public boolean contains(ContentEntry[] entries)
    {
        boolean state = true;

        for (int i = 0; i < entries.length; ++i)
        {
            if (!contains(entries[i]))
            {
                state = false;
                break;
            }
        }

        return state;
    }
    
    /**
     * Internal method to recursively remove ContentEntries from object graph.
     * @param entry Entry to remove, if a ContentContainer, it will remove it's 
     * entries recursively.
     */
    private void recursiveRemove(ContentEntry entry)
    {
        if(entry instanceof ContentContainerImpl)
        {
            Vector v = ((ContentContainerImpl)entry).m_children;
            if(v != null && v.size() > 0)
            {
                while(v.size() > 0)
                {
                    ContentEntry e = (ContentEntry)v.get(0);
                    if(e instanceof ContentContainerImpl)
                    {
                        recursiveRemove(e);
                    }
                    else
                    {
                        v.remove(0);
                    }
                    
                    if(e instanceof ContentEntryImpl)
                    {
                        ((ContentEntryImpl)e).setEntryParent(null);
                    }
                }
            }
        }
        
        if(entry instanceof ContentEntryImpl)
        {
            removeEntry(entry);
            ((ContentEntryImpl)entry).setEntryParent(null);
        }
    }
    
    /**
     * Removes a ContentEntry node from this ContentContainer
     * 
     * @return boolean True if the entry was successfully removed from the ContentContainer.
     *         False indicates that an error occurred when processing the entry.
     */
    public boolean removeEntry(ContentEntry entry)
    {
        if (entry == null)
        {
            return false;
        }

        boolean retVal = false;
        
        if (m_children != null)
        {
            for (int i = 0; i < m_children.size(); i++)
            {
                ContentEntry child = (ContentEntry) m_children.get(i);
                if (child.getID().equals(entry.getID()))
                {
                    if (child instanceof ContentEntryImpl)
                    {
                        ((ContentEntryImpl) child).setEntryParent(null);
                    }

                    m_children.remove(i);
                    
                    retVal = true;
                    break;
                }
            }
        }

        return retVal;
    }

    /**
     * Returns an Vector of entries associated with this Container. If there are
     * no entries associated with this container, then return null;
     * 
     * @param traverse
     *            boolean value True indicates to traverse sub containers, and
     *            their entries.
     * 
     * @return Vector Containing entries for this ContentContainer and
     *         subcontainers if traverse is true.
     * 
     */
    protected Vector getEntries(boolean traverse)
    {
        Vector entries = null;
        if (null != m_children)
        {
            if (!traverse)
            {
                /* Simply add all the children of this container */
                entries = new Vector();
                entries.addAll(m_children);
            }
            else
            {
                /*
                 * Iterate through all entries, recursing if entry is container
                 */
                ContentEntry ce;
                Enumeration enumEntries = m_children.elements();
                entries = new Vector();
                while (enumEntries.hasMoreElements())
                {
                    ce = (ContentEntry) enumEntries.nextElement();
                    entries.add((Object) ce);
                    if (ce instanceof ContentContainerImpl)
                    {
                        Vector children = ((ContentContainerImpl) ce).getEntries(traverse);
                        if (children != null)
                        {
                            entries.addAll(children);
                        }
                    }
                }
            } /* endif !traverse */
        }
        return entries;
    }

    /**
     * Adds a ContentEntry to this ContentContainer and refreshes the child count property.
     * @param child ContentEntry to add
     */
    public void addChild(ContentEntryImpl child)
    {
        if (null == m_children)
        {
            m_children = new Vector();
        }
        m_children.add(child);
        resetChildCount();
    }
        
    /**
     * Override parent method and reset container specific update values;
     */
    public long serviceReset(long objectUpdateID)
    {
        if(getRootMetadataNode() == null)
        {
            return objectUpdateID; 
        }
        long curObjectUpdateID = super.serviceReset(objectUpdateID);
        
        ((MetadataNodeImpl)getRootMetadataNode()).addMetadataRegardless(UPnPConstants.QN_UPNP_CONTAINER_UPDATE_ID, "0");
        ((MetadataNodeImpl)getRootMetadataNode()).addMetadataRegardless(UPnPConstants.QN_UPNP_TOTAL_DELETED_CHILD_COUNT, "0");

        // Perform a service reset on any children
        if (m_children != null)
        {
            Iterator i = m_children.iterator();
            while (i.hasNext())
            {
                Object child = i.next();
                curObjectUpdateID++;
                if (child instanceof ContentContainerImpl)
                {
                    curObjectUpdateID = ((ContentContainerImpl)child).serviceReset(curObjectUpdateID);
                }
                else if (child instanceof ContentEntryImpl)
                {
                    curObjectUpdateID = ((ContentEntryImpl)child).serviceReset(curObjectUpdateID);
                }
            }
        }
        return curObjectUpdateID;
    }

    /**
     * Updates the metadata for ContainerUpdateID.
     * @param systemUpdateID
     */
    public void setContainerUpdateID(long systemUpdateID)
    {
        if(getRootMetadataNode() == null)
        {
            return;
        }
        
        ((MetadataNodeImpl)getRootMetadataNode()).addMetadataRegardless(UPnPConstants.QN_UPNP_CONTAINER_UPDATE_ID, 
                Long.toString(systemUpdateID));        
    }
    
    /**
     * Overrides the ContentEntryImpl call, sets the ContainerUpdateID, but not the parents.
     */
    public void setObjectUpdateID(long systemUpdateID)
    {
        if(getRootMetadataNode() == null)
        {
            return;
        }
        
        ((MetadataNodeImpl)getRootMetadataNode()).addMetadataRegardless(UPnPConstants.QN_UPNP_OBJECT_UPDATE_ID, 
                    Long.toString(systemUpdateID));
        
        setContainerUpdateID(systemUpdateID);
    }
    
    /**
     * Updates the metadata when a child is deleted from this container.
     */
    public void incrementDeletedChildCount()
    {
        ((MetadataNodeImpl)getRootMetadataNode()).addMetadataRegardless(UPnPConstants.QN_UPNP_TOTAL_DELETED_CHILD_COUNT, Long.toString(++totalDeletedChildCount));
    }
    

    /**
     * Sets the child count property for this ContentContainer to the current size of the children in the container.
     */
    public void resetChildCount()
    {
        ((MetadataNodeImpl)getRootMetadataNode()).addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_CHILD_COUNT_ATTR, Integer.toString(getComponentCount()));
    }

    private boolean isChannelGroupContainer()
    {
        return CHANNEL_GROUP_CONTAINER.equals(getMetadataRegardless(UPnPConstants.QN_UPNP_CLASS));
    }
}
