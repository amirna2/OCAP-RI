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

#include "mpe_error.h"

#ifndef _MPE_VBIEVENTS_H
#define _MPE_VBIEVENTS_H

/*
 * Events generated from vbi filter component.
 */

#define MPE_VBI_EVENT_UNKNOWN                        (0x00001000)
#define MPE_VBI_EVENT_BUFFER_FOUND                   (MPE_VBI_EVENT_UNKNOWN + 1)
#define MPE_VBI_EVENT_LAST_BUFFER_FOUND              (MPE_VBI_EVENT_UNKNOWN + 2)
#define MPE_VBI_EVENT_FILTER_CANCELLED               (MPE_VBI_EVENT_UNKNOWN + 3)
#define MPE_VBI_EVENT_FILTER_PREEMPTED               (MPE_VBI_EVENT_UNKNOWN + 4)
#define MPE_VBI_EVENT_SOURCE_CLOSED                  (MPE_VBI_EVENT_UNKNOWN + 5)
#define MPE_VBI_EVENT_OUT_OF_MEMORY                  (MPE_VBI_EVENT_UNKNOWN + 6)
#define MPE_VBI_EVENT_FILTER_AVAILABLE               (MPE_VBI_EVENT_UNKNOWN + 7)
#define MPE_VBI_EVENT_CA                             (MPE_VBI_EVENT_UNKNOWN + 8)
#define MPE_VBI_EVENT_STREAM_CLOSED                  (MPE_VBI_EVENT_UNKNOWN + 9)

/*
 * Return values from vbi filter APIs.
 */

#define MPE_VBI_ERROR                                    (0x00002000)
#define MPE_VBI_ERROR_FILTER_NOT_AVAILABLE               (MPE_VBI_ERROR + 1)
#define MPE_VBI_ERROR_INVALID_BUFFER_HANDLE             (MPE_VBI_ERROR + 2)
#define MPE_VBI_ERROR_BUFFER_NOT_AVAILABLE              (MPE_VBI_ERROR + 3)
#define MPE_VBI_ERROR_INVALID_SESSION                    (MPE_VBI_ERROR + 4)
#define MPE_VBI_ERROR_TUNER_NOT_AT_FREQUENCY             (MPE_VBI_ERROR + 5)
#define MPE_VBI_ERROR_UNSUPPORTED                        (MPE_VBI_ERROR + 6)
#define MPE_VBI_ERROR_CA                                 (MPE_VBI_ERROR + 7)
#define MPE_VBI_ERROR_CA_ENTITLEMENT                     (MPE_VBI_ERROR_CA + 1)
#define MPE_VBI_ERROR_CA_RATING                          (MPE_VBI_ERROR_CA + 2)
#define MPE_VBI_ERROR_CA_TECHNICAL                       (MPE_VBI_ERROR_CA + 3)
#define MPE_VBI_ERROR_CA_BLACKOUT                        (MPE_VBI_ERROR_CA + 4)
#define MPE_VBI_ERROR_CA_DIAG                            (MPE_VBI_ERROR_CA + 5)
#define MPE_VBI_ERROR_CA_DIAG_PAYMENT                    (MPE_VBI_ERROR_CA_DIAG + 1)
#define MPE_VBI_ERROR_CA_DIAG_RATING                     (MPE_VBI_ERROR_CA_DIAG + 2)
#define MPE_VBI_ERROR_CA_DIAG_TECHNICAL                  (MPE_VBI_ERROR_CA_DIAG + 3)
#define MPE_VBI_ERROR_CA_DIAG_PREVIEW                    (MPE_VBI_ERROR_CA_DIAG + 4)

#endif /* _MPE_VBIEVENTS_H */
