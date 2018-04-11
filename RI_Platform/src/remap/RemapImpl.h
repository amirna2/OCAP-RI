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

// Private REMAP Definitions

#ifndef _REMAP_IMPL_H
#define _REMAP_IMPL_H "$Rev: 141 $"

#include "RemapIntf.h"

typedef enum
{

    RemapPatStateIn,
    RemapPatState00,
    RemapPatState01,
    RemapPatState02,
    RemapPatState03,
    RemapPatState04,
    RemapPatState05,
    RemapPatState06,
    RemapPatState07,
    RemapPatState08,
    RemapPatState09,
    RemapPatState10,
    RemapPatState11,
    RemapPatState12,
    RemapPatState13,
    RemapPatState14,
    RemapPatState15,
    RemapPatState16,
    RemapPatState17,
    RemapPatState18,
    RemapPatState19,
    RemapPatState20,
    RemapPatState21,
    RemapPatState22,
    RemapPatState23,
    RemapPatState24,

} RemapPatState;

typedef enum
{

    RemapPatEntState00,
    RemapPatEntState01,
    RemapPatEntState02,
    RemapPatEntState03,

} RemapPatEntState;

typedef enum
{

    RemapPmtStateIn,
    RemapPmtState00,
    RemapPmtState01,
    RemapPmtState02,
    RemapPmtState03,
    RemapPmtState04,
    RemapPmtState05,
    RemapPmtState06,
    RemapPmtState07,
    RemapPmtState08,
    RemapPmtState09,
    RemapPmtState10,
    RemapPmtState11,
    RemapPmtState12,
    RemapPmtState13,
    RemapPmtState14,
    RemapPmtState15,
    RemapPmtState16,
    RemapPmtState17,

} RemapPmtState;

typedef enum
{

    RemapPmtEntState00,
    RemapPmtEntState01,
    RemapPmtEntState02,
    RemapPmtEntState03,
    RemapPmtEntState04,
    RemapPmtEntState05,

} RemapPmtEntState;

typedef enum
{

    RemapPmtDesState00,
    RemapPmtDesState01,
    RemapPmtDesState02,
    RemapPmtDesState03,
    RemapPmtDesState04,
    RemapPmtDesState05,
    RemapPmtDesState06,
    RemapPmtDesState07,

} RemapPmtDesState;

typedef struct RemapPids
{
    RemapPid newPid[REMAP_NULL_PID_VALUE + 1];

} RemapPids;

#ifdef SAVE_PAT_AND_PMT

typedef struct
{
    RemapProg prog;
    RemapPid pid;

}SavePat;

#endif

typedef struct RemapHandleImpl
{
    // PID filtering and remapping parameters
    RemapPids * pRemapPids;

    // PMT rewriting parameters
    RemapPid oldPmtPid;

    // PAT rewriting parameters
    RemapProg programNumber; // Input, the Program Number
    RemapPid newPmtPid;
    RemapPid padPid;

    // PMT processing parameters
    RemapPmtState remapPmtState;
    RemapPmtDesState remapPmtDesState;
    RemapPmtEntState remapPmtEntState;
    unsigned long pmtTemp;
    unsigned short pmtCnt0, pmtMax0; // Used to iterate over descriptor bytes
    unsigned short pmtCnt1, pmtMax1; // Used to iterate over descriptors
    unsigned short pmtCnt2, pmtMax2; // Used to iterate over the entries
    unsigned long pmtCrc;

    // PAT processing parameters
    RemapPatState remapPatState;
    RemapPatEntState remapPatEntState;
    unsigned long patTemp;
    unsigned short patCnt0, patMax0; // Used to iterate over the entry bytes
    unsigned long patCrc;

    // Handles and pointers to the previous data
    NumPackets prevNumPackets;
    RemapPacket ** pPrevPointers;
    unsigned char * pPidMsb;
    unsigned char pidMsb;
    unsigned char pad[3];
    RemapBoolean msbPtrGotSet;
    void * prevToken;

#ifdef SAVE_PAT_AND_PMT
    SavePat * pSavePat;
    unsigned short nextPat;
    unsigned char streamType;
    unsigned char pad2;
    RemapBoolean donePat;
    RemapPid videoPid;
    short pad3;
#endif

} RemapHandleImpl;

const char * DescriptorToString(unsigned char descriptor);
const char * StreamTypeToString(unsigned char streamType);

#endif
