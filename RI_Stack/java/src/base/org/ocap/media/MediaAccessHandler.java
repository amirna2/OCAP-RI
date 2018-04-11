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
//        Â·Redistributions of source code must retain the above copyright notice, this list 
//             of conditions and the following disclaimer.
//        Â·Redistributions in binary form must reproduce the above copyright notice, this list of conditions 
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

package org.ocap.media;

import javax.media.Player;
import org.davic.mpeg.ElementaryStream;
import org.ocap.net.OcapLocator;

/**
 * A class implementing this interface can prevent the presentation of A/V
 * service components.
 * <p>
 * Only one instance of the class that implements this interface can be
 * registered to <code>{@link MediaAccessHandlerRegistrar}</code> via the
 *
 * <code>{@link MediaAccessHandlerRegistrar#registerMediaAccessHandler(MediaAccessHandler)}</code>
 * method. JMF calls checkMediaAccessAuthorization() before AV service
 * components presentation.
 * <p>
 * An application which has a MonitorAppPermission("mediaAccess") may implement
 * this interface, and may set an instance of it in
 * <code>{@link MediaAccessHandlerRegistrar}</code>.
 * </p>
 * <p>
 * Note : this handler is only responsible for the presentation of A/V service
 * components and not for launching or not applications.
 * </p>
 */
public interface MediaAccessHandler
{

    /**
     * The <code>checkMediaAccessAuthorization()</code> method is invoked
     * each time a <code>{@link MediaPresentationEvaluationTrigger}</code> is
     * generated either by the OCAP implementation, or, by a monitor application
     * that has MonitorAppPermission(“mediaAccess”) through the
     * <code>{@link MediaAccessConditionControl}</code> JMF control. The OCAP
     * implementation SHALL block the new presentation corresponding to the new
     * environment that led to the generation of the trigger until the
     * MediaAccessHandler grants permission. It is implementation dependent
     * whether presentation of previously selected service components is stopped
     * or not. The OCAP implementation gives all the service components that are
     * part of the service selection even if they are already presented before
     * the trigger is issued.
     *
     * @param p
     *            the concerned player.
     * @param sourceURL
     *            the URL of the content to be presented.
     * @param isSourceDigital
     *            a boolean indicating if the source is digital or analog.
     * @param esList
     *            is the list of service components that are going to be
     *            presented. esList can be null, for instance if isSourceDigital
     *            is false.
     * @param evaluationTrigger
     *            is one of the constant defined in
     *            <code>{@link MediaPresentationEvaluationTrigger}</code> or an
     *            application defined
     *            <code>{@link MediaPresentationEvaluationTrigger}</code>.
     * @return a {@link MediaAccessAuthorization} defined by MediaAccessHandler
     *         for the given service components. The MediaAccessAuthorization
     *         contains the reason(s), if any, of denied access (use constant
     *         defined in
     *         <code>{@link AlternativeMediaPresentationReason}</code>) per
     *         service component.
     * @see MediaAccessAuthorization
     * @see AlternativeMediaPresentationReason
     * @see MediaPresentationEvaluationTrigger
     */
    public MediaAccessAuthorization checkMediaAccessAuthorization(Player p, OcapLocator sourceURL,
            boolean isSourceDigital, ElementaryStream[] esList, MediaPresentationEvaluationTrigger evaluationTrigger);

}
