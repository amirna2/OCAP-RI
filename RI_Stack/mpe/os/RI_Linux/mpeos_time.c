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

#include <sys/time.h>       /* gettimeofday(2) */
#include <sys/times.h>      /* times(2) */
#include <stdio.h>          /* stderr */
#include <unistd.h>         /* sysconf(3) */
#include <string.h>         /* memcpy(3) */

#include <mpe_types.h>
#include <mpe_error.h>
#include <mpeos_time.h>

/***
 *** File-Private Function Prototypes
 ***/
static long getTicksPerSec(void);

/***
 *** File-Scope Variables
 ***/
//static clock_t g_offset_ticks=0; // for calculating system clock-ticks value (re: times(3))

/**
 * <i>mpeos_timeGet()</i>
 *
 * Get the current time, which is a value representing seconds since midnight 1970.
 *
 * @param time is a pointer for returning the current time value.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_timeGet(mpe_Time *ptime)
{
    if (NULL == ptime)
    {
        return MPE_EINVAL;
    }
    *ptime = time(NULL);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_timeGetMillis()</i>
 *
 * Get the current time, which is a value representing milliseconds since 
 * midnight 1970.
 *
 * @param time is a pointer for returning the current time value.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_timeGetMillis(mpe_TimeMillis *ptime)
{
    int ret;
    struct timeval tv;
    struct timezone tz;
    mpe_TimeMillis msec;

    if (NULL == ptime)
    {
        return MPE_EINVAL;
    }

    ret = gettimeofday(&tv, &tz);
    if (0 != ret)
    {
        // TODO - should there be an MPE_MOD_TIME ???
        (void) fprintf(stderr, "mpeos_timeGetMillis(): gettimeofday(): %d.\n",
                ret);
        return MPE_EINVAL;
    }

    msec = tv.tv_sec;
    msec *= 1000;
    msec += (tv.tv_usec / 1000);
    *ptime = msec;
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_timeToDate()</i> 
 *
 * Convert a time value to a local date value.
 *
 * @param time is the time value to convert.
 * @param local is a pointer to a time structure to populate with the local
 *          date information as converted from the time value.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_timeToDate(mpe_Time time, mpe_TimeTm *local)
{
    mpe_TimeTm *mylocal_p;
    if (NULL == local)
    {
        return MPE_EINVAL;
    }
    mylocal_p = localtime(&time);
    if (NULL == mylocal_p)
    {
        return MPE_EINVAL;
    }
    (void) memcpy(local, mylocal_p, sizeof(mpe_TimeTm));
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_timeSystemClock()</i> 
 *
 * Get the current value of the system clock ticks
 * <p>
 * If the target platform does  not contain support for this call (-1) will be returned.  
 *
 * @return the total number of system clock ticks (-1) if
 *          the value can not be provided.
 */
mpe_TimeClock mpeos_timeSystemClock(void)
{
    return (mpe_TimeClock) - 1;
}

/**
 * <i>mpeos_timeClock()</i> 
 *
 * Get the best approximation of processor time used by associated process/thread.  
 * This value should represent the number of system clock ticks utilized by the thread.
 * <p>
 * If the target platform does  not contain support for this call (-1) will be returned.  
 *
 * @return the total number of clock ticks consumed by the caller or (-1) if
 *          the value can not be provided.
 */
mpe_TimeClock mpeos_timeClock(void)
{
    return clock();
}

/**
 * <i>mpeos_timeClockTicks()</i> 
 *
 * Get the number of clock ticks per second used in the system for time keeping.
 *
 * @return the number of system clock ticks per second.
 */
mpe_TimeClock mpeos_timeClockTicks(void)
{
    return (mpe_TimeClock) getTicksPerSec();
}

/**
 * <i>mpeos_timeClockToMillis()</i> 
 *
 * Convert a <i>mpe_TimeClock<i/> value to a uint32_t millisecond value.
 *
 * @param clk is the clock tick value to convert.
 * @return the milliseconds representation.
 */
uint32_t mpeos_timeClockToMillis(mpe_TimeClock clk)
{
    /* N.B. in the following expression, order DOES matter! */
    return (uint32_t)((clk * 1000) / getTicksPerSec());
}

/**
 * <i>mpeos_timeClockToTime()</i> 
 *
 * convert a <i>mpe_TimeClock<i/> value to a <i>mpe_Time<i/> value.
 *
 * @param clock is the clock tick value to convert.
 *
 * @return the converted time value.
 */
mpe_Time mpeos_timeClockToTime(mpe_TimeClock clk)
{
    return (mpe_Time)(clk / getTicksPerSec());
}

/**
 * <i>mpeos_timeMillisToClock()</i> 
 *
 * Convert a time value expressed in milliseconds to a system clock tick value.
 *
 * @param milliseconds is time value in milliseconds to convert.
 *
 * @return the converted time value expressed in system clock ticks.
 */
mpe_TimeClock mpeos_timeMillisToClock(uint32_t ms)
{
    return ((ms * getTicksPerSec()) / 1000);
}

/**
 * <i>mpeos_timeTimetoClock()</i> 
 *
 * Convert a <i>mpe_Time<i/> time value to a system clock tick value.
 * 
 * @param time is the time value to convert.
 *
 * @return the converted time value expressed in system clock ticks.
 */
mpe_TimeClock mpeos_timeTimeToClock(mpe_Time time)
{
    return (clock_t)(time * getTicksPerSec());
}

/**
 * <i>mpeos_timeTmToTime()</i> 
 *
 * Convert a <i>mpe_TimeTm<i/> time value to a <i>mpe_Time<i/> value.
 *
 * @param tm is a pointer to the time structure containing the value to convert.
 *
 * @return the converted time value.
 */
mpe_Time mpeos_timeTmToTime(mpe_TimeTm *_tm)
{
    if (NULL == _tm)
    {
        return MPE_EINVAL;
    }
    return mktime(_tm);
}

/***
 *** File-Private Functions
 ***/

/*
 * If this value cannot change, a static variable could be used to
 * "cache" it.
 */
static long getTicksPerSec()
{
    long ticksPerSec = sysconf(_SC_CLK_TCK);
    if (-1 == ticksPerSec)
    {
        (void) fprintf(stderr,
                "mpeos_timeClockTicks() sysconf(_SC_CLK_TCK): %ld\n",
                ticksPerSec);
    }
    return ticksPerSec;
}
