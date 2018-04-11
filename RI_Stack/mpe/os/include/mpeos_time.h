#if !defined(_MPEOS_TIME_H)
#define _MPEOS_TIME_H
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
#include <mpe_types.h>		/* Resolve basic type references. */
#include <os_types.h>
#include <os_time.h>

#ifdef __cplusplus
extern "C"
{
#endif

/***
 * Time/Date type definitions:
 */

#define MPE_EPOCH_DELTA		OS_EPOCH_DELTA

typedef os_Clock mpe_TimeClock;
typedef os_Time mpe_Time;
typedef os_Tm mpe_TimeTm;
typedef os_TimeVal mpe_TimeVal;
typedef os_TimeMillis mpe_TimeMillis;

/***
 * Time/Date API prototypes:
 */

/**
 * The <i>mpeos_timeGet()</i> function will get the current time, which is
 * a value representing seconds since midnight 1970.
 *
 * @param time is a pointer for returning the current time value.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_timeGet(mpe_Time *time);

/**
 * The mpeos_timeGetMillis() function will get a representation of time
 * in milliseconds with no worse than 10ms precision. The value returned by this
 * function is guaranteed to increase monotonically over the duration of time the
 * system is up. If SITP.SI.STT.ENABLED is set to TRUE in the mpeenv.ini, the
 * implementation of this API need not return any real representation of UTC
 * time. If SITP.SI.STT.ENABLED is set to FALSE, this API MUST always return
 * an accurate representation of cable-network UTC time (milliseconds since
 * midnight 1970) and MUST NOT be subjectto large time corrections.
 *
 * @param time is a pointer for returning the 64-bit current time value.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_timeGetMillis(mpe_TimeMillis *time);

/**
 * The <i>mpeos_timeToDate()</i> function will convert a time value to a local
 * date value.
 *
 * @param time is the time value to convert.
 * @param local is a pointer to a time structure to populate with the local
 *          date information as converted from the time value.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_timeToDate(mpe_Time time, mpe_TimeTm *local);

/**
 * The <i>mpeos_timeClock()</i> function will get the best approximation of 
 * processor time used by associated process/thread.  This value should 
 * represented the number of system clock ticks utilized by the thread.
 * <p>
 * If the target platform does  not contain support for this call (-1) will be returned.  
 *
 * @return the total number of clock ticks consumed by the caller or (-1) if
 *          the value can not be provided.
 */
mpe_TimeClock mpeos_timeClock(void);

/**
 * 
 *
 * The <i>mpeos_timeSystemClock()</i> function will get the current value
 * of the system clock ticks
 * <p>
 * If the target platform does  not contain support for this call (-1) will be returned.  
 *
 * @return the total number of system clock ticks or (-1) if
 *          the value can not be provided.
 */
mpe_TimeClock mpeos_timeSystemClock(void);

/**
 * The <i>mpeos_timeClockTicks()</i> function will get the number of clock ticks
 * per second used in the system for time keeping.
 *
 * @return the number of system clock ticks per second.
 */
mpe_TimeClock mpeos_timeClockTicks(void);

/**
 * The <i>mpeos_timeClockToMillis()</i> function will convert a <i>mpe_TimeClock<i/>
 * value to a uint32_t millisecond value.
 *
 * @param clock is the clock tick value to convert.
 * @return the milliseconds representation.
 */
uint32_t mpeos_timeClockToMillis(mpe_TimeClock clock);

/**
 * The <i>mpeos_timeClockToTime()</i> function will convert a <i>mpe_TimeClock<i/>
 * value to a <i>mpe_Time<i/> value.
 *
 * @param clock is the clock tick value to convert.
 * @return the converted time value.
 */
mpe_Time mpeos_timeClockToTime(mpe_TimeClock clock);

/**
 * The <i>mpeos_timeMillisToClock()</i> function will convert a time value
 * expressed in milliseconds to a system clock tick value.
 *
 * @param milliseconds is time value in milliseconds to convert.
 * @return the converted time value expressed in system clock ticks.
 */
mpe_TimeClock mpeos_timeMillisToClock(uint32_t milliseconds);

/**
 * The <i>mpeos_timeTimetoClock()</i> function will convert a <i>mpe_Time<i/>
 * time value to a system clock tick value.
 * 
 * @param time is the time value to convert.
 * @return the converted time value expressed in system clock ticks.
 */
mpe_TimeClock mpeos_timeTimeToClock(mpe_Time time);

/**
 * The <i>mpeos_timeTmToTime()</i> function will convert a <i>mpe_TimeTm<i/> time
 * value to a <i>mpe_Time<i/> value.
 *
 * @param tm is a pointer to the time structure containing the value to convert.
 * @return the converted time value.
 */
mpe_Time mpeos_timeTmToTime(mpe_TimeTm *tm);

#ifdef __cplusplus
}
#endif
#endif /* _MPEOS_TIME_H */
