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


#ifndef _MPEOS_STORAGE_H_
#define _MPEOS_STORAGE_H_

#include <mpe_types.h>
#include <mpe_error.h>
#include <mpeos_event.h>
#include <os_storage.h>

/******************************************************************************
 * Public Data Types, Constants, and Enumerations
 *****************************************************************************/

/**
 * This macro defines the maximum number of characters in the human readable
 * display name for a storage device.
 */
#define MPE_STORAGE_MAX_DISPLAY_NAME_SIZE OS_STORAGE_MAX_DISPLAY_NAME_SIZE

/**
 * This macro defines the maximum number of characters in the storage device name.
 * The OS_STORAGE_MAX_NAME_SIZE is a platform-specific value defined in os_storage.h
 */
#define MPE_STORAGE_MAX_NAME_SIZE OS_STORAGE_MAX_NAME_SIZE

/**
 *  This macro defines the maximum number of characters in the root path of a
 * storage device.  The OS_STORAGE_MAX_PATH_SIZE is a platform-specific value
 * in os_storage.h
 */
#define MPE_STORAGE_MAX_PATH_SIZE OS_STORAGE_MAX_PATH_SIZE

/**
 * mpeos_Storage is an opaque data type that represents a storage device.
 * The internal representation of this data type is known only by the
 * platform-dependent MPE OS implementation.  mpe_StorageHandle represents
 * a platform-independent handle or reference to a particular storage device.
 */
typedef os_StorageDeviceInfo mpeos_Storage;
typedef mpeos_Storage* mpe_StorageHandle;

/**
 * This data type enumerates the set of possible event codes triggered by the Storage module
 *
 * WARNING! This enumeration must remain in synch with the event codes defined in
 * org.ocap.storage.StorageManagerEvent
 */
typedef enum
{
    MPE_EVENT_STORAGE_ATTACHED = 0,
    MPE_EVENT_STORAGE_DETACHED,
    MPE_EVENT_STORAGE_CHANGED
} mpe_StorageEvent;

/**
 *  This data type enumerates the set of possible storage device attributes
 * that may be queried via the mpe_storageGetInfo() API.
 */
typedef enum
{
    MPE_STORAGE_DISPLAY_NAME, // human friendly name of the device for display
    MPE_STORAGE_NAME, // unique device name
    MPE_STORAGE_FREE_SPACE, // total free space (MEDIAFS + GPFS)
    MPE_STORAGE_CAPACITY, // total capacity (MEDIAFS + GPFS - HIDDENFS)
    MPE_STORAGE_STATUS, // status of the device
    MPE_STORAGE_GPFS_PATH, // general purpose file system path for file i/o
    MPE_STORAGE_GPFS_FREE_SPACE, // free space on general purpose file system
    MPE_STORAGE_MEDIAFS_PARTITION_SIZE, // size of the MEDIAFS partition
    // note that GPFS partition size is derived by
    // subtracting this value from the MPE_STORAGE_CAPACITY
    MPE_STORAGE_MEDIAFS_FREE_SPACE,
    MPE_STORAGE_SUPPORTED_ACCESS_RIGHTS
// EFAP access rights supported by this storage
//device
} mpe_StorageInfoParam;

/**
 * This data type enumerates the set of possible states that a storage device may be in
 *
 * WARNING! The ordering of items in this enumeration must remain in sync with the
 * status constants defined in org.ocap.storage.StorageProxy.java
 */
typedef enum
{
    MPE_STORAGE_STATUS_READY = 0, // indicates that the device is mounted, initialized,
    // and ready for use
    MPE_STORAGE_STATUS_OFFLINE, // indicates that the device is present  but requires a call
    // to mpe_storageMakeReadyForUse() before it can be used
    MPE_STORAGE_STATUS_BUSY, // indicates that the device is busy (e.g. being initialized,
    // configured, checked for consistency, or made ready to detach).
    // It does not indicate that I/O operations are currently
    // in progress.
    MPE_STORAGE_STATUS_UNSUPPORTED_DEVICE, // the device is not supported by the platform
    MPE_STORAGE_STATUS_UNSUPPORTED_FORMAT, // the device type and model is supported by the platform,
    // but the current partitioning or formatting is not supported by
    // the platform without reinitialization and loss of the existing
    // contents
    MPE_STORAGE_STATUS_UNINITIALIZED, // the device is not formatted and contains no existing data
    MPE_STORAGE_STATUS_DEVICE_ERR, // indicates that the device is in an unrecoverable error
    // state and cannot be used
    MPE_STORAGE_STATUS_NOT_PRESENT
// indicates that a detected storage device bay does not
// contain a removable storage device
} mpe_StorageStatus;

/**
 * This data type enumerates the set of bit masks for determining the set of
 * access rights supported by a given storage device.
 *
 * @see mpe_storageGetInfo(MPE_STORAGE_SUPPORTED_ACCESS_RIGHTS)
 */
typedef enum
{
    MPE_STORAGE_ACCESS_RIGHT_WORLD_READ = 1,
    MPE_STORAGE_ACCESS_RIGHT_WORLD_WRITE = 2,
    MPE_STORAGE_ACCESS_RIGHT_APP_READ = 4,
    MPE_STORAGE_ACCESS_RIGHT_APP_WRITE = 8,
    MPE_STORAGE_ACCESS_RIGHT_ORG_READ = 16,
    MPE_STORAGE_ACCESS_RIGHT_ORG_WRITE = 32,
    MPE_STORAGE_ACCESS_RIGHT_OTHER_READ = 64,
    MPE_STORAGE_ACCESS_RIGHT_OTHER_WRITE = 128
} mpe_StorageSupportedAccessRights;

/**
 * This data type enumerates the set of possible error codes that
 * may be returned by Storage Manager calls.
 */
typedef enum
{
    MPE_STORAGE_ERR_NOERR = MPE_SUCCESS, // no error
    MPE_STORAGE_ERR_INVALID_PARAM = MPE_EINVAL,// a parameter is invalid
    MPE_STORAGE_ERR_OUT_OF_MEM = MPE_ENOMEM, // out of memory
    MPE_STORAGE_ERR_OUT_OF_SPACE, // no space left on device
    MPE_STORAGE_ERR_BUSY = MPE_EBUSY, // device or resource is busy
    MPE_STORAGE_ERR_UNSUPPORTED, // operation is not supported
    MPE_STORAGE_ERR_NOT_ALLOWED, // operation is not allowed
    MPE_STORAGE_ERR_DEVICE_ERR, // a hardware device error
    MPE_STORAGE_ERR_OUT_OF_STATE, // operation not appropriate
    // at this time
    MPE_STORAGE_ERR_BUF_TOO_SMALL, // the specified memory buffer is not
    // large enough to fit all of the data
    MPE_STORAGE_ERR_UNKNOWN
// unknown error
} mpe_StorageError;

/******************************************************************************
 * Public Function Prototypes
 *****************************************************************************/

void mpeos_storageInit(void);

mpe_StorageError mpeos_storageRegisterQueue(mpe_EventQueue queueId, void *act);

mpe_StorageError mpeos_storageGetDeviceCount(uint32_t* count);

mpe_StorageError mpeos_storageGetDeviceList(uint32_t* count,
        mpe_StorageHandle *devices);

mpe_StorageError mpeos_storageGetInfo(mpe_StorageHandle device,
        mpe_StorageInfoParam param, void* output);

mpe_StorageError mpeos_storageIsDetachable(mpe_StorageHandle device,
        mpe_Bool* value);

mpe_StorageError mpeos_storageMakeReadyToDetach(mpe_StorageHandle device);

mpe_StorageError mpeos_storageMakeReadyForUse(mpe_StorageHandle device);

mpe_StorageError mpeos_storageIsDvrCapable(mpe_StorageHandle device,
        mpe_Bool* value);

mpe_StorageError mpeos_storageIsRemovable(mpe_StorageHandle device,
        mpe_Bool* value);

mpe_StorageError mpeos_storageEject(mpe_StorageHandle device);

mpe_StorageError mpeos_storageInitializeDevice(mpe_StorageHandle device,
        mpe_Bool userAuthorized, uint64_t* mediaFsSize);

#endif /* _MPEOS_STORAGE_H_ */
