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

#ifndef _MPE_FILTERMGR_BINDINGS_H_
#define _MPE_FILTERMGR_BINDINGS_H_

#include "mpe_sys.h"
#include "../mgr/include/filtermgr.h"

/* This macro will extract filter manager's function table from the master table */
#define mpe_filter_ftable ((mpe_FilterFtable*)(FTABLE[MPE_MGR_TYPE_FILTER]))

/* These macros redirect calls to filter mgr API to the
 * function pointers in filter mgr function table.
 */
#define mpe_filterInit (*(mpe_filter_ftable->mpe_filter_init_ptr))
#define mpe_filterShutdown (*(mpe_filter_ftable->mpe_filter_shutdown_ptr))
#define mpe_filterSetFilter (*(mpe_filter_ftable->mpe_filter_setFilter_ptr))
#define mpe_filterCancelFilter (*(mpe_filter_ftable->mpe_filter_cancelFilter_ptr))
#define mpe_filterGetSectionHandle (*(mpe_filter_ftable->mpe_filter_getSectionHandle_ptr))
#define mpe_filterRelease (*(mpe_filter_ftable->mpe_filter_release_ptr))
#define mpe_filterGetSectionSize (*(mpe_filter_ftable->mpe_filter_getSectionSize_ptr))
#define mpe_filterSectionRead (*(mpe_filter_ftable->mpe_filter_sectionRead_ptr))
#define mpe_filterSectionRelease (*(mpe_filter_ftable->mpe_filter_sectionRelease_ptr))

#define mpe_filterCreateFilterSpec (*(mpe_filter_ftable->mpe_filter_createFilterSpec_ptr))
#define mpe_filterDestroyFilterSpec (*(mpe_filter_ftable->mpe_filter_destroyFilterSpec_ptr))
#define mpe_filterCreateFilterComps (*(mpe_filter_ftable->mpe_filter_createFilterComps_ptr))
#define mpe_filterDestroyFilterComps (*(mpe_filter_ftable->mpe_filter_destroyFilterComps_ptr))
#define mpe_filterZeroSpec (*(mpe_filter_ftable->mpe_filter_zeroFilterSpec_ptr))
#define mpe_filterAlterSpec (*(mpe_filter_ftable->mpe_filter_alterFilterSpec_ptr))
#define mpe_filterSetFilterSpec (*(mpe_filter_ftable->mpe_filter_setFilterSpec_ptr))
#define mpe_filterRegisterAvailability (*(mpe_filter_ftable->mpe_filter_registerAvailability_ptr))
#define mpe_filterUnregisterAvailability (*(mpe_filter_ftable->mpe_filter_unregisterAvailability_ptr))

/**
 * Priorities used within the implementation when setting filters for specific purposes.
 * These priorities should be considered private to the implementation and not treated
 * special by MPEOS or below.
 * These definitions are subject to change based upon the whims of MPE and above.
 */
enum
{
    MPE_SF_FILTER_PRIORITY_EAS = 1, /**< Priority for IB/OOB Emergency Alert acquisition. */
    MPE_SF_FILTER_PRIORITY_SITP, /**< Priority for IB/OOB SI acquisition. */
    MPE_SF_FILTER_PRIORITY_XAIT, /**< Priority for OOB XAIT acquisition. */
    MPE_SF_FILTER_PRIORITY_AIT, /**< Priority for IB AIT acquisition. */
    MPE_SF_FILTER_PRIORITY_OC, /**< Priority for OC acquisition. */
    MPE_SF_FILTER_PRIORITY_DAVIC,
/**< Priority for Application-controlled DAVIC filtering. */
};

#endif /* _MPE_FILTERMGR_BINDINGS_H_ */
