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

import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.media.Time;
import javax.tv.locator.LocatorFactory;
import javax.tv.service.SIManager;
import javax.tv.service.SIRequestFailureType;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;

import org.apache.log4j.Logger;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.manager.RecordingDBManager;
import org.cablelabs.impl.manager.recording.OcapRecordedServiceExt;
import org.cablelabs.impl.manager.recording.RecordingImpl;
import org.cablelabs.impl.manager.recording.RecordingManagerInterface;
import org.cablelabs.impl.media.mpe.HNAPIImpl;
import org.cablelabs.impl.media.mpe.MPEMediaError;
import org.cablelabs.impl.media.streaming.exception.HNStreamingException;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescription;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentDescriptionLocalSV;
import org.cablelabs.impl.media.streaming.session.data.HNStreamContentLocationType;
import org.cablelabs.impl.media.streaming.session.data.HNStreamProtocolInfo;
import org.cablelabs.impl.media.streaming.session.util.StreamUtil;
import org.cablelabs.impl.ocap.hn.UsageTrackingContentItem;
import org.cablelabs.impl.ocap.hn.content.ContentContainerImpl;
import org.cablelabs.impl.ocap.hn.content.ContentItemImpl;
import org.cablelabs.impl.ocap.hn.content.ContentResourceExt;
import org.cablelabs.impl.ocap.hn.content.ContentResourceImpl;
import org.cablelabs.impl.ocap.hn.content.MetadataNodeImpl;
import org.cablelabs.impl.ocap.hn.recording.LocalRecordingContentItem;
import org.cablelabs.impl.ocap.hn.recording.LocalRecordingContentItemResource;
import org.cablelabs.impl.ocap.hn.recording.RecordScheduleDirectManual;
import org.cablelabs.impl.ocap.hn.recording.RecordingActions;
import org.cablelabs.impl.ocap.hn.recording.RecordingContentItemImpl;
import org.cablelabs.impl.ocap.hn.transformation.NativeContentTransformation;
import org.cablelabs.impl.ocap.hn.transformation.OutputVideoContentFormatExt;
import org.cablelabs.impl.ocap.hn.transformation.TransformationImpl;
import org.cablelabs.impl.ocap.hn.transformation.TransformationManagerImpl;
import org.cablelabs.impl.ocap.hn.upnp.MediaServer;
import org.cablelabs.impl.ocap.hn.upnp.UPnPConstants;
import org.cablelabs.impl.ocap.hn.upnp.cds.ContentDirectoryService;
import org.cablelabs.impl.ocap.hn.upnp.cds.Utils;
import org.cablelabs.impl.recording.RecordingInfo2;
import org.cablelabs.impl.service.NetworkExt;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.SIRequestException;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.TransportExt;
import org.cablelabs.impl.storage.MediaStorageVolumeImpl;
import org.cablelabs.impl.util.Containable;
import org.cablelabs.impl.util.HNUtil;
import org.dvb.application.AppID;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.hn.ContentServerNetModule;
import org.ocap.hn.NetActionHandler;
import org.ocap.hn.NetActionRequest;
import org.ocap.hn.content.ContentContainer;
import org.ocap.hn.content.ContentFormat;
import org.ocap.hn.content.ContentItem;
import org.ocap.hn.content.ContentResource;
import org.ocap.hn.content.IOStatus;
import org.ocap.hn.content.MetadataNode;
import org.ocap.hn.recording.NetRecordingEntry;
import org.ocap.hn.recording.RecordingContentItem;
import org.ocap.hn.transformation.Transformation;
import org.ocap.hn.transformation.TransformationListener;
import org.ocap.hn.transformation.TransformationManager;
import org.ocap.net.OcapLocator;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;
import org.ocap.storage.ExtendedFileAccessPermissions;

/**
 * This class represents an OcapRecordingRequest and implements the functionality of 
 * a RecordingContentItem.
 * 
 * Instances of this will be created by the DVR RecordingManager. The implementation
 * of the HN-managed RecordingContentItem (RecordingContentItemImpl) delegates some of 
 * its functionality to this class. And this class delegates some of its functionality
 * to the HN-managed RecordingContentItem (and its sub-classes). 
 */
public class RecordingContentItemLocal extends RecordingImpl 
    implements UsageTrackingContentItem, RecordingContentItem, IOStatus, Containable,
               LocalRecordingContentItem
{
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long NANOS_PER_SECOND = 1000000000L;
    private static final long NANOS_PER_MILLI = NANOS_PER_SECOND / MILLIS_PER_SECOND;
    
    /** List of all profiles supported for this RecordingContentItem */
    private List m_nativeProfileList;

    /** List of all profiles enabled for this RecordingContentItem 
     *   (can be modified via ContentResource.delete()) */
    private List m_enabledProfileList;

    /** List of transformations applied before the RCI has any resources */
    private List m_pendingTransformationList = null;

    /** List of all transformations enabled for this RecordingContentItem 
     *  (can be modified via TransformationManager calls and ContentResource.delete()) */
    private List m_enabledTransformationList;

    /** List of all currently-active ContentResources 
     *  (can be modified via TransformationManager calls and ContentResource.delete()) */
    private List m_resourceList = new ArrayList(0);

    /** Set of enabled Transformations that have had their TransformationListener called */
    private Set m_notifiedTransformations = new HashSet();
    private boolean m_recordingMetadataInitialized = false;
    
    private final String m_logPrefix = "RCIL 0x" + Integer.toHexString(this.hashCode()) + ": ";
    
    /**
     * Generate a comma separated list of properties which may appear
     * in UPnP actions related to RecordTask.
     *
     * @return  string containing comma separated values which are
     * property names which may be used in UPnP actions
     */
    public static String getRecordTaskPropertyCSVStr()
    {
        return RecordScheduleDirectManual.buildPropertyCSVStr(ALLOWEDPROPERTYKEYSFORTASK.iterator());
    }

    /**
     * Return an array of strings which represents the name of allowed properties
     * for record tasks returned in response to a UPnP action such as BrowseRecordTasks
     *
     * @return  strings which represent allowed record task properties
     */
    public static String[] getRecordTaskProperties()
    {
        return RecordScheduleDirectManual.buildPropertiesArray(ALLOWEDPROPERTYKEYSFORTASK.iterator());
    }

    /**
     * Return an array of strings which represents the name of required properties
     * for record tasks returned in response to a UPnP action such as BrowseRecordTasks
     *
     * @return  strings which represent required record task properties
     */
    public static String[] getRecordTaskPropertiesRequired()
    {
        return RecordScheduleDirectManual.buildPropertiesArray(REQUIREDPROPERTYKEYSFORTASK.iterator());
    }

    /**
     * Return an XML fragment which describes the allowed values for the
     * list of supplied properties.
     *
     * @return  XML fragment describing allowed values for properties
     */
    public static String getRecordTaskAllowedFieldsXMLStr(String properties[])
    {
        return RecordScheduleDirectManual.buildAllowedValuesXMLStr(properties);
    }

    // used to hold the Set of allowed property keys for RECORD_TASK_USAGE
    private static final Set ALLOWEDPROPERTYKEYSFORTASK = new HashSet();

    // used to hold the Set of required property keys for RECORD_TASK_USAGE
    private static final Set REQUIREDPROPERTYKEYSFORTASK = new HashSet();
    
    /**
     * Construct a RecordingContentItemLocal from a RecordingInfo2 et al.
     */
    public RecordingContentItemLocal(RecordingInfo2 info, RecordingDBManager rdbm,
            RecordingManagerInterface recordingManager)
    {
        super(info, rdbm, recordingManager);

        // Use the appID associated with the recording to create metadata
        AppID appID = info.getAppId();

        recordingContentItem = new RecordingContentItemImpl(this, metadataNode(appID));
            
        initializeMetadata();
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "construct with RecordingInfo2");
        }
    }

    /**
     * Construct a RecordingContentItemLocal from a LocatorRecordingSpec et al.
     */
    public RecordingContentItemLocal(LocatorRecordingSpec lrs, RecordingDBManager rdbm,
            RecordingManagerInterface recordingManager)
    {
        super(lrs, rdbm, recordingManager);

        recordingContentItem = new RecordingContentItemImpl(this, metadataNode());

        initializeMetadata();
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "construct with LocatorRecordingSpec");
        }
    }

    /**
     * Construct a RecordingContentItemLocal from a ServiceRecordingSpec et al.
     */
    public RecordingContentItemLocal(ServiceRecordingSpec srs, RecordingDBManager rdbm,
            RecordingManagerInterface recordingManager)
    {
        super(srs, rdbm, recordingManager);

        recordingContentItem = new RecordingContentItemImpl(this, metadataNode());

        initializeMetadata();
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "construct with ServiceRecordingSpec");
        }
    }

    /**
     * Construct a RecordingContentItemLocal from a ServiceContextRecordingSpec
     * et al.
     */
    public RecordingContentItemLocal(ServiceContextRecordingSpec scrs, RecordingDBManager rdbm,
            RecordingManagerInterface recordingManager)
    {
        super(scrs, rdbm, recordingManager);

        recordingContentItem = new RecordingContentItemImpl(this, metadataNode());

        initializeMetadata();
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "construct with ServiceContextRecordingSpec");
        }
    }
    
    private void initializeMetadata()
    {
        synchronized (m_sync)
        {
            initializeRecordingRequestMetadataValues();
            if (hasPlayableRecordedService())
            {
                initializeRecordingMetadataValues();
            }
        } // END synchronized (m_sync)
    }

    /**
     * Return a RecordedService for the requested mediatime
     * 
     * @param startOffsetNanos
     *            requested start offset in nanoseconds
     * @return a recordedservice valid for that mediatime or null if service is pending
     */
    public OcapRecordedServiceExt getRecording(long startOffsetNanos)
    {
        OcapRecordedServiceExt service = null;
        RecordedService[] services;
        
        //a recorded service may not exist, return null in that case
        
        synchronized (m_sync)
        {
            try
            {
                if (!hasPlayableRecordedService())
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "getRecording - service is null - returning null");
                    }
                    return null;
                }
                services = getService().getSegments();
            }
            catch (IllegalStateException ise)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "getRecording - service is pending - returning null");
                }
                return null;
            }
            catch (AccessDeniedException e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(m_logPrefix + "getRecording - Unable to retrieve service: " + e.getMessage());
                }
                return null;
            }
            
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "getRecording - evaluating recording segments - count: " + services.length + 
                ", looking for nanos offset: " + startOffsetNanos);
            }
            long recordingDurationMillis = 0L;
            int i = 0;
            for (; i < services.length; i++)
            {
                // duration is millis
                long thisRecordingDurationMillis = services[i].getRecordedDuration();
                recordingDurationMillis += thisRecordingDurationMillis;
                if (startOffsetNanos <= (recordingDurationMillis * NANOS_PER_MILLI))
                {
                    service = (OcapRecordedServiceExt) services[i];
                    break;
                }
            }
            if (service == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(m_logPrefix + "getRecording - Unable to find segment for offset nanos: " + startOffsetNanos + ", recording duration nanos: " + (recordingDurationMillis * NANOS_PER_MILLI));
                }
                return null;
            }
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "getRecording for nanos: " + startOffsetNanos + " returning index: " + i);
            }
        } // END synchronized (m_sync)
        return service;
    }
    
    /**
     * Method over-ridden from RecordingImpl
     * 
     * @see org.cablelabs.impl.manager.recording.RecordingImpl#delete()
     */
    public void delete() throws AccessDeniedException
    {
        // This means the RecordingRequest is being deleted, which is implicitly Local 
        super.delete();
        
        try
        {
            deleteEntry();
            synchronized (m_sync)
            {
                removeRecordingRequestMetadataValues();
            }
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Exception deleting " + this, e);
            }
        }
    }

    /**
     * Method over-ridden from RecordingImpl
     * 
     * @see org.cablelabs.impl.manager.recording.RecordingImpl#saveRecordingInfo()
     */
    public void saveRecordingInfo(int updateFlag)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        // This is called anytime the RecordingInfo is being written out to persistent
        //  storage. We'll use this as an opportunity to update the RCIL metadata
        super.saveRecordingInfo(updateFlag);
        
        if ((updateFlag & (RecordingDBManager.STATE | RecordingDBManager.MEDIA_TIME)) != 0)
        {
            this.updateRecordingRequestMetadataValues();
            if (hasPlayableRecordedService())
            {
                if (!m_recordingMetadataInitialized)
                {
                    initializeRecordingMetadataValues();
                }
                else
                {
                    updateRecordingMetadataValues();
                }
            }
            else if (this.getState() == LeafRecordingRequest.DELETED_STATE)
            {
                removeRecordingMetadataValues();
            }
        }
    }

    /**
     * Initialize and formulate all the metadata associated with the RecordingRequest.
     * 
     * This should be called when the RecordingRequest is created.
     */
    private void initializeRecordingRequestMetadataValues()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "initializeRecordingRequestMetadataValues");
        }
        
        /**
         * Update the RecordingContentItemImpl MetadataNode with
         * RecordingRequest values
         */
        MetadataNodeImpl node = (MetadataNodeImpl) getRootMetadataNode();

        String access_permissions = m_info.getFap() != null ? Utils.toCSV(m_info.getFap()) : null;
        if (access_permissions != null)
        {
            node.addMetadataRegardless(UPnPConstants.QN_OCAP_ACCESS_PERMISSIONS, access_permissions);
        }
        
        /* See JIRA issue OCSPEC-243. */
        node.addMetadataRegardless(PROP_DESTINATION, "TODO");
        node.addMetadataRegardless(UPnPConstants.QN_UPNP_CLASS, ContentItem.VIDEO_ITEM);
        node.addMetadataRegardless(UPnPConstants.QN_OCAP_APP_ID, m_info.getAppId());
        node.addMetadataRegardless(PROP_MSO_CONTENT, Boolean.TRUE); // recordings are always mso content
    } // END initializeRecordingRequestMetadataValues()
    
    /**
     * Reformulate all the metadata associated with the RecordingRequest that depends
     * on change-able RecordingRequest fields. 
     * 
     * This should be called when the RecordingRequest changes.
     */
    private void updateRecordingRequestMetadataValues()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "updateRecordingRequestMetadataValues");
        }
        
        /**
         * Gather the metadata from the RecordingRequest
         */
        String scheduledChannelIDType;
        String scheduledChannelID;
        Object expirationPeriod = null;
        Object spaceRequired = null;

        try
        {
            RecordingSpec recSpec;
            try
            {
                recSpec = getRecordingSpec();
                expirationPeriod = new Long(recSpec.getProperties().getExpirationPeriod());
                spaceRequired = new Long(getSpaceRequired());
            }
            catch (Exception e)
            {
                recSpec = null;
            }

            ServiceExt sourceService;
            OcapLocator sourceLocator;

            if (recSpec instanceof LocatorRecordingSpec)
            {
                sourceLocator = (OcapLocator) ((LocatorRecordingSpec) recSpec).getSource()[0];
                sourceService = (ServiceExt) (SIManager.createInstance().getService(sourceLocator));
            }
            else if (recSpec instanceof ServiceRecordingSpec)
            {
                sourceService = (ServiceExt) ((ServiceRecordingSpec) recSpec).getSource();
                sourceLocator = (OcapLocator) sourceService.getLocator();
            }
            else if (recSpec instanceof ServiceContextRecordingSpec)
            {
                ServiceContext sc = ((ServiceContextRecordingSpec) recSpec).getServiceContext();

                if (sc == null || m_info.getServiceLocators() == null)
                {
                    throw new SIRequestException(SIRequestFailureType.DATA_UNAVAILABLE);
                }
                sourceService = (ServiceExt) sc.getService();
                sourceLocator = m_info.getServiceLocators()[0];
            }
            else
            {
                sourceService = null;
                sourceLocator = null;
            }

            if ((sourceService == null) || (sourceLocator == null))
            {
                scheduledChannelIDType = "unknown";
                scheduledChannelID = "unknown";
            }
            else
            {
                final ServiceDetailsExt sourceSD = (ServiceDetailsExt) (sourceService.getDetails());
    
                // First try to generate "SI" form...
                final int sourceID = sourceLocator.getSourceID();
    
                if (sourceID > 0)
                {
                    scheduledChannelIDType = "SI";
    
                    final SIManagerExt sim = (SIManagerExt) SIManager.createInstance();
    
                    final int networkID = ((NetworkExt) ((TransportExt) (sim.getTransports()[0])).getNetworks()[0]).getNetworkID();
                    final int tsID = sim.getTransportStream(sourceLocator).getTransportStreamID();
    
                    scheduledChannelID = networkID + "," + tsID + "," + sourceID;
                }
                else
                {
                    if (sourceSD.isAnalog())
                    { // Analog
                        scheduledChannelIDType = "ANALOG";
    
                        sourceLocator = (OcapLocator) (LocatorFactory.getInstance().transformLocator(sourceLocator)[0]);
                        //final int freq = sourceLocator.getFrequency();
    
                        // Map analog frequency to a NTSC channel number
                        final int ntscChannelNumber = 7; // TODO: write: int
                                                         // getNTSCChannelForFrequency(freq);
                        scheduledChannelID = Integer.toString(ntscChannelNumber);
                    }
                    else
                    { // Digital
                        scheduledChannelIDType = "DIGITAL";
    
                        final int majorNumber = sourceService.getServiceNumber();
                        final int minorNumber = sourceService.getMinorNumber();
                        scheduledChannelID = majorNumber + "," + minorNumber;
                    }
                }
            }
        }
        catch (Exception e)
        {
            scheduledChannelIDType = "unknown";
            scheduledChannelID = "unknown";
        }

        if (log.isDebugEnabled())
        {
            log.debug("updateRecordingRequestMetadataValues: scheduledChannelID@Type/scheduledChannelID: " + scheduledChannelIDType + '/'
            + scheduledChannelID);
        }

        /**
         * Update the RecordingContentItemImpl MetadataNode with RecordingRequest values
         */
        MetadataNodeImpl node = (MetadataNodeImpl) getRootMetadataNode();

        // TODO: use QNs here
        node.addMetadataRegardless(PROP_DURATION,           new Integer((int)m_info.getRequestedDuration()));
        node.addMetadataRegardless(PROP_EXPIRATION_PERIOD,  expirationPeriod);
        node.addMetadataRegardless(PROP_ORGANIZATION,       m_info.getOrganization() == null ? "" : m_info.getOrganization());
        node.addMetadataRegardless(PROP_PRIORITY_FLAG,      new Integer((int)m_info.getPriority()));
        node.addMetadataRegardless(PROP_RECORDING_STATE,    new Integer(m_info.getState()));
        node.addMetadataRegardless(PROP_RETENTION_PRIORITY, new Integer(m_info.getRetentionPriority()));
        node.addMetadataRegardless(PROP_SOURCE_ID,          scheduledChannelID);
        node.addMetadataRegardless(PROP_SOURCE_ID_TYPE,     scheduledChannelIDType);
        node.addMetadataRegardless(PROP_SPACE_REQUIRED,     spaceRequired); // TODO: type coercion here needs to be checked
        node.addMetadataRegardless(PROP_START_TIME,         new Date(m_info.getRequestedStartTime()));
        if (!hasPlayableRecordedService() || (getItemService() == null))
        {
            node.addMetadataRegardless(PROP_MEDIA_FIRST_TIME, new Long(0));
        }
    } // END updateRecordingRequestMetadataValues()
    
    /**
     * Remove all the metadata associated with the RecordingRequest.
     */
    private void removeRecordingRequestMetadataValues()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "removeRecordingRequestMetadataValues");
        }
        
        /**
         * Update the RecordingContentItemImpl MetadataNode with RecordingRequest values
         */
        MetadataNodeImpl node = (MetadataNodeImpl) getRootMetadataNode();
        
        node.addMetadataRegardless(PROP_DURATION,           null);
        node.addMetadataRegardless(PROP_EXPIRATION_PERIOD,  null);
        node.addMetadataRegardless(PROP_ORGANIZATION,       null);
        node.addMetadataRegardless(PROP_PRIORITY_FLAG,      null);
        node.addMetadataRegardless(PROP_RECORDING_STATE,    null);
        node.addMetadataRegardless(PROP_RETENTION_PRIORITY, null);
        node.addMetadataRegardless(PROP_SOURCE_ID,          null);
        node.addMetadataRegardless(PROP_SOURCE_ID_TYPE,     null);
        node.addMetadataRegardless(PROP_SPACE_REQUIRED,     null);
        node.addMetadataRegardless(PROP_START_TIME,         null);
        node.addMetadataRegardless(PROP_MEDIA_FIRST_TIME,   null);
        
        this.removeRecordingMetadataValues();
    } // END removeRecordingRequestMetadataValues()
    
    /**
     * Formulate/initialize all the metadata associated with the RecordedService.
     * This should be called when the RecordedService is created (a one-time occurrence)
     */
    private void initializeRecordingMetadataValues()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        // Assert: hasPlayableRecordedService() == true
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "initializeRecordingMetadataValues");
        }

        //
        // Initialize the list of primary resource profiles
        //
        String nativeProfileIDs[] = HNAPIImpl.nativeServerGetDLNAProfileIds(
                HNStreamContentLocationType.HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT, 
                getContentDescription(0L) );
        
        if (ASSERTING)
            Assert.condition(m_nativeProfileList==null, "The native profile list is not null!");

        if (ASSERTING)
            Assert.condition(m_enabledProfileList==null, "The enabled profile list is not null!");

        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "initializeRecordingMetadataValues: Initializing with " 
                       + nativeProfileIDs.length + " profiles" );
        }
        m_nativeProfileList = Arrays.asList(nativeProfileIDs);

        // All native profiles are enabled by default 
        m_enabledProfileList = new ArrayList(m_nativeProfileList);
        
        //
        // Initialize the list of transformed resource profiles
        //
        
        // Transformations can be applied to ContentItems - even if they don't yet have resources...
        final List initialTransformationList;
        
        if (m_pendingTransformationList != null)
        { // We've had Transformations set before we have resources. This is our default.
            initialTransformationList = m_pendingTransformationList;
            m_pendingTransformationList = null;
        }
        else
        {
            final TransformationManager tm = TransformationManagerImpl.getInstanceRegardless();
            if (tm == null)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(m_logPrefix + "initializeRecordingMetadataValues: Could not get a TransformationManager instance");
                }
                initialTransformationList = new ArrayList(0);
            }
            else
            {
                initialTransformationList = Arrays.asList(tm.getDefaultTransformations()); 
            }
        }
        
        filterAndSaveTransformations(initialTransformationList);
        if (log.isDebugEnabled())
        {
            log.debug( m_logPrefix + "initializeRecordingMetadataValues: Initializing with " 
                       + m_enabledTransformationList.size() + " transformations" );
        }
        
        // Create the res blocks
        updateRecordingMetadataValues();
        
        m_recordingMetadataInitialized = true;
    } // END initializeRecordingMetadataValues()
    
    /**
     * Formulate all the metadata associated with the RecordedService.
     * This should be called when the RecordedService is updated.
     */
    private void updateRecordingMetadataValues()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        /**
         * Update the RecordingContentItemImpl MetadataNode to reflect RecordedService values
         */
        
        final String prefix = m_logPrefix + "updateRecordingMetadataValues(id=" + this.getID() 
                                          + ",rrid=" + this.getId() + "): ";
        final TransformationManagerImpl transformationManager 
                                          = (TransformationManagerImpl)
                                            TransformationManagerImpl.getInstanceRegardless();
        final MetadataNodeImpl ourNode = (MetadataNodeImpl) getRootMetadataNode();
        
        final Long presentationPoint = new Long(m_info.getMediaTime() / NANOS_PER_MILLI);
        ourNode.addMetadataRegardless(PROP_PRESENTATION_POINT, presentationPoint);
        
        final Long firstMediatime = new Long(((RecordedService) getItemService()).getFirstMediaTime().getNanoseconds() / NANOS_PER_MILLI);
        ourNode.addMetadataRegardless(PROP_MEDIA_FIRST_TIME, firstMediatime);
        
        final boolean dtcpEnabled = MediaServer.getLinkProtectionFlag();
        int recordingType;
        boolean needsLinkProtection;
        String durationStr;

        switch (m_info.getState())
        {
            case LeafRecordingRequest.IN_PROGRESS_STATE:
            case LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE:
            case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
            case LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE:
            {
                // There's a recording, but it's not complete (yet)
                if (hasPlayableRecordedService())
                {
                    // For in-progress recordings, consider the current duration the total
                    //  requested duration (will be updated with actual when the recording
                    //  completes)
                    durationStr = Utils.formatDateResDuration(this.getDuration());
                    
                    if (log.isInfoEnabled()) 
                    {
                        log.info(prefix + "in-progress recording duration: " + durationStr);
                    }

                    if (dtcpEnabled) 
                    { // We don't look at the CCI bits in this case. 
                      //  Even if there aren't any, one could be added while recording
                        needsLinkProtection = true;
                        recordingType = HNStreamProtocolInfo.PROTOCOL_TYPE_DTCP_RECORDING_INPROGRESS;
                    }
                    else
                    {
                        needsLinkProtection = false;
                        recordingType = HNStreamProtocolInfo.PROTOCOL_TYPE_RECORDING_INPROGRESS;
                    }
                }
                else
                {
                    durationStr = "";
                    needsLinkProtection = false;
                    recordingType = HNStreamProtocolInfo.PROTOCOL_TYPE_UNDEFINED;
                }

                break;
            }
            case LeafRecordingRequest.COMPLETED_STATE:
            case LeafRecordingRequest.INCOMPLETE_STATE:
            {
                // Recording has completed
                recordingType = HNStreamProtocolInfo.PROTOCOL_TYPE_RECORDING;
                if (hasPlayableRecordedService())
                {
                    durationStr = getRecordingDurationStr();
                    if (dtcpEnabled && m_segmentedRecordedService.cciIndicatesEncryptionRequired())
                    {
                        needsLinkProtection = true;
                        recordingType = HNStreamProtocolInfo.PROTOCOL_TYPE_DTCP_RECORDING;
                    }
                    else
                    {
                        needsLinkProtection = false;
                        recordingType = HNStreamProtocolInfo.PROTOCOL_TYPE_RECORDING;
                    }
                }
                else
                {
                    durationStr = "";
                    needsLinkProtection = false;
                    recordingType = HNStreamProtocolInfo.PROTOCOL_TYPE_UNDEFINED;
                }

                break;
            }
            case LeafRecordingRequest.FAILED_STATE:
            case LeafRecordingRequest.DELETED_STATE:
            case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
            case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
            case OcapRecordingRequest.CANCELLED_STATE:
            case OcapRecordingRequest.TEST_STATE:
            default:
            { // We shouldn't even be in this function in these cases...
                if (log.isWarnEnabled()) 
                {
                    log.warn(prefix + "Called for a RecordingContentItem without a recording!");
                }
                return;
            }
        } // END switch (m_info.getState())
        
        //
        // Record the mappings of the alt URIs before we rewrite the res blocks so we can re-map 
        //  them to the correct res blocks if/when the ordering of the res blocks changes
        //
        Hashtable altURIMappings = new Hashtable();
        
        final String resURIs[] 
                       = (String[])ourNode.getMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES);
        final String altURIs[] 
                       = (String[])ourNode.getMetadataRegardless(UPnPConstants.QN_OCAP_RES_ALT_URI);
        
        if ((resURIs != null) && (altURIs != null))
        {
            for (int i=0; i<resURIs.length; i++)
            {
                String alturi = (i < altURIs.length) ? altURIs[i] : null;
                if (alturi != null)
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info(prefix + "res[" + i + "]: " + resURIs[i]
                                 + " has alt URI " + alturi );
                    }
                    // We're just going to map the alt URI to the resource path to avoid mixups
                    altURIMappings.put(HNUtil.getPathFromHttpURI(resURIs[i]), alturi);
                }
            }
        }
        
        if (log.isInfoEnabled()) 
        {
            log.info(prefix + "inProgress=" +
                    ((recordingType == HNStreamProtocolInfo.PROTOCOL_TYPE_RECORDING_INPROGRESS) 
                     || (recordingType == HNStreamProtocolInfo.PROTOCOL_TYPE_DTCP_RECORDING_INPROGRESS)) 
                     + ", needsLinkProtection=" + needsLinkProtection);
        }

        //
        // Build the resources for the native/non-transformed profiles
        //
        final List enabledProfileResourceList = 
            createResourcesForEnabledNativeProfiles( recordingType, durationStr, 
                                                     needsLinkProtection, altURIMappings );
        //
        // Build the resources for the transformations
        //
        final List enabledTransformResourceList = 
            createResourcesForEnabledTransformations( recordingType, durationStr, 
                                                      needsLinkProtection, altURIMappings );
        
        //
        // Over-write the current resource list
        //
        m_resourceList = new ArrayList( enabledProfileResourceList.size() 
                                        + enabledTransformResourceList.size() );
        m_resourceList.addAll(enabledProfileResourceList);
        m_resourceList.addAll(enabledTransformResourceList);
        enabledProfileResourceList.clear(); // We're done with this list

        //
        // Should have all our ContentResources lined up - now set the res blocks
        //
        {
            final int numResBlocks = m_resourceList.size();

            if (log.isInfoEnabled())
            {
                log.info(prefix + "Updating metadata for " + numResBlocks + " res blocks");
            }
            
            // These arrays will constitute the res blocks - one element resource
            final String[] uriStrs = new String[numResBlocks];
            final String[] altUriStrs = new String[numResBlocks];
            final String[] protocolInfoStrs = new String[numResBlocks];
            final String[] durationStrs = new String[numResBlocks];
            final String[] bitrateStrs = new String[numResBlocks];
            final String[] resolutionStrs = new String[numResBlocks];
            final String[] updateCountStrs = new String[numResBlocks];
            final String[] cleartextSizeStrs = new String[numResBlocks];
            final String[] sizeStrs = new String[numResBlocks];

            // Populate the export arrays according to the resources (set above)
            Iterator it = m_resourceList.iterator();
            int resIndex = 0;
            while (it.hasNext())
            {
                final ContentResourceImpl cri = (ContentResourceImpl)it.next();
                // Convert the URI to the "exportable" form (to enable URI rewriting)
                uriStrs[resIndex] = MediaServer.getContentExportURLPlaceholder(cri.getURIPath());
                altUriStrs[resIndex] = cri.getAltURI();
                protocolInfoStrs[resIndex] = cri.getProtocolInfo().getAsString();
                durationStrs[resIndex] = durationStr; // Duration is the same for all res
                final long bitrate = cri.getBitrate();
                bitrateStrs[resIndex] = (bitrate > 0) ? Long.toString(bitrate) : null;
                final Dimension resolution = cri.getResolution();
                resolutionStrs[resIndex] = (resolution != null) 
                                           ? ( Integer.toString(resolution.width) 
                                               + 'x' + Integer.toString(resolution.height))
                                           : null;
                updateCountStrs[resIndex] = "0"; // Update count is reset for all res
                final Long cleartextSize = cri.getContentCleartextSize();
                cleartextSizeStrs[resIndex] = (cleartextSize != null) 
                                              ? cleartextSize.toString() : null;
                final long size = cri.getContentSize();
                sizeStrs[resIndex] = (size > 0) ? Long.toString(size) : null;
                
                resIndex++;
            }
            
            final String contentURI = (numResBlocks > 0) ? uriStrs[0] : null;

            final MetadataNodeImpl node = (MetadataNodeImpl) getRootMetadataNode();

            node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES, uriStrs);
            node.addMetadataRegardless(UPnPConstants.RESOURCE_ALT_URI, altUriStrs);
            node.addMetadataRegardless(UPnPConstants.RESOURCE_PROTOCOL_INFO, protocolInfoStrs);
            node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_DURATION, durationStrs);
            node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_BIT_RATE, bitrateStrs);
            node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_RESOLUTION, resolutionStrs);
            node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_UPDATE_COUNT, updateCountStrs);
            node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_CLEARTEXT_SIZE, cleartextSizeStrs);
            node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_SIZE, sizeStrs);
            node.addMetadataRegardless(UPnPConstants.QN_OCAP_CONTENT_URI, 
                    MediaServer.getContentExportURLPlaceholder(HNUtil.getPathFromHttpURI(contentURI)));
        }
        if (log.isInfoEnabled())
        {
            log.info(prefix + "updateResBlock: res blocks updated");
        }
        
        //
        // Perform notifications for newly-added transformations
        //
        if (transformationManager.transformationListenerRegistered())
        { // Let's not do this work unless it makes sense...
            for (Iterator it=m_enabledTransformationList.iterator();it.hasNext();)
            {
                Transformation transform = (Transformation)it.next();
                if (!m_notifiedTransformations.contains(transform))
                {
                    transformationManager.enqueueTransformationReadyNotification(this, transform);

                    // Remember that we've enqueued the notification. Each 
                    //  (contentItem,transformation) tuple should receive exactly one notification
                    m_notifiedTransformations.add(transform);
                }
            }
        }
    } // END updateRecordingMetadataValues()
    
    /**
     * Create LocalRecordingContentItemResources for all the enabled native profiles 
     * and return them in a List.
     */
    private List createResourcesForEnabledNativeProfiles( final int recordingType, 
                                                          final String durationStr,
                                                          final boolean needsLinkProtection,
                                                          final Hashtable altURIMappings )
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        final MediaServer mediaServer = MediaServer.getInstance();
        final String prefix = m_logPrefix + "createResourcesForEnabledNativeProfiles(id=" 
                                          + this.getID() + ",rrid=" + this.getId() + "): ";
        final List resourceList = new ArrayList(m_enabledProfileList.size());
        if (m_enabledProfileList.size() > 0)
        {
            String [] enabledProfiles = new String[m_enabledProfileList.size()];
            enabledProfiles = (String [])(m_enabledProfileList.toArray(enabledProfiles));
            
            HNStreamProtocolInfo enabledProtocolInfos[] 
                    = HNStreamProtocolInfo.getProtocolInfoStrsForProfiles(
                              recordingType, 
                              HNStreamContentLocationType.HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT, 
                              getContentDescription(0L), needsLinkProtection, enabledProfiles);
            
            for (int i = 0; i < enabledProtocolInfos.length; i++)
            {
                final HNStreamProtocolInfo protocolInfo = enabledProtocolInfos[i];
                final String resPath = 
                    ContentDirectoryService.RECORDING_REQUEST_URI_PATH
                    + '?'
                    + ContentDirectoryService.RECORDING_REQUEST_URI_ID_PREFIX
                    + getId()
                    + '&'
                    + ContentDirectoryService.REQUEST_URI_PROFILE_PREFIX
                    + protocolInfo.getProfileId()
                    + '&'
                    + ContentDirectoryService.REQUEST_URI_MIME_PREFIX
                    + protocolInfo.getContentFormat();
                
                // This is the URI we'll use for the local ContentResources
                final String localURI = mediaServer.getContentLocalURLForm(resPath);
                
                // Re-associate the alt URI with its original res URI (may be null)
                String altURIStr = (String)altURIMappings.get(resPath);
                
                String protocolInfoStr = protocolInfo.getAsString();
                if (log.isInfoEnabled()) 
                {
                    log.info(prefix + "profile res block["+i+"]:");
                    log.info(prefix + " profile [" + i + "] res value: " + localURI);
                    log.info(prefix + " profile [" + i + "] " + UPnPConstants.RESOURCE_PROTOCOL_INFO 
                                    + ": " + protocolInfoStr );
                }
    
                if (log.isInfoEnabled())
                {
                    log.info( prefix + " profile ["+i+"] " + UPnPConstants.RESOURCE_DURATION 
                              + ": " + durationStr);
                }
                
                String sizeStr = null;
                String cleartextSizeStr = null;
                
                if ( recordingType == HNStreamProtocolInfo.PROTOCOL_TYPE_RECORDING
                     || recordingType == HNStreamProtocolInfo.PROTOCOL_TYPE_DTCP_RECORDING) // completed recording
                {
                    final long cleartextSize = getRecordingSize( protocolInfo.getProfileId(),
                                                                 protocolInfo.getContentFormat(), 
                                                                 null, false );
                    if (needsLinkProtection)
                    { // The res@size will be the encrypted size and the non-encrypted size 
                      //  is included as res@dlna:cleartextSize
                        final long encryptedSize = getRecordingSize(
                                                     protocolInfo.getProfileId(),
                                                     protocolInfo.getContentFormat(), 
                                                     null, true );
                        sizeStr = (encryptedSize > 0) ? Long.toString(encryptedSize) : null;
                        cleartextSizeStr = (cleartextSize > 0) ? Long.toString(cleartextSize) : null;
                        if (log.isInfoEnabled())
                        {
                            log.info( prefix + " profile ["+i+"] res@dlna:cleartextSize: " 
                                             + cleartextSizeStr);
                        }
                    }
                    else
                    { // The res@size is just the unencrypted size
                        cleartextSizeStr = null;
                        sizeStr = (cleartextSize > 0) ? Long.toString(cleartextSize) : null;
                    }
                    if (log.isInfoEnabled())
                    {
                        log.info( prefix + " profile [" + i + "] " 
                                         + UPnPConstants.RESOURCE_SIZE + ": " + sizeStr );
                    }
                } // END if (recording is completed)
                
                // Create the ContentResource for this native profile
                ContentResourceImpl localContentResource 
                    = new LocalRecordingContentItemResource(this, localURI,
                            protocolInfoStr, sizeStr, durationStr, null, 
                            null, null, null, null, null, null, null,
                            cleartextSizeStr, altURIStr, protocolInfo.getBaseProfileId() ); 
                resourceList.add(localContentResource);
            } // END for (protocol infos for enabled native profiles)
        } // END creation of profile-based resources

        return resourceList;
    } // END addResourcesForEnabledNativeProfiles()

    /**
     * Create LocalRecordingContentItemResources for all the enabled Transformations
     * and return them in a List.
     */
    private List createResourcesForEnabledTransformations( final int recordingType, 
                                                           final String durationStr,
                                                           final boolean needsLinkProtection,
                                                           final Hashtable altURIMappings )
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        final MediaServer mediaServer = MediaServer.getInstance();
        final String prefix = m_logPrefix + "createResourcesForEnabledTransformations(id=" 
                                          + this.getID() + ",rrid=" + this.getId() + "): ";
        final List resourceList = new ArrayList(m_enabledProfileList.size());
        
        if (m_enabledTransformationList.size() > 0)
        {
            if (log.isInfoEnabled()) 
            {
                log.info(prefix + "Creating resources for "+ m_enabledTransformationList.size() 
                                + " transformations" );
            }
            Iterator it = m_enabledTransformationList.listIterator();
            // Loop through the transformations
            while (it.hasNext())
            {
                final TransformationImpl transformation = (TransformationImpl)it.next();
                final OutputVideoContentFormatExt outputFormat 
                                           = (OutputVideoContentFormatExt)
                                             transformation.getOutputContentFormat();
                HNStreamProtocolInfo enabledProtocolInfos[] 
                  = HNStreamProtocolInfo.getProtocolInfoStrsForTransformedContent(
                            recordingType, 
                            HNStreamContentLocationType.HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT, 
                            getContentDescription(0L), needsLinkProtection, outputFormat );
                
                if (log.isInfoEnabled()) 
                {
                    log.info(prefix + "Got "+ enabledProtocolInfos.length 
                                    + " protocol infos for transformation " 
                                    + outputFormat.getOutputFormatId() );
                }
                // Loop through the protocol infos associated with the output format
                for (int i = 0; i < enabledProtocolInfos.length; i++)
                {
                    final int resCount = m_resourceList.size();
                    
                    final HNStreamProtocolInfo protocolInfo = enabledProtocolInfos[i];
                    final String resPath = 
                        ContentDirectoryService.RECORDING_REQUEST_URI_PATH
                        + '?'
                        + ContentDirectoryService.RECORDING_REQUEST_URI_ID_PREFIX
                        + getId()
                        + '&'
                        + ContentDirectoryService.REQUEST_URI_PROFILE_PREFIX
                        + protocolInfo.getProfileId()
                        + '&'
                        + ContentDirectoryService.REQUEST_URI_MIME_PREFIX
                        + protocolInfo.getContentFormat()
                        + '&'
                        + ContentDirectoryService.REQUEST_URI_TRANSFORMATION_PREFIX
                        + outputFormat.getOutputFormatId();
                    
                    // This is the URI we'll use for the local ContentResources
                    final String localURI = mediaServer.getContentLocalURLForm(resPath);
                    
                    // Re-associate the alt URI with its original res URI (may be null)
                    String altURIStr = (String)altURIMappings.get(resPath);
                    
                    String protocolInfoStr = protocolInfo.getAsString();
                    if (log.isInfoEnabled()) 
                    {
                        log.info(prefix + "transform res block["+resCount+"]:");
                        log.info(prefix + " transform [" + resCount + "] res value: " + localURI);
                        log.info(prefix + " transform [" + resCount + "] " 
                                        + UPnPConstants.RESOURCE_PROTOCOL_INFO 
                                        + ": " + protocolInfoStr );
                    }
        
                    if (log.isInfoEnabled())
                    {
                        log.info( prefix + " transform ["+resCount+"] " + UPnPConstants.RESOURCE_DURATION 
                                  + ": " + durationStr);
                    }
                    
                    String sizeStr = null;
                    String cleartextSizeStr = null;
                    
                    if ( recordingType == HNStreamProtocolInfo.PROTOCOL_TYPE_RECORDING
                         || recordingType == HNStreamProtocolInfo.PROTOCOL_TYPE_DTCP_RECORDING) // completed recording
                    {
                        final long cleartextSize = getRecordingSize(
                                                        protocolInfo.getProfileId(),
                                                        protocolInfo.getContentFormat(), 
                                                        outputFormat.getNativeTransformation(),
                                                        false );
                        if (needsLinkProtection)
                        { // The res@size will be the encrypted size and the non-encrypted size 
                          //  is included as res@dlna:cleartextSize
                            final long encryptedSize = getRecordingSize(
                                                         protocolInfo.getProfileId(),
                                                         protocolInfo.getContentFormat(), 
                                                         outputFormat.getNativeTransformation(),
                                                         true );
                            sizeStr = (encryptedSize > 0) ? Long.toString(encryptedSize) : null;
                            cleartextSizeStr = (cleartextSize > 0) ? Long.toString(cleartextSize) : null;
                            
                            if (log.isInfoEnabled())
                            {
                                log.info( prefix + " transform ["+resCount+"] res@dlna:cleartextSize: " 
                                          + cleartextSizeStr);
                            }
                        }
                        else
                        { // The res@size is just the unencrypted size
                            sizeStr = (cleartextSize > 0) ? Long.toString(cleartextSize) : null;
                            cleartextSizeStr = null;
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info( prefix + " transform ["+resCount+"] " 
                                      + UPnPConstants.RESOURCE_SIZE + ": " + sizeStr);
                        }
                    } // END if (recording is completed)
                    
                    // Create the ContentResource for this transformation
                    ContentResourceImpl localContentResource 
                        = new LocalRecordingContentItemResource(this, localURI, 
                                protocolInfoStr, sizeStr, durationStr, 
                                null, null, null, null, null, 
                                cleartextSizeStr, altURIStr, outputFormat ); 
                    resourceList.add(localContentResource);
                } // END for (protocol infos for enabled native profiles)
            } // END while (all enabled transformations)
        }
        
        return resourceList;
    } // END addResourcesForEnabledTransformations()

    /**
     * Remove all metadata associated with the RecordedService.
     * This should be called when the RecordedService is deleted 
     * (or becomes inaccessible?)
     */
    private void removeRecordingMetadataValues()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "removeRecordingMetadataValues");
        }
        final MetadataNodeImpl node = (MetadataNodeImpl) getRootMetadataNode();

        // Blank all the res blocks
        node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES, null);
        node.addMetadataRegardless(UPnPConstants.RESOURCE_ALT_URI, null);
        node.addMetadataRegardless(UPnPConstants.RESOURCE_PROTOCOL_INFO, null);
        node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_DURATION, null);
        node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_BIT_RATE, null);
        node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_RESOLUTION, null);
        node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_UPDATE_COUNT, null);
        node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_CLEARTEXT_SIZE, null);
        node.addMetadataRegardless(UPnPConstants.QN_DIDL_LITE_RES_SIZE, null);
        node.addMetadataRegardless(UPnPConstants.QN_OCAP_CONTENT_URI, null);
    } // END removeRecordingMetadataValues()

    public HNStreamContentDescription getContentDescription(long mediaTime)
    {
        return getContentDescription(getRecording(mediaTime));
    }

    public HNStreamContentDescription getContentDescription(OcapRecordedServiceExt recording)
    {
        if (recording == null)
        {
            if (log.isDebugEnabled()) 
            {
                log.debug("getContentDescription - recording is null - returning null");
            }
            return null;
        }
        
        MediaStorageVolume volume = getVolume();
        if (volume == null)
        {
            if (log.isWarnEnabled()) 
            {
                log.warn("getContentDescription - volumeHandle is null - returning null");
            }
            return null;
        }

        String nativeName = recording.getNativeName();

        // assuming volume will stay the same for all segments of a recording
        int volumeHandle = ((MediaStorageVolumeImpl)getVolume()).getNativeHandle();

        return new HNStreamContentDescriptionLocalSV(nativeName, volumeHandle);
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
            log.debug("RecordingContentItemLocal.isInUse() - " + "called with inUseCnt = " + inUseCnt);
        }
        boolean inUse = false;
        if (inUseCnt > 0)
        {
            inUse = true;
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
            log.debug("RecordingContentItemLocal.incrementInUseCnt() - " + "called with inUseCnt = " + inUseCnt);
        }
        inUseCnt++;
    }

    /**
     * Decrements the number of occurrences where this item is being streamed.
     */
    public void decrementInUseCount()
    {
        if (log.isDebugEnabled())
        {
            log.debug("RecordingContentItemLocal.decrementInUseCnt() - " + "called with inUseCnt = " + inUseCnt);
        }
        inUseCnt--;
        if (inUseCnt < 0)
        {
            if (log.isErrorEnabled())
            {
                log.error("RecordingContentItemLocal.decrementInUseCnt() - "
                + "Problem with logic, current in use cnt is negative = " + inUseCnt);
            }
    }
    }

    /**
     * Returns the NetRecordingEntry which contains this recording content item
     * if the NetRecordingEntry is available.
     *
     * @return null if this RecordingContentItem is not added to any
     *         NetRecordingEntry or if the NetRecordingEntry containing this
     *         RecordingContentItem is not available. Otherwise the
     *         NetRecordingEntry containing this RecordingContentItemcontent
     *         item
     */
    public NetRecordingEntry getRecordingEntry()
    {
        if (recordSchedule != null)
        {
            return recordSchedule.getNetRecordingEntry();
        }

        return null;
    }

    /**
     * Returns the ObjectID of the NetRecordingEntry which contains this
     * recording content item. The ObjectID can be obtained from
     * ocap:netRecordingEntry property of this recording content item.
     *
     * @return null if this RecordingContentItem does not contain
     *         ocap:netRecordingEntry property. Otherwise, the value contained
     *         in ocap:netRecordingEntry property of this RecordingContentItem.
     */
    public String getRecordingEntryID()
    {
        return (String) getRootMetadataNode().getMetadata(PROP_NET_RECORDING_ENTRY);
    }

    public NetActionRequest requestConflictingRecordings(NetActionHandler handler)
    {
        return recordingContentItem.requestConflictingRecordings(handler);
    }

    /**
     * The local instance of RecordingContentItem does not work this way. The
     * entry in the CDS must be used to set the media time, not the local
     * instance. The api in the base class, RecordingRequest can be used
     * instead.
     */
    public NetActionRequest requestSetMediaTime(Time time, NetActionHandler handler)
    {
        return recordingContentItem.requestSetMediaTime(time, handler);
    }

    public boolean containsResource(ContentResource entry)
    {
        return recordingContentItem.containsResource(entry);
    }

    public boolean deleteEntry() throws IOException, SecurityException
    {
        return recordingContentItem.deleteEntry();
    }

    public boolean hasBeenDeleted()
    {
        return recordingContentItem.hasBeenDeleted();
    }

    public String getContentClass()
    {
        return recordingContentItem.getContentClass();
    }

    public Service getItemService()
    {
        RecordedService s;

        try
        {
            s = getService();
        }
        catch (IllegalStateException e)
        {
            s = null;

            if (log.isDebugEnabled())
            {
                log.debug("Can't get service of " + this, e);
            }
        }
        catch (AccessDeniedException e)
        {
            s = null;

            if (log.isDebugEnabled())
            {
                log.debug("Can't get service of " + this, e);
            }
        }

        return s;
    }

    public ContentResource[] getRenderableResources()
    {
        return recordingContentItem.getRenderableResources();
    }

    public ContentResource getResource(int n) throws ArrayIndexOutOfBoundsException
    {
        return recordingContentItem.getResource(n);
    }

    public int getResourceCount()
    {
        return recordingContentItem.getResourceCount();
    }

    public int getResourceIndex(ContentResource r)
    {
        return recordingContentItem.getResourceIndex(r);
    }

    public ContentResource[] getResources()
    {
        return recordingContentItem.getResources();
    }

    public String getTitle()
    {
        return recordingContentItem.getTitle();
    }

    public boolean hasAudio()
    {
        return recordingContentItem.hasAudio();
    }

    public boolean hasStillImage()
    {
        return recordingContentItem.hasStillImage();
    }

    public boolean hasVideo()
    {
        return recordingContentItem.hasVideo();
    }

    public boolean isRenderable()
    {
        return recordingContentItem.isRenderable();
    }

    public long getContentSize()
    {
        return recordingContentItem.getContentSize();
    }

    public Date getCreationDate()
    {
        return recordingContentItem.getCreationDate();
    }

    public ContentContainer getEntryParent() throws IOException
    {
        return recordingContentItem.getEntryParent();
    }

    public ExtendedFileAccessPermissions getExtendedFileAccessPermissions()
    {
        return recordingContentItem.getExtendedFileAccessPermissions();
    }

    public String getID()
    {
        return recordingContentItem.getID();
    }

    public String getParentID()
    {
        return recordingContentItem.getParentID();
    }

    public MetadataNode getRootMetadataNode()
    {
        return recordingContentItem.getRootMetadataNode();
    }

    public ContentServerNetModule getServer()
    {
        return recordingContentItem.getServer();
    }

    public boolean hasWritePermission()
    {
        return recordingContentItem.hasWritePermission();
    }

    /**
     * Instances of this class are always local.
     */
    public boolean isLocal()
    {
        return true;
    }

    // ////////////////////////////////
    // / SRS local behavior below here
    // ///////////////////////////////
    /**
     * Check for a RecordSchedule containment
     *
     * @return true if this instance is contained in a RecordSchedule, false if
     *         not.
     */
    public boolean hasRecordSchedule()
    {
        return recordSchedule != null;
    }

    /**
     * Check for a NetRecordingEntryLocal containment
     *
     * @return true if is contained, false if not
     */
    public NetRecordingEntry getNetRecordingEntry()
    {
        return netRecordingEntry;
    }

    /**
     * Check for RecordTask association.
     *
     * @return true if this instance is associated with a RecordTask, false if
     *         not.
     */
    public boolean hasRecordTask()
    {
        return recordTask != null;
    }

    /**
     * Creates a RecordTask and associates it with this RecordingContentItem
     *
     * @return true if successful, false if not
     */
    public boolean createRecordTask()
    {
        // must have a RecordSchedule before creating a RecordTask
        if (recordSchedule == null)
        {
            return false;
        }

        // only create once
        if (recordTask != null)
        {
            return true;
        }

        recordTask = recordSchedule.createRecordTask(this);

        if (recordTask != null)
        {
            getRootMetadataNode().addMetadata(RecordingActions.RECORD_TASK_ID_KEY, recordTask.getObjectID());
        }

        //FindBugs claims that this is a "Nullcheck of value previously dereferenced". FindBugs is wrong.
        return recordTask != null;
    }

    /**
     * Removes the RecordTask from this RecordingContentItem.
     *
     * @return true if removed or already removed, false if RecordTask exists
     *         but a RecordSchedule does not, or if the remove failed.
     */
    public boolean removeRecordTask()
    {
        if (recordTask != null)
        {
            if (recordSchedule == null)
            {
                return false; // no RecordSchedule so can't remove RecordTask
            }

            if (!recordSchedule.removeRecordTask(recordTask))
            {
                return false;
            }

            recordTask = null;
        }
        // else RecordTask already removed

        return true;
    }

    /**
     * Removes a RecordSchedule from this instance if it is not associated with
     * a NetRecordingEntry.
     *
     * @return true if removed, false if not
     */
    public boolean removeRecordSchedule()
    {
        // if contained in a NetRecordingEntryLocal
        if (netRecordingEntry != null)
        {
            return false; // only the NetRecordingEntryLocal can remove the
                          // RecordSchedule
        }
        // else this is not contained in a NetRecordingEntryLocal

        if (recordSchedule != null)
        {
            if (((ScheduledRecordingService)MediaServer.getInstance().getSRS()).removeRecordSchedule(recordSchedule))
            {
                recordSchedule = null;
                return true;
            }
        }

        return false;
    }

    /**
     * Creates a RecordSchedule and associate it with this instance
     *
     * @return true if success, false otherwise
     */
    public boolean createRecordSchedule()
    {
        // only create once
        if (recordSchedule == null)
        {
            recordSchedule = ((ScheduledRecordingService)MediaServer.getInstance().getSRS()).createRecordSchedule();

            getRootMetadataNode().addMetadata(RecordingActions.RECORD_SCHEDULE_ID_KEY,
                    recordSchedule.getObjectID());
        }

        return recordSchedule != null;
    }

    /**
     * Returns the upnp:srsRecordScheduleID of the RecordSchedule that this
     * instance is contained in.
     *
     * @return upnp:srsRecordScheduleID value
     */
    public String getSrsRecordScheduleID()
    {
        String retVal = "";

        if (recordSchedule != null)
        {
            retVal = recordSchedule.getObjectID();
        }

        return retVal;
    }

    /**
     * Returns the upnp:srsRecordTaskID of the RecordTask that this instance is
     * associated in.
     *
     * @return upnp:srsRecordTaskID value
     */
    public String getSrsRecordTaskID()
    {
        String retVal = "";

        if (recordTask != null)
        {
            retVal = recordTask.getObjectID();
        }

        return retVal;
    }

    // Implementation of LocalRecordingContentItem method
    public ContentItemImpl getContentItemImpl()
    {
        return recordingContentItem;
    }

    // Implementation of LocalRecordingContentItem method
    public HNStreamProtocolInfo[] getProtocolInfo()
    {
        synchronized (m_sync)
        {
            HNStreamProtocolInfo protocolInfoArray[] 
                                    = new HNStreamProtocolInfo[m_resourceList.size()];
            Iterator it = m_resourceList.iterator();
            int i=0;
            while (it.hasNext())
            {
                LocalRecordingContentItemResource cr = (LocalRecordingContentItemResource)
                                                       it.next();
                protocolInfoArray[i++] = cr.getProtocolInfo();
            }
            
            return protocolInfoArray;
        }
    }

    // Implementation of LocalRecordingContentItem method
    public ContentResourceExt[] getContentResourceList()
    {
        synchronized (m_sync)
        {
            ContentResourceExt resourceList[] = new ContentResourceExt[m_resourceList.size()];
            return (ContentResourceExt[])(m_resourceList.toArray(resourceList));
        }
    }

    // Implementation of LocalRecordingContentItem method
    public void setPlaybackMediaTime(long mediaTime)
    {
        m_info.setMediaTime(mediaTime * 1000000L);
    }
    
    public void setID(String id)
    {
        recordingContentItem.setID(id);
    }

    /**
     * Return the total size of the recording as rendered in the designated profile/transformation
     * and with the designated link protection. If the size is not calculable, -1 is returned.
     * 
     * @param profileId The profile to calculate the size for
     * @param mimeType The MIME type to calculate the size for
     * @param nativeTransformation The transformation to be applied to the content (null if none)
     * @param needsLinkProtection Boolean indicating whether DTCP link protection is to be applied
     * 
     * @return The total size of the recording or -1 if unknown
     */
    private long getRecordingSize( String profileId, String mimeType, 
                                   NativeContentTransformation nativeTransformation,
                                   boolean needsLinkProtection )
     {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        long size = 0;
        if (hasPlayableRecordedService())
        {
            try
            {
                RecordedService[] services = getService().getSegments();
                for (int i = 0; i < services.length; i++)
                {
                    if (services[i] != null)
                    {
                        if (services[i] instanceof OcapRecordedServiceExt)
                        {
                            long tempSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(
                                    HNStreamContentLocationType.HN_CONTENT_LOCATION_LOCAL_MSV_CONTENT,
                                    getContentDescription((OcapRecordedServiceExt)services[i]),
                                    profileId, mimeType, nativeTransformation);

                            if(tempSize < 0)
                            {
                                // If the recording is in progress the platfrom may return -1
                                // Just return from here
                                return -1;
                            }
                            if(needsLinkProtection)
                            {
                                size += StreamUtil.getDTCPEncryptedSize(tempSize);
                            }
                            else
                            {
                                size += tempSize;
                            }

                            if (log.isDebugEnabled())
                            {
                                log.debug("getRecordingSize - retrieved recording seg " + i + ", size: " +
                                size);
                            }
                        }
                    }
                }
                if (log.isDebugEnabled())
                {
                    log.debug("getRecordingSize - total size: " + size);
                }
            }
            catch (AccessDeniedException e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("getRecordingSize - exception while calculating duration for entry: " + getID(), e);
                }
                size = -1;
            }
            catch (IllegalStateException iae)
            {
                if (log.isInfoEnabled())
                {
                    log.info("getRecordingSize - exception while calculating duration for entry: " + getID(), iae);
                }
                size = -1;
            }
            catch (MPEMediaError mediaError)
            {
                if (log.isInfoEnabled()) 
                {
                    log.info("Unable to retrieve size for entry: " + getID());
                }
                size = -1;
            }
        }

        return size;
    }

    private String getRecordingDurationStr()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        String durStr = null;
        long durationMS = 0L;

        if (hasPlayableRecordedService())
        {
            try
            {
                RecordedService[] services = getService().getSegments();
                for (int i = 0; i < services.length; i++)
                {
                    if (services[i] != null)
                    {
                        durationMS += services[i].getRecordedDuration();

                    }
                }
                if (log.isInfoEnabled())
                {
                    log.info("getRecordingDurationStr - total duration: " + durationMS);
                }
            }
            catch (AccessDeniedException e)
            {
                if (log.isInfoEnabled())
                {
                    log.info("getRecordingDurationStr - exception while calculating duration for entry: " + getID(), e);
                }
            }
            catch (IllegalStateException iae)
            {
                if (log.isInfoEnabled())
                {
                    log.info("getRecordingDurationStr - exception while calculating duration for entry: " + getID(), iae);
                }
            }
        }

        // Convert duration into H:MM:SS string
        durStr = Utils.formatDateResDuration(durationMS);

        return durStr;
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
        if (log.isDebugEnabled()) 
        {
            log.debug("enter: " + this + ", container: " + c);
        }
        boolean retVal = true;

        // Create a RecordSchedule for this item if it doesn't have one.
        // This indicates either this item has not been added to a
        // NetRecordingEntry
        // or the item has not been added to a ContentContainer yet.
        if (! hasRecordSchedule())
        {
            if (! createRecordSchedule())
            {
                retVal = false;
            }
        }

        if (retVal) // the RecordingContentItem has a RecordSchedule
        {
            // If the RecordingContentItem does not have a RecordTask,
            // Create one and associate it with this RecordingContentItem
            if (! hasRecordTask())
            {
                if (! createRecordTask())
                {
                    retVal = false;
                }
            }

            if (retVal) // the RecordingContentItem has a RecordTask
            {
                ContentItemImpl cdsContentItem = getContentItemImpl();

                retVal = ((ContentContainerImpl) c).addEntry(cdsContentItem);

                if (retVal)
                {
                    // NOTE: setCdsReference and setScheduledCDSEntryID do not exist
                    // and are not called:
                    // see JIRA issue OCSPEC-231.

//                    updateMetadata();

                    // TODO: shouldn't this rather be done in
                    // NetRecordingEntryLocal.addRecordingContentItem?
                    // See JIRA issue OCSPEC-237.
                    NetRecordingEntry netRecordingEntry = getNetRecordingEntry();
                    if (netRecordingEntry instanceof NetRecordingEntryLocal)
                    {
                        final MetadataNodeImpl node = (MetadataNodeImpl)getRootMetadataNode();
                        node.addMetadataRegardless(PROP_NET_RECORDING_ENTRY, netRecordingEntry.getID());
                        ((NetRecordingEntryLocal) netRecordingEntry).addRecordingContentItemID(getID());
                    }
                }
            }
        }

        return retVal;
    }

    /**
     * Returns the last seekable byte
     * Logic moved from RecordingStream.java
     * @param protocolInfo - describes the server's ability to serve the recording
     * @param contentLocationType - from where the content originated
     * @param encrypted - denotes whether or not the content should be encrypted when streaming
     * @param transformation - the transformation being applied to the RecordingContentItem, if any
     * @return - the last byte that a client may seek, can return -1 if not able to be determined.
     * @throws HNStreamingException
     */
    public long getAvailableSeekEndByte(HNStreamProtocolInfo protocolInfo, int contentLocationType, 
                                        boolean encrypted, NativeContentTransformation transformation)
            throws HNStreamingException
    {
        try
        {
            //add up network bytes for each segment in the recording
            RecordedService[] services = getService().getSegments();
            long result = 0L;
            for (int i=0;i<services.length;i++)
            {
                HNStreamContentDescription thisContentDescription = getContentDescription((OcapRecordedServiceExt) services[i]);
                long tempSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType, thisContentDescription,
                        protocolInfo.getProfileId(), protocolInfo.getContentFormat(), transformation);
                
                if(tempSize < 0)
                {
                    // For in-progress recordings the platform may return -1 as size
                    // Hence just return an unknown content size of -1
                    return -1;
                }
                
                if(protocolInfo.isLinkProtected() && encrypted)
                {
                    result += StreamUtil.getDTCPEncryptedSize(tempSize);
                }
                else
                {
                    result += tempSize;
                }
            }
            //seek position is size - 1
            return result - 1;
        }
        catch (AccessDeniedException err)
        {
            throw new HNStreamingException("Failed to retrieve available seek end byte", err);
        }
    }

    /**
     * Returns the expected number of bytes a complete recording stream should be sending when requested
     * over HTTP.
     * @param startByte - the beginning byte of the stream to send
     * @param endByte - the last byte of the stream to send
     * @param transformation TODO
     * @return - the total number of bytes for a given start and end byte for a given stream
     */
    public long getContentLength(HNStreamProtocolInfo protocolInfo, int contentLocationType, long startByte,
            long endByte, NativeContentTransformation transformation) 
        throws HNStreamingException
    {
        if (protocolInfo.isLinkProtected())
        {
            // Determine each segment end and add to segmentList
            long combinedSize = 0L;
            long encryptesSize = 0L;
            RecordedService[] segments;
            boolean startSegmentSizeFound = false;
            boolean endSegmentSizeFound = false;
            try
            {
                segments = getService().getSegments();
                for (int i = 0; i < segments.length; i++)
                {
                    OcapRecordedServiceExt thisSegment = (OcapRecordedServiceExt) segments[i];
                    HNStreamContentDescription thisDescription = getContentDescription(thisSegment);
                    long thisRecSize = HNAPIImpl.nativeServerGetNetworkContentItemSize(contentLocationType,
                            thisDescription, protocolInfo.getProfileId(), protocolInfo.getContentFormat(), 
                            transformation);
                    // If content size returned by native for this segment
                    // is unknown (-1) return from here.
                    // This may happen for certain types of content (e.g. live
                    // streaming, in-progress recording etc.)
                    if(thisRecSize < 0)
                    {
                        return -1;
                    }
                    long currentCombinedSize = combinedSize + thisRecSize;
                    if (currentCombinedSize >= startByte && !startSegmentSizeFound)
                    {
                        long startSegmentRemainingBytes; 
                        if(endByte < currentCombinedSize)
                        {
                            startSegmentRemainingBytes = endByte - startByte + 1;
                            startSegmentSizeFound = true;
                            endSegmentSizeFound = true;
                            encryptesSize += StreamUtil.getDTCPEncryptedSize(startSegmentRemainingBytes);
                            break;
                        }
                        else
                        {
                            startSegmentRemainingBytes = startByte - currentCombinedSize;
                            encryptesSize += StreamUtil.getDTCPEncryptedSize(startSegmentRemainingBytes);
                            startSegmentSizeFound = true;
                        }
                    }
                    else if (currentCombinedSize <= endByte && startSegmentSizeFound && !endSegmentSizeFound)
                    {
                        encryptesSize += StreamUtil.getDTCPEncryptedSize(thisRecSize);
                        if(currentCombinedSize == endByte)
                        {
                            endSegmentSizeFound = true;
                            break;
                        }
                    }
                    else if(currentCombinedSize > endByte && startSegmentSizeFound && !endSegmentSizeFound)
                    {
                        long endSegmentRemainingBytes = endByte - combinedSize;
                        encryptesSize += StreamUtil.getDTCPEncryptedSize(endSegmentRemainingBytes);
                        endSegmentSizeFound = true;
                        break;
                    }
                    combinedSize += thisRecSize;
                }
            }
            catch (IllegalStateException e)
            {
                throw new HNStreamingException("IllegalStateException while getting getContentLength ", e);
            }
            catch (AccessDeniedException e)
            {
                throw new HNStreamingException("AccessDeniedException while getting getContentLength ", e);
            }
            catch (Exception e)
            {
                throw new HNStreamingException("Exception while getting getContentLength ", e);
            }

            return encryptesSize;
        }
        else
        {
            // Not link protected so simply return the difference
            return endByte - startByte + 1;
        }
    }
    
    // From Transformable
    public List getTransformations()
    {
        synchronized (m_sync)
        {
            return m_enabledTransformationList;
        }
    }
    
    // From Transformable
    public void setTransformations(final List transformations) 
    {
        final TransformationManagerImpl tm = (TransformationManagerImpl)
                                             TransformationManagerImpl.getInstanceRegardless();
        synchronized (m_sync)
        {
            if (hasBeenDeleted())
            {
                if (log.isInfoEnabled()) 
                {
                    log.info(m_logPrefix + "setTransformations: The RecordingContentItem has been deleted");
                }
                if (tm.transformationListenerRegistered())
                { // Let's not do this work unless it makes sense...
                    if (log.isInfoEnabled()) 
                    {
                        log.info(m_logPrefix + "setTransformations: Signaling transformation failed");
                    }
                    for (Iterator it=transformations.iterator();it.hasNext();)
                    {
                        Transformation transform = (Transformation)it.next();
                        tm.enqueueTransformationFailedNotification( 
                              this, transform, TransformationListener.REASON_CONTENTITEM_DELETED );
                    }
                }
            }
            else
            {
                if (hasPlayableRecordedService())
                {
                    filterAndSaveTransformations(transformations);
                    updateRecordingMetadataValues();
                }
                else
                { // Just save off the set list (and overwrite whatever was previously set)
                    m_pendingTransformationList = new ArrayList(transformations);
                }
            }
        } // END synchronized (m_sync)
    }

    /**
     * Save the transformations that have input profiles that match a native profile
     * and signal notifyTransformationFailed() for transformations that don't match  
     * a native profile (with reason REASON_RESOURCE_UNAVAILABLE)
     * 
     * @param transformations Transformations to filter and save
     */
    void filterAndSaveTransformations(final List transformations)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        final TransformationManagerImpl tm = (TransformationManagerImpl)
                                             TransformationManagerImpl.getInstanceRegardless();
        m_enabledTransformationList = new ArrayList(transformations.size());
        for (Iterator it=transformations.iterator();it.hasNext();)
        {
            final Transformation transform = (Transformation)(it.next());
            // This is a safe series of derefs since transforms are stack-created to always
            //  have 1 input format with 1 valid profile
            final String inputProfile = transform.getInputContentFormat().getContentProfile();
            if (m_nativeProfileList.contains(inputProfile))
            { // If the input profile matches a native profile, it's considered "supported"
                m_enabledTransformationList.add(transform);
            }
            else
            { // This particular transformation is not supported for this item
                if (tm.transformationListenerRegistered())
                {
                    if (log.isInfoEnabled()) 
                    {
                        log.info(m_logPrefix + "filterAndSaveTransformations: Signaling transformation failed for nonmatching input profile "
                                             + inputProfile );
                    }
                    tm.enqueueTransformationFailedNotification( 
                            this, transform, TransformationListener.REASON_NONMATCHING_INPUT_PROFILE );
                }
            }
        }
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
        boolean retVal = false;

        if (removeRecordTask())
        {
            // only remove RecordSchedule if it is not contained by a
            // NetRecordingEntry
            if (getNetRecordingEntry() == null)
            {
                removeRecordSchedule();
                retVal = true;
            }
               
        }

        retVal = retVal && removeFromNetRecordingEntry();

        if (retVal)
        {
            retVal = ((ContentContainerImpl) c).removeEntry(this);
        }

        return retVal;
    }

    /**
     * Returns the RecordSchedule that is associated with this instance
     */
    RecordSchedule getRecordSchedule()
    {
        return recordSchedule;
    }

    /**
     * Associates the NetRecordingEntryLocal with this instance Called by
     * NetRecordingEntryLocal.addRecordingContentItem()
     */
    void addNetRecordingEntry(NetRecordingEntryLocal nre)
    {
        netRecordingEntry = nre;
        recordSchedule = netRecordingEntry.getRecordSchedule();

        MetadataNodeImpl netRecordingEntryMetadataNode = (MetadataNodeImpl) netRecordingEntry.getRootMetadataNode();

        MetadataNodeImpl thisMetadataNode = (MetadataNodeImpl) getRootMetadataNode();

        String cdsNetRecordingEntry = (String) netRecordingEntryMetadataNode.getMetadata(UPnPConstants.QN_OCAP_SCHEDULED_CDS_ENTRY_ID);

        if (cdsNetRecordingEntry != null && !cdsNetRecordingEntry.equals(""))
        {
            thisMetadataNode.addMetadataRegardless(UPnPConstants.QN_OCAP_NET_RECORDING_ENTRY, cdsNetRecordingEntry);
        }

        String srsRecordScheduleID = (String) netRecordingEntryMetadataNode.getMetadata(UPnPConstants.QN_UPNP_SRS_RECORD_SCHEDULE_ID);

        if (srsRecordScheduleID != null && !srsRecordScheduleID.equals(""))
        {
            thisMetadataNode.addMetadataRegardless(UPnPConstants.QN_UPNP_SRS_RECORD_SCHEDULE_ID, srsRecordScheduleID);
            thisMetadataNode.addMetadataRegardless(UPnPConstants.QN_SRS_RECORD_SCHEDULE_ID, srsRecordScheduleID);
        }
    }

    /**
     * Remove the association between this instance and a NetRecordingEntryLocal
     *
     * @param nre the netRecordingEntry
     */
    void removeNetRecordingEntry(NetRecordingEntry nre)
    {
        // only remove if there was an existing association
        if (netRecordingEntry != null && netRecordingEntry == nre)
        {
            netRecordingEntry = null;
            recordSchedule = null;
        }
    }

    // Implementation of LocalRecordingContentItem method
    public boolean removeFromNetRecordingEntry()
    {
        if (log.isDebugEnabled())
        {
            log.debug("In removeFromNetRecordingEntry");
        }

        boolean retVal = true;
        NetRecordingEntry nre = getNetRecordingEntry();
        if (removeRecordTask())
        {
            // only remove RecordSchedule if it is not contained by a
            // NetRecordingEntry
            if (nre == null)
            {
                removeRecordSchedule();
            }
               
        }
        if (nre != null)
        {
            //OCSPEC-342 ..remove from NetRecordingEntry
            try 
            {
                nre.removeRecordingContentItem(this);
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn("Error removing from NetRecordingEntry", e);
                }
                retVal = false;
            }
            
        }
        return retVal;
    }

    /**
     * This function will remove the given ContentResource from the RecordingContentItem
     * and return true if the resource was present and removed.
     * 
     * @param lrcir The LocalRecordingContentItemResource requesting the removal
     * @return true if the resource was present and removed, false otherwise
     */
    public boolean deleteContentResource(LocalRecordingContentItemResource lrcir)
    {
        synchronized (m_sync)
        {
            final String nativeProfile = lrcir.getNativeProfile();
            final OutputVideoContentFormatExt outputContentFormat = lrcir.getOutputVideoContentFormat();
            final boolean resourceRemoved = m_resourceList.remove(lrcir);
            if (nativeProfile != null)
            {
                final boolean profileRemoved = m_enabledProfileList.remove(nativeProfile);
                if (hasPlayableRecordedService())
                {
                    updateRecordingMetadataValues();
                }
                return (resourceRemoved || profileRemoved);
            }
            else if (outputContentFormat != null)
            {
                Transformation transformationWithProfile 
                                    = findTransformationContaining(outputContentFormat);
                boolean transformationRemoved 
                            = m_enabledTransformationList.remove(transformationWithProfile);
                if (hasPlayableRecordedService())
                {
                    updateRecordingMetadataValues();
                }
                return (resourceRemoved || transformationRemoved);
            }
            else
            {
                return false;
            }
        } // END synchronized (m_sync)
    } // END deleteContentResource()
    
    private Transformation findTransformationContaining(OutputVideoContentFormatExt outputContentFormat)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        if (outputContentFormat == null)
        {
            return null;
        }
        
        Iterator it = m_enabledTransformationList.iterator();
        while (it.hasNext())
        {
            Transformation transformation = (Transformation)it.next();
            ContentFormat ocf = transformation.getOutputContentFormat();
            if (outputContentFormat.equals(ocf))
            {
                return transformation;
            }
        }
        // Went through the whole list and didn't find it
        return null;
    } // END findTransformationContaining

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append("RecordingContentItemLocal [");
        sb.append("id = ");
        sb.append(getID());
        sb.append(", ");

        sb.append("CDS RecordingContentItem = ");
        sb.append(recordingContentItem);

        // TODO: add other member variables

        sb.append(", ");
        sb.append(super.toString());
        sb.append("]");

        return sb.toString();
    }

    // //////////////////////////////////////////////////////////////////
    //
    // Private
    //
    // //////////////////////////////////////////////////////////////////

    private static final MetadataNodeImpl metadataNode()
    {
        MetadataNodeImpl mn = new MetadataNodeImpl();

        mn.setAppID(Utils.getCallerAppID());

        return mn;
    }

    private static final MetadataNodeImpl metadataNode(AppID appID)
    {
        MetadataNodeImpl mn = new MetadataNodeImpl();

        mn.setAppID(appID);

        return mn;
    }
    /**
     * Initializes the static structures for this class
     */
    private static void initStaticStructures()
    {
        //
        // Initialize static allowed properties Sets once for all instances
        //

        // TODO: need to create a set of multi-value properties to be used to
        // build a Vector of values
        // This Set contains all of the allowed property names that can be used
        // to store property values
        // for the RecordTask usage
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_ID_ATTR);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TITLE);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_CLASS);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_PRIORITY);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_RECORD_DESTINATION);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_RECORD_SCHEDULE_ID);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TASK_CHANNEL_ID);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TASK_CHANNEL_ID_TYPE_ATTR);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TASK_START_DATE_TIME);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TASK_DURATION);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_RECORD_QUALITY);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_RECORD_QUALITY_TYPE_ATTR);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TASK_STATE);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_FATAL_ERROR);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_BITS_RECORDED);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_BITS_MISSING);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_INFO_LIST);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_CURRENT_ERRORS);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_PENDING_ERRORS);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_RECORDING);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_PHASE);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_ERROR_HISTORY);

        /*
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_DESTINATION);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_ACCESS_PERMISSIONS);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_APP_ID);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_CONTENT_URI);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_SCHEDULED_DURATION);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_EXPIRATION_PERIOD);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_MSO_CONTENT_INDICATOR);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_ORGANIZATION);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_MEDIA_PRESENTATION_POINT);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_PRIORITY_FLAG);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_TASK_STATE);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_RETENTION_PRIORITY);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_SCHEDULED_CHANNEL_ID);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_SCHEDULED_CHANNEL_ID_TYPE);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_SPACE_REQUIRED);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_SCHEDULE_START_DATE_TIME);
        ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_OCAP_MEDIA_FIRST_TIME);
        */
        //ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.RESOURCE_SIZE);
        //ALLOWEDPROPERTYKEYSFORTASK.add(UPnPConstants.RESOURCE_DURATION);

        // This Set contains all of the required property names that are used
        // to store property values
        // for the RecordSchedule usage
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_ID_ATTR);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TITLE);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_CLASS);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_PRIORITY);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_RECORD_DESTINATION);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_RECORD_SCHEDULE_ID);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TASK_CHANNEL_ID);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TASK_CHANNEL_ID_TYPE_ATTR);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TASK_START_DATE_TIME);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TASK_DURATION);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_RECORD_QUALITY);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_RECORD_QUALITY_TYPE_ATTR);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_TASK_STATE);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_FATAL_ERROR);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_BITS_RECORDED);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_BITS_MISSING);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_INFO_LIST);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_CURRENT_ERRORS);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_PENDING_ERRORS);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_RECORDING);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_PHASE);
        REQUIREDPROPERTYKEYSFORTASK.add(UPnPConstants.QN_SRS_ERROR_HISTORY);
    }

    // association between RecordTask and the RecordingContentItem
    private RecordTask recordTask = null;

    // the RecordSchedule that this item/task is contained in
    private RecordSchedule recordSchedule = null;

    // the NetRecordingEntry this instance is contained in
    private NetRecordingEntryLocal netRecordingEntry = null;

    // Log4J logger.
    private static final Logger log = Logger.getLogger(RecordingContentItemLocal.class);

    // the CDS representation of this instance
    private final RecordingContentItemImpl recordingContentItem;

    // Keeps track of the number of occurrences where this item is being
    // streamed
    private int inUseCnt = 0;

    static
    {
        initStaticStructures();
    }
}
