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

import org.davic.resources.ResourceClient;
import org.havi.ui.HVideoConfiguration;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.ocap.resource.ResourceUsageImpl;
import org.cablelabs.impl.util.NativeHandle;

/**
 * The VideoDevice interface specifies all
 * {@link org.cablelabs.impl.havi.port.mpe.HDVideoDevice} methods that are
 * required by a {@link org.cablelabs.impl.media.player.AVPlayerBase}. JMF
 * components reference an HVideoDevice through this interface.
 * 
 * @author schoonma
 */
public interface VideoDevice extends NativeHandle
{
    /** Successfully controlled video device. */
    public final int CONTROL_SUCCESS = 0;

    /** Could not get reservation of video device. */
    public final int NO_RESERVATION = 1;

    /** Caller's priority is not high enough to take away use of video device. */
    public final int INSUFFICIENT_PRIORITY = 2;

    /** The configuration does not support video. */
    public final int BAD_CONFIGURATION = 3;

    /** The Player does not have an owner. */
    public final int NO_CALLER_CONTEXT = 4;

    /**
     * This is like {@link org.havi.ui.HVideoDevice#getVideoController()},
     * except that it returns the {@link AVPlayer} and doesn't check whether the
     * caller is the same as the owning application.
     * 
     * @return Returns the {@link AVPlayer} that currently controls the video
     *         device; <code>null</code> if the device is not currently
     *         controlled.
     */
    AVPlayer getController();

    /**
     * Allows the calling {@link AVPlayer} to attempt to control the video
     * device&mdash;i.e., to become the "video controller" returned by
     * {@link org.havi.ui.HVideoDevice#getVideoController()}. Only one
     * {@link AVPlayerBase} at a time can be the controller. The video device
     * must be CONTRIBUTING, or this will fail. Also, the video device must not
     * be controlled already, or if controlled, must be controlled by a lower
     * priority application. If control is taken away from the current
     * controller, the current controller will be notified via
     * {@link AVPlayer#loseVideoDeviceControl()}.
     * 
     * @param player
     *            - the {@link AVPlayer} that is requesting control
     * @param resourceUsage
     *            - the {@link ResourceUsageImpl#} associated with the player
     * @return Returns {@link #CONTROL_SUCCESS} if successful. Returns
     *         {@link #INSUFFICIENT_PRIORITY} if the caller can't become the
     *         controller due to the controller's application having higher
     *         priority. Returns {@link #BAD_CONFIGURATION} if a contributing
     *         video configuration could not be established.
     */
    int controlVideoDevice(AVPlayer player, ResourceUsageImpl resourceUsage);

    /**
     * Provides a process for attempting to reserve a particular device based
     * upon the <code>ResourceUsage</code> param. This method adheres to the
     * process for resolving resource contention set forth in OCAP 19.2.1.1.3.
     * 
     * @param usage
     *            a representation of the request to reserve the resource
     * @param client
     *            a representation of the intended owner of the resource
     * @return true if the right is granted, otherwise false
     */
    boolean reserveDevice(ResourceUsageImpl usage, ResourceClient client, CallerContext context);

    /**
     * This is like {@link #controlVideoDevice(AVPlayer)}, except that it also
     * attempts to reserve the video device.
     * 
     * @param player
     *            The {@link AVPlayer} to become the controller.
     * @param usage
     *            The {@link ResourceUsageImpl} to use to reserve the video
     *            device.
     * @param resClient
     *            The {@link ResourceClient} to be notified if the reservation
     *            is lost.
     * @return In addition to the values returned by
     *         {@link #controlVideoDevice(AVPlayer)}, it will return
     *         {@link #NO_RESERVATION} if the video device could not be
     *         reserved.
     */
    int reserveAndControlDevice(AVPlayer player, ResourceUsageImpl usage, ResourceClient resClient);

    /**
     * Allows the calling {@link AVPlayer} to relinquish control of the video
     * device, which it obtained by a prior successful call to
     * {@link #controlVideoDevice(AVPlayer)}.
     * 
     * @param player
     *            - the calling {@link AVPlayer} that is relinquishing control
     */
    void relinquishVideoDevice(AVPlayer player);

    /**
     * Swaps the {@link AVPlayer} being controlled by the {@link VideoDevice}
     * with the {@link AVPlayer} being controlled by another {@link VideoDevice}
     * . This also swaps the reference from the {@link AVPlayer} to the
     * {@link VideoDevice}. This is used by
     * {@link org.ocap.system.ScaledVideoManager#swapServiceContextSettings(ServiceContext, ServiceContext, boolean, boolean)}
     * .
     * 
     * @param otherPlayer
     *            The {@link AVPlayer} with which to perform the swap.
     */
    void swapControllers(AVPlayer otherPlayer);

    //
    // Expose HScreenDevice and HVideoDevice methods that are needed by AVPlayer
    //

    /** @see org.havi.ui.HScreenDevice#releaseDevice(CallerContext) */
    void releaseDevice(CallerContext context);

    /** @see org.havi.ui.HScreenDevice#getClient() */
    ResourceClient getClient();

    /** @see org.havi.ui.HVideoDevice#getCurrentConfiguration() */
    HVideoConfiguration getCurrentConfiguration();

    /** @see org.havi.ui.HVideoDevice#getDefaultConfiguration() */
    HVideoConfiguration getDefaultConfiguration();
}
