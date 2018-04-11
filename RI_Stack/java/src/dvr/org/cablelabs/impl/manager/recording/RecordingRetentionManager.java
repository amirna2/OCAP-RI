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

import org.apache.log4j.Logger;
import org.ocap.dvr.storage.*;
import org.ocap.shared.dvr.AccessDeniedException;
import org.ocap.shared.dvr.RecordingFailedException;
import org.ocap.dvr.OcapRecordedService;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageManagerEvent;
import org.ocap.storage.StorageManagerListener;
import org.ocap.storage.StorageOption;
import org.ocap.storage.StorageProxy;

import org.cablelabs.impl.manager.DVRStorageManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.RecordingManager;
import org.cablelabs.impl.util.MPEEnv;
import org.cablelabs.impl.util.SystemEventUtil;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

/**
 * The recording retention manager maintains the list of purgable recordings,
 * where a purgable recording is a recording that has passed it's expiration
 * period but has not yet been removed from the database. In addition, the
 * retention manager maintains the list of ongoing recordings, and, if
 * necessary, will remove purgable recordings from the disk (according to their
 * retention priority) in order to provide sufficient space.
 */
public class RecordingRetentionManager implements StorageManagerListener
{
    static RecordingFailedException RFE = new RecordingFailedException();

    /**
     * Singleton implementation
     * 
     * @return the single instance of the recording retention manager
     */
    static synchronized RecordingRetentionManager getInstance()
    {
        if (m_instance == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("Creating RecordingRetentionManager");
            }

            m_dvrStorageMgr = (org.cablelabs.impl.manager.DVRStorageManager)
                              ManagerManager.getInstance(org.cablelabs.impl.manager.StorageManager.class);
            m_storageManager = StorageManager.getInstance();
            m_recordingManager = (org.cablelabs.impl.manager.RecordingManager) 
                                 ManagerManager.getInstance(org.cablelabs.impl.manager.RecordingManager.class);
            m_instance = new RecordingRetentionManager();
        }
        return m_instance;
    }

    /**
     * Expose the internal sync object to allow RecordingManager to implement a
     * single-lock implementation.
     */
    void setSyncObject(Object sync)
    {
        // m_sync = sync;
    }

    /**
     * Adds <i>recording</i> to the list of on-going recordings, and checks the
     * estimated disk available for the entire list. The state of each recording
     * already in the list should be updated to reflect the estimated remaining
     * space (IN_PROGRESS_STATE or IN_PROGRESS_INSUFFICIENT_SPACE_STATE).
     * 
     * If it is determined that there may be insufficient space to complete this
     * recording this method should attempt to remove any purgable recordings
     * from the disk in order to free up sufficient space.
     * 
     * @param recording
     *            the recording which is transitioning to an in-progress state
     *            this method should not update the state of the recording
     *            passed in.
     * @return true if, after processing, it is expected that this recording
     *         will complete with sufficient space. False if the implementation
     *         was not able to free up sufficient space.
     */
    boolean addInProgressRecording(RecordingImplInterface recording)
    {
        if (log.isInfoEnabled())
        {
            log.info("Adding In Progress Recording: " + recording.toString() + ": Using "
                    + recording.getSpaceRequired() + "bytes");
        }

        // Get the volume.
        MediaStorageVolume volume = recording.getVolume();

        // Get the in progress list for this MSV
        Vector ipList = getInprogressList(volume);

        // If the recording is already here, punt.
        if (ipList.contains(recording))
        {
            if (log.isDebugEnabled())
            {
                log.debug("Recording already registered");
            }
            return true;
        }
        // Now that we know how much space is available, let's add myself to the
        // list.
        ipList.add(recording);

        // First check if the recording will be IN_PROGRESS_INCOMPLETE
        // if so, ignore the MSV and just notify of the change.
        int failedReason = recording.getRecordingInfo().getFailedExceptionReason();
        if ((failedReason != RFE.REASON_NOT_KNOWN) || false == recording.verifyStartTimeWithinTolerance())
        {
            if (log.isDebugEnabled())
            {
                log.debug("Recording was in error or started late");
            }
            recording.setInProgressSpaceState(LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE);
        }

        return evaluateAvailableSpace(volume);
    }

    /**
     * Removes <i>recording</i> from the list of on-going recordings, and checks
     * the estimated disk available for the entire list. The state of each
     * recording in the list should be updated to reflect the estimated
     * remaining space (IN_PROGRESS_STATE or
     * IN_PROGRESS_INSUFFICIENT_SPACE_STATE).
     * 
     * @param recording
     *            the recording which is transition from an in-progress state
     */
    void removeInProgressRecording(RecordingImplInterface recording)
    {
        if (log.isInfoEnabled())
        {
            log.info("Removing in progress recording " + recording.toString());
        }
        // Get the stats of what's what.
        MediaStorageVolume volume = recording.getVolume();

        // Remove the recording.
        Vector ipList = getInprogressList(volume);
        ipList.removeElement(recording);

        // Now, reevaluate the list of recordings;
        evaluateAvailableSpace(volume);
    }

    /**
     * Adds <i>recording</i> to the list of purgable recordings - ie, recordings
     * whose expiration has passed and are available for deletion if space is
     * required for other uses.
     * 
     * @param recording
     *            the recording which is now eligable for removal
     */
    void insertPurgableRecording(RecordingImplInterface recording)
    {
        if (log.isInfoEnabled())
        {
            log.info("insertPurgableRecording " + recording.toString());
        }
        MediaStorageVolume msv = recording.getVolume();
        Vector pList = getPurgableList(msv);
        if (pList.contains(recording))
        {
            return;
        }
        else
        {
            pList.add(recording);
            evaluateAvailableSpace(msv);
        }
    }

    /**
     * Removes <i>recording</i> from the list of purgable recordings. This
     * indicates that the recording in question is no longer eligable purging
     * (ie, is being deleted for other reasons)
     * 
     * @param recording
     *            the recording which is no longer available for removal
     */
    void removePurgableRecording(RecordingImplInterface recording)
    {
        if (log.isInfoEnabled())
        {
            log.info("removePurgableRecording " + recording.toString());
        }
        MediaStorageVolume msv = recording.getVolume();
        Vector pList = getPurgableList(msv);
        pList.removeElement(recording);
        // TODO: Do we need to evaluate the recordings here?
    }

    /**
     * Attempts to free disk space by removing purgable recordings from the disk
     * according to the available recording's retention policy
     * 
     * @param msv
     *            storage volume on which to free disk space
     * @param size
     *            the amount of disk space requested for removal
     * @return the amount of space actually freed
     */
    long freeDiskSpace(MediaStorageVolume msv, long size)
    {
        if (msv == null)
        {
            RecordingManagerImpl rmi = (RecordingManagerImpl) m_recordingManager;
            msv = rmi.getDefaultMediaStorageVolume();
        }
        Vector pList = getPurgableList(msv);
        if (log.isInfoEnabled())
        {
            log.info("Attempting to free " + size + " bytes of purgable recordings");
        }
        long freedSize = 0;
        // First, sort the list.
        Collections.sort(pList);
        // Now, grab a list of the elements. Clone the original, as it can be
        // changed underneath us.
        Vector tempList = (Vector) pList.clone();
        Iterator elements = tempList.iterator();

        if (log.isDebugEnabled())
        {
            log.debug("Checking " + pList.size() + " recordings to purge");
        }
        while (freedSize < size && (elements.hasNext()))
        {
            RecordingImplInterface rec = (RecordingImplInterface) elements.next();
            if (!rec.isPresenting())
            {
                if (log.isInfoEnabled())
                {
                    log.info("Purging recording " + rec.toString() + " to free space");
                }
                try
                {
                    RecordedServiceImpl rs = (RecordedServiceImpl) rec.getService();

                    // It is possible for recordings to have been expired prior
                    // to
                    // physical recording creation. Verify that a service is
                    // present
                    if (rs == null)
                    {
                        continue;
                    }

                    long recSize = rs.getRecordedSize();
                    // Delete the sucker. We delete the underlying service, not
                    // the whole
                    // recording. This will actually remove the element from the
                    // purgable list.
                    rs.purgeExpiredRecording();

                    freedSize += recSize;
                }
                catch (IllegalStateException e)
                {
                    // this is an expected exception for purged but not
                    // in-progress recordings
                }
                catch (Exception e)
                {
                    if (log.isWarnEnabled())
                    {
                        log.warn("Unable to free up recording " + rec.toString(), e);
                    }
                }
            }
        }

        return freedSize;
    }

    /**
     * Checks the available disk space and available purgable recordings to
     * determine if space could be made to support an allocation of "size"
     * bytes.
     * 
     * @param msv
     *            storage volume on which to free disk space
     * @param size
     *            the amount of disk space requested for removal
     * @return true if "size" space could be made available by this call
     */
    boolean checkDiskSpace(MediaStorageVolume msv, long size)
    {
        if (msv == null)
        {
            RecordingManagerImpl rmi = (RecordingManagerImpl) m_recordingManager;
            msv = rmi.getDefaultMediaStorageVolume();
        }
        // Get the amount of unaccounted for space no the drive.
        long spaceAvail = getAvailableDiskSpace(msv);
        // Short cut out if we already have the space.
        if (spaceAvail >= size)
        {
            return true;
        }
        // Get the purgable list for this MSV
        Vector pList = getPurgableList(msv);
        // Figure out how much space is available in the purgable recordings
        // list.
        for (int i = 0; i < pList.size(); i++)
        {
            RecordingImplInterface r = (RecordingImplInterface) pList.get(i);
            if (!r.isPresenting())
            {
                spaceAvail += r.getSpaceRequired();
            }
        }
        // And return whether or not we have enough space available.
        return (spaceAvail >= size);
    }

    /**
     * Notifies the RecordingRetentionManager that space is available on the
     * designated MediaStorageVolume (for example, due to allocation of a time
     * shift buffer). This will cause the on-going recording list to be
     * re-evaluated for sufficient space.
     * 
     * @param msv
     *            the media volume with available space
     */
    void notifySpaceAvailable(MediaStorageVolume msv)
    {
        if (msv == null)
        {
            RecordingManagerImpl rmi = (RecordingManagerImpl) m_recordingManager;
            msv = rmi.getDefaultMediaStorageVolume();
        }
        if (evaluateAvailableSpace(msv))
        { // We'll only notify if space is, or was made, available
            m_dvrStorageMgr.notifyMediaVolumeSpaceAvailable(msv);
        }
    }
    /**
     * Calculate the amount of space available on the disk, that's not either
     * used, or preallocated for other in progress recordings. If the value
     * returned is negative, this indicates that we are overcommited by that
     * much disk space.
     * 
     * @param msv
     *            The volume we care about.
     * @return Amount of space available.
     */
    long getAvailableDiskSpace(MediaStorageVolume msv)
    {
        // First , get the amount of space not used at all.
        long unusedSpace = msv.getFreeSpace();
        // Next, get the amount of space that's in the list.
        long allocatedSpace = getUnusedInprogressSpace(msv);

        return unusedSpace - allocatedSpace;
    }

    /**
     * Compute the amount of space that we can expect to use in recordings which
     * are already in progress. This is only the amount of space that they still
     * expect to use, not the amount of space that they've already consumed.
     * TODO: This is currently calculated as the amount ef space required minus
     * the amount of space used. This should probably be changed to be the
     * amount of space required times the percentage of the recording remaining.
     * Will more accurately reflect the amount of space that will be needed.
     * 
     * @param msv
     *            The MediaStorageVolume that we're interested in.
     * @return The amount of space we still expect to use.
     */
    private long getUnusedInprogressSpace(MediaStorageVolume msv)
    {
        long space = 0;
        Vector ipList = getInprogressList(msv);
        Iterator enu = ipList.iterator();

        while (enu.hasNext())
        {
            RecordingImplInterface r = (RecordingImplInterface) enu.next();
            long spaceAllocated = r.getSpaceRequired();
            int state = r.getState();
            // If the recording is still pending, we're right at the start
            // and we can't do a getService() call, so we'll just claim that the
            // space will be the full required space.
            if (state == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE
                    || state == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)
            {
                space += spaceAllocated;
            }
            else
            {
                try
                {
                    OcapRecordedService rs = (OcapRecordedService) r.getService();

                    // verify a valid service
                    if (rs == null)
                    {
                        continue;
                    }

                    long spaceUsed = rs.getRecordedSize();
                    // Add in the amount of space that we want, but that we
                    // haven't
                    // allocated yet.
                    // Don't count the space used, as that's already in the
                    // MediaStorageVolume.getSpace()
                    // number.
                    space += (spaceAllocated - spaceUsed);
                }
                catch (IllegalStateException e1)
                {
                    // we expect this exception for expired but not started
                    // recordings
                }
                catch (AccessDeniedException e1)
                {
                    SystemEventUtil.logRecoverableError(e1);
                }
            }
        }
        return space;
    }

    /**
     * Evaluate the amount of space available on a MediaStorageVolume, and if
     * it's over committed, attempt to free up any purged recordings. Set the
     * state of all in progress recordings appropriately.
     * 
     * @param volume
     *            The volume to check.
     * @return true if there was enough space, or if enough space could be made
     *         available, false if still over committed.
     */
    private boolean evaluateAvailableSpace(MediaStorageVolume volume)
    {
        // Compute the amount of space which is not already accounted for.
        long spaceAvailable = getAvailableDiskSpace(volume);
        
        // Determine the MSV's capacity/allocation
        long msvCapacity = volume.getAllocatedSpace();
        if (msvCapacity == 0)
        {
            final StorageOption msos[] = volume.getStorageProxy().getOptions();
            for (int i=0; i<msos.length; i++)
            {
                if (msos[i] instanceof MediaStorageOption)
                {
                    msvCapacity = ((MediaStorageOption)msos[i]).getTotalMediaStorageCapacity();
                    break;
                }
            }
        }
        
        final long minSpaceToMaintain = (long)( m_msvSpaceCheckThreshold/100F
                                                * (float) msvCapacity ) + 1;

        // If we have enough space, great, get out.
        if ((spaceAvailable >= 0) && (volume.getFreeSpace() >= minSpaceToMaintain))
        {
            if (log.isDebugEnabled())
            {
                log.debug("evaluateAvailableSpace: Sufficient space available.  Not purging");
            }
            setInProgressRecordingStates(volume, LeafRecordingRequest.IN_PROGRESS_STATE);
            return true;
        }

        // Ok, we didn't have enough space. Free some of that stuff up.
        final long spaceToFree = Math.max( -spaceAvailable, 
                                           (minSpaceToMaintain-volume.getFreeSpace()) );
        final long freedSpace = freeDiskSpace(volume, spaceToFree);

        // If we got enough back, say yes, otherwise, uh, uh-oh.
        if (freedSpace >= spaceToFree)
        {
            if (log.isDebugEnabled())
            {
                log.debug("evaluateAvailableSpace: Successfully freed " + freedSpace + " bytes.");
            }
            setInProgressRecordingStates(volume, LeafRecordingRequest.IN_PROGRESS_STATE);
            return true;
        }
        else
        {
            // Anything I should do here?
            if (log.isInfoEnabled())
            {
                log.info("evaluateAvailableSpace: Unable to free sufficient space.  Needed "
                         + spaceToFree + "bytes, freed " + freedSpace + " bytes.");
            }
            setInProgressRecordingStates(volume, LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE);
            return false;
        }
    }

    /**
     * Set the state of all recordings in the inprogress list to the given
     * state.
     * 
     * @param volume
     *            The volume to check for in progress recordings.
     * @param state
     *            The state to set those recordings to.
     */
    private void setInProgressRecordingStates(MediaStorageVolume volume, int state)
    {
        Vector ipList = getInprogressList(volume);
        Iterator recs = ipList.iterator();
        while (recs.hasNext())
        {
            RecordingImplInterface recording = (RecordingImplInterface) recs.next();
            // The inprogress_incomplete recordings will not shift to and from
            // the
            // in_progress_insufficient_space state based on addition and
            // removal of
            // inprogress recordings, so ignore them.
            if (recording.getState() == LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE) continue;
            recording.setInProgressSpaceState(state);
        }
    }

    /**
     * Get the purgable list corresponding to a MediaStorageVolume.
     * 
     * @param msv
     *            The MediaStorageVolume to get the list for.
     * @return The purgable list for this msv.
     */
    private Vector getPurgableList(MediaStorageVolume msv)
    {
        return getVector(msv, m_purgableRecordings);
    }

    /**
     * Get the InProgress list for a MediaStorageVolume.
     * 
     * @param msv
     *            The MediaStorageVolume to get the list for.
     * @return The purgable list for this msv.
     */
    private Vector getInprogressList(MediaStorageVolume msv)
    {
        return getVector(msv, m_inProgressRecordings);
    }

    /**
     * Get a vector from a hash table. If it doesn't exist, create it.
     * 
     * @param msv
     *            The "key".
     * @param hash
     *            The hash table.
     * @return The vector.
     */
    private Vector getVector(MediaStorageVolume msv, Hashtable hash)
    {
        Vector vec = (Vector) hash.get(msv);
        if (vec == null)
        {
            vec = new Vector();
            hash.put(msv, vec);
        }
        return vec;
    }

    /**
     * Create a new recording retention manager.
     */
    private RecordingRetentionManager()
    {
        m_purgableRecordings = new Hashtable();
        m_inProgressRecordings = new Hashtable();
        m_msvListeners = new Hashtable();
        
        // If set, we're going to watch all MSVs and attempt to purge when they 
        //  hit their space threshold
        m_msvSpaceCheckThreshold = MPEEnv.getEnv("OCAP.dvr.recording.retention.FreeSpaceThresholdPercent", 0);
        
        if (m_msvSpaceCheckThreshold != 0)
        {
            if (log.isInfoEnabled())
            {
                log.info( "RetentionManager free space threshold set to " 
                        + m_msvSpaceCheckThreshold + '%' );
            }
            m_storageManager.addStorageManagerListener(this);
            
            // Enumerate all MSVs on all SPs and register listeners
            final StorageProxy sp[] = m_storageManager.getStorageProxies();
            for (int i=0; i<sp.length; i++)
            {
                registerMSVListenersForStorageProxy(sp[i]);
            }
        }
        else
        {
            if (log.isInfoEnabled())
            {
                log.info( "RetentionManager free space threshold disabled. " 
                          + m_msvSpaceCheckThreshold + '%' );
            }
        }
    }
    
    private void registerMSVListenersForStorageProxy(final StorageProxy sp)
    {
        if (log.isDebugEnabled())
        {
            log.debug( "Registering FreeSpaceListeners for all MSVs in " + sp);
        }
        final LogicalStorageVolume lsv[] = sp.getVolumes();
        for (int j=0; j<lsv.length; j++)
        {
            if (lsv[j] instanceof MediaStorageVolume)
            {
                final MediaStorageVolume msv = (MediaStorageVolume)lsv[j];
                registerMSVListener(msv);
            }
        }
    }

    private void unregisterMSVListenersForStorageProxy(final StorageProxy sp)
    {
        final LogicalStorageVolume lsv[] = sp.getVolumes();
        for (int j=0; j<lsv.length; j++)
        {
            if (lsv[j] instanceof MediaStorageVolume)
            {
                final MediaStorageVolume msv = (MediaStorageVolume)lsv[j];
                unregisterMSVListener(msv);
            }
        }
    }

    class MSVFreeSpaceListener implements FreeSpaceListener
    {
        private final MediaStorageVolume m_msv;
        MSVFreeSpaceListener(MediaStorageVolume msv)
        {
            m_msv = msv;
        }
        
        public void notifyFreeSpace()
        {
            if (log.isDebugEnabled())
            {
                log.debug("MSVFreeSpaceListener: Received free space notification on " + m_msv);
            }
            evaluateAvailableSpace(m_msv);
        }
        
        public String toString()
        {
            return "MSV listener for " + m_msv;
        }
    }
    
    private void registerMSVListener(final MediaStorageVolume msv)
    {
        // The MSV notification doesn't include an MSV reference.
        //  So we need one listener per MSV
        MSVFreeSpaceListener msvl = new MSVFreeSpaceListener(msv);
        msv.addFreeSpaceListener(msvl,m_msvSpaceCheckThreshold);
        m_msvListeners.put(msv,msvl);
        if (log.isDebugEnabled())
        {
            log.debug( "Registered " + msvl);
        }
    }

    private void unregisterMSVListener(final MediaStorageVolume msv)
    {
        final MSVFreeSpaceListener msvl = (MSVFreeSpaceListener)
                                          m_msvListeners.remove(msv);
        if (msvl != null)
        {
            msv.removeFreeSpaceListener(msvl);
            
            if (log.isDebugEnabled())
            {
                log.debug( "Unregistered " + msvl);
            }
        }
    }
    
    private static RecordingRetentionManager m_instance;

    private static RecordingManager m_recordingManager;

    private static DVRStorageManager m_dvrStorageMgr;

    private static StorageManager m_storageManager;

    // Log4J Logger
    private static final Logger log = Logger.getLogger(RecordingRetentionManager.class.getName());

    // private Object m_sync;
    private Hashtable m_purgableRecordings;

    private Hashtable m_inProgressRecordings;
    
    private final int m_msvSpaceCheckThreshold;

    private Hashtable m_msvListeners;

    /**
     * {@inheritDoc}
     */
    public void notifyChange(StorageManagerEvent sme)
    {
        // Assert: m_msvSpaceCheckThreshold != 0
        
        final StorageProxy sp = sme.getStorageProxy();
        switch (sme.getEventType())
        {
            case StorageManagerEvent.STORAGE_PROXY_ADDED:
            {
                registerMSVListenersForStorageProxy(sp);
                break;
            }
            case StorageManagerEvent.STORAGE_PROXY_REMOVED:
            {
                unregisterMSVListenersForStorageProxy(sp);
                break;
            }
            case StorageManagerEvent.STORAGE_PROXY_CHANGED:
            {
                unregisterMSVListenersForStorageProxy(sp);
                registerMSVListenersForStorageProxy(sp);
                break;
            }
        }
    }
}
