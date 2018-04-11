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

package org.cablelabs.impl.media.player;

import java.awt.Container;
import java.awt.Rectangle;

import javax.media.Time;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.locator.Locator;
import javax.tv.service.Service;
import javax.tv.service.navigation.ServiceDetails;
import javax.tv.service.selection.InvalidServiceComponentException;

import org.cablelabs.impl.davic.net.tuning.ExtendedNetworkInterface;
import org.dvb.media.VideoTransformation;

import org.cablelabs.impl.media.player.AbstractVideoPlayer.VideoComponent;
import org.cablelabs.impl.media.session.Session;

/**
 * Interface that must be implemented by all {@link javax.media.Player Player}s
 * that present a {@link Service}.
 * 
 * @author schoonma
 */
public interface ServicePlayer extends AVPlayer
{
    /**
     * Specify the initial {@link Service} and components to be selected. This
     * can be either the default components or explicitly specified components
     * (if no components are specified).
     * 
     * @param serviceDetails
     *            The {@link ServiceDetails} from which media components are
     *            presented.
     * 
     * @param componentLocators
     *            Any array of {@link Locator}s of service compoenents to
     *            present. If this is <code>null</code>, then the default
     *            components will be selected.
     * 
     * @throws InvalidLocatorException
     *             If a locator provided does not reference a selectable service
     *             component.
     * 
     * @throws InvalidServiceComponentException
     *             If a specified service component is not part of the
     *             <code>Service</code> to which the
     *             <code>MediaSelectControl</code> is restricted, if a specified
     *             service component must be presented in conjunction with
     *             another service component not contained in
     *             <code>components</code>, if the specified set of service
     *             components cannot be presented as a coherent whole, or if the
     *             service components are not all available simultaneously.
     */
    void setInitialSelection(ServiceDetails serviceDetails, Locator[] componentLocators) throws InvalidLocatorException,
            InvalidServiceComponentException;

    /**
     * Update presentation to use provided locators - called only in the ServiceContext.select code path on an
     * already-presenting player.
     *
     * @param componentLocators
     *            Any array of {@link Locator}s of service compoenents to
     *            present. If this is <code>null</code>, then the default
     *            components will be selected.
     *
     * @throws InvalidLocatorException
     *             If a locator provided does not reference a selectable service
     *             component.
     *
     * @throws InvalidServiceComponentException
     *             If a specified service component is not part of the
     *             <code>Service</code> to which the
     *             <code>MediaSelectControl</code> is restricted, if a specified
     *             service component must be presented in conjunction with
     *             another service component not contained in
     *             <code>components</code>, if the specified set of service
     *             components cannot be presented as a coherent whole, or if the
     *             service components are not all available simultaneously.
     */
    void updateServiceContextSelection(Locator[] componentLocators) throws InvalidLocatorException, InvalidServiceComponentException;

    /**
     * Sets the video transformation for the {@link javax.media.Player Player}
     * as a background player and sets the initial bounds for the
     * {@link VideoComponent} if/when the player transitions to a component
     * based player. This method is called by
     * {@link org.cablelabs.impl.service.javatv.selection.ServiceContextImpl
     * ServicecontextImpl} just after creating the player when the persistent
     * video modes have been set for a given service context.
     * 
     * @param trans
     *            The background player video transformation (null if not
     *            applicable).
     * @param container
     *            The component player parent container (null if not
     *            applicable).
     * @param bounds
     *            The component player component AWT bounds rectangle (null if
     *            not applicable).
     */
    void setInitialVideoSize(VideoTransformation trans, Container container, Rectangle bounds);

    /**
     * Swap a the decoder being used by this player with another player. After
     * the swap, this player will be presenting the video that was being
     * presented by the other player, and vice versa. Since audio can only be
     * taken from one player, this method allows the caller to specify whether
     * the audio should be taken from the player with which the swap is
     * occurring.
     * 
     * @param otherPlayer
     *            The {@link ServicePlayer} with which to perform the swap.
     * @param useOtherAudio
     *            This boolean indicates whether the audio of
     * @param otherPlayer
     *            should be used.
     */
    void swapDecoders(ServicePlayer otherPlayer, boolean useOtherAudio);

    /**
     * Register a {@link SessionChangeCallback} to receive synchronous
     * notifications of session change events, and return the session handle
     * that is currently in effect. If the callback object is already
     * registered, or if the specified callback is <code>null</code>, this
     * method has no effect. Multiple different instances of callbacks can be
     * registered.
     * 
     * @param cb
     *            the callback to be notified.
     * @return Returns the current session id in effect at the time the callback
     *         was registered. Returns {@link Session#INVALID} if no session is
     *         in effect.
     */
    int addSessionChangeCallback(SessionChangeCallback cb);

    /**
     * Unregister a {@link SessionChangeCallback} that was previously registered
     * by {@link #addSessionChangeCallback(SessionChangeCallback)}. Once it is
     * unregistered, it will no longer receive callback notifications. If the
     * callback is not currently registered, or if the callback is
     * <code>null</code>, this method has no effect.
     * 
     * @param cb
     *            - the callback to be unregistered
     */
    void removeSessionChangeCallback(SessionChangeCallback cb);

    /**
     * Reports true if the player is a ServiceContext specific player created by
     * implicitly via ServiceContext.select method, or false if the player was
     * created explicity via by the application via createPlayer.
     * 
     * @return true if it is a ServiceContext specific player.
     */
    boolean isServiceContextPlayer();

    /**
     * Switch into alternative content mode by stopping the decode session
     *
     * @param alternativeContentReasonClass
     * @param alternativeContentReasonCode
     */
    void switchToAlternativeContent(Class alternativeContentReasonClass, int alternativeContentReasonCode);

    /**
     * Set the (optional) NetworkInterface used by this ServicePlayer to present the service
     *
     * @param networkInterface the NetworkInterface to use 
     */
    void setNetworkInterface(ExtendedNetworkInterface networkInterface);

    /**
     * Support setMediaTime with additional control over if MediaTimeSetEvent is posted.
     * 
     * @param mt the requested mediatime
     * @param sendEvent true will cause a MediaTimeSetEvent to be posted
     */
    void setMediaTime(Time mt, boolean sendEvent);
}
