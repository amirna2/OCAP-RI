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

import javax.media.GainControl;
import javax.media.Time;

/**
 * This is a base interface to a component that presents content within a
 * {@link org.cablelabs.impl.media.presentation.PresentationContext
 * PresentationContext}.
 * <p>
 * This an application of the Facade Pattern. <code>Presentation</code> defines
 * a unified, simplified interface to <code>PlayerBase</code> for dealing with
 * all of the complexity associated with presenting content&mdash;e.g., setting
 * up and tearing down native decode sessions for various state transitions,
 * such as live to timeshift mode, etc.
 * <p>
 * A <code>Presentation</code> runs within a
 * {@link org.cablelabs.impl.media.presentation.PresentationContext
 * PresentationContext}, which would normally be a <code>PlayerBase</code>
 * implementation, but could also be a "canned" context used for testing the
 * <code>Presentation</code> in isolation from a <code>Player</code>.
 * 
 * @author schoonma
 */
public interface Presentation
{
    /* constant that represents an undefined playback rate */
    public static final float RATE_UNDEFINED = Float.NaN;

    /**
     * Start the presentation.
     * 
     * Presentation implementations must call PresentationContext#notifyStarted when decode is initiated to transition
     * the player to the Started state.
     * 
     */
    void start();

    /**
     * @return Returns <code>true</code> if the {@link Presentation} is
     *         presenting content.
     */
    boolean isPresenting();

    /**
     * Stop the presentation. Has no effect of presentation is already stopped.
     */
    void stop();

    /**
     * Sets the playback rate of the presentation.
     * 
     * @param rate
     *            The requested rate.
     * @return Returns the actual rate assigned, which may be different than the
     *         requested rate. Returns {@link #RATE_UNDEFINED} if called when
     *         the presentation is stopped.
     */
    float setRate(float rate);

    /**
     * Sets the media time of the presentation.  Will update player clock with the result.
     *
     * This method may execute asynchronously (don't call setMediaTime immediately followed by getMediaTime unless
     * is it explicitly documented as supported in the presentation and player implementations)
     * 
     * @param mt
     *            The requested media time.
     */
    void setMediaTime(Time mt);

    /**
     * @return Returns the current playback rate.
     */
    float getRate();

    /**
     * @return Returns the current media time.
     */
    Time getMediaTime();

    /**
     * Mute or unmute the audio of a decode session
     *
     * @param mute new mute state
     */
    void setMute(boolean mute);

    /**
     * Update the audio gain value as described by {@link GainControl#setDB}
     *
     * @param gain new audio gain value in decibels
     */
    float setGain(float gain);
}
