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

/*
 * ri_audio_output_port.h
 *
 *  Created on: October 7, 2009
 *      Author: Chris Sweeney
 */

#ifndef _RI_AUDIO_OUTPUT_PORT_H_
#define _RI_AUDIO_OUTPUT_PORT_H_

// Include RI Platform header files.
#include <ri_types.h>

typedef enum
{
    RI_AP_COMPRESSION_NONE,
    RI_AP_COMPRESSION_LIGHT,
    RI_AP_COMPRESSION_MEDIUM,
    RI_AP_COMPRESSION_HEAVY
} ri_ap_compression;

typedef enum
{
    RI_AP_ENCODING_NONE,
    RI_AP_ENCODING_DISPLAY,
    RI_AP_ENCODING_PCM,
    RI_AP_ENCODING_AC3
} ri_ap_encoding;

typedef enum
{
    RI_AP_STEREO_MODE_MONO,
    RI_AP_STEREO_MODE_STEREO,
    RI_AP_STEREO_MODE_SURROUND
} ri_ap_stereo_mode;

/**
 * Type definition for Audio Output Port.
 */
typedef struct ri_audioOutputPort_s ri_audioOutputPort_t;
typedef struct ri_audioOutputPortData_s ri_audioOutputPortData_t;

/**
 * An Audio Output Port object.
 *
 * This object mimics the Java AudioOutputPort interface (for good reason).
 * The actual implementation will need to control a gstreamer audio pipeline.
 * TODO: Add audio pipeline integration
 */
struct ri_audioOutputPort_s
{
    char* (*getId)(ri_audioOutputPort_t*);
    ri_ap_compression (*getCompression)(ri_audioOutputPort_t*);
    void (*setCompression)(ri_audioOutputPort_t*, ri_ap_compression);
    float (*getGain)(ri_audioOutputPort_t*);
    void (*setGain)(ri_audioOutputPort_t*, float);
    ri_ap_encoding (*getEncoding)(ri_audioOutputPort_t*);
    void (*setEncoding)(ri_audioOutputPort_t*, ri_ap_encoding);
    float (*getLevel)(ri_audioOutputPort_t*);
    void (*setLevel)(ri_audioOutputPort_t*, float);
    float (*getOptimalLevel)(ri_audioOutputPort_t*);
    float (*getMaxDb)(ri_audioOutputPort_t*);
    float (*getMinDb)(ri_audioOutputPort_t*);
    ri_ap_stereo_mode (*getStereoMode)(ri_audioOutputPort_t*);
    void (*setStereoMode)(ri_audioOutputPort_t*, ri_ap_stereo_mode);
    ri_ap_compression* (*getSupportedCompressions)(ri_audioOutputPort_t*);
    uint8_t (*getSupportedCompressionsCount)(ri_audioOutputPort_t*);
    ri_ap_encoding* (*getSupportedEncodings)(ri_audioOutputPort_t*);
    uint8_t (*getSupportedEncodingsCount)(ri_audioOutputPort_t*);
    ri_ap_stereo_mode* (*getSupportedStereoModes)(ri_audioOutputPort_t*);
    uint8_t (*getSupportedStereoModesCount)(ri_audioOutputPort_t*);
    ri_bool (*isLoopThru)(ri_audioOutputPort_t*);
    void (*setLoopThru)(ri_audioOutputPort_t*, ri_bool);
    ri_bool (*isMuted)(ri_audioOutputPort_t*);
    void (*setMuted)(ri_audioOutputPort_t*, ri_bool);
    ri_bool (*isLoopThruSupported)(ri_audioOutputPort_t*);

    ri_audioOutputPortData_t* m_data;
};

#endif /* _RI_AUDIO_OUTPUT_PORT_H_ */
