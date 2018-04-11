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
#ifndef _VBIMGR_H_
#define _VBIMGR_H_

#include <mpe_types.h>
#include <mpe_error.h>
#include <mpeos_vbi.h>

#ifdef __cplusplus
extern "C"
{
#endif

void mpe_vbiSetup(void);
void mpe_vbiInit(void);
void mpe_vbiShutdown(void);

/******************************************
 * Vbi Manager function prototypes
 *****************************************/
typedef struct mpe_VbiFtable
{

    void (*mpe_vbi_init_ptr)(void);

    void (*mpe_vbi_shutdown_ptr)(void);
    mpe_Error (*mpe_vbi_filterStart_ptr)(mpe_VBISource *filterSource,
            mpe_FilterSpec *filterSpec, mpe_EventQueue queueId, void *act,
            mpe_VBIDataFormat dataFormat, uint32_t dataUnitSize,
            uint32_t bufferSize, uint32_t dataUnitNotificationThreshold,
            uint32_t flags, mpe_VBIFilterSession *filterSession);
    mpe_Error (*mpe_vbi_filterStop_ptr)(mpe_VBIFilterSession filterSession);
    mpe_Error (*mpe_vbi_filterRelease_ptr)(mpe_VBIFilterSession filterSession);
    mpe_Error (*mpe_vbi_filterReadData)(mpe_VBIFilterSession filterSession,
            uint32_t offset, uint32_t byteCount, uint32_t flags,
            uint8_t * buffer, uint32_t * bytesRead);
    mpe_Error (*mpe_vbi_filterSetParam)(mpe_VBIFilterSession session,
            mpe_VBIParameter parameter, uint32_t value);
    mpe_Error (*mpe_vbi_filterGetParam)(mpe_VBIFilterSession session,
            mpe_VBIParameter parameter, uint32_t * value);
    mpe_Error (*mpe_vbi_getParam)(mpe_VBIParameter parameter, uint32_t option1,
            uint32_t option2, uint32_t * outval);

    mpe_Error (*mpe_vbi_registerAvailability_ptr)(mpe_EventQueue q, void * act);
    mpe_Error (*mpe_vbi_unregisterAvailability_ptr)(mpe_EventQueue q,
            void * act);
} mpe_VbiFtable;

#ifdef __cplusplus
}
;
#endif

#endif /* _VBIMGR_H_ */
