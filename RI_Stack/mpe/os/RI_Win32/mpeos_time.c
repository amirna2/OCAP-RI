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

/* Header Files */
#include <mpe_types.h>      /* Resolve basic type references. */
#include <mpe_error.h>      /* Resolve error type reference. */
#include <mpeos_time.h>     /* Resolve time type references. */

#include <windows.h>

/**
 * <i>mpeos_timeGet()</i>
 *
 * Get the current time, which is a value representing seconds since midnight 1970.
 *
 * @param time Is a pointer for returning the current time value.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_timeGet(mpe_Time *ptime)
{
    if (ptime == NULL)
        return MPE_EINVAL;

    /* Current time + epoch delta. */
    *ptime = time(NULL);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_timeGetMillis()</i>
 *
 * Get the current time, which is a value representing milliseconds since
 * midnight 1970.
 *
 * @param time Is a pointer for returning the current time value.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_timeGetMillis(mpe_TimeMillis * pMpeTimeMillis)
{
    // Milliseconds between Jan 1 1601 00:00 and Jan 1 1970 00:00
#define WIN32_POSIX_TIME_OFF_MILLIS 0xa9730b66800LL

    static CRITICAL_SECTION g_targTimeMillis_lock;

    static mpe_Bool g_targTimeMillis_lock_initialised = FALSE;
    static LONGLONG millisSinceEpoch = 0;
    static LONGLONG lastTickSynch = 0;

    LONGLONG millisSinceSynch = 0;
    FILETIME f;

    if (pMpeTimeMillis == NULL)
        return MPE_EINVAL;

    if (!g_targTimeMillis_lock_initialised)
    {
        InitializeCriticalSection(&g_targTimeMillis_lock);
        g_targTimeMillis_lock_initialised = TRUE;
    }

    EnterCriticalSection(&g_targTimeMillis_lock);

    if (millisSinceEpoch) // In synch
    {
        LONGLONG millisSinceStart;

        // Get the number of milliseconds since the program start
        millisSinceStart = GetTickCount();

        // Calculate the number of milliseconds since the last synch
        millisSinceSynch = millisSinceStart - lastTickSynch;

        // Calculate the result
        *pMpeTimeMillis = millisSinceEpoch + millisSinceSynch;

        if (millisSinceSynch < 60000) // if less than 60 seconds then done
        {
            LeaveCriticalSection(&g_targTimeMillis_lock);
            return MPE_SUCCESS;
        }
    }

    // Synch the system time and the performance counter
    GetSystemTimeAsFileTime(&f);
    lastTickSynch = GetTickCount();
    millisSinceEpoch = (((((LONGLONG) f.dwHighDateTime) << 32)
            | f.dwLowDateTime) / 10000 - WIN32_POSIX_TIME_OFF_MILLIS);

    if (millisSinceSynch && (*pMpeTimeMillis > millisSinceEpoch)) // Went back in time!
        millisSinceEpoch = *pMpeTimeMillis; // Don't allow time reversal

    *pMpeTimeMillis = millisSinceEpoch;

    LeaveCriticalSection(&g_targTimeMillis_lock);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_timeToDate()</i>
 *
 * Convert a time value to a local date value.
 *
 * @param time Is the time value to convert.
 * @param local Is a pointer to a time structure to populate with the local
 *          date information as converted from the time value.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_timeToDate(mpe_Time mpeTime, mpe_TimeTm *local)
{
    struct tm *ltime;

    if (local == NULL)
        return MPE_EINVAL;

    ltime = localtime(&mpeTime);

    if (ltime == NULL)
        return MPE_EINVAL;

    local->tm_hour = ltime->tm_hour;
    local->tm_isdst = ltime->tm_isdst;
    local->tm_mday = ltime->tm_mday;
    local->tm_min = ltime->tm_min;
    local->tm_mon = ltime->tm_mon;
    local->tm_sec = ltime->tm_sec;
    local->tm_wday = ltime->tm_wday;
    local->tm_yday = ltime->tm_yday;
    local->tm_year = ltime->tm_year;

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_timeClock()</i>
 *
 * Get the best approximation of processor time used by associated process/thread.
 * This value should represent the number of system clock ticks utilized by the thread.
 * <p>
 * If the target platform does  not contain support for this call (-1) will be returned.
 *
 * @return The total number of clock ticks consumed by the caller or (-1) if
 *          the value can not be provided.
 */
mpe_TimeClock mpeos_timeClock(void)
{
    return clock();
}

/**
 * <i>mpeos_timeSystemClock()</i>
 *
 * Get the current value of the system clock ticks
 * <p>
 * If the target platform does  not contain support for this call (-1) will be returned.
 *
 * @return The total number of system clock ticks or (-1) if
 *          the value can not be provided.
 */
mpe_TimeClock mpeos_timeSystemClock(void)
{
    return -1;
}

/**
 * <i>mpeos_timeClockTicks()</i>
 *
 * Get the number of clock ticks per second used in the system for time keeping.
 *
 * @return The number of system clock ticks per second.
 */
mpe_TimeClock mpeos_timeClockTicks(void)
{
    return CLOCKS_PER_SEC;
}

/**
 * <i>mpeos_timeClockToMillis()</i>
 *
 * Convert a <i>mpe_TimeClock<i/> value to a uint32_t millisecond value.
 *
 * @param clk Is the clock tick value to convert.
 *
 * @return The milliseconds representation.
 */
uint32_t mpeos_timeClockToMillis(mpe_TimeClock clk)
{
    return ((uint32_t)((clk * 1000) / CLOCKS_PER_SEC));
}

/**
 * <i>mpeos_timeClockToTime()</i>
 *
 * convert a <i>mpe_TimeClock<i/> value to a <i>mpe_Time<i/> value.
 *
 * @param clock Is the clock tick value to convert.
 *
 * @return The converted time value.
 */
mpe_Time mpeos_timeClockToTime(mpe_TimeClock clk)
{
    return (mpe_Time)(clk / CLOCKS_PER_SEC);
}

/**
 * <i>mpeos_timeMillisToClock()</i>
 *
 * Convert a time value expressed in milliseconds to a system clock tick value.
 *
 * @param milliseconds Is time value in milliseconds to convert.
 *
 * @return The converted time value expressed in system clock ticks.
 */
mpe_TimeClock mpeos_timeMillisToClock(uint32_t ms)
{
    return (mpe_TimeClock)((ms * CLOCKS_PER_SEC) / 1000);
}

/**
 * <i>mpeos_timeTimetoClock()</i>
 *
 * Convert a <i>mpe_Time<i/> time value to a system clock tick value.
 *
 * @param time Is the time value to convert.
 *
 * @return The converted time value expressed in system clock ticks.
 */
mpe_TimeClock mpeos_timeTimeToClock(mpe_Time mpeTime)
{
    return (clock_t)(mpeTime * CLOCKS_PER_SEC);
}

/**
 * <i>mpeos_timeTmToTime()</i>
 *
 * Convert a <i>mpe_TimeTm<i/> time value to a <i>mpe_Time<i/> value.
 *
 * @param tm Is a pointer to the time structure containing the value to convert.
 *
 * @return The converted time value.
 */
mpe_Time mpeos_timeTmToTime(mpe_TimeTm *_tm)
{
    /* Sanity check... */
    if (_tm == NULL)
        return 0;

    return mktime(_tm);
}
