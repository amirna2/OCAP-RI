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

/* Header Files */
#include <mpe_types.h>      /* Resolve basic types. */
#include <mpe_error.h>      /* Resolve error codes. */
#include <mpe_os.h>      /* Resolve memory definitions. */

#include <mpeos_dbg.h>      /* Resolve log message defintions. */
#include <mpeos_event.h>  /* Resolve event definitions. */
#include <mpeos_filter.h>   /* for filter definitions */
#include <mpeos_thread.h>    /* Resolve thread definitions. */
#include <mpeos_time.h>     /* Resolve time definitions. */
#include <mpeos_util.h>      /* Resolve generic STB config structure type. */

#include <platform_filter.h>

#include <ri_pipeline_manager.h>
#include <ri_section_cache.h>

#include <string.h>

#define MPE_MEM_DEFAULT MPE_MEM_FILTER

/////////////////
// Global Data //
/////////////////
static mpe_Mutex sectionRequestMutex;
static GHashTable* sectionRequests; // Key: filter ID (integer)
// Value: pointer to ri_sf_sectionRequest

static mpe_Mutex outstandingSectionMutex;
static GHashTable* outstandingSections; // Key: section ID (integer)
// Value: pointer to ri_sf_section

static mpe_Mutex availabilityListenersMutex;
static GSList* availabilityListeners;
typedef struct _AvailabilityListener
{
    mpe_EventQueue queue;
    void* act;
} AvailabilityListener;

static ri_pipeline_manager_t* pipelineMgr;
static void cancelFilter(uint32_t filterID, int filterEvent);

/**
 * Release our section data structure and instruct the platform to
 * release any resources associated with this section
 */
static void destroySection(ri_sf_section* section)
{
    ri_section_cache_t* sectionCache = ri_get_section_cache();

    // Release section data
    sectionCache->release_section_data(sectionCache, section->sectionID);

    // Free the section data structure
    mpe_memFree(section);
}

/**
 * This function is passed to the glib 'foreach' functions to release
 * all unclaimed sections for a released filter
 */
static void destroyIncomingSection(ri_sf_section* section, gpointer* ignored)
{
    destroySection(section);
}

/**
 * This function is registered to the outstanding sections hash table.
 * This function ensures that section resources are properly freed
 * when the client releases them via a call to mpeos_filterSectionRelease().
 */
static void destroyOutstandingSection(ri_sf_section* section)
{
    destroySection(section);
}

/**
 * This function is registered with the section request hash table
 * to be called whenever a request is removed.  This function ensures
 * that all outstanding sections are properly released when a
 * section filter is released
 */
static void destroySectionRequest(ri_sf_sectionRequest* request)
{
    // Release all incoming sections associated with this filter that
    // have not been retrieved by the client
    g_queue_foreach(request->incomingSections, (GFunc) destroyIncomingSection,
            NULL);

    g_queue_clear(request->incomingSections);
    g_queue_free(request->incomingSections);

    // Free the section request structure
    mpe_memFree(request);
}

/**
 * This MPEOS-private API is used to close out existing filter requests
 * when a new tune is about to be initiated
 */
void sf_tuneInitiated(uint32_t tunerID)
{
    GHashTableIter iter;
    gpointer key, value;

    mpeos_mutexAcquire(sectionRequestMutex);

    // Search through the list of current filter requests
    // looking for ones associated with this tuner.  Cancel them
    // and send an event to their listeners indicating that the
    // source has been closed
    g_hash_table_iter_init(&iter, sectionRequests);
    while (g_hash_table_iter_next(&iter, &key, &value))
    {
        uint32_t filterID = GPOINTER_TO_UINT(key);
        ri_sf_sectionRequest* request = (ri_sf_sectionRequest*) value;

        if (request->tunerID == tunerID)
        {
            cancelFilter(filterID, MPE_SF_EVENT_SOURCE_CLOSED);
        }
    }

    mpeos_mutexRelease(sectionRequestMutex);
}

mpe_Error mpeos_filterInit(void)
{
    // Create section request list and mutex
    mpeos_mutexNew(&sectionRequestMutex);
    sectionRequests = g_hash_table_new_full(NULL, NULL, NULL,
            (GDestroyNotify) destroySectionRequest);

    // Create outstanding sections list and mutex
    mpeos_mutexNew(&outstandingSectionMutex);
    outstandingSections = g_hash_table_new_full(NULL, NULL, NULL,
            (GDestroyNotify) destroyOutstandingSection);

    // Create registered section listeners list and mutex
    mpeos_mutexNew(&availabilityListenersMutex);
    availabilityListeners = NULL;

    pipelineMgr = ri_get_pipeline_manager();

    return MPE_SUCCESS;
}

mpe_Error mpeos_filterShutdown(void)
{
    // TODO:  Free section requests list

    mpeos_mutexDelete(availabilityListenersMutex);
    mpeos_mutexDelete(sectionRequestMutex);
    return MPE_SUCCESS;
}

/**
 * This is the main callback for new section data notifications from all
 * pipelines.
 */
static void section_data_cb(ri_section_filter_t* filter, uint32_t sectionID,
        uint32_t filterID, uint8_t* sectionData, uint16_t sectionLength)
{
    ri_sf_section* section;
    ri_sf_sectionRequest* request;
    mpe_Bool shouldCancelFilter = FALSE;
    int filterEvent = MPE_SF_EVENT_SECTION_FOUND;
    ri_section_cache_t* sectionCache = ri_get_section_cache();

    mpeos_mutexAcquire(sectionRequestMutex);

    // Find the sectionRequest structure that corresponds to the given filter ID
    request = (ri_sf_sectionRequest*) g_hash_table_lookup(sectionRequests,
            GUINT_TO_POINTER(filterID));

    // This filter must have already been released or cancelled, so just go
    // ahead and release this section
    if (request == NULL || request->state == RI_SF_REQSTATE_CANCELLED
            || request->state == RI_SF_REQSTATE_MATCHED)
    {
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_FILTER,
                "%s -- (%d) request already canceled! request = %p, state = %d\n",
                __FUNCTION__, filterID, request, request == NULL ? -1
                        : request->state);
        sectionCache->release_section_data(sectionCache, sectionID);
        mpeos_mutexRelease(sectionRequestMutex);
        return;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER,
            "%s -- Filter ID = (%d), data = %p, length = %d!\n", __FUNCTION__,
            filterID, sectionData, sectionLength);

    // Check the times-to-match value.  If we have matched the last section,
    // go ahead and cancel the filter
    if (request->matchesRemaining > 0)
    {
        if (!request->runTillCanceled)
        {
            request->matchesRemaining--;
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER,
                    "%s -- (%d) matchesRemaining = %d!\n", __FUNCTION__,
                    filterID, request->matchesRemaining);
        }
        if (request->matchesRemaining == 0) // Matched last section
        {
            request->state = RI_SF_REQSTATE_MATCHED;
            filterEvent = MPE_SF_EVENT_LAST_SECTION_FOUND;
            shouldCancelFilter = TRUE;
        }
    }

    // Allocate section data structure
    if (mpe_memAlloc(sizeof(ri_sf_section), (void**) &section) != MPE_SUCCESS)
    {
        mpeos_mutexRelease(sectionRequestMutex);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                "Could not allocate section data structure!\n");
        sectionCache->release_section_data(sectionCache, sectionID);
        return;
    }

    // Post the event to the requestors event queue
    if (mpeos_eventQueueSend(request->requestorQueue, filterEvent,
            (void*) filterID, request->requestorACT, 0) != MPE_SUCCESS)
    {
        mpeos_mutexRelease(sectionRequestMutex);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                "Could not post section event to client queue!\n");
        mpe_memFree(section);
        sectionCache->release_section_data(sectionCache, sectionID);
        return;
    }

    // Populate section data structure
    section->sectionID = sectionID;
    section->sectionData = sectionData;
    section->sectionLength = sectionLength;

    // Add to our incoming section queue
    g_queue_push_tail(request->incomingSections, section);

    // Do we need to cancel the filter because we're fully matched?
    if (shouldCancelFilter)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER,
                "%s -- Need to cancel filter (%d)!\n", __FUNCTION__, filterID);
        // Do NOT dispatch an event
        cancelFilter(filterID, MPE_SF_EVENT_UNKNOWN);
    }

    mpeos_mutexRelease(sectionRequestMutex);
}

// Notifies listeners that a section filter resource may now be available
static void notifyFilterAvailable(AvailabilityListener* listener,
        gpointer* data)
{
    mpeos_eventQueueSend(listener->queue, MPE_SF_EVENT_FILTER_AVAILABLE, NULL,
            listener->act, 0);
}

// Internal routine for cancelling filters, handles the dispatching
// of the given event.  If the filterEvent is MPE_SF_EVENT_UNKNOWN,
// no event is dispatched.
static void cancelFilter(uint32_t filterID, int filterEvent)
{
    // ASSERT:  sectionRequestMutex is held by the caller

    ri_sf_sectionRequest * request;

    // Find the section request structure
    request = (ri_sf_sectionRequest*) g_hash_table_lookup(sectionRequests,
            GUINT_TO_POINTER(filterID));

    // If we can not find the request, or if it has already been
    // canceled, let the caller know
    if (request == NULL || request->state == RI_SF_REQSTATE_CANCELLED)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                "Could not find the request or it has already been cancelled!\n");
        return;
    }

    request->state = RI_SF_REQSTATE_CANCELLED;

    (void) request->pipelineFilter->cancel_filter(request->pipelineFilter,
            filterID);

    // Notify listeners that a filter resource may be available
    g_slist_foreach(availabilityListeners, (GFunc) notifyFilterAvailable, NULL);

    // Post correct event to mpeos queue
    if (filterEvent != MPE_SF_EVENT_UNKNOWN)
    {
        if (mpeos_eventQueueSend(request->requestorQueue, filterEvent,
                (void*) filterID, request->requestorACT, 0) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                    "Could not post cancelled/preempted event to client queue!\n");
        }
    }
}

// Create a filter and initiate filtering for a section(s)
// originating from the section source which matched the designated
// filter specification.
mpe_Error mpeos_filterSetFilter(const mpe_FilterSource* filterSource,
        const mpe_FilterSpec* filterSpec, mpe_EventQueue queueID, void* act,
        uint8_t filterPriority, uint32_t timesToMatch, uint32_t flags,
        uint32_t* uniqueID)
{
    mpe_Error error;
    uint32_t filterID;
    ri_section_filter_t* pipelineFilter;
    ri_sf_sectionRequest* new_request;
    uint32_t tunerID;

    // Validate parameters
    if (filterSource == NULL || filterSpec == NULL || uniqueID == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                "mpeos_filterSetFilter -- parameter is NULL\n");
        return MPE_EINVAL;
    }

    // Find the pipeline that corresponds to this request
    switch (filterSource->sourceType)
    {
    case MPE_FILTER_SOURCE_OOB:
    {
        // Retrieve the out-of-band pipeline
        const ri_oob_pipeline_t* oobPipeline = pipelineMgr->get_oob_pipeline(
                pipelineMgr);
        if (oobPipeline == NULL)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                    "mpeos_filterSetFilter -- no OOB pipeline!\n");
            return MPE_EINVAL;
        }

        tunerID = 0;

        // Retrieve the section filtering implementation for the OOB
        // pipeline
        pipelineFilter = oobPipeline->get_section_filter(oobPipeline);

        break;
    }

    case MPE_FILTER_SOURCE_INB:
    {
        uint32_t numPipelines;
        const ri_pipeline_t** pipelines = pipelineMgr->get_live_pipelines(
                pipelineMgr, &numPipelines);
        ri_pipeline_t* pipeline;
        ri_tuner_t* tuner;
        ri_tuner_status_t tuner_status;

        // Tuner ID is just the index+1 into the live pipeline array
        if ((filterSource->parm.p_INB.tunerId - 1) >= numPipelines)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                    "mpeos_filterSetFilter -- invalid tuner ID (%d)\n",
                    filterSource->parm.p_INB.tunerId);
            return MPE_EINVAL;
        }

        tunerID = filterSource->parm.p_INB.tunerId;
        pipeline = (ri_pipeline_t*) pipelines[tunerID - 1];

        // Retrieve the section filtering implementation for the desired
        // in-band pipeline
        pipelineFilter = pipeline->get_section_filter(pipeline);

        // Check the tuner state to ensure it is actually tuned
        // to the desired frequency
        tuner = pipeline->get_tuner(pipeline);
        tuner->request_status(tuner, &tuner_status);
        if (tuner_status.frequency != filterSource->parm.p_INB.freq)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_FILTER,
                    "mpeos_filterSetFilter -- tuner is not tuned to the requested frequency.  TunerID = %d, Expected Freq = %d, Actual Freq = %d\n",
                    filterSource->parm.p_INB.tunerId,
                    filterSource->parm.p_INB.freq, tuner_status.frequency);
            return MPE_EINVAL;
        }

        break;
    }

    case MPE_FILTER_SOURCE_DSG_APPID:
    {
        // Retrieve the DSG/application pipeline
        const ri_dsg_pipeline_t* dsgPipeline = pipelineMgr->get_dsg_pipeline(
                pipelineMgr);

        if (dsgPipeline == NULL)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                    "mpeos_filterSetFilter -- no DSG pipeline!\n");
            return MPE_EINVAL;
        }

        tunerID = 0;

        // Retrieve the section filtering implementation for the DSG
        // pipeline
        pipelineFilter = dsgPipeline->get_section_filter(dsgPipeline);
        error = pipelineFilter->set_appID(pipelineFilter,
                                          filterSource->parm.p_DSGA.appId);
        if (error)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                    "mpeos_filterSetFilter error(%d) setting appID!\n", error);
        }

        break;
    }

    case MPE_FILTER_SOURCE_HN_STREAM:
    {
        // Retrieve the hn player pipeline
        const ri_pipeline_t* hnPlayerPipeline = pipelineMgr->get_hn_player_pipeline(
                pipelineMgr);
        if (hnPlayerPipeline == NULL)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                    "mpeos_filterSetFilter -- no HN Player pipeline!\n");
            return MPE_EINVAL;
        }

        tunerID = 0;
        // TODO: filterSource->parm.p_HNS.hn_stream_session;

        // Retrieve the section filtering implementation for hn player pipeline
        pipelineFilter = hnPlayerPipeline->get_section_filter((ri_pipeline_t*)hnPlayerPipeline);
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_FILTER,
            "%s -- HN Stream Filter: PID = %X, matches = %d, pipeFilter = %p\n",
            __FUNCTION__, filterSource->pid, timesToMatch, pipelineFilter);
        break;
    }

    default:
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                "mpeos_filterSetFilter -- invalid filter source\n");
        return MPE_EINVAL;
    }

    /////////////////////////
    // Create section request
    /////////////////////////

    // Allocate structure memory
    if (mpe_memAlloc(sizeof(ri_sf_sectionRequest), (void**) &new_request)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                "Could not allocate section request structure!\n");
        return MPE_ENOMEM;
    }

    // Initialize values
    new_request->pipelineFilter = pipelineFilter;
    new_request->state = RI_SF_REQSTATE_INVALID;
    new_request->requestorQueue = queueID;
    new_request->requestorACT = act;
    new_request->priority = filterPriority;
    new_request->incomingSections = g_queue_new();
    new_request->tunerID = tunerID;

    // Do we match a certain number or run indefinitely?
    if (timesToMatch == 0)
    {
        new_request->matchesRemaining = 1;
        new_request->runTillCanceled = TRUE;
    }
    else
    {
        new_request->matchesRemaining = timesToMatch;
        new_request->runTillCanceled = FALSE;
    }

    // Acquire this mutex so that we do not process any section
    // events until we have added the request structure
    mpeos_mutexAcquire(sectionRequestMutex);

    // Keep trying to set this filter until we can't preempt any
    // lower priority filters
    do
    {
        // Attempt to set the filter
        error = pipelineFilter->create_filter(pipelineFilter, &filterID,
                filterSource->pid, filterSpec->pos.mask, filterSpec->pos.vals,
                filterSpec->pos.length, filterSpec->neg.mask,
                filterSpec->neg.vals, filterSpec->neg.length, section_data_cb);

        // Failed to create the filter, so try to pre-empt a lower priority
        if (error == RI_ERROR_FILTER_NOT_AVAILABLE)
        {
            GHashTableIter iter;
            gpointer key, value;
            mpe_Bool filterPreempted = FALSE;

            MPEOS_LOG(
                    MPE_LOG_DEBUG,
                    MPE_MOD_FILTER,
                    "mpeos_filterSetFilter -- No filters available!  Checking for lower priority filters.  This priority = %d\n",
                    filterPriority);

            // Search through the list of current filter requests
            // looking for one with lower priority and cancel it.
            g_hash_table_iter_init(&iter, sectionRequests);
            while (g_hash_table_iter_next(&iter, &key, &value))
            {
                uint32_t tempFilterID = GPOINTER_TO_UINT(key);
                ri_sf_sectionRequest* request = (ri_sf_sectionRequest*) value;

                MPEOS_LOG(
                        MPE_LOG_DEBUG,
                        MPE_MOD_FILTER,
                        "mpeos_filterSetFilter -- Checking if this filter can be pre-empted.  Prioriy = %d\n",
                        request->priority);
                // Check for lower priority (1 is the highest priority)
                if (request->priority > filterPriority)
                {
                    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER,
                            "mpeos_filterSetFilter -- Pre-empting this filter!\n");
                    cancelFilter(tempFilterID, MPE_SF_EVENT_FILTER_PREEMPTED);
                    filterPreempted = TRUE;
                    break;
                }
            }

            // If we could not preempt any filters then we will not be
            // able to set this new filter
            if (!filterPreempted)
            {
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER,
                        "mpeos_filterSetFilter -- No filters could be pre-empted!\n");
                // Release our mutex and free the section request
                mpe_memFree(new_request);
                mpeos_mutexRelease(sectionRequestMutex);
                return MPE_SF_ERROR_FILTER_NOT_AVAILABLE;
            }
        }
        else
            // Success
            break;
    } while (error == RI_ERROR_FILTER_NOT_AVAILABLE);

    // Filter successfully created, set state to READY
    new_request->state = RI_SF_REQSTATE_READY;

    // Insert the new request into our request table
    g_hash_table_insert(sectionRequests, GUINT_TO_POINTER(filterID),
            new_request);

    mpeos_mutexRelease(sectionRequestMutex);

    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_FILTER,
            "%s -- %s Filter Created! PID = %X, timesToMatch = %d, ID = (%d)\n",
            __FUNCTION__, tunerID == 0 ? "OOB" : "IB", filterSource->pid,
            timesToMatch, filterID);

    // Return the new filter ID to the caller
    *uniqueID = filterID;

    return MPE_SUCCESS;
}

// Cancel the designated filter.
mpe_Error mpeos_filterCancelFilter(uint32_t uniqueID)
{
    mpeos_mutexAcquire(sectionRequestMutex);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_FILTER, "%s -- Filter Canceled (%d)!\n",
            __FUNCTION__, uniqueID);

    cancelFilter(uniqueID, MPE_SF_EVENT_FILTER_CANCELLED);

    mpeos_mutexRelease(sectionRequestMutex);

    return MPE_SUCCESS;
}

// Get a handle to the next available MPEG section for the filter
mpe_Error mpeos_filterGetSectionHandle(uint32_t uniqueID, uint32_t flags,
        mpe_FilterSectionHandle * sectionHandle)
{
    ri_sf_sectionRequest* request;
    ri_sf_section* section;

    if (sectionHandle == NULL)
        return MPE_EINVAL;

    mpeos_mutexAcquire(sectionRequestMutex);

    // Find the section request for this filter
    request = (ri_sf_sectionRequest*) g_hash_table_lookup(sectionRequests,
            GUINT_TO_POINTER(uniqueID));

    // Can not find section request
    if (request == NULL)
    {
        mpeos_mutexRelease(sectionRequestMutex);
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILTER,
                "mpeos_filterGetSectionHandle -- Could not find section request (%d)!\n",
                uniqueID);
        return MPE_EINVAL;
    }

    // Pull the next section off of the incoming section queue
    section = g_queue_pop_head(request->incomingSections);

    mpeos_mutexRelease(sectionRequestMutex);

    // Is there a section available?
    if (((flags & MPE_SF_OPTION_IF_NOT_CANCELLED) && request->state
            == RI_SF_REQSTATE_CANCELLED) || section == NULL)
    {
        return MPE_SF_ERROR_SECTION_NOT_AVAILABLE;
    }

    mpeos_mutexAcquire(outstandingSectionMutex);

    // Add this section to our list of outstanding sections and return its
    // pointer as the section handle
    g_hash_table_insert(outstandingSections, GUINT_TO_POINTER(
            section->sectionID), section);

    mpeos_mutexRelease(outstandingSectionMutex);

    // Return the sectionID as the handle
    *sectionHandle = (mpe_FilterSectionHandle)(section->sectionID);

    return MPE_SUCCESS;
}

// Release the filter and any unclaimed sections
mpe_Error mpeos_filterRelease(uint32_t uniqueID)
{
    ri_sf_sectionRequest* request;

    mpeos_mutexAcquire(sectionRequestMutex);

    // Validate filterID
    request = (ri_sf_sectionRequest*) g_hash_table_lookup(sectionRequests,
            GUINT_TO_POINTER(uniqueID));

    if (request == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                "mpeos_filterRelease -- invalid filter ID (%d)\n", uniqueID);
        mpeos_mutexRelease(sectionRequestMutex);
        return MPE_EINVAL;
    }

    // Make sure the filter is canceled
    if (request->state != RI_SF_REQSTATE_CANCELLED)
    {
        cancelFilter(uniqueID, MPE_SF_EVENT_FILTER_CANCELLED);
    }

    // Remove the request structure
    (void) g_hash_table_remove(sectionRequests, GUINT_TO_POINTER(uniqueID));

    mpeos_mutexRelease(sectionRequestMutex);

    return MPE_SUCCESS;
}

// Write the size (in bytes) of the designated section to *size
mpe_Error mpeos_filterGetSectionSize(mpe_FilterSectionHandle sectionHandle,
        uint32_t * size)
{
    ri_sf_section* section;

    // Validate arguments
    if (sectionHandle == 0)
        return MPE_SF_ERROR_INVALID_SECTION_HANDLE;

    if (size == NULL)
        return MPE_EINVAL;

    mpeos_mutexAcquire(outstandingSectionMutex);

    // Retrieve the section size
    section = g_hash_table_lookup(outstandingSections, GUINT_TO_POINTER(
            sectionHandle));
    if (section == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                "mpeos_filterGetSectionSize -- invalid section ID (%d)\n",
                sectionHandle);
        mpeos_mutexRelease(outstandingSectionMutex);
        return MPE_EINVAL;
    }

    *size = section->sectionLength;

    mpeos_mutexRelease(outstandingSectionMutex);

    return MPE_SUCCESS;
}

// Copy byteCount, or (section size - offset) if smaller than byteCount, bytes
//  starting at offset bytes within the section. The total number of bytes read
//  will be stored in *bytesRead, if bytesRead is non-null.
mpe_Error mpeos_filterSectionRead(mpe_FilterSectionHandle sectionHandle,
        uint32_t offset, uint32_t byteCount, uint32_t flags, uint8_t * buffer,
        uint32_t * bytesRead)
{
    ri_sf_section* section;
    uint32_t bytesToCopy;
    uint32_t sectionLength;

    mpeos_mutexAcquire(outstandingSectionMutex);

    section = (ri_sf_section*) g_hash_table_lookup(outstandingSections,
            GUINT_TO_POINTER(sectionHandle));

    // Validate section handle
    if (section == NULL)
    {
        mpeos_mutexRelease(outstandingSectionMutex);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                "mpeos_filterSectionRead - invalid section handle (%d)\n",
                sectionHandle);
        return MPE_SF_ERROR_INVALID_SECTION_HANDLE;
    }

    sectionLength = section->sectionLength;

    // Validate all other arguments
    if (buffer == NULL || offset > sectionLength || byteCount == 0)
    {
        mpeos_mutexRelease(outstandingSectionMutex);
        return MPE_EINVAL;
    }

    // Validate offset
    if (offset >= sectionLength)
    {
        mpeos_mutexRelease(outstandingSectionMutex);
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_FILTER,
                "mpeos_filterSectionRead - offset larger than size (%d >= %d)\n",
                offset, sectionLength);
        return MPE_EINVAL;
    }

    // Determine the number of bytes to copy.
    bytesToCopy = (byteCount < sectionLength - offset) ? byteCount
            : sectionLength - offset;

    // Copy the data
    memcpy(buffer, (section->sectionData) + offset, bytesToCopy);

    mpeos_mutexRelease(outstandingSectionMutex);

    // The contract for this function says that the user can pass NULL for
    // this pointer.
    if (bytesRead != NULL)
    {
        *bytesRead = bytesToCopy;
    }

    // Should we release the section?
    if (flags & MPE_SF_OPTION_RELEASE_WHEN_COMPLETE)
    {
        (void) mpeos_filterSectionRelease(sectionHandle); // Should the return value be checked here?
    }

    return MPE_SUCCESS;
}

// Release the designated section
mpe_Error mpeos_filterSectionRelease(mpe_FilterSectionHandle sectionHandle)
{
    ri_sf_section* section;

    mpeos_mutexAcquire(outstandingSectionMutex);

    section = (ri_sf_section*) g_hash_table_lookup(outstandingSections,
            GUINT_TO_POINTER(sectionHandle));

    // Validate section handle
    if (section == NULL)
    {
        mpeos_mutexRelease(outstandingSectionMutex);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                "mpeos_filterSectionRead - invalid section handle (%d)\n",
                sectionHandle);
        return MPE_SF_ERROR_INVALID_SECTION_HANDLE;
    }

    // Remove this section from our outstanding sections list
    (void) g_hash_table_remove(outstandingSections, GUINT_TO_POINTER(
            sectionHandle));

    mpeos_mutexRelease(outstandingSectionMutex);

    return MPE_SUCCESS;
}

// Register event queue for availability event
// TODO:  Listeners are not yet being notified
mpe_Error mpeos_filterRegisterAvailability(mpe_EventQueue q, void* act)
{
    AvailabilityListener* listener;

    // Allocate structure memory
    if (mpe_memAlloc(sizeof(AvailabilityListener), (void**) &listener)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_FILTER,
                "Could not allocate section listener structure!\n");
        return MPE_ENOMEM;
    }

    listener->queue = q;
    listener->act = act;

    // Add this listener to our list
    mpeos_mutexAcquire(availabilityListenersMutex);
    availabilityListeners = g_slist_prepend(availabilityListeners, listener);
    mpeos_mutexRelease(availabilityListenersMutex);

    return MPE_SUCCESS;
}

gint findListener(AvailabilityListener* a, AvailabilityListener*b)
{
    return ((a->queue == b->queue) && (a->act == b->act)) ? 0 : 1;
}

// Unregister event queue for availability event
// TODO:  Listeners are not yet being notified
mpe_Error mpeos_filterUnregisterAvailability(mpe_EventQueue q, void* act)
{
    GSList* foundElement;
    AvailabilityListener listener;
    listener.queue = q;
    listener.act = act;

    mpeos_mutexAcquire(availabilityListenersMutex);

    // Find this listener in our list
    foundElement = g_slist_find_custom(availabilityListeners, &listener,
            (GCompareFunc) findListener);
    if (foundElement != NULL)
    {
        // Remove it from our list
        availabilityListeners = g_slist_delete_link(availabilityListeners,
                foundElement);
    }

    mpeos_mutexRelease(availabilityListenersMutex);

    return MPE_SUCCESS;
}
