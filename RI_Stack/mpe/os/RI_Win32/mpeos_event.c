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
#include <mpe_types.h>    /* Resolve basic type references. */
#include <mpeos_event.h>  /* Resolve target specific event definitions. */
#include <mpeos_mem.h>
#include <mpe_error.h>

/* Routine that handles the common queue retrieval code between
 * the two methods mpeos_eventQueueWaitNext() and
 * mpeos_eventQueueNext().
 */
static mpe_Error eventQueueWaitNextInternal(mpe_EventQueue queueId,
        mpe_Event *eventId, void **optionalEventData1,
        void **optionalEventData2, uint32_t *optionalEventData3,
        uint32_t timeout)
{
    os_EventQueue queue = (os_EventQueue) queueId;
    queue_elem *elem;

    /* Sanity check of input parameters. */
    if ((queueId == NULL) || (eventId == NULL))
    {
        return MPE_EINVAL;
    }

    /* Check for an event in the queue. */
    if (WAIT_OBJECT_0 != WaitForSingleObject(queue->m_semaphore, timeout))
    {
        return MPE_ETIMEOUT;
    }

    /* Grab the next event off the top of the queue. */
    EnterCriticalSection(&queue->m_critSect);

    elem = queue->m_first;

    /* Is this the only element in the queue? */
    if (&(elem->elemNext) == queue->m_lastGoesHere)
    {
        /* We'll need to reset where to put the next new element to be */
        /* the first location. */
        queue->m_lastGoesHere = &(queue->m_first);
        queue->m_first = NULL;
    }
    else
    {
        /* Just bump the first pointer forward. */
        queue->m_first = elem->elemNext;
    }

    LeaveCriticalSection(&queue->m_critSect);

    /* Return the contents of the element. */
    *eventId = elem->event;
    if (optionalEventData1 != NULL)
    {
        *optionalEventData1 = elem->data1;
    }
    if (optionalEventData2 != NULL)
    {
        *optionalEventData2 = elem->data2;
    }
    if (optionalEventData3 != NULL)
    {
        *optionalEventData3 = elem->data3;
    }

    /* Free the event data structure. */
    mpeos_memFreeP(MPE_MEM_EVENT, elem);

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_eventQueueNew()</i> creates a new event queue.
 *
 * @param queueId Is a pointer for returning the identifier of the new event queue.
 * @param queueName Is a pointer to a constant string identifying this queue.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *         is returned.
 */
mpe_Error mpeos_eventQueueNew(mpe_EventQueue *queueId, const char *queueName)
{
    MPE_UNUSED_PARAM(queueName);

    os_EventQueue_s *queue;
    HANDLE sema;

    /* Sanity check of input parameters */
    if (queueId == NULL)
    {
        return MPE_EINVAL;
    }

    /* Create event queue protection semaphore */
    sema = CreateSemaphore(NULL, 0, 0x7FFFFFFF, NULL);
    if (sema == NULL)
    {
        return MPE_EMUTEX;
    }

    /* Allocate memory for the event queue structure */
    if (mpeos_memAllocP(MPE_MEM_EVENT, sizeof(os_EventQueue_s),
            (void **) &queue) != MPE_SUCCESS)
    {
        (void) CloseHandle(sema);
        return MPE_ENOMEM;
    }

    InitializeCriticalSection(&queue->m_critSect);
    queue->m_first = NULL;
    queue->m_lastGoesHere = &(queue->m_first);
    queue->m_semaphore = sema;

    *queueId = queue;

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_eventQueueDelete()</i> function will delete the specified event queue.
 *
 * @param queueId Is the identifier of the event queue.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *         is returned.
 */
mpe_Error mpeos_eventQueueDelete(mpe_EventQueue queueId)
{
    os_EventQueue_s *queue = (os_EventQueue_s*) queueId;
    mpe_Event eventId;

    /* Sanity check of input parameters. */
    if (queueId == NULL)
    {
        return MPE_EINVAL;
    }

    /* Empty the queue. */
    while (MPE_SUCCESS == eventQueueWaitNextInternal(queueId, &eventId, NULL,
            NULL, NULL, 0))
    { /*  Nothing to do. */
    }

    (void) CloseHandle(queue->m_semaphore);
    DeleteCriticalSection(&queue->m_critSect);
    mpeos_memFreeP(MPE_MEM_EVENT, queue);

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_eventQueueSend()</i> function will post the specified event to
 * specified queue with the optional event data, which is a void * pointer to any
 * additional data for the receiver of the event.
 *
 * @param queueId Is the target event queue identifier.
 * @param eventId Is the event identifier to queue.
 * @param optionalEventData1 Is a pointer to optional event data to deliver along
 *          with the specified event.
 * @param optionalEventData2 Is a pointer to optional event data to deliver along
 *          with the specified event.
 * @param optionalEventData3 Is a pointer to optional event data to deliver along
 *          with the specified event.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_eventQueueSend(mpe_EventQueue queueId, mpe_Event eventId,
        void *optionalEventData1, void *optionalEventData2,
        uint32_t optionalEventData3)
{
    os_EventQueue_s *queue = (os_EventQueue_s *) queueId;
    LONG prev;
    queue_elem *elem;

    /* Sanity check of input parameters. */
    if (queueId == NULL)
    {
        return MPE_EINVAL;
    }

    if (mpeos_memAllocP(MPE_MEM_EVENT, sizeof(queue_elem), (void **) &elem)
            != MPE_SUCCESS)
    {
        return MPE_ENOMEM;
    }

    elem->event = eventId;
    elem->data1 = optionalEventData1;
    elem->data2 = optionalEventData2;
    elem->data3 = optionalEventData3;
    elem->elemNext = NULL;

    EnterCriticalSection(&queue->m_critSect);

    /* Put the new element at the end of the queue. */
    *(queue->m_lastGoesHere) = elem;

    /* Reset the place to put the next element to be the next ptr of the new element. */
    queue->m_lastGoesHere = &(elem->elemNext);

    LeaveCriticalSection(&queue->m_critSect);

    /* Add one to the semaphore so waiters can proceed. */
    (void) ReleaseSemaphore(queue->m_semaphore, 1, &prev);

    return MPE_SUCCESS;
}

/**
 * The <i>mpeos_eventQueueNext()</i> function will retrieve the next event from
 * the specified event queue (non-blocking).  Returns via the specified pointers;
 * the identifier of the next event and any optional data.
 *
 * @param queueId Is the target event queue identifier.
 * @param eventId Is a pointer for returning the event identifier of the next
 *          event in the queue.
 * @param optionalEventData1 Is a void pointer for returning any optional event
 *          data to be delivered along with the event.
 * @param optionalEventData2 Is a void pointer for returning any optional event
 *          data to be delivered along with the event.
 * @param optionalEventData3 Is a int pointer for returning any optional event
 *          data to be delivered along with the event.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_eventQueueNext(mpe_EventQueue queueId, mpe_Event *eventId,
        void **optionalEventData1, void **optionalEventData2,
        uint32_t *optionalEventData3)
{
    /* Use the internal common method without blocking. */
    return eventQueueWaitNextInternal(queueId, eventId, optionalEventData1,
            optionalEventData2, optionalEventData3, 0);
}

/**
 * The <i>mpeos_eventQueueWaitNext ()</i> function will retrieve the next event
 * from the specified event queue (blocking with timeout).  Returns via the
 * specified pointers; the identifier of the next event and any optional data.
 *
 * @param queueId Is the target event queue identifier.
 * @param eventId Is a pointer for returning the event identifier of the next
 *          event in the queue.
 * @param optionalEventData1 Is a void pointer for returning any optional event
 *          data to be delivered along with the event.
 * @param optionalEventData2 Is a void pointer for returning any optional event
 *          data to be delivered along with the event.
 * @param optionalEventData3 Is a int pointer for returning any optional event
 *          data to be delivered along with the event.
 * @param timeout Is the maximum time period in milliseconds to wait for the
 *          next event.  A time period of zero means wait indefinitely.
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/>
 *          is returned.
 */
mpe_Error mpeos_eventQueueWaitNext(mpe_EventQueue queueId, mpe_Event *eventId,
        void **optionalEventData1, void **optionalEventData2,
        uint32_t *optionalEventData3, uint32_t timeout)
{
    /* A zero timeout is infinite to this method.
     * (Passing zero to the internal method would actually not block at all.) */
    if (timeout == 0)
    {
        timeout = INFINITE;
    }

    /* Use the internal common method. */
    return eventQueueWaitNextInternal(queueId, eventId, optionalEventData1,
            optionalEventData2, optionalEventData3, timeout);
}
