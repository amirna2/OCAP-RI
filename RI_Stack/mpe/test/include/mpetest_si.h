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

#ifndef _MPETEST_SI_H_
#define _MPETEST_SI_H_ 1

/**
 * MPE / MPEOS function names are re-defined here using macros, in order to
 * support MPE or MPEOS tests using the same test code.
 *
 * If #define TEST_MPEOS is defined, then tests will be for MPEOS, else MPE.
 */

#include "../mgr/include/simgr.h"

#ifdef TEST_MPEOS
# include <mpeos_dbg.h>
# include <mpeos_si.h>
# include "mgrdef.h"
# define MPETEST_SI(x)  mpeos_ ## x
#else
# include <mpe_dbg.h>
# include "mpe_sys.h"
# include <mpe_si.h>
# include "mgrdef.h"
# define MPETEST_SI(x)  mpe_ ## x
#endif /* TEST_MPEOS */

/*
 #define si_shutdown					MPETEST_SI(siShutdown)
 #define GetServiceIdByComponent		MPETEST_SI(siGetServiceIdByComponent)
 #define GetAllServiceIdByComponent  MPETEST_SI(siGetAllServiceIdByComponent)
 #define GetComponent				MPETEST_SI(siGetComponent)
 #define GetComponentRangeByValue	MPETEST_SI(siGetComponentRangeByValue)
 #define GetComponentRangeByCount	MPETEST_SI(siGetComponentRangeByCount)
 */

/* New SI APIs */
#define getPidByAssociationTag		        MPETEST_SI(siGetPidByAssociationTag)
#define getPidByCarouselID	                MPETEST_SI(siGetPidByCarouselID)
#define getPidByComponentTag                MPETEST_SI(siGetPidByComponentTag)
#define getDefaultObjectCarousel			MPETEST_SI(siGetDefaultObjectCarousel)

#define getServiceHandleBySourceId						MPETEST_SI(siGetServiceHandleBySourceId)
#define getServiceHandleByFrequencyProgramNumber		MPETEST_SI(siGetServiceHandleByFrequencyProgramNumber)
#define getServiceHandleByServiceName					MPETEST_SI(GetServiceHandleByServiceName)

#define getSiHandleByUniqueifier		    MPETEST_SI(siGetSiHandleByUniqueifier)
#define createUniqueifierBySiHandle		    MPETEST_SI(siCreateUniqueifierBySiHandle)
#define deleteUniqueifier           	    MPETEST_SI(siDeleteUniqueifier)

#define releaseSiHandle                     MPETEST_SI(siReleaseSiHandle)

#define getNumberOfSourceIds				MPETEST_SI(siGetNumberOfSourceIds)
#define getAllSourceIds						MPETEST_SI(siGetAllSourceIds)

#define getTransportStreamId				MPETEST_SI(siGetTransportStreamId)
#define getElementaryStreams				MPETEST_SI(siGetElementaryStreams)

#define getServiceType						MPETEST_SI(siGetServiceType)
#define getServiceName						MPETEST_SI(siGetServiceName)
#define getServiceNumber                    MPETEST_SI(siGetServiceNumber)
#define getSourceId							MPETEST_SI(siGetSourceId)
#define getFreqProgramNumber                MPETEST_SI(siGetFreqProgramNumber)

#define getPidByComponentName               MPETEST_SI(siGetPidByComponentName)
#define getPidByStreamType					MPETEST_SI(siGetPidByStreamType)
#define getPidByLanguage					MPETEST_SI(siGetPidByLanguage)
#define getLanguageByPid					MPETEST_SI(siGetLanguageByPid)
#define getNumberOfPids						MPETEST_SI(siGetNumberOfPids)
#define getAllPids							MPETEST_SI(siGetAllPids)

#define getComponentHandleByPid					MPETEST_SI(siGetComponentHandleByPid)
#define getComponentHandleByName				MPETEST_SI(siGetComponentHandleByName)
#define getComponentHandleByTag					MPETEST_SI(siGetComponentHandleByTag)
#define getComponentHandleForDefaultCarousel	MPETEST_SI(siGetComponentHandleForDefaultCarousel)
#define getComponentHandleByCarouselId			MPETEST_SI(siGetComponentHandleByCarouselId)

#define getComponentPid						MPETEST_SI(siGetComponentPid)
#define getComponentName					MPETEST_SI(siGetComponentName)
#define getComponentTag						MPETEST_SI(siGetComponentTag)
#define getCarouselId						MPETEST_SI(siGetCarouselId)
#define getComponentLanguage				MPETEST_SI(siGetComponentLanguage)
#define getStreamType						MPETEST_SI(siGetStreamType)

#define getAllTransportStreamIds			MPETEST_SI(siGetAllTransportStreamIds)
#define getAllServicesByTransportStreamId	MPETEST_SI(siGetAllServicesByTransportStreamId)

#define getSiHandleByUniqueifier			MPETEST_SI(siGetSiHandleByUniqueifier)
#define createUniqueifierBySiHandle			MPETEST_SI(siCreateUniqueifierBySiHandle)
#define deleteUniqueifier					MPETEST_SI(siDeleteUniqueifier)

#endif /* _MPETEST_SI_H_ */ 
