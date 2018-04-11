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

#if !defined(_MPEOS_FILTER_H)
#define _MPEOS_FILTER_H

#ifdef __cplusplus
extern "C"
{
#endif

#include <mpe_types.h>  /* Resolve basic type references. */
#include <mpe_filterevents.h>
#include <mpe_error.h>
#include "mpeos_event.h"
#include "mpeos_sync.h"
#include "mpeos_hn.h"

/**
 * @file mpeos_filter.h
 * 
 * Defines all symbols and functions for the MPEOS Section Filtering API.
 * 
 */

/**
 * Section Filtering option flags
 */

/** Indicates that the associated operation should only be carried out 
 *   if the filter is not in the cancelled state. */
#define MPE_SF_OPTION_IF_NOT_CANCELLED             (0x00000001)

/** Indicates that the associated operation should release the target 
 *    element after performing the operation.  */
#define MPE_SF_OPTION_RELEASE_WHEN_COMPLETE        (0x00000002)

/**
 * Section Filtering error codes
 */
/** Indicates that the designated section handle is invalid */
#define MPE_SF_INVALID_SECTION_HANDLE              (0)
/** Indicates that the designated unique ID is invalid */
#define MPE_SF_INVALID_UNIQUE_ID                   (0)

/**
 * Section Filtering source types
 */
typedef enum mpe_FilterSourceType
{
    MPE_FILTER_SOURCE_OOB = 1, /**< Source is out-of-band/persistent tuner (e.g. ext chan, DSG broadcast tunnel*/
    MPE_FILTER_SOURCE_INB, /**< Source is an already-tuned in-band/adjustable tuner */
    MPE_FILTER_SOURCE_DSG_APPID, /**< Source is a DSG application tunnel managed by section filtering */
    MPE_FILTER_SOURCE_HN_STREAM /**< Source is a HN Stream session. */
} mpe_FilterSourceType;

/**
 * Out-of-band-specific section source parameters.
 * 
 * NOTE: Per SCTE-65, all out-of-band SI sections must be delivered from a single virtual channel
 * source (i.e. all delivered SVCT sections must have the same VCT_ID) 
 */
typedef struct mpe_FilterSource_OOB
{
    uint32_t tsId; /**< Transport stream ID (should always be 1 currently) */
} mpe_FilterSource_OOB;

/**
 * In-band-specific section source parameters
 */
typedef struct mpe_FilterSource_INB
{
    uint32_t tunerId; /**< Tuner identifier (1..n for in-band ) */
    uint32_t freq; /**< Tuner frequency, in Hz (used to validate tuner state) */
    uint32_t tsId; /**< Transport stream ID (should always be 1 currently) */
    uint8_t  ltsid; /**< When non-0, identifies the Local Transport Stream ID carrying the sections */
} mpe_FilterSource_INB;

/**
 * DSG application tunnel-specific section source parameters.
 */
typedef struct mpe_FilterSource_DSG_APPID
{
    uint32_t appId; /**< App ID for DSG application tunnel */
} mpe_FilterSource_DSG_APPID;


/**
 * HN streaming session section source parameters.
 */
typedef struct mpe_FilterSource_HN_STREAM
{
    mpe_HnStreamSession hn_stream_session; /**< session handle for HN streaming */
} mpe_FilterSource_HN_STREAM;

/**
 * Container for all source-type-specific parameters. This is a discriminated 
 * union with mpe_FilterSourceType as the discriminator.
 */
typedef union mpe_FilterSourceParams
{
    mpe_FilterSource_OOB p_OOB; /**< References out-of-band-specific parameters */
    mpe_FilterSource_INB p_INB; /**< References in-band-specific parameters */
    mpe_FilterSource_DSG_APPID p_DSGA; /**< References DSG application tunnel parameters */
    mpe_FilterSource_HN_STREAM p_HNS; /**< References HN streaming session parameters */
} mpe_FilterSourceParams;

/**
 * Container for all common source parameters
 */
typedef struct mpe_FilterSource
{
    mpe_FilterSourceType sourceType; /**< Type of the section source */
    uint32_t pid; /**< PID within the source to filter on */
    mpe_FilterSourceParams parm; /**< Type-specific parameters */
} mpe_FilterSource;

/**
 * Describes a set of data values which can be compared against a target section.
 */
typedef struct mpe_FilterComponent
{
    uint32_t length; /**< Specifies the length, in bytes, of the mask and vals arrays. */
    uint8_t * mask; /**< Pointer to an array of bytes defining the mask to be applied
     *    to the target data. The array of bytes must be at least length bytes. */
    uint8_t * vals; /**< Pointer to an array of bytes defining the values to be
     *    compared against in the target data. The array of bytes must be
     *    at least length bytes. */
} mpe_FilterComponent;

/** Identifies the handle for a section. */
typedef uint32_t mpe_FilterSectionHandle;

/** mpe_FilterSpec describes the terms which define a filter. The filter specification
 *   describes the conditional terms to be evaluated against the target MPEG
 *   section. If the target section positive components and negative components are
 *   considered as arbitrarily-long data words, the logical expression of the filter is:
 *   
 *   <code>
 *   ((sectiondata[0] & pos.mask) == pos.vals)
 *   &&
 *   !((sectiondata[0] & neg.mask) == neg.vals) 
 *   </code> */
typedef struct mpe_FilterSpec
{
    mpe_FilterComponent pos; /**< Positive-match criteria */
    mpe_FilterComponent neg; /**< Negative-match criteria */
} mpe_FilterSpec;

/**
 * <i>mpeos_filterSetFilter()</i> will create a filter and initiate filtering 
 * for a section(s) originating from the section source which match the 
 * designated filter specification. 
 *
 * @param filterSource
 *           pointer to a description of the section source. The 
 *           filter will operate on sections from this source.
 * @param filterSpec
 *           pointer to the filter specification. This defines the 
 *           criteria that the filter will evaluate sections against.
 * @param queueId
 *           designates the queue in which to deliver filter events.
 * @param act
 *           optional asynchronous completion token to be delivered as <i>optionalEventData2</i>
 *           part of event; this would be the ED handle if ED is being used.
 * @param filterPriority 
 *           designates the priority of the filter (1 being the highest)
 * @param timesToMatch 
 *           designates the number of times the filter should match 
 *           before stopping. A value of 0 indicates the filter 
 *           should continue matching until cancelled.
 * @param flags 
 *           specifies certain options for retrieving the handle. 
 *           Should be 0 currently.
 * @param uniqueId 
 *           designates a location to store the unique identifier used to 
 *           identify the filter in event notifications and any 
 *           subsequent operations.
 *    
 * @return If the call is successful, <i>mpeos_filterSetFilter()</i> should return 
 *         <i>MPE_SUCCESS</i>. Otherwise, it will return one of the following 
 *         error codes:
 *    
 *         <i>MPE_EINVAL</i>
 *            indicates at least one input parameter has an invalid value.
 *         <i>MPE_ENOMEM</i>
 *            indicates insufficient memory.
 *         <i>MPE_SF_ERROR_FILTER_NOT_AVAILABLE</i>
 *            indicates insufficient filtering resources for the requested 
 *            filter specification.
 *         <i>MPE_SF_ERROR_TUNER_NOT_TUNED</i>
 *            indicates the specified filter source is not currently tuned.
 *         <i>MPE_SF_ERROR_TUNER_NOT_AT_FREQUENCY</i>
 *            indicates the specified filter source is not at the expected 
 *            frequency.
 * 
 * NOTE: When timesToMatch is greater than 1, it is expected that the MPEOS port will 
 *       return consecutive sections that match the given criteria (i.e. will not
 *       skip sections). Failure to return consecutive matching sections will result
 *       in degraded performance for object/data carousels and DAVIC SectionFilter users.
 */
mpe_Error mpeos_filterSetFilter(const mpe_FilterSource * filterSource,
        const mpe_FilterSpec * filterSpec, mpe_EventQueue queueId, void * act,
        uint8_t filterPriority, uint32_t timesToMatch, uint32_t flags,
        uint32_t * uniqueId);

/**
 * <i>mpeos_filterCancelFilter</i> should cancel the designated filter. 
 *  If the filter has not already been put into the cancalled state, an
 *  MPE_SF_EVENT_FILTER_CANCELLED event is put in the event queue
 *  associated with the unique ID. Any matched section data which has not been
 *  released via mpeos_filterSectionRelease() or implcitly released via
 *  <i>mpeos_filterSectionRead()</i> will not be released until 
 *  <i>mpeos_filterRelease()</i> is called. 
 *
 * @param uniqueId
 *           identifies the filter to be cancelled. 
 *    
 * @return If the call is successful, <i>mpeos_filterCancelFilter()</i> 
 *         should return <i>MPE_SUCCESS</i>. Otherwise, it should return 
 *         the following error code:
 * 
 *         <i>MPE_EINVAL</i>  
 *            indicates uniqueId is invalid.
 */
mpe_Error mpeos_filterCancelFilter(uint32_t uniqueId);

/**
 * <i>mpeos_filterGetSectionHandle()</i> should return a handle to a section 
 * matching the filtering specification set in mpeos_filterSetFilter(). 
 * Input parameters specified by uniqueId identifies the section filtering 
 * request. 
 *   
 * If the filter has matched more than one section, 
 * <i>mpeos_filterGetSectionHandle()</i> should return the sections in the  
 * order they are matched.
 * 
 * For efficient operation, the caller should call 
 *  <i>mpeos_filterGetSectionHandle()</i> in response to a 
 *  <i>MPE_SF_EVENT_SECTION_FOUND</i> or 
 *  <i>MPE_SF_EVENT_LAST_SECTION_FOUND</i>.
 * 
 * When the caller is done accessing the section, 
 * <i>mpeos_filterReleaseSection()</i> must be called to release the  
 * section and its associated resources.
 * 
 * If <i>MPE_SF_OPTION_IF_NOT_CANCELLED</i> is specified as part of the 
 * flags, a handle will be returned only if a section is available and the 
 * filter has not been cancelled. This is useful when the filter may be 
 * cancelled by another thread and the thread calling 
 * <i>mpeos_filterGetSectionHandle</i> does not want to process sections on a 
 * cancelled filter.
 * 
 * @param uniqueId
 *           identifies the filtering request. uniqueId must have been
 *           returned by a previous call to <i>mpeos_filterSetFilter()</i>. 
 * @param flags
 *           specifies options for retrieving the handle.
 * @param sectionHandle
 *           is a pointer to the destination for the section handle. The 
 *           handle is used in subsequent section operations.
 *    
 * @return If the call is successful, <i>mpeos_filterGetSectionHandle()</i> should return 
 *         <i>MPE_SUCCESS</i>. Otherwise, it will return one of the following 
 *         error codes:
 * 
 *         <i>MPE_EINVAL</i> 
 *            indicates at least one input parameter has an invalid value.
 * 
 *         <i>MPE_EINVAL</i> 
 *            if uniqueId is invalid, sectionHandle is null, or an invalid flag
 *            is specified.
 * 
 *         <i>MPE_ENOMEM</i> 
 *            indicates insufficient memory resource.
 *         <i>MPE_SF_ERROR_SECTION_NOT_AVAILABLE</i>
 *            indicates the section is not available, or the
 *            <i>MPE_SF_OPTION_IF_NOT_CANCELLED</i> option is indicated and
 *            the filter is cancelled.
 */
mpe_Error mpeos_filterGetSectionHandle(uint32_t uniqueId, uint32_t flags,
        mpe_FilterSectionHandle * sectionHandle);

/**
 * <i>mpeos_filterRelease()</i> will release the filter and any associated 
 * resources - including any sections that have been matched but not yet 
 * retrieved via <i>mpeos_filterGetSectionHandle()</i>. If the filter has not 
 * already been put into the cancalled state, an 
 * <i>MPE_SF_EVENT_FILTER_CANCELLED</i> event is put in the event queue 
 * associated with the unique ID.
 * 
 * @param uniqueId
 *           identifies the filtering request. uniqueId must have been
 *           returned by a previous call to <i>mpeos_filterSetFilter()</i>. 
 *    
 * @return If the call is successful, <i>mpeos_filterRelease()</i> 
 *         should return MPE_SUCCESS. Otherwise, it should return the 
 *         following error code:
 * 
 *         <i>MPE_EINVAL</i> 
 *            indicates uniqueId is invalid.
 */
mpe_Error mpeos_filterRelease(uint32_t uniqueId);

/**
 * <i>mpeos_filterGetSectionSize()</i> should return the total size 
 *  of the section.
 * 
 * @param sectionHandle
 *           is the handle assigned to the section. sectionHandle must have
 *           been returned by a previous call to 
 *           <i>mpeos_filterGetSectionHandle()</i>. 
 * @param size
 *           is the pointer to the destination for the section size value..
 *    
 * @return If the call is successful, <i>mpeos_filterGetSectionSize()</i> 
 *         should return MPE_SUCCESS. Otherwise, it should return one of the 
 *         following error codes:
 * 
 *         <i>MPE_EINVAL</i> 
 *            returned if size is NULL.
 * 
 *         <i>MPE_SF_ERROR_INVALID_SECTION_HANDLE</i> 
 *            indicates an invalid section handle was specified.
 */
mpe_Error mpeos_filterGetSectionSize(mpe_FilterSectionHandle sectionHandle,
        uint32_t * size);

/**
 * <i>mpeos_filterSectionRead()</i> should copy byteCount, 
 *  or (<i>section size - offset</i>) if smaller than <i>byteCount</i>, 
 *  starting at <i>offset</i> bytes within the section. The total number of 
 *  bytes read should be stored in <i>*bytesRead</i>, if <i>bytesRead</i> 
 *  is non-null.
 * 
 * If the <i>MPE_SF_OPTION_RELEASE_WHEN_COMPLETE</i> option is signalled in the
 * flags, the section will be released upon completion of a successful read 
 * (and <i>MPE_SUCCESS</i> is returned).
 *
 * @param sectionHandle
 *           identifies the handle assigned to the section. sectionHandle must
 *           have been returned by a previous call to 
 *           <i>mpeos_filterGetSectionHandle()</i>.
 * @param offset
 *           designates the offset to read from within the section. 
 * @param byteCount
 *           designates the number of bytes to read from the section. This can
 *           be larger than the size of the section (for example, the target
 *           buffer size).
 * @param flags 
 *           specifies options for reading the section data.
 * @param buffer 
 *           is a pointer to the buffer where the section data is copied.
 * @param bytesRead 
 *           is a pointer to the destination for the number of bytes actually
 *           copied.
 *    
 * @return If the call is successful, <i>mpeos_filterSectionRead()</i> should 
 *         return <i>MPE_SUCCESS</i>. Otherwise, it will return one of the 
 *         following error codes:
 *    
 *         <i>MPE_EINVAL</i>
 *            returned if <i>buffer</i> is null, <i>byteCount</i> is 0, or 
 *            <i>offset</i> is equal or greater than the section size.
 *         <i>MPE_SF_ERROR_INVALID_SECTION_HANDLE</i>
 *            indicates an invalid section handle was specified.
 */
mpe_Error mpeos_filterSectionRead(mpe_FilterSectionHandle sectionHandle,
        uint32_t offset, uint32_t byteCount, uint32_t flags, uint8_t * buffer,
        uint32_t * bytesRead);

/**
 * <i>mpeos_filterSectionRelease()</i> should release the section.
 *
 * @param sectionHandle
 *           identifies the handle assigned to the section. sectionHandle must
 *           have been returned by a previous call to 
 *           <i>mpeos_filterGetSectionHandle()</i>.
 *    
 * @return If the call is successful, <i>mpeos_filterSectionRead()</i> should 
 *         return <i>MPE_SUCCESS</i>. Otherwise, it will return the 
 *         following error code:
 *    
 *         <i>MPE_SF_ERROR_INVALID_SECTION_HANDLE</i>
 *            indicates an invalid section handle was specified.
 */
mpe_Error mpeos_filterSectionRelease(mpe_FilterSectionHandle sectionHandle);

/**
 * <i>mpeos_filterInit()</i> should initialize the section filtering system 
 *  to prepare it for use. This function will only be called once and no 
 *  other methods on this API will be called until this method returns with an 
 *  MPE_SUCCESS return value.
 *    
 * @return If initialization is successful, <i>mpeos_filterInit()</i> should 
 *         return <i>MPE_SUCCESS</i>. Otherwise, it will return the 
 *         following error code:
 *    
 *         <i>MPE_SF_ERROR</i>
 *            indicates that initialization failed.
 */
mpe_Error mpeos_filterInit(void);

/**
 * <i>mpeos_filterShutdown()</i> should shutdown the section filtering system. 
 * This function will only be called once. No other methods on this API will 
 * be called after this method returns - regardless of the return code. 
 * The section filtering subsystem should release as many resources as 
 * possible. But outstanding filters do not need to be issued 
 * <i>MPE_SF_EVENT_SOURCE_CLOSED</i> or any other events on shutdown.
 *    
 * @return If shutdown is successful, <i>mpeos_filterShutdown()</i> should 
 *         return <i>MPE_SUCCESS</i>. Otherwise, it will return the 
 *         following error code:
 *    
 *         <i>MPE_SF_ERROR</i>
 *            indicates that shutdown failed.
 */
mpe_Error mpeos_filterShutdown(void);

/**
 * <i>mpeos_filterRegisterAvailability()</i> requests that the given event queue
 * be notified when additional section filters are made generally available.
 * Following successful registration, an event will be sent to the given event
 * queue signaling filter availability when applicable.  
 * <p>
 * When an event is sent (and received) it will be composed of the following
 * information:
 * <ul>
 * <li> eventId = MPE_SF_FILTERS_AVAILABLE
 * <li> optionalEventData1 = reserved/unused
 * <li> optionalEventData2 = ACT (as given upon invocation of this function)
 * <li> optionalEventData3 = reserved/unused
 * </ul>
 *
 * @param queueId the event queue to send availability events to
 * @param act asynchronous completion token
 * @return if the call is successful, then <i>MPE_SUCCESS</i> will be returned.
 *         Otherwise, it will return the following error code:
 *
 *         <i>MPE_SF_EINVAL</i> 
 *            indicates that invalid parameters were provided.
 *         <i>MPE_ENOMEM</i>
 *            indicates insufficient memory.
 */
mpe_Error mpeos_filterRegisterAvailability(mpe_EventQueue queueId, void* act);

/**
 * Unregisters the given eventQueue/completion token pair that was previously
 * provided on a call to <i>mpeos_filterRegisterAvailability()</i>.  After successful
 * completion of this call, the given queue should no longer receive notifications
 * of filter availability.
 *
 * @param queueId the event queue to send availability events to
 * @param act asynchronous completion token
 * @return if the call is successful, then <i>MPE_SUCCESS</i> will be returned.
 *         Otherwise, it will return the following error code:
 *
 *         <i>MPE_SF_EINVAL</i> 
 *            indicates that invalid parameters were provided.
 */
mpe_Error mpeos_filterUnregisterAvailability(mpe_EventQueue queueId, void* act);

#ifdef __cplusplus
}
#endif

#endif  /* _MPEOS_FILTER_H */
