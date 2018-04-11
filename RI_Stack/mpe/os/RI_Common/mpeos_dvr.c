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

#include <mpe_file.h>

#include <mpeos_dvr.h>
#include <mpeos_time.h>
#include <mpeos_mem.h>
#include <mpeos_dbg.h>
#include <mpeos_thread.h>
#include <mpeos_sync.h>
#include <mpeos_util.h>
#include <mpeos_disp.h>
#include <platform.h>
#include <os_dvr.h>
#include <os_storage.h>
#include <os_media.h>

#include <glib.h>

#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include <ri_pipeline_manager.h>

#include <ri_test_interface.h>
#include "test_3dtv.h"

#define MPE_MEM_DEFAULT MPE_MEM_DVR

/* DVR Playback */
typedef struct _os_DvrTsbPlayback
{
    mpe_DvrPidInfo pids[MPE_DVR_MAX_PIDS]; /* pids associated with the playback */
    uint32_t pidCount; /* number of pids */
    ri_video_device_t *videoDevice; /* playback video device handle */
    float trickMode; /* current play scale */
    mpe_EventQueue evQueue; /* for event notification */
    void *act; /* notification completion token */
    uint64_t currentTime; /* the current playback position (nanoseconds) */
} os_DvrTsbPlayback;

/* DVR Playback */
typedef struct _os_DvrPlayback
{
    mpe_DvrPidInfo pids[MPE_DVR_MAX_PIDS]; /* pids associated with the playback */
    uint32_t pidCount; /* number of pids */
    ri_video_device_t *videoDevice; /* playback video device handle */
    float trickMode; /* current play scale */
    mpe_EventQueue evQueue; /* for event notification */
    void *act; /* notification completion token */
    char datafile[MPE_FS_MAX_PATH]; /* full path to metadata file */
    mpe_Mutex mutex;
} os_DvrPlayback;

/* DVR Recording */
typedef struct _os_DvrRecording
{
    mpe_DvrTsb buffer; /* TSb from which the recording is done */
    mpe_EventQueue evQueue; /* for event notification */
    uint64_t convertStartTime; /* Time when this recording was started (in TSB media time ns) */
    void *act; /* notification completion token */
    char datafile[MPE_FS_MAX_PATH]; /* full path to metadata file */
    os_MediaVolumeInfo *volume;
} os_DvrRecording;

/* Active DVR Time-shift Buffering Session */
typedef struct _os_DvrTimeShift
{
    uint32_t tunerID; /* tuner ID : source of the buffering */
    mpe_DvrPidInfo pids[MPE_DVR_MAX_PIDS]; /* pids associated with the recording */
    uint32_t pidCount; /* number of pids */
    mpe_EventQueue evQueue; /* for event notification */
    void *act; /* notification completion token */
    mpe_Cond startCond; /* Allows us to wait until we receive the START event from the platform */
} os_DvrTimeShift;

/* DVR Time-Shift Buffer */
typedef struct _os_DvrTimeShiftBuffer
{
    mpe_Bool isFull;
    uint64_t duration; /* buffer duration in seconds */
    uint64_t size; /* buffer size in bytes */
    uint64_t sysStartTime; /* system start time (nanoseconds) when the TSB was started */
    uint64_t startTime; /* current start time (nanoseconds) offset from sysStartTime */
    uint64_t endTime; /* current end time (nanoseconds) offset from sysStartTime */
    os_DvrTsbPlayback *playback; /* identifies an active tsb playback */
    os_DvrRecording *recording; /* identifies an active tsb conversion */
    os_DvrTimeShift *buffering; /* identifies an active tsb buffering session */
    os_StorageDeviceInfo *device; /* storage device where this TSB is located */
    ri_tsbHandle platformHandle; /* platform TSB handle */
    mpe_Mutex mutex;
} os_DvrTimeShiftBuffer;

/* Recording metadata file contents */
typedef struct _os_RecFileInfo
{
    uint64_t currentLength; /* The current length of this recording (in nanoseconds) */
    uint64_t playbackTime; /* The current playback position (in nanoseconds) */
    uint64_t size; /* The size on disk (in bytes) of this recording */
    uint32_t pidCount; /* Number of PIDs specified in the mpe_DvrPidInfo array */
    mpe_DvrPidInfo pids[MPE_DVR_MAX_PIDS]; /* Array of PIDs actually recorded for this media */
} os_RecFileInfo;

typedef struct _VolumeMgrData
{
    mpe_EventQueue listenerQueue;
    void* listenerACT; // asynchronous completion token
    mpe_Bool queueRegistered;
} volumeMgrData;

/********************************************************************************/
/*                      DVR globals and support functions                       */
/********************************************************************************/

#define MPE_MEM_DEFAULT MPE_MEM_DVR

/* maximum supported bit rate */
mpe_DvrBitRate gMaxBitRate = MPE_DVR_BITRATE_HIGH;

/* List of recordings (allocated locally) */
static mpe_DvrString_t *gRecNames; /* recording name list */

static float playScaleList[] =
{ -64.0, -32.0, -16.0, -8.0, -4.0, -2.0, -1.0, 0, 0.5, 1.0, 2.0, 4.0, 8.0,
        16.0, 32.0, 64.0 };
static int playScaleListSize = 16; /* Size of the above array */

/* define number of nanoseconds in one second */
#define NANO_SECONDS 1000000000LL
#define NANO_MILLIS 1000000

// Maximum playback/recording numbers
static const uint32_t MAX_PLAYBACK = 1;
static const uint32_t MAX_TSBS = 50;

// global definitions
static os_DvrPlayback** gActivePlay = NULL; // active playbacks
static mpe_Mutex gPlayMutex;
static os_DvrTimeShiftBuffer** gTimeShiftBuffers = NULL; // master allocated TSB list (active and non-active)
static mpe_Mutex gTsbMutex;
static mpe_Mutex gRecFileIOMutex;

static volumeMgrData gVolumeMgrData;

// Fake implementation of 3DTV - default to 2D
static mpe_Media3DPayloadType g_payloadType = 0;
static mpe_DispStereoscopicMode g_stereoscopicMode = 0;
static uint8_t *g_payload = NULL;
static uint32_t g_payloadSz = 0;

// Fake implementation of input video scan mode
static mpe_MediaScanMode g_videoScanMode = SCANMODE_UNKNOWN;

// Prototypes
mpe_DispDeviceDest dispGetDestDevice(mpe_DispDevice disp);
ri_video_device_t* dispGetVideoDevice(mpe_DispDevice disp);

static mpe_DvrPlayback *getCurrentRecordingPlaybackSession()
{
    int i;
    mpe_DvrPlayback *playback = NULL;
    mpeos_mutexAcquire(gPlayMutex);

    for (i = 0; i < MAX_PLAYBACK; i++)
    {
        playback = (mpe_DvrPlayback*)gActivePlay[i];
        break;
    }
    mpeos_mutexRelease(gPlayMutex);
    return playback;
}

static int testRecordingPlaybackInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    int ret = 0;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s(%d, %s);\n",
              __FUNCTION__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    mpe_DvrPlayback *playback = getCurrentRecordingPlaybackSession();
    if (playback == NULL)
    {
        ri_test_SendString(sock, "\r\n\nNO CURRENT RECORDING PLAYBACK SESSION -- EVENTS WILL NOT BE SENT!\r\n");
    }

#ifdef __linux__
    mpe_EventQueue queue = 0;
#else
    mpe_EventQueue queue = NULL;
#endif
    void* act = NULL;
    if (playback != NULL)
    {
        queue = ((os_DvrPlayback*)playback)->evQueue;
        act = ((os_DvrPlayback*)playback)->act;
    }

    ret = test3DTVInputHandler(sock, rxBuf, queue, act,
            &g_payloadType, &g_stereoscopicMode, &g_payload, &g_payloadSz, 
            &g_videoScanMode);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s - Exit %d\n", __FUNCTION__, ret);
    return ret;
}

static MenuItem MpeosRecPlaybackMenuItem =
{ false, "r", "Recording Playback", MPEOS_3DTV_TESTS, testRecordingPlaybackInputHandler };

static os_DvrTsbPlayback *getCurrentTSBPlaybackSession()
{
    uint32_t i;
    os_DvrTimeShiftBuffer* tsb = NULL;
    os_DvrTsbPlayback* playback = NULL;

    mpeos_mutexAcquire(gTsbMutex);
    for (i = 0; i < MAX_TSBS; i++)
    {
         tsb = gTimeShiftBuffers[i];
         if (tsb != NULL && tsb->playback != NULL)
         {
            playback = tsb->playback;
            break;
         }
    }
    mpeos_mutexRelease(gTsbMutex);

    return playback;
}

static int testTSBPlaybackInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    int ret = 0;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s(%d, %s);\n",
              __FUNCTION__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    os_DvrTsbPlayback *tsb = getCurrentTSBPlaybackSession();
    if (tsb == NULL)
    {
        ri_test_SendString(sock, "\r\n\nNO CURRENT TSB PLAYBACK SESSION -- EVENTS WILL NOT BE SENT!\r\n");
    }

#ifdef __linux__
    mpe_EventQueue queue = 0;
#else
    mpe_EventQueue queue = NULL;
#endif
    void* act = NULL;
    if (tsb != NULL)
    {
        queue = tsb->evQueue;
        act = tsb->act;
    }

    ret = test3DTVInputHandler(sock, rxBuf, queue, act,
            &g_payloadType, &g_stereoscopicMode, &g_payload, &g_payloadSz,
            &g_videoScanMode);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s - Exit %d\n", __FUNCTION__, ret);
    return ret;
}

static MenuItem MpeosTSBPlaybackMenuItem =
{ false, "t", "TSB Playback", MPEOS_3DTV_TESTS, testTSBPlaybackInputHandler };

#define MPEOS_TSB_BUFFERING_TESTS \
    "\r\n" \
    "|---+-----------------------\r\n" \
    "| c | Simulate CCI update (TSB + live decode)  \r\n" \

static os_DvrTimeShift *getCurrentTSBBufferingSession()
{
    uint32_t i;
    os_DvrTimeShiftBuffer* tsb = NULL;
    os_DvrTimeShift* buffering = NULL;

    mpeos_mutexAcquire(gTsbMutex);
    for (i = 0; i < MAX_TSBS; i++)
    {
         tsb = gTimeShiftBuffers[i];
         if (tsb != NULL && tsb->buffering != NULL)
         {
            buffering = tsb->buffering;
            break;
         }
    }
    mpeos_mutexRelease(gTsbMutex);

    return buffering;
}

static int testTSBBufferingInputHandler(int sock, char *rxBuf, int *retCode, char **retStr)
{
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s(%d, %s);\n",
              __FUNCTION__, sock, rxBuf);
    *retCode = MENU_SUCCESS;

    if (strstr(rxBuf, "x"))
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "%s - Exit -1\n", __FUNCTION__);
        return -1;
    }

    mpe_EventQueue bufQueue = 0;
    void* bufACT;

    os_DvrTimeShift *buffering = getCurrentTSBBufferingSession();
    if (buffering != NULL)
    {
        bufQueue = buffering->evQueue;
        bufACT = buffering->act;
    }


    if (strstr(rxBuf, "c"))
    {
        int emi = 0;
        int aps = 0;
        int cit = 0;
        int rct = 0;
        int cci = 0;

        char buf[64];

        ri_test_SendString(sock, "\r\n\n" \
            "  OC-SP-CCCP2.0 CableCARD Copy Protection 2.0   \r\n" \
            " Table 9.1-2 - EMI Values and Copy Permissions  \r\n" \
            "+-----------+----------------------------------+\r\n" \
            "| EMI Value |     Digital Copy Permission      |\r\n" \
            "+-----------+----------------------------------+\r\n" \
            "| 00b (0)   | Copying not restricted           |\r\n" \
            "+-----------+----------------------------------+\r\n" \
            "| 01b (1)   | No further copying is permitted  |\r\n" \
            "+-----------+----------------------------------+\r\n" \
            "| 10b (2)   | One generation copy is permitted |\r\n" \
            "+-----------+----------------------------------+\r\n" \
            "| 11b (3)   | Copying prohibited               |\r\n" \
            "+-----------+----------------------------------+\r\n");
        emi = ri_test_GetNumber(sock, buf, sizeof(buf),
            "\r\nEnter desired EMI value (0 - 3): ", 0);
        if (emi < 0 || emi > 3)
        {
            ri_test_SendString(sock, "\r\n\nInvalid EMI value!\r\n");
            *retCode = MENU_FAILURE;
            return 0;
        }

        ri_test_SendString(sock, "\r\n\n" \
            "    OC-SP-CCCP2.0 CableCARD Copy Protection 2.0    \r\n" \
            "        Table 9.1-3 - APS Value Definitions        \r\n" \
            "+---------+---------------------------------------+\r\n" \
            "|   APS   |              Description              |\r\n" \
            "+---------+---------------------------------------+\r\n" \
            "| 00b (0) | Copy Protection Encoding Off          |\r\n" \
            "+---------+---------------------------------------+\r\n" \
            "| 01b (1) | AGC Process On, Splt Burst Off        |\r\n" \
            "+---------+---------------------------------------+\r\n" \
            "| 10b (2) | AGC Process On, 2 Line Split Burst On |\r\n" \
            "+---------+---------------------------------------+\r\n" \
            "| 11b (3) | AGC Process On, 4 Line Split Burst On |\r\n" \
            "+---------+---------------------------------------+\r\n");
        aps = ri_test_GetNumber(sock, buf, sizeof(buf),
            "\r\nEnter desired APS value (0 - 3): ", 0);
        if (aps < 0 || aps > 3)
        {
            ri_test_SendString(sock, "\r\n\nInvalid APS value!\r\n");
            *retCode = MENU_FAILURE;
            return 0;
        }

        ri_test_SendString(sock, "\r\n\n" \
            "OC-SP-CCCP2.0 CableCARD Copy Protection 2.0\r\n" \
            "    Table 9.1-4 - CIT Value Definitions    \r\n" \
            "+---------+-------------------------------+\r\n" \
            "|   CIT   |          Description          |\r\n" \
            "+---------+-------------------------------+\r\n" \
            "|  0b (0) | No Image Constraint asserted  |\r\n" \
            "+---------+-------------------------------+\r\n" \
            "|  1b (1) | Image Constraint asserted     |\r\n" \
            "+---------+-------------------------------+\r\n");
        cit = ri_test_GetNumber(sock, buf, sizeof(buf),
            "\r\nEnter desired APS value (0 - 3): ", 0);
        if (cit < 0 || cit > 1)
        {
            ri_test_SendString(sock, "\r\n\nInvalid CIT value!\r\n");
            *retCode = MENU_FAILURE;
            return 0;
        }

        ri_test_SendString(sock, "\r\n\n" \
            "  OC-SP-CCCP2.0 CableCARD Copy Protection 2.0  \r\n" \
            "      Table 9.1-4 - RCT Value Definitions      \r\n" \
            "+--------+------------------------------------+\r\n" \
            "|  RCT   |            Description             |\r\n" \
            "+--------+------------------------------------+\r\n" \
            "| 0b (0) | No Redistribution Control asserted |\r\n" \
            "+---------+-----------------------------------+\r\n" \
            "| 1b (1) | Redistribution Control asserted    |\r\n" \
            "+--------+------------------------------------+\r\n");
        rct = ri_test_GetNumber(sock, buf, sizeof(buf),
            "\r\nEnter desired RCT value (0 - 1): ", 0);
        if (rct < 0 || rct > 1)
        {
            ri_test_SendString(sock, "\r\n\nInvalid RCT value!\r\n");
            *retCode = MENU_FAILURE;
            return 0;
        }

        cci = emi | (aps << 2) | (cit << 4) | (rct << 5);

        if (bufQueue != 0)
        {
            mpeos_eventQueueSend(bufQueue, MPE_DVR_EVT_CCI_UPDATE, (void*)0, bufACT, cci);
            ri_test_SendString(sock, "Sent CCI update to buffering queue\r\n\n\r\n");
        }
        if (os_mediaNotifyCCIUpdate(cci) == MPE_SUCCESS)
        {
            char *success = "SUCCESS sending CCI update to decode queue\r\n";
            *retStr = (char *) g_malloc (strlen(success) + 1);
            strcpy(*retStr, success);
            ri_test_SendString(sock, "Sent CCI update to decode queue\r\n\n\r\n");
        }
        else
        {
            char *failure = "FAILURE sending CCI update to decode queue\r\n";
            *retStr =(char *) g_malloc (strlen(failure) + 1);
            strcpy(*retStr, failure);
            ri_test_SendString(sock, "Failure sending CCI update to decode queue\r\n\n\r\n");
        }
    }

    return 0;
}

static MenuItem MpeosTSBBufferingMenuItem =
{ false, "b", "TSB Buffering", MPEOS_TSB_BUFFERING_TESTS, testTSBBufferingInputHandler };

static void initVolumes()
{
    uint32_t i;
    uint32_t deviceCount = MAX_STORAGE_DEVICES;
    os_StorageDeviceInfo *devices[MAX_STORAGE_DEVICES];
    char mediaVolumesDatafile[MPE_FS_MAX_PATH];
    char path[MPE_FS_MAX_PATH];
    mpe_File f;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s\n", __FUNCTION__);

    // Iterate through all devices looking for existing media volumes
    (void) mpeos_storageGetDeviceList(&deviceCount, devices);
    for (i = 0; i < deviceCount; i++)
    {
        uint32_t numVolumes;
        int64_t offset = 0;
        uint32_t bytes;
        uint32_t j;

        devices[i]->freeMediaSize = devices[i]->mediaFsSize;

        // Remove any pre-existing TSBs and then create a new TSB directory
        // for this device
        sprintf(path, "%s/%s", devices[i]->rootPath, OS_DVR_TSB_DIR_NAME);
#ifdef MPE_WINDOWS
        // Convert /c/ to c:/
        path[0] = path[1];
        path[1] = ':';
#endif
        (void) removeFiles(path);
        (void) createFullPath(path);

        // Open our media volumes datafile for this device
        sprintf(mediaVolumesDatafile, "%s/%s", devices[i]->rootPath,
                OS_DVR_VOLUME_DATA_FILENAME);
        if (mpe_fileOpen(mediaVolumesDatafile, MPE_FS_OPEN_READ, &f)
                != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR,
                    "<<DVR>> %s - No media volumes for device: %s\n",
                    __FUNCTION__, devices[i]->displayName);
            continue;
        }

        // Seek to the start of the file and read the number of volumes
        (void) mpe_fileSeek(f, MPE_FS_SEEK_SET, &offset);
        bytes = sizeof(numVolumes);
        (void) mpe_fileRead(f, &bytes, (void*) &numVolumes);

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - Device (%s) found %d volumes\n", __FUNCTION__,
                devices[i]->name, numVolumes);

        // Load info for each pre-existing media volume
        for (j = 0; j < numVolumes; j++)
        {
            uint32_t stringLength;
            os_MediaVolumeInfo *volume;

            // Create volume data structure and populate it from the volume
            // data file
            (void) mpe_memAlloc(sizeof(os_MediaVolumeInfo), (void**) &volume);

            bytes = sizeof(stringLength);

            // Paths
            (void) mpe_fileRead(f, &bytes, (void*) &stringLength);
            (void) mpe_fileRead(f, &stringLength, (void*) volume->rootPath);
            volume->rootPath[stringLength] = '\0';
            (void) mpe_fileRead(f, &bytes, (void*) &stringLength);
            (void) mpe_fileRead(f, &stringLength, (void*) volume->mediaPath);
            volume->mediaPath[stringLength] = '\0';
            (void) mpe_fileRead(f, &bytes, (void*) &stringLength);
            (void) mpe_fileRead(f, &stringLength, (void*) volume->dataPath);
            volume->dataPath[stringLength] = '\0';

            // Volume size
            bytes = sizeof(volume->reservedSize);
            (void) mpe_fileRead(f, &bytes, (void*) &volume->reservedSize);

            // Volume used size
            bytes = sizeof(volume->usedSize);
            (void) mpe_fileRead(f, &bytes, (void*) &volume->usedSize);

            // Initialize the rest of the data
            volume->device = devices[i];
            volume->next = NULL;
            volume->alarmList = NULL;
            volume->tsbMinSizeBytes = 0;
            mpeos_mutexNew(&(volume->mutex));

            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Volume (%p) path = %s, reserved size = %"PRIu64", used size = %"PRIu64"\n",
                    __FUNCTION__, volume, volume->rootPath, volume->reservedSize, volume->usedSize);

            // Update the device's free space
            if (volume->reservedSize == 0)
                devices[i]->freeMediaSize -= volume->usedSize;
            else
                devices[i]->freeMediaSize -= volume->reservedSize;

            MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "<<DVR>> %s - Device free size is now %"PRIu64"\n",
                    __FUNCTION__, devices[i]->freeMediaSize);

            // Add this volume to the device's list of volumes
            volume->next = devices[i]->volumes;
            devices[i]->volumes = volume;
        }

        mpe_fileClose(f);
    }
}

/**
 * Update the persistent file that tracks all media volume information for
 * the given storage device
 */
static void updateVolumeDataFile(os_StorageDeviceInfo* device)
{
    mpe_File f;
    char path[MPE_FS_MAX_PATH];
    uint32_t numVolumes = 0;
    uint32_t bytes;
    os_MediaVolumeInfo* volume;
    uint32_t i;

    // Create our volume datafile pathname
    sprintf(path, "%s/%s", device->rootPath, OS_DVR_VOLUME_DATA_FILENAME);

    // Count the volumes on this device
    for (volume = device->volumes; volume != NULL; volume = volume->next)
        numVolumes++;

    // Write the data file for the volume
    (void) mpe_fileOpen(path, MPE_FS_OPEN_CAN_CREATE | MPE_FS_OPEN_TRUNCATE
            | MPE_FS_OPEN_WRITE, &f);
    bytes = sizeof(numVolumes);
    (void) mpe_fileWrite(f, &bytes, &numVolumes);

    volume = device->volumes;
    for (i = 0; i < numVolumes; i++)
    {
        uint32_t stringLength;

        // Write each member of the os_MediaVolumeInfo structure, strings are stored
        // preceded by a 32bit length value

        stringLength = strlen(volume->rootPath);
        bytes = sizeof(stringLength);
        (void) mpe_fileWrite(f, &bytes, &stringLength);
        (void) mpe_fileWrite(f, &stringLength, volume->rootPath);

        stringLength = strlen(volume->mediaPath);
        bytes = sizeof(stringLength);
        (void) mpe_fileWrite(f, &bytes, &stringLength);
        (void) mpe_fileWrite(f, &stringLength, volume->mediaPath);

        stringLength = strlen(volume->dataPath);
        bytes = sizeof(stringLength);
        (void) mpe_fileWrite(f, &bytes, &stringLength);
        (void) mpe_fileWrite(f, &stringLength, volume->dataPath);

        bytes = sizeof(volume->reservedSize);
        (void) mpe_fileWrite(f, &bytes, (void*) &volume->reservedSize);

        bytes = sizeof(volume->usedSize);
        (void) mpe_fileWrite(f, &bytes, (void*) &volume->usedSize);

        // NOTE: Alarm list and TSB min size not persisted

        volume = volume->next;
    }

    mpe_fileClose(f);
}

/* <i>dvrMediaGetUsedSize()</i>
 * Returns the total amount of used media space for all volumes on a device.
 *
 * @param: device Device for which to get the used media size
 *
 * @return: Total amount of space used by all media volumes on a device
 *
 */
uint64_t dvrMediaGetUsedSize(os_StorageDeviceInfo *device)
{
    return device->mediaFsSize - device->freeMediaSize;
}

/*
 * Removes the given media volume from our global list and frees its data
 * structures and files on-disk
 */
void deleteMediaVolume(os_MediaVolumeInfo* volume)
{
    (void) removeFiles(volume->rootPath);
    mpe_mutexDelete(volume->mutex);
    mpe_memFree(volume);
}

/**
 * Gets the OS specific tsb platform handle.
 *
 * @param   opaque datastructure which is really pointer to underlying OS structure.
 *
 * @return  pointer to underlying platform datastructure which is a ri_tsbHandle
 */
ri_tsbHandle* getTsbHandle(mpe_DvrTsb buffer)
{
    os_DvrTimeShiftBuffer* tsBuffer = (os_DvrTimeShiftBuffer*) buffer;
    return tsBuffer->platformHandle;
}

/*
 * OCAP Recording names for this port are formatted like this:
 *     [location]/[platform_name]
 *
 * Where [location] is the MediaStorageVolume root path minus the
 * system storage root path as defined by storageGetRoot() (in os_storage.h).
 * For example, if the system storage root path is:
 *
 *     /storage/x1/x2
 *
 * and the MediaStorageVolume root path is:
 *
 *     /storage/x1/x2/device1/volumes/1234/1234/myVolume
 *
 * then [location] is:
 *
 *    device1/volumes/1234/1234/myVolume
 *
 * [platform_name] is the platform-specified recording name
 *
 * Returns 0 if successful, or -1 if an error occurred.
 */
static int expandRecordingName(const char* recordingName,
        char location[OS_FS_MAX_PATH],
        char platformRecName[RI_MAX_RECORDING_NAME_LENGTH])
{
    const char* delimiter;

    MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_DVR, "<<DVR>> %s - recordingName = %s\n",
            __FUNCTION__, recordingName);

    // Find last '/' char which separates recording name from location
    if ((delimiter = strrchr(recordingName, '/')) == NULL)
        return -1;

    strncpy(location, recordingName, delimiter - recordingName);
    location[delimiter - recordingName] = '\0';
    strcpy(platformRecName, delimiter + 1);

    MPEOS_LOG(
            MPE_LOG_TRACE3,
            MPE_MOD_DVR,
            "<<DVR>> %s - recordingName = %s, location = %s, platformRecName = %s\n",
            __FUNCTION__, recordingName, location, platformRecName);

    return 0;
}

/**
 * Expands an OCAP recording name into components that can be used to:
 * call platform APIs to manipulate DVR media
 *
 * Returns 0 if successful, or -1 if an error occurred.
 */
static int convertRecordingNameToPlatform(const char* recordingName,
        char path[OS_DVR_MEDIA_VOL_MAX_PATH_SIZE],
        char platformRecName[RI_MAX_RECORDING_NAME_LENGTH])
{
    char location[OS_FS_MAX_PATH];

    MPEOS_LOG(MPE_LOG_TRACE3, MPE_MOD_DVR, "<<DVR>> %s - recordingName = %s\n",
            __FUNCTION__, recordingName);

    if (expandRecordingName(recordingName, location, platformRecName) == -1)
        return -1;

    /* We always pass the "MEDIA" directory to the platform */
    sprintf(path, "%s/%s/%s", storageGetRoot(), location, OS_DVR_MEDIA_DIR_NAME);

#ifdef MPE_WINDOWS
    // Convert /c/ to c:/
    path[0] = path[1];
    path[1] = ':';
#endif

    MPEOS_LOG(
            MPE_LOG_TRACE3,
            MPE_MOD_DVR,
            "<<DVR>> %s - recordingName = %s, path = %s, platformRecName = %s\n",
            __FUNCTION__, recordingName, path, platformRecName);

    return 0;
}

/**
 * Converts an OCAP recording name into the full path to its associated
 * datafile
 *
 * Returns 0 if successful, or -1 if an error occurred.
 */
int convertRecordingNameToDatafile(const char* recordingName,
        char datafile[OS_DVR_MEDIA_VOL_MAX_PATH_SIZE])
{
    char location[OS_FS_MAX_PATH];
    char platformRecName[RI_MAX_RECORDING_NAME_LENGTH];

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - recordingName = %s\n",
            __FUNCTION__, recordingName);

    if (expandRecordingName(recordingName, location, platformRecName) == -1)
        return -1;

    /* Datafile is named exactly same as platform name, but it lives in
     "MEDIA_DATA" directory */
    sprintf(datafile, "%s/%s/%s/%s", storageGetRoot(), location,
            OS_DVR_MEDIA_DATA_DIR_NAME, platformRecName);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - recordingName = %s, datafile = %s\n", __FUNCTION__,
            recordingName, datafile);

    return 0;
}

/* <i>findVolumeByRecordingName()</i>
 * This function finds a volume by searching the recording name in the media
 * data directory.  !!IMPORTANT!! When a matching volume is found containing
 * the given recording, the caller has acquired ownership of the volume's
 * device mutex.  This prevents the volume from being deleted once you have
 * identified it.  So, if this function returns non-null, you must release
 * the volume's device mutex!
 *
 * @param: recordingName the recording name from which to exract the volume
 *
 * @return: volume or NULL of not found
 */
static os_MediaVolumeInfo *findVolumeByRecordingName(char *recordingName)
{
    uint32_t i;
    char path[MPE_FS_MAX_PATH];
    uint32_t deviceCount = MAX_STORAGE_DEVICES;
    os_StorageDeviceInfo *devices[MAX_STORAGE_DEVICES];
    os_MediaVolumeInfo *volume;
    char location[OS_FS_MAX_PATH];
    char platformRecName[RI_MAX_RECORDING_NAME_LENGTH]; // ignored

    strcpy(path, recordingName);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - recordingName = %s\n",
            __FUNCTION__, recordingName);

    // Build the volume root path from the recording name
    if (expandRecordingName(recordingName, location, platformRecName) == -1)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid recording name!  %s\n", __FUNCTION__,
                recordingName);
        return NULL;
    }
    sprintf(path, "%s/%s", storageGetRoot(), location);

    // Find the device by name in the list of available devices
    (void) mpeos_storageGetDeviceList(&deviceCount, devices);

    for (i = 0; i < deviceCount; i++)
    {
        mpeos_mutexAcquire(devices[i]->mutex);
        for (volume = devices[i]->volumes; volume != NULL; volume
                = volume->next)
        {
            if (strncmp(path, volume->rootPath, strlen(volume->rootPath)) == 0)
            {
                MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                        "<<DVR>> %s - found volume (%p)\n", __FUNCTION__,
                        volume);
                // Intentionally not releasing device mutex!  See function comment
                return volume;
            }
        }
        mpeos_mutexRelease(devices[i]->mutex);
    }

    return NULL;
}

/* <i>notifyDvrQueue()</i>
 * This function sends an event to registered dvr queues.
 * Events can be sent to playback and/or recording queues
 *
 * @param:
 *  evQueue         The event queue that will receive the event
 *  act             optional data associated with the event
 *  evt             the event to send.
 * @return:         MPE_SUCCESS if the event is sent successfully. An error, otherwise.
 */
static void notifyDvrQueue(mpe_EventQueue evQueue, void *act, uint32_t evt)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - queueID: 0x%x, event: 0x%X\n", __FUNCTION__, evQueue,
            evt);

    mpeos_eventQueueSend(evQueue, evt, NULL, act, 0);
}

/* <i>notifyDvrQueueWithArg()</i>
 * This function sends an event to registered dvr queues.
 * Events can be sent to playback and/or recording queues.
 * Function supports passing data1 argument
 * @param:
 *  evQueue         The event queue that will receive the event
 *  act             optional data associated with the event
 *  evt             the event to send.
 *  data1           the data1 value to send
 * @return:         MPE_SUCCESS if the event is sent successfully. An error, otherwise.
 */
static void notifyDvrQueueWithArg(mpe_EventQueue evQueue, void *act, uint32_t evt, void * data1)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - queueID: 0x%x, event: 0x%X, data1: 0x%X\n", __FUNCTION__, evQueue,
            evt, data1);

    mpeos_eventQueueSend(evQueue, evt, data1, act, 0);
}

static mpe_Error getRecFileInfo(char* filename, os_RecFileInfo* recData)
{
    mpe_File f;
    uint32_t readCount = sizeof(os_RecFileInfo);

    if (recData == NULL || filename == NULL)
        return MPE_DVR_ERR_INVALID_PARAM;

    mpeos_mutexAcquire(gRecFileIOMutex);
    
    MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_DVR, "<<DVR>> %s - filename = %s\n",
            __FUNCTION__, filename);

    if (mpe_fileOpen(filename, MPE_FS_OPEN_READ | MPE_FS_OPEN_CAN_CREATE, &f)
            != MPE_SUCCESS)
    {
        mpeos_mutexRelease(gRecFileIOMutex);
        return MPE_DVR_ERR_OS_FAILURE;
    }

    if (mpe_fileRead(f, &readCount, (void*) recData) != MPE_SUCCESS)
    {
        mpe_fileClose(f);
        mpeos_mutexRelease(gRecFileIOMutex);
        return MPE_DVR_ERR_OS_FAILURE;
    }

    MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_DVR, "<<DVR>> %s - currentLength = %"PRIu64", playbackTime = %"PRIu64", size = %"PRIu64", pidCount = %d\n",
            __FUNCTION__, recData->currentLength,
            recData->playbackTime, recData->size, recData->pidCount);

    mpe_fileClose(f);
    mpeos_mutexRelease(gRecFileIOMutex);

    return MPE_SUCCESS;
}

static mpe_Error setRecFileInfo(char* filename, os_RecFileInfo* recData)
{
    mpe_File f;
    uint32_t writeCount = sizeof(os_RecFileInfo);

    if (recData == NULL || filename == NULL)
        return MPE_DVR_ERR_INVALID_PARAM;

    mpeos_mutexAcquire(gRecFileIOMutex);
    MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_DVR, "<<DVR>> %s - currentLength = %"PRIu64", playbackTime = %"PRIu64", size = %"PRIu64", pidCount = %d\n",
            __FUNCTION__, recData->currentLength,
            recData->playbackTime, recData->size, recData->pidCount);

    if (mpe_fileOpen(filename, MPE_FS_OPEN_WRITE | MPE_FS_OPEN_CAN_CREATE
            | MPE_FS_OPEN_TRUNCATE, &f) != MPE_SUCCESS)
    {
        mpeos_mutexRelease(gRecFileIOMutex);
        return MPE_DVR_ERR_OS_FAILURE;
    }

    if (mpe_fileWrite(f, &writeCount, (void*) recData) != MPE_SUCCESS)
    {
        mpe_fileClose(f);
        mpeos_mutexRelease(gRecFileIOMutex);
        return MPE_DVR_ERR_OS_FAILURE;
    }

    mpe_fileClose(f);
    mpeos_mutexRelease(gRecFileIOMutex);

    return MPE_SUCCESS;
}

void sendVolumeAlarms(os_MediaVolumeInfo *volume)
{
    uint8_t freeSpacePercent;
    os_VolumeSpaceAlarm* walker;
    uint64_t volumeSize;
    uint64_t freeSize;

    if (volume->reservedSize == 0)
        return;

    /* This loop will take care of the levels between prev and present levels */
    mpeos_mutexAcquire(volume->mutex);

    /* Does this volume have a pre-allocated size? */
    volumeSize = (volume->reservedSize == 0) ? volume->device->mediaFsSize
            : volume->reservedSize;
    freeSize = (volume->reservedSize == 0) ? (volume->device->freeMediaSize)
            : (volume->reservedSize - volume->usedSize);

    /* calculate percentage of present available space */
    freeSpacePercent = (uint8_t)(((double) freeSize / (double) volumeSize)
            * 100);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - Current free space level is %u\n", __FUNCTION__,
            freeSpacePercent);

    walker = volume->alarmList;

    /* Re-arm alarms at or below the current free space level so that
     alarm may fire next time free space drops below that threshold level */
    while (walker != NULL && walker->level <= freeSpacePercent)
    {
        if (walker->status == UNARMED)
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                    "<<DVR>> %s - Resetting free space alarm for level %u\n",
                    __FUNCTION__, walker->level);
            walker->status = ARMED;
        }

        walker = walker->next;
    }

    /* Fire armed alarms above the current free space level then disarm the alarm to
     prevent redundant alarms on subsequent polling intervals. */
    while (walker != NULL && walker->level <= MAX_MEDIA_VOL_LEVEL)
    {
        if (walker->status == ARMED)
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                    "<<DVR>> %s - Sending free space alarm for level %u\n",
                    __FUNCTION__, walker->level);

            /* The last parameter value is sent with the level value
             so that the same listener can differentiate among various
             levels it has registered */
            if (gVolumeMgrData.queueRegistered)
            {
                mpeos_eventQueueSend(gVolumeMgrData.listenerQueue,
                        MPE_MEDIA_VOL_EVT_FREE_SPACE_ALARM, (void*) volume,
                        gVolumeMgrData.listenerACT, walker->level);
            }

            walker->status = UNARMED;
        }
        walker = walker->next;
    }

    mpeos_mutexRelease(volume->mutex);
}

/*
 * Find the TSB associated with the given TSB buffering session and safely
 * acquire its mutex
 *
 * @return the TSB if successful, null otherwise
 */
static os_DvrTimeShiftBuffer* lockTSBByBuffering(os_DvrTimeShift* buffering)
{
    uint32_t i;
    os_DvrTimeShiftBuffer* tsb = NULL;

    mpeos_mutexAcquire(gTsbMutex);
    for (i = 0; i < MAX_TSBS; i++)
    {
        tsb = gTimeShiftBuffers[i];
        if (tsb != NULL && tsb->buffering == buffering)
        {
            mpeos_mutexAcquire(tsb->mutex);
            break;
        }

    }
    mpeos_mutexRelease(gTsbMutex);

    return i != MAX_TSBS ? tsb : NULL;
}

/*
 * In the given TSB is valid, safely acquire its mutex
 *
 * @return the true if successful, false otherwise
 */
static mpe_Bool lockTSB(os_DvrTimeShiftBuffer* buffer)
{
    uint32_t i;
    os_DvrTimeShiftBuffer* tsb = NULL;

    mpeos_mutexAcquire(gTsbMutex);
    for (i = 0; i < MAX_TSBS; i++)
    {
        tsb = gTimeShiftBuffers[i];
        if (tsb == buffer)
        {
            mpeos_mutexAcquire(tsb->mutex);
            break;
        }
    }
    mpeos_mutexRelease(gTsbMutex);

    return i != MAX_TSBS;
}

/*
 * Safely acquire the mutex of the TSB associated with the given
 * playback session
 *
 * @return the TSB if successful, null otherwise
 */
static os_DvrTimeShiftBuffer* lockTSBByPlayback(os_DvrTsbPlayback* pb)
{
    uint32_t i;
    os_DvrTimeShiftBuffer* tsb = NULL;

    mpeos_mutexAcquire(gTsbMutex);
    for (i = 0; i < MAX_TSBS; i++)
    {
        tsb = gTimeShiftBuffers[i];
        if (tsb != NULL && tsb->playback == pb)
        {
            mpeos_mutexAcquire(tsb->mutex);
            break;
        }

    }
    mpeos_mutexRelease(gTsbMutex);

    return i != MAX_TSBS ? tsb : NULL;
}

/*
 * Safely acquire the mutex of the TSB associated with a given
 * playback session using the given display device
 *
 * @return the TSB if successful, null otherwise
 */
static os_DvrTimeShiftBuffer* lockTSBByDisplay(ri_video_device_t* device)
{
    uint32_t i;
    os_DvrTimeShiftBuffer* tsb = NULL;

    mpeos_mutexAcquire(gTsbMutex);
    for (i = 0; i < MAX_TSBS; i++)
    {
        tsb = gTimeShiftBuffers[i];
        if (tsb != NULL && tsb->playback != NULL && tsb->playback->videoDevice
                == device)
        {
            mpeos_mutexAcquire(tsb->mutex);
            break;
        }
    }
    mpeos_mutexRelease(gTsbMutex);

    return i != MAX_TSBS ? tsb : NULL;
}

/*
 * Safely acquire the mutex of the given active DVR
 * recording playback session (non-TSB)
 *
 * @return true if successful, false otherwise
 */
static mpe_Bool lockPlayback(os_DvrPlayback* pb)
{
    uint32_t i;
    os_DvrPlayback* playback = NULL;

    mpeos_mutexAcquire(gPlayMutex);
    for (i = 0; i < MAX_PLAYBACK; i++)
    {
        playback = gActivePlay[i];
        if (playback != NULL && playback == pb)
        {
            mpeos_mutexAcquire(playback->mutex);
            break;
        }
    }
    mpeos_mutexRelease(gPlayMutex);

    return i != MAX_PLAYBACK;
}

/*
 * Safely acquire the mutex of the active DVR recording
 * playback session (non-TSB) using the given video device
 *
 * @return the playback if successful, null otherwise
 */
static os_DvrPlayback* lockPlaybackByDisplay(ri_video_device_t* device)
{
    uint32_t i;
    os_DvrPlayback* playback = NULL;

    mpeos_mutexAcquire(gPlayMutex);
    for (i = 0; i < MAX_PLAYBACK; i++)
    {
        playback = gActivePlay[i];
        if (playback != NULL && playback->videoDevice == device)
        {
            mpeos_mutexAcquire(playback->mutex);
            break;
        }
    }
    mpeos_mutexRelease(gPlayMutex);

    return i != MAX_PLAYBACK ? playback : NULL;
}

/**
 * Called by the DVR event handlers to update disk usage.  If this is a TSB update,
 * only the storage device is passed in (volume is NULL).  If this is a conversion
 * update, both the media volume and its associated device is passed in.
 *
 * @param volume for conversion updates, indicates the media volume that is
 *        being updated
 * @param device for both TSB and conversion updates, indicates the storage device
 *        that is being updated
 * @param diskUsageDelta the change in disk usage (in bytes)
 * @return the "disk full" status.  Returns true if the device or volume is full,
 *         false otherwise
 */
static mpe_Bool updateDiskUsage(os_MediaVolumeInfo* volume,
        os_StorageDeviceInfo* device, int64_t diskUsageDelta)
{
    mpe_Bool diskFull = FALSE;

    mpeos_mutexAcquire(device->mutex);

    if (volume != NULL)
    {
        MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_DVR,
                "<<DVR>> %s - Updating volume usage -- Volume = %p, device free media size = %"PRIu64", current size = %"PRIu64", delta = %"PRId64"\n",
                __FUNCTION__, volume, volume->device->freeMediaSize, volume->usedSize, diskUsageDelta);

        // Update storage volume
        mpeos_mutexAcquire(volume->mutex);

        // If this media volume has a reserved size, we will report "disk full" when
        // there is not enough free space for one more chunk of the given delta size
        if (volume->reservedSize != 0 &&
            diskUsageDelta > volume->reservedSize - volume->usedSize)
            diskFull = TRUE;
        else if (diskUsageDelta > 0)
            volume->usedSize += diskUsageDelta;
        else
            volume->usedSize -= diskUsageDelta;

        mpeos_mutexRelease(volume->mutex);

        // Check free space alarms
        sendVolumeAlarms(volume);
    }

    // If the volume does not have a reserved size, we must update the device as well
    // Additionally, if we did not set a volume, this was a TSB update and we need to
    // update the device associated with the TSB
    if (volume == NULL || volume->reservedSize == 0)
    {
        MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_DVR,
                "<<DVR>> %s - Updating device usage -- Device = %p, free media size = %"PRIu64", delta = %"PRId64"\n",
                __FUNCTION__, device, device->freeMediaSize, diskUsageDelta);

        // Report "disk full" if there is not enough free space for one more chunk
        // of the given delta size.
        if (diskUsageDelta > device->freeMediaSize)
            diskFull = TRUE;
        else if (diskUsageDelta > 0)
            device->freeMediaSize -= diskUsageDelta;
        else
            device->freeMediaSize += diskUsageDelta;
    }

    mpeos_mutexRelease(device->mutex);

    return diskFull;
}

// These functions are required because we can't call back into the platform on
// its own TSB callback thread.  So we have to spawn a new thread and call these
// functions
static void stop_convert(void* tsb)
{
    (void) mpeos_dvrTsbConvertStop((mpe_DvrConversion) tsb, TRUE);
}

/**
 * Event handler for active non-TSB playback sessions
 */
static void playback_event_cb(ri_dvr_event event, void* event_data,
        void* cb_data)
{
    os_DvrPlayback* pb = (os_DvrPlayback*) cb_data;

    // Make sure we think this playback is valid
    if (!lockPlayback(pb))
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - No active playback associated with this event! playback = %p\n",
                __FUNCTION__, pb);
        return;
    }

    switch (event)
    {
    case RI_DVR_EVENT_PLAYBACK_STARTED:
        // Notify the queue that we're presenting and set rate to 1.0
        notifyDvrQueueWithArg(pb->evQueue, pb->act, MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_2D_SUCCESS);

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - PLAYBACK_STARTED event received\n", __FUNCTION__);
        break;

    case RI_DVR_EVENT_PLAYBACK_STATUS:
    {
        mpe_Error err;
        ri_playback_status_t* status = (ri_playback_status_t*) event_data;
        os_RecFileInfo info;

        // Update our current playback position and rate
        if (MPE_SUCCESS != (err = getRecFileInfo(pb->datafile, &info)))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                      "<<DVR>> %s(%p) - getRecFileInfo error %d\n",
                      __FUNCTION__, pb, err);
            break;
        }
        info.playbackTime = status->position;

        if (MPE_SUCCESS != (err = setRecFileInfo(pb->datafile, &info)))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                      "<<DVR>> %s(%p) - setRecFileInfo error %d\n",
                      __FUNCTION__, pb, err);
            break;
        }
        pb->trickMode = status->rate;

        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_DVR,
                "<<DVR>> %s - REC_PLAYBACK_STATUS event received! playback = %p, playback time = %"PRIu64", rate = %f\n",
                __FUNCTION__, pb, info.playbackTime, pb->trickMode);
        break;
    }

    case RI_DVR_EVENT_PLAYBACK_STOPPED:
    {
        uint32_t i;

        // Remove this playback from our global list
        mpeos_mutexAcquire(gPlayMutex);
        for (i = 0; i < MAX_PLAYBACK; i++)
        {
            if (gActivePlay[i] == pb)
                gActivePlay[i] = NULL;
        }
        mpeos_mutexRelease(gPlayMutex);

        // Send the termination event for this DVR session
        notifyDvrQueue(pb->evQueue, pb->act, MPE_DVR_EVT_SESSION_CLOSED);

        // Free the playback session structure
        mpeos_mutexRelease(pb->mutex);
        mpeos_mutexDelete(pb->mutex);
        mpe_memFree(pb);

        return; // Return here because we have destroyed the mutex
        // that gets released at the end of this function
    }

    case RI_DVR_EVENT_END_OF_FILE:
        // Notify the queue of end-of-file event and set rate to 0.0
        notifyDvrQueue(pb->evQueue, pb->act, MPE_DVR_EVT_END_OF_FILE);

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - RECORDING_END_OF_FILE event received\n",
                __FUNCTION__);
        break;

    case RI_DVR_EVENT_START_OF_FILE:

        // Notify the queue of start-of-file event and set rate to 0.0
        notifyDvrQueue(pb->evQueue, pb->act, MPE_DVR_EVT_START_OF_FILE);

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - RECORDING_START_OF_FILE event received\n",
                __FUNCTION__);

        break;

    default:
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - unknown event (%d) received!\n", __FUNCTION__,
                event);
        break;
    }

    mpeos_mutexRelease(pb->mutex);
}

/**
 * Event handler for active TSB buffering or playback sessions
 */
static void tsb_event_cb(ri_dvr_event event, void* event_data, void* cb_data)
{
    // Validate the pipeline index
    os_DvrTimeShiftBuffer* tsb = (os_DvrTimeShiftBuffer*) cb_data;

    // Make sure we think there is an active tsb associated with this pipeline
    // If the client has deleted the buffer associated with this time-shift session, it
    // is possible that it no longer exists.  Just ignore the rest of the events.
    if (!lockTSB(tsb))
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - No active TSB! (%p).  TSB may have been deleted.\n",
                __FUNCTION__, tsb);
        return;
    }

    switch (event)
    {
    case RI_DVR_EVENT_TSB_START:

        if (tsb->buffering == NULL)
        {
            MPEOS_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - TSB_BUFFERING_STATUS -- No active TSB buffering session! tsb =  %p\n",
                    __FUNCTION__, tsb);
            break;
        }

        // Update the actual TSB start time
        tsb->sysStartTime = *((uint64_t*) event_data);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - TSB_START event received! tsb = %p, start time = %"PRIu64"\n",
                __FUNCTION__, tsb, tsb->sysStartTime);
        mpeos_condSet(tsb->buffering->startCond);
        notifyDvrQueue(tsb->buffering->evQueue, tsb->buffering->act, MPE_DVR_EVT_SESSION_RECORDING);
        break;

    case RI_DVR_EVENT_TSB_STATUS:
    {
        ri_tsb_status_t* status = (ri_tsb_status_t*) event_data;
        os_DvrTsbPlayback* pb = tsb->playback;

        if (tsb->buffering == NULL)
        {
            MPEOS_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - TSB_BUFFERING_STATUS -- No active TSB buffering session! tsb =  %p\n",
                    __FUNCTION__, tsb);
            break;
        }

        // Update the start, end, and current disk size for this TSB
        tsb->startTime = status->start_time - tsb->sysStartTime;
        tsb->endTime = status->end_time - tsb->sysStartTime;

        if (NULL != pb)
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s"
                      " max:%"PRIu64" current:%"PRIu64" start:%"PRIu64
                      " end:%"PRIu64"\n", __FUNCTION__,
                      tsb->duration * NANO_SECONDS, pb->currentTime,
                      tsb->startTime, tsb->endTime);

            if (pb->currentTime < tsb->startTime)
            {
                if (FALSE == tsb->isFull)
                {
                    // Notify the event queue and set the trick mode to paused
                    notifyDvrQueue(pb->evQueue, pb->act, MPE_DVR_EVT_START_OF_FILE);
                    tsb->isFull = TRUE;
                    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "<<DVR>> tsb->isFull == TRUE\n");
                }
            }
            else if (TRUE == tsb->isFull)
            {
                tsb->isFull = FALSE;
                MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "<<DVR>> tsb->isFull == FALSE\n");
            }
        }

        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_DVR,
                "<<DVR>> %s - TSB_STATUS event received! tsb = %p, start time = %"PRIu64", end time = %"PRIu64"\n",
                __FUNCTION__, tsb, tsb->startTime, tsb->endTime);

        break;
    }

    case RI_DVR_EVENT_TSB_STOPPED:

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s (%p) - TSB_STOPPED event received!\n",
                __FUNCTION__, tsb);

        if (tsb->buffering == NULL)
        {
            MPEOS_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - TSB_BUFFERING_STOPPED -- No active TSB buffering session! tsb =  %p\n",
                    __FUNCTION__, tsb);
            break;
        }

        // Notify the event queue that the TSB has session is finished
        notifyDvrQueue(tsb->buffering->evQueue, tsb->buffering->act,
                MPE_DVR_EVT_SESSION_CLOSED);

        // Just release our active TSB structure
        mpeos_condDelete(tsb->buffering->startCond);
        mpe_memFree(tsb->buffering);
        tsb->buffering = NULL;
        break;

    case RI_DVR_EVENT_PLAYBACK_STARTED:
    {
        os_DvrTsbPlayback* pb = tsb->playback;

        // Make sure we think there is an active playback
        if (pb == NULL)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - TSB_PLAYBACK_STARTED -- No active TSB playback! tsb =  %p\n",
                    __FUNCTION__, tsb);
            break;
        }

        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_DVR,
                "<<DVR>> %s (%p) - TSB_PLAYBACK_STARTED event received! playback = %p\n",
                __FUNCTION__, tsb, pb);

        // Notify the queue that we're presenting and set rate to 1.0
        notifyDvrQueueWithArg(pb->evQueue, pb->act, MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_2D_SUCCESS);
        break;
    }

    case RI_DVR_EVENT_PLAYBACK_STATUS:
    {
        ri_playback_status_t* status = (ri_playback_status_t*) event_data;
        os_DvrTsbPlayback* pb = tsb->playback;

        // Make sure we think there is an active playback
        if (pb == NULL)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - TSB_PLAYBACK_STATUS -- No active TSB playback! tsb =  %p\n",
                    __FUNCTION__, tsb);
            break;
        }

        // Update our current playback position
        pb->currentTime = status->position;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s (%p) - TSB_PLAYBACK_STATUS event received! playback = %p, current time = %"PRIu64"\n",
                __FUNCTION__, tsb, pb, pb->currentTime);

        break;
    }

    case RI_DVR_EVENT_PLAYBACK_STOPPED:
    {
        os_DvrTsbPlayback* pb = tsb->playback;

        // Make sure we think there is an active playback
        if (pb == NULL)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - TSB_PLAYBACK_STOPPED -- No active TSB playback! tsb =  %p\n",
                    __FUNCTION__, tsb);
            break;
        }

        // Send the termination event for this DVR session
        notifyDvrQueue(pb->evQueue, pb->act, MPE_DVR_EVT_SESSION_CLOSED);

        // Close our open recording info file and delete the playback record
        mpe_memFree(pb);
        tsb->playback = NULL;
        break;
    }

    case RI_DVR_EVENT_END_OF_FILE:
    {
        os_DvrTsbPlayback* pb = tsb->playback;

        // Make sure we think there is an active TSB playback
        if (pb == NULL)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - TSB_PLAYBACK_END_OF_FILE -- No active TSB playback! tsb =  %p\n",
                    __FUNCTION__, tsb);
            break;
        }

        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_DVR,
                "<<DVR>> %s (%p) - TSB_PLAYBACK_END_OF_FILE event received! playback = %p\n",
                __FUNCTION__, tsb, pb);

        // Notify the event queue and set the trick mode to paused
        notifyDvrQueue(pb->evQueue, pb->act, MPE_DVR_EVT_END_OF_FILE);
        break;
    }

    case RI_DVR_EVENT_START_OF_FILE:
    {
        os_DvrTsbPlayback* pb = tsb->playback;

        // Make sure we think there is an active TSB playback
        if (pb == NULL)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - TSB_PLAYBACK_START_OF_FILE -- No active TSB playback! tsb =  %p\n",
                    __FUNCTION__, tsb);
            break;
        }

        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_DVR,
                "<<DVR>> %s (%p) - TSB_PLAYBACK_START_OF_FILE event received! playback = %p\n",
                __FUNCTION__, tsb, pb);

        // Notify the event queue and set the trick mode to paused
        notifyDvrQueue(pb->evQueue, pb->act, MPE_DVR_EVT_START_OF_FILE);
        break;
    }

    default:
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - unknown event (%d) received!\n", __FUNCTION__,
                event);
        break;
    }

    mpeos_mutexRelease(tsb->mutex);
}

/**
 * Event handler for active TSB conversions
 */
void convert_event_cb(ri_dvr_event event, void* event_data, void* cb_data)
{
    // Validate the pipeline index
    os_DvrTimeShiftBuffer* tsb = (os_DvrTimeShiftBuffer*) cb_data;

    // Make sure we think there is an active tsb associated with this pipeline
    // If the client has deleted the buffer associated with this time-shift session, it
    // is possible that it no longer exists.  Just ignore the rest of the events.
    if (!lockTSB(tsb))
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - No active TSB! (%p).  TSB may have been deleted.\n",
                __FUNCTION__, tsb);
        return;
    }

    switch (event)
    {
    case RI_DVR_EVENT_TSB_CONVERSION_STATUS:
    {
        mpe_Error err;
        ri_tsb_status_t* status = (ri_tsb_status_t*) event_data;
        os_RecFileInfo info;
        os_DvrRecording *rec;

        // Make sure we think there is an active conversion
        if (tsb->recording == NULL)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s (%p) - CONVERSION_STATUS -- No active TSB conversion!\n",
                    __FUNCTION__, tsb);
            break;
        }
        rec = tsb->recording;

        // Update our persistent datafile for this recording
        if (MPE_SUCCESS != (err = getRecFileInfo(rec->datafile, &info)))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                      "<<DVR>> %s(%p) - getRecFileInfo error %d\n",
                      __FUNCTION__, tsb, err);
            break;
        }

        // Determine the amount by which our disk usage has changed and check for
        // "full" status
        if (updateDiskUsage(rec->volume, rec->volume->device, status->size
                - info.size))
        {
            mpe_ThreadId thread;
            MPEOS_LOG(
                    MPE_LOG_WARN,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - Device is full! Conversion session terminating! sess = %p, volume = %p, device = %p\n",
                    __FUNCTION__, rec, rec->volume, rec->volume->device);

            // We are full, notify the queue and tear down this session
            notifyDvrQueue(rec->evQueue, rec->act, MPE_DVR_EVT_OUT_OF_SPACE);
            (void) mpeos_threadCreate(stop_convert, (void*) tsb,
                    MPE_THREAD_PRIOR_DFLT, MPE_THREAD_STACK_SIZE, &thread,
                    "Stop Convert (Disk Full)");
        }
        else
        {
            /* Update the file metadata only if the MSV can accommodate the incoming chunk of TSB  */
            info.currentLength = status->end_time - (tsb->sysStartTime
                    + rec->convertStartTime);
            info.size = status->size;

            MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_DVR,
                    "<<DVR>> %s (%p) - TSB_CONVERSION_STATUS event received! status->end_time = %"PRIu64", sysStartTime = %"PRIu64", convertStartTime = %"PRIu64"\n",
                    __FUNCTION__, tsb, status->end_time, tsb->sysStartTime, rec->convertStartTime);

            if (MPE_SUCCESS != (err = setRecFileInfo(rec->datafile, &info)))
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                          "<<DVR>> %s(%p) - setRecFileInfo error %d\n",
                          __FUNCTION__, tsb, err);
                break;
            }
        }

        MPEOS_LOG(MPE_LOG_TRACE1, MPE_MOD_DVR,
                "<<DVR>> %s (%p) - TSB_CONVERSION_STATUS event received! currentLength = %"PRIu64", size = %"PRIu64"\n",
                __FUNCTION__, tsb, info.currentLength, info.size);

        break;
    }

    case RI_DVR_EVENT_TSB_CONVERSION_COMPLETE:
    {
        mpe_Error err;
        ri_conversion_results_t* results =
                (ri_conversion_results_t*) event_data;
        os_RecFileInfo info;
        os_DvrRecording *rec;

        // Make sure we think there is an active conversion
        if (tsb->recording == NULL)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s (%p) - CONVERSION_COMPLETE -- No active TSB conversion!\n",
                    __FUNCTION__, tsb);
            break;
        }
        rec = tsb->recording;

        // Finalize our persistent datafile for this recording
        if (MPE_SUCCESS != (err = getRecFileInfo(rec->datafile, &info)))
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                      "<<DVR>> %s(%p) - getRecFileInfo error %d\n",
                      __FUNCTION__, tsb, err);
            break;
        }

        // Update our storage usage and check for disk full.
        if(!updateDiskUsage(rec->volume, rec->volume->device, results->size - info.size))
        {
            /* Update the file metadata only if the MSV is not full  */
            info.currentLength = results->duration;
            info.size = results->size;

            if (MPE_SUCCESS != (err = setRecFileInfo(rec->datafile, &info)))
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                          "<<DVR>> %s(%p) - setRecFileInfo error %d\n",
                          __FUNCTION__, rec, err);
                break;
            }
        }

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s (%p) - TSB_CONVERSION_COMPLETE event received! recording = %p, length secs = %d, size = %"PRIu64"\n",
                __FUNCTION__, tsb, tsb->recording, info.currentLength / NANO_SECONDS, info.size);

        // Notify the event queue that the conversion is complete
        notifyDvrQueue(rec->evQueue, rec->act, MPE_DVR_EVT_CONVERSION_STOP);

        // Free our recording information
        tsb->recording = NULL;
        mpe_memFree(rec);
        break;
    }

    default:
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - unknown event (%d) received!\n", __FUNCTION__,
                event);
        break;
    }

    mpeos_mutexRelease(tsb->mutex);
}

static mpe_Error dvrPlaybackInit3DConfig ()
{
    ri_display_t* display = NULL;
    int32_t formatTypeTemp;
    int32_t payloadTypeTemp;
    int32_t scanModeTemp;

    ri_pipeline_manager_t* pMgr = ri_get_pipeline_manager();
    if (NULL != pMgr)
    {
        display = pMgr->get_display(pMgr);
        if (NULL != display)
        {
            int nReturnCode = display->get_threedtv_info(display, &formatTypeTemp, &payloadTypeTemp,
                &g_payloadSz, NULL, &scanModeTemp);
            if (nReturnCode != 0)
            {
                g_payload = (uint8_t*) malloc (g_payloadSz);

                nReturnCode = display->get_threedtv_info(display, &formatTypeTemp, &payloadTypeTemp,
                    &g_payloadSz, g_payload, &scanModeTemp);
                if (nReturnCode == 0)
                {
                    g_payloadType = payloadTypeTemp;
                    g_stereoscopicMode = formatTypeTemp;
                    g_videoScanMode = scanModeTemp;
                }
                else
                {
                    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                            "DVR: could not retrieve 3DTV settings -- 1\n");
                    return MPE_EINVAL;
                }
            }
            else
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                        "DVR: could not retrieve 3DTV settings -- 2\n");
                return MPE_EINVAL;
            }
        }
        else
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                    "DVR: could not retrieve 3DTV settings -- 3\n");
            return MPE_EINVAL;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "DVR: could not retrieve pipeline manager -- 4\n");
        return MPE_EINVAL;
    }
        
    return (mpe_Error) MPE_SUCCESS;
}
/********************************************************************************/
//                                                                              //
//                      MPE OS DVR public functions                             //
//                                                                              //
/********************************************************************************/

/*
 * Description:
 *
 * Native DVR manager entry point.
 * Called by MPE DVR manager
 *
 * Returns:
 * MPE_DVR_ERR_NOERR
 */
mpe_Error mpeos_dvrInit(void)
{
    mpe_Error err;
    uint32_t i;
    uint32_t numPipelines;

    dvrPlaybackInit3DConfig();

    // For now, each live pipeline in the platform is only capable of a single TSB,
    // recording, or playback.  So just get the number of live pipelines and that will
    // determine how many active playbacks and recordings we can support.
    (void) ri_get_pipeline_manager()->get_live_pipelines(
            ri_get_pipeline_manager(), &numPipelines);

    ri_test_RegisterMenu(&MpeosRecPlaybackMenuItem);
    ri_test_RegisterMenu(&MpeosTSBPlaybackMenuItem);
    ri_test_RegisterMenu(&MpeosTSBBufferingMenuItem);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Enabling DVR system\n",
            __FUNCTION__);

    gVolumeMgrData.queueRegistered = FALSE;

    // Allocate and initialize global TSB_RECORDING structure(s)
    (void) mpe_memAlloc(sizeof(os_DvrTimeShiftBuffer*) * MAX_TSBS,
            (void**) &gTimeShiftBuffers);
    for (i = 0; i < MAX_TSBS; i++)
        gTimeShiftBuffers[i] = NULL;
    if ((err = mpeos_mutexNew(&gTsbMutex)) != MPE_SUCCESS)
        return err;

    // Setup a file IO mutex to prevent file read/write collisions
    if ((err = mpeos_mutexNew(&gRecFileIOMutex)) != MPE_SUCCESS)
        return err;

    // Allocate and initialize global PLAYBACK structure(s)
    (void) mpe_memAlloc(sizeof(os_DvrPlayback*) * MAX_PLAYBACK,
            (void**) &gActivePlay);
    for (i = 0; i < MAX_PLAYBACK; i++)
        gActivePlay[i] = NULL;
    if ((err = mpeos_mutexNew(&gPlayMutex)) != MPE_SUCCESS)
        return err;

    initVolumes();

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "\n<<DVR>> Init Success!\n");

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 * This function returns all the supported play scales
 *
 * Parameters:
 *
 * storage          a storage ID
 * playScales       pointer to playscales array (floating point values)
 * num              number of entries in the playscales array
 *
 * Returns:
 * MPE_DVR_ERR_NOERR
 * MPE_DVR_ERR_INVALID_PARAM
 * MPE_DVR_ERR_OS_FAILURE
 * MPE_DVR_ERR_DEVICE_ERR
 */
mpe_Error mpeos_dvrGetPlayScales(mpe_StorageHandle storage, float **playScales,
        uint32_t *num)
{
    MPE_UNUSED_PARAM(storage);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s \n", __FUNCTION__);

    // Sanity check
    if (playScales == NULL || num == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - returning MPE_DVR_ERR_INVALID_PARAM...\n",
                __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    /* We use a static list with what seems to be a "reasonable" set of play scales - this
     can be changed as needed.  */
    *playScales = playScaleList;
    *num = playScaleListSize;

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 * This function returns the amount of time it takes for the slowest device
 * needed to make a recording to resume from a low power state.
 *
 * Returns:
 * uint32_t
 */
uint32_t mpeos_dvrGetLowPowerResumeTime(void)
{
    return 0;
}

/*
 * Description:
 *
 * This function causes all devices associated with making a recording to
 * be brought out of a low power state.
 *
 * Returns:
 * MPE_DVR_ERR_NOERR
 * MPE_DVR_ERR_OS_FAILURE
 */
mpe_Error mpeos_dvrResumeFromLowPower(void)
{
    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 * This function return a list of existing recording names from a given storage device
 *
 * Parameters:
 *
 * storage          a storage ID
 * count            pointer to a number of recording names in the list
 * recording names  array of strings representing recording names.
 *
 * Returns:
 * MPE_DVR_ERR_NOERR
 * MPE_DVR_ERR_INVALID_PARAM
 * MPE_DVR_ERR_OS_FAILURE
 * MPE_DVR_ERR_DEVICE_ERR
 */
mpe_Error mpeos_dvrGetRecordingList(mpe_StorageHandle device, uint32_t* count,
        mpe_DvrString_t **recordingNames)
{
    mpe_DirEntry ent;
    mpe_Dir dirHand;
    os_MediaVolumeInfo *volume;
    mpe_Error err;
    int numRecs = 0;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s\n", __FUNCTION__);

    // TODO: <STMGR-COMP> Remove this check?
    if (device == NULL)
    {
        device = storageGetDefaultDevice();
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_DVR,
                "<<DVR>> %s - No storage device specified!  Using default (%s).\n",
                __FUNCTION__, device->name);
    }

    mpeos_mutexAcquire(device->mutex);

    *count = 0;
    volume = device->volumes;
    while (volume != NULL)
    {
        mpeos_mutexAcquire(volume->mutex);

        // Open the media data directory for this volume
        if (mpe_dirOpen(volume->dataPath, &dirHand) == MPE_SUCCESS)
        {
            while (mpe_dirRead(dirHand, &ent) == MPE_SUCCESS)
            {
                // Skip directories (should be none)
                if (!ent.isDir)
                    numRecs++;
            }

            mpe_dirClose(dirHand);
        }

        mpeos_mutexRelease(volume->mutex);
        volume = volume->next;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - Found %d recording on device (%s).\n", __FUNCTION__,
            numRecs, device->name);

    // No recordings
    if (numRecs == 0)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - No recordings\n",
                __FUNCTION__);
        *recordingNames = NULL;
        *count = 0;
        mpeos_mutexRelease(device->mutex);
        return MPE_SUCCESS;
    }

    /* Allocate space now that we know how many entries there are */
    if ((err = mpe_memAlloc(sizeof(mpe_DvrString_t) * numRecs,
            (void **) &gRecNames)) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - error allocating memory!\n", __FUNCTION__);
        mpeos_mutexRelease(device->mutex);
        return err;
    }

    /* Now, let's loop through all over again to copy data into the structure */
    numRecs = 0;
    volume = device->volumes;
    while (volume != NULL)
    {
        mpeos_mutexAcquire(volume->mutex);

        // Open the media data directory for this volume
        if (mpe_dirOpen(volume->dataPath, &dirHand) == MPE_SUCCESS)
        {
            while (mpe_dirRead(dirHand, &ent) == MPE_SUCCESS)
            {
                // Skip directories (should be none)
                if (!ent.isDir)
                {
                    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                            "<<DVR>> %s - Found recording (%s).\n",
                            __FUNCTION__, ent.name);
                    snprintf(gRecNames[numRecs++], MPE_DVR_MAX_NAME_SIZE,
                            "%s/%s", STORAGE_TRIM_ROOT(volume->rootPath),
                            ent.name);
                }
            }

            mpe_dirClose(dirHand);
        }

        mpeos_mutexRelease(volume->mutex);
        volume = volume->next;
    }

    /* Return a pointer to our structure and the count */
    *recordingNames = gRecNames;
    *count = numRecs;

    mpeos_mutexRelease(device->mutex);

    return MPE_SUCCESS;
}

/*
 * Description:
 *
 * This function releases the memory allocated for the recording list
 * returned by mpeos_dvrGetRecordingList( ).
 *
 * Parameters:
 *
 * Returns:
 * MPE_DVR_ERR_NOERR
 */
mpe_Error mpeos_dvrFreeRecordingList(void)
{
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s\n", __FUNCTION__);

    /* If there is a currently allocated list, then free it */
    if (gRecNames)
        mpe_memFree(gRecNames);

    /* Set the list pointer to NULL */
    gRecNames = NULL;

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 * This function returns generic information, such as recording information,
 * available space, etc...
 *
 * Parameters:
 *
 * param        information parameter.
 * input        input argument associated with the parameter.
 * output       returned value for the given parameter.
 *
 *
 * Possible mpe_DvrInfoParam are:
 *
 * MPE_DVR_STORAGE_SPACE: To get available space on a given storage device
 * - Input: a storage device ID. If NULL, the information will be retrieved from
 * the default internal storage device.
 * - Output: the space available (in bytes) on the given storage device.
 *
 * Returns:
 * MPE_DVR_ERR_NOERR
 * MPE_DVR_ERR_INVALID_PARAM
 * MPE_DVR_ERR_OS_FAILURE
 * MPE_DVR_ERR_DEVICE_ERR
 */
mpe_Error mpeos_dvrGet(mpe_DvrInfoParam param, void *input, void *output)
{
    /* Sanity Check */
    if (output == NULL)
    {
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    switch (param)
    {
    case MPE_DVR_MAX_BITRATE:
    {
        uint32_t maxBitRateInBitsPerSec = (uint32_t)(gMaxBitRate * 1000);
        memcpy(output, (void*) &maxBitRateInBitsPerSec, sizeof(uint32_t));
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s MPE_DVR_MAX_BITRATE = %d\n", __FUNCTION__,
                maxBitRateInBitsPerSec);
        break;
    }

    case MPE_DVR_MAX_RECORDING_BANDWIDTH:
    {
        // gMaxBitRate defined as 19,500 KiloBits/sec for HD capable box
        // and 6000 KiloBits/sec for SD box
        uint32_t maxBitRateInBitsPerSec = (uint32_t)(gMaxBitRate * 1000);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s MPE_DVR_MAX_RECORDING_BANDWIDTH = %d\n",
                __FUNCTION__, gMaxBitRate);
        *(uint32_t*) output = maxBitRateInBitsPerSec;
        break;
    }

    case MPE_DVR_MAX_PLAYBACK_BANDWIDTH:
    {
        // gMaxBitRate defined as 19,500 KiloBits/sec for HD capable box
        // and 6000 KiloBits/sec for SD box
        uint32_t maxBitRateInBitsPerSec = (uint32_t)(gMaxBitRate * 1000);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s MPE_DVR_MAX_PLAYBACK_BANDWIDTH = %d\n",
                __FUNCTION__, gMaxBitRate);
        *(uint32_t*) output = maxBitRateInBitsPerSec;
        break;
    }

    case MPE_DVR_SIMULTANEOUS_PLAY_RECORD:
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s MPE_DVR_SIMULTANEOUS_PLAY_RECORD returning TRUE\n",
                __FUNCTION__);
        *(uint32_t*) output = 1;
        break;
    }

    case MPE_DVR_SUPPORTS_CROSS_MSV_TSB_CONVERT:
    {
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_DVR,
                "<<DVR>> %s MPE_DVR_SUPPORTS_CROSS_MSV_TSB_CONVERT returning TRUE\n",
                __FUNCTION__);
        *(uint32_t*) output = 1;
        break;
    }

    case MPE_DVR_STORAGE_MEDIAFS_CAPACITY:
    {
        uint64_t capacityInBytes = 0;
        mpe_StorageHandle *device = input;

        if (!input)
            return MPE_DVR_ERR_INVALID_PARAM;

        (void) mpeos_storageGetInfo(*device,
                MPE_STORAGE_MEDIAFS_PARTITION_SIZE, &capacityInBytes); // Return code?
        *(uint64_t*) output = capacityInBytes;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s MPE_DVR_STORAGE_MEDIAFS_CAPACITY = %"PRIu64"\n",
                __FUNCTION__, capacityInBytes);
        break;
    }

    case MPE_DVR_STORAGE_MEDIAFS_ALLOCATABLE_SPACE:
    {
        mpe_StorageHandle *device = input;
        uint64_t allocatableSpace = 0;

        if (!input)
        {
            return MPE_DVR_ERR_INVALID_PARAM;
        }

        allocatableSpace = (*device)->freeMediaSize;
        *(uint64_t*)output = allocatableSpace;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s MPE_DVR_STORAGE_MEDIAFS_ALLOCATABLE_SPACE = %"PRIu64"\n",
                __FUNCTION__, allocatableSpace);
        break;
    }

    case MPE_DVR_STORAGE_MEDIAFS_FREE_SPACE:
    {
        uint64_t freeSpace = 0;
        mpe_StorageHandle handle = input;

        if (!input)
        {
            return MPE_DVR_ERR_INVALID_PARAM;
        }

        (void)mpeos_storageGetInfo(handle, MPE_STORAGE_MEDIAFS_FREE_SPACE, &freeSpace); // Return code?
        *(uint64_t*)output = freeSpace;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s MPE_DVR_STORAGE_MEDIAFS_FREE_SPACE = %"PRIu64"\n",
                __FUNCTION__, freeSpace);
        break;
    }

    case MPE_DVR_TSB_MIN_BUF_SIZE:
    {
        uint32_t minBufferSize = OS_DVR_MIN_TSB_DURATION;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s MPE_DVR_TSB_MIN_BUF_SIZE = %d seconds\n",
                __FUNCTION__, minBufferSize);
        memcpy(output, (void*)&minBufferSize, sizeof(uint32_t));
        break;
    }

    case MPE_DVR_MEDIA_START_TIME:
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s MPE_DVR_MEDIA_START_TIME always 0 on this platform\n",
                __FUNCTION__);
        *(uint32_t*)output = 0;
       break;
    }

    default:
    {
        /* Unknown parameter */
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "<<DVR>> %s Invalid attribute!\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }
    }

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *  This function removes the given recorded content from the disk.
 *  An active recording cannot be deleted until it becomes inactive (in MPE_DVR_STOPPED state).
 *
 * Parameters:
 *  recording   a recording unique name identifier.
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR           if successful
 *  MPE_DVR_ERR_INVALID_PARAM   if a parameter is invalid
 *  MPE_DVR_ERR_OS_FAILURE      if an OS error occurs
 *  MPE_DVR_ERR_NOT_ALLOWED     if the operation is not allowed
 */
mpe_Error mpeos_dvrRecordingDelete(char *recordingName)
{
    mpe_Error err;
    char datafile[MPE_FS_MAX_PATH];
    char platformPath[MPE_FS_MAX_PATH];
    char platformRecName[RI_MAX_RECORDING_NAME_LENGTH];
    os_MediaVolumeInfo *volume;
    uint64_t bytesDeleted;
    os_RecFileInfo info;
    uint32_t i;

    if (recordingName == NULL || recordingName[0] == 0
            || convertRecordingNameToDatafile(recordingName, datafile) == -1
            || convertRecordingNameToPlatform(recordingName, platformPath,
                    platformRecName) == -1)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid Parameter\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - recordingName = %s\n",
            __FUNCTION__, recordingName);

    //
    // Ensure that this recording is not currently active as a recording or playback
    //

    // Search active recordings
    for (i = 0; i < MAX_TSBS; i++)
    {
        if (gTimeShiftBuffers[i] != NULL && gTimeShiftBuffers[i]->recording
                != NULL && strcmp(gTimeShiftBuffers[i]->recording->datafile,
                datafile) == 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                    "<<DVR>> %s - Recording is actively recording!\n",
                    __FUNCTION__);
            return MPE_DVR_ERR_NOT_ALLOWED;
        }
    }

    // Search active playbacks
    for (i = 0; i < MAX_PLAYBACK; i++)
    {
        if (gActivePlay[i] != NULL
                && strcmp(gActivePlay[i]->datafile, datafile) == 0)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                    "<<DVR>> %s - Recording is actively playing back!\n",
                    __FUNCTION__);
            return MPE_DVR_ERR_NOT_ALLOWED;
        }
    }

    // Delete the recording media files
    (void) strcat(platformPath,"/");
    (void) strcat(platformPath,platformRecName);
    if (removeFiles(platformPath) != MPE_FS_ERROR_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Unable to delete recording data files -- %s\n",
                __FUNCTION__, recordingName);
        return MPE_DVR_ERR_OS_FAILURE;
    }

    // Find the volume that holds this recording
    if ((volume = findVolumeByRecordingName(recordingName)) == NULL)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - Can't find recording.  Volume may have been deleted (%s)\n",
                __FUNCTION__, recordingName);
        return MPE_DVR_ERR_NOERR;
    }

    // Get the size of this recording on disk so we can update our volume's
    // available space
    if (MPE_SUCCESS != (err = getRecFileInfo(datafile, &info)))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                  "<<DVR>> %s(%p) - getRecFileInfo error %d\n",
                  __FUNCTION__, datafile, err);
        return err;
    }

    bytesDeleted = info.size;

    mpeos_mutexAcquire(volume->mutex);

    // Update the volume's space used
    volume->usedSize -= bytesDeleted;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Volume (%s) now has %"PRIu64" bytes used\n",
            __FUNCTION__, volume->rootPath, volume->usedSize);

    // If the volume does not have a statically allocated size, then update the device
    // info as well
    if (volume->reservedSize == 0)
    {
        volume->device->freeMediaSize += bytesDeleted;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Device(%s) now has %"PRIu64" bytes free\n",
                __FUNCTION__, volume->device->name, volume->device->freeMediaSize);
    }

    // Finally delete the datafile
    (void)mpe_fileDelete(datafile);

    mpeos_mutexRelease(volume->device->mutex); // Implicitly acquired by findVolumeByRecordingName
    mpeos_mutexRelease(volume->mutex);

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 * This function retrieves information related to a recording whether it is active or not.
 *
 * Parameters:
 *
 *  recordingName   unique name identifier.
 *  param       information parameter.
 *  output      returned value for the given parameter.
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_OS_FAILURE
 *  MPE_DVR_ERR_DEVICE_ERR
 *
 *
 * Possible mpe_DvrInfoParam are:
 *
 * MPE_DVR_RECORDING_SIZE: to get the size (in bytes) of a given recording.
 * Output: the size of the record in bytes.
 * If the given recording is active, this function will return an ongoing size value.
 *
 * MPE_DVR_MEDIA_TIME: to get the current recording time.
 * This time is expressed in nanoseconds. It represents how long the recording has been ongoing.
 *
 * MPE_DVR_RECORDING_LENGTH: to get recording length.
 * Output: The recording length is expressed in seconds. It represents the final length of a recording.
 *
 * MPE_DVR_PIDCOUNT: to get the number of pids in a given record.
 * Output: The number of pids in a given record.
 *
 * MPE_DVR_PIDINFO: to get the pid information from a given record.
 * Output: an array of pid data information defined as mpe_MediaPID.
 */
mpe_Error mpeos_dvrRecordingGet(mpe_StorageHandle device, char *recordingName,
        mpe_DvrInfoParam param, void *output)
{
    char datafile[MPE_FS_MAX_PATH];
    os_RecFileInfo recInfo;

    MPE_UNUSED_PARAM(device);

    if (convertRecordingNameToDatafile(recordingName, datafile) == -1
            || getRecFileInfo(datafile, &recInfo) != MPE_DVR_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid recording name (%s)!\n", __FUNCTION__,
                recordingName);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    /* Decide which data to return */
    switch (param)
    {
    case MPE_DVR_RECORDING_SIZE:
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s (%s) MPE_DVR_RECORDING_SIZE = %"PRIu64"\n",
                __FUNCTION__, recordingName, recInfo.size);

        /* Copy the recording size in bytes to the output buffer */
        memcpy(output, (void *) &recInfo.size, sizeof(uint64_t));
        break;
    }

    case MPE_DVR_RECORDING_LENGTH_MS:
    {
        uint64_t lengthMS = recInfo.currentLength / NANO_MILLIS;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s (%s) MPE_DVR_RECORDING_LENGTH_MS = %"PRIu64"\n",
                __FUNCTION__, recordingName, lengthMS);

        /* Copy the recording length in secs to output */
        memcpy(output, (void *) &lengthMS, sizeof(uint64_t));
        break;
    }

    case MPE_DVR_MEDIA_TIME:
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s (%s) MPE_DVR_MEDIA_TIME = %"PRIu64"\n",
                __FUNCTION__, recordingName, recInfo.currentLength);

        /* Copy the recording size in ns to the output buffer */
        memcpy(output, (void *) &recInfo.currentLength, sizeof(uint64_t));
        break;
    }

    case MPE_DVR_PID_COUNT:
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s MPE_DVR_PID_COUNT = %d\n", __FUNCTION__,
                recInfo.pidCount);

        /* Copy the final count into the output buffer */
        memcpy(output, (void *) &recInfo.pidCount, sizeof(uint32_t));
        break;
    }

    case MPE_DVR_PID_INFO:
    {
        uint32_t i;
        mpe_DvrPidInfo* pids = (mpe_DvrPidInfo*) output;

        for (i = 0; i < recInfo.pidCount; i++)
        {
            pids[i].streamType = recInfo.pids[i].streamType;
            pids[i].recPid = recInfo.pids[i].recPid;
            pids[i].recEltStreamType = recInfo.pids[i].recEltStreamType;
        }
        break;
    }

    default:
        /* Some unknown parameter */
        return MPE_DVR_ERR_UNSUPPORTED;
    }

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 * This function creates and starts a new DVR playback for a given recording name.
 * A new playback handle is returned if the call is successful. The recording will start
 * playing back at the normal rate (1.0) at the given media time. The returned handle can
 * be used to control the speed, the direction and the position of the play back in the stream.
 * An event is sent to notify that the playback state is changed to MPE_DVR_PLAYING.
 *
 * Parameters:
 *
 *  recordName          a record unique string identifier.
 *  videoDevice         identifier of target video device handle to play to.
 *  pids                array of pids to be played back. If the given pids
 *                      are invalid, an MPE_DVR_ERR_INVALID_PARAM error is returned.
 *  pidCount            number of pids in the array.
 *  mediaTime           the media time is expressed in  nanoseconds and can
 *                      be any value between 0 (the beginning of the stream)
 *                      and the recording length. It represents where to start playing back
 *                      in the stream.
 *  requestedRate       initial rate
 *  actualRate          pointer to a float that will be set with the actual rate
 *  blocked             designates whether the video is initially considered blocked.
 *                        (See mpeos_dvrPlaybackBlockPresentation() below for more information.)
 *  muted               Initial mute state
 *  requestedGain       initial gain
 *  actualGain          pointer to a float that will be set with the actual gain
 *  evQueue             the event queue to post DVR recording events.
 *  act                 the completion token for async events
 *  playback            pointer to a DVR playback handle. This handle is used
 *                      to control trick play and media positions.
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_OS_FAILURE
 *  MPE_DVR_ERR_DEVICE_ERR
 */
mpe_Error mpeos_dvrRecordingPlayStart(char *recordingName,
        mpe_DispDevice videoDevice, mpe_DvrPidInfo *pids, uint32_t pidCount,
        int64_t mediaTime, float requestedRate, float *actualRate, mpe_Bool blocked, mpe_Bool muted, float requestedGain,
        float *actualGain, uint8_t cci, int64_t alarmMediaTime, mpe_EventQueue evQueue, void *act,
        mpe_DvrPlayback *playback)
{
    uint32_t playbackIdx, i;
    char datafile[MPE_FS_MAX_PATH];
    char platformPath[MPE_FS_MAX_PATH];
    char platformRecName[RI_MAX_RECORDING_NAME_LENGTH];
    os_DvrPlayback* newPlayback;
    os_DvrPlayback* pb = NULL;
    os_DvrTimeShiftBuffer* tsb = NULL;
    os_RecFileInfo info;
    ri_video_device_t* video_device;
    ri_pid_info_t* ri_pids;
    mpe_Error err;

    MPE_UNUSED_PARAM(blocked);
    MPE_UNUSED_PARAM(muted);
    MPE_UNUSED_PARAM(cci);
    MPE_UNUSED_PARAM(alarmMediaTime);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - recordingName = %s, mediaTime = %"PRId64", requestedRate = %f\n",
            __FUNCTION__, recordingName, mediaTime, requestedRate);

    // RITODO -- Fix this when mpeos_media can give me a pipeline ID based on the mpe_DispDevice
    playbackIdx = 0;

    video_device = dispGetVideoDevice(videoDevice);
    if (NULL == video_device)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Unable to find specified video device\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error;
    }

    if (recordingName == NULL || pids == NULL
            || convertRecordingNameToDatafile(recordingName, datafile) == -1
            || convertRecordingNameToPlatform(recordingName, platformPath,
                    platformRecName) == -1)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid Parameters\n", __FUNCTION__);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error;
    }

    mpeos_mutexAcquire(gPlayMutex);

    // Ensure that this video device is not engaged in any other playback
    // TODO: Should go through the dispmgr/decodemgr
    if ((tsb = lockTSBByDisplay(video_device)) != NULL)
        mpeos_mutexRelease(tsb->mutex);
    if ((pb = lockPlaybackByDisplay(video_device)) != NULL)
        mpeos_mutexRelease(pb->mutex);
    if (tsb != NULL || pb != NULL)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - Must stop existing playback before starting a new one!\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error;
    }

    // Allocate a new playback structure
    if (mpe_memAlloc(sizeof(os_DvrPlayback), (void**) &newPlayback)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Cannot allocate new playback structure\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error;
    }

    newPlayback->evQueue = evQueue;
    newPlayback->act = act;
    newPlayback->trickMode = requestedRate;
    newPlayback->videoDevice = video_device;
    mpeos_mutexNew(&newPlayback->mutex);
    strcpy(newPlayback->datafile, datafile);

    // Update our current playback position in the data file
    if (MPE_SUCCESS != (err = getRecFileInfo(newPlayback->datafile, &info)))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                  "<<DVR>> %s(%p) - getRecFileInfo error %d\n",
                  __FUNCTION__, tsb, err);
        goto error;
    }

    info.playbackTime = mediaTime;

    if (MPE_SUCCESS != (err = setRecFileInfo(newPlayback->datafile, &info)))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                  "<<DVR>> %s(%p) - setRecFileInfo error %d\n",
                  __FUNCTION__, pb, err);
        goto error;
    }

    // Allocate an array of RI_Platform pids
    if (mpe_memAlloc(sizeof(ri_pid_info_t) * pidCount, (void**) &ri_pids)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Cannot allocate ri_pids\n", __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error;
    }

    // Copy PID information
    newPlayback->pidCount = pidCount;
    for (i = 0; i < pidCount; i++)
    {
        // Copy to our playback structure
        newPlayback->pids[i].streamType = pids[i].streamType;
        newPlayback->pids[i].recEltStreamType = pids[i].srcEltStreamType;
        newPlayback->pids[i].recPid = pids[i].srcPid;

        // Copy to RI pid structure
        ri_pids[i].mediaType = pids[i].streamType;
        ri_pids[i].recFormat = pids[i].srcEltStreamType;
        ri_pids[i].recPid = pids[i].srcPid;
    }

    // Start the playback
    if (recording_playback_start(video_device, (const char*) platformPath,
            (const char*) platformRecName, mediaTime, requestedRate, ri_pids,
            pidCount, playback_event_cb, (void*) newPlayback) != RI_ERROR_NONE)
    {
        mpe_memFree(newPlayback);
        mpe_memFree(ri_pids);
        *actualRate = requestedRate;
        *actualGain = requestedGain;

        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - Platform returned an error when initiating a playback!\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error;
    }

    gActivePlay[playbackIdx] = newPlayback;

    mpeos_mutexRelease(gPlayMutex);
    mpe_memFree(ri_pids);

    /* return playback handle */
    *playback = (mpe_DvrPlayback) newPlayback;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - Returning playback handle = %p\n", __FUNCTION__,
            newPlayback);

    return MPE_DVR_ERR_NOERR;

    error: mpeos_mutexRelease(gPlayMutex);
    return err;
}

mpe_Error mpeos_dvrPlaybackGetVideoScanMode (mpe_DvrPlayback playback, mpe_MediaScanMode* scanMode)
{
    *scanMode = g_videoScanMode;

    return (mpe_Error) MPE_SUCCESS;
}

mpe_Error mpeos_dvrPlaybackGet3DConfig (mpe_DvrPlayback playback, mpe_DispStereoscopicMode* stereoscopicMode,
    mpe_Media3DPayloadType* payloadType, uint8_t* payload, uint32_t* payloadSz)
{
    if (*payloadSz < g_payloadSz)
    {
        *payloadSz = g_payloadSz;
        return (mpe_Error) MPE_ENOMEM;
    }

    *payloadType = g_payloadType;
    *stereoscopicMode = g_stereoscopicMode;

    memcpy (payload, g_payload, g_payloadSz);

    *payloadSz = g_payloadSz;

    return (mpe_Error) MPE_SUCCESS;
}
/**
 * <i>mpeos_dvrPlaybackSetMute()</i>
 *
 * Set the mute
 */
mpe_Error mpeos_dvrPlaybackSetMute(mpe_DvrPlayback playback, mpe_Bool mute)
{
    MPE_UNUSED_PARAM(playback);
    MPE_UNUSED_PARAM(mute);
    return MPE_SUCCESS;
}

/**
 * <i>mpeos_dvrPlaybackSetGain()</i>
 *
 * Set the gain
 */
mpe_Error mpeos_dvrPlaybackSetGain(mpe_DvrPlayback playback, float gain, float *actualGain)
{
    MPE_UNUSED_PARAM(playback);
    MPE_UNUSED_PARAM(gain);
    *actualGain = gain;
    return MPE_SUCCESS;
}

/*
 * Description:
 *
 * This function creates and starts a new DVR playback from a given  time shift buffer.
 * A new playback handle is returned if the call is successful. The recording will start
 * playing back at the normal rate (1.0) at the given position.
 * The returned handle can be used to control the speed,
 * the direction and the position of the play back in the stream.
 *
 *
 * Parameters:
 *
 *  videoDevice     the videoDevice to which the playback is presented.
 *  pid             pointer to the list of pids to be played back
 *  pidCount        the number of pids to play back
 *  mediaTime       the position at which the playback is started
 *  requestedRate   initial rate
 *  actualRate      pointer to a float that will be set with the actual rate
 *  blocked         designates whether the video is initially considered blocked.
 *                    (See mpeos_dvrPlaybackBlockPresentation() below for more information.)
 *  muted           Initial mute state
 *  requestedGain   initial gain
 *  actualGain      pointer to a float that will be set with the actual gain
 *  evQueue         queue to sent playback events to.
 *  act             the completion token for async events
 *  playback        pointer to a DVR playback handle. This handle is used
 *                    to control trick play and media positions.
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_OS_FAILURE
 *  MPE_DVR_ERR_DEVICE_ERR
 */
mpe_Error mpeos_dvrTsbPlayStart(mpe_DvrTsb buffer, mpe_DispDevice videoDevice,
        mpe_DvrPidInfo *pids, uint32_t pidCount, int64_t mediaTime,
        float requestedRate, float *actualRate, mpe_Bool blocked, mpe_Bool muted, float requestedGain, float *actualGain,
        uint8_t cci, int64_t alarmMediaTime, mpe_EventQueue evQueue, void *act,
        mpe_DvrPlayback *playback)
{
    uint32_t i;
    ri_video_device_t* video_device;
    os_DvrTsbPlayback* newPlayback;
    os_DvrPlayback* pb = NULL;
    os_DvrTimeShiftBuffer* tsb = NULL;
    os_DvrTimeShiftBuffer* tsBuffer = (os_DvrTimeShiftBuffer*) buffer;
    ri_pid_info_t* ri_pids;
    mpe_Error err;

    MPE_UNUSED_PARAM(blocked);
    MPE_UNUSED_PARAM(muted);
    MPE_UNUSED_PARAM(cci);
    MPE_UNUSED_PARAM(alarmMediaTime);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "<<DVR>> %s - TSB = %p, mediaTime = %"PRId64", requestedRate = %f\n",
            __FUNCTION__, tsBuffer, mediaTime, requestedRate);

    if (buffer == NULL || playback == NULL || pidCount == 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid Parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    // RITODO: When we support multiple displays, we can keep track of playbacks better
    video_device = dispGetVideoDevice(videoDevice);
    if (NULL == video_device)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Unable to find specified video device\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error;
    }

    // Ensure that this video device is not engaged in any other playback
    // TODO: Should go through the dispmgr/decodemgr
    if ((tsb = lockTSBByDisplay(video_device)) != NULL)
        mpeos_mutexRelease(tsb->mutex);
    if ((pb = lockPlaybackByDisplay(video_device)) != NULL)
        mpeos_mutexRelease(pb->mutex);
    if (tsb != NULL || pb != NULL)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - Must stop existing playback before starting a new one!\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error;
    }

    // Ensure that this TSB still exists
    if (!lockTSB(tsBuffer))
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - No active TSB! (%p).  TSB may have been deleted.\n",
                __FUNCTION__, tsBuffer);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error;
    }

    // Ensure that this TSB is not already playing back
    if (tsBuffer->playback != NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - TSB already has active playback session (%p)\n",
                __FUNCTION__, tsBuffer);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error;
    }

    // Allocate a new playback structure
    if (mpe_memAlloc(sizeof(os_DvrTsbPlayback), (void**) &newPlayback)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Cannot allocate new playback structure\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error;
    }

    // Allocate an array of RI_Platform pids
    if (mpe_memAlloc(sizeof(ri_pid_info_t) * pidCount, (void**) &ri_pids)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Cannot allocate ri_pids\n", __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error;
    }

    newPlayback->videoDevice = video_device;
    newPlayback->evQueue = evQueue;
    newPlayback->act = act;
    newPlayback->trickMode = requestedRate;
    newPlayback->currentTime = mediaTime;
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
              "<<DVR>> %s (%p) - playback = %p, current time = %"PRIu64"\n",
              __FUNCTION__, tsBuffer, newPlayback, newPlayback->currentTime);

    // Copy PID information from src (as specified by upper-layers) to
    //  rec (as expected by lower-layer)
    newPlayback->pidCount = pidCount;
    for (i = 0; i < pidCount; i++)
    {
        // Copy to our playback structure
        newPlayback->pids[i].streamType = pids[i].streamType;
        newPlayback->pids[i].recEltStreamType = pids[i].srcEltStreamType;
        newPlayback->pids[i].recPid = pids[i].srcPid;

        // Copy to RI pid structure
        ri_pids[i].mediaType = pids[i].streamType;
        ri_pids[i].recFormat = pids[i].srcEltStreamType;
        ri_pids[i].recPid = pids[i].srcPid;
    }

    // Start the playback
    if (tsb_playback_start(tsBuffer->platformHandle, video_device, ri_pids,
            pidCount, mediaTime, requestedRate, tsb_event_cb, (void*) tsBuffer)
            != RI_ERROR_NONE)
    {
        mpe_memFree(newPlayback);
        mpe_memFree(ri_pids);
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - Platform returned an error when initiating a playback!\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error;
    }

    tsBuffer->playback = newPlayback;
    mpeos_mutexRelease(tsBuffer->mutex);
    mpe_memFree(ri_pids);

    *actualRate = requestedRate;
    *actualGain = requestedGain;

    /* return playback handle */
    *playback = (mpe_DvrPlayback) newPlayback;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - Returning playback handle = %p\n", __FUNCTION__,
            newPlayback);

    return MPE_DVR_ERR_NOERR;

    error: mpeos_mutexRelease(tsBuffer->mutex);
    return err;
}

/*
 * Description:
 *
 *  This function stops a given playback. This function does nothing if
 *  the playback was already stopped. An event is sent to notify the
 *  caller that the playback state has changed.
 *
 * Parameters:
 *
 *  playback    handle to a playback.
 *  holdFrameMode   display black (default) or last frame
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 */
mpe_Error mpeos_dvrPlayBackStop(mpe_DvrPlayback playback, uint32_t holdFrameMode)
{
    os_DvrTsbPlayback *tsbPB = (os_DvrTsbPlayback*) playback;
    os_DvrPlayback *pb = (os_DvrPlayback*) playback;
    os_DvrTimeShiftBuffer *tsb = NULL;
    ri_video_device_t *video_device = NULL;

    MPE_UNUSED_PARAM(holdFrameMode);

    if (pb == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid Parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - playback = %p\n",
            __FUNCTION__, tsbPB);

    // Find our pipeline index by searching standard and TSB playback sessions
    if ((tsb = lockTSBByPlayback(tsbPB)) != NULL)
    {
        video_device = tsbPB->videoDevice;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - playback is TSB. Video device = %p\n",
                __FUNCTION__, video_device);

        mpeos_mutexRelease(tsb->mutex);

        // Terminate the playback
        if (playback_stop(video_device) != RI_ERROR_NONE)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                    "<<DVR>> %s - Platform could not stop playback!\n",
                    __FUNCTION__);
            return MPE_DVR_ERR_OS_FAILURE;
        }
    }
    else if (lockPlayback(pb))
    {
        video_device = pb->videoDevice;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - playback is recording. Video device = %p\n",
                __FUNCTION__, video_device);

        mpeos_mutexRelease(pb->mutex);

        // Terminate the playback
        if (playback_stop(video_device) != RI_ERROR_NONE)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                    "<<DVR>> %s - Platform could not stop playback!\n",
                    __FUNCTION__);
            return MPE_DVR_ERR_OS_FAILURE;
        }
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid playback session\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 *  This function returns the current play back time. This time represents how long the playback has been ongoing.
 *
 * Parameters:
 *
 *  playback    an active playback handle.
 *  mediaTime   returned media time in nanoseconds.
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_OS_FAILURE
 */
mpe_Error mpeos_dvrPlaybackGetTime(mpe_DvrPlayback playback, int64_t *mediaTime)
{
    os_DvrTsbPlayback *tsbPB = (os_DvrTsbPlayback*) playback;
    os_DvrPlayback *pb = (os_DvrPlayback*) playback;
    os_DvrTimeShiftBuffer *tsb = NULL;

    if (playback == NULL || mediaTime == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid Parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    if ((tsb = lockTSBByPlayback(tsbPB)) != NULL)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - playback is TSB\n",
                __FUNCTION__);
        *mediaTime = tsbPB->currentTime;
        mpeos_mutexRelease(tsb->mutex);
    }
    else if (lockPlayback(pb))
    {
        os_RecFileInfo info;

        if (getRecFileInfo(pb->datafile, &info) != MPE_SUCCESS)
        {
            mpeos_mutexRelease(pb->mutex);
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - Can not get current playback time from file.\n",
                    __FUNCTION__);
            return MPE_DVR_ERR_OS_FAILURE;
        }

        *mediaTime = (int64_t) info.playbackTime;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - playback is normal recording\n", __FUNCTION__);

        mpeos_mutexRelease(pb->mutex);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - No active playback found!\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - media time = %"PRId64"\n",
            __FUNCTION__, *mediaTime);

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 *  This function returns the media time of the closest renderable frame to the given
 *  mediaTime in the given direction. It is expected that calling mpeos_dvrPlaybackGetTime()
 *  after calling mpeos_dvrPlaybackSetTime() with a value returned from this function will
 *  result in the same value returned in frameTime.
 *
 * Parameters:
 *
 *  recordingName    unique name identifier, representing the permanent recording.
 *  mediaTime       the desired mediaTime
 *  direction       desired direction for the nearest renderable frame
 *  frameTime       a pointer to store the media time corresponding with the nearest
 *                  renderable frame
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM   if playback handle is not valid or active
 *                              or the mediaTime is out of range
 *  MPE_DVR_ERR_OS_FAILURE
 */
mpe_Error mpeos_dvrRecordingMediaTimeForFrame(char * recordingName,
        int64_t mediaTime, mpe_DvrDirection direction, int64_t * frameTime)
{
    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "<<DVR>> %s -- NOT IMPLEMENTED\n",
            __FUNCTION__);
    MPE_UNUSED_PARAM(recordingName);
    MPE_UNUSED_PARAM(mediaTime);
    MPE_UNUSED_PARAM(direction);
    MPE_UNUSED_PARAM(frameTime);
    return MPE_DVR_ERR_NOT_IMPLEMENTED;
}

/*
 * Description:
 *
 *  This function steps one video frame forward or backward on a paused playback session.
 *  The next video frame may be the next fully-coded frame (e.g. an MPEG-2 I/P frame) or
 *  an intermediate frame, if the platform supports it. After a successful call, the
 *  media time returned by mpeos_dvrPlaybackGetTime() must reflect the selected frame.
 *
 * Parameters:
 *
 *  playback        an active playback handle.
 *  stepDirection   direction to step
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM   if playback handle is not valid or active
 *  MPE_DVR_ERR_UNSUPPORTED     if the playback is not paused (rate==0)
 *  MPE_DVR_ERR_OS_FAILURE
 */
mpe_Error mpeos_dvrPlaybackStepFrame(mpe_DvrPlayback playback,
        mpe_DvrDirection stepDirection)
{
    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "<<DVR>> %s -- NOT IMPLEMENTED\n",
            __FUNCTION__);
    MPE_UNUSED_PARAM(playback);
    MPE_UNUSED_PARAM(stepDirection);
    return MPE_DVR_ERR_NOT_IMPLEMENTED;
}

/*
 * Description:
 *
 *  This function sets the playback position in the playback stream.
 *
 * Parameters:
 *
 *  playback    an active playback handle.
 *  mediaTime   position to jump to (in nanoseconds).
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_OS_FAILURE
 */
mpe_Error mpeos_dvrPlaybackSetTime(mpe_DvrPlayback playback, int64_t mediaTime)
{
    os_DvrTsbPlayback *tsbPB = (os_DvrTsbPlayback*) playback;
    os_DvrPlayback *pb = (os_DvrPlayback*) playback;
    os_DvrTimeShiftBuffer *tsb = NULL;
    ri_video_device_t* video_device = NULL;
    mpe_Error err;

    if (mediaTime < 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - invalid mediaTime\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }
    if (mediaTime == MPE_DVR_POSITIVE_INFINITY)
    {
        // RITODO
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - POSITIVE_INFINITY media time not supported yet!\n",
                __FUNCTION__);
        return MPE_DVR_ERR_OS_FAILURE;
    }

    if (pb == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid Parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "<<DVR>> %s -- playback = %p, mediatime = %"PRId64"\n",
            __FUNCTION__, tsbPB, mediaTime);

    // Find our pipeline index by searching standard and TSB playback sessions
    if ((tsb = lockTSBByPlayback(tsbPB)) != NULL)
    {
        video_device = tsbPB->videoDevice;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - playback is TSB. Video device = %p\n",
                __FUNCTION__, video_device);

        // Set the playback position
        if (playback_set_position(video_device, mediaTime) != RI_ERROR_NONE)
        {
            mpeos_mutexRelease(tsb->mutex);
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                    "<<DVR>> %s - Platform could not set playback position!\n",
                    __FUNCTION__);
            return MPE_DVR_ERR_OS_FAILURE;
        }

        tsbPB->currentTime = (uint64_t) mediaTime;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                  "<<DVR>> %s (%p) - playback = %p, current time = %"PRIu64"\n",
                  __FUNCTION__, tsb, tsbPB, tsbPB->currentTime);
        mpeos_mutexRelease(tsb->mutex);
    }
    else if (lockPlayback(pb))
    {
        os_RecFileInfo info;

        video_device = tsbPB->videoDevice;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - playback is recording. Video device = %p\n",
                __FUNCTION__, video_device);

        // Set the playback position
        if (playback_set_position(video_device, mediaTime) != RI_ERROR_NONE)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                    "<<DVR>> %s - Platform could not set playback position!\n",
                    __FUNCTION__);
            err = MPE_DVR_ERR_OS_FAILURE;
            goto error;
        }

        // Set our current media time
        if (getRecFileInfo(pb->datafile, &info) != MPE_SUCCESS)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - Can not get current playback time from file.\n",
                    __FUNCTION__);
            err = MPE_DVR_ERR_OS_FAILURE;
            goto error;
        }

        info.playbackTime = (uint64_t) mediaTime;

        if (setRecFileInfo(pb->datafile, &info) != MPE_SUCCESS)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - Can not set current playback time from file.\n",
                    __FUNCTION__);
            err = MPE_DVR_ERR_OS_FAILURE;
            goto error;
        }

        mpeos_mutexRelease(pb->mutex);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid playback session\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    return MPE_DVR_ERR_NOERR;

    error: mpeos_mutexRelease(pb->mutex);
    return err;
}

/*
 * Description:
 *
 *  This function returns the current playback pid information
 *
 * Parameters:
 *
 *  playback    an active playback handle.
 *  pidTable    pointer to DVR pid set that contains pid information
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_OS_FAILURE
 */
mpe_Error mpeos_dvrPlaybackGetPids(mpe_DvrPlayback playback,
        mpe_DvrPidTable *pidTable)
{
    os_DvrPlayback *pb = (os_DvrPlayback*) playback;
    os_RecFileInfo info;

    if (pidTable == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid parameters!\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    if (pb == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid Playback handle\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - playback = %p\n",
            __FUNCTION__, pb);

    if (getRecFileInfo(pb->datafile, &info) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Can not get current playback info from file.\n",
                __FUNCTION__);
        return MPE_DVR_ERR_OS_FAILURE;
    }

    // RITODO
    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "<<DVR>> %s - NOT IMPLEMENTED!\n",
            __FUNCTION__);

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 *   Block or unblock the presentation of the desired dvr playback.  Blocking dvr
 *   playback is accomplished by muting audio and displaying a black video area.
 *   The dvr playback continues to process the stream as expected, however, the audio
 *   is not emitted and the video is not displayed.   This method controls the blocking
 *   state of the dvr playback by either blocking or unblocking the audio/video output.
 *
 * @param playback - handle to the media dvr playback to be block / unblocked
 * @param block - boolean indicating whether to block (TRUE) or unblock (FALSE)
 *
 * @return MPE_DVR_ERR_INVALID_PARAM - the playback parameter is null or invalid.
 *         MPE_SUCCESS - the function completed successfully.
 */
mpe_Error mpeos_dvrPlaybackBlockPresentation(mpe_DvrPlayback playback,
        mpe_Bool block)
{
    os_DvrPlayback *pb = (os_DvrPlayback*) playback;

    if (pb == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - NULL playback handle\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "<<DVR>> %s - NOT IMPLEMENTED!\n",
            __FUNCTION__);

    // have the specified decoder perform the block/unblock operation
    // RITODO
    //if (!Decoder_BlockPresentation(decoderId, block))
    //{
    //    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
    //              "mpeos_dvrPlaybackBlockPresentation() could not perform block/unblock operation\n");
    //    ret = MPE_ERROR_MEDIA_OS;
    //}

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 *   Set the Copy Control Information (CCI) associated with the given playback.
 *   This over-rides the previous CCI associated with this playback session and may
 *   result in a change in the copy protection applied to the content when output to
 *   one or more ports (e.g. analog protection or DTCP encryption).
 *
 * @param playback - handle to the media dvr playback to be block / unblocked
 * @param cci - The Copy Control Information associated with the content being
 *              played back, per Section 9 of the OpenCable CableCARD Copy
 *              Protection 2.0 Specification (CCCP 2.0). Values of CCI indicate
 *              whether output-port-specific content protection should be applied.
 *
 * @return MPE_DVR_ERR_INVALID_PARAM - the playback parameter is null or invalid.
 *         MPE_SUCCESS - the function completed successfully.
 */
mpe_Error mpeos_dvrPlaybackSetCCI(mpe_DvrPlayback playback, uint8_t cci)
{
    MPE_UNUSED_PARAM(playback);
    MPE_UNUSED_PARAM(cci);
    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 *   Set/change/clear the playback alarm.
 *
 * Parameters:
 *  alarmMediaTime   the alarm media time is expressed in nanoseconds and can
 *                   be any value between 0 (the beginning of the stream)
 *                   and the recording length. If MPE_DVR_MEDIATIME_UNSPECIFIED,
 *                   the current alarm is cancelled. Otherwise this value represents
 *                   the media time where the platform must issue
 *                   MPE_DVR_EVT_PLAYBACK_ALARM when the alarm's media time
 *                   is crossed in any direction and at any rate.
 * playback          Handle to the media dvr playback to be block / unblocked
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR           alarm was changed/set successfully
 *  MPE_DVR_ERR_INVALID_PARAM   the playback parameter is null or invalid
 *                              or the alarmMediaTime is outside the bounds of
 *                              the content
 */
mpe_Error mpeos_dvrPlaybackSetAlarm(mpe_DvrPlayback playback, int64_t alarmMediaTime)
{
    MPE_UNUSED_PARAM(playback);
    MPE_UNUSED_PARAM(alarmMediaTime);
    return MPE_DVR_ERR_NOERR;
}


/*
 * Description:
 *
 *  This function controls the playback speed and direction.
 *  The trick mode play may not be supported by the OS.
 *  In this case the closest supported trick mode is set and returned.
 *
 *
 * Parameters:
 *  dvrPlayback     handle to a DVR playback
 *  mode            desired trick mode
 *  actual_mode     the actual mode set by the system.
 *
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_OS_FAILURE
 */
mpe_Error mpeos_dvrSetTrickMode(mpe_DvrPlayback playback, float mode,
        float *actualMode)
{
    os_DvrPlayback *pb = (os_DvrPlayback*) playback;
    os_DvrTsbPlayback *tsbPB = (os_DvrTsbPlayback*) playback;
    os_DvrTimeShiftBuffer *tsb = NULL;
    int rateIdx;
    ri_video_device_t* video_device = NULL;

    if (pb == NULL || actualMode == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> mpeos_dvrSetTrickMode() - Invalid Parameters\n");
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - pb = %p, requested rate = %3f\n", __FUNCTION__, pb,
            mode);

    // Look for the closest supported rate to the requested one
    rateIdx = 0;
    while (rateIdx < playScaleListSize && mode > playScaleList[rateIdx])
        rateIdx++;

    if (mode == playScaleList[rateIdx] || rateIdx == 0) // Exact match or first in the list
        *actualMode = playScaleList[rateIdx];
    else if (rateIdx == playScaleListSize) // Last in the list
        *actualMode = playScaleList[playScaleListSize - 1];
    else
    {
        // Determine which is closer, the current index or the previous
        if (playScaleList[rateIdx] - mode < mode - playScaleList[rateIdx - 1])
            *actualMode = playScaleList[rateIdx];
        else
            *actualMode = playScaleList[rateIdx - 1];
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - setting actual rate to %3f\n", __FUNCTION__,
            *actualMode);

    // Find the given video device in the current list of TSBs or recording playbacks
    if ((tsb = lockTSBByPlayback(tsbPB)) != NULL)
    {
        video_device = tsbPB->videoDevice;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - playback is TSB. Video device = %p\n",
                __FUNCTION__, video_device);

        // Terminate the playback
        if (playback_set_rate(video_device, *actualMode) != RI_ERROR_NONE)
        {
            mpeos_mutexRelease(tsb->mutex);
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                    "<<DVR>> %s - Platform could not set playback rate!\n",
                    __FUNCTION__);
            return MPE_DVR_ERR_OS_FAILURE;
        }

        mpeos_mutexRelease(tsb->mutex);
    }
    else if (lockPlayback(pb))
    {
        video_device = tsbPB->videoDevice;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - playback is recording. Video device = %p\n",
                __FUNCTION__, video_device);

        // Terminate the playback
        if (playback_set_rate(video_device, *actualMode) != RI_ERROR_NONE)
        {
            mpeos_mutexRelease(pb->mutex);
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                    "<<DVR>> %s - Platform could not set playback rate!\n",
                    __FUNCTION__);
            return MPE_DVR_ERR_OS_FAILURE;
        }

        mpeos_mutexRelease(pb->mutex);
    }
    else
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid playback session\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 *  This function returns the current trick mode associated with a given playback.
 *
 * Parameters:
 *  playback    Handle to a DVR playback
 *  mode        Pointer to the current trick mode play
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR           if successful
 *  MPE_DVR_ERR_INVALID_PARAM   if a parameter is invalid
 */
mpe_Error mpeos_dvrGetTrickMode(mpe_DvrPlayback playback, float *mode)
{
    os_DvrPlayback *pb = (os_DvrPlayback*) playback;

    if (playback == NULL || mode == NULL)
    {
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - trickMode = %3f, playback = %p\n", __FUNCTION__,
            pb->trickMode, pb);

    *mode = pb->trickMode;

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 *  This function allocates a new time shift buffer and returns a handle to it.
 *  The implementation should return an error in case there is not enough space on
 *  the given storage device to create the buffer. mpe_DvrTsbNew() does not
 *  start the time shift buffer recording, but pre-allocates space on the storage
 *  device for future use. This makes sure a time shift buffer is be available at
 *  the time of recording.
 *
 * Parameters:
 *  duration    duration (in seconds) of the time shift buffer
 *  storage     a handle to the storage device where the buffer is
 *              created.
 *  buffer      a handle to the new buffer
 *
 * Returns:
 *
 *  MPE_DVR_ERR_NOERR           if successful
 *  MPE_DVR_ERR_INVALID_PARAM   if a parameter is invalid
 *  MPE_DVR_ERR_OS_FAILURE      if an OS failure occurs
 *  MPE_DVR_ERR_OUT_OF_SPACE    if no space is available on the
 *                              storage device.
 */
mpe_Error mpeos_dvrTsbNew(mpe_StorageHandle device, int64_t duration,
        mpe_DvrTsb *buffer)
{
    os_DvrTimeShiftBuffer *newTimeShiftBuffer;
    os_StorageDeviceInfo* dev = (os_StorageDeviceInfo*) device;
    char tsbPath[MPE_FS_MAX_PATH];
    uint32_t tsbIdx;
    uint64_t tsbSize;
    mpe_Error err = MPE_DVR_ERR_NOERR;

    /* Sanity check */
    if (duration == 0 || buffer == NULL || duration < OS_DVR_MIN_TSB_DURATION)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    // TODO: <STMGR-COMP> Remove this check?
    if (dev == NULL)
    {
        dev = storageGetDefaultDevice();
        MPEOS_LOG(
                MPE_LOG_DEBUG,
                MPE_MOD_DVR,
                "<<DVR>> %s - storage device is null! Using default storage deivce!\n",
                __FUNCTION__);
    }

    // Allocate space on the device
    tsbSize = ((MPE_DVR_BITRATE_HIGH * 1000) / 8) * (uint64_t) duration;
    mpeos_mutexAcquire(dev->mutex);
    if (dev->freeMediaSize < tsbSize)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - Not enough space on device (%s) to allocate tsb!!\n",
                __FUNCTION__, dev->name);
        mpeos_mutexRelease(dev->mutex);
        return MPE_DVR_ERR_OUT_OF_SPACE;
    }
    dev->freeMediaSize -= tsbSize;
    mpeos_mutexRelease(dev->mutex);

    mpeos_mutexAcquire(gTsbMutex);

    // Do we have available TSB slots?
    for (tsbIdx = 0; tsbIdx < MAX_TSBS; ++tsbIdx)
    {
        if (gTimeShiftBuffers[tsbIdx] == NULL)
            break;
    }
    if (tsbIdx == MAX_TSBS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - No more TimeShiftBuffers can be created!\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - storage device (%s), duration = %"PRId64", tsbIdx = %d\n",
            __FUNCTION__, dev->name, duration, tsbIdx);

    // Create tsbPath
    sprintf(tsbPath, "%s/%s", dev->rootPath, OS_DVR_TSB_DIR_NAME);
    (void) createFullPath(tsbPath);

#ifdef MPE_WINDOWS
    // Convert /c/ to c:/
    tsbPath[0] = tsbPath[1];
    tsbPath[1] = ':';
#endif

    // Allocate a new TSB structure
    if (mpe_memAlloc(sizeof(os_DvrTimeShiftBuffer),
            (void **) &(newTimeShiftBuffer)) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - unable to allocate memory for tsb\n",
                __FUNCTION__);
        err = MPE_ENOMEM;
        goto error;
    }

    // Initialize platform TSB
    if (tsb_init(tsbPath, duration, &newTimeShiftBuffer->platformHandle)
            != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Platform could not initialize TSB\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error;
    }

    newTimeShiftBuffer->duration = duration;
    newTimeShiftBuffer->size = tsbSize;
    newTimeShiftBuffer->device = dev;
    newTimeShiftBuffer->playback = NULL;
    newTimeShiftBuffer->recording = NULL;
    newTimeShiftBuffer->buffering = NULL;
    mpeos_mutexNew(&newTimeShiftBuffer->mutex);

    gTimeShiftBuffers[tsbIdx] = newTimeShiftBuffer;

    mpeos_mutexRelease(gTsbMutex);

    *buffer = (mpe_DvrTsb) newTimeShiftBuffer;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - Returning TSB = %p, platformHandle = %p\n",
            __FUNCTION__, newTimeShiftBuffer,
            newTimeShiftBuffer->platformHandle);

    return MPE_DVR_ERR_NOERR;

    error: mpeos_mutexRelease(gTsbMutex);
    return err;
}

/*
 * Description:
 *
 * mpeos_dvrTsbBufferingStart initiates the recording of the specified PIDs into the
 * given time shift buffer and returns a buffering handle.
 *
 * The following events may be sent on the specified mpe_EventQueue:
 *
 *  MPE_DVR_EVT_SESSION_RECORDING: indicates the buffering session has started.
 *  MPE_DVR_EVT_SESSION_NO_DATA: indicates that the buffering session is not recording
 *                               due to lack of source data
 *  MPE_DVR_EVT_OUT_OF_SPACE: indicates the buffer session was closed due to lack of space
 *  MPE_DVR_EVT_CCI_UPDATE:  immediately upon start when CCI is available and whenever CCI changes
 *  MPE_DVR_EVT_SESSION_CLOSED: indicates the buffer session was closed for a non-specific reason
 *
 *
 * Parameters:
 *  tunerId             represents the tuner from which the recording is performed.
 *  buffer              a handle to a time shift buffer.
 *  bit_rate            desired recording quality.
 *  desiredDuration     duration (in seconds) that is requested for buffering.
 *                      This value may be larger than the TSB's pre-allocated
 *                      size (only intended for platforms with expandable/
 *                      variable-rate TSBs).
 *  maxDuration         maximum duration of content (in seconds) that the
 *                      platform may hold in the TSB (e.g. when buffering is
 *                      restricted due to copy control). Note: This value may
 *                      be smaller than the TSB's pre-allocated duration.
 *  duration    duration (in seconds) of the time shift buffer
 *  pids                array of pids to be recorded. If the given pids
 *                      are invalid, an MPE_DVR_ERR_INVALID_PARAM error is returned.
 *  pidCount            number of pids in the array
 *  queueId             the event queue to post DVR recording events.
 *  act                 the completion token for async events
 *  tsbSession          a handle to the active buffering session
 *
 * Returns:
 *
 *  MPE_DVR_ERR_NOERR           if successful
 *  MPE_DVR_ERR_INVALID_PARAM   if a parameter is invalid
 *  MPE_DVR_ERR_OS_FAILURE      if an OS failure occurs
 *  MPE_DVR_ERR_OUT_OF_SPACE    if no space is available on the storage device.
 */
mpe_Error mpeos_dvrTsbBufferingStart(uint32_t tunerID, uint8_t ltsid,
        mpe_DvrTsb buffer,
        mpe_DvrBitRate bitRate,
        int64_t desiredDuration,
        int64_t maxDuration,
        mpe_EventQueue evQueue, void *act,
        mpe_DvrPidInfo *pids, uint32_t pidCount, mpe_DvrBuffering *tsbSession)
{
    uint32_t pipelineIdx, i;
    os_DvrTimeShift *newTimeShift;
    os_DvrTimeShiftBuffer *tsBuffer = (os_DvrTimeShiftBuffer*) buffer;
    ri_pid_info_t* ri_pids;
    mpe_Error err;

    MPE_UNUSED_PARAM(ltsid);
    MPE_UNUSED_PARAM(bitRate);

    MPE_UNUSED_PARAM(desiredDuration);
    MPE_UNUSED_PARAM(maxDuration);
    // The RI doesn't support extendable TSBs - it just buffers as much
    //  as the TSB holds - no more, no less. Note: It isn't really legal
    //  to ignore maxDuration. When/if CCI TSB enforcement is implemented,
    //  maxDuration cannot be ignored.

    /* Sanity check */
    if (tunerID == 0 || buffer == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    // Check to see if the tuner ID is already involved in a buffering session
    mpeos_mutexAcquire(gTsbMutex);
    for (i = 0; i < MAX_TSBS; ++i)
    {
        os_DvrTimeShiftBuffer* tsb = gTimeShiftBuffers[i];
        if (tsb != NULL && tsb->buffering != NULL && tsb->buffering->tunerID
                == tunerID)
        {
            mpeos_mutexRelease(gTsbMutex);
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - Tuner ID %d already has an active TSB session!\n",
                    __FUNCTION__, tunerID);
            return MPE_DVR_ERR_INVALID_PARAM;
        }
    }
    mpeos_mutexRelease(gTsbMutex);

    // Find our TSB index
    if (!lockTSB(tsBuffer))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Buffer (%p) is invalid!\n", __FUNCTION__,
                tsBuffer);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    // Check to see if this TSB is already buffering
    if (tsBuffer->buffering != NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Buffer (%p) is already buffering!\n",
                __FUNCTION__, tsBuffer);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error3;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Buffer = %p\n",
            __FUNCTION__, tsBuffer);

    // For now, we always flush the TSB before we start buffering
    if (tsb_flush(tsBuffer->platformHandle) != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Platform could not flush TSB!\n", __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error3;
    }

    pipelineIdx = tunerID - 1;

    uint32_t numPipes;
    const ri_pipeline_t** pipelines =
            ri_get_pipeline_manager()->get_live_pipelines(
                    ri_get_pipeline_manager(), &numPipes);

    // Allocate an array of RI_Platform pids
    if (mpe_memAlloc(sizeof(ri_pid_info_t) * pidCount, (void**) &ri_pids)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Cannot allocate ri_pids\n", __FUNCTION__);
        err = MPE_ENOMEM;
        goto error3;
    }

    // Allocate a new TS structure
    if (mpe_memAlloc(sizeof(os_DvrTimeShift), (void **) &newTimeShift)
            != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - unable to allocate memory for tsb\n",
                __FUNCTION__);
        err = MPE_ENOMEM;
        goto error2;
    }

    // Handle PIDs
    for (i = 0; i < pidCount; i++)
    {
        // Copy to the RI's pid array
        ri_pids[i].mediaType = pids[i].streamType;
        ri_pids[i].srcPid = pids[i].srcPid;
        ri_pids[i].srcFormat = pids[i].srcEltStreamType;
    }

    // Reset our buffer time info
    tsBuffer->sysStartTime = 0;
    tsBuffer->startTime = 0;
    tsBuffer->endTime = 0;
    tsBuffer->isFull = FALSE;

    // Populate our active TSB data structure
    newTimeShift->pidCount = pidCount;
    newTimeShift->tunerID = tunerID;
    newTimeShift->evQueue = evQueue;
    newTimeShift->act = act;
    mpeos_condNew(FALSE, FALSE, &newTimeShift->startCond);

    tsBuffer->buffering = newTimeShift;

    mpeos_mutexRelease(tsBuffer->mutex);

    // Tell the platform to start a new TSB
    if (pipelines[pipelineIdx]->tsb_start(
            (ri_pipeline_t*) pipelines[pipelineIdx], tsBuffer->platformHandle,
            ri_pids, pidCount, tsb_event_cb, (void*) tsBuffer) != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Platform could not start TSB!\n", __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error1;
    }

    for (i = 0; i < pidCount; i++)
    {
        // handle pid re-mapping
        MPEOS_LOG(
                MPE_LOG_INFO,
                MPE_MOD_DVR,
                "%s srcType:%d srcPid:%d srcFormat:%d, recPid:%d recFormat:%d\n",
                __FUNCTION__, ri_pids[i].mediaType, ri_pids[i].srcPid,
                ri_pids[i].srcFormat, ri_pids[i].recPid, ri_pids[i].recFormat);

        pids[i].recPid = ri_pids[i].recPid;
        pids[i].recEltStreamType = ri_pids[i].recFormat;

        newTimeShift->pids[i].recPid = ri_pids[i].recPid;
        newTimeShift->pids[i].recEltStreamType = ri_pids[i].recFormat;
    }

    mpe_memFree(ri_pids);

    *tsbSession = (mpe_DvrBuffering) newTimeShift;

    // Wait for the TSB_START event from the platform
    if (mpeos_condWaitFor(newTimeShift->startCond, 10000) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Took more than 10 seconds to start TSB!\n",
                __FUNCTION__);
        // Re-lock our TSB and clear out the buffering session.  If this TSB
        // has been deleted, we will do nothing as expected
        if (lockTSB(tsBuffer))
        {
            tsBuffer->buffering = NULL;
            mpeos_mutexRelease(tsBuffer->mutex);

            // Ensure that the platform knows that this pipeline should not be buffering
            (void) pipelines[pipelineIdx]->tsb_stop(
                    (ri_pipeline_t*) pipelines[pipelineIdx]);

            mpeos_condDelete(newTimeShift->startCond);
            mpe_memFree(newTimeShift);
        }

        return MPE_DVR_ERR_OS_FAILURE;
    }

    MPEOS_LOG(
            MPE_LOG_DEBUG,
            MPE_MOD_DVR,
            "<<DVR>> %s - tsb buffering successfully started. TSB session = %p\n",
            __FUNCTION__, newTimeShift);

    return MPE_DVR_ERR_NOERR;

    error1: tsBuffer->buffering = NULL;
    mpe_memFree(newTimeShift);

    error2: mpe_memFree(ri_pids);

    error3: mpeos_mutexRelease(tsBuffer->mutex);
    return err;
}

/*
 * Description:
 *
 * This function stops the recording in the given time shift buffer.
 * It also deletes the playback associated with it.
 *
 * Parameters:
 *  tsbSession  the active buffering session to stop.
 *
 * Returns:
 *
 *  MPE_DVR_ERR_NOERR           if successful
 *  MPE_DVR_ERR_INVALID_PARAM   if a parameter is invalid
 *  MPE_DVR_ERR_OS_FAILURE      if an OS failure occurs
 */
mpe_Error mpeos_dvrTsbBufferingStop(mpe_DvrBuffering tsbSession)
{
    int pipelineIdx;
    os_DvrTimeShift *session = (os_DvrTimeShift*) tsbSession;
    os_DvrTimeShiftBuffer *tsb = NULL;

    if (tsbSession == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid Parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - TSB session = %p\n",
            __FUNCTION__, tsbSession);

    // Is this a valid TSB session?
    if ((tsb = lockTSBByBuffering(session)) == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid TSB session\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    pipelineIdx = session->tunerID - 1;

    uint32_t numPipes;
    const ri_pipeline_t** pipelines =
            ri_get_pipeline_manager()->get_live_pipelines(
                    ri_get_pipeline_manager(), &numPipes);

    mpeos_mutexRelease(tsb->mutex);

    // Tell the platform to terminate the TSB session
    if (pipelines[pipelineIdx]->tsb_stop(
            (ri_pipeline_t*) pipelines[pipelineIdx]) != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Platform could not stop TSB session\n",
                __FUNCTION__);
        return MPE_DVR_ERR_OS_FAILURE;
    }

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 * This function deletes the given time shift buffer and release all resources
 * associated with it. Any on going recording is lost and playback is stopped.
 *
 * Parameters:
 *      buffer  a handle to the buffer to delete
 *
 * Returns:
 *
 *  MPE_DVR_ERR_NOERR           if successful
 *  MPE_DVR_ERR_INVALID_PARAM   if a parameter is invalid
 *  MPE_DVR_ERR_OS_FAILURE      if an OS failure occurs
 */
mpe_Error mpeos_dvrTsbDelete(mpe_DvrTsb buffer)
{
    os_DvrTimeShiftBuffer *tsBuffer = (os_DvrTimeShiftBuffer*) buffer;
    mpe_Error err = MPE_DVR_ERR_NOERR;
    uint32_t i;

    if (buffer == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s() - Invalid Parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    mpeos_mutexAcquire(gTsbMutex);

    // Find our TSB index
    for (i = 0; i < MAX_TSBS; i++)
    {
        if (gTimeShiftBuffers[i] == tsBuffer)
            break;
    }
    if (i == MAX_TSBS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Buffer (%p) is invalid!\n", __FUNCTION__,
                tsBuffer);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error2;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Buffer = %p\n",
            __FUNCTION__, tsBuffer);

    // First stop any active conversion, playback, and/or buffering active on this buffer
    mpeos_mutexAcquire(tsBuffer->mutex);

    // Stop playback
    if (tsBuffer->playback != NULL)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - TSB has an active playback session! Can't delete!\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error1;
    }

    // Stop conversion
    if (tsBuffer->recording != NULL)
    {
        MPEOS_LOG(
                MPE_LOG_ERROR,
                MPE_MOD_DVR,
                "<<DVR>> %s - TSB has an active recording session! Can't delete!\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error1;
    }

    if (tsBuffer->buffering != NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - TSB currently buffering! Can't delete!\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error1;
    }

    // Tell the platform to delete all media and other files associated with this TSB
    if (tsb_delete(tsBuffer->platformHandle) != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Platform could not delete TSB\n", __FUNCTION__);
        goto error1;
    }

    gTimeShiftBuffers[i] = NULL;
    mpeos_mutexRelease(gTsbMutex);

    // Update our device's available size
    mpeos_mutexAcquire(tsBuffer->device->mutex);
    tsBuffer->device->freeMediaSize += tsBuffer->size;
    mpeos_mutexRelease(tsBuffer->device->mutex);

    mpeos_mutexRelease(tsBuffer->mutex);
    mpeos_mutexDelete(tsBuffer->mutex);
    mpe_memFree(tsBuffer);

    return MPE_DVR_ERR_NOERR;

    error1: mpeos_mutexRelease(tsBuffer->mutex);

    error2: mpeos_mutexRelease(gTsbMutex);
    return err;
}

/*
 * Description:
 *
 *  This function returns information on the Time Shift buffer recording such as start time and end time.
 *
 *
 * Parameters:
 *  buffer      a buffer handle.
 *  param       information parameter.
 *  time        output value for the given parameter.
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_OS_FAILURE
 *  MPE_DVR_ERR_UNSUPPORTED
 *  MPE_DVR_ERR_NOT_ALLOWED
 *
 * Possible mpe_DvrInfoParam are:
 *
 * MPE_DVR_TSB_START_TIME: to get the beginning time of a shift buffer.
 * Output: this value is expressed in nanoseconds. It is equal to 0 if the buffer has
 * not wrapped around, or a position within a valid area of the buffer after the wrap around.
 *
 * MPE_DVR_TSB_END_TIME: to get the end time of a TSB.
 * Output: this value is expressed in nanoseconds. The end time represents how long the
 * TSB has been in MPE_DVR_RECORDING state.
 */
mpe_Error mpeos_dvrTsbGet(mpe_DvrTsb buffer, mpe_DvrInfoParam param,
        int64_t *pTime)
{
    os_DvrTimeShiftBuffer *tsBuffer = (os_DvrTimeShiftBuffer*) buffer;

    if (buffer == NULL || pTime == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid Parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    if (!lockTSB(tsBuffer))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid time shift buffer (%p)\n", __FUNCTION__,
                tsBuffer);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    switch (param)
    {
    case MPE_DVR_TSB_START_TIME:
    {
        int64_t startTime = (int64_t)(tsBuffer->startTime);
        memcpy(pTime, (void *) &startTime, sizeof(int64_t));
        MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_DVR, "<<DVR>> %s - Buffer = %p, MPE_DVR_TSB_START_TIME = %"PRIu64"\n",
                __FUNCTION__, tsBuffer, startTime);
        break;
    }
    case MPE_DVR_TSB_END_TIME:
    {
        int64_t endTime = (int64_t)(tsBuffer->endTime);
        memcpy(pTime, (void *)&endTime, sizeof(int64_t));
        MPEOS_LOG(MPE_LOG_TRACE2, MPE_MOD_DVR, "<<DVR>> %s - Buffer = %p, MPE_DVR_TSB_END_TIME = %"PRIu64"\n",
                __FUNCTION__, tsBuffer, endTime);
        break;
    }
    default:
        mpeos_mutexRelease(tsBuffer->mutex);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "<<DVR>> %s - Illegal atribute (%d)!\n",
                __FUNCTION__, param);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    mpeos_mutexRelease(tsBuffer->mutex);

    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 *  This function returns the media time of the closest renderable frame to the given
 *  mediaTime in the given direction. It is expected that calling mpeos_dvrPlaybackGetTime()
 *  after calling mpeos_dvrPlaybackSetTime() with a value returned from this function will
 *  result in the same value returned in frameTime.
 *
 * Parameters:
 *
 *  tsb             tsb handle
 *  mediaTime       the desired mediaTime
 *  direction       desired direction for the nearest renderable frame
 *  frameTime       a pointer to store the media time corresponding with the nearest
 *                  renderable frame
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM   if playback handle is not valid or active
 *                              or the mediaTime is out of range
 *  MPE_DVR_ERR_OS_FAILURE
 */
mpe_Error mpeos_dvrTsbMediaTimeForFrame(mpe_DvrTsb tsb, int64_t mediaTime,
        mpe_DvrDirection direction, int64_t * frameTime)
{
    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "<<DVR>> %s -- NOT IMPLEMENTED\n",
            __FUNCTION__);
    MPE_UNUSED_PARAM(tsb);
    MPE_UNUSED_PARAM(mediaTime);
    MPE_UNUSED_PARAM(direction);
    MPE_UNUSED_PARAM(frameTime);
    return MPE_DVR_ERR_NOT_IMPLEMENTED;
}

/** <i>mpe_dvrTsbBufferingChangePids</i>
 *
 *  This method allows PID change on an active buffering session.
 *  MPE_DVR_ERR_UNSUPPORTED is returned if the platform does not support this operation.
 *
 * @param:
 * buffering            active time shift buffering session
 * pids                 array of new pids
 * pidCount             number of pids in the array
 * mediaTime            returbed location in the buffer, where the pid change occurs.
 * @return:             an error code
 */
mpe_Error mpeos_dvrTsbBufferingChangePids(mpe_DvrBuffering tsbSession,
        mpe_DvrPidInfo *pids, uint32_t pidCount, int64_t *mediaTime)
{
    MPE_UNUSED_PARAM(mediaTime);

    if (pids == NULL || pidCount == 0 || tsbSession == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    // RITODO
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - NOT IMPLEMENTED\n",
            __FUNCTION__);

    return MPE_DVR_ERR_UNSUPPORTED;
}

/** <i>mpe_dvrTsbBufferingChangeDuration</i>
 *
 *  This method allows the desiredDuration and maxDuration parameters on the
 *  TSB buffering session to be changed. This operation must be performed
 *  without affecting already-buffered content or ongoing mpe_DvrPlayback
 *  of the associated TSB.
 *
 *  MPE_DVR_ERR_UNSUPPORTED may be returned if the platform does not support
 *  this operation.
 *
 * Parameters:
 *
 *  tsbSession          active time shift buffering session
 *  desiredDuration     duration (in seconds) that is requested for buffering.
 *                      This value may be larger than the TSB's pre-allocated
 *                      size (only intended for platforms with expandable/
 *                      variable-rate TSBs). A 0 value implies no change.
 *  maxDuration         maximum duration of content (in seconds) that the
 *                      platform may hold in the TSB (e.g. when buffering is
 *                      restricted due to copy control). Note: This value may
 *                      be smaller than the TSB's allocated size/duration.
 *                      A 0 value implies no change.
 *  Returns
 *     MPE_SUCCESS if the change was successful.
 *
 *     MPE_DVR_ERR_UNSUPPORTED if the platform doesn't support changing the
 *                             duration.
 *
 *     An appropriate error code for internal failures.
 */
mpe_Error mpeos_dvrTsbBufferingChangeDuration(mpe_DvrBuffering tsbSession,
                              int64_t desiredDuration, int64_t maxDuration )
{
    MPE_UNUSED_PARAM(desiredDuration);
    MPE_UNUSED_PARAM(maxDuration);

    if (tsbSession == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    // The RI doesn't support extendable TSBs - it just buffers as much
    //  as the TSB holds - no more, no less. Note: It isn't really legal
    //  to ignore maxDuration. When/if CCI TSB enforcement is implemented,
    //  maxDuration cannot be ignored.

    // Successfully ignored the duration change.
    return MPE_SUCCESS;
}

/** <i>mpe_dvrPlaybackChangePids</i>
 *
 *  This method allows PID change on an active playback session.
 *
 * @param:
 * playback             active playback session
 * pids                 pointer to the list of pids to be played back
 * pidCount             the number of pids to play back 
 * @return:             an error code
 */
mpe_Error mpeos_dvrPlaybackChangePids(mpe_DvrPlayback playback,
        mpe_DvrPidInfo *pids, uint32_t pidCount)
{
    os_DvrPlayback *pb = (os_DvrPlayback*) playback;

    if (pidCount == 0 || pids == NULL || pb == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid parameters!\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    // RITODO
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - NOT IMPLEMENTED\n",
            __FUNCTION__);
    //temporary fix until playbackChangePids is properly implemented - post the content presenting notification
    notifyDvrQueueWithArg(pb->evQueue, pb->act, MPE_CONTENT_PRESENTING, (void*)MPE_PRESENTING_2D_SUCCESS);

    return MPE_DVR_ERR_NOERR;
}

/** <i>mpe_dvrTsbChangeDuration</i>
 *
 *  This method re-allocates the buffer associated with the given
 *  buffering session, based on the given duration (in seconds).
 *  If the buffer is in use, existing content in the buffer should be preserved.
 *  MPE_DVR_ERR_UNSUPPORTED is returned if the platform does not support this operation.
 *
 * @param:
 * buffer           handle to a buffer
 * duration             the new duration in seconds.
 * @return:             an error code
 */
mpe_Error mpeos_dvrTsbChangeDuration(mpe_DvrTsb buffer, int64_t duration)
{
    os_DvrTimeShiftBuffer *tsBuffer = (os_DvrTimeShiftBuffer *) buffer;
    uint32_t newDur = duration;

    if (buffer == NULL || duration == 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid Parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    if (!lockTSB(tsBuffer))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid time shift buffer (%p)\n", __FUNCTION__,
                tsBuffer);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    // If not changing the buffer, just return
    if (duration == tsBuffer->duration)
    {
        mpeos_mutexRelease(tsBuffer->mutex);
        return MPE_DVR_ERR_NOERR;
    }

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "<<DVR>> %s - TSB = %p, duration = %"PRId64"\n",
            __FUNCTION__, tsBuffer, duration);

    // Tell the platform to update the TSB duration
    if (tsb_set_duration(tsBuffer->platformHandle, newDur) != RI_ERROR_NONE)
    {
        mpeos_mutexRelease(tsBuffer->mutex);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Platform could not set new TSB duration\n",
                __FUNCTION__);
        return MPE_DVR_ERR_OS_FAILURE;
    }

    mpeos_mutexRelease(tsBuffer->mutex);
    return MPE_DVR_ERR_NOERR;
}

/*
 * Description:
 *
 * This function converts the content of a TSB into a permanent file.
 * A new conversion handle is returned if the conversion is still on going.
 * This conversion handle is used when we want to stop it.
 * A recording unique name identifier is returned in all cases.
 *
 * Parameters:
 * buffer         handle to a DVR time shift buffer.
 * startTime      System time in milliseconds when to start the conversion.
 *                This value holds the actual content start time on return.
 * duration       length of the conversion in milliseconds.
 * bitRate        recording bit rate.
 * evQueue        the event queue to post DVR recording events.
 * act            the completion token for async events.
 * pidTableCount  number of pid Sets in the array. 0 if not supported.
 * pidTable       array of pid Table to be recorded. NULL if platform does not support this.
 *                The actual recorded pids will be returned to the caller.
 * conversion     a handle representing an on going tsb conversion. This is NULL if the conversion
 *                is all in the past.
 * recordingName  unique name identifier, representing the permanent recording.
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_OS_FAILURE
 *  MPE_DVR_ERR_NOT_ALLOWED
 *  MPE_DVR_ERR_DEVICE_ERR
 *  MPE_DVR_ERR_OUT_OF_SPACE
 */
mpe_Error mpeos_dvrTsbConvertStart(mpe_DvrTsb buffer, mpe_MediaVolume volume,
        int64_t *startTime, int64_t duration, mpe_DvrBitRate bitRate,
        mpe_EventQueue evQueue, void *act, uint32_t pidTableCount,
        mpe_DvrPidTable *pidTable, mpe_DvrConversion *conversion,
        char *recordingName)
{
    char platformRecName[RI_MAX_RECORDING_NAME_LENGTH];
    char platformMediaPath[MPE_FS_MAX_PATH];
    os_DvrTimeShiftBuffer* tsBuffer = (os_DvrTimeShiftBuffer*) buffer;
    os_DvrRecording* newRecording;
    os_RecFileInfo recInfo;
    mpe_TimeMillis systemTime;
    ri_pid_info_t* ri_pids;
    uint64_t convertStartTime;
    uint64_t requestedStartTime;
    mpe_Error err;
    uint32_t i;

    MPE_UNUSED_PARAM(bitRate);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - buffer = %p\n", __FUNCTION__, tsBuffer);

    // Validate parameters
    if (tsBuffer == NULL || conversion == NULL || duration == 0
            || recordingName == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid Parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    if (!lockTSB(tsBuffer))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid time shift buffer (%p)\n", __FUNCTION__,
                tsBuffer);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - buffer = %p, startTime = %"PRId64", duration = %"PRId64", bitRate = %d\n",
            __FUNCTION__, buffer, *startTime, duration, bitRate);

    // Check start time and ensure it doesn't appear to be in the future (to keep the platform sane)
    // (this should only happen when there's a slight system clock differential on systems
    //  with high-granularity system clocks)
    mpeos_timeGetMillis(&systemTime);
    if ((mpe_TimeMillis) * startTime > systemTime)
    {
        MPEOS_LOG(
                MPE_LOG_INFO,
                MPE_MOD_DVR,
                "<<DVR>> %s - Convert start time in the future - adjusting time to NOW (offset %dms)\n",
                __FUNCTION__, ((mpe_TimeMillis) *startTime) - systemTime);
        *startTime = systemTime;
    }

    // Create platform convert start time
    requestedStartTime = *startTime * NANO_MILLIS; // Convert start time passed in to nanoseconds
    convertStartTime = requestedStartTime - tsBuffer->sysStartTime; // Convert to TSB-time
    if (requestedStartTime < tsBuffer->sysStartTime || convertStartTime
            < tsBuffer->startTime)
    {
        MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "<<DVR>> %s - Convert start time is before TSB start time (%"PRIu64"). Starting immediately!\n",
                __FUNCTION__, tsBuffer->sysStartTime);
        convertStartTime = tsBuffer->startTime;
    }

    // Validate the volume information -- if NULL use default volume
    if (volume == NULL)
    {
        volume = storageGetDefaultDevice()->volumes;
        if (volume == NULL)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                    "<<DVR>> %s - No media volumes have been created!\n",
                    __FUNCTION__);
            err = MPE_DVR_ERR_INVALID_PARAM;
            goto error2;
        }
    }
    strcpy(platformMediaPath, volume->mediaPath);

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "<<DVR>> %s - media path = %s, convertStartTime = %"PRIu64", duration = %"PRId64"ms\n",
            __FUNCTION__, platformMediaPath, convertStartTime, duration);

    // Allocate an array of RI_Platform pids
    if (mpe_memAlloc(sizeof(ri_pid_info_t) * pidTable[0].count,
            (void**) &ri_pids) != MPE_SUCCESS)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Cannot allocate ri_pids\n", __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error2;
    }

    // Handle PIDs
    for (i = 0; i < pidTable[0].count; i++)
    {
        // handle pid re-mapping
        MPEOS_LOG(
                MPE_LOG_INFO,
                MPE_MOD_DVR,
                "%s srcType:%d srcPid:%d srcFormat:%d, recPid:%d recFormat:%d\n",
                __FUNCTION__, pidTable[0].pids[i].streamType,
                pidTable[0].pids[i].srcPid,
                pidTable[0].pids[i].srcEltStreamType,
                pidTable[0].pids[i].recPid,
                pidTable[0].pids[i].recEltStreamType);

        // Copy to the RI's pid array
        ri_pids[i].mediaType = pidTable[0].pids[i].streamType;
        ri_pids[i].recPid = pidTable[0].pids[i].srcPid;
        ri_pids[i].recFormat = pidTable[0].pids[i].srcEltStreamType;
    }

#ifdef MPE_WINDOWS
    // Convert /c/ to c:/
    platformMediaPath[0] = platformMediaPath[1];
    platformMediaPath[1] = ':';
#endif

    // Tell the platform to start the conversion
    if (tsb_convert(tsBuffer->platformHandle, platformMediaPath,
            platformRecName, ri_pids, pidTable[0].count, &convertStartTime,
            duration / 1000, convert_event_cb, (void*) tsBuffer)
            != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Platform could not start TSB conversion!\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error1;
    }

    // Allocate a new recording structure
    if (mpe_memAlloc(sizeof(os_DvrRecording), (void **) &(newRecording))
            != MPE_SUCCESS)
    {
        mpe_memFree(ri_pids);
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - unable to allocate memory for tsb\n",
                __FUNCTION__);
        err = MPE_ENOMEM;
        goto error1;
    }
    newRecording->buffer = buffer;
    newRecording->evQueue = evQueue;
    newRecording->act = act;
    newRecording->volume = volume;
    sprintf(newRecording->datafile, "%s/%s", volume->dataPath, platformRecName);
#ifdef MPE_WINDOWS
    // Convert /c/ to c:/
    newRecording->datafile[0] = newRecording->datafile[1];
    newRecording->datafile[1] = ':';
#endif

    tsBuffer->recording = newRecording;

    // Initialize recording metadata
    recInfo.currentLength = 0;
    recInfo.playbackTime = 0;
    recInfo.size = 0;

    // Update our recording structure AND the caller's PID table with
    // actual recorded PIDs provided by the platform
    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR,
            "<<DVR>> %s - PID Summary -- num pids = %d\n", __FUNCTION__,
            pidTable[0].count);
    recInfo.pidCount = pidTable[0].count;
    for (i = 0; i < pidTable[0].count; i++)
    {
        // Update caller's PID table
        pidTable[0].pids[i].recPid = ri_pids[i].recPid;
        pidTable[0].pids[i].recEltStreamType = ri_pids[i].recFormat;

        // Update recinfo
        recInfo.pids[i].streamType = pidTable[0].pids[i].streamType;
        recInfo.pids[i].recPid = ri_pids[i].recPid;
        recInfo.pids[i].recEltStreamType = ri_pids[i].recFormat;
    }

    mpe_memFree(ri_pids);

    // Write the initial metadata
    if (setRecFileInfo(newRecording->datafile, &recInfo) != MPE_DVR_ERR_NOERR)
    {
        MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DVR,
                "<<DVR>> %s - Could not write recording metadata file!\n",
                __FUNCTION__);
    }

    // Create recording name
    snprintf(recordingName, OS_DVR_MAX_NAME_SIZE, "%s/%s", STORAGE_TRIM_ROOT(
            volume->rootPath), platformRecName);

    // Just return the ts buffer as the unique recording handle
    *conversion = (mpe_DvrConversion) tsBuffer;

    newRecording->convertStartTime = convertStartTime;

    // Save the effective TSB convert start time (ms system time)
    *startTime = (tsBuffer->sysStartTime + convertStartTime) / NANO_MILLIS;

    MPEOS_LOG(MPE_LOG_INFO, MPE_MOD_DVR, "<<DVR>> %s - TSB = %p, startTime = %"PRId64", recording name = %s\n",
            __FUNCTION__, tsBuffer, *startTime, recordingName);

    mpeos_mutexRelease(tsBuffer->mutex);
    return MPE_DVR_ERR_NOERR;

    error1: mpe_memFree(ri_pids);

    error2: mpeos_mutexRelease(tsBuffer->mutex);
    return err;
}

/*
 * Description:
 *
 * This function will stop a TSB conversion
 *
 * Parameters:
 * conversion     handle to a tsb conversion to stop
 * immediate      a flag to indicate an immediate stop.
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_OS_FAILURE
 */
mpe_Error mpeos_dvrTsbConvertStop(mpe_DvrConversion conversion,
        mpe_Bool immediate)
{
    os_DvrTimeShiftBuffer *tsBuffer = (os_DvrTimeShiftBuffer*) conversion;
    mpe_Error err;

    MPE_UNUSED_PARAM(immediate);

    if (tsBuffer == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - mpe_DvrConversion handle is NULL\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    // If the given time shift buffer is invalid return error
    if (!lockTSB(tsBuffer))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - TSB is not valid! (%p)\n", __FUNCTION__, tsBuffer);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    // If the given time shift buffer is not actively recording return error.
    if (tsBuffer->recording == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - TSB is not currently converting! (%p)\n",
                __FUNCTION__, tsBuffer);
        err = MPE_DVR_ERR_INVALID_PARAM;
        goto error;
    }

    // stop the active recording with the same index as the active TSB
    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - TSB = %p\n",
            __FUNCTION__, tsBuffer);

    // Tell the platform to stop the conversion
    if (tsb_convert_stop(tsBuffer->platformHandle) != RI_ERROR_NONE)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Platform could not start TSB conversion!\n",
                __FUNCTION__);
        err = MPE_DVR_ERR_OS_FAILURE;
        goto error;
    }

    mpeos_mutexRelease(tsBuffer->mutex);
    return MPE_DVR_ERR_NOERR;

    error: mpeos_mutexRelease(tsBuffer->mutex);
    return err;
}

/*
 * Description:
 *
 * This function will change the current converted pids
 * If the platform does not support seamless pid change,
 * the error code MPE_DVR_ERR_UNSUPPORTED is returned.
 * Parameters:
 * conversion     handle to a conversion session
 * pids           an array containing the new pids to convert
 * pidCount       the number of pids in the array
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_OS_FAILURE
 *
 */
mpe_Error mpeos_dvrTsbConvertChangePids(mpe_DvrConversion conversion,
        mpe_DvrPidInfo *pids, uint32_t pidCount)
{
    MPE_UNUSED_PARAM(conversion);
    MPE_UNUSED_PARAM(pids);
    MPE_UNUSED_PARAM(pidCount);

    // RITODO
    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "%s - NOT IMPLEMENTED!\n",
            __FUNCTION__);

    return MPE_DVR_ERR_UNSUPPORTED;
}


/*
 * Description:
 *
 *
 *
 * Parameters:
 *  recName
 *
 * Returns:
 */
mpe_Bool mpeos_dvrIsDecodable(char *recName)
{
    MPE_UNUSED_PARAM(recName);

    //RITODO
    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "%s - NOT IMPLEMENTED!\n",
            __FUNCTION__);

    return MPE_DVR_ERR_NOT_IMPLEMENTED;
}

/*
 * Description:
 *
 *
 * Parameters:
 *  recName
 *
 * Returns:
 */
mpe_Bool mpeos_dvrIsDecryptable(char *recName)
{
    MPE_UNUSED_PARAM(recName);

    //RITODO
    MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "%s - NOT IMPLEMENTED!\n",
            __FUNCTION__);

    return MPE_DVR_ERR_NOT_IMPLEMENTED;
}

/*
 * Description:
 *
 *
 * Parameters:
 *  recName
 *
 * Returns:
 */
mpe_Bool isRecordingInProgress(char *recordingName)
{
    char datafile[MPE_FS_MAX_PATH];
    int i=0;

    if (recordingName == NULL ||
            (convertRecordingNameToDatafile(recordingName, datafile) == -1))
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - recordingName:%s Invalid Parameter..\n", __FUNCTION__, recordingName);
        return FALSE;
    }
#ifdef MPE_WINDOWS
    // Convert /c/ to c:/
    datafile[0] = datafile[1];
    datafile[1] = ':';
#endif
    //
    // verify if this recording is currently active as a recording
    //

    // Search active recordings
    for (i = 0; i < MAX_TSBS; i++)
    {
        if (gTimeShiftBuffers[i] != NULL
                && gTimeShiftBuffers[i]->recording != NULL
                && (strcmp(gTimeShiftBuffers[i]->recording->datafile, datafile) == 0))
        {
            MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                    "<<DVR>> %s - Recording is active!\n",
                    __FUNCTION__);
            return TRUE;
        }
    }

    return FALSE;
}

/**
 * Description:
 *
 * This function retrieves the number of media volumes found on the specified
 * storage device.
 *
 * Paramters
 *  count is the number of devices found
 *
 * Returns
 *  MPE_DVR_ERR_NOERR - no error
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_DEVICE_ERR
 */
mpe_Error mpeos_dvrMediaVolumeGetCount(mpe_StorageHandle device,
        uint32_t *count)
{
    os_MediaVolumeInfo *volume;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Device (%s)\n",
            __FUNCTION__, device->name);

    *count = 0;

    mpeos_mutexAcquire(device->mutex);

    for (volume = device->volumes; volume != NULL; volume = volume->next)
        (*count)++;

    mpeos_mutexRelease(device->mutex);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - Media volume count is %d\n", __FUNCTION__, *count);

    return MPE_DVR_ERR_NOERR;
}

/**
 * Description:
 *
 * This function retrieves the list of media storage volumes on a given storage device.
 *
 * Paramters:
 *  device    the storage handle of the device to query
 *  count     on input, this parameter specifies the number of volume handles that
 *            can fit into the pre-allocated volumes array passed as the second
 *            parameter.  On output, indicates the actual number of volume handles
 *            returned in the volumes array.
 *  volumes   pre-allocated memory buffer for returning an array of native volume
 *            handles.
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR if no error
 *  MPE_DVR_ERR_INVALID_PARAM
 *  MPE_DVR_ERR_DEVICE_ERR
 *  MPE_DVR_ERR_BUF_TOO_SMALL indicates that the pre-allocated volumes buffer is
 *            not large enough to fit all of the native volume handles found on
 *            the storage device.  Only as many volume handles as can fit in the
 *            buffer are returned.
 */
mpe_Error mpeos_dvrMediaVolumeGetList(mpe_StorageHandle device,
        uint32_t *count, mpe_MediaVolume *volumes)
{
    uint32_t actualCount = 0;
    os_MediaVolumeInfo *volume;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Device (%s)\n",
            __FUNCTION__, device->name);

    mpeos_mutexAcquire(device->mutex);

    volume = device->volumes;
    while (actualCount < *count && volume != NULL)
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
                "<<DVR>> %s - Adding volume %s (%p)\n", __FUNCTION__,
                volume->rootPath, volume);
        volumes[actualCount] = volume;
        actualCount++;
        volume = volume->next;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Actual count is %d\n",
            __FUNCTION__, actualCount);

    mpeos_mutexRelease(device->mutex);

    *count = actualCount;
    return MPE_DVR_ERR_NOERR;
}

/**
 * Description:
 *
 * This function registers a specified queue to receive media volume related events.
 * Only one queue may be registered at a time.  Subsequent calls replace previously
 * registered queue.
 *
 * Parameters:
 *  eventQueue   specifies the destination queue for media volume events
 *  act          specifies the asynchronous completion token.  Media volume
 *               events delivered by the MPE DVR module to the specified queue
 *               will carry this value in the optionalData2 parameter.  The
 *               actual value of this token should be the Event Dispatcher
 *               handle (mpe_EdEventInfo*) created by a call to mpe_edCreateHandle.
 *
 * Events:
 *  MPE_MEDIA_VOL_EVT_FREE_SPACE_ALARM is triggered when the free space on a media
 *               volume drops below the threshold level of a registered free space
 *               alarm on that media volume.
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR if no error
 *  MPE_DVR_ERR_INVALID_PARAM specified queue is invalid
 */
mpe_Error mpeos_dvrMediaVolumeRegisterQueue(mpe_EventQueue eventQueue,
        void *act)
{
    gVolumeMgrData.listenerQueue = eventQueue;
    gVolumeMgrData.listenerACT = act;
    gVolumeMgrData.queueRegistered = TRUE;

    return MPE_DVR_ERR_NOERR;
}

/**
 * Description:
 *
 * This function registers a free space alarm for the specified media volume.
 * Multiple alarms may be registered for the same volume, but the level must be
 * unique across all free space alarms for that volume.
 *
 * Parameters:
 *  volume     specifies the media volume to monitor for free space
 *  level      specifies the level of free space remaining at which
 *             point the caller would like to be notified.  This value is a
 *             percentage of the specified media volume�s capacity.  The valid
 *             range of values is 1-99.
 *
 * Events:
 *  MPE_MEDIA_VOL_EVT_FREE_SPACE_ALARM is triggered when the free space for the
 *             specified media storage volume drops below the specified level
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR - no error
 *  MPE_DVR_ERR_INVALID_PARAM - specified either an invalid volume or an invalid level
 *  MPE_DVR_ERR_NOT_ALLOWED - the specified alarm criteria is already registered
 */
mpe_Error mpeos_dvrMediaVolumeAddAlarm(mpe_MediaVolume volume, uint8_t level)
{
    mpe_Error err = MPE_DVR_ERR_NOERR;
    os_VolumeSpaceAlarm* walker;
    os_VolumeSpaceAlarm* newAlarm;

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - Volume = %s (%p), level = %d\n", __FUNCTION__,
            volume->rootPath, volume, level);

    if (level < MIN_MEDIA_VOL_LEVEL || level > MAX_MEDIA_VOL_LEVEL || volume
            == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid parameters! volume = %p, level = %d\n",
                __FUNCTION__, volume, level);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    mpeos_mutexAcquire(volume->mutex);

    walker = volume->alarmList;

    /* check if alarm of particular level is already set*/
    while (walker != NULL)
    {
        if (walker->level == level)
        {
            err = MPE_DVR_ERR_NOT_ALLOWED;
            goto error;
        }
        walker = walker->next;
    }

    /* Allocate a new alarm structure */
    if (mpe_memAlloc(sizeof(os_VolumeSpaceAlarm), (void**) &newAlarm)
            != MPE_SUCCESS)
    {
        err = MPE_ENOMEM;
        goto error;
    }

    newAlarm->status = ARMED;
    newAlarm->level = level;
    newAlarm->next = NULL;

    /* Insert the new alarm in sorted order */

    if (volume->alarmList == NULL) /* Is the list empty? */
    {
        volume->alarmList = newAlarm;
    }
    else if (level < volume->alarmList->level) /* insert at head */
    {
        newAlarm->next = volume->alarmList;
        volume->alarmList = newAlarm;
    }
    else
    {
        mpe_Bool inserted = FALSE;
        walker = volume->alarmList;
        while (walker->next != NULL) /* Walk the list looking for the correct spot */
        {
            if (level < walker->next->level) /* Found the spot to insert */
            {
                newAlarm->next = walker->next;
                walker->next = newAlarm;
                inserted = TRUE;
                break;
            }

            walker = walker->next;
        }

        if (!inserted) /* End of the list? */
        {
            walker->next = newAlarm;
        }
    }

    mpeos_mutexRelease(volume->mutex);

    return MPE_DVR_ERR_NOERR;

    error: mpeos_mutexRelease(volume->mutex);
    return err;
}

/**
 * Description:
 *
 * This function unregisters a free space alarm for the specified media volume.
 *
 * Parameters:
 *  volume     the media volume
 *  level      the threshold level to unregister
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM
 */
mpe_Error mpeos_dvrMediaVolumeRemoveAlarm(mpe_MediaVolume volume, uint8_t level)
{
    os_VolumeSpaceAlarm* walker;
    os_VolumeSpaceAlarm* follower;

    if (level < MIN_MEDIA_VOL_LEVEL || level > MAX_MEDIA_VOL_LEVEL || volume
            == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Invalid parameters\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - Volume = %s (%p), level = %d\n", __FUNCTION__,
            volume->rootPath, volume, level);

    mpeos_mutexAcquire(volume->mutex);

    walker = volume->alarmList;
    follower = NULL;

    while (walker != NULL)
    {
        if (walker->level == level)
        {
            if (follower == NULL)
                volume->alarmList = walker->next; /* Front of list */
            else
                follower->next = walker->next; /* All other places */
            mpe_memFree(walker);
            break;
        }

        if (walker == volume->alarmList)
            follower = volume->alarmList; // First time
        else if (follower)
            follower = follower->next; // Other times

        walker = walker->next;
    }

    mpeos_mutexRelease(volume->mutex);

    return MPE_DVR_ERR_NOERR;
}

/**
 * Description:
 *
 * This function is used to retrieve attributes of a specified media storage volume.
 *
 * Parameters:
 *  volume      identifies the media storage volume to query
 *  param       identifies which attribute to query
 *  output      stores the resulting value.  This buffer is allocated by the caller.
 *
 * Returns:
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM if specified an invalid parameter.
 *  MPE_DVR_ERR_NOT_ALLOWED
 *  MPE_DVR_ERR_DEVICE_ERR if I/O failure
 */
mpe_Error mpeos_dvrMediaVolumeGetInfo(mpe_MediaVolume volume,
        mpe_MediaVolumeInfoParam param, void *output)
{
    mpe_Error result = MPE_DVR_ERR_NOERR;

    if (!output || !volume)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "<<DVR>> %s - Invalid parameter",
                __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    mpeos_mutexAcquire(volume->mutex);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s Volume = %s (%p)\n",
            __FUNCTION__, volume->rootPath, volume);

    switch (param)
    {
    case MPE_DVR_MEDIA_VOL_SIZE:
        *((uint64_t*) output) = volume->reservedSize;
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s MPE_DVR_MEDIA_VOL_SIZE = %"PRIu64"\n",
                __FUNCTION__, volume->reservedSize);
        break;
    case MPE_DVR_MEDIA_VOL_FREE_SPACE:
    {
        uint64_t freeSize = 0;

        if (volume->reservedSize != 0)
            freeSize = volume->reservedSize - volume->usedSize;
        else
            freeSize = volume->device->freeMediaSize;

        *((uint64_t*) output) = freeSize;
        MPEOS_LOG    (MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s MPE_DVR_MEDIA_VOL_FREE_SPACE = %"PRIu64", reservedSize = %"PRId64", usedSize = %"PRId64", device freeMediaSize = %"PRId64"\n",
                      __FUNCTION__, freeSize, volume->reservedSize, volume->usedSize, volume->device->freeMediaSize);
        break;
    }
    
    case MPE_DVR_MEDIA_VOL_PATH:
        strcpy(output, volume->rootPath);
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s MPE_DVR_MEDIA_VOL_PATH = %s\n",
                  __FUNCTION__, volume->rootPath);
        break;

    case MPE_DVR_MEDIA_VOL_CREATE_TIME:
    {
        mpe_FileInfo info;
        if (mpe_fileGetStat(volume->rootPath, MPE_FS_STAT_CREATEDATE, &info) != MPE_SUCCESS)
        {
            MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                      "<<DVR>> %s Error retrieving MPE_DVR_MEDIA_VOL_CREATE_TIME!\n", __FUNCTION__);
            result = MPE_DVR_ERR_INVALID_PARAM;
        }
        else
        {
            *((uint32_t*) output) = info.createDate;
        }
        break;
    }
        
    case MPE_DVR_MEDIA_VOL_TSB_MIN_SIZE_BYTES:
    {
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s MPE_DVR_MEDIA_VOL_TSB_MIN_SIZE_BYTES = %"PRIu64"\n",
                __FUNCTION__, volume->tsbMinSizeBytes);
        *((uint64_t*) output) = volume->tsbMinSizeBytes;
        break;
    }

    default:
        result = MPE_DVR_ERR_INVALID_PARAM;
    }
    
    mpeos_mutexRelease(volume->mutex);
    return result;
}

/**
 * Description:
 *
 * This function is used to create a new media storage volume on a specified
 * storage device.  By default, the newly created media volume will have no
 * minimum guaranteed size and may use as much space as is available on the
 * storage device that is not already reserved by other media volumes on that
 * device.  To change the size of the media volume, call
 * mpe_dvrMediaVolumeSetInfo(MPE_DVR_MEDIA_VOL_SIZE).
 *
 * Note that this function does not accept volume name, org Id, app Id, or
 * extended file access permissions as input.  The reason is because they are
 * not needed at this level.  Each media volume created at the MPE level will
 * have a corresponding MediaStorageVolume object at the Java layer which
 * inherits the functionality of a LogicalStorageVolume object.  The volume
 * name, org Id, app Id, and extended file access permissions associated with
 * the LSV object at the Java layer also implicitly apply to the corresponding
 * media volume.  However, this information is not completely hidden from the
 * MPE layer because the volume name, org Id, and app Id are encoded in the path
 * specified by the caller and the EFAP can be obtained from the MPE OS ITFS File
 * System module using this same path.
 *
 * Parameters:
 *  device      the handle of the storage device where the media volume is to
 *              be created
 *  path        this is the full path of the volume as it will be presented to
 *              applications via the LogicalStorageVolume.getPath() method
 *              implemented at the Java layer.  This path must be unique across
 *              all media volumes on the specified storage device.  This path must
 *              begin with the MPE_STORAGE_GPFS_PATH of the specified storage device
 *              to be considered valid.  No other identifiers are needed to uniquely
 *              identify this volume.  The reason that this path is used as the
 *              identifier for the media volume is because this path also uniquely
 *              identifies the corresponding LogicalStorageVolume instance maintained
 *              at the Java layer and because the MPE OS DVR module implemented on
 *              platforms that support exposing media files on a media volume through
 *              java.io would need this information to do so.  On S-A platforms, this
 *              path is only used by the MPE OS DVR module as an identifier.  In other
 *              words, media files recorded on the media volume will not be visible
 *              through java.io using this path.  Note that this is not the same path
 *              where the MPE OS DVR module will store its internal meta-data for this
 *              media volume.
 *  volume      a handle to the newly created media volume if successful
 *
 * Returns
 *  MPE_DVR_ERR_NOERR if media storage volume was successfully created
 *  MPE_DVR_ERR_INVALID_PARAM if specified path is not unique, mount point in the
 *              specified path does not match up with the mount point for the
 *              specified device, or invalid device or volume parameter is NULL.
 *  MPE_DVR_ERR_DEVICE_ERR
 *  MPE_DVR_ERR_UNSUPPORTED if the platform does not support creation of media
 *              storage volumes
 *  MPE_DVR_ERR_NOT_ALLOWED - caller attempted to create a second MSV for the
 *              same application on the same storage device.  An application may
 *              only have one MSV per storage device.
 */
mpe_Error mpeos_dvrMediaVolumeNew(mpe_StorageHandle device, char *path,
        mpe_MediaVolume *volume)
{
    os_MediaVolumeInfo *newVolume;
    os_MediaVolumeInfo *scanVolume;

    /* Check whether parameters are valid */
    if (path == NULL || volume == NULL || device == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s -  Invalid parameter\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    if (strncmp(path, device->rootPath, strlen(device->rootPath)) != 0)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Path does not match GPFS\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - path = %s\n",
            __FUNCTION__, path);

    mpeos_mutexAcquire(device->mutex);

    if (device->status != MPE_STORAGE_STATUS_READY)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Device is not ready\n", __FUNCTION__);
        mpeos_mutexRelease(device->mutex);
        return MPE_DVR_ERR_NOT_ALLOWED;
    }

    scanVolume = device->volumes;
    // Make sure the path isn't a duplicate
    while (scanVolume != NULL)
    {
        if (strcmp(path, scanVolume->rootPath) == 0)
        {
            MPEOS_LOG(MPE_LOG_WARN, MPE_MOD_DVR,
                    "<<DVR>> %s - Volume with same path already exists: %s\n",
                    __FUNCTION__, path);
            *volume = (mpe_MediaVolume) scanVolume;
            mpeos_mutexRelease(device->mutex);
            return MPE_DVR_ERR_NOERR;
        }

        scanVolume = scanVolume->next;
    }

    // Add the new volume to the list
    (void) mpe_memAlloc(sizeof(os_MediaVolumeInfo), (void**) &newVolume);
    newVolume->next = device->volumes;
    device->volumes = newVolume;

    // Set all of the attributes for the new volume
    sprintf(newVolume->rootPath, "%s", path);
    sprintf(newVolume->mediaPath, "%s/%s", newVolume->rootPath,
            OS_DVR_MEDIA_DIR_NAME);
    sprintf(newVolume->dataPath, "%s/%s", newVolume->rootPath,
            OS_DVR_MEDIA_DATA_DIR_NAME);
    newVolume->device = device;
    newVolume->usedSize = 0;
    newVolume->reservedSize = 0;
    newVolume->alarmList = NULL;
    //initialize min TSB size to zero until initialized by application
    newVolume->tsbMinSizeBytes = 0;

    mpeos_mutexNew(&(newVolume->mutex));

    (void) createFullPath(newVolume->mediaPath);
    (void) createFullPath(newVolume->dataPath);

    // Write the data file for the volume
    updateVolumeDataFile(device);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR,
            "<<DVR>> %s - Volume created = %s (%p)\n", __FUNCTION__,
            newVolume->rootPath, newVolume);

    mpeos_mutexRelease(device->mutex);

    *volume = newVolume;

    return MPE_DVR_ERR_NOERR;
}

/**
 * Description:
 *
 * This function deletes a media storage volume.  The effect of calling this
 * function is that any disk space reserved for this volume is immediately
 * returned to the pool of MPE_DVR_STORAGE_MEDIAFS_ALLOCATABLE_SPACE.
 *
 * Pre-conditions:
 *  The contents of the media volume must have already been deleted.  The
 *  reason for this pre-condition is because the MPE OS does not maintain
 *  the database of recorded services and its associated meta-data.  This is
 *  done at the Java layer.
 *
 * Parameters
 *  volume      The volume to delete
 *
 * Returns
 *  MPE_DVR_ERR_NOERR if successfully deleted volume or volume does not exist
 *  MPE_DVR_ERR_NOT_ALLOWED if the media volume is not empty
 *  MPE_DVR_ERR_DEVICE_ERR
 */
mpe_Error mpeos_dvrMediaVolumeDelete(mpe_MediaVolume volume)
{
    mpe_Error result = MPE_DVR_ERR_NOERR;
    os_MediaVolumeInfo *walker;
    os_MediaVolumeInfo *prev;

    mpeos_mutexAcquire(volume->device->mutex);

    // Find volume in the list for the device
    prev = walker = volume->device->volumes;
    while (walker != NULL && walker != volume)
    {
        // Increment our previous pointer once the walker has left
        // the front of the list
        if (walker != volume->device->volumes)
            prev = prev->next;

        walker = walker->next;
    }

    if (walker == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "<<DVR>> %s - Invalid volume\n",
                __FUNCTION__);
        mpeos_mutexRelease(volume->device->mutex);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Volume = %s (%p)\n",
            __FUNCTION__, volume->rootPath, (os_MediaVolumeInfo*) volume);

    // Remove it from the device's list of volumes
    if (walker == volume->device->volumes)
        volume->device->volumes = walker->next; // First in list
    else
        prev->next = walker->next;

    // Update device's free size
    if (volume->reservedSize != 0)
        volume->device->freeMediaSize += volume->reservedSize;
    else
        volume->device->freeMediaSize += volume->usedSize;

    // Update our device's media volume datafile
    updateVolumeDataFile(volume->device);

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Volume deleted.  Device free size now = %"PRIu64"\n",
            __FUNCTION__, volume->device->freeMediaSize);

    mpeos_mutexRelease(volume->device->mutex);

    deleteMediaVolume(volume);

    return result;
}

/**
 * Description:
 *
 * This function is used to modify attributes of a media storage volume.
 *
 * Parameters
 *  volume      the media volume whose attribute is being set
 *  param       specifies the attribute to modify.  See table below for valid
 *              range of values.
 *  value       a pointer of the new value of the modified attribute.  See table
 *              below for valid range of values.
 *
 * Returns
 *  MPE_DVR_ERR_NOERR
 *  MPE_DVR_ERR_INVALID_PARAM if one of the parameters specified is outside of
 *              the valid range of values.
 *  MPE_DVR_ERR_NOT_ALLOWED if attempted to modify an attribute of the default
 *              media storage volume owned by the system
 *  MPE_DVR_ERR_DEVICE_ERR - i/o failure
 */
mpe_Error mpeos_dvrMediaVolumeSetInfo(mpe_MediaVolume volume,
        mpe_MediaVolumeInfoParam param, void *value)
{
    mpe_Error err = MPE_DVR_ERR_NOERR;

    if (volume == NULL || value == NULL)
    {
        MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR,
                "<<DVR>> %s - Parameter is invalid\n", __FUNCTION__);
        return MPE_DVR_ERR_INVALID_PARAM;
    }

    MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s Volume = %s (%p)\n",
            __FUNCTION__, volume->rootPath, volume);

    mpeos_mutexAcquire(volume->device->mutex);
    mpeos_mutexAcquire(volume->mutex);

    switch (param)
    {
    case MPE_DVR_MEDIA_VOL_SIZE:
    {
        uint64_t newSize = *(uint64_t*) value;

        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Requesting new volume (%s) reserve size of %"PRId64" - used size: %"PRId64" - reserved size: %"PRId64"\n",
                __FUNCTION__, volume->rootPath, newSize, volume->usedSize, volume->reservedSize);

        // New size is smaller than the currently used space
        if (newSize < volume->usedSize)
        {
            MPEOS_LOG(
                    MPE_LOG_ERROR,
                    MPE_MOD_DVR,
                    "<<DVR>> %s - New value is negative or smaller than existing usage (%"PRIu64")\n",
                    __FUNCTION__, newSize);
            err = MPE_DVR_ERR_INVALID_PARAM;
            break;
        }

        if (volume->reservedSize == 0)
        {
            // We are limiting a previously unlimited volume, make sure the new limit
            // does not exceed the device's total media space
            if (newSize > volume->device->freeMediaSize)
            {
                MPEOS_LOG(MPE_LOG_ERROR, MPE_MOD_DVR, "<<DVR>> %s - Initializing volume reservation.  New value (%"PRId64") is larger than media filesystem on device (%"PRIu64")\n",
                        __FUNCTION__, newSize, volume->device->mediaFsSize);
                err = MPE_DVR_ERR_INVALID_PARAM;
                break;
            }
            volume->device->freeMediaSize -= newSize - volume->usedSize;
        }
        else if (newSize > volume->reservedSize)
        {
            // If we are expanding the existing volume size -- make sure we have enough room
            // on this device's media partition
            uint64_t delta = newSize - volume->reservedSize;

            if (delta > volume->device->freeMediaSize)
            {
                MPEOS_LOG(MPE_LOG_ERROR,
                        MPE_MOD_DVR,
                        "<<DVR>> %s - Modifying volume reservation.  New value (%"PRId64") is larger than available space on device (%"PRIu64")\n",
                        __FUNCTION__,
                        newSize,
                        volume->device->freeMediaSize);
                err = MPE_DVR_ERR_INVALID_PARAM;
                break;
            }

            volume->device->freeMediaSize -= delta;
        }
        else // contracting
        {
            volume->device->freeMediaSize += (volume->reservedSize - newSize);
        }

        volume->reservedSize = newSize;
        updateVolumeDataFile(volume->device);

        MPEOS_LOG (MPE_LOG_DEBUG,
            MPE_MOD_DVR,
            "<<DVR>> %s - Setting volume reserve size to %"PRId64" Volume = %s, Device free size = %"PRIu64"\n",
            __FUNCTION__,
            newSize,
            volume->rootPath,
            volume->device->freeMediaSize);
        }
        break;

    case MPE_DVR_MEDIA_VOL_TSB_MIN_SIZE_BYTES:
    {
        uint64_t newSize = *(uint64_t*) value;
        MPEOS_LOG (MPE_LOG_DEBUG,
                MPE_MOD_DVR,
                "<<DVR>> %s - Setting minimum TSB size in bytes to %"PRId64" Volume = %s\n",
                __FUNCTION__,
                newSize,
                volume->rootPath);
        volume->tsbMinSizeBytes = newSize;
        break;
    }

    default:
        MPEOS_LOG(MPE_LOG_DEBUG, MPE_MOD_DVR, "<<DVR>> %s - Unknown param value\n", __FUNCTION__);
        err = MPE_DVR_ERR_INVALID_PARAM;
        break;
    }

    mpeos_mutexRelease(volume->mutex);
    mpeos_mutexRelease(volume->device->mutex);

    return err;
}

