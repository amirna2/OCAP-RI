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

/** \file
 *
 * \brief Common include file for section filtering tests
 *
 * \author Ric Yeates, Vidiom Systems Corp.
 *
 */

#ifndef _TEST_FILTER_SECTIONFILTERING_H_
#define _TEST_FILTER_SECTIONFILTERING_H_

#include <test_filter.h>

typedef struct ProgramPIDMap_t ProgramPIDMap_t;
typedef struct TableHeader_t TableHeader_t;
typedef struct StreamInfo_t StreamInfo_t;

struct ProgramPIDMap_t
{
    uint16_t program_number;
    uint16_t pid; // may be network pid
    ProgramPIDMap_t *next;
};

struct StreamInfo_t
{
    uint8_t stream_type;
    uint16_t elementary_PID;
    uint16_t ES_info_length;
    uint8_t *stream_info;
    StreamInfo_t *next;
};

struct TableHeader_t
{
    uint16_t table_id;
    uint16_t section_syntax_indicator;
    uint16_t section_length;
};

typedef struct PAT_t
{
    TableHeader_t header;
    uint16_t transport_stream_id;
    uint8_t version_number;
    uint8_t current_next_indicator;
    uint8_t section_number;
    uint8_t last_section_number;
    ProgramPIDMap_t *map;
} PAT_t;

typedef struct PMT_t
{
    TableHeader_t header;
    uint16_t program_number;
    uint8_t version_number;
    uint8_t current_next_indicator;
    uint8_t section_number;
    uint8_t last_section_number;
    uint16_t PCR_PID;
    uint16_t program_info_length;
    uint8_t *program_info;
    StreamInfo_t *streams;
} PMT_t;

typedef struct SectionFilter_t SectionFilter_t;
typedef struct SectionContainer_t SectionContainer_t;

typedef enum
{
    STATE_INIT = 1, // specification has been allocated
    STATE_READY, // filter is currently active
    STATE_CANCELLED
// cancelled for some reason
} FilterState_t;

#define SFF_RESERVED	0x0000ffff	// flags passed directly to filterSetFilter
#define SFF_MANUAL_GET	0x00010000	// prevent event thread from doing filterGetSectionHandle
#define SFF_CANCEL		0x00020000	// expect MPE_SF_EVENT_FILTER_CANCELLED
struct SectionContainer_t
{
    mpe_FilterSectionHandle handle; // section's handle
    SectionContainer_t *next; // next section
};

struct SectionFilter_t
{
    uint32_t uid; // unique ID for the filter
    mpe_FilterSource *source; // filter's source
    mpe_FilterSpec *spec; // filter spec for the filter
    uint8_t priority; // filter priority
    uint32_t timesToMatch; // times to match
    mpe_Bool infinite; // timesToMatch mean anything?
    uint32_t flags; // sf flags and filterSetFilter flags
    FilterState_t state; // current state of the filter
    mpe_Event eventId; // most recent event for filter
    SectionContainer_t *sects; // sections returned by filter
    SectionFilter_t *next; // next filter in DB
};

typedef struct sfData_t
{
    mpe_Bool inized; // has the sf sub-system been initialized

    mpe_EventQueue que; // event queue

    mpe_ThreadId eventThreadID; // event thread ID
    mpe_Bool terminate; // event thread should terminate
    mpe_Bool terminated; // event thread has terminated

    mpe_Mutex mutex; // lock for list of filters
#define LOCK() mutexAcquire(sfData->mutex)
#define UNLOCK() mutexRelease(sfData->mutex)

    SectionFilter_t *filters; // list of all filters

    CuTest *tc; // test case we're working on
} sfData_t;

/*
 * Prototypes:
 */
mpe_Error GoToOOBChannel(mpe_FilterSource **pFilterSource);
mpe_Error GoToInbandChannel(mpe_FilterSource **pFilterSource);

// Utils
void SectionFilter_HexDump(int bufferSize, int8_t* buffer);
mpe_Error PATSectionFilter(mpe_FilterSource *source, mpe_FilterSpec **pSpec);
mpe_Error PATSectionParse(mpe_FilterSectionHandle sect, PAT_t **returnPAT);
void PATSectionDump(PAT_t *pat);
void PATSectionFree(PAT_t *pat);
mpe_Error PMTSectionFilter(mpe_FilterSource *source, uint16_t program_number,
        mpe_FilterSpec **pSpec);
mpe_Error PMTSectionParse(mpe_FilterSectionHandle sect, PMT_t **returnPMT);
void PMTSectionDump(PMT_t *pat);
void PMTSectionFree(PMT_t *pat);
char *translateError(mpe_Error err);
char *translateEvent(mpe_Event evt);
char *translateStreamType(uint8_t stream_type);

void test_getTestSuite_sectionFiltering_SimpleFilter(CuSuite* suite);
void test_getTestSuite_sectionFiltering_Basic(CuSuite* suite);
void test_getTestSuite_sectionFiltering_Dump(CuSuite* suite);
void test_getTestSuite_sectionFiltering_Negative(CuSuite* suite);
void test_getTestSuite_sectionFiltering_Priority(CuSuite* suite);
void test_getTestSuite_sectionFiltering_Cancel(CuSuite* suite);

mpe_Error sfInit(sfData_t *sfData, CuTest *tc);
mpe_Error sfTerm(sfData_t *sfData);
mpe_Error sfNew(sfData_t *sfData, mpe_FilterSource *source,
        mpe_FilterSpec *spec, uint8_t priority, uint32_t timesToMatch,
        uint32_t flags, SectionFilter_t **filter);
mpe_Error sfStart(sfData_t *sfData, SectionFilter_t *filter);
mpe_Error sfGetSection(sfData_t *sfData, SectionFilter_t *filter,
        mpe_FilterSectionHandle *sect);
mpe_Error sfDumpSection(sfData_t *sfData, mpe_FilterSectionHandle sect);
mpe_Error sfDelete(sfData_t *sfData, SectionFilter_t *filter);
mpe_Error sfCancel(sfData_t *sfData, SectionFilter_t *filter);

#endif // _TEST_FILTER_SECTIONFILTERING_H_
