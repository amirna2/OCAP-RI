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

/* include files */
#include <stdio.h>          /* Resolve sprintf. */
#include <stdlib.h>
#include <string.h>

#include <mpe_os.h>

#include <mpeos_event.h>    /* Resolve types. */
#include <mpeos_sync.h>
#include <mpe_error.h>      /* Resolve error codes. */
#include <mpeos_dbg.h>

#define MPE_MEM_DEFAULT MPE_MEM_EVENT

// Maxium number of event queues supported
#define MAX_EVENT_QUEUES 64

// Event queue name used when user doesn't provide one
#define DEFAULT_QUEUE_NAME "MPE_Event_Queue-"
static int g_queue_name_count = 1;

// Event queues
mpe_Mutex g_event_queues_mutex;
static linux_event_queue_t* g_event_queues[MAX_EVENT_QUEUES];

// Event system initialization
static mpe_Bool g_initialized = FALSE;
static void initialize()
{
    int i;

    mpeos_mutexNew(&g_event_queues_mutex);

    // Initialize the event queue list
    for (i = 0; i < MAX_EVENT_QUEUES; i++)
        g_event_queues[i] = NULL;

    g_initialized = TRUE;
}

// Event queue Helper routines
#define queueEmpty(queue) (queue->cur_event == queue->avail_event)
#define queueFull(queue)  (queue->cur_event == ((queue->avail_event + 1) % MAX_EVENTS_PER_QUEUE))

// Free all resources associated with the given queue
static void freeQueue(linux_event_queue_t* queue)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT,
            "%s: Freeing resources for event queue (Name = %s)\n",
            __FUNCTION__, queue->name);

    mpeos_condDelete(queue->cond);
    mpeos_mutexDelete(queue->mutex);
    free(queue->name);
    mpe_memFree(queue);
}

// Get the number of events currently in the queue
int numEvents(linux_event_queue_t* queue)
{
    return abs((int) (queue->cur_event) - (int) (queue->avail_event));
}

// Find an unused event queue index and allocate a new queue structure in that spot
int findAvailableQueueAndCreate()
{
    int i;

    mpeos_mutexAcquire(g_event_queues_mutex);

    for (i = 0; i < MAX_EVENT_QUEUES; ++i)
    {
        if (g_event_queues[i] == NULL)
        {
            linux_event_queue_t* newQueue;

            // Allocate data for new queue structure
            if (mpe_memAlloc(sizeof(linux_event_queue_t), (void**) &newQueue)
                    != MPE_SUCCESS)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_EVENT,
                        "%s: Could not allocate memory for new event queue!\n",
                        __FUNCTION__);
                return MPE_ENOMEM;
            }

            // Initialize basic event queue data
            newQueue->cur_event = 0;
            newQueue->avail_event = 0;
            newQueue->valid = TRUE;
            newQueue->num_waiting = 0;
            mpeos_mutexNew(&newQueue->mutex);
            mpeos_condNew(FALSE, FALSE, &newQueue->cond);

            // Place our new queue into the queue list
            g_event_queues[i] = newQueue;

            mpeos_mutexRelease(g_event_queues_mutex);
            return i;
        }
    }

    mpeos_mutexRelease(g_event_queues_mutex);

    MPEOS_LOG(
            MPE_LOG_ERROR,
            MPE_MOD_EVENT,
            "%s: No more queues available!  Consider increasing the current queue limit (%d)\n",
            __FUNCTION__, MAX_EVENT_QUEUES);

    return -1;
}

/**
 * <i>mpeos_eventQueueNew()</i>
 *
 * Create a new event queue.
 *
 * @param queueId is a pointer for returning the identifier of the new
 *        event queue.
 *
 * @param queueName constant string representation of the queue
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS</i>
 *         is returned
 */
mpe_Error mpeos_eventQueueNew(mpe_EventQueue *queueId, const char *queueName)
{
    uint32_t newQueueIndex;
    linux_event_queue_t* newQueue;

    if (!g_initialized)
        initialize();

    // Validate arguments
    if (queueId == NULL)
        return MPE_EINVAL;

    // Find an available queue slot
    if ((newQueueIndex = findAvailableQueueAndCreate()) == -1)
        return MPE_EEVENT;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT,
            "%s: Creating new event queue -- %s\n", __FUNCTION__, queueName);

    newQueue = g_event_queues[newQueueIndex];

    // Create new event name if not provided by user
    if (queueName == NULL)
    {
        char newEventQueueName[64];
        snprintf(newEventQueueName, 64, "%s%d", DEFAULT_QUEUE_NAME,
                g_queue_name_count++);
        newQueue->name = strdup(newEventQueueName);
    }
    else
    {
        newQueue->name = strdup(queueName);
    }

    // Return the index+1 to the user (don't want to return 0 as a queue ID)
    *queueId = newQueueIndex + 1;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT,
            "%s: New event queue created.  Name = %s, ID = %d\n", __FUNCTION__,
            newQueue->name, *queueId);

    return MPE_SUCCESS;
}

/**
 * <i> mpeos_eventQueueDelete()</i>
 *
 * Delete the specified event queue and all associated resources.
 *
 * @param queueId is the identifier of the event queue.
 *
 * @return The MPE error code if the delete fails, otherwise <i>MPE_SUCCESS</i>
 *         is returned
 */
mpe_Error mpeos_eventQueueDelete(mpe_EventQueue queueId)
{
    linux_event_queue_t* queue;

    if (!g_initialized)
        initialize();

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT,
            "%s: Deleting event queue. ID = %d\n", __FUNCTION__, queueId);

    mpeos_mutexAcquire(g_event_queues_mutex);

    // Make sure this is a valid queue ID
    if ((queueId == 0) || ((queue = g_event_queues[queueId - 1]) == NULL)
            || (!queue->valid))
    {
        mpeos_mutexRelease(g_event_queues_mutex);
        return MPE_EINVAL;
    }

    // Remove it from our list of queues
    g_event_queues[queueId - 1] = NULL;

    // Get exclusive access to this queue
    mpeos_mutexAcquire(queue->mutex);

    // Invalidate this queue
    queue->valid = FALSE;

    mpeos_mutexRelease(g_event_queues_mutex);

    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_EVENT,
            "%s: Event queue (ID = %d) marked for deletion. Alerting waiting threads.\n",
            __FUNCTION__, queueId);

    // Notify all waiting threads
    mpeos_condSet(queue->cond);

    // If there are no threads waiting, release queue resources
    if (queue->num_waiting == 0)
    {
        mpeos_mutexRelease(queue->mutex);
        freeQueue(queue);
        return MPE_SUCCESS;
    }

    mpeos_mutexRelease(queue->mutex);

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_eventQueueSend()</i> function will post the specified event to
 * specified queue with the optional event data, which is a void * pointer to any
 * additional data for the receiver of the event.
 *
 * @param queueId is the target event queue identifier.
 * @param eventId is the event identifier to queue.
 * @param optionalEventData1 is a pointer to optional event data to deliver along
 *          with the specified event.
 * @param optionalEventData2 is a pointer to optional event data to deliver along
 *          with the specified event.
 * @param eventFlag is an integer flag delivered along with the specified event
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_eventQueueSend(mpe_EventQueue queueId, mpe_Event eventId,
        void* optionalEventData1, void* optionalEventData2, uint32_t eventFlag)
{
    linux_event_queue_t* queue;

    if (!g_initialized)
        initialize();

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT,
            "%s: ID = %d.  EventId = %d, data1 = %p, data2 = %p, flag = %d\n",
            __FUNCTION__, queueId, eventId, optionalEventData1,
            optionalEventData2, eventFlag);

    mpeos_mutexAcquire(g_event_queues_mutex);

    // Make sure this is a valid queue ID
    if ((queueId == 0) || ((queue = g_event_queues[queueId - 1]) == NULL)
            || (!queue->valid))
    {
        mpeos_mutexRelease(g_event_queues_mutex);
        return MPE_EINVAL;
    }

    // Get exclusive access to this queue
    mpeos_mutexAcquire(queue->mutex);

    mpeos_mutexRelease(g_event_queues_mutex);

    // Is there room left in the queue
    if (queueFull(queue))
    {
        mpeos_mutexRelease(queue->mutex);
        MPEOS_LOG(
                MPE_LOG_WARN,
                MPE_MOD_EVENT,
                "%s: ID = %d.  Queue is FULL!  Consider increasing the number of events allowed in a queue\n",
                __FUNCTION__, queueId);

        return MPE_EEVENT;
    }

    // Populate event data
    queue->events[queue->avail_event].event = eventId;
    queue->events[queue->avail_event].event_data1 = optionalEventData1;
    queue->events[queue->avail_event].event_data2 = optionalEventData2;
    queue->events[queue->avail_event].event_flag = eventFlag;

    // Update location of next available event slot
    queue->avail_event = (queue->avail_event + 1) % MAX_EVENTS_PER_QUEUE;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT, "%s: ID = %d.  Num events = %d\n",
            __FUNCTION__, queueId, numEvents(queue));

    // Notify waiting threads of a new event
    mpeos_condSet(queue->cond);

    mpeos_mutexRelease(queue->mutex);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_eventQueueNext()</i>
 *
 * Retrieve the next event from the specified event queue (non-blocking).
 * Returns via the specified pointers; the identifier of the next event and
 * any optional data.
 *
 * @param queueId is the target event queue identifier.
 * @param eventId is a pointer for returning the event identifier of the next
 *        event in the queue
 * @param eventData is a void pointer for returing any optional event data
 *        to be delivered along with the event.
 *
 * @return The MPE error code if retrieve fails, otherwise <i>MPE_SUCCESS</i>
 *         is returned
 */
mpe_Error mpeos_eventQueueNext(mpe_EventQueue queueId, mpe_Event* eventId,
        void** eventData1, void** eventData2, uint32_t* eventFlag)
{
    linux_event_queue_t* queue;

    if (!g_initialized)
        initialize();

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT, "%s: ID = %d\n", __FUNCTION__,
            queueId);

    mpeos_mutexAcquire(g_event_queues_mutex);

    // Make sure this is a valid queue ID
    if ((eventId == NULL) || (queueId == 0) || ((queue = g_event_queues[queueId
            - 1]) == NULL) || (!queue->valid))
    {
        mpeos_mutexRelease(g_event_queues_mutex);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_EVENT,
                "%s: Invalid event queue ID (%d)!\n", __FUNCTION__, queueId);
        return MPE_EINVAL;
    }

    // Get exclusive access to this queue
    mpeos_mutexAcquire(queue->mutex);

    mpeos_mutexRelease(g_event_queues_mutex);

    // Are there events in the queue
    if (queueEmpty(queue))
    {
        mpeos_mutexRelease(queue->mutex);
        return MPE_EEVENT;
    }

    // Retrieve the current event data
    *eventId = queue->events[queue->cur_event].event;
    if (eventData1 != NULL)
        *eventData1 = queue->events[queue->cur_event].event_data1;
    if (eventData2 != NULL)
        *eventData2 = queue->events[queue->cur_event].event_data2;
    if (eventFlag != NULL)
        *eventFlag = queue->events[queue->cur_event].event_flag;

    // Increment the current event index
    queue->cur_event = (queue->cur_event + 1) % MAX_EVENTS_PER_QUEUE;

    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_EVENT,
            "%s: ID = %d.  Num events = %d. Next event: EventId = %d, data1 = %p, data2 = %p, flag = %d\n",
            __FUNCTION__, queueId, numEvents(queue), *eventId, (eventData1
                    != NULL) ? *eventData1 : 0,
            (eventData2 != NULL) ? *eventData2 : 0,
            (eventFlag != NULL) ? *eventFlag : 0);

    // If the event queue is now empty, unset the condition var
    if (queueEmpty(queue))
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT,
                "%s: ID = %d.  Queue is now empty.\n", __FUNCTION__, queueId);
        mpeos_condUnset(queue->cond);
    }

    mpeos_mutexRelease(queue->mutex);

    return MPE_SUCCESS;
}

/**
 * <i>mpeos_eventQueueWaitNext()</i>
 *
 * Retrieve the next event from the specified event queue (blocking with
 * timeout). Returns via the specified pointers; the identifier of the
 * next event and any optional data.
 *
 * @param queueId is the target event queue identifier.
 * @param eventId is a pointer for returning the event identifier of the next
 *        event in the queue.
 * @param eventData is a void pointer for returning any optional event data
 *        to be delivered along with the event.
 * @param timeout is the maximum time period in milliseconds to wait for the
 *        next event. A time period of zero means wait indefinitely.
 *
 * @return The MPE error code if the wait fails, otherwise <i>MPE_SUCCESS</i>
 *         is returned.
 */
mpe_Error mpeos_eventQueueWaitNext(mpe_EventQueue queueId, mpe_Event* eventId,
        void** eventData1, void** eventData2, uint32_t* eventFlag,
        uint32_t timeout)
{
    mpe_Error retval;
    linux_event_queue_t* queue;

    if (!g_initialized)
        initialize();

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT, "%s: ID = %d, timeout = %d\n",
            __FUNCTION__, queueId, timeout);

    mpeos_mutexAcquire(g_event_queues_mutex);

    // Make sure this is a valid queue ID and valid event ID pointer
    if ((eventId == NULL) || (queueId == 0) || ((queue = g_event_queues[queueId
            - 1]) == NULL) || (!queue->valid))
    {
        mpeos_mutexRelease(g_event_queues_mutex);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_EVENT,
                "%s: Invalid event queue ID (%d)!\n", __FUNCTION__, queueId);
        return MPE_EINVAL;
    }

    // Get exclusive access to this queue
    mpeos_mutexAcquire(queue->mutex);

    mpeos_mutexRelease(g_event_queues_mutex);

    // Even if this thread will not actually wait, we increment the waiting
    // count here, we'll decrement it after we pass through the condition
    queue->num_waiting++;

    mpeos_mutexRelease(queue->mutex);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT, "%s: ID = %d, Waiting...\n",
            __FUNCTION__, queueId);

    // Wait for events to be available
    retval = mpeos_condWaitFor(queue->cond, timeout);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT, "%s: ID = %d, Events available!\n",
            __FUNCTION__, queueId);

    mpeos_mutexAcquire(queue->mutex);

    queue->num_waiting--;

    // This queue may have been deleted while we were waiting.  If it has been
    // invalidated, we release queue resources if there are no other threads
    // still waiting
    if (!queue->valid && queue->num_waiting == 0)
    {
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_EVENT,
                "%s: ID = %d, timeout = %d. "
                    "Queue has been deleted and we are the last waiting thread -- Freeing resources!\n",
                __FUNCTION__, queueId, timeout);

        mpeos_mutexRelease(queue->mutex);
        freeQueue(queue);
        return MPE_EEVENT;
    }

    // Did we timeout from our cond var wait?
    if (retval != MPE_SUCCESS)
    {
        mpeos_mutexRelease(queue->mutex);
        return MPE_ETIMEOUT;
    }

    // Retrieve the current event data
    *eventId = queue->events[queue->cur_event].event;
    if (eventData1 != NULL)
        *eventData1 = queue->events[queue->cur_event].event_data1;
    if (eventData2 != NULL)
        *eventData2 = queue->events[queue->cur_event].event_data2;
    if (eventFlag != NULL)
        *eventFlag = queue->events[queue->cur_event].event_flag;

    // Increment the current event index
    queue->cur_event = (queue->cur_event + 1) % MAX_EVENTS_PER_QUEUE;

    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_EVENT,
            "%s: ID = %d.  Num events = %d. Next event: EventId = %d, data1 = %p, data2 = %p, flag = %d\n",
            __FUNCTION__, queueId, numEvents(queue), *eventId, (eventData1
                    != NULL) ? *eventData1 : 0,
            (eventData2 != NULL) ? *eventData2 : 0,
            (eventFlag != NULL) ? *eventFlag : 0);

    // If the event queue is now empty, unset the condition var
    if (queueEmpty(queue))
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_EVENT,
                "%s: ID = %d.  Queue is now empty.\n", __FUNCTION__, queueId);
        mpeos_condUnset(queue->cond);
    }

    mpeos_mutexRelease(queue->mutex);

    return MPE_SUCCESS;
}
