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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.media.Time;

import org.apache.log4j.Logger;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.ocap.hn.ContentServerNetModuleImpl;
import org.cablelabs.impl.ocap.hn.NetActionEventImpl;
import org.cablelabs.impl.ocap.hn.NetActionRequestImpl;
import org.cablelabs.impl.ocap.hn.NetModuleActionInvocation;
import org.cablelabs.impl.ocap.hn.content.ContentContainerImpl;
import org.cablelabs.impl.ocap.hn.content.ContentItemImpl;
import org.cablelabs.impl.ocap.hn.content.ContentResourceExt;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.transformation.Transformable;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.util.HNUtil;
import org.ocap.hn.Device;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.IOStatus;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.upnp.client.UPnPActionResponseHandler;
import org.ocap.hn.upnp.client.UPnPClientService;
import org.ocap.hn.upnp.common.UPnPAction;
import org.ocap.hn.upnp.common.UPnPActionInvocation;
import org.ocap.hn.upnp.common.UPnPActionResponse;
import org.ocap.hn.upnp.common.UPnPResponse;

/**
 * This class implements the org.ocap.hn.recording.RecordingContentItem
 * interface. It represents a RecordingContentItem (RecordTask) on a remote SRS
 * and CDS.
 * 
 */
public class RecordingContentItemImpl extends ContentItemImpl 
    implements RecordingContentItem, UPnPActionResponseHandler, Transformable
{
    private Object m_sync = new Object();

    private static final String PRESENTATION_POINT_START_TAG = "<" + PROP_PRESENTATION_POINT + ">";

    private static final String PRESENTATION_POINT_END_TAG = "</" + PROP_PRESENTATION_POINT + ">";
    
    private final String m_logPrefix = "RCII 0x" + Integer.toHexString(this.hashCode()) + ": ";
    
    /**
     * The RecordingContentItemLocal associated with this
     * RecordingContentItemImpl, if any.
     * 
     * Note: Changes to the "local" RecordingContentItem need to be delegated to this 
     * reference since there may be multiple/discreet instantiations of RecordingContentItemImpl. 
     * But there is only one RecordingContentItemLocal/OcapRecordingRequest that manages the
     * state (and metadata) for the local RecordingContentItem.
     */
    private final LocalRecordingContentItem localRecordingContentItem;

    /**
     * Construct a server-side object of this class.
     * 
     * @param localRecordingContentItem
     *            The local RecordingContentItem associated with this RecordingContentItemImpl.
     * @param metadataNode
     *            The metadata node representing this piece of content.
     */
    public RecordingContentItemImpl(LocalRecordingContentItem localRecordingContentItem, MetadataNodeImpl metadataNode)
    {
        super(metadataNode);
        this.localRecordingContentItem = localRecordingContentItem;
    }

    public RecordingContentItemImpl(UPnPAction action, MetadataNodeImpl metadataNode)
    {
        super(action, metadataNode);
        final ContentEntry cdsContentEntry = getCDSContentEntry();
        
        if (cdsContentEntry instanceof RecordingContentItemImpl)
        { // This RecordingContentItem is a representation of a local RecordingContentItem
          // Connect it to the real local RecordingContentItem
            this.localRecordingContentItem = ((RecordingContentItemImpl)cdsContentEntry)
                                                .getLocalRecordingContentItem();
        }
        else
        { // This RecordingContentItem is a remote RecordingContentItem (or is local non- 
          //  RecordingContentItem?)
            this.localRecordingContentItem = null;
        }
    }

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(RecordingContentItemImpl.class);

    /*
     * This is the remote representation for a RecordingContentItem. There will
     * be no associated NetRecordingEntry for instances of these.
     */
    public NetRecordingEntry getRecordingEntry()
    {
        return null;
    }

    /**
     * This returns the ObjectID of the NetRecordingEntry which contains this
     * recording content item. This is the ObjectID of NetRecordingEntry
     * contained in the CDS.
     */
    public String getRecordingEntryID()
    {
        return (String) m_metadataNode.getMetadata(PROP_NET_RECORDING_ENTRY);
    }

    public NetActionRequest requestConflictingRecordings(NetActionHandler handler)
    {
        RecordingNetModuleImpl recordingNetModule = getRecordingNetModuleImpl();

        if (recordingNetModule == null) // didn't find it
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "requestConflictingRecordings() - Could not find RecordingNetModuleImpl in the Device that contains this RecordingNetModule");
            }

            return null;
        }

        UPnPAction action = recordingNetModule.getActionByName(RecordingActions.GET_RECORD_TASK_CONFLICTS_ACTION_NAME);

        // get the RecordTaskID from the MetadataNode
        String recordTaskID = (String) m_metadataNode.getMetadata(RecordingActions.RECORD_TASK_ID_KEY);

        GetRecordTaskConflictsRequest request = new GetRecordTaskConflictsRequest(handler, this.getServer());
        UPnPActionInvocation invocation = new NetModuleActionInvocation(new String[] { recordTaskID }, action, request);
        // create request
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Calling doAction for requestConflictingRecordings");
        }

        // do the action
        UPnPClientService service = (UPnPClientService)action.getService();
        service.postActionInvocation(invocation, request);

        return request;
    }

    /**
     * Requests that the presentation point of this recording be updated.
     * 
     * @param time
     *            The presentation point of this recording.
     * 
     * @param handler
     *            The NetActionHandler which gets informed once this request
     *            completes.
     * 
     * @return NetActionRequest which can be used to monitor asynchronous action
     *         progress
     */
    public NetActionRequest requestSetMediaTime(Time time, NetActionHandler handler)
    {
        if (time == null)
        {
            if (log.isErrorEnabled())
            {
                log.error(m_logPrefix + "requestSetMediaTime(): time is null");
            }

            return null;
        }

        Object currentPP = m_metadataNode.getMetadataRegardless(UPnPConstants.QN_OCAP_MEDIA_PRESENTATION_POINT);

        String newPP = Long.toString(time.getNanoseconds() / 1000000);

        String currentTagValue = currentPP != null ? PRESENTATION_POINT_START_TAG + currentPP
                + PRESENTATION_POINT_END_TAG : "";

        String newTagValue = PRESENTATION_POINT_START_TAG + newPP + PRESENTATION_POINT_END_TAG;

        ContentServerNetModuleImpl csnm = (ContentServerNetModuleImpl) getServer();

        UPnPAction action = csnm.getActionByName(ContentDirectoryService.UPDATE_OBJECT);
        ActionRequest request = new ActionRequest(handler);
        UPnPActionInvocation invocation = new NetModuleActionInvocation(new String[] {
                getID(), 
                currentTagValue, 
                newTagValue }, 
                action, request);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Calling doAction for requestSetMediaTime");
        }

        // do the action
        UPnPClientService service = (UPnPClientService)action.getService();
        service.postActionInvocation(invocation, this);

        return request;
    }

    /**
     * Get the RecordingContentItemLocal that contains this
     * RecordingContentItemImpl, if any.
     * <p>
     * 
     * @return The RecordingContentItemLocal that contains this
     *         RecordingContentItemImpl, if any; else null.
     */
    public LocalRecordingContentItem getLocalRecordingContentItem()
    {
        return localRecordingContentItem;
    }

    //
    // ContentItem/ContentEntry Overrides
    //
    
    // Override
    public void notifyUPnPActionResponse(UPnPResponse response)
    {        
        NetModuleActionInvocation invocation = (NetModuleActionInvocation)response.getActionInvocation();
          
        NetActionRequestImpl request = (NetActionRequestImpl)invocation.getNetActionRequest();
        request.notifyHandler(new NetActionEventImpl(request, response, response.getHTTPResponseCode(), 
                response instanceof UPnPActionResponse ? NetActionEvent.ACTION_COMPLETED : NetActionEvent.ACTION_FAILED));        
    }

    // Override
    public boolean isLocal()
    {
        return (localRecordingContentItem != null);
    }
    
    // Override
    public boolean isInUse()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "RecordingContentItemImpl.isInUse() - called");
        }               

        // In use is only applicable to local items
        if (localRecordingContentItem != null)
        {
            return ((IOStatus) localRecordingContentItem).isInUse();
        }

        return false;
    }
    
    // Override
    public void wasModifiedRemotely()
    {
        final String logPrefix = m_logPrefix + "RecordingContentItemImpl.wasModifiedRemotely(): ";
        if (log.isDebugEnabled())
        {
            log.debug(logPrefix + "called");
        }

        // The only recording metadata that has a side-effect is the the OCAP mediaPresentationPoint
        Object currentPP = m_metadataNode.getMetadataRegardless(UPnPConstants.QN_OCAP_MEDIA_PRESENTATION_POINT);

        Long msecObj = HNUtil.toLong(currentPP);
        
        // Do nothing if the property isn't present or the new value cannot be converted to 
        // a valid millisecond value.
        if ((msecObj == null) || (msecObj.longValue() < 0))
        {
            if (log.isInfoEnabled())
            {
                log.info(logPrefix + "cannot convert " + UPnPConstants.QN_OCAP_MEDIA_PRESENTATION_POINT + 
                                    " value of " +  currentPP + " to milliseconds");
            }

            return;
        }
        
        // OK, we have a valid presentation point value - delegate the set to the RCIL
        if (localRecordingContentItem == null)
        {
            if (log.isWarnEnabled())
            {
                log.warn(logPrefix + "No local RecordingContentItem!");
            }
            return;
        }
        
        localRecordingContentItem.setPlaybackMediaTime(msecObj.longValue());
    }
    
    // Override
    public boolean deleteEntry() throws IOException
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "RecordingContentItemImpl.deleteEntry() - called");
        }
        
        boolean itemDeleted = super.deleteEntry();
        
        boolean removedNRE = true;
        if (localRecordingContentItem != null)
        { // Delegate to the real RecordingContentItem
            try
            {
                removedNRE = localRecordingContentItem.removeFromNetRecordingEntry();
            } 
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Error removing NetRecordingEntry", e);
                }
                removedNRE = false; 
            }
        }

        return itemDeleted;
    }

    // Override
    public boolean deleteResources()
    {
        // Per RecordingContentItem.deleteEntry() javadoc: 
        //   Deletes this RecordingContentItem, but does not remove the physical recording.
        m_deleted = true; // Treat either the deletion of the CI or its resources as "deleted"

        return true;
    }
    
    // Override
    public File getContentFile()
    {
        return null;
    }

    // Override
    public HNStreamProtocolInfo[] getProtocolInfo()
    {
        if (localRecordingContentItem != null)
        { // Delegate to the real RecordingContentItem
            return localRecordingContentItem.getProtocolInfo();
        }
        else
        {
            return super.getProtocolInfo();
        }
    }
    
    // Override
    protected ContentResourceExt[] getContentResourceList()
    {
        if (localRecordingContentItem != null)
        { // Delegate to the real RecordingContentItem
            return localRecordingContentItem.getContentResourceList();
        }
        else
        {
            return super.getContentResourceList();
        }
    }

    /**
     * Find the RecordingNetModule (SRS)
     */
    private RecordingNetModuleImpl getRecordingNetModuleImpl()
    {
        Device device = this.getDevice(); // get the Device for this
                                          // RecordingContentItem
        NetList netList = device.getNetModuleList(); // get the NetModules in
                                                     // the Device
        Enumeration enumeration = netList.getElements();
        RecordingNetModuleImpl recordingNetModuleImpl = null;

        while (enumeration.hasMoreElements())
        {
            Object obj = enumeration.nextElement();

            if (obj instanceof RecordingNetModuleImpl)
            {
                recordingNetModuleImpl = (RecordingNetModuleImpl) obj;
                break; // found it
            }
        }

        return recordingNetModuleImpl;
    }

    //
    // Implementation of the Transformable interface
    //
    
    public void setTransformations(final List transformations)
    {
        final LocalRecordingContentItem lrci = getLocalRecordingContentItem();
        if (lrci != null)
        { // Delegate to the "real" RecordingContentItem - the one that manages the resources
          //  and metadata (RecordingContentItemLocal)
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "setTransformations: Delegating to the LocalRecordingContentItem: " + lrci);
            }
            lrci.setTransformations(transformations);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "setTransformations: No LocalRecordingContentItem - nothing to do");
            }
        }
    }
    
    public List getTransformations()
    {
        final LocalRecordingContentItem lrci = getLocalRecordingContentItem();
        if (lrci != null)
        { // Delegate to the "real" RecordingContentItem - the one that manages the resources
          //  and metadata (RecordingContentItemLocal)
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "getTransformations: Delegating to the LocalRecordingContentItem: " + lrci);
            }
            return lrci.getTransformations();
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "getTransformations: No LocalRecordingContentItem - nothing to do");
            }
            return new ArrayList(0);
        }
    }

    public String toString()
    {
        return m_logPrefix + "local 0x" 
               + ((localRecordingContentItem==null) ? "0" : Integer.toHexString(localRecordingContentItem.hashCode()));  
    }
}
