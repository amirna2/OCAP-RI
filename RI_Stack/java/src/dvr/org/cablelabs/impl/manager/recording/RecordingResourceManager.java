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
import java.util.ArrayList;
import java.util.Vector;

import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.SIManager;
import javax.tv.service.Service;

import org.apache.log4j.Logger;
import org.davic.net.tuning.NetworkInterface;
import org.davic.net.tuning.NetworkInterfaceManager;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.OcapRecordingRequest;
import org.ocap.dvr.RecordingAlertEvent;
import org.ocap.dvr.RecordingAlertListener;
import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.dvr.storage.MediaStorageOption;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.LocatorRecordingSpec;
import org.ocap.shared.dvr.RecordingManager;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.RecordingSpec;
import org.ocap.shared.dvr.ServiceContextRecordingSpec;
import org.ocap.shared.dvr.ServiceRecordingSpec;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingStateFilter;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageManager;
import org.ocap.storage.StorageOption;
import org.ocap.storage.StorageProxy;

import org.cablelabs.impl.ocap.resource.DVRSharedResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.ocap.resource.SharedResourceUsageImpl;
import org.cablelabs.impl.davic.net.tuning.NetworkInterfaceImpl;
import org.cablelabs.impl.debug.Assert;
import org.cablelabs.impl.debug.Asserting;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.ResourceManager;
import org.cablelabs.impl.manager.TimeShiftManager;
import org.cablelabs.impl.manager.resource.NotifySetWarningPeriod;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * The RecordingResourceManager encapsulates resource management and conflcit
 * detection
 */
public class RecordingResourceManager implements RecordingAlertListener, NotifySetWarningPeriod,
                                                 Asserting
{

    /**
     * @author jspruiel
     * 
     */
    private class ConflictResults
    {
        public boolean conflictDetected = false;

        ResourceUsage requestedResourceUsage;

        ArrayList currentReservations;

        ConflictResults()
        {
            currentReservations = new ArrayList();
        }
    }

    class IntermediateResults
    {
        boolean inNonEmptyList;

        boolean supported;

        Vector locationOfNewRequest;
    }

    RecordingResourceManager()
    {
        m_networkInterfaceCount = NetworkInterfaceManager.getInstance().getNetworkInterfaces().length;
    }

    static synchronized RecordingResourceManager getInstance()
    {
        if (m_instance == null)
        {
            m_instance = createInstance();
            ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);
            rm.registerWarningPeriodListener(m_instance);
        }
        return m_instance;
    }

    static RecordingResourceManager createInstance()
    {
        return new RecordingResourceManager();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.cablelabs.impl.manager.resource.NotifySetWarningPeriod#
     * notifySetWarningPeriod(int)
     */
    public void notifySetWarningPeriod(int wp)
    {
        if (log.isDebugEnabled())
        {
            log.debug("RecordingResourceManager - notifySetWarningPeriod. wp = " + wp);
        }
        warningPeriod = wp;

        if (log.isDebugEnabled())
        {
            log.debug("RecordingResourceManager - call addBeforeStartListener warning period = " + warningPeriod);
        }
        Scheduler.getInstance().removeListener(this);
        Scheduler.getInstance().addBeforeStartListener(this, warningPeriod);
    }

    /**
     * Expose the internal sync object to allow RecordingManager to implement a
     * single-lock implementation. Will only be called at initialization (so
     * lock wll not be held).
     */
    void setSyncObject(Object sync)
    {
        m_sync = sync;
    }

    /**
     * Determine if the added recording is in conflict with other
     * PENDING_NO_CONFLICT or IN_PROGRESS recordings. If there is a conflict,
     * call the monitor app's ResourceContentionHandler and re-prioritize
     * recordings based on the relative priorities returned by the handler.
     * 
     * Note that resolveConflictsForRecordingAdd may change the state of
     * recordings which overlap with recording. These state changes will be
     * notified. But the the state of the passed recording is expected to be
     * INIT_STATE and the caller is expected to examine the final state of the
     * passed recording and perform appropriately.
     * 
     * @parm recording The recording being added.
     */
    void resolveConflictsForRecordingAdd(RecordingImplInterface recording)
    {
        if (log.isDebugEnabled())
        {
            log.debug("resolveConflictsForRecordingAdd: " + recording);
        }

        final NavigationManager nm = NavigationManager.getInstance();
        final ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);
        ResourceUsage[] overlappingUsages;
        int dbstate, prevdbstate;

        int conflictingRecordingStates[] = new int[] { LeafRecordingRequest.PENDING_NO_CONFLICT_STATE,
                LeafRecordingRequest.IN_PROGRESS_STATE, LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE,
                LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE,
                OcapRecordingRequest.TEST_STATE };

        OrderedRecordingSet overlappingRecordings = null;

        if (ASSERTING) Assert.lockNotHeld(m_sync);
        
        //
        // Check to see if the affected recording causes an over-use
        // of network interfaces
        //
        synchronized (m_sync)
        {
            overlappingRecordings = nm.getOverlappingEntriesInStates(recording, conflictingRecordingStates);

            // overlappingRecording defines the "Sphere of Influence" for
            // the conflict resolution process. i.e. nothing outside the SoI
            // will have its state changed. BUT, things outside the SoI may
            // need to be consulted.

            if (log.isDebugEnabled())
            {
                log.debug("resolveConflictsForRecordingAdd: Overlapping entries");
            }
            if (log.isDebugEnabled())
            {
                log.debug(overlappingRecordings.toString());
            }

            //
            // Check for usage more than m_networkInterfaceCount-1 for all
            // PENDING_NO_CONFLICT and IN_PROGRESS states
            //
            if (!overlappingRecordings.simultaneousUseMoreThan(m_networkInterfaceCount - 1))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("resolveConflictsForRecordingAdd: Recording GOT a tuner reservation");
                }
                // No prob - if this isn't a test recording,
                // set the new one to PENDING_NO_CONFLICT and store state to
                // disk
                if (recording.getPriority() != OcapRecordingProperties.TEST_RECORDING)
                {
                    recording.setStateNoNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
                }
                else
                {
                    recording.setStateNoNotify(OcapRecordingRequest.TEST_STATE);
                }
                recording.saveRecordingInfo(org.cablelabs.impl.manager.RecordingDBManager.STATE);
                return;

            }

            // Assert: Not enough free resources for duration of recording

            //
            // Prepare to call the resource contention handler
            //

            // Note that the recording that we're adding is passed explicitly

            // We'll save away the state counter to determine if the DB changes
            // while we're waiting for the handler to return
            prevdbstate = nm.getStateCounter();

            // Assert: recording conflicts - let the juggling begin...

            overlappingUsages = overlappingRecordings.getRecordingResourceUsages();
        } // END synchronized(m_sync)

        if (ASSERTING) Assert.lockNotHeld(m_sync);
        
        //
        // Call the conflict resolution handler with the overlapping recording
        // entries
        //

        ResourceUsageImpl overlapERUs[] = new ResourceUsageImpl[overlappingUsages.length];
        for (int i = 0; i < overlappingUsages.length; i++)
        {
            overlapERUs[i] = (ResourceUsageImpl) overlappingUsages[i];
        }

        if (log.isDebugEnabled())
        {
            log.debug("resolveConflictsForRecordingAdd: Calling resource contention handler:");
        }
        if (log.isDebugEnabled())
        {
            log.debug("resolveConflictsForRecordingAdd:   newRequest: " + recording.getResourceUsage());
        }
            for (int i = 0; i < overlappingUsages.length; i++)
            {
            if (log.isDebugEnabled())
            {
                log.debug("resolveConflictsForRecordingAdd:   curRes " + i + ": " + overlapERUs[i]);
            }
        }

        // TODO: refactor RecordingListIterator iterator =
        // overlappingRecordings.createRecordingListIterator();
        // TODO: refactor DVRActivityLogger.getInstance().log(recording,
        // iterator);

        // Note: We're not holding the lock while calling back - to allow
        // recordings to be changed inside/during the contention
        ResourceUsage prioritizedERUs[] = rm.prioritizeContention2((ResourceUsageImpl) recording.getResourceUsage(),
                overlapERUs);

        // Assert: The handler has returned and has told us its relative
        // priorities

        if (log.isDebugEnabled())
        {
            log.debug("resolveConflictsForRecordingAdd: Resource Contention Handler returned:");
        }
            for (int i = 0; i < prioritizedERUs.length; i++)
            {
            if (log.isDebugEnabled())
            {
                log.debug("resolveConflictsForRecordingAdd:  Priority " + i + ": " + prioritizedERUs[i]);
            }
        }

        //
        // Now fulfill the resource handler's wishes
        // 
        synchronized (m_sync)
        {
            dbstate = nm.getStateCounter();

            if (dbstate != prevdbstate)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("resolveConflictsForRecordingAdd: Recording DB changed during conflict resolution");
                }
            }

            // Assert: Conflict resolution handler's prioritized list still
            // applies to the overlappingRecordings
            if (log.isDebugEnabled())
            {
                log.debug("resolveConflictsForRecordingAdd:  Recording conflicts - applying resource contention handler priorities");
            }

            // Note that the constructor will filter out any
            // non-RecordingResourceUsage objects - in case the handler
            // decides to do something funny
            OrderedRecordingSet orderedRecordings = new OrderedRecordingSet(prioritizedERUs);

            // The orderedRecordings represent the new Sphere of Influence
            // This *should* be exactly the same SoI as we passed into the
            // handler. But we don't depend on this - we act on the list
            // the RH gave us.

            //
            // Save the state of recordings in the list - so we know
            // what we changed when we're done
            //
            orderedRecordings.saveAllRecordingStates();

            //
            // Now consider everything in the SoI to be PENDING_WITH_CONFLICT
            //
            orderedRecordings.setRecordingStates(LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE, null);

            //
            // Test recordings should not be granted reservations.
            // Prevent attempts to reserve resources for them by transitioning
            // into the TEST_STATE
            //
            OrderedRecordingSet testRecordings = orderedRecordings.getSubsetWithPriority(OcapRecordingProperties.TEST_RECORDING);
            testRecordings.setRecordingStates(OcapRecordingRequest.TEST_STATE, null);

            //
            // Now attempt to move everything in the list to PENDING_NO_CONFLICT
            // in the designated order - as the contention handler prescribed.
            //
            attemptToReserveRecordingsInOrder(orderedRecordings);

            // Recordings which lost reservations and are still pending
            // will be PENDING_WITH_CONFLICT after
            // attemptToReserveRecordingsInOrder()
            // (since they were all PENDING_NO_CONFLICT initially). Note that
            // orderedRecordings that are IN_PROGRESS will be P_W_C before
            // we call processConflictStateTransitions()
            OrderedRecordingSet unreservedRecordings = orderedRecordings.getSubsetWithState(LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE);

            //
            // Attempt to assign any lost reservations to recordings
            // currently w/o a reservation.
            //

            // Note: attemptToReassignLostReservations() operates on recordings
            // which are, by definition, NOT in the orderedRecordings.

            // Note: We do this before processConflictStateTransitions()
            // so that reservations held by IN_PROGRESS recordings can
            // be reassigned (since they temporarily appear as
            // PENDING_WITH_CONFLICT before processConflictStateTransitions()

            if (!unreservedRecordings.isEmpty())
            {
                attemptToReassignLostReservations(unreservedRecordings);
            }

            //
            // Compare the (temporary) current state to the saved state,
            // perform appropriate action, and determine appropriate final
            // state
            //
            processConflictStateTransitions(orderedRecordings);

        } // END synchronized(m_sync)
    } // END resolveConflictsForRecordingAdd()

    /**
     * Determine if the changed recording is in conflict with other
     * PENDING_NO_CONFLICT or IN_PROGRESS recordings. If there is a conflict,
     * call the monitor app's ResourceContentionHandler and re-prioritize
     * recordings based on the relative priorities returned by the handler.
     * 
     * Note that resolveConflictsForRecordingChange may change the state of
     * recordings which overlap with recording. These state changes will be
     * notified as well as any state of the passed recording.
     * 
     * @param recording
     *            The recording being changed. This should already have its
     *            start time and duration updated
     * @param prevStartTime
     *            The previous start time for the recording
     * @param prevDuration
     *            The previous duration for the recording
     */
    void resolveConflictsForRecordingChange(RecordingImplInterface recording, final long prevStartTime,
            final long prevDuration)
    {
        if (log.isDebugEnabled())
        {
            log.debug("resolveConflictsForRecordingChange: " + recording);
        }

        final NavigationManager nm = NavigationManager.getInstance();
        final ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);
        ResourceUsage[] overlappingUsages;
        int dbstate, prevdbstate;

        final int conflictingRecordingStates[] = new int[] { LeafRecordingRequest.PENDING_NO_CONFLICT_STATE,
                LeafRecordingRequest.IN_PROGRESS_STATE, LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE,
                LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE,
                OcapRecordingRequest.TEST_STATE };

        OrderedRecordingSet overlappingRecordings = null;
        
        //
        // Check to see if the affected recording causes an over-use
        // of network interfaces
        //
        synchronized (m_sync)
        {
            overlappingRecordings = nm.getOverlappingEntriesInStates(recording, conflictingRecordingStates);

            // overlappingRecording defines the "Sphere of Influence" for
            // the conflict resolution process. i.e. nothing outside the SoI
            // will have its state changed. BUT, things outside the SoI may
            // need to be consulted. Note that the recording itself is excluded
            // from this set.

            if (log.isDebugEnabled())
            {
                log.debug("resolveConflictsForRecordingChange: Overlapping entries");
            }
            if (log.isDebugEnabled())
            {
                log.debug(overlappingRecordings.toString());
            }

            // Save the current state of recording (used by
            // processConflictStateTransitions())
            recording.saveState();

            //
            // Check for usage more than m_networkInterfaceCount-1 for all
            // PENDING_NO_CONFLICT and IN_PROGRESS states
            //
            if (!overlappingRecordings.simultaneousUseMoreThan(m_networkInterfaceCount - 1))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("resolveConflictsForRecordingChange: Recording GOT a tuner reservation");
                }
                // No prob - if this isn't a test recording,
                // set the new one to PENDING_NO_CONFLICT and store state to
                // disk
                if (recording.getPriority() != OcapRecordingProperties.TEST_RECORDING)
                {
                    recording.setStateNoNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
                }
                else
                {
                    recording.setStateNoNotify(OcapRecordingRequest.TEST_STATE);
                }

                // Will do the right thing for state transition
                processConflictStateTransitions(new OrderedRecordingSet(recording));

                // Now try moving any P_W_C recordings during the old timespan
                // to P_N_C
                attemptToReserveRecordingsDuring(prevStartTime, prevDuration);

                // And we're gone...
                return;
            }

            // Assert: Not enough free resources for duration of recording

            //
            // Prepare to call the resource contention handler
            //

            // Note that the changed recording is passed explicitly. So we don't
            // want
            // to add it to the set

            // We'll save away the state counter to determine if the DB changes
            // while we're waiting for the handler to return
            prevdbstate = nm.getStateCounter();

            // Assert: recording conflicts - let the juggling begin...

            overlappingUsages = overlappingRecordings.getRecordingResourceUsages();
        } // END synchronized(m_sync)

        //
        // Call the conflict resolution handler with the overlapping recording
        // entries
        //
        
        if (ASSERTING) Assert.lockNotHeld(m_sync);        

        ResourceUsageImpl overlapERUs[] = new ResourceUsageImpl[overlappingUsages.length];
        for (int i = 0; i < overlappingUsages.length; i++)
        {
            overlapERUs[i] = (ResourceUsageImpl) overlappingUsages[i];
        }

        if (log.isDebugEnabled())
        {
            log.debug("resolveConflictsForRecordingChange: Calling conflict resolution handler:");
        }
        if (log.isDebugEnabled())
        {
            log.debug("resolveConflictsForRecordingChange:   changedRequest: " + recording.getResourceUsage());
        }
            for (int i = 0; i < overlappingUsages.length; i++)
            {
            if (log.isDebugEnabled())
            {
                log.debug("resolveConflictsForRecordingChange:   curRes " + i + ": " + overlapERUs[i]);
            }
        }

        // TODO: refactor RecordingListIterator iterator =
        // overlappingRecordings.createRecordingListIterator();
        // TODO: refactor DVRActivityLogger.getInstance().log(recording,
        // iterator);

        // Note: We're not holding the lock while calling back - to allow
        // recordings to be changed inside/during the contention
        ResourceUsage prioritizedERUs[] = rm.prioritizeContention2((ResourceUsageImpl) recording.getResourceUsage(),
                overlapERUs);

        // Assert: The handler has returned and has told us its relative
        // priorities

        if (log.isDebugEnabled())
        {
            log.debug("resolveConflictsForRecordingChange: Resource Contention Handler returned:");
        }
            for (int i = 0; i < prioritizedERUs.length; i++)
            {
            if (log.isDebugEnabled())
            {
                log.debug("resolveConflictsForRecordingChange:   Priority " + i + ": " + prioritizedERUs[i]);
            }
        }

        //
        // Now fulfill the resource handler's wishes
        // 
        synchronized (m_sync)
        {
            dbstate = nm.getStateCounter();

            if (dbstate != prevdbstate)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("resolveConflictsForRecordingChange: Recording DB changed during conflict resolution");
                }
            }

            // Assert: Conflict resolution handler's prioritized list still
            // applies to the overlappingRecordings
            if (log.isDebugEnabled())
            {
                log.debug("resolveConflictsForRecordingChange: Recording conflicts - applying conflict resolution handler priorities");
            }

            // Note that the constructor will filter out any
            // non-RecordingResourceUsage objects - in case the handler
            // decides to do something funny
            OrderedRecordingSet orderedRecordings = new OrderedRecordingSet(prioritizedERUs);

            // The orderedRecordings represent the new Sphere of Influence
            // This *should* be exactly the same SoI as we passed into the
            // handler. But we don't depend on this - we act on the list
            // the RH gave us.

            //
            // Save the state of recordings in the list - so we know
            // what we changed when we're done
            //
            orderedRecordings.saveAllRecordingStates();

            //
            // Now consider everything in the SoI to be PENDING_WITH_CONFLICT
            //
            orderedRecordings.setRecordingStates(LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE, null);

            //
            // Test recordings should not be granted reservations.
            // Prevent attempts to reserve resources for them by transitioning
            // into the TEST_STATE
            //
            OrderedRecordingSet testRecordings = orderedRecordings.getSubsetWithPriority(OcapRecordingProperties.TEST_RECORDING);
            testRecordings.setRecordingStates(OcapRecordingRequest.TEST_STATE, null);

            //
            // Now attempt to move everything in the list to PENDING_NO_CONFLICT
            // in the designated order - as the contention handler prescribed.
            //
            attemptToReserveRecordingsInOrder(orderedRecordings);

            //
            // Compare the (temporary) current state to the saved state,
            // perform appropriate action, and determine appropriate final
            // state
            //
            processConflictStateTransitions(orderedRecordings);

            //
            // Attempt to transition any P_W_C recordings to P_N_C
            // for the previous timespan
            //
            attemptToReserveRecordingsDuring(prevStartTime, prevDuration);
        } // END synchronized(m_sync)

    } // END resolveConflictsForRecordingChange()

    /**
     * Attempt to move PENDING_WITH_CONFLICT recordings which overlap the
     * deleted recording to PENDING_NO_CONFLICT.
     * 
     * Note that resolveConflictsForRecordingRemoval may change the state of
     * recordings which overlap with recording. These state changes will be
     * notified. But no state changes will be made for the passed recording and
     * no notifications will be performed for it.
     * 
     * @parm recording The recording being removed.
     */
    void resolveConflictsForRecordingRemoval(RecordingImplInterface recording)
    {
        if (log.isDebugEnabled())
        {
            log.debug("resolveConflictsForRecordingRemoval: " + recording);
        }

        attemptToReserveRecordingsDuring(recording.getRequestedStartTime(), recording.getDuration());
    } // END resolveConflictsForRecordingRemoval()

    /**
     * Attempt to move PENDING_WITH_CONFLICT recordings which overlap the
     * designated timespan to PENDING_NO_CONFLICT. Note that this function will
     * perform state notifications for all affected recording requests.
     * 
     * @param startTime
     *            The start of the timespan
     * @param duration
     *            The length of the timespan
     */
    void attemptToReserveRecordingsDuring(final long startTime, final long duration)
    {
        final NavigationManager nm = NavigationManager.getInstance();

        //
        // Just attempt to move all PENDING_WITH_CONFLICT recordings overlapping
        // with the deleted recording to PENDING_NO_CONFLICT
        //

        synchronized (m_sync)
        {
            OrderedRecordingSet overlappingPWCRecordings = nm.getOverlappingEntriesInStates(startTime, duration,
                    new int[] { LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE });

            if (log.isDebugEnabled())
            {
                log.debug("attemptToReserveRecordingsDuring: << start " + startTime + ", dur " + duration
                        + ">>: Overlapping P_W_C entries");
            }
            if (log.isDebugEnabled())
            {
                log.debug(overlappingPWCRecordings.toString());
            }

            //
            // Save the state of all recordings in the list - so we know
            // what we changed when we're done
            //
            overlappingPWCRecordings.saveAllRecordingStates();

            //
            // Now attempt to move everything in the list to PENDING_NO_CONFLICT
            // Note: This order is arbitrary. Different orders will produce
            // different
            // results. This is an issue the spec doesn't currently speak to
            //
            attemptToReserveRecordingsInOrderWithRCH(overlappingPWCRecordings);

        } // END synchronized(m_sync)
    } // END resolveConflictsForRecordingRemoval()

    /**
     * Explicitly set the prioritization of the recordings represented by the
     * passed resource usages.
     * 
     * @param resourceUsageList
     *            a list of ResourceUsages sorted in descending order of
     *            prioritization
     */
    void setRecordingPrioritiesExplicitly(ResourceUsage[] resourceUsageList)
    {
        //
        // Now fulfill the resource handler's wishes
        // 
        synchronized (m_sync)
        {
            // Note that the constructor will filter out any
            // non-RecordingResourceUsage objects - in case the handler
            // decides to do something funny
            OrderedRecordingSet orderedRecordings = new OrderedRecordingSet(resourceUsageList);

            // The orderedRecordings represent the Sphere of Influence for
            // priority changes

            //
            // Save the state of recordings in the list - so we know
            // what we changed when we're done
            //
            orderedRecordings.saveAllRecordingStates();

            //
            // Now consider everything in the SoI to be PENDING_WITH_CONFLICT
            //
            orderedRecordings.setRecordingStates(LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE, null);

            //
            // Now attempt to move everything in the list to PENDING_NO_CONFLICT
            // in the designated order - as the contention handler prescribed.
            //
            attemptToReserveRecordingsInOrder(orderedRecordings);

            // Recordings which lost reservations and are still pending
            // will be PENDING_WITH_CONFLICT after
            // attemptToReserveRecordingsInOrder()
            // (since they were all PENDING_NO_CONFLICT initially). Note that
            // orderedRecordings that are IN_PROGRESS will be P_W_C before
            // we call processConflictStateTransitions()
            OrderedRecordingSet unreservedRecordings = orderedRecordings.getSubsetWithState(LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE);

            //
            // Attempt to assign any lost reservations to recordings
            // currently w/o a reservation.
            //

            // Note: attemptToReassignLostReservations() operates on recordings
            // which are, by definition, NOT in the orderedRecordings.

            // Note: We do this before processConflictStateTransitions()
            // so that reservations held by IN_PROGRESS recordings can
            // be reassigned (since they temporarily appear as
            // PENDING_WITH_CONFLICT before processConflictStateTransitions()

            if (!unreservedRecordings.isEmpty())
            {
                attemptToReassignLostReservations(unreservedRecordings);
            }

            //
            // Compare the (temporary) current state to the saved state,
            // perform appropriate action, and determine appropriate final
            // state
            //
            processConflictStateTransitions(orderedRecordings);
        } // END synchronized(m_sync)
    } // END setRecordingPrioritiesExplicitly()

    /**
     * Get the prioritized list of overlapping ResourceUsages corresponding to a
     * particular recording request. The list will contain only
     * RecordingResourceUsages. The ResourceUsage corresponding to the specified
     * recording request is also included in the prioritized list. The
     * prioritized list is sorted according to the state of the associated
     * recording, as follows: PENDING_NO_CONFLICT, IN_PROGRESS,
     * IN_PROGRESS_INSUFFICIENT_SPACE, PENDING_WITH_CONFLICT. Recordings in all
     * other states are excluded.
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
     */
    public ResourceUsage[] getPrioritizedResourceUsages(RecordingRequest recording)
    {
        final NavigationManager nm = NavigationManager.getInstance();
        ResourceUsage[] prioritizedUsages;

        if (!(recording instanceof RecordingImplInterface))
        {
            return null;
        }

        final int prioritizedRecordingStates[] = new int[] { LeafRecordingRequest.PENDING_NO_CONFLICT_STATE,
                LeafRecordingRequest.IN_PROGRESS_STATE, LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE,
                LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE,
                LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE };

        synchronized (m_sync)
        {
            final RecordingImplInterface ri = (RecordingImplInterface) recording;
            final int riState = ri.getInternalState();
            int i, j, k;

            boolean isOneOfTheStates = false;

            for (i = 0; i < prioritizedRecordingStates.length && !isOneOfTheStates; i++)
            {
                isOneOfTheStates = (riState == prioritizedRecordingStates[i]);
            }

            if (!isOneOfTheStates)
            {
                return null;
            }

            // Assert: ri is in one of the pending or in-progress states

            OrderedRecordingSet overlappingRecordings = nm.getOverlappingEntriesInStates(ri, prioritizedRecordingStates);

            overlappingRecordings.addRecording(ri);

            // overlappingRecordings contains all the recordings that will go
            // into the prioritized list. Now we'll just pull them out in the
            // prescribed order. Note that the order of entries in
            // prioritizedRecordingStates prescribes the order in the list.

            prioritizedUsages = new ResourceUsage[overlappingRecordings.size()];
            i = 0;
            RecordingResourceUsage[] tempList;

            if (log.isDebugEnabled())
            {
                log.debug("getPrioritizedResourceUsages: Prioritized recodings:");
            }

            for (j = 0; j < prioritizedRecordingStates.length; j++)
            {
                OrderedRecordingSet orsForState;

                orsForState = overlappingRecordings.getSubsetWithState(prioritizedRecordingStates[j]);
                // Now add them to the list
                tempList = orsForState.getRecordingResourceUsages();

                for (k = 0; k < tempList.length; k++)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("getPrioritizedResourceUsages: res use " + i + ": "
                                + tempList[k].getRecordingRequest());
                    }

                    prioritizedUsages[i++] = tempList[k];
                }
            } // END for (j)

        } // END synchronized(m_sync)

        return prioritizedUsages;

    } // END getPrioritizedResourceUsages()

    /**
     * Attempt to make recordings PENDING_NO_CONFLICT_STATE via best-effort.
     * i.e. The contention handler will not be re-invoked. If a recording
     * conflicts, it will remain PENDING_WITH_CONFLICT.
     * 
     * The caller is presumed to have locked out recording and recordingDB
     * changes and saved all old states in ors.
     * 
     * @param Set
     *            of recordings to attempt transition
     */
    void attemptToReserveRecordingsInOrder(final OrderedRecordingSet orderedRecordings)
    {
        final NavigationManager nm = NavigationManager.getInstance();
        final int conflictingRecordingStates[] = new int[] { LeafRecordingRequest.PENDING_NO_CONFLICT_STATE,
                LeafRecordingRequest.IN_PROGRESS_STATE, LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE,
                LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE, LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE };
        final int numRecordings = orderedRecordings.size();

        // Assert: Caller has locked down the recordings and DB
        // Assert: Caller has saved the previous recording states
        // for all recordings in orderedRecordings
        // Assert: Caller has marked any recordings that it wishes to be
        // reserved to PENDING_WITH_CONFLICT_STATE

        if (log.isDebugEnabled())
        {
            log.debug("attemptToReserveRecordingsInOrder: Attempting to reserve " + numRecordings + " recordings on "
                    + m_networkInterfaceCount + " tuners");
        }

        //
        // Walk the list and try to light up each recording one-by-one
        //
        for (int i = 0; i < numRecordings; i++)
        {
            // We know that each recording is a RecordingImplInterface - since
            // we
            // passed them all to the ResourceContentionHandler with one
            // attached.
            RecordingImplInterface curRecording = (RecordingImplInterface) orderedRecordings.getRecordingRequest(i);

            if (curRecording.getInternalState() != LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)
            { // We're only going to mess with those marked P_W_C
                continue;
            }

            // SoI for attempting to move curRecording to PENDING_NO_CONFLICT
            // is the PENDING_NO_CONFLICT and IN_PROGRESS recordings which
            // overlap with curRecording.
            // Note that we could use the entire set of pending recordings
            // with simultaneousUseMoreThan() - but it's an n^2 operation,
            // and it's A Very Good Thing (tm) to make n small
            OrderedRecordingSet curRecordingOverlap = nm.getOverlappingEntriesInStates(curRecording,
                    conflictingRecordingStates);

            //
            // Make sure that the orderedRecordings are considered as well
            // since this function may be called prior to a recording being
            // added to the navigation manager
            //

            OrderedRecordingSet overlappingReservationInOrderedRecordings = orderedRecordings.getSubsetWithStates(
                    conflictingRecordingStates).getOverlappingEntries(curRecording);

            curRecordingOverlap.addRecordings(overlappingReservationInOrderedRecordings);

            if (log.isDebugEnabled())
            {
                log.debug("attemptToReserveRecordingsInOrder: Overlapping conflicting entries: ");
            }
            if (log.isDebugEnabled())
            {
                log.debug(curRecordingOverlap.toString());
            }

            // This is the expensive operation
            // Check for resource availabilty in the overlap
            if (curRecordingOverlap.simultaneousUseMoreThan(m_networkInterfaceCount - 1))
            {
                // Not enough network interfaces to reserve for
                // curRecording
                // It's already PENDING_WITH_CONFLICT - or we wouldn't
                // be here - just leave it as such
                if (log.isDebugEnabled())
                {
                    log.debug("attemptToReserveRecordingsInOrder: Recording " + curRecording
                            + " did NOT get a tuner reservation");
                }
            }
            else
            {
                // Assert: The max resource usage during curRecording's
                // duration is less than m_networkInterfaceCount.
                // Go ahead and make curRecording PENDING_NO_CONFLICT
                curRecording.setStateNoNotify(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);
                if (log.isDebugEnabled())
                {
                    log.debug("attemptToReserveRecordingsInOrder: Recording " + curRecording
                            + " GOT a tuner reservation");
                }
            }

            // Assert: curRecording current state is now either
            // PENDING_NO_CONFLICT or PENDING_WITH_CONFLICT
        } // END for (each recording in ordered set)
    } // END attemptToReserveRecordingsInOrder()

    /**
     * Attempt to make recordings PENDING_NO_CONFLICT_STATE via best-effort or
     * RCH.
     * 
     * @param Set
     *            of recordings to attempt transition
     */
    void attemptToReserveRecordingsInOrderWithRCH(final OrderedRecordingSet orderedRecordings)
    {
        RecordingImpl curRecording = null;
        ResourceUsage[] overlappingUsages = null;
        int dbstate, prevdbstate;
        final NavigationManager nm = NavigationManager.getInstance();
        final ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);

        final int conflictingRecordingStates[] = new int[] { LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE };

        final int numRecordings = orderedRecordings.size();

        // Assert: Caller has locked down the recordings and DB
        // Assert: Caller has saved the previous recording states
        // for all recordings in orderedRecordings
        // Assert: Caller has marked any recordings that it wishes to be
        // reserved to PENDING_WITH_CONFLICT_STATE

        if (log.isDebugEnabled())
        {
            log.debug("attemptToReserveRecordingsInOrderWithRCH: Attempting to reserve " + numRecordings
                    + " recordings on " + m_networkInterfaceCount + " tuners");
        }

        //
        // Walk the list and try to light up each recording one-by-one
        //
        for (int i = 0; i < numRecordings; i++)
        {

            synchronized (m_sync)
            {

                // We know that each recording is a RecordingImpl - since we
                // passed them all to the ResourceContentionHandler with one
                // attached.
                curRecording = (RecordingImpl) orderedRecordings.getRecordingRequest(i);

                if (curRecording.getInternalState() != LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)
                { // We're only going to mess with those marked P_W_C
                    continue;
                }

                // SoI for attempting to move curRecording to
                // PENDING_NO_CONFLICT
                // is the PENDING_NO_CONFLICT and IN_PROGRESS recordings which
                // overlap with curRecording.
                // Note that we could use the entire set of pending recordings
                // with simultaneousUseMoreThan() - but it's an n^2 operation,
                // and it's A Very Good Thing (tm) to make n small
                OrderedRecordingSet curRecordingOverlap = nm.getOverlappingEntriesInStates(curRecording,
                        conflictingRecordingStates);

                //
                // Make sure that the orderedRecordings are considered as well
                // since this function may be called prior to a recording being
                // added to the navigation manager
                //
                OrderedRecordingSet overlappingReservationInOrderedRecordings = orderedRecordings.getSubsetWithStates(
                        conflictingRecordingStates).getOverlappingEntries(curRecording);

                curRecordingOverlap.addRecordings(overlappingReservationInOrderedRecordings);

                if (log.isDebugEnabled())
                {
                    log.debug("attemptToReserveRecordingsInOrderWithRCH: <<" + curRecording
                            + ">>: Overlapping conflicting entries: ");
                }
                if (log.isDebugEnabled())
                {
                    log.debug(curRecordingOverlap.toString());
                }

                // This is the expensive operation
                // Check for resource availability in the overlap
                if (!curRecordingOverlap.simultaneousUseMoreThan(m_networkInterfaceCount - 1))
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("attemptToReserveRecordingsInOrderWithRCH: Recording " + curRecording
                                + " May have a tuner reservation - attempt to reserve it");
                    }
                    attemptToReserveRecordingsInOrder(curRecordingOverlap);
                    continue;
                }

                // We'll save away the state counter to determine if the DB
                // changes
                // while we're waiting for the handler to return
                prevdbstate = nm.getStateCounter();

                curRecordingOverlap.removeRecording(curRecording);

                // Assert: recording conflicts - let the juggling begin...
                overlappingUsages = curRecordingOverlap.getRecordingResourceUsages();
            }// END synchronized(m_sync)

            // Do the RCH stuff
            //
            // Call the conflict resolution handler with the overlapping
            // recording entries
            //

            ResourceUsageImpl overlapERUs[] = new ResourceUsageImpl[overlappingUsages.length];
            for (int j = 0; j < overlappingUsages.length; j++)
            {
                overlapERUs[j] = (ResourceUsageImpl) overlappingUsages[j];
            }

            if (log.isDebugEnabled())
            {
                log.debug("attemptToReserveRecordingsInOrderWithRCH: <<" + curRecording
                        + ">>: Calling conflict resolution handler:");
            }
            if (log.isDebugEnabled())
            {
                log.debug("attemptToReserveRecordingsInOrderWithRCH: <<" + curRecording + ">>:   newRequest: "
                        + curRecording.getResourceUsage());
            }
                for (int j = 0; j < overlappingUsages.length; j++)
                {
                if (log.isDebugEnabled())
                {
                    log.debug("attemptToReserveRecordingsInOrderWithRCH: <<" + curRecording + ">>:   curRes " + j
                            + ": " + overlapERUs[j]);
                }
            }

            // TODO: refactor RecordingListIterator iterator =
            // overlappingRecordings.createRecordingListIterator();
            // TODO: refactor DVRActivityLogger.getInstance().log(recording,
            // iterator);

            // Note: We're not holding the lock while calling back - to allow
            // recordings to be changed inside/during the contention
            ResourceUsage prioritizedERUs[] = rm.prioritizeContention2(
                    (ResourceUsageImpl) curRecording.getResourceUsage(), overlapERUs);

            // Assert: The handler has returned and has told us its relative
            // priorities

            if (log.isDebugEnabled())
            {
                log.debug("attemptToReserveRecordingsInOrderWithRCH: <<" + curRecording
                        + ">>: Resource Contention Handler returned:");
            }
                for (int ii = 0; ii < prioritizedERUs.length; ii++)
                {
                if (log.isDebugEnabled())
                {
                    log.debug("attemptToReserveRecordingsInOrderWithRCH:   Priority " + ii + ": <<0x"
                            + ((RecordingResourceUsage) prioritizedERUs[ii]).getRecordingRequest().getId() + ">>");
                }
            }

            //
            // Now fulfill the resource handler's wishes
            // 
            synchronized (m_sync)
            {
                dbstate = nm.getStateCounter();

                if (dbstate != prevdbstate)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("attemptToReserveRecordingsInOrderWithRCH: <<" + curRecording
                                + ">>: Recording DB changed during conflict resolution");
                    }
                }

                // Assert: Conflict resolution handler's prioritized list still
                // applies to the overlappingRecordings
                if (log.isDebugEnabled())
                {
                    log.debug("attemptToReserveRecordingsInOrderWithRCH: <<" + curRecording
                            + ">>: Recording conflicts - applying conflict resolution handler priorities");
                }

                // Note that the constructor will filter out any
                // non-RecordingResourceUsage objects - in case the handler
                // decides to do something funny
                OrderedRecordingSet orderedRecordingSet = new OrderedRecordingSet(prioritizedERUs);

                // The orderedRecordings represent the new Sphere of Influence
                // This *should* be exactly the same SoI as we passed into the
                // handler. But we don't depend on this - we act on the list
                // the RH gave us.

                //
                // Save the state of recordings in the list - so we know
                // what we changed when we're done
                //
                orderedRecordingSet.saveAllRecordingStates();

                //
                // Now consider everything in the SoI to be
                // PENDING_WITH_CONFLICT
                //
                orderedRecordingSet.setRecordingStates(LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE, null);

                //
                // Test recordings should not be granted reservations.
                // Prevent attempts to reserve resources for them by
                // transitioning
                // into the TEST_STATE
                //
                OrderedRecordingSet testRecordings = orderedRecordingSet.getSubsetWithPriority(OcapRecordingProperties.TEST_RECORDING);
                testRecordings.setRecordingStates(OcapRecordingRequest.TEST_STATE, null);

                //
                // Now attempt to move everything in the list to
                // PENDING_NO_CONFLICT
                // in the designated order - as the contention handler
                // prescribed.
                //
                attemptToReserveRecordingsInOrder(orderedRecordingSet);
            } // END synchronized(m_sync)
        } // END for (each recording in ordered set)
    } // END attemptToReserveRecordingsInOrderWithRCH()

    /**
     * Attempt to assign reservations lost by recordings in the
     * unreservedRecordings set via best-effort - potentially making some other
     * PENDING_WITH_CONFLICT recordings PENDING_NO_CONFLICT.
     * 
     * The caller is presumed to have locked out recording and recordingDB
     * changes.
     * 
     * @param Set
     *            of recordings which have lost their reservation
     */
    void attemptToReassignLostReservations(final OrderedRecordingSet unreservedRecordings)
    {
        long checkStart = Long.MAX_VALUE;
        long checkEnd = 0;
        final int numRecordings = unreservedRecordings.size();

        if (log.isDebugEnabled())
        {
            log.debug("attemptToReassignLostReservations: unreservedRecordings");
        }
        if (log.isDebugEnabled())
        {
            log.debug(unreservedRecordings.toString());
        }

        if (numRecordings == 0)
        {
            return;
        }

        //
        // Establish a time range - to avoid doing multiple (expensive) overlap
        // checks
        // against the entire recording DB
        //
        for (int i = 0; i < numRecordings; i++)
        {
            // We know that each recording is a RecordingImplInterface - since
            // we
            // passed them all to the ResourceContentionHandler with one
            // attached.
            RecordingImplInterface curRecording = (RecordingImplInterface) unreservedRecordings.getRecordingRequest(i);
            long curStart = curRecording.getRequestedStartTime();
            long curEnd = curStart + curRecording.getDuration();

            if (curStart < checkStart)
            {
                checkStart = curStart;
            }

            if (curEnd > checkEnd)
            {
                checkEnd = curEnd;
            }

        } // END for (unreservedRecordings)

        //
        // Now get all PENDING_WITH_CONFLICT recordings in designated time range
        //
        final NavigationManager nm = NavigationManager.getInstance();
        OrderedRecordingSet eligibleReceivers = nm.getOverlappingEntriesInStates(checkStart, checkEnd - checkStart,
                new int[] { LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE });

        // Remove the losers.
        eligibleReceivers.removeRecordings(unreservedRecordings);

        // Save the states of the eligible reservation recipients
        eligibleReceivers.saveAllRecordingStates();

        if (log.isDebugEnabled())
        {
            log.debug("attemptToReassignLostReservations: found " + eligibleReceivers.size()
                    + " eligible reservation receivers from " + checkStart + " to " + checkEnd);
        }
        if (log.isDebugEnabled())
        {
            log.debug(eligibleReceivers.toString());
        }

        // Attempt to transition them
        attemptToReserveRecordingsInOrder(eligibleReceivers);

        //
        // Compare the (temporary) current state to the saved state,
        // perform appropriate action, and determine appropriate final
        // state
        //
        processConflictStateTransitions(eligibleReceivers);
    } // END attemptToReassignLostReservations()

    /**
     * Compare the (temporary) current state to the saved state, perform
     * appropriate action, and determine appropriate final state.
     * 
     * The caller is presumed to have locked out recording and recordingDB
     * changes and saved all old states in ors.
     * 
     * @param Set
     *            of recordings to process transitions on
     */
    void processConflictStateTransitions(OrderedRecordingSet ors)
    {
        final int numRecordings = ors.size();

        for (int i = 0; i < numRecordings; i++)
        {
            RecordingImplInterface curRecording = (RecordingImplInterface) ors.getRecordingRequest(i);

            // Note: Once we've moved it to PENDING_NO_CONFLICT_STATE,
            // it won't move again. Go ahead and process it if needbe
            int oldState = curRecording.getSavedState();
            int newState = curRecording.getInternalState();

            //
            // Now examine what the end-result state transition was by
            // comparing the saved (old) state to the temporary new state
            // and determining the actual final state. Also perform
            // appropriate action for the transition.
            //
            switch (oldState)
            {
                case LeafRecordingRequest.INCOMPLETE_STATE:
                case LeafRecordingRequest.COMPLETED_STATE:
                case LeafRecordingRequest.FAILED_STATE:
                case LeafRecordingRequest.DELETED_STATE:
                case OcapRecordingRequest.TEST_STATE:
                case RecordingRequestImpl.DESTROYED_STATE:

                {
                    // Shouldn't even be here. What are we doing trying to
                    // transition these to PENDING_NO_CONFLICT?
                    // Doesn't matter what newState is - just get it back
                    // to its old state.
                    curRecording.restoreState();
                    break;
                }
                case RecordingRequestImpl.INIT_STATE:
                {
                    // New state can stick

                    // Was in INIT, and is now can be PENDING_NO_CONFLICT
                    // Assume the recording is still in process of beint
                    // initialized and the caller will do the notification.

                    // Leave the state as set so the caller will know what
                    // to do when it's done initializing.

                    // set the state w/o notification, and write the state to
                    // disk
                    curRecording.setStateNoNotify(newState);
                    curRecording.saveRecordingInfo(org.cablelabs.impl.manager.RecordingDBManager.STATE);
                    checkForResourceContentionWarning(curRecording, oldState, newState);
                    break;
                }
                case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                {
                    // New state can stick
                    // This guy may have just lost his reservation
                    // or gained a reservation - notify for either case,
                    curRecording.notifyIfStateChangedFromSaved();

                    // if the state has changed, write the new state to disk
                    if (oldState != newState)
                        curRecording.saveRecordingInfo(org.cablelabs.impl.manager.RecordingDBManager.STATE);

                    checkForResourceContentionWarning(curRecording, oldState, newState);
                    break;
                }
                case LeafRecordingRequest.IN_PROGRESS_STATE:
                case LeafRecordingRequest.IN_PROGRESS_INSUFFICIENT_SPACE_STATE:
                case LeafRecordingRequest.IN_PROGRESS_WITH_ERROR_STATE:
                case LeafRecordingRequest.IN_PROGRESS_INCOMPLETE_STATE:
                {
                    switch (newState)
                    {
                        case LeafRecordingRequest.PENDING_NO_CONFLICT_STATE:
                        case LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE:
                        {
                            // Whether this guy lost his reservation or not,
                            // Just let him stay IN_PROGRESS
                            curRecording.restoreState();
                            break;
                        }
                    }
                    break;
                } // END case (oldState==IN_PROGRESS_*_STATE)
            } // END switch (oldstate)
        } // END for (each recording in ordered set)
    } // END processConflictStateTransitions()

    /**
     * Determines if a conflict will at the warning period associated with the
     * current recording. If the recording is a new recording and the current
     * time is within the warning period, this method may invoke the
     * <code>ResourceContentionHandler</code>.
     * 
     * @param curRecording
     *            current recording
     * @param oldState
     *            current recording's old state
     * @param newState
     *            current recording's new state
     */
    private void checkForResourceContentionWarning(RecordingImplInterface curRecording, int oldState, int newState)
    {
        ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);
        if (rm == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("CIC - processConflictStateTransitions - RezMgr NULL! ");
            }
        }
        else
        {
            if (!rm.isContentionHandlerValid())
            {
                if (log.isDebugEnabled())
                {
                    log.debug(" CIC - processConflictStateTransitions - ResourceContentionHandler not installed");
                }
                return;
            }

            if (checkImpendingConflictsPreTest(curRecording, oldState, newState))
            {
                ConflictResults cr = checkImpendingConflicts(curRecording);
                if (cr.conflictDetected)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("CIC - conflicts detected");
                    }

                    ResourceUsage[] rua = new ResourceUsage[0];
                    rua = (ResourceUsage[]) cr.currentReservations.toArray(rua);
                    rm.deliverContentionWarningMessage(cr.requestedResourceUsage, rua);

                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("CIC - NO conflicts detected");
                    }
                }
            }
        }
    }

    /**
     * Determines whether the current recording meets the criteria to continue
     * performing the contention check. The recording will not meet the criteria
     * if it's state is PENDING_WITH_CONFLICT_STATE and it's priority flag is
     * RECORDING_IF_NO_CONFLICTS, otherwise the criteria is met.
     * 
     * @param rImpl
     *            current recording
     * @return true if the recording meets the criteria, false otherwise.
     */
    private boolean checkImpendingConflictsPreTest(RecordingImplInterface rImpl, int oldState, int newState)
    {
        if (log.isDebugEnabled())
        {
            log.debug("CIC - checkImpendingConflictsPreTest rec = " + rImpl);
        }

        // Must consider recording priority.
        OcapRecordingProperties orp = (OcapRecordingProperties) rImpl.getRecordingSpec().getProperties();
        byte priority = orp.getPriorityFlag();

        if ((newState == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE)
                && (priority == OcapRecordingProperties.RECORD_IF_NO_CONFLICTS))
        {
            if (log.isDebugEnabled())
            {
                log.debug("CIC - checkImpendingConflictsPreTest return false");
            }
            return false;
        }

        long tNOW = System.currentTimeMillis();

        ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);
        // Check for short circuit settings.
        // Do nothing if wp is invalid.
        int wp = rm.getWarningPeriod();
        if (wp <= 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("CIC - checkImpendingConflictsPreTest wp is <= 0");
            }
            return false;
        }

        if (tNOW >= (rImpl.getRequestedStartTime() - wp) && tNOW < rImpl.getRequestedStartTime())
        {
            if (log.isDebugEnabled())
            {
                log.debug("CIC - checkImpendingConflictsPreTest warning period passed for the recording - ret true");
            }
            return true;
        }

        if (log.isDebugEnabled())
        {
            log.debug("CIC - checkImpendingConflictsPreTest return false");
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.ocap.dvr.RecordingAlertListener#recordingAlert(org.ocap.dvr.
     * RecordingAlertEvent)
     */
    public void recordingAlert(RecordingAlertEvent e)
    {
        if (log.isDebugEnabled())
        {
            log.debug("CIC - trigger for rec = " + e.getRecordingRequest());
        }
        // Called to check for impending conflict for a recording.
        // The recording may be cancelled so check the state first.
        ResourceManager rm = (ResourceManager) ManagerManager.getInstance(ResourceManager.class);

        // Check for short circuit settings.
        // Do nothing if wp is invalid
        int wp = rm.getWarningPeriod();
        if (wp <= 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("CIC - warning period invalid!");
            }
            return;
        }

        if (!rm.isContentionHandlerValid())
        {
            if (log.isDebugEnabled())
            {
                log.debug("CIC:recordingAlert - ResourceContentionHandler not installed");
            }
            return;
        }

        ConflictResults cr = null;

        synchronized (m_sync)
        {
            if (log.isDebugEnabled())
            {
                log.debug("CIC:recordingAlert - calling checkImpendingConflicts");
            }
            cr = checkImpendingConflicts((RecordingImplInterface) e.getRecordingRequest());
        }

        if (cr.conflictDetected)
        {
            if (log.isDebugEnabled())
            {
                log.debug("CIC:recordingAlert - calling deliverContentionWarningMessage");
            }

            ResourceUsage[] rua = new ResourceUsage[0];
            rua = (ResourceUsage[]) cr.currentReservations.toArray(rua);
            rm.deliverContentionWarningMessage(cr.requestedResourceUsage, rua);
        }
    }

    /**
     * Invoked by the caller to determine if the conditions meet criteria to
     * invoke the
     * <code>ResourceContentionHandler#resourceContentionWarning</code> method.
     * 
     * @param recA
     *            The new request.
     * @return An object storing the results of the contention check.
     */
    private ConflictResults checkImpendingConflicts(RecordingImplInterface recA)
    {
        TimeShiftManager tsm = (TimeShiftManager) ManagerManager.getInstance(TimeShiftManager.class);

        if (log.isDebugEnabled())
        {
            log.debug("CIC - for rec = " + recA);
        }

        // Step 1.
        // init resource usage list from NICs
        NetworkInterface[] niFaces = NetworkInterfaceManager.getInstance().getNetworkInterfaces();

        // need the number of interfaces to determine the number of vectors
        int niMax = niFaces.length;
        if (log.isDebugEnabled())
        {
            log.debug("CIC - num NICs = " + niMax);
        }

        // Array var to hold each resource list since it is constant.
        Vector ruLists = new Vector();

        for (int nic_i = 0; nic_i < niMax; nic_i++)
        {
            NetworkInterfaceImpl nImpl = (NetworkInterfaceImpl) niFaces[nic_i];
            ArrayList al = nImpl.getResourceUsages();

            // convert to vector. It will be null if there are no
            // resource usages known to NetworkInterface.
            Vector vl = new Vector();

            if (al != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - ar not null");
                }
                for (int n = 0; n < al.size(); n++)
                {
                    vl.add(al.get(n));
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - nic[" + nic_i + " has no RUs. List is null");
                }
            }
            ruLists.add(vl);
        }

        // Step 2. Create list of pending recordings and include the target
        // recording.
        long startTimeRecA = recA.getRequestedStartTime();
        ArrayList pendingList = createPendingList(recA, startTimeRecA);

        // Step 3. For all NetworkInterfaceControllers call getRULIfNotRec(nic,
        // rul, rrul)
        // to obtain a new list of actual resource usages.
        for (int nic_i = 0; nic_i < niMax; nic_i++)
        {
            // Extract non-ongoing recordings from RUs obtained from each NIC.
            // If there are none, or the ruList is empty, null is returned.
            RecordingResourceUsage[] rrulNotOngoing = getListOfNotOngoing((Vector) ruLists.get(nic_i), startTimeRecA);

            if (rrulNotOngoing == null)
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - rrulNotOngoing is null");
                }

            // Request actual usages. Note the resultant RUL may be the same
            // as the input. Be careful, the method is expected to modify the
            // lists.
            if (rrulNotOngoing != null && rrulNotOngoing.length != 0)
            {
                Vector rul = tsm.getRULWithoutRecs((NetworkInterfaceImpl) niFaces[nic_i],// current
                                                                                         // NIC
                        (Vector) ruLists.get(nic_i), // RUL from current NIC
                        rrulNotOngoing); // List of RRU that are not ongoing
                ruLists.setElementAt(rul, nic_i);
            }
        }

        // Step 4. for all recordings in the set pendingSet which overlaps with
        // TR
        // The algorithm creates a new resource usage list when a recording from
        // the pendingRecList cannot be accomodated by one of the rul lists.
        Vector nonEmptyStore = new Vector();
        Service svc = null;
        int sizePendingList = pendingList.size();
        IntermediateResults info = new IntermediateResults();

        if (log.isDebugEnabled())
        {
            log.debug("\n\nCIC - recs pending = " + sizePendingList + "num RULs = " + ruLists.size());
        }

        for (int rec_i = 0; rec_i < sizePendingList; rec_i++)
        {
            // FOR Each RUL can the TSWMgr accomodate the pending recording
            // through sharing?
            if (log.isDebugEnabled())
            {
                log.debug("\n\nCIC ----------- TOP OF LOOP ---------");
            }

            // set to true if the pending request can be supported by a rul.
            info.supported = false;
            RecordingResourceUsage pendingRRu;

            RecordingImplInterface pendingRecording = (RecordingImplInterface) pendingList.get(rec_i);
            pendingRRu = pendingRecording.getResourceUsage();

            if (log.isDebugEnabled())
            {
                log.debug("CIC - Checking PendingRecording[" + rec_i + "] = " + pendingRecording + "\n\n");
            }
            if (log.isDebugEnabled())
            {
                log.debug("CIC - RRu of PendingRRu[" + rec_i + "] = " + pendingRRu + "\n\n");
            }

            svc = identifyService((RecordingRequest) pendingRecording);
            if (svc == null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - IdentifyService returned null");
                }
                continue;
            }

                if ((rec_i == 0))
                {
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - canSupportPending with rec_i == 0");
                }
            }

            if (canSupportPending(tsm, ruLists, niFaces, pendingRRu, svc, (rec_i == 0), info))
            {
                // only for the first pending recording.
                // The info.locationOfNewRequest is the same object as that
                // particular list in ruLists.
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - ruLists accepted RRU = " + pendingRRu);
                }
                if (rec_i == 0)
                {
                    info.supported = true;
                    info.inNonEmptyList = false;
                }
                continue;
            }

            // Now see if it can be shared by one of the new
            // lists.
            if (canSupportPending(tsm, nonEmptyStore, niFaces, pendingRRu, svc, (rec_i == 0), info))
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - nonEmptyLists accepted RRU = " + pendingRRu);
                }
                // it will be in the first non-empty list.
                if (rec_i == 0)
                {
                    info.inNonEmptyList = true;
                }
                continue;
            }

            // if none of the lists can support this pending request,
            // create a new list to signify an additional tuner is required.
            if (log.isDebugEnabled())
            {
                log.debug("CIC - pendingRRu NOT SUPPORTED. Newlist CREATED!");
            }

            // create a new list and
            // if it is the newRequest, remember the list.
            Vector newList = new Vector();
            RecordingImplInterface rec = (RecordingImplInterface) pendingList.get(rec_i);

            newList.add(rec.getResourceUsage());

            // If it is the newRequest, record the list to which it
            // belongs.
            if (rec_i == 0)
            {
                info.locationOfNewRequest = newList;
                info.inNonEmptyList = true;
            }

            // Add the newList to list of nonEmptyLists
            nonEmptyStore.add(newList);

        }// end loop of pending recordings

        // if (the number of non-empty lists is >= the number of tuners
        // a contention exists.)
        ConflictResults cr = new ConflictResults();
        // if (nonEmptyStore.size() >= niMax)
        if (nonEmptyStore.size() > 0)
        {
            cr.conflictDetected = true;
            if (log.isDebugEnabled())
            {
                log.debug("\n CIC -------- CONFLICT DETECTED --------");
            }

            // Now remove newRequest from the list.
            /**
             * If the newRequest RRU is in a newList of nonEmptyStore, Then
             * remove the RRU from the newList.
             * 
             * And if the list becomes empty after newRequest RRU is removed,
             * Then discard the newList from the nonEmptyStore because there are
             * no RUs to obtain from it.
             * 
             * If the newRequest RRU is in a list maintained by ruLists, Then
             * remove the newRequest RRU from that list.
             * 
             */
            if (info.inNonEmptyList == true)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - TARGET is inNonEmptyList sizeOf = " + nonEmptyStore.size());
                }
                info.locationOfNewRequest.remove(recA.getResourceUsage());
                if (info.locationOfNewRequest.size() == 0)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("CIC - REMOVE list for TARGET from NonEmptyList set.");
                    }
                    nonEmptyStore.remove(info.locationOfNewRequest);
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - TARGET is in ruLists.");
                }
                // This removes the non-empty list. It will only contain
                // one RRU.
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - REMOVE TARGET from ruLists");
                }
                if (info.locationOfNewRequest.remove(recA.getResourceUsage()) == false)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("CIC - failed to remove TARGET from ruList list.");
                    }
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("CIC - Success on remove TARGET from ruList list.");
                    }
            }
        }
            if (log.isDebugEnabled())
            {
                log.debug("CIC - elems in nonEmptyStore = " + nonEmptyStore.size());
            }
        }

        // create ResourceContentionWarning params. This will change when
        // other pieces are implemented.
        if (cr.conflictDetected)
        {
            int size = ruLists.size();

            for (int i = 0; i < size; i++)
            {
                Vector currRul = (Vector) ruLists.get(i);
                int currSize = currRul.size();
                if (currSize == 0)
                {
                    // Should never be the case unless there is a bug.
                    // If a conflict is detected, then the rul in rulLists
                    // should have one or more RUs that do conflict.
                    if (log.isDebugEnabled())
                    {
                        log.debug("CIC - rulList[" + i + "] is EMPTY Size == 0; continue");
                    }
                    continue;
                }

                // if list has one ResourceUsage it is obviously not shared.
                if (currSize == 1)
                {
                    ResourceUsage ru = (ResourceUsage) currRul.get(0);
                    if (log.isDebugEnabled())
                    {
                        log.debug("CIC - list has one ResourceUsage; not shared " + "currRes.add -> " + ru);
                    }

                    cr.currentReservations.add(ru);
                }

                // if list has mutiple ResourceUsages
                // wrap them in a SharedResourceUsage and add that to
                // currentReservations..
                if (currSize > 1)
                {

                    // Add any SharedResourceUsage objects to the
                    // currentReservations
                    // in ConflictResults.
                    ResourceUsage[] rua = new ResourceUsage[0];
                    rua = (ResourceUsage[]) currRul.toArray(rua);
                    SharedResourceUsageImpl sru = new DVRSharedResourceUsageImpl(rua);

                    if (log.isDebugEnabled())
                    {
                        log.debug("CIC - Adding SRU of size = " + rua.length);
                    }

                    // Add SharedResourceUsage object to the currentReservations
                    // list.
                    cr.currentReservations.add(sru);
                }
            }

            if (log.isDebugEnabled())
            {
                log.debug("CIC - num nonEmptyLists = " + nonEmptyStore.size());
            }
            for (int i = 0; i < nonEmptyStore.size(); i++)
            {
                Vector list = (Vector) nonEmptyStore.get(i);

                if (list.size() > 1) // create SRU
                {
                    ResourceUsage[] rua = new ResourceUsage[0];
                    rua = (ResourceUsage[]) list.toArray(rua);
                    SharedResourceUsageImpl sru = new DVRSharedResourceUsageImpl(rua);
                    // Add SharedResourceUsage object to the currentReservations
                    // list.
                    cr.currentReservations.add(sru);
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("From nonEmptyStore adding RU");
                    }
                    ResourceUsage ru = (ResourceUsage) list.get(0);
                    if (ru == null)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("ru = null");
                        }
                    }
                    cr.currentReservations.add(ru);
                }
            }
            cr.requestedResourceUsage = recA.getResourceUsage();
        }

        if (log.isDebugEnabled())
        {
            log.debug(" \n\n\n\n CIC - Summary Conflict = " + cr.conflictDetected);
        }
        if (log.isDebugEnabled())
        {
            log.debug(" CIC - Summary newRequest = " + cr.requestedResourceUsage);
        }
        if (log.isDebugEnabled())
        {
            log.debug(" CIC - Summary  currentReservations size = " + cr.currentReservations.size() + "\n\n\n\n");
        }
        return cr;
    }

    boolean canSupportPending(TimeShiftManager tsm, Vector ruls, NetworkInterface[] nia,
            RecordingResourceUsage pendingRRu, Service svc, boolean isNewRequest, IntermediateResults info)
    {
        // FOR Each RUL can the TSWMgr accomodate the pending recording
        // through sharing?
        int rulsSize = ruls.size();
        if (log.isInfoEnabled())
        {
            log.info("CIC - ENTERED canSupportPending NewRequest = " + isNewRequest);
        }

        // info.supported = false;
        boolean supported = false;
        // set to true if the pending request can be supported by a rul.
        for (int rul_i = 0; rul_i < rulsSize & !supported; rul_i++)
        {
            // get rulList.
            Vector currRul = (Vector) ruls.get(rul_i);

            if (currRul != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug(" CIC - currRul[" + rul_i + "] elem cnt = " + currRul.size());
                }
            }

            if (tsm.canRULSupportRecording(currRul, nia[rul_i], pendingRRu, svc))
            {
                // add the resource usage to the list of shared and
                // if it is the newRequest, remember the list.
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - supported rec = " + pendingRRu);
                }

                currRul.add(pendingRRu);
                // info.supported = true;
                supported = true;
                if (isNewRequest)
                {
                    // store list away.
                    if (log.isInfoEnabled())
                    {
                        log.info("CIC - pendingRec is TARGET.");
                    }
                    info.locationOfNewRequest = currRul;
                    info.inNonEmptyList = false;
                }
                break;
            }
        } // end loop of ru lists

        // return info.supported;
        if (log.isInfoEnabled())
        {
            log.info("canSupportPending  returning " + supported + "\n");
        }
        return supported;
    }

    /**
     * Returns the <code>Service</code> corresponding to the
     * <code>RecordingRequest</code>.
     * 
     * @param rr
     *            A RecordingRequest
     * @return Service corresponding to the RecordingRequest.
     * @throws InvalidLocatorException
     * @throws SecurityException
     */
    Service identifyService(RecordingRequest rr)
    {
        RecordingSpec rs = rr.getRecordingSpec();
        Service svc = null;

        if (rs instanceof ServiceRecordingSpec)
        {
            ServiceRecordingSpec srs = (ServiceRecordingSpec) rs;
            svc = srs.getSource();

        }
        else if (rs instanceof ServiceContextRecordingSpec)
        {
            ServiceContextRecordingSpec scrs = (ServiceContextRecordingSpec) rs;
            svc = scrs.getServiceContext().getService();

        }
        else if (rs instanceof LocatorRecordingSpec)
        {
            LocatorRecordingSpec lrs = (LocatorRecordingSpec) rs;
            SIManager sim = SIManager.createInstance();
            Locator locs[] = lrs.getSource();
            try
            {
                svc = sim.getService(locs[0]);
            }
            catch (SecurityException e)
            {
                e.printStackTrace();
            }
            catch (InvalidLocatorException e)
            {
                e.printStackTrace();
            }
        }
        return svc;
    }

    private RecordingResourceUsage[] getListOfNotOngoing(Vector resourceUsages, long startTimeOfTarget)
    {
        if (resourceUsages == null)
        {
            return null; // new RecordingResourceUsage [0];
        }

        int size = resourceUsages.size();

        // Don't know how many there will be so.
        ArrayList notOngoingList = new ArrayList();

        if (log.isDebugEnabled())
        {
            log.debug("CIC - getListOfNotOngoing.");
        }

        // For each RU in list.
        for (int i = 0; i < size; i++)
        {
            ResourceUsage ru = (ResourceUsage) resourceUsages.get(i);
            if (ru instanceof RecordingResourceUsage)
            {
                RecordingResourceUsage rru = (RecordingResourceUsage) ru;

                RecordingImplInterface rr = (RecordingImplInterface) rru.getRecordingRequest();
                long stopTimeElem = rr.getRequestedStartTime() + rr.getDuration();

                // Add recording to list of NOT-ongoing rec' res' usages
                if (stopTimeElem <= startTimeOfTarget)
                {
                    notOngoingList.add(rru);
                }
            }
        }// endfor

        if (log.isDebugEnabled())
        {
            log.debug("CIC - Not Ongoing Rec = " + notOngoingList.size());
        }

        RecordingResourceUsage[] ret = null;

        if (notOngoingList.size() > 0)
        {
            ret = new RecordingResourceUsage[notOngoingList.size()];
            for (int i = 0; i < notOngoingList.size(); i++)
            {
                ret[i] = (RecordingResourceUsage) notOngoingList.get(i);
            }
        }
        return ret;
    }

    ArrayList createPendingList(RecordingImplInterface newRequest, long startTimeRecA)
    {
        ArrayList pendingList = new ArrayList();

        // add the newRequest to the beginning of the list.
        pendingList.add(newRequest);

        RecordingStateFilter rsf = new RecordingStateFilter(LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE);

        // Note: Use the public RM getInstance() to ensure all recordings are
        // loaded
        RecordingList rlist = RecordingManager.getInstance().getEntries(rsf);

        int ndxOfRec = rlist.indexOf((RecordingRequest) newRequest);
        boolean skipFurtherChecks = false;
        if (ndxOfRec >= 0)
        {
            skipFurtherChecks = true;
        }

        if (log.isDebugEnabled())
        {
            log.debug("CIC - rlist PWC cnt = " + rlist.size());
        }
        for (int i = 0; i < rlist.size(); i++)
        {
            // priority = get the priority of the recording request from
            // RecordingImplInterface
            RecordingImplInterface rImpl = (RecordingImplInterface) rlist.getRecordingRequest(i);
            if (log.isDebugEnabled())
            {
                log.debug("CIC - In loop checking rec = " + rImpl);
            }

            // If the newRequest is in this list, and the loop counter
            // reaches its position, skip over it. Quicker to compare scalars
            // than to compare objects. In addition I set a flag to short
            // circuit
            // uneccessary checks in other lists.
            if (i == ndxOfRec)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - don't add self = " + rImpl);
                }
                skipFurtherChecks = true;
                continue;
            }

            OcapRecordingProperties orp = (OcapRecordingProperties) rImpl.getRecordingSpec().getProperties();

            long startTimeOther = rImpl.getRequestedStartTime();
            long stopTimeOther = rImpl.getRequestedStartTime() + rImpl.getDuration();
            byte priority = orp.getPriorityFlag();

            if ((priority == OcapRecordingProperties.RECORD_WITH_CONFLICTS) && (startTimeOther <= startTimeRecA)
                    && stopTimeOther > startTimeRecA)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - adding PWC + RWC rec =  " + rImpl);
                }
                pendingList.add(rImpl);
            }
        }

        rsf = new RecordingStateFilter(LeafRecordingRequest.PENDING_NO_CONFLICT_STATE);

        // Note: Use the public RM getInstance() to ensure all recordings are
        // loaded
        rlist = RecordingManager.getInstance().getEntries(rsf);
        if (log.isDebugEnabled())
        {
            log.debug("CIC - rlist PNC cnt = " + rlist.size());
        }

        // If it was not in the above list then it is in this list.
        // But reset the index to -1
        ndxOfRec = -1;
        if (skipFurtherChecks != true)
        {
            ndxOfRec = rlist.indexOf((RecordingRequest) newRequest);
        }

        // get recordings scheduled to start, which are pending without
        // conflict.
        for (int i = 0; i < rlist.size(); i++)
        {
            // If the newRequest is in this list, and the loop counter
            // reaches its position, skip over it. Quicker to compare scalars
            // than to compare objects. In addition I set a flag to short
            // circuit
            // uneccessary checks in other lists.
            if (i == ndxOfRec)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - don't add self");
                }
                skipFurtherChecks = true;
                continue;
            }

            RecordingImplInterface rImpl = (RecordingImplInterface) rlist.getRecordingRequest(i);
            if (log.isDebugEnabled())
            {
                log.debug("CIC - In loop checking rec = " + rImpl);
            }

            long startTimeOther = rImpl.getRequestedStartTime();
            long stopTimeOther = rImpl.getRequestedStartTime() + rImpl.getRecordingInfo().getRequestedDuration();

            // priority = get the priority of the recording request from
            // RecordingImplInterface.
            if (startTimeOther <= startTimeRecA && stopTimeOther > startTimeRecA)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("CIC - adding PNC rec =  " + rImpl);
                }
                pendingList.add(rImpl);
            }
        }

        return pendingList;
    }

    /**
     * Default media storage volume
     */
    private MediaStorageVolume m_defaultMediaStorageVolume = null;

    /**
     * Number of network interfaces, used for conflict detection
     */
    private int m_networkInterfaceCount = 0;

    /**
     * internal sync object
     */
    private Object m_sync = new Object();

    /**
     * singleton instance
     */
    protected static RecordingResourceManager m_instance = null;

    /**
     * Log4J logging facility.
     */
    private static final Logger log = Logger.getLogger(RecordingResourceManager.class.getName());

    /**
     * warning period
     */
    private int warningPeriod;
    // static ResourceManager rm =
    // (ResourceManager)ManagerManager.getInstance(ResourceManager.class);

}
