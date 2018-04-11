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

import javax.media.Clock;
import javax.media.ClockStartedError;
import javax.media.ClockStoppedException;
import javax.media.IncompatibleTimeBaseException;
import javax.media.StopTimeSetError;
import javax.media.Time;
import javax.media.TimeBase;

import org.apache.log4j.Logger;

/**
 * Implementation of the {@link Clock} interface. This class is used
 * to map between a TimeBase time and a Media time.
 * 
 * @author schoonma
 */
public class ClockImpl implements PlayerClock
{
    /** Log4j */
    private static final Logger log = Logger.getLogger(ClockImpl.class);

    /**
     * Internal value to indicate that a nanosecond time field (long) is
     * unassigned.
     */
    private static final long UNDEFINED = -1;

    /** This is the TimeBase to use for this Clock. */
    private TimeBase timeBase = new MediaTimeBase();

    /** Time-base start time, assigned by syncStart(). */
    private long timeBaseNanosAtLastSyncStart = UNDEFINED;

    /** Base media time (assigned when clock is stopped). */
    private long mediaTime = 0;

    /** Clock adjustment factor. */
    private float rate = 1;

    /** Media stop time, assigned by {@link #setStopTime(Time)}. */
    private long stopTime = UNDEFINED;

    private final String id;
    
    public ClockImpl()
    {
        //no need to use 'this' in the identity hash lookup, just a unique identifier
        this("Id: 0x" + Integer.toHexString(System.identityHashCode(new Object())) + ": ");
    }
    
    public ClockImpl(String id)
    {
        this.id = id;
    }
    
    private String getId()
    {
        return id;
    }
    
    public synchronized boolean isStarted()
    {
        // If time-base start time is UNDEFINED, then Clock is stopped.
        // It is initially UNDEFINED, and gets explicitly set to UNDEFINED by
        // stop().
        if (timeBaseNanosAtLastSyncStart == UNDEFINED)
        {
            return false;
        }

        // The only other way it can be stopped is if a stop time has been set.
        // Therefore, if stop-time hasn't been set, then it must be started.
        if (stopTime == UNDEFINED)
        {
            return true;
        }

        // syncStart() has been called, but time-base time hasn't yet reached
        // time-base start time,
        // so there's no way it could have hit the stop time yet, so it must be
        // started.
        long now = timeBase.getNanoseconds();
        if (now < timeBaseNanosAtLastSyncStart)
        {
            return true;
        }

        // Clock has reached time-base start time, so check whether it has
        // passed the stop time,
        // taking into account whether rate is negative or positive. If passed
        // the stop time,
        // reset the Clock. (If rate is zero, then Clock is started but not
        // moving toward a
        // stop time, so we can say that it is started.)

        long curMediaTime = (long) (((double) (now - timeBaseNanosAtLastSyncStart) * rate) + mediaTime);
        if ((rate > 0 && curMediaTime >= stopTime) || (rate < 0 && curMediaTime <= stopTime))
        {
            // We have gone past the scheduled stop time, so constrain media
            // time to the stop time.
            mediaTime = stopTime;
            if (log.isDebugEnabled())
            {
                log.debug(getId() + "isStarted() - past scheduled stop time - updating clock mediaTime to: " + mediaTime);
            }
            return false;
        }

        // Either the rate was zero or current media hasn't hit the stop time
        // yet.
        return true;
    }

    public synchronized String toString()
    {
        return "{started=" + isStarted() + ", timeBaseNanosAtLastSyncStart=" + timeBaseNanosAtLastSyncStart
                + ", mediaTime=" + mediaTime + ", rate=" + rate + ", stopTime=" + stopTime + "}";
    }

    public synchronized void setTimeBase(TimeBase t) throws IncompatibleTimeBaseException
    {
        if (isStarted())
        {
            throw new ClockStartedError("setTimeBase() cannot be used on a started clock.");
        }

        if (t == null)
        {
            // If the current tbt is not a SystemTimeBase, assign a new
            // SystemTimeBase.
            if (!(timeBase instanceof MediaTimeBase))
            {
                timeBase = new MediaTimeBase();
            }
            // If it's already a SystemTimeBase, there's no need to re-assign
            // it.
        }
        else
        {
            timeBase = t;
        }
    }

    public synchronized TimeBase getTimeBase()
    {
        return timeBase;
    }

    public synchronized void syncStart(Time syncStartUpdateTime)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "syncStart(" + syncStartUpdateTime + ")");
        }

        if (isStarted())
        {
            throw new ClockStartedError(getId() + "syncStart() cannot be used on an already started clock.");
        }

        // If we are already past 'syncStartUpdateTime', then use tbt intead of
        // syncStartUpdateTime.
        timeBaseNanosAtLastSyncStart = timeBase.getNanoseconds() > syncStartUpdateTime.getNanoseconds() ? timeBase.getNanoseconds()
                : syncStartUpdateTime.getNanoseconds();
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "updating timeBaseNanosAtLastSyncStart to: " + timeBaseNanosAtLastSyncStart);
        }
    }

    public synchronized void stop()
    {
        // Nothing to do if already stopped.
        if (!isStarted())
        {
            return;
        }

        // It's a change of state, so remember mediaTime.
        mediaTime = getMTNS();
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "stop() - clock mediaTime updated to: " + new Time(mediaTime));
        }

        // Reset the start time.
        timeBaseNanosAtLastSyncStart = UNDEFINED;
    }

    public synchronized void setStopTime(Time t)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "setStopTime(" + t + ")");
        }

        // Can't set more than once when started.
        if (isStarted() && stopTime != UNDEFINED && t != Clock.RESET)
        {
            throw new StopTimeSetError(getId() + "setStopTime() may be set only once on a Started Clock");
        }

        // Reset the stop time.
        if (t == Clock.RESET)
        {
            stopTime = UNDEFINED;
        }
        // Set the stop time.
        else
        {
            stopTime = t.getNanoseconds();
        }
    }

    public synchronized Time getStopTime()
    {
        // Return the Clock.RESET constant if no stop time.
        if (stopTime == UNDEFINED)
        {
            return Clock.RESET;
        }

        return new Time(stopTime);
    }

    public synchronized void setMediaTime(Time t)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "setMediaTime(" + t + ")");
        }

        if (isStarted())
        {
            throw new ClockStartedError(getId() + "setMediaTime() cannot be used on a started clock.");
        }
        mediaTime = t.getNanoseconds();
    }

    public synchronized Time getMediaTime()
    {
        return new Time(getMTNS());
    }

    public synchronized long getMediaNanoseconds()
    {
        return getMTNS();
    }

    /**
     * This helper method is called by {@link #getMediaNanoseconds()} and
     * {@link #getMediaTime()}. This allows <code>getMediaTime()</code> to not
     * be implemented by calling <code>getMediaNanoseconds()</code>, which
     * results in double-logging the media time.
     * 
     * @return mediatime in nanos
     */
    private long getMTNS()
    {
        if (!isStarted())
        {
            return mediaTime;
        }
        else
        {
            long now = timeBase.getNanoseconds();
            return now > timeBaseNanosAtLastSyncStart ? (long) ((double) (now - timeBaseNanosAtLastSyncStart) * rate)
                    + mediaTime : mediaTime;
        }
    }

    public synchronized Time getSyncTime()
    {
        // This only makes sense if the Clock is started.
        if (!isStarted())
        {
            return new Time(0);
        }

        // Compute how many nanoseconeds until we hit start time.
        long remaining = timeBaseNanosAtLastSyncStart - timeBase.getNanoseconds();
        return remaining > 0 ? new Time(remaining) : getMediaTime();
    }

    public synchronized Time mapToTimeBase(Time mediaTime) throws ClockStoppedException
    {
        if (!isStarted())
        {
            throw new ClockStoppedException();
        }

        Time result = new Time(
                (long) (mediaTime.getNanoseconds() - (this.mediaTime / rate) + timeBaseNanosAtLastSyncStart));
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "mapToTimeBase - mediaTime: " + mediaTime + ", rate: " + rate
                    + ", timeBaseNanosAtLastSyncStart: " + timeBaseNanosAtLastSyncStart + ", result: " + result);
        }
        return result;
    }

    public synchronized float getRate()
    {
        return rate;
    }

    public synchronized float setRate(float r)
    {
        if (log.isDebugEnabled())
        {
            log.debug(getId() + "setRate(" + r + ")");
        }

        if (isStarted())
        {
            throw new ClockStartedError(getId() + "setRate() cannot be used on a started clock.");
        }

        return rate = r;
    }

}
