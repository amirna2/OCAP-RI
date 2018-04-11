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

package javax.tv.service.selection;

/**
 * <code>SelectionFailedEvent</code> is generated when a service selection
 * operation fails. <code>SelectionFailedEvent</code> is not generated when a
 * service selection fails with an exception.
 * <p>
 * 
 * Presentation failures enforced via a conditional access system may be
 * reported by this event (with the reason code CA_REFUSAL) or by
 * <code>AlternativeContentEvent.</code> Which of these is used depends on the
 * precise nature of the conditional access system. Applications must allow for
 * both modes of failure.
 * 
 * @see AlternativeContentEvent
 */

public class SelectionFailedEvent extends ServiceContextEvent
{

    private int reason = 0;

    /**
     * Reason code : Selection has been interrupted by another selection
     * request.
     */
    public final static int INTERRUPTED = 1;

    /**
     * Reason code : Selection failed due to the CA system refusing to permit
     * it.
     */
    public final static int CA_REFUSAL = 2;

    /**
     * Reason code : Selection failed because the requested content could not be
     * found in the network.
     */
    public final static int CONTENT_NOT_FOUND = 3;

    /**
     * Reason code : Selection failed due to absence of a
     * <code>ServiceContentHandler</code> required to present the requested
     * service.
     * 
     * @see ServiceContentHandler
     */
    public final static int MISSING_HANDLER = 4;

    /**
     * Reason code : Selection failed due to problems with tuning.
     */
    public final static int TUNING_FAILURE = 5;

    /**
     * Reason code : Selection failed due to a lack of resources required to
     * present this service.
     */
    public final static int INSUFFICIENT_RESOURCES = 6;

    /**
     * Reason code: Selection failed due to an unknown reason or for multiple
     * reasons.
     */
    public final static int OTHER = 255;

    /**
     * Constructs the event with a reason code.
     * 
     * @param source
     *            The <code>ServiceContext</code> that generated the event.
     * @param reason
     *            The reason why the selection failed.
     */
    public SelectionFailedEvent(ServiceContext source, int reason)
    {
        super(source);
        this.reason = reason;
    }

    /**
     * Reports the reason why the selection failed.
     * 
     * @return The reason why the selection failed.
     */
    public int getReason()
    {
        return reason;
    }

}

/* 
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */