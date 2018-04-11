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
 * The APIs described in this section help users of the section filtering code
 *  allocate, deallocate and manipulate an mpe_FilterSpec structure.
 */

/* TSB-TODO get this include list correct/minimal */
#include <mpe_error.h>      /* Resolve error codes. */
#include <mpe_types.h>      /* Resolve basic types. */
#include <mpeos_filter.h>   /* Resolve basic filter types */
#include <mpeos_media.h>
#include <filtermgr.h>      /* Resolve MPE filter types */
#include <mpe_dbg.h>        /* Resolve log message defintions. */
#include <mpe_os.h>

#include <../include/si_util.h>

#include "filter_support.h" /* Get prototypes for functions */

#include <string.h>

/****************************************************************************/
/*                          Public APIs                                     */
/****************************************************************************/

/*****************************************************************************
 filter_zeroSpec()
 This function is used to zero out the positive and negative mask/vals pairs
 in the FilterComponents of the specified FilterSpec.

 @param pFilter - (input/output) specifies the location of the mpe_FilterSpec
 to zero
 @returns MPE_SUCCESS, if successful.
 @returns MPE_EINVAL, if there is a problem with the parameters
 *****************************************************************************/

mpe_Error filter_zeroSpec(mpe_FilterSpec *pFilter)
{
    /* do minimal argument checking */
    if (pFilter == NULL)
    {
        return MPE_EINVAL;
    }

    /* now zero out all the mask and vals bits in the filter */
    memset(pFilter->pos.mask, 0, pFilter->pos.length);
    memset(pFilter->pos.vals, 0, pFilter->pos.length);
    memset(pFilter->neg.mask, 0, pFilter->neg.length);
    memset(pFilter->neg.vals, 0, pFilter->neg.length);

    return MPE_SUCCESS;
}

/*****************************************************************************
 filter_createFilterComps()
 This function is used to allocate all the memory needed for a section filter
 component.  It is designed to be used in tandem with the
 filter_destroyFilterComps function and will additionally check parameters
 (in debug build only) for excessive allocations.

 @param PosFilterLen - (input) specifies the length of the Positive Filter Mask.
 @param NegFilterLen - (input) specifies the length of the Negative Filter Mask.
 @param pFilter - (output) specifies the location of the mpe_FilterSpec to allocate
 and initialize the positive and negative FilterComponents of
 said mpe_FilterSpec.
 @returns MPE_SUCCESS, if successful.
 @returns MPE_EINVAL, if there is a problem with at least one of the parameters
 @returns MPE_ENOMEM, if there is a memory error at any point in the allocations
 *****************************************************************************/
mpe_Error filter_createFilterComps(uint8_t posFilterLen, uint8_t negFilterLen,
        mpe_FilterSpec *pFilter)
{
    mpe_Error ec;

    /* quick check of Filter Lengths */
#if defined(MPE_FEATURE_DEBUG)
    if (posFilterLen > 32)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER,"<createFilterComps> posFilterLen = 0x%x (maximum of 32)\n",posFilterLen);
        posFilterLen = 32;
    }
    if (negFilterLen > 32)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER,"<createFilterComps>  negFilterLen = 0x%x (maximum of 32)\n",negFilterLen);
        negFilterLen = 32;
    }
#endif
    /* check for NULL on the input filter */
    if (pFilter == NULL)
    {
        return MPE_EINVAL;
    }

    /* first, allocate the positive mask/vals Component (if needed) */
    pFilter->pos.length = posFilterLen;
    if (posFilterLen > 0)
    {
        if ((ec = mpeos_memAllocP(MPE_MEM_FILTER, posFilterLen,
                (void **) (&(pFilter->pos.mask)))) != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_FILTER,
                    "<createFilterComps>  mpe_FilterCreateFilterComps() failed to allocate memory\n");
            return ec;
        }
        if ((ec = mpeos_memAllocP(MPE_MEM_FILTER, posFilterLen,
                (void **) (&(pFilter->pos.vals)))) != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_FILTER,
                    "<createFilterComps>  mpe_FilterCreateFilterComps() failed to allocate memory\n");
            mpeos_memFreeP(MPE_MEM_FILTER, pFilter->pos.mask);
            return ec;
        }
    }
    else
    {
        pFilter->pos.mask = NULL;
        pFilter->pos.vals = NULL;
    }

    /* now, repeat the process for the negative mask/vals Component (if needed) */
    pFilter->neg.length = negFilterLen;
    if (negFilterLen > 0)
    {
        if ((ec = mpeos_memAllocP(MPE_MEM_FILTER, negFilterLen,
                (void **) (&(pFilter->neg.mask)))) != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_FILTER,
                    "<createFilterComps>  mpe_FilterCreateFilterComps() failed to allocate memory\n");
            mpeos_memFreeP(MPE_MEM_FILTER, pFilter->pos.vals);
            mpeos_memFreeP(MPE_MEM_FILTER, pFilter->pos.mask);
            return ec;
        }
        if ((ec = mpeos_memAllocP(MPE_MEM_FILTER, negFilterLen,
                (void **) (&(pFilter->neg.vals)))) != MPE_SUCCESS)
        {
            MPE_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_FILTER,
                    "<createFilterComps>  mpe_FilterCreateFilterComps() failed to allocate memory\n");
            mpeos_memFreeP(MPE_MEM_FILTER, pFilter->neg.mask);
            mpeos_memFreeP(MPE_MEM_FILTER, pFilter->pos.vals);
            mpeos_memFreeP(MPE_MEM_FILTER, pFilter->pos.mask);
            return ec;
        }
    }
    else
    {
        pFilter->neg.mask = NULL;
        pFilter->neg.vals = NULL;
    }

    /* now initialize the filters to zero and return */
    return filter_zeroSpec(pFilter);
}

/*****************************************************************************
 filter_destroyFilterComps()
 This function is used to deallocate all the memory used for a section filter
 component.  It is designed to be used in tandem with the
 filter_createFilterComps function.

 @param pFilter - (output) specifies the location of the mpe_FilterSpec to deallocate
 @returns MPE_SUCCESS, if successful.
 @returns MPE_EINVAL, if there is a problem with the parameters
 @returns MPE_ENOMEM, if there is a memory error at any point in the allocations
 *****************************************************************************/
mpe_Error filter_destroyFilterComps(mpe_FilterSpec *pFilter)
{

    /* do minimal argument checking */
    if (pFilter == NULL)
    {
        return MPE_EINVAL;
    }

    /* now Destroy the positive and negative Filter Components */
    if (pFilter->pos.length > 0)
    {
        mpeos_memFreeP(MPE_MEM_FILTER, pFilter->pos.mask);
        pFilter->pos.mask = NULL;

        mpeos_memFreeP(MPE_MEM_FILTER, pFilter->pos.vals);
        pFilter->pos.vals = NULL;

        pFilter->pos.length = 0;
    }

    if (pFilter->neg.length > 0)
    {
        mpeos_memFreeP(MPE_MEM_FILTER, pFilter->neg.mask);
        pFilter->neg.mask = NULL;

        mpeos_memFreeP(MPE_MEM_FILTER, pFilter->neg.vals);
        pFilter->neg.vals = NULL;

        pFilter->neg.length = 0;
    }
    return MPE_SUCCESS;
}

/*****************************************************************************
 filter_createFilterSpec()
 This function is used to allocate all the memory needed for a section filter.
 It is designed to be used in tandem with the filter_destroyFilterSpec
 function and will additionally check parameters (in debug build only) for
 excessive allocations.

 @param PosFilterLen - (input) specifies the length of the Positive Filter Mask.
 @param NegFilterLen - (input) specifies the length of the Negative Filter Mask.
 @param ppFilter - (input/output) specifies the output location of a pointer to
 an mpe_FilterSpec structure (which this routine will allocate).
 Additionally, this routine will also allocate and initialize
 the positive and negative FilterComponents of said mpe_FilterSpec.
 @returns MPE_SUCCESS, if successful.
 @returns MPE_EINVAL, if there is a problem with at least one of the parameters
 @returns MPE_ENOMEM, if there is a memory error at any point in the allocations
 *****************************************************************************/

mpe_Error filter_createFilterSpec( uint8_t posFilterLen,
                                   uint8_t negFilterLen,
                                   mpe_FilterSpec **ppFilter )
{
    mpe_Error ec;

    if (ppFilter == NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER,
                "<createFilterSpec> ppFilter = NULL\n");
        return MPE_EINVAL;
    }

    /* first allocate the mpe_FilterSpec portion of the filter */
    if ((ec = mpeos_memAllocP(MPE_MEM_FILTER, sizeof(mpe_FilterSpec),
            (void **) ppFilter)) != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER,
                "<createFilterSpec> mpe_FilterCreateFilter() failed to allocate memory\n");
        return ec;
    }

    /* now allocate the mask and vals fields of the FilterSpec */
    if ((ec = filter_createFilterComps(posFilterLen, negFilterLen, *ppFilter))
            != MPE_SUCCESS)
    {
        mpeos_memFreeP(MPE_MEM_FILTER, *ppFilter);
        return ec;
    }

    return MPE_SUCCESS;
}

/*****************************************************************************
 filter_destroyFilterSpec()
 This function is used to deallocate all the memory used for a section filter
 component including the filter itself.  It is designed to be used in tandem with the
 filter_createFilterSpec function.

 @param ppFilter - (input/output) specifies a pointer to the location of the
 mpe_FilterSpec to deallocate
 @returns MPE_SUCCESS, if successful.
 @returns MPE_EINVAL, if there is a problem with the parameters
 @returns MPE_ENOMEM, if there is a memory error at any point in the allocations
 *****************************************************************************/

mpe_Error filter_destroyFilterSpec(mpe_FilterSpec *pFilter)
{
    mpe_Error ec;

    /* do minimal argument checking */
    if (pFilter == NULL)
    {
        return MPE_EINVAL;
    }

    /* first Destroy the positive and negative Filter Components */
    if ((ec = filter_destroyFilterComps(pFilter)) != MPE_SUCCESS)
    {
        return ec;
    }

    /* now go and destroy the Filter itself */
    mpeos_memFreeP(MPE_MEM_FILTER, pFilter);

    return MPE_SUCCESS;
}

/*****************************************************************************
 filter_alterSpec()
 This function is used to set up "typical" areas of the FilterComponent (either
 positive or negative).  The debug version does extra sanity tests that shouldn't
 be needed, but are there in the meantime.

 @param pFilter - (input) specifies the location of the mpe_FilterSpec to alter
 @param FiltField - (input) field to alter
 @param FilterVal - (input) value to filter for (or avoid for negative filters)
 @param PosNeg - (input) specifies Component (positive or negative) to alter
 @returns MPE_SUCCESS, if successful.
 @returns MPE_EINVAL, if there is a problem with the parameters
 *****************************************************************************/
mpe_Error filter_alterSpec(mpe_FilterSpec *pFilter, mpe_FilterField filtField,
        uint32_t filterVal, mpe_FieldPolarity posNeg)
{
    mpe_FilterComponent *pComp;
    mpe_FilterComponent *pOppositeComp;
    mpe_Bool resetOpposite;

    /* table to give the minimum size required for a field to be set */
    static uint8_t gNeededLen[MPE_FILTERFIELD_LAST] =
    { 1, /* MPE_FILTERFIELD_TABLE_ID */
    5, /* MPE_FILTERFIELD_EXT_TABLE_ID */
    6, /* MPE_FILTERFIELD_VERSION */
    7 /* MPE_FILTERFIELD_SECTION */
    };

    /* do minimal argument checking */
    if (pFilter == NULL)
    {
        return MPE_EINVAL;
    }

#if defined(MPE_FEATURE_DEBUG)
    {
        uint32_t fieldLen;

        /* additional testing... just in case */
        if (posNeg == MPE_MATCH)
        {
            fieldLen = pFilter->pos.length;
        }
        else
        {
            fieldLen = pFilter->neg.length;
        }

        /* make sure the specified field is part of the filter */
        if (fieldLen < gNeededLen[filtField])
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER,"<alterSpec> mpe_FilterAlterSpec - specified field not in range of Component Length\n");
            return MPE_EINVAL;
        }
    }
#endif

    /* if user called this with a DONT_CARE, just make sure the value is not being filtered */
    if (posNeg == MPE_DONT_CARE)
    {
        /* clear out the appropriate positive field (if applicable) */
        if (pFilter->pos.length >= gNeededLen[filtField])
        {
            switch (filtField)
            {
            case MPE_FILTERFIELD_TABLE_ID:
                pFilter->pos.mask[0] = 0x00;
                break;
            case MPE_FILTERFIELD_EXT_TABLE_ID:
                pFilter->pos.mask[3] = 0x00;
                pFilter->pos.mask[4] = 0x00;
                break;
            case MPE_FILTERFIELD_VERSION:
                pFilter->pos.mask[5] &= 0xc1;
                break;
            case MPE_FILTERFIELD_SECTION:
                pFilter->pos.mask[6] = 0xff;
                break;
            default:
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER,
                        "<alterSpec> invalid filtField\n");
                break;
            }
        }

        /* clear out the appropriate negative field (if applicable) */
        if (pFilter->neg.length >= gNeededLen[filtField])
        {
            switch (filtField)
            {
            case MPE_FILTERFIELD_TABLE_ID:
                pFilter->neg.mask[0] = 0x00;
                break;
            case MPE_FILTERFIELD_EXT_TABLE_ID:
                pFilter->neg.mask[3] = 0x00;
                pFilter->neg.mask[4] = 0x00;
                break;
            case MPE_FILTERFIELD_VERSION:
                pFilter->neg.mask[5] &= 0xc1;
                break;
            case MPE_FILTERFIELD_SECTION:
                pFilter->neg.mask[6] = 0xff;
                break;
            default:
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER,
                        "<alterSpec> invalid filtField\n");
                break;
            }
        }
        return MPE_SUCCESS;
    }

    /* get a pointer to the FilterComponent that will be altered */
    if (posNeg == MPE_MATCH)
    {
        pComp = &pFilter->pos;
        pOppositeComp = &pFilter->neg;
    }
    else
    {
        pComp = &pFilter->neg;
        pOppositeComp = &pFilter->pos;
    }

    /*
     check to see if the "opposite" filter is even big enough to consider making sure its
     not set.
     */
    if (pOppositeComp->length < gNeededLen[filtField])
    {
        resetOpposite = FALSE;
    }
    else
    {
        resetOpposite = TRUE;
    }

    /* now update the mask and value based on the Field to be altered */
    switch (filtField)
    {
    case MPE_FILTERFIELD_TABLE_ID:
        pComp->mask[0] = 0xff;
        pComp->vals[0] = ((uint8_t) filterVal) & 0xff;
        if (resetOpposite)
        {
            pOppositeComp->mask[0] = 0x00;
        }
        break;

    case MPE_FILTERFIELD_EXT_TABLE_ID:
        pComp->mask[3] = 0xff;
        pComp->vals[3] = ((uint8_t)(filterVal >> 8)) & 0xff;
        pComp->mask[4] = 0xff;
        pComp->vals[4] = ((uint8_t) filterVal) & 0xff;
        if (resetOpposite)
        {
            pOppositeComp->mask[3] = 0x00;
            pOppositeComp->mask[4] = 0x00;
        }
        break;

    case MPE_FILTERFIELD_VERSION:
        pComp->mask[5] |= 0x3e;
        pComp->vals[5] = (((uint8_t) filterVal) & 0x1f) << 1;
        if (resetOpposite)
        {
            /* reset the bits that aren't the version field */
            pOppositeComp->mask[5] &= 0xc1;
        }
        break;

    case MPE_FILTERFIELD_SECTION:
        pComp->mask[6] = 0xff;
        pComp->vals[6] = ((uint8_t) filterVal) & 0xff;
        if (resetOpposite)
        {
            pOppositeComp->mask[6] = 0x00;
        }
        break;
    default:
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<alterSpec> invalid filtField\n");
        break;

    }

    return MPE_SUCCESS;
}

/*****************************************************************************
 filter_setSpec()
 This function is used to set up multiple "typical" areas of the FilterComponent
 (either positive or negative).

 @param pFilter - (input) specifies the location of the mpe_FilterSpec to alter
 @param tableId - (input) Table ID to filter for (MUST BE POSITIVE FILTER)
 @param matchTableIdExt - (input) specifies whether to MATCH or DONT_MATCH the
 Table ID Extension field
 @param tableIdExt - (input) Extended table ID to match (or avoid)
 @param matchVer - (input) specifies whether to MATCH or DONT_MATCH the
 table version field
 @param version - (input) table version number to match (or avoid)
 @param matchSect - (input) specifies whether to MATCH or DONT_MATCH the
 table section field
 @param section - (input) table section number to match (or avoid)
 @returns MPE_SUCCESS, if successful.
 @returns MPE_EINVAL, if there is a problem with the parameters
 *****************************************************************************/
mpe_Error filter_setSpec(mpe_FilterSpec *pFilter, uint32_t tableId,
        mpe_FieldPolarity matchTableIdExt, uint32_t tableIdExt,
        mpe_FieldPolarity matchVer, uint32_t version,
        mpe_FieldPolarity matchSect, uint32_t section)
{
    /* do minimal argument checking */
    if (pFilter == NULL)
    {
        return MPE_EINVAL;
    }

    filter_alterSpec(pFilter, MPE_FILTERFIELD_TABLE_ID, tableId, MPE_MATCH);

    /* only set up the areas of the spec if they care about the outcome */
    if (matchTableIdExt != MPE_DONT_CARE)
    {
        filter_alterSpec(pFilter, MPE_FILTERFIELD_EXT_TABLE_ID, tableIdExt,
                matchTableIdExt);
    }
    if (matchVer != MPE_DONT_CARE)
    {
        filter_alterSpec(pFilter, MPE_FILTERFIELD_VERSION, version, matchVer);
    }
    if (matchSect != MPE_DONT_CARE)
    {
        filter_alterSpec(pFilter, MPE_FILTERFIELD_SECTION, section, matchSect);
    }

    return MPE_SUCCESS;
}

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////
// Filter group datatypes/variables/functions
///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

#define FILTER_MAX_SECTION_SIZE (4096)
#define FILTER_TIME_MIN(X,Y) ((X) < (Y) ? (X) : (Y))
#define FILTER_NO_WAIT_WAITTIME (1)

typedef enum
{
    /** Filter has been removed from the DB (and presumably freed) */
    FILTER_SHARED_FILTER_STATE_DELETED = 0,
    /** Filter is ready to be set (either initially or after time-slice) */
    FILTER_SHARED_FILTER_STATE_READY,
    /** Filter is set (has a native filter set for it) */
    FILTER_SHARED_FILTER_STATE_SET,
    /** Filter is fully matched (will no longer be set) */
    FILTER_SHARED_FILTER_STATE_MATCHED,
    /** Filter timed out (will no longer be set) */
    FILTER_SHARED_FILTER_STATE_TIMED_OUT,
    /** Filter was canceled by the platform. It will not be reset. */
    FILTER_SHARED_FILTER_STATE_CANCELLED,
    /** Native filter was set and canceled for time-slice. Waiting for ack
        to move back to READY state */
    FILTER_SHARED_FILTER_STATE_READY_PENDING,
    /** Native filter was set and canceled due to timeout. Waiting for ack
        to move to TIMEOUT state */
    FILTER_SHARED_FILTER_STATE_TIMEOUT_PENDING,
    /** Native filter was set and canceled due to being removed. Waiting for
        ack to delete */
    FILTER_SHARED_FILTER_STATE_REMOVE_PENDING,
    /** Native filter failed to set. Async event dispatched to invoke
        callback */
    FILTER_SHARED_FILTER_STATE_SET_FAILED,
    /** Filter delete requested while in callback  */
    FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK
} filter_SharedFilterState;

#if defined(MPE_FEATURE_DEBUG)
// Note: Indexes must be lined up with the enum values
static char * filter_SharedFilterStateString[] =
    {
        "DELETED",
        "READY",
        "SET",
        "MATCHED",
        "TIMED_OUT",
        "CANCELLED",
        "READY_PENDING",
        "TIMEOUT_PENDING",
        "REMOVE_PENDING",
        "SET_FAILED",
        "DEL_IN_CB"
    };
#endif

typedef struct filter_SharedFilter
{
    // These fields should not change once set

    struct filter_FilterGroup * group;
    mpe_FilterSource filterSource;
    mpe_FilterSpec filterSpec;
    uint8_t priority;
    uint32_t matchCount;
    uint32_t timeoutMs;
    void * userPointer;
    uint32_t userData;
    mpe_SharedFilterCallback callbackFunc;

    // These fields are dynamic

    filter_SharedFilterState state;
    mpe_Bool inCallback;
    uint32_t remainingMatches;
    int32_t remainingTimeoutMs;
    mpe_TimeMillis lastSetTimeMs;
    uint32_t filterId;
    struct filter_SharedFilter * prev;
    struct filter_SharedFilter * next;
} filter_SharedFilter;

typedef enum
{
    FILTER_GROUP_STATE_DELETED = 0,
    FILTER_GROUP_STATE_PAUSED,
    FILTER_GROUP_STATE_STOPPED,
    FILTER_GROUP_STATE_RUNNING,
    FILTER_GROUP_STATE_DELETE_PENDING
} filter_FilterGroupState;

#if defined(MPE_FEATURE_DEBUG)
// Note: Indexes must be lined up with the enum values
static char * filter_FilterGroupStateString[] =
    {
        "DELETED",
        "PAUSED",
        "STOPPED",
        "RUNNING",
        "DELETE_PENDING"
    };
#endif

typedef struct filter_FilterGroup
{
    filter_FilterGroupState state;
    char name[40];
    uint32_t intervalMs;
    int32_t remainingIntervalMs;
    filter_SharedFilter * firstFilter;
    filter_SharedFilter * activeFilter;
    uint8_t activePriority;
    uint32_t numSetOrReady;
    mpe_TimeMillis lastStartedTimeMs;
    struct filter_FilterGroup * prev;
    struct filter_FilterGroup * next;
} filter_FilterGroup;

static mpe_Mutex g_filterGroupDBMutex = NULL;
static mpe_ThreadId g_filterGroupThreadID = (mpe_ThreadId) - 1;
static mpe_EventQueue g_filterGroupThreadQueue;
static mpe_Bool g_filterValidateFilterPointers;
static mpe_Bool g_filterLookupFiltersByFilterID;

// The head of the FilterGroup list
static filter_FilterGroup * g_filterGroupHead = NULL;

static uint8_t g_sectionBuffer[FILTER_MAX_SECTION_SIZE];
static uint32_t g_sectionBufferSize;

typedef enum
{
    FILTER_GROUP_THREAD_EVENT_INVALID = 0,
    FILTER_GROUP_THREAD_EVENT_WAKEUP = 1,
    FILTER_GROUP_THREAD_EVENT_SET_FAILED = 2
} filter_FilterGroupThreadEvent;

// The shared filter that's the next to timeout
static filter_SharedFilter * g_filter_nextFilterToTimeout;

// The filter group that's next to switch filters
static filter_FilterGroup * g_filter_nextGroupToSwitch;

// The epoch time of the next timeout (timeout or group switch)
static mpe_TimeMillis g_filter_nextThreadWakeupTimeMs;

// Prototypes for helper functions                          */

static void filter_filterGroupThread(void * threadData);

void filter_adjustActiveTimeouts( const mpe_TimeMillis timeSliceStart,
                                      const mpe_TimeMillis timeSliceEnd,
                                      filter_SharedFilter ** const pNextSharedFilterToTimeout,
                                      filter_FilterGroup ** const pNextFilterGroupToSwitch );

void filter_processFilterTimeout(filter_SharedFilter * sharedFilter);

void filter_processFilterGroupTimeslice(filter_FilterGroup * const filterGroup);

void filter_processFilterGroupThreadEvent( const mpe_Event eventId,
                                           const void * eventData1,
                                           const void * eventData2,
                                           const uint32_t eventData3 );

void filter_unlinkFilterGroup(filter_FilterGroup * targetGroup);

void filter_unlinkFilterFromGroup(filter_SharedFilter * targetFilter);

filter_SharedFilter * filter_findHighestPriorityReadyFilterInGroup(
                                 const filter_FilterGroup * targetGroup);

mpe_Bool filter_isFilterInGroup( const filter_SharedFilter * const targetFilter,
                                 const filter_FilterGroup * const targetGroup );

filter_SharedFilter * filter_findNextReadyFilterAtPriority(
                                    filter_FilterGroup * const targetGroup );

mpe_Error filter_setMpeosFilter(filter_SharedFilter * const targetFilter);

mpe_Error filter_cancelMpeosFilter( filter_FilterGroup * const targetGroup,
                                    const filter_SharedFilterState newState );
mpe_Error filter_releaseMpeosFilter(filter_SharedFilter * const targetFilter);
mpe_Bool filter_filterGroupIsValid(filter_FilterGroup * const filterGroup);
mpe_Bool filter_filterIsValid(filter_SharedFilter * const sharedFilter);
filter_SharedFilter * filter_filterForID(uint32_t id);

void filter_dumpFilterGroupDB(const int level, const char * func);
void filter_dumpFilterGroup(const int level, const char * func, const filter_FilterGroup * targetGroup);
void filter_dumpSharedFilter(const int level, const char * func, const filter_SharedFilter * targetFilter);
void filter_dumpSharedFilterSource(const int level, const char * func, const filter_SharedFilter * targetFilter);

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

void filter_releaseFilterGroup(filter_FilterGroup * filterGroup)
{
    mpeos_memFreeP(MPE_MEM_FILTER, filterGroup);
}

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_newFilterGroup(filter_FilterGroup ** pNewFilterGroup)
{
    return mpeos_memAllocP( MPE_MEM_FILTER,
                            sizeof(filter_FilterGroup),
                            (void **) pNewFilterGroup );
}

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

void filter_releaseSharedFilter(filter_SharedFilter * sharedFilter)
{
    if (sharedFilter->filterId != 0)
    {
        filter_releaseMpeosFilter(sharedFilter);
    }
    mpeos_memFreeP(MPE_MEM_FILTER, sharedFilter->filterSpec.pos.vals);
    mpeos_memFreeP(MPE_MEM_FILTER, sharedFilter);
}

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_newSharedFilter( const mpe_FilterSource * filterSource,
                                  const mpe_FilterSpec * filterSpec,
                                  uint32_t timesToMatch,
                                  uint8_t filterPriority,
                                  uint32_t timeoutMs,
                                  void * userPointer,
                                  uint32_t userData,
                                  mpe_SharedFilterCallback callbackFunc,
                                  filter_SharedFilter ** sharedFilter )
{
    filter_SharedFilter * newSharedFilter;
    mpe_Error retCode;

    // TODO: Consider using a pool for these...
    retCode = mpeos_memAllocP( MPE_MEM_FILTER,
                               sizeof(filter_SharedFilter),
                               (void **) &newSharedFilter );
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error creating new filter group (error %d)\n",
                                               __func__, retCode );
        return retCode;
    }

    newSharedFilter->filterSource = *filterSource;
    newSharedFilter->filterSpec.pos.length = filterSpec->pos.length;
    newSharedFilter->filterSpec.neg.length = filterSpec->neg.length;

    { // Deep copy the mask/value arrays
        uint8_t * filterBlock;
        // Allocate one block for all val/mask arrays
        //  (and remember: only one deallocation too)
        retCode = mpeos_memAllocP( MPE_MEM_FILTER,
                                   filterSpec->pos.length * 2
                                   + filterSpec->neg.length * 2,
                                   (void **)&filterBlock );
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error allocating positive filter block (error %d)\n",
                                                  __func__, retCode );
            filter_releaseSharedFilter(newSharedFilter);
            return retCode;
        }

        newSharedFilter->filterSpec.pos.vals = filterBlock;

        if (filterSpec->pos.length > 0)
        { // Copy positive elements
            memcpy(filterBlock, filterSpec->pos.vals, filterSpec->pos.length);
            filterBlock += filterSpec->pos.length;
            memcpy(filterBlock, filterSpec->pos.mask, filterSpec->pos.length);
            newSharedFilter->filterSpec.pos.mask = filterBlock;
            filterBlock += filterSpec->pos.length;
        }
        else
        {
            newSharedFilter->filterSpec.pos.mask = NULL;
        }

        if (filterSpec->neg.length > 0)
        { // Copy negative elements
            memcpy(filterBlock, filterSpec->neg.vals, filterSpec->neg.length);
            newSharedFilter->filterSpec.neg.vals = filterBlock;
            filterBlock += filterSpec->neg.length;
            memcpy(filterBlock, filterSpec->neg.mask, filterSpec->neg.length);
            newSharedFilter->filterSpec.neg.mask = filterBlock;
        }
        else
        {
            newSharedFilter->filterSpec.neg.vals = NULL;
            newSharedFilter->filterSpec.neg.mask = NULL;
        }
    } // Done with filterSpec

    newSharedFilter->priority = filterPriority;
    newSharedFilter->matchCount = timesToMatch;
    newSharedFilter->remainingMatches = timesToMatch;
    newSharedFilter->timeoutMs = timeoutMs;
    newSharedFilter->remainingTimeoutMs = timeoutMs;
    newSharedFilter->lastSetTimeMs = 0;
    newSharedFilter->userPointer = userPointer;
    newSharedFilter->userData = userData;
    newSharedFilter->callbackFunc = callbackFunc;
    newSharedFilter->filterId = 0;
    newSharedFilter->inCallback = FALSE;

    // Note: next/prev pointers are the caller's problem

    *sharedFilter = newSharedFilter;

    return MPE_SUCCESS;
} // END filter_newSharedFilter()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

/**
 * For starting up the filter group system
 */
mpe_Error filter_filterGroupStartup(void)
{
    mpe_Error retCode;
    const char *safemode_enable_setting = NULL;

    MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILTER, "<%s>: STARTUP\n", __func__);

    /* Create filter group database mutex */
    retCode = mpe_mutexNew(&g_filterGroupDBMutex);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG( MPE_LOG_ERROR, MPE_MOD_FILTER,
                 "<%s>: Failed to create filter group DB mutex - returning.\n",
                 __func__ );
        return retCode;
    }

    safemode_enable_setting = mpeos_envGet("FILTERGROUP.SAFEMODE.ENABLED");
    if ((NULL == safemode_enable_setting) || (stricmp(safemode_enable_setting, "FALSE") == 0))
    {
        g_filterValidateFilterPointers = FALSE;
        g_filterLookupFiltersByFilterID = FALSE;
    }
    else
    {
        MPE_LOG(MPE_LOG_INFO, MPE_MOD_FILTER,
                "<%s> - FilterGroup safe mode is ENABLED\n",
                __func__ );

        g_filterValidateFilterPointers = TRUE;
        g_filterLookupFiltersByFilterID = TRUE;
    }

    retCode = mpeos_eventQueueNew(&g_filterGroupThreadQueue, "MpeFilterGroupQ");
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG( MPE_LOG_ERROR, MPE_MOD_FILTER,
                 "<%s>: Failed to create filter group queue (returned %d).\n",
                 __func__, retCode );
        return retCode;
    }

    g_filter_nextFilterToTimeout = NULL;
    g_filter_nextGroupToSwitch = NULL;
    g_filter_nextThreadWakeupTimeMs = INT64_MAX;

    retCode = mpeos_threadCreate( filter_filterGroupThread,
                                  NULL,
                                  MPE_THREAD_PRIOR_SYSTEM,
                                  MPE_THREAD_STACK_SIZE,
                                  &g_filterGroupThreadID,
                                  "filterGroupThread" );
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG( MPE_LOG_ERROR, MPE_MOD_FILTER,
                 "<%s>: Failed to create psiWorkerThread, error: %ld\n",
                 __func__, retCode );
    }
    else
    {
        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                 "<%s>: Filter group thread started successfully (thread ID 0x%x)\n",
                 __func__, g_filterGroupThreadID );
    }

    MPE_LOG( MPE_LOG_INFO, MPE_MOD_FILTER,
             "<%s>: STARTUP COMPLETE.\n", __func__ );
    return MPE_SUCCESS;
} // END filter_filterGroupStartup()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

/**
 * For shutting down the filter group system
 */
void filter_filterGroupShutdown(void)
{
    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s()\n", __func__);
} // END filter_filterGroupShutdown()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_createFilterGroup( uint32_t intervalMs,
                                    char * name,
                                    mpe_FilterGroup * filterGroup )
{
    filter_FilterGroup * newGroup;
    mpe_Error retCode;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(intervalMs %dms, name \"%s\" fg* %p)\n",
                                           __func__, intervalMs, name, filterGroup );

    mpe_mutexAcquire(g_filterGroupDBMutex);

    retCode = filter_newFilterGroup(&newGroup);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error creating new filter group (error %d)\n",
                                               __func__, retCode );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return retCode;
    }

    newGroup->intervalMs = intervalMs;
    strncpy(newGroup->name, name, sizeof(newGroup->name));
    newGroup->name[sizeof(newGroup->name)-1] = '\0';
    newGroup->firstFilter = NULL;
    newGroup->activeFilter = NULL;
    newGroup->activePriority = 0;
    newGroup->numSetOrReady = 0;
    newGroup->state = FILTER_GROUP_STATE_STOPPED;

    // Always put the new group at the head
    newGroup->next = g_filterGroupHead;
    newGroup->prev = NULL;

    if (g_filterGroupHead)
    {
        g_filterGroupHead->prev = newGroup;
    }

    g_filterGroupHead = newGroup;

    *filterGroup = (mpe_FilterGroup)newGroup;

    // Note: Adding an empty filter group doesn't affect anything
    //       that the service thread needs to do.

    mpe_mutexRelease(g_filterGroupDBMutex);

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Created FilterGroup %p (\"%s\")\n",
                                           __func__, newGroup, name );

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(intervalMs %dms, name \"%s\" fg* %p) returning MPE_SUCCESS\n",
                                           __func__, intervalMs, name, filterGroup );
    return MPE_SUCCESS;
} // END filter_createFilterGroup()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_startFilterGroup(mpe_FilterGroup filterGroup)
{
    mpe_Error retCode;
    filter_FilterGroup * const targetGroup = (filter_FilterGroup *) filterGroup;
    filter_SharedFilter * targetFilter;
    filter_FilterGroupState groupState;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p)\n",
                                            __func__, filterGroup );

    mpe_mutexAcquire(g_filterGroupDBMutex);

    if (!filter_filterGroupIsValid(targetGroup))
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: INVALID FILTERGROUP %p!\n",
                 __func__, targetGroup );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return MPE_EINVAL;
    }

    if ( (targetGroup->state == FILTER_GROUP_STATE_RUNNING)
         && !targetGroup->activeFilter
         && targetGroup->firstFilter )
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Filtergroup %p RUNNING with no active filter - treating it as PAUSED\n",
                                               __func__, targetGroup );
        groupState = FILTER_GROUP_STATE_PAUSED;
    }
    else
    {
        groupState = targetGroup->state;
    }

    switch (groupState)
    {
        case FILTER_GROUP_STATE_DELETED:
        case FILTER_GROUP_STATE_DELETE_PENDING:
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Filter group %p has already been deleted! (state %s)\n",
                                                   __func__, targetGroup, targetGroup->state );
            retCode = MPE_EINVAL;
            break;
        }
        case FILTER_GROUP_STATE_RUNNING:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Nothing to do - filter group %p was already running\n",
                                                   __func__, targetGroup );
            retCode = MPE_SUCCESS;
            break;
        }
        case FILTER_GROUP_STATE_STOPPED:
        case FILTER_GROUP_STATE_PAUSED:
        {
            mpe_TimeMillis curTimeMs;

            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: STARTING filter group %p from %s state\n",
                                                   __func__, targetGroup,
                                                   filter_FilterGroupStateString[targetGroup->state]);
            retCode = mpeos_timeGetMillis(&curTimeMs);
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                         "<%s>: mpeos_timeGetMillis failure (returned %d)\n",
                         __func__, retCode );
                curTimeMs = 0;
            }

            if (targetGroup->activeFilter)
            {
                if (targetGroup->numSetOrReady == 1)
                {
                    // We must have been paused with a filter that stayed active
                    //  through the pause - nothing to do
                    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Nothing to do - filter group %p already had a single SET filter\n",
                                                           __func__, targetGroup );
                }
                else if (targetGroup->intervalMs > 0)
                { // Round-robining is enabled on this group.
                  // Check to see if the thread's timeout time needs to be recalculated...
                    mpe_TimeMillis nextTimesliceTimeForGroupMs;

                    nextTimesliceTimeForGroupMs =
                            curTimeMs + targetGroup->remainingIntervalMs;

                    if ( nextTimesliceTimeForGroupMs < g_filter_nextThreadWakeupTimeMs )
                    { // Need to wakeup the thread so it can change its wait timeout
                        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Timeout time of started group %p (%llums from now) is earlier than current thread wait (%llums from now) - WAKING UP event thread...\n",
                                                               __func__, targetGroup,
                                                               nextTimesliceTimeForGroupMs-curTimeMs,
                                                               g_filter_nextThreadWakeupTimeMs-curTimeMs );

                        retCode = mpe_eventQueueSend( g_filterGroupThreadQueue,
                                                      FILTER_GROUP_THREAD_EVENT_WAKEUP,
                                                      NULL, NULL, 0 );
                        if (retCode != MPE_SUCCESS)
                        {
                            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error sending WAKEUP event to filter group thread queue %p (error %d)\n",
                                                                   __func__,
                                                                   g_filterGroupThreadQueue,
                                                                   retCode );
                        }
                    }
                } // END else/if (targetGroup->numSetOrReady == 1)
            } // END if (targetGroup->activeFilter)
            else
            {
                targetFilter = filter_findHighestPriorityReadyFilterInGroup(targetGroup);

                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: found filter %p to activate\n",
                                                       __func__, targetFilter );

                if (targetFilter != NULL)
                {
                    retCode = filter_setMpeosFilter(targetFilter);
                    // Note: This will set the group activeFilter/activePriority and
                    //       poke the group thread to adjust its sleep time, if necessary.
                    if (retCode != MPE_SUCCESS)
                    {
                        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error activating filter %p in group %p (error %d)\n",
                                                               __func__,
                                                               targetFilter, targetGroup,
                                                               retCode );
                        // Attempt to carry on and deal with sporadic platform
                        //  failures. We'll try another set on the next group
                        //  timeout
                    }
                } // END if (targetFilter != NULL)
            } // END else/if (targetGroup->activeFilter)

            // We started a filter or there were no filters to start

            targetGroup->state = FILTER_GROUP_STATE_RUNNING;
            targetGroup->lastStartedTimeMs = curTimeMs;
            targetGroup->remainingIntervalMs = targetGroup->intervalMs;

            // Dump the group when we transition to RUNNING
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: STARTED filter group %p:\n",
                                                   __func__, targetGroup );
            filter_dumpFilterGroup(MPE_LOG_DEBUG, __func__, targetGroup);

            retCode = MPE_SUCCESS;
            break;
        }
        default:
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Filter group %p was in an unknown state!\n",
                                                   __func__, targetGroup );
            retCode = MPE_EINVAL;
            break;
        }
    } // END switch (targetGroup->state)

    mpe_mutexRelease(g_filterGroupDBMutex);

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "<%s> returning %d (0x%x)\n",
                                            __func__, retCode, retCode );

    return retCode;
} // END filter_startFilterGroup()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_pauseFilterGroup(mpe_FilterGroup filterGroup)
{
    mpe_Error retCode;
    filter_FilterGroup * targetGroup = (filter_FilterGroup *) filterGroup;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p)\n",
                                            __func__, filterGroup );
    mpe_mutexAcquire(g_filterGroupDBMutex);

    if (!filter_filterGroupIsValid(targetGroup))
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: INVALID FILTERGROUP %p!\n",
                 __func__, targetGroup );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return MPE_EINVAL;
    }

    switch (targetGroup->state)
    {
        case FILTER_GROUP_STATE_DELETED:
        case FILTER_GROUP_STATE_DELETE_PENDING:
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Filter group %p has already been deleted! (state %s)\n",
                                                   __func__, targetGroup, targetGroup->state );
            retCode = MPE_EINVAL;
            break;
        }
        case FILTER_GROUP_STATE_RUNNING:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: PAUSING running filter group %p\n",
                                                   __func__, targetGroup );
            // Not much to do here - responsibility to honor the pause is
            //  left to other parts of the code...
            targetGroup->state = FILTER_GROUP_STATE_PAUSED;

            filter_dumpFilterGroup(MPE_LOG_DEBUG, __func__, targetGroup);

            if (g_filter_nextGroupToSwitch == targetGroup)
            {
                g_filter_nextGroupToSwitch = NULL;
            }
            retCode = MPE_SUCCESS;
            break;
        }
        case FILTER_GROUP_STATE_PAUSED:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Nothing to do - filter group %p was already paused\n",
                                                   __func__, targetGroup );
            retCode = MPE_SUCCESS;
            break;
        }
        case FILTER_GROUP_STATE_STOPPED:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Can't pause STOPPED filter group %p\n",
                                                   __func__, targetGroup );
            retCode = MPE_EINVAL;
            break;
        }
        default:
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Filter group %p was in an unknown state!\n",
                                                   __func__, targetGroup );
            retCode = MPE_EINVAL;
            break;
        }
    } // END switch (targetGroup->state)

    MPE_LOG( MPE_LOG_TRACE2, MPE_MOD_FILTER,
             "<%s>: Filter group dump:\n", __func__);
    filter_dumpFilterGroup(MPE_LOG_TRACE2, __func__, targetGroup);

    mpe_mutexRelease(g_filterGroupDBMutex);

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p) returning %d (0x%x)\n",
                                            __func__, filterGroup,
                                            retCode, retCode );
    return retCode;
} // END filter_pauseFilterGroup()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_stopFilterGroup(mpe_FilterGroup filterGroup)
{
    mpe_Error retCode;
    filter_FilterGroup * targetGroup = (filter_FilterGroup *) filterGroup;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p)\n",
                                            __func__, filterGroup );
    mpe_mutexAcquire(g_filterGroupDBMutex);

    if (!filter_filterGroupIsValid(targetGroup))
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: INVALID FILTERGROUP %p!\n",
                 __func__, targetGroup );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return MPE_EINVAL;
    }

    switch (targetGroup->state)
    {
        case FILTER_GROUP_STATE_DELETED:
        case FILTER_GROUP_STATE_DELETE_PENDING:
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Filter group %p has already been deleted! (state %s)\n",
                                                   __func__, targetGroup, targetGroup->state );
            retCode = MPE_EINVAL;
            break;
        }
        case FILTER_GROUP_STATE_PAUSED:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: STOPPING paused filter group %p\n",
                                                   __func__, targetGroup );

            // Just a little sanity check...
            if (targetGroup->activeFilter)
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Found PAUSED filter group %p with active filter!\n",
                                                       __func__, targetGroup );

                filter_dumpFilterGroup(MPE_LOG_WARN, __func__, targetGroup);
                // Fall through to STOP code...
            }
            else
            { // All is well...
                targetGroup->state = FILTER_GROUP_STATE_STOPPED;

                // g_filter_nextGroupToSwitch should not be set to targetGroup

                filter_dumpFilterGroup(MPE_LOG_DEBUG, __func__, targetGroup);
                retCode = MPE_SUCCESS;
                break;
            }
        }
        case FILTER_GROUP_STATE_RUNNING:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: STOPPING filter group %p (state %s)\n",
                                                   __func__, targetGroup,
                                                   filter_FilterGroupStateString[targetGroup->state] );

            retCode = filter_cancelMpeosFilter( targetGroup,
                                                FILTER_SHARED_FILTER_STATE_READY_PENDING );
            if (retCode == MPE_SUCCESS)
            {
                targetGroup->state = FILTER_GROUP_STATE_STOPPED;
                if (g_filter_nextGroupToSwitch == targetGroup)
                {
                    g_filter_nextGroupToSwitch = NULL;
                }
                filter_dumpFilterGroup(MPE_LOG_DEBUG, __func__, targetGroup);
            }
            else
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error deactivating filter during filter group %p STOP!\n",
                                                       __func__, targetGroup );

                filter_dumpFilterGroup(MPE_LOG_WARN, __func__, targetGroup);
            }

            break;
        }
        case FILTER_GROUP_STATE_STOPPED:
        {
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Nothing to do - filter group %p was already stopped\n",
                                                   __func__, targetGroup );
            retCode = MPE_SUCCESS;
            break;
        }
        default:
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Filter group %p was in an unknown state!\n",
                                                   __func__, targetGroup );
            retCode = MPE_EINVAL;
            break;
        }
    } // END switch (targetGroup->state)

    MPE_LOG( MPE_LOG_TRACE2, MPE_MOD_FILTER,
             "<%s>: Filter group dump:\n", __func__);
    filter_dumpFilterGroup(MPE_LOG_TRACE2, __func__, targetGroup);

    mpe_mutexRelease(g_filterGroupDBMutex);

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p) returning %d (0x%x)\n",
                                            __func__, filterGroup,
                                            retCode, retCode );
    return retCode;
} // END filter_stopFilterGroup()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_destroyFilterGroup(mpe_FilterGroup filterGroup)
{
    filter_FilterGroup * targetGroup = (filter_FilterGroup *) filterGroup;
    filter_SharedFilter * curFilter, *lastFilter, *targetFilter;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p)\n",
                                            __func__, filterGroup );

    mpe_mutexAcquire(g_filterGroupDBMutex);

    if (!filter_filterGroupIsValid(targetGroup))
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: INVALID FILTERGROUP %p!\n",
                 __func__, targetGroup );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return MPE_EINVAL;
    }

    lastFilter = targetGroup->firstFilter;

    if (targetGroup->firstFilter != NULL)
    {
        // We'll cancel all the filters in the group - but if
        //  there's a running filter, we'll need it to wait for
        //  the async cancel
        lastFilter = targetGroup->firstFilter->prev;
        curFilter = targetGroup->firstFilter;
        do
        {
            targetFilter = curFilter;
            curFilter = curFilter->next;

            filter_removeSharedFilter( (mpe_SharedFilter)targetFilter );
        } while (targetFilter != lastFilter);
    } // END if (firstFilter != NULL)

    // Assert: All filters have been removed or canceled

    if (targetGroup->firstFilter == NULL)
    { // All the filters were non-running (probably a non-running group)
        filter_unlinkFilterGroup(targetGroup);
        filter_releaseFilterGroup(targetGroup);
        targetGroup = NULL;
    }
    else
    { // Mark it - reap it when the filter is canceled
        targetGroup->state = FILTER_GROUP_STATE_DELETE_PENDING;
    }

    if (g_filter_nextGroupToSwitch == targetGroup)
    {
        g_filter_nextGroupToSwitch = NULL;
    }

    mpe_mutexRelease(g_filterGroupDBMutex);

    return MPE_SUCCESS;
} // END filter_destroyFilterGroup()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_addSharedFilter( mpe_FilterGroup filterGroup,
                                  const mpe_FilterSource * filterSource,
                                  const mpe_FilterSpec * filterSpec,
                                  uint32_t timesToMatch,
                                  uint8_t filterPriority,
                                  uint32_t timeout,
                                  void * userPointer,
                                  uint32_t userData,
                                  mpe_SharedFilterCallback callbackFunc,
                                  mpe_SharedFilter * sharedFilter )
{
    filter_FilterGroup  * targetGroup = (filter_FilterGroup  *)filterGroup;
    filter_SharedFilter * newSharedFilter;
    filter_SharedFilter * oldHead;
    mpe_Error retCode;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p, source (type %d, PID 0x%x), "
                                           "ttm %d, priority %d, timeout %dms, upointer %p, udata 0x%08x, "
                                           "callback %p, sfp %p)\n",
                                           __func__,
                                           filterGroup, filterSource->sourceType, filterSource->pid,
                                           timesToMatch, filterPriority, timeout, userPointer, userData,
                                           callbackFunc, sharedFilter );
    mpe_mutexAcquire(g_filterGroupDBMutex);

    if ( (filterSource == NULL)
         || (filterSpec == NULL)
         || (callbackFunc == NULL)
         || (sharedFilter == NULL) )
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: NULL PARAM PASSED (fg %p, fsrc %p, fspec %p, cb %p, sfp %p)\n",
                                               __func__,
                                               filterGroup, filterSource, filterSpec, callbackFunc, sharedFilter );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return MPE_EINVAL;
    }

    if (!filter_filterGroupIsValid(targetGroup))
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: INVALID FILTERGROUP %p!\n",
                 __func__, targetGroup );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return MPE_EINVAL;
    }

    // Constructor sets most of the fields
    retCode = filter_newSharedFilter( filterSource,
                                      filterSpec,
                                      timesToMatch,
                                      filterPriority,
                                      timeout,
                                      userPointer,
                                      userData,
                                      callbackFunc,
                                      &newSharedFilter );
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error creating new shared filter (error %d)\n",
                                               __func__, retCode );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return retCode;
    }

    newSharedFilter->state = FILTER_SHARED_FILTER_STATE_READY;
    newSharedFilter->group = (filter_FilterGroup *)filterGroup;

    // Link the new filter into the group
    oldHead = targetGroup->firstFilter;

    if (oldHead == NULL)
    {
        newSharedFilter->next = newSharedFilter;
        newSharedFilter->prev = newSharedFilter;
        targetGroup->firstFilter = newSharedFilter;
    }
    else
    {
        newSharedFilter->next = oldHead;
        newSharedFilter->prev = oldHead->prev;

        oldHead->prev->next = newSharedFilter;
        oldHead->prev = newSharedFilter;
    }

    targetGroup->numSetOrReady++;

    // The new filter is now in the list and is READY

    if (targetGroup->state == FILTER_GROUP_STATE_RUNNING)
    {
        // TODO: Cancel a pre-empted filter even if we're paused
        if (newSharedFilter->priority > targetGroup->activePriority)
        {
            if (targetGroup->activeFilter)
            { // Need to preempt the running filter
                retCode = filter_cancelMpeosFilter( targetGroup,
                                                    FILTER_SHARED_FILTER_STATE_READY_PENDING);
                // Note: This will clear the group's activeFilter/activePriority
                // Cancel liberates resources synchronously - so we can set
                //  a new filter now...
                if (retCode != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error deactivating group %p (error %d)\n",
                                                           __func__,
                                                           targetGroup, retCode );
                    filter_dumpFilterGroup(MPE_LOG_WARN, __func__, targetGroup);
                }
            }

            // Lower-priority filter has been deactivated or no filter was active
            //  in the group

            retCode = filter_setMpeosFilter(newSharedFilter);
            // Note: This will set the group activeFilter/activePriority and
            //       poke the group thread to adjust its sleep time, if necessary.
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error activating filter %p (error %d)\n",
                                                       __func__,
                                                       newSharedFilter );
                filter_dumpSharedFilter(MPE_LOG_WARN, __func__, newSharedFilter);
                mpe_mutexRelease(g_filterGroupDBMutex);
                return retCode;
            }
        } // END if (newSharedFilter->priority > targetGroup->activePriority)

        if (targetGroup->numSetOrReady == 2)
        { // May need to schedule the start of the next slice...
            mpe_TimeMillis curTimeMs, nextTimesliceTimeForGroupMs;

            retCode = mpeos_timeGetMillis(&curTimeMs);
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                         "<%s>: mpeos_timeGetMillis failure (returned %d)\n",
                         __func__, retCode );
                curTimeMs = 0;
            }
            nextTimesliceTimeForGroupMs =
                    curTimeMs + targetGroup->remainingIntervalMs;

            if ( nextTimesliceTimeForGroupMs < g_filter_nextThreadWakeupTimeMs )
            { // Need to wakeup the thread so it can change its wait timeout
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Group timesharing re-enabled & timeout time of group %p (%llums from now) is earlier than current thread wait (%llums from now) - WAKING UP event thread...\n",
                                                       __func__, targetGroup,
                                                       nextTimesliceTimeForGroupMs-curTimeMs,
                                                       g_filter_nextThreadWakeupTimeMs-curTimeMs );

                retCode = mpe_eventQueueSend( g_filterGroupThreadQueue,
                                              FILTER_GROUP_THREAD_EVENT_WAKEUP,
                                              NULL, NULL, 0 );
                if (retCode != MPE_SUCCESS)
                {
                    MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error sending WAKEUP event to filter group thread queue %p (error %d)\n",
                                                           __func__,
                                                           g_filterGroupThreadQueue,
                                                           retCode );
                }
            }
        } // END if (targetGroup->numSetOrReady == 2)
    } // END if (group is RUNNING)

    // Everything's good-to-go
    *sharedFilter = (mpe_SharedFilter)newSharedFilter;

    MPE_LOG( MPE_LOG_TRACE2, MPE_MOD_FILTER,
             "<%s>: Filter group dump:\n", __func__);
    filter_dumpFilterGroup(MPE_LOG_TRACE2, __func__, targetGroup);

    mpe_mutexRelease(g_filterGroupDBMutex);

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p, source (type %d, PID 0x%x), ...) "
                                           "created filter %p, returned %d (0x%x)\n",
                                           __func__,
                                           filterGroup, filterSource->sourceType, filterSource->pid,
                                           *sharedFilter, retCode, retCode );
    return MPE_SUCCESS;
} // END filter_addSharedFilter()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_removeSharedFilter(mpe_SharedFilter sharedFilter)
{
    mpe_Error retCode = MPE_EINVAL;
    filter_SharedFilter * targetFilter = (filter_SharedFilter *)sharedFilter;
    filter_FilterGroup * targetGroup = targetFilter->group;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(sf %p)\n",
                                            __func__,
                                            sharedFilter );

    mpe_mutexAcquire(g_filterGroupDBMutex);

    if (!filter_filterIsValid(targetFilter))
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: INVALID FILTER %p!\n",
                 __func__, targetFilter );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return MPE_EINVAL;
    }

    if (targetGroup->firstFilter == NULL)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: TRIED TO REMOVE FILTER FROM EMPTY GROUP (fg %p, sf %p)\n",
                                               __func__,
                                               targetGroup, targetFilter );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return MPE_EINVAL;
    }

    switch (targetFilter->state)
    {
        case FILTER_SHARED_FILTER_STATE_DELETED:
            // If we're here, the filter has been freed and we're touching
            //  memory we shouldn't be touching!
        case FILTER_SHARED_FILTER_STATE_REMOVE_PENDING:
        case FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK:
        { // As far as the caller is concerned, these filters are already removed...
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: TRIED TO REMOVE FILTER IN INVALID STATE (fg %p, sf %p, state %s)\n",
                                                   __func__,
                                                   targetGroup, targetFilter,
                                                   filter_SharedFilterStateString[targetFilter->state] );
            filter_dumpFilterGroup(MPE_LOG_WARN, __func__, targetGroup);
            retCode = MPE_EINVAL;
            break;
        }
        case FILTER_SHARED_FILTER_STATE_SET:
        {
            if (targetGroup->activeFilter != targetFilter)
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Filter %p in SET state is not the group's active filter\n",
                                                       __func__, targetFilter );
                filter_dumpFilterGroup(MPE_LOG_WARN, __func__, targetGroup);
                retCode = MPE_EINVAL;
                break;
            }

            // Can't delete it right away - need to wait for the async cancel
            //  (we want to ensure the ACT refers to a valid filter and we may
            //  have an already-enqueued/in-flight event to process on the thread)
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Filter %p is SET - canceling filter ID %d (0x%x) and setting filter to REMOVE_PENDING\n",
                                                   __func__, targetFilter,
                                                   targetFilter->filterId,
                                                   targetFilter->filterId );
            retCode = filter_cancelMpeosFilter( targetGroup,
                                                FILTER_SHARED_FILTER_STATE_REMOVE_PENDING );
            // Note: This will clear the group activeFilter/activePriority
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                         "<%s>: Failed to deactivate active filter in group %p (returned %d).\n",
                         __func__, targetGroup, retCode );
                break;
            }

            if (g_filter_nextFilterToTimeout == targetFilter)
            { // To ensure we don't process/signal a timeout on this filter
                g_filter_nextFilterToTimeout = NULL;
            }

            // We had a set filter, so it's fair to say it's running. But let's
            //  not assume...
            if (targetGroup->state == FILTER_GROUP_STATE_RUNNING)
            {
                // Consider the group suspended and turn the crank...
                targetGroup->state = FILTER_GROUP_STATE_PAUSED;
                filter_startFilterGroup((mpe_FilterGroup)targetGroup);
            }
            retCode = MPE_SUCCESS;
            break;
        }
        case FILTER_SHARED_FILTER_STATE_READY_PENDING:
        case FILTER_SHARED_FILTER_STATE_TIMEOUT_PENDING:
        { // We're already waiting for a cancel ack. Just change the state
          //  so it's deleted when the cancel is processed
            targetFilter->state = FILTER_SHARED_FILTER_STATE_REMOVE_PENDING;
            retCode = MPE_SUCCESS;
            break;
        }
        case FILTER_SHARED_FILTER_STATE_READY:
        {
            targetGroup->numSetOrReady--;
            // FALL THROUGH
        }
        case FILTER_SHARED_FILTER_STATE_MATCHED:
        case FILTER_SHARED_FILTER_STATE_TIMED_OUT:
        case FILTER_SHARED_FILTER_STATE_CANCELLED:
        { // If we're removing a non-set, non-canceled filter, we can deal with it now
            if (targetFilter->inCallback)
            {
                MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                         "<%s>: Filter delete while in callback - changing filter %p state to %s\n",
                         __func__ , targetFilter,
                         filter_SharedFilterStateString[FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK] );
                targetFilter->state = FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK;
            }
            else
            {
                MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Filter %p is %s - releasing it from group %p now\n",
                                                       __func__, targetFilter,
                                                       filter_SharedFilterStateString[targetFilter->state],
                                                       targetGroup );

                filter_unlinkFilterFromGroup(targetFilter);
                filter_releaseSharedFilter(targetFilter);
                targetFilter = NULL;
                retCode = MPE_SUCCESS;
            } // END else/if (targetFilter->inCallback)
            break;
        }
        default:
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: TRIED TO REMOVE FILTER IN UNKNOWN STATE (fg %p, sf %p, stateval %d)\n",
                                                   __func__,
                                                   targetGroup, targetFilter,
                                                   targetFilter->state );
            retCode = MPE_EINVAL;
            break;
        }
    } // END switch (targetFilter->state)

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Completed\n", __func__ );

    MPE_LOG( MPE_LOG_TRACE2, MPE_MOD_FILTER,
             "<%s>: Filter group dump:\n", __func__);
    filter_dumpFilterGroup(MPE_LOG_TRACE2, __func__, targetGroup);

    mpe_mutexRelease(g_filterGroupDBMutex);

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(sf %p) returning %d (0x%x)\n",
                                            __func__,
                                            sharedFilter,
                                            retCode, retCode );
    return retCode;
} // END filter_removeSharedFilter()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_groupForFilter( mpe_SharedFilter sharedFilter,
                                 mpe_FilterGroup * filterGroup)
{
    filter_SharedFilter * targetFilter = (filter_SharedFilter *)sharedFilter;

    mpe_mutexAcquire(g_filterGroupDBMutex);

    if (!filter_filterIsValid(targetFilter))
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: INVALID FILTER %p!\n",
                 __func__, targetFilter );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return MPE_EINVAL;
    }

    *filterGroup = (mpe_FilterGroup)(targetFilter->group);

    mpe_mutexRelease(g_filterGroupDBMutex);

    return MPE_SUCCESS;
} // END filter_groupForFilter()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_getSharedFilterState( mpe_SharedFilter sharedFilter,
                                       mpe_SharedFilterState * filterState)
{
    filter_SharedFilter * targetFilter = (filter_SharedFilter *)sharedFilter;

    mpe_mutexAcquire(g_filterGroupDBMutex);

    if (!filter_filterIsValid(targetFilter))
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: INVALID FILTER %p!\n",
                 __func__, targetFilter );
        mpe_mutexRelease(g_filterGroupDBMutex);
        return MPE_EINVAL;
    }

    switch (targetFilter->state)
    {
        case FILTER_SHARED_FILTER_STATE_READY:
        case FILTER_SHARED_FILTER_STATE_SET:
        case FILTER_SHARED_FILTER_STATE_READY_PENDING:
        case FILTER_SHARED_FILTER_STATE_SET_FAILED:
        {
            *filterState = MPE_FILTERGROUP_FILTERSTATE_SET;
            break;
        }
        case FILTER_SHARED_FILTER_STATE_DELETED:
        case FILTER_SHARED_FILTER_STATE_REMOVE_PENDING:
        case FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK:
        {
            *filterState = MPE_FILTERGROUP_FILTERSTATE_DELETED;
            break;
        }
        case FILTER_SHARED_FILTER_STATE_MATCHED:
        {
            *filterState = MPE_FILTERGROUP_FILTERSTATE_MATCHED;
            break;
        }
        case FILTER_SHARED_FILTER_STATE_TIMED_OUT:
        case FILTER_SHARED_FILTER_STATE_TIMEOUT_PENDING:
        {
            *filterState = MPE_FILTERGROUP_FILTERSTATE_TIMEDOUT;
            break;
        }
        case FILTER_SHARED_FILTER_STATE_CANCELLED:
        {
            *filterState = MPE_FILTERGROUP_FILTERSTATE_CANCELLED;
            break;
        }
        default:
        {
            *filterState = MPE_FILTERGROUP_FILTERSTATE_INVALID;
            break;
        }
    } // switch (targetFilter->state)

    mpe_mutexRelease(g_filterGroupDBMutex);

    return MPE_SUCCESS;
} // END filter_getSharedFilterState()


///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

void filter_unlinkFilterFromGroup(filter_SharedFilter * targetFilter)
{
    // Assert: Filter DB mutex is owned by the caller
    filter_FilterGroup * targetGroup = targetFilter->group;

    // Link around the target
    //  (if this is the only entry, this does nothing since this is a circular LL)
    targetFilter->prev->next = targetFilter->next;
    targetFilter->next->prev = targetFilter->prev;

    // Mark it dead, just in case someone tries to pass it back in...
    targetFilter->state = FILTER_SHARED_FILTER_STATE_DELETED;

    // Make sure it wasn't the head...
    if (targetGroup->firstFilter == targetFilter)
    {
        if (targetFilter->next == targetFilter)
        { // The target is linked to itself - so it's the only entry
            targetGroup->firstFilter = NULL;
        }
        else
        { // Update firstFilter to the next filter
            targetGroup->firstFilter = targetFilter->next;
        }
    }
} // END filter_unlinkFilterFromGroup()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

void filter_unlinkFilterGroup(filter_FilterGroup * targetGroup)
{
    // Assert: Filter DB mutex is owned by the caller
    if (targetGroup->next)
    {
        targetGroup->next->prev = targetGroup->prev;
    }

    if (targetGroup->prev)
    {
        targetGroup->prev->next = targetGroup->next;
    }

    if (g_filterGroupHead == targetGroup)
    {
        g_filterGroupHead = targetGroup->next;
    }
} // END filter_unlinkFilterGroup()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

static void filter_filterGroupThread(void * threadData)
{
    mpe_Error retCode;
    mpe_TimeMillis timeBeforeWaitMs;
    mpe_TimeMillis lastTimeBeforeWaitMs;

    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
             "<%s>: STARTING...\n", __func__ );

    // Note: We'll hold the mutex except when in the wait
    mpe_mutexAcquire(g_filterGroupDBMutex);

    retCode = mpeos_timeGetMillis(&timeBeforeWaitMs);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: Failed to timeGetMillis for timeBeforeWaitMs initialization (returned %d).\n",
                 __func__, retCode );
        timeBeforeWaitMs = 0;
    }

    while (TRUE)
    {
        mpe_Event event = 0;
        void * eventData1 = NULL;
        void * eventData2 = NULL;
        uint32_t eventData3 = 0;
        mpe_Bool waitIndefinite;

        int32_t waitTimeMs;

        // Calculate the duration of the last timeslice

        lastTimeBeforeWaitMs = timeBeforeWaitMs;
        retCode = mpeos_timeGetMillis(&timeBeforeWaitMs);
        if (retCode != MPE_SUCCESS)
        {
            MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                     "<%s>: Failed to timeGetMillis (returned %d).\n",
                     __func__, retCode );
            timeBeforeWaitMs = lastTimeBeforeWaitMs;
        }

        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                     "<%s>: Last timeslice was %ldms...\n",
                     __func__, timeBeforeWaitMs-lastTimeBeforeWaitMs );

        // Adjust remaining time by slice time and find the smallest (nearest)
        //  quantum/timeout
        filter_adjustActiveTimeouts( lastTimeBeforeWaitMs,
                                     timeBeforeWaitMs,
                                     &g_filter_nextFilterToTimeout,
                                     &g_filter_nextGroupToSwitch );
        if (g_filter_nextGroupToSwitch)
        {
            waitIndefinite = FALSE;
            waitTimeMs = g_filter_nextGroupToSwitch->remainingIntervalMs;
            g_filter_nextThreadWakeupTimeMs = timeBeforeWaitMs + waitTimeMs;
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                         "<%s>: Found nearest timeout (%ldms) for group %p...\n",
                         __func__, waitTimeMs, g_filter_nextGroupToSwitch );
        }
        else if (g_filter_nextFilterToTimeout)
        { // Next timeout is for a filter
            waitIndefinite = FALSE;
            waitTimeMs = g_filter_nextFilterToTimeout->remainingTimeoutMs;
            g_filter_nextThreadWakeupTimeMs = timeBeforeWaitMs + waitTimeMs;
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                         "<%s>: Found nearest timeout (%ldms) for filter %p...\n",
                         __func__, waitTimeMs, g_filter_nextFilterToTimeout );
        }
        else
        {
            waitIndefinite = TRUE;
            g_filter_nextThreadWakeupTimeMs = INT64_MAX;
        }

        if (waitIndefinite)
        {
            waitTimeMs = 0;
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                         "<%s>: Waiting for event without timeout...\n",
                         __func__ );
        }
        else
        {
            waitTimeMs = (waitTimeMs > 0) ? waitTimeMs: FILTER_NO_WAIT_WAITTIME;
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                         "<%s>: Waiting for event with timeout (%dms)...\n",
                         __func__, waitTimeMs );
        }

        if (waitTimeMs != FILTER_NO_WAIT_WAITTIME)
        {
            mpe_mutexRelease(g_filterGroupDBMutex);
        }

        retCode = mpeos_eventQueueWaitNext( g_filterGroupThreadQueue,
                                            &event,
                                            &eventData1,
                                            &eventData2,
                                            &eventData3,
                                            waitTimeMs );

        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                     "<%s>: Came out of mpeos_eventQueueWaitNext with retCode %d (0x%x)\n",
                     __func__, retCode, retCode );

        if (waitTimeMs != FILTER_NO_WAIT_WAITTIME)
        {
            mpe_mutexAcquire(g_filterGroupDBMutex);
        }

        if (retCode == MPE_ETIMEOUT)
        { // If we didn't wait or wait timed out, we didn't process an event
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                     "<%s>: Processing timeout...\n", __func__ );

            if (g_filter_nextGroupToSwitch)
            { // Our wait timeout was for a group and it timed out
                filter_processFilterGroupTimeslice(g_filter_nextGroupToSwitch);
            }
            else if (g_filter_nextFilterToTimeout)
            { // Our wait timeout was for a filter and it timed out
                filter_processFilterTimeout(g_filter_nextFilterToTimeout);
            }
            else
            { // Both g_filter_nextGroupToSwitch and
              //  g_filter_nextFilterToTimeout could be null when
              //  the group/filter we were waiting for was deleted/stopped/
              //  paused while we were waiting for timeout
                MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                         "<%s>: Timed out with nothing to do...\n",
                         __func__, retCode );
            }
        } // END if (wait was skipped or timed out)
        else if (retCode == MPE_SUCCESS)
        { // We woke up due to an event
            //
            // If we're here, we received an event...
            //

            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                     "<%s>: Received event %d (0x%x) - processing...\n",
                     __func__, event, event );

            filter_processFilterGroupThreadEvent( event,
                                                  eventData1,
                                                  eventData2,
                                                  eventData3 );
        }
        else
        { // Processing a non-timeout error waiting for an event
            MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                     "<%s>: eventQueueWaitNext error (returned %d)\n",
                     __func__, retCode );
        }

        // And around we go...
    } // END while (TRUE)
} // END filter_filterGroupThread()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

/**
 * Adjust the timeouts for active groups/filters and determine the smallest
 * timeout in the FilterGroup DB.
 */
void filter_adjustActiveTimeouts( const mpe_TimeMillis timeSliceStart,
                                      const mpe_TimeMillis timeSliceEnd,
                                      filter_SharedFilter ** const pNextSharedFilterToTimeout,
                                      filter_FilterGroup ** const pNextFilterGroupToSwitch )
{
    // Assert: Filter DB mutex is owned by the caller

    filter_FilterGroup * curGroup;
    filter_FilterGroup * smallestGroup = NULL;
    filter_SharedFilter * smallestFilter = NULL;
    int32_t smallestTimeoutMs = INT32_MAX;

    MPE_LOG( MPE_LOG_TRACE1, MPE_MOD_FILTER,
            "%s(timeSliceStart %llums, timeSliceEnd %llums, pNextSharedFilterToTimeout %p, pNextFilterGroupToSwitch %p)\n",
//             "%s(timeSliceDelta %ldms, pNextSharedFilterToTimeout %p, pNextFilterGroupToSwitch %p)\n",
             __func__,
             timeSliceStart, timeSliceEnd,
             pNextSharedFilterToTimeout,
             pNextFilterGroupToSwitch );

    curGroup = g_filterGroupHead;
    while (curGroup != NULL)
    {
        filter_SharedFilter * curFilter;

        if ( (curGroup->state == FILTER_GROUP_STATE_RUNNING)
             && curGroup->activeFilter
             && (curGroup->numSetOrReady > 1) )
        {
            int32_t adjustmentMs
                  = FILTER_TIME_MIN( timeSliceEnd - curGroup->lastStartedTimeMs,
                                     timeSliceEnd - timeSliceStart );
            curGroup->remainingIntervalMs -= adjustmentMs;

            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                     "<%s>: adjusting group %p by %dms. Remaining time to switch %dms\n",
                     __func__, curGroup, adjustmentMs,
                     curGroup->remainingIntervalMs );

            if (curGroup->remainingIntervalMs < smallestTimeoutMs)
            {
                smallestTimeoutMs = curGroup->remainingIntervalMs;
                smallestGroup = curGroup;
                smallestFilter = NULL;
            }
        }

        curFilter = curGroup->activeFilter;
        if (curFilter != NULL)
        {
            if (curFilter->state != FILTER_SHARED_FILTER_STATE_SET)
            {
                MPE_LOG( MPE_LOG_INFO, MPE_MOD_FILTER,
                         "<%s>: The active filter %p is not in SET state!\n",
                         __func__, curFilter );
            }
            else if (curFilter->timeoutMs)
            { // If timeoutMs is 0, the filter doesn't have a timeout - skip it
                int32_t adjustmentMs
                      = FILTER_TIME_MIN( timeSliceEnd - curFilter->lastSetTimeMs,
                                         timeSliceEnd - timeSliceStart );

                curFilter->remainingTimeoutMs -= adjustmentMs;

                MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                         "<%s>: adjusted filter %p by %dms:\n",
                         __func__, curFilter, adjustmentMs );
                filter_dumpSharedFilter(MPE_LOG_DEBUG, __func__, curFilter);

                if (curFilter->remainingTimeoutMs < smallestTimeoutMs)
                {
                    smallestTimeoutMs = curFilter->remainingTimeoutMs;
                    smallestFilter = curFilter;
                    smallestGroup = NULL;
                }
            }
        } // END if (curFilter != NULL)

        curGroup = curGroup->next;
    } // END while (curGroup != NULL)

    *pNextSharedFilterToTimeout = smallestFilter;
    *pNextFilterGroupToSwitch = smallestGroup;

    if (smallestGroup == NULL && smallestFilter == NULL)
    {
        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                 "<%s>: No timeout found (no active groups/filters)\n",
                 __func__);
    }
    else
    {
        // Assert: Either have a smallest group or a smallest filter timeout

        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                 "<%s>: next timeout is for %s %p (%dms)\n",
                 __func__,
                 (smallestFilter ? "SharedFilter" : "FilterGroup"),
                 (smallestFilter ? (void*)smallestFilter : (void*)smallestGroup),
                 smallestTimeoutMs );
    }

    MPE_LOG( MPE_LOG_TRACE1, MPE_MOD_FILTER,
             "<%s>: Complete\n", __func__ );
    return;
} // END filter_adjustActiveTimeouts()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

void filter_processFilterTimeout(filter_SharedFilter * timedOutFilter)
{
    filter_FilterGroup * filterGroup;

    // Assert: Filter DB mutex is owned by the caller

    MPE_LOG( MPE_LOG_TRACE1, MPE_MOD_FILTER,
             "%s(filter %p)\n", __func__, timedOutFilter );

    if (timedOutFilter == NULL)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: NULL filter passed!\n",
                                               __func__ );
        return;
    }

    if (timedOutFilter->state != FILTER_SHARED_FILTER_STATE_SET)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Filter %p timed out, but was not set!\n",
                                               __func__,
                                               timedOutFilter );
        filter_dumpSharedFilter(MPE_LOG_WARN, __func__, timedOutFilter);
        return;
    }

    filterGroup = timedOutFilter->group;

    filter_cancelMpeosFilter( filterGroup,
                              FILTER_SHARED_FILTER_STATE_TIMEOUT_PENDING );
    // Note: This will clear the group activeFilter/activePriority

    if (filterGroup->state == FILTER_GROUP_STATE_RUNNING)
    {
        filterGroup->state = FILTER_GROUP_STATE_PAUSED;
    }

    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
             "<%s>: Invoking MPE_FILTERGROUP_EVENT_TIMEOUT callback to function %p: filter %p, group %p, userpointer %p, userdata 0x%08x\n",
             __func__, timedOutFilter->callbackFunc, timedOutFilter,
             timedOutFilter->userPointer, timedOutFilter->userData );

    timedOutFilter->inCallback = TRUE;

    // Don't hold the lock during the callback - to prevent lock inversions
    mpe_mutexRelease(g_filterGroupDBMutex);

    // Perform callback
    (timedOutFilter->callbackFunc)( (mpe_SharedFilter) timedOutFilter,
                                    (mpe_FilterGroup) timedOutFilter->group,
                                    timedOutFilter->userPointer,
                                    timedOutFilter->userData,
                                    MPE_FILTERGROUP_EVENT_TIMEOUT,
                                    0, NULL, FALSE );

    mpe_mutexAcquire(g_filterGroupDBMutex);

    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
             "<%s>: MPE_FILTERGROUP_EVENT_TIMEOUT callback returned\n",
             __func__ );

    // NOTE: DB MAY HAVE BEEN CHANGED AND/OR ACTIVATED IN THE CALLBACK

    timedOutFilter->inCallback = FALSE;

    if (timedOutFilter->state == FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK)
    {
        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                 "<%s>: Performing callback-delayed filter release for filter %p\n",
                 __func__ , timedOutFilter );
        filter_unlinkFilterFromGroup(timedOutFilter);

        if (filterGroup->state == FILTER_GROUP_STATE_DELETE_PENDING)
        { // And a group that's waiting for its one filter's latent death...
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                     "<%s>: Performing callback-delayed filter group release for group %p\n",
                     __func__ , filterGroup );
            filter_unlinkFilterGroup(filterGroup);
            filter_releaseFilterGroup(filterGroup);
            filterGroup = NULL;
        }
        filter_releaseSharedFilter(timedOutFilter);
        timedOutFilter = NULL;
    }

    MPE_LOG( MPE_LOG_TRACE2, MPE_MOD_FILTER,
             "<%s>: Filter group dump:\n", __func__);
    filter_dumpFilterGroup(MPE_LOG_TRACE2, __func__, filterGroup);

    MPE_LOG( MPE_LOG_TRACE1, MPE_MOD_FILTER,
             "<%s>: Complete\n", __func__ );
} // END filter_processFilterTimeout()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

void filter_processFilterGroupTimeslice(filter_FilterGroup * const filterGroup)
{
    filter_SharedFilter * nextFilter;
    mpe_TimeMillis curTime;
    mpe_Error retCode;

    // Assert: Filter DB mutex is owned by the caller

    MPE_LOG( MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p)\n",
                                             __func__, filterGroup );

    // We've processed a timeslice - start a new one
    filterGroup->remainingIntervalMs = filterGroup->intervalMs;

    if (!filterGroup->activeFilter)
    {
        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER, "No filter currently active in group %p\n",
                                                __func__, filterGroup );
        return;
    }

    nextFilter = filter_findNextReadyFilterAtPriority(filterGroup);

    if (nextFilter == NULL)
    {
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: No other filters currently READY at priority %d in group %p - nothing to do\n",
                                               __func__,
                                               filterGroup->activePriority,
                                               filterGroup );
        filter_dumpFilterGroup(MPE_LOG_DEBUG, __func__, filterGroup);
        return;
    }

    if (nextFilter == filterGroup->activeFilter)
    { // There's currently only 1 filter at the priority - let it stand
        MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Only one filter at priority %d in group %p - nothing to do\n",
                                               __func__,
                                               filterGroup->activePriority,
                                               filterGroup );
        return;
    }

    // Otherwise, it's time to switch filters...

    filter_cancelMpeosFilter( filterGroup,
                              FILTER_SHARED_FILTER_STATE_READY_PENDING );

    // Cancel liberates resources synchronously - so we can set now...
    retCode = filter_setMpeosFilter(nextFilter);
    // Note: This will set the group activeFilter/activePriority and
    //       poke the group thread to adjust its sleep time, if necessary.
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error activating filter %p in group %p (error %d)\n",
                                               __func__,
                                               nextFilter, filterGroup,
                                               retCode );
        return;
    }

    retCode = mpeos_timeGetMillis(&curTime);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: timeGetMillis returned %d (0x%x)!\n",
                 __func__, retCode, retCode );
    }
    else
    {
        filterGroup->lastStartedTimeMs = curTime;
    }

    MPE_LOG( MPE_LOG_TRACE2, MPE_MOD_FILTER,
             "<%s>: Filter group dump:\n", __func__);
    filter_dumpFilterGroup(MPE_LOG_TRACE2, __func__, filterGroup);

    MPE_LOG( MPE_LOG_TRACE1, MPE_MOD_FILTER, "<%s>: Complete\n", __func__ );
    return;
} // END filter_processFilterGroupTimeslice()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

void filter_processFilterGroupThreadEvent( const mpe_Event eventId,
                                           const void * eventData1,
                                           const void * eventData2,
                                           const uint32_t eventData3 )
{
    filter_SharedFilter * filter = NULL;
    filter_FilterGroup * filterGroup = NULL;
    uint32_t filterId = (uint32_t)eventData1;
    mpe_Error retCode;

    // Assert: Filter DB mutex is owned by the caller
    MPE_LOG( MPE_LOG_TRACE1, MPE_MOD_FILTER,
             "%s(eventId 0x%x,"
             "eventData1 %p,eventData2 %p,eventData3 0x%08x)\n",
             __func__, eventId, eventData1, eventData2, eventData3 );

    switch (eventId)
    {
        case FILTER_GROUP_THREAD_EVENT_WAKEUP:
        {
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                     "<%s>: Processing WAKEUP event\n",
                     __func__ );
            break;
        }
        case MPE_SF_EVENT_SECTION_FOUND:
        case MPE_SF_EVENT_LAST_SECTION_FOUND:
        {
            //
            // SECTION FOUND
            //
            mpe_FilterSectionHandle sectionHandle = MPE_SF_INVALID_SECTION_HANDLE;

            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                     "<%s>: Processing %s event for filterID %d (0x%x)\n",
                     __func__, (eventId == MPE_SF_EVENT_SECTION_FOUND)
                               ? "MPE_SF_EVENT_SECTION_FOUND"
                               : "MPE_SF_EVENT_LAST_SECTION_FOUND",
                     filterId, filterId );

            if (g_filterLookupFiltersByFilterID)
            {
                filter = filter_filterForID(filterId);
            }
            else
            {
                // State logic should ensure that this is always a safe cast
                filter = (filter_SharedFilter *)eventData2;
            }

            if (filter == NULL)
            {
                MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                         "<%s>: INVALID filter for event %d (0x%d) (filterID %d (0x%x), eventData2 %p)!\n",
                         __func__, eventId, eventId,
                         filterId, filterId, eventData2 );
                return;
            }

            if (filter->filterId != filterId)
            {
                MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                         "<%s>: Event %d (0x%x) FilterID %d (0x%x) doesn't match filter's FilterID %d (0x%x)\n",
                         __func__, eventId, eventId, filterId, filterId,
                         filter->filterId, filter->filterId );
                filter_dumpSharedFilter(MPE_LOG_WARN, __func__, filter);
            }

            filter_dumpSharedFilter(MPE_LOG_DEBUG, __func__, filter);

            filterGroup = filter->group;

            switch (filter->state)
            {
                case FILTER_SHARED_FILTER_STATE_SET:
                {
                    filter->remainingMatches--;

                    if (eventId == MPE_SF_EVENT_LAST_SECTION_FOUND)
                    {
                        if (filterGroup->state == FILTER_GROUP_STATE_RUNNING)
                        {
                            filterGroup->state = FILTER_GROUP_STATE_PAUSED;
                        }
                        filter->state = FILTER_SHARED_FILTER_STATE_MATCHED;
                        filterGroup->activeFilter = NULL;
                        filterGroup->activePriority = 0;
                        filterGroup->numSetOrReady--;
                    }

                    retCode = mpeos_filterGetSectionHandle( filter->filterId,
                                                            0,
                                                            &sectionHandle );

                    if (eventId == MPE_SF_EVENT_LAST_SECTION_FOUND)
                    { // We tried to get the section data from the last filter. We're done with it.
                        filter_releaseMpeosFilter(filter);
                    }

                    if (retCode != MPE_SUCCESS)
                    {
                        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                                 "<%s>: MPE error %d (0x%08x) retrieving section handle for filter %p\n",
                                 __func__, retCode, retCode, filter );
                        filter_dumpSharedFilter(MPE_LOG_WARN, __func__, filter);
                        sectionHandle = MPE_SF_INVALID_SECTION_HANDLE;
                        break;
                    }

                    retCode = mpeos_filterSectionRead( sectionHandle,
                                                       0, FILTER_MAX_SECTION_SIZE,
                                                       0,
                                                       g_sectionBuffer,
                                                       &g_sectionBufferSize );
                    if (retCode != MPE_SUCCESS)
                    {
                        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                                 "<%s>: MPE error %d (0x%08x) reading from section handle 0x%08x for filter %p\n",
                                 __func__, retCode, retCode, sectionHandle, filter );
                        filter_dumpSharedFilter(MPE_LOG_WARN, __func__, filter);
                        g_sectionBufferSize = 0;
                        break;
                    }

                    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                            "<%s>: Copying data from filter %d (0x%x) section %d (%p) "
                            "(%02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x...)\n",
                            __func__, filter->filterId, filter->filterId,
                            sectionHandle, sectionHandle,
                            g_sectionBuffer[0],g_sectionBuffer[1],g_sectionBuffer[2],
                            g_sectionBuffer[3],g_sectionBuffer[4],g_sectionBuffer[5],
                            g_sectionBuffer[6],g_sectionBuffer[7],g_sectionBuffer[8],
                            g_sectionBuffer[9],g_sectionBuffer[10],g_sectionBuffer[11] );

                    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                             "<%s>: Invoking MPE_FILTERGROUP_EVENT_MATCHED callback to function %p: filter %p, userpointer %p, userdata 0x%08x\n",
                             __func__, filter->callbackFunc, filter,
                             filter->userPointer, filter->userData );

                    filter->inCallback = TRUE;

                    // Don't hold the lock during the callback - to prevent lock inversions
                    mpe_mutexRelease(g_filterGroupDBMutex);

                    // Perform callback
                    (filter->callbackFunc)( (mpe_SharedFilter) filter,
                                            (mpe_FilterGroup) filter->group,
                                            filter->userPointer,
                                            filter->userData,
                                            MPE_FILTERGROUP_EVENT_MATCHED,
                                            g_sectionBufferSize,
                                            g_sectionBuffer,
                                            eventId == MPE_SF_EVENT_LAST_SECTION_FOUND );

                    mpe_mutexAcquire(g_filterGroupDBMutex);

                    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                             "<%s>: MPE_FILTERGROUP_EVENT_MATCHED callback returned\n",
                             __func__ );

                    // NOTE: DB MAY HAVE BEEN CHANGED AND/OR ACTIVATED IN THE CALLBACK

                    // Section data would have need to been processed in the callback
                    //   Consider the data gone as of now
                    g_sectionBufferSize = 0;

                    filter->inCallback = FALSE;

                    if (filter->state == FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK)
                    {
                        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                 "<%s>: Performing callback-delayed filter release for filter %p\n",
                                 __func__ , filter );
                        filter_unlinkFilterFromGroup(filter);

                        if (filterGroup->state == FILTER_GROUP_STATE_DELETE_PENDING)
                        { // And a group that's waiting for its one filter's latent death...
                            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                     "<%s>: Performing callback-delayed filter group release for group %p\n",
                                     __func__ , filterGroup );
                            filter_unlinkFilterGroup(filterGroup);
                            filter_releaseFilterGroup(filterGroup);
                            filterGroup = NULL;
                        }
                        filter_releaseSharedFilter(filter);
                        filter = NULL;
                    }
                    break;
                } // END case FILTER_SHARED_FILTER_STATE_SET
                case FILTER_SHARED_FILTER_STATE_READY_PENDING:
                { // We're not going to pass this section (we already consider it cancelled)
                    if (eventId == MPE_SF_EVENT_LAST_SECTION_FOUND)
                    { // This is the terminal event for this filter - treat it as such
                        // The filter was canceled for time-slicing - make it READY
                        //  now that it's async termination has arrived
                        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                 "<%s>: Ignoring MPE_SF_EVENT_LAST_SECTION_FOUND for filter %p in state %s - moving to READY state\n",
                                 __func__ , filter, filter_SharedFilterStateString[filter->state] );

                        // We're done with the mpeos filter - it's reached a terminal state
                        filter_releaseMpeosFilter(filter);

                        filter->state = FILTER_SHARED_FILTER_STATE_READY;
                        filterGroup->numSetOrReady++;
                        // Another filter should have already been activated, if desired.
                    }
                    break;
                } // END case FILTER_SHARED_FILTER_STATE_READY_PENDING
                case FILTER_SHARED_FILTER_STATE_REMOVE_PENDING:
                { // We're not going to pass this section (we already consider it cancelled)
                    if (eventId == MPE_SF_EVENT_LAST_SECTION_FOUND)
                    { // This is the terminal event for this filter - treat it as such
                      // The shared filter was released by the owner, this is its latent death
                        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                 "<%s>: Handling delayed filter release for filter %p\n",
                                 __func__ , filter );

                        // We're done with the mpeos filter - it's reached a terminal state
                        filter_releaseMpeosFilter(filter);

                        if (filter->inCallback)
                        {
                            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                     "<%s>: Filter delete while in callback - changing filter %p state to %s\n",
                                     __func__ , filter,
                                     filter_SharedFilterStateString[FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK] );
                            filter->state = FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK;
                        }
                        else
                        { // We can blow away the shared filter now that the MPEOS filter is done
                            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                     "<%s>: Not in callback - releasing filter %p\n",
                                     __func__ , filter );
                            filter_unlinkFilterFromGroup(filter);

                            if (filterGroup->state == FILTER_GROUP_STATE_DELETE_PENDING)
                            { // And a group that's waiting for its one filter's latent death...
                                MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                         "<%s>: Performing delayed filter group release for group %p\n",
                                         __func__ , filterGroup );
                                filter_unlinkFilterGroup(filterGroup);
                                filter_releaseFilterGroup(filterGroup);
                            }
                            filter_releaseSharedFilter(filter);
                            filter = NULL;
                        } // END else/if (filter->inCallback)
                    }
                    break;
                }
                case FILTER_SHARED_FILTER_STATE_TIMEOUT_PENDING:
                {
                    // Client doesn't expect a callback in this state
                    if (eventId == MPE_SF_EVENT_LAST_SECTION_FOUND)
                    { // This is the terminal event for this filter - treat it as such
                        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                 "<%s>: Ignoring MPE_SF_EVENT_LAST_SECTION_FOUND for filter %p in state %s - moving to TIMEOUT state\n",
                                 __func__ , filter, filter_SharedFilterStateString[filter->state] );

                        // We're done with the mpeos filter - it's reached a terminal state
                        filter_releaseMpeosFilter(filter);

                        filter->state = FILTER_SHARED_FILTER_STATE_TIMED_OUT;
                        // Another filter should have already been activated, if
                        //  desired. Nothing else to do here
                    }
                    // Note: Filter will be released below
                    break;
                }
                case FILTER_SHARED_FILTER_STATE_READY:
                case FILTER_SHARED_FILTER_STATE_DELETED:
                case FILTER_SHARED_FILTER_STATE_MATCHED:
                case FILTER_SHARED_FILTER_STATE_CANCELLED:
                case FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK:
                {
                    MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                             "<%s>: MPE_SF_EVENT_SECTION_FOUND not expected for filter %p - ignoring\n",
                             __func__, filter );
                    filter_dumpSharedFilter(MPE_LOG_INFO, __func__, filter);
                    break;
                }
                default:
                {
                    MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                             "<%s>: found invalid state for filter %p when processing section found event %d - ignoring\n",
                             __func__, filter, eventId );
                    filter_dumpSharedFilter(MPE_LOG_INFO, __func__, filter);
                    break;
                }
            } // END switch (filter->state)

            // SECTION FOUND CLEANUP

            // Release our handle on the section data if we have one
            if (sectionHandle != MPE_SF_INVALID_SECTION_HANDLE)
            {
                MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                         "<%s>: Releasing section %p\n",
                         __func__, sectionHandle );
                retCode = mpeos_filterSectionRelease(sectionHandle);
                if (retCode != MPE_SUCCESS)
                {
                    MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                             "<%s>: MPE error %d (0x%08x) releasing section %p\n",
                             __func__, retCode, retCode,
                             sectionHandle );
                    // Keep on trucking in this case...
                }
                sectionHandle = MPE_SF_INVALID_SECTION_HANDLE;
            }
            break;
        } // END case MPE_SF_EVENT_SECTION_FOUND/MPE_SF_EVENT_LAST_SECTION_FOUND
        case MPE_SF_EVENT_FILTER_CANCELLED:
        case MPE_SF_EVENT_FILTER_PREEMPTED:
        case MPE_SF_EVENT_OUT_OF_MEMORY:
        case MPE_SF_EVENT_SOURCE_CLOSED:
        case FILTER_GROUP_THREAD_EVENT_SET_FAILED:
        {
            //
            // PLATFORM-CANCELLED OR USER-CANCELLED FILTER
            //
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                     "<%s>: Processing filter CANCEL event (eventId %d/0x%x) for filter ID %d (0x%x)\n",
                     __func__ , eventId, eventId, filterId, filterId );

            if (g_filterLookupFiltersByFilterID)
            {
                filter = filter_filterForID(filterId);
            }
            else
            {
                // State logic should ensure that this is always a safe cast
                filter = (filter_SharedFilter *)eventData2;
            }

            if (filter == NULL)
            {
                MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                         "<%s>: INVALID filter for event %d (0x%d) (filterID %d (0x%x), eventData2 %p)!\n",
                         __func__, eventId, eventId,
                         filterId, filterId, eventData2 );
                return;
            }

            if (filter->filterId != filterId)
            {
                MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                         "<%s>: Event %d (0x%x) FilterID %d (0x%x) doesn't match filter's FilterID %d (0x%x)\n",
                         __func__, eventId, eventId, filterId, filterId,
                         filter->filterId, filter->filterId );
                filter_dumpSharedFilter(MPE_LOG_WARN, __func__, filter);
            }

            filterGroup = filter->group;

            // We're done with the mpeos filter - it's reached a terminal state
            filter_releaseMpeosFilter(filter);

            switch (filter->state)
            {
                case FILTER_SHARED_FILTER_STATE_SET:
                case FILTER_SHARED_FILTER_STATE_SET_FAILED:
                { // Indicates a platform-canceled filter or a failed set
                  //  (we wouldn't be in this state if we called
                  //  mpeos_filterCancelFilter())
                    if (filterGroup->state == FILTER_GROUP_STATE_RUNNING)
                    {
                        filterGroup->state = FILTER_GROUP_STATE_PAUSED;
                    }

                    filter->state = FILTER_SHARED_FILTER_STATE_CANCELLED;
                    filterGroup->activeFilter = NULL;
                    filterGroup->activePriority = 0;
                    filterGroup->numSetOrReady--;

                    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                             "<%s>: Invoking MPE_FILTERGROUP_EVENT_CANCELLED callback to function %p: filter %p, userpointer %p, userdata 0x%08x\n",
                             __func__, filter->callbackFunc, filter,
                             filter->userPointer, filter->userData );

                    filter->inCallback = TRUE;

                    // Don't hold the lock during the callback - to prevent lock inversions
                    mpe_mutexRelease(g_filterGroupDBMutex);

                    // Perform callback
                    (filter->callbackFunc)( (mpe_SharedFilter) filter,
                                            (mpe_FilterGroup) filterGroup,
                                            filter->userPointer,
                                            filter->userData,
                                            MPE_FILTERGROUP_EVENT_CANCELLED,
                                            0, NULL, FALSE );

                    mpe_mutexAcquire(g_filterGroupDBMutex);

                    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                             "<%s>: MPE_FILTERGROUP_EVENT_CANCELLED callback returned\n",
                             __func__ );

                    // NOTE: DB MAY HAVE BEEN CHANGED AND/OR ACTIVATED IN THE CALLBACK
                    filter->inCallback = FALSE;

                    if (filter->state == FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK)
                    {
                        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                 "<%s>: Performing callback-delayed filter release for filter %p\n",
                                 __func__ , filter );
                        filter_unlinkFilterFromGroup(filter);

                        if (filterGroup->state == FILTER_GROUP_STATE_DELETE_PENDING)
                        { // And a group that's waiting for its one filter's latent death...
                            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                     "<%s>: Performing callback-delayed filter group release for group %p\n",
                                     __func__ , filterGroup );
                            filter_unlinkFilterGroup(filterGroup);
                            filter_releaseFilterGroup(filterGroup);
                            filterGroup = NULL;
                        }
                        filter_releaseSharedFilter(filter);
                        filter = NULL;
                    }
                    break;
                }
                case FILTER_SHARED_FILTER_STATE_READY_PENDING:
                { // The filter was canceled for time-slicing - make it READY
                  //  now that it's async CANCEL has arrived
                    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                             "<%s>: Filter cancel acknowledged for filter %p in state %s - moving to READY state\n",
                             __func__ , filter, filter_SharedFilterStateString[filter->state] );

                    filter->state = FILTER_SHARED_FILTER_STATE_READY;
                    filterGroup->numSetOrReady++;
                    // Another filter should have already been activated, if
                    //  desired. Nothing else to do here
                    break;
                }
                case FILTER_SHARED_FILTER_STATE_TIMEOUT_PENDING:
                { // The filter was canceled for time-slicing - make it READY
                  //  now that it's async CANCEL has arrived
                    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                             "<%s>: Filter cancel acknowledged for filter %p in state %s - moving to TIMEOUT state\n",
                             __func__ , filter, filter_SharedFilterStateString[filter->state] );

                    filter->state = FILTER_SHARED_FILTER_STATE_TIMED_OUT;
                    // Another filter should have already been activated, if
                    //  desired. Nothing else to do here
                    break;
                }
                case FILTER_SHARED_FILTER_STATE_REMOVE_PENDING:
                { // The filter was released by the owner, this is its latent death
                    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                             "<%s>: Handling delayed filter release for filter %p\n",
                             __func__ , filter );

                    if (filter->inCallback)
                    {
                        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                 "<%s>: Filter delete while in callback - changing filter %p state to %s\n",
                                 __func__ , filter,
                                 filter_SharedFilterStateString[FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK] );
                        filter->state = FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK;
                    }
                    else
                    {
                        MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                 "<%s>: Not in callback - releasing filter %p\n",
                                 __func__ , filter );
                        filter_unlinkFilterFromGroup(filter);

                        if (filterGroup->state == FILTER_GROUP_STATE_DELETE_PENDING)
                        { // And a group that's waiting for its one filter's latent death...
                            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                                     "<%s>: Performing delayed filter group release for group %p\n",
                                     __func__ , filterGroup );
                            filter_unlinkFilterGroup(filterGroup);
                            filter_releaseFilterGroup(filterGroup);
                        }
                        filter_releaseSharedFilter(filter);
                        filter = NULL;
                    } // END else/if (filter->inCallback)
                    break;
                }
                case FILTER_SHARED_FILTER_STATE_READY:
                case FILTER_SHARED_FILTER_STATE_DELETED:
                case FILTER_SHARED_FILTER_STATE_MATCHED:
                case FILTER_SHARED_FILTER_STATE_CANCELLED:
                case FILTER_SHARED_FILTER_STATE_DELETED_DURING_CALLBACK:
                { // Filters in these states shouldn't have SET filters
                    MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                             "<%s>: filter cancel event %d not expected for filter %p in state %s - ignoring\n",
                             __func__, eventId, filter,
                             filter_SharedFilterStateString[filter->state] );
                    filter_dumpSharedFilter(MPE_LOG_WARN, __func__, filter);
                    break;
                }
                default:
                {
                    MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                             "<%s>: found invalid state for filter %p when processing cancel event %d - ignoring\n",
                             __func__, filter, eventId );
                    filter_dumpSharedFilter(MPE_LOG_WARN, __func__, filter);
                    break;
                }
            } // END switch (filter->state)

            break;
        } // END case Filter cancelled spontaneously
        case MPE_SF_EVENT_FILTER_AVAILABLE:
        {
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                     "<%s>: Processing MPE_SF_EVENT_FILTER_AVAILABLE\n",
                     __func__ );
            break;
        } // END case MPE_SF_EVENT_FILTER_AVAILABLE
        default:
        {
            MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
                     "<%s>: Ignoring UNKNOWN eventId 0x%08x\n",
                     __func__, eventId );
            break;
        } // END default
    } // END switch (eventId)

    MPE_LOG( MPE_LOG_TRACE1, MPE_MOD_FILTER,
             "%s(eventId 0x%x,"
             "eventData1 %p,eventData2 %p,eventData3 0x%08x) returning\n",
             __func__, eventId, eventData1, eventData2, eventData3 );
    return;
} // END filter_processFilterGroupThreadEvent()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

filter_SharedFilter * filter_findHighestPriorityReadyFilterInGroup(
                         const filter_FilterGroup * targetGroup )
{
    // Assert: Filter DB mutex is owned by the caller
    filter_SharedFilter *startFilter, *foundFilter, *curFilter;
    uint8_t highestPriority;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p)\n",
                                            __func__, targetGroup );

    foundFilter = NULL;
    highestPriority = 0;

    if (targetGroup->activeFilter)
    {
        startFilter = targetGroup->activeFilter;
    }
    else
    {
        startFilter = targetGroup->firstFilter;
    }

    if (startFilter != NULL)
    {
        curFilter = startFilter;
        do
        {
            if ( (curFilter->state == FILTER_SHARED_FILTER_STATE_READY)
                 && (curFilter->priority > highestPriority) )
            {
                foundFilter = curFilter;
                highestPriority = foundFilter->priority;
            }
            curFilter = curFilter->next;
        } while (curFilter != startFilter);
    } // END if (targetGroup->firstFilter != NULL)

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p) returning filter %p\n",
                                            __func__, targetGroup,
                                            foundFilter );

    return foundFilter;
} // END filter_findHighestPriorityReadyFilterInGroup()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

filter_SharedFilter * filter_findNextReadyFilterAtPriority(
                                    filter_FilterGroup * const targetGroup )
{
    // Assert: Filter DB mutex is owned by the caller
    filter_SharedFilter *curFilter;
    filter_SharedFilter *lastFilter;
    filter_SharedFilter *nextReadyFilter = NULL;
    uint8_t priority = targetGroup->activePriority;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p)\n",
                                            __func__, targetGroup );

    // Start at the filter immediately after target and stop when we
    //  find a match or come full circle...
    curFilter = targetGroup->activeFilter->next;
    lastFilter = curFilter;

    // Walk until we loop around the double-linked-list
    do
    {
        if ( (curFilter->priority == priority)
             && (curFilter->state == FILTER_SHARED_FILTER_STATE_READY) )
        {
            nextReadyFilter = curFilter;
        }
        curFilter = curFilter->next;
    } while (!nextReadyFilter && (curFilter != lastFilter));

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p) returning filter %p\n",
                                            __func__, targetGroup, curFilter );
    return nextReadyFilter;
} // END filter_findNextReadyFilterAtPriority()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Bool filter_isFilterInGroup( const filter_SharedFilter * const targetFilter,
                                 const filter_FilterGroup * const targetGroup )
{
    // Assert: Filter DB mutex is owned by the caller
    filter_SharedFilter *curFilter;
    filter_SharedFilter *lastFilter;
    mpe_Bool found = FALSE;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(filter %p, fg %p)\n",
                                            __func__, targetFilter,
                                            targetGroup );

    // Start at the filter immediately after target and stop when we
    //  find a match or come full circle...
    curFilter = targetGroup->activeFilter;
    lastFilter = curFilter;

    // Walk until we loop around the double-linked-list
    do
    {
        if (curFilter == targetFilter)
        {
            found = TRUE;
        }
        curFilter = curFilter->next;
    } while (!found && (curFilter != lastFilter));

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(filter%p, fg %p) returning %s\n",
                                            __func__, targetFilter,
                                            targetGroup,
                                            found ? "TRUE" : "FALSE" );
    return found;
} // END filter_isFilterInGroup()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_setMpeosFilter(filter_SharedFilter * const targetFilter)
{
    // Assert: Filter DB mutex is owned by the caller
    mpe_Error retCode;
    mpe_TimeMillis curTimeMs, timeoutTimeForFilterMs;

    MPE_LOG( MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(filter %p)\n",
                                            __func__, targetFilter );

    if (targetFilter->group->activeFilter != NULL)
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: a filter is already active on the filter's group\n",
                 __func__ );
        filter_dumpFilterGroup(MPE_LOG_WARN, __func__, targetFilter->group);
        return MPE_EINVAL;
    }

    retCode = mpeos_filterSetFilter( &targetFilter->filterSource,
                                     &targetFilter->filterSpec,
                                     g_filterGroupThreadQueue,
                                     targetFilter,
                                     targetFilter->priority,
                                     targetFilter->remainingMatches,
                                     0,
                                     &targetFilter->filterId );
    if (retCode != MPE_SUCCESS)
    {
        mpe_Error retCode2;

        MPE_LOG( ( ( retCode == MPE_SF_ERROR_TUNER_NOT_AT_FREQUENCY
                     || retCode == MPE_SF_ERROR_TUNER_NOT_TUNED)
                   ? MPE_LOG_DEBUG : MPE_LOG_WARN),
                 MPE_MOD_FILTER, "<%s>: Error %d (0x%x) setting filter %p\n",
                                               __func__, retCode, retCode,
                                               targetFilter );
        filter_dumpSharedFilter(MPE_LOG_INFO, __func__, targetFilter);
        targetFilter->state = FILTER_SHARED_FILTER_STATE_SET_FAILED;

        // Insert event into the queue
        retCode2 = mpe_eventQueueSend( g_filterGroupThreadQueue,
                                      FILTER_GROUP_THREAD_EVENT_SET_FAILED,
                                      0, targetFilter, 0 );
        if (retCode2 != MPE_SUCCESS)
        {
            MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error sending SET_FAILED event to filter group thread queue %p (error %d)\n",
                                                   __func__,
                                                   g_filterGroupThreadQueue,
                                                   retCode2 );
            return retCode2;
        }
        return retCode;
    }

    retCode = mpeos_timeGetMillis(&curTimeMs);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: mpeos_timeGetMillis failure (returned %d)\n",
                 __func__, retCode );
        return retCode;
    }

    targetFilter->state = FILTER_SHARED_FILTER_STATE_SET;
    targetFilter->group->activeFilter = targetFilter;
    targetFilter->group->activePriority = targetFilter->priority;
    targetFilter->lastSetTimeMs = curTimeMs;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: SET filter %p (group: %s):\n",
                                           __func__, targetFilter, targetFilter->group->name );
    filter_dumpSharedFilterSource(MPE_LOG_DEBUG, __func__, targetFilter);
    filter_dumpSharedFilter(MPE_LOG_DEBUG, __func__, targetFilter);

    if (targetFilter->timeoutMs > 0)
    { // Filter has a timeout set
        timeoutTimeForFilterMs = curTimeMs + targetFilter->remainingTimeoutMs;

        if ( timeoutTimeForFilterMs < g_filter_nextThreadWakeupTimeMs )
        { // Need to wakeup the thread so it can change its wait timeout
            MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Timeout time of activated filter %p (%llums from now) is earlier than current thread wait (%llums from now) - WAKING UP event thread...\n",
                                                   __func__, targetFilter,
                                                   timeoutTimeForFilterMs-curTimeMs,
                                                   g_filter_nextThreadWakeupTimeMs-curTimeMs );

            retCode = mpe_eventQueueSend( g_filterGroupThreadQueue,
                                          FILTER_GROUP_THREAD_EVENT_WAKEUP,
                                          NULL, NULL, 0 );
            if (retCode != MPE_SUCCESS)
            {
                MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Error sending WAKEUP event to filter group thread queue %p (error %d)\n",
                                                       __func__,
                                                       g_filterGroupThreadQueue,
                                                       retCode );
            }
        }
    } // END if (targetFilter->timeoutMs > 0)

    return retCode;
} // END filter_setMpeosFilter()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_cancelMpeosFilter( filter_FilterGroup * const targetGroup,
                                    const filter_SharedFilterState newState )
{
    // Assert: Filter DB mutex is owned by the caller
    mpe_Error retCode;

    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(filtergroup %p, newState %s)\n",
                                            __func__, targetGroup,
                                            filter_SharedFilterStateString[newState] );

    if (targetGroup == NULL)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: NULL filter group passed\n",
                                               __func__ );

        return MPE_EINVAL;
    }

    if (targetGroup->activeFilter == NULL)
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: a filter is not active on the filter's group\n",
                 __func__ );
        filter_dumpFilterGroup(MPE_LOG_WARN, __func__, targetGroup);
        return MPE_EINVAL;
    }

    retCode = mpeos_filterCancelFilter(targetGroup->activeFilter->filterId);
    // Cancel is async - so don't flatten the filterId (yet)

    targetGroup->activeFilter->state = newState;

    MPE_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "<%s>: Deactivated filter %p\n",
                                           __func__, targetGroup->activeFilter );
    filter_dumpSharedFilter(MPE_LOG_DEBUG, __func__, targetGroup->activeFilter);

    targetGroup->activeFilter = NULL;
    targetGroup->activePriority = 0;
    targetGroup->numSetOrReady--;

    // Note: We don't need to poke the group thread here. If we deactivated
    //       a filter that was next to timeout, the thread may wakeup and
    //       realize there's nothing to do - which is exactly the same work
    //       as if we'd poked it. So just leave it alone...

    return retCode;
} // END filter_cancelMpeosFilter()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Error filter_releaseMpeosFilter(filter_SharedFilter * const targetFilter)
{
    // Assert: Filter DB mutex is owned by the caller
    mpe_Error retCode;

    MPE_LOG( MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(filter %p)\n",
                                            __func__, targetFilter );

    if (targetFilter->filterId == 0)
    {
        MPE_LOG(MPE_LOG_WARN, MPE_MOD_FILTER, "<%s>: Filter %p doesn't have an MPEOS filter!\n",
                                               __func__, targetFilter->filterId );
        filter_dumpSharedFilter(MPE_LOG_WARN, __func__, targetFilter);
        return MPE_EINVAL;
    }

    MPE_LOG( MPE_LOG_DEBUG, MPE_MOD_FILTER,
             "<%s>: Releasing filter %d (0x%08x)\n",
             __func__, targetFilter->filterId, targetFilter->filterId );

    retCode = mpeos_filterRelease(targetFilter->filterId);
    if (retCode != MPE_SUCCESS)
    {
        MPE_LOG( MPE_LOG_WARN, MPE_MOD_FILTER,
                 "<%s>: MPE error %d (0x%08x) releasing filter %d (0x%08x)\n",
                 __func__, retCode, retCode,
                 targetFilter->filterId, targetFilter->filterId );
        filter_dumpSharedFilter(MPE_LOG_WARN, __func__, targetFilter);
    }
    targetFilter->filterId = 0;

    return retCode;
} // END filter_releaseMpeosFilter()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Bool filter_filterGroupIsValid(filter_FilterGroup * const targetGroup)
{
    // Assert: Filter DB mutex is owned by the caller
    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(fg %p)\n",
                                            __func__, targetGroup );
    if (targetGroup == NULL)
    {
        return FALSE;
    }

    if (g_filterValidateFilterPointers)
    {
        filter_FilterGroup * curGroup = g_filterGroupHead;
        while (curGroup != NULL)
        {
            if (curGroup == targetGroup)
            {
                return TRUE;
            }
            curGroup = curGroup->next;
        } // END while (curGroup != NULL)

        // Got through the list without finding the group
        return FALSE;
    }
    else
    {
        return TRUE;
    }
} // END filter_filterGroupIsValid()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

mpe_Bool filter_filterIsValid(filter_SharedFilter * const targetFilter)
{
    // Assert: Filter DB mutex is owned by the caller
    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(f %p)\n",
                                            __func__, targetFilter );
    if (targetFilter == NULL)
    {
        return FALSE;
    }

    if (g_filterValidateFilterPointers)
    {
        filter_FilterGroup * curGroup = g_filterGroupHead;
        while (curGroup != NULL)
        {
            filter_SharedFilter * firstFilter = curGroup->firstFilter;

            if (firstFilter != NULL)
            {
                filter_SharedFilter * curFilter = firstFilter;
                do
                {
                    if (curFilter == targetFilter)
                    {
                        return TRUE;
                    }
                    curFilter = curFilter->next;
                } while (curFilter != firstFilter);
            }
            curGroup = curGroup->next;
        } // END while (curGroup != NULL)

        // Got through the list without finding the filter
        return FALSE;
    } // END if (g_filterValidateFilterPointers)
    else
    {
        return TRUE;
    }
} // END filter_filterIsValid()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

filter_SharedFilter * filter_filterForID(uint32_t id)
{
    // Assert: Filter DB mutex is owned by the caller
    MPE_LOG(MPE_LOG_TRACE1, MPE_MOD_FILTER, "%s(id %d (0x%x))\n",
                                            __func__, id, id );
    filter_FilterGroup * curGroup = g_filterGroupHead;
    while (curGroup != NULL)
    {
        filter_SharedFilter * firstFilter = curGroup->firstFilter;

        if (firstFilter != NULL)
        {
            filter_SharedFilter * curFilter = firstFilter;
            do
            {
                if (curFilter->filterId == id)
                {
                    return curFilter;
                }
                curFilter = curFilter->next;
            } while (curFilter != firstFilter);
        }
        curGroup = curGroup->next;
    } // END while (curGroup != NULL)

    // Got through the list without finding the filter
    return NULL;
} // END filter_filterForID()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

void filter_dumpFilterGroupDB(const int level, const char * func)
{
    // Assert: Filter DB mutex is owned by the caller
#if defined(MPE_FEATURE_DEBUG)

    filter_FilterGroup * curGroup;
    uint32_t groupCount = 0;

    MPE_LOG(level, MPE_MOD_FILTER, "<%s>: FilterGroup DB:\n", func );

    curGroup = g_filterGroupHead;
    while (curGroup != NULL)
    {
        filter_dumpFilterGroup(level, func, curGroup);
        curGroup = curGroup->next;
        groupCount++;
    } // END while (curGroup != NULL)

    MPE_LOG(level, MPE_MOD_FILTER, "<%s>: DONE (%d groups).\n",
                                   func, groupCount );

#endif
} // END filter_dumpFiterGroupDB()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

void filter_dumpFilterGroup( const int level,
                             const char * func,
                             const filter_FilterGroup * targetGroup )
{
    // Assert: Filter DB mutex is owned by the caller
#if defined(MPE_FEATURE_DEBUG)
    mpe_TimeMillis curTimeMs;

    mpeos_timeGetMillis(&curTimeMs);

    MPE_LOG( level, MPE_MOD_FILTER, "<%s>:   FilterGroup %p: \"%s\"\n",
                                    func, targetGroup,targetGroup->name );
    MPE_LOG( level, MPE_MOD_FILTER, "<%s>:     state %s, interval %dms (%dms remaining)\n",
                                    func,
                                    filter_FilterGroupStateString[targetGroup->state],
                                    targetGroup->intervalMs,
                                    targetGroup->remainingIntervalMs );
    MPE_LOG( level, MPE_MOD_FILTER, "<%s>:     activePriority %d, activeFilter %p, numSetOrReady %d\n",
                                    func,
                                    targetGroup->activePriority,
                                    targetGroup->activeFilter,
                                    targetGroup->numSetOrReady );
    MPE_LOG( level, MPE_MOD_FILTER, "<%s>:     lastStartedTimeMs %llums (%llums ago)\n",
                                    func,
                                    targetGroup->lastStartedTimeMs,
                                    targetGroup->lastStartedTimeMs
                                        ? (curTimeMs - targetGroup->lastStartedTimeMs)
                                        : 0 );

    if (targetGroup->firstFilter != NULL)
    {
        uint32_t filterCount = 0;
        filter_SharedFilter * curFilter = targetGroup->firstFilter;

        MPE_LOG( level, MPE_MOD_FILTER, "<%s>:     Filter list:\n",
                                        func );
        do
        {
            filter_dumpSharedFilter(level, func, curFilter);
            curFilter = curFilter->next;
            filterCount++;
        } while (curFilter != targetGroup->firstFilter);
        MPE_LOG( level, MPE_MOD_FILTER, "<%s>:     End FilterGroup %p (%d shared filters)\n",
                                        func,targetGroup, filterCount );
    }
    else
    {
        MPE_LOG( level, MPE_MOD_FILTER, "<%s>:     EMPTY GROUP.\n",
                                        func );
    }
#endif
} // END filter_dumpFilterGroup()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

void filter_dumpSharedFilter( const int level,
                              const char * func,
                              const filter_SharedFilter * targetFilter )
{
    // Assert: Filter DB mutex is owned by the caller
#if defined(MPE_FEATURE_DEBUG)

    char scratch[512];
    uint32_t i;
    mpe_TimeMillis curTimeMs;

    mpeos_timeGetMillis(&curTimeMs);

    MPE_LOG(level, MPE_MOD_FILTER, "<%s>:     SharedFilter %p:\n",
                                   func, targetFilter );

    // Print filter source
    filter_dumpSharedFilterSource(level, func, targetFilter);

    // Print positive part of filter
    for ( i=0;
          ( (i < targetFilter->filterSpec.pos.length)
            && (i*6 < sizeof(scratch) ) );
          i++ )
    {
        sprintf( scratch+(i*6), "%02hx&%02hx ",
                                targetFilter->filterSpec.pos.vals[i],
                                targetFilter->filterSpec.pos.mask[i] );
    }
    scratch[i*6] = '\0';
    MPE_LOG( level, MPE_MOD_FILTER, "<%s>:         pos comp: %s\n",
                                    func,
                                    targetFilter->filterSpec.pos.length ? scratch : "none" );

    // Print negative part of filter
    for ( i=0;
          ( (i < targetFilter->filterSpec.neg.length)
            && (i*6 < sizeof(scratch) ) );
          i++ )
    {
        sprintf( scratch+(i*6), "%02hx&%02hx ",
                                targetFilter->filterSpec.neg.vals[i],
                                targetFilter->filterSpec.neg.mask[i] );
    }
    scratch[i*6] = '\0';
    MPE_LOG( level, MPE_MOD_FILTER, "<%s>:         neg comp: %s\n",
             func, targetFilter->filterSpec.neg.length ? scratch : "none" );

    // Print everything else...
    MPE_LOG(level, MPE_MOD_FILTER, "<%s>:         state %s%s, priority %d, timeout %dms (%dms remaining)\n",
                                   func,
                                   filter_SharedFilterStateString[targetFilter->state],
                                   targetFilter->inCallback ? " (in CB)" : "",
                                   targetFilter->priority,
                                   targetFilter->timeoutMs,
                                   targetFilter->remainingTimeoutMs );
    MPE_LOG(level, MPE_MOD_FILTER, "<%s>:         filterId %d (0x%x), lastSetTimeMs %llums (%llums ago)\n",
                                   func,
                                   targetFilter->filterId,
                                   targetFilter->filterId,
                                   targetFilter->lastSetTimeMs,
                                   (targetFilter->lastSetTimeMs > 0)
                                     ? (curTimeMs - targetFilter->lastSetTimeMs)
                                     : 0 );
    MPE_LOG( level, MPE_MOD_FILTER, "<%s>:         match count: %d (%d remaining)\n",
             func, targetFilter->matchCount, targetFilter->remainingMatches );
    MPE_LOG( level, MPE_MOD_FILTER, "<%s>:         callback: %p (userPointer %p, userData 0x%08x)\n",
             func, targetFilter->callbackFunc, targetFilter->userPointer, targetFilter->userData );
#endif
} // END filter_dumpSharedFilter()

///////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////

void filter_dumpSharedFilterSource( const int level,
                                    const char * func,
                                    const filter_SharedFilter * targetFilter )
{
    // Assert: Filter DB mutex is owned by the caller
#if defined(MPE_FEATURE_DEBUG)
    switch (targetFilter->filterSource.sourceType)
    {
        case MPE_FILTER_SOURCE_OOB:
        {
            MPE_LOG(level, MPE_MOD_FILTER, "<%s>:         source: type OOB, PID %d (0x%x)\n",
                                           func, targetFilter->filterSource.pid,
                                           targetFilter->filterSource.pid );
            break;
        }
        case MPE_FILTER_SOURCE_INB:
        {
            MPE_LOG(level, MPE_MOD_FILTER, "<%s>:         source: type INB, PID %d (0x%x), tuner %d, freq %dHz\n",
                                           func,
                                           targetFilter->filterSource.pid,
                                           targetFilter->filterSource.pid,
                                           targetFilter->filterSource.parm.p_INB.tunerId,
                                           targetFilter->filterSource.parm.p_INB.freq );
            break;
        }
        case MPE_FILTER_SOURCE_DSG_APPID:
        {
            MPE_LOG(level, MPE_MOD_FILTER, "<%s>:         source: type DSG, PID %d (0x%x), AppID %d (0x%x)\n",
                                           func,
                                           targetFilter->filterSource.pid,
                                           targetFilter->filterSource.pid,
                                           targetFilter->filterSource.parm.p_DSGA.appId,
                                           targetFilter->filterSource.parm.p_DSGA.appId );
            break;
        }
        case MPE_FILTER_SOURCE_HN_STREAM:
        {
            MPE_LOG(level, MPE_MOD_FILTER, "<%s>:         source: type HNS, PID %d (0x%x), StreamSession %d (0x%x)\n",
                                           func,
                                           targetFilter->filterSource.pid,
                                           targetFilter->filterSource.pid,
                                           targetFilter->filterSource.parm.p_HNS.hn_stream_session,
                                           targetFilter->filterSource.parm.p_HNS.hn_stream_session );
            break;
        }
        default:
        {
            MPE_LOG(level, MPE_MOD_FILTER, "<%s>:         source: type UNKNOWN\n",
                                           func );
            break;
        }
    } // END switch (targetFilter->filterSource.sourceType)
#endif
} // END filter_dumpSharedFilterSource()

/*****************************************************************************
 END OF FILE
 *****************************************************************************/
