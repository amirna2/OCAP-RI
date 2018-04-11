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

package org.cablelabs.impl.ocap.hn.recording;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.cablelabs.impl.ocap.hn.content.ContentContainerImpl;
import org.cablelabs.impl.ocap.hn.content.ContentEntryImpl;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.ocap.hn.HomeNetPermission;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.IOStatus;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.upnp.common.UPnPAction;

/**
 * Content Directory NetRecordingEntry.
 * 
 * @author Michael Jastad
 * @version $Revision$
 */
public class NetRecordingEntryImpl extends ContentEntryImpl implements NetRecordingEntry
{
    private static final Logger log = Logger.getLogger(NetRecordingEntryImpl.class);

    private static final String[] EMPTY_STRING_ARRAY = {};

    private static final HomeNetPermission PERMISSION = new HomeNetPermission("recordinghandler");

    /**
     * The NetRecordingEntryLocal associated with this
     * NetRecordingEntryImpl, if any.
     */
    private final NetRecordingEntry netRecordingEntryLocal;

    /**
     * Construct a server-side object of this class.
     * 
     * @param netRecordingEntryLocal
     *            The NetRecordingEntryLocal associated with this NetRecordingEntryImpl.
     * @param metadataNode
     *            The metadata node representing this piece of content.
     */
    public NetRecordingEntryImpl(NetRecordingEntry netRecordingEntryLocal, MetadataNodeImpl metadataNode)
    {
        super(metadataNode);
        this.netRecordingEntryLocal = netRecordingEntryLocal;
    }

    /**
     * Construct a client-side object of this class.
     * 
     * @param action
     *            The action for which this object is being constructed.
     * @param metadataNode
     *            The metadata node representing this piece of content.
     */
    public NetRecordingEntryImpl(UPnPAction action, MetadataNodeImpl metadataNode)
    {
        super(action, metadataNode);
        this.netRecordingEntryLocal = null;
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

        NetRecordingEntryImpl nrei = (NetRecordingEntryImpl) getCDSContentEntry();
        if (nrei == null)
        {
            nrei = this;
        }

        assert nrei.netRecordingEntryLocal != null;

        nrei.netRecordingEntryLocal.addRecordingContentItem(item);
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

        StringBuffer idString = getMetadata(PROP_RCI_LIST) == null ? new StringBuffer() : new StringBuffer(
                getMetadata(PROP_RCI_LIST));

        if (idString.length() > 0)
        {
            idString.append(",");
        }
        idString.append(id);

        m_metadataNode.addMetadata(PROP_RCI_LIST, idString.toString());
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

        NetRecordingEntryImpl nrei = (NetRecordingEntryImpl) getCDSContentEntry();
        if (nrei == null)
        {
            nrei = this;
        }

        assert nrei.netRecordingEntryLocal != null;

        return nrei.netRecordingEntryLocal.getRecordingContentItems();
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

        NetRecordingEntryImpl nrei = (NetRecordingEntryImpl) getCDSContentEntry();
        if (nrei == null)
        {
            nrei = this;
        }

        assert nrei.netRecordingEntryLocal != null;

        nrei.netRecordingEntryLocal.removeRecordingContentItem(item);
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
            log.debug("NetRecordingEntryImpl.isInUse() - called");
        }

        // In use is only applicable to local items
        if (isLocal())
        {
            // Make sure this is the real CDS entry, not a proxy resulting from
            // a CDS action
            NetRecordingEntryImpl nrei = (NetRecordingEntryImpl) getCDSContentEntry();
            if (nrei == null)
            {
                nrei = this;
            }

            assert nrei.netRecordingEntryLocal != null;

            return ((IOStatus) nrei.netRecordingEntryLocal).isInUse();
        }

        return false;
    }

    public NetRecordingEntry getNetRecordingEntryLocal()
    {
        return netRecordingEntryLocal;
    }

    /**
     * Deletes this NetRecordingEntry if and only if it contains no RecordingContentItems.
     * Deletes a local NetRecordingEntry only. If the #isLocal method returns false
     * an exception is thrown. 
     *
     * @return True if this NetRecordingEntry was deleted, otherwise returns
     *         false.
     * @throws SecurityException - if the application does not have write
     *               ExtendedFileAccessPermission.
     * @throws IOException - if the NetRecordingEntry is not local.
     */
    public boolean deleteEntry() throws IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug("NetRecordingEntryImpl.deleteEntry() - called");
        }

        boolean state = false;

        if (!isLocal())
        {
            throw new IOException("deleteEntry() called with isLocal() false");
        }

        // Get parent entry
        ContentContainer parent = 
            (ContentContainer) MediaServer.getInstance().getCDS().getRootContainer().getEntry(getParentID());

        if (parent == null)
        {
            // parent is root container
            parent = MediaServer.getInstance().getCDS().getRootContainer();
        }
        state = MediaServer.getInstance().getCDS().removeContentEntry(parent, this, true);

        return state;
    }

}
