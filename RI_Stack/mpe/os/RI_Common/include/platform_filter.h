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

#if !defined(PLATFORM_FILTER_H)
#define PLATFORM_FILTER_H

#include <mpe_types.h>      /* Resolve basic types. */
#include <mpe_error.h>      /* Resolve error codes. */
#include <mpeos_filter.h>   /* for filter definitions */
#include <mpeos_thread.h>    /* Resolve thread definitions. */

#include <glib.h>

#include <ri_section_filter.h>

#ifdef __cplusplus
extern "C"
{
#endif

////////////////////////////////////////////////////////////////////////////
// TYPE DEFINITIONS
////////////////////////////////////////////////////////////////////////////

typedef enum ri_sf_requestState
{
    RI_SF_REQSTATE_INVALID,
    RI_SF_REQSTATE_READY,
    RI_SF_REQSTATE_MATCHED,
    RI_SF_REQSTATE_CANCELLED
} ri_sf_requestState;

/** Stores parameters needed for fulfilling a section acquisition/filtering 
 * request. A ri_sf_sectionRequest instance is created for every call to 
 * mpeos_filterSetFilter() and serves as the binding mechanism between a 
 * RI Stream, associated hardware & software filters, and the sections which 
 * are matched. The lifetime of a ri_sf_sectionRequest is directly tied to 
 * the lifetime of a filter which is identified by a uniqueID.
 */
typedef struct ri_sf_sectionRequest
{
    // Pipeline filter associated with this request
    ri_section_filter_t* pipelineFilter;

    // Current filter state
    ri_sf_requestState state;

    // Client event queue data
    mpe_EventQueue requestorQueue;
    void* requestorACT;

    // Filter priority
    uint8_t priority;

    // When TRUE, the matchesRemaining field is ignored.  This filter
    // will run until it is explicitly canceled
    mpe_Bool runTillCanceled;

    // This keeps track of how many matches are remaining.
    // If matchesRemaining == 0 and request is in the READY
    // state, then this filter will match sections until
    // cancelled
    uint32_t matchesRemaining;

    // Incoming section queue.  As sections are received from the
    // platform, they are placed in queue waiting for the client to 
    // retrieve them
    GQueue* incomingSections;

    // MPE tuner ID that this request is associated with
    uint32_t tunerID;

} ri_sf_sectionRequest;

/**
 * This structure represents section data delivered by the
 * platform
 */
typedef struct ri_sf_section
{
    // Native RI platform filter ID
    uint32_t sectionID;

    // Section data and length
    uint8_t* sectionData;
    uint16_t sectionLength;
} ri_sf_section;

/**
 * This MPEOS-private API is used to close out existing filter requests
 * when a new tune is about to be initiated
 */
void sf_tuneInitiated(uint32_t tunerID);

#ifdef __cplusplus
}
#endif

#endif /* PLATFORM_FILTER_H */
