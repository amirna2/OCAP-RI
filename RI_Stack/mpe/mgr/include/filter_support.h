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
#ifndef _FILTER_SUPPORT_H_
#define _FILTER_SUPPORT_H_

#include "mpe_types.h"
#include "mpe_error.h"
#include "mpeos_filter.h"	

#ifdef __cplusplus
extern "C"
{
#endif

mpe_Error filter_zeroSpec(mpe_FilterSpec *pFilter);
mpe_Error filter_createFilterComps(uint8_t posFilterLen, uint8_t negFilterLen,
        mpe_FilterSpec *pFilter);
mpe_Error filter_destroyFilterComps(mpe_FilterSpec *pFilter);
mpe_Error filter_createFilterSpec(uint8_t posFilterLen, uint8_t negFilterLen,
        mpe_FilterSpec **ppFilter);
mpe_Error filter_destroyFilterSpec(mpe_FilterSpec *pFilter);
mpe_Error filter_alterSpec(mpe_FilterSpec *pFilter, mpe_FilterField filtField,
        uint32_t filterVal, mpe_FieldPolarity posNeg);
mpe_Error filter_setSpec(mpe_FilterSpec *pFilter, uint32_t tableId,
        mpe_FieldPolarity matchTableIdExt, uint32_t tableIdExt,
        mpe_FieldPolarity matchVer, uint32_t version,
        mpe_FieldPolarity matchSect, uint32_t section);

///////////////////////////////////////////////////////////
// Filter group functions
///////////////////////////////////////////////////////////

/** Datatype definitions for the filter group functions */

typedef struct
{
    int unused1;
} *mpe_FilterGroup;

typedef struct
{
    int unused1;
} *mpe_SharedFilter;

typedef enum
{
    MPE_FILTERGROUP_EVENT_MATCHED = 1,
    MPE_FILTERGROUP_EVENT_CANCELLED = 2,
    MPE_FILTERGROUP_EVENT_TIMEOUT = 3
} mpe_SharedFilterEvent;

typedef enum
{
    MPE_FILTERGROUP_FILTERSTATE_INVALID = 0,
    MPE_FILTERGROUP_FILTERSTATE_MATCHED = 1, // Filter has been fully satisfied (timesToMatch sections)
    MPE_FILTERGROUP_FILTERSTATE_CANCELLED = 2, // Filter was cancelled by the platform
    MPE_FILTERGROUP_FILTERSTATE_TIMEDOUT = 3, // Filter timed out
    MPE_FILTERGROUP_FILTERSTATE_SET = 4, // Filter is set/active
    MPE_FILTERGROUP_FILTERSTATE_DELETED = 5 // Filter was deleted/removed (via filter_removeSharedFilter)
} mpe_SharedFilterState;

#define MPE_FILTERGROUP_INVALID_FILTER (NULL)

#define MPE_FILTERGROUP_INVALID_GROUP (NULL)

/**
 * For starting up the filter group system
 */
mpe_Error filter_filterGroupStartup(void);

/**
 * For shutting down the filter group system
 */
void filter_filterGroupShutdown(void);

/**
 * Create an MPE Filter Group using prioritized time-division multiplex.
 * A MPE Filter Group is a collection of filters which share a number of
 * MPEOS native filters.
 *
 * This method will share the native filter according
 * to priority. When more than one Shared Filter is set at the same priority,
 * and that priority is the highest in the group,
 * time-division multiplexing will be used to periodically set the
 * native/mpeos filter.
 *
 * A filter group is suspended when it is created. startFilterGroup()
 * will initiate the native/mpeos filtering.
 *
 * @param interval         The frequency (in milliseconds) to perform
 *                         time-division multiplexing (when necessary).
 * @param name             Human-readable name for the filter group
 * @param filterGroup      Pointer to store the handle for the newly-created
 *                         Filter Group.
 * @return If the group is created successfully, <i>createFilterGroup()</i>
 *         returns <i>MPE_SUCCESS</i>. Otherwise, it will return one of the
 *         following error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            indicates at least one input parameter has an invalid value.
 *         <i>MPE_ENOMEM</i>
 *            indicates insufficient memory.
 */
mpe_Error filter_createFilterGroup( uint32_t interval,
                                    char * name,
                                    mpe_FilterGroup * filterGroup );

/**
 * Start the MPE Filter Group. If at least 1 Shared Filter is in the
 * group at the time startFilterGroup is called(), a native (mpeos)
 * filter will be set immediately.
 *
 * @param filterGroup      Handle for the Filter Group to start.
 *
 * @return If the group is started successfully or was already started,
 *         <i>startFilterGroup</i> returns <i>MPE_SUCCESS</i>. Otherwise,
 *         it will return one of the following error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            indicates the filter group is invalid
 */
mpe_Error filter_startFilterGroup(mpe_FilterGroup filterGroup);

/**
 * Pause the MPE Filter Group. Any native filters remain set
 * but no switching is performed until a call to <i>startFilterGroup()</i>.
 * If a filter is matched or canceled, a new native filter will not be set.
 *
 * @param filterGroup      Handle for the Filter Group to suspend.
 *
 * @return If the group was paused successfully or was already paused,
 *         <i>pauseFilterGroup</i> returns <i>MPE_SUCCESS</i>. Otherwise,
 *         it will return one of the following error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            indicates the filter group is invalid
 */
mpe_Error filter_pauseFilterGroup(mpe_FilterGroup filterGroup);

/**
 * Stop the MPE Filter Group. All native filtering is suspended until
 * a call to <i>startFilterGroup()</i>. If any native/mpeos filter(s) are
 * set, they will be canceled immediately.
 *
 * @param filterGroup      Handle for the Filter Group to suspend.
 *
 * @return If the group was stopped successfully or was already stopped,
 *         <i>stopFilterGroup</i> returns <i>MPE_SUCCESS</i>. Otherwise,
 *         it will return one of the following error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            indicates the filter group is invalid
 */
mpe_Error filter_stopFilterGroup(mpe_FilterGroup filterGroup);

/**
 * Destroy the MPE Filter Group. Any set native/mpeos filter(s) are
 * canceled immediately. Callbacks for any shared filters in the group
 * will be invoked with event type MPE_FILTERGROUP_EVENT_CANCELLED.
 *
 * @param filterGroup      Handle for the Filter Group to suspend.
 *
 * @return If the group is destroyed successfully, <i>destroyFilterGroup</i>
 *         returns <i>MPE_SUCCESS</i>. Otherwise, it will return one of the
 *         following error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            indicates the filter group is invalid
 */
mpe_Error filter_destroyFilterGroup(mpe_FilterGroup filterGroup);

/**
 * Callback function prototype for methods utilizing a Shared Filter.
 * The callback will inform the Shared Filter client of changes to the
 * shared filter, such as it being canceled, matched, or timing out.
 *
 * The filter group is Paused (no time-switching occurs) before the callback
 * is invoked. So the next filter in the group will not be set until
 * <i>startFilterGroup</i> is called to resume the group.
 *
 * Event descriptions:
 *
 * MPE_FILTERGROUP_EVENT_MATCHED
 *
 *  The Shared Filter was matched. The matched section length and data
 *  (sectionSize/sectionData) can be used within the context of this
 *  callback. If the data is needed outside the callback, the data must
 *  be copied before the function returns.
 *
 * MPE_FILTERGROUP_EVENT_CANCELLED
 *
 *  The Shared Filter was canceled by either the transport stream being
 *  removed or the filter group being destroyed. The Shared Filter is no
 *  longer part of the Filter Group.
 *
 * MPE_FILTERGROUP_EVENT_TIMEOUT
 *
 *  The Shared Filter was canceled after being set for the Shared
 *  Filter's designated timeout interval and not matching. The Shared Filter
 *  is no longer part of the Filter Group.
 *
 * @param sharedFilter  The Shared Filter handle returned from
 *                      addSharedFilter
 * @param filterGroup  The (now Paused) Filter Group that owns the filter
 * @param userPointer The userPointer value supplied corresponding to sharedFilter
 *                  set via addSharedFilter
 * @param userData  The userData value supplied corresponding to sharedFilter
 *                  set via addSharedFilter
 * @param event     The Shared Filter Event being signaled in the callback
 *                  (see above)
 * @param sectionSize For event==MPE_FILTERGROUP_EVENT_MATCHED, the size of
 *                    the matched section
 * @param sectionSize For event==MPE_FILTERGROUP_EVENT_MATCHED, the matched
 *                    section data
 * @param isLast For event==MPE_FILTERGROUP_EVENT_MATCHED, indicates that
 *                    this event represents the last section for the filter
 *                    (timesToMatch has been satisfied)
 */
typedef uint32_t (*mpe_SharedFilterCallback)( mpe_SharedFilter sharedFilter,
                                              mpe_FilterGroup filterGroup,
                                              void * userPointer,
                                              uint32_t userData,
                                              mpe_SharedFilterEvent event,
                                              uint16_t sectionSize,
                                              uint8_t sectionData[],
                                              mpe_Bool isLast );

/**
 * Create a Shared Filter and add it to the designed Filter Group.
 *
 * If the Filter Group is started, a native/mpeos filter may be canceled
 * and/or set immediately, depending upon the group's policy and the
 * criteria of this filter.
 *
 * @param filterSource  See <i>mpeos_filterSetFilter</i> for description
 * @param filterSpec    See <i>mpeos_filterSetFilter</i> for description
 * @param timesToMatch  See <i>mpeos_filterSetFilter</i> for description
 * @param filterPriorty For all Filter Group types, higher values of
 *                      filterPriority indicate higher precedence.
 * @param timeout       The amount of time in milliseconds that the filter
 *                      can be set without being considered timed out.
 * @param userData      User pointer to be provided to the caller when calling
 *                      back the
 * @param userData      User data to be provided to the caller when calling
 *                      back the
 * @param callback      The callback function to be invoked to signal any
 *                      events associated with the Shared Filter.
 * @param sharedFilter  Pointer to store the Shared Filter handle
 *
 * @return If the shared filter is created successfully,
 *         <i>addSharedFilter()</i> returns <i>MPE_SUCCESS</i>. Otherwise,
 *         it will return one of the following error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            indicates at least one input parameter has an invalid value.
 *         <i>MPE_ENOMEM</i>
 *            indicates insufficient memory.
 */
mpe_Error filter_addSharedFilter( mpe_FilterGroup filterGroup,
                                  const mpe_FilterSource * filterSource,
                                  const mpe_FilterSpec * filterSpec,
                                  uint32_t timesToMatch,
                                  uint8_t filterPriority,
                                  uint32_t timeout,
                                  void * userPointer,
                                  uint32_t userData,
                                  mpe_SharedFilterCallback callbackFunc,
                                  mpe_SharedFilter * sharedFilter );

/**
 * Remove a Shared Filter from its Filter Group.
 *
 * If the designated Shared Filter is currently active, the native/mpeos
 * filter is canceled immediately. In all cases, the Shared Filter's callback
 * will be invoked with event type MPE_FILTERGROUP_EVENT_CANCELLED.
 *
 * @return If the shared filter is removed successfully,
 *         <i>removeSharedFilter()</i> returns <i>MPE_SUCCESS</i>. Otherwise,
 *         it will return one of the following error codes:
 *
 *         <i>MPE_EINVAL</i>
 *            indicates the shared filter handle is invalid.
 */
mpe_Error filter_removeSharedFilter( mpe_SharedFilter sharedFilter );

/**
 * Provide the FilterGroup associated with the Shared Filter
 *
 * @return If a non-NULL filter is passed, <i>filter_groupForFilter()</i>
 *         returns <i>MPE_SUCCESS</i> and *filterGroup will contain the
 *         FilterGroup that sharedFilter belongs to.
 *
 *         <i>MPE_EINVAL</i>
 *            indicates sharedFilter is NULL
 */
mpe_Error filter_groupForFilter( mpe_SharedFilter sharedFilter,
                                 mpe_FilterGroup * filterGroup);

/**
 * Return the current state of the Shared Filter
 *
 * @return If a non-NULL filter is passed, <i>filter_getSharedFilterState()</i>
 *         returns <i>MPE_SUCCESS</i> and *filterState will contain the
 *         state of sharedFilter.
 *
 *         <i>MPE_EINVAL</i>
 *            indicates sharedFilter is NULL
 */
mpe_Error filter_getSharedFilterState( mpe_SharedFilter sharedFilter,
                                       mpe_SharedFilterState * filterState);

#ifdef __cplusplus
}
;
#endif

#endif /* _FILTER_SUPPORT_H_ */
