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

#if !defined(_MPEOS_SI_H)
#define _MPEOS_SI_H

#ifdef __cplusplus
extern "C"
{
#endif

#include <mpe_types.h>  /* Resolve basic type references. */
#include <mpe_error.h>
#include <mpeos_event.h>

typedef enum mpe_SiElemStreamType
{
    MPE_SI_ELEM_MPEG_1_VIDEO = 0x01,
    MPE_SI_ELEM_MPEG_2_VIDEO = 0x02,
    MPE_SI_ELEM_MPEG_1_AUDIO = 0x03,
    MPE_SI_ELEM_MPEG_2_AUDIO = 0x04,
    MPE_SI_ELEM_MPEG_PRIVATE_SECTION = 0x05,
    MPE_SI_ELEM_MPEG_PRIVATE_DATA = 0x06,
    MPE_SI_ELEM_MHEG = 0x07,
    MPE_SI_ELEM_DSM_CC = 0x08,
    MPE_SI_ELEM_H_222 = 0x09,
    MPE_SI_ELEM_DSM_CC_MPE = 0x0A,
    MPE_SI_ELEM_DSM_CC_UN = 0x0B,
    MPE_SI_ELEM_DSM_CC_STREAM_DESCRIPTORS = 0x0C,
    MPE_SI_ELEM_DSM_CC_SECTIONS = 0x0D,
    MPE_SI_ELEM_AUXILIARY = 0x0E,
    MPE_SI_ELEM_AAC_ADTS_AUDIO = 0x0F,
    MPE_SI_ELEM_ISO_14496_VISUAL = 0x10,
    MPE_SI_ELEM_AAC_AUDIO_LATM = 0x11,
    MPE_SI_ELEM_FLEXMUX_PES = 0x12,
    MPE_SI_ELEM_FLEXMUX_SECTIONS = 0x13,
    MPE_SI_ELEM_SYNCHRONIZED_DOWNLOAD = 0x14,
    MPE_SI_ELEM_METADATA_PES = 0x15,
    MPE_SI_ELEM_METADATA_SECTIONS = 0x16,
    MPE_SI_ELEM_METADATA_DATA_CAROUSEL = 0x17,
    MPE_SI_ELEM_METADATA_OBJECT_CAROUSEL = 0x18,
    MPE_SI_ELEM_METADATA_SYNCH_DOWNLOAD = 0x19,
    MPE_SI_ELEM_MPEG_2_IPMP = 0x1A,
    MPE_SI_ELEM_AVC_VIDEO = 0x1B,
    MPE_SI_ELEM_VIDEO_DCII = 0x80,
    MPE_SI_ELEM_ATSC_AUDIO = 0x81,
    MPE_SI_ELEM_STD_SUBTITLE = 0x82,
    MPE_SI_ELEM_ISOCHRONOUS_DATA = 0x83,
    MPE_SI_ELEM_ASYNCHRONOUS_DATA = 0x84,
    MPE_SI_ELEM_ENHANCED_ATSC_AUDIO = 0x87
} mpe_SiElemStreamType;

typedef enum mpe_SiModulationMode
{
    MPE_SI_MODULATION_UNKNOWN = 0,
    MPE_SI_MODULATION_QPSK,
    MPE_SI_MODULATION_BPSK,
    MPE_SI_MODULATION_OQPSK,
    MPE_SI_MODULATION_VSB8,
    MPE_SI_MODULATION_VSB16,
    MPE_SI_MODULATION_QAM16,
    MPE_SI_MODULATION_QAM32,
    MPE_SI_MODULATION_QAM64,
    MPE_SI_MODULATION_QAM80,
    MPE_SI_MODULATION_QAM96,
    MPE_SI_MODULATION_QAM112,
    MPE_SI_MODULATION_QAM128,
    MPE_SI_MODULATION_QAM160,
    MPE_SI_MODULATION_QAM192,
    MPE_SI_MODULATION_QAM224,
    MPE_SI_MODULATION_QAM256,
    MPE_SI_MODULATION_QAM320,
    MPE_SI_MODULATION_QAM384,
    MPE_SI_MODULATION_QAM448,
    MPE_SI_MODULATION_QAM512,
    MPE_SI_MODULATION_QAM640,
    MPE_SI_MODULATION_QAM768,
    MPE_SI_MODULATION_QAM896,
    MPE_SI_MODULATION_QAM1024,
    MPE_SI_MODULATION_QAM_NTSC = 255
// for analog mode
} mpe_SiModulationMode;

/*
 This function initializes mpeos_SI module
 Return          Returns true if the module is successfully initialized,
 else returns false
 */
mpe_Bool mpeos_SIInit(void);

/*
 This function shuts down mpeos_SI module
 */
void mpeos_SIShutdown(void);

#ifdef __cplusplus
}
#endif

#endif  /* _MPEOS_SI_H */
