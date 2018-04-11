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

package org.cablelabs.impl.media.presentation;

import org.cablelabs.impl.media.access.ComponentAuthorization;
import org.cablelabs.impl.media.session.Session;
import org.davic.media.MediaFreezeException;
import org.ocap.media.MediaPresentationEvaluationTrigger;

/**
 * This is the interface to a video presentation that presents a
 * {@link javax.tv.service.Service Service}. It runs within a
 * {@link org.cablelabs.impl.media.presentation.ServicePresentationContext
 * ServicePresentationContext}.
 * 
 * @see org.cablelabs.impl.media.presentation.ServicePresentationContext
 * @author schoonma
 */
public interface ServicePresentation extends VideoPresentation
{
    //supported alternative content modes

    //decode session remains active and video remains decoded
    int ALTERNATIVE_CONTENT_MODE_RENDER_VIDEO = 1;
    //decode session remains active and but video is blocked
    int ALTERNATIVE_CONTENT_MODE_RENDER_BLACK = 2;
    //decode session is stopped and alternative content is rendered via drip feed
    int ALTERNATIVE_CONTENT_MODE_STOP_DECODE = 3;

    /**
     * (This is defined by the super-interface {@link Presentation}, but is
     * documented here to cover requirements specific to service presentation.)
     * <p>
     * A precondition of this method is that the {@link ServicePresentation} has
     * been primed with a {@link Selection} that specifies the initial requested
     * streams.
     * <p>
     * On successfully starting the presentation, this should notify the
     * {@link ServicePresentationContext} by calling
     * {@link ServicePresentationContext#notifyMediaPresented()}. If successful,
     * it should also indicate whether normal or alternative content is being
     * presented, by calling
     * {@link ServicePresentationContext#notifyMediaAuthorization(int)} or
     * {@link ServicePresentationContext#notifyAlternativeMediaPresentation(ComponentAuthorization)}
     * , respectively.
     * 
     * @see org.cablelabs.impl.media.presentation.Presentation#start()
     */
    void start();

    // MediaSelectControl support

    /**
     * Attempt to present a different set of streams, which are assumed to be
     * available on the same input source.
     * {@link MediaPresentationEvaluationTrigger#NEW_SELECTED_SERVICE_COMPONENTS}
     * is the trigger for this form of selection. This method is asynchronous,
     * and notifies its status to the owning {@link ServicePresentationContext}
     * via callback methods:
     * <ul>
     * <li>
     * If selection succeeds, it calls
     * {@link ServicePresentationContext#notifyMediaSelectSucceeded()()}. Also,
     * it will indicate whether normal or alternative content is being presented
     * by calling:
     * <ul>
     * <li>{@link ServicePresentationContext#notifyMediaAuthorization(int)}, or</li>
     * <li>
     * {@link ServicePresentationContext#notifyAlternativeMediaPresentation(ComponentAuthorization)}
     * </li>
     * </ul>
     * </li>
     * <li>
     * If it fails, it calls
     * {@link ServicePresentationContext#notifyMediaSelectFailed()()}, and
     * presentation should continue with the previously presenting components.</li>
     * </ul>
     * 
     * @param selection
     *            - A {@link Selection} that indicates what to present.
     */
    void select(Selection selection);

    /**
     * @return Returns the current {@link Selection}. It does <em>not</em>
     *         return the pending selection (if there is one) only the
     *         <em>current</em> selection.
     */
    Selection getCurrentSelection();

    /**
     * 
     * @return Waits for the current selection to complete and returns the
     *         Selection or null if the selection failed.
     */
    Selection waitForCurrentSelection();

    // MediaAccessHandler support

    /**
     * Re-select the current selection, forcing it to reauthorize the selected
     * streams. This method is asynchronous, and it notifies the caller of
     * whether normal or alternative content is presenting by calling these
     * methods:
     * <ul>
     * <li>{@link ServicePresentationContext#notifyMediaAuthorization(int)}, or</li>
     * <li>
     * {@link ServicePresentationContext#notifyAlternativeMediaPresentation(ComponentAuthorization)}
     * </li>
     * </ul>
     * 
     * @param trigger
     *            Indicates what triggered the reselection.
     */
    void reselect(MediaPresentationEvaluationTrigger trigger);

    // FreezeControl support

    /**
     * Freeze the output window on the last frame decoded.
     */
    void freeze() throws MediaFreezeException;

    /**
     * Resume the decoding stream, frozen by a prior call to {@link #freeze()}.
     */
    void resume() throws MediaFreezeException;

    // ScaledVideoManager support

    void swap(ServicePresentation other);

    // VideoFormatControl support

    int getAspectRatio();

    int getActiveFormatDefinition();

    int getDecoderFormatConversion();

    boolean checkDecoderFormatConversion(int dfc);

    /**
     * @return Returns the native session handle for the currently active
     *         session. Returns {@link Session#INVALID} if no session is in
     *         progress.
     */
    int getSessionHandle();

    /**
     * Present alternative content
     * @param alternativeContentMode one of the defined alternative content mode constants
     * @param alternativeContentReasonClass AlternativeContentErrorEvent or subclass
     * @param alternativeContentReasonCode reason code valid for the alternativeContentReasonClass
     */
    void switchToAlternativeContent(int alternativeContentMode, Class alternativeContentReasonClass, int alternativeContentReasonCode);
}
