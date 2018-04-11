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

import javax.tv.service.SIElement;
import javax.tv.service.SIRequest;
import javax.tv.service.SIRequestor;
import javax.tv.service.Service;

/**
 * <code>ProgramEvent</code> represents collection of elementary streams with a
 * common time base, an associated start time, and an associated end time. An
 * event is equivalent to the common industry usage of "TV program."
 * <p>
 * 
 * 
 * The Event Information Table (EIT) contains information (titles, start times,
 * etc.) for events on defined services. An event is, in most cases, a typical
 * TV program, however its definition may be extended to include particular data
 * broadcasting sessions and other information segments.
 * <p>
 * 
 * A <code>ProgramEvent</code> object may optionally implement the
 * <code>CAIdentification</code> interface. Note that all time values are in UTC
 * time.
 * <P>
 * 
 * @see java.util.Date java.util.Date
 * 
 * @see javax.tv.service.navigation.CAIdentification
 * 
 * @see <a
 *      href="../../../../overview-summary.html#guidelines-opinterfaces">Optionally
 *      implemented interfaces</a>
 */
public interface ProgramEvent extends SIElement
{

    /**
     * Returns the start time of this program event. The start time is in UTC
     * time.
     * 
     * @return This program's start time (UTC).
     */
    public abstract java.util.Date getStartTime();

    /**
     * Returns the end time of this program event. The end time is in UTC time.
     * 
     * @return This program's end time (UTC).
     */
    public abstract java.util.Date getEndTime();

    /**
     * Returns the duration of this program event in seconds.
     * 
     * @return This program's duration in seconds.
     */
    public abstract long getDuration();

    /**
     * Returns the program event title. This information may be obtained in the
     * ATSC EIT table or the DVB Short Event Descriptor.
     * 
     * @return A string representing this program's title, or an empty string if
     *         the title is unavailable.
     */
    public abstract String getName();

    /**
     * Retrieves a textual description of the event. This method delivers its
     * results asynchronously.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @see ProgramEventDescription
     */
    public abstract SIRequest retrieveDescription(SIRequestor requestor);

    /**
     * Reports content advisory information associated with this program for the
     * local rating region.
     * 
     * @return A <code>ContentRatingAdvisory</code> object describing the rating
     *         of this <code>ProgramEvent</code> or <code>null</code> if no
     *         rating information is available.
     * 
     */
    public abstract ContentRatingAdvisory getRating();

    /**
     * Reports the <code>Service</code> this program event is associated with.
     * 
     * @return The <code>Service</code> this program event is delivered on.
     */
    public abstract Service getService();

    /**
     * Retrieves an array of service components which are part of this
     * <code>ProgramEvent</code>. Service component information may not always
     * be available. If the <code>ProgramEvent</code> is current, this method
     * will provide only service components associated with the
     * <code>Service</code> to which the <code>ProgramEvent</code> belongs. If
     * the <code>ProgramEvent</code> is not current, no guarantee is provided
     * that all or even any of its service components will be available.
     * <p>
     * 
     * This method delivers its results asynchronously. The retrieved array will
     * only contain <code>ServiceComponent</code> instances <code>c</code> for
     * which the caller has
     * <code>javax.tv.service.ReadPermission(c.getLocator())</code>. If no
     * <code>ServiceComponent</code> instances meet this criteria, this method
     * will result in an <code>SIRequestFailureType</code> of
     * <code>DATA_UNAVAILABLE</code>.
     * 
     * @param requestor
     *            The <code>SIRequestor</code> to be notified when this
     *            retrieval operation completes.
     * 
     * @return An <code>SIRequest</code> object identifying this asynchronous
     *         retrieval request.
     * 
     * @see javax.tv.service.navigation.ServiceComponent
     * @see javax.tv.service.ReadPermission
     */
    public abstract SIRequest retrieveComponents(SIRequestor requestor);
}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
