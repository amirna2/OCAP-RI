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

#include "javavm/include/porting/time.h"
#include <mpe_os.h>
#include <mpe_types.h>

CVMInt64 CVMtimeMillis(void)
{
    mpe_TimeMillis time = 0;
    mpe_timeGetUTCTimeMillis(&time);
    return (CVMInt64) time;
}

/*
 * Initialize the high-resolution clocks.  Called during VM startup.
 */
void CVMtimeClockInit(void)
{
}

/*
 * Initialize the per-thread high-resolution timer.  Called during the
 * thread creation process in the VM.
 * args:
 *  threadID - CVM thread ID (i.e. not the underlying OS thread id)
 */
void CVMtimeThreadCpuClockInit(CVMThreadID *threadID)
{
    (void) threadID;
}

/*
 * Returns the current value of the most precise available system
 * timer, in nanoseconds.
 * This timer only returns elapsed time and not time-of-day time.
 * This method provides nanosecond precision, but not
 * necessarily nanosecond accuracy.
 */
CVMInt64 CVMtimeNanosecs(void)
{
    return CVMtimeMillis() * 1000000;
}

/*
 * Thread CPU Time - return the fast estimate on a platform
 * For example:
 * On Solaris - call gethrvtime (fast) - user time only
 * On Linux   - fast clock_gettime where available - user+sys
 *            - otherwise: very slow /proc fs - user+sys
 * On Windows - GetThreadTimes - user+sys
 */
CVMInt64 CVMtimeThreadCpuTime(CVMThreadID *threadID)
{
    return -1;
}

/*
 * CVMtimeCurrentThreadCputime() is included in the HPI because 
 * the OS may provide a more efficient implementation than going
 * through CVMtimeThreadCputime().
 * If such an efficient implementation does not exists, the port
 * can choose to implement it as a call to CVMtimeThreadCputime()
 * instead.
 * The caller from shared code will always pass the CVMThreadID of
 * the current thread.  This is for the convenient of the port.
 */
CVMInt64 CVMtimeCurrentThreadCpuTime(CVMThreadID *threadID)
{
    return -1;
}

/*
 * Returns true if this OS supports per-thread cpu timers
 */
CVMBool CVMtimeIsThreadCpuTimeSupported()
{
    return CVM_FALSE;
}

