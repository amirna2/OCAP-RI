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

#ifndef _FDC_H_
#define _FDC_H_

#include "sectionutils.h"

// OOB table IDs
#define XAIT_TID 0x74
#define NIT_TID  0xC2
#define NTT_TID  0xC3
#define SVCT_TID 0xC4
#define STT_TID  0xC5
#define MGT_TID  0xC7
#define LVCT_TID 0xC9
#define RRT_TID  0xCA
#define EAS_TID  0xD8
#define PAT_TID  0x00
#define PMT_TID  0x02

// OCAP OOB Table PID
#define OOB_PID  0x1FFC

#define DSMCC_TID_MPDATA   0x3A
#define DSMCC_TID_UNMSG    0x3B
#define DSMCC_TID_DDMSG    0x3C
#define DSMCC_TID_STRMDESC 0x3D
#define DSMCC_TID_PRIV     0x3E
#define DSMCC_TID_ADDR     0x3F

#define DSMCC_ES_TYPEA 0x0A  // DSMCC elementary stream type in PMT
#define DSMCC_ES_TYPEB 0x0B  // DSMCC elementary stream type in PMT
#define DSMCC_ES_TYPEC 0x0C  // DSMCC elementary stream type in PMT
#define DSMCC_ES_TYPED 0x0D  // DSMCC elementary stream type in PMT

extern SectionCache* GetFdcSectionCache(void);
extern ri_bool FdcInit();
extern void FdcExit();
extern ri_bool AddFdcSectionsFromFile(char *fdcFilePath);
extern ri_bool AddFdcSectionToFile(char *file, uint8_t *buf, size_t bytes);
extern ri_bool GetFdcSection(int section, unsigned char *buf, int bufsize);
extern void FreeFdcSection(int section);
extern int GetNumFdcSections();

#endif

