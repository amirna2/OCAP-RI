#if !defined(_MPEOS_EVENT_H)
#define _MPEOS_EVENT_H
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
#include <mpe_types.h>	  /* Resolve basic type references. */
#include <os_event.h>	  /* Resolve target specific event definitions. */

#ifdef __cplusplus
extern "C"
{
#endif

/***
 * Event type definitions:
 */
typedef os_Event mpe_Event; /* Event identifier type. */
typedef os_EventQueue mpe_EventQueue; /* Event queue type. */

/***
 * Event Queue API prototypes:
 */

/**
 * <i>mpeos_eventQueueNew()</i> function will create a new event queue.
 *
 * @param queueId is a pointer for returning the identifier of the new event queue.
 * @param queueName is a pointer to a constant string identifying this queue.
 *        This string is used for debugging purposes only and is not meant to uniquely
 *        identify a queue.  No guarantees are made as to the uniqueness of the
 *        given name across all queues
 *
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_eventQueueNew(mpe_EventQueue *queueId, const char *queueName);

/**
 * The <i>mpeos_eventQueueDelete()</i> function will delete the specified event queue.
 *
 * @param queueId is the identifier of the event queue.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_eventQueueDelete(mpe_EventQueue queueId);

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
 * @param optionalEventData3 is a pointer to optional event data to deliver along 
 *          with the specified event.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_eventQueueSend(mpe_EventQueue queueId, mpe_Event eventId,
        void *optionalEventData1, void *optionalEventData2,
        uint32_t optionalEventData3);

/**
 * The <i>mpeos_eventQueueNext()</i> function will retrieve the next event from 
 * the specified event queue (non-blocking).  Returns via the specified pointers; 
 * the identifier of the next event and any optional data.
 *
 * @param queueId is the target event queue identifier.
 * @param eventId is a pointer for returning the event identifier of the next
 *          event in the queue.
 * @param optionalEventData1 is a void pointer for returning any optional event
 *          data to be delivered along with the event.
 * @param optionalEventData2 is a void pointer for returning any optional event
 *          data to be delivered along with the event.
 * @param optionalEventData3 is a int pointer for returning any optional event
 *          data to be delivered along with the event.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_eventQueueNext(mpe_EventQueue queueId, mpe_Event *eventId,
        void **optionalEventData1, void **optionalEventData2,
        uint32_t *optionalEventData3);

/**
 * The <i>mpeos_eventQueueWaitNext ()</i> function will retrieve the next event 
 * from the specified event queue (blocking with timeout).  Returns via the 
 * specified pointers; the identifier of the next event and any optional data.
 *
 * @param queueId is the target event queue identifier.
 * @param eventId is a pointer for returning the event identifier of the next
 *          event in the queue.
 * @param optionalEventData1 is a void pointer for returning any optional event
 *          data to be delivered along with the event.
 * @param optionalEventData2 is a void pointer for returning any optional event
 *          data to be delivered along with the event.
 * @param optionalEventData3 is a int pointer for returning any optional event
 *          data to be delivered along with the event.
 * @param timeout is the maximum time period in milliseconds to wait for the 
 *          next event.  A time period of zero means wait indefinitely.
 * @return The MPE error code if the create fails, otherwise <i>MPE_SUCCESS<i/> 
 *          is returned.
 */
mpe_Error mpeos_eventQueueWaitNext(mpe_EventQueue queueId, mpe_Event *eventId,
        void **optionalEventData1, void **optionalEventData2,
        uint32_t *optionalEventData3, uint32_t timeout);

#ifdef __cplusplus
}
#endif
#endif /* _MPEOS_EVENT_H */
