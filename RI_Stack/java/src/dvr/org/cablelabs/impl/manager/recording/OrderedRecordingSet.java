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

import java.util.Collection;
import java.util.Iterator;

import org.ocap.dvr.RecordingResourceUsage;
import org.ocap.resource.ResourceUsage;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;

/**
 * The OrderedRecordingSet enables operations which can be performed on an
 * entire set of RecordingRequests. Being a set, it doesn't allow duplicates.
 * But it does allow ordering.
 */

class OrderedRecordingSet extends RecordingListImpl
{
    /**
     * Create an empty OrderedRecordingSet
     * 
     */
    OrderedRecordingSet()
    {
        super();
    }

    /**
     * Create a copy of an OrderedRecordingSet
     * 
     * @param orderedRecordingSet
     *            The OrderedRecordingSet to copy
     */
    OrderedRecordingSet(RecordingList recordingList)
    {
        super(((RecordingListImpl)recordingList).getRecordings());
    }

    /**
     * Create a new OrderedRecordingSet with one recording
     * 
     * @param recordingImpl
     *            The recording to put in the set.
     */
    OrderedRecordingSet(RecordingImplInterface recordingImpl)
    {
        super();
        getRecordings().add(recordingImpl);
    }

    /**
     * Create a OrderedRecordingSet from a collection. The should contain
     * RecordingImplInterface objects and should be non-mutable.
     * 
     * @param collection
     *            The Collection used as the OrderedRecordingSet
     */
    OrderedRecordingSet(Collection collection)
    {
        super(collection);
    }

    /**
     * Create an OrderedRecordingSet from an array of resource usages. Only
     * RecordingResourceUsages which refer to RecordingImplInterface objects
     * will be used to populate the OrderedRecordingSet.
     * 
     * @param rous
     *            Array used to initialize the OrderedRecordingSet
     */
    OrderedRecordingSet(ResourceUsage[] rous)
    {
        super();

        for (int i = 0; i < rous.length; i++)
        {
            // Skip everything but RecordingResourceUsages
            if (rous[i] instanceof RecordingResourceUsage)
            {
                // Assert: rous[i] is a RecordingResourceUsage

                RecordingResourceUsage rru = (RecordingResourceUsage) rous[i];
                RecordingRequest rr = rru.getRecordingRequest();

                // Shouldn't be possible to have a RecordingRequest in the
                // system that isn't backed by a RecordingImplInterface
                // Assert: rr is a RecordingImplInterface

                addRecording((RecordingImplInterface) rr);
            }
        } // END for
    } // END OrderedRecordingSet(org.ocap.resource.ResourceUsage[])

    /**
     * Add a recording to the end of the set, if it's not already in the set.
     * 
     * @param recordingImpl
     *            The RecordingImpl to add
     */
    void addRecording(RecordingImplInterface recordingImpl)
    {
        if (!getRecordings().contains(recordingImpl))
        {
            getRecordings().add(recordingImpl);
        }
    }

    /**
     * Remove a recording from the set
     * 
     * @param recordingImpl
     *            The recording to remove
     * @return true if it was removed and false if it's not present in the set
     */
    boolean removeRecording(RecordingImplInterface recordingImpl)
    {
        return getRecordings().remove(recordingImpl);
    }

    /**
     * Add all recordings in RecordingList to the list. Note that duplicate
     * entries in the RecordingList or entries already in the
     * OrderedRecordingSet will not be added to the OrderedRecordingSet.
     * 
     * @parm orderedRecordingSet The set to aggregate
     */
    void addRecordings(OrderedRecordingSet orderedRecordingSet)
    {
        Iterator iterator = orderedRecordingSet.getRecordings().iterator();
        while (iterator.hasNext())
        {
            addRecording((RecordingImplInterface) iterator.next());
        }
    }

    /**
     * Add all recordings in the collection to the list. Note that duplicate
     * entries in the RecordingList or entries already in the
     * OrderedRecordingSet will not be added to the OrderedRecordingSet.
     * 
     * @parm collection The set to aggregate
     */
    void addRecordings(final Collection collection)
    {
        getRecordings().addAll(collection);
    }

    /**
     * Remove all designated recordings from the set. If a recording is not
     * present in the set, it will silently continue with the remaining
     * recordings.
     * 
     * @param orderedRecordingSet
     *            The set to remove
     */
    void removeRecordings(OrderedRecordingSet orderedRecordingSet)
    {
        Iterator iterator = orderedRecordingSet.getRecordings().iterator();
        while (iterator.hasNext())
        {
            removeRecording((RecordingImplInterface) iterator.next());
        }
    }

    /**
     * Return true if the OrderedRecordingSet is empty.
     * 
     */
    boolean isEmpty()
    {
        return getRecordings().isEmpty();
    }

    /**
     * Create a new OrderedRecordingSet containing all entries in the
     * OrderedRecordingSet which are ongoing with target.
     * 
     * @param target
     *            The recording to check against the set
     * @return The set of all overlapping recordings
     */
    OrderedRecordingSet getOverlappingEntries(final RecordingImplInterface target)
    {
        return getOverlappingEntries(target.getRequestedStartTime(), target.getDuration());
    } // END getOverlappingEntries(RecordingImplInterface)

    /**
     * Create a new OrderedRecordingSet containing all entries in the
     * OrderedRecordingSet which are ongoing during the specified time period.
     * 
     * @param startTime
     *            The start of the time period
     * @param duration
     *            The duration of the time period
     * @return The set of all recordings ongoing during time period
     */
    OrderedRecordingSet getOverlappingEntries(final long startTime, final long duration)
    {
        final long endTime = startTime + duration;
        Iterator iterator = getRecordings().iterator();
        OrderedRecordingSet overlap = new OrderedRecordingSet();

        while (iterator.hasNext())
        {
            RecordingImplInterface recordingImpl = (RecordingImplInterface) iterator.next();
            long ri_s = recordingImpl.getRequestedStartTime();

            if (!(((ri_s + recordingImpl.getDuration()) <= startTime) || (ri_s >= endTime)))
            { // The current ri neither ends before the target start
                // or starts before the target end
                overlap.getRecordings().add(recordingImpl);
                // Note: We don't need to use addRecording() since we're
                // taking a subset of a set - which is a set - so no need to
                // check for dups
            }
        }

        return overlap;
    }

    /**
     * Get a new OrderedRecordingSet containing recordings in the designated
     * state(s).
     * 
     * @param states
     *            Only consider recordings in the designated states. If null,
     *            ignore state
     * @return New OrderedRecordingSet containing elements of the
     *         OrderedRecordingSet which are in one of the designated states.
     */
    OrderedRecordingSet getSubsetWithStates(int states[])
    {
        if (states == null)
        {
            return new OrderedRecordingSet(this);
        }

        OrderedRecordingSet subset = new OrderedRecordingSet();
        Iterator iterator = getRecordings().iterator();
        while (iterator.hasNext())
        {
            RecordingImplInterface recordingImpl = (RecordingImplInterface) iterator.next();
            int internalState = recordingImpl.getInternalState();

            for (int k = 0; k < states.length; k++)
            {
                if (internalState == states[k])
                {
                    subset.getRecordings().add(recordingImpl);
                    break;
                }
            }
        }

        return subset;
    }

    /**
     * Get a new OrderedRecordingSet containing recordings in the designated
     * state.
     * 
     * @param state
     *            Only consider recordings in the designated state.
     * 
     * @return New OrderedRecordingSet containing elements of the
     *         OrderedRecordingSet which are in the designated state.
     */
    OrderedRecordingSet getSubsetWithState(int state)
    {
        OrderedRecordingSet subset = new OrderedRecordingSet();
        Iterator iterator = getRecordings().iterator();
        while (iterator.hasNext())
        {
            RecordingImplInterface recordingImpl = (RecordingImplInterface) iterator.next();
            if (recordingImpl.getInternalState() == state)
            {
                subset.getRecordings().add(recordingImpl);
            }
        }
        return subset;
    }

    /**
     * Get a new OrderedRecordingSet containing recordings with the designated
     * priority.
     * 
     * @param priority
     *            Only consider recordings with the designated priority.
     * 
     * @return New OrderedRecordingSet containing elements of the
     *         OrderedRecordingSet with the designated priority.
     */
    OrderedRecordingSet getSubsetWithPriority(byte priority)
    {
        OrderedRecordingSet subset = new OrderedRecordingSet();
        Iterator iterator = getRecordings().iterator();
        while (iterator.hasNext())
        {
            RecordingImplInterface recordingImpl = (RecordingImplInterface) iterator.next();
            if (recordingImpl.getPriority() == priority)
            {
                subset.getRecordings().add(recordingImpl);
            }
        }
        return subset;
    }

    /**
     * Check for maxuse overlaps of all recordings in the OrderedRecordingSet
     * 
     * @param maxuse
     *            The overlap limit
     * 
     * @return true if there is more than maxuse recordings going on at any time
     *         with the recordings in the OrderedRecordingSet
     * @return false if no more than maxuse recordings are ongoing at any time
     *         within the OrderedRecordingSet
     */
    boolean simultaneousUseMoreThan(int maxuse)
    {
        final int numrecs = getRecordings().size();

        // For each entry's start time, check all other entries to see how
        // many are ongoing at the start time

        if ((maxuse == 0) && (numrecs > 0))
        {
            return true;
        }

        for (int i = 0; i < numrecs; i++)
        {
            RecordingImplInterface ri_i = (RecordingImplInterface) (getRecordings().get(i));
            long st_i = ri_i.getRequestedStartTime();
            int count = 1;

            for (int j = 0; j < numrecs; j++)
            {
                if (i != j)
                {
                    RecordingImplInterface ri_j = (RecordingImplInterface) (getRecordings().get(j));

                    // Assert: st_j is in one of the requested states

                    long st_j = ri_j.getRequestedStartTime();

                    if ((st_i >= st_j) && (st_i < (st_j + ri_j.getDuration())))
                    {
                        if (++count > maxuse) return true;

                    } // END if (st_i is during j)
                } // END if (i != j)
            } // END for (j)
        } // END for (i)

        return false;
    }

    /**
     * Get an array of the RecordingResourceUsages for the recordings contained
     * in the OrderedRecordingSet. The array will contain the recordings in the
     * same order as they are within the OrderedRecordingSet
     * 
     * @return the array of RecordingResourceUsages for the recordings
     */
    RecordingResourceUsage[] getRecordingResourceUsages()
    {
        final RecordingResourceUsage rous[] = new RecordingResourceUsage[getRecordings().size()];
        Iterator iterator = getRecordings().iterator();
        int i = 0;
        while (iterator.hasNext())
        {
            rous[i++] = ((RecordingImplInterface) iterator.next()).getResourceUsage();

        }
        return rous;
    }

    /**
     * Save the state of each recording in the set
     */
    void saveAllRecordingStates()
    {
        Iterator iterator = getRecordings().iterator();
        while (iterator.hasNext())
        {
            ((RecordingImplInterface) iterator.next()).saveState();
        }
    }

    /**
     * Conditionally set the state of each recording in the set. The state of
     * each recording in the set will be changed to newState without an
     * associated notification if the current state is in oldStates or oldStates
     * is null.
     * 
     * @param state
     *            new state
     * @param states
     *            Only change recordings in the designated states. If null,
     *            ignore state
     */
    void setRecordingStates(int newState, int[] oldStates)
    {
        Iterator iterator = getRecordings().iterator();
        if (oldStates == null)
        {
            while (iterator.hasNext())
            {
                ((RecordingImplInterface) iterator.next()).setStateNoNotify(newState);
            }
        }
        else
        {
            while (iterator.hasNext())
            {
                RecordingImplInterface ri = (RecordingImplInterface) iterator.next();
                int internalState = ri.getInternalState();

                for (int j = 0; j < oldStates.length; j++)
                {
                    if (internalState == oldStates[j])
                    {
                        ri.setStateNoNotify(newState);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Notify if state changed for each recording in the set
     */
    void notifyAllChangedRecordingStates()
    {
        Iterator iterator = getRecordings().iterator();
        while (iterator.hasNext())
        {
            ((RecordingImplInterface) iterator.next()).notifyIfStateChangedFromSaved();
        }
    }

    /**
     * String representation - for debugging.
     * 
     * @return Strong representation of the OrderedRecordingSet
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        final String nl = System.getProperty("line.separator");
        final int numrecs = getRecordings().size();

        sb.append("OrderedRecordingSet 0x").append(Integer.toHexString(this.hashCode()));
        sb.append(" has ").append(numrecs).append(" recordings:").append(nl);

        for (int i = 0; i < numrecs; i++)
        {
            RecordingImplInterface ri = (RecordingImplInterface) (getRecordings().get(i));
            long starttime = ri.getRequestedStartTime();
            long dur = ri.getDuration();
            sb.append("  Recording " + i + '(' + ri.getName() + '/' + ri.hashCode() + "): ");
            sb.append(ri);
            sb.append(nl);
            sb.append("    start " + starttime);
            sb.append(", dur " + dur);
            sb.append(", end " + (starttime + dur));
            sb.append(", state " + ri.getInternalState());
            sb.append(", savstate " + ri.getSavedState());
            sb.append(nl);
        }

        return sb.toString();
    }
}
