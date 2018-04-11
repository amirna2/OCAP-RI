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

package org.cablelabs.lib.utils.oad.hndvr;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.cablelabs.lib.utils.oad.OcapAppDriverCore;
import org.cablelabs.lib.utils.oad.dvr.CompleteRecordings;
import org.cablelabs.lib.utils.oad.dvr.OcapAppDriverDVR;
import org.cablelabs.lib.utils.oad.hn.OcapAppDriverHN;
import org.cablelabs.lib.utils.oad.hn.UPnP;
import org.cablelabs.lib.utils.oad.hn.NetActionHandlerImpl;
import org.dvb.application.AppID;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetActionEvent;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.NetList;
import org.ocap.hn.NetManager;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentEntry;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.ContentResource;
import org.ocap.hn.content.IOStatus;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.content.navigation.ContentList;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.NetRecordingRequestHandler;
import org.ocap.hn.recording.NetRecordingRequestManager;
import org.ocap.hn.recording.NetRecordingSpec;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * This class is collection of OcapAppDriver methods which are specific to HN and DVR.
 * It will implement the HN DVR portion of OcapAppDriver interface.  It provides the
 * HN functionality which is also dependent of DVR.  HN methods which require
 * DVR extension should be included in this class (see OcapAppDriverIntefaceHNDVR).
*/
public class OcapAppDriverHNDVR implements OcapAppDriverInterfaceHNDVR
{
    
    /**
     * The Singleton instance of this class, as type OcapAppDriverInterfaceHNDVR
     */
    private static OcapAppDriverHNDVR s_instance;
    
    private static final long serialVersionUID = -3599127111275002971L;
    private static final Logger log = Logger.getLogger(OcapAppDriverHNDVR.class);
    private static final long HN_ACTION_TIMEOUT = 15000;

    private OcapRecordingRequest m_recordingRequestCurrent;
    private NetRecordingEntry m_netRecordingEntry;
    private RecordingContentItem m_localRecordingItem;
    private OcapAppDriverHN m_oadHN;
    private OcapAppDriverDVR m_oadDVR;
    private OcapAppDriverCore m_oadCore;
    private UPnP m_upnp;
    private NetRecordingRequestManager m_localNetRecordingRequestManager;
    private NetRecordingRequestHandlerImpl m_netRecordingRequestHandler;
    private NetActionRequest m_netActionRequest;
    private NetActionHandlerImpl m_netActionHandler;
    
    // The name of the content container for recordings
    private static final String RECORDING_CONTAINER_NAME = "videos";
    
    // Indicates whether recordings should be published to the root container or
    // the recording container. Default is false
    private boolean m_publishToRoot = false;
    
    // The ID of the Content Container for publishing recordings
    private String m_RecordingContainerID;
    
    // The Content Container for publishing recordings
    private ContentContainer m_recordingContainer;
    
    /**
     * A List of recording log messages pertaining to m_localRecordingItem
     */
    private Vector m_recordingLogMessages;
    
    /**
     * A List of recording log messages pertaining to m_netRecordingEntry;
     */
    private Vector m_CDSRecordingLogMessages;

    /** 
     *  Constants used for ocap application metadata
     */
    private static final String NS_TUNER_INDEX = "ocapApp:tunerIndex";
    private static final String TUNER_INDEX = "tunerIndex";
    private static final String NS_DELAY_DURATION = "ocapApp:delayDuration";
    private static final String DELAY_DURATION = "delayDuration";
    private static final String NS_RECORDING_MODE = "ocapApp:recordingMode";
    private static final String RECORDING_MODE = "recordingMode";
    private static final String NS_REMOTELY_SCHEDULED = "ocapApp:remotelyScheduled";
    private static final String REMOTELY_SCHEDULED = "remotelyScheduled";
    
    private OcapAppDriverHNDVR()
    {
        m_oadCore = (OcapAppDriverCore) OcapAppDriverCore.getOADCoreInterface();
        m_oadHN = (OcapAppDriverHN) OcapAppDriverHN.getOADHNInterface();
        m_oadDVR = (OcapAppDriverDVR) OcapAppDriverDVR.getOADDVRInterface();
        m_upnp = m_oadHN.getUPnP();
        
        m_netRecordingRequestHandler = new NetRecordingRequestHandlerImpl();
        m_recordingLogMessages = new Vector();
        m_CDSRecordingLogMessages = new Vector();
        
        hnFindLocalNetRecordingRequestManager();
    }

    /**
     * Gets an instance of the OcapAppDriverHNDVR, but as a OcapAppDriverInterfaceHNDVR
     * to enforce that all methods be defined in the OcapAppDriverInterfaceHNDVR class.
     * Using lazy initialization of s_instance since the constructor requires multiple
     * parameters making instantiation in class loading not possible.
    */
 
    public static OcapAppDriverInterfaceHNDVR getOADHNDVRInterface()
    {
        if (s_instance == null)
        {
            s_instance = new OcapAppDriverHNDVR();
        }
        return s_instance;
    }
    
    public boolean createScheduledRecording(final int index, final long duration, final long delay, final boolean background)
    {
        if (log.isDebugEnabled())
        {
            log.debug("createScheduledRecording() - creating new recording spec");
        }

        // Convert the provided duration into compliant UPnP format
        StringBuffer scheduledDuration = new StringBuffer().append("P")
                .append((int) (duration / 3600))
                .append(":")
                .append((int) (duration / 60))
                .append(":")
                .append(duration % 60);

        // Convert the provided delay into UPnP SRS format with delay in
        // future(+) or past(-).
        String delaySign = (delay >= 0) ? "+" : "-";
        StringBuffer delayDuration = new StringBuffer().append(delaySign)
                .append("P")
                .append((int) (delay / 3600))
                .append(":")
                .append((int) (delay / 60))
                .append(":")
                .append(duration % 60);

        Date scheduledStartDate = new Date(new Date().getTime() + (delay * 1000));

        /**
         * Make requestSchedule() request
         **/

        NetRecordingSpec spec = new NetRecordingSpec();
        MetadataNode node = spec.getMetadata();

        node.addMetadata("dc:title", "NRE Title");
        node.addMetadata("srs:scheduledChannelID", "scheduledChannelIDParameter");
        node.addMetadata("srs:scheduledChannelID@type", "scheduledChannelID@typeParameter");
        node.addMetadata("srs:@id", "");// must be set to empty string for
        // recordSchedule
        node.addMetadata("srs:title", "titleParameter");
        node.addMetadata("srs:class", "object.recordSchedule.direct.manual");
        node.addMetadata("srs:scheduledStartDateTime",
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(scheduledStartDate));
        node.addMetadata("srs:scheduledDuration", scheduledDuration.toString());
        node.addMetadata("srs:scheduledStartDateTimeAdjust", delayDuration);
        node.addMetadata("ocap:organization", m_oadCore.getOrganization());
        // Pass in the ocap Application specific data. It will be handled if the
        // handler application knows what to do in the notifySchedule()
        // If the values are not available in the request, tunerIndex is
        // assumed to be 0, delayDuration is 0 and background mode is true.
        node.addMetadata(NS_DELAY_DURATION, String.valueOf(delay));
        node.addMetadata(NS_TUNER_INDEX, String.valueOf(index));
        node.addMetadata(NS_RECORDING_MODE, String.valueOf(background));
        node.addMetadata(NS_REMOTELY_SCHEDULED, "true");

        if (m_localNetRecordingRequestManager == null)
        {
            // Attempt to find again
            if (!hnFindLocalNetRecordingRequestManager())
            {
                m_oadHN.waitForLocalContentServerNetModule(HN_ACTION_TIMEOUT);
                if (log.isErrorEnabled())
                {
                    log.error("createScheduledRecording() - Null local net recording request manager");
                }
                resetCurrentRecording();
                return false;
            }
        }
        m_netRecordingRequestHandler.setHNActionTimeoutMS(HN_ACTION_TIMEOUT);
        m_localNetRecordingRequestManager.setNetRecordingRequestHandler(m_netRecordingRequestHandler);

        m_netActionHandler = new NetActionHandlerImpl();
        // Moved the null check before passing this handler
        if (m_netActionHandler == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("createScheduledRecording() - Null local net action handler");
            }
            return false;
        }

        if (log.isInfoEnabled())
        {
            log.info("createScheduledRecording() - initiating requestSchedule with given NRE metadata");
        }
        m_netActionRequest = m_localNetRecordingRequestManager.requestSchedule(spec, m_netActionHandler);

        m_netActionHandler.setNetActionRequest(m_netActionRequest);
        if (!m_netActionHandler.waitRequestResponse(HN_ACTION_TIMEOUT))
        {
            if (log.isErrorEnabled())
            {
                log.error("createScheduledRecording() - didn't get response from request schedule action");
            }
            return false;
        }

        if (log.isDebugEnabled())
        {
            log.debug("createScheduledRecording() - done waiting for response");
        }
        Object obj = m_netActionHandler.getEventResponse(m_netActionHandler.getNetActionEvent());

        if (obj == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("createScheduledRecording() - returned response event was null");
            }
            return false;
        }

        if (!(obj instanceof NetRecordingEntry))
        {
            if (log.isErrorEnabled())
            {
                log.error("NetActionEvent did not contain a NetRecordingEntry");
            }
            return false;
        }

        NetRecordingEntry netRecordingEntry = (NetRecordingEntry) obj;

        try
        {
            if (netRecordingEntry.getRecordingContentItems() == null)
            {
                if (log.isErrorEnabled())
                {
                    log.error("NetActionEvent did not contain a RecordingItems");
                }
                return false;
            }
        }
        catch (IOException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("NetActionEvent did not contain a NetRecordingEntry");
            }
            return false;
        }

        if (log.isDebugEnabled())
        {
            log.debug("createScheduledRecording() - Completed creating recording schedule.");
        }

        return true;

    }
    
    /**
     * Search for the local net recording request manager and save reference
     * when found.
     * 
     * @return  returns true if found, false otherwise
     */
    private boolean hnFindLocalNetRecordingRequestManager()
    {
        boolean wasFound = false;
        
        NetList moduleList = NetManager.getInstance().getNetModuleList(null);
        for (int i = 0; i < moduleList.size(); i++)
        {
            Object obj = moduleList.getElement(i);
            if (obj instanceof NetRecordingRequestManager)
            {
                if (((NetRecordingRequestManager)obj).isLocal())
                {
                    m_localNetRecordingRequestManager = (NetRecordingRequestManager)obj;
                    wasFound = true;
                    if (log.isInfoEnabled())
                    {
                        log.info("Local NetRecordingRequestManager found");
                    }
                }
            }        
        }
        return wasFound;
    }
    //
    /// NetRecordingRequestHandlerImpl
    //
    /// This inner class is used in multiple places to wait for responses to
    /// network-based recording requests (i.e. client/server)
    //
    private class NetRecordingRequestHandlerImpl implements
                  NetRecordingRequestHandler
    {
        private long m_hnActionTimeoutMS = 15000;
        
        public void setHNActionTimeoutMS(long timeoutMS)
        {
            m_hnActionTimeoutMS = timeoutMS;
        }
        
        public boolean notifyDelete(InetAddress arg0, ContentEntry arg1)
        {
            return true;
        }

        public boolean notifyDeleteService(InetAddress arg0, ContentEntry arg1)
        {
            return true;
        }

        public boolean notifyDisable(InetAddress arg0, ContentEntry arg1)
        {
            // TODO Auto-generated method stub
            return true;
        }

        public boolean notifyPrioritization(InetAddress arg0,
                                            NetRecordingEntry[] arg1)
        {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean notifyReschedule(InetAddress arg0,
                                        ContentEntry arg1,
                                        NetRecordingEntry arg2)
        {
            // TODO Auto-generated method stub
            return false;
        }

        /**
         * Creates a RecordingContentItem using MetdataNode contents sent from
         * remote client via requestSchedule(spec);
         */
        private RecordingContentItem createRCI(MetadataNode node)
        {
            // Extract the values from the NRE metadata to create a recording.
            // Get the tuner index
            final String tunerIndex = (node.getMetadata(NS_TUNER_INDEX) == null ? "0"
                    : (String) node.getMetadata(NS_TUNER_INDEX));

            if (log.isDebugEnabled())
            {
                log.debug("The tuner index in NRE metadata is :" + tunerIndex);
            }

            // Get the duration for recording will be returned in milliseconds.
            // This cannot be null as the this is a mandatory property
            final long recordDurationSecs = (((Integer) node.getMetadata("ocap:scheduledDuration")).intValue()) / 1000;

            if (log.isDebugEnabled())
            {
                log.debug("The recording duration in seconds :" + recordDurationSecs);
            }

            // Get the delay that has to be set for recording.
            final String delayDuration = (node.getMetadata(NS_DELAY_DURATION) == null ? "0"
                    : (String) node.getMetadata(NS_DELAY_DURATION));

            if (log.isDebugEnabled())
            {
                log.debug("The recording delay in seconds :" + delayDuration);
            }

            // Get the recording mode if it is background or not.
            final String recordingMode = (node.getMetadata(NS_RECORDING_MODE) == null ? "true"
                    : (String) node.getMetadata(NS_RECORDING_MODE));

            if (log.isDebugEnabled())
            {
                log.debug("The recording mode :" + recordingMode);
            }

            RecordingRequest request;

            // Create the recording as per the NRE metadata
            boolean recordingScheduled = m_oadDVR.recordTuner(Integer.parseInt(tunerIndex),
                    (recordDurationSecs == 0) ? 1 : recordDurationSecs, Long.parseLong(delayDuration), Boolean.valueOf(
                            recordingMode).booleanValue());

            if (!recordingScheduled)
            {
                if(log.isErrorEnabled())
                {
                    log.error("FAILURE : Scheduling recording in notifySchedule()");
                }
                return null;
            }

            RecordingList list = OcapRecordingManager.getInstance().getEntries();

            request = list.getRecordingRequest(list.size() - 1);

            if (log.isDebugEnabled())
            {
                log.debug("Recording request intiated for duration in notifySchedule() with duration : "
                        + recordDurationSecs);
            }

            if (null == request)
            {
                if (log.isInfoEnabled())
                {
                    log.info("FAIL: No current recording");
                }

                return null;
            }

            if ((request instanceof RecordingContentItem) == false)
            {
                if (log.isInfoEnabled())
                {
                    log.info("returned request is not RecordingContentItem!?");
                }

                return null;
            }
            // Reset the current RecordingRequest value
            resetCurrentRecording();

            // Update the RecordingContentItem with the ocap application data
            // that this recording has been scheduled remotely.
            try
            {
                request.addAppData(REMOTELY_SCHEDULED, "true");
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Not able to add app data", e);
                }
            }
            RecordingContentItem rcItem = (RecordingContentItem) request;
            rcItem.getRootMetadataNode().addMetadata(NS_TUNER_INDEX, tunerIndex);
            rcItem.getRootMetadataNode().addMetadata(NS_DELAY_DURATION, delayDuration);
            rcItem.getRootMetadataNode().addMetadata(NS_RECORDING_MODE, recordingMode);
            rcItem.getRootMetadataNode().addMetadata(NS_REMOTELY_SCHEDULED, "true");
            return rcItem;
        }

        /**
         * This is where the NetRecordingEntry is added to the ContentContainer,
         * a RecordingContentItem is created and added both the
         * NetRecordingEntry and the ContentContainer.
         */
        public boolean notifySchedule(InetAddress arg0,
                                      NetRecordingEntry netRecordingEntry)
        {
            // Publishing NetRecordingEntry as restricted item
            m_netRecordingEntry = netRecordingEntry;

            // The NetRecordingEntry must be local to the SRS.
            if (netRecordingEntry.isLocal() == false)
            {
                if (log.isInfoEnabled())
                {
                    log.info("ERROR: netRecordingEntry is not local.");
                }

                return false;
            }

            // Get the MetadataNode that contains the instructions for creating
            // the recording
            MetadataNode node = netRecordingEntry.getRootMetadataNode();

            if (node == null)
            {
                if (log.isErrorEnabled())
                {
                    log.error("ERROR NetRecordingEntry MetadataNode is null.");
                }
                return false;
            }

            // Create the RecordingContentItem from the MetadataNode contents.
            m_localRecordingItem = createRCI(node);
            if (m_localRecordingItem == null)
            {
                return false;
            }

            m_oadHN.waitForLocalContentServerNetModule(m_hnActionTimeoutMS);

            ContentContainer publishContainer = null;
            if (m_publishToRoot)
            {
                publishContainer = m_oadHN.getRootContainer(m_hnActionTimeoutMS);
                if (publishContainer == null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Root container is null");
                    }
                    return false;
                }
            }
            else
            {
                if (m_recordingContainer == null)
                {
                    if (!createVideoContainer(30000))
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info("Unable to create Video ContentContainer");    
                        }
                        return false;
                    }
                }
                publishContainer = m_recordingContainer;
            }

            // Add NetRecordingEntry to CDS
            if (!publishContainer.addContentEntry(netRecordingEntry))
            {
                if (log.isErrorEnabled())
                {
                    log.info("failed to add ContentEntry to Video ContentContainer");
                }
                return false;
            }
            createPublishMessage(netRecordingEntry, netRecordingEntry.getRootMetadataNode());
            
            // Add the RecordingContentItem to the NetRecordingEntry
            // This will create an SRS RecordTask and associate it with the
            // RecordingContentItem.
            try
            {
                netRecordingEntry.addRecordingContentItem(m_localRecordingItem);
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("exception adding RecordingContentItem to " + "NetRecordingEntry. " + e);
                }
                return false;
            }

            // Add RecordingContentItem to CDS
            if (!publishContainer.addContentEntry(m_localRecordingItem))
            {
                if (log.isErrorEnabled())
                {
                    log.error("Error adding RecordingContentItem to Video Container");
                }
                return false;
            }
            createPublishMessage(m_localRecordingItem, m_localRecordingItem.getRootMetadataNode());
            // Call CDS:UpdateObject on the NetRecordingEntry and add a dc:title
            // to the NetRecordingEntry
            final String currentTagValue = "";
            final String nreTitle = "Recording " + netRecordingEntry.getID();

            // Get the mediatype of the RecordingContentItem to update in the
            // title name
            final String mediaType = getMediaType(m_localRecordingItem.getRootMetadataNode());
            final String title = "Recording " + m_localRecordingItem.getID() + " " + mediaType;

            final String idsToUpdate[] = { netRecordingEntry.getID(), m_localRecordingItem.getID() };
            final String titlesToUpdate[] = { nreTitle, title };
            for (int i = 0; i < idsToUpdate.length; i++)
            {
                if (!m_upnp.invokeCdsUpdateObject(idsToUpdate[i], currentTagValue, "<dc:title>" + titlesToUpdate[i]
                        + "</dc:title>"))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("publishRecording() - Could not add <dc:title> to the RecordingContentItem.");
                    }
                    // Ignore error for now, since item is still published
                }
            }

            //Making the object restricted after making updates the object's metadata. 
            netRecordingEntry.getRootMetadataNode().addMetadata("didl-lite:@restricted", "1");

            return true;
        }

        public boolean notifyPrioritization(InetAddress arg0,
                                            RecordingContentItem[] arg1)
        {
            // TODO Auto-generated method stub
            return false;
        }
    }
    
    private void buildMetadata(MetadataNode node, String title, boolean withSRS)
    {
        node.addMetadata("dc:title", title);
        if (withSRS)
        {
            node.addMetadata("srs:scheduledChannelID",
                             "scheduledChannelIDParameter");
            node.addMetadata("srs:scheduledChannelID@type",
                                 "scheduledChannelID@typeParameter");
            node.addMetadata("srs:@id", "");// must be set to empty string for
                                            // recordSchedule
            node.addMetadata("srs:title", "titleParameter");
            node.addMetadata("srs:class", "object.recordSchedule.direct.manual");
            node.addMetadata("srs:scheduledStartDateTime", "2010-07-21T20:02:03");
            node.addMetadata("srs:scheduledDuration", "3");
        }

        node.addMetadata("ocap:organization", m_oadCore.getOrganization());
        node.addMetadata("ocap:scheduledStartDateTime", "2010-07-21T19:00:00");
    }    
    
    public boolean publishRecording(int recordingIndex, long timeoutMS, boolean publishToRoot)
    {
        if (log.isDebugEnabled())
        {
            log.debug("publishRecording() - called with index: " + recordingIndex);
        }
        m_recordingRequestCurrent = getRecordingRequest(recordingIndex);

        if (m_recordingRequestCurrent == null)
        {
            if (log.isInfoEnabled())
            {
                log.info("publishRecording() - didn't find recording at index " + recordingIndex);
            }
            return false;
        }

        //
        // Get Root ContentConainer
        //
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("publishRecording() - getting video container");
            }
            // Create the video Container if it hasn't been created yet
            if (!publishToRoot && m_recordingContainer == null)
            {
                if (!createVideoContainer(timeoutMS))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("publishRecording() - unable to get video container");
                    }
                    return false;
                }
            }
            
            ContentContainer publishContainer;
            
            if (publishToRoot)
            {
                publishContainer = m_oadHN.getRootContainer(timeoutMS);
                if (publishContainer == null)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("publishRecording()- unable to get Root container");
                    }
                    return false;
                }
            }
            else
            {
                publishContainer = m_recordingContainer;
            }
            
            if (m_recordingRequestCurrent instanceof RecordingContentItem)
            {
                // Add the application specific data to the recording metadata.
                m_localRecordingItem = (RecordingContentItem) m_recordingRequestCurrent;
                m_localRecordingItem.getRootMetadataNode().addMetadata(NS_TUNER_INDEX,
                        m_recordingRequestCurrent.getAppData(TUNER_INDEX));
                m_localRecordingItem.getRootMetadataNode().addMetadata(NS_DELAY_DURATION,
                        m_recordingRequestCurrent.getAppData(DELAY_DURATION));
                m_localRecordingItem.getRootMetadataNode().addMetadata(NS_RECORDING_MODE,
                        m_recordingRequestCurrent.getAppData(RECORDING_MODE));
                if (m_recordingRequestCurrent.getAppData(REMOTELY_SCHEDULED) != null)
                {
                    m_localRecordingItem.getRootMetadataNode().addMetadata(NS_REMOTELY_SCHEDULED,
                            m_recordingRequestCurrent.getAppData(REMOTELY_SCHEDULED));
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("OcapRecordingRequest is not a RecordingContentItem");
                }
                return false;
            }
            if (isRemoteScheduledRecording(recordingIndex))
            {
                return publishNetRecordingEntry(timeoutMS, publishContainer);
            }
            else
            {
                if (!publishContainer.addContentEntry(m_localRecordingItem))
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("Error adding RecordingContentItem to CDS");
                    }
                    return false;
                }

                MetadataNode data = m_localRecordingItem.getRootMetadataNode();
                final String mediaType = getMediaType(data);
                final String title = "Recording " + m_localRecordingItem.getID() + " " + mediaType;

                buildMetadata(data, title, false);

                createPublishMessage(m_localRecordingItem, data);
                String recordingURI = null;

                try
                {
                    recordingURI = ((String[]) data.getMetadata("didl-lite:res"))[0];
                }
                catch(NullPointerException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("publishRecording() - Exception", e);
                    }
                }

                if (log.isInfoEnabled())
                {
                    log.info("Published recording URI: " + recordingURI);
                }
            }
            if (log.isDebugEnabled())
            {
                log.debug("publishRecording() - done publishing recording");
            }
        }
        // Creating a finally block as resetCurrentRecording is called in lots
        // of if else blocks.
        finally
        {
            resetCurrentRecording();
        }
        return true;
    }

    /**
     * This method attempts to instantiate the m_recordingContainer Object
     * by creating a new ContentContainer, which is added to the Root container
     * of the CDS.
     * @param timeout the amount of time in milliseconds to wait for the Root
     * container to be obtained
     * @return true if m_recordingContainer has already been created or is successfully
     * created by this method, false if an error occurs trying to create the
     * ContentContainer
     */
    private boolean createVideoContainer(long timeout)
    {
        boolean created = true;
        if (!m_oadHN.waitForLocalContentServerNetModule(timeout))
        {
            if (log.isErrorEnabled())
            {
                log.error("Unable to find local media server");
            }
            return false;
        }
        ContentContainer root = m_oadHN.getRootContainer(timeout);
        if (root == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("Root container is null");
            }
            return false;
        }
        if (root.getEntry(m_RecordingContainerID) != null)
        {
            return true;
        }
        ExtendedFileAccessPermissions efap = 
                new ExtendedFileAccessPermissions
                (true,
                 true,
                 false,
                 false,
                 false,
                 true,
                 null,
                 null);
        created = root.createContentContainer(RECORDING_CONTAINER_NAME, efap);
        if (created)
        {
            Enumeration entries = root.getEntries();
            while (entries.hasMoreElements())
            {
                ContentEntry entry = (ContentEntry)entries.nextElement();
                if (entry instanceof ContentContainer)
                {
                    ContentContainer container = (ContentContainer)entry;
                    if (container.getName().equals(RECORDING_CONTAINER_NAME))
                    {
                        m_recordingContainer = container;
                    }
                }
            }
            m_RecordingContainerID = m_recordingContainer.getID();
            MetadataNode node = m_recordingContainer.getRootMetadataNode();
            node.addMetadata("dc:title", RECORDING_CONTAINER_NAME);
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("Unable to create ContentContainer");
            }
        }
        
        return created;
    }
    
    /**
     * Returns the ContentContainer that is used for storing RecordingContentItems.
     * 
     * @param timeout the amount of time to wait in milliseconds for the 
     * m_recordingContainer to be created if necessary
     * @return the ContentContainer used for storing RecordingContentItems, or null
     * if the ContentContainer could not be created
     */
    protected ContentContainer getVideoContainer(long timeout)
    {
        if (m_recordingContainer == null)
        {
            createVideoContainer(timeout);
        }
        return m_recordingContainer;
    }
    
    /**
     * Returns the media type
     * @return - the media type or the value "unknown" if could not be determined
     */
    private String getMediaType(MetadataNode data)
    {
        Object protocolInfo = data.getMetadata("didl-lite:res@protocolInfo");
        if (protocolInfo instanceof String[])
        {
            final String[] protocolInfos = (String[]) protocolInfo;
            final StringTokenizer tokenizer = new StringTokenizer(protocolInfos[0], ":;");
            while (tokenizer.hasMoreTokens())
            {
                final String token = tokenizer.nextToken();
                if (token.startsWith("DLNA.ORG_PN"))
                {
                    final int index = token.indexOf("=");
                    return token.substring(index + 1, token.length());
                }
            }
        }
        return "unknown";
    }

    private boolean publishNetRecordingEntry(long timeoutMS, final ContentContainer publishContainer)
    {
        if (m_localNetRecordingRequestManager == null)
        {
            // Attempt to find again
            if (!hnFindLocalNetRecordingRequestManager())
            {
                m_oadHN.waitForLocalContentServerNetModule(HN_ACTION_TIMEOUT);
                if (log.isErrorEnabled())
                {
                    log.error("createScheduledRecording() - Null local net recording request manager");
                }
                resetCurrentRecording();
                return false;
            }
        }

        NetRecordingEntry l_nre = null;
        try
        {
            l_nre = m_localNetRecordingRequestManager.createNetRecordingEntry();
        }
        catch (IOException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("publishNetRecordingEntry() - Exception while creating NetRecordingEntry");
            }
        }
        
        if (l_nre == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("publishNetRecordingEntry() - NetRecordingEntry is null");
            }
            return false;
        }
        
        if (log.isDebugEnabled())
        {
            log.debug("publishNetRecordingEntry() - creating new recording spec");
        }
        // 1. Get the current Instance of the RecordingContentItem to be
        // published.
        // 2. Retrieve the NetRecordingEntry object created in the previous step
        // 3. Add NetRecordingEntry to CDS.
        // 4. Add RecordingContentItem to CDS.

        RecordingContentItem l_localRecordingItem = (RecordingContentItem) m_recordingRequestCurrent;

        MetadataNode rciMetaData = l_localRecordingItem.getRootMetadataNode();

        // Update the metadata before adding to CDS.
        // This step is to make sure we recreate NRE for remotely scheduled
        // recordings from the persisted RecordingRequest data.
        if (rciMetaData != null)
        {
            final String scheduledDateTime;
            Date scheduledStartDateTime = (Date) rciMetaData.getMetadata("ocap:scheduledStartDateTime");
            if (scheduledStartDateTime != null)
            {
                // Convert the scheduledStartDateTime to compliant format and add it to NRE Metadata
                scheduledDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(scheduledStartDateTime);
                l_nre.getRootMetadataNode().addMetadata("ocap:scheduledStartDateTime", scheduledDateTime);
                l_nre.getRootMetadataNode().addMetadata("ocap:scheduleStartDateTime", scheduledDateTime);
            }
            l_nre.getRootMetadataNode().addMetadata("ocap:appID", (AppID) rciMetaData.getMetadata("ocap:appID"));
            l_nre.getRootMetadataNode().addMetadata("ocap:scheduledDuration",
                    rciMetaData.getMetadata("ocap:scheduledDuration"));
            l_nre.getRootMetadataNode().addMetadata("ocap:organization",
                    rciMetaData.getMetadata("ocap:organization"));

            l_nre.getRootMetadataNode().addMetadata(NS_TUNER_INDEX,
                    m_recordingRequestCurrent.getAppData(TUNER_INDEX));
            l_nre.getRootMetadataNode().addMetadata(NS_DELAY_DURATION,
                    m_recordingRequestCurrent.getAppData(DELAY_DURATION));
            l_nre.getRootMetadataNode().addMetadata(NS_RECORDING_MODE,
                    m_recordingRequestCurrent.getAppData(RECORDING_MODE));
            l_nre.getRootMetadataNode().addMetadata(NS_REMOTELY_SCHEDULED,
                    m_recordingRequestCurrent.getAppData(REMOTELY_SCHEDULED));
        }
        // Add NetRecordingEntry to CDS
        if (!publishContainer.addContentEntry(l_nre))
        {
            if (log.isErrorEnabled())
            {
                log.error("Error adding NetRecordgingItem to CDS");
            }
            return false;
        }
        
        // After publishing it to CDS, get the ID and then add dc:title
        l_nre.getRootMetadataNode().addMetadata("dc:title", "NRE Title for Recording " + l_nre.getID());

        // Add NetRecordingEntry publish message
        createPublishMessage(l_nre, l_nre.getRootMetadataNode());

        // Add RecordingContentItem to CDS
        if (!publishContainer.addContentEntry(l_localRecordingItem))
        {
            if (log.isErrorEnabled())
            {
                log.error("Error adding RecordingContentItem to CDS");
            }
            return false;
        }

        // Add RecordingContentItem publish message
        createPublishMessage(l_localRecordingItem, l_localRecordingItem.getRootMetadataNode());

        final String currentTagValue = "";

        // Get the mediatype of the RecordingContentItem to update in the
        // title name
        final String mediaType = getMediaType(l_localRecordingItem.getRootMetadataNode());
        final String title = "Recording " + l_localRecordingItem.getID() + " " + mediaType;

        final String idsToUpdate[] = { l_localRecordingItem.getID() };
        final String titlesToUpdate[] = { title };

        // Call CDS:UpdateObject on the RecordingContentItem and add a dc:title
        for (int i = 0; i < idsToUpdate.length; i++)
        {
            if (!m_upnp.invokeCdsUpdateObject(idsToUpdate[i], currentTagValue, "<dc:title>" + titlesToUpdate[i]
                    + "</dc:title>"))
            {
                if (log.isInfoEnabled())
                {
                    log.info("publishRecording() - Could not add <dc:title> to the RecordingContentItem.");
                }
                // Ignore error for now, since item is still published
            }
        }

        // Make the NetRecordingEntry restricted at the last step
        l_nre.getRootMetadataNode().addMetadata("didl-lite:@restricted", "1");

        if (log.isDebugEnabled())
        {
            log.debug("publishRecording() - updating publishing messages");
        }

        return true;
    }
    
    public boolean publishAllRecordings(long timeoutMS, boolean publishToRoot)
    {
        m_oadHN.clearPublishedContent();
        boolean retVal = false;
        RecordingList list = OcapRecordingManager.getInstance().getEntries(
                                                new CompleteRecordings());
        if (list.size() <= 0)
        {
            if (log.isInfoEnabled())
            {
                log.info("No recordings to publish!");
            }
        }
        else
        {
            // if we have any recordings to publish, start with true...
            retVal = true;

            for (int i = 0; i < list.size(); i ++)
            {
                // Remote scheduled recordings are automatically published so
                // only publish local recordings
                if (!isRemoteScheduledRecording(i))
                {
                    if (publishRecording(i, timeoutMS, publishToRoot) == false)
                    {
                        // if any recording fails to publish, return false.
                        retVal = false;
                    }
                }
            }
        }
        return retVal;
    }

    private OcapRecordingRequest getRecordingRequest(int recordingIndex)
    {
        RecordingList list = OcapRecordingManager.getInstance().getEntries(
                                                new CompleteRecordings());
        if (log.isDebugEnabled())
        {
            log.debug("getRecordingRequest() - got list");
        }
        if (list == null)
        {
            if (log.isErrorEnabled())
            {
                log.error("getRecordingRequest() - Unable to get list of recordings");
            }
            return null;            
        }

        if (log.isDebugEnabled())
        {
            log.debug("getRecordingRequest() - getting request");
        }
        OcapRecordingRequest orr =
            (OcapRecordingRequest) list.getRecordingRequest(recordingIndex);

        if (log.isDebugEnabled())
        {
            log.debug("getRecordingRequest() - got request "+orr);
        }
        return orr;
    }
    
    public boolean isRemoteScheduledRecording(int recordingIndex)
    {
        OcapRecordingRequest orr = getRecordingRequest(recordingIndex);
        if (orr == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("OcapRecordingRequest null for the recordingIndex passed.");
            }
            return false;
        }
        if (orr.getAppData(REMOTELY_SCHEDULED) != null
                && ((String) orr.getAppData(REMOTELY_SCHEDULED)).equalsIgnoreCase("true"))
        {
            if (log.isDebugEnabled())
            {
                log.debug("This recording has been remotely scheduled");
            }
            return true;
        }

        if (log.isDebugEnabled())
        {
            log.debug("This recording has been locally scheduled");
        }
        return false;
    }
    
    public void logRecordingInUse()
    {
        String message = "";
        IOStatus ios = null;
        if (null != m_localRecordingItem)
        {
            ios = (IOStatus) m_localRecordingItem;
            message = "Recording Content Item In Use? " + ios.isInUse();
            m_recordingLogMessages.add(message);
            if (log.isInfoEnabled())
            {
                log.info(message);
            }
            ContentResource resources[] = m_localRecordingItem.getResources();
            for (int i = 0; i < resources.length; i++)
            {
                ios = (IOStatus) resources[i];
                message = "Recording Content Item Resource " + (i + 1) + " In Use? " + ios.isInUse();
                m_recordingLogMessages.add(message);
                if (log.isInfoEnabled())
                {
                    log.info(message);
                }
            }
        }
        if (null != m_netRecordingEntry)
        {
            ios = (IOStatus) m_netRecordingEntry;
            message = "Net Recording Entry In Use? " + ios.isInUse();
            m_recordingLogMessages.add(message);
            if (log.isInfoEnabled())
            {
                log.info(message);
            }

            try
            {
                ContentContainer cc = m_netRecordingEntry.getEntryParent();
                if (null != cc)
                {
                    ios = (IOStatus) cc;
                    message = "Net Recording Container In Use? " + ios.isInUse();
                    m_recordingLogMessages.add(message);
                    if (log.isInfoEnabled())
                    {
                        log.info(message);
                    }
                }
            }
            catch (Throwable t)
            {
                if (log.isInfoEnabled())
                {
                    log.info(t);
                }
            }
        }
        else
        {
            message = "Recording Item is NULL";
            m_recordingLogMessages.add(message);
            if (log.isInfoEnabled())
            {
                log.info(message);
            }
        }
    }
    
    public void logRecordingInUseViaCDS(long timeoutMS)
    {
        String message = "";
        IOStatus ios = null;
        ContentServerNetModule localMediaServer = m_oadHN.getLocalContentServerNetModule();
        if (localMediaServer != null)
        {
            if (log.isInfoEnabled())
            {
                log.info("Creating new Net Action Handler");
            }
            NetActionHandlerImpl netActionHandler = new NetActionHandlerImpl();

            m_netActionRequest = localMediaServer.requestSearchEntries("0", "*", 0, 0, null, "", netActionHandler);
            if (!netActionHandler.waitRequestResponse(timeoutMS))
            {
                if (log.isInfoEnabled())
                {
                    log.info("NetRecordingRequestManager.requestSchedule() response failed. Reason: "
                        + netActionHandler.getFailReason());
                }
                return;
            }

            Object resp = getResponseFromEvent(netActionHandler.getNetActionEvent());
            if (resp == null)
            {
                if (log.isInfoEnabled())
                {
                    log.info("Could not get Response from event.");
                }
                return;
            }
            RecordingContentItem rci = null;
            NetRecordingEntry nre = null;
            Object obj = netActionHandler.getNetActionEvent().getResponse();
            ContentList list = null;
            if (obj instanceof ContentList)
            {
                list = (ContentList) netActionHandler.getNetActionEvent().getResponse();
                if (list == null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Could not get list from event.");
                    }
                    return;
                }
                if (log.isInfoEnabled())
                {
                    log.info("Response was a Content List with " + list.size() + " entries");
                }
                while (list.hasMoreElements())
                {
                    obj = list.nextElement();
                    if (obj instanceof RecordingContentItem)
                    {
                        rci = (RecordingContentItem) obj;
                    }
                    if (obj instanceof NetRecordingEntry)
                    {
                        nre = (NetRecordingEntry) obj;
                    }
                }
            }
            else if (obj instanceof NetRecordingEntry)
            {
                nre = (NetRecordingEntry) obj;
                if (log.isInfoEnabled())
                {
                    log.info("Response was a NetRecordingEntry");
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("FAILED - Returned response was not expected class: " + obj.getClass().getName());
                }
                return;
            }

            if (null != rci)
            {
                ios = (IOStatus) rci;
                message = "Recording Content Item In Use? " + ios.isInUse();
                m_CDSRecordingLogMessages.add(message);
                if (log.isInfoEnabled())
                {
                    log.info(message);
                }
                ContentResource resources[] = rci.getResources();
                for (int i = 0; i < resources.length; i++)
                {
                    ios = (IOStatus) resources[i];
                    message = "Recording Content Item Resource " + (i + 1) + " In Use? " + ios.isInUse();
                    m_CDSRecordingLogMessages.add(message);
                    if (log.isInfoEnabled())
                    {
                        log.info(message);
                    }
                }

                // Get the container of rci
                m_netActionRequest = localMediaServer.requestBrowseEntries(rci.getParentID(), "*", false, 0, 1, "",
                        netActionHandler);
                if (!netActionHandler.waitRequestResponse(timeoutMS))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("NetRecordingRequestManager.requestSchedule() response failed. Reason: "
                            + m_netActionHandler.getFailReason());
                    }
                    return;
                }

                resp = getResponseFromEvent(netActionHandler.getNetActionEvent());
                if (resp == null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Could not get Response from event.");
                    }
                    return;
                }
                list = (ContentList) resp;
                ContentContainer cc = (ContentContainer) list.nextElement();
                ios = (IOStatus) cc;
                message = "Recording Content Item Container In Use? " + ios.isInUse();
                m_CDSRecordingLogMessages.add(message);
                if (log.isInfoEnabled())
                {
                    log.info(message);
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("FAILED - Unable to find Recording Content Item");
                }
            }
            if (null != nre)
            {
                ios = (IOStatus) nre;
                message = "Net Recording Entry In Use? " + ios.isInUse();
                m_CDSRecordingLogMessages.add(message);
                if (log.isInfoEnabled())
                {
                    log.info(message);
                }

                // Get the container of nre
                m_netActionRequest = localMediaServer.requestBrowseEntries(nre.getParentID(), "*", false, 0, 1, "",
                        netActionHandler);
                if (!netActionHandler.waitRequestResponse(timeoutMS))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("NetRecordingRequestManager.requestSchedule() response failed. Reason: "
                            + netActionHandler.getFailReason());
                    }
                    return;
                }

                resp = getResponseFromEvent(netActionHandler.getNetActionEvent());
                if (resp == null)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info("Could not get Response from event.");
                    }
                    return;
                }
                list = (ContentList) resp;
                ContentContainer cc = (ContentContainer) list.nextElement();
                ios = (IOStatus) cc;
                message = "Net Recording Entry Container In Use? " + ios.isInUse();
                m_CDSRecordingLogMessages.add(message);
                if (log.isInfoEnabled())
                {
                    log.info(message);
                }
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("FAILED - Net Recording Entry is NULL");
                }
            }
        }
    }
    
    protected Object getResponseFromEvent(NetActionEvent event)
    {
        String failReason = "";
        if (event == null)
        {
            return null;
        }

        NetActionRequest receivedNetActionRequest = event.getActionRequest();
        int receivedActionStatus = event.getActionStatus();
        int receivedError = event.getError();
        Object receivedResponse = event.getResponse();

        // check that request is same as sent
        if (receivedNetActionRequest != m_netActionRequest) // compare references
                                                          // only
        {
            if (log.isInfoEnabled())
            {
                log.info("Recieved NetActionRequest is not the same as the request return value.");
            }
            return null;
        }

        // Check action status
        if (receivedActionStatus != NetActionEvent.ACTION_COMPLETED)
        {
            failReason = "The NetActionRequest returned ActionStatus = ";

            if (receivedActionStatus == NetActionEvent.ACTION_CANCELED)
            {
                failReason += "ACTION_CANCELED ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_FAILED)
            {
                failReason += "ACTION_FAILED ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_IN_PROGRESS)
            {
                failReason += "ACTION_IN_PROGRESS ";
            }
            else if (receivedActionStatus == NetActionEvent.ACTION_STATUS_NOT_AVAILABLE)
            {
                failReason += "ACTION_STATUS_NOT_AVAILABLE ";
            }
            else
            {
                failReason += "UNKONWN ACTION STATUS value=" + receivedActionStatus;
            }

            failReason += ", Error value " + String.valueOf(receivedError);
            
            if (log.isInfoEnabled())
            {
                log.info(failReason);
            }
            return null;
        }

        return receivedResponse;
    }
    
    public int getNumRecordingLogUses()
    {
        return m_recordingLogMessages.size();
    }
    
    public String getRecordingLogMessage(int index)
    {
        if (index < m_recordingLogMessages.size())
        {
            return (String)m_recordingLogMessages.remove(index);
        }
        return null;
    }
    
    public int getNumCDSRecordingLogUses()
    {
        return m_CDSRecordingLogMessages.size();
    }
    
    public String getCDSRecordingLogMessage(int index)
    {
        if (index < m_CDSRecordingLogMessages.size())
        {
            return (String)m_CDSRecordingLogMessages.remove(index);
        }
        return null;
    }

    public boolean unPublishAllRecordings()
    {
        int localIdx = m_oadHN.findLocalMediaServer();
        if (localIdx != -1)
        {
            // If recordings were published with SRS, it's necessary to get the
            // RootContainer in order to remove the NetRecordingEntries
            ContentContainer root = m_oadHN.getRootContainer(30000);
            if (root == null)
            {
                // Logging at info level, since this situation only occurs
                // intermittently and may not occur again when running at DEBUG
                // level logging. Also helps with diagnosing issues from nightly
                // Rx script results. OCORI-5055
                if (log.isInfoEnabled())
                {
                    log.info("unpublishAllRecordings() - root container is null");
                }
                return false;
            }
            Enumeration entries = root.getEntries();
            if (entries != null)
            {
                while (entries.hasMoreElements())
                {
                    Object nextEntry = entries.nextElement();
                    if (nextEntry instanceof ContentEntry)
                    {
                        ContentEntry ce = (ContentEntry) nextEntry;
                        if (ce instanceof ContentContainer)
                        {
                            if (!unPublishContentContainer(ce))
                            {
                                if (log.isErrorEnabled())
                                {
                                    log.error("unPublishAllRecordings() - error during unPublishContentContainer, exception: ");
                                }
                                return false;
                            }
                        }
                        else if (ce instanceof NetRecordingEntry)
                        {
                            if (!unPublishNetRecordingEntries(ce))
                            {
                                if (log.isErrorEnabled())
                                {
                                    log.error("unPublishAllRecordings() - error during unPublishNetRecordingEntries, exception: ");
                                }
                                return false;
                            }
                        }
                        else if (ce instanceof ContentItem)
                        {
                            if (!unPublishContentItem(ce))
                            {
                                if (log.isErrorEnabled())
                                {
                                    log.error("unPublishAllRecordings() - error during unPublishContentItem, exception: ");
                                }
                                return false;
                            }
                        }
                    }
                }
            }
            resetVideoContainer();
        }

        return true;
    }

    /**
     * Deletes the ContentItem from CDS
     * 
     * @param ce
     *            - ContentEntry to be unPublished from CDS
     * @return true if unpublished successfully, else false
     */
    private boolean unPublishContentItem(ContentEntry ce)
    {
        if (ce instanceof RecordingContentItem)
        {
            try
            {
                // Delete the item
                ce.deleteEntry();
            }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("unPublishContentItem() - error deleting recordings, exception: ", e);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Deletes the ContentContainer and its contents from CDS
     * 
     * @param ce
     *            - ContentEntry to be unPublished from CDS
     * @return true if unpublished successfully, else false
     */
    private boolean unPublishContentContainer(ContentEntry ce)
    {
        Enumeration containerItems = ((ContentContainer) ce).getEntries();
        if (containerItems != null)
        {
            while (containerItems.hasMoreElements())
            {
                Object nextEntry = containerItems.nextElement();
                if (nextEntry instanceof ContentEntry)
                {
                    ContentEntry cEntry = (ContentEntry) nextEntry;
                    if (cEntry instanceof NetRecordingEntry)
                    {
                        if (!unPublishNetRecordingEntries(cEntry))
                        {
                            if (log.isErrorEnabled())
                            {
                                log.error("unPublishContentContainer() - error during unPublishNetRecordingEntries, exception: ");
                            }
                            return false;
                        }
                    }
                    else if (cEntry instanceof ContentContainer)
                    {
                        if (!unPublishContentContainer(cEntry))
                        {
                            if (log.isErrorEnabled())
                            {
                                log.error("unPublishContentContainer() - error during unPublishContentContainer, exception: ");
                            }
                            return false;
                        }
                    }
                }
            }
            try
            {
                // Delete the ContentContainer
                ce.deleteEntry();
            }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("unPublishRecordings() - error deleting recordings, exception: ", e);
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Deletes the NetRecordingEntry and its associated RecordingContentItems
     * from CDS
     * 
     * @param ce
     *            - ContentEntry to be unPublished from CDS
     * @return true if unpublished successfully, else false
     */
    private boolean unPublishNetRecordingEntries(ContentEntry ce)
    {
        try
        {
            RecordingContentItem[] rcis = ((NetRecordingEntry) ce).getRecordingContentItems();
            for (int idx = 0; idx < rcis.length; idx++)
            {
                if (!rcis[idx].deleteEntry())
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("unPublishAllRecordings() - RecordingContentItem.deletEntry() returned false.");
                    }
                    return false;
                }
            }
            if (!ce.deleteEntry())
            {
                if (log.isErrorEnabled())
                {
                    log.error("unPublishAllRecordings() - NetRecordingEntry.deletEntry() returned false.");
                }
                return false;
            }
        }
        catch (IOException e)
        {
            if (log.isErrorEnabled())
            {
                log.error("unPublishAllRecordings() - Exception unpublishing recording", e);
            }
            return false;
        }
        return true;
    }

    /**
     * Resets the Video Container variables.
     */
    private void resetVideoContainer()
    {
        m_recordingContainer = null;
        m_RecordingContainerID = null;
    }

    // Reset the current RecordingRequest
    public void resetCurrentRecording()
    {
        if (m_recordingRequestCurrent != null)
        {
            m_recordingRequestCurrent = null;
        }
    }
    
    /**
     * Creates the message which reports info about recording that was published
     * 
     * @param item
     *            item which was published
     * @param data
     *            item's metadata
     */
    private void createPublishMessage(ContentEntry item, MetadataNode data)
    {
        if (item instanceof RecordingContentItem)
        {
            final String uriId = m_oadHN.getURIId(data);
            final String mediaType = getMediaType(data);
            final String title = "Recording " + item.getID() + " " + mediaType;
            m_oadHN.addPublishedContent("ID[" + item.getID() + "] " + "Title: " + title + ", URI ID: " + uriId);
        }
        else if (item instanceof NetRecordingEntry)
        {
            final String title = "NetRecordginEntry " + item.getID();
            m_oadHN.addPublishedContent("ID[" + item.getID() + "] " + "Title: " + title);
        }
    }
}

