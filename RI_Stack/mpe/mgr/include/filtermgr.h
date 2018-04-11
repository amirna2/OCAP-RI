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
#ifndef _FILTERMGR_H_
#define _FILTERMGR_H_

#include "mpe_types.h"
#include "mpe_error.h"
#include "mpeos_filter.h"

#ifdef __cplusplus
extern "C"
{
#endif

void mpe_filterSetup(void);
void mpe_filterInit(void);
void mpe_filterShutdown(void);

/** Identifies a common MPEG field for a table filter alteration */
typedef enum
{
    MPE_FILTERFIELD_TABLE_ID,
    MPE_FILTERFIELD_EXT_TABLE_ID,
    MPE_FILTERFIELD_VERSION,
    MPE_FILTERFIELD_SECTION,
    MPE_FILTERFIELD_LAST
} mpe_FilterField;

/** Identifies whether a common MPEG table field should be Matched or Not Matched */
typedef enum
{
    MPE_MATCH, MPE_DONT_MATCH, MPE_DONT_CARE
} mpe_FieldPolarity;

/******************************************
 * Filter Manager function prototypes
 *****************************************/
typedef struct mpe_FilterFtable
{
    void (*mpe_filter_init_ptr)(void);

    void (*mpe_filter_shutdown_ptr)(void);
    mpe_Error (*mpe_filter_setFilter_ptr)(const mpe_FilterSource *filterSource,
            const mpe_FilterSpec *filterSpec, mpe_EventQueue queueId,
            void *act, uint8_t filterPriority, uint32_t timesToMatch,
            uint32_t flags, uint32_t * uniqueID);
    mpe_Error (*mpe_filter_getSectionSize_ptr)(
            mpe_FilterSectionHandle sectionHandle, uint32_t *size);
    mpe_Error (*mpe_filter_getSectionHandle_ptr)(uint32_t uniqueID,
            uint32_t flags, mpe_FilterSectionHandle *sectionHandle);
    mpe_Error (*mpe_filter_cancelFilter_ptr)(uint32_t uniqueID);
    mpe_Error (*mpe_filter_release_ptr)(uint32_t uniqueID);
    uint32_t (*mpe_filter_sectionRead_ptr)(
            mpe_FilterSectionHandle sectionHandle, uint32_t offset,
            uint32_t byteCount, uint32_t flags, uint8_t *buffer,
            uint32_t *bytesRead);
    mpe_Error (*mpe_filter_sectionRelease_ptr)(
            mpe_FilterSectionHandle sectionHandle);
    mpe_Error (*mpe_filter_createFilterSpec_ptr)(uint8_t posFilterLen,
            uint8_t negFilterLen, mpe_FilterSpec **ppFilter);
    mpe_Error (*mpe_filter_destroyFilterSpec_ptr)(mpe_FilterSpec *pFilter);
    mpe_Error (*mpe_filter_createFilterComps_ptr)(uint8_t posFilterLen,
            uint8_t negFilterLen, mpe_FilterSpec *pFilter);
    mpe_Error (*mpe_filter_destroyFilterComps_ptr)(mpe_FilterSpec *pFilter);
    mpe_Error (*mpe_filter_zeroFilterSpec_ptr)(mpe_FilterSpec *pFilter);
    mpe_Error (*mpe_filter_alterFilterSpec_ptr)(mpe_FilterSpec *pFilter,
            mpe_FilterField filtField, uint32_t filterVal,
            mpe_FieldPolarity posNeg);
    mpe_Error (*mpe_filter_setFilterSpec_ptr)(mpe_FilterSpec *pFilter,
            uint32_t tableId, mpe_FieldPolarity matchTableIdExt,
            uint32_t tableIdExt, mpe_FieldPolarity matchVer, uint32_t version,
            mpe_FieldPolarity matchSect, uint32_t section);
    mpe_Error (*mpe_filter_registerAvailability_ptr)(mpe_EventQueue queueId,
            void *act);
    mpe_Error (*mpe_filter_unregisterAvailability_ptr)(mpe_EventQueue queueId,
            void *act);
} mpe_FilterFtable;

#ifdef __cplusplus
}
;
#endif

#endif /* _FILTERMGR_H_ */

