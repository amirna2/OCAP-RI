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

#ifndef _MPETEST_SYS_H_
#define _MPETEST_SYS_H_ 1

#ifdef TEST_MPEOS
# define MPETEST_SYS(x)    mpeos_ ## x
#else
# define MPETEST_SYS(x)    mpe_ ## x
#endif /* TEST_MPEOS */

/**
 * MPE / MPEOS function names are re-defined here using macros, in order to
 * support MPE or MPEOS tests using the same test code.
 *
 * If #define TEST_MPEOS is defined, then tests will be for MPEOS, else MPE.
 */

#define dbgMsg               MPETEST_SYS(dbgMsg)
#define dlmodOpen            MPETEST_SYS(dlmodOpen)
#define dlmodClose           MPETEST_SYS(dlmodClose)
#define dlmodGetSymbol       MPETEST_SYS(dlmodGetSymbol)
#define memAlloc             MPETEST_SYS(memAlloc)
#define memFree              MPETEST_SYS(memFree)
#define memRealloc           MPETEST_SYS(memRealloc)
#define memInit              MPETEST_SYS(memInit)
#define memAllocP            MPETEST_SYS(memAllocP)
#define memFreeP             MPETEST_SYS(memFreeP)
#define memReallocP          MPETEST_SYS(memReallocP)
#define memAllocH            MPETEST_SYS(memAllocH)
#define memFreeH             MPETEST_SYS(memFreeH)
#define memReallocH          MPETEST_SYS(memReallocH)
#define memLockH             MPETEST_SYS(memLockH)
#define memCompact           MPETEST_SYS(memCompact)
#define memPurge             MPETEST_SYS(memPurge)
#define memGetFreeSize       MPETEST_SYS(memGetFreeSize)
#define memGetLargestFree    MPETEST_SYS(memGetLargestFree)
#define memGetStats          MPETEST_SYS(memGetStats)
#define memStats             MPETEST_SYS(memStats)
#define mutexNew             MPETEST_SYS(mutexNew)
#define mutexDelete          MPETEST_SYS(mutexDelete)
#define mutexAcquire         MPETEST_SYS(mutexAcquire)
#define mutexAcquireTry      MPETEST_SYS(mutexAcquireTry)
#define mutexRelease         MPETEST_SYS(mutexRelease)
#define condNew              MPETEST_SYS(condNew)
#define condDelete           MPETEST_SYS(condDelete)
#define condGet              MPETEST_SYS(condGet)
#define condWaitFor          MPETEST_SYS(condWaitFor)
#define condSet              MPETEST_SYS(condSet)
#define condUnset            MPETEST_SYS(condUnset)
#define threadCreate         MPETEST_SYS(threadCreate)
#define threadDestroy        MPETEST_SYS(threadDestroy)
#define threadAttach         MPETEST_SYS(threadAttach)
#define threadSetPriority    MPETEST_SYS(threadSetPriority)
#define threadGetStatus      MPETEST_SYS(threadGetStatus)
#define threadSetStatus      MPETEST_SYS(threadSetStatus)
#define threadGetData        MPETEST_SYS(threadGetData)
#define threadSetData        MPETEST_SYS(threadSetData)
#define threadGetCurrent     MPETEST_SYS(threadGetCurrent)
#define threadSleep     MPETEST_SYS(threadSleep)
#define threadYield     MPETEST_SYS(threadYield)
#define timeGetMillis   MPETEST_SYS(timeGetMillis)
#define timeGet         MPETEST_SYS(timeGet)
#define timeToDate      MPETEST_SYS(timeToDate)
#define timeClock       MPETEST_SYS(timeClock)
#define timeSystemClock MPETEST_SYS(timeSystemClock)
#define timeClockTicks  MPETEST_SYS(timeClockTicks)
#define timeClockToMillis    MPETEST_SYS(timeClockToMillis)
#define timeClockToTime  MPETEST_SYS(timeClockToTime)
#define timeMillisToClock    MPETEST_SYS(timeMillisToClock)
#define timeTimeToClock      MPETEST_SYS(timeTimeToClock)
#define timeTmToTime    MPETEST_SYS(timeTmToTime)
#define eventQueueNew   MPETEST_SYS(eventQueueNew)
#define eventQueueDelete     MPETEST_SYS(eventQueueDelete)
#define eventQueueSend  MPETEST_SYS(eventQueueSend)
#define eventQueueNext  MPETEST_SYS(eventQueueNext)
#define eventQueueWaitNext   MPETEST_SYS(eventQueueWaitNext)
#define atomicDecrement      MPETEST_SYS(atomicDecrement)
#define atomicIncrement      MPETEST_SYS(atomicIncrement)
#define atomicOperation      MPETEST_SYS(atomicOperation)

/*  UTIL API  */

#define envGet                  MPETEST_SYS(envGet)
#define envInit                 MPETEST_SYS(envInit)
#define envSet                  MPETEST_SYS(envSet)
#define iniSetPath              MPETEST_SYS(iniSetPath)
#define longJmp                 MPETEST_SYS(longJmp)
#define registerForPowerKey     MPETEST_SYS(registerForPowerKey)
#define setJmp                  MPETEST_SYS(setJmp)
#define stbBoot                 MPETEST_SYS(stbBoot)
#define stbBootStatus           MPETEST_SYS(stbBootStatus)
#define stbGetAcOutletState     MPETEST_SYS(stbGetAcOutletState)
#define stbGetPowerStatus       MPETEST_SYS(stbGetPowerStatus)
#define stbGetRootCerts         MPETEST_SYS(stbGetRootCerts)
#define stbSetAcOutletState     MPETEST_SYS(stbSetAcOutletState)

#endif /* _MPETEST_SYS_H_ */ 
