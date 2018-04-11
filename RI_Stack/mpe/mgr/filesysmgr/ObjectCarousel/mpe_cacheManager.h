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

#ifndef MPE_CACHEMANAGER_H
#define MPE_CACHEMANAGER_H

#include "mpe_types.h"
#include "mpe_os.h"
#include "mpe_error.h"
#include "mpe_filter.h"
#include "jni.h"

typedef struct mpe_DccCacheObject mpe_DccCacheObject;

/**
 * Create a new cache object, and insert it at the head of the LRU list (ie, NUR).
 * Creates the object from the section handle, and will release the section handle, the caller
 * should not release the handle.
 *
 * @param section       Handle for the section being processed.
 * @param retObject     [out] Pointer to where to return a pointer to the cache block.
 *
 * @return MPE_SUCCESS if the block is successfully created, error codes otherwise.
 */
extern mpe_Error mpe_dccNewCacheObject(mpe_FilterSectionHandle section,
        mpe_DccCacheObject **retObject);

/**
 * Read from a cache object.  The object must be locked before being read.
 *
 * @param cacheObject   The object to read from.
 * @param offset        How far into the object to start reading.
 * @param length        Number of bytes to read.
 * @param dest          Pointer to an array to fill with the copied data.
 *
 * @return MPE_SUCCESS if the data is read.  Error codes otherwise.
 */
extern mpe_Error mpe_dccReadCacheObject(mpe_DccCacheObject *cacheObject,
        int offset, int length, uint8_t *dest);

/**
 * Lock a cache object so it can't be purged.
 * Does not prevent the object from being released.
 *
 * @param cacheObject   The object to lock.
 *
 * @return MPE_SUCCESS if the block is locked and can be read, MPE_ENODATA if the block has been purged.
 */
extern mpe_Error mpe_dccLockCacheObject(mpe_DccCacheObject *cacheObject);

/**
 * Unlock a block.
 *
 * @param cacheObject   The object to unlock.
 *
 * @return MPE_SUCCESS if the block is successfully unlocked, error codes otherwise.
 */
extern mpe_Error mpe_dccUnlockCacheObject(mpe_DccCacheObject *cacheObject);

/**
 * Release a cache block and it's underlying memory.  Any block can be released,
 * even locked ones.
 *
 * @param cacheObject   The cache object to release.
 *
 * @return MPE_SUCCESS if the block is successfully freed.
 */
extern mpe_Error mpe_dccReleaseCacheObject(mpe_DccCacheObject *cacheObject);

/**
 * Determine if a block is currently present in the cache.  This is only true at the exact moment
 * of the check, no lock is performed.  The block could easily be purged before you access it.
 *
 * @param cacheObject   The cache object to check.
 *
 * @return True if the block is there, false if it's been purged.
 */
extern mpe_Bool mpe_dccBlockPresent(mpe_DccCacheObject *cacheObject);

/**
 * Initialize the cache manager.
 *
 * @return MPE_SUCCESS if we successfully initialize.
 */
extern void mpe_dccInit(void);

#endif /* MPE_CACHEMANAGER_H */
