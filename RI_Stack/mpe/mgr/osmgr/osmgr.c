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

/*
 * The MPE system manager implementation
 */

#include <osmgr.h>
#include "time_util.h"
#include <string.h>
#include <stdio.h>

static void mpe_osInit(void);
mpe_Error mpe_timeGetUTCTimeMillis(mpe_TimeMillis * const pMpeTimeInMillis);

static mpe_Cond firstSTTCond;
static mpe_Mutex STTMutex;
static mpe_Bool gotFirstSample = FALSE;
static double slope = 0.0;
static double intercept = 0.0;
static uint64_t previousSttSample = 0;
static uint64_t previousMpeSample = 0;

static mpe_Error mpe_dlmodOpen(const char* name, mpe_Dlmod *dlmodId);

extern char *filesys_canonicalPath(const char *rawPath);

mpe_os_ftable_t os_ftable =
{ mpe_osInit,

mpe_dlmodOpen, mpeos_dlmodClose, mpeos_dlmodGetSymbol,

mpeos_memAllocPGen, mpeos_memFreePGen, mpeos_memReallocPGen,
        mpeos_memGetFreeSize, mpeos_memGetLargestFree,

        mpeos_memAllocH, mpeos_memFreeH, mpeos_memReallocH, mpeos_memLockH,
        mpeos_memPurge, mpeos_memCompact,

        mpeos_memStats, mpeos_memRegisterMemFreeCallback,
        mpeos_memUnregisterMemFreeCallback,

        mpeos_memGetStats, mpeos_memAllocPProf, mpeos_memFreePProf,
        mpeos_memReallocPProf,

        mpeos_mutexNew, mpeos_mutexDelete, mpeos_mutexAcquire,
        mpeos_mutexAcquireTry, mpeos_mutexRelease,

        mpeos_condNew, mpeos_condDelete, mpeos_condGet, mpeos_condWaitFor,
        mpeos_condSet, mpeos_condUnset, mpeos_threadCreate,
        mpeos_threadDestroy, mpeos_threadAttach, mpeos_threadSetPriority,
        mpeos_threadGetStatus, mpeos_threadSetStatus, mpeos_threadGetData,
        mpeos_threadSetData, mpeos_threadGetCurrent, mpeos_threadSleep,
        mpeos_threadYield, mpeos_threadGetPrivateData, mpeos_threadSuspend,
        mpeos_threadResume, mpeos_threadSetName,

        mpeos_timeGetMillis, mpeos_timeGet, mpeos_timeToDate, mpeos_timeClock,
        mpeos_timeSystemClock, mpeos_timeClockTicks, mpeos_timeClockToMillis,
        mpeos_timeClockToTime, mpeos_timeMillisToClock, mpeos_timeTimeToClock,
        mpeos_timeTmToTime,

        mpeos_setJmp, mpeos_longJmp, mpeos_eventQueueNew,
        mpeos_eventQueueDelete, mpeos_eventQueueSend, mpeos_eventQueueNext,
        mpeos_eventQueueWaitNext,

        mpeos_envGet, mpeos_stbGetPowerStatus, mpeos_registerForPowerKey,
        mpeos_stbBoot, mpeos_stbGetAcOutletState, mpeos_stbSetAcOutletState,
        mpeos_stbGetRootCerts,

        /* DSExt */
        mpeos_stbSetPowerStatus, mpeos_stbSetSystemMuteKeyControl,
        mpeos_stbSetSystemVolumeKeyControl, mpeos_stbSetSystemVolumeRange,
        mpeos_stbResetAllDefaults,

        /* New time API */
        mpe_timeGetUTCTimeMillis,

        mpeos_stbGetAudioStatus, mpeos_stbSetAudioStatus
};

/**
 * <i>mpe_osSetup</i> calls the sys manager to install the debug manager function table
 */
void mpe_osSetup(void)
{
    mpe_sys_install_ftable(&os_ftable, MPE_MGR_TYPE_OS);
}

/**
 * <i>mpe_osInit</i> currently, no os manager initialization is required
 */
static void mpe_osInit(void)
{
    static mpe_Bool inited = false;
    extern void **mpe_ftable;
    extern void mpeos_dlmodInit(void**);

    if (!inited)
    {
        inited = true;
        /*
         * Call mpeos_dlmodInit to get the mpe_ftable pointer populated into
         * the MPEOS porting layer.
         */
        mpeos_dlmodInit(mpe_ftable);

        mpeos_condNew(FALSE, FALSE, &firstSTTCond);
        mpeos_mutexNew(&STTMutex);
    }
}

/**
 * Canonicalize filename before passing to MPEOS
 */
mpe_Error mpe_dlmodOpen(const char* name, mpe_Dlmod *dlmodId)
{
    char* newname = filesys_canonicalPath(name);
    mpe_Error err = mpeos_dlmodOpen(newname, dlmodId);
    mpeos_memFreeP(MPE_MEM_FILE, newname);
    return err;
}

#define SHOW_RAW_DATA
#define SHOW_LINE_CALC

void setSTTTime(const uint32_t sttTimeInSeconds,
        const mpe_TimeMillis mpeTimeInMillis)
{
    const uint64_t actualSttSample = (uint64_t) sttTimeInSeconds
            * (uint64_t) 1000;
    const uint64_t actualMpeSample = mpeTimeInMillis;

    if (gotFirstSample)
    {
        if ((previousSttSample < actualSttSample) && (previousMpeSample
                < actualMpeSample)) // All OK
        {
            const uint64_t prevSttEstimate = slope * actualMpeSample
                    + intercept;
            const uint64_t sttSampleDelta = (actualSttSample
                    - previousSttSample) * 2; // "smoothing factor"
            const uint64_t nextSttEstimate = actualSttSample + sttSampleDelta;
            const uint64_t nextMpeEstimate = actualMpeSample + sttSampleDelta;

            mpeos_mutexAcquire(STTMutex);
            // Calculate the new slope (rise / run)
            slope = ((double) ((int64_t) nextSttEstimate
                    - (int64_t) prevSttEstimate)
                    / (double) ((int64_t) nextMpeEstimate
                            - (int64_t) actualMpeSample));

            if (slope > 2.0)
                slope = 2.0;
            if (slope < 0.5)
                slope = 0.5;

            // Calculate the new intercept (b = y - mx)
            intercept = prevSttEstimate - slope * actualMpeSample;

            previousSttSample = actualSttSample;
            previousMpeSample = actualMpeSample;

            mpeos_mutexRelease(STTMutex);

#ifdef SHOW_RAW_DATA
            printf("%lld %lld %lld\n", actualMpeSample, actualSttSample,
                    prevSttEstimate);
#endif
#ifdef SHOW_LINE_CALC
            printf("UTC(mpe) = (%.12f) * mpe + (%f)\n", slope, intercept);
#endif
        }
        else if (previousMpeSample < actualMpeSample)
        {
            const uint64_t prevSttEstimate = slope * actualMpeSample
                    + intercept;

#ifdef SHOW_RAW_DATA
            printf("%lld %lld %lld\n", actualMpeSample, actualSttSample,
                    prevSttEstimate);
#endif
#ifdef SHOW_LINE_CALC
            printf("UTC(mpe) = (%.12f) * mpe + (%f) [STT thrown out]\n", slope,
                    intercept);
#endif
        }
        else
        {
#ifdef SHOW_RAW_DATA
            printf("%lld %lld              \n", actualMpeSample,
                    actualSttSample);
#endif
#ifdef SHOW_LINE_CALC
            printf(
                    "UTC(mpe) = (%.12f) * mpe + (%f) [Entire sample thrown out]\n",
                    slope, intercept);
#endif
        }
    }
    else // This is the very first sample
    {
        mpeos_mutexAcquire(STTMutex);
        // Start with the ideal slope of "one second per second"
        slope = 1.0;

        // Start with the ideal intercept based on the ideal slope (b = y - mx)
        intercept = actualSttSample - slope * actualMpeSample;

        // Set our condition var to let mpe_timeGetUTCTimeMillis requests go through
        mpeos_condSet(firstSTTCond);
        gotFirstSample = TRUE;

        previousSttSample = actualSttSample;
        previousMpeSample = actualMpeSample;

        mpeos_mutexRelease(STTMutex);

#ifdef SHOW_RAW_DATA
        printf("%lld %lld %lld\n", actualMpeSample, actualSttSample,
                actualSttSample);
#endif
#ifdef SHOW_LINE_CALC
        printf("UTC(mpe) = (%.12f) * mpe + (%f)\n", slope, intercept);
#endif
    }
}

mpe_Error mpe_timeGetUTCTimeMillis(mpe_TimeMillis * const pMpeTimeInMillis)
{
    const char * sttEnabled = mpeos_envGet("SITP.SI.STT.ENABLED");

    mpe_Error mpeError = mpeos_timeGetMillis(pMpeTimeInMillis);

    if (mpeError == MPE_SUCCESS)
    {
        if ((sttEnabled != NULL) && (stricmp(sttEnabled, "TRUE") == 0))
        {
            if (!gotFirstSample)
            {
                // Wait for our first STT to arrive
                mpeos_condGet(firstSTTCond);
            }

            mpeos_mutexAcquire(STTMutex);
            *pMpeTimeInMillis = slope * (*pMpeTimeInMillis) + intercept;
            mpeos_mutexRelease(STTMutex);
        }
    }

    return mpeError;
}
