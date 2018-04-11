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

/** This class implements the OCAP defined VBIFilter interface.
 * For more details:
 * @see VBIFilter.java
 */

package org.cablelabs.impl.media.vbi;

import org.apache.log4j.Logger;

import org.cablelabs.impl.util.EventMulticaster;
import org.cablelabs.impl.util.SystemEventUtil;
import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.manager.CallerContextManager;
import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.VBIFilterManager;
import org.cablelabs.impl.manager.VBIFilterManager.VBIFilterSpec;

import org.cablelabs.impl.manager.VBIFilterManager.VbiFilter;
import org.cablelabs.impl.manager.VBIFilterManager.VBIFilterCallback;
import org.cablelabs.impl.manager.vbi.NativeVBIFilterManager;
import org.cablelabs.impl.media.vbi.VBIFilterGroupImpl;

import org.ocap.media.VBIFilter;
import org.ocap.media.VBIFilterEvent;
import org.ocap.media.VBIFilterListener;

import javax.tv.util.*;

/**
 * This class implements VBIFilter interface
 * 
 * @author Amir Nathoo
 * 
 */
public class VBIFilterImpl implements VBIFilter
{

    public VBIFilterImpl(VBIFilterGroupImpl group, int[] lineNumber, int field, int dataFormat, int unitLength,
            int bufferSize)
    {
        if (log.isInfoEnabled())
        {
            log.info("VBIFilterImpl...");
        }

        // save reference to the filter group and its lock
        this.filterGroup = group;
        this.lock = group.getLock();

        // store filter parameters - some of these will be needed when we make
        // the native
        // call to start a filtering session

        this.filterParams = new FilterParams(lineNumber, field, dataFormat, unitLength, bufferSize);
        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterImpl - bufferSize: " + bufferSize);
        }

        CallerContextManager ccm = (CallerContextManager) ManagerManager.getInstance(CallerContextManager.class);
        callerCtx = ccm.getCurrentContext();

        sessionTimerSpec.setAbsolute(false);
        sessionTimerSpec.setRepeat(false);
        sessionTimerSpec.addTVTimerWentOffListener(sessionTimerListener);

        dataTimerSpec.setAbsolute(false);
        dataTimerSpec.setRepeat(false);
        dataTimerSpec.addTVTimerWentOffListener(dataTimerListener);
    }

    /**
     * Initiate filtering of VBI data for the specified line and the specified
     * data format by a VBIFilterGroup. Filtering starts only after the
     * {@link VBIFilterGroup#attach} method is called.
     * 
     * @param appData
     *            application specific data. This data is notified to the
     *            application with a SectionFilterEvent. Null is possible.
     * 
     */
    public void startFiltering(Object appData)
    {
        startFiltering(appData, 0, null, null, null, null);
    }

    /**
     * Initiate filtering of VBI data for the specified line and the specified
     * data format by a VBIFilterGroup. Only data unit(s) matching with a
     * specified filter parameters are retrieved. Filtering starts only after
     * the {@link VBIFilterGroup#attach} method is called.
     * 
     * @param appData
     *            application specific data. This data is notified to the
     *            application with a SectionFilterEvent. Null is possible.
     * 
     * @param offset
     *            defines a number of offset bytes that the specified matching
     *            bits and masking bits are applied. Value 0 means no offset.
     *            Value 1 means that the matching/masking bit is applied from
     *            the second byte.
     * 
     * @param posFilterDef
     *            defines values to match for bits in a single data unit. Only
     *            data unit that has matching bytes with this posFilterDef are
     *            retrieved. Maximum length is 36 bytes.
     * 
     * @param posFilterMask
     *            defines which bits in the data unit are to be compared against
     *            the posFilterDef bytes. Matching calculation of negFilterDef
     *            and negFilterMask obeys E.8.1 of DAVIC 1.4.1 Part 9. Maximum
     *            length is 36 bytes.
     * 
     * @param negFilterDef
     *            defines values to match for bits in a single data unit. Only
     *            data unit that has matching bytes with this negFilterDef are
     *            retrieved. Maximum length is 36 bytes.
     * 
     * @param negFilterMask
     *            defines which bits in the data unit are to be compared against
     *            the negFilterDef bytes. Matching calculation of negFilterDef
     *            and negFilterMask obeys E.8.1 of DAVIC 1.4.1 Part 9. Maximum
     *            length is 36 bytes.
     */
    public void startFiltering(Object appData, int offset, byte posFilterDef[], byte posFilterMask[],
            byte negFilterDef[], byte negFilterMask[])
    {
        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterImpl::startFiltering...1");
        }

        if (offset < VBI_MIN_OFFSET || offset >= VBI_MAX_OFFSET)
        {
            if (log.isDebugEnabled())
            {
                log.debug("VBIFilterImpl invalid offset...");
            }
            // throw new
            // IllegalFilterDefinitionException("offset must be greater than or equal to "
            // +
            // VBI_MIN_OFFSET + " and less than " + VBI_MAX_OFFSET);
            return;
        }

        // Make sure they are both set or both null.
        if ((negFilterMask != null) != (negFilterDef != null))
        {
            if (log.isDebugEnabled())
            {
                log.debug("VBIFilterImpl negative filter and mask must both have values or both be null...");
            }
            // throw new
            // IllegalFilterDefinitionException("negative filter and mask must both have values or both be null");
            return;
        }

        // If we made it here, we know that both are either set or null.
        // Check one of the values to make sure it's set and then validate
        // equal lengths.
        if (negFilterMask != null)
        {
            if (negFilterMask.length != negFilterDef.length)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("VBIFilterImpl negative filter and mask must be the same length...");
                }
                // throw new
                // IllegalFilterDefinitionException("negative filter and mask must be the same length");
                return;
            }
        }

        // Do the same test for positive that we did for negative.
        if ((posFilterMask != null) != (posFilterDef != null))
        {
            if (log.isDebugEnabled())
            {
                log.debug("VBIFilterImpl positive filter and mask must both have values or both be null...");
            }
            // throw new
            // IllegalFilterDefinitionException("positive filter and mask must both have values or both be null");
            return;
        }

        if (posFilterMask != null)
        {
            if (posFilterMask.length != posFilterDef.length)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("VBIFilterImpl positive filter and mask must be the same length...");
                }
                // throw new
                // IllegalFilterDefinitionException("positive filter and mask must be the same length");

                return;
            }
        }

        // Now we need to validate that the positive and negative filters are of
        // equal length if they are both specified.
        if ((posFilterDef != null) && (negFilterDef != null))
        {
            if (posFilterDef.length != negFilterDef.length)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("VBIFilterImpl positive filter and negative filter must be the same length...");
                }
                // throw new
                // IllegalFilterDefinitionException("positive filter and negative filter must be the same length");
                return;
            }
        }

        // Make sure that the length + offset <= DAVIC_MAX_FILTER_LEN (defined
        // above).
        // If it is not, then we will throw an exception.
        int length = Math.max((posFilterDef != null ? posFilterDef.length : 0),
                (negFilterDef != null ? negFilterDef.length : 0));
        if ((length + offset) > VBI_MAX_FILTER_LEN)
        {
            if (log.isDebugEnabled())
            {
                log.debug("VBIFilterImpl filter length plus offset must be equal to or less than..."
                        + VBI_MAX_FILTER_LEN);
            }
            // throw new
            // IllegalFilterDefinitionException("the filter length plus offset must be equal to or less than "
            // + VBI_MAX_FILTER_LEN);
            return;
        }

        /*
         * if(posFilterDef != null) { for(int i=0; i<posFilterDef.length; i++) {
         * if(Logging.LOGGING) log.debug("VBIFilterImpl - " + "posFilterDef[" +
         * i + "]: " + posFilterDef[i] ); } } if(posFilterMask != null) {
         * for(int i=0; i<posFilterMask.length; i++) { if(Logging.LOGGING)
         * log.debug("VBIFilterImpl - " + "posFilterMask[" + i + "]: " +
         * posFilterMask[i]); } }
         */
        // create a new VBI filtering session
        FilterSession newSession = new FilterSession(appData, offset, posFilterDef, posFilterMask, negFilterDef,
                negFilterMask);
        synchronized (lock)
        {
            if (log.isInfoEnabled())
            {
                log.info("VBIFilterImpl::startFiltering...[filterGroup: " + filterGroup + ", filter: " + this + "]");
            }

            if (state == FILTER_STATE_RUNNING)
                filterGroup.doStopFiltering(this, false);

            if (filterGroup.doStartFiltering(this, newSession) == VBIFilterGroupImpl.FG_STATE_DETACHED)
            {
                filterSession = newSession;
                setState(FILTER_STATE_PENDING_RUN);
            }
        }
    }

    /**
     * This method implements VBIFilter#stopFiltering() Stop current filtering
     * of this VBI filter. Note that the VBIFilterGroup holding this VBI filter
     * doesn't detach.
     */
    public void stopFiltering()
    {
        // begin the round trip from VBIFilter to VBIGroupImpl to
        // VBIFilterResourceManager and
        // back to VBIFilterImpl

        // Tell our parent group to stop filtering first.
        // The group will then call the filter resource manager to handle the
        // stop.
        // The resource manager will call back into VBI filter to actually stop
        // the session.

        // Additionally, if the stop is a result of the group being detached,
        // then resource manager will
        // also remove the group from its reservation list, after the last
        // filter has being stopped.
        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterImpl::stopFiltering...");
        }

        filterGroup.doStopFiltering(this, false);
    }

    /**
     * Set a timeout value. If no VBI data unit is retrieved after calling the
     * startFiltering() method within the timeout value, the filtering stops
     * automatically and SectionFilterEvent with EVENT_CODE_TIMEOUT notifies a
     * timeout occured.
     * 
     * @param milliseconds
     *            a timeout value in milli seconds. A default value is -1 that
     *            indicates infinite.
     */
    public void setTimeOut(long milliseconds)
    {
        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterImpl::setTimeOut: " + milliseconds + " milliseconds");
        }

        synchronized (lock)
        {
            // If there is a timer scheduled already, (try to)deschedule it
            // first
            if (sessionTimerStarted == true)
            {
                // Timer already fired, return..
                if (sessionTimerFired == true)
                    return;

                long now = System.currentTimeMillis();

                if (log.isDebugEnabled())
                {
                    log.debug("VBIFilterImpl::setTimeOut: descheduling timer");
                }
                sessionTimer.deschedule(sessionTimerSpec);
                sessionTimerStarted = false;

                try
                {
                    // Set the new time value
                    long adjustTimerValue = now - sessionTimerSystemTime;
                    sessionTimeOut = milliseconds - adjustTimerValue;

                    // Invalid case!
                    if (sessionTimeOut < 0)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("VBIFilterImpl: invalid new timer value..");
                        }
                        return;
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("VBIFilterImpl: new timer value: " + sessionTimeOut);
                    }

                    sessionTimerSpec.setDelayTime(sessionTimeOut);

                    sessionTimerSpec = sessionTimer.scheduleTimerSpec(sessionTimerSpec);
                    sessionTimerStarted = true;

                    return;
                }
                catch (TVTimerScheduleFailedException e)
                {
                }
            }

            // Set the new timeout
            sessionTimeOut = milliseconds;
            sessionTimerSpec.setDelayTime(sessionTimeOut);
        }
    }

    /**
     * Set a notification time. By setting a notification time, the OCAP
     * implementation notifies a VBIFilterEvent with
     * EVENT_CODE_TIME_NOTIFICATION when the specified time-period has elapsed
     * after receiving the first byte of the data unit. The event shall be sent
     * even if the data received does not form a complete data unit. The event
     * is sent only once. The filter continues filtering after sending the
     * event.
     * 
     * @param milliseconds
     *            a time-period value in milli seconds. A default value is -1
     *            that indicates infinite.
     */
    public void setNotificationByTime(long milliseconds)
    {
        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterImpl::setNotificationByTime: " + milliseconds + " milliseconds");
        }

        synchronized (lock)
        {
            // If there is a timer scheduled already, (try to)deschedule it
            // first
            if (dataTimerStarted == true)
            {
                // Timer already fired..
                if (dataTimerFired == true)
                    return;

                long now = System.currentTimeMillis();

                if (log.isDebugEnabled())
                {
                    log.debug("VBIFilterImpl::setNotificationByTime: descheduling timer");
                }

                dataTimer.deschedule(dataTimerSpec);
                dataTimerStarted = false;

                try
                {
                    // Set the new time value
                    long adjustTimerValue = now - dataTimerSystemTime;
                    dataNotifyTime = milliseconds - adjustTimerValue;

                    // Invalid case!
                    if (dataNotifyTime < 0)
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("VBIFilterImpl: invalid new timer value..");
                        }
                        return;
                    }

                    if (log.isDebugEnabled())
                    {
                        log.debug("VBIFilterImpl: new timer value: " + dataNotifyTime);
                    }

                    dataTimerSpec.setDelayTime(dataNotifyTime);
                    dataTimerSpec = dataTimer.scheduleTimerSpec(dataTimerSpec);
                    dataTimerStarted = true;

                    return;
                }
                catch (TVTimerScheduleFailedException e)
                {
                }
            }

            // Set the new time value
            dataNotifyTime = milliseconds;
            dataTimerSpec.setDelayTime(dataNotifyTime);
        }
    }

    /**
     * Set the number of data units to reveive a cyclic notification. By setting
     * the number of data units, the OCAP implementation notifies a
     * VBIFilterEvent with EVENT_CODE_UNITS_NOTIFICATION cyclically everytime
     * when the specified number of new data units are filtered and stored in a
     * buffer. The filter continues filtering after sending the event.
     * 
     * @param numberOfDataUnits
     *            the number of data units to be notified. A default value is 0
     *            that indicates no notification. Note that if a small number of
     *            data units is specified, the notification may be delayed and
     *            affects to the host performance. For example, if 1 is
     *            specified for UNKNOWN data unit that comes every field (i.e.,
     *            1/60 seconds), the host has to notify every 1/60 seconds and
     *            makes an over load.
     * 
     * @throws IllegalArgumentException
     *             if the numberOfDataUnit is larger than the bufferSize
     *             specified by a {@link VBIFilterGroup#newVBIFilter} method.
     */
    public void setNotificationByDataUnits(int numberOfDataUnits)
    {
        if (numberOfDataUnits > filterParams.bufferSize)
        {
            throw new IllegalArgumentException("numberOfDataUnits larger than buffer size");
        }

        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterImpl::setNotificationByDataUnits: " + numberOfDataUnits);
        }

        synchronized (lock)
        {
            // store the number of data units - if the filter is running call
            // native to set the value
            filterParams.dataUnits = numberOfDataUnits;

            if (filterSession != null)
            {
                // call the native filter to (re)set the value
                filterSession.set(NativeVBIFilterManager.MPE_VBI_PARAM_DATA_UNIT_THRESHOLD, numberOfDataUnits);
            }
        }
    }

    /**
     * Add a new VBIFilterListener instance to this VBI filter. If the same
     * instance that exists currently is specified, this method does nothing and
     * no exception is thrown.
     * 
     * @param listener
     *            a VBIFilterListener instance to be notified a VBI filtering
     *            events.
     */
    public void addVBIFilterListener(VBIFilterListener listener)
    {
        listeners = EventMulticaster.add(listeners, listener);
    }

    /**
     * Remove an existing VBIFilterListener instance from this VBI filter. If
     * the specified instance has not been added, this method does nothing and
     * no exception is thrown.
     */
    public void removeVBIFilterListener(VBIFilterListener listener)
    {
        listeners = EventMulticaster.remove(listeners, listener);
    }

    /**
     * This method returns multiple VBI data unit bytes. The data unit format is
     * defined in a description of a {@link VBIFilter} interface. The returned
     * bytes is a simple concatenated VBI data at the moment. Note that the
     * return value is not aligned by a complete VBI data unit. I.e., incomplete
     * data unit may return. When this method is called, an internal buffer is
     * cleared once. I.e., next call returns a next byte of retrieved VBI data.
     * 
     * @return a concatenated VBI data of the form of specified by a
     *         {@link VBIFilterGroup#newVBIFilter} method.
     */
    public byte[] getVBIData()
    {
        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterImpl::getVBIData...filterSession: " + filterSession);
        }

        synchronized (lock)
        {
            if (filterSession != null)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("VBIFilterImpl::getVBIData done...");
                }

                return filterSession.getVBIData();
            }
        }
        return null;
    }

    /**
     * Clear an internal buffer to store retrieved VBI data. An application
     * shall call this method before data full.
     */
    public void clearBuffer()
    {
        // TODO:Amir
        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterImpl::clearBuffer...");
        }

        synchronized (lock)
        {
            if (filterSession != null)
            {
                filterSession.clearData();
            }
        }
    }

    public int getState()
    {
        return state;
    }

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
            case FILTER_STATE_PENDING_RUN:
                break;

            case FILTER_STATE_STOPPED:
            {
                sessionTimer.deschedule(sessionTimerSpec);
                sessionTimerStarted = false;
                dataTimer.deschedule(dataTimerSpec);
                dataTimerStarted = false;
            }
                break;

            case FILTER_STATE_RUNNING:
                // start the session timer if the value is set
                if (sessionTimeOut != 0)
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug("VBIFilterImpl::scheduleTimerSpec sessionTimeOut: " + sessionTimeOut);
                    }
                    try
                    {
                        sessionTimerSpec = sessionTimer.scheduleTimerSpec(sessionTimerSpec);
                        sessionTimerStarted = true;
                        sessionTimerSystemTime = System.currentTimeMillis();
                    }
                    catch (TVTimerScheduleFailedException e)
                    {
                    }
                }
                break;
            default:
                break;
        }
    }

    public static class FilterParams
    {
        public FilterParams(int[] lineNumber, int field, int dataFormat, int unitLength, int bufferSize)
        {
            this.lineNumber = new int[lineNumber.length];
            System.arraycopy(lineNumber, 0, this.lineNumber, 0, lineNumber.length);

            this.field = field;
            this.dataFormat = dataFormat;
            this.unitLength = unitLength;
            this.bufferSize = bufferSize;
        }

        // This filter parameters
        public int[] lineNumber; // VBI line numbers to filter

        public int field; // VBI field to filter : see VBIFilterGroup for field
                          // definitions

        public int dataFormat; // Format of VBI data packets one of the
                               // VBIFilterGroup#VBI_DATA_FORMAT_XXX

        public int unitLength = 0; // data unit length (if format is unknown) in
                                   // bits

        public int bufferSize; // the VBI filter buffer size in bytes

        public int dataUnits = 0; // number of data units for vbi data
                                  // notification
    };

    /**
     * This method is called by the VBIFilterGroupImpl to initiate a VBI
     * filtering session.
     * 
     * The filter state updated to: RUNNING - if the session is sucessfully
     * started PENDING_RUN - if failed due to ResourceException STOPPED - if
     * failed due to Invalid Source or Invalid Filter Definition or Failure to
     * descramble
     * 
     */
    public boolean doStartVBIFiltering(Object session)
    {
        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterImpl::doStartVBIFiltering...");
        }

        synchronized (lock)
        {
            // We are already running - do nothing
            if (state == FILTER_STATE_RUNNING)
            {
                if (session == null || session == filterSession)
                    return true;

                // Otherwise, stop the current filter
                stopFiltering();
            }
            else if (state == FILTER_STATE_STOPPED)
            {
                // If there is an exisiting session, release that
                if (filterSession != null)
                {
                    filterSession.releaseSession();
                }
            }

            // Attempt to start (and reserve) the native section filter
            FilterSession fs = (session == null) ? filterSession : (FilterSession) session;

            if (log.isDebugEnabled())
            {
                log.debug("VBIFilterImpl::doStartVBIFiltering...filterSession: " + fs);
            }

            // media decode session handle
            fs.decodeSessionType = this.filterGroup.getMediaDecodeSessionHandleType();
            fs.decodeSessionHandle = this.filterGroup.getMediaDecodeSessionHandle();

            if (log.isDebugEnabled())
            {
                log.debug("VBIFilterImpl::doStartVBIFiltering...calling start session");
            }

            if (fs.startSession())
            {
                filterSession = fs;
                setState(FILTER_STATE_RUNNING);
                return true;
            }
        }
        return false;
    }

    /**
     * Stop this section filter and release the native resources associated with
     * it
     * 
     * @param detached
     *            true if this filter is being stopped as a result of its filter
     *            being detached or losing connection to its stream, false if
     *            the section filter was stopped by a call to stopFilterint() or
     *            it finished filtering (i.e. its buffer is full)
     */
    public void doStopVBIFiltering(boolean detached, int reason)
    {
        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterImpl::doStopVBIFiltering...detached: " + detached);
        }

        if (state == FILTER_STATE_RUNNING && filterSession != null)
        {
            filterSession.stopSession();
        }

        synchronized (lock)
        {
            if (log.isDebugEnabled())
            {
                log.debug("VBIFilterImpl::doStopVBIFiltering...filterSession: " + filterSession);
            }

            if (detached)
            {
                notifyVBIFilterListener(new VBIFilterEventImpl(this, VBIFilterEvent.EVENT_CODE_FORCIBLE_TERMINATED,
                        filterSession.appData));
                setState(FILTER_STATE_PENDING_RUN);
            }
            else
            {
                if (reason == VBIFilterGroupImpl.FG_REASON_SOURCE_CHANGE)
                {
                    // Is this the right event??
                    notifyVBIFilterListener(new VBIFilterEventImpl(this,
                            VBIFilterEvent.EVENT_CODE_VIDEO_SOURCE_CHANGED, filterSession.appData));
                    setState(FILTER_STATE_STOPPED);
                }
                else
                {
                    notifyVBIFilterListener(new VBIFilterEventImpl(this, VBIFilterEvent.EVENT_CODE_FORCIBLE_TERMINATED,
                            filterSession.appData));
                    setState(FILTER_STATE_STOPPED);
                }
            }
        }
    }

    /**
     * <i>This method is not part of the defined public API, but is present for
     * the implementation only.</i>
     * 
     * @param e
     *            the event to deliver
     */
    protected void notifyVBIFilterListener(final VBIFilterEvent e)
    {
        callerCtx.runInContext(new Runnable()
        {
            public void run()
            {
                VBIFilterListener l = listeners;
                if (l != null) l.filterUpdate(e);
            }
        });
    }

    /**
     * This class extends VBIFilterSpec and handles Filter Session activities.
     * It is used as an interface between this implementation and the native
     * layer to start/stop filters.
     * 
     * @author Amir Nathoo
     * 
     */
    public class FilterSession extends VBIFilterManager.VBIFilterSpec
    {
        /**
         * Creates an instance of <code>FilterSession</code> to be used as a
         * bridge to native filter sessions
         * 
         * @param appData
         *            application specific data
         * @param offset
         *            offset within VBI packet to apply filter/mask (&gt;= 3)
         * @param posMask
         *            positive filter mask
         * @param posFilter
         *            positive filter
         * @param negMask
         *            negative filter mask
         * @param negFilter
         *            negative filter
         */
        public FilterSession(Object appData, int offset, byte posFilterDef[], byte posFilterMask[],
                byte negFilterDef[], byte negFilterMask[])
        {
            if (log.isDebugEnabled())
            {
                log.debug("FilterSession constructor..");
            }

            this.appData = appData;

            this.posMask = saveArray(offset, posFilterMask);
            this.posFilter = saveArray(offset, posFilterDef);
            this.negMask = saveArray(offset, negFilterMask);
            this.negFilter = saveArray(offset, negFilterDef);
        }

        /**
         * Get the VBI packet data
         * 
         * @return a buffer containing the VBI data
         */
        public byte[] getVBIData()
        {
            return vbiFilter.getData();
        }

        public void clearData()
        {
            vbiFilter.clearData();
        }

        /**
         * Starts a native VBI filtering session. A VbiFilter handle is returned
         * from the native call.
         */
        public boolean startSession()
        {
            VBIFilterManager api = (VBIFilterManager) ManagerManager.getInstance(VBIFilterManager.class);

            vbiFilter = api.startFilter(this, filterParams, callback);
            if (vbiFilter == null)
                return false;
            else
                return true;
        }

        /**
         * sets new paramter values on a given filter session.
         */
        public void set(int param, int val)
        {
            vbiFilter.set(param, val);
        }

        /**
         * gets paramter values on a given filter session.
         */
        public int get(int param, int val)
        {
            return vbiFilter.get(param, val);
        }

        /**
         * Stops a native vbi session
         * 
         */
        public void stopSession()
        {
            vbiFilter.stop();
        }

        /**
         * releases a native vbi session
         * 
         */
        public void releaseSession()
        {
            vbiFilter.release();
        }

        /**
         * clear vbi buffer
         * 
         */
        public void clearBuffer()
        {
            vbiFilter.clearData();
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
            if (array == null)
                return null;

            if (log.isDebugEnabled())
            {
                log.debug("FilterSession saveArray..length: " + array.length);
            }

            byte[] copy = new byte[offset + array.length];
            System.arraycopy(array, 0, copy, offset, array.length);

            return copy;
        }

        /**
         * App-specific data provided on construction.
         */
        public final Object appData;

        /**
         * A native filter session handle: returned when we start a filtering
         * session This handle is need when calling session related native APIs
         */
        private VbiFilter vbiFilter = null;

        /**
         * a native decode Session handle
         */
        // TODO: use correct definition
        public int decodeSessionType;

        public int decodeSessionHandle;
    };

    public FilterSession getFilterSession()
    {
        return filterSession;
    }

    protected void finalize()
    {
        if (log.isDebugEnabled())
        {
            log.debug("VBIFilterImpl::finalize calling releaseSession...");
        }
        // Release any unreleased filter resources..
        if (filterSession != null)
        {
            filterSession.releaseSession();
            filterSession = null;
        }
    }

    //
    // Class Members
    //

    // used for log messages
    private static final Logger log = Logger.getLogger(VBIFilterImpl.class);

    // synchronize the methods of this class - the lock is obtained from the VBI
    // filter group.
    private Object lock = null;

    private VBIFilterListener listeners = null;

    private CallerContext callerCtx;

    // VBIFilter session timer
    // This time is set by calling {@link VBIFilter#setTimeout()}
    private TVTimer sessionTimer = TVTimer.getTimer();

    private TVTimerSpec sessionTimerSpec = new TVTimerSpec();

    private boolean sessionTimerStarted = false;

    private long sessionTimeOut = 0;

    private boolean sessionTimerFired = false;

    long sessionTimerSystemTime = 0;

    private TVTimerWentOffListener sessionTimerListener = new TVTimerWentOffListener()
    {
        // Called when the TVTimer goes off to stop filtering and send timeout
        // event
        public void timerWentOff(TVTimerWentOffEvent e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("VBIFilterImpl::sessionTimer received TVTimerWentOffEvent...");
            }
            // Send out the timeout message
            synchronized (lock)
            {
                if (state == FILTER_STATE_RUNNING && filterSession != null)
                {
                    notifyVBIFilterListener(new VBIFilterEventImpl(VBIFilterImpl.this,
                            VBIFilterEvent.EVENT_CODE_TIMEOUT, filterSession.appData));
                }
            }
            sessionTimerStarted = false;
            sessionTimerFired = true;
            // we call the filter group to stop this VBIFilter
            // The filter will go in STOPPED state. i.e it can only be started
            // again by calling VBIFilter#startFiltering()
            // But the group will remain attached.
            filterGroup.doStopFiltering(VBIFilterImpl.this, false);
        }
    };

    // Data timer
    // This timer is set by calling {@link VBIFilter#setNotificationByTime}
    private TVTimer dataTimer = TVTimer.getTimer();

    private TVTimerSpec dataTimerSpec = new TVTimerSpec();

    private boolean dataTimerStarted = false;

    private boolean dataTimerFired = false;

    private long dataNotifyTime = 0;

    long dataTimerSystemTime = 0;

    private TVTimerWentOffListener dataTimerListener = new TVTimerWentOffListener()
    {
        // Called when the TVTimer goes off
        public void timerWentOff(TVTimerWentOffEvent e)
        {
            if (log.isDebugEnabled())
            {
                log.debug("VBIFilterImpl::dataTimer received TVTimerWentOffEvent...");
            }
            // Send out the timeout message
            synchronized (lock)
            {
                // if we get here, that means we are not processing the session
                // timer
                // we only notify if we are still in RUNNING state.
                if (state == FILTER_STATE_RUNNING && filterSession != null)
                {
                    notifyVBIFilterListener(new VBIFilterEventImpl(VBIFilterImpl.this,
                            VBIFilterEvent.EVENT_CODE_TIME_NOTIFICATION, filterSession.appData));
                }
                dataTimerStarted = false;
                dataTimerFired = true;
            }
        }
    };

    private VBIFilterCallback callback = new VBIFilterCallback()
    {

        public void notifyDataAvailable(VBIFilterSpec spec, int reason)
        {
            // If section corresponds to currently started filter...
            if (spec == filterSession)
            {
                switch (reason)
                {
                    case VBIFilterCallback.REASON_FIRST_DATAUNIT_AVAILABLE:
                    {
                        // if ( Logging.LOGGING )
                        // log.debug("notifyDataAvailable FIRST_DATAUNIT_AVAILABLE"
                        // + "appData: " + filterSession.appData.toString());

                        // start the data timer if the value is set
                        if (dataNotifyTime != 0)
                        {
                            if (log.isDebugEnabled())
                            {
                                log.debug("VBIFilterImpl::scheduleTimerSpec dataNotifyTime: " + dataNotifyTime);
                            }
                            try
                            {
                                dataTimerSpec = dataTimer.scheduleTimerSpec(dataTimerSpec);
                                dataTimerStarted = true;
                                dataTimerSystemTime = System.currentTimeMillis();
                            }
                            catch (TVTimerScheduleFailedException e)
                            {
                            }
                        }

                        notifyVBIFilterListener(new VBIFilterEventImpl(VBIFilterImpl.this,
                                VBIFilterEvent.EVENT_CODE_FIRST_VBI_DATA_AVAILABLE, filterSession.appData));
                    }
                        break;

                    case VBIFilterCallback.REASON_DATAUNITS_RECEIVED:
                    {
                        if (log.isDebugEnabled())
                        {
                            log.debug("notifyDataAvailable DATAUNITS_RECEIVED" + "appData: "
                                    + filterSession.appData.toString());
                        }

                        notifyVBIFilterListener(new VBIFilterEventImpl(VBIFilterImpl.this,
                                VBIFilterEvent.EVENT_CODE_UNITS_NOTIFICATION, filterSession.appData));
                    }
                        break;

                    default:
                        break;
                }
            }
            // This is a stray event which shouldn't be propogated.
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Ignoring stray event for " + spec);
                }
        }
        }

        // TODO: Amir
        // revisit event code mapping between native and java
        public void notifyCanceled(VBIFilterSpec spec, int reason)
        {
            boolean stopFilter = false;

            if (spec == filterSession)
            {
                switch (reason)
                {
                    case VBIFilterCallback.REASON_BUFFER_FULL:
                        notifyVBIFilterListener(new VBIFilterEventImpl(VBIFilterImpl.this,
                                VBIFilterEvent.EVENT_CODE_BUFFER_FULL, filterSession.appData));

                        // stopFilter = true;
                        break;

                    case VBIFilterCallback.REASON_FILTER_STOPPED:
                        notifyVBIFilterListener(new VBIFilterEventImpl(VBIFilterImpl.this,
                                VBIFilterEvent.EVENT_CODE_FORCIBLE_TERMINATED, filterSession.appData));

                        break;
                    case VBIFilterCallback.REASON_SOURCE_CLOSED:
                        notifyVBIFilterListener(new VBIFilterEventImpl(VBIFilterImpl.this,
                                VBIFilterEvent.EVENT_CODE_VIDEO_SOURCE_CHANGED, filterSession.appData));
                        break;
                    case VBIFilterCallback.REASON_SOURCE_SCRAMBLED:
                        notifyVBIFilterListener(new VBIFilterEventImpl(VBIFilterImpl.this,
                                VBIFilterEvent.EVENT_CODE_FAILED_TO_DESCRAMBLE, filterSession.appData));
                        break;
                    default:
                        break;
                }

                synchronized (lock)
                {
                    if (state == FILTER_STATE_RUNNING && stopFilter == true)
                    {
                        filterGroup.doStopFiltering(VBIFilterImpl.this, true);
                    }
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Ignoring stray event for " + spec);
                }
        }
        }
    };

    // reference to container group
    private VBIFilterGroupImpl filterGroup = null;

    private FilterParams filterParams = null;

    private FilterSession filterSession = null;

    // This filter states
    public static final int FILTER_STATE_RUNNING = 0;

    public static final int FILTER_STATE_PENDING_RUN = 1;

    public static final int FILTER_STATE_STOPPED = 2;

    // inital state is FILTER_STATE_STOPPED
    public int state = FILTER_STATE_STOPPED;

    // VBI filter constants
    public static final int VBI_MIN_OFFSET = 0;

    public static final int VBI_MAX_OFFSET = 1;

    public static final int VBI_MAX_FILTER_LEN = 36;
}
