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

package javax.tv.service.guide;

import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import java.util.Date;
import javax.tv.service.SIException;
import javax.tv.locator.Locator;
import javax.tv.locator.InvalidLocatorException;

/**
 * This interface represents a collection of program events for a given service
 * ordered by time. It provides the current, next and future program events.
 * <p>
 * 
 * Note that all time values are in UTC time.
 * 
 * @see java.util.Date java.util.Date
 * @see javax.tv.service.guide.ProgramEvent
 * @see javax.tv.service.ReadPermission
 */
public interface ProgramSchedule
{

    /**
     * Retrieves the current <code>ProgramEvent</code>. The resulting
     * <code>ProgramEvent</code> is available for immediate viewing.
     * <p>
     * 
     * This method delivers its results asynchronously. If the caller does not
     * have <code>javax.tv.service.ReadPermission(pe.getLocator())</code> (where
     * <code>pe</code> is the current program event), this method will result in
     * an <code>SIRequestFailureType</code> of <code>DATA_UNAVAILABLE</code>.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @see ProgramEvent
     * @see javax.tv.service.ReadPermission
     */
    public SIRequest retrieveCurrentProgramEvent(SIRequestor requestor);

    /**
     * Retrieves the program event for the specified time. The specified time
     * will fall between the resulting program event's start time (inclusive)
     * and end time (exclusive).
     * <p>
     * 
     * This method delivers its results asynchronously. If the caller does not
     * have <code>javax.tv.service.ReadPermission(pe.getLocator())</code> (where
     * <code>pe</code> is the program event at the specified time), this method
     * will result in an <code>SIRequestFailureType</code> of
     * <code>DATA_UNAVAILABLE</code>.
     * 
     * @param time
     *            The time of the program event to be retrieved.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @throws SIException
     *             If <code>time</code> does not represent a future time value.
     * 
     * @see ProgramEvent
     * @see javax.tv.service.ReadPermission
     */
    public SIRequest retrieveFutureProgramEvent(Date time, SIRequestor requestor) throws SIException;

    /**
     * Retrieves all known program events on this service for the specified time
     * interval. A program event <code>pe</code> is retrieved by this method if
     * the time interval from <code>pe.getStartTime()</code> (inclusive) to
     * <code>pe.getEndTime()</code> (exclusive) intersects the time interval
     * from <code>begin</code> (inclusive) to <code>end</code> (exclusive)
     * specified by the input parameters.
     * <p>
     * 
     * This method returns data asynchronously. Only program events
     * <code>pe</code> for which the caller has
     * <code>javax.tv.service.ReadPermission(pe.getLocator())</code> will be
     * retrieved. If no program events meet this criteria, this method will
     * result in an <code>SIRequestFailureType</code> of
     * <code>DATA_UNAVAILABLE</code>.
     * 
     * @param begin
     *            Time identifying the beginning of the interval.
     * 
     * @param end
     *            Time identifying the end of the interval.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @throws SIException
     *             If <code>end</code> represents a time value before
     *             <code>begin</code>, or if <code>end</code> does not represent
     *             a future time value.
     * 
     * @see ProgramEvent
     * @see javax.tv.service.ReadPermission
     */
    public SIRequest retrieveFutureProgramEvents(Date begin, Date end, SIRequestor requestor) throws SIException;

    /**
     * Retrieves a event which follows the specified event.
     * <p>
     * 
     * This method delivers its results asynchronously. If the caller does not
     * have <code>javax.tv.service.ReadPermission(pe.getLocator())</code> (where
     * <code>pe</code> is the next program event), this method will result in an
     * <code>SIRequestFailureType</code> of <code>DATA_UNAVAILABLE</code>.
     * 
     * @param event
     *            A reference event.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @throws SIException
     *             If <code>event</code> does not belong to this
     *             <code>ProgramSchedule</code>.
     * 
     * @see ProgramEvent
     * @see javax.tv.service.ReadPermission
     */
    public SIRequest retrieveNextProgramEvent(ProgramEvent event, SIRequestor requestor) throws SIException;

    /**
     * Retrieves a program event matching the locator. Note that the event must
     * be part of this schedule.
     * <p>
     * 
     * This method returns data asynchronously.
     * 
     * @param locator
     *            Locator referencing the <code>ProgramEvent</code> of interest.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @throws InvalidLocatorException
     *             If <code>locator</code> does not reference a valid
     *             <code>ProgramEvent</code> in this
     *             <code>ProgramSchedule</code>.
     * 
     * @throws SecurityException
     *             If the caller does not have
     *             <code>javax.tv.service.ReadPermission(locator)</code>.
     * 
     * @see ProgramEvent
     * @see javax.tv.service.ReadPermission
     */
    public SIRequest retrieveProgramEvent(Locator locator, SIRequestor requestor) throws InvalidLocatorException,
            SecurityException;

    /**
     * Registers a <code>ProgramScheduleListener</code> to be notified of
     * changes to program events on this <code>ProgramSchedule</code>.
     * Subsequent changes will be indicated through instances of
     * <code>ProgramScheduleEvent</code>, with this <code>ProgramSchedule</code>
     * as the event source and an <code>SIChangeType</code> of <code>ADD</code>,
     * <code>REMOVE</code>, <code>MODIFY</code>, or
     * <code>CURRENT_PROGRAM_EVENT</code>. Only changes to
     * <code>ProgramEvent</code> instances <code>p</code> for which the caller
     * has <code>javax.tv.service.ReadPermission(p.getLocator())</code> will be
     * reported.
     * <p>
     * 
     * This method is only a request for notification. No guarantee is provided
     * that the SI database will detect all, or even any, changes to the
     * <code>ProgramSchedule</code>, or whether such changes will be detected in
     * a timely fashion.
     * <p>
     * 
     * If the specified <code>ProgramScheduleListener</code> is already
     * registered, no action is performed.
     * 
     * @param listener
     *            A <code>ProgramScheduleListener</code> to be notified of
     *            changes to program events on this <code>ProgramSchedule</code>
     *            .
     * 
     * @see ProgramEvent
     * @see ProgramScheduleEvent
     * @see ProgramScheduleChangeType
     * @see javax.tv.service.ReadPermission
     */
    public void addListener(ProgramScheduleListener listener);

    /**
     * Unregisters a <code>ProgramScheduleListener</code>. If the specified
     * <code>ProgramScheduleListener</code> is not registered, no action is
     * performed.
     * 
     * @param listener
     *            A previously registered listener.
     */
    public void removeListener(ProgramScheduleListener listener);

    /**
     * Reports the transport-dependent locator referencing the service to which
     * this <code>ProgramSchedule</code> belongs. Note that applications may use
     * this method to establish the identity of a <code>ProgramSchedule</code>
     * after it has changed.
     * 
     * @return The transport-dependent locator referencing the service to which
     *         this <code>ProgramSchedule</code> belongs.
     * 
     * @see ProgramScheduleEvent#getProgramSchedule
     */
    public Locator getServiceLocator();
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
