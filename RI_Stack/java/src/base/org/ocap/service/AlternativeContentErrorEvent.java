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

package org.ocap.service;

import javax.tv.service.selection.AlternativeContentEvent;
import javax.tv.service.selection.PresentationTerminatedEvent;
import javax.tv.service.selection.SelectionFailedEvent;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceContext;

/**
 * <code>AlternativeContentErrorEvent</code> is generated to indicate that
 * "alternative" content is being presented due to an error that
 * prevents the presentation of normal content as part of selection
 * of a service and during presentation of that selected service.
 * <p>
 * This event will be generated instead of <code>SelectionFailedEvent</code>
 * where normal content could not be presented due to the following situations:
 *
 * <ul>
 * <li> The parental control settings prevent it.
 * <li> The CA system refusing to permit it.
 * <li> The requested content could not be found in the network.
 * <li> The absence of a <code>ServiceContentHandler</code>
 *      required to present the requested service.
 * <li> Problems with tuning.
 * </ul>
 *
 * Such presentation failures are not considered selection failures.
 *
 * <p>
 * This event will be generated instead of <code>PresentationTerminatedEvent</code>
 * where normal content presentation could not continue due to the following
 * situations:
 *
 * <ul>
 * <li> The parental control settings prevent it.
 * <li> The CA system refusing to permit it.
 * <li> Inability to locate the requested content on the network.
 * <li> The absence of a <code>ServiceContentHandler</code>
 *      required to present the requested service.
 * <li> Change of tuning information.
 * </ul>
 *
 * Such presentation failures do not terminate presentation and allow for
 * restoration of normal content presentation after correction of the error
 * condition.
 *
 * <p>
 * Note: The set of reason codes defined in this class may be extended by subclasses.
 * Care should be taken to ensure that the values of newly-defined reason codes
 * are unique.
 *
 *
 * @see SelectionFailedEvent
 * @see PresentationTerminatedEvent
 * @see AlternativeContentEvent
 *
 * @author Aaron Kamienski
 */
public class AlternativeContentErrorEvent
    extends AlternativeContentEvent
{
    /**
     * Reason code: Normal content could not be presented due to a
     * parental control rating problem.
     */
    public static final int RATING_PROBLEM = 100;

    /**
     * Reason code: Normal content could not be presented due to the
     * CA system refusing to permit it.
     */
    public static final int CA_REFUSAL = 101;

    /**
     * Reason code : Normal content could not be presented because the requested
     * content could not be found in the network.
     */
    public static final int CONTENT_NOT_FOUND = 102;

    /**
     * Reason code : Normal content could not be presented due to absence of a
     * <code>ServiceContentHandler</code> required to present the requested
     * service's content.
     *
     * @see ServiceContentHandler
     */
    public static final int MISSING_HANDLER = 103;

    /**
     * Reason code : Normal content could not be presented due to problems with tuning.
     * This includes lack of tuning information as well as errors encountered during
     * tuning.
     */
    public static final int TUNING_FAILURE = 104;

    private final int reason;

    /**
     * Constructs an event with a reason code.
     *
     * @param source The <code>ServiceContext</code> that generated the event.
     * @param reason The reason why alternative content is being presented.
     */
    public AlternativeContentErrorEvent(ServiceContext source, int reason)
    {
        super(source);
        this.reason = reason;
    }

    /**
     * Reports the reason why alternative content is being presented.
     *
     * @return The reason why alternative content is being presented.
     *         This SHALL be one of
     *         {@link #RATING_PROBLEM},
     *         {@link #CA_REFUSAL},
     *         {@link #CONTENT_NOT_FOUND},
     *         {@link #MISSING_HANDLER},
     *         or
     *         {@link #TUNING_FAILURE}.
     */
    public int getReason()
    {
        return reason;
    }

    /**
     * Provide a String describing the event, including reason
     *
     * @return description
     */
    public String toString()
    {
        return "AlternativeContentErrorEvent[" + reason + "]";
    }
}
