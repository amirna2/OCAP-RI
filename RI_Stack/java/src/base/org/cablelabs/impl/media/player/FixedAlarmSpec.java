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

import javax.media.Time;

import org.apache.log4j.Logger;

/**
 * Represents an alarmSpec whose alarm time is fixed and applied only in a forward or reverse direction
 */
public class FixedAlarmSpec implements AlarmClock.AlarmSpec
{
    private static final Logger log = Logger.getLogger(FixedAlarmSpec.class);

    private final String name;

    private final long mediaTimeNanos;

    private final byte direction;

    /**
     * Construct the {@link FixedAlarmSpec}.
     *
     * @param alarmName
     *            Assigned to the {@link #name} field.
     * @param mediaTimeNanos
     *            Assigned to the {@link #mediaTimeNanos} field.
     * @param playbackDirection
     *            Assigned to the {@link #direction} field.
     */
    public FixedAlarmSpec(String alarmName, long mediaTimeNanos, byte playbackDirection)
    {
        this.name = alarmName;
        this.mediaTimeNanos = mediaTimeNanos;
        this.direction = playbackDirection;
    }

    /** The name of the alarm spec (used for logging). */
    public String getName()
    {
        return name;
    }

    /** The media time at which the alarm is to be triggered. */
    public long getMediaTimeNanos()
    {
        return mediaTimeNanos;
    }

    public long getDelayWallTimeNanos(long baseMediaTimeNanos, float rate)
    {
        return (long) ((mediaTimeNanos - baseMediaTimeNanos) / rate);
    }

    public boolean canSchedule(float rate, long baseMediaTimeNanos)
    {
        if (rate == 0)
        {
            if (log.isDebugEnabled())
            {
                log.debug("canSchedule - returning false because rate is zero - playback mediatime: " + new Time(baseMediaTimeNanos) + ": " + this);
            }
            return false;
        }

        // If rate is not in the same direction as the alarm, skip.
        int d = direction;
        if ((rate > 0 && d < 0) || (rate < 0 && d > 0))
        {
            if (log.isDebugEnabled())
            {
                log.debug("canSchedule - returning false because playback rate does not match alarmspec direction - rate: " + rate + ", playback mediatime: " + new Time(baseMediaTimeNanos) + ": " + this);
            }
            return false;
        }

        // Ok, it's in the same direction, but is it "ahead"? I.e., if play
        // continues,
        // will the media time be "hit".
        if ((rate > 0 && baseMediaTimeNanos > getMediaTimeNanos()) || (rate < 0 && baseMediaTimeNanos < getMediaTimeNanos()))
        {
            if (log.isDebugEnabled())
            {
                log.debug("canSchedule - returning false because alarmspec firing time is behind playback time - rate: " + rate + ", playback mediatime: " + new Time(baseMediaTimeNanos) + ": " + this);
            }
            return false;
        }
        if (log.isInfoEnabled())
        {
            log.info("canSchedule - returning true - rate: " + rate + ", playback mediatime: " + new Time(baseMediaTimeNanos) +": " + this);
        }
        return true;
    }

    public boolean shouldFire(long currentMediaTimeNanos, long newMediaTimeNanos)
    {
        //don't fire if equal (can come back through here due to a an update of rate without a change in mediatime)
        if (newMediaTimeNanos > currentMediaTimeNanos && newMediaTimeNanos > mediaTimeNanos && currentMediaTimeNanos < mediaTimeNanos && Direction.FORWARD == direction)
        {
            if (log.isInfoEnabled())
            {
                log.info("shouldFire - returning true - crossed over alarm by navigating forward - currentMediaTime: " + new Time(currentMediaTimeNanos) + ", newMediaTime: " + new Time(newMediaTimeNanos) + ": " + this);
            }
            return true;
        }
        //navigating backward over a backward-direction alarm
        if (newMediaTimeNanos < currentMediaTimeNanos && newMediaTimeNanos < mediaTimeNanos && currentMediaTimeNanos > mediaTimeNanos && Direction.REVERSE == direction)
        {
            if (log.isInfoEnabled())
            {
                log.info("shouldFire - returning true - crossed over alarm by navigating backward - currentMediaTime: " + new Time(currentMediaTimeNanos) + ", newMediaTimeNanos: " + new Time(newMediaTimeNanos) + ": " + this);
            }
            return true;
        }
        if (log.isDebugEnabled())
        {
            log.debug("shouldFire - returning false - currentMediaTime: " + new Time(currentMediaTimeNanos) + ", newMediaTimeNanos: " + new Time(newMediaTimeNanos) + ": " + this);
        }
        return false;
    }

    public String toString()
    {
        return "FixedAlarmSpec [name=" + name + ", time=" + new Time(mediaTimeNanos) + ", direction=" + direction + ", id: 0x" + Integer.toHexString(hashCode()).toUpperCase() +"]";
    }
}
