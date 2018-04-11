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
#ifndef _OS_STORAGE_H_
#define _OS_STORAGE_H_

#ifdef __cplusplus
extern "C"
{
#endif

#include <mpe_os.h>

#include <os_file.h>

// We are using the device serial number as the device name
// According to SDM API documentation, serial number is 
// constrained to less than 64 characters
#define OS_STORAGE_MAX_NAME_SIZE 20

#define OS_STORAGE_MAX_PATH_SIZE OS_FS_MAX_PATH

#define OS_STORAGE_MAX_DISPLAY_NAME_SIZE 40

#define MAX_STORAGE_DEVICES 10

/** 
 * This data type enumerates the set of possible storage volumes types.
 * Note that devices can be both detachable and removable 
 */
typedef enum
{
    MPEOS_STORAGE_TYPE_INTERNAL = 0x0000,
    MPEOS_STORAGE_TYPE_DETACHABLE = 0x0001,
    MPEOS_STORAGE_TYPE_REMOVABLE = 0x0002
} mpeos_StorageDeviceType;

/* Definitions represents media volume free space alarm levels */
#define MAX_MEDIA_VOL_LEVEL 99
#define MIN_MEDIA_VOL_LEVEL 1

/* Represents free space Alarm status as armed, unarmed and not used */
typedef enum
{
    ARMED, UNARMED, UNUSED
} os_FreeSpaceAlarmStatus;

typedef struct _os_MediaVolumeInfo os_MediaVolumeInfo;
typedef struct _os_StorageDeviceInfo os_StorageDeviceInfo;
typedef struct _os_VolumeSpaceAlarm os_VolumeSpaceAlarm;

struct _os_VolumeSpaceAlarm
{
    os_FreeSpaceAlarmStatus status;
    uint8_t level;
    os_VolumeSpaceAlarm* next;
};

struct _os_MediaVolumeInfo
{
    char rootPath[OS_STORAGE_MAX_PATH_SIZE + 1];
    char mediaPath[OS_STORAGE_MAX_PATH_SIZE + 1];
    char dataPath[OS_STORAGE_MAX_PATH_SIZE + 1];
    uint64_t reservedSize;
    uint64_t usedSize;
    os_VolumeSpaceAlarm* alarmList;
    os_StorageDeviceInfo *device;
    mpe_Mutex mutex;
    os_MediaVolumeInfo *next;
    uint64_t tsbMinSizeBytes;
};

struct _os_StorageDeviceInfo
{
    char name[OS_STORAGE_MAX_NAME_SIZE + 1];
    char displayName[OS_STORAGE_MAX_DISPLAY_NAME_SIZE + 1];
    char rootPath[OS_STORAGE_MAX_PATH_SIZE + 1];
    uint64_t size; // bytes
    uint64_t mediaFsSize; // bytes
    uint64_t defaultMediaFsSize; // bytes
    uint64_t freeMediaSize; // bytes
    mpeos_StorageDeviceType type;
    uint16_t status; // holds values of type mpe_StorageStatus
    mpe_Bool attached;
    mpe_Bool mediaPresent;
    os_MediaVolumeInfo *volumes;
    mpe_Mutex mutex;
};

/**
 * This data type enumerates the set of possible error codes that
 * can be passed to another component in the MPEOS layer.
 */
typedef enum
{
    OS_STORAGE_ERR_NOERR, // no error
    OS_STORAGE_ERR_OUT_OF_SPACE, // no space left on device
    OS_STORAGE_ERR_DEVICE_ERR,
// a hardware device error
} os_StorageError;

char *storageGetRoot();
os_StorageDeviceInfo *storageGetDefaultDevice();

#define STORAGE_TRIM_ROOT(path) ((path) + strlen(storageGetRoot()) + 1)

#ifdef __cplusplus
}
#endif

#endif // _OS_STORAGE_H_
