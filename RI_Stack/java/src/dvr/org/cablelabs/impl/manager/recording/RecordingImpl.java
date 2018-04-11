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

package org.cablelabs.impl.manager.recording;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIChangeEvent;
import javax.tv.service.SIElement;
import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.ServiceNumber;
import javax.tv.service.selection.InvalidServiceComponentException;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceMediaHandler;

import org.apache.log4j.Logger;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceManagerImpl;
import org.cablelabs.impl.davic.resources.ResourceOfferListener;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.DVRStorageManager;
import org.cablelabs.impl.manager.DisableBufferingListener;
import org.cablelabs.impl.manager.EventDispatchManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.manager.RecordingDBManager;
import org.cablelabs.impl.manager.RecordingExt;
import org.cablelabs.impl.manager.TimeShiftBuffer;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.TimeShiftWindowChangedListener;
import org.cablelabs.impl.manager.TimeShiftWindowClient;
import org.cablelabs.impl.manager.TimeShiftWindowStateChangedEvent;
import org.cablelabs.impl.manager.ed.EDListener;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEvent;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventChangeListener;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventStoreChange;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventStoreChangeImpl;
import org.cablelabs.impl.manager.lightweighttrigger.LightweightTriggerEventStoreWrite;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientAddedEvent;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver;
import org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientRemovedEvent;
import org.cablelabs.impl.manager.lightweighttrigger.SequentialMediaTimeStrategy;
import org.cablelabs.impl.manager.service.SISnapshotManager;
import org.cablelabs.impl.manager.timeshift.TimeShiftManagerImpl;
import org.cablelabs.impl.media.access.CopyControlInfo;
import org.cablelabs.impl.media.player.AbstractDVRServicePlayer;
import org.cablelabs.impl.media.player.RecordedServicePlayer;
import org.cablelabs.impl.ocap.OcapMain;
import org.cablelabs.impl.ocap.dvr.RecordingResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.ocap.si.ProgramAssociationTableManagerImpl;
import org.cablelabs.impl.ocap.si.ProgramMapTableManagerImpl;
import org.cablelabs.impl.recording.RecordedSegmentInfo;
import org.cablelabs.impl.recording.RecordedServiceComponentInfo;
import org.cablelabs.impl.recording.RecordingInfo2;
import org.cablelabs.impl.recording.RecordingInfoNode;
import org.cablelabs.impl.recording.TimeAssociatedDetailsInfo;
import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceComponentExt;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServiceDetailsExt;
import org.cablelabs.impl.service.ServiceExt;
import org.cablelabs.impl.service.javatv.navigation.ServiceDetailsImpl;
import org.cablelabs.impl.service.javatv.selection.ServiceContextCallback;
import org.cablelabs.impl.service.javatv.selection.ServiceContextImpl;
import org.cablelabs.impl.spi.SPIService;
import org.cablelabs.impl.spi.ProviderInstance;
import org.cablelabs.impl.spi.ProviderInstance.SelectionSessionWrapper;
import org.cablelabs.impl.storage.DVRStorageManagerEvent;
import org.cablelabs.impl.storage.MediaStorageVolumeExt;
import org.cablelabs.impl.storage.MediaStorageVolumeImpl;
import org.cablelabs.impl.storage.StorageProxyImpl;
import org.cablelabs.impl.util.GenericTimeAssociatedElement;
import org.cablelabs.impl.util.LightweightTriggerEventTimeTable;
import org.cablelabs.impl.util.LocatorUtil;
import org.cablelabs.impl.util.MediaStreamType;
import org.cablelabs.impl.util.PidMapEntry;
import org.cablelabs.impl.util.PidMapTable;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SimpleCondition;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.util.TimeTable;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NoFreeInterfaceException;
import org.davic.resources.ResourceStatusEvent;
import org.dvb.application.AppID;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.TimeShiftProperties;
import org.ocap.dvr.storage.FreeSpaceListener;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.net.OcapLocator;
import org.ocap.service.AbstractService;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.DeletionDetails;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.shared.dvr.RecordingPermission;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.SegmentedRecordedService;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.si.ProgramAssociationTable;
import org.ocap.si.ProgramAssociationTableManager;
import org.ocap.si.ProgramMapTableManager;
import org.ocap.si.TableChangeListener;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageManagerEvent;
import org.ocap.storage.StorageManagerListener;
import org.ocap.storage.StorageProxy;

/**
 * Class that implements the mechanics of saving and playing a recording.
 * <p>
 * <b>Note:</b>Not serialized.
 */

public class RecordingImpl extends RecordingRequestImpl implements RecordingImplInterface, OcapRecordingRequest,
        RecordingExt, Comparable, TimeShiftWindowChangedListener, TableChangeListener,
        DVRStorageManager.MediaStorageVolumeUpdateListener, DisableBufferingListener,
        RecordingManagerImpl.RecordingDisabledListener, LightweightTriggerEventStoreWrite,
        LightweightTriggerEventStoreChange, StorageManagerListener, ServiceContextCallback,
        Asserting
{
    /**
     * Event code to indicate that this recording has run out of disk space
     * TODO: integrate Java event codes w/ MPE level definitions
     */
    static final int MPE_DVR_EVT_OUT_OF_SPACE = 0x1000;

    static final int MPE_DVR_EVT_CONVERSION_STOP = 0x1003;

    static final int MPE_DVR_EVT_PLAYBACK_PID_CHANGE = 0x1004;

    static final int MPE_DVR_EVT_SESSION_CLOSED = 0x1005;

    /**
     * Native DVR error codes
     */
    static final int MPE_DVR_ERR_NOERR = 0x00; // no error

    static final int MPE_DVR_ERR_INVALID_PID = 0x01; // invalid pid error

    static final int MPE_DVR_ERR_INVALID_PARAM = 0x02; // a parameter is invalid

    static final int MPE_DVR_ERR_OS_FAILURE = 0x03; // error occured at the
                                                    // OSlevel

    static final int MPE_DVR_ERR_PATH_ENGINE = 0x04; // operation is notm
                                                     // supported

    static final int MPE_DVR_ERR_UNSUPPORTED = 0x05; // operation is notm
                                                     // supported

    static final int MPE_DVR_ERR_NOT_ALLOWED = 0x06; // operation is not alowed

    static final int MPE_DVR_ERR_DEVICE_ERR = 0x07; // hardware device error

    static final int MPE_DVR_ERR_OUT_OF_SPACE = 0x08; // no more space on the
                                                      // HDD

    static final int MPE_DVR_ERR_NOT_IMPLEMENTED = 0x09; // no more space on the
                                                         // HDD

    static final int MPE_DVR_ERR_NO_ACTIVE_SESSSION = 0x0a; // no more space on
                                                            // the HDD

    private static final long NANOS_PER_MILLI = 1000000;

    // This priority should be defined elsewhere!!
    // Used while registering for PAT/PMT changes
    static final int RECORDING_PRIORITY = 10;

    static RecordingFailedException RFE = new RecordingFailedException();

    /**
     * Constructor:
     */
    protected RecordingImpl()
    {
    }

    /**
     * Constructor:
     */
    protected RecordingImpl(LocatorRecordingSpec lrs, RecordingDBManager rdbm,
            RecordingManagerInterface recordingManager)
    {
        m_logPrefix = "RI 0x" + Integer.toHexString(this.hashCode()) + ": ";
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "LRS constructor: " + lrs + "\n");
        }

        m_rdbm = rdbm;
        m_info = m_rdbm.newRecord();
        m_info.setRequestType(RecordingInfo2.SPECTYPE_LOCATOR);
        m_info.setServiceNumber(RecordingInfo2.SERVICENUMBER_UNDEFINED); // ServiceNumber only needed for SRS
        m_info.setFailedExceptionReason(RFE.REASON_NOT_KNOWN);
        m_recordingSpec = lrs;
        OcapLocator serviceLocator[] = new OcapLocator[lrs.getSource().length];
        for (int i = 0; i < lrs.getSource().length; i++)
        {
            serviceLocator[i] = (OcapLocator) lrs.getSource()[i];
            if (log.isDebugEnabled())
            {
                log.debug("LocatorUtil.isServiceComponent(serviceLocator[" + i + "]) returned : "
                        + LocatorUtil.isServiceComponent(serviceLocator[i]) + "\n");
            }
        }
        m_info.setServiceLocators(serviceLocator);
        m_info.setRequestedStartTime(lrs.getStartTime().getTime());
        m_info.setRequestedDuration(lrs.getDuration());

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        m_info.setAppId((AppID) ccm.getCurrentContext().get(CallerContext.APP_ID));

        m_info.setExpirationDate(
                new Date( RecordingManagerImpl.roundOffOverflow( lrs.getProperties().getExpirationPeriod(),
                                                                 m_info.getRequestedStartTime() ) ) );

        if (lrs.getProperties() instanceof OcapRecordingProperties)
        {
            OcapRecordingProperties orp = (OcapRecordingProperties) lrs.getProperties();

            m_info.setBitRate(orp.getBitRate());
            m_info.setPriority(orp.getPriorityFlag());
            m_info.setRetentionPriority(orp.getRetentionPriority());
            m_info.setFap(orp.getAccessPermissions() == null ? getDefaultFAP() : orp.getAccessPermissions());
            m_info.setOrganization(orp.getOrganization());
            m_info.setDestination(orp.getDestination());
            m_info.setResourcePriority(orp.getResourcePriority());
        }
        else
        {
            m_info.setBitRate(OcapRecordingProperties.HIGH_BIT_RATE);
            m_info.setPriority(OcapRecordingProperties.RECORD_WITH_CONFLICTS);
            m_info.setRetentionPriority(OcapRecordingProperties.DELETE_AT_EXPIRATION);
            m_info.setFap(getDefaultFAP());
            m_info.setOrganization(null);
            m_info.setDestination(null);
            m_info.setResourcePriority(0);
            // TODO: uses internal device only!
        }
        m_info.setState(INIT_STATE);
        m_recordingManager = recordingManager;
        m_sync = recordingManager;

        m_scheduledServiceContext = null;
        m_tswClient = null;
        m_tsMgr = (TimeShiftManager) ManagerManager.getInstance(TimeShiftManager.class);

        // default setting.
        m_info.setDeletionReason(DeletionDetails.USER_DELETED);
        m_resourceUsage = createResourceUsage();

        // Set the time created.
        m_timeCreated = System.currentTimeMillis();
    }

    /**
     * Constructor:
     */
    protected RecordingImpl(ServiceRecordingSpec srs, RecordingDBManager rdbm,
            RecordingManagerInterface recordingManager)
    {
        m_logPrefix = "RI 0x" + Integer.toHexString(this.hashCode()) + ": ";
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "SRS constructor: " + srs + "\n");
        }

        m_rdbm = rdbm;
        m_info = m_rdbm.newRecord();
        m_info.setRequestType(RecordingInfo2.SPECTYPE_SERVICE);
        m_info.setFailedExceptionReason(RFE.REASON_NOT_KNOWN);
        m_recordingSpec = srs;
        OcapLocator serviceLocator[] = new OcapLocator[1];
        try
        {
            Service specService = srs.getSource();
            if (specService instanceof ServiceExt)
            {
                serviceLocator[0] = (OcapLocator)((ServiceExt)srs.getSource()).getDetails().getLocator();
            }
            else
            {
                serviceLocator[0] = new OcapLocator(specService.getLocator().toExternalForm());
            }

            if (specService instanceof ServiceNumber)
            {
                m_info.setServiceNumber(((ServiceNumber)specService).getServiceNumber());
            }
            else
            {
                m_info.setServiceNumber(RecordingInfo2.SERVICENUMBER_UNDEFINED);
            }
        }
        catch (Exception e)
        {
            SystemEventUtil.logRecoverableError(e);
            throw new IllegalArgumentException("Could not dereference Service object. Reason: " + e.getMessage());
        }

        m_info.setServiceLocators(serviceLocator);
        m_info.setRequestedStartTime(srs.getStartTime().getTime());
        m_info.setRequestedDuration(srs.getDuration());

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        m_info.setAppId((AppID) ccm.getCurrentContext().get(CallerContext.APP_ID));

        // Make sure the ED manager framework is active.
        ManagerManager.getInstance(EventDispatchManager.class);

        m_info.setExpirationDate(new Date(RecordingManagerImpl.roundOffOverflow(srs.getProperties()
                .getExpirationPeriod(), m_info.getRequestedStartTime())));
        if (srs.getProperties() instanceof OcapRecordingProperties)
        {
            OcapRecordingProperties orp = (OcapRecordingProperties) srs.getProperties();

            m_info.setBitRate(orp.getBitRate());
            m_info.setPriority(orp.getPriorityFlag());
            m_info.setRetentionPriority(orp.getRetentionPriority());
            m_info.setFap(orp.getAccessPermissions() == null ? getDefaultFAP() : orp.getAccessPermissions());
            m_info.setOrganization(orp.getOrganization());
            m_info.setDestination(orp.getDestination());
            m_info.setResourcePriority(orp.getResourcePriority());
        }
        else
        {
            m_info.setBitRate(OcapRecordingProperties.HIGH_BIT_RATE);
            m_info.setPriority(OcapRecordingProperties.RECORD_WITH_CONFLICTS);
            m_info.setRetentionPriority(OcapRecordingProperties.DELETE_AT_EXPIRATION);
            m_info.setFap(getDefaultFAP());
            m_info.setOrganization(null);
            m_info.setDestination(null);
            m_info.setResourcePriority(0);
        }
        m_info.setState(INIT_STATE);
        m_recordingManager = recordingManager;
        m_sync = recordingManager;

        m_scheduledServiceContext = null;
        m_tswClient = null;
        m_tsMgr = (TimeShiftManager) ManagerManager.getInstance(TimeShiftManager.class);

        // default setting.
        m_info.setDeletionReason(DeletionDetails.USER_DELETED);
        m_resourceUsage = createResourceUsage();

        // Set the time created.
        m_timeCreated = System.currentTimeMillis();
    }

    /**
     * Constructor:
     */
    protected RecordingImpl(ServiceContextRecordingSpec scrs, RecordingDBManager rdbm,
            RecordingManagerInterface recordingManager)
    {
        m_logPrefix = "RI 0x" + Integer.toHexString(this.hashCode()) + ": ";
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "SCRS constructor: " + scrs + "\n");
        }
        m_rdbm = rdbm;

        // Validate the state of the ServiceContext
        ServiceContext sctx = scrs.getServiceContext();
        if (sctx == null || sctx.getService() == null || (sctx.getService() instanceof AbstractService))
        {
            throw new IllegalArgumentException("ServiceContext is invalid or presenting an unrecordable Service");
        }

        // Check to see that we have a valid service and that it's not a
        // recorded service
        // is done in IStatepending:handleStart()

        // proceed with initialization
        m_info = m_rdbm.newRecord();
        m_recordingSpec = scrs;
        m_info.setRequestType(RecordingInfo2.SPECTYPE_SERVICECONTEXT);
        m_info.setServiceNumber(RecordingInfo2.SERVICENUMBER_UNDEFINED); // Only needed for SRS
        m_info.setFailedExceptionReason(RFE.REASON_NOT_KNOWN);

        OcapLocator[] serviceLocator = null;
        m_info.setServiceLocators(serviceLocator);
        m_scheduledServiceContext = (ServiceContextExt) scrs.getServiceContext();
        m_tswClient = null;
        m_tsMgr = (TimeShiftManager) ManagerManager.getInstance(TimeShiftManager.class);

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        m_info.setAppId((AppID) ccm.getCurrentContext().get(CallerContext.APP_ID));

        m_info.setRequestedStartTime(scrs.getStartTime().getTime());
        m_info.setRequestedDuration(scrs.getDuration());
        m_info.setExpirationDate(new Date(RecordingManagerImpl.roundOffOverflow(scrs.getProperties()
                .getExpirationPeriod(), m_info.getRequestedStartTime())));

        if (scrs.getProperties() instanceof OcapRecordingProperties)
        {
            OcapRecordingProperties orp = (OcapRecordingProperties) scrs.getProperties();

            m_info.setBitRate(orp.getBitRate());
            m_info.setPriority(orp.getPriorityFlag());
            m_info.setRetentionPriority(orp.getRetentionPriority());
            m_info.setFap(orp.getAccessPermissions() == null ? getDefaultFAP() : orp.getAccessPermissions());
            m_info.setOrganization(orp.getOrganization());
            m_info.setDestination(orp.getDestination());
            m_info.setResourcePriority(orp.getResourcePriority());
        }
        else
        {
            m_info.setBitRate(OcapRecordingProperties.HIGH_BIT_RATE);
            m_info.setPriority(OcapRecordingProperties.RECORD_WITH_CONFLICTS);
            m_info.setRetentionPriority(OcapRecordingProperties.DELETE_AT_EXPIRATION);
            m_info.setFap(getDefaultFAP());
            m_info.setOrganization(null);
            m_info.setDestination(null);
            m_info.setResourcePriority(0);
            // TODO: uses internal device only!
        }
        m_info.setState(INIT_STATE);
        m_recordingManager = recordingManager;
        m_sync = recordingManager;

        // default setting.
        m_info.setDeletionReason(DeletionDetails.USER_DELETED);
        m_resourceUsage = createResourceUsage();

        // Set the time created.
        m_timeCreated = System.currentTimeMillis();
    }

    /**
     * Construct a RecordingImpl from a RecordingInfo record.
     */
    protected RecordingImpl(RecordingInfo2 info, RecordingDBManager rdbm, RecordingManagerInterface recordingManager)
            throws IllegalArgumentException
    {
        m_logPrefix = "RI 0x" + Integer.toHexString(this.hashCode()) + ": ";
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "RI2 constructor: " + info + "\n");
        }
        m_rdbm = rdbm;
        m_info = info;

        // The ServiceContext associated with this RecordingRequest is GONE after reboot
        m_scheduledServiceContext = null;

        m_tswClient = null;
        m_recordingManager = recordingManager;
        m_sync = recordingManager;
        m_tsMgr = (TimeShiftManager) ManagerManager.getInstance(TimeShiftManager.class);

        // Leverage the code to regenerate the RecordingSpec/RecordingProperties from metadata
        m_recordingSpec = getRecordingSpec();

        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "constructor RecordingInfo2: Reconstituted " + m_recordingSpec);
        }

        // Set the time created to 0L b/c the recording was
        // created from persistent storage; therefore, we are unable
        // to determine the creation time.
        m_timeCreated = 0L;

        // NOTE: initalizeForState() needs to be called to fully initialize the
        // RecordingImpl
    } // END RecordingImpl(RecordingInfo info, RecordingDBManager rdbm, Object

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        // Read default fields
        in.defaultReadObject();
        this.m_lastSegmentCached = -1;
        this.m_lwteCache = null;
        this.m_srcToPendingEvents = null;
    }

    /**
     * Complete initialization of the RecordingImpl according to the set state
     * and persist the recording. This is where the recording's internal state
     * shall be set, recorded service created ,and resource usage created.
     *
     * This MUST be called prior to passing off the RecordingImpl reference.
     *
     */
    public void initializeForState()
    {
        if (ASSERTING)
            Assert.lockHeld(m_sync);

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "initializeForState state: " + externalStateToString(m_info.getState()) + "\n");
        }
        // Perform state-specific initialization
        switch (m_info.getState())
        {
            case INIT_STATE:
            case DESTROYED_STATE:
            {
                throw new IllegalStateException("RI has not had its state set to a valid initial state.");
            }
            case PENDING_NO_CONFLICT_STATE:
            case PENDING_WITH_CONFLICT_STATE:
            case IN_PROGRESS_STATE:
            case IN_PROGRESS_INCOMPLETE_STATE:
            case IN_PROGRESS_WITH_ERROR_STATE:
            {
                m_segmentedRecordedService = new SegmentedRecordedServiceImpl(this, m_sync);
                m_istate = new IStatePending();
                m_resourceUsage = createResourceUsage();
                break;
            }
            case COMPLETED_STATE:
            case INCOMPLETE_STATE:
            case FAILED_STATE:
            {
                // Set the associated SegmentedRecordedService object
                m_segmentedRecordedService = new SegmentedRecordedServiceImpl(this, m_sync);
                // FALL THROUGH
            }
            case DELETED_STATE:
            {
                // If we've determined that this recording is complete,
                // incomplete, failed, or deleted state set it to a completed
                // state
                m_istate = new IStateEnded(false, false);
                break;
            }
            case CANCELLED_STATE:
            {
                m_istate = new IStateEnded(true, false);
                break;
            }
            case TEST_STATE:
            {
                // TEST recordings should never execute - so we put them in
                // the Ended internal state. However, they may be used in RCH
                // invocation - se we'll need a recording usage
                m_istate = new IStateEnded(false, false);
                m_resourceUsage = createResourceUsage();
                break;
            }
        } // END switch (m_info.getState())

        saveRecordingInfo(RecordingDBManager.ALL);
    } // END initializeForState()

    public void saveRecordingInfo(int updateFlag)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + " saveRecordingInfo: " + this);
        }

        super.saveRecordingInfo(updateFlag);
    }

    private boolean attachToTSW()
    {
        try
        {
            final int uses = TimeShiftManager.TSWUSE_BUFFERING
                             | TimeShiftManager.TSWUSE_RECORDING
                             | TimeShiftManager.TSWUSE_NIRES;

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "attachForBufferingAndRecording: TimeShiftWindow attachFor ("
                        + TimeShiftManagerImpl.useString(uses) + ") ");
            }
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "attachForBufferingAndRecording: m_tswClient: " + m_tswClient);
            }

            // Attach to timeshiftwindow for recording
            m_tswClient.attachFor(uses);

            return true;
        }
        catch (Exception e)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "attachForBufferingAndRecording: Unexpected Exception", e);
            }
            removeTableChangeListeners();

            IStateEnded endState = new IStateEnded(false, false);
            m_istate.setState(endState);
            RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);

            setStateAndNotify(endState.getCompletionStateCode());
        }

        return false;
    } // END attachToTSW()()

    /**
     * Perform start recording. At this point in time, we may be tuned but the
     * tuner may not necessarily be ready to attach the TimeShiftWindow for
     * recording. attachFor() will invoke the tune, if necessary.
     * @param convertStartTimeMs TODO
     *
     * @return state of the recording
     */
    private void startRecording(long convertStartTimeMs, boolean stateNotify)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "startRecording(convertStart " + new Date(convertStartTimeMs)
                     +", notify " + stateNotify + ") entered");
        }

        // Check to see if we are ready, if not set to proper state
        int state = m_tswClient.getState();
        if (state == TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER)
        {
            setFailedExceptionReason(RecordingFailedException.SERVICE_VANISHED);
            m_istate.setState(new IStateSuspendedTunerNotReady());
            setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
            return;
        }

        // Check to see if the MediaStorage Volume is online, if not we
        // go to the proper suspended state
        MediaStorageVolumeImpl msv = (MediaStorageVolumeImpl)m_info.getDestination();
        if (msv == null || (msv.getStorageProxy().getStatus() != StorageProxy.READY))
        {
            // if not, we need to set ourselves to to the suspended state
            setFailedExceptionReason(RecordingFailedException.RESOURCES_REMOVED);
            m_istate.setState(new IStateSuspendedMSVUnavailable());
            RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
            if (stateNotify)
                setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
            else
                setStateNoNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
            return;
        }

        int failedExceptionReason = RFE.REASON_NOT_KNOWN;

        // verify that recording is enabled
        if (m_recordingManager.isBufferingEnabled() == false)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "Cannot record - buffering is not enabled");
            }
            failedExceptionReason = RecordingFailedException.INSUFFICIENT_RESOURCES;
        }
        if (m_recordingManager.isRecordingEnabled() == false)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "Cannot record - recording is not enabled");
            }
            failedExceptionReason = RecordingFailedException.INSUFFICIENT_RESOURCES;
        }
        if (hasMSVAccess() == false)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "Cannot record - insufficient rights for destination MSV");
            }
            failedExceptionReason = RecordingFailedException.RESOURCES_REMOVED;
        }

        if (failedExceptionReason != RFE.REASON_NOT_KNOWN)
        {
            setFailedExceptionReason(failedExceptionReason);
            m_istate.setState(new IStateSuspendedBufferingDisabled());
            RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
            setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
            return;
        }

        TimeShiftBuffer currentTSB = null;
        boolean isTSBAttached = false;
        boolean isCopyProtected = false;
        boolean isCopyOnce = false;
        boolean isOngoingConversion = false;
        int convertRetCode = MPE_DVR_ERR_NOERR;

        try
        {
            // Service components here should be retrieved from
            // the time shift window to be matched against
            // broadcast components. Start time of the recording may be in
            // the past which means that the recording locators need to be
            // compared retroactively with servicedetails in the past.
            // Retrieve time shift window components returns a 'TimeTable'
            // which contains 'n' time table elements. Each time table
            // element contains one mediaTime and a vector of ServiceComponents

            // Remaining start time is the difference between the scheduled end time and
            //  the requested convert start time
            long remainingDurMs = (m_info.getRequestedStartTime()+m_info.getRequestedDuration())
                                  -convertStartTimeMs;

            // Walk the TSBs for the recording's time range
            Enumeration tsbe = m_tswClient.getTSBsForSystemTimeSpan(convertStartTimeMs, remainingDurMs);

            if (!tsbe.hasMoreElements())
            {
                if (log.isWarnEnabled())
                {
                    log.warn(m_logPrefix + "startRecording: Found 0 overlapping TSBs!");
                }
            }

            // There will be 1 conversion session per TSB
            // Setup the TimeTable that will be used to convert
            TimeTable convertTT = new TimeTable();

            //
            // Loop through all TSBs in the TimeShiftWindow and
            // INITIATE CONVERT and create a new RECORDED SERVICE for each
            // TSB
            // Note: The last one (and only the last one) may be on-going
            //
            while (tsbe.hasMoreElements())
            {
                currentTSB = (TimeShiftBuffer) tsbe.nextElement();
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "startRecording: processing overlapping tsb: " + currentTSB.toString());
                }

                m_tswClient.attachToTSB(currentTSB);
                isTSBAttached = true;

                final long convertEndTimeMs = Math.min( (convertStartTimeMs + remainingDurMs),
                                                        currentTSB.getSystemEndTime() );

                // Calculate the media time in the TSB that corresponds to the recStart/End (in ms)
                final long convertStartTimeInTSBms = Math.max( 0, convertStartTimeMs
                                                                  - currentTSB.getSystemStartTime());
                final long convertEndTimeInTSBms = convertEndTimeMs - currentTSB.getSystemStartTime();

                if (log.isDebugEnabled())
                {
                    log.debug( m_logPrefix + "startRecording: recording overlap in TSB: "
                               + (convertEndTimeInTSBms-convertStartTimeInTSBms) + "ms ("
                               + convertStartTimeInTSBms + "ms to " + convertEndTimeInTSBms + "ms)");
                }

                // Copy/rebase CopyControlInfo indications from TSB CCI
                //  TimeTable into the recording segment's CCI TimeTable
                TimeTable tsbCCITable = currentTSB.getCCITimeTable().subtableFromTimespan(
                                          convertStartTimeInTSBms*SequentialMediaTimeStrategy.MS_TO_NS,
                                          (convertEndTimeInTSBms-convertStartTimeInTSBms)
                                            * SequentialMediaTimeStrategy.MS_TO_NS,
                                          true );
                Enumeration cciEnum = tsbCCITable.elements();
                TimeTable cciTT = new TimeTable();

                while (cciEnum.hasMoreElements())
                {
                    CopyControlInfo tsbCCI = (CopyControlInfo)(cciEnum.nextElement());
                    final long recTimeOffsetNs
                                 = Math.max( 0,
                                             tsbCCI.getTimeNanos()
                                             - ( convertStartTimeInTSBms
                                                 * SequentialMediaTimeStrategy.MS_TO_NS ) );

                    CopyControlInfo recCCI = new CopyControlInfo( recTimeOffsetNs,
                                                                  tsbCCI.getCCI() );
                    cciTT.addElement(recCCI);
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "startRecording: Transferred "
                                  + tsbCCI + " from TSB into recorded segment: "
                                  + recCCI );
                    }

                    byte emi = tsbCCI.getEMI();
                    switch (emi)
                    {
                        case CopyControlInfo.EMI_COPY_NEVER:
                        case CopyControlInfo.EMI_COPY_NO_MORE:
                        {
                            isCopyProtected = true;
                            break;
                        }
                        case CopyControlInfo.EMI_COPY_ONCE:
                        {
                            isCopyOnce = true;
                            break;
                        }
                        case CopyControlInfo.EMI_COPY_FREELY:
                        {
                            // Ignore
                            break;
                        }
                        default:
                        {
                            if (log.isWarnEnabled())
                            {
                                log.warn(m_logPrefix + "startRecording: Found unknown CCI/EMI value 0x"
                                         + Integer.toHexString(emi) + " at media time "
                                         + tsbCCI.getTimeMillis() + "ms in " + currentTSB );
                            }
                        }
                    } // END switch (emi)
                } // END while (cciEnum.hasMoreElements())

                if (isCopyProtected && !m_recordingManager.isContentHostBound())
                {
                    if (log.isInfoEnabled())
                    {
                        log.info( m_logPrefix + "startRecording: Content is not host-bound and CCI indicates TSB content is not copyable in "
                                  + currentTSB );
                    }
                    isOngoingConversion = false;
                    m_tswClient.detachFromTSB(currentTSB);
                    continue;
                }

                // Assert: All preconditions for conversion are met

                // Get the component TimeTable from the tsb
                TimeTable tsbCompTT = currentTSB.getPidMapTimeTable();

                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "startRecording: TSB component timetable: " + tsbCompTT.toString());
                }

                // Clear the conversion TimeTable
                convertTT.removeAllElements();

                //
                // Convert PidMapTables from TSB into the PidMapTables that
                // we need to pass to the native convert
                //

                {
                    // Get the PIDMapTable that immediately precedes the recording start time
                    // This will be the basis for the 0-time entry for the conversion PidMapTable
                    // Note: We want the entry preceding or at the start time - so add 1
                    //       in case they're equal.
                    GenericTimeAssociatedElement preceedingElem = (GenericTimeAssociatedElement) tsbCompTT.getEntryBefore(convertStartTimeInTSBms + 1);
                    PidMapTable preceedingTSBPidMap = (PidMapTable) preceedingElem.value;

                    OcapLocator[] locators = m_info.getServiceLocators();
                    SIManager siManager = SIManager.createInstance();
                    for (int i = 0; i < locators.length; i++)
                    {
                        Service s = siManager.getService(locators[i]);
                        if (s instanceof SPIService)
                        {
                            ProviderInstance spi = (ProviderInstance) ((SPIService) s).getProviderInstance();
                            SelectionSessionWrapper session = (SelectionSessionWrapper) spi.getSelectionSession((SPIService)s);
                            locators[i] = LocatorUtil.convertJavaTVLocatorToOcapLocator(session.getMappedLocator());
                        }
                    }

                    // Create a new PidMapTable from the TSB PidMapTable
                    PidMapTable initialRecPidMap = createRecPidMapFromTSBPidMap( preceedingTSBPidMap,
                                                                                 locators );

                    // And add the PidMap to the conversion timetable and the metadata timetable
                    convertTT.addElement(new GenericTimeAssociatedElement(0, initialRecPidMap));

                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "startRecording: Composing initial PidMapTable");
                        log.debug(m_logPrefix + "startRecording: preceedingTSBPidMap (time "
                                  + preceedingElem.getTime() + "): " + preceedingTSBPidMap);
                        log.debug(m_logPrefix + "startRecording: initialRecPidMap (time 0):" + initialRecPidMap);
                    }

                    // Now we need to add an entry to the TT for each PidMap
                    // between (recStartTimeInTSB, recEndTimeInTSB). Note:
                    // TimeTables are sorted by time - low to high
                    Enumeration tsbPidMapEnum = tsbCompTT.elements();
                    while (tsbPidMapEnum.hasMoreElements())
                    {
                        GenericTimeAssociatedElement curRow = (GenericTimeAssociatedElement)
                                                              tsbPidMapEnum.nextElement();
                        if (curRow.getTime() <= convertStartTimeInTSBms)
                        { // Before our range - skip
                            continue;
                        }
                        if (curRow.getTime() >= convertEndTimeInTSBms)
                        { // After our range - we're out
                            break;
                        }
                        // Assert: curRow is in our range

                        // Create a new PidMapTable from the TSB PidMapTable
                        PidMapTable curTSBPidMap = (PidMapTable) curRow.value;
                        PidMapTable newRecPidMap = createRecPidMapFromTSBPidMap(curTSBPidMap,
                                m_info.getServiceLocators());
                        long timeInRecording = curRow.getTime() - convertStartTimeInTSBms;

                        // And add it to the conversion TimeTable and the
                        // segment metadata timetable
                        convertTT.addElement(new GenericTimeAssociatedElement(timeInRecording, newRecPidMap));

                        if (log.isDebugEnabled())
                        {
                            log.debug("startRecording: Composing PidMapTable for conversion");
                            log.debug( m_logPrefix + "startRecording: curTSBPidMap (time "
                                       + curRow.getTime() + "): " + curTSBPidMap );
                            log.debug( m_logPrefix + "startRecording: newRecPidMap (time "
                                       + timeInRecording + "): " + newRecPidMap );
                        }
                    } // END while (tsbPidMaps.hasMoreElements())
                } // END convertTT initialization

                // Attempt to clear space for recording
                clearDiskSpace();

                // Note: If there's insufficient space, we'll handle the native
                //       space full indication and suspend recording

                if (msv != null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug( m_logPrefix + "startRecording: Free space on "
                                   + msv + ": " + msv.getFreeSpace() + " bytes" );
                    }
                }

                m_nativeTSBBufferHandle = currentTSB.getNativeTSBHandle();
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "startRecording: m_nativeTSBBufferHandle: 0x"
                            + Integer.toHexString(m_nativeTSBBufferHandle));
                }

                m_nativeConversionListener = new ConversionListener();

                if (log.isInfoEnabled())
                {
                    log.info( m_logPrefix + "startRecording: Calling nConvertTimeShiftToRecording(tsbhandle 0x"
                              + Integer.toHexString(m_nativeTSBBufferHandle)
                              + ", convListener " + m_nativeConversionListener
                              + ", start " + convertStartTimeMs
                              + ", dur " + remainingDurMs
                              + ", bitrate " + m_info.getBitRate()
                              + ", convertTT " + convertTT
                              + ", msv " + msv + ')');
                }

                //
                // "Convert" content from the TSB into a permanent recording. This may
                // partially or fully retroactive, and if the end time is in the future,
                // will represent an on-going session.
                //
                // TODO: (future) change convert API to use TSB media time
                // instead of system time
                convertRetCode = nConvertTimeShiftToRecording( m_nativeTSBBufferHandle,
                                                               m_nativeConversionListener,
                                                               convertStartTimeMs,
                                                               remainingDurMs,
                                                               m_info.getBitRate(),
                                                               convertTT,
                                                               msv.getNativeHandle() );

                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "startRecording: nConvertTimeShiftToRecording return code "
                            + convertRetCode + " and handle 0x"
                            + Integer.toHexString(m_nativeRecordingHandle) );
                }

                if (convertRetCode != MPE_DVR_ERR_NOERR)
                { // We'll try to keep trucking - but can't created a RecordedService for
                  //  this segment
                    if (log.isWarnEnabled())
                    {
                        log.warn( m_logPrefix + "startRecording: nConvertTimeShiftToRecording returned error "
                                  + convertRetCode + " converting " + remainingDurMs + "ms of content starting from "
                                  + convertStartTimeInTSBms + "ms within " + currentTSB );
                    }
                    m_tswClient.detachFromTSB(currentTSB);
                    isOngoingConversion = false;
                    m_nativeConversionListener.deactivate();
                    m_nativeConversionListener = null;
                    continue;
                }

                // Assert: Conversion initiation was successful

                isOngoingConversion = System.currentTimeMillis() <= currentTSB.getContentEndTimeInSystemTime();

                if (!isOngoingConversion)
                {
                    m_tswClient.detachFromTSB(currentTSB);
                    m_nativeConversionListener.deactivate();
                    m_nativeConversionListener = null;
                    // Otherwise, we'll detach from the TSB when we stop recording
                }

                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "startRecording: Creating a recordedService: name "
                            + m_nativeRecordingName
                            + ", handle 0x" + Integer.toHexString(m_nativeRecordingHandle)
                            + ", actualStartTime " + m_nativeRecordingActualStartTime);
                }

                // Note: Native is required to fill in the recPID/recElementaryStreamType for
                //  each PidMapTable in the convertTT

                // Convert convertTT(segmentPidTimeTable) elements into the
                // Segment's RecordedSegmentInfo.RecordedServiceComponentInfo using
                // the associated recPID/recElementaryStreamType/serviceComponentReference
                this.m_activeRecordedService = addRecordedService( convertTT,
                                                                   isCopyOnce || isCopyProtected,
                                                                   cciTT );

                this.m_curPidMap = (PidMapTable) ((GenericTimeAssociatedElement) convertTT.getLastEntry()).value;

                // move lightweight trigger events located in previous
                // segments to the correct segment. These events are called "future" events
                // since they occur in the future (i.e., in a segment that dosen't exist at
                // the time they were added).
                moveFutureEvents();

                // Test only!
                printSegmentInfo();
            } // END while (tsbe.hasMoreElements()) (loop through all TimeShiftBuffers)

            // We've started (and possibly ended) conversion on 1 or more TSBs,
            //  but only the last one may still be on-going.

            if (isOngoingConversion)
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "startRecording: On-going conversion started of "
                            + currentTSB );
                }

                addTableChangeListeners();

                // Transition to the started state
                m_istate.setState(new IStateStarted());

                // Add self as "in progress" to retention manager
                // this will transition state to 'in_progress'
                RecordingRetentionManager.getInstance().addInProgressRecording(RecordingImpl.this);

                saveRecordingInfo(RecordingDBManager.ALL);
            }
            else if (isCopyProtected)
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "startRecording: Content marked copy protected in "
                            + currentTSB );
                }

                if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                { // See rules of DVR 6.2.1.1.3.3
                    setFailedExceptionReason(RecordingFailedException.CA_REFUSAL);
                }
                setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                m_istate.setState(new IStateSuspendedCopyProtected());
            }
            else
            { // Last conversion was not on-going
                // RecordingRequest is done
                final IStateEnded endState = new IStateEnded(false, false);
                RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);

                setStateAndNotify(endState.getCompletionStateCode());
                m_istate.setState(endState);
            }
        }
        catch (Exception e)
        {
            if (isTSBAttached)
            {
                // make sure we detach from tsb before returning
                m_tswClient.detachFromTSB(currentTSB);
            }

            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix + "startRecording: Recording Start Failed for " + this, e);
            }

            m_nativeRecordingHandle = 0;

            if (convertRetCode != MPE_DVR_ERR_NOERR)
                setFailedExceptionReason(nativeErrorToFailureReason(convertRetCode));

            // Not sure how to come out of a native error or when to retry
            //  So put the RecordingRequest in a terminal state
            m_istate.setState(new IStateEnded(false, false));
            RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
        }
    } // END startRecording()

    /**
     * Internal method used to retrieve components based on the input locators
     * passed in. This should handle service locator, component locators etc.
     *
     * @return 0 on success
     */
    boolean retrieveBroadcastServiceComponents(Vector comps)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        // Assert: comps is an empty vector

        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "retrieveBroadcastServiceComponents... ");
        }
        try
        {
            SIManagerExt siMgr = (SIManagerExt) SIManager.createInstance();
            int i = 0, j = 0, k = 0;

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "m_info.getServiceLocator().length: " + m_info.getServiceLocators().length);
            }

            for (i = 0; i < m_info.getServiceLocators().length; i++)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "m_info.getServiceLocator()[" + i + "]: " + m_info.getServiceLocators()[i]);
                }
            }

            for (i = 0; i < m_info.getServiceLocators().length; i++)
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_logPrefix + "Calling getSIElement..");
                }

                SIElement[] sie = siMgr.getSIElement(m_info.getServiceLocators()[i]);

                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "sie.toString(): " + sie.toString());
                }

                for (k = 0; k < sie.length; k++)
                {
                    // if (LOGGING)
                    // log.debug(m_logPrefix + "siElement[" + k + "]: " +
                    // sie[k]);

                    if (sie[k] instanceof ServiceDetailsImpl)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "siElement instanceof ServiceDetailsImpl...");
                        }

                        ServiceDetailsImpl sdi = (ServiceDetailsImpl) sie[k];
                        ServiceComponentExt[] compArray = (ServiceComponentExt[]) sdi.getComponents();

                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "component array length: " + compArray.length);
                        }

                        for (j = 0; j < compArray.length; j++)
                        {
                            ServiceComponentExt sce = compArray[j];
                            comps.add(sce);
                            if (log.isDebugEnabled())
                            {
                                log.debug(m_logPrefix + "compArray[" + j + "]." + "pid:0x"
                                        + Integer.toHexString(sce.getPID()));
                            }
                            if (log.isDebugEnabled())
                            {
                                log.debug(m_logPrefix + "compArray[" + j + "]." + "streamType:0x"
                                        + Integer.toHexString(sce.getElementaryStreamType()));
                            }
                            if (log.isDebugEnabled())
                            {
                                log.debug(m_logPrefix + "compArray[" + j + "]." + "language:"
                                        + sce.getAssociatedLanguage());
                            }
                            if (log.isDebugEnabled())
                            {
                                log.debug(m_logPrefix + "compArray[" + j + "]." + "ID:" + sce.getID());
                            }
                        }
                    }
                    else if (sie[k] instanceof ServiceComponentExt)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "siElement instanceof ServiceComponentExt...");
                        }

                        ServiceComponentExt sce = (ServiceComponentExt) sie[k];
                        comps.add(sce);
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "sie[" + i + "]." + "pid:0x" + Integer.toHexString(sce.getPID()));
                        }
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "sie[" + i + "]." + "streamType:0x"
                                    + Integer.toHexString(sce.getElementaryStreamType()));
                        }
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "sie[" + i + "]." + "language:" + sce.getAssociatedLanguage());
                        }
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "sie[" + i + "]." + "ID:" + sce.getID());
                        }
                    }
                }
            }
            if (log.isInfoEnabled())
            {
                log.info(m_logPrefix + "retrieveBroadcastServiceComponents... returning true");
            }
            return true;
        }
        catch (InvalidLocatorException e)
        {
            if (log.isErrorEnabled())
            {
                log.error(m_logPrefix + "InvalidLocatorException", e);
            }
        }
        catch (Exception ex)
        {
            if (log.isErrorEnabled())
            {
                log.error(m_logPrefix + "Unable to retrieve component list", ex);
            }
        }

        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "retrieveBroadcastServiceComponents... returning false");
        }
        return false;
    }

    /**
     *
     * Create a conversion PidMapTable given a TSB PidMapTable (which contains
     * an SISnapshot) and the locators - which effectively select which
     * components in the tsbPidMapTable are going to be converted into the
     * recording.
     *
     * @param tsbPidMapTable
     *            A PidMapTable from the TSB
     * @param recLocators
     *            The Locator(s) specifying which components must be converted
     * @return A PidMapTable which contains the components that need to be
     *         converted from the TSB to a recording.
     */
    private PidMapTable createRecPidMapFromTSBPidMap(PidMapTable tsbPidMap, OcapLocator[] recLocators)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (false)
        {
            log.debug(m_logPrefix + "createRecPidMapFromTSBPidMap: Constructing recording PidMap from TSB PidMap...");
            log.debug(m_logPrefix + "createRecPidMapFromTSBPidMap: TSB PIDMap: " + tsbPidMap);
            StringBuffer locatorString = new StringBuffer("createRecPidMapFromTSBPidMap: recording locators: ");
            for (int i = 0; i < recLocators.length; i++)
            {
                locatorString.append('[').append(i).append("]:").append(recLocators[i].toExternalForm());
            }
            log.debug(m_logPrefix + "createRecPidMapFromTSBPidMap: " + locatorString);
        }

        //
        // Use the SISnapshot to resolve the recording locators to the TSB
        // components
        //

        // We'll make the recPidMap large enough to accommodate all service
        // components, in case the platform wants to record everything in the
        // TSB
        final int tsbPidMapSize = tsbPidMap.getSize();
        PidMapTable recPidMap = new PidMapTable(tsbPidMapSize);

        SISnapshotManager snapshot = tsbPidMap.getSISnapshot();

        // Assert: Every PidMapTable will have a snapshot reference

        SIElement[] elementsForLocators;
        try
        {
            elementsForLocators = snapshot.getSIElements(recLocators);
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix
                        + "createRecPidMapFromTSBPidMap: Could not get SI elements for recording locators: ", e);
            }
            elementsForLocators = new SIElement[0];
        }

        //
        // Check for Service reference or explicit ServiceComponent references
        //
        if ((elementsForLocators.length == 0) || (elementsForLocators[0] instanceof ServiceDetailsExt))
        {
            // If the first SIElement is a ServiceDetailsExt or there are no
            // components
            // specified (e.g. for an analog Service), put all the components in
            // the PID map

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "createRecPidMapFromTSBPidMap: Recording complete Service...");
            }

            recPidMap.setServiceDetails(tsbPidMap.getServiceDetails());
            PidMapEntry newRecEntry;

            // Dest fields from the TSB are source fields for the recording
            // Assert: recPidMap.getSize() == tsbPidMap..getSize()
            for (int i = 0; i < tsbPidMapSize; i++)
            {
                PidMapEntry tsbEntry = tsbPidMap.getEntryAtIndex(i);
                newRecEntry = new PidMapEntry(tsbEntry.getStreamType(), tsbEntry.getRecordedElementaryStreamType(),
                        tsbEntry.getRecordedPID(), PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, PidMapEntry.PID_UNKNOWN,
                        tsbEntry.getServiceComponentReference());
                recPidMap.addEntryAtIndex(i, newRecEntry);
            } // END for (tsbPidMap entries)
        } // END if (recComponents.length == 0)
        else if (elementsForLocators[0] instanceof ServiceComponentExt)
        {
            // Explicit components specified - match with the TSB PidMap entries
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "createRecPidMapFromTSBPidMap: Matching requested Service components to TSB...");
            }

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "createRecPidMapFromTSBPidMap: Found " + elementsForLocators.length
                        + " TSB components in snapshot");
            }
            for (int i = 0; i < elementsForLocators.length; i++)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "createRecPidMapFromTSBPidMap: Snapshot SI element #" + i + ": "
                            + elementsForLocators[i]);
                }
            }

            //
            // Walk the list of returned components and create an entry in the
            // recPidMap for each app-designated component
            //
            PidMapEntry newRecEntry;
            int nextRecEntry = 0;

            for (int i = 0; i < tsbPidMapSize; i++)
            {
                PidMapEntry tsbEntry = tsbPidMap.getEntryAtIndex(i);
                if (false)
                    log.debug(m_logPrefix + "createRecPidMapFromTSBPidMap: Looking at TSB entry #" + i + ": "
                            + tsbEntry);

                boolean copyEntry = false;

                if ((tsbEntry.getStreamType() == MediaStreamType.PCR)
                        || (tsbEntry.getStreamType() == MediaStreamType.PMT))
                { // Always copy the PCR and PMT entry
                    copyEntry = true;
                }
                else
                { // Check for component match
                    for (int j = 0; j < elementsForLocators.length && !copyEntry; j++)
                    {
                        // Copy only service components that were resolved from
                        // the Locators
                        // and the PCR
                        if ((elementsForLocators[j].equals(tsbEntry.getServiceComponentReference())))
                        {
                            copyEntry = true;
                        }
                    } // END for (recComponents)
                }

                if (copyEntry)
                {
                    newRecEntry = new PidMapEntry(tsbEntry.getStreamType(), tsbEntry.getRecordedElementaryStreamType(),
                            tsbEntry.getRecordedPID(), PidMapEntry.ELEM_STREAMTYPE_UNKNOWN, PidMapEntry.PID_UNKNOWN,
                            tsbEntry.getServiceComponentReference());
                    recPidMap.addEntryAtIndex(nextRecEntry, newRecEntry);
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "createRecPidMapFromTSBPidMap: Added entry #" + nextRecEntry + ": "
                                + newRecEntry);
                    }
                    nextRecEntry++;
                }
            } // END for (tsbPidMap entries)

            // Blank out any we didn't copy (leave room for platform to add
            // comps)

            // Assert: recPidMap.getSize() == tsbPidMap..getSize()
            while (nextRecEntry < tsbPidMapSize)
            {
                // This will add PidMapEntries with undefined values
                recPidMap.addEntryAtIndex(nextRecEntry++, new PidMapEntry());
            }
        } // END else/if (recComponents.length == 0)

        // Assert: recPidMap has tsbPidMap.getSize() entries - some of which may
        // be blank
        return recPidMap;
    } // END createRecPidMapFromTSBPidMap()

    /**
     * Check two recording PID maps for source equality (source pids/types)
     *
     * @param pidMap1
     * @param pidMap2
     * @return true if all PIDs in pidMap1 are in pidMap2, are of the same type
     *         and vice-vera false otherwise
     */
    private boolean recordingPidMapsEquivalent(PidMapTable pidMap1, PidMapTable pidMap2)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        for (int i = 0; i < pidMap1.getSize(); i++)
        {
            PidMapEntry pme1 = pidMap1.getEntryAtIndex(i);

            boolean found = false;
            for (int j = 0; !found && j < pidMap2.getSize(); j++)
            {
                PidMapEntry pme2 = pidMap2.getEntryAtIndex(j);
                if ((pme1.getSourceElementaryStreamType() == pme2.getSourceElementaryStreamType())
                        && (pme1.getSourcePID() == pme2.getSourcePID()))
                {
                    found = true;
                }
            } // END for (pidMap2)

            if (!found)
            {
                return false;
            }
        } // END for (pidMap1)
        return true;
    } // END recordingPidMapsEquivalent()

    /*
     * Internal method to save the segment info and service components data to
     * persistent storage
     *
     * This must be called after nConvertTimeShiftToRecording so the
     * PidMapTables reflect the recorded components.
     */
    private RecordedServiceImpl addRecordedService( final TimeTable segmentPidMapTimeTable,
                                                    final boolean copyProtected,
                                                    final TimeTable cciTimeTable )
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        // Convert the PID Map Table into the recorded components
        TimeTable detailsTimeTable = createDetailsFromPidMapTable(segmentPidMapTimeTable);
        final OcapLocator serviceLocators [] = m_info.getServiceLocators();
        final String svcName = serviceLocators != null ? serviceLocators[0].toExternalForm() 
                                                       : Integer.toHexString(this.hashCode());

        RecordedSegmentInfo segmentInfo = new RecordedSegmentInfo( svcName,
                                                                   m_nativeRecordingName,
                                                                   m_nativeRecordingActualStartTime,
                                                                   0,
                                                                   detailsTimeTable,
                                                                   new LightweightTriggerEventTimeTable(),
                                                                   copyProtected,
                                                                   cciTimeTable );

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "createRecordedService: " + segmentInfo);
        }

        int segmentIndex = m_info.addRecordedSegmentInfo(segmentInfo);

        RecordedServiceImpl newRecordedService = new RecordedServiceImpl(segmentInfo, this, segmentIndex, m_sync);

        this.m_segmentedRecordedService.addRecordedService(newRecordedService);

        return newRecordedService;
    } // END addRecordedService()

    /**
     * @param segmentPidMapTimeTable
     *            TimeTable containing PidMapTable objects
     *
     * @return TimeTable containing TimeAssociatedDetailsInfo objects
     */
    private TimeTable createDetailsFromPidMapTable(TimeTable segmentPidMapTimeTable)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        TimeTable detailsTimeTable = new TimeTable(segmentPidMapTimeTable.getSize());

        // Loop through all the PidMapTables and create a corresponding details
        // timetable
        Enumeration timeTableEnum = segmentPidMapTimeTable.elements();

        while (timeTableEnum.hasMoreElements())
        {
            GenericTimeAssociatedElement elem = (GenericTimeAssociatedElement) timeTableEnum.nextElement();

            PidMapTable pidMapTable = (PidMapTable) elem.value;

            TimeAssociatedDetailsInfo details = createDetailsFromPidMapTable(pidMapTable, elem.getTime());

            detailsTimeTable.addElement(details);
        } // while timeTableValues.hasMoreElements()

        return detailsTimeTable;
    } // END createDetailsFromPidMapTable()

    /**
     * Create a TimeAssociatedDetailsInfo from the given PidMapTable.
     * RecordedServiceComponentInfo objects are added for each valid non-PCR
     * entry in pidMapTable. The TimeAssociatedDetailsInfo PCR value is set from
     * the MediaStreamType.PCR entry in the PidMapTable.
     *
     * Note: This method does not set the TimeAssociatedElement.time in the
     * TimeAssociatedDetailsInfo. The caller must set this value on the returned
     * instance.
     *
     * @param pidMapTable
     *            The PidMapTable used to produce the TimeAssociatedDetailsInfo
     * @param time
     * @return The TimeAssociatedDetailsInfo derived from pidMapTable
     */
    private TimeAssociatedDetailsInfo createDetailsFromPidMapTable(PidMapTable pidMapTable, long time)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        final int pidMapSize = pidMapTable.getSize();
        Vector recordedComponents = new Vector(pidMapSize);
        int pcrPid = -1;

        for (int i = 0; i < pidMapSize; i++)
        {
            PidMapEntry pme;
            if ((pme = pidMapTable.getEntryAtIndex(i)) != null)
            {
                if (pme.getStreamType() == MediaStreamType.PCR)
                {
                    pcrPid = pme.getRecordedPID();
                    continue;
                }

                // Make sure we have valid elementary stream type and Pid
                if ( ( (pme.getRecordedPID() != PidMapEntry.PID_UNKNOWN)
                       && (pme.getRecordedElementaryStreamType() != PidMapEntry.ELEM_STREAMTYPE_UNKNOWN) )
                     || ( (pme.getStreamType() == MediaStreamType.PMT)
                          && (pme.getRecordedPID() != PidMapEntry.PID_UNKNOWN) ) )
                {
                    RecordedServiceComponentInfo rscInfo = new RecordedServiceComponentInfo(pme);

                    // Add to the vector
                    recordedComponents.add(rscInfo);
                }
            } // if pidMapTable.getEntryAtIndex(i) != null
        } // for (PidMapEntry loop)

        return new TimeAssociatedDetailsInfo(time, recordedComponents, pcrPid);
    } // END createDetailsFromPidMapTable()

    /*
     * Internal method to print the segment info and service components
     */
    void printSegmentInfo()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isDebugEnabled())
        {
            // Retrieve recorded service components
            Enumeration seg_enum = m_info.getRecordedSegmentInfoElements();
            int segment = 0;
            while (seg_enum.hasMoreElements())
            {
                RecordedSegmentInfo seg_info = (RecordedSegmentInfo) seg_enum.nextElement();

                int detail_count = 0;

                TimeTable detailTT = seg_info.getTimeAssociatedDetails();

                Enumeration detail_enum = detailTT.elements();
                while (detail_enum.hasMoreElements())
                {
                    TimeAssociatedDetailsInfo details = (TimeAssociatedDetailsInfo) detail_enum.nextElement();
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "printSegmentInfo: Segment " + segment + ": details " + detail_count
                                + " @ " + details.getTimeMillis() + "ms: PCR PID " + details.getPcrPid());
                    }

                    int comp_count = 0;

                    Enumeration comp_enum = details.getComponents().elements();

                    while (comp_enum.hasMoreElements())
                    {
                        RecordedServiceComponentInfo rsc = (RecordedServiceComponentInfo) comp_enum.nextElement();

                        if (log.isDebugEnabled())
                        {
                            log.debug(m_logPrefix + "printSegmentInfo:    comp " + comp_count + ": " + rsc.toString());
                        }
                        comp_count++;
                    } // END while (RecordedServiceComponent enum)
                    detail_count++;
                } // END while (TimeAssociatedDetailsInfo enum)
                if (log.isDebugEnabled())
                {
                    log.debug( m_logPrefix + "printSegmentInfo:    CCI " + seg_info.getCCITimeTable() );
                }
                segment++;
            } // END while (RecordedSegmentInfo enum)
        } // END if (LOGGING)
    } // END printSegmentInfo()

    /**
     * This routine is to be called to setup the resources and initiate
     * recording At this point, there may or may not be a shared resource
     * available to share or tuner reserved.
     *
     */
    void initiateRecordingProcess(boolean stateNotify)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + " Initiating recording process");
        }

        if (!m_recordingManager.isRecordingEnabled())
        {
            if (log.isDebugEnabled())
            {
                log.debug( m_logPrefix
                           + "initiateRecordingProcess: Recording is disabled - suspending recording." );
            }

            // Put us in a state where we'll kick off recording after recording
            //  has been enabled/re-enabled...
            m_istate.setState(new IStateSuspendedRecordingDisabled());
            return;
        }

        // Get the volume string name
        // if the recording is associated with a detached volume and the
        // volume has just been attached
        // the volume selected shall be at this time set in the
        // RecordingInfoNode
        String msvName = m_info.getMSVReference().getVolumeName();

        // if msv string name is null, set the msv to record to default
        // volume
        if (msvName == null)
        {
            // get the default storage volume
            MediaStorageVolume msv = m_recordingManager.getDefaultMediaStorageVolume();
            // set it as the volume to record to
            m_info.setDestination(msv);
        }

        // Check for a ServiceContext-based RecordingRequest
        if (m_recordingSpec instanceof ServiceContextRecordingSpec)
        {
            // This is a service context based recording, so no tuning is
            // necessary We must confirm that this service context is
            // valid and presenting
            if ( (m_scheduledServiceContext == null)
                 || (m_scheduledServiceContext.getService() == null) )
            {
                // The SC does not exist or is not presenting, so fail this recording
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + " initiateRecordingProcess: null ServiceContext or null getService()");
                }

                final IStateEnded endState = new IStateEnded(false, false);
                setStateAndNotify(endState.getCompletionStateCode());
                m_istate.setState(endState);

                if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                {
                    setFailedExceptionReason(RecordingFailedException.CONTENT_NOT_FOUND);
                }

                RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug( m_logPrefix + "initiateRecordingProcess: Starting ServiceContextRecordingSpec recording for SC "
                           + m_scheduledServiceContext + " (service" + m_scheduledServiceContext.getService() +')' );
            }

            // Service is presenting. Perform the native recording start
            // Native call should set the recording handle and recording
            // name

            // At this point the service context should have an associated
            // TimeShiftWindow
            // which may or may not buffering depending on whether
            // 'timeshift' is enabled.
            // Get that timeShiftWindow
            // The recording should initiate buffering if not already
            // buffering (attachForBuffering)
            // and then initiate recording (attachForRecording)...

            // This has to be done synchronously 'cause any
            // state change in service context (tune away, destroy)
            // will cause the recording to terminate.
            ExtendedNetworkInterface ni = null;
            Service service = null;

            try
            {
                ni = (ExtendedNetworkInterface) ((TimeShiftProperties) m_scheduledServiceContext).getNetworkInterface(false);

                service = m_scheduledServiceContext.getService();
            }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error(m_logPrefix + "initiateRecordingProcess: failed to get NI from SC", e);
                }
            }

            // Assert 'ni' is valid
            // Assert 'service' is a broadcast service

            if ((ni != null) && ((service instanceof org.cablelabs.impl.service.javatv.service.ServiceImpl)
                                 || (service instanceof SPIService)))
            {
                OcapLocator[] serviceLocator = new OcapLocator[1];
                serviceLocator[0] = (OcapLocator) service.getLocator();

                // Set the service locator in meta-data
                m_info.setServiceLocators(serviceLocator);

                try
                {
                    int uses = TimeShiftManager.TSWUSE_BUFFERING | TimeShiftManager.TSWUSE_RECORDING
                            | TimeShiftManager.TSWUSE_NIRES;

                    // Find a suitable timeshiftwindow
                    // Assert:The timeshiftwindow should be attached to the
                    // service context( ECR requirement!)
                    // This call will either return a valid timeshiftwindow
                    // client or throw noFreeInterface exception

                    m_tswClient = m_tsMgr.getTSWByInterface( service, ni, m_info.getRequestedStartTime(),
                            m_info.getRequestedDuration(), uses, m_resourceUsage, RecordingImpl.this,
                            TimeShiftManager.LISTENER_PRIORITY_HIGH );

                    if (m_tswClient != null)
                    {
                        int state = m_tswClient.getState();
                        if (log.isInfoEnabled())
                        {
                            log.info(m_logPrefix + "TimeShiftWindow state: " + TimeShiftManager.stateString[state]
                                    + "\n");
                        }

                        // If the state is 'buffering' start recording immediately
                        // Otherwise wait for asynchronous notifications
                        // from timeshiftwindow client

                        switch (state)
                        {
                            case  TimeShiftManager.TSWSTATE_BUFFERING:
                            {
                                attachToTSW();
                                startRecording(m_info.getRequestedStartTime(), stateNotify);
                                return;
                            }
                            case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                            case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
                            {
                                attachToTSW();
                                // FALL THROUGH
                            }
                            default:
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info(m_logPrefix + "Waiting for TSW to become READY or BUFFERING...");
                                }
                                m_istate.setState(new IStateWaitTuneSuccess());
                                return;
                            }
                        } // END switch (state)
                    }
                }
                catch (NoFreeInterfaceException e)
                {
                    // TODO: handle the exception
                    // If this exception is thrown here it could mean that
                    // the
                    // service context has lost the NI resource during
                    // contention
                    // Fail the recording?

                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "NoFreeInterfaceException", e);
                    }
                }
            } // END if ( (ni != null) && (service instanceof ServiceImpl)
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn(m_logPrefix + "No networkinterface provided by the ServiceContext, or not a supported Service implementation - ni: " + ni +
                            ", service: " + service + " - setting state to FAILED, reason insufficient resources");
                }
            }
            // If a serviceContext based recording has trouble starting, go
            // to the FAILED_STATE.
            m_istate.setState(new IStateEnded(false, false));
            setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
            RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
            setStateAndNotify(LeafRecordingRequest.FAILED_STATE);
            return;
        } // End of service context based recording handling

        // If there is no service context associated, treat this as normal
        // scheduled recording
        // Query the TimeShiftWindowManager to find a TimeShiftWindow by
        // start time
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "initiateRecordingProcess: Normal Service-/Locator-based recording");
        }

        ServiceExt service = null;

        try
        {
            SIManager siMgr = SIManager.createInstance();

            if (m_recordingSpec instanceof ServiceRecordingSpec)
            {
                Service specService = ((ServiceRecordingSpec)m_recordingSpec).getSource();
                if (specService instanceof ServiceExt)
                {
                    service = (ServiceExt)specService;
                }
                else
                {
                    service = (ServiceExt)siMgr.getService(m_info.getServiceLocators()[0]);
                }

                if (log.isDebugEnabled())
                {
                    log.debug( m_logPrefix + "initiateRecordingProcess: Starting ServiceRecordingSpec recording for service "
                               + service );
                }
            }
            else if (m_recordingSpec instanceof LocatorRecordingSpec)
            {
                try
                {
                    service = (ServiceExt)siMgr.getService(m_info.getServiceLocators()[0]);
                }
                catch(InvalidLocatorException e)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "initiateRecordingProcess: caught " + e);
                    }
                    m_istate.setState(new IStateEnded(false, false));
                    setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                    RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
                    setStateAndNotify(LeafRecordingRequest.FAILED_STATE);
                    return;
                }
                // Verify ServiceComponents are all part of the same Service
                for (int i = 1; i < m_info.getServiceLocators().length; i++)
                {
                    if (!service.equals(siMgr.getService(m_info.getServiceLocators()[i])))
                    {
                        // If we have an array of Locators (component Locator array), and one
                        //  is not of the same Service, we don't really have a sane Service ref
                        service = null;
                        break;
                    }
                }

                if (log.isDebugEnabled())
                {
                    log.debug( m_logPrefix + "initiateRecordingProcess: Starting LocatorRecordingSpec recording for service "
                               + service );
                }
            }

            if (service == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix
                            + "initiateRecordingProcess: service is null (could not resolve Service from Locator/ServiceRecordingSpec)");
                }

                m_istate.setState(new IStateSuspendedServiceUnavailable());
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "initiateRecordingProcess: Attempting to acquire TSW for service "
                        + service + " requestedStartTime: " + m_info.getRequestedStartTime()
                        + " requestedDuration: " + m_info.getRequestedDuration() + " m_resourceUsage: "
                        + m_resourceUsage);
            }

            m_tswClient = m_tsMgr.getTSWByTimeSpan( service,
                                                    m_info.getRequestedStartTime(),
                                                    m_info.getRequestedDuration(),
                                                    TimeShiftManager.TSWUSE_BUFFERING | TimeShiftManager.TSWUSE_RECORDING,
                                                    m_resourceUsage,
                                                    RecordingImpl.this,
                                                    TimeShiftManager.LISTENER_PRIORITY_HIGH );

            int tswstate = m_tswClient.getState();
            // Switch to appropriate state if we received an already-tuned
            // TSW

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "initiateRecordingProcess: Acquired a TSW in state "
                        + TimeShiftManager.stateString[tswstate]);
            }

            switch (tswstate)
            {
                case TimeShiftManager.TSWSTATE_RESERVE_PENDING:
                case TimeShiftManager.TSWSTATE_TUNE_PENDING:
                {
                    // Set state prior to attempting acquisition
                    m_istate.setState(new IStateWaitTuneSuccess());
                    break;
                }
                case TimeShiftManager.TSWSTATE_IDLE:
                case TimeShiftManager.TSWSTATE_INTSHUTDOWN:
                {
                    m_istate.setState(new IStateWaitTuneSuccess());
                    break;
                }
                case TimeShiftManager.TSWSTATE_BUFFERING:
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix
                                + "initiateRecordingProcess: inducing state initialization via TSWSTATE_BUFFERING");
                    }

                    m_istate.setState(new IStateWaitTuneSuccess());
                    m_istate.processTswEventReadyToBuffer(new TimeShiftWindowStateChangedEvent(
                            TimeShiftManager.TSWSTATE_TUNE_PENDING, TimeShiftManager.TSWSTATE_READY_TO_BUFFER,
                            TimeShiftManager.TSWREASON_NOREASON));
                    m_istate.processTswEventBuffering(new TimeShiftWindowStateChangedEvent(
                            TimeShiftManager.TSWSTATE_READY_TO_BUFFER, TimeShiftManager.TSWSTATE_BUFFERING,
                            TimeShiftManager.TSWREASON_NOREASON));
                    break;
                }
                case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
                case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix
                                + "initiateRecordingProcess: inducing state initialization via TSWSTATE_READY_TO_BUFFER");
                    }

                    m_istate.setState(new IStateWaitTuneSuccess());
                    m_istate.processTswEventReadyToBuffer(new TimeShiftWindowStateChangedEvent(
                            TimeShiftManager.TSWSTATE_TUNE_PENDING, TimeShiftManager.TSWSTATE_READY_TO_BUFFER,
                            TimeShiftManager.TSWREASON_NOREASON));
                    break;
                }
                case TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER:
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix
                                + "initiateRecordingProcess: inducing state initialization via TSWSTATE_NOT_READY_TO_BUFFER");
                    }

                    m_istate.setState(new IStateWaitTuneSuccess());
                    m_istate.processTswEventNotReadyToBuffer(new TimeShiftWindowStateChangedEvent(
                            TimeShiftManager.TSWSTATE_TUNE_PENDING, TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER,
                            TimeShiftManager.TSWREASON_NOREASON));
                    break;
                }
                case TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN:
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix
                                + "initiateRecordingProcess: received TSW in shutdown cycle. Changing states.");
                    }
                    // Put us in a state where we'll kick off buffering
                    // after buffering shutdown cycle has completed
                    m_istate.setState(new IStateSuspendedTSWBufShutdown());

                    break;
                }
                default:
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(m_logPrefix + "initiateRecordingProcess: Unexpected TSW state");
                    }
                    break;
                }
            } // END switch (tswstate)
        }
        catch (Exception e)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "initiateRecordingProcess: caught " + e);
            }

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "initiateRecordingProcess: inducing state initialization via TSWSTATE_IDLE");
            }

            m_istate.processTswEventIdle(new TimeShiftWindowStateChangedEvent(
                    TimeShiftManager.TSWSTATE_TUNE_PENDING, TimeShiftManager.TSWSTATE_IDLE,
                    TimeShiftManager.TSWREASON_NOFREEINT));
        }

        return;
    } // END initiateRecordingProcess()

    /**
     * Set the state of this recording without notifying listeners that a state
     * transition has occurred.
     */
    public void setStateNoNotify(int state)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "setStateNoNotify - newState: " + externalStateToString(state));
        }
        m_info.setState(state);
    }

    /**
     * Set the state of this recording. For all publicly visible state
     * transitions, this method should notify the NavigationManager that a state
     * transition has occurred iff state is different than the current state.
     */
    public void setStateAndNotify(int state)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (m_info.getState() == state)
            return;
        int oldState, newState;
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "setStateAndNotify - oldState: " + externalStateToString(m_info.getState())
                + " New State: " + externalStateToString(state));
        }
        oldState = m_info.getState();
        newState = state;

        m_info.setState(newState);
        this.saveRecordingInfo(RecordingDBManager.STATE);
        this.notifyStateChange(newState, oldState);
    }

    /**
     * Set the in-progress state of this recording. This is only for use by
     * RecordingRetentionManager and is only allowed to switch to/from
     * IN_PROGRESS states
     */
    public void setInProgressSpaceState(int state)
    {
        synchronized (m_sync)
        {
            if ( !( (state == IN_PROGRESS_INSUFFICIENT_SPACE_STATE)
                    || (state == IN_PROGRESS_INCOMPLETE_STATE)
                    || (state == IN_PROGRESS_STATE) ) )
            {
                return;
            }

            // Only moving from I_P to I_P_I_S or vice versa
            setStateAndNotify(state);
        }
    }

    /**
     * Retrieve the <code>NetworkInterface</code>
     *
     * @return ni the NetworkInterface
     */
    public NetworkInterface getNetworkInterface()
    {
        synchronized (m_sync)
        {
            if (m_scheduledServiceContext != null)
            {
                return ((TimeShiftProperties) m_scheduledServiceContext).getNetworkInterface(false);
            }
            else if (m_tswClient != null)
            {
                return m_tswClient.getNetworkInterface();
            }
            return null;
        }
    }

    /**
     * Notify listeners if the current state is different from saved state.
     *
     * @return false if state didn't change or true if state changed (and
     *         clients notified)
     */
    public boolean notifyIfStateChangedFromSaved()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        int oldState, newState;

        newState = m_info.getState();

        if (newState == m_savedState) return false;

        // Save to locals to notify w/o lock
        oldState = m_savedState;

        this.notifyStateChange(newState, oldState);
        return true;
    }

    /**
     * Returns the saved state of the recording request.
     *
     * @return Saved state of the recording request.
     */
    public int getSavedState()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        return m_savedState;
    }

    /**
     * Set the saved state of the recording request.
     */
    void setSavedState(int state)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        m_savedState = state;
    }

    /**
     * Sets the saved state of the recording request to the current state.
     */
    public void saveState()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        m_savedState = m_info.getState();
    }

    /**
     * Restore the current state of the recording request from the saved state.
     */
    public void restoreState()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        m_info.setState(m_savedState);
    }

    /**
     * Checks whether the recording request was a root recording request
     * generated when the application called the RecordingManager.record(..)
     * method. The implementation should create a root recording request
     * corresponding to each successful call to the record method.
     *
     * @return True, if the recording request is a root recording request, false
     *         if the recording request was generated during the process of
     *         resolving another recording request.
     */
    public boolean isRoot()
    {
        synchronized (m_sync)
        {
            if (m_info.getState() == RecordingRequestImpl.DESTROYED_STATE)
                throw new IllegalStateException("m_info.getState() == RecordingRequestImpl.DESTROYED_STATE");
            return m_isRoot;
        }
    }

    /**
     * Gets the root recording request corresponding to this recording request.
     * A root recording request is the recording request that was returned when
     * the application called the RecordingManager.record(..) method.
     * <p>
     * If the current recording request is a root recording request, the current
     * recording request is returned.
     *
     * @return the root recording request for this recording request, null if
     *         the application does not have read accesss permission for the
     *         root recording request.
     * @see getNode
     */
    public RecordingRequest getRoot()
    {
        synchronized (m_sync)
        {
            if (m_info.getState() == RecordingRequestImpl.DESTROYED_STATE)
                throw new IllegalStateException("m_info.getState() == RecordingRequestImpl.DESTROYED_STATE");
            return getNode(m_root);
        }
    }

    /**
     * Gets the parent recording request corresponding to this recording
     * request.
     *
     * @return the parent recording request for this recording request, null if
     *         the application does not have read accesss permission for the
     *         parent recording request or if this recording request is the root
     *         recording request.
     * @see getNode
     */
    public RecordingRequest getParent()
    {
        synchronized (m_sync)
        {
            if (m_info.getState() == RecordingRequestImpl.DESTROYED_STATE)
                throw new IllegalStateException("m_info.getState() == RecordingRequestImpl.DESTROYED_STATE");
            return getNode(m_parent);
        }
    }

    /**
     * Returns the RecordingSpec corresponding to the recording request. This
     * will be either the source as specified in the call to the record(..)
     * method which caused this recording request to be created or the
     * RecordingSpec generated by the system during the resolution of the
     * original application specified RecordingSpec. Any modification to the
     * RecordingSpec due to any later calls to the SetRecordingProperties
     * methods on this instance will be reflected on the returned RecordingSpec.
     * <p>
     * When the implementation generates a recording request while resolving
     * another recording request, a new instance of the RecordingSpec is created
     * with an identical copy of the RecordingProperties of the parent recording
     * request.
     *
     * @return a RecordingSpec containing information about this recording
     *         request.
     */
    public RecordingSpec getRecordingSpec()
    {
        synchronized (m_sync)
        {
            if (m_info.getState() == RecordingRequestImpl.DESTROYED_STATE)
                throw new IllegalStateException("m_info.getState() == RecordingRequestImpl.DESTROYED_STATE");

            RecordingSpec outRecSpec = null;

            // recreate the recording properties from stored metadata

            RecordingProperties recordingProperties = new OcapRecordingProperties(
                                            m_info.getBitRate(),
                                            (m_info.getExpirationDate().getTime() == Long.MAX_VALUE)
                                                ? Long.MAX_VALUE
                                                : ((m_info.getExpirationDate().getTime() - m_info.getRequestedStartTime()) / 1000),
                                            m_info.getRetentionPriority(),
                                            m_info.getPriority(),
                                            m_info.getFap(),
                                            m_info.getOrganization(),
                                            m_info.getDestination() );

            // recreate the recording specification
            switch (m_info.getRequestType())
            {
                case RecordingInfo2.SPECTYPE_SERVICE:
                {
                    // Attempt to re-bind the RecordingRequest with a matching Service object
                    // If type is SPECTYPE_SERVICE, there will be 1 Service locator in the Service Locator array
                    Service specService = null;

                    final OcapLocator specLocator = m_info.getServiceLocators()[0];
                    try
                    {
                        final SIManagerExt siMgr = (SIManagerExt) SIManager.createInstance();
                        final int specServiceNumber = m_info.getServiceNumber();

                        if ( (specServiceNumber == RecordingInfo2.SERVICENUMBER_UNDEFINED)
                             || (specLocator.getSourceID() == -1) )
                        { // No reason to walk the entire Service list if no ServiceNumber was
                          //  saved or if the Locator is not a SourceID Locator
                            specService = siMgr.getService(specLocator);
                        }
                        else
                        {
                            // We have a SourceID locator with a ServiceNumber
                            // Walk the list and find a Service with matching tuple
                            // Get all ServiceDetails with matching sourceId
                            SIElement[] sie = siMgr.getSIElement(specLocator);

                            for (int k = 0; k < sie.length; k++)
                            {
                                if (sie[k] instanceof ServiceDetailsImpl)
                                {
                                    ServiceDetailsImpl sdi = (ServiceDetailsImpl) sie[k];

                                    if (log.isDebugEnabled())
                                    {
                                        log.debug(m_logPrefix + "specServiceNumber: " + specServiceNumber + ", specSourceID():0x" + Integer.toHexString(specLocator.getSourceID())
                                              + ", sdi.getServiceNumber(): " + sdi.getServiceNumber() + ", sdi.getSourceID():0x" + Integer.toHexString(specLocator.getSourceID()));
                                    }

                                    if ( (sdi.getServiceNumber() == specServiceNumber)
                                         && (sdi.getSourceID() == specLocator.getSourceID()) )
                                    {
                                        if (log.isDebugEnabled())
                                        {
                                            log.debug(m_logPrefix + "Matched specServiceNumber and sourceId... ");
                                        }
                                        specService = sdi.getService();
                                        break;
                                    }
                                }
                            }

                            if(specService == null)
                            {
                                // Couldn't find the Service in the list of currently-defined Services
                                throw new InvalidLocatorException(specLocator, "Didn't find a matching Service");
                            }
                            else
                            {
                                if (log.isInfoEnabled())
                                {
                                    log.info(m_logPrefix + "specService: " + specService);
                                }
                            }
                        } // END else/if (SourceID or ServiceNumber undefined)
                    }
                    catch (Exception e)
                    {
                        SystemEventUtil.logRecoverableError("Could not re-bind ServiceRecordingSpec for locator " + specLocator, e);
                        // We'll continue on here. We want to allow the RecordingRequest (and
                        //  associated RecordedService, if any) to exist even if we can't
                        //  re-bind the Service object in the ServiceRecordingSpec (e.g. if the
                        //  Service is not present after reboot due to removal or network issues)
                    }

                    outRecSpec = new ServiceRecordingSpec( specService,
                                                                new Date(m_info.getRequestedStartTime()),
                                                                m_info.getRequestedDuration(),
                                                                recordingProperties );
                    break;
                }
                case RecordingInfo2.SPECTYPE_SERVICECONTEXT:
                {
                    outRecSpec = new ServiceContextRecordingSpec( m_scheduledServiceContext,
                                                                   new Date(m_info.getRequestedStartTime()),
                                                                   m_info.getRequestedDuration(),
                                                                   recordingProperties );
                    break;
                }

                case RecordingInfo2.SPECTYPE_UNKNOWN:
                case RecordingInfo2.SPECTYPE_LOCATOR:
                default:
                {
                    try
                    {
                        outRecSpec = new LocatorRecordingSpec( m_info.getServiceLocators(),
                                                                new Date(m_info.getRequestedStartTime()),
                                                                m_info.getRequestedDuration(),
                                                                recordingProperties );
                    }
                    catch (Exception e)
                    {
                        // We'll continue on here. We want to allow the RecordingRequest (and
                        //  associated RecordedService, if any) to exist even if we can't
                        //  re-resolve the Locator(s) at startup (e.g. if the Service is not
                        //  present after reboot due to removal or network issues)
                        SystemEventUtil.logRecoverableError("Component verification could not be performed", e);

                        try
                        {
                            outRecSpec = new LocatorRecordingSpec( new OcapLocator[0],
                                                                    new Date(m_info.getRequestedStartTime()),
                                                                    m_info.getRequestedDuration(),
                                                                    recordingProperties );
                        }
                        catch (InvalidServiceComponentException isce)
                        {
                            // Current constructor for this class can't throw with 0-length Locator array
                            outRecSpec = null;
                        }
                    }
                    break;
                }
            } // END switch (info.getRequestType())
            return outRecSpec;
        }
    }



    /**
     * Modify the RecordingProperties corresponding to the RecordingSpec for
     * this recording request. Applications may change any properties associated
     * with a recording request by calling this method. Changing the properties
     * may result in changes in the states of this recording request. Changing
     * the properties of a parent recording request will not automatically
     * change the properties of any of its child recording requests that are
     * already created. Any child recording requests created after the
     * invocation of this method will inherit the new values for the properties.
     *
     * @param properties
     *            the new recording properties to set.
     *
     * @throws IllegalStateException
     *             if changing one of the parameters that has been modified in
     *             the new recording properties is not legal for the current
     *             state of the recording request. For example, changing the
     *             duration of a completed recording request. The parameters
     *             that may be modified during each state of the recording
     *             request is defined in the specifcation text.
     *
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)
     */
    public void setRecordingProperties(RecordingProperties properties) throws IllegalStateException,
            AccessDeniedException
    {
        SecurityUtil.checkPermission(new RecordingPermission("modify", "own"));
        synchronized (m_sync)
        {
            checkWriteExtFileAccPerm();

            int state = m_info.getState();
            if (state == RecordingRequestImpl.DESTROYED_STATE)
                throw new IllegalStateException("state == RecordingRequestImpl.DESTROYED_STATE");

            // TODO: setRecordingProperties - JavaDoc does not match "throws"
            // list

            if (properties instanceof OcapRecordingProperties)
            {
                OcapRecordingProperties ocp = (OcapRecordingProperties) properties;
                CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                AppID callingApp = (AppID) ccm.getCurrentContext().get(CallerContext.APP_ID);
                OcapSecurityManager osm = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);
                MediaStorageVolumeImpl msv = (MediaStorageVolumeImpl) getDestination();
                if (!osm.hasWriteAccess(msv.getAppId(), msv.getFileAccessPermissions(), callingApp,
                        OcapSecurityManager.FILE_PERMS_ANY))
                {
                    throw new AccessDeniedException();
                }

                // ECR 874: If this recording is not pending,
                // it is not legal to set the bit rate, destination or priority
                if (state != LeafRecordingRequest.PENDING_NO_CONFLICT_STATE
                        && state != LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)
                {
                    // TODO: StorageManager - when full storage manager support
                    // is added
                    // this should check for equal destination device as well
                    if (m_info.getBitRate() != ocp.getBitRate() || m_info.getPriority() != ocp.getPriorityFlag())
                    {
                        throw new IllegalStateException("Invalid recording property change request.");
                    }
                }

                long tempExpirationPeriod = RecordingManagerImpl.roundOffOverflow(properties.getExpirationPeriod(),
                        m_info.getRequestedStartTime());
                // ECR 874: If this recording is CANCELLED or DELETED,
                // it is not legal to set retentionPriority or expiration period
                if (state == OcapRecordingRequest.CANCELLED_STATE || state == LeafRecordingRequest.DELETED_STATE)
                {
                    if (m_info.getRetentionPriority() != ocp.getRetentionPriority()
                            || m_info.getExpirationDate().getTime() != tempExpirationPeriod)
                    {
                        throw new IllegalStateException("Invalid recording property change request.");
                    }
                }

                // check if the expiration has changed - if so, reschedule
                // expiration
                if (m_info.getExpirationDate().getTime() != tempExpirationPeriod ||
                    m_info.getRetentionPriority() != ocp.getRetentionPriority())
                {
                    m_info.setExpirationDate(new Date(tempExpirationPeriod));
                    m_info.setRetentionPriority(ocp.getRetentionPriority());
                    removeAsPurgable();

                    Scheduler scheduler = Scheduler.getInstance();
                    scheduler.descheduleExpiration(this);
                    scheduler.scheduleExpiration(this);
                }

                m_info.setBitRate(ocp.getBitRate());
                m_info.setOrganization(ocp.getOrganization());
                m_info.setPriority(ocp.getPriorityFlag());
                m_info.setRetentionPriority(ocp.getRetentionPriority());
                m_info.setFap(ocp.getAccessPermissions() == null ? getDefaultFAP() : ocp.getAccessPermissions());
                m_info.setDestination(ocp.getDestination());
                m_info.setResourcePriority(ocp.getResourcePriority());
            }
            saveRecordingInfo(RecordingDBManager.ALL);
        } // END synchronized (m_sync)
    }
    
    /**
     * Performs the steps necessary to stop the time shift conversion.
     */
    private void stopTimeShiftConversion(boolean immediateStop)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        removeTableChangeListeners();
        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "stopTimeShiftConversion: Calling nStopTimeShiftConversion(handle 0x"
                    + Integer.toHexString(m_nativeTSBBufferHandle) + ", immediateStop " + immediateStop + ')');
        }
        nStopTimeShiftConversion(m_nativeRecordingHandle, immediateStop);
        m_activeRecordedService = null;
        m_nativeRecordingHandle = 0;
        if (immediateStop)
        {
            m_nativeConversionListener.deactivate();
            m_nativeConversionListener = null;
        }
    }

    /**
     * Deletes the recording request from the database. The method removes the
     * recording request, all its descendant recording requests, as well as the
     * corresponding {@link RecordedService} objects and all recorded elementary
     * streams (e.g., files and directory entries) associated with the
     * RecordedService. If any application calls any method on stale references
     * of removed objects the implementation shall throw an
     * IllegalStateException.
     * <p>
     * If the recording request is in the IN_PROGRESS state the implementation
     * will stop the recording before deleting the recording request. If a
     * RecordedService was being presented when it was deleted, a
     * {@link javax.tv.service.selection.PresentationTerminatedEvent} will be
     * sent with reason SERVICE_VANISHED.
     * </p>
     *
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("delete",..) or
     *             RecordingPermission("*",..)
     */
    public void delete() throws AccessDeniedException
    {
        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "Recording delete() Enter");
        }

        SecurityUtil.checkPermission(new RecordingPermission("delete", "own"));

        // Now clean up our resources
        synchronized (m_sync)
        {
            if (m_info.getState() == DESTROYED_STATE)
            {
                throw (new IllegalStateException("RecordingRequest already deleted"));
            }

            checkWriteExtFileAccPerm();

            // let the RecordingManager handle the removal of this recording from the
            //  nav/sched. the state is DELETED_STATE.
            Scheduler.getInstance().descheduleRecording(this);

            // The recording request would be in the deleted state because it expired,
            // or because its associated RecordingService object was deleted.
            // Deleted state means the RecordedService data was deleted.
            if (m_info.getState() != LeafRecordingRequest.DELETED_STATE)
            {
                // 'userStop' is true and 'immediate' is true
                m_istate.handleStop(true, true);
                deleteRecordedServiceData(true);
                setDeletionDetails(System.currentTimeMillis(), DeletionDetails.USER_DELETED);
                saveRecordingInfo(RecordingDBManager.DELETION_DETAILS);
            }

            // If this recording was marked as purge-able, remove it from the
            // retention manager.
            removeAsPurgable();

            if (!(m_istate instanceof IStateEnded))
            {
                final IStateEnded endState = new IStateEnded(false, false);
                m_istate.setState(endState);
            }

            m_rdbm.deleteRecord(m_info);

            // Tell parent I am ready to be forgotten and send a recording changed
            // event.
            ParentNodeImpl prr = m_parent;
            if (prr != null)
            {
                prr.notifyRemoveChild(this, LeafRecordingRequest.DELETED_STATE, m_info.getState());
            }

            NavigationManager.getInstance().removeRecording(this, LeafRecordingRequest.DELETED_STATE, m_info.getState());

            m_info.setState(DESTROYED_STATE);

            RecordingResourceManager rrm = RecordingResourceManager.getInstance();

            // Attempt to transfer reservation to overlapping recordings
            rrm.resolveConflictsForRecordingRemoval(this);
        } // END synchronized (m_sync)
    }

    /**
     * Called by RecordingImpl and RecordedServiceImpl to dispose of the
     * recording/native resources.
     *
     * @param notifyRTM
     *            Notify the retention manager that the disk space has changed.
     *            Should be set to false if this deletion originated in the
     *            retention manager, in which case it already knows.
     */
    public void deleteRecordedServiceData(final boolean notifyRTM)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "deleteRecordedServiceData(notifyRTM " + notifyRTM + ')');
        }
        final RecordingRetentionManager rtm = RecordingRetentionManager.getInstance();

        // If this recording was marked as purgeable, remove it from the
        // retention manager.
        removeAsPurgable();

        {
            // Make a copy of the Players currently associated with this recording (while we 
            //  have the lock)
            final Vector playerListCopy = (Vector) m_players.clone();

            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            ccm.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    // Stop all the players before deleting the recording (do this while not holding the lock)
                    Enumeration players = playerListCopy.elements();
                    while (players.hasMoreElements())
                    {
                        AbstractDVRServicePlayer pl = (AbstractDVRServicePlayer) players.nextElement();
                        try
                        {
                            pl.close();
                        }
                        catch (Exception e)
                        { // Perhaps playback has already been stopped? (but Player was still in the list)
                            if (log.isInfoEnabled())
                            {
                                log.info(m_logPrefix + "Exception stopping player for recording (this may be OK): " + pl, e);
                            }
                        }
                    }

                    synchronized(m_sync)
                    {
                        // Iterate through the SegmentedRecordedService's RecordedService
                        // vector and delete the native recordings.
                        if (m_segmentedRecordedService != null)
                        {
                            List segments = m_segmentedRecordedService.getRecordedServices();
                            for (Iterator iter = segments.iterator();iter.hasNext();)
                            {
                                RecordedServiceImpl service = (RecordedServiceImpl) iter.next();
                                if (log.isInfoEnabled())
                                {
                                    log.info(m_logPrefix + "Deleting native recording:" + service.getNativeName());
                                }
                                if (service.getNativeName() != null)
                                {
                                    nDeleteRecording(service.getNativeName());
                                    if (notifyRTM)
                                    {
                                        rtm.notifySpaceAvailable(getVolume());
                                    }
                                }
                            }
                            // Delete the SegmentedRecordedService member and the the
                            // segment info.
                            m_segmentedRecordedService.deleteRecordedServices();
                        } // END if (m_segmentedRecordedService)

                        m_info.deleteRecordedSegmentInfoElements();
                    } // END synchronized (m_sync)
                } // END run()
            } ); // END runnable
        }
    }

    public void updateFinalStateForRecordedDuration()
    {
        if (ASSERTING) Assert.lockHeld(m_sync);

        long recordedDuration = getRecordedDuration();

        // Do not do anything. It is possible that this recording is in SATA
        if (recordedDuration <= 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "updateFinalStateForRecordedDuration: Recording " + this
                        + "(length is zero)- FAILED");
            }
            setStateNoNotify(LeafRecordingRequest.FAILED_STATE);
        }
        else if (recordedDuration >= (m_info.getRequestedDuration() - m_recLengthTolerance))
        { // Recording length is as long (or nearly as long) as
            // the scheduled duration
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "updateFinalStateForRecordedDuration: Recording " + this
                        + " COMPLETED (recording length is sufficient)");
            }

            setStateNoNotify(LeafRecordingRequest.COMPLETED_STATE);
        }
        // Recording is shorter than the acceptable duration but greater than 0
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "updateFinalStateForRecordedDuration: Recording " + this
                        + " INCOMPLETE (recording length is insufficient)");
            }
            setStateNoNotify(LeafRecordingRequest.INCOMPLETE_STATE);
        }
    } // END updateFinalStateForRecordedDuration()

    /**
     * Gets the estimated space, in bytes, required for the recording.
     *
     * @return Space required for the recording in bytes. This method returns
     *         zero if the recordings is in failed state.
     */
    public long getSpaceRequired()
    {
        synchronized (m_sync)
        {
            if (m_info.getState() == OcapRecordingRequest.FAILED_STATE)
            {
                return 0L;
            }

            if (m_info.getState() == RecordingRequestImpl.DESTROYED_STATE)
            {
                throw (new IllegalStateException(m_logPrefix + "RecordingRequest has been deleted."));
            }

            // TODO: We estimate space required for a recording by assuming HD?
            // for SD use 6000 Kb/s
            // Use 'ini' variable to differentiate between SD or HD

            // duration(ms) * (~19500kb/s) / (8 bits per byte * 1000 ms/s)
            return m_info.getRequestedDuration() * (19500000L) / (8L * 1000L);
        } // END synchronized (m_sync)
    }

    /**
     * Gets any other RecordingRequest that overlaps with the duration of this
     * recording request. This method will return null unless the recording
     * request is in the PENDING_WITH_CONFLICTS_STATE,
     * PENDING_NO_CONFLICTS_STATE, IN_PROGRESS_INSUFFICIENT_SPACE_STATE or
     * IN_PROGRESS_STATE. The returned list will contain only overlapping
     * recording requests for which the application has read access permission.
     * The the RecordingList returned is only a copy of the list of overlapping
     * entries at the time of this method call. This list is not updated if
     * there are any changes. A new call to this method will be required to get
     * the updated list.
     *
     * @return a RecordingList
     */
    public RecordingList getOverlappingEntries()
    {
        synchronized (m_sync)
        {
            int state = m_info.getState();

            if (state == RecordingRequestImpl.DESTROYED_STATE)
                throw new IllegalStateException("m_info.getState() == DESTROYED_STATE");

            // Writing this logic in reverse in hopes of making this more
            // future-
            // proof (since it already is neglecting to consider
            // IN_PROGRESS_WITH_ERROR)
            if ((state == LeafRecordingRequest.COMPLETED_STATE) || (state == LeafRecordingRequest.FAILED_STATE)
                    || (state == LeafRecordingRequest.DELETED_STATE)
                    || (state == LeafRecordingRequest.INCOMPLETE_STATE))
            {
                return null;
            }

            return NavigationManager.getInstance().getOverlappingReadableEntries(this);
        }
    }

    /**
     * Returns whether the destined <code>MediaStorageVolume</code> for this
     * recording is present and ready or not. This method SHALL return
     * <code>true</code> under the following conditions:
     * <ul>
     * <li>
     * <code>getRecordingProperties().getDestination().getStatus() == READY</code>
     * <li> <code>getRecordingProperties().getDestination()</code> returns null
     * and a default recording volume exists for which
     * <code>getStatus() == READY</code>
     * </ul>
     * Otherwise, <code>false</code> SHALL be returned.
     *
     * @return <code>true</code> if the explicit or an implicit destination
     *         volume is present and ready; <code>false</code> otherwise
     */
    public boolean isStorageReady()
    {
        StorageProxy sp = null;
        MediaStorageVolume msv = m_info.getDestination();
        String msvName = m_info.getMSVReference().getVolumeName();

        // if msv is null and there is no msv name, use the default storage
        // proxy
        if (msv == null)
        {
            if (msvName == null)
            {
                sp = m_recordingManager.getDefaultStorageProxy();
            } // else fail in check for a storageProxy
        }
        else
        {
            sp = msv.getStorageProxy();
        }
        if (sp == null || sp.getStatus() != StorageProxy.READY)
            return false;
        else
            return true;
    }

    /**
     * Cancels a pending recording request. The recording request will be
     * deleted from the database after the sucessful invocation of this method.
     *
     * Cancelling a recording request may resolve one or more conflicts. In this
     * case some pending recordings with conflicts would be changed to pending
     * without conflicts.
     *
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("cancel",..) or
     *             RecordingPermission("*",..)
     * @throws IllegalStateException
     *             if the stateof the recording is not in
     *             PENDING_STATE_NO_CONFLICT_STATE or
     *             PENDING_WITH_CONFLICT_STATE.
     */
    public void cancel() throws IllegalStateException, AccessDeniedException
    {
        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "Recording cancel() Enter");
        }

        SecurityUtil.checkPermission(new RecordingPermission("cancel", "own"));
        synchronized (m_sync)
        {
            checkWriteExtFileAccPerm();
            int state = m_info.getState();
            if (state != PENDING_WITH_CONFLICT_STATE && state != PENDING_NO_CONFLICT_STATE)
            {
                throw new IllegalStateException("State=" + state);
            }
            final IStateEnded endState = new IStateEnded(true, false);
            m_istate.setState(endState);
            setStateAndNotify(endState.getCompletionStateCode());
            saveRecordingInfo(RecordingDBManager.STATE);
        }

        RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(this);

        Scheduler scheduler = Scheduler.getInstance();
        scheduler.cancelRecording(this);

        // TODO: refactor
        // DVRActivityLogger.getInstance().log(DVRActivityLogger.DVR_CANCEL_REQUEST,
        // this);
    }

    /**
     * Stops the recording for an in-progress recording request regardless of
     * how much of the duration has been recorded. Moves the recording to the
     * INCOMPLETE_STATE.
     *
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("cancel",..) or
     *             RecordingPermission("*",..)
     * @throws IllegalStateException
     *             if the recording is not in the IN_PROGRESS_STATE, or
     *             IN_PROGRESS_INSUFFICIENT_SPACE_STATE or
     *             IN_PROGRESS_WITH_ERROR_STATE, or
     *             IN_PROGRESS_INCOMPLETE_STATE..
     */
    public void stop() throws IllegalStateException, AccessDeniedException
    {
        SecurityUtil.checkPermission(new RecordingPermission("cancel", "own"));
        synchronized (m_sync)
        {
            checkWriteExtFileAccPerm();

            int state = m_info.getState();
            if ( state != IN_PROGRESS_STATE
                 && state != IN_PROGRESS_INSUFFICIENT_SPACE_STATE
                 && state != IN_PROGRESS_WITH_ERROR_STATE
                 && state != IN_PROGRESS_INCOMPLETE_STATE )
            {
                throw new IllegalStateException("State=" + state);
            }
            // handle stop request
            // 'userStop' is true and 'immediate' is true
            m_istate.handleStop(true, true);
            RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(this);
        }
    }

    /**
     * Gets the exception that caused the recording request to enter the
     * <code>FAILED_STATE</code>, or <code>INCOMPLETE_STATE</code> or
     * <code>IN_PROGRESS_WITH_ERROR_STATE</code>, or
     * <code>IN_PROGRESS_INCOMPLETE_STATE</code>.
     *
     * @return The exception that caused the failure. The exception returned
     *         will be a RecordingFailedException.
     *
     *
     * @throws IllegalStateException
     *             if the recording request is not in the FAILED_STATE or
     *             INCOMPLETE_STATE or IN_PROGRESS_WITH_ERROR_STATE, or
     *             IN_PROGRESS_INCOMPLETE_STATE.
     */
    public Exception getFailedException() throws IllegalStateException
    {
        synchronized (m_sync)
        {
            int state = m_info.getState();
            if (!((state == LeafRecordingRequest.FAILED_STATE) || (state == LeafRecordingRequest.INCOMPLETE_STATE)
                    || (state == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE) || (state == LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE)))
            {
                throw new IllegalStateException("RecordingRequest is in state " + state);
            }

            return new RecordingFailedException(m_info.getFailedExceptionReason());
        }
    }

    /**
     * Returns the {@link SegmentedRecordedService} corresponding to the
     * recording request.
     *
     * @return The recorded service associated with the recording request.
     * @throws IllegalStateException
     *             if the recording request is not in INCOMPLETE_STATE,
     *             IN_PROGRESS_STATE, IN_PROGRESS_INSUFFICIENT_SPACE_STATE,
     *             IN_PROGRESS_WITH_ERROR_STATE, IN_PROGRESS_INCOMPLETE_STATE,
     *             or COMPLETED_STATE.
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     */
    public SegmentedRecordedService getService() throws IllegalStateException, AccessDeniedException
    {
        synchronized (m_sync)
        {
            // Per LeafRecordingRequest.getState() Javadoc
            final int state = m_info.getState();
            if (!stateImpliesRecordedService())
            {
                throw new IllegalStateException(this.toString() + " is in State " + state);
            }

            if (!isPlayable())
            {
                return null;
            }

            return m_segmentedRecordedService;
        }
    }

    /**
     * Gets the duration requested for the recording.
     *
     * @return The duration of the recording in milli-seconds.
     */
    public long getDuration()
    {
        synchronized (m_sync)
        {
            return m_info.getRequestedDuration();
        }
    }

    /**
     * Inserts this recording into the retention manager as eligable for
     * purging. If the recording is already purgable, this call is ignored.
     */
    void addAsPurgable()
    {
        synchronized (m_sync)
        {
            if (!m_purgable)
            {
                m_purgable = true;
                RecordingRetentionManager.getInstance().insertPurgableRecording(this);
            }
        }
    }

    /**
     * Removes this recording from the retention manager as elegable for
     * purging. If the recording is not currently purgable, this call is
     * ignored.
     */
    void removeAsPurgable()
    {
        synchronized (m_sync)
        {
            if (m_purgable)
            {
                m_purgable = false;
                RecordingRetentionManager.getInstance().removePurgableRecording(this);
            }
        }
    }

    /**
     * Gets the duration actually recorded for the recording.
     *
     * @return The duration of the recording in milli-seconds.
     */
    public long getRecordedDuration()
    {
        long totalDuration = 0;
        long duration = 0;
        synchronized (m_sync)
        {
            if (m_info.getState() == RecordingRequestImpl.DESTROYED_STATE) throw new IllegalStateException();

            Enumeration seg_enum = m_info.getRecordedSegmentInfoElements();
            while (seg_enum.hasMoreElements())
            {
                RecordedSegmentInfo seg_info = (RecordedSegmentInfo) seg_enum.nextElement();
                final String recordingId = seg_info.getNativeRecordingName();
                if (recordingId != null)
                {
                    duration = nGetRecordedDurationMS(recordingId);
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "getRecordingDuration::nativeRecordingName: " + recordingId
                                + " duration: " + duration + "ms");
                    }
                    totalDuration += duration;
                }
            }
            return totalDuration;
        }
    }

    /**
     * Retrieves the recordings AlarmSpec obI mayject.
     *
     * @return The AlarmSpec object for this recording.
     */
    public Object getAlarmSpec()
    {
        return m_alarmSpec;
    }

    public Object getExpirSpec()
    {
        return m_expirSpec;
    }

    public MediaStorageVolumeExt getDestination()
    {
        return (MediaStorageVolumeExt) getVolume();
    }

    /**
     * Set the AlarmSpec object.
     *
     * @param m_spec
     *            the alarm spec.
     */
    public void setAlarmSpec(Object spec)
    {
        m_alarmSpec = spec;
    }

    public void setExpirSpec(Object expirSpec)
    {
        m_expirSpec = expirSpec;
    }

    public void setFailedExceptionReason(int reason)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isInfoEnabled())
        {
            log.info( m_logPrefix + "setFailedExceptionReason: setting reason to "
                       + failureReasonToString(reason) + " (" + reason +')' );
        }
        m_info.setFailedExceptionReason(reason);
    }


    /**
     * Mark the current segment as copy protected (do not copy)
     */
    public void setCurrentSegmentCopyProtected()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        RecordedServiceImpl lastRS = this.m_segmentedRecordedService.getLastSegment();

        if (ASSERTING) Assert.condition(lastRS != null, "setSegmentCopyNoMore called with no segments");

        if (lastRS != null)
        {
            lastRS.setCopyProtected(true);
        }
    }

    /**
     *
     * @return returns the start time for this recording.
     */
    public long getRequestedStartTime()
    {
        synchronized (m_sync)
        {
            return m_info.getRequestedStartTime();
        }
    }

    /**
     *
     * @return returns the request priority
     *         (RECORD_WITH_CONFLICTS/RECORD_IF_NO_CONFLICTS)
     */
    public byte getPriority()
    {
        synchronized (m_sync)
        {
            return m_info.getPriority();
        }
    }

    /**
     *
     * @return returns the request expiration date
     */
    Date getExpirationDate()
    {
        synchronized (m_sync)
        {
            return m_info.getExpirationDate();
        }
    }

    int getRetentionPriority()
    {
        synchronized (m_sync)
        {
            return m_info.getRetentionPriority();
        }
    }

    /**
     * Return the service locator associated with this recording
     */
    public OcapLocator getServiceLocator()
    {
        return m_info.getServiceLocators()[0];
    }

    public void setRootAndParentRecordingRequest(RecordingRequest root, RecordingRequest parent)
    {
        // if request is null, then it is root.
        // otherwise it is not root and request is stored.
        if (root == null)
        { // if null root param, then I am root
            // therefore have not parent.
            // Assert: parent == null
            m_parent = null;
            m_root = this;
            m_isRoot = true;
        }
        else
        {
            // if root is someone else,
            // then this recording has a parent.
            m_root = root;
            m_isRoot = false;
            m_parent = (ParentNodeImpl) parent;
        }
    }

    /**
     * retrieves the locator of the service to be recorded for this recording.
     * If the recording is based on ServiceContext, null will be returned
     *
     * @return the locator corresponding to the service to be selected
     */
    public OcapLocator[] getLocator()
    {
        synchronized (m_sync)
        {
            return m_info.getServiceLocators();
        }
    }

    /**
     * called by our implementation to start the native recording
     */
    public void startInternal()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Internal start method called:" + this);
        }

        synchronized (m_sync)
        {
            m_istate.handleStart();
        }
    }

    /**
     * called by our implementation to stop the native recording
     */
    public void stopInternal()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Internal stop method called:" + this);
        }
        synchronized (m_sync)
        {
            // 'userStop' is false and 'immediate' is false
            m_istate.handleStop(false, false);
        }
    }

    /**
     * called outside the class but is user induced
     */
    public void stopExternal()
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "External stop method called:" + this);
        }
        synchronized (m_sync)
        {
            // 'userStop' is true and 'immediate' is true
            m_istate.handleStop(true, true);
        }
    }

    /**
     * called by our implementation to trigger this recording's expiration
     */
    public void expire()
    {
        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "Recording expiration timer triggered: " + this);
        }

        synchronized (m_sync)
        {
            m_istate.handleExpiration();
        }
    }

    /**
     * returns the native name corresponding to this recording
     */
    public String getName()
    {
        synchronized (m_sync)
        {
            Enumeration seg_enum = m_info.getRecordedSegmentInfoElements();

            if (seg_enum.hasMoreElements())
            {
                RecordedSegmentInfo segment_info = (RecordedSegmentInfo) seg_enum.nextElement();
                return (segment_info != null) ? segment_info.getNativeRecordingName() : null;
            }
            return null;
        }
    }

    /**
     * set the media playback time for a recorded service
     *
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)*
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     */
    public void setMediaTime(long time) throws AccessDeniedException
    {
        SecurityUtil.checkPermission(new RecordingPermission("modify", "own"));
        synchronized (m_sync)
        {
            checkWriteExtFileAccPerm();

            m_info.setMediaTime(time);
            saveRecordingInfo(RecordingDBManager.MEDIA_TIME);
        }
    }

    /**
     * get the media playback time for a recorded service
     */
    public long getMediaTime()
    {
        synchronized (m_sync)
        {
            return m_info.getMediaTime();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.ocap.shared.dvr.LeafRecordingRequest#getDeletionDetails()
     */
    public DeletionDetails getDeletionDetails() throws IllegalStateException
    {
        synchronized (m_sync)
        {
            final int state = m_info.getState();

            if (!((state == LeafRecordingRequest.DELETED_STATE) || (state == RecordingRequestImpl.DESTROYED_STATE)))
            {
                throw new IllegalStateException("RecordingRequest is in state " + state);
            }

            return (m_delDetails != null) ? m_delDetails : (m_delDetails = new DeletionDetails(
                    m_info.getDeletionReason(), new Date(m_info.getDeletionTime())));
        } // END synchronized (m_sync)
    }

    public void setDeletionDetails(long deleteTime, int reason)
    {
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (m_info.getState() == RecordingRequestImpl.DESTROYED_STATE) throw new IllegalStateException();

        m_info.setDeletionTime(deleteTime);
        m_info.setDeletionReason(reason);
    }

    /**
     * Modify the details of a recording request. The recording request shall be
     * re-evaluated based on the newly provided RecordingSpec. Rescheduling a
     * root recording request may result in state transitions for the root
     * recording request or its child recording requests. Rescheduling a root
     * recording request may also result in the scheduling of one or more new
     * child recording requests, or the deletion of one or more pending child
     * recording requests.
     * <p>
     * Note: If the recording request or one of its child recording request is
     * in IN_PROGRESS_STATE or IN_PROGRESS_INSUFFICIENT_SPACE_STATE, any changes
     * to the start time shall be ignored. In this case all other valid
     * parameters are applied. If the new value for a parameter is not valid
     * (e.g. the start-time and the duration is in the past), the implementation
     * shall ignore that parameter. In-progress recordings shall continue
     * uninterrupted, if the new recording spec does not request the recording
     * to be stopped.
     *
     * @param newRecordingSpec
     *            the new recording spec that shall be used to reschedule the
     *            root RecordingRequest.
     *
     * @throws IllegalArgumentException
     *             if the new recording spec and the current recording spec for
     *             the recording request are different sub-classes of
     *             RecordingSpec.
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)*
     */
    public void reschedule(RecordingSpec newRecordingSpec) throws AccessDeniedException
    {
        SecurityUtil.checkPermission(new RecordingPermission("modify", "own"));
        // Used to save off the orig. start time and duration.
        long startTime;
        long duration;

        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "Recording reschedule Enter ");
        }

        synchronized (m_sync)
        {
            checkWriteExtFileAccPerm();

            if (m_info.getState() == RecordingRequestImpl.DESTROYED_STATE) throw new IllegalStateException();
            // Save off the originalstart time and duration to use in the
            // resolveConflictsForRecordingChange below.
            startTime = m_info.getRequestedStartTime();
            duration = m_info.getRequestedDuration();

            // Update passed in parameters based on spec subtype
            if (newRecordingSpec instanceof LocatorRecordingSpec)
            {
                if (!(m_recordingSpec instanceof LocatorRecordingSpec))
                    throw new IllegalArgumentException("Illegal RecordingSpec sub-class");
                updateLocatorRecordingSpec((LocatorRecordingSpec) newRecordingSpec);
            }
            else if (newRecordingSpec instanceof ServiceContextRecordingSpec)
            {
                if (!(m_recordingSpec instanceof ServiceContextRecordingSpec))
                    throw new IllegalArgumentException("Illegal RecordingSpec sub-class");

                updateServiceContextRecordingSpec((ServiceContextRecordingSpec) newRecordingSpec);
            }
            else if (newRecordingSpec instanceof ServiceRecordingSpec)
            {
                if (!(m_recordingSpec instanceof ServiceRecordingSpec))
                    throw new IllegalArgumentException("Illegal RecordingSpec sub-class");

                updateServiceRecordingSpec((ServiceRecordingSpec) newRecordingSpec);
            }
            else
            {
                throw new IllegalArgumentException("Unknown RecordingSpec sub-class");
            }

            // If the expiration date is in the future, make sure we are not
            // currently
            // marked as purgable.
            if (m_info.getExpirationDate().getTime() > System.currentTimeMillis())
            {
                removeAsPurgable();
            }
        }

        // resolve conflict outside of the sync block b/c of resource contention
        // handling.
        RecordingResourceManager.getInstance().resolveConflictsForRecordingChange(this, startTime, duration);

        synchronized (m_sync)
        {
            // update the scheduled timers
            rescheduleTimers();
        }

        // TODO: refactor
        // DVRActivityLogger.getInstance().log(DVRActivityLogger.DVR_RESCHEDULE,
        // this);
    }

    /**
     * calls the scheduler to reschedule all recording timers
     */
    void rescheduleTimers()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        // Update the scheduled recording events(start/stop, etc)
        // 1: cancel all scheduled events
        Scheduler scheduler = Scheduler.getInstance();
        scheduler.cancelRecording(this);

        // 2: reschedule recording events - only schedule a start
        // if we're pending (not in progress or complete)
        if (m_info.getState() == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE
                || m_info.getState() == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)
        {
            scheduler.scheduleRecording(this, m_info.getRequestedStartTime(), m_info.getRequestedDuration(),
                    m_info.getExpirationDate().getTime(), false);
        }
        else
        {
            scheduler.scheduleRecording(this, m_info.getRequestedStartTime(), m_info.getRequestedDuration(),
                    m_info.getExpirationDate().getTime(), true);
        }
    }

    /**
     * Update our internal recording info based on a new locator recording spec.
     * Follow the update rules specified by the reschedule() doc as well as
     * additional details in the LocatorRecordingSpec javadoc (ie, Locators must
     * match original scheduled locators)
     *
     * @param lrs
     */
    void updateLocatorRecordingSpec(LocatorRecordingSpec lrs)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "updateLocatorRecordingSpec Enter ");
        }
        // TODO: check source locators for validity

        // If we are in progress, we must verify that the source locators match
        if (m_info.getState() == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE
                || m_info.getState() == LeafRecordingRequest.IN_PROGRESS_STATE
                || m_info.getState() == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE
                || m_info.getState() == LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE)
        {
            OcapLocator[] arr = new OcapLocator[lrs.getSource().length];
            try
            {
                for (int i = 0; i < lrs.getSource().length; i++)
                {
                    arr[i] = LocatorUtil.convertJavaTVLocatorToOcapLocator(lrs.getSource()[i]);
                }
            }
            catch (InvalidLocatorException e)
            {
                throw new IllegalArgumentException("Invalid locators.");
            }

            if (!checkSourceLocators(m_info.getServiceLocators(), arr))
            {
                throw new IllegalArgumentException("Source Locator Mismatch.");
            }
        }
        else
        {
            // only update time if we're not in progress
            m_info.setRequestedStartTime(lrs.getStartTime().getTime());
            // update the locator
            m_info.setServiceLocators((OcapLocator[]) lrs.getSource());
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "updateLocatorRecordingSpec update start time: "
                        + m_info.getRequestedStartTime());
            }
        }

        // update duration if valid
        if (m_info.getRequestedStartTime() + lrs.getDuration() > System.currentTimeMillis())
        {
            m_info.setRequestedDuration(lrs.getDuration());

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "updateLocatorRecordingSpec update duration: " + m_info.getRequestedDuration());
            }
        }

        updateRecordingProperties(lrs.getProperties());

        saveRecordingInfo(RecordingDBManager.ALL);
    }

    /**
     * Update our internal recording info based on a new service recording spec.
     * Follow the update rules specified by the reschedule() doc as well as
     * additional details in the LocatorRecordingSpec javadoc (ie, Locators must
     * match original scheduled locators)
     *
     * @param lrs
     */
    void updateServiceRecordingSpec(ServiceRecordingSpec srs)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "updateServiceRecordingSpec Enter ");
        }
        // TODO: check source locators for validity

        // If we are in progress, we must verify that the source locators match
        if (m_info.getState() == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE
                || m_info.getState() == LeafRecordingRequest.IN_PROGRESS_STATE
                || m_info.getState() == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE
                || m_info.getState() == LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE)
        {
            if (!m_info.getServiceLocators()[0].equals(srs.getSource().getLocator()))
            {
                throw new IllegalArgumentException("Service Mismatch.");
            }
        }
        else
        {
            // only update time if we're not in progress and time is valid
            // (future)
            m_info.setRequestedStartTime(srs.getStartTime().getTime());
        }

        // update duration if valid
        if (m_info.getRequestedStartTime() + srs.getDuration() > System.currentTimeMillis())
            m_info.setRequestedDuration(srs.getDuration());

        updateRecordingProperties(srs.getProperties());

        saveRecordingInfo(RecordingDBManager.ALL);
    }

    /**
     * Update our internal recording info based on a new service context
     * recording spec. Follow the update rules specified by the reschedule() doc
     * as well as additional details in the ServiceContextRecordingSpec javadoc
     *
     * @param scrs
     *            the service context recording spec
     */
    void updateServiceContextRecordingSpec(ServiceContextRecordingSpec scrs)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "updateServiceContextRecordingSpec Enter ");
        }
        // TODO: check source locators for validity

        // If we are in progress, we must verify that the source locators match
        if (scrs.getServiceContext() != m_scheduledServiceContext)
            throw new IllegalArgumentException("Source ServiceContext Mismatch.");

        // Ignore start times for rescheduling based on ServiceContext
        // (should be inprogress or complete m_infp.startTime = update duration
        // if valid
        if (m_info.getRequestedStartTime() + scrs.getDuration() > System.currentTimeMillis())
            m_info.setRequestedDuration(scrs.getDuration());

        updateRecordingProperties(scrs.getProperties());

        saveRecordingInfo(RecordingDBManager.ALL);
    }

    /**
     * Update this recording according to RecordingProperties for reschedule
     *
     * @param properties
     *            the new set of Recording properties
     */
    void updateRecordingProperties(RecordingProperties properties)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        long tempExpirationPeriod = RecordingManagerImpl.roundOffOverflow(properties.getExpirationPeriod(),
                m_info.getRequestedStartTime());
        // update expiration if valid
        if (tempExpirationPeriod > System.currentTimeMillis())
            m_info.setExpirationDate(new Date(tempExpirationPeriod));
        // Update OCAP recording properties. These parameters will already have
        // been validated
        // by the OcapRecordingProperties impl
        if (properties instanceof OcapRecordingProperties)
        {
            OcapRecordingProperties orp = (OcapRecordingProperties) properties;

            // Only update these properties if we're not in progress
            if (m_info.getState() != LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE
                    && m_info.getState() != LeafRecordingRequest.IN_PROGRESS_STATE
                    && m_info.getState() != LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE)
            {
                m_info.setBitRate(orp.getBitRate());
            }
            m_info.setPriority(orp.getPriorityFlag());
            m_info.setRetentionPriority(orp.getRetentionPriority());
            m_info.setFap(orp.getAccessPermissions() == null ? getDefaultFAP() : orp.getAccessPermissions());
            m_info.setOrganization(orp.getOrganization());
            m_info.setDestination(orp.getDestination());
            m_info.setResourcePriority(orp.getResourcePriority());
        }
    }

    /**
     * Verifies that loc1 and loc2 idenify the same set of source locators
     *
     * @param loc1
     *            array of OcapLocators
     * @param loc2
     *            array of OcapLocators
     * @return true if loc1 and loc2 are the same sources
     */
    boolean checkSourceLocators(OcapLocator[] loc1, OcapLocator[] loc2)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (loc1 != loc2) // same array or both null
        {
            if (loc1 == null || loc2 == null || loc1.length != loc2.length) return false;

            for (int i = 0; i < loc1.length; ++i)
            {
                if (loc1[i] == null)
                {
                    if (loc2[i] != null) return false;
                }
                else if (!loc1[i].equals(loc2[i]))
                {
                    return false;
                }
            }
        }
        return true;
    }

    public int getArtificialCarouselID()
    {
        // TODO: Make this more real.
        return 0xffff0001;
    }

    int[] getBroadcastCarouselPIDs()
    {
        return null;
    }

    /**
     * returns true if this recording is currently being presented
     */
    public boolean isPresenting()
    {
        synchronized (m_sync)
        {
            return !m_players.isEmpty();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasPlayableRecordedService()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);
        
        return (stateImpliesRecordedService() && isPlayable());
    }

    private boolean stateImpliesRecordedService()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        final int state = m_info.getState();
        return ( state == INCOMPLETE_STATE
                 || state == IN_PROGRESS_STATE
                 || state == IN_PROGRESS_INSUFFICIENT_SPACE_STATE
                 || state == IN_PROGRESS_WITH_ERROR_STATE
                 || state == IN_PROGRESS_INCOMPLETE_STATE
                 || state == COMPLETED_STATE );
    }

    private boolean isPlayable()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        return ( (m_segmentedRecordedService != null)
                && (m_segmentedRecordedService.getSegments().length > 0)
                && isStorageReady() );
    }

    /**
     * Notify all registered players that playback of this recording must be
     * terminated. This method will block until all presentations have been
     * terminated.
     */
    private void terminateOngoingPresentations()
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        synchronized (m_sync)
        {
            if (m_players.isEmpty())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + " terminateOngoingPresentations - none found!");
                }
                return;
            }

            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + " terminateOngoingPresentations - " + m_recordingUpdateListeners.size()
                        + " listeners found.");
            }

            // verify that presentation condition is set false
            m_presentationTerminated.setFalse();

            // notify all listeners that this MSV is being disabled
            for (int i = 0; i < m_recordingUpdateListeners.size(); i++)
            {
                final RecordingUpdateListener rul = (RecordingUpdateListener) m_recordingUpdateListeners.elementAt(i);
                ccm.getSystemContext().runInContextAsync(new Runnable()
                {
                    public void run()
                    {
                        rul.notifyRecordingDisable(RecordingImpl.this);
                    }
                });
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Awaiting presentation termination  - " + this);
        }

        try
        {
            m_presentationTerminated.waitUntilTrue();
        }
        catch (Exception e)
        {
            if (log.isWarnEnabled())
            {
                log.warn(m_logPrefix + "Presentation termination interrupted!", e);
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Presentation termination  complete - " + this);
        }

    }

    /**
     * Adds a listener to receive notification upon recording state changes and
     * disables.
     *
     * @param listener
     */
    public void addRecordingUpdateListener(RecordingUpdateListener listener)
    {
        synchronized (m_sync)
        {
            if (!m_recordingUpdateListeners.contains(listener))
            {
                m_recordingUpdateListeners.add(listener);
            }
        }
    }

    /**
     * Removes a recording change listener
     *
     * @param listener
     */
    public void removeRecordingUpdateListener(RecordingUpdateListener listener)
    {
        synchronized (m_sync)
        {
            m_recordingUpdateListeners.remove(listener);
        }
    }

    /**
     * A utility method for determining if the calling application has the
     * necessary permissions in order to obtain the requested Recording Request
     * node (e.g., "parent" or "root").
     *
     * @see getRoot
     * @see getParent
     *
     * @return the requested node, if the calling application has the necessary
     *         access. Otherwise, <code>null</code> is returned.
     */
    private RecordingRequest getNode(RecordingRequest requested)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (null == requested)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "getNode() - Requested node specified by calling app is null!");
            }
            return null;
        }
        RecordingRequest node = null;
        try
        {
            SecurityUtil.checkPermission(new RecordingPermission("read", "*"));
            node = requested;
        }
        catch (SecurityException e1)
        {
            try
            {
                SecurityUtil.checkPermission(new RecordingPermission("read", "own"));
                AppID ownerAppID = requested.getAppID();
                CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                AppID callerAppID = (AppID) ccm.getCurrentContext().get(CallerContext.APP_ID);
                if (ownerAppID.equals(callerAppID))
                {
                    node = requested;
                }
            }
            catch (SecurityException e2)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "getNode() - Calling app does not have access to requested node: "
                            + requested, e2);
                }
            }
        }
        return node;
    }

    /**
     * Adds the listeners for PAT/PMT changes.
     */
    private void addTableChangeListeners()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Add table change listeners for:" + this + ":"
                    + m_info.getServiceLocators()[0].toExternalForm());
        }

        ProgramMapTableManagerImpl pmtManager = (ProgramMapTableManagerImpl) ProgramMapTableManager.getInstance();
        pmtManager.addInBandChangeListener(this, m_info.getServiceLocators()[0], RECORDING_PRIORITY);

        ProgramAssociationTableManagerImpl patManager = (ProgramAssociationTableManagerImpl) ProgramAssociationTableManager.getInstance();
        patManager.addInBandChangeListener(this, m_info.getServiceLocators()[0], RECORDING_PRIORITY);
    }

    /**
     * Removes the listeners for PAT/PMT changes.
     */
    private void removeTableChangeListeners()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "Remove table change listeners for: " + this + ":"
                    + m_info.getServiceLocators()[0].toExternalForm());
        }

        ProgramMapTableManager pmtManager = ProgramMapTableManager.getInstance();
        pmtManager.removeInBandChangeListener(this);

        ProgramAssociationTableManager patManager = ProgramAssociationTableManager.getInstance();
        patManager.removeInBandChangeListener(this);
    }

    /**
     * The AlarmSpec object.
     */
    private Object m_alarmSpec;

    private Object m_expirSpec;

    /**
     *
     * Given a new state and an old state, notify the navigation manager of a
     * state change if necessary
     *
     * @return
     */
    void notifyStateChange(int newState, int oldState)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (oldState == INIT_STATE) return;
        if (oldState == newState) return;

        if (newState == DESTROYED_STATE) // if we've transitioned to the
                                         // destroyed state,
            newState = oldState; // do not update the new state in the event
                                 // (not exposed)

        NavigationManager.getInstance().updateRecording(this, newState, oldState);
    }

    /*
     * receive notifications from StorageManager about changes to
     * MediaStorageVolumes
     */
    public void notifyChange(StorageManagerEvent sme)
    {
        synchronized (m_sync)
        {
            StorageProxy sp = sme.getStorageProxy();
            if (getVolume().getStorageProxy() == sp)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Notification of storage event");
                }
                m_istate.handleMsvEvent(sme);
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Recording does not exist in Storage Proxy " + sp.getName());
                }
            }
        }
    }

    /**
     * receive notification that a service has reappeared (switch digital case).
     * Attempt to restart recording.
     *
     */
    boolean notifyServiceAvailable()
    {
        /*
         * This is just a place holder for service reappear case. Currently no
         * one calls this method. The mechanism for service re-appearing is
         * still being flushed out!!
         */
        synchronized (m_sync)
        {
            return m_istate.handleServiceAvailable();
        }
    }

    public void notifyStoppingPlayer(ServiceContext sc, ServiceMediaHandler player)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyStoppingPlayer ...");
        }
        synchronized (m_sync)
        {
            if (m_scheduledServiceContext == sc)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Terminating service context recording...");
                }
                m_istate.handleSCTuneAway();
            }
        }
    }

    public void notifyPlayerStarted(ServiceContext sc, ServiceMediaHandler player)
    {
        if (log.isDebugEnabled())
        {
            log.debug("notifyPlayerStarted ...");
        }
        // Changes done for synchronization issue - Start
        // This synchronization block is commented as it is empty.
        // synchronized (m_sync)
        // {
            // Nothing to do currently
        // }
        // Changes done for synchronization issue - End
    }

    /**
     * Respond to a change in the PAT or PMT that refers to this recording.
     *
     * @return
     */
    public void notifyChange(SIChangeEvent event)
    {
        // The only PAT change that we care about is if a program we are
        // interested
        // in is removed from the PAT. But this change should also result in a
        // corresponding PMT change (type: REMOVE)
        // Based on the type of SIChangeEvent (ADD, REMOVE, MODIFY)
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "notifyChange::getChangeType(): " + event.getChangeType().toString()
                    + "; notifyChange::getSIElement(): " + event.getSIElement().toString());
        }

        if (event.getSIElement() instanceof ProgramAssociationTable)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "notifyChange::getSIElement() indicates a PAT change - ignoring... ");
            }
            return;
        }

        // Commented out for fixing findbugs issue - Start
        // This synchronization block is commented as the content was commented out already.
        // synchronized (m_sync)
        // {
            // m_istate.handlePMTChange(event);
        // }
        // Changes done for synchronization issue - End
    } // END notifyChange(SIChangeEvent event)

    /**
     * registers this recording to listen for buffering, schedule and storage
     * state changes. These registrations should be active during all
     * "in progress" states
     *
     */
    void registerSystemStateChangeListeners()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        // For all other cases, start monitoring our destination volume state
        // We will remain a registered listener until we enter the Ended state
        DVRStorageManager dsm = (DVRStorageManager) ManagerManager.getInstance(org.cablelabs.impl.manager.StorageManager.class);
        dsm.addMediaStorageVolumeUpdateListener(RecordingImpl.this);

        // Start monitoring system wide buffering state changes
        m_recordingManager.addDisableBufferingListener(RecordingImpl.this);

        // Start monitoring RecordingManager schedule state changes
        m_recordingManager.addRecordingDisabledListener(RecordingImpl.this);
    }

    static final SimpleDateFormat s_shortDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ssZ");

    public String toString()
    {
        //
        // NOTE: This method should never acquire (directly or indirectly) the RecordingManager lock.
        //       Put more simply, only print data fields from the m_info structure.
        //
        StringBuffer sb = new StringBuffer();
        OcapLocator serviceLocator = null;
        if (m_recordingSpec instanceof LocatorRecordingSpec || m_recordingSpec instanceof ServiceRecordingSpec)
        {
            serviceLocator = m_info.getServiceLocators()[0];
        }
        /*
         * else if (m_recordingSpec instanceof ServiceContextRecordingSpec) {
         * serviceLocator = null; }
         */
        //findbugs detected problem. Date formats are not thread safe, so need some synchronization...
        String requestedStartTime;
        synchronized(s_shortDateFormat)
        {
            requestedStartTime = s_shortDateFormat.format(new Date(m_info.getRequestedStartTime())).toString();
        }
        sb.append("RI 0x")
                .append(Integer.toHexString(this.hashCode()))
                .append(":[id ")
                .append(m_info.getUniqueIDInt())
                .append(",app ")
                .append((m_info.getAppId() == null) ? "null" : m_info.getAppId().toString())
                .append(",loc ")
                .append((serviceLocator == null) ? "null" : serviceLocator.toString())
                .append(",start ")
                .append(requestedStartTime)
                .append(",dur ")
                .append(m_info.getRequestedDuration()/1000)
                .append("s,es ")
                .append(externalStateToString(m_info.getState()))
                .append(",is ")
                .append((m_istate == null) ? "null" : m_istate.toString())
                .append(",pri ")
                .append(m_info.getPriority());

        final Enumeration se = m_info.getRecordedSegmentInfoElements();
        if (!se.hasMoreElements())
        {
            sb.append(",no segments");
        }
        else
        {
            for (int s=1; se.hasMoreElements(); s++)
            {
                RecordedSegmentInfo rsi = (RecordedSegmentInfo) se.nextElement();
                String name = rsi.getNativeRecordingName();

                String actualStartTime;
                synchronized(s_shortDateFormat)
                {
                    actualStartTime = s_shortDateFormat.format(new Date(rsi.getActualStartTime()));
                }
                sb.append(",seg ")
                        .append(s)
                        .append(":[")
                        .append("start ")
                        .append(actualStartTime)
                        .append(",nname ")
                        .append((name == null) ? "null" : name)
                        .append(",tad ")
                        .append(rsi.getTimeAssociatedDetails())
                        .append(",ttc ")
                        .append(rsi.getLightweightTriggerEventTimeTable())
                        .append(']');
                if (se.hasMoreElements()) sb.append(',');
            }
        }
        sb.append(']');

        return sb.toString();
    } // END toString()

    RecordingImpl getRecordingInstance()
    {
        return this;
    }

    /**
     * Base class for RecordingImpl state management. Contains basic no-op
     * implementations of all potential state dependent requests.
     */
    abstract class IState
    {
        final String m_istateLogPrefix = m_logPrefix + "IState: ";

        /**
         * Process a request (external or internal) to stop this recording
         * @param immediate TODO
         *
         * @return the externally visible state upon completion
         */
        void handleStop(boolean userStop, boolean immediate)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handleStop()");
            }
        }

        /**
         * Process a request to start this recording
         *
         * @return the externally visible state upon completion
         */
        void handleStart()
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handleStart()");
            }
        }

        /**
         * Process ServiceContext tune away
         *
         * @return the externally visible state upon completion
         */
        void handleSCTuneAway()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            // Handle service context tune away scenario and terminate the
            // recording. This should be done synchronously from service context
            // to enable correct termination...
            if (m_scheduledServiceContext != null)
            {
                // Check to determine if we're a fully complete recording
                // and remove ourselves from the "in_progress" list
                RecordingRetentionManager.getInstance().removeInProgressRecording(RecordingImpl.this);

                // Since this is a ServiceContext tune away, treat it as
                // pre-mature stop and hence will end up in INCOMPLETE state
                final IStateEnded endState = new IStateEnded(false, true);
                setStateAndNotify(endState.getCompletionStateCode());
                m_istate.setState(endState);
                RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);

                RecordingImpl.this.setFailedExceptionReason(RecordingFailedException.TUNED_AWAY);
                RecordingImpl.this.saveRecordingInfo(RecordingDBManager.FAILED_EXCEPTION_REASON);
            }
            else
            {
                if (log.isWarnEnabled())
                {
                    log.warn(m_istateLogPrefix
                            + "Received handleSCTuneAway with no scheduled ServiceContext. m_istate:"
                            + RecordingImpl.this.m_istate.toString());
                }
            }
        }

        /**
         * Process events generated by the TimeShiftWindowClient This base
         * implementation of the event handler and it associated process methods
         * shall not process the events in any way This is left to subclasses to
         * define.
         *
         */
        void handleTswStateChange(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "handleTswStateChange Enter");
            }

            int state = ev.getNewState();

            switch (state)
            {
                case TimeShiftManager.TSWSTATE_RESERVE_PENDING:
                case TimeShiftManager.TSWSTATE_TUNE_PENDING:
                {
                    processTswEventTunePending(ev);
                    break;
                }
                case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
                {
                    processTswEventReadyToBuffer(ev);
                    break;
                }
                case TimeShiftManager.TSWSTATE_NOT_READY_TO_BUFFER:
                {
                    processTswEventNotReadyToBuffer(ev);
                    break;
                }
                case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                {
                    processTswEventBufferingPending(ev);
                    break;
                }
                case TimeShiftManager.TSWSTATE_BUFFERING:
                {
                    processTswEventBuffering(ev);
                    break;
                }
                case TimeShiftManager.TSWSTATE_IDLE:
                {
                    processTswEventIdle(ev);
                    break;
                }
                case TimeShiftManager.TSWSTATE_BUFF_SHUTDOWN:
                {
                    processTswEventBufShutdown(ev);
                    break;
                }
                case TimeShiftManager.TSWSTATE_INTSHUTDOWN:
                {
                    processTswEventIntShutdown(ev);
                    break;
                }
                default:
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("State not handled here");
                    }
                    break;
                }
            }
            return;
        }

        /**
         * Process handler functions tied to specific events generated by
         * TimeShiftWindowManager
         */
        void processTswEventTunePending(TimeShiftWindowStateChangedEvent ev)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring processTswEventTunePending()");
            }
        }

        void processTswEventReadyToBuffer(TimeShiftWindowStateChangedEvent ev)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring processTswEventReadyToBuffer()");
            }
        }

        void processTswEventNotReadyToBuffer(TimeShiftWindowStateChangedEvent ev)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring processTswEventNotReadyToBuffer()");
            }
        }

        void processTswEventBufferingPending(TimeShiftWindowStateChangedEvent ev)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring processTswEventBufferingPending()");
            }
        }

        void processTswEventBuffering(TimeShiftWindowStateChangedEvent ev)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring processTswEventBuffering()");
            }
        }

        void processTswEventIdle(TimeShiftWindowStateChangedEvent ev)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring processTswEventNotTuned()");
            }
        }

        void processTswEventBufShutdown(TimeShiftWindowStateChangedEvent ev)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring processTswEventBufShutdown()");
            }
        }

        void processTswEventIntShutdown(TimeShiftWindowStateChangedEvent ev)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring processTswEventIntShutdown()");
            }
        }

        void handleTswCCIChange(CopyControlInfo cci)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handleTswCCIChange()");
            }

        }
        /**
         * Process events generated by the the StorageManager
         *
         * @param sme
         */
        void handleMsvEvent(StorageManagerEvent sme)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handleMsvEvent() in state " + toString());
            }
        }

        void setState(IState newState)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + " setting state to " + newState.toString());
            }
            m_istate = newState;
        }

        /**
         * Default expiration handler. Policy is the same for all states except
         * for FAILED: If we're marked as delete at expiration, then tear down
         * all recording process, enter the ended state, and delete all content.
         *
         * If we're not marked delete at expiration, simply add ourselves as
         * purgable with the RecordingRetentionManager, unless we're marked as
         * FAILED (in which case, we'll never have content to delete
         */
        void handleExpiration()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            // If we're set to delete immediately, delete the recorded
            // service
            if (m_info.getRetentionPriority() == OcapRecordingProperties.DELETE_AT_EXPIRATION)
            {
                if (RecordingImpl.this.m_segmentedRecordedService != null)
                {
                    // 'userStop' is false and 'immediate' is true
                    handleStop(false, true);

                    deleteRecordedServiceData(true);
                    setDeletionDetails(System.currentTimeMillis(), DeletionDetails.EXPIRED);
                    saveRecordingInfo(RecordingDBManager.DELETION_DETAILS);
                    setStateAndNotify(LeafRecordingRequest.DELETED_STATE);
                }
                else
                {
                    SystemEventUtil.logRecoverableError(new Exception(
                            "Recorded service not found for expired recording!"));
                }
            }
            else
            {
                // We're not to expire immediately, add to the retention
                // manager
                if (m_info.getState() != OcapRecordingRequest.FAILED_STATE
                        && m_info.getState() != OcapRecordingRequest.DELETED_STATE
                        && m_info.getState() != OcapRecordingRequest.CANCELLED_STATE
                        && m_info.getState() != RecordingImpl.DESTROYED_STATE)
                {
                    addAsPurgable();
                }
            }
        }// End handleExpiration

        void handleNativeConversionStopped()
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handleNativeConversionStopped");
            }
        }

        void handleNativeSessionClosed(int eventCode)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handleNativeSessionClosed(" + eventCode + ")");
            }
        }

        public void handleNativeOutOfSpace(int eventCode)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handleNativeOutOfSpace(" + eventCode + ")");
            }
        }

        void handleBufferingStateChanged()
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handleBufferingStateChanged()");
            }

        }

        /**
         * Called in response to RecordingDisabledListener.notifyRecordingEnabledStateChange()
         */
        void handleRecordingEnabledStateChanged()
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handleRecordingEnabledStateChanged()");
            }
        }

        /**
         * notify the recording that a service has reappeared
         */
        boolean handleServiceAvailable()
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handleServiceAvailable()");
            }
            return false;
        }

        /**
         * notify the recording that SI change occured
         *
         * @param changeEvent
         *
         * @return false
         */
        void handlePMTChange(SIChangeEvent changeEvent)
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handlePMTChange()");
            }
        }


        /**
         * notification of space availability on the recording's MSV
         */
        public void handleSpaceAvailable()
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "ignoring handleSpaceAvailable()");
            }
        }

        public String toString()
        {
            return "RecordingImpl.IState";
        }
    } // END class IState

    /*
     * Initialization state.
     */
    class IStateInit extends IState
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateInit: ";

        IStateInit()
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + " - constucting");
            }
        }

        public String toString()
        {
            return "RecordingImpl.IStateInit";
        }
    }

    /*
     * Base pending state.
     */
    class IStatePending extends IState
    {
        final String m_istateLogPrefix = m_logPrefix + "IStatePending: ";

        IStatePending()
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + " - constucting");
            }
        }

        // handle the recording start request
        void handleStart()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "handleStart()");
            }

            // First check to see if there is a reservation and if the priority
            // tells the request to fail.
            if (m_info.getState() == PENDING_WITH_CONFLICT_STATE
                    && m_info.getPriority() == OcapRecordingProperties.RECORD_IF_NO_CONFLICTS)
            {
                // This recording should not be started w/ conflicts.
                // Transition into the failed state
                if (log.isInfoEnabled())
                {
                    log.info(m_istateLogPrefix + "Recording in conflict - failing: " + this);
                }

                final IStateEnded endState = new IStateEnded(false, false);
                setStateAndNotify(endState.getCompletionStateCode());
                m_istate.setState(endState);
                setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                setStateAndNotify(LeafRecordingRequest.FAILED_STATE);
                return;
            }

            // register for buffering, storage and schedule state changes
            registerSystemStateChangeListeners();

            // initiate acquisition of TSW resources and recording process
            initiateRecordingProcess(true);
        }

        public String toString()
        {
            return "RecordingImpl.IStatePending";
        }
    }

    /**
     * The WaitTuneSuccess state handles the selection of content to record. For
     * a scheduled locator based recording, this will include finding a
     * timeshiftwindow that internally reserves a network interface for tuning.
     */
    class IStateWaitTuneSuccess extends IState
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateWaitTuneSuccess: ";

        IStateWaitTuneSuccess()
        {
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }
        }

        void processTswEventReadyToBuffer(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            // attach for buffering+recording
            attachToTSW();
        }

        void processTswEventBuffering(TimeShiftWindowStateChangedEvent ev)
        {
            startRecording(m_info.getRequestedStartTime(), true);
        }

        void processTswEventNotReadyToBuffer(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            int recordingExceptionReason;
            switch (ev.getReason())
            {
                case TimeShiftManager.TSWREASON_ACCESSWITHDRAWN:
                { // See rules of DVR 6.2.1.1.3.3
                    m_istate.setState(new IStateSuspendedCaDenied());
                    recordingExceptionReason = RecordingFailedException.CA_REFUSAL;
                    break;
                }
                case TimeShiftManager.TSWREASON_SERVICEVANISHED:
                {
                    // No known recovery mode currently
                    m_istate.setState(new IStateSuspended());
                    recordingExceptionReason = RecordingFailedException.SERVICE_VANISHED;
                    break;
                }
                case TimeShiftManager.TSWREASON_SYNCLOST:
                {
                    m_istate.setState(new IStateSuspendedTunerNotReady());
                    recordingExceptionReason = RecordingFailedException.TUNING_FAILURE;
                    break;
                }
                case TimeShiftManager.TSWREASON_NOCOMPONENTS:
                default:
                {
                    m_istate.setState(new IStateSuspendedTunerNotReady());
                    recordingExceptionReason = RecordingFailedException.CONTENT_NOT_FOUND;
                    break;
                }
            }
            if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
            {
                setFailedExceptionReason(recordingExceptionReason);
            }
            setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
        }

        void processTswEventIdle(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "processTswEventNotTuned()");
            }

            // If current recording state is PENDING_NO_CONFLICT_STATE and
            // recording is created as
            // RECORD_IF_NO_CONFLICTS, the NoFreeInterfaceException indicates a
            // conflict
            // hence transition to failed
            if (m_info.getState() == PENDING_NO_CONFLICT_STATE
                    && m_info.getPriority() == OcapRecordingProperties.RECORD_IF_NO_CONFLICTS)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix
                            + "initiateRecordingProcess: Priority is RECORD_IF_NO_CONFLICTS - Failing the recording");
                }
                m_istate.setState(new IStateEnded(false, false));

                if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                {
                    setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                }
                RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
                setStateAndNotify(LeafRecordingRequest.FAILED_STATE);
            }

            switch (ev.getReason())
            {
                case TimeShiftManager.TSWREASON_NOFREEINT:
                case TimeShiftManager.TSWREASON_INTLOST:
                {
                    if (m_info.getState() == PENDING_NO_CONFLICT_STATE
                            && m_info.getPriority() == OcapRecordingProperties.RECORD_IF_NO_CONFLICTS)
                    { // Treat this the same way as if the RecordingRequest were
                      // PENDING_WITH_CONFLICT
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_istateLogPrefix + "Priority is RECORD_IF_NO_CONFLICTS - Failing the recording");
                        }
                        m_istate.setState(new IStateEnded(false, false));

                        if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                        {
                            setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                        }
                        RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
                        setStateAndNotify(LeafRecordingRequest.FAILED_STATE);
                    }
                    else
                    {
                        m_istate.setState(new IStateSuspendedTunerUnavailable());
                        setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                        setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                    }
                    break;
                }
                case TimeShiftManager.TSWREASON_SERVICEVANISHED:
                {
                    m_istate.setState(new IStateSuspendedServiceUnavailable());
                    setFailedExceptionReason(RecordingFailedException.SERVICE_VANISHED);
                    setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                    break;
                }
                case TimeShiftManager.TSWREASON_TUNEFAILURE:
                {
                    m_istate.setState(new IStateSuspendedTuneFailed());
                    setFailedExceptionReason(RecordingFailedException.TUNING_FAILURE);
                    setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                    break;
                }
                default:
                {
                    setFailedExceptionReason(RFE.REASON_NOT_KNOWN);
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_istateLogPrefix + "unexpected tuning event received "
                                + TimeShiftManager.reasonString[ev.getReason()] + " received in TSW_PENDING: " + this);
                    }
                    // Non-recoverable error - set to the ended state
                    m_istate.setState(new IStateEnded(false, false));
                    break;
                }
            }
        }

        void handleStop(boolean userStop, boolean immediate)
        {
            // If we received a stop call while in the WaitTuneSuccess state,
            // this means
            // that our find-TimeShiftWindow stage never completed, and our
            // physical recording failed.
            // We must then transition into the failed state
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "Stop called while tune pending");
            }
            //
            final IStateEnded endState = new IStateEnded(false, false);
            setStateAndNotify(endState.getCompletionStateCode());
            m_istate.setState(endState);

            if (m_info.getState() != LeafRecordingRequest.COMPLETED_STATE)
            {
                if (true == userStop)
                {
                    setFailedExceptionReason(RecordingFailedException.USER_STOP);
                }
                else
                {
                    setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                }
                RecordingImpl.this.saveRecordingInfo(RecordingDBManager.FAILED_EXCEPTION_REASON);
            }
        }

        public String toString()
        {
            return "RecordingImpl.IStateWaitTuneSuccess";
        }
    }

    /**
     * Represents a recording for which the native recording operation has
     * completed (either as scheduled, or prematurely)
     */
    class IStateEnded extends IState
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateEnded: ";

        final int m_completionStateCode;

        IStateEnded(boolean userCancelled, boolean userStopped)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "constructing......");
            }

            if (m_storageListening) StorageManager.getInstance().removeStorageManagerListener(getRecordingInstance());
            m_storageListening = false;

            if (m_scheduledServiceContext != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "Removing self as service context listener...");
                }

                m_scheduledServiceContext.removeServiceContextCallback(getRecordingInstance());
                m_scheduledServiceContext = null;
            }

            // Set the external state of the recording
            m_completionStateCode = processInterruptionState(userCancelled, userStopped);

            // we're no longer interested in system buffering state changes
            m_recordingManager.removeDisableBufferingListener(RecordingImpl.this);

            // we're no longer interested in recording manager schedule state changes
            m_recordingManager.removeRecordingDisabledListener(RecordingImpl.this);

            // we're no longer interested in storage volume changes
            DVRStorageManager dsm = (DVRStorageManager) ManagerManager.getInstance(org.cablelabs.impl.manager.StorageManager.class);
            dsm.removeMediaStorageVolumeUpdateListener(RecordingImpl.this);

            releaseTSWClient();
            cleanupLightweightTriggerTimeTable();
        }

        /**
         * This recording has been set in the ended state. Check the current
         * physical recording state to determine the proper state transition. If
         * we're are or nearly completed (within our tolerance) indicate the
         * completed state. If we've not yet started our physical recording,
         * indicate the failed state. If we have a partial recording, indicate
         * incomplete.
         *
         * @param userCancelled
         *            denotes that user had cancelled the recording
         * @param userStopped
         *            TODO
         * @return the recommended state resulting from the interruption
         */
        int processInterruptionState(boolean userCancelled, boolean userStopped)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "processInterruptionState Enter...");
            }

            long actualDuration = RecordingImpl.this.getRecordedDuration();
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "processInterruption - actual duration of this recording: "
                        + actualDuration);
            }

            // We need to transition into a END state.
            // If the user has explicitly cancelled the recording, set the state
            // and exit out
            if (userCancelled)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "processInterruption - user cancelled");
                }
                return OcapRecordingRequest.CANCELLED_STATE;
            }

            if (userStopped)
            {
                int newState;

                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "Incomplete recording - due to user stop=");
                }

                if (actualDuration == 0)
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(m_istateLogPrefix
                                 + "processInterruption on user stop - no recorded content found. Transitioning to FAILED_STATE");
                    }

                    deleteRecordedServiceData(true);
                    newState = LeafRecordingRequest.FAILED_STATE;
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "processInterruption - native recording found: " + this
                                + "  -- transitioning to INCOMPLETE_STATE");
                    }
                    newState = LeafRecordingRequest.INCOMPLETE_STATE;
                }
                return newState;

            }
            // If we are nearly completed, this interruption may be a race
            // condition between our termination and another recordings
            // startup. Check our recorded duration - if within tolerance,
            // set state to "completed"

            // if we do not have a recording ID (because, say, we've begun tuning
            // but haven't started the physical recording) mark as "failed".
            // this indicates that no physical recording is available (and no
            // recorded service)
            if (actualDuration == 0)
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_istateLogPrefix
                             + "processInterruption on scheduled stop - no recorded content found. Transitioning to FAILED_STATE");
                }
                deleteRecordedServiceData(true);
                return LeafRecordingRequest.FAILED_STATE;
            }

            // If current time (System.currentTimeMillis())is less than the
            // m_info.getRequestedStartTime() + m_info.getRequestedDuration(),
            // then
            // the recording must transition to the external
            // IN_PROGRESS_INCOMPLETE_STATE and notify and
            // set the internal state to m_istate.setState(new
            // IStateSuspendedResourceLost) and register
            // for the NI available listener.
            if (actualDuration + RecordingImpl.m_recLengthTolerance < m_info.getRequestedDuration()
                    || (m_info.getFailedExceptionReason() != RFE.REASON_NOT_KNOWN))
            {
                // ECN 1321 - recording in the IN_PROGRESS_STATE with a duration
                // that does not
                // meet the expected duration do not tranistion to
                // INCOMPLETE_STATE
                if ((m_info.getState() == LeafRecordingRequest.IN_PROGRESS_STATE)
                        && (RecordingImpl.this.m_timeCreated > (RecordingImpl.this.m_info.getRequestedStartTime() + RecordingImpl.m_recLengthTolerance)))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(m_istateLogPrefix + "complete recording - event with unexpected duration:"
                                + actualDuration + " expected: " + m_info.getRequestedDuration());
                    }
                    // this recording was "almost" done - call it complete
                    setStateAndNotify(LeafRecordingRequest.COMPLETED_STATE);
                    return LeafRecordingRequest.COMPLETED_STATE;
                }

                if (log.isInfoEnabled())
                {
                    log.info(m_istateLogPrefix + "incomplete recording - duration:" + actualDuration + " expected: "
                            + m_info.getRequestedDuration());
                }

                return LeafRecordingRequest.INCOMPLETE_STATE;
            }

            // Only IN_PROGRESS_STATE recording can transition to
            // COMPLETED_STATE. All others
            // transition to IN_COMPLETE per specification.
            if (m_info.getState() == LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE
                    || m_info.getState() == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE)
            {
                if (log.isInfoEnabled())
                {
                    log.info(m_istateLogPrefix + "incomplete recording due to state - state == "
                            + externalStateToString(m_info.getState()));
                }

                return LeafRecordingRequest.INCOMPLETE_STATE;
            }

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "COMPLETE recording - duration:" + actualDuration + " expected: "
                        + m_info.getRequestedDuration() + " tolerance: " + RecordingImpl.m_recLengthTolerance);
            }
            // this recording was "almost" done - call it complete
            return LeafRecordingRequest.COMPLETED_STATE;
        }

        public int getCompletionStateCode()
        {
            return m_completionStateCode;
        }

        public String toString()
        {
            return "RecordingImpl.IStateEnded";
        }

    }

    public void tswStateChanged(TimeShiftWindowClient tswc, TimeShiftWindowStateChangedEvent ev)
    {
        synchronized (m_sync)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + " RecordingImpl: tswStateChanged " + ev.toString());
            }

            m_istate.handleTswStateChange(ev);
        }
    }

    public void tswCCIChanged(TimeShiftWindowClient tswc, CopyControlInfo ccie)
    {
        synchronized (m_sync)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + " RecordingImpl: tswCCIChanged " + ccie.toString());
            }

            m_istate.handleTswCCIChange(ccie);
        }
    } // END tswCCIChanged


    /**
     * Set the native handle associated with this recordingImpl called from
     * native by the recording start/tsb conversion JNI
     */
    void setNativeRecordingHandle(int nativeHandle)
    {
        this.m_nativeRecordingHandle = nativeHandle;
    }

    /**
     * Set the native handle associated with this recordingImpl called from
     * native by the recording start/tsb conversion JNI
     */
    void setNativeRecordingName(String name)
    {
        this.m_nativeRecordingName = name;
    }

    /**
     * Set the actual start time of recorded content when this recording was
     * initiated. Called from native by the recording tsb conversion JNI.
     *
     * Time is in ms (system time)
     */
    void setActualStartTime(long startTime)
    {
        this.m_nativeRecordingActualStartTime = startTime;
    }

    /**
     * Gets the actual start time of recorded content.
     *
     * @return actual start time in ms (system time)
     */
    public long getActualStartTime()
    {
        return this.m_nativeRecordingActualStartTime;
    }

    class ConversionListener implements EDListener
    {
        private boolean m_active;
        final String m_listenerLogPrefix = m_logPrefix + "ConversionListener 0x" + Integer.toHexString(this.hashCode()) + ": ";

        ConversionListener()
        {
            m_active = true;
        }

        public void deactivate()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            m_active = false;
        }

        public String toString()
        {
            return "ConversionListener 0x" + Integer.toHexString(this.hashCode());
        }

        public void asyncEvent(int eventCode, int eventData1, int eventData2)
        {
            synchronized (m_sync)
            {
                if (!m_active)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_listenerLogPrefix + "asyncEvent: Listener deactivated - ignoring event " + eventCode);
                    }
                    return;
                }

                if (log.isDebugEnabled())
                {
                    log.debug(m_listenerLogPrefix + "asyncEvent: processing event " + eventCode);
                }

                switch(eventCode)
                {
                    case RecordingImpl.MPE_DVR_EVT_CONVERSION_STOP:
                    {
                        m_istate.handleNativeConversionStopped();
                    }
                    break;
                    case RecordingImpl.MPE_DVR_EVT_OUT_OF_SPACE:
                    {
                        m_istate.handleNativeOutOfSpace(eventCode);
                    }
                    break;
                    case RecordingImpl.MPE_DVR_EVT_SESSION_CLOSED:
                    {
                        m_istate.handleNativeSessionClosed(eventCode);
                    }
                    break;
                    default:
                    {
                        if (log.isWarnEnabled())
                        {
                            log.warn(m_logPrefix + "asyncEvent: Received unknown eventCode: " + eventCode);
                        }
                    }
                    break;
                }
            } // END synchronized (m_sync)
        } // END asyncEvent()
    } // END class ConversionListener

    /**
     * The Started state represents a recording that is in progress. It's
     * primary responsibility is to monitor for end conditions from
     * ServiceContext, NetworkInterface, RecordingManager and native methods
     */
    class IStateStarted extends IState
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateStarted: ";

        IStateStarted()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing");
            }
            if (!m_storageListening) StorageManager.getInstance().addStorageManagerListener(getRecordingInstance());
            m_storageListening = true;

            if (m_scheduledServiceContext != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "ServiceContext recording... ");
                }
                // RecordingImpl implements ServiceContextCallback
                // What priority should be used??
                ((ServiceContextImpl) m_scheduledServiceContext).addServiceContextCallback(getRecordingInstance(), 20);
            }
        }

        void processTswEventBufShutdown(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "processTswEventBufShutdown");
            }

            int tswEventReason = ev.getReason();
            if (m_nativeRecordingHandle != 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "calling native stop " + this);
                }
                stopTimeShiftConversion(true);
            }

            // remove ourselves from the "in progress" list.
            RecordingRetentionManager.getInstance().removeInProgressRecording(RecordingImpl.this);

            // Check to determine completion state
            long currentTime = System.currentTimeMillis();
            long endTime = m_info.getRequestedStartTime() + m_info.getRequestedDuration();
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "recording still has a future end time: current time " + currentTime
                        + ", endTime: " + endTime);
            }

            switch (tswEventReason)
            {
                case TimeShiftManager.TSWREASON_PIDCHANGE:
                case TimeShiftManager.TSWREASON_SIZEINCREASE:
                case TimeShiftManager.TSWREASON_SIZEREDUCTION:
                {
                    // These are transitory conditions - they should not exhibit
                    // an external change to the recording
                    break;
                }
                case TimeShiftManager.TSWREASON_NOCOMPONENTS:
                case TimeShiftManager.TSWREASON_SYNCLOST:
                case TimeShiftManager.TSWREASON_SERVICEVANISHED:
                {
                    // These cause the recording to suspend for an unspecified
                    // time - go to IN_PROGRESS_WITH_ERROR
                    if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                    {
                        setFailedExceptionReason(RecordingFailedException.CONTENT_NOT_FOUND);
                    }
                    setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);

                    break;
                }
                case TimeShiftManager.TSWREASON_ACCESSWITHDRAWN:
                {
                    if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                    { // See rules of DVR 6.2.1.1.3.3
                        setFailedExceptionReason(RecordingFailedException.ACCESS_WITHDRAWN);
                    }
                    setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);

                    break;
                }
                default:
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(m_istateLogPrefix + "Buffering shutdown for unhandled reason: "
                                + TimeShiftManager.reasonString[tswEventReason]);
                    }

                    if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                    {
                        setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                    }
                    setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);

                    break;
                }
            } // END switch (tswEventReason)

            // Regardless of the reason, proceed with the shutdown
            m_istate.setState(new IStateSuspendedTSWBufShutdown());

            RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
        } // END processTswEventBufShutdown()

        void processTswEventIntShutdown(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "processTswEventIntShutdown");
            }
            int tswEventReason = ev.getReason();
            // If conversion is in progress, stop it
            if (m_nativeRecordingHandle != 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "calling native stop " + this);
                }
                stopTimeShiftConversion(true);
            }

            // remove ourselves from the "in progress" list.
            RecordingRetentionManager.getInstance().removeInProgressRecording(RecordingImpl.this);

            // Check to determine completion state
            long currentTime = System.currentTimeMillis();
            long endTime = m_info.getRequestedStartTime() + m_info.getRequestedDuration();

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "recording still has a future end time: current time " + currentTime
                        + " endTime: " + endTime + "\n");
            }
            if (tswEventReason == TimeShiftManager.TSWREASON_SERVICEREMAP)
            {
                // This event means that a retune is about to happen
                // as in Switched digital video case
                setFailedExceptionReason(RecordingFailedException.SERVICE_VANISHED);
                m_istate.setState(new IStateSuspendedServiceRemap());
            }
            else if (tswEventReason == TimeShiftManager.TSWREASON_SERVICEVANISHED)
            {
                setFailedExceptionReason(RecordingFailedException.SERVICE_VANISHED);
                setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                m_istate.setState(new IStateSuspendedServiceUnavailable());
            }
            else if (tswEventReason == TimeShiftManager.TSWREASON_INTLOST)
            {
                if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                {
                    setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                }
                setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                m_istate.setState(new IStateSuspendedTunerUnavailable());
            }
            else
            {
                // What if the recording was dServiceContext based?
                if (m_scheduledServiceContext == null)
                {
                    if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                    {
                        setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                    }
                    setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                    m_istate.setState(new IStateSuspendedTunerUnavailable());
                }
            }
            RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
        }

        void handleTswCCIChange(CopyControlInfo cci)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "handleTswCCIChange: Handling  " + cci);
            }

            { // Add the CCI event to the segment's CCI TimeTable
                final long cciSegmentTimeOffsetNs = ( cci.getTimeMillis()
                                                     - m_activeRecordedService.getRecordingStartTimeMs() )
                                                     * SequentialMediaTimeStrategy.MS_TO_NS;

                TimeTable segmentCCITT = m_activeRecordedService.m_segmentInfo.getCCITimeTable();

                final CopyControlInfo segmentCCI = new CopyControlInfo(cciSegmentTimeOffsetNs, cci.getCCI());
                segmentCCITT.addElement(segmentCCI);

                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "handleTswCCIChange: Adding " + segmentCCI + ", segment CCI " + segmentCCITT);
                }
            }

            switch (cci.getEMI())
            {
                case CopyControlInfo.EMI_COPY_FREELY:
                { // Nothing to do - carry on recording
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_istateLogPrefix + "handleTswCCIChange: CCI is COPY_FREELY - continuing recording...");
                    }
                    break;
                }
                case CopyControlInfo.EMI_COPY_ONCE:
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_istateLogPrefix + "handleTswCCIChange: CCI is COPY_ONCE. Marking current segment COPY_NO_MORE and continuing");
                    }
                    setCurrentSegmentCopyProtected();
                    break;
                }
                case CopyControlInfo.EMI_COPY_NEVER:
                case CopyControlInfo.EMI_COPY_NO_MORE:
                {
                    if (m_recordingManager.isContentHostBound())
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_istateLogPrefix + "handleTswCCIChange: CCI is COPY_NEVER/NO_MORE and host-bound content is supported - continuing");
                        }
                        setCurrentSegmentCopyProtected();
                    }
                    else
                    { // Have to stop recording
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_istateLogPrefix + "handleTswCCIChange: CCI is COPY_NEVER/NO_MORE and host-bound content is NOT supported - suspending recording");
                        }

                        // If conversion is in progress, stop it
                        if (m_nativeRecordingHandle != 0)
                        {
                            stopTimeShiftConversion(true);
                        }

                        // remove ourselves from the "in progress" list.
                        RecordingRetentionManager.getInstance().removeInProgressRecording(RecordingImpl.this);

                        if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                        { // See rules of DVR 6.2.1.1.3.3
                            setFailedExceptionReason(RecordingFailedException.ACCESS_WITHDRAWN);
                        }
                        setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                        m_istate.setState(new IStateSuspendedCopyProtected());
                        RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
                    }
                    break;
                }
            } // END switch (ccie.getEMI())
        } // END handleTswCCIChange()

        void handleMsvEvent(StorageManagerEvent sme)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "handleMsvEvent " + sme.toString());
            }

            StorageProxyImpl spi = (StorageProxyImpl) sme.getStorageProxy();
            int status = spi.getStatus();

            if (StorageManagerEvent.STORAGE_PROXY_REMOVED == sme.getEventType()
                    || (StorageManagerEvent.STORAGE_PROXY_CHANGED == sme.getEventType() && (status == StorageProxy.OFFLINE
                            || status == StorageProxy.NOT_PRESENT || status == StorageProxy.DEVICE_ERROR)))
            {
                // Need to verify if the this recording is affected
                if (sme instanceof DVRStorageManagerEvent)
                {
                    RecordingList rl = ((DVRStorageManagerEvent) sme).getEntries();

                    if (rl.contains(getRecordingInstance()))
                    {
                        // If not seen we need to to suspended state
                        // remove ourselves from the "in_progress" list
                        RecordingRetentionManager.getInstance().removeInProgressRecording(RecordingImpl.this);

                        setFailedExceptionReason(RecordingFailedException.RESOURCES_REMOVED);
                        setStateNoNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                        m_istate.setState(new IStateSuspendedMSVUnavailable());
                        RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
                    }
                }
            }
        }// End handleMsvEvent

        void handleStop(boolean userStop, boolean immediate)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "handleStop(" + userStop + ')');
            }

            // stop the native recording
            if (m_nativeRecordingHandle != 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "calling native stop " + this + " userStop: " + userStop);
                }
                // handle scheduled stop - false indicates that this is not
                // a user stop
                stopTimeShiftConversion(immediate);
            }

            // If 'immediate' flag is set to 'false', we will wait for native termination
            // event (MPE_DVR_EVT_CONVERSION_STOP) to set the recording state.

            // If 'immediate' flag is set to 'true', when the native termination call returns
            // we will consider the recording to be stopped and can go ahead and set the state
            // to IStateEnded and everything else below.
            if(immediate)
            {
                // remove ourselves from the "in_progress" list
                RecordingRetentionManager.getInstance().removeInProgressRecording(RecordingImpl.this);

                // transition to a completed state
                final IStateEnded endState = new IStateEnded(false, userStop);
                setStateAndNotify(endState.getCompletionStateCode());
                m_istate.setState(endState);

                if (m_info.getState() != LeafRecordingRequest.COMPLETED_STATE)
                {
                    if (true == userStop)
                    {
                        setFailedExceptionReason(RecordingFailedException.USER_STOP);
                    }
                    else
                    {
                        if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                        {
                            setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                        }
                    }
                    RecordingImpl.this.saveRecordingInfo(RecordingDBManager.FAILED_EXCEPTION_REASON);
                }
                RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
            }
        } // End handleStop

        /**
         * Process ServiceContext tune away
         *
         * @return the externally visible state upon completion
         */
        void handleSCTuneAway()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "handleSCTuneAway");
            }

            if (m_nativeRecordingHandle != 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "calling native stop ");
                }

                stopTimeShiftConversion(true);
            }

            super.handleSCTuneAway();
        }

        void handleNativeConversionStopped()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "handleNativeConversionStopped");
            }

            // Eliminate our reference to the native recording handle
            m_nativeRecordingHandle = 0;

            m_nativeConversionListener.deactivate();
            m_nativeConversionListener = null;

            IStateEnded endState = new IStateEnded(false, false);
            setStateAndNotify(endState.getCompletionStateCode());
            m_istate.setState(endState);

            // tell the RecordingManager that we're no longer in progress
            RecordingRetentionManager.getInstance().removeInProgressRecording(RecordingImpl.this);
            RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
        }

        /*
         * Native termination event handler.
         */
        void handleNativeSessionClosed(int eventCode)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "handleNativeSessionClosed");
            }

            int newState;

            // Eliminate our reference to the native recording handle
            m_nativeRecordingHandle = 0;

            if (m_nativeConversionListener != null)
            {
                m_nativeConversionListener.deactivate();
                m_nativeConversionListener = null;
            }

            // Only go to the IN_PROGRESS_WITH_ERROR_STATE if the recording
            // is not a serviceContext based recording and if the termination has
            // occured prior to the end time of the recording.
            long currentTime = System.currentTimeMillis();
            long endTime = m_info.getRequestedStartTime() + m_info.getRequestedDuration();
            if ((currentTime < endTime) && (m_scheduledServiceContext == null))
            {
                setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                m_istate.setState(new IStateSuspendedTunerUnavailable());
            }
            else
            {
                IStateEnded endState = new IStateEnded(false, false);
                setStateAndNotify(endState.getCompletionStateCode());
                m_istate.setState(endState);
            }

            // Now set the Failed exception based on the MPE native termination
            //  event.
            newState = m_info.getState();
            if (newState != LeafRecordingRequest.COMPLETED_STATE)
            {
                // update the recording failure exception
                if (eventCode == RecordingImpl.MPE_DVR_EVT_OUT_OF_SPACE)
                {
                    setFailedExceptionReason(RecordingFailedException.SPACE_FULL);
                }
                else if (eventCode == RecordingImpl.MPE_DVR_EVT_PLAYBACK_PID_CHANGE)
                {
                    setFailedExceptionReason(RecordingFailedException.SERVICE_VANISHED);
                }
                else if (eventCode == RecordingImpl.MPE_DVR_EVT_CONVERSION_STOP
                        || eventCode == RecordingImpl.MPE_DVR_EVT_SESSION_CLOSED)
                {
                    // TODO: should OCAP define additional failure codes?
                    if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                    {
                        setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                    }
                }
            }

            // Save the exception and the State
            saveRecordingInfo(RecordingDBManager.STATE | RecordingDBManager.FAILED_EXCEPTION_REASON);

            // tell the RecordingManager that we're no longer in progress
            RecordingRetentionManager.getInstance().removeInProgressRecording(RecordingImpl.this);
            RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
        } // End handleNativeTerminationNotification

        void handleBufferingStateChanged()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "handleBufferingStateChanged");
            }

            int failedExceptionReason = RFE.REASON_NOT_KNOWN;

            // verify that recording is enabled
            if (m_recordingManager.isBufferingEnabled() == false)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "Cannot record - buffering is not enabled");
                }
                failedExceptionReason = RecordingFailedException.INSUFFICIENT_RESOURCES;
            }
            if (m_recordingManager.isRecordingEnabled() == false)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "Cannot record - recording is not enabled");
                }
                failedExceptionReason = RecordingFailedException.INSUFFICIENT_RESOURCES;
            }
            if (hasMSVAccess() == false)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "Cannot record - insufficient rights for destination MSV");
                }
                failedExceptionReason = RecordingFailedException.RESOURCES_REMOVED;
            }

            if (failedExceptionReason != RFE.REASON_NOT_KNOWN)
            {
                // We no longer have access - tear down ongoing
                // recording and transition to disabled state
                if (m_nativeRecordingHandle != 0)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_istateLogPrefix + "calling native stop");
                    }

                    stopTimeShiftConversion(true);
                }

                // remove ourselves from the "in progress" list
                RecordingRetentionManager.getInstance().removeInProgressRecording(RecordingImpl.this);

                if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                {
                    setFailedExceptionReason(failedExceptionReason);
                }
                setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                m_istate.setState(new IStateSuspendedBufferingDisabled());
                RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);

                return;
            }
        }

        /**
         * Process SI change event
         *
         * @return
         */
        void handlePMTChange(SIChangeEvent changeEvent)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "handlePMTChange");
            }

            synchronized (m_sync)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + ": Processing PMT change event...");
                }

                TimeShiftBuffer tsb = m_tswClient.getBufferingTSB();

                // Note: There will always be at least 1
                GenericTimeAssociatedElement lastTTe = (GenericTimeAssociatedElement) tsb.getPidMapTimeTable()
                        .getLastEntry();

                PidMapTable curTSBPidMap = (PidMapTable) lastTTe.value;

                PidMapTable newRecPidMap = createRecPidMapFromTSBPidMap(curTSBPidMap, m_info.getServiceLocators());

                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + this.toString() + ": curPidMap: " + m_curPidMap);
                }
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + this.toString() + ": newRecPidMap: " + newRecPidMap);
                }

                //
                // Check for presence of sufficient components
                //

                //
                // Compare newRecPidMap to currently-recording components
                //
                if (recordingPidMapsEquivalent(newRecPidMap, m_curPidMap))
                {
                    if (log.isInfoEnabled())
                    {
                        log.info(m_istateLogPrefix + "PMT change doesn't require change in conversion - ignoring");
                    }
                    return;
                }

                // Assert: We need to change the buffering components

                // Attempt changing components being converted
                if (m_nativeRecordingHandle != 0)
                {
                    int native_error = 0;
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug(m_istateLogPrefix + "calling native nativeConversionChangeComponents... ");
                        }
                        // new components are already populated into pidTable

                        // attempt to change components on the ongoing
                        // conversion

                        native_error = nConversionChangeComponents(m_nativeRecordingHandle, newRecPidMap);
                        if (native_error == 0) // nativeConversionChangeComponents
                                               // worked!
                        {
                            m_curPidMap = newRecPidMap;
                            TimeAssociatedDetailsInfo newDetails = createDetailsFromPidMapTable(
                                    m_curPidMap,
                                    RecordingImpl.this.m_nativeRecordingActualStartTime
                                      * SequentialMediaTimeStrategy.MS_TO_NS );
                            m_info.getLastRecordedSegment().getTimeAssociatedDetails().addElement(newDetails);
                            return;
                        }
                    }

                    // nativeConversionChangeComponents failed!
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_istateLogPrefix + "calling native stop ");
                    }

                    // If change components failed, stop the conversion
                    stopTimeShiftConversion(true);

                    // remove ourselves from the "in progress" list.
                    RecordingRetentionManager.getInstance().removeInProgressRecording(RecordingImpl.this);
                    setFailedExceptionReason(RecordingFailedException.CONTENT_NOT_FOUND);
                    setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                    m_istate.setState(new IStateSuspendedTSWBufShutdown());
                    RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
                }
            } // synchronized (m_sync)

        } // END handlePMTChange()


        /**
         * {@inheritDoc}
         */
        public void handleNativeOutOfSpace(int eventCode)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "handleNativeOutOfSpace");
            }

            if (m_nativeRecordingHandle != 0)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "handleNativeOutOfSpace: calling native stop " + this);
                }
                stopTimeShiftConversion(true);
            }

             // remove ourselves from the "in progress" list.
            RecordingRetentionManager.getInstance().removeInProgressRecording(RecordingImpl.this);

            if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
            {
                setFailedExceptionReason(RecordingFailedException.SPACE_FULL);
            }
            setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);

            m_istate.setState(new IStateSuspendedInsufficientSpace());
            RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
        } // END handleNativeOutOfSpace()

        public String toString()
        {
            return "RecordingImpl.IStateStarted";
        }
    }// End IStateStarted class

    /**
     * This represents the base class for every suspended case. In a suspended
     * state, the recording is set to external state IN_PROGRESS_WITH_ERROR. By
     * default, the suspended state treats the TimeShiftWindow as if it is still
     * active. The suspended states will inherit the process
     * TimeShiftWindowChangedEvents listed within: Upon a TUNE_SYNC, the
     * recording will attempt to record. Upon a TUNE_UNSYNC, the recording will
     * be suspended in the SyncLost state Upon a TUNE_INTSHUTDOWN or NOT_TUNED,
     * the recording will be suspended TunerLost state if the interface has
     * become unavailable or fail if no content is found
     *
     */
    class IStateSuspended extends IState
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspended: ";

        // Required to conditionalize when end blocks of TimeShiftWindow events
        IStateSuspended()
        {

        }

        void processTswEventIntShutdown(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            int tswEventReason = ev.getReason();
            if (tswEventReason == TimeShiftManager.TSWREASON_INTLOST
                    || tswEventReason == TimeShiftManager.TSWREASON_NOFREEINT)
            {
                if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                {
                    setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                }
                setStateAndNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                m_istate.setState(new IStateSuspendedTunerUnavailable());
            }
            else
            {
                if (!(m_istate instanceof IStateEnded))
                {
                    m_istate.setState(new IStateEnded(false, false));
                    setFailedExceptionReason(RecordingFailedException.CONTENT_NOT_FOUND);
                    RecordingResourceManager.getInstance().resolveConflictsForRecordingRemoval(RecordingImpl.this);
                }
                setStateAndNotify(LeafRecordingRequest.FAILED_STATE);
            }
        }

        void processTswEventIdle(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            processTswEventIntShutdown(ev);
        }

        /*
         * handleStop() assumes that a time shift conversion has taken place so
         * it is just the mater of setting it to the ended state
         */
        void handleStop(boolean userStop, boolean immediate)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            // If we received a stop call while in the pending state, this means
            // that our find-TimeShiftWindow stage never completed, and our
            // physical recording failed.
            // We must then transition into the failed state
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "handleStop - Stop called");
            }

            // transition to a completed state
            final IStateEnded endState = new IStateEnded(false, userStop);
            setStateAndNotify(endState.getCompletionStateCode());
            m_istate.setState(endState);

            if (m_info.getState() != LeafRecordingRequest.COMPLETED_STATE)
            {
                if (true == userStop)
                {
                    setFailedExceptionReason(RecordingFailedException.USER_STOP);
                }
                else
                {
                    if (m_info.getFailedExceptionReason() == RFE.REASON_NOT_KNOWN)
                    {
                        setFailedExceptionReason(RecordingFailedException.INSUFFICIENT_RESOURCES);
                    }
                }
                RecordingImpl.this.saveRecordingInfo(RecordingDBManager.FAILED_EXCEPTION_REASON);
            }
        }

        public void detachFromTSW()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (m_tswClient != null)
            {
                m_tswClient.detachFor(m_tswClient.getUses());
            }
        }

        public String toString()
        {
            return "RecordingImpl.IStateSuspended";
        }
    }

    /**
     * Suspended state representing when the buffering is deactivated. On
     * signalling of a state change of the buffering, recording is attempted.
     * Here, the TimeShiftWindow is still alive but there is no ongoing
     * bufffering.
     *
     */
    class IStateSuspendedBufferingDisabled extends IStateSuspended
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedBufferingDisabled: ";

        IStateSuspendedBufferingDisabled()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }
            detachFromTSW();
        }

        void handleBufferingStateChanged()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (m_recordingManager.isBufferingEnabled())
            {
                final int tswState = m_tswClient.getState();
                switch (tswState)
                {
                    case TimeShiftManager.TSWSTATE_BUFFERING:
                    {
                        attachToTSW();
                        startRecording(m_info.getRequestedStartTime(), true);
                        return;
                    }
                    case TimeShiftManager.TSWSTATE_READY_TO_BUFFER:
                    case TimeShiftManager.TSWSTATE_BUFF_PENDING:
                    {
                        attachToTSW();
                        // Wait for the indication that buffering has started
                        //  (processTswEventBuffering())
                        break;
                    }
                    default:
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info( m_logPrefix + "Buffering is enabled but TSW cannot buffer (state "
                                      + TimeShiftManager.stateString[tswState] + ')');
                        }
                        return;
                    }
                } // END switch (tswState)
            }
        } // END handleBufferingStateChanged()

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedBufferingDisabled";
        }

        void processTswEventBuffering(TimeShiftWindowStateChangedEvent ev)
        {
            startRecording( System.currentTimeMillis(), false );
        }
    } // END class IStateSuspendedBufferingDisabled

    /**
     * Suspended state representing when recording is disabled system-wide. On
     * signalling of a state change of recording, recording is attempted.
     */
    class IStateSuspendedRecordingDisabled extends IStateSuspended
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedRecordingDisabled: ";

        IStateSuspendedRecordingDisabled()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }
            releaseTSWClient();
        }

        void handleStop(boolean userStop, boolean immediate)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            super.handleStop(userStop, immediate);
        }
        void handleRecordingEnabledStateChanged()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (m_recordingManager.isRecordingEnabled())
            {
                // initiate acquisition of TSW resources and recording process
                initiateRecordingProcess(true);

                m_recordingManager.removeRecordingDisabledListener(RecordingImpl.this);
            }
        }

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedRecordingDisabled";
        }
    } // END class IStateSuspendedRecordingDisabled

    /**
     * Suspended state representing the tuner has become disassociated with the
     * recording At this time, there is no TimeShiftWindow since there is no
     * tuner, so no events will be acted upon. Upon being signalled that a tuner
     * resource is avaliable, we will reinitiate recording.
     *
     */
    class IStateSuspendedTunerUnavailable extends IStateSuspended implements ResourceOfferListener
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedTunerUnavailable: ";

        IStateSuspendedTunerUnavailable()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }
            releaseTSWClient();
            nim = (NetworkInterfaceManagerImpl) NetworkInterfaceManagerImpl.getInstance();
            nim.addResourceOfferListener(this, 3);
        }

        void handleTswStateChange(TimeShiftWindowStateChangedEvent ev)
        {
        }

        void handleStop(boolean userStop, boolean immediate)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            nim.removeResourceOfferListener(this);

            super.handleStop(userStop, immediate);
        }

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedTunerUnavailable";
        }

        public boolean offerResource(Object resource)
        {
            synchronized (m_sync)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_istateLogPrefix + "offerResource called...");
                }
                // The resource that is being offered will always be a Network
                // Resource
                // Since we will be unconditionally transitioning from this state
                // to another, we should remove ourselves as a ResourceOfferListener
                nim.removeResourceOfferListener(this);

                // initiate acquisition of TSW resources and recording process
                initiateRecordingProcess(true);
                return true;
            }
        }

        public void statusChanged(ResourceStatusEvent event)
        {
            // Do nothing here
            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "statusChanged called");
            }
        }

        protected NetworkInterfaceManagerImpl nim;
    }

    /**
     * Suspended state repreenting when the storage volume has disappeared. Per
     * spec, resources should be held onto including
     *
     *
     */

    public class IStateSuspendedMSVUnavailable extends IStateSuspended
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedMSVUnavailable: ";

        IStateSuspendedMSVUnavailable()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }
            if (!m_storageListening) StorageManager.getInstance().addStorageManagerListener(getRecordingInstance());
            m_storageListening = true;
        }

        void handleMsvEvent(StorageManagerEvent sme)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + " handleMsvEvent() called...");
            }

            // is the event associated with the msv of this recording
            // what event is it? If the associated device has been added or
            // changed,
            // the MSV may be there.
            StorageProxyImpl spi = (StorageProxyImpl) sme.getStorageProxy();

            if ((StorageManagerEvent.STORAGE_PROXY_CHANGED == sme.getEventType())
                    && (spi.getStatus() == StorageProxy.READY))
            {
                // Need to verify if the this recording is affected
                if (sme instanceof DVRStorageManagerEvent)
                {
                    RecordingList rl = ((DVRStorageManagerEvent) sme).getEntries();

                    if (rl.contains(getRecordingInstance()))
                    {
                        // initiate acquisition of TSW resources and recording
                        // process
                        attachToTSW();
                    }
                }
            }
        }

        void processTswEventNotReadyToBuffer(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            detachFromTSW();
        }

        void processTswEventBuffering(TimeShiftWindowStateChangedEvent ev)
        {
            startRecording(System.currentTimeMillis(), false);
        }

        void processTswEventBufShutdown()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            detachFromTSW();
        }

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedMSVUnavaliable";
        }
    }

    /**
     * IStateSuspendedTSWBufShutdown state handles all the cases when a
     * TimeShiftWindow transitions to a BUF_SHUTDOWN state as a result of a pid
     * change, TSB resize etc. May be signaled that the TSW is Ready or
     * Not Ready to buffer.
     */
    class IStateSuspendedTSWBufShutdown extends IStateSuspended
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedTSWBufShutdown: ";

        IStateSuspendedTSWBufShutdown()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }
            detachFromTSW();
        }

        void processTswEventReadyToBuffer(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            attachToTSW();
        }

        void processTswEventBuffering(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            startRecording(System.currentTimeMillis(), true );
        }

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedTSWBufShutdown";
        }
    }

    /**
     * This is internal state that a recording transitions to when a service
     * remap occurs as in Switch Digital Video case. In this case a service on
     * one transport stream may move to a different transport stream. Stack
     * internally causes a retune to the new service params.
     */
    class IStateSuspendedServiceRemap extends IStateSuspended
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedServiceRemap: ";

        IStateSuspendedServiceRemap()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }
            detachFromTSW();
        }

        void processTswEventReadyToBuffer(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            attachToTSW();
        }

        void processTswEventBuffering(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            // attach for buffering+recording
            startRecording(System.currentTimeMillis(), true);
        }

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedServiceRemap";
        }
    }

    /**
     * This is internal state that a recording transitions to when a service
     * becomes unavailable...
     */
    class IStateSuspendedServiceUnavailable extends IStateSuspended
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedServiceUnavailable: ";

        IStateSuspendedServiceUnavailable()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }
            releaseTSWClient();
        }

        void handleTswStateChange(TimeShiftWindowStateChangedEvent ev)
        {
        }

        boolean handleServiceAvailable()
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            // initiate acquisition of TSW resources and recording process
            initiateRecordingProcess(true);

            return true;
        }

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedServiceUnavailable";
        }
    }

    /**
     * This is internal state that a recording transitions to when a tune has
     * failed...
     */
    class IStateSuspendedTuneFailed extends IStateSuspended
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedTuneFailed: ";

        IStateSuspendedTuneFailed()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "constructing..");
            }
            releaseTSWClient();
        }

        void handleTswStateChange(TimeShiftWindowStateChangedEvent ev)
        {
        }

        void processTswEventReadyToBuffer(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            attachToTSW();
        }

        void processTswEventBuffering(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            startRecording(System.currentTimeMillis(), true);
        }

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedTuneFailed";
        }
    }

    class IStateSuspendedTunerNotReady extends IStateSuspended
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedTunerNotReady: ";

        IStateSuspendedTunerNotReady()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }
            detachFromTSW();
        }

        void processTswEventReadyToBuffer(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            attachToTSW();
        }

        void processTswEventBuffering(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            startRecording(System.currentTimeMillis(), true);
        }

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedTunerNotReady";
        }
    }

    class IStateSuspendedCaDenied extends IStateSuspended
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedCaDenied: ";

        IStateSuspendedCaDenied()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }
            detachFromTSW();
        }

        void processTswEventReadyToBuffer(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            attachToTSW();
        }

        void processTswEventBuffering(TimeShiftWindowStateChangedEvent ev)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            startRecording(System.currentTimeMillis(), true);
        }

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedCaDenied";
        }
    } // END class IStateSuspendedCaDenied

    class IStateSuspendedCopyProtected extends IStateSuspended
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedCopyProtected: ";

        IStateSuspendedCopyProtected()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }
        }

        void handleTswCCIChange(CopyControlInfo cci)
        {
            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            switch (cci.getEMI())
            {
                case CopyControlInfo.EMI_COPY_FREELY:
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_istateLogPrefix + "handleTswCCIChange: Recording now allowed - attempting to resume recording...");
                    }
                    // Note: this will change our state
                    startRecording(System.currentTimeMillis(), true);
                    break;
                }
                case CopyControlInfo.EMI_COPY_ONCE:
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_istateLogPrefix + "handleTswCCIChange: Marking current segment COPY_NO_MORE and continuing");
                    }
                    // Note: this will change our state
                    startRecording(System.currentTimeMillis(), true);
                    setCurrentSegmentCopyProtected();
                    break;
                }
                case CopyControlInfo.EMI_COPY_NEVER:
                case CopyControlInfo.EMI_COPY_NO_MORE:
                { // Have to stop recording
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_istateLogPrefix + "handleTswCCIChange: Recording already suspended - ignoring");
                    }
                    break;
                }
            } // END switch (ccie.getEMI())
        } // END handleTswCCIChange()

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedCopyProtected";
        }
    } // END class IStateSuspendedCopyProtected

    class IStateSuspendedInsufficientSpace extends IStateSuspended
    {
        final String m_istateLogPrefix = m_logPrefix + "IStateSuspendedInsufficientSpace: ";

        IStateSuspendedInsufficientSpace()
        {
            super();

            // Internal method - caller should hold the lock
            if (ASSERTING) Assert.lockHeld(m_sync);

            if (log.isInfoEnabled())
            {
                log.info(m_istateLogPrefix + "constructing...");
            }

            final MediaStorageVolume msv = getVolume();
            if (msv != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug( m_istateLogPrefix + "constructor: "
                               + msv + " reporting " + msv.getFreeSpace()
                               + " bytes available" );
                }
            }

            detachFromTSW();
        } // END IStateSuspendedInsufficientSpace()

        /**
         * {@inheritDoc}
         */
        public void handleSpaceAvailable()
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_istateLogPrefix + "handleSpaceAvailable: Attempting to resume recording...");
            }

            final MediaStorageVolume msv = getVolume();

            if (log.isDebugEnabled())
            {
                log.debug( m_istateLogPrefix + "handleSpaceAvailable: "
                           + msv + " reporting " + msv.getFreeSpace()
                           + " bytes available - resuming recording" );
            }
            startRecording(System.currentTimeMillis(), true);
        }

        public String toString()
        {
            return "RecordingImpl.IStateSuspendedInsufficientSpace";
        }
    } // END class IStateSuspendedInsufficientSpace

    /**
     * Returns the RecordingInfo object that contains the metadata about this
     * recording. Callers not holding the lock should not expect sane results
     * if retrieving more than one field from the RecordingInfo since the
     * info may change while being read.
     */
    public RecordingInfo2 getRecordingInfo()
    {
        return m_info;
    }

    /**
     * Returns the base RecordingInfoNode that contains metadata about this
     * recording
     */
    RecordingInfoNode getRecordingInfoNode()
    {
        return m_info;
    }

    /**
     * Creates a RecordingResourceUsage to be used for the reservation.
     */
    ResourceUsageImpl createResourceUsage()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        // Priority is handled incorrectly. The right way of doing is to persist
        // the app priority
        // and use it here. For now the app priority is not persisted. This will
        // be taken care in a separate CR.

        return new RecordingResourceUsageImpl(this.getAppID(), this.getPriority(), this);
    }

    /*
     * Determines if the actual start time is outside of the start time
     * tolerance
     */
    public boolean verifyStartTimeWithinTolerance()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        // If the actual start time is not less than the requested time plus the
        // tolerance,
        // then the recording did not start on time.
        // ECN 1321 timeCreated addition:
        // "When a recording request is created with a start time in the past
        // and a duration
        // completion in the future but none or only some of the requested
        // service is
        // buffered the implementation SHALL transition the recording to the
        // IN_PROGRESS_STATE.
        // Missing some portion of a recording at the beiginning due to instant
        // record after
        // the start of a program is not considered an error and the recording
        // can complete
        // successfully. When the duration completes and if the state is still
        // IN_PROGRESS_STATE
        // the implementation SHALL transition the recording to the
        // COMPLETED_STATE."

        if (((m_info.getRequestedStartTime() + (m_recLengthTolerance)) < this.m_nativeRecordingActualStartTime)
                && m_info.getRequestedStartTime() > this.m_timeCreated)
        {
            return false;
        }
        return true;
    }

    void releaseTSWClient()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "releaseTSWClient Called");
        }

        if (m_tswClient != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "Calling TimeshiftClient.release()");
            }

            // release the timeshiftwindow client
            m_tswClient.release();
            m_tswClient = null;
        }
    }

    /**
     * This method is called when the recording is terminated and will not be
     * started up again. The method will cleanup an events or other information
     * stored in the TimeTables found inside the recording impl.
     */
    void cleanupLightweightTriggerTimeTable()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (m_info.getNumSegments() == 0)
        {
            // nothing to clean up, return;
            return;
        }

        if (log.isInfoEnabled())
        {
            log.info(m_logPrefix + "cleanupTimeTableCollection");
        }
        LightweightTriggerEventTimeTable lastTT = m_info.getLastRecordedSegment().getLightweightTriggerEventTimeTable();

        /*
         * get rid of any future events found in the last segment (where future
         * events are always stored). This can be done now because the duration
         * of the recording is known.
         */
        String segName = m_info.getLastRecordedSegment().getNativeRecordingName();
        long durMS = nGetRecordedDurationMS(segName);

        TimeTable futureEvents = SequentialMediaTimeStrategy.getFutureEvents(durMS,
                lastTT.getLightWeightTriggerEvents());
        lastTT.getLightWeightTriggerEvents().removeSubtable(futureEvents);
    }

    /**
     * Retrieve the ResourceUsage associated with the recording
     *
     * @return The RecordingResourceUsage for the recording
     */
    public org.ocap.dvr.RecordingResourceUsage getResourceUsage()
    {
        return (RecordingResourceUsage) m_resourceUsage.getResourceUsage();
    }

    // Description copied from DisableBufferingListener
    public void notifyBufferingDisabledStateChange(boolean enabled)
    {
        synchronized (m_sync)
        {
            m_istate.handleBufferingStateChanged();
        }
    }

    public void notifyRecordingEnabledStateChange(boolean enabled)
    {
        synchronized (m_sync)
        {
            m_istate.handleRecordingEnabledStateChanged();
        }
    }

    // Description copied from MediaStorageVolumeUpdateListener public void
    // notifyVolumeAccessStateChanged(MediaStorageVolume msv)
    public void notifyVolumeAccessStateChanged(MediaStorageVolume msv)
    {
        if (log.isDebugEnabled())
        {
            log.debug(" notifyVolumeAccessStateChanged (" + msv + ") for Recording:" + this);
        }

        // handle notification of destination change
        synchronized (m_sync)
        {
            m_istate.handleBufferingStateChanged();

            // If this recordings MSV is disabled, terminate ongoing presentations
            // for any internal state
            if (msv == getDestination())
            {
                MediaStorageVolumeExt msve = (MediaStorageVolumeExt) msv;
                if (!msve.hasAccess(m_info.getAppId()))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(" Terminating ongoing presentations for Recording:" + this);
                    }
                    terminateOngoingPresentations();
                }
            }
        } // END synchronized (m_sync)
    }

    /**
     * {@inheritDoc}
     */
    public void notifySpaceAvailable(MediaStorageVolume msv)
    {
        // handle notification of space change
        synchronized (m_sync)
        {
            if (msv.equals(getVolume()))
            {
                m_istate.handleSpaceAvailable();
            }
        } // END synchronized (m_sync)
    }

    // This method is for bootup where the stack will not signal that there is a
    // resource to offer
    // Instead we just blindly assume and trigger off that the resources are
    // there waiting
    // for us
    public void notifyResourceAvailable(NetworkInterface ni)
    {
        synchronized (m_sync)
        {
            if (m_istate instanceof ResourceOfferListener)
            {
                // We are passing aling the NI that is unreserved at this point
                ((ResourceOfferListener) m_istate).offerResource(ni);
            }
        }
    }

    /**
     * Static class initializer.
     */
    static
    {
        OcapMain.loadLibrary();

        nInit();
    }

    /**
     * Native method definitions
     */
    /** Initialize the JNI code */
    private static native void nInit();

    static native void nDeleteRecording(String nativeRecordingHandle);

    public native long nGetRecordedDurationMS(String recordingName);

    public native long nGetRecordedSize(String recordingName);

    // native int nConvertTimeShiftToRecording(int tsbNativeHandle, long
    // startTime, long duration, long bitrate,
    // int[] pids, short[] types);

    native int nConvertTimeShiftToRecording(int tsbNativeHandle, EDListener edListener,
            long startTime, long duration, long bitrate,
            TimeTable timeTable, int volumeNativeHandle);

    native int nConversionChangeComponents(int tsbNativeHandle, PidMapTable pidTable);

    native int nStopTimeShiftConversion(int tsbNativeHandle, boolean immediate);

    /**
     * Time tolerance when checking actual duration against recorded duration
     * (in milliseconds)
     */
    private static final int m_recLengthTolerance = RecordingManagerImpl.m_recLengthTolerance;

    /**
     * Internal permanently stored recording structure
     */
    public RecordingInfo2 m_info;

    ParentNodeImpl m_parent = null;

    RecordingRequest m_root = null;

    boolean m_isRoot = false;

    /**
     * The recording specification used to instantiate this RecordingImpl
     */
    private RecordingSpec m_recordingSpec;

    /**
     * The RecordedService representation of this recording
     */
    protected SegmentedRecordedServiceImpl m_segmentedRecordedService = null;

    /**
     * stores the pending externally visible recording state
     */
    private int m_savedState = 0;

    /**
     * stores the internal state
     */
    private IState m_istate;

    /**
     * Actively-recording RecordedService
     */
    private RecordedServiceImpl m_activeRecordedService;

    /**
     * Native handle to the active recording
     */
    private int m_nativeRecordingHandle = 0;

    private String m_nativeRecordingName = null;

    private ConversionListener m_nativeConversionListener;

    // system time
    private long m_nativeRecordingActualStartTime = 0;

    /**
     * Native tsb buffering handle
     */
    private int m_nativeTSBBufferHandle = 0;

    /**
     * reference to the ServiceContext to be used for a scheduled recording
     */
    private ServiceContextExt m_scheduledServiceContext;

    /**
     * reference to the timeshiftwindow client used for 'converting' to
     * permanent recording
     */
    private TimeShiftWindowClient m_tswClient;

    static private TimeShiftManager m_tsMgr = null;

    /**
     * Indicates whether we have expired and are awaiting a purge (delete)
     * notification from the retention manager
     */
    private boolean m_purgable = false;

    /**
     * Implement the Comparable Interface. This function compares two
     * RecordingImpl objects, and orders them based on their priorities and
     * recording start dates. This is expected to be used ONLY from the
     * RecordingRetentionManager.
     *
     */
    public int compareTo(Object x)
    {
        RecordingImpl target = (RecordingImpl) x;
        // Get the priorities of the two objects.
        int myPriority = this.getRetentionPriority();
        int yourPriority = target.getRetentionPriority();
        // If the priorities are the same,
        if (myPriority == yourPriority)
        {
            long myStartTime = getRequestedStartTime();
            long yourStartTime = target.getRequestedStartTime();
            if (myStartTime > yourStartTime)
            {
                return 1;
            }
            else if (myStartTime < yourStartTime)
            {
                return -1;
            }
            else
            {
                return 0;
            }
        }
        if (myPriority > yourPriority)
        {
            return 1;
        }
        return -1;
    }

    /**
     * Clears space to start this recording
     */
    void clearDiskSpace()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        long spaceNeeded = getSpaceRequired() - getVolume().getFreeSpace();

        if (spaceNeeded > 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "clearDiskSpace: Attempting to free " + spaceNeeded + " bytes");
            }
            RecordingRetentionManager.getInstance().freeDiskSpace(getVolume(), spaceNeeded);
        }
    }

    /**
     * Find the MediaStorageVolume that a recording is on. Get it from the
     * recording
     *
     * @return The MediaStorageVolume this recording is on.
     */
    public MediaStorageVolume getVolume()
    {
        synchronized (m_sync)
        {
            return getRecordingInfo().getDestination();
        }
    }

    /**
     * Get a new TimeShiftWindowClient from the TimeShiftWindow associated with
     * the RecordingRequest (if any). Active RecordingRequests (those in the
     * IN_PROGRESS or IN_PROGRESS_INCOMPLETE states) should generally have a
     * TimeShiftWindowClient.
     *
     * @return A TimeShiftWindowClient on the RecordingImpl's TimeShiftWindow or
     *         null if the RecordingImpl is not actively recording
     * @param reserveFor
     *            requested reservation
     */
    public TimeShiftWindowClient getNewTSWClient( final int reserveFor,
                                                  final TimeShiftWindowChangedListener tswcl )
    {
        synchronized (m_sync)
        {
            if (m_tswClient != null)
            {
                return m_tswClient.addClient(reserveFor, tswcl, TimeShiftManager.LISTENER_PRIORITY_DEFAULT);
            }
            else
            {
                return null;
            }
        }
    }

    /**
     * Checks to determine if the RecordingRequest has read/write access to the
     * destination MSV.
     *
     * @return true if access is granted
     */
    private boolean hasMSVAccess()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        MediaStorageVolumeExt msve = getDestination();
        if (msve.hasAccess(m_info.getAppId()) == false)
        {
            return false;
        }

        return true;
    } // END hasMSVAccess()

    /**
     * Stolen from MediaStorageVolumeImpl.java. This is how the permitted ORGIDs
     * are formatted, so we need to format the same way, for the string
     * comparison to come later.
     *
     */
    private String encodeOrgIdAsHexString(int value)
    {
        String hexaValue = Integer.toString(value, HEX_BASE);
        if (hexaValue.length() < MAXIMUM_ORGID_NAME_LENGTH)
        {
            int appendZeroCount = MAXIMUM_ORGID_NAME_LENGTH - hexaValue.length();
            for (int i = 0; i < appendZeroCount; i++)
            {
                hexaValue = "0" + hexaValue;
            }
        }
        return hexaValue;
    }

    /**
     * Translates any native recording failure codes to exception codes
     *
     * @param nativeErr
     * @return the RecordingFailedException code
     */
    private int nativeErrorToFailureReason(int nativeErr)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        int reason = 0;
        switch (nativeErr)
        {
            case MPE_DVR_ERR_OUT_OF_SPACE:
                reason = RecordingFailedException.SPACE_FULL;
                break;

            case MPE_DVR_ERR_INVALID_PID:
                reason = RecordingFailedException.CONTENT_NOT_FOUND;
                break;

            case MPE_DVR_ERR_NOT_ALLOWED:
            case MPE_DVR_ERR_DEVICE_ERR:
            case MPE_DVR_ERR_INVALID_PARAM:
            case MPE_DVR_ERR_OS_FAILURE:
            case MPE_DVR_ERR_UNSUPPORTED:
                reason = RecordingFailedException.INSUFFICIENT_RESOURCES;
                break;
            default:
                SystemEventUtil.logRecoverableError(new Exception("Unexpected native error code: " + nativeErr));
                reason = RecordingFailedException.INSUFFICIENT_RESOURCES;
                break;
        }
        return reason;
    }

    /*
     * Return the external state - for debugging.
     */
    public String externalStateToString(int state)
    {
        switch (state)
        {
            case LeafRecordingRequest.COMPLETED_STATE:
                return "COMPLETED_STATE";
            case LeafRecordingRequest.DELETED_STATE:
                return "DELETED_STATE";
            case LeafRecordingRequest.FAILED_STATE:
                return "FAILED_STATE";
            case LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE:
                return "IN_PROGRESS_INCOMPLETE_STATE";
            case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                return "IN_PROGRESS_INSUFFICIENT_SPACE_STATE";
            case LeafRecordingRequest.IN_PROGRESS_STATE:
                return "IN_PROGRESS_STATE";
            case LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE:
                return "IN_PROGRESS_WITH_ERROR_STATE";
            case LeafRecordingRequest.INCOMPLETE_STATE:
                return "INCOMPLETE_STATE";
            case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                return "PENDING_NO_CONFLICT_STATE";
            case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                return "PENDING_WITH_CONFLICT_STATE";
            case OcapRecordingRequest.CANCELLED_STATE:
                return "CANCELLED_STATE";
            case RecordingRequestImpl.DESTROYED_STATE:
                return "DESTROYED_STATE";
            case RecordingRequestImpl.INIT_STATE:
                return "INIT_STATE";
            default:
                return "UNKNOWN_STATE: (" + state + ")";
        }
    }

    /*
     * Return the reason code as a string - for debugging.
     */
    public String failureReasonToString(int reason)
    {
        switch (reason)
        {
            case RecordingFailedException.CA_REFUSAL:
                return "CA_REFUSAL";
            case RecordingFailedException.CONTENT_NOT_FOUND:
                return "CONTENT_NOT_FOUND";
            case RecordingFailedException.TUNING_FAILURE:
                return "TUNING_FAILURE";
            case RecordingFailedException.INSUFFICIENT_RESOURCES:
                return "INSUFFICIENT_RESOURCES";
            case RecordingFailedException.ACCESS_WITHDRAWN:
                return "ACCESS_WITHDRAWN";
            case RecordingFailedException.RESOURCES_REMOVED:
                return "RESOURCES_REMOVED";
            case RecordingFailedException.SERVICE_VANISHED:
                return "SERVICE_VANISHED";
            case RecordingFailedException.TUNED_AWAY:
                return "TUNED_AWAY";
            case RecordingFailedException.USER_STOP:
                return "USER_STOP";
            case RecordingFailedException.SPACE_FULL:
                return "SPACE_FULL";
            case RecordingFailedException.OUT_OF_BANDWIDTH:
                return "OUT_OF_BANDWIDTH";
            case RecordingFailedException.RESOLUTION_ERROR:
                return "RESOLUTION_ERROR";
            case RecordingFailedException.POWER_INTERRUPTION:
                return "POWER_INTERRUPTION";
            case 14: // RecordingFailedException.REASON_NOT_KNOWN: - filed OCSPEC-
                return "REASON_NOT_KNOWN";
            default:
                return "*INVALID REASON CODE*";
        }
    } // END failureReasonToString()

    /*
     * Code used to deal with conversion from media time from beginning of
     * segment (what is stored in lwte) to media time from beginning of
     * recording
     */
    private transient int m_lastSegmentCached = -1;

    private transient Vector m_lwteCache = null;

    private transient HashMap m_srcToPendingEvents = null;

    private void addDuration(RecordedSegmentInfo info, TimeTable tt)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        String segName = info.getNativeRecordingName();
        long segDur = nGetRecordedDurationMS(segName);
        // TODO: This is modifying the RecordingInfo's time association, which seems wrong
        //       The LightWeightTriggerEventCache needs to be checked...
        info.setTimeMillis(segDur);
        tt.addElement(info);
    }

    private void setMediaTimeAndEventsCache(RecordedSegmentInfo info)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "setMediaTimeAndEventsCache (start): lastSegCached=" + m_lastSegmentCached
                    + ", info.hashCode=" + (info == null ? 0 : info.hashCode()));
        }
        if (m_lastSegmentCached != (info == null ? 0 : info.hashCode()) || m_lwteCache == null)
        {
            TimeTable durationAndSegmentInfoTt = new TimeTable();

            if (info == null)
            {
                // get durations for the segments and associate this with the
                // events in the segment
                Enumeration segInfoEnum = m_info.getRecordedSegmentInfoElements();
                while (segInfoEnum.hasMoreElements())
                {
                    RecordedSegmentInfo segInfo = (RecordedSegmentInfo) segInfoEnum.nextElement();
                    if (log.isDebugEnabled())
                    {
                        log.debug(m_logPrefix + "setMediaTimeAndEventsCache lastSegCached being set, segInfo="
                                + segInfo);
                    }
                    addDuration(segInfo, durationAndSegmentInfoTt);
                }
            }
            else
            {
                addDuration(info, durationAndSegmentInfoTt);
            }
            // now have timetable with segment duration associated with that
            // segment's events:
            // long duration (seconds) associated with RecordedSegmentInfo,
            // which contains the events.
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "setMediaTimeAndEventsCache lastSegCached being set, durationAndSegmentInfoTt="
                        + durationAndSegmentInfoTt);
            }
            m_lwteCache = SequentialMediaTimeStrategy.getMediaTimeAndEventsFromSegments(durationAndSegmentInfoTt);
            m_lastSegmentCached = (info == null ? 0 : info.hashCode());
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "setMediaTimeAndEventsCache lastSegCached set");
            }
        }
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "setMediaTimeAndEventsCache (end): lastSegCached=" + m_lastSegmentCached
                    + ", cache size=" + m_lwteCache.size());
        }
    }

    private void moveFutureEvents()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        // need two or more tsbs for future events
        if (m_info.getNumSegments() < 2)
        {
            return;
        }

        // future events are always in the next to last segment
        RecordedSegmentInfo nextToLastSegInfo = (RecordedSegmentInfo) m_info.getNextToLastRecordedSegment();
        RecordedSegmentInfo lastSegInfo = (RecordedSegmentInfo) m_info.getLastRecordedSegment();

        // work through the list of events in the nextToLastSegInfo and move
        // any future events
        // event time is media time from beginning of segment so if the
        // media time is past the end of
        // the segment, it needs to move
        String segName = nextToLastSegInfo.getNativeRecordingName();
        long nextToLastDurMS = nGetRecordedDurationMS(segName);

        LightweightTriggerEventTimeTable nextToLastTTC = nextToLastSegInfo.getLightweightTriggerEventTimeTable();
        LightweightTriggerEventTimeTable lastTTC = lastSegInfo.getLightweightTriggerEventTimeTable();
        // this code moves future events from next to last TTC to last TTC
        // in chain
        // of segments.
        lastTTC.moveFutureLwte(nextToLastDurMS, nextToLastTTC);
    }

    public boolean cacheLightweightTriggerEvent(Object src, LightweightTriggerEvent lwte)
    {
        synchronized (m_sync)
        {
            if (m_srcToPendingEvents == null)
            {
                m_srcToPendingEvents = new HashMap(); // m_srcToPendingEvents is
                                                      // transient so create it
                                                      // on the fly
            }
            else if (checkPending(lwte)) return false;

            if (checkStored(lwte, null)) return false;

            Vector eventV = (Vector) m_srcToPendingEvents.get(src);
            if (eventV != null)
            {
                eventV.addElement(lwte);
            }
            else
            {
                eventV = new Vector();
                eventV.addElement(lwte);
                m_srcToPendingEvents.put(src, eventV);
            }

            return true;
        }
    }

    public void store(Object src)
    {
        synchronized (m_sync)
        {
            if (m_srcToPendingEvents != null)
            {
                Vector eventV = (Vector) m_srcToPendingEvents.get(src);
                m_srcToPendingEvents.put(src, null);
                if (eventV != null)
                {
                    addLightweightTriggerEvents(eventV);
                }
            }
        }
    }

    public boolean addLightweightTriggerEvent(LightweightTriggerEvent lwte)
    {
        synchronized (m_sync)
        {
            if (checkStored(lwte, null)) return false;
            if (checkPending(lwte)) return false;
            m_info.addLightweightTriggerEvent(lwte);
            // ignore the rare case where the event is not in the recording
            m_lastSegmentCached = -1;
            m_lwteChangeHelper.notifyListeners();
            return true;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @seeorg.cablelabs.impl.manager.lightweighttrigger.
     * LightweightTriggerEventStoreWrite
     * #addLightweightTriggerEvents(org.cablelabs.impl.util.TimeTable)
     */
    public void addLightweightTriggerEvents(Vector v)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        m_info.addLightweightTriggerEvents(v);
        // ignore the rare case where the event is not in the recording
        m_lastSegmentCached = -1;
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "addLightweightTriggerEvents: lastSegCached=" + m_lastSegmentCached);
        }

        m_lwteChangeHelper.notifyListeners();
    }

    public LightweightTriggerEvent getEventByName(RecordedSegmentInfo info, String name)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "getEventName, info=" + info + ", name=" + name);
        }

        setMediaTimeAndEventsCache(info);

        Enumeration lwteEnum = m_lwteCache.elements();
        while (lwteEnum.hasMoreElements())
        {
            LightweightTriggerEvent lwte = (LightweightTriggerEvent) lwteEnum.nextElement();
            if (name.equals(lwte.eventName))
            {
                return lwte;
            }
        }
        return null;
    }

    public String[] getEventNames(RecordedSegmentInfo info)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "getEventNames, info=" + info);
        }

        setMediaTimeAndEventsCache(info);

        String[] names = new String[m_lwteCache.size()];
        Enumeration lwteEnum = m_lwteCache.elements();
        int i = 0;
        while (lwteEnum.hasMoreElements())
        {
            LightweightTriggerEvent lwte = (LightweightTriggerEvent) lwteEnum.nextElement();
            names[i++] = lwte.eventName;
        }
        return names;
    }

    private boolean checkPending(LightweightTriggerEvent lwte)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        if (m_srcToPendingEvents == null)
        {
            return false;
        }

        Set srcSet = m_srcToPendingEvents.keySet();
        Iterator srcIter = srcSet.iterator();

        while (srcIter.hasNext())
        {
            Object src = srcIter.next();
            Vector vEvents = (Vector) m_srcToPendingEvents.get(src);
            if (vEvents == null) continue;

            Enumeration eventEnum = vEvents.elements();
            while (eventEnum.hasMoreElements())
            {
                LightweightTriggerEvent pendingLwte = (LightweightTriggerEvent) eventEnum.nextElement();
                if (pendingLwte.hasSameIdentity(lwte)) return true;
            }
        }
        return false;
    }

    public boolean checkStored(LightweightTriggerEvent lwte, RecordedSegmentInfo info)
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        setMediaTimeAndEventsCache(info);

        Enumeration lwteEnum = m_lwteCache.elements();
        while (lwteEnum.hasMoreElements())
        {
            LightweightTriggerEvent storedLwte = (LightweightTriggerEvent) lwteEnum.nextElement();
            if (storedLwte.hasSameIdentity(lwte)) return true; // already exists
        }
        return false; // not found

    }

    public void registerChangeNotification(LightweightTriggerEventChangeListener listener)
    {
        m_lwteChangeHelper.registerChangeNotification(listener);
    }

    public void unregisterChangeNotification(LightweightTriggerEventChangeListener listener)
    {
        m_lwteChangeHelper.unregisterChangeNotification(listener);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#addObserver
     * (org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver)
     */
    public void addObserver(PlaybackClientObserver listener)
    {
        synchronized (m_sync)
        {
            if (!m_playbackListeners.contains(listener)) m_playbackListeners.addElement(listener);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#removeObserver
     * (org.cablelabs.impl.manager.lightweighttrigger.PlaybackClientObserver)
     */
    public void removeObserver(PlaybackClientObserver listener)
    {
        synchronized (m_sync)
        {
            m_playbackListeners.removeElement(listener);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#getPlayers()
     */
    public Vector getPlayers()
    {
        // Internal method - caller should hold the lock
        if (ASSERTING) Assert.lockHeld(m_sync);

        return (Vector) m_players.clone();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#addPlayer
     * (org.cablelabs.impl.media.player.AbstractDVRServicePlayer)
     */
    public void addPlayer(AbstractDVRServicePlayer player)
    {
        if (log.isDebugEnabled())
        {
            log.debug(m_logPrefix + "addPlayer(" + player + ")");
        }

        Object array[] = new Object[0];
        int i;
        synchronized (m_sync)
        {
            // remember player
            if (player == null || m_players.contains(player))
            {
                throw new IllegalArgumentException("addPlayer called but player null or already added: " + player);
            }

            // TODO: refactor
            // DVRActivityLogger.getInstance().log(DVRActivityLogger.DVR_START_RECORDING_PLAY,this);

            if (player instanceof RecordedServicePlayer)
            {
                ((RecordedServicePlayer) player).setRecordingPlaybackTrigger(true);
            }

            m_players.add(player);
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + "addPlayer - current players: " + m_players + " for recording: " + this);
            }

            // indicate that presentation is ongoing
            m_presentationTerminated.setFalse();

            // Notify observers player attached.
            // Get a copy of the list to step through because the lock will be
            // released.
            i = m_playbackListeners.size();
            if (i > 0)
            {
                array = new Object[i];
                m_playbackListeners.copyInto(array);
            }
        }// end sync

        // Notify observers that Player has attached for playback. Observers
        // must perform state checking to determine how to proceed since
        // we do not hold a lock.
        for (int ndx = 0; ndx < i; ndx++)
        {
            PlaybackClientObserver observer = ((PlaybackClientObserver) array[ndx]);
            try
            {
                observer.clientNotify(new PlaybackClientAddedEvent(player));
            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("addPlayer: Exception thrown by listener, continue looping");
                }
            }
        }
    } // END addPlayer()

    /*
     * (non-Javadoc)
     *
     * @see
     * org.cablelabs.impl.manager.RecordingExt#notifyServiceContextPresenting
     * (javax.tv.service.selection.ServiceMediaHandler)
     */
    public void notifyServiceContextPresenting(ServiceContext sc, ServiceMediaHandler smh)
    {
        if (smh instanceof RecordedServicePlayer)
        {
            RecordedServicePlayer player = (RecordedServicePlayer) smh;
            if (player.getRecordingPlaybackTrigger() == false) return;

            // Satisfy org.ocap.dvr.RecordingPlaybackListener requirements
            // Do not notify for discreet Player based recording playback.
            if (player.isServiceContextPlayer())
            {
                int artificialCarouselId = getArtificialCarouselID();
                int[] pids = getBroadcastCarouselPIDs();

                if (log.isDebugEnabled())
                {
                    log.debug(" notifyServiceContextPresenting, artificialCarouselId=" + artificialCarouselId);
                }

                player.setRecordingPlaybackTrigger(false);
                m_recordingManager.notifyPlayBackStart(sc, artificialCarouselId, pids);
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.cablelabs.impl.manager.lightweighttrigger.PlaybackClient#removePlayer
     * (org.cablelabs.impl.media.player.AbstractDVRServicePlayer)
     */
    public void removePlayer(AbstractDVRServicePlayer player)
    {
        Object[] array = new Object[0];
        int i;

        synchronized (m_sync)
        {
            if (player == null || !m_players.contains(player))
            {
                throw new IllegalArgumentException("removePlayer called but player null or not in the collection: " + player);
            }
            if (log.isDebugEnabled())
            {
                log.debug(m_logPrefix + " - removePlayer - players: " + m_players + ", recording: " + this);
            }

            m_players.removeElement(player);
            if (m_players.isEmpty())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(m_logPrefix + "Signalling presentation terminated condition to true");
                }
                // indicate that presentation has been terminated
                m_presentationTerminated.setTrue();
            }

            // Get a copy of the list to step through. The actual list may
            // change
            // but the receipts will still be around.
            i = m_playbackListeners.size();
            if (i > 0)
            {
                array = new Object[i];
                m_playbackListeners.copyInto(array);
            }
        }// end sync

        // Notify observers Player dettached. Observers
        // must perform state checking to determine how to proceed since
        // we do not hold a lock.
        for (int ndx = 0; ndx < i; ndx++)
        {
            PlaybackClientObserver observer = ((PlaybackClientObserver) array[ndx]);
            try
            {
                observer.clientNotify(new PlaybackClientRemovedEvent(player));
            }
            catch (Exception e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Exception thrown by listener, continue looping");
                }
            }
        }
    }

    /**
     * Sets the parent for this recording request. If the parent parameter is
     * null this leaf is orphaned from any previously set parent. If the parent
     * parameter is null and this leaf does not have a parent, this method does
     * nothing and returns successfully. If the parameter is not null and the
     * parent was already set by any method, this leaf is removed from the
     * previously set parent and added to the parent parameter. Unless otherwise
     * noted, the state of the previously set parent will not be affected.
     *
     * If, as a result of this method invocation, this
     * <code>OcapRecordingRequest</code> is removed from a
     * <code>ParentRecordingRequest</code> which is in the
     * COMPLETELY_RESOLVED_STATE, and which contains no other
     * <code>RecordingRequest</code>s, that <code>ParentRecordingRequest</code>
     * SHALL be transitioned to the PARTIALLY_RESOLVED_STATE.
     *
     * If, as a result of this method invocation, this
     * <code>OcapRecordingRequest</code> is removed from a
     * <code>ParentRecordingRequest</code> which is in the CANCELLED_STATE and
     * which contains no additional <code>RecordingRequest</code>s, that
     * <code>ParentRecordingRequest</code> SHALL be deleted from the recording
     * database.
     *
     * @param parent
     *            The new parent of this leaf recording request or null if the
     *            leaf is to be orphaned.
     *
     * @param resolutionParentState
     *            The state into which the parent recording parameter shall be
     *            transitioned to as a result of this method invocation. If the
     *            parent parameter in this method is null, this parameter is
     *            ignored.
     *
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("modify",..) or
     *             RecordingPermission("*",..)
     *
     * @throws IllegalStateException
     *             if the parent parameter is in the CANCELLED_STATE
     *
     */
    public void setParent(ParentRecordingRequest parent, int resolutionParentState)
    {
        SecurityUtil.checkPermission(new RecordingPermission("modify", "own"));
        synchronized (m_sync)
        {
            if (parent.getState() == parent.CANCELLED_STATE) throw new IllegalStateException();

            if (m_parent != null)
            {
                m_parent.notifyRemoveChild(this, m_info.getState(), m_info.getState());
                try
                {
                    m_rdbm.saveRecord(m_parent.m_infoTree);
                }
                catch (IOException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("IOException cause saving record in setParent: " + parent + "." + e);
                    }
                }
            }
            m_parent = (ParentNodeImpl) parent;
            if (m_parent != null)
            {
                m_parent.insertChild((RecordingRequest) this);
                m_parent.m_infoTree.setState(resolutionParentState);
                try
                {
                    m_rdbm.saveRecord(m_parent.m_infoTree);
                }
                catch (IOException e)
                {
                    if (log.isErrorEnabled())
                    {
                        log.error("IOException cause saving record in setParent: " + parent + "." + e);
                    }
                }

            }
            try
            {
                m_rdbm.saveRecord(m_info);
            }
            catch (IOException e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("IOException cause saving record in setParent: " + parent + "." + e);
                }
            }

        }
    }

    /**
     * Returns the log prefix
     */
    public String getLogPrefix()
    {
        return m_logPrefix;
    }

    /**
     * Resource usage for the recording
     */
    private ResourceUsageImpl m_resourceUsage = null;

    /**
     * Stolen from MediaStorageVolumeImpl.java.
     */

    private static final int MAXIMUM_ORGID_NAME_LENGTH = 8;

    private static final int HEX_BASE = 16;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(RecordingImpl.class.getName());

    /**
     * DeletionDetails
     */
    DeletionDetails m_delDetails;

    /*
     * Condition used to signal the removal of all presenting JMF players
     */
    private SimpleCondition m_presentationTerminated = new SimpleCondition(false);

    // list of entities listening for updates
    private Vector m_recordingUpdateListeners = new Vector();

    // Table containing a mapping of broadcast components to
    // time shifted components
    private PidMapTable m_curPidMap;

    String m_logPrefix;

    // When this recording is played back, the player is added to this list so
    // that
    // LightweightTrigger events can be synchronized to it. This field is for
    // LightweightTrigger support.
    private Vector m_players = new Vector();

    /*
     * There is an assumption made here that segments are not shared between
     * different recorded services. If so, the segments would have to keep the
     * list of listeners.
     */
    private LightweightTriggerEventStoreChangeImpl m_lwteChangeHelper = new LightweightTriggerEventStoreChangeImpl();

    /*
    */
    private Vector m_playbackListeners = new Vector();

    /*
     * Flag to check to see if we are already listening
     */
    private boolean m_storageListening = false;

    /*
     * Long representing the system time when the recording was created. This is
     * used to determine the end state of the recording.
     */
    private long m_timeCreated = 0L;

}
