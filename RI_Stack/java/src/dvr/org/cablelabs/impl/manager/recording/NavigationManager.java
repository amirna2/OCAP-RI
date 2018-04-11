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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.dvb.application.AppID;
import org.ocap.dvr.OcapRecordingProperties;
import org.ocap.dvr.storage.MediaStorageVolume;
import org.ocap.shared.dvr.LeafRecordingRequest;
import org.ocap.shared.dvr.ParentRecordingRequest;
import org.ocap.shared.dvr.RecordingChangedEvent;
import org.ocap.shared.dvr.RecordingChangedListener;
import org.ocap.shared.dvr.RecordingPermission;
import org.ocap.shared.dvr.RecordingProperties;
import org.ocap.shared.dvr.RecordingRequest;
import org.ocap.shared.dvr.navigation.RecordingList;
import org.ocap.shared.dvr.navigation.RecordingListFilter;
import org.ocap.storage.LogicalStorageVolume;
import org.ocap.storage.StorageProxy;

import org.cablelabs.impl.manager.CallbackData;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.storage.StorageProxyImpl;
import org.cablelabs.impl.util.SecurityUtil;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * The navigation manager has the responsibility of implementing the navigation
 * functionality as specified by org.ocap.dvr.navigation. It will implement the
 * org.ocap.dvr.navigation.RecordingList interface and will populate these lists
 * accordingly.
 */

public class NavigationManager
{

    /**
     * 
     * @uml.property name="m_instance"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    static private NavigationManager m_instance = null;

    /**
     * <code>m_masterRecordingList</code> reference to the main recording list.
     */
    private Vector m_masterRecordingList;

    private Vector m_parentMasterRecordingList;

    /**
     * <code>m_notificands</code> reference to listeners.
     */
    private Vector m_notificands;

    /**
     * <code>m_listenerLock</code> synchronizes access to the recording lists.
     */
    private Object m_sync;

    /**
     * <code>m_stateCounter</code> tracks the state of the database.
     */
    private int m_stateCounter = 1;

    /**
     * <code>m_ccm<.code> reference to the <code>CallerContextManager</code>.
     * 
     */
    private CallerContextManager m_ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);

    /**
     * RecordingID to RecordingRequest Hashtable.
     */
    private Hashtable m_recordingIDHash;

    /**
     * Specifically monitors when an application terminates.
     * 
     * @author Jeff Spruiel
     */
    class Client implements CallbackData
    {
        // Called when an application is terminated.
        public void destroy(CallerContext ctx)
        {
            releaseClientResources(ctx);
        }

        public void pause(CallerContext ctx)
        {
        }

        public void active(CallerContext callerContext)
        {
        }
    }

    /**
     * Returns the singleton instance of the NavigationManager.
     */
    public static synchronized NavigationManager getInstance()
    {
        if (m_instance == null)
        {
            m_instance = new NavigationManager();
        }
        return m_instance;
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
     * Constructor.
     */
    protected NavigationManager()
    {
        m_sync = new Object();
        // Note: This sync object will be changed when RecordingManager comes up
        // and before any entries are in the master recording list
        m_masterRecordingList = new Vector(0, 0);
        m_parentMasterRecordingList = new Vector(0, 0);
        m_notificands = new Vector(0, 0);
        m_recordingIDHash = new Hashtable();
    }

    /**
     * Releases all resources and sets static singleton variable to null.
     */
	// Added for findbugs issues fix - Moving m_sync variable outside the synchronization block
    void shutDown()
    {
        synchronized (m_sync)
        {
            if (m_masterRecordingList != null)
            {
                m_masterRecordingList.removeAllElements();
                m_masterRecordingList = null;
            }

            if (m_parentMasterRecordingList != null)
            {
                m_parentMasterRecordingList.removeAllElements();
                m_parentMasterRecordingList = null;
            }

            if (m_notificands != null)
            {
                m_notificands.removeAllElements();
                m_notificands = null;
            }

            if (m_recordingIDHash != null)
            {
                m_recordingIDHash.clear();
                m_recordingIDHash = null;
            }
            m_ccm = null;
            m_instance = null;
            
        }
            m_sync = null;
    }

    /**
     * Notifies listeners that a recording list entry has been updated
     * 
     * @param recording
     *            the recording which was updated.
     * @param newState
     *            the recordings new state.
     * @param oldState
     *            the recordings old state.
     */
    void updateRecording(RecordingRequest request, int newState, int oldState)
    {
        RecordingChangedEvent event;
        synchronized (m_sync)
        {
            /**
             * non-javadoc Create a <code>RecordingChangedEvent</code> for
             * reason <code>RecordingChangedEvent.ENTRY_STATE_CHANGED</code> and
             * deliver to each listener.
             */

            setStateChanged();

            event = new RecordingChangedEvent(request, newState, oldState, RecordingChangedEvent.ENTRY_STATE_CHANGED);

            if (log.isInfoEnabled())
            {
                log.info("NavigationManager: updateRecording from state = " + oldState + " to new state " + newState
                        + " for " + request);
            }

            dispatchRecordingChangedEvent(event);
        }
    }

    /**
     * Adds a recording to the list of outstanding recordings in the recording
     * database.
     */
    void insertRecording(RecordingRequest request)
    {
        Vector list = null;
        // Prevent access to the main list of recordings
        synchronized (m_sync)
        { // TODO: This should be synced at a higher level. 
            if (request instanceof ParentRecordingRequest)
            {
                list = m_parentMasterRecordingList;

            }
            else
            {
                list = m_masterRecordingList;
            }

            // Add the recording to the main list
            list.addElement(request);

            // Add recordingID and recording to hashtable (ECR-829)
            m_recordingIDHash.put(new Integer(request.getId()), request);

            setStateChanged();

        }
    }

    protected void notifyRecordingAdded(RecordingRequestImpl request)
    {
        // MCN
        // Create event for RecordingListEvent.ENTRY_ADDED and send to
        // listeners.
        // It was decided that our implementation report the current state
        // as the old state and new state. Rev I01 of the DVR spec
        // is not clear in regards to states and events.
        int state = request.getState();
        RecordingChangedEvent event = new RecordingChangedEvent(request, state, state,
                RecordingChangedEvent.ENTRY_ADDED);

        if (log.isInfoEnabled())
        {
            log.info("NavigationManager: notifyRecordingAdded state = " + state + " for " + request);
        }

        // Deliver the event to all listeners
        dispatchRecordingChangedEvent(event);
    }

    /**
     * Removes a recording from the recording database
     */
    void removeRecording(RecordingRequest recording, int newState, int oldState)
    {
        Vector list;

        if (recording instanceof LeafRecordingRequest)
        {
            list = m_masterRecordingList;
        }
        else
        {
            list = m_parentMasterRecordingList;
        }

        if (list.removeElement(recording) == false)
        {
            return;
        }

        // Add recordingID and recording to hashtable (ECR-829)
        m_recordingIDHash.remove(new Integer(recording.getId()));

        setStateChanged();

        // Lookup the RecordingListEntry in the main recording list
        // If not found
        // return and don't send event, rare
        // Else remove the RecordingListEntry from the list
        RecordingChangedEvent event = new RecordingChangedEvent(recording, newState, oldState,
                RecordingChangedEvent.ENTRY_DELETED);
        if (log.isInfoEnabled())
        {
            log.info("NavigatorManager::removeRecording  from state " + oldState + " to  " + newState + " for req = "
                    + recording);
        }

        // Deliver the event to all listeners
        dispatchRecordingChangedEvent(event);
    }

    /**
     * Gets the list of entries maintained by the RecordingManager. This list
     * includes scheduled recordings, ongoing recordings, completed recordings
     * and failed recordings.
     * 
     * OCAP1 I16 Table 10-2: "Only entries for which the application has read
     * extended file access permission will be listed."
     * 
     * @return an instance of RecordingList containing all the entries
     *         maintained by the RecordingManager to which the calling
     *         application has read file access permission. If the calling
     *         application has MonitorAppPermission("recording") all entries are
     *         returned.
     * 
     * @see getEntries(RecordingListFilter filter)
     */
    RecordingList getEntries()
    {
        if (log.isDebugEnabled())
        {
            log.debug("NavigationManager: getEntries");
        }

        return getEntries(null);
    }

    /**
     * Gets the list of recording matching the specified filter.
     * 
     * @param filter
     *            the filter to use on the total set of recording requests If
     *            null, no filtering will be applied.
     * 
     *            OCAP1 I16 Table 10-2: "Only entries for which the application
     *            has read extended file access permission will be listed."
     * 
     * @return an instance of RecordingList containing all the entries matching
     *         the specified filter to which the calling application has read
     *         file access permission. If the calling application has
     *         MonitorAppPermission("recording") all matching entries are
     *         returned.
     * 
     * @see getEntries
     */
    public RecordingList getEntries(RecordingListFilter filter)
    {
        // Prevent access to the main list
        // Copy the list to temp space
        // Allow access, adds & deletes requests continue while filtering.
        synchronized (m_sync)
        {
            if (log.isDebugEnabled())
            {
                log.debug("NavigationManager: getEntries(filter)");
            }

            Vector sum = new Vector(0, 0);
            sum.addAll(m_masterRecordingList);
            sum.addAll(m_parentMasterRecordingList);
            RecordingListImpl recImpl = null;
            try
            {
                SecurityUtil.checkPermission(new RecordingPermission("read", "*"));
                // NO NEED FOR FURTHER CHECKS ... BUT MIGHT NEED TO FILTER ...
                Vector partialSum = new Vector(sum.size());
                Enumeration enumer = sum.elements();
                while (enumer.hasMoreElements())
                {
                    RecordingRequest rr = (RecordingRequest) enumer.nextElement();
                    if (rr instanceof RecordingImpl)
                    {
                        RecordingImpl ri = (RecordingImpl) rr;
                        
                        if (ri.getInternalState() == RecordingImpl.DESTROYED_STATE)
                        {
                            continue;
                        }
                        // check for removed storage here
                        if ( ( ri.getInternalState() == RecordingImpl.INCOMPLETE_STATE
                               || ri.getInternalState() == RecordingImpl.COMPLETED_STATE 
                               || ri.getInternalState() == RecordingImpl.FAILED_STATE )
                             && ri.isStorageReady() == false )
                        {
                            continue;
                        }
                    }
                    if (null == filter || filter.accept(rr))
                    {
                        partialSum.addElement(rr);
                    }
                }
                recImpl = new RecordingListImpl(partialSum);
            }
            catch (SecurityException e1)
            {
                Vector partialSum = new Vector(sum.size());
                try
                {
                    SecurityUtil.checkPermission(new RecordingPermission("read", "own"));
                    Enumeration enumer = sum.elements();
                    while (enumer.hasMoreElements())
                    {
                        RecordingRequest rr = (RecordingRequest) enumer.nextElement();
                        if (rr instanceof RecordingImpl)
                        {
                            RecordingImpl ri = (RecordingImpl) rr;
                            
                            if (ri.getInternalState() == RecordingImpl.DESTROYED_STATE)
                            {
                                continue;
                            }
                            // check for removed storage here
                            if ( ( ri.getInternalState() == RecordingImpl.INCOMPLETE_STATE
                                   || ri.getInternalState() == RecordingImpl.COMPLETED_STATE 
                                   || ri.getInternalState() == RecordingImpl.FAILED_STATE )
                                 && ri.isStorageReady() == false )
                            {
                                continue;
                            }
                        }
                        if (null == filter || filter.accept(rr))
                        {
                            partialSum.addElement(rr);
                        }
                    }
                    recImpl = new RecordingListImpl(partialSum);
                }
                catch (SecurityException e2)
                {
                    // RETURN EMPTY LIST (not null)
                    partialSum.removeAllElements();
                    recImpl = new RecordingListImpl(partialSum);
                }
            }
            return recImpl;
        }
    }

    RecordingList getLeafEntries()
    {
        synchronized (m_sync)
        {
            if (log.isDebugEnabled())
            {
                log.debug("NavigationManager: getLeafEntries");
            }

            RecordingListImpl recImpl = new RecordingListImpl(m_masterRecordingList);
            return recImpl;
        }
    }

    /**
     * Gets the list of recording matching the specified filter.
     * 
     * @return an instance of RecordingList containing all the entries matching
     *         the specified filter to which the calling application has read
     *         file access permission. If the calling application has
     *         MonitorAppPermission("recording") all matching entries are
     *         returned.
     * 
     */
    RecordingList getLeafEntries(RecordingListFilter filter)
    {
        if (log.isDebugEnabled())
        {
            log.debug("NavigationManager: getLeafEntries(filter)");
        }

        synchronized (m_sync)
        {
            Vector clone = (Vector) m_masterRecordingList.clone();
            if (filter == null)
            {
                return new RecordingListImpl(m_masterRecordingList);
            }
    
            Vector output = new Vector();
    
            //
            // Brief: Step through each RecordingListEntry which the caller has read
            // access,
            // apply the filter, add the entry to the list and so on.
    
            // Acquire the AppID and FAP based from current caller context
            // Determine whether caller has monitor access or read access
            // using determineAccessLevel helper method, then store boolean result
            // to
            // access level flag... alf.
    
            // Loop through each entry
            RecordingRequest rle;
            int numElems = clone.size();
            for (int i = 0; i < numElems; i++)
            {
                rle = (RecordingRequest) clone.elementAt(i);
                // Acquire the RecordingListEntry's FAP
                // If alf is true no need to check entry FAP
                // Tests if a particular entry passes this filter.
                // IF true is returned
                RecordingRequestImpl ri = (RecordingRequestImpl) rle;
                int state = ri.getInternalState();
                if (state == RecordingImpl.DESTROYED_STATE)
                {
                    continue;
                }
                if (filter.accept(rle) == true)
                {
                    // Add RecordingListEntry to the list to be returned
                    output.addElement(rle);
                }// Endif
                // Endif
            }// Endloop
    
            clone = null;
            return new RecordingListImpl(output);
        } // END synchronized (m_sync)
    }

    /**
     * Gets the vector of recordings
     * 
     * @return the vector of recordings stored in Navigation Manager
     * 
     */
    protected Vector getRecordingList()
    {
        return m_masterRecordingList;
    }

    /**
     * 
     * Gets any other RecordingImplInterface that overlaps with the duration of
     * this RecordingImplInterface. This method will return null unless the
     * session is in the PENDING_WITH_CONFLICTS_STATE,
     * PENDING_NO_CONFLICTS_STATE, or IN_PROGRESS_STATE. The returned list will
     * contain only Overlapping RecordingImplInterfaces for which the
     * application has read access permission. The RecordingList returned is
     * only a copy of the list of overlapping entries at the time of this method
     * call. This list is not updated if there are any changes. A new call to
     * this method will be required to get the updated list.
     * 
     */
    public OrderedRecordingSet getOverlappingReadableEntries(RecordingImplInterface recording)
            throws IllegalStateException
    {
        /**
         * non-Javadoc Gets the entire list. 'This' same list is returned if the
         * filter argument is null. Note that we have direct access to the main
         * list. Applications can only access list that have been returned to
         * them. see definition of
         * org.ocap.dvr.navigation.RecordingList.filterRecordingList
         */
        Vector outputVector = new Vector();

        synchronized (m_sync)
        {
            // Visit each element in the list
            // Test for overlap
            // If overlap add the entry to the output list

            int numElems = m_masterRecordingList.size();
            RecordingImplInterface currRle = null;

            // re: "... for which the application has read access permission"
            // N.B. According to Pat Ladd, we should use
            // RecordingPermission("read", ...) rather than File Access
            // Permissions
            boolean canReadAnyReq = false;
            boolean canReadOwnReq = false;
            AppID callerAppID = null;
            try
            {
                SecurityUtil.checkPermission(new RecordingPermission("read", "*"));
                canReadAnyReq = true;
            }
            catch (SecurityException e1)
            {
                try
                {
                    SecurityUtil.checkPermission(new RecordingPermission("read", "own"));
                    CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
                    callerAppID = (AppID) (ccm.getCurrentContext().get(CallerContext.APP_ID));
                    if (null != callerAppID)
                    {
                        canReadOwnReq = true;
                    }
                }
                catch (SecurityException e2)
                {
                }
            }

            for (int i = 0; i < numElems; i++)
            {
                currRle = (RecordingImplInterface) m_masterRecordingList.elementAt(i);
                if (!currRle.equals(recording))
                {
                    if (doesOverlapOccur(recording, currRle) == true)
                    {
                        AppID ownerAppID = currRle.getAppID();
                        if (canReadAnyReq || (canReadOwnReq && (callerAppID.equals(ownerAppID))))
                        {
                            outputVector.addElement(currRle);
                        }
                    }
                }
            }// END for
        } // END synchronized(m_sync)
        return new OrderedRecordingSet(outputVector);
    } // END getOverlappingReadableEntries()

    /**
     * 
     * Gets any other RecordingImplInterface that overlaps with the duration of
     * this RecordingImplInterface. This method will return null unless the
     * recording is in the PENDING_WITH_CONFLICTS_STATE,
     * PENDING_NO_CONFLICTS_STATE, or IN_PROGRESS_STATE. The returned list will
     * contain only Overlapping RecordingImplInterfaces for which the
     * application has read access permission. The RecordingList returned is
     * only a copy of the list of overlapping entries at the time of this method
     * call. This list is not updated if there are any changes. A new call to
     * this method will be required to get the updated list.
     * 
     */
    public OrderedRecordingSet getOverlappingEntries(RecordingImplInterface recording) throws IllegalStateException
    {
        /**
         * non-Javadoc Gets the entire list. 'This' same list is returned if the
         * filter arguement is null. Note that we have direct access to the main
         * list. Applications can only access list that have been returned to
         * them. see definition of
         * org.ocap.dvr.navigation.RecordingList.filterRecordingList
         */
        Vector outputVector = new Vector();

        synchronized (m_sync)
        {
            // Visit each element in the list
            // Test for overlap
            // If overlap add the entry to the output list

            int numElems = m_masterRecordingList.size();
            RecordingImplInterface currRle = null;

            for (int i = 0; i < numElems; i++)
            {
                currRle = (RecordingImplInterface) m_masterRecordingList.elementAt(i);

                if (currRle.equals(recording))
                {
                    continue;
                }

                if (doesOverlapOccur(recording, currRle) == false)
                {
                    continue;
                }

                // currRle is not recording and overlaps recording - add it
                // to the list
                outputVector.addElement(currRle);
            }// END for
        } // END synchronized(m_sync)

        return new OrderedRecordingSet(outputVector);
    } // END getOverlappingEntries()

    /**
     * Gets any other RecordingImplInterface that overlaps with the designated
     * recording and is in one of the states contained in the states array. The
     * set returned is only a copy of the list of overlapping entries at the
     * time of this method call and will not contain recording itself. This list
     * is not updated if there are any changes. A new call to this method will
     * be required to get new/updated entries.
     * 
     * @param recording
     *            The recording to check recordings against for overlap
     * @param states
     *            Array of states desired in the set
     */
    public OrderedRecordingSet getOverlappingEntriesInStates(final RecordingImplInterface recording, final int states[])
            throws IllegalStateException
    {
        OrderedRecordingSet ors = getOverlappingEntriesInStates(recording.getRequestedStartTime(),
                recording.getDuration(), states);

        ors.removeRecording(recording);

        return ors;
    } // END getOverlappingEntries()

    /**
     * Gets any other RecordingImplInterface that overlaps with the designated
     * timespan and is in one of the states contained in the states array. The
     * set returned is only a copy of the list of overlapping entries at the
     * time of this method call. This list is not updated if there are any
     * changes. A new call to this method will be required to get new/updated
     * entries.
     * 
     * @param startTime
     *            Start time for the timespan
     * @param duration
     *            Duration of the timespan
     * @param states
     *            Array of states desired in the set
     */
    public OrderedRecordingSet getOverlappingEntriesInStates(final long startTime, final long duration, int states[])
            throws IllegalStateException
    {
        /**
         * non-Javadoc Gets the entire list. 'This' same list is returned if the
         * filter arguement is null. Note that we have direct access to the main
         * list. Applications can only access list that have been returned to
         * them. see definition of
         * org.ocap.dvr.navigation.RecordingList.filterRecordingList
         */
        Vector outputVector = new Vector();
        final long endTime = startTime + duration;

        synchronized (m_sync)
        {
            // Visit each element in the list
            // Test for overlap
            // If overlap add the entry to the output list

            final int numElems = m_masterRecordingList.size();
            RecordingImplInterface currRle = null;

            for (int i = 0; i < numElems; i++)
            {
                currRle = (RecordingImplInterface) m_masterRecordingList.elementAt(i);

                long rle_s = currRle.getRequestedStartTime();
                long rle_e = rle_s + currRle.getDuration();

                if ((startTime >= rle_e) || (endTime <= rle_s))
                { // No overlap between currRle and timespan
                    continue;
                }

                boolean isOneOfTheStates = false;
                int currRleState = currRle.getInternalState();

                for (int j = 0; j < states.length && !isOneOfTheStates; j++)
                {
                    isOneOfTheStates = (currRleState == states[j]);
                }

                if (!isOneOfTheStates)
                {
                    continue;
                }

                // Assert: currRle is not recording, overlaps recording,
                // and is in one of the requested states - add it
                // to the list
                outputVector.addElement(currRle);
            }// END for
        } // END synchronized(m_sync)

        return new OrderedRecordingSet(outputVector);
    } // END getOverlappingEntries()

    /**
     * Get the entire set of recordings as an OrderedRecordingSet
     */
    public OrderedRecordingSet getAllEntries()
    {
        synchronized (m_sync)
        {
            final OrderedRecordingSet ors = new OrderedRecordingSet(m_masterRecordingList);
            ors.addRecordings(this.m_parentMasterRecordingList);
            return ors;
        }
    }
    
    /**
     * non-Javadoc A helper function to detect overlap of two
     * RecordingImplInterface objects.
     * 
     */
    boolean doesOverlapOccur(RecordingImplInterface rec1, RecordingImplInterface rec2)
    {
        long S = rec1.getRequestedStartTime();
        long E = S + rec1.getDuration();

        long s = rec2.getRequestedStartTime();
        long d = rec2.getDuration();
        long e = s + d;

        return ((s >= E) || (e <= S)) ? false : true;
    }

    /**
     * Returns a list of pending recordings regardless of the file access
     * permissions because the permissions may change from now till the
     * RecordingSession startTime arrives.
     * 
     * @return recordings in either PENDING_NO_CONFLICT_STATE or
     *         PENDING_WITH_CONFLICT_STATE state.
     */
    RecordingList getPendingRecordings()
    {
        Vector outputVector = new Vector();

        synchronized (m_sync)
        {
            // Visit each element in the list
            // Test for overlap
            // If overlap add the entry to the output list

            int numElems = m_masterRecordingList.size();
            RecordingImplInterface currRle = null;

            for (int i = 0; i < numElems; i++)
            {
                currRle = (RecordingImplInterface) m_masterRecordingList.elementAt(i);
                int state = currRle.getInternalState();

                if ((state == LeafRecordingRequest.PENDING_NO_CONFLICT_STATE)
                        || (state == LeafRecordingRequest.PENDING_WITH_CONFLICT_STATE))
                {
                    outputVector.addElement(currRle);
                }
            }// END for
        } // END synchronized(m_sync)

        return new OrderedRecordingSet(outputVector);
    } // END getPendingRecordings()

    /**
     * This method is called by member functions <code>insertRecording</code>
     * and <code>removeRecording</code> to deliver a
     * <code>RecordingListEvent</code> to each registered listener. The same
     * object is delivered to each listener regardless of the application.
     * 
     * Event delivery is governed by listener access to the associated
     * RecordingRequest. All events are delivered to listeners with
     * RecordingPermission("read","*"). Listeners with
     * RecordingPermission("read","own") receive only events that are accessible
     * as defined by the RecordingRequest ExtendedFileAccessPermissions.
     * 
     * Event registration is restricted to applications with
     * RecordingPermission("read",...).
     * 
     * @param event
     *            the event to be delivered.
     * 
     * @see hasReadAccess
     */
    void dispatchRecordingChangedEvent(final RecordingChangedEvent event)
    {
        // final local variables for inner class access
        final RecordingRequestImpl ri = (RecordingRequestImpl) event.getRecordingRequest();

        if (log.isDebugEnabled())
        {
            log.debug("NavigationManager: dispatchRecordingChangedEvent: Broadcasting " + event + " for " + ri );
        }
        // Loop through each caller context to find the listener
        int size = this.m_notificands.size();
        for (int i = 0; i < size; i++)
        {
            class ContextNotifier implements Runnable
            {
                private final ContextNotificand m_notificand;
                public ContextNotifier(final ContextNotificand notificand)
                {
                    m_notificand = notificand;
                }
                public void run()
                {
                    // Loop through each listener for the caller context
                    int numListeners = m_notificand.size();
                    for (int j = 0; j < numListeners; j++)
                    {
                        RecordingChangedListener rll = null;
                        try
                        {
                            rll  = (RecordingChangedListener) m_notificand.elementAt(j);
            
                            if (log.isDebugEnabled())
                            {
                                log.debug("NavigationManager: dispatchRecordingChangedEvent: Dispatching " 
                                          + event + " to  " + rll );
                            }
                            rll.recordingChanged(event);
                        }
                        catch (Throwable t)
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info( "NavigationManager: dispatchRecordingChangedEvent: "
                                          + "Caught an exception dispatching " + event 
                                          + " to  " + rll + " for " + ri, t );
                            }
                            // Continue along with the next listener
                        }
                    } // END for (through listener list)
                } // END run()
            } // END class ContextNotifier
            
            // Acquire the caller context from the notificand
            ContextNotificand notificand = (ContextNotificand) m_notificands.elementAt(i);
            CallerContext cctx = notificand.getContext();
            AppID listenerAppID = (AppID) (cctx.get(CallerContext.APP_ID));

            if ((notificand.hasGlobalEventAccess()) || (ri.hasReadAccess(listenerAppID)))
            {
                // deliver all events for this context on one of the context's threads 
                cctx.runInContextAsync(new ContextNotifier(notificand));
            }
        }

    }

    /**
     * Adds an event listener for changes in status of recording list entries.
     * The listener parameter will only be informed of changes that affect
     * entries the calling application has read file access permission to, or
     * all events if the calling application has
     * MonitorAppPermission("recording"). The listener object can be registered
     * only once.
     * 
     * @param rll
     *            The listener to be registered.
     */
    public void addRecordingChangedListener(RecordingChangedListener rcl)
    {
        addRecordingListListener(rcl);
    }

    void addRecordingListListener(RecordingChangedListener rll)
    {
        // Acquire the context of the caller
        CallerContext cctx = m_ccm.getCurrentContext();

        if (rll == null)
        {
            throw new NullPointerException();
        }

        // Prevent access to the list of listeners
        synchronized (m_sync)
        {
            // Check to see if the listener already exists, if so
            // a context notificand will be found and returned.
            // Then we'll look up the listener in the ContextNotificand
            ContextNotificand notif = getContextNotificand(cctx);

            // If calling application has a listener in the list
            // Then get the ContextNotificand which holds listeners.
            // Else create a new contextNotificandis already in the list,
            // therefore has a listener object already registered.
            if (notif != null)
            {
                // If the same listener object is registered, return;
                // Else else add the listener.
                if (notif.contains(rll))
                {
                    return;
                }
                else
                {
                    notif.addElement(rll);
                }

            }
            else
            {
                // Create a new ContextNotificand
                notif = new ContextNotificand(cctx);

                // add the listener to the new notificand
                // and then add the notificand to the notificand list
                notif.addElement(rll);
                m_notificands.addElement(notif);

                // Add object to signify owned resources. It is only added
                // once.
                // Check if the Callback data which is used for monitoring is
                // installed.
                if (cctx.getCallbackData(m_instance) == null)
                {
                    cctx.addCallbackData(new Client(), m_instance);
                }
            }
        }// Endsync
    }

    /**
     * Supports the ECN 829 RecordingID stuff.
     * 
     * @param id
     *            the recording id
     * @return a recording request or null if the key doesn't map to a value.
     */
    RecordingRequest getRecordingRequest(int id)
    {
        synchronized (m_sync)
        {
            return (RecordingRequest) m_recordingIDHash.get(new Integer(id));
        }
    }

    /**
     * This method returns the <code>ContextNotificand</code> associated with a
     * CallerContext or null if cctx is not present.
     * 
     * @param cctx
     * @return The associated object representing for the specified cctx object.
     */
    ContextNotificand getContextNotificand(CallerContext cctx)
    {
        // Loop ContextNotificands
        int numCNotifs = this.m_notificands.size();
        for (int i = 0; i < numCNotifs; i++)
        {
            // compare current and target objects
            ContextNotificand notif = (ContextNotificand) m_notificands.elementAt(i);
            // If objects match
            // Return the ContextNotificand object
            CallerContext target = notif.getContext();
            if (cctx == target)
            {
                return notif;
            }
            // Continue
        }// Endloop
        return null;
    }

    /**
     * Removes a registed event listener for changes in status of recording list
     * entries. If the listener specified is not registered then this method has
     * no effect.
     * 
     * @param rll
     *            the listener to be removed.
     */
    public void removeRecordingChangedListener(RecordingChangedListener rcl)
    {
        removeRecordingListListener(rcl);
    }

    void removeRecordingListListener(RecordingChangedListener rll)
    {
        if (rll == null) throw new NullPointerException();

        // Acquire the execution context of this thread
        CallerContext cctx = m_ccm.getCurrentContext();
        // Prevent access to the list
        synchronized (m_sync)
        {
            // The context notificand will be returned if any listener
            // is in the list, otherwise nothing will be returned.
            ContextNotificand notif = getContextNotificand(cctx);

            if (notif == null)
            {
                return;
            }
            else
            {
                // If listener is found remove it
                if (notif.contains(rll) == true)
                {
                    // If removed and there are no more listeners
                    // Then remove the notificand from the list of
                    // context notificands.
                    if (notif.removeElement(rll) == true)
                    {
                        if (notif.size() == 0)
                        {
                            m_notificands.removeElement(notif);
                        }

                    }
                }
            }
        }// Endsync
    }

    void releaseClientResources(CallerContext ctx)
    {
        // The context notificand will be returned if any listener
        // is in the list, otherwise nothing will be returned.
        synchronized (m_sync)
        {
            ContextNotificand notif = getContextNotificand(ctx);
    
            if (notif != null)
            {
                notif.removeAllElements();
                m_notificands.removeElement(notif);
            }
        }
    }

    /*
     * Returns <code>true</code> if the caller has read access to the recording
     * request.
     * 
     * @param rr the <code>RecordingRequest</code> whose accessibility is being
     * tested.
     * 
     * @see hasReadAccess(RecordingRequest rr, OcapSecurityManager osm, AppID
     * id)
     */
    private boolean hasReadAccess(RecordingRequestImpl ri)
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        if (null != ccm)
        {
            return ri.hasReadAccess((AppID) (ccm.getCurrentContext().get(CallerContext.APP_ID)));
        }
        else
        {
            SystemEventUtil.logRecoverableError(new Exception("Failed to obtain Object ref - CallerContextManager ("
                    + ccm + ")"));
            return false;
        }
    }

    void setStateChanged()
    {
        m_stateCounter = (m_stateCounter + 1) % 0x7FFFFFFF;
    }

    int getStateCounter()
    {
        return m_stateCounter;
    }

    /**
     * An object of this type represents a mapping of listeners to a
     * CallerContext.
     */
    public class ContextNotificand extends Vector
    {
        /**
         * <code>m_context</code> a reference to the application context.
         */
        private CallerContext m_context;

        /*
         * We cannot easily check RecordingPermission("read",...) for a
         * registered listener when delivering events, so we record the
         * listener's global event access at registration.
         */
        private boolean m_hasGlobalEventAccess;

        /**
         * Constructor
         * 
         * @param cctx
         *            the context used to initialize an instance of this class.
         */
        ContextNotificand(CallerContext cctx)
        {
            m_context = cctx;
            try
            {
                SecurityUtil.checkPermission(new RecordingPermission("read", "*"));
                m_hasGlobalEventAccess = true;
            }
            catch (SecurityException e)
            {
            }
        }

        /**
         * Returns this stored caller context field for registered listeners.
         * 
         * @return the store CallerContext
         */
        CallerContext getContext()
        {
            return m_context;
        }

        /**
         * Returns the stored caller event access flag for registered listeners.
         * 
         * @return <code>true</code> if the stored caller can access all events.
         *         Otherwise, return <code>false</code>.
         */
        boolean hasGlobalEventAccess()
        {
            return m_hasGlobalEventAccess;
        }

    } // END class ContextNotifican

    // Log4J Logger
    private static final Logger log = Logger.getLogger(NavigationManager.class.getName());

}
