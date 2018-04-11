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


#ifndef _MPE_STORAGE_H_
#define _MPE_STORAGE_H_

#include "mpe_sys.h"
#include "../mgr/include/mgrdef.h"
#include "../mgr/include/storagemgr.h"

#define mpe_storage_mgr_ftable ((mpe_storage_ftable_t*)(FTABLE[MPE_MGR_TYPE_STORAGE]))

#define mpe_storageInit (mpe_storage_mgr_ftable->mpe_storageInit_ptr)

#define mpe_storageRegisterQueue (mpe_storage_mgr_ftable->mpe_storageRegisterQueue_ptr)
#define mpe_storageGetDeviceCount (mpe_storage_mgr_ftable->mpe_storageGetDeviceCount_ptr)
#define mpe_storageGetDeviceList (mpe_storage_mgr_ftable->mpe_storageGetDeviceList_ptr)
#define mpe_storageGetInfo (mpe_storage_mgr_ftable->mpe_storageGetInfo_ptr)
#define mpe_storageIsDetachable (mpe_storage_mgr_ftable->mpe_storageIsDetachable_ptr)
#define mpe_storageMakeReadyToDetach (mpe_storage_mgr_ftable->mpe_storageMakeReadyToDetach_ptr)
#define mpe_storageMakeReadyForUse (mpe_storage_mgr_ftable->mpe_storageMakeReadyForUse_ptr)
#define mpe_storageIsDvrCapable (mpe_storage_mgr_ftable->mpe_storageIsDvrCapable_ptr)
#define mpe_storageIsRemovable (mpe_storage_mgr_ftable->mpe_storageIsRemovable_ptr)
#define mpe_storageEject (mpe_storage_mgr_ftable->mpe_storageEject_ptr)
#define mpe_storageInitializeDevice (mpe_storage_mgr_ftable->mpe_storageInitializeDevice_ptr)

#endif /* _MPE_STORAGE_H_ */
