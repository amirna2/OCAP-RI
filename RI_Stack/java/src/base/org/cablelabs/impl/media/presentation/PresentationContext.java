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

import javax.media.Time;
import javax.media.protocol.DataSource;

import org.cablelabs.impl.manager.CallerContext;
import org.cablelabs.impl.media.player.AlarmClock;
import org.cablelabs.impl.media.player.AlarmClock.Alarm;
import org.cablelabs.impl.media.player.PlayerClock;
import org.cablelabs.impl.util.TaskQueue;

/**
 * This class represents the environment in which a
 * {@link org.cablelabs.impl.media.presentation.Presentation Presentation}
 * executes. It serves as the communication interface between a
 * <code>Presentation</code> and its environment. It provides information that
 * is needed by the <code>Presentation</code> as well as defining methods that
 * the <code>Presentation</code> uses to notify the implementing class of
 * important occurrences, such as errors or completion of selection.
 * Sub-interfaces add methods as needed.
 * 
 * @author schoonma
 */
public interface PresentationContext
{
    /** Time constant returned when Player is closed. */
    Time CLOSED_TIME = new Time(0);

    /**
     * @return Returns the {@link DataSource} for the content.
     */
    DataSource getSource();

    /**
     * @return Returns an object that should be used for synchronization. This
     *         allows one synchronization object to be used between a
     *         {@link PresentationContext} and its {@link Presentation}.
     */
    Object getLock();

    /**
     * @return Return the {@link PlayerClock} being used to keep track of media
     *         time by the {@link PresentationContext}.
     */
    PlayerClock getClock();

    /**
     * Set the media time directly on the clock, bypassing Player state checks.
     * 
     * @param mt
     * @param postMediaTimeEvent
     */
    void clockSetMediaTime(Time mt, boolean postMediaTimeEvent);

    /**
     * Accessor - used to support presentation implementations which support the concept of 'default mediatime'
     * 
     * @return true if setmediaTime has been called (not default mediatime)
     */
    boolean isMediaTimeSet();
    
    /**
     * Sets the playback rate directly on the clock, bypassing Player state
     * checks.
     *
     * @param rate
     * @param postRateChangEvent
     */
    void clockSetRate(float rate, boolean postRateChangEvent);

    /**
     * @return Returns a {@link TaskQueue} that the associated
     *         {@link Presentation} should use for queueing asynchronous
     *         operations.
     */
    TaskQueue getTaskQueue();

    /**
     * @return Returns the {@link CallerContext} of the "owner" of the
     *         {@link PresentationContext}. The owner is the application on
     *         whose behalf the <code>PresentationContext</code> was created.
     */
    CallerContext getOwnerCallerContext();

    /**
     * Notify the context that the media is presenting. This happens if the
     * {@link Presentation#start()} call is successful. It may happen
     * synchronously or asynchronously, depending on the behavior of the native
     * layer. For example, starting and audio presentation generates this
     * immediately; however, for a service-based presentation, this happens upon
     * receipt of an asynchronous success event.
     */
    void notifyMediaPresented();

    /**
     * Notify the context that the decode of the presentation has been initiated, allowing the player to transition to
     * the Started state.
     */
    void notifyStarted();
    
    /**
     * Notify the context that a serious error has occurred in the
     * {@link Presentation} and that the presentation has been stopped.
     * 
     * @param reason
     *            This is a descriptive string of why it failed.
     * @param throwable
     *            optional exception
     */
    void notifyStopByError(String reason, Throwable throwable);

    /**
     * @see AlarmClock#createAlarm(AlarmClock.AlarmSpec ,
     *      org.cablelabs.impl.media.player.AlarmClock.Alarm.Callback)
     */
    Alarm createAlarm(AlarmClock.AlarmSpec spec, Alarm.Callback callback);

    /**
     * @see AlarmClock#destroyAlarm(Alarm)
     */
    void destroyAlarm(Alarm alarm);

    /**
     * Mute accessor (only applies to PresentationContext implementations supporting audio)
     *
     * @return true if audio is muted
     */
    boolean getMute();

    /**
     * Gain accessor (only applies to PresentationContext implementations supporting audio)
     *
     * @return gain or 0.0F if audio is not supported by implementation
     */
    float getGain();
}
