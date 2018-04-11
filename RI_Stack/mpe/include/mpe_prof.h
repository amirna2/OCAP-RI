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

#ifndef _MPE_PROF_H_
#define _MPE_PROF_H_

#ifdef __cplusplus
extern "C"
{
#endif

#ifdef MPE_FEATURE_PROF

#include <mpe_types.h>	/* Resolve basic type references. */
#include <mpe_os.h>

#include "../mgr/include/mgrdef.h"
#include "../mgr/include/profmgr.h"

#define MAX_PROF_CNT  128				/* max number of events to track */
#define MAX_CHARS_FOR_CODE_SECT 40		/* the description is limited to 40 chars! */

#define mpe_profmgr_ftable ((mpe_prof_ftable_t*)(FTABLE[MPE_MGR_TYPE_PROF]))

#define mpe_profInit() 								((mpe_profmgr_ftable->mpe_profInit_ptr))()
#define mpe_profStartTiming(timee, timeeT, timeeI)	((mpe_profmgr_ftable->mpe_profStartTiming_ptr))(timee, timeeT, timeeI)
#define mpe_profStopTiming(timeeI, timeeT)			((mpe_profmgr_ftable->mpe_profStopTiming_ptr))(timeeI, timeeT)
#define mpe_profDestroy()							((mpe_profmgr_ftable->mpe_profDestroy_ptr))()
#define mpe_profSetWhere(timeeI, where)				((mpe_profmgr_ftable->mpe_profSetWhere_ptr))(timeeI, where)
#define mpe_profPopWhereStack(timeeI)				((mpe_profmgr_ftable->mpe_profPopWhereStack_ptr))(timeeI)
#define mpe_profGetIndex(labelStr,labelIndex)		((mpe_profmgr_ftable->mpe_profGetIndex_ptr))(labelStr,labelIndex)
#define mpe_profDisplayTiming(numPrints)			((mpe_profmgr_ftable->mpe_profDisplayTiming_ptr))(numPrints)
#define mpe_profAddLabel(labelStr,labelIndex)		((mpe_profmgr_ftable->mpe_profAddLabel_ptr))(labelStr,labelIndex)
#define mpe_profAddComment(labelStr)				((mpe_profmgr_ftable->mpe_profAddComment_ptr))(labelStr)

#else /* MPE_FEATURE_PROF */

#define mpe_profInit()
#define mpe_profStartTiming(timee, timeeT, timeeI)
#define mpe_profStopTiming(timeeI, timeeT)
#define mpe_profDestroy()
#define mpe_profSetWhere(timeeI, where)
#define mpe_profPopWhereStack(timeeI)
#define mpe_profGetIndex(labelStr,labelIndex)
#define mpe_profDisplayTiming(numPrints)
#define mpe_profAddLabel(labelStr,labelIndex)
#define mpe_profAddComment(labelStr)

#endif /* MPE_FEATURE_PROF */

#ifdef __cplusplus
}
#endif

#endif /* _MPE_PROF_H_ */
