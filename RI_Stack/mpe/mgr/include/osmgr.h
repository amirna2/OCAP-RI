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

#ifndef _OSMGR_H_
#define _OSMGR_H_

#ifdef __cplusplus
extern "C"
{
#endif

#include <mpe_error.h>
#include <mpeos_dll.h>
#include <mpeos_thread.h>
#include <mpeos_time.h>
#include <mpeos_util.h>
#include <mpeos_mem.h>
#include <mpeos_sync.h>
#include <mpeos_thread.h>
#include <mpeos_event.h>
#include "sysmgr.h"
#include "mgrdef.h"

void mpe_osSetup(void);

/* OS manager function table definition */
typedef struct
{
    void (*mpe_os_init_ptr)(void);

    mpe_Error (*mpe_dlmodOpen_ptr)(const char *name, mpe_Dlmod *dlmodId);
    mpe_Error (*mpe_dlmodClose_ptr)(mpe_Dlmod dlmodId);
    mpe_Error (*mpe_dlmodGetSymbol_ptr)(mpe_Dlmod dlmodId, const char *symbol,
            void **value);

    mpe_Error (*mpe_memAllocP_ptr)(mpe_MemColor color, uint32_t size,
            void **memory);
    mpe_Error (*mpe_memFreeP_ptr)(mpe_MemColor color, void *memory);
    mpe_Error (*mpe_memReallocP_ptr)(mpe_MemColor color, uint32_t size,
            void **memory);
    mpe_Error (*mpe_memGetFreeSize_ptr)(mpe_MemColor color, uint32_t *freeSize);
    mpe_Error (*mpe_memGetLargestFree_ptr)(mpe_MemColor color,
            uint32_t *freeSize);

    mpe_Error (*mpe_memAllocH_ptr)(mpe_MemColor color, uint32_t size,
            uint32_t priority, mpe_MemHandle *h);
    mpe_Error (*mpe_memFreeH_ptr)(mpe_MemColor color, mpe_MemHandle h);
    mpe_Error (*mpe_memReallocH_ptr)(mpe_MemColor color, uint32_t size,
            uint32_t priority, mpe_MemHandle *h);
    mpe_Error (*mpe_memLockH_ptr)(mpe_MemHandle h, void** ptr);
    mpe_Error (*mpe_memPurge_ptr)(mpe_MemColor color, uint32_t priority);
    mpe_Error (*mpe_memCompact_ptr)(mpe_MemColor color);

    void (*mpe_memStats_ptr)(mpe_Bool, mpe_MemColor, const char*);
    mpe_Error (*mpe_memRegisterMemFreeCallback_ptr)(mpe_MemColor,
            int32_t(*function)(mpe_MemColor, int32_t, int64_t, void *), void *);
    mpe_Error (*mpe_memUnregisterMemFreeCallback_ptr)(mpe_MemColor,
            int32_t(*function)(mpe_MemColor, int32_t, int64_t, void *), void *);
    mpe_Error (*mpe_memGetStats_ptr)(uint32_t statsSize,
            mpe_MemStatsInfo *stats);
    mpe_Error (*mpe_memAllocPProf_ptr)(mpe_MemColor color, uint32_t size,
            void **memory, char* fileName, uint32_t lineNum);
    mpe_Error (*mpe_memFreePProf_ptr)(mpe_MemColor color, void *memory,
            char* fileName, uint32_t lineNum);
    mpe_Error (*mpe_memReallocPProf_ptr)(mpe_MemColor color, uint32_t size,
            void **memory, char* fileName, uint32_t lineNum);

    mpe_Error (*mpe_mutexNew_ptr)(mpe_Mutex *mutex);
    mpe_Error (*mpe_mutexDelete_ptr)(mpe_Mutex mutex);
    mpe_Error (*mpe_mutexAcquire_ptr)(mpe_Mutex mutex);
    mpe_Error (*mpe_mutexAcquireTry_ptr)(mpe_Mutex mutex);
    mpe_Error (*mpe_mutexRelease_ptr)(mpe_Mutex mutex);

    mpe_Error (*mpe_condNew_ptr)(mpe_Bool autoReset, mpe_Bool initialState,
            mpe_Cond *cond);
    mpe_Error (*mpe_condDelete_ptr)(mpe_Cond cond);
    mpe_Error (*mpe_condGet_ptr)(mpe_Cond cond);
    mpe_Error (*mpe_condWaitFor_ptr)(mpe_Cond cond, uint32_t timeout);
    mpe_Error (*mpe_condSet_ptr)(mpe_Cond cond);
    mpe_Error (*mpe_condUnset_ptr)(mpe_Cond cond);

    mpe_Error (*mpe_threadCreate_ptr)(void(*entry)(void *), void *data,
            uint32_t priority, uint32_t stackSize, mpe_ThreadId *threadId,
            const char *name);
    mpe_Error (*mpe_threadDestroy_ptr)(mpe_ThreadId threadId);
    mpe_Error (*mpe_threadAttach_ptr)(mpe_ThreadId *threadId);
    mpe_Error (*mpe_threadSetPriority_ptr)(mpe_ThreadId threadId,
            uint32_t priority);
    mpe_Error (*mpe_threadGetStatus_ptr)(mpe_ThreadId threadId,
            uint32_t *threadStatus);
    mpe_Error (*mpe_threadSetStatus_ptr)(mpe_ThreadId threadId,
            uint32_t threadStatus);
    mpe_Error (*mpe_threadGetData_ptr)(mpe_ThreadId threadId,
            void **threadLocals);
    mpe_Error (*mpe_threadSetData_ptr)(mpe_ThreadId threadId,
            void *threadLocals);
    mpe_Error (*mpe_threadGetCurrent_ptr)(mpe_ThreadId *threadId);
    mpe_Error (*mpe_threadSleep_ptr)(uint32_t milliseconds,
            uint32_t microseconds);
    void (*mpe_threadYield_ptr)(void);
    mpe_Error (*mpe_threadGetPrivateData_ptr)(mpe_ThreadId threadId,
            mpe_ThreadPrivateData **data);
    mpe_Error (*mpe_threadSuspend_ptr)(mpe_ThreadId threadId);
    mpe_Error (*mpe_threadResume_ptr)(mpe_ThreadId threadId);
    mpe_Error (*mpe_threadSetName_ptr)(mpe_ThreadId threadId, const char *name);

    mpe_Error (*mpe_timeGetMillis_ptr)(mpe_TimeMillis *ptime);
    mpe_Error (*mpe_timeGet_ptr)(mpe_Time *time);
    mpe_Error (*mpe_timeToDate_ptr)(mpe_Time time, mpe_TimeTm *local);
    mpe_TimeClock (*mpe_timeClock_ptr)(void);
    mpe_TimeClock (*mpe_timeSystemClock_ptr)(void);
    mpe_TimeClock (*mpe_timeClockTicks_ptr)(void);
    uint32_t (*mpe_timeClockToMillis_ptr)(mpe_TimeClock clock);
    mpe_Time (*mpe_timeClockToTime_ptr)(mpe_TimeClock clock);
    mpe_TimeClock (*mpe_timeMillisToClock_ptr)(uint32_t milliseconds);
    mpe_TimeClock (*mpe_timeTimeToClock_ptr)(mpe_Time time);
    mpe_Time (*mpe_timeTmToTime_ptr)(mpe_TimeTm *tm);

    int (*mpe_setJmp_ptr)(mpe_JmpBuf jmpBuf);
    void (*mpe_longJmp_ptr)(mpe_JmpBuf jmpBuf, int val);

    mpe_Error (*mpe_eventQueueNew_ptr)(mpe_EventQueue *queueId,
            const char *queueName);
    mpe_Error (*mpe_eventQueueDelete_ptr)(mpe_EventQueue queueId);
    mpe_Error (*mpe_eventQueueSend_ptr)(mpe_EventQueue queueId,
            mpe_Event eventId, void *optionalEventData1,
            void *optionalEventData2, uint32_t eventFlag);
    mpe_Error (*mpe_eventQueueNext_ptr)(mpe_EventQueue queueId,
            mpe_Event *eventId, void **optionalEventData1,
            void **optionalEventData2, uint32_t *eventFlag);
    mpe_Error (*mpe_eventQueueWaitNext_ptr)(mpe_EventQueue queueId,
            mpe_Event *eventId, void **optionalEventData1,
            void **optionalEventData2, uint32_t *eventFlag, uint32_t timeout);

    const char*(*mpe_envGet_ptr)(const char*);
    mpe_PowerStatus (*mpe_stbGetPowerStatus_ptr)(void);
    mpe_Error (*mpe_registerForPowerKey_ptr)(mpe_EventQueue queueId, void *act);
    mpe_Error (*mpe_stbBoot_ptr)(mpe_STBBootMode);
    mpe_Error (*mpe_stbGetAcOutletState_ptr)(mpe_Bool *state);
    mpe_Error (*mpe_stbSetAcOutletState_ptr)(mpe_Bool enable);
    mpe_Error (*mpe_stbGetRootCerts_ptr)(uint8_t **roots, uint32_t *len);

    // DSExt methods
    mpe_Error (*mpe_stbSetPowerStatus_ptr)(mpe_PowerStatus newPowerMode);
    mpe_Error (*mpe_stbSetSystemMuteKeyControl_ptr)(mpe_Bool enable);
    mpe_Error (*mpe_stbSetSystemVolumeKeyControl_ptr)(mpe_Bool enable);
    mpe_Error (*mpe_stbSetSystemVolumeRange_ptr)(uint32_t range);
    mpe_Error (*mpe_stbResetAllDefaults_ptr)(void);

    mpe_Error (*mpe_timeGetUTCTimeMillis_ptr)(
            mpe_TimeMillis * const pMpeTimeInMillis);

    mpe_AudioStatus (*mpe_stbGetAudioStatus_ptr)(void);
    mpe_Error (*mpe_stbSetAudioStatus_ptr)(mpe_AudioStatus newAudioMode);

} mpe_os_ftable_t;

#ifdef __cplusplus
}
;
#endif

#endif /* __OSMGR_H_ */
