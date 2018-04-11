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

#ifndef _MPETEST_DVR_H_
#define _MPETEST_DVR_H_ 1

/**
 * MPE / MPEOS function names are re-defined here using macros, in order to
 * support MPE or MPEOS tests using the same test code.
 *
 * If #define TEST_MPEOS is defined, then tests will be for MPEOS, else MPE.
 */

#ifdef TEST_MPEOS
# include "mpeos_dvr.h"
# include "mpeos_time.h"
# define MPETEST_FCT(x)  mpeos_ ## x
#else
# include "mpe_sys.h"
# include "mpe_dvr.h"
# define MPETEST_FCT(x)  mpe_ ## x

#endif /* TEST_MPEOS */

#define dvrGet                 MPETEST_FCT(dvrGet)
#define dvrGetRecordingList    MPETEST_FCT(dvrGetRecordingList)
#define dvrFreeRecordingList   MPETEST_FCT(dvrFreeRecordingList)

#define dvrRecordingStart      MPETEST_FCT(dvrRecordingStart)
#define dvrRecordingStop       MPETEST_FCT(dvrRecordingStop)
#define dvrRecordingDelete     MPETEST_FCT(dvrRecordingDelete)
#define dvrRecordingGet        MPETEST_FCT(dvrRecordingGet)

#define dvrRecordingPlayStart  MPETEST_FCT(dvrRecordingPlayStart)
#define dvrTsbPlayStart        MPETEST_FCT(dvrTsbPlayStart)
#define dvrPlayBackStop        MPETEST_FCT(dvrPlayBackStop)
#define dvrPlayBackDelete      MPETEST_FCT(dvrPlayBackDelete)
#define dvrPlaybackGetTime     MPETEST_FCT(dvrPlaybackGetTime)
#define dvrPlaybackBlockPresentation MPETEST_FCT(dvrPlaybackBlockPresentation)

#define dvrSetTrickMode        MPETEST_FCT(dvrSetTrickMode)
#define dvrGetTrickMode        MPETEST_FCT(dvrGetTrickMode)

#define dvrTsbNew              MPETEST_FCT(dvrTsbNew)
#define dvrTsbBufferingStart   MPETEST_FCT(dvrTsbBufferingStart)
#define dvrTsbBufferingStop    MPETEST_FCT(dvrTsbBufferingStop)
#define dvrTsbDelete           MPETEST_FCT(dvrTsbDelete)
#define dvrTsbGet              MPETEST_FCT(dvrTsbGet)
#define dvrTsbConvertStart     MPETEST_FCT(dvrTsbConvertStart)
#define dvrTsbConvertStop      MPETEST_FCT(dvrTsbConvertStop)
#define dvrTsbSetSize          MPETEST_FCT(dvrTsbSetSize)
#define dvrTsbSetDuration      MPETEST_FCT(dvrTsbSetDuration)

#define dvrPlaybackSetTime     MPETEST_FCT(dvrPlaybackSetTime)
#undef  timeGetMillis
#define timeGetMillis          MPETEST_FCT(timeGetMillis)

#endif /* _MPETEST_DVR_H_ */ 
