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

package org.davic.mpeg.sections;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.SectionFilterManager;
import org.cablelabs.impl.manager.SectionFilterManager.Filter;
import org.cablelabs.impl.manager.SectionFilterManager.FilterCallback;
import org.cablelabs.impl.manager.SectionFilterManager.FilterSpec;
import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.SystemEventUtil;

import javax.tv.util.TVTimer;
import javax.tv.util.TVTimerScheduleFailedException;
import javax.tv.util.TVTimerSpec;
import javax.tv.util.TVTimerWentOffEvent;
import javax.tv.util.TVTimerWentOffListener;

import org.apache.log4j.Logger;
import org.davic.mpeg.NotAuthorizedException;
import org.davic.mpeg.TransportStream;
import org.davic.mpeg.TuningException;

/**
 * This class is the base class for a set of classes describing section filters
 * with different characteristics of life cycle and buffering.
 * 
 * When a SectionFilterGroup is detached, either by the client or through
 * resource withdrawal, started SectionFilters shall remain started. Hence if
 * the <code>SectionFilterGroup</code> is re-attached, those filters shall
 * re-activate.
 * 
 * @version Updated to DAVIC 1.4.1p9
 */
public abstract class SectionFilter
{
    /**
     * Defines a SectionFilter object as filtering only for sections matching a
     * specific PID. If the parent SectionFilterGroup is attached to a
     * TransportStream then filtering will start immediately.
     * 
     * @param appData
     *            An object supplied by the application. This object will be
     *            delivered to the subscribed section filter listener as part of
     *            all SectionFilterEvents that will be generated because of this
     *            method call. The application can use this object for internal
     *            communication purposes. If the application does not need any
     *            application data, the parameter can be null.
     * @param pid
     *            the value of the PID to filter for incoming sections
     * @exception FilterResourceException
     *                if all the number of started SectionFilters for the parent
     *                SectionFilterGroup is already equal to the number of
     *                section filters associated with the SectionFilterGroup
     *                when it was created. Note that this is applied whether the
     *                parent section filter group is connected to a TS or not.
     * @exception org.davic.mpeg.NotAuthorizedException
     *                if the information requested is scrambled and permission
     *                to descramble it is refused.
     * @exception IllegalFilterDefinitionException
     *                if called for a TableSectionFilter.
     * @exception ConnectionLostException
     *                if the parent SectionFilterGroup is in the ConnectionLost
     *                state and hence is unable to satisfy the method call due
     *                to absence of resources or absence of sections to filter.
     */
    public void startFiltering(Object appData, int pid) throws FilterResourceException, NotAuthorizedException,
            IllegalFilterDefinitionException, ConnectionLostException
    {
        startFiltering(false, appData, pid, -1, DAVIC_MIN_OFFSET, null, null, null, null);
    }

    /**
     * Defines a SectionFilter object as filtering only for sections matching a
     * specific PID and table_id. If the parent SectionFilterGroup is attached
     * to a TransportStream then filtering will start immediately.
     * 
     * @param appData
     *            An object supplied by the application. This object will be
     *            delivered to the subscribed section filter listener as part of
     *            all SectionFilterEvents that will be generated because of this
     *            method call. The application can use this object for internal
     *            communication purposes. If the application does not need any
     *            application data, the parameter can be null.
     * @param pid
     *            the value of the PID to filter for incoming sections
     * @param tableID
     *            the value of the table_id to filter for in incoming sections
     * @exception FilterResourceException
     *                if all the number of started SectionFilters for the parent
     *                SectionFilterGroup is already equal to the number of
     *                section filters associated with the SectionFilterGroup
     *                when it was created. Note that this is applied whether the
     *                parent section filter group is connected to a TS or not.
     * @exception org.davic.mpeg.NotAuthorizedException
     *                if the information requested is scrambled and permission
     *                to descramble it is refused.
     * @exception ConnectionLostException
     *                if the parent SectionFilterGroup is in the ConnectionLost
     *                state and hence is unable to satisfy the method call due
     *                to absence of resources or absence of sections to filter.
     * @exception IllegalFilterDefinitionException
     *                where either integer is negative or larger than allowed by
     *                the MPEG specification
     */
    public void startFiltering(Object appData, int pid, int tableID) throws FilterResourceException,
            NotAuthorizedException, ConnectionLostException, IllegalFilterDefinitionException
    {
        startFiltering(appData, pid, tableID, DAVIC_MIN_OFFSET, null, null, null, null);
    }

    /**
     * Defines a SectionFilter object as filtering only for sections matching a
     * specific PID and table_id, and where contents of the section match the
     * specified filter pattern. The first byte of each array corresponds to the
     * third byte of the section. If the parent SectionFilterGroup is attached
     * to a TransportStream then filtering will start immediately.
     * 
     * @param appData
     *            An object supplied by the application. This object will be
     *            delivered to the subscribed section filter listener as part of
     *            all <code>SectionFilterEvents</code> that will be generated
     *            because of this method call. The application can use this
     *            object for internal communication purposes. If the application
     *            does not need any application data, the parameter can be null.
     * @param pid
     *            the value of the PID to filter for incoming sections.
     * @param tableID
     *            the value of the table_id field to filter for ncoming sections
     * @param posFilterDef
     *            defines values to match for bits in the section, as defined in
     *            clause E.8.1.
     * @param posFilterMask
     *            defines which bits in the section are to be compared against
     *            the values specified in the posFilterDef parameter, as defined
     *            in clause E.8.1.
     * @exception FilterResourceException
     *                if all the number of started SectionFilters for the parent
     *                SectionFilterGroup is already equal to the number of
     *                section filters associated with the SectionFilterGroup
     *                when it was created. Note that this is applied whether the
     *                parent section filter group is connected to a TS or not.
     * @exception IllegalFilterDefinitionException
     *                the filter definition specified is illegal either because
     *                the posFilterDef and posFilterMask arrays are of different
     *                sizes or because their length is beyond the filtering
     *                capacity of the system.
     * @exception org.davic.mpeg.NotAuthorizedException
     *                if the information requested is scrambled and permission
     *                to descramble it is refused.
     * @exception ConnectionLostException
     *                if the parent SectionFilterGroup is in the ConnectionLost
     *                state and hence is unable to satisfy the method call due
     *                to absence of resources or absence of sections to filter.
     */
    public void startFiltering(Object appData, int pid, int tableID, byte posFilterDef[], byte posFilterMask[])
            throws FilterResourceException, IllegalFilterDefinitionException, NotAuthorizedException,
            ConnectionLostException
    {
        startFiltering(appData, pid, tableID, DAVIC_MIN_OFFSET, posFilterDef, posFilterMask, null, null);
    }

    /**
     * Defines a SectionFilter object as filtering only for sections matching a
     * specific PID and table_id, and where contents of the section match the
     * specified filter pattern. If the parent SectionFilterGroup is attached to
     * a TransportStream then filtering will start immediately.
     * 
     * @param appData
     *            An object supplied by the application. This object will be
     *            delivered to the subscribed section filter listener as part of
     *            all <code>SectionFilterEvents</code> that will be generated
     *            because of this method call. The application can use this
     *            object for internal communication purposes. If the application
     *            does not need any application data, the parameter can be null.
     * @param pid
     *            the value of the PID to filter for incoming sections.
     * @param tableID
     *            the value of the table_id field to filter for incoming
     *            sections
     * @param offset
     *            defines the offset within the section which the first byte of
     *            the posFilterDef and posFilterMask arrays is intended to
     *            match. The offset must be less than 31 as described in DAVIC
     *            part 10, section 115.3. The offset must be equal to or greater
     *            than 3.
     * @param posFilterDef
     *            defines values to match for bits in the section, as defined in
     *            clause E.8.1.
     * @param posFilterMask
     *            defines which bits in the section are to be compared against
     *            the values specified in the posFilterDef parameter, as defined
     *            in clause E.8.1.
     * @exception FilterResourceException
     *                if all the number of started SectionFilters for the parent
     *                SectionFilterGroup is already equal to the number of
     *                section filters associated with the SectionFilterGroup
     *                when it was created. Note that this is applied whether the
     *                parent section filter group is connected to a TS or not.
     * @exception IllegalFilterDefinitionException
     *                the filter definition specified is illegal either because
     *                the posFilterDef and posFilterMask arrays are not the same
     *                size or because their length is beyond the filtering
     *                capacity of the system or because the specified offset is
     *                too large.
     * @exception org.davic.mpeg.NotAuthorizedException
     *                if the information requested is scrambled and permission
     *                to descramble it is refused.
     * @exception ConnectionLostException
     *                if the parent SectionFilterGroup is in the ConnectionLost
     *                state and hence is unable to satisfy the method call due
     *                to absence of resources or absence of sections to filter.
     */
    public void startFiltering(Object appData, int pid, int tableID, int offset, byte posFilterDef[],
            byte posFilterMask[]) throws FilterResourceException, IllegalFilterDefinitionException,
            NotAuthorizedException, ConnectionLostException
    {
        startFiltering(appData, pid, tableID, offset, posFilterDef, posFilterMask, null, null);
    }

    /**
     * Defines a SectionFilter object as filtering only for sections matching a
     * specific PID and table_id, and where contents of the section match the
     * specified filter pattern. The first byte of each array corresponds to the
     * third byte of the section. If the parent SectionFilterGroup is attached
     * to a TransportStream then filtering will start immediately.
     * 
     * @param appData
     *            An object supplied by the application. This object will be
     *            delivered to the subscribed section filter listener as part of
     *            all <code>SectionFilterEvents</code> that will be generated
     *            because of this method call. The application can use this
     *            object for internal communication purposes. If the application
     *            does not need any application data, the parameter can be null.
     * @param pid
     *            the value of the PID to filter for incoming sections.
     * @param tableID
     *            the value of the table_id field to filter for incoming
     *            sections
     * @param posFilterDef
     *            defines values to match for bits in the section, as defined in
     *            clause E.8.1.
     * @param posFilterMask
     *            defines which bits in the section are to be compared against
     *            the values specified in the posFilterDef parameter, as defined
     *            in clause E.8.1.
     * @param negFilterDef
     *            defines values to match for bits in the section, as defined in
     *            clause E.8.1.
     * @param negFilterMask
     *            defines which bits in the section are to be compared against
     *            the values specified in the negFilterDef parameter, as defined
     *            in clause E.8.1.
     * @exception FilterResourceException
     *                if all the number of started SectionFilters for the parent
     *                SectionFilterGroup is already equal to the number of
     *                section filters associated with the SectionFilterGroup
     *                when it was created. Note that this is applied whether the
     *                parent section filter group is connected to a TS or not.
     * @exception IllegalFilterDefinitionException
     *                the filter definition specified is illegal either because
     *                the arrays posFilterDef, posFilterMask, negFilterDef,
     *                negFilterMask are not all the same size or because their
     *                length is beyond the filtering capacity of the system.
     * @exception org.davic.mpeg.NotAuthorizedException
     *                if the information requested is scrambled and permission
     *                to descramble it is refused.
     * @exception ConnectionLostException
     *                if the parent SectionFilterGroup is in the ConnectionLost
     *                state and hence is unable to satisfy the method call due
     *                to absence of resources or absence of sections to filter.
     */
    public void startFiltering(Object appData, int pid, int tableID, byte posFilterDef[], byte posFilterMask[],
            byte negFilterDef[], byte negFilterMask[]) throws FilterResourceException,
            IllegalFilterDefinitionException, NotAuthorizedException, ConnectionLostException
    {
        startFiltering(appData, pid, tableID, DAVIC_MIN_OFFSET, posFilterDef, posFilterMask, negFilterDef,
                negFilterMask);
    }

    /**
     * Defines a SectionFilter object as filtering only for sections matching a
     * specific PID and table_id, and where contents of the section match the
     * specified filter pattern. If the parent
     * <code>SectionFilterGroup<code> is attached to a
     * <code>TransportStream</code> then filtering will start immediately.
     * 
     * @param appData
     *            An object supplied by the application. This object will be
     *            delivered to the subscribed section filter listener as part of
     *            all <code>SectionFilterEvents</code> that will be generated
     *            because of this method call. The application can use this
     *            object for internal communication purposes. If the application
     *            does not need any application data, the parameter can be null.
     * @param pid
     *            the value of the PID to filter for incoming sections.
     * @param tableID
     *            the value of the table_id field to filter for incoming
     *            sections
     * @param offset
     *            defines the offset within the section which the first byte of
     *            the posFilterDef, posFilterMask, negFilterDef and
     *            negFilterMask arrays is intended to match. The offset must be
     *            less than 31 as described in DAVIC part 10, section 115.3. The
     *            offset must be equal to or greater than 3.
     * @param posFilterDef
     *            defines values to match for bits in the section, as defined in
     *            clause E.8.1.
     * @param posFilterMask
     *            defines which bits in the section are to be compared against
     *            the values specified in the posFilterDef parameter, as defined
     *            in clause E.8.1.
     * @param negFilterDef
     *            defines values to match for bits in the section, as defined in
     *            clause E.8.1.
     * @param negFilterMask
     *            defines which bits in the section are to be compared against
     *            the values specified in the negFilterDef parameter, as defined
     *            in clause E.8.1.
     * @exception FilterResourceException
     *                if all the number of started SectionFilters for the parent
     *                <code>SectionFilterGroup</code> is already equal to the
     *                number of section filters associated with the
     *                SectionFilterGroup when it was created. Note that this is
     *                applied whether the parent section filter group is
     *                connected to a TS or not.
     * @exception IllegalFilterDefinitionException
     *                the filter definition specified is illegal either because
     *                the posFilterDef, posFilterMask, negFilterDef,
     *                negFilterMask arrays are not all the same size or because
     *                their length is beyond the filtering capacity of the
     *                system or because the specified offset is too large.
     * @exception org.davic.mpeg.NotAuthorizedException
     *                if the information requested is scrambled and permission
     *                to descramble it is refused.
     * @exception ConnectionLostException
     *                if the parent SectionFilterGroup is in the ConnectionLost
     *                state and hence is unable to satisfy the method call due
     *                to absence of resources or absence of sections to filter.
     */
    public void startFiltering(Object appData, int pid, int tableID, int offset, byte posFilterDef[],
            byte posFilterMask[], byte negFilterDef[], byte negFilterMask[]) throws FilterResourceException,
            IllegalFilterDefinitionException, NotAuthorizedException, ConnectionLostException
    {
        startFiltering(true, appData, pid, tableID, offset, posFilterDef, posFilterMask, negFilterDef, negFilterMask);
    }

    private void startFiltering(boolean isTableFilter, Object appData, int pid, int tableID, int offset,
            byte posFilterDef[], byte posFilterMask[], byte negFilterDef[], byte negFilterMask[])
            throws FilterResourceException, IllegalFilterDefinitionException, NotAuthorizedException,
            ConnectionLostException
    {
        if (pid < 0 || pid > 0x1fff)
            throw new IllegalFilterDefinitionException("pid value should be between 0 and 0x1fff");

        // table id needs to be between 0 and 0xfe
        if (isTableFilter)
            if (tableID < 0 || tableID > 0xfe)
                throw new IllegalFilterDefinitionException("table_id value should be between 0 and 0xfe");

        if (offset < DAVIC_MIN_OFFSET || offset >= DAVIC_MAX_OFFSET)
            throw new IllegalFilterDefinitionException("offset must be greater than or equal to " + DAVIC_MIN_OFFSET
                    + " and less than " + DAVIC_MAX_OFFSET);

        // Make sure they are both set or both null.
        if ((negFilterMask != null) != (negFilterDef != null))
            throw new IllegalFilterDefinitionException("negative filter and mask must both have values or both be null");

        // If we made it here, we know that both are either set or null.
        // Check one of the values to make sure it's set and then validate
        // equal lengths.
        if (negFilterMask != null)
        {
            if (negFilterMask.length != negFilterDef.length)
                throw new IllegalFilterDefinitionException("negative filter and mask must be the same length");
        }

        // Do the same test for positive that we did for negative.
        if ((posFilterMask != null) != (posFilterDef != null))
            throw new IllegalFilterDefinitionException("positive filter and mask must both have values or both be null");
        if (posFilterMask != null)
        {
            if (posFilterMask.length != posFilterDef.length)
                throw new IllegalFilterDefinitionException("positive filter and mask must be the same length");
        }

        // Now we need to validate that the positive and negative filters are of
        // equal length if they are both specified.
        if ((posFilterDef != null) && (negFilterDef != null))
        {
            if (posFilterDef.length != negFilterDef.length)
                throw new IllegalFilterDefinitionException(
                        "positive filter and negative filter must be the same length");
        }

        // Make sure that the length + offset <= DAVIC_MAX_FILTER_LEN (defined
        // above).
        // If it is not, then we will throw an exception.
        int length = Math.max((posFilterDef != null ? posFilterDef.length : 0),
                (negFilterDef != null ? negFilterDef.length : 0));
        if ((length + offset) > DAVIC_MAX_FILTER_LEN)
            throw new IllegalFilterDefinitionException("the filter length plus offset must be equal to or less than "
                    + DAVIC_MAX_FILTER_LEN);

        FilterSession newFilter = new FilterSession(pid, tableID, offset, posFilterMask, posFilterDef, negFilterMask,
                negFilterDef, appData, sectionSizeLimit);

        synchronized (controller.groupLock)
        {
            if (state == SF_STATE_RUNNING) controller.stopFilter(false);

            if (controller.startFilter(newFilter) == SectionFilterGroup.SFG_STATE_DISCONNECTED)
            {
                filterSession = newFilter;
                setState(SF_STATE_PENDING_RUN);
            }
        }
    }

    /**
     * If the parent <code>SectionFilterGroup</code> is attached to a
     * <code>TransportStream</code> then filtering for sections matching this
     * <code>SectionFilter</code> object will stop. If the parent is not
     * attached then should it become attached, filtering for sections matching
     * this <code>SectionFilter<code> object will not start.
     */
    public void stopFiltering()
    {
        controller.stopFilter(false);
    }

    /**
     * Sets the time-out for this section filter. When the time-out happens, a
     * TimeOutEvent will be generated and sent to the SectionFilter object and
     * filtering stops. For a <code>SimpleSectionFilter</code> this will be
     * generated if no sections arrive within the specified period. For a
     * <code>TableSectionFilter</code>, this will be generated if the complete
     * table does not arrive within the specified time. For a
     * <code>RingSectionFilter</code>, this will be generated if the specified
     * time has elapsed since the arrival of the last section being successfully
     * filtered. Setting a time-out of 0 milliseconds has the effect of removing
     * a possible time-out. A set time-out only applies to subsequent filter
     * activations, not to a possible filter activation that is currently in
     * progress when the call to this method is made. The default time-out value
     * is 0.
     * 
     * @param milliseconds
     *            the time out period
     * @exception IllegalArgumentException
     *                if the 'milliseconds' parameter is negative
     */
    public void setTimeOut(long milliseconds) throws IllegalArgumentException
    {
        if (milliseconds < 0) throw new IllegalArgumentException(milliseconds + " is negative");

        timeOut = milliseconds;

        // If the new timeOut is 0, then the timeout should be cancelled.
        // If it's not running, then this is a benign call.
        if (timeOut == 0)
            timer.deschedule(timerSpec);
        else
            timerSpec.setDelayTime(timeOut);
    }

    /**
     * Specifies an object to be notified of events relating to this
     * <code>SectionFilter</code> object.
     * 
     * @param listener
     *            the object to be notified of events
     */
    public synchronized void addSectionFilterListener(SectionFilterListener listener)
    {
        // SF is private to a single app, so public sync is okay
        listeners = EventMulticaster.add(listeners, listener);
    }

    /**
     * Indicates that an object is no longer to be notified of events relating
     * to this <code>SectionFilter</code> object. If the object was not
     * specified as to be notified then this method has no effect.
     * 
     * @param listener
     *            the object no longer to be notified of events
     */
    public synchronized void removeSectionFilterListener(SectionFilterListener listener)
    {
        // SF is private to a single app, so public sync is okay
        listeners = EventMulticaster.remove(listeners, listener);
    }

    /**
     * Construct a new SectionFilter.
     * 
     * @param group
     *            the <code>SectionFilterGroup</code> that this filter is to be
     *            associated with
     * @param numberOfMatches
     *            the number of times this section filter should match a section
     *            based on its filtering criteria
     */
    SectionFilter(SectionFilterGroup group, int numberOfMatches, int sectionSize)
    {
        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        context = ccm.getCurrentContext();

        this.timesToMatch = numberOfMatches;

        this.sectionSizeLimit = sectionSize;

        timerSpec.setAbsolute(false);
        timerSpec.setRepeat(false);
        timerSpec.addTVTimerWentOffListener(timerListener);

        /**
         * The creation of this controller links this section filter to a
         * <code>SectionFilterGroup</code>. Calls to startFilter() and
         * stopFilter() are first passed through the group. Then, if the
         * operation is allowed, these functions are called to continue the
         * process.
         */
        controller = group.new FilterController()
        {
            // Description copied from SectionFilterGroup.FilterController
            public void doStartFilter(Object session, boolean isInBand, int tunerId, int frequency,
                    TransportStream transportStream, int priority) throws FilterResourceException,
                    InvalidSourceException, IllegalFilterDefinitionException, TuningException, NotAuthorizedException
            {
                synchronized (groupLock)
                {
                    // We are already running with the given session, so do
                    // nothing
                    if (state == SF_STATE_RUNNING)
                    {
                        // If we are calling a redundant restart on this filter,
                        // then
                        // just return
                        if (session == null || session == filterSession) return;

                        // Otherwise, we must be trying to restart this running
                        // filter
                        // on another PID, so stop the current filter
                        stopFiltering();
                    }

                    // Call the filter-specific start code
                    handleStart();

                    // Attempt to start (and reserve) the native section filter
                    FilterSession fs = (session == null) ? filterSession : (FilterSession) session;
                    try
                    {
                        fs.start(isInBand, tunerId, frequency, transportStream, priority);
                        filterSession = fs;
                        setState(SF_STATE_RUNNING);
                    }
                    catch (FilterResourceException e)
                    {
                        setState(SF_STATE_PENDING_RUN);
                        throw e;
                    }
                    catch (InvalidSourceException e)
                    {
                        setState(SF_STATE_STOPPED);
                        throw e;
                    }
                    catch (IllegalFilterDefinitionException e)
                    {
                        setState(SF_STATE_STOPPED);
                        throw e;
                    }
                    catch (TuningException e)
                    {
                        setState(SF_STATE_STOPPED);
                        throw e;
                    }
                    catch (org.cablelabs.impl.davic.mpeg.NotAuthorizedException e)
                    {
                        setState(SF_STATE_STOPPED);
                        e.setElementaryStreams(transportStream, fs.pid);
                        throw e;
                    }
                }
            }

            // Description copied from SectionFilterGroup.FilterController
            public void doStopFilter(boolean detached)
            {
                synchronized (groupLock)
                {
                    if (state == SF_STATE_RUNNING && filterSession != null)
                    {
                        // Call the filter-specific stop code
                        handleStop();

                        // Stop our native filter session
                        filterSession.stop();

                        // If this section filter was stopped because its group
                        // was
                        // detached, it must be auto-started when the group is
                        // re-attached
                        if (detached)
                        {
                            setState(SF_STATE_PENDING_RUN);
                            notifySectionFilterListener(new IncompleteFilteringEvent(SectionFilter.this,
                                    filterSession.appData));
                        }
                        else
                        {
                            setState(SF_STATE_STOPPED);
                        }
                    }
                    else if (state == SF_STATE_PENDING_RUN && !detached)
                    {
                        handleStop();
                        setState(SF_STATE_STOPPED);
                    }
                }
            }
        };
    }

    /**
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     * 
     * @param e
     *            the event to deliver
     */
    protected void notifySectionFilterListener(final SectionFilterEvent e)
    {
        context.runInContext(new Runnable()
        {
            public void run()
            {
                SectionFilterListener l = listeners;
                if (l != null)
                {
                    // Notify dispatcher of section
                    if (log.isDebugEnabled())
                    {
                        log.debug("notifySectionFilterListener: run() calling listener to update");
                    }
                    l.sectionFilterUpdate(e);
                }

            }
        });
    }

    /**
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i> This method delivers the events
     * synchronizing on the provided lock
     * 
     * @param e
     *            the event to deliver
     * @param lock
     *            the object on which the event delivery has to be synchronized
     */
    protected void notifySectionFilterListener(final SectionFilterEvent e, final Object lock)
    {
        context.runInContext(new Runnable()
        {
            public void run()
            {
                SectionFilterListener l = listeners;
                if (l != null && lock != null) synchronized (lock)
                {
                    l.sectionFilterUpdate(e);
                }
            }
        });
    }

    /**
     * Concrete section filter classes will implement this method to provide
     * filter-specific actions to take place when this section filter is about
     * to start filtering
     */
    void handleStart()
    {
    }

    /**
     * Concrete section filter classes will implement this method to provide
     * filter-specific actions to take place when this section filter receives
     * an MPEG-2 private section
     * 
     * @param nativeSection
     *            the native handle that uniquely identifies the section that
     *            has just been received
     * @param appData
     *            TODO
     */
    void handleSection(Section nativeSection, Object appData)
    {
    }

    /**
     * Concrete section filter classes will implement this method to provide
     * filter-specific actions to take place when this section filter is stopped
     */
    void handleStop()
    {
    }

    /**
     * Returns whether or not this section filter is currently running and
     * actively filtering sections
     * 
     * @return true if this section filter is running, false otherwise
     */
    boolean isRunning()
    {
        return state == SF_STATE_RUNNING;
    }

    // SectionFilter timer
    protected TVTimer timer = TVTimer.getTimer();

    protected TVTimerSpec timerSpec = new TVTimerSpec();

    protected long timeOut = 0;

    private TVTimerWentOffListener timerListener = new TVTimerWentOffListener()
    {
        // Called when the TVTimer goes off to stop filtering and send timeout
        // event
        public void timerWentOff(TVTimerWentOffEvent e)
        {
            // Send out the timeout message
            synchronized (controller.groupLock)
            {
                if (state == SF_STATE_RUNNING && filterSession != null)
                    notifySectionFilterListener(new TimeOutEvent(SectionFilter.this, filterSession.appData));
            }

            stopFiltering();
        }
    };

    /**
     * Update our current state
     * 
     * @param newState
     *            the new state
     */
    private void setState(int newState)
    {
        // Only change the state if it is different
        if (state == newState) return;

        state = newState;

        switch (newState)
        {
            case SF_STATE_STOPPED:
                timer.deschedule(timerSpec);
                filterSession = null;
                break;

            case SF_STATE_RUNNING:
                if (timeOut != 0)
                {
                    try
                    {
                        timerSpec = timer.scheduleTimerSpec(timerSpec);
                    }
                    catch (TVTimerScheduleFailedException e)
                    {
                    }
                }
                break;
        }
    }

    // SectionFilter states
    private static final int SF_STATE_RUNNING = 0;

    private static final int SF_STATE_PENDING_RUN = 1;

    private static final int SF_STATE_STOPPED = 2;

    private int state = SF_STATE_STOPPED;

    private SectionFilterGroup.FilterController controller;

    /**
     * Multicaster for executing SectionFilterListener.
     */
    private SectionFilterListener listeners = null;

    private CallerContext context = null;

    private static final Logger log = Logger.getLogger(SectionFilter.class);

    // Contants to enumerate special "times to match" values
    static final int FILTER_RUN_TILL_CANCELED = 0;

    static final int FILTER_ONE_SHOT = 1;

    /**
     * Callback that is notified of <code>Section</code>s and errors for an
     * outstanding <code>FilterSession</code>.
     * 
     * @author Aaron Kamienski
     */
    private FilterCallback callback = new FilterCallback()
    {
        public void notifySection(FilterSpec source, Section s, boolean last)
        {
            // If section corresponds to currently started filter...
            if (source == filterSession)
            {
                handleSection(s, ((FilterSession) source).appData);
                if (last) controller.stopFilter(false);
            }
            // This is a stray event which shouldn't be propogated.
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Ignoring stray section for " + source);
                }
                s.setEmpty();
            }
        }

        public void notifyCanceled(FilterSpec source, int reason)
        {
            if (source == filterSession)
            {
                switch (reason)
                {
                    case REASON_CANCELED:
                        // TODO: Can this be cancelled outside our control?
                        // Should we tell controller?
                        break;

                    case REASON_UNKNOWN:
                        SystemEventUtil.logRecoverableError(new Exception("Unknown event received: " + reason));
                    case REASON_CLOSED:
                    case REASON_PREEMPTED:
                        synchronized (controller.groupLock)
                        {
                            if (state == SF_STATE_RUNNING)
                                controller.stopFilter(true);
                        }
                        break;
                    case REASON_CA:
                        synchronized (controller.groupLock)
                        {
                            if (state == SF_STATE_RUNNING)
                                controller.stopFilter(true);
                            // Notify EndOfFilteringEvent
                            notifySectionFilterListener(new EndOfFilteringEvent(SectionFilter.this, ((FilterSession) source).appData));
                        }
                        break;
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Ignoring stray event for " + source);
                }
        }
        }
    };

    /**
     * Represents a currently active filtering operation.
     * 
     * @author Aaron Kamienski
     */
    class FilterSession extends FilterSpec
    {
        /**
         * Creates an instance of <code>FilterSession</code> to be used as a
         * <code>FilterSpec</code>.
         * 
         * @param pid
         *            the id of the elementary stream upon which to apply the
         *            filter
         * @param tableId
         *            the id of the table sections to filter
         * @param offset
         *            offset within section to apply filter/mask (&gt;= 3)
         * @param posMask
         *            positive filter mask
         * @param posFilter
         *            positive filter
         * @param negMask
         *            negative filter mask
         * @param negFilter
         *            negative filter
         */
        FilterSession(int pid, int tableId, int offset, byte[] posMask, byte[] posFilter, byte[] negMask,
                byte[] negFilter, Object appData, int sectionSize)
        {
            this.pid = pid;
            this.appData = appData;
            this.timesToMatch = SectionFilter.this.timesToMatch;
            this.sectionSize = sectionSize;

            boolean hasTableId = tableId >= 0;
            this.posMask = saveArray(offset, posMask);
            this.posFilter = saveArray(offset, posFilter);
            this.negMask = saveArray(offset, negMask);
            this.negFilter = saveArray(offset, negFilter);
            if (hasTableId)
            {
                if (this.posMask == null)
                {
                    this.posMask = new byte[1];
                    this.posFilter = new byte[1];
                }
                this.posMask[0] = (byte) 0xFF;
                this.posFilter[0] = (byte) tableId;
            }
        }

        /**
         * Start this filter.
         * 
         * @param isInBand
         * @param tunerId
         * @param frequency
         * @param transportStream
         * @param priority
         * 
         * @throws InvalidSourceException
         * @throws FilterResourceException
         * @throws TuningException
         * @throws NotAuthorizedException
         * @throws IllegalFilterDefinitionException
         */
        void start(boolean isInBand, int tunerId, int frequency, TransportStream transportStream, int priority)
                throws InvalidSourceException, FilterResourceException, TuningException, NotAuthorizedException,
                IllegalFilterDefinitionException
        {
            this.isInBand = isInBand;
            this.priority = priority;
            if (isInBand)
            {
                this.tunerId = tunerId;
                this.frequency = frequency;
                this.transportStream = transportStream;
            }

            SectionFilterManager api = (SectionFilterManager) ManagerManager.getInstance(SectionFilterManager.class);

            filter = api.startFilter(this, callback);
        }

        /**
         * Stop this filter.
         */
        void stop()
        {
            Filter tmp = filter;
            filter = null;
            if (tmp != null)
            {
                tmp.cancel();
            }
        }

        /**
         * Overrides {@link Object#toString()}, returning a <code>String</code>
         * representation of this filter session.
         */
        public String toString()
        {
            StringBuffer sb = new StringBuffer("FilterSession");

            sb.append(System.identityHashCode(this)).append('[');
            if (isInBand)
            {
                sb.append("freq=").append(frequency);
                sb.append(",transportStream=").append(transportStream);
                sb.append(",tuner=").append(tunerId);
                sb.append(",sectionSize=").append(sectionSize);
            }
            else
            {
                sb.append("OOB");
            }
            sb.append(",pid=").append(this.pid);

            sb.append(",pri=").append(this.priority);
            if (this.timesToMatch == 0)
                sb.append(",infinite");
            else
                sb.append(",").append(this.timesToMatch).append("-match");

            sb.append(",sectionSize=").append(sectionSize);
            if (this.posMask != null || this.posFilter != null || this.negMask != null || this.negFilter != null)
            {
                toString(",posMask", this.posMask, sb);
                toString(",posFilt", this.posFilter, sb);
                toString(",negMask", this.negMask, sb);
                toString(",negFilt", this.negFilter, sb);
            }
            return sb.append(']').toString();
        }

        /**
         * Used by {@link #toString()} to convert <code>byte[]</code> to a
         * <code>String</code>.
         * 
         * @param str
         *            field name
         * @param field
         *            byte array to convert
         * @param sb
         *            the <code>StringBuffer</code> to update with the
         *            conversion
         */
        private void toString(String str, byte[] field, StringBuffer sb)
        {
            sb.append(str).append('=');
            if (field == null)
                sb.append("null");
            else
            {
                char delim = '[';
                for (int i = 0; i < field.length; ++i)
                {
                    sb.append(delim).append("0x").append(Integer.toHexString(field[i] & 0xFF));
                    delim = ',';
                }
                sb.append(']');
            }
        }

        /**
         * Saves a copy of the given array, pre-pending <i>offset</i> bytes to
         * the beginning of the new array.
         * 
         * @param offset
         *            number of <i>extra</i> bytes needed at the front end of
         *            the new array
         * @param array
         *            the array to copy into a new array
         * @return the new array; <code>null</code> if <i>array</i> is
         *         <code>null</code>
         */
        private byte[] saveArray(int offset, byte[] array)
        {
            if (array == null) return null;

            byte[] copy = new byte[offset + array.length];
            System.arraycopy(array, 0, copy, offset, array.length);
            return copy;
        }

        /**
         * App-specific data provided on construction.
         */
        public final Object appData;

        /**
         * The currently set filter.
         */
        private Filter filter;
    }

    /**
     * Make sure that we stop this filter if garbage collected
     */
    protected void finalize()
    {
        stopFiltering();
    }

    // DAVIC section filter constants
    static final int DAVIC_MIN_OFFSET = 3;

    static final int DAVIC_MAX_OFFSET = 31;

    static final int DAVIC_MAX_FILTER_LEN = 32;

    private int timesToMatch;

    /**
     * Native filter session.
     */
    private FilterSession filterSession;

    /** Contains the maximumal allowable size of a section. */
    private int sectionSizeLimit = -1;

}
