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

#ifndef _RI_TYPES_H_
#define _RI_TYPES_H_

#ifdef WIN32
#define    RI_MODULE_EXPORT        __declspec(dllexport)
#else
#define    RI_MODULE_EXPORT
#endif

// Define stdint.h types
#if defined(RI_HAVE_STDINT_H) || defined(HAVE_STDINT_H)
#include <stdint.h>
#else
#include <glib.h>

typedef guint8 uint8_t;
typedef gint8 int8_t;

typedef guint16 uint16_t;
typedef gint16 int16_t;

typedef guint32 uint32_t;
typedef gint32 int32_t;

typedef guint64 uint64_t;
typedef gint64 int64_t;
#endif

/**
 * Error codes returned by the RI Platform APIs
 */
typedef enum ri_error_enum
{
    RI_ERROR_NONE = 0,
    RI_ERROR_GENERAL,
    RI_ERROR_NOT_IMPLEMENTED,
    RI_ERROR_ILLEGAL_ARG,
    RI_ERROR_OUT_OF_RESOURCES,
    RI_ERROR_TUNE_IN_PROGRESS,
    RI_ERROR_FILTER_NOT_AVAILABLE,
    RI_ERROR_INVALID_TUNE_REQUEST,
    RI_ERROR_INVALID_FREQUENCY,
    RI_ERROR_INVALID_MODULATION_MODE,
    RI_ERROR_ALREADY_EXISTS,
    RI_ERROR_NO_TSB,
    RI_ERROR_NO_PLAYBACK,
    RI_ERROR_NO_CONVERSION,
    RI_ERROR_RECORDING_IN_USE,
    RI_ERROR_CABLECARD_NOT_READY,
    RI_ERROR_GF_NOT_SUPPORTED,
    RI_ERROR_INVALID_SAS_APPID,
    RI_ERROR_CONNECTION_NOT_AVAIL,
    RI_ERROR_APDU_SEND_FAIL,
    RI_ERROR_EOS,
    RI_ERROR_NO_DATA
} ri_error;

/* Boolean type */
typedef int ri_bool;

#ifndef FALSE
#define FALSE 0
#endif

#ifndef TRUE
#define TRUE (!FALSE)
#endif

#define boolStr(b)   ((b)? "TRUE" : "FALSE")

#ifndef MIN
#define MIN(a,b) ((a) > (b) ? (b) : (a))
#endif

#ifndef MAX
#define MAX(a,b) ((a) > (b) ? (a) : (b))
#endif


/**
 * Represents an area in normalized screen coordinates.
 */
typedef struct
{
    float x;
    float y;
    float width;
    float height;
} ri_rect;

#endif /* _RI_TYPES_H_ */
