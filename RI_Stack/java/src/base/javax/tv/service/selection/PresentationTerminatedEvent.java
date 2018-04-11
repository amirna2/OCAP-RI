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
 * <code>PresentationTerminatedEvent</code> is generated when the presentation
 * of a service terminates. This includes both normal termination (e.g., due to
 * an application calling the <code>stop()</code> method) and abnormal
 * termination (e.g., due to some change in the environment). Examples of
 * abnormal termination include:
 * 
 * <ul>
 * <li>a tuning operation making the service unavailable</li>
 * 
 * <li>removal of fundamental resources required to present the service</li>
 * 
 * <li>withdrawal of CA authorization</li>
 * </ul>
 * 
 * <code>PresentationTerminatedEvent</code> is also generated following a
 * <code>SelectionFailedEvent</code> either if the service context was not
 * previously in the <em>presenting</em> state or if recovery of what was being
 * presented previously is not possible.
 * <code>PresentationTerminatedEvent</code> is only generated when no components
 * of the requested service can be presented.
 * <p>
 * When a <code>PresentationTerminatedEvent</code> is generated following a
 * failed selection attempt, the reason code of the
 * <code>PresentationTerminatedEvent</code> is derived from the reason code for
 * the <code>SelectionFailedEvent</code> which preceded it, according to the
 * table below.
 * <p>
 * <table align="center" width="80%" border="1" cellspacing="2" cellpadding="2">
 * <tbody>
 * <tr>
 * <th align="center">SelectionFailedEvent reason code</th>
 * <th align="center">PresentationTerminatedEvent reason code</th>
 * </tr>
 * 
 * <tr>
 * <td>CA_REFUSAL</td>
 * <td>ACCESS_WITHDRAWN</td>
 * </tr>
 * 
 * <tr>
 * <td>CONTENT_NOT_FOUND</td>
 * <td>SERVICE_VANISHED</td>
 * </tr>
 * 
 * <tr>
 * <td>INSUFFICIENT_RESOURCES</td>
 * <td>RESOURCES_REMOVED</td>
 * </tr>
 * 
 * <tr>
 * <td>MISSING_HANDLER</td>
 * <td>RESOURCES_REMOVED</td>
 * </tr>
 * 
 * <tr>
 * <td>TUNING_FAILURE</td>
 * <td>TUNED_AWAY</td>
 * </tr>
 * 
 * <tr>
 * <td>OTHER</td>
 * <td>OTHER</td>
 * </tr>
 * 
 * </tbody>
 * </table>
 * <p>
 * (No reason code corresponding to
 * <code>SelectionFailedEvent.INTERRUPTED</code> is necessary, since this code
 * signals that a new service selection operation is underway.)
 * <p>
 * Once a <code>PresentationTerminatedEvent</code> is generated, the
 * <code>ServiceContext</code> will be in the <em>not presenting</em> state
 * until a call to a <code>select()</code> method succeeds. When this event is
 * generated, all resources used for the presentation have been released, and
 * <code>ServiceContentHandler</code> instances previously associated with the
 * <code>ServiceContext</code> will have ceased presentation of their content.
 * 
 * @see SelectionFailedEvent
 */
public class PresentationTerminatedEvent extends ServiceContextEvent
{
    private int reason = 0;

    /**
     * Reason code : The service vanished from the network.
     */
    public final static int SERVICE_VANISHED = 1;

    /**
     * Reason code : Tuning made the service unavailable.
     */
    public final static int TUNED_AWAY = 2;

    /**
     * Reason code : Resources needed to present the service have been removed.
     */
    public final static int RESOURCES_REMOVED = 3;

    /**
     * Reason code : Access to the service or some component of it has been
     * withdrawn by the system. An example of this is the end of a free preview
     * period for IPPV content.
     */
    public final static int ACCESS_WITHDRAWN = 4;

    /**
     * Reason code : The application or user requested that the presentation be
     * stopped.
     */
    public final static int USER_STOP = 5;

    /**
     * Reason code: The presentation was terminated due to an unknown reason or
     * for multiple reasons.
     */
    public final static int OTHER = 255;

    /**
     * Constructs the event with a reason code.
     * 
     * @param source
     *            The <code>ServiceContext</code> that generated the event.
     * @param reason
     *            The reason for which the presentation was terminated.
     * 
     */
    public PresentationTerminatedEvent(ServiceContext source, int reason)
    {
        super(source);
        this.reason = reason;
    }

    /**
     * Reports the reason for which the presentation was terminated.
     * 
     * @return A reason code for why the presentation was terminated.
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
