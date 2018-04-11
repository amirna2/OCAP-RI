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

/******************************************************************************
 * Includes
 *****************************************************************************/

#include <mpe_storage.h>        /* Resolve storage definitions. */
#include <mpeos_storage.h>      /* Resolve storage module definitions */

#include <mpe_types.h>          /* Resolve basic type references. */
#include <mpe_error.h>          /* Resolve error code definitions */

#include <mpeos_event.h>        /* Resolve event definitions. */
#include <mpeos_mem.h>          // Resolve memory definitions.
#include <mpeos_dbg.h>          /* Resolve debug module definitions */
#include <mpeos_util.h>         /* Resolve generic STB definitions */
#include <mpeos_file.h>
#include <mpeos_sync.h>

#include <ri_test_interface.h>

#ifdef MPE_FEATURE_DVR
#include <os_dvr.h>
#endif

#include <ctype.h>
#include <dirent.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <sys/stat.h>

#include "inttypes.h"

/******************************************************************************
 * PRIVATE Data Types
 *****************************************************************************/

static void test_registerMenus();
static mpe_StorageError initializeDevice(mpe_StorageHandle device);
static mpe_Bool isInitialized(mpe_StorageHandle device);
static void uninitializeDevice(mpe_StorageHandle device);

typedef struct
{
    mpe_Bool queueRegistered;
    mpe_EventQueue listenerQueue;
    void* listenerACT; // asynchronous completion token
} StorageMgrData;

static const char* DEFAULT_STORAGE_ROOT = "storage";
static const char* DEVICE_DATA_FILENAME = "-devdata-";
static const mpeos_Storage DEFAULT_DEVICES[] =
{
        { "ID", "Internal Device", "", // Full path computed later
                171798691840LL, // 160GB for total size
                170724950016LL, // 159GB for media fs size
                170724950016LL, // 159GB for default media fs size
                0, // 0MB of used space
                MPEOS_STORAGE_TYPE_INTERNAL, MPE_STORAGE_STATUS_READY,
                true, true, NULL },
        { "DD", "Detachable Device",
                "", // Full path computed later
                85899345920LL, // 80GB for total size
                84825604096LL, // 79GB for media fs size
                84825604096LL, // 79GB for default media fs size
                0, // 0MB of used space
                MPEOS_STORAGE_TYPE_DETACHABLE, MPE_STORAGE_STATUS_OFFLINE,
                true, true, NULL },
        { "RD", "Removable Device",
                "", // Full path computed later
                85899345920LL, // 80GB for total size
                84825604096LL, // 79GB for media fs size
                84825604096LL, // 79GB for default media fs size
                0, // 0MB of used space
                MPEOS_STORAGE_TYPE_REMOVABLE, MPE_STORAGE_STATUS_OFFLINE,
                true, true, NULL },
        { "DRD", "Detachable Removable Device",
                "", // Full path computed later
                85899345920LL, // 80GB for total size
                84825604096LL, // 79GB for media fs size
                84825604096LL, // 79GB for default media fs size
                0, // 0MB of used space
                MPEOS_STORAGE_TYPE_DETACHABLE | MPEOS_STORAGE_TYPE_REMOVABLE,
                MPE_STORAGE_STATUS_OFFLINE,
                true, true, NULL } };

// Global storage data
static StorageMgrData gStorageMgrData;
static mpeos_Storage gDevices[MAX_STORAGE_DEVICES];
static mpe_Mutex gDeviceMutex;
static char gStorageRoot[MPE_FS_MAX_PATH];
static uint8_t gDevCount = 0;

static void sendStorageEvent(mpe_StorageHandle device, mpe_StorageEvent event)
{
    if (gStorageMgrData.queueRegistered)
    {
        mpeos_eventQueueSend(gStorageMgrData.listenerQueue, event, device,
                gStorageMgrData.listenerACT, device->status);
    }
}

/******************************************************************************
 * String conversion functions
 *****************************************************************************/

static char* deviceTypeToString(mpeos_StorageDeviceType type)
{
    if (type == MPEOS_STORAGE_TYPE_INTERNAL)
    {
        return "Internal";
    }

    if ((type & MPEOS_STORAGE_TYPE_DETACHABLE) &&
        (type & MPEOS_STORAGE_TYPE_REMOVABLE))
    {
        return "Detachable/Removable";
    }
    
    if (type & MPEOS_STORAGE_TYPE_DETACHABLE)
    {
        return "Detachable";
    }
    
    if (type & MPEOS_STORAGE_TYPE_REMOVABLE)
    {
        return "Removable";
    }

    return "INVALID DEVICE TYPE";
}

static char* boolToString(mpe_Bool boolValue)
{
    return (boolValue) ? "TRUE" : "FALSE";
}

static char* deviceStatusToString(mpe_StorageStatus status)
{
    switch (status)
    {
    case MPE_STORAGE_STATUS_READY:
        return "Ready";
    case MPE_STORAGE_STATUS_OFFLINE:
        return "Offline";
    case MPE_STORAGE_STATUS_BUSY:
        return "Busy";
    case MPE_STORAGE_STATUS_UNSUPPORTED_DEVICE:
        return "Unsupported Device";
    case MPE_STORAGE_STATUS_UNSUPPORTED_FORMAT:
        return "Unsupported Format";
    case MPE_STORAGE_STATUS_UNINITIALIZED:
        return "Unitialized";
    case MPE_STORAGE_STATUS_DEVICE_ERR:
        return "Error";
    case MPE_STORAGE_STATUS_NOT_PRESENT:
        return "Media Not Present";
    }

    return "INVALID STATUS";
}

#define DEVICE_STRING_FORMAT \
    "Device %d: %s (%s)               \r\n" \
    "\tType = %s                      \r\n" \
    "\tStatus = %s                    \r\n" \
    "\tAttached = %s                  \r\n" \
    "\tMedia present = %s             \r\n" \
    "\tSize = %llu                    \r\n" \
    "\tTotal Media Size = %llu        \r\n" \
    "\tFree Media Size = %llu         \r\n" \
    "\tDefault Media Size = %llu      \r\n" \
    "\tPath = %s                      \r\n"

#define MAX_DEVICE_STRING_LEN 512
static char deviceStringBuffer[MAX_DEVICE_STRING_LEN];
static char* storageDeviceToString(int deviceIdx, os_StorageDeviceInfo* device)
{
    if (deviceIdx == -1)
    {
        return "Undefined";
    }
    
    snprintf(deviceStringBuffer, MAX_DEVICE_STRING_LEN,
             DEVICE_STRING_FORMAT,
             deviceIdx,
             device->name, device->displayName,
             deviceTypeToString(device->type),
             deviceStatusToString(device->status),
             boolToString(device->attached),
             boolToString(device->mediaPresent),
             device->size,
             device->mediaFsSize,
             device->freeMediaSize,
             device->defaultMediaFsSize,
             device->rootPath);
    return deviceStringBuffer;
}

/******************************************************************************
 * Test menu support
 *****************************************************************************/

int gTest_CurrentStorageDevice = -1;

static int test_storageMenuMainInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    int i;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_STORAGE, "%s(%d, %s);\n",
              __FUNCTION__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    if (!gStorageMgrData.queueRegistered)
    {
        ri_test_SendString(sock, "\r\n\nNO STORAGE EVENT QUEUE REGISTERED!\r\n");
        *retCode = MENU_FAILURE;
        return 0;
    }

    switch (rxBuf[0])
    {
    case 'a': // Display attached devices
        ri_test_SendString(sock,"\r\n");
        mpeos_mutexAcquire(gDeviceMutex);
        for (i = 0; i < gDevCount; i++)
        {
            if (gDevices[i].attached)
            {
                ri_test_SendString(sock,storageDeviceToString(i,&gDevices[i]));
            }
        }
        mpeos_mutexRelease(gDeviceMutex);
        break;
            
    case 'd': // Display detached devices
        ri_test_SendString(sock,"\r\n");
        mpeos_mutexAcquire(gDeviceMutex);
        for (i = 0; i < gDevCount; i++)
        {
            if (!gDevices[i].attached)
            {
                ri_test_SendString(sock,storageDeviceToString(i,&gDevices[i]));
            }
        }
        mpeos_mutexRelease(gDeviceMutex);
        break;

    case 'n': // Add new device
        if (gDevCount == MAX_STORAGE_DEVICES)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_STORAGE, "%s Already at max number of devices!\n",
                      __FUNCTION__);
            break;
        }
        mpeos_mutexAcquire(gDeviceMutex);
        {
            char buffer[OS_STORAGE_MAX_NAME_SIZE+1];
            char pathBuffer[OS_STORAGE_MAX_PATH_SIZE+1];
            char smallBuffer[16];
            mpeos_Storage* device = &gDevices[gDevCount];
            ri_test_SendString(sock,"\r\n");

            // Default status is detached/offline
            device->attached = FALSE;
            device->mediaPresent = TRUE;
            device->status = MPE_STORAGE_STATUS_OFFLINE;

            // Device name
            (void)ri_test_GetString(sock, buffer, OS_STORAGE_MAX_NAME_SIZE+1, "\r\nDevice name: ");
            strcpy(device->name,buffer);

            // Device display name
            (void)ri_test_GetString(sock, buffer, OS_STORAGE_MAX_NAME_SIZE+1, "\r\nDisplay name: ");
            strcpy(device->displayName,buffer);

            // Device size in megabytes
            device->size = 1048576 *
                ((uint64_t)(ri_test_GetNumber(sock, buffer, OS_STORAGE_MAX_NAME_SIZE+1,
                                              "\r\nDevice size (MB): ", 100000000)));

            // Media partition size in megabytes after initialization
            while (TRUE)
            {
                device->defaultMediaFsSize = 1048576 *
                    ((uint64_t)(ri_test_GetNumber(sock, buffer, OS_STORAGE_MAX_NAME_SIZE+1,
                                                  "\r\nMedia partition size after device initialization (MB): ", 1000000)));
                if (device->defaultMediaFsSize <= device->size)
                {
                    break;
                }

                ri_test_SendString(sock, "\r\n\nMedia partition must be smaller than device!\r\n");
            }
            
            // Media partition size is 0 until device is initialized
            device->mediaFsSize = 0;

            // Device type
            while (TRUE)
            {
                device->type = MPEOS_STORAGE_TYPE_INTERNAL;
                (void)ri_test_GetString(sock, smallBuffer, 16,
                                        "\r\nDevice type (i = Internal, d = Detachable, r = Removable, dr = Detachable/Removable): ");
                if (strcasecmp(smallBuffer,"i") == 0)
                {
                    break;
                }
                else if (strcasecmp(smallBuffer,"d") == 0)
                {
                    device->type |= MPEOS_STORAGE_TYPE_DETACHABLE;
                    break;
                }
                else if (strcasecmp(smallBuffer,"r") == 0)
                {
                    device->type |= MPEOS_STORAGE_TYPE_REMOVABLE;
                    break;
                }
                else if (strcasecmp(smallBuffer,"dr") == 0)
                {
                    device->type |= MPEOS_STORAGE_TYPE_DETACHABLE;
                    device->type |= MPEOS_STORAGE_TYPE_REMOVABLE;
                    break;
                }
            }

            // Root path
            (void)ri_test_GetString(sock, pathBuffer, OS_STORAGE_MAX_PATH_SIZE+1, "\r\nRoot path: ");
            strcpy(device->rootPath,pathBuffer);

            // Is device attached?
            if (device->type & MPEOS_STORAGE_TYPE_DETACHABLE)
            {
                while (TRUE)
                {
                    (void)ri_test_GetString(sock, smallBuffer, 16,
                                            "\r\nIs the device attached? (Y/N) :");
                    if (strcasecmp(smallBuffer,"y") == 0)
                    {
                        device->attached = TRUE;
                        break;
                    }
                    else if (strcasecmp(smallBuffer,"n") == 0)
                    {
                        break;
                    }
                }
            }

            // Is removable media present?
            if (device->attached && device->type & MPEOS_STORAGE_TYPE_REMOVABLE)
            {
                while (TRUE)
                {
                    (void)ri_test_GetString(sock, smallBuffer, 16,
                                            "\r\nIs removable media present? (Y/N) :");
                    if (strcasecmp(smallBuffer,"y") == 0)
                    {
                        break;
                    }
                    else if (strcasecmp(smallBuffer,"n") == 0)
                    {
                        device->mediaPresent = FALSE;
                        break;
                    }
                }
            }

            // Initialize?
            if (device->attached && device->mediaPresent)
            {
                while (TRUE)
                {
                    (void)ri_test_GetString(sock, smallBuffer, 16,
                                            "\r\nInitialize device? (Y/N - Select N for additional error status options) :");
                    if (strcasecmp(smallBuffer,"y") == 0)
                    {
                        if (initializeDevice(device) != MPE_STORAGE_ERR_NOERR)
                        {
                            device->status = MPE_STORAGE_STATUS_DEVICE_ERR;
                        }
                        break;
                    }
                    else if (strcasecmp(smallBuffer,"n") == 0)
                    {
                        (void)ri_test_GetString(sock, smallBuffer, 16,
                                                "\r\nSelect device status (P = Unsupported Device, F = Unsupported Format, U = Uninitialized, E = Device Error) :");
                        if (strcasecmp(smallBuffer, "p") == 0)
                        {
                            device->status = MPE_STORAGE_STATUS_UNSUPPORTED_DEVICE;
                            break;
                        }
                        else if (strcasecmp(smallBuffer, "f") == 0)
                        {
                            device->status = MPE_STORAGE_STATUS_UNSUPPORTED_FORMAT;
                            break;
                        }
                        else if (strcasecmp(smallBuffer, "u") == 0)
                        {
                            device->status = MPE_STORAGE_STATUS_UNINITIALIZED;
                            break;
                        }
                        else if (strcasecmp(smallBuffer, "e") == 0)
                        {
                            device->status = MPE_STORAGE_STATUS_DEVICE_ERR;
                            break;
                        }
                    }
                }
            }

            gDevCount++;

            // Send event to notify of new device status
            sendStorageEvent(device, MPE_EVENT_STORAGE_ATTACHED);
        }

        mpeos_mutexRelease(gDeviceMutex);
        break;

    case 's':
        {
            char buffer[4];
            int device = ri_test_GetNumber(sock, buffer, 4, "\r\nSelect device: ", -1);
            if (device == -1 || device >= gDevCount)
            {
                ri_test_SendString(sock, "\r\nInvalid device number!");
            }
            else
            {
                gTest_CurrentStorageDevice = device;
            }
        }
        break;

    case 'm':
        if (gTest_CurrentStorageDevice == -1)
        {
            ri_test_SendString(sock, "\r\nMust select a device first!");
            break;
        }
        if (!ri_test_SetNextMenu(sock, ri_test_FindMenu("MPEOS Storage Modify Device")))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_STORAGE, "%s: Could not select \"MPEOS Storage Modify Device\" menu!\n",
                      __FUNCTION__);
            break;
        }
        return 1;

    case 'x': // Exit
        return -1;

    default:
        strcat(rxBuf, " - unrecognized\r\n\n");
        ri_test_SendString(sock, rxBuf);
        *retCode = MENU_INVALID;
        break;
    } 

    // Always re-register our menus to update current device status in the
    // menu display
    test_registerMenus();

    return 0;
} 

static int test_storageMenuModifyInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    mpeos_Storage *device = &gDevices[gTest_CurrentStorageDevice];
    
    *retCode = MENU_SUCCESS;

    if (!gStorageMgrData.queueRegistered)
    {
        ri_test_SendString(sock, "\r\n\nNO STORAGE EVENT QUEUE REGISTERED!\r\n");
        *retCode = MENU_FAILURE;
        return 0;
    }

    switch (rxBuf[0])
    {
    case 'a': // Toggle attach state
        if (!(device->type & MPEOS_STORAGE_TYPE_DETACHABLE))
        {
            ri_test_SendString(sock, "\r\nCurrent device is not detachable!\r\n");
            *retCode = MENU_FAILURE;
            return 0;
        }
        
        mpeos_mutexAcquire(gDeviceMutex);
        if (device->attached)
        {
            // Mark device as detached
            device->attached = FALSE;
            
            // Send the event to notify of device added and set the status to OFFLINE
            sendStorageEvent(device, MPE_EVENT_STORAGE_DETACHED);
        }
        else
        {
            // Mark device as attached
            device->attached = TRUE;
            if (isInitialized(device))
            {
                device->status = MPE_STORAGE_STATUS_READY;
            }
            else
            {
                device->status = MPE_STORAGE_STATUS_UNINITIALIZED;
            }
            
            // Send the event to notify of device added and set the status to OFFLINE
            sendStorageEvent(device, MPE_EVENT_STORAGE_ATTACHED);
        }
        mpeos_mutexRelease(gDeviceMutex);
        break;

    case 'r': // Toggle removable media
        if (!(device->type & MPEOS_STORAGE_TYPE_REMOVABLE))
        {
            ri_test_SendString(sock, "\r\nCurrent device is not removable!\r\n");
            *retCode = MENU_FAILURE;
            return 0;
        }
        
        mpeos_mutexAcquire(gDeviceMutex);
        if (device->attached)
        {
            // Mark device as attached
            device->attached = FALSE;
            
            // Send the event to notify of device added and set the status to OFFLINE
            sendStorageEvent(device, MPE_EVENT_STORAGE_DETACHED);
        }
        else
        {
            // Mark device as attached
            device->attached = TRUE;
            device->status = MPE_STORAGE_STATUS_OFFLINE;
            
            // Send the event to notify of device added and set the status to OFFLINE
            sendStorageEvent(device, MPE_EVENT_STORAGE_ATTACHED);
        }
        mpeos_mutexRelease(gDeviceMutex);
        break;

    case 'i': // Initialize device
        mpeos_mutexAcquire(gDeviceMutex);
        if (!device->attached || !device->mediaPresent)
        {
            ri_test_SendString(sock, "\r\nCurrent device must be attached with media present!\r\n");
            *retCode = MENU_FAILURE;
            return 0;
        }
        if (isInitialized(device))
        {
            ri_test_SendString(sock, "\r\nCurrent device is already initialized!\r\n");
            *retCode = MENU_FAILURE;
            return 0;
        }
        if (initializeDevice(device) != MPE_SUCCESS)
        {
            ri_test_SendString(sock, "\r\nFailed to initialize current device!!\r\n");
            *retCode = MENU_FAILURE;
        }
        sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);
        mpeos_mutexRelease(gDeviceMutex);
        break;

    case 'u': // Uninitialize device
        mpeos_mutexAcquire(gDeviceMutex);
        if (!isInitialized(device))
        {
            ri_test_SendString(sock, "\r\nCurrent device is already uninitialized!\r\n");
            *retCode = MENU_FAILURE;
            return 0;
        }
        uninitializeDevice(device);
        sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);
        mpeos_mutexRelease(gDeviceMutex);
        break;

    case 's': // Set miscellaneous status
        {
            char buffer[4];
            mpeos_mutexAcquire(gDeviceMutex);
            while (TRUE)
            {
                (void)ri_test_GetString(sock, buffer, 4,
                                        "\r\nNew status? (B = Busy, R = Ready, D = Unsupported device, F = Unsupported format, E = Device error) :");
                if (strcasecmp(buffer, "b") == 0)
                {
                    if (device->status != MPE_STORAGE_STATUS_READY)
                    {
                        ri_test_SendString(sock, "\r\nDevice must be in \"READY\" state in order to make \"BUSY\"\r\n");
                        *retCode = MENU_FAILURE;
                    }
                    else
                    {
                        device->status = MPE_STORAGE_STATUS_BUSY;
                        sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);
                    }
                    break;
                }
                else if (strcasecmp(buffer, "r") == 0)
                {
                    if (device->status != MPE_STORAGE_STATUS_BUSY)
                    {
                        ri_test_SendString(sock, "\r\nDevice must be in \"BUSY\" state in order to make \"READY\"\r\n");
                        *retCode = MENU_FAILURE;
                    }
                    else
                    {
                        device->status = MPE_STORAGE_STATUS_READY;
                        sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);
                    }
                    break;
                }
                else if (strcasecmp(buffer, "d") == 0)
                {
                    uninitializeDevice(device);
                    device->status = MPE_STORAGE_STATUS_UNSUPPORTED_DEVICE;
                    sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);
                }
                else if (strcasecmp(buffer, "f") == 0)
                {
                    uninitializeDevice(device);
                    device->status = MPE_STORAGE_STATUS_UNSUPPORTED_FORMAT;
                    sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);
                }
                else if (strcasecmp(buffer, "e") == 0)
                {
                    uninitializeDevice(device);
                    device->status = MPE_STORAGE_STATUS_DEVICE_ERR;
                    sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);
                }
            }
            mpeos_mutexRelease(gDeviceMutex);
        }
        break;

    case 'x':
        return -1;

    default:
        strcat(rxBuf, " - unrecognized\r\n\n");
        ri_test_SendString(sock, rxBuf);
        *retCode = MENU_INVALID;
        return 0;
    }

    // Always re-register our menus to update current device status in the
    // menu display
    test_registerMenus();

    return 0;
}

static MenuItem MpeosStorageMenuItem_Main = {
    false, "s", "Storage", NULL, test_storageMenuMainInputHandler
};

#define STORAGE_TEST_MAX_MENU_LENGTH 1024

char gTest_StorageMainMenu[STORAGE_TEST_MAX_MENU_LENGTH];
#define STORAGE_TEST_MENU_MAIN \
    "\r\n" \
    "|---+---------------------------- \r\n" \
    "| a | Display attached devices    \r\n" \
    "|---+---------------------------- \r\n" \
    "| d | Display detached devices    \r\n" \
    "|---+---------------------------- \r\n" \
    "| n | Create new device           \r\n" \
    "|---+---------------------------- \r\n" \
    "| s | Select current device       \r\n" \
    "|---+---------------------------- \r\n" \
    "| m | Modify current device       \r\n" \
    "|---+---------------------------- \r\n" \
    "|---+                             \r\n" \
    "| Current Device =                \r\n" \
    "|   %s                            \r\n" 

static MenuItem MpeosStorageMenuItem_Modify = {
    false, "m", "MPEOS Storage Modify Device", NULL, test_storageMenuModifyInputHandler
};

char gTest_StorageModifyMenu[STORAGE_TEST_MAX_MENU_LENGTH];
#define STORAGE_TEST_MENU_MODIFY \
    "\r\n" \
    "|---+---------------------------- \r\n" \
    "| a | Toggle attached state       \r\n" \
    "|---+---------------------------- \r\n" \
    "| r | Toggle removable media      \r\n" \
    "|---+---------------------------- \r\n" \
    "| i | Initialize device           \r\n" \
    "|---+---------------------------- \r\n" \
    "| u | Uninitialize device         \r\n" \
    "|---+---------------------------- \r\n" \
    "| s | Set miscellaneous status    \r\n" \
    "|---+---------------------------- \r\n" \
    "|---+                             \r\n" \
    "| Current Device =                \r\n" \
    "|   %s                            \r\n"

static void test_registerMenus()
{
    // Register main menu
    snprintf(gTest_StorageMainMenu, STORAGE_TEST_MAX_MENU_LENGTH,
             STORAGE_TEST_MENU_MAIN,
             (gTest_CurrentStorageDevice == -1) ?
                 "Undefined" :
             storageDeviceToString(gTest_CurrentStorageDevice, &gDevices[gTest_CurrentStorageDevice]));

    if (!ri_test_RegisterMenu(&MpeosStorageMenuItem_Main))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_STORAGE,
                  "%s: Error registering MpeosStorageMenuItem_Main\n", __FUNCTION__);
    }

    // Register device modify menu
    snprintf(gTest_StorageModifyMenu, STORAGE_TEST_MAX_MENU_LENGTH,
             STORAGE_TEST_MENU_MODIFY,
             (gTest_CurrentStorageDevice == -1) ?
                 "Undefined" :
             storageDeviceToString(gTest_CurrentStorageDevice, &gDevices[gTest_CurrentStorageDevice]));

    if (!ri_test_RegisterMenu(&MpeosStorageMenuItem_Modify))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_STORAGE,
                  "%s: Error registering MpeosStorageMenuItem_Modify\n", __FUNCTION__);
    }
}

/******************************************************************************
 * PRIVATE functions
 *****************************************************************************/

// Update persistent device information
static mpe_Error updateDeviceData(os_StorageDeviceInfo* device)
{
    char filename[MPE_FS_MAX_PATH];
    mpe_File file;
    mpe_Error err;
    uint32_t count;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s - %s\n",
            __FUNCTION__, device->displayName);

    mpeos_mutexAcquire(device->mutex);
    sprintf(filename, "%s/%s", device->rootPath, DEVICE_DATA_FILENAME);

    // If the file doesn't already exist, then create it with default values
    if ((err = mpe_fileOpen(filename, MPE_FS_OPEN_CAN_CREATE
            | MPE_FS_OPEN_WRITE | MPE_FS_OPEN_TRUNCATE, &file)) == MPE_SUCCESS)
    {
        // Total device size
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s - size = "FMTU64"\n",
                __FUNCTION__, device->size);
        count = sizeof(device->size);
        if ((err = mpe_fileWrite(file, &count, (void*) &device->size))
                != MPE_SUCCESS)
            goto done;

        // Media filesystem size
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s - media fs size = "FMTU64"\n",
                __FUNCTION__, device->mediaFsSize);
        count = sizeof(device->mediaFsSize);
        if ((err = mpe_fileWrite(file, &count, (void*) &device->mediaFsSize))
                != MPE_SUCCESS)
            goto done;

        // Default media filesystem size
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s - default media fs size = "FMTU64"\n",
                __FUNCTION__, device->defaultMediaFsSize);
        count = sizeof(device->mediaFsSize);
        if ((err = mpe_fileWrite(file, &count,
                (void*) &device->defaultMediaFsSize)) != MPE_SUCCESS)
            goto done;
    }

    done: mpeos_mutexRelease(device->mutex);
    return err;
}

// Initialize the given device
static mpe_StorageError initializeDevice(mpe_StorageHandle device)
{
    mpe_Error err = MPE_SUCCESS;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s\n", __FUNCTION__);

    mpe_mutexAcquire(device->mutex);

    // Our devices are always formatted, so just remove all files
    if ((err = removeFiles(device->rootPath)) != MPE_FS_ERROR_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_STORAGE,
                "<<STORAGE>> %s - could not remove device directroy: %s\n",
                __FUNCTION__, device->rootPath);
        goto error;
    }

    // Make sure the full path to this device exists
    if (createFullPath(device->rootPath) != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_STORAGE,
                "<<STORAGE>> %s - could not create device directroy: %s\n",
                __FUNCTION__, device->rootPath);
        err = MPE_STORAGE_ERR_UNKNOWN;
        goto error;
    }

    // Reset our allocation information
    device->mediaFsSize = device->defaultMediaFsSize;
    device->freeMediaSize = device->mediaFsSize;

    // Finally, make sure all device data is persisted
    if ((err = updateDeviceData(device)) != MPE_SUCCESS)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_STORAGE,
                "<<STORAGE>> %s - could not update persistent device data: %s\n",
                __FUNCTION__, device->rootPath);
        goto error;
    }

    // Device is ready
    device->status = MPE_STORAGE_STATUS_READY;

    mpe_mutexRelease(device->mutex);

    return MPE_STORAGE_ERR_NOERR;

    error: mpe_mutexRelease(device->mutex);
    return err;
}

// Uninitialize the given device by removing its persistent info file
static void uninitializeDevice(mpe_StorageHandle device)
{
    char filename[MPE_FS_MAX_PATH];

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s\n", __FUNCTION__);

    // Create persistent data filename
    sprintf(filename, "%s/%s", device->rootPath, DEVICE_DATA_FILENAME);

    mpe_mutexAcquire(device->mutex);

    // Remove the device's persistent data file
    (void) mpe_fileDelete(filename);

    // Device is unitialized
    device->status = MPE_STORAGE_STATUS_UNINITIALIZED;

    mpe_mutexRelease(device->mutex);
}

// Returns TRUE if the given device has been initialized
static mpe_Bool isInitialized(mpe_StorageHandle device)
{
    mpe_File file;
    char filename[MPE_FS_MAX_PATH];

    // Create persistent data filename
    sprintf(filename, "%s/%s", device->rootPath, DEVICE_DATA_FILENAME);

    // Does the device info file exist for this device?
    if (mpe_fileOpen(filename, MPE_FS_OPEN_READ, &file) == MPE_SUCCESS)
    {
        mpe_fileClose(file);
        return TRUE;
    }

    return FALSE;
}

// Load persistent device information
static mpe_Error loadDeviceData(mpe_StorageHandle device)
{
    char filename[MPE_FS_MAX_PATH];
    mpe_File file;
    mpe_Error err;
    uint32_t count;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s - %s\n",
            __FUNCTION__, device->displayName);

    sprintf(filename, "%s/%s", device->rootPath, DEVICE_DATA_FILENAME);

    if ((err = mpe_fileOpen(filename, MPE_FS_OPEN_READ, &file)) == MPE_SUCCESS)
    {
        // Total device size
        count = sizeof(device->size);
        if ((err = mpe_fileRead(file, &count, (void*) &device->size)) != MPE_SUCCESS)
        {
            goto done;
        }
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s - size = "FMTU64"\n",
                  __FUNCTION__, device->size);

        // Media filesystem size
        count = sizeof(device->mediaFsSize);
        if ((err = mpe_fileRead(file, &count, (void*) &device->mediaFsSize)) != MPE_SUCCESS)
        {
            goto done;
        }
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s - media fs size = "FMTU64"\n",
                  __FUNCTION__, device->mediaFsSize);

        // Default media filesystem size
        count = sizeof(device->mediaFsSize);
        if ((err = mpe_fileRead(file, &count, (void*) &device->defaultMediaFsSize)) != MPE_SUCCESS)
        {
            goto done;
        }
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s - default media fs size = "FMTU64"\n",
                  __FUNCTION__, device->defaultMediaFsSize);
    }

    // Device is ready
    device->status = MPE_STORAGE_STATUS_READY;
    
 done:
    return err;
}

static void setupAllDevices()
{
    char devEnvName[20 + (MAX_STORAGE_DEVICES % 10) + 1]; // includes EOS character
    char* devInfoFromIni = NULL;
    char* pDevData;
    int i;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s\n", __FUNCTION__);

    devInfoFromIni = (char *) mpeos_envGet("STORAGE.DEVICE.0");

    mpeos_mutexAcquire(gDeviceMutex);

    // If there was no information for devices in mpeenv.ini, then use the
    // default devices.  Initialize if not already initialized.
    if (devInfoFromIni == NULL)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
                "<<STORAGE>> %s - using default devices\n", __FUNCTION__);

        gDevCount = sizeof(DEFAULT_DEVICES) / sizeof(DEFAULT_DEVICES[0]);

        // Setup the default devices
        for (i = 0; i < gDevCount; i++)
        {
            memcpy(&gDevices[i], &DEFAULT_DEVICES[i], sizeof(mpeos_Storage));
            mpeos_mutexNew(&gDevices[i].mutex);

            // Fill out the file paths in the devices
            sprintf(gDevices[i].rootPath, "%s/%s", gStorageRoot,
                    gDevices[i].name);

            // Create the device directory if the device data file does not exist
            if (isInitialized(&gDevices[i]))
                (void) loadDeviceData(&gDevices[i]);
            else
                (void) initializeDevice(&gDevices[i]);

            gDevices[i].volumes = NULL;
        }
    }
    else
    {
        while (devInfoFromIni != NULL && gDevCount < MAX_STORAGE_DEVICES)
        {
            mpe_Bool isInited;

            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
                    "<<STORAGE>> %s - found device info: %s\n", __FUNCTION__,
                    devInfoFromIni);

            strcpy(gDevices[gDevCount].name, strtok(devInfoFromIni, ","));
            strcpy(gDevices[gDevCount].displayName, strtok(NULL, ","));
            sprintf(gDevices[gDevCount].rootPath, "%s/%s", gStorageRoot,
                    gDevices[gDevCount].name);

            mpeos_mutexNew(&gDevices[gDevCount].mutex);
            gDevices[gDevCount].volumes = NULL;

            // Does the device info file exist for this device?
            isInited = isInitialized(&gDevices[gDevCount]);

            pDevData = strtok(NULL, ",");

            //Set type based upon the device name
            if (strcmp(pDevData, "D") == 0)
                gDevices[gDevCount].type = MPEOS_STORAGE_TYPE_DETACHABLE;
            else if (strcmp(pDevData, "R") == 0)
                gDevices[gDevCount].type = MPEOS_STORAGE_TYPE_REMOVABLE;
            else if (strcmp(pDevData, "DR") == 0)
                gDevices[gDevCount].type = MPEOS_STORAGE_TYPE_DETACHABLE
                        | MPEOS_STORAGE_TYPE_REMOVABLE;
            else
                gDevices[gDevCount].type = MPEOS_STORAGE_TYPE_INTERNAL;

            // Init device data
            pDevData = strtok(NULL, ",");

            // Initialize the device if the requested state is not 'IC' or is 'IC' and
            // isn't already initialized
            if (pDevData && (toupper(*pDevData) == 'I') && ((toupper(*(pDevData
                    + 1)) != 'C') || !isInited))
            {
                // Get device size and allocation data from INI file
                gDevices[gDevCount].size = (uint64_t) atof(strtok(NULL, ","));
                gDevices[gDevCount].defaultMediaFsSize = (uint64_t) atof(
                        strtok(NULL, ","));
                gDevices[gDevCount].mediaFsSize
                        = gDevices[gDevCount].defaultMediaFsSize;

                (void) initializeDevice(&gDevices[gDevCount]);
            }
            // Unitialized - meaning prepared but not initialized
            else if (pDevData && (toupper(*pDevData) == 'U'))
            {
                // Get device size and allocation data from INI file
                gDevices[gDevCount].size = (uint64_t) atof(strtok(NULL, ","));
                gDevices[gDevCount].defaultMediaFsSize = (uint64_t) atof(
                        strtok(NULL, ","));
                gDevices[gDevCount].mediaFsSize
                        = gDevices[gDevCount].defaultMediaFsSize;

                uninitializeDevice(&gDevices[gDevCount]);
            }
            // Default is 'C' - leave as current - either unitialized or load data
            // from file
            else if (isInited)
            {
                (void) loadDeviceData(&gDevices[gDevCount]);
            }

            // When the mpeos_dvr initializes the media volumes it will update
            // the device's available media size

            gDevices[gDevCount].attached = TRUE;
            gDevices[gDevCount].mediaPresent = TRUE;

            // Get the next device from the ini file
            sprintf(devEnvName, "STORAGE.DEVICE.%d", ++gDevCount);
            devInfoFromIni = (char *) mpeos_envGet(devEnvName);
        }
    }

    mpeos_mutexRelease(gDeviceMutex);
}

static mpe_Bool isValidDevice(mpe_StorageHandle device)
{
    mpe_Bool result = FALSE;
    int ctr;

    mpeos_mutexAcquire(gDeviceMutex);

    for (ctr = 0; ctr < gDevCount; ctr++)
    {
        if (device == &gDevices[ctr] /*&& (gDevices[ctr].attached == true)*/)
        {
            result = TRUE;
            break;
        }
    }

    mpeos_mutexRelease(gDeviceMutex);

    return result;
}

/******************************************************************************
 * MPEOS private functions
 *****************************************************************************/

char *storageGetRoot()
{
    return gStorageRoot;
}

os_StorageDeviceInfo *storageGetDefaultDevice()
{
    return &gDevices[0];
}

/******************************************************************************
 * PUBLIC functions
 *****************************************************************************/

/**
 * <i>mpeos_storageInit()</i>
 *
 * This function initializes the MPE OS storage manager module.  This API must
 * be called only once per boot cycle.  It must be called before any other MPE
 * Storage module APIs are called.  Any MPE Storage module calls made before
 * this function is called will result in an MPE_STORAGE_ERR_OUT_OF_STATE error
 * being returned to the caller
 */
void mpeos_storageInit(void)
{
    char *storageRoot;
    char* reset = (char *) mpeos_envGet("STORAGE.RESET.ON.START");
    char defaultDir[MPE_FS_MAX_PATH];
    char tmp[MPE_FS_MAX_PATH];

    // Register our test menus
    MpeosStorageMenuItem_Main.text = gTest_StorageMainMenu;
    MpeosStorageMenuItem_Modify.text = gTest_StorageModifyMenu;
    test_registerMenus();

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> mpeos_storageInit\n");

    if (mpeos_mutexNew(&gDeviceMutex) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_STORAGE, "<<STORAGE>> mpeos_storageInit could not initialize device mutex\n");
        return;
    }

    gStorageMgrData.queueRegistered = FALSE;

    if ((storageRoot = (char *) mpeos_envGet("STORAGE.ROOT")) == NULL)
    {
        storageRoot = (char *) DEFAULT_STORAGE_ROOT;
    }
    strcpy(gStorageRoot, storageRoot);

    // Convert the storage root to an absolute, port-specific path if it
    // begins with '/syscwd'
    if (strncmp(gStorageRoot, "/syscwd", strlen("/syscwd")) == 0)
    {
        (void) mpeos_filesysGetDefaultDir(defaultDir, MPE_FS_MAX_PATH);

        // Remove any trailing slash
        if (defaultDir[strlen(defaultDir) - 1] == '/')
            defaultDir[strlen(defaultDir) - 1] = '\0';

        sprintf(tmp, "%s%s", defaultDir, gStorageRoot + strlen("/syscwd"));
        strcpy(gStorageRoot, tmp);
    }

    if (reset != NULL && toupper(*reset) == 'T')
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
                "<<STORAGE>> mpeos_storageInit: resetting all devices\n");
        (void) removeFiles(gStorageRoot);
    }

    if (createFullPath(gStorageRoot) != 0)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_STORAGE,
                "<<STORAGE>> mpeos_storageInit: could not create device root: %s\n",
                gStorageRoot);
        return;
    }

    setupAllDevices();
}

/**
 *  <i>mpeos_storageRegisterQueue()</i>
 *
 * This function registers a specified queue to receive storage related events.
 * Only one queue may be registered at a time.  Subsequent calls replace
 * previously registered queue.
 *
 * @param eventQueue specifies the destination queue for storage events
 * @param act specifies the asynchronous completion token.  Events delivered by the
 *            MPE Storage module to the specified queue will carry this value in the
 *            optionalData2 parameter.  The actual value of this token should be the
 *            Event Dispatcher handle (mpe_EdEventInfo*) created by a call to
 *            mpe_edCreateHandle.
 *
 * @return MPE_STORAGE_ERR_NOERR if no error
 *         MPE_STORAGE_ERR_INVALID_PARAM if specified queue is invalid
 */
mpe_StorageError mpeos_storageRegisterQueue(mpe_EventQueue eventQueue,
        void *act)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
            "<<STORAGE>> mpeos_storageRegisterQueue\n");

    gStorageMgrData.listenerQueue = eventQueue;
    gStorageMgrData.listenerACT = act;
    gStorageMgrData.queueRegistered = TRUE;

    return MPE_STORAGE_ERR_NOERR;
}

/**
 * <i>mpeos_storageGetDeviceCount()</i>
 *
 * This function retireves the number of storage devices found on the platform
 *
 * @param count indicates the number of devices found on output
 *
 * @return MPE_STORAGE_ERR_NOERR if no error
 *         MPE_STORAGE_ERR_INVALID_PARAM if specified NULL parameter
 */
mpe_StorageError mpeos_storageGetDeviceCount(uint32_t* count)
{
    *count = gDevCount;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s - count = %d\n",
            __FUNCTION__, *count);

    return MPE_STORAGE_ERR_NOERR;
}

/**
 * <i>mpeos_storageGetDeviceList()</i>
 *
 * This function retrieves the list of storage devices found on the platform.
 * The first device in the list returned is the default storage device for the
 * platform (e.g. internal HDD).
 *
 * @param count on input, this parameter specifies the number of device handles
 *              that can fit into the pre-allocated devices array passed as the
 *              second parameter.  On output, indicates the actual number of device
 *              handles returned in the devices array.
 * @param devices specifies a pre-allocated memory buffer for returning an array
 *                of native storage handles.
 *
 * @return MPE_STORAGE_ERR_NOERR if no error
 *         MPE_STORAGE_ERR_INVALID_PARAM if specified NULL parameter
 *         MPE_STORAGE_ERR_BUF_TOO_SMALL indicates that the pre-allocated devices
 *                                       buffer is not large enough to fit all of the
 *                                       native device handles found on the platform.
 *                                       Only as many devices as can fit in the buffer
 *                                       are returned.
 */
mpe_StorageError mpeos_storageGetDeviceList(uint32_t* count,
        mpe_StorageHandle *devices)
{
    mpe_StorageError result = MPE_STORAGE_ERR_OUT_OF_STATE;
    uint32_t deviceCount;
    uint32_t i;

    if (count == NULL || devices == NULL)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
                "<<STORAGE>> %s - NULL parameters!\n", __FUNCTION__);
        return MPE_STORAGE_ERR_INVALID_PARAM;
    }

    // Determine if the input buffer is large enough */
    (void) mpeos_storageGetDeviceCount(&deviceCount); // Should the return value be checked here?
    if (*count < deviceCount)
    {
        result = MPE_STORAGE_ERR_BUF_TOO_SMALL;
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_STORAGE,
                "<<STORAGE>> %s - Input buffer is too small, only returning %d out of %d devices!\n",
                __FUNCTION__, *count, deviceCount);
    }
    else
    {
        *count = deviceCount;
        result = MPE_STORAGE_ERR_NOERR;
    }

    // Copy storage devices to caller's buffer
    for (i = 0; i < deviceCount; i++)
    {
        devices[i] = &gDevices[i];
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
                "<<STORAGE>> %s - Device Name = %s\n", __FUNCTION__,
                devices[i]->displayName);
    }

    return result;
}

/**
 *  <i>mpeos_storageGetInfo()</i>
 *
 * This function is used to retrieve information about a given storage device.
 *
 * @param device specifies the device
 * @param param specifies the attribute of interest
 * @param output specifies the attribute value on output
 *
 * @return MPE_STORAGE_ERR_NOERR if no error
 *         MPE_STORAGE_ERR_INVALID_PARAM if invalid parameter was specified
 *         MPE_STORAGE_ERR_OUT_OF_STATE if queried path name before device has entered the ready state
 */
mpe_StorageError mpeos_storageGetInfo(mpe_StorageHandle device,
        mpe_StorageInfoParam param, void* output)
{
    volatile mpe_StorageError result = MPE_STORAGE_ERR_OUT_OF_STATE;

    if (!output || !isValidDevice(device))
    {
        result = MPE_STORAGE_ERR_INVALID_PARAM;
    }
    else
    {
        result = MPE_STORAGE_ERR_NOERR;
        switch (param)
        {
        case MPE_STORAGE_DISPLAY_NAME:
            strcpy((char*) output, device->displayName);
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
                    "<<STORAGE>> %s - displayName = %s\n", __FUNCTION__,
                    (char*) output);
            break;

        case MPE_STORAGE_NAME:
            strcpy((char*) output, device->name);
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
                    "<<STORAGE>> %s (%s) - name = %s\n", __FUNCTION__,
                    device->displayName, (char*) output);
            break;

        case MPE_STORAGE_FREE_SPACE:
            *((uint64_t*) output) = device->size - device->mediaFsSize
                    + device->freeMediaSize;
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s (%s) - free space = "FMTU64"\n",
                    __FUNCTION__, device->displayName, *((uint64_t*)output));
            break;

        case MPE_STORAGE_CAPACITY:
            *((uint64_t*) output) = device->size;
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s (%s) - size = "FMTU64"\n",
                    __FUNCTION__, device->displayName, *((uint64_t*)output));
            break;

        case MPE_STORAGE_STATUS:
            *((mpe_StorageStatus*) output) = device->status;
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
                    "<<STORAGE>> %s (%s) - status = %d\n", __FUNCTION__,
                    device->displayName, *((uint32_t*) output));
            break;

        case MPE_STORAGE_GPFS_PATH:
            strcpy(output, device->rootPath);
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
                    "<<STORAGE>> %s (%s) - GPFS path = %s\n", __FUNCTION__,
                    device->displayName, (char*) output);
            break;

        case MPE_STORAGE_GPFS_FREE_SPACE:
            // This is currently not used by any JNI or MPE code.  Not sure why we need it
            result = MPE_STORAGE_ERR_UNSUPPORTED;
            break;

        case MPE_STORAGE_MEDIAFS_PARTITION_SIZE:
            *((uint64_t*) output) = device->mediaFsSize;
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s (%s) - media fs size = "FMTU64"\n",
                    __FUNCTION__, device->displayName, *((uint64_t*)output));
            break;

        case MPE_STORAGE_MEDIAFS_FREE_SPACE:
            *((uint64_t*) output) = device->freeMediaSize;
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s (%s) - free media size = "FMTU64"\n",
                    __FUNCTION__, device->displayName, *((uint64_t*)output));
            break;

        case MPE_STORAGE_SUPPORTED_ACCESS_RIGHTS:
            // Support all access on all devices
            *((uint8_t*) output) = MPE_STORAGE_ACCESS_RIGHT_WORLD_READ
                    | MPE_STORAGE_ACCESS_RIGHT_WORLD_WRITE
                    | MPE_STORAGE_ACCESS_RIGHT_APP_READ
                    | MPE_STORAGE_ACCESS_RIGHT_APP_WRITE
                    | MPE_STORAGE_ACCESS_RIGHT_ORG_READ
                    | MPE_STORAGE_ACCESS_RIGHT_ORG_WRITE
                    | MPE_STORAGE_ACCESS_RIGHT_OTHER_READ
                    | MPE_STORAGE_ACCESS_RIGHT_OTHER_WRITE;
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
                    "<<STORAGE>> %s (%s) - access rights = %d\n", __FUNCTION__,
                    device->displayName, *((uint8_t*) output));
            break;

        default:
            result = MPE_STORAGE_ERR_INVALID_PARAM;
            break;
        }
    }

    return result;
}

/**
 * <i>mpeos_storageIsDetachable</i>
 *
 * This function is used to determine whether the specified device is a
 * detachable storage device
 *
 * @param device specifies the handle to the storage device
 * @param value specfies a pointer to a boolean result that, on output, will
 *        indicate true if the specified device is a detachable storage device.
 *        False otherwise.
 *
 * @return MPE_STORAGE_ERR_NOERR if no error
 *         MPE_STORAGE_ERR_INVALID_PARAM otherwise
 */
mpe_StorageError mpeos_storageIsDetachable(mpe_StorageHandle device,
        mpe_Bool* value)
{
    volatile mpe_StorageError result = MPE_STORAGE_ERR_OUT_OF_STATE;

    if (!value || !isValidDevice(device))
    {
        result = MPE_STORAGE_ERR_INVALID_PARAM;
    }
    else
    {
        if (device->type & MPEOS_STORAGE_TYPE_DETACHABLE)
            *value = TRUE;
        else
            *value = FALSE;

        result = MPE_STORAGE_ERR_NOERR;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE, "<<STORAGE>> %s (%s) - %s\n",
                __FUNCTION__, device->displayName, *value ? "TRUE" : "FALSE");
    }

    return result;
}

/**
 * <i>mpeos_storageMakeReadyToDetach()</i>
 *
 * This function makes the specified device ready to be safely detached.
 * Any pending I/O operations on this device are immediately cancelled.
 * This call will result in the device transitioning to the
 * MPE_STORAGE_STATUS_OFFLINE state if it is capable of being brought
 * back online without physically detaching and reattaching the device.
 * Otherwise, the device will be removed from the device list and an
 * MPE_EVENT_STORAGE_DETACHED event will be delivered to the registered queue.
 * Further I/O attempts on this device while in this state will fail until the
 * device is reattached or brought back online via the mpe_storageMakeReadyForUse() API.
 *
 * Events
 *    MPE_EVENT_STORAGE_CHANGED - triggered if device is successfully taken offline
 *                                and is capable of being brought back online without
 *                                physically detaching and reattaching the device.
 *                                This event is also triggered if the device enters
 *                                an unrecoverable error state.
 *    MPE_EVENT_STORAGE_DETACHED - triggered if device is successfully made ready to
 *                                detach and is incapable of being brought back online
 *                                without physically detaching and reattaching the device.
 *
 * @param device specifies a handle to the storage device
 *
 * @return MPE_STORAGE_ERR_NOERR if success
 *         MPE_STORAGE_ERR_INVALID_PARAM if specified invalid device
 *         MPE_STORAGE_ERR_UNSUPPORTED if specified non-detachable device
 *         MPE_STORAGE_ERR_DEVICE_ERR if unable to make the device safe to detach
 *
 * S-A platform implementation notes:
 *    This function will be implemented as a stub that always returns
 *    MPE_STORAGE_ERR_UNSUPPORTED.  The reason is because no detachable storage devices
 *    will be presented to apps on the target S-A platforms.
 */
mpe_StorageError mpeos_storageMakeReadyToDetach(mpe_StorageHandle device)
{
    volatile mpe_StorageError result = MPE_STORAGE_ERR_OUT_OF_STATE;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
            "<<STORAGE>> mpeos_storageMakeReadyToDetach\n");

    if (!isValidDevice(device) || !(device->type
            & MPEOS_STORAGE_TYPE_DETACHABLE))
    {
        result = MPE_STORAGE_ERR_INVALID_PARAM;
    }
    else
    {
        mpeos_mutexAcquire(device->mutex);
        // TODO: <STMGR-COMP> Disable/remove any logical storage/media storage volumes
        device->status = MPE_STORAGE_STATUS_OFFLINE;
        result = MPE_STORAGE_ERR_NOERR;

        sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);

        mpeos_mutexRelease(device->mutex);
    }

    return result;
}

/**
 * <i>mpeos_storageMakeReadyForUse</i>
 *
 * This function makes the specified detachable storage device ready to be used
 * after having previously been made ready to detach.  If the specified device is
 * in the MPE_STORAGE_STATUS_OFFLINE state, this call attempts to activate the
 * device and make it available as if it was newly attached.  As a result of this
 * call, the device will transition to one of the following states.
 *    MPE_STORAGE_STATUS_READY
 *    MPE_STORAGE_STATUS_DEVICE_ERR
 *    MPE_STORAGE_STATUS_UNINITIALIZED
 *    MPE_STORAGE_STATUS_UNSUPPORTED_DEVICE
 *    MPE_STORAGE_STATUS_UNSUPPORTED_FORMAT
 *
 * This function will not format an unformatted device and will not reformat a
 * device with an unsupported format.
 *
 * Events
 *    MPE_EVENT_STORAGE_CHANGED - device changed states
 *
 * @param device specifies a handle to the storage device
 *
 * @return MPE_STORAGE_ERR_NOERR if device is ready for use.
 *         MPE_STORAGE_ERR_INVALID_PARAM if specified invalid device
 *         MPE_STORAGE_ERR_UNSUPPORTED if specified non-detachable device
 *         MPE_STORAGE_ERR_DEVICE_ERR if unable to make the device ready for use
 */
mpe_StorageError mpeos_storageMakeReadyForUse(mpe_StorageHandle device)
{
    volatile mpe_StorageError result = MPE_STORAGE_ERR_OUT_OF_STATE;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
            "<<STORAGE>> mpeos_storageMakeReadyForUse\n");
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
            "<<STORAGE>> MPE_STORAGE_STATUS = %d \n", device->status);

    if (!isValidDevice(device))
    {
        result = MPE_STORAGE_ERR_INVALID_PARAM;
    }
    else if ((device->status != MPE_STORAGE_STATUS_NOT_PRESENT)
            && (device->status != MPE_STORAGE_STATUS_READY) && (device->status
            != MPE_STORAGE_STATUS_OFFLINE))
    {
        result = MPE_STORAGE_ERR_DEVICE_ERR;
    }
    else
    {
        mpeos_mutexAcquire(device->mutex);

        if (isInitialized(device))
        {
            result = MPE_STORAGE_ERR_NOERR;
            device->status = MPE_STORAGE_STATUS_READY;
        }
        else
        {
            result = MPE_STORAGE_ERR_DEVICE_ERR;
            device->status = MPE_STORAGE_STATUS_UNINITIALIZED;
        }

        sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);

        mpeos_mutexRelease(device->mutex);
    }

    return result;
}

/**
 *  <i>mpeos_storageIsDvrCapable</i>
 *
 * This function is used to determine whether the specified device is capable
 * of storing DVR content within a media storage device.
 *
 * @param device specifies a handle to the storage device;
 * @return MPE_STORAGE_ERR_NOERR if no error
 *         MPE_STORAGE_ERR_INVALID_PARAM otherwise
 */
mpe_StorageError mpeos_storageIsDvrCapable(mpe_StorageHandle device,
        mpe_Bool* value)
{

    // TODO: Add "NULL" value for mediafs size and then return false if there is no mediafs

    volatile mpe_StorageError result = MPE_STORAGE_ERR_OUT_OF_STATE;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
            "<<STORAGE>> mpeos_storageIsDvrCapable\n");

    if (!value || !isValidDevice(device))
    {
        result = MPE_STORAGE_ERR_INVALID_PARAM;
    }
    else
    {
        *value = TRUE;
        result = MPE_STORAGE_ERR_NOERR;
    }

    return result;
}

/**
 * <i>mpeos_storageIsRemovable</i>
 *
 * This function is used to determine whether the specified device is capable of
 * housing removable storage media
 *
 * @param device specifies a handle to the storage device
 * @param value is a pointer to a boolean result that, on output, indicates true
 *              if the specified device is a physical bay that can house
 *              removable storage media.  Otherwise, false.
 *
 * @return MPE_STORAGE_ERR_NOERR if no error
 *         MPE_STORAGE_ERR_INVALID_PARAM otherwise
 *
 * S-A platform implementation notes
 *    This function will be implemented as a stub that always indicates FALSE and
 *    returns MPE_STORAGE_ERR_NOERR.  The reason is because no removable storage
 *    devices will be presented to apps on the target S-A platforms.
 */
mpe_StorageError mpeos_storageIsRemovable(mpe_StorageHandle device,
        mpe_Bool* value)
{
    volatile mpe_StorageError result = MPE_STORAGE_ERR_OUT_OF_STATE;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
            "<<STORAGE>> mpeos_storageIsRemovable\n");

    if (!value || !isValidDevice(device))
    {
        result = MPE_STORAGE_ERR_INVALID_PARAM;
    }
    else
    {
        if (device->type & MPEOS_STORAGE_TYPE_REMOVABLE)
            *value = TRUE;
        else
            *value = FALSE;

        result = MPE_STORAGE_ERR_NOERR;
    }

    return result;
}

/**
 * <i>mpeos_storageEject</i>
 *
 * This function the removable media storage device to be physically ejected
 * from its bay if applicable.  If this operation is not applicable to the
 * storage device, this function does nothing and returns success.
 *
 * @param device specifies a handle to the storage device
 *
 * @return MPE_STORAGE_ERR_NOERR if success or no op
 *         MPE_STORAGE_ERR_INVALID_PARAM if specified invalid parameter
 *         MPE_STORAGE_ERR_UNSUPPORTED if specified device is not capable of
 *                                     housing removable media
 *
 * No support for removable devices currently exists.  Therefore, always
 * return MPE_STORAGE_ERR_UNSUPPORTED.
 */
mpe_StorageError mpeos_storageEject(mpe_StorageHandle device)
{
    volatile mpe_StorageError result = MPE_STORAGE_ERR_OUT_OF_STATE;

    // TODO: <STMGR-COMP> Remove/disable any logical storage/media storage volumes

    if (!isValidDevice(device))
    {
        result = MPE_STORAGE_ERR_INVALID_PARAM;
    }
    else
    {
        mpeos_mutexAcquire(device->mutex);
        device->size = 0;
        device->defaultMediaFsSize = 0;
        device->mediaFsSize = 0;
        device->status = MPE_STORAGE_STATUS_NOT_PRESENT;

        sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);

        mpeos_mutexRelease(device->mutex);
    }

    return result;
}

/**
 * <i>mpeos_storageInitializeDevice</i>
 *
 *  This function initializes the specified storage device for use.  It is
 * usually called on a newly attached storage device that is not currently
 * suitable for use (i.e., UNSUPPORTED_FORMAT or UNINITIALIZED), but may also
 * be called to reformat or repartition a storage device that is in the READY
 * state.  On successful invocation, the state of the device will enter the
 * MPE_STORAGE_STATUS_BUSY state while initialization is in progress then will
 * enter one of the following final states when the operation is complete
 *    MPE_STORAGE_STATUS_READY
 *    MPE_STORAGE_STATUS_DEVICE_ERR
 *
 * An MPE_EVENT_STORAGE_CHANGED event will be delivered to the registered queue
 * when the operation is initiated and again when it is complete.
 *
 * @param device specifies the storage device to (re)initialize
 * @param userAuthorized is a Boolean indicating whether the (re)initialize operation
 *        was explicitly authorized by the end user.
 * @param mediaFsSize is a pointer to uint64_t indicating the minimum size of the
 *        MEDIAFS partition requested for this device in bytes.  A value of zero
 *        bytes indicates that no minimum MEDIAFS size is requested.  A non-NULL
 *        value indicates that the caller is requesting the device to be repartitioned.
 *        The effects of repartitioning the device may include loss of application
 *        data on this device (On S-A platforms, repartitioning the device will
 *        definitely result in loss of all data  stored on the device).  A NULL
 *        value indicates that the caller is requesting the device to be formatted
 *        which will result in loss of all application data stored on the device
 *        (On S-A platforms, reformatting the device will result in loss of all
 *        data stored on this device).  If the device is in the READY state and
 *        NULL is specified, this call will reformat each partition without
 *        repartitioning the device.  If the device is in the UNINITIALIZED or
 *        UNSUPPORTED_FORMAT state and NULL is specified, the device will be
 *        partitioned with platform-specific default sizes for MEDIAFS and GPFS.
 *
 * @return MPE_STORAGE_ERR_NOERR if operation was initiated successfully
 *         MPE_STORAGE_ERR_INVALID_PARAM if specified invalid device or mediaFsSize
 *              greater than storage capacity of device or non-zero mediaFsSize for
 *              non-DVR capable storage device.
 *         MPE_STORAGE_ERR_BUSY if initialization is already in progress for this device
 *         MPE_STORAGE_ERR_UNSUPPORTED if the specified storage device is an unsupported device
 *         MPE_STORAGE_ERR_DEVICE_ERR if failed to initialize the device for use
 *         MPE_STORAGE_ERR_OUT_OF_STATE if device is not in a valid state for
 *              partitioning or formatting (e.g. OFFLINE, NOT_PRESENT, UNSUPPORTED_DEVICE)
 */
mpe_StorageError mpeos_storageInitializeDevice(mpe_StorageHandle device,
        mpe_Bool userAuthorized, uint64_t* mediaFsSize)
{
    volatile mpe_StorageError result = MPE_STORAGE_ERR_OUT_OF_STATE;

    MPE_UNUSED_PARAM(userAuthorized);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_STORAGE,
            "<<STORAGE>> mpeos_storageInitializeDevice\n");

    if (!isValidDevice(device))
    {
        result = MPE_STORAGE_ERR_INVALID_PARAM;
    }
    else if (device->status == MPE_STORAGE_STATUS_BUSY)
    {
        result = MPE_STORAGE_ERR_BUSY;
    }
    else if (device->status == MPE_STORAGE_STATUS_OFFLINE)
    {
        result = MPE_STORAGE_ERR_OUT_OF_STATE;
    }
    else
    {
        mpe_StorageStatus originalStatus;
        uint64_t originalMediaSize;

        mpeos_mutexAcquire(device->mutex);

        // Cache the device's original status and media size then make it busy
        originalStatus = device->status;
        originalMediaSize = device->mediaFsSize;
        device->status = MPE_STORAGE_STATUS_BUSY;
        sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);

        // This will format the device and reset the media size to default
        (void) initializeDevice(device);

        if (mediaFsSize == NULL)
        {
            if (originalStatus == MPE_STORAGE_STATUS_READY)
            {
                // If the status was READY, restore the original media size
                device->mediaFsSize = originalMediaSize;
            }
        }
        else
            device->mediaFsSize = *mediaFsSize;

#ifdef MPE_FEATURE_DVR
        {
            os_MediaVolumeInfo* walker;
            // Release all the media volumes for this device
            walker = device->volumes;
            while (walker != NULL)
            {
                device->volumes = walker->next;
                deleteMediaVolume(walker);
                walker = device->volumes;
            }
        }
#endif

        sendStorageEvent(device, MPE_EVENT_STORAGE_CHANGED);

        (void) updateDeviceData(device);

        mpeos_mutexRelease(device->mutex);

        result = MPE_STORAGE_ERR_NOERR;
    }

    return result;
}
