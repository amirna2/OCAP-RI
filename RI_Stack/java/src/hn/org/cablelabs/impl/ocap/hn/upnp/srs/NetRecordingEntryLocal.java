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

package org.cablelabs.impl.ocap.hn.upnp.srs;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.content.ContentContainerImpl;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.recording.NetRecordingEntryImpl;
import org.cablelabs.impl.ocap.hn.recording.RecordingActions;
import org.cablelabs.impl.ocap.hn.recording.RecordingContentItemImpl;
import org.cablelabs.impl.ocap.hn.recording.RecordingNetModuleImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.cds.DIDLLite;
import org.cablelabs.impl.ocap.hn.upnp.srs.ScheduledRecordingService;
import org.cablelabs.impl.util.Containable;
import org.cablelabs.impl.util.SecurityUtil;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.HomeNetPermission;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.IOStatus;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * Content Directory NetRecordingEntry.
 * 
 * @author Michael Jastad
 * @version $Revision$
 */
public class NetRecordingEntryLocal implements NetRecordingEntry, IOStatus, Containable
{
    private static final Logger log = Logger.getLogger(NetRecordingEntryLocal.class);

    private static final String[] EMPTY_STRING_ARRAY = {};

    private static final HomeNetPermission PERMISSION = new HomeNetPermission("recordinghandler");

    private final List recordingContentItems = new LinkedList();

    private RecordSchedule recordSchedule = null;

    /**
     * Construct an object of this class.
     * 
     * @param metadataNode
     *            The metadata node representing this piece of content.
     */
    public NetRecordingEntryLocal(MetadataNodeImpl metadataNode)
    {
        netRecordingEntry = new NetRecordingEntryImpl(this, metadataNode);
    }

    /**
     * Adds a recording content item to this entry.
     * 
     * @param item
     *            The RecordingContentItem to be added
     * 
     * @throws IllegalStateException
     *             DOCUMENT ME!
     * @throws IllegalArgumentException
     *             DOCUMENT ME!
     * @throws SecurityException
     *             DOCUMENT ME!
     * @throws IOException
     *             DOCUMENT ME!
     */
    public void addRecordingContentItem(RecordingContentItem item) throws IllegalArgumentException, SecurityException,
            IOException, IllegalStateException
    {
        if (!isLocal())
        {
            throw new IOException("addRecordingContentItem() called with isLocal() false");
        }

        SecurityUtil.checkPermission(PERMISSION);

        if (item == null)
        {
            throw new IllegalArgumentException("Input parameter is null.");
        }

        if (!(item instanceof RecordingContentItemLocal))
        {
            throw new IllegalArgumentException("Input RecordingContentItem is not local.");
        }

        RecordingContentItemLocal recordingContentItemLocal = (RecordingContentItemLocal) item;

        // check to see if this entry has been associated with a RecordSchedule
        if (recordSchedule == null)
        {
            throw new IllegalStateException("NetRecordingEntry is not associated with a RecordSchedule");
        }

        // check to see if the item has been associated with a RecordTask
        if (recordingContentItemLocal.hasRecordTask())
        {
            throw new IllegalArgumentException("RecordingContentItem is associated with a RecordTask");
        }

        synchronized (recordingContentItems)
        {
            // associate this instance with the item
            recordingContentItemLocal.addNetRecordingEntry(this);

            if (!recordingContentItems.contains(recordingContentItemLocal))
            {
                recordingContentItems.add(recordingContentItemLocal);
            }
        }
    }

    /**
     * Helper method used to add id to the RCIList
     * 
     * @param id
     *            of RecordingContentItem to add to list.
     */
    public void addRecordingContentItemID(String id)
    {
        if (id == null)
        {
            return;
        }

        String[] ids = getRecordingContentItemIDs();
        for (int i = 0; i < ids.length; i++)
        {
            // If already in the list return
            if (id.equals(ids[i]))
            {
                return;
            }
        }

        StringBuffer idString = getMetadata(PROP_RCI_LIST) == null ? new StringBuffer() : new StringBuffer(getMetadata(PROP_RCI_LIST));

        if (idString.length() > 0)
        {
            idString.append(",");
        }
        idString.append(id);

        getRootMetadataNode().addMetadata(PROP_RCI_LIST, idString.toString());
    }

    public boolean deleteEntry() throws IOException, SecurityException
    {
        return netRecordingEntry.deleteEntry();
    }

    /**
     * Add this object to a <code>ContentContainer</code>.
     *
     * @param c The <code>ContentContainer</code>.
     *
     * @return True if the addition was successful; else false.
     */
    public boolean enter(ContentContainer c)
    {
        boolean retVal = true;

        // If the entry doesn't already have a RecordSchedule, create one
        // and and associate it with this NetRecordingEntry.
        if (!hasRecordSchedule())
        {
            if (!createRecordSchedule())
            {
                retVal = false;
            }
        }

        if (retVal) // The NetRecordingEntry has a RecordSchedule
        {
            //
            // add the NetRecordingEntry to the CDS
            //
            retVal = ((ContentContainerImpl) c).addEntry(netRecordingEntry);

            if (retVal)
            {
                setCdsReference(DIDLLite.getView(this));
                setScheduledCDSEntryID(getID());
            }
        }

        return retVal;
    }

    /**
     * Remove this object from a <code>ContentContainer</code>.
     *
     * @param c The <code>ContentContainer</code>.
     *
     * @return True if the removal was successful; else false.
     */
    public boolean exit(ContentContainer c)
    {
        boolean retVal = true;

        if (hasRecordSchedule())
        {
            RecordingContentItem[] rcis;

            try
            {
                rcis = getRecordingContentItems();
            }
            catch (IOException e)
            {
                // this will never happen since isLocal is true
                // NetRecordingEntry is local and does not have any
                // RecordingContentItems

                if (log.isDebugEnabled())
                {
                    log.debug("This was supposed to never happen.");
                }

                return false;
            }

            if (rcis.length > 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Cannot remove a NetRecordingEntry that contains RecordingContentItems.");
                }
                throw new IllegalArgumentException("NetRecordingEntry contains one or more RecordingContentItems");
            }

            if (retVal)
            {
                retVal = removeRecordSchedule();
            }
        }

        if (retVal)
        {
            retVal = ((ContentContainerImpl) c).removeEntry(this);
        }

        return retVal;
    }

    public long getContentSize()
    {
        return netRecordingEntry.getContentSize();
    }

    public Date getCreationDate()
    {
        return netRecordingEntry.getCreationDate();
    }

    public ContentContainer getEntryParent() throws IOException
    {
        return netRecordingEntry.getEntryParent();
    }

    public ExtendedFileAccessPermissions getExtendedFileAccessPermissions()
    {
        return netRecordingEntry.getExtendedFileAccessPermissions();
    }

    public String getID()
    {
        return netRecordingEntry.getID();
    }

    public String getMetadata(String key)
    {
        Object obj = getRootMetadataNode().getMetadata(key);

        return obj != null ? obj.toString() : "";
    }

    public String getParentID()
    {
        return netRecordingEntry.getParentID();
    }

    /**
     * Implementation of NetRecordingEntry interface.
     * 
     * @see org.ocap.hn.recording.NetRecordingEntry#getRecordingContentItemIDs()
     */
    public String[] getRecordingContentItemIDs()
    {
        final String rciList = getMetadata(PROP_RCI_LIST);

        if (rciList == null)
        {
            return EMPTY_STRING_ARRAY;
        }

        // split rciList by commas, omitting empty strings

        int j; // id counter

        final char[] ca = rciList.toCharArray();
        final int n = ca.length;

        j = 0;

        for (int i = 0; i < n;)
        {
            int count = 0;

            for (; i < n && ca[i] != ','; ++i)
            {
                ++count;
            }

            if (count > 0)
            {
                ++j;
            }

            if (i < n)
            {
                ++i;
            }
        }

        if (j == 0)
        {
            return EMPTY_STRING_ARRAY;
        }

        final String[] result = new String[j];

        j = 0;

        for (int i = 0; i < n;)
        {
            int offset = i;
            int count = 0;

            for (; i < n && ca[i] != ','; ++i)
            {
                ++count;
            }

            if (count > 0)
            {
                result[j] = new String(ca, offset, count);
                ++j;
            }

            if (i < n)
            {
                ++i;
            }
        }

        return result;
    }

    /**
     * Returns a list of RecordingContentItems
     * 
     * @return an array of RecordingContentItems.
     * 
     * @throws IOException
     *             Throws exception if this entry is not local.
     */
    public RecordingContentItem[] getRecordingContentItems() throws IOException
    {
        if (!isLocal())
        {
            throw new IOException("getRecordingContentItems() called with isLocal() false");
        }

        return (RecordingContentItem[]) recordingContentItems.toArray(new RecordingContentItem[recordingContentItems.size()]);
    }

    /**
     * Get the root MetadataNode of the CDS content entry.
     *
     * @return The root MetadataNode.
     */
    public MetadataNode getRootMetadataNode()
    {
        return netRecordingEntry.getRootMetadataNode();
    }

    public ContentServerNetModule getServer()
    {
        return netRecordingEntry.getServer();
    }

    /**
     * Removes a specified RecordingContentItem from this entry.
     * 
     * @param item
     *            A reference to the item to be removed.
     * 
     * @throws IllegalArgumentException
     *             Thrown if the item is null
     * @throws SecurityException
     *             Thrown is not authorized.
     * @throws IOException
     *             Thrown if not local.
     */
    public void removeRecordingContentItem(RecordingContentItem item) throws IllegalArgumentException,
            SecurityException, IOException
    {
        if (!isLocal())
        {
            throw new IOException("removeRecordingContentItem() called with isLocal() false");
        }

        SecurityUtil.checkPermission(PERMISSION);

        if (item == null)
        {
            throw new IllegalArgumentException("Input parameter is null.");
        }

        RecordingContentItemLocal recordingContentItemLocal = null;

        if (item instanceof RecordingContentItemLocal)
        {
            recordingContentItemLocal = (RecordingContentItemLocal) item;
        }
        else if (item instanceof RecordingContentItemImpl)
        {

            recordingContentItemLocal = (RecordingContentItemLocal)
                ((RecordingContentItemImpl) item).getLocalRecordingContentItem();
        }

        if (recordingContentItemLocal == null)
        {
            throw new IllegalArgumentException("Input RecordingContentItem is not local.");
        }

        // check to see if the item has been associated with a RecordTask
        if (recordingContentItemLocal.hasRecordTask())
        {
            throw new IllegalArgumentException("RecordingContentItem is associated with a RecordTask");
        }

        synchronized (recordingContentItems)
        {
            if (recordingContentItems.contains(recordingContentItemLocal))
            {
                recordingContentItems.remove(recordingContentItemLocal);
            }

            // remove the association between this instance and the item
            recordingContentItemLocal.removeNetRecordingEntry(this);
        }
    }

    /**
     * Check for association with RecordSchedule
     * 
     * @return true if RecordSchedule associated with this instance, false if
     *         not.
     */
    public boolean hasRecordSchedule()
    {
        return this.recordSchedule != null;
    }

    /**
     * Instances of this class are always local.
     */
    public boolean isLocal()
    {
        return true;
    }

    /**
     * Creates a RecordSchedule and associates it with this NetRecordingEntry
     * 
     * @return true if successful, false if not.
     */
    public boolean createRecordSchedule()
    {
        // only create once
        if (this.recordSchedule != null)
        {
            return true;
        }

        this.recordSchedule = ((ScheduledRecordingService) MediaServer.getInstance().getSRS()).createRecordSchedule(this);

        if (recordSchedule != null)
        {
            getRootMetadataNode().addMetadata(RecordingActions.RECORD_SCHEDULE_ID_KEY, recordSchedule.getObjectID());

            return true;
        }

        return false;
    }

    /**
     * Removes the RecordSchedule associated with this NetRecordingEntry. Can
     * only remove if there are no RecordTasks in the RecordSchedule and no
     * RecordingContentItems in this NetRecordingEntry.
     * 
     * @return true if successful, false if not.
     */
    public boolean removeRecordSchedule()
    {
        if (recordingContentItems.size() > 0)
        {
            return false;
        }

        if (this.recordSchedule != null)
        {
            if (!((ScheduledRecordingService) MediaServer.getInstance().getSRS()).removeRecordSchedule(this.recordSchedule))
            {
                return false;
            }
        }

        this.recordSchedule = null;

        return true;
    }

    /**
     * Sets the ocap:scheduledCDSEntryID to the RecordSchedule associated with
     * this NetRecordingEntry.
     * 
     * @param id
     *            ocap:scheduledCDSEntryID
     */
    public void setScheduledCDSEntryID(String id)
    {
        if (recordSchedule != null)
        {
            recordSchedule.setScheduledCDSEntryID(id);
        }
    }

    /**
     * Gets the ocap:scheduledCDSEntryID of the RecordSchedule associated with
     * this NetRecordingEntry.
     * 
     * @param id
     *            ocap:scheduledCDSEntryID
     */
    public String getScheduledCDSEntryID()
    {
        return recordSchedule != null ? recordSchedule.getScheduledCDSEntryID() : null;
    }

    /**
     * Sets the ocap:cdsReference to the RecordSchedule associated with this
     * NetRecordingEntry.
     * 
     * @param reference
     *            ocap:cdsReference
     */
    public void setCdsReference(String reference)
    {
        if (recordSchedule != null)
        {
            recordSchedule.setCdsReference(reference);
        }
    }

    /**
     * Returns the ocap:cdsReference to the RecordSchedule associated with this
     * NetRecordingEntry.
     */
    public String getCdsReference()
    {
        return recordSchedule != null ? recordSchedule.getCdsReference() : null;
    }

    /**
     * Returns the upnp:srsRecordScheduleID of the RecordSchedule associated
     * with this NetRecordingEntry.
     * 
     * @return upnp:srsRecordScheduleID
     */
    public String getSrsRecordScheduleID()
    {
        String retVal = "";

        if (this.recordSchedule != null)
        {
            retVal = this.recordSchedule.getObjectID();
        }

        return retVal;
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
            log.debug("NetRecordingEntryLocal.isInUse() - called");
        }

        // In use is only applicable to local items
        if (isLocal())
        {
            for (Iterator i = recordingContentItems.iterator(); i.hasNext();)
            {
                RecordingContentItemLocal rci = (RecordingContentItemLocal) i.next();

                if (rci.isInUse())
                {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * Returns the RecordSchedule associated with this NetRecordingEntry
     */
    RecordSchedule getRecordSchedule()
    {
        return this.recordSchedule;
    }

    /**
     * Check for ocap:cdsReference and ocap:scheduledCDSEntryID entries. No
     * validation is performed on the entries. This is only a check for their
     * existence.
     * 
     * @return true if this instance has the above references, false if not
     */
    boolean hasCdsReferences()
    {
        if (this.getCdsReference().equals(""))
        {
            return false;
        }
        else if (this.getScheduledCDSEntryID().equals(""))
        {
            return false;
        }

        return true;
    }

    // the CDS representation of this instance
    private final NetRecordingEntryImpl netRecordingEntry;
}
