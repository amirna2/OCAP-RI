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

#ifndef _SIUTIL_H_
#define _SIUTIL_H_

#include "mpe_types.h"

#ifdef __cplusplus
extern "C"
{
#endif

// --------------------------------------------------------------
//                          LINKED LISTS
// --------------------------------------------------------------

/*
 * Format of a list link.
 */
typedef struct link LINK;
struct link
{
    LINK * nextp; /* List foreward pointer */
    LINK * prevp; /* List back pointer */
    struct linkhdr * headerp; /* Pointer to list link is in */
    void * datap; /* Pointer to arbitrary data */
};

/*
 * Format of a list header.
 */
typedef struct linkhdr LINKHD;
struct linkhdr
{
    LINK * headp; /* Pointer to first link */
    LINK * tailp; /* pointer to last link */
    unsigned long nlinks; /* Number of links in the list */
};

LINKHD * llist_create(void);
void llist_free(LINKHD *headerp);
LINK * llist_mklink(void * datap);
void llist_freelink(LINK *linkp);
void llist_append(LINKHD *headerp, LINK *linkp);
LINK * llist_first(LINKHD * headerp);
LINK * llist_after(LINK * linkp);
LINK * llist_last(LINKHD * headerp);
void llist_rmlink(LINK * linkp);
void llist_rmfirst(LINKHD * headerp);
void llist_rmafter(LINK * afterp);
void llist_rmlast(LINKHD * headerp);
LINK * llist_setdata(LINK * linkp, void * datap);
void * llist_getdata(LINK * linkp);
unsigned long llist_cnt(LINKHD * headerp);
LINK * llist_linkof(LINKHD * headerp, void * datap);
LINK * llist_nextlinkof(void * datap, LINK * afterp);
LINK * llist_getNodeAtIndex(LINKHD * headerp, unsigned long index);
LINKHD * llist_hdrof(LINK * lp);

typedef LINKHD * ListSI;

mpe_Error cache_si_entries(const char *);
mpe_Error load_si_entries(const char *) ;
mpe_Error cache_sns_entries(const char *);
mpe_Error load_sns_entries(const char *) ;
void get_frequency_range(uint32_t *freqArr, int count, uint32_t *minFreq, uint32_t *maxFreq );

mpe_Error verify_si_cache_files_exist(const char *siFileLocation, const char *snsFileLocation);
mpe_Error write_crc_for_si_and_sns_cache(const char* pSICache, const char* pSISNSCache);
int verify_version_and_crc (const char *siOOBCacheLocation, const char *siOOBSNSCacheLocation);

// --------------------------------------------------------------
//                          CRC UTILITY FUNCTIONS
// --------------------------------------------------------------

void init_mpeg2_crc(void);

uint32_t calc_mpeg2_crc(uint8_t * data, uint32_t len);

#define MPE_SI_CACHE_FILE_VERSION 0x101u

#ifdef __cplusplus
}
;
#endif

#endif /* _SIUTIL_H_ */
