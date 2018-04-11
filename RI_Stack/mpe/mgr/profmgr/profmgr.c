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

// profmgr.c module
// used for performance analysis of mpe (or any C code)
//

#include <mpe_types.h>
#include <mpe_file.h>
#include <mpe_dbg.h>
#include <mpe_error.h>
#include <mpeos_mem.h>
#include <sysmgr.h>
#include <mpe_prof.h>
#include <mgrdef.h>
#include <profmgr.h>
#include <mpeos_util.h>

#ifdef MPE_FEATURE_PROF
#define PROF_START_COUNT (3)

// in debug mode
#define PROF_MSG_OUT(msg) MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_PROD, msg)

enum
{
    eWhereStackDepth = 1024
};

// data on each activity (startTiming)
typedef struct timeeTable
{
    char timeestring[512]; // timee being timed
    unsigned char timeStamp[8]; // encoded time stamp
    uint32_t totaltimeeTime; // total time that timee took
    uint32_t totaltimeeTimeCPU; // total time that timee took in CPU time
    char *comment; // one comment ptr (additional info added during timing, each call is an append!)
}timeeTable;

// data on each "where" (SetWhere)
typedef struct timingTable
{
    char section[MAX_CHARS_FOR_CODE_SECT]; // str describing code section (descending sort by time)
    uint32_t milliseconds; // how many milliseconds in this code for this timee
    uint32_t millisecondsCPU; // how many milliseconds of CPU in this code for this timee
    uint32_t count; // how many times this "where" called
}timingTable;

// array of each "where"s timings (one timing per activity)
typedef struct sectTimingData
{
    timingTable t[MAX_PROF_CNT]; // each code section in milliseconds indexed by timee
}sectTimingData;

typedef struct Profiling_PROF_Data
{
    uint32_t startingIndex; // first timingTable index used (0 if none)
    uint32_t endingIndex; // last timingTable index (= startingIndex to startingIndex + 29)
    timeeTable ptimee_Table[MAX_PROF_CNT]; // the timees and when they were timed
    sectTimingData pTimingTable[1]; // timing for each code section (variable length!)
}Profiling_PROF_Data;

typedef struct
{
    uint32_t fTime; // clock time
    uint32_t fTimeCPU; // thread time
    uint32_t fTimeCount; // count of time SetWhere called
    const char *fWhere; // ptr to where description
}TimeEntry;

enum
{
    eClockTime = 0,
    eCPUTime = 1
};

enum
{
    eSliceTime = 0,
    eStopTime = 1,
    eTotalTimeWhenSliced = 2
};

// NOTE: keep each string
// under MAX_CHARS_FOR_CODE_SECT characters long
// count of strings in gWhereCount
static char **gWhereStrings = NULL;
static int32_t gWhereCount = 1; // always 1 string...

typedef struct TimingEntry
{
    mpe_TimeClock dTiming[2];
    mpe_TimeClock dTimingTotal[2];
    uint32_t dTimeCount;
}TimingEntry;

typedef struct TimeData
{
    mpe_TimeClock dStartTime; // starting time for timee
    mpe_TimeClock dStartTimeTotal; // real starting time for timee when sliced
    mpe_TimeClock dEndTime; // ending time for timee
    mpe_TimeClock dStartTimeCPU; // starting time for timee
    mpe_TimeClock dStartTimeCPUTotal; // real starting time for timee when sliced
    mpe_TimeClock dEndTimeCPU; // ending time for timee
    mpe_ThreadId dStartThread; // starting thread
    mpe_ThreadId dEndThread; // ending thread
    char *dtimee; // ptr to string of timee we are timing
    mpe_TimeClock dWhen; // set during timing transition (curtime - gWhen = timing)
    mpe_ThreadId dSetThread; // starting thread
    mpe_TimeClock dWhenCPU; // set during timing transition (curtime - gWhen = timing)
    mpe_ThreadId dPopThread; // ending thread
    uint32_t dWhereStack[eWhereStackDepth];
    uint32_t dStackDepth;
    uint32_t dMaxStackDepth;
    int32_t dtimeeTime; // current number of seconds since started timee
    char *comment; // comment for this activity
    TimingEntry t[1]; // actually variable length, but always at least 1 entry
}TimeData;

static uint32_t gSliceTime = 0; // set non-zero if timing every n seconds
static mpe_TimeClock gNextSlot; // used when timing every n seconds
static mpe_TimeClock gSlotBump; // amount of time corresponding to gSliceTime seconds
static TimeData **pTime = NULL; // pointer to array of ptrs to time data (pTime[])
static uint32_t timeeCount = 0; // count of number of TimeData elements in above array (some may be empty)
static mpe_Bool gTimingStarted = FALSE;
static mpe_Bool gTimingInited = FALSE;
mpe_Bool gProfiling_Inited = false; // set true by init, false by destroy
Profiling_PROF_Data* gPROFTiming = NULL;

static mpe_Error Profiling_Init(void);
static mpe_TimeClock x_mSecs;

static uint32_t x_TicksToMS(mpe_TimeClock theTime)
{
    return (uint32_t) ((theTime + (x_mSecs >> 1)) / x_mSecs);
}

static void x_SplitTime(uint32_t milliSeconds, uint32_t *wholeSeconds, uint32_t *fractionalSeconds)
{
    *wholeSeconds = milliSeconds / 1000;
    *fractionalSeconds = milliSeconds - (*wholeSeconds * 1000);
}

static int x_TimeCompare(const void *a, const void *b)
{
    return (int) (((TimeEntry *) b)->fTime) - (int) (((TimeEntry *) a)->fTime);
}

// this formats it prior to the profiling going to the output
static void TimingToPROF(uint32_t outputState, uint32_t timeDataIndex)
{
    uint32_t count;
    TimeEntry *sortedList;
    uint32_t timeeIndex;
    uint32_t timeeLen;
    mpe_TimeTm date;
    mpe_TimeTm *t = &date;
    mpe_Time Time;
    TimeData *pTimeData;
    uint32_t maxStrLen = MAX_CHARS_FOR_CODE_SECT - 1;
    struct PROF_time
    {
        uint8_t hi_year;
        uint8_t lo_year;
        uint8_t month;
        uint8_t day;
        uint8_t hour;
        uint8_t minutes;
        uint8_t seconds;
        uint8_t deciseconds;
    }profTime;

    if (!gProfiling_Inited) // should never happen
    return;
    pTimeData = pTime[timeDataIndex];

    // these currently have a "range" which marches along (for SNMP compatibility)
    // when MAX_PROF_CNT entries are reached, the range goes to 2 to MAX_PROF_CNT+1,
    // then 3 to MAX_PROF_CNT+2, etc
    if (++gPROFTiming->endingIndex > gPROFTiming->startingIndex + MAX_PROF_CNT - 1)
    gPROFTiming->startingIndex = gPROFTiming->endingIndex - (MAX_PROF_CNT - 1);
    if (gPROFTiming->endingIndex == 1)
    gPROFTiming->startingIndex = 1;

    // go from PROF index to index in array in memory, endingIndex = next place to put timing
    timeeIndex = (gPROFTiming->endingIndex - 1) % MAX_PROF_CNT;
    timeeLen = strlen(pTimeData->dtimee);

    if (timeeLen > 511) // get last 511 chars of timee
    strcpy(gPROFTiming->ptimee_Table[timeeIndex].timeestring, pTimeData->dtimee + (timeeLen - 511));
    else
    strcpy(gPROFTiming->ptimee_Table[timeeIndex].timeestring, pTimeData->dtimee);

    gPROFTiming->ptimee_Table[timeeIndex].totaltimeeTime = x_TicksToMS(pTimeData->dEndTime - pTimeData->dStartTime);
    if (pTimeData->dStartThread == pTimeData->dEndThread)
    {
        gPROFTiming->ptimee_Table[timeeIndex].totaltimeeTimeCPU
        = x_TicksToMS(pTimeData->dEndTimeCPU - pTimeData->dStartTimeCPU);
    }
    else
    {
        gPROFTiming->ptimee_Table[timeeIndex].totaltimeeTimeCPU = 0;
    }

    mpeos_timeGet(&Time);
    mpeos_timeToDate(Time, t); // local time
    profTime.hi_year = (t->tm_year + 1900) >> 8; // assure hi-endian
    profTime.lo_year = (t->tm_year + 1900) & 0x00FF;
    profTime.month = t->tm_mon + 1;
    profTime.day = t->tm_mday;
    profTime.hour = t->tm_hour;
    profTime.minutes = t->tm_min;
    profTime.seconds = t->tm_sec;
    profTime.deciseconds = 0; // sorry, we don't have this available
    memcpy(gPROFTiming->ptimee_Table[timeeIndex].timeStamp, &profTime, 8);

    gPROFTiming->ptimee_Table[timeeIndex].comment = pTimeData->comment;
    pTimeData->comment = 0; // so doesn't free

    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, gWhereCount * sizeof(TimeEntry), (void**)&sortedList))
    {
        PROF_MSG_OUT("DisplayTiming - Allocation of sorted list failed\n");
        return;
    }

    for (count = 0; count < gWhereCount; ++count)
    {
        TimingEntry *pT = &pTimeData->t[count];
        TimeEntry *sort = &sortedList[count];
        sort->fTime = x_TicksToMS(pT->dTiming[eClockTime]);
        sort->fTimeCPU = x_TicksToMS(pT->dTiming[eCPUTime]);
        sort->fTimeCount = pT->dTimeCount;
        sort->fWhere = gWhereStrings[count];
        if (gSliceTime)
        {
            pT->dTimingTotal[eClockTime] += pT->dTiming[eClockTime];
            pT->dTimingTotal[eCPUTime] += pT->dTiming[eCPUTime];
        }
    }

    qsort((void *) sortedList, gWhereCount, sizeof(TimeEntry), x_TimeCompare);

    for (count = 0; count < gWhereCount; ++count)
    {
        TimeEntry *sort = &sortedList[count];
        if (sortedList[count].fTime || sortedList[count].fTimeCount) // anything worth noting

        {
            gPROFTiming->pTimingTable[count].t[timeeIndex].milliseconds = sort->fTime;
            gPROFTiming->pTimingTable[count].t[timeeIndex].millisecondsCPU = sort->fTimeCPU;
            gPROFTiming->pTimingTable[count].t[timeeIndex].count = sort->fTimeCount;
        }
        else // no time spent here...

        {
            gPROFTiming->pTimingTable[count].t[timeeIndex].milliseconds = 0;
            gPROFTiming->pTimingTable[count].t[timeeIndex].millisecondsCPU = 0;
            gPROFTiming->pTimingTable[count].t[timeeIndex].count = 0;
        }
        // no gWhereStrings string should be over MAX_CHARS_FOR_CODE_SECT characters long!
        // but if the coder makes a mistake and does this, truncate it for them to avoid a crash
        if (strlen(sortedList[count].fWhere) > maxStrLen)
        {
            strncpy(gPROFTiming->pTimingTable[count].t[timeeIndex].section,sort->fWhere, maxStrLen);
            gPROFTiming->pTimingTable[count].t[timeeIndex].section[maxStrLen] = 0;
        }
        else
        {
            strcpy(gPROFTiming->pTimingTable[count].t[timeeIndex].section,sort->fWhere);
        }
    }

    mpeos_memFreeP(MPE_MEM_UTIL, sortedList);

    if (gSliceTime)
    {
        char colonTime[20];
        switch (outputState)
        {
            case eSliceTime:
            pTimeData->dtimeeTime += gSliceTime; // slices of time
            sprintf(colonTime,":%d", (int)pTimeData->dtimeeTime);
            break;
            case eStopTime:
            pTimeData->dtimeeTime += (uint32_t)((pTimeData->dEndTime - pTimeData->dStartTime)/ mpeos_timeClockTicks());
            sprintf(colonTime,":%d", (int)pTimeData->dtimeeTime);
            break;
            case eTotalTimeWhenSliced:
            strcpy(colonTime,":total"); // null string
            break;
        }

        if (strlen(gPROFTiming->ptimee_Table[timeeIndex].timeestring) < 504) // avoid buffer overflow
        strcat(gPROFTiming->ptimee_Table[timeeIndex].timeestring, colonTime);

        // reset all timings
        for (count = 0; count < gWhereCount; ++count)
        {
            pTimeData->t[count].dTiming[eClockTime] = 0;
            pTimeData->t[count].dTiming[eCPUTime] = 0;
        }
        pTimeData->dStartTime = mpeos_timeSystemClock();
        mpeos_threadGetCurrent(&pTimeData->dStartThread);
        pTimeData->dStartTimeCPU = mpeos_timeClock();
    }

}

static void mpe_profmgrInit(void)
{
    char topLevelStr[11] = "Time Entry"; // strlen is 10 + 1 for zero byte
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, sizeof(char*), (void**)&gWhereStrings))
    {
        PROF_MSG_OUT("mpe_profmgrInit - Allocation of label ptrs failed\n");
        return;
    }
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, 11, (void**)&gWhereStrings[0]))
    {
        PROF_MSG_OUT("mpe_profmgrInit - Allocation of label 0 failed\n");
        return;
    }
    strcpy(gWhereStrings[0], topLevelStr);
    gWhereCount = 1;
}

static mpe_Error InitTiming(void)
{
    timeeCount = 0; // make sure in case fail (since used as flag later)
    // PROF_START_COUNT active at once to start
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, PROF_START_COUNT * sizeof(TimeData *), (void**)&pTime))
    {
        PROF_MSG_OUT("InitTiming - Allocation of time data pointers failed\n");
        pTime = 0; // make sure
        return MPE_ENOMEM;
    }
    memset(pTime, 0, PROF_START_COUNT * sizeof(TimeData *));
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, sizeof(TimeData), (void**)&pTime[0]))
    {
        PROF_MSG_OUT("InitTiming - Allocation of time data 0 failed\n");
        mpeos_memFreeP(MPE_MEM_UTIL, pTime);
        pTime = 0;
        return MPE_ENOMEM;
    }
    memset(pTime[0], 0, sizeof(TimeData));
    timeeCount = PROF_START_COUNT;
    gTimingInited = TRUE;
    return MPE_SUCCESS;
}

// returns timeeindex
static mpe_Error mpe_profmgrStartTiming(const char *timee, mpe_Bool timeeTiming, uint32_t* timeeIndex)
{
    TimeData *pTimeData = NULL;
    uint32_t rettimeeIndex = 0;

    if (timee == NULL || timeeIndex == NULL)
    {
        return MPE_EINVAL;
    }
    *timeeIndex = 0;

    if (gTimingStarted && timeeTiming)
    {
        return MPE_SUCCESS;
    }
    gTimingStarted = TRUE;

    if (timeeCount == 0)
    {
        if (InitTiming() != MPE_SUCCESS)
        return MPE_ENOMEM;
    }

    if (timeeTiming)
    {
        pTimeData = pTime[0];
    }
    else // use existing table

    {
        uint32_t i;
        int32_t timeDataSize = sizeof(TimeData) + gWhereCount * sizeof(TimingEntry);

        for (i = 0; i < timeeCount; i++) // find a hole and fill it

        {
            if (!pTime[i])
            {
                if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, timeDataSize, (void**)&pTimeData))
                {
                    PROF_MSG_OUT("StartTiming - Allocation of time data failed\n");
                    return MPE_ENOMEM;
                }
                memset(pTimeData, 0, timeDataSize);
                pTime[i] = pTimeData;
                rettimeeIndex = i;
                break;
            }
        }
        if (!pTimeData) // must expand table since no holes

        {
            Mem_Pointer newPtr;
            timeeCount += 3; // add 3 consecutive entries
            newPtr = (Mem_Pointer)pTime;
            if (MPE_SUCCESS != mpeos_memReallocP(MPE_MEM_UTIL, timeeCount * sizeof(TimeData *), (void**)&newPtr))
            {
                PROF_MSG_OUT("StartTiming - reAllocation of time data failed\n");
                return MPE_ENOMEM;
            }
            pTime = (TimeData **)newPtr;
            pTime[timeeCount - 1] = pTime[timeeCount - 2] = 0;
            rettimeeIndex = timeeCount - 3;
            if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, timeDataSize, (void**)&pTimeData))
            {
                PROF_MSG_OUT("StartTiming - reAllocation of time entry failed\n");
                return MPE_ENOMEM;
            }
            memset(pTimeData, 0, timeDataSize);
            pTime[rettimeeIndex] = pTimeData;
        }
    }

    // assumes there is something on stack already!
    // so give it highest level of timing an animation
    pTimeData->dWhereStack[0] = 0; // timing process is at zero
    pTimeData->dStackDepth = 1;
    if (pTimeData->comment)
    {
        mpeos_memFreeP(MPE_MEM_UTIL, pTimeData->comment);
    }

    if (!gProfiling_Inited)
    {
        Profiling_Init();
        if (!gProfiling_Inited)
        return MPE_ENOMEM;
    }
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, strlen(timee) + 1, (void**)&pTimeData->dtimee))
    {
        PROF_MSG_OUT("StartTiming - Allocation of timee string failed");
        return MPE_ENOMEM; // most likely reason, out of memory
    }
    strcpy(pTimeData->dtimee, timee);

    memset(&pTimeData->t[0], 0, gWhereCount * sizeof(TimingEntry));

    pTimeData->dStartTime = mpeos_timeSystemClock();
    mpeos_threadGetCurrent(&pTimeData->dStartThread);
    pTimeData->dStartTimeCPU = mpeos_timeClock();
    pTimeData->dWhen = pTimeData->dStartTime;
    pTimeData->dWhenCPU = pTimeData->dStartTimeCPU;
    pTimeData->dSetThread = pTimeData->dStartThread;
    pTimeData->comment = 0;
    if (gSliceTime)
    {
        gNextSlot = pTimeData->dStartTime + gSlotBump;
        pTimeData->dtimeeTime = 0;
        pTimeData->dStartTimeTotal = pTimeData->dStartTime;
        pTimeData->dStartTimeCPUTotal = pTimeData->dStartTimeCPU;
    }
    *timeeIndex = rettimeeIndex;
    return MPE_SUCCESS;
}

static mpe_Error mpe_profmgrStopTiming(uint32_t timeeIndex, mpe_Bool timeeTiming)
{
    uint32_t seconds, count;
    uint32_t fractionalSeconds;
    TimeData *pTimeData = pTime[timeeIndex];

    if (timeeCount == 0 || !gProfiling_Inited)
    {
        return MPE_SUCCESS;
    }
    if (!gTimingStarted && timeeTiming)
    {
        return MPE_SUCCESS;
    }
    gTimingStarted = FALSE;

    pTimeData->dEndTime = mpeos_timeSystemClock();
    mpeos_threadGetCurrent(&pTimeData->dEndThread);
    pTimeData->dPopThread = pTimeData->dEndThread;
    pTimeData->dEndTimeCPU = mpeos_timeClock();

    pTimeData->t[pTimeData->dWhereStack[pTimeData->dStackDepth - 1]].dTiming[eClockTime]
    += pTimeData->dEndTime - pTimeData->dWhen;
    if (pTimeData->dEndThread == pTimeData->dPopThread)
    {
        pTimeData->t[pTimeData->dWhereStack[pTimeData->dStackDepth - 1]].dTiming[eCPUTime]
        += pTimeData->dEndTimeCPU - pTimeData->dWhenCPU;
    }

    TimingToPROF(eStopTime, timeeIndex); // put timing out to structs in memory
    if (gSliceTime && timeeIndex == 0)
    {
        // set times to total times
        pTimeData->dStartTime = pTimeData->dStartTimeTotal;
        pTimeData->dStartTimeCPU = pTimeData->dStartTimeCPUTotal;
        for (count = 0; count < gWhereCount; count++)
        {
            pTimeData->t[count].dTiming[eClockTime] = pTimeData->t[count].dTimingTotal[eClockTime];
            pTimeData->t[count].dTiming[eCPUTime] = pTimeData->t[count].dTimingTotal[eCPUTime];
        }
        TimingToPROF(eTotalTimeWhenSliced, timeeIndex); // put total timing out to structs in memory
    }

    x_SplitTime(x_TicksToMS(pTimeData->dEndTime - pTimeData->dStartTime), &seconds, &fractionalSeconds);

    if (pTimeData->dtimee)
    {
        mpeos_memFreeP(MPE_MEM_UTIL, pTimeData->dtimee);
    }
    if (!timeeTiming) // leave the 0th ptr around for timeeTiming

    {
        mpeos_memFreeP(MPE_MEM_UTIL, pTime[timeeIndex]); // delete the time data for this run
        pTime[timeeIndex] = 0; // create a hole
    }
}

static void x_SetTime(TimeData *pTimeData, int32_t where, mpe_Bool isSetWhere)
{
    mpe_TimeClock now;
    mpe_TimeClock nowCPU;

    now = mpeos_timeSystemClock();
    if (isSetWhere)
    {
        mpeos_threadGetCurrent(&pTimeData->dSetThread);
        nowCPU = mpeos_timeClock();
        if (pTimeData->dSetThread == pTimeData->dPopThread)
        pTimeData->t[where].dTiming[eCPUTime] += nowCPU - pTimeData->dWhenCPU;
    }
    else // is a popWhere

    {
        mpeos_threadGetCurrent(&pTimeData->dPopThread);
        nowCPU = mpeos_timeClock();
        if (pTimeData->dSetThread == pTimeData->dPopThread)
        pTimeData->t[where].dTiming[eCPUTime] += nowCPU - pTimeData->dWhenCPU;
        ++pTimeData->t[where].dTimeCount;
    }

    pTimeData->t[where].dTiming[eClockTime] += now - pTimeData->dWhen;
    pTimeData->dWhen = now;
    pTimeData->dWhenCPU = nowCPU;
}

static mpe_Error mpe_profmgrSetWhere(int32_t timeeIndex, int32_t where)
{
    TimeData *pTimeData = pTime[timeeIndex];

    if (!gTimingStarted)
    {
        return MPE_SUCCESS; // must be inside a StartTime/StopTime pair!
    }
    if (where < 1 || where > gWhereCount)
    {
        return MPE_EINVAL;
    }

    // add to time for level above this when start this level
    // time is not cumulative (time on each level doesn't include sublevels)
    // sprintf(buffer,"calling x_SetTime, timeeIndex = %d, pTimeData = %x\n", timeeIndex, (uint32_t)pTimeData);
    x_SetTime(pTimeData, pTimeData->dWhereStack[pTimeData->dStackDepth - 1], true);
    pTimeData->dWhereStack[pTimeData->dStackDepth++] = where;

    if (pTimeData->dStackDepth > pTimeData->dMaxStackDepth)
    {
        pTimeData->dMaxStackDepth = pTimeData->dStackDepth;
    }
    return MPE_SUCCESS;
}

static mpe_Error mpe_profmgrPopWhereStack(int32_t timeeIndex)
{
    TimeData *pTimeData;

    if (!gTimingStarted)
    {
        return MPE_SUCCESS; // must be inside a StartTime/StopTime pair!
    }
    pTimeData = pTime[timeeIndex];
    --pTimeData->dStackDepth; // stack depth is a count, after decrement can use as index

    x_SetTime(pTimeData, pTimeData->dWhereStack[pTimeData->dStackDepth], false); // add to time for this level (and start timer for higher level)
    if (gSliceTime && timeeIndex == 0)
    {
        if (pTimeData->dWhen >= gNextSlot)
        {
            pTimeData->dEndTime = mpeos_timeSystemClock();
            mpeos_threadGetCurrent(&pTimeData->dEndThread);
            pTimeData->dEndTimeCPU = mpeos_timeClock();
            TimingToPROF(eSliceTime, 0); // put timing out to PROF
            gNextSlot += gSlotBump;
            pTimeData->dWhen = mpeos_timeSystemClock(); // ignore TimingToPROF delay
            mpeos_threadGetCurrent(&pTimeData->dStartThread);
            pTimeData->dSetThread = pTimeData->dStartThread;
            pTimeData->dWhenCPU = mpeos_timeClock();
        }
    }
    return MPE_SUCCESS;
}

static mpe_Error mpe_profmgrGetIndex(const char *labelStr, uint32_t *labelIndex)
{
    uint32_t iLabel;

    if (labelStr == NULL || labelIndex == NULL)
    {
        return MPE_EINVAL;
    }

    for (iLabel = 1; iLabel < gWhereCount; iLabel++)
    {
        if (!strcmp(gWhereStrings[iLabel], labelStr)) // must be case-sensitive match

        {
            // matched
            *labelIndex = iLabel;
            return MPE_SUCCESS;
        }
    }
    *labelIndex = 0; // never found (label numbers start at 1)
    return MPE_ENODATA; // no data found that matched
}

static mpe_Error mpe_profmgrAddLabel(const char *labelStr, uint32_t* labelIndex)
{
    uint32_t i;
    int32_t bumpCount = gWhereCount + 1;
    uint32_t sizeOfWhereStringPtrs = bumpCount * sizeof(char*);
    int32_t timeDataSize = sizeof(TimeData) + bumpCount * sizeof(TimingEntry);
    int32_t profilingDataSize = sizeof(Profiling_PROF_Data) + bumpCount * sizeof(sectTimingData);

    if (labelStr == NULL || labelIndex == NULL)
    {
        return MPE_EINVAL;
    }

    // check for label already exists
    if (MPE_SUCCESS == mpe_profmgrGetIndex(labelStr, labelIndex))
    {
        // label already exists, just give it back
        return MPE_SUCCESS;
    }

    *labelIndex = 0; // done already by mpe_profmgrGetIndex, but let's make sure

    if (timeeCount == 0)
    {
        if (InitTiming() != MPE_SUCCESS)
        return MPE_ENOMEM;
    }

    if (!gProfiling_Inited)
    {
        Profiling_Init();
        if (!gProfiling_Inited)
        return MPE_ENOMEM;
    }

    // add label to array
    // update array of ptrs (add 1)
    if (gWhereStrings)
    {
        if (MPE_SUCCESS != mpeos_memReallocP(MPE_MEM_UTIL, sizeOfWhereStringPtrs, (void**)&gWhereStrings))
        {
            PROF_MSG_OUT("mpe_profmgrAddLabel - Allocation of label ptrs failed\n");
            return MPE_ENOMEM;
        }
    }
    else
    {
        if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, sizeOfWhereStringPtrs, (void**)&gWhereStrings))
        {
            PROF_MSG_OUT("mpe_profmgrAddLabel - Allocation of label ptrs failed\n");
            return MPE_ENOMEM;
        }
    }

    // add new string to array
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, strlen(labelStr) + 1, (void**)&gWhereStrings[gWhereCount]))
    {
        PROF_MSG_OUT("mpe_profmgrAddLabel - Allocation of label failed\n");
        return MPE_ENOMEM;
    }

    strcpy(gWhereStrings[gWhereCount], labelStr);

    // update all timing tables
    for (i = 0; i < timeeCount; i++) // find a table and update it

    {
        if (pTime[i])
        {
            if (MPE_SUCCESS != mpeos_memReallocP(MPE_MEM_UTIL, timeDataSize, (void**)&pTime[i]))
            {
                PROF_MSG_OUT("mpe_profmgrAddLabel - ReAllocation of time data failed\n");
                return MPE_ENOMEM;
            }
            memset(&pTime[i]->t[gWhereCount], 0, sizeof(TimingEntry));
        }
    }

    // update table in memory (must exist due to init above)
    if (MPE_SUCCESS != mpeos_memReallocP(MPE_MEM_UTIL, profilingDataSize, (void**)&gPROFTiming))
    {
        PROF_MSG_OUT("WARNING!! WARNING!! NOT ENOUGH MEMORY FOR PROFILING TABLE\n");
        return MPE_ENOMEM; // most likely reason, out of memory
    }
    memset(&gPROFTiming->pTimingTable[gWhereCount], 0, sizeof(sectTimingData));
    *labelIndex = gWhereCount++; // everything OK, bump count of labels
    return MPE_SUCCESS;
}

static mpe_Error mpe_profmgrAddComment(const char *labelStr)
{

    TimeData *pTimeData; // only add to first for now
    int32_t labelLen;

    if (!gTimingStarted)
    {
        return MPE_ECOND; // must be inside a StartTime/StopTime pair!
    }
    if (labelStr == NULL)
    {
        return MPE_EINVAL;
    }
    pTimeData = pTime[0];
    labelLen = strlen(labelStr);

    // append string to comment
    if (pTimeData->comment)
    {
        int32_t comLen = strlen(pTimeData->comment);
        char *pCom; // pts to zero terminator
        if (labelLen + comLen + 9 > 255) // includes "comment: "

        {
            return MPE_SUCCESS; // just don't do it!
        }
        if (MPE_SUCCESS != mpeos_memReallocP(MPE_MEM_UTIL,
                        // + 2 since includes separator, terminator
                        comLen + labelLen + 2, (void**)&pTimeData->comment))
        {
            PROF_MSG_OUT("mpe_profmgrAddComment - reallocation of label failed\n");
            return MPE_ENOMEM;
        }
        // since time critical (happens during timing), do it this way rather than strcats
        pCom = &pTimeData->comment[comLen]; // pts to zero terminator
        *pCom++ = ';'; // separator
        strncpy(pCom, labelStr, labelLen + 1); // writes after ";"
    }
    else
    {
        if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, labelLen + 2, (void**)&pTimeData->comment))
        {
            PROF_MSG_OUT("mpe_profmgrAddComment - allocation of label failed\n");
            return MPE_ENOMEM;
        }
        strncpy(pTimeData->comment, labelStr, labelLen + 1);
    }

    return MPE_SUCCESS;
}

// outputs all timing data to serial (debug) or UDP to headend (production,
// provided that is the current OS output method)
// if numPrints == 0, do all prints, otherwise do last numPrints prints
static mpe_Error mpe_profmgrDisplayTiming(uint32_t numPrints)
{
    uint32_t cntTimings;
    uint32_t count;
    uint32_t seconds, secondsCPU;
    uint32_t fractionalSeconds, fractionalSecondsCPU;
    timeeTable *ptimeeTable;
    uint32_t timeeIndexStart = (gPROFTiming->startingIndex - 1) % MAX_PROF_CNT;
    uint32_t timeeIndexEnd = (gPROFTiming->endingIndex - 1) % MAX_PROF_CNT;
    uint32_t timeeCnt = gPROFTiming->endingIndex - gPROFTiming->startingIndex + 1;
    struct PROF_time
    {
        uint8_t hi_year;
        uint8_t lo_year;
        uint8_t month;
        uint8_t day;
        uint8_t hour;
        uint8_t minutes;
        uint8_t seconds;
        uint8_t deciseconds;
    }profTime;
    char buffer[512];
    int32_t indexToUse = timeeIndexStart;
#if defined(qDebug) && qDebug
    char leadingComma[2] = "";
#else
    char leadingComma[2] = ",";
#endif

    if (numPrints) // if not zero, must fix to last numPrints

    {
        if (numPrints > timeeCnt) // limit to amount on hand

        {
            numPrints = timeeCnt;
        }
        indexToUse = timeeIndexEnd - numPrints + 1; // back up from last index
        if (indexToUse < 0) // could wrap around

        {
            indexToUse = timeeIndexStart + (timeeCnt - numPrints); // then go forward from start
        }
        if (indexToUse >= MAX_PROF_CNT) // could wrap around forward

        {
            indexToUse -= MAX_PROF_CNT; // bring back within range
        }
        timeeCnt = numPrints; // we are doing how many they told us (or as many as we have)
    }

    // for each startTime/stopTime
    // in .cvs format for Excel
    for (cntTimings = 0; cntTimings < timeeCnt; ++cntTimings)
    {
        ptimeeTable = &gPROFTiming->ptimee_Table[indexToUse];
        sprintf(buffer,"%sTiming for:, %s\n", leadingComma, ptimeeTable->timeestring);
        PROF_MSG_OUT(buffer);
        if (ptimeeTable->comment)
        {
            sprintf(buffer,"%scomment:, %s\n", leadingComma, ptimeeTable->comment);
            PROF_MSG_OUT(buffer);
        }
        memcpy(&profTime, ptimeeTable->timeStamp, 8); // in formatting mode
        sprintf(buffer,"%sDate and Time:, %d/%d/%d, %d:%02d:%02d \n",
                leadingComma, profTime.month, profTime.day, (profTime.hi_year << 8) + profTime.lo_year,
                profTime.hour, profTime.minutes, profTime.seconds);
        PROF_MSG_OUT(buffer);
        x_SplitTime(ptimeeTable->totaltimeeTime, &seconds, &fractionalSeconds);
        x_SplitTime(ptimeeTable->totaltimeeTimeCPU, &secondsCPU, &fractionalSecondsCPU);
        sprintf(buffer,"%sTotal Clock and CPU Time (in seconds):, %3d.%03d, %3d.%03d\n",
                leadingComma, (int)seconds, (int)fractionalSeconds, (int)secondsCPU, (int)fractionalSecondsCPU);
        PROF_MSG_OUT(buffer);
#if defined(qDebug) && qDebug
        PROF_MSG_OUT("Action, Time(in Seconds), CPU Time(in Seconds), invoked\n");
#else
        PROF_MSG_OUT(",Action, Time(in Seconds), CPU Time(in Seconds), invoked\n");
#endif
        for (count = 0; count < gWhereCount; ++count)
        {
            timingTable *pTimeData = &gPROFTiming->pTimingTable[count].t[indexToUse];
            if (pTimeData->section[0]) // anything in section (should always be)

            {
                x_SplitTime(pTimeData->milliseconds, &seconds, &fractionalSeconds);
                x_SplitTime(pTimeData->millisecondsCPU, &secondsCPU, &fractionalSecondsCPU);
                sprintf(buffer,"%s%s, %3d.%03d, %3d.%03d, %d\n",
                        leadingComma, pTimeData->section,
                        (int)seconds, (int)fractionalSeconds, (int)secondsCPU, (int)fractionalSecondsCPU,
                        (int)pTimeData->count);
                PROF_MSG_OUT(buffer);
            }
        }
        PROF_MSG_OUT("\n");
        if (++indexToUse >= MAX_PROF_CNT)
        {
            indexToUse = 0; // wrap around
        }
    }
    return MPE_SUCCESS;
}

// returns kSnmp_NoErr if succeeds
// since this is only for debugging, not a serious problem
static mpe_Error Profiling_Init(void)
{
    // initialize the tables in memory
    // allocate a buffer
    if (MPE_SUCCESS != mpeos_memAllocP(MPE_MEM_UTIL, sizeof(Profiling_PROF_Data), (void**)&gPROFTiming))
    {
        PROF_MSG_OUT("WARNING!! WARNING!! NOT ENOUGH MEMORY FOR PROFILING TABLE\n");
        return MPE_ENOMEM; // most likely reason, out of memory
    }
    memset(gPROFTiming,0,sizeof(Profiling_PROF_Data));

    // for completeness, so memory structure mirrors PROF structure
    gPROFTiming->startingIndex = 0;
    gPROFTiming->endingIndex = 0;

    // for now, no slice and dice
    gSliceTime = 0; //gHTML_Opt.iProfileSlice; // only do this once per session
    if (gSliceTime)
    {
        if (gSliceTime < 0 || gSliceTime >= 120) // sanity check
        gSliceTime = 0;
        else
        gSlotBump = mpeos_timeClockTicks() * gSliceTime;
    }

    x_mSecs = mpeos_timeMillisToClock(1); // init this variable
    gProfiling_Inited = true;

    return MPE_SUCCESS;
}
// frees memory
// deletes the prof so nobody tries to access it
// using a program that doesn't exist
static mpe_Error mpe_profmgrProfilingDestroy(void)
{

    gProfiling_Inited = false; // even if fails, in bad state!

    mpeos_memFreeP(MPE_MEM_UTIL, gPROFTiming); // get rid of memory used

    return MPE_SUCCESS;
}

/**
 * Function dispatch table for MPE Profiling APIs.
 */
mpe_prof_ftable_t profmgr_ftable =
{
    mpe_profmgrInit,

    mpe_profmgrStartTiming,
    mpe_profmgrStopTiming,
    mpe_profmgrProfilingDestroy,
    mpe_profmgrSetWhere,
    mpe_profmgrPopWhereStack,
    mpe_profmgrGetIndex,
    mpe_profmgrDisplayTiming,
    mpe_profmgrAddLabel,
    mpe_profmgrAddComment
};

/**
 * <i>mpe_profSetup<i/>
 *
 * Profile manager setup entry point, which installs the Profile manager's
 * set of MPE functions into the MPE function table.
 * ALWAYS exists
 */
void mpe_profSetup(void)
{
    mpe_sys_install_ftable(&profmgr_ftable, MPE_MGR_TYPE_PROF);
}
#endif /* MPE_FEATURE_PROF */
