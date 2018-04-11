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

package javax.tv.util;

import org.cablelabs.impl.manager.ManagerManager;
import org.cablelabs.impl.manager.TimerManager;

/**
 * A class representing a timer.
 *
 * A timer is responsible for managing a set of timer events specified by timer
 * specifications. When the timer event should be sent, the timer calls the
 * timer specification's <code>notifyListeners()</code> method.
 *
 * @see TVTimerSpec
 *
 * @author: Alan Bishop
 */
public abstract class TVTimer
{
    /**
     * Constructs a TVTimer object.
     */
    public TVTimer() { }

    /**
     * Returns the default timer for the system. There may be one TVTimer
     * instance per virtual machine, one per applet, one per call to
     * <code>getTimer()</code>, or some other platform dependent implementation.
     *
     * @return A non-null TVTimer object.
     */
    public static TVTimer getTimer()
    {
        TimerManager tm = (TimerManager) ManagerManager.getInstance(TimerManager.class);
        return tm.getTimer();
    }

    /**
     * Begins monitoring a TVTimerSpec.
     *
     * <p>
     * When the timer specification should go off, the timer will call
     * <code>TVTimerSpec.notifyListeners().</code>
     * </p>
     *
     * <p>
     * Returns the actual <code>TVTimerSpec</code> that got scheduled. If you
     * schedule a specification that implies a smaller granularity than this
     * timer can provide, or a repeat timer specification that has a smaller
     * repeating interval than this timer can provide, the timer should round to
     * the closest value and return that value as a {@link TVTimerSpec} object.
     * An interested application can use accessor methods
     * {@link #getMinRepeatInterval} and {@link #getGranularity} to obtain the
     * Timer's best knowledge of the Timer's limitation on granularity and
     * repeat interval.
     * <p>
     * If you schedule an absolute specification that should have gone off
     * already, it will be scheduled to go off immediately, and the return value
     * of this method will be an absolute specification reflecting the current
     * time. The actual listener notification may happen asynchronously.
     * <p>
     * If the scheduled specification cannot be satisfied, the exception
     * {@link TVTimerScheduleFailedException} should be thrown.
     * </p>
     *
     * <p>
     * You may schedule a timer specification with multiple timers. You may
     * schedule a timer specification with the same timer multiple times (in
     * which case it will go off multiple times). If you modify a timer
     * specification after it has been scheduled with any timer, the results are
     * unspecified.
     * <p>
     * Note: The specified <code>TimerSpec</code> object may be modified by this
     * method, e.g., a delayed <code>TimerSpec</code> may be transformed into an
     * absolute <code>TimerSpec</code>.
     *
     * @param t
     *            The timer specification to begin monitoring.
     * @return The real TVTimerSpec that was scheduled.
     * @exception TVTimerScheduleFailedException
     *                is thrown when the scheduled specification cannot be
     *                satisfied.
     */
    public abstract TVTimerSpec scheduleTimerSpec(TVTimerSpec t) throws TVTimerScheduleFailedException;

    /**
     * Removes a timer specification from the set of monitored specifications.
     * The descheduling happens as soon as practical, but may not happen
     * immediately. If the timer specification has been scheduled multiple times
     * with this timer, all the schedulings are canceled. No other instances of
     * timer specifications shall be descheduled.
     * <p>
     * The specified <code>TVTimerSpec</code> instance <code>t</code> must be an
     * instance previously passed into the method {@link scheduleTimerSpec} on
     * the same instance of <code>TVTimer</code>. If it is not, no action is
     * performed.
     *
     * @param t
     *            The timer specification to end monitoring.
     */
    public abstract void deschedule(TVTimerSpec t);

    /**
     * Report the minimum interval that this timer can repeat tasks. For
     * example, it's perfectly reasonable for a Timer to specify that the
     * minimum interval for a repeatedly performed task is 1000 milliseconds
     * between every run. This is to avoid possible system overloading.
     *
     * @return The timer's best knowledge of minimum repeat interval in
     *         milliseconds. Return -1 if this timer doesn't know its repeating
     *         interval limitation.
     */
    public abstract long getMinRepeatInterval();

    /**
     * Report the granularity of this timer, i.e., the length of time between
     * "ticks" of this timer.
     *
     * @return The timer's best knowledge of the granularity in milliseconds.
     *         Return -1 if this timer doesn't know its granularity.
     *
     */
    public abstract long getGranularity();

}

/*
 * ***** EDITOR CONTROL STRINGS ***** Local Variables: tab-width: 8
 * c-basic-offset: 4 indent-tabs-mode: t End: vi:set ts=8 sw=4:
 * *********************************
 */
