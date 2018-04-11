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

package org.ocap.media;

import javax.media.Player;
import javax.media.Time;

import org.cablelabs.impl.media.player.AlarmClock;
import org.cablelabs.impl.media.player.AlarmClock.Alarm;
import org.cablelabs.impl.media.player.AlarmClock.AlarmException;
import org.cablelabs.impl.media.player.FixedAlarmSpec;
import org.cablelabs.impl.util.SystemEventUtil;

/**
 * This is a timer class that counts time based on a media time of a specified
 * Player. A media time is specified by the JMF specification. An application
 * can specify a range between a first time and a last time. When a current
 * media time exceeds this range, this timer fires and calls a
 * MediaTimerListener.notify() method. I.e., when a current media time passes
 * the last time, this timer fires and calls the notify() method with
 * MEDIA_WENTOFF_LAST event. On the other hand, when a current media time passes
 * the first time in a reverse playback or a skip playback, this timer fires and
 * calls the notify() method with MEDIA_WENTOFF_FIRST event.
 */
public class MediaTimer extends Object
{
    Time firstTime;

    Time lastTime;

    AlarmClock.Alarm firstAlarm;

    AlarmClock.Alarm lastAlarm;

    org.cablelabs.impl.media.player.Player player;

    MediaTimerListener mtl;

    boolean started;

    Object lock = new Object();

    /**
     * Constructor to make a MediaTimer object that counts time based on the
     * media time line of the specified Player.
     * 
     * @param p
     *            a JMF Player.
     */
    public MediaTimer(javax.media.Player p, MediaTimerListener listener)
    {
        if (p == null) throw new NullPointerException("null Player");
        if (listener == null) throw new NullPointerException("null MediaTimerListener");

        this.player = (org.cablelabs.impl.media.player.Player) p;
        this.mtl = listener;
    }

    class MediaTimeAlarmCallback implements AlarmClock.Alarm.Callback
    {
        public void fired(AlarmClock.Alarm alarm)
        {
            synchronized (lock)
            {
                if (alarm == firstAlarm)
                    mtl.notify(MediaTimerListener.TIMER_WENTOFF_FIRST, player);
                else if (alarm == lastAlarm) mtl.notify(MediaTimerListener.TIMER_WENTOFF_LAST, player);
            }
        }

        public void destroyed(Alarm alarm, AlarmException reason)
        {
            // TODO Auto-generated method stub

        }
    }

    private MediaTimeAlarmCallback callback = new MediaTimeAlarmCallback();

    /**
     * Set a first time of a time range. This MediaTimer object shall go off
     * when a current time passes the specified first time in a reverse playback
     * or a skip playback. A first time value specified in the past will be
     * cleared, when a new first time is set by this method.
     * 
     * @param time
     *            a time to go off.
     */
    public void setFirstTime(Time time)
    {
        synchronized (lock)
        {
            // Disable and remove the current first timer, if set.
            if (firstAlarm != null) player.destroyAlarm(firstAlarm);

            // Assign and the new one (if not null).
            if (time != null)
            {
                firstTime = time;
                AlarmClock.AlarmSpec firstSpec = new FixedAlarmSpec("first", time.getNanoseconds(),
                        AlarmClock.AlarmSpec.Direction.REVERSE);
                firstAlarm = player.createAlarm(firstSpec, callback);
                try
                {
                    if (started) firstAlarm.activate();
                }
                catch (AlarmException x)
                {
                    SystemEventUtil.logUncaughtException(x);
                    firstAlarm = null;
                    throw new IllegalStateException("could not set first time");
                }
            }
        }
    }

    /**
     * Set a last time of a time range. This MediaTimer object shall go off when
     * a current time passes the specified last time in a normal playback or a
     * skip playback. A last time value specified in the past will be cleared,
     * when a new last time is set by this method.
     * 
     * @param time
     *            a time to go off.
     */
    public void setLastTime(Time time)
    {
        synchronized (lock)
        {
            // Disable and remove the current first timer, if set.
            if (lastAlarm != null) player.destroyAlarm(lastAlarm);

            // Assign and the new one (if not null).
            if (time != null)
            {
                lastTime = time;
                AlarmClock.AlarmSpec lastSpec = new FixedAlarmSpec("last", time.getNanoseconds(),
                        AlarmClock.AlarmSpec.Direction.FORWARD);
                lastAlarm = player.createAlarm(lastSpec, callback);
                try
                {
                    if (started) lastAlarm.activate();
                }
                catch (AlarmException x)
                {
                    SystemEventUtil.logUncaughtException(x);
                    lastAlarm = null;
                    throw new IllegalStateException("could not set last time");
                }
            }
        }
    }

    /**
     * Get a first time that was set to this MediaTimer object.
     * 
     * @return a time to go off.
     */
    public Time getFirstTime()
    {
        synchronized (lock)
        {
            return firstTime;
        }
    }

    /**
     * Get a last time that was set to this MediaTimer object.
     * 
     * @return a time to go off.
     */
    public Time getLastTime()
    {
        synchronized (lock)
        {
            return lastTime;
        }
    }

    /**
     * Start this MediaTimer object. A MediaTimerListener is called with
     * TIMER_START event value by the OCAP implementation.
     */
    public void start()
    {
        synchronized (lock)
        {
            if (!started)
            {
                try
                {
                    if (firstAlarm != null) firstAlarm.activate();
                }
                catch (AlarmException x)
                {
                    SystemEventUtil.logUncaughtException(x);
                    throw new IllegalStateException("could not start first timer");
                }

                try
                {
                    if (lastAlarm != null) lastAlarm.activate();
                }
                catch (AlarmException x)
                {
                    SystemEventUtil.logUncaughtException(x);
                    firstAlarm.deactivate();
                    throw new IllegalStateException("could not start last timer");
                }
                started = true;
            }
        }
        mtl.notify(MediaTimerListener.TIMER_START, player);
    }

    /**
     * Stop this MediaTimer object. A MediaTimerListener is called with
     * TIMER_STOP event value by the OCAP implementation.
     */
    public void stop()
    {
        synchronized (lock)
        {
            if (started)
            {
                started = false;
                if (firstAlarm != null) firstAlarm.deactivate();
                if (lastAlarm != null) lastAlarm.deactivate();
            }
        }
        mtl.notify(MediaTimerListener.TIMER_STOP, player);
    }

    /**
     * Get a Player that was tied to this MediaTimer object by a constructor.
     * 
     * @return a Player that was tied to this MediaTimer object.
     */
    public Player getPlayer()
    {
        return player;
    }
}
