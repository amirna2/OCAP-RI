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

#if !defined(_OS_EVENT_H)
#define _OS_EVENT_H

#include <mpe_types.h>
#include <mpeos_sync.h>

#ifdef __cplusplus
extern "C"
{
#endif

#define MAX_EVENTS_PER_QUEUE 64

/***
 * Event type definitions:
 */
typedef uint32_t os_Event;

/***
 * Event data structure
 */
typedef struct _linux_event
{
    os_Event event;
    void* event_data1;
    void* event_data2;
    uint32_t event_flag;

} linux_event_t;

/**
 * Structure representing our event queue.  Pending events are stored in
 * a circular array with one extra slot (used to determine when the array
 * is full)
 */
typedef struct _linux_event_queue
{
    mpe_Cond cond; // Main condition variable for this queue.  Threads waiting for
    // events will wait on this cond var

    mpe_Bool valid; // Set to false when this queue has been deleted, so that
    // waiting threads can exit safely

    uint32_t num_waiting; // The number of threads currently waiting for events.  This
    // allows us to properly de-alloc event queue memory

    char* name; // Descriptive queue name

    mpe_Mutex mutex; // Mutex to protect event array and indices

    uint32_t cur_event; // Index of the next available event
    uint32_t avail_event; // Index of the first available event slot

    // Circular array of events
    linux_event_t events[MAX_EVENTS_PER_QUEUE + 1];

} linux_event_queue_t;

/**
 * Event queue ID is just an index into an array of available queues
 */
typedef uint32_t os_EventQueue;

#ifdef __cplusplus
}
#endif
#endif /* _OS_EVENT_H */
