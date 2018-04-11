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


import org.cablelabs.impl.util.DVREventMulticaster;

import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceManagerImpl;
import org.cablelabs.impl.manager.DisableBufferingListener;
import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.OcapSecurityManager;
import org.cablelabs.impl.manager.RecordingDBManager;
import org.cablelabs.impl.manager.DVRStorageManager;
import org.cablelabs.impl.manager.ServiceManager;
import org.cablelabs.impl.recording.RecordingInfo;
import org.cablelabs.impl.recording.RecordingInfo2;
import org.cablelabs.impl.recording.RecordingInfoNode;
import org.cablelabs.impl.recording.RecordingInfoTree;
import org.cablelabs.impl.storage.StorageProxyImpl;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SimpleCondition;
import org.cablelabs.impl.util.SystemEventUtil;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.tv.service.SIManager;
import javax.tv.service.Service;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextPermission;
import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;
import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.ocap.dvr.BufferingRequest;
import org.ocap.dvr.OcapRecordingManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.PrivateRecordingSpec;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.dvr.RequestResolutionHandler;
import org.ocap.dvr.RecordingPlaybackListener;
import org.ocap.dvr.storage.MediaStorageOption;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.dvr.storage.SpaceAllocationHandler;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.NoMoreDataEntriesException;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordedService;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.shared.dvr.RecordingPermission;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListFilter;
import org.ocap.shared.dvr.navigation.RecordingListIterator;
import org.ocap.storage.DetachableStorageOption;
import org.ocap.storage.ExtendedFileAccessPermissions;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.RemovableStorageOption;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageManagerEvent;
import org.ocap.storage.StorageManagerListener;
import org.ocap.storage.StorageOption;
import org.ocap.storage.StorageProxy;
import org.ocap.system.MonitorAppPermission;

import java.io.Serializable;

import org.cablelabs.impl.service.SIManagerExt;
import org.cablelabs.impl.service.ServiceContextExt;
import org.cablelabs.impl.service.ServicesDatabase;

public class RecordingManagerImpl extends OcapRecordingManager implements RecordingManagerInterface,
        StorageManagerListener

{
    private static final int HEX_BASE = 16;

    /*
     * RecordingImpl factory. Overridden in sub-classes to return specialized
     * RecordingImpl instances.
     * 
     * @return instance behind interface
     * 
     * @throws IllegalArgumentException
     */
    public RecordingImplInterface createRecordingImpl(RecordingInfo2 info) throws IllegalArgumentException
    {
        return new RecordingImpl(info, m_rdbm, this);
    }

    /**
     * RecordingImpl factory. Overridden in sub-classes to return specialized
     * RecordingImpl instances.
     * 
     * @return instance behind interface
     * @throws IllegalArgumentException
     */
    public RecordingImplInterface createRecordingImpl(LocatorRecordingSpec lrs)
    {
        return new RecordingImpl(lrs, m_rdbm, this);
    }

    /**
     * RecordingImpl factory. Overridden in sub-classes to return specialized
     * RecordingImpl instances.
     * 
     * @return instance behind interface
     * @throws IllegalArgumentException
     */
    public RecordingImplInterface createRecordingImpl(ServiceRecordingSpec srs)
    {
        return new RecordingImpl(srs, m_rdbm, this);
    }

    /**
     * RecordingImpl factory. Overridden in sub-classes to return specialized
     * RecordingImpl instances.
     * 
     * @return instance behind interface
     * @throws IllegalArgumentException
     */
    public RecordingImplInterface createRecordingImpl(ServiceContextRecordingSpec scrs)
    {
        return new RecordingImpl(scrs, m_rdbm, this);
    }

    /**
     * From org.cablelabs.impl.manager.Manager
     */
    public void destroy()
    {
        // clean up resources
    }

    /**
     * Initialize and restore DVR subsystem. Only gets initialized once when
     * first called. This should only be called by the Manager class:
     * org.cablelabs.impl.manager.recording.RecordingMgr during a
     * getRecordingManager() call. This has to be done outside of the
     * constructor to ensure that all the manager dependencies are ready before
     * calling them.
     */
    void initialize()
    {
        synchronized (this)
        {
            //
            // Initialize low power resume timers
            //
            CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
            long lowPowerResumeTime = nGetLowPowerResumeTime();
            if (lowPowerResumeTime != 0)
            {
                m_scheduler.addBeforeStartRecordingListener(new LowPowerRecordingListener(), ccm.getSystemContext(),
                        lowPowerResumeTime, true);
            }

            //
            // We want to make sure the recording DB is initialized after SI is
            // available,
            // but we can't block manager initialization. So run this async so
            // we don't block
            // initialization.

            m_callerContextManager.getSystemContext().runInContextAsync(new Runnable()
            {
                public void run()
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("initialize (async block): Waiting for SI...");
                    }

                    //
                    // Block until the SI data is available
                    //
                    SIManagerExt siMgr = (SIManagerExt) SIManager.createInstance();
                    siMgr.filterServices(null);

                    //
                    // Restore recordings and initialize the recording database
                    //
                    restoreRecordings();

                    //
                    // Set our initialization condition
                    //
                    RecordingManagerImpl.m_initializationCompleteCondition.setTrue();
                }
            });
        }
    }

    /**
     * Constructor must only be invoked by Manager class:
     * org.cablelabs.impl.manager.recording.RecordingMgr. RecordingMgr contains
     * the sole instance of this class. The correct way to get the correct
     * instance of this class is to call: RecordingManagerInterface
     * recordingManager =
     * (RecordingManagerInterface)((RecordingManager)ManagerManager
     * .getInstance(RecordingManager.class)).getRecordingManager();
     */
    RecordingManagerImpl()
    {
        // instantiate sub-components
        m_navigationManager = NavigationManager.getInstance();
        m_scheduler = Scheduler.getInstance();
        m_resourceManager = RecordingResourceManager.getInstance();
        m_retentionManager = RecordingRetentionManager.getInstance();
        m_callerContextManager = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

        // share a single synchronization object among all sub-components and
        // individual recording objects.
        m_navigationManager.setSyncObject(this);
        m_scheduler.setSyncObject(this);
        m_resourceManager.setSyncObject(this);

        m_networkInterfaceManager = (NetworkInterfaceManagerImpl) NetworkInterfaceManagerImpl.getInstance();

        StorageManager.getInstance().addStorageManagerListener(this);

        // Listen for configured signal
        ServiceManager sm = (ServiceManager) ManagerManager.getInstance(ServiceManager.class);
        sm.getServicesDatabase().addBootProcessCallback(new BootProcessCallbackListener());

        // Get the recording tolerance from the properties file
        m_recLengthTolerance = getRecordingTolerance();
        if (log.isInfoEnabled())
        {
            log.info( "Recording length tolerance: " 
                      + m_recLengthTolerance + "ms" );
        }
        
        // Get the minimum time that a BeforeStartRecordingListener can precede
        //  recording start
        m_minimumBeforeStartNotificationIntervalMs = loadMinimumBeforeStartNotificationInterval();
        if (log.isInfoEnabled())
        {
            log.info( "Minimum before start notification interval: " 
                      + m_minimumBeforeStartNotificationIntervalMs + "ms" );
        }
        
        m_scheduler.setMinimumBeforeStartNotificationInterval(
                     m_minimumBeforeStartNotificationIntervalMs );

        // Get the host-bound content capability flag. This indicates whether the host binds
        //  content to the host in a way that prevents the content from being transferred to 
        //  another host 
        String flagString = MPEEnv.getEnv("OCAP.dvr.recording.hostBoundContent");
        if ((flagString != null) && flagString.equals("true"))
        {
            m_hostBoundContentFlag = true;
        }
        else
        {
            m_hostBoundContentFlag = false;
        }
        if (log.isInfoEnabled())
        {
            log.info( "Content is " + (m_hostBoundContentFlag ? "" : "NOT ") + "considered host-bound."); 
        }
            
        //
        // Attempt to locate our default storage proxy (ignore return value)
        setDefaultStorageProxy();
    }

    /**
     * Loads persisted recordings, updates their states, and starts scheduler.
     * 
     */
    protected void restoreRecordings()
    {
        if (log.isDebugEnabled())
        {
            log.debug("restoreRecordings: Loading recordings...");
        }
        if (log.isDebugEnabled())
        {
            log.debug("getSmallestTimeShiftDuration() returned: " + getSmallestTimeShiftDuration());
        }

        //
        // Load & initialize scheduled recordings
        //
        if (m_storageProxy != null)
        {
            synchronized (this)
            {
                // Load the navigation manager with persisted recordings in valid
                // states
                loadPersistentRecordings(0, m_rdbm.loadRecords(), null, null);
    
                // Manage the recordings that were interrupted during STB downtime
                updateInterruptedRecordings();
    
                // Complete initialization of recordings based on their updated
                // state
                completeRecordingInitialization();
                m_recdbLoaded = true;
    
                // Now check for - and optionally delete - on-disk recordings with
                // no associated OCAP RecordingRequest
                checkForOrphanedRecordings();
    
                // Add PENDING recordings to schedule. Schedule just expirations for
                // non-PENDING recordings
                initializeSchedule();
            } // END synchronized (this)

            if (log.isInfoEnabled())
            {
                log.info("restoreRecordings: finished loading of persisted recordings");
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info("restoreRecordings: Storage not ready - deferring load of recording database");
            }
        }
    } // END restoreRecordings()

    /**
     * Loads all recording metadata from given vector into NavigationManager.
     * 
     * Navigation Manager is loaded from persisted recordings Parent and Leaf
     * recordings are connected Recoding DB entries that were in the INIT_STATE
     * or DESTROYED_STATE are deleted PENDING_WITH_CONFLICT Recording entries
     * that overlap non-fully-initialized recordings may be moved to
     * PENDING_NO_CONFLICT
     * 
     * @return Vector of instantiated RecordingImpls
     * 
     * @author Craig Pratt
     */
    protected Vector loadPersistentRecordings(final int depth, Vector recordings, ParentNodeImpl parent, ParentNodeImpl root)
    {
        final String logPrefix = "RecordingManagerImpl.loadPersistentRecordings(" + depth + "): ";
        Vector reconstitutedRecordings = new Vector();
        Vector uninitializedRecordings = new Vector();

        if (log.isInfoEnabled())
        {
            log.info(logPrefix + "START");
        }
        if (log.isInfoEnabled())
        {
            log.info(logPrefix + "Loading " + recordings.size() + " recordings...");
        }

        for (int i = 0; i < recordings.size(); i++)
        {
            RecordingInfoNode rinfo = (RecordingInfoNode) recordings.get(i);

            int rid;

            try
            {
                rid = rinfo.getUniqueIDInt();
            }
            catch (Exception e)
            {
                if (log.isWarnEnabled())
                {
                    log.warn(logPrefix + "Could not get unique ID from RecordingInfoNode");
                }
                continue;
            }

            if (rinfo instanceof RecordingInfo)
            { // Found a (now depricated) RecordingInfo
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "RecordingInfo -> RecordingInfo2... ");
                }

                try
                {
                    // We will now convert the RecordingInfo to a RecordingInfo2
                    // using the uniqueID from the rinfo. This will allow the
                    // overwrite of the old RecordingInfo to happen when rinfo2
                    // is saved
                    RecordingInfo2 rinfo2 = m_rdbm.newRecord((RecordingInfo) rinfo, rinfo.uniqueId);

                    if (log.isDebugEnabled())
                    {
                        log.debug(logPrefix + "Convertee " + rinfo);
                    }
                    if (log.isDebugEnabled())
                    {
                        log.debug(logPrefix + "Converted " + rinfo2);
                    }

                    // Complete the conversion by saving the RecordingInfo2
                    m_rdbm.saveRecord(rinfo2, RecordingDBManager.ALL);

                    // Now fall through - treating rinfo2 as the
                    // RecordingInfoNode
                    rinfo = rinfo2;
                }
                catch (Exception e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(logPrefix + "Caught exception instantiating recording with ID " + rid + " (" + e + ')');
                    }
                    SystemEventUtil.logRecoverableError(e);
                    continue;
                }
            } // END if (rinfo instanceof RecordingInfo)

            if (rinfo instanceof RecordingInfo2)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "RecordingInfo2:  creating RecordingImpl... ");
                }

                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "RecordingInfo2=" + rinfo);
                }

                RecordingImplInterface rimpl;

                // Attempt to instantiate a leaf recording object from the
                // stored metadata
                try
                {
                    rimpl = createRecordingImpl((RecordingInfo2) rinfo);

                    if ((rimpl.getInternalState() == RecordingImpl.INIT_STATE)
                            || (rimpl.getInternalState() == RecordingImpl.DESTROYED_STATE))
                    {
                        if (log.isInfoEnabled())
                        {
                            log.info(logPrefix + "Found uninitialized or destroyed recording with ID " + rid);
                        }
                    }
                    else
                    {
                        // Set the root and parent of this leaf
                        rimpl.setRootAndParentRecordingRequest(root, parent);

                        // Add recording to the navigation manager
                        m_navigationManager.insertRecording((RecordingRequest) rimpl);

                        reconstitutedRecordings.add(rimpl);

                        if (log.isInfoEnabled())
                        {
                            log.info(logPrefix + "Loaded " + rimpl);
                        }
                }
                }
                catch (Exception e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn(logPrefix + "Caught exception instantiating recording with ID " + rid, e);
                    }
                    SystemEventUtil.logRecoverableError(e);
                    continue;
                }

                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "RecordingInfo2:  done creating RecordingImpl... ");
                }
                // rimpl now owns the recording info
            } // END if (rinfo instanceof RecordingInfo)
            else
            { // rinfo is not a RecordingInfo or RecordingInfo2
                // Assert: rinfo is a RecordingInfoTree
                RecordingInfoTree rit = (RecordingInfoTree) rinfo;

                if (log.isInfoEnabled())
                {
                    log.info(logPrefix + "FOUND Parent RecordingInfoTree " + rit + " with " + rit.children.size()
                            + " children");
                }

                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "Creating new ParentNodeImpl from RecordingInfoTree... ");
                }

                // attempt to instantiate parent recording from
                // RecordingInfoTree
                ParentNodeImpl newParent;
                try
                {
                    newParent = new ParentNodeImpl(rit, m_rdbm, this);
                }
                catch (Exception e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Exception caught instantiating parent recording with ID " + rid, e);
                    }
                    continue;
                }

                newParent.setParent(parent);
                newParent.setRoot(root);

                //
                // Now load the children - setting parent to this one and root
                // to parent's root, or us
                //

                if (log.isDebugEnabled())
                {
                    log.debug(logPrefix + "Adding children for RecordingInfoTree " + rit + "...");
                }

                Vector newChildren = loadPersistentRecordings(depth+1, rit.children, newParent, (root != null) ? root
                        : newParent);

                // Assert: All parent and leaf recordings in rit.children have
                // been instantiated
                // Assert: parent for all newChildren set to newParent
                // Assert: root for all newChildren set to root or newParent, if
                // root is null

                newParent.addChildren(newChildren);

                // Add parent recording to the navigation manager
                m_navigationManager.insertRecording(newParent);

                reconstitutedRecordings.add(newParent);

                if (log.isInfoEnabled())
                {
                    log.info(logPrefix + "Added scheduled parent recording " + newParent + " with "
                            + newChildren.size() + " children");
                }
            } // END else / rinfo is not a RecordingImpl
        } // END for (all RecordingInfoNodes)

        // for any recordings that were still in the INIT state,
        // just delete them. (Adding them here involves the possibility that
        // the Resource Contention Handler may need to be called - but one
        // is unlikely to be registered if this function is run early)
        for (int i = 0; i < uninitializedRecordings.size(); i++)
        {
            RecordingImpl rimpl = (RecordingImpl) uninitializedRecordings.get(i);
            // Assert: all entries in the uninitializedRecordings are
            // RecordingImpls

            final long startTime = rimpl.getRequestedStartTime();
            final long duration = rimpl.getDuration();

            // Note: Do we know that INIT_STATE and DELETED_STATE recordings
            // don't have any on-disk recording?
            // Is this is the right thing to do here? Can we just rely on orphan
            // code to clean up?
            try
            {
                rimpl.delete();
            }
            catch (Exception e)
            {
                SystemEventUtil.logRecoverableError(logPrefix + "Error deleting uninitialized recording starting at "
                        + startTime, e);
            }

            // Make sure any P_W_C recordings which may have been bumped due to
            // this recording
            // are moved to P_N_C (note: If there's more than one, there's no
            // way to know if the same
            // one that went from P_N_C to P_W_C will go back to P_N_C)
            m_resourceManager.attemptToReserveRecordingsDuring(startTime, duration);
            // Note: This may not be necessary if we call delete() - since it
            // calls this function anyway
        } // END for (all uninitialized recordings)

        if (log.isInfoEnabled())
        {
            log.info(logPrefix + "COMPLETE");
        }
        return reconstitutedRecordings;
    } // END loadPersistentRecordings

    /**
     * Updates the state of all interrupted recordings and clones any
     * interrupted recordings which are not yet complete. This routine will be
     * responsible for the setting of the recording state prior to
     * 
     * Pre-conditions for this function:
     * 
     * Navigation manager is loaded RecordingManager mutex is held by caller
     * ri.initializeForState() has not been called.
     * 
     * Post-conditions for this function:
     * 
     * <pre>
     *   IN-PROGRESS recordings are either put in either:
     *     o FAILED state, if end time passed or R_I_N_C and no content was recorded
     *     o INCOMPLETE state, if end time passed or R_I_N_C and some content was recorded
     *     o COMPLETE state, if end time passed or R_I_N_C and all content was recorded
     *     o IN_PROGRESS_WITH_ERROR if end in future and R_W_C
     *     o All their failure exception set to POWER_INTERRUPTION
     *   PENDING recordings with start times in the past and end times in the future:
     *     o FAILED if R_I_N_C and end time passed
     *     o IN_PROGRESS_WITH_ERROR if if R_W_C, start time passed, but end time didn't
     *   PENDING recordings with start times in the past and end times in the past:
     *     o Have their state set to FAILED
     *     o Have their failure exception set to POWER_INTERRUPTION
     *   IN_PROGRESS_WITH_ERROR recordings are put in either:
     *     o FAILED
     *     o Have their failure exception set to POWER_INTERRUPTION
     * </pre>
     * 
     * All recording changes and clones are persisted to recording DB
     */
    void updateInterruptedRecordings()
    {
        final long curTime = System.currentTimeMillis();
        final Vector recordings = m_navigationManager.getRecordingList();

        if (log.isDebugEnabled())
        {
            log.debug("updateInterruptedRecordings: Checking for interrupted recordings...");
        }

        for (int i = 0; i < recordings.size(); i++)
        {
            try
            {
                RecordingImpl rimpl = (RecordingImpl) recordings.get(i);
                long startTime = rimpl.getRequestedStartTime();
                long endTime = startTime + rimpl.getDuration();

                switch (rimpl.getState())
                {
                    case LeafRecordingRequest.FAILED_STATE:
                    {
                        // Leave it alone
                        break;
                    }
                    case LeafRecordingRequest.INCOMPLETE_STATE:
                    case LeafRecordingRequest.COMPLETED_STATE:
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("updateInterruptedRecordings: Recording state is INCOMPLETE_STATE or COMPLETED_STATE");
                        }

                        rimpl.updateFinalStateForRecordedDuration();
                        break;
                    }
                    case LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE:
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("updateInterruptedRecordings: Recording state is IN_PROGRESS_WITH_ERROR_STATE");
                        }
                        if (endTime <= curTime)
                        { // Recording lost resource before power loss,
                            // and end time is in the past
                            if (log.isDebugEnabled())
                            {
                                log.debug("updateInterruptedRecordings: IN_PROGRESS_WITH_ERROR_STATE recording "
                                        + rimpl + " FAILED (start and end time passed)");
                            }

                            rimpl.setFailedExceptionReason(RecordingFailedException.POWER_INTERRUPTION);
                            rimpl.updateFinalStateForRecordedDuration();
                            break;
                        } // END if (recording end time in past)

                        // Assert: I_P_W_E recording has future end time
                        // Assert: rimpl is not RECORDING_IF_NO_CONFLICTS (it
                        // never would have been I_P_W_E)

                        // Leave the recording I_P_W_E
                        break;
                    } // END case IN_PROGRESS_WITH_ERROR_STATE:
                    case LeafRecordingRequest.IN_PROGRESS_STATE:
                    case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                    case LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE:
                    {
                        // First set the Failure exception of the last segment.
                        rimpl.setFailedExceptionReason(RecordingFailedException.POWER_INTERRUPTION);

                        if (endTime > curTime)
                        {
                            // In this case, lack of the "power" resource
                            // interrupted the recording
                            if (log.isDebugEnabled())
                            {
                                log.debug("updateInterruptedRecordings: IN_PROGRESS Recording " + rimpl
                                        + " set to IN_PROGRESS_WITH_ERROR_STATE (end time in future)");
                            }

                            rimpl.setStateNoNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                            break;
                        }

                        // Assert: End time is in the past or recording is
                        // RECORD_IF_NO_CONFLICTS
                        // Determine final state of the formerly in-progress
                        // non-continuable recording
                        rimpl.updateFinalStateForRecordedDuration();
                        break;
                    } // END case IN_PROGRESS

                    case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                    case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                    {
                        if (endTime <= curTime)
                        { // Pending recording was never started

                            if (log.isDebugEnabled())
                            {
                                log.debug("updateInterruptedRecordings: PENDING recording " + rimpl
                                        + " FAILED (start and end time passed)");
                            }

                            rimpl.setStateNoNotify(LeafRecordingRequest.FAILED_STATE);
                            rimpl.setFailedExceptionReason(RecordingFailedException.POWER_INTERRUPTION);
                            break;
                        } // END if (recording end time in past)

                        // Assert: End time is in the future

                        if ((startTime <= curTime) && (endTime > curTime))
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("updateInterruptedRecordings: PENDING recording " + rimpl
                                        + " now IN_PROGRESS_WITH_ERROR (start time past, end in future)");
                            }

                            rimpl.setStateNoNotify(LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE);
                            rimpl.setFailedExceptionReason(RecordingFailedException.POWER_INTERRUPTION);

                            // We'll try to kick off this recording after we're
                            // done here
                            break;
                        }

                        // Assert: Start and end time of PENDING recording in
                        // the future

                        // If start and end time in future, just leave the
                        // recording
                        // in its PENDING state. Scheduler will pick it up
                        break;
                    } // END case PENDING_WITH/NO_CONFLICT
                } // END switch (recording.getState())
            }
            catch (Throwable e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        } // END for all recordings in NavManager

        // Assert: All PENDING recordings in NavManager have end times in the
        // future
        if (log.isDebugEnabled())
        {
            log.debug("updateInterruptedRecordings: COMPLETE");
        }

    } // END updateInterruptedRecordings()

    /**
     * Calls the initializeForState() method on all recordings in Navigation
     * Manager.
     * 
     * Pre-conditions for this function:
     * 
     * Navigation manager must have been initialized All recordings in
     * Navigation Manager have their state set to PENDING, COMPLETED,
     * INCOMPLETE, or FAILED All recordings in Navigation Manager have NOT been
     * initializeForState()-ed
     * 
     * Post-conditions for this function:
     * 
     * All recordings in Navigation Manager have NOT been
     * initializeForState()-ed
     */
    void completeRecordingInitialization()
    {
        final Vector recordings = m_navigationManager.getRecordingList();

        if (log.isDebugEnabled())
        {
            log.debug("completeRecordingInitialization: Completing recording initialization...");
        }

        for (int i = 0; i < recordings.size(); i++)
        {
            RecordingImpl rimpl = (RecordingImpl) recordings.get(i);

            try
            {
                rimpl.initializeForState();
                // Note: This will also re-persist the recording
            }
            catch (Throwable e)
            {
                SystemEventUtil.logRecoverableError(e);
            }
        }
    } // END completeRecordingInitialization()

    /**
     * Add all pending or expire-able recordings to the Scheduler to be started
     * or expired appropriately.
     * 
     * Pre-conditions for this function:
     * 
     * Navigation manager must have been initialized All recordings in
     * Navigation Manager are PENDING, COMPLETED, INCOMPLETE, or FAILED
     * Scheduler is blank or ignores duplicate entries RecordingManager mutex is
     * held by caller
     * 
     * Post-conditions for this function:
     * 
     * PENDING and IN_PROGRESS recordings have start, end, expiration, and 
     * purge times scheduled COMPLETE, INCOMPLETE, and FAILED recordings have 
     * only their expirations scheduled
     */
    void initializeSchedule()
    {
        final Vector recordings = m_navigationManager.getRecordingList();

        if (log.isDebugEnabled())
        {
            log.debug("initializeSchedule: Scheduling recordings & expirations...");
        }

        for (int i = 0; i < recordings.size(); i++)
        {
            RecordingImpl rimpl = (RecordingImpl) recordings.get(i);

            switch (rimpl.getState())
            {
                case LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE:
                case LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE:
                case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("initializeSchedule: Scheduling " + rimpl.externalStateToString(rimpl.getState())
                                  + " recording " + rimpl + '('+ rimpl.getRecordingInfo().toString() + ')');
                    }

                    m_scheduler.scheduleRecording(rimpl, rimpl.getRequestedStartTime(), rimpl.getDuration(),
                            rimpl.getExpirationDate().getTime(), false);
                    break;
                } // END case PENDING_WITH/NO_CONFLICT

                case LeafRecordingRequest.COMPLETED_STATE:
                case LeafRecordingRequest.INCOMPLETE_STATE:
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("initializeSchedule: Scheduling expiration of "
                                  + ((rimpl.getState() == LeafRecordingRequest.COMPLETED_STATE) 
                                          ? "COMPLETED" : "INCOMPLETE") + " recording " + rimpl 
                                          + '(' + rimpl.getRecordingInfo().toString() + ')');
                    }

                    if (rimpl.getExpirationDate().getTime() < System.currentTimeMillis())
                    {
                        rimpl.expire();
                    }
                    else
                    {
                        // Restart expiration timer for all other states.
                        m_scheduler.scheduleExpiration(rimpl);
                    }
                    break;
                }
            } // END switch (recording.getState())
        } // END for all recordings in NavManager

        if (log.isDebugEnabled())
        {
            log.debug("initializeSchedule: COMPLETE");
        }
    } // END initializeSchedule()

    /**
     * Determines if any orphaned recordings exist, and keeps or deletes them as
     * necessary.
     */
    private void checkForOrphanedRecordings()
    {
        Vector nlist = getNativeRecordings();

        Vector olist = getOrphanedRecordings(nlist);

        // if in default recording management mode AND leaveOrphans != true
        // then delete the orphaned recordings
        if (isDefaultRecordingManagementEnabled() == true)
        {
            if (getLeaveOrphansFlag() == false)
            {
                deleteOrphanedRecordings(olist);
            }
            orphanList = null;
        }
        else
        // if (defaultRecordingManagementEnabled == false)
        {
            if (olist.size() == 0)
            {
                try
                {
                    setDefaultRecordingManagementEnabled("Set by RecordingManager");
                }
                catch (Exception e)
                {
                    SystemEventUtil.logRecoverableError(e);
                }
                orphanList = null;
            }
            else
            {
                // keep orphan list so they can be migrated to OCAP recordings
                orphanList = olist;
            }
        }
    }

    /**
     * Builds a list of recording files.
     * 
     * @return list of recording files.
     */
    private Vector getNativeRecordings()
    {
        Vector nativeList = new Vector();

        nRetrieveRecordingList(((StorageProxyImpl) m_storageProxy).getNativeHandle(), nativeList);

        if (log.isDebugEnabled())
        {
            int n = nativeList.size();

            if (log.isDebugEnabled())
        {
                log.debug("getNativeRecordings - number of native recordings = " + n);
        }

            for (int i = 0; i < n; i++)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Recording [" + i + "] = " + (String) nativeList.elementAt(i));
                }
        }
        }
        return nativeList;
    }

    /**
     * Builds a list of orphaned recordings.
     * 
     * Compares the list of native recordings with the recordings that are in
     * the OCAP recording database. If there are any native recordings for which
     * no metadata exists, then add those recordings the orphan list.
     * 
     * @param nativeRecordings
     *            list of names of native recordings
     * 
     * @return list of orphaned recordings
     */
    private Vector getOrphanedRecordings(Vector nativeRecordings)
    {

        int i;
        int n;
        Vector olist = new Vector();

        java.util.Hashtable ht = new java.util.Hashtable();

        // get list of recordings from the DB
        RecordingList rl = m_navigationManager.getEntries();

        if (rl != null)
        {
            // build hashtable map of recording names
            n = rl.size();
            for (i = 0; i < n; i++)
            {
                RecordingRequest rr = rl.getRecordingRequest(i);
                if (rr instanceof LeafRecordingRequest)
                {
                    LeafRecordingRequest lrr = (LeafRecordingRequest) rr;
                    if (log.isDebugEnabled())
                    {
                        log.debug("getOrphanedRecordings - rle found: state: " + lrr.getState());
                    }
                    if (lrr.getState() == LeafRecordingRequest.COMPLETED_STATE
                            || lrr.getState() == LeafRecordingRequest.INCOMPLETE_STATE
                            || lrr.getState() == LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE)
                    {
                        try
                        {
                            SegmentedRecordedServiceImpl srsi = (SegmentedRecordedServiceImpl) lrr.getService();
                            if (srsi != null)
                            {
                                RecordedService[] rsi = srsi.getSegments();
                                for (int j = 0; j < rsi.length; j++)
                                {
                                    if (log.isInfoEnabled())
                                    {
                                        log.info("getOrphanedRecordings - nativeName: "
                                                + ((RecordedServiceImpl) rsi[j]).getNativeName() + "-");
                                    }
                                    ht.put(((RecordedServiceImpl) rsi[j]).getNativeName(), rr);
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            SystemEventUtil.logRecoverableError(e);
                        }
                    }
                }
            }

            // If there are any native recordings for which we have no metadata,
            // then add them to an orphan list.
            n = nativeRecordings.size();
            for (i = 0; i < n; i++)
            {
                if (ht.get(nativeRecordings.elementAt(i)) == null)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getOrphanedRecordings - found a orphan - " + (String) nativeRecordings.elementAt(i));
                    }
                    olist.addElement(nativeRecordings.elementAt(i));
                }
            }
        }

        return olist;
    }

    /**
     * Delete the orphaned recordings in the specified list.
     * 
     * @param olist
     *            list containing all of the orphaned recordings
     */
    private void deleteOrphanedRecordings(Vector olist)
    {
        // for each element in the Vector olist,
        // get the string and delete the recording.
        int n = olist.size();
        for (int i = 0; i < n; i++)
        {
            if (log.isDebugEnabled())
            {
                log.debug("deleteOrphanedRecordings - deleting " + (String) olist.elementAt(i));
            }
            RecordingImpl.nDeleteRecording((String) olist.elementAt(i));
        }
    }

    // Returns true if the given proxy supports the MediaStorageOption
    private boolean isMediaCapable(StorageProxy proxy)
    {
        StorageOption options[] = proxy.getOptions();
        for (int i = 0; i < options.length; i++)
        {
            if (options[i] instanceof MediaStorageOption)
            {
                return true;
            }
        }
        return false;
    }
    
    // Returns a StorageProxy that may serve as the current default storage
    // device
    // for recordings with no specified destination
    private synchronized void setDefaultStorageProxy()
    {
        StorageProxy proxies[] = StorageManager.getInstance()
                                               .getStorageProxies();

        // If there are internal storage devices, we always choose from those
        // first since they are the least likely to come and go.
        StorageProxy primaryCandidate = null;
        StorageProxy secondaryCandidate = null;
        for (int i = 0; i < proxies.length; i++)
        {
            primaryCandidate = proxies[i];

            // Examine the storage options for this proxy
            StorageOption options[] = primaryCandidate.getOptions();
            for (int j = 0; j < options.length; j++)
            {
                // If this device is detachable or removeable then skip it
                // (but keep in mind for secondary choice)
                if (options[j] instanceof DetachableStorageOption ||
                    options[j] instanceof RemovableStorageOption)
                {
                    if (secondaryCandidate == null)
                    {
                        secondaryCandidate = proxies[i];
                    }
                    primaryCandidate = null;
                    break;
                }
            }

            if (primaryCandidate != null)
            {
                // To be a valid candidate, this device must be in the READY state
                // and be capable of storing media
                if (primaryCandidate.getStatus() != StorageProxy.READY ||
                    !isMediaCapable(primaryCandidate))
                {
                    primaryCandidate = null;
                }
                else
                {
                    break; // we have a primary candidate, so no need to keep looking
                }
            }
            // This can not be a secondary candidate if it is not READY or
            // capable of storing media
            else if (secondaryCandidate != null &&
                     (secondaryCandidate.getStatus() != StorageProxy.READY ||
                      !isMediaCapable(secondaryCandidate)))
            {
                secondaryCandidate = null;
            }
        }

        // We have a valid internal candidate, so use it
        if (primaryCandidate != null)
        {
            m_storageProxy = primaryCandidate;
        }
        // No internal candidate, but we have a valid secondary (non-internal)
        // proxy, so use it
        else if (secondaryCandidate != null)
        {
            m_storageProxy = secondaryCandidate;
        }
        // No primary or secondary candidates found. Until something changes,
        // we have no default storage device
        else
        {
            m_storageProxy = null;
        }

        if (m_storageProxy == null)
        {
            SystemEventUtil.logRecoverableError(new Exception("Default StorageProxy not found!!!"));
        }
        else 
        {
            if (log.isInfoEnabled())
            {
                log.info("Default storage proxy is: " + m_storageProxy.getName()
                         + " (" + m_storageProxy.getDisplayName() + ")");
            }
        }
    }
            
    /**
     * Request to get the default device
     * 
     * @return m_storageProxy The proxy representing the default device
     * 
     */
    // Added synchronization modifier for findbugs issues fix
    public synchronized StorageProxy getDefaultStorageProxy()
    {
        return m_storageProxy;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws InterruptedException
     */
    public boolean waitForInitializationToComplete() throws InterruptedException
    {
        boolean waitCondition = m_initializationCompleteCondition.getState();

        if (waitCondition)
        { // Return right away - this condition never goes from true to false
            return true;
        }

        // Assert: condition is false, so we'll wait until it's true

        if (log.isInfoEnabled())
        {
            log.info("waitForInitializationToComplete: WAITING...", new RuntimeException(
                    "Waiting thread (this is not an error):"));
        }

        waitCondition = m_initializationCompleteCondition.waitUntilTrue(18000000); // 5
                                                                                   // minutes

        if (log.isInfoEnabled())
        {
            log.info("waitForInitializationToComplete: DONE "
                    + (waitCondition ? "(initialization completed)" : "(timed out)"));
        }

        return waitCondition;
    } // END waitForInitializationToComplete()

    /**
     * Deletes the specified recording file.
     * 
     * @param recordingId
     *            ID of the recording file to delete.
     */
    public void deleteNativeRecordingFile(String recordingId)
    {
        if (recordingId != null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("deleteNativeRecordingFile - deleting " + recordingId);
            }
            RecordingImpl.nDeleteRecording(recordingId);
        }
    }

    /**
     * Returns a list of orphaned recordings.
     * 
     * @return list of orphaned recordings.
     */
    public Vector getOrphanList()
    {
        return orphanList;
    }

    /**
     * Adds the metadata of an legacy recording to the database.
     * 
     * This method is intended to be used by the DVRUpgradeManager extension to
     * allow pre-OCAP recordings to be added to the OCAP DVR recording database.
     * 
     * @param recInfo
     *            Info to be added to the database.
     * @param appDataId
     *            App-specific data identifier.
     * @param appData
     *            App-specific data that is to be associated with this
     *            recording.
     * 
     * @throws AccessDeniedException
     *             is thrown by RecordingImpl.addAppData
     * @throws IllegalArgumentException
     *             is thrown by RecordingImpl.addAppData
     * @throws NoMoreDataEntriesException
     *             is thrown by RecordingImpl.addAppData
     * @throws NullPointerException
     *             if recInfo is null, or if unable to create an instance of
     *             RecordingImpl.
     */
    public void addOrphanedRecording(RecordingInfo2 recInfo, String appDataId, java.io.Serializable appData)
            throws AccessDeniedException, IllegalArgumentException, NoMoreDataEntriesException, NullPointerException
    {
        if (recInfo == null)
        {
            throw new NullPointerException();
        }

        RecordingImplInterface ri = createRecordingImpl(recInfo);
        if (ri == null)
        {
            SystemEventUtil.logRecoverableError(new Exception("addOrphanedRecording - createRecordingImpl FAILED"));

            throw new NullPointerException();
        }

        ri.updateFinalStateForRecordedDuration();
        ri.initializeForState();

        m_navigationManager.insertRecording((RecordingRequest) ri);
        ri.addAppData(appDataId, appData);
    }

    /**
     * Returns a boolean that idicates if we are to preserve (not delete)
     * orphaned recordings.
     * 
     * @return true if the orphaned recordings are to be preserved.
     */
    private boolean getLeaveOrphansFlag()
    {
        // Determine if we keep or delete orphaned recordings
        String leaveOrphans = MPEEnv.getEnv("OCAP.dvr.recording.leaveOrphans");
        if ((leaveOrphans != null) && leaveOrphans.equals("true"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * 
     * Returns a boolean that indicates if we are operating in the default
     * recording management mode.
     * 
     * @return true if we are in default recording management mode.
     */
    public boolean isDefaultRecordingManagementEnabled()
    {
        boolean result = false;

        String filename = getDefaultRecordingManagementEnabledFilename();

        File f = new File(filename);
        if (f.exists() == true)
        {
            result = true;
        }

        if (log.isDebugEnabled())
        {
            log.debug("isDefaultRecordingManagementEnabled = " + result);
        }

        return result;
    }
    
    /**
     * Writes a flag to the hard drive which indicates that we are operating in
     * the default recording management mode.
     * 
     * If the stack is operating in the default recording managemant mode, then
     * it can keep, recover, or delete recordings as necessary (per (MPEEnv
     * properties, etc.),
     * <p>
     * If the stack is NOT operating in the default recording managemant mode,
     * then the recordings will always be preserved. This will allow an Xlet,
     * with sufficient privileges, to provide information about those recordings
     * so that they may be added to the OCAP recording database.
     * 
     * @param logInfo
     *            This string will be written into a 'flag' file. This will
     *            allow an Xlet to log information about the upgrade that it
     *            performed.
     * 
     * @throws IOException
     *             if unable to create an instance of FileWriter or PrintWriter.
     */
    public void setDefaultRecordingManagementEnabled(String logInfo) throws IOException
    {
        if (isDefaultRecordingManagementEnabled() == false)
        {
            String filename = getDefaultRecordingManagementEnabledFilename();

            FileWriter fw = new FileWriter(filename);
            if (fw == null)
            {
                SystemEventUtil.logRecoverableError(new IOException("setDefaultRecordingManagementEnabled - "
                        + "new FileWriter returned null"));
                throw new IOException();
            }

            PrintWriter pw = new PrintWriter(fw);
            if (pw == null)
            {
                SystemEventUtil.logRecoverableError(new IOException("setDefaultRecordingManagementEnabled - "
                        + "new PrintWriter returned null"));
                throw new IOException();
            }

            if (logInfo == null)
            {
                pw.println("DefaultRecordingManagementEnabled");
            }
            else
            {
                pw.println(logInfo);
            }

            pw.close();
        }
    }

    /**
     * Builds a String that contains the path and filename of the default
     * recording management flag file.
     * 
     * @returns The path and filename of the default recording management flag
     *          file.
     */
    private String getDefaultRecordingManagementEnabledFilename()
    {
        String filename;
        String file;
        String root;

        root = MPEEnv.getEnv(BASEDIR_PROP, DEFAULT_DIR);

        file = MPEEnv.getSystemProperty("org.cablelabs.impl.manager.recording.DefaultRecordingManagementEnabled",
                DEFAULT_FILENAME);
        filename = root + "/" + file;

        if (log.isDebugEnabled())
        {
            log.debug("getDefaultRecordingManagementEnabledFilename = " + filename);
        }

        return filename;
    }

    /**
     * Acquires the recording tolerance from the ini file - or sets it the
     * default value.
     * 
     * @returns the recording tolerance
     */
    private int getRecordingTolerance()
    {
        String sTolerance;

        // Default tolerance is 30 sec.
        int defTolerance = 30000;

        sTolerance = MPEEnv.getEnv("OCAP.dvr.recording.tolerance");
        if (sTolerance == null)
        {
            return defTolerance;
        }

        try
        {
            // we have an environment entry, attempt to parse it
            Integer tol = new Integer(sTolerance);
            return tol.intValue();
        }
        catch (Exception e)
        {
            // unable to parse the value. return default tolerance.
            SystemEventUtil.logRecoverableError(e);
            return defTolerance;
        }
    }

    static final int MIN_BEFORE_START_NOTIFICATION_GRANULARITY_MULTIPLIER = 3;
    static final int MIN_BEFORE_START_NOTIFICATION_DEFAULT_MS = 500;
    
    /**
     * This will establish what minimum interval will be used when scheduling
     * BeforeStartRecordingListener notifications. This will use either the
     * timer granularity or an property to define the minimum interval.
     * 
     * @return minimum interval to be used when scheduling
     * BeforeStartRecordingListener
     */
    private long loadMinimumBeforeStartNotificationInterval()
    {
        String sInterval;
        
        // First try to read the environment-forced value
        sInterval = MPEEnv.getEnv("OCAP.dvr.recording.minbeforestartnotificationinterval");
        if (sInterval != null)
        {
            // we have an environment entry, attempt to parse it
            try
            {
                Integer tol = new Integer(sInterval);
                return tol.intValue();
            }
            catch (Exception e)
            {
                // unable to parse the value - log and fall through
                SystemEventUtil.logRecoverableError(e);
            }
        }

        long timerGranularity = TVTimer.getTimer().getGranularity();
        
        if (timerGranularity > 0)
        {
            return (timerGranularity * MIN_BEFORE_START_NOTIFICATION_GRANULARITY_MULTIPLIER);
        }
        
        sInterval = MPEEnv.getEnv("OCAP.dvr.recording.minbeforestartnotificationinterval.default");
        
        if (sInterval == null)
        {
            return MIN_BEFORE_START_NOTIFICATION_DEFAULT_MS;
        }
        
        try
        {
            Integer tol = new Integer(sInterval);
            return tol.intValue();
        }
        catch (Exception e)
        {
            // unable to parse the value. return default tolerance.
            SystemEventUtil.logRecoverableError(e);
            return MIN_BEFORE_START_NOTIFICATION_DEFAULT_MS;
        }
    } // END loadMinimumBeforeStartNotificationInterval()

    /**
     * This will establish what minimum interval will be used when scheduling
     * BeforeStartRecordingListener notifications. This will use either the
     * timer granularity or an property to define the minimum interval.
     * 
     * @return minimum interval to be used when scheduling
     * BeforeStartRecordingListener
     */
    public long getMinimumBeforeStartNotificationInterval()
    {
        return m_minimumBeforeStartNotificationIntervalMs;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isContentHostBound()
    {
        return m_hostBoundContentFlag;
    }

    /**
     * Adds an event listener for receiving events corresponding to a transition
     * from a pending state to an in-progress state or a failed state. The
     * listener parameter will only be informed of these events for entries the
     * calling application has read file access permission to.
     * 
     * @param ral
     *            The listener to be registered.
     */
    public void addRecordingAlertListener(RecordingAlertListener ral)
    {
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: addRecordingAlertListener(" + ral + ')');
        }

        m_scheduler.addBeforeStartListener(ral, 0);
    }

    /**
     * Adds an event listener for recieving events corresponding to a transition
     * from a pending state to an in-progress state or a failed state. The
     * listener parameter will only be informed of these events for entries the
     * calling application has read file access permission to.
     * 
     * @param ral
     *            The listener to be registered.
     * 
     * @param alertBefore
     *            Time in milliseconds for the alert to be generated before the
     *            start of the scheduled event.
     */
    public void addRecordingAlertListener(RecordingAlertListener ral, long alertBefore)
    {
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: addRecordingAlertListener(" + ral + ',' + alertBefore + ')');
        }
        
        m_scheduler.addBeforeStartListener(ral, alertBefore);
    }

    /**
     * Removes a registed event listener for receiving recording events. If the
     * listener specified is not registered then this method has no effect.
     * 
     * @param ral
     *            the listener to be removed.
     */
    public void removeRecordingAlertListener(RecordingAlertListener ral)
    {
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: removeRecordingAlertListener(" + ral + ')');
        }
        
        m_scheduler.removeListener(ral);
    }

    /**
     * Adds an event listener for receiving events corresponding to a recording
     * playback start.
     * 
     * @param listener
     *            The listener to add.
     */
    public void addRecordingPlaybackListener(RecordingPlaybackListener listener)
    {
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: addRecordingPlaybackListener(" + listener + ')');
        }
        
        synchronized (m_recPbackLock)
        {
            CallerContext cc = m_callerContextManager.getCurrentContext();
            CCData data = getCCData(cc);
            data.listeners = DVREventMulticaster.add(data.listeners, listener);
        }
    }

    /**
     * Removes a registered event listener for receiving recording playback
     * events. If the listener specified is not registered then this method has
     * no effect.
     * 
     * @param listener
     *            The listener to be removed.
     */
    public void removeRecordingPlaybackListener(RecordingPlaybackListener listener)
    {
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: removeRecordingPlaybackListener(" + listener + ')');
        }
        
        synchronized (m_recPbackLock)
        {
            CallerContext cc = m_callerContextManager.getCurrentContext();
            CCData data = getCCData(cc);
            data.listeners = DVREventMulticaster.remove(data.listeners, listener);
        }
    }

    /**
     * Set the SpaceAllocationHandler that will be invoked when any application
     * attempts to allocate space in any MediaStorageVolume. At most only one
     * instance of this handler can be set. Subsequent calls to this method
     * replaces the previous instance with the new one.
     * 
     * @param sah
     *            the space reservation handler.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.recording").
     */
    public void setSpaceAllocationHandler(SpaceAllocationHandler sah)
    {
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: setSpaceAllocationHandler(" + sah + ')');
        }
        
        AllocationHandler ahandler = null;

        SecurityUtil.checkPermission(new MonitorAppPermission("handler.recording"));

        if (sah != null)
        {
            ahandler = new AllocationHandler(sah, m_callerContextManager.getCurrentContext());
        }
    	// Added synchronization block for findbugs issues fix
        synchronized (this) 
        {
        this.m_allocationhandler = ahandler;
    }
    }

    /**
     * Set the RequestResolutionHandler that will be invoked when any
     * application calls the RecordingManager.record method. At most only one
     * instance of this handler can be set. Subsequent calls to this method
     * replaces the previous instance with the new one.
     * 
     * @param rrh
     *            the request resolution handler.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.recording").
     */
    public void setRequestResolutionHandler(RequestResolutionHandler rrh)
    {
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: setRequestResolutionHandler(" + rrh + ')');
        }
        
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.recording"));

        if (log.isDebugEnabled())
        {
            log.debug("CallerContextMgr = " + m_callerContextManager);
        }

        CallerContext cctx = m_callerContextManager.getCurrentContext();
        if (log.isDebugEnabled())
        {
            log.debug("cctx = " + cctx);
        }

        AppID appID = (AppID) cctx.get(CallerContext.APP_ID);
        if (log.isDebugEnabled())
        {
            log.debug("appID = " + appID);
        }

        String key = appID.toString();

        RequestResolutionHandler stored = (RequestResolutionHandler) m_reqRezHandlers.get(key);
        if (stored != null)
        {
            m_reqRezHandlers.remove(key);
        }
        if (rrh != null)
        {
            m_reqRezHandlers.put(key, rrh);
        }

        // monitor for application termination
        new RRHCallbackData(cctx);

    }

    /**
     * Request resolution handler application termination callback
     */
    private class RRHCallbackData implements CallbackData
    {
        RRHCallbackData(CallerContext ctx)
        {
            ctx.addCallbackData(this, this);
        }

        public void destroy(CallerContext ctx)
        {

            AppID appID = (AppID) ctx.get(CallerContext.APP_ID);

            String key = appID.toString();

            RequestResolutionHandler stored = (RequestResolutionHandler) m_reqRezHandlers.get(key);
            if (stored != null)
            {
                m_reqRezHandlers.remove(key);
            }

            ctx.removeCallbackData(this);
        }

        public void active(CallerContext ctx)
        {

        }

        public void pause(CallerContext ctx)
        {

        }
    }

    /**
     * Schedule a child recording request corresponding to an unresolved or
     * partially resolved recording request. This method is called either by the
     * RequestResolutionHandler or by an application that has enough information
     * to provide request resolutions. The implementation shall generate a
     * recording request corresponding to each successful invocation of this
     * method and make that recording request a child of the RecordingRequest
     * passed in as the first parameter. If the implementation has enough
     * information to resolve the newly created recording request, the
     * implementation should resolve the recording request.
     * <p>
     * Implementation should set the state of the recording request "request" to
     * "resolutionState" before the return of this call.
     * 
     * @param request
     *            the RecordingRequest for which the resolution is provided.
     * @param source
     *            the RecordingSpec for the child recording request.
     * @param resolutionState
     *            the state of the RecordingRequest after the return of this
     *            method. The possible values for this parameter are the states
     *            defined in ParentRecordingRequest.
     * 
     * @return the newly scheduled recording request.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.recording").
     * @throws IllegalArgumentException
     *             if the resoltionState is not a state defined in
     *             ParentRecordingRequest, or if the request is not in
     *             unresolved or partially resolved state.
     */
    public RecordingRequest resolve(RecordingRequest request, RecordingSpec source, int resolutionState)
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.recording"));
        int reqState = request.getState();

        if (!((reqState == ParentRecordingRequest.PARTIALLY_RESOLVED_STATE) || (reqState == ParentRecordingRequest.UNRESOLVED_STATE)))
        {
            throw new IllegalStateException();
        }

        if (!((resolutionState == ParentRecordingRequest.CANCELLED_STATE)
                || (resolutionState == ParentRecordingRequest.COMPLETELY_RESOLVED_STATE)
                || (resolutionState == ParentRecordingRequest.PARTIALLY_RESOLVED_STATE) || (resolutionState == ParentRecordingRequest.UNRESOLVED_STATE)))
        {
            throw new IllegalArgumentException("Invalid resolutionState: " + resolutionState);
        }

        // according to 6.2.1.1.7
        if (source instanceof PrivateRecordingSpec)
        {
            // Per OCAP DVR IO5 6.2.1.1.7 (Request Resolution Process), the
            // ParentRecordingRequest SHALL be created using an OcapRecordingProperties
            // that was constructed with an ExtendedFileAccessPermissions with
            // read and write permission for the calling application only
            synchronized (this)
            {
                PrivateRecordingSpec prs = (PrivateRecordingSpec) source;
                OcapRecordingProperties orp = (OcapRecordingProperties) prs.getProperties();
    
                // Create a efap that only contains application read/write
                // permissions
                ExtendedFileAccessPermissions appOnlyPermissions = new ExtendedFileAccessPermissions(false, false, false,
                        false, true, true, null, null);
                OcapRecordingProperties newprops = new OcapRecordingProperties(orp.getBitRate(), orp.getExpirationPeriod(),
                        orp.getRetentionPriority(), orp.getPriorityFlag(), appOnlyPermissions, orp.getOrganization(),
                        orp.getDestination());
    
                PrivateRecordingSpec newPRS = new PrivateRecordingSpec(prs.getPrivateData(), newprops);
    
                // Create new parent in UNRESOLVED_STATE and make it a child
                // the first argument. State changes in request argument will
                // generate a RecordingChangedEvent.STATE_CHANGED.
                ParentNodeImpl child = new ParentNodeImpl(request.getRoot(), request, newPRS, m_rdbm, this);
    
                ((ParentNodeImpl) request).setResolutionState(resolutionState);
                ((ParentNodeImpl) (request)).insertChild(child); // event
                m_navigationManager.insertRecording(child);
                m_navigationManager.notifyRecordingAdded(child);
                // generated
                return child;
            } // END synchronized(this)
        }

        // Schedule LeafRecordingRequest.
        //
        LeafRecordingRequest leaf = null;
        if (source instanceof LocatorRecordingSpec)
        {
            synchronized (this)
            {
                leaf = (LeafRecordingRequest) createRecordingImpl((LocatorRecordingSpec) source);
    
                ((RecordingImpl) leaf).setStateNoNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
                ((RecordingImpl) leaf).initializeForState();
    
                // Check for and resolve conflicts between the new recording
                // and existing recordings. Note: This may involve invoking the
                // resource contention handler and change the states of existing
                // recordings and the new recording
                // TODO: Is this the correct place to call this?
                m_navigationManager.insertRecording(leaf);
            } // END synchronized (this)

            m_resourceManager.resolveConflictsForRecordingAdd((RecordingImpl) leaf);

            synchronized (this)
            {
                ((RecordingImpl) leaf).setRootAndParentRecordingRequest(request.getRoot(), request);
                ((ParentNodeImpl) request).insertChild(leaf);
                ((ParentNodeImpl) request).setResolutionState(resolutionState);
                m_navigationManager.notifyRecordingAdded((RecordingRequestImpl) leaf);
    
                // This method is empty for now. We could move the contexts of this
                // block
                // into completeRecordByLocator later.
                // completeRecordByLocator((LocatorRecordingSpec)source,
                // (RecordingImpl)leaf);
    
                // Add recording to the scheduler
                // The ACT of 0 indicates the originating schedule
                // Reschedule method calls on this recording impl will cause
                // scheduler.scheduleRecordings w/ non-zero ACTs
    
                if (log.isDebugEnabled())
                {
                    log.debug("RecordingManagerImpl: leaf " + leaf + " / start time "
                            + ((LocatorRecordingSpec) source).getStartTime().getTime() + " / duration "
                            + ((LocatorRecordingSpec) source).getDuration());
                }
    
                OcapRecordingProperties orps = (OcapRecordingProperties) (source.getProperties());
                m_scheduler.scheduleRecording((RecordingImpl) leaf, ((LocatorRecordingSpec) source).getStartTime()
                        .getTime(), ((LocatorRecordingSpec) source).getDuration(), roundOffOverflow(
                        orps.getExpirationPeriod(), ((LocatorRecordingSpec) source).getStartTime().getTime()), false);
    
                if (resolutionState != ParentRecordingRequest.COMPLETELY_RESOLVED_STATE)
                {
                    CallerContext cctx = m_callerContextManager.getCurrentContext();
                    AppID appID = (AppID) cctx.get(CallerContext.APP_ID);
    
                    String key = appID.toString();
    
                    final RequestResolutionHandler rrh = (RequestResolutionHandler) m_reqRezHandlers.get(key);
    
                    final RecordingRequest finalLeaf = leaf;
                    cctx.runInContext(new Runnable()
                    {
                        public void run()
                        {
                            rrh.requestResolution(finalLeaf);
                        }
                    });
                }
            } // END synchronized (this)
        } // END if (source instanceof LocatorRecordingSpec)

        if (source instanceof ServiceContextRecordingSpec)
        {
            // TODO: DVR Security - validate record permission and
            // ServiceContext Access Permission
            // TODO: Must updated recording impl to move TSB conversion out of
            // constructor
            // in order to correctly check for conflicts before conversion

            leaf = (LeafRecordingRequest) completeRecordByServiceContext((ServiceContextRecordingSpec) source,
                    (ParentNodeImpl) request, resolutionState);
        }

        return leaf;
    }

    RecordingRequest completeRecordByServiceContext(ServiceContextRecordingSpec source, final ParentNodeImpl parent,
            int resolutionState)
    {
        // TODO: DVR Security - validate record permission and ServiceContext
        // Access Permission
        
        final RecordingImplInterface recording;
        
        synchronized (this)
        {
            recording = createRecordingImpl(source);
            recording.setRootAndParentRecordingRequest(parent.getRoot(), parent);
    
            if (recording.getState() == LeafRecordingRequest.FAILED_STATE)
            {
                // TSB conversion failed. Return.
                return (RecordingRequest) recording;
            }
    
            // Set the initial state to P_N_C. (state may change after running
            // resource contention below)
            recording.setStateNoNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
            recording.initializeForState();
            if (log.isDebugEnabled())
            {
                log.debug("RecordingManagerImpl.recordByLocator: after initializeForState state: " + recording.getState()
                        + "\n");
            }
        } // END synchronized (this)
    
            // insert recording into DB before RCH
            m_navigationManager.insertRecording((RecordingRequest) recording);

        // TODO: Time shift buffer conversion

        // Check for and resolve conflicts between the new recording
        // and existing recordings. Note: This may involve invoking the
        // resource contention handler and change the states of existing
        // recordings and the new recording
        // TODO: Is this the exact, correct place to call this?
        m_resourceManager.resolveConflictsForRecordingAdd(recording);

        synchronized (this)
        {
            // If the recording impl is still in the Init state, process this as
            // a scheduled recording
            if (recording.getState() != LeafRecordingRequest.IN_PROGRESS_STATE
                    && recording.getState() != LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE)
            {
                if (log.isInfoEnabled())
                {
                    log.info("RecordingManagerImpl: ServiceContext recording - treated as future recording");
                }
                RecordingProperties props = null;
                try
                {
                    recording.setRecordingProperties((props = parent.getRecordingSpec().getProperties()));
                }
                catch (Exception e)
                {
                    SystemEventUtil.logRecoverableError(e);
                }

                parent.insertChild((RecordingRequest) recording);
                m_navigationManager.notifyRecordingAdded((RecordingRequestImpl) recording);
                parent.setResolutionState(resolutionState);

                // Add recording to the scheduler
                // The ACT of 0 indicates the originating schedule
                // Reschedule method calls on this recording impl will cause
                // scheduler.scheduleRecordings w/ non-zero ACTs
                m_scheduler.scheduleRecording(recording, source.getStartTime().getTime(), source.getDuration(),
                        roundOffOverflow(props.getExpirationPeriod(), source.getStartTime().getTime()), false);
            }
            else
            {
                if (log.isInfoEnabled())
                {
                    log.info("RecordingManagerImpl: ServiceContext recording - TSB convertion started");
                }

                // updated in progress recordings for disk usage
                m_retentionManager.addInProgressRecording(recording);

                // The ACT of 0 indicates the originating schedule
                // Reschedule method calls on this recording impl will cause
                // scheduler.scheduleRecordings w/ non-zero ACTs
                m_scheduler.scheduleRecording(
                        recording,
                        source.getStartTime().getTime(),
                        source.getDuration(),
                        roundOffOverflow(source.getProperties().getExpirationPeriod(), source.getStartTime().getTime()),
                        true);
            }

            if (resolutionState != ParentRecordingRequest.COMPLETELY_RESOLVED_STATE)
            {
                CallerContext cctx = m_callerContextManager.getCurrentContext();
                AppID appID = (AppID) cctx.get(CallerContext.APP_ID);

                String key = appID.toString();

                final RequestResolutionHandler rrh = (RequestResolutionHandler) m_reqRezHandlers.get(key);

                cctx.runInContext(new Runnable()
                {
                    public void run()
                    {
                        rrh.requestResolution((RecordingRequest) recording);
                    }
                });
            }
        }

        return (RecordingRequest) recording;
    }

    /**
     * Get the prioritized list of overlapping ResourceUsages corresponding to a
     * particular recording request. The list of resource usages may include
     * RecordingResourceUsages and other types of ResourceUsages. The
     * ResourceUsage corresponding to the specified recording request is also
     * included in the prioritized list. The prioritized list is sorted in
     * descending order of prioritization. The prioritization for resource
     * usages are based on the order specified by the ResourceContentionHandler
     * or based on the order specified by a previous call to the
     * setPrioritization() call. If a ResourceContentionHandler is not
     * registered or the setResourcePriorities() was not called previously, the
     * prioritization order will be based on application priorities of the
     * applications returned by the getAppID() call on the ResourceUsages.
     * 
     * @param recording
     *            the RecordingRequest for which overlapping resource usages are
     *            sought.
     * 
     * @return the list of ResourceUsages overlapping with the specified
     *         RecordingRequest, including the ResourceUsage corresponding to
     *         the specified RecordingRequest, sorted in descending order of
     *         prioritization, null if the RecordingRequest is not in one of
     *         pending or in-progress states.
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.recording").
     */
    public ResourceUsage[] getPrioritizedResourceUsages(RecordingRequest recording)
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.recording"));
        return m_resourceManager.getPrioritizedResourceUsages(recording);
    }

    /**
     * Sets the relative priorities for a set of ResourceUsages. This method may
     * be used by an application with MonitorAppPermission("handler.recording")
     * to set the relative priorities for a set of overlapping resource usages.
     * The implementation should use the specified prioritization scheme to
     * resolve conflicts (resource conflicts as well as conflicts for
     * RecordingRequests) between these overlapping resource usages. This call
     * may change the relative priorities specified by the contention handler or
     * a previous call to this method. Changing the relative priorities for the
     * resource usages may result in one or more recording requests changing
     * states.
     * 
     * @param resourceUsageList
     *            a list of ResourceUsages sorted in descending order of
     *            prioritization
     * 
     * @throws SecurityException
     *             if the caller does not have
     *             MonitorAppPermission("handler.recording").
     */
    public void setPrioritization(ResourceUsage[] resourceUsageList)
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.recording"));

        m_resourceManager.setRecordingPrioritiesExplicitly(resourceUsageList);
    }

    /**
     * This RecordingListFilter will only return items to which the identified
     * app has read permission to, according to the read permissions associated
     * with the RecordingRequest.
     * 
     * @author cpratt
     */
    class RecordingRequestFAPReadFilter extends RecordingListFilter
    {
        AppID allowedAppID;

        RecordingRequestFAPReadFilter(AppID allowedAppID)
        {
            this.allowedAppID = allowedAppID;
        }

        public boolean accept(RecordingRequest entry)
        {
            return (((RecordingRequestImpl) entry).hasReadAccess(allowedAppID));
        }
    } // END class RecordingRequestFAPReadFilter

    /**
     * {@inheritDoc}
     */
    public RecordingList getEntries()
    {
        SecurityUtil.checkPermission(new RecordingPermission("read", "own"));

        if (SecurityUtil.hasPermission(new RecordingPermission("*", "*")))
        {
            // Per OCAP DVR IO5 section 7.2.1.4.2, RecordingPermission("*","*")
            // implies:
            // Create, read, modify, delete or cancel any RecordingRequest or
            // RecordedService, regardless of any restrictions specified through
            // the extended file access permission associated with the
            // RecordingRequest.
            return m_navigationManager.getEntries();
        }
        else
        {
            // Assert: Caller has "read" permission (per above)
            AppID callerAppID = (AppID) (m_callerContextManager.getCurrentContext().get(CallerContext.APP_ID));

            // Only return RecordingRequests to which the caller has access
            RecordingListFilter appReadFilter = new RecordingRequestFAPReadFilter(callerAppID);

            return m_navigationManager.getEntries(appReadFilter);
        }
    }

    /**
     * {@inheritDoc}
     */
    public RecordingList getEntries(RecordingListFilter filter)
    {
        SecurityUtil.checkPermission(new RecordingPermission("read", "own"));

        if (SecurityUtil.hasPermission(new RecordingPermission("*", "*")))
        {
            // Per OCAP DVR IO5 section 7.2.1.4.2, RecordingPermission("*","*")
            // implies:
            // Create, read, modify, delete or cancel any RecordingRequest or
            // RecordedService, regardless of any restrictions specified through
            // the extended file access permission associated with the
            // RecordingRequest.
            return m_navigationManager.getEntries(filter);
        }
        else
        {
            // Assert: Caller has "read" permission (per above)
            // Only return RecordingRequests to which the caller has access
            AppID callerAppID = (AppID) (m_callerContextManager.getCurrentContext().get(CallerContext.APP_ID));

            RecordingListFilter comboFilter = new RecordingRequestFAPReadFilter(callerAppID);

            comboFilter.setCascadingFilter(filter);

            return m_navigationManager.getEntries(comboFilter);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addRecordingChangedListener(RecordingChangedListener rll)
    {
        SecurityUtil.checkPermission(new RecordingPermission("read", "own"));
        m_navigationManager.addRecordingChangedListener(rll);
    }

    /**
     * {@inheritDoc}
     */
    public void removeRecordingChangedListener(RecordingChangedListener rll)
    {
        m_navigationManager.removeRecordingChangedListener(rll);
    }

    /**
     * {@inheritDoc}
     */
    public void signalRecordingStart()
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.recording"));
        synchronized (this)
        {
            if (log.isDebugEnabled())
            {
                log.debug("signalRecordingStart called");
            }

            if (!m_recordingEnabled)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("signalRecordingStart: Enabling system-wide recording");
                }
                if (m_recordingDelayTimer != null)
                {
                    m_recordingDelayTimer.deschedule();
                }
                m_recordingEnabled = true;
                notifyRecordingDisabledChangeAsync();
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("signalRecordingStart: System-wide recording already enabled");
                }
            }
        }
    }

    /**
     * Records the stream or streams according to the source parameter. The
     * concrete sub-class of RecordingSpec may define additional semantics to be
     * applied when instances of that sub-class are used.
     * 
     * @param source
     *            specification of stream or streams to be recorded and how they
     *            are to be recorded.
     * 
     * @return an instance of RecordingRequest that represents the added
     *         recording.
     * 
     * @throws IllegalArgumentException
     *             if the source is an application defined class or as defined
     *             in the concrete sub-class of RecordingSpec for instances of
     *             that class
     * 
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("create",..) or
     *             RecordingPermission("*",..)
     * @throws SecurityException
     *             if argument is an instance of
     * @see org.ocap.shared.dvr.ServiceContextRecordingSpec and the calling
     *      application does not have permission to access the
     * @see javax.tv.service.selection.ServiceContext
     * 
     */
    public RecordingRequest record(RecordingSpec source) throws IllegalArgumentException, AccessDeniedException
    {
        return record(source, new String[0], new Serializable[0]);
    }

    /**
     * Records the stream or streams according to the source parameter. The
     * concrete sub-class of RecordingSpec MAY define additional semantics to be
     * applied when instances of that sub-class are used. Overloaded from the
     * <code>org.ocap.shared.dvr.RecordingManager.record</code> method. This
     * method is identical to that method except for the key and appData
     * parameters used to add application specific private data. </p>
     * <p>
     * The keys and appData parameters are parallel arrays where the first entry
     * in the keys array corresponds to the first entry in the appData array and
     * so forth. When a <code>RecordingRequest</code> is created from a call to
     * this method and then delivered to a <code>RecordingChangedListener</code>
     * , the request SHALL contain the application data passed to this method.
     * 
     * @param source
     *            specification of stream or streams to be recorded and how they
     *            are to be recorded.
     * @param keys
     *            the IDs under which the application data is to be added.
     * @param data
     *            the private application data to be added.
     * 
     * @return an instance of RecordingRequest that represents the added
     *         recording.
     * 
     * @throws IllegalArgumentException
     *             if the source is an application defined class or as defined
     *             in the concrete sub-class of RecordingSpec for instances of
     *             that class. Also throws this exception if the keys or appData
     *             parameters are null or not the same length.
     * @throws AccessDeniedException
     *             if the calling application is not permitted to perform this
     *             operation by RecordingRequest specific security attributes.
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("create",..) or
     *             RecordingPermission("*",..)
     */
    public RecordingRequest record(RecordingSpec source, String[] keys, java.io.Serializable[] appData)
            throws IllegalArgumentException, AccessDeniedException
    {
        RecordingRequest rr = null;
        if (keys == null || appData == null || (keys.length != appData.length))
        {
            throw new IllegalArgumentException();
        }

        SecurityUtil.checkPermission(new RecordingPermission("create", "own"));
        OcapRecordingProperties ocapRecProp = null;
        
        if (source == null)
        {
            throw new IllegalArgumentException("RecordingSpec is null");
        }
        
        if (source.getProperties() instanceof OcapRecordingProperties)
            ocapRecProp = (OcapRecordingProperties) source.getProperties();
        OcapSecurityManager osm = (OcapSecurityManager) ManagerManager.getInstance(OcapSecurityManager.class);

        CallerContext cctx = m_callerContextManager.getCurrentContext();
        AppID callerAppid = (AppID) cctx.get(CallerContext.APP_ID);
        String organization = null;
        if (ocapRecProp != null) organization = ocapRecProp.getOrganization();
        if (organization != null)
        {
            organization = organization.trim();
            int orgId = 0;
            try
            {
                orgId = Integer.parseInt(organization, HEX_BASE);
            }
            catch (NumberFormatException e)
            {
                throw e;
            }
            if (callerAppid.getOID() != orgId)
            {
                throw new IllegalArgumentException();
            }
        }

        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: record source: " + source + "\n");
        }

        // Make sure that the recording database has already been initialized
        // and that we're ready to accept recording requests
        synchronized (this)
        {
            if (!m_recdbLoaded)
            {
                throw new IllegalStateException("Recording Database not initialized.");
            }
        }

        RecordingRequest newRecRequest;
        if (source.getClass() == org.ocap.shared.dvr.LocatorRecordingSpec.class)
        {
            newRecRequest = recordByLocator((LocatorRecordingSpec) source, keys, appData);
        }
        else if (source.getClass() == org.ocap.shared.dvr.ServiceContextRecordingSpec.class)
        {
            newRecRequest = recordByServiceContext((ServiceContextRecordingSpec) source, keys, appData);
        }
        else if (source.getClass() == org.ocap.shared.dvr.ServiceRecordingSpec.class)
        {
            newRecRequest = recordByService((ServiceRecordingSpec) source, keys, appData);
        }
        // Todo: Feature 0.9 check for PrivateRecordingSpec
        else if (source.getClass() == org.ocap.dvr.PrivateRecordingSpec.class)
        {
            synchronized (this)
            {
                // Per OCAP DVR IO5 6.2.1.1.7 (Request Resolution Process), the
                // ParentRecordingRequest SHALL be created using an
                // OcapRecordingProperties
                // that was constructed with an ExtendedFileAccessPermissions with
                // read and write
                // permission for the calling application only
                PrivateRecordingSpec prs = (PrivateRecordingSpec) source;
                RecordingProperties newprops = prs.getProperties();
                // Create a efap that only contains application read/write
                // permissions
                ExtendedFileAccessPermissions appOnlyPermissions = new ExtendedFileAccessPermissions(false, false,
                         false, false, true, true, null, null);
                if (newprops instanceof OcapRecordingProperties)
                {
                    OcapRecordingProperties orp = (OcapRecordingProperties)newprops;
                    newprops = new OcapRecordingProperties(orp.getBitRate(), orp.getExpirationPeriod(),
                            orp.getRetentionPriority(), orp.getPriorityFlag(), appOnlyPermissions,
                            orp.getOrganization(), orp.getDestination());         
                }
                else
                {
                    // If base RecordingProperties was used create a OcapRecordingProperties with default values
                    // and application efap
                    newprops = new OcapRecordingProperties(OcapRecordingProperties.HIGH_BIT_RATE, newprops.getExpirationPeriod(),
                            OcapRecordingProperties.DELETE_AT_EXPIRATION, OcapRecordingProperties.RECORD_WITH_CONFLICTS, appOnlyPermissions, null, null);
                }

                PrivateRecordingSpec newPRS = new PrivateRecordingSpec(prs.getPrivateData(), newprops);
        
                final ParentNodeImpl parentRoot = new ParentNodeImpl(null, null, newPRS, m_rdbm, this);
        
                // The resolve process will by synchronous and made asynchronous
                // later.
                m_navigationManager.insertRecording(parentRoot);
                m_navigationManager.notifyRecordingAdded(parentRoot);
        
                for (int i = 0; i < keys.length; i++)
                {
                    try
                    {
                        parentRoot.addAppData(keys[i], appData[i]);
                    }
                    catch (NoMoreDataEntriesException e)
                    {
                        throw new IllegalArgumentException("No more data entries can be entered");
                    }
                }
        
                // get request resolution handler
                String key = callerAppid.toString();
        
                final RequestResolutionHandler rrh = (RequestResolutionHandler) this.m_reqRezHandlers.get(key);
        
                // if no RRH, just return the parent as is
                if (rrh == null)
                {
                    return parentRoot;
                }
        
                // I cannot guarantee the caller that he will return
                // before his resolution handler is called or even before
                // he receives a RecordingChangedEvent. If it is desirable
                // for the application to return from this call before
                // its RequestResolutionHandler is entered, it can apply
                // synchronization.
                cctx.runInContext(new Runnable()
                {
                    public void run()
                    {
                        // Note: lock not held while in the RRH
                        rrh.requestResolution(parentRoot);
                    }
                });
                newRecRequest = parentRoot;
            } // END synchronized (this)
        }
        else
        {
            throw new IllegalArgumentException("Recording spec cannot be an application-defined class (" + source + ')');
        }
        
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: created " + newRecRequest);
        }

        return newRecRequest;
    } // END record()

    /**
     * Gets the smallest time-shift duration supported by the implementation.
     * This method SHALL return a value greater than zero.
     * 
     * @return The smallest time-shift duration in seconds that is supported by
     *         the implementation.
     */
    public long getSmallestTimeShiftDuration()
    {
        if (log.isDebugEnabled())
        {
            log.debug("getSmallestTimeShiftDuration()...");
        }
        return nGetSmallestTimeShiftDuration();
    }

    /**
     * {@inheritDoc}
     */
    public void requestBuffering(BufferingRequest request)
    {
        synchronized (this)
        {
            CallerContext ctx = m_callerContextManager.getCurrentContext();

            if (ctx != m_callerContextManager.getSystemContext())
            {
                m_buffReqManager.addAppToActiveList();
            }
            m_buffReqManager.addActiveBufferingRequest(request);
            addDisableBufferingListener((BufferingRequestImpl) request);
            ((BufferingRequestImpl) request).startBuffering();
        }
    }

    /**
     * @param ctx
     */
    void releaseBufferingRequestsForApp(CallerContext ctx)
    {
        m_buffReqManager.releaseBufferingRequestsForApp(ctx);
    }

    /**
     * Gets a set of buffering requests that were passed to the requestBuffering
     * method and have not been cancelled.
     * 
     * @return An array of active buffering requests, or a 0 length array if no
     *         buffering requests are active.
     */
    public BufferingRequest[] getBufferingRequests()
    {
        synchronized (this)
        {
            return m_buffReqManager.getBufferingRequests();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void cancelBufferingRequest(BufferingRequest request)
    {
        removeDisableBufferingListener((BufferingRequestImpl) request);
        m_buffReqManager.cancelBufferingRequest((BufferingRequestImpl) request);
    }

    /**
     * Gets the maximum bit rate the implementation will use for duration to
     * space in calculations.
     * 
     * @return Maximum bit-rate in bits per second.
     */
    public long getMaxBitRate()
    {
        return nGetMaxBitRate();
    }

    /**
     * Sets the amount of time to delay the start of scheduled recordings after
     * the initial monitor application is running. Calling this method more than
     * once over-writes the previous setting.
     * 
     * @param seconds
     *            Number of seconds to delay.
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("recording").
     * @throws IllegalArgumentException
     *             is the parameter is negative.
     */
    public void setRecordingDelay(long seconds)
    {
        SecurityUtil.checkPermission(new MonitorAppPermission("recording"));
        if (seconds < 0)
        {
            throw new IllegalArgumentException("Illegal negative delay value.");
        }
        synchronized (this)
        {
            if (log.isDebugEnabled())
            {
                log.debug("setRecordingDelay: " + seconds + " enabled: " + true);
            }

            m_recordingDelay = seconds;
        }
    }

    public RequestResolutionHandler getRequestResolutionHandler(String key)
    {
        return (RequestResolutionHandler) m_reqRezHandlers.get(key);
    }

    /**
     * Look up a recording request from the identifier. Implementations of this
     * method should be optimised considering the likely very large number of
     * recording requests. For applications with RecordingPermission("read",
     * "own"), only RecordingRequests of which the calling application has
     * visibility as defined by any RecordingRequest specific security
     * attributes will be returned.
     * 
     * @param id
     *            an identifier as returned by RecordingRequest.getId
     * @return the corresponding RecordingRequest
     * @throws IllegalArgumentException
     *             if there is no recording request corresponding to this
     *             identifier or if the recording request is not visible as
     *             defined by RecordingRequest specific security attributes
     * @throws SecurityException
     *             if the calling application does not have
     *             RecordingPermission("read",..) or RecordingPermission("*",..)
     * @see RecordingRequest#getId
     */
    public RecordingRequest getRecordingRequest(int id) throws IllegalArgumentException
    {
        synchronized (this)
        {
            boolean exception = false;

            RecordingRequest rr = m_navigationManager.getRecordingRequest(id);
            if (rr == null)
            {
                throw new IllegalArgumentException("Recording RecordingRequest " + id + " not found");
            }

            AppID callerAppID = (AppID) (m_callerContextManager.getCurrentContext().get(CallerContext.APP_ID));

            if (!SecurityUtil.hasPermission(new RecordingPermission("*", "*")))
            {
                RecordingListFilter appReadFilter = new RecordingRequestFAPReadFilter(callerAppID);

                if (!appReadFilter.accept(rr))
                {
                    throw new IllegalArgumentException("Read access to RecordingRequest " + id
                            + " denied by file access permissions");
                }
            }

            // Throw exception if we don't have read all permission.
            try
            {
                SecurityUtil.checkPermission(new RecordingPermission("read", "*"));
            }
            catch (SecurityException se)
            {
                // Now throw exception if we don't have 'owner'.
                try
                {
                    SecurityUtil.checkPermission(new RecordingPermission("read", "own"));
                    // Finally plan to throw exception if we don't own the rr.
                    if ((callerAppID == null) || (!callerAppID.equals(rr.getAppID())))
                    {
                        exception = true;
                    }
                }
                catch (SecurityException se2)
                {
                    throw (new SecurityException("No owner priviledges"));
                }
            }

            if (exception)
            {
                throw new SecurityException("not owner of object");
            }

            return rr;
        }
    }

    /**
     * Internal implementation of record method for Locator recording spec. This
     * is called by the publicly visible record() method when the source is an
     * instance of LocatorRecordingSpec
     * 
     * @param source
     *            locator based recording specification
     * @return resultant recording request
     */
    RecordingRequest recordByLocator(LocatorRecordingSpec source, String[] keys, Serializable[] appData)
            throws IllegalArgumentException, AccessDeniedException
    {
        if (log.isDebugEnabled())
        {
            log.debug("RecordingManagerImpl: recordByLocator source: " + source + "\n");
        }

        // verify that the end time is not in the past, per
        // LocatorRecordingSpec javadoc
        final Date now = new Date();
        if (source.getStartTime().getTime() + source.getDuration() <= now.getTime())
        {
            if (log.isInfoEnabled())
            {
                // Fully retro-active recordings are now supported
                log.info("RecordingManagerImpl: recordByLocator end time is in the past.. " + "\n");                
            }
        }

        RecordingImplInterface recording;
        
        synchronized (this)
        {
            recording = createRecordingImpl(source);
    
            // Set the initial state to P_N_C. (state may change after running
            // resource contention below)
            recording.setStateNoNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
            recording.initializeForState();
            if (log.isDebugEnabled())
            {
                log.debug("RecordingManagerImpl.recordByLocator: after initializeForState state: " + recording.getState()
                        + "\n");
            }
    
            for (int i = 0; i < keys.length; i++)
            {
                try
                {
                    recording.addAppData(keys[i], appData[i]);
                }
                catch (NoMoreDataEntriesException e)
                {
                    throw new IllegalArgumentException("No more data entries can be entered");
                }
            }
            recording.setRootAndParentRecordingRequest(null, null);
    
            // Add recording to the navigation manager
            m_navigationManager.insertRecording((RecordingRequest) recording);
        } // END synchronized (this)

        // Check for and resolve conflicts between the new recording
        // and existing recordings. Note: This may involve invoking the
        // resource contention handler and change the states of existing
        // recordings and the new recording
        m_resourceManager.resolveConflictsForRecordingAdd(recording);

        if (log.isDebugEnabled())
        {
            log.debug("RecordingManagerImpl: after resolveConflictsForRecordingAdd state: " + recording.getState()
                    + "\n");
        }

        synchronized (this)
        {
            if (log.isInfoEnabled())
            {
                log.info("RecordingManagerImpl: Scheduling " + recording);
            }

            // Add recording to the scheduler
            // The ACT of 0 indicates the originating schedule
            // Reschedule method calls on this recording impl will cause
            // scheduler.scheduleRecordings w/ non-zero ACTs
            m_scheduler.scheduleRecording(recording, source.getStartTime().getTime(), source.getDuration(),
                    roundOffOverflow(source.getProperties().getExpirationPeriod(), source.getStartTime().getTime()),
                    false);

            m_navigationManager.notifyRecordingAdded((RecordingRequestImpl) recording);

        } // END synchronized(this)

        return (RecordingRequest) recording;
    }

    /**
     * Internal implementation of record method for Service recording spec. This
     * is called by the publicly visible record() method when the source is an
     * instance of ServiceRecordingSpec
     * 
     * @param source
     *            service based recording specification
     * @return resultant recording request
     */
    RecordingRequest recordByService(ServiceRecordingSpec source, String[] keys, Serializable[] appData)
            throws IllegalArgumentException, AccessDeniedException
    {
        if (log.isDebugEnabled())
        {
            log.debug("RecordingManagerImpl: recordByService source: " + source + "\n");
        }

        // verify that the end time is not in the past, per
        // ServiceRecordingSpec javadoc
        final Date now = new Date();
        if (source.getStartTime().getTime() + source.getDuration() <= now.getTime())
        {
            if (log.isInfoEnabled())
            {
                // Fully retro-active recordings are now supported
                log.info("RecordingManagerImpl: recordByService end time is in the past.. " + "\n");                
            }
        }

        RecordingImplInterface recording;
        
        synchronized (this)
        {
            recording = createRecordingImpl(source);
    
            // Set the initial state to P_N_C. (state may change after running
            // resource contention below)
            recording.setStateNoNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
            recording.initializeForState();
            if (log.isDebugEnabled())
            {
                log.debug("RecordingManagerImpl.recordByService: after initializeForState state: " + recording.getState()
                        + "\n");
            }
    
            for (int i = 0; i < keys.length; i++)
            {
                try
                {
                    recording.addAppData(keys[i], appData[i]);
                }
                catch (NoMoreDataEntriesException e)
                {
                    throw new IllegalArgumentException("No more data entries can be entered");
                }
            }
            recording.setRootAndParentRecordingRequest(null, null);
    
            // Add recording to the navigation manager
            m_navigationManager.insertRecording((RecordingRequest) recording);
        }

        // Check for and resolve conflicts between the new recording
        // and existing recordings. Note: This may involve invoking the
        // resource contention handler and change the states of existing
        // recordings and the new recording
        m_resourceManager.resolveConflictsForRecordingAdd(recording);

        synchronized (this)
        {
            if (log.isInfoEnabled())
            {
                log.info("RecordingManagerImpl: service recording...");
            }

            // Add recording to the scheduler
            // The ACT of 0 indicates the originating schedule
            // Reschedule method calls on this recording impl will cause
            // scheduler.scheduleRecordings w/ non-zero ACTs
            m_scheduler.scheduleRecording(recording, source.getStartTime().getTime(), source.getDuration(),
                    roundOffOverflow(source.getProperties().getExpirationPeriod(), source.getStartTime().getTime()),
                    false);

            m_navigationManager.notifyRecordingAdded((RecordingRequestImpl) recording);
        }

        return (RecordingRequest) recording;
    }

    /**
     * Internal implementation of record method for ServiceContext recording
     * spec. This is called by the publicly visible record() method when the
     * source is an instance of ServiceContextRecordingSpec
     * 
     * @param source
     *            service context based recording specification
     * @return resultant recording request
     */
    RecordingRequest recordByServiceContext(ServiceContextRecordingSpec source, String[] keys,
            java.io.Serializable[] appData) throws IllegalArgumentException, AccessDeniedException
    {
        if (source.getStartTime().after(new Date()))
        {
            throw new IllegalArgumentException("Specified start time is later than current time");
        }

        SecurityUtil.checkPermission(new ServiceContextPermission("access", "own"));

        RecordingImplInterface recording;
        
        synchronized (this)
        {
            recording = createRecordingImpl(source);
    
            recording.setStateNoNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
            recording.initializeForState();
            if (log.isDebugEnabled())
            {
                log.debug("RecordingManagerImpl.recordByServiceContext: after initializeForState state: "
                        + recording.getState() + "\n");
            }
    
            for (int i = 0; i < keys.length; i++)
            {
                try
                {
                    recording.addAppData(keys[i], appData[i]);
                }
                catch (NoMoreDataEntriesException e)
                {
                    throw new IllegalArgumentException("No more data entries can be entered");
                }
            }
            recording.setRootAndParentRecordingRequest(null, null);
    
            // Add recording to the navigation manager
            m_navigationManager.insertRecording((RecordingRequest) recording);
        } // END synchronized (this)

        // Check for and resolve conflicts between the new recording
        // and existing recordings. Note: This may involve invoking the
        // resource contention handler and change the states of existing
        // recordings and the new recording
        // TODO: Is this the exact, correct place to call this?
        m_resourceManager.resolveConflictsForRecordingAdd(recording);

        synchronized (this)
        {
            if (log.isInfoEnabled())
            {
                log.info("RecordingManagerImpl: ServiceContext recording...");
            }

            // Add recording to the scheduler
            // The ACT of 0 indicates the originating schedule
            // Reschedule method calls on this recording impl will cause
            // scheduler.scheduleRecordings w/ non-zero ACTs
            m_scheduler.scheduleRecording(recording, source.getStartTime().getTime(), source.getDuration(),
                    roundOffOverflow(source.getProperties().getExpirationPeriod(), source.getStartTime().getTime()),
                    false);

            m_navigationManager.notifyRecordingAdded((RecordingRequestImpl) recording);
        } // END synchronized(this)

        return (RecordingRequest) recording;
    }

    /**
     * Deletes the recording from storage. The method removes the
     * {@link RecordingListEntry}, as well as the corresponding
     * {@link RecordingSession}, {@link RecordedService} objects, and all
     * recorded elementary streams (e.g., files and directory entries)
     * associated with the RecordedService. If any application call any method
     * on stale references of removed objects the implementation shall throw an
     * IllegalStateException.
     * <p>
     * If the recording is in the IN_PROGRESS state the implementation will call
     * the {@link RecordingSession} stop method before deleting the recording.
     * </p>
     * 
     * @param rle
     *            The recording list entry to be deleted.
     * 
     * @throws SecurityException
     *             if the calling application does not have write file access
     *             permission, or MonitorAppPermission("recording").
     */
    public void delete(RecordingImpl recording)
    {
        /*
         * Validate recording
         */
        if (recording == null || recording.getInternalState() == RecordingImpl.DESTROYED_STATE)
        {
            throw (new IllegalStateException());
        }

        synchronized (this)
        {
            int oldState = recording.getState();
            /* TODO: DVR Security - does app have write permission */

            /*
             * If the recording has pending scheduled events, tell the scheduler
             * to remove this recording from the schedule TODO: May need to
             * update if expiration is moved to the scheduler
             */
            if (oldState == LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE
                    || oldState == LeafRecordingRequest.IN_PROGRESS_STATE
                    || oldState == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE
                    || oldState == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE
                    || oldState == LeafRecordingRequest.DELETED_STATE)
            {
                m_scheduler.cancelRecording(recording);
            }

            /*
             * tell nav manager to remove this recording from the database
             */
            m_navigationManager.removeRecording(recording, LeafRecordingRequest.DELETED_STATE, oldState);
        }
    }

    /**
     * 
     * @param reservice
     */
    void delete(RecordedServiceImpl service)
    {
        // TODO: this method is never called -- should it be removed?
    }

    /**
     * Implementation of StorageManagerListener
     */
    public void notifyChange(StorageManagerEvent evt)
    {
        synchronized (this)
        {
            StorageProxy proxy = evt.getStorageProxy();

            switch (evt.getEventType())
            {
            case StorageManagerEvent.STORAGE_PROXY_ADDED:
                // We only care about this event when we don't currently
                // have a default storage device
                if (m_storageProxy == null)
                {
                    setDefaultStorageProxy();
                }
                break;

            case StorageManagerEvent.STORAGE_PROXY_CHANGED:
                if (m_storageProxy != null)
                {
                    // Check to see if our current default storage device has
                    // become "not ready". If so, try to set a replacement
                    if (proxy == m_storageProxy &&
                        proxy.getStatus() != StorageProxy.READY)
                    {
                        setDefaultStorageProxy();
                    }
                }
                else
                {
                    // We don't currently have a default storage proxy, so if
                    // this proxy has become READY, then attempt to set a new one
                    setDefaultStorageProxy();
                }
                break;

            case StorageManagerEvent.STORAGE_PROXY_REMOVED:
                if (m_storageProxy != null && proxy == m_storageProxy)
                {
                    setDefaultStorageProxy();
                }
                break;

            default:
                if (log.isWarnEnabled())
                {
                    log.warn("StorageManagerEvent received: Invalid event type - "
                            + evt.getEventType());
                }
                break;
            }
        }

    }

    /**
     * A <i>wrapper</i> for a given <code>SpaceAllocationHandler</code> and its
     * originating <code>CallerContext</code>.
     */
    private class AllocationHandler implements SpaceAllocationHandler, CallbackData
    {
        public AllocationHandler(SpaceAllocationHandler handler, CallerContext context)
        {
            this.handler = handler;
            this.context = context;

            context.addCallbackData(this, getClass());
        }

        /**
         * Implements <code>SpaceAllocationHandler.allowReservation()</code> by
         * invoking the <i>handler</i> within the <i>context</i> given at
         * construction time.
         */
        public long allowReservation(LogicalStorageVolume volume, AppID app, long spaceRequested)
        {
            final LogicalStorageVolume param1 = volume;
            final AppID param2 = app;
            final long param3 = spaceRequested;
            final long[] returnValue = { 0 };

            CallerContext.Util.doRunInContextSync(context, new Runnable()
            {
                public void run()
                {
                    if (context != null) // in case the app is gone...
                    {
                        returnValue[0] = handler.allowReservation(param1, param2, param3);
                    }
                }
            }); // block until complete

            return returnValue[0];
        }

        /**
         * Causes the <code>RezMgr</code> to forget this filter. Causes the
         * <code>CallerContext</code> to forget this data.
         */
        public synchronized void destroy(CallerContext context)
        {
            clearHandler(this);
            handler = null;
            context.removeCallbackData(getClass());
            context = null;
        }

        public void pause(CallerContext callerContext)
        {
        }

        public void active(CallerContext callerContext)
        {
        }

        private SpaceAllocationHandler handler;

        private CallerContext context;
    }

    /**
     * Clears the currently set handler for the given resourceProxy if it is the
     * same as the given handler.
     */
    private synchronized void clearHandler(AllocationHandler handler)
    {
        if (this.m_allocationhandler == handler) this.m_allocationhandler = null;
    }

    // Description copied from RecordingManager interface
    public synchronized long checkAllocation(final LogicalStorageVolume volume, final AppID app,
            final long spaceRequested)
    {
        if (volume == null) throw new NullPointerException("volume must be specified");
        if (app == null) throw new NullPointerException("appId must be specified");

        return (m_allocationhandler != null) ? m_allocationhandler.allowReservation(volume, app, spaceRequested)
                : spaceRequested;
    }

    /**
     * Checks to determine if the default storage device is ready
     * 
     * @return true if default storage is ready
     */
    boolean isStorageReady()
    {
        return m_storageProxy != null;
    }

    /**
     * Get the MediaStorageVolume that is the default storage volume location
     * for recordings. This will be called if and only if orp.getDestination
     * returns null at the start of a recording.
     * 
     * @return default MediaStorageVolume for the default device
     *         (m_defaultStorageProxy in StorageManagerImpl)
     */
    public MediaStorageVolume getDefaultMediaStorageVolume()
    {
        MediaStorageVolume defaultMSV = null;
    	// Added code for caching for findbugs issues fix
        StorageProxy l_storageProxy;
        synchronized(RecordingManagerImpl.this)
        {
        	l_storageProxy = m_storageProxy;
        }
        if (l_storageProxy == null)
        {
            return defaultMSV;
        }
        StorageOption[] storageOptionList = l_storageProxy.getOptions();
        for (int j = 0; (null == defaultMSV) && j < storageOptionList.length; j++)
        {
            if (storageOptionList[j] instanceof MediaStorageOption)
            {
                defaultMSV = ((MediaStorageOption) storageOptionList[j]).getDefaultRecordingVolume();
            }
        }
        return defaultMSV;
    }

    /**
     * Adds a buffering disabled listener to the recording manager. This
     * listener will be notified of any changes to the stack-wide buffering
     * enabled state as implied by the OcapRecordingManager
     * enable/disableBuffering() methods.
     * 
     * If the listener passed in is already registered, this method will have no
     * affect.
     * 
     * @param bdl
     *            the listener to add
     */
    public void addDisableBufferingListener(DisableBufferingListener dbl)
    {
        synchronized (m_bufferingDisableListenerList)
        {
            if (log.isDebugEnabled())
            {
                log.debug("addDisableBufferingListener: " + dbl);
            }
            m_bufferingDisableListenerList.add(dbl);
        }
    }

    /**
     * Removes the specified buffering disabled listener from the recording
     * manager.
     * 
     * If the listener passed in is not already registered, this method will
     * have no affect.
     * 
     * @param dbl
     *            the listener to remove
     */
    public void removeDisableBufferingListener(DisableBufferingListener dbl)
    {
        synchronized (m_bufferingDisableListenerList)
        {
            if (log.isDebugEnabled())
            {
                log.debug("removeDisableBufferingListener: " + dbl);
            }
            m_bufferingDisableListenerList.remove(dbl);
        }
    }

    /**
     * queries the recording manager to determine if state wide buffering is
     * enabled.
     * 
     * @return true if buffering is enabled
     */
    public boolean isBufferingEnabled()
    {
        return m_bufferingEnabled;
    }

    private void notifyBufferingDisabledChange()
    {
        final boolean enabledState = m_bufferingEnabled;
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: notifyBufferingDisabledChange(): " + enabledState);
        }
        
        Object [] listenerList;
        
        synchronized (m_bufferingDisableListenerList)
        {
            listenerList = m_bufferingDisableListenerList.toArray();
        }
        
        for (int i = 0; i < listenerList.length; i++)
        {
            final DisableBufferingListener dbl = (DisableBufferingListener) listenerList[i];

            try
            {
                dbl.notifyBufferingDisabledStateChange(enabledState);
            }
            catch (Exception e)
            {
                if (log.isErrorEnabled())
                {
                    log.error("Exception caught in notifyBufferDisableChange:" + e);
                }
            }
        }
    } // END notifyBufferingDisabledChange()

    /**
     * Enables time-shift buffering and buffering without presentation. The
     * default is buffering is enabled.
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("recording").
     */
    public void enableBuffering()
    {
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: enableBuffering()");
        }
        
        SecurityUtil.checkPermission(new MonitorAppPermission("recording"));
        
        m_bufferingEnabled = true;
        
        notifyBufferingDisabledChange();
    }

    /**
     * Disables time-shift buffering and buffering without presentation. All
     * time-shift operations cease immediately and any presenting services that
     * are time-shifted SHALL be taken to the live point. Any buffering without
     * presentation activities SHALL cease to be honored. Any content in a
     * time-shift buffer before this method was called SHALL not be accessible
     * if the <code>enableBuffering</code> method is called. If an
     * implementation uses time-shift buffering for recording creation it MAY
     * segment the recording.
     * 
     * @throws SecurityException
     *             if the calling application does not have
     *             MonitorAppPermission("recording").
     */
    public void disableBuffering()
    {
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: disableBuffering()");
        }
        
        SecurityUtil.checkPermission(new MonitorAppPermission("recording"));
        
        m_bufferingEnabled = false;

        notifyBufferingDisabledChange();
    }

    /**
     * Deletes multiple recordings. The implementation SHALL execute the
     * equivalent of the <code>RecordingRequest.delete</code> method for each
     * <code>RecordingRequest</code> in the <code>requests</code> parameter.
     * </p>
     * <p>
     * The recordings SHALL be deleted in incrementing array index order from
     * the first element at <code>requests[0]</code>.
     * 
     * @param requests
     *            List of <code>RecordingRequest</code> recordings to delete.
     * 
     * @throws AccessDeniedException
     *             if the calling application is not granted
     *             MonitorAppPermission("handler.recording").
     */
    public void deleteRecordings(final RecordingList requests)
    {
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: deleteRecordings()");
        }
        
        // Filed JIRA issue OCORI-792 to track spec issues related with
        // this method
        // Note: This will throw SecurityException instead of
        // AccessDeniedException.
        // Since AccessDeniedException is not declared, we can't throw it.
        // And it's inconsistent with the rest of the methods here anyway...
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.recording"));
        
        deleteRecordingsInternal(requests);
    } // END deleteRecordings()

    // Make sure every LeafRecordingRequest state is represented in these arrays
    public final static int RR_DELETED_STATES[] = new int[] { LeafRecordingRequest.DELETED_STATE };

    public final static int RR_PENDING_STATES[] = new int[] { LeafRecordingRequest.PENDING_NO_CONFLICT_STATE,
            LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE };

    public final static int RR_IN_PROGRESS_STATES[] = new int[] { LeafRecordingRequest.IN_PROGRESS_STATE,
            LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE,
            LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE,
            LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE };

    public final static int RR_FAILED_STATES[] = new int[] { LeafRecordingRequest.FAILED_STATE };

    public final static int RR_COMPLETED_STATES[] = new int[] { LeafRecordingRequest.COMPLETED_STATE,
            LeafRecordingRequest.INCOMPLETE_STATE };

    /**
     * This will delete any/all recordings in the designated state(s). Caller is
     * assumed to have checked for appropriate permissions.
     * 
     * @param states
     */
    public void deleteRecordingsInStates(int[] states)
    {
        deleteRecordingsInternal(m_navigationManager.getOverlappingEntriesInStates(0, Long.MAX_VALUE, states));
    } // END deleteRecordingsInStates()

    /**
     * This will delete any/all recordings in the designated RecordingList. Caller is
     * assumed to have checked for appropriate permissions.
     * 
     * @param rl The recordings to delete
     */
    private void deleteRecordingsInternal(final RecordingList rl)
    {
        RecordingListIterator rli = rl.createRecordingListIterator();

        while (rli.hasNext())
        {
            RecordingRequest rr = rli.nextEntry();

            try
            {
                rr.delete();
            }
            catch (Exception e)
            {
                if (log.isInfoEnabled())
                {
                    log.info( "RecordingManagerImpl.deleteRecordingsInternal(ORS): Caught exception " + e.toString()
                              + " deleting " + rr.toString() );
                }
            }
        } // END while
    } // END deleteRecordingsInternal(RecordingList)

    /**
     * Deletes all recordings. The implementation SHALL execute the equivalent
     * of the <code>RecordingRequest.delete</code> method for each
     * <code>RecordingRequest</code> in the database of recordings maintained by
     * this manager and delete all of the recordings in the database. </p>
     * <p>
     * The recordings SHALL be deleted in order of pending recordings first in
     * any order, then in progress recordings in any order, then failed
     * recordings in any order, then completed recordings in any order.
     * 
     * @throws AccessDeniedException
     *             if the calling application is not granted
     *             MonitorAppPermission("handler.recording").
     */
    public void deleteAllRecordings()
    {
        if (log.isInfoEnabled())
        {
            log.info("RecordingManagerImpl: deleteAllRecordings()");
        }
        
        // Filed JIRA issues OCORI-792 and and OCSPEC-336 to track spec issues related with
        // this method
        // Note: This will throw SecurityException instead of
        // AccessDeniedException.
        // Since AccessDeniedException is not declared, we can't throw it.
        // And it's inconsistent with the rest of the methods here anyway...
        SecurityUtil.checkPermission(new MonitorAppPermission("handler.recording"));

        synchronized (this)
        {
            // The Javadoc states that we need to delete the recordings in a
            // particular order
    
            // Delete PENDING recordings
            deleteRecordingsInStates(RR_PENDING_STATES);
    
            // Delete IN_PROGRESS recordings
            deleteRecordingsInStates(RR_IN_PROGRESS_STATES);
    
            // Delete FAILED recordings
            deleteRecordingsInStates(RR_FAILED_STATES);
    
            // Delete DELETED recordings (RecordingRequests that completed but 
            //  had their RecordedService delete()-ed)
            deleteRecordingsInStates(RR_DELETED_STATES);
    
            // Delete COMPLETED recordings
            deleteRecordingsInStates(RR_COMPLETED_STATES);
            
            // Delete everything that's left (order is specified in the API - see OCSPEC-336)
            deleteRecordingsInternal(m_navigationManager.getAllEntries());
        } // END synchronized (this)
    } // END deleteAllRecordings()

    /**
     * Adds a recording disabled listener to the recording manager. This listener
     * will be notified of any changes to the recording enabled/disabled state
     * 
     * If the listener passed in is already registered, this method will have no
     * affect.
     * 
     * @param dsl
     *            the listener to add
     */
    public void addRecordingDisabledListener(RecordingDisabledListener dsl)
    {
        synchronized (this)
        {
            if (log.isDebugEnabled())
            {
                log.debug("addDisableRecordingListener: " + dsl);
            }
            if (!m_scheduleDisableListenerList.contains(dsl))
            {
                m_scheduleDisableListenerList.add(dsl);
            }
        }
    }

    /**
     * Removes the specified recording disabled listener from the recording
     * manager.
     * 
     * If the listener passed in is not already registered, this method will
     * have no affect.
     * 
     * @param dsl
     *            the listener to remove
     */
    public void removeRecordingDisabledListener(RecordingDisabledListener dsl)
    {
        synchronized (this)
        {
            if (log.isDebugEnabled())
            {
                log.debug("removeDisableRecordingListener: " + dsl);
            }
            m_scheduleDisableListenerList.remove(dsl);
        }
    }

    /**
     * queries the recording manager to determine if recording is
     * currently enabled.
     * 
     * @return true if recording is enabled
     */
    public boolean isRecordingEnabled()
    {
        synchronized (this)
        {
            return m_recordingEnabled;
        }
    }

    /**
     * Notify listeners synchronously that recording is disabled
     */
    private void notifyRecordingDisabledChangeSync()
    {
        synchronized (this)
        {
            RecordingDisabledListener dsl[] = (RecordingDisabledListener[]) m_scheduleDisableListenerList.toArray(new RecordingDisabledListener[0]);
            for (int i = 0; i < dsl.length; i++)
            {
                dsl[i].notifyRecordingEnabledStateChange(m_recordingEnabled);
            }
        }
    }

    /**
     * Notify listeners asynchronously that the recording is disabled
     */
    private void notifyRecordingDisabledChangeAsync()
    {
        final boolean enabledState = m_recordingEnabled;
        CallerContext sysctx = m_callerContextManager.getSystemContext();
        synchronized (this)
        {
            for (int i = 0; i < m_scheduleDisableListenerList.size(); i++)
            {
                final RecordingDisabledListener dsl = (RecordingDisabledListener) m_scheduleDisableListenerList.get(i);

                sysctx.runInContext(new Runnable()
                {
                    public void run()
                    {
                        dsl.notifyRecordingEnabledStateChange(enabledState);
                    }
                });
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public BufferingRequest createBufferingRequest(Service service, long minDuration, long maxDuration,
            ExtendedFileAccessPermissions efap)
    {
        return m_buffReqManager.createBufferingRequest(service, minDuration, maxDuration, efap);
    }

    /**
     * {@inheritDoc}
     */
    public BufferingRequest createBufferingRequest(Service service, long minDuration, long maxDuration,
            ExtendedFileAccessPermissions efap, CallerContext cctx)
    {
        if (cctx == null)
        {
            throw new IllegalArgumentException("CallerContext must not be null.");
        }

        return m_buffReqManager.createBufferingRequest(service, minDuration, maxDuration, efap, cctx);
    }

    /**
     * Timer specification used to signal recordings waiting for recording to 
     * become enabled system-wide
     */
    class RecordingDelayTimer extends TVTimerSpec implements TVTimerWentOffListener
    {
        TVTimerSpec retSpec = null;

        TVTimer timer = m_scheduler.getSystemTimer();

        RecordingDelayTimer()
        {
            super.addTVTimerWentOffListener(this);
        }

        public void scheduleRecordingDBStartTrigger() throws TVTimerScheduleFailedException
        {
			// Added synchronization block for findbugs issues fix
        	synchronized (this)
            {
        	    retSpec = timer.scheduleTimerSpec(this);
            }
        }

        /**
         * Cancel this rec db start trigger.
         */
        public void deschedule()
        {
            synchronized (this)
            {
                if (retSpec != null)
                {
                    timer.deschedule(retSpec);
                    retSpec = null;
                }
                m_triggerEnabled = false;
            }
        }

        public void timerWentOff(TVTimerWentOffEvent ev)
        {
            synchronized (this)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("timerWentOff: Recording enabled timer expired.");
                }
                if (m_triggerEnabled)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("timerWentOff: Recording enabled timer: Notifying RecordingDisabledListeners...");
                    }
					// Added synchronization block for findbugs issues fix
                    synchronized(RecordingManagerImpl.this)
                    {
                        m_recordingEnabled = true;
                    }
                    notifyRecordingDisabledChangeAsync();
                }
            }
        }

        private boolean m_triggerEnabled = true;
    }

    /**
     * Implements the boot process state listener
     */
    class BootProcessCallbackListener implements ServicesDatabase.BootProcessCallback
    {
        /**
         * Upon shutdown notification, all active recordings should be
         * synchronously transitioned into a disabled state
         */
        public boolean monitorApplicationShutdown(ServicesDatabase.ShutdownCallback cb)
        {
            synchronized (this)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("BootProcessCallback - received system shutdown indication");
                }
                // If there is an outstanding trigger, cancel it now
				// Added code for caching for findbugs issues fix
                RecordingDelayTimer l_recDBStartTrigger;
                synchronized(RecordingManagerImpl.this)
                {
                	l_recDBStartTrigger = m_recordingDelayTimer;
                }
                if (l_recDBStartTrigger != null)
                {
                	l_recDBStartTrigger.deschedule();
                }
                // Disable the schedule and shutdown ongoing recordings
                synchronized(RecordingManagerImpl.this)
                {
                if (m_recordingEnabled == true)
                {
                    m_recordingEnabled = false;
                    notifyRecordingDisabledChangeSync();
                }
	            }
            }
            // return false to indicate that our clean has been performed
            // synchronously
            return false;
        }

        /**
         * from ServicesDatabase.BootProcessCallback interface. When notified
         * that the reboot process has been completed, enable recording
         * according to the delay set through setRecordingDelay()
         */
        public void monitorApplicationStarted()
        {
            synchronized (this)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("BootProcessCallback.started: received system started indication");
                }
                // No delay set for schedule start. Attempt to load persistent
                // recordings
                if (m_recordingDelay == 0)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("BootProcessCallback.started: No recording delay set. Enabling system-wide recording...");
                    }
                    m_recordingEnabled = true;
                    notifyRecordingDisabledChangeAsync();
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("BootProcessCallback.started: Recording delay set. Scheduling timer for " + m_recordingDelay + 's');
                    }

                    // instantiate a new timer trigger and set for current time plus
                    // timeout
                    m_recordingDelayTimer = new RecordingDelayTimer();
                    m_recordingDelayTimer.setAbsoluteTime(System.currentTimeMillis() + m_recordingDelay * 1000);

                    // attempt to schedule the start trigger
                    try
                    {
                        m_recordingDelayTimer.scheduleRecordingDBStartTrigger();
                    }
                    catch (Exception e)
                    {
                        SystemEventUtil.logRecoverableError(e);
                        return;
                    }
                }
            } // END synchronized (this)
        } // END started()
        
        /**
         * Called after unbound autostart apps have been started
         */
        public void initialUnboundAutostartApplicationsStarted()
        {
            // Nothing to do (only care about monitor app startup)
        }
    } // END class BootProcessCallbackListener

    /**
     * This method is used to calculate the expiration time to be scheduled from
     * the expiration period set in the recording properties. This also rounds
     * off the value to Long.MAX_VALUE (max positive value possible) for any
     * overflow of values that happen on such a conversion
     * 
     * The time calculation is (expireTimeInSeconds * 1000) + requestedStartTime
     * 
     * @param expirationTimeInSeconds
     *            - the expiration time from the recording properties
     * @param requestedStartTime
     *            - the start time of the recording
     * 
     * @return - positive value in case there is no overflow on time
     *         calculation; - Long.MAX_VALUE in case the above time calculation
     *         returns -ve value which indicates an overflow
     */
    static long roundOffOverflow(long expireTimeInSeconds, long requestedStartTime)
    {
        long tempExpirationPeriod = expireTimeInSeconds * 1000;
        return (tempExpirationPeriod < 0) || ((tempExpirationPeriod += requestedStartTime) < 0) 
               ? Long.MAX_VALUE
               : tempExpirationPeriod; // prevent overflow
    }

    /**
     * This interface should be implemented by entities in the OCAP stack that
     * wish to be notified of recording enabled/disabled status changes. See OCAP
     * DVR ECN-856
     * 
     */
    public interface RecordingDisabledListener
    {
        /**
         * Notfiy this listener that the recording enabled state of the stack has
         * been updated. If the state is disabled, this listener should tear
         * down any active recording processes
         * 
         * This method will be called asyncronously from the recording manager
         * implementation.
         * 
         * @param enabled
         *            true if the schedule is enabled, false if disabled
         * 
         */
        public void notifyRecordingEnabledStateChange(boolean enabled);
    }

    /**
     * Notifies the registered listeners when the recorded content is played
     * back
     * 
     * @param serviceContext
     * @param artificialCarouselId
     * @param pids
     */
    public void notifyPlayBackStart(final ServiceContext serviceContext, final int artificialCarouselId,
            final int[] pids)
    {

        CallerContext cc = ((ServiceContextExt) serviceContext).getCallerContext();
        final CCData data = getCCData(cc);

        if ((data != null) && (data.listeners != null))
        {
            cc.runInContext(new Runnable()
            {
                public void run()
                {
                    data.listeners.notifyRecordingPlayback(serviceContext, artificialCarouselId, pids);
                }
            });
        }

    }

    /**
     * Per caller context data
     */
    private class CCData implements CallbackData
    {
        /**
         * The listeners is used to keep track of all objects that have
         * registered to be notified of storage manager events.
         */
        public volatile RecordingPlaybackListener listeners;

        // Definition copied from CallbackData
        public void active(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void pause(CallerContext cc)
        {
        }

        // Definition copied from CallbackData
        public void destroy(CallerContext cc)
        {
            synchronized (m_recPbackLock)
            {
                // Remove this caller context from the list then throw away
                // the CCData for it.
                if (log.isDebugEnabled())
                {
                    log.debug("Entering in to Caller context Destruction");
                }
                ccList = CallerContext.Multicaster.remove(ccList, cc);
                cc.removeCallbackData(m_recPbackLock);
                listeners = null;
            }
        }
    }

    /**
     * Retrieve the caller context data (CCData) for the specified caller
     * context. Create one if this caller context does not have one yet.
     * 
     * @param cc
     *            the caller context whose data object is to be returned
     * @return the data object for the specified caller context
     */
    private CCData getCCData(CallerContext cc)
    {
        synchronized (m_recPbackLock)
        {
            // Retrieve the data for the caller context
            CCData data = (CCData) cc.getCallbackData(m_recPbackLock);

            // If a data block has not yet been assigned to this caller context
            // then allocate one and add this caller context to ccList.
            if (data == null)
            {
                data = new CCData();
                cc.addCallbackData(data, m_recPbackLock);
                ccList = CallerContext.Multicaster.add(ccList, cc);
            }
            return data;
        }
    }

    /**
     * Native method to retieve the list on native recordings
     * 
     * @param recList
     *            vector which is populated by this native call
     */
    public native void nRetrieveRecordingList(int nativeStorageHandle, Vector recList);

    /**
     * Native method to get the amount of time in ms that it takes to ready all
     * DVR devices that have been placed in a low power mode.
     */
    public native int nGetLowPowerResumeTime();

    /**
     * Native method to cause DVR devices to resume from low power mode.
     */
    native int nResumeFromLowPower();

    /**
     * Native method to get the maximum bit rate.
     */
    private native long nGetMaxBitRate();

    /**
     * Native method to get the smallest time shift duration.
     */
    private native long nGetSmallestTimeShiftDuration();

    /**
     * Reference to the current Space Allocation Handler
     */
    private AllocationHandler m_allocationhandler = null;

    /**
     * Reference to the NavigationManager singleton
     */
    private NavigationManager m_navigationManager = null;

    /**
     * Reference to the Scheduler singleton
     */
    private Scheduler m_scheduler = null;

    /**
     * Reference to the recording resource manager
     */
    private RecordingResourceManager m_resourceManager = null;

    /**
     * Reference to the recording retention manager
     */
    private RecordingRetentionManager m_retentionManager = null;

    /*
     * Reference to the Caller Context Manager
     */
    private CallerContextManager m_callerContextManager = null;

    /**
     * Reference to the NetworkInterfaceManager for NI resource offers.
     */
    private NetworkInterfaceManagerImpl m_networkInterfaceManager = null;

    /**
     * Default internal storage device
     */
    private StorageProxy m_storageProxy = null;

    /**
     * Has our internal recording database been loaded?
     */
    private boolean m_recdbLoaded = false;

    /**
     * 
     */
    protected static final Object m_recPbackLock = new Object();

    /**
     * List of listeners to be notified of buffering enable/diable state changes
     */
    private final HashSet m_bufferingDisableListenerList = new HashSet();

    /**
     * List of listeners to be notified of buffering enable/diable state changes
     */
    private Vector m_scheduleDisableListenerList = new Vector();

    /**
     * indicates whether buffering is currently enabled or disabled
     */
    private boolean m_bufferingEnabled = true;

    /**
     * Indicates whether signalRecordingStart has been called or the recording
     *  delay timer has expired
     */
    private boolean m_recordingEnabled = false;

    /**
     * Handles the timer callback for delayed recording start
     */
    private RecordingDelayTimer m_recordingDelayTimer = null;

    /**
     * the recording delay. Defaults to 0 unless set via ORM.setRecordingDelay()
     */
    private long m_recordingDelay = 0;

    /**
     * Persistent storage manager
     */
    protected RecordingDBManager m_rdbm = (RecordingDBManager) ManagerManager.getInstance(RecordingDBManager.class);

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(RecordingManagerImpl.class.getName());

    // Feature 0.9
    Hashtable m_reqRezHandlers = new Hashtable();

    /*
     * This is the list of orphaned (legacy) recordings that need to be
     * upgraded/converted to OCAP recordings.
     */
    private Vector orphanList;

    /**
     * Time tolerance when checking actual duration against recorded duration
     * (in milliseconds)
     */
    protected static int m_recLengthTolerance;

    /**
     * Minimum amount of time that a BeforeStartRecordingListener can 
     * precede recording start (in milliseconds)
     */
    protected static long m_minimumBeforeStartNotificationIntervalMs;
    
    /*
     * This is the 'flag' file that indicates that we are in the default
     * recording management mode, and not in legacy recording upgrade mode. This
     * is the same default directory used by SerializationMgr.
     */
    private static final String DEFAULT_DIR = "/syscwd";

    private static final String DEFAULT_FILENAME = "DefRec";

    protected static final String BASEDIR_PROP = "OCAP.persistent.dvr";

    protected static final String DEFAULT_SUBDIR = "recmgrconfig";

    /**
     * This flag indicates that the host supports host-bound content
     */
    private boolean m_hostBoundContentFlag;
    
    /**
     * Manages creation and storing of BufferingRequest objects. An iTSB
     * feature.
     */
    private BufferingRequestManager m_buffReqManager = new BufferingRequestManager();

    /**
     * Multicast list of caller context objects for tracking listeners for this
     * Recording Playback. At any point in time this list will be the complete
     * list of caller context objects that have an assigned CCData.
     */
    volatile CallerContext ccList = null;

    private static final SimpleCondition m_initializationCompleteCondition = new SimpleCondition(false);
}
