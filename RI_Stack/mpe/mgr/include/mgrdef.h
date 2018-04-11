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

#ifndef _MGRDEF_H_
#define _MGRDEF_H_

#ifdef __cplusplus
extern "C"
{
#endif

/******************************************
 * Base manager entrypoint prototypes
 ******************************************/
typedef struct _MPEManagerBase *(*mpe_mgrGetInstance)(void);
typedef void (*mpe_mgrInstantiate)(void);
typedef void (*mpe_mgrDeinstantiate)(void);

/******************************************
 * Base manager function table
 ******************************************/
typedef struct mpe_MgrBaseFuncs
{
    mpe_mgrGetInstance getInstance;
    mpe_mgrInstantiate instantiate;
    mpe_mgrDeinstantiate deinstantiate;
} mpe_MgrBaseFuncs;

/******************************************
 * Base manager function table
 ******************************************/
typedef struct _MPEManagerBase
{
    mpe_MgrBaseFuncs basefuncs;
} mpe_MgrBase;

// Enumaration for locating specific MPE manager functionality
enum MPE_MGR_TYPES
{
    MPE_MGR_TYPE_SYS = 0, // System Manager
    MPE_MGR_TYPE_JVM, // JVM Manager
    MPE_MGR_TYPE_TEST, // test manager
    MPE_MGR_TYPE_DBG, // debug manager
    MPE_MGR_TYPE_FILESYS, // File System manager
    MPE_MGR_TYPE_DISP, // display manager
    MPE_MGR_TYPE_MEDIA, // media manager
    MPE_MGR_TYPE_SI, // SI manager
    MPE_MGR_TYPE_OS, // OS manager
    MPE_MGR_TYPE_NET, // Networking manager
    MPE_MGR_TYPE_FILTER, // filter mgr
    MPE_MGR_TYPE_CC, // Closed captioning manager
    MPE_MGR_TYPE_POD, // POD manager
    MPE_MGR_TYPE_ED, // ED manager
    MPE_MGR_TYPE_HOSTSET, // Host Settings Manager
    MPE_MGR_TYPE_STORAGE, // Storage manager
#ifdef MPE_FEATURE_DVR
    MPE_MGR_TYPE_DVR, // DVR manager
#endif
#ifdef MPE_FEATURE_HN
    MPE_MGR_TYPE_HN, // Home Networking Manager
#endif
    MPE_MGR_TYPE_SND, // Sound Manager
#ifdef MPE_FEATURE_PROF
    MPE_MGR_TYPE_PROF, // Profile manager
#endif
#ifdef MPE_FEATURE_FRONTPANEL
    MPE_MGR_TYPE_FRONTPANEL,
#endif
    MPE_MGR_TYPE_CDL, // Code Download manager
    MPE_MGR_TYPE_VBI, // VBI data acquisition manager
    MPE_MGR_COUNT, // total manager count
    MPE_MGR_MAX = 34
};

#ifdef __cplusplus
}
;
#endif

#endif /* _MGRDEF_H_ */
