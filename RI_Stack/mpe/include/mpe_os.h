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

#ifndef _MPE_OS_H_
#define _MPE_OS_H_

#include "mpe_sys.h"
#include "../mgr/include/osmgr.h"

#define mpe_osmgr_ftable ((mpe_os_ftable_t*)(FTABLE[MPE_MGR_TYPE_OS]))

#define mpe_osInit                 ((mpe_osmgr_ftable->mpe_os_init_ptr))

#define mpe_dlmodOpen               ((mpe_osmgr_ftable->mpe_dlmodOpen_ptr))
#define mpe_dlmodClose              ((mpe_osmgr_ftable->mpe_dlmodClose_ptr))
#define mpe_dlmodGetSymbol          ((mpe_osmgr_ftable->mpe_dlmodGetSymbol_ptr))

/**
 * The following conditional remaps the memory allocation and deallocation functions to
 * profiling versions of those functions.
 * c - color, s - size, m - memory
 */
#ifdef MPE_FEATURE_MEM_PROF
#define mpe_memAlloc(s,m)           ((mpe_osmgr_ftable->mpe_memAllocPProf_ptr)(MPE_MEM_DEFAULT, s, m, __FILE__, __LINE__))
#define mpe_memFree(m)          ((mpe_osmgr_ftable->mpe_memFreePProf_ptr)(MPE_MEM_DEFAULT, m, __FILE__, __LINE__))
#define mpe_memRealloc(s,m)     ((mpe_osmgr_ftable->mpe_memReallocPProf_ptr)(MPE_MEM_DEFAULT, s, m, __FILE__, __LINE__))

#define mpe_memAllocP(c,s,m)        ((mpe_osmgr_ftable->mpe_memAllocPProf_ptr)(c, s, m, __FILE__, __LINE__))
#define mpe_memFreeP(c,m)           ((mpe_osmgr_ftable->mpe_memFreePProf_ptr)(c, m, __FILE__, __LINE__))
#define mpe_memReallocP(c,s,m)  ((mpe_osmgr_ftable->mpe_memReallocPProf_ptr)(c, s, m, __FILE__, __LINE__))
#else
#define mpe_memAlloc(s,m)           mpe_memAllocP(MPE_MEM_DEFAULT, s, m)
#define mpe_memFree(m)          mpe_memFreeP(MPE_MEM_DEFAULT, m)
#define mpe_memRealloc(s,m)     mpe_memReallocP(MPE_MEM_DEFAULT, s, m)

#define mpe_memAllocP               ((mpe_osmgr_ftable->mpe_memAllocP_ptr))
#define mpe_memFreeP                ((mpe_osmgr_ftable->mpe_memFreeP_ptr))
#define mpe_memReallocP         ((mpe_osmgr_ftable->mpe_memReallocP_ptr))
#endif /* MPE_FEATURE_MEM_PROF */

#define mpe_memAllocH               ((mpe_osmgr_ftable->mpe_memAllocH_ptr))
#define mpe_memFreeH                ((mpe_osmgr_ftable->mpe_memFreeH_ptr))
#define mpe_memReallocH             ((mpe_osmgr_ftable->mpe_memReallocH_ptr))
#define mpe_memLockH                ((mpe_osmgr_ftable->mpe_memLockH_ptr))
#define mpe_memPurge                ((mpe_osmgr_ftable->mpe_memPurge_ptr))
#define mpe_memCompact              ((mpe_osmgr_ftable->mpe_memCompact_ptr))
#define mpe_memGetFreeSize          ((mpe_osmgr_ftable->mpe_memGetFreeSize_ptr))
#define mpe_memGetLargestFree       ((mpe_osmgr_ftable->mpe_memGetLargestFree_ptr))

#define mpe_memStats                ((mpe_osmgr_ftable->mpe_memStats_ptr))
#define mpe_memGetStats             ((mpe_osmgr_ftable->mpe_memGetStats_ptr))
#define mpe_memRegisterMemFreeCallback  ((mpe_osmgr_ftable->mpe_memRegisterMemFreeCallback_ptr))
#define mpe_memUnregisterMemFreeCallback  ((mpe_osmgr_ftable->mpe_memUnregisterMemFreeCallback_ptr))
#define mpe_mutexNew                ((mpe_osmgr_ftable->mpe_mutexNew_ptr))
#define mpe_mutexDelete             ((mpe_osmgr_ftable->mpe_mutexDelete_ptr))
#define mpe_mutexAcquire            ((mpe_osmgr_ftable->mpe_mutexAcquire_ptr))
#define mpe_mutexAcquireTry         ((mpe_osmgr_ftable->mpe_mutexAcquireTry_ptr))
#define mpe_mutexRelease            ((mpe_osmgr_ftable->mpe_mutexRelease_ptr))

#define mpe_condNew                 ((mpe_osmgr_ftable->mpe_condNew_ptr))
#define mpe_condDelete              ((mpe_osmgr_ftable->mpe_condDelete_ptr))
#define mpe_condGet                 ((mpe_osmgr_ftable->mpe_condGet_ptr))
#define mpe_condWaitFor             ((mpe_osmgr_ftable->mpe_condWaitFor_ptr))
#define mpe_condSet                 ((mpe_osmgr_ftable->mpe_condSet_ptr))
#define mpe_condUnset               ((mpe_osmgr_ftable->mpe_condUnset_ptr))
#define mpe_threadCreate            ((mpe_osmgr_ftable->mpe_threadCreate_ptr))
#define mpe_threadDestroy           ((mpe_osmgr_ftable->mpe_threadDestroy_ptr))
#define mpe_threadAttach            ((mpe_osmgr_ftable->mpe_threadAttach_ptr))
#define mpe_threadSetPriority       ((mpe_osmgr_ftable->mpe_threadSetPriority_ptr))
#define mpe_threadGetStatus         ((mpe_osmgr_ftable->mpe_threadGetStatus_ptr))
#define mpe_threadSetStatus         ((mpe_osmgr_ftable->mpe_threadSetStatus_ptr))
#define mpe_threadGetData           ((mpe_osmgr_ftable->mpe_threadGetData_ptr))
#define mpe_threadSetData           ((mpe_osmgr_ftable->mpe_threadSetData_ptr))
#define mpe_threadGetPrivateData    ((mpe_osmgr_ftable->mpe_threadGetPrivateData_ptr))
#define mpe_threadGetCurrent        ((mpe_osmgr_ftable->mpe_threadGetCurrent_ptr))
#define mpe_threadSleep             ((mpe_osmgr_ftable->mpe_threadSleep_ptr))
#define mpe_threadYield             ((mpe_osmgr_ftable->mpe_threadYield_ptr))
#define mpe_threadSuspend           ((mpe_osmgr_ftable->mpe_threadSuspend_ptr))
#define mpe_threadResume            ((mpe_osmgr_ftable->mpe_threadResume_ptr))
#define mpe_threadSetName           ((mpe_osmgr_ftable->mpe_threadSetName_ptr))

#define mpe_timeGetMillis           ((mpe_osmgr_ftable->mpe_timeGetMillis_ptr))
#define mpe_timeGet                 ((mpe_osmgr_ftable->mpe_timeGet_ptr))
#define mpe_timeToDate              ((mpe_osmgr_ftable->mpe_timeToDate_ptr))
#define mpe_timeClock               ((mpe_osmgr_ftable->mpe_timeClock_ptr))
#define mpe_timeSystemClock         ((mpe_osmgr_ftable->mpe_timeSystemClock_ptr))
#define mpe_timeClockTicks          ((mpe_osmgr_ftable->mpe_timeClockTicks_ptr))
#define mpe_timeClockToMillis       ((mpe_osmgr_ftable->mpe_timeClockToMillis_ptr))
#define mpe_timeClockToTime         ((mpe_osmgr_ftable->mpe_timeClockToTime_ptr))
#define mpe_timeMillisToClock       ((mpe_osmgr_ftable->mpe_timeMillisToClock_ptr))
#define mpe_timeTimeToClock         ((mpe_osmgr_ftable->mpe_timeTimeToClock_ptr))
#define mpe_timeTmToTime            ((mpe_osmgr_ftable->mpe_timeTmToTime_ptr))

#define mpe_setJmp                  ((mpe_osmgr_ftable->mpe_setJmp_ptr))
#define mpe_longJmp                 ((mpe_osmgr_ftable->mpe_longJmp_ptr))

#define mpe_eventQueueNew           ((mpe_osmgr_ftable->mpe_eventQueueNew_ptr))
#define mpe_eventQueueDelete        ((mpe_osmgr_ftable->mpe_eventQueueDelete_ptr))
#define mpe_eventQueueSend          ((mpe_osmgr_ftable->mpe_eventQueueSend_ptr))
#define mpe_eventQueueNext          ((mpe_osmgr_ftable->mpe_eventQueueNext_ptr))
#define mpe_eventQueueWaitNext      ((mpe_osmgr_ftable->mpe_eventQueueWaitNext_ptr))

#define mpe_envGet                  ((mpe_osmgr_ftable->mpe_envGet_ptr))
#define mpe_stbGetPowerStatus       ((mpe_osmgr_ftable->mpe_stbGetPowerStatus_ptr))
#define mpe_stbGetAudioStatus       ((mpe_osmgr_ftable->mpe_stbGetAudioStatus_ptr))
#define mpe_registerForPowerKey     ((mpe_osmgr_ftable->mpe_registerForPowerKey_ptr))
#define mpe_stbBoot                 ((mpe_osmgr_ftable->mpe_stbBoot_ptr))
#define mpe_stbGetAcOutletState     ((mpe_osmgr_ftable->mpe_stbGetAcOutletState_ptr))
#define mpe_stbSetAcOutletState     ((mpe_osmgr_ftable->mpe_stbSetAcOutletState_ptr))
#define mpe_stbGetRootCerts         ((mpe_osmgr_ftable->mpe_stbGetRootCerts_ptr))

#define mpe_stbSetPowerStatus             ((mpe_osmgr_ftable->mpe_stbSetPowerStatus_ptr))
#define mpe_stbSetAudioStatus             ((mpe_osmgr_ftable->mpe_stbSetAudioStatus_ptr))
#define mpe_stbSetSystemMuteKeyControl    ((mpe_osmgr_ftable->mpe_stbSetSystemMuteKeyControl_ptr))
#define mpe_stbSetSystemVolumeKeyControl  ((mpe_osmgr_ftable->mpe_stbSetSystemVolumeKeyControl_ptr))
#define mpe_stbSetSystemVolumeRange       ((mpe_osmgr_ftable->mpe_stbSetSystemVolumeRange_ptr))
#define mpe_stbResetAllDefaults           ((mpe_osmgr_ftable->mpe_stbResetAllDefaults_ptr))

#define mpe_timeGetUTCTimeMillis          ((mpe_osmgr_ftable->mpe_timeGetUTCTimeMillis_ptr))

#endif  /* _MPE_OS_H_ */
