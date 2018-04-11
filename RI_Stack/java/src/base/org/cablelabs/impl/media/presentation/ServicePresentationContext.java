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

import javax.tv.locator.Locator;

import org.cablelabs.impl.davic.mpeg.ElementaryStreamExt;
import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.cablelabs.impl.media.access.ComponentAuthorization;
import org.cablelabs.impl.media.player.BroadcastAuthorization;
import org.cablelabs.impl.media.player.SessionChangeCallback;
import org.ocap.media.MediaPresentationEvaluationTrigger;
import org.ocap.net.OcapLocator;

/**
 * This extension of
 * {@link org.cablelabs.impl.media.presentation.VideoPresentationContext
 * VideoPresentationContext} is used for presenting an OCAP
 * {@link javax.tv.service.Service Service}. It provides the execution
 * environment for a
 * {@link org.cablelabs.impl.media.presentation.ServicePresentation
 * ServicePresentation}. This interface defines asynchronous callback methods
 * that are invoked when important events occur during the lifetime of the
 * {@link org.cablelabs.impl.media.presentation.ServicePresentation
 * ServicePresentation}.
 * 
 * @author schoonma
 */
public interface ServicePresentationContext extends VideoPresentationContext, SessionChangeCallback
{
    /*
     * Context Information Methods
     */

    /**
     * Get the nextwork interface that is delivering a service.
     * 
     * @return Returns the {@link ExtendedNetworkInterface} that the
     *         presentation should use when decoding live from the network.
     * 
     *         If the NetworkInterface isn't available via the context, this
     *         method (e.g., playback of a recording, which should use the
     *         TimeShiftWindowClient networkInterface if recording is ongoing ),
     *         this method can return <code>null</code>.
     */
    ExtendedNetworkInterface getNetworkInterface();

    /**
     * Return a component that can provide broadcast authorization
     * @return BroadcastAuthorization or null if authorization is never required for this service 
     */
    BroadcastAuthorization getBroadcastAuthorization();

    /*
     * Asynchronous Notification Callback Methods
     */

    /**
     * This should be invoked by the {@link ServicePresentation} if the content
     * source disappears (e.g., a recording being removed before presentation
     * starts).
     * 
     * @param msg
     *            - a descriptive message
     * @param throwable
     *            an optional throwable cause
     */
    void notifyNoSource(String msg, Throwable throwable);

    /**
     * Invoked by {@link ServicePresentation} if presentationfails
     * asynchronously due to CA failue. This will also cause the
     * {@link ServicePresentation} to stop. If the
     * {@link ServicePresentationContext} is a player, this method should post a
     * {@link org.dvb.media.CAStopEvent} to registered
     * {@link javax.media.ControllerListener ControllerListener}s and should
     * stop the player.
     */
    void notifyCAStop();

    /**
     * Invoked by {@link ServicePresentation} if presentation fails
     * asynchronously due to a NO_DATA error. This will also cause the
     * {@link ServicePresentation} to stop. If the
     * {@link ServicePresentationContext} is a player, this method should post a
     * {@link org.dvb.media.ServiceRemovedEvent} to registered
     * {@link javax.media.ControllerListener ControllerListener}s and should
     * stop the player.
     */
    void notifyNoData();

    /**
     * Invoked when {@link ServicePresentation#select(ElementaryStreamExt[])}
     * starts presenting the new streams. If the
     * {@link ServicePresentationContext} is a player, this method should post
     * {@link javax.tv.media.MediaSelectSucceededEvent} to registered
     * {@link javax.tv.media.MediaSelectListener MediaSelectListener}s.
     */
    void notifyMediaSelectSucceeded(Locator[] locators);

    /**
     * Invoked by {@link ServicePresentation} if {@link Presentation#start()}
     * fails presenting requested streamms. If the
     * {@link ServicePresentationContext} is a player, this method should post
     * {@link javax.tv.media.MediaSelectFailedEvent} to registered
     * {@link javax.tv.media.MediaSelectListener MediaSelectListener}s.
     */
    void notifyMediaSelectFailed(Locator[] locators);

    /**
     * Invoked by {@link ServicePresentation} when the set of presenting streams
     * changes. If the {@link ServicePresentationContext} is a player, this
     * method should post {@link org.dvb.media.PresentationChangedEvent} to
     * registered {@link javax.media.ControllerListener ControllerListener}s.
     * 
     * @param reason
     *            - the reason for the change (e.g.,
     *            {@link org.dvb.media.PresentationChangedEvent#CA_FAILURE}).
     */
    void notifyPresentationChanged(int reason);

    /**
     * Invoked by {@link ServicePresentation} to indicate that a media decode
     * session started up and, if alternative media is being presented, the
     * reason for the alternative media. This method should post a
     * {@link org.ocap.media.NormalMediaPresentationEvent} or
     * {@link org.ocap.media.AlternativeMediaPresentationEvent}, depending on
     * the value of the
     * 
     * @param reason
     *            parameter, to registered
     *            {@link javax.media.ControllerListener ControllerListener}s.
     * @param altMediaPresReason
     *            if alternative media is being presented, this is a constant
     *            defined by {@link AlternativeMediaPresentationReason}; if
     *            normal content is being presented, this is -1.
     */
    void notifyMediaAuthorization(ComponentAuthorization authorization);

    /**
     * Called when there are no components to present&mdash;e.g., due to a PMT
     * being removed.
     */
    void notifyNoComponentSelected();

    /**
     * Invoked by {@link ServicePresentation} to indicate that alternative
     * content is being presented
     * 
     *
     *
     * @param alternativeContentClass
     *            AlternativeContentErrorEvent or a subclass
     * @param alternativeContentReasonCode
     *            the reason alternative content is being presented
     *
     * @see org.ocap.service.AlternativeContentErrorEvent
     */
    void notifyAlternativeContent(Class alternativeContentClass, int alternativeContentReasonCode);

    /**
     * Invoked by {@link ServicePresentation} to indicate that normal content is
     * being presented FOLLOWING a notification of alternative content.
     */
    void notifyNormalContent();

    /**
     * Generate AlternativeMediaPresentationEvent for the provided streams with no reasons
     *
     * @param streams
     * @param locator
     * @param trigger
     * @param digital
     */
    void notifyNoReasonAlternativeMediaPresentation(ElementaryStreamExt[] streams, OcapLocator locator, MediaPresentationEvaluationTrigger trigger, boolean digital);
}
